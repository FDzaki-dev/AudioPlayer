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
 * Batch 682 — jawab "SIAPA yang menahan thread utama" pada `Choreographer: Skipped 31 frames!` di frame pertama (log v681;
 * sebelumnya 33+30 di v679; 0 di v680 dan 0 di 4 start dingin force-stop v677). Selama satu jendela
 * ([StartupStallFormat.WINDOW_MS]): (a) `Looper.setMessageLogging` mengukur tiap pesan thread utama dan menyimpan teks
 * MENTAH platform (Handler + callback + what) untuk yang lama; (b) `Window.addOnFrameMetricsAvailableListener` menyimpan
 * rincian fase frame lambat (tunda vsync = thread utama sibuk SEBELUM frame; layout/gambar/sinkron = ongkos frame itu sendiri).
 * Entri ditahan di memori dan ditulis ke Log Diagnostik (tag `StartupStall`, level WARN) HANYA bila ada stall besar
 * (lihat [StartupStallFormat]); start normal tidak menulis apa pun. Tak mengubah perilaku app: semua dibungkus `runCatching`
 * dan dilepas otomatis di akhir jendela. Instrumentasi sementara — hapus bila penyebab sudah ketemu dan diperbaiki.
 *
 * Batch 683 — jendela dijangkarkan ke Activity pertama, bukan hanya ke proses mulai. Log v682 membuktikan jendela 8 dtk
 * sejak proses mulai MELESET: proses pasca-instal hidup tanpa UI 8273 ms sebelum Activity dibuat (jendela habis di t=+8000,
 * Activity t=+8439 -> 0 baris `StartupStall`), dan pid 31012 (v681) membuka Activity 3,4 jam setelah proses lahir. Sekarang:
 * - jendela proses (8 dtk dari proses mulai) tetap ada utk kerja sebelum UI (mis. `PlayerWidgetProvider.onUpdate`);
 * - bila jendela itu habis tanpa Activity, probe HANYA menyisakan satu `ActivityLifecycleCallbacks` (tanpa biaya per pesan;
 *   `Looper.setMessageLogging` TIDAK dibiarkan menyala berjam-jam di proses pemutar musik) sampai Activity pertama dibuat;
 * - di `onActivityPreCreated` Activity pertama, jendela baru dibuka (atau jendela proses yang masih terbuka diperpanjang)
 *   selama [StartupStallFormat.WINDOW_MS], lalu semuanya dilepas. Activity kedua dst. tak memicu apa-apa.
 * Batasan platform: `Looper` memakai printer yang ditangkap di AWAL tiap pesan, jadi pesan peluncuran Activity itu sendiri
 * (yang sedang berjalan saat printer dipasang di `onActivityPreCreated`) TIDAK tercatat; yang tercatat = frame pertama
 * (`FrameMetrics`, dipasang di `onActivityPostCreated`) dan semua pesan sesudahnya.
 */
object StartupStallProbe {
    private const val TAG = "StartupStall"

    private val started = AtomicBoolean(false)
    private val collector = StallCollector()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Panggil sekali dari `Application.onCreate` (setelah `AppLogger.init`). Panggilan berikutnya tak berefek. */
    fun start(app: Application) {
        if (!started.compareAndSet(false, true)) return
        runCatching { ProbeSession(app, collector, scope, TAG).begin() }
    }
}

/**
 * Satu sesi probe per proses. SEMUA anggota hanya disentuh thread utama (`Application.onCreate`, callback lifecycle, dan
 * `Handler` utama), kecuali penulisan log yang dilempar ke [scope].
 */
