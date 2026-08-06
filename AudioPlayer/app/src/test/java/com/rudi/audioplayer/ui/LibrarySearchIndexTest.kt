package com.rudi.audioplayer.ui

import android.net.Uri
import com.rudi.audioplayer.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class LibrarySearchIndexTest {

    // Uri.parse(...) returns null here (plain-JVM test, android.jar stub with
    // isReturnDefaultValues = true) — NOT a harmless placeholder, it throws a
    // NullPointerException the moment it's assigned to Song's non-null `uri: Uri` field. A
    // mock is used instead purely so each song gets *some* distinct, non-throwing Uri
    // instance — nothing in these tests reads the Uri's actual content.
    private fun song(id: Long, title: String, artist: String) = Song(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        albumId = 1L,
        duration = 200_000L,
        dateAdded = 0L,
        uri = mock(Uri::class.java),
        folderName = "Music",
        folderPath = "/Music"
    )

    @Test
    fun `blank query returns every song unfiltered`() {
        val songs = listOf(song(1, "A", "X"), song(2, "B", "Y"))
        val index = LibrarySearchIndex(songs)

        assertEquals(songs, index.search(""))
        assertEquals(songs, index.search("   "))
    }

    @Test
    fun `matches by title regardless of case`() {
        val songs = listOf(song(1, "Bohemian Rhapsody", "Queen"), song(2, "Yesterday", "The Beatles"))
        val index = LibrarySearchIndex(songs)

        val result = index.search("RHAPSODY")

        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    @Test
    fun `matches by artist as well as title`() {
        val songs = listOf(song(1, "Bohemian Rhapsody", "Queen"), song(2, "Yesterday", "The Beatles"))
        val index = LibrarySearchIndex(songs)

        val result = index.search("beatles")

        assertEquals(1, result.size)
        assertEquals(2L, result.first().id)
    }

    @Test
    fun `query never matches across title and artist boundary`() {
        // "rhapsodyqueen" (title tail + artist head glued together) must not match — the
        // index joins them with a NUL separator specifically to prevent this kind of
        // false-positive substring match.
        val songs = listOf(song(1, "Rhapsody", "Queen"))
        val index = LibrarySearchIndex(songs)

        assertTrue(index.search("rhapsodyqueen").isEmpty())
    }

    @Test
    fun `no match returns an empty list`() {
        val songs = listOf(song(1, "Bohemian Rhapsody", "Queen"))
        val index = LibrarySearchIndex(songs)

        assertTrue(index.search("nonexistent").isEmpty())
    }
}
