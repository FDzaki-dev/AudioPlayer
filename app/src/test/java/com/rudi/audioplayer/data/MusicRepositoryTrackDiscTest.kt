package com.rudi.audioplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Gap List #4 (Metadata model diperkuat) — track/disc number parsing. Pure functions, no
 * Context/cursor needed, so this runs without Robolectric like the rest of app/src/test. */
class MusicRepositoryTrackDiscTest {

    // --- parseTrackOrDiscString (API 30+ CD_TRACK_NUMBER / DISC_NUMBER, both string cols) ---

    @Test
    fun `bare number string parses directly`() {
        assertEquals(5, MusicRepository.parseTrackOrDiscString("5"))
    }

    @Test
    fun `N over M form takes only the leading number`() {
        assertEquals(5, MusicRepository.parseTrackOrDiscString("5/12"))
    }

    @Test
    fun `null, blank, zero and non-numeric all mean not present`() {
        assertNull(MusicRepository.parseTrackOrDiscString(null))
        assertNull(MusicRepository.parseTrackOrDiscString(""))
        assertNull(MusicRepository.parseTrackOrDiscString("0"))
        assertNull(MusicRepository.parseTrackOrDiscString("unknown"))
    }

    @Test
    fun `leading whitespace is trimmed before parsing`() {
        assertEquals(3, MusicRepository.parseTrackOrDiscString("  3/10"))
    }

    // --- parseLegacyTrackColumn (pre-R combined TRACK int: disc*1000+track) ---

    @Test
    fun `combined value under 1000 is track-only, no disc tag`() {
        assertEquals(7 to null, MusicRepository.parseLegacyTrackColumn(7))
    }

    @Test
    fun `combined value splits into disc and track`() {
        assertEquals(5 to 2, MusicRepository.parseLegacyTrackColumn(2005))
    }

    @Test
    fun `zero or negative means neither track nor disc present`() {
        assertEquals(null to null, MusicRepository.parseLegacyTrackColumn(0))
        assertEquals(null to null, MusicRepository.parseLegacyTrackColumn(-1))
    }

    @Test
    fun `disc boundary with zero track is track-absent but disc-present`() {
        // 3000 = disc 3, track 0 — a tagger that only wrote the disc number.
        assertEquals(null to 3, MusicRepository.parseLegacyTrackColumn(3000))
    }
}
