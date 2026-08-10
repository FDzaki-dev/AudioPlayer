package com.rudi.audioplayer.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
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

/**
 * Background playback: owns the [MediaLibrarySession], the ExoPlayer instance, and the
 * cold-start/resumption notification handling.
 *
 * This is the single riskiest file in the project — Android's foreground-service and
 * media-session lifecycle rules are full of version-specific, OEM-specific, and easy-to-guess-
 * wrong behavior. Two real build/behavior bugs already happened here from confident-but-wrong
 * assumptions (see CHANGELOG.md, Batch 10 through 14) — one about `onTaskRemoved` not being
 * sufficient on its own to keep the session alive, one an import path
 * (`MediaLibraryService.MediaLibrarySession` is nested, not top-level) that only surfaced as a
 * CI build failure. Before changing anything about session lifecycle, notifications, or
 * `onTaskRemoved`/`onPlaybackResumption` here: read CHANGELOG.md's Batch 10-14 entries and the
 * "Keputusan Arsitektur" section of README.md first, and verify any Media3 API assumption
 * against https://developer.android.com/media/media3/session/background-playback or the actual
 * androidx/media source on GitHub rather than trusting memory/training data — this project
 * pins media3 1.3.1 specifically (see app/build.gradle.kts), and Media3's session API has
 * changed shape across versions (e.g. `onPlaybackResumption`'s signature).
 */
class PlaybackService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var shakeDetector: ShakeDetector? = null
    private var coldStartNotificationActive = false
    // Batch 34: pushWidgetUpdate fires on every track transition and every play/pause tap —
    // both high-frequency, high-visibility moments. Tracking the job lets a fast skip/toggle
    // cancel a still-decoding older update instead of letting it land after a newer one and
    // show stale art/state.
    private var widgetUpdateJob: Job? = null

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

        mediaSession = MediaLibrarySession.Builder(this, player, PlaybackSessionCallback())
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    private fun pushWidgetUpdate(player: Player) {
        val metadata = player.currentMediaItem?.mediaMetadata
        // saveState is cheap (SharedPreferences.apply() is already async-safe) and stays on
        // the caller's thread; updateAll is the expensive part — it decodes, center-crops, and
        // rounds the album-art bitmap, which used to block this listener's thread (main) on
        // every track change and every play/pause tap. Batch 34: moved to IO, with the previous
        // in-flight update cancelled so a fast skip/toggle can't have an older decode land after
        // a newer one and show stale art.
        WidgetUpdater.saveState(
            context = this,
            title = metadata?.title?.toString(),
            artist = metadata?.artist?.toString(),
            artworkUri = metadata?.artworkUri,
            isPlaying = player.isPlaying
        )
        widgetUpdateJob?.cancel()
        widgetUpdateJob = serviceScope.launch(Dispatchers.IO) {
            WidgetUpdater.updateAll(this@PlaybackService)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

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

    /** What a saved queue resolves to: the actual MediaItems (already matched back against
     * MediaStore, since only song IDs are persisted), which one to start on, and where in it.
     * Shared by the widget cold-start path ([restoreLastQueue]) and playback resumption
     * ([PlaybackSessionCallback.onPlaybackResumption]) so both restore a saved queue exactly
     * the same way — one no longer needs to be kept in sync with the other by hand. */
    private data class SavedQueueItems(val items: List<MediaItem>, val startIndex: Int, val startPositionMs: Long)

    private suspend fun loadSavedQueueItems(): SavedQueueItems? {
        val saved = PlaybackStateStore(this).load() ?: return null
        val foundSongs = withContext(Dispatchers.IO) {
            MusicRepository(this@PlaybackService).getSongsByIds(saved.songIds)
        }
        if (foundSongs.isEmpty()) return null

        val songMap = foundSongs.associateBy { it.id }
        val orderedSongs = saved.songIds.mapNotNull { songMap[it] }
        if (orderedSongs.isEmpty()) return null

        val items = orderedSongs.map { song ->
            // Batch 67: dulu URI legacy "content://media/external/audio/albumart/$albumId" —
            // tabel cache albumart itu sering kosong di Android modern (khususnya API 29+),
            // jadi FileNotFoundException konsisten utk banyak album (lihat log diagnostik,
            // ratusan entri sejak Batch 22). song.uri (URI file audio-nya sendiri, dari
            // MusicRepository) didukung loadThumbnail() secara native — framework fallback ke
            // MediaMetadataRetriever buat ekstrak art embedded, tidak bergantung cache lama itu.
            MediaItem.Builder()
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

        val index = saved.index.coerceIn(0, items.size - 1)
        return SavedQueueItems(items, index, saved.positionMs)
    }

    private suspend fun restoreLastQueue() {
        val saved = loadSavedQueueItems() ?: return
        mediaSession?.player?.apply {
            setMediaItems(saved.items, saved.startIndex, saved.startPositionMs)
            prepare()
        }
    }

    /** Accepts every controller with the same full default access the app has always had
     * (no callback was set before this class existed — MediaLibrarySession.Builder requires
     * one, unlike MediaSession.Builder, so this replicates that same "accept everyone, no
     * restrictions" behavior explicitly rather than relying on an implicit default). The only
     * behavior actually being added is [onPlaybackResumption]. */
    private inner class PlaybackSessionCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                .setAvailableSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS)
                .build()
        }

        // Lets Bluetooth devices and the Android System UI media-resumption feature restart
        // playback with the last saved queue even after this service (and the whole app
        // process) has been killed — the actual mechanism behind the lock-screen media control
        // and status-bar indicator being reachable again without reopening the app.
        @UnstableApi
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return CallbackToFutureAdapter.getFuture { completer ->
                serviceScope.launch {
                    val saved = loadSavedQueueItems()
                    if (saved != null) {
                        completer.set(
                            MediaSession.MediaItemsWithStartPosition(saved.items, saved.startIndex, saved.startPositionMs)
                        )
                    } else {
                        // Nothing to resume (fresh install, or the saved songs are all gone) —
                        // fail the future rather than hand back an empty/fake item, which is what
                        // used to leave a dead, control-less notification stuck on screen.
                        completer.setException(IllegalStateException("Tidak ada antrean tersimpan untuk dilanjutkan"))
                    }
                }
                "onPlaybackResumption" // debug tag only, shown if this future leaks/never completes
            }
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
