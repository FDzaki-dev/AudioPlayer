package com.rudi.audioplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.toAndroidUri
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import coil3.Uri as CoilUri

/**
 * Batch 68: root cause of the "album art hilang semua" regression shipped in Batch 67.
 *
 * Batch 67 pointed [com.rudi.audioplayer.ui.Utils.AlbumArt] at `song.uri` (the song's OWN
 * audio content URI, e.g. content://media/external/audio/media/$id) instead of the old
 * per-album cache URI. That part was correct — but `AlbumArt` hands that URI to Coil's
 * default pipeline as an image `model`. Coil's built-in `ContentUriFetcher` opens the URI
 * and decodes the raw bytes with `BitmapFactory`/`ImageDecoder`, which only works for URIs
 * that ARE images. `song.uri` is an audio file; decoding its bytes as a bitmap fails for
 * every single song, so Coil's `error{}` callback fired everywhere (Library/Home/
 * MiniPlayerBar/NowPlaying) and the "no cover" music-note icon replaced all artwork —
 * matching exactly what shipped. This was silent because `AlbumArt`'s `error{}` fallback
 * was designed to also cover songs that genuinely have no embedded art, so a 100% failure
 * rate looked the same as "working as intended" without a device check (which Batch 67's
 * own notes flagged as not yet done).
 *
 * The 3 non-Coil call sites Batch 67 also touched (widget/[com.rudi.audioplayer.playback.PlaybackService],
 * [com.rudi.audioplayer.playback.AccentColorExtractor]) never had this bug — they call
 * `contentResolver.loadThumbnail()` directly instead of going through Coil, which is exactly
 * why widget/notification/accent-color art kept working while on-screen UI art broke.
 *
 * Fix: intercept audio content URIs before Coil's default fetcher and extract the embedded
 * artwork the same way those 3 call sites already do, so Coil gets an actual [Bitmap] instead
 * of raw audio bytes.
 *
 * Batch 425 (Coil 2.6.0 -> 3.6.2 migration): `AlbumArt` still passes a plain `android.net.Uri`
 * as `model` (untouched, ZERO-REFACTOR) — Coil 3's built-in `AndroidUriMapper` runs first and
 * maps that to a `coil3.Uri` before any Fetcher.Factory is consulted, so this Factory must now
 * match on `coil3.Uri`, NOT `android.net.Uri`, or it silently never claims the data and every
 * song falls through to Coil's default fetcher again — same failure shape as the Batch 68
 * regression this class exists to fix, just with a different trigger. `data.toAndroidUri()`
 * converts back so `loadThumbnail()`'s `ContentResolver` call is untouched below.
 */
