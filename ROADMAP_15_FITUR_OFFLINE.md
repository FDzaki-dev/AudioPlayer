# Roadmap: 15 Fitur Generik 100% Offline (Belum Tersedia)

Dibuat Batch 77. Daftar ini murni dokumen perencanaan — belum ada implementasi kode apa pun
di batch ini. Semua fitur dipilih dengan syarat: (1) generik/berguna untuk audio player pada
umumnya (bukan niche), (2) bisa berjalan 100% lokal di perangkat tanpa izin INTERNET (konsisten
dengan klaim privasi app ini, lihat README § Keputusan Arsitektur), (3) belum ada di codebase
saat ini (dicek terhadap `FILE_MANIFEST.txt` + daftar Fitur v1 di `README.md`).

Setiap entri: deskripsi singkat, kenapa berguna, perkiraan kompleksity, dan dependency/risiko
teknis utama — supaya batch mana pun yang mau mengeksekusi salah satu dari daftar ini bisa
langsung mulai tanpa riset ulang dari nol.

---

## 1. Editor Tag Metadata (ID3/Vorbis Comment)
Edit judul, artis, album, genre, tahun, nomor track, dan artwork tertanam langsung dari app,
ditulis balik ke file audio asli.
- **Kenapa**: banyak file hasil rip/unduhan lama punya tag berantakan/kosong — saat ini app
  cuma bisa membaca tag, tidak bisa memperbaikinya.
- **Kompleksitas**: Sedang-Tinggi. Perlu library penulis tag (mis. `mp3agic`/JAudioTagger, atau
  implementasi manual ID3v2 writer) — tidak ada di dependency saat ini, perlu ditambah.
- **Risiko**: menulis file audio user berisiko korupsi kalau ada bug; wajib ada backup/dry-run
  sebelum overwrite, dan uji ekstensif per format (MP3/FLAC/M4A tag scheme beda-beda).

## 2. Smart Playlist Otomatis ✅ SELESAI (Batch 89)
Playlist yang isinya otomatis mengikuti kriteria (~~genre~~, tahun rilis, rentang durasi, rating
bintang, folder) — bukan playlist manual isi-tangan seperti fitur Playlist yang sudah ada.
- **Kenapa**: playlist manual saat ini butuh isi-tangan tiap lagu; smart playlist auto-update
  begitu ada lagu baru yang cocok kriteria.
- **Kompleksitas**: Sedang. Query filter di atas `MusicRepository`/`RatingStore` yang sudah ada,
  UI builder kriteria (chip filter) baru.
- **Risiko**: rendah — murni query lokal, tidak menyentuh file.
- **Catatan implementasi**: genre TIDAK dikerjakan — MediaStore taruh genre di tabel `Genres`
  terpisah (query per-lagu / N+1), beda dari YEAR yang satu kolom `MediaStore.Audio.Media.YEAR`
  langsung. Detail lengkap: `CHANGELOG.md` Batch 89.

## 3. Editor Lirik LRC Tap-to-Sync
Buat file LRC baru dari nol dengan menekan tombol "tandai baris ini" sambil lagu diputar —
pelengkap fitur Lirik yang sudah ada (saat ini cuma bisa tempel LRC yang sudah jadi).
- **Kenapa**: banyak lagu lokal/indie tidak punya LRC siap pakai di internet; user yang mau
  bikin sendiri sekarang harus pakai app/situs lain.
- **Kompleksitas**: Sedang. UI mirip teleprompter + tombol tap, simpan ke `LyricsStore` yang
  sudah ada (format penyimpanan tidak berubah, cuma cara membuatnya).
- **Risiko**: rendah.

## 4. A-B Repeat & Bookmark Posisi ✅ SELESAI (Batch 91)
Ulang segmen tertentu dalam lagu (titik A ke titik B berulang), dan tandai beberapa posisi
favorit dalam satu file (intro/reff/part solo) untuk lompat cepat.
- **Kenapa**: berguna untuk latihan musik/bahasa, atau podcast/audiobook panjang.
- **Kompleksitas**: Sedang. State baru di `PlayerViewModel` (pointA/pointB), listener posisi
  yang sudah ada (`position` tick) tinggal dipakai ulang untuk cek boundary.
