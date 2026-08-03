package com.rudi.audioplayer.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rudi.audioplayer.data.CustomFolderInfo
import com.rudi.audioplayer.data.LibraryFilterStore
import com.rudi.audioplayer.data.OnboardingHintStore
import com.rudi.audioplayer.data.Playlist
import com.rudi.audioplayer.data.SearchHistoryStore
import com.rudi.audioplayer.data.Song
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
fun LibraryScreen(
    rawSongs: List<Song>,
    loading: Boolean,
    onRescan: () -> Unit,
    favoriteIds: ImmutableSet<Long>,
    onToggleFavorite: (Long) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    playlists: List<Playlist>,
    onCreatePlaylist: (String) -> Playlist,
    onDeletePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onAddSongToPlaylist: (String, Long) -> Boolean,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onMoveSongInPlaylist: (String, Int, Int) -> Unit,
    customFolders: List<CustomFolderInfo>,
    onAddCustomFolder: (Uri) -> Unit,
    onRemoveCustomFolder: (String) -> Unit,
    onDeleteSongs: (List<Song>) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val filterStore = remember { LibraryFilterStore(context) }
    val hintStore = remember(context) { OnboardingHintStore(context) }
    var showLibraryHint by remember { mutableStateOf(!hintStore.hasSeenLibraryHint()) }
    val searchHistoryStore = remember { SearchHistoryStore(context) }
    var searchHistory by remember { mutableStateOf(searchHistoryStore.getHistory()) }
    var selectedTab by remember { mutableStateOf(0) }
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var songForPlaylistDialog by remember { mutableStateOf<Song?>(null) }
    var showFolderManager by remember { mutableStateOf(false) }
    var filterVersion by remember { mutableStateOf(0) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(persistentSetOf<Long>()) }
    var songForBulkPlaylistDialog by remember { mutableStateOf(false) }
    var songsPendingDelete by remember { mutableStateOf<List<Song>>(emptyList()) }

    fun exitSelectionMode() {
        selectionMode = false
        selectedIds = persistentSetOf()
    }

    fun toggleSelect(id: Long) {
        selectedIds = if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
        if (selectedIds.isEmpty()) selectionMode = false
    }

    val songs = remember(rawSongs, filterVersion) { filterStore.apply(rawSongs) }

    val folderSummaries = remember(rawSongs, filterVersion) {
        val excluded = filterStore.getExcludedFolders()
        rawSongs.groupBy { it.folderPath }
            .map { (path, group) ->
                FolderSummary(
                    path = path,
                    name = group.first().folderName,
                    songCount = group.size,
                    excluded = excluded.contains(path)
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val hiddenSongsList = remember(rawSongs, filterVersion) {
        val hiddenIds = filterStore.getHiddenSongIds()
        rawSongs.filter { hiddenIds.contains(it.id) }
    }

    // Normalize searchable fields once per visible-library change. This avoids repeating
    // case-insensitive string normalization for every song on every search keystroke.
    val searchIndex = remember(songs) { LibrarySearchIndex(songs) }
    val filteredSongs = remember(searchIndex, searchQuery) {
        searchIndex.search(searchQuery)
    }

    val playNext: (Song) -> Unit = {
        onPlayNext(it)
        Toast.makeText(context, "Diputar setelah lagu ini", Toast.LENGTH_SHORT).show()
    }
    val addToQueue: (Song) -> Unit = {
        onAddToQueue(it)
        Toast.makeText(context, "Ditambahkan ke antrean", Toast.LENGTH_SHORT).show()
    }
    val addToPlaylist: (Song) -> Unit = { songForPlaylistDialog = it }
    var undoHideIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var undoBarKey by remember { mutableStateOf(0) }
    val hideSong: (Song) -> Unit = {
        filterStore.setSongHidden(it.id, true)
        filterVersion++
        undoHideIds = listOf(it.id)
        undoBarKey++
    }
    val bulkHide: () -> Unit = {
        selectedIds.forEach { id -> filterStore.setSongHidden(id, true) }
        filterVersion++
        undoHideIds = selectedIds.toList()
        undoBarKey++
        exitSelectionMode()
    }
    val undoHide: () -> Unit = {
        undoHideIds.forEach { id -> filterStore.setSongHidden(id, false) }
        undoHideIds = emptyList()
    }
    val bulkAddToPlaylist: () -> Unit = { songForBulkPlaylistDialog = true }
    val bulkDelete: () -> Unit = {
        songsPendingDelete = rawSongs.filter { selectedIds.contains(it.id) }
    }
    val deleteSong: (Song) -> Unit = { songsPendingDelete = listOf(it) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectionMode) {
            SelectionActionBar(
                count = selectedIds.size,
                onClose = { exitSelectionMode() },
                onAddToPlaylist = bulkAddToPlaylist,
                onHide = bulkHide,
                onDelete = bulkDelete
            )
        } else {
            LibraryHeader(
                searchActive = searchActive,
                onToggleSearch = {
                    searchActive = !searchActive
                    if (!searchActive) searchQuery = ""
                },
                onRescan = onRescan,
                onOpenFolderManager = { showFolderManager = true }
            )
        }

        if (searchActive) {
            LibrarySearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClose = { searchActive = false; searchQuery = "" }
            )
        }

        if (!searchActive) {
            LibraryFilterChips(selectedTab = selectedTab, onSelect = { selectedTab = it })
        }

        if (!searchActive && showLibraryHint) {
            FeatureHintBanner(
                text = "Folder, Favorit, dan Playlist sekarang ada di tab \"Lainnya\" biar tampilan depan nggak penuh.",
                onDismiss = {
                    showLibraryHint = false
                    hintStore.markLibraryHintSeen()
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        if (searchActive) {
            if (searchQuery.isBlank()) {
                SearchHistoryView(
                    history = searchHistory,
                    onSelect = { q -> searchQuery = q },
                    onClear = {
                        searchHistoryStore.clear()
                        searchHistory = emptyList()
                    }
                )
            } else {
                SearchResultsView(
                    query = searchQuery,
                    songs = filteredSongs,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = onToggleFavorite,
                    onSongClick = { list, index ->
                        searchHistoryStore.record(searchQuery)
                        searchHistory = searchHistoryStore.getHistory()
                        onSongClick(list, index)
                    },
                    onGroupSelect = { name -> searchQuery = name },
                    onPlayNext = playNext,
                    onAddToQueue = addToQueue,
                    onAddToPlaylist = addToPlaylist,
                    onHideSong = hideSong
                )
            }
        } else {
        when {
            loading -> ShimmerList()
            songs.isEmpty() -> EmptyState(
                title = "Belum ada musik",
                subtitle = "Tambahkan file audio ke penyimpanan perangkat, lalu pindai ulang.",
                actionLabel = "Pindai Ulang",
                onAction = onRescan
            )
            selectedTab == 4 -> {
                val favoriteSongs = filteredSongs.filter { favoriteIds.contains(it.id) }
                if (favoriteSongs.isEmpty()) {
                    EmptyState(
                        title = "Belum ada favorit",
                        subtitle = "Ketuk ikon hati pada lagu untuk menambahkannya ke sini."
                    )
                } else {
                    SongListView(favoriteSongs, favoriteIds, onToggleFavorite, onSongClick, playNext, addToQueue, addToPlaylist, hideSong)
                }
            }
            selectedTab == 5 -> PlaylistTabView(
                allSongs = rawSongs,
                playlists = playlists,
                onSongClick = onSongClick,
                onCreatePlaylist = { name -> onCreatePlaylist(name) },
                onDeletePlaylist = onDeletePlaylist,
                onRenamePlaylist = onRenamePlaylist,
                onRemoveSongFromPlaylist = onRemoveSongFromPlaylist,
                onMoveSongInPlaylist = onMoveSongInPlaylist
            )
            filteredSongs.isEmpty() -> EmptyState(
                title = "Tidak ditemukan",
                subtitle = "Coba kata kunci lain."
            )
            selectedTab == 0 -> SongListView(
                songs = filteredSongs,
                favoriteIds = favoriteIds,
                onFavoriteToggle = onToggleFavorite,
                onSongClick = onSongClick,
                onPlayNext = playNext,
                onAddToQueue = addToQueue,
                onAddToPlaylist = addToPlaylist,
                onHideSong = hideSong,
                onDeleteSong = deleteSong,
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                onToggleSelect = { id -> toggleSelect(id) },
                onEnterSelectionMode = { id -> selectionMode = true; selectedIds = persistentSetOf(id) }
            )
            selectedTab == 1 -> AlbumGridView(
                songs = filteredSongs,
                onSongClick = onSongClick
            )
            selectedTab == 2 -> GroupedListView(
                songs = filteredSongs,
                groupOf = { it.artist },
                favoriteIds = favoriteIds,
                onFavoriteToggle = onToggleFavorite,
                onSongClick = onSongClick,
                onPlayNext = playNext,
                onAddToQueue = addToQueue,
                onAddToPlaylist = addToPlaylist,
                onHideSong = hideSong
            )
            else -> GroupedListView(
                songs = filteredSongs,
                groupOf = { it.folderName },
                favoriteIds = favoriteIds,
                onFavoriteToggle = onToggleFavorite,
                onSongClick = onSongClick,
                onPlayNext = playNext,
                onAddToQueue = addToQueue,
                onAddToPlaylist = addToPlaylist,
                onHideSong = hideSong
            )
        }
        }
    }

    val pendingSong = songForPlaylistDialog
    if (pendingSong != null) {
        AddToPlaylistDialog(
            song = pendingSong,
            playlists = playlists,
            onAddToExisting = { playlist ->
                val added = onAddSongToPlaylist(playlist.id, pendingSong.id)
                if (added) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(
                    context,
                    if (added) "Ditambahkan ke \"${playlist.name}\"" else "Sudah ada di \"${playlist.name}\"",
                    Toast.LENGTH_SHORT
                ).show()
                songForPlaylistDialog = null
            },
            onCreateAndAdd = { name ->
                val playlist = onCreatePlaylist(name)
                onAddSongToPlaylist(playlist.id, pendingSong.id)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Dibuat & ditambahkan ke \"${playlist.name}\"", Toast.LENGTH_SHORT).show()
                songForPlaylistDialog = null
            },
            onDismiss = { songForPlaylistDialog = null }
        )
    }

    if (songForBulkPlaylistDialog && selectedIds.isNotEmpty()) {
        val firstSong = rawSongs.firstOrNull { selectedIds.contains(it.id) }
        if (firstSong != null) {
            AddToPlaylistDialog(
                song = firstSong,
                playlists = playlists,
                onAddToExisting = { playlist ->
                    selectedIds.forEach { id -> onAddSongToPlaylist(playlist.id, id) }
                    Toast.makeText(context, "Ditambahkan ke \"${playlist.name}\"", Toast.LENGTH_SHORT).show()
                    songForBulkPlaylistDialog = false
                    exitSelectionMode()
                },
                onCreateAndAdd = { name ->
                    val playlist = onCreatePlaylist(name)
                    selectedIds.forEach { id -> onAddSongToPlaylist(playlist.id, id) }
                    Toast.makeText(context, "Dibuat & ditambahkan ke \"${playlist.name}\"", Toast.LENGTH_SHORT).show()
                    songForBulkPlaylistDialog = false
                    exitSelectionMode()
                },
                onDismiss = { songForBulkPlaylistDialog = false }
            )
        }
    }

    if (songsPendingDelete.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { songsPendingDelete = emptyList() },
            title = { Text("Hapus dari Perangkat?") },
            text = {
                Text(
                    if (songsPendingDelete.size == 1)
                        "\"${songsPendingDelete.first().title}\" akan dihapus permanen dari penyimpanan HP. Tindakan ini tidak bisa dibatalkan."
                    else
                        "${songsPendingDelete.size} lagu akan dihapus permanen dari penyimpanan HP. Tindakan ini tidak bisa dibatalkan."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSongs(songsPendingDelete)
                    songsPendingDelete = emptyList()
                    exitSelectionMode()
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { songsPendingDelete = emptyList() }) { Text("Batal") }
            }
        )
    }

    if (undoHideIds.isNotEmpty()) {
        LaunchedEffect(undoBarKey) {
            kotlinx.coroutines.delay(4000)
            undoHideIds = emptyList()
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (undoHideIds.size == 1) "1 lagu disembunyikan" else "${undoHideIds.size} lagu disembunyikan",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = undoHide) { Text("Batalkan") }
                }
            }
        }
    }

    if (showFolderManager) {
        val addFolderLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri -> if (uri != null) onAddCustomFolder(uri) }

        FolderManagerSheet(
            folders = folderSummaries,
            hiddenSongs = hiddenSongsList,
            customFolders = customFolders,
            onDismiss = { showFolderManager = false },
            onToggleFolder = { path, excluded ->
                filterStore.setFolderExcluded(path, excluded)
                filterVersion++
            },
            onUnhideSong = { songId ->
                filterStore.setSongHidden(songId, false)
                filterVersion++
            },
            onAddCustomFolder = { addFolderLauncher.launch(null) },
            onRemoveCustomFolder = onRemoveCustomFolder
        )
    }

}

