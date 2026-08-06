package com.rudi.audioplayer.data

/** A user-created playlist: just a name and an ordered list of song IDs, which can span any folder. */
data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long> = emptyList()
)
