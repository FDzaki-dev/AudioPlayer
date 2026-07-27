package com.rudi.audioplayer.data

import android.content.Context

data class SavedPlaybackState(
    val songIds: List<Long>,
    val index: Int,
    val positionMs: Long
)

/**
 * Persists the last playback queue + position so listening can resume
 * exactly where the user left off, even after the app process was killed.
 */
class PlaybackStateStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(songIds: List<Long>, index: Int, positionMs: Long) {
        prefs.edit()
            .putString(KEY_IDS, songIds.joinToString(","))
            .putInt(KEY_INDEX, index)
            .putLong(KEY_POSITION, positionMs)
            .apply()
    }

    fun load(): SavedPlaybackState? {
        val idsRaw = prefs.getString(KEY_IDS, null) ?: return null
        val ids = idsRaw.split(",").mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) return null
        val index = prefs.getInt(KEY_INDEX, 0).coerceIn(0, ids.size - 1)
        val position = prefs.getLong(KEY_POSITION, 0L)
        return SavedPlaybackState(ids, index, position)
    }

    companion object {
        private const val PREFS_NAME = "playback_state"
        private const val KEY_IDS = "song_ids"
        private const val KEY_INDEX = "index"
        private const val KEY_POSITION = "position"
    }
}