private class ProbeSession(
    private val app: Application,
    private val collector: StallCollector,
    private val scope: CoroutineScope,
    private val tag: String
) : Application.ActivityLifecycleCallbacks {
    private val main = Handler(Looper.getMainLooper())
    private val processStart = Process.getStartUptimeMillis()
    private val frameListener = FrameListener(processStart, collector)
    private val sampleCounter = StackSampleCounter()
    private val slowIntervals = SlowIntervals()
    private val sampler = StackSampler(Looper.getMainLooper().thread, processStart, sampleCounter)
    private val attached = ArrayList<Window>()
    private val closeWindow = Runnable { close() }

    private var open = false
    private var kind = StartupStallFormat.KIND_PROCESS
    private var openedAtMs = 0L
    private var activitySeen = false
    private var activityAtMs: Long? = null

    fun begin() {
        app.registerActivityLifecycleCallbacks(this)
        val remainingMs = StartupStallFormat.processWindowRemainingMs(processStart, SystemClock.uptimeMillis())
        if (remainingMs > 0L) openWindow(StartupStallFormat.KIND_PROCESS, remainingMs)
    }

    private fun sinceProcessStartMs(): Long = SystemClock.uptimeMillis() - processStart

    private fun openWindow(newKind: String, durationMs: Long) {
        kind = newKind
        openedAtMs = sinceProcessStartMs()
        open = true
        Looper.getMainLooper().setMessageLogging(MessagePrinter(processStart, collector, slowIntervals))
        sampler.start()
        main.postDelayed(closeWindow, durationMs)
    }

    /** Activity PERTAMA proses: buka jendela baru, atau perpanjang jendela proses yang masih terbuka. Selanjutnya diabaikan. */
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activitySeen) return
        activitySeen = true
        activityAtMs = sinceProcessStartMs()
        runCatching {
            if (open) {
                kind = StartupStallFormat.KIND_PROCESS_ACTIVITY
                main.removeCallbacks(closeWindow)
                main.postDelayed(closeWindow, StartupStallFormat.WINDOW_MS)
            } else {
                openWindow(StartupStallFormat.KIND_ACTIVITY, StartupStallFormat.WINDOW_MS)
            }
        }
    }

    /**
     * Memasang listener frame ke jendela Activity SETELAH `onCreate` selesai (API 29+: konten sudah dipasang, jadi
     * `getDecorView()` tak mengganggu `requestFeature`/setContent); hanya selama jendela terbuka.
     */
    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (!open) return
        val window = activity.window
        runCatching {
            window.addOnFrameMetricsAvailableListener(frameListener, main)
            attached.add(window)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        val window = activity.window
        runCatching {
            window.removeOnFrameMetricsAvailableListener(frameListener)
            attached.remove(window)
        }
    }

    /** Akhir jendela: lepas semua pengamat, tulis entri bila ada stall besar, kosongkan penampung utk jendela berikutnya. */
    private fun close() {
        runCatching {
            Looper.getMainLooper().setMessageLogging(null)
            sampler.stop()
            attached.forEach { runCatching { it.removeOnFrameMetricsAvailableListener(frameListener) } }
            attached.clear()
            open = false
            val lines = collector.drain()
            val attributionLines = if (lines.isEmpty()) emptyList() else {
                slowIntervals.longest(StartupStallFormat.MAX_ATTRIBUTIONS).flatMap {
                    sampleCounter.topBetween(it.fromMs, it.toMs, StartupStallFormat.ATTRIBUTION_SITES)
                }
            }
            val sampleLines = if (lines.isNotEmpty()) sampleCounter.topLines(StartupStallFormat.MAX_SAMPLE_LINES) else emptyList()
            collector.reset()
            sampleCounter.reset()
            slowIntervals.reset()
            if (lines.isNotEmpty()) {
                val header = StartupStallFormat.windowLine(kind, openedAtMs, activityAtMs)
                // Satu coroutine supaya urutan baris di berkas log sama dengan urutan [lines] (bukan di thread utama: tak menambah stall).
                scope.launch {
                    AppLogger.w(tag, header)
                    lines.forEach { AppLogger.w(tag, it) }
                    attributionLines.forEach { AppLogger.w(tag, it) }
                    sampleLines.forEach { AppLogger.w(tag, it) }
                }
            }
            if (activitySeen) app.unregisterActivityLifecycleCallbacks(this)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}

/** Mengukur durasi tiap pesan thread utama dari pasangan teks `Looper` ">>>>> Dispatching ..." / "<<<<< Finished ...". */
private class MessagePrinter(
    private val processStart: Long,
    private val collector: StallCollector,
    private val slowIntervals: SlowIntervals
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
                if (durationMs >= StartupStallFormat.ATTRIBUTE_MS) slowIntervals.add(atMs - durationMs, atMs)
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
    private val seen = RecentKeys(16)

    override fun onFrameMetricsAvailable(window: Window, frameMetrics: FrameMetrics, dropCountSinceLastInvocation: Int) {
        val totalNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
        val totalMs = StartupStallFormat.nsToMs(totalNs)
        if (totalMs < StartupStallFormat.MIN_LOG_MS) return
        // Batch 684: platform menyerahkan metrik frame yang sama 2-3x (log v683) -> satu frame = satu entri.
        val intendedNs = frameMetrics.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP)
        if (!seen.firstTime(intendedNs)) return
        val sample = FrameSample(
            totalNs = totalNs,
            vsyncDelayNs = frameMetrics.getMetric(FrameMetrics.VSYNC_TIMESTAMP) - intendedNs,
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
 * Batch 684 — "kode mana" yang menahan thread utama: selama jendela terbuka, thread daemon mengambil tumpukan thread utama tiap
 * [StartupStallFormat.SAMPLE_INTERVAL_MS] dan menghitung per lokasi ([StartupStallFormat.siteOf], teks `StackTraceElement`
 * mentah; sampel saat thread utama menganggur dibuang). Dihentikan di akhir jendela; tak menyentuh state app. Catatan: build
 * rilis memakai R8, jadi frame kode app/Compose tampil sebagai nama pendek; frame platform (`android.*`) tetap terbaca.
 */
private class StackSampler(
    private val mainThread: Thread,
    private val processStart: Long,
    private val counter: StackSampleCounter
) {
    @Volatile
    private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "StartupStallSampler").also {
            it.isDaemon = true
            it.start()
        }
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }

    private fun loop() {
        while (running) {
            try {
                Thread.sleep(StartupStallFormat.SAMPLE_INTERVAL_MS)
            } catch (e: InterruptedException) {
                break
            }
            val frames = runCatching { mainThread.stackTrace }.getOrNull() ?: continue
            val site = StartupStallFormat.siteOf(frames.take(StartupStallFormat.SITE_DEPTH).map { it.toString() }) ?: continue
            counter.add(site, SystemClock.uptimeMillis() - processStart)
        }
    }
}
