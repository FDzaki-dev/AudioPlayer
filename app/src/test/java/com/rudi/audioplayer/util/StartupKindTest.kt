package com.rudi.audioplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupKindTest {

    private fun isHeadless(appDone: Long, activityStart: Long) =
        StartupKind.describe(appDone, activityStart).contains("jenis=PROSES_HIDUP_TANPA_UI")

    @Test
    fun `start dingin murni nyata dari log v677 dibaca murni`() {
        // app_selesai -> act_mulai: 42->75, 45->83, 49->85, 48->89 (jeda 33/38/36/41 ms)
        assertEquals(" | jenis=start_dingin_murni(jeda app_selesai->act_mulai=33ms)", StartupKind.describe(42, 75))
        assertTrue(!isHeadless(45, 83))
        assertTrue(!isHeadless(49, 85))
        assertTrue(!isHeadless(48, 89))
    }

    @Test
    fun `proses pasca instal 1860 ms dari log v679 dibaca hidup tanpa UI`() {
        assertEquals(
            " | jenis=PROSES_HIDUP_TANPA_UI(jeda app_selesai->act_mulai=1860ms; BUKAN start dingin murni)",
            StartupKind.describe(375, 2235)
        )
    }

    @Test
    fun `proses hidup lama tanpa UI dibaca hidup tanpa UI`() {
        assertTrue(isHeadless(348, 11828)) // pid 8027 v677: 11480 ms
        assertTrue(isHeadless(393, 19509555)) // proses hidup ±5,4 jam sebelum Activity dibuka
    }

    @Test
    fun `batas ambang tepat 500 ms`() {
        assertTrue(!isHeadless(0, StartupKind.HEADLESS_GAP_MS - 1))
        assertTrue(isHeadless(0, StartupKind.HEADLESS_GAP_MS))
    }
}
