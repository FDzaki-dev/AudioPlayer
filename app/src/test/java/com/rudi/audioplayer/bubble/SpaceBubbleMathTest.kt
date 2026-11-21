package com.rudi.audioplayer.bubble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Batch 660 — matematika murni skin Space bubble (kedipan, meteor/kilatan, orbit, denyut, alpha warna, bintang). */
class SpaceBubbleMathTest {

    private val eps = 0.0001f

    @Test
    fun `kedipan bintang selalu di rentang redup sampai terang`() {
        var t = 0f
        while (t < 40f) {
            val v = SpaceBubbleMath.twinkle(phase = 0.37f, periodS = 4.5f, t = t)
            assertTrue("nilai $v di luar rentang", v >= 0.22f - eps && v <= 1f + eps)
            t += 0.37f
        }
    }

    @Test
    fun `kedipan dengan periode tak valid kembali terang penuh`() {
        assertEquals(1f, SpaceBubbleMath.twinkle(phase = 0.2f, periodS = 0f, t = 3f), eps)
        assertEquals(1f, SpaceBubbleMath.twinkle(phase = 0.2f, periodS = -2f, t = 3f), eps)
    }

    @Test
    fun `kemajuan ledakan aktif di awal siklus lalu mati dan berulang`() {
        assertEquals(0.5f, SpaceBubbleMath.burstProgress(t = 1f, periodS = 10f, activeFraction = 0.2f, offsetS = 0f), eps)
        assertEquals(-1f, SpaceBubbleMath.burstProgress(t = 2.5f, periodS = 10f, activeFraction = 0.2f, offsetS = 0f), eps)
        assertEquals(0.25f, SpaceBubbleMath.burstProgress(t = 10.5f, periodS = 10f, activeFraction = 0.2f, offsetS = 0f), eps)
    }

    @Test
    fun `kemajuan ledakan memakai offset dan menolak masukan tak valid`() {
        assertEquals(0.5f, SpaceBubbleMath.burstProgress(t = 0f, periodS = 10f, activeFraction = 0.2f, offsetS = 1f), eps)
        assertEquals(-1f, SpaceBubbleMath.burstProgress(t = 1f, periodS = 0f, activeFraction = 0.2f, offsetS = 0f), eps)
        assertEquals(-1f, SpaceBubbleMath.burstProgress(t = 1f, periodS = 10f, activeFraction = 0f, offsetS = 0f), eps)
    }

    @Test
    fun `sudut orbit dimulai dari sudut awal dan setengah periode menambah setengah putaran`() {
        assertEquals(0.9f, SpaceBubbleMath.orbitAngle(t = 0f, periodS = 7f, startRad = 0.9f), eps)
        assertEquals(0.9f + SpaceBubbleMath.TWO_PI / 2f, SpaceBubbleMath.orbitAngle(t = 3.5f, periodS = 7f, startRad = 0.9f), 0.001f)
        assertEquals(0.9f, SpaceBubbleMath.orbitAngle(t = 3f, periodS = 0f, startRad = 0.9f), eps)
    }

    @Test
    fun `denyut mulai dari setengah dan tetap di rentang nol sampai satu`() {
        assertEquals(0.5f, SpaceBubbleMath.pulse(t = 0f, periodS = 2f), eps)
        var t = 0f
        while (t < 10f) {
            val v = SpaceBubbleMath.pulse(t = t, periodS = 1.8f)
            assertTrue(v >= -eps && v <= 1f + eps)
            t += 0.13f
        }
        assertEquals(1f, SpaceBubbleMath.pulse(t = 1f, periodS = 0f), eps)
    }

    @Test
    fun `alpha warna menggantikan byte tertinggi dan menjepit rentang`() {
        val cyan = SpaceBubblePalette.ACCENT
        assertEquals(0x805CD6FF.toInt(), SpaceBubblePalette.withAlpha(cyan, 0.5f))
        assertEquals(0x005CD6FF, SpaceBubblePalette.withAlpha(cyan, -3f))
        assertEquals(0xFF5CD6FF.toInt(), SpaceBubblePalette.withAlpha(cyan, 9f))
    }

    @Test
    fun `warna bintang mengikuti tint dan tint asing jatuh ke putih`() {
        assertEquals(SpaceBubblePalette.STAR_WHITE, SpaceBubblePalette.starColor(SpaceBubblePalette.TINT_WHITE))
        assertEquals(SpaceBubblePalette.ACCENT, SpaceBubblePalette.starColor(SpaceBubblePalette.TINT_CYAN))
        assertEquals(SpaceBubblePalette.STAR_GOLD, SpaceBubblePalette.starColor(SpaceBubblePalette.TINT_GOLD))
        assertEquals(SpaceBubblePalette.STAR_WHITE, SpaceBubblePalette.starColor(99))
    }

    @Test
    fun `medan bintang deterministik dan seluruh nilainya di rentang yang sah`() {
        val a = SpaceBubbleStars(count = 28, seed = 6602)
        val b = SpaceBubbleStars(count = 28, seed = 6602)
        assertEquals(28, a.count)
        for (i in 0 until a.count) {
            assertEquals(a.x[i], b.x[i], 0f)
            assertEquals(a.y[i], b.y[i], 0f)
            assertEquals(a.tint[i], b.tint[i])
            assertTrue(a.x[i] in 0f..1f && a.y[i] in 0f..1f)
            assertTrue(a.weight[i] in 0f..1f)
            assertTrue(a.phase[i] in 0f..1f)
            assertTrue(a.period[i] >= 3f && a.period[i] <= 9f)
            assertTrue(a.tint[i] in 0..2)
        }
    }
}
