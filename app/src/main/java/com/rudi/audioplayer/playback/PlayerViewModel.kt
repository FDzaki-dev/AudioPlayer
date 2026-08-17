package com.rudi.audioplayer.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.rudi.audioplayer.util.AppLogger
import com.rudi.audioplayer.data.AudiobookModeStore
import com.rudi.audioplayer.data.Bookmark
import com.rudi.audioplayer.data.BookmarkStore
import com.rudi.audioplayer.data.CrossfadeStore
import com.rudi.audioplayer.data.CustomFolderInfo
import com.rudi.audioplayer.data.CustomFolderScanner
import com.rudi.audioplayer.data.CustomFolderStore
import com.rudi.audioplayer.data.FavoritesStore
import com.rudi.audioplayer.data.LyricsStore
import com.rudi.audioplayer.data.MusicRepository
import com.rudi.audioplayer.data.PlaybackStateStore
import com.rudi.audioplayer.data.AppLockStore
import com.rudi.audioplayer.data.HourlyListenStore
import com.rudi.audioplayer.data.ListeningHistoryStore
import com.rudi.audioplayer.data.ListeningStatsEngine
import com.rudi.audioplayer.data.PlayStatsStore
import com.rudi.audioplayer.data.RatingStore
import com.rudi.audioplayer.data.RadioSettingsStore
import com.rudi.audioplayer.data.ShakeSettingsStore
import com.rudi.audioplayer.data.SilenceSkipStore
import com.rudi.audioplayer.data.Playlist
import com.rudi.audioplayer.data.PlaylistStore
import com.rudi.audioplayer.data.SmartPlaylist
import com.rudi.audioplayer.data.SmartPlaylistStore
import com.rudi.audioplayer.data.ThemeStore
import com.rudi.audioplayer.data.VisualizerSettingsStore
import com.rudi.audioplayer.ui.theme.ThemeIdentity
import com.rudi.audioplayer.ui.theme.ThemeMode
import com.rudi.audioplayer.data.Song
import com.rudi.audioplayer.widget.WidgetUpdater
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

data class PlaybackUiState(
    val currentSong: Song? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1f,
    val volume: Float = 1f,
    val queue: List<Song> = emptyList(),
    // Mirrors `queue` 1:1. A slot's id follows its song when the queue is reordered, instead
    // of being tied to position — so the Queue sheet can key list items on something that
    // stays stable across a move, which is what actually lets item-move animate smoothly
    // instead of every row appearing to be swapped out for a new one.
    val queueSlotIds: List<Long> = emptyList()
)

class PlayerViewModel(private val appContext: Context) : ViewModel() {

    private var controller: MediaController? = null
    // Batch 78 — fix: onCleared() called `controller?.release()`, which is a no-op if
    // controllerFuture hasn't resolved yet (controller still null at that point — e.g. the
    // ViewModel is cleared almost immediately after connect(), a fast rotation/nav-away before
    // the Media3 session handshake finishes). Nothing then ever cancels/releases the in-flight
    // future, so its listener stays registered and the async connection to PlaybackService keeps
    // resolving after the ViewModel is already gone — a real (if narrow-window) connection leak.
    // Kept as a field so onCleared() can always reach it, resolved or not.
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var currentQueue: List<Song> = emptyList()
    private var currentQueueSlotIds: List<Long> = emptyList()
    private var nextQueueSlotId: Long = 0L

    private fun newSlotIds(count: Int): List<Long> = List(count) { nextQueueSlotId++ }

    private val favoritesStore = FavoritesStore(appContext)
    private val playbackStateStore = PlaybackStateStore(appContext)
    private val playStatsStore = PlayStatsStore(appContext)
    private val ratingStore = RatingStore(appContext)
    private val appLockStore = AppLockStore(appContext)
    private val listeningHistoryStore = ListeningHistoryStore(appContext)
    private val hourlyListenStore = HourlyListenStore(appContext)
    private val shakeSettingsStore = ShakeSettingsStore(appContext)
    private val radioSettingsStore = RadioSettingsStore(appContext)
    private val floatingBubbleStore = com.rudi.audioplayer.data.FloatingBubbleStore(appContext)

    private val _shakeToSkipEnabled = MutableStateFlow(shakeSettingsStore.isEnabled())
    val shakeToSkipEnabled: StateFlow<Boolean> = _shakeToSkipEnabled.asStateFlow()

    fun setShakeToSkipEnabled(enabled: Boolean) {
        shakeSettingsStore.setEnabled(enabled)
        _shakeToSkipEnabled.value = enabled
    }

    // Roadmap #11, Floating Mini Player — nilai ini murni CATATAN preferensi user (dicek lagi
    // via Settings.canDrawOverlays() di MainActivity sebelum benar-benar start/stop service);
    // ViewModel sengaja tidak start/stop Service Android di sini (butuh Context Activity utk
    // permission launcher-nya), murni simpan preferensi seperti store lain di file ini.
    private val _floatingBubbleEnabled = MutableStateFlow(floatingBubbleStore.isEnabled())
    val floatingBubbleEnabled: StateFlow<Boolean> = _floatingBubbleEnabled.asStateFlow()

    fun setFloatingBubbleEnabled(enabled: Boolean) {
        floatingBubbleStore.setEnabled(enabled)
        _floatingBubbleEnabled.value = enabled
    }

    /** Batch 100 — bubble sekarang bisa ditoggle dari LUAR ViewModel ini sama sekali (Quick
     * Settings Tile, lihat BubbleTileService.kt, baca/tulis langsung ke [floatingBubbleStore]
     * tanpa lewat StateFlow di sini). Dipanggil dari MainActivity's resume-effect supaya switch
     * di SettingsScreen tidak nyangkut nunjukin state basi kalau user toggle dari tile lalu
     * balik ke app. */
    fun refreshFloatingBubbleEnabled() {
        _floatingBubbleEnabled.value = floatingBubbleStore.isEnabled()
    }

    // Roadmap #8, Trim Keheningan Otomatis — beda pola dari toggle lain di atas: nilainya
    // TIDAK cukup disimpan ke store doang, harus juga di-relay LIVE ke ExoPlayer di
    // PlaybackService lewat custom SessionCommand (ACTION_SET_SKIP_SILENCE), karena
    // setSkipSilenceEnabled() adalah method ExoPlayer, bukan bagian interface Player umum yang
    // diekspos MediaController — lihat PlaybackService.onCustomCommand().
    private val silenceSkipStore = SilenceSkipStore(appContext)
    private val _silenceSkipEnabled = MutableStateFlow(silenceSkipStore.isEnabled())
    val silenceSkipEnabled: StateFlow<Boolean> = _silenceSkipEnabled.asStateFlow()

    fun setSilenceSkipEnabled(enabled: Boolean) {
        silenceSkipStore.setEnabled(enabled)
        _silenceSkipEnabled.value = enabled
        val args = Bundle().apply { putBoolean(PlaybackService.EXTRA_SKIP_SILENCE_ENABLED, enabled) }
        controller?.sendCustomCommand(SessionCommand(PlaybackService.ACTION_SET_SKIP_SILENCE, Bundle.EMPTY), args)
        // controller == null (belum sempat konek): tidak fatal — PlaybackService baca nilai
        // yang baru saja disimpan store di atas sendiri saat ExoPlayer-nya pertama kali dibuat
        // (lihat onCreate di PlaybackService.kt), jadi tetap sinkron begitu Service benar-benar
        // start.
    }

