package com.rudi.audioplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.data.CustomFolderInfo
import com.rudi.audioplayer.data.Song

data class FolderSummary(
    val path: String,
    val name: String,
    val songCount: Int,
    val excluded: Boolean
)

/**
 * Bottom sheet to scope which folders get scanned (e.g. turn off WhatsApp's
 * audio folder), bring back individually hidden songs, and add extra folders
 * via the system's folder picker for audio MediaStore hasn't indexed yet.
 * Purely a display filter for the auto-scanned side — never touches files on disk.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderManagerSheet(
    folders: List<FolderSummary>,
    hiddenSongs: List<Song>,
    customFolders: List<CustomFolderInfo>,
    onDismiss: () -> Unit,
    onToggleFolder: (String, Boolean) -> Unit,
    onUnhideSong: (Long) -> Unit,
    onAddCustomFolder: () -> Unit,
    onRemoveCustomFolder: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    // Berbeda dari hapus-di-antrean/playlist (bisa di-undo instan), izin folder yang sudah
    // dilepas (releasePersistableUriPermission) tidak bisa dikembalikan tanpa user memilih
    // ulang lewat system picker — jadi bukan kandidat pola Undo Snackbar yang sama. Konfirmasi
    // dulu di sini justru lebih jujur daripada tombol "Urungkan" yang sebenarnya tidak akan
    // benar-benar berhasil mengembalikan akses.
    var folderPendingRemoval by remember { mutableStateOf<CustomFolderInfo?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent
    ) {
        Column(modifier = Modifier.fillMaxWidth().frostedGlass().padding(bottom = 24.dp)) {
            Text(
                "Kelola Perpustakaan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Matikan folder yang tidak ingin ditampilkan, misalnya folder audio WhatsApp.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (folders.isEmpty()) {
                EmptyState(
                    title = "Belum ada folder terdeteksi",
                    subtitle = "Folder musik di perangkat kamu akan muncul di sini.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 24.dp)
                )
            }

            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(folders, key = { it.path }) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .animateItemPlacement(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folder.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${folder.songCount} lagu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Switch(
                            checked = !folder.excluded,
                            onCheckedChange = { checked -> onToggleFolder(folder.path, !checked) }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Folder Tambahan", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Pilih folder lewat sistem untuk memindai lagu yang belum terdeteksi otomatis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onAddCustomFolder,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih Folder")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(customFolders, key = { it.uri }) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                folder.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Gap List #5 — izin bisa dicabut dari luar app kapan saja tanpa
                            // pemberitahuan apa pun ke app; sebelumnya lagu folder ini cuma
                            // diam-diam hilang dari library tanpa penjelasan ("kok folder saya
                            // kosong" versi kedua, setelah Batch 16 menutup celah yang serupa
                            // saat izin AWAL gagal). Badge ini bikin sebabnya kelihatan di UI.
                            if (!folder.permissionGranted) {
                                Text(
                                    "Izin dicabut — lagunya tidak lagi terpindai. Hapus lalu pilih ulang.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = { folderPendingRemoval = folder }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus folder tambahan")
                        }
                    }
                }

                if (hiddenSongs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Lagu Disembunyikan",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(hiddenSongs, key = { it.id }) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    song.artist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            TextButton(onClick = { onUnhideSong(song.id) }) {
                                Text("Tampilkan")
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }

    folderPendingRemoval?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderPendingRemoval = null },
            title = { Text("Hapus folder tambahan?") },
            text = {
                Text(
                    "\"${folder.displayName}\" tidak akan dipindai lagi. Untuk menambahkannya " +
                        "kembali, kamu perlu memilihnya ulang lewat pemilih folder sistem."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRemoveCustomFolder(folder.uri)
                    folderPendingRemoval = null
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderPendingRemoval = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
