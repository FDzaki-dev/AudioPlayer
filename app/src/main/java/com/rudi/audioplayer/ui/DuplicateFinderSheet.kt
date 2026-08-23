package com.rudi.audioplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.DuplicateDetector
import com.rudi.audioplayer.data.Song

/**
 * Gap List #2 — Duplicate Detection UI. Full-height ModalBottomSheet (not a small popup — group
 * lists can get long on a big library) showing two SEPARATE sections built from
 * [DuplicateDetector.findLibraryDuplicates] / [DuplicateDetector.findPhysicalDuplicates] — see
 * that file's KDoc for why the two are distinct rather than merged into one list.
 *
 * Selection + delete is entirely MANUAL and per-song: nothing here is ever auto-deleted (gap doc
 * explicit requirement). The user ticks exactly the copies they want gone, sees an in-app confirm
 * dialog naming the exact count, and only then is [onDeleteSongs] invoked — the same
 * `MainActivity.deleteSongsFromDevice` LibraryScreen already uses, which itself still goes
 * through the OS scoped-storage delete confirmation on Android 10+.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateFinderSheet(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onDeleteSongs: (List<Song>) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val libraryGroups = remember(songs) { DuplicateDetector.findLibraryDuplicates(songs) }
    val physicalGroups = remember(songs) { DuplicateDetector.findPhysicalDuplicates(songs) }

    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showConfirm by remember { mutableStateOf(false) }

    fun toggle(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp)
        ) {
            Text("Deteksi File Duplikat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tidak ada yang dihapus otomatis — pilih manual lagu yang ingin dihapus, lalu konfirmasi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (libraryGroups.isEmpty() && physicalGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "Tidak ditemukan duplikat di library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (libraryGroups.isNotEmpty()) {
                        item {
                            DuplicateSectionHeader(
                                icon = Icons.Default.LibraryMusic,
                                title = "Duplikat Entri Library (${libraryGroups.size} grup)",
                                subtitle = "Judul, artis & durasi sama — bisa jadi 2 file berbeda"
                            )
                        }
                        libraryGroups.forEach { group ->
                            items(group.songs, key = { "lib_${it.id}" }) { song ->
                                DuplicateSongRow(song = song, checked = song.id in selectedIds, onToggle = { toggle(song.id) })
                            }
                            item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }
                        }
                    }
                    if (physicalGroups.isNotEmpty()) {
                        item {
                            DuplicateSectionHeader(
                                icon = Icons.Default.FileCopy,
                                title = "Duplikat File Fisik (${physicalGroups.size} grup)",
                                subtitle = "Ukuran & durasi file sama persis — kemungkinan besar file yang sama"
                            )
                        }
                        physicalGroups.forEach { group ->
                            items(group.songs, key = { "phys_${it.id}" }) { song ->
                                DuplicateSongRow(song = song, checked = song.id in selectedIds, onToggle = { toggle(song.id) })
                            }
                            item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                val deleteInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { if (selectedIds.isNotEmpty()) showConfirm = true },
                    enabled = selectedIds.isNotEmpty(),
                    interactionSource = deleteInteraction,
                    modifier = Modifier.fillMaxWidth().bouncyPress(deleteInteraction)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus ${selectedIds.size} Terpilih")
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showConfirm) {
        val toDelete = songs.filter { it.id in selectedIds }
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Hapus ${toDelete.size} file?") },
            text = { Text("File yang dipilih akan dihapus permanen dari perangkat. Aksi ini tidak bisa dibatalkan.") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteSongs(toDelete)
                    selectedIds = emptySet()
                    showConfirm = false
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun DuplicateSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun DuplicateSongRow(song: Song, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                "${song.artist} · ${song.folderName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1
            )
        }
    }
}
