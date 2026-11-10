package com.rudi.audioplayer.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock

/**
 * Batch 627 — keluaran `dumpsys` untuk "Ekspor Logcat Debug" (log jadi jauh lebih lengkap). Permintaan user:
 * "tambahkan tab izin dumpsys sekaligus pengecekan centang biar debug log panjang".
 *
 * `dumpsys` hanya boleh dibaca pemegang izin `android.permission.DUMP` (protection: signature|privileged|development),
 * yang TIDAK bisa diminta lewat dialog. Dua jalur akses ([Access]): (1) DIRECT = izin DUMP sudah diberikan ke aplikasi
 * ini (`adb shell pm grant <paket> android.permission.DUMP`, atau tombol "Beri izin DUMP via Shizuku"); (2) SHIZUKU
 * (Batch 628) = Shizuku berjalan + diizinkan: `dumpsys` dijalankan di proses shell Shizuku tanpa memberi izin apa pun.
 * Tanpa keduanya, ekspor tetap jalan seperti biasa dan hanya mencatat status akses. Setiap bagian punya centang (default semua ON, disimpan di prefs `debug_dumpsys`).
 *
 * Aman: tiap `dumpsys` punya batas waktu, seluruh pengumpulan punya anggaran waktu total, keluaran dibatasi, dan
 * tidak ada fungsi di sini yang melempar. Blocking (proses + I/O): panggil dari `Dispatchers.IO`. Hanya android.* +
 * JDK (aturan ArchUnit "util mandiri").
 */
object DumpsysCollector {

    const val PERMISSION = "android.permission.DUMP"

    private const val PREFS = "debug_dumpsys"
    private const val PER_SECTION_TIMEOUT_MS = 6_000L
    private const val TOTAL_BUDGET_MS = 40_000L
    private const val RAW_LINE_LIMIT = 30_000
    private const val GRANT_TIMEOUT_MS = 8_000L
    private const val GRANT_LINE_LIMIT = 50

    /** Satu bagian dumpsys. [needle] != null = hanya baris yang memuat teks itu (+/- [contextLines]) yang disimpan
     *  (layanan sistem mencetak SEMUA aplikasi; yang relevan hanya milik aplikasi ini). */
    data class Section(
        val id: String,
        val label: String,
        val description: String,
        val args: List<String>,
        val needle: String? = null,
        val contextLines: Int = 8,
        val maxLines: Int = 400
    )

    /** Hasil uji: [ok] true = dumpsys benar-benar berjalan dan tidak ditolak sistem. */
    data class TestResult(val ok: Boolean, val detail: String)

    /** Cara dumpsys dijalankan: [DIRECT] izin DUMP milik aplikasi, [SHIZUKU] proses shell Shizuku, [NONE] tak ada akses. */
    enum class Access(val label: String) {
        DIRECT("izin DUMP langsung"),
        SHIZUKU("lewat Shizuku"),
        NONE("TIDAK ADA")
    }

    /** Daftar bagian (id stabil: dipakai sebagai kunci prefs). Dipilih sesuai kebutuhan debug proyek ini: kill/swipe,
     *  Recents, sesi media, fokus/efek audio, notifikasi, widget, doze, memori, getar. */
    fun sections(packageName: String): List<Section> = listOf(
        Section("package", "Paket & izin", "Izin diberikan, status instalasi, versi", listOf("package", packageName), maxLines = 500),
        Section("services", "Layanan aplikasi", "Status foreground service pemutaran", listOf("activity", "services", packageName), maxLines = 200),
        Section("recents", "Recents", "Isi daftar Recents (bukti RecentsGuard)", listOf("activity", "recents"), packageName, 10, 250),
        Section("processes", "Proses", "Status/prioritas proses aplikasi (bahan analisis kill)", listOf("activity", "processes"), packageName, 6, 250),
        Section("media_session", "Sesi media", "Sesi pemutar & status yang dilihat sistem", listOf("media_session"), packageName, 10, 250),
        Section("audio", "Audio", "Fokus audio, pemutar aktif, efek", listOf("audio"), maxLines = 600),
        Section("notification", "Notifikasi", "Notifikasi pemutar & kanalnya", listOf("notification"), packageName, 6, 300),
        Section("appwidget", "Widget", "Widget beranda terpasang", listOf("appwidget"), packageName, 6, 150),
        Section("power", "Daya", "Wake lock milik aplikasi", listOf("power"), packageName, 2, 120),
        Section("deviceidle", "Doze", "Pengecualian hemat baterai", listOf("deviceidle"), packageName, 1, 80),
        Section("meminfo", "Memori", "Pemakaian memori aplikasi", listOf("meminfo", packageName), maxLines = 120),
        Section("vibrator", "Getar", "Kemampuan & riwayat getar (bahan haptic)", listOf("vibrator_manager"), maxLines = 200),
        Section("jobscheduler", "Job terjadwal", "Tugas WorkManager aplikasi", listOf("jobscheduler", packageName), maxLines = 150)
    )

