# PENDING_CodeTidyPlan.md — Rencana Perapihan Kode SONIX (berbasis Konstitusi Pipeline v2.9)

Dibuat Batch 505. **PROPOSAL — 0 item dieksekusi.** Tidak ada source Kotlin/Gradle/XML yang diubah
saat dokumen ini dibuat, dan tidak ada build/test yang dijalankan; semua angka di bawah hasil
`wc`/`grep`/baca source ZIP `AudioPlayer-main.zip` (snapshot Batch 504), BUKAN hasil runtime.
Nomor baris = snapshot; **verifikasi ulang ke ZIP terbaru sebelum eksekusi tiap item**.

Pelengkap `docs/PLANNING.md` (roadmap + audit Batch 504: cakupan test, Gradle Wrapper, temuan P0
`docs/archive/`) — isi di sini TIDAK menduplikasi itu. Fokus dokumen ini: **merapikan struktur
kode tanpa mengubah perilaku**, tiap item selaras P0 konstitusi (STABILITY + ZERO-REGRESSION).
Arsipkan dokumen ini ke `docs/archive/` HANYA setelah semua item DONE/dibatalkan eksplisit oleh user
(aturan Proactive Archiving) — P0 `docs/archive/` sudah beres (Batch 506).

## 1. Baseline Terverifikasi (Batch 505)

**Ukuran & konsentrasi kode**
- 113 file Kotlin `main` (27.116 baris), 16 unit test, 2 androidTest.
- 7 file >800 baris = 11.485 baris (**≈42% seluruh kode**): `NowPlayingScreen.kt` 2530,
  `MainActivity.kt` 2402, `PlayerViewModel.kt` 1853, `LibraryScreen.kt` 1571,
  `FloatingBubbleService.kt` 1300, `SettingsScreen.kt` 951, `PlaybackService.kt` 878.
- Fungsi monolit (≈, dari jarak antar deklarasi top-level): `NowPlayingScreen()` baris 149–1554
  (≈1.4k), `AppNavHost()` 1034–2357 di `MainActivity.kt` (≈1.3k), `SettingsScreen()` 60–648
  (≈590), `LibraryScreen()` 95–642 (≈550). `PlayerViewModel` = 110 fungsi dalam 1 class.

**Kepatuhan guard konstitusi (hasil grep app-wide) — yang sudah bersih, JANGAN diusik**
- 0 `runBlocking`, 0 `Thread.sleep`, 0 `GlobalScope`; 0 `collectAsState(` non-lifecycle vs 53
  `collectAsStateWithLifecycle`.
- 0 literal API key/secret/token/password; 0 `TODO/FIXME/XXX`; 0 `readBytes()/readByteArray()`.
- `AppLogger` memasang `Thread.setDefaultUncaughtExceptionHandler` (1 titik); update APK sudah
  streaming (`UpdateDownloader.kt`, chunked). 55 pemakaian `AppLogger.`.
- Hanya **2** non-null assertion `!!` nyata di kode (`LyricsSheet.kt:110`, `VaultSheet.kt:294`);
  sisa `!!` yang tertangkap grep mentah (IosScrollPhysics dst.) ada di komentar/string.

