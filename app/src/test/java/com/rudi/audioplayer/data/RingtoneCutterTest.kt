package com.rudi.audioplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RingtoneCutterTest {

    @Test
    fun `clampRange keeps range inside song bounds`() {
        val r = RingtoneCutter.clampRange(startMs = -5_000L, endMs = 999_999L, songDurationMs = 180_000L)
        assertTrue(r.startMs >= 0L)
        assertTrue(r.endMs <= 180_000L)
    }

    @Test
    fun `clampRange enforces minimum duration`() {
        val r = RingtoneCutter.clampRange(startMs = 10_000L, endMs = 10_100L, songDurationMs = 180_000L)
        assertTrue(r.durationMs >= RingtoneCutter.MIN_DURATION_MS)
    }

    @Test
    fun `clampRange enforces maximum duration by trimming end first`() {
        val r = RingtoneCutter.clampRange(startMs = 5_000L, endMs = 200_000L, songDurationMs = 300_000L)
        assertEquals(RingtoneCutter.MAX_DURATION_MS, r.durationMs)
        assertEquals(5_000L, r.startMs)
    }

    @Test
    fun `clampRange pulls start back when end pinned to song end`() {
        // start requested very late, near the very end of a long song — end can't exceed
        // songDurationMs, so start must be pulled back to still respect MAX_DURATION_MS.
        val r = RingtoneCutter.clampRange(startMs = 299_000L, endMs = 300_000L, songDurationMs = 300_000L)
        assertTrue(r.durationMs <= RingtoneCutter.MAX_DURATION_MS)
        assertEquals(300_000L, r.endMs)
    }

    @Test
    fun `clampRange on very short song returns whole song`() {
        val r = RingtoneCutter.clampRange(startMs = 0L, endMs = 500L, songDurationMs = 500L)
        assertEquals(0L, r.startMs)
        assertEquals(500L, r.endMs)
    }

    @Test
    fun `isValid true for a well-formed range`() {
        val r = RingtoneCutter.TrimRange(10_000L, 25_000L)
        assertTrue(RingtoneCutter.isValid(r, 180_000L))
    }

    @Test
    fun `isValid false when range exceeds song duration`() {
        val r = RingtoneCutter.TrimRange(10_000L, 25_000L)
        assertFalse(RingtoneCutter.isValid(r, 20_000L))
    }

    @Test
    fun `isValid false when shorter than minimum`() {
        val r = RingtoneCutter.TrimRange(10_000L, 10_500L)
        assertFalse(RingtoneCutter.isValid(r, 180_000L))
    }

    @Test
    fun `isValid false when longer than maximum`() {
        val r = RingtoneCutter.TrimRange(0L, RingtoneCutter.MAX_DURATION_MS + 1_000L)
        assertFalse(RingtoneCutter.isValid(r, 300_000L))
    }

    @Test
    fun `formatTimestamp formats mm-ss`() {
        assertEquals("00:00", RingtoneCutter.formatTimestamp(0L))
        assertEquals("00:05", RingtoneCutter.formatTimestamp(5_000L))
        assertEquals("01:05", RingtoneCutter.formatTimestamp(65_000L))
        assertEquals("10:00", RingtoneCutter.formatTimestamp(600_000L))
    }
}
