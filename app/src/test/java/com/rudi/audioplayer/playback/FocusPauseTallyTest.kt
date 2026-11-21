package com.rudi.audioplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusPauseTallyTest {

    @Test
    fun summary_awalSemuaNol() {
        assertEquals("jeda_fokus=0 total=0dtk maks=0dtk", FocusPauseTally().summary())
    }

    @Test
    fun jedaLaluLanjutOlehFokus_mengembalikanLamaDanDihitung() {
        val tally = FocusPauseTally()
        tally.onFocusPause(1_000L)
        assertEquals(1_500L, tally.onResume(2_500L, byFocusRegain = true))
        assertEquals("jeda_fokus=1 total=1dtk maks=1dtk", tally.summary())
    }

    @Test
    fun dilanjutkanManual_tidakDihitung() {
        val tally = FocusPauseTally()
        tally.onFocusPause(0L)
        assertNull(tally.onResume(5_000L, byFocusRegain = false))
        assertEquals("jeda_fokus=0 total=0dtk maks=0dtk", tally.summary())
    }

    @Test
    fun lanjutTanpaJedaFokus_null() {
        assertNull(FocusPauseTally().onResume(1_000L, byFocusRegain = true))
    }

    @Test
    fun duaJeda_totalDanTerlama() {
        val tally = FocusPauseTally()
        tally.onFocusPause(0L)
        tally.onResume(1_000L, byFocusRegain = true)
        tally.onFocusPause(10_000L)
        tally.onResume(23_000L, byFocusRegain = true)
        assertEquals("jeda_fokus=2 total=14dtk maks=13dtk", tally.summary())
    }

    @Test
    fun jedaGanda_tidakMenggeserAwal() {
        val tally = FocusPauseTally()
        tally.onFocusPause(100L)
        tally.onFocusPause(900L)
        assertEquals(1_000L, tally.onResume(1_100L, byFocusRegain = true))
    }

    @Test
    fun jamMundur_lamaTidakNegatif() {
        val tally = FocusPauseTally()
        tally.onFocusPause(1_000L)
        assertEquals(0L, tally.onResume(500L, byFocusRegain = true))
    }

    @Test
    fun jedaYangSudahDitutup_tidakDihitungDuaKali() {
        val tally = FocusPauseTally()
        tally.onFocusPause(0L)
        tally.onResume(2_000L, byFocusRegain = true)
        assertNull(tally.onResume(9_000L, byFocusRegain = true))
        assertEquals("jeda_fokus=1 total=2dtk maks=2dtk", tally.summary())
    }
}