class AudioArtFetcher(
    private val uri: android.net.Uri,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val bitmap = loadEmbeddedArt()
            ?: throw FileNotFoundException("Tidak ada artwork tertanam di $uri")
        return ImageFetchResult(
            image = BitmapDrawable(context.resources, bitmap).asImage(),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    // Batch 402: pre-Q MediaMetadataRetriever fallback removed — minSdk 31 (Batch 290)
    // guarantees API>=29 on every installable device, so that branch was unreachable dead code
    // (loadThumbnail() is the only path that has ever actually run).
    private fun loadEmbeddedArt(): Bitmap? = try {
        context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
    } catch (e: Exception) {
        // Batch 621 — jalur Coil (seni sampul di layar) dulu diam-diam; lagu TANPA sampul memang melempar di sini
        // dan itu normal (tak dicatat). Hanya kegagalan decoder (`ImageDecoder.DecodeException`) yang diprobe,
        // 1x per URI, lewat [ArtFailureLog] (tidak mengubah hasil: tetap null).
        ArtFailureLog.report(context, "AudioArtFetcher", uri, 512, e)
        null
    }

    /** Only claims audio URIs, so this never intercepts a real image model if one is ever added. */
    class Factory(private val context: Context) : Fetcher.Factory<CoilUri> {
        override fun create(data: CoilUri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val androidUri = data.toAndroidUri()
            if (androidUri.scheme != "content") return null
            val mimeType = try {
                context.contentResolver.getType(androidUri)
            } catch (e: Exception) {
                null
            }
            if (mimeType == null || !mimeType.startsWith("audio/")) return null
            return AudioArtFetcher(androidUri, context)
        }
    }
}

/**
 * Batch 621 — instrumentasi FAKTA MENTAH untuk `ImageDecoder$DecodeException: Failed to create image decoder with
 * message 'unimplemented'` dari `ContentResolver.loadThumbnail` (log 8-9 Nov: WidgetUpdater x23, SongArtBitmapLoader x6,
 * AccentColorExtractor x2). Pesan aslinya TIDAK memuat URI/format sehingga akarnya tak bisa ditentukan dari log.
 * Kini, tiap kegagalan decoder diprobe SEKALI per URI (maks [MAX_PROBES] URI per proses, supaya log/CPU tak membengkak):
 * nama+ukuran+mime berkas audio, ukuran + magic byte sampul tertanam (`MediaMetadataRetriever.embeddedPicture`), hasil
 * `BitmapFactory` (bounds) dan `ImageDecoder` pada byte yang sama. Murni pencatatan: TIDAK mengubah hasil pemanggil,
 * TIDAK melempar. Blocking I/O — semua pemanggil sudah berjalan di thread latar (tempat `loadThumbnail` dipanggil).
 */
object ArtFailureLog {
    private const val TAG = "ArtFailure"
    private const val MAX_PROBES = 24
    private const val MAGIC_BYTES = 16
    private const val SAMPLE_SIZE = 8
    private const val BYTE_MASK = 0xFF

    private val lock = Any()
    private val probed = LinkedHashSet<String>()

    /**
     * Batch 622 — pencatatan seragam kegagalan muat sampul (widget/notifikasi/aksen/bubble). DASAR (data probe Batch 621,
     * log 9 Nov 17:28): 5/5 URI yang gagal dengan `ImageDecoder$DecodeException 'unimplemented'` punya `embeddedPicture=null`,
     * dan judul pada galat widget lama (26/62/64 huruf) = lagu yang sama dengan probe -> perangkat ini (MediaProvider vendor)
     * melempar `DecodeException` untuk lagu TANPA sampul tertanam; pemanggil sudah jatuh ke ikon bawaan (hasil null, tak berubah).
     * Maka `DecodeException` dicatat 1 baris WARN mentah (kelas + pesan asli + URI, tanpa stacktrace yang tadinya
     * memenuhi 584 dari 978 baris log); galat lain TETAP ERROR + stacktrace. Probe fakta ([report]) tetap jalan, jadi bila ada lagu dengan
     * sampul tertanam yang benar-benar tak terdecode, `embeddedPicture` + `magic` + hasil `ImageDecoder` tercatat.
     */
    fun logFailure(context: Context, tag: String, message: String, uri: android.net.Uri, requestedPx: Int, error: Throwable) {
        if (error is ImageDecoder.DecodeException) {
            AppLogger.w(tag, "$message: uri=$uri ${error.javaClass.name}: ${error.message}")
        } else {
            AppLogger.e(tag, message, error)
        }
        report(context, tag, uri, requestedPx, error)
    }

    /** Dipanggil dari blok catch pemanggil; hanya [ImageDecoder.DecodeException] yang diprobe (kegagalan lain, mis. lagu tanpa sampul, normal). */
    fun report(context: Context, source: String, uri: android.net.Uri, requestedPx: Int, error: Throwable) {
        if (error !is ImageDecoder.DecodeException) return
        try {
            val order = markFirst(uri.toString())
            if (order == 0) return
            val note = if (order == MAX_PROBES) " | (batas $MAX_PROBES probe tercapai; probe berikutnya dilewati)" else ""
            AppLogger.w(
                TAG,
                "Probe artwork #$order (1x per URI): sumber=$source uri=$uri ukuranMinta=${requestedPx}px | " +
                    "${describeFile(context, uri)} | ${describePicture(context, uri)}$note"
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "Probe artwork sendiri gagal: sumber=$source uri=$uri ${e.javaClass.name}: ${e.message}")
        }
    }

    /** Nomor urut probe untuk URI ini (1..[MAX_PROBES]), atau 0 bila sudah pernah diprobe / batas tercapai. */
    private fun markFirst(key: String): Int = synchronized(lock) {
        if (probed.size >= MAX_PROBES || key in probed) 0 else {
            probed.add(key)
            probed.size
        }
    }

    private fun describeFile(context: Context, uri: android.net.Uri): String = try {
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                "berkas: nama=${cursor.getString(0)} size=${cursor.getLong(1)} mime=${cursor.getString(2)}"
            } else {
                "berkas: baris tak ditemukan"
            }
        } ?: "berkas: query null"
    } catch (e: Exception) {
        "berkas: GAGAL ${e.javaClass.name}: ${e.message}"
    }

    private fun describePicture(context: Context, uri: android.net.Uri): String {
        val retriever = MediaMetadataRetriever()
        val bytes: ByteArray? = try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture
        } catch (e: Exception) {
            return "embeddedPicture: GAGAL ${e.javaClass.name}: ${e.message}"
        } finally {
            retriever.release()
        }
        if (bytes == null) return "embeddedPicture: null (MediaMetadataRetriever tak menemukan sampul tertanam)"
        val magic = bytes.take(MAGIC_BYTES).joinToString("") { "%02X".format(it.toInt() and BYTE_MASK) }
        return "embeddedPicture: bytes=${bytes.size} magic=$magic | BitmapFactory: ${boundsOf(bytes)} | ImageDecoder: ${decodeHeader(bytes)}"
    }

    private fun boundsOf(bytes: ByteArray): String {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return "mime=${options.outMimeType} ${options.outWidth}x${options.outHeight}"
    }

    private fun decodeHeader(bytes: ByteArray): String {
        var mime = "?"
        var dimensions = "?"
        return try {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
                mime = info.mimeType
                dimensions = "${info.size.width}x${info.size.height}"
                decoder.setTargetSampleSize(SAMPLE_SIZE)
            }
            bitmap.recycle()
            "OK mime=$mime $dimensions"
        } catch (e: Exception) {
            "GAGAL ${e.javaClass.name}: ${e.message} (mime=$mime ukuran=$dimensions)"
        }
    }
}
