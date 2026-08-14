package com.rudi.audioplayer.data

import android.content.Context
import com.rudi.audioplayer.util.AppLogger
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persists position bookmarks per song ID as a JSON array in SharedPreferences — same
 * storage shape/pattern as [SmartPlaylistStore] (JSON array, parse-with-fallback), keyed
 * per-song like [LyricsStore] (`KEY_PREFIX + songId`, one entry per song touched).
 */
class BookmarkStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBookmarks(songId: Long): List<Bookmark> {
        val raw = prefs.getString(KEY_PREFIX + songId, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i -> parse(array.getJSONObject(i)) }
                .sortedBy { it.positionMs }
        } catch (e: Exception) {
            // Same tradeoff as SmartPlaylistStore: an unparsable record shows as "belum ada
            // bookmark" rather than crashing the sheet, logged locally so it's traceable.
            AppLogger.e("BookmarkStore", "Gagal parse bookmark tersimpan untuk lagu $songId", e)
            emptyList()
        }
    }

    private fun parse(obj: JSONObject): Bookmark = Bookmark(
        id = obj.getString("id"),
        label = obj.getString("label"),
        positionMs = obj.getLong("positionMs")
    )

    private fun save(songId: Long, bookmarks: List<Bookmark>) {
        if (bookmarks.isEmpty()) {
            prefs.edit().remove(KEY_PREFIX + songId).apply()
            return
        }
        val array = JSONArray()
        bookmarks.forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("label", b.label)
            obj.put("positionMs", b.positionMs)
            array.put(obj)
        }
        prefs.edit().putString(KEY_PREFIX + songId, array.toString()).apply()
    }

    fun addBookmark(songId: Long, label: String, positionMs: Long): Bookmark {
        val bookmark = Bookmark(id = UUID.randomUUID().toString(), label = label, positionMs = positionMs)
        save(songId, getBookmarks(songId) + bookmark)
        return bookmark
    }

    fun deleteBookmark(songId: Long, bookmarkId: String) {
        save(songId, getBookmarks(songId).filterNot { it.id == bookmarkId })
    }

    companion object {
        private const val PREFS_NAME = "bookmarks"
        private const val KEY_PREFIX = "bookmarks_"
    }
}
