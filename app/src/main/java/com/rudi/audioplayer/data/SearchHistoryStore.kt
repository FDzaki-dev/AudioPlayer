package com.rudi.audioplayer.data

import android.content.Context
import com.rudi.audioplayer.util.AppLogger
import org.json.JSONArray

/** Keeps the user's most recent successful searches so the search screen isn't a blank box when opened. */
class SearchHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHistory(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            // Riwayat pencarian yang rusak bukan hal fatal (fallback ke kosong sudah aman),
            // tapi tetap dicatat — tanpa ini kerusakan datanya tidak pernah kelihatan sama sekali.
            AppLogger.e("SearchHistoryStore", "Gagal parse riwayat pencarian", e)
            emptyList()
        }
    }

    /** Moves [query] to the front of the history, deduplicating and capping at [MAX_ITEMS]. */
    fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val updated = listOf(trimmed) + getHistory().filterNot { it.equals(trimmed, ignoreCase = true) }
        save(updated.take(MAX_ITEMS))
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun save(history: List<String>) {
        prefs.edit().putString(KEY_HISTORY, JSONArray(history).toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "search_history"
        private const val KEY_HISTORY = "recent_queries"
        private const val MAX_ITEMS = 8
    }
}
