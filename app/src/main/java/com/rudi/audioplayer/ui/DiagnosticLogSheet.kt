package com.rudi.audioplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Read-only viewer for AppLogger's local diagnostic log — errors it caught, and the
 * stack trace of any uncaught crash. Purely a window onto a file that already lives in this
 * app's private storage; nothing shown here is ever sent anywhere. Lets the user actually see
 * what would otherwise be an invisible failure, and copy it out if they want help debugging it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticLogSheet(onDismiss: () -> Unit, onInfoMessage: (String) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Batch 431 — Thread Safety (sektor "belum terjamah" di luar ui/, dibuka eksplisit per
    // instruksi user): `AppLogger.readLog()` sebelumnya dipanggil LANGSUNG di body composable
    // (initial value `remember`) — baca file (bisa sampai ~200KB, MAX_LOG_BYTES) di Main thread
    // tiap sheet ini pertama dibuka. Gap-nya spesifik: audit literal grep I/O app-wide `ui/`
    // (PROJECT_STATE.md) hanya menyisir syntax I/O yang ADA LANGSUNG di file `ui/` — 0 hit di sini
    // karena `FileInputStream`/`readText()` sendiri hidup di `util/AppLogger.kt` (paket lain, belum
    // diaudit), bukan di file ini; audit sebelumnya tidak menyisir call site fungsi lintas-paket
    // yang blocking. Fix: pindah ke `LaunchedEffect(Unit)` + `Dispatchers.IO`, pola sama
    // SignatureMatcherSheet.kt (Batch 421)/BackupRestoreSheet.kt.
    var logText by remember { mutableStateOf("") }
    var isLoadingLog by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val text = withContext(Dispatchers.IO) { AppLogger.readLog() }
        logText = text
        isLoadingLog = false
    }
    // Inline feedback instead of relying solely on onInfoMessage's Snackbar: ModalBottomSheet
    // renders in its own layer above Scaffold, so a Snackbar fired while this sheet is open is
    // visually stuck behind it — user taps "Repack ke Dokumen" and sees nothing happen, easy to
    // mistake for a hang. This banner lives inside the sheet itself, so it's always visible.
    var exportResult by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(exportResult) {
        if (exportResult != null) {
            delay(2500)
            exportResult = null
        }
    }

    // Batch 603 — crash terbaru (sisi baca AppLogger, diadaptasi dari CrashLogger Boomly): tampil sebagai "baru"
    // SEKALI lalu ditandai sudah dilihat. Query MediaStore -> Dispatchers.IO (bukan di body composable).
    var latestCrash by remember { mutableStateOf<AppLogger.CrashLogEntry?>(null) }
    var crashUnseen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val (crash, unseen) = withContext(Dispatchers.IO) {
            val latest = AppLogger.latestCrashLog(context)
            val isNew = latest != null && AppLogger.isCrashUnseen(context, latest)
            if (latest != null) AppLogger.markCrashSeen(context)
            latest to isNew
        }
        latestCrash = crash
        crashUnseen = unseen
    }

    // Batch 603 — ekspor logcat debug (DiagnosticLogSheet sudah punya banner inline karena Snackbar tertutup sheet).
    var debugExporting by remember { mutableStateOf(false) }
    var debugExport by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    LaunchedEffect(debugExport) {
        if (debugExport != null) {
            delay(4000)
            debugExport = null
        }
    }

    // Batch 604 — LAYOUT DIRAPIKAN (laporan user + screen recording: "layout berantakan dan gak estetik"). Tiga akar yang
    // terlihat di video: (1) panel `frostedGlass()` di dalam `ModalBottomSheet` = jendela TERPISAH, jadi tak ada sumber blur
    // dan panel hanya tint transparan -> teks layar Pengaturan di belakangnya tembus dan menumpuk dengan isi sheet;
    // sekarang panel punya DASAR OPAK (`colorScheme.surface`) di bawah kaca (rim/tint/kedalaman tema tetap digambar di atasnya).
    // (2) Dua tombol `weight(1f)` berdampingan: label "Repack ke Dokumen" terlalu panjang untuk setengah lebar -> patah per
    // huruf ("Dokume/n") dan tombol pil memanjang jadi oval raksasa; sekarang label pendek ("Simpan Log"), `maxLines = 1`,
    // tinggi seragam 48dp, tombol utama "Ekspor Logcat Debug" penuh lebar di atas. (3) Isi log tanpa wadah menempel ke tombol;
    // sekarang di kotak berlekuk sendiri (tinggi dibatasi, bisa scroll) dengan waktu + tingkat (WARN/ERROR/FATAL) berwarna.
    // Logika (muat IO, ekspor, hapus, crash terakhir, banner) TIDAK diubah. Catatan lama (Batch 321/340/465): sheet tetap
    // `containerColor = Transparent` (jendela Haze), `.frostedGlass()` setelah `.fillMaxWidth()`, log tetap LazyColumn per baris.
    val panelShape = MaterialTheme.shapes.large
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, panelShape)
                .frostedGlass()
                .heightIn(max = 560.dp)
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 28.dp)
        ) {
            Text(
                "Log Diagnostik",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Catatan error & crash tersimpan lokal di HP ini saja — tidak pernah dikirim ke mana pun.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            latestCrash?.let { crash ->
                Spacer(modifier = Modifier.height(12.dp))
                val crashTint = if (crashUnseen) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(crashTint.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = crashTint)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (crashUnseen) "Crash baru terdeteksi" else "Crash terakhir",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = crashTint
                        )
                        Text(
                            crash.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Wadah log: satu bentuk untuk keadaan memuat / kosong / berisi, supaya tinggi tidak melompat-lompat.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            ) {
                if (isLoadingLog) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (logText.isBlank()) {
                    Text(
                        "Belum ada catatan. Kalau nanti ada error atau crash, jejaknya akan muncul di sini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else {
                    // Batch 465 — FIX force-close/freeze: split per baris + LazyColumn (Compose hanya measure baris
                    // yang KELIHATAN); `logText` tetap dibaca utuh, "Simpan Log" tetap mengekspor `readLog()` utuh.
                    val logLines = remember(logText) { logText.split("\n") }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        items(logLines) { line -> LogLine(line) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    debugExporting = true
                    scope.launch(Dispatchers.IO) {
                        val path = AppLogger.exportDebugLogToDocuments(context)
                        withContext(Dispatchers.Main) {
                            debugExporting = false
                            haptic.performHapticFeedback(
                                if (path != null) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress
                            )
                            debugExport = if (path != null) {
                                Pair(true, "Tersimpan di $path")
                            } else {
                                Pair(false, "Gagal menyimpan log debug")
                            }
                            onInfoMessage(if (path != null) "Log debug disimpan ke $path" else "Gagal menyimpan log debug")
                        }
                    }
                },
                enabled = !debugExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (debugExporting) "Membaca logcat…" else "Ekspor Logcat Debug", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val ok = AppLogger.exportLogToDocuments(context)
                            withContext(Dispatchers.Main) {
                                haptic.performHapticFeedback(
                                    if (ok) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress
                                )
                                exportResult = ok
                                onInfoMessage(
                                    if (ok) "Log disimpan ke Documents/AudioPlayer/logs"
                                    else "Gagal menyimpan log (perlu Android 10+)"
                                )
                            }
                        }
                    },
                    enabled = logText.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Log", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            AppLogger.clearLog()
                            withContext(Dispatchers.Main) {
                                logText = ""
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onInfoMessage("Log diagnostik dihapus")
                            }
                        }
                    },
                    enabled = logText.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            debugExport?.let { (ok, text) ->
                Spacer(modifier = Modifier.height(12.dp))
                ResultBanner(
                    style = ResultBannerStyle.Solid,
                    icon = if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                    text = text,
                    containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (ok) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }

            exportResult?.let { ok ->
                Spacer(modifier = Modifier.height(12.dp))
                ResultBanner(
                    style = ResultBannerStyle.Solid,
                    // Batch 228 — Iconography 3/7 (samakan visual weight icon sejenis).
                    icon = if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                    text = if (ok) "Tersimpan di Documents/AudioPlayer/logs" else "Gagal menyimpan (perlu Android 10 ke atas)",
                    containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (ok) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/** Awal entri log (format persis `AppLogger.appendEntry`): `yyyy-MM-dd HH:mm:ss LEVEL pesan`. */
private val LOG_HEADER_REGEX = Regex("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) (FATAL|ERROR|WARN) (.*)$")

/**
 * Satu baris log. Baris awal entri dipecah: waktu (redup) + tingkat (WARN = aksen, ERROR/FATAL = merah) di atas, pesan di
 * bawahnya, dengan jarak antar-entri; baris lanjutan (stack trace) tampil apa adanya. Teks TIDAK diubah, hanya ditata.
 */
@Composable
private fun LogLine(line: String) {
    val match = remember(line) { LOG_HEADER_REGEX.matchEntire(line) }
    val bodyColor = MaterialTheme.colorScheme.onSurface
    if (match != null) {
        val time = match.groupValues[1]
        val level = match.groupValues[2]
        val message = match.groupValues[3]
        val levelColor = if (level == "WARN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        val dimColor = MaterialTheme.colorScheme.secondary
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = levelColor, fontWeight = FontWeight.Bold)) { append(level) }
                    withStyle(SpanStyle(color = dimColor)) { append("  ") ; append(time) }
                },
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = bodyColor
            )
        }
    } else {
        Text(
            line,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = bodyColor
        )
    }
}
