package com.rudi.audioplayer.playback

/**
 * Holds the audio session ID that [PlaybackService] pins onto the ExoPlayer instance at creation
 * time. [PlayerViewModel] only talks to playback through a [androidx.media3.session.MediaController],
 * which exposes the common `Player` API and has no way to read ExoPlayer-specific properties like
 * `audioSessionId`. Sharing the ID this way is safe because the service and the app's ViewModel
 * always run in the same process (no `android:process` isolation is declared for PlaybackService).
 */
object PlaybackAudioSession {
    var sessionId: Int = 0
}
