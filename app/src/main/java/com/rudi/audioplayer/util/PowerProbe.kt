package com.rudi.audioplayer.util

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicIntegerArray

/**
 * Batch 669 — "catcher" daya: mengamati SEBERAPA BOROS daya yang diserap aplikasi ini, ditulis apa adanya ke
 * [AppLogger] (tag `PowerProbe`; ikut masuk "Ekspor Logcat Debug"). Permintaan user: "tolong pasangkan catcher untuk
 * pengamatan seberapa boros daya yang diserap aplikasi". Log v668 tidak punya data baterai sama sekali, sehingga
 * penghematan Batch 668 tak bisa dibuktikan.
 *
 * Cara kerja (murah: ~1 baca /proc + 1 baris log per segmen, tanpa alarm/wakelock — timer memakai uptime sehingga TIDAK
 * membangunkan CPU dari tidur): proses dibagi jadi SEGMEN. Segmen ditutup (dan satu baris dicatat) saat layar
 * menyala/mati, charger tersambung/dicabut, dan tiap [PERIODIC_MS] (10 menit). Tiap baris memuat untuk segmen itu:
 *  - dur / awake: lama nyata dan persentase waktu CPU HP terjaga (uptime/elapsed) — rendah = HP sempat tidur dalam;
 *  - cpuApp: waktu CPU proses ini (persen satu core terhadap waktu nyata) + 5 thread teratas (nama = `comm` Linux,
 *    terpotong 15 huruf; mis. RenderThread = gambar UI/bubble, main = thread utama, ExoPlayer:* = pemutaran);
 *  - bat / kuras: persen baterai awal->akhir dan KURAS NYATA dari `BATTERY_PROPERTY_CHARGE_COUNTER` (uAh; mA rata-rata =
 *    uAh*3,6/dtk). Ini daya seluruh HP (layar, radio, app lain), BUKAN hanya app ini — baca bersama cpuApp & mode layar;
 *  - arus_kini / suhu: pembacaan sesaat; keadaan sesaat: musik aktif, kepentingan proses (FG/FGS/VIS/SVC/CACHED), hemat
 *    daya, doze, termal;
 *  - hit: penghitung frame animasi bubble & latar kosmos (frame/dtk) — penyebab dugaan boros saat layar menyala;
 *  - kum: rata-rata kumulatif sejak proses mulai per mode layar (mA rata-rata, % CPU), tanpa segmen charger.
 * Segmen dengan charger tersambung ditandai CHARGING dan tak dihitung ke kumulatif. Segmen < [MIN_LOG_SEGMENT_MS]
 * (layar berkedip) tetap dihitung ke kumulatif tetapi barisnya tak dicatat.
 *
 * Aman: seluruh kerja di thread latar sendiri, setiap sampel dibungkus try/catch (galat dicatat mentah via
 * [AppLogger.e]), tak ada I/O di thread utama. Hanya android.* + JDK (aturan ArchUnit "util mandiri").
 */
object PowerProbe {

    /** Penghitung lock-free untuk pekerjaan berkala yang dicurigai menguras daya (panggil [hit] dari mana saja). */
    enum class Hit { BUBBLE_FRAME, COSMOS_FRAME }

    private const val TAG = "PowerProbe"
    private const val PERIODIC_MS = 600_000L
    private const val MIN_LOG_SEGMENT_MS = 20_000L
    private const val MIN_CUMULATIVE_S = 60.0
    private const val TOP_THREADS = 5
    private const val MIN_THREAD_PERCENT = 0.1
    private const val DEFAULT_CLK_TCK = 100L
    private const val UNSUPPORTED = Long.MIN_VALUE
    private const val UNSUPPORTED_INT = Int.MIN_VALUE
    private const val MS_PER_S = 1000.0
    private const val SECONDS_PER_MIN = 60L
    private const val SECONDS_PER_HOUR = 3600L
    private const val UAH_TO_MA = 3.6 // uAh per dtk -> mA rata-rata: uAh * 3600 dtk / dtk / 1000
    private const val OFFLOAD_RATE_44K = 44_100
    private const val OFFLOAD_RATE_48K = 48_000
    private const val UA_PER_MA = 1000
    private const val PERCENT = 100.0
    private const val DECI = 10.0
    private const val STAT_UTIME_INDEX = 11 // setelah ')' + spasi: field 3 = indeks 0 -> utime (field 14) = 11
    private const val STAT_STIME_INDEX = 12
    private const val LINE_CAPACITY = 640
    private const val TRIGGER_PERIODIC = "periodik"