    fun isPermissionGranted(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** Perintah adb pemberi izin (disalin pengguna dari tab Izin). */
    fun grantCommand(context: Context): String = "adb shell pm grant ${context.packageName} $PERMISSION"

    /** Akses efektif saat ini (izin langsung diutamakan, lalu Shizuku). Blocking ringan: panggil dari IO. */
    fun accessMode(context: Context): Access = when {
        isPermissionGranted(context) -> Access.DIRECT
        ShizukuBridge.state(context) == ShizukuBridge.State.READY -> Access.SHIZUKU
        else -> Access.NONE
    }

    /** Beri izin DUMP ke aplikasi ini lewat shell Shizuku (`pm grant`); [TestResult.ok] true bila izin kini terpasang. */
    fun grantViaShizuku(context: Context): TestResult {
        if (ShizukuBridge.state(context) != ShizukuBridge.State.READY) return TestResult(false, "Shizuku belum siap")
        val run = ShizukuBridge.runShell("pm grant ${context.packageName} $PERMISSION 2>&1", GRANT_TIMEOUT_MS, GRANT_LINE_LIMIT)
        if (isPermissionGranted(context)) return TestResult(true, "izin DUMP diberikan lewat Shizuku")
        val reason = run.error ?: run.lines.firstOrNull().orEmpty().ifEmpty { "tanpa keluaran" }
        return TestResult(false, "gagal memberi izin: ${reason.take(160)}")
    }

    fun isSelected(context: Context, id: String): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("sel_$id", true)

    fun setSelected(context: Context, id: String, selected: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("sel_$id", selected).apply()
    }

    /** Uji nyata: jalankan `dumpsys meminfo <paket>`; ok bila ada keluaran dan tidak ditolak sistem. */
    fun testDumpsys(context: Context): TestResult {
        val access = accessMode(context)
        if (access == Access.NONE) return TestResult(false, "belum ada akses: izin DUMP belum diberikan dan Shizuku belum siap")
        val run = runDumpsys(listOf("meminfo", context.packageName), PER_SECTION_TIMEOUT_MS, access)
        return when {
            run.error != null -> TestResult(false, "gagal dijalankan: ${run.error}")
            run.lines.isEmpty() -> TestResult(false, if (run.timedOut) "melewati batas waktu" else "tanpa keluaran")
            DumpsysFormat.isDenied(run.lines) -> TestResult(false, "ditolak sistem: ${run.lines.first().take(120)}")
            else -> TestResult(true, "berjalan ${access.label} (${run.lines.size} baris)")
        }
    }

    /** Bagian laporan untuk ekspor: satu bagian status + (bila izin ada) satu bagian per centang yang aktif. Tak melempar. */
    fun collectSections(context: Context): List<String> {
        val app = context.applicationContext
        val access = accessMode(app)
        val all = sections(app.packageName)
        val chosen = all.filter { isSelected(app, it.id) }
        val out = ArrayList<String>()
        out.add(DumpsysFormat.statusSection(access.label, access != Access.NONE, grantCommand(app), chosen.map { it.label }, all.size))
        if (access == Access.NONE) return out
        val deadline = SystemClock.elapsedRealtime() + TOTAL_BUDGET_MS
        for (section in chosen) {
            if (SystemClock.elapsedRealtime() >= deadline) {
                out.add(DumpsysFormat.buildSection(section.args, emptyList(), null, 0, 0, false, "dilewati: anggaran waktu total habis"))
                continue
            }
            out.add(
                runCatching {
                    val run = runDumpsys(section.args, PER_SECTION_TIMEOUT_MS, access)
                    DumpsysFormat.buildSection(section.args, run.lines, section.needle, section.contextLines, section.maxLines, run.timedOut, run.error)
                }.getOrElse { error ->
                    DumpsysFormat.buildSection(section.args, emptyList(), null, 0, 0, false, "${error.javaClass.simpleName}: ${error.message.orEmpty()}")
                }
            )
        }
        return out
    }

    /** Jalankan `dumpsys <args>` lewat [access] (DIRECT = ProcessBuilder; SHIZUKU = shell Shizuku) dengan batas waktu. */
    private fun runDumpsys(args: List<String>, timeoutMs: Long, access: Access): ShellResult {
        if (access == Access.SHIZUKU) {
            return ShizukuBridge.runShell((listOf("dumpsys") + args).joinToString(" ") + " 2>&1", timeoutMs, RAW_LINE_LIMIT)
        }
        val process = runCatching { ProcessBuilder(listOf("dumpsys") + args).redirectErrorStream(true).start() }
            .getOrElse { return ShellResult(emptyList(), false, "${it.javaClass.simpleName}: ${it.message.orEmpty()}") }
        return drainProcess(process, timeoutMs, RAW_LINE_LIMIT)
    }
}

/**
 * Bagian MURNI (tanpa Android) dari [DumpsysCollector]: penyaring baris sekitar kata kunci, pemotong, deteksi
 * "ditolak", dan pembangun bagian laporan — supaya bisa dites di JVM. `internal` (pola sama `DebugLogFormat`).
 */
internal object DumpsysFormat {

