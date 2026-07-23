package com.rudi.audioplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Equalizer
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import android.view.WindowManager
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.rudi.audioplayer.playback.EqualizerController
import com.rudi.audioplayer.playback.EqualizerUiState
import com.rudi.audioplayer.playback.PlaybackUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NowPlayingScreen(
    uiState: PlaybackUiState,
    isFavorite: Boolean,
    sleepTimerRemainingMs: Long?,
    accentColor: Color?,
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
    crossfadeEnabled: Boolean,
    onSetCrossfadeEnabled: (Boolean) -> Unit,
    onSetVolume: (Float) -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onGetLyrics: (Long) -> String?,
    onSaveLyrics: (Long, String) -> Unit,
    onDeleteLyrics: (Long) -> Unit,
    equalizerState: EqualizerUiState,
    onOpenEqualizer: () -> Unit,
    onToggleEqualizerEnabled: (Boolean) -> Unit,
    onEqualizerBandChange: (Int, Short) -> Unit,
    onEqualizerPresetSelect: (Int) -> Unit,
    onEqualizerBoldPresetSelect: (EqualizerController.BoldPreset) -> Unit,
    onBack: () -> Unit
) {
    val song = uiState.currentSong
    val haptic = LocalHapticFeedback.current
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }

    // --- Swipe gesture: brightness (left of album art) & audio volume (right of album art) ---
    val gestureScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var brightnessLevel by remember {
        mutableStateOf(
            activity?.window?.attributes?.screenBrightness
                ?.takeIf { it in 0f..1f } ?: 0.5f
        )
    }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    val latestVolume = rememberUpdatedState(uiState.volume)

    fun applyBrightness(target: Float) {
        val clamped = target.coerceIn(0.02f, 1f)
        brightnessLevel = clamped
        val window = activity?.window ?: return
        val params = window.attributes
        params.screenBrightness = clamped
        window.attributes = params
    }

    // The brightness override only applies to this screen — restore the system/app
    // default the moment Now Playing is closed, instead of leaving it dimmed everywhere.
    DisposableEffect(Unit) {
        onDispose {
            val window = activity?.window ?: return@onDispose
            val params = window.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = params
        }
    }

    val fallback = MaterialTheme.colorScheme.primary
    val animatedAccent by animateColorAsState(
        targetValue = accentColor ?: fallback,
        animationSpec = tween(700),
        label = "accentColor"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = song?.albumId?.let { albumArtUri(it) },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(60.dp)
                .alpha(0.5f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            animatedAccent.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.97f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Tutup")
            }
            Spacer(modifier = Modifier.weight(1f))
            val favoriteInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleFavorite()
                },
                interactionSource = favoriteInteraction,
                modifier = Modifier.bouncyPress(favoriteInteraction, pressedScale = 0.75f)
            ) {
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
                    tint = if (sleepTimerRemainingMs != null) animatedAccent else MaterialTheme.colorScheme.secondary
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
            IconButton(onClick = {
                onOpenEqualizer()
                showEqualizerSheet = true
            }) {
                Icon(
                    Icons.Default.Equalizer,
                    contentDescription = "Equalizer",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val entranceScale = remember { Animatable(0.55f) }
        val entranceAlpha = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            launch {
                entranceScale.animateTo(
                    1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            }
            launch { entranceAlpha.animateTo(1f, animationSpec = tween(280)) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left zone: swipe up/down to raise/lower screen brightness.
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { showBrightnessIndicator = true },
                            onDragEnd = {
                                gestureScope.launch {
                                    delay(600)
                                    showBrightnessIndicator = false
                                }
                            },
                            onDragCancel = { showBrightnessIndicator = false },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                applyBrightness(brightnessLevel - dragAmount / size.height)
                            }
                        )
                    }
            )

            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = entranceScale.value
                    scaleY = entranceScale.value
                    alpha = entranceAlpha.value
                }
            ) {
                VinylAlbumArt(
                    albumId = song?.albumId,
                    isPlaying = uiState.isPlaying,
                    accentColor = animatedAccent,
                    onSwipeNext = onNext,
                    onSwipePrevious = onPrevious
                )
            }

            // Right zone: swipe up/down to raise/lower playback volume.
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { showVolumeIndicator = true },
                            onDragEnd = {
                                gestureScope.launch {
                                    delay(600)
                                    showVolumeIndicator = false
                                }
                            },
                            onDragCancel = { showVolumeIndicator = false },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val next = (latestVolume.value - dragAmount / size.height).coerceIn(0f, 1f)
                                onSetVolume(next)
                            }
                        )
                    }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "SEDANG DIPUTAR",
            style = MaterialTheme.typography.labelSmall,
            color = animatedAccent
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
                thumbColor = animatedAccent,
                activeTrackColor = animatedAccent,
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
            val shuffleInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onShuffle,
                interactionSource = shuffleInteraction,
                modifier = Modifier.bouncyPress(shuffleInteraction)
            ) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Acak",
                    tint = if (uiState.shuffleEnabled) animatedAccent else MaterialTheme.colorScheme.secondary
                )
            }
            val prevInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onPrevious,
                interactionSource = prevInteraction,
                modifier = Modifier.bouncyPress(prevInteraction)
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Sebelumnya", modifier = Modifier.size(36.dp))
            }
            val playPauseInteraction = remember { MutableInteractionSource() }
            FilledIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayPause()
                },
                interactionSource = playPauseInteraction,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = animatedAccent,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier
                    .size(68.dp)
                    .bouncyPress(playPauseInteraction, pressedScale = 0.85f)
            ) {
                AnimatedContent(targetState = uiState.isPlaying, label = "playPause") { playing ->
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Jeda" else "Putar",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            val nextInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onNext,
                interactionSource = nextInteraction,
                modifier = Modifier.bouncyPress(nextInteraction)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya", modifier = Modifier.size(36.dp))
            }
            val repeatInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onRepeat,
                interactionSource = repeatInteraction,
                modifier = Modifier.bouncyPress(repeatInteraction)
            ) {
                val icon = if (uiState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                Icon(
                    icon,
                    contentDescription = "Ulangi",
                    tint = if (uiState.repeatMode != Player.REPEAT_MODE_OFF) animatedAccent else MaterialTheme.colorScheme.secondary
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

        AnimatedVisibility(
            visible = showBrightnessIndicator,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300))
        ) {
            GestureIndicatorBadge(
                icon = when {
                    brightnessLevel < 0.33f -> Icons.Default.BrightnessLow
                    brightnessLevel < 0.66f -> Icons.Default.BrightnessMedium
                    else -> Icons.Default.BrightnessHigh
                },
                value = brightnessLevel,
                accentColor = animatedAccent
            )
        }

        AnimatedVisibility(
            visible = showVolumeIndicator,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300))
        ) {
            GestureIndicatorBadge(
                icon = when {
                    uiState.volume <= 0f -> Icons.Default.VolumeOff
                    uiState.volume < 0.5f -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeUp
                },
                value = uiState.volume,
                accentColor = animatedAccent
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
            crossfadeEnabled = crossfadeEnabled,
            onDismiss = { showSpeedDialog = false },
            onSelect = onSetSpeed,
            onToggleCrossfade = onSetCrossfadeEnabled
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

    if (showEqualizerSheet) {
        EqualizerSheet(
            state = equalizerState,
            onDismiss = { showEqualizerSheet = false },
            onToggleEnabled = onToggleEqualizerEnabled,
            onBandChange = onEqualizerBandChange,
            onPresetSelect = onEqualizerPresetSelect,
            onBoldPresetSelect = onEqualizerBoldPresetSelect
        )
    }
}

/**
 * Small floating pill shown while dragging the brightness/volume swipe zones,
 * mirroring the transient overlay pattern used by most media/video apps.
 */
@Composable
private fun GestureIndicatorBadge(icon: ImageVector, value: Float, accentColor: Color) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = accentColor)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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
    accentColor: Color,
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
                .border(3.dp, accentColor, CircleShape)
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
private fun SpeedDialog(
    currentSpeed: Float,
    crossfadeEnabled: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Float) -> Unit,
    onToggleCrossfade: (Boolean) -> Unit
) {
    val options = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan Putar") },
        text = {
            Column {
                Text(
                    "Kecepatan",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                options.forEach { speed ->
                    val isSelected = speed == currentSpeed
                    TextButton(
                        onClick = { onSelect(speed) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isSelected) "${speed}x  ✓" else "${speed}x",
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleCrossfade(!crossfadeEnabled) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fade Transisi", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Volume melandai halus di pergantian lagu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(checked = crossfadeEnabled, onCheckedChange = onToggleCrossfade)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
