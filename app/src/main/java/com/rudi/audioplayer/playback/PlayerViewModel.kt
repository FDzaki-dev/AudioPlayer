package com.rudi.audioplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.rudi.audioplayer.data.CrossfadeStore
import com.rudi.audioplayer.data.FavoritesStore
import com.rudi.audioplayer.data.LyricsStore
import com.rudi.audioplayer.data.MusicRepository
import com.rudi.audioplayer.data.PlaybackStateStore
import com.rudi.audioplayer.data.PlayStatsStore
import com.rudi.audioplayer.data.Playlist
import com.rudi.audioplayer.data.PlaylistStore
import com.rudi.audioplayer.data.Song
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
    val queue: List<Song> = emptyList()
)

class PlayerViewModel(private val appContext: Context) : ViewModel() {

    private var controller: MediaController? = null
    private var currentQueue: List<Song> = emptyList()

    private val favoritesStore = FavoritesStore(appContext)
    private val playbackStateStore = PlaybackStateStore(appContext)
    private val playStatsStore = PlayStatsStore(appContext)
    private val playlistStore = PlaylistStore(appContext)
    private val lyricsStore = LyricsStore(appContext)
    private val crossfadeStore = CrossfadeStore(appContext)
    private var positionTick = 0
    private var userTargetVolume = 1f
    private var fadeJob: Job? = null
    private var fadedOutForIndex = -1

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val _statsVersion = MutableStateFlow(0)
    val statsVersion: StateFlow<Int> = _statsVersion.asStateFlow()

    private val _playlists = MutableStateFlow(playlistStore.getPlaylists())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _favoriteIds = MutableStateFlow(loadFavoriteIds())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()
    private var sleepTimerJob: Job? = null

    private val _accentColor = MutableStateFlow<Color?>(null)
    val accentColor: StateFlow<Color?> = _accentColor.asStateFlow()

    private val _librarySongs = MutableStateFlow<List<Song>>(emptyList())
    val librarySongs: StateFlow<List<Song>> = _librarySongs.asStateFlow()

    private val _libraryLoading = MutableStateFlow(true)
    val libraryLoading: StateFlow<Boolean> = _libraryLoading.asStateFlow()

    private val _crossfadeEnabled = MutableStateFlow(crossfadeStore.isEnabled())
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private var libraryLoadedOnce = false

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
                _statsVersion.value += 1
            }
            updateAccentColor(song)
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
    }

    /**
     * When the queue plays through to the end with repeat off, keep the music going with
     * more songs from the library instead of falling silent — the "radio continues" feel
     * of Spotify/YouTube Music, built from purely local data (no streaming catalog needed).
     */
    private fun continuePlaybackIfQueueEnded() {
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
        _uiState.value = _uiState.value.copy(queue = currentQueue)
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
    }

    /** Scans MediaStore once and caches the result so Home/Library/Playlist don't each scan independently. */
    fun ensureLibraryLoaded() {
        if (libraryLoadedOnce) return
        refreshLibrary()
    }

    /** Forces a fresh MediaStore scan (used by the Library screen's "Pindai Ulang" button). */
    fun refreshLibrary() {
        libraryLoadedOnce = true
        viewModelScope.launch {
            _libraryLoading.value = true
            _librarySongs.value = withContext(Dispatchers.IO) { MusicRepository(appContext).getAllSongs() }
            _libraryLoading.value = false
        }
    }

    private fun startPositionLoop() {
        viewModelScope.launch {
            while (true) {
                controller?.let { c ->
                    val position = c.currentPosition.coerceAtLeast(0)
                    val duration = c.duration.coerceAtLeast(0)
                    _uiState.value = _uiState.value.copy(position = position, duration = duration)

                    if (_crossfadeEnabled.value && c.isPlaying && duration > 0) {
                        val remaining = duration - position
                        val currentIndex = c.currentMediaItemIndex
                        val hasNext = currentIndex < currentQueue.size - 1 || c.repeatMode != Player.REPEAT_MODE_OFF
                        if (remaining in 1..FADE_DURATION_MS && hasNext && fadedOutForIndex != currentIndex) {
                            fadedOutForIndex = currentIndex
                            startFadeOut()
                        }
                    }
                }
                positionTick++
                if (positionTick % 10 == 0) persistPlaybackState()
                delay(500)
            }
        }
    }

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

    private fun persistPlaybackState() {
        val c = controller ?: return
        val index = c.currentMediaItemIndex
        if (currentQueue.isEmpty() || index !in currentQueue.indices) return
        playbackStateStore.save(
            songIds = currentQueue.map { it.id },
            index = index,
            positionMs = c.currentPosition.coerceAtLeast(0)
        )
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
        _uiState.value = _uiState.value.copy(
            queue = songs,
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
        _uiState.value = _uiState.value.copy(
            queue = currentQueue,
            currentIndex = c.currentMediaItemIndex
        )
        persistPlaybackState()
    }

    /** Removes a song from the queue. Keeps at least one item so playback never goes fully empty. */
    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index !in currentQueue.indices || currentQueue.size <= 1) return
        c.removeMediaItem(index)
        currentQueue = currentQueue.toMutableList().apply { removeAt(index) }
        val newIndex = c.currentMediaItemIndex
        _uiState.value = _uiState.value.copy(
            queue = currentQueue,
            currentIndex = newIndex,
            currentSong = currentQueue.getOrNull(newIndex)
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
        _uiState.value = _uiState.value.copy(queue = currentQueue, currentIndex = c.currentMediaItemIndex)
        persistPlaybackState()
    }

    /** Appends a song to the end of the queue. */
    fun addToQueue(song: Song) {
        val c = controller ?: return
        c.addMediaItem(mediaItemFor(song))
        currentQueue = currentQueue + song
        _uiState.value = _uiState.value.copy(queue = currentQueue, currentIndex = c.currentMediaItemIndex)
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
        playlistStore.removeSong(playlistId, songId)
        _playlists.value = playlistStore.getPlaylists()
    }

    fun moveSongInPlaylist(playlistId: String, from: Int, to: Int) {
        playlistStore.moveSong(playlistId, from, to)
        _playlists.value = playlistStore.getPlaylists()
    }

    fun getLyrics(songId: Long): String? = lyricsStore.getLyrics(songId)

    fun saveLyrics(songId: Long, text: String) = lyricsStore.setLyrics(songId, text)

    fun deleteLyrics(songId: Long) = lyricsStore.deleteLyrics(songId)

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
        controller?.release()
        super.onCleared()
    }

    companion object {
        private const val FADE_DURATION_MS = 3000L
        private const val FADE_FLOOR = 0.15f
    }
}
