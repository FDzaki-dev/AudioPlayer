package com.rudi.audioplayer.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
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
import androidx.media3.session.SessionToken
import com.rudi.audioplayer.util.AppLogger
import com.google.common.util.concurrent.MoreExecutors
import com.rudi.audioplayer.data.CrossfadeStore
import com.rudi.audioplayer.data.CustomFolderInfo
import com.rudi.audioplayer.data.CustomFolderScanner
import com.rudi.audioplayer.data.CustomFolderStore
import com.rudi.audioplayer.data.FavoritesStore
import com.rudi.audioplayer.data.LyricsStore
import com.rudi.audioplayer.data.MusicRepository
import com.rudi.audioplayer.data.PlaybackStateStore
import com.rudi.audioplayer.data.AppLockStore
import com.rudi.audioplayer.data.ListeningHistoryStore
import com.rudi.audioplayer.data.PlayStatsStore
import com.rudi.audioplayer.data.RatingStore
import com.rudi.audioplayer.data.RadioSettingsStore
import com.rudi.audioplayer.data.ShakeSettingsStore
import com.rudi.audioplayer.data.Playlist
import com.rudi.audioplayer.data.PlaylistStore
import com.rudi.audioplayer.data.ThemeStore
import com.rudi.audioplayer.ui.theme.AppTheme
import com.rudi.audioplayer.data.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val shakeSettingsStore = ShakeSettingsStore(appContext)
    private val radioSettingsStore = RadioSettingsStore(appContext)

    private val _shakeToSkipEnabled = MutableStateFlow(shakeSettingsStore.isEnabled())
    val shakeToSkipEnabled: StateFlow<Boolean> = _shakeToSkipEnabled.asStateFlow()

    fun setShakeToSkipEnabled(enabled: Boolean) {
        shakeSettingsStore.setEnabled(enabled)
        _shakeToSkipEnabled.value = enabled
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
    private val equalizerController = EqualizerController(appContext)
    val equalizerState: StateFlow<EqualizerUiState> = equalizerController.state

    private val crossfadeStore = CrossfadeStore(appContext)
    private val customFolderStore = CustomFolderStore(appContext)
    private val themeStore = ThemeStore(appContext)
    private val _appTheme = MutableStateFlow(themeStore.getTheme())
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()
    private val customFolderScanner = CustomFolderScanner(appContext)

    private val _customFolders = MutableStateFlow(loadCustomFolderInfos())
    val customFolders: StateFlow<List<CustomFolderInfo>> = _customFolders.asStateFlow()
    private var userTargetVolume = 1f
    private var fadeJob: Job? = null
    private var fadedOutForIndex = -1
    private var positionTick = 0

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val _statsVersion = MutableStateFlow(0)
    val statsVersion: StateFlow<Int> = _statsVersion.asStateFlow()

    private val _crossfadeEnabled = MutableStateFlow(crossfadeStore.isEnabled())
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _playlists = MutableStateFlow(playlistStore.getPlaylists())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _favoriteIds = MutableStateFlow(loadFavoriteIds())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()
    private var sleepTimerJob: Job? = null

    private val _accentColor = MutableStateFlow<Color?>(null)
    val accentColor: StateFlow<Color?> = _accentColor.asStateFlow()

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
                _statsVersion.value += 1
                checkListeningMilestone()
            }
            updateAccentColor(song)
            _currentRating.value = song?.let { ratingStore.getRating(it.id) } ?: 0
            persistPlaybackState()

            if (_crossfadeEnabled.value) {
                startFadeIn()
            }
            fadedOutForIndex = -1
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
        val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            controller?.addListener(playerListener)
            startPositionLoop()
        }, MoreExecutors.directExecutor())
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
            try {
                val songs = withContext(Dispatchers.IO) {
                    val mediaStoreSongs = musicRepository.getAllSongs()
                    val mediaStoreSignatures = mediaStoreSongs.asSequence()
                        .map(::dedupeSignature)
                        .toHashSet()
                    val customSongs = customFolderStore.getFolderUris().asSequence().flatMap { uriString ->
                        try {
                            val uri = Uri.parse(uriString)
                            customFolderScanner.scan(uri, folderLabelFor(uri)).asSequence()
                        } catch (_: Exception) {
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

    fun setAppTheme(theme: AppTheme) {
        themeStore.setTheme(theme)
        _appTheme.value = theme
    }

    private fun loadCustomFolderInfos(): List<CustomFolderInfo> =
        customFolderStore.getFolderUris().map { uriString ->
            val uri = Uri.parse(uriString)
            CustomFolderInfo(uri = uriString, displayName = folderLabelFor(uri))
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

                    if (_crossfadeEnabled.value && c.isPlaying && duration > 0) {
                        val remaining = duration - position
                        val currentIndex = c.currentMediaItemIndex
                        val hasNext = currentIndex < currentQueue.size - 1 || c.repeatMode != Player.REPEAT_MODE_OFF
                        if (remaining in 1..FADE_DURATION_MS && hasNext && fadedOutForIndex != currentIndex) {
                            fadedOutForIndex = currentIndex
                            startFadeOut()
                        }
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
        viewModelScope.launch(Dispatchers.IO) {
            playbackStateStore.save(
                songIds = songIds,
                index = index,
                positionMs = positionMs
            )
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
        viewModelScope.launch {
            val color = withContext(Dispatchers.IO) {
                AccentColorExtractor.extract(appContext, song?.albumId)
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
        val artworkUri = android.net.Uri.parse("content://media/external/audio/albumart/${song.albumId}")
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(artworkUri)
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

    fun createPlaylist(name: String): Playlist {
        val playlist = playlistStore.createPlaylist(name.trim().ifBlank { "Playlist Baru" })
        _playlists.value = playlistStore.getPlaylists()
        return playlist
    }

    fun deletePlaylist(id: String) {
        playlistStore.deletePlaylist(id)
        _playlists.value = playlistStore.getPlaylists()
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

    fun getLyrics(songId: Long): String? = lyricsStore.getLyrics(songId)

    fun saveLyrics(songId: Long, text: String) = lyricsStore.setLyrics(songId, text)

    fun deleteLyrics(songId: Long) = lyricsStore.deleteLyrics(songId)

    /** Attaches the equalizer to the current playback session. Call when the Equalizer sheet opens.
     * Safe to call repeatedly. */
    fun ensureEqualizerAttached() {
        equalizerController.attach(PlaybackAudioSession.sessionId)
    }

    fun setEqualizerEnabled(enabled: Boolean) = equalizerController.setEnabled(enabled)

    fun setEqualizerBand(band: Int, level: Short) = equalizerController.setBandLevel(band, level)

    fun useEqualizerPreset(presetIndex: Int) = equalizerController.usePreset(presetIndex)

    fun useBoldEqualizerPreset(preset: EqualizerController.BoldPreset) = equalizerController.useBoldPreset(preset)

    /** Ramps volume down toward the end of a track, just before the next one begins. */
    private fun startFadeOut() {
        fadeJob?.cancel()
        fadeJob = viewModelScope.launch {
            animateVolume(from = userTargetVolume, to = userTargetVolume * FADE_FLOOR)
        }
    }

    /** Ramps volume back up at the start of a new track, softening the transition. */
    private fun startFadeIn() {
        fadeJob?.cancel()
        controller?.setVolume(userTargetVolume * FADE_FLOOR)
        fadeJob = viewModelScope.launch {
            animateVolume(from = userTargetVolume * FADE_FLOOR, to = userTargetVolume)
        }
    }

    private suspend fun animateVolume(from: Float, to: Float) {
        val steps = 24
        val stepDelay = FADE_DURATION_MS / steps
        for (i in 0..steps) {
            val fraction = i / steps.toFloat()
            controller?.setVolume((from + (to - from) * fraction).coerceIn(0f, 1f))
            delay(stepDelay)
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        crossfadeStore.setEnabled(enabled)
        _crossfadeEnabled.value = enabled
        if (!enabled) {
            fadeJob?.cancel()
            controller?.setVolume(userTargetVolume)
        }
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
        fadeJob?.cancel()
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

    private fun loadFavoriteIds(): Set<Long> =
        favoritesStore.getFavorites().mapNotNull { it.toLongOrNull() }.toSet()

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        var remaining = minutes * 60_000L
        _sleepTimerRemaining.value = remaining
        sleepTimerJob = viewModelScope.launch {
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _sleepTimerRemaining.value = remaining.coerceAtLeast(0)
            }
            controller?.pause()
            _sleepTimerRemaining.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemaining.value = null
    }

    override fun onCleared() {
        sleepTimerJob?.cancel()
        fadeJob?.cancel()
        libraryRefreshJob?.cancel()
        libraryAutoRefreshJob?.cancel()
        libraryContentObserver?.let { runCatching { appContext.contentResolver.unregisterContentObserver(it) } }
        equalizerController.release()
        controller?.release()
        super.onCleared()
    }

    companion object {
        private const val FADE_DURATION_MS = 3000L
        private const val FADE_FLOOR = 0.15f
    }
}
