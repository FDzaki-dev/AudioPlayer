package com.rudi.audioplayer.data

import android.content.Context

/**
 * Stores user-provided lyrics text per song ID. Accepts plain text or
 * LRC-style synced text (lines starting with [mm:ss.xx]).
 */
class LyricsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLyrics(songId: Long): String? = prefs.getString(KEY_PREFIX + songId, null)

    fun setLyrics(songId: Long, text: String) {
        prefs.edit().putString(KEY_PREFIX + songId, text).apply()
    }

    fun deleteLyrics(songId: Long) {
        prefs.edit().remove(KEY_PREFIX + songId).apply()
    }

    companion object {
        private const val PREFS_NAME = "lyrics"
        private const val KEY_PREFIX = "lyrics_"
    }
}
