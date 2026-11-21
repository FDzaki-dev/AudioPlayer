package com.rudi.audioplayer.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStripSizingTest {

    @Test
    fun `strip sempit 46x263 tetap ukuran dasar`() {
        val s = WidgetStripSizing.compute(46, 263)
        assertEquals(1f, s.scale, 0.001f)
        assertEquals(32f, s.artDp, 0.001f)
    }

    @Test
    fun `strip 129x264 membesarkan sampul dan kenop`() {
        val s = WidgetStripSizing.compute(129, 264)
        assertEquals(1.5f, s.scale, 0.001f)
        assertEquals(83f, s.artDp, 0.001f)
    }

    @Test
    fun `tinggi minimum 160 memakai ukuran dasar`() {
        val s = WidgetStripSizing.compute(100, 160)
        assertEquals(1f, s.scale, 0.001f)
        assertEquals(32f, s.artDp, 0.001f)
    }

    @Test
    fun `isi tak pernah melebihi tinggi untuk semua ukuran strip`() {
        for (w in 40..149) {
            for (h in 160..700) {
                val s = WidgetStripSizing.compute(w, h)
                val cluster = 96f * s.scale + 6f
                assertTrue("w=$w h=$h", s.artDp + cluster + 19f <= h + 0.001f)
                assertTrue("w=$w h=$h skala", s.scale in 1f..WidgetStripSizing.SCALE_MAX)
                assertTrue("w=$w h=$h sampul", s.artDp >= WidgetStripSizing.ART_MIN_DP)
            }
        }
    }
}
