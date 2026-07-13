package com.rudi.audioplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rudi.audioplayer.data.MusicRepository
import com.rudi.audioplayer.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LibraryScreen(onSongClick: (List<Song>, Int) -> Unit) {
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        songs = withContext(Dispatchers.IO) { MusicRepository(context).getAllSongs() }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Musik Saya") })
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Lagu") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Folder") })
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            songs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada file audio ditemukan di perangkat.")
            }
            selectedTab == 0 -> SongListView(songs = songs, onSongClick = onSongClick)
            else -> FolderListView(songs = songs, onSongClick = onSongClick)
        }
    }
}

@Composable
private fun SongListView(songs: List<Song>, onSongClick: (List<Song>, Int) -> Unit) {
    LazyColumn {
        itemsIndexed(songs) { index, song ->
            SongRow(song = song, onClick = { onSongClick(songs, index) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun FolderListView(songs: List<Song>, onSongClick: (List<Song>, Int) -> Unit) {
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    val grouped = remember(songs) { songs.groupBy { it.folderName } }

    if (selectedFolder == null) {
        LazyColumn {
            items(grouped.keys.toList().sorted()) { folder ->
                ListItem(
                    headlineContent = { Text(folder) },
                    supportingContent = { Text("${grouped[folder]?.size ?: 0} lagu") },
                    modifier = Modifier.clickable { selectedFolder = folder }
                )
                HorizontalDivider()
            }
        }
    } else {
        val folderSongs = grouped[selectedFolder].orEmpty()
        Column {
            TextButton(onClick = { selectedFolder = null }) { Text("< Kembali ke Folder") }
            LazyColumn {
                itemsIndexed(folderSongs) { index, song ->
                    SongRow(song = song, onClick = { onSongClick(folderSongs, index) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
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
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, maxLines = 1)
            Text(song.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall)
        }
        Text(formatDuration(song.duration), style = MaterialTheme.typography.bodySmall)
    }
}
