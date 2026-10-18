package com.rudi.audioplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayCountTrackerTest {

    @Test
    fun `ambang adalah 30 detik atau setengah durasi mana yang lebih kecil`() {
        assertEquals(30_000L, PlayCountTracker.thresholdFor(0L))
        assertEquals(30_000L, PlayCountTracker.thresholdFor(-5L))
        assertEquals(30_000L, PlayCountTracker.thresholdFor(200_000L))
        assertEquals(20_000L, PlayCountTracker.thresholdFor(40_000L))
        assertEquals(5_000L, PlayCountTracker.thresholdFor(10_000L))
        assertEquals(1_000L, PlayCountTracker.thresholdFor(1_000L))
    }

    @Test
    fun `lagu di skip sebelum ambang tidak dihitung`() {
        val tracker = PlayCountTracker()
        tracker.startItem(1L, 200_000L, nowMs = 0L, playing = true)

        assertNull(tracker.takeIfQualified(10_000L))

        tracker.startItem(2L, 200_000L, nowMs = 10_000L, playing = true)
        assertNull(tracker.takeIfQualified(20_000L))
    }

    @Test
    fun `lagu dihitung tepat sekali saat ambang tercapai`() {
        val tracker = PlayCountTracker()
        tracker.startItem(7L, 200_000L, nowMs = 0L, playing = true)

        assertNull(tracker.takeIfQualified(29_999L))
        assertEquals(7L, tracker.takeIfQualified(30_000L))
        assertNull(tracker.takeIfQualified(60_000L))
        assertNull(tracker.msUntilQualified(60_000L))
    }

    @Test
    fun `jeda tidak menambah waktu dengar`() {
        val tracker = PlayCountTracker()
        tracker.startItem(3L, 200_000L, nowMs = 0L, playing = true)
        tracker.setPlaying(false, nowMs = 10_000L)

        assertNull(tracker.takeIfQualified(110_000L))
        assertNull(tracker.msUntilQualified(110_000L))

        tracker.setPlaying(true, nowMs = 110_000L)
        assertEquals(20_000L, tracker.msUntilQualified(110_000L))
        assertNull(tracker.takeIfQualified(129_999L))
        assertEquals(3L, tracker.takeIfQualified(130_000L))
    }

    @Test
    fun `sesi dimulai dalam keadaan jeda baru berjalan setelah diputar`() {
        val tracker = PlayCountTracker()
        tracker.startItem(4L, 200_000L, nowMs = 0L, playing = false)

        assertNull(tracker.msUntilQualified(50_000L))
        assertNull(tracker.takeIfQualified(50_000L))

        tracker.setPlaying(true, nowMs = 50_000L)
        assertEquals(4L, tracker.takeIfQualified(80_000L))
    }

    @Test
    fun `lagu pendek memakai setengah durasi`() {
        val tracker = PlayCountTracker()
        tracker.startItem(5L, 10_000L, nowMs = 0L, playing = true)

        assertNull(tracker.takeIfQualified(4_999L))
        assertEquals(5L, tracker.takeIfQualified(5_000L))
    }

    @Test
    fun `mengulang lagu yang sama adalah sesi baru dan bisa dihitung lagi`() {
        val tracker = PlayCountTracker()
        tracker.startItem(9L, 60_000L, nowMs = 0L, playing = true)
        assertEquals(9L, tracker.takeIfQualified(30_000L))

        tracker.startItem(9L, 60_000L, nowMs = 60_000L, playing = true)
        assertNull(tracker.takeIfQualified(70_000L))
        assertEquals(9L, tracker.takeIfQualified(90_000L))
    }

    @Test
    fun `tanpa sesi semua aman dan hasilnya null`() {
        val tracker = PlayCountTracker()

        assertFalse(tracker.hasItem())
        tracker.setPlaying(true, 0L)
        assertNull(tracker.msUntilQualified(1L))
        assertNull(tracker.takeIfQualified(1_000_000L))

        tracker.startItem(1L, 100_000L, 0L, true)
        assertTrue(tracker.hasItem())
        tracker.clearItem()
        assertFalse(tracker.hasItem())
        assertNull(tracker.takeIfQualified(1_000_000L))
    }
}
