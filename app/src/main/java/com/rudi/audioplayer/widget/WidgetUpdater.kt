package com.rudi.audioplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.SystemClock
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.TextView
import com.rudi.audioplayer.MainActivity
import com.rudi.audioplayer.R
import com.rudi.audioplayer.data.ThemeStore
import com.rudi.audioplayer.playback.PlaybackService
import com.rudi.audioplayer.ui.theme.ThemeMode
import com.rudi.audioplayer.util.AppLogger
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil

/**
 * Pushes the current playback state into any home-screen widgets. The service calls
 * [updateAll] whenever the track or play state changes; [PlayerWidgetProvider.onUpdate]
 * calls it too (reading the last cached state) whenever the system asks a widget to redraw,
 * e.g. after being placed or on reboot.
 *
 * Batch 206 — REVERT PENUH Batch 201-204 ("widget tahan banting" — height-check compact,
 * responsive SizeF map API 31+, marquee judul via setSelected). User laporan: widget render
 * benar sesaat, lalu ~15 detik kemudian jatuh ke placeholder "Ketuk untuk memulihkan" khas
 * Android saat sebuah widget provider melempar exception/gagal update — bukan cuma soal
 * layout/distorsi visual lagi, sudah 4 iterasi coba (201/202/203/204) dan situasinya makin
 * parah, bukan membaik. User minta eksplisit: fallback total ke kondisi SEBELUM Batch 201,
 * tanpa kosmetik tambahan apapun. File ini persis logic pra-Batch-201 (width-only threshold,
 * 1 RemoteViews per update, ellipsize statis — lihat widget_player.xml, direvert sepasang).
 */
object WidgetUpdater {
    private const val PREFS_NAME = "widget_state"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"
    private const val KEY_ARTWORK_URI = "artwork_uri"
    private const val KEY_IS_PLAYING = "is_playing"
    private const val COMPACT_WIDTH_THRESHOLD_DP = 180
    // Batch 208 — dikembalikan (bukan Batch 203's SizeF/setSelected marquee — itu yang
    // dicurigai jadi penyebab crash "Ketuk untuk memulihkan", TETAP di-revert). Height-check
    // sendiri (Batch 201/202, sebelum SizeF masuk di Batch 203) TIDAK PERNAH bikin crash —
    // cuma nilainya yang salah kalibrasi awalnya (90dp > minHeight 80dp declared, sudah
    // dikoreksi 70dp di Batch 202). User laporan screenshot: widget full layout 1-baris
    // (dipaksa pendek) bikin judul+artis kepotong vertikal — persis kasus yang height-check
    // ini tangani, tanpa API baru/reflection apapun yang berisiko.
    private const val COMPACT_HEIGHT_THRESHOLD_DP = 70

    // Batch 589 — inset sudut widget. Padding root di bawah HARUS sama dengan `android:padding` root
    // `widget_player.xml` (14dp) dan `widget_player_compact.xml` (8dp). `CORNER_INSET_FRACTION` = 1 - 1/sqrt(2):
    // jarak minimum dari tepi agar sudut siku isi tidak keluar dari busur sudut berradius R (launcher meng-clip
    // widget ke radius sistem). `MAX_CORNER_INSET_DP` mencegah layout compact terdesak pada radius ekstrem.
    private const val FULL_PADDING_DP = 14f
    private const val COMPACT_PADDING_DP = 8f
    private const val CORNER_INSET_FRACTION = 0.2929f
    private const val MAX_CORNER_INSET_DP = 16f

    // Batch 591 — marquee judul. Marquee `TextView` hanya jalan bila view selected/fokus, dan XML saja ternyata tak
    // cukup di launcher user. `RemoteViews` hanya boleh memanggil method ber-anotasi `@RemotableViewMethod`
    // (kalau tidak, `apply` di launcher melempar ActionException -> widget error). Maka remotable-nya
    // `TextView.setSelected` DIPERIKSA di perangkat (nama anotasi dibandingkan sebagai string: kelas
    // anotasinya @hide) — dipanggil lewat RemoteViews hanya bila terbukti remotable, selain itu dilewati.
    private const val REMOTABLE_ANNOTATION = "android.view.RemotableViewMethod"

