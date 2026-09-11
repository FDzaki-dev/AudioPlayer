# PROJECT_STATE.md

RAM instan sesi kerja — hanya rule AKTIF final. Tanpa histori revisi, kutipan user, atau
kronologi batch. Histori lengkap tiap batch: `CHANGELOG.md`. Ringkasan fitur: `README.md`.
Arsip batch lama (1-424): `docs/archive/PROJECT_STATE_ARCHIVE.md`.

## ✅ STATUS PROYEK: ACTIVE
Banner DISCONTINUED dicabut eksplisit oleh user (Batch 432). Proyek lanjut normal, sektor dibuka
per instruksi eksplisit user seperti biasa (lihat "Sektor DITUTUP" di bawah untuk yang masih
butuh reopen spesifik).

**Catatan Batch 437**: permintaan FITUR BARU eksplisit user (lampiran screenshot bottom nav) —
efek "kaca pembesar ala iOS" di LABEL 3 tab bawah (Beranda/Perpustakaan/Pengaturan), bereaksi
tergantung "kaca diarahkan kesitu/bukan". Bukan reopen sektor DITUTUP manapun — perluasan
langsung dari fitur swipe Batch 435 (sektor sama, belum pernah ditutup).

**1 file diubah** (dalam batas 3 file/tugas):
1. `MainActivity.kt` (`AppNavHost`) — 0 gesture/state baru: `tabMagnifyFocus(tabIndex)` (fungsi
   lokal baru) murni MEMBACA ULANG `tabDragOffsetPx` (`MutableFloatState` Batch 435, ±40px,
   sudah live tiap frame `onHorizontalDrag` + sudah spring-back ke 0 di `onDragEnd`/
   `onDragCancel`) sebagai bobot fokus 0f..1f per tab — tab yang sedang aktif mulai dari fokus
   1f dan turun mengikuti `max(towardNext, towardPrev)` selama drag, tab tetangga yang dituju
   naik dari 0f ke arah 1f secara kontinu (BUKAN snap di ujung threshold 120px) — persis efek
   lensa bergeser dari 1 label ke label sebelah selama jari masih menekan.
   Label `NavigationBarItem` (`Text("Beranda")` polos dkk) diganti composable baru
   `MagnifyingTabLabel(text, focus)`: `fontSize` discale kontinu dari `LocalTextStyle.current`
   (bukan angka sp hardcode — ikut style/tema label bawaan apa pun yang aktif), plus
   `graphicsLayer{scaleX/scaleY/alpha}` + `Modifier.blur()` (aman tanpa cek `Build.VERSION`,
   minSdk project ini 31 = RenderEffect selalu tersedia) — tab fokus penuh jadi sedikit lebih
   besar/tajam/terang, tab non-fokus mengecil/buram/redup sebagian, transisi mengikuti jari
   frame-demi-frame. HANYA `NavigationBar` bawah (layout ponsel COMPACT, sesuai screenshot user)
   yang disentuh — `NavigationRailItem` (tablet/foldable Medium/Expanded) SENGAJA tidak ikut
   diubah, di luar scope diminta (screenshot user = bottom bar ponsel), 0 side-quest. Dibaca di
   titik pemakaian (dalam tiap `label = { ... }`, bukan di-hoist ke `NavigationBar`) supaya scope
   recomposition sekecil mungkin (hanya `Text` label yang recompose tiap frame drag, bukan
   seluruh bar) — pola read-state-di-leaf yang sama dipakai `BlurUtils.kt`/`IosScrollPhysics.kt`.
   0 breaking change: `selected`/`onClick`/`icon`/route logic Batch 301/435 tidak disentuh sama
   sekali, warna label selected/unselected tetap 100% dari `LocalContentColor` bawaan M3 (tidak
   di-override). Detail lengkap: `CHANGELOG.md` § Batch 437.

