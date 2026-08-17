package com.rudi.audioplayer.ui

import android.net.Uri
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.Player
import com.rudi.audioplayer.data.OnboardingHintStore
import com.rudi.audioplayer.playback.EqualizerController
import com.rudi.audioplayer.playback.EqualizerUiState
import com.rudi.audioplayer.playback.PlaybackUiState
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.ui.theme.tactileEmboss
import com.rudi.audioplayer.ui.theme.skeuEmboss
import com.rudi.audioplayer.ui.theme.isTactileTheme
import com.rudi.audioplayer.ui.theme.isSkeuTheme
import com.rudi.audioplayer.ui.theme.TactileHighlight
import com.rudi.audioplayer.ui.theme.TactileShadow
import com.rudi.audioplayer.ui.theme.TactileLightHighlight
import com.rudi.audioplayer.ui.theme.TactileLightShadow
import com.rudi.audioplayer.ui.theme.SkeuAmbientOcclusion
import com.rudi.audioplayer.ui.theme.SkeuHighlight
import com.rudi.audioplayer.ui.theme.SkeuShadow
import com.rudi.audioplayer.ui.theme.SkeuSpecular
import com.rudi.audioplayer.ui.theme.SkeuEmerald
import com.rudi.audioplayer.ui.theme.SkeuLightEmerald
import com.rudi.audioplayer.ui.theme.SkeuLightAmbientOcclusion
import com.rudi.audioplayer.ui.theme.SkeuLightHighlight
import com.rudi.audioplayer.ui.theme.SkeuLightShadow
import com.rudi.audioplayer.ui.theme.SkeuLightSpecular
import com.rudi.audioplayer.ui.theme.LocalIsDarkTheme
import com.rudi.audioplayer.ui.theme.Radius
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    uiState: PlaybackUiState,
    isFavorite: Boolean,
    currentRating: Int,
    onSetRating: (Int) -> Unit,
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
    abRepeatPointA: Long?,
    abRepeatPointB: Long?,
    onSetAbRepeatPointA: (Long) -> Unit,
    onSetAbRepeatPointB: (Long) -> Unit,
    onClearAbRepeat: () -> Unit,
    onGetBookmarks: (Long) -> List<com.rudi.audioplayer.data.Bookmark>,
    onAddBookmark: (Long, String, Long) -> Unit,
    onDeleteBookmark: (Long, String) -> Unit,
    equalizerState: EqualizerUiState,
    onOpenEqualizer: () -> Unit,
    onToggleEqualizerEnabled: (Boolean) -> Unit,
    onEqualizerBandChange: (Int, Short) -> Unit,
    onEqualizerPresetSelect: (Int) -> Unit,
    onEqualizerBoldPresetSelect: (EqualizerController.BoldPreset) -> Unit,
    audiobookModeEnabled: Boolean,
    onToggleAudiobookMode: (Boolean) -> Unit,
    visualizerEnabled: Boolean,
    visualizerSupported: Boolean,
    visualizerPermissionGranted: Boolean,
    visualizerBars: FloatArray,
    onOpenVisualizer: () -> Unit,
    onCloseVisualizer: () -> Unit,
    onToggleVisualizerEnabled: (Boolean) -> Unit,
    onRequestVisualizerPermission: () -> Unit,
    onBack: () -> Unit
) {
    val song = uiState.currentSong
    val haptic = LocalHapticFeedback.current
    // Batch 55 (Tactile polish) — hoisted here (was only computed inside GestureIndicatorBadge/
    // AlbumArtHero before) so the main transport row below can also branch on it: the play/pause
    // button was the single most-seen control that still rendered byte-identical between Apple
    // and Tactile (default M3 circular FilledIconButton, no shape/bevel difference at all).
    val isTactile = isTactileTheme()
    // Batch 58 — same hoist reasoning as isTactile above: the play/pause button and
    // GestureIndicatorBadge still rendered Skeu byte-identical to Apple (default M3 circle +
    // translucent 0.9f-alpha Surface) despite Skeu having had its own skeuEmboss() primitive
    // ready since Batch 57 — the exact gap Batch 57's own PROJECT_STATE entry flagged.
    val isSkeu = isSkeuTheme()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showVisualizerSheet by remember { mutableStateOf(false) }
    var showAdvancedSheet by remember { mutableStateOf(false) }
    var showAbRepeatBookmarkSheet by remember { mutableStateOf(false) }

    // --- Swipe gesture: brightness (left of album art) & audio volume (right of album art) ---
    val gestureScope = rememberCoroutineScope()
    val context = LocalContext.current
    val hintStore = remember(context) { OnboardingHintStore(context) }
    var showNowPlayingHint by remember { mutableStateOf(!hintStore.hasSeenNowPlayingHint()) }
    val activity = remember(context) { context.findActivity() }
    // Full 0-100% swing over a fixed 140dp of drag, regardless of how tall the gesture zone
    // itself renders — the old version divided by the zone's full 300dp height, so a normal
    // thumb swipe barely moved the value at all and felt like it needed a long, deep drag to
    // respond. This roughly doubles sensitivity for the same physical swipe distance.
    val density = LocalDensity.current
    val gestureRangePx = remember(density) { with(density) { 140.dp.toPx() } }
    var brightnessLevel by remember {
        mutableStateOf(
            activity?.window?.attributes?.screenBrightness
                ?.takeIf { it in 0f..1f } ?: 0.5f
        )
    }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }

    // The volume swipe controls the phone's actual system media volume (the same one the
    // hardware buttons and notification-shade slider control) via AudioManager — not
    // controller.setVolume(), which only scales this app's own output and never touches the
    // real system level. The separate slider further down (onSetVolume/uiState.volume) is a
    // distinct, deliberate in-app attenuation control and is left as-is.
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxSystemVolume = remember(audioManager) {
        (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
    }
    var systemVolumeFraction by remember {
        mutableStateOf(
            ((audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0).toFloat() / maxSystemVolume)
                .coerceIn(0f, 1f)
        )
    }

    fun applyBrightness(target: Float) {
        val clamped = target.coerceIn(0.02f, 1f)
        brightnessLevel = clamped
        val window = activity?.window ?: return
        val params = window.attributes
        params.screenBrightness = clamped
        window.attributes = params
    }

    fun applySystemVolume(target: Float) {
        val clamped = target.coerceIn(0f, 1f)
        systemVolumeFraction = clamped
        val level = (clamped * maxSystemVolume).roundToInt()
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
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
        AlbumArt(
            artworkUri = song?.uri,
            contentScale = ContentScale.Crop,
            showIcon = false,
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
                // Batch 112 — root cause TERPISAH dari Batch 111 (yang soal insets di luar
                // Scaffold): layar ini SUDAH di dalam Scaffold/NavHost, jadi sudah dapat
                // contentWindowInsets via `padding` di AppNavHost. Bug sebenarnya: Column ini
                // fillMaxSize() TANPA scroll, isinya fixed-height (hero art 300dp + hint banner
                // ~150dp kalau tampil + title/rating/waveform/tombol) — total gampang > tinggi
                // viewport asli setelah dipotong status/nav bar, apalagi 3-button nav (umum di
                // Android 15 ke bawah) yang makan tinggi layar riil vs gesture-nav (overlay
                // tipis, Android 16 test device) yang nyaris tidak makan ruang — match persis
                // observasi user "normal di 16, kacau di 15 ke bawah". Sebelumnya konten yang
                // overflow BUKAN discroll, tapi ke-clip diam-diam di tepi layar — baris tombol
                // transport (shuffle/prev/play/next/repeat), paling bawah dalam urutan Column,
                // paling sering jadi korban. `verticalScroll` di sini murni jaring pengaman:
                // kalau konten muat (layar tinggi/gesture-nav), scroll offset tetap 0, nol
                // perubahan visual; kalau tidak muat, sekarang bisa digeser bukan hilang.
                .verticalScroll(rememberScrollState())
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
            IconButton(onClick = { showAdvancedSheet = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Kontrol lanjutan",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        if (showNowPlayingHint) {
            Spacer(modifier = Modifier.height(8.dp))
            FeatureHintBanner(
                text = "Geser di kiri/kanan piringan buat atur kecerahan & volume HP. Ketuk ⋮ buat Sleep Timer, Kecepatan, dan Equalizer.",
                onDismiss = {
                    showNowPlayingHint = false
                    hintStore.markNowPlayingHintSeen()
                }
            )
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            // Left half of the whole row: swipe up/down to raise/lower screen brightness.
            // Sized to a true 50% of the available width — independent of however big the
            // vinyl art itself is — so the touch target is generous, not a thin edge sliver.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.5f)
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
                                applyBrightness(brightnessLevel - dragAmount / gestureRangePx)
                            }
                        )
                    }
            )

            // Right half of the whole row: swipe up/down to raise/lower the phone's actual
            // system media volume (not just this app's internal gain).
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.5f)
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
                                applySystemVolume(systemVolumeFraction - dragAmount / gestureRangePx)
                            }
                        )
                    }
            )

            // Vinyl sits centered on top of both zones. It gets first claim on touches within
            // its own bounds (its own pointerInput for horizontal swipe-next/prev), the same
            // way it already did before this change — only the leftover vertical drag outside
            // its bounds reaches the brightness/volume zones underneath.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = entranceScale.value
                        scaleY = entranceScale.value
                        alpha = entranceAlpha.value
                    }
            ) {
                AlbumArtHero(
                    artworkUri = song?.uri,
                    accentColor = animatedAccent,
                    onSwipeNext = onNext,
                    onSwipePrevious = onPrevious
                )
            }
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
        Text(
            song?.artist ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        StarRatingRow(rating = currentRating, onRate = onSetRating, accentColor = animatedAccent)

        Spacer(modifier = Modifier.height(24.dp))

        var sliderPosition by remember(uiState.position) { mutableStateOf(uiState.position.toFloat()) }
        val progressFraction = (sliderPosition / uiState.duration.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

        Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            WaveformSeekBar(
                seed = song?.id ?: 0L,
                progress = progressFraction,
                playedColor = animatedAccent,
                unplayedColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(32.dp)
            )
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSeek(sliderPosition.toLong())
                },
                valueRange = 0f..(uiState.duration.coerceAtLeast(1L).toFloat()),
                colors = SliderDefaults.colors(
                    thumbColor = animatedAccent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                formatDuration(uiState.position),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                // Roadmap #12 (Mode Audiobook/Podcast, Batch 93) — "menit tersisa" alih-alih
                // total durasi untuk file yang di-opt-in mode ini, format "-mm:ss" sama seperti
                // konvensi umum podcast player (Spotify/Apple/Google Podcasts) — universal tanpa
                // perlu kata tambahan, dan langsung beda dari total durasi biasa secara visual.
                if (audiobookModeEnabled) {
                    "-" + formatDuration((uiState.duration - uiState.position).coerceAtLeast(0))
                } else {
                    formatDuration(uiState.duration)
                },
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
            // Batch 55 — Tactile gets its own shape language here too (moderate rounded-square,
            // matching TactileShapes.medium, same "machined control" read as every other tactile
            // surface) instead of silently inheriting Apple's circular filledShape default; wrapped
            // in tactileEmboss() so the app's single most-used button reads as a lifted hardware
            // key (diagonal bevel + drop shadow), not just a flat colored disc like Apple's.
            val playPauseShape = if (isTactile || isSkeu) MaterialTheme.shapes.medium else CircleShape
            FilledIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayPause()
                },
                interactionSource = playPauseInteraction,
                shape = playPauseShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = animatedAccent,
                    // Batch 69: dulu `MaterialTheme.colorScheme.background` — warna latar
                    // HALAMAN, sama sekali tidak berkaitan dengan warna lingkaran tombol ini
                    // sendiri (animatedAccent, aksen dinamis per lagu). Kalau kebetulan
                    // keduanya senasib gelap (mode gelap + aksen gelap) atau senasib terang,
                    // ikon menyatu sempurna dengan lingkarannya -> "gak kelihatan sama
                    // sekali" / "box kosong". Fix: pola luminance yang sama persis dgn
                    // MiniPlayerBar.kt (accentContentColor) — kontras terhadap animatedAccent
                    // itu sendiri, bukan warna halaman.
                    contentColor = if (animatedAccent.luminance() > 0.55f) Color.Black else Color.White
                ),
                modifier = Modifier
                    .size(68.dp)
                    .then(
                        when {
                            isTactile -> Modifier.tactileEmboss(shape = playPauseShape, elevation = 10.dp)
                            isSkeu -> Modifier.skeuEmboss(shape = playPauseShape, elevation = 10.dp)
                            else -> Modifier
                        }
                    )
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
                    systemVolumeFraction <= 0f -> Icons.Default.VolumeOff
                    systemVolumeFraction < 0.5f -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeUp
                },
                value = systemVolumeFraction,
                accentColor = animatedAccent,
                label = "Volume HP"
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
            audiobookModeEnabled = audiobookModeEnabled,
            onDismiss = { showSpeedDialog = false },
            onSelect = onSetSpeed,
            onToggleCrossfade = onSetCrossfadeEnabled,
            onToggleAudiobookMode = onToggleAudiobookMode
        )
    }

    if (showQueueSheet) {
        QueueSheet(
            queue = uiState.queue,
            slotIds = uiState.queueSlotIds,
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

    if (showAbRepeatBookmarkSheet && song != null) {
        var bookmarks by remember(song.id) { mutableStateOf(onGetBookmarks(song.id)) }
        ABRepeatBookmarkSheet(
            songId = song.id,
            positionMs = uiState.position,
            pointAMs = abRepeatPointA,
            pointBMs = abRepeatPointB,
            bookmarks = bookmarks,
            onDismiss = { showAbRepeatBookmarkSheet = false },
            onSetPointA = onSetAbRepeatPointA,
            onSetPointB = onSetAbRepeatPointB,
            onClearAbRepeat = onClearAbRepeat,
            onSeek = onSeek,
            onAddBookmark = { label, positionMs ->
                onAddBookmark(song.id, label, positionMs)
                bookmarks = onGetBookmarks(song.id)
            },
            onDeleteBookmark = { bookmarkId ->
                onDeleteBookmark(song.id, bookmarkId)
                bookmarks = onGetBookmarks(song.id)
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

    if (showVisualizerSheet) {
        VisualizerSheet(
            enabled = visualizerEnabled,
            supported = visualizerSupported,
            permissionGranted = visualizerPermissionGranted,
            bars = visualizerBars,
            accentColor = animatedAccent,
            onDismiss = {
                showVisualizerSheet = false
                onCloseVisualizer()
            },
            onToggleEnabled = onToggleVisualizerEnabled,
            onRequestPermission = onRequestVisualizerPermission
        )
    }

    if (showAdvancedSheet) {
        AdvancedControlsSheet(
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            playbackSpeed = uiState.playbackSpeed,
            volume = uiState.volume,
            onSetVolume = onSetVolume,
            onDismiss = { showAdvancedSheet = false },
            // Fix hierarki tombol (feedback user, screenshot layar Now Playing): top bar tadinya
            // 5 ikon berbobot sama (Tutup/Favorit/Antrean/Lirik/Lanjutan) — membingungkan karena
            // tidak ada yang menonjol sebagai aksi utama. Antrean & Lirik (dipakai situasional,
            // bukan tiap sesi dengar) sekarang gabung ke sheet "Kontrol Lanjutan" yang sama,
            // persis pola yang sudah dipakai Timer/Kecepatan/Equalizer di sheet ini (lihat
            // doc-comment fungsi ini). Top bar sekarang cuma 3 ikon: Tutup, Favorit, Lanjutan.
            onOpenQueue = {
                showAdvancedSheet = false
                showQueueSheet = true
            },
            onOpenLyrics = {
                showAdvancedSheet = false
                showLyricsSheet = true
            },
            onOpenSleepTimer = {
                showAdvancedSheet = false
                showSleepTimerDialog = true
            },
            onOpenSpeed = {
                showAdvancedSheet = false
                showSpeedDialog = true
            },
            onOpenEqualizer = {
                showAdvancedSheet = false
                onOpenEqualizer()
                showEqualizerSheet = true
            },
            onOpenAbRepeatBookmark = {
                showAdvancedSheet = false
                showAbRepeatBookmarkSheet = true
            },
            onOpenVisualizer = {
                showAdvancedSheet = false
                onOpenVisualizer()
                showVisualizerSheet = true
            }
        )
    }
}

/** Houses the controls a casual listener rarely touches mid-song — antrean, lirik, sleep timer,
 * playback speed, equalizer, and the in-app volume attenuation — behind one "Lanjutan" entry
 * point instead of crowding the main Now Playing top bar with equal-weight icons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedControlsSheet(
    sleepTimerRemainingMs: Long?,
    playbackSpeed: Float,
    volume: Float,
    onSetVolume: (Float) -> Unit,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenAbRepeatBookmark: () -> Unit,
    onOpenVisualizer: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent
    ) {
        Column(modifier = Modifier.fillMaxWidth().frostedGlass()) {
            Text(
                "Kontrol Lanjutan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            AdvancedControlRow(
                icon = Icons.Default.QueueMusic,
                label = "Antrean Putar",
                value = null,
                onClick = onOpenQueue
            )
            AdvancedControlRow(
                icon = Icons.Default.Article,
                label = "Lirik",
                value = null,
                onClick = onOpenLyrics
            )
            AdvancedControlRow(
                icon = Icons.Default.Timer,
                label = "Sleep Timer",
                value = if (sleepTimerRemainingMs != null) "Aktif" else "Nonaktif",
                onClick = onOpenSleepTimer
            )
            AdvancedControlRow(
                icon = Icons.Default.Speed,
                label = "Kecepatan Putar",
                value = "${playbackSpeed}x",
                onClick = onOpenSpeed
            )
            AdvancedControlRow(
                icon = Icons.Default.Equalizer,
                label = "Equalizer",
                value = null,
                onClick = onOpenEqualizer
            )
            AdvancedControlRow(
                icon = Icons.Default.Repeat,
                label = "Repeat A-B & Bookmark",
                value = null,
                onClick = onOpenAbRepeatBookmark
            )
            AdvancedControlRow(
                icon = Icons.Default.GraphicEq,
                label = "Visualizer Audio",
                value = null,
                onClick = onOpenVisualizer
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Text(
                "Peredam Dalam Aplikasi (bukan volume HP)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val volumeIcon = when {
                    volume <= 0f -> Icons.Default.VolumeOff
                    volume < 0.5f -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeUp
                }
                Icon(volumeIcon, contentDescription = "Peredam dalam aplikasi", tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = volume,
                    onValueChange = onSetVolume,
                    onValueChangeFinished = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AdvancedControlRow(icon: ImageVector, label: String, value: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

/**
 * Small floating pill shown while dragging the brightness/volume swipe zones,
 * mirroring the transient overlay pattern used by most media/video apps.
 */
/** A deterministic (not real-audio-analyzed — see project README) pseudo-waveform, seeded by
 * song ID so the same song always renders the same bar pattern rather than reshuffling on
 * every recomposition. Purely decorative visual layer; an invisible Slider drawn on top of
 * this handles all actual seek interaction, so seeking behavior is completely unchanged. */
@Composable
private fun WaveformSeekBar(
    seed: Long,
    progress: Float,
    playedColor: Color,
    unplayedColor: Color,
    modifier: Modifier = Modifier
) {
    val barHeights = remember(seed) {
        val random = kotlin.random.Random(seed)
        List(BAR_COUNT) { 0.25f + random.nextFloat() * 0.75f }
    }

    Canvas(modifier = modifier) {
        val barWidth = size.width / BAR_COUNT
        val gap = barWidth * 0.35f
        val playedBars = (progress * BAR_COUNT).toInt()

        barHeights.forEachIndexed { index, heightFraction ->
            val barHeightPx = size.height * heightFraction
            drawRoundRect(
                color = if (index < playedBars) playedColor else unplayedColor,
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = index * barWidth + gap / 2,
                    y = (size.height - barHeightPx) / 2
                ),
                size = androidx.compose.ui.geometry.Size(barWidth - gap, barHeightPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
        }
    }
}

private const val BAR_COUNT = 48

@Composable
private fun StarRatingRow(rating: Int, onRate: (Int) -> Unit, accentColor: Color) {
    val haptic = LocalHapticFeedback.current
    Row(horizontalArrangement = Arrangement.Center) {
        for (star in 1..5) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRate(if (rating == star) 0 else star)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Beri rating $star bintang",
                    tint = if (star <= rating) accentColor else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun GestureIndicatorBadge(icon: ImageVector, value: Float, accentColor: Color, label: String? = null) {
    val isTactile = isTactileTheme()
    // Batch 58 — was falling into the Apple-else branch (translucent 0.9f-alpha Surface, another
    // literal glassmorphism cue) for Skeu; now gets the same opaque + embossed treatment Tactile
    // already had, consistent with the rest of this batch's frostedGlass()/skeuEmboss() fixes.
    val isSkeu = isSkeuTheme()
    val isPanelTheme = isTactile || isSkeu
    Surface(
        modifier = when {
            isTactile -> Modifier.tactileEmboss(shape = RoundedCornerShape(Radius.xl), elevation = 8.dp)
            isSkeu -> Modifier.skeuEmboss(shape = RoundedCornerShape(Radius.xl), elevation = 8.dp)
            else -> Modifier
        },
        shape = RoundedCornerShape(Radius.xl),
        color = if (isPanelTheme) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        // Batch 48/49 lesson: explicit contentColor, never rely on the Transparent fallback.
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = if (isPanelTheme) 0.dp else 6.dp,
        shadowElevation = if (isPanelTheme) 0.dp else 4.dp
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
            // Only the volume badge passes a label — it disambiguates this swipe (the phone's
            // real system volume) from the separate in-app slider further down the screen,
            // which the two shared no visual distinction for before.
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/** Apple Music-style hero art: a large rounded-square image with a soft ambient glow
 * (tinted by the same accent color already extracted from this song's artwork) instead of
 * the old spinning vinyl. Horizontal swipe-to-skip gesture logic is unchanged from before. */
@Composable
private fun AlbumArtHero(
    artworkUri: Uri?,
    accentColor: Color,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var totalDrag by remember { mutableStateOf(0f) }

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
        val isTactile = isTactileTheme()
        // Batch 59 — same gap as HomeScreen/LibraryScreen/MiniPlayerBar: this hero art (the
        // single largest, most-looked-at surface on the whole screen) was Tactile-only, Skeu
        // fell into the generic Apple shadow-only branch below with no bevel of its own at all.
        val isSkeu = isSkeuTheme()
        val isPanelTheme = isTactile || isSkeu
        // Batch 74 — fix: this manual draw (unlike skeuEmboss()/tactileEmboss(), which both
        // already branch on LocalIsDarkTheme) hardcoded dark-only tokens (TactileHighlight/
        // TactileShadow, SkeuHighlight/SkeuShadow/SkeuAmbientOcclusion/SkeuSpecular/
        // SkeuInnerGroove) with no light-mode branch at all — since Batch 61 made Tactile/Skeu
        // fully autonomous per light/dark mode, this hero art (the single largest surface on
        // the whole screen) has been silently drawing dark bevel colors over a light-mode panel
        // this whole time. Fixed below via isDark + light-token fallback, matching the pattern
        // skeuEmboss()/tactileEmboss() already use.
        val isDark = LocalIsDarkTheme.current
        // Batch 52: recolored again for the literal Midnight Blue spec
        // (compose-skeuomorphism-lite-midnight-blue.md) — same drawn top-down shadow +
        // vertical-gradient bevel border technique kept from Batch 45/46/49-51, no code changes
        // here at all; TactileHighlight/TactileShadow are plain white/black-based again this
        // batch (see Color.kt), so this hero art picks up the new palette automatically through
        // those same two token references.
        val heroShape = if (isPanelTheme) MaterialTheme.shapes.large else RoundedCornerShape(Radius.hero)
        Box(
            modifier = Modifier
                .size(300.dp)
                .blur(90.dp)
                .background(accentColor.copy(alpha = 0.38f), CircleShape)
        )
        AlbumArt(
            artworkUri = artworkUri,
            modifier = Modifier
                .size(280.dp)
                .then(
                    when {
                        isTactile -> {
                            val heroHighlight = if (isDark) TactileHighlight else TactileLightHighlight
                            val heroShadow = if (isDark) TactileShadow else TactileLightShadow
                            Modifier
                                .drawBehind {
                                    val outline = heroShape.createOutline(size, layoutDirection, this)
                                    val outlinePath = Path().apply { addOutline(outline) }
                                    translate(top = 9.dp.toPx()) {
                                        drawPath(outlinePath, color = heroShadow.copy(alpha = if (isDark) 0.55f else 0.30f))
                                    }
                                }
                                .clip(heroShape)
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        // Batch 55 — was verticalGradient, the one remaining spot in the
                                        // whole Tactile identity still drawing top-down light instead of
                                        // spec §9's diagonal top-left -> bottom-right (BlurUtils.kt's
                                        // edgeBrush and TactileDepth.kt's tactileEmboss() border both
                                        // already use linearGradient's default diagonal — this hero art
                                        // border was the one inconsistent leftover from Batch 45/46,
                                        // predating the diagonal rule adopted in Batch 53).
                                        Brush.linearGradient(
                                            listOf(
                                                heroHighlight.copy(alpha = if (isDark) 0.12f else heroHighlight.alpha),
                                                heroShadow.copy(alpha = if (isDark) 0.32f else heroShadow.alpha)
                                            )
                                        )
                                    ),
                                    heroShape
                                )
                                // Localized accent glow on the hero art is spec-sanctioned (§9: "Use
                                // [glow] for… selected states… important tactile edges") since this
                                // is the one always-active/selected surface on the whole screen —
                                // alpha trimmed from the old 0.5f for restraint per §9/§13.
                                .shadow(elevation = 18.dp, shape = heroShape, spotColor = accentColor.copy(alpha = 0.42f))
                        }
                        isSkeu -> {
                            // Batch 79 — NEUMORPHISM upgrade: sama arsitektur dual-shadow dgn
                            // skeuEmboss() (TactileDepth.kt, Batch 79) — sisi gelap kanan-bawah
                            // (AO dekat + shadow jauh, 3 layer offset+alpha bertingkat), sisi
                            // terang kiri-atas (specular dekat + highlight jauh, 2 layer) — TIDAK
                            // ADA lagi border/inner-groove sama sekali (neumorphism generik tidak
                            // punya garis batas, kedalaman murni dari bayangan). Manual draw di
                            // sini (bukan lewat skeuEmboss() langsung) tetap dipertahankan karena
                            // Box ini juga membawa .shadow() accent glow per-lagu di bawah, yang
                            // perlu tetap jadi layer terpisah/paling akhir.
                            // Batch 81 — fix 2 hal: (1) sisi TERANG dulu ada di drawBehind TERPISAH
                            // SETELAH .clip(heroShape) (beda dari sisi gelap yg SEBELUM .clip()) —
                            // artinya sisi terang selama ini kepotong tepat di tepi shape, tidak
                            // pernah benar-benar "meluber ke luar" sebagai bayangan lembut kayak
                            // sisi gelap, beda arsitektur dari skeuEmboss() sendiri yg gambar KEDUA
                            // sisi dalam 1 drawBehind sebelum .clip(). Disatukan di bawah, sama
                            // pola dgn skeuEmboss(). (2) clipRect() halo ditambahkan (fix "Ambient
                            // Light gak bocor", instruksi user yg belum tersentuh Batch 79/80) —
                            // hero art ini panel TERBESAR di app, jadi juga yg paling berisiko
                            // numpang-nimpa MiniPlayerBar/tombol kontrol di bawahnya kalau tidak
                            // dibatasi.
                            val heroAo = if (isDark) SkeuAmbientOcclusion else SkeuLightAmbientOcclusion
                            val heroShadow = if (isDark) SkeuShadow else SkeuLightShadow
                            val heroSpecular = if (isDark) SkeuSpecular else SkeuLightSpecular
                            val heroHighlight = if (isDark) SkeuHighlight else SkeuLightHighlight
                            val emerald = if (isDark) SkeuEmerald else SkeuLightEmerald
                            // Batch 80 — fix: Batch 79's emerald di hero art cuma lerp-blend 14%
                            // ke arah heroSpecular (putih/perak nyaris opaque) — di layar HP nyaris
                            // tak berubah dari putih polos (user: "yang kelihatan cuman Titanium
                            // dominan, mana zamrudnya??"). Sekarang jadi radial glint TERPISAH
                            // (warna emerald murni, bukan campuran) di pojok kiri-atas, alpha tetap
                            // & jauh lebih tinggi (0.35f/0.42f) — permanen (hero art statis, tidak
                            // ada state pressed spt skeuEmboss()), genuinely kebaca sebagai titik
                            // hijau di logam titanium, bukan cuma teknis-ada-di-kode.
                            val heroEmeraldAlpha = if (isDark) 0.35f else 0.42f
                            Modifier
                                .drawBehind {
                                    val outline = heroShape.createOutline(size, layoutDirection, this)
                                    val outlinePath = Path().apply { addOutline(outline) }
                                    // Halo tetap (18.dp) — offset terjauh yg dipakai di bawah cuma
                                    // 14.dp (literal, bukan proporsional ke param elevation kayak
                                    // skeuEmboss(), krn hero art ini ukurannya selalu tetap 280.dp),
                                    // jadi 18.dp cukup longgar utk tidak memotong bentuk bayangan
                                    // sendiri, sekaligus jadi batas tegas yg dijamin tidak dilewati.
                                    val haloPx = 18.dp.toPx()
                                    clipRect(
                                        left = -haloPx,
                                        top = -haloPx,
                                        right = size.width + haloPx,
                                        bottom = size.height + haloPx
                                    ) {
                                        // Sisi GELAP — kanan-bawah, 3 layer offset makin jauh +
                                        // alpha makin tipis (faux-blur bertingkat, sama teknik
                                        // skeuEmboss()).
                                        translate(left = 3.dp.toPx(), top = 3.dp.toPx()) {
                                            drawPath(outlinePath, color = heroAo)
                                        }
                                        translate(left = 8.dp.toPx(), top = 8.dp.toPx()) {
                                            drawPath(outlinePath, color = heroShadow.copy(alpha = (if (isDark) 0.40f else heroShadow.alpha) * 0.75f))
                                        }
                                        translate(left = 14.dp.toPx(), top = 14.dp.toPx()) {
                                            drawPath(outlinePath, color = heroShadow.copy(alpha = (if (isDark) 0.40f else heroShadow.alpha) * 0.35f))
                                        }
                                        // Sisi TERANG — kiri-atas, 2 layer, murni Titanium/Silver.
                                        // Batch 81: dipindah ke sini (sebelum .clip()), sisi gelap
                                        // di atas — dulu di drawBehind terpisah SETELAH .clip(),
                                        // jadi tak pernah bisa meluber sama sekali (lihat komentar
                                        // Batch 81 di atas).
                                        translate(left = -3.dp.toPx(), top = -3.dp.toPx()) {
                                            drawPath(outlinePath, color = heroSpecular.copy(alpha = if (isDark) 0.35f else heroSpecular.alpha * 0.6f))
                                        }
                                        translate(left = -8.dp.toPx(), top = -8.dp.toPx()) {
                                            drawPath(outlinePath, color = heroHighlight.copy(alpha = (if (isDark) 0.16f else heroHighlight.alpha) * 0.7f))
                                        }
                                    }
                                }
                                .clip(heroShape)
                                .drawBehind {
                                    // Zamrud — glint bulat kecil terpisah, pojok kiri-atas, warna
                                    // murni (bukan blend) supaya genuinely kebaca hijau. Sengaja
                                    // tetap SETELAH .clip() (beda dari dual-shadow di atas) — ini
                                    // permata di PERMUKAAN panel, bukan bayangan yg perlu meluber.
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(emerald.copy(alpha = heroEmeraldAlpha), Color.Transparent),
                                            center = Offset(size.width * 0.16f, size.height * 0.14f),
                                            radius = size.minDimension.coerceAtLeast(1f) * 0.28f
                                        )
                                    )
                                }
                                .shadow(elevation = 18.dp, shape = heroShape, spotColor = accentColor.copy(alpha = 0.42f))
                        }
                        else -> Modifier.shadow(elevation = 28.dp, shape = heroShape, spotColor = accentColor.copy(alpha = 0.45f))
                    }
                )
                .clip(heroShape)
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
    audiobookModeEnabled: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Float) -> Unit,
    onToggleCrossfade: (Boolean) -> Unit,
    onToggleAudiobookMode: (Boolean) -> Unit
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

                // Roadmap #12 (Mode Audiobook/Podcast, Batch 93) — per-song opt-in, scoped to
                // whichever song is loaded when this dialog is open (PlayerViewModel keys the
                // saved state off currentSong.id, not a global setting).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mode Audiobook/Podcast", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Ingat kecepatan & posisi khusus lagu ini, terpisah dari lagu lain",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(checked = audiobookModeEnabled, onCheckedChange = onToggleAudiobookMode)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Transisi Antar Lagu",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Gapless has always been the actual playback engine's default behavior —
                // ExoPlayer decodes a real back-to-back playlist with zero re-buffering
                // between tracks whenever crossfade doesn't touch volume. The only thing
                // that was missing was ever telling the user this exists; before this, "off"
                // was just the crossfade switch's unlabeled resting state.
                TransitionModeOption(
                    title = "Gapless (Murni)",
                    subtitle = "Sambung langsung tanpa jeda atau perubahan volume — persis seperti file aslinya",
                    selected = !crossfadeEnabled,
                    onClick = { onToggleCrossfade(false) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Batch 102 — subtitle diperbarui: sebelum ini cuma volume 1 pemutar yang
                // dilandaikan turun-naik di sekitar titik ganti lagu (jeda senyap singkat tetap
                // ada, cuma disamarkan). Sekarang lagu berikutnya benar-benar mulai main
                // (overlap) SEBELUM lagu ini habis — dua sumber suara sungguhan tumpang tindih,
                // bukan cuma efek volume. Lihat CrossfadeEngine.kt.
                TransitionModeOption(
                    title = "Fade Halus",
                    subtitle = "Lagu berikutnya mulai main sebelum lagu ini habis, saling menumpuk lalu bertukar halus",
                    selected = crossfadeEnabled,
                    onClick = { onToggleCrossfade(true) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun TransitionModeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
