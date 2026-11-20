package com.rudi.audioplayer.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import okio.buffer
import okio.sink
import okio.source
import rikka.shizuku.Shizuku
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/** Hasil menjalankan satu proses: keluaran (stdout+stderr), apakah melewati batas waktu, dan galat bila gagal mulai. */
internal class ShellResult(val lines: List<String>, val timedOut: Boolean, val error: String?)

private const val READER_JOIN_MS = 1_000L
private const val EXIT_POLL_MS = 50L
private const val INSTALL_LINE_LIMIT = 40
private const val WRITER_JOIN_MS = 1_000L

/**
 * Batch 694 — hasil pasang senyap: [success] true HANYA bila keluaran `pm` memuat baris "Success" (tanpa batas waktu);
 * [detail] = keluaran/galat MENTAH (tanpa tafsiran) untuk dicatat apa adanya di log diagnostik.
 */
internal class SilentInstallResult(val success: Boolean, val detail: String)

/**
 * Batch 694 — perintah pasang: `pm install -r -S <byte>` TANPA path = APK dibaca dari stdin (wajib `-S` = ukuran byte). Berkas di
 * cacheDir privat app tak terbaca uid `shell` (Shizuku), jadi APK dialirkan lewat stdin, bukan lewat path. `2>&1` menggabung
 * galat ke keluaran (pola sama pemanggil `runShell` lain).
 */
internal fun installCommand(sizeBytes: Long): String = "pm install -r -S $sizeBytes 2>&1"

/** Batch 694 — murni (tanpa Android): ubah keluaran proses `pm install` + galat baca/tulis menjadi [SilentInstallResult]. */
internal fun parseInstallOutput(
    lines: List<String>,
    timedOut: Boolean,
    readError: String?,
    writeError: String?
): SilentInstallResult {
    val ok = !timedOut && lines.any { it.trim().startsWith("Success") }
    val raw = buildString {
        lines.forEach { appendLine(it) }
        if (timedOut) appendLine("[batas waktu terlewati]")
        if (readError != null) appendLine("[baca keluaran gagal] $readError")
        if (writeError != null) appendLine("[tulis APK ke stdin gagal] $writeError")
    }.trimEnd()
    return SilentInstallResult(ok, raw.ifEmpty { "(tanpa keluaran)" })
}

/**
 * Batch 629 — pengganti `Process.waitFor(timeout, unit)`. Versi bawaan JDK hanya menangkap IllegalThreadStateException
 * saat polling `exitValue()`, sedangkan `ShizukuRemoteProcess.exitValue()` melempar IllegalArgumentException("process
 * hasn't exited") selama proses masih berjalan -> aplikasi CRASH (crash log 11 Nov 06:40, v628, `Process.waitFor`).
 * Di sini SETIAP galat `exitValue()` dianggap "belum selesai"; true = sudah selesai, false = batas waktu/terinterupsi.
 */
private fun waitForExit(process: Process, timeoutMs: Long): Boolean {
    val deadline = SystemClock.elapsedRealtime() + timeoutMs
    while (true) {
        if (runCatching { process.exitValue() }.isSuccess) return true
        if (SystemClock.elapsedRealtime() >= deadline) return false
        try {
            Thread.sleep(EXIT_POLL_MS)
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
            return false
        }
    }
}

/**
 * Batch 628 — baca keluaran [process] di thread terpisah (dibatasi [lineLimit] baris) dengan batas waktu [timeoutMs].
 * Dipakai bersama oleh jalur langsung (ProcessBuilder) dan jalur Shizuku. Blocking: panggil dari `Dispatchers.IO`.
 */
internal fun drainProcess(process: Process, timeoutMs: Long, lineLimit: Int): ShellResult {
    val lines = ArrayList<String>()
    val readFailure = AtomicReference<String?>(null)
    val reader = thread(isDaemon = true, name = "shell-reader") {
        runCatching {
            process.inputStream.bufferedReader().use { r ->
                r.forEachLine { line -> synchronized(lines) { if (lines.size < lineLimit) lines.add(line) } }
            }
        }.onFailure { readFailure.set("${it.javaClass.simpleName}: ${it.message.orEmpty()}") }
    }
    val finished = waitForExit(process, timeoutMs)
    if (!finished) runCatching { process.destroyForcibly() }
    reader.join(READER_JOIN_MS)
    val snapshot = synchronized(lines) { ArrayList(lines) }
    return ShellResult(snapshot, !finished, readFailure.get())
}

