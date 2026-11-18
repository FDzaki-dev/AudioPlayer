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
     * Batch 683: 200 -> 150. Frame pertama normal terukur 123-126 ms (v677 x4 + v682 pid 23208; `frame1_predraw` ->
     * `frame1_selesai`); frame pertama yang kena `Skipped frames` 158 ms (v681) dan 208 ms (v681 pid 31012). Ambang 200 akan
     * membisukan kasus 158 ms — padahal pesan peluncuran Activity TIDAK bisa tercatat `Looper` (pemasangan printer di tengah
     * pesan itu), jadi frame pertama adalah satu-satunya pemicu yang tersisa pada jendela Activity.
     */
    const val FRAME_TRIGGER_MS = 150L

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