    private val _radioAutoContinueEnabled = MutableStateFlow(radioSettingsStore.isEnabled())
    val radioAutoContinueEnabled: StateFlow<Boolean> = _radioAutoContinueEnabled.asStateFlow()

    fun setRadioAutoContinueEnabled(enabled: Boolean) {
        radioSettingsStore.setEnabled(enabled)
        _radioAutoContinueEnabled.value = enabled
    }

    private val _lockEnabled = MutableStateFlow(appLockStore.isLockEnabled())
    val lockEnabled: StateFlow<Boolean> = _lockEnabled.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(appLockStore.isBiometricEnabled())
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    fun setPin(pin: String) {
        appLockStore.setPin(pin)
        _lockEnabled.value = true
    }

    fun disableLock() {
        appLockStore.disableLock()
        _lockEnabled.value = false
        _biometricEnabled.value = false
    }

    fun setBiometricEnabled(enabled: Boolean) {
        appLockStore.setBiometricEnabled(enabled)
        _biometricEnabled.value = enabled
    }

    fun verifyPin(pin: String): AppLockStore.PinResult = appLockStore.verifyPin(pin)

    /** Lets the lock screen restore an in-progress lockout countdown after a process restart. */
    fun currentPinLockout(): Long? = appLockStore.lockedOutUntil()
    private val playlistStore = PlaylistStore(appContext)
    private val lyricsStore = LyricsStore(appContext)
    private val bookmarkStore = BookmarkStore(appContext)
    private val audiobookModeStore = AudiobookModeStore(appContext)
    private val _audiobookModeEnabled = MutableStateFlow(false)
    /** Whether the CURRENT song (whatever's playing/loaded right now) has Roadmap #12 opted in —
     * recomputed on every song transition, not a per-song lookup table exposed to the UI. */
    val audiobookModeEnabled: StateFlow<Boolean> = _audiobookModeEnabled.asStateFlow()
    private val equalizerController = EqualizerController(appContext)
    val equalizerState: StateFlow<EqualizerUiState> = equalizerController.state

    // --- Visualizer Audio (Roadmap #9, Batch 92) ---
    private val visualizerSettingsStore = VisualizerSettingsStore(appContext)
    private val audioVisualizerController = AudioVisualizerController()
    val visualizerBars: StateFlow<FloatArray> = audioVisualizerController.bars
    val visualizerSupported: StateFlow<Boolean> = audioVisualizerController.supported

    private val _visualizerEnabled = MutableStateFlow(visualizerSettingsStore.isEnabled())
    val visualizerEnabled: StateFlow<Boolean> = _visualizerEnabled.asStateFlow()

