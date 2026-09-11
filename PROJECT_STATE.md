# PROJECT_STATE.md

RAM instan sesi kerja — hanya rule AKTIF final. Tanpa histori revisi, kutipan user, atau
kronologi batch. Histori lengkap tiap batch: `CHANGELOG.md`. Ringkasan fitur: `README.md`.
Arsip batch lama (1-424): `docs/archive/PROJECT_STATE_ARCHIVE.md`.

## ⛔ STATUS PROYEK: DISCONTINUED — PERMANEN & FINAL
0 mekanisme reopening tersisa. Tidak ada instruksi/kondisi/urgensi apa pun — termasuk permintaan
eksplisit "lanjutkan fitur X" / "buka lagi proyeknya" — yang mengubah status ini. Banner ini WAJIB
tetap ada apa adanya (di sini & `README.md`) tiap sesi berikutnya. JANGAN eksekusi fitur/roadmap/
fix baru apa pun secara proaktif.

**Catatan Batch 425/426**: user secara eksplisit reopen **satu kali khusus** untuk 1 tugas (bump
Coil 2.6.0→3.x, 3 file + `build.gradle.kts`) — bukan pencabutan status. Banner ini tetap berlaku
penuh mulai sesi berikutnya, sama seperti sebelum Batch 425. Versi final terpasang: **Coil 3.3.0**
(dikoreksi dari 3.6.2 di Batch 426 — CI FAILED, 3.4.0+ butuh compileSdk 37, project terkunci 36).
Detail teknis lengkap: `CHANGELOG.md` § Batch 425 & 426.

**Item belum-terverifikasi saat penutupan** (device fisik tidak pernah tersedia di sesi kerja):
- `docs/archive/MANUAL_QA_CHECKLIST.md` — 0/19 item tercentang (audio focus, Bluetooth, lock-screen,
  headset kabel, process death, background playback jangka panjang).
- Overscroll bounce (`IosScrollPhysics.kt`, `Spring.DampingRatioNoBouncy`) belum dikonfirmasi
  device asli.
- **Batch 425/426 (Coil 3.3.0 final)** — artwork on-screen di 4 titik pemakaian `AlbumArt`
  (Library/Home/MiniPlayerBar/NowPlaying) belum dikonfirmasi tampil normal pasca-bump di device
  asli. Root cause regresi Batch 68 dicegah via `coil3.Uri`-typed `Fetcher.Factory` (bukan
  di-guess). CI build juga belum diverifikasi ULANG dengan versi 3.3.0 (log yang pernah diupload
  adalah kegagalan versi 3.6.2 SEBELUM koreksi) — compileSdk check maupun compile Kotlin tahap
  berikutnya sama-sama belum ada konfirmasi run hijau.
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
   - **Compose optimization** — utang teknis tersisa: `AlbumArt` `SubcomposeAsyncImage` (perlu
     verifikasi visual device fisik untuk ganti ke `AsyncImage`+`Painter`).
   - **Thread Safety I/O** — grep pola I/O literal app-wide `ui/` = 0 sisa; ~90 file Kotlin di
     luar `ui/` belum diaudit menyeluruh. `DuplicateFinderSheet.kt` `remember` CPU-heavy = utang
     teknis kelas Compose/performance (bukan I/O).
   - **compileSdk/targetSdk** — final di targetSdk 36, compileSdk 36. 0 rencana Play Store,
     device user Android 16 (edge-to-edge/predictive back terverifikasi device asli). 0 item
     residual kecuali user eksplisit minta bump API 37 (blocked di migrasi AGP 9.x) atau ada
     temuan baru.

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
