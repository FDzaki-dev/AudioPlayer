package com.rudi.audioplayer.util

/**
 * Batch 682 — bagian MURNI (tanpa API Android, bisa diuji di JVM) dari [StartupStallProbe].
 *
 * Dasar (log v679 + v681, `Choreographer: Skipped 31 frames!` di proses pasca-instal yang lahir tanpa UI lalu Activity dibuka
 * 1,9 dtk kemudian; 0 kejadian di 4 start dingin force-stop v677 dan di proses v680): penyebab thread utama macet di frame
 * pertama BELUM diketahui (log hanya memberi jumlah frame, bukan siapa yang menahan). Probe mencatat (a) tiap pesan thread utama
 * yang lama (teks mentah dari `Looper`: Handler + callback + what) dan (b) rincian fase tiap frame lambat (`FrameMetrics`),
 * selama [WINDOW_MS].
 *
 * Batch 683 — jendela TIDAK LAGI hanya 8 dtk dari proses mulai. Log v682 (pid 23208): Activity baru dibuat 8439 ms setelah proses
 * lahir (proses pasca-instal hidup tanpa UI, user mengetuk "Buka" belakangan) = SETELAH jendela 8 dtk habis -> probe sudah
 * dilepas, 0 baris `StartupStall`. Pada pid 31012 (v681) Activity dibuka 3,4 JAM setelah proses lahir dan tetap kena
 * `Skipped 30 + 31 frames!` — jadi stall frame pertama TIDAK khas pasca-instal dan jendela berbasis waktu-proses tak pernah
 * menjangkaunya. Kini ada dua jendela: (1) [KIND_PROCESS] = [WINDOW_MS] sejak proses mulai (kerja proses/widget sebelum UI),
 * (2) [KIND_ACTIVITY] = [WINDOW_MS] sejak Activity PERTAMA proses dibuat, berapa pun umur prosesnya.
 *
 * Batch 684 — log v683 (CI run #110 hijau) membuktikan probe Batch 683 jalan (`jenis=activity_pertama` pada proses yang hidup 33 dtk
 * tanpa UI) dan menunjukkan DUA cacat probe sendiri: (1) baris frame terduplikasi 2-3x (platform menyerahkan metrik frame yang
 * sama berulang; first-frame 3x) -> kini dibuang lewat [RecentKeys] berkunci `INTENDED_VSYNC_TIMESTAMP`; (2) ambang frame 150 ms
 * (Batch 683) salah kalibrasi: total `FrameMetrics` frame pertama NORMAL = 155 ms (start dingin murni) / 175 ms (pasca-instal),
 * bukan 123-126 ms (itu selang `frame1_predraw` -> `frame1_selesai` di `StartupTrace`) -> kembali 200 ms. Jank nyata yang
 * terukur: satu frame `gambar`=168-182 ms (total 232-242 ms; pesan `Choreographer$FrameDisplayEventReceiver` 206-208 ms) ±1,9-2,1 dtk
 * SETELAH Activity dibuat, di kedua proses v682/v683; probe baru tahu "frame mana", bukan "kode mana" -> ditambah sampler
 * tumpukan thread utama ([StackSampleCounter]) yang menjawab "kode mana".
 *
 * Batch 687 — log v686 (probe Batch 685 jalan): pesan doFrame 225 ms (t=+4434..+4659, ±1,9 dtk setelah Activity) berisi 13 sampel aktif
 * yang tersebar di >= 9 lokasi (Compose `b1.w1.i < p1.e.c < ...`, `Canvas.drawPath <- c2.a.b`, `TextLine.getRunAdvance`) = kerja Compose
 * yang MELEBAR (komposisi + ukur teks + gambar path), bukan satu titik panas; dan 4 frame teratas semuanya nama R8 pendek, jadi tak
 * menunjuk kode app. Perbaikan: R8 tak mengacak nama + sampel dikelompokkan menurut frame kode app terdekat ([isAppClass], dipakai sampler).
 *
 * Agar Log Diagnostik (batas 200 KB) tidak penuh lagi: entri DITAHAN di memori dan baru ditulis bila ada satu stall besar
 * (pesan >= [MESSAGE_TRIGGER_MS] atau frame >= [FRAME_TRIGGER_MS]); start normal = 0 baris.
 */
internal object StartupStallFormat {
    /** Lama satu jendela pemantauan (ms): dihitung dari proses mulai ([KIND_PROCESS]) atau dari Activity pertama ([KIND_ACTIVITY]). */
    const val WINDOW_MS = 8_000L

