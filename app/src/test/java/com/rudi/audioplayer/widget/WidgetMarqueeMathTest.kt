package com.rudi.audioplayer.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetMarqueeMathTest {

    @Test
    fun `teks muat di kolom tidak digeser`() {
        assertEquals(0f, WidgetMarqueeMath.travelDp(100f, 120f), 0.001f)
        assertEquals(0f, WidgetMarqueeMath.travelDp(121f, 120f), 0.001f) // lebih < MIN_OVERFLOW_DP
    }

    @Test
    fun `kolom terlalu sempit tidak digeser`() {
        assertEquals(0f, WidgetMarqueeMath.travelDp(441f, 10f), 0.001f)
    }

    @Test
    fun `jarak geser = teks - kolom + sisa ujung`() {
        assertEquals(441f - 150f + WidgetMarqueeMath.END_SLACK_DP, WidgetMarqueeMath.travelDp(441f, 150f), 0.001f)
    }

    @Test
    fun `jarak geser dibatasi lebar maksimum TextView`() {
        val t = WidgetMarqueeMath.travelDp(9000f, 100f)
        assertEquals(WidgetMarqueeMath.MAX_TEXT_DP - 100f + WidgetMarqueeMath.END_SLACK_DP, t, 0.001f)
    }

    @Test
    fun `offset diam di awal, linear saat geser, diam di ujung, lalu mengulang`() {
        val travel = 300f
        val scroll = WidgetMarqueeMath.scrollMs(travel)
        assertEquals(10_000L, scroll)
        assertEquals(0f, WidgetMarqueeMath.offsetDp(travel, 0L), 0.001f)
        assertEquals(0f, WidgetMarqueeMath.offsetDp(travel, WidgetMarqueeMath.START_PAUSE_MS - 1), 0.001f)
        // 1 detik setelah jeda awal = 30dp
        assertEquals(30f, WidgetMarqueeMath.offsetDp(travel, WidgetMarqueeMath.START_PAUSE_MS + 1000L), 0.01f)
        // ujung: tepat travel, tetap travel selama jeda akhir
        assertEquals(travel, WidgetMarqueeMath.offsetDp(travel, WidgetMarqueeMath.START_PAUSE_MS + scroll), 0.01f)
        assertEquals(travel, WidgetMarqueeMath.offsetDp(travel, WidgetMarqueeMath.cycleMs(travel) - 1), 0.01f)
        // siklus berikutnya mulai dari 0
        assertEquals(0f, WidgetMarqueeMath.offsetDp(travel, WidgetMarqueeMath.cycleMs(travel)), 0.001f)
        assertEquals(30f, WidgetMarqueeMath.offsetDp(travel, WidgetMarqueeMath.cycleMs(travel) + WidgetMarqueeMath.START_PAUSE_MS + 1000L), 0.01f)
    }

    @Test
    fun `offset tanpa jarak geser selalu nol`() {
        assertEquals(0f, WidgetMarqueeMath.offsetDp(0f, 5_000L), 0.001f)
        assertEquals(Long.MAX_VALUE, WidgetMarqueeMath.delayToNextMs(0f, 5_000L, 100L))
    }

    @Test
    fun `jeda kiriman berikutnya - diam menunggu sisa, geser memakai tick`() {
        val travel = 300f
        assertEquals(WidgetMarqueeMath.START_PAUSE_MS, WidgetMarqueeMath.delayToNextMs(travel, 0L, 100L))
        assertEquals(100L, WidgetMarqueeMath.delayToNextMs(travel, WidgetMarqueeMath.START_PAUSE_MS + 500L, 100L))
        val scrollEnd = WidgetMarqueeMath.START_PAUSE_MS + WidgetMarqueeMath.scrollMs(travel)
        assertEquals(WidgetMarqueeMath.END_PAUSE_MS, WidgetMarqueeMath.delayToNextMs(travel, scrollEnd, 100L))
    }

    @Test
    fun `offset tak pernah negatif atau melebihi jarak geser`() {
        for (travel in listOf(5f, 60f, 300f, 1200f)) {
            var t = 0L
            while (t < 3 * WidgetMarqueeMath.cycleMs(travel)) {
                val o = WidgetMarqueeMath.offsetDp(travel, t)
                assertTrue("travel=$travel t=$t o=$o", o >= 0f && o <= travel + 0.001f)
                assertTrue(WidgetMarqueeMath.delayToNextMs(travel, t, 100L) >= 1L)
                t += 37L
            }
        }
    }
}
