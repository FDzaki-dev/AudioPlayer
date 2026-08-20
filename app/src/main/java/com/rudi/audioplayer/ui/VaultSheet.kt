package com.rudi.audioplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.Song
import com.rudi.audioplayer.data.VaultStore
import kotlinx.coroutines.delay

/**
 * Roadmap item #14 — Vault: songs here are PIN-gated in addition to being excluded from
 * every normal library view (wired at the [VaultStore.apply] call sites in HomeScreen/
 * LibraryScreen). Self-contained (own PIN store, own list), no dependency on
 * [com.rudi.audioplayer.data.AppLockStore] — see [VaultStore]'s KDoc for why the two locks
 * are deliberately independent.
 *
 * **Sengaja MVP, dicatat jujur bukan disembunyikan**: sheet ini murni manajemen keanggotaan
 * vault (tambah/keluarkan song) + gerbang PIN, TIDAK ada tombol putar langsung dari sini —
 * memutar lagu butuh mengeluarkannya dari vault dulu. Scope pemutaran-langsung-dari-vault
 * bisa jadi batch lanjutan kalau diminta; menahannya di sini menjaga sheet ini tidak perlu
 * disambungkan ke seluruh permukaan `MediaController`/`PlayerViewModel` di batch pertamanya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSheet(
    songs: List<Song>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val vaultStore = remember { VaultStore(context) }
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var vaultEnabled by remember { mutableStateOf(vaultStore.isVaultEnabled()) }
    var unlocked by remember { mutableStateOf(false) }
    var vaultedIdsVersion by remember { mutableStateOf(0) }
    var showDisableConfirm by remember { mutableStateOf(false) }
    var showAddPicker by remember { mutableStateOf(false) }

    val vaultedSongs = remember(songs, vaultedIdsVersion) {
        val ids = vaultStore.getVaultedSongIds()
        songs.filter { it.id in ids }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp)
        ) {
            Text("Vault Lagu Privat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Sembunyikan lagu tertentu total dari Beranda/Library, dilindungi PIN terpisah dari Kunci Aplikasi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                !vaultEnabled -> VaultSetupSection(
                    onPinSet = { pin ->
                        vaultStore.setPin(pin)
                        vaultEnabled = true
                        unlocked = true
                    }
                )
                !unlocked -> VaultUnlockSection(
                    vaultStore = vaultStore,
                    onUnlocked = { unlocked = true; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                )
                else -> VaultContentSection(
                    vaultedSongs = vaultedSongs,
                    onRemove = { id ->
                        vaultStore.setSongVaulted(id, false)
                        vaultedIdsVersion++
                    },
                    onAddClick = { showAddPicker = true },
                    onDisableClick = { showDisableConfirm = true }
                )
            }
        }
    }

    if (showAddPicker) {
        VaultAddPickerDialog(
            allSongs = songs,
            vaultedIds = vaultStore.getVaultedSongIds(),
            onAdd = { id ->
                vaultStore.setSongVaulted(id, true)
                vaultedIdsVersion++
            },
            onDismiss = { showAddPicker = false }
        )
    }

    if (showDisableConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            title = { Text("Nonaktifkan Vault?") },
            text = {
                Text(
                    "PIN vault dihapus dan SEMUA ${vaultedSongs.size} lagu di dalamnya kembali " +
                        "tampil normal di Beranda/Library. File lagu itu sendiri tidak pernah " +
                        "disentuh — ini murni membuka status sembunyi-terkunci-nya."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vaultStore.disableVault()
                    vaultEnabled = false
                    unlocked = false
                    vaultedIdsVersion++
                    showDisableConfirm = false
                }) { Text("Nonaktifkan") }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirm = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun VaultSetupSection(onPinSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column {
        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Vault belum diaktifkan. Atur PIN 6 digit untuk mulai memakainya.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
            label = { Text("PIN Vault") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
            label = { Text("Konfirmasi PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(12.dp))
        val activateInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = {
                when {
                    pin.length != 6 -> error = "PIN harus 6 digit"
                    pin != confirmPin -> error = "PIN tidak cocok"
                    else -> onPinSet(pin)
                }
            },
            interactionSource = activateInteraction,
            modifier = Modifier.fillMaxWidth().bouncyPress(activateInteraction)
        ) { Text("Aktifkan Vault") }
    }
}

@Composable
private fun VaultUnlockSection(vaultStore: VaultStore, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lockedUntil by remember { mutableStateOf(vaultStore.lockedOutUntil()) }

    // Re-check every second while locked out so the countdown text stays accurate and the
    // field re-enables itself the instant the lockout actually expires, without needing the
    // user to back out and reopen the sheet.
    LaunchedEffect(lockedUntil) {
        while (lockedUntil != null && System.currentTimeMillis() < lockedUntil!!) {
            delay(1000)
            lockedUntil = vaultStore.lockedOutUntil()
        }
    }

    Column {
        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Masukkan PIN Vault", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
            label = { Text("PIN") },
            singleLine = true,
            enabled = lockedUntil == null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        val until = lockedUntil
        if (until != null) {
            val remainingSec = ((until - System.currentTimeMillis()).coerceAtLeast(0L) / 1000L) + 1
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Terlalu banyak percobaan salah. Coba lagi dalam ${remainingSec}d.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            error?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        val unlockInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = {
                when (val result = vaultStore.verifyPin(pin)) {
                    is VaultStore.PinResult.Success -> onUnlocked()
                    is VaultStore.PinResult.Wrong -> { error = "PIN salah"; pin = "" }
                    is VaultStore.PinResult.LockedOut -> { lockedUntil = result.untilMillis; pin = "" }
                }
            },
            enabled = lockedUntil == null && pin.length == 6,
            interactionSource = unlockInteraction,
            modifier = Modifier.fillMaxWidth().bouncyPress(unlockInteraction)
        ) { Text("Buka") }
    }
}

@Composable
private fun VaultContentSection(
    vaultedSongs: List<Song>,
    onRemove: (Long) -> Unit,
    onAddClick: () -> Unit,
    onDisableClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("${vaultedSongs.size} lagu di vault", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tambah")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (vaultedSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "Belum ada lagu di vault. Tap Tambah untuk pindahkan lagu ke sini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(vaultedSongs, key = { it.id }) { song ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1
                            )
                        }
                        val removeInteraction = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = { onRemove(song.id) },
                            interactionSource = removeInteraction,
                            modifier = Modifier.bouncyPress(removeInteraction, pressedScale = 0.8f)
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Keluarkan dari vault")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onDisableClick, modifier = Modifier.fillMaxWidth()) {
            Text("Nonaktifkan Vault", color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun VaultAddPickerDialog(
    allSongs: List<Song>,
    vaultedIds: Set<Long>,
    onAdd: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val candidates = remember(allSongs, vaultedIds, query) {
        allSongs.filter { it.id !in vaultedIds }
            .filter {
                query.isBlank() ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true)
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah ke Vault") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Cari judul/artis") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (candidates.isEmpty()) {
                    Text(
                        "Tidak ada lagu yang cocok.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    LazyColumn {
                        items(candidates, key = { it.id }) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAdd(song.id) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    Text(
                                        song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1
                                    )
                                }
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Selesai") }
        }
    )
}
