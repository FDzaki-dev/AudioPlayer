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
 * berkas lama tak lagi dirujuk siapa pun.
 *
 * Batch 704 — sisa terakhir ikut dibersihkan. Log v703 (`sonix_debug_20261121_121113.txt`): 12:10:52 pembersihan sebelum unduhan
 * menghapus `v1.11.13` (`terhapus=1 (7101147 byte)`), 12:10:55 `Pasang senyap via Shizuku dimulai: update_AudioPlayer-v1.11.14-run676.apk`
 * lalu proses mati karena pemasangan itu sendiri -> APK `v1.11.14` (±7,1 MB) tertinggal di cache SESUDAH terpasang, dan proses
 * lamanya sudah mati sehingga tak bisa menghapusnya (Batch 702 menerimanya sebagai "paling banyak satu"). [purgeInstalled] dipanggil
 * dari `Application.onCreate` (thread latar): menghapus `update_*.apk` yang versinya <= versi terpasang. APK dengan versi LEBIH BARU
 * (sudah diunduh, belum dipasang; mis. installer sistem masih terbuka) dan nama yang versinya tak terbaca TIDAK disentuh.
 *
 * Murni `java.io` + Kotlin (tanpa API Android) supaya bisa diuji di JVM.
 */
internal object UpdateApkCache {
    private const val PREFIX = "update_"
    private const val SUFFIX = ".apk"

    /** `<...>-v<M>.<m>.<p>-run<N>.apk` (pola nama aset rilis CI: `AudioPlayer-v1.11.14-run676.apk`). */
    private val ASSET_VERSION = Regex("""-v(\d+)\.(\d+)\.(\d+)-run\d+\.apk$""", RegexOption.IGNORE_CASE)

    /** `versionName` lokal murni angka `M.m.p` (lihat komentar `UpdateManager.checkForUpdate`). */
    private val VERSION_NAME = Regex("""^(\d+)\.(\d+)\.(\d+)$""")

    /** Hasil [purge]: jumlah berkas terhapus, total byte-nya, dan jumlah yang gagal dihapus. */
    data class PurgeResult(val deleted: Int, val deletedBytes: Long, val failed: Int)

    /** True bila [name] berpola `update_<sesuatu>.apk` (huruf besar-kecil pada `.apk` tak dibedakan). */
    fun isUpdateApk(name: String): Boolean =
        name.length > PREFIX.length + SUFFIX.length && name.startsWith(PREFIX) && name.endsWith(SUFFIX, ignoreCase = true)

    /** Nama berkas APK pembaruan yang boleh dihapus: pola [isUpdateApk] dan BUKAN [keepName]. Berkas lain tak pernah dipilih. */
    fun staleNames(names: Collection<String>, keepName: String?): List<String> =
        names.filter { it != keepName && isUpdateApk(it) }

    /** `[M, m, p]` dari nama berkas APK pembaruan; null bila bukan [isUpdateApk] atau versinya tak terbaca. */
    fun apkVersion(name: String): List<Int>? =
        if (isUpdateApk(name)) ASSET_VERSION.find(name)?.let { toVersion(it) } else null

    /** `[M, m, p]` dari `versionName` murni angka (`1.11.14`); null bila ada akhiran/format lain. */
    fun installedVersion(versionName: String): List<Int>? = VERSION_NAME.find(versionName)?.let { toVersion(it) }

    /**
     * Nama APK pembaruan yang versinya <= [installedVersionName] (sudah terpasang atau lebih lama = tak berguna lagi).
     * Konservatif: bila `versionName` terpasang atau versi di nama berkas tak terbaca, berkas itu TIDAK dipilih.
     */
    fun installedNames(names: Collection<String>, installedVersionName: String): List<String> {
        val installed = installedVersion(installedVersionName) ?: return emptyList()
        return names.filter { name ->
            val version = apkVersion(name)
            version != null && compareVersions(version, installed) <= 0
        }
    }

    /** Menghapus APK pembaruan basi di [cacheDir] (blocking I/O: panggil dari Dispatchers.IO). Tidak melempar. */
    fun purge(cacheDir: File, keepName: String?): PurgeResult =
        deleteSelected(cacheDir) { names -> staleNames(names, keepName) }

    /** Menghapus APK pembaruan yang versinya <= versi terpasang (lihat [installedNames]). Blocking I/O. Tidak melempar. */
    fun purgeInstalled(cacheDir: File, installedVersionName: String): PurgeResult =
        deleteSelected(cacheDir) { names -> installedNames(names, installedVersionName) }

    private fun deleteSelected(cacheDir: File, select: (List<String>) -> List<String>): PurgeResult {
        val files = cacheDir.listFiles() ?: return PurgeResult(0, 0L, 0)
        val chosen = select(files.map { it.name }).toHashSet()
        var deleted = 0
        var deletedBytes = 0L
        var failed = 0
        for (file in files) {
            if (file.name !in chosen || !file.isFile) continue
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

    private fun toVersion(match: MatchResult): List<Int>? {
        val parts = match.groupValues.drop(1).map { it.toIntOrNull() }
        return if (parts.any { it == null }) null else parts.filterNotNull()
    }

    private fun compareVersions(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val diff = a[i].compareTo(b[i])
            if (diff != 0) return diff
        }
        return a.size.compareTo(b.size)
    }
}
