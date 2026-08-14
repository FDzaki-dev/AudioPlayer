package com.rudi.audioplayer.data

/**
 * Pure matching logic behind [SmartPlaylist], deliberately separated from [SmartPlaylistStore]/
 * Context so every edge case (blank keyword, unknown year vs. a year-bounded rule, absent
 * rating) is unit-testable without constructing a [Song]'s android.net.Uri — same pattern as
 * [LibraryFilterStore.shouldKeep].
 */
object SmartPlaylistEngine {

    /**
     * @param ratingOf looks up a song's stored star rating (0 = unrated) by ID. Passed in rather
     * than reading [RatingStore] directly so this stays pure/testable.
     */
    fun matches(playlist: SmartPlaylist, song: Song, ratingOf: (Long) -> Int): Boolean {
        if (playlist.folderNames.isNotEmpty() && song.folderName !in playlist.folderNames) {
            return false
        }
        playlist.minDurationMs?.let { if (song.duration < it) return false }
        playlist.maxDurationMs?.let { if (song.duration > it) return false }
        if (playlist.minRating > 0 && ratingOf(song.id) < playlist.minRating) return false

        if (playlist.minYear != null || playlist.maxYear != null) {
            // A song with no embedded year (year == 0) can never satisfy a year-bounded rule —
            // treating the unknown 0 as if it were literally year 0 would either wrongly match
            // an impossibly-old lower bound or wrongly fail an upper bound, neither of which
            // reflects "we don't actually know this song's year".
            if (song.year <= 0) return false
            playlist.minYear?.let { if (song.year < it) return false }
            playlist.maxYear?.let { if (song.year > it) return false }
        }

        val keyword = playlist.keyword.trim()
        if (keyword.isNotEmpty()) {
            val q = keyword.lowercase()
            val hit = song.title.lowercase().contains(q) ||
                song.artist.lowercase().contains(q) ||
                song.album.lowercase().contains(q)
            if (!hit) return false
        }

        return true
    }

    fun resolve(playlist: SmartPlaylist, songs: List<Song>, ratingOf: (Long) -> Int): List<Song> =
        songs.filter { matches(playlist, it, ratingOf) }
}
