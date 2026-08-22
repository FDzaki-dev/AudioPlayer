package com.rudi.audioplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews
import com.rudi.audioplayer.MainActivity
import com.rudi.audioplayer.R
import com.rudi.audioplayer.data.ThemeStore
import com.rudi.audioplayer.playback.PlaybackService
import com.rudi.audioplayer.ui.theme.ThemeMode
import com.rudi.audioplayer.util.AppLogger

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
    private const val COMPACT_WIDTH_THRESHOLD_DP = 180
    // Batch 202 — HOTFIX regresi Batch 201. 90dp lebih TINGGI dari `minHeight="80dp"` yang
    // dideklarasikan sendiri di widget_player_info.xml, jadi widget ukuran DEFAULT (baru
    // dipasang, belum di-resize sama sekali) otomatis kena compact — bukan cuma yang di-shrink
    // paksa. Dampaknya nyaris universal: hampir semua widget 1-baris (tinggi umum ~80-110dp)
    // jatuh ke compact (cuma art+play), padahal sebelumnya tampil penuh. Diturunkan ke 70dp —
    // di bawah minHeight 80dp (placement default TETAP full layout), tapi masih di atas
    // kebutuhan riil compact (~52dp), jadi masih menangkap kasus asli Batch 201 (widget
    // di-shrink SUNGGUHAN sampai <70dp).
    private const val COMPACT_HEIGHT_THRESHOLD_DP = 70

    // Batch 68: fix "widget nggak pernah sinkron pas ganti tema" — widget_player(.compact).xml
    // used to hardcode the dark palette permanently (no light counterpart, and nothing ever
    // called updateAll() when the user flipped the in-app theme toggle), so the widget just
    // never moved regardless of ThemeMode. Root cause was 2-fold, fixed together: (1) no call
    // path from PlayerViewModel.setThemeMode/setThemeIdentity to WidgetUpdater.updateAll —
    // added there — and (2) the layout itself had no light variant to switch to — added here.
    //
    // Batch 84 — values swapped to the EXACT SkeuDarkText/SkeuDarkSecondaryText/SkeuLightText/
    // SkeuLightSecondaryText hex from Color.kt (was a separate, slightly-off ad-hoc palette:
    // pure #FFFFFF instead of the cooler near-white #EDEFF2, etc.) as part of "redesign widget
    // theme -> Neumorphism, hardcode": this is isDark/light MODE, a different axis from the
    // Tactile/Skeu/Apple theme picker, left untouched — but the actual color VALUES on both
    // sides of that axis now come from the app's one canonical Neumorphism palette instead of
    // an independent set that happened to look similar.
    private const val TITLE_COLOR_DARK = 0xFFEDEFF2.toInt()
    private const val ARTIST_COLOR_DARK = 0xFFA6ABB2.toInt()
    private const val TITLE_COLOR_LIGHT = 0xFF212327.toInt()
    private const val ARTIST_COLOR_LIGHT = 0xFF63676D.toInt()

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
        val albumArt = artworkUriRaw?.let { loadAlbumArtBitmap(context, Uri.parse(it)) }
        val isDark = resolveIsDark(context, ThemeStore(context).getMode())

        for (id in ids) {
            // Batch 203 — user minta widget "tahan banting" walau di-stretch/minimize
            // sekstrem, tanpa distorsi. Threshold-tebak (Batch 201/202) selalu punya celah:
            // kita cuma tahu `minWidth`/`minHeight` OPSI TERAKHIR yang di-report sistem, bukan
            // ukuran AKTUAL live selama drag — makanya berulang kali salah tebak.
            //
            // Fix struktural (API 31+/Android 12+, `RemoteViews(Map<SizeF, RemoteViews>)`):
            // OS sendiri yang pilih layout berdasar ukuran NYATA saat itu, terus-menerus,
            // dijamin oleh kontraknya sendiri tidak akan pernah render RemoteViews yang lebih
            // besar dari SizeF yang dijanjikan pas dengan ruang tersedia — jadi hard-clip
            // strukturally TIDAK MUNGKIN terjadi lagi di jalur ini, bukan cuma "threshold yang
            // lebih pas". Android <12 (minSdk 23) tetap pakai threshold Batch 202 (70dp/180dp)
            // sebagai fallback terbaik yang bisa dilakukan tanpa API itu.
            val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RemoteViews(
                    mapOf(
                        // SizeF = kebutuhan MINIMUM tiap layout (compact ~52dp tinggi/36dp
                        // art, full ~80dp tinggi/52dp art — dari komentar Batch 201/202).
                        // Sengaja SAMA dengan angka threshold lama, supaya perilaku di titik
                        // breakpoint konsisten dengan jalur fallback API<31 di bawah.
                        SizeF(110f, 52f) to buildViewsFor(context, isCompact = true, isDark, isPlaying, title, artist, albumArt),
                        SizeF(180f, 80f) to buildViewsFor(context, isCompact = false, isDark, isPlaying, title, artist, albumArt)
                    )
                )
            } else {
                val options = manager.getAppWidgetOptions(id)
                val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250)
                val isCompact = minWidthDp < COMPACT_WIDTH_THRESHOLD_DP || minHeightDp < COMPACT_HEIGHT_THRESHOLD_DP
                buildViewsFor(context, isCompact, isDark, isPlaying, title, artist, albumArt)
            }

            manager.updateAppWidget(id, views)
        }
    }

    /** Builds the RemoteViews for ONE widget instance at ONE size variant (compact or full).
     * Extracted (Batch 203) so the same construction logic can be reused for both entries of
     * the API 31+ responsive [SizeF] map AND the single-layout API<31 fallback path — the two
     * must stay byte-for-byte identical in what they set, or the breakpoint behavior would
     * silently diverge between OS versions. */
    private fun buildViewsFor(
        context: Context,
        isCompact: Boolean,
        isDark: Boolean,
        isPlaying: Boolean,
        title: String?,
        artist: String?,
        albumArt: Bitmap?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, if (isCompact) R.layout.widget_player_compact else R.layout.widget_player)

        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (isDark) R.drawable.widget_background else R.drawable.widget_background_light
        )

        // Batch 84 — play/pause disc now also has a dark/light pair (was 1 file reused for
        // both, fine back when it was a flat solid red that read okay either way; the new
        // Neumorphism SkeuEmerald/SkeuLightEmerald pair genuinely needs to switch like the
        // panel background above does, same reasoning).
        views.setInt(
            R.id.widget_play_pause,
            "setBackgroundResource",
            if (isDark) R.drawable.widget_play_button_bg else R.drawable.widget_play_button_bg_light
        )

        if (!isCompact) {
            views.setTextViewText(R.id.widget_title, title ?: "Tidak ada lagu")
            views.setTextViewText(R.id.widget_artist, artist ?: "Buka AudioPlayer")
            views.setTextColor(R.id.widget_title, if (isDark) TITLE_COLOR_DARK else TITLE_COLOR_LIGHT)
            views.setTextColor(R.id.widget_artist, if (isDark) ARTIST_COLOR_DARK else ARTIST_COLOR_LIGHT)
            // Batch 201 — `ellipsize="marquee"` in XML alone does NOT animate inside a
            // widget: real TextView marquee normally needs the view to hold Android focus
            // to scroll, and widget host views are never focusable. `setSelected(true)` is
            // the documented workaround — marquee also runs whenever `isSelected` is true,
            // regardless of focus. Static ellipsis ("...") stays as the fallback look on
            // launchers/OS versions where this trick doesn't take (matches XML default).
            views.setBoolean(R.id.widget_title, "setSelected", true)
            views.setOnClickPendingIntent(R.id.widget_next, servicePendingIntent(context, ACTION_NEXT, 2))
            views.setOnClickPendingIntent(R.id.widget_prev, servicePendingIntent(context, ACTION_PREVIOUS, 3))
        }

        views.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )

        if (albumArt != null) {
            views.setImageViewBitmap(R.id.widget_album_art, roundBitmapCorners(albumArt, radiusPx = 24f))
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)
        }

        views.setOnClickPendingIntent(R.id.widget_play_pause, servicePendingIntent(context, ACTION_TOGGLE_PLAY, 1))

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPending = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_album_art, openAppPending)
        if (!isCompact) views.setOnClickPendingIntent(R.id.widget_title, openAppPending)

        return views
    }

    /** Mirrors [com.rudi.audioplayer.ui.theme.colorsFor]'s SYSTEM branch (Compose's
     * `isSystemInDarkTheme()`) using the plain-Android equivalent, since this runs outside
     * any Composable. */
    private fun resolveIsDark(context: Context, mode: ThemeMode): Boolean = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> {
            val nightFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightFlags == Configuration.UI_MODE_NIGHT_YES
        }
    }

    private fun servicePendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, PlaybackService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        // Widget taps happen with no guarantee the app process is alive — e.g. right after
        // the user swipes AudioPlayer away from recent apps and the system reclaims the
        // process. PendingIntent.getService() sends the intent as a plain Context.startService(),
        // which Android 8+ (API 26) actively REFUSES to start from that background state
        // (IllegalStateException), so the service — and everything in it, including the
        // cold-start restore logic in PlaybackService — never even runs. getForegroundService()
        // instead calls startForegroundService(), which IS allowed from the background, on the
        // condition the service promotes itself with startForeground() within 5 seconds —
        // PlaybackService's cold-start path already does exactly that as its very first step.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(context, requestCode, intent, flags)
        }
    }

    private fun loadAlbumArtBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, android.util.Size(200, 200), null)
            } else {
                context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            // Widget jatuh balik ke ikon aplikasi kalau ini gagal (masih terlihat wajar di layar
            // beranda), tapi dicatat supaya kegagalan muat artwork tidak sepenuhnya tak terlihat.
            AppLogger.e("WidgetUpdater", "Gagal muat artwork widget", e)
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
