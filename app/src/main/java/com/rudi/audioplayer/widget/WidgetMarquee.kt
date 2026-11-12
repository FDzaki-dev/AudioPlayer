package com.rudi.audioplayer.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.SystemClock
import android.util.TypedValue
import android.widget.RemoteViews
import com.rudi.audioplayer.R
import com.rudi.audioplayer.util.AppLogger
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Batch 644 — MATEMATIKA marquee judul widget (fungsi murni tanpa Android, bisa diuji unit).
 *
 * Riwayat: marquee bawaan TextView tak bisa jalan di widget (`setSelected` tidak remotable, log v639: remotable=false; fokus XML tak andal di
 * Smart Launcher). v641 = app mengirim margin per 100 ms (patah-patah). v642 = 30 fps + loop mulus (masih patah di layar 120 Hz: user "gak ada
 * cara lebih organik?" + refresh 60 Hz diam / 120 Hz dipakai).
 *
 * Batch 643 — GERAK DIGAMBAR LAUNCHER: judul = TextView 4000dp (`widget_title`) berisi DUA salinan `judul + spasi + judul` di dalam ViewFlipper
 * (`widget_title_flipper`, tanpa autoStart) dengan animasi masuk `@anim/widget_title_segment` (mengikuti refresh rate layar). Tiap awal segmen app
 * mengirim 1 update parsial: margin kiri = -k x SEGMENT_DP + `setDisplayedChild(0)` (mengulang animasi dari 0 pada frame yang sama dengan pergeseran
 * margin -> posisi kontinu). Satu LOOP = [q] segmen, q = periode / SEGMENT_DP; periode (lebar `judul + spasi`) dibulatkan ke kelipatan SEGMENT_DP oleh
 * WidgetUpdater (spasi) agar akhir loop = bingkai awal (salinan ke-2 tepat di posisi salinan ke-1). Setelah loop: diam [PAUSE_MS] (ujung animasi
 * terakhir ditahan `fillAfter`, tampilannya identik dengan awal), lalu loop berikutnya.
 *
 * Batch 644 — v643 (segmen 30dp / 1 dtk) gerak maju tapi TERSENDAT tiap ~1 dtk (laporan user: "berkedut kedut", gejala dipilih: "Maju terus, tapi tersendat
 * tiap ±1 detik"). Dugaan (belum terbukti): tiap update parsial membuat launcher menerapkan ulang seluruh RemoteViews tersimpan -> frame jatuh sesaat, dan
 * jumlahnya sebanding dgn jumlah update. Perbaikan: segmen 90dp / 3 dtk (kecepatan SAMA 30dp/dtk) -> update 3x lebih jarang; selain itu jumlah segmen
 * dipilih ke kelipatan TERDEKAT (bukan selalu ke atas) agar jarak antar salinan tak melebar rata-rata ~45dp ([segmentsForText]).
 */
internal object WidgetMarqueeMath {
    const val SPEED_DP_PER_SEC = 30f

    /** Jarak satu segmen (dp) = 2,25% x 4000dp di `anim/widget_title_segment.xml`; JANGAN diubah tanpa mengubah XML itu. */
    const val SEGMENT_DP = 90f

    /** Durasi satu segmen (ms) = `android:duration` di `anim/widget_title_segment.xml`. */
    const val SEGMENT_MS = 3000L

    /** Diam di awal tiap loop (judul terbaca dari awal). */
    const val PAUSE_MS = 1500L

    /** Batas jumlah segmen (17 x 90dp = periode 1530dp, +-190 huruf; 2 salinan < 4000dp TextView); judul lebih panjang tampil statis (ellipsize). */
    const val MAX_SEGMENTS = 17

    /** Teks yang lebihnya di bawah ini dari kolom dianggap muat (tidak digeser). */
    const val MIN_OVERFLOW_DP = 2f

    /** Kolom lebih sempit dari ini tak layak digeser (judul praktis tak terlihat). */
    const val MIN_COLUMN_DP = 24f

    /** Jarak antar salinan minimum (dp). */
    const val MIN_GAP_DP = 24f

    /** Jarak antar salinan: sepertiga kolom (seperti marquee bawaan), minimal [MIN_GAP_DP]. */
    fun gapDp(columnDp: Float): Float = max(MIN_GAP_DP, columnDp / 3f)

    fun shouldScroll(textDp: Float, columnDp: Float): Boolean =
        columnDp >= MIN_COLUMN_DP && textDp - columnDp >= MIN_OVERFLOW_DP

    /** Jumlah segmen (>= 1) agar q x SEGMENT_DP >= [periodDp]. */
    fun segmentsFor(periodDp: Float): Int = ceil(periodDp / SEGMENT_DP).toInt().coerceAtLeast(1)

    /**
     * Jumlah segmen untuk teks selebar [textDp] di kolom [columnDp]: kelipatan SEGMENT_DP TERDEKAT dari `teks + gapDp(kolom)`, tetapi jarak antar salinan
     * tak pernah di bawah [MIN_GAP_DP] (jadi dibulatkan ke atas bila perlu).
     */
    fun segmentsForText(textDp: Float, columnDp: Float): Int {
        val nearest = ((textDp + gapDp(columnDp)) / SEGMENT_DP).roundToInt()
        return max(segmentsFor(textDp + MIN_GAP_DP), nearest)
    }

    fun targetPeriodDp(segments: Int): Float = segments * SEGMENT_DP

    fun loopMs(segments: Int): Long = segments * SEGMENT_MS

    fun cycleMs(segments: Int): Long = loopMs(segments) + PAUSE_MS

    /** Margin kiri (dp, <= 0) untuk awal segmen ke-[segment]. */
    fun marginDp(segment: Int): Float = -segment * SEGMENT_DP

    /**
     * Kunci kejadian kirim (loop x segmen + indeks segmen) pada [elapsedMs] sejak siklus dimulai; -1 selama diam di antara loop.
     * Kunci naik tepat 1 tiap SEGMENT_MS selama loop, sehingga kiriman tak pernah ganda dan tak pernah terlewat.
     */
    fun eventKey(segments: Int, elapsedMs: Long): Long {
        if (elapsedMs < 0L) return -1L
        val cycle = cycleMs(segments)
        val t = elapsedMs % cycle
        if (t >= loopMs(segments)) return -1L
        return (elapsedMs / cycle) * segments + t / SEGMENT_MS
    }

    /** Indeks segmen (0 until segments) dari kunci kejadian. */
    fun segmentOfKey(segments: Int, key: Long): Int = (key % segments).toInt()

    /** Jeda (ms, >= 1) sampai kejadian kirim berikutnya: batas segmen berikut, atau awal loop berikutnya setelah segmen terakhir. */
    fun delayToNextMs(segments: Int, elapsedMs: Long): Long {
        val cycle = cycleMs(segments)
        val t = elapsedMs % cycle
        val next = if (t < (segments - 1) * SEGMENT_MS) (t / SEGMENT_MS + 1) * SEGMENT_MS else cycle
        return (next - t).coerceAtLeast(1L)
    }
}

/**
 * Batch 643 — penggerak segmen marquee judul widget (lihat [WidgetMarqueeMath]). Satu HandlerThread; tiap "lajur" = satu widget dengan judul yang melebihi
 * kolomnya. Hanya 1 update parsial per SEGMEN (v644: 3 dtk; v643: 1 dtk; v642: 30/dtk) per widget; gerak halusnya digambar launcher. Berhenti sendiri bila layar mati
 * (ACTION_SCREEN_OFF), tak ada lajur, atau 3 kegagalan beruntun. Saat lagu TIDAK diputar tiap lajur hanya [IDLE_LOOPS] loop lalu berhenti pada akhir loop
 * (tampilan akhir = awal, tak perlu reset); layar menyala = siklus baru. Update penuh ([WidgetUpdater.performUpdate]) menata ulang margin = 0 + memulai
 * segmen ke-0 lalu memanggil [configure].
 */
internal object WidgetMarqueeDriver {
    private const val TAG = "WidgetMarquee"
    private const val IDLE_LOOPS = 3
    private const val MAX_CONSECUTIVE_FAILURES = 3

    /** Satu widget yang judulnya digeser; [layoutRes] WAJIB sama dengan layout update penuh terakhir (host mendaur ulang view per layout id). */
    internal class Lane(val widgetId: Int, val layoutRes: Int, val segments: Int)

    private class Snapshot(val lanes: List<Lane>, val playing: Boolean, val startedAtMs: Long)

    @Volatile private var snapshot: Snapshot? = null
    @Volatile private var screenOn = true
    private val lastKey = HashMap<Int, Long>() // hanya diakses di thread handler
    private var failures = 0 // hanya diakses di thread handler

    @Volatile private var handler: Handler? = null
    private var appContext: Context? = null
    private var receiverRegistered = false

    private val tick = Runnable { runTick() }

    /** Dipanggil di akhir update penuh (di dalam updateLock). [lanes] kosong = hentikan. */
    @Synchronized
    fun configure(context: Context, lanes: List<Lane>, playing: Boolean) {
        if (lanes.isEmpty()) {
            stop()
            return
        }
        ensureStarted(context)
        snapshot = Snapshot(lanes, playing, SystemClock.uptimeMillis())
        AppLogger.w(
            TAG,
            "Penggulir judul (segmen launcher) aktif: ${lanes.size} widget, segmen 90dp/3dtk=${lanes.joinToString { "${it.widgetId}:${it.segments}" }} " +
                "playing=$playing layarMenyala=$screenOn"
        )
        val h = handler ?: return
        h.post {
            // Update penuh barusan menata margin = 0 dan memulai segmen ke-0, jadi kunci 0 dianggap sudah terkirim.
            lastKey.clear()
            for (lane in lanes) lastKey[lane.widgetId] = 0L
            failures = 0
            h.removeCallbacks(tick)
            if (screenOn) h.post(tick)
        }
    }

    @Synchronized
    fun stop() {
        snapshot = null
        handler?.removeCallbacks(tick)
    }

    private fun ensureStarted(context: Context) {
        if (handler == null) {
            val thread = HandlerThread("WidgetMarquee")
            thread.start()
            handler = Handler(thread.looper)
        }
        if (!receiverRegistered) {
            val app = context.applicationContext
            appContext = app
            screenOn = try {
                (app.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
            } catch (e: Exception) {
                true
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    onScreen(intent.action == Intent.ACTION_SCREEN_ON)
                }
            }
            val filter = IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_SCREEN_OFF) }
            try {
                app.registerReceiver(receiver, filter)
                receiverRegistered = true
            } catch (e: Exception) {
                AppLogger.e(TAG, "Gagal mendaftarkan penerima layar", e)
            }
        }
    }

    private fun onScreen(on: Boolean) {
        screenOn = on
        val h = handler ?: return
        h.removeCallbacks(tick)
        val snap = snapshot ?: return
        if (on) {
            // Layar menyala = pengguna mungkin melihat widget: siklus baru dari awal (juga saat lagu dijeda). Kunci dihapus agar segmen ke-0
            // DIKIRIM (posisi di host bisa di mana saja setelah layar mati).
            snapshot = Snapshot(snap.lanes, snap.playing, SystemClock.uptimeMillis())
            h.post {
                for (lane in snap.lanes) lastKey.remove(lane.widgetId)
                failures = 0
                h.post(tick)
            }
        }
    }

    private fun runTick() {
        val snap = snapshot ?: return
        if (!screenOn) return
        val context = appContext ?: return
        var nextDelay = Long.MAX_VALUE
        try {
            val manager = AppWidgetManager.getInstance(context)
            val elapsed = SystemClock.uptimeMillis() - snap.startedAtMs
            for (lane in snap.lanes) {
                val finished = !snap.playing && elapsed >= IDLE_LOOPS * WidgetMarqueeMath.cycleMs(lane.segments)
                if (finished) continue
                val key = WidgetMarqueeMath.eventKey(lane.segments, elapsed)
                if (key >= 0L && lastKey[lane.widgetId] != key) {
                    val views = RemoteViews(context.packageName, lane.layoutRes)
                    // Urutan penting: margin dulu, lalu ulang animasi (T=0) -> posisi kontinu pada frame yang sama.
                    views.setViewLayoutMargin(
                        R.id.widget_title, RemoteViews.MARGIN_LEFT,
                        WidgetMarqueeMath.marginDp(WidgetMarqueeMath.segmentOfKey(lane.segments, key)), TypedValue.COMPLEX_UNIT_DIP
                    )
                    views.setDisplayedChild(R.id.widget_title_flipper, 0)
                    manager.partiallyUpdateAppWidget(lane.widgetId, views)
                    lastKey[lane.widgetId] = key
                }
                nextDelay = min(nextDelay, WidgetMarqueeMath.delayToNextMs(lane.segments, elapsed))
            }
            failures = 0
        } catch (e: Exception) {
            failures++
            AppLogger.e(TAG, "Gagal menggeser judul widget (#$failures)", e)
            if (failures >= MAX_CONSECUTIVE_FAILURES) {
                AppLogger.w(TAG, "Penggulir judul dihentikan setelah $failures kegagalan beruntun")
                snapshot = null
                return
            }
            nextDelay = 1000L
        }
        if (nextDelay != Long.MAX_VALUE && snapshot === snap) handler?.postDelayed(tick, nextDelay)
    }
}
