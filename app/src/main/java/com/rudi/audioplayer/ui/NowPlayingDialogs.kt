package com.rudi.audioplayer.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.rudi.audioplayer.ui.theme.Radius

// Batch 508 — Wave 2 T4 (docs/PENDING_CodeTidyPlan.md): SleepTimerDialog, SpeedDialog,
// RatingDialog, TransitionModeOption dipindah MOVE-ONLY dari NowPlayingScreen.kt (baris
// 2250-2530, snapshot v507). Badan fungsi identik karakter-per-karakter; yang berubah hanya
// visibilitas `private` -> `internal` pada 3 fungsi yang dipanggil NowPlayingScreen()
// (TransitionModeOption tetap `private` — hanya dipakai SpeedDialog di file ini). Paket sama.

@Composable
internal fun SleepTimerDialog(
    sleepTimerRemaining: StateFlow<Long?>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    // Batch 354 — collect lokal di sini (dialog ini genuinely menampilkan countdown-nya lewat
    // formatDuration di bawah, batas scope paling pas). Disalin ke `val` biasa (BUKAN dipakai
    // langsung sebagai delegated property `by`) karena Kotlin tidak bisa smart-cast Long? -> Long
    // lewat local delegated property — tanpa penyalinan ini, null-check di bawah yang lalu
    // dioper ke formatDuration(Long) TIDAK akan compile.
    val remainingState by sleepTimerRemaining.collectAsStateWithLifecycle()
    val currentRemainingMs: Long? = remainingState
    val options = listOf(10, 15, 30, 45, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                if (currentRemainingMs != null) {
                    Text(
                        "Aktif — berhenti dalam ${formatDuration(currentRemainingMs)}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                options.forEach { minutes ->
                    TextButton(
                        onClick = { onSelect(minutes); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("$minutes menit")
                    }
                }
            }
        },
        confirmButton = {
            if (currentRemainingMs != null) {
                TextButton(onClick = { onCancelTimer(); onDismiss() }) { Text("Matikan Timer") }
            } else {
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        },
        dismissButton = {
            if (currentRemainingMs != null) {
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        }
    )
}

@Composable
internal fun SpeedDialog(
    currentSpeed: Float,
    crossfadeEnabled: Boolean,
    audiobookModeEnabled: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Float) -> Unit,
    onToggleCrossfade: (Boolean) -> Unit,
    onToggleAudiobookMode: (Boolean) -> Unit
) {
    val options = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan Putar") },
        text = {
            // Batch 318 (laporan user, screenshot) — Column ini beda dari 5 sheet yang sudah
            // diaudit Batch 314-316 (semua ModalBottomSheet): SpeedDialog ini AlertDialog, jadi
            // luput dari audit "pola tab serupa" yang scope-nya cuma ModalBottomSheet. Simptom
            // & root cause PERSIS sama: total tinggi konten (6 opsi Kecepatan + toggle Mode
            // Audiobook + 2 opsi Transisi Antar Lagu dengan subtitle panjang) melebihi tinggi
            // yang dialokasikan Material3 AlertDialog ke slot `text`, baris paling bawah
            // ("Fade Halus" subtitle) diam-diam ke-clip alih-alih bisa digeser.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Kecepatan",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                // Batch 163 (Micro UI/UX kategori #5, Interactive States — selected/active
                // consistency) — dulu di sini TextButton polos + teks "✓" (dan warna teks
                // berubah) buat menandai speed yang aktif, PADAHAL daftar pilihan-tunggal LAIN
                // di dialog yang SAMA persis ini (Transisi Antar Lagu, langsung di bawah lewat
                // TransitionModeOption) sudah pakai widget RadioButton sungguhan. Dua bahasa
                // visual beda utk konsep yang identik (pilih 1 dari beberapa opsi), padahal
                // cuma dipisah 1 Divider — disamakan ke pola RadioButton yang sama.
                options.forEach { speed ->
                    val isSelected = speed == currentSpeed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.md))
                            .selectable(
                                selected = isSelected,
                                onClick = { onSelect(speed) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${speed}x", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // Roadmap #12 (Mode Audiobook/Podcast, Batch 93) — per-song opt-in, scoped to
                // whichever song is loaded when this dialog is open (PlayerViewModel keys the
                // saved state off currentSong.id, not a global setting).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mode Audiobook/Podcast", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Ingat kecepatan & posisi khusus lagu ini, terpisah dari lagu lain",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(checked = audiobookModeEnabled, onCheckedChange = onToggleAudiobookMode)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Transisi Antar Lagu",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Gapless has always been the actual playback engine's default behavior —
                // ExoPlayer decodes a real back-to-back playlist with zero re-buffering
                // between tracks whenever crossfade doesn't touch volume. The only thing
                // that was missing was ever telling the user this exists; before this, "off"
                // was just the crossfade switch's unlabeled resting state.
                TransitionModeOption(
                    title = "Gapless (Murni)",
                    subtitle = "Sambung langsung tanpa jeda atau perubahan volume — persis seperti file aslinya",
                    selected = !crossfadeEnabled,
                    onClick = { onToggleCrossfade(false) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Batch 102 — subtitle diperbarui: sebelum ini cuma volume 1 pemutar yang
                // dilandaikan turun-naik di sekitar titik ganti lagu (jeda senyap singkat tetap
                // ada, cuma disamarkan). Sekarang lagu berikutnya benar-benar mulai main
                // (overlap) SEBELUM lagu ini habis — dua sumber suara sungguhan tumpang tindih,
                // bukan cuma efek volume. Lihat CrossfadeEngine.kt.
                TransitionModeOption(
                    title = "Fade Halus",
                    subtitle = "Lagu berikutnya mulai main sebelum lagu ini habis, saling menumpuk lalu bertukar halus",
                    selected = crossfadeEnabled,
                    onClick = { onToggleCrossfade(true) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

// Batch 360 — pengganti StarRatingRow lama (5 IconButton permanen di layar, dihapus Batch 357).
// Isi 5-bintang & konvensi "tap bintang yang sama = hapus rating" DIPERTAHANKAN 1:1 (persis
// yang direferensikan komentar SmartPlaylistScreen.kt sendiri: "same tap-to-clear convention
// as NowPlayingScreen's rating row") — cuma WADAHnya yang berubah dari row permanen jadi dialog
// on-demand (Opsi 4 PENDING_RatingEntryPoint.md). Warna Star terisi/kosong REUSE pola
// primary/secondary SmartPlaylistScreen.kt (bukan animatedAccent) — dialog ini konteksnya beda
// (per-lagu absolute rating, bukan filter Smart Playlist), tapi sama-sama "pilihan di dalam
// AlertDialog" jadi token warna standar M3 lebih pas daripada warna aksen dinamis per-lagu yang
// dipakai Row entry-point di luar dialog.
@Composable
internal fun RatingDialog(
    currentRating: Int,
    onDismiss: () -> Unit,
    onSetRating: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Beri Rating") },
        text = {
            // Batch 362 — FIX BUG NYATA (laporan user + screenshot): star Row & Text hint
            // "Belum ada rating..." SALING TUMPANG TINDIH (overlap) — kefoto jelas: teks 2 baris
            // & 5 bintang render di posisi (x,y) yang sama, numpuk. ROOT CAUSE: slot `text` di
            // Material3 `AlertDialog` MEWADAHI kontennya pakai `Box` (bukan `Column`) — jadi 2
            // composable sibling langsung (Row lalu Text, TANPA pembungkus) yang dipasang di sini
            // sebelumnya otomatis numpuk di titik origin yang sama alih-alih tersusun ke bawah
            // (gotcha Compose yang cukup umum: slot lambda manapun yang dalamnya nampung LEBIH
            // DARI 1 composable top-level WAJIB dibungkus Column/Row eksplisit sendiri — TIDAK
            // ada Column implisit dari sisi pemanggil). FIX: bungkus semua isi slot `text` dalam
            // 1 `Column` eksplisit — urutan dipertahankan sama seperti niat semula (hint dulu,
            // baru Row bintang di bawahnya, dikasih `Spacer(8.dp)` supaya ada jarak, bukan cuma
            // "kebetulan tidak numpuk lagi" krn Column otomatis susun vertikal).
            Column(modifier = Modifier.fillMaxWidth()) {
                if (currentRating == 0) {
                    Text(
                        "Belum ada rating — ketuk bintang untuk memberi rating",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        val starInteraction = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSetRating(if (currentRating == star) 0 else star)
                            },
                            interactionSource = starInteraction,
                            modifier = Modifier.bouncyPress(starInteraction, pressedScale = 0.75f)
                        ) {
                            Icon(
                                if (star <= currentRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star bintang",
                                tint = if (star <= currentRating) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun TransitionModeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
