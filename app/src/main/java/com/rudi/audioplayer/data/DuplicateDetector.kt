package com.rudi.audioplayer.data

/**
 * Gap List #2 (Duplicate Detection). Pure, stateless grouping over an already-scanned song
 * list — no I/O, no Context, no auto-delete anywhere in this file (gap doc explicit
 * requirement: "Jangan melakukan delete otomatis"). Two DISTINCT groupings, not one, because
 * they answer different questions:
 *
 * - [findLibraryDuplicates]: "which library entries look like the same SONG?" — same identity
 *   key PlayerViewModel.dedupeSignature() already uses to merge MediaStore vs SAF scan results
 *   (title+artist trim/lowercase + duration bucketed to the second). Two entries can share this
 *   signature while being two genuinely different files on disk (e.g. the same track ripped
 *   twice at different quality/source) — that's still a real "duplicate library entry" from the
 *   user's point of view. Kept as a separate copy of the signature logic here rather than
 *   exposing PlayerViewModel's private fun — this file has zero ViewModel/Context dependency by
 *   design (pure list-in, groups-out), which keeps it trivially unit-testable on its own.
 * - [findPhysicalDuplicates]: "which entries are LIKELY THE SAME BYTES on disk?" — grouped by
 *   (fileSize, duration bucketed to the second). This is a heuristic, not a byte-for-byte hash:
 *   hashing full file content for every song during a library scan is exactly the kind of
 *   per-song I/O cost this app has deliberately avoided elsewhere (see Song.kt's codec/bitrate
 *   KDoc, same reasoning genre was skipped for pre-Batch 116). fileSize + duration is already a
 *   very strong collision-resistant pair for real-world audio files, and both are read for free
 *   during scan (zero extra I/O here). Songs with fileSize <= 0 (unknown — shouldn't normally
 *   happen, see Song.kt) are excluded since an unknown size can't support the heuristic.
 *
 * Both functions only RETURN groups (size >= 2) for the caller (UI) to display; neither
 * function deletes, merges, tags, or mutates anything.
 */
object DuplicateDetector {

    enum class Reason { SAME_LIBRARY_ENTRY, SAME_PHYSICAL_FILE }

    data class DuplicateGroup(
        val songs: List<Song>,
        val reason: Reason
    )

    /** Mirrors PlayerViewModel.dedupeSignature() by design — see class KDoc above. */
    private fun librarySignature(song: Song): Triple<String, String, Long> = Triple(
        song.title.trim().lowercase(),
        song.artist.trim().lowercase(),
        song.duration / 1000
    )

    /** Null when fileSize is unknown (<= 0) — such songs are excluded from physical grouping. */
    private fun physicalSignature(song: Song): Pair<Long, Long>? {
        if (song.fileSize <= 0L) return null
        return song.fileSize to (song.duration / 1000)
    }

    fun findLibraryDuplicates(songs: List<Song>): List<DuplicateGroup> =
        songs.groupBy(::librarySignature)
            .values
            .filter { it.size >= 2 }
            .map { DuplicateGroup(it, Reason.SAME_LIBRARY_ENTRY) }
            .sortedByDescending { it.songs.size }

    fun findPhysicalDuplicates(songs: List<Song>): List<DuplicateGroup> =
        songs.asSequence()
            .mapNotNull { song -> physicalSignature(song)?.let { it to song } }
            .groupBy({ it.first }, { it.second })
            .values
            .filter { it.size >= 2 }
            .map { DuplicateGroup(it, Reason.SAME_PHYSICAL_FILE) }
            .sortedByDescending { it.songs.size }
}
