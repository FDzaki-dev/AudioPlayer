package com.rudi.audioplayer.data

/**
 * A user-marked position within one specific song (intro/reff/solo/etc.) for quick jump —
 * distinct from [PlaybackStateStore], which only remembers a single last-playback position for
 * the whole queue, not multiple named points inside one song. Part of Roadmap item #4
 * (`ROADMAP_15_FITUR_OFFLINE.md`), Batch 91.
 */
data class Bookmark(
    val id: String,
    val label: String,
    val positionMs: Long
)
