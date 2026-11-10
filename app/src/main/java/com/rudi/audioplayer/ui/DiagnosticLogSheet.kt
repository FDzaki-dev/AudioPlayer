package com.rudi.audioplayer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
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
import com.rudi.audioplayer.util.DumpsysCollector
import com.rudi.audioplayer.util.ShizukuBridge
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
    // Batch 627 — tab: 0 = Log, 1 = Izin dumpsys (rememberSaveable: tahan rotasi).
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val panelShape = MaterialTheme.shapes.large
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        // Batch 630 — permintaan user: tab Log Diagnostik FULL SCREEN, bukan setengah layar. Sebelumnya panel dibatasi
        // `heightIn(max = 560.dp)` + kotak isi `weight(1f, fill = false)` (membungkus konten) -> sheet hanya ~setengah tinggi
        // layar. Sekarang `fillMaxHeight()` (= tinggi maksimum yang diberikan sheet; pola sama AdvancedControlsSheet Batch 554,
        // insets status/navigasi tetap ditangani `ModalBottomSheet`) dan kotak log / panel Izin dumpsys `weight(1f)` mengisi
        // sisa ruang, sehingga tombol Ekspor/Simpan/Hapus tetap menempel di bawah dan area log/centang selapang mungkin.
        // Logika, state (`tab` rememberSaveable), dan isi TIDAK diubah.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, panelShape)
                .frostedGlass()
                .fillMaxHeight()
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

            DiagnosticTabs(selected = tab, onSelect = { tab = it })
            Spacer(modifier = Modifier.height(10.dp))

            if (tab == 0) {
                // Wadah log: satu bentuk untuk keadaan memuat / kosong / berisi, supaya tinggi tidak melompat-lompat.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
            } else {
                DumpsysPermissionPanel(modifier = Modifier.fillMaxWidth().weight(1f))
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

/** Batch 627 — pemilih tab sederhana (Log | Izin dumpsys); sengaja bukan TabRow bawaan agar tak menambah peringatan deprecated. */
@Composable
private fun DiagnosticTabs(selected: Int, onSelect: (Int) -> Unit) {
    val titles = listOf("Log", "Izin dumpsys")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        titles.forEachIndexed { index, title ->
            val isSelected = index == selected
            val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) tint.copy(alpha = 0.14f) else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = tint,
                    maxLines = 1
                )
            }
        }
    }
}

/** Keadaan tab Izin dumpsys hasil satu kali pemeriksaan (dihitung di IO). */
private class DumpsysPanelState(
    val granted: Boolean,
    val shizuku: ShizukuBridge.State,
    val access: DumpsysCollector.Access,
    val test: DumpsysCollector.TestResult?,
    val selected: Set<String>
)

/**
 * Batch 627/628 — tab "Izin dumpsys": status akses (izin DUMP langsung atau Shizuku) + uji dumpsys nyata (centang hijau /
 * silang merah), tombol Shizuku (buka, izinkan, beri izin DUMP), perintah adb cadangan (bisa disalin), dan centang per
 * bagian dumpsys yang ikut "Ekspor Logcat Debug" (default semua ON, tersimpan). Semua pembacaan (izin, Shizuku, uji
 * proses, prefs) di Dispatchers.IO; tak ada yang dikirim ke mana pun.
 */
