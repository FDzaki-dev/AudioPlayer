package com.rudi.audioplayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.ui.theme.Radius
import com.rudi.audioplayer.ui.theme.ThemeIdentity
import com.rudi.audioplayer.ui.theme.ThemeMode
import com.rudi.audioplayer.ui.theme.calmAberration
import com.rudi.audioplayer.ui.theme.colorsFor
import com.rudi.audioplayer.ui.theme.skeuEmboss
import com.rudi.audioplayer.ui.theme.tactileEmboss

// Batch 515 — Wave 2 T7 (docs/PENDING_CodeTidyPlan.md): 4 composable ini dipindah MOVE-ONLY dari
// SettingsScreen.kt (badan fungsi identik karakter-per-karakter; hanya `private` -> `internal`
// supaya SettingsScreen() di file lain, paket sama, tetap bisa memanggilnya).

// Batch 61 — dulu (Batch 60) toggle ini punya cabang `isCustomTheme` yang men-disable "Mode
// Gelap" saat Tactile/Skeu aktif (karena keduanya dulu dark-only). Sekarang KETIGA identitas
// otonom di kedua mode, jadi toggle ini murni soal ThemeMode saja — tidak lagi butuh tahu
// identitas apa yang sedang aktif sama sekali (parameter identity dihapus total).
@Composable
internal fun ThemeModeToggleSection(currentThemeMode: ThemeMode, onSelectThemeMode: (ThemeMode) -> Unit) {
    val followSystem = currentThemeMode == ThemeMode.SYSTEM
    // Preferensi gelap/terang yang "diingat" toggle ini walau lagi disabled (Ikuti Sistem ON).
    val isDarkChecked = currentThemeMode != ThemeMode.LIGHT

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(Radius.xl)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ikuti Sistem", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Otomatis menyesuaikan mode terang/gelap perangkat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = followSystem,
                    onCheckedChange = { checked ->
                        onSelectThemeMode(
                            when {
                                checked -> ThemeMode.SYSTEM
                                isDarkChecked -> ThemeMode.DARK
                                else -> ThemeMode.LIGHT
                            }
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mode Gelap", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        when {
                            followSystem -> "Nonaktif — mengikuti pengaturan sistem"
                            isDarkChecked -> "Aktif — berlaku untuk tema apa pun yang dipilih"
                            else -> "Nonaktif — berlaku untuk tema apa pun yang dipilih"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = isDarkChecked,
                    enabled = !followSystem,
                    onCheckedChange = { checked ->
                        onSelectThemeMode(if (checked) ThemeMode.DARK else ThemeMode.LIGHT)
                    }
                )
            }
        }
    }
}

