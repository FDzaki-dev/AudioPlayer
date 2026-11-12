package com.rudi.audioplayer.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetMarqueeMathTest {

    @Test
    fun `teks muat di kolom tidak digeser`() {
        assertFalse(WidgetMarqueeMath.shouldScroll(100f, 120f))
        assertFalse(WidgetMarqueeMath.shouldScroll(121f, 120f)) // lebih < MIN_OVERFLOW_DP
    }

    @Test
    fun `kolom terlalu sempit tidak digeser`() {
        assertFalse(WidgetMarqueeMath.shouldScroll(441f, 10f))
    }

    @Test
    fun `teks melebihi kolom digeser`() {
        assertTrue(WidgetMarqueeMath.shouldScroll(441f, 150f))
    }

    @Test
    fun `jarak antar salinan sepertiga kolom minimal batas bawah`() {
        assertEquals(WidgetMarqueeMath.MIN_GAP_DP, WidgetMarqueeMath.gapDp(30f), 0.001f)
        assertEquals(60f, WidgetMarqueeMath.gapDp(180f), 0.001f)
    }

    @Test
    fun `jumlah segmen = periode dibulatkan ke atas ke kelipatan 30dp`() {
        assertEquals(1, WidgetMarqueeMath.segmentsFor(1f))
        assertEquals(1, WidgetMarqueeMath.segmentsFor(30f))
        assertEquals(2, WidgetMarqueeMath.segmentsFor(30.1f))
        assertEquals(15, WidgetMarqueeMath.segmentsFor(444f)) // 444 -> 450dp
        assertEquals(450f, WidgetMarqueeMath.targetPeriodDp(15), 0.001f)
    }

    @Test
    fun `konstanta sejalan dengan animasi XML 30dp per 1000 ms = 30dp per detik`() {
        assertEquals(WidgetMarqueeMath.SPEED_DP_PER_SEC, WidgetMarqueeMath.SEGMENT_DP / (WidgetMarqueeMath.SEGMENT_MS / 1000f), 0.001f)
    }

    @Test
    fun `siklus = loop segmen + diam`() {
        assertEquals(15_000L, WidgetMarqueeMath.loopMs(15))
        assertEquals(15_000L + WidgetMarqueeMath.PAUSE_MS, WidgetMarqueeMath.cycleMs(15))
    }

    @Test
    fun `margin awal segmen kelipatan -30dp`() {
        assertEquals(0f, WidgetMarqueeMath.marginDp(0), 0.001f)
        assertEquals(-30f, WidgetMarqueeMath.marginDp(1), 0.001f)
        assertEquals(-420f, WidgetMarqueeMath.marginDp(14), 0.001f)
    }

    @Test
    fun `kunci kejadian naik 1 per segmen, -1 saat diam, lalu loop berikutnya lanjut`() {
        val q = 3
        assertEquals(0L, WidgetMarqueeMath.eventKey(q, 0L))
        assertEquals(0L, WidgetMarqueeMath.eventKey(q, 999L))
        assertEquals(1L, WidgetMarqueeMath.eventKey(q, 1000L))
        assertEquals(2L, WidgetMarqueeMath.eventKey(q, 2999L))
        assertEquals(-1L, WidgetMarqueeMath.eventKey(q, 3000L)) // mulai diam
        assertEquals(-1L, WidgetMarqueeMath.eventKey(q, 3000L + WidgetMarqueeMath.PAUSE_MS - 1))
        assertEquals(3L, WidgetMarqueeMath.eventKey(q, WidgetMarqueeMath.cycleMs(q))) // loop ke-2 segmen 0
        assertEquals(0, WidgetMarqueeMath.segmentOfKey(q, 3L))
        assertEquals(2, WidgetMarqueeMath.segmentOfKey(q, 5L))
    }

    @Test
    fun `satu segmen per loop tetap menghasilkan kunci berbeda tiap loop`() {
        val q = 1
        assertEquals(0L, WidgetMarqueeMath.eventKey(q, 0L))
        assertEquals(-1L, WidgetMarqueeMath.eventKey(q, 1000L))
        assertEquals(1L, WidgetMarqueeMath.eventKey(q, WidgetMarqueeMath.cycleMs(q)))
    }

    @Test
    fun `jeda kiriman berikutnya - batas segmen, lalu awal loop berikutnya`() {
        val q = 3
        assertEquals(1000L, WidgetMarqueeMath.delayToNextMs(q, 0L))
        assertEquals(1L, WidgetMarqueeMath.delayToNextMs(q, 999L))
        assertEquals(1000L, WidgetMarqueeMath.delayToNextMs(q, 1000L))
        // setelah segmen terakhir dikirim (t = 2000..): kejadian berikut = awal loop berikutnya
        assertEquals(WidgetMarqueeMath.cycleMs(q) - 2000L, WidgetMarqueeMath.delayToNextMs(q, 2000L))
        assertEquals(WidgetMarqueeMath.cycleMs(q) - 3500L, WidgetMarqueeMath.delayToNextMs(q, 3500L))
        assertEquals(WidgetMarqueeMath.cycleMs(1), WidgetMarqueeMath.delayToNextMs(1, 0L)) // 1 segmen: kejadian berikut = loop berikutnya
    }

    @Test
    fun `mengikuti jadwal jeda tak pernah melewatkan atau menggandakan kejadian`() {
        for (q in listOf(1, 2, 5, 15, 50)) {
            var t = 0L
            var lastKey = -1L
            var sent = 0
            val horizon = 4 * WidgetMarqueeMath.cycleMs(q)
            while (t < horizon) {
                val key = WidgetMarqueeMath.eventKey(q, t)
                if (key >= 0 && key != lastKey) {
                    assertTrue("q=$q t=$t key=$key last=$lastKey", key == lastKey + 1)
                    lastKey = key
                    sent++
                }
                t += WidgetMarqueeMath.delayToNextMs(q, t)
            }
            assertEquals("q=$q", 4 * q, sent)
        }
    }
}
