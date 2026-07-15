package com.rudi.audioplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.rudi.audioplayer.data.FavoritesStore
import com.rudi.audioplayer.data.PlaybackStateStore
import com.rudi.audioplayer.data.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1f,
    val queue: List<Song> = emptyList()
)

class PlayerViewModel(private val appContext: Context) : ViewModel() {

    private var controller: MediaController? = null
    private var currentQueue: List<Song> = emptyList()

    private val favoritesStore = FavoritesStore(appContext)
    private val playbackStateStore = PlaybackStateStore(appContext)
    private var positionTick = 0

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val _favoriteIds = MutableStateFlow(loadFavoriteIds())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()
    private var sleepTimerJob: Job? = null

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
                duration = controller?.duration?.coerceAtLeast(0) ?: 0L
            )
            persistPlaybackState()
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
    }

    fun connect() {
        val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            controller?.addListener(playerListener)
            startPositionLoop()
        }, MoreExecutors.directExecutor())
    }

    private fun startPositionLoop() {
        viewModelScope.launch {
            while (true) {
                controller?.let { c ->
                    _uiState.value = _uiState.value.copy(
                        position = c.currentPosition.coerceAtLeast(0),
                        duration = c.duration.coerceAtLeast(0)
                    )
                }
                positionTick++
                if (positionTick % 10 == 0) persistPlaybackState()
                delay(500)
            }
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
        _uiState.value = _uiState.value.copy(queue = songs, currentSong = songs.getOrNull(startIndex))

        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }

        controller?.apply {
            setMediaItems(items, startIndex, startPositionMs)
            prepare()
            if (autoPlay) play()
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
        controller?.release()
        super.onCleared()
    }
}
