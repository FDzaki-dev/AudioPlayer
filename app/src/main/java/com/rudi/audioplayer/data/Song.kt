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
    val year: Int = 0,
    // --- Gap List #4 (Metadata model diperkuat) ---
    // All added as nullable/0-default so every existing call site (incl. test fixtures)
    // stays source-compatible without edits. Scoped to fields readable from the SAME
    // MediaStore cursor row / SAF MediaMetadataRetriever pass already done during scan — zero
    // extra I/O per song. Fields that would need a SECOND per-file retriever pass during a
    // full library scan (bitrate, sampleRate, channelCount, codec, genre, embedded-artwork
    // presence) are deliberately NOT here yet — same N+1-cost reasoning genre was already
    // skipped for in Batch 89 (`SmartPlaylist`), and genre itself has its own gap list item
    // (#11) reserved for that follow-up work. Candidate approach when tackled: on-demand
    // per-song detail fetch (e.g. an "Info Lagu" sheet), not a bulk-scan cost paid by every
    // song whether the user ever looks at it or not.
    val albumArtist: String? = null,
    val composer: String? = null,
    /** 1-based track number within its disc, null = not present in the file's tags. */
    val trackNumber: Int? = null,
    /** 1-based disc number, null = not present (most files: single-disc, tag omitted). */
    val discNumber: Int? = null,
    /** Raw file size in bytes, 0 = unknown (shouldn't normally happen — both MediaStore's
     *  SIZE column and SAF's DocumentFile.length() are always available). */
    val fileSize: Long = 0L,
    /** MIME type as reported by MediaStore/DocumentFile, e.g. "audio/mpeg", "audio/flac" —
     *  used for the container/format field; not a verified codec (that needs a decoder-level
     *  probe, out of scope here, same reasoning as bitrate/sampleRate above). */
    val mimeType: String? = null
)