    /** Jendela dibuka saat `Application.onCreate` dan Activity belum dibuat selama jendela itu berjalan. */
    const val KIND_PROCESS = "proses_mulai"

    /** Activity pertama dibuat SAAT jendela proses masih terbuka: jendela diperpanjang [WINDOW_MS] sejak Activity itu. */
    const val KIND_PROCESS_ACTIVITY = "proses_mulai+activity"

    /** Activity pertama dibuat SETELAH jendela proses habis (proses pasca-instal/hidup lama tanpa UI): jendela baru dibuka. */
    const val KIND_ACTIVITY = "activity_pertama"

    /** Pesan/frame di bawah ini tak dicatat sama sekali. */
    const val MIN_LOG_MS = 60L

    /** Pesan thread utama selama ini (atau lebih) = stall besar -> seluruh entri jendela ditulis ke log. */
    const val MESSAGE_TRIGGER_MS = 150L

    /**
     * Total durasi frame selama ini (atau lebih) = stall besar -> seluruh entri jendela ditulis ke log.
     * Batch 684: 150 -> 200 ms. Terukur di log v683 (`FrameMetrics.TOTAL_DURATION`): frame pertama NORMAL 155 ms (start dingin murni)
     * dan 175 ms (proses pasca-instal, Activity 33 dtk setelah proses lahir) = tak boleh memicu; frame berat 208-242 ms memicu.
     * (Batch 683 memakai 150 dari selang `StartupTrace` 123-126 ms yang BUKAN ukuran yang sama dengan `TOTAL_DURATION`.)
     */
    const val FRAME_TRIGGER_MS = 200L

    /** Selang pengambilan sampel tumpukan thread utama selama jendela terbuka (ms). */
    const val SAMPLE_INTERVAL_MS = 15L

    /** Jumlah frame teratas tumpukan yang dipakai sebagai kunci "lokasi" sampel. */
    const val SITE_DEPTH = 4

    /** Batas jumlah lokasi berbeda yang ditahan di memori per jendela (lokasi baru di atas batas diabaikan). */
    const val MAX_SITES = 200

    /** Jumlah baris sampel (lokasi terbanyak) yang ditulis per jendela bila ada stall besar. */
    const val MAX_SAMPLE_LINES = 5

    /** Pesan thread utama selama ini (atau lebih) dikaitkan ke sampel tumpukan pada rentang waktunya sendiri. */
    const val ATTRIBUTE_MS = 100L

    /** Jumlah pesan lambat terpanjang yang diberi baris atribusi per jendela. */
    const val MAX_ATTRIBUTIONS = 5

    /** Jumlah lokasi teratas per pesan lambat pada baris atribusi. */
    const val ATTRIBUTION_SITES = 3

    /**
     * Batch 687 — awalan paket kode app. Log v686: sampel selama pesan doFrame 225 ms (13 sampel aktif) tersebar di >= 9 lokasi
     * berbeda dan 4 frame teratasnya semuanya internal Compose/platform berbentuk nama R8 pendek (`b1.w1.i`, `c2.a.b`) -> tak
     * satu pun menunjuk kode app. Kini tiap sampel juga mencatat frame kode app TERDEKAT dari puncak tumpukan.
     */
    const val APP_PACKAGE_PREFIX = "com.rudi.audioplayer."

    /** Kelas probe sendiri dikecualikan dari "kode app" (thread utama sesaat berada di `MessagePrinter.println`). */
    private const val PROBE_CLASS_PREFIX = "com.rudi.audioplayer.util.StartupStall"

    /** Jumlah frame dari puncak tumpukan yang dipindai mencari frame kode app (tumpukan Compose bisa > 100 frame). */
    const val APP_SCAN_DEPTH = 150

    /** Jumlah baris "kode app" teratas per pesan lambat (ditulis SEBELUM baris lokasi pesan yang sama). */
    const val ATTRIBUTION_APP_SITES = 3

    /** Pengganti frame kode app bila tumpukan sampel tak memuat satu pun frame app (murni Compose/platform). */
    const val NO_APP_FRAME = "(tanpa frame kode app)"

    /** Pemisah antara 4 frame teratas dan frame kode app pada kunci lokasi. */
    private const val APP_FRAME_SEPARATOR = " << kode app: "

    /** Batas sampel mentah (waktu + lokasi) yang ditahan per jendela (+-22 dtk pada selang 15 ms). */
    const val MAX_RAW_SAMPLES = 1_500

