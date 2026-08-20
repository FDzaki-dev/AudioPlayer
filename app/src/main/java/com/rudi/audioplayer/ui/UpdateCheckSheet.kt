package com.rudi.audioplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.BuildConfig
import com.rudi.audioplayer.update.UpdateManager

/**
 * Release Downloader Spec — manual-only entry point, never runs on app start. Purely additive:
 * reachable only from the new "Cek Update" row in Settings, doesn't alter any existing screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateCheckSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by UpdateManager.state.collectAsState()

    DisposableEffect(Unit) {
        UpdateManager.checkForUpdate(BuildConfig.VERSION_NAME)
        onDispose { UpdateManager.reset() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Cek Update", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Versi terpasang: ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(20.dp))

            when (val s = state) {
                is UpdateManager.UpdateState.Idle,
                is UpdateManager.UpdateState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Mengecek rilis terbaru di GitHub…")
                    }
                }
                is UpdateManager.UpdateState.UpToDate -> {
                    StatusBanner(
                        color = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Default.CheckCircle,
                        text = "Sudah versi terbaru (${s.currentVersion})"
                    )
                }
                is UpdateManager.UpdateState.Available -> {
                    Text("Update tersedia: ${s.release.tagName}", style = MaterialTheme.typography.bodyMedium)
                    // Batch 156 — catatan rilis (pesan commit git HEAD, lihat GitHubReleaseChecker.kt
                    // & build.yml "Create GitHub Release"). Blank check WAJIB: rilis lama pra-Batch
                    // 156 tidak punya body sama sekali, section ini harus hilang total (bukan
                    // nampilin kotak kosong) daripada terlihat rusak/setengah-jadi.
                    if (s.release.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            s.release.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { UpdateManager.downloadAndPrepareInstall(context, s.release) }) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unduh & Instal")
                    }
                }
                is UpdateManager.UpdateState.Downloading -> {
                    Text("Mengunduh… ${s.progressPercent}%", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { s.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is UpdateManager.UpdateState.ReadyToInstall -> {
                    StatusBanner(
                        color = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Default.CheckCircle,
                        text = "Unduhan selesai — siap instal"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { UpdateManager.launchInstall(context, s.apkFile) }) {
                        Text("Buka Installer")
                    }
                }
                is UpdateManager.UpdateState.Error -> {
                    StatusBanner(
                        color = MaterialTheme.colorScheme.error,
                        icon = Icons.Default.Error,
                        text = s.message
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    // Batch 165 — delegates to the shared ResultBanner (Bare style) so this and the 2 other
    // hand-duplicated result-banner implementations (BackupRestoreSheet/DiagnosticLogSheet,
    // SignatureMatcherSheet) can't silently drift apart again. Same look as before: no
    // background, just icon+gap+text, kept minimal on purpose since this sits inside a
    // multi-step stepper (Checking→Available→Downloading→ReadyToInstall→Error) next to a
    // progress bar/button — a full banner would compete with those visually.
    ResultBanner(
        style = ResultBannerStyle.Bare,
        icon = icon,
        text = text,
        containerColor = color,
        contentColor = color
    )
}
