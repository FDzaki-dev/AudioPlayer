package com.rudi.audioplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class DebugLogFormatTest {

    @Test
    fun `banjir AudioEffect command dan getParameter dibuang`() {
        assertTrue(DebugLogFormat.isNoise("11-06 14:22:02.123 D/AudioEffect( 1234): command() cmdCode = 65536"))
        assertTrue(DebugLogFormat.isNoise("11-06 14:22:02.123 V/AudioEffect( 1234): commandExecuted() status 0"))
        assertTrue(DebugLogFormat.isNoise("11-06 14:22:02.123 V/AudioEffect( 1234): getParameter: param: 1, param2: 2"))
    }

    @Test
    fun `derau UI sistem dibuang`() {
        assertTrue(DebugLogFormat.isNoise("11-06 14:22:02.123 D/VRI[MainActivity](12345): draw"))
        assertTrue(DebugLogFormat.isNoise("11-06 14:22:02.123 D/DecorView( 1234): onWindowFocusChanged"))
    }

    @Test
    fun `baris bermakna tetap dipertahankan`() {
        assertFalse(DebugLogFormat.isNoise("11-06 14:22:02.123 W/AudioEffect( 1234): setParameter(): param 3"))
        assertFalse(DebugLogFormat.isNoise("11-06 14:22:02.123 E/AndroidRuntime( 1234): FATAL EXCEPTION: main"))
        assertFalse(DebugLogFormat.isNoise("11-06 14:22:02.123 W/EqualizerController( 1234): attach gagal"))
        assertFalse(DebugLogFormat.isNoise("baris tanpa timestamp logcat"))
    }

    @Test
    fun `nama berkas memakai awalan dan stempel waktu`() {
        assertEquals(
            "sonix_debug_20261107_090503.txt",
            DebugLogFormat.fileName(LocalDateTime.of(2026, 11, 7, 9, 5, 3))
        )
    }

    @Test
    fun `laporan mempertahankan baris logcat PALING BARU dan menaruh logcat paling akhir`() {
        val report = DebugLogFormat.buildReport(
            header = listOf("H1", "H2"),
            sections = listOf("S1\n"),
            logLines = listOf("log-1", "log-2", "log-3", "log-4", "log-5"),
            maxLines = 3
        )

        assertTrue(report.startsWith("H1\nH2\nS1\n"))
        assertTrue(report.contains("(3 baris terakhir)"))
        assertTrue(report.contains("log-3\nlog-4\nlog-5\n"))
        assertFalse(report.contains("log-1"))
        assertFalse(report.contains("log-2"))
        assertTrue(report.indexOf("S1") < report.indexOf("---- logcat SONIX"))
    }

    @Test
    fun `laporan dengan logcat kosong memberi petunjuk`() {
        val report = DebugLogFormat.buildReport(listOf("H"), emptyList(), emptyList(), 10)

        assertTrue(report.contains("(0 baris terakhir)"))
        assertTrue(report.contains("(kosong:"))
    }

    @Test
    fun `bagian log diagnostik mengambil baris pertama yaitu entri terbaru`() {
        val section = DebugLogFormat.buildDiagnosticSection("baru\ntengah\nlama\n", 2)

        assertTrue(section.contains("2 dari 3 baris"))
        assertTrue(section.contains("baru\ntengah\n"))
        assertFalse(section.contains("lama"))
    }

    @Test
    fun `bagian log diagnostik kosong ditandai`() {
        val section = DebugLogFormat.buildDiagnosticSection("  \n", 10)

        assertTrue(section.contains("0 dari 0 baris"))
        assertTrue(section.contains("(kosong)"))
    }

    @Test
    fun `label tipe efek dikenal tanpa peduli huruf besar dan spasi`() {
        assertEquals("Equalizer", DebugLogFormat.effectTypeLabel("  0BED4300-DDD6-11DB-8F34-0002A5D5C51B "))
        assertEquals(DebugLogFormat.UNKNOWN_EFFECT_TYPE, DebugLogFormat.effectTypeLabel("00000000-0000-0000-0000-000000000000"))
    }

    @Test
    fun `bagian efek meringkas per tipe dan melaporkan Environmental Reverb`() {
        val effects = listOf(
            DebugLogFormat.EffectInfo("0bed4300-ddd6-11db-8f34-0002a5d5c51b", "u1", "insert", "Equalizer", "AOSP"),
            DebugLogFormat.EffectInfo("11111111-1111-1111-1111-111111111111", "u2", "insert", "Vendor FX", "ACME")
        )

        val section = DebugLogFormat.buildEffectsSection(effects, null)

        assertTrue(section.contains("2 efek"))
        assertTrue(section.contains("Equalizer x1"))
        assertTrue(section.contains("${DebugLogFormat.UNKNOWN_EFFECT_TYPE} x1"))
        assertTrue(section.contains("Environmental Reverb tersedia: TIDAK"))
    }

    @Test
    fun `bagian efek gagal dibaca memuat alasan`() {
        assertTrue(DebugLogFormat.buildEffectsSection(null, "SecurityException: x").contains("(gagal dibaca: SecurityException: x)"))
        assertTrue(DebugLogFormat.buildEffectsSection(null, null).contains("mengembalikan null"))
    }

    @Test
    fun `batas baris logcat tetap wajar agar berkas mudah dikirim`() {
        assertTrue(DebugLogFormat.MAX_LINES in 1_000..20_000)
    }
}
