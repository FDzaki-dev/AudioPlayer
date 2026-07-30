package com.rudi.audioplayer.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local-only diagnostic log. Every entry is written to a single file in this app's private
 * storage (never anywhere else) so a crash or a caught-but-swallowed error leaves a trace the
 * user can view and copy (Settings > Lanjutan > Log Diagnostik) instead of vanishing with
 * nothing to go on. Deliberately not a third-party crash SDK: this app has no INTERNET
 * permission and nothing it collects should ever need one — a remote crash reporter would
 * require adding that permission and would undercut the "everything stays on this device"
 * guarantee the app otherwise makes.
 */
object AppLogger {
    private const val LOG_FILE_NAME = "diagnostic_log.txt"

    // Once the file crosses this size, the oldest half of its lines is dropped — keeps the
    // log useful (recent-first context for whatever just went wrong) without growing forever.
    private const val MAX_LOG_BYTES = 200_000L

    private var logFile: File? = null
    private var previousHandler: Thread.UncaughtExceptionHandler? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /** Call once from Application.onCreate(). Safe to call more than once (no-ops after the first). */
    @Synchronized
    fun init(context: Context) {
        if (logFile != null) return
        logFile = File(context.applicationContext.filesDir, LOG_FILE_NAME)

        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                appendEntry("FATAL", "Uncaught di thread '${thread.name}': ${Log.getStackTraceString(throwable)}")
            }
            // Always defer to whatever handler existed before (or terminate normally if there
            // was none) — this logger only ever observes a crash, never changes how it's handled.
            previousHandler?.uncaughtException(thread, throwable) ?: Runtime.getRuntime().exit(2)
        }
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
}