/**
 * Batch 628 — jembatan ke Shizuku (https://shizuku.rikka.app): setelah layanan Shizuku berjalan (Wireless debugging /
 * root) dan pengguna mengizinkan aplikasi ini, perintah shell (uid `shell`, yang memegang izin DUMP) dapat dijalankan
 * TANPA adb dari PC: dipakai [DumpsysCollector] untuk `dumpsys` dan untuk memberi izin DUMP ke aplikasi ini.
 *
 * Semua fungsi aman dipanggil kapan pun: bila Shizuku tak terpasang / tak berjalan / refleksi gagal, hasilnya status
 * `NOT_*` atau [ShellResult.error] (tak melempar). `Shizuku.newProcess` private sejak API 13.1.0 -> dipanggil lewat
 * refleksi (aturan keep R8 di `proguard-rules.pro`). Hanya android.* + pustaka Shizuku/Okio (aturan ArchUnit "util mandiri").
 */
object ShizukuBridge {

    const val REQUEST_CODE = 6271
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    enum class State { NOT_INSTALLED, NOT_RUNNING, NEED_PERMISSION, READY }

    /** Status Shizuku saat ini. `pingBinder` = layanan hidup; `checkSelfPermission` = pengguna sudah mengizinkan aplikasi ini. */
    fun state(context: Context): State {
        val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!running) return if (launchIntent(context) != null) State.NOT_RUNNING else State.NOT_INSTALLED
        val granted = runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
        return if (granted) State.READY else State.NEED_PERMISSION
    }

    /** Intent peluncur aplikasi Shizuku, null bila tak terpasang. */
    fun launchIntent(context: Context): Intent? =
        context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Munculkan dialog izin Shizuku; true bila permintaan terkirim (hasil lewat listener). */
    fun requestPermission(): Boolean = runCatching { Shizuku.requestPermission(REQUEST_CODE) }.isSuccess

    /** Daftarkan pendengar hasil dialog izin; kembalikan pendengarnya untuk [removePermissionListener]. Null bila gagal. */
    fun addPermissionListener(onResult: () -> Unit): Shizuku.OnRequestPermissionResultListener? {
        val listener = Shizuku.OnRequestPermissionResultListener { _, _ -> onResult() }
        return if (runCatching { Shizuku.addRequestPermissionResultListener(listener) }.isSuccess) listener else null
    }

    fun removePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        runCatching { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    /** Jalankan [commandLine] lewat `sh -c` di proses Shizuku (stderr digabung lewat `2>&1` oleh pemanggil). */
    internal fun runShell(commandLine: String, timeoutMs: Long, lineLimit: Int): ShellResult {
        val process = start(commandLine).getOrElse {
            return ShellResult(emptyList(), false, "Shizuku: ${it.javaClass.simpleName}: ${it.message.orEmpty()}")
        }
        return drainProcess(process, timeoutMs, lineLimit)
    }

    /**
     * Batch 694 — pasang [apk] TANPA dialog konfirmasi installer: `pm install -r -S <byte>` dijalankan di proses Shizuku (uid
     * `shell` memegang INSTALL_PACKAGES) dan APK dialirkan lewat stdin (buffered Okio, `use {}`; lihat [installCommand]).
     * Hanya bila [state] == READY; selain itu langsung gagal supaya pemanggil jatuh ke installer sistem. Blocking: panggil dari
     * `Dispatchers.IO`. Bila berhasil, sistem mematikan proses app ini (PACKAGE_UPDATED) SEBELUM fungsi sempat kembali — normal;
     * pemulihan pemutaran ditangani `UpdateResume*`. Penulisan stdin berjalan di thread sendiri dan keluaran dibaca bersamaan
     * (tak ada kebuntuan pipa penuh); batas waktu [timeoutMs] menghentikan proses bila macet.
     */
    internal fun installApk(context: Context, apk: File, timeoutMs: Long): SilentInstallResult {
        if (state(context) != State.READY) return SilentInstallResult(false, "Shizuku belum siap (status bukan READY)")
        val size = apk.length()
        if (size <= 0L) return SilentInstallResult(false, "APK kosong atau tak ada: ${apk.path}")
        val process = start(installCommand(size)).getOrElse {
            return SilentInstallResult(false, "Shizuku: ${it.javaClass.simpleName}: ${it.message.orEmpty()}")
        }
        val writeFailure = AtomicReference<String?>(null)
        val writer = thread(isDaemon = true, name = "apk-writer") { writeFailure.set(streamApk(process, apk)) }
        val result = drainProcess(process, timeoutMs, INSTALL_LINE_LIMIT)
        writer.join(WRITER_JOIN_MS)
        return parseInstallOutput(result.lines, result.timedOut, result.error, writeFailure.get())
    }

    /** Alirkan [apk] ke stdin [process] lalu tutup (EOF = tanda selesai bagi `pm`); null = sukses, selain itu galat mentah. */
    private fun streamApk(process: Process, apk: File): String? =
        try {
            process.outputStream.sink().buffer().use { sink ->
                apk.source().use { source -> sink.writeAll(source) }
            }
            null
        } catch (e: IOException) {
            "${e.javaClass.simpleName}: ${e.message.orEmpty()}"
        }

    private fun start(commandLine: String): Result<Process> = runCatching {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        method.invoke(null, arrayOf("sh", "-c", commandLine), null, null) as Process
    }
}