- **Risiko**: rendah — murni logic playback, tidak menyentuh file.
- **Catatan implementasi**: boundary check diekstrak ke `AbRepeatLogic.kt` (pure object, pola
  sama seperti `SmartPlaylistEngine`/`ListeningStatsEngine`) supaya testable tanpa Robolectric.
  Bookmark disimpan per-lagu via `BookmarkStore.kt` (JSON, pola sama `SmartPlaylistStore`).
  Detail lengkap: `CHANGELOG.md` Batch 91.

## 5. Pemangkas & Pembuat Nada Dering (Ringtone Cutter)
Potong bagian lagu (drag range di waveform/seekbar) lalu set langsung sebagai nada dering,
notifikasi, atau alarm sistem Android.
- **Kenapa**: fitur klasik yang sering dicari user audio player, dan Android sudah punya API
  resmi (`RingtoneManager`) untuk ini — tidak butuh root.
- **Kompleksitas**: Sedang. Perlu encode potongan ke file baru (lewat `MediaMetadataRetriever`/
  `MediaExtractor`+`MediaMuxer` bawaan Android, tanpa library tambahan) + `RingtoneManager` API
  untuk set sebagai default.
- **Risiko**: izin `WRITE_SETTINGS` mungkin diperlukan untuk set-as-default langsung (API level
  tertentu) — perlu dicek dan disediakan fallback "simpan lalu pilih manual" kalau ditolak.

## 6. Pencari & Pembersih File Duplikat
Deteksi file audio duplikat (berdasarkan durasi+ukuran, atau audio fingerprint sederhana) dan
tampilkan daftar untuk dihapus/pilih mana yang disimpan.
- **Kenapa**: koleksi musik lokal yang lama biasa punya duplikat dari berbagai sumber
  (unduhan berulang, transfer HP lama).
- **Kompleksitas**: Sedang-Tinggi kalau pakai audio fingerprinting asli (analisis sinyal); Rendah
  kalau cuma cocokkan metadata (judul+artis+durasi mirip) — disarankan mulai dari yang murah dulu.
- **Risiko**: hapus file adalah aksi destruktif — wajib preview + konfirmasi eksplisit + idealnya
  opsi pindah ke folder "Duplikat" dulu (bukan hapus permanen langsung), pola mirip Undo Snackbar
  yang sudah ada di app tapi hapus file fisik tidak bisa di-undo asli (sama kelasnya dengan
  keputusan Batch 26 soal folder tambahan).

## 7. Cadangan & Pulihkan Data Lokal (Backup/Restore)
Ekspor seluruh data app (playlist, favorit, riwayat dengar, rating, pengaturan tema, dst.) ke
satu file `.json`/`.zip` yang bisa disimpan sendiri atau dipindah ke HP lain — sepenuhnya lewat
file lokal (share/copy manual), tanpa cloud/akun.
- **Kenapa**: ganti HP/reset app saat ini berarti semua playlist & riwayat hilang total.
- **Kompleksitas**: Sedang. Serialisasi seluruh `*Store` (SharedPreferences-based) yang sudah
  ada ke satu JSON terstruktur, tulis via MediaStore (pola sama seperti crash logger/log export
  yang sudah ada di `AppLogger`). Restore perlu validasi skema + migrasi kalau versi beda.
- **Risiko**: rendah-sedang — pastikan tidak menimpa data existing tanpa konfirmasi, dan skema
  JSON perlu versioned dari awal supaya backup lama tetap bisa dipulihkan setelah app di-update.

