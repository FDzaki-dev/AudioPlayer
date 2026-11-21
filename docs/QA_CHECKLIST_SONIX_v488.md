# Checklist Kematangan Fitur Dasar AudioPlayer — SONIX v488
**Basis penilaian:** inspeksi source ZIP `SONIX_v488.zip` (static/source inspection).  
**Legenda:** `[x]` = terimplementasi di source; `[~]` = sebagian / belum memenuhi seluruh kriteria / runtime belum terverifikasi; `[-]` = tidak relevan atau belum tersedia.

> **Catatan penting:** tanda `[x]` di bawah berarti implementasinya terlihat jelas di source dan/atau memiliki automated test yang relevan. Itu **bukan** klaim bahwa seluruh perilaku sudah lulus device-QA fisik.

---

## [~] 1. Kontrol Pemutaran Utama (Core Playback Engine)
- [~] **Play / Pause / Stop**
  - [x] Play / Pause terimplementasi melalui `MediaController` → `PlaybackService`/Media3.
  - [~] Respons instan dalam hitungan milidetik tanpa lag — arsitektur async sudah benar, tetapi latency nyata tidak dapat dibuktikan hanya dari ZIP.
  - [x] Status ikon UI mengikuti `isPlaying` dari player.
  - [~] **Stop** — tidak ditemukan kontrol Stop musik eksplisit di UI; terdapat pemanggilan `stop()` lain yang bukan kontrol playback utama.
- [x] **Next & Previous**
  - [x] Next/Previous terhubung ke Media3.
  - [~] Transisi bebas glitch belum dapat dibuktikan tanpa playback device nyata.
  - [x] Previous memakai perilaku previous Media3; konfigurasi custom ambang 3 detik tidak ditemukan di source. Implementasi mengandalkan default Media3, sehingga kriteria 3 detik belum diverifikasi eksplisit.
- [x] **Scrubbing / Seeking (Timeline Slider)**
  - [x] `seekTo(positionMs)` tersedia dan progress/duration disinkronkan.
  - [x] Ada instrumentation test `seekTo_movesPlaybackPosition`.
  - [~] Tidak dapat membuktikan nol glitch/buffer/crash pada semua device hanya dari source.
- [x] **Volume & Mute Control**
  - [x] Volume player dapat diatur melalui `MediaController.setVolume()`.
  - [~] Sinkronisasi sempurna dengan volume/mute fisik sistem belum diverifikasi lewat device QA.

---

## [~] 2. Manajemen Audio Back-End & Interupsi Sistem (Kestabilan Sistem)
- [x] **Background Playback (Pemutaran di Latar Belakang)**
  - [x] `PlaybackService` adalah foreground media playback service.
  - [x] MediaSessionService/Media3 dipakai sehingga playback tidak bergantung pada Activity.
  - [~] Ketahanan terhadap aplikasi berat/tekanan RAM jangka panjang belum diverifikasi pada device fisik.
- [x] **Audio Focus Handling (Interupsi Suara OS)**
  - [x] ExoPlayer dibuat dengan `AudioAttributes` media/music dan `handleAudioFocus = true`.
  - [x] Audio focus otomatis dikelola Media3 untuk interupsi aplikasi/telepon yang sesuai.
  - [~] Detail pause/duck/resume pada seluruh kombinasi telepon, WhatsApp call, alarm, Maps, dan notifikasi belum diuji device nyata.
- [x] **Audio Output Switching (Penyandangan Perangkat)**
  - [x] **Becoming Noisy Handler:** `setHandleAudioBecomingNoisy(true)` terpasang pada session player.
  - [~] **Seamless Bluetooth Reconnection:** MediaSession + media-button/resumption support tersedia, tetapi perpindahan output Bluetooth di tengah lagu belum dibuktikan dengan uji hardware.
- [-] **Asynchronous Buffering (Khusus Audio Online/Streaming)**
  - [-] Tidak ada audio streaming/online playback yang menjadi core fitur proyek ini; repository utama memindai audio lokal via MediaStore.
  - [~] Error jaringan untuk streaming audio tidak dapat dinilai sebagai fitur playback lokal.

---

## [~] 3. Manajemen Daftar Putar & Antrean (Playlist & Queue)
- [~] **Shuffle (Acak)**
  - [x] Shuffle Media3 tersedia dan dapat di-toggle.
  - [x] Ada instrumentation test `shuffleToggle_reportsEnabledState`.
  - [~] Algoritma “adil” dan jaminan tidak mengulang lagu yang sama dalam waktu dekat tidak ditemukan sebagai constraint khusus. `shuffled()`/shuffle order tidak membuktikan anti-repeat-nearby.
- [x] **Repeat (Ulang)**
  - [x] Repeat One tersedia.
  - [x] Repeat All tersedia.
  - [x] Ada instrumentation test untuk Repeat One dan Repeat All.
