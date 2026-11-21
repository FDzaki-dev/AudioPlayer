package com.rudi.audioplayer.data

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock

class LibrarySortTest {

    // Sama alasan dengan SmartPlaylistEngineTest: Uri.parse() null di bawah isReturnDefaultValues.
    private fun song(
        id: Long,
        title: String = "T$id",
        artist: String = "Artis",
        album: String = "Album",
        durationMs: Long = 1000L,
        dateAdded: Long = 0L,
        track: Int? = null,
        disc: Int? = null,
        albumArtist: String? = null
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = 1L,
        duration = durationMs,
        dateAdded = dateAdded,
        uri = mock(Uri::class.java),
        folderName = "Musik",
        folderPath = "/Musik",
        albumArtist = albumArtist,
        trackNumber = track,
        discNumber = disc
    )

    private fun ids(songs: List<Song>) = songs.map { it.id }

    @Test
    fun `bawaan mengembalikan daftar yang sama tanpa diubah`() {
        val list = listOf(song(3, "c"), song(1, "a"), song(2, "b"))

        assertSame(list, LibrarySort.sortSongs(list, SongSort.DEFAULT))
    }

    @Test
    fun `judul urut abjad tanpa peduli huruf besar dan judul kosong paling akhir`() {
        val list = listOf(song(1, "banana"), song(2, ""), song(3, "Apple"), song(4, "cherry"))

        assertEquals(listOf(3L, 1L, 4L, 2L), ids(LibrarySort.sortSongs(list, SongSort.TITLE)))
    }

    @Test
    fun `artis lalu album lalu nomor track`() {
        val list = listOf(
            song(1, artist = "B", album = "X", track = 1),
            song(2, artist = "A", album = "Z", track = 2),
            song(3, artist = "A", album = "Z", track = 1),
            song(4, artist = "A", album = "Y", track = null)
        )

        assertEquals(listOf(4L, 3L, 2L, 1L), ids(LibrarySort.sortSongs(list, SongSort.ARTIST)))
    }

    @Test
    fun `album urut abjad lalu cakram lalu track lalu judul`() {
        val list = listOf(
            song(1, album = "B", disc = 1, track = 1),
            song(2, album = "A", disc = 2, track = 1),
            song(3, album = "A", disc = 1, track = 2),
            song(4, album = "A", disc = 1, track = 1)
        )

        assertEquals(listOf(4L, 3L, 2L, 1L), ids(LibrarySort.sortSongs(list, SongSort.ALBUM)))
    }

    @Test
    fun `terbaru ditambahkan dan terpanjang menurun`() {
        val list = listOf(
            song(1, dateAdded = 10L, durationMs = 100L),
            song(2, dateAdded = 30L, durationMs = 300L),
            song(3, dateAdded = 20L, durationMs = 200L)
        )

        assertEquals(listOf(2L, 3L, 1L), ids(LibrarySort.sortSongs(list, SongSort.DATE_ADDED)))
        assertEquals(listOf(2L, 3L, 1L), ids(LibrarySort.sortSongs(list, SongSort.DURATION)))
    }

    @Test
    fun `nilai tersimpan yang tak dikenal kembali ke bawaan`() {
        assertEquals(SongSort.DEFAULT, SongSort.fromName(null))
        assertEquals(SongSort.DEFAULT, SongSort.fromName("RUSAK"))
        assertEquals(SongSort.DURATION, SongSort.fromName("DURATION"))
        assertEquals(AlbumSort.NAME, AlbumSort.fromName("RUSAK"))
        assertEquals(AlbumSort.RECENT, AlbumSort.fromName("RECENT"))
    }

    private val grouped = mapOf(
        "beta" to listOf(song(1, artist = "Zed", dateAdded = 5L), song(2, artist = "Zed", dateAdded = 50L)),
        "Alpha" to listOf(song(3, artist = "Mid", dateAdded = 40L)),
        "gamma" to listOf(song(4, artist = "Abe", dateAdded = 1L, albumArtist = "Zzz"))
    )

    @Test
    fun `album nama urut tanpa peduli huruf besar`() {
        assertEquals(listOf("Alpha", "beta", "gamma"), LibrarySort.sortAlbumKeys(grouped, AlbumSort.NAME))
    }

    @Test
    fun `album artis memakai artis album bila ada`() {
        // gamma: albumArtist "Zzz" (bukan artis lagu "Abe"); beta: Zed; Alpha: Mid
        assertEquals(listOf("Alpha", "beta", "gamma"), LibrarySort.sortAlbumKeys(grouped, AlbumSort.ARTIST))
    }

    @Test
    fun `album terbaru memakai tanggal tambah terbaru di dalam album`() {
        assertEquals(listOf("beta", "Alpha", "gamma"), LibrarySort.sortAlbumKeys(grouped, AlbumSort.RECENT))
    }

    @Test
    fun `album terbanyak lagu menurun lalu nama`() {
        assertEquals(listOf("beta", "Alpha", "gamma"), LibrarySort.sortAlbumKeys(grouped, AlbumSort.SONG_COUNT))
    }
}
