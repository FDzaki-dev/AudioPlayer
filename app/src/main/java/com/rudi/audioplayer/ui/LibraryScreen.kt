package com.rudi.audioplayer.ui

import android.net.Uri
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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.rudi.audioplayer.ui.theme.tactileEmboss
import com.rudi.audioplayer.ui.theme.skeuEmboss
import com.rudi.audioplayer.ui.theme.isTactileTheme
import com.rudi.audioplayer.ui.theme.isSkeuTheme
import com.rudi.audioplayer.ui.theme.isCalmRetroTheme
import com.rudi.audioplayer.ui.theme.isLiquidGlassTheme
import com.rudi.audioplayer.ui.theme.calmScanlines
import com.rudi.audioplayer.ui.theme.Radius
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.CustomFolderInfo
import com.rudi.audioplayer.data.LibraryFilterStore
import com.rudi.audioplayer.data.OnboardingHintStore
import com.rudi.audioplayer.data.Playlist
import com.rudi.audioplayer.data.RatingStore
import com.rudi.audioplayer.data.SearchHistoryStore
import com.rudi.audioplayer.data.SmartPlaylist
import com.rudi.audioplayer.data.Song
import com.rudi.audioplayer.data.VaultStore
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet

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
    smartPlaylists: List<SmartPlaylist>,
    onCreateSmartPlaylist: (SmartPlaylist) -> SmartPlaylist,
    onUpdateSmartPlaylist: (SmartPlaylist) -> Unit,
    onDeleteSmartPlaylist: (String) -> Unit,
    customFolders: List<CustomFolderInfo>,
    onAddCustomFolder: (Uri) -> Unit,
    onRemoveCustomFolder: (String) -> Unit,
    onDeleteSongs: (List<Song>) -> Unit,
    onInfoMessage: (String) -> Unit,
    // Pending item dari audit Batch 163: SongRow di sini sebelumnya 0 indikator "sedang
    // diputar" sama sekali (beda dari QueueSheet yang sudah punya sejak lama). Default null
    // (bukan lupa ditambahkan di call site) supaya kalau ada fixture/preview lain yang masih
    // memanggil LibraryScreen(...) tanpa parameter ini, tetap compile — perilakunya jatuh ke
    // "tidak ada lagu yang di-highlight", sama seperti sebelum batch ini, bukan crash.
    currentSongId: Long? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val filterStore = remember { LibraryFilterStore(context) }
    val vaultStore = remember { VaultStore(context) }
    val ratingStore = remember { RatingStore(context) }
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
    // Shortcut FAB Batch 266 — laporan user (screenshot tab Favorit kosong): satu-satunya cara
    // sebelumnya WAJIB muter ke tab Lagu dulu buat nambah favorit manual.
    var showFavoritePicker by remember { mutableStateOf(false) }

    fun exitSelectionMode() {
        selectionMode = false
        selectedIds = persistentSetOf()
    }

    fun toggleSelect(id: Long) {
        // Batch 272 — user minta eksplisit: selectionMode TIDAK BOLEH auto-exit lagi cuma
        // gara-gara selectedIds balik ke 0 (mis. user long-press 1 lagu lalu iseng
        // deselect lagu itu sendiri tanpa gerak sweep apapun). SATU-SATUNYA jalan keluar dari
        // selectionMode sekarang WAJIB lewat tombol Close eksplisit di SelectionActionBar
        // (`exitSelectionMode()`, `onClose`) — baris `if (selectedIds.isEmpty()) selectionMode
        // = false` yang lama SENGAJA DIHAPUS, bukan lupa.
        selectedIds = if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
    }

    // Roadmap #14 — vaulted songs excluded here the same way as hidden ones; the Vault itself
    // is managed from Settings, so this list won't reflect a vault change made there until this
    // screen is re-entered (remember block re-runs on remount) — same class of staleness the
    // project already accepts for other cross-screen store writes (see Backup/Restore, Batch 115).
    val songs = remember(rawSongs, filterVersion) { vaultStore.apply(filterStore.apply(rawSongs)) }

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

    // Chip options for the Smart Playlist builder's folder filter — same source (folderName,
    // not folderPath) the Folder tab already groups by, so what the user picks here matches
    // what they see there.
    val availableFolderNames = remember(rawSongs) {
        rawSongs.map { it.folderName }.distinct().sorted()
    }

    // Gap List #11 — same precedent as availableFolderNames right above, for the Smart
    // Playlist builder's genre chip picker. mapNotNull drops songs with no genre tag.
    val availableGenreNames = remember(rawSongs) {
        rawSongs.mapNotNull { it.genre }.distinct().sorted()
    }

    // Normalize searchable fields once per visible-library change. This avoids repeating
    // case-insensitive string normalization for every song on every search keystroke.
    val searchIndex = remember(songs) { LibrarySearchIndex(songs) }
    val filteredSongs = remember(searchIndex, searchQuery) {
        searchIndex.search(searchQuery)
    }

    val playNext: (Song) -> Unit = {
        onPlayNext(it)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onInfoMessage("Diputar setelah lagu ini")
    }
    val addToQueue: (Song) -> Unit = {
        onAddToQueue(it)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onInfoMessage("Ditambahkan ke antrean")
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
                    onHideSong = hideSong,
                    currentSongId = currentSongId
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
                Box(modifier = Modifier.fillMaxSize()) {
                    if (favoriteSongs.isEmpty()) {
                        EmptyState(
                            title = "Belum ada favorit",
                            subtitle = "Ketuk ikon hati pada lagu untuk menambahkannya ke sini."
                        )
                    } else {
                        SongListView(
                            songs = favoriteSongs,
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
                            onEnterSelectionMode = { id -> selectionMode = true; selectedIds = persistentSetOf(id) },
                            onSweepSelectRange = { ids -> selectionMode = true; selectedIds = ids.toPersistentSet() },
                            currentSongId = currentSongId
                        )
                    }
                    if (!selectionMode) {
                        FloatingActionButton(
                            onClick = { showFavoritePicker = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(20.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Tambah lagu ke favorit")
                        }
                    }
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
                onMoveSongInPlaylist = onMoveSongInPlaylist,
                onAddSongToPlaylist = onAddSongToPlaylist,
                onInfoMessage = onInfoMessage,
                currentSongId = currentSongId
            )
            selectedTab == 6 -> SmartPlaylistTabView(
                // Same allSongs = rawSongs precedent as the manual Playlist tab right above
                // (tab 5) — hidden/excluded-folder filtering is a Library-tab-only display
                // concern, not applied to either playlist kind.
                allSongs = rawSongs,
                availableFolders = availableFolderNames,
                availableGenres = availableGenreNames,
                smartPlaylists = smartPlaylists,
                ratingOf = { id -> ratingStore.getRating(id) },
                onSongClick = onSongClick,
                onCreate = onCreateSmartPlaylist,
                onUpdate = onUpdateSmartPlaylist,
                onDelete = onDeleteSmartPlaylist,
                currentSongId = currentSongId
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
                onEnterSelectionMode = { id -> selectionMode = true; selectedIds = persistentSetOf(id) },
                onSweepSelectRange = { ids -> selectionMode = true; selectedIds = ids.toPersistentSet() },
                currentSongId = currentSongId
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
                onHideSong = hideSong,
                onDeleteSong = deleteSong,
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                onToggleSelect = { id -> toggleSelect(id) },
                onEnterSelectionMode = { id -> selectionMode = true; selectedIds = persistentSetOf(id) },
                onSweepSelectRange = { ids -> selectionMode = true; selectedIds = ids.toPersistentSet() },
                currentSongId = currentSongId
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
                onHideSong = hideSong,
                onDeleteSong = deleteSong,
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                onToggleSelect = { id -> toggleSelect(id) },
                onEnterSelectionMode = { id -> selectionMode = true; selectedIds = persistentSetOf(id) },
                onSweepSelectRange = { ids -> selectionMode = true; selectedIds = ids.toPersistentSet() },
                currentSongId = currentSongId
            )
        }
        }
    }

    val pendingSong = songForPlaylistDialog
    if (showFavoritePicker) {
        SongPickerSheet(
            title = "Tambah ke Favorit",
            allSongs = rawSongs,
            alreadyAddedIds = favoriteIds,
            onConfirm = { ids ->
                ids.forEach { onToggleFavorite(it) }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onInfoMessage(if (ids.size == 1) "1 lagu ditambahkan ke favorit" else "${ids.size} lagu ditambahkan ke favorit")
            },
            onDismiss = { showFavoritePicker = false }
        )
    }
    if (pendingSong != null) {
        AddToPlaylistDialog(
            song = pendingSong,
            playlists = playlists,
            onAddToExisting = { playlist ->
                val added = onAddSongToPlaylist(playlist.id, pendingSong.id)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onInfoMessage(
                    if (added) "Ditambahkan ke \"${playlist.name}\"" else "Sudah ada di \"${playlist.name}\""
                )
                songForPlaylistDialog = null
            },
            onCreateAndAdd = { name ->
                val playlist = onCreatePlaylist(name)
                onAddSongToPlaylist(playlist.id, pendingSong.id)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onInfoMessage("Dibuat & ditambahkan ke \"${playlist.name}\"")
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
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onInfoMessage("Ditambahkan ke \"${playlist.name}\"")
                    songForBulkPlaylistDialog = false
                    exitSelectionMode()
                },
                onCreateAndAdd = { name ->
                    val playlist = onCreatePlaylist(name)
                    selectedIds.forEach { id -> onAddSongToPlaylist(playlist.id, id) }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onInfoMessage("Dibuat & ditambahkan ke \"${playlist.name}\"")
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
                        "\"${songsPendingDelete.first().title}\" akan dihapus permanen dari penyimpanan HP. Aksi ini tidak bisa dibatalkan."
                    else
                        "${songsPendingDelete.size} lagu akan dihapus permanen dari penyimpanan HP. Aksi ini tidak bisa dibatalkan."
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
            val isTactile = isTactileTheme()
            // Batch 59 — same Tactile-only gap pattern fixed elsewhere this batch: Skeu fell
            // into the Apple-else flat-Surface branch here.
            val isSkeu = isSkeuTheme()
            val isPanelTheme = isTactile || isSkeu
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .then(
                        when {
                            isTactile -> Modifier.tactileEmboss(shape = RoundedCornerShape(Radius.xxl), elevation = 10.dp)
                            isSkeu -> Modifier.skeuEmboss(shape = RoundedCornerShape(Radius.xxl), elevation = 10.dp)
                            else -> Modifier
                        }
                    ),
                shape = RoundedCornerShape(Radius.xxl),
                color = if (isPanelTheme) Color.Transparent else MaterialTheme.colorScheme.surface,
                // Batch 48/49 lesson: don't rely on Surface's own contentColor-from-color
                // fallback when color is Transparent — set it explicitly so this never
                // regresses into invisible text like the LockScreen bug did.
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = if (isPanelTheme) 0.dp else 6.dp,
                shadowElevation = if (isPanelTheme) 0.dp else 6.dp
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
                    TextButton(onClick = undoHide) { Text("Urungkan") }
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
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
                    AlbumArt(
                        artworkUri = albumSongs.first().uri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(Radius.xxxl))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(album, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
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
                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
    // Batch 272 — count BISA 0 sekarang (selectionMode tidak lagi auto-exit saat kosong,
    // lihat toggleSelect()). Aksi massal (Playlist/Hide/Hapus) DISABLE saat count==0 — bukan
    // cuma kosmetik, mencegah bulkHide()/bulkDelete() beneran jalan atas 0 lagu (mis.
    // songsPendingDelete jadi list kosong, berpotensi munculkan dialog konfirmasi "hapus 0
    // lagu" yang aneh). Tombol Close (`onClose`) TETAP SELALU aktif — itu satu-satunya jalan
    // keluar yang sah sekarang, harus tetap bisa dipakai kapan saja.
    val hasSelection = count > 0
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
        IconButton(onClick = onAddToPlaylist, enabled = hasSelection) {
            Icon(Icons.Default.QueueMusic, contentDescription = "Tambah ke Playlist")
        }
        IconButton(onClick = onHide, enabled = hasSelection) {
            Icon(Icons.Default.VisibilityOff, contentDescription = "Sembunyikan")
        }
        IconButton(onClick = onDelete, enabled = hasSelection) {
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
    // Batch 36: field ini sebelumnya tidak set ImeAction sama sekali — hasil pencarian sudah
    // live/reaktif per keystroke, tapi tombol "Selesai/Cari" di keyboard tidak melakukan
    // apa-apa, jadi satu-satunya cara nutup keyboard adalah tombol back atau tap di luar field.
    // ImeAction.Search + hide() di sini murni soal menutup keyboard supaya hasil pencarian
    // kelihatan penuh — tidak mengubah logika pencarian itu sendiri.
    val keyboardController = LocalSoftwareKeyboardController.current
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
        shape = RoundedCornerShape(Radius.xxl),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
    )
}

@Composable
private fun LibraryFilterChips(selectedTab: Int, onSelect: (Int) -> Unit) {
    val primaryLabels = listOf("Lagu", "Album", "Artis")
    val moreLabels = listOf("Folder", "Favorit", "Playlist", "Otomatis") // indices 3, 4, 5, 6
    var showMoreMenu by remember { mutableStateOf(false) }
    val moreSelected = selectedTab in 3..6
    val moreChipLabel = if (moreSelected) moreLabels[selectedTab - 3] else "Lainnya"
    // Batch 287 — Liquid Glass fase 3 sisa langkah: audit pill/chip lebar. Chip filter tab ini
    // (lebar≠tinggi, teks pendek dgn padding, BUKAN tombol persegi/lingkaran) genuinely pill
    // secara visual tapi radius-nya `Radius.xxl` (20dp FIXED) — cuma KEBETULAN terlihat pill
    // di ukuran teks pendek ini, bukan stadium sungguhan yg auto-adaptif ke tinggi berapa pun
    // (`Radius.liquidPill`=999dp dijamin selalu stadium PENUH apa pun metrik font/padding live
    // di device, `xxl` fixed bisa saja tidak pas kalau line-height berbeda). SENGAJA opt-in
    // per-identitas (`isLiquidGlassTheme()`, pola sama seluruh redesign ini) — tema lain TETAP
    // `Radius.xxl` seperti sebelumnya, 0 perubahan visual buat mereka.
    val chipRadius = if (isLiquidGlassTheme()) Radius.liquidPill else Radius.xxl

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(primaryLabels) { index, label ->
            val selected = selectedTab == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(chipRadius))
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
                        .clip(RoundedCornerShape(chipRadius))
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
                        .animateItem()
                        .clickable { onSelect(query) }
                        .padding(horizontal = 20.dp)
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
    onHideSong: (Song) -> Unit,
    currentSongId: Long? = null
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
                        .animateItem()
                        .clickable { onGroupSelect(artist) }
                        .padding(horizontal = 20.dp)
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
                        .animateItem()
                        .clickable { onGroupSelect(album) }
                        .padding(horizontal = 20.dp)
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
                    onHideSong = { onHideSong(song) },
                    isPlaying = song.id == currentSongId
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
    onEnterSelectionMode: (Long) -> Unit = {},
    onSweepSelectRange: (ImmutableSet<Long>) -> Unit = {},
    currentSongId: Long? = null
) {
    val haptic = LocalHapticFeedback.current
    // Batch 70 — root-coordinate bounds of every currently-composed row, refreshed as
    // LazyColumn recycles/composes items. Keyed by index (not song id) since that's what a
    // contiguous "from here to here" range is naturally expressed in.
    val rowBoundsInRoot = remember(songs) { mutableStateMapOf<Int, ClosedFloatingPointRange<Float>>() }
    var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var sweepAnchorIndex by remember { mutableStateOf<Int?>(null) }
    var sweepLastIndex by remember { mutableStateOf<Int?>(null) }
    // Batch 73 — fix "sweep-select kepentok, long-press baru mereset bukan melanjutkan": a
    // second sweep gesture (e.g. user hit the edge of the visible list, lifted their finger,
    // and long-pressed again to keep extending the selection) used to always start from
    // `persistentSetOf(songs[idx].id)` — a brand-new single-item set — discarding whatever was
    // already selected from the PREVIOUS sweep/tap. `selectedIds` is read via
    // rememberUpdatedState because this pointerInput block is only relaunched when `songs`
    // changes, not when `selectedIds` changes — without this, onDragStart/onDrag would close
    // over a stale snapshot of the selection from whenever the gesture detector was last
    // (re)installed, silently undoing selection changes made by taps in between sweeps too.
    val currentSelectedIds by rememberUpdatedState(selectedIds)
    // Snapshot of the selection that existed before the CURRENT sweep gesture began — every
    // sweep-in-progress update below is (this base) UNION (range just swept), so lifting the
    // finger and starting a new long-press-drag extends on top of prior selections instead of
    // replacing them. Captured once per gesture in onDragStart, not read continuously, so that
    // dragging back-and-forth within one continuous gesture still behaves like a plain range
    // select (shrinking the range removes rows again) rather than only ever growing.
    var sweepBaseSelection by remember { mutableStateOf(persistentSetOf<Long>()) }
    // Root cause (user report): a stationary long-press (held, then released with ZERO
    // movement) looked like it did nothing — worse, felt like it actively CANCELLED itself.
    // Sequence: onDragStart below fires normally (Batch 72 already fixed the earlier "long
    // press does nothing AT ALL" bug by removing SongRow's competing onLongClick), selects the
    // row, sets selectionMode=true. But `onDrag` never runs (no movement = no
    // PointerInputChange to consume), so the ORIGINATING down/up touch itself is never
    // consumed by this detector — SongRow's own plain `clickable` (still listening to that same
    // down/up pair for its own click, since `clickable` has no long-press timing of its own,
    // just a press-then-release) sees a perfectly normal, unconsumed tap and fires `onClick`
    // a beat later. Because `selectionMode` is now (correctly) true, SongRow's own
    // `if (selectionMode) onToggleSelect() else onClick()` routes that phantom tap to
    // `onToggleSelect` — which immediately toggles the row BACK OFF. Net visible result: select
    // → instant self-deselect, i.e. nothing. Fix: latch which row id the sweep gesture just
    // touched; the very next click/toggle for THAT id is swallowed once (self-clearing), every
    // other row and every later, genuine tap behaves exactly as before.
    var suppressClickForId by remember { mutableStateOf<Long?>(null) }

    fun indexAt(rootY: Float): Int? = rowBoundsInRoot.entries.firstOrNull { rootY in it.value }?.key

    LazyColumn(
        modifier = Modifier
            .onGloballyPositioned { containerCoordinates = it }
            .pointerInput(songs) {
                // Batch 1 (v263 session) — Pending Queue item 2: user reported sweep-select in
                // tab Lagu as over-sensitive vs iOS's standard feel. Root cause: `indexAt()`
                // flips `sweepLastIndex` the INSTANT the Y coordinate crosses a row's exact
                // pixel boundary — completely normal finger tremor while holding roughly still
                // near a boundary line reads as several rapid crossings, so selection flickered
                // in/out on rows the user never meant to touch. Fix: hysteresisPx — once a row
                // is committed, the touch must travel that much PAST the previous row's boundary
                // (not just 1px past it) before the next row is allowed to commit. Doesn't
                // change fast/deliberate swipes at all (those clear the margin trivially), only
                // damps the tiny-jitter-near-a-boundary case.
                val hysteresisPx = 6.dp.toPx()
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val root = containerCoordinates?.localToRoot(offset) ?: return@detectDragGesturesAfterLongPress
                        val idx = indexAt(root.y) ?: return@detectDragGesturesAfterLongPress
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sweepAnchorIndex = idx
                        sweepLastIndex = idx
                        sweepBaseSelection = currentSelectedIds.toPersistentSet()
                        suppressClickForId = songs[idx].id
                        onSweepSelectRange(sweepBaseSelection.add(songs[idx].id))
                    },
                    onDrag = { change, _ ->
                        val anchor = sweepAnchorIndex ?: return@detectDragGesturesAfterLongPress
                        change.consume()
                        // Real movement confirmed — this is an actual sweep, not a stationary
                        // press, so the anchor row's own click will never fire (SongRow's
                        // `clickable` self-cancels once touch slop is exceeded). Clear the latch
                        // now instead of leaving it set until some unrelated future tap on this
                        // same row, which would otherwise get silently swallowed by mistake.
                        suppressClickForId = null
                        val root = containerCoordinates?.localToRoot(change.position) ?: return@detectDragGesturesAfterLongPress
                        val lastIdx = sweepLastIndex ?: return@detectDragGesturesAfterLongPress
                        val idx = indexAt(root.y) ?: return@detectDragGesturesAfterLongPress
                        if (idx == lastIdx) return@detectDragGesturesAfterLongPress
                        val lastBounds = rowBoundsInRoot[lastIdx]
                        if (lastBounds != null) {
                            val committed = if (idx > lastIdx) root.y > lastBounds.endInclusive + hysteresisPx
                            else root.y < lastBounds.start - hysteresisPx
                            if (!committed) return@detectDragGesturesAfterLongPress
                        }
                        sweepLastIndex = idx
                        val range = minOf(anchor, idx)..maxOf(anchor, idx)
                        val sweptIds = range.map { songs[it].id }
                        onSweepSelectRange(sweepBaseSelection.addAll(sweptIds))
                    },
                    onDragEnd = { sweepAnchorIndex = null; sweepLastIndex = null },
                    // Defensive cleanup only — the stationary-press case is expected to clear
                    // `suppressClickForId` itself via the swallowed click (see wiring below), and
                    // the real-drag case already clears it in `onDrag` above. This just prevents
                    // a leak into some unrelated future tap on the same row in any edge case
                    // where neither of those paths runs (e.g. gesture cancelled by an ancestor
                    // before either fires).
                    onDragCancel = { sweepAnchorIndex = null; sweepLastIndex = null; suppressClickForId = null }
                )
            }
    ) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            // Batch 78 — fix: rowBoundsInRoot only ever got entries WRITTEN (onGloballyPositioned),
            // never REMOVED. Once a row scrolled far enough to leave composition (LazyColumn
            // recycling), its bounds entry stayed in the map forever at its last known (now stale)
            // position — Batch 70 flagged this exact scroll-during-sweep scenario as "belum
            // ditest" without root-causing it. Concretely: user sweeps, lifts finger, scrolls the
            // list normally (unclaimed by detectDragGesturesAfterLongPress since it never reaches
            // long-press threshold), then long-presses again — indexAt() does
            // `entries.firstOrNull { rootY in it.value }` over a map that can contain both live
            // entries (current on-screen positions) AND stale ones (disposed rows' old positions,
            // which now overlap completely different rows after the scroll) — whichever the map
            // happens to hit first wins, so the sweep could silently anchor on/extend through the
            // wrong songs. DisposableEffect removes each row's own entry the moment it leaves
            // composition, so the map only ever holds bounds for rows actually on screen right now.
            DisposableEffect(index) {
                onDispose { rowBoundsInRoot.remove(index) }
            }
            SongRow(
                modifier = Modifier.onGloballyPositioned { coords ->
                    val top = coords.positionInRoot().y
                    rowBoundsInRoot[index] = top..(top + coords.size.height)
                },
                song = song,
                isFavorite = favoriteIds.contains(song.id),
                onFavoriteToggle = { onFavoriteToggle(song.id) },
                onClick = {
                    if (suppressClickForId == song.id) suppressClickForId = null
                    else onSongClick(songs, index)
                },
                onPlayNext = { onPlayNext(song) },
                onAddToQueue = { onAddToQueue(song) },
                onAddToPlaylist = { onAddToPlaylist(song) },
                onHideSong = { onHideSong(song) },
                onDeleteSong = { onDeleteSong(song) },
                selectionMode = selectionMode,
                isSelected = selectedIds.contains(song.id),
                onToggleSelect = {
                    if (suppressClickForId == song.id) suppressClickForId = null
                    else onToggleSelect(song.id)
                },
                onEnterSelectionMode = { onEnterSelectionMode(song.id) },
                isPlaying = song.id == currentSongId
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
    onHideSong: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit = {},
    selectionMode: Boolean = false,
    selectedIds: ImmutableSet<Long> = persistentSetOf(),
    onToggleSelect: (Long) -> Unit = {},
    onEnterSelectionMode: (Long) -> Unit = {},
    onSweepSelectRange: (ImmutableSet<Long>) -> Unit = {},
    currentSongId: Long? = null
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
                        .animateItem()
                        .clickable { selectedGroup = group }
                        .padding(horizontal = 20.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    } else {
        val groupSongs = grouped[selectedGroup].orEmpty()
        Column {
            TextButton(onClick = { selectedGroup = null }) { Text("< Kembali") }
            SongListView(
                songs = groupSongs,
                favoriteIds = favoriteIds,
                onFavoriteToggle = onFavoriteToggle,
                onSongClick = onSongClick,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onHideSong = onHideSong,
                onDeleteSong = onDeleteSong,
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                onToggleSelect = onToggleSelect,
                onEnterSelectionMode = onEnterSelectionMode,
                onSweepSelectRange = onSweepSelectRange,
                currentSongId = currentSongId
            )
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
    onEnterSelectionMode: () -> Unit = {},
    // Pending item Batch 163: sebelumnya SongRow 0 indikator "sedang diputar" sama sekali,
    // beda dari QueueRow yang sudah punya (primary 12% alpha bg + bold title). Default false —
    // 0 caller lama di luar 3 titik yang sudah diupdate (SongListView/GroupedListView/
    // SearchResultsView) yang perlu berubah.
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    // v3 upgrade lanjutan — spread Pilar A (CRT scanlines, Batch 133) dari AlbumArtHero (Now
    // Playing) ke sini: kandidat "Card list lagu" yang spec sebut eksplisit tapi sengaja
    // ditunda batch itu ("gak usah greedy", pola sama presedan aberrasi CTA Batch 129->130-131).
    // SongRow ini 1 titik dipakai ulang di semua tampilan daftar lagu (tab Lagu/GroupedListView/
    // SearchResultsView — 3 call site, grep-confirmed), jadi 1 edit di sini otomatis menjangkau
    // ketiganya, tidak perlu disentuh 1-1.
    val isCalmRetro = isCalmRetroTheme()

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Batch 163 pending-item fix: samakan pola highlight "sedang diputar" dengan
                // `QueueRow` (primary 12% alpha bg) — background dipasang SEBELUM clickable,
                // urutan modifier sama persis QueueRow, supaya ripple clickable tetap kelihatan
                // di atas warna latar ini, bukan ketutup.
                .background(if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                // Batch 72: this used to also carry onLongClick -> onEnterSelectionMode()
                // (Batch 66) — a second, INDEPENDENT long-press recognizer on the exact same
                // touch as SongListView's new sweep-select detectDragGesturesAfterLongPress
                // (Batch 70, wraps the whole LazyColumn). Two unrelated long-press gesture
                // families racing for the same physical touch is why sweep-select "did
                // literally nothing" — combinedClickable's own press/ripple tracking marks the
                // pointer consumed as part of recognizing ITS long click, which cancels the
                // outer sweep detector's awaitLongPressOrCancellation before it can ever fire.
                // Sweep's own onDragStart already reproduces plain "press-and-hold this row"
                // (calls onSweepSelectRange with just that one id when the finger never
                // leaves it), so this isn't lost functionality — "Pilih" in the row's overflow
                // menu (search onEnterSelectionMode() below) remains as the explicit-tap entry
                // point into selection mode.
                .clickable(onClick = { if (selectionMode) onToggleSelect() else onClick() })
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                Spacer(modifier = Modifier.width(4.dp))
            }
            AlbumArt(
                artworkUri = song.uri,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(Radius.xxl))
                    .then(if (isCalmRetro) Modifier.calmScanlines() else Modifier)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPlaying) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = "Sedang diputar",
                            // Batch 229 — Iconography 4/7 (action vs decorative icon), konsisten
                            // dgn fix QueueSheet.kt: badge status murni, bukan `primary`.
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        song.title,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false).basicMarquee()
                    )
                }
                Text(
                    song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                IconButton(onClick = {
                    // Was the only place this toggle fired with zero haptic — Now Playing's
                    // identical favorite button already had it (see below). Same action,
                    // same feedback, regardless of which screen it's tapped from.
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFavoriteToggle()
                }) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Hapus dari favorit" else "Tambah ke favorit",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
                // Batch 265 (v263 session Batch2) — user lapor "gak bisa pilih lagu langsung dari
                // tab favorit/playlist". Root cause SUNGGUHAN, bukan gap per-tab: `showMenu`
                // (DropdownMenu di bawah, isinya termasuk "Pilih" -> onEnterSelectionMode()) TIDAK
                // PERNAH di-set true di MANA PUN di file ini — grep `showMenu` cuma nongol di
                // deklarasi + di dalam DropdownMenu itu sendiri, 0 trigger. Menu ini sepenuhnya
                // unreachable, di SEMUA tab yang lewat SongRow (Lagu/Favorit/Artis/Folder/Search
                // — 1 composable dipakai ulang, grep-confirmed komentar Batch 133 di atas), bukan
                // cuma Favorit/Playlist seperti dugaan awal (Batch 262/264). Playlist tab sendiri
                // TETAP belum tersentuh — itu `PlaylistTabView`, composable lain total, tidak
                // lewat SongRow sama sekali, root cause ini tidak menjangkaunya.
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opsi lagu lainnya"
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
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
        .fillMaxSize()
        .padding(32.dp)
) {
    Column(
        modifier = modifier,
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
            // Batch 275 — kategori "Repeated Components" (POLISH_AUDIT.md): audit Button
            // lintas screen nemu `EmptyState` (dipakai BANYAK tempat) tidak ikut kena
            // `bouncyPress` tap-feedback yang sudah jadi standar app-wide (Motion, Batch 256)
            // — 1 fix di sini otomatis nyebar ke SEMUA pemanggil `EmptyState` dengan CTA.
            val actionInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onAction,
                interactionSource = actionInteraction,
                modifier = Modifier.bouncyPress(actionInteraction)
            ) { Text(actionLabel) }
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(Radius.xxl))
                .background(brush)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(Radius.xs))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(11.dp)
                    .clip(RoundedCornerShape(Radius.xs))
                    .background(brush)
            )
        }
    }
}

@Composable
private fun ShimmerList() {
    Column { repeat(8) { ShimmerRow() } }
}
