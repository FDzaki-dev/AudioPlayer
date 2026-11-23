package com.rudi.audioplayer.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DragDismissTrackerTest {

    private fun tracker() = DragDismissTracker(thresholdPx = 100f)

    @Test
    fun `tarikan ke bawah di bawah ambang tak memicu`() {
        val t = tracker()
        t.begin()
        assertFalse(t.onDelta(40f))
        assertFalse(t.onDelta(59f))
    }

    @Test
    fun `memicu tepat saat total mencapai ambang`() {
        val t = tracker()
        t.begin()
        assertFalse(t.onDelta(60f))
        assertTrue(t.onDelta(40f))
    }

    @Test
    fun `memicu hanya sekali per tarikan`() {
        val t = tracker()
        t.begin()
        assertTrue(t.onDelta(150f))
        assertFalse(t.onDelta(50f))
        assertFalse(t.onDelta(500f))
    }

    @Test
    fun `tarikan baru setelah begin bisa memicu lagi`() {
        val t = tracker()
        t.begin()
        assertTrue(t.onDelta(120f))
        t.begin()
        assertFalse(t.onDelta(30f))
        assertTrue(t.onDelta(80f))
    }

    @Test
    fun `gerak ke atas mengurangi total`() {
        val t = tracker()
        t.begin()
        assertFalse(t.onDelta(80f))
        assertFalse(t.onDelta(-50f))
        assertFalse(t.onDelta(60f))
        assertTrue(t.onDelta(10f))
    }

    @Test
    fun `gerak ke atas di awal tak menambah jarak yang harus ditempuh`() {
        val t = tracker()
        t.begin()
        assertFalse(t.onDelta(-200f))
        assertFalse(t.onDelta(99f))
        assertTrue(t.onDelta(1f))
    }
}
