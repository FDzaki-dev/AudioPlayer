package com.rudi.audioplayer.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val dateAdded: Long,
    val uri: Uri,
    val folderName: String,
    val folderPath: String,
    /** Release year from embedded metadata, 0 = unknown. Default keeps every existing call
     *  site (test fixtures included) source-compatible without needing to pass it. */
    val year: Int = 0
)