- [x] **Queue Management (Antrean Lagu)**
  - [x] Up Next/Queue sheet tersedia.
  - [x] Lagu dapat ditambah ke queue.
  - [x] Lagu dapat dihapus dari queue.
  - [x] Urutan queue dapat dipindah dengan drag-and-drop melalui dedicated drag handle.
  - [x] Reordering dilakukan saat playback aktif dan state queue dipersist.

---

## [~] 4. Integrasi Sistem & UI/UX Layar Kunci
- [x] **Media Notification & Lock Screen Control**
  - [x] MediaSession/MediaLibrarySession digunakan oleh `PlaybackService`.
  - [x] Metadata track + artwork disediakan melalui MediaSession/bitmap loader.
  - [x] Kontrol media play/pause/next/previous terhubung melalui MediaSession.
  - [~] Respons nyata di notification shade/lock screen belum diverifikasi secara manual pada device.
- [x] **Media Session / Hardware Key Integration**
  - [x] `MediaButtonReceiver` dideklarasikan.
  - [x] MediaSession menangani kontrol eksternal/headset/Bluetooth.
  - [~] Kombinasi tekan 1x/2x spesifik TWS bergantung pada perangkat dan belum diuji pada hardware nyata.
- [~] **Penyimpanan Status Terakhir (Persistence State)**
  - [x] `PlaybackStateStore` menyimpan queue, index, position, repeat, shuffle, dan speed.
  - [x] State dipersist berkala saat playback dan langsung ketika pause.
  - [x] Resume memulihkan queue/index/position serta repeat/shuffle.
  - [~] “Posisi terakhir” saat proses mati paksa tidak dijamin sampai milidetik terakhir karena checkpoint periodik; state terakhir yang sudah tersimpan yang dipulihkan.
  - [x] Jalur resume tidak otomatis memutar tanpa perintah autoplay/resume yang sesuai.

---

## [~] 5. Manajemen Berkas & Penanganan Eror (Data & Storage)
- [~] **Media Scanner (Khusus Audio Lokal)**
  - [x] Scan memakai MediaStore dan `IS_MUSIC != 0`.
  - [x] File berdurasi 0 dibuang.
  - [ ] Filter durasi pendek khusus untuk rekaman WhatsApp/ringtone/game effect **tidak ditemukan**. `DURATION > 0` bukan filter “musik minimum”.
- [x] **Robust Error Handling (File Rusak/Korup)**
  - [x] `onPlayerError()` menangani file hilang, permission, malformed/unsupported, dan error lainnya.
  - [x] Error diberi pesan ke state UI dan playback mencoba skip ke track berikutnya.
  - [x] Ada batas consecutive playback errors agar queue yang seluruhnya rusak tidak masuk loop tanpa akhir.
  - [~] Toast/Snackbar dan perilaku skip pada seluruh variasi file rusak belum diuji exhaustively di device.
- [~] **Dukungan Multi-format Audio**
  - [x] Source repository secara eksplisit mencakup format mainstream seperti MP3, AAC/M4A, FLAC, WAV, OGG/Vorbis, OPUS, dan AMR melalui MediaStore/platform media framework.
  - [~] Kestabilan decoder MP3/AAC/WAV/FLAC/OGG pada seluruh variasi codec/container belum dapat dinyatakan 100% lulus tanpa corpus file nyata/device test.

---

# Verdict SONIX v488

**Status source:** fitur core playback sudah sangat lengkap dan sebagian memiliki instrumentation test nyata, tetapi checklist ini **belum boleh dicentang 100% PASS** karena beberapa item memang belum dibuktikan lewat device QA.

### Gap yang benar-benar terlihat dari source
1. **Stop playback eksplisit** belum terlihat sebagai kontrol utama.
2. **Previous 3 detik** tidak memiliki konfigurasi custom eksplisit; mengandalkan perilaku default Media3.
3. **Shuffle anti-repeat-nearby** tidak dijamin oleh implementasi.
4. **Bluetooth reconnection/output switching** belum terbukti lewat hardware.
5. **Audio focus: pause/duck/resume seluruh skenario** belum device-tested.
6. **Filter audio pendek** belum ada; scanner hanya mensyaratkan `DURATION > 0`.
7. **Streaming audio online** bukan scope playback lokal proyek ini.
8. **Process-death/background/lock-screen/TWS** membutuhkan device QA untuk verdict final.
9. **Multi-format decoder** terdeteksi didukung oleh stack MediaStore/Media3, tetapi robustness lintas file nyata belum terbukti.

### Automated evidence yang tersedia
- Instrumentation test playback: play/pause, seek, next, previous, repeat-one, repeat-all, shuffle.
- Unit test untuk beberapa komponen playback/data.
- `PlaybackService` menggunakan Media3 `AudioAttributes`, automatic audio-focus handling, `setHandleAudioBecomingNoisy(true)`, dan MediaSession.
- `PlaybackStateStore` menyimpan queue/position/repeat/shuffle/speed.

**Kesimpulan:** **SOURCE-IMPLEMENTED: tinggi | DEVICE-QA: belum lengkap | 100% PASS: belum.**
