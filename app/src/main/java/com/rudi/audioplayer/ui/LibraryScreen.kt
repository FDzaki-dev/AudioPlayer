package com.rudi.audioplayer.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.ui.theme.calmScanlines
import com.rudi.audioplayer.ui.theme.Radius
import com.rudi.audioplayer.ui.theme.rememberIosFlingBehavior
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
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
                // Batch 400 — Optimasi Compose: badan besar LibraryScreen (banyak `var` state
                // tak-terkait di scope yang sama — searchHistory, showFolderManager,
                // songForBulkPlaylistDialog, undoBarKey, dst) memaksa SELURUH `when` block ini
                // re-run tiap kali salah satu state itu berubah, termasuk cabang tab Favorit
                // ini kalau sedang aktif — persis kelas bug identityRootBrush (Batch 392) /
                // accent wash NowPlayingScreen (Batch 398), bedanya di sini objeknya List hasil
                // filter, bukan Brush. `remember` dgn key `filteredSongs`+`favoriteIds` (2 input
                // NYATA operasi filter ini) memastikan list favorit cuma dihitung ulang saat
                // salah satunya benar-benar berubah, bukan tiap recomposition state tak-terkait.
                val favoriteSongs = remember(filteredSongs, favoriteIds) {
                    filteredSongs.filter { favoriteIds.contains(it.id) }
                }
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
            // Batch 301 — user melaporkan tab Library masih flat total. Grep `isPanelTheme` di
            // seluruh app nemuin ini SATU-SATUNYA titik yang belum ikut fix Batch 300 (di luar 2
            // gesture badge NowPlayingScreen — transient overlay, di luar scope laporan tab):
            // cabang isTactile/isSkeu sendiri tapi Liquid Glass jatuh ke else generik (Modifier
            // polos + Surface warna solid opaque). Pola identik ContinueListeningCard/
            // StatSectionCard Batch 300 — isLiquidGlass -> .frostedGlass(), Surface color jadi
            // Transparent lewat isPanelTheme di bawah.
            val isLiquidGlass = isLiquidGlassTheme()
            val isPanelTheme = isTactile || isSkeu || isLiquidGlass
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .then(
                        when {
                            isTactile -> Modifier.tactileEmboss(shape = RoundedCornerShape(Radius.xxl), elevation = 10.dp)
                            isSkeu -> Modifier.skeuEmboss(shape = RoundedCornerShape(Radius.xxl), elevation = 10.dp)
                            isLiquidGlass -> Modifier.frostedGlass()
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
