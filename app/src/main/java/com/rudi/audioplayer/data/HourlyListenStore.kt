package com.rudi.audioplayer.data

import android.content.Context
import java.util.Calendar

/**
 * Tracks how many songs have been played in each hour-of-day bucket (0-23), all-time,
 * backed by SharedPreferences. Powers "jam favorit dengar musik" on the Stats Dashboard
 * (Batch 90). Deliberately separate from `ListeningHistoryStore` (which is keyed by calendar
 * date, not hour) rather than extending it — adding an hour dimension to that store's existing
 * per-date key format would be a data-migration risk for history already saved by real users;
 * 24 flat counters here carry zero migration risk and never grow unbounded.
 */
class HourlyListenStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Call whenever a song actually starts playing — same call site as PlayStatsStore.recordPlay. */
    fun recordPlay() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val key = COUNT_PREFIX + hour
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    /** All-time play counts per hour, index 0 = 00:00-00:59 ... index 23 = 23:00-23:59. */
    fun getHourlyCounts(): IntArray =
        IntArray(24) { hour -> prefs.getInt(COUNT_PREFIX + hour, 0) }

    companion object {
        private const val PREFS_NAME = "hourly_listen_stats"
        private const val COUNT_PREFIX = "hour_"
    }
}
