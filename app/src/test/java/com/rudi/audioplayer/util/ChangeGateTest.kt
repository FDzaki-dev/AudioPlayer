package com.rudi.audioplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeGateTest {

    @Test
    fun `kunci baru selalu dianggap berubah`() {
        val gate = ChangeGate()
        assertTrue(gate.changed(220, "slim|290x46|290x46|1.15|true"))
    }

    @Test
    fun `tanda sama berturut turut tidak ditulis ulang`() {
        val gate = ChangeGate()
        assertTrue(gate.changed(220, "slim|290x46|290x46|1.15|true"))
        assertFalse(gate.changed(220, "slim|290x46|290x46|1.15|true"))
        assertFalse(gate.changed(220, "slim|290x46|290x46|1.15|true"))
    }

    @Test
    fun `tanda berbeda ditulis lalu tanda baru jadi patokan`() {
        val gate = ChangeGate()
        assertTrue(gate.changed(221, "penuh|290x129|290x129|1.15|true"))
        assertTrue(gate.changed(221, "penuh|290x129|290x129|1.15|false")) // judul pendek: teks muat di kolom
        assertFalse(gate.changed(221, "penuh|290x129|290x129|1.15|false"))
    }

    @Test
    fun `kunci saling bebas`() {
        val gate = ChangeGate()
        assertTrue(gate.changed(220, "x"))
        assertTrue(gate.changed(221, "x")) // widget lain dengan tanda yang sama tetap ditulis sekali
        assertFalse(gate.changed(220, "x"))
        assertFalse(gate.changed(221, "x"))
    }

    @Test
    fun `A ke B ke A ditulis tiga kali karena dibanding tanda terakhir`() {
        val gate = ChangeGate()
        assertTrue(gate.changed(1, "A"))
        assertTrue(gate.changed(1, "B"))
        assertTrue(gate.changed(1, "A"))
    }

    @Test
    fun `pola nyata log v680 proses 06 39 enam pembaruan dua widget tertulis dua baris`() {
        // Marquee diag #1..#3 untuk id=220 (slim 290x46) dan id=221 (penuh 290x129), fontScale 1.15, judul 61 huruf
        // (teks 427 dp > kolom): pembaruan #1 berubah=true, #2/#3 berubah=false (play/jeda) -> tanda identik.
        val gate = ChangeGate()
        val sigSlim = "slim|290x46|290x46|1.15|true"
        val sigPenuh = "penuh|290x129|290x129|1.15|true"
        var ditulis = 0
        repeat(3) {
            if (gate.changed(220, sigSlim)) ditulis++
            if (gate.changed(221, sigPenuh)) ditulis++
        }
        assertEquals(2, ditulis)
    }

    @Test
    fun `proses baru memakai pintu baru sehingga baris pertama tertulis lagi`() {
        val proses1 = ChangeGate()
        val proses2 = ChangeGate() // pintu hidup di memori proses: restart = objek baru
        assertTrue(proses1.changed(220, "s"))
        assertFalse(proses1.changed(220, "s"))
        assertTrue(proses2.changed(220, "s"))
    }
}
