package com.rudi.audioplayer.data

import java.util.Locale

/**
 * Roadmap #5 — Ringtone Cutter. Logika murni penentuan rentang potong (pola sama
 * [com.rudi.audioplayer.playback.AbRepeatLogic]/`LrcSyncEditor`: Context-free supaya testable
 * tanpa Robolectric). [RingtoneEncoder] yang memakai [TrimRange] ini untuk encode file
 * sungguhan lewat MediaExtractor/MediaMuxer.
 */
object RingtoneCutter {

    /** Batas bawah — di bawah ini nyaris tidak berguna sebagai nada dering (klik doang). */
    const val MIN_DURATION_MS = 1_000L

    /** Batas atas wajar untuk nada dering/notifikasi/alarm — bukan pemutar lagu penuh. */
    const val MAX_DURATION_MS = 60_000L

    data class TrimRange(val startMs: Long, val endMs: Long) {
        val durationMs: Long get() = endMs - startMs
    }

    /**
     * Jepit [startMs]/[endMs] mentah (dari drag slider UI, bisa apa saja) ke rentang valid
     * relatif [songDurationMs]: (1) tidak boleh keluar batas lagu, (2) durasi minimal
     * [MIN_DURATION_MS], (3) durasi maksimal [MAX_DURATION_MS]. Lagu yang lebih pendek dari
     * [MIN_DURATION_MS] (kasus langka/edge) mengembalikan seluruh lagu apa adanya — tidak
     * dipaksa memenuhi minimum yang mustahil dipenuhi.
     */
    fun clampRange(startMs: Long, endMs: Long, songDurationMs: Long): TrimRange {
        if (songDurationMs <= MIN_DURATION_MS) return TrimRange(0L, songDurationMs.coerceAtLeast(0L))

        var s = startMs.coerceIn(0L, songDurationMs - MIN_DURATION_MS)
        var e = endMs.coerceIn(s + MIN_DURATION_MS, songDurationMs)
        if (e - s > MAX_DURATION_MS) {
            // Prioritaskan menjaga titik AWAL yang user pilih (drag start biasanya lebih
            // disengaja — titik masuk reff/hook), geser titik akhir ke dalam.
            e = s + MAX_DURATION_MS
        }
        if (e > songDurationMs) {
            e = songDurationMs
            s = (e - MAX_DURATION_MS).coerceAtLeast(0L)
        }
        return TrimRange(s, e)
    }

    fun isValid(range: TrimRange, songDurationMs: Long): Boolean {
        return range.startMs in 0..songDurationMs &&
            range.endMs in range.startMs..songDurationMs &&
            range.durationMs in MIN_DURATION_MS..MAX_DURATION_MS
    }

    /** "mm:ss", pola sama `LrcSyncEditor.formatTimestamp` minus desimal (potongan nada
     *  dering tidak butuh presisi sub-detik yang terlihat user). */
    fun formatTimestamp(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return String.format(Locale.ROOT, "%02d:%02d", m, s)
    }
}
