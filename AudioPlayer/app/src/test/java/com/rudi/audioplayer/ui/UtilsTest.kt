package com.rudi.audioplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest {

    @Test
    fun `zero milliseconds formats as 0-00`() {
        assertEquals("0:00", formatDuration(0L))
    }

    @Test
    fun `seconds under a minute are zero-padded`() {
        assertEquals("0:05", formatDuration(5_000L))
        assertEquals("0:59", formatDuration(59_000L))
    }

    @Test
    fun `exactly one minute rolls over correctly`() {
        assertEquals("1:00", formatDuration(60_000L))
    }

    @Test
    fun `minutes and seconds combine as expected`() {
        assertEquals("3:45", formatDuration(225_000L))
    }

    @Test
    fun `partial seconds are truncated, not rounded`() {
        // 1:59.9 must still read 1:59, not roll up to 2:00 — a duration label should never
        // display a time later than the position it's meant to describe.
        assertEquals("1:59", formatDuration(119_900L))
    }

    @Test
    fun `double-digit minutes are not zero-padded`() {
        assertEquals("125:30", formatDuration(7_530_000L))
    }
}
