package com.rudi.audioplayer.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/** Hasil menjalankan satu proses: keluaran (stdout+stderr), apakah melewati batas waktu, dan galat bila gagal mulai. */
internal class ShellResult(val lines: List<String>, val timedOut: Boolean, val error: String?)

private const val READER_JOIN_MS = 1_000L
private const val EXIT_POLL_MS = 50L

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
 * refleksi (aturan keep R8 di `proguard-rules.pro`). Hanya android.* + pustaka Shizuku (aturan ArchUnit "util mandiri").
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
