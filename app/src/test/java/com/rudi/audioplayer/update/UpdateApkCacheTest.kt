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

    // --- Batch 704: sisa APK yang SUDAH terpasang (log v703: v1.11.14-run676 tertinggal setelah pemasangan) ---

    @Test
    fun `versi dibaca dari nama aset nyata dan dari versionName murni angka`() {
        assertEquals(listOf(1, 11, 14), UpdateApkCache.apkVersion("update_AudioPlayer-v1.11.14-run676.apk"))
        assertEquals(listOf(1, 11, 7), UpdateApkCache.apkVersion("update_AudioPlayer-v1.11.7-run670.apk"))
        assertEquals(listOf(1, 11, 14), UpdateApkCache.apkVersion("update_AudioPlayer-v1.11.14-run676.APK"))
        assertEquals(listOf(1, 11, 14), UpdateApkCache.installedVersion("1.11.14"))
    }

    @Test
    fun `versi yang tak terbaca menghasilkan null`() {
        assertEquals(null, UpdateApkCache.apkVersion("update_foo.apk"))
        assertEquals(null, UpdateApkCache.apkVersion("update_AudioPlayer-v1.11-run676.apk"))
        assertEquals(null, UpdateApkCache.apkVersion("update_AudioPlayer-v99999999999.1.1-run1.apk"))
        assertEquals(null, UpdateApkCache.apkVersion("AudioPlayer-v1.11.14-run676.apk")) // bukan update_*.apk
        assertEquals(null, UpdateApkCache.installedVersion("1.11.14-debug"))
        assertEquals(null, UpdateApkCache.installedVersion("1.11"))
        assertEquals(null, UpdateApkCache.installedVersion(""))
    }

    @Test
    fun `installedNames memilih versi sama dan lebih lama - versi lebih baru dan tak terbaca dipertahankan`() {
        val names = listOf(
            "update_AudioPlayer-v1.11.13-run675.apk", // lebih lama
            "update_AudioPlayer-v1.11.14-run676.apk", // sama = baru saja terpasang (kasus log v703)
            "update_AudioPlayer-v1.11.15-run677.apk", // lebih baru: diunduh, belum dipasang
            "update_AudioPlayer-v1.12.0-run700.apk", // lebih baru (minor)
            "update_AudioPlayer-v2.0.0-run900.apk", // lebih baru (major)
            "update_AudioPlayer-v1.9.21-run471.apk", // 1.9 < 1.11 (perbandingan angka, bukan teks)
            "update_foo.apk", // versi tak terbaca
            "sigcheck_1.apk", // bukan APK pembaruan
            "catatan.txt"
        )

        assertEquals(
            listOf(
                "update_AudioPlayer-v1.11.13-run675.apk",
                "update_AudioPlayer-v1.11.14-run676.apk",
                "update_AudioPlayer-v1.9.21-run471.apk"
            ),
            UpdateApkCache.installedNames(names, "1.11.14")
        )
    }

    @Test
    fun `installedNames tanpa versionName terbaca memilih apa pun`() {
        val names = listOf("update_AudioPlayer-v1.11.13-run675.apk")

        assertEquals(emptyList<String>(), UpdateApkCache.installedNames(names, "1.11.14-debug"))
        assertEquals(emptyList<String>(), UpdateApkCache.installedNames(names, ""))
    }

    @Test
    fun `purgeInstalled menghapus APK terpasang dan lama saja dan menyisakan yang lebih baru`() {
        val dir = tmp.newFolder("cache")
        file(dir, "update_AudioPlayer-v1.11.13-run675.apk", 10)
        file(dir, "update_AudioPlayer-v1.11.14-run676.apk", 20)
        val newer = file(dir, "update_AudioPlayer-v1.11.15-run677.apk", 30)
        val unreadable = file(dir, "update_foo.apk", 40)
        val sigcheck = file(dir, "sigcheck_1.apk", 50)

        val result = UpdateApkCache.purgeInstalled(dir, "1.11.14")

        assertEquals(UpdateApkCache.PurgeResult(deleted = 2, deletedBytes = 30L, failed = 0), result)
        assertFalse(File(dir, "update_AudioPlayer-v1.11.13-run675.apk").exists())
        assertFalse(File(dir, "update_AudioPlayer-v1.11.14-run676.apk").exists())
        assertTrue(newer.exists())
        assertTrue(unreadable.exists())
        assertTrue(sigcheck.exists())
    }

    @Test
    fun `purgeInstalled pada folder tak ada atau tanpa APK melaporkan nol`() {
        assertEquals(UpdateApkCache.PurgeResult(0, 0L, 0), UpdateApkCache.purgeInstalled(File(tmp.root, "tidak_ada"), "1.11.14"))
        assertEquals(UpdateApkCache.PurgeResult(0, 0L, 0), UpdateApkCache.purgeInstalled(tmp.newFolder("kosong"), "1.11.14"))
    }

    @Test
    fun `purgeInstalled tidak menyentuh direktori bernama mirip APK pembaruan`() {
        val dir = tmp.newFolder("cache")
        val lookalike = File(dir, "update_AudioPlayer-v1.11.13-run675.apk").also { assertTrue(it.mkdir()) }

        assertEquals(UpdateApkCache.PurgeResult(0, 0L, 0), UpdateApkCache.purgeInstalled(dir, "1.11.14"))
        assertTrue(lookalike.isDirectory)
    }
}
