package com.rudi.audioplayer.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rudi.audioplayer.data.MusicRepository
import com.rudi.audioplayer.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LibraryScreen(
    favoriteIds: Set<Long>,
    onToggleFavorite: (Long) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit
) {
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var refreshKey by remember { mutableStateOf(0) }
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(refreshKey) {
        loading = true
        songs = withContext(Dispatchers.IO) { MusicRepository(context).getAllSongs() }
        loading = false
    }

    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LibraryHeader(
            searchActive = searchActive,
            onToggleSearch = {
                searchActive = !searchActive
                if (!searchActive) searchQuery = ""
            },
            onRescan = { refreshKey++ }
        )

        if (searchActive) {
            LibrarySearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClose = { searchActive = false; searchQuery = "" }
            )
        }

        LibraryFilterChips(selectedTab = selectedTab, onSelect = { selectedTab = it })

        when {
            loading -> ShimmerList()
            songs.isEmpty() -> EmptyState(
                title = "Belum ada musik",
                subtitle = "Tambahkan file audio ke penyimpanan perangkat, lalu pindai ulang.",
                actionLabel = "Pindai Ulang",
                onAction = { refreshKey++ }
            )
            selectedTab == 4 -> {
                val favoriteSongs = filteredSongs.filter { favoriteIds.contains(it.id) }
                if (favoriteSongs.isEmpty()) {
                    EmptyState(
                        title = "Belum ada favorit",
                        subtitle = "Ketuk ikon hati pada lagu untuk menambahkannya ke sini."
                    )
                } else {
                    SongListView(favoriteSongs, favoriteIds, onToggleFavorite, onSongClick)
                }
            }
            filteredSongs.isEmpty() -> EmptyState(
                title = "Tidak ditemukan",
                subtitle = "Coba kata kunci lain."
            )
            selectedTab == 0 -> SongListView(filteredSongs, favoriteIds, onToggleFavorite, onSongClick)
            selectedTab == 1 -> GroupedListView(
                songs = filteredSongs,
                groupOf = { it.album.ifBlank { "Album Tidak Diketahui" } },
                favoriteIds = favoriteIds,
                onFavoriteToggle = onToggleFavorite,
                onSongClick = onSongClick
            )
            selectedTab == 2 -> GroupedListView(
                songs = filteredSongs,
                groupOf = { it.artist },
                favoriteIds = favoriteIds,
                onFavoriteToggle = onToggleFavorite,
                onSongClick = onSongClick
            )
            else -> GroupedListView(
                songs = filteredSongs,
                groupOf = { it.folderName },
                favoriteIds = favoriteIds,
                onFavoriteToggle = onToggleFavorite,
                onSongClick = onSongClick
            )
        }
    }
}

@Composable
private fun LibraryHeader(searchActive: Boolean, onToggleSearch: () -> Unit, onRescan: () -> Unit) {
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
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
private fun LibraryFilterChips(selectedTab: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Lagu", "Album", "Artis", "Folder", "Favorit")
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(labels) { index, label ->
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
    }
}

@Composable
private fun SongListView(
    songs: List<Song>,
    favoriteIds: Set<Long>,
    onFavoriteToggle: (Long) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit
) {
    LazyColumn {
        itemsIndexed(songs) { index, song ->
            SongRow(
                song = song,
                isFavorite = favoriteIds.contains(song.id),
                onFavoriteToggle = { onFavoriteToggle(song.id) },
                onClick = { onSongClick(songs, index) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun GroupedListView(
    songs: List<Song>,
    groupOf: (Song) -> String,
    favoriteIds: Set<Long>,
    onFavoriteToggle: (Long) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit
) {
    var selectedGroup by remember(songs) { mutableStateOf<String?>(null) }
    val grouped = remember(songs) { songs.groupBy(groupOf) }

    if (selectedGroup == null) {
        LazyColumn {
            items(grouped.keys.toList().sorted()) { group ->
                ListItem(
                    headlineContent = { Text(group, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("${grouped[group]?.size ?: 0} lagu") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { selectedGroup = group }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    } else {
        val groupSongs = grouped[selectedGroup].orEmpty()
        Column {
            TextButton(onClick = { selectedGroup = null }) { Text("< Kembali") }
            LazyColumn {
                itemsIndexed(groupSongs) { index, song ->
                    SongRow(
                        song = song,
                        isFavorite = favoriteIds.contains(song.id),
                        onFavoriteToggle = { onFavoriteToggle(song.id) },
                        onClick = { onSongClick(groupSongs, index) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = albumArtUri(song.albumId),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
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

@Composable
private fun EmptyState(
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
private fun ShimmerBrush(): Brush {
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
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