@Composable
private fun DumpsysPermissionPanel(modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sections = remember { DumpsysCollector.sections(context.packageName) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var checking by remember { mutableStateOf(true) }
    var panel by remember {
        mutableStateOf(
            DumpsysPanelState(false, ShizukuBridge.State.NOT_INSTALLED, DumpsysCollector.Access.NONE, null, sections.map { it.id }.toSet())
        )
    }
    var working by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick) {
        checking = true
        // Batch 629 — galat apa pun saat memeriksa tak boleh menjatuhkan aplikasi (crash v628): jatuh ke keadaan "tak ada akses".
        val fallback = DumpsysPanelState(false, ShizukuBridge.State.NOT_INSTALLED, DumpsysCollector.Access.NONE, null, sections.map { it.id }.toSet())
        panel = runCatching {
            withContext(Dispatchers.IO) {
                val access = DumpsysCollector.accessMode(context)
                DumpsysPanelState(
                    granted = DumpsysCollector.isPermissionGranted(context),
                    shizuku = ShizukuBridge.state(context),
                    access = access,
                    test = if (access != DumpsysCollector.Access.NONE) DumpsysCollector.testDumpsys(context) else null,
                    selected = sections.filter { DumpsysCollector.isSelected(context, it.id) }.map { it.id }.toSet()
                )
            }
        }.getOrDefault(fallback)
        checking = false
    }
    // Hasil dialog izin Shizuku -> periksa ulang otomatis.
    DisposableEffect(Unit) {
        val listener = ShizukuBridge.addPermissionListener { refreshTick++ }
        onDispose { listener?.let { ShizukuBridge.removePermissionListener(it) } }
    }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    val setSection: (String, Boolean) -> Unit = { id, on ->
        panel = DumpsysPanelState(
            panel.granted, panel.shizuku, panel.access, panel.test,
            if (on) panel.selected + id else panel.selected - id
        )
        scope.launch(Dispatchers.IO) { DumpsysCollector.setSelected(context, id, on) }
    }
    val command = remember { DumpsysCollector.grantCommand(context) }
    val secondary = MaterialTheme.colorScheme.secondary
    val noAccess = panel.access == DumpsysCollector.Access.NONE

    Column(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Izin dumpsys", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Dengan akses dumpsys, \"Ekspor Logcat Debug\" ikut memuat keluaran dumpsys (Recents, sesi media, audio, notifikasi, " +
                "widget, memori, dst.) sehingga log jauh lebih lengkap. Akses didapat lewat Shizuku (disarankan) atau izin DUMP " +
                "lewat adb. Tanpa keduanya, ekspor tetap berjalan seperti biasa.",
            style = MaterialTheme.typography.bodySmall,
            color = secondary
        )
        CheckRow(
            ok = panel.shizuku == ShizukuBridge.State.READY,
            loading = checking,
            title = "Shizuku",
            detail = when {
                checking -> "Memeriksa…"
                panel.shizuku == ShizukuBridge.State.READY -> "Siap dan sudah diizinkan"
                panel.shizuku == ShizukuBridge.State.NEED_PERMISSION -> "Berjalan, belum mengizinkan aplikasi ini"
                panel.shizuku == ShizukuBridge.State.NOT_RUNNING -> "Terpasang tetapi belum berjalan (jalankan lewat Wireless debugging di aplikasi Shizuku)"
                else -> "Belum terpasang (RikkaApps/Shizuku)"
            }
        )
        CheckRow(
            ok = panel.granted,
            loading = checking,
            title = "Izin DUMP",
            detail = if (checking) "Memeriksa…" else if (panel.granted) "Diberikan" else "Belum diberikan"
        )
        CheckRow(
            ok = panel.test?.ok == true,
            loading = checking,
            title = "Uji dumpsys",
            detail = if (checking) "Memeriksa…" else if (noAccess) "Tidak dijalankan (belum ada akses)" else panel.test?.detail.orEmpty()
        )
        note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }

        // Tombol Shizuku sesuai keadaan.
        when (panel.shizuku) {
            ShizukuBridge.State.NEED_PERMISSION -> OutlinedButton(
                onClick = {
                    note = if (ShizukuBridge.requestPermission()) null else "Gagal membuka dialog izin Shizuku."
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Izinkan lewat Shizuku", maxLines = 1, overflow = TextOverflow.Ellipsis) }

            ShizukuBridge.State.READY -> if (!panel.granted) OutlinedButton(
                onClick = {
                    working = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { DumpsysCollector.grantViaShizuku(context) }
                        note = result.detail
                        working = false
                        refreshTick++
                    }
                },
                enabled = !working,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(if (working) "Memberi izin…" else "Beri izin DUMP via Shizuku", maxLines = 1, overflow = TextOverflow.Ellipsis) }

            ShizukuBridge.State.NOT_RUNNING -> OutlinedButton(
                onClick = {
                    ShizukuBridge.launchIntent(context)?.let { intent ->
                        note = if (runCatching { context.startActivity(intent) }.isSuccess) null else "Gagal membuka Shizuku."
                    }
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Buka Shizuku", maxLines = 1, overflow = TextOverflow.Ellipsis) }

            ShizukuBridge.State.NOT_INSTALLED -> Unit
        }

        if (noAccess) {
            Text(
                "Cadangan tanpa Shizuku: beri izin SEKALI lewat adb (tanpa root), mis. dari PC atau Termux + Wireless debugging, " +
                    "lalu tekan Periksa ulang:",
                style = MaterialTheme.typography.bodySmall,
                color = secondary
            )
            Text(
                command,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .padding(10.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (noAccess) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("perintah izin DUMP", command))
                        copied = true
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (copied) "Tersalin" else "Salin perintah", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedButton(
                onClick = { refreshTick++ },
                enabled = !checking,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Periksa ulang", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Text(
            "Bagian yang ikut diekspor (${panel.selected.size} dari ${sections.size})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        sections.forEach { section ->
            val on = section.id in panel.selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Checkbox) { setSection(section.id, !on) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = on, onCheckedChange = { setSection(section.id, it) })
                Column(modifier = Modifier.weight(1f)) {
                    Text(section.label, style = MaterialTheme.typography.bodyMedium)
                    Text(section.description, style = MaterialTheme.typography.bodySmall, color = secondary)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { sections.forEach { setSection(it.id, true) } }) { Text("Centang semua") }
            TextButton(onClick = { sections.forEach { setSection(it.id, false) } }) { Text("Kosongkan") }
        }
    }
}

/** Satu baris status: ikon centang hijau (ok) / silang merah (gagal) / pemuat, judul, rincian. */
@Composable
private fun CheckRow(ok: Boolean, loading: Boolean, title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = if (ok) "Lulus" else "Belum",
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
