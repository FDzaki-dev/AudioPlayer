package com.rudi.audioplayer.playback

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

/**
 * Extracts a dominant color from a song's album art so the Now Playing
 * screen and mini player can feel alive and personal to each track, rather
 * than using one static accent for the whole app.
 */
object AccentColorExtractor {

    /** Blocking/IO work — always call this from a background dispatcher. */
    fun extract(context: Context, albumId: Long?): Color? {
        if (albumId == null) return null
        return try {
            val uri = Uri.parse("content://media/external/audio/albumart/$albumId")
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(160, 160), null)
            } else {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            } ?: return null

            val palette = Palette.from(bitmap).generate()
            val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
            swatch?.let { normalize(Color(it.rgb)) }
        } catch (e: Exception) {
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
