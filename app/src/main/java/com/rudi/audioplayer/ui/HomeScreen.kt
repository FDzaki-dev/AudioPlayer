package com.rudi.audioplayer.ui

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rudi.audioplayer.data.MusicRepository
import com.rudi.audioplayer.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun HomeScreen(
    favoriteIds: Set<Long>,
    onSongClick: (List<Song>, Int) -> Unit,
    resumePreview: (List<Song>) -> Song?,
    onResumeClick: (List<Song>) -> Unit,
    recentSongsProvider: (List<Song>) -> List<Song>,
    mostPlayedProvider: (List<Song>) -> List<Song>,
    statsVersion: Int,
    onShuffleAll: (List<Song>) -> Unit
) {
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        songs = withContext(Dispatchers.IO) { MusicRepository(context).getAllSongs() }
        loading = false
    }

    val continueSong = remember(songs) { if (songs.isEmpty()) null else resumePreview(songs) }
    val favoriteSongs = remember(songs, favoriteIds) { songs.filter { favoriteIds.contains(it.id) } }
    val recentlyAdded = remember(songs) { songs.sortedByDescending { it.dateAdded }.take(15) }
    val recentlyPlayed = remember(songs, statsVersion) { recentSongsProvider(songs) }
    val mostPlayed = remember(songs, statsVersion) { mostPlayedProvider(songs) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            HomeGreeting(
                showShuffleAll = songs.isNotEmpty(),
                onShuffleAll = { onShuffleAll(songs) }
            )
        }

        if (continueSong != null) {
            item {
                ContinueListeningCard(
                    song = continueSong,
                    onClick = { onResumeClick(songs) }
                )
            }
        }

        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        if (favoriteSongs.isNotEmpty()) {
            item {
                HomeSectionRow(
                    title = "Favorit",
                    songs = favoriteSongs,
                    onSongClick = { song -> onSongClick(favoriteSongs, favoriteSongs.indexOf(song)) }
                )
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item {
                HomeSectionRow(
                    title = "Baru Diputar",
                    songs = recentlyPlayed,
                    onSongClick = { song -> onSongClick(recentlyPlayed, recentlyPlayed.indexOf(song)) }
                )
            }
        }

        if (mostPlayed.isNotEmpty()) {
            item {
                HomeSectionRow(
                    title = "Paling Sering Diputar",
                    songs = mostPlayed,
                    onSongClick = { song -> onSongClick(mostPlayed, mostPlayed.indexOf(song)) }
                )
            }
        }

        if (recentlyAdded.isNotEmpty()) {
            item {
                HomeSectionRow(
                    title = "Baru Ditambahkan",
                    songs = recentlyAdded,
                    onSongClick = { song -> onSongClick(recentlyAdded, recentlyAdded.indexOf(song)) }
                )
            }
        }

        if (!loading && songs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Belum ada musik. Buka tab Perpustakaan untuk memindai.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun HomeGreeting(showShuffleAll: Boolean, onShuffleAll: () -> Unit) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..10 -> "Selamat Pagi"
            in 11..14 -> "Selamat Siang"
            in 15..18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("BERANDA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(greeting, style = MaterialTheme.typography.titleLarge)
        }
        if (showShuffleAll) {
            IconButton(onClick = onShuffleAll) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Acak semua musik",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ContinueListeningCard(song: Song, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = albumArtUri(song.albumId),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "LANJUTKAN MENDENGARKAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
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
            FilledIconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Lanjutkan")
            }
        }
    }
}

@Composable
private fun HomeSectionRow(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                HomeSongCard(song = song, onClick = { onSongClick(song) })
            }
        }
    }
}

@Composable
private fun HomeSongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = albumArtUri(song.albumId),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
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
}