**0 diverifikasi CI/device Batch 437** — review manual (baca kode + cek balance brace/paren:
`{}` 286/286, `()` 734/734, `[]` 3/3), tidak ada env Android nyata/device fisik di sesi ini.
Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 436**: user laporan "abis update saya nunggu buffer screen lumayan ±20s",
menduga terkait fitur swipe Batch 435. Investigasi (grep, bukan asumsi) — **0 file diubah**:
1. Diff Batch 435 (`AppNavHost`) di-baca ulang penuh: isinya murni `pointerInput`/
   `detectHorizontalDragGestures` + `graphicsLayer` (kerja UI-thread, non-blocking, 0 I/O, 0
   panggilan network/disk baru). Tidak mungkin jadi sumber jeda 20 detik secara struktural.
2. Root cause sesungguhnya (kode sudah ada SEBELUM Batch 435, tidak disentuh): `ensureLibraryLoaded()`
   → `refreshLibrary()` (`PlayerViewModel.kt`) jalan di **setiap cold start proses** (flag
   `libraryLoadedOnce` in-memory, bukan persisted) — dikonfirmasi lewat komentar existing Batch
   419 sendiri: **"jalur paling panas cold-start"**. Scan `musicRepository.getAllSongs()` +
   (kalau ada) SAF custom folder via Binder/IPC per folder (`customFolderScanner.scan()`) jalan
   di `Dispatchers.IO` — sudah benar secara threading (non-blocking Main), tapi durasi wall-clock
   scan MediaStore tetap naik seiring ukuran library/jumlah folder custom, TIDAK instan. Install
   APK baru = proses baru = scan ini trigger ulang dari nol — persis skenario "abis update".
3. Kesimpulan: **bukan regresi Batch 435**. Perilaku ini sudah ada sebelum swipe gesture ditambah,
   ter-dokumentasi sendiri di README § fitur ("Shimmer skeleton loading" selama fase ini). Tidak
   ada perubahan kode.

**0 diverifikasi CI/device Batch 436** — kesimpulan murni dari pembacaan kode (grep + baca
`PlayerViewModel.kt`/`MusicRepository.kt`), tidak ada env Android nyata/device fisik di sesi ini.

**Catatan Batch 435**: permintaan FITUR BARU eksplisit user — "tambahkan gesture swipe able
lintas 3 tab. alih-alih user hanya bisa tap-tab manual berulang!!" (Beranda/Perpustakaan/
Pengaturan). Bukan reopen sektor DITUTUP manapun (lihat daftar "Sektor DITUTUP" di bawah) —
sektor terpisah, navigasi tab bawah belum pernah masuk 3 sektor yang ditutup itu.

**1 file diubah** (dalam batas 3 file/tugas):
1. `MainActivity.kt` (`AppNavHost`) — app ini pakai Jetpack Navigation Compose dengan 3 route
   top-level TERPISAH (`"home"`/`"library"`/`"settings"`, BUKAN `HorizontalPager` 1-route) —
   swap ke arsitektur pager penuh ditolak sebagai solusi (butuh restrukturisasi NavHost +
   `now_playing`/`stats_dashboard` jadi push-di-atas-pager, risiko regresi jauh lebih besar dari
   scope diminta). Pendekatan dipilih: `Modifier.pointerInput` + `detectHorizontalDragGestures`
   dipasang di `Box` pembungkus `NavHost`, HANYA aktif saat `currentRoute` ada di 3 tab itu
   (`TAB_ROUTES`, konstanta baru) — di `"now_playing"`/`"stats_dashboard"` modifier ini tidak
   terpasang sama sekali (0 rebutan dgn `detectHorizontalDragGestures` `AlbumArtHero` yang sudah
   ada di `now_playing`, Batch 434). Saat threshold ±120px terlampaui, swipe memicu
   `navController.navigate()` dengan opsi IDENTIK ke `onClick` `NavigationBarItem`/
   `NavigationRailItem` yang sudah ada (`popUpTo("home"){saveState=true}` + `launchSingleTop` +
   `restoreState`, pola Batch 301) — 0 state baru di sisi state-preservation, murni trigger
   berbeda (gesture, bukan cuma tap). `enterTransition`/`exitTransition` `NavHost` (fade
   200/150ms, Batch 330) TIDAK disentuh — dipakai apa adanya baik utk tap maupun swipe.

   Pola threshold 120px + haptic (`HapticFeedbackType.LongPress`) + `dragOffsetPx`
   (`MutableFloatState`, sinkron)/`Animatable` (springback-only, via `spring(DampingRatioMediumBouncy,
   StiffnessLow)`) REUSE 1:1 dari `AlbumArtHero` (`NowPlayingScreen.kt`, Batch 434) — sengaja
   tidak reinvent, termasuk `onDragStart` yang panggil `dragOffset.stop()` (jaring pengaman
   sinkron-vs-asinkron yang sama). Beda dari `AlbumArtHero`: nudge visual (`graphicsLayer
   translationX`) di sini dibatasi lebih kecil (±40px, multiplier 0.3, bukan ±48dp/0.5) karena
   yang digeser konten SATU LAYAR PENUH (bukan 1 kartu album), dan page-swap sesungguhnya tetap
   lewat fade `NavHost` yang sudah ada — nudge ini murni sinyal "tergenggam", bukan preview
   halaman berikutnya. `TAB_ROUTES.any{it==currentRoute}`/`indexOfFirst{it==currentRoute}`
   dipakai (bukan `in`/`indexOf` langsung) karena `currentRoute` bertipe `String?` sedangkan
   `List<String>.contains`/`indexOf` mengharap parameter non-null — perbandingan `==` selalu
   type-safe utk operand nullable, `in`/`indexOf` langsung berisiko unresolved/type-mismatch.
   0 breaking change ke `NavigationBarItem`/`NavigationRailItem`/route lain (signature/pola tap
   lama tidak disentuh sama sekali).

