package com.rudi.audioplayer.data

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Records which songs were played on which calendar day, so "Kilas Balik" can surface
 * what was being listened to on this same date in a previous month/year. Pruned to the
 * last 2 years on write to keep storage bounded. */
class ListeningHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun recordPlay(songId: Long) {
        val key = dateFormatter.format(LocalDate.now())
        val existing = prefs.getStringSet(key, emptySet()) ?: emptySet()
        prefs.edit().putStringSet(key, existing + songId.toString()).apply()
        pruneOldEntries()
    }

    /** Song IDs played on the given exact calendar date, empty if none. */
    fun getSongIdsForDate(date: LocalDate): List<Long> {
        val key = dateFormatter.format(date)
        return (prefs.getStringSet(key, emptySet()) ?: emptySet()).mapNotNull { it.toLongOrNull() }
    }

    private fun pruneOldEntries() {
        val cutoff = LocalDate.now().minusYears(2)
        val editor = prefs.edit()
        var changed = false
        for (key in prefs.all.keys) {
            val date = try { LocalDate.parse(key, dateFormatter) } catch (e: Exception) { null }
            if (date != null && date.isBefore(cutoff)) {
                editor.remove(key)
                changed = true
            }
        }
        if (changed) editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "listening_history"
    }
}