    private const val IDLE_TOP_FRAME = "android.os.MessageQueue.nativePollOnce"

    /** Batas jumlah baris biasa yang disimpan per jendela (baris stall besar selalu disimpan). */
    const val MAX_LINES = 12

    /** Awal teks `Looper.setMessageLogging` (platform): ">>>>> Dispatching to <target> <callback>: <what>". */
    const val DISPATCH_PREFIX = ">>>>> Dispatching to "

    /** Awal teks penutup pesan: "<<<<< Finished to <target> <callback>". */
    const val FINISH_PREFIX = "<<<<< Finished to "

    private const val MAX_TEXT_CHARS = 200
    private const val NS_PER_MS = 1_000_000L

    fun nsToMs(ns: Long): Long = ns / NS_PER_MS

    /**
     * Sisa jendela proses (ms) pada waktu [nowMs]; semuanya `uptimeMillis`. <= 0 = jendela proses sudah lewat (proses hidup
     * tanpa UI lebih lama dari [WINDOW_MS]) -> probe menunggu Activity pertama lalu membuka jendela [KIND_ACTIVITY].
     */
    fun processWindowRemainingMs(processStartMs: Long, nowMs: Long): Long = processStartMs + WINDOW_MS - nowMs

    /** Baris pembuka tiap jendela yang ditulis: jenis, saat dibuka, dan saat Activity pertama dibuat (null = belum ada). */
    fun windowLine(kind: String, openedAtMs: Long, activityAtMs: Long?): String =
        "Jendela probe: jenis=$kind dibuka t=+${openedAtMs}ms sejak proses mulai, Activity pertama " +
            (if (activityAtMs == null) "belum dibuat" else "dibuat t=+${activityAtMs}ms")

    /**
     * Kunci "lokasi" satu sampel tumpukan thread utama: [SITE_DEPTH] frame teratas (teks `StackTraceElement` mentah) digabung
     * " < ". Null bila thread utama menganggur (`nativePollOnce`) atau tumpukan kosong = sampel dibuang.
     */
    fun siteOf(frames: List<String>, appFrame: String? = null): String? {
        val top = frames.firstOrNull() ?: return null
        if (top.startsWith(IDLE_TOP_FRAME)) return null
        val head = frames.take(SITE_DEPTH)
        val base = head.joinToString(" < ")
        // Batch 687: frame kode app terdekat ditambahkan HANYA bila belum termasuk 4 frame teratas (tanpa duplikat).
        return if (appFrame == null || appFrame in head) base else base + APP_FRAME_SEPARATOR + appFrame
    }

    /** True bila [className] milik kode app ([APP_PACKAGE_PREFIX]) dan bukan kelas probe ini sendiri. */
    fun isAppClass(className: String): Boolean =
        className.startsWith(APP_PACKAGE_PREFIX) && !className.startsWith(PROBE_CLASS_PREFIX)

    /** Baris atribusi: sampel tumpukan yang jatuh DI DALAM rentang satu pesan lambat ([fromMs]..[toMs]); [active] = sampel tak-idle di rentang itu. */
    fun intervalSampleLine(fromMs: Long, toMs: Long, active: Int, count: Int, site: String): String =
        "Sampel selama pesan lambat t=+${fromMs}..+${toMs}ms ($active sampel aktif): ${count}x | $site"

    /** Baris atribusi per frame KODE APP: sama dengan [intervalSampleLine] tetapi dikelompokkan menurut frame app terdekat. */
    fun appIntervalLine(fromMs: Long, toMs: Long, active: Int, count: Int, appFrame: String): String =
        "Kode app selama pesan lambat t=+${fromMs}..+${toMs}ms ($active sampel aktif): ${count}x | $appFrame"

    /** Baris untuk satu lokasi sampel: jumlah sampel, rentang waktu (ms sejak proses mulai), dan tumpukan mentah. */
    fun sampleLine(count: Int, firstMs: Long, lastMs: Long, site: String): String =
        "Sampel tumpukan thread utama: ${count}x (tiap ~${SAMPLE_INTERVAL_MS}ms; t=+${firstMs}..+${lastMs}ms sejak proses mulai) | $site"

    /** Baris untuk satu pesan thread utama yang lama. [atMs] = saat pesan SELESAI, ms sejak proses mulai. */
    fun messageLine(atMs: Long, durationMs: Long, dispatchText: String): String {
        val raw = dispatchText.removePrefix(DISPATCH_PREFIX).trim()
        val text = if (raw.length > MAX_TEXT_CHARS) raw.take(MAX_TEXT_CHARS) + "..." else raw
        return "Pesan thread utama lambat: ${durationMs}ms (selesai t=+${atMs}ms sejak proses mulai) | $text"
    }