@Composable
private fun AlbumGridView(songs: List<Song>, onSongClick: (List<Song>, Int) -> Unit) {
    var selectedAlbum by remember(songs) { mutableStateOf<String?>(null) }
    val grouped = remember(songs) { songs.groupBy { it.album.ifBlank { "Album Tidak Diketahui" } } }
    val sortedAlbumKeys = remember(grouped) { grouped.keys.sortedBy { it.lowercase() } }

    if (selectedAlbum == null) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sortedAlbumKeys, key = { it }) { album ->
                val albumSongs = grouped[album].orEmpty()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAlbum = album }
                ) {
                    AsyncImage(
                        model = albumArtUri(albumSongs.first().albumId),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(album, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${albumSongs.size} lagu",
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    } else {
        val albumSongs = grouped[selectedAlbum].orEmpty()
        Column {
            TextButton(onClick = { selectedAlbum = null }) { Text("< Kembali ke Album") }
            LazyColumn {
                itemsIndexed(albumSongs, key = { _, song -> song.id }) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(albumSongs, index) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(song.title, maxLines = 1, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(formatDuration(song.duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SelectionActionBar(
    count: Int,
    onClose: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Batal")
        }
        Text(
            "$count dipilih",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onAddToPlaylist) {
            Icon(Icons.Default.QueueMusic, contentDescription = "Tambah ke Playlist")
        }
        IconButton(onClick = onHide) {
            Icon(Icons.Default.VisibilityOff, contentDescription = "Sembunyikan")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteForever, contentDescription = "Hapus dari Perangkat", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun LibraryHeader(
    searchActive: Boolean,
    onToggleSearch: () -> Unit,
    onRescan: () -> Unit,
    onOpenFolderManager: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "LIBRARY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text("Musik Saya", style = MaterialTheme.typography.titleLarge)
        }
        IconButton(onClick = onToggleSearch) {
            Icon(
                if (searchActive) Icons.Default.Close else Icons.Default.Search,
                contentDescription = "Cari"
            )
        }
        IconButton(onClick = onOpenFolderManager) {
            Icon(Icons.Default.Tune, contentDescription = "Kelola folder")
        }
        IconButton(onClick = onRescan) {
            Icon(Icons.Default.Refresh, contentDescription = "Pindai ulang")
        }
    }
}

@Composable
private fun LibrarySearchField(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        singleLine = true,
        placeholder = { Text("Cari judul atau artis...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Tutup pencarian")
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
private fun LibraryFilterChips(selectedTab: Int, onSelect: (Int) -> Unit) {
    val primaryLabels = listOf("Lagu", "Album", "Artis")
    val moreLabels = listOf("Folder", "Favorit", "Playlist") // indices 3, 4, 5
    var showMoreMenu by remember { mutableStateOf(false) }
    val moreSelected = selectedTab in 3..5
    val moreChipLabel = if (moreSelected) moreLabels[selectedTab - 3] else "Lainnya"

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(primaryLabels) { index, label ->
            val selected = selectedTab == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        item {
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (moreSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .clickable { showMoreMenu = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        moreChipLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (moreSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (moreSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                    moreLabels.forEachIndexed { offset, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onSelect(3 + offset)
                                showMoreMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryView(
    history: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    if (history.isEmpty()) {
        EmptyState(
            title = "Cari lagu, album, atau artis",
            subtitle = "Riwayat pencarian kamu akan muncul di sini."
        )
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pencarian Terbaru",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClear) { Text("Hapus") }
        }
        LazyColumn {
            items(history, key = { it }) { query ->
                ListItem(
                    headlineContent = { Text(query) },
                    leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .animateItemPlacement()
                        .clickable { onSelect(query) }
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

/** Search results grouped by type — Artis / Album / Lagu — like big-name music apps, instead of one flat list. */
@Composable
private fun SearchResultsView(
    query: String,
    songs: List<Song>,
    favoriteIds: ImmutableSet<Long>,
    onToggleFavorite: (Long) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onGroupSelect: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onHideSong: (Song) -> Unit
) {
    val matchedSongs = remember(songs, query) {
        songs.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }
    }
    val matchedArtists = remember(songs, query) {
        songs.map { it.artist }.distinct()
            .filter { it.isNotBlank() && it.contains(query, ignoreCase = true) }
            .sorted()
            .take(6)
    }
    val matchedAlbums = remember(songs, query) {
        songs.map { it.album }.distinct()
            .filter { it.isNotBlank() && it.contains(query, ignoreCase = true) }
            .sorted()
            .take(6)
    }

    if (matchedSongs.isEmpty() && matchedArtists.isEmpty() && matchedAlbums.isEmpty()) {
        EmptyState(title = "Tidak ditemukan", subtitle = "Coba kata kunci lain.")
        return
    }

    LazyColumn {
        if (matchedArtists.isNotEmpty()) {
            item { SearchSectionLabel("Artis") }
            items(matchedArtists, key = { it }) { artist ->
                ListItem(
                    headlineContent = { Text(artist) },
                    leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .animateItemPlacement()
                        .clickable { onGroupSelect(artist) }
                        .padding(horizontal = 4.dp)
                )
            }
        }
        if (matchedAlbums.isNotEmpty()) {
            item { SearchSectionLabel("Album") }
            items(matchedAlbums, key = { it }) { album ->
                ListItem(
                    headlineContent = { Text(album) },
                    leadingContent = { Icon(Icons.Default.Album, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .animateItemPlacement()
                        .clickable { onGroupSelect(album) }
                        .padding(horizontal = 4.dp)
                )
            }
        }
        if (matchedSongs.isNotEmpty()) {
            item { SearchSectionLabel("Lagu") }
            itemsIndexed(matchedSongs, key = { _, song -> song.id }) { index, song ->
                SongRow(
                    song = song,
                    isFavorite = favoriteIds.contains(song.id),
                    onFavoriteToggle = { onToggleFavorite(song.id) },
                    onClick = { onSongClick(matchedSongs, index) },
                    onPlayNext = { onPlayNext(song) },
                    onAddToQueue = { onAddToQueue(song) },
                    onAddToPlaylist = { onAddToPlaylist(song) },
                    onHideSong = { onHideSong(song) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
private fun SongListView(
    songs: List<Song>,
    favoriteIds: ImmutableSet<Long>,
    onFavoriteToggle: (Long) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onHideSong: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit = {},
    selectionMode: Boolean = false,
    selectedIds: ImmutableSet<Long> = persistentSetOf(),
    onToggleSelect: (Long) -> Unit = {},
    onEnterSelectionMode: (Long) -> Unit = {}
) {
    LazyColumn {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                song = song,
                isFavorite = favoriteIds.contains(song.id),
                onFavoriteToggle = { onFavoriteToggle(song.id) },
                onClick = { onSongClick(songs, index) },
                onPlayNext = { onPlayNext(song) },
                onAddToQueue = { onAddToQueue(song) },
                onAddToPlaylist = { onAddToPlaylist(song) },
                onHideSong = { onHideSong(song) },
                onDeleteSong = { onDeleteSong(song) },
                selectionMode = selectionMode,
                isSelected = selectedIds.contains(song.id),
                onToggleSelect = { onToggleSelect(song.id) },
                onEnterSelectionMode = { onEnterSelectionMode(song.id) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun GroupedListView(
    songs: List<Song>,
    groupOf: (Song) -> String,
    favoriteIds: ImmutableSet<Long>,
    onFavoriteToggle: (Long) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onHideSong: (Song) -> Unit
) {
    var selectedGroup by remember(songs) { mutableStateOf<String?>(null) }
    val grouped = remember(songs) { songs.groupBy(groupOf) }
    val sortedGroupKeys = remember(grouped) { grouped.keys.sorted() }

    if (selectedGroup == null) {
        LazyColumn {
            items(sortedGroupKeys, key = { it }) { group ->
                ListItem(
                    headlineContent = { Text(group, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("${grouped[group]?.size ?: 0} lagu") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .animateItemPlacement()
                        .clickable { selectedGroup = group }
                        .padding(horizontal = 4.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    } else {
        val groupSongs = grouped[selectedGroup].orEmpty()
        Column {
            TextButton(onClick = { selectedGroup = null }) { Text("< Kembali") }
            LazyColumn {
                itemsIndexed(groupSongs, key = { _, song -> song.id }) { index, song ->
                    SongRow(
                        song = song,
                        isFavorite = favoriteIds.contains(song.id),
                        onFavoriteToggle = { onFavoriteToggle(song.id) },
                        onClick = { onSongClick(groupSongs, index) },
                        onPlayNext = { onPlayNext(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        onHideSong = { onHideSong(song) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongRow(
    song: Song,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onHideSong: () -> Unit,
    onDeleteSong: () -> Unit = {},
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onEnterSelectionMode: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (selectionMode) onToggleSelect() else onClick() },
                    onLongClick = { if (selectionMode) onToggleSelect() else onEnterSelectionMode() }
                )
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                Spacer(modifier = Modifier.width(4.dp))
            }
            AsyncImage(
                model = albumArtUri(song.albumId),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    song.artist,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!selectionMode) {
                Text(
                    formatDuration(song.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Hapus dari favorit" else "Tambah ke favorit",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Putar Berikutnya") },
                leadingIcon = { Icon(Icons.Default.PlaylistPlay, contentDescription = null) },
                onClick = { showMenu = false; onPlayNext() }
            )
            DropdownMenuItem(
                text = { Text("Tambah ke Antrean") },
                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                onClick = { showMenu = false; onAddToQueue() }
            )
            DropdownMenuItem(
                text = { Text("Tambah ke Playlist") },
                leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                onClick = { showMenu = false; onAddToPlaylist() }
            )
            DropdownMenuItem(
                text = { Text("Sembunyikan") },
                leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                onClick = { showMenu = false; onHideSong() }
            )
            DropdownMenuItem(
                text = { Text("Pilih") },
                leadingIcon = { Icon(Icons.Default.CheckCircleOutline, contentDescription = null) },
                onClick = { showMenu = false; onEnterSelectionMode() }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            DropdownMenuItem(
                text = { Text("Hapus dari Perangkat", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = { showMenu = false; onDeleteSong() }
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = base.copy(alpha = 0.5f)
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 200f)
    )
}

@Composable
private fun ShimmerRow() {
    val brush = ShimmerBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(brush)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(11.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
private fun ShimmerList() {
    Column { repeat(8) { ShimmerRow() } }
}
