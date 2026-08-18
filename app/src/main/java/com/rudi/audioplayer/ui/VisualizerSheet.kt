package com.rudi.audioplayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.ui.theme.isCalmRetroTheme
import com.rudi.audioplayer.ui.theme.calmScanlines

/** Roadmap #9 (Visualizer Audio, ROADMAP_15_FITUR_OFFLINE.md), Batch 92. Same shell pattern as
 * EqualizerSheet.kt (header row + Switch, frostedGlass sheet), plus explicit permission education
 * — RECORD_AUDIO is a scary-sounding permission for what is, in practice, a purely local visual
 * effect on this app's own already-playing audio, so the "why" needs to be right there instead of
 * left to the system dialog's generic wording alone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerSheet(
    enabled: Boolean,
    supported: Boolean,
    permissionGranted: Boolean,
    bars: FloatArray,
    accentColor: Color,
    onDismiss: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onRequestPermission: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // v3 upgrade lanjutan (Batch 134 -> 135) — panel kontrol kedua yang dapat Pilar A, pola
    // identik EqualizerSheet.kt (shell sama sejak Batch 92): clip(shapes.large) dulu SEBELUM
    // calmScanlines() supaya overlay terkurung di dalam sudut membulat panel, bukan bocor ke
    // luar (frostedGlass()'s background() tidak meng-clip children/draw sesudahnya).
    val isCalmRetro = isCalmRetroTheme()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass()
                .then(
                    if (isCalmRetro) Modifier.clip(MaterialTheme.shapes.large).calmScanlines() else Modifier
                )
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Visualizer Audio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = enabled && permissionGranted,
                    onCheckedChange = { checked ->
                        // Requesting permission is async (system dialog) — don't flip the switch
                        // here. MainActivity's launcher calls back into setVisualizerEnabled(true)
                        // itself once the user actually grants it (see visualizerPermissionLauncher).
                        if (checked && !permissionGranted) onRequestPermission() else onToggleEnabled(checked)
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            when {
                !permissionGranted -> Text(
                    "Butuh izin Mikrofon untuk aktif — ini BUKAN untuk merekam suara. Sistem " +
                        "Android mewajibkan izin ini untuk fitur Visualizer apa pun, termasuk yang " +
                        "cuma membaca sinyal lagu yang diputar aplikasi ini sendiri. Tidak ada " +
                        "suara yang direkam, disimpan, atau dikirim ke mana pun.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                !supported -> Text(
                    "Visualizer tidak didukung di perangkat ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                !enabled -> Text(
                    "Nonaktif — nyalakan untuk melihat animasi spektrum mengikuti lagu yang diputar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                else -> Text(
                    "Aktif — spektrum di bawah bergerak mengikuti lagu yang sedang diputar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (enabled && permissionGranted && supported) {
                SpectrumBars(
                    bars = bars,
                    color = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }
    }
}

/** Minimal Canvas bar chart — no gridline/axis/text, same restrained-scope precedent as
 * WeeklyTrendChart (StatsDashboardScreen.kt, Batch 90's first Canvas chart in this codebase):
 * fewer moving parts to get visually wrong without a compiler/emulator available to verify.
 * Redraws every time [bars] changes (AudioVisualizerController pushes a new FloatArray per
 * captured frame, ~15fps — see its TARGET_CAPTURE_RATE_MILLIHZ). */
@Composable
private fun SpectrumBars(bars: FloatArray, color: Color, modifier: Modifier = Modifier) {
    val trackColor = color.copy(alpha = 0.15f)
    Canvas(modifier = modifier) {
        if (bars.isEmpty()) return@Canvas
        val gapFraction = 0.25f
        val barWidth = size.width / (bars.size + (bars.size - 1) * gapFraction)
        val gap = barWidth * gapFraction
        val cornerRadius = CornerRadius(barWidth / 3f)
        bars.forEachIndexed { index, magnitude ->
            val left = index * (barWidth + gap)
            val barHeight = (size.height * magnitude.coerceIn(0f, 1f)).coerceAtLeast(4f)
            // Faint full-height track first, so silent bars are still visible as a slot instead
            // of empty space — same touch WeeklyTrendChart already uses for zero-play days.
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = cornerRadius
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}
