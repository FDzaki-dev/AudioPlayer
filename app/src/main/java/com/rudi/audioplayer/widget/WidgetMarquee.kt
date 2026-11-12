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
import kotlin.math.min

/**
 * Batch 641/642 — MATEMATIKA marquee judul widget (fungsi murni tanpa Android, bisa diuji unit).
 *
 * Latar: marquee bawaan TextView TIDAK bisa dijalankan dari widget — `TextView.setSelected` tidak remotable (log v639: remotable=false)
 * dan marquee berbasis fokus XML tak andal di launcher (Smart Launcher). Maka app menggeser judul SENDIRI: judul = TextView lebar tetap
 * (4000dp, XML) di dalam viewport FrameLayout yang memotong; app mengirim `partiallyUpdateAppWidget` berisi margin kiri NEGATIF.
 *
 * Batch 642 (user: "udah kaku, tidak mulus looping nya lagi"; log v641 = 10 fps langkah ~3dp + lompat mendadak ke awal):
 * (1) LOOP MULUS seperti marquee asli: teks dipasang DUA SALINAN `judul + jarak + judul`; menggeser sejauh satu PERIODE
 *     (= lebar `judul + jarak`) menaruh salinan ke-2 tepat di posisi salinan ke-1 -> bingkai akhir = bingkai awal, lompat ke 0 tak terlihat.
 *     Bila dua salinan tak muat di lebar TextView (judul sangat panjang) -> mode TERBATAS lama (geser ke ujung, diam, lompat ke awal).
 * (2) Gerak trapesium: percepatan [RAMP_MS] -> kecepatan tetap [SPEED_DP_PER_SEC] -> perlambatan [RAMP_MS] (berhenti halus, bukan stop mendadak).
 * Siklus: diam [START_PAUSE_MS] -> geser -> (mode terbatas: diam [END_PAUSE_MS]) -> ulang.
 */
internal object WidgetMarqueeMath {
    const val SPEED_DP_PER_SEC = 30f
    const val START_PAUSE_MS = 1500L
    const val END_PAUSE_MS = 1200L
    const val RAMP_MS = 500L

    /** Ruang tambahan di ujung (mode terbatas): menyerap selisih ESTIMASI lebar (font/launcher) sehingga huruf terakhir tak terpotong. */
    const val END_SLACK_DP = 12f

    /** Teks yang lebihnya di bawah ini dari kolom dianggap muat (tidak digeser). */
    const val MIN_OVERFLOW_DP = 2f

    /** Batas lebar TextView di XML (4000dp); isi (dua salinan atau satu) tak boleh melewatinya. */
    const val MAX_TEXT_DP = 3900f

    /** Kolom lebih sempit dari ini tak layak digeser (judul praktis tak terlihat). */
    const val MIN_COLUMN_DP = 24f

    /** Jarak antar salinan minimum (dp) pada mode loop. */
    const val MIN_GAP_DP = 24f

    /** [travelDp] = jarak geser per siklus (dp; 0 = tak digeser); [loop] = teks digandakan dan jarak = satu periode. */
    class Plan(val loop: Boolean, val travelDp: Float)

    val NONE = Plan(false, 0f)

    /** Jarak antar salinan: sepertiga kolom (seperti marquee bawaan), minimal [MIN_GAP_DP]. */
    fun gapDp(columnDp: Float): Float = maxOf(MIN_GAP_DP, columnDp / 3f)

    /**
     * @param textDp lebar teks (sudah dikali faktor aman), @param periodDp lebar PERSIS `judul + jarak` (tanpa faktor aman — harus sama
     * dengan geometri render agar loop mulus), @param columnDp lebar kolom judul minimum.
     */
    fun plan(textDp: Float, periodDp: Float, columnDp: Float): Plan {
        if (columnDp < MIN_COLUMN_DP) return NONE
        if (textDp - columnDp < MIN_OVERFLOW_DP) return NONE
        if (periodDp + textDp <= MAX_TEXT_DP) return Plan(true, periodDp)
        return Plan(false, min(textDp, MAX_TEXT_DP) - columnDp + END_SLACK_DP)
    }

    /** Lama fase geser (ms): jarak / kecepatan + satu [RAMP_MS] (trapesium: 2 ramp mengurangi jarak setara satu ramp penuh). */
    fun scrollMs(travelDp: Float): Long = (travelDp / SPEED_DP_PER_SEC * 1000f).toLong() + RAMP_MS

    fun endPauseMs(loop: Boolean): Long = if (loop) 0L else END_PAUSE_MS

    fun cycleMs(travelDp: Float, loop: Boolean): Long = START_PAUSE_MS + scrollMs(travelDp) + endPauseMs(loop)

    /** Offset geser (dp, 0..travel) pada [elapsedMs] sejak siklus dimulai. [travelDp] <= 0 selalu 0. */
    fun offsetDp(travelDp: Float, loop: Boolean, elapsedMs: Long): Float {
        if (travelDp <= 0f || elapsedMs < 0L) return 0f
        val t = elapsedMs % cycleMs(travelDp, loop)
        if (t < START_PAUSE_MS) return 0f
        val tau = t - START_PAUSE_MS
        val total = scrollMs(travelDp)
        if (tau >= total) return travelDp
        val v = SPEED_DP_PER_SEC
        val r = RAMP_MS / 1000f
        val s = tau / 1000f
        val tt = total / 1000f
        val d = when {
            s < r -> v * s * s / (2f * r)
            s < tt - r -> v * (s - r / 2f)
            else -> travelDp - v * (tt - s) * (tt - s) / (2f * r)
        }
        return d.coerceIn(0f, travelDp)
    }

