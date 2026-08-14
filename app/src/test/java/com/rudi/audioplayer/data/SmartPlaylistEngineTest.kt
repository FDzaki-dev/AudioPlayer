package com.rudi.audioplayer.data

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class SmartPlaylistEngineTest {

    // Same reasoning as LibrarySearchIndexTest's helper: Uri.parse(...) returns null in a
    // plain-JVM unit test (no real Android runtime), which throws the moment it's assigned to
    // Song's non-null `uri` field. A mock just needs to exist, never read.
    private fun song(
        id: Long = 1L,
        title: String = "Title",
        artist: String = "Artist",
        album: String = "Album",
        durationMs: Long = 200_000L,
        folderName: String = "Musik",
        year: Int = 0
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = 1L,
        duration = durationMs,
        dateAdded = 0L,
        uri = mock(Uri::class.java),
        folderName = folderName,
        folderPath = "/$folderName",
        year = year
    )

    private val noRatings: (Long) -> Int = { 0 }

    @Test
    fun `empty criteria matches everything`() {
        val playlist = SmartPlaylist(id = "1", name = "Semua")
        assertTrue(SmartPlaylistEngine.matches(playlist, song(), noRatings))
    }

    @Test
    fun `folder filter keeps only songs in one of the listed folders`() {
        val playlist = SmartPlaylist(id = "1", name = "Musik saja", folderNames = setOf("Musik"))
        assertTrue(SmartPlaylistEngine.matches(playlist, song(folderName = "Musik"), noRatings))
        assertFalse(SmartPlaylistEngine.matches(playlist, song(folderName = "WhatsApp Audio"), noRatings))
    }

    @Test
    fun `duration range is inclusive on both ends`() {
        val playlist = SmartPlaylist(id = "1", name = "3-5 menit", minDurationMs = 180_000L, maxDurationMs = 300_000L)
        assertTrue(SmartPlaylistEngine.matches(playlist, song(durationMs = 180_000L), noRatings))
        assertTrue(SmartPlaylistEngine.matches(playlist, song(durationMs = 300_000L), noRatings))
        assertFalse(SmartPlaylistEngine.matches(playlist, song(durationMs = 179_999L), noRatings))
        assertFalse(SmartPlaylistEngine.matches(playlist, song(durationMs = 300_001L), noRatings))
    }

    @Test
    fun `min rating filter respects the stored rating lookup`() {
        val playlist = SmartPlaylist(id = "1", name = "Bintang 4+", minRating = 4)
        val ratings: (Long) -> Int = { id -> if (id == 1L) 5 else 2 }
        assertTrue(SmartPlaylistEngine.matches(playlist, song(id = 1L), ratings))
        assertFalse(SmartPlaylistEngine.matches(playlist, song(id = 2L), ratings))
    }

    @Test
    fun `unrated song never matches a min rating filter`() {
        val playlist = SmartPlaylist(id = "1", name = "Bintang 1+", minRating = 1)
        assertFalse(SmartPlaylistEngine.matches(playlist, song(), noRatings))
    }

    @Test
    fun `song with unknown year never matches a year-bounded rule`() {
        val playlist = SmartPlaylist(id = "1", name = "2015-2020", minYear = 2015, maxYear = 2020)
        assertFalse(SmartPlaylistEngine.matches(playlist, song(year = 0), noRatings))
    }

    @Test
    fun `year range is inclusive and rejects years outside it`() {
        val playlist = SmartPlaylist(id = "1", name = "2015-2020", minYear = 2015, maxYear = 2020)
        assertTrue(SmartPlaylistEngine.matches(playlist, song(year = 2015), noRatings))
        assertTrue(SmartPlaylistEngine.matches(playlist, song(year = 2020), noRatings))
        assertFalse(SmartPlaylistEngine.matches(playlist, song(year = 2014), noRatings))
        assertFalse(SmartPlaylistEngine.matches(playlist, song(year = 2021), noRatings))
    }

    @Test
    fun `keyword matches title, artist, or album case-insensitively`() {
        val playlist = SmartPlaylist(id = "1", name = "cari", keyword = "night")
        assertTrue(SmartPlaylistEngine.matches(playlist, song(title = "Good Night"), noRatings))
        assertTrue(SmartPlaylistEngine.matches(playlist, song(artist = "Nightwish"), noRatings))
        assertTrue(SmartPlaylistEngine.matches(playlist, song(album = "MIDNIGHT"), noRatings))
        assertFalse(SmartPlaylistEngine.matches(playlist, song(title = "Day", artist = "X", album = "Y"), noRatings))
    }

    @Test
    fun `blank keyword after trimming does not filter anything`() {
        val playlist = SmartPlaylist(id = "1", name = "spasi", keyword = "   ")
        assertTrue(SmartPlaylistEngine.matches(playlist, song(), noRatings))
    }

    @Test
    fun `combined criteria require all of them to pass, not just one`() {
        val playlist = SmartPlaylist(
            id = "1",
            name = "gabungan",
            folderNames = setOf("Musik"),
            minRating = 3,
            keyword = "rock"
        )
        val ratings: (Long) -> Int = { 5 }
        // Matches folder + rating but not keyword.
        assertFalse(SmartPlaylistEngine.matches(playlist, song(folderName = "Musik", title = "Pop Song"), ratings))
        // Matches all three.
        assertTrue(SmartPlaylistEngine.matches(playlist, song(folderName = "Musik", title = "Rock Anthem"), ratings))
    }

    @Test
    fun `resolve filters a full list down to only the matches`() {
        val playlist = SmartPlaylist(id = "1", name = "Musik", folderNames = setOf("Musik"))
        val songs = listOf(
            song(id = 1L, folderName = "Musik"),
            song(id = 2L, folderName = "WhatsApp Audio"),
            song(id = 3L, folderName = "Musik")
        )
        val result = SmartPlaylistEngine.resolve(playlist, songs, noRatings)
        assertEquals(listOf(1L, 3L), result.map { it.id })
    }
}