    private val hits = AtomicIntegerArray(Hit.entries.size)
    private var handler: Handler? = null
    private var appContext: Context? = null
    private var started = false
    private var mark: Mark? = null
    private var clkTck = DEFAULT_CLK_TCK
    @Volatile private var stateExtras: (() -> String)? = null
    private val totalOn = Total()
    private val totalOff = Total()

    private class ThreadTicks(val name: String, val ticks: Long)

    private class Mark(
        val elapsedMs: Long,
        val uptimeMs: Long,
        val cpuMs: Long,
        val chargeUah: Long,
        val levelPct: Int,
        val charging: Boolean,
        val tempDeciC: Int,
        val millivolt: Int,
        val currentUa: Int,
        val screenOn: Boolean,
        val threads: Map<Int, ThreadTicks>,
        val hits: IntArray
    )

    private class Total {
        var seconds = 0.0
        var uah = 0L
        var cpuMs = 0L
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> closeSegment("layar_nyala", true)
                Intent.ACTION_SCREEN_OFF -> closeSegment("layar_mati", false)
                Intent.ACTION_POWER_CONNECTED -> closeSegment("charger_tersambung", null)
                Intent.ACTION_POWER_DISCONNECTED -> closeSegment("charger_dicabut", null)
                else -> Unit
            }
        }
    }

    private val periodic = object : Runnable {
        override fun run() {
            closeSegment(TRIGGER_PERIODIC, null)
            handler?.postDelayed(this, PERIODIC_MS)
        }
    }

    /** Hitung satu kejadian (murah, lock-free; aman dari thread mana pun termasuk thread utama). */
    fun hit(kind: Hit) {
        hits.incrementAndGet(kind.ordinal)
    }

    /**
     * Batch 670 — teks status tambahan yang dibaca di tiap baris (saklar fitur yang memengaruhi CPU pemutaran: crossfade, trim
     * keheningan) agar angka `cpuApp`/`thr` bisa dikaitkan ke konfigurasi tanpa menebak. Diisi dari lapisan atas
     * (`AudioPlayerApplication`) supaya `util` tetap mandiri (ArchUnit). Provider dipanggil di thread probe; kegagalannya dicatat di
     * barisnya sendiri dan tak pernah mematikan pencatat.
     */
    fun setStateExtras(provider: () -> String) {
        stateExtras = provider
    }

    /** Panggil sekali dari Application.onCreate(). Aman dipanggil berulang (diabaikan setelah yang pertama). */
    @Synchronized
    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        appContext = app
        val thread = HandlerThread("PowerProbe", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        val probeHandler = Handler(thread.looper)
        handler = probeHandler
        probeHandler.post { begin(app, probeHandler) }
    }

    /**
     * Batch 674 — dukungan audio offload perangkat per format (`AudioManager.getPlaybackOffloadSupport`, API 31): 0 = tidak, 1 = ya,
     * 2 = ya + gapless. Dicatat sekali saat mulai untuk menjawab apakah offload (Batch 672) BISA aktif untuk format yang diputar;
     * log v673 menunjukkan AudioTrack tetap PCM (`flags=0x0`) pada lagu Opus. Gagal vendor dicatat sebagai teks, tak mematikan pencatat.
     */
    private fun describeOffloadSupport(): String = runCatching {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        listOf(
            Triple("mp3", AudioFormat.ENCODING_MP3, OFFLOAD_RATE_44K),
            Triple("aac", AudioFormat.ENCODING_AAC_LC, OFFLOAD_RATE_44K),
            Triple("opus", AudioFormat.ENCODING_OPUS, OFFLOAD_RATE_48K)
        ).joinToString(" ") { (name, encoding, rate) ->
            val format = AudioFormat.Builder()
                .setEncoding(encoding)
                .setSampleRate(rate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
            "$name=${AudioManager.getPlaybackOffloadSupport(format, attrs)}"
        }
    }.getOrElse { "galat:${it.javaClass.simpleName}" }

    private fun begin(ctx: Context, probeHandler: Handler) {
        try {
            clkTck = runCatching { Os.sysconf(OsConstants._SC_CLK_TCK) }.getOrDefault(DEFAULT_CLK_TCK)
                .takeIf { it > 0L } ?: DEFAULT_CLK_TCK
            val power = ctx.getSystemService(PowerManager::class.java)
            val first = readMark(ctx, power?.isInteractive ?: true)
            mark = first
            val filter = IntentFilter(Intent.ACTION_SCREEN_ON).apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            ctx.registerReceiver(receiver, filter, null, probeHandler)
            AppLogger.w(
                TAG,
                "[mulai] pencatat daya aktif: periodik ${PERIODIC_MS / MS_PER_S.toLong() / SECONDS_PER_MIN} mnt + layar/charger | " +
                    "layar=${if (first.screenOn) "nyala" else "mati"} bat=${first.levelPct}% charging=${first.charging} " +
                    "charge_counter=${if (first.chargeUah == UNSUPPORTED) "TIDAK didukung" else "${first.chargeUah}uAh"} " +
                    "arus_kini=${if (first.currentUa == UNSUPPORTED_INT) "TIDAK didukung" else "${first.currentUa / UA_PER_MA}mA"} " +
                    "clkTck=$clkTck thread=${first.threads.size}"
            )
            AppLogger.w(TAG, "[offload] dukungan perangkat (0=tidak 1=ya 2=ya+gapless): ${describeOffloadSupport()}")
            probeHandler.postDelayed(periodic, PERIODIC_MS)
        } catch (e: Exception) {
            AppLogger.e(TAG, "gagal memulai pencatat daya", e)
        }
    }

    private fun closeSegment(trigger: String, newScreenOn: Boolean?) {
        val ctx = appContext ?: return
        val prev = mark ?: return
        try {
            val now = readMark(ctx, newScreenOn ?: prev.screenOn)
            mark = now
            val dtMs = now.elapsedMs - prev.elapsedMs
            if (dtMs <= 0L) return
            accumulate(prev, now, dtMs)
            if (dtMs >= MIN_LOG_SEGMENT_MS || trigger == TRIGGER_PERIODIC) {
                AppLogger.w(TAG, buildLine(ctx, trigger, prev, now, dtMs))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "sampel '$trigger' gagal", e)
        }
    }

    private fun accumulate(prev: Mark, now: Mark, dtMs: Long) {
        if (prev.charging || now.charging) return
        if (prev.chargeUah == UNSUPPORTED || now.chargeUah == UNSUPPORTED) return
        val total = if (prev.screenOn) totalOn else totalOff
        total.seconds += dtMs / MS_PER_S
        total.uah += prev.chargeUah - now.chargeUah
        total.cpuMs += now.cpuMs - prev.cpuMs
    }

    private fun readMark(ctx: Context, screenOn: Boolean): Mark {
        val sticky = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val manager = ctx.getSystemService(BatteryManager::class.java)
        val charge = manager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: UNSUPPORTED
        val current = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: UNSUPPORTED_INT
        return Mark(
            elapsedMs = SystemClock.elapsedRealtime(),
            uptimeMs = SystemClock.uptimeMillis(),
            cpuMs = Process.getElapsedCpuTime(),
            chargeUah = if (charge > 0L) charge else UNSUPPORTED,
            levelPct = if (level >= 0 && scale > 0) (level * PERCENT / scale).toInt() else -1,
            charging = plugged != 0 || status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
            tempDeciC = sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, UNSUPPORTED_INT) ?: UNSUPPORTED_INT,
            millivolt = sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1,
            currentUa = current,
            screenOn = screenOn,
            threads = readThreads(),
            hits = IntArray(hits.length()) { hits.get(it) }
        )
    }

    /** CPU per thread proses ini dari `/proc/self/task/<tid>/stat` (utime+stime dalam tick `clkTck`). */
    private fun readThreads(): Map<Int, ThreadTicks> {
        val dirs = File("/proc/self/task").listFiles() ?: return emptyMap()
        val out = HashMap<Int, ThreadTicks>(dirs.size * 2)
        for (dir in dirs) {
            val tid = dir.name.toIntOrNull() ?: continue
            val stat = runCatching { File(dir, "stat").readText() }.getOrNull() ?: continue
            val open = stat.indexOf('(')
            val close = stat.lastIndexOf(')')
            if (open < 0 || close < open) continue
            val rest = stat.substring(close + 2).split(' ')
            val utime = rest.getOrNull(STAT_UTIME_INDEX)?.toLongOrNull() ?: continue
            val stime = rest.getOrNull(STAT_STIME_INDEX)?.toLongOrNull() ?: continue
            out[tid] = ThreadTicks(stat.substring(open + 1, close), utime + stime)
        }
        return out
    }

    private fun buildLine(ctx: Context, trigger: String, prev: Mark, now: Mark, dtMs: Long): String {
        val dtS = dtMs / MS_PER_S
        val cpuMs = now.cpuMs - prev.cpuMs
        val upMs = now.uptimeMs - prev.uptimeMs
        val sb = StringBuilder(LINE_CAPACITY)
        sb.append('[').append(trigger).append("] layar=").append(if (prev.screenOn) "nyala" else "mati")
            .append(" dur=").append(fmtDur(dtMs))
            .append(" awake=").append(fmt(upMs * PERCENT / dtMs, 0)).append('%')
            .append(" cpuApp=").append(fmt(cpuMs * PERCENT / dtMs, 2)).append("% (").append(fmt(cpuMs / MS_PER_S, 1))
            .append("dtk) thr: ").append(describeThreads(prev, now, dtMs, cpuMs))
        appendBattery(sb, prev, now, dtS)
        sb.append(" | ").append(describeState(ctx))
        sb.append(" | hit:").append(describeHits(prev, now, dtS))
        sb.append(describeTotals())
        return sb.toString()
    }

    private fun appendBattery(sb: StringBuilder, prev: Mark, now: Mark, dtS: Double) {
        sb.append(" | bat=").append(prev.levelPct).append("->").append(now.levelPct).append('%')
        val charging = prev.charging || now.charging
        if (charging) {
            sb.append(" CHARGING(kuras diabaikan)")
        } else if (prev.chargeUah == UNSUPPORTED || now.chargeUah == UNSUPPORTED) {
            sb.append(" kuras=n/a")
        } else {
            val uah = prev.chargeUah - now.chargeUah
            sb.append(" kuras=").append(uah).append("uAh(~").append(fmt(uah * UAH_TO_MA / dtS, 0)).append("mA)")
        }
        if (now.currentUa != UNSUPPORTED_INT) sb.append(" arus_kini=").append(now.currentUa / UA_PER_MA).append("mA")
        if (now.tempDeciC != UNSUPPORTED_INT) sb.append(" suhu=").append(fmt(now.tempDeciC / DECI, 1)).append('C')
        if (now.millivolt > 0) sb.append(" volt=").append(now.millivolt).append("mV")
    }

    private fun describeThreads(prev: Mark, now: Mark, dtMs: Long, cpuMs: Long): String {
        val perName = HashMap<String, Long>()
        for ((tid, thread) in now.threads) {
            val before = prev.threads[tid]
            val delta = if (before != null && before.name == thread.name) thread.ticks - before.ticks else thread.ticks
            if (delta > 0L) perName[thread.name] = (perName[thread.name] ?: 0L) + delta
        }
        val tickMs = MS_PER_S / clkTck
        val ranked = perName.entries.sortedByDescending { it.value }
        val top = ranked.take(TOP_THREADS)
            .map { it.key to it.value * tickMs * PERCENT / dtMs }
            .filter { it.second >= MIN_THREAD_PERCENT }
        val rest = (cpuMs - ranked.sumOf { it.value } * tickMs).coerceAtLeast(0.0) * PERCENT / dtMs
        val text = top.joinToString(" ") { "${it.first}=${fmt(it.second, 2)}%" }
        return (if (text.isEmpty()) "-" else text) + " sisa(thread selesai)=" + fmt(rest, 2) + "%"
    }

    private fun describeState(ctx: Context): String {
        val audio = ctx.getSystemService(AudioManager::class.java)
        val power = ctx.getSystemService(PowerManager::class.java)
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return "musik=${yn(audio?.isMusicActive)} impor=${importanceLabel(info.importance)}(${info.importance}) " +
            "hemat=${yn(power?.isPowerSaveMode)} doze=${yn(power?.isDeviceIdleMode)} termal=${power?.currentThermalStatus ?: -1}" +
            describeExtras()
    }

    private fun describeExtras(): String {
        val provider = stateExtras ?: return ""
        return " " + runCatching { provider() }.getOrElse { "extra=galat:${it.javaClass.simpleName}" }
    }

    private fun describeHits(prev: Mark, now: Mark, dtS: Double): String {
        val sb = StringBuilder()
        for (kind in Hit.entries) {
            val delta = now.hits[kind.ordinal] - prev.hits[kind.ordinal]
            sb.append(' ').append(kind.name).append('=').append(delta).append('(').append(fmt(delta / dtS, 1)).append("/dtk)")
        }
        return sb.toString()
    }

    private fun describeTotals(): String {
        val parts = ArrayList<String>(2)
        totalLabel("nyala", totalOn)?.let { parts += it }
        totalLabel("mati", totalOff)?.let { parts += it }
        return if (parts.isEmpty()) "" else " | kum(sejak proses mulai, tanpa charger): " + parts.joinToString("; ")
    }

    private fun totalLabel(name: String, total: Total): String? {
        if (total.seconds < MIN_CUMULATIVE_S) return null
        return "$name ${fmtDur((total.seconds * MS_PER_S).toLong())} ~${fmt(total.uah * UAH_TO_MA / total.seconds, 0)}mA " +
            "cpu=${fmt(total.cpuMs / MS_PER_S * PERCENT / total.seconds, 2)}%"
    }

    private fun importanceLabel(importance: Int): String = when {
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FG"
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> "FGS"
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "VIS"
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "PERC"
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "SVC"
        else -> "CACHED"
    }

    private fun yn(value: Boolean?): String = when (value) {
        true -> "ya"
        false -> "tidak"
        null -> "?"
    }

    private fun fmt(value: Double, digits: Int): String = String.format(Locale.ROOT, "%.${digits}f", value)

    private fun fmtDur(ms: Long): String {
        val totalS = ms / MS_PER_S.toLong()
        val h = totalS / SECONDS_PER_HOUR
        val m = totalS % SECONDS_PER_HOUR / SECONDS_PER_MIN
        val s = totalS % SECONDS_PER_MIN
        return if (h > 0L) "${h}j${m}m" else "${m}m${s}d"
    }
}
