package com.rudi.audioplayer.ui

import com.rudi.audioplayer.data.Song
import java.util.Locale

/**
 * Small in-memory search index for the currently visible library.
 *
 * Searchable text is normalized once when the visible song list changes,
 * rather than once for every song on every keystroke. Supports tokenized
 * multi-word queries across title, artist, and album fields.
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
        val album = song.album.lowercase(Locale.ROOT)
        
        Entry(
            song = song,
            searchableText = "$title $artist $album"
        )
    }

    /**
     * Searches songs matching all keywords in the query, regardless of order.
     * Example: "cold yellow" will match "Yellow" by "Coldplay".
     */
    fun search(query: String): List<Song> {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return allSongs

        // Pecah kata kunci berdasarkan spasi agar pencarian multi-kata lebih fleksibel
        val tokens = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return allSongs

        return entries.asSequence()
            .filter { entry ->
                // Semua token kata kunci harus ditemukan dalam searchableText lagu
                tokens.all { token -> entry.searchableText.contains(token) }
            }
            .map { it.song }
            .toList()
    }
}
