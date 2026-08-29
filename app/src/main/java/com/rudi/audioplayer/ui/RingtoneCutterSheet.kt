package com.rudi.audioplayer.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.RingtoneCutter
import com.rudi.audioplayer.data.RingtoneEncoder
import com.rudi.audioplayer.data.Song
import com.rudi.audioplayer.ui.theme.Radius
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.ui.theme.isLiquidGlassTheme

/**
 * Roadmap #5 — Ringtone Cutter. MVP disengaja: 2 [Slider] terpisah (awal/akhir) alih-alih 1
 * `RangeSlider` — tidak ada precedent `RangeSlider` di codebase ini (`EqualizerSheet`/
 * `NowPlayingScreen` semua pakai `Slider` tunggal), 2-slider lebih konsisten dengan pola yang
 * sudah terbukti dipakai. Tidak ada preview audio langsung di sheet ini (butuh player terpisah
 * dari sesi putar utama) — user dengar hasil dari file yang sudah tersimpan lewat app player
 * lain, batasan yang sama jujurnya seperti MVP [VaultSheet] (Batch 119).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneCutterSheet(
    song: Song,
    onDismiss: () -> Unit,
    onCut: (Song, RingtoneCutter.TrimRange, RingtoneEncoder.Destination, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    val duration = song.duration.coerceAtLeast(1L)

    var startMs by remember(song.id) { mutableStateOf(0L) }
    var endMs by remember(song.id) {
        mutableStateOf((duration).coerceAtMost(RingtoneCutter.MAX_DURATION_MS))
    }
    var destination by remember { mutableStateOf(RingtoneEncoder.Destination.RINGTONE) }

    val range = remember(startMs, endMs, duration) {
        RingtoneCutter.clampRange(startMs, endMs, duration)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                "Potong Nada Dering",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                song.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "Awal: ${RingtoneCutter.formatTimestamp(range.startMs)}",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = startMs.toFloat(),
                onValueChange = { startMs = it.toLong() },
                valueRange = 0f..duration.toFloat()
            )

            Text(
                "Akhir: ${RingtoneCutter.formatTimestamp(range.endMs)}",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = endMs.toFloat(),
                onValueChange = { endMs = it.toLong() },
                valueRange = 0f..duration.toFloat()
            )

            Text(
                "Durasi potongan: ${RingtoneCutter.formatTimestamp(range.durationMs)} " +
                    "(maks ${RingtoneCutter.MAX_DURATION_MS / 1000}s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(16.dp))
            Text("Simpan sebagai", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DestinationChip(
                    label = "Nada Dering",
                    icon = Icons.Default.RingVolume,
                    selected = destination == RingtoneEncoder.Destination.RINGTONE,
                    modifier = Modifier.weight(1f)
                ) { destination = RingtoneEncoder.Destination.RINGTONE }
                DestinationChip(
                    label = "Notifikasi",
                    icon = Icons.Default.Notifications,
                    selected = destination == RingtoneEncoder.Destination.NOTIFICATION,
                    modifier = Modifier.weight(1f)
                ) { destination = RingtoneEncoder.Destination.NOTIFICATION }
                DestinationChip(
                    label = "Alarm",
                    icon = Icons.Default.Alarm,
                    selected = destination == RingtoneEncoder.Destination.ALARM,
                    modifier = Modifier.weight(1f)
                ) { destination = RingtoneEncoder.Destination.ALARM }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Tersimpan ke penyimpanan lalu bisa dipilih manual di Pengaturan > Suara — " +
                    "aplikasi ini tidak mengubah nada dering sistem secara otomatis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(16.dp))
            val cutInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCut(song, range, destination, song.title)
                },
                enabled = RingtoneCutter.isValid(range, duration),
                interactionSource = cutInteraction,
                modifier = Modifier.fillMaxWidth().bouncyPress(cutInteraction)
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Potong & Simpan")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DestinationChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val chipInteraction = remember { MutableInteractionSource() }
    // Batch 288 — kandidat FilterChip Batch 287, pola sama EqualizerSheet.kt/SmartPlaylistScreen.kt.
    val chipLiquidShape = if (isLiquidGlassTheme()) RoundedCornerShape(Radius.liquidPill) else FilterChipDefaults.shape
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        interactionSource = chipInteraction,
        shape = chipLiquidShape,
        modifier = modifier.bouncyPress(chipInteraction, pressedScale = 0.92f)
    )
}
