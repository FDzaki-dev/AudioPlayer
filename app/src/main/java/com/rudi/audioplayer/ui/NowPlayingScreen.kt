package com.rudi.audioplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.rudi.audioplayer.playback.PlaybackUiState

@Composable
fun NowPlayingScreen(
    uiState: PlaybackUiState,
    isFavorite: Boolean,
    sleepTimerRemainingMs: Long?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetVolume: (Float) -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onGetLyrics: (Long) -> String?,
    onSaveLyrics: (Long, String) -> Unit,
    onDeleteLyrics: (Long) -> Unit,
    onBack: () -> Unit
) {
    val song = uiState.currentSong
    val haptic = LocalHapticFeedback.current
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Tutup")
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Hapus dari favorit" else "Tambah ke favorit",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = { showSleepTimerDialog = true }) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = "Sleep timer",
                    tint = if (sleepTimerRemainingMs != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = { showSpeedDialog = true }) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = "Kecepatan putar",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = { showQueueSheet = true }) {
                Icon(
                    Icons.Default.QueueMusic,
                    contentDescription = "Antrean putar",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = { showLyricsSheet = true }) {
                Icon(
                    Icons.Default.Article,
                    contentDescription = "Lirik",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        VinylAlbumArt(
            albumId = song?.albumId,
            isPlaying = uiState.isPlaying,
            onSwipeNext = onNext,
            onSwipePrevious = onPrevious
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "SEDANG DIPUTAR",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            song?.title ?: "-",
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )
        Text(song?.artist ?: "-", style = MaterialTheme.typography.bodyMedium, maxLines = 1)

        Spacer(modifier = Modifier.height(24.dp))

        var sliderPosition by remember(uiState.position) { mutableStateOf(uiState.position.toFloat()) }
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { onSeek(sliderPosition.toLong()) },
            valueRange = 0f..(uiState.duration.coerceAtLeast(1L).toFloat()),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                formatDuration(uiState.position),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                formatDuration(uiState.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onShuffle) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Acak",
                    tint = if (uiState.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Sebelumnya", modifier = Modifier.size(36.dp))
            }
            FilledIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayPause()
                },
                modifier = Modifier.size(68.dp)
            ) {
                AnimatedContent(targetState = uiState.isPlaying, label = "playPause") { playing ->
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Jeda" else "Putar",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya", modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onRepeat) {
                val icon = if (uiState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                Icon(
                    icon,
                    contentDescription = "Ulangi",
                    tint = if (uiState.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${uiState.playbackSpeed}x",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val volumeIcon = when {
                uiState.volume <= 0f -> Icons.Default.VolumeOff
                uiState.volume < 0.5f -> Icons.Default.VolumeDown
                else -> Icons.Default.VolumeUp
            }
            Icon(volumeIcon, contentDescription = "Volume", tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = uiState.volume,
                onValueChange = onSetVolume,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentRemainingMs = sleepTimerRemainingMs,
            onDismiss = { showSleepTimerDialog = false },
            onSelect = onSetSleepTimer,
            onCancelTimer = onCancelSleepTimer
        )
    }

    if (showSpeedDialog) {
        SpeedDialog(
            currentSpeed = uiState.playbackSpeed,
            onDismiss = { showSpeedDialog = false },
            onSelect = onSetSpeed
        )
    }

    if (showQueueSheet) {
        QueueSheet(
            queue = uiState.queue,
            currentIndex = uiState.currentIndex,
            onDismiss = { showQueueSheet = false },
            onPlayIndex = { index -> onPlayQueueIndex(index) },
            onMove = { from, to -> onMoveQueueItem(from, to) },
            onRemove = { index -> onRemoveFromQueue(index) }
        )
    }

    if (showLyricsSheet && song != null) {
        var lyricsText by remember(song.id) { mutableStateOf(onGetLyrics(song.id)) }
        LyricsSheet(
            rawLyrics = lyricsText,
            positionMs = uiState.position,
            onDismiss = { showLyricsSheet = false },
            onSave = { text ->
                onSaveLyrics(song.id, text)
                lyricsText = text
            },
            onDelete = {
                onDeleteLyrics(song.id)
                lyricsText = null
            }
        )
    }
}

/**
 * The app's signature element: album art rendered as a spinning vinyl disc.
 * Rotation runs continuously while playing and freezes in place on pause,
 * exactly like a real turntable.
 */
@Composable
private fun VinylAlbumArt(
    albumId: Long?,
    isPlaying: Boolean,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit
) {
    val rotation = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    var totalDrag by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 18000, easing = LinearEasing)
                )
            )
        } else {
            rotation.stop()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { totalDrag = 0f },
                onDragEnd = {
                    if (totalDrag < -120f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwipeNext()
                    } else if (totalDrag > 120f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwipePrevious()
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    totalDrag += dragAmount
                    change.consume()
                }
            )
        }
    ) {
        AsyncImage(
            model = albumId?.let { albumArtUri(it) },
            contentDescription = null,
            modifier = Modifier
                .size(260.dp)
                .graphicsLayer { rotationZ = rotation.value }
                .clip(CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

@Composable
private fun SleepTimerDialog(
    currentRemainingMs: Long?,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    val options = listOf(10, 15, 30, 45, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                if (currentRemainingMs != null) {
                    Text(
                        "Aktif — berhenti dalam ${formatDuration(currentRemainingMs)}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                options.forEach { minutes ->
                    TextButton(
                        onClick = { onSelect(minutes); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("$minutes menit")
                    }
                }
            }
        },
        confirmButton = {
            if (currentRemainingMs != null) {
                TextButton(onClick = { onCancelTimer(); onDismiss() }) { Text("Matikan Timer") }
            } else {
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        },
        dismissButton = {
            if (currentRemainingMs != null) {
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        }
    )
}

@Composable
private fun SpeedDialog(currentSpeed: Float, onDismiss: () -> Unit, onSelect: (Float) -> Unit) {
    val options = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kecepatan Putar") },
        text = {
            Column {
                options.forEach { speed ->
                    val isSelected = speed == currentSpeed
                    TextButton(
                        onClick = { onSelect(speed); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isSelected) "${speed}x  ✓" else "${speed}x",
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
