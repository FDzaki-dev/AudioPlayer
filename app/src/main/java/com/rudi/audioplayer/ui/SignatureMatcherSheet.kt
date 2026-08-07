package com.rudi.audioplayer.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.util.ApkSignatureChecker
import com.rudi.audioplayer.util.ApkSignatureResult

/**
 * Lets the user pick two APK files (e.g. the currently-installed build and a freshly
 * downloaded one) and checks whether they're signed with the same certificate — the exact
 * thing Android silently checks before allowing an update to install over an existing app.
 * Fully local: files are read straight off the device, nothing is uploaded anywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureMatcherSheet(onDismiss: () -> Unit, onInfoMessage: (String) -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var oldResult by remember { mutableStateOf<ApkSignatureResult?>(null) }
    var newResult by remember { mutableStateOf<ApkSignatureResult?>(null) }
    var showLogDialog by remember { mutableStateOf(false) }

    val oldPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) oldResult = ApkSignatureChecker.inspect(context, uri, displayNameFor(context, uri))
    }
    val newPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) newResult = ApkSignatureChecker.inspect(context, uri, displayNameFor(context, uri))
    }

    val matchState: MatchState? = remember(oldResult, newResult) {
        val o = oldResult
        val n = newResult
        when {
            o == null || n == null -> null
            !o.isOk || !n.isOk -> MatchState.ERROR
            o.sha256 == n.sha256 -> MatchState.MATCH
            else -> MatchState.MISMATCH
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Pencocok Signature APK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Cek apakah dua APK (versi lama & baru) ditandatangani dengan key yang sama — inilah yang menentukan apakah instalasi bisa update langsung atau minta uninstall dulu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(20.dp))

            ApkPickerRow(
                label = "APK Lama",
                result = oldResult,
                onPick = { oldPicker.launch(arrayOf("application/vnd.android.package-archive")) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ApkPickerRow(
                label = "APK Baru",
                result = newResult,
                onPick = { newPicker.launch(arrayOf("application/vnd.android.package-archive")) }
            )

            if (matchState != null) {
                Spacer(modifier = Modifier.height(20.dp))
                val (bannerColor, bannerIcon, bannerText) = when (matchState) {
                    MatchState.MATCH -> Triple(MaterialTheme.colorScheme.tertiary, Icons.Default.CheckCircle, "Signature COCOK — key sama persis")
                    MatchState.MISMATCH -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Error, "Signature TIDAK COCOK — instalasi akan minta uninstall dulu")
                    MatchState.ERROR -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Error, "Ada masalah membaca salah satu file")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bannerColor.copy(alpha = 0.15f), shape = MaterialTheme.shapes.medium)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(bannerIcon, contentDescription = null, tint = bannerColor)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(bannerText, style = MaterialTheme.typography.bodyMedium, color = bannerColor)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showLogDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Lihat Laporan Lengkap")
                }
            }
        }
    }

    if (showLogDialog) {
        val report = buildReport(oldResult, newResult, matchState)
        SignatureLogDialog(report = report, onDismiss = { showLogDialog = false }, onInfoMessage = onInfoMessage)
    }
}

private enum class MatchState { MATCH, MISMATCH, ERROR }

@Composable
private fun ApkPickerRow(label: String, result: ApkSignatureResult?, onPick: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FileOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(result?.fileName ?: "Pilih File APK")
        }
        if (result != null) {
            Spacer(modifier = Modifier.height(6.dp))
            if (result.error != null) {
                Text(
                    result.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    "${result.packageName ?: "?"}  •  v${result.versionName ?: "?"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    result.sha256 ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/** Full copyable report — this is the dialog the user gets instead of Android's bare "OK" popup. */
@Composable
private fun SignatureLogDialog(report: String, onDismiss: () -> Unit, onInfoMessage: (String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Laporan Lengkap") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    report,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(report))
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onInfoMessage("Laporan disalin ke papan klip")
            }) {
                Text("Salin ke Papan Klip")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

private fun buildReport(old: ApkSignatureResult?, new: ApkSignatureResult?, state: MatchState?): String {
    val sb = StringBuilder()
    sb.appendLine("=== Laporan Pencocokan Signature APK ===")
    sb.appendLine()
    sb.appendLine("[APK Lama]")
    sb.appendLine(reportBlock(old))
    sb.appendLine()
    sb.appendLine("[APK Baru]")
    sb.appendLine(reportBlock(new))
    sb.appendLine()
    sb.appendLine(
        "Hasil: " + when (state) {
            MatchState.MATCH -> "COCOK — signature identik, update bisa langsung dipasang."
            MatchState.MISMATCH -> "TIDAK COCOK — signature berbeda, sistem akan minta uninstall dulu sebelum install ulang."
            MatchState.ERROR -> "GAGAL DIBACA — lihat pesan error di atas untuk masing-masing file."
            null -> "Belum lengkap — pilih kedua file APK."
        }
    )
    return sb.toString()
}

private fun reportBlock(result: ApkSignatureResult?): String {
    if (result == null) return "  (belum dipilih)"
    if (result.error != null) return "  File     : ${result.fileName}\n  Error    : ${result.error}"
    return buildString {
        append("  File     : ${result.fileName}\n")
        append("  Package  : ${result.packageName ?: "-"}\n")
        append("  Versi    : ${result.versionName ?: "-"}\n")
        append("  SHA-256  : ${result.sha256 ?: "-"}")
    }
}

private fun displayNameFor(context: Context, uri: Uri): String {
    var name = uri.lastPathSegment ?: "apk"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex) ?: name
            }
        }
    } catch (e: Exception) {
        // Fall back to the URI's last path segment already assigned above.
    }
    return name
}
