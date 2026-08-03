# Audio Player

Audio player Android — Kotlin + Jetpack Compose + Media3 ExoPlayer.

> **Mulai dari sini kalau ini sesi/percakapan baru:** baca `PROJECT_STATE.md` (status +
> riwayat insiden ringkas), `CHANGELOG.md` (histori tiap batch), dan bagian "Keputusan
> Arsitektur" di bawah sebelum mengubah apa pun — terutama sebelum menyentuh
> `PlaybackService.kt`, `AppLockStore.kt`, atau `app/build.gradle.kts`. Konteks percakapan
> lama tidak ikut ke sesi baru; dua file itu sudah pernah menyebabkan asumsi salah yang baru
> ketahuan setelah build gagal (lihat Batch 10-14 di CHANGELOG).

## Fitur v1
- Scan otomatis semua file audio di perangkat via MediaStore (mendukung codec mainstream: MP3, AAC/M4A, FLAC, WAV, OGG/Vorbis, OPUS, AMR — apa pun yang bisa diindeks sistem)
- Tab **Lagu** (semua lagu, urut judul) dan **Folder** (dikelompokkan per folder, folder = playlist)
- Playback background via foreground service (Media3 `MediaLibraryService`, mendukung Playback Resumption resmi) + kontrol di notification/lock screen
- Now Playing: album art, seek bar, shuffle, repeat (off/all/one), play/pause/next/prev
- Mini player persisten di bagian bawah layar
- Dark mode default
- Pencarian (search) judul/artis di Perpustakaan
- Sleep timer (10/15/30/45/60 menit)
- Kecepatan putar (0.5x–2x)
- Resume otomatis dari posisi terakhir setelah app ditutup
- Favorit lagu
- **Antrean putar (Queue)**: lihat antrean, naik/turunkan urutan, hapus dari antrean, lompat langsung ke lagu manapun, plus aksi "Putar Berikutnya" / "Tambah ke Antrean" via tekan-lama pada lagu di Perpustakaan
- **Beranda pintar**: bagian "Baru Diputar" dan "Paling Sering Diputar" otomatis terisi dari riwayat dengar, tanpa perlu aksi manual dari user
- **Playlist manual**: buat playlist sendiri lintas folder/album/artis, isi lewat "Tambah ke Playlist" (tekan-lama lagu), atur urutan, hapus lagu, ganti nama, atau hapus playlist — tersedia di tab Playlist pada Perpustakaan
- **Lirik**: tambahkan lirik sendiri per lagu (tempel teks biasa, atau format LRC `[mm:ss.xx]` untuk lirik yang otomatis mengikuti posisi putar dan auto-scroll)
- **Tema dinamis**: warna aksen di Now Playing & mini player otomatis diambil dari sampul album lagu yang sedang diputar
- **Kontrol volume** langsung dari Now Playing (slider attenuasi internal app, terpisah dari volume sistem)
- **Kelola folder**: pilih folder mana saja yang mau disertakan/dikecualikan dari pemindaian musik otomatis, **plus tambahkan folder tambahan lewat izin sistem** (Storage Access Framework) untuk memindai audio yang belum terdeteksi MediaStore — bekerja penuh offline/lokal, tidak ada koneksi internet yang terlibat
- **Widget lebih tahan banting**: diperbaiki supaya tetap merespons play/pause/next meski aplikasi sudah disingkirkan total dari recent apps — service langsung "lapor" ke sistem sebagai proses aktif begitu widget ditekan, sebelum sempat dibunuh oleh pengelola baterai agresif (relevan khususnya di skin seperti XOS/MIUI). Widget juga ikut update judul/artis/status main tiap kali lagu berganti
- **Pencocok Signature APK**: alat diagnostik di Perpustakaan (ikon sidik jari) — pilih dua file APK (versi lama & baru), langsung ketahuan apakah keduanya ditandatangani dengan key yang sama tanpa perlu install dulu. Kalau beda, laporan lengkap (nama package, versi, SHA-256 tiap file) bisa disalin ke papan klip lewat dialog khusus (bisa di-scroll penuh) — bukan cuma tombol OK polos kayak dialog instalasi Android
- **3 tema penuh** (bukan cuma ganti warna): tiap tema punya palet warna, jenis huruf, dan bahasa bentuk sudut sendiri-sendiri.
  - **Ink & Brass** (gelap) — boutique hi-fi, emas hangat di atas hitam pekat, judul bertipe serif tegas
  - **Midnight Bloom** (gelap) — jewel-tone, rose-orchid lembut di atas aubergine pekat, tipografi bold dengan tracking rapat, sudut sedang
  - **Paper & Ink** (terang) — krem hangat + sienna terbakar, serif tipis gaya editorial cetak, sudut nyaris kotak

  Dipilih lewat tab **Pengaturan** baru di navigasi bawah, tersimpan otomatis, diterapkan ulang tiap sesi
