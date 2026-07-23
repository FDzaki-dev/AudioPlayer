package com.rudi.audioplayer.data

import android.content.Context

/**
 * Tracks how often and how recently each song has been played, backed by
 * SharedPreferences. Powers the "Baru Diputar" (recently played) and
 * "Paling Sering Diputar" (most played) sections on the Home screen.
 */
class PlayStatsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Call whenever a song actually starts playing. */
    fun recordPlay(songId: Long) {
        val countKey = COUNT_PREFIX + songId
        val lastKey = LAST_PREFIX + songId
        val newCount = prefs.getInt(countKey, 0) + 1
        prefs.edit()
            .putInt(countKey, newCount)
            .putLong(lastKey, System.currentTimeMillis())
            .apply()
    }

    /** Song IDs ordered by most recently played first. */
    fun getRecentIds(limit: Int = 20): List<Long> =
        prefs.all.entries
            .filter { it.key.startsWith(LAST_PREFIX) }
            .mapNotNull { (key, value) ->
                val id = key.removePrefix(LAST_PREFIX).toLongOrNull()
                val timestamp = value as? Long
                if (id != null && timestamp != null) id to timestamp else null
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

    /** Song IDs ordered by highest play count first. */
    fun getMostPlayedIds(limit: Int = 20): List<Long> =
        prefs.all.entries
            .filter { it.key.startsWith(COUNT_PREFIX) }
            .mapNotNull { (key, value) ->
                val id = key.removePrefix(COUNT_PREFIX).toLongOrNull()
                val count = value as? Int
                if (id != null && count != null && count > 0) id to count else null
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

    /** Sum of every song's play count — the app's all-time "songs played" tally,
     * used to celebrate small listening milestones. */
    fun totalPlayCount(): Int =
        prefs.all.entries
            .filter { it.key.startsWith(COUNT_PREFIX) }
            .sumOf { (it.value as? Int) ?: 0 }

    companion object {
        private const val PREFS_NAME = "play_stats"
        private const val COUNT_PREFIX = "count_"
        private const val LAST_PREFIX = "last_"
    }
}
