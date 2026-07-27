package com.rudi.audioplayer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persists user-created playlists (name + ordered song IDs) as a JSON array
 * in SharedPreferences. No database needed for this small amount of data.
 */
class PlaylistStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPlaylists(): List<Playlist> {
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val idsArray = obj.getJSONArray("songIds")
                val ids = (0 until idsArray.length()).map { idsArray.getLong(it) }
                Playlist(id = obj.getString("id"), name = obj.getString("name"), songIds = ids)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save(playlists: List<Playlist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            val obj = JSONObject()
            obj.put("id", playlist.id)
            obj.put("name", playlist.name)
            obj.put("songIds", JSONArray(playlist.songIds))
            array.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, array.toString()).apply()
    }

    fun createPlaylist(name: String): Playlist {
        val playlist = Playlist(id = UUID.randomUUID().toString(), name = name)
        save(getPlaylists() + playlist)
        return playlist
    }

    fun deletePlaylist(id: String) {
        save(getPlaylists().filterNot { it.id == id })
    }

    fun renamePlaylist(id: String, newName: String) {
        save(getPlaylists().map { if (it.id == id) it.copy(name = newName) else it })
    }

    /** Adds a song to the playlist if it isn't already there. Returns false if it was already present. */
    fun addSong(playlistId: String, songId: Long): Boolean {
        var added = false
        save(
            getPlaylists().map { playlist ->
                if (playlist.id == playlistId && !playlist.songIds.contains(songId)) {
                    added = true
                    playlist.copy(songIds = playlist.songIds + songId)
                } else {
                    playlist
                }
            }
        )
        return added
    }

    fun removeSong(playlistId: String, songId: Long) {
        save(
            getPlaylists().map { playlist ->
                if (playlist.id == playlistId) playlist.copy(songIds = playlist.songIds - songId) else playlist
            }
        )
    }

    fun moveSong(playlistId: String, from: Int, to: Int) {
        save(
            getPlaylists().map { playlist ->
                if (playlist.id != playlistId) return@map playlist
                if (from !in playlist.songIds.indices || to !in playlist.songIds.indices) return@map playlist
                val reordered = playlist.songIds.toMutableList().apply { add(to, removeAt(from)) }
                playlist.copy(songIds = reordered)
            }
        )
    }

    companion object {
        private const val PREFS_NAME = "playlists"
        private const val KEY_PLAYLISTS = "playlists_json"
    }
}
