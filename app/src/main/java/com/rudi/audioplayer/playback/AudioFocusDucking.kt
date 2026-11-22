package com.rudi.audioplayer.playback

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Batch 713 — pengelola fokus audio kustom (Pengaturan Putar > Fokus Audio > Ducking Fokus Audio; sejak Batch 714 berlaku LANGSUNG tanpa
 * mulai ulang; sejak Batch 715 default AKTIF, dimatikan lewat saklar yang sama). Latar: log v711/v712 membuktikan com.shopee.id berulang meminta fokus SEMENTARA (`requestAudioFocus req=2`),
 * sehingga ExoPlayer menjeda otomatis (6 jeda, total 53 dtk dari ±131 dtk pada 22 Nov 06:31-06:33). Saat saklar ON, ExoPlayer dibangun
 * dengan `handleAudioFocus=false` dan kelas ini mengurus fokus: fokus hilang sementara dari aplikasi lain -> volume DITURUNKAN (musik tetap
 * jalan), saat panggilan/dering -> dijeda, hilang permanen -> dijeda dan fokus dilepas, fokus kembali -> volume dipulihkan/dilanjutkan.
 */
internal enum class FocusAction { NONE, PAUSE, DUCK, RESUME_AND_UNDUCK, PAUSE_PERMANENT }

/** Langkah yang harus dijalankan [PlaybackService] saat saklar Ducking Fokus Audio berubah. */
internal enum class FocusToggleStep { NONE, ENABLE, DISABLE }

/**
 * Batch 714 — saklar berlaku LANGSUNG: log v713 (06:44-06:47) menunjukkan pengguna mengetes Shopee tanpa baris `Ducking fokus audio AKTIF`
 * (2 jeda fokus masih lewat jalur ExoPlayer) karena saklar hanya dibaca saat service dibuat. [step] murni: [enabled] = nilai saklar,
 * [active] = pengelola kustom sedang terpasang.
 */
internal object FocusToggle {
    fun step(enabled: Boolean, active: Boolean): FocusToggleStep = when {
        enabled && !active -> FocusToggleStep.ENABLE
        !enabled && active -> FocusToggleStep.DISABLE
        else -> FocusToggleStep.NONE
    }
}

/** Keputusan murni atas satu perubahan fokus ([change] = `AudioManager.AUDIOFOCUS_*`; [inCall] = mode audio panggilan/dering). */
internal object FocusPolicy {
    /** Mode audio panggilan/dering ([mode] = `AudioManager.MODE_*`). */
    fun isCallMode(mode: Int): Boolean =
        mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_RINGTONE

    /**
     * Batch 715 — pengaman default AKTIF: fokus sementara dari telepon bisa tiba SEBELUM `AudioManager.mode` berganti, sehingga dering
     * dikira video biasa dan hanya diturunkan volumenya. Bila mode kemudian berubah ke panggilan/dering SELAGI volume diturunkan -> jeda.
     */
    fun onModeChange(mode: Int, ducked: Boolean): FocusAction =
        if (ducked && isCallMode(mode)) FocusAction.PAUSE else FocusAction.NONE

    fun onFocusChange(change: Int, inCall: Boolean): FocusAction = when (change) {
        AudioManager.AUDIOFOCUS_GAIN -> FocusAction.RESUME_AND_UNDUCK
        AudioManager.AUDIOFOCUS_LOSS -> FocusAction.PAUSE_PERMANENT
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (inCall) FocusAction.PAUSE else FocusAction.DUCK
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> FocusAction.DUCK
        else -> FocusAction.NONE
    }
}

/** Efek yang harus dijalankan pemanggil setelah [DuckingFocusState.apply]. */
internal data class FocusEffects(
    val pause: Boolean = false,
    val resume: Boolean = false,
    val duck: Boolean = false,
    val unduck: Boolean = false,
    val abandon: Boolean = false
)

/** Status penahanan akibat fokus (murni Kotlin, diuji di JVM): sedang diturunkan volumenya dan/atau dijeda oleh fokus. */
internal class DuckingFocusState {
    var ducked = false
        private set
    var pausedByFocus = false
        private set

    fun apply(action: FocusAction): FocusEffects = when (action) {
        FocusAction.DUCK ->
            if (ducked) {
                FocusEffects()
            } else {
                ducked = true
                FocusEffects(duck = true)
            }
        FocusAction.PAUSE -> {
            pausedByFocus = true
            FocusEffects(pause = true)
        }
        FocusAction.RESUME_AND_UNDUCK -> {
            val effects = FocusEffects(resume = pausedByFocus, unduck = ducked)
            pausedByFocus = false
            ducked = false
            effects
        }
        FocusAction.PAUSE_PERMANENT -> {
            val effects = FocusEffects(pause = true, unduck = ducked, abandon = true)
            pausedByFocus = false
            ducked = false
            effects
        }
        FocusAction.NONE -> FocusEffects()
    }