@Composable
internal fun ThemeOptionCard(identity: ThemeIdentity, isDark: Boolean, selected: Boolean, onClick: () -> Unit) {
    // Batch 61 — dulu preview selalu pakai resolveIsDark(theme) (identitas kustom hardcode
    // gelap). Sekarang isDark datang dari mode aktif (param), jadi preview live INI benar-benar
    // menunjukkan bagaimana identitas tersebut tampil di mode yang lagi dipilih user —
    // demonstrasi langsung bahwa identitasnya sekarang otonom, bukan asumsi statis lagi.
    val previewColors = colorsFor(identity, isDark)
    // Batch 49: the Tactile row in this exact picker is the app's own showcase —
    // it should demonstrate the depth treatment live, not sit flat like every other row.
    // Batch 57: Skeuomorphism Dark Lite gets the same live-showcase treatment via its own
    // skeuEmboss() primitive — both custom "physical panel" identities now demo themselves.
    val isTactilePreview = identity == ThemeIdentity.TACTILE
    val isSkeuPreview = identity == ThemeIdentity.SKEU_DARK_LITE
    val isEmbossPreview = isTactilePreview || isSkeuPreview
    // Batch 131 — gap terakhir dari audit cakupan Calm Retro: Tactile/Skeu sudah live-showcase
    // di baris preview masing-masing (emboss di seluruh Surface), tapi identitas Calm Retro
    // sengaja TIDAK ikut pola itu (Surface-nya tetap flat/opaque, sesuai identitas — lihat Batch
    // 130). Showcase-nya sendiri diterapkan lebih presisi: sama seperti CTA play/pause asli
    // (Batch 129), aberrasi cuma di lingkaran aksen 30dp, bukan seluruh kartu.
    val isCalmRetroPreview = identity == ThemeIdentity.CALM_RETRO

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .then(
                when {
                    isTactilePreview -> Modifier.tactileEmboss(shape = RoundedCornerShape(Radius.xl), elevation = if (selected) 12.dp else 8.dp)
                    isSkeuPreview -> Modifier.skeuEmboss(shape = RoundedCornerShape(Radius.xl), elevation = if (selected) 12.dp else 8.dp)
                    else -> Modifier.clip(RoundedCornerShape(Radius.xl))
                }
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        color = if (isEmbossPreview) Color.Transparent else previewColors.surface,
        // Batch 48/49 lesson: explicit contentColor, never rely on the Transparent-color
        // fallback chain (that's exactly what caused the invisible-text LockScreen bug).
        contentColor = previewColors.onSurface,
        tonalElevation = if (isEmbossPreview) 0.dp else 4.dp,
        shadowElevation = if (isEmbossPreview) 0.dp else if (selected) 6.dp else 0.dp,
        border = if (selected) BorderStroke(2.dp, previewColors.primary) else null,
        shape = RoundedCornerShape(Radius.xl)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live swatch of the theme's own background/surface/accent — the actual
            // colors, not a description of them.
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(previewColors.background),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .then(if (isCalmRetroPreview) Modifier.calmAberration(bias = 2.dp) else Modifier)
                        .clip(CircleShape)
                        .background(previewColors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = previewColors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    identity.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = previewColors.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    identity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = previewColors.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Tema aktif",
                    tint = previewColors.primary
                )
            }
        }
    }
}

@Composable
internal fun AppLockSection(
    lockEnabled: Boolean,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onSetPin: (String) -> Unit,
    onDisableLock: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit
) {
    // Batch 544 — T11: hanya konfirmasi nonaktifkan kunci yang tahan rotasi. `showSetPinDialog` + field
    // PIN (`SetPinDialog`) SENGAJA tetap `remember`: PIN tak boleh masuk Bundle state instance.
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showDisableLockConfirm by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Kunci Aplikasi (PIN)", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (lockEnabled) "Aktif — diminta tiap kali app dibuka" else "Nonaktif",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Switch(
                checked = lockEnabled,
                onCheckedChange = { enabled ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (enabled) showSetPinDialog = true else showDisableLockConfirm = true
                }
            )
        }

        if (lockEnabled) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = { showSetPinDialog = true }) { Text("Ubah PIN") }

            if (biometricAvailable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Buka dengan Sidik Jari", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleBiometric(it)
                        }
                    )
                }
            }
        }
    }

    if (showSetPinDialog) {
        SetPinDialog(
            onConfirm = { pin -> onSetPin(pin); showSetPinDialog = false },
            onDismiss = { showSetPinDialog = false }
        )
    }

    if (showDisableLockConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableLockConfirm = false },
            icon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Nonaktifkan Kunci Aplikasi?") },
            text = {
                Text(
                    "PIN yang tersimpan akan dihapus permanen. Kalau diaktifkan lagi nanti, PIN " +
                        "baru harus dibuat dari awal.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDisableLock()
                    showDisableLockConfirm = false
                }) { Text("Nonaktifkan", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDisableLockConfirm = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
internal fun SetPinDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur PIN") },
        text = {
            Column {
                Text("Masukkan 6 digit PIN baru", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("PIN") },
                    singleLine = true,
                    // Batch 36: sebelumnya field ini polos — PIN kelihatan jelas di layar
                    // sambil diketik, dan keyboard yang muncul QWERTY penuh (bukan numerik),
                    // padahal LockScreen (layar buka app) sudah lama pakai PIN pad custom
                    // dengan dot mask. NumberPassword sekaligus kasih keyboard angka DAN
                    // masking bawaan platform (titik/dot), tanpa perlu VisualTransformation
                    // manual terpisah.
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                    label = { Text("Konfirmasi PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                error?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    pin.length != 6 -> error = "PIN harus 6 digit"
                    pin != confirmPin -> error = "PIN tidak cocok"
                    else -> onConfirm(pin)
                }
            }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
