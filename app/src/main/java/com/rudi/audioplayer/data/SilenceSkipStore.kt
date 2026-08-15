package com.rudi.audioplayer.data

import android.content.Context

/**
 * On/off preference for automatic silence skipping during playback (Roadmap #8). Off by
 * default — the roadmap's own risk note is explicit: a wrong/too-aggressive threshold can chew
 * through a musically-intentional quiet intro/outro, not just genuinely dead technical silence,
 * so this must be something the user opts into deliberately, never a silent default behavior
 * change to how every song plays. Same reasoning as [ShakeSettingsStore]/[FloatingBubbleStore].
 */
class SilenceSkipStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "silence_skip_settings"
        private const val KEY_ENABLED = "enabled"
    }
}
