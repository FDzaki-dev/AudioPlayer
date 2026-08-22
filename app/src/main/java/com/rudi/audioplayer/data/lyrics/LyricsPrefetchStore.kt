package com.rudi.audioplayer.data.lyrics

import android.content.Context

/**
 * Batch 247 — Lyrics offline-first 4/4b. On/off preference: prefetch lirik lagu depan queue
 * otomatis saat WiFi ([com.rudi.audioplayer.worker.LyricsPrefetchWorker], `NetworkType.UNMETERED`).
 *
 * Default ON — SENGAJA beda dari [com.rudi.audioplayer.data.ShakeSettingsStore]/
 * [com.rudi.audioplayer.data.FloatingBubbleStore]/[com.rudi.audioplayer.data.SilenceSkipStore]
 * (semua default OFF, karena masing-masing mengubah PERILAKU pemutaran/tampilan yang user
 * rasakan langsung — roadmap fitur itu eksplisit minta opt-in, bukan default diam-diam berubah).
 * Fitur ini beda kelas risiko: murni background caching WiFi-only, 0 dampak ke pemutaran atau
 * kuota data seluler kalau user memang tidak pernah nyambung WiFi — konsekuensi ON diam-diam
 * jauh lebih ringan (boros sedikit request WiFi) dibanding OFF diam-diam (fitur lirik offline
 * jadi terasa lambat/gagal terus tanpa user sadar kenapa).
 */
class LyricsPrefetchStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "lyrics_prefetch_settings"
        private const val KEY_ENABLED = "enabled"
    }
}
