package com.rudi.audioplayer.data

import android.content.Context

/** On/off preference for shake-to-skip, remembered across sessions. Off by default —
 * a physical-motion gesture should be something the user deliberately opts into. */
class ShakeSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "shake_settings"
        private const val KEY_ENABLED = "enabled"
    }
}
