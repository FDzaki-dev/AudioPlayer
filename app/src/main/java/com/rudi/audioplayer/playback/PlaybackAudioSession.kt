package com.rudi.audioplayer.playback

/**
 * Holds the audio session ID that [PlaybackService] pins onto the ExoPlayer instance at creation
 * time. [PlayerViewModel] only talks to playback through a [androidx.media3.session.MediaController],
 * which exposes the common `Player` API and has no way to read ExoPlayer-specific properties like
 * `audioSessionId`. Sharing the ID this way is safe because the service and the app's ViewModel
 * always run in the same process (no `android:process` isolation is declared for PlaybackService).
 *
 * Batch 314 — [onSessionIdChanged] lets [PlayerViewModel] re-attach the persisted equalizer to
 * every NEW session automatically (app cold-start, service restart, ExoPlayer recreating its
 * AudioTrack mid-playback), instead of only when the user manually opens the Equalizer sheet.
 * The setter here is the single point where a valid new ID first becomes known, so this stays a
 * 1-file hook — [PlaybackService]'s existing `onEvents` listener is untouched.
 */
object PlaybackAudioSession {
    var onSessionIdChanged: ((Int) -> Unit)? = null

    var sessionId: Int = 0
        set(value) {
            field = value
            if (value != 0) onSessionIdChanged?.invoke(value)
        }
}
