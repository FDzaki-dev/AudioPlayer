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
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Rect
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
import kotlinx.coroutines.delay
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
 *
 * **Batch 453 — auto-fade saat idle**: instruksi eksplisit user — pill/tab dibiarkan diam di atas
 * konten app lain (opaque penuh) dirasa terlalu menutupi, minta "bisa split/di-minimize total,
 * atau minimal fade out saat tidak digeser". [minimize] ke tepi layar (Batch 100) SUDAH ada
 * sebagai mekanisme "total" (manual, lewat tombol chevron) — celah yang belum ada adalah kondisi
 * IDLE tanpa aksi user sama sekali. Fix: [keepAwakeAndScheduleFade] meredupkan alpha
 * [bubbleView] (container, bukan per-child — otomatis ikut kena baik pill penuh MAUPUN tab
 * minimized, mana pun yang sedang terlihat) ke [IDLE_FADE_ALPHA] setelah [IDLE_FADE_DELAY_MS]
 * tanpa sentuhan, dan mengembalikannya ke opaque penuh SEKETIKA di setiap awal interaksi baru
 * (sentuh/drag di [setupDrag], atau tap tombol kontrol/minimize di [setupControls]) — bubble
 * TIDAK PERNAH pudar selagi benar-benar sedang dipakai/digeser, cuma saat benar-benar dibiarkan
 * diam. Timer pakai [bubbleScope] yang SUDAH ada (bukan Handler/Thread baru) supaya otomatis ikut
 * ter-cancel oleh `bubbleScope.cancel()` di [onDestroy] — 0 leak, 0 dependency baru selain 1
 * import `kotlinx.coroutines.delay` (satu paket dengan `launch`/`withContext` yang sudah dipakai).
 * Alpha window overlay tidak mengubah keterjangkauan sentuh (`FLAG_NOT_FOCUSABLE` tidak terkait
 * alpha) — tap pada bubble yang lagi pudar tetap normal, jadi ini murni sinyal visual "idle".
 *
 * **Batch 454 — auto-minimize total saat idle berkepanjangan**: instruksi asli user (dikutip di
 * catatan Batch 453) minta "wajib bisa split/di-minimize total, ATAU minimal dulu bisa fade out"
 * — Batch 453 baru menuntaskan fallback minimumnya (fade). Fix ini menuntaskan mandat utamanya:
 * kalau bubble TETAP idle lebih lama lagi setelah fade ([IDLE_AUTO_MINIMIZE_DELAY_MS] dihitung
 * dari titik interaksi terakhir yang sama dgn timer fade, BUKAN ditambah setelah fade selesai),
 * [minimize] ke tab tepi layar dipanggil OTOMATIS — 0 logic collapse baru, reuse fungsi [minimize]
 * yang sudah ada sejak Batch 100 apa adanya (termasuk guard `if (isMinimized) return` di
 * dalamnya, jadi aman dipanggil berulang tanpa efek samping kalau user sempat minimize manual
 * duluan). Timer kedua ([idleMinimizeJob]) dijadwalkan/dibatalkan di titik yang SAMA PERSIS
 * dengan [idleFadeJob] di [keepAwakeAndScheduleFade] — 1 titik kontrol utk kedua timer, 0
 * Handler/Thread baru, otomatis ikut ter-cancel oleh `bubbleScope.cancel()` di [onDestroy] yang
 * sudah ada. Alpha container TIDAK direset ke opaque saat auto-minimize terjadi (bubble sudah
 * pudar dari fade sebelumnya, tab hasil minimize mewarisi alpha yang sama — konsisten dengan
 * desain "1 titik kontrol alpha di container" Batch 453, 0 percabangan state baru). Sentuhan/tap
 * apa pun (drag maupun tombol kontrol) tetap membatalkan KEDUA timer via pemanggilan
 * [keepAwakeAndScheduleFade] yang sudah ada di [setupDrag]/[setupControls] — 0 perubahan di
 * kedua fungsi itu.
 *
 * **Batch 455 — tab minimized kliping SETENGAH di tepi layar**: feedback eksplisit user setelah
 * Batch 454 ("minimize otomatis nya berhasil, TAPI yang benar-benar diinginkan: circle bubble
 * bisa kliping setengah/menyisakan mini trigger, wajib mentok maksimal ke tepi layar saat idle").
 * Tab 48dp bundar (Batch 100) sebelumnya berhenti flush-tapi-100%-kelihatan di X=0/`screenWidth -
 * lebarTab`. Sekarang [snapMinimizedToNearestEdge] mendorong X SETENGAH lebar tab melewati batas
 * layar (`-lebarTab/2` kiri, `screenWidth - lebarTab/2` kanan) — window overlay SUDAH
 * `FLAG_LAYOUT_NO_LIMITS` sejak Batch 100 ([addBubbleView]), jadi 0 flag/permission baru,
 * WindowManager & sistem yang otomatis memotong render di luar layar. **0 fungsi/state/timer
 * baru** — cuma formula X di 1 titik kontrol yang sudah ada, otomatis berlaku ke semua pemicu
 * snap yang sudah ada (lepas-drag minimized, auto-minimize Batch 454, tombol chevron manual,
 * restart service, rotasi). Selagi masih di-drag aktif tab tetap dibatasi penuh di dalam layar
 * ([setupDrag] tidak disentuh) — setengah-tersembunyi HANYA muncul begitu benar-benar idle/diam
 * di tepi, sesuai kata "saat idle" di instruksi user.
 *
 * **Batch 456 — kurangi fraksi clip tepi [SALAH ARAH, DIREVERT Batch 457]**: fraksi
 * disembunyikan dikecilkan 50%→30%, TAPI ini justru MEMPERBESAR bagian tab yang kelihatan
 * (`hiddenWidth` turun → sisa kelihatan naik) — kebalikan dari maksud user, hasilnya tab makin
 * "timbul" bukan makin ke-clip. Formula generalisasi (`hiddenWidth = width * EDGE_CLIP_FRACTION`)
 * DIPERTAHANKAN (regresi-aman, netral arah), cuma nilai konstantanya yang salah.
 *
 * **Batch 457 — revert fraksi clip ke Batch 455**: feedback eksplisit user ("bukannya hilangin
 * yang timbul malah dibikin tambah timbul"). [EDGE_CLIP_FRACTION] dikembalikan 30%→50% (nilai
 * awal Batch 455) — satu-satunya perubahan, 0 formula/fungsi/titik panggil baru disentuh. Hasil
 * setelah revert identik matematis dengan Batch 455 (tab minimized separuh lebar tersembunyi di
 * luar layar, separuh kelihatan sebagai mini trigger).
 *
 * **Batch 458 — kurangi timbul lebih jauh dari Batch 455/457**: user minta "~30%" — DIKONFIRMASI
 * via pilihan tap (bukan ditebak) bahwa ini merujuk ke bagian TIMBUL (kelihatan), bukan ke fraksi
 * klip itu sendiri, supaya tidak mengulang kesalahan arah Batch 456. [EDGE_CLIP_FRACTION]
 * dinaikkan 50%→70% (fraksi SEMBUNYI), hasilnya bagian kelihatan turun jadi ~30%. 0
 * formula/fungsi/titik panggil baru — cuma nilai konstanta.
 *
 * **Batch 460 — touch target independen dari visual, + fix kliping landscape**: 2 instruksi
 * eksplisit user, 2 file (`bubble_minimized.xml` + file ini). (1) Konfirmasi device fisik Batch
 * 458 mencatat mini trigger "sedikit lebih susah" di-tap seiring bagian TIMBUL mengecil — opsi
 * yang sudah dicatat PROJECT_STATE.md ("perbesar touch target independen dari lebar visual")
 * dieksekusi sekarang: `bubble_minimized.xml` root DILEBARKAN jadi murni area-sentuh (48dp→88dp),
 * visual bulat asli DIPINDAH ke child baru `R.id.bubble_minimized_visual` (tetap 48dp, gravity
 * CENTER) — area ekstra 100% transparan (0 background), murni menambah hit-box tanpa menambah
 * apa pun yang kelihatan. [snapMinimizedToNearestEdge] dipisah jadi 2 lebar (`visualWidth` untuk
 * [EDGE_CLIP_FRACTION], `width`/root untuk posisi X) — `touchPad` (selisih keduanya /2) SELALU
 * ikut ke sisi yang tetap di layar, jadi [EDGE_CLIP_FRACTION] naik = visual makin ngumpet TANPA
 * ikut mengecilkan area sentuh (2 parameter independen). (2) [EDGE_CLIP_FRACTION] dinaikkan lagi
 * 70%→90% — instruksi eksplisit user pakai kata "timbul" ("visual turunkan jadi ~10% yang timbul
 * saja"), konsisten arah konvensi Batch 456→458, 0 klarifikasi tap diperlukan (kata sudah
 * eksplisit, lihat catatan proses PROJECT_STATE.md). (3) `resources.displayMetrics` (4 titik:
 * [onConfigurationChanged], [setupDrag], [expand], [snapMinimizedToNearestEdge]) DIGANTI
 * `windowManager.currentWindowMetrics.bounds` — root cause bubble gagal konsisten mentok ke ujung
 * layar saat landscape: `resources.displayMetrics` di Context Service tidak dijamin ter-refresh
 * SEKETIKA saat [onConfigurationChanged] terpanggil pasca-rotasi (beda dari Activity/
 * WindowContext), `currentWindowMetrics` (API 30+, aman di minSdk 31) selalu bounds window
 * REAL-TIME. 0 breaking change ke formula/state lain, 0 sektor DITUTUP disentuh.
 *
 * **Batch 461 [FIX RESIDUAL] — bounds landscape masih tidak konsisten meski Batch 460**:
 * konfirmasi device fisik user (1,2,4,5 ✅, 3 ❌ "masih nongol" — tab minimized belum SELALU
 * mentok tepi kiri/kanan pas rotasi landscape berulang). Root cause: `windowManager` di kelas ini
 * didapat dari Context Service BIASA (bukan `UiContext`/`WindowContext` seperti Activity) — per
 * dokumentasi resmi `WindowManager#getCurrentWindowMetrics()`, Context non-UI SELALU jatuh ke
 * `getMaximumWindowMetrics()`, hasilnya BUKAN dijamin sinkron atomik persis di momen rotasi (kelas
 * masalah sama dengan `resources.displayMetrics` yang sudah didiagnosis Batch 460 — cuma API-nya
 * beda, root sumbernya sama-sama Context Service). **Fix**: [screenBounds] (field baru, single
 * source of truth) SEKARANG diisi dari `newConfig` (parameter [onConfigurationChanged]) — SATU-
 * SATUNYA sumber yang dijamin sistem fresh PERSIS di momen callback rotasi terpanggil, bukan
 * re-query Context async. 4 titik baca (sama seperti Batch 460: [onConfigurationChanged],
 * [setupDrag], [expand], [snapMinimizedToNearestEdge]) sekarang baca [screenBounds] ter-cache,
 * 0 lagi query `windowManager.currentWindowMetrics` langsung di titik mana pun selain nilai awal
 * (`onCreate`, baseline sebelum rotasi pertama). 0 breaking change ke formula
 * EDGE_CLIP_FRACTION/touchPad/visualWidth Batch 460, 0 sektor DITUTUP disentuh. BELUM
 * diverifikasi device fisik (0 env Android nyata di sesi ini).
 *
 * **Batch 462 [FIX RESIDUAL #2] — Batch 461 GAGAL TOTAL, bukan cuma kurang tepat**: konfirmasi
 * device fisik user: tab minimized 100% KELIHATAN/gak keclip sama sekali di landscape (bukan versi
 * "kurang pas dikit" — total tidak ter-klip). Sinyal ini menggeser diagnosis dari "sumber bounds
 * kurang akurat" (teori Batch 460/461, TERBUKTI SALAH — `screenBounds` Batch 461 sendiri sudah
 * benar dp→px dari `newConfig`) ke **"ada pihak lain menimpa posisi window SETELAH snap kita
 * apply"** — paling mungkin sanitasi/enforcement posisi window oleh sistem selama transisi ANIMASI
 * rotasi (perilaku umum overlay `TYPE_APPLICATION_OVERLAY` lintas OEM, tidak seragam & tidak bisa
 * dipastikan tanpa logcat device asli). **Fix — safety-net re-assert, BUKAN ganti formula/sumber
 * data lagi** (SOP eksplisit: jangan ulang pola "ganti API baca metrics"): [onConfigurationChanged]
 * sekarang re-panggil [snapMinimizedToNearestEdge] 2x tambahan dengan delay 150ms & 400ms
 * (`ROTATION_RESNAP_DELAYS_MS`) SETELAH panggilan immediate yang sudah ada — membracket durasi
 * animasi transisi rotasi tipikal. Idempotent kalau snap pertama sudah benar (re-apply nilai sama,
 * 0 efek kelihatan tambahan), jadi fallback pasti kalau snap pertama sempat ketiban sistem. 0
 * breaking change ke formula EDGE_CLIP_FRACTION/touchPad/visualWidth/screenBounds, 0 sektor
 * DITUTUP disentuh. **CATATAN JUJUR**: ini mitigasi defensif berdasar sinyal device fisik, BUKAN
 * root-cause pasti (0 akses logcat/device fisik di sesi ini) — kalau residual masih muncul lagi
 * setelah batch ini, WAJIB logcat device asli sebelum lanjut tebak lagi (lihat PROJECT_STATE.md).
 *
 * **Batch 463 [PIVOT KE INSTRUMENTASI] — Batch 462 GAGAL LAGI, logcat user 0 sinyal**: user
 * konfirmasi device fisik "masih nongol" pasca Batch 462, DAN membawa `bubble_log.txt` (logcat
 * `-iE "floatingbubble|configurationchanged|windowmanager"`) — dicek baris-per-baris, hasilnya
 * NOL baris dari service ini (2 satu-satunya kecocokan "floatingbubble" adalah ECHO PERINTAH
 * grep-nya sendiri, bukan output). Kesimpulan: service ini TIDAK PERNAH menulis apa pun ke
 * logcat — 3 teori berturut-turut (Batch 460/461/462) semua ditebak murni dari baca-kode +
 * dokumentasi resmi, TANPA data eksekusi nyata. SOP sendiri (PROJECT_STATE.md, "PELAJARAN PROSES
 * Batch 461→462") melarang tebakan fix ke-4 tanpa data baru. **0 formula/logic diubah sama
 * sekali** — batch ini murni menambah [AppLogger].w() (tag `"FloatingBubbleService"`, pola sama
 * [loadAlbumArtBitmap]) di 4 titik: (1) entry [onConfigurationChanged] (orientation+screenBounds+
 * isMinimized), (2) 2 guard null `bubbleView`/`layoutParams` yang sebelumnya return diam-diam,
 * (3) tiap callback [ROTATION_RESNAP_DELAYS_MS] benar tereksekusi, (4) di [snapMinimizedToNearestEdge]:
 * nilai X TARGET tepat sebelum `updateViewLayout` + hasil sukses/gagalnya (`runCatching` lama
 * SEBELUMNYA membungkam exception total, 0 sinyal kalau apply gagal) + **readback posisi NYATA
 * di layar 250ms kemudian** (`container.getLocationOnScreen()`) — satu-satunya cara membuktikan
 * atau membantah teori Batch 462 ("sistem menimpa posisi window pasca-snap") dengan data, bukan
 * dugaan. `AppLogger.w()` (bukan `Log.d` polos) sengaja dipilih — otomatis kepakai ke ATAU
 * logcat ATAU `diagnostic_log.txt` privat app yang bisa diekspor lewat Settings > Lanjutan > Log
 * Diagnostik (0 perlu Termux/adb kalau salah satu kanal gagal lagi, lihat `AppLogger.kt`).
 * Sesi berikutnya: baca log HASIL rotasi nyata dulu (logcat ATAU ekspor Log Diagnostik), BARU
 * putuskan fix ke-4 dari situ — bukan dari teori baru tanpa bukti (lihat PROJECT_STATE.md).
 *
 * **Batch 464 [DATA BARU MENGEJUTKAN — perluas instrumentasi]**: readback +250ms Batch 463
 * (device fisik pertama) menunjukkan mismatch BESAR (target x=1011 vs nyata x=551) SAAT
 * [minimize] BIASA — 0 rotasi terlibat sama sekali (screenWidth tetap 1080/portrait). Ini
 * mengubah arah dugaan: mismatch mungkin BUKAN soal animasi transisi ROTASI (fokus Batch 462),
 * tapi soal window overlay `WRAP_CONTENT` ini resize FISIK tiap toggle expanded↔minimized, dan
 * `width` yang dibaca [snapMinimizedToNearestEdge] via `container.post{}` mungkin representasi
 * View yang sudah di-measure tapi window WindowManager-nya sendiri belum tuntas resize saat
 * `updateViewLayout` dipanggil — teori BARU, belum pernah diuji Batch 460-462. **0 formula/logic
 * diubah lagi** — 2 tambahan MURNI observasi: (1) readback sekarang JUGA log `container.width`/
 * `container.height` NYATA, dibanding ke ukuran saat target dihitung — kalau beda, itu bukti
 * langsung window/view masih resize saat snap di-apply; (2) readback KEDUA ditambah di +800ms
 * (selain +250ms yang sudah ada) — kalau +800ms sudah cocok ke target (beda dari +250ms yang
 * meleset), itu soal SETTLING/animasi sementara; kalau +800ms MASIH meleset sama, itu salah
 * PERMANEN, bukan soal waktu tunggu. Sesi berikutnya: baca PERBANDINGAN kedua readback (250ms vs
 * 800ms) + ukuran nyata vs target SEBELUM memutuskan fix apa pun — 2 hasil berbeda mengarah ke 2
 * kelas fix yang sama sekali berbeda (delay lebih panjang vs re-urutan resize-lalu-posisikan).
 *
 * **Batch 468 [FIX — branch (c) Batch 464 dikonfirmasi jadi kode, berdasar data device Batch
 * 467]**: user jalankan device-test Method A/B (protokol lengkap: PROJECT_STATE.md Batch 467).
 * (A) drag bubble ke tepi ATAS layar (portrait) — GAP KOSONG terlihat sebelum bubble mulai,
 * bubble TIDAK pernah menutupi status bar. (B) screenshot landscape (app lain fullscreen)
 * menunjukkan pola sama + 1 temuan tambahan: bubble minimized landscape tetap mentok PERSIS ke
 * ujung layar TANPA ter-klip sama sekali — gejala IDENTIK dengan kegagalan landscape-edge-clip
 * Batch 460/461/462 yang SEBELUMNYA dianggap bug terpisah & gagal terdiagnosis 3x berturut-turut.
 *
 * Root cause: [addBubbleView] pasang `FLAG_LAYOUT_NO_LIMITS` TANPA `FLAG_LAYOUT_IN_SCREEN`.
 * Tanpa flag itu, per dokumentasi resmi `WindowManager.LayoutParams`, posisi x/y dengan
 * `Gravity.TOP|START` diinterpretasi RELATIF ke content area (exclude status bar & inset sistem
 * lain, beda tiap orientasi/config), sedangkan [screenBounds] (sumber target, sejak Batch
 * 460/461 dari `currentWindowMetrics.bounds`) SELALU full-screen absolut — SAMA seperti
 * `container.getLocationOnScreen()` yang dipakai readback Batch 463. Target dihitung di 1 sistem
 * koordinat, diterapkan window manager di sistem koordinat LAIN — origin mismatch itu sumber
 * SEMUA delta readback Batch 463-466 (99px/66px, cocok kisaran inset status/nav bar), DAN
 * kemungkinan besar JUGA sumber gejala "tidak ter-klip landscape": kalau inset landscape (nav
 * bar sisi) cukup besar, ia bisa mengkompensasi hampir seluruh `hiddenWidth`/[EDGE_CLIP_FRACTION]
 * yang seharusnya menyembunyikan bubble, membuatnya nongol utuh walau formula clip Batch 456-459
 * sendiri BENAR. **Ini hipotesis PENYATUAN 2 gejala yang sebelumnya dianggap terpisah** — kuat
 * didukung data, TAPI belum kepastian mutlak sampai dikonfirmasi device pasca-build.
 *
 * **Fix (1 baris)**: tambah `WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN` ke flags
 * [addBubbleView] (sebelah `FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_NO_LIMITS` yang sudah ada sejak
 * Batch 100). **0 formula lain diubah** — [EDGE_CLIP_FRACTION]/`touchPad`/`visualWidth`/
 * [screenBounds]/`ROTATION_RESNAP_DELAYS_MS` TETAP persis Batch 460-464, tidak disentuh sama
 * sekali (1 variabel diubah per percobaan, supaya efeknya terisolasi & bisa dibaca dari data).
 * Readback instrumentasi Batch 463/464 SENGAJA TIDAK dihapus — dipakai validasi: kalau fix ini
 * benar, delta readback harusnya (0,0) di semua kasus pasca-update.
 *
 * **RISIKO**: flag ini mengubah origin koordinat SELURUH window overlay, bukan cuma snap-to-edge
 * — termasuk posisi awal expand/drag manual & posisi TERSIMPAN (`FloatingBubbleStore.getPosition()`)
 * dari sesi SEBELUM update (dihitung di sistem koordinat lama/salah, mungkin kelihatan "geser"
 * sekali di buka pertama pasca-update — EXPECTED, bukan bug baru, re-snap 1x manual akan
 * memperbaikinya). WAJIB regression-test manual penuh ke SEMUA yang sebelumnya "confirmed"
 * (Batch 98-100/451/453-458/460-463): drag manual, mini trigger, minimize/expand/fade/
 * auto-minimize, DAN khususnya mentok-tepi-landscape (poin yang gagal 3x sebelumnya). **0
 * diverifikasi CI/device Batch 468** — 0 env Android nyata/device fisik/compiler Kotlin di sesi
 * ini. Perlu dari user: install APK baru, ulangi Method A/B + tes khusus landscape-edge-clip,
 * kirim hasil/log lagi.
 *
 * **Batch 469 [REGRESI BARU pasca-468 — instrumentasi, BUKAN fix lagi]**: user laporkan bubble
 * MENGHILANG TOTAL saat di-drag ke tepi landscape yang berbeda (sisi nav-bar-bottom), balik
 * normal HANYA kalau device dirotasi ke portrait lagi. Ini gejala BARU, lebih parah dari sebelum
 * Batch 468 (dulu cuma "kurang ter-klip", sekarang bisa hilang total/unreachable). Dugaan kuat:
 * [screenBounds] diisi dari 2 API BERBEDA tergantung KAPAN — `onCreate` pakai
 * `currentWindowMetrics.bounds` (baris ~401), `onConfigurationChanged` pakai
 * `newConfig.screenWidthDp/HeightDp * density` (Batch 461, demi alasan freshness/timing yang
 * TERBUKTI valid saat itu) — 2 API ini TIDAK dijamin identik secara SEMANTIK (area yang diukur
 * bisa beda: device-level Configuration vs WindowMetrics). Sebelum Batch 468 (posisi
 * content-area-relative), potensi selisih ini "aman" karena WindowManager & clamp SAMA-SAMA
 * relatif ke area yang lebih kecil; sejak Batch 468 (posisi full-screen-absolute), selisih itu
 * bisa mendorong posisi hasil clamp ke [screenBounds] versi `newConfig` keluar dari layar NYATA
 * yang sekarang dipakai WindowManager. **BELUM DIPASTIKAN** — teori, bukan bukti. SOP eksplisit
 * ("PELAJARAN PROSES Batch 461->462"): JANGAN ganti-ganti sumber API lagi tanpa data, apalagi ini
 * regresi ke-2 di file yang sama minggu ini. **0 formula/clamp/posisi diubah SAMA SEKALI** —
 * batch ini MURNI 2 titik log baru: (1) [onConfigurationChanged] sekarang JUGA log
 * `currentWindowMetrics.bounds` (read-only, cuma pembanding) di sebelah [screenBounds] yang
 * BENAR-BENAR dipakai — kalau 2 angka itu beda, itu BUKTI LANGSUNG teori di atas; (2)
 * [setupDrag] `ACTION_UP` (drag manual, path yang SEBELUMNYA 0 instrumentasi sama sekali, beda
 * dari [snapMinimizedToNearestEdge] yang sudah ada sejak Batch 463) sekarang log target akhir +
 * [screenBounds] yang dipakai clamp + readback `getLocationOnScreen()` +250ms — kalau readback
 * keluar dari rentang [0, screenBounds] atau beda drastis dari target, itu bukti bubble memang
 * dirender di luar layar nyata. Sesi berikutnya: WAJIB baca log HASIL reproduksi persis skenario
 * ini (landscape, drag ke tepi nav-bottom, JANGAN rotasi balik dulu sebelum ekspor Log
 * Diagnostik — rotasi ke portrait "memperbaiki" gejala tapi JUGA menghapus jendela diagnostik)
 * SEBELUM coding fix apa pun — bukan tebakan ke-5.
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
    // Batch 461 — single source of truth utk bounds layar, GANTI 4 titik baca langsung
    // `windowManager.currentWindowMetrics.bounds` (Batch 460). Root cause residual landscape
    // masih tidak konsisten mentok tepi meski Batch 460 sudah pakai currentWindowMetrics:
    // `windowManager` di sini didapat dari Context Service biasa (BUKAN UiContext/WindowContext) —
    // per dokumentasi resmi Android, `currentWindowMetrics` pada Context non-UI selalu jatuh ke
    // `getMaximumWindowMetrics()`, yang bergantung pada objek Display yang di-cache Context itu;
    // TIDAK ada jaminan sinkron atomik dengan momen persis rotasi terjadi (beda kelas masalah dari
    // `resources.displayMetrics`, tapi akar sama: keduanya nebeng Context Service yang sama).
    // Fix: [onConfigurationChanged] di-refresh dari `newConfig` — SATU-SATUNYA sumber yang
    // dijamin sistem fresh PERSIS di momen rotasi (bukan re-query async) — field ini lalu dibaca
    // oleh 3 fungsi lain (`setupDrag`, `expand`, `snapMinimizedToNearestEdge`), 0 lagi query
    // WindowManager langsung di tempat lain.
    private val screenBounds = Rect()
    private var layoutParams: WindowManager.LayoutParams? = null
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val bubbleScope = CoroutineScope(Dispatchers.Main + Job())
    private var bubbleArtJob: Job? = null
    // Batch 453 — timer auto-fade idle, lihat KDoc kelas ini & keepAwakeAndScheduleFade().
    private var idleFadeJob: Job? = null
    // Batch 454 — timer auto-minimize-total idle, lihat KDoc kelas ini & keepAwakeAndScheduleFade().
    private var idleMinimizeJob: Job? = null

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
        // Batch 461 — nilai awal screenBounds sebelum rotasi pertama terjadi (baseline, lihat
        // KDoc field). Sesudah ini SATU-SATUNYA writer adalah onConfigurationChanged.
        screenBounds.set(windowManager.currentWindowMetrics.bounds)
        startForegroundWithNotification()
        addBubbleView()
        connectController()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Batch 461 — `newConfig` DIJAMIN sistem fresh persis di momen rotasi (lihat KDoc field
        // screenBounds) — refresh SATU-SATUNYA writer ini DULUAN, SEBELUM early-return guard di
        // bawah, supaya screenBounds tidak pernah lewat ter-skip walau bubbleView/layoutParams
        // sempat null (mis. timing race view belum sempat di-attach).
        val density = resources.displayMetrics.density
        screenBounds.set(0, 0, (newConfig.screenWidthDp * density).toInt(), (newConfig.screenHeightDp * density).toInt())
        // Batch 463 — instrumentasi MURNI (0 formula/logic diubah): Batch 462 (re-assert 2x
        // delay) dikonfirmasi GAGAL LAGI oleh user ("masih nongol") dan logcat yang dibawa user
        // (grep floatingbubble|configurationchanged|windowmanager) TERBUKTI 0 baris dari service
        // ini sama sekali — cuma berisi echo command-nya sendiri. Root cause tidak bisa ditebak
        // lagi tanpa data; log ini adalah data itu. AppLogger.w() dipilih (bukan Log.d polos)
        // supaya OTOMATIS tersimpan ke 2 kanal sekaligus: logcat (tag "FloatingBubbleService",
        // akan kena grep yang SAMA persis dipakai user) DAN diagnostic_log.txt privat app
        // (dibaca/diekspor lewat Settings > Lanjutan > Log Diagnostik, 0 perlu Termux/adb sama
        // sekali) — 2 jalur pengambilan data, salah satu pasti kepakai.
        AppLogger.w(
            "FloatingBubbleService",
            "Batch463 onConfigurationChanged: orientation=${newConfig.orientation} " +
                "screenBounds=$screenBounds isMinimized=$isMinimized"
        )
        // Batch 469 — instrumentasi MURNI (0 formula diubah): user laporkan bubble MENGHILANG
        // total saat di-drag ke tepi landscape berbeda (nav bottom), balik normal cuma kalau
        // rotasi ke portrait lagi — BARU muncul setelah Batch 468 (FLAG_LAYOUT_IN_SCREEN).
        // Hipotesis: [screenBounds] di atas (dari `newConfig.screenWidthDp/HeightDp`, API BEDA
        // dari `currentWindowMetrics.bounds` yang dipakai `onCreate` baris ~383) mungkin TIDAK
        // sama dengan bounds full-screen NYATA yang sekarang dipakai WindowManager buat render
        // (sejak Batch 468) — kalau bounds dari `newConfig` LEBIH BESAR dari layar asli, params
        // hasil clamp ke bounds itu bisa jatuh DI LUAR layar yang sungguhan attributable.
        // JANGAN ganti sumber [screenBounds] lagi tanpa data (pola terlarang, lihat KDoc kelas
        // "PELAJARAN PROSES Batch 461->462") — baris ini CUMA baca+log pembanding, 0 mengubah
        // nilai [screenBounds] yang benar-benar dipakai formula di bawah.
        val liveBounds = runCatching { windowManager.currentWindowMetrics.bounds }.getOrNull()
        AppLogger.w(
            "FloatingBubbleService",
            "Batch469 bounds-compare: newConfigBounds=$screenBounds vs " +
                "currentWindowMetricsBounds=$liveBounds density=${resources.displayMetrics.density} " +
                "orientation=${newConfig.orientation}"
        )
        val view = bubbleView ?: run {
            AppLogger.w("FloatingBubbleService", "Batch463 onConfigurationChanged: bubbleView NULL, skip re-snap")
            return
        }
        val params = layoutParams ?: run {
            AppLogger.w("FloatingBubbleService", "Batch463 onConfigurationChanged: layoutParams NULL, skip re-snap")
            return
        }
        // Batch 100 — kalau lagi minimized, X SELALU harus tetap di tepi 0/maxX (bukan cuma
        // di-clamp masuk batas layar baru) — re-snap penuh, bukan clamp biasa yang bisa saja
        // menyisakan X "nyaris tepi tapi bukan tepi" pas rotasi mengubah lebar layar.
        if (isMinimized) {
            snapMinimizedToNearestEdge()
            // Batch 462 — safety-net re-assert, lihat KDoc kelas "Batch 462". Device fisik
            // konfirmasi Batch 461 GAGAL total (tab 100% kelihatan/gak keclip sama sekali di
            // landscape, bukan cuma "kurang tepat") — indikasi kuat ADA pihak lain (transisi
            // animasi rotasi sistem) menimpa posisi window SETELAH snap pertama kita apply.
            // Re-assert 2x dengan delay berbeda (bracket durasi animasi rotasi tipikal) —
            // idempotent kalau snap pertama sudah benar (re-apply nilai sama, 0 efek kelihatan),
            // tapi jadi fallback pasti kalau snap pertama sempat ketiban sistem.
            for (delay in ROTATION_RESNAP_DELAYS_MS) {
                view.postDelayed({
                    // Batch 463 — instrumentasi: buktikan apakah callback delay ini BENAR
                    // tereksekusi sama sekali (0 cara lain memverifikasi ini dari luar).
                    AppLogger.w("FloatingBubbleService", "Batch463 re-snap delay=${delay}ms terpanggil, isMinimized=$isMinimized")
                    if (isMinimized) snapMinimizedToNearestEdge()
                }, delay)
            }
            return
        }
        // Batch 461 — baca dari screenBounds ter-cache (bukan lagi query currentWindowMetrics
        // langsung), lihat KDoc field.
        val bounds = screenBounds
        val maxX = (bounds.width() - view.width).coerceAtLeast(0)
        val maxY = (bounds.height() - view.height).coerceAtLeast(0)
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
        stopForeground(STOP_FOREGROUND_REMOVE)
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

        val overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        isMinimized = bubbleStore.isMinimized()
        val saved = bubbleStore.getPosition()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
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
        keepAwakeAndScheduleFade() // Batch 453 — mulai idle timer dari saat bubble pertama tampil
    }

    private fun applyOvalClip(imageView: ImageView) {
        imageView.clipToOutline = true
        imageView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                outline.setOval(0, 0, v.width, v.height)
            }
        }
    }

    /** Batch 453 — reset bubble ke opaque penuh SEKETIKA (batalkan fade yang mungkin lagi
     * berjalan/sudah selesai) lalu jadwalkan ulang fade berikutnya [IDLE_FADE_DELAY_MS] dari
     * SEKARANG. Dipanggil di SETIAP titik masuk interaksi user (lihat pemanggil di [setupDrag]
     * & [setupControls]) — hasilnya bubble selalu full-opacity selama masih dipakai, mulai
     * meredup hanya setelah benar-benar tidak disentuh selama durasi itu. Lihat KDoc kelas ini
     * untuk rasionalisasi penuh (kenapa [bubbleScope] dipakai ulang, kenapa alpha di container). */
    private fun keepAwakeAndScheduleFade() {
        idleFadeJob?.cancel()
        idleMinimizeJob?.cancel()
        val view = bubbleView ?: return
        view.animate().cancel()
        if (view.alpha != 1f) view.alpha = 1f
        idleFadeJob = bubbleScope.launch {
            delay(IDLE_FADE_DELAY_MS)
            bubbleView?.animate()?.alpha(IDLE_FADE_ALPHA)?.setDuration(IDLE_FADE_ANIM_MS)?.start()
        }
        // Batch 454 — mandat utama user ("wajib bisa split/di-minimize total"), lihat KDoc kelas.
        // Guard `!isMinimized` murni optimisasi (skip launch sia-sia kalau sudah minimized manual
        // duluan) — minimize() sendiri SUDAH guard `if (isMinimized) return`, jadi aman tanpa cek
        // ini juga kalau state berubah di tengah delay.
        idleMinimizeJob = bubbleScope.launch {
            delay(IDLE_AUTO_MINIMIZE_DELAY_MS)
            if (!isMinimized) minimize()
        }
    }

    /** Drag-untuk-pindah + tap-untuk-buka-app di area kosong pill, dibedakan lewat TOTAL jarak
     * gerak (bukan cuma delta awal-akhir, supaya jari gemetar kecil tidak salah dianggap drag).
     * Tombol play/pause/prev/next tetap dapat event klik normal — ImageButton clickable
     * mengonsumsi ACTION_DOWN duluan sebelum sempat ke OnTouchListener root ini, jadi drag/tap
     * di sini otomatis cuma aktif di luar area ke-3 tombol tanpa perlu logic pemisah manual.
     * Batch 98: metrics dibaca ULANG tiap ACTION_MOVE (bukan di-cache sekali di awal seperti
     * sebelumnya) — device bisa saja rotasi PAS lagi di-drag, metrics yang di-cache di awal akan
     * basi. Batch 460: sumbernya `windowManager.currentWindowMetrics` (bukan lagi
     * `resources.displayMetrics`, lihat KDoc kelas "Batch 460"). */
    private fun setupDrag(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var totalMovement = 0f

        view.setOnTouchListener { v, event ->
            keepAwakeAndScheduleFade() // Batch 453 — sentuhan apa pun = full-opacity + reset timer
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
                        // Batch 461 — screenBounds ter-cache, lihat KDoc field (ganti currentWindowMetrics).
                        val bounds = screenBounds
                        val maxX = (bounds.width() - v.width).coerceAtLeast(0)
                        val maxY = (bounds.height() - v.height).coerceAtLeast(0)
                        params.x = (initialX + dx.toInt()).coerceIn(0, maxX)
                        params.y = (initialY + dy.toInt()).coerceIn(0, maxY)
                        runCatching { windowManager.updateViewLayout(v, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (totalMovement > TOUCH_SLOP) {
                        bubbleStore.savePosition(params.x, params.y)
                        // Batch 469 — instrumentasi MURNI (0 formula diubah, lihat KDoc kelas
                        // "Batch 469"): user laporkan bubble MENGHILANG saat di-drag ke tepi
                        // landscape berbeda (nav bottom), pola BARU muncul setelah Batch 468
                        // (FLAG_LAYOUT_IN_SCREEN). Log target akhir drag + [screenBounds] yang
                        // dipakai clamp DI SINI (baris ~657-660), PLUS readback posisi NYATA di
                        // layar 250ms kemudian (`getLocationOnScreen`, pola sama
                        // [snapMinimizedToNearestEdge] Batch 463) — kalau readback keluar dari
                        // rentang [0, screenBounds] atau beda dari target, itu bukti langsung
                        // bubble memang dirender di luar layar nyata saat drag biasa (BUKAN cuma
                        // lewat [snapMinimizedToNearestEdge]).
                        val dragTargetX = params.x
                        val dragTargetY = params.y
                        val dragBounds = Rect(screenBounds)
                        AppLogger.w(
                            "FloatingBubbleService",
                            "Batch469 drag release target: x=$dragTargetX y=$dragTargetY " +
                                "screenBounds=$dragBounds isMinimized=$isMinimized"
                        )
                        v.postDelayed({
                            val loc = IntArray(2)
                            v.getLocationOnScreen(loc)
                            AppLogger.w(
                                "FloatingBubbleService",
                                "Batch469 drag readback (+250ms): posisi layar nyata x=${loc[0]} " +
                                    "y=${loc[1]} (target x=$dragTargetX y=$dragTargetY, " +
                                    "screenBounds=$dragBounds)"
                            )
                        }, 250L)
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
        // Batch 453 — 4 tombol ini clickable, jadi mengonsumsi ACTION_DOWN SEBELUM sempat ke
        // OnTouchListener root di setupDrag (lihat KDoc di sana) — reset idle-fade dipanggil
        // ulang eksplisit di sini supaya tap tombol kontrol juga dihitung "sedang dipakai".
        view.findViewById<ImageButton>(R.id.bubble_play_pause).setOnClickListener {
            keepAwakeAndScheduleFade()
            if (hasQueue) sendPlaybackAction(WidgetUpdater.ACTION_TOGGLE_PLAY) else openApp()
        }
        view.findViewById<ImageButton>(R.id.bubble_prev).setOnClickListener {
            keepAwakeAndScheduleFade()
            if (hasQueue) sendPlaybackAction(WidgetUpdater.ACTION_PREVIOUS) else openApp()
        }
        view.findViewById<ImageButton>(R.id.bubble_next).setOnClickListener {
            keepAwakeAndScheduleFade()
            if (hasQueue) sendPlaybackAction(WidgetUpdater.ACTION_NEXT) else openApp()
        }
        view.findViewById<ImageButton>(R.id.bubble_minimize).setOnClickListener {
            keepAwakeAndScheduleFade()
            minimize()
        }
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
            // Batch 461 — screenBounds ter-cache, lihat KDoc field (ganti currentWindowMetrics).
            val maxX = (screenBounds.width() - container.width)
                .coerceAtLeast(0)
            params.x = (lastExpandedX ?: params.x).coerceIn(0, maxX)
            runCatching { windowManager.updateViewLayout(container, params) }
            bubbleStore.savePosition(params.x, params.y)
        }
    }

    /** Chat-head-style "nempel tepi", **Batch 455 — kliping SETENGAH**: X dipaksa ke `-lebarTab/2`
     * (kiri) atau `screenWidth - lebarTab/2` (kanan) — mana pun yang lebih dekat dari posisi X
     * saat ini, TIDAK PERNAH mengambang bebas di tengah layar selagi minimized. Sebelumnya
     * (Batch 100) tab berhenti flush tapi 100% kelihatan (`0`/`screenWidth - lebarTab`) — instruksi
     * eksplisit user menolak itu ("kliping setengah/menyisakan mini trigger", "wajib mentok
     * maksimal ke tepi"): sekarang SETENGAH lebar tab sengaja diposisikan MELEWATI batas layar,
     * sisa setengah yang kelihatan jadi tap-target "mini trigger" mentok tepi. Window overlay
     * SUDAH `FLAG_LAYOUT_NO_LIMITS` sejak awal (lihat [addBubbleView]) — prasyarat X negatif/lewat
     * `screenWidth` diterima WindowManager, 0 flag baru; sistem otomatis memotong render di luar
     * layar, tidak perlu clip manual. `container.post{}` supaya ukuran tab yang SEBENARNYA (dari
     * `layout_width="48dp"` di bubble_minimized.xml, sudah ke-measure oleh sistem) yang dipakai
     * hitung `lebarTab/2` — bukan angka dp ditebak manual dari kode. **1 titik kontrol ini saja**
     * yang diubah — otomatis berlaku ke SEMUA pemanggil yang sudah ada (drag-lepas saat minimized,
     * auto-minimize idle Batch 454, tombol chevron manual via [minimize], restart service, rotasi
     * via [onConfigurationChanged]) — 0 titik panggil baru, 0 state/timer baru. Saat masih di-drag
     * aktif, tab TETAP dibatasi penuh di dalam layar seperti biasa ([setupDrag] clamp `[0, maxX]`
     * pakai lebar penuh) — half-clip HANYA berlaku begitu jari dilepas & tab benar-benar diam
     * (idle) di tepi, bukan selagi masih dipegang/dipindah.
     *
     * **Batch 456 [SALAH ARAH]**: fraksi clip dikecilkan 50%→30% — efek sebenarnya menambah
     * bagian tab yang kelihatan (tambah "timbul"), kebalikan dari maksud user. Formula
     * digeneralisasi (`hiddenWidth = width * EDGE_CLIP_FRACTION`, bukan `width/2` hardcoded)
     * TETAP DIPERTAHANKAN — netral arah, cuma nilai konstanta yang salah.
     *
     * **Batch 457**: [EDGE_CLIP_FRACTION] DIREVERT 30%→50% atas feedback eksplisit user ("malah
     * dibikin tambah timbul, bukan saya suruh"). 0 formula/titik panggil lain disentuh — pada
     * 0.5f formula ini identik matematis dengan Batch 455 (regresi-aman).
     *
     * **Batch 458**: [EDGE_CLIP_FRACTION] dinaikkan 50%→70% — user minta bagian TIMBUL turun ke
     * ~30%, dikonfirmasi via tap-choice merujuk ke "kelihatan" bukan ke fraksi klip (menghindari
     * ulang salah-arah Batch 456). 0 formula/titik panggil baru.
     *
     * **Batch 460**: 2 perubahan. (1) `hiddenWidth` dipisah jadi `touchPad + visualWidth *
     * EDGE_CLIP_FRACTION` (bukan `width * EDGE_CLIP_FRACTION` polos) — `width` root sekarang
     * lebih lebar dari `visualWidth` sejak `bubble_minimized.xml` dapat child
     * `R.id.bubble_minimized_visual` terpisah (area ekstra transparan, murni perluas area sentuh,
     * lihat KDoc kelas). `touchPad` (selisih root-visual /2, simetris kiri/kanan by design) SELALU
     * ikut nempel di sisi yang tetap di layar tiap arah snap — area sentuh naik TANPA ikut
     * mengecil saat [EDGE_CLIP_FRACTION] naik. (2) [EDGE_CLIP_FRACTION] 70%→90% (bagian TIMBUL
     * turun ke ~10%). `windowManager.currentWindowMetrics` ganti `resources.displayMetrics` (fix
     * kliping landscape, lihat KDoc kelas) — 0 formula lain berubah. */
    private fun snapMinimizedToNearestEdge() {
        val container = bubbleView as? FrameLayout ?: return
        val params = layoutParams ?: return
        container.post {
            val width = container.width.takeIf { it > 0 } ?: return@post
            // Batch 460 — lebar VISUAL (tab bulat asli) dibaca terpisah dari lebar ROOT/area-
            // sentuh (`width`, sekarang lebih lebar) — EDGE_CLIP_FRACTION cuma memotong bagian
            // VISUAL, `touchPad` (selisih root-visual) selalu ikut ke sisi yang tetap di layar.
            val visualWidth = minimizedView?.findViewById<View>(R.id.bubble_minimized_visual)
                ?.width?.takeIf { it > 0 } ?: width
            val touchPad = ((width - visualWidth) / 2).coerceAtLeast(0)
            // Batch 461 — screenBounds ter-cache, lihat KDoc field (ganti currentWindowMetrics).
            val bounds = screenBounds
            val screenWidth = bounds.width()
            val hiddenWidth = touchPad + (visualWidth * EDGE_CLIP_FRACTION).toInt()
            val nearestRight = (params.x + width / 2) > screenWidth / 2
            params.x = if (nearestRight) (screenWidth - width + hiddenWidth) else -hiddenWidth
            // Y juga di-clamp (bukan cuma X yang "dipaksa tepi") — rotasi bisa mengubah tinggi
            // layar juga, Y lama yang valid di orientasi sebelumnya bisa jadi melebihi batas.
            val maxY = (bounds.height() - container.height).coerceAtLeast(0)
            params.y = params.y.coerceIn(0, maxY)
            // Batch 463 — instrumentasi MURNI (0 formula diubah, lihat KDoc kelas "Batch 463"):
            // log nilai TARGET tepat sebelum apply, supaya bisa dibandingkan ke posisi NYATA di
            // layar (readback di bawah) — satu-satunya cara membuktikan/membantah teori Batch 462
            // ("ada pihak lain menimpa posisi window pasca-snap") tanpa menebak lagi.
            AppLogger.w(
                "FloatingBubbleService",
                "Batch463 snap target: screenWidth=$screenWidth hiddenWidth=$hiddenWidth " +
                    "nearestRight=$nearestRight -> x=${params.x} y=${params.y}"
            )
            val applied = runCatching { windowManager.updateViewLayout(container, params) }
            applied.exceptionOrNull()?.let { err ->
                AppLogger.w("FloatingBubbleService", "Batch463 snap: updateViewLayout GAGAL - $err")
            }
            bubbleStore.savePosition(params.x, params.y)
            // Batch 463 — readback +250ms: baca posisi NYATA container di layar (bukan
            // layoutParams kita, yang cuma mencatat apa yang KITA minta) — kalau sistem
            // menimpanya setelah apply (teori Batch 462), angka ini akan beda dari x target di
            // atas. 250ms dipilih supaya readback tiap snap (immediate/150ms/400ms) sempat
            // menangkap window SEBELUM re-assert berikutnya menimpanya lagi. Target di-snapshot
            // ke `val` LOKAL (`targetX`/`targetY`, bukan baca `params.x` lagi di dalam lambda) —
            // `params` object yang SAMA dipakai bergantian oleh 3 panggilan snap (immediate/150ms/
            // 400ms), jadi kalau dibaca lagi nanti isinya bisa saja sudah ditimpa panggilan
            // berikutnya, bukan lagi nilai yang di-apply oleh panggilan INI.
            val targetX = params.x
            val targetY = params.y
            // Batch 464 — hasil readback Batch 463 (data device fisik pertama, MENGEJUTKAN):
            // mismatch BESAR terjadi bahkan saat minimize() BIASA (0 rotasi sama sekali, screenWidth
            // 1080 tetap portrait) — target x=1011 vs nyata x=551 (selisih 460px). Ini menggeser
            // dugaan: mungkin BUKAN soal animasi transisi ROTASI (teori Batch 462) — window overlay
            // ini `WRAP_CONTENT` (resize fisik tiap toggle expanded<->minimized), dan `width` yang
            // dibaca di `container.post{}` BISA JADI representasi ukuran View yang sudah di-measure
            // tapi window WindowManager-nya sendiri belum tuntas resize saat `updateViewLayout`
            // dipanggil (race lain, beda dari 3 teori Batch 460-462, TIDAK PERNAH diuji sebelum ini).
            // 2 tambahan MURNI observasi (0 masih formula/logic diubah): (1) log `container.width`/
            // `container.height` DI READBACK juga — kalau beda dari `width`/`container.height` saat
            // target dihitung di atas, itu BUKTI LANGSUNG window/view masih resize saat kita apply;
            // (2) 1 readback TAMBAHAN di +800ms (bukan cuma +250ms) — kalau posisi di +800ms SUDAH
            // sama dengan target (beda dari +250ms yang meleset), itu bukti ini soal SETTLING/
            // ANIMASI (sementara), bukan salah permanen; kalau +800ms MASIH meleset sama, itu bukti
            // salahnya permanen (bukan soal waktu tunggu sama sekali).
            val targetWidth = width
            val targetHeight = container.height
            fun logReadback(label: String) {
                val loc = IntArray(2)
                container.getLocationOnScreen(loc)
                AppLogger.w(
                    "FloatingBubbleService",
                    "Batch464 readback ($label): posisi layar nyata x=${loc[0]} y=${loc[1]} " +
                        "(target x=$targetX y=$targetY) | ukuran nyata w=${container.width} " +
                        "h=${container.height} (target w=$targetWidth h=$targetHeight)"
                )
            }
            container.postDelayed({ logReadback("+250ms") }, 250L)
            container.postDelayed({ logReadback("+800ms") }, 800L)
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
                startForegroundService(intent)
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
        contentResolver.loadThumbnail(uri, Size(120, 120), null)
    } catch (e: Exception) {
        AppLogger.e("FloatingBubbleService", "Gagal muat artwork bubble", e)
        null
    }

    companion object {
        private const val TOUCH_SLOP = 12f
        private const val NOTIFICATION_CHANNEL_ID = "floating_bubble"
        private const val NOTIFICATION_ID = 7002 // beda dari COLD_START_NOTIFICATION_ID (7001)

        // Batch 453 — tuning auto-fade idle, lihat KDoc kelas & keepAwakeAndScheduleFade().
        private const val IDLE_FADE_DELAY_MS = 2500L
        private const val IDLE_FADE_ALPHA = 0.45f
        private const val IDLE_FADE_ANIM_MS = 250L
        // Batch 454 — tuning auto-minimize idle (dihitung dari titik interaksi terakhir, SAMA
        // dgn titik hitung IDLE_FADE_DELAY_MS, bukan ditambah setelahnya). > IDLE_FADE_DELAY_MS
        // supaya urutan visual selalu fade dulu, baru collapse — user masih sempat lihat bubble
        // meredup sebelum menciut total, bukan langsung "hilang" tiba-tiba dari opaque penuh.
        private const val IDLE_AUTO_MINIMIZE_DELAY_MS = 6000L
        // Batch 455/456/457/458/460 — tuning half-clip tepi layar tab minimized (fraksi bagian
        // VISUAL saja sejak Batch 460, lihat KDoc snapMinimizedToNearestEdge()). Batch 456 SALAH
        // ARAH (0.5f->0.3f menaikkan bagian kelihatan = tambah timbul, kebalikan dari diminta),
        // Batch 457 REVERT ke 0.5f, Batch 458 naik ke 0.7f (30% timbul, dikonfirmasi tap). Batch
        // 460: user eksplisit pakai kata "timbul" minta turun ke ~10% — naik lagi ke 0.9f (90%
        // sembunyi, 10% timbul). Sejak Batch 460 TIDAK LAGI ikut mengecilkan area sentuh (lihat
        // touchPad independen di snapMinimizedToNearestEdge()).
        private const val EDGE_CLIP_FRACTION = 0.9f
        // Batch 462 — 2 delay safety-net re-assert kliping pasca rotasi (lihat KDoc kelas
        // "Batch 462" & onConfigurationChanged). Bracket durasi animasi transisi rotasi tipikal
        // sistem (bervariasi antar OEM/API level) — jaga-jaga ada override posisi window dari
        // sistem SETELAH snap pertama (immediate) tapi SEBELUM animasi rotasi selesai settle.
        private val ROTATION_RESNAP_DELAYS_MS = longArrayOf(150L, 400L)
    }
}