    /**
     * Jeda (ms, >= 1) sampai kiriman berikutnya perlu: selama fase geser = [tickMs]; selama diam = sisa diam
     * (tak ada kiriman sia-sia saat judul tidak bergerak).
     */
    fun delayToNextMs(travelDp: Float, loop: Boolean, elapsedMs: Long, tickMs: Long): Long {
        if (travelDp <= 0f) return Long.MAX_VALUE
        val cycle = cycleMs(travelDp, loop)
        val t = elapsedMs % cycle
        val scrollEnd = START_PAUSE_MS + scrollMs(travelDp)
        return when {
            t < START_PAUSE_MS -> START_PAUSE_MS - t
            t < scrollEnd -> tickMs
            else -> cycle - t
        }.coerceAtLeast(1L)
    }
}

/**
 * Batch 641 — penggulir judul widget yang digerakkan app (lihat [WidgetMarqueeMath]). Satu HandlerThread; tiap "lajur" = satu widget
 * dengan judul yang melebihi kolomnya. Berhenti sendiri bila layar mati (ACTION_SCREEN_OFF), tak ada lajur, atau 3 kegagalan beruntun.
 * Saat lagu TIDAK diputar, tiap lajur hanya [IDLE_CYCLES] siklus lalu berhenti di posisi awal (hemat baterai); layar menyala = mulai lagi.
 * Update penuh ([WidgetUpdater.performUpdate]) mengatur ulang margin ke 0 dan memanggil [configure].
 */
internal object WidgetMarqueeDriver {
    private const val TAG = "WidgetMarquee"
    // Batch 642 — 33 ms (~30 fps; langkah ~1dp pada 30dp/dtk) dari 100 ms (~3dp = patah-patah). Dijadwalkan terhadap waktu mulai tick (bukan tertunda
    // oleh lama panggilan binder); posisi dihitung dari waktu nyata, jadi tick yang terlambat tidak memperlambat geseran.
    private const val TICK_MS = 33L
    private const val IDLE_CYCLES = 3
    private const val MAX_CONSECUTIVE_FAILURES = 3

    /** Satu widget yang judulnya perlu digeser; [layoutRes] WAJIB sama dengan layout update penuh terakhir (host mendaur ulang view per layout id). */
    internal class Lane(val widgetId: Int, val layoutRes: Int, val travelDp: Float, val loop: Boolean)

    private class Snapshot(val lanes: List<Lane>, val playing: Boolean, val startedAtMs: Long)

    @Volatile private var snapshot: Snapshot? = null
    @Volatile private var screenOn = true
    private val lastSentPx = HashMap<Int, Int>() // hanya diakses di thread handler
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
            "Penggulir judul aktif: ${lanes.size} widget, jarak geser dp=${lanes.joinToString { "${it.widgetId}:${it.travelDp.toInt()}${if (it.loop) "L" else "T"}" }} " +
                "playing=$playing layarMenyala=$screenOn"
        )
        val h = handler ?: return
        h.post {
            // Update penuh barusan mengatur margin = 0, jadi 0 dianggap sudah terkirim (tak ada kiriman sia-sia).
            lastSentPx.clear()
            for (lane in lanes) lastSentPx[lane.widgetId] = 0
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
            // Layar menyala = pengguna mungkin melihat widget: siklus baru dari awal (juga saat lagu dijeda).
            snapshot = Snapshot(snap.lanes, snap.playing, SystemClock.uptimeMillis())
            h.post {
                for (lane in snap.lanes) lastSentPx.remove(lane.widgetId)
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
        val tickStart = SystemClock.uptimeMillis()
        try {
            val manager = AppWidgetManager.getInstance(context)
            val elapsed = tickStart - snap.startedAtMs
            val density = context.resources.displayMetrics.density
            for (lane in snap.lanes) {
                val finished = !snap.playing && elapsed >= IDLE_CYCLES * WidgetMarqueeMath.cycleMs(lane.travelDp, lane.loop)
                val offsetDp = if (finished) 0f else WidgetMarqueeMath.offsetDp(lane.travelDp, lane.loop, elapsed)
                val px = (offsetDp * density).toInt()
                if (lastSentPx[lane.widgetId] != px) {
                    val views = RemoteViews(context.packageName, lane.layoutRes)
                    views.setViewLayoutMargin(R.id.widget_title, RemoteViews.MARGIN_LEFT, -offsetDp, TypedValue.COMPLEX_UNIT_DIP)
                    manager.partiallyUpdateAppWidget(lane.widgetId, views)
                    lastSentPx[lane.widgetId] = px
                }
                if (!finished) {
                    nextDelay = min(nextDelay, WidgetMarqueeMath.delayToNextMs(lane.travelDp, lane.loop, elapsed, TICK_MS))
                }
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
            nextDelay = TICK_MS
        }
        if (nextDelay != Long.MAX_VALUE && snapshot === snap) {
            // Fase geser: kurangi waktu kerja tick ini agar irama tetap ~TICK_MS (bukan TICK_MS + lama panggilan binder).
            val spent = SystemClock.uptimeMillis() - tickStart
            handler?.postDelayed(tick, if (nextDelay == TICK_MS) (TICK_MS - spent).coerceAtLeast(1L) else nextDelay)
        }
    }
}
