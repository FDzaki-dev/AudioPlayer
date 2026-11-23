package com.rudi.audioplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Batch 657 — geometri pil bilah tab (jari -> indeks/pusat pil, tepi kiri pil) yang dipakai gestur & lapisan warna ter-klip. */
class TabPillMathTest {

    private val eps = 0.0001f

    @Test
    fun `indeks tab dari koordinat jari dibagi rata dan dijepit`() {
        assertEquals(0, tabIndexAt(x = 0f, width = 300f, count = 3))
        assertEquals(0, tabIndexAt(x = 99.9f, width = 300f, count = 3))
        assertEquals(1, tabIndexAt(x = 100f, width = 300f, count = 3))
        assertEquals(2, tabIndexAt(x = 299.9f, width = 300f, count = 3))
        assertEquals(2, tabIndexAt(x = 300f, width = 300f, count = 3))
        assertEquals(0, tabIndexAt(x = -40f, width = 300f, count = 3))
        assertEquals(2, tabIndexAt(x = 900f, width = 300f, count = 3))
    }

    @Test
    fun `indeks tab dengan lebar atau jumlah tidak valid kembali 0`() {
        assertEquals(0, tabIndexAt(x = 10f, width = 0f, count = 3))
        assertEquals(0, tabIndexAt(x = 10f, width = 300f, count = 0))
    }

    @Test
    fun `pusat pil mengikuti jari dan dijepit ke pusat tab pertama dan terakhir`() {
        assertEquals(0.5f, tabPillTarget(x = 0f, width = 300f, count = 3), eps)
        assertEquals(0.5f, tabPillTarget(x = 50f, width = 300f, count = 3), eps)
        assertEquals(1.0f, tabPillTarget(x = 100f, width = 300f, count = 3), eps)
        assertEquals(1.5f, tabPillTarget(x = 150f, width = 300f, count = 3), eps)
        assertEquals(2.5f, tabPillTarget(x = 250f, width = 300f, count = 3), eps)
        assertEquals(2.5f, tabPillTarget(x = 300f, width = 300f, count = 3), eps)
    }

    @Test
    fun `pusat pil dengan lebar tidak valid kembali pusat tab pertama`() {
        assertEquals(0.5f, tabPillTarget(x = 10f, width = 0f, count = 3), eps)
    }

    @Test
    fun `tepi kiri pil sama dengan rumus kunci timbul`() {
        // bar 300 px, 3 tab -> slot 100, pil 74% = 74 px; pusat tab ke-2 (1,5) -> 150 - 37 = 113
        assertEquals(113f, tabPillLeft(position = 1.5f, barWidth = 300f, count = 3, widthFraction = 0.74f), eps)
        // pusat tab pertama (0,5) -> 50 - 37 = 13 (di dalam bar, tidak menyentuh tepi)
        assertEquals(13f, tabPillLeft(position = 0.5f, barWidth = 300f, count = 3, widthFraction = 0.74f), eps)
    }

    @Test
    fun `tepi kiri pil dijepit agar tidak keluar bar`() {
        assertEquals(0f, tabPillLeft(position = -2f, barWidth = 300f, count = 3, widthFraction = 0.74f), eps)
        assertEquals(226f, tabPillLeft(position = 9f, barWidth = 300f, count = 3, widthFraction = 0.74f), eps)
    }

    @Test
    fun `label yang sudah muat tidak disusutkan`() {
        assertEquals(1f, navLabelFitScale(naturalPx = 90, availablePx = 96, minScale = 0.75f), eps)
        assertEquals(1f, navLabelFitScale(naturalPx = 96, availablePx = 96, minScale = 0.75f), eps)
    }

    @Test
    fun `label yang terlalu lebar diskalakan tepat sebesar lebar tersedia per lebar alami`() {
        // lebar alami 120 px, slot 96 px -> skala 0,8 (teks tersusut persis muat)
        assertEquals(0.8f, navLabelFitScale(naturalPx = 120, availablePx = 96, minScale = 0.75f), eps)
    }

    @Test
    fun `label tidak pernah diskalakan di bawah lantai`() {
        assertEquals(0.75f, navLabelFitScale(naturalPx = 400, availablePx = 96, minScale = 0.75f), eps)
    }

    @Test
    fun `ukuran tak valid atau lantai di atas satu mengembalikan skala satu`() {
        assertEquals(1f, navLabelFitScale(naturalPx = 0, availablePx = 96, minScale = 0.75f), eps)
        assertEquals(1f, navLabelFitScale(naturalPx = 120, availablePx = 0, minScale = 0.75f), eps)
        assertEquals(1f, navLabelFitScale(naturalPx = 120, availablePx = 96, minScale = 1.5f), eps)
    }

    // Batch 725 — kontrak ukuran bilah/pil/wajah ("bilah tab kebesaran, tidak fit dengan pil geser").

    @Test
    fun `wajah tab muat penuh di dalam pil dan pil muat di dalam bilah`() {
        val pillHeight = NAV_BAR_HEIGHT_DP * TAB_PILL_HEIGHT_FRACTION
        assertTrue("wajah ${navFaceHeightDp()} > pil $pillHeight", navFaceHeightDp() <= pillHeight)
        assertTrue("pil $pillHeight > bilah $NAV_BAR_HEIGHT_DP", pillHeight < NAV_BAR_HEIGHT_DP)
        // ruang di atas/bawah pil dibatasi: bilah tidak boleh jauh lebih besar dari pil (dulu 80 vs 49,6)
        assertTrue(NAV_BAR_HEIGHT_DP - pillHeight <= 12f)
    }

    @Test
    fun `slot sentuh minimum tidak melebihi bilah dan tidak lebih kecil dari wajah`() {
        assertTrue(NAV_TAB_MIN_TOUCH_DP <= NAV_BAR_HEIGHT_DP)
        assertTrue(NAV_TAB_MIN_TOUCH_DP >= navFaceHeightDp())
    }

    @Test
    fun `tinggi wajah dihitung dari padding ikon jarak dan baris label`() {
        assertEquals(46f, navFaceHeightDp(), eps)
    }

    @Test
    fun `lebar maksimum wajah adalah pecahan lebar slot`() {
        assertEquals(98, tabFaceMaxWidth(slotPx = 117, widthFraction = 0.84f))
        assertEquals(100, tabFaceMaxWidth(slotPx = 100, widthFraction = 1f))
        assertEquals(100, tabFaceMaxWidth(slotPx = 100, widthFraction = 3f))
    }

    @Test
    fun `lebar maksimum wajah dengan ukuran tak valid kembali nol`() {
        assertEquals(0, tabFaceMaxWidth(slotPx = 0, widthFraction = 0.84f))
        assertEquals(0, tabFaceMaxWidth(slotPx = -5, widthFraction = 0.84f))
        assertEquals(0, tabFaceMaxWidth(slotPx = 100, widthFraction = 0f))
    }

    @Test
    fun `tepi kiri pil memakai pecahan lebar pil terbaru`() {
        // bar 300 px, 3 tab -> slot 100, pil 84% = 84 px; pusat tab ke-2 (1,5) -> 150 - 42 = 108
        assertEquals(108f, tabPillLeft(position = 1.5f, barWidth = 300f, count = 3, widthFraction = TAB_PILL_WIDTH_FRACTION), eps)
    }
}
