package com.rudi.audioplayer.ui

import android.content.Intent
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.hapticfeedback.HapticFeedback
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.Player
import com.rudi.audioplayer.data.Playlist
import com.rudi.audioplayer.ui.lyrics.LyricsViewModel
import com.rudi.audioplayer.playback.EqualizerController
import com.rudi.audioplayer.playback.EqualizerUiState
import com.rudi.audioplayer.playback.PlaybackProgress
import com.rudi.audioplayer.playback.PlaybackUiState
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.ui.theme.neuSurface
import com.rudi.audioplayer.ui.theme.tactileEmboss
import com.rudi.audioplayer.ui.theme.skeuEmboss
import com.rudi.audioplayer.ui.theme.isTactileTheme
import com.rudi.audioplayer.ui.theme.isSkeuTheme
import com.rudi.audioplayer.ui.theme.isCalmRetroTheme
import com.rudi.audioplayer.ui.theme.calmAberration
import com.rudi.audioplayer.ui.theme.Radius
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    uiState: PlaybackUiState,
    // Batch 353 (Opsi A, PENDING_FixGlobalLagRecomposition.md) — dikoleksi LOKAL oleh
    // PlaybackProgressRow & WithLivePlaybackProgress di bawah, TIDAK di top-level fungsi ini,
    // supaya tick posisi tiap detik tidak ikut memaksa seluruh NowPlayingScreen (dan
    // AppNavHost pemanggilnya di MainActivity.kt) recompose.
    playbackProgress: StateFlow<PlaybackProgress>,
    isFavorite: Boolean,
    currentRating: Int,
    onSetRating: (Int) -> Unit,
    // Batch 354 — sama pola playbackProgress di atas (Batch 353): StateFlow mentah, dikoleksi
    // LOKAL oleh SleepTimerDialog (teks countdown) & AdvancedControlsSheet (status Aktif/
    // Nonaktif) di bawah, TIDAK di top-level fungsi ini — tick 1 detik dulu bocor sampai
    // AppNavHost (MainActivity.kt) lewat parameter Long? polos di sini.
    sleepTimerRemaining: StateFlow<Long?>,
    accentColor: Color?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    // Batch 489 — QA checklist gap #1 (kontrol Stop eksplisit), lihat komentar
    // PlayerViewModel.stopPlayback(). Diletakkan di sheet "Kontrol Lanjutan" (bukan Row transport
    // utama Shuffle/Prev/Play/Next/Repeat) supaya 0 mengubah layout 5-ikon yang sudah di-tuning
    // (SpaceEvenly, hierarki ukuran, dst — lihat komentar Batch 170/224/226 di Row itu).
    onStopPlayback: () -> Unit,
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
    // Batch 354 — sama alasan sleepTimerRemaining di atas, malah lebih kritis: ~15fps (bukan
    // 1 tick/detik) + dulu menembus 4 layer composable (AppNavHost -> NowPlayingScreen ->
    // VisualizerSheet -> SpectrumBars) sebelum akhirnya dipakai. Dikoleksi LOKAL di SpectrumBars
    // saja (VisualizerSheet.kt) — VisualizerSheet sendiri & fungsi ini cuma meneruskan referensi.
    visualizerBars: StateFlow<FloatArray>,
    onOpenVisualizer: () -> Unit,
    onCloseVisualizer: () -> Unit,
    onToggleVisualizerEnabled: (Boolean) -> Unit,
    onRequestVisualizerPermission: () -> Unit,
    // Gap List "Wajib" #1 (Tag Editor) — fire-and-forget, sama pola onSaveLyrics/onAddBookmark
    // di atas: hasil sukses/gagal muncul lewat Snackbar infoMessage/actionErrorMessage yang
    // sudah ada di MainActivity, bukan callback result langsung ke sini.
    onSaveSongTags: (com.rudi.audioplayer.data.Song, com.rudi.audioplayer.data.Id3TagWriter.EditableTags) -> Unit,
    // Batch 358 — ikon "Plus" baru di samping judul (lihat Row baru di body fungsi ini) reuse
    // AddToPlaylistDialog (PlaylistScreen.kt) + pola callback yang SAMA PERSIS dgn LibraryScreen.kt
    // (addToPlaylist lambda di sana), bukan mekanisme baru. 3 param ini wajib supaya sheet itu
    // bisa dipanggil dari sini juga.
    playlists: List<Playlist>,
    onAddSongToPlaylist: (String, Long) -> Boolean,
    onCreatePlaylist: (String) -> Playlist,
    // Roadmap #5 (Ringtone Cutter) — fire-and-forget sama pola onSaveSongTags di atas.
    onCutRingtone: (
        com.rudi.audioplayer.data.Song,
        com.rudi.audioplayer.data.RingtoneCutter.TrimRange,
        com.rudi.audioplayer.data.RingtoneEncoder.Destination,
        String
    ) -> Unit,
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
    // Batch 129 — hoist sama alasan isTactile/isSkeu di atas: tombol play/pause ini persis
    // "tombol utama"/`.calm-play-button` yang ditarget spec markdown user.
    val isCalmRetro = isCalmRetroTheme()
    // Batch 537 (T11) — flag dialog/sheet RINGAN pakai `rememberSaveable` supaya tidak hilang saat
    // rotasi (Activity di-recreate; manifest tanpa `configChanges`). SENGAJA tetap `remember`:
    // Equalizer, Visualizer (capture OS ~15fps), SongInfoEdit (state form), RingtoneCutter (decode
    // audio) — sheet berat/ber-efek-samping tidak dibuka ulang otomatis (keputusan user: paling aman).
    var showSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
    var showSpeedDialog by rememberSaveable { mutableStateOf(false) }
    var showQueueSheet by rememberSaveable { mutableStateOf(false) }
    var showLyricsSheet by rememberSaveable { mutableStateOf(false) }
    var showAddToPlaylistDialog by rememberSaveable { mutableStateOf(false) } // Batch 358
    var showRatingDialog by rememberSaveable { mutableStateOf(false) } // Batch 360
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showVisualizerSheet by remember { mutableStateOf(false) }
    var showAdvancedSheet by rememberSaveable { mutableStateOf(false) }
    var showAbRepeatBookmarkSheet by rememberSaveable { mutableStateOf(false) }
    var showSongInfoEditSheet by remember { mutableStateOf(false) }
    var showRingtoneCutterSheet by remember { mutableStateOf(false) }

    // --- Swipe gesture: brightness (left of album art) & audio volume (right of album art) ---
    val gestureScope = rememberCoroutineScope()
    val context = LocalContext.current
    // Batch 248 — wire Lyrics offline-first (Batch 243-247) ke NowPlayingScreen. ViewModel
    // di-hoist di sini (bukan di dalam blok `showLyricsSheet` di bawah) supaya request auto-fetch
    // jalan begitu lagu berganti (debounce 5 detik di ViewModel sendiri yg jamin tidak nembak
    // API tiap transisi cepat), bukan baru mulai fetch pas user buka sheet — lirik sudah siap
    // duluan saat sheet dibuka.
    val lyricsViewModel: LyricsViewModel = viewModel(factory = LyricsViewModel.factory(context))
    val lyricsAutoState by lyricsViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(song?.id) {
        song?.let { lyricsViewModel.loadLyrics(it.artist, it.title, it.album) }
    }
    // Batch 341 — user eksplisit lapor (screenshot NowPlayingScreen): banner onboarding "bisa
    // kena dismiss permanen dan gak balik lagi" — begitu di-tap X sekali, hilang selamanya, 0
    // cara buka lagi kalau lupa isinya. GANTI TOTAL mekanismenya: bukan lagi auto-tampil-sekali
    // + persist "sudah pernah lihat" (`OnboardingHintStore.hasSeenNowPlayingHint()`/
    // `markNowPlayingHintSeen()`, dihapus dari file ini — class-nya sendiri TETAP ada & TIDAK
    // diubah, masih dipakai `LibraryScreen.kt` utk hint lain, ZERO-REFACTOR) — SEKARANG murni
    // toggle biasa dikontrol tombol info permanen di Row atas (samping ikon favorit, lihat
    // Row bawah), bisa dibuka/tutup KAPAN SAJA tanpa batas, mulai dari tersembunyi (`false`).
    // Efek samping yang SENGAJA disertakan: karena hint sekarang cuma tampil atas aksi eksplisit
    // user (bukan lagi otomatis kejadian di setiap first-launch tanpa diminta), seluruh saga
    // "art box menyusut buat kompensasi ruang scroll selama hint numpang tampil" (Batch 336-338,
    // cabang `showNowPlayingHint -> 260.dp` di `albumArtBoxHeight` bawah) TIDAK relevan lagi —
    // dihapus di titik itu (lihat komentar di sana). Cabang layar pendek (`screenHeightDp <
    // 640.dp`, Batch 336) TETAP ada — itu fix legitimate terpisah, tidak terkait hint sama sekali.
    var showNowPlayingHint by rememberSaveable { mutableStateOf(false) }
    val activity = remember(context) { context.findActivity() }
    // Full 0-100% swing over a fixed 140dp of drag, regardless of how tall the gesture zone
    // itself renders — the old version divided by the zone's full 300dp height, so a normal
    // thumb swipe barely moved the value at all and felt like it needed a long, deep drag to
    // respond. This roughly doubles sensitivity for the same physical swipe distance.
    val density = LocalDensity.current
    val gestureRangePx = remember(density) { with(density) { 140.dp.toPx() } }
    // Batch 336 — root cause beda level dari Batch 335 (bukan overscroll glow, tapi safety
    // net Batch 112/334 sendiri regresi): header (Row + hint banner opsional + art box FIXED
    // 300dp) tidak ikut discroll, jadi di layar pendek (landscape/split-screen) sisa ruang
    // buat Column konten (weight+verticalScroll) bisa kepepet sampai nyaris 0dp — transport
    // row jadi TIDAK kejangkau walau discroll. Sebelum Batch 334 semuanya 1 Column scroll jadi
    // art ikut ke-scroll off-screen; sekarang art dikunci fixed supaya gesture brightness/
    // volume-nya lolos dari nested-scroll conflict (Batch 334). Fix: susutkan TINGGI art box
    // (bukan strukturnya — gesture zone TETAP di luar ancestor scrollable, tidak regresi
    // Batch 334) secara proporsional saat layar pendek, supaya sisa ruang scroll cukup buat
    // transport row selalu kejangkau. (Update Batch 338 di bawah: layar normal SEKARANG BISA
    // ikut menyusut juga, tapi HANYA sementara selama hint banner tampil — bukan lagi 100%
    // tetap 300dp seperti klaim awal batch ini.)
    // Batch 546 — lint `ConfigurationScreenWidthHeight`: sumber ukuran jendela dipindah dari
    // `LocalConfiguration.current.screen{Width,Height}Dp` ke `LocalWindowInfo.current.containerDpSize`
    // (pengganti yang disarankan lint; targetSdk 36 -> kedua sumber sama-sama ukuran jendela PENUH,
    // jadi hitungan di bawah tak bergeser selain pembulatan dp). Nama/pemakaian variabel TETAP.
    val windowSizeDp = LocalWindowInfo.current.containerDpSize
    val screenHeightDp = windowSizeDp.height
    // Batch 338 — BUG FIX lanjutan (laporan user + konfirmasi: hint banner MASIH nongol, belum
    // pernah di-dismiss): sebelumnya cuma layar pendek (<640dp) yang dapet art box lebih kecil.
    // Tapi di layar NORMAL sekalipun, jumlah tinggi fixed (art 300dp + header + hint banner
    // ~150dp, Batch 112) + konten scrollable (judul s/d transport) bisa TETAP melebihi viewport
    // selama hint SEDANG tampil (kondisi sekali-tampil, sementara) — scroll jadi kepicu padahal
    // user anggap layarnya "normal", harusnya muat tanpa scroll sama sekali. Fix: pas
    // `showNowPlayingHint == true`, susutkan art box juga (260dp, bukan cuma saat layar pendek)
    // — begitu user dismiss (SEKALI, permanen via hintStore, tidak muncul lagi selamanya),
    // art balik penuh 300dp seperti biasa. Layar pendek (<640dp) tetap pakai rumus proporsional
    // lama (Batch 336) — dua kondisi ini independen, yang paling kecil yang menang.
    // Batch 341 — cabang `showNowPlayingHint -> 260.dp` (Batch 338) DIHAPUS: hint sekarang
    // murni opt-in lewat tombol info (Row atas), bukan lagi otomatis tampil tiap first-launch
    // tanpa diminta — 0 lagi alasan buat preemptive-susutkan art box FIXED cuma krn hint
    // "kebetulan lagi kebuka". Cabang layar pendek di bawah (Batch 336) TIDAK disentuh, itu
    // fix legitimate terpisah (device pendek beneran, tidak terkait hint sama sekali).
    val albumArtBoxHeight = when {
        screenHeightDp < 640.dp -> (screenHeightDp * 0.28f).coerceIn(160.dp, 300.dp)
        else -> 300.dp
    }
    // Batch 346 — sejak batch ini, `albumArtBoxHeight` di atas TIDAK LAGI dipakai LANGSUNG sbg
    // tinggi final Box piringan — perannya berubah jadi fallback PRA-pengukuran saja (lihat
    // `dynamicArtSize` dekat Box piringan bawah). Formula/komentar di atas TIDAK diubah sama
    // sekali (masih valid persis sebagai fallback layar-pendek), murni PERAN-nya yang berubah.
    // `contentGroupHeightPx` — hasil ukur Column pembungkus grup konten (hint s/d baris waktu,
    // lihat komentar `onGloballyPositioned` di sana) dalam pixel, 0 = belum pernah terukur.
    var contentGroupHeightPx by remember { mutableIntStateOf(0) }
    var brightnessLevel by remember {
        mutableFloatStateOf(
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
    // Batch 132 — fix laporan user (screenshot): CTA/wash Calm Retro ikut warna dominan album
    // art per-lagu (accentColor), Muted Sage & aberrasi jadi tenggelam/tak kebaca di lagu
    // beraksen kuat (merah dst). Identitas ini sudah terkunci gelap total (Batch 128) — locknya
    // sekarang meluas ke accent juga: SELALU CalmRetroAccent literal, tidak pernah ikut album
    // art, konsisten dgn filosofi "tidak ikut-ikutan" identitas ini. Ini SATU titik kontrol
    // (animatedAccent dipakai jadi seluruh CTA/wash/rating di bawah), jadi cukup 1 baris.
    val animatedAccent by animateColorAsState(
        targetValue = if (isCalmRetro) fallback else (accentColor ?: fallback),
        animationSpec = tween(700),
        label = "accentColor"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // v3 upgrade — audit item "Do's" spec palet_warna_calm_retro_v3.md yang Batch 133
        // sengaja tunda ("blur album-art 80dp/15% sebagai backdrop jauh Now Playing"): backdrop
        // generik ini (semua identitas, sejak Batch 67) sudah ADA tapi angkanya beda (60dp/50%
        // alpha) — bukan gap fungsional (Calm Retro sudah dapat backdrop blur sejak awal, sama
        // seperti identitas lain), murni beda intensitas. Karena ini bagian "Do's" (saran, bukan
        // salah satu 4 Pilar wajib) dan angka literal spec eksplisit beda dari nilai generik
        // project, Calm Retro dapat angkanya sendiri di sini (mengaburkan 80dp, opacity 15% —
        // jauh lebih halus dari 50% generik, sesuai nada "jauh"/subtle spec) — identitas lain
        // TIDAK disentuh, tetap 60dp/50% seperti sebelumnya.
        val backdropBlurRadius = if (isCalmRetro) 80.dp else 60.dp
        val backdropAlpha = if (isCalmRetro) 0.15f else 0.5f
        AlbumArt(
            artworkUri = song?.uri,
            contentScale = ContentScale.Crop,
            showIcon = false,
            modifier = Modifier
                .fillMaxSize()
                .blur(backdropBlurRadius)
                .alpha(backdropAlpha)
        )

        // Batch 398 — `NowPlayingScreen(...)` (fungsi ini) adalah 1 badan composable BESAR
        // (~1400 baris) yang membaca BANYAK state langsung di scope yang sama (dialog show/hide,
        // queue, sleep timer, dst — live position sendiri SUDAH diisolasi terpisah sejak Batch
        // 353, lihat `WithLivePlaybackProgress` di bawah, tapi state lain TIDAK). Brush wash
        // di bawah cuma bergantung 2 input nyata (`animatedAccent`,
        // `MaterialTheme.colorScheme.background`) yang jauh lebih jarang berubah drpd frekuensi
        // recomposition scope ini secara keseluruhan — pola identik `identityRootBrush` (Batch
        // 392, `MainActivity.kt`). Warna background dibaca ke `val` biasa DI LUAR `remember{}`
        // (pola fix Batch 393 utk `@DisallowComposableCalls`) sebelum jadi remember key.
        val nowPlayingBgColor = MaterialTheme.colorScheme.background
        val accentWashBrush = remember(animatedAccent, nowPlayingBgColor) {
            Brush.verticalGradient(
                listOf(
                    animatedAccent.copy(alpha = 0.35f),
                    nowPlayingBgColor.copy(alpha = 0.75f),
                    nowPlayingBgColor.copy(alpha = 0.97f)
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(accentWashBrush)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Batch 334 — FIX BUG NYATA (laporan user + screenshot): `verticalScroll()`
                // (Batch 112, jaring pengaman layar pendek) SEBELUMNYA membungkus Box gesture
                // brightness/volume (baris di bawah) — 2 pointer-drag-vertikal recognizer di
                // SUMBU YANG SAMA bersarang (parent scrollable + child `detectVerticalDragGestures`)
                // BENTROK memperebutkan touch stream yang sama, walau child sudah `change.consume()`
                // (Compose's ancestor `scrollable()` tetap bisa menang arbitrase drag-start/slop
                // duluan sebelum child sempat consume). GEJALA: swipe kecerahan/volume di piringan
                // jadi tersendat/salah baca sebagai scroll, persis laporan user.
                // FIX: Column ini TIDAK LAGI scrollable sendiri — cuma header (tombol atas+hint+
                // Spacer+Box art/gesture) yang tetap di sini (fixed, 0 ancestor scrollable lagi utk
                // gesture zone). Sisa konten (judul s/d tombol transport) dipindah ke Column BARU
                // di bawah (`.weight(1f).verticalScroll(...)`) — jaring pengaman Batch 112 utk
                // "transport row kepotong di layar pendek" TETAP ada, cuma scope-nya sekarang PAS
                // ke bagian yang benar2 butuh (bukan ikut membungkus area gesture yang architecturally
                // tidak boleh scroll).
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Batch 349 — user eksplisit (klarifikasi bertahap via tappable option, bukan tebakan):
        // `Arrangement.SpaceBetween` (Batch 342, dipertahankan+diverifikasi ulang s.d. Batch 348)
        // dilaporkan "jelek banget" — user secara eksplisit TIDAK mau ke-4 ikon disebar rata
        // sepanjang lebar layar. Diklarifikasi 2 tahap sebelum eksekusi (bukan langsung tebak):
        // (1) "urutan/posisi ikon" vs "jarak/spacing" vs "gaya ikon" -> user pilih spacing; (2)
        // opsi konkret arah pengelompokan -> user pilih eksplisit "Tutup sendiri di kiri, 3 ikon
        // lain rapat di kanan" — INI ADALAH POLA YANG SAMA PERSIS yang di-Batch-342 (versi lama,
        // sebelum SpaceBetween) & sempat dicatat "ditolak" di riwayat investigasi Batch 345. TAPI
        // preferensi user bisa berubah dari sesi ke sesi — pilihan REALTIME & EKSPLISIT sesi ini
        // (bukan asumsi ulang dari catatan lama) yang jadi rujukan, sesuai Hierarki `User Inst >
        // Core Protocol > PROJECT_STATE.md`. Fix: `horizontalArrangement = SpaceBetween` dibuang
        // dari `Row` (balik ke default `Start`), `Spacer(weight(1f))` dipasang lagi PERSIS setelah
        // tombol Tutup — Tutup presisi kiri mentok, Favorit+Info+Kontrol Lanjutan menumpuk rapat
        // di kanan mentok. 0 ikon ditambah/dihapus/diganti fungsi, 0 urutan logis 3 ikon kanan
        // diubah (tetap Favorit→Info→Kontrol Lanjutan, sesuai Batch 341).
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onBack,
                interactionSource = backInteraction,
                modifier = Modifier.bouncyPress(backInteraction)
            ) {
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
            // Batch 341 — user eksplisit (screenshot): ganti banner onboarding auto-tampil-
            // sekali (bisa "kena dismiss permanen dan gak balik lagi") jadi tombol permanen di
            // samping ikon favorit ini — buka/tutup kartu tip gestur (geser=kecerahan/volume,
            // ⋮=Sleep Timer/Kecepatan/Equalizer) KAPAN SAJA, bukan cuma sekali di awal. Toggle
            // (bukan cuma buka) — tap lagi saat kartu sudah tampil = tutup, simetris dgn tombol
            // X di kartunya sendiri. `showNowPlayingHint` (state sama yg dulu dikontrol
            // hintStore) dipakai ulang 1:1 — lihat deklarasi & render kartu di bawah.
            // Batch 343 — user eksplisit (screenshot): Row 4-ikon ini (spacing SpaceBetween sejak
            // Batch 342) masih "kelihatan anomali" — root cause BUKAN spacing (dikonfirmasi ulang
            // dari screenshot user: ke-4 posisi ikon renggang merata, sama seperti niat Batch 342),
            // tapi BOBOT VISUAL: `Icons.Default.Info` (varian "Filled") me-render sebagai lingkaran
            // PADAT dgn "i" — satu-satunya ikon berbentuk badge solid di antara 3 ikon lain yang
            // semuanya guratan tipis (Tutup/chevron, Favorit-border, Kontrol Lanjutan/titik tiga)
            // — persis kelas masalah yang sama dgn audit "samakan visual weight icon sejenis"
            // (Batch 228). Fix: `Icons.Outlined.Info` (paket `material-icons-extended`, SUDAH jadi
            // dependency app ini — grep `app/build.gradle.kts` konfirmasi) — cuma lingkaran GARIS
            // tipis + "i" tipis, bobot visual sama dgn 3 ikon lain, 0 lagi terlihat sbg badge
            // menonjol sendirian. 0 posisi/handler/tooltip Row ini disentuh batch itu (spacing
            // `SpaceBetween` Batch 342 waktu itu dipertahankan apa adanya — BELAKANGAN diganti
            // lagi jadi pengelompokan kanan oleh Batch 349, lihat komentar Batch 349 di atas Row).
            val hintButtonInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showNowPlayingHint = !showNowPlayingHint
                },
                interactionSource = hintButtonInteraction,
                modifier = Modifier.bouncyPress(hintButtonInteraction)
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = if (showNowPlayingHint) "Tutup tip gestur" else "Tip gestur & pintasan",
                    tint = if (showNowPlayingHint) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
            val advancedInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = { showAdvancedSheet = true },
                interactionSource = advancedInteraction,
                modifier = Modifier.bouncyPress(advancedInteraction)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Kontrol lanjutan",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Batch 337 — hint banner (~150dp, Batch 112's own catatan) DIPINDAH dari sini ke dalam
        // Column scrollable di bawah (lihat komentar Batch 337 di sana) — bukan lagi bagian
        // fixed zone. Zero gesture handling di banner ini (cuma Card+teks+tombol dismiss), jadi
        // aman dipindah ke ancestor scrollable, 0 regresi ke fix Batch 334 (itu spesifik soal
        // Box gesture brightness/volume yang TETAP tidak boleh py ancestor scrollable).

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

        // Batch 346 — inti fitur "art scale dinamis". Rasional lengkap ada di komentar Column
        // pengukur (`onGloballyPositioned`) di atas, dekat blok if(showNowPlayingHint) — cuma
        // rangkuman perhitungan di sini:
        // sisaRuang = tinggiKontenTersedia − chromeTetap − tinggiGrupKontenTerukur
        // dynamicArtSize = sisaRuang − 20dp (selisih art↔glow, lihat AlbumArtHero) lalu di-clamp.
        // `fixedChromeHeight` SENGAJA konstanta (bukan diukur run-time spt grup konten) — Row
        // ikon-atas (48dp, default IconButton) & Row transport (68dp, FilledIconButton eksplisit
        // .size(68.dp) adalah child tertinggi) keduanya deterministik dari kode sendiri, 0
        // bergantung ke song/font-scale — 1 measurement loop lebih sedikit = risiko lebih rendah.
        val screenWidthDp = windowSizeDp.width
        val fixedChromeHeight = 48.dp + 12.dp + 16.dp + 68.dp
        // Piringan persegi TIDAK BOLEH lebih lebar dari layar. 80dp = 2×40dp margin yang sudah
        // dipakai default lama (280dp piringan di layar 360dp lebar = 320dp konten setelah
        // padding Column 20dp -> 40dp margin tersisa) — formula ini SENGAJA balik ke 280dp
        // persis di layar 360dp lebar, konsisten dgn tampilan default lama, bukan lompatan baru.
        val maxArtByWidth = (screenWidthDp - 80.dp).coerceAtLeast(200.dp)
        val dynamicArtSize = if (contentGroupHeightPx > 0) {
            val measuredContentHeight = with(density) { contentGroupHeightPx.toDp() }
            val availableContentHeight = screenHeightDp - 40.dp // padding(20.dp) Column induk, 2 sisi
            (availableContentHeight - fixedChromeHeight - measuredContentHeight - 20.dp)
                .coerceIn(140.dp, maxArtByWidth)
        } else {
            // Frame pertama sebelum Column pengukur sempat invoke onGloballyPositioned —
            // fallback ke `albumArtBoxHeight` (formula lama, sudah adaptif layar pendek sejak
            // Batch 336) supaya 0 flash ukuran aneh sebelum pengukuran nyata mendarat.
            (albumArtBoxHeight - 20.dp).coerceAtLeast(140.dp)
        }
        val dynamicGestureBoxHeight = dynamicArtSize + 20.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dynamicGestureBoxHeight)
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

            // Batch 350 — BUG FIX (laporan berulang user, "dari dulu susahnya minta ampun"):
            // klaim komentar lama di bawah ("vinyl dapat first claim, HANYA leftover DI LUAR
            // bounds-nya yang sampai ke zona brightness/volume") ternyata TIDAK match perilaku
            // asli device — swipe vertikal yang jarinya mendarat DI ATAS vinyl (area yang sangat
            // wajar disentuh, krn itu elemen visual terbesar di layar) tetap "ditelan" duluan oleh
            // `detectHorizontalDragGestures` milik `AlbumArtHero` (baris ~1577), walau gerakan
            // jarinya vertikal murni — root cause: gesture recognizer terpisah (kiri/kanan
            // vertikal vs vinyl horizontal) sama-sama bersaing di 1 titik sentuh tanpa 1 wasit
            // tunggal yg menentukan SUMBU gerakan lebih dulu.
            //
            // Fix: `pointerInput` BARU di sini (bukan mengubah `AlbumArtHero`/`detectHorizontal-
            // DragGestures`-nya sama sekali — 0 baris logic swipe-next/prev threshold-120px/
            // spring/haptic Batch 178/256 disentuh) — didaftarkan di `PointerEventPass.Initial`,
            // yang dijalankan Compose DULUAN (top-down) SEBELUM event sampai ke pointerInput Main-
            // pass default milik `AlbumArtHero` di bawahnya. Selama sumbu gerakan belum jelas
            // (belum lewati `touchSlop`), event dibiarkan lewat APA ADANYA (0 consume) — vinyl
            // tetap bebas mendeteksi sendiri seperti biasa. Begitu akumulasi gerakan melewati
            // slop: kalau dominan HORIZONTAL, tetap 0 disentuh (biarkan vinyl lanjut persis
            // seperti sebelum batch ini — swipe ganti lagu 0 regresi). Kalau dominan VERTIKAL,
            // baru DARI SITU setiap `change` di-consume() di pass Initial — akibatnya `detect-
            // HorizontalDragGestures` milik vinyl (jalan belakangan, di pass Main) melihat change
            // yang SUDAH consumed, jadi otomatis cancel (memicu `onDragCancel` bawaannya sendiri
            // -> `dragOffset` spring balik ke 0, 0 kode baru perlu ditulis utk itu) — sementara di
            // sini delta-Y-nya dialihkan ke `applyBrightness`/`applySystemVolume` yang SAMA PERSIS
            // dipakai 2 Box zona kiri/kanan di bawah (baris ~569-615), termasuk indikator pill +
            // delay 600ms sebelum hilang, biar konsisten 1 pengalaman dgn versi di luar vinyl.
            // Separuh kiri/kanan ditentukan dari posisi X sentuh-awal RELATIF ke lebar vinyl itu
            // sendiri (`size.width` milik Box vinyl ini) — karena vinyl dipusatkan (`Alignment.
            // Center`) di dalam Box induk yang sama, titik tengah lokal vinyl ini otomatis persis
            // sejajar garis tengah layar yang sama dipakai 2 zona kiri/kanan itu (0 offset
            // koordinat perlu dikonversi manual). 2 Box zona kiri/kanan ITU SENDIRI 0 disentuh —
            // fix ini murni menambal celah "vertikal di ATAS vinyl", bukan mengganti apa pun yang
            // sudah benar (perilaku touch DI LUAR vinyl 0 berubah).
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = entranceScale.value
                        scaleY = entranceScale.value
                        alpha = entranceAlpha.value
                    }
                    .pointerInput(Unit) {
                        val slop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial)
                            val isLeftHalf = down.position.x < size.width / 2f
                            var axisLocked: Boolean? = null // null = belum ketahuan, true = vertikal (intercept), false = horizontal (biarkan)
                            var accumX = 0f
                            var accumY = 0f
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val delta = change.position - change.previousPosition
                                    when (axisLocked) {
                                        null -> {
                                            accumX += delta.x
                                            accumY += delta.y
                                            if (abs(accumX) > slop || abs(accumY) > slop) {
                                                axisLocked = abs(accumY) > abs(accumX)
                                                if (axisLocked == true) {
                                                    if (isLeftHalf) showBrightnessIndicator = true else showVolumeIndicator = true
                                                }
                                            }
                                        }
                                        true -> {
                                            change.consume()
                                            if (isLeftHalf) {
                                                applyBrightness(brightnessLevel - delta.y / gestureRangePx)
                                            } else {
                                                applySystemVolume(systemVolumeFraction - delta.y / gestureRangePx)
                                            }
                                        }
                                        false -> { /* horizontal terkunci — 0 disentuh, vinyl lanjut normal */ }
                                    }
                                }
                            } finally {
                                if (axisLocked == true) {
                                    gestureScope.launch {
                                        delay(600)
                                        if (isLeftHalf) showBrightnessIndicator = false else showVolumeIndicator = false
                                    }
                                }
                            }
                        }
                    }
            ) {
                AlbumArtHero(
                    artworkUri = song?.uri,
                    accentColor = animatedAccent,
                    artSize = dynamicArtSize,
                    onSwipeNext = onNext,
                    onSwipePrevious = onPrevious
                )
            }
        }

        // Batch 334 — badan scrollable terpisah (lihat komentar Column induk di atas): jaring
        // pengaman Batch 112 utk layar pendek dipindah SPESIFIK ke sini (judul s/d transport),
        // TIDAK LAGI ikut membungkus Box gesture brightness/volume di atas.
        // Batch 335 — FIX BUG BARU (laporan user, device): begitu Column ini py `weight(1f)`
        // sendiri (fixed height dari sisa ruang, BUKAN unbounded lagi spt Column tunggal lama
        // sebelum Batch 334), pada layar yang cukup tinggi kontennya SUDAH muat penuh (maxScroll
        // scrollState = 0) — TAPI overscroll stretch-glow bawaan Android 12+/Compose Foundation
        // tetap terpicu visual tiap drag disentuh, walau posisi scroll tidak benar-benar
        // berpindah (rubber-band kosong). User baca ini sebagai "masih bisa discroll" walau
        // konten sudah muat. Root cause BEDA dari bug Batch 334 (itu soal 2 recognizer axis sama
        // bentrok, INI soal efek visual overscroll yang terpicu independen dari maxScroll).
        // FIX: matikan overscroll KHUSUS di Column ini lewat overload `verticalScroll(state,
        // overscrollEffect = null, ...)` — API OverscrollEffect langsung di parameter fungsi,
        // BUKAN pola lama `CompositionLocalProvider(LocalOverscrollConfiguration provides null)`
        // (dipakai SmartPlaylistScreen.kt Batch 263 saat BOM masih 2024.05.00) — dicek ulang
        // `web_search` ke dokumentasi resmi Compose Foundation: `LocalOverscrollConfiguration`/
        // `OverscrollConfiguration` SUDAH DEPRECATED (diganti `LocalOverscrollFactory`), persis
        // risiko yang sudah ditandai eksplisit di catatan Batch 291 soal lompatan BOM ini —
        // overload `overscrollEffect` di `verticalScroll()` sendiri sudah tersedia sejak lama di
        // BOM 2026.04.01 project ini (jauh di atas versi minimum ditambahkannya parameter itu),
        // jadi dipakai langsung sesuai kebijakan prioritas mutakhir (aturan sesi #3) — bukan
        // menambah 1 lagi titik pakai API usang yang sudah ketahuan berisiko. Getaran/animasi
        // scroll GENUINELY dibutuhkan (kalau konten overflow di layar pendek) TETAP jalan penuh
        // via `scrollState` — cuma efek visual overscroll DI LUAR rentang scroll asli yang
        // dimatikan, 0 logic gesture/scroll lain diubah.
        // Batch 343 — user eksplisit ("bagian pemutar dilarang keras untuk mengambang/tidak
        // menyentuh dasar sama sekali"): Row transport (shuffle/prev/play-pause/next/repeat)
        // SEBELUMNYA jadi child TERAKHIR di dalam Column scrollable+weight(1f) ini. Root cause
        // "mengambang": `Column` biasa (verticalArrangement default = Top) menaruh anak-anaknya
        // rapat dari ATAS ruang yang tersedia — begitu total tinggi konten (judul s/d transport)
        // LEBIH PENDEK dari tinggi weighted-area (kasus umum di layar normal/tinggi, art box
        // sudah fixed 300dp duluan di atas), transport row berhenti persis di bawah konten
        // terakhirnya sendiri, MENYISAKAN spasi kosong di antara transport row dan tepi bawah
        // layar — persis "mengambang" yang dilaporkan, bukan cuma soal padding/margin.
        // FIX (struktural, bukan tuning angka): Row transport ini DIKELUARKAN dari Column
        // scrollable ini, jadi sibling TETAP (fixed) tepat SETELAH Column scrollable ini ditutup
        // (lihat Row transport & Spacer 16dp pemisahnya di bawah, di luar blok `{ }` Column ini).
        // Karena Column INDUK (fillMaxSize, bukan yang scrollable ini) menaruh Column scrollable
        // ini dgn `weight(1f)`, Column scrollable otomatis kebagian PERSIS sisa ruang di ATAS Row
        // transport yang sekarang fixed di posisi TERAKHIR Column induk — hasilnya Row transport
        // SELALU presisi di tepi bawah (sebelum padding 20dp layar), 0 spasi kosong tersisa di
        // bawahnya, terlepas dari tinggi konten judul-slider di atasnya ataupun tinggi layar.
        // Bonus: ini SEKALIGUS menuntaskan seluruh saga reachability Batch 336-338 secara lebih
        // kuat — transport SEKARANG SELALU terlihat tanpa perlu scroll sama sekali (bukan cuma
        // "terjangkau via scroll"), di layar pendek pun cuma konten judul-slider yang battle-scroll
        // di ruang tersisa, transport tetap fixed & penuh terlihat. 0 logic scroll/gesture/timing
        // lain di Column ini diubah — cuma 1 child (Row transport) yang pindah lokasi.
        // Batch 345 — user kirim 2 screenshot (crop Row ikon atas + crop area waktu/transport) +
        // laporan: "susunan badge anomali yang terpaku oleh jarak" & "masih ada bagian kosong
        // karena bagian atas terlalu mentok ke badge — gak ada susunan normal begitu". Diinvestigasi
        // eksplisit poin 1 (Row 4-ikon atas) dulu — DIUKUR ULANG per-pixel (bukan cuma lihat
        // sekilas): jarak antar-ikon 279/280/279px, PERSIS merata (`SpaceBetween` Batch 342 masih
        // benar), dan bobot visual ke-4 ikon sudah konsisten tipis (`Outlined.Info` Batch 343 juga
        // masih benar) — 0 regresi di Row itu sendiri. Root cause SEBENARNYA (dikonfirmasi via
        // screenshot ke-2): fix Batch 343 (Row transport dikeluarkan jadi footer fixed) MEMINDAH
        // lokasi "gambang" tsb, TIDAK MENGHILANGKANNYA — Column INI (scrollable+weight) masih
        // `verticalArrangement` default (Top), jadi begitu tinggi konten (judul s/d waktu) LEBIH
        // PENDEK dari ruang weighted (kasus layar user), semua sisa ruang kosong tetap menumpuk
        // jadi SATU gap besar, cuma sekarang lokasinya PINDAH ke ANTARA baris waktu & Row
        // transport (bukan lagi di bawah Row transport) — persis yang kelihatan di screenshot
        // ke-2 user. Laporan poin 1 ("terpaku oleh jarak") & poin 2 ("bagian atas mentok") SAMA
        // root cause ini dilihat dari 2 sudut: konten atas (Row ikon+art+judul dst) tetap rapat
        // ke atas ("mentok"/"terpaku") walau ruang tersedia jauh lebih tinggi — gak ada distribusi
        // proporsional ("susunan normal") atas sisa ruang tsb, semua dikumpulkan jadi 1 blok di
        // bawah. FIX: `verticalArrangement = Arrangement.Center` di Column ini — saat konten LEBIH
        // PENDEK dari ruang weighted, `Center` membagi sisa ruang itu proporsional ke ATAS (antara
        // art box & judul) DAN ke BAWAH (antara baris waktu & Row transport) alih-alih ditumpuk
        // 100% di satu sisi — 1 gap besar jadi 2 gap seimbang, lebih dekat ke "susunan normal"
        // yang diminta. 0 efek saat konten SUDAH >= tinggi viewport (layar pendek/konten panjang)
        // — `Center` cuma berlaku kalau ada sisa ruang, scroll tetap jalan identik seperti
        // sebelumnya kalau tidak ada sisa ruang. Row transport TETAP fixed footer presisi di tepi
        // bawah (Batch 343 TIDAK disentuh/dibatalkan — itu tetap benar & sudah dikonfirmasi user
        // "no more floating thing").
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(state = rememberScrollState(), overscrollEffect = null),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

        // Batch 337 — BUG FIX (laporan user, device): Batch 336 (album art box adaptif) TIDAK
        // CUKUP — "belum ngefek" di device user. Root cause SATU LEVEL LEBIH DALAM (kebijakan
        // Batch 24: fix resmi sudah diikuti, gejala IDENTIK, curigai akar beda): `FeatureHintBanner`
        // (~150dp, dicatat eksplisit di root cause asli Batch 112) SEBELUMNYA ada di fixed header
        // zone (sebelum Box art) — elemen fixed ~150dp INI, bukan cuma art 300dp, yang jadi
        // kontributor terbesar ke penyempitan ruang scroll di layar pendek (khususnya kombinasi
        // 3-button nav + hint belum di-dismiss user, persis skenario asli Batch 112). Susutkan
        // art (Batch 336) saja tidak cukup selama hint banner MASIH fixed & tidak bisa direbut
        // ulang ruangnya oleh scroll. FIX: banner ini (0 gesture handling — cuma Card+teks+tombol
        // dismiss, aman dipindah, 0 regresi Batch 334) dipindah jadi child PERTAMA di Column
        // scrollable ini — sekarang ikut jadi bagian yang bisa "discroll lewat" utk menjangkau
        // transport row di layar pendek, alih-alih permanen menghabiskan jatah fixed zone yang
        // tidak pernah bisa diciutkan scroll. Urutan visual SEDIKIT berubah (hint sekarang di
        // BAWAH piringan art, bukan di ATAS lagi, sebelum art) — trade-off sengaja diambil demi
        // reachability transport row (fungsi inti) di atas posisi visual hint (onboarding,
        // sekali tampil, dismissable).
        // Batch 338 — lanjutan (user konfirmasi hint MASIH nongol saat komplain "scroll gak
        // seharusnya kepicu di layar saya"): selain art box (di atas), teks banner ini sendiri
        // dipersingkat (5-ish baris → ~2 baris bodySmall) + 2 Spacer sekitarnya diciutkan —
        // makna/isi 2 tip TIDAK berkurang (kecerahan/volume + menu ⋮), cuma dikemas lebih padat.
        // Total 3 lever batch ini (art box, teks banner, spacer) sengaja dikombinasi
        // supaya layar "normal" (bukan cuma yg <640dp) juga muat tanpa scroll SELAMA hint
        // sekali-tampil ini masih ada — begitu di-dismiss, semua balik ke ukuran penuh biasa.
        // Batch 341 — user eksplisit: "kena dismiss permanen dan gak balik lagi" jadi masalah
        // utama — `onDismiss` di bawah TIDAK lagi panggil `hintStore.markNowPlayingHintSeen()`
        // (dihapus, lihat deklarasi `showNowPlayingHint` di atas), cuma toggle tutup POPUP saat
        // ini — bisa dibuka lagi kapan saja lewat tombol info baru di Row atas (samping ikon
        // favorit). Teks/posisi/tampilan banner ITU SENDIRI 0 diubah (masih card sama, Batch 338).
        // Batch 346 — user pilih lanjut ide "art scale dinamis" yang dicatat sbg trade-off
        // Batch 345 (solusi paling "otentik" ala Spotify: sisa ruang di-ISI PIRINGAN, bukan
        // didistribusikan jadi 2 gap kosong via Arrangement.Center). Column pembungkus BARU ini
        // (hint s/d baris waktu, grup yang SAMA yang tadinya dipusatkan Center) diukur tinggi
        // NYATA-nya lewat `onGloballyPositioned` — kuncinya: `verticalScroll` (parent) memberi
        // constraint tinggi TAK TERBATAS ke children-nya (supaya tahu total tinggi buat discroll),
        // jadi tinggi yang dilaporkan grup ini SELALU intrinsik (isi asli), TIDAK PERNAH terpotong/
        // dipaksa oleh `weight(1f)` Column induknya — beda dari mengukur Column induk itu sendiri
        // (yang akan selalu melaporkan tinggi teralokasi, bukan tinggi konten). Hasil pengukuran
        // (`contentGroupHeightPx`) dipakai di deklarasi `dynamicArtSize` bawah (sebelum Box
        // piringan) buat menghitung sisa ruang yang diberikan ke piringan. Pola `onGloballyPositioned`
        // ini BUKAN hal baru di codebase — sudah dipakai identik di LibraryScreen.kt/QueueSheet.kt/
        // SongPickerSheet.kt/PlaylistScreen.kt.
        Column(
            modifier = Modifier.onGloballyPositioned { contentGroupHeightPx = it.size.height },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        if (showNowPlayingHint) {
            FeatureHintBanner(
                text = "Geser piringan: kiri = kecerahan, kanan = volume. Ketuk ⋮ buat Sleep Timer, Kecepatan & Equalizer.",
                onDismiss = { showNowPlayingHint = false }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(if (showNowPlayingHint) 20.dp else 32.dp))

        Text(
            "SEDANG DIPUTAR",
            style = MaterialTheme.typography.labelSmall,
            color = animatedAccent
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Batch 359 — REVERT arah Batch 358. Screenshot device user (Row Plus/Title/Share)
        // dilaporkan "aneh"/berantakan (judul kepanjangan bikin ikon Share nabrak/nempel teks
        // marquee, ikon Plus mepet tepi kiri) — user eksplisit minta disamakan ke entry+layout
        // "Lirik" saja, yaitu opsi yang sudah dicatat (tapi belum dipilih) di komentar Batch 358
        // sendiri. Judul balik jadi Text polos (0 ikon flanking, 0 Row pembungkus) — sama seperti
        // sebelum Batch 358, cuma fillMaxWidth+textAlign Center dipertahankan (bukan wrap-content)
        // supaya basicMarquee tetap mulai dari titik tengah horizontalAlignment Column induk,
        // identik posisi visual sebelumnya.
        Text(
            song?.title ?: "-",
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().basicMarquee()
        )
        Text(
            song?.artist ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Tambah/Bagikan sekarang REUSE persis pola visual+struktur tombol "Lirik" (icon 16dp +
        // Spacer 6dp + Text labelMedium bertint animatedAccent, bouncyPress 0.92f, contentPadding
        // horizontal=14/vertical=6) — bukan cuma disamakan sekilas, tapi 1:1 entry yang sama,
        // ditaruh dalam 1 Row bareng "Lirik" (Arrangement.Center, jarak alami dari padding tiap
        // TextButton sendiri, 0 Spacer manual tambahan diperlukan). Handler/logic KEDUANYA 0
        // diubah dari Batch 358 (AddToPlaylistDialog & Intent.ACTION_SEND sama persis) — murni
        // migrasi tempat+gaya render, ikon jadi contentDescription = null (pola Batch 230/235:
        // decorative krn sudah ada Text label sibling di button yang sama, semantics ke-merge ke
        // 1 title TalkBack per tombol, konsisten cara "Lirik" sudah dari awal).
        // Batch 361 — FIX BUG NYATA (laporan user + screenshot): di layar sempit, 4 TextButton
        // (Tambah/Bagikan/Lirik/Rating, ditambah Batch 360) TIDAK muat 1 baris — Row tanpa scroll
        // dipaksa mengecilkan lebar TextButton terakhir sampai nyaris 0dp, bikin Text("Rating")
        // di dalamnya wrap PER-HURUF turun vertikal di tepi kanan layar (persis yang kefoto:
        // "R/a/t/i/n/g" numpuk 1 kolom, nembus ke bawah sampai overlap area waveform). ROOT CAUSE
        // GANDA, 2 FIX terpisah tapi saling melengkapi (bukan cuma 1):
        // (1) Row 0 pernah dikasih jalan keluar kalau kontennya kepanjangan (0 scroll, 0 wrap) —
        //     ditambah `.horizontalScroll(rememberScrollState())` supaya di layar manapun yang
        //     kurang lebar, Row jadi scrollable ke samping (bukan overflow diam-diam/kepotong)
        //     alih-alih maksa compress. Kalau muat (layar lebar), scrollState diam di 0 & visualnya
        //     IDENTIK versi sebelumnya (Arrangement.Center tetap efektif krn area scroll = area
        //     Row kalau konten <= viewport). `fillMaxWidth()` ditambah eksplisit di modifier Row
        //     (dulu implicit ikut ukuran anak) — perlu supaya area scroll punya batas ukur yang
        //     jelas, bukan supaya krusial untuk fix ini sendiri.
        // (2) Independen dari (1), SEMUA 4 Text label tombol (Tambah/Bagikan/Lirik/Rating) dikasih
        //     `maxLines = 1` + `overflow = TextOverflow.Ellipsis` (pola sama persis judul lagu di
        //     atas — Text(song?.title, maxLines=1,...) — cuma belum pernah diterapkan ke Text di
        //     Row tombol ini dari batch manapun sebelumnya, termasuk 3 tombol lama). Ini JARING
        //     PENGAMAN independen dari (1): meskipun (1) sudah bikin Row scrollable (jadi secara
        //     teori tiap TextButton SELALU dapat lebar penuh sesuai intrinsic width-nya, tidak
        //     pernah dipaksa compress lagi), maxLines=1 memastikan text tidak PERNAH bisa wrap
        //     vertikal lagi di skenario manapun (mis. font-scale sistem user disetel besar) —
        //     4 tombol lama SEHARUSNYA sudah begini dari awal, gap yang baru ketahuan sekarang.
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            val addToPlaylistInteraction = remember { MutableInteractionSource() }
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showAddToPlaylistDialog = true
                },
                interactionSource = addToPlaylistInteraction,
                modifier = Modifier.bouncyPress(addToPlaylistInteraction, pressedScale = 0.92f),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = animatedAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Tambah",
                    style = MaterialTheme.typography.labelMedium,
                    color = animatedAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val shareInteraction = remember { MutableInteractionSource() }
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    song?.let { s ->
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, s.uri)
                            putExtra(Intent.EXTRA_TITLE, s.title)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Bagikan lagu"))
                    }
                },
                interactionSource = shareInteraction,
                modifier = Modifier.bouncyPress(shareInteraction, pressedScale = 0.92f),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    tint = animatedAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Bagikan",
                    style = MaterialTheme.typography.labelMedium,
                    color = animatedAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val lyricsQuickInteraction = remember { MutableInteractionSource() }
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showLyricsSheet = true
                },
                interactionSource = lyricsQuickInteraction,
                modifier = Modifier.bouncyPress(lyricsQuickInteraction, pressedScale = 0.92f),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Article,
                    contentDescription = null,
                    tint = animatedAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Lirik",
                    style = MaterialTheme.typography.labelMedium,
                    color = animatedAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Batch 360 — jawaban PENDING_RatingEntryPoint.md (Batch 357): Opsi 4 dipilih user
            // ("Balik ke Now Playing, versi ringkas") — StarRatingRow LAMA (5 IconButton tetap
            // tampil) TIDAK dikembalikan; sebagai gantinya 1 entry "Rating" REUSE 1:1 pola
            // visual+struktur Tambah/Bagikan/Lirik di atas (icon 16dp + Spacer 6dp + Text
            // labelMedium bertint animatedAccent, bouncyPress 0.92f, contentPadding sama) —
            // ringkas sesuai literal permintaan user, bukan mengembalikan 5 ikon permanen.
            // `currentRating`/`onSetRating` dari signature fungsi ini (SENGAJA tidak dihapus
            // Batch 357, lihat komentar parameter) dipakai lagi di sini tanpa perlu menyentuh
            // MainActivity.kt sama sekali. Icon dibuat DINAMIS (Star terisi vs StarBorder) —
            // beda dari Tambah/Bagikan/Lirik yang iconnya statis — karena di sini bentuk icon
            // itu sendiri membawa informasi (sudah dirating atau belum) yang TIDAK terwakili
            // oleh label teks statis "Rating"; makanya contentDescription-nya TIDAK null (beda
            // dari 3 sibling di atas) — konsisten aturan Batch 230/235 (null cuma untuk icon yang
            // genuinely decorative, di sini icon menyampaikan state, bukan dekoratif semata).
            val ratingInteraction = remember { MutableInteractionSource() }
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showRatingDialog = true
                },
                interactionSource = ratingInteraction,
                modifier = Modifier.bouncyPress(ratingInteraction, pressedScale = 0.92f),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    if (currentRating > 0) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (currentRating > 0) {
                        "Rating saat ini: $currentRating dari 5 bintang"
                    } else {
                        "Belum ada rating"
                    },
                    tint = animatedAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Rating",
                    style = MaterialTheme.typography.labelMedium,
                    color = animatedAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Batch 353 (Opsi A) — blok Slider+waveform+teks waktu dipindah ke composable terpisah
        // (PlaybackProgressRow, definisi di bawah fungsi ini) yang collect playbackProgress
        // SENDIRI, bukan lewat uiState.position/duration lagi. Behavior/tampilan 1:1 sama,
        // cuma scope recompose-nya sekarang terisolasi ke composable kecil itu saja. Komentar
        // asli (Batch v3 font waktu, Roadmap #12 audiobook remaining-time) dipindah ke sana.
        PlaybackProgressRow(
            playbackProgress = playbackProgress,
            audiobookModeEnabled = audiobookModeEnabled,
            animatedAccent = animatedAccent,
            isCalmRetro = isCalmRetro,
            songId = song?.id,
            haptic = haptic,
            onSeek = onSeek
        )
        } // tutup Column pengukur (Batch 346, onGloballyPositioned) — pasangan pembuka di atas,
          // sebelum blok if(showNowPlayingHint) — lihat rasional lengkap di sana.
        } // tutup Column scrollable (Batch 334) — Batch 343: penutup ini SENGAJA dipindah ke sini
          // (sebelumnya menutup SETELAH Row transport di bawah) supaya Row transport jadi sibling
          // FIXED milik Column induk (fillMaxSize), bukan lagi child terakhir Column scrollable —
          // rasionalisasi lengkap "mengambang" ada di komentar deklarasi Column scrollable (atas).

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            // Batch 170 — sebelumnya Row ini TANPA fillMaxWidth()/horizontalArrangement sama
            // sekali (cuma verticalAlignment), beda dari konvensi player pada umumnya (Spotify/
            // Apple Music/YouTube Music selalu spread 5 kontrol playback merata sepanjang
            // lebar layar, bukan cluster rapat di tengah) — dan beda dari kebiasaan file ini
            // sendiri yang SELALU mengomentari keputusan layout sengaja (glow, shape, shadow,
            // dst — cek komentar Batch di sekitar tombol play/pause tepat di bawah), Row ini
            // 0 komentar sama sekali, ciri khas oversight bukan keputusan sadar.
            // SpaceEvenly dipilih (bukan SpaceBetween) supaya jarak kiri tombol Acak ke tepi
            // Column dan kanan tombol Ulangi ke tepi Column TIDAK menempel rapat ke padding
            // 20dp Column — tetap ada ruang, konsisten "bernapas" dengan elemen lain di layar
            // yang sama (title/artist/slider semua punya margin dari tepi).
            // Batch 343 — Row ini SEKARANG fixed footer (sibling Column induk, BUKAN lagi child
            // Column scrollable di atasnya) — jaminan "menyentuh dasar" datang dari posisi barunya
            // ini, bukan dari Row ini sendiri. 0 isi/ikon/handler/spacing Row ini diubah, murni
            // pindah lokasi struktural (lihat rasionalisasi lengkap di komentar deklarasi Column
            // scrollable di atas).
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shuffleInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onShuffle,
                interactionSource = shuffleInteraction,
                modifier = Modifier.bouncyPress(shuffleInteraction)
            ) {
                Icon(
                    Icons.Default.Shuffle,
                    // Batch 231 — Iconography 6/7 (semantic label actionable icon). Sebelumnya
                    // "Acak" statis — TalkBack user tidak tahu status ON/OFF saat ini (beda
                    // dari user awas yang lihat lewat tint animatedAccent vs secondary).
                    // Fix: label ikut state, konsisten pola dgn Repeat di bawah.
                    contentDescription = if (uiState.shuffleEnabled) "Acak: aktif" else "Acak: nonaktif",
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
                            isCalmRetro -> Modifier.calmAberration()
                            else -> Modifier
                        }
                    )
                    .bouncyPress(playPauseInteraction, pressedScale = 0.85f)
            ) {
                AnimatedContent(
                    targetState = uiState.isPlaying,
                    label = "playPause",
                    // Batch 332 — Pending Queue item 1 (dari Batch 330): upgrade default
                    // `AnimatedContent` (fade polos bawaan Compose kalau `transitionSpec` tidak
                    // diisi) jadi morph scale+fade — ikon baru masuk membesar dari 0.6x sambil
                    // fade in, ikon lama keluar mengecil ke 0.6x sambil fade out. Durasi REUSE
                    // persis pola asimetris "masuk lebih pelan, keluar lebih cepat" yang sudah
                    // divalidasi Batch 330 (200ms/150ms, dipakai NavHost tab transition) — bukan
                    // angka baru. `togetherWith` (bukan `with` yang sudah deprecated).
                    transitionSpec = {
                        (scaleIn(initialScale = 0.6f, animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)))
                            .togetherWith(scaleOut(targetScale = 0.6f, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)))
                    }
                ) { playing ->
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Jeda" else "Putar",
                        // Batch 224 — Iconography 1/7 (audit ukuran icon). Sebelumnya 34dp: LEBIH
                        // KECIL dari icon SkipPrevious/SkipNext yang mengapitnya (36dp), padahal
                        // tombol ini kontainer PALING BESAR di row (68dp vs default ~48dp
                        // IconButton) — hierarki visual kebalik (aksi utama harusnya glyph
                        // TERBESAR, bukan terkecil). Baris Shuffle/Repeat (default 24dp, tanpa
                        // override) < Skip (36dp) < Play/Pause sekarang 40dp — urutan bobot
                        // visual 3-tingkat yang benar utuh dipulihkan.
                        // Batch 226 — Iconography 2/7 (audit optical alignment). Glyph segitiga
                        // PlayArrow punya bobot visual condong ke kiri dalam bounding box-nya
                        // (beda dari Pause yang simetris) — kalau ukuran sama & posisi sama
                        // persis pas AnimatedContent switch, mata lihat PlayArrow "kegeser kiri"
                        // dari titik pusat lingkaran tombol. Fix: offset +1dp ke kanan HANYA
                        // saat PlayArrow (bukan Pause) buat kompensasi bias optik tsb.
                        modifier = Modifier
                            .size(40.dp)
                            .then(if (!playing) Modifier.offset(x = 1.dp) else Modifier)
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
                    // Batch 231 — Iconography 6/7 (semantic label actionable icon). Sebelumnya
                    // "Ulangi" statis utk toggle 3-state (OFF→ALL→ONE) — TalkBack user tidak
                    // bisa bedakan OFF vs ALL sama sekali (icon glyph identik, cuma tint beda,
                    // dan tint tidak terbaca screen reader). Fix: label sebut mode aktif.
                    contentDescription = when (uiState.repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Ulangi: satu lagu"
                        Player.REPEAT_MODE_ALL -> "Ulangi: semua lagu"
                        else -> "Ulangi: mati"
                    },
                    tint = if (uiState.repeatMode != Player.REPEAT_MODE_OFF) animatedAccent else MaterialTheme.colorScheme.secondary
                )
            }
        }
        } // tutup Column induk (fillMaxSize, Batch 343) — Row transport di atas persis child
          // TERAKHIRnya, jadi selalu presisi di tepi bawah layar, 0 spasi kosong tersisa.

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
            sleepTimerRemaining = sleepTimerRemaining,
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

    // Batch 358 — dialog utk ikon "Plus" baru di Row judul. Reuse AddToPlaylistDialog
    // (PlaylistScreen.kt) apa adanya, callback body copy pola addToPlaylist/onAddToExisting/
    // onCreateAndAdd LibraryScreen.kt (baris ~219 & ~474) — beda cuma sumber `song` (di sini dari
    // uiState.currentSong yg sudah di-scope di atas, bukan songForPlaylistDialog lokal) & 0
    // onInfoMessage (NowPlayingScreen belum punya param itu, konsisten pola fire-and-forget
    // lain di file ini spt onAddBookmark/onSetRating — haptic LongPress cukup jadi konfirmasi).
    if (showAddToPlaylistDialog && song != null) {
        AddToPlaylistDialog(
            song = song,
            playlists = playlists,
            onAddToExisting = { playlist ->
                onAddSongToPlaylist(playlist.id, song.id)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showAddToPlaylistDialog = false
            },
            onCreateAndAdd = { name ->
                val playlist = onCreatePlaylist(name)
                onAddSongToPlaylist(playlist.id, song.id)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showAddToPlaylistDialog = false
            },
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    // Batch 360 — dialog untuk entry "Rating" baru di Row Tambah/Bagikan/Lirik/Rating (lihat
    // komentar di atas Row-nya). RatingDialog (definisi di bawah fungsi ini, pola sama persis
    // SpeedDialog: AlertDialog + tombol "Tutup", pilihan diterapkan LANGSUNG saat ditekan tanpa
    // menutup dialog, biar user bisa lihat hasilnya dulu sebelum menutup manual).
    if (showRatingDialog) {
        RatingDialog(
            currentRating = currentRating,
            onDismiss = { showRatingDialog = false },
            onSetRating = onSetRating
        )
    }

    if (showLyricsSheet && song != null) {
        var lyricsText by remember(song.id) { mutableStateOf(onGetLyrics(song.id)) }
        // Batch 353 (Opsi A) — WithLivePlaybackProgress (definisi di bawah fungsi ini) collect
        // playbackProgress di scope-nya SENDIRI lalu suplai positionMs lewat parameter lambda;
        // sheet ini aktif live (auto-scroll lirik), jadi WAJAR ikut re-render tiap tick — yang
        // penting itu TIDAK bocor ke scope NowPlayingScreen di luar blok if ini.
        WithLivePlaybackProgress(playbackProgress) { livePositionMs, _ ->
            LyricsSheet(
                rawLyrics = lyricsText,
                autoUiState = lyricsAutoState,
                positionMs = livePositionMs,
                isPlaying = uiState.isPlaying,
                onPlayPause = onPlayPause,
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

    if (showAbRepeatBookmarkSheet && song != null) {
        var bookmarks by remember(song.id) { mutableStateOf(onGetBookmarks(song.id)) }
        // Batch 353 (Opsi A) — sama alasan dengan LyricsSheet di atas.
        WithLivePlaybackProgress(playbackProgress) { livePositionMs, _ ->
            ABRepeatBookmarkSheet(
                songId = song.id,
                positionMs = livePositionMs,
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
        // Batch 537 (baterai) — capture Visualizer OS (~15fps + FFT) dulu HANYA berhenti lewat
        // `onDismiss`. Dua celah bocor: (1) rotasi/komposisi lepas -> flag `remember` hilang TANPA
        // onDismiss, capture jalan terus tanpa sheet; (2) app ke background dgn sheet terbuka -> capture
        // jalan terus saat layar mati. Sekarang: ON_STOP -> lepas, ON_START (setelah STOP) -> pasang
        // lagi, onDispose -> lepas (idempoten: `release()` aman dipanggil berulang).
        val vizLifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(vizLifecycleOwner) {
            var stoppedInBackground = false
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        stoppedInBackground = true
                        onCloseVisualizer()
                    }
                    Lifecycle.Event.ON_START -> if (stoppedInBackground) {
                        stoppedInBackground = false
                        onOpenVisualizer()
                    }
                    else -> Unit
                }
            }
            vizLifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                vizLifecycleOwner.lifecycle.removeObserver(observer)
                onCloseVisualizer()
            }
        }
        VisualizerSheet(
            enabled = visualizerEnabled,
            supported = visualizerSupported,
            permissionGranted = visualizerPermissionGranted,
            visualizerBars = visualizerBars,
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
            sleepTimerRemaining = sleepTimerRemaining,
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
            onStopPlayback = {
                showAdvancedSheet = false
                onStopPlayback()
            },
            onOpenVisualizer = {
                showAdvancedSheet = false
                onOpenVisualizer()
                showVisualizerSheet = true
            },
            onOpenSongInfoEdit = {
                showAdvancedSheet = false
                showSongInfoEditSheet = true
            },
            onOpenRingtoneCutter = {
                showAdvancedSheet = false
                showRingtoneCutterSheet = true
            }
        )
    }

    if (showSongInfoEditSheet && song != null) {
        SongInfoEditSheet(
            song = song,
            onDismiss = { showSongInfoEditSheet = false },
            onSave = { tags ->
                onSaveSongTags(song, tags)
                showSongInfoEditSheet = false
            }
        )
    }

    if (showRingtoneCutterSheet && song != null) {
        RingtoneCutterSheet(
            song = song,
            onDismiss = { showRingtoneCutterSheet = false },
            onCut = { s, range, destination, label ->
                onCutRingtone(s, range, destination, label)
                showRingtoneCutterSheet = false
            }
        )
    }
}

// Batch 353 (Opsi A, PENDING_FixGlobalLagRecomposition.md) — waveform+Slider+teks waktu,
// dipindah dari isi NowPlayingScreen langsung. Composable TERPISAH ini yang collect
// playbackProgress (bukan NowPlayingScreen di scope atas), jadi tiap tick posisi cuma
// invalidasi baris kecil ini — tidak ikut menyeret seluruh NowPlayingScreen recompose.
// Tampilan & behavior 1:1 sama seperti sebelum dipindah.
@Composable
private fun PlaybackProgressRow(
    playbackProgress: StateFlow<PlaybackProgress>,
    audiobookModeEnabled: Boolean,
    animatedAccent: Color,
    isCalmRetro: Boolean,
    songId: Long?,
    haptic: HapticFeedback,
    onSeek: (Long) -> Unit
) {
    val progress by playbackProgress.collectAsStateWithLifecycle()
    var sliderPosition by remember(progress.position) { mutableFloatStateOf(progress.position.toFloat()) }
    val progressFraction = (sliderPosition / progress.duration.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
        WaveformSeekBar(
            seed = songId ?: 0L,
            progress = progressFraction,
            playedColor = lerp(animatedAccent, Color.White, 0.2f),
            unplayedColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        )
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSeek(sliderPosition.toLong())
            },
            valueRange = 0f..(progress.duration.coerceAtLeast(1L).toFloat()),
            colors = SliderDefaults.colors(
                thumbColor = animatedAccent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        // Batch v3 upgrade — Pilar C spec palet_warna_calm_retro_v3.md ("Muted Monospace"):
        // font ketikan-mesin HANYA di data fungsional pendek (durasi waktu), sesuai literal
        // contoh spec `01:42 / 03:55` — bukan judul/lirik (larangan eksplisit §4 "JANGAN").
        val timeFontFamily = if (isCalmRetro) FontFamily.Monospace else FontFamily.Default
        Text(
            formatDuration(progress.position),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            fontFamily = timeFontFamily
        )
        Text(
            // Roadmap #12 (Mode Audiobook/Podcast, Batch 93) — "menit tersisa" alih-alih
            // total durasi untuk file yang di-opt-in mode ini, format "-mm:ss" sama seperti
            // konvensi umum podcast player (Spotify/Apple/Google Podcasts) — universal tanpa
            // perlu kata tambahan, dan langsung beda dari total durasi biasa secara visual.
            if (audiobookModeEnabled) {
                "-" + formatDuration((progress.duration - progress.position).coerceAtLeast(0))
            } else {
                formatDuration(progress.duration)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            fontFamily = timeFontFamily
        )
    }
}

// Batch 353 (Opsi A) — helper generik: collect playbackProgress di scope composable INI
// (bukan di pemanggilnya), lalu suplai position/duration ke `content` lewat parameter lambda.
// `content` adalah lambda @Composable yang, saat dipanggil dari sini, dapat restart-group-nya
// SENDIRI (pola sama seperti `content` di Box/Column bawaan Compose) — jadi tick posisi tiap
// detik cuma invalidasi isi `content`, TIDAK bocor ke scope pemanggil (blok if di
// NowPlayingScreen). Dipakai untuk LyricsSheet & ABRepeatBookmarkSheet yang perlu positionMs
// tapi punya closure (var lyricsText/bookmarks) yang lebih aman dibiarkan apa adanya di
// pemanggil, ketimbang dipindah jadi parameter composable baru.
@Composable
private fun WithLivePlaybackProgress(
    playbackProgress: StateFlow<PlaybackProgress>,
    content: @Composable (livePositionMs: Long, liveDurationMs: Long) -> Unit
) {
    val progress by playbackProgress.collectAsStateWithLifecycle()
    content(progress.position, progress.duration)
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
private fun GestureIndicatorBadge(icon: ImageVector, value: Float, accentColor: Color, label: String? = null) {
    val isTactile = isTactileTheme()
    // Batch 58 — was falling into the Apple-else branch (translucent 0.9f-alpha Surface, another
    // literal glassmorphism cue) for Skeu; now gets the same opaque + embossed treatment Tactile
    // already had, consistent with the rest of this batch's frostedGlass()/skeuEmboss() fixes.
    val isSkeu = isSkeuTheme()
    // Batch 552 — Apple/Calm Retro/Liquid Glass/Aurora: `neuSurface()` (kedalaman Neumorphism
    // Boomly); fill kini digambar mesin itu (opak), bukan Surface surface@0.9f; Surface transparan.
    Surface(
        modifier = when {
            isTactile -> Modifier.tactileEmboss(shape = RoundedCornerShape(Radius.xl), elevation = 8.dp)
            isSkeu -> Modifier.skeuEmboss(shape = RoundedCornerShape(Radius.xl), elevation = 8.dp)
            else -> Modifier.neuSurface(shape = RoundedCornerShape(Radius.xl), elevation = 8.dp)
        },
        shape = RoundedCornerShape(Radius.xl),
        color = Color.Transparent,
        // Batch 48/49 lesson: explicit contentColor, never rely on the Transparent fallback.
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
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
