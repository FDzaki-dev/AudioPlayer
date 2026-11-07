package com.rudi.audioplayer.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Batch 603 — bagian MURNI (tanpa Android) dari ekspor log debug [AppLogger.exportDebugLogToDocuments], supaya
 * bisa dites di JVM. Diadaptasi dari `DebugLogExporter`/`DebugLogFormat` proyek Boomly: penyaring derau logcat,
 * pembangun laporan (header + bagian + logcat terbaru), nama berkas, dan daftar efek audio perangkat.
 *
 * `internal` + tanpa dependensi lapisan app lain (aturan ArchUnit "util mandiri").
 */
internal object DebugLogFormat {

    /** Jumlah baris logcat BERMAKNA terakhir yang disimpan (derau dibuang SAMBIL membaca, jadi batas ini tidak
     *  dihabiskan banjir `AudioEffect command()`). Batas atas dijaga tes supaya berkas tetap mudah dikirim. */
    const val MAX_LINES = 10_000

    /** Batas baris log diagnostik aplikasi ([AppLogger.readLog], terbaru di atas) yang ikut dilampirkan. */
    const val DIAGNOSTIC_MAX_LINES = 1_500

    /** Awalan nama berkas; folder `debug/` sengaja dipisah dari `logs/` (log crash) agar pembaca crash tak keliru. */
    const val FILE_PREFIX = "sonix_debug_"

