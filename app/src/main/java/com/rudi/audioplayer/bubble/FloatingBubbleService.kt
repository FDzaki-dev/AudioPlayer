package com.rudi.audioplayer.bubble

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
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
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.app.NotificationCompat
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 * **Batch 98 — jadi foreground service beneran**: sebelumnya (Batch 95) BUKAN foreground —
 * cuma mengandalkan window overlay yang tampil menaikkan importance proses "mendekati visible",
 * dan skin Android agresif tetap bisa membunuhnya kapan saja (dicatat sebagai "batasan jujur").
 * Sekarang `startForeground()` dipanggil beneran (tipe `specialUse`, API 34+ belum punya
 * kategori resmi utk "overlay window") — trade-off sadar: 1 notifikasi importance MIN ekstra
 * selama bubble aktif (nyaris tidak kelihatan — MIN disembunyikan dari status bar, cuma muncul
 * kalau notification shade ditarik turun), demi kepastian bubble TIDAK dibunuh OS selama masih
 * dianggap app aktif, sama level proteksi seperti [PlaybackService]. Restart setelah reboot HP
 * ditangani [BubbleBootReceiver], bukan di sini.
 *
 * **Batch 98 — state antrean kosong**: sebelumnya tombol play/prev/next tetap "aktif" walau
 * tidak ada lagu dimuat sama sekali (tap play = no-op senyap yang membingungkan). Sekarang
 * [hasQueue] dicek tiap update — kalau kosong, tombol jadi setengah transparan dan tap-nya
 * membuka app alih-alih coba mainkan apa pun.
 *
 * **Batch 98 — rotasi layar**: posisi bubble di-clamp ulang di [onConfigurationChanged] —
 * sebelumnya rotasi bisa membuat bubble kepental separuh di luar layar (mis. y besar di
 * portrait jadi melebihi tinggi layar landscape yang lebih pendek) sampai user drag manual.
 *
 * **Batch 100 — minimize ke tepi layar (chat-head style)**: 3 celah dari instruksi lanjutan
 * user ("tombol close/foreground service", "trigger tanpa buka app", "wajib bisa di-minimize,
 * bukan di-close total") — Batch 98 sudah menuntaskan foreground service, tapi bagian
 * minimize-nya waktu itu SALAH DIBACA sebagai "tombol dismiss/close" dan sengaja ditolak
 * ("Di luar cakupan" di CHANGELOG Batch 98). Instruksi aslinya jelas beda: minimize BUKAN
 * dismiss — Service/notifikasi TETAP hidup, cuma tampilan pill-nya yang menciut jadi tab
 * bundar kecil nempel tepi layar, tap lagi untuk buka penuh. Koreksi keputusan itu di sini.
 *
 * Implementasi: [bubbleView] sekarang [FrameLayout] berisi 2 child sekaligus (`bubble_mini_
 * player.xml` pill penuh + `bubble_minimized.xml` tab 48dp), cuma salah satu yang `VISIBLE`
 * (yang lain `GONE`) — window `WRAP_CONTENT` otomatis menciut/membesar ikut ukuran child yang
 * kelihatan, TANPA perlu remove+re-add view/window terpisah tiap toggle. [setupDrag]'s
 * pembeda tap-vs-drag (lihat KDoc-nya) dipakai ulang apa adanya untuk kedua state — tap di tab
 * minimized memanggil [expand] alih-alih [openApp], drag+lepas saat minimized memicu
 * [snapMinimizedToNearestEdge] alih-alih cuma simpan posisi bebas seperti pill penuh.
 *
 * **Batch 97 — artwork decode dipindah ke background thread**: `refreshBubbleContent()` dulu
 * memanggil `loadAlbumArtBitmap()` (I/O blocking — `contentResolver.loadThumbnail()` atau
 * `MediaMetadataRetriever`) langsung di `Player.Listener.onEvents()`, yang jalan di main thread
 * — root cause class yang SAMA PERSIS dengan widget jank Batch 34/35 ("decode bitmap sinkron di
 * main thread tiap ganti lagu"), tapi dampaknya lebih parah di sini: overlay ini digambar di
 * atas SELURUH app lain, jadi tiap ganti lagu berisiko nge-jank UI thread app manapun yang
 * sedang dibuka user, bukan cuma UI AudioPlayer sendiri. Fix: `bubbleScope.launch { ... }` +
 * `withContext(Dispatchers.IO)` untuk decode, `bubbleArtJob?.cancel()` sebelum tiap relaunch
 * (pola identik `widgetUpdateJob` di `PlaybackService.kt` — skip/next cepat berturut-turut tidak
 * boleh bikin hasil decode lama landing belakangan menimpa art lagu yang lebih baru).
 */
class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleStore: FloatingBubbleStore
    private var bubbleView: View? = null
    // Batch 100 — child dari bubbleView (FrameLayout), disimpan terpisah supaya minimize()/
    // expand() tidak perlu findViewById ulang tiap toggle.
    private var expandedView: View? = null
    private var minimizedView: View? = null
    private var isMinimized = false
    // Posisi X terakhir SEBELUM diminimize, dipulihkan saat expand() lagi — murni in-memory
    // (tidak perlu persist terpisah dari FloatingBubbleStore.savePosition biasa: kalau Service
    // mati total lalu restart, posisi tersimpan yang dibaca ulang toh sudah posisi APAPUN state
    // terakhir, expanded atau minimized, cukup akurat untuk titik awal).
    private var lastExpandedX: Int? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val bubbleScope = CoroutineScope(Dispatchers.Main + Job())
    private var bubbleArtJob: Job? = null

    // Optimistic default TRUE — sebelum controller sempat konek, tap tombol tetap harus jatuh
    // ke fallback Intent lama (lihat sendPlaybackAction), bukan langsung dianggap "kosong".
    // Baru di-set FALSE kalau controller SUDAH konek dan benar-benar mengonfirmasi antrean 0.
    private var hasQueue = true

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
        startForegroundWithNotification()
        addBubbleView()
        connectController()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val view = bubbleView ?: return
        val params = layoutParams ?: return
        // Batch 100 — kalau lagi minimized, X SELALU harus tetap di tepi 0/maxX (bukan cuma
        // di-clamp masuk batas layar baru) — re-snap penuh, bukan clamp biasa yang bisa saja
        // menyisakan X "nyaris tepi tapi bukan tepi" pas rotasi mengubah lebar layar.
        if (isMinimized) {
            snapMinimizedToNearestEdge()
            return
        }
        val metrics = resources.displayMetrics
        val maxX = (metrics.widthPixels - view.width).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - view.height).coerceAtLeast(0)
        val clampedX = params.x.coerceIn(0, maxX)
        val clampedY = params.y.coerceIn(0, maxY)
        if (clampedX != params.x || clampedY != params.y) {
            params.x = clampedX
            params.y = clampedY
            runCatching { windowManager.updateViewLayout(view, params) }
            bubbleStore.savePosition(params.x, params.y)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        bubbleScope.cancel() // batalkan bubbleArtJob yang mungkin masih in-flight sekalian
        bubbleView?.let { view -> runCatching { windowManager.removeView(view) } }
        bubbleView = null
    }

    /** Foreground promotion (Batch 98) — lihat catatan trade-off importance MIN di KDoc kelas
     * ini. Ikon & channel-creation-guard meniru persis pola `PlaybackService.
     * startForegroundColdStartNotification()` untuk konsistensi gaya di seluruh proyek. */
    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        "Mini Player Mengambang",
                        NotificationManager.IMPORTANCE_MIN
                    )
                )
            }
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, 102, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Mini Player Mengambang aktif")
            .setContentText("Ketuk untuk buka SONIX. Matikan lewat Settings kalau tidak dibutuhkan.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
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
        // Batch 100 — container tunggal berisi KEDUA tampilan (pill penuh + tab minimized)
        // sekaligus, cuma salah satunya VISIBLE. 1 window WindowManager saja untuk keduanya:
        // toggle visibility, bukan remove+re-add view/window tiap minimize/expand — lebih
        // sederhana & tanpa risiko flicker/race dibanding gonta-ganti window.
        val container = FrameLayout(this)
        val expanded = LayoutInflater.from(this).inflate(R.layout.bubble_mini_player, container, false)
        val minimized = LayoutInflater.from(this).inflate(R.layout.bubble_minimized, container, false)
        container.addView(expanded)
        container.addView(minimized)
        bubbleView = container
        expandedView = expanded
        minimizedView = minimized

        applyOvalClip(expanded.findViewById(R.id.bubble_album_art))
        applyOvalClip(minimized.findViewById(R.id.bubble_minimized_art))
        expanded.findViewById<ImageView>(R.id.bubble_album_art).setImageResource(R.mipmap.ic_launcher)
        minimized.findViewById<ImageView>(R.id.bubble_minimized_art).setImageResource(R.mipmap.ic_launcher)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        isMinimized = bubbleStore.isMinimized()
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
        layoutParams = params

        expanded.visibility = if (isMinimized) View.GONE else View.VISIBLE
        minimized.visibility = if (isMinimized) View.VISIBLE else View.GONE
        if (!isMinimized) lastExpandedX = params.x

        setupDrag(container, params)
        setupControls(expanded)

        runCatching { windowManager.addView(container, params) }
            .onFailure { AppLogger.e("FloatingBubbleService", "Gagal memasang overlay bubble", it) }

        // Sesi sebelumnya diakhiri dalam keadaan minimized — posisi tersimpan mungkin bukan
        // posisi tepi yang valid lagi (mis. rotasi/resolusi beda sejak terakhir disimpan).
        // Snap ulang begitu container ke-layout, konsisten sama kondisi minimize() manapun.
        if (isMinimized) snapMinimizedToNearestEdge()
    }

    private fun applyOvalClip(imageView: ImageView) {
        imageView.clipToOutline = true
        imageView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                outline.setOval(0, 0, v.width, v.height)
            }
        }
    }

    /** Drag-untuk-pindah + tap-untuk-buka-app di area kosong pill, dibedakan lewat TOTAL jarak
     * gerak (bukan cuma delta awal-akhir, supaya jari gemetar kecil tidak salah dianggap drag).
     * Tombol play/pause/prev/next tetap dapat event klik normal — ImageButton clickable
     * mengonsumsi ACTION_DOWN duluan sebelum sempat ke OnTouchListener root ini, jadi drag/tap
     * di sini otomatis cuma aktif di luar area ke-3 tombol tanpa perlu logic pemisah manual.
     * Batch 98: DisplayMetrics dibaca ULANG tiap ACTION_MOVE (bukan di-cache sekali di awal
     * seperti sebelumnya) — device bisa saja rotasi PAS lagi di-drag, metrics yang di-cache di
     * awal akan basi. */
    private fun setupDrag(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var totalMovement = 0f

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
                        val metrics = resources.displayMetrics
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
                        // Batch 100 — mode minimized SELALU "nempel" tepi terdekat begitu jari
                        // dilepas, tidak boleh mengambang bebas di tengah layar seperti pill
                        // penuh (itu yang membedakan visual "minimized" dari "expanded biasa").
                        if (isMinimized) snapMinimizedToNearestEdge()
                    } else if (isMinimized) {
                        expand()
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
            if (hasQueue) sendPlaybackAction(WidgetUpdater.ACTION_TOGGLE_PLAY) else openApp()
        }
        view.findViewById<ImageButton>(R.id.bubble_prev).setOnClickListener {
            if (hasQueue) sendPlaybackAction(WidgetUpdater.ACTION_PREVIOUS) else openApp()
        }
        view.findViewById<ImageButton>(R.id.bubble_next).setOnClickListener {
            if (hasQueue) sendPlaybackAction(WidgetUpdater.ACTION_NEXT) else openApp()
        }
        view.findViewById<ImageButton>(R.id.bubble_minimize).setOnClickListener { minimize() }
    }

    /** Ciutkan pill penuh jadi tab 48dp nempel tepi layar. Service/notifikasi foreground TIDAK
     * disentuh — cuma toggle visibility 2 child dalam [bubbleView] yang sama, lihat KDoc "Batch
     * 100" di kelas ini untuk kenapa ini BUKAN tombol close/dismiss. */
    private fun minimize() {
        if (isMinimized) return
        val params = layoutParams ?: return
        isMinimized = true
        lastExpandedX = params.x
        expandedView?.visibility = View.GONE
        minimizedView?.visibility = View.VISIBLE
        bubbleStore.setMinimized(true)
        snapMinimizedToNearestEdge()
    }

    /** Kebalikan [minimize] — dipanggil dari tap (bukan drag) di atas tab minimized (lihat
     * [setupDrag]). X dipulihkan ke posisi SEBELUM diminimize ([lastExpandedX]), di-clamp ULANG
     * terhadap lebar pill penuh yang baru saja terlihat lagi (`container.post{}` menunggu satu
     * layout pass supaya `container.width` yang dibaca adalah ukuran pill, bukan sisa ukuran
     * tab 48dp dari frame sebelumnya). */
    private fun expand() {
        if (!isMinimized) return
        val container = bubbleView as? FrameLayout ?: return
        val params = layoutParams ?: return
        isMinimized = false
        minimizedView?.visibility = View.GONE
        expandedView?.visibility = View.VISIBLE
        bubbleStore.setMinimized(false)
        container.post {
            val maxX = (resources.displayMetrics.widthPixels - container.width).coerceAtLeast(0)
            params.x = (lastExpandedX ?: params.x).coerceIn(0, maxX)
            runCatching { windowManager.updateViewLayout(container, params) }
            bubbleStore.savePosition(params.x, params.y)
        }
    }

    /** Chat-head-style "nempel tepi": X dipaksa ke 0 (kiri) atau `screenWidth - lebarTab`
     * (kanan) — mana pun yang lebih dekat dari posisi X saat ini, TIDAK PERNAH mengambang bebas
     * di tengah layar selagi minimized. `container.post{}` supaya ukuran tab yang SEBENARNYA
     * (dari `layout_width="48dp"` di bubble_minimized.xml, sudah ke-measure oleh sistem) yang
     * dipakai hitung tepi kanan — bukan angka dp ditebak manual dari kode, yang gampang meleset
     * kalau ukuran layout diubah lagi nanti dan lupa disinkronkan ke sini. */
    private fun snapMinimizedToNearestEdge() {
        val container = bubbleView as? FrameLayout ?: return
        val params = layoutParams ?: return
        container.post {
            val width = container.width.takeIf { it > 0 } ?: return@post
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val nearestRight = (params.x + width / 2) > screenWidth / 2
            params.x = if (nearestRight) (screenWidth - width).coerceAtLeast(0) else 0
            // Y juga di-clamp (bukan cuma X yang "dipaksa tepi") — rotasi bisa mengubah tinggi
            // layar juga, Y lama yang valid di orientasi sebelumnya bisa jadi melebihi batas.
            val maxY = (metrics.heightPixels - container.height).coerceAtLeast(0)
            params.y = params.y.coerceIn(0, maxY)
            runCatching { windowManager.updateViewLayout(container, params) }
            bubbleStore.savePosition(params.x, params.y)
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
        hasQueue = player.mediaItemCount > 0

        val playPause = view.findViewById<ImageButton>(R.id.bubble_play_pause)
        val prev = view.findViewById<ImageButton>(R.id.bubble_prev)
        val next = view.findViewById<ImageButton>(R.id.bubble_next)
        playPause.setImageResource(if (player.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        // Batch 98 — indikasi visual antrean kosong: tombol tetap kelihatan (bukan disembunyikan
        // total, biar bentuk pill tidak "loncat" ukuran) tapi setengah transparan, dan tap-nya
        // membuka app alih-alih coba mainkan apa pun (lihat setupControls).
        val alpha = if (hasQueue) 1f else 0.4f
        playPause.alpha = alpha
        prev.alpha = alpha
        next.alpha = alpha

        // Play/pause icon di atas murni ganti drawable resource — murah, aman tetap sync. Cuma
        // decode artwork (I/O blocking) yang wajib pindah background thread, lihat catatan
        // "Batch 97" di kelas ini. Batch 100: art di-set ke KEDUA ImageView (pill penuh +
        // tab minimized) sekaligus, biar yang lagi disembunyikan pun tetap sudah sinkron begitu
        // user expand() nanti — bukan nunggu event lagu berganti lagi baru ke-update.
        val artworkUri = player.currentMediaItem?.mediaMetadata?.artworkUri
        bubbleArtJob?.cancel()
        if (artworkUri == null) {
            view.findViewById<ImageView>(R.id.bubble_album_art).setImageResource(R.mipmap.ic_launcher)
            view.findViewById<ImageView>(R.id.bubble_minimized_art).setImageResource(R.mipmap.ic_launcher)
            return
        }
        bubbleArtJob = bubbleScope.launch {
            val bitmap = withContext(Dispatchers.IO) { loadAlbumArtBitmap(artworkUri) }
            // bubbleView bisa saja sudah null (Service di-destroy selagi decode jalan) — re-cek,
            // jangan pakai `view` closure lama yang mungkin sudah dilepas dari WindowManager.
            val root = bubbleView ?: return@launch
            val expandedArt = root.findViewById<ImageView>(R.id.bubble_album_art)
            val minimizedArt = root.findViewById<ImageView>(R.id.bubble_minimized_art)
            if (bitmap != null) {
                expandedArt.setImageBitmap(bitmap)
                minimizedArt.setImageBitmap(bitmap)
            } else {
                expandedArt.setImageResource(R.mipmap.ic_launcher)
                minimizedArt.setImageResource(R.mipmap.ic_launcher)
            }
        }
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
        private const val NOTIFICATION_CHANNEL_ID = "floating_bubble"
        private const val NOTIFICATION_ID = 7002 // beda dari COLD_START_NOTIFICATION_ID (7001)
    }
}
