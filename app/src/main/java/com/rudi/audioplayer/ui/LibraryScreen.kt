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

// Batch 413: LibraryFilterChips menerima `selectedTab: Int` sebagai parameter yang dibaca
// langsung di badan fungsi (`selectedTab in 3..6`, `selectedTab == index`) — artinya SELURUH
// badan composable ini re-run tiap kali user ganti tab, termasuk 2 `listOf(...)` literal di
// bawah yang isinya TETAP SAMA (bukan turunan dari `selectedTab`/parameter apa pun). Sebelum
// batch ini keduanya dideklarasi ulang (alokasi List + array baru) di badan fungsi tiap
// recomposition; dipindah jadi top-level val (pola sama `LRC_LINE_REGEX` di
// `ui/lyrics/LyricsView.kt`/`fileStampFormat` `RingtoneEncoder.kt`) supaya dialokasikan SEKALI
// per proses, bukan sekali per tab switch. Zero behavior change — isi, urutan, dan index yang
// dipakai (`LIBRARY_MORE_TAB_LABELS[selectedTab - 3]`) identik persis dengan sebelumnya.
private val LIBRARY_PRIMARY_TAB_LABELS = listOf("Lagu", "Album", "Artis")
private val LIBRARY_MORE_TAB_LABELS = listOf("Folder", "Favorit", "Playlist", "Otomatis") // indices 3, 4, 5, 6

@Composable
private fun LibraryFilterChips(selectedTab: Int, onSelect: (Int) -> Unit) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val moreSelected = selectedTab in 3..6
    val moreChipLabel = if (moreSelected) LIBRARY_MORE_TAB_LABELS[selectedTab - 3] else "Lainnya"
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
        itemsIndexed(LIBRARY_PRIMARY_TAB_LABELS) { index, label ->
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
                    LIBRARY_MORE_TAB_LABELS.forEachIndexed { offset, label ->
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
                    headlineContent = {
                        Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
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
                    headlineContent = {
                        Text(album, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
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
