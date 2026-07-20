package com.rudi.audioplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.rudi.audioplayer.MainActivity
import com.rudi.audioplayer.R
import com.rudi.audioplayer.playback.PlaybackService

/**
 * Pushes the current playback state into any home-screen widgets. The service calls
 * [updateAll] whenever the track or play state changes; [PlayerWidgetProvider.onUpdate]
 * calls it too (reading the last cached state) whenever the system asks a widget to redraw,
 * e.g. after being placed or on reboot.
 */
object WidgetUpdater {
    private const val PREFS_NAME = "widget_state"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"
    private const val KEY_ARTWORK_URI = "artwork_uri"
    private const val KEY_IS_PLAYING = "is_playing"

    const val ACTION_TOGGLE_PLAY = "com.rudi.audioplayer.widget.TOGGLE_PLAY"
    const val ACTION_NEXT = "com.rudi.audioplayer.widget.NEXT"
    const val ACTION_PREVIOUS = "com.rudi.audioplayer.widget.PREVIOUS"

    fun saveState(context: Context, title: String?, artist: String?, artworkUri: Uri?, isPlaying: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TITLE, title)
            .putString(KEY_ARTIST, artist)
            .putString(KEY_ARTWORK_URI, artworkUri?.toString())
            .putBoolean(KEY_IS_PLAYING, isPlaying)
            .apply()
    }

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, PlayerWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, null)
        val artist = prefs.getString(KEY_ARTIST, null)
        val artworkUriRaw = prefs.getString(KEY_ARTWORK_URI, null)
        val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)

        val views = RemoteViews(context.packageName, R.layout.widget_player)
        views.setTextViewText(R.id.widget_title, title ?: "Tidak ada lagu")
        views.setTextViewText(R.id.widget_artist, artist ?: "Buka AudioPlayer")
        views.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )

        val albumArt = artworkUriRaw?.let { loadAlbumArtBitmap(context, Uri.parse(it)) }
        if (albumArt != null) {
            views.setImageViewBitmap(R.id.widget_album_art, roundBitmapCorners(albumArt, radiusPx = 24f))
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)
        }

        views.setOnClickPendingIntent(R.id.widget_play_pause, servicePendingIntent(context, ACTION_TOGGLE_PLAY, 1))
        views.setOnClickPendingIntent(R.id.widget_next, servicePendingIntent(context, ACTION_NEXT, 2))
        views.setOnClickPendingIntent(R.id.widget_prev, servicePendingIntent(context, ACTION_PREVIOUS, 3))

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPending = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_album_art, openAppPending)
        views.setOnClickPendingIntent(R.id.widget_title, openAppPending)

        for (id in ids) {
            manager.updateAppWidget(id, views)
        }
    }

    private fun servicePendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, PlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun loadAlbumArtBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, android.util.Size(200, 200), null)
            } else {
                context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Center-crops to a square and rounds the corners, so widget art matches the app's rounded look. */
    private fun roundBitmapCorners(source: Bitmap, radiusPx: Float): Bitmap {
        val size = minOf(source.width, source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2
        val squared = Bitmap.createBitmap(source, x, y, size, size)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squared, 0f, 0f, paint)
        return output
    }
}
