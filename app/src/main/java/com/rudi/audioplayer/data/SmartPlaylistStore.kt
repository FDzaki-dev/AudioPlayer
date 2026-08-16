package com.rudi.audioplayer.data

import android.content.Context
import com.rudi.audioplayer.util.AppLogger
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persists smart playlist criteria (never song lists — those are resolved live by
 * [SmartPlaylistEngine] every time) as a JSON array in SharedPreferences. Same storage
 * shape/pattern as [PlaylistStore], just a different record shape.
 */
class SmartPlaylistStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSmartPlaylists(): List<SmartPlaylist> {
        val raw = prefs.getString(KEY_SMART_PLAYLISTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i -> parse(array.getJSONObject(i)) }
        } catch (e: Exception) {
            // Falling back to an empty list here means every saved smart playlist appears to
            // vanish from the UI — same tradeoff PlaylistStore makes, worth logging rather than
            // failing silently so a user report of "playlist otomatis saya hilang" is traceable.
            AppLogger.e("SmartPlaylistStore", "Gagal parse data smart playlist tersimpan", e)
            emptyList()
        }
    }

    private fun parse(obj: JSONObject): SmartPlaylist {
        val folderArray = obj.optJSONArray("folderNames") ?: JSONArray()
        val folders = (0 until folderArray.length()).map { folderArray.getString(it) }.toSet()
        return SmartPlaylist(
            id = obj.getString("id"),
            name = obj.getString("name"),
            folderNames = folders,
            minDurationMs = if (obj.has("minDurationMs")) obj.getLong("minDurationMs") else null,
            maxDurationMs = if (obj.has("maxDurationMs")) obj.getLong("maxDurationMs") else null,
            minRating = obj.optInt("minRating", 0),
            minYear = if (obj.has("minYear")) obj.getInt("minYear") else null,
            maxYear = if (obj.has("maxYear")) obj.getInt("maxYear") else null,
            keyword = obj.optString("keyword", "")
        )
    }

    private fun save(playlists: List<SmartPlaylist>) {
        val array = JSONArray()
        playlists.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("folderNames", JSONArray(p.folderNames.toList()))
            p.minDurationMs?.let { obj.put("minDurationMs", it) }
            p.maxDurationMs?.let { obj.put("maxDurationMs", it) }
            obj.put("minRating", p.minRating)
            p.minYear?.let { obj.put("minYear", it) }
            p.maxYear?.let { obj.put("maxYear", it) }
            obj.put("keyword", p.keyword)
            array.put(obj)
        }
        prefs.edit().putString(KEY_SMART_PLAYLISTS, array.toString()).apply()
    }

    /** [playlist.id] is ignored and replaced with a fresh UUID — callers pass a draft with a
     *  throwaway id from the builder UI. */
    fun createSmartPlaylist(playlist: SmartPlaylist): SmartPlaylist {
        val withId = playlist.copy(id = UUID.randomUUID().toString())
        save(getSmartPlaylists() + withId)
        return withId
    }

    fun updateSmartPlaylist(playlist: SmartPlaylist) {
        save(getSmartPlaylists().map { if (it.id == playlist.id) playlist else it })
    }

    fun deleteSmartPlaylist(id: String) {
        save(getSmartPlaylists().filterNot { it.id == id })
    }

    /** Batch 101 — pasangan undo [deleteSmartPlaylist], sama polanya dgn
     *  [PlaylistStore.restorePlaylist]: simpan balik kriteria APA ADANYA (bukan lewat
     *  [createSmartPlaylist] yang sengaja generate id baru). */
    fun restoreSmartPlaylist(playlist: SmartPlaylist) {
        if (getSmartPlaylists().any { it.id == playlist.id }) return
        save(getSmartPlaylists() + playlist)
    }

    companion object {
        private const val PREFS_NAME = "smart_playlists"
        private const val KEY_SMART_PLAYLISTS = "smart_playlists_json"
    }
}
