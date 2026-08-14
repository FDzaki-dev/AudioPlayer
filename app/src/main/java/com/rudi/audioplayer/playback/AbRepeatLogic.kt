package com.rudi.audioplayer.playback

/**
 * Pure A-B repeat boundary logic — deliberately separated from [PlayerViewModel] (same pattern
 * as `ListeningStatsEngine`/`SmartPlaylistEngine`: Context-free so it can be unit-tested without
 * Robolectric). Consulted every position tick (~500ms, see `PlayerViewModel.startPositionLoop()`).
 */
object AbRepeatLogic {

    /**
     * Whether A-B repeat is currently configured: both points set AND B strictly after A.
     * A null point, or B <= A (user taps "Set B" before "Set A", or marks the same instant
     * twice), is treated as "not active" rather than crashing or looping on a single instant.
     */
    fun isActive(pointAMs: Long?, pointBMs: Long?): Boolean =
        pointAMs != null && pointBMs != null && pointBMs > pointAMs

    /**
     * Returns true when playback has reached/passed [pointB] and should jump back to [pointA].
     * Caller is expected to only act on `true` (seek to [pointAMs]) — this function never
     * returns the destination itself, keeping it a pure boolean check matching every other call
     * site's existing "if (...) controller?.seekTo(...)" shape in the position loop.
     */
    fun shouldLoopBack(positionMs: Long, pointAMs: Long?, pointBMs: Long?): Boolean {
        // Re-checked inline (not delegated to isActive()) so pointBMs smart-casts to non-null
        // Long below without an unsafe cast — Kotlin can't carry null-safety across a separate
        // function call without a contract, but it can across this single `if` condition.
        if (pointAMs == null || pointBMs == null || pointBMs <= pointAMs) return false
        return positionMs >= pointBMs
    }
}