- **Halaman Pengaturan**: tab baru di navigasi bawah — berisi pemilih tema dan info versi aplikasi (nomor versi + build)
- **Filter Perpustakaan tersimpan**: tab yang terakhir dipilih (Lagu/Album/Artis/Folder/dst.) diingat di antar sesi
- **Onboarding**: layar selamat datang menjelaskan kenapa izin dibutuhkan sebelum dialog izin muncul, dengan fallback "Buka Pengaturan Aplikasi" kalau izin ditolak permanen
- **Optimalisasi**: pemindaian MediaStore kini terpusat satu kali di ViewModel (bukan diulang di setiap Beranda/Perpustakaan/Playlist), mengurangi kerja I/O berulang dan flicker loading tiap pindah tab
- **Navigasi gesture back**: gesture back sistem sekarang navigasi bertahap sesuai stack (Now Playing → Perpustakaan/Beranda), bukan langsung keluar aplikasi
- **Now Playing lebih atmosferik**: backdrop blur dari album art (seperti Spotify/Apple Music) di belakang piringan hitam yang berputar, dipadu warna aksen dinamis per lagu
- **Micro-interaction**: tombol play/pause, shuffle, repeat, favorit, dan navigasi lagu kini punya animasi "bounce" halus tiap ditekan — bukan cuma ganti ikon instan
- **Status bar & navigation bar** dipaksa kontras terang, konsisten dengan tema gelap aplikasi, tidak ikut mode terang/gelap sistem
- **Shimmer skeleton loading** kini konsisten di Beranda (bentuknya persis kartu asli), bukan cuma spinner generik
- **Haptic diperluas**: reorder antrean, hapus dari antrean, dan berhasil tambah ke playlist masing-masing punya pola getar berbeda
- **Search dikelompokkan**: hasil pencarian dipisah per Artis / Album / Lagu (bukan satu list rata), plus riwayat pencarian terbaru saat kolom pencarian masih kosong
- **Transisi Now Playing**: piringan hitam "tumbuh" masuk dengan animasi scale-spring saat layar dibuka, alih-alih muncul instan penuh
- **Audio focus & "becoming noisy"**: auto-pause saat headset/Bluetooth dicabut, auto-duck saat ada notifikasi/telepon masuk
- **Radio otomatis & Mix Artis**: saat antrean habis (repeat off), pemutaran otomatis lanjut dengan lagu lain dari library; Beranda punya bagian "Mix: [Artis]" berdasarkan artis yang paling sering didengar
- **App icon adaptif**: ikon aplikasi diganti dari placeholder default Android Studio menjadi identitas "Ink & Brass" sendiri (piringan hitam brass di atas ink hitam), pakai format Adaptive Icon (`mipmap-anydpi-v26`) untuk Android 8+, dengan fallback PNG untuk versi lebih lama
- **Equalizer**: `EqualizerController.kt` tersambung penuh — sheet di Now Playing dengan toggle aktif/nonaktif, **preset kuat buatan sendiri** (Flat/Bass+/Treble+/Vokal+, sengaja dibuat dramatis karena preset bawaan Android biasanya sangat halus di banyak perangkat), preset bawaan perangkat, dan slider per band frekuensi. Geser slider atau pilih preset otomatis mengaktifkan efeknya. Tersimpan otomatis dan diterapkan ulang tiap sesi
- **Gapless (Murni) vs Fade Halus**: dialog "Pengaturan Putar" sekarang menampilkan dua pilihan eksplisit, bukan cuma satu toggle fade — "Gapless (Murni)" membiarkan transisi alami tanpa campur tangan (default), "Fade Halus" menerapkan fade volume ±3 detik di tiap pergantian lagu
- **APK release ditandatangani konsisten**: CI memakai keystore release asli (lewat secret GitHub) untuk build release, bukan debug key — install APK baru tidak perlu uninstall dulu. Otomatis jatuh ke debug key kalau secret belum diisi, jadi tidak pernah gagal build
- **Gesture swipe kecerahan & volume**: di layar Now Playing, geser vertikal di **separuh kiri** untuk atur kecerahan layar, **separuh kanan** untuk atur volume — masing-masing zona selebar 50% layar penuh (bukan cuma strip tipis di ujung), lengkap indikator persentase mengambang selagi digeser. Volume yang diatur adalah **volume sistem Android sungguhan** (sama seperti tombol fisik HP), bukan sekadar penguat internal app. Area geser horizontal di piringan hitam (lagu berikutnya/sebelumnya) tetap berfungsi normal di atasnya. Kecerahan diatur lewat override per-window (tidak butuh izin tambahan) dan otomatis kembali ke pengaturan sistem begitu layar Now Playing ditutup
- **Keamanan kunci PIN**: hash PIN pakai PBKDF2 + salt unik per-instalasi (bukan SHA-256 polos), plus lockout berjenjang setelah 4x percobaan gagal (30 detik → 1 menit → 2 menit → maksimal 4 menit) — layar kunci menampilkan hitung mundur dan mengunci keypad selama masa lockout
- **Backup dikecualikan untuk data sensitif**: `app_lock` (hash PIN + status lockout) dikecualikan dari cloud backup & device transfer Android; data lain tetap ikut backup seperti biasa
- **Release build di-shrink & di-obfuscate** (R8 minify + resource shrinking) — APK lebih kecil dan tidak gampang dibongkar kalau bocor
- **Antrean (Queue) bisa di-drag**: pegangan drag baru di tiap baris antrean untuk mengurutkan ulang lewat tahan-geser, tombol panah atas/bawah tetap ada sebagai cara presisi/fallback
- **Undo untuk hapus**: hapus lagu dari antrean atau playlist memunculkan Snackbar "Urungkan" yang mengembalikan lagu persis ke posisi semula
- **Auto-refresh library**: perubahan file musik dari luar app (file manager, sinkronisasi) terdeteksi otomatis lewat pengamat MediaStore selagi app terbuka, tanpa perlu pencet "Pindai Ulang" manual
- **Log diagnostik lokal**: Settings → Lanjutan → Log Diagnostik — catatan error & crash tersimpan di file privat HP, tidak pernah dikirim ke mana pun (app ini tidak punya izin INTERNET sama sekali). Khusus untuk crash fatal (app tidak bisa dibuka sama sekali), tersimpan juga salinannya di `Documents/AudioPlayer/logs/crash_<waktu>.txt` — folder publik yang bisa dibuka pakai File Manager biasa tanpa root/ADB (Android 10 ke atas)
- **Playback Resumption**: musik bisa dilanjutkan dari lock screen / kontrol Bluetooth walau proses aplikasi sudah benar-benar mati (lewat mekanisme resmi Android `MediaLibraryService` + `onPlaybackResumption`), dan sesi tidak lagi mati otomatis hanya karena di-swipe dari Recents saat musik sedang dijeda
- **Penanganan error playback**: file yang dihapus/rusak saat sedang diputar memicu pesan + otomatis lompat ke lagu berikutnya, bukan diam macet tanpa penjelasan
- **Kilas Balik**: bagian "Kilas Balik: [label]" di Beranda, otomatis menampilkan lagu yang didengar persis di tanggal yang sama 1 tahun lalu, atau 6 bulan lalu, atau 1 bulan lalu (dicek berurutan dari yang terjauh, dipakai match pertama yang ada isinya) — berdasarkan riwayat dengar harian asli, bukan tebakan. Baru muncul setelah ada histori dengar minimal sebulan
- **Shake-to-Skip**: opsi di Pengaturan (nonaktif secara default — gesture fisik harus sengaja diaktifkan user) untuk lompat ke lagu berikutnya dengan mengocok HP selagi musik diputar. Pakai accelerometer dengan debounce (satu kocokan = satu skip, bukan rentetan), dan sensor cuma aktif selagi musik benar-benar berjalan — hemat baterai, tidak kepicu asal di kantong saat idle

