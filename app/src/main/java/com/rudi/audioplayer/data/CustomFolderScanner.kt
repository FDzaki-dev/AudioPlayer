package com.rudi.audioplayer.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.rudi.audioplayer.util.AppLogger

/**
 * Scans a folder tree the user granted through the system's folder picker (Storage
 * Access Framework) for audio files — useful for folders MediaStore hasn't indexed yet
 * (freshly copied files, some file-manager writes, folders outside the usual watch
 * paths). Reads metadata directly from each file with [MediaMetadataRetriever],
 * independent of the system's media index. Purely local/offline, no network involved.
 */
class CustomFolderScanner(private val context: Context) {

    fun scan(treeUri: Uri, rootLabel: String): List<Song> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val results = mutableListOf<Song>()
        collect(root, rootLabel, depth = 0, out = results)
        return results
    }

    private fun collect(dir: DocumentFile, folderLabel: String, depth: Int, out: MutableList<Song>) {
        if (depth > MAX_DEPTH) return
        val children = try {
            dir.listFiles()
        } catch (e: Exception) {
            // Folder tambahan mungkin sudah dipindah/dicabut aksesnya sejak izin diberikan —
            // scan folder lain tetap lanjut (return di sini hanya menghentikan cabang ini),
            // tapi dicatat supaya "kok folder saya kosong" bisa ditelusuri lewat Log Diagnostik.
            AppLogger.e("CustomFolderScanner", "Gagal membaca isi folder '$folderLabel'", e)
            return
        }
        for (child in children) {
            when {
                child.isDirectory -> collect(child, child.name ?: folderLabel, depth + 1, out)
                child.isFile && isAudioFile(child.name) -> readSong(child, folderLabel)?.let { out.add(it) }
            }
        }
    }

    private fun isAudioFile(name: String?): Boolean {
        val ext = name?.substringAfterLast('.', "")?.lowercase(java.util.Locale.ROOT) ?: return false
        return ext in AUDIO_EXTENSIONS
    }

    private fun readSong(doc: DocumentFile, folderLabel: String): Song? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, doc.uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            if (duration <= 0L) return null

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: doc.name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                ?: "Tanpa Judul"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: "Tidak Diketahui"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
            // METADATA_KEY_YEAR isn't always a bare "YYYY" (some taggers write "YYYY-MM-DD" or
            // similar) — take the leading digit run so "2015" out of "2015-03-01" still parses,
            // instead of toIntOrNull() failing on the whole string and silently dropping it.
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.takeWhile { it.isDigit() }
                ?.toIntOrNull() ?: 0

            Song(
                id = stableId(doc.uri),
                title = title,
                artist = artist,
                album = album,
                // No MediaStore album ID exists for SAF-sourced files, so there's no
                // artwork lookup for these tracks yet — they show the app's default
                // placeholder art instead of embedded cover art.
                albumId = -1L,
                duration = duration,
                dateAdded = doc.lastModified() / 1000,
                uri = doc.uri,
                folderName = folderLabel,
                folderPath = "Folder Tambahan/$folderLabel",
                year = year
            )
        } catch (e: Exception) {
            // File ini sudah lolos filter ekstensi audio tapi metadatanya tidak terbaca (file
            // rusak/setengah-tersalin/format tak didukung retriever) — dilewati diam-diam ke
            // UI (lagu lain di folder yang sama tetap tampil), tapi dicatat agar bisa dilacak.
            AppLogger.e("CustomFolderScanner", "Gagal baca metadata '${doc.name}'", e)
            null
        } finally {
            retriever.release()
        }
    }

    /** Deterministic negative ID from the file's URI. Negative space is reserved
     * exclusively for SAF-sourced songs — MediaStore IDs (`MusicRepository`) are always
     * non-negative real `_ID` values, so the sign bit alone already separates the two
     * identity namespaces explicitly; no extra tagging needed.
     *
     * Gap list #3/#5: this used to be `String.hashCode()` (Java's 32-bit hash) masked to
     * 31 bits — only ~2.1 billion buckets, and the 32-bit algorithm itself is a weak,
     * publicly-known-collision-prone mix (birthday-bound collisions expected past roughly
     * tens of thousands of distinct URIs, well within reach of a large SAF-scanned library).
     * Replaced with 64-bit FNV-1a over the URI's UTF-8 bytes (masked to 63 bits, so the
     * result always stays negative after negation) — collision space is ~2^63, birthday-bound
     * collision only expected past ~3 billion distinct URIs. Still technically a hash (not a
     * registry-backed guaranteed-unique key), but that gap is now astronomically smaller and
     * the algorithm itself has no known practical weakness. */
    private fun stableId(uri: Uri): Long = Companion.stableId(uri.toString())

    companion object {
        private const val MAX_DEPTH = 6
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "amr", "wma")

        /** Pure, Context-free — kept in the companion so it's unit-testable without
         * Robolectric (same pattern as `MusicRepository.deriveFolderName`, Batch 27). */
        fun stableId(uriString: String): Long {
            val fnvHash = fnv1a64(uriString)
            return -(fnvHash and 0x7FFFFFFFFFFFFFFFL) - 1L
        }

        private fun fnv1a64(input: String): Long {
            var hash = -3750763034362895579L // FNV offset basis (0xcbf29ce484222325)
            val prime = 1099511628211L // FNV prime
            for (byte in input.toByteArray(Charsets.UTF_8)) {
                hash = hash xor (byte.toLong() and 0xFF)
                hash *= prime
            }
            return hash
        }
    }
}
