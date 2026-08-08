package com.rudi.audioplayer.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.LibraryFilterStore
import com.rudi.audioplayer.data.Song
import com.rudi.audioplayer.ui.theme.matteEmboss
import kotlinx.collections.immutable.ImmutableSet
import java.util.Calendar

@Composable
fun HomeScreen(
    rawSongs: List<Song>,
    loading: Boolean,
    favoriteIds: ImmutableSet<Long>,
    onSongClick: (List<Song>, Int) -> Unit,
    resumePreview: (List<Song>) -> Song?,
    onResumeClick: (List<Song>) -> Unit,
    recentSongsProvider: (List<Song>) -> List<Song>,
    mostPlayedProvider: (List<Song>) -> List<Song>,
    topArtistMixProvider: (List<Song>) -> Pair<String, List<Song>>?,
    flashbackProvider: (List<Song>) -> Pair<String, List<Song>>?,
    statsVersion: Int,
    onShuffleAll: (List<Song>) -> Unit
) {
    val context = LocalContext.current
    val songs = remember(rawSongs) { LibraryFilterStore(context).apply(rawSongs) }

    val continueSong = remember(songs) { if (songs.isEmpty()) null else resumePreview(songs) }
    val favoriteSongs = remember(songs, favoriteIds) { songs.filter { favoriteIds.contains(it.id) } }
    val recentlyAdded = remember(songs) { songs.sortedByDescending { it.dateAdded }.take(15) }
    val recentlyPlayed = remember(songs, statsVersion) { recentSongsProvider(songs) }
    val mostPlayed = remember(songs, statsVersion) { mostPlayedProvider(songs) }
    val artistMix = remember(songs, statsVersion) { topArtistMixProvider(songs) }
    val flashback = remember(songs, statsVersion) { flashbackProvider(songs) }

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
            item { HomeShimmerSection() }
            item { HomeShimmerSection() }
        }

        if (flashback != null) {
            val (label, flashbackSongs) = flashback
            item {
                HomeSectionRow(
                    title = "Kilas Balik: $label",
                    songs = flashbackSongs,
                    onSongClick = { song -> onSongClick(flashbackSongs, flashbackSongs.indexOf(song)) }
                )
            }
        }

        if (artistMix != null) {
            val (artistName, artistSongs) = artistMix
            item {
                HomeSectionRow(
                    title = "Mix: $artistName",
                    songs = artistSongs,
                    onSongClick = { song -> onSongClick(artistSongs, artistSongs.indexOf(song)) }
                )
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
                EmptyState(
                    title = "Belum ada musik",
                    subtitle = "Buka tab Perpustakaan untuk memindai musik di perangkat kamu.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = 32.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun HomeShimmerSection() {
    val brush = ShimmerBrush()
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .width(120.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                Column(modifier = Modifier.width(120.dp)) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
            }
        }
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
    val isMatte = MaterialTheme.colorScheme.background == com.rudi.audioplayer.ui.theme.MatteBackground
    // Batch 40: this card was hardcoded RoundedCornerShape(18.dp) regardless of theme (never
    // picked up Matte Noir's sharp shape tokens) and used flat tonalElevation only — first
    // thing the eye hits on Home, so it's a priority "epic" touch point.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .then(
                if (isMatte)
                    Modifier.matteEmboss(shape = MaterialTheme.shapes.medium, elevation = 10.dp)
                else
                    Modifier.clip(RoundedCornerShape(18.dp))
            )
            .clickable(onClick = onClick),
        color = if (isMatte) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isMatte) 0.dp else 4.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(
                albumId = song.albumId,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
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
                    overflow = TextOverflow.Ellipsis,
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
        AlbumArt(
            albumId = song.albumId,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(20.dp))
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
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