    // Batch 593 — INSTRUMENTASI marquee judul (BUKAN perbaikan; hipotesis Batch 592 terbukti salah: gejala sama).
    // Lebar TETAP non-judul tiap layout (dp), dari XML, hanya untuk ESTIMASI lebar kolom judul di log:
    // penuh = padding 14x2 + sampul 52 + margin kolom 14+8 + prev 34 + (6+44) + (6+34) = 226;
    // compact = padding 8x2 + sampul 36 + margin judul 8 + (28+4) + (40+4) + (28+4) = 168.
    // Harus sama dgn `widget_player.xml` / `widget_player_compact.xml` (hanya memengaruhi baris log).
    private const val FULL_FIXED_WIDTH_DP = 226f
    private const val COMPACT_FIXED_WIDTH_DP = 168f
    private const val FULL_TITLE_SP = 14f
    private const val COMPACT_TITLE_SP = 13f

    // Batch 68: fix "widget nggak pernah sinkron pas ganti tema" — widget_player(.compact).xml
    // used to hardcode the dark palette permanently (no light counterpart, and nothing ever
    // called updateAll() when the user flipped the in-app theme toggle), so the widget just
    // never moved regardless of ThemeMode. Root cause was 2-fold, fixed together: (1) no call
    // path from PlayerViewModel.setThemeMode/setThemeIdentity to WidgetUpdater.updateAll —
    // added there — and (2) the layout itself had no light variant to switch to — added here.
    //
    // Batch 583 — ROMBAK Neumorphism burgundy: kelima warna di bawah = hex PERSIS token Color.kt
    // (Batch 563), bukan salinan ad-hoc. Batch 84 dulu menyalin SkeuDark/LightText versi lama
    // (abu-perak dingin); sejak Batch 563 token itu menjadi ivory/champagne (gelap) dan near-black
    // anggur/umber (terang), tapi widget tertinggal di palet lama. Kontras (hitungan di komentar
    // Color.kt): judul gelap 14.5:1, artis gelap 8.8:1, judul terang 13:1, artis terang 6.1:1.
    private const val TITLE_COLOR_DARK = 0xFFEFE6D8.toInt() // SkeuDarkText
    private const val ARTIST_COLOR_DARK = 0xFFC2B49C.toInt() // SkeuDarkSecondaryText
    private const val TITLE_COLOR_LIGHT = 0xFF2A2024.toInt() // SkeuLightText
    private const val ARTIST_COLOR_LIGHT = 0xFF62524A.toInt() // SkeuLightSecondaryText

    // Batch 583 — ikon prev/next asli (ic_widget_previous/next.png) berwarna ivory 0xFFF5F1E8: terbaca
    // di panel gelap, TAPI hampir hilang di perkamen mode terang. Mode terang memakai salinan
    // berwarna burgundy SkeuAccentLight (kontras ~9.8:1 di atas permukaan kenop); mode gelap tetap PNG asli.
    private const val GLYPH_COLOR_LIGHT = 0xFF741C32.toInt() // SkeuAccentLight

    // Batch 583 — fillet gilt di tepi sampul (pola Modifier.neuArtFillet Batch 569: gilt diagonal
    // alpha 0.90 -> 0.18 -> 0.60). Gelap = SkeuGilt, terang = SkeuGiltDeep (champagne terang hilang di perkamen).
    internal const val FILLET_COLOR_DARK = 0xFFDCC393.toInt() // SkeuGilt
    private const val FILLET_COLOR_LIGHT = 0xFF9C7E4E.toInt() // SkeuGiltDeep
    private const val FILLET_ALPHA_START = 0.90f
    private const val FILLET_ALPHA_MID = 0.18f
    private const val FILLET_ALPHA_END = 0.60f

    // Batch 583/584 — BENTUK "DAUN DIAGONAL" (Batch 584: user minta bentuk segar, bukan lingkaran): sudut kiri-atas &
    // kanan-bawah besar (0.36 x sisi), kanan-atas & kiri-bawah kecil (0.16 x sisi) — sumbu besar searah sumber
    // cahaya kiri-atas, keluarga yang sama dgn panel/medali/kenop (res/drawable/widget_*.xml). Proporsional:
    // ~18.7/8.3dp pada sampul widget 52dp, ~14.4/6.4dp pada sampul bubble 40dp. Tebal fillet 0.024 x sisi ~ 1.25dp
    // pada 52dp.
    private const val LEAF_BIG_FRACTION = 0.36f
    private const val LEAF_SMALL_FRACTION = 0.16f
    private const val ART_FILLET_STROKE_FRACTION = 0.024f

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

