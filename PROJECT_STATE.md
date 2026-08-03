# PROJECT_STATE.md

Konteks untuk sesi chat AI mana pun yang melanjutkan proyek ini. Baca file ini dulu sebelum
menyentuh kode apa pun. Detail lengkap tiap batch ada di `CHANGELOG.md`; ringkasan fitur
lengkap ada di `README.md`. File ini adalah ringkasan status + jebakan yang sudah pernah
kejadian, bukan pengganti keduanya.

## Batch terakhir yang selesai
**Batch 22** — Fitur baru: crash logger ke folder publik. Saat crash fatal, `AppLogger`
sekarang juga menulis salinan stack trace ke `Documents/AudioPlayer/logs/crash_<waktu>.txt`
lewat MediaStore (API 29+, tanpa izin storage tambahan) — supaya bisa diambil pakai File
Manager biasa tanpa ADB/root, khusus untuk kasus app tidak bisa dibuka sama sekali. Log
diagnostik privat yang lama (`Settings → Lanjutan`) tidak diubah, tetap jalan seperti biasa.
`versionName` masih `3.8`.

**Batch 21** — Hotfix build gagal dari Batch 20, 2 root cause terpisah ditemukan lewat 2 kali
log CI: (1) `app/compose_stability_config.conf` pakai komentar `#`, parser
`stabilityConfigurationPath` cuma mengenali `//` — baris `#` dibaca sebagai pattern tidak
valid; (2) `LibraryScreen.kt` baris 115, operator `selectedIds - id` / `selectedIds + id`
resolve ke `kotlin.collections.Set` bawaan (bukan versi `kotlinx.collections.immutable`) karena
tidak ada import operator yang tepat, hasilnya `Set<Long>` bukan `PersistentSet<Long>` yang
diharapkan — diganti ke method `.remove(id)`/`.add(id)` bawaan `PersistentSet` (lebih aman,
tidak tergantung import operator). Tidak ada perubahan behavior/fitur. `versionName` masih
`3.8`.

**Batch 20** — Audit null-safety (state collection) & performa (recomposition list panjang).
Null-safety hasilnya bersih. Performa: 2 temuan nyata diperbaiki — `favoriteIds`/`selectedIds`
di `LibraryScreen` pindah ke `ImmutableSet<Long>` (kotlinx.collections.immutable), dan
`android.net.Uri` ditandai stable lewat Compose stability config karena bikin `Song` unstable.
Ditambah: `collectAsState()` → `collectAsStateWithLifecycle()` di seluruh `MainActivity`, dan
2 pemanggilan `sortedBy`/`sorted` yang belum di-`remember` di `LibraryScreen` dirapikan.
`versionName` masih `3.8` (batch maintenance, bukan rilis fitur baru).

## Riwayat insiden kronologis (jangan dihapus)
Ditulis supaya kesalahan yang sama tidak terulang di sesi baru yang tidak tahu konteksnya.

- **Batch 7** — Bug reorder-key Queue: key lama gabungan id+posisi merusak animasi tiap
  reorder karena terikat ke posisi, bukan identitas lagu.
- **Batch 9** — Bug yang sama ternyata ada juga di PlaylistScreen. Terpisah, snackbar Undo
  ternyata actionLabel-nya hardcode `null` di kode lama sehingga tombol aksi tidak pernah
  muncul sama sekali.
- **Batch 10** — Notifikasi cold-start "Memuat lagu..." bisa macet berjam-jam: proses
  pemulihan antrean tidak punya try/catch, jadi kalau gagal (izin dicabut, lagu terhapus,
  dll) kode pembersih notifikasi tidak pernah kesampaian.
- **Batch 11** — Perbaikan `onTaskRemoved` di Batch 10 ternyata **tidak cukup** — Media3
  punya timeout internal 10 menit saat jeda yang di luar kendali `onTaskRemoved`. Solusi
  sesungguhnya baru datang di Batch 12 (Playback Resumption resmi).
- **Batch 12** — Migrasi `MediaSessionService` → `MediaLibraryService` (prasyarat resmi untuk
  Playback Resumption). Ini titik migrasi arsitektur paling signifikan di proyek ini.
- **Batch 14** — Hotfix build error dari Batch 12: `MediaLibrarySession` ternyata nested di
  dalam `MediaLibraryService` (`MediaLibraryService.MediaLibrarySession`), bukan class
  top-level seperti yang diasumsikan di Batch 12. **Pelajaran: jangan tebak-tebak API Media3,
  cek langsung ke source code resmi androidx/media sebelum menulis kode yang menyentuhnya.**
- **Batch 15** — Ditambah komentar level-file di `PlaybackService.kt` yang menunjuk balik ke
  insiden Batch 10-14, supaya sesi baru mana pun otomatis kebaca peringatannya.
- **Batch 16** — Audit menemukan `addCustomFolder` gagal ambil izin folder (SecurityException)
  sebelumnya `return` polos tanpa penjelasan apa pun ke user — sudah diperbaiki (lihat
  CHANGELOG.md).
