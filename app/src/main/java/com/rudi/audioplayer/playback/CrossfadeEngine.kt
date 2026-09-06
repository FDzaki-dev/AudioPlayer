package com.rudi.audioplayer.playback

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.rudi.audioplayer.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Batch 102 — Gap List item #1 (True Crossfade). Replaces the old single-player volume-envelope
 * "fake fade" (fade to near-silence, let the queue advance, fade back in — see CHANGELOG Batch
 * <old> / README "Catatan jujur soal Gapless Playback" before this batch) with a genuine second
 * audio engine that physically overlaps two [ExoPlayer] instances.
 *
 * [sessionPlayer] is the ONE player [PlaybackService]'s [androidx.media3.session.MediaSession]
 * ever points to — it is NEVER swapped or replaced. That's a deliberate choice, not an
 * oversight: `MediaSession.setPlayer(Player)` exists and looks like the obvious way to hand off
 * control to a second engine, but it's documented by Google's own media3 maintainers as
 * unreliable in practice (see https://github.com/androidx/media/issues/764 — "the entire media
 * session just seems to end when I switch players in this way"), and the officially-suggested
 * alternative (`ForwardingSimpleBasePlayer`) only exists from media3 1.4.0 — this project pins
 * 1.3.1 (see app/build.gradle.kts and PlaybackService's own class doc on why bumping that blind
 * has already broken the build twice before, Batch 23/24 and Batch 29). Session ownership never
 * moving means the notification, lock screen, Bluetooth/Android Auto controls, queue, shuffle,
 * and repeat mode — all of it — keep working exactly as before, with zero new risk surface.
 *
 * [overlapPlayer] is a second, private ExoPlayer that only ever holds ONE upcoming [MediaItem]
 * at a time. The session, notification, and UI never know it exists. Its only job is to
 * physically start playing the next track a few seconds early so its audio genuinely overlaps
 * [sessionPlayer]'s tail in the mixed output — that overlap IS the "true crossfade" the gap list
 * asked for, as opposed to one player fading itself out then back in around a silent gap.
 *
 * ## Mechanism
 * 1. [maybeStartCrossfade] (polled every ~250ms by [PlaybackService] while something is
 *    playing) starts [overlapPlayer] on the resolved next item once [sessionPlayer] is within
 *    [crossfadeDurationMs] of the end of its current item, then ramps volume: sessionPlayer
 *    target→0, overlapPlayer 0→target. Both are audibly mixed for that whole window — genuine
 *    overlap, not a gap.
 * 2. [sessionPlayer]'s own queue/timeline/shuffle/repeat state is NEVER touched by this engine —
 *    it's left alone to reach its own natural end-of-item and its own internal gapless
 *    transition into the next item, exactly like before this batch. That means shuffle order,
 *    repeat-all wraparound, and manual queue edits all keep being resolved by the same
 *    already-correct ExoPlayer machinery that handled them before — nothing here re-implements
 *    or risks that logic. By the time that transition happens, sessionPlayer's volume is already
 *    at (or very near) 0 from the ramp above, so it silently starts playing the same next track
 *    from position 0 — inaudible, so this does not yet cause an echo.
 * 3. [onSessionAutoTransition] fires right on that transition (reason == AUTO, not a seek).
 *    Because sessionPlayer is silent at this exact instant, it's safe to immediately
 *    `seekTo(overlapPlayer.currentPosition)` on it — any seek latency is inaudible — then ramp
 *    sessionPlayer 0→target while ramping overlapPlayer target→0. Both are now position-locked
 *    to the same content, so this handback ramp cannot itself produce an echo.
 * 4. Once the handback ramp finishes, overlapPlayer is paused and cleared — idle, ready for the
 *    next transition.
 *
 * [onSessionManualDiscontinuity] aborts everything immediately on any genuinely manual seek
 * (skip button, seek bar, headset button, notification action, lock screen, Bluetooth) so a
 * manual skip can never leave overlapPlayer talking over whatever the user jumped to.
 * [onSessionPlayWhenReadyChanged] mirrors pause/resume onto overlapPlayer while a crossfade is
 * in flight, so hitting pause mid-transition doesn't leave the incoming track playing on its own.
 *
 * ## Known, deliberate limitations (documented, not fixed this batch — see PROJECT_STATE.md)
 * - Equalizer/Visualizer are bound to [PlaybackAudioSession.sessionId], which mirrors
 *   sessionPlayer's own ExoPlayer-assigned audio session id. overlapPlayer gets its own,
 *   separate audio session id (it's a second, independent ExoPlayer/AudioTrack) — so an active
 *   EQ curve or the visualizer capture briefly does not apply to the incoming track's audio
 *   during the ~[crossfadeDurationMs] overlap window specifically. Both are opt-in features;
 *   this is a narrow, cosmetic gap, not a functional one.
 * - If the user drags the in-app volume slider while a crossfade is actively ramping, that
 *   slider write and this engine's own ramp ticks briefly race on `sessionPlayer.volume` — the
 *   ramp's next tick overwrites it, so the slider can appear to "catch up" only once the ramp
 *   finishes (well under [crossfadeDurationMs]). Minor, transient, and only during the narrow
 *   overlap window itself.
 */
@UnstableApi
class CrossfadeEngine(
    private val sessionPlayer: ExoPlayer,
    private val overlapPlayer: ExoPlayer,
    private val scope: CoroutineScope,
    private val crossfadeDurationMs: Long = 3000L
) {
    @Volatile
    var enabled: Boolean = false
        private set

    private enum class State { IDLE, FADING, HANDBACK }

    private var state = State.IDLE
    private var triggeredForIndex = -1
    private var rampJob: Job? = null

    // Batch 366 — nilai sessionPlayer.volume yang ditangkap SEBELUM ramp apa pun menyentuhnya
    // di awal siklus fade (lihat maybeStartCrossfade()). Satu-satunya sumber kebenaran buat
    // restore di onSessionAutoTransition() & abort() — jangan pernah coba tebak ulang volume
    // "utuh" dari nilai yang sudah di tengah jalan diubah ramp, itu akar bug Batch 366 (lihat
    // abort() di bawah).
    private var preFadeVolume: Float = 1f

    // Tells onSessionManualDiscontinuity apart from this engine's own handback seek in
    // onSessionAutoTransition, which fires the exact same SEEK discontinuity reason and must
    // NOT be treated as a manual skip that aborts itself.
    @Volatile
    private var internalSeekInFlight = false

    init {
        overlapPlayer.volume = 0f
        overlapPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // Fail safe: if the next track can't even prepare (deleted/corrupt/permission
                // revoked since the queue was built), silently drop back to no-crossfade-this-
                // time rather than leaving sessionPlayer stuck faded down with nothing to hand
                // back to — the P0 error-recovery gap (item #8) is a separate batch, but a
                // crossfade engine failure must never itself be the cause of a silent player.
                AppLogger.e("CrossfadeEngine", "overlapPlayer gagal, batalkan crossfade ini", error)
                abort()
            }
        })
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) abort()
    }

    /** Call on every position-poll tick while sessionPlayer is alive. Cheap no-op unless
     * genuinely within the crossfade window. */
    fun maybeStartCrossfade() {
        if (!enabled || state != State.IDLE) return
        // Repeat-one's "next" item is itself — crossfading a track into its own restart is
        // exactly the "crossfade yang salah" the gap list calls out. Skip entirely; repeat-one
        // keeps its existing plain instant-restart behavior, untouched by this engine.
        if (sessionPlayer.repeatMode == Player.REPEAT_MODE_ONE) return
        if (!sessionPlayer.isPlaying) return

        val duration = sessionPlayer.duration
        if (duration <= 0 || duration == C.TIME_UNSET) return
        val remaining = duration - sessionPlayer.currentPosition
        if (remaining !in 1..crossfadeDurationMs) return

        val currentIndex = sessionPlayer.currentMediaItemIndex
        if (triggeredForIndex == currentIndex) return

        val nextIndex = sessionPlayer.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) return
        val nextItem = sessionPlayer.getMediaItemAt(nextIndex)

        triggeredForIndex = currentIndex
        state = State.FADING

        val target = sessionPlayer.volume.let { if (it < VOLUME_EPSILON) 1f else it }
        preFadeVolume = target

        overlapPlayer.setMediaItem(nextItem)
        overlapPlayer.volume = 0f
        overlapPlayer.prepare()
        overlapPlayer.play()

        val fadeSpan = remaining.coerceAtMost(crossfadeDurationMs).coerceAtLeast(200L)
        rampJob?.cancel()
        rampJob = scope.launch {
            animateDualVolume(fromA = target, toA = 0f, fromB = 0f, toB = target, durationMs = fadeSpan) { a, b ->
                sessionPlayer.volume = a
                overlapPlayer.volume = b
            }
        }
    }

    /** Call from sessionPlayer's own onMediaItemTransition when reason == MEDIA_ITEM_TRANSITION_
     * REASON_AUTO — i.e. it reached the end of an item naturally, not via seek/skip. */
    fun onSessionAutoTransition() {
        if (state != State.FADING) return
        state = State.HANDBACK
        rampJob?.cancel()

        // Batch 366 — dulu ditebak dari overlapPlayer.volume saat ini (rapuh: kalau ramp awal
        // maybeStartCrossfade() belum sempat benar-benar sampai ke target pas transisi alami
        // keburu terjadi — mis. sessionPlayer buru-buru resume dekat ujung lagu abis cold-start
        // eksternal — target jadi ikut fraksi asal-asalan, bukan volume "utuh" yang benar).
        // preFadeVolume adalah nilai ASLI yang ditangkap di awal siklus ini, selalu benar apa
        // pun progress ramp saat ini.
        val target = preFadeVolume

        internalSeekInFlight = true
        // Safe precisely because sessionPlayer's volume is already ~0 here — see class doc
        // step 3. Any decode/seek latency this causes is inaudible.
        sessionPlayer.seekTo(overlapPlayer.currentPosition)
        internalSeekInFlight = false

        val startB = overlapPlayer.volume
        rampJob = scope.launch {
            animateDualVolume(fromA = 0f, toA = target, fromB = startB, toB = 0f, durationMs = HANDBACK_MS) { a, b ->
                sessionPlayer.volume = a
                overlapPlayer.volume = b
            }
            overlapPlayer.pause()
            overlapPlayer.clearMediaItems()
            state = State.IDLE
            triggeredForIndex = -1
        }
    }

    /** Call from sessionPlayer's onPositionDiscontinuity for every discontinuity reason;
     * only [Player.DISCONTINUITY_REASON_SEEK] ones matter here. */
    fun onSessionManualDiscontinuity(reason: Int) {
        if (reason != Player.DISCONTINUITY_REASON_SEEK) return
        if (internalSeekInFlight) return
        if (state == State.IDLE) return
        abort()
    }

    /** Mirror pause/resume onto overlapPlayer while a crossfade is in flight, so pausing
     * mid-transition doesn't leave the incoming track playing on its own in the background. */
    fun onSessionPlayWhenReadyChanged(isPlaying: Boolean) {
        if (state == State.IDLE) return
        if (isPlaying) overlapPlayer.play() else overlapPlayer.pause()
    }

    fun release() {
        abort()
        overlapPlayer.release()
    }

    private fun abort() {
        rampJob?.cancel()
        rampJob = null
        if (state != State.IDLE) {
            // Batch 366 BUGFIX (root cause dari laporan user — audio bisu setelah lagu
            // pertama pasca kill+trigger eksternal, notifikasi/widget tetap "Memutar", cuma
            // hilang lagi setelah restart device). Baris lama:
            //   sessionPlayer.volume = sessionPlayer.volume.let { if (it <= 0f) 1f else it }
            // adalah NO-OP di HAMPIR SEMUA kejadian nyata: volume di tengah ramp (step 60ms,
            // lihat animateDualVolume) nyaris tidak pernah persis 0.0f, jadi cabang "reset ke
            // 1f" nyaris tidak pernah kena — baris itu efektif cuma menulis ulang volume ke
            // NILAINYA SENDIRI yang sudah turun (mis. 0.05), meninggalkan sessionPlayer
            // nyangkut nyaris bisu SETIAP kali abort() dipicu pertengahan fade/handback:
            // overlapPlayer.onPlayerError (decoder kedua gagal siap — paling rawan persis
            // detik-detik awal sesi baru dari cold-start eksternal, saat pipeline audio
            // pertama sendiri belum tentu selesai stabil), skip manual selagi fading, atau
            // toggle Nonaktifkan Crossfade pertengahan fade. Karena Service (dan sessionnya)
            // tetap hidup di background, sekadar buka-lagi App TIDAK membuat ExoPlayer baru
            // (volume tidak ikut reset ke default 1.0) — cuma restart device (yang benar-benar
            // mematikan proses Service) yang terasa "menyembuhkan", sampai kondisi yang sama
            // terpicu lagi. Fix: restore ke preFadeVolume yang ditangkap SEBELUM ramp apa pun
            // menyentuhnya — satu-satunya nilai yang selalu benar, tidak bergantung sudah
            // sejauh apa ramp berjalan saat abort() dipanggil.
            sessionPlayer.volume = preFadeVolume
            overlapPlayer.pause()
            overlapPlayer.clearMediaItems()
            overlapPlayer.volume = 0f
            AppLogger.w(
                "CrossfadeEngine",
                "Crossfade dibatalkan pertengahan (state=$state) — sessionPlayer.volume dipulihkan ke $preFadeVolume"
            )
        }
        state = State.IDLE
        triggeredForIndex = -1
    }

    private suspend fun animateDualVolume(
        fromA: Float,
        toA: Float,
        fromB: Float,
        toB: Float,
        durationMs: Long,
        apply: (Float, Float) -> Unit
    ) {
        val steps = (durationMs / STEP_MS).toInt().coerceAtLeast(1)
        for (i in 0..steps) {
            if (!scope.isActive) return
            val fraction = i / steps.toFloat()
            apply(
                (fromA + (toA - fromA) * fraction).coerceIn(0f, 1f),
                (fromB + (toB - fromB) * fraction).coerceIn(0f, 1f)
            )
            if (i < steps) delay(STEP_MS)
        }
    }

    companion object {
        private const val STEP_MS = 60L
        private const val HANDBACK_MS = 400L
        // Batch 366 — ambang toleransi float utk anggap "sudah 0", dipakai konsisten di semua
        // pengecekan (dulu ada yang exact `<= 0f`, ada yang `<= 0.01f`, tidak konsisten).
        private const val VOLUME_EPSILON = 0.01f
    }
}