## 8. Trim Keheningan Otomatis (Silence Skip)
Deteksi bagian hening (silence) di awal/akhir file lalu lewati otomatis saat transisi lagu,
opsional bisa di-toggle per-user.
- **Kenapa**: banyak file dari sumber tertentu punya beberapa detik hening yang mengganggu
  alur dengar, terutama saat gapless playback.
- **Kompleksitas**: Sedang-Tinggi. Perlu analisis amplitude sederhana (baca beberapa frame
  PCM di awal/akhir file) — Media3 punya API level rendah untuk ini, tapi butuh riset
  implementasi yang hati-hati supaya tidak menambah lag saat transisi lagu.
- **Risiko**: kalau threshold silence salah, bisa memotong bagian intro/outro yang memang
  senyap secara musikal (bukan silence teknis) — wajib ada toggle on/off + threshold yang
  bisa disesuaikan, jangan default agresif.

## 9. Visualizer Audio (Waveform/Spectrum) ✅ SELESAI (Batch 92)
Animasi visual real-time (waveform bergerak atau spectrum bar) sinkron dengan audio yang
sedang diputar, ditampilkan di Now Playing.
- **Kenapa**: elemen visual yang lazim ada di audio player premium, memperkuat identitas
  visual tiap tema custom yang sudah dikerjakan berbatch-batch (Tactile/Skeu).
- **Kompleksitas**: Sedang. Android punya `android.media.audiofx.Visualizer` bawaan (butuh
  `sessionId` dari ExoPlayer, sudah bisa diambil) — tidak perlu library eksternal, cuma perlu
  render Canvas Compose dari data waveform/FFT yang dikembalikan API itu.
- **Risiko**: `Visualizer` butuh izin `RECORD_AUDIO` di beberapa versi Android (walau cuma baca
  sinyal internal player, bukan mic sungguhan) — perlu dicek behaviour per API level, dan pastikan
  tidak menambah battery drain signifikan (throttle refresh rate).
- **Catatan implementasi**: dikonfirmasi lewat riset — `RECORD_AUDIO` ternyata wajib di SEMUA versi
  Android untuk audio session apa pun (bukan cuma versi tertentu seperti dugaan awal di atas),
  tidak ada pengecualian "sesi sendiri". Izin diminta on-demand (baru saat user nyalakan toggle di
  sheet, bukan di onboarding wajib) via `visualizerPermissionLauncher` di `MainActivity.kt`. Refresh
  rate ditahan ke ~15fps (`AudioVisualizerController.TARGET_CAPTURE_RATE_MILLIHZ`). Sesi audio dipakai
  ulang dari `PlaybackAudioSession` (mekanisme yang sama dipakai `EqualizerController`, ekualizer
  tidak punya cara lain baca `audioSessionId` ExoPlayer karena hanya pegang `MediaController`).
  Detail lengkap & risiko yang belum diverifikasi di device: `CHANGELOG.md` Batch 92.

## 10. Dashboard Statistik Dengar Lokal ✅ SELESAI (Batch 90)
Visualisasi grafik dari data yang sebenarnya sudah dikumpulkan (`PlayStatsStore`,
`ListeningHistoryStore`) — jam favorit dengar musik, genre/artis paling sering, tren mingguan,
dll — saat ini datanya cuma dipakai untuk "Paling Sering Diputar" di Beranda, belum ada halaman
statistik dedicated.
- **Kenapa**: data sudah ada, "cuma" butuh UI baru untuk memvisualisasikannya — return-on-effort
  tinggi dibanding fitur lain di daftar ini.
- **Kompleksitas**: Rendah-Sedang. Murni UI Compose (Canvas custom chart atau grid sederhana)
  di atas data yang sudah tersimpan, tidak ada perubahan data model.
- **Risiko**: rendah.

## 11. Floating Mini Player (Bubble Mode)
Mini player mengambang di atas aplikasi lain (mirip chat bubble Messenger/PiP video), kontrol
play/pause/next tanpa perlu buka app AudioPlayer.
- **Kenapa**: pelengkap Widget yang sudah ada — widget di home screen, bubble ini bisa diakses
  dari layar mana pun (termasuk saat di dalam app lain).
