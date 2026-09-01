package com.rudi.audioplayer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.BackupManager
import kotlinx.coroutines.delay

/**
 * Gap List #10 — UI backup/restore. Launcher SAF (`OpenDocument`) sengaja dideklarasikan LANGSUNG
 * di sini (bukan di-drilling dari MainActivity seperti visualizerPermissionLauncher/
 * overlayPermissionLauncher) — `rememberLauncherForActivityResult` cuma butuh
 * `ActivityResultRegistryOwner`, yang tersedia di mana pun dalam pohon Compose Activity yang
 * sama termasuk di dalam ModalBottomSheet, jadi tidak ada alasan menambah parameter/launcher baru
 * ke MainActivity.kt (protected asset) untuk fitur yang lingkupnya murni 1 sheet ini.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreSheet(onDismiss: () -> Unit, onInfoMessage: (String) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Sama alasan DiagnosticLogSheet: ModalBottomSheet ada di layer sendiri di atas Scaffold,
    // Snackbar dari onInfoMessage bisa tertutup selagi sheet ini terbuka — banner inline supaya
    // hasil export/import selalu kelihatan.
    var resultBanner by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var pendingPayload by remember { mutableStateOf<BackupManager.BackupPayload?>(null) }

    LaunchedEffect(resultBanner) {
        if (resultBanner != null) {
            delay(3000)
            resultBanner = null
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val payload = BackupManager.readAndValidate(context, uri)
        haptic.performHapticFeedback(
            if (payload != null) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress
        )
        if (payload == null) {
            resultBanner = false to "File bukan backup SONIX yang valid, atau formatnya sudah tidak dikenali versi ini"
        } else {
            // Validasi lolos, TAPI belum langsung diterapkan — konfirmasi eksplisit dari user
            // (dialog di bawah) adalah pagar terakhir sebelum data saat ini benar-benar ditimpa.
            pendingPayload = payload
        }
    }

    // Batch 321 — fix blur lintas-window (root cause dikonfirmasi ulang via dokumentasi resmi
    // Haze: sample BottomSheet.kt/DialogSample.kt upstream MEMANG mendukung hazeEffect di dalam
    // ModalBottomSheet/Dialog yang sumbernya (`hazeSource`) berada di window Activity berbeda —
    // klaim Batch 311 "RenderNode tidak bisa sample lintas-window sama sekali" TIDAK akurat,
    // lihat catatan lengkap di PROJECT_STATE.md Batch 321). Syarat WAJIB dari sample resmi:
    // `containerColor = Color.Transparent` di ModalBottomSheet — sheet ini 1 dari 7 di seluruh
    // app yang kelewat sejak ditambahkan (17 total call site ModalBottomSheet, grep dikonfirmasi
    // ulang batch ini), beda dari `frostedGlass()`-nya sendiri yang sudah benar sejak awal.
    // Tanpa ini, containerColor DEFAULT Material3 (opak) berpotensi menimpa hazeEffect di
    // dalamnya tepat di tepi/sudut sheet. TIDAK menaikkan/menurunkan `liquidGlassAlpha` di
    // `BlurUtils.kt` batch ini — tetap di 0.85f/0.90f (fallback aman Batch 311) sampai user
    // verifikasi visual device bahwa blur benar-benar tampil sekarang, BUKAN diturunkan
    // spekulatif sebelum ada konfirmasi (lihat rasionalisasi penuh di PROJECT_STATE.md).
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        // Batch 316 — lanjutan audit Batch 314/315: prioritas 5 (terakhir) dari 5 sheet yang kena
        // pola sama (Column fixed tanpa verticalScroll/LazyColumn jaring pengaman). Konten pendek
        // & risiko rendah (judul, deskripsi, 2 tombol, banner hasil opsional), tapi tetap
        // diterapkan demi konsistensi pola jaring-pengaman di seluruh 22 sheet/screen ModalBottomSheet.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Cadangkan & Pulihkan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Simpan playlist, favorit, rating, riwayat & pengaturan ke 1 file lokal — bisa dipulihkan kalau app di-uninstall atau pindah HP. Selalu tersimpan di HP ini saja, tidak pernah dikirim ke mana pun.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            val backupInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = {
                    val fileName = BackupManager.exportToDocuments(context)
                    haptic.performHapticFeedback(
                        if (fileName != null) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress
                    )
                    resultBanner = if (fileName != null) {
                        true to "Backup tersimpan: Documents/AudioPlayer/backups/$fileName"
                    } else {
                        false to "Gagal membuat backup (perlu Android 10 ke atas)"
                    }
                    onInfoMessage(
                        if (fileName != null) "Backup tersimpan ke Documents/AudioPlayer/backups"
                        else "Gagal membuat backup"
                    )
                },
                interactionSource = backupInteraction,
                modifier = Modifier.fillMaxWidth().bouncyPress(backupInteraction)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buat Backup Sekarang")
            }

            Spacer(modifier = Modifier.height(8.dp))

            val restoreInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                interactionSource = restoreInteraction,
                modifier = Modifier.fillMaxWidth().bouncyPress(restoreInteraction)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pulihkan dari File")
            }

            resultBanner?.let { (ok, message) ->
                Spacer(modifier = Modifier.height(12.dp))
                ResultBanner(
                    style = ResultBannerStyle.Solid,
                    // Batch 228 — Iconography 3/7 (samakan visual weight icon sejenis).
                    // CheckCircle solid dipasangkan ErrorOutline (garis tipis) — bobot visual
                    // beda utk 1 pasangan sukses/gagal yg sama. Referensi pola benar:
                    // SignatureMatcherSheet.kt pakai Icons.Default.Error (solid) utk gagal,
                    // konsisten dgn CheckCircle solid. Samakan di sini.
                    icon = if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                    text = message,
                    containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (ok) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }

    pendingPayload?.let { payload ->
        val counts = payload.summaryCounts()
        AlertDialog(
            onDismissRequest = { pendingPayload = null },
            title = { Text("Timpa data saat ini?") },
            text = {
                Column {
                    Text(
                        "Data di bawah ini akan MENGGANTI data yang ada sekarang di app. Aksi ini tidak bisa dibatalkan.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (counts.isEmpty()) {
                        Text(
                            "File ini tidak berisi data yang dikenali.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        counts.forEach { (label, count) ->
                            Text("• $label ($count)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        BackupManager.applyBackup(context, payload)
                        pendingPayload = null
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        resultBanner = true to "Data berhasil dipulihkan. Tutup dan buka ulang app supaya semua layar ikut ter-refresh."
                        onInfoMessage("Data berhasil dipulihkan")
                    },
                    enabled = counts.isNotEmpty()
                ) {
                    Text("Timpa & Pulihkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPayload = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
