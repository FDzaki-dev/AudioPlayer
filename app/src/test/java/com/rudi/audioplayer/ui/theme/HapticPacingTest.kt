package com.rudi.audioplayer.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticPacingTest {

    @Test
    fun heavyTimings_polaOnOffDenganJedaAwalNol() {
        val t = HapticPacing.heavyTimings()
        assertEquals(4, t.size)
        assertEquals(0L, t[0])
        assertEquals(HapticPacing.HEAVY_PULSE_MS, t[1])
        assertEquals(HapticPacing.HEAVY_GAP_MS, t[2])
        assertEquals(HapticPacing.HEAVY_PULSE_MS, t[3])
    }

    @Test
    fun heavyTotalMs_samaDenganJumlahPola() {
        assertEquals(HapticPacing.heavyTimings().sum(), HapticPacing.heavyTotalMs())
    }

    @Test
    fun heavyTimings_arrayBaruTiapPanggilan() {
        val first = HapticPacing.heavyTimings()
        first[1] = 1L
        assertEquals(HapticPacing.HEAVY_PULSE_MS, HapticPacing.heavyTimings()[1])
    }

    @Test
    fun kuatLebihPanjangDariKetuk() {
        assertTrue(HapticPacing.HEAVY_PULSE_MS > HapticPacing.TAP_PULSE_MS)
        assertTrue(HapticPacing.heavyTotalMs() > HapticPacing.TAP_PULSE_MS)
    }

    @Test
    fun jedaKetuk_menyisakanWaktuMatiSepanjangSatuDenyut() {
        assertTrue(HapticPacing.TAP_MIN_GAP_MS - HapticPacing.TAP_PULSE_MS >= HapticPacing.TAP_PULSE_MS)
    }

    @Test
    fun ketukPertama_diizinkan() {
        assertTrue(HapticThrottle().allow(HapticTier.TAP, 1_000L))
    }

    @Test
    fun ketukBeruntun_dibatasiSampaiJedaMinimum() {
        val gap = HapticPacing.TAP_MIN_GAP_MS
        val throttle = HapticThrottle()
        assertTrue(throttle.allow(HapticTier.TAP, 1_000L))
        assertFalse(throttle.allow(HapticTier.TAP, 1_000L + gap - 1))
        assertTrue(throttle.allow(HapticTier.TAP, 1_000L + gap))
    }

    @Test
    fun ketukDitolak_tidakMemperpanjangJendela() {
        val gap = HapticPacing.TAP_MIN_GAP_MS
        val throttle = HapticThrottle()
        assertTrue(throttle.allow(HapticTier.TAP, 0L))
        assertFalse(throttle.allow(HapticTier.TAP, gap / 2))
        assertTrue(throttle.allow(HapticTier.TAP, gap))
    }

    @Test
    fun kuat_selaluDiizinkan() {
        val throttle = HapticThrottle()
        assertTrue(throttle.allow(HapticTier.HEAVY, 0L))
        assertTrue(throttle.allow(HapticTier.HEAVY, 1L))
    }

    @Test
    fun ketukDitahan_selamaPolaKuatBerjalan() {
        val busy = HapticPacing.heavyTotalMs()
        val throttle = HapticThrottle()
        assertTrue(throttle.allow(HapticTier.HEAVY, 5_000L))
        assertFalse(throttle.allow(HapticTier.TAP, 5_000L + busy - 1))
        assertTrue(throttle.allow(HapticTier.TAP, 5_000L + busy))
    }

    @Test
    fun kuat_tidakDitahanKetukSebelumnya() {
        val throttle = HapticThrottle()
        assertTrue(throttle.allow(HapticTier.TAP, 0L))
        assertTrue(throttle.allow(HapticTier.HEAVY, 1L))
    }

    @Test
    fun jamMundur_tidakMemblokirSelamanya() {
        val throttle = HapticThrottle()
        assertTrue(throttle.allow(HapticTier.TAP, 1_000L))
        assertTrue(throttle.allow(HapticTier.TAP, 500L))
    }

    @Test
    fun summary_awalSemuaNol() {
        assertEquals("ketuk_terkirim=0 ketuk_dilewati=0 kuat_terkirim=0", HapticThrottle().summary())
    }

    @Test
    fun summary_menghitungTerkirimDanDilewati() {
        val throttle = HapticThrottle()
        throttle.allow(HapticTier.TAP, 0L)
        throttle.allow(HapticTier.TAP, 1L)
        throttle.allow(HapticTier.TAP, 2L)
        throttle.allow(HapticTier.HEAVY, 3L)
        assertEquals("ketuk_terkirim=1 ketuk_dilewati=2 kuat_terkirim=1", throttle.summary())
    }
}
