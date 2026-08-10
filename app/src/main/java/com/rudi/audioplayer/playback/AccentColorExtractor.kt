package com.rudi.audioplayer.playback

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.rudi.audioplayer.util.AppLogger

/**
 * Extracts a dominant color from a song's album art so the Now Playing
 * screen and mini player can feel alive and personal to each track, rather
 * than using one static accent for the whole app.
 */
object AccentColorExtractor {

    /** Blocking/IO work — always call this from a background dispatcher.
     * [songUri] is the song's own MediaStore content URI (e.g. `song.uri` from
     * [com.rudi.audioplayer.data.Song]) — not the legacy per-album "albumart" authority, which
     * relies on a cache table that's frequently empty on modern Android and threw
     * FileNotFoundException on most songs (see diagnostic log history, Batch 22 onward).
     * loadThumbnail() on the song's own URI is the framework-supported path: it falls back to
     * MediaMetadataRetriever internally to pull embedded art, no legacy cache needed. */
    fun extract(context: Context, songUri: Uri?): Color? {
        if (songUri == null) return null
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(songUri, Size(160, 160), null)
            } else {
                context.contentResolver.openInputStream(songUri)?.use { BitmapFactory.decodeStream(it) }
            } ?: return null

            val palette = Palette.from(bitmap).generate()
            val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
            swatch?.let { normalize(Color(it.rgb)) }
        } catch (e: Exception) {
            // Gagal ekstrak warna hanya berarti Now Playing jatuh balik ke aksen statis (tidak
            // fatal), tapi dicatat karena sebelumnya kegagalan ini tidak meninggalkan jejak sama sekali.
            AppLogger.e("AccentColorExtractor", "Gagal ekstrak warna aksen dari album art", e)
            null
        }
    }

    /** Keeps whatever color comes out of the album art vivid and legible on a dark background. */
    private fun normalize(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt(),
            hsv
        )
        hsv[1] = hsv[1].coerceAtLeast(0.5f)
        hsv[2] = hsv[2].coerceIn(0.6f, 0.95f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
