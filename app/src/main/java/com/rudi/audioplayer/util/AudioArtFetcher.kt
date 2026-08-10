package com.rudi.audioplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import java.io.FileNotFoundException

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
 */
class AudioArtFetcher(
    private val uri: Uri,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val bitmap = loadEmbeddedArt()
            ?: throw FileNotFoundException("Tidak ada artwork tertanam di $uri")
        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    private fun loadEmbeddedArt(): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return try {
                context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
            } catch (e: Exception) {
                null
            }
        }
        // Pre-Q: loadThumbnail() doesn't exist yet — read embedded art via MediaMetadataRetriever.
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    /** Only claims audio URIs, so this never intercepts a real image model if one is ever added. */
    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != "content") return null
            val mimeType = try {
                context.contentResolver.getType(data)
            } catch (e: Exception) {
                null
            }
            if (mimeType == null || !mimeType.startsWith("audio/")) return null
            return AudioArtFetcher(data, context)
        }
    }
}
