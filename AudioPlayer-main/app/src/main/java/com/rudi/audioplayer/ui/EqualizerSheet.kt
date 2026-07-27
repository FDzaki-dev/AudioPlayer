package com.rudi.audioplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.playback.EqualizerController
import com.rudi.audioplayer.playback.EqualizerUiState
import java.util.Locale
import kotlin.math.roundToInt

private val boldPresetOptions = listOf(
    EqualizerController.BoldPreset.FLAT to "Flat",
    EqualizerController.BoldPreset.BASS_BOOST to "Bass+",
    EqualizerController.BoldPreset.TREBLE_BOOST to "Treble+",
    EqualizerController.BoldPreset.VOCAL_BOOST to "Vokal+"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    state: EqualizerUiState,
    onDismiss: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onBandChange: (band: Int, level: Short) -> Unit,
    onPresetSelect: (Int) -> Unit,
    onBoldPresetSelect: (EqualizerController.BoldPreset) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Equalizer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state.enabled,
                    onCheckedChange = onToggleEnabled,
                    enabled = state.supported
                )
            }

            if (state.supported) {
                Text(
                    if (state.enabled) "Aktif — geser slider untuk menyesuaikan" else "Nonaktif — nyalakan atau geser slider untuk mendengar efek",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (!state.supported) {
                Text(
                    "Equalizer tidak didukung di perangkat ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Preset Kuat",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(boldPresetOptions.size) { index ->
                        val (preset, label) = boldPresetOptions[index]
                        FilterChip(
                            selected = state.boldPreset == preset.name,
                            onClick = { onBoldPresetSelect(preset) },
                            label = { Text(label) }
                        )
                    }
                }

                if (state.presets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Preset Bawaan Perangkat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.presets.size) { index ->
                            FilterChip(
                                selected = state.selectedPreset == index,
                                onClick = { onPresetSelect(index) },
                                enabled = state.enabled,
                                label = { Text(state.presets[index]) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                state.bands.forEach { band ->
                    val dbValue = band.levelMillibel / 100f
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                formatFrequency(band.frequencyHz),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                String.format(Locale.getDefault(), "%+.1f dB", dbValue),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = band.levelMillibel.toFloat(),
                            onValueChange = { newValue ->
                                onBandChange(band.index, newValue.roundToInt().toShort())
                            },
                            valueRange = state.minLevel.toFloat()..state.maxLevel.toFloat(),
                            enabled = state.enabled,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun formatFrequency(hz: Int): String =
    if (hz >= 1000) {
        String.format(Locale.getDefault(), "%.1f kHz", hz / 1000f)
    } else {
        "$hz Hz"
    }
