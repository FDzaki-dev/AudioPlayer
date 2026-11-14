package com.rudi.audioplayer

import org.junit.Assert.assertEquals
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
}
