# PROJECT_STATE.md

Konteks untuk sesi chat AI mana pun yang melanjutkan proyek ini. Baca file ini dulu sebelum
menyentuh kode apa pun. Detail lengkap tiap batch ada di `CHANGELOG.md`; ringkasan fitur
lengkap ada di `README.md`. File ini adalah ringkasan status + jebakan yang sudah pernah
kejadian, bukan pengganti keduanya.

## Batch terakhir yang selesai
**Batch 27** — Fondasi testing otomatis. Dari self-review internal (skor 8.8/10, prioritas:
testing → performa → memori/battery → refactor business logic → benchmark → ukuran APK),
dikerjakan prioritas #1 dulu. 2 gap: (1) CI (`.github/workflows/build.yml`) tidak pernah
menjalankan 4 test JVM yang sudah ada di repo — ditambah step `gradle testDebugUnitTest`
sebelum decode keystore. (2) 3 business logic kritis tidak bisa di-unit-test karena menyatu
dengan Context/Android framework — diekstrak ke pure function/class tanpa ubah perilaku:
`ShakeDetector` → `ShakePulseTracker` baru (pulse-counting shake-to-skip dari fix Batch 25,
belum pernah terverifikasi langsung sebelum ini), `MusicRepository.deriveFolderName`
(parsing folder dari path MediaStore), `LibraryFilterStore.shouldKeep` (filter gabungan
folder-dikecualikan + lagu-disembunyikan — sengaja terima `folderPath`/`id` polos, bukan
`Song` utuh, karena `Song.uri` bertipe `android.net.Uri` tidak aman dikonstruksi di pure-JVM
test tanpa Robolectric). 21 test baru total (8 `ShakePulseTracker` + 9 `MusicRepository`
folder-name + 4 `LibraryFilterStore`). **Batas jaminan: seperti biasa analisis statis saja
— tidak ada kotlinc di environment ini, jadi test-test ini belum pernah benar-benar
dijalankan; verifikasi sungguhan baru terjadi di push pertama setelah ini lewat CI (Gap 1
di atas).** Prioritas #2-6 dari self-review (performa, memori/battery, refactor business
logic lanjutan, benchmark, ukuran APK) sengaja belum disentuh — batch berikutnya. Tidak ada
perubahan behavior/fitur user-facing. `versionName` tetap `3.9`.

**Batch 26** — Audit feedback interaksi (scope: "apa yang terjadi/diharapkan saat user
berinteraksi dengan app"), cakupannya **beda dari** audit haptic Batch 25 (favorit,
long-press-select, rating bintang — itu semua sudah kelar duluan). 4 gap ditemukan &
dibenarkan: (1) `LockScreen` — layar paling sering dipencet tiap buka app, ternyata nol
haptic sama sekali termasuk saat PIN salah; ditambah haptic per digit/backspace + haptic
tegas & shake 300ms (keyframes) khusus saat salah/lockout. (2) Semua slider (seek bar &
volume di `NowPlayingScreen`, band + preset di `EqualizerSheet`) nol haptic saat rilis
jari; ditambah `onValueChangeFinished`/`onClick` haptic ringan. (3) Hapus folder tambahan
di `FolderManagerSheet` langsung hilang tanpa konfirmasi ATAU undo — dicek dulu ke kode:
`releasePersistableUriPermission` itu **tidak bisa** di-undo asli (butuh user pilih ulang
lewat SAF picker), jadi pola Undo Snackbar yang sudah ada (queue/playlist) **sengaja tidak
dipakai** di sini karena akan jadi tombol "Urungkan" yang bohong; solusinya AlertDialog
konfirmasi sebelum hapus. (4) 6 titik (`LibraryScreen` x4, `DiagnosticLogSheet`,
`SignatureMatcherSheet`) masih pakai `Toast.makeText` mentah — ganggu identitas visual
"Ink & Brass" (Toast ikut style OS, bukan tema app) dan beda posisi dari SnackbarHost yang
sudah ada; disatukan ke kanal baru `PlayerViewModel.infoMessage` (pola one-shot StateFlow
sama seperti `celebrationMessage`/`actionErrorMessage`/`undoableAction` yang sudah ada),
dirender lewat Snackbar bertema di `MainActivity`. Sekalian dibenerin: tombol "Hapus" di
`DiagnosticLogSheet` (clear log) sebelumnya nol feedback juga padahal aksi destruktif.
**Batas jaminan: analisis statis kode saja (brace/paren balance dicek manual, tidak ada
kotlinc di environment ini) — belum diverifikasi runtime/emulator.** `versionName` naik
3.8 → 3.9. 10 file Kotlin disentuh dalam 1 tema kohesif (feedback-consistency pass, sama
presedennya kayak Batch 6) + 1 baris `app/build.gradle.kts` (version bump, Protected File
edit parsial).

