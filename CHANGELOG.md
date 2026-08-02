# Changelog

Satu entri per batch pengembangan. Ditulis supaya sesi chat AI yang baru (atau siapa pun
yang baru gabung ke proyek ini) bisa langsung baca file ini dan tahu histori keputusan,
tanpa harus gali `git log` atau riwayat chat lama yang sudah tidak bisa diakses lagi.

Catatan: pencatatan per-batch di file ini baru dimulai dari Batch 5. Semua hasil kerja
sebelum itu (fondasi awal: scan MediaStore, playback dasar via Media3, UI Library/Home/Now
Playing/Settings, dst.) sudah terangkum sebagai satu kesatuan di daftar fitur pada
`README.md` — tidak dipecah ulang per batch di sini karena detail per-batch-nya sudah tidak
tersedia.

## Batch 17 — Sinkron dokumentasi + penamaan artifact CI
- README diperbarui: dua fitur yang sudah lama terimplementasi penuh (termasuk toggle di
  Pengaturan) tapi belum pernah tercatat — **Kilas Balik** (bagian Beranda yang menampilkan
  lagu yang didengar persis 1 tahun/6 bulan/1 bulan lalu di tanggal yang sama) dan
  **Shake-to-Skip** (opsi kocok HP untuk skip lagu, nonaktif secara default) — sekarang masuk
  daftar Fitur v1
- `PROJECT_STATE.md` dicatat: pelajaran supaya fitur baru langsung masuk README di batch yang
  sama, tidak menyusul belakangan
- Nama artifact GitHub Actions (`.github/workflows/build.yml`) diubah dari
  `AudioPlayer-v<versi>-<short-sha>` jadi `AudioPlayer-v<versi>-release` — short commit hash
  dihapus dari nama tag supaya polanya stabil dan tidak berubah-ubah tiap commit. `SHORT_SHA`
  tetap dihitung dan muncul di log run Actions (guna jejak commit saat debug), cuma tidak lagi
  ikut jadi bagian nama file/artifact
- README § "Standar Penomoran Versi" dan § "Build" disinkronkan ke pola nama baru
- Tidak ada perubahan logic build/signing/versionCode — murni penamaan output & dokumentasi

## Batch 16 — Konsistensi observability & feedback kegagalan senyap
- `AppLogger` ditambahkan ke 7 titik yang sebelumnya gagal 100% diam-diam tanpa jejak sama
  sekali di Log Diagnostik: `SearchHistoryStore`, `PlaylistStore` (parse gagal → playlist
  user tampak hilang tanpa penjelasan), `CustomFolderScanner` (dua titik: gagal baca isi
  folder, gagal baca metadata satu file), `WidgetUpdater` (gagal muat artwork widget),
  `AccentColorExtractor` (gagal ekstrak warna aksen), `EqualizerController` (gagal attach
  equalizer — sebelumnya tidak bisa dibedakan dari "device memang tidak dukung equalizer"),
  dan `PlayerViewModel` (gagal scan satu folder tambahan saat refresh library gabungan)
- Bug UX ditemukan sekalian saat audit: `addCustomFolder` gagal ambil izin (`SecurityException`)
  sebelumnya `return` polos tanpa penjelasan apa pun ke user — user pilih folder, tidak
  terjadi apa-apa, tidak ada cara tahu kenapa. Sekarang dicatat ke Log Diagnostik + muncul
  Snackbar "Gagal menambahkan folder — izin ditolak sistem."
- `PlayerViewModel` dapat flow baru `actionErrorMessage` (terpisah dari `playbackErrorMessage`
  yang sudah ada) khusus untuk kegagalan aksi di luar playback, supaya penamaan flow tetap
  jujur soal konteksnya masing-masing — bukan menumpuk semua jenis error ke satu nama yang
  jadi menyesatkan
- Tidak ada perubahan behavior lain: semua fallback (kosongkan list, lewati file, dsb.) tetap
  identik seperti sebelumnya — perubahan murni menambah jejak log + satu pesan Snackbar baru
- `PROJECT_STATE.md` dan `FILE_MANIFEST.txt` ditambahkan di root, sesuai standar proyek yang
  belum sempat diterapkan ke repo ini sebelumnya

## Batch 12 — Playback Resumption resmi
- `PlaybackService` dipindah dari `MediaSessionService` ke `MediaLibraryService` — prasyarat
  resmi Google supaya kartu resume media di System UI bisa muncul
- `onPlaybackResumption` diimplementasi — memakai ulang logic `restoreLastQueue` yang sudah
  ada (dipecah jadi `loadSavedQueueItems` supaya jalur widget cold-start dan jalur resumption
  ini pakai logic identik, tidak dobel-tulis)
- `onConnect` ditulis eksplisit untuk mereplikasi persis perilaku lama (terima semua
  controller, command default penuh) — `MediaLibrarySession.Builder` mewajibkan callback,
  beda dari `MediaSessionService` yang boleh tanpa callback sama sekali
- Manifest: `PlaybackService` di-export, tambah action `MediaBrowserService`, deklarasi
  `MediaButtonReceiver` baru
- Keterbatasan yang tidak bisa dihilangkan dari sisi kode: HP dengan RAM-management agresif
  (sebagian skin Xiaomi/Oppo/Vivo/Samsung) tetap bisa membunuh proses aplikasi kapan saja
  kecuali user manual whitelist di pengaturan baterai

## Batch 11 — Perbaikan label tombol notifikasi
- Tombol notifikasi cold-start ternyata beku permanen di "Lanjutkan" — dibangun sekali di
  awal dan tidak pernah di-refresh. Sekarang notifikasi di-post ulang tiap kali status
  play/pause benar-benar berubah
