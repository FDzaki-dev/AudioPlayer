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
            folderColumn
        )

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
                        year = year
                    )
                )
            }
        }
        return songs
    }

    companion object {
        private val BASE_SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"

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
    }
}
