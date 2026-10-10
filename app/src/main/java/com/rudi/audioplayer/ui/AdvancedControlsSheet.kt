package com.rudi.audioplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.util.AppLogger
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Batch 509 — Wave 2 T5 (docs/PENDING_CodeTidyPlan.md): AdvancedControlsSheet,
// AdvancedControlsSectionHeader, AdvancedControlRow dipindah MOVE-ONLY dari NowPlayingScreen.kt
// (baris 1634-1841, snapshot v508). Badan fungsi identik karakter-per-karakter; yang berubah
// hanya visibilitas `private` -> `internal` pada AdvancedControlsSheet (dipanggil
// NowPlayingScreen()). AdvancedControlsSectionHeader & AdvancedControlRow tetap `private` —
// hanya dipakai sheet ini. Paket sama.

/** Houses the controls a casual listener rarely touches mid-song — antrean, lirik, sleep timer,
 * playback speed, equalizer, and the in-app volume attenuation — behind one "Lanjutan" entry
 * point instead of crowding the main Now Playing top bar with equal-weight icons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdvancedControlsSheet(
    sleepTimerRemaining: StateFlow<Long?>,
    playbackSpeed: Float,
    volume: Float,
    onSetVolume: (Float) -> Unit,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenAbRepeatBookmark: () -> Unit,
    onOpenVisualizer: () -> Unit,
    onOpenSongInfoEdit: () -> Unit,
    onOpenRingtoneCutter: () -> Unit,
    // Batch 489 — lihat komentar parameter `onStopPlayback` di NowPlayingScreen (pemanggil).
    onStopPlayback: () -> Unit
) {
    // Batch 354 — collect lokal di sini (sheet ini sendiri sudah jadi batas scope yang pas,
    // sama pola MiniPlayerBar.kt Batch 353 — tidak perlu extract composable baru lagi). Tick
    // sekarang cuma invalidate sheet ini SAAT terbuka (showAdvancedSheet), tidak lagi bocor ke
    // NowPlayingScreen/AppNavHost. Value ini cuma dipakai null-check (Aktif/Nonaktif) di bawah,
    // jadi delegated property `by` langsung aman dipakai (0 kebutuhan smart-cast ke Long).
    val sleepTimerRemainingMs by sleepTimerRemaining.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    // Batch 554 — PROBE diagnostik (0 perubahan visual): sheet ini terlihat berosilasi naik-turun
    // periodik di screen recording user (rim+handle+judul bergerak kaku bersama, ~250 ms/siklus,
    // amplitudo ~24dp & ~48dp). Akar belum terbukti (Batch 553 gagal menghilangkannya) -> catat
    // ke Log Diagnostik (tag `SheetProbe`): offset sheet (tiap perubahan >= 12px), ukuran Column,
    // dan insets status/nav/IME. Dibaca dari Pengaturan > Log Diagnostik setelah sheet dibuka ~10 dtk.
    val probeDensity = LocalDensity.current
    val probeNavBottom = WindowInsets.navigationBars.getBottom(probeDensity)
    val probeTop = WindowInsets.statusBars.getTop(probeDensity)
    val probeIme = WindowInsets.ime.getBottom(probeDensity)
    var probeColumnSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(probeNavBottom, probeTop, probeIme, probeColumnSize) {
        val msg = "insets nav=$probeNavBottom top=$probeTop ime=$probeIme " +
            "column=${probeColumnSize.width}x${probeColumnSize.height}"
        withContext(Dispatchers.IO) { AppLogger.w("SheetProbe", msg) }
    }
    LaunchedEffect(sheetState) {
        var lastOffset = Float.NaN
        snapshotFlow { runCatching { sheetState.requireOffset() }.getOrDefault(Float.NaN) }
            .collect { off ->
                if (!off.isNaN() && (lastOffset.isNaN() || abs(off - lastOffset) >= 12f)) {
                    lastOffset = off
                    val msg = "offset=${off.roundToInt()} target=${sheetState.targetValue} " +
                        "current=${sheetState.currentValue}"
                    withContext(Dispatchers.IO) { AppLogger.w("SheetProbe", msg) }
                }
            }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent
    ) {
        // Batch 314 — laporan user: sheet ini terpotong (baris terakhir, "Potong Nada Dering",
        // tidak terjangkau di layar pendek/font besar). Sama persis root cause & pola jaring
        // pengaman yang sudah dipakai body utama NowPlayingScreen (lihat komentar
        // `verticalScroll` di scaffold utama fungsi ini): 3 seksi + divider + slider volume TIDAK
        // pernah discroll, cuma diam-diam ke-clip di tepi layar begitu total tinggi > tinggi sheet
        // yang tersedia. Kalau konten muat (layar tinggi/gesture-nav), scroll offset tetap 0, nol
        // perubahan visual; kalau tidak muat, sekarang bisa digeser bukan hilang.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Batch 554 — tinggi panel dibuat TETAP (= tinggi maksimum yang diberikan sheet; konten
                // sheet ini memang sudah melebihinya, jadi tampilan sama) agar tinggi sheet — dan
                // anchor Expanded `ModalBottomSheet` — tak lagi bergantung pada tinggi konten/insets.
                .fillMaxHeight()
                .onSizeChanged { probeColumnSize = it }
                // Batch 553 — di dalam ModalBottomSheet bayangan luar tak terlihat (di-clip Surface
                // sheet): dilewati supaya tak ada gambar tambahan per frame saat sheet bergerak.
                .frostedGlass(outerShadow = false)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Kontrol Lanjutan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Batch 312 — sebelumnya 9 baris flat tanpa pengelompokan (permintaan user: "rapikan
            // menu utilitas yang tidak dipisahkan berdasarkan kegunaan umumnya"). Dikelompokkan
            // jadi 3 seksi ala grouped-list iOS (label kecil di atas tiap grup + divider di
            // antaranya, pola sama seperti "Peredam Dalam Aplikasi" yang sudah ada sebelumnya):
            // "Pemutaran" (kontrol JALANNYA putar lagu saat ini), "Audio" (pemrosesan/tampilan
            // sinyal audio), "Lagu" (konten/metadata per-lagu, bukan soal pemutaran real-time).
            AdvancedControlsSectionHeader("Pemutaran")
            AdvancedControlRow(
                icon = Icons.Default.QueueMusic,
                label = "Antrean Putar",
                value = null,
                onClick = onOpenQueue
            )
            AdvancedControlRow(
                icon = Icons.Default.Timer,
                label = "Sleep Timer",
                value = if (sleepTimerRemainingMs != null) "Aktif" else "Nonaktif",
                onClick = onOpenSleepTimer
            )
            AdvancedControlRow(
                icon = Icons.Default.Speed,
                label = "Kecepatan Putar",
                value = "${playbackSpeed}x",
                onClick = onOpenSpeed
            )
            AdvancedControlRow(
                icon = Icons.Default.Repeat,
                label = "Repeat A-B & Bookmark",
                value = null,
                onClick = onOpenAbRepeatBookmark
            )
            // Batch 489 — kontrol Stop eksplisit (QA checklist gap #1). Beda dari swipe-dismiss
            // mini player (MiniPlayerBar, Batch 476/480 — itu "cancel" total: musik berhenti +
            // queue dikosongkan + bisa di-Urungkan): baris ini murni jeda + kembali ke posisi
            // awal lagu yang sama, antrean tetap utuh. 0 dialog konfirmasi — aksinya reversibel
            // sendiri (tinggal tekan Play lagi), beda kelas risiko dari dismiss yang menghapus
            // queue makanya dismiss butuh Undo.
            AdvancedControlRow(
                icon = Icons.Default.Stop,
                label = "Stop Pemutaran",
                value = null,
                onClick = onStopPlayback
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            AdvancedControlsSectionHeader("Audio")
            AdvancedControlRow(
                icon = Icons.Default.Equalizer,
                label = "Equalizer",
                value = null,
                onClick = onOpenEqualizer
            )
            AdvancedControlRow(
                icon = Icons.Default.GraphicEq,
                label = "Visualizer Audio",
                value = null,
                onClick = onOpenVisualizer
            )
            Text(
                "Peredam Dalam Aplikasi (bukan volume HP)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val volumeIcon = when {
                    volume <= 0f -> Icons.Default.VolumeOff
                    volume < 0.5f -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeUp
                }
                Icon(volumeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                NeuSlider(
                    value = volume,
                    onValueChange = onSetVolume,
                    onValueChangeFinished = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            AdvancedControlsSectionHeader("Lagu")
            AdvancedControlRow(
                icon = Icons.Default.Article,
                label = "Lirik",
                value = null,
                onClick = onOpenLyrics
            )
            AdvancedControlRow(
                icon = Icons.Default.Edit,
                label = "Edit Info Lagu",
                value = null,
                onClick = onOpenSongInfoEdit
            )
            AdvancedControlRow(
                icon = Icons.Default.ContentCut,
                label = "Potong Nada Dering",
                value = null,
                onClick = onOpenRingtoneCutter
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/** Batch 312 — label kecil di atas tiap grup "Kontrol Lanjutan" (Pemutaran/Audio/Lagu), gaya
 * sama persis "Peredam Dalam Aplikasi" yang sudah ada sebelumnya (labelSmall + secondary),
 * supaya terasa 1 sistem konsisten, bukan pola baru yang asing di sheet ini. */
@Composable
private fun AdvancedControlsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun AdvancedControlRow(icon: ImageVector, label: String, value: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
