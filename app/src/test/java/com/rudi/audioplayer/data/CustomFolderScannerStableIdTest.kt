package com.rudi.audioplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gap List #3/#5 — SAF song identity must not be a weak 32-bit hash. Covers the
 * FNV-1a 64-bit replacement in [CustomFolderScanner.stableId]. Pure function, no
 * Context/Uri needed, so this runs without Robolectric like the rest of app/src/test. */
class CustomFolderScannerStableIdTest {

    @Test
    fun `same uri string always produces the same id`() {
        val uri = "content://com.android.externalstorage.documents/tree/primary%3AMusic/song.mp3"
        assertEquals(CustomFolderScanner.stableId(uri), CustomFolderScanner.stableId(uri))
    }

    @Test
    fun `different uri strings produce different ids`() {
        val a = CustomFolderScanner.stableId("content://.../Music/song_a.mp3")
        val b = CustomFolderScanner.stableId("content://.../Music/song_b.mp3")
        assertNotEquals(a, b)
    }

    @Test
    fun `id is always negative, never collides with a MediaStore non-negative id`() {
        val samples = listOf(
            "content://.../Music/a.mp3",
            "content://.../Podcast/b.flac",
            "",
            "content://.../über/日本語/файл.wav"
        )
        samples.forEach { assertTrue("id for '$it' must be < 0", CustomFolderScanner.stableId(it) < 0L) }
    }

    @Test
    fun `near-identical uri strings do not collide (basic avalanche sanity check)`() {
        val ids = (1..500).map { CustomFolderScanner.stableId("content://.../Music/track_$it.mp3") }
        assertEquals("500 distinct near-identical URIs must map to 500 distinct ids", 500, ids.toSet().size)
    }
}