**Celah / kandidat (belum tentu bug — tiap butir ada status verifikasinya)**
1. **`rememberSaveable` = 0 pemakaian.** `<activity MainActivity>` di manifest TIDAK punya
   `configChanges` → Activity di-recreate saat rotasi. State transien UI dipegang `remember {
   mutableStateOf }` di ≥10 file (LibraryScreen 22, NowPlayingScreen 18, SettingsScreen 14,
   VaultSheet 13, SmartPlaylistScreen 12, PlaylistScreen 11, SongPickerSheet 8, …), termasuk 12
   flag dialog/sheet di `NowPlayingScreen.kt` 252–263 dan `selectedTab`/`selectedIds`/dialog di
   `LibraryScreen.kt` 133–148. Secara struktural tidak bertahan rotasi (guard "UI State &
   Lifecycle"). **Perilaku nyata BELUM diuji di device.** Perhatian: `selectedIds` bertipe
   `persistentSetOf<Long>()` → butuh `Saver` kustom.
2. **Kode duplikat — sleep-timer countdown** identik di `PlayerViewModel.kt` ≈336–342 (`init`)
   dan ≈1802–1808 (`startSleepTimer`).
3. **Dua parser LRC hidup berdampingan dalam satu layar**: `LyricsSheet.kt` memakai
   `LyricsStateView` → `parseLRC`/`activeLyricIndex` (`ui/lyrics/LyricsView.kt`) di baris 227,
   DAN `LyricsParser.parse` (`data/LyricsParser.kt`) di baris 249. Belum dibandingkan semantiknya.
4. **Komponen bersama salah rumah**: `EmptyState`, `ShimmerBrush`, `ShimmerRow`, `ShimmerList`
   didefinisikan di `LibraryScreen.kt` 1466–1571 tapi dipakai 8 file lain.
5. **`imePadding()` = 0 pemakaian**; ada 9 file dengan TextField (`LyricsSheet`, `SongPickerSheet`,
   `SmartPlaylistScreen`, `VaultSheet`, `LibraryScreen`, `SettingsScreen`, `PlaylistScreen`,
   `ABRepeatBookmarkSheet`, `SongInfoEditSheet`). Bisa saja sudah aman lewat `Scaffold`/
   `ModalBottomSheet` — **belum diaudit**, jadi bukan klaim bug.
6. 3 `Log.w` langsung melewati `AppLogger` (`PlaybackStateStore.kt:80`, `LibraryCacheStore.kt:82,127`;
   2 sisanya adalah pembungkus `AppLogger.kt` sendiri — wajar).
7. **Dokumen membengkak vs tujuannya**: `PROJECT_STATE.md` 2.826 baris/230 KB padahal headernya
   menyatakan "RAM instan… tanpa histori"; `CHANGELOG.md` 1,3 MB/17.359 baris. Komentar source
   merujuk 23× ke 3 dokumen `PENDING_*.md` (IosFlingBehavior 16, FixGlobalLagRecomposition 5,
   RatingEntryPoint 2) yang **tidak ditemukan di ZIP ini** (lokasi asli tidak bisa diverifikasi).

## 2. Batas Keras (berlaku untuk SEMUA item di bawah)

- **Move-only untuk pemecahan file**: badan fungsi dipindah identik karakter-per-karakter. Yang
  boleh berubah hanya `private`→`internal`, import, dan lokasi file (package TETAP sama). Bukti
  wajib: diff yang menunjukkan blok pindahan sama persis.
- **1 batch = 1 target logis, maks 3–5 file source** (pemindahan = file sumber + 1 file baru = 2).
- **Dikecualikan — JANGAN disentuh tanpa instruksi eksplisit user**: `PlaybackService.kt`,
  `AppLockStore.kt`, `app/build.gradle.kts` (dicatat "paling berisiko" di `PROJECT_STATE.md`);
  `FloatingBubbleService.kt` (sektor bubble/Roadmap #11, riwayat item landscape-clip Batch
  460–464 — status akhir belum saya verifikasi, cek entri terbaru dulu); sektor bottom nav
  (stabil, riwayat regresi berulang) kecuali T10 dengan syarat ketat; sektor DITUTUP (Thread
  Safety I/O, compileSdk/targetSdk, Compose optimization) — tidak dibuka ulang oleh dokumen ini.
- Tidak ada refactor logika, ganti dependency, redesign, atau migrasi. Tidak ada perubahan
  minSdk/versionName/versionCode/signing/R8.
- **Build hijau ≠ behavior verified.** CI (`testDebugUnitTest assembleRelease`) = gerbang
  kompilasi; perilaku diverifikasi device oleh user per checklist wave. Jangan klaim "100%
  verified" tanpa itu.

## 3. Rencana per Wave

Status: `BELUM` = belum dikerjakan. Risiko: R1 rendah, R2 sedang, R3 tinggi.

### Wave 0 — Gerbang (tanpa kode)
| ID | Aksi | Pemilik | Status |
|----|------|---------|--------|
| G1 | Selesaikan P0 `docs/archive/` (`docs/PLANNING.md` §1) sebelum `[DAILY UPDATE]` berikutnya | User | **SELESAI Batch 506** (dipulihkan dari git, ikut ZIP v506) |
| G2 | Konfirmasi CI baseline hijau di commit terakhir sebelum wave pertama | User | BELUM |
| G3 | Pilih wave/item yang dieksekusi (dokumen ini tidak jalan otomatis) | User | **SELESAI Batch 507** (user: mulai dari yang low-risk → Wave 1 T1+T2) |

### Wave 1 — Rapikan kecil (R1)
| ID | Item | File | Validasi |
|----|------|------|----------|
| T1 | Pindah `EmptyState`/`ShimmerBrush`/`ShimmerRow`/`ShimmerList` ke `ui/SharedComponents.kt` (paket sama → 0 ubah import di paket `ui`; cek `ui/theme/TactileDepth.kt` yang paketnya beda) | `LibraryScreen.kt` + baru | CI compile; buka Library kosong/loading + 2 layar lain pemakai |
| T2 | Ekstrak helper privat `startSleepTimerCountdown(endAt)` menggantikan 2 loop identik | `PlayerViewModel.kt` | CI; set timer → hitung mundur → batal; restart app saat timer aktif → hitungan pulih |
| T3 | (Opsional) arahkan 3 `Log.w` ke `AppLogger` — perhatian: kegagalan yang tadinya senyap kini masuk log diagnostik | `PlaybackStateStore.kt`, `LibraryCacheStore.kt` | CI; Log Diagnostik tidak banjir saat start normal — **KODE SELESAI Batch 524** (pakai `AppLogger.e` agar stack trace terjaga; belum diverifikasi CI/device) |

**Status Wave 1 (Batch 507)**: T1 KODE SELESAI, T2 KODE SELESAI — keduanya belum diverifikasi
CI/device (0 build dijalankan saat pengerjaan; G2 CI baseline juga belum dikonfirmasi user).
T3 BELUM (opsional, mengubah perilaku log — tunggu persetujuan terpisah).
**Update Batch 508**: user melaporkan "wave 1 sudah berhasil" (rincian CI vs device tidak dirinci).
T3 tetap BELUM.
**Update Batch 524**: user minta "next kandidat low-risk" -> **T3 KODE SELESAI** (2 file: `PlaybackStateStore.kt`, `LibraryCacheStore.kt`; `AppLogger.e` dipilih
karena `AppLogger.w` tanpa Throwable). Belum diverifikasi CI/device (0 build dijalankan). **Wave 1 T1-T3 semuanya KODE SELESAI.**

### Wave 2 — Pecah file besar, move-only (R2)
Estimasi kasar pengurangan (bukan janji): NowPlayingScreen ≈2530→≈1730, MainActivity ≈2402→≈1860,
SettingsScreen ≈951→≈650, LibraryScreen ≈1571→≈1020.
| ID | Pindahkan → file baru | Baris ≈ |
|----|------------------------|---------|
| T4 | `SleepTimerDialog`, `SpeedDialog`, `RatingDialog`, `TransitionModeOption` → `NowPlayingDialogs.kt` | 2251–2530 |
| T5 | `AdvancedControlsSheet` + `AdvancedControlsSectionHeader` + `AdvancedControlRow` → `AdvancedControlsSheet.kt` | 1641–1853 |
| T6 | `AlbumArtHero` → `AlbumArtHero.kt` — **periksa dulu apakah berisi gesture/brightness/volume; kalau ya, turun ke R3** | 1941–2250 |
| T7 | `ThemeModeToggleSection`, `ThemeOptionCard`, `AppLockSection`, `SetPinDialog` → `SettingsSections.kt` | 649–951 |
| T8a | `SongRow`, `SongListView`, `GroupedListView`, `AlbumGridView` → `LibraryLists.kt` | 643–703, 1045–1465 |
| T8b | `LibraryHeader`, `LibrarySearchField`, `LibraryFilterChips`, `SearchHistoryView`, `SearchSectionLabel`, `SearchResultsView` → `LibrarySearch.kt` | 745–1044 |
| T9 | `WelcomeScreen`, `WelcomeHighlight`, `PermissionRationale` → `OnboardingScreens.kt` | 540–677, 2358–2402 |
| T10 | **(R3, terakhir)** `MagnifyingTabLabel`, `GlassTabIcon`, `NoRippleIndication`, `CustomNavBarTabItem` → `BottomNavBar.kt`. Wajib byte-identik; `AppNavHost` TIDAK dipecah | 678–1033 |
**Status Wave 2 (Batch 508)**: T4 KODE SELESAI (2 file: `NowPlayingScreen.kt` + baru
`NowPlayingDialogs.kt`, 281 baris move-only). **Update Batch 509**: user melaporkan "wave 2 nol
regression nyata" (rincian CI vs device tidak dirinci; ditafsirkan = T4).
**Status Wave 2 (Batch 509)**: T5 KODE SELESAI (2 file: `NowPlayingScreen.kt` + baru
`AdvancedControlsSheet.kt`, 208 baris move-only) — belum diverifikasi CI/device. T6–T10 BELUM.
**Update Batch 510**: user melaporkan T5 "nol regression (I guess)" — tentatif, rincian CI vs device
tidak dirinci.
**Status Wave 2 (Batch 510)**: T6 KODE SELESAI, diturunkan ke **R3** (blok berisi gesture swipe
horizontal; brightness/volume TIDAK di blok) — 2 file: `NowPlayingScreen.kt` + baru `AlbumArtHero.kt`,
315 baris move-only byte-identik (hanya `private`→`internal`). Belum diverifikasi CI/device. T7–T10 BELUM.
**Update Batch 511**: user melaporkan v510 (T6) — satu-satunya regresi nyata = sektor haptic feedback (sistemik,
BUKAN akibat pemindahan T6; call site identik v509). Wave 2 DITAHAN; Batch 511 = perbaikan haptic terpusat di luar
daftar T (3 file: `AppHaptics.kt` baru, `Theme.kt`, `AndroidManifest.xml`). T7 dilanjutkan setelah user
memutuskan hasil haptic.
**Update Batch 515**: user memutuskan haptic v514 "udah bagus" -> penahanan Wave 2 dicabut.
**Status Wave 2 (Batch 515)**: T7 KODE SELESAI — 2 file: `SettingsScreen.kt` (951->642 baris) + baru `SettingsSections.kt`,
308 baris move-only (hanya `private`->`internal` pada 4 fungsi). Belum diverifikasi CI/device (0 build dijalankan).
T8a-T10 BELUM.
**Update Batch 516**: user melaporkan v515 (T7) "aman terkendali" (tentatif; rincian CI vs device tidak dirinci), dengan 2 bug di
luar T7 (prompt sidik jari berulang; gesture pilih-lagu) — bukan akibat pemindahan T7. Batch 516 = fix 2 bug itu (3 file, di luar
daftar T); T8a dilanjutkan setelah hasilnya dikonfirmasi user.
**Update Batch 517**: user melaporkan v516 "2 bug telah teratasi" (rincian CI vs device tidak dirinci) -> T8a dilanjutkan.
**Status Wave 2 (Batch 517)**: T8a KODE SELESAI — 2 file: `LibraryScreen.kt` (1460->970 baris) + baru `LibraryLists.kt`, 489 baris
move-only (hanya `private`->`internal` pada 4 fungsi; nomor baris tabel di atas sudah bergeser sejak T1, blok nyata = 631-690 &
1033-1460). Belum diverifikasi CI/device (0 build dijalankan). T8b-T10 BELUM.
**Update Batch 518**: user melaporkan v517 "zero diff" (rincian CI vs device tidak dirinci) -> T8b dilanjutkan.
**Status Wave 2 (Batch 518)**: T8b KODE SELESAI — 2 file: `LibraryScreen.kt` (970->670 baris) + baru `LibrarySearch.kt`, 299 baris
move-only (hanya `private`->`internal` pada 5 fungsi; `SearchSectionLabel` + 2 `val` label tab tetap private; `SelectionActionBar`
TIDAK termasuk T8b dan tetap di `LibraryScreen.kt`). Belum diverifikasi CI/device (0 build dijalankan). T9-T10 BELUM.
**Update Batch 519**: user melaporkan v518 "tetap zero diff" (rincian CI vs device tidak dirinci) -> T9 dilanjutkan.
**Status Wave 2 (Batch 519)**: T9 KODE SELESAI — 2 file: `MainActivity.kt` (2414->2272 baris) + baru `OnboardingScreens.kt` (paket ROOT
`com.rudi.audioplayer`, bukan `ui`), 140 baris move-only (hanya `private`->`internal` pada `WelcomeScreen` + `PermissionRationale`;
`WelcomeHighlight` tetap private). Blok nyata = 551-644 & 2369-2414 (nomor tabel di atas sudah bergeser). Belum diverifikasi
CI/device (0 build dijalankan). T10 BELUM (R3, terakhir).
**Update Batch 520**: user melaporkan v519 "another zero diff" (rincian CI vs device tidak dirinci) -> T10 dilanjutkan.
**Status Wave 2 (Batch 520)**: T10 KODE SELESAI (R3) — 2 file: `MainActivity.kt` (2272->1873 baris) + baru `BottomNavBar.kt` (paket ROOT
`com.rudi.audioplayer`), 398 baris move-only byte-identik (hanya `private`->`internal` pada `GlassTabIcon` + `CustomNavBarTabItem`;
`MagnifyingTabLabel` + `NoRippleIndication` tetap private; `NoRippleIndication` = kode mati, 0 pemakai). `AppNavHost` TIDAK dipecah.
Blok nyata = 551-948 (nomor tabel di atas sudah bergeser). Belum diverifikasi CI/device (0 build dijalankan). **Semua T4-T10 Wave 2
KODE SELESAI**; sisa Wave 2 = verifikasi CI/device T10. Belum diarsipkan: Wave 3-5, T3, G2 masih BELUM.
**Update Batch 521**: user melaporkan v520 (T10) "regression nyata ... cuman performance yang downgrade" (gejala tidak dirinci) ->
**T10 DITARIK KEMBALI**, source `app/` = v519 utuh (`BottomNavBar.kt` dihapus), **Wave 2 DIHENTIKAN** (plan §5.6). Akar masalah BELUM
diketahui (analisis statis: 0 mekanisme jelas; kandidat: dampak build R8/DEX, atau efek instal-baru tanpa baseline profile — HIPOTESIS).
v521 = eksperimen pembeda (kode = v519). **Status akhir Wave 2: T4-T9 KODE SELESAI; T10 DIBATALKAN/DITAHAN** — tidak diulang tanpa
data pengukuran performa. Konsekuensi: blok bottom nav tetap di `MainActivity.kt` (AppNavHost + 4 simbol), tidak ada pemecahan lagi.
**Update Batch 522**: user atas v521: performa turun v520 "mungkin hanya ... hardware device saya yang over heat" (dugaan user, tidak
terverifikasi) -> penahanan T10 DICABUT, **T10 dieksekusi ulang**. **Status Wave 2 (Batch 522)**: T10 KODE SELESAI (R3, eksekusi ke-2) — 2 file:
`MainActivity.kt` (2272->1873 baris) + baru `BottomNavBar.kt` (paket ROOT), 398 baris move-only byte-identik (hanya `private`->`internal`
pada `GlassTabIcon` + `CustomNavBarTabItem`; hasil `MainActivity.kt` identik dengan v520). `AppNavHost` TIDAK dipecah. Belum diverifikasi
CI/device (0 build dijalankan). **Semua T4-T10 Wave 2 KODE SELESAI**; sisa Wave 2 = verifikasi CI/device T10 (+ bandingkan performa pada suhu
setara). Wave 3-5, T3, G2 BELUM.
**Update Batch 523**: user melaporkan v522 "another zero diff" (rincian CI vs device tidak dirinci) -> T10 dianggap nol regresi; **Wave 2 T4-T10 SELESAI**.
Verifikasi CI/device rinci T10 tetap di tangan user (G2 belum). User memilih Wave 4 D1-D3 (bukan Wave 3) -> dikerjakan Batch 523 (dokumen saja, 0 source).
Tiap T: 2 file disentuh, diff move-only, CI hijau, lalu device smoke test layar terkait (T10:
seret tab-bar, tap tab, label tidak ellipsis, font besar, 0 regresi minimize/expand — daftar
lengkap di `PROJECT_STATE.md` sektor bottom nav).

### Wave 3 — Kepatuhan guard yang mengubah perilaku terlihat (R2–R3, butuh persetujuan per item)
| ID | Item | Catatan |
|----|------|---------|
| T11 | `rememberSaveable` untuk state transien (dialog/sheet/tab/scalar sederhana), **1 layar per batch**: mulai `NowPlayingScreen.kt` 252–263, lalu `LibraryScreen.kt` (`selectedIds` pakai `Saver`) | Uji rotasi dengan dialog terbuka; putuskan dulu apakah sheet berat (Equalizer/Visualizer) memang boleh terbuka lagi setelah rotasi |
| T12 | Audit IME/insets di 9 file TextField (§1.5) — **baca saja dulu**, perbaikan hanya jika terbukti bermasalah | Uji keyboard terbuka + font scale besar + landscape |
| T13 | Bandingkan semantik `parseLRC` vs `LyricsParser` (format timestamp, multi-timestamp, offset); gabung HANYA jika identik, kalau beda dokumentasikan & biarkan | Analisis dulu, 0 kode sampai hasilnya jelas |

### Wave 4 — Dokumen (G1 SELESAI Batch 506; **D1-D3 SELESAI Batch 523**; tetap butuh pilihan user, tidak jalan otomatis)
| ID | Item | Catatan |
|----|------|---------|
| D1 | Pangkas `PROJECT_STATE.md` ke rule aktif; pindahkan detail batch ≤503 ke `docs/archive/PROJECT_STATE_ARCHIVE.md` | Arsip asli sudah dipulihkan (Batch 506) — TAMBAHKAN ke file itu, jangan timpa — **SELESAI Batch 523**: Catatan Batch 425-503 + entri RESUME <=502 dipindah VERBATIM (byte-identik, dicek `cmp`); `PROJECT_STATE.md` 3148 -> 461 baris; batch 504-522 tetap; arsip lama utuh. Batch 220-424 tidak ada di arsip (hanya `CHANGELOG.md`) |
| D2 | Selaraskan `FILE_MANIFEST.txt` dengan isi ZIP nyata tiap batch | Catat file baru + selisih hitungan — **SELESAI Batch 523**: 206 path = 204 file ZIP + 2 dotfile sengaja, 0 drift |
| D3 | Rujukan komentar ke `PENDING_*.md` yatim (§1.7): cukup catat pointer di README/PLANNING — jangan edit 23 komentar di ~17 file (scope creep) | **SELESAI Batch 523**: pointer di `docs/PLANNING.md` §5; nyata 23 rujukan di 14 file (bukan ~17), 0 komentar diedit |

### Wave 5 — Opsional & berisiko tinggi (R3, HANYA dengan instruksi eksplisit)
Pecah badan `NowPlayingScreen()` / `AppNavHost()` menjadi sub-composable. Menyentuh closure
gesture/state; **prasyarat**: Compose UI Test sektor terkait (Roadmap A di `docs/PLANNING.md`)
sudah ada. Tanpa itu, jangan dijalankan.

## 4. Catatan Observasi (TIDAK dijadwalkan)
- `PlayerViewModel.startPositionLoop()` (≈baris 1066) = loop `while(true)`/`delay(1000)` seumur
  ViewModel, jalan selama `controller != null` (tanpa cek `isPlaying`/foreground). Guard baterai
  konstitusi melarang polling tak berujung tanpa alasan kuat; alasannya ada (progress bar,
  loop-back A-B Repeat, persist ±5 dtk — Batch 352/353). Mengetatkannya = **perubahan perilaku**
  (A-B Repeat bisa berhenti saat layar mati) dan menyentuh sektor Thread Safety yang DITUTUP —
  hanya jika user meminta eksplisit.
- Marker seksi "Visualizer Audio" muncul dua kali di `PlayerViewModel.kt` (baris 244 dan 1669).
  Belum dicek apakah itu dua blok terpisah atau sisa duplikasi label.

## 5. Protokol Tiap Batch Eksekusi
1. Cold start: `PROJECT_STATE.md` → `[RESUME POINT]`; ZIP terbaru = source of truth.
2. Verifikasi ulang nomor baris & pemakai simbol (`grep`) sebelum memindah apa pun.
3. Edit minimum sesuai ID; catat file yang disentuh (maks 3–5; dokumen VIP tak dihitung).
4. Validasi berurutan: syntax/references → CI compile+unit test → behavior device → regresi →
   integritas ZIP (hitung file vs `FILE_MANIFEST.txt`) → docs.
5. Perbarui `FILE_MANIFEST.txt` (file baru), `CHANGELOG.md`, `PROJECT_STATE.md`; ubah status di
   tabel dokumen ini.
6. CI merah / regresi device → kembali ke ZIP batch sebelumnya, hentikan wave, jangan lanjut.

## 6. Definisi Selesai (per item)
Intent terimplementasi + 0 scope creep + CI hijau + perilaku terdampak dicek device (atau
ditandai jujur "belum diverifikasi device") + docs/manifest terbarui + ZIP terkemas + 0 secret
bocor + kontinuitas git terjaga.