    private val FILE_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT)

    private const val TS = """^\d\d-\d\d \d\d:\d\d:\d\d\.\d{3} """

    /** Banjir capture Visualizer (`command() ... cmdCode = 65536` ~10x/dtk) dan balasannya `commandExecuted()`,
     *  plus gema baca-ulang `getParameter: param: X, param2: Y` (hanya nomor parameter, tanpa nilai = nol
     *  informasi). `setParameter():` SENGAJA tetap disimpan (bukti penulisan parameter efek). */
    private val AUDIO_EFFECT_FLOOD =
        Regex(TS + """[VDIWEF]/AudioEffect\(\s*\d+\): (command\(\)|commandExecuted\(\)|getParameter:)""")

    /** Derau jendela/UI sistem yang tak berguna untuk analisis audio (dan memuat koordinat sentuhan). */
    private val UI_NOISE = Regex(
        TS + """[VDIWEF]/(VRI\[[^\]]*\]|DecorView|InsetsController|ImeTracker|BLASTBufferQueue|""" +
            """os\.SingleLiceFactory|os\.LiceInfo|TranClassInfo|GraphicsEnvironment|WindowOnBackDispatcher)\("""
    )

    /** True untuk baris derau yang dibuang dari laporan; baris lain (efek, serah-terima, crash) selalu dipertahankan. */
    fun isNoise(line: String): Boolean = AUDIO_EFFECT_FLOOD.containsMatchIn(line) || UI_NOISE.containsMatchIn(line)

    /** `sonix_debug_yyyyMMdd_HHmmss.txt` — mudah diurutkan dan dikenali di File Manager. */
    fun fileName(time: LocalDateTime): String = "$FILE_PREFIX${time.format(FILE_STAMP)}.txt"

    /** Header + [sections] (bagian kecil: efek, log diagnostik) + baris logcat. Bila baris logcat melebihi
     *  [maxLines], yang DIPERTAHANKAN adalah yang PALING BARU. Logcat ditaruh PALING AKHIR karena paling besar. */
    fun buildReport(header: List<String>, sections: List<String>, logLines: List<String>, maxLines: Int): String {
        val kept = if (logLines.size > maxLines) logLines.takeLast(maxLines) else logLines
        return buildString {
            header.forEach { appendLine(it) }
            sections.forEach { append(it) }
            appendLine("---- logcat SONIX (${kept.size} baris terakhir) ----")
            if (kept.isEmpty()) {
                appendLine("(kosong: belum ada log, atau buffer logcat sudah tertimpa. Ulangi skenario lalu ekspor segera.)")
            } else {
                kept.forEach { appendLine(it) }
            }
        }
    }

    /** Log diagnostik aplikasi ([AppLogger.readLog] sudah terbaru-di-atas): ambil [maxLines] baris PERTAMA = entri
     *  terbaru. Teks apa adanya (tanpa tafsir). */
    fun buildDiagnosticSection(diagnosticLog: String, maxLines: Int): String = buildString {
        val all = if (diagnosticLog.isBlank()) emptyList() else diagnosticLog.trimEnd('\n').split('\n')
        val kept = all.take(maxLines)
        appendLine("---- log diagnostik aplikasi (AppLogger, terbaru di atas, ${kept.size} dari ${all.size} baris) ----")
        if (kept.isEmpty()) appendLine("(kosong)") else kept.forEach { appendLine(it) }
    }

    /** Satu efek dari `AudioEffect.queryEffects()` (nilai sudah jadi teks supaya bisa dites di JVM). */
    data class EffectInfo(val typeUuid: String, val uuid: String, val connectMode: String, val name: String, val implementor: String)

    const val ENVIRONMENTAL_REVERB = "Environmental Reverb"
    const val UNKNOWN_EFFECT_TYPE = "tipe tak dikenal"
    private const val MAX_EFFECT_LINES = 300

    /** UUID tipe efek publik AOSP (`AudioEffect.EFFECT_TYPE_*`) -> nama. Tipe lain (vendor, Spatializer, Haptic, dst.)
     *  TIDAK ditebak: dicetak sebagai [UNKNOWN_EFFECT_TYPE] bersama UUID mentahnya. */
    private val KNOWN_EFFECT_TYPES: Map<String, String> = mapOf(
        "0bed4300-ddd6-11db-8f34-0002a5d5c51b" to "Equalizer",
        "0634f220-ddd4-11db-a0fc-0002a5d5c51b" to "Bass Boost",
        "37cc2c00-dddd-11db-8577-0002a5d5c51b" to "Virtualizer",
        "47382d60-ddd8-11db-bf3a-0002a5d5c51b" to "Preset Reverb",
        "c2e5d5f0-94bd-4763-9cac-4e234d06839e" to ENVIRONMENTAL_REVERB,
        "fe3199be-aed0-413f-87bb-11260eb63cf1" to "Loudness Enhancer",
        "7261676f-6d75-7369-6364-28e2fd3ac39e" to "Dynamics Processing",
        "e46b26a0-dddd-11db-8afd-0002a5d5c51b" to "Visualizer",
        "7b491460-8d4d-11e0-bd61-0002a5d5c51b" to "Acoustic Echo Canceler",
        "0a8abfe0-654c-11e0-ba26-0002a5d5c51b" to "Automatic Gain Control",
        "58b4b260-8e06-11e0-aa8e-0002a5d5c51b" to "Noise Suppressor"
    )

    fun effectTypeLabel(typeUuid: String): String = KNOWN_EFFECT_TYPES[typeUuid.trim().lowercase(Locale.ROOT)] ?: UNKNOWN_EFFECT_TYPE

    /** Bagian laporan "efek audio tersedia". [effects] `null` = `queryEffects()` mengembalikan null atau gagal
     *  ([error] berisi alasan). */
    fun buildEffectsSection(effects: List<EffectInfo>?, error: String?): String = buildString {
        if (effects == null) {
            appendLine("---- efek audio tersedia (AudioEffect.queryEffects) ----")
            appendLine(if (error != null) "(gagal dibaca: $error)" else "(queryEffects mengembalikan null: device tak melaporkan daftar efek)")
            return@buildString
        }
        val rows = effects.map { it to effectTypeLabel(it.typeUuid) }.sortedWith(compareBy({ it.second }, { it.first.name }))
        appendLine("---- efek audio tersedia (AudioEffect.queryEffects, ${effects.size} efek) ----")
        val counts = rows.groupingBy { it.second }.eachCount()
        appendLine("ringkasan: " + if (counts.isEmpty()) "(kosong)" else counts.entries.joinToString(", ") { "${it.key} x${it.value}" })
        val envCount = counts[ENVIRONMENTAL_REVERB] ?: 0
        appendLine("$ENVIRONMENTAL_REVERB tersedia: " + if (envCount > 0) "ya ($envCount entri)" else "TIDAK (device tak melaporkan tipe ini)")
        rows.take(MAX_EFFECT_LINES).forEach { (e, label) ->
            appendLine("$label | ${e.name} | mode=${e.connectMode} | implementor=${e.implementor} | type=${e.typeUuid} | uuid=${e.uuid}")
        }
        if (rows.size > MAX_EFFECT_LINES) appendLine("(${rows.size - MAX_EFFECT_LINES} efek lain tak ditampilkan)")
    }
}