- **Kompleksitas**: Tinggi. Butuh `SYSTEM_ALERT_WINDOW` (overlay permission, izin sensitif yang
  wajib diminta eksplisit ke user lewat Settings sistem) + `WindowManager` service terpisah dari
  `PlaybackService` yang sudah ada.
- **Risiko**: overlay permission sering jadi red flag di mata user (sering disalahgunakan app
  lain) — perlu penjelasan jelas di onboarding kenapa dibutuhkan, dan pastikan tidak
  mengganggu app lain (touch pass-through di luar area bubble).

## 12. Mode Audiobook/Podcast (Per-File Speed & Posisi) ✅ SELESAI (Batch 93)
Ingat kecepatan putar dan posisi terakhir secara individual per-file (bukan pengaturan speed
global yang sekarang berlaku ke semua lagu), plus tampilan progres "menit tersisa" alih-alih
cuma posisi/durasi total — relevan untuk file audio panjang (podcast, buku audio, kuliah).
- **Kenapa**: fitur speed 0.5x-2x sudah ada tapi global; audiobook/podcast biasa butuh speed beda
  dari musik biasa dan wajib resume persis di posisi terakhir per judul.
- **Kompleksitas**: Sedang. Extend `PlaybackStateStore`/`Song` model dengan field per-file
  (speed, lastPosition sudah ada secara umum lewat resume, tinggal di-scope per-song bukan
  global-terakhir-saja).
- **Risiko**: rendah — perluasan data model yang sudah ada, bukan sistem baru dari nol.
- **Catatan implementasi**: bukan extend `PlaybackStateStore` seperti dugaan awal (itu tetap
  murni resume 1 QUEUE global) — dibuat `AudiobookModeStore.kt` terpisah, 1 record JSON per lagu
  (opt-in via toggle di dialog "Pengaturan Putar" yang sudah ada). Resume speed+posisi dipasang
  di `onMediaItemTransition`, sengaja di-skip untuk `MEDIA_ITEM_TRANSITION_REASON_REPEAT` (loop
  Repeat Satu Lagu) supaya tidak fight sama restart-dari-nol repeat-one. "Menit tersisa" pakai
  format `-mm:ss` (konvensi umum podcast player). Detail lengkap: `CHANGELOG.md` Batch 93.


## 13. Konverter Format Audio Lokal
Konversi file antar format umum (MP3 ↔ WAV ↔ FLAC ↔ M4A) langsung di perangkat, hasil disimpan
sebagai file baru.
- **Kenapa**: kadang user butuh format tertentu untuk kompatibilitas alat lain (mis. WAV untuk
  editing, MP3 untuk kompatibilitas device lama) — saat ini app cuma pemutar, bukan alat konversi.
- **Kompleksitas**: Tinggi. Decoding+encoding audio penuh (bukan cuma trim seperti Ringtone
  Cutter) butuh codec encoder yang tidak semua tersedia native di Android `MediaCodec` (mis.
  encoder FLAC/MP3 tidak selalu ada built-in di semua API level) — kemungkinan perlu library
  encoder tambahan (menambah ukuran APK, kontra sama optimasi ukuran APK Batch 28).
- **Risiko**: paling berisiko & paling berat di seluruh daftar ini — pertimbangkan matang-matang
  sebelum eksekusi, termasuk cek lisensi library encoder yang dipakai (beberapa encoder MP3
  historically ada isu paten/lisensi, walau sudah expired di kebanyakan yurisdiksi sejak 2017).

