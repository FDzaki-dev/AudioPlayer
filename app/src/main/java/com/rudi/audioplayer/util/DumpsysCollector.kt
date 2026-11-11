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

    /** Batch 631 — Android 16: layanan `package`, `activity`, `notification`, `jobscheduler` menolak pembaca yang hanya punya
     *  DUMP ("Permission Denial ... due to missing android.permission.PACKAGE_USAGE_STATS", log v629: 6 dari 13 bagian). */
    const val PERMISSION_USAGE = "android.permission.PACKAGE_USAGE_STATS"

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

    fun isUsageGranted(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION_USAGE) == PackageManager.PERMISSION_GRANTED

    /** Perintah adb pemberi izin (disalin pengguna dari tab Izin): DUMP + PACKAGE_USAGE_STATS (Batch 631). */
    fun grantCommand(context: Context): String =
        "adb shell pm grant ${context.packageName} $PERMISSION && adb shell pm grant ${context.packageName} $PERMISSION_USAGE"

    /** Akses efektif saat ini (izin langsung diutamakan, lalu Shizuku). Blocking ringan: panggil dari IO. */
    fun accessMode(context: Context): Access = runCatching { accessModeUnsafe(context) }.getOrDefault(Access.NONE)

    private fun accessModeUnsafe(context: Context): Access = when {
        isPermissionGranted(context) -> Access.DIRECT
        ShizukuBridge.state(context) == ShizukuBridge.State.READY -> Access.SHIZUKU
        else -> Access.NONE
    }

    /**
     * Beri izin DUMP + PACKAGE_USAGE_STATS ke aplikasi ini lewat shell Shizuku (`pm grant`, Batch 631: izin kedua ikut).
     * [TestResult.ok] true bila DUMP kini terpasang; [TestResult.detail] memuat status kedua izin + keluaran `pm` apa adanya
     * (alasan gagal, bila ada). Bagian dumpsys yang masih ditolak tetap bisa dibaca lewat jalur cadangan Shizuku per bagian.
     */
    fun grantViaShizuku(context: Context): TestResult = runCatching { grantViaShizukuUnsafe(context) }
        .getOrElse { TestResult(false, "galat: ${it.javaClass.simpleName}: ${it.message.orEmpty().take(120)}") }

    private fun grantViaShizukuUnsafe(context: Context): TestResult {
        if (ShizukuBridge.state(context) != ShizukuBridge.State.READY) return TestResult(false, "Shizuku belum siap")
        val pkg = context.packageName
        val run = ShizukuBridge.runShell(
            "pm grant $pkg $PERMISSION 2>&1; pm grant $pkg $PERMISSION_USAGE 2>&1",
            GRANT_TIMEOUT_MS,
            GRANT_LINE_LIMIT
        )
        val dump = isPermissionGranted(context)
        val usage = isUsageGranted(context)
        if (dump && usage) return TestResult(true, "izin DUMP + PACKAGE_USAGE_STATS diberikan lewat Shizuku")
        val reason = run.error ?: run.lines.filter { it.isNotBlank() }.take(2).joinToString(" | ").ifEmpty { "tanpa keluaran" }
        return TestResult(dump, "DUMP=${yesNo(dump)}, PACKAGE_USAGE_STATS=${yesNo(usage)}; pm: ${reason.take(200)}")
    }

    private fun yesNo(value: Boolean): String = if (value) "ya" else "tidak"

    private fun isShizukuReady(context: Context): Boolean =
        runCatching { ShizukuBridge.state(context) == ShizukuBridge.State.READY }.getOrDefault(false)

    fun isSelected(context: Context, id: String): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("sel_$id", true)

    fun setSelected(context: Context, id: String, selected: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("sel_$id", selected).apply()
    }

    /**
     * Uji nyata: jalankan `dumpsys activity services <paket>` (layanan yang butuh DUMP + PACKAGE_USAGE_STATS di Android 16);
     * ok bila ada keluaran dan tidak ditolak / tak ditemukan. Batch 631 — SEBELUMNYA memakai `dumpsys meminfo <paket>` dan
     * menandai LULUS (centang hijau palsu) walau keluarannya hanya satu baris "Can't find service: meminfo". Bila jalur
     * langsung gagal dan Shizuku siap, diuji ulang lewat Shizuku (sama seperti ekspor).
     */
    fun testDumpsys(context: Context): TestResult = runCatching { testDumpsysUnsafe(context) }
        .getOrElse { TestResult(false, "galat: ${it.javaClass.simpleName}: ${it.message.orEmpty().take(120)}") }

    private fun testDumpsysUnsafe(context: Context): TestResult {
        val access = accessMode(context)
        if (access == Access.NONE) return TestResult(false, "belum ada akses: izin DUMP belum diberikan dan Shizuku belum siap")
        val shizukuReady = access == Access.SHIZUKU || isShizukuReady(context)
        val outcome = runWithFallback(listOf("activity", "services", context.packageName), PER_SECTION_TIMEOUT_MS, access, shizukuReady)
        val run = outcome.run
        return when {
            run.error != null -> TestResult(false, "gagal dijalankan: ${run.error}")
            run.lines.isEmpty() -> TestResult(false, if (run.timedOut) "melewati batas waktu" else "tanpa keluaran")
            DumpsysFormat.isFailure(run.lines) -> TestResult(false, "ditolak/tak ditemukan: ${run.lines.first().take(120)}")
            else -> TestResult(true, "berjalan ${outcome.route ?: access.label} (${run.lines.size} baris)")
        }
    }

    /** Bagian laporan untuk ekspor: satu bagian status + (bila izin ada) satu bagian per centang yang aktif. Tak melempar. */
    fun collectSections(context: Context): List<String> {
        val app = context.applicationContext
        val access = accessMode(app)
        val all = sections(app.packageName)
        val chosen = all.filter { isSelected(app, it.id) }
        val shizukuReady = access == Access.SHIZUKU || (access == Access.DIRECT && isShizukuReady(app))
        val out = ArrayList<String>()
        val permissionLine = "izin: DUMP=${yesNo(isPermissionGranted(app))}, PACKAGE_USAGE_STATS=${yesNo(isUsageGranted(app))}; " +
            "Shizuku=${if (shizukuReady) "siap" else "tidak siap"}"
        out.add(
            DumpsysFormat.statusSection(access.label, access != Access.NONE, grantCommand(app), chosen.map { it.label }, all.size, permissionLine)
        )
        if (access == Access.NONE) return out
        val deadline = SystemClock.elapsedRealtime() + TOTAL_BUDGET_MS
        for (section in chosen) {
            if (SystemClock.elapsedRealtime() >= deadline) {
                out.add(DumpsysFormat.buildSection(section.args, emptyList(), null, 0, 0, false, "dilewati: anggaran waktu total habis"))
                continue
            }
            out.add(
                runCatching {
                    val outcome = runWithFallback(section.args, PER_SECTION_TIMEOUT_MS, access, shizukuReady)
                    val run = outcome.run
                    DumpsysFormat.buildSection(
                        section.args, run.lines, section.needle, section.contextLines, section.maxLines, run.timedOut, run.error, outcome.route
                    )
                }.getOrElse { error ->
                    DumpsysFormat.buildSection(section.args, emptyList(), null, 0, 0, false, "${error.javaClass.simpleName}: ${error.message.orEmpty()}")
                }
            )
        }
        return out
    }

    private class Outcome(val run: ShellResult, val route: String?)

    /**
     * Batch 631 — jalankan lewat [access]; bila jalur DIRECT gagal (ditolak `Permission Denial` / `Can't find service`) dan
     * Shizuku siap, ulangi bagian itu lewat shell Shizuku (uid shell punya DUMP + PACKAGE_USAGE_STATS dan bisa melihat layanan
     * `meminfo`). [Outcome.route] != null = hasilnya dari jalur cadangan (dicatat di laporan). Gagal juga lewat Shizuku =
     * keluaran jalur pertama dipakai apa adanya.
     */
    private fun runWithFallback(args: List<String>, timeoutMs: Long, access: Access, shizukuReady: Boolean): Outcome {
        val first = runDumpsys(args, timeoutMs, access)
        val failed = first.error != null || DumpsysFormat.isFailure(first.lines)
        if (access != Access.DIRECT || !shizukuReady || !failed) return Outcome(first, null)
        val second = runDumpsys(args, timeoutMs, Access.SHIZUKU)
        val recovered = second.error == null && second.lines.isNotEmpty() && !DumpsysFormat.isFailure(second.lines)
        return if (recovered) Outcome(second, DumpsysFormat.ROUTE_SHIZUKU_FALLBACK) else Outcome(first, null)
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

    /** Penanda bagian yang dibaca lewat jalur cadangan (Batch 631). */
    const val ROUTE_SHIZUKU_FALLBACK = "lewat Shizuku (jalur izin langsung gagal)"

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

    /** Layanan menolak pembaca tanpa izin DUMP dengan pesan `Permission Denial` / `SecurityException` di awal keluaran.
     *  Batch 632 — juga `Security exception` BERSPASI (log v631: `dumpsys jobscheduler` = "Security exception: MATCH_ANY_USER
     *  flag requires INTERACT_ACROSS_USERS ..." lolos dari deteksi lama sehingga tak diulang lewat Shizuku). */
    fun isDenied(lines: List<String>): Boolean = lines.take(5).any {
        it.contains("Permission Denial", ignoreCase = true) ||
            it.contains("SecurityException", ignoreCase = true) ||
            it.contains("Security exception", ignoreCase = true)
    }

    /** Batch 631 — gagal dibaca: ditolak ([isDenied]) ATAU layanan tak terlihat uid ini (`Can't find service: meminfo`, log v629). */
    fun isFailure(lines: List<String>): Boolean =
        isDenied(lines) || lines.take(5).any { it.contains("Can't find service", ignoreCase = true) }

    /** Satu bagian laporan. [error] != null = gagal sebelum/saat membaca (alasan dicetak); [route] != null = jalur cadangan. */
    fun buildSection(
        args: List<String>,
        rawLines: List<String>,
        needle: String?,
        contextLines: Int,
        maxLines: Int,
        timedOut: Boolean,
        error: String?,
        route: String? = null
    ): String = buildString {
        appendLine("---- dumpsys ${args.joinToString(" ")} ----")
        if (route != null && error == null) appendLine("(jalur: $route)")
        when {
            error != null -> appendLine("($error)")
            rawLines.isEmpty() -> appendLine(if (timedOut) "(melewati batas waktu, tanpa keluaran)" else "(kosong)")
            isFailure(rawLines) -> {
                appendLine(if (isDenied(rawLines)) "(ditolak sistem)" else "(layanan tidak ditemukan)")
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
    fun statusSection(
        accessLabel: String,
        hasAccess: Boolean,
        grantCommand: String,
        chosenLabels: List<String>,
        totalSections: Int,
        detail: String? = null
    ): String = buildString {
        appendLine("---- dumpsys (status) ----")
        appendLine("akses dumpsys: $accessLabel")
        if (detail != null) appendLine(detail)
        if (!hasAccess) appendLine("(aktifkan Shizuku lalu izinkan aplikasi di tab Izin dumpsys, atau beri izin sekali lewat: $grantCommand)")
        val chosen = if (chosenLabels.isEmpty()) "(tak ada yang dicentang)" else chosenLabels.joinToString(", ")
        appendLine("bagian dicentang (${chosenLabels.size} dari $totalSections): $chosen")
    }
}
