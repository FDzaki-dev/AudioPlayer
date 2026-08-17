package com.rudi.audioplayer.data

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.os.Build
import android.provider.MediaStore
import com.rudi.audioplayer.util.AppLogger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

/**
 * Gap List "Wajib" #1 — orkestrasi Tag Editor. Scope batch ini SENGAJA dibatasi ke lagu
 * MediaStore (bukan lagu dari folder tambahan/SAF): folder tambahan cuma dikasih izin BACA
 * saat ditambahkan ([PlayerViewModel.addCustomFolder] — `FLAG_GRANT_READ_URI_PERMISSION` saja,
 * dicek ulang sebelum nulis batch ini, bukan diasumsikan), jadi menulis balik ke file SAF
 * butuh alur izin tulis TERPISAH yang belum ada — gap tersisa, dicatat di CHANGELOG, BUKAN
 * dipaksakan diam-diam dengan asumsi izin yang ternyata tidak pernah diminta.
 */
class TagEditor(private val context: Context) {

    sealed class TagWriteResult {
        object Success : TagWriteResult()
        /** Format file/sumber lagu ini belum didukung — [reason] pesan yang aman ditampilkan
         *  langsung ke user (bukan pesan teknis/stack trace). */
        data class Unsupported(val reason: String) : TagWriteResult()
        /** Android 11+: perlu izin tulis dari user dulu (dialog sistem) sebelum lanjut —
         *  caller (ViewModel) simpan [song]/[tags] lalu launch [intentSender], panggil
         *  [writeTagsWithConsent] lagi kalau user setuju. */
        data class NeedsConsent(val intentSender: IntentSender) : TagWriteResult()
        data class Failure(val reason: String) : TagWriteResult()
    }

    private fun editabilityCheck(song: Song): TagWriteResult.Unsupported? {
        if (song.uri.authority != MediaStore.AUTHORITY) {
            return TagWriteResult.Unsupported(
                "Lagu dari folder tambahan belum didukung untuk diedit (butuh izin tulis terpisah)."
            )
        }
        val mime = song.mimeType?.lowercase(Locale.ROOT)
        if (mime != "audio/mpeg" && mime != "audio/mp3") {
            return TagWriteResult.Unsupported("Format file ini belum didukung untuk diedit (baru MP3).")
        }
        return null
    }

    fun writeTags(song: Song, tags: Id3TagWriter.EditableTags): TagWriteResult {
        editabilityCheck(song)?.let { return it }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+: minta izin user DI MUKA lewat API resmi, sebelum coba nulis
                // apa pun — lebih eksplisit daripada mengandalkan exception sebagai kontrol
                // alur (pola yang sama seperti createDeleteRequest yang sudah ada di
                // MainActivity.deleteSongsFromDevice untuk hapus lagu).
                val pending = MediaStore.createWriteRequest(context.contentResolver, listOf(song.uri))
                TagWriteResult.NeedsConsent(pending.intentSender)
            } else {
                // Android 10: belum ada createWriteRequest — pola resminya coba tulis dulu,
                // tangkap RecoverableSecurityException kalau app ini bukan pemilik file.
                try {
                    performRewrite(song, tags)
                    rescan(song)
                    TagWriteResult.Success
                } catch (e: RecoverableSecurityException) {
                    TagWriteResult.NeedsConsent(e.userAction.actionIntent.intentSender)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("TagEditor", "Gagal menulis tag untuk '${song.title}'", e)
            TagWriteResult.Failure(e.message ?: "Kesalahan tidak diketahui")
        }
    }

    /** Dipanggil ViewModel setelah user menyetujui dialog izin dari [TagWriteResult.NeedsConsent]
     *  (jalur Android 11+ `createWriteRequest`; jalur Android 10 `RecoverableSecurityException`
     *  di [writeTags] sudah langsung retry sendiri lewat sistem activity-result, tidak pernah
     *  sampai ke fungsi ini). */
    fun writeTagsWithConsent(song: Song, tags: Id3TagWriter.EditableTags): TagWriteResult {
        return try {
            performRewrite(song, tags)
            rescan(song)
            TagWriteResult.Success
        } catch (e: Exception) {
            AppLogger.e("TagEditor", "Gagal menulis tag (setelah izin) untuk '${song.title}'", e)
            TagWriteResult.Failure(e.message ?: "Kesalahan tidak diketahui")
        }
    }

    /**
     * Alur aman: (1) tulis hasil rewrite ke file SEMENTARA di cache app dulu — bukan langsung
     * ke [song.uri] — supaya kalau ada bug/exception di tengah proses encode/copy, file ASLI
     * user 0% tersentuh; (2) baru kalau file sementara itu selesai utuh, salin isinya ke
     * [song.uri] (mode "rwt" — truncate lalu tulis). Risiko yang TETAP ada dan sengaja dicatat
     * jujur (bukan diklaim 100% aman): langkah (2) tetap 1 operasi truncate+write ke file asli
     * — kalau app di-kill paksa sistem PAS di tengah langkah ini, file asli bisa berakhir
     * TERPOTONG (audio hilang sebagian dari titik itu ke akhir), bukan "rusak diam-diam
     * dengan audio salah" — kegagalan yang kalau terjadi setidaknya terdeteksi (durasi file
     * jelas beda), bukan korupsi senyap. Android tidak punya primitif rename atomik lintas
     * SAF/MediaStore provider yang bisa diandalkan untuk menghilangkan risiko ini sepenuhnya.
     */
    private fun performRewrite(song: Song, tags: Id3TagWriter.EditableTags) {
        val newTag = Id3TagWriter.buildTag(tags)
        val tempFile = File(context.cacheDir, "tagwrite_${System.nanoTime()}.tmp")
        try {
            val sourceOpened = context.contentResolver.openInputStream(song.uri)?.use { input ->
                FileOutputStream(tempFile).use { tempOut ->
                    Id3TagWriter.rewrite(input, tempOut, newTag)
                }
                true
            } ?: false
            if (!sourceOpened) throw IOException("Tidak bisa membuka file sumber untuk dibaca")

            val destOpened = context.contentResolver.openOutputStream(song.uri, "rwt")?.use { finalOut ->
                FileInputStream(tempFile).use { tempIn -> tempIn.copyTo(finalOut) }
                true
            } ?: false
            if (!destOpened) throw IOException("Tidak bisa membuka file tujuan untuk ditulis")
        } finally {
            tempFile.delete()
        }
    }

    /** Paksa MediaStore re-index file yang baru saja ditulis ulang, supaya kolom TITLE/ARTIST/
     *  dst DAN tabel Genres (Gap List #11) langsung sinkron tanpa nunggu scan media perangkat
     *  berikutnya — [PlayerViewModel.refreshLibrary] (dipanggil caller setelah Success) baru
     *  kebaca benar kalau index MediaStore sendiri sudah update duluan. */
    private fun rescan(song: Song) {
        MediaScannerConnection.scanFile(context, arrayOf(song.uri.toString()), null, null)
    }
}