## 14. Vault — Kunci & Sembunyikan Lagu Privat
Folder/koleksi terpisah yang dilindungi PIN sendiri (bisa pakai PIN yang sama dengan App Lock
atau PIN kedua terpisah) untuk menyembunyikan lagu tertentu total dari tampilan Library utama
— beda dari fitur "Kelola folder → sembunyikan folder" yang sudah ada (itu untuk folder scan,
ini untuk lagu individual dengan proteksi akses, bukan cuma exclude dari index).
- **Kenapa**: privasi tambahan untuk lagu personal (rekaman voice memo pribadi yang discan
  sebagai audio, dll) tanpa perlu sembunyikan seluruh app lewat App Lock.
- **Kompleksitas**: Sedang. Reuse `AppLockStore`/`PinLockoutPolicy` yang sudah ada (proven,
  sudah PBKDF2+salt+lockout) untuk PIN vault, tinggal tambah filter tampilan di
  `LibraryFilterStore` (pola sama seperti `shouldKeep` yang sudah ada, tambah 1 kriteria).
- **Risiko**: rendah — banyak infrastruktur sudah ada tinggal disambungkan ulang.

## 15. Alarm Musik (Wake-Up Alarm)
Kebalikan dari Sleep Timer yang sudah ada — alarm yang membunyikan lagu/playlist pilihan
sendiri sebagai nada bangun pada jam tertentu, dengan volume yang naik bertahap (fade-in).
- **Kenapa**: pelengkap natural dari Sleep Timer, use-case umum ("bangun pakai lagu favorit"
  alih-alih nada alarm generik bawaan HP).
- **Kompleksitas**: Sedang-Tinggi. Butuh `AlarmManager` (`setExactAndAllowWhileIdle` untuk
  akurasi walau Doze mode) + trigger `PlaybackService` dari luar konteks UI aktif, mirip pola
  `PlayerWidgetProvider` yang sudah bisa start service dari luar Activity.
- **Risiko**: exact alarm butuh izin `SCHEDULE_EXACT_ALARM` (API 31+, wajib diminta eksplisit
  user di beberapa versi Android) — dan alarm harus tetap bunyi walau app di-force-close/HP
  reboot (butuh `BOOT_COMPLETED` receiver untuk reschedule alarm setelah restart perangkat).

---

## Ringkasan prioritas (usulan, bukan keputusan final)
Diurutkan dari **effort rendah + risiko rendah** ke **effort tinggi + risiko tinggi**, sebagai
saran titik mulai kalau user mau eksekusi bertahap:

| # | Fitur | Effort | Risiko |
|---|---|---|---|
| 10 | Dashboard Statistik Dengar | Rendah | Rendah |
| 4 | A-B Repeat & Bookmark | Sedang | Rendah |
| 2 | Smart Playlist Otomatis | Sedang | Rendah |
| 12 | Mode Audiobook/Podcast | Sedang | Rendah |
| 14 | Vault Lagu Privat | Sedang | Rendah |
| 3 | Editor Lirik LRC Tap-to-Sync | Sedang | Rendah |
| 7 | Cadangan & Pulihkan Data | Sedang | Sedang |
| 5 | Ringtone Cutter | Sedang | Sedang |
| 9 | Visualizer Audio | Sedang | Sedang |
| 1 | Editor Tag Metadata | Sedang-Tinggi | Sedang |
| 6 | Pencari Duplikat | Sedang-Tinggi | Sedang |
| 8 | Trim Keheningan Otomatis | Sedang-Tinggi | Sedang |
| 15 | Alarm Musik | Sedang-Tinggi | Sedang-Tinggi |
| 11 | Floating Mini Player | Tinggi | Tinggi |
| 13 | Konverter Format Audio | Tinggi | Tinggi |

Tidak ada satu pun dari 15 fitur ini yang butuh izin INTERNET — konsisten dengan klaim privasi
app (lihat README § Keputusan Arsitektur: "app ini sengaja tidak punya izin INTERNET sama
sekali"). Beberapa butuh izin sensitif lain (overlay, exact alarm, record-audio-internal,
write-settings) yang perlu diminta eksplisit dengan penjelasan jelas ke user, mengikuti pola
onboarding permission yang sudah ada di app (lihat fitur Onboarding di README).