    /** Baris untuk satu frame lambat. [atMs] = saat metrik frame diterima, ms sejak proses mulai. */
    fun frameLine(atMs: Long, sample: FrameSample): String =
        "Frame lambat: total=${nsToMs(sample.totalNs)}ms (t=+${atMs}ms sejak proses mulai) " +
            "frame_pertama=${if (sample.firstDraw) "ya" else "tidak"} drop=${sample.dropCount} | " +
            "tunda_vsync=${nsToMs(sample.vsyncDelayNs)}ms input=${nsToMs(sample.inputNs)}ms " +
            "animasi=${nsToMs(sample.animationNs)}ms layout=${nsToMs(sample.layoutNs)}ms " +
            "gambar=${nsToMs(sample.drawNs)}ms sinkron=${nsToMs(sample.syncNs)}ms " +
            "perintah=${nsToMs(sample.issueNs)}ms swap=${nsToMs(sample.swapNs)}ms " +
            "tak_terhitung=${nsToMs(sample.unknownNs)}ms"
}

/** Nilai mentah satu frame dari `FrameMetrics` (nanodetik), dipisah dari API Android supaya format bisa diuji di JVM. */
internal data class FrameSample(
    val totalNs: Long,
    val vsyncDelayNs: Long,
    val inputNs: Long,
    val animationNs: Long,
    val layoutNs: Long,
    val drawNs: Long,
    val syncNs: Long,
    val issueNs: Long,
    val swapNs: Long,
    val unknownNs: Long,
    val firstDraw: Boolean,
    val dropCount: Int
)

/**
 * Menahan baris stall di memori. [add] dipanggil dari thread utama (pesan) dan thread callback frame; [drain] dari thread utama
 * di akhir jendela — karena itu semuanya `@Synchronized`.
 */
internal class StallCollector {
    private val lines = ArrayList<String>()
    private var triggered = false

    /**
     * Simpan [line] bila [durationMs] >= [StartupStallFormat.MIN_LOG_MS] dan batas baris belum penuh; tandai "stall besar"
     * bila [durationMs] >= [triggerMs]. Satu pengecualian: baris stall besar SELALU disimpan walau batas baris sudah penuh
     * (justru itu baris yang dicari; jumlahnya dibatasi oleh jendela [StartupStallFormat.WINDOW_MS]).
     */
    @Synchronized
    fun add(durationMs: Long, triggerMs: Long, line: String) {
        val big = durationMs >= triggerMs
        if (durationMs >= StartupStallFormat.MIN_LOG_MS && (lines.size < StartupStallFormat.MAX_LINES || big)) lines.add(line)
        if (big) triggered = true
    }

    /** Baris yang perlu ditulis ke log: seluruh entri bila ada stall besar, selain itu kosong. */
    @Synchronized
    fun drain(): List<String> = if (triggered) lines.toList() else emptyList()

    /** Kosongkan entri + pemicu. Dipanggil di akhir TIAP jendela supaya jendela berikutnya (Batch 683: ada dua) mulai bersih. */
    @Synchronized
    fun reset() {
        lines.clear()
        triggered = false
    }
}

/**
 * Pembuang duplikat: ingat [capacity] kunci terakhir. Platform menyerahkan metrik frame yang SAMA berulang (log v683: frame
 * pertama 3x, frame lain 2x), jadi kuncinya `FrameMetrics.INTENDED_VSYNC_TIMESTAMP` (unik per frame).
 */
internal class RecentKeys(private val capacity: Int) {
    private val keys = LongArray(capacity)
    private var used = 0
    private var next = 0

    /** True bila [key] belum ada di [capacity] kunci terakhir (lalu dicatat); false = duplikat, abaikan. */
    @Synchronized
    fun firstTime(key: Long): Boolean {
        for (i in 0 until used) if (keys[i] == key) return false
        keys[next] = key
        next = (next + 1) % capacity
        if (used < capacity) used++
        return true
    }
}

/**
 * Menghitung sampel tumpukan thread utama per "lokasi" ([StartupStallFormat.siteOf]). [add] dipanggil dari thread sampler,
 * [topLines] / [reset] dari thread utama di akhir jendela — karena itu `@Synchronized`.
 */
internal class StackSampleCounter {
    private class Site(var count: Int, val firstMs: Long, var lastMs: Long)

