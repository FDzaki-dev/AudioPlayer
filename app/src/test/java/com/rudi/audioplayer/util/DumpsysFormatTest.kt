package com.rudi.audioplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DumpsysFormatTest {

    @Test
    fun `filterAround menyimpan baris cocok plus konteks dan memisahkan kelompok`() {
        val lines = listOf("a", "b", "X1", "c", "d", "e", "f", "X2", "g")
        val out = DumpsysFormat.filterAround(lines, "X", 1)
        assertEquals(listOf("b", "X1", "c", DumpsysFormat.GAP_MARK, "f", "X2", "g"), out)
    }

    @Test
    fun `filterAround tanpa baris cocok mengembalikan kosong`() {
        assertTrue(DumpsysFormat.filterAround(listOf("a", "b"), "Z", 2).isEmpty())
    }

    @Test
    fun `capLines memotong dan mencatat sisa`() {
        val out = DumpsysFormat.capLines(listOf("1", "2", "3", "4", "5"), 3)
        assertEquals(listOf("1", "2", "3", "(... 2 baris lain dipotong)"), out)
        assertEquals(listOf("1", "2"), DumpsysFormat.capLines(listOf("1", "2"), 3))
    }

    @Test
    fun `isDenied mengenali penolakan izin`() {
        assertTrue(DumpsysFormat.isDenied(listOf("Permission Denial: can't dump meminfo from pid=1")))
        assertFalse(DumpsysFormat.isDenied(listOf("Applications Memory Usage (in Kilobytes):")))
    }

    @Test
    fun `buildSection memuat judul dan isi`() {
        val text = DumpsysFormat.buildSection(listOf("meminfo", "pkg"), listOf("baris1", "baris2"), null, 0, 10, false, null)
        assertTrue(text.startsWith("---- dumpsys meminfo pkg ----"))
        assertTrue(text.contains("baris1"))
    }

    @Test
    fun `statusSection menyebut akses tidak ada beserta perintahnya`() {
        val text = DumpsysFormat.statusSection("TIDAK ADA", false, "adb shell pm grant x android.permission.DUMP", listOf("Audio"), 13)
        assertTrue(text.contains("akses dumpsys: TIDAK ADA"))
        assertTrue(text.contains("adb shell pm grant x"))
        assertTrue(text.contains("(1 dari 13)"))
    }

    @Test
    fun `statusSection dengan akses tidak menampilkan petunjuk pemberian izin`() {
        val text = DumpsysFormat.statusSection("lewat Shizuku", true, "adb shell pm grant x android.permission.DUMP", listOf("Audio", "Memori"), 13)
        assertTrue(text.contains("akses dumpsys: lewat Shizuku"))
        assertFalse(text.contains("adb shell pm grant"))
    }
}
