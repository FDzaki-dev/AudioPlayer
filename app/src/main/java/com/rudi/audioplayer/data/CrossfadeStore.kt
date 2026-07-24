package com.rudi.audioplayer.data

import android.content.Context

/** Simple on/off flag for the fade-transition feature, remembered across sessions. */
class CrossfadeStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Gapless (no fade) is the default — an honest, unaltered playback experience out of the
    // box. Fade is an opt-in stylistic choice, not something imposed on new installs.
    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "crossfade"
        private const val KEY_ENABLED = "enabled"
    }
}
