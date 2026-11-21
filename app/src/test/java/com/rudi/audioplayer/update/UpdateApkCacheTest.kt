package com.rudi.audioplayer.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UpdateApkCacheTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // Nama NYATA dari log v701 (`Pasang senyap via Shizuku dimulai: <nama> 7101147 byte`).
    private val realNames = listOf(
        "update_AudioPlayer-v1.11.7-run670.apk",
        "update_AudioPlayer-v1.11.8-run671.apk",
        "update_AudioPlayer-v1.11.9-run672.apk",
        "update_AudioPlayer-v1.11.10-run673.apk",
        "update_AudioPlayer-v1.11.11-run674.apk"
    )

    private fun file(dir: File, name: String, bytes: Int): File =
        File(dir, name).also { it.writeBytes(ByteArray(bytes)) }

    @Test
    fun `nama aset nyata dari log v701 dikenali sebagai APK pembaruan`() {
        realNames.forEach { assertTrue(it, UpdateApkCache.isUpdateApk(it)) }
        assertTrue(UpdateApkCache.isUpdateApk("update_AudioPlayer-v1.11.11-run674.APK"))
    }

    @Test
    fun `berkas cache lain tidak pernah dikenali`() {
        listOf(
            "sigcheck_1795212929417_123.apk", // ApkSignatureChecker
            "tagwrite_123456789.tmp", // TagEditor
            "ringtone_cut_987654321.tmp", // RingtoneEncoder
            "update_AudioPlayer-v1.11.11-run674.apk.part",
            "update_.apk",
            "Update_AudioPlayer-v1.11.11-run674.apk",
            "AudioPlayer-v1.11.11-run674.apk",
            ""
        ).forEach { assertFalse("'$it'", UpdateApkCache.isUpdateApk(it)) }
    }

    @Test
    fun `lima nama dari log v701 - yang sedang diunduh dipertahankan empat lainnya basi`() {
        val keep = realNames.last()
        assertEquals(realNames.dropLast(1), UpdateApkCache.staleNames(realNames, keep))
    }

    @Test
    fun `tanpa nama yang dipertahankan semua APK pembaruan basi tetapi berkas lain tetap aman`() {
        val names = realNames + listOf("sigcheck_1.apk", "tagwrite_2.tmp")
        assertEquals(realNames, UpdateApkCache.staleNames(names, null))
    }

    @Test
    fun `purge menghapus APK lama saja dan melaporkan jumlah serta byte`() {
        val dir = tmp.newFolder("cache")
        file(dir, "update_AudioPlayer-v1.11.9-run672.apk", 10)
        file(dir, "update_AudioPlayer-v1.11.10-run673.apk", 20)
        val keep = file(dir, "update_AudioPlayer-v1.11.11-run674.apk", 30)
        val sigcheck = file(dir, "sigcheck_1.apk", 40)
        val other = file(dir, "catatan.txt", 5)

        val result = UpdateApkCache.purge(dir, keep.name)

        assertEquals(UpdateApkCache.PurgeResult(deleted = 2, deletedBytes = 30L, failed = 0), result)
        assertTrue(keep.exists())
        assertTrue(sigcheck.exists())
        assertTrue(other.exists())
        assertFalse(File(dir, "update_AudioPlayer-v1.11.9-run672.apk").exists())
        assertFalse(File(dir, "update_AudioPlayer-v1.11.10-run673.apk").exists())
    }

    @Test
    fun `purge tanpa nama yang dipertahankan menghapus semua APK pembaruan`() {
        val dir = tmp.newFolder("cache")
        realNames.forEach { file(dir, it, 7) }

        val result = UpdateApkCache.purge(dir, null)

        assertEquals(5, result.deleted)
        assertEquals(35L, result.deletedBytes)
        assertEquals(0, dir.listFiles()!!.size)
    }

    @Test
    fun `purge tidak menyentuh direktori bernama mirip APK pembaruan`() {
        val dir = tmp.newFolder("cache")
        val lookalike = File(dir, "update_folder.apk").also { assertTrue(it.mkdir()) }

        val result = UpdateApkCache.purge(dir, null)

        assertEquals(UpdateApkCache.PurgeResult(0, 0L, 0), result)
        assertTrue(lookalike.isDirectory)
    }

    @Test
    fun `purge pada folder yang tak ada tidak melempar dan melaporkan nol`() {
        val missing = File(tmp.root, "tidak_ada")

        assertEquals(UpdateApkCache.PurgeResult(0, 0L, 0), UpdateApkCache.purge(missing, null))
    }
}
