package com.rudi.audioplayer.util

import android.app.Activity
import android.os.Process
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Batch 677 — jejak cold start: di mana ±1,8 dtk "sejak proses mulai" (Batch 675: `PlaybackService.onCreate` hanya 32 ms)
 * habis sebelum layar pertama tergambar. Instrumentasi dulu, perbaikan diputuskan dari angka (pelajaran Batch 461-463).
 *
 * Cara kerja: [mark] mencatat satu titik (label + ms sejak PROSES mulai, `Process.getStartElapsedRealtime`) ke antrean
 * lock-free — tanpa I/O, tanpa log di thread utama. [traceFirstFrame] memasang pendengar pra-gambar sekali pakai di
 * `decorView`; begitu frame pertama selesai dicatat (`frame1_selesai`), seluruh titik ditulis SATU baris WARN `StartupTiming`
 * lewat thread sendiri, lalu pencatat mati (start dingin saja; rotasi/Activity baru tak dicatat lagi).
 *
 * Titik yang dipasang: `app_mulai`/`app_selesai` (Application.onCreate), `act_mulai`/`act_super_selesai`/`act_vm_dibuat`/
 * `act_connect_selesai` (MainActivity.onCreate), `jendela_terpasang` (decorView menempel ke jendela = awal traversal pertama
 * + komposisi), `compose_apply` (komposisi pertama selesai diterapkan; [markOnce] supaya rekomposisi sebelum frame pertama
 * tak menggandakan titik), `frame1_predraw` (ukur/tata letak selesai, awal gambar frame pertama), `frame1_selesai` (pesan
 * utama pertama sesudah frame itu). Sub-komposisi lambat (NavHost/Scaffold) masuk selisih `compose_apply` -> `frame1_predraw`.
 * Angka `app_mulai` = waktu sistem sebelum kode aplikasi jalan (fork, class loading, ContentProvider).
 *
 * Aman: hanya android.* + JDK + [AppLogger] (aturan ArchUnit "util mandiri"). Bila Activity tak pernah menggambar
 * (proses dihidupkan tanpa UI), tak ada yang ditulis dan antrean hanya berisi beberapa titik kecil.
 */
object StartupTrace {

    private const val TAG = "StartupTiming"
    private const val THREAD_NAME = "StartupTraceLog"

    private class Point(val label: String, val sinceProcessMs: Long)

    private val points = ConcurrentLinkedQueue<Point>()
    private val done = AtomicBoolean(false)

    /** Catat satu titik waktu. Murah (satu pembacaan jam + satu add antrean); diabaikan setelah baris ditulis. */
    fun mark(label: String) {
        if (done.get()) return
        points.add(Point(label, SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()))
    }

    /** Seperti [mark] tetapi diabaikan bila label yang sama sudah tercatat (untuk titik yang bisa dipanggil berulang). */
    fun markOnce(label: String) {
        if (done.get()) return
        if (points.none { it.label == label }) mark(label)
    }

    /** Pasang pencatat frame pertama pada jendela [activity]. Panggil sekali dari `onCreate` (sebelum/sesudah `setContent`). */
    fun traceFirstFrame(activity: Activity) {
        if (done.get()) return
        val decor = activity.window.decorView
        decor.addOnAttachStateChangeListener(AttachListener())
        decor.viewTreeObserver.addOnPreDrawListener(FirstFrameListener(decor))
    }

    private class AttachListener : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            v.removeOnAttachStateChangeListener(this)
            StartupTrace.mark("jendela_terpasang")
        }

        override fun onViewDetachedFromWindow(v: View) = Unit
    }

    private class FirstFrameListener(private val decor: View) : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            val observer = decor.viewTreeObserver
            if (observer.isAlive) observer.removeOnPreDrawListener(this)
            StartupTrace.mark("frame1_predraw")
            decor.post {
                StartupTrace.mark("frame1_selesai")
                StartupTrace.flush()
            }
            return true
        }
    }

    private fun flush() {
        if (!done.compareAndSet(false, true)) return
        val line = buildLine()
        Thread({ AppLogger.w(TAG, line) }, THREAD_NAME).start()
    }

    private fun buildLine(): String {
        val sb = StringBuilder("Cold start (ms sejak proses mulai, +selisih dari titik sebelumnya): ")
        var previous = -1L
        for (point in points) {
            sb.append(point.label).append('=').append(point.sinceProcessMs).append("ms")
            if (previous >= 0L) sb.append("(+").append(point.sinceProcessMs - previous).append(')')
            sb.append(' ')
            previous = point.sinceProcessMs
        }
        return sb.toString().trimEnd()
    }
}
