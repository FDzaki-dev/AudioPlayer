package com.rudi.audioplayer.data

import android.content.Context

/** On/off preference for "radio mode" — auto-continuing playback with more songs from the
 * library once the queue plays out with repeat off. On by default since that's the existing
 * behavior users already have; this store exists purely to let anyone who prefers the music
 * to actually stop when their queue/playlist ends turn it off, instead of being surprised by
 * random songs starting on their own. */
class RadioSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "radio_settings"
        private const val KEY_ENABLED = "enabled"
    }
}
