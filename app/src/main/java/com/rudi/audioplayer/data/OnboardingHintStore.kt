package com.rudi.audioplayer.data

import android.content.Context

/** Tracks which one-time feature hints the user has already dismissed, so a coach-mark banner
 * for a relocated or hidden control (Now Playing's gesture zones, Library's grouped tabs) only
 * ever shows once instead of nagging on every visit. */
class OnboardingHintStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSeenNowPlayingHint(): Boolean = prefs.getBoolean(KEY_NOW_PLAYING, false)
    fun markNowPlayingHintSeen() {
        prefs.edit().putBoolean(KEY_NOW_PLAYING, true).apply()
    }

    fun hasSeenLibraryHint(): Boolean = prefs.getBoolean(KEY_LIBRARY, false)
    fun markLibraryHintSeen() {
        prefs.edit().putBoolean(KEY_LIBRARY, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "onboarding_hints"
        private const val KEY_NOW_PLAYING = "seen_now_playing_hint"
        private const val KEY_LIBRARY = "seen_library_hint"
    }
}
