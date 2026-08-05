package com.rudi.audioplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParserTest {

    // --- parse(): plain (unsynced) lyrics ---

    @Test
    fun `plain text without timestamps parses as unsynced lines`() {
        val result = LyricsParser.parse("Baris pertama\nBaris kedua")

        assertEquals(listOf(LyricLine(null, "Baris pertama"), LyricLine(null, "Baris kedua")), result)
    }

    @Test
    fun `blank lines are dropped, surrounding whitespace is trimmed`() {
        val result = LyricsParser.parse("  Halo dunia  \n\n   \nBaris lain")

        assertEquals(listOf(LyricLine(null, "Halo dunia"), LyricLine(null, "Baris lain")), result)
    }

    // --- parse(): LRC-synced lyrics, tiap lebar digit milidetik ---

    @Test
    fun `mm-ss without milliseconds parses with zero millis`() {
        val result = LyricsParser.parse("[01:02]Halo")

        assertEquals(listOf(LyricLine(62_000L, "Halo")), result)
    }

    @Test
    fun `one-digit milliseconds are scaled by 100`() {
        // [00:00.5] -> 0.5 detik -> 500ms, bukan 5ms
        val result = LyricsParser.parse("[00:00.5]Halo")

        assertEquals(500L, result.single().timeMs)
    }

    @Test
    fun `two-digit milliseconds are scaled by 10`() {
        // [00:00.50] -> 0.50 detik -> 500ms
        val result = LyricsParser.parse("[00:00.50]Halo")

        assertEquals(500L, result.single().timeMs)
    }

    @Test
    fun `three-digit milliseconds are used as-is`() {
        // [00:00.500] -> 500ms langsung, tanpa skala
        val result = LyricsParser.parse("[00:00.500]Halo")

        assertEquals(500L, result.single().timeMs)
    }

    @Test
    fun `minutes and seconds combine correctly with milliseconds`() {
        val result = LyricsParser.parse("[02:15.250]Reff")

        assertEquals(2 * 60_000L + 15_000L + 250L, result.single().timeMs)
    }

    @Test
    fun `lyric text after the timestamp bracket is trimmed`() {
        val result = LyricsParser.parse("[00:01.00]   Teks dengan spasi   ")

        assertEquals("Teks dengan spasi", result.single().text)
    }

    // --- parse(): baris yang tidak cocok pola LRC jatuh balik ke teks polos ---

    @Test
    fun `line with malformed bracket falls back to plain unsynced text`() {
        val result = LyricsParser.parse("[bukan timestamp]Halo")

        assertEquals(LyricLine(null, "[bukan timestamp]Halo"), result.single())
    }

    @Test
    fun `line with unclosed bracket falls back to plain unsynced text`() {
        val result = LyricsParser.parse("[01:02 Halo tanpa kurung tutup")

        assertEquals(LyricLine(null, "[01:02 Halo tanpa kurung tutup"), result.single())
    }

    // --- isSynced() ---

    @Test
    fun `isSynced is true only when every line carries a timestamp`() {
        val allSynced = LyricsParser.parse("[00:01.00]A\n[00:02.00]B")
        val mixed = LyricsParser.parse("[00:01.00]A\nB tanpa waktu")
        val noneSynced = LyricsParser.parse("A\nB")

        assertTrue(LyricsParser.isSynced(allSynced))
        assertFalse(LyricsParser.isSynced(mixed))
        assertFalse(LyricsParser.isSynced(noneSynced))
    }

    @Test
    fun `isSynced is false for an empty line list`() {
        assertFalse(LyricsParser.isSynced(emptyList()))
    }

    // --- currentLineIndex() ---

    @Test
    fun `currentLineIndex is -1 before the first cue`() {
        val lines = LyricsParser.parse("[00:10.00]A\n[00:20.00]B")

        assertEquals(-1, LyricsParser.currentLineIndex(lines, positionMs = 5_000L))
    }

    @Test
    fun `currentLineIndex matches the most recent cue at or before the position`() {
        val lines = LyricsParser.parse("[00:10.00]A\n[00:20.00]B\n[00:30.00]C")

        assertEquals(0, LyricsParser.currentLineIndex(lines, positionMs = 15_000L))
        assertEquals(1, LyricsParser.currentLineIndex(lines, positionMs = 20_000L))
        assertEquals(2, LyricsParser.currentLineIndex(lines, positionMs = 999_000L))
    }

    @Test
    fun `currentLineIndex skips over unsynced lines mixed into synced lyrics`() {
        // Baris tanpa waktu di tengah tidak boleh menghentikan pencarian cue berikutnya.
        val lines = LyricsParser.parse("[00:10.00]A\nCatatan tanpa waktu\n[00:20.00]B")

        assertEquals(2, LyricsParser.currentLineIndex(lines, positionMs = 25_000L))
    }

    @Test
    fun `currentLineIndex is -1 for fully unsynced lyrics`() {
        val lines = LyricsParser.parse("A\nB\nC")

        assertEquals(-1, LyricsParser.currentLineIndex(lines, positionMs = 999_000L))
    }
}
