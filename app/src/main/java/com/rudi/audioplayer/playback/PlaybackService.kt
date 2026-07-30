package com.rudi.audioplayer.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.rudi.audioplayer.MainActivity
import com.rudi.audioplayer.R
import com.rudi.audioplayer.data.MusicRepository
import com.rudi.audioplayer.data.PlaybackStateStore
import com.rudi.audioplayer.data.ShakeSettingsStore
import com.rudi.audioplayer.util.AppLogger
import com.rudi.audioplayer.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var shakeDetector: ShakeDetector? = null
    private var coldStartNotificationActive = false

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
                if (isPlaying && ShakeSettingsStore(this@PlaybackService).isEnabled()) {
                    shakeDetector?.start()
                } else {
                    shakeDetector?.stop()
                }
                // The cold-start placeholder is a plain NotificationCompat notification, not
                // a MediaStyle one Media3 keeps in sync automatically — without this, its
                // action button was permanently stuck on whatever label it had at the instant
                // it was first built (always "Lanjutkan", since nothing was playing yet at
                // that exact moment), even after playback actually started.
                if (coldStartNotificationActive) {
                    updateColdStartNotification(isPlaying)
                }
            }
        })

        shakeDetector = ShakeDetector(this) { mediaSession?.player?.seekToNextMediaItem() }

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
                // (fresh install, or the service was killed). Promote to a foreground service
                // IMMEDIATELY, before any suspending work — some OEM skins (XOS/MIUI-style
                // aggressive background killers) will kill a freshly-spawned process within
                // moments if it isn't already flagged as foreground, and the MediaStore query
                // + queue restore below can easily take longer than that window.
                startForegroundColdStartNotification()
                coldStartNotificationActive = true
                serviceScope.launch {
                    try {
                        restoreLastQueue()
                        applyWidgetAction(action)
                        // Only give up our placeholder once playback is CONFIRMED actually
                        // running — not after a blind fixed delay. The old fixed 1s timer could
                        // fire before the MediaStore query + queue restore above even finished
                        // (easily >1s on a slower device or large library), cancelling the only
                        // visible notification before Media3's own real one had anything to show
                        // yet — leaving no pause control anywhere until the app was reopened.
                        val player = mediaSession?.player
                        var waited = 0L
                        while (player?.isPlaying != true && waited < MAX_HANDOFF_WAIT_MS) {
                            delay(150)
                            waited += 150
                        }
                    } catch (e: Exception) {
                        // Whatever failed here (MediaStore permission revoked, a saved queue
                        // pointing at songs since deleted, etc.) must never leave the placeholder
                        // stuck — an ongoing "Memuat lagu..." notification with no working
                        // controls, unable to be swiped away, forever, was exactly this bug.
                        AppLogger.e("PlaybackService", "Cold-start handoff gagal", e)
                    } finally {
                        coldStartNotificationActive = false
                        NotificationManagerCompat.from(this@PlaybackService).cancel(COLD_START_NOTIFICATION_ID)
                    }
                }
            } else {
                applyWidgetAction(action)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun buildColdStartNotification(isPlaying: Boolean): android.app.Notification {
        val toggleIntent = Intent(this, PlaybackService::class.java).setAction(WidgetUpdater.ACTION_TOGGLE_PLAY)
        val toggleFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val togglePendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, 101, toggleIntent, toggleFlags)
        } else {
            PendingIntent.getService(this, 101, toggleIntent, toggleFlags)
        }

        return NotificationCompat.Builder(this, COLD_START_CHANNEL_ID)
            .setContentTitle("AudioPlayer")
            .setContentText("Memuat lagu…")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                if (isPlaying) "Jeda" else "Lanjutkan",
                togglePendingIntent
            )
            .build()
    }

    /** Re-posts the placeholder with an action label matching the player's actual current
     * state. Only meaningful while [coldStartNotificationActive] — never called after Media3's
     * own notification has taken over. */
    private fun updateColdStartNotification(isPlaying: Boolean) {
        NotificationManagerCompat.from(this).notify(COLD_START_NOTIFICATION_ID, buildColdStartNotification(isPlaying))
    }

    /** Bare-minimum "waking up" notification so the OS treats this process as a legitimate
     * foreground service from the very first instant of a widget-triggered cold start. Still
     * carries a real Jeda/Lanjutkan action — even in this brief handoff window before Media3's
     * own full notification takes over, the user has something to tap instead of a dead,
     * control-less notification. */
    private fun startForegroundColdStartNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(COLD_START_CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        COLD_START_CHANNEL_ID,
                        "Memulai Pemutaran",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }

        val notification = buildColdStartNotification(isPlaying = mediaSession?.player?.isPlaying == true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(COLD_START_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(COLD_START_NOTIFICATION_ID, notification)
        }
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
        // Swiping the app away from Recents must not silently kill a session that still has
        // something loaded — the whole point of a media notification and the lock-screen
        // media control is that they keep working after the app itself is gone from Recents.
        // Only tear down when there's genuinely nothing left to control: no player, or an
        // empty queue. A *paused* session with real songs in it stays exactly as visible and
        // controllable from the notification/lock screen as it was right before the swipe.
        if (player == null || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        shakeDetector?.stop()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        private const val COLD_START_CHANNEL_ID = "playback_cold_start"
        private const val COLD_START_NOTIFICATION_ID = 7001
        // Safety net only — normal handoff to Media3's own notification happens much sooner,
        // as soon as isPlaying flips true. This just prevents the placeholder from lingering
        // forever in the rare case playback never actually starts (e.g. every saved song was
        // deleted from disk since it was last played).
        private const val MAX_HANDOFF_WAIT_MS = 8000L
    }
}
