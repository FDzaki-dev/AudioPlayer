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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.Player
import com.rudi.audioplayer.ui.lyrics.LyricsViewModel
import com.rudi.audioplayer.playback.EqualizerController
import com.rudi.audioplayer.playback.EqualizerUiState
import com.rudi.audioplayer.playback.PlaybackUiState
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.ui.theme.tactileEmboss
import com.rudi.audioplayer.ui.theme.skeuEmboss
import com.rudi.audioplayer.ui.theme.isTactileTheme
import com.rudi.audioplayer.ui.theme.isSkeuTheme
import com.rudi.audioplayer.ui.theme.isCalmRetroTheme
import com.rudi.audioplayer.ui.theme.calmScanlines
import com.rudi.audioplayer.ui.theme.calmAberration
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
import kotlin.math.abs
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
    // Gap List "Wajib" #1 (Tag Editor) — fire-and-forget, sama pola onSaveLyrics/onAddBookmark
    // di atas: hasil sukses/gagal muncul lewat Snackbar infoMessage/actionErrorMessage yang
    // sudah ada di MainActivity, bukan callback result langsung ke sini.
    onSaveSongTags: (com.rudi.audioplayer.data.Song, com.rudi.audioplayer.data.Id3TagWriter.EditableTags) -> Unit,
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
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showVisualizerSheet by remember { mutableStateOf(false) }
    var showAdvancedSheet by remember { mutableStateOf(false) }
    var showAbRepeatBookmarkSheet by remember { mutableStateOf(false) }
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
    val lyricsAutoState by lyricsViewModel.uiState.collectAsState()
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
    var showNowPlayingHint by remember { mutableStateOf(false) }
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
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
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
    var contentGroupHeightPx by remember { mutableStateOf(0) }
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
        val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
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
            // Batch v3 upgrade — Pilar C spec palet_warna_calm_retro_v3.md ("Muted Monospace"):
            // font ketikan-mesin HANYA di data fungsional pendek (durasi waktu), sesuai literal
            // contoh spec `01:42 / 03:55` — bukan judul/lirik (larangan eksplisit §4 "JANGAN").
            // `isCalmRetro` sudah di-hoist di atas (baris 198, dipakai bareng CTA aberration).
            val timeFontFamily = if (isCalmRetro) FontFamily.Monospace else FontFamily.Default
            Text(
                formatDuration(uiState.position),
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
                    "-" + formatDuration((uiState.duration - uiState.position).coerceAtLeast(0))
                } else {
                    formatDuration(uiState.duration)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                fontFamily = timeFontFamily
            )
        }
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
            autoUiState = lyricsAutoState,
            positionMs = uiState.position,
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
    onOpenVisualizer: () -> Unit,
    onOpenSongInfoEdit: () -> Unit,
    onOpenRingtoneCutter: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent
    ) {
        // Batch 314 — laporan user: sheet ini terpotong (baris terakhir, "Potong Nada Dering",
        // tidak terjangkau di layar pendek/font besar). Sama persis root cause & pola jaring
        // pengaman yang sudah dipakai body utama NowPlayingScreen (lihat komentar
        // `verticalScroll` di scaffold utama fungsi ini): 3 seksi + divider + slider volume TIDAK
        // pernah discroll, cuma diam-diam ke-clip di tepi layar begitu total tinggi > tinggi sheet
        // yang tersedia. Kalau konten muat (layar tinggi/gesture-nav), scroll offset tetap 0, nol
        // perubahan visual; kalau tidak muat, sekarang bisa digeser bukan hilang.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Kontrol Lanjutan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Batch 312 — sebelumnya 9 baris flat tanpa pengelompokan (permintaan user: "rapikan
            // menu utilitas yang tidak dipisahkan berdasarkan kegunaan umumnya"). Dikelompokkan
            // jadi 3 seksi ala grouped-list iOS (label kecil di atas tiap grup + divider di
            // antaranya, pola sama seperti "Peredam Dalam Aplikasi" yang sudah ada sebelumnya):
            // "Pemutaran" (kontrol JALANNYA putar lagu saat ini), "Audio" (pemrosesan/tampilan
            // sinyal audio), "Lagu" (konten/metadata per-lagu, bukan soal pemutaran real-time).
            AdvancedControlsSectionHeader("Pemutaran")
            AdvancedControlRow(
                icon = Icons.Default.QueueMusic,
                label = "Antrean Putar",
                value = null,
                onClick = onOpenQueue
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
                icon = Icons.Default.Repeat,
                label = "Repeat A-B & Bookmark",
                value = null,
                onClick = onOpenAbRepeatBookmark
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            AdvancedControlsSectionHeader("Audio")
            AdvancedControlRow(
                icon = Icons.Default.Equalizer,
                label = "Equalizer",
                value = null,
                onClick = onOpenEqualizer
            )
            AdvancedControlRow(
                icon = Icons.Default.GraphicEq,
                label = "Visualizer Audio",
                value = null,
                onClick = onOpenVisualizer
            )
            Text(
                "Peredam Dalam Aplikasi (bukan volume HP)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
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
                Icon(volumeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
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

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            AdvancedControlsSectionHeader("Lagu")
            AdvancedControlRow(
                icon = Icons.Default.Article,
                label = "Lirik",
                value = null,
                onClick = onOpenLyrics
            )
            AdvancedControlRow(
                icon = Icons.Default.Edit,
                label = "Edit Info Lagu",
                value = null,
                onClick = onOpenSongInfoEdit
            )
            AdvancedControlRow(
                icon = Icons.Default.ContentCut,
                label = "Potong Nada Dering",
                value = null,
                onClick = onOpenRingtoneCutter
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/** Batch 312 — label kecil di atas tiap grup "Kontrol Lanjutan" (Pemutaran/Audio/Lagu), gaya
 * sama persis "Peredam Dalam Aplikasi" yang sudah ada sebelumnya (labelSmall + secondary),
 * supaya terasa 1 sistem konsisten, bukan pola baru yang asing di sheet ini. */
@Composable
private fun AdvancedControlsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 4.dp)
    )
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
            val starInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRate(if (rating == star) 0 else star)
                },
                interactionSource = starInteraction,
                modifier = Modifier.bouncyPress(starInteraction, pressedScale = 0.75f)
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
 * the old spinning vinyl. Horizontal swipe-to-skip gesture logic is unchanged from before.
 * Batch 346 — `artSize` baru (dulu literal 280.dp hardcode di sini): caller (NowPlayingScreen)
 * sekarang menghitung ukuran dinamis ("art scale dinamis") dan meneruskannya ke sini. Glow
 * Box tetap `artSize + 20.dp` (rasio 300/280 lama dipertahankan persis). */
@Composable
private fun AlbumArtHero(
    artworkUri: Uri?,
    accentColor: Color,
    artSize: Dp,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var totalDrag by remember { mutableStateOf(0f) }
    // Batch 178 — Now Playing item 10/11 fix: swipe-to-skip di sini sebelumnya 0 feedback
    // visual selama drag berlangsung (cuma haptic SEKALI di dragEnd kalau lolos threshold
    // 120px) — beda dari gesture brightness/volume di Box induk (GestureIndicatorBadge
    // muncul LIVE mengikuti drag). User tidak tahu sudah "cukup jauh" menggeser sampai
    // jarinya dilepas. `dragOffset` bikin art ikut bergeser mengikuti jari (clamp ±48dp,
    // damped 0.5x — bukan 1:1, supaya tidak terkesan bisa diseret jauh tak terbatas) lalu
    // spring balik ke tengah begitu jari dilepas/gesture dibatalkan. Threshold/logic
    // swipe-next/prev itu sendiri (totalDrag, 120px) SAMA SEKALI TIDAK DIUBAH — murni layer
    // visual tambahan di atasnya, bukan perubahan playback/navigation logic.
    val dragOffset = remember { Animatable(0f) }
    val dragScope = rememberCoroutineScope()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer { translationX = dragOffset.value }
            .pointerInput(Unit) {
            val maxOffsetPx = 48.dp.toPx()
            // Batch 256 — POLISH_AUDIT §Motion: stiffness = Spring.StiffnessLow ditambah ke 2
            // spring snap-back di bawah (onDragEnd + onDragCancel), dulu default (Medium),
            // beda dari bouncyPress (Utils.kt) & entrance spring (baris ~410) yg sama-sama pakai
            // StiffnessLow eksplisit — biar swipe-snap terasa 1 sistem sama animasi bouncy lain
            // di screen ini, bukan 2 "rasa" beda. dampingRatio (MediumBouncy) tidak diubah.
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
                    dragScope.launch { dragOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
                },
                onDragCancel = {
                    dragScope.launch { dragOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
                },
                onHorizontalDrag = { change, dragAmount ->
                    totalDrag += dragAmount
                    change.consume()
                    dragScope.launch { dragOffset.snapTo((totalDrag * 0.5f).coerceIn(-maxOffsetPx, maxOffsetPx)) }
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
        // v3 upgrade — Pilar A spec palet_warna_calm_retro_v3.md (CRT scanlines), dipasang di
        // bawah lewat .calmScanlines() SETELAH .clip(heroShape) (bukan sebelum, beda dari
        // teknik shadow Tactile/Skeu di atas yang sengaja bocor sebelum clip) — scanline harus
        // terkurung rapi di dalam bentuk album art, tidak boleh meluber ke luar shape.
        val isCalmRetroHero = isCalmRetroTheme()
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
        // Batch 347 — user pilih lanjut sempurnakan trade-off yang sengaja ditunda Batch 346
        // ("Radius.hero ikut skala"). Baseline referensi TETAP 280dp (konsisten dgn konvensi
        // `dynamicArtSize` Batch 346 yang sengaja balik ke 280dp persis di layar 360dp lebar) —
        // rasio artSize aktual thd baseline ini dikalikan ke `Radius.hero` (28dp, Spacing.kt)
        // supaya sudut piringan tetap PROPORSIONAL secara visual di ukuran manapun (piringan
        // besar = sudut ikut besar, piringan kecil = sudut ikut kecil), bukan radius absolut
        // tetap yang terlihat makin "tajam"/kurang membulat relatif saat piringan membesar (atau
        // sebaliknya berlebihan membulat saat mengecil). `Dp.div(Dp): Float` & `Dp.times(Float):
        // Dp` — dicek ulang lewat dokumentasi resmi Compose sebelum dipakai (operator baku kelas
        // `Dp`, bukan API custom) — `Dp * Float` sendiri sudah ada presedennya di file ini
        // (`screenHeightDp * 0.28f`, baris `albumArtBoxHeight`).
        // Token `Radius.hero` GLOBAL ITU SENDIRI (Spacing.kt) TIDAK disentuh — dipakai HANYA sbg
        // nilai baseline di sini, bukan diubah jadi dinamis (token itu dipakai juga di Theme.kt
        // utk `MaterialTheme.shapes.large`, dampak globalnya jauh di luar 1 layar ini).
        // Cabang Tactile/Skeu (`isPanelTheme -> MaterialTheme.shapes.large`) SENGAJA TIDAK ikut
        // diskalakan — 2 identitas itu memang didesain pakai bahasa sudut SERAGAM lintas berbagai
        // ukuran permukaan (panel/sheet/kartu lain di app ini semua pakai radius theme yang sama,
        // bukan proporsional per-objek); mengikutkan hero art di sini justru bikin hero beda
        // sendiri dari permukaan besar lain di identitas yang sama — kebalikan dari konsistensi
        // yang justru diinginkan bahasa desain panel itu. Scope PERSIS sesuai literal yang
        // dikonfirmasi user: "Radius.hero ikut skala" — token itu spesifik cuma dipakai di cabang
        // non-panel (Apple/default) ini.
        val heroCornerRadius = Radius.hero * (artSize / 280.dp)
        val heroShape = if (isPanelTheme) MaterialTheme.shapes.large else RoundedCornerShape(heroCornerRadius)
        Box(
            modifier = Modifier
                .size(artSize + 20.dp)
                .blur(90.dp)
                .background(accentColor.copy(alpha = 0.38f), CircleShape)
        )
        AlbumArt(
            artworkUri = artworkUri,
            modifier = Modifier
                .size(artSize)
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
                                    // skeuEmboss()). Batch 346 — hero art ini TIDAK LAGI selalu
                                    // 280.dp (sekarang `artSize` dinamis, lihat definisi fungsi) —
                                    // TAPI margin halo 18dp SENGAJA tetap literal, bukan diikutkan
                                    // skala: ini jarak bayangan-ke-tepi-shape yang wajar konstan
                                    // di seluruh rentang ukuran (140dp s/d lebar layar), bukan
                                    // proporsi visual yang perlu ikut membesar/mengecil bareng art.
                                    // 18dp tetap cukup longgar utk tidak memotong bentuk bayangan
                                    // sendiri di ukuran manapun, sekaligus batas tegas yg dijamin
                                    // tidak dilewati.
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
                .then(if (isCalmRetroHero) Modifier.calmScanlines() else Modifier)
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
            // Batch 318 (laporan user, screenshot) — Column ini beda dari 5 sheet yang sudah
            // diaudit Batch 314-316 (semua ModalBottomSheet): SpeedDialog ini AlertDialog, jadi
            // luput dari audit "pola tab serupa" yang scope-nya cuma ModalBottomSheet. Simptom
            // & root cause PERSIS sama: total tinggi konten (6 opsi Kecepatan + toggle Mode
            // Audiobook + 2 opsi Transisi Antar Lagu dengan subtitle panjang) melebihi tinggi
            // yang dialokasikan Material3 AlertDialog ke slot `text`, baris paling bawah
            // ("Fade Halus" subtitle) diam-diam ke-clip alih-alih bisa digeser.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Kecepatan",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                // Batch 163 (Micro UI/UX kategori #5, Interactive States — selected/active
                // consistency) — dulu di sini TextButton polos + teks "✓" (dan warna teks
                // berubah) buat menandai speed yang aktif, PADAHAL daftar pilihan-tunggal LAIN
                // di dialog yang SAMA persis ini (Transisi Antar Lagu, langsung di bawah lewat
                // TransitionModeOption) sudah pakai widget RadioButton sungguhan. Dua bahasa
                // visual beda utk konsep yang identik (pilih 1 dari beberapa opsi), padahal
                // cuma dipisah 1 Divider — disamakan ke pola RadioButton yang sama.
                options.forEach { speed ->
                    val isSelected = speed == currentSpeed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.md))
                            .selectable(
                                selected = isSelected,
                                onClick = { onSelect(speed) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${speed}x", style = MaterialTheme.typography.bodyMedium)
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
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
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