    // Batch 587 — `updateAll` dipanggil dari 4 tempat (PlaybackService.pushWidgetUpdate, PlayerViewModel x2,
    // PlayerWidgetProvider) dan kodenya non-suspend: `widgetUpdateJob?.cancel()` tak menghentikan yang sedang
    // jalan, jadi saat lagu diganti cepat lewat widget (next/prev berulang) belasan update memuat thumbnail +
    // membuat bitmap + memanggil updateAppWidget SERENTAK di thread IO. Dan exception apa pun di sini
    // (RuntimeException dari updateAppWidget, OutOfMemoryError) lolos ke `launch` tanpa handler -> seluruh
    // proses (termasuk pemutaran) mati hanya gara-gara menyegarkan widget. Sekarang: (1) satu update dalam
    // satu waktu; (2) permintaan yang basi dilewati karena yang terbaru membaca state terbaru saat berjalan;
    // (3) kegagalan dicatat mentah ke log diagnostik dan tidak pernah menjatuhkan proses.
    private val updateLock = Any()
    private val updateTicket = AtomicInteger()

    // Batch 593 — penghitung diagnostik marquee (semua akses di dalam `synchronized(updateLock)`; atomik
    // supaya aman tanpa @Volatile): nomor update, waktu update sebelumnya, judul terakhir, jumlah permintaan
    // basi yang dilewati sejak baris log terakhir.
    private val diagSeq = AtomicInteger()
    private val diagLastAtMs = AtomicLong()
    private val diagLastTitle = AtomicReference<String?>(null)
    private val diagSkipped = AtomicInteger()

    fun updateAll(context: Context) {
        val ticket = updateTicket.incrementAndGet()
        synchronized(updateLock) {
            if (ticket != updateTicket.get()) {
                diagSkipped.incrementAndGet()
                return
            }
            try {
                performUpdate(context)
            } catch (e: Exception) {
                AppLogger.e("WidgetUpdater", "Gagal memperbarui widget", e)
            } catch (e: OutOfMemoryError) {
                AppLogger.e("WidgetUpdater", "Memori habis saat memperbarui widget", e)
            }
        }
    }

    // Batch 589 — "window insets" untuk widget layar beranda. RemoteViews dirender launcher dan TIDAK punya API
    // WindowInsets, jadi padanannya: padding root minimal sebesar jarak aman terhadap radius sudut widget yang
    // dipakai launcher (`android.R.dimen.system_app_widget_background_radius`, API 31 = minSdk). Padding XML
    // dipertahankan bila sudah cukup (kasus umum: radius <= ~27dp untuk compact, <= ~47dp untuk penuh -> tak ada
    // perubahan); hanya dinaikkan bila radius sistem perangkat lebih besar. Gagal = dilewati (padding XML tetap).
    private fun applyCornerInset(context: Context, views: RemoteViews, isCompact: Boolean) {
        try {
            val resources = context.resources
            val density = resources.displayMetrics.density
            val xmlPadding = (if (isCompact) COMPACT_PADDING_DP else FULL_PADDING_DP) * density
            val radius = resources.getDimension(android.R.dimen.system_app_widget_background_radius)
            val inset = (radius * CORNER_INSET_FRACTION).coerceAtMost(MAX_CORNER_INSET_DP * density)
            if (inset > xmlPadding) {
                val pad = ceil(inset).toInt()
                views.setViewPadding(R.id.widget_root, pad, pad, pad, pad)
            }
        } catch (e: Exception) {
            AppLogger.e("WidgetUpdater", "Gagal menerapkan inset sudut widget", e)
        }
    }

