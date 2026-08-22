package com.rudi.audioplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
    onMoveSongInPlaylist: (String, Int, Int) -> Unit,
    currentSongId: Long? = null
) {
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

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

            // Pola persis QueueSheet.kt: gesture drag berjalan lintas banyak frame di coroutine
            // sendiri, rememberUpdatedState mencegah callback baca closure basi kalau
            // playlistSongs/onMoveSongInPlaylist berganti identity di tengah drag.
            val currentPlaylistSongs by rememberUpdatedState(playlistSongs)
            val currentOnMoveSongInPlaylist by rememberUpdatedState(onMoveSongInPlaylist)
            val currentPlaylistId by rememberUpdatedState(selectedPlaylist.id)
            var draggingSongId by remember { mutableStateOf<Long?>(null) }
            var dragOffsetPx by remember { mutableStateOf(0f) }
            var rowHeightPx by remember { mutableStateOf(0f) }

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
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Hapus playlist",
                            tint = MaterialTheme.colorScheme.error
                        )
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
                        itemsIndexed(playlistSongs, key = { _, song -> song.id }) { index, song ->
                            val isDragging = song.id == draggingSongId
                            PlaylistSongRow(
                                modifier = Modifier
                                    .then(if (isDragging) Modifier else Modifier.animateItemPlacement())
                                    .onGloballyPositioned { coordinates ->
                                        if (rowHeightPx == 0f) rowHeightPx = coordinates.size.height.toFloat()
                                    }
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragOffsetPx else 0f
                                        shadowElevation = if (isDragging) 10f else 0f
                                    }
                                    .zIndex(if (isDragging) 1f else 0f),
                                song = song,
                                isPlaying = song.id == currentSongId,
                                canMoveUp = index > 0,
                                canMoveDown = index < playlistSongs.lastIndex,
                                onClick = { onSongClick(playlistSongs, index) },
                                onMoveUp = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onMoveSongInPlaylist(selectedPlaylist.id, index, index - 1)
                                },
                                onMoveDown = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onMoveSongInPlaylist(selectedPlaylist.id, index, index + 1)
                                },
                                onRemove = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRemoveSongFromPlaylist(selectedPlaylist.id, song.id)
                                },
                                dragHandleModifier = Modifier.pointerInputPlaylistDragHandle(
                                    songId = song.id,
                                    onDragStart = {
                                        draggingSongId = song.id
                                        dragOffsetPx = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragEnd = {
                                        draggingSongId = null
                                        dragOffsetPx = 0f
                                    },
                                    onDragDelta = { deltaY ->
                                        val h = rowHeightPx
                                        if (h > 0f) {
                                            dragOffsetPx += deltaY
                                            val fromIndex = currentPlaylistSongs.indexOfFirst { it.id == song.id }
                                            if (fromIndex >= 0) {
                                                if (dragOffsetPx > h / 2 && fromIndex < currentPlaylistSongs.lastIndex) {
                                                    currentOnMoveSongInPlaylist(currentPlaylistId, fromIndex, fromIndex + 1)
                                                    dragOffsetPx -= h
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                } else if (dragOffsetPx < -h / 2 && fromIndex > 0) {
                                                    currentOnMoveSongInPlaylist(currentPlaylistId, fromIndex, fromIndex - 1)
                                                    dragOffsetPx += h
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                        }
                                    }
                                )
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

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("Hapus Playlist?") },
                    text = {
                        Text(
                            "\"${selectedPlaylist.name}\" akan dihapus. Lagu di dalamnya tidak " +
                                "terhapus dari perangkat, cuma playlist ini yang hilang. Aksi ini " +
                                "tidak bisa dibatalkan."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            onDeletePlaylist(selectedPlaylist.id)
                            showDeleteConfirm = false
                            selectedPlaylistId = null
                        }) {
                            Text("Hapus", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
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

/**
 * Tahan-lalu-drag di handle khusus, persis pola `QueueSheet.kt`'s `pointerInputDragHandle` —
 * duplikat sengaja (bukan diekstrak shared) karena masing-masing private ke file composable-nya,
 * konsisten cara file ini sudah berdiri sendiri dari QueueSheet.
 */
private fun Modifier.pointerInputPlaylistDragHandle(
    songId: Long,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragDelta: (deltaY: Float) -> Unit
): Modifier = this.then(
    Modifier.pointerInput(songId) {
        detectDragGesturesAfterLongPress(
            onDragStart = { onDragStart() },
            onDragEnd = { onDragEnd() },
            onDragCancel = { onDragEnd() },
            onDrag = { change, dragAmount ->
                change.consume()
                onDragDelta(dragAmount.y)
            }
        )
    }
)

@Composable
private fun PlaylistSongRow(
    song: Song,
    isPlaying: Boolean = false,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val background = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = dragHandleModifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Tahan lalu geser untuk mengurutkan ulang",
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                        Spacer(modifier = Modifier.width(8.dp))
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
