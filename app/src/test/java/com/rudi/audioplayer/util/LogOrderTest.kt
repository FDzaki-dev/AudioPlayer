package com.rudi.audioplayer.util

import org.junit.Assert.assertEquals
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
}
