package com.rudi.audioplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFocusDuckingTest {
    private val gain = 1
    private val loss = -1
    private val lossTransient = -2
    private val lossCanDuck = -3

    @Test
    fun policy_gain_pulihkan() {
        assertEquals(FocusAction.RESUME_AND_UNDUCK, FocusPolicy.onFocusChange(gain, inCall = false))
    }

    @Test
    fun policy_hilangPermanen_jedaPermanen() {
        assertEquals(FocusAction.PAUSE_PERMANENT, FocusPolicy.onFocusChange(loss, inCall = false))
    }

    @Test
    fun policy_sementaraBukanPanggilan_turunkanVolume() {
        assertEquals(FocusAction.DUCK, FocusPolicy.onFocusChange(lossTransient, inCall = false))
    }

    @Test
    fun policy_sementaraSaatPanggilan_jeda() {
        assertEquals(FocusAction.PAUSE, FocusPolicy.onFocusChange(lossTransient, inCall = true))
    }

    @Test
    fun policy_bolehTurunkan_selaluTurunkanVolume() {
        assertEquals(FocusAction.DUCK, FocusPolicy.onFocusChange(lossCanDuck, inCall = false))
        assertEquals(FocusAction.DUCK, FocusPolicy.onFocusChange(lossCanDuck, inCall = true))
    }

    @Test
    fun policy_kodeTakDikenal_tidakAdaAksi() {
        assertEquals(FocusAction.NONE, FocusPolicy.onFocusChange(0, inCall = false))
    }

    @Test
    fun state_duckDuaKali_efekHanyaSekali() {
        val s = DuckingFocusState()
        assertTrue(s.apply(FocusAction.DUCK).duck)
        assertEquals(FocusEffects(), s.apply(FocusAction.DUCK))
        assertTrue(s.ducked)
    }

    @Test
    fun state_jedaLaluGain_melanjutkan() {
        val s = DuckingFocusState()
        assertTrue(s.apply(FocusAction.PAUSE).pause)
        val e = s.apply(FocusAction.RESUME_AND_UNDUCK)
        assertTrue(e.resume)
        assertFalse(e.unduck)
        assertFalse(s.pausedByFocus)
    }

    @Test
    fun state_duckLaluGain_memulihkanVolumeTanpaMelanjutkan() {
        val s = DuckingFocusState()
        s.apply(FocusAction.DUCK)
        val e = s.apply(FocusAction.RESUME_AND_UNDUCK)
        assertTrue(e.unduck)
        assertFalse(e.resume)
        assertFalse(s.ducked)
    }

    @Test
    fun state_gainTanpaPenahanan_tidakAdaEfek() {
        assertEquals(FocusEffects(), DuckingFocusState().apply(FocusAction.RESUME_AND_UNDUCK))
    }

    @Test
    fun state_duckLaluJedaLaluGain_pulihkanDanLanjut() {
        val s = DuckingFocusState()
        s.apply(FocusAction.DUCK)
        s.apply(FocusAction.PAUSE)
        val e = s.apply(FocusAction.RESUME_AND_UNDUCK)
        assertTrue(e.unduck)
        assertTrue(e.resume)
    }

    @Test
    fun state_hilangPermanen_jedaLepasFokusPulihkanVolumeDanTakAdaLanjutOtomatis() {
        val s = DuckingFocusState()
        s.apply(FocusAction.DUCK)
        val e = s.apply(FocusAction.PAUSE_PERMANENT)
        assertTrue(e.pause)
        assertTrue(e.abandon)
        assertTrue(e.unduck)
        assertEquals(FocusEffects(), s.apply(FocusAction.RESUME_AND_UNDUCK))
    }

    @Test
    fun state_fokusTertunda_gainMelanjutkan() {
        val s = DuckingFocusState()
        s.holdForDelayedFocus()
        assertTrue(s.apply(FocusAction.RESUME_AND_UNDUCK).resume)
    }

    @Test
    fun state_reset_mengembalikanButuhPulihVolumeDanMembersihkan() {
        val s = DuckingFocusState()
        s.apply(FocusAction.DUCK)
        s.apply(FocusAction.PAUSE)
        assertTrue(s.reset())
        assertFalse(s.ducked)
        assertFalse(s.pausedByFocus)
        assertFalse(s.reset())
    }
}
