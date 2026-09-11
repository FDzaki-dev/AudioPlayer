package com.rudi.audioplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    // Batch 321 — fix blur lintas-window, pola sama BackupRestoreSheet.kt batch ini (rasionalisasi
    // penuh + link dokumentasi resmi Haze di sana / PROJECT_STATE.md Batch 321): tambah
    // `containerColor = Color.Transparent` yang kelewat sejak sheet ini dibuat.
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        // Batch 340 — BUG FIX (lanjutan antrean "🔍 Audit tambahan" Batch 339, pola identik
        // BackupRestoreSheet.kt/UpdateCheckSheet.kt batch ini — lihat PROJECT_STATE.md Batch 340
        // utk rasionalisasi penuh): `.frostedGlass()` ditambah setelah `.fillMaxWidth()`.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass()
                .heightIn(max = 480.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Log Diagnostik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Catatan error & crash tersimpan lokal di HP ini saja — tidak pernah dikirim ke mana pun.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoadingLog) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (logText.isBlank()) {
                Text(
                    "Belum ada catatan. Kalau nanti ada error atau crash, jejaknya akan muncul di sini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        logText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Repack ke Dokumen")
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
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus")
                }
            }

            exportResult?.let { ok ->
                Spacer(modifier = Modifier.height(12.dp))
                ResultBanner(
                    style = ResultBannerStyle.Solid,
                    // Batch 228 — Iconography 3/7 (samakan visual weight icon sejenis),
                    // konsisten dgn fix BackupRestoreSheet.kt & referensi SignatureMatcherSheet.kt.
                    icon = if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                    text = if (ok) "Tersimpan di Documents/AudioPlayer/logs" else "Gagal menyimpan (perlu Android 10 ke atas)",
                    containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (ok) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
