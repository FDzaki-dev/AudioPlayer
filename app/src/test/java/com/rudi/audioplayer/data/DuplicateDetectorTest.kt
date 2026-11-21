package com.rudi.audioplayer.data

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

/** Sektor Testing (Batch 417) — [DuplicateDetector] adalah pure/stateless (0 Context, 0 I/O)
 * dan KDoc-nya sendiri eksplisit bilang "trivially unit-testable on its own" (lihat file
 * tsb), tapi belum pernah ada test file utk class ini sejak dibuat (Gap List #2). Fixture
 * `song()` sama persis pola [SmartPlaylistEngineTest]/[LibrarySearchIndexTest]: `mock(Uri::
 * class.java)` krn `Uri.parse(...)` null di JVM murni (lihat insiden Batch 27 revisi 2). */
class DuplicateDetectorTest {

    private fun song(
        id: Long = 1L,
        title: String = "Title",
        artist: String = "Artist",
        durationMs: Long = 200_000L,
        fileSize: Long = 0L
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        albumId = 1L,
        duration = durationMs,
        dateAdded = 0L,
        uri = mock(Uri::class.java),
        folderName = "Musik",
        folderPath = "/Musik",
        fileSize = fileSize
    )

    // --- findLibraryDuplicates (signature: title+artist trim/lowercase + duration/1000) ---

    @Test
    fun `identical title and artist with same duration bucket groups as library duplicate`() {
        val a = song(id = 1L, title = "Lagu", artist = "Artis", durationMs = 200_000L)
        val b = song(id = 2L, title = "Lagu", artist = "Artis", durationMs = 200_400L)
        val groups = DuplicateDetector.findLibraryDuplicates(listOf(a, b))
        assertEquals(1, groups.size)
        assertEquals(DuplicateDetector.Reason.SAME_LIBRARY_ENTRY, groups[0].reason)
        assertEquals(setOf(1L, 2L), groups[0].songs.map { it.id }.toSet())
    }

    @Test
    fun `title and artist comparison is case-insensitive and trims whitespace`() {
        val a = song(id = 1L, title = "  Lagu  ", artist = "ARTIS")
        val b = song(id = 2L, title = "lagu", artist = "  artis")
        val groups = DuplicateDetector.findLibraryDuplicates(listOf(a, b))
        assertEquals(1, groups.size)
        assertEquals(2, groups[0].songs.size)
    }

    @Test
    fun `duration bucket boundary at full second does not merge`() {
        // 200_000ms -> bucket 200; 200_999ms -> bucket 200 (masih sama, truncation).
        // 201_000ms -> bucket 201 (beda), TIDAK boleh ikut grup.
        val a = song(id = 1L, durationMs = 200_999L)
        val b = song(id = 2L, durationMs = 201_000L)
        val groups = DuplicateDetector.findLibraryDuplicates(listOf(a, b))
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `different title never groups regardless of artist or duration`() {
        val a = song(id = 1L, title = "Lagu A")
        val b = song(id = 2L, title = "Lagu B")
        assertTrue(DuplicateDetector.findLibraryDuplicates(listOf(a, b)).isEmpty())
    }

    @Test
    fun `singleton entries are excluded, only groups of 2 or more returned`() {
        val a = song(id = 1L, title = "Unik A")
        val b = song(id = 2L, title = "Unik B")
        val c = song(id = 3L, title = "Unik C")
        assertTrue(DuplicateDetector.findLibraryDuplicates(listOf(a, b, c)).isEmpty())
    }

    @Test
    fun `results sorted descending by group size`() {
        val groupOf3 = listOf(
            song(id = 1L, title = "Trio"),
            song(id = 2L, title = "Trio"),
            song(id = 3L, title = "Trio")
        )
        val groupOf2 = listOf(
            song(id = 4L, title = "Duo"),
            song(id = 5L, title = "Duo")
        )
        // Sengaja dikirim urut duo-dulu supaya test ini benar2 menguji sorting, bukan
        // kebetulan ikut urutan input.
        val groups = DuplicateDetector.findLibraryDuplicates(groupOf2 + groupOf3)
        assertEquals(2, groups.size)
        assertEquals(3, groups[0].songs.size)
        assertEquals(2, groups[1].songs.size)
    }

    // --- findPhysicalDuplicates (signature: fileSize + duration/1000, fileSize<=0 excluded) ---

    @Test
    fun `same file size and duration bucket groups as physical duplicate`() {
        val a = song(id = 1L, title = "A", artist = "X", durationMs = 180_000L, fileSize = 5_000_000L)
        val b = song(id = 2L, title = "B beda total", artist = "Y beda", durationMs = 180_500L, fileSize = 5_000_000L)
        val groups = DuplicateDetector.findPhysicalDuplicates(listOf(a, b))
        assertEquals(1, groups.size)
        assertEquals(DuplicateDetector.Reason.SAME_PHYSICAL_FILE, groups[0].reason)
    }

    @Test
    fun `songs with unknown file size (0 or negative) are excluded from physical grouping`() {
        val a = song(id = 1L, durationMs = 180_000L, fileSize = 0L)
        val b = song(id = 2L, durationMs = 180_000L, fileSize = -1L)
        val c = song(id = 3L, durationMs = 180_000L, fileSize = 0L)
        assertTrue(DuplicateDetector.findPhysicalDuplicates(listOf(a, b, c)).isEmpty())
    }

    @Test
    fun `different file size never groups even with identical duration`() {
        val a = song(id = 1L, durationMs = 180_000L, fileSize = 1_000_000L)
        val b = song(id = 2L, durationMs = 180_000L, fileSize = 2_000_000L)
        assertTrue(DuplicateDetector.findPhysicalDuplicates(listOf(a, b)).isEmpty())
    }

    @Test
    fun `library and physical grouping are independent — different titles can still be physical duplicates`() {
        // Kasus nyata fitur ini: 2 file BEDA lagu (title/artist beda total) tapi size+duration
        // sama persis (mis. korup/silent placeholder) tetap harus lolos physical grouping,
        // walau tidak akan pernah lolos library grouping.
        val a = song(id = 1L, title = "Judul Berbeda 1", artist = "Artis 1", durationMs = 90_000L, fileSize = 3_500_000L)
        val b = song(id = 2L, title = "Judul Berbeda 2", artist = "Artis 2", durationMs = 90_000L, fileSize = 3_500_000L)
        assertTrue(DuplicateDetector.findLibraryDuplicates(listOf(a, b)).isEmpty())
        assertEquals(1, DuplicateDetector.findPhysicalDuplicates(listOf(a, b)).size)
    }
}