**0 diverifikasi CI/device Batch 435** — review manual (baca kode + cek balance brace/paren:
`{}` 280/280, `()` 700/700, `[]` 3/3), tidak ada env Android nyata/device fisik di sesi ini.
Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 434**: reopen eksplisit user — laporan spesifik "effect bounce juga masih
stuttering, belum smooth like butter!!". SAMA KELAS BUG dgn Batch 433 (`IosScrollPhysics.kt`),
tapi di file BEDA: `ui/NowPlayingScreen.kt` → `AlbumArtHero` (swipe horizontal next/prev pada
album art) — ditemukan lewat grep `bounce`/`spring(` menyeluruh ke seluruh `app/src/main/java`
(bukan tebakan single-file), setelah `IosScrollPhysics.kt` sendiri dikonfirmasi baca-kode sudah
bersih dari sumbu bug ini (drag sinkron via `dragOffset` sejak Batch 433, tidak disentuh lagi).

**1 file diubah** (dalam batas 3 file/tugas):
1. `ui/NowPlayingScreen.kt` (`AlbumArtHero`) — root cause identik Batch 433: `onHorizontalDrag`
   menulis posisi lewat `dragScope.launch { dragOffset.snapTo(...) }` di SETIAP delta drag —
   coroutine baru per delta, bisa menumpuk/tidak berurutan saat drag cepat. Fix: `dragOffsetPx`
   (`MutableFloatState` polos, via `mutableFloatStateOf`) jadi sumber kebenaran SINKRON yang
   dibaca `graphicsLayer` (ditulis LANGSUNG dari `onHorizontalDrag`, 0 coroutine). `dragOffset`
   (`Animatable`) tetap ada, sekarang HANYA dipakai di fase springback (`onDragEnd`/
   `onDragCancel`) — `snapTo` posisi drag terakhir dulu, tiap frame `animateTo` disinkronkan
   balik ke `dragOffsetPx` lewat parameter `block` resmi. Tambahan (gap yang tidak muncul di
   Batch 433 krn kasusnya scroll/fling bawaan `scrollable()`, bukan drag-gesture manual):
   `onDragStart` sekarang panggil `dragOffset.stop()` — jaring pengaman springback-lama-vs-
   drag-baru, supaya `block` lama berhenti menimpa `dragOffsetPx` kalau user mulai drag baru
   sebelum springback sebelumnya selesai. `totalDrag`/threshold swipe-next/prev 120px/haptic/
   `dampingRatio`/`stiffness` (Batch 256) TIDAK disentuh — sumbu bug ini murni SINKRON vs
   ASINKRON penulisan offset. Detail lengkap: `CHANGELOG.md` § Batch 434.

