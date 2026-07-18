package com.rudi.audioplayer.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.rudi.audioplayer.MainActivity

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Generated explicitly (rather than left to lazy ExoPlayer default) so the Equalizer can
        // attach to a known, stable session ID from PlayerViewModel — see PlaybackAudioSession.
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioSessionId = audioManager.generateAudioSessionId()
        PlaybackAudioSession.sessionId = audioSessionId

        val player = ExoPlayer.Builder(this)
            // true = ExoPlayer requests/abandons audio focus automatically, ducking or
            // pausing when a call, notification sound, or another app needs the output.
            .setAudioAttributes(audioAttributes, true)
            // Auto-pauses when headphones are unplugged or a Bluetooth device disconnects,
            // instead of blasting through the speaker unannounced — table stakes in every
            // major music app.
            .setHandleAudioBecomingNoisy(true)
            .setAudioSessionId(audioSessionId)
            .build()

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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

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
