package com.rudi.audioplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogOrderTest {

    private val one = "2026-10-13 08:00:01 WARN [A] satu"
    private val two = "2026-10-13 08:00:02 WARN [A] dua"
    private val three = "2026-10-13 08:00:03 ERROR [B] tiga"

    @Test
    fun `entri terbaru berada paling atas`() {
        val raw = "$one\n$two\n$three\n"

        assertEquals("$three\n$two\n$one\n", LogOrder.newestFirst(raw))
    }

    @Test
    fun `entri multi baris tetap utuh dan urutan barisnya tidak terbalik`() {
        val multi = "2026-10-13 08:00:01 ERROR [A] gagal: java.lang.IllegalStateException: x\n" +
            "\tat a.B.c(B.kt:1)\n" +
            "\tat a.B.d(B.kt:2)"
        val raw = "$multi\n$two\n"

        assertEquals("$two\n$multi\n", LogOrder.newestFirst(raw))
    }

    @Test
    fun `baris kosong di dalam satu entri tidak memecahnya`() {
        val trace = "2026-10-13 08:00:01 ERROR [ApplicationExitInfo] waktu_mati=x\n" +
            "--- trace ANR ---\n" +
            "Subject: y\n" +
            "\n" +
            "\n" +
            "--- CriticalEventLog ---"
        val raw = "$trace\n$two\n"

        assertEquals("$two\n$trace\n", LogOrder.newestFirst(raw))
    }

    @Test
    fun `sisa entri terpotong di awal berkas tampil paling bawah`() {
        val orphan = "\tat potongan.Lama(Lama.kt:9)"
        val raw = "$orphan\n$one\n$two\n"

        assertEquals("$two\n$one\n$orphan\n", LogOrder.newestFirst(raw))
    }

    @Test
    fun `log kosong atau hanya spasi dikembalikan apa adanya`() {
        assertEquals("", LogOrder.newestFirst(""))
        assertEquals("  \n", LogOrder.newestFirst("  \n"))
    }

    // Batch 679 — keepNewestHalf (pemangkasan berkas log per entri berbasis ukuran).

    private fun entry(n: Int) = "2026-10-13 08:00:%02d WARN [A] entri-%02d ${"x".repeat(40)}\n".format(n, n)

    @Test
    fun `keepNewestHalf mempertahankan entri terbaru dan mulai tepat di awal entri`() {
        val raw = (0 until 10).joinToString("") { entry(it) }

        val kept = LogOrder.keepNewestHalf(raw)

        assertEquals(entry(5) + entry(6) + entry(7) + entry(8) + entry(9), kept)
    }

    @Test
    fun `keepNewestHalf membuang entri multi baris yang melintasi titik tengah secara utuh`() {
        val trace = "2026-10-13 08:00:30 ERROR [B] gagal: java.lang.IllegalStateException: x\n" +
            "\tat a.B.c(B.kt:1)\n" +
            "\tat a.B.d(B.kt:2)\n" +
            "\tat a.B.e(B.kt:3)\n"
        val raw = entry(1) + trace + entry(2) + entry(3)

        val kept = LogOrder.keepNewestHalf(raw)

        assertEquals(entry(2) + entry(3), kept)
        assertTrue(kept.startsWith("2026-"))
    }

    @Test
    fun `keepNewestHalf pada satu entri raksasa memotong di batas baris dan pasti mengecil`() {
        val giant = "2026-10-13 08:00:01 ERROR [A] raksasa\n" + (0 until 40).joinToString("") { "\tat baris.ke$it(F.kt:$it)\n" }

        val kept = LogOrder.keepNewestHalf(giant)

        assertTrue(kept.length < giant.length)
        assertTrue(kept.endsWith("\n"))
        assertTrue(kept.startsWith("\tat baris."))
    }

    @Test
    fun `keepNewestHalf pada teks kosong atau tanpa baris baru tidak melempar`() {
        assertEquals("", LogOrder.keepNewestHalf(""))
        assertEquals("abcdef".substring(3), LogOrder.keepNewestHalf("abcdef"))
    }

    @Test
    fun `hasil keepNewestHalf tetap bisa dibaca newestFirst tanpa entri yatim`() {
        val raw = (0 until 8).joinToString("") { entry(it) }

        val shown = LogOrder.newestFirst(LogOrder.keepNewestHalf(raw))

        assertEquals(entry(7) + entry(6) + entry(5) + entry(4), shown)
    }
}
