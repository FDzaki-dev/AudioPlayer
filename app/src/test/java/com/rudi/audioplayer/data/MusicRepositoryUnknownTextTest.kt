package com.rudi.audioplayer.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Batch 726 — MediaStore memakai literal "<unknown>" (bukan null) untuk ARTIST/ALBUM kosong.
 *  Fungsi murni, tanpa Context/cursor. */
class MusicRepositoryUnknownTextTest {

    @Test
    fun `literal unknown MediaStore diganti fallback`() {
        assertEquals("Tidak Diketahui", MusicRepository.cleanMediaStoreText("<unknown>", "Tidak Diketahui"))
    }

    @Test
    fun `literal unknown tidak peka huruf dan spasi tepi`() {
        assertEquals("", MusicRepository.cleanMediaStoreText("  <UNKNOWN> ", ""))
    }

    @Test
    fun `null dan kosong atau spasi saja jatuh ke fallback`() {
        assertEquals("Tidak Diketahui", MusicRepository.cleanMediaStoreText(null, "Tidak Diketahui"))
        assertEquals("Tidak Diketahui", MusicRepository.cleanMediaStoreText("", "Tidak Diketahui"))
        assertEquals("Tidak Diketahui", MusicRepository.cleanMediaStoreText("   ", "Tidak Diketahui"))
    }

    @Test
    fun `nilai nyata dikembalikan apa adanya tanpa di-trim`() {
        assertEquals("Erzagar BZA", MusicRepository.cleanMediaStoreText("Erzagar BZA", "Tidak Diketahui"))
        assertEquals(" Rammor ", MusicRepository.cleanMediaStoreText(" Rammor ", "Tidak Diketahui"))
    }

    @Test
    fun `teks yang hanya mengandung kata unknown tidak ikut terganti`() {
        assertEquals("Unknown Mortal Orchestra", MusicRepository.cleanMediaStoreText("Unknown Mortal Orchestra", "Tidak Diketahui"))
        assertEquals("<unknown> Pleasures", MusicRepository.cleanMediaStoreText("<unknown> Pleasures", "Tidak Diketahui"))
    }
}
