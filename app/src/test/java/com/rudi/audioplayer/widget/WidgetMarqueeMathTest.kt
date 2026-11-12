package com.rudi.audioplayer.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetMarqueeMathTest {

    @Test
    fun `teks muat di kolom tidak digeser`() {
        assertEquals(0f, WidgetMarqueeMath.plan(100f, 140f, 120f).travelDp, 0.001f)
        assertEquals(0f, WidgetMarqueeMath.plan(121f, 160f, 120f).travelDp, 0.001f) // lebih < MIN_OVERFLOW_DP
    }

    @Test
    fun `kolom terlalu sempit tidak digeser`() {
        assertEquals(0f, WidgetMarqueeMath.plan(441f, 480f, 10f).travelDp, 0.001f)
    }

    @Test
    fun `judul biasa memakai loop mulus dengan jarak geser = periode`() {
        val p = WidgetMarqueeMath.plan(441f, 480f, 150f)
        assertTrue(p.loop)
        assertEquals(480f, p.travelDp, 0.001f)
    }

    @Test
    fun `judul sangat panjang jatuh ke mode terbatas sampai ujung teks`() {
        val p = WidgetMarqueeMath.plan(2500f, 2540f, 100f) // 2 salinan (5040) > 3900
        assertFalse(p.loop)
        assertEquals(2500f - 100f + WidgetMarqueeMath.END_SLACK_DP, p.travelDp, 0.001f)
    }

    @Test
    fun `mode terbatas dibatasi lebar maksimum TextView`() {
        val p = WidgetMarqueeMath.plan(9000f, 9040f, 100f)
        assertFalse(p.loop)
        assertEquals(WidgetMarqueeMath.MAX_TEXT_DP - 100f + WidgetMarqueeMath.END_SLACK_DP, p.travelDp, 0.001f)
    }

    @Test
    fun `jarak antar salinan sepertiga kolom minimal batas bawah`() {
        assertEquals(WidgetMarqueeMath.MIN_GAP_DP, WidgetMarqueeMath.gapDp(30f), 0.001f)
        assertEquals(60f, WidgetMarqueeMath.gapDp(180f), 0.001f)
    }

    @Test
    fun `loop - diam di awal, trapesium, tiba tepat di periode lalu mulai lagi dari nol`() {
        val travel = 300f
        val total = WidgetMarqueeMath.scrollMs(travel) // 10000 + 500
        assertEquals(10_500L, total)
        assertEquals(WidgetMarqueeMath.START_PAUSE_MS + total, WidgetMarqueeMath.cycleMs(travel, true)) // loop: tanpa diam di ujung
        assertEquals(0f, WidgetMarqueeMath.offsetDp(travel, true, 0L), 0.001f)
        assertEquals(0f, WidgetMarqueeMath.offsetDp(travel, true, WidgetMarqueeMath.START_PAUSE_MS), 0.001f)
        // ujung siklus mendekati travel (= bingkai awal pada loop)
        assertEquals(travel, WidgetMarqueeMath.offsetDp(travel, true, WidgetMarqueeMath.cycleMs(travel, true) - 1), 0.1f)
        // siklus berikutnya mulai dari 0
        assertEquals(0f, WidgetMarqueeMath.offsetDp(travel, true, WidgetMarqueeMath.cycleMs(travel, true)), 0.001f)
    }

    @Test
    fun `kecepatan tengah = kecepatan tetap dan ramp mulai pelan`() {
        val travel = 300f
        val base = WidgetMarqueeMath.START_PAUSE_MS
        // di tengah (jauh dari ramp): selisih 1 dtk = 30dp
        val mid = base + 5_000L
        val d = WidgetMarqueeMath.offsetDp(travel, true, mid + 1000L) - WidgetMarqueeMath.offsetDp(travel, true, mid)
        assertEquals(30f, d, 0.05f)
        // 100 ms pertama jauh lebih pendek dari kecepatan penuh (3dp)
        assertTrue(WidgetMarqueeMath.offsetDp(travel, true, base + 100L) < 0.5f)
        // 100 ms terakhir juga pelan (perlambatan)
        val end = base + WidgetMarqueeMath.scrollMs(travel)
        assertTrue(travel - WidgetMarqueeMath.offsetDp(travel, true, end - 100L) < 0.5f)
    }

    @Test
    fun `mode terbatas - diam di ujung selama jeda akhir lalu mengulang`() {
        val travel = 300f
        val scrollEnd = WidgetMarqueeMath.START_PAUSE_MS + WidgetMarqueeMath.scrollMs(travel)
        assertEquals(travel, WidgetMarqueeMath.offsetDp(travel, false, scrollEnd), 0.01f)
        assertEquals(travel, WidgetMarqueeMath.offsetDp(travel, false, WidgetMarqueeMath.cycleMs(travel, false) - 1), 0.01f)
        assertEquals(0f, WidgetMarqueeMath.offsetDp(travel, false, WidgetMarqueeMath.cycleMs(travel, false)), 0.001f)
        assertEquals(WidgetMarqueeMath.START_PAUSE_MS + WidgetMarqueeMath.scrollMs(travel) + WidgetMarqueeMath.END_PAUSE_MS, WidgetMarqueeMath.cycleMs(travel, false))
    }

    @Test
    fun `tanpa jarak geser selalu nol`() {
        assertEquals(0f, WidgetMarqueeMath.offsetDp(0f, true, 5_000L), 0.001f)
        assertEquals(Long.MAX_VALUE, WidgetMarqueeMath.delayToNextMs(0f, true, 5_000L, 33L))
    }

    @Test
    fun `jeda kiriman berikutnya - diam menunggu sisa, geser memakai tick`() {
        val travel = 300f
        assertEquals(WidgetMarqueeMath.START_PAUSE_MS, WidgetMarqueeMath.delayToNextMs(travel, true, 0L, 33L))
        assertEquals(33L, WidgetMarqueeMath.delayToNextMs(travel, true, WidgetMarqueeMath.START_PAUSE_MS + 500L, 33L))
        val scrollEnd = WidgetMarqueeMath.START_PAUSE_MS + WidgetMarqueeMath.scrollMs(travel)
        assertEquals(WidgetMarqueeMath.END_PAUSE_MS, WidgetMarqueeMath.delayToNextMs(travel, false, scrollEnd, 33L))
    }

    @Test
    fun `offset tak pernah negatif, tak melebihi jarak, tak pernah mundur dalam satu fase geser`() {
        for (loop in listOf(true, false)) {
            for (travel in listOf(20f, 60f, 300f, 1200f)) {
                var t = 0L
                var prev = 0f
                val cycle = WidgetMarqueeMath.cycleMs(travel, loop)
                while (t < 3 * cycle) {
                    val o = WidgetMarqueeMath.offsetDp(travel, loop, t)
                    assertTrue("loop=$loop travel=$travel t=$t o=$o", o >= 0f && o <= travel + 0.001f)
                    if (t % cycle != 0L && t / cycle == (t - 7L) / cycle) {
                        assertTrue("mundur loop=$loop travel=$travel t=$t", o >= prev - 0.001f)
                    }
                    assertTrue(WidgetMarqueeMath.delayToNextMs(travel, loop, t, 33L) >= 1L)
                    prev = o
                    t += 7L
                }
            }
        }
    }
}