    private val crossfadeStore = CrossfadeStore(appContext)
    private val customFolderStore = CustomFolderStore(appContext)
    private val themeStore = ThemeStore(appContext)
    // Batch 61 — dulu 1 StateFlow<AppTheme> gabungan; sekarang 2 StateFlow independen supaya
    // identitas tema & mode terang/gelap bisa diubah terpisah dari mana pun (SettingsScreen).
    private val _themeIdentity = MutableStateFlow(themeStore.getIdentity())
    val themeIdentity: StateFlow<ThemeIdentity> = _themeIdentity.asStateFlow()
    private val _themeMode = MutableStateFlow(themeStore.getMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    private val customFolderScanner = CustomFolderScanner(appContext)

    private val _customFolders = MutableStateFlow(loadCustomFolderInfos())
    val customFolders: StateFlow<List<CustomFolderInfo>> = _customFolders.asStateFlow()
    private var userTargetVolume = 1f
    private var positionTick = 0

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val _statsVersion = MutableStateFlow(0)
    val statsVersion: StateFlow<Int> = _statsVersion.asStateFlow()

    private val _crossfadeEnabled = MutableStateFlow(crossfadeStore.isEnabled())
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _playlists = MutableStateFlow(playlistStore.getPlaylists())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val smartPlaylistStore = SmartPlaylistStore(appContext)
    private val _smartPlaylists = MutableStateFlow(smartPlaylistStore.getSmartPlaylists())
    val smartPlaylists: StateFlow<List<SmartPlaylist>> = _smartPlaylists.asStateFlow()

    private val _favoriteIds = MutableStateFlow(loadFavoriteIds())
    val favoriteIds: StateFlow<ImmutableSet<Long>> = _favoriteIds.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()
    private var sleepTimerJob: Job? = null
    private val sleepTimerStore = com.rudi.audioplayer.data.SleepTimerStore(appContext)

    init {
        // Gap List #7 (Batch 109) — restore TAMPILAN countdown setelah ViewModel dibuat ulang
        // (mis. proses sempat mati, user buka app lagi selagi Service masih menghitung mundur
        // di background). Eksekusi nyata (pause tepat waktu) tidak bergantung pada blok ini
        // sama sekali — itu urusan `PlaybackService.resumeSleepTimerFromStore()`, sudah jalan
        // sendiri begitu Service dibuat, terlepas dari ViewModel ini pernah query store atau
        // tidak. Blok ini murni supaya UI tidak "lupa" ada timer aktif kalau layar dibuka ulang.
        val endAt = sleepTimerStore.getEndAt()
        if (endAt != null && endAt > System.currentTimeMillis()) {
            _sleepTimerRemaining.value = endAt - System.currentTimeMillis()
            sleepTimerJob = viewModelScope.launch {
                while (true) {
                    val remaining = endAt - System.currentTimeMillis()
                    if (remaining <= 0) break
                    _sleepTimerRemaining.value = remaining
                    delay(1000)
                }
                _sleepTimerRemaining.value = null
            }
        }
    }


    // A-B Repeat (Roadmap #4, Batch 91) — points live here rather than in PlaybackUiState
    // because they're checked every 500ms tick in startPositionLoop() and don't need to
    // trigger a full uiState recomposition on their own; the sheet observes them directly.
    private val _abRepeatPointA = MutableStateFlow<Long?>(null)
    val abRepeatPointA: StateFlow<Long?> = _abRepeatPointA.asStateFlow()
    private val _abRepeatPointB = MutableStateFlow<Long?>(null)
    val abRepeatPointB: StateFlow<Long?> = _abRepeatPointB.asStateFlow()

    private val _accentColor = MutableStateFlow<Color?>(null)
    val accentColor: StateFlow<Color?> = _accentColor.asStateFlow()
    private var accentColorJob: Job? = null

    private val _currentRating = MutableStateFlow(0)
    val currentRating: StateFlow<Int> = _currentRating.asStateFlow()

    fun setCurrentSongRating(stars: Int) {
        val songId = _uiState.value.currentSong?.id ?: return
        ratingStore.setRating(songId, stars)
        _currentRating.value = ratingStore.getRating(songId)
    }

    // A small, one-shot delight when a real listening milestone is crossed — never a nag to
    // open the app more, just an occasional "nice, look how much you've listened" moment that
    // only ever fires as a side effect of listening the user was already doing.
    private val _celebrationMessage = MutableStateFlow<String?>(null)
    val celebrationMessage: StateFlow<String?> = _celebrationMessage.asStateFlow()

    fun consumeCelebrationMessage() {
        _celebrationMessage.value = null
    }

    // Same one-shot pattern as celebrationMessage, so a playback failure (deleted/corrupt
    // file, unreadable SAF folder, etc.) surfaces as a Snackbar instead of the player just
    // going silent with no explanation.
    private val _playbackErrorMessage = MutableStateFlow<String?>(null)
    val playbackErrorMessage: StateFlow<String?> = _playbackErrorMessage.asStateFlow()

    fun consumePlaybackErrorMessage() {
        _playbackErrorMessage.value = null
    }

    // Same one-shot pattern again, tapi untuk kegagalan aksi di luar playback (mis. gagal
    // menambahkan folder tambahan) — dipisah dari playbackErrorMessage supaya namanya tetap
    // jujur soal konteksnya, bukan dipakai ulang untuk hal yang tidak berhubungan.
    private val _actionErrorMessage = MutableStateFlow<String?>(null)
    val actionErrorMessage: StateFlow<String?> = _actionErrorMessage.asStateFlow()

    fun consumeActionErrorMessage() {
        _actionErrorMessage.value = null
    }

    // Same one-shot pattern again, untuk konfirmasi ringan non-error dan non-undoable
    // (mis. "disalin ke papan klip", "ditambahkan ke antrean") — sebelumnya beberapa layar
    // pakai Toast mentah untuk ini, yang mengabaikan tema gelap/terang app dan posisinya beda
    // dari SnackbarHost yang sudah ada. Disatukan lewat kanal ini supaya semua konfirmasi
    // ringan tampil konsisten.
    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    fun showInfoMessage(message: String) {
        _infoMessage.value = message
    }

    fun consumeInfoMessage() {
        _infoMessage.value = null
    }

    /** Carries both the Snackbar message and the exact action that reverses it — the Snackbar
     * itself doesn't need to know *what* was removed, only how to undo it. */
    data class UndoableAction(val message: String, val undo: () -> Unit)

    private val _undoableAction = MutableStateFlow<UndoableAction?>(null)
    val undoableAction: StateFlow<UndoableAction?> = _undoableAction.asStateFlow()

    fun consumeUndoableAction() {
        _undoableAction.value = null
    }

    private val _librarySongs = MutableStateFlow<List<Song>>(emptyList())
    val librarySongs: StateFlow<List<Song>> = _librarySongs.asStateFlow()

    private val _libraryLoading = MutableStateFlow(true)
    val libraryLoading: StateFlow<Boolean> = _libraryLoading.asStateFlow()

    private var libraryLoadedOnce = false
    private var libraryRefreshJob: Job? = null
    private var libraryRefreshGeneration = 0L
    private val musicRepository = MusicRepository(appContext)

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            if (!isPlaying) persistPlaybackState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = controller?.currentMediaItemIndex ?: 0
            val song = currentQueue.getOrNull(index)
            _uiState.value = _uiState.value.copy(
                currentSong = song,
                currentIndex = index,
                duration = controller?.duration?.coerceAtLeast(0) ?: 0L
            )
            song?.let {
                playStatsStore.recordPlay(it.id)
                listeningHistoryStore.recordPlay(it.id)
                hourlyListenStore.recordPlay()
                _statsVersion.value += 1
                checkListeningMilestone()
            }
            updateAccentColor(song)
            _currentRating.value = song?.let { ratingStore.getRating(it.id) } ?: 0
            persistPlaybackState()

            // Roadmap #12 (Mode Audiobook/Podcast, Batch 93) — resume THIS song's own remembered
            // speed + position, independent of whatever speed the previous track left behind
            // (that "carries over to every song" behavior is exactly what the roadmap flags as
            // the thing this feature replaces, per-song, for opted-in files). Skipped on
            // MEDIA_ITEM_TRANSITION_REASON_REPEAT (REPEAT_MODE_ONE looping the same item) — re-
            // seeking to a stale saved position on every loop would fight repeat-one's own
            // restart-from-zero behavior instead of just looping cleanly.
            val audiobookState = song?.let { audiobookModeStore.get(it.id) }
            _audiobookModeEnabled.value = audiobookState?.enabled == true
            if (audiobookState?.enabled == true && reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                controller?.setPlaybackSpeed(audiobookState.speed)
                if (audiobookState.lastPositionMs > 0) controller?.seekTo(audiobookState.lastPositionMs)
            }

            // Batch 102 — dulu di sini ada startFadeIn() (fade volume single-player). True
            // crossfade sekarang seluruhnya dikelola CrossfadeEngine di PlaybackService (dua
            // ExoPlayer overlap sungguhan) — ViewModel ini cuma pegang MediaController, tidak
            // punya akses ke ExoPlayer mentah, jadi tidak ada lagi kerja fade di sisi sini sama
            // sekali. Lihat CrossfadeEngine.kt.

            // A-B Repeat is scoped to one song, never carried to the next — a stale point B
            // from the previous track would either never trigger (positions rarely line up) or
            // worse, silently clip the start of a new song if it happened to be short enough.
            _abRepeatPointA.value = null
            _abRepeatPointB.value = null
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.value = _uiState.value.copy(shuffleEnabled = shuffleModeEnabled)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.value = _uiState.value.copy(repeatMode = repeatMode)
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _uiState.value = _uiState.value.copy(playbackSpeed = playbackParameters.speed)
        }