    /** Fokus tertunda: pemutaran ditahan sampai fokus diberikan (GAIN). */
    fun holdForDelayedFocus() {
        pausedByFocus = true
    }

    /** Pengguna menjeda manual / sesi berakhir: lepas semua penahanan. True bila volume perlu dipulihkan. */
    fun reset(): Boolean {
        val needUnduck = ducked
        ducked = false
        pausedByFocus = false
        return needUnduck
    }
}

/**
 * Penghubung Android untuk [DuckingFocusState]: meminta/melepas fokus mengikuti `playWhenReady`, menjalankan efek pada [player]
 * (volume diturunkan ke [DUCK_FACTOR] dari volume saat itu, lalu dipulihkan). Tindakan internal (jeda/lanjut karena fokus) memicu
 * `onPlayWhenReadyChanged` yang SENGAJA diabaikan lewat penghitung [expectedInternal], supaya tak dikira jeda/putar dari pengguna.
 * Batasan: volume yang diubah pengguna/crossfade SELAMA ducking (beberapa detik) ditimpa saat dipulihkan.
 */
internal class DuckingFocusController(
    private val context: Context,
    private val player: ExoPlayer,
    private val log: (String) -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private val state = DuckingFocusState()
    private var request: AudioFocusRequest? = null
    private var savedVolume = 1f
    private var expectedInternal = 0

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change -> onFocusChange(change) }

    // Batch 715 — API 31 (minSdk project = 31); callback di thread utama (executor utama), sama dengan [handler].
    private val modeListener = AudioManager.OnModeChangedListener { mode -> onModeChanged(mode) }

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (expectedInternal > 0) {
                expectedInternal--
            } else if (playWhenReady) {
                requestFocus()
            } else {
                releaseHold()
            }
        }
    }

    fun attach() {
        player.addListener(playerListener)
        audioManager.addOnModeChangedListener(context.mainExecutor, modeListener)
        log("Ducking fokus audio AKTIF: ExoPlayer handleAudioFocus=false, fokus diurus kustom (turunkan volume x$DUCK_FACTOR saat fokus sementara hilang)")
        // Batch 714 — dipasang saat pemain SUDAH memutar (saklar dinyalakan langsung): ExoPlayer baru saja melepas fokusnya, jadi minta fokus
        // sekarang; pada pemasangan di onCreate `playWhenReady` masih false dan fokus diminta oleh listener saat mulai memutar.
        if (player.playWhenReady) requestFocus()
    }

    fun release() {
        player.removeListener(playerListener)
        audioManager.removeOnModeChangedListener(modeListener)
        handler.removeCallbacksAndMessages(null)
        if (state.reset()) restoreVolume()
        abandonFocus()
    }

    private fun requestFocus() {
        val req = request ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAcceptsDelayedFocusGain(true)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(focusListener, handler)
            .build()
            .also { request = it }
        when (audioManager.requestAudioFocus(req)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> log("Fokus audio diberikan")
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                state.holdForDelayedFocus()
                internalPause()
                log("Fokus audio TERTUNDA: dijeda sampai fokus diberikan")
            }
            else -> {
                internalPause()
                log("Fokus audio DITOLAK: dijeda")
            }
        }
    }

    private fun releaseHold() {
        if (state.reset()) restoreVolume()
        abandonFocus()
    }

    private fun onFocusChange(change: Int) {
        val inCall = FocusPolicy.isCallMode(audioManager.mode)
        val action = FocusPolicy.onFocusChange(change, inCall)
        val effects = state.apply(action)
        if (effects.unduck) restoreVolume()
        if (effects.duck) duck()
        if (effects.pause) internalPause()
        if (effects.resume) internalPlay()
        if (effects.abandon) abandonFocus()
        log("Fokus berubah: kode=$change panggilan=$inCall aksi=$action volume=${player.volume}")
    }

    private fun onModeChanged(mode: Int) {
        val action = FocusPolicy.onModeChange(mode, state.ducked)
        if (action == FocusAction.NONE) return
        val effects = state.apply(action)
        if (effects.pause) internalPause()
        log("Mode audio berubah: mode=$mode saat volume diturunkan -> aksi=$action (dijeda, lanjut saat fokus kembali)")
    }

    private fun duck() {
        savedVolume = player.volume
        player.volume = savedVolume * DUCK_FACTOR
    }

    private fun restoreVolume() {
        player.volume = savedVolume
    }

    private fun internalPause() {
        if (player.playWhenReady) {
            expectedInternal++
            player.pause()
        }
    }

    private fun internalPlay() {
        if (!player.playWhenReady) {
            expectedInternal++
            player.play()
        }
    }

    private fun abandonFocus() {
        request?.let { audioManager.abandonAudioFocusRequest(it) }
    }

    private companion object {
        const val DUCK_FACTOR = 0.2f
    }
}
