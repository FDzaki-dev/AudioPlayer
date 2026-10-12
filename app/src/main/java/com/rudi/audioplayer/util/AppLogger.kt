package com.rudi.audioplayer.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Local-only diagnostic log. Every entry is written to a single file in this app's private
 * storage (never anywhere else) so a crash or a caught-but-swallowed error leaves a trace the
 * user can view and copy (Settings > Lanjutan > Log Diagnostik) instead of vanishing with
 * nothing to go on. Deliberately not a third-party crash SDK: this app has no INTERNET
 * permission and nothing it collects should ever need one — a remote crash reporter would
 * require adding that permission and would undercut the "everything stays on this device"
 * guarantee the app otherwise makes.
 *
 * Batch 22: a crash that happens before the app can ever reach Settings (e.g. on launch)
 * makes the private log above unreachable without root/ADB. On a fatal uncaught exception,
 * this also writes a standalone .txt into the public Documents/AudioPlayer/logs folder via
 * MediaStore (API 29+ only — no storage permission needed, scoped-storage apps can always
 * contribute new files to public collections) so it can be grabbed with any ordinary file
 * manager. Non-fatal errors (`AppLogger.e`) still only go to the private log — this is
 * specifically for the "app won't even open" case.
 */
object AppLogger {
    private const val LOG_FILE_NAME = "diagnostic_log.txt"

    // Once the file crosses this size, the oldest half of its lines is dropped — keeps the
    // log useful (recent-first context for whatever just went wrong) without growing forever.
    private const val MAX_LOG_BYTES = 200_000L

    // FIFO cap for the public Documents/AudioPlayer/logs folder — a crash loop must not fill
    // the user's Documents with an unbounded number of files.
    private const val MAX_CRASH_LOGS = 50
    private val CRASH_LOG_RELATIVE_PATH = "${Environment.DIRECTORY_DOCUMENTS}/AudioPlayer/logs"

    // Batch 586 — ApplicationExitInfo (API 30+; minSdk 31). Handler FATAL di atas HANYA melihat exception
    // Java; ANR, native crash, dan proses dimatikan sistem (memori/OEM) tidak melewatinya sehingga tak
    // meninggalkan jejak apa pun. Sistem sendiri mencatat alasan tiap proses mati — dibaca saat proses
    // berikutnya mulai. Riwayat bertahan antar proses; cap waktu terakhir yang sudah ditulis disimpan agar
    // tiap kematian dicatat sekali saja.
    private const val EXIT_PREFS_NAME = "exit_info_state"
    private const val KEY_LAST_EXIT_TS = "last_exit_ts"
    private const val EXIT_HISTORY_MAX = 10
    private const val EXIT_TRACE_MAX_CHARS = 8000
    private val ABNORMAL_EXIT_REASONS = setOf(
        ApplicationExitInfo.REASON_UNKNOWN,
        ApplicationExitInfo.REASON_SIGNALED,
        ApplicationExitInfo.REASON_LOW_MEMORY,
        ApplicationExitInfo.REASON_CRASH,
        ApplicationExitInfo.REASON_CRASH_NATIVE,
        ApplicationExitInfo.REASON_ANR,
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
        ApplicationExitInfo.REASON_DEPENDENCY_DIED,
        ApplicationExitInfo.REASON_OTHER
    )

