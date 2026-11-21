package com.rudi.audioplayer.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Batch 618 — jaring pengaman untuk bagian VITAL (callback pemutar, scope coroutine service/ViewModel/widget/
 * bubble). Galat yang dulu langsung mematikan aplikasi kini DICATAT MENTAH (exception + stacktrace apa adanya,
 * tanpa penjelasan buatan) lewat [AppLogger.e] bersama nama operasi + thread, lalu aplikasi lanjut. Yang TIDAK
 * ditelan: [CancellationException] (dilempar ulang) dan `Error` (OutOfMemoryError dll — bukan `Exception`).
 * Pencatatan di-throttle per (tag, op, kelas exception): 3 kejadian pertama penuh, lalu tiap kelipatan 100,
 * agar callback yang terpanggil tiap event tidak membanjiri log.
 */
object SafeGuard {
    private val counts = ConcurrentHashMap<String, AtomicInteger>()

    /** Jalankan [block]; Exception dicatat dan ditelan. `return` non-lokal di dalam [block] tetap berlaku. */
    inline fun guard(tag: String, op: String, block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            report(tag, op, e)
        }
    }

    /** Seperti [guard] tetapi mengembalikan hasil [block], atau null bila terjadi Exception. */
    inline fun <T> attempt(tag: String, op: String, block: () -> T): T? {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            report(tag, op, e)
            null
        }
    }

    /**
     * Handler untuk `launch(handler)` / `CoroutineScope(... + handler)`. PAKAI bersama `SupervisorJob()`:
     * `Job()` biasa dibatalkan seluruhnya oleh satu anak yang gagal, sehingga scope mati diam-diam.
     */
    fun handler(tag: String, op: String = "coroutine"): CoroutineExceptionHandler =
        CoroutineExceptionHandler { ctx, e ->
            report(tag, "$op ${ctx[CoroutineName]?.name.orEmpty()}".trim(), e)
        }

    fun report(tag: String, op: String, e: Throwable) {
        try {
            val key = "$tag|$op|${e.javaClass.name}"
            val n = counts.getOrPut(key) { AtomicInteger() }.incrementAndGet()
            if (n > 3 && n % 100 != 0) return
            val suffix = if (n > 1) " (kejadian ke-$n; log di-throttle)" else ""
            AppLogger.e(tag, "Galat terjaring, aplikasi tetap berjalan: op=$op thread=${Thread.currentThread().name}$suffix", e)
        } catch (t: Throwable) {
            android.util.Log.e("SafeGuard", "report gagal untuk $tag/$op", t)
        }
    }
}
