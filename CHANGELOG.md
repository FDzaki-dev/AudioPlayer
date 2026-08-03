# Changelog

Satu entri per batch pengembangan. Ditulis supaya sesi chat AI yang baru (atau siapa pun
yang baru gabung ke proyek ini) bisa langsung baca file ini dan tahu histori keputusan,
tanpa harus gali `git log` atau riwayat chat lama yang sudah tidak bisa diakses lagi.

Catatan: pencatatan per-batch di file ini baru dimulai dari Batch 5. Semua hasil kerja
sebelum itu (fondasi awal: scan MediaStore, playback dasar via Media3, UI Library/Home/Now
Playing/Settings, dst.) sudah terangkum sebagai satu kesatuan di daftar fitur pada
`README.md` — tidak dipecah ulang per batch di sini karena detail per-batch-nya sudah tidak
tersedia.

## Batch 21 — Hotfix: build CI gagal setelah Batch 20 (2 root cause terpisah)
- **Root cause #1**: `app/compose_stability_config.conf` (ditambahkan di Batch 20) pakai gaya
  komentar `#`, tapi parser `stabilityConfigurationPath` milik Compose compiler plugin hanya
  mengenali `//` sebagai penanda komentar. Baris `#` diperlakukan sebagai pattern class
  literal, dan gagal validasi karena mengandung spasi/tanda baca — error persisnya:
  `Error parsing stability configuration file on line 0` diikuti `... is not a valid pattern`,
  membuat task `:app:compileReleaseKotlin` gagal dan seluruh build CI berhenti
- **Fix #1**: semua baris komentar di `compose_stability_config.conf` diganti dari `#` ke `//`,
  dicocokkan dengan format resmi yang dipakai Google sendiri di file sejenis pada
  `android/nowinandroid`. Baris pattern-nya sendiri (`android.net.Uri`) tidak berubah — sudah
  benar sejak awal
- **Root cause #2** (muncul di run CI berikutnya, setelah fix #1 lolos): `LibraryScreen.kt:115`,
  `selectedIds - id` / `selectedIds + id` — operator `+`/`-` versi `kotlinx.collections.immutable`
  yang bertipe benar (`PersistentSet<E>`) hanya aktif kalau diimport eksplisit; tanpa itu Kotlin
  fallback ke operator `+`/`-` bawaan stdlib yang selalu mengembalikan `Set<T>` polos —
  `Type mismatch: inferred type is Set<Long> but PersistentSet<Long> was expected`
- **Fix #2**: `selectedIds - id` / `selectedIds + id` diganti `selectedIds.remove(id)` /
  `selectedIds.add(id)` — method bawaan `PersistentSet` yang sudah dideklarasikan return
  `PersistentSet<E>` langsung, tidak tergantung import operator tambahan. Sudah diaudit,
  tidak ada titik lain di project yang pakai pola operator serupa pada Persistent/ImmutableSet
- Diagnosis kedua-duanya berdasarkan log build GitHub Actions asli, bukan tebakan
- 2 file berubah total (`app/compose_stability_config.conf`, `LibraryScreen.kt`), tidak ada
  file baru/dihapus. Tidak ada perubahan behavior — murni perbaikan syntax/tipe kompilasi

## Batch 20 — Audit null-safety (state collection) & performa (recomposition list panjang)
- **Null-safety di layer UI (Compose state collection) — diaudit, hasilnya bersih**: semua
  `StateFlow` nullable (`celebrationMessage`, `playbackErrorMessage`, `actionErrorMessage`,
  `undoableAction`) sudah dikonsumsi lewat pola aman (`?: return@LaunchedEffect`), dan
  `accentColor` selalu di-fallback (`accentColor ?: fallback`). Satu-satunya `!!` yang
  ditemukan (`SettingsScreen.kt`, dialog atur PIN) sudah dijaga `if (error != null)` sebelumnya
  jadi bukan bug — tetap dirapikan jadi `error?.let { ... }` untuk konsistensi gaya
- **`favoriteIds`/`selectedIds` di `LibraryScreen` → `ImmutableSet<Long>` (perbaikan performa
  utama)**: sebelumnya `Set<Long>` biasa, yang selalu dianggap *unstable* oleh Compose
  compiler. Ini bikin `SongListView`/`GroupedListView`/setiap `SongRow` tidak bisa di-skip
  recomposition-nya sekalipun isinya sama persis — jadi setiap kali komposabel induk
  recompose karena alasan lain (mis. posisi playback yang tick tiap detik saat lagu main),
  seluruh baris lagu yang sedang tampil di layar ikut recompose walau tidak ada satu pun
  favorit/seleksi yang berubah. Ditambah dependency `kotlinx-collections-immutable:0.3.7`;
  `PlayerViewModel.favoriteIds` dan `selectedIds` lokal di `LibraryScreen` sekarang
  `ImmutableSet<Long>`/`PersistentSet<Long>`
- **`Song.uri: Uri` ditandai stable lewat Compose stability config (perbaikan performa
  kedua)**: `android.net.Uri` adalah tipe platform yang tidak bisa diverifikasi compiler,
  jadi seluruh `Song` (walau field lain Long/String sudah stabil) ikut dianggap unstable —
  memperparah temuan di atas karena `SongRow` menerima `Song` sebagai parameter. File baru
  `app/compose_stability_config.conf` menandai `android.net.Uri` sebagai stable (aman karena
  `Uri` efektif immutable, tidak ada setter publik), diwire lewat `stabilityConfigurationPath`
  di `freeCompilerArgs`