**0 diverifikasi CI/device Batch 434** — review manual (baca kode + cek balance brace/paren:
`{}` 292/292, `()` 1287/1287, `[]` 1/1), tidak ada env Android nyata/device fisik di sesi ini.
Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 433**: reopen eksplisit user — laporan spesifik "effect scrolling/transition like
iOS masih terasa stuttering gak halus sama sekali". Sumbu BARU, beda dari seluruh histori tuning
`ui/theme/IosScrollPhysics.kt` Batch 364-383 (semuanya soal KARAKTER pegas — stiffness/
dampingRatio/rubberBand, sudah dikonfirmasi user via banyak iterasi) — "stuttering" = gejala frame
drop/jank, bukan parameter animasi mana yang dipakai.

**1 file diubah** (dalam batas 3 file/tugas):
1. `ui/theme/IosScrollPhysics.kt` — root cause: `applyToScroll` (kontrak resmi non-suspend, justru
   supaya overscroll bisa diterapkan sinkron dalam frame sentuhan yang sama) sebelumnya menulis
   posisi lewat `coroutineScope.launch { overscrollOffset.snapTo(...) }` di SETIAP event scroll
   delta selama drag di zona overscroll — tiap delta bikin coroutine baru krn `Animatable.snapTo`
   cuma suspend, dan `launch` menambah giliran dispatcher yang bisa menumpuk/tidak berurutan saat
   drag cepat = persis gejala stutter yang dilaporkan. Fix: state baru `dragOffset`
   (`MutableState<Offset>` polos) jadi sumber kebenaran SINKRON yang ditulis LANGSUNG (0 coroutine)
   dari `applyToScroll`, pola sama `Modifier.pointerInput { detectDragGestures { ... } }` standar
   Compose. `overscrollOffset` (`Animatable`) tetap ada, sekarang HANYA dipakai internal di fase
   settle (`settleToZero`, sudah suspend by design) — tiap frame animasinya disinkron balik ke
   `dragOffset` lewat parameter `block` resmi `Animatable.animateTo`. `measure()` baca `dragOffset`
   (bukan lagi `overscrollOffset` langsung). `dampingRatio`/`stiffness`/`rubberBandResistance`
   (semua tuning Batch 368-383) TIDAK disentuh — sumbu bug ini murni soal SINKRON vs ASINKRON-nya
   penulisan offset, bukan parameter pegasnya. Detail lengkap: `CHANGELOG.md` § Batch 433.

**0 diverifikasi CI/device Batch 433** — review manual (baca kode + cek balance brace/paren), tidak
ada env Android nyata/device fisik di sesi ini. Item belum-terverifikasi bertambah 1 (lihat daftar
di bawah).

**Catatan Batch 425–430**: user secara eksplisit reopen **satu kali khusus** untuk Coil migration
(bump 2.6.0→3.x, 3 file + `build.gradle.kts`), lalu reopen KEDUA secara terpisah eksplisit untuk
sektor Compose optimization (`AlbumArt`, Batch 429) — bukan pencabutan status permanen. Versi
final Coil: **3.3.0**, HIJAU CI. `AlbumArt` AsyncImage swap — user konfirmasi device asli render
NORMAL, 0 regresi visual (Batch 430). Kedua sektor TUNTAS & terverifikasi penuh (CI + device).
Detail teknis lengkap: `CHANGELOG.md` § Batch 425–430.

**Catatan Batch 431**: reopen ketiga, eksplisit user minta audit menyeluruh atas seluruh sektor
"belum terjamah" (di luar `ui/`), KECUALI paket `update/` (`GitHubReleaseChecker.kt`,
`UpdateDownloader.kt`, `UpdateManager.kt` — waktu itu masih tertutup, kini disisir Batch 432
di bawah).

**Catatan Batch 432**: reopen eksplisit user untuk audit paket `update/` (3 file, belum pernah
disisir sebelumnya). Hasil:
- `GitHubReleaseChecker.kt` — `fetchLatest()` pakai `.execute()` blocking sengaja (bukan
  `suspend`), tapi HANYA dipanggil dari dalam `UpdateManager.scope.launch` (Dispatchers.IO) —
  tidak pernah jalan di Main thread. Komentar file menjelaskan `.string()` di sini aman karena
  cuma JSON kecil (beda dari APK binary di `UpdateDownloader`). Tidak diubah.
