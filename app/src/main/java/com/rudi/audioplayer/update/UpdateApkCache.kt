package com.rudi.audioplayer.update

import java.io.File

/**
 * Batch 702 — pembersih APK pembaruan lama di `cacheDir`.
 *
 * DASAR (log v701, `sonix_debug_20261121_061550.txt`): lima pemasangan senyap dalam ±10 jam menulis
 * `Pasang senyap via Shizuku dimulai: update_AudioPlayer-v1.11.7-run670.apk 7101147 byte` (19:58),
 * `...v1.11.8-run671.apk` (20:07), `...v1.11.9-run672.apk` (20:25), `...v1.11.10-run673.apk` (05:37),
 * `...v1.11.11-run674.apk` (06:15). [UpdateManager.downloadAndPrepareInstall] menyimpan APK ke
 * `cacheDir/update_<nama aset>` dan nama aset memuat versi + nomor run, jadi namanya TIDAK pernah sama dua kali:
 * `UpdateDownloader` (hanya menghapus berkas bernama SAMA) tak pernah menyentuh yang lama, dan tak ada kode lain di app
 * yang menghapus `update_*.apk` (cari `update_` / `cacheDir` di seluruh `app/src/main`). Tiap pembaruan menambah ±7 MB
 * permanen di cache app (sistem baru mengosongkan cache saat penyimpanan hampir penuh).
 *
 * Kebijakan: sebelum APK baru diunduh, hapus SEMUA `update_*.apk` selain target unduhan itu. Aman karena status
 * `ReadyToInstall` hanya hidup di memori (hilang saat proses mati) dan pengunduhan baru selalu mengunduh ulang berkasnya;
 * berkas lama tak lagi dirujuk siapa pun. Sisa yang bertahan paling banyak satu (APK pembaruan yang baru terpasang; proses
 * lamanya sudah mati sehingga tak bisa menghapusnya) sampai pengunduhan berikutnya.
 *
 * Murni `java.io` + Kotlin (tanpa API Android) supaya bisa diuji di JVM.
 */
internal object UpdateApkCache {
    private const val PREFIX = "update_"
    private const val SUFFIX = ".apk"

    /** Hasil [purge]: jumlah berkas terhapus, total byte-nya, dan jumlah yang gagal dihapus. */
    data class PurgeResult(val deleted: Int, val deletedBytes: Long, val failed: Int)

    /** True bila [name] berpola `update_<sesuatu>.apk` (huruf besar-kecil pada `.apk` tak dibedakan). */
    fun isUpdateApk(name: String): Boolean =
        name.length > PREFIX.length + SUFFIX.length && name.startsWith(PREFIX) && name.endsWith(SUFFIX, ignoreCase = true)

    /** Nama berkas APK pembaruan yang boleh dihapus: pola [isUpdateApk] dan BUKAN [keepName]. Berkas lain tak pernah dipilih. */
    fun staleNames(names: Collection<String>, keepName: String?): List<String> =
        names.filter { it != keepName && isUpdateApk(it) }

    /** Menghapus APK pembaruan basi di [cacheDir] (blocking I/O: panggil dari Dispatchers.IO). Tidak melempar. */
    fun purge(cacheDir: File, keepName: String?): PurgeResult {
        val files = cacheDir.listFiles() ?: return PurgeResult(0, 0L, 0)
        val stale = staleNames(files.map { it.name }, keepName).toHashSet()
        var deleted = 0
        var deletedBytes = 0L
        var failed = 0
        for (file in files) {
            if (file.name !in stale || !file.isFile) continue
            val size = file.length()
            if (file.delete()) {
                deleted++
                deletedBytes += size
            } else {
                failed++
            }
        }
        return PurgeResult(deleted, deletedBytes, failed)
    }
}
