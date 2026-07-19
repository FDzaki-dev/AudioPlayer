# Audio Player

Audio player Android — Kotlin + Jetpack Compose + Media3 ExoPlayer.

## Fitur v1
- Scan otomatis semua file audio di perangkat via MediaStore (mendukung codec mainstream: MP3, AAC/M4A, FLAC, WAV, OGG/Vorbis, OPUS, AMR — apa pun yang bisa diindeks sistem)
- Tab **Lagu** (semua lagu, urut judul) dan **Folder** (dikelompokkan per folder, folder = playlist)
- Playback background via foreground service (Media3 `MediaSessionService`) + kontrol di notification/lock screen
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
- **Kontrol volume** langsung dari Now Playing
- **Kelola folder**: pilih folder mana saja yang mau disertakan/dikecualikan dari pemindaian musik
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
- **Equalizer**: `EqualizerController.kt` kini tersambung penuh — sheet baru di Now Playing dengan toggle aktif/nonaktif, **preset kuat buatan sendiri** (Flat/Bass+/Treble+/Vokal+, sengaja dibuat dramatis karena preset bawaan Android biasanya sangat halus di banyak perangkat), preset bawaan perangkat, dan slider per band frekuensi. Geser slider atau pilih preset otomatis mengaktifkan efeknya. Tersimpan otomatis dan diterapkan ulang tiap sesi
- **Fade Transisi (crossfade sederhana)**: volume melandai halus ±3 detik di pergantian lagu, bisa diatur nyala/mati lewat dialog "Pengaturan Putar" (ikon kecepatan). Aktif secara default
- **APK release ditandatangani konsisten**: CI kini bisa memakai keystore release asli (lewat secret GitHub) untuk build release, bukan debug key — install APK baru tidak perlu uninstall dulu. Otomatis jatuh ke debug key kalau secret belum diisi, jadi tidak pernah gagal build

## Belum selesai / dalam pengerjaan
- Gapless playback murni (tanpa jeda encoder/decoder) belum ada — yang ada sekarang crossfade berbasis fade volume, bukan penyambungan buffer audio
- Shared-element transition sungguhan (mini player → Now Playing sebagai satu elemen visual) belum ada — versi sekarang pakai animasi scale-in sebagai pendekatan yang lebih aman (lihat catatan di riwayat commit)

## Build
Build otomatis lewat GitHub Actions setiap push ke `main`. Hasil APK release diunggah sebagai artifact bernama `AudioPlayer-release`. Kalau secret `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, dan `SIGNING_KEY_PASSWORD` sudah diisi di pengaturan repo, APK ditandatangani pakai keystore release asli — kalau salah satu kosong, otomatis jatuh ke debug key tanpa bikin build gagal.

## Rencana v2 (belum dibuat)
- Gapless playback murni (penyambungan buffer audio tanpa jeda)
- Shared-element transition mini player ↔ Now Playing (butuh bump versi Compose)
- Widget home screen
- Lirik otomatis (cari/unduh dari internet — versi sekarang murni input manual)
