package com.rudi.audioplayer.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.rudi.audioplayer.util.AppLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Roadmap #5 — Ringtone Cutter, orkestrasi potong file. Sama seperti [TagEditor]/[Id3TagWriter],
 * scope batch ini SENGAJA dipersempit dulu ke yang paling aman: (1) lagu MediaStore saja (bukan
 * folder tambahan/SAF — alasan sama persis [TagEditor.editabilityCheck]), (2) format sumber
 * `audio/mpeg` (MP3) & `audio/mp4a-latm` (AAC/M4A) saja — dua-duanya bisa disalin stream-copy
 * (tanpa re-encode) ke wadah MPEG_4 lewat [MediaMuxer] mulai API 26 tanpa risiko kualitas/lisensi
 * encoder tambahan. FLAC/OGG/WAV ditolak dengan pesan jujur, bukan diam-diam gagal. (3) API 29+
 * saja untuk simpan hasil — `RELATIVE_PATH` MediaStore publik (pola identik [BackupManager]/
 * `AppLogger`), tidak butuh permission storage legacy.
 *
 * Beda dari [TagEditor]: TIDAK PERNAH menulis balik ke [Song.uri] asli — selalu bikin file BARU
 * di direktori sistem (Ringtones/Notifications/Alarms), jadi tidak butuh alur consent
 * `createWriteRequest`/`RecoverableSecurityException` sama sekali. File asli 0% tersentuh.
 *
 * **Batasan jujur (bukan disembunyikan)**: hasil potongan TIDAK otomatis jadi nada dering aktif
 * — `WRITE_SETTINGS` (untuk `RingtoneManager.setActualDefaultRingtoneUri`) sengaja tidak diminta
 * batch ini (izin sensitif, butuh flow approval terpisah). File tersimpan ke folder sistem yang
 * benar dengan flag `IS_RINGTONE`/`IS_NOTIFICATION`/`IS_ALARM` sehingga otomatis muncul di
 * pemilih nada dering bawaan Android (Pengaturan > Suara) — user pilih manual dari situ.
 */
class RingtoneEncoder(private val context: Context) {

    enum class Destination(val relativeDir: String, val label: String) {
        RINGTONE(Environment.DIRECTORY_RINGTONES, "Nada Dering"),
        NOTIFICATION(Environment.DIRECTORY_NOTIFICATIONS, "Notifikasi"),
        ALARM(Environment.DIRECTORY_ALARMS, "Alarm")
    }

    sealed class CutResult {
        data class Success(val displayName: String, val destination: Destination) : CutResult()
        data class Unsupported(val reason: String) : CutResult()
        data class Failure(val reason: String) : CutResult()
    }

    private val fileStampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    private fun editabilityCheck(song: Song): CutResult.Unsupported? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return CutResult.Unsupported("Potong nada dering butuh Android 10 ke atas.")
        }
        if (song.uri.authority != MediaStore.AUTHORITY) {
            return CutResult.Unsupported("Lagu dari folder tambahan belum didukung untuk dipotong.")
        }
        val mime = song.mimeType?.lowercase(Locale.ROOT)
        if (mime != "audio/mpeg" && mime != "audio/mp4a-latm" && mime != "audio/mp4" && mime != "audio/m4a") {
            return CutResult.Unsupported("Format file ini belum didukung untuk dipotong (baru MP3/AAC-M4A).")
        }
        return null
    }

    /** Potong [song] sesuai [range] (lihat [RingtoneCutter.clampRange]), simpan ke [destination]
     *  dengan nama tampilan [label]. Jalankan di background thread (I/O berat) — caller
     *  (ViewModel) yang bertanggung jawab pindah dispatcher, kelas ini sengaja tidak `suspend`
     *  supaya tetap sinkron/mudah diuji manual, pola sama [TagEditor.writeTags]. */
    fun cut(song: Song, range: RingtoneCutter.TrimRange, destination: Destination, label: String): CutResult {
        editabilityCheck(song)?.let { return it }
        if (!RingtoneCutter.isValid(range, song.duration)) {
            return CutResult.Failure("Rentang potong tidak valid.")
        }

        var tempFile: File? = null
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, song.uri, null)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) {
                extractor.release()
                return CutResult.Failure("Tidak ada trek audio yang bisa dibaca dari file ini.")
            }
            extractor.selectTrack(audioTrackIndex)

            tempFile = File(context.cacheDir, "ringtone_cut_${System.nanoTime()}.tmp")
            val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val startUs = range.startMs * 1000
            val endUs = range.endMs * 1000
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val bufferSize = 1 shl 20 // 1MB, cukup untuk 1 sample audio compressed mana pun
            val buffer = java.nio.ByteBuffer.allocate(bufferSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            while (true) {
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs < 0 || sampleTimeUs > endUs) break

                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                // Presentasi waktu direbase ke 0 dari titik potong — hasil akhir file dimulai
                // dari 0, bukan dari offset lagu asli (pemutar mana pun butuh timestamp mulai
                // dari 0 di file baru yang berdiri sendiri).
                bufferInfo.presentationTimeUs = (sampleTimeUs - startUs).coerceAtLeast(0)
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()

            val displayName = buildDisplayName(label)
            val savedUri = saveToMediaStore(tempFile, displayName, destination)
                ?: return CutResult.Failure("Gagal menyimpan hasil potongan ke penyimpanan.")

            AppLogger.i("RingtoneEncoder", "Potongan tersimpan: $displayName -> $savedUri")
            return CutResult.Success(displayName, destination)
        } catch (e: Exception) {
            AppLogger.e("RingtoneEncoder", "Gagal memotong '${song.title}'", e)
            return CutResult.Failure(e.message ?: "Kesalahan tidak diketahui saat memotong.")
        } finally {
            tempFile?.delete()
        }
    }

    private fun buildDisplayName(label: String): String {
        val safeLabel = label.trim().ifEmpty { "Ringtone" }
            .replace(Regex("[^A-Za-z0-9 _-]"), "")
            .take(40)
        return "${safeLabel}_${fileStampFormat.format(Date())}_${UUID.randomUUID().toString().take(8)}.m4a"
    }

    private fun saveToMediaStore(tempFile: File, displayName: String, destination: Destination): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${destination.relativeDir}/AudioPlayer")
            put(MediaStore.Audio.Media.IS_RINGTONE, destination == Destination.RINGTONE)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, destination == Destination.NOTIFICATION)
            put(MediaStore.Audio.Media.IS_ALARM, destination == Destination.ALARM)
            put(MediaStore.Audio.Media.IS_MUSIC, false)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            tempFile.inputStream().use { it.copyTo(out) }
        } ?: return null
        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }
}
