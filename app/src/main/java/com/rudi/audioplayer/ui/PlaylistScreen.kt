package com.rudi.audioplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.Playlist
import com.rudi.audioplayer.data.Song

/** Playlist tab content: list of playlists, or the detail view of a selected one. */
@Composable
fun PlaylistTabView(
    allSongs: List<Song>,
    playlists: List<Playlist>,
    onSongClick: (List<Song>, Int) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onMoveSongInPlaylist: (String, Int, Int) -> Unit
) {
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val selectedPlaylist = playlists.find { it.id == selectedPlaylistId }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedPlaylist == null) {
            if (playlists.isEmpty()) {
                EmptyState(
                    title = "Belum ada playlist",
                    subtitle = "Ketuk tombol + di kanan bawah, atau tekan-lama lagu di tab Lagu.",
                    actionLabel = "Buat Playlist",
                    onAction = { showCreateDialog = true }
                )
            } else {
                LazyColumn {
                    itemsIndexed(playlists, key = { _, playlist -> playlist.id }) { _, playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name) },
                            supportingContent = { Text("${playlist.songIds.size} lagu") },
                            leadingContent = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            modifier = Modifier.clickable { selectedPlaylistId = playlist.id }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buat playlist baru")
            }
        } else {
            val playlistSongs = remember(selectedPlaylist, allSongs) {
                val songMap = allSongs.associateBy { it.id }
                selectedPlaylist.songIds.mapNotNull { songMap[it] }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { selectedPlaylistId = null }) { Text("< Kembali") }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Ganti nama playlist")
                    }
                    IconButton(onClick = {
                        onDeletePlaylist(selectedPlaylist.id)
                        selectedPlaylistId = null
                    }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus playlist")
                    }
                }
                Text(
                    selectedPlaylist.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                if (playlistSongs.isEmpty()) {
                    EmptyState(
                        title = "Playlist kosong",
                        subtitle = "Tekan-lama lagu di tab Lagu, lalu pilih \"Tambah ke Playlist\"."
                    )
                } else {
                    LazyColumn {
                        itemsIndexed(playlistSongs, key = { index, song -> "${song.id}_$index" }) { index, song ->
                            PlaylistSongRow(
                                song = song,
                                canMoveUp = index > 0,
                                canMoveDown = index < playlistSongs.lastIndex,
                                onClick = { onSongClick(playlistSongs, index) },
                                onMoveUp = { onMoveSongInPlaylist(selectedPlaylist.id, index, index - 1) },
                                onMoveDown = { onMoveSongInPlaylist(selectedPlaylist.id, index, index + 1) },
                                onRemove = { onRemoveSongFromPlaylist(selectedPlaylist.id, song.id) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }

            if (showRenameDialog) {
                TextInputDialog(
                    title = "Ganti Nama Playlist",
                    initialValue = selectedPlaylist.name,
                    confirmLabel = "Simpan",
                    onDismiss = { showRenameDialog = false },
                    onConfirm = { newName ->
                        onRenamePlaylist(selectedPlaylist.id, newName)
                        showRenameDialog = false
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        TextInputDialog(
            title = "Buat Playlist Baru",
            initialValue = "",
            confirmLabel = "Buat",
            placeholder = "Nama playlist",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun PlaylistSongRow(
    song: Song,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Naikkan urutan",
                tint = if (canMoveUp) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Turunkan urutan",
                tint = if (canMoveDown) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Hapus dari playlist", tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

/** Small reusable dialog for entering a single line of text (used for create & rename). */
@Composable
private fun TextInputDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    placeholder: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (value.isNotBlank()) onConfirm(value.trim()) },
                enabled = value.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

/** Dialog shown from a song's long-press menu: pick an existing playlist, or create a new one and add to it. */
@Composable
fun AddToPlaylistDialog(
    song: Song,
    playlists: List<Playlist>,
    onAddToExisting: (Playlist) -> Unit,
    onCreateAndAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateField by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah ke Playlist") },
        text = {
            Column {
                Text(
                    song.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (playlists.isEmpty() && !showCreateField) {
                    Text(
                        "Belum ada playlist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        itemsIndexed(playlists, key = { _, playlist -> playlist.id }) { _, playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                supportingContent = { Text("${playlist.songIds.size} lagu") },
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                modifier = Modifier.clickable { onAddToExisting(playlist) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (showCreateField) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("Nama playlist baru") },
                        singleLine = true
                    )
                } else {
                    TextButton(onClick = { showCreateField = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buat Playlist Baru")
                    }
                }
            }
        },
        confirmButton = {
            if (showCreateField) {
                TextButton(
                    onClick = { if (newName.isNotBlank()) onCreateAndAdd(newName.trim()) },
                    enabled = newName.isNotBlank()
                ) { Text("Buat & Tambah") }
            } else {
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        },
        dismissButton = {
            if (showCreateField) {
                TextButton(onClick = { showCreateField = false }) { Text("Batal") }
            }
        }
    )
}
