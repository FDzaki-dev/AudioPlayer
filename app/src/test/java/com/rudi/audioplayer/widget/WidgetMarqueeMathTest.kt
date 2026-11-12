package com.rudi.audioplayer.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max

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
    fun `jumlah segmen = periode dibulatkan ke atas ke kelipatan 90dp`() {
        assertEquals(1, WidgetMarqueeMath.segmentsFor(1f))
        assertEquals(1, WidgetMarqueeMath.segmentsFor(90f))
        assertEquals(2, WidgetMarqueeMath.segmentsFor(90.1f))
        assertEquals(5, WidgetMarqueeMath.segmentsFor(444f)) // 444 -> 450dp
        assertEquals(450f, WidgetMarqueeMath.targetPeriodDp(5), 0.001f)
    }

    @Test
    fun `segmen untuk teks = kelipatan terdekat tapi jarak antar salinan tak di bawah minimum`() {
        // teks 396dp, kolom 141dp -> jarak ideal 47dp -> 443dp -> 4,92 segmen -> 5 (periode 450dp, jarak 54dp)
        assertEquals(5, WidgetMarqueeMath.segmentsForText(396f, 141f))
        // teks 396dp, kolom 176dp -> jarak ideal 58,7dp -> 454,7dp -> 5,05 -> 5
        assertEquals(5, WidgetMarqueeMath.segmentsForText(396f, 176f))
        // teks 380dp, kolom 141dp: ideal 427dp -> 4,74 -> 5; minimum 404dp -> 5
        assertEquals(5, WidgetMarqueeMath.segmentsForText(380f, 141f))
        // teks 350dp, kolom 141dp: ideal 397dp -> 4,41 -> bulat 4 (360dp) tapi 350+24=374 > 360 -> naik ke 5
        assertEquals(5, WidgetMarqueeMath.segmentsForText(350f, 141f))
        // teks 320dp, kolom 141dp: ideal 367dp -> 4,08 -> 4 (360dp), jarak 40dp >= 24dp
        assertEquals(4, WidgetMarqueeMath.segmentsForText(320f, 141f))
    }

    @Test
    fun `jarak antar salinan selalu >= minimum dan < max(ideal + setengah segmen, minimum + satu segmen)`() {
        var text = 100f
        while (text <= 1400f) {
            for (col in listOf(60f, 141f, 176f, 300f)) {
                val q = WidgetMarqueeMath.segmentsForText(text, col)
                val gap = WidgetMarqueeMath.targetPeriodDp(q) - text
                assertTrue("text=$text col=$col gap=$gap", gap >= WidgetMarqueeMath.MIN_GAP_DP - 0.001f)
                val upper = max(
                    WidgetMarqueeMath.gapDp(col) + WidgetMarqueeMath.SEGMENT_DP / 2f,
                    WidgetMarqueeMath.MIN_GAP_DP + WidgetMarqueeMath.SEGMENT_DP
                )
                assertTrue("text=$text col=$col gap=$gap", gap <= upper + 0.001f)
            }
            text += 7f
        }
    }

    @Test
    fun `dua salinan periode terpanjang muat di TextView 4000dp`() {
        assertTrue(2 * WidgetMarqueeMath.targetPeriodDp(WidgetMarqueeMath.MAX_SEGMENTS) < 4000f)
    }

    @Test
    fun `konstanta sejalan dengan animasi XML 90dp per 3000 ms = 30dp per detik`() {
        assertEquals(WidgetMarqueeMath.SPEED_DP_PER_SEC, WidgetMarqueeMath.SEGMENT_DP / (WidgetMarqueeMath.SEGMENT_MS / 1000f), 0.001f)
    }

    @Test
    fun `siklus = loop segmen + diam`() {
        assertEquals(15_000L, WidgetMarqueeMath.loopMs(5))
        assertEquals(15_000L + WidgetMarqueeMath.PAUSE_MS, WidgetMarqueeMath.cycleMs(5))
    }

    @Test
    fun `margin awal segmen kelipatan -90dp`() {
        assertEquals(0f, WidgetMarqueeMath.marginDp(0), 0.001f)
        assertEquals(-90f, WidgetMarqueeMath.marginDp(1), 0.001f)
        assertEquals(-360f, WidgetMarqueeMath.marginDp(4), 0.001f)
    }

    @Test
    fun `kunci kejadian naik 1 per segmen, -1 saat diam, lalu loop berikutnya lanjut`() {
        val q = 3
        val seg = WidgetMarqueeMath.SEGMENT_MS
        val loop = q * seg
        assertEquals(0L, WidgetMarqueeMath.eventKey(q, 0L))
        assertEquals(0L, WidgetMarqueeMath.eventKey(q, seg - 1))
        assertEquals(1L, WidgetMarqueeMath.eventKey(q, seg))
        assertEquals(2L, WidgetMarqueeMath.eventKey(q, loop - 1))
        assertEquals(-1L, WidgetMarqueeMath.eventKey(q, loop)) // mulai diam
        assertEquals(-1L, WidgetMarqueeMath.eventKey(q, loop + WidgetMarqueeMath.PAUSE_MS - 1))
        assertEquals(3L, WidgetMarqueeMath.eventKey(q, WidgetMarqueeMath.cycleMs(q))) // loop ke-2 segmen 0
        assertEquals(0, WidgetMarqueeMath.segmentOfKey(q, 3L))
        assertEquals(2, WidgetMarqueeMath.segmentOfKey(q, 5L))
    }

    @Test
    fun `satu segmen per loop tetap menghasilkan kunci berbeda tiap loop`() {
        val q = 1
        assertEquals(0L, WidgetMarqueeMath.eventKey(q, 0L))
        assertEquals(-1L, WidgetMarqueeMath.eventKey(q, WidgetMarqueeMath.SEGMENT_MS))
        assertEquals(1L, WidgetMarqueeMath.eventKey(q, WidgetMarqueeMath.cycleMs(q)))
    }

    @Test
    fun `jeda kiriman berikutnya - batas segmen, lalu awal loop berikutnya`() {
        val q = 3
        val seg = WidgetMarqueeMath.SEGMENT_MS
        assertEquals(seg, WidgetMarqueeMath.delayToNextMs(q, 0L))
        assertEquals(1L, WidgetMarqueeMath.delayToNextMs(q, seg - 1))
        assertEquals(seg, WidgetMarqueeMath.delayToNextMs(q, seg))
        // setelah segmen terakhir dikirim (t = 2 x seg ..): kejadian berikut = awal loop berikutnya
        assertEquals(WidgetMarqueeMath.cycleMs(q) - 2 * seg, WidgetMarqueeMath.delayToNextMs(q, 2 * seg))
        assertEquals(WidgetMarqueeMath.cycleMs(q) - (3 * seg + 500L), WidgetMarqueeMath.delayToNextMs(q, 3 * seg + 500L))
        assertEquals(WidgetMarqueeMath.cycleMs(1), WidgetMarqueeMath.delayToNextMs(1, 0L)) // 1 segmen: kejadian berikut = loop berikutnya
    }

    @Test
    fun `mengikuti jadwal jeda tak pernah melewatkan atau menggandakan kejadian`() {
        for (q in listOf(1, 2, 5, 12, WidgetMarqueeMath.MAX_SEGMENTS)) {
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
