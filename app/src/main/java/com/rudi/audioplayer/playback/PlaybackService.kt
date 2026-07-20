package com.rudi.audioplayer.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.rudi.audioplayer.MainActivity
import com.rudi.audioplayer.data.MusicRepository
import com.rudi.audioplayer.data.PlaybackStateStore
import com.rudi.audioplayer.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            // true = ExoPlayer requests/abandons audio focus automatically, ducking or
            // pausing when a call, notification sound, or another app needs the output.
            .setAudioAttributes(audioAttributes, true)
            // Auto-pauses when headphones are unplugged or a Bluetooth device disconnects,
            // instead of blasting through the speaker unannounced — table stakes in every
            // major music app.
            .setHandleAudioBecomingNoisy(true)
            .build()

        // ExoPlayer assigns its own audio session ID lazily (once the AudioTrack is created).
        // PlayerViewModel talks to playback only through a MediaController, which doesn't expose
        // this ExoPlayer-specific property — so we mirror it into a same-process singleton every
        // time the player reports an event, and the Equalizer reads it from there. onEvents is
        // part of the stable, common Player.Listener API, so this stays robust across versions.
        // The same listener also keeps the home-screen widget in sync with the current track.
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                val id = (player as? ExoPlayer)?.audioSessionId ?: return
                if (id != 0) PlaybackAudioSession.sessionId = id
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                pushWidgetUpdate(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                pushWidgetUpdate(player)
            }
        })

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    private fun pushWidgetUpdate(player: Player) {
        val metadata = player.currentMediaItem?.mediaMetadata
        WidgetUpdater.saveState(
            context = this,
            title = metadata?.title?.toString(),
            artist = metadata?.artist?.toString(),
            artworkUri = metadata?.artworkUri,
            isPlaying = player.isPlaying
        )
        WidgetUpdater.updateAll(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val isWidgetAction = action == WidgetUpdater.ACTION_TOGGLE_PLAY ||
            action == WidgetUpdater.ACTION_NEXT ||
            action == WidgetUpdater.ACTION_PREVIOUS

        if (isWidgetAction) {
            val player = mediaSession?.player
            if (player != null && player.mediaItemCount == 0) {
                // Cold start: the widget was tapped before this session ever loaded a queue
                // (fresh install, or the service was killed). Restore the last saved queue
                // first, then apply the tapped action — otherwise "play" from the widget would
                // silently do nothing since there'd be nothing queued to play.
                serviceScope.launch {
                    restoreLastQueue()
                    applyWidgetAction(action)
                }
            } else {
                applyWidgetAction(action)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun applyWidgetAction(action: String?) {
        val player = mediaSession?.player ?: return
        when (action) {
            WidgetUpdater.ACTION_TOGGLE_PLAY -> if (player.isPlaying) player.pause() else player.play()
            WidgetUpdater.ACTION_NEXT -> player.seekToNextMediaItem()
            WidgetUpdater.ACTION_PREVIOUS -> player.seekToPreviousMediaItem()
        }
    }

    private suspend fun restoreLastQueue() {
        val saved = PlaybackStateStore(this).load() ?: return
        val foundSongs = withContext(Dispatchers.IO) {
            MusicRepository(this@PlaybackService).getSongsByIds(saved.songIds)
        }
        if (foundSongs.isEmpty()) return

        val songMap = foundSongs.associateBy { it.id }
        val orderedSongs = saved.songIds.mapNotNull { songMap[it] }
        if (orderedSongs.isEmpty()) return

        val items = orderedSongs.map { song ->
            val artworkUri = Uri.parse("content://media/external/audio/albumart/${song.albumId}")
            MediaItem.Builder()
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

        val index = saved.index.coerceIn(0, items.size - 1)
        mediaSession?.player?.apply {
            setMediaItems(items, index, saved.positionMs)
            prepare()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
