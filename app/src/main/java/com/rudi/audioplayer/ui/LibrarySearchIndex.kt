package com.rudi.audioplayer.ui

import com.rudi.audioplayer.data.Song

/**
 * Small in-memory search index for the currently visible library.
 *
 * The expensive lowercase normalization is performed once when the visible song list changes,
 * rather than once for every song on every keystroke.
 */
class LibrarySearchIndex(songs: List<Song>) {
    private data class Entry(
        val song: Song,
        val title: String,
        val artist: String
    )

    private val entries = songs.map { song ->
        Entry(
            song = song,
            title = song.title.lowercase(),
            artist = song.artist.lowercase()
        )
    }

    fun search(query: String): List<Song> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return entries.map { it.song }
        return entries.asSequence()
            .filter { it.title.contains(normalized) || it.artist.contains(normalized) }
            .map { it.song }
            .toList()
    }
}