    private val titleMarqueeSelectable: Boolean by lazy {
        val remotable = try {
            TextView::class.java.getMethod("setSelected", Boolean::class.javaPrimitiveType)
                .annotations.any { it.annotationClass.java.name == REMOTABLE_ANNOTATION }
        } catch (e: Exception) {
            false
        }
        AppLogger.w("WidgetUpdater", "Marquee judul: TextView.setSelected remotable=$remotable (true = dipanggil lewat RemoteViews)")
        remotable
    }

    // Batch 592 — urutan WAJIB: setSelected(false) SEBELUM teks judul diganti, setSelected(true) SESUDAHNYA.
    // Mengganti teks `TextView` yang marquee-nya sedang berjalan (selected) merusak state marquee-nya
    // (layout marquee/ellipsis tertukar), dan `setSelected` hanya memulai marquee saat state BERUBAH. Gejala
    // yang dilaporkan: marquee jalan hanya saat judul placeholder "Tidak ada lagu" (teks tak pernah berganti)
    // dan mati begitu judul lagu berganti. Dipanggil di setiap update, jadi marquee mulai dari awal tiap update.
    private fun setTitleSelected(views: RemoteViews, selected: Boolean) {
        if (titleMarqueeSelectable) views.setBoolean(R.id.widget_title, "setSelected", selected)
    }