- `UpdateDownloader.kt` — `download()` streaming 8 KB per chunk ke disk (`Buffer`/`sink()`),
  TIDAK pernah `readBytes()`/`.string()` pada body APK — sesuai Safety Locks SOP. Dipanggil dari
  background thread oleh caller. Tidak diubah.
- `UpdateManager.kt` — **1 file diubah**. Gap nyata: `checkForUpdate()` &
  `downloadAndPrepareInstall()` jalan di `Thread {}` mentah, bukan Coroutines — melanggar SOP
  §2 Thread Safety ("WAJIB Coroutines"), dan Thread lepas tidak bisa dibatalkan kalau proses
  butuh cleanup. Fix: scope baru `CoroutineScope(SupervisorJob() + Dispatchers.IO)` milik
  singleton ini, `Thread { ... }.start()` → `scope.launch { ... }`. `kotlinx.coroutines` sudah
  jadi dependency existing (dipakai `FloatingBubbleService.kt` & lainnya) — 0 dependency baru.
  API publik (`checkForUpdate`, `downloadAndPrepareInstall`, `launchInstall`, `reset`, `state`)
  tidak berubah, 0 breaking change ke `UpdateCheckSheet.kt`.

**0 diverifikasi CI/device Batch 432** — review manual (baca kode + cek balance brace/paren),
sama seperti Batch 431. Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**2 file diubah** (dalam batas 3 file/tugas):
1. `ui/DiagnosticLogSheet.kt` — 3 titik panggilan `AppLogger.readLog()`/`exportLogToDocuments()`/
   `clearLog()` sebelumnya jalan LANGSUNG di Main thread (initial value `remember` + 2x `onClick`),
   0 coroutine wrapper. Gap ini LOLOS dari audit literal-grep `ui/` sebelumnya karena syntax I/O
   asli (`FileInputStream`, `readText()`, dst) hidup di `util/AppLogger.kt` — paket lain, bukan di
   file `ui/` itu sendiri — bukan false positif, tapi kelas gap yang memang belum pernah disisir:
   call site fungsi lintas-paket yang blocking. Fix: `rememberCoroutineScope()` +
   `Dispatchers.IO`/`withContext(Dispatchers.Main)`, pola identik `SignatureMatcherSheet.kt`
   (Batch 421)/`BackupRestoreSheet.kt`.
2. `ui/DuplicateFinderSheet.kt` — item Compose-perf yang sudah tercatat (lihat riwayat rule di
   bawah): `DuplicateDetector.findLibraryDuplicates`/`findPhysicalDuplicates` (groupBy +
   sortedByDescending atas seluruh `songs`) sebelumnya jalan synchronous di dalam `remember` =
   bagian dari composition phase (Main thread). Fix: `LaunchedEffect(songs)` +
   `Dispatchers.Default` (CPU-bound, bukan I/O), state `isScanning` baru buat loading indicator
   supaya tidak salah tampil "0 duplikat ditemukan" sebelum hasil async selesai.

**Cakupan audit** (grep pola blocking I/O — `commit()`, `Thread.sleep`, `HttpURLConnection`/
`.execute()`, `FileInputStream`/`FileOutputStream`/`open{Input,Output}Stream`, `readBytes`/
`readText`/`writeText`/`writeBytes`, `runBlocking`, `BitmapFactory`, `MediaMetadataRetriever`,
`listFiles`/`walk`, `contentResolver.query` — di seluruh 70 file Kotlin di luar `ui/` & `update/`):
selain 2 fix di atas, SISANYA sudah benar (konfirmasi baca kode, bukan asumsi) —
`ApkSignatureChecker.inspect()` (Batch 421), `TagEditor`/`RingtoneEncoder` (viewModelScope.launch
Dispatchers.IO di `PlayerViewModel.kt`), `BackupManager` (scope.launch Dispatchers.IO di
`BackupRestoreSheet.kt`), `CustomFolderScanner.scan()` (withContext Dispatchers.IO, Batch 386/418),
`LyricsApi.getLyrics` (`suspend fun` + Retrofit, bukan `.execute()` mentah) — semua sudah
terbungkus dispatcher yang benar sejak batch-batch sebelumnya. `AppLogger.writePublicCrashLog()`
sengaja TETAP synchronous (dipanggil dari uncaught-exception-handler saat proses bisa mati kapan
saja — mendispatch ke thread lain di titik ini tidak aman) — bukan bug, tidak diubah.

