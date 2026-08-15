package com.rudi.audioplayer.bubble

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Outline
import android.graphics.PixelFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.rudi.audioplayer.MainActivity
import com.rudi.audioplayer.R
import com.rudi.audioplayer.data.FloatingBubbleStore
import com.rudi.audioplayer.playback.PlaybackService
import com.rudi.audioplayer.util.AppLogger
import com.rudi.audioplayer.widget.WidgetUpdater
import java.util.concurrent.Executor
import kotlin.math.abs

/**
 * Roadmap #11 — mini player mengambang di atas app apa pun, lewat izin sensitif
 * `SYSTEM_ALERT_WINDOW` (`Settings.ACTION_MANAGE_OVERLAY_PERMISSION`). Start/stop murni
 * dikontrol dari toggle di SettingsScreen (lihat MainActivity: overlayPermissionLauncher +
 * toggleFloatingBubble) — service ini TIDAK PERNAH menyalakan dirinya sendiri, sama filosofi
 * opt-in ShakeDetector, tapi untuk izin yang jauh lebih sensitif/terlihat.
 *
 * **Sengaja plain Android View, bukan Compose**: ComposeView yang dipasang di luar Activity
 * butuh LifecycleOwner/SavedStateRegistryOwner rakitan sendiri (ViewTreeLifecycleOwner.set()
 * dkk) sebelum Compose mau nempel — kompleksitas nyata untuk pil 3-tombol tanpa scroll/animasi
 * rumit. `bubble_mini_player.xml` reuse drawable widget apa adanya (widget_background.xml,
 * widget_play_button_bg.xml, ic_widget_*.png) — identitas visual otomatis konsisten sama
 * widget, 0 asset baru.
 *
 * **Kontrol/state**: [MediaController] asli (pola sama `PlayerViewModel.connect()`) untuk
 * update LIVE play/pause/art lewat `Player.Listener` — bukan polling. Tap tombol pakai
 * controller langsung kalau sudah konek; `WidgetUpdater.ACTION_TOGGLE_PLAY/NEXT/PREVIOUS` ke
 * `PlaybackService` (kontrak Intent yang SAMA dipakai widget) jadi fallback kalau controller
 * belum sempat konek — tidak ada action constant baru yang perlu ditambah.
 *
 * **Touch pass-through**: window overlay di-`WRAP_CONTENT` (bukan `MATCH_PARENT`) + tanpa flag
 * fullscreen — area di luar pill 100% tembus ke app di bawahnya secara struktural, bukan
 * sesuatu yang perlu ditangani manual per-event.
 *
 * **Batasan jujur** (skin Android agresif membunuh proses background — lihat catatan OEM yang
 * sama di README § Keputusan Arsitektur): service ini BUKAN foreground service (window overlay
 * yang sedang tampil sudah menaikkan importance proses mendekati "visible" selama ada di layar,
 * cukup untuk kebanyakan device), tapi tidak ada jaminan 100% di skin yang sangat agresif —
 * keterbatasan platform yang sama seperti widget, bukan sesuatu yang bisa dijamin dari kode
 * manapun.
 */