- **`collectAsState()` → `collectAsStateWithLifecycle()` di seluruh `MainActivity`**: versi
  lama tetap collect StateFlow walau app di background, buang-buang kerja. Dependency baru:
  `androidx.lifecycle:lifecycle-runtime-compose:2.8.1`
- **2 pemanggilan `sortedBy`/`sorted` yang belum di-`remember`** (daftar kunci album di
  `AlbumGridView`, daftar kunci grup di `GroupedListView`) dirapikan jadi
  `remember(grouped) { ... }`, konsisten dengan pola `remember` yang sudah dipakai di bagian
  lain file yang sama
- 6 file kode produksi berubah (`PlayerViewModel.kt`, `LibraryScreen.kt`, `HomeScreen.kt`,
  `MainActivity.kt`, `SettingsScreen.kt`, `build.gradle.kts`) + 1 file baru
  (`compose_stability_config.conf`). Tidak ada perubahan behavior yang terlihat user — murni
  perbaikan internal Compose recomposition & 1 rapi-rapi gaya kode

## Batch 19 — Audit lifecycle: EqualizerController, AccentColorExtractor, ShakeDetector
- **`AccentColorExtractor` — race condition nyata, diperbaiki**: `updateAccentColor()`
  melempar coroutine baru setiap `onMediaItemTransition` tanpa membatalkan yang sebelumnya.
  Skip cepat beruntun (next ditekan cepat, shake-to-skip berkali-kali) bisa memicu beberapa
  ekstraksi tumpang tindih — kalau yang untuk lagu lama selesai belakangan, warna aksen lagu
  yang sudah dilewati bisa menimpa warna lagu yang sedang main. Diperbaiki dengan
  `accentColorJob?.cancel()` sebelum melempar job baru — pola yang sama persis dengan
  `fadeJob`/`sleepTimerJob`/`libraryRefreshJob` yang sudah ada di file yang sama. Job baru ini
  juga dibatalkan di `onCleared()` untuk konsisten dengan job-job lain
- **`EqualizerController` — diaudit, tidak ada leak**: `attach()` sudah memanggil `release()`
  duluan sebelum bikin instance baru (self-cleaning), dan `PlayerViewModel.onCleared()` sudah
  memanggil `equalizerController.release()`. `ensureEqualizerAttached()` juga cuma dipanggil
  sekali per buka sheet Equalizer (klik eksplisit user), bukan tiap recomposition. Tidak ada
  perubahan kode dari audit ini — dicatat di sini supaya tidak diaudit ulang sia-sia
- **`ShakeDetector` start/stop di transisi audio focus — diaudit, tidak ada bug**: start/stop
  terikat satu sumber kebenaran (`Player.Listener.onIsPlayingChanged`), dan `ExoPlayer` sudah
  dikonfigurasi `setAudioAttributes(..., handleAudioFocus = true)` — jadi kehilangan fokus
  audio (telepon masuk, app lain butuh output), auto-pause "becoming noisy" (headset
  dicabut), dan jeda manual user semuanya lewat jalur yang sama dan otomatis memicu
  `shakeDetector?.stop()`. Duck sementara (notifikasi singkat) sengaja tidak menghentikan
  sensor karena musik secara konsep masih "main", cuma volumenya turun — perilaku itu benar,
  bukan celah. Tidak ada perubahan kode dari audit ini
- 1 file kode produksi berubah (`PlayerViewModel.kt`), murni perbaikan race condition — tidak
  ada perubahan behavior lain

## Batch 18 — Unit test untuk LyricsParser (logic murni yang belum pernah ditest)
- Audit accessibility (Icon `contentDescription = null`) dicoba dulu sebagai kandidat batch
  ini — hasilnya **negatif**: dari 34 kemunculan, yang benar-benar interaktif (di dalam
  `IconButton`/`.clickable`) cuma 5, dan kelimanya sudah benar apa adanya (Icon/gambar
  berdampingan dengan `Text` label dalam satu Row yang sama — Compose otomatis menggabung
  semantics-nya untuk TalkBack, jadi ngasih `contentDescription` di situ justru bikin dibaca
  dobel). Kontrol utama (play/pause di Mini Player & Now Playing) sudah punya
  `contentDescription` dinamis ("Putar"/"Jeda") sejak awal. **Tidak ada perubahan kode dari
  temuan ini** — dicatat di sini supaya sesi berikutnya tidak mengulang audit yang sama.
- Kandidat pengganti yang dieksekusi: `LyricsParser` (parsing LRC, `com.rudi.audioplayer.data`)
  murni logic tanpa dependensi Android — persis pola yang sudah dipakai project ini untuk
  `PinLockoutPolicyTest` dan `LibrarySearchIndexTest` — tapi belum pernah dapat test sendiri.
  Ditambahkan `LyricsParserTest.kt`, 16 test mencakup: parsing lebar digit milidetik (1/2/3
  digit), baris tak-tersinkron vs LRC, baris rusak/kurung tak tertutup (jatuh balik ke teks
  polos), `isSynced()`, dan `currentLineIndex()` termasuk perilaku melompati baris tanpa
  timestamp yang diselipkan di tengah lirik LRC
- Murni penambahan test — **nol perubahan kode produksi**, jadi risiko regresi nol
- `FILE_MANIFEST.txt` diperbarui (102 file, tambah `LyricsParserTest.kt`)

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
