package com.rudi.audioplayer.data

import android.content.ContentUris
import android.content.Context
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

    /**
     * Gap List #11 — genre has no plain column on the main Media row (unlike track/disc/
     * album-artist above), it only exists via the separate Genres/Genres.Members tables.
     * Building one id->name map here costs one query per GENRE THE DEVICE HAS (typically a
     * handful to a few dozen), not one per song — deliberately avoiding the N+1-per-song
     * cost that was the stated reason genre was skipped back in Batch 89's SmartPlaylist work.
     */
    private fun buildGenreMap(): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        context.contentResolver.query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            null, null, null
        )?.use { genresCursor ->
            val genreIdCol = genresCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val nameCol = genresCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
            while (genresCursor.moveToNext()) {
                val genreName = genresCursor.getString(nameCol)?.takeIf { it.isNotBlank() } ?: continue
                val genreId = genresCursor.getLong(genreIdCol)
                val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                context.contentResolver.query(
                    membersUri,
                    arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                    null, null, null
                )?.use { membersCursor ->
                    val audioIdCol = membersCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                    while (membersCursor.moveToNext()) {
                        // A song that's (unusually) tagged into more than one genre bucket
                        // keeps whichever genre this loop visits last — acceptable for a
                        // single display field, same simplification the gap list's own
                        // "multiple genre bila format memungkinkan" phrasing treats as
                        // optional, not mandatory.
                        map[membersCursor.getLong(audioIdCol)] = genreName
                    }
                }
            }
        }
        return map
    }

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
        val genreMap = buildGenreMap()

        // Sektor SDK_INT legacy mati (Batch 402 katalog, dieksekusi hati-hati batch ini): minSdk
        // 31 (sejak Batch 290) sudah di atas Q (29) — RELATIVE_PATH SELALU tersedia, cabang DATA
        // (pre-Q) tidak pernah bisa tereksekusi lagi di device manapun yang bisa install app ini.
        // deriveFolderName() sendiri (companion object di bawah) TETAP mendukung kedua mode apa
        // adanya — masih diuji langsung lewat useRelativePath=false di
        // MusicRepositoryFolderNameTest.kt, fungsinya TIDAK disentuh, cuma call site ini.
        val folderColumn = MediaStore.Audio.Media.RELATIVE_PATH

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
            // minSdk 31 sudah di atas R (30) — CD_TRACK_NUMBER/DISC_NUMBER SELALU ada, kolom
            // TRACK gabungan lama (pre-R) tidak pernah dibaca lagi lewat jalur query ini.
            val trackCol = cursor.getColumnIndex(CD_TRACK_NUMBER_COLUMN)
            val discCol = cursor.getColumnIndex(DISC_NUMBER_COLUMN)

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

                val trackStr = trackCol.takeIf { it >= 0 }?.let { cursor.getString(it) }
                val discStr = discCol.takeIf { it >= 0 }?.let { cursor.getString(it) }
                val (trackNumber, discNumber) = parseTrackOrDiscString(trackStr) to parseTrackOrDiscString(discStr)

                val folderName = deriveFolderName(rawFolder, useRelativePath = true)

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
                        mimeType = mimeType,
                        genre = genreMap[id]
                    )
                )
            }
        }
        return songs
    }

    /** minSdk 31 sudah di atas R (30) — CD_TRACK_NUMBER/DISC_NUMBER SELALU ada di projection,
     *  cabang TRACK gabungan lama (pre-R) tidak pernah diminta lagi (dihapus, Batch 402 sektor
     *  SDK_INT legacy mati). Dibaca balik tetap lewat `getColumnIndex` (bukan
     *  `getColumnIndexOrThrow`) di `querySongs()` — bukan lagi karena API level, tapi jaring
     *  pengaman kalau provider MediaStore device tertentu genuinely tidak expose kolom ini. */
    private fun trackDiscColumns(): Array<String> = arrayOf(CD_TRACK_NUMBER_COLUMN, DISC_NUMBER_COLUMN)

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
