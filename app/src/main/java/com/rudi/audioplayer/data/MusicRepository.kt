package com.rudi.audioplayer.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.io.File

/**
 * Scans the device's MediaStore for every audio file the app has permission
 * to see. This automatically covers all mainstream codecs the platform's
 * media framework can index (MP3, AAC/M4A, FLAC, WAV, OGG/Vorbis, OPUS, AMR)
 * because MediaStore only lists files the system extractor already recognizes.
 */
class MusicRepository(private val context: Context) {

    fun getAllSongs(): List<Song> = querySongs(selection = BASE_SELECTION, selectionArgs = null)

    /** Targeted lookup for just a few IDs — used to restore a saved queue without a full library scan. */
    fun getSongsByIds(ids: List<Long>): List<Song> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val selection = "$BASE_SELECTION AND ${MediaStore.Audio.Media._ID} IN ($placeholders)"
        val selectionArgs = ids.map { it.toString() }.toTypedArray()
        return querySongs(selection, selectionArgs)
    }

    private fun querySongs(selection: String, selectionArgs: Array<String>?): List<Song> {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val folderColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.RELATIVE_PATH
        } else {
            MediaStore.Audio.Media.DATA
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.YEAR,
            folderColumn,
            // Gap List #4 — all four already sit in the same row as everything above, so
            // this stays a single query pass per scan (no N+1).
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        ) + trackDiscColumns()

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val folderCol = cursor.getColumnIndexOrThrow(folderColumn)
            val albumArtistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val composerCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val useModernTrackColumns = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            val trackCol = cursor.getColumnIndex(if (useModernTrackColumns) CD_TRACK_NUMBER_COLUMN else MediaStore.Audio.Media.TRACK)
            val discCol = if (useModernTrackColumns) cursor.getColumnIndex(DISC_NUMBER_COLUMN) else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Tanpa Judul"
                val artist = cursor.getString(artistCol) ?: "Tidak Diketahui"
                val album = cursor.getString(albumCol) ?: ""
                val albumId = cursor.getLong(albumIdCol)
                val duration = cursor.getLong(durationCol)
                val dateAdded = cursor.getLong(dateAddedCol)
                val year = cursor.getInt(yearCol)
                val rawFolder = cursor.getString(folderCol) ?: ""
                val albumArtist = cursor.getString(albumArtistCol)?.takeIf { it.isNotBlank() }
                val composer = cursor.getString(composerCol)?.takeIf { it.isNotBlank() }
                val fileSize = cursor.getLong(sizeCol)
                val mimeType = cursor.getString(mimeTypeCol)?.takeIf { it.isNotBlank() }

                val (trackNumber, discNumber) = if (useModernTrackColumns) {
                    val trackStr = trackCol.takeIf { it >= 0 }?.let { cursor.getString(it) }
                    val discStr = discCol.takeIf { it >= 0 }?.let { cursor.getString(it) }
                    parseTrackOrDiscString(trackStr) to parseTrackOrDiscString(discStr)
                } else {
                    val legacyRaw = trackCol.takeIf { it >= 0 }?.let { cursor.getInt(it) } ?: 0
                    parseLegacyTrackColumn(legacyRaw)
                }

                val folderName = deriveFolderName(rawFolder, Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)

                val uri = ContentUris.withAppendedId(collection, id)

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        duration = duration,
                        dateAdded = dateAdded,
                        uri = uri,
                        folderName = folderName,
                        folderPath = rawFolder,
                        year = year,
                        albumArtist = albumArtist,
                        composer = composer,
                        trackNumber = trackNumber,
                        discNumber = discNumber,
                        fileSize = fileSize,
                        mimeType = mimeType
                    )
                )
            }
        }
        return songs
    }

    /** API 30+ has dedicated CD_TRACK_NUMBER/DISC_NUMBER string columns; below that, only
     *  the legacy combined TRACK int column exists. Requesting a column name the OS doesn't
     *  know about throws `IllegalArgumentException` at query time, so branch project-side
     *  and read back with `getColumnIndex` (not `getColumnIndexOrThrow`) defensively. */
    private fun trackDiscColumns(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(CD_TRACK_NUMBER_COLUMN, DISC_NUMBER_COLUMN)
        } else {
            arrayOf(MediaStore.Audio.Media.TRACK)
        }

    companion object {
        private val BASE_SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"

        // Referenced by string literal (not MediaStore.Audio.AudioColumns.CD_TRACK_NUMBER/
        // DISC_NUMBER constants) so this file still compiles against older compileSdk stubs —
        // the column names themselves are stable API 30+ platform contract either way.
        private const val CD_TRACK_NUMBER_COLUMN = "cd_track_number"
        private const val DISC_NUMBER_COLUMN = "disc_number"

        /**
         * Turns MediaStore's raw folder column into the short display name shown in Library
         * tabs (e.g. "Musik", "WhatsApp Audio"). Pure string logic, extracted so its edge
         * cases — blank path, trailing slash, root-level file — are unit-testable without a
         * real Context or file system.
         */
        internal fun deriveFolderName(rawFolder: String, useRelativePath: Boolean): String =
            if (useRelativePath) {
                rawFolder.trimEnd('/').substringAfterLast('/').ifBlank { "Musik" }
            } else {
                File(rawFolder).parentFile?.name ?: "Musik"
            }

        /** API 30+ CD_TRACK_NUMBER/DISC_NUMBER are strings, sometimes "N" and sometimes
         *  "N/total" (e.g. "5/12") — takes the leading digit run so both forms parse, same
         *  convention already used for METADATA_KEY_YEAR in `CustomFolderScanner`. Blank/
         *  non-numeric/null all collapse to "not present" (null), not a crash or a false 0. */
        internal fun parseTrackOrDiscString(raw: String?): Int? =
            raw?.trim()?.takeWhile { it.isDigit() }?.toIntOrNull()?.takeIf { it > 0 }

        /** Pre-R MediaStore only exposes the legacy combined TRACK int, historically encoded
         *  as `disc * 1000 + track` (Android's own convention, matches AOSP MediaProvider).
         *  0 or absent = neither present. A value under 1000 is track-only (single-disc,
         *  the overwhelming majority of files) — no disc tag to report, not disc 0. */
        internal fun parseLegacyTrackColumn(raw: Int): Pair<Int?, Int?> {
            if (raw <= 0) return null to null
            val disc = raw / 1000
            val track = raw % 1000
            return (track.takeIf { it > 0 }) to (disc.takeIf { it > 0 })
        }
    }
}