- **Batch 17** — README sempat tertinggal dari kode selama beberapa batch: fitur Kilas Balik
  dan Shake-to-Skip sudah lama terimplementasi penuh (termasuk toggle setting) tapi baru
  tercatat di README di batch ini. **Pelajaran: fitur baru wajib langsung masuk README di
  batch yang sama saat diimplementasikan, jangan ditunda.**
- **Batch 21 — Insiden pertama (build)**: file konfigurasi baru di Batch 20
  (`compose_stability_config.conf`) pakai komentar bergaya `#`, tapi parser compiler plugin
  `stabilityConfigurationPath` cuma mengenali `//`. Baris `#` dibaca sebagai pattern class,
  bukan komentar, dan gagal validasi ("is not a valid pattern") — build CI gagal total.
  **Pelajaran: file konfigurasi non-standar seperti ini tidak divalidasi compiler saat
  ditulis (bukan kode Kotlin biasa), jadi format syntax-nya wajib dicek ke referensi
  resmi/proyek AOSP dulu sebelum dianggap benar, bukan ditebak dari kebiasaan format komentar
  bahasa lain.**
- **Batch 21 — Insiden kedua (proses, bukan kode)**: percobaan pertama update via Termux
  gagal *bukan* karena kode, tapi karena command "Update Harian" lama meng-unzip ke folder
  induk (`~/projects/`) dengan asumsi ZIP-nya membungkus semua file dalam satu folder
  `AudioPlayer/`. Konvensi ZIP proyek ini justru sebaliknya (tanpa folder pembungkus, file
  langsung di root ZIP) — jadi seluruh isi nyasar ke `~/projects/app/...` dkk, bukan
  `~/projects/AudioPlayer/app/...`. Safety-check jumlah file (fallback tanpa manifest) benar
  mendeteksi ini sebagai file-drop besar dan rollback otomatis — repo tidak rusak, tapi update
  tidak masuk. **Pelajaran: command Update Harian/Inisialisasi wajib unzip langsung ke
  direktori project yang sudah di-`cd`, BUKAN ke folder induknya** — konvensi "ZIP tanpa
  folder pembungkus" mengharuskan ini, jangan diasumsikan sebaliknya lagi.
- **Batch 21 — Insiden ketiga (build lagi, kode)**: setelah insiden pertama beres, build maju
  sampai `compileReleaseKotlin` lalu gagal beda error: `LibraryScreen.kt:115`
  `selectedIds - id` / `selectedIds + id` inferred `Set<Long>`, padahal `selectedIds`
  bertipe `PersistentSet<Long>`. Sebabnya: operator `+`/`-` versi
  `kotlinx.collections.immutable` yang tipe-nya benar HANYA aktif kalau di-import eksplisit
  (lihat README resmi library-nya) — tanpa itu, Kotlin fallback ke operator `+`/`-` bawaan
  `kotlin.collections.Set` yang selalu mengembalikan `Set<T>` polos, bukan tipe konkretnya.
  Diperbaiki pakai method `.add()`/`.remove()` bawaan `PersistentSet` (dideklarasikan return
  `PersistentSet<E>` langsung), bukan menambah import operator. **Pelajaran: kalau kerja
  dengan `PersistentList`/`PersistentSet` dari kotlinx.collections.immutable, jangan pakai
  operator `+`/`-`, langsung `.add()`/`.remove()`/`.addAll()`/`.removeAll()` — bebas dari
  jebakan resolusi operator ini sama sekali, tidak perlu diingat-ingat importnya.**

## Keputusan arsitektur utama
Ringkasan penuh + alasan ada di README.md § "Keputusan Arsitektur". Poin paling kritis:
- `PlaybackService` pakai `MediaLibraryService`, **bukan** `MediaSessionService` — prasyarat
  Playback Resumption resmi.
- `AppLogger` lokal murni (bukan Crashlytics/Sentry) — app ini sengaja tidak punya izin
  INTERNET sama sekali, itu bagian dari klaim privasinya.
- `PinLockoutPolicy` dipisah dari `AppLockStore` supaya bisa di-unit-test tanpa Context.
- File paling berisiko untuk diubah tanpa cek dokumentasi dulu: `PlaybackService.kt`,
  `AppLockStore.kt`, `app/build.gradle.kts`.

## Struktur package (ringkas)
```
com.rudi.audioplayer/
├── data/      — Store & repository (SharedPreferences/MediaStore), model data (Song, Playlist)
├── playback/  — PlaybackService (MediaLibraryService), PlayerViewModel, Equalizer, ShakeDetector
├── ui/        — Semua Composable screen & sheet (Home, Library, NowPlaying, Settings, dst.)
├── ui/theme/  — 3 tema (Ink & Brass, Midnight Bloom, Paper & Ink), warna, tipografi
├── util/      — AppLogger (log diagnostik lokal), ApkSignatureChecker
└── widget/    — Home screen widget (PlayerWidgetProvider, WidgetUpdater)
```

## Konvensi penamaan ZIP & versi
`AudioPlayer-batchN-release.zip` melacak nomor batch percakapan (bukan `versionName`).
`versionCode` naik otomatis mengikuti jumlah commit git. `versionName` dibump manual hanya
di titik rilis yang dianggap layak, bukan tiap batch. Detail lengkap di README.md §
"Standar Penomoran Versi".