    private val sites = HashMap<String, Site>()
    private val rawTimes = LongArray(StartupStallFormat.MAX_RAW_SAMPLES)
    private val rawSites = arrayOfNulls<String>(StartupStallFormat.MAX_RAW_SAMPLES)
    private val rawApps = arrayOfNulls<String>(StartupStallFormat.MAX_RAW_SAMPLES)
    private var rawCount = 0

    /** [appFrame] (Batch 687) = frame kode app terdekat dari puncak tumpukan; null = tumpukan tanpa frame app. */
    @Synchronized
    fun add(site: String, atMs: Long, appFrame: String? = null) {
        if (rawCount < rawTimes.size) {
            rawTimes[rawCount] = atMs
            rawSites[rawCount] = site
            rawApps[rawCount] = appFrame
            rawCount++
        }
        val known = sites[site]
        if (known != null) {
            known.count++
            known.lastMs = atMs
        } else if (sites.size < StartupStallFormat.MAX_SITES) {
            sites[site] = Site(1, atMs, atMs)
        }
    }

    /** Lokasi terbanyak dulu; seri -> yang muncul lebih awal, lalu urutan teks (deterministik). */
    @Synchronized
    fun topLines(limit: Int): List<String> =
        sites.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Site>> { it.value.count }
                    .thenBy { it.value.firstMs }
                    .thenBy { it.key }
            )
            .take(limit)
            .map { StartupStallFormat.sampleLine(it.value.count, it.value.firstMs, it.value.lastMs, it.key) }

    /**
     * Lokasi terbanyak di antara sampel yang waktunya jatuh dalam [fromMs]..[toMs] (inklusif) — "kode mana" yang menahan thread
     * utama selama satu pesan lambat. Tanpa sampel di rentang itu -> satu baris dengan hitungan 0 supaya tetap terlihat.
     */
    @Synchronized
    fun topBetween(fromMs: Long, toMs: Long, limit: Int): List<String> {
        val counts = HashMap<String, Int>()
        var active = 0
        for (i in 0 until rawCount) {
            val t = rawTimes[i]
            val site = rawSites[i]
            if (site != null && t >= fromMs && t <= toMs) {
                counts[site] = (counts[site] ?: 0) + 1
                active++
            }
        }
        if (active == 0) return listOf(StartupStallFormat.intervalSampleLine(fromMs, toMs, 0, 0, "(tidak ada sampel aktif)"))
        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { StartupStallFormat.intervalSampleLine(fromMs, toMs, active, it.value, it.key) }
    }

    /**
     * Batch 687 — seperti [topBetween] tetapi dikelompokkan menurut FRAME KODE APP terdekat (bukan 4 frame teratas): sampel yang
     * tersebar di banyak lokasi Compose/platform tetap menumpuk di satu fungsi app bila memang itu yang memanggilnya. Sampel
     * tanpa frame app dihitung sebagai [StartupStallFormat.NO_APP_FRAME]. Tanpa sampel di rentang itu -> daftar KOSONG
     * (barisnya sudah dilaporkan [topBetween]).
     */
    @Synchronized
    fun topAppBetween(fromMs: Long, toMs: Long, limit: Int): List<String> {
        val counts = HashMap<String, Int>()
        var active = 0
        for (i in 0 until rawCount) {
            val t = rawTimes[i]
            if (rawSites[i] != null && t >= fromMs && t <= toMs) {
                val key = rawApps[i] ?: StartupStallFormat.NO_APP_FRAME
                counts[key] = (counts[key] ?: 0) + 1
                active++
            }
        }
        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { StartupStallFormat.appIntervalLine(fromMs, toMs, active, it.value, it.key) }
    }

    @Synchronized
    fun reset() {
        sites.clear()
        rawCount = 0
    }
}

/** Rentang waktu (ms sejak proses mulai) pesan thread utama yang lambat; hanya disentuh thread utama. */
internal class SlowIntervals {
    class Interval(val fromMs: Long, val toMs: Long)

    private val list = ArrayList<Interval>()

    fun add(fromMs: Long, toMs: Long) {
        if (list.size < MAX_KEPT) list.add(Interval(fromMs, toMs))
    }

    /** [limit] rentang terpanjang, diurutkan menurut waktu (yang paling awal dulu). */
    fun longest(limit: Int): List<Interval> =
        list.sortedByDescending { it.toMs - it.fromMs }.take(limit).sortedBy { it.fromMs }

    fun reset() {
        list.clear()
    }

    private companion object {
        const val MAX_KEPT = 64
    }
}
