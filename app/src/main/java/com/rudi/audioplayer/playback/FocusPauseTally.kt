package com.rudi.audioplayer.playback

/**
 * Batch 711 — pencatat jeda yang disebabkan fokus audio hilang (murni Kotlin, bisa diuji di JVM).
 *
 * DASAR (log v711 `sonix_debug_20261121_234843.txt`, keluhan "audio sering naik turun"): `dumpsys audio` memuat
 * `requestAudioFocus() ... callingPack=com.shopee.id AA=USAGE_MEDIA req=2` (GAIN_TRANSIENT) lalu `abandonAudioFocus()` 1-14 dtk
 * kemudian, berulang; tiap kali SONIX menerima `onAudioFocusChange(-2)`, ExoPlayer menjeda otomatis dan melanjutkan saat fokus
 * kembali. Di log aplikasi jedanya hanya tampak sebagai "Antrean disimpan (jeda)" tanpa sebab, sehingga pelaku baru ketahuan
 * lewat dumpsys. Kelas ini menghitung jeda-otomatis-karena-fokus (jumlah, total, terlama) supaya sebabnya tertulis di log aplikasi.
 *
 * Alur: [onFocusPause] saat `playWhenReady` jadi false dengan alasan fokus hilang; [onResume] saat `playWhenReady` jadi true.
 * Jeda yang dilanjutkan OLEH pemulihan fokus dihitung; jeda yang berakhir karena pengguna menekan putar (fokus hilang permanen,
 * mis. aplikasi video) tidak dihitung.
 */
internal class FocusPauseTally {
    private var pausedAt = NONE
    private var count = 0
    private var totalMs = 0L
    private var maxMs = 0L

    /** Catat awal jeda karena fokus hilang; pemanggilan ganda tidak menggeser awal jeda yang sedang berjalan. */
    @Synchronized
    fun onFocusPause(nowMs: Long) {
        if (pausedAt == NONE) pausedAt = nowMs
    }

    /**
     * Tutup jeda yang sedang berjalan. Mengembalikan lama jeda (ms) bila [byFocusRegain] (dilanjutkan otomatis karena fokus kembali),
     * atau null bila tak ada jeda fokus yang terbuka / dilanjutkan oleh sebab lain.
     */
    @Synchronized
    fun onResume(nowMs: Long, byFocusRegain: Boolean): Long? {
        val start = pausedAt
        pausedAt = NONE
        if (start == NONE || !byFocusRegain) return null
        val duration = (nowMs - start).coerceAtLeast(0L)
        count++
        totalMs += duration
        if (duration > maxMs) maxMs = duration
        return duration
    }

    /** Ringkasan kumulatif untuk log. */
    @Synchronized
    fun summary(): String = "jeda_fokus=$count total=${totalMs / MS_PER_SECOND}dtk maks=${maxMs / MS_PER_SECOND}dtk"

    private companion object {
        const val NONE = -1L
        const val MS_PER_SECOND = 1_000L
    }
}