**0 diverifikasi CI/device sesi ini** (tidak ada compiler/device fisik tersedia) — 2 fix di atas
murni review manual (baca kode + cross-reference pola batch sebelumnya + cek balance
brace/paren). Item belum-terverifikasi bertambah 2 (lihat daftar di bawah).

**Item belum-terverifikasi saat penutupan** (device fisik tidak pernah tersedia di sesi kerja):
- `MainActivity.kt` efek kaca-pembesar label tab bawah (Batch 437, di atas) — 0 compile log, 0
  konfirmasi device. Perlu ditest: drag pelan (fokus label bergeser mulus tab-ke-tab, bukan
  patah-patah), drag cepat lalu lepas sebelum threshold (springback fokus kembali ke tab asal
  mulus), drag di tab ujung (Beranda/Pengaturan, tidak crash walau tidak ada tab tujuan), dan
  0 frame-drop/jank tambahan di atas nudge konten yang sudah ada saat 3 label render bersamaan.
- `MainActivity.kt` swipe-lintas-3-tab (Batch 435, di atas) — 0 compile log, 0 konfirmasi device.
  Perlu ditest: swipe kiri/kanan di Beranda/Perpustakaan/Pengaturan (compact & rail/tablet),
  swipe di tab ujung (Beranda/Pengaturan) tidak nyasar/crash, swipe pendek (di bawah threshold)
  snapback mulus, dan gesture TIDAK kepicu sama sekali di `now_playing`/`stats_dashboard`.
- `ui/NowPlayingScreen.kt` `AlbumArtHero` (Batch 434, di atas) — 0 compile log, 0 konfirmasi
  device. Perlu ditest: swipe cepat berulang next/prev, springback di dragEnd/dragCancel, dan
  drag baru yang menyusul cepat sebelum springback lama selesai (skenario baru yang dijaga
  `dragOffset.stop()`).
- `ui/theme/IosScrollPhysics.kt` (Batch 433, di atas) — 0 compile log, 0 konfirmasi device. Perlu
  ditest: drag cepat berulang di list panjang (stutter hilang?), transisi antar layar, dan flow
  settle (lepas jari di tengah overscroll) tetap 0 regresi ke karakter pegas Batch 368-383 yang
  sudah disetujui user.
- `ui/DiagnosticLogSheet.kt` & `ui/DuplicateFinderSheet.kt` (Batch 431, di atas) — 0 compile log,
  0 konfirmasi device.
- `update/UpdateManager.kt` (Batch 432, di atas) — 0 compile log, 0 konfirmasi device (Thread →
  Coroutines migration, flow "Cek Update" perlu ditest ulang: check, download, install).
- `docs/archive/MANUAL_QA_CHECKLIST.md` — 0/19 item tercentang (audio focus, Bluetooth, lock-screen,
  headset kabel, process death, background playback jangka panjang).
- Overscroll bounce (`IosScrollPhysics.kt`, `Spring.DampingRatioNoBouncy`) belum dikonfirmasi
  device asli.
- `docs/archive/ROADMAP_LIQUID_GLASS_REDESIGN.md` & fling behavior 14/14 layar — **sudah final CLOSED**,
  bukan item terbuka.

## Aturan sesi aktif
1. Dilarang edit manual `versionCode`/`versionName` di `app/build.gradle.kts` — auto dari jumlah
   commit git. Tiap kirim ZIP wajib sebut nomor batch + ingatkan versionName pasti baru setelah
   `git push`.
2. Box code pesan commit WAJIB tampil di atas heading "Update Harian" tiap respons chat, isi
   penjelasan fitur singkat dari `CHANGELOG.md` batch itu — dilarang angka versi polos saja.
3. Prioritas versi/dependency/komponen paling mutakhir, bukan kompatibilitas OS lama — user tidak
   peduli dukungan Android <12/API 31. Jangan bikin/pertahankan fallback legacy kalau ada opsi
   modern lebih bersih. `minSdk` tidak pernah diubah otomatis — WAJIB konfirmasi eksplisit user.
