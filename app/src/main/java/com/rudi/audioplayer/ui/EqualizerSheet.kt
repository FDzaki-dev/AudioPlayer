package com.rudi.audioplayer.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rudi.audioplayer.ui.theme.Radius
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.ui.theme.isCalmRetroTheme
import com.rudi.audioplayer.ui.theme.isLiquidGlassTheme
import com.rudi.audioplayer.ui.theme.calmScanlines
import com.rudi.audioplayer.ui.theme.rememberIosFlingBehavior
import com.rudi.audioplayer.playback.EqImportResult
import com.rudi.audioplayer.playback.EqProfileImport
import com.rudi.audioplayer.playback.EqualizerController
import com.rudi.audioplayer.playback.EqualizerUiState
import com.rudi.audioplayer.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
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
    val haptic = LocalHapticFeedback.current
    // Batch 533 — locale dibaca lewat LocalConfiguration (observable: berubah saat ganti bahasa)
    // menggantikan Locale.getDefault() di composable (lint NonObservableLocale).
    val locale = LocalConfiguration.current.locales[0]
    // v3 upgrade lanjutan (Batch 134 -> 135) — spread Pilar A (CRT scanlines) dari
    // AlbumArtHero/SongRow ke "panel kontrol" yang spec sebut eksplisit sebagai target lain.
    // Equalizer = panel kontrol paling literal di app ini (slider band + preset), jadi kandidat
    // pertama giliran ini. .clip(MaterialTheme.shapes.large) dulu SEBELUM .calmScanlines() —
    // frostedGlass()'s background() sendiri sudah "shaped" tapi TIDAK meng-clip children/draw
    // sesudahnya (pelajaran sama seperti "Ambient Light gak bocor" Batch 81), jadi scanline
    // overlay wajib dikurung eksplisit di sini supaya tidak bocor melewati sudut membulat panel.
    val isCalmRetro = isCalmRetroTheme()
    // Batch 288 — Liquid Glass fase 3 sisa langkah: kandidat FilterChip Batch 287 (Material3
    // bawaan, shape default ~8dp kotak-bulat, BUKAN custom shape kayak LibraryFilterChips).
    // Sama opt-in per-identitas: tema lain tetap FilterChipDefaults.shape, 0 perubahan visual.
    val chipLiquidShape = if (isLiquidGlassTheme()) RoundedCornerShape(Radius.liquidPill) else FilterChipDefaults.shape

    // Batch 542 — preset EQ pengguna + impor profil AutoEq. Memakai shared controller (getInstance)
    // langsung supaya 0 perubahan di PlayerViewModel/NowPlayingScreen/MainActivity (protected asset);
    // `state` di atas berasal dari singleton yang SAMA (PlayerViewModel.equalizerState), jadi UI
    // otomatis ikut ter-update. State dialog/pesan pakai rememberSaveable (tahan rotasi).
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { EqualizerController.getInstance(context) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var presetName by rememberSaveable { mutableStateOf("") }
    var infoMessage by rememberSaveable { mutableStateOf("") }
    val bandCount = state.bands.size
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val text = readProfileText(context, uri)
            withContext(Dispatchers.Main) {
                infoMessage = if (text == null) {
                    "Gagal membaca file profil."
                } else {
                    describeImport(controller.importProfile(text), bandCount)
                }
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        // Batch 315 — audit "pola tab serupa" dari Batch 314 (fix sheet "Kontrol Lanjutan"
        // terpotong): sheet ini juga punya Column fixed tanpa verticalScroll/LazyColumn jaring
        // pengaman — prioritas TERTINGGI di antara 5 sheet yang kena pola sama, karena jumlah
        // band EQ variatif per device (makin banyak band, makin tinggi total konten) ditambah 2
        // baris preset di atasnya. Pola fix PERSIS sama Batch 314: kalau konten muat, scroll
        // offset tetap 0 (nol perubahan visual); kalau tidak muat (device banyak band/layar
        // pendek), sekarang bisa digeser, bukan diam-diam ke-clip di band terakhir.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass()
                .then(
                    if (isCalmRetro) Modifier.clip(MaterialTheme.shapes.large).calmScanlines() else Modifier
                )
                .verticalScroll(rememberScrollState())
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
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    flingBehavior = rememberIosFlingBehavior()
                ) {
                    items(boldPresetOptions.size, key = { index -> boldPresetOptions[index].first.name }) { index ->
                        val (preset, label) = boldPresetOptions[index]
                        val chipInteraction = remember { MutableInteractionSource() }
                        FilterChip(
                            selected = state.boldPreset == preset.name,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBoldPresetSelect(preset)
                            },
                            interactionSource = chipInteraction,
                            // Batch 485 — `.animateItem()` di awal chain (pola sama LibraryScreen/
                            // VaultSheet). Key stabil (`preset.name`) sudah ada. Grep app-wide file
                            // ini: 0 hit `isDragging`/`draggable`/`reorder`.
                            modifier = Modifier.animateItem().bouncyPress(chipInteraction, pressedScale = 0.92f),
                            shape = chipLiquidShape,
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
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        flingBehavior = rememberIosFlingBehavior()
                    ) {
                        items(state.presets.size, key = { index -> state.presets[index] }) { index ->
                            val chipInteraction = remember { MutableInteractionSource() }
                            FilterChip(
                                selected = state.selectedPreset == index,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPresetSelect(index)
                                },
                                enabled = state.enabled,
                                interactionSource = chipInteraction,
                                // Batch 485 — `.animateItem()`, sama alasan blok Preset Kuat di
                                // atas. Key stabil (`state.presets[index]`) sudah ada.
                                modifier = Modifier.animateItem().bouncyPress(chipInteraction, pressedScale = 0.92f),
                                shape = chipLiquidShape,
                                label = { Text(state.presets[index]) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Preset Saya",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    flingBehavior = rememberIosFlingBehavior()
                ) {
                    items(state.userPresets.size, key = { index -> state.userPresets[index] }) { index ->
                        val name = state.userPresets[index]
                        val chipInteraction = remember { MutableInteractionSource() }
                        FilterChip(
                            selected = state.selectedUserPreset == name,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                controller.useUserPreset(name)
                            },
                            interactionSource = chipInteraction,
                            modifier = Modifier.animateItem().bouncyPress(chipInteraction, pressedScale = 0.92f),
                            shape = chipLiquidShape,
                            label = { Text(name) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showSaveDialog = true
                            },
                            shape = chipLiquidShape,
                            label = { Text("+ Simpan") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Text("Impor profil AutoEq")
                    }
                    if (state.selectedUserPreset.isNotEmpty()) {
                        TextButton(onClick = { controller.deleteUserPreset(state.selectedUserPreset) }) {
                            Text("Hapus preset")
                        }
                    }
                }
                if (infoMessage.isNotEmpty()) {
                    Text(
                        infoMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
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
                                String.format(locale, "%+.1f dB", dbValue),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        NeuSlider(
                            value = band.levelMillibel.toFloat(),
                            onValueChange = { newValue ->
                                onBandChange(band.index, newValue.roundToInt().toShort())
                            },
                            onValueChangeFinished = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
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

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Simpan preset") },
                text = {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it.take(24) },
                        singleLine = true,
                        label = { Text("Nama preset") }
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = presetName.isNotBlank(),
                        onClick = {
                            val saved = controller.saveUserPreset(presetName)
                            infoMessage = if (saved) {
                                "Preset \"${presetName.trim()}\" tersimpan."
                            } else {
                                "Gagal menyimpan preset (maksimal 20)."
                            }
                            showSaveDialog = false
                            presetName = ""
                        }
                    ) { Text("Simpan") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}

/**
 * Batch 542 — baca teks profil dari SAF Uri, DIPOTONG di [EqProfileImport.MAX_TEXT_CHARS]
 * (guard OOM: file besar tak pernah dimuat utuh). Dipanggil dari Dispatchers.IO.
 */
private fun readProfileText(context: Context, uri: Uri): String? =
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val reader = stream.bufferedReader(Charsets.UTF_8)
            val buffer = CharArray(EqProfileImport.MAX_TEXT_CHARS)
            var total = 0
            while (total < buffer.size) {
                val read = reader.read(buffer, total, buffer.size - total)
                if (read < 0) break
                total += read
            }
            String(buffer, 0, total)
        }
    } catch (e: IOException) {
        AppLogger.e("EqualizerSheet", "Gagal membaca profil EQ", e)
        null
    } catch (e: SecurityException) {
        AppLogger.e("EqualizerSheet", "Akses ditolak membaca profil EQ", e)
        null
    }

private fun describeImport(result: EqImportResult, bandCount: Int): String = when {
    result.usedFilters == 0 ->
        "Tidak ada filter yang didukung di file ini (butuh format AutoEq: PK / LSC / HSC)."
    !result.applied ->
        "Equalizer belum siap — putar lagu dulu, lalu impor ulang."
    result.ignoredFilters > 0 ->
        "Profil diterapkan: ${result.usedFilters} filter dipetakan ke $bandCount band " +
            "(perkiraan), ${result.ignoredFilters} filter dilewati."
    else ->
        "Profil diterapkan: ${result.usedFilters} filter dipetakan ke $bandCount band (perkiraan)."
}

private fun formatFrequency(hz: Int): String =
    if (hz >= 1000) {
        String.format(Locale.getDefault(), "%.1f kHz", hz / 1000f)
    } else {
        "$hz Hz"
    }