    const val GAP_MARK = "..."

    /** Hanya baris yang memuat [needle] ditambah [context] baris di kiri-kanannya; kelompok yang tak bersambung
     *  dipisah satu baris [GAP_MARK]. [needle] kosong = semua baris apa adanya. */
    fun filterAround(lines: List<String>, needle: String, context: Int): List<String> {
        if (needle.isEmpty()) return lines
        val keep = BooleanArray(lines.size)
        for (i in lines.indices) {
            if (lines[i].contains(needle)) {
                val from = maxOf(0, i - context)
                val to = minOf(lines.size - 1, i + context)
                for (j in from..to) keep[j] = true
            }
        }
        val out = ArrayList<String>()
        var gapPending = false
        for (i in lines.indices) {
            if (keep[i]) {
                if (gapPending && out.isNotEmpty()) out.add(GAP_MARK)
                out.add(lines[i])
                gapPending = false
            } else {
                gapPending = true
            }
        }
        return out
    }

    /** Potong ke [maxLines] baris pertama; sisa dicatat satu baris keterangan. */
    fun capLines(lines: List<String>, maxLines: Int): List<String> =
        if (lines.size <= maxLines) lines else lines.take(maxLines) + "(... ${lines.size - maxLines} baris lain dipotong)"

    /** Layanan menolak pembaca tanpa izin DUMP dengan pesan `Permission Denial` / `SecurityException` di awal keluaran. */
    fun isDenied(lines: List<String>): Boolean =
        lines.take(5).any { it.contains("Permission Denial", ignoreCase = true) || it.contains("SecurityException", ignoreCase = true) }

    /** Satu bagian laporan. [error] != null = gagal sebelum/saat membaca (alasan dicetak). */
    fun buildSection(
        args: List<String>,
        rawLines: List<String>,
        needle: String?,
        contextLines: Int,
        maxLines: Int,
        timedOut: Boolean,
        error: String?
    ): String = buildString {
        appendLine("---- dumpsys ${args.joinToString(" ")} ----")
        when {
            error != null -> appendLine("($error)")
            rawLines.isEmpty() -> appendLine(if (timedOut) "(melewati batas waktu, tanpa keluaran)" else "(kosong)")
            isDenied(rawLines) -> {
                appendLine("(ditolak sistem)")
                rawLines.take(3).forEach { appendLine(it) }
            }
            else -> {
                val filtered = if (needle != null) filterAround(rawLines, needle, contextLines) else rawLines
                if (needle != null && filtered.isEmpty()) {
                    appendLine("(tak ada baris yang memuat \"$needle\" dari ${rawLines.size} baris keluaran)")
                } else {
                    if (needle != null) appendLine("(disaring: baris memuat \"$needle\" +/- $contextLines dari ${rawLines.size} baris)")
                    capLines(filtered, maxLines).forEach { appendLine(it) }
                    if (timedOut) appendLine("(keluaran terpotong: melewati batas waktu)")
                }
            }
        }
    }

    /** Bagian pembuka: status akses + daftar bagian yang dicentang. Selalu ada, dengan atau tanpa izin. */
    fun statusSection(accessLabel: String, hasAccess: Boolean, grantCommand: String, chosenLabels: List<String>, totalSections: Int): String = buildString {
        appendLine("---- dumpsys (status) ----")
        appendLine("akses dumpsys: $accessLabel")
        if (!hasAccess) appendLine("(aktifkan Shizuku lalu izinkan aplikasi di tab Izin dumpsys, atau beri izin sekali lewat: $grantCommand)")
        val chosen = if (chosenLabels.isEmpty()) "(tak ada yang dicentang)" else chosenLabels.joinToString(", ")
        appendLine("bagian dicentang (${chosenLabels.size} dari $totalSections): $chosen")
    }
}