## Standar Penomoran Versi
`versionCode` (nomor internal, tidak terlihat user) naik otomatis mengikuti jumlah commit git — jadi tidak akan pernah lupa di-bump dan APK baru selalu dikenali "lebih baru" oleh Android. `versionName` (nomor yang terlihat user, misal `3.8`) tetap dikontrol manual, dibump sesekali di titik-titik rilis yang dianggap layak, bukan tiap batch.

Nama file ZIP hasil tiap batch pengembangan (`AudioPlayer-batchN-release.zip`) melacak nomor batch percakapan, **bukan** `versionName` — keduanya sengaja dipisah: `versionName` untuk rilis yang user-facing, nomor batch untuk melacak paket kerja per sesi supaya ZIP lama dan baru gampang dibedakan.

File APK hasil build dan artifact GitHub Actions membawa nomor versi di namanya, diakhiri `-release` (`AudioPlayer-v1.0.247-release.apk`) — bukan nama generik statis, dan sengaja **tanpa** short commit hash di belakang supaya nama artifact tetap stabil/gampang dikenali. Penamaan ini dikerjakan di level workflow CI (`.github/workflows/build.yml`), bukan dobel dengan Gradle, biar tidak saling tabrak.

## Keputusan Arsitektur
Ringkasan kenapa, bukan cuma apa — supaya sesi kerja berikutnya (chat AI baru sekalipun) tidak perlu menebak ulang alasan di balik hal-hal yang tidak jelas kalau cuma baca kode.

