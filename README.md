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

## Belum selesai / dalam pengerjaan
- `EqualizerController.kt` sudah ada (baca/tulis band frekuensi, preset, persist ke SharedPreferences) tapi **belum disambungkan** ke ViewModel maupun UI manapun

## Build
Build otomatis lewat GitHub Actions setiap push ke `main`. Hasil APK release diunggah sebagai artifact bernama `AudioPlayer-release`.

## Rencana v2 (belum dibuat)
- Equalizer (selesaikan wiring UI-nya)
- Widget home screen
- Lirik otomatis (cari/unduh dari internet — versi sekarang murni input manual)
