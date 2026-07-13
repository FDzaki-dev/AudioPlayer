package com.rudi.audioplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.rudi.audioplayer.playback.PlaybackUiState

@Composable
fun NowPlayingScreen(
    uiState: PlaybackUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onBack: () -> Unit
) {
    val song = uiState.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Tutup")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AsyncImage(
            model = song?.let { albumArtUri(it.albumId) },
            contentDescription = null,
            modifier = Modifier.size(280.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(song?.title ?: "-", style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Text(song?.artist ?: "-", style = MaterialTheme.typography.bodyMedium, maxLines = 1)

        Spacer(modifier = Modifier.height(24.dp))

        var sliderPosition by remember(uiState.position) { mutableStateOf(uiState.position.toFloat()) }
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { onSeek(sliderPosition.toLong()) },
            valueRange = 0f..(uiState.duration.coerceAtLeast(1L).toFloat())
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(uiState.position))
            Text(formatDuration(uiState.duration))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onShuffle) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Acak",
                    tint = if (uiState.shuffleEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Sebelumnya", modifier = Modifier.size(36.dp))
            }
            FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp)) {
                Icon(
                    if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya", modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onRepeat) {
                val icon = if (uiState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                Icon(
                    icon,
                    contentDescription = "Ulangi",
                    tint = if (uiState.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        }
    }
}