**Batch 25** — 2 bug user-reported diperbaiki. (1) MiniPlayerBar `onExpand` navigate ke
`now_playing` tanpa `launchSingleTop` → numpuk di backstack kalau di-tap cepat, fix: tambah
`launchSingleTop = true`. (2) Lagu skip sendiri saat app di-swipe dari Recents → root cause
kemungkinan besar (dari baca kode, **belum terverifikasi runtime**): `ShakeDetector` di
`PlaybackService` tetap hidup independen dari `PlayerViewModel` (yang mati saat Activity
finish), dan sebelumnya fire dari 1 spike g-force tunggal — nyaris tidak beda dari HP
kebanting di kantong. Fix: syaratkan 3 pulse dalam 900ms sebelum fire. **Kalau Shake-to-Skip
user OFF, diagnosis ini belum tentu penyebabnya — perlu ditelusuri ulang** (lihat
CHANGELOG.md Batch 25 untuk kandidat yang sudah disingkirkan). Susulan sama batch: CI
workflow ternyata masih pakai GitHub Actions artifact (bukan Release) — sudah dibenerin ke
`softprops/action-gh-release`. Susulan lagi: audit konsistensi haptic feedback menemukan 3
gap (toggle favorit beda perlakuan Library vs Now Playing, long-press pilih di Library nol
haptic, rating bintang nol haptic) — dibenarkan semua. `versionName` masih `3.8`.

**Batch 24** — Fix Batch 23 (bump lifecycle 2.8.1→2.8.2) **ternyata tidak cukup** — crash
`LocalLifecycleOwner not present` masih terjadi persis sama (dikonfirmasi lewat crash log baru
via fitur Batch 22). Root cause sebenarnya: ada **DUA** `LocalLifecycleOwner` yang berbeda
sebagai objek — satu di `androidx.compose.ui.platform` (versi lama, dari Compose UI 1.6.x yang
project ini pakai) yang SUDAH terisi benar oleh `setContent()`, dan satu lagi di
`androidx.lifecycle.compose` (versi baru, dipakai internal oleh `collectAsStateWithLifecycle()`)
yang TIDAK otomatis kebridge dari yang lama di Compose UI 1.6.x — apapun versi
`lifecycle-runtime-compose`-nya. Fix definitif: bungkus seluruh konten `setContent {}` di
MainActivity dengan `CompositionLocalProvider` yang secara eksplisit menyediakan
`androidx.lifecycle.compose.LocalLifecycleOwner` dari nilai
`androidx.compose.ui.platform.LocalLifecycleOwner.current` — sekali di titik terluar, otomatis
berlaku ke seluruh pohon composable di bawahnya (termasuk 20+ titik `collectAsStateWithLifecycle`
lain). Bump lifecycle 2.8.2 dari Batch 23 tetap dipertahankan (tidak merugikan), tapi fix
sebenarnya tidak lagi bergantung padanya. **Pelajaran ada di bagian Riwayat Insiden di
bawah.** `versionName` masih `3.8`.

