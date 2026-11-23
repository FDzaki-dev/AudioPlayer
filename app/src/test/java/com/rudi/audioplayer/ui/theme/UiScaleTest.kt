package com.rudi.audioplayer.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiScaleTest {

    private val eps = 1e-5f

    @Test
    fun `kepadatan efektif = kepadatan asli x faktor`() {
        // Infinix X6850 (log v723): 1080 px lebar, 420 dpi -> density 2.625
        assertEquals(2.625f * UiScale.DENSITY_FACTOR, UiScale.density(2.625f), eps)
        assertEquals(UiScale.DENSITY_FACTOR, UiScale.density(1f), eps)
    }

    @Test
    fun `faktor kepadatan mengecilkan tampilan tetapi tidak sampai terlalu kecil`() {
        assertTrue(UiScale.DENSITY_FACTOR < 1f)
        assertTrue(UiScale.DENSITY_FACTOR >= 0.8f)
    }

    @Test
    fun `font sistem 115 persen dibatasi ke batas atas`() {
        // Nilai nyata dari log v723: fontScale=1.15
        assertEquals(UiScale.MAX_FONT_SCALE, UiScale.fontScale(1.15f), eps)
    }

    @Test
    fun `font sistem di atas batas selalu dipotong ke batas`() {
        assertEquals(UiScale.MAX_FONT_SCALE, UiScale.fontScale(2.0f), eps)
        assertEquals(UiScale.MAX_FONT_SCALE, UiScale.fontScale(UiScale.MAX_FONT_SCALE + 0.01f), eps)
    }

    @Test
    fun `font sistem pada atau di bawah batas dihormati apa adanya`() {
        assertEquals(UiScale.MAX_FONT_SCALE, UiScale.fontScale(UiScale.MAX_FONT_SCALE), eps)
        assertEquals(0.85f, UiScale.fontScale(0.85f), eps)
    }

    @Test
    fun `teks efektif per sp tidak pernah lebih besar dari baseline tanpa font sistem`() {
        // px per sp = density x fontScale. Dengan font sistem 1.15 (kasus user) hasilnya HARUS lebih kecil dari sebelum
        // skala ini; dengan font 1.0 harus tepat faktor kepadatan.
        val density = 2.625f
        val before = density * 1.15f
        val after = UiScale.density(density) * UiScale.fontScale(1.15f)
        assertTrue(after < before)
        assertEquals(density * UiScale.DENSITY_FACTOR, UiScale.density(density) * UiScale.fontScale(1.0f), eps)
    }
}
