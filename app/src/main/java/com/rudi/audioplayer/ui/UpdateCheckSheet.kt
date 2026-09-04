package com.rudi.audioplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.rudi.audioplayer.ui.theme.frostedGlass
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

    // Batch 339 — BUG FIX (laporan user: "sudah selesai install update package tapi gak sengaja
    // salah mencet, malah ke cancel dari awal lagi unduhannya"). Root cause: `onDispose` di bawah
    // SEBELUMNYA panggil `UpdateManager.reset()` TANPA SYARAT tiap sheet ini keluar dari komposisi
    // (sengaja ditutup ATAU salah ke-tap/ke-dismiss) — termasuk saat state sedang `Downloading`
    // (thread unduhan TETAP jalan di background, tidak ikut ke-cancel betulan) atau sudah
    // `ReadyToInstall` (APK SUDAH lengkap di cache). Reset ke Idle di momen itu SIA-SIA membuang
    // progres asli, dan `checkForUpdate()` di baris atas bakal jalan LAGI dari nol tiap sheet
    // dibuka ulang — user kelihatannya "harus unduh ulang dari awal" walau APK sebenarnya sudah
    // ada/lengkap di cache. FIX: skip checkForUpdate() (on-enter) & reset() (on-dispose) SAMA
    // SEKALI kalau state saat ini `Downloading` atau `ReadyToInstall` — 2 state itu representasi
    // kerja nyata (unduhan jalan/APK jadi) yang TIDAK BOLEH hilang cuma krn sheet ke-tutup. State
    // lain (Idle/Checking/UpToDate/Available/Error) — 0 perubahan perilaku, tetap cek ulang tiap
    // buka & reset tiap tutup seperti sebelumnya (tidak ada progres berarti yang bisa hilang).
    DisposableEffect(Unit) {
        val stateOnEnter = UpdateManager.state.value
        if (stateOnEnter !is UpdateManager.UpdateState.Downloading &&
            stateOnEnter !is UpdateManager.UpdateState.ReadyToInstall
        ) {
            UpdateManager.checkForUpdate(BuildConfig.VERSION_NAME)
        }
        onDispose {
            val stateOnExit = UpdateManager.state.value
            if (stateOnExit !is UpdateManager.UpdateState.Downloading &&
                stateOnExit !is UpdateManager.UpdateState.ReadyToInstall
            ) {
                UpdateManager.reset()
            }
        }
    }

    // Batch 323 — fix blur lintas-window, pola sama Batch 322 (rasionalisasi penuh di
    // PROJECT_STATE.md Batch 321/322): tambah `containerColor = Color.Transparent` yang kelewat
    // sejak sheet ini dibuat. `Color` sudah diimpor sebelumnya (dipakai fungsi lain file ini).
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        // Batch 339 — BUG FIX (laporan user + screenshot: "tab update masih mengalami regresi
        // tembus pandang" — teks "Tentang Aplikasi" dari SettingsScreen di belakang kelihatan
        // tembus/tumpang-tindih). Root cause: `containerColor = Color.Transparent` (Batch 322/323,
        // syarat WAJIB dari sample resmi Haze) TERNYATA tidak cukup SENDIRIAN — itu cuma matikan
        // fill solid Material3 default, TIDAK menggambar blur apa pun. Elemen yang benar-benar
        // menggambar blur adalah `.frostedGlass()` (`BlurUtils.kt`) — dan sheet ini SATU-SATUNYA
        // (dibanding 12+ call site lain: RingtoneCutterSheet.kt, SongInfoEditSheet.kt, dst) yang
        // KELEWAT modifier ini sejak dibuat. Transparent TANPA frostedGlass() = benar-benar
        // tembus pandang (0 blur, 0 fill) — bukan cuma "kurang blur", tapi literally kosong,
        // konten di baliknya (SettingsScreen) kelihatan penuh tanpa filter. FIX: `.frostedGlass()`
        // ditambah persis di posisi yang sama seperti RingtoneCutterSheet.kt/SongInfoEditSheet.kt
        // (setelah `.fillMaxWidth()`, sebelum `.verticalScroll()`) — pola identik, 0 penyesuaian.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass()
                .verticalScroll(rememberScrollState())
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
                        // Batch 277 — release_notes.txt (build.yml) sekarang diawali baris
                        // "v1.3.14-run276 (e4ea7ca)" + baris kosong SEBELUM pesan commit
                        // (biar halaman web GitHub Release lebih informatif, bukan cuma pesan
                        // commit polos). Prefix itu REDUNDAN di sini — "Update tersedia:
                        // ${tagName}" di atas SUDAH menampilkan info versi yang sama persis.
                        // `substringAfter("\n\n", releaseNotes)` buang prefix itu SEBELUM
                        // baris kosong pertama; fallback ke `releaseNotes` utuh kalau tidak
                        // ketemu separator (rilis lama pra-Batch-277 belum punya format ini).
                        Text(
                            s.release.releaseNotes.substringAfter("\n\n", s.release.releaseNotes),
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
