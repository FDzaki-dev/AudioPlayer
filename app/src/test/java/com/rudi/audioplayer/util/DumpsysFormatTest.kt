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
    fun `isFailure mengenali Cant find service selain Permission Denial`() {
        assertTrue(DumpsysFormat.isFailure(listOf("Can't find service: meminfo")))
        assertTrue(DumpsysFormat.isFailure(listOf("Permission Denial: can't dump ActivityManager due to missing android.permission.PACKAGE_USAGE_STATS permission")))
        assertFalse(DumpsysFormat.isFailure(listOf("ACTIVITY MANAGER SERVICES (dumpsys activity services)", "  (nothing)")))
    }

    @Test
    fun `buildSection menandai layanan tidak ditemukan dan jalur cadangan`() {
        val gagal = DumpsysFormat.buildSection(listOf("meminfo", "pkg"), listOf("Can't find service: meminfo"), null, 0, 10, false, null)
        assertTrue(gagal.contains("(layanan tidak ditemukan)"))
        assertTrue(gagal.contains("Can't find service: meminfo"))
        val cadangan = DumpsysFormat.buildSection(
            listOf("meminfo", "pkg"), listOf("Total PSS by process:"), null, 0, 10, false, null, DumpsysFormat.ROUTE_SHIZUKU_FALLBACK
        )
        assertTrue(cadangan.contains("(jalur: ${DumpsysFormat.ROUTE_SHIZUKU_FALLBACK})"))
        assertTrue(cadangan.contains("Total PSS by process:"))
    }

    @Test
    fun `statusSection memuat baris rincian izin bila diberikan`() {
        val text = DumpsysFormat.statusSection(
            "izin DUMP langsung", true, "x", listOf("Audio"), 13, "izin: DUMP=ya, PACKAGE_USAGE_STATS=tidak; Shizuku=siap"
        )
        assertTrue(text.contains("izin: DUMP=ya, PACKAGE_USAGE_STATS=tidak; Shizuku=siap"))
    }

    @Test
    fun `statusSection dengan akses tidak menampilkan petunjuk pemberian izin`() {
        val text = DumpsysFormat.statusSection("lewat Shizuku", true, "adb shell pm grant x android.permission.DUMP", listOf("Audio", "Memori"), 13)
        assertTrue(text.contains("akses dumpsys: lewat Shizuku"))
        assertFalse(text.contains("adb shell pm grant"))
    }
}