4. `docs/archive/ARCHIVED_POLISH_AUDIT.md` / `docs/archive/ARCHIVED_MICRO_UIUX_AUDIT.md` = arsip, tidak aktif diikuti.
   `docs/archive/ROADMAP_LIQUID_GLASS_REDESIGN.md` = 100% tuntas, tidak ada item terbuka.
5. Nama folder Termux: `~/projects/audioplayer` (lowercase) — FINAL. `rootProject.name` tetap
   `"AudioPlayer"` (hardcoded `settings.gradle.kts`), tidak terikat nama folder/`git remote`.
6. Sektor DITUTUP — jangan proaktif dibuka ulang pada instruksi generik ("next"/"lanjut"); BOLEH
   dieksekusi kalau user beri instruksi eksplisit spesifik minta sektor ini dibuka lagi:
   - **Thread Safety I/O** — Batch 431: audit pola-blocking menyeluruh selesai untuk 70 file di
     luar `ui/` (KECUALI paket `update/`, waktu itu belum disentuh). Batch 432: paket `update/`
     (3 file) disisir — 1 gap ditemukan+fix (`UpdateManager.kt`, Thread→Coroutines), 2 file
     lainnya (`GitHubReleaseChecker.kt`/`UpdateDownloader.kt`) konfirmasi sudah benar. 0 item
     residual diketahui KECUALI pola di luar 9 kategori grep Batch 431 (mis. Room DAO
     non-suspend, kalau ada — belum dicek eksplisit).
   - **compileSdk/targetSdk** — final di targetSdk 36, compileSdk 36. 0 rencana Play Store,
     device user Android 16 (edge-to-edge/predictive back terverifikasi device asli). 0 item
     residual kecuali user eksplisit minta bump API 37 (blocked di migrasi AGP 9.x) atau ada
     temuan baru.
   - **Compose optimization** — `AlbumArt` TUNTAS Batch 429 (`SubcomposeAsyncImage` →
     `AsyncImage`), device asli konfirmasi render normal 0 regresi (Batch 430).
     `DuplicateFinderSheet.kt` `remember` CPU-heavy TUNTAS Batch 431 (pindah `LaunchedEffect` +
     `Dispatchers.Default`), belum diverifikasi device/CI. 0 utang teknis residual lain diketahui
     di sektor ini.

## Keputusan arsitektur utama
Ringkasan penuh + alasan: README.md § "Keputusan Arsitektur". Poin paling kritis:
- `PlaybackService` pakai `MediaLibraryService`, **bukan** `MediaSessionService` — prasyarat
  Playback Resumption resmi.
- `AppLogger` lokal murni (bukan Crashlytics/Sentry) — app tidak punya izin INTERNET sama
  sekali, bagian dari klaim privasinya.
- `PinLockoutPolicy` dipisah dari `AppLockStore` supaya bisa di-unit-test tanpa Context.
- File paling berisiko diubah tanpa cek dokumentasi dulu: `PlaybackService.kt`,
  `AppLockStore.kt`, `app/build.gradle.kts`.

## Struktur package (ringkas)
```
com.rudi.audioplayer/
├── data/      — Store & repository (SharedPreferences/MediaStore), model data (Song, Playlist,
│                SmartPlaylist — rule-based, resolve live via SmartPlaylistEngine)
├── playback/  — PlaybackService (MediaLibraryService), PlayerViewModel, Equalizer, ShakeDetector
├── ui/        — Semua Composable screen & sheet (Home, Library, NowPlaying, Settings, dst.)
├── ui/theme/  — Apple SYSTEM/LIGHT/DARK (utama) + Matte Noir (custom, kebalikan), warna, tipografi
├── util/      — AppLogger (log diagnostik lokal), ApkSignatureChecker
└── widget/    — Home screen widget (PlayerWidgetProvider, WidgetUpdater)
```

## Konvensi penamaan ZIP & versi
`AudioPlayer-batchN-release.zip` melacak nomor batch percakapan (bukan versionName/versionCode).
`versionCode`/`versionName` otomatis dari jumlah commit git. Detail lengkap: README.md §
"Standar Penomoran Versi".
