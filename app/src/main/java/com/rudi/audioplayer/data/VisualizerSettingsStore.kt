package com.rudi.audioplayer.data

import android.content.Context

/** On/off preference for the audio visualizer (Roadmap #9, ROADMAP_15_FITUR_OFFLINE.md),
 * remembered across sessions. Off by default — same reasoning as [ShakeSettingsStore]: a feature
 * that needs the sensitive RECORD_AUDIO permission (see AndroidManifest.xml /
 * AudioVisualizerController.kt for why a purely-local visual effect needs a microphone-sounding
 * permission) should always be something the user deliberately opts into, never implied. */
class VisualizerSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "visualizer_settings"
        private const val KEY_ENABLED = "enabled"
    }
}
