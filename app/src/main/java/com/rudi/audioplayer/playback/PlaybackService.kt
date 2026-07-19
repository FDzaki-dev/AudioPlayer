package com.rudi.audioplayer.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.rudi.audioplayer.MainActivity
import com.rudi.audioplayer.widget.WidgetUpdater

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

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
        when (intent?.action) {
            WidgetUpdater.ACTION_TOGGLE_PLAY -> mediaSession?.player?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
            WidgetUpdater.ACTION_NEXT -> mediaSession?.player?.seekToNextMediaItem()
            WidgetUpdater.ACTION_PREVIOUS -> mediaSession?.player?.seekToPreviousMediaItem()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