        override fun onVolumeChanged(volume: Float) {
            _uiState.value = _uiState.value.copy(volume = volume)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                continuePlaybackIfQueueEnded()
            }
        }

        // Without this, a deleted/moved/corrupt file just silently stops the player with
        // nothing shown to the user. Logs locally (never transmitted) and tries to keep
        // the music going by skipping to the next queued track when one exists.
        override fun onPlayerError(error: PlaybackException) {
            val index = controller?.currentMediaItemIndex ?: -1
            val song = currentQueue.getOrNull(index)
            val label = song?.let { "${it.title} — ${it.artist}" } ?: "Lagu ini"
            AppLogger.e("PlayerViewModel", "Playback error pada index=$index ($label)", error)
            _playbackErrorMessage.value = "$label tidak bisa diputar (file mungkin dihapus atau rusak)."

            val c = controller
            if (c != null && c.hasNextMediaItem()) {
                c.seekToNextMediaItem()
                c.play()
            }
        }
    }

    /**
     * When the queue plays through to the end with repeat off, keep the music going with
     * more songs from the library instead of falling silent — the "radio continues" feel
     * of Spotify/YouTube Music, built from purely local data (no streaming catalog needed).
     */
    private fun continuePlaybackIfQueueEnded() {
        if (!_radioAutoContinueEnabled.value) return
        val c = controller ?: return
        if (c.repeatMode != Player.REPEAT_MODE_OFF) return
        val library = _librarySongs.value
        if (library.isEmpty()) return

        val queuedIds = currentQueue.map { it.id }.toSet()
        val candidates = library.filter { it.id !in queuedIds }
        val pool = if (candidates.isEmpty()) library else candidates
        val toAdd = pool.shuffled().take(20)
        if (toAdd.isEmpty()) return

        c.addMediaItems(toAdd.map { mediaItemFor(it) })
        currentQueue = currentQueue + toAdd
        currentQueueSlotIds = currentQueueSlotIds + newSlotIds(toAdd.size)
        _uiState.value = _uiState.value.copy(queue = currentQueue, queueSlotIds = currentQueueSlotIds)
        c.seekToNextMediaItem()
        c.play()
    }

    fun connect() {
        val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            controller = future.get()
            controller?.addListener(playerListener)
            startPositionLoop()
        }, Executor { it.run() }) // same-thread executor — Guava's directExecutor() had no special behavior beyond this
        ensureLibraryLoaded()
        registerLibraryContentObserver()
    }

    private var libraryContentObserver: ContentObserver? = null
    private var libraryAutoRefreshJob: Job? = null

    /**
     * Watches MediaStore so a file added/removed by another app (a file manager, a sync tool)
     * while this app is already open in the foreground shows up without the user having to
     * background-and-resume or hit "Pindai Ulang" manually. MediaStore can fire a burst of
     * change notifications for one bulk operation, so each one just restarts a short debounce
     * instead of triggering a rescan per notification.
     */
    private fun registerLibraryContentObserver() {
        if (libraryContentObserver != null) return
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                libraryAutoRefreshJob?.cancel()
                libraryAutoRefreshJob = viewModelScope.launch {
                    delay(1500)
                    refreshLibrary()
                }
            }
        }
        runCatching {
            appContext.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        }.onFailure { AppLogger.e("PlayerViewModel", "Gagal mendaftarkan pengamat library", it) }
        libraryContentObserver = observer
    }

    /** Scans MediaStore once and caches the result so Home/Library/Playlist don't each scan independently. */
    fun ensureLibraryLoaded() {
        if (libraryLoadedOnce) return
        refreshLibrary()
    }

    /** Forces a fresh MediaStore scan (used by the Library screen's "Pindai Ulang" button). */
    fun refreshLibrary() {
        libraryLoadedOnce = true
        libraryRefreshJob?.cancel()
        val generation = ++libraryRefreshGeneration
        libraryRefreshJob = viewModelScope.launch {
            _libraryLoading.value = true
            // Gap List #5: refresh the permission-status badge every scan, not just right
            // after add/remove — a grant can be revoked from outside the app at any time
            // with no callback, so "was true last time we checked" can go stale silently.
            _customFolders.value = loadCustomFolderInfos()
            try {
                val songs = withContext(Dispatchers.IO) {
                    val mediaStoreSongs = musicRepository.getAllSongs()
                    val mediaStoreSignatures = mediaStoreSongs.asSequence()
                        .map(::dedupeSignature)
                        .toHashSet()
                    val customSongs = customFolderStore.getFolderUris().asSequence().flatMap { uriString ->
                        val uri = Uri.parse(uriString)
                        // Gap List #5: check the OS grant BEFORE attempting to scan, not just
                        // catch the SecurityException after the fact. A revoked grant would
                        // otherwise throw + log identically on every single refresh forever
                        // (content observer fires often) — this makes the "already known gone"
                        // case a cheap no-op instead of a repeated failed IO attempt + log spam,
                        // while still logging once here for the case a permission check itself
                        // errors out (different from "permission confirmed absent").
                        if (!hasPersistedReadPermission(uri)) return@flatMap emptySequence()
                        try {
                            customFolderScanner.scan(uri, folderLabelFor(uri)).asSequence()
                        } catch (e: Exception) {
                            // Lagu-lagu folder ini hilang diam-diam dari library sampai kejadian
                            // ini dicatat — folder lain tetap discan normal (satu folder gagal
                            // tidak menggagalkan seluruh refresh).
                            AppLogger.e("PlayerViewModel", "Gagal scan folder tambahan '$uriString'", e)
                            emptySequence()
                        }
                    }.toList()

                    // Prefer the MediaStore copy when a SAF folder is also indexed by MediaStore.
                    val dedupedCustomSongs = customSongs.filterNot {
                        dedupeSignature(it) in mediaStoreSignatures
                    }
                    mediaStoreSongs + dedupedCustomSongs
                }
                // A newer refresh may have started while this scan was running. Never let an
                // older, slower scan overwrite the newer result.
                if (generation == libraryRefreshGeneration) {
                    _librarySongs.value = songs
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A transient MediaStore/SAF failure must not permanently mark the library as
                // loaded. The next ensureLibraryLoaded() call can retry safely — but the
                // failure itself is still worth a local record, since it used to vanish here.
                AppLogger.e("PlayerViewModel", "Gagal memindai library", e)
                if (generation == libraryRefreshGeneration) {
                    libraryLoadedOnce = false
                }
            } finally {
                if (generation == libraryRefreshGeneration) {
                    _libraryLoading.value = false
                }
            }
        }
    }

    /** A cross-source identity key: title+artist+duration survives a song being seen once via
     * MediaStore and once via a raw SAF folder scan, even though those paths assign unrelated
     * IDs. Duration is bucketed to the nearest second since MediaStore and
     * MediaMetadataRetriever can report durations a handful of milliseconds apart for the
     * exact same file. */
    private fun dedupeSignature(song: Song): Triple<String, String, Long> = Triple(
        song.title.trim().lowercase(),
        song.artist.trim().lowercase(),
        song.duration / 1000
    )

    /** Grants persistent access to a folder the user picked via the system folder picker,
     * remembers it, and rescans so its audio shows up immediately. */
    fun addCustomFolder(treeUri: Uri) {
        try {
            appContext.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Sebelumnya gagal 100% diam-diam — user memilih folder, tidak terjadi apa-apa, dan
            // tidak ada cara untuk tahu kenapa. Sekarang dicatat dan dikabari lewat Snackbar.
            AppLogger.e("PlayerViewModel", "Gagal ambil izin folder tambahan", e)
            _actionErrorMessage.value = "Gagal menambahkan folder — izin ditolak sistem."
            return
        }
        customFolderStore.addFolder(treeUri.toString())
        _customFolders.value = loadCustomFolderInfos()
        refreshLibrary()
    }

    fun removeCustomFolder(uriString: String) {
        try {
            appContext.contentResolver.releasePersistableUriPermission(
                Uri.parse(uriString),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Permission may already be gone (e.g. folder moved/deleted) — still forget it locally.
        }
        customFolderStore.removeFolder(uriString)
        _customFolders.value = loadCustomFolderInfos()
        refreshLibrary()
    }

    fun setThemeIdentity(identity: ThemeIdentity) {
        themeStore.setIdentity(identity)
        _themeIdentity.value = identity
        // Batch 68: widget never redrew on theme switch — nothing called updateAll() from
        // here before. Identity itself doesn't change widget colors (Tactile/Skeu stay
        // dark-only), but keeping both setters symmetrical avoids the same bug resurfacing
        // if a future batch gives the widget an identity-aware look too.
        // Batch 72: moved off the caller's thread (this fires directly from a Compose click
        // handler on Main) — WidgetUpdater.updateAll() decodes/crops/rounds a bitmap
        // (loadThumbnail + Canvas work), the exact class of blocking disk+CPU work
        // PlaybackService.pushWidgetUpdate already avoids doing on Main (see its own Batch 34
        // note). Not the root cause of the widget staying visually frozen across theme
        // switches — see PROJECT_STATE.md for what's still unconfirmed there — but leaving
        // Main-thread I/O in a newly-added call path is a real bug on its own regardless.
        viewModelScope.launch(Dispatchers.IO) { WidgetUpdater.updateAll(appContext) }
    }

    fun setThemeMode(mode: ThemeMode) {
        themeStore.setMode(mode)
        _themeMode.value = mode
        // Batch 68: this is the call that actually flips the widget's light/dark background.
        // Batch 72: see setThemeIdentity() above — same Main-thread-I/O fix applied here.
        viewModelScope.launch(Dispatchers.IO) { WidgetUpdater.updateAll(appContext) }
    }

    private fun loadCustomFolderInfos(): List<CustomFolderInfo> =
        customFolderStore.getFolderUris().map { uriString ->
            val uri = Uri.parse(uriString)
            CustomFolderInfo(
                uri = uriString,
                displayName = folderLabelFor(uri),
                permissionGranted = hasPersistedReadPermission(uri)
            )
        }

    /** Gap List #5 — the only reliable way to know a SAF grant is still alive: the system
     * never tells the app when a grant is revoked from outside (no broadcast/callback), so
     * this has to be checked freshly against the live list rather than cached anywhere. */
    private fun hasPersistedReadPermission(uri: Uri): Boolean =
        appContext.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }

    private fun folderLabelFor(treeUri: Uri): String =
        DocumentFile.fromTreeUri(appContext, treeUri)?.name ?: "Folder Tambahan"

    private fun startPositionLoop() {
        viewModelScope.launch {
            while (true) {
                val c = controller
                if (c != null) {
                    val position = c.currentPosition.coerceAtLeast(0)
                    val duration = c.duration.coerceAtLeast(0)
                    val currentState = _uiState.value

                    // Avoid emitting a new immutable UI state while paused and nothing changed.
                    // This prevents needless recompositions across the whole player UI.
                    if (currentState.position != position || currentState.duration != duration) {
                        _uiState.value = currentState.copy(position = position, duration = duration)
                    }

                    if (AbRepeatLogic.shouldLoopBack(position, _abRepeatPointA.value, _abRepeatPointB.value)) {
                        c.seekTo(_abRepeatPointA.value ?: 0L)
                    }

                    positionTick++
                    if (c.isPlaying && positionTick % 10 == 0) persistPlaybackState()
                }
                delay(500)
            }
        }
    }

    private fun persistPlaybackState() {
        val c = controller ?: return
        val index = c.currentMediaItemIndex
        if (currentQueue.isEmpty() || index !in currentQueue.indices) return

        // Playback state persistence is infrequent disk I/O. Keep it off the main
        // thread so periodic saves cannot cause UI jank while the user scrolls or
        // interacts with the player.
        val songIds = currentQueue.map { it.id }
        val positionMs = c.currentPosition.coerceAtLeast(0)
        val currentSongId = currentQueue.getOrNull(index)?.id
        val speed = _uiState.value.playbackSpeed
        // Batch 108 fix2 (crash log 20260817_111602) — `c.repeatMode`/`c.shuffleModeEnabled`
        // WAJIB dibaca di sini, di main thread, sebelum masuk `launch(Dispatchers.IO)`.
        // MediaController melempar IllegalStateException ("called from a wrong thread") kalau
        // method-nya diakses dari thread mana pun selain thread yang membuatnya (main) — sama
        // seperti `currentPosition`/`currentMediaItemIndex` di atas yang sudah lama benar
        // dibaca di luar coroutine. Repeat/shuffle (ditambah Batch 108) sempat salah taruh di
        // DALAM lambda IO, jadi ikut girang dieksekusi di background thread pool tiap checkpoint
        // ~5 detik — crash nyata pertama app ini yang ketahuan lewat crash logger sejak Batch 22.
        val repeatMode = c.repeatMode
        val shuffleEnabled = c.shuffleModeEnabled
        viewModelScope.launch(Dispatchers.IO) {
            playbackStateStore.save(
                songIds = songIds,
                index = index,
                positionMs = positionMs,
                repeatMode = repeatMode,
                shuffleEnabled = shuffleEnabled
            )
            // Roadmap #12 (Batch 93) — no-ops internally if this song was never opted into
            // audiobook mode, so it's safe to call unconditionally at the same cadence as the
            // save above (~5s while playing, immediately on pause via onIsPlayingChanged).
            if (currentSongId != null) audiobookModeStore.updateProgress(currentSongId, speed, positionMs)
        }
    }

    /** Fires a one-time celebration the exact play that crosses a milestone total —
     * checking equality (not >=) means it can only ever trigger once per threshold,
     * even though this runs on every single track transition. */
    private fun checkListeningMilestone() {
        val total = playStatsStore.totalPlayCount()
        val message = when (total) {
            10 -> "10 lagu sudah kamu putar 🎧"
            50 -> "50 lagu! Selera musik kamu mulai kebaca nih 🎶"
            100 -> "100 lagu diputar — telinga kamu sudah kerja keras hari ini 🔥"
            250 -> "250 lagu total. Ini udah kebiasaan, bukan cuma iseng 😌"
            500 -> "500 lagu sepanjang masa. Serius, respect. 🏆"
            1000 -> "1000 lagu! Kamu ini pendengar sejati 👑"
            else -> null
        }
        if (message != null) _celebrationMessage.value = message
    }

    private fun updateAccentColor(song: Song?) {
        // Tanpa ini, skip cepat (next beruntun, shake-to-skip berkali-kali) bisa memicu
        // beberapa ekstraksi tumpang tindih — kalau yang lebih lama selesai belakangan, warna
        // aksen lagu yang sudah dilewati bisa menimpa warna lagu yang sedang main sekarang.
        accentColorJob?.cancel()
        accentColorJob = viewModelScope.launch {
            val color = withContext(Dispatchers.IO) {
                AccentColorExtractor.extract(appContext, song?.uri)
            }
            _accentColor.value = color
        }
    }

    /** Looks up the last-played song for display, without starting playback. */
    fun peekSavedSong(allSongs: List<Song>): Song? {
        val saved = playbackStateStore.load() ?: return null
        val songMap = allSongs.associateBy { it.id }
        return saved.songIds.getOrNull(saved.index)?.let { songMap[it] }
    }

    /** Rebuilds the last queue from the freshly scanned library and resumes at the saved position. */
    fun resumeFromSaved(allSongs: List<Song>, autoPlay: Boolean = true) {
        val saved = playbackStateStore.load() ?: return
        val songMap = allSongs.associateBy { it.id }
        val resolvedSongs = saved.songIds.mapNotNull { songMap[it] }
        if (resolvedSongs.isEmpty()) return
        val newIndex = saved.index.coerceIn(0, resolvedSongs.size - 1)
        // Gap List #6 (Batch 108) — repeat/shuffle sebelumnya tidak pernah dipulihkan, selalu
        // reset ke off tiap resume walau user terakhir kali mengaktifkannya. Diset SEBELUM
        // playQueue() supaya shuffle order (kalau ada) berlaku sejak media item pertama kali
        // di-set, bukan re-shuffle setelah queue sudah jalan.
        controller?.repeatMode = saved.repeatMode
        controller?.shuffleModeEnabled = saved.shuffleEnabled
        playQueue(resolvedSongs, newIndex, saved.positionMs, autoPlay)
    }

    fun playQueue(songs: List<Song>, startIndex: Int, startPositionMs: Long = 0L, autoPlay: Boolean = true) {
        currentQueue = songs
        currentQueueSlotIds = newSlotIds(songs.size)
        _uiState.value = _uiState.value.copy(
            queue = songs,
            queueSlotIds = currentQueueSlotIds,
            currentSong = songs.getOrNull(startIndex),
            currentIndex = startIndex
        )

        val items = songs.map { song -> mediaItemFor(song) }

        controller?.apply {
            setMediaItems(items, startIndex, startPositionMs)
            prepare()
            if (autoPlay) play()
        }
    }

    /** Jumps straight to a specific position in the current queue and plays it. */
    fun playFromQueueIndex(index: Int) {
        val c = controller ?: return
        if (index !in currentQueue.indices) return
        c.seekTo(index, 0)
        c.play()
    }

    /** Moves an item within the queue (used for drag/up-down reordering in the Queue sheet). */
    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        if (from == to || from !in currentQueue.indices || to !in currentQueue.indices) return
        c.moveMediaItem(from, to)
        currentQueue = currentQueue.toMutableList().apply { add(to, removeAt(from)) }
        currentQueueSlotIds = currentQueueSlotIds.toMutableList().apply { add(to, removeAt(from)) }
        _uiState.value = _uiState.value.copy(
            queue = currentQueue,
            queueSlotIds = currentQueueSlotIds,
            currentIndex = c.currentMediaItemIndex
        )
        persistPlaybackState()
    }

    /** Removes a song from the queue. Keeps at least one item so playback never goes fully empty. */
    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index !in currentQueue.indices || currentQueue.size <= 1) return
        val removedSong = currentQueue[index]
        val removedSlotId = currentQueueSlotIds.getOrNull(index)
        c.removeMediaItem(index)
        currentQueue = currentQueue.toMutableList().apply { removeAt(index) }
        currentQueueSlotIds = currentQueueSlotIds.toMutableList().apply { removeAt(index) }
        val newIndex = c.currentMediaItemIndex
        _uiState.value = _uiState.value.copy(
            queue = currentQueue,
            queueSlotIds = currentQueueSlotIds,
            currentIndex = newIndex,
            currentSong = currentQueue.getOrNull(newIndex)
        )
        persistPlaybackState()
        _undoableAction.value = UndoableAction("\"${removedSong.title}\" dihapus dari antrean") {
            reinsertIntoQueue(removedSong, index, removedSlotId)
        }
    }

    /** The undo half of [removeFromQueue] — puts the song back at (as close as possible to)
     * the index it was removed from, keeping its original slot id so nothing else in the
     * queue appears to shuffle around just because one item came back. */
    private fun reinsertIntoQueue(song: Song, atIndex: Int, slotId: Long?) {
        val c = controller ?: return
        val insertAt = atIndex.coerceIn(0, currentQueue.size)
        c.addMediaItem(insertAt, mediaItemFor(song))
        currentQueue = currentQueue.toMutableList().apply { add(insertAt, song) }
        currentQueueSlotIds = currentQueueSlotIds.toMutableList().apply { add(insertAt, slotId ?: nextQueueSlotId++) }
        _uiState.value = _uiState.value.copy(
            queue = currentQueue,
            queueSlotIds = currentQueueSlotIds,
            currentIndex = c.currentMediaItemIndex
        )
        persistPlaybackState()
    }

    /** Inserts a song to play right after the current track, without disturbing the rest of the queue. */
    fun playNext(song: Song) {
        val c = controller ?: return
        val insertAt = (c.currentMediaItemIndex + 1).coerceAtMost(currentQueue.size)
        val item = mediaItemFor(song)
        c.addMediaItem(insertAt, item)
        currentQueue = currentQueue.toMutableList().apply { add(insertAt, song) }
        currentQueueSlotIds = currentQueueSlotIds.toMutableList().apply { add(insertAt, nextQueueSlotId++) }
        _uiState.value = _uiState.value.copy(queue = currentQueue, queueSlotIds = currentQueueSlotIds, currentIndex = c.currentMediaItemIndex)
        persistPlaybackState()
    }

    /** Appends a song to the end of the queue. */
    fun addToQueue(song: Song) {
        val c = controller ?: return
        c.addMediaItem(mediaItemFor(song))
        currentQueue = currentQueue + song
        currentQueueSlotIds = currentQueueSlotIds + (nextQueueSlotId++)
        _uiState.value = _uiState.value.copy(queue = currentQueue, queueSlotIds = currentQueueSlotIds, currentIndex = c.currentMediaItemIndex)
        persistPlaybackState()
    }

    private fun mediaItemFor(song: Song): MediaItem {
        // Batch 67: song.uri (bukan URI legacy "content://media/external/audio/albumart/$id")
        // — lihat catatan sama di PlaybackService.loadSavedQueueItems().
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.uri)
                    .build()
            )
            .build()
    }

    /** Resolves recently-played song IDs against the freshly scanned library, most recent first. */
    /**
     * The closest honest local equivalent to a streaming service's algorithmic "Artist Mix":
     * finds the artist the user actually listens to most (from real play counts, not a guess)
     * and returns their other songs. Returns null until there's enough listening history.
     */
    /** "Kilas Balik" — looks back at exactly 1 year, 6 months, then 1 month ago (in that
     * priority) for the same calendar date, returning the first match with actual listening
     * history plus a human label. Null until there's at least a month of history behind it. */
    fun getFlashback(allSongs: List<Song>): Pair<String, List<Song>>? {
        val today = java.time.LocalDate.now()
        val candidates = listOf(
            "Setahun lalu hari ini" to today.minusYears(1),
            "6 bulan lalu hari ini" to today.minusMonths(6),
            "Sebulan lalu hari ini" to today.minusMonths(1)
        )
        val songMap = allSongs.associateBy { it.id }
        for ((label, date) in candidates) {
            val ids = listeningHistoryStore.getSongIdsForDate(date)
            val songs = ids.mapNotNull { songMap[it] }
            if (songs.isNotEmpty()) return label to songs
        }
        return null
    }

    fun getTopArtistMix(allSongs: List<Song>): Pair<String, List<Song>>? {
        val mostPlayedIds = playStatsStore.getMostPlayedIds(50)
        if (mostPlayedIds.isEmpty()) return null
        val songMap = allSongs.associateBy { it.id }
        val topArtist = mostPlayedIds.mapNotNull { songMap[it]?.artist }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: return null
        val artistSongs = allSongs.filter { it.artist == topArtist }
        if (artistSongs.size < 3) return null
        return topArtist to artistSongs
    }

    fun getRecentSongs(allSongs: List<Song>, limit: Int = 15): List<Song> {
        val songMap = allSongs.associateBy { it.id }
        return playStatsStore.getRecentIds(limit).mapNotNull { songMap[it] }
    }

    /** Resolves most-played song IDs against the freshly scanned library, highest count first. */
    fun getMostPlayedSongs(allSongs: List<Song>, limit: Int = 15): List<Song> {
        val songMap = allSongs.associateBy { it.id }
        return playStatsStore.getMostPlayedIds(limit).mapNotNull { songMap[it] }
    }

    /** Assembles the Stats Dashboard snapshot (Batch 90) — thin wrapper collecting raw data
     * from PlayStatsStore/ListeningHistoryStore/HourlyListenStore and delegating all actual
     * math to ListeningStatsEngine (Context-free, unit-tested). */
    fun getListeningStats(allSongs: List<Song>): ListeningStatsEngine.Snapshot =
        ListeningStatsEngine.buildSnapshot(
            songs = allSongs,
            counts = playStatsStore.getAllCounts(),
            totalPlays = playStatsStore.totalPlayCount(),
            rawDailyCounts = listeningHistoryStore.getCountsForLastDays(7),
            hourlyCounts = hourlyListenStore.getHourlyCounts()
        )

    fun createPlaylist(name: String): Playlist {
        val playlist = playlistStore.createPlaylist(name.trim().ifBlank { "Playlist Baru" })
        _playlists.value = playlistStore.getPlaylists()
        return playlist
    }

    /** Batch 101 — sebelumnya hapus permanen 1 tap tanpa jalan balik sama sekali (gap UX nyata:
     *   1 playlist berisi puluhan lagu bisa hilang cuma krn salah tap). Sekarang ikut pola
     *  [UndoableAction] yang sama dgn [removeFromQueue]/[removeSongFromPlaylist] — snapshot
     *  [Playlist] penuh diambil SEBELUM dihapus supaya undo mengembalikan persis (nama + urutan
     *  lagu), bukan bikin playlist kosong baru. */
    fun deletePlaylist(id: String) {
        val removed = playlistStore.getPlaylists().find { it.id == id } ?: return
        playlistStore.deletePlaylist(id)
        _playlists.value = playlistStore.getPlaylists()
        _undoableAction.value = UndoableAction("Playlist \"${removed.name}\" dihapus") {
            playlistStore.restorePlaylist(removed)
            _playlists.value = playlistStore.getPlaylists()
        }
    }

    fun renamePlaylist(id: String, newName: String) {
        if (newName.isBlank()) return
        playlistStore.renamePlaylist(id, newName.trim())
        _playlists.value = playlistStore.getPlaylists()
    }

    /** Returns true if the song was newly added (false if it was already in that playlist). */
    fun addSongToPlaylist(playlistId: String, songId: Long): Boolean {
        val added = playlistStore.addSong(playlistId, songId)
        _playlists.value = playlistStore.getPlaylists()
        return added
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        val originalIndex = playlistStore.getPlaylists().find { it.id == playlistId }?.songIds?.indexOf(songId) ?: -1
        playlistStore.removeSong(playlistId, songId)
        _playlists.value = playlistStore.getPlaylists()
        if (originalIndex >= 0) {
            val title = _librarySongs.value.find { it.id == songId }?.title ?: "Lagu"
            _undoableAction.value = UndoableAction("\"$title\" dihapus dari playlist") {
                playlistStore.addSong(playlistId, songId)
                val lastIndex = playlistStore.getPlaylists().find { it.id == playlistId }?.songIds?.lastIndex ?: -1
                if (lastIndex > 0 && originalIndex < lastIndex) {
                    playlistStore.moveSong(playlistId, lastIndex, originalIndex)
                }
                _playlists.value = playlistStore.getPlaylists()
            }
        }
    }

    fun moveSongInPlaylist(playlistId: String, from: Int, to: Int) {
        playlistStore.moveSong(playlistId, from, to)
        _playlists.value = playlistStore.getPlaylists()
    }

    /** [playlist.id] from the builder UI is a throwaway draft value — [SmartPlaylistStore]
     *  replaces it with a real UUID, so the returned value (not the argument) is the one to use. */
    fun createSmartPlaylist(playlist: SmartPlaylist): SmartPlaylist {
        val created = smartPlaylistStore.createSmartPlaylist(playlist)
        _smartPlaylists.value = smartPlaylistStore.getSmartPlaylists()
        return created
    }

    fun updateSmartPlaylist(playlist: SmartPlaylist) {
        smartPlaylistStore.updateSmartPlaylist(playlist)
        _smartPlaylists.value = smartPlaylistStore.getSmartPlaylists()
    }

    /** Batch 101 — sama alasannya dgn [deletePlaylist]: hapus permanen 1 tap tanpa undo adalah
     *  gap UX, apalagi smart playlist bisa berisi kriteria yang disusun manual (folder/durasi/
     *  rating/tahun/kata kunci) yang merepotkan diketik ulang kalau ke-tap hapus tidak sengaja. */
    fun deleteSmartPlaylist(id: String) {
        val removed = smartPlaylistStore.getSmartPlaylists().find { it.id == id } ?: return
        smartPlaylistStore.deleteSmartPlaylist(id)
        _smartPlaylists.value = smartPlaylistStore.getSmartPlaylists()
        _undoableAction.value = UndoableAction("Playlist otomatis \"${removed.name}\" dihapus") {
            smartPlaylistStore.restoreSmartPlaylist(removed)
            _smartPlaylists.value = smartPlaylistStore.getSmartPlaylists()
        }
    }

    fun getLyrics(songId: Long): String? = lyricsStore.getLyrics(songId)

    fun saveLyrics(songId: Long, text: String) = lyricsStore.setLyrics(songId, text)

    fun deleteLyrics(songId: Long) = lyricsStore.deleteLyrics(songId)

    // --- A-B Repeat (Roadmap #4, Batch 91) ---

    fun setAbRepeatPointA(positionMs: Long) {
        _abRepeatPointA.value = positionMs
        // Setting a new A after B was already placed behind it would leave A-B repeat
        // silently inactive (AbRepeatLogic.isActive requires B strictly after A) with no
        // feedback why — clearing the stale B forces the user to re-mark B deliberately.
        val currentB = _abRepeatPointB.value
        if (currentB != null && currentB <= positionMs) _abRepeatPointB.value = null
    }

    fun setAbRepeatPointB(positionMs: Long) {
        _abRepeatPointB.value = positionMs
    }

    fun clearAbRepeat() {
        _abRepeatPointA.value = null
        _abRepeatPointB.value = null
    }

    // --- Mode Audiobook/Podcast (Roadmap #12, Batch 93) ---

    /** Toggled from the "Pengaturan Putar" (speed) dialog, scoped to whatever song is currently
     * loaded. Seeds the saved speed with what's already playing at the moment of opting in (see
     * [AudiobookModeStore.setEnabled]), then immediately persists the current position too —
     * without this second call, a fresh toggle-on would sit with `lastPositionMs = 0` until the
     * next periodic tick (~5s), which is a needless window to lose if the app is killed right
     * after enabling. */
    fun setAudiobookModeEnabled(enabled: Boolean) {
        val songId = _uiState.value.currentSong?.id ?: return
        val speed = _uiState.value.playbackSpeed
        audiobookModeStore.setEnabled(songId, enabled, speed)
        if (enabled) audiobookModeStore.updateProgress(songId, speed, _uiState.value.position)
        _audiobookModeEnabled.value = enabled
    }

    // --- Bookmark Posisi (Roadmap #4, Batch 91) ---

    fun getBookmarks(songId: Long): List<Bookmark> = bookmarkStore.getBookmarks(songId)

    fun addBookmark(songId: Long, label: String, positionMs: Long): Bookmark =
        bookmarkStore.addBookmark(songId, label, positionMs)

    fun deleteBookmark(songId: Long, bookmarkId: String) =
        bookmarkStore.deleteBookmark(songId, bookmarkId)

    /** Attaches the equalizer to the current playback session. Call when the Equalizer sheet opens.
     * Safe to call repeatedly. */
    fun ensureEqualizerAttached() {
        equalizerController.attach(PlaybackAudioSession.sessionId)
    }

    fun setEqualizerEnabled(enabled: Boolean) = equalizerController.setEnabled(enabled)

    fun setEqualizerBand(band: Int, level: Short) = equalizerController.setBandLevel(band, level)

    fun useEqualizerPreset(presetIndex: Int) = equalizerController.usePreset(presetIndex)

    fun useBoldEqualizerPreset(preset: EqualizerController.BoldPreset) = equalizerController.useBoldPreset(preset)

    // --- Visualizer Audio (Roadmap #9, Batch 92) ---

    /** Call when the Visualizer sheet opens. Unlike [ensureEqualizerAttached] (unconditional —
     * the equalizer must keep affecting real audio in the background regardless of whether its
     * sheet is open), this is gated on the user's own on/off preference: attaching starts an
     * active OS-level capture, and there's no reason to spend that battery/CPU unless the user
     * has actually opted in. Does NOT request RECORD_AUDIO itself — see AudioVisualizerController's
     * doc comment for why that's deliberately the UI layer's job (MainActivity), not this one's. */
    fun ensureVisualizerAttached() {
        if (_visualizerEnabled.value) audioVisualizerController.attach(PlaybackAudioSession.sessionId)
    }

    fun setVisualizerEnabled(enabled: Boolean) {
        visualizerSettingsStore.setEnabled(enabled)
        _visualizerEnabled.value = enabled
        if (enabled) {
            audioVisualizerController.attach(PlaybackAudioSession.sessionId)
        } else {
            audioVisualizerController.release()
        }
    }

    /** Called when the Visualizer sheet closes. Stops the OS-level capture but deliberately
     * leaves the persisted on/off preference untouched, so it silently reattaches (via
     * [ensureVisualizerAttached]) next time the sheet opens — the user shouldn't need to flip the
     * switch again just because they navigated away and back. */
    fun stopVisualizerCapture() {
        audioVisualizerController.release()
    }

    // Batch 102 (Gap List #1, True Crossfade) — startFadeOut()/startFadeIn()/animateVolume()
    // yang dulu ada di sini (single-player volume envelope) sudah dihapus total. True crossfade
    // sekarang dikerjakan CrossfadeEngine di PlaybackService lewat overlap dua ExoPlayer
    // sungguhan — ViewModel ini cuma relay toggle on/off-nya lewat custom SessionCommand
    // (ACTION_SET_CROSSFADE_ENABLED), pola identik setSilenceSkipEnabled() di atas, karena
    // ExoPlayer mentah/CrossfadeEngine tidak diekspos lewat MediaController.
    fun setCrossfadeEnabled(enabled: Boolean) {
        crossfadeStore.setEnabled(enabled)
        _crossfadeEnabled.value = enabled
        val args = Bundle().apply { putBoolean(PlaybackService.EXTRA_CROSSFADE_ENABLED, enabled) }
        controller?.sendCustomCommand(SessionCommand(PlaybackService.ACTION_SET_CROSSFADE_ENABLED, Bundle.EMPTY), args)
        // controller == null: sama seperti setSilenceSkipEnabled(), tidak fatal — PlaybackService
        // baca CrossfadeStore langsung di onCreate-nya sendiri begitu benar-benar start.
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() = controller?.seekToNextMediaItem() ?: Unit
    fun previous() = controller?.seekToPreviousMediaItem() ?: Unit
    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs) ?: Unit

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        controller?.let { c ->
            c.repeatMode = when (c.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
    }

    fun setVolume(volume: Float) {
        userTargetVolume = volume.coerceIn(0f, 1f)
        controller?.setVolume(userTargetVolume)
    }

    /** Shuffles the whole library and starts playing immediately — one tap from Home. */
    fun shuffleAll(allSongs: List<Song>) {
        if (allSongs.isEmpty()) return
        val shuffled = allSongs.shuffled()
        playQueue(shuffled, 0)
        controller?.shuffleModeEnabled = true
    }

    fun toggleFavorite(songId: Long) {
        favoritesStore.toggleFavorite(songId)
        _favoriteIds.value = loadFavoriteIds()
    }

    private fun loadFavoriteIds(): ImmutableSet<Long> =
        favoritesStore.getFavorites().mapNotNull { it.toLongOrNull() }.toPersistentSet()

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val endAt = System.currentTimeMillis() + minutes * 60_000L
        _sleepTimerRemaining.value = minutes * 60_000L
        // Gap List #7 (Batch 109) — eksekusi NYATA (pause sungguhan saat waktu habis) sekarang
        // dijadwalkan di PlaybackService lewat custom SessionCommand (survive kalau ViewModel
        // ini di-clear/proses mati), bukan cuma coroutine ViewModel seperti sebelumnya. Loop di
        // bawah ini SEKARANG murni kosmetik — cuma menghitung mundur angka yang ditampilkan UI;
        // kalau loop ini hilang (ViewModel di-clear), timer TETAP akan berbunyi tepat waktu dari
        // sisi Service, cuma UI tidak lagi menampilkan angka mundurnya sampai ViewModel baru
        // dibuat ulang dan query ulang ke store.
        val args = Bundle().apply { putLong(PlaybackService.EXTRA_SLEEP_TIMER_END_AT, endAt) }
        controller?.sendCustomCommand(SessionCommand(PlaybackService.ACTION_SET_SLEEP_TIMER, Bundle.EMPTY), args)
        // controller == null di sini BUKAN kasus aman-diabaikan seperti toggle lain
        // (setSilenceSkipEnabled dkk membaca ulang store-nya sendiri di Service.onCreate) —
        // sleep timer adalah aksi sekali-jalan, bukan setting persisten yang di-load ulang.
        // Tapi secara praktis sleep timer cuma bisa dipicu dari UI Now Playing yang mensyaratkan
        // playback sudah berjalan, jadi MediaController.connect() sudah pasti selesai di titik
        // ini.
        sleepTimerJob = viewModelScope.launch {
            while (true) {
                // Dihitung ulang dari endAt - now() tiap tick (bukan sekadar decrement lokal)
                // supaya angka yang ditampilkan tidak drift dari deadline sungguhan yang
                // dipegang Service, walau app sempat di-throttle di background.
                val remaining = endAt - System.currentTimeMillis()
                if (remaining <= 0) break
                _sleepTimerRemaining.value = remaining
                delay(1000)
            }
            _sleepTimerRemaining.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemaining.value = null
        // -1L = sentinel batal (Bundle tidak bisa bawa Long? null) — lihat
        // PlaybackService.onCustomCommand(ACTION_SET_SLEEP_TIMER).
        val args = Bundle().apply { putLong(PlaybackService.EXTRA_SLEEP_TIMER_END_AT, -1L) }
        controller?.sendCustomCommand(SessionCommand(PlaybackService.ACTION_SET_SLEEP_TIMER, Bundle.EMPTY), args)
    }

    override fun onCleared() {
        sleepTimerJob?.cancel()
        accentColorJob?.cancel()
        libraryRefreshJob?.cancel()
        libraryAutoRefreshJob?.cancel()
        libraryContentObserver?.let { runCatching { appContext.contentResolver.unregisterContentObserver(it) } }
        equalizerController.release()
        audioVisualizerController.release()
        // Batch 78 — MediaController.releaseFuture() handles BOTH cases correctly: cancels the
        // future if the async connect() handshake hasn't resolved yet, or releases the resolved
        // controller if it has. `controller?.release()` alone only covered the second case.
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }

}
