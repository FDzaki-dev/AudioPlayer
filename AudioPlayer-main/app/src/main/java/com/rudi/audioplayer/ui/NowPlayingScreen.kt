package com.rudi.audioplayer.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.rudi.audioplayer.playback.EqualizerController
import com.rudi.audioplayer.playback.EqualizerUiState
import com.rudi.audioplayer.playback.PlaybackUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(
    uiState: PlaybackUiState, isFavorite: Boolean, currentRating: Int, onSetRating: (Int) -> Unit,
    sleepTimerRemainingMs: Long?, accentColor: Color?, onPlayPause: () -> Unit, onNext: () -> Unit,
    onPrevious: () -> Unit, onSeek: (Long) -> Unit, onShuffle: () -> Unit, onRepeat: () -> Unit,
    onToggleFavorite: () -> Unit, onSetSleepTimer: (Int) -> Unit, onCancelSleepTimer: () -> Unit,
    onSetSpeed: (Float) -> Unit, crossfadeEnabled: Boolean, onSetCrossfadeEnabled: (Boolean) -> Unit,
    onSetVolume: (Float) -> Unit, onPlayQueueIndex: (Int) -> Unit, onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit, onGetLyrics: (Long) -> String?, onSaveLyrics: (Long, String) -> Unit,
    onDeleteLyrics: (Long) -> Unit, equalizerState: EqualizerUiState, onOpenEqualizer: () -> Unit,
    onToggleEqualizerEnabled: (Boolean) -> Unit, onEqualizerBandChange: (Int, Short) -> Unit,
    onEqualizerPresetSelect: (Int) -> Unit, onEqualizerBoldPresetSelect: (EqualizerController.BoldPreset) -> Unit,
    onBack: () -> Unit
) {
    val song = uiState.currentSong
    val haptic = LocalHapticFeedback.current
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showEq by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val density = LocalDensity.current
    val rangePx = remember(density) { with(density) { 140.dp.toPx() } }

    var brightness by remember { mutableStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.5f) }
    var showBrtInd by remember { mutableStateOf(false) }
    var showVolInd by remember { mutableStateOf(false) }

    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVol = remember(audioManager) { (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1) }
    var sysVol by remember { mutableStateOf(((audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0).toFloat() / maxVol).coerceIn(0f, 1f)) }

    fun setBrt(v: Float) {
        brightness = v.coerceIn(0.02f, 1f)
        activity?.window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = brightness } }
    }

    fun setVol(v: Float) {
        sysVol = v.coerceIn(0f, 1f)
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, (sysVol * maxVol).roundToInt(), 0)
    }

    DisposableEffect(Unit) {
        onDispose { activity?.window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } } }
    }

    val animatedAccent by animateColorAsState(accentColor ?: MaterialTheme.colorScheme.primary, tween(700), label = "acc")
    val playPauseColor = if (animatedAccent.luminance() > 0.55f) Color.Black else Color.White

    Box(Modifier.fillMaxSize()) {
        AsyncImage(song?.albumId?.let { albumArtUri(it) }, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(60.dp).alpha(0.5f))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(animatedAccent.copy(0.35f), MaterialTheme.colorScheme.background.copy(0.80f), MaterialTheme.colorScheme.background.copy(0.98f)))))

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 16.dp), Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.Default.KeyboardArrowDown, "Back", Modifier.size(32.dp)) }
                Spacer(Modifier.weight(1f))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    IconButton({ haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggleFavorite() }) { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Fav", tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary) }
                    IconButton({ showSleepTimer = true }) { Icon(Icons.Default.Timer, "Timer", tint = if (sleepTimerRemainingMs != null) animatedAccent else MaterialTheme.colorScheme.secondary) }
                    IconButton({ showSpeed = true }) { Icon(Icons.Default.Speed, "Speed", tint = MaterialTheme.colorScheme.secondary) }
                    IconButton({ showQueue = true }) { Icon(Icons.Default.QueueMusic, "Queue", tint = MaterialTheme.colorScheme.secondary) }
                    IconButton({ showLyrics = true }) { Icon(Icons.Default.Article, "Lyrics", tint = MaterialTheme.colorScheme.secondary) }
                    IconButton({ onOpenEqualizer(); showEq = true }) { Icon(Icons.Default.Equalizer, "EQ", tint = MaterialTheme.colorScheme.secondary) }
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(Modifier.fillMaxWidth().height(300.dp)) {
                Box(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.5f).fillMaxHeight().pointerInput(Unit) {
                    detectVerticalDragGestures(onDragStart = { showBrtInd = true }, onDragEnd = { scope.launch { delay(600); showBrtInd = false } }, onVerticalDrag = { c, d -> c.consume(); setBrt(brightness - d / rangePx) })
                })
                Box(Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.5f).fillMaxHeight().pointerInput(Unit) {
                    detectVerticalDragGestures(onDragStart = { showVolInd = true }, onDragEnd = { scope.launch { delay(600); showVolInd = false } }, onVerticalDrag = { c, d -> c.consume(); setVol(sysVol - d / rangePx) })
                })
                AlbumArtHero(song?.albumId, animatedAccent, onNext, onPrevious)
            }

            Spacer(Modifier.height(28.dp))
            Text("SEDANG DIPUTAR", style = MaterialTheme.typography.labelSmall, color = animatedAccent)
            Text(song?.title ?: "-", style = MaterialTheme.typography.titleLarge, maxLines = 1, modifier = Modifier.basicMarquee())
            Text(song?.artist ?: "-", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            StarRatingRow(currentRating, onSetRating, animatedAccent)

            Spacer(Modifier.height(20.dp))
            var sliderPos by remember(uiState.position) { mutableStateOf(uiState.position.toFloat()) }
            val progress = (sliderPos / uiState.duration.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

            Box(Modifier.fillMaxWidth().height(48.dp), Alignment.Center) {
                WaveformSeekBar(song?.id ?: 0L, progress, animatedAccent, MaterialTheme.colorScheme.surfaceVariant, Modifier.fillMaxWidth().height(32.dp))
                Slider(sliderPos, { sliderPos = it }, onValueChangeFinished = { onSeek(sliderPos.toLong()) }, valueRange = 0f..(uiState.duration.coerceAtLeast(1L).toFloat()), colors = SliderDefaults.colors(thumbColor = animatedAccent, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent))
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(formatDuration(uiState.position), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text(formatDuration(uiState.duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                IconButton(onShuffle) { Icon(Icons.Default.Shuffle, "Acak", tint = if (uiState.shuffleEnabled) animatedAccent else MaterialTheme.colorScheme.secondary) }
                IconButton(onPrevious) { Icon(Icons.Default.SkipPrevious, "Prev", Modifier.size(38.dp)) }
                FilledIconButton({ haptic.performHapticFeedback(HapticFeedbackType.LongPress); onPlayPause() }, Modifier.size(68.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = animatedAccent, contentColor = playPauseColor)) {
                    AnimatedContent(uiState.isPlaying, label = "pp") { playing -> Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", Modifier.size(36.dp)) }
                }
                IconButton(onNext) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(38.dp)) }
                IconButton(onRepeat) { Icon(if (uiState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", tint = if (uiState.repeatMode != Player.REPEAT_MODE_OFF) animatedAccent else MaterialTheme.colorScheme.secondary) }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (uiState.volume <= 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp, "Vol", tint = MaterialTheme.colorScheme.secondary)
                Slider(uiState.volume, onSetVolume, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.secondary, activeTrackColor = MaterialTheme.colorScheme.secondary))
            }
        }

        AnimatedVisibility(showBrtInd, Modifier.align(Alignment.CenterStart).padding(start = 20.dp)) { GestureBadge(Icons.Default.BrightnessMedium, brightness, animatedAccent) }
        AnimatedVisibility(showVolInd, Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)) { GestureBadge(Icons.Default.VolumeUp, sysVol, animatedAccent) }
    }

    if (showSleepTimer) SleepTimerDialog(sleepTimerRemainingMs, { showSleepTimer = false }, onSetSleepTimer, onCancelSleepTimer)
    if (showSpeed) SpeedDialog(uiState.playbackSpeed, crossfadeEnabled, { showSpeed = false }, onSetSpeed, onSetCrossfadeEnabled)
    if (showQueue) QueueSheet(uiState.queue, uiState.currentIndex, { showQueue = false }, onPlayQueueIndex, onMoveQueueItem, onRemoveFromQueue)
    if (showLyrics && song != null) {
        var lyricsText by remember(song.id) { mutableStateOf(onGetLyrics(song.id)) }
        LyricsSheet(lyricsText, uiState.position, { showLyrics = false }, { onSaveLyrics(song.id, it); lyricsText = it }, { onDeleteLyrics(song.id); lyricsText = null })
    }
    if (showEq) EqualizerSheet(equalizerState, { showEq = false }, onToggleEqualizerEnabled, onEqualizerBandChange, onEqualizerPresetSelect, onEqualizerBoldPresetSelect)
}
