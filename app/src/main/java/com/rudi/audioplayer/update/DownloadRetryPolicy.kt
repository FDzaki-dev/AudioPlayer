package com.rudi.audioplayer.update

import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Batch 708 — pengulangan otomatis unduhan APK pembaruan untuk galat jaringan SEMENTARA.
 *
 * DASAR (log v707, `sonix_debug_20261121_180441.txt`): 18:03:02 `Unduhan dimulai: AudioPlayer-v1.11.18-run680.apk`, 18:03:10
 * `Unduhan gagal: Unable to resolve host "github.com": No address associated with hostname` (8 dtk), lalu 18:03:15 unduhan yang
 * SAMA berhasil (`7101147 byte`, 3 dtk). DNS pulih dalam ±5 dtk, tetapi `UpdateDownloader` menyerah pada galat pertama dan status
 * `Error` di `UpdateCheckSheet` tidak punya tombol: pengguna harus menutup lalu membuka ulang sheet. Kebijakan ini mengulang
 * unduhan dari awal (berkas ditulis ulang) paling banyak [MAX_ATTEMPTS] kali dengan jeda bertambah, hanya untuk galat yang
 * lazimnya hilang sendiri ([isTransient]). HTTP non-2xx, SSL, dan unduhan tidak lengkap TIDAK diulang.
 *
 * Murni `java.net` + Kotlin (tanpa API Android/OkHttp) supaya bisa diuji di JVM.
 */
internal object DownloadRetryPolicy {

    /** Total percobaan (percobaan pertama + pengulangan). */
    const val MAX_ATTEMPTS = 3

    private const val BASE_DELAY_MS = 1_500L

    /** Hasil akhir [execute]: [result] percobaan terakhir dan [attempts] = jumlah percobaan yang benar-benar dijalankan. */
    class Outcome<out R>(val result: R, val attempts: Int)

    /**
     * True bila [error] biasanya sementara: DNS gagal ([UnknownHostException]), koneksi ditolak/terputus/tak ada rute
     * ([SocketException] dan turunannya `ConnectException`/`NoRouteToHostException`), atau batas waktu ([SocketTimeoutException]).
     */
    fun isTransient(error: Throwable): Boolean =
        error is UnknownHostException || error is SocketException || error is SocketTimeoutException

    /** Jeda (ms) sebelum percobaan berikutnya setelah percobaan ke-[failedAttempt] gagal: 1500, 3000, ... (kelipatan, min. 1x). */
    fun retryDelayMs(failedAttempt: Int): Long = BASE_DELAY_MS * failedAttempt.coerceAtLeast(1)

    /**
     * Menjalankan [attempt] sampai berhasil, tak lagi layak diulang ([shouldRetry] false), atau [MAX_ATTEMPTS] tercapai.
     * [onRetry] dipanggil SEBELUM jeda dengan nomor percobaan yang gagal, lama jeda, dan hasilnya; [pause] melakukan jeda
     * (blocking; default pemanggil = `Thread.sleep`). Bila [pause] melempar [InterruptedException], flag interrupt dipulihkan
     * dan hasil percobaan gagal terakhir dikembalikan tanpa percobaan lagi. Blocking: panggil dari `Dispatchers.IO`.
     */
    fun <R> execute(
        shouldRetry: (R) -> Boolean,
        pause: (Long) -> Unit,
        onRetry: (failedAttempt: Int, delayMs: Long, failed: R) -> Unit,
        attempt: () -> R
    ): Outcome<R> {
        var number = 1
        var result = attempt()
        while (number < MAX_ATTEMPTS && shouldRetry(result)) {
            val delayMs = retryDelayMs(number)
            onRetry(number, delayMs, result)
            try {
                pause(delayMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            number++
            result = attempt()
        }
        return Outcome(result, number)
    }
}