- **`MediaLibraryService`, bukan `MediaSessionService`**: dibutuhkan spesifik untuk Playback Resumption resmi Android (kartu resume di System UI, kontrol lewat Bluetooth walau proses aplikasi sudah mati). `MediaSessionService` saja tidak cukup untuk fitur ini per dokumentasi resmi Media3.
- **Notifikasi cold-start terpisah** (`startForegroundColdStartNotification`): saat widget home screen ditekan sementara aplikasi benar-benar mati, harus ada notifikasi foreground service **instan** sebelum proses pemulihan antrean (query MediaStore, dst.) selesai — beberapa skin Android (XOS/MIUI dkk) membunuh proses baru dalam hitungan saat kalau belum ditandai foreground. Notifikasi ini sengaja sementara, digantikan notifikasi asli Media3 begitu lagu benar-benar mulai.
- **`onTaskRemoved` hanya mematikan sesi kalau antrean kosong**: sebelumnya juga mati kalau musik sedang dijeda, yang berarti kontrol lock screen ikut hilang setiap kali app di-swipe saat tidak sedang main lagu. Catatan jujur: ini perbaikan yang tidak cukup sendirian — lihat batasan Playback Resumption di bawah.
- **Log diagnostik lokal (`AppLogger`), bukan Crashlytics/Sentry**: app ini tidak punya izin INTERNET sama sekali dan itu bagian dari klaim privasinya (diverifikasi eksplisit: semua pemrosesan lokal di HP). Crash reporting pihak ketiga akan butuh izin itu.
- **`PinLockoutPolicy` dipisah dari `AppLockStore`**: murni supaya rumus lockout-nya bisa di-unit-test tanpa perlu Context/SharedPreferences.
- **`favoriteIds`/`selectedIds` pakai `ImmutableSet<Long>` (kotlinx.collections.immutable), bukan `Set<Long>` biasa**: `Set`/`List` bawaan Kotlin selalu dianggap *unstable* oleh Compose compiler (tidak ada jaminan compile-time bebas mutasi in-place), jadi composable manapun yang menerimanya sebagai parameter tidak bisa di-skip recomposition-nya sekalipun isinya sama persis — di `LibraryScreen`, ini bikin seluruh baris lagu yang sedang tampil ikut recompose setiap kali state lain yang tidak terkait berubah (mis. posisi playback yang tick tiap detik). Konsekuensinya: kalau ada `Set<Long>`/`List<T>` baru yang dikirim sebagai parameter ke composable list yang sering di-recompose, pertimbangkan pola yang sama, bukan `Set`/`List` polos.
- **Compose stability config (`app/compose_stability_config.conf`) menandai `android.net.Uri` sebagai stable**: `Song.uri` bertipe `Uri` (tipe platform yang tidak bisa diverifikasi compiler), yang bikin seluruh `Song` dianggap unstable walau field lainnya (Long/String) sudah stabil. `Uri` sendiri efektif immutable (tidak ada setter publik), jadi aman ditandai stable secara manual.

