package com.rudi.audioplayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.BuildConfig
import com.rudi.audioplayer.data.Song
import com.rudi.audioplayer.data.lyrics.LyricsPrefetchStore
import com.rudi.audioplayer.data.lyrics.LyricsRepository
import com.rudi.audioplayer.ui.theme.ThemeIdentity
import com.rudi.audioplayer.ui.theme.ThemeMode
import com.rudi.audioplayer.ui.theme.colorsFor
import com.rudi.audioplayer.ui.theme.neuRowTile
import com.rudi.audioplayer.ui.theme.neuPressTile
import com.rudi.audioplayer.ui.theme.tactileEmboss
import com.rudi.audioplayer.ui.theme.skeuEmboss
import com.rudi.audioplayer.ui.theme.calmAberration
import com.rudi.audioplayer.ui.theme.resolveIsDark
import com.rudi.audioplayer.ui.theme.Radius
import com.rudi.audioplayer.ui.theme.rememberIosFlingBehavior
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentThemeIdentity: ThemeIdentity,
    currentThemeMode: ThemeMode,
    onSelectThemeIdentity: (ThemeIdentity) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    lockEnabled: Boolean,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onSetPin: (String) -> Unit,
    onDisableLock: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    shakeToSkipEnabled: Boolean,
    onToggleShakeToSkip: (Boolean) -> Unit,
    radioAutoContinueEnabled: Boolean,
    onToggleRadioAutoContinue: (Boolean) -> Unit,
    floatingBubbleEnabled: Boolean,
    onToggleFloatingBubble: (Boolean) -> Unit,
    silenceSkipEnabled: Boolean,
    onToggleSilenceSkip: (Boolean) -> Unit,
    onInfoMessage: (String) -> Unit,
    onOpenStats: () -> Unit,
    // Gap List #2 (Duplicate Detection). songs = current in-memory library snapshot (same
    // source LibraryScreen/StatsDashboard use), onDeleteSongs reuses MainActivity's existing
    // deleteSongsFromDevice — no new deletion path, just a new manual entry point into it.
    songs: List<Song> = emptyList(),
    onDeleteSongs: (List<Song>) -> Unit = {}
) {
    // Batch 544 — T11 (layar ke-3): HANYA flag ringan yang tahan rotasi (`rememberSaveable`): log
    // diagnostik (read-only), seksi Lanjutan, konfirmasi hapus cache lirik. SENGAJA tetap `remember`:
    // Vault (konten aman), DuplicateFinder (scan berat), UpdateCheck (cek jaringan saat masuk),
    // BackupRestore + SignatureMatcher (state hasil SAF/payload ikut hilang bila sheet dibuka ulang).
    var showSignatureMatcher by remember { mutableStateOf(false) }
    var showDiagnosticLog by rememberSaveable { mutableStateOf(false) }
    var showBackupRestore by remember { mutableStateOf(false) }
    var showDuplicateFinder by remember { mutableStateOf(false) }
    var showVault by remember { mutableStateOf(false) }
    var showAdvancedSettings by rememberSaveable { mutableStateOf(false) }
    var showUpdateCheck by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    // Batch 247 — Lyrics offline-first 4/4b. Pola sama Vault/Duplicate/Backup di file ini:
    // Store dibaca/ditulis LANGSUNG dari sini pakai LocalContext, bukan di-hoist ke
    // MainActivity — fitur "utilitas" mandiri di file ini semuanya begini, beda dari toggle
    // playback-behavior lama (shakeToSkipEnabled dst.) yang state-nya di-hoist dari luar.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lyricsPrefetchEnabled by remember { mutableStateOf(LyricsPrefetchStore(context).isEnabled()) }
    var showClearLyricsCacheConfirm by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Batch 377 — PENDING_IosFlingBehavior.md item 5/14.
        flingBehavior = rememberIosFlingBehavior()
    ) {
        item {
            Text(
                "PENGATURAN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Tampilan & Info",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 20.dp, bottom = 16.dp)
            )
        }

        item {
            Text(
                "Tema",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Setiap tema punya warna, jenis huruf, dan bentuk sudutnya sendiri — dan sekarang " +
                    "tampil otonom di kedua mode, bukan cuma versi gelap.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            // Batch 61 — mode terang/gelap dipisah TOTAL dari identitas tema (dulu Tactile &
            // Skeu terkunci gelap permanen, digabung 1 enum dengan System/Light/Dark). Toggle
            // ini sekarang berlaku sama untuk KETIGA identitas — pindah "Mode Gelap" langsung
            // mengubah ekspresi Tactile/Skeu yang lagi aktif juga, bukan cuma Apple.
            ThemeModeToggleSection(currentThemeMode = currentThemeMode, onSelectThemeMode = onSelectThemeMode)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Identitas Tema",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Warna, tipografi, dan bentuk sudut — mengikuti mode terang/gelap di atas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(ThemeIdentity.entries.toList(), key = { it.name }) { identity ->
            ThemeOptionCard(
                identity = identity,
                isDark = resolveIsDark(currentThemeMode),
                selected = identity == currentThemeIdentity,
                onClick = { onSelectThemeIdentity(identity) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Perilaku Pemutaran",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Batch 557 — tiap baris pengaturan (switch & navigasi) = tile berkedalaman lewat
            // `neuRowTile()` (semua tema). Margin 12dp + padding dalam 8dp = 20dp lama: posisi teks
            // tidak bergeser. Spacer antar tile nav 4dp -> 8dp supaya bayangan tak menimpa tetangga.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .neuRowTile()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Goyang untuk Lagu Berikutnya", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Aktif hanya saat sedang memutar musik",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = shakeToSkipEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleShakeToSkip(it)
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .neuRowTile()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lanjutkan Otomatis (Radio)", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Saat antrean habis, putar lagu lain dari library alih-alih berhenti",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = radioAutoContinueEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleRadioAutoContinue(it)
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .neuRowTile()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mini Player Mengambang (Bubble)", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Kontrol play/pause/next di atas app lain mana pun — butuh izin " +
                            "\"tampil di atas app lain\", diminta lewat pengaturan sistem saat " +
                            "dinyalakan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = floatingBubbleEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleFloatingBubble(it)
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .neuRowTile()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lewati Keheningan Otomatis", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Percepat bagian hening saat lagu diputar (pakai deteksi bawaan " +
                            "Media3, belum ada slider sensitivitas). Bisa memotong intro/outro " +
                            "yang memang senyap secara musikal — coba dulu, matikan lagi kalau " +
                            "terasa mengganggu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = silenceSkipEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleSilenceSkip(it)
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .neuRowTile()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Batch 247 — Lyrics offline-first 4/4b. Switch ke-5 section ini, pola
                    // identik 4 lainnya (title+subtitle+Switch, Spacer 12dp) — beda dari 5
                    // baris "Alat & Utilitas" yg icon+nav-row, konsisten pembagian row-species
                    // yg sudah diaudit Batch 217/218 (switch vs nav-row beda afinitas
                    // interaksi, bukan hal yang perlu disamakan).
                    Text("Prefetch Lirik Saat WiFi", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Unduh lirik 10 lagu berikutnya di antrean otomatis saat tersambung " +
                            "WiFi, supaya sudah tersedia offline duluan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = lyricsPrefetchEnabled,
                    onCheckedChange = { enabled ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lyricsPrefetchEnabled = enabled
                        LyricsPrefetchStore(context).setEnabled(enabled)
                    }
                )
            }
        }

        item {
            // Batch 215 — Settings polish 1/9 (grouping antar section, MICRO_UIUX_AUDIT.md §
            // Settings). 4 baris ini (Statistik/Backup/Duplikat/Vault) SEBELUMNYA masing-masing
            // dibungkus divider+Spacer sendiri TANPA title — tampak seperti 4 "section" kosong
            // nama, beda dari pola section lain di file ini (mis. "Perilaku Pemutaran") yang
            // selalu 1 title menaungi beberapa item terkait. Disatukan 1 title "Alat &
            // Utilitas" menaungi ke-4-nya — 0 logic/navigasi/aksi berubah, murni restrukturisasi
            // visual (title baru + divider antar-item dibuang, ganti Spacer kecil).
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Alat & Utilitas",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .neuPressTile { onOpenStats() }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.QueryStats, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Statistik Dengar", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Total putar, tren mingguan, artis favorit, jam favorit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .neuPressTile { showBackupRestore = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Cadangkan & Pulihkan", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Simpan playlist, favorit, rating & pengaturan ke 1 file lokal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .neuPressTile { showDuplicateFinder = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Deteksi File Duplikat", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Cari lagu/file kembar di library — hapus manual, tidak ada yang otomatis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .neuPressTile { showVault = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Vault Lagu Privat", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Sembunyikan lagu tertentu total dari Beranda/Library, dilindungi PIN sendiri",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            // Batch 247 — Lyrics offline-first 4/4b. Item ke-5 grup "Alat & Utilitas", pola
            // identik 4 lainnya (Spacer 4dp, icon+title+subtitle, row seluruhnya .clickable).
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .neuPressTile { showClearLyricsCacheConfirm = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Hapus Cache Lirik", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Bersihkan semua lirik tersimpan offline — akan diunduh ulang saat lagu " +
                            "diputar lagi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvancedSettings = !showAdvancedSettings }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lanjutan", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Kunci PIN, sidik jari, dan alat developer — nggak wajib disentuh",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Icon(
                    if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showAdvancedSettings) "Tutup Lanjutan" else "Buka Lanjutan",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            if (showAdvancedSettings) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Keamanan",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppLockSection(
                    lockEnabled = lockEnabled,
                    biometricEnabled = biometricEnabled,
                    biometricAvailable = biometricAvailable,
                    onSetPin = onSetPin,
                    onDisableLock = onDisableLock,
                    onToggleBiometric = onToggleBiometric
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Alat Developer",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Bukan untuk penggunaan sehari-hari — dipakai untuk mengecek APK sebelum instal update manual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .neuPressTile { showSignatureMatcher = true }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Cek Signature APK", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .neuPressTile { showDiagnosticLog = true }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Log Diagnostik", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Tentang Aplikasi",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                // Batch 36: sebelumnya "AudioPlayer versi 1.0.254 (build 254)" — angka commit
                // count yang sama muncul dua kali (ekor versionName & build code terpisah),
                // berantakan dan lebih panjang dari perlu. versionName sendiri sudah unik dan
                // strictly increasing (basis git commit count, lihat app/build.gradle.kts),
                // jadi "(build N)" tidak menambah info baru bagi pengguna. Skema penomoran
                // versi (auto dari commit count) TIDAK diubah — cuma ringkas tampilannya.
                "SONIX versi ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Dibuat untuk didengarkan sepenuhnya offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Release Downloader Spec — satu-satunya tempat app ini pernah menyentuh jaringan,
            // dan hanya kalau baris ini ditekan manual (tidak pernah otomatis di background).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .neuPressTile { showUpdateCheck = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.SettingsBackupRestore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Cek Update", style = MaterialTheme.typography.bodyMedium)
                    // Batch 216 — Settings polish 2/9 (title/subtitle row). Baris ini dulu
                    // title-only, beda dari 4 baris "Alat & Utilitas" (Statistik/Backup/
                    // Duplikat/Vault) yang semua title+subtitle. Beda dari "Cek Signature APK"/
                    // "Log Diagnostik" (juga title-only) yang dinaungi 1 deskripsi section
                    // bersama ("Alat Developer"), baris ini BERDIRI SENDIRI tanpa konteks apa
                    // pun di dekatnya — subtitle ditambah, bukan dibuang lagi.
                    Text(
                        "Cek versi APK terbaru dari GitHub Release — satu-satunya koneksi " +
                            "internet di app ini",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showSignatureMatcher) {
        SignatureMatcherSheet(onDismiss = { showSignatureMatcher = false }, onInfoMessage = onInfoMessage)
    }
    if (showDiagnosticLog) {
        DiagnosticLogSheet(onDismiss = { showDiagnosticLog = false }, onInfoMessage = onInfoMessage)
    }
    if (showBackupRestore) {
        BackupRestoreSheet(onDismiss = { showBackupRestore = false }, onInfoMessage = onInfoMessage)
    }
    if (showUpdateCheck) {
        UpdateCheckSheet(onDismiss = { showUpdateCheck = false })
    }

    if (showDuplicateFinder) {
        DuplicateFinderSheet(
            songs = songs,
            onDismiss = { showDuplicateFinder = false },
            onDeleteSongs = onDeleteSongs
        )
    }

    if (showVault) {
        VaultSheet(
            songs = songs,
            onDismiss = { showVault = false }
        )
    }

    if (showClearLyricsCacheConfirm) {
        // Batch 247 — pola identik showDisableLockConfirm di bawah (AppLockSection): AlertDialog
        // konfirmasi buat aksi destruktif, TextButton warna error di confirmButton.
        AlertDialog(
            onDismissRequest = { showClearLyricsCacheConfirm = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Cache Lirik?") },
            text = {
                Text(
                    "Semua lirik tersimpan offline akan dihapus. Lirik akan diunduh ulang " +
                        "otomatis saat lagu terkait diputar lagi (butuh koneksi internet).",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        LyricsRepository(context).clearCache()
                        onInfoMessage("Cache lirik dihapus")
                    }
                    showClearLyricsCacheConfirm = false
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearLyricsCacheConfirm = false }) { Text("Batal") }
            }
        )
    }
}