    private var logFile: File? = null
    private var appContext: Context? = null
    private var previousHandler: Thread.UncaughtExceptionHandler? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
    private val fileStampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT)

    /** Call once from Application.onCreate(). Safe to call more than once (no-ops after the first). */
    @Synchronized
    fun init(context: Context) {
        if (logFile != null) return
        appContext = context.applicationContext
        logFile = File(context.applicationContext.filesDir, LOG_FILE_NAME)

        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                appendEntry("FATAL", "Uncaught di thread '${thread.name}': ${Log.getStackTraceString(throwable)}")
            }
            runCatching { writePublicCrashLog(thread, throwable) }
            // Always defer to whatever handler existed before (or terminate normally if there
            // was none) — this logger only ever observes a crash, never changes how it's handled.
            previousHandler?.uncaughtException(thread, throwable) ?: Runtime.getRuntime().exit(2)
        }

        logPreviousExits(context.applicationContext)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val detail = if (throwable != null) "$message: ${Log.getStackTraceString(throwable)}" else message
        appendEntry("ERROR", "[$tag] $detail")
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        appendEntry("WARN", "[$tag] $message")
    }

    /** Full current log contents, or an empty string if nothing has been recorded yet. */
    fun readLog(): String = logFile?.takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrDefault("") } ?: ""

    fun clearLog() {
        runCatching { logFile?.writeText("") }
    }

    // FIFO cap for exported log_*.txt files in the same public folder — mirrors the crash-log
    // retention policy so manual exports can't grow the Documents folder unbounded either.
    private const val MAX_EXPORT_LOGS = 20

    /** Repacks the current in-app diagnostic log into a standalone log_<timestamp>.txt file inside
     * the public Documents/AudioPlayer/logs folder (same folder crash reports use via MediaStore,
     * API 29+, no storage permission needed) so it can be pulled off with any file manager instead
     * of only living in the clipboard. Returns true on success, false if there's nothing to export
     * or the write failed. */
    fun exportLogToDocuments(context: Context): Boolean {
        val text = readLog()
        if (text.isBlank()) return false
        return runCatching {
            val fileName = "log_${fileStampFormat.format(Date())}_${UUID.randomUUID()}.txt"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, CRASH_LOG_RELATIVE_PATH)
            }
            val resolver = context.applicationContext.contentResolver
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values) ?: return false
            resolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            enforceExportLogRetention(context)
            true
        }.getOrDefault(false)
    }

    /** Keeps only the newest [MAX_EXPORT_LOGS] exported log_*.txt files — same FIFO idea as
     * [enforceCrashLogRetention], scoped to the "log_" prefix so it never touches crash_*.txt. */
    private fun enforceExportLogRetention(context: Context) {
        val resolver = context.applicationContext.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("$CRASH_LOG_RELATIVE_PATH/", "log_%.txt")
        val ids = mutableListOf<Long>()
        resolver.query(collection, projection, selection, selectionArgs, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) ids.add(cursor.getLong(idCol))
        }
        if (ids.size <= MAX_EXPORT_LOGS) return
        for (id in ids.drop(MAX_EXPORT_LOGS)) {
            runCatching { resolver.delete(ContentUris.withAppendedId(collection, id), null, null) }
        }
    }

    @Synchronized
    private fun appendEntry(level: String, message: String) {
        val file = logFile ?: return
        runCatching {
            file.appendText("${dateFormat.format(Date())} $level $message\n")
            if (file.length() > MAX_LOG_BYTES) trim(file)
        }
    }

    private fun trim(file: File) {
        runCatching {
            val lines = file.readLines()
            val kept = lines.takeLast(lines.size / 2)
            file.writeText(kept.joinToString("\n", postfix = "\n"))
        }
    }

    /** Batch 586 — menulis ke log diagnostik alasan kematian proses SEBELUMNYA yang belum pernah dicatat,
     * persis seperti yang dilaporkan sistem (`ApplicationExitInfo`): alasan, status, importance, pss/rss,
     * deskripsi mentah; untuk ANR ditambah awal berkas trace-nya (teks). Tanpa tafsir buatan. Dijalankan di
     * Dispatchers.IO (panggilan ke ActivityManager + baca trace), dibungkus `runCatching` supaya kegagalan
     * pencatat tak pernah menjatuhkan aplikasi. */
    private fun logPreviousExits(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                    ?: return@runCatching
                val prefs = context.getSharedPreferences(EXIT_PREFS_NAME, Context.MODE_PRIVATE)
                val lastTs = prefs.getLong(KEY_LAST_EXIT_TS, 0L)
                val fresh = manager.getHistoricalProcessExitReasons(context.packageName, 0, EXIT_HISTORY_MAX)
                    .filter { it.timestamp > lastTs }
                    .sortedBy { it.timestamp }
                for (info in fresh) appendExitInfo(info)
                fresh.maxOfOrNull { it.timestamp }?.let { prefs.edit().putLong(KEY_LAST_EXIT_TS, it).apply() }
            }
        }
    }

    private fun appendExitInfo(info: ApplicationExitInfo) {
        val exitTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date(info.timestamp))
        val trace = if (info.reason == ApplicationExitInfo.REASON_ANR) readExitTrace(info) else null
        val message = buildString {
            append("[ApplicationExitInfo] waktu_mati=").append(exitTime)
            append(" alasan=").append(exitReasonName(info.reason))
            append(" status=").append(info.status)
            append(" importance=").append(exitImportanceName(info.importance))
            append(" pid=").append(info.pid)
            append(" proses=").append(info.processName)
            append(" pss_kb=").append(info.pss)
            append(" rss_kb=").append(info.rss)
            append(" deskripsi=").append(info.description ?: "-")
            if (trace != null) append("\n--- trace ANR (awal, maks ").append(EXIT_TRACE_MAX_CHARS).append(" karakter) ---\n").append(trace)
        }
        appendEntry(if (info.reason in ABNORMAL_EXIT_REASONS) "ERROR" else "WARN", message)
    }

    private fun readExitTrace(info: ApplicationExitInfo): String? = runCatching {
        info.traceInputStream?.bufferedReader()?.use { reader ->
            val buffer = CharArray(EXIT_TRACE_MAX_CHARS)
            var total = 0
            while (total < EXIT_TRACE_MAX_CHARS) {
                val n = reader.read(buffer, total, EXIT_TRACE_MAX_CHARS - total)
                if (n < 0) break
                total += n
            }
            if (total > 0) String(buffer, 0, total) else null
        }
    }.getOrNull()

    private fun exitReasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_UNKNOWN -> "UNKNOWN"
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_CRASH -> "CRASH"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
        ApplicationExitInfo.REASON_OTHER -> "OTHER"
        else -> "KODE_$reason"
    }

    private fun exitImportanceName(importance: Int): String = when (importance) {
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> "FOREGROUND_SERVICE"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "VISIBLE"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "PERCEPTIBLE"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "SERVICE"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "CACHED"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE -> "GONE"
        else -> "KODE_$importance"
    }

    /** Writes a standalone crash report to the public Documents/AudioPlayer/logs folder so it's
     * reachable with a normal file manager even if the app can no longer be opened at all.
     *
     * Batch 34: brought in line with the original crash-logger spec, which this had drifted from —
     * filename now carries a UUID (two crashes in the same second no longer overwrite each other),
     * content now includes app version + OS + device model (not just thread/stacktrace, which alone
     * isn't enough to tell which build or which device a report came from), and a FIFO sweep now
     * caps this folder at 50 files instead of growing forever. */
    private fun writePublicCrashLog(thread: Thread, throwable: Throwable) {
        val context = appContext ?: return
        val fileName = "crash_${fileStampFormat.format(Date())}_${UUID.randomUUID()}.txt"
        val versionInfo = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
        }.getOrDefault("unknown")
        val content = "Waktu: ${dateFormat.format(Date())}\n" +
            "Versi Aplikasi: $versionInfo\n" +
            "OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "Perangkat: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
            "Thread: ${thread.name}\n\n" +
            Log.getStackTraceString(throwable)

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, CRASH_LOG_RELATIVE_PATH)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values) ?: return
        resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
        runCatching { enforceCrashLogRetention(context) }
    }

    /** Keeps only the newest [MAX_CRASH_LOGS] files in the public crash-log folder, oldest first
     * out — a crash loop (the exact scenario this logger exists for) must not fill the user's
     * Documents folder with an unbounded number of files. */
    private fun enforceCrashLogRetention(context: Context) {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("$CRASH_LOG_RELATIVE_PATH/", "crash_%.txt")
        val ids = mutableListOf<Long>()
        resolver.query(collection, projection, selection, selectionArgs, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) ids.add(cursor.getLong(idCol))
        }
        if (ids.size <= MAX_CRASH_LOGS) return
        for (id in ids.drop(MAX_CRASH_LOGS)) {
            runCatching {
                resolver.delete(ContentUris.withAppendedId(collection, id), null, null)
            }
        }
    }
}
