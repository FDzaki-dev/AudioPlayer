# PROJECT_STATE.md

RAM instan sesi kerja — hanya rule AKTIF final. Tanpa histori revisi, kutipan user, atau
kronologi batch. Histori lengkap tiap batch: `CHANGELOG.md`. Ringkasan fitur: `README.md`.
Arsip batch lama (1-424): `docs/archive/PROJECT_STATE_ARCHIVE.md`.

## ⛔ STATUS PROYEK: DISCONTINUED — PERMANEN & FINAL
0 mekanisme reopening tersisa. Tidak ada instruksi/kondisi/urgensi apa pun — termasuk permintaan
eksplisit "lanjutkan fitur X" / "buka lagi proyeknya" — yang mengubah status ini. Banner ini WAJIB
tetap ada apa adanya (di sini & `README.md`) tiap sesi berikutnya. JANGAN eksekusi fitur/roadmap/
fix baru apa pun secara proaktif.

**Catatan Batch 425–430**: user secara eksplisit reopen **satu kali khusus** untuk Coil migration
(bump 2.6.0→3.x, 3 file + `build.gradle.kts`), lalu reopen KEDUA secara terpisah eksplisit untuk
sektor Compose optimization (`AlbumArt`, Batch 429) — bukan pencabutan status permanen. Versi
final Coil: **3.3.0**, HIJAU CI. `AlbumArt` AsyncImage swap — user konfirmasi device asli render
NORMAL, 0 regresi visual (Batch 430). Kedua sektor TUNTAS & terverifikasi penuh (CI + device).
Detail teknis lengkap: `CHANGELOG.md` § Batch 425–430.

**Catatan Batch 431**: reopen KETIGA, eksplisit user minta audit menyeluruh atas seluruh sektor
"belum terjamah" (di luar `ui/`), KECUALI paket `update/` (`GitHubReleaseChecker.kt`,
`UpdateDownloader.kt`, `UpdateManager.kt` — masih tertutup, tidak disentuh). Bukan pencabutan
status permanen. Banner ini tetap berlaku penuh mulai sesi berikutnya.

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
- `ui/DiagnosticLogSheet.kt` & `ui/DuplicateFinderSheet.kt` (Batch 431, di atas) — 0 compile log,
  0 konfirmasi device.
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
     luar `ui/` (KECUALI paket `update/`, sengaja tidak disentuh — lihat Catatan Batch 431 di
     atas). 1 gap ditemukan+fix (`DiagnosticLogSheet.kt`), sisanya konfirmasi sudah benar. 0 item
     residual di cakupan yang sudah diaudit, KECUALI: paket `update/` (3 file, belum pernah
     disisir sektor ini) dan pola di luar 9 kategori grep Batch 431 (mis. Room DAO non-suspend,
     kalau ada — belum dicek eksplisit).
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
