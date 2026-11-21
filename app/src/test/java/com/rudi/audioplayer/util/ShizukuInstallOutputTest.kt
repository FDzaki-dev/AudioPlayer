package com.rudi.audioplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batch 694 — pasang senyap via Shizuku: logika murni (tanpa Android/Shizuku) yang menentukan "berhasil atau jatuh ke installer
 * sistem". Salah menilai "berhasil" = pengguna tak melihat dialog installer DAN tak ada pembaruan; salah menilai "gagal" =
 * dialog installer muncul sesudah pemasangan sukses. Keduanya dijaga di sini.
 */
class ShizukuInstallOutputTest {

    @Test
    fun `perintah pasang membaca APK dari stdin dengan ukuran byte dan menggabung galat ke keluaran`() {
        assertEquals("pm install -r -S 26214400 2>&1", installCommand(26_214_400L))
    }

    @Test
    fun `baris Success berarti berhasil`() {
        val result = parseInstallOutput(listOf("Success"), timedOut = false, readError = null, writeError = null)
        assertTrue(result.success)
        assertEquals("Success", result.detail)
    }

    @Test
    fun `Success dengan spasi di tepi tetap berhasil`() {
        val result = parseInstallOutput(listOf("  Success  "), timedOut = false, readError = null, writeError = null)
        assertTrue(result.success)
    }

    @Test
    fun `Failure berarti gagal dan keluaran mentah dipertahankan apa adanya`() {
        val raw = "Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.rudi.audioplayer signatures do not match]"
        val result = parseInstallOutput(listOf(raw), timedOut = false, readError = null, writeError = null)
        assertFalse(result.success)
        assertEquals(raw, result.detail)
    }

    @Test
    fun `kata Success di tengah kalimat bukan tanda berhasil`() {
        val result = parseInstallOutput(
            listOf("Failure [INSTALL_FAILED_INTERNAL_ERROR]", "Error: not Success"),
            timedOut = false,
            readError = null,
            writeError = null
        )
        assertFalse(result.success)
    }

    @Test
    fun `batas waktu mengalahkan Success dan ditandai di keluaran`() {
        val result = parseInstallOutput(listOf("Success"), timedOut = true, readError = null, writeError = null)
        assertFalse(result.success)
        assertTrue(result.detail.contains("[batas waktu terlewati]"))
    }

    @Test
    fun `galat tulis stdin tanpa keluaran dicatat mentah dan dianggap gagal`() {
        val result = parseInstallOutput(emptyList(), timedOut = false, readError = null, writeError = "IOException: Broken pipe")
        assertFalse(result.success)
        assertTrue(result.detail.contains("IOException: Broken pipe"))
    }

    @Test
    fun `galat baca keluaran dicatat mentah bersama keluaran yang sempat terbaca`() {
        val result = parseInstallOutput(listOf("Failure [X]"), timedOut = false, readError = "IOException: Stream closed", writeError = null)
        assertFalse(result.success)
        assertEquals("Failure [X]\n[baca keluaran gagal] IOException: Stream closed", result.detail)
    }

    @Test
    fun `tanpa keluaran sama sekali dianggap gagal dengan keterangan`() {
        val result = parseInstallOutput(emptyList(), timedOut = false, readError = null, writeError = null)
        assertFalse(result.success)
        assertEquals("(tanpa keluaran)", result.detail)
    }
}
