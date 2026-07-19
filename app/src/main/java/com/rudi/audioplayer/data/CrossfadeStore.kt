package com.rudi.audioplayer.data

import android.content.Context

/** Simple on/off flag for the fade-transition feature, remembered across sessions. */
class CrossfadeStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "crossfade"
        private const val KEY_ENABLED = "enabled"
    }
}
