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
                folderPath = "Folder Tambahan/$folderLabel"
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

    /** Deterministic negative ID from the file's URI — MediaStore IDs are always
     * non-negative, so this can never collide with a real scanned song. */
    private fun stableId(uri: Uri): Long {
        val hash = uri.toString().hashCode().toLong()
        return -(hash and 0x7FFFFFFFL) - 1L
    }

    companion object {
        private const val MAX_DEPTH = 6
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "amr", "wma")
    }
}
