package com.rudi.audioplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupStallFormatTest {

    @Test
    fun `nsToMs membagi sejuta dan membuang sisa`() {
        assertEquals(258L, StartupStallFormat.nsToMs(258_400_000L))
        assertEquals(0L, StartupStallFormat.nsToMs(999_999L))
    }

    @Test
    fun `messageLine membuang awalan Looper dan memuat durasi serta waktu`() {
        val raw = ">>>>> Dispatching to Handler (android.app.ActivityThread\$H) {abc} null: 159"
        assertEquals(
            "Pesan thread utama lambat: 233ms (selesai t=+2294ms sejak proses mulai) | " +
                "Handler (android.app.ActivityThread\$H) {abc} null: 159",
            StartupStallFormat.messageLine(2294, 233, raw)
        )
    }

    @Test
    fun `messageLine memotong teks mentah yang panjang`() {
        val raw = StartupStallFormat.DISPATCH_PREFIX + "x".repeat(300)
        val line = StartupStallFormat.messageLine(1, 100, raw)
        assertTrue(line.endsWith("x".repeat(200) + "..."))
        assertFalse(line.contains("x".repeat(201)))
    }

    @Test
    fun `frameLine memuat semua fase dalam milidetik`() {
        val sample = FrameSample(
            totalNs = 258_000_000,
            vsyncDelayNs = 190_000_000,
            inputNs = 1_000_000,
            animationNs = 2_000_000,
            layoutNs = 30_000_000,
            drawNs = 25_000_000,
            syncNs = 5_000_000,
            issueNs = 3_000_000,
            swapNs = 2_000_000,
            unknownNs = 0,
            firstDraw = true,
            dropCount = 31
        )
        assertEquals(
            "Frame lambat: total=258ms (t=+2452ms sejak proses mulai) frame_pertama=ya drop=31 | " +
                "tunda_vsync=190ms input=1ms animasi=2ms layout=30ms gambar=25ms sinkron=5ms " +
                "perintah=3ms swap=2ms tak_terhitung=0ms",
            StartupStallFormat.frameLine(2452, sample)
        )
    }

    @Test
    fun `tanpa stall besar tidak ada baris yang ditulis`() {
        val collector = StallCollector()
        collector.add(100, StartupStallFormat.MESSAGE_TRIGGER_MS, "pesan sedang")
        collector.add(180, StartupStallFormat.FRAME_TRIGGER_MS, "frame sedang")
        assertTrue(collector.drain().isEmpty())
    }

    @Test
    fun `entri di bawah ambang minimum tidak disimpan`() {
        val collector = StallCollector()
        collector.add(59, StartupStallFormat.MESSAGE_TRIGGER_MS, "kecil")
        collector.add(200, StartupStallFormat.MESSAGE_TRIGGER_MS, "besar")
        assertEquals(listOf("besar"), collector.drain())
    }

    @Test
    fun `ada stall besar maka seluruh entri keluar berurutan`() {
        val collector = StallCollector()
        collector.add(70, StartupStallFormat.MESSAGE_TRIGGER_MS, "1")
        collector.add(160, StartupStallFormat.MESSAGE_TRIGGER_MS, "2")
        collector.add(90, StartupStallFormat.FRAME_TRIGGER_MS, "3")
        assertEquals(listOf("1", "2", "3"), collector.drain())
    }

    @Test
    fun `batas baris dihormati tetapi stall besar tetap tersimpan`() {
        val collector = StallCollector()
        repeat(StartupStallFormat.MAX_LINES) { collector.add(70, StartupStallFormat.MESSAGE_TRIGGER_MS, "n$it") }
        collector.add(75, StartupStallFormat.MESSAGE_TRIGGER_MS, "biasa-lewat-batas") // dibuang: batas penuh
        assertTrue(collector.drain().isEmpty()) // belum ada pemicu
        collector.add(300, StartupStallFormat.MESSAGE_TRIGGER_MS, "besar-lewat-batas")
        val out = collector.drain()
        assertEquals(StartupStallFormat.MAX_LINES + 1, out.size)
        assertEquals("besar-lewat-batas", out.last())
        assertFalse(out.contains("biasa-lewat-batas"))
    }

    @Test
    fun `pola nyata v681 peluncuran Activity 233ms memicu dan frame pertama ikut tercatat`() {
        // v681 (pid 27485): act_mulai=2061ms -> frame1_predraw=2294ms = 233 ms; frame1_selesai=2452ms (frame ~158 ms).
        val collector = StallCollector()
        collector.add(233, StartupStallFormat.MESSAGE_TRIGGER_MS, "peluncuran")
        collector.add(158, StartupStallFormat.FRAME_TRIGGER_MS, "frame pertama")
        assertEquals(listOf("peluncuran", "frame pertama"), collector.drain())
        // v677 start dingin murni: act_mulai=89 -> frame1_predraw=202 = 113 ms (< 150) dan frame ~123 ms (< 200): senyap.
        val normal = StallCollector()
        normal.add(113, StartupStallFormat.MESSAGE_TRIGGER_MS, "peluncuran")
        normal.add(123, StartupStallFormat.FRAME_TRIGGER_MS, "frame pertama")
        assertTrue(normal.drain().isEmpty())
    }
}
