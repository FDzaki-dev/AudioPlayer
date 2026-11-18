package com.rudi.audioplayer.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Printer
import android.view.FrameMetrics
import android.view.Window
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Batch 682 — jawab "SIAPA yang menahan thread utama" pada `Choreographer: Skipped 31 frames!` di frame pertama proses
 * pasca-instal (log v681; sebelumnya 33+30 di v679; 0 di v680 dan 0 di 4 start dingin force-stop v677). Dalam
 * [StartupStallFormat.WINDOW_MS] pertama proses: (a) `Looper.setMessageLogging` mengukur tiap pesan thread utama dan menyimpan teks
 * MENTAH platform (Handler + callback + what) untuk yang lama; (b) `Window.addOnFrameMetricsAvailableListener` menyimpan rincian
 * fase frame lambat (tunda vsync = thread utama sibuk SEBELUM frame; layout/gambar/sinkron = ongkos frame itu sendiri).
 * Entri ditahan di memori dan ditulis ke Log Diagnostik (tag `StartupStall`, level WARN) HANYA bila ada stall besar
 * (lihat [StartupStallFormat]); start normal tidak menulis apa pun. Tak mengubah perilaku app: semua dibungkus `runCatching`
 * dan dilepas otomatis di akhir jendela. Instrumentasi sementara — hapus bila penyebab sudah ketemu dan diperbaiki.
 */
object StartupStallProbe {
    private const val TAG = "StartupStall"

    private val started = AtomicBoolean(false)
    private val collector = StallCollector()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Panggil sekali dari `Application.onCreate` (setelah `AppLogger.init`). Panggilan berikutnya tak berefek. */
    fun start(app: Application) {
        if (!started.compareAndSet(false, true)) return
        runCatching {
            val processStart = Process.getStartUptimeMillis()
            val remainingMs = processStart + StartupStallFormat.WINDOW_MS - SystemClock.uptimeMillis()
            if (remainingMs > 0L) install(app, processStart, remainingMs)
        }
    }

    private fun install(app: Application, processStart: Long, remainingMs: Long) {
        val main = Handler(Looper.getMainLooper())
        val frames = FrameWatcher(main, FrameListener(processStart, collector))
        Looper.getMainLooper().setMessageLogging(MessagePrinter(processStart, collector))
        app.registerActivityLifecycleCallbacks(frames)
        main.postDelayed({
            runCatching {
                Looper.getMainLooper().setMessageLogging(null)
                app.unregisterActivityLifecycleCallbacks(frames)
                frames.detachAll()
                flush()
            }
        }, remainingMs)
    }

    private fun flush() {
        val lines = collector.drain()
        if (lines.isEmpty()) return
        // Satu coroutine supaya urutan baris di berkas log sama dengan urutan [lines] (bukan di thread utama: tak menambah stall).
        scope.launch { lines.forEach { AppLogger.w(TAG, it) } }
    }
}

/** Mengukur durasi tiap pesan thread utama dari pasangan teks `Looper` ">>>>> Dispatching ..." / "<<<<< Finished ...". */
private class MessagePrinter(
    private val processStart: Long,
    private val collector: StallCollector
) : Printer {
    // Hanya disentuh thread utama (Looper memanggil printer dari thread loop).
    private var beginNs = 0L
    private var dispatchText: String? = null

    override fun println(x: String?) {
        if (x == null) return
        if (x.startsWith(StartupStallFormat.DISPATCH_PREFIX)) {
            beginNs = SystemClock.elapsedRealtimeNanos()
            dispatchText = x
        } else if (x.startsWith(StartupStallFormat.FINISH_PREFIX)) {
            val text = dispatchText ?: return
            dispatchText = null
            val durationMs = StartupStallFormat.nsToMs(SystemClock.elapsedRealtimeNanos() - beginNs)
            if (durationMs >= StartupStallFormat.MIN_LOG_MS) {
                val atMs = SystemClock.uptimeMillis() - processStart
                collector.add(
                    durationMs,
                    StartupStallFormat.MESSAGE_TRIGGER_MS,
                    StartupStallFormat.messageLine(atMs, durationMs, text)
                )
            }
        }
    }
}

/** Menyalin nilai `FrameMetrics` seketika (objek aslinya dipakai ulang platform) lalu menyerahkannya ke [collector]. */
private class FrameListener(
    private val processStart: Long,
    private val collector: StallCollector
) : Window.OnFrameMetricsAvailableListener {
    override fun onFrameMetricsAvailable(window: Window, frameMetrics: FrameMetrics, dropCountSinceLastInvocation: Int) {
        val totalNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
        val totalMs = StartupStallFormat.nsToMs(totalNs)
        if (totalMs < StartupStallFormat.MIN_LOG_MS) return
        val sample = FrameSample(
            totalNs = totalNs,
            vsyncDelayNs = frameMetrics.getMetric(FrameMetrics.VSYNC_TIMESTAMP) -
                frameMetrics.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP),
            inputNs = frameMetrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION),
            animationNs = frameMetrics.getMetric(FrameMetrics.ANIMATION_DURATION),
            layoutNs = frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION),
            drawNs = frameMetrics.getMetric(FrameMetrics.DRAW_DURATION),
            syncNs = frameMetrics.getMetric(FrameMetrics.SYNC_DURATION),
            issueNs = frameMetrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION),
            swapNs = frameMetrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION),
            unknownNs = frameMetrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION),
            firstDraw = frameMetrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) != 0L,
            dropCount = dropCountSinceLastInvocation
        )
        val atMs = SystemClock.uptimeMillis() - processStart
        collector.add(totalMs, StartupStallFormat.FRAME_TRIGGER_MS, StartupStallFormat.frameLine(atMs, sample))
    }
}

/**
 * Memasang [listener] ke jendela tiap Activity SETELAH `onCreate` selesai (`onActivityPostCreated`, API 29+: konten sudah
 * dipasang, jadi `getDecorView()` tak mengganggu `requestFeature`/setContent) dan melepasnya saat Activity dihancurkan atau
 * jendela pemantauan berakhir. Semua callback jalan di thread utama.
 */
private class FrameWatcher(
    private val handler: Handler,
    private val listener: FrameListener
) : Application.ActivityLifecycleCallbacks {
    private val attached = ArrayList<Window>()

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        val window = activity.window
        runCatching {
            window.addOnFrameMetricsAvailableListener(listener, handler)
            attached.add(window)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        val window = activity.window
        runCatching {
            window.removeOnFrameMetricsAvailableListener(listener)
            attached.remove(window)
        }
    }

    fun detachAll() {
        attached.forEach { runCatching { it.removeOnFrameMetricsAvailableListener(listener) } }
        attached.clear()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
