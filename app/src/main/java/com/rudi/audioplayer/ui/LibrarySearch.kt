package com.rudi.audioplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.rudi.audioplayer.ui.theme.isLiquidGlassTheme
import com.rudi.audioplayer.ui.theme.Radius
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.Song
import kotlinx.collections.immutable.ImmutableSet

// Batch 518 — Wave 2 T8b (MOVE-ONLY): LibraryHeader/LibrarySearchField/LibraryFilterChips/SearchHistoryView/
// SearchSectionLabel/SearchResultsView + 2 val label tab dipindah dari LibraryScreen.kt. Badan identik
// karakter-per-karakter; satu-satunya beda = `private` -> `internal` pada 5 fungsi yang dipanggil dari
// LibraryScreen() (SearchSectionLabel + 2 val tetap private: hanya dipakai di file ini). Lihat CHANGELOG Batch 518.

@Composable
internal fun LibraryHeader(
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
internal fun LibrarySearchField(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
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
internal fun LibraryFilterChips(selectedTab: Int, onSelect: (Int) -> Unit) {
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
internal fun SearchHistoryView(
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
internal fun SearchResultsView(
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