- Koreksi klaim Batch 10: perbaikan `onTaskRemoved` waktu itu **tidak cukup** untuk menjamin
  kontrol lock screen tetap ada setelah swipe — Media3 punya timeout internal 10 menit saat
  jeda yang di luar kendali `onTaskRemoved`. Solusi sesungguhnya adalah Playback Resumption
  (Batch 12)

## Batch 10 — Notifikasi cold-start macet permanen
- Root cause notifikasi "Memuat lagu..." yang macet berjam-jam: proses pemulihan antrean di
  cold-start tidak punya try/catch — kalau gagal (izin storage dicabut, lagu tersimpan sudah
  dihapus, dll), kode pembersih notifikasi tidak pernah kesampaian
- Ditambah try/catch/finally supaya notifikasi placeholder dijamin hilang apa pun yang terjadi
- Tombol Lanjutkan/Jeda ditambahkan ke notifikasi placeholder itu sendiri (sebelumnya nol
  kontrol sama sekali di notifikasi ini)
- `onTaskRemoved`: sesi tidak lagi otomatis mati hanya karena musik sedang dijeda saat
  di-swipe dari Recents — hanya mati kalau memang tidak ada antrean sama sekali

## Batch 9 — Frontend: drag reorder, Undo, konsistensi haptic
- Drag-to-reorder di Queue (pegangan drag baru, tombol panah atas/bawah tetap ada sebagai
  fallback)
- Snackbar "Urungkan" untuk hapus dari antrean/playlist — nemu bug tersembunyi: tombol aksi
  Snackbar di-hardcode `null` di kode lama, jadi actionLabel apa pun sebelumnya tidak pernah
  muncul
- Haptic feedback diperluas ke semua switch di Settings dan aksi reorder/hapus di Playlist
- Bug reorder-key yang sama seperti Queue (Batch 7) ternyata ada juga di PlaylistScreen —
  dibetulkan sekalian

## Batch 8 — Observability & auto-refresh
- `AppLogger`: log diagnostik lokal (nangkap crash tak tertangani + error yang sebelumnya
  kebungkam diam-diam), disimpan di file privat HP, tidak pernah dikirim ke mana pun —
  sengaja bukan Crashlytics/Sentry karena app ini tidak punya izin INTERNET sama sekali dan
  itu bagian dari klaim privasinya
- `onPlayerError` ditangani — file dihapus/rusak saat diputar sekarang memicu Snackbar +
  auto-skip ke lagu berikutnya, bukan diam macet
- `ContentObserver` MediaStore — library auto-refresh (debounce 1.5 detik) saat file
  ditambah/dihapus dari luar app selagi app terbuka
- Unit test pertama di proyek ini: `PinLockoutPolicyTest`, `LibrarySearchIndexTest`,
  `UtilsTest` — murni JVM, tanpa Robolectric/emulator

## Batch 7 — Keamanan
- PIN: dari SHA-256 tanpa salt jadi PBKDF2 + salt per-instalasi + lockout berjenjang (4x gagal
  gratis, lalu 30d/1m/2m/maks 4m)
- `dataExtractionRules.xml` + `backup_rules.xml` — mengecualikan `app_lock` dari cloud backup
  & device transfer
- R8 minify + resource shrinking diaktifkan di release build (sebelumnya `false`)
- Bug reorder-key Queue: key lama `"songId_index"` (gabungan id lagu + posisi) rusak
  animasinya tiap reorder karena terikat ke posisi, bukan identitas lagu — diganti key stabil
  per-slot yang ikut lagu saat dipindah
- Beberes repo: folder duplikat `AudioPlayer-main/AudioPlayer-main/` dan file nyasar
  `ession.MediaSession` dihapus

## Batch 15 — Persiapan lanjut di sesi lain
- Ditambah komentar level-file di `PlaybackService.kt` yang eksplisit menunjuk ke CHANGELOG
  Batch 10-14 dan mengingatkan supaya asumsi soal API Media3 dicek ulang ke dokumentasi
  resmi/source code sebelum diubah — file ini yang paling sering jadi sumber asumsi salah
  sepanjang riwayat batch di atas
- README ditambah catatan "mulai dari sini" di paling atas untuk sesi chat baru mana pun
- Referensi basi "Media3 `MediaSessionService`" di README diperbaiki jadi `MediaLibraryService`
  (sudah pindah sejak Batch 12, belum sempat disinkronkan)
- Dipertimbangkan tapi tidak dikerjakan: menambah tahap compile-check terpisah di CI sebelum
  build release penuh — dicek dulu ke `.github/workflows/build.yml`, ternyata
  `compileReleaseKotlin` (tempat error Batch 14 ketahuan) sudah gagal duluan sebelum tahap
  minify/sign yang mahal, jadi tidak ada keuntungan waktu yang jelas untuk saat ini

## Batch 14 — Hotfix build error dari Batch 12
- `MediaLibrarySession` ternyata nested di dalam `MediaLibraryService`
  (`MediaLibraryService.MediaLibrarySession`), bukan class top-level di package
  `androidx.media3.session` seperti yang ditulis di Batch 12 — dicek ulang langsung ke source
  code resmi androidx/media (konsisten dari versi 1.0.0 sampai rilis terbaru) untuk
  memastikan sebelum memperbaiki
- Satu baris import yang salah; semua pemakaian lain di file itu otomatis benar begitu
  import-nya benar

## Batch 6 dan sebelumnya
Lihat daftar fitur di `README.md` — detail per-batch untuk rentang ini tidak tercatat
terpisah di file ini.