    // Batch 593 — SATU baris log per widget per update (level WARN, tag WidgetUpdater, awalan "Marquee diag"),
    // supaya penyebab marquee mati saat lagu nyata bisa diputuskan dari DATA, bukan tebakan. Isi: nomor update +
    // selang dari update sebelumnya (update beruntun <~1,2 dtk = marquee diulang sebelum sempat bergerak, jeda mulai
    // marquee Android 1200 ms) + jumlah permintaan basi yang digabung; id widget, mode, ukuran min/maks yang
    // dilaporkan launcher; panjang judul + apakah berubah + sedang main; ESTIMASI lebar teks (Paint bold, sp ikut
    // font scale) vs lebar kolom MINIMUM (min width - lebar tetap layout; kolom asli >= angka ini) -> apakah teks
    // memang lebih lebar dari kolom (kalau tidak, marquee wajar tidak jalan); status remotable. Gagal = dicatat,
    // tak mengganggu update.
    private fun logMarqueeDiag(
        context: Context,
        seq: Int,
        sinceLastMs: Long,
        skipped: Int,
        title: String?,
        titleChanged: Boolean,
        isPlaying: Boolean,
        widgetId: Int,
        isCompact: Boolean,
        options: android.os.Bundle
    ) {
        try {
            val metrics = context.resources.displayMetrics
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, -1)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, -1)
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, -1)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, -1)
            val shown = title ?: "Tidak ada lagu"
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                if (isCompact) COMPACT_TITLE_SP else FULL_TITLE_SP,
                metrics
            )
            val textDp = paint.measureText(shown) / metrics.density
            val columnMinDp = minW - (if (isCompact) COMPACT_FIXED_WIDTH_DP else FULL_FIXED_WIDTH_DP)
            AppLogger.w(
                "WidgetUpdater",
                "Marquee diag #$seq +${sinceLastMs}ms digabung=$skipped | id=$widgetId mode=${if (isCompact) "compact" else "penuh"} " +
                    "min=${minW}x${minH}dp maks=${maxW}x${maxH}dp fontScale=${context.resources.configuration.fontScale} | " +
                    "judul=${shown.length}huruf berubah=$titleChanged playing=$isPlaying | " +
                    "ESTIMASI teks=${textDp.toInt()}dp kolomMin=${columnMinDp.toInt()}dp teks>kolomMin=${textDp > columnMinDp} | " +
                    "remotable=$titleMarqueeSelectable"
            )
        } catch (e: Exception) {
            AppLogger.e("WidgetUpdater", "Gagal mencatat diagnostik marquee", e)
        }
    }

    private fun performUpdate(context: Context) {
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

        // Batch 593 — diagnostik marquee: nomor + selang dari update sebelumnya + judul berubah atau tidak.
        val diagNow = SystemClock.elapsedRealtime()
        val diagPrevAt = diagLastAtMs.getAndSet(diagNow)
        val diagSinceMs = if (diagPrevAt == 0L) -1L else diagNow - diagPrevAt
        val diagSeqNo = diagSeq.incrementAndGet()
        val diagSkippedNow = diagSkipped.getAndSet(0)
        val diagTitleChanged = diagLastTitle.getAndSet(title) != title

        // Batch 583 — bahan visual yang SAMA untuk semua widget dibuat SEKALI di luar loop (dulu sampul
        // dibulatkan ulang per widget): sampul berfillet gilt + (mode terang saja) ikon prev/next
        // berwarna burgundy. Null = pakai bawaan layout (ikon PNG ivory / ic_launcher).
        val artPlate = albumArt?.let { buildArtPlate(it, if (isDark) FILLET_COLOR_DARK else FILLET_COLOR_LIGHT) }
        val prevGlyph = if (isDark) null else tintedGlyph(context, R.drawable.ic_widget_previous, GLYPH_COLOR_LIGHT)
        val nextGlyph = if (isDark) null else tintedGlyph(context, R.drawable.ic_widget_next, GLYPH_COLOR_LIGHT)

        for (id in ids) {
            // Widgets can be resized independently by the user, so each one gets its own
            // layout choice — a widget shrunk down to just an icon shouldn't try to cram in
            // a title, artist, and three buttons that no longer fit.
            val options = manager.getAppWidgetOptions(id)
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250)
            val isCompact = minWidthDp < COMPACT_WIDTH_THRESHOLD_DP || minHeightDp < COMPACT_HEIGHT_THRESHOLD_DP
            logMarqueeDiag(context, diagSeqNo, diagSinceMs, diagSkippedNow, title, diagTitleChanged, isPlaying, id, isCompact, options)

            val views = RemoteViews(context.packageName, if (isCompact) R.layout.widget_player_compact else R.layout.widget_player)
            applyCornerInset(context, views, isCompact)

            views.setInt(
                R.id.widget_root,
                "setBackgroundResource",
                if (isDark) R.drawable.widget_background else R.drawable.widget_background_light
            )

            // Batch 583 — medali play/pause (burgundy + cincin gilt) SATU drawable untuk kedua mode:
            // `widget_play_button_bg_light.xml` (pasangan Batch 84) DIHAPUS, jadi tak ada lagi penukaran
            // di sini; `widget_player(.compact).xml` sudah menunjuk `widget_play_button_bg`. Kenop
            // sekunder prev/next BARU berkedalaman dan punya pasangan gelap/terang (permukaan netral
            // mengikuti panel), ditukar lewat setBackgroundResource — mekanisme yang sama persis dgn panel.
            val keyBackground = if (isDark) R.drawable.widget_key_bg else R.drawable.widget_key_bg_light
            views.setInt(R.id.widget_prev, "setBackgroundResource", keyBackground)
            views.setInt(R.id.widget_next, "setBackgroundResource", keyBackground)
            if (prevGlyph != null) views.setImageViewBitmap(R.id.widget_prev, prevGlyph)
            if (nextGlyph != null) views.setImageViewBitmap(R.id.widget_next, nextGlyph)

            setTitleSelected(views, false)
            views.setTextViewText(R.id.widget_title, title ?: "Tidak ada lagu")
            views.setTextColor(R.id.widget_title, if (isDark) TITLE_COLOR_DARK else TITLE_COLOR_LIGHT)
            setTitleSelected(views, true)
            views.setOnClickPendingIntent(R.id.widget_next, servicePendingIntent(context, ACTION_NEXT, 2))
            views.setOnClickPendingIntent(R.id.widget_prev, servicePendingIntent(context, ACTION_PREVIOUS, 3))
            if (!isCompact) {
                views.setTextViewText(R.id.widget_artist, artist ?: "Buka SONIX")
                views.setTextColor(R.id.widget_artist, if (isDark) ARTIST_COLOR_DARK else ARTIST_COLOR_LIGHT)
            }

            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )

            if (artPlate != null) {
                views.setImageViewBitmap(R.id.widget_album_art, artPlate)
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
            views.setOnClickPendingIntent(R.id.widget_title, openAppPending)

            manager.updateAppWidget(id, views)
        }
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
        // Batch 402: pre-O getService() fallback removed — minSdk 31 (Batch 290) guarantees
        // API>=26 on every installable device, so that branch was unreachable dead code.
        return PendingIntent.getForegroundService(context, requestCode, intent, flags)
    }

    private fun loadAlbumArtBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            // Batch 402: pre-Q decodeStream() fallback removed — minSdk 31 guarantees API>=29
            // on every installable device, so that branch was unreachable dead code.
            context.contentResolver.loadThumbnail(uri, android.util.Size(200, 200), null)
        } catch (e: Exception) {
            // Widget jatuh balik ke ikon aplikasi kalau ini gagal (masih terlihat wajar di layar
            // beranda), tapi dicatat supaya kegagalan muat artwork tidak sepenuhnya tak terlihat.
            AppLogger.e("WidgetUpdater", "Gagal muat artwork widget", e)
            null
        }
    }

    /**
     * Jalur "daun diagonal" persegi [size]: sudut kiri-atas & kanan-bawah besar, kanan-atas & kiri-bawah kecil.
     * [inset] mengecilkan jalur ke dalam (radius ikut dikurangi) — dipakai fillet; `internal` karena bubble
     * (FloatingBubbleService) memakai bentuk yang SAMA untuk clip sampul & tab minimized. Batch 584.
     */
    internal fun leafPath(size: Float, inset: Float = 0f): Path {
        val big = (size * LEAF_BIG_FRACTION - inset).coerceAtLeast(0f)
        val small = (size * LEAF_SMALL_FRACTION - inset).coerceAtLeast(0f)
        return Path().apply {
            addRoundRect(
                RectF(inset, inset, size - inset, size - inset),
                floatArrayOf(big, big, small, small, big, big, small, small),
                Path.Direction.CW
            )
        }
    }

    /**
     * Center-crops to a square, clips it to the leaf shape ([leafPath]) and draws a thin diagonal gilt fillet on
     * the edge — the widget counterpart of `Modifier.neuArtFillet` (Batch 569, TactileDepth.kt). Pengganti
     * `roundBitmapCorners` (radius tetap 24px, tanpa fillet) — Batch 583; bentuk daun diagonal — Batch 584.
     * Fillet digambar DI DALAM batas bitmap (jalur diinset setengah lebar stroke) supaya seluruh lebarnya terlihat;
     * gradien 3-stop diagonal kiri-atas -> kanan-bawah = arah cahaya Neumorphism. `internal`: bubble memakainya
     * untuk sampul pill (selalu gelap -> [FILLET_COLOR_DARK]).
     */
    internal fun buildArtPlate(source: Bitmap, filletColor: Int): Bitmap {
        val size = minOf(source.width, source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2
        val squared = Bitmap.createBitmap(source, x, y, size, size)

        val sizeF = size.toFloat()
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawPath(leafPath(sizeF), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squared, 0f, 0f, paint)

        val filletWidth = sizeF * ART_FILLET_STROKE_FRACTION
        val fillet = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = filletWidth
            shader = LinearGradient(
                0f, 0f, sizeF, sizeF,
                intArrayOf(
                    withAlpha(filletColor, FILLET_ALPHA_START),
                    withAlpha(filletColor, FILLET_ALPHA_MID),
                    withAlpha(filletColor, FILLET_ALPHA_END)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(leafPath(sizeF, inset = filletWidth / 2f), fillet)
        return output
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        (color and 0x00FFFFFF) or ((alpha * 255f).toInt() shl 24)

    /**
     * Salinan ikon [resId] (PNG ivory) yang diwarnai [color] (SRC_IN: alfa asli dipertahankan, RGB diganti).
     * Dipakai ikon prev/next mode terang. Gagal = null -> layout tetap memakai PNG asli (tak terbaca di
     * perkamen tapi widget tidak rusak). Bitmap kecil (72px basis, di-skala densitas), jauh di bawah batas RemoteViews.
     */
    private fun tintedGlyph(context: Context, resId: Int, color: Int): Bitmap? = try {
        val source = BitmapFactory.decodeResource(context.resources, resId)
        if (source == null) {
            null
        } else {
            val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
            Canvas(output).drawBitmap(source, 0f, 0f, paint)
            output
        }
    } catch (e: Exception) {
        AppLogger.e("WidgetUpdater", "Gagal mewarnai ikon widget", e)
        null
    }
}
