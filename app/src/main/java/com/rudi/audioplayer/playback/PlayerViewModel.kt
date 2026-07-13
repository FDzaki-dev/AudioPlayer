package com.rudi.audioplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.rudi.audioplayer.data.Song
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
    val queue: List<Song> = emptyList()
)

class PlayerViewModel(private val appContext: Context) : ViewModel() {

    private var controller: MediaController? = null
    private var currentQueue: List<Song> = emptyList()

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = controller?.currentMediaItemIndex ?: 0
            val song = currentQueue.getOrNull(index)
            _uiState.value = _uiState.value.copy(
                currentSong = song,
                duration = controller?.duration?.coerceAtLeast(0) ?: 0L
            )
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.value = _uiState.value.copy(shuffleEnabled = shuffleModeEnabled)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.value = _uiState.value.copy(repeatMode = repeatMode)
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
                delay(500)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        currentQueue = songs
        _uiState.value = _uiState.value.copy(queue = songs)

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
            setMediaItems(items, startIndex, 0L)
            prepare()
            play()
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

    override fun onCleared() {
        controller?.release()
        super.onCleared()
    }
}
