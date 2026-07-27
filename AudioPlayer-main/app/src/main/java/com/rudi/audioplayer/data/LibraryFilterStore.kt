package com.rudi.audioplayer.data

import android.content.Context

/**
 * Lets the user scope their library to specific folders (e.g. excluding
 * WhatsApp's audio folder) and hide individual songs from view. Nothing here
 * ever touches the actual files on disk — it only filters what MusicRepository
 * returns to the UI.
 */
class LibraryFilterStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getExcludedFolders(): Set<String> =
        prefs.getStringSet(KEY_EXCLUDED_FOLDERS, emptySet()) ?: emptySet()

    fun setFolderExcluded(folderPath: String, excluded: Boolean) {
        val current = getExcludedFolders().toMutableSet()
        if (excluded) current.add(folderPath) else current.remove(folderPath)
        prefs.edit().putStringSet(KEY_EXCLUDED_FOLDERS, current).apply()
    }

    fun getHiddenSongIds(): Set<Long> =
        (prefs.getStringSet(KEY_HIDDEN_SONGS, emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    fun setSongHidden(songId: Long, hidden: Boolean) {
        val current = getHiddenSongIds().map { it.toString() }.toMutableSet()
        if (hidden) current.add(songId.toString()) else current.remove(songId.toString())
        prefs.edit().putStringSet(KEY_HIDDEN_SONGS, current).apply()
    }

    /** Applies the current exclusion rules to a freshly scanned song list. */
    fun apply(songs: List<Song>): List<Song> {
        val excludedFolders = getExcludedFolders()
        val hiddenIds = getHiddenSongIds()
        if (excludedFolders.isEmpty() && hiddenIds.isEmpty()) return songs
        return songs.filter { it.folderPath !in excludedFolders && it.id !in hiddenIds }
    }

    companion object {
        private const val PREFS_NAME = "library_filter"
        private const val KEY_EXCLUDED_FOLDERS = "excluded_folders"
        private const val KEY_HIDDEN_SONGS = "hidden_songs"
    }
}
