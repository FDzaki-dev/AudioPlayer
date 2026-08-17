package com.rudi.audioplayer.ui

import com.rudi.audioplayer.data.Song
import java.util.Locale

/**
 * Small in-memory search index for the currently visible library.
 *
 * Searchable text is normalized once when the visible song list changes,
 * rather than once for every song on every keystroke.
 */
class LibrarySearchIndex(songs: List<Song>) {
    private data class Entry(
        val song: Song,
        val searchableText: String
    )

    private val allSongs = songs
    private val entries = songs.map { song ->
        val title = song.title.lowercase(Locale.ROOT)
        val artist = song.artist.lowercase(Locale.ROOT)
        // Gap List #11 — genre now included in library search, same "single blob, null-
        // separated" pattern as title/artist (empty string when a song has no genre tag,
        // never crashes/needs a null check at query time).
        val genre = song.genre?.lowercase(Locale.ROOT) ?: ""
        Entry(
            song = song,
            searchableText = "$title\u0000$artist\u0000$genre"
        )
    }

    fun search(query: String): List<Song> {
        val normalized = query.trim().lowercase(Locale.ROOT)
        if (normalized.isEmpty()) return allSongs
        return entries.asSequence()
            .filter { it.searchableText.contains(normalized) }
            .map { it.song }
            .toList()
    }
}