class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleStore: FloatingBubbleStore
    private var bubbleView: View? = null
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_MEDIA_METADATA_CHANGED
                )
            ) {
                refreshBubbleContent(player)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        bubbleStore = FloatingBubbleStore(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        addBubbleView()
        connectController()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        bubbleView?.let { view -> runCatching { windowManager.removeView(view) } }
        bubbleView = null
    }

    private fun connectController() {
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(playerListener)
            refreshBubbleContent(c)
        }, Executor { it.run() }) // same-thread executor, pola identik PlayerViewModel.connect()
    }

    private fun addBubbleView() {
        val view = LayoutInflater.from(this).inflate(R.layout.bubble_mini_player, null)
        bubbleView = view

        val albumArt = view.findViewById<ImageView>(R.id.bubble_album_art)
        albumArt.clipToOutline = true
        albumArt.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                outline.setOval(0, 0, v.width, v.height)
            }
        }
        albumArt.setImageResource(R.mipmap.ic_launcher)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val saved = bubbleStore.getPosition()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = saved?.first ?: 0
            y = saved?.second ?: 200
        }

        setupDrag(view, params)
        setupControls(view)

        runCatching { windowManager.addView(view, params) }
            .onFailure { AppLogger.e("FloatingBubbleService", "Gagal memasang overlay bubble", it) }
    }

    /** Drag-untuk-pindah + tap-untuk-buka-app di area kosong pill, dibedakan lewat TOTAL jarak
     * gerak (bukan cuma delta awal-akhir, supaya jari gemetar kecil tidak salah dianggap drag).
     * Tombol play/pause/prev/next tetap dapat event klik normal — ImageButton clickable
     * mengonsumsi ACTION_DOWN duluan sebelum sempat ke OnTouchListener root ini, jadi drag/tap
     * di sini otomatis cuma aktif di luar area ke-3 tombol tanpa perlu logic pemisah manual. */
    private fun setupDrag(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var totalMovement = 0f
        val metrics = resources.displayMetrics

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    totalMovement = 0f
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    totalMovement += abs(dx) + abs(dy)
                    if (totalMovement > TOUCH_SLOP) {
                        val maxX = (metrics.widthPixels - v.width).coerceAtLeast(0)
                        val maxY = (metrics.heightPixels - v.height).coerceAtLeast(0)
                        params.x = (initialX + dx.toInt()).coerceIn(0, maxX)
                        params.y = (initialY + dy.toInt()).coerceIn(0, maxY)
                        runCatching { windowManager.updateViewLayout(v, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (totalMovement > TOUCH_SLOP) {
                        bubbleStore.savePosition(params.x, params.y)
                    } else {
                        openApp()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupControls(view: View) {
        view.findViewById<ImageButton>(R.id.bubble_play_pause).setOnClickListener {
            sendPlaybackAction(WidgetUpdater.ACTION_TOGGLE_PLAY)
        }
        view.findViewById<ImageButton>(R.id.bubble_prev).setOnClickListener {
            sendPlaybackAction(WidgetUpdater.ACTION_PREVIOUS)
        }
        view.findViewById<ImageButton>(R.id.bubble_next).setOnClickListener {
            sendPlaybackAction(WidgetUpdater.ACTION_NEXT)
        }
    }

    private fun sendPlaybackAction(action: String) {
        val c = controller
        when {
            c != null && action == WidgetUpdater.ACTION_TOGGLE_PLAY -> if (c.isPlaying) c.pause() else c.play()
            c != null && action == WidgetUpdater.ACTION_NEXT -> c.seekToNextMediaItem()
            c != null && action == WidgetUpdater.ACTION_PREVIOUS -> c.seekToPreviousMediaItem()
            else -> {
                // Fallback: controller belum konek, pakai kontrak Intent yang sama widget pakai.
                val intent = Intent(this, PlaybackService::class.java).setAction(action)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            }
        }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun refreshBubbleContent(player: Player) {
        val view = bubbleView ?: return
        view.findViewById<ImageButton>(R.id.bubble_play_pause).setImageResource(
            if (player.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )

        val albumArt = view.findViewById<ImageView>(R.id.bubble_album_art)
        val artworkUri = player.currentMediaItem?.mediaMetadata?.artworkUri
        val bitmap = artworkUri?.let { loadAlbumArtBitmap(it) }
        if (bitmap != null) albumArt.setImageBitmap(bitmap) else albumArt.setImageResource(R.mipmap.ic_launcher)
    }

    /** Sama persis pendekatan AudioArtFetcher/WidgetUpdater — loadThumbnail() langsung di URI
     * lagu itu sendiri (bukan decode byte mentah, lihat catatan Batch 68 di AudioArtFetcher.kt
     * kenapa pendekatan lain pernah gagal total di sini). */
    private fun loadAlbumArtBitmap(uri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.loadThumbnail(uri, Size(120, 120), null)
        } else {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, uri)
                retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            } finally {
                retriever.release()
            }
        }
    } catch (e: Exception) {
        AppLogger.e("FloatingBubbleService", "Gagal muat artwork bubble", e)
        null
    }

    companion object {
        private const val TOUCH_SLOP = 12f
    }
}
