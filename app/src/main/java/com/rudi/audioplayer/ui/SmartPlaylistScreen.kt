package com.rudi.audioplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.SmartPlaylist
import com.rudi.audioplayer.data.SmartPlaylistEngine
import com.rudi.audioplayer.data.Song

/**
 * Smart Playlist tab content: list of rule-based playlists (with live match count), or the
 * computed song list of a selected one. Mirrors [PlaylistTabView]'s list/detail structure —
 * the difference is entirely in what "the songs" means: resolved live by [SmartPlaylistEngine]
 * every recomposition instead of a stored, user-ordered ID list.
 */
@Composable
fun SmartPlaylistTabView(
    allSongs: List<Song>,
    availableFolders: List<String>,
    // Gap List #11 — same "distinct values seen in the current library" precedent as
    // availableFolders right above (computed once by the caller from song.genre).
    availableGenres: List<String>,
    smartPlaylists: List<SmartPlaylist>,
    ratingOf: (Long) -> Int,
    onSongClick: (List<Song>, Int) -> Unit,
    onCreate: (SmartPlaylist) -> SmartPlaylist,
    onUpdate: (SmartPlaylist) -> Unit,
    onDelete: (String) -> Unit,
    currentSongId: Long? = null
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showBuilder by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<SmartPlaylist?>(null) }
    val haptic = LocalHapticFeedback.current

    val selected = smartPlaylists.find { it.id == selectedId }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selected == null) {
            if (smartPlaylists.isEmpty()) {
                EmptyState(
                    title = "Belum ada playlist otomatis",
                    subtitle = "Buat aturan sekali (folder, durasi, rating, tahun, kata kunci) — lagu baru yang cocok otomatis ikut masuk, tidak perlu isi manual.",
                    actionLabel = "Buat Playlist Otomatis",
                    onAction = { editingPlaylist = null; showBuilder = true }
                )
            } else {
                LazyColumn {
                    itemsIndexed(smartPlaylists, key = { _, p -> p.id }) { _, playlist ->
                        val matchCount = remember(playlist, allSongs) {
                            SmartPlaylistEngine.resolve(playlist, allSongs, ratingOf).size
                        }
                        ListItem(
                            headlineContent = { Text(playlist.name) },
                            supportingContent = { Text("$matchCount lagu cocok") },
                            leadingContent = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { selectedId = playlist.id }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            FloatingActionButton(
                onClick = { editingPlaylist = null; showBuilder = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Buat playlist otomatis baru")
            }
        } else {
            val matchedSongs = remember(selected, allSongs) {
                SmartPlaylistEngine.resolve(selected, allSongs, ratingOf)
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { selectedId = null }) { Text("< Kembali") }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { editingPlaylist = selected; showBuilder = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Ubah aturan")
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete(selected.id)
                        selectedId = null
                    }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus playlist otomatis")
                    }
                }
                Text(
                    selected.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Text(
                    "${matchedSongs.size} lagu cocok — mengikuti aturan, otomatis berubah kalau ada lagu baru yang cocok",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                if (matchedSongs.isEmpty()) {
                    EmptyState(
                        title = "Tidak ada lagu yang cocok",
                        subtitle = "Longgarkan aturan lewat tombol pensil di atas."
                    )
                } else {
                    LazyColumn {
                        itemsIndexed(matchedSongs, key = { _, song -> song.id }) { index, song ->
                            val isPlaying = song.id == currentSongId
                            val background = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                            ListItem(
                                headlineContent = {
                                    Text(
                                        song.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        song.artist,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = background),
                                modifier = Modifier.clickable { onSongClick(matchedSongs, index) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showBuilder) {
        SmartPlaylistBuilderSheet(
            initial = editingPlaylist,
            availableFolders = availableFolders,
            availableGenres = availableGenres,
            onDismiss = { showBuilder = false },
            onSave = { draft ->
                if (editingPlaylist == null) {
                    val created = onCreate(draft)
                    selectedId = created.id
                } else {
                    onUpdate(draft)
                    selectedId = draft.id
                }
                showBuilder = false
            }
        )
    }
}

/**
 * Bottom sheet to create or edit a [SmartPlaylist]'s criteria. Duration/year fields are plain
 * numeric text fields (minutes / 4-digit year) rather than a custom slider — sliders need a
 * custom thumb/track to fit the app's design language (see README "Belum selesai") and are
 * flagged there as higher-risk without a compiler to verify the drag-gesture code; a text field
 * is the safe, already-proven pattern ([SetPinDialog], search field) for a first version of this
 * feature.
 */
/** Counts how many independent criteria a draft has set, for the sheet's live "N aturan aktif"
 *  hint — a folder set counts once regardless of how many folders are picked. */
private fun activeCriteriaCount(playlist: SmartPlaylist): Int {
    var count = 0
    if (playlist.folderNames.isNotEmpty()) count++
    if (playlist.minDurationMs != null || playlist.maxDurationMs != null) count++
    if (playlist.minRating > 0) count++
    if (playlist.minYear != null || playlist.maxYear != null) count++
    if (playlist.keyword.isNotBlank()) count++
    if (!playlist.genre.isNullOrBlank()) count++
    return count
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartPlaylistBuilderSheet(
    initial: SmartPlaylist?,
    availableFolders: List<String>,
    availableGenres: List<String>,
    onDismiss: () -> Unit,
    onSave: (SmartPlaylist) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var selectedFolders by remember { mutableStateOf(initial?.folderNames ?: emptySet()) }
    var minMinutes by remember { mutableStateOf(initial?.minDurationMs?.let { (it / 60_000L).toString() } ?: "") }
    var maxMinutes by remember { mutableStateOf(initial?.maxDurationMs?.let { (it / 60_000L).toString() } ?: "") }
    var minRating by remember { mutableStateOf(initial?.minRating ?: 0) }
    var minYear by remember { mutableStateOf(initial?.minYear?.toString() ?: "") }
    var maxYear by remember { mutableStateOf(initial?.maxYear?.toString() ?: "") }
    var keyword by remember { mutableStateOf(initial?.keyword ?: "") }
    var selectedGenre by remember { mutableStateOf(initial?.genre) }

    fun buildDraft(): SmartPlaylist = SmartPlaylist(
        id = initial?.id ?: "",
        name = name.trim().ifBlank { "Playlist Otomatis" },
        folderNames = selectedFolders,
        minDurationMs = minMinutes.trim().toLongOrNull()?.let { it * 60_000L },
        maxDurationMs = maxMinutes.trim().toLongOrNull()?.let { it * 60_000L },
        minRating = minRating,
        minYear = minYear.trim().toIntOrNull(),
        maxYear = maxYear.trim().toIntOrNull(),
        keyword = keyword.trim(),
        genre = selectedGenre
    )

    val draftPreview = remember(name, selectedFolders, minMinutes, maxMinutes, minRating, minYear, maxYear, keyword, selectedGenre) {
        buildDraft()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Bug report user: "Buat Playlist Otomatis" TRUNCATED, 0 bisa discroll —
                // Column ini isinya banyak (nama+folder chips+genre chips+durasi+tahun+rating+
                // tombol Batal/Simpan di paling bawah) TANPA verticalScroll sama sekali,
                // konten yang > tinggi sheet ke-clip diam-diam, tombol Simpan pun ikut tidak
                // terjangkau. Root cause & fix sama persis Batch 112 (`NowPlayingScreen.kt`):
                // verticalScroll murni jaring pengaman — kalau konten muat, scroll offset
                // tetap 0 (0 perubahan visual), kalau tidak muat, sekarang bisa digeser bukan
                // ke-clip/hilang.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                if (initial == null) "Buat Playlist Otomatis" else "Ubah Aturan",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama playlist") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (availableFolders.isNotEmpty()) {
                Text("Folder", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Kosongkan untuk semua folder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableFolders) { folder ->
                        FilterChip(
                            selected = folder in selectedFolders,
                            onClick = {
                                selectedFolders =
                                    if (folder in selectedFolders) selectedFolders - folder
                                    else selectedFolders + folder
                            },
                            label = { Text(folder) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (availableGenres.isNotEmpty()) {
                // Gap List #11 — exact-match picker (not a text field), consistent with the
                // folder chips right above: values come from tags actually seen in the
                // library, not free text the user could mistype against what's stored.
                Text("Genre", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Kosongkan untuk semua genre",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableGenres) { genreOption ->
                        FilterChip(
                            selected = genreOption == selectedGenre,
                            onClick = {
                                // Tap-to-clear on the already-selected chip, same convention
                                // as the rating stars below.
                                selectedGenre = if (genreOption == selectedGenre) null else genreOption
                            },
                            label = { Text(genreOption) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text("Rentang durasi (menit)", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minMinutes,
                    onValueChange = { v -> minMinutes = v.filter { it.isDigit() } },
                    label = { Text("Min") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxMinutes,
                    onValueChange = { v -> maxMinutes = v.filter { it.isDigit() } },
                    label = { Text("Maks") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text("Rentang tahun rilis", style = MaterialTheme.typography.labelLarge)
            Text(
                "Lagu tanpa metadata tahun tidak akan cocok kalau diisi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minYear,
                    onValueChange = { v -> minYear = v.filter { it.isDigit() }.take(4) },
                    label = { Text("Dari") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxYear,
                    onValueChange = { v -> maxYear = v.filter { it.isDigit() }.take(4) },
                    label = { Text("Sampai") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text("Rating minimum", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (star in 1..5) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        // Tapping the star that's already the current minimum clears the filter
                        // (same "tap to clear" convention as NowPlayingScreen's rating row).
                        minRating = if (minRating == star) 0 else star
                    }) {
                        Icon(
                            if (star <= minRating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$star bintang",
                            tint = if (star <= minRating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                if (minRating == 0) {
                    Text(
                        "Semua rating",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("Kata kunci (judul/artis/album)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (draftPreview.isEmpty())
                    "Belum ada aturan diisi — akan cocok dengan seluruh lagu di library"
                else
                    "${activeCriteriaCount(draftPreview)} aturan aktif",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Batal") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onSave(buildDraft()) }) {
                    Text(if (initial == null) "Buat" else "Simpan")
                }
            }
        }
    }
}
