package com.rudi.audioplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/** Batch 726 — format tampilan Info Lagu. Fungsi murni (tanpa Context), jalan di JVM biasa. */
class SongInfoFormatTest {

    @Test
    fun `ukuran berkas memilih satuan yang pas`() {
        assertEquals("512 B", SongInfoFormat.fileSize(512, Locale.US))
        assertEquals("1.0 KB", SongInfoFormat.fileSize(1024, Locale.US))
        assertEquals("1.8 MB", SongInfoFormat.fileSize(1_869_992, Locale.US))
        assertEquals("45.0 MB", SongInfoFormat.fileSize(47_139_304, Locale.US))
        assertEquals("2.00 GB", SongInfoFormat.fileSize(2L * 1024 * 1024 * 1024, Locale.US))
    }

    @Test
    fun `ekstensi didahulukan - opus dan ogg tidak tertukar walau MIME sama`() {
        assertEquals("Opus (OGG)", SongInfoFormat.container("audio/ogg", "LUA NA PRACA.opus"))
        assertEquals("OGG", SongInfoFormat.container("audio/ogg", "lagu.ogg"))
        assertEquals("WAV", SongInfoFormat.container("audio/x-wav", "bxkq - Aria Math.wav"))
    }

    @Test
    fun `tanpa ekstensi dikenal jatuh ke MIME lalu ke tidak diketahui`() {
        assertEquals("MP3", SongInfoFormat.container("audio/mpeg", null))
        assertEquals("FLAC", SongInfoFormat.container("AUDIO/FLAC", "tanpa_ekstensi"))
        assertEquals("AAC (M4A)", SongInfoFormat.container("audio/mp4", ""))
        assertEquals("MIDI", SongInfoFormat.container("audio/x-midi", null))
        assertEquals("Tidak diketahui", SongInfoFormat.container(null, null))
        assertEquals("Tidak diketahui", SongInfoFormat.container("  ", "x.bin"))
    }

    @Test
    fun `bitrate dibulatkan ke kbps dan nol atau null disembunyikan`() {
        assertEquals("128 kbps", SongInfoFormat.bitrate(128_000))
        assertEquals("320 kbps", SongInfoFormat.bitrate(319_600))
        assertNull(SongInfoFormat.bitrate(0))
        assertNull(SongInfoFormat.bitrate(null))
    }

    @Test
    fun `bitrate rata-rata dari ukuran dan durasi - seketika tanpa probe`() {
        assertEquals("128 kbps", SongInfoFormat.averageBitrate(5_760_000L, 360_000L))
        // Nilai nyata dari log uji v726: 125,9 MiB (132_018_000 B) / 128:11 (7_691_000 ms) = 137 kbps.
        assertEquals("137 kbps", SongInfoFormat.averageBitrate(132_018_000L, 7_691_000L))
        assertEquals("1067 kbps", SongInfoFormat.averageBitrate(4_000_000L, 30_000L))
        assertNull(SongInfoFormat.averageBitrate(0L, 360_000L))
        assertNull(SongInfoFormat.averageBitrate(5_760_000L, 0L))
        assertNull(SongInfoFormat.averageBitrate(-1L, -1L))
    }

    @Test
    fun `sample rate dan kedalaman bit`() {
        assertEquals("48 kHz", SongInfoFormat.sampleRate(48_000, Locale.US))
        assertEquals("44.1 kHz", SongInfoFormat.sampleRate(44_100, Locale.US))
        assertNull(SongInfoFormat.sampleRate(0, Locale.US))
        assertNull(SongInfoFormat.sampleRate(null, Locale.US))
        assertEquals("16-bit", SongInfoFormat.bitDepth(16))
        assertNull(SongInfoFormat.bitDepth(0))
        assertNull(SongInfoFormat.bitDepth(null))
    }
}