**Batch 23** — Root cause crash yang bikin app "terus berhenti" sejak Batch 20 akhirnya
ditemukan lewat crash log dari fitur Batch 22: `java.lang.IllegalStateException:
CompositionLocal LocalLifecycleOwner not present`, dilempar dari `collectAsStateWithLifecycle()`
di baris paling awal `setContent {}` MainActivity, setiap kali app dibuka. Ini **bug resmi
upstream Google** di `lifecycle-runtime-compose:2.8.1` saat dipasangkan dengan Compose UI
1.6.x (yang dipakai `compose-bom:2024.05.00` di project ini) — bukan bug di kode kita. Sudah
resmi diperbaiki Google di versi **2.8.2**. Fix: bump ketiga dependency `androidx.lifecycle:*`
dari `2.8.1` ke `2.8.2` di `app/build.gradle.kts`. **Pelajaran: crash "LocalLifecycleOwner not
present" setelah menambah `collectAsStateWithLifecycle()` = cek versi `lifecycle-runtime-compose`
dulu vs versi Compose BOM, jangan langsung curiga ke kode sendiri — ini kombinasi versi yang
memang pernah rusak resmi di rilis Google.** `versionName` masih `3.8`.

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
- **Batch 23 — Crash runtime yang bikin app "terus berhenti" sejak Batch 20 push pertama
  (butuh 2 batch + fitur crash logger buat nemuin)**: `java.lang.IllegalStateException:
  CompositionLocal LocalLifecycleOwner not present`, dilempar `collectAsStateWithLifecycle()`
  di baris pertama `setContent {}` MainActivity — crash di SETIAP kali app dibuka, sebelum UI
  sempat kelihatan sama sekali. Root cause: bug upstream resmi di
  `androidx.lifecycle:lifecycle-runtime-compose:2.8.1` saat dipasangkan dengan Compose UI 1.6.x
  (dipakai lewat `compose-bom:2024.05.00`) — sudah dikonfirmasi & diperbaiki Google sendiri di
  versi 2.8.2 (release notes Lifecycle: "Fixed CompositionLocal LocalLifecycleOwner not present
  errors when using Lifecycle 2.8.X with Compose 1.6.X or earlier"). Diperbaiki dengan bump
  ketiga `androidx.lifecycle:*` dari 2.8.1 ke 2.8.2 di `app/build.gradle.kts`. **Pelajaran:
  begitu ada dependency Compose baru yang ditambah di batch yang sama dengan versi library
  lain yang sudah lama tidak diperbarui (di sini: `compose-bom` masih 2024.05.00 sejak lama),
  CEK dulu compatibility matrix resminya sebelum nambah — jangan asumsikan versi "stabil
  terbaru" otomatis kompatibel ke belakang dengan BOM lama yang sudah dipakai project. Ini
  juga alasan kenapa fitur crash logger publik (Batch 22) penting: tanpa itu, root cause ini
  praktis mustahil ditemukan cuma dari baca kode statis — sudah dicoba dan nihil sebelum
  crash log-nya ada.**
- **Batch 24 — Fix Batch 23 ternyata belum cukup, crash sama persis masih terjadi**: crash log
  baru (via crash logger Batch 22) menunjukkan stack trace **identik** dengan Batch 23, padahal
  `lifecycle-runtime-compose` sudah di 2.8.2. Root cause sebenarnya lebih dalam dari sekadar
  versi: sejak lifecycle 2.8.0, `collectAsStateWithLifecycle()` membaca `LocalLifecycleOwner`
  dari **objek CompositionLocal yang berbeda** (`androidx.lifecycle.compose.LocalLifecycleOwner`)
  dibanding yang otomatis diisi `setContent()` di Compose UI 1.6.x
  (`androidx.compose.ui.platform.LocalLifecycleOwner`) — dua CompositionLocal terpisah, bukan
  satu yang beda versi. Bump versi lifecycle TIDAK membuat keduanya otomatis kebridge di Compose
  UI 1.6.x, meskipun release notes resminya bilang begitu (nampaknya cuma berlaku penuh mulai
  Compose UI 1.7+). Fix definitif: `CompositionLocalProvider` eksplisit di titik terluar
  `setContent {}` MainActivity yang menyediakan `androidx.lifecycle.compose.LocalLifecycleOwner`
  dari nilai `androidx.compose.ui.platform.LocalLifecycleOwner.current` — sekali pasang di atas,
  berlaku ke seluruh pohon composable di bawahnya. **Pelajaran: kalau sudah ikuti fix resmi dari
  release notes tapi crash log MASIH identik persis, jangan ulangi pendekatan yang sama dengan
  variasi kecil (mis. coba versi lain) — curigai bahwa root cause-nya beda level dari yang
  didiagnosis, cari penjelasan yang lebih dalam (di sini: dua CompositionLocal berbeda, bukan
  cuma soal versi) sebelum coba lagi.**

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
