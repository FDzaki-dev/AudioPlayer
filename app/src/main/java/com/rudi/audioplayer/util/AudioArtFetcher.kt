package com.rudi.audioplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
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
