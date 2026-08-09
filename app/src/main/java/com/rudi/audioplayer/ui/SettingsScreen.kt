package com.rudi.audioplayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.BuildConfig
import com.rudi.audioplayer.ui.theme.AppTheme
import com.rudi.audioplayer.ui.theme.colorsFor
import com.rudi.audioplayer.ui.theme.tactileEmboss
import com.rudi.audioplayer.ui.theme.skeuEmboss
import com.rudi.audioplayer.ui.theme.resolveIsDark
import com.rudi.audioplayer.ui.theme.Radius

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit,
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
    onInfoMessage: (String) -> Unit
) {
    var showSignatureMatcher by remember { mutableStateOf(false) }
    var showDiagnosticLog by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                "Setiap tema punya warna, jenis huruf, dan bentuk sudutnya sendiri — bukan cuma ganti warna.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(AppTheme.entries.toList(), key = { it.name }) { theme ->
            ThemeOptionCard(
                theme = theme,
                selected = theme == currentTheme,
                onClick = { onSelectTheme(theme) }
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
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
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
                modifier = Modifier.padding(horizontal = 20.dp),
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
                        .clickable { showSignatureMatcher = true }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
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
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDiagnosticLog = true }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
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
                "AudioPlayer versi ${BuildConfig.VERSION_NAME}",
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
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showSignatureMatcher) {
        SignatureMatcherSheet(onDismiss = { showSignatureMatcher = false }, onInfoMessage = onInfoMessage)
    }
    if (showDiagnosticLog) {
        DiagnosticLogSheet(onDismiss = { showDiagnosticLog = false }, onInfoMessage = onInfoMessage)
    }
}

@Composable
private fun ThemeOptionCard(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val previewColors = colorsFor(theme, resolveIsDark(theme))
    // Batch 49: the Tactile row in this exact picker is the app's own showcase —
    // it should demonstrate the depth treatment live, not sit flat like every other row.
    // Batch 57: Skeuomorphism Dark Lite gets the same live-showcase treatment via its own
    // skeuEmboss() primitive — both custom "physical panel" identities now demo themselves.
    val isTactilePreview = theme == AppTheme.TACTILE
    val isSkeuPreview = theme == AppTheme.SKEU_DARK_LITE
    val isEmbossPreview = isTactilePreview || isSkeuPreview

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
            .clickable(onClick = onClick),
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
                    theme.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = previewColors.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    theme.description,
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
private fun AppLockSection(
    lockEnabled: Boolean,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onSetPin: (String) -> Unit,
    onDisableLock: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit
) {
    var showSetPinDialog by remember { mutableStateOf(false) }
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
                    if (enabled) showSetPinDialog = true else onDisableLock()
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
}

@Composable
private fun SetPinDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
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