**Batasan yang tidak bisa dihilangkan dari sisi kode:** HP dengan pengelolaan RAM agresif (sebagian skin Xiaomi/Oppo/Vivo, dan sebagian Samsung) tetap bisa membunuh proses aplikasi kapan saja kecuali user manual whitelist app itu di pengaturan baterai — ini keterbatasan Android, bukan sesuatu yang bisa diperbaiki murni lewat kode di app manapun.

Riwayat lebih detail per batch pengembangan ada di `CHANGELOG.md`.

## Belum selesai / dalam pengerjaan
- Shared-element transition sungguhan (mini player → Now Playing sebagai satu elemen visual) belum ada — versi sekarang pakai animasi scale-in sebagai pendekatan yang lebih aman (lihat catatan di riwayat commit)
- Pull-to-refresh gesture di Library belum ada (cuma tombol manual + auto-refresh saat resume/ContentObserver) — proyek ini pakai Compose BOM 2024.05.00, dan API pull-to-refresh Material3 yang simpel baru stabil di versi BOM lebih baru; naikkan BOM berisiko ke komponen lain yang sudah jalan
- Ekstraksi penuh string hardcode ke `strings.xml` (untuk i18n) belum dikerjakan — ada ratusan string tersebar di ~12 file, refactor mekanis sebesar itu belum aman dikerjakan tanpa akses compiler untuk verifikasi
- Belum pernah diuji di perangkat fisik sungguhan oleh siapa pun selain lewat deskripsi/screenshot — termasuk hasil build release dengan minify yang baru aktif sejak Batch 7

## Catatan jujur soal Gapless Playback
Mesin pemutarannya **sudah gapless secara arsitektur** sejak awal — satu ExoPlayer yang hidup terus sepanjang sesi, memutar satu playlist asli (`setMediaItems`/`addMediaItem`), bukan mengganti lagu satu per satu dengan restart/re-prepare. Ini menghilangkan penyebab paling umum dari "jeda" antar lagu (loading ulang, klik/pop transisi). Toggle "Gapless (Murni)" vs "Fade Halus" di dialog Pengaturan Putar cuma menentukan apakah transisi itu **dibiarkan alami** (gapless) atau **sengaja diberi efek fade turun-naik volume**.

Untuk file **FLAC/WAV** (lossless), ini menjamin sambungan sempurna tanpa jeda — dijamin oleh cara format itu bekerja, bukan sekadar klaim.

Untuk **MP3/AAC** (lossy), gapless yang benar-benar sample-accurate juga bergantung pada metadata encoder di file itu sendiri (LAME tag / iTunSMPB) dan seberapa tepat decoder ExoPlayer memangkas padding-nya — ini terjadi di level library, bukan sesuatu yang bisa "ditambahkan" lewat kode aplikasi ini, dan saya belum bisa memverifikasinya dengan telinga di device fisik. Kalau setelah dicoba masih kerasa ada jeda halus khusus di file MP3/AAC tertentu, itu petunjuk berharga — kabari, biar bisa ditelusuri lebih spesifik ke file/formatnya.

## Build
Build otomatis lewat GitHub Actions setiap push ke `main`. Hasil APK release diunggah sebagai artifact bernama `AudioPlayer-v<versi>-release` (nama filenya sendiri juga membawa nomor versi, tanpa commit hash di belakang). Kalau secret `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, dan `SIGNING_KEY_PASSWORD` sudah diisi di pengaturan repo, APK ditandatangani pakai keystore release asli — kalau salah satu kosong, otomatis jatuh ke debug key tanpa bikin build gagal.

## Rencana v2 (belum dibuat)
- Shared-element transition mini player ↔ Now Playing (butuh bump versi Compose)
- Lirik otomatis (cari/unduh dari internet — versi sekarang murni input manual)
