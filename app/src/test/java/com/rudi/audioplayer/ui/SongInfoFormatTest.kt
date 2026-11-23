package com.rudi.audioplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/**
 * Batch 726 — format tampilan Info Lagu. Fungsi murni (tanpa Context), jalan di JVM biasa.
 */
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

    // ---- Batch 728: header OGG (sample rate tanpa memindai file) ----

    /** Halaman OGG pertama minimal: "OggS" + 22 byte header tetap + 1 segmen berisi [payload]. */
    private fun oggPage(payload: ByteArray): ByteArray {
        val header = ByteArray(27)
        "OggS".forEachIndexed { i, c -> header[i] = c.code.toByte() }
        header[5] = 0x02 // awal stream
        header[26] = 1 // jumlah segmen
        return header + byteArrayOf(payload.size.toByte()) + payload
    }

    private fun le32(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())

    private fun opusHead(inputRate: Int): ByteArray =
        "OpusHead".toByteArray(Charsets.US_ASCII) + byteArrayOf(1, 2, 0x38, 0x01) + le32(inputRate) + byteArrayOf(0, 0, 0)

    private fun vorbisId(rate: Int): ByteArray =
        byteArrayOf(1) + "vorbis".toByteArray(Charsets.US_ASCII) + le32(0) + byteArrayOf(2) + le32(rate) +
            le32(0) + le32(128_000) + le32(0) + byteArrayOf(0xB8.toByte(), 1)

    @Test
    fun `header opus selalu 48 kHz walau input_sample_rate lain`() {
        assertEquals(48_000, OggHeader.sampleRateHz(oggPage(opusHead(44_100))))
        assertEquals(48_000, OggHeader.sampleRateHz(oggPage(opusHead(48_000))))
    }

    @Test
    fun `header vorbis membaca sample rate little-endian`() {
        assertEquals(44_100, OggHeader.sampleRateHz(oggPage(vorbisId(44_100))))
        assertEquals(48_000, OggHeader.sampleRateHz(oggPage(vorbisId(48_000))))
        assertEquals(22_050, OggHeader.sampleRateHz(oggPage(vorbisId(22_050))))
    }

    @Test
    fun `bukan OGG atau terpotong atau paket lain menghasilkan null`() {
        assertNull(OggHeader.sampleRateHz("ID3\u0004\u0000\u0000\u0000\u0000\u0000\u0000padding-padding-padding".toByteArray(Charsets.ISO_8859_1)))
        assertNull(OggHeader.sampleRateHz(ByteArray(0)))
        val opus = oggPage(opusHead(48_000))
        assertNull(OggHeader.sampleRateHz(opus, opus.size - 1)) // OpusHead terpotong
        val vorbis = oggPage(vorbisId(44_100))
        assertNull(OggHeader.sampleRateHz(vorbis, 27 + 1 + 15)) // Vorbis terpotong sebelum sample rate lengkap
        val flacInOgg = oggPage(byteArrayOf(0x7F) + "FLAC".toByteArray(Charsets.US_ASCII) + ByteArray(30))
        assertNull(OggHeader.sampleRateHz(flacInOgg))
        assertNull(OggHeader.sampleRateHz(oggPage(vorbisId(0)))) // sample rate 0 tidak valid
    }

    @Test
    fun `mightBeOgg dari MIME atau ekstensi`() {
        assertEquals(true, OggHeader.mightBeOgg("audio/ogg", null))
        assertEquals(true, OggHeader.mightBeOgg("AUDIO/OGG", "x.bin"))
        assertEquals(true, OggHeader.mightBeOgg(null, "lagu.OPUS"))
        assertEquals(true, OggHeader.mightBeOgg("application/octet-stream", "lagu.oga"))
        assertEquals(false, OggHeader.mightBeOgg("audio/mpeg", "lagu.mp3"))
        assertEquals(false, OggHeader.mightBeOgg(null, null))
    }
}
