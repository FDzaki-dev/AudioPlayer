# SONIX

Audio player Android — Kotlin + Jetpack Compose + Media3 ExoPlayer. 100% offline, tanpa izin
INTERNET sama sekali.

### 📥 Unduh Aplikasi
**[⬇️ Download APK terbaru — GitHub Release](../../releases/latest)** — sudah ditandatangani
(signed), siap install langsung, tidak perlu build sendiri. Setiap push ke `main` otomatis
memicu build baru lewat GitHub Actions (lihat bagian [Build](#build)).

> 🆕 **Update terbaru — Batch 330 (Default crossfade transisi tab navigasi bawah, 1 file):**
> Transisi antar tab Beranda/Perpustakaan/Pengaturan (+ buka Statistik dari Pengaturan)
> sebelumnya cut instan — 0 `enter`/`exitTransition` di level `NavHost`. Sekarang pakai fade
> ringan (`tween(200)` masuk / `tween(150)` keluar, angka reuse persis dari yang sudah ada di
> file yang sama, bukan angka baru). Rute "now_playing" TIDAK terdampak — transisi slide+fade
> khususnya sendiri (sejak lama) tetap berlaku, override eksplisit menang atas default baru
> ini. `MainActivity.kt` (Protected, edit parsial).
> Batch 329 (Matikan blur asli Liquid Glass PERMANEN app-wide, 2 file):
> User pilih opsi paling aman dari 2 opsi yang ditawarkan, setelah root cause stutter/lag Batch
> 328 ditelusuri lebih dalam: blur asli (`hazeEffect`) genuinely aktif di 17/17 `ModalBottomSheet`
> sejak Batch 324 + `MiniPlayerBar` yang SELALU tervisible selama musik main adalah persis biaya
> GPU resample-per-frame yang sudah diperingatkan sejak param `blurRadius` pertama ditambah.
> `hazeEffect` (`BlurUtils.kt`) dihapus dari cabang Liquid Glass; `hazeSource` (`MainActivity.kt`,
> Protected/edit parsial) juga dilepas — 0 consumer tersisa berarti 0 alasan tetap membayar
> capture backdrop tiap frame. Tint (`liquidGlassAlpha`) balik ke fallback opaque 0.85f/0.90f —
> nilai yang sama persis sudah pernah tervalidasi Batch 311-324, kini jadi status permanen.
> Identitas Liquid Glass sekarang tint+edge-glow saja seperti 4 tema lain, TANPA blur asli.
> Batch 328 (REVERT animasi rim-glow Aurora, 3 file): User laporan device
> sungguhan: musik stuttering + lag/glitch swipe sheet "Kontrol Lanjutan". Root cause: berbagi 1
> `rememberInfiniteTransition` (Batch 326) tetap memicu recomposition tiap frame di semua panel
> — dikembalikan ke statis. Alpha Batch 327 (`AuroraRimGlowAlpha` 0.44f) TETAP dipertahankan,
> bukan penyebab regresi. Rim-glow Aurora kini: statis, alpha terang, TIDAK animated.
> Batch 325 (Blur Liquid Glass dikonfirmasi user di device sungguhan —
> `liquidGlassAlpha` diturunkan balik 0.85f/0.90f→0.38f/0.48f, 1 file): Sub-langkah 5/5
> (visual) roadmap blur asli selesai. Performa (GPU/lag) masih belum eksplisit dikonfirmasi.
> Batch 324 (Tuntaskan antrean Batch 322/323: fix `containerColor` di
> `VaultSheet.kt`, 1 file): Ke-7 gap `containerColor`/blur lintas-window yang ditemukan Batch
> 322 TUNTAS semua — 17/17 call site `ModalBottomSheet` app-wide sudah konsisten.
> Batch 323 (Lanjutan Batch 322: fix `containerColor` 3 dari 4 sheet
> tersisa, 3 file): `SignatureMatcherSheet`/`SmartPlaylistScreen`/`UpdateCheckSheet` diperbaiki.
> Batch 322 (Fix blur lintas-window Liquid Glass, 3 file — root cause Batch
> 311 diriset ulang & ternyata keliru, `MainActivity.kt` diperiksa tapi 0 bug jadi TIDAK diubah):**
> 7 sheet kelewat pasang `containerColor = Color.Transparent` (syarat resmi Haze) — 3 diperbaiki
> batch ini, 4 diantre. Blur belum diturunkan tint-nya sampai user konfirmasi visual device.
> Batch 321 (Housekeeping dokumentasi: arsip `PROJECT_STATE.md` Batch
> 58–219 → `PROJECT_STATE_ARCHIVE.md`, 0 kode diubah):** Murni beres-beres dokumentasi internal
> proyek (file catatan sesi kerja terlalu panjang) — 0 fitur/perilaku app berubah sama sekali
> dari Batch 320.
> Batch 320 (Verifikasi integritas rilis, 0 bug, 0 kode diubah): Sesi
> tanpa laporan bug baru — repack ini murni diverifikasi ulang (manifest file, keseimbangan
> kode seluruh file `.kt`, hasil Batch 318/319 dicek cocok) sebelum dikirim ulang. 0 fitur/
> perilaku berubah dari Batch 319.
> Batch 319 (Fix efek persistent hilang via kontrol eksternal setelah app
> di-kill + notifikasi cold-start jadi statis/universal): Kecepatan Putar/Repeat/Shuffle
> sekarang genuinely ikut ke lagu yang diputar walau app di-kill total lalu dikontrol lewat
> widget/media player eksternal/notifikasi (sebelumnya cuma dipulihkan kalau app dibuka).
> Notifikasi cold-start "SONIX" juga dibuat statis: tombol toggle jadi 1 ikon Putar/Jeda gabungan
> kustom (bukan lagi ganti-ganti ikon+label "Jeda"/"Lanjutkan"), teks jadi kalimat tetap (bukan
> lagi judul lagu yang bisa stale) — keduanya tidak lagi bergantung pada sinkronisasi yang oleh
> desainnya sendiri tidak akan pernah 100% akurat.
> Batch 318 (Fix teks "Fade Halus" ke-clip di dialog Pengaturan Putar):
> `SpeedDialog` (Now Playing → ⋮ → Kecepatan) sekarang bisa discroll (`verticalScroll`) — pola
> `Column` fixed tanpa jaring pengaman yang sama seperti Batch 314-316, tapi di `AlertDialog`
> yang luput dari audit ModalBottomSheet batch-batch itu.
> Batch 317 (Kecepatan Putar sekarang persistent): Sebelumnya "Kecepatan
> Putar" cuma hidup di memori — reset ke 1x tiap app dibuka ulang. Sekarang tersimpan otomatis dan
> dipulihkan lintas sesi, berlaku ke lagu apa pun yang diputar (bukan cuma saat lanjut queue lama).
> Mode Audiobook per-lagu tetap override-nya sendiri kalau diaktifkan untuk lagu itu.
> Batch 316 (Tuntaskan audit Batch 314: `verticalScroll` ke 2 sheet terakhir): `UpdateCheckSheet.kt` dan `BackupRestoreSheet.kt` sekarang bisa discroll (`verticalScroll`) — 2 sisa terakhir dari 5 sheet yang diaudit Batch 314. Dengan ini, audit "pola tab serupa" (`Column` tanpa jaring pengaman scroll) TUNTAS untuk semua 5 sheet yang teridentifikasi (`EqualizerSheet.kt`/`RingtoneCutterSheet.kt`/`VisualizerSheet.kt` Batch 315, 2 sheet ini Batch 316).
> Batch 315: `verticalScroll` ke `EqualizerSheet.kt`, `RingtoneCutterSheet.kt`, `VisualizerSheet.kt` — 3 dari 5 sheet yang diaudit Batch 314 karena punya pola sama (`Column` tanpa jaring pengaman scroll, berisiko baris bawah ke-clip diam-diam di layar pendek/font besar).
> Batch 314: Fix sheet "Kontrol Lanjutan" terpotong (`AdvancedControlsSheet` sekarang bisa discroll, sebelumnya "Potong Nada Dering" ke-clip diam-diam) + Equalizer kini auto re-attach ke sesi audio BARU (cold-start/Service restart/lagu baru) lewat hook `PlaybackAudioSession.onSessionIdChanged` — sebelumnya cuma ter-reattach kalau sheet Equalizer dibuka manual.
> Batch 313: Fix CI build gagal dari Batch 312 — `Modifier.padding(horizontal=..., top=..., bottom=...)` mencampur 2 overload berbeda yang tidak valid di `AdvancedControlsSectionHeader`. Diganti ke `Modifier.padding(start, top, end, bottom)` — hasil visual identik, cuma sintaks yang diperbaiki.
> Batch 312: Sheet "Kontrol Lanjutan" dikelompokkan jadi 3 seksi — **Pemutaran** (Antrean/Sleep Timer/Kecepatan/Repeat A-B), **Audio** (Equalizer/Visualizer/Peredam Volume), **Lagu** (Lirik/Edit Info/Potong Nada Dering) — tiap seksi punya label kecil + divider pemisah.
> Fitur Ringtone Cutter sendiri (Batch 121): potong bagian lagu (MP3/M4A) jadi file baru, simpan
> sebagai Nada Dering/Notifikasi/Alarm lewat Kontrol Lanjutan (Roadmap #5).
> Riwayat lengkap ada di `CHANGELOG.md` (selalu terbaru di paling atas).

> **Mulai dari sini kalau ini sesi/percakapan baru:** baca `PROJECT_STATE.md` (status +
> riwayat insiden ringkas), `CHANGELOG.md` (histori tiap batch), dan bagian "Keputusan
> Arsitektur" di bawah sebelum mengubah apa pun — terutama sebelum menyentuh
> `PlaybackService.kt`, `AppLockStore.kt`, atau `app/build.gradle.kts`. Konteks percakapan
> lama tidak ikut ke sesi baru; dua file itu sudah pernah menyebabkan asumsi salah yang baru
> ketahuan setelah build gagal (lihat Batch 10-14 di CHANGELOG).

## Daftar Isi
- [📥 Unduh Aplikasi](#-unduh-aplikasi)
- [Fitur v1](#fitur-v1)
- [Standar Penomoran Versi](#standar-penomoran-versi)
- [Keputusan Arsitektur](#keputusan-arsitektur)
- [Testing](#testing)
- [Belum selesai / dalam pengerjaan](#belum-selesai--dalam-pengerjaan)
- [Catatan jujur soal Gapless Playback](#catatan-jujur-soal-gapless-playback)
- [Build](#build)
- [Rencana v2](#rencana-v2-belum-dibuat)

## Fitur v1
- Scan otomatis semua file audio di perangkat via MediaStore (mendukung codec mainstream: MP3, AAC/M4A, FLAC, WAV, OGG/Vorbis, OPUS, AMR — apa pun yang bisa diindeks sistem)
- Tab **Lagu** (semua lagu, urut judul) dan **Folder** (dikelompokkan per folder, folder = playlist)
- Playback background via foreground service (Media3 `MediaLibraryService`, mendukung Playback Resumption resmi) + kontrol di notification/lock screen
- Now Playing: album art, seek bar, shuffle, repeat (off/all/one), play/pause/next/prev
- Mini player persisten di bagian bawah layar
- Dark mode default
- Pencarian (search) judul/artis di Perpustakaan
- Sleep timer (10/15/30/45/60 menit)
- Kecepatan putar (0.5x–2x), tersimpan otomatis lintas sesi (dipulihkan tiap app dibuka ulang)
- Resume otomatis dari posisi terakhir setelah app ditutup
- Favorit lagu
- **Antrean putar (Queue)**: lihat antrean, naik/turunkan urutan, hapus dari antrean, lompat langsung ke lagu manapun, plus aksi "Putar Berikutnya" / "Tambah ke Antrean" via tekan-lama pada lagu di Perpustakaan
- **Beranda pintar**: bagian "Baru Diputar" dan "Paling Sering Diputar" otomatis terisi dari riwayat dengar, tanpa perlu aksi manual dari user
- **Playlist manual**: buat playlist sendiri lintas folder/album/artis, isi lewat "Tambah ke Playlist" (tekan-lama lagu), atur urutan, hapus lagu, ganti nama, atau hapus playlist — tersedia di tab Playlist pada Perpustakaan
- **Playlist Otomatis (Smart Playlist)**: berbeda dari playlist manual — atur aturan sekali (folder, genre, rentang durasi, rating minimum, rentang tahun rilis, kata kunci judul/artis/album), lagu yang cocok (termasuk lagu baru yang ditambahkan belakangan) otomatis ikut masuk tanpa perlu diisi manual. Tersedia di tab "Otomatis" pada Perpustakaan (dropdown "Lainnya")
- **Genre metadata**: dibaca dari tabel `Genres`/`Genres.Members` MediaStore (satu map id→nama dibangun sekali per scan, bukan query per-lagu) untuk lagu MediaStore, dan dari tag file langsung untuk lagu folder tambahan (SAF). Ikut dalam pencarian Perpustakaan & tersedia sebagai kriteria exact-match Playlist Otomatis
- **Edit Info Lagu (Tag Editor)**: tulis balik metadata (judul/artis/album/artis album/genre/komposer/no. track/no. disc) langsung ke file — tersedia lewat "Kontrol Lanjutan" di Now Playing. **Cakupan MVP saat ini**: lagu MediaStore format MP3 saja (lihat "Belum selesai / dalam pengerjaan" untuk batasan format lain & lagu folder tambahan)
- **Ringtone Cutter**: Now Playing → Kontrol Lanjutan → "Potong Nada Dering" — pilih rentang awal/akhir (1-60 detik) lalu simpan sebagai file baru ke Nada Dering/Notifikasi/Alarm sistem. File asli tidak pernah tersentuh; hasil otomatis muncul di pemilih nada dering bawaan Android (Pengaturan > Suara), aplikasi ini tidak set-as-default otomatis. **Cakupan MVP**: lagu MediaStore format MP3/AAC-M4A saja, 0 preview audio dari sheet potong
- **Vault Lagu Privat**: Pengaturan → "Vault Lagu Privat" — sembunyikan lagu tertentu TOTAL dari Beranda/Library (beda dari "sembunyikan lagu" biasa yang murni toggle tampilan), dilindungi PIN 6 digit sendiri (independen dari PIN Kunci Aplikasi, hash+salt+lockout escalating sama seperti itu). Tambah/keluarkan lagu dari sheet Vault sendiri; nonaktifkan vault otomatis mengembalikan semua lagu ke tampilan normal (file fisik tidak pernah disentuh). **Cakupan MVP**: murni manajemen keanggotaan, belum ada tombol putar langsung dari sheet Vault (keluarkan dulu dari vault untuk memutar)
- **Statistik Dengar**: dashboard di Pengaturan → "Statistik Dengar" — total lagu diputar, estimasi waktu dengar (durasi × jumlah putar), grafik tren 7 hari terakhir, jam favorit dengar musik (dari 24 jam-dalam-hari, seluruh riwayat), dan artis paling sering. Semua dihitung langsung dari data yang sudah dikumpulkan (`PlayStatsStore`/`ListeningHistoryStore`), plus 1 pencatat baru (`HourlyListenStore`) khusus untuk jam favorit
- **Repeat A-B & Bookmark Posisi**: dari Now Playing → Kontrol Lanjutan → "Repeat A-B & Bookmark". Tandai Titik A & B untuk mengulang satu bagian lagu terus-menerus (latihan musik/bahasa, podcast/audiobook) — direset otomatis tiap ganti lagu. Bookmark Posisi menandai beberapa titik favorit per-lagu (intro/reff/solo dll, diberi nama sendiri) untuk lompat cepat kapan pun, tersimpan permanen per lagu
- **Lirik**: tambahkan lirik sendiri per lagu (tempel teks biasa, atau format LRC `[mm:ss.xx]` untuk lirik yang otomatis mengikuti posisi putar dan auto-scroll) — atau pakai **Mode Tap-to-Sync**: tempel lirik polos, lalu tekan "Tandai Sekarang" per baris sambil lagu diputar untuk membuat versi tersinkron tanpa mengetik timestamp manual
- **Tema dinamis**: warna aksen di Now Playing & mini player otomatis diambil dari sampul album lagu yang sedang diputar
- **Kontrol volume** langsung dari Now Playing (slider attenuasi internal app, terpisah dari volume sistem)
- **Kelola folder**: pilih folder mana saja yang mau disertakan/dikecualikan dari pemindaian musik otomatis, **plus tambahkan folder tambahan lewat izin sistem** (Storage Access Framework) untuk memindai audio yang belum terdeteksi MediaStore — bekerja penuh offline/lokal, tidak ada koneksi internet yang terlibat
- **Widget lebih tahan banting**: diperbaiki supaya tetap merespons play/pause/next meski aplikasi sudah disingkirkan total dari recent apps — service langsung "lapor" ke sistem sebagai proses aktif begitu widget ditekan, sebelum sempat dibunuh oleh pengelola baterai agresif (relevan khususnya di skin seperti XOS/MIUI). Widget juga ikut update judul/artis/status main tiap kali lagu berganti
- **Mini Player Mengambang (Bubble)**: pelengkap widget — kontrol play/pause/prev/next mengambang di atas app lain mana pun (mirip chat bubble Messenger), bisa digeser ke mana saja di layar, posisi tersimpan otomatis & di-clamp ulang saat rotasi. Bisa di-**minimize** jadi tab kecil nempel tepi kiri/kanan layar (bukan di-close total — Service tetap hidup), tap lagi untuk buka penuh. Nyala otomatis begitu playback mulai dari widget/notifikasi/headset mana pun, tanpa perlu buka app dulu — plus **Quick Settings Tile** sendiri untuk toggle cepat dari shade notifikasi. Opt-in dari Settings (nonaktif default), butuh izin sistem "tampil di atas app lain" yang diminta eksplisit saat toggle dinyalakan. Foreground service (notifikasi importance minim, nyaris tak kelihatan) + auto-restart setelah HP reboot — jauh lebih tahan dibunuh OS dibanding rilis awal
- **Lewati Keheningan Otomatis**: percepat bagian hening di tengah/awal/akhir lagu saat diputar, pakai deteksi bawaan Media3/ExoPlayer (belum ada slider sensitivitas custom). Opt-in dari Settings (nonaktif default) — bisa memotong intro/outro yang memang senyap secara musikal, coba dulu lalu matikan lagi kalau terasa mengganggu
- **Pencocok Signature APK**: alat diagnostik di Perpustakaan (ikon sidik jari) — pilih dua file APK (versi lama & baru), langsung ketahuan apakah keduanya ditandatangani dengan key yang sama tanpa perlu install dulu. Kalau beda, laporan lengkap (nama package, versi, SHA-256 tiap file) bisa disalin ke papan klip lewat dialog khusus (bisa di-scroll penuh) — bukan cuma tombol OK polos kayak dialog instalasi Android
- **5 tema**: keluarga utama gaya Apple (ikuti sistem, terang, gelap — palet iOS system color, sudut membulat generous) plus dua identitas custom. **Tactile**, murni dari spesifikasi desain Premium AMOLED Hybrid Glassmorphism yang di-supply user (`compose-amoled-hybrid-glass-final.md`, Batch 53 — menggantikan varian literal Midnight Blue Batch 52, hybrid-glass Batch 51, AMOLED-hitam Batch 50, varian terang Batch 49, & Matte Noir sebelumnya). Wajib gelap (bukan ikut sistem, selalu gelap), fondasi AMOLED near-black (`#030508`) dengan permukaan kaca translusen (level glass §5) sebagai material utama, Midnight Blue (`#191970`) HANYA sebagai gradient ambient tipis di layar root (bukan warna latar datar lagi), aksen biru restrain (`#6670FF`), bevel/bayangan gambar-tangan diagonal (bukan aset tekstur bitmap) lewat satu helper terpusat `tactileEmboss()` — bukan sekadar ganti warna datar. Border & glow sengaja direstrain (alpha rendah, literal 0.065/0.035/0.70 dari spec, brush diagonal top-left→bottom-right, bukan border putih terang); sliders/toggles/switches tetap komponen Material3 polos (di luar cakupan batch ini, kandidat
polish berikutnya). Batch 55: tombol play/pause (mini player & Now Playing) — kontrol paling
sering dilihat — sekarang punya bentuk rounded-square + emboss taktil sendiri, beda dari lingkaran
flat Apple; border hero art album disamakan ke arah bevel diagonal yang sama seperti seluruh
permukaan kaca lainnya.

  **Neumorphism — Titanium & Emerald** (Batch 57, redesain total Batch 73, upgrade identitas dari
  Skeuomorphism ke Neumorphism Batch 79-81) — identitas custom kedua, otonom penuh dari Tactile
  (tidak berbagi mekanisme bevel `embossSurface()` maupun struktur border). Panel dibaca "dipahat
  dari material yang sama dengan kanvas" — bukan lagi panel logam bertekstur seperti sebelumnya —
  lewat dual soft-shadow multi-layer di `skeuEmboss()`: sisi gelap (kanan-bawah) & sisi terang
  (kiri-atas) masing-masing beberapa layer offset+alpha bertingkat meniru soft-blur box-shadow
  ganda ala neumorphism klasik, dibatasi `clipRect()` supaya bayangannya tidak pernah meluber ke
  komponen lain di sekitarnya. **0 border, 0 tekstur grain** — ciri khas neumorphism, kedalaman
  murni dari bayangan. Tertekan (pressed) = *concave*, sisi terang/gelap terbalik total (bukan
  cuma mengecil), kesan permukaan masuk ke kanvas. Fondasi tetap charcoal/platinum netral dengan
  aksen Titanium+Silver metalik dingin yang dominan (satu-satunya token di role warna utama), plus
  sentuhan Zamrud/Emerald kecil & disengaja (glint permata kecil saat panel ditekan, satu vena
  tipis di ambient wash layar utama, dan titik permanen di piringan album) — bukan aksen yang
  bersaing dengan Titanium. Wajib gelap-mode-independen (punya ekspresi terang sendiri sejak
  Batch 61). Dipasang di mini player, tombol play/pause (mini & Now Playing), piringan album hero,
  kartu "Lanjutkan Mendengarkan", banner undo-sembunyikan-lagu, badge indikator gesture, dan baris
  pemilih tema. **Typography sendiri** (Batch 305, murni 100% — bukan pinjaman Apple lagi):
  bobot satu tingkat lebih ringan di tiap judul (kebalikan Tactile yang lebih tebal), 0
  letterSpacing di semua slot (huruf ikut dilucuti dari gaya tracking, selaras "0 border/0 grain"
  di atas — kedalaman murni dari bayangan, bukan dari gaya huruf), dan lineHeight paling longgar
  dari seluruh identitas, meniru kesan dipahat dari bantalan material lunak.

  Dipilih lewat tab **Pengaturan** di navigasi bawah, tersimpan otomatis, diterapkan ulang tiap sesi
- **Halaman Pengaturan**: tab baru di navigasi bawah — berisi pemilih tema dan info versi aplikasi (nomor versi + build)
- **Filter Perpustakaan tersimpan**: tab yang terakhir dipilih (Lagu/Album/Artis/Folder/dst.) diingat di antar sesi
- **Onboarding**: layar selamat datang menjelaskan kenapa izin dibutuhkan sebelum dialog izin muncul, dengan fallback "Buka Pengaturan Aplikasi" kalau izin ditolak permanen
- **Optimalisasi**: pemindaian MediaStore kini terpusat satu kali di ViewModel (bukan diulang di setiap Beranda/Perpustakaan/Playlist), mengurangi kerja I/O berulang dan flicker loading tiap pindah tab
- **Navigasi gesture back**: gesture back sistem sekarang navigasi bertahap sesuai stack (Now Playing → Perpustakaan/Beranda), bukan langsung keluar aplikasi
- **Now Playing lebih atmosferik**: backdrop blur dari album art (seperti Spotify/Apple Music) di belakang piringan hitam yang berputar, dipadu warna aksen dinamis per lagu
- **Micro-interaction**: tombol play/pause, shuffle, repeat, favorit, dan navigasi lagu kini punya animasi "bounce" halus tiap ditekan — bukan cuma ganti ikon instan
- **Status bar & navigation bar** ikut mode terang/gelap tema aktif (ikon gelap di tema terang, ikon terang di tema gelap/Tactile) — bukan dipaksa satu arah
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
`versionCode` dan `versionName` (Batch 30, formula `versionName` diperbarui Batch 86) **sama-sama
otomatis**, keduanya turunan dari jumlah commit git (`gitCommitCount()` di `app/build.gradle.kts`)
— tidak ada lagi angka yang perlu diingat/dibump manual. `versionName` berpola
`MAJOR.MINOR.PATCH` dengan `MINOR = jumlah_commit / 50` dan `PATCH = jumlah_commit % 50` (jadi
genuinely naik seiring waktu — `1.0.x` → `1.1.x` → `1.2.x` dst — bukan nomor tengah yang beku
selamanya di `1.0` seperti skema lama), contoh commit ke-267 → `1.5.17`. `MAJOR` (`1`) tetap
konstanta yang di-set manual dengan sengaja — sama seperti hampir semua skema semver "otomatis"
lain (termasuk tooling semantic-release), bump MAJOR selalu butuh sinyal breaking-change dari
manusia, bukan sesuatu yang aman dihitung otomatis dari jumlah commit.

Konsekuensi praktis: nomor yang tampil di app (`Settings → AudioPlayer versi 1.5.17 (build 267)`)
dan nomor di tag GitHub Release/nama file APK (`AudioPlayer-v1.5.17-run42.apk`) **selalu
sama persis** — keduanya menghitung `git rev-list --count HEAD` dari commit yang sama lalu
formula MAJOR.MINOR.PATCH yang identik, jadi tidak mungkin drift, walau dihitung di dua tempat
terpisah (Gradle & `.github/workflows/build.yml`) dan tidak butuh salah satunya membaca dari yang
lain — kalau `commitsPerMinor` diubah di salah satu tempat, HARUS diubah sama di yang satunya
juga (dicatat sebagai komentar di kedua file).

Nama file ZIP hasil tiap batch pengembangan (`AudioPlayer-batchN-release.zip`) tetap melacak nomor batch percakapan, **bukan** versionName/versionCode — ini sengaja tetap terpisah, karena nomor batch melacak paket kerja per sesi chat, sedangkan versionName/versionCode melacak histori commit git; keduanya naik dengan kecepatan berbeda (satu batch chat bisa berisi banyak commit).

File APK hasil build dan asset GitHub Release membawa nomor versi di namanya, diakhiri `-run<nomor run>` (`AudioPlayer-v1.5.17-run42.apk`) — bukan nama generik statis, dan sengaja **tanpa** short commit hash di belakang supaya nama file tetap stabil/gampang dikenali. Penamaan ini dikerjakan di level workflow CI (`.github/workflows/build.yml`), bukan dobel dengan Gradle, biar tidak saling tabrak. Kata "release" yang dulu ada di antara versi & run number dihapus (Batch 107) — run number sendiri sudah cukup bikin nama unik per run, "release" di tengah cuma noise.

**Tag vs judul rilis (Batch 107)** — git tag (`v1.5.17-run42`, dipakai `tag_name` di workflow) tetap wajib menyertakan run number supaya unik per run (alasan sama seperti nama file APK di atas, lihat juga Batch 65). Judul rilis yang tampil di daftar Releases repo (`name` di workflow) SENGAJA dipisah dari tag — cuma nomor versi polos (`v1.5.17`, tanpa run number), biar daftar rilis di halaman repo enak dibaca. Keduanya diturunkan dari `$VERSION_NAME` yang sama persis di satu baris workflow, jadi tetap tidak mungkin drift satu sama lain maupun dari APK — cuma dua representasi berbeda dari angka yang sama (unik-untuk-tag vs minimalis-untuk-tampilan).

## Keputusan Arsitektur
Ringkasan kenapa, bukan cuma apa — supaya sesi kerja berikutnya (chat AI baru sekalipun) tidak perlu menebak ulang alasan di balik hal-hal yang tidak jelas kalau cuma baca kode.

- **`MediaLibraryService`, bukan `MediaSessionService`**: dibutuhkan spesifik untuk Playback Resumption resmi Android (kartu resume di System UI, kontrol lewat Bluetooth walau proses aplikasi sudah mati). `MediaSessionService` saja tidak cukup untuk fitur ini per dokumentasi resmi Media3.
- **Notifikasi cold-start terpisah** (`startForegroundColdStartNotification`): saat widget home screen ditekan sementara aplikasi benar-benar mati, harus ada notifikasi foreground service **instan** sebelum proses pemulihan antrean (query MediaStore, dst.) selesai — beberapa skin Android (XOS/MIUI dkk) membunuh proses baru dalam hitungan saat kalau belum ditandai foreground. Notifikasi ini sengaja sementara, digantikan notifikasi asli Media3 begitu lagu benar-benar mulai.
- **`onTaskRemoved` hanya mematikan sesi kalau antrean kosong**: sebelumnya juga mati kalau musik sedang dijeda, yang berarti kontrol lock screen ikut hilang setiap kali app di-swipe saat tidak sedang main lagu. Catatan jujur: ini perbaikan yang tidak cukup sendirian — lihat batasan Playback Resumption di bawah.
- **Log diagnostik lokal (`AppLogger`), bukan Crashlytics/Sentry**: app ini tidak punya izin INTERNET sama sekali dan itu bagian dari klaim privasinya (diverifikasi eksplisit: semua pemrosesan lokal di HP). Crash reporting pihak ketiga akan butuh izin itu.
- **`PinLockoutPolicy` dipisah dari `AppLockStore`**: murni supaya rumus lockout-nya bisa di-unit-test tanpa perlu Context/SharedPreferences.
- **`ShakePulseTracker` dipisah dari `ShakeDetector`** (Batch 27): pola sama seperti `PinLockoutPolicy` — state machine pulse-counting shake-to-skip (fix Batch 25) diekstrak murni dari `SensorEventListener`/`SensorManager` supaya bisa di-unit-test langsung, bukan cuma dianalisis dari baca kode.
- **`MusicRepository.deriveFolderName` & `LibraryFilterStore.shouldKeep` jadi pure function di companion object** (Batch 27): folder-name parsing dan filter gabungan folder/lagu-disembunyikan sebelumnya menyatu dengan kelas ber-Context, jadi tidak testable. `shouldKeep` sengaja menerima `folderPath`/`id` polos, bukan `Song` utuh — `Song.uri` bertipe `android.net.Uri` yang tidak aman dikonstruksi di unit test JVM murni (lihat bagian Testing di bawah).
- **`favoriteIds`/`selectedIds` pakai `ImmutableSet<Long>` (kotlinx.collections.immutable), bukan `Set<Long>` biasa**: `Set`/`List` bawaan Kotlin selalu dianggap *unstable* oleh Compose compiler (tidak ada jaminan compile-time bebas mutasi in-place), jadi composable manapun yang menerimanya sebagai parameter tidak bisa di-skip recomposition-nya sekalipun isinya sama persis — di `LibraryScreen`, ini bikin seluruh baris lagu yang sedang tampil ikut recompose setiap kali state lain yang tidak terkait berubah (mis. posisi playback yang tick tiap detik). Konsekuensinya: kalau ada `Set<Long>`/`List<T>` baru yang dikirim sebagai parameter ke composable list yang sering di-recompose, pertimbangkan pola yang sama, bukan `Set`/`List` polos.
- **Compose stability config (`app/compose_stability_config.conf`) menandai `android.net.Uri` sebagai stable**: `Song.uri` bertipe `Uri` (tipe platform yang tidak bisa diverifikasi compiler), yang bikin seluruh `Song` dianggap unstable walau field lainnya (Long/String) sudah stabil. `Uri` sendiri efektif immutable (tidak ada setter publik), jadi aman ditandai stable secara manual.
- **`androidx.concurrent:concurrent-futures`, bukan `com.google.guava:guava` penuh** (Batch 28): satu-satunya alasan Guava ada di project ini adalah API session callback Media3 yang mengembalikan `ListenableFuture` — bukan kebutuhan Guava yang sesungguhnya (collections/cache/dst. tidak dipakai sama sekali). `concurrent-futures` menyediakan `CallbackToFutureAdapter` untuk konstruksi `ListenableFuture` async, dan tipe `ListenableFuture`-nya sendiri datang dari shim kecil `com.google.guava:listenablefuture:1.0` (dependency transitif, bukan jar Guava penuh).

**Batasan yang tidak bisa dihilangkan dari sisi kode:** HP dengan pengelolaan RAM agresif (sebagian skin Xiaomi/Oppo/Vivo, dan sebagian Samsung) tetap bisa membunuh proses aplikasi kapan saja kecuali user manual whitelist app itu di pengaturan baterai — ini keterbatasan Android, bukan sesuatu yang bisa diperbaiki murni lewat kode di app manapun.

Riwayat lebih detail per batch pengembangan ada di `CHANGELOG.md`.

## Testing
Semua test di `app/src/test` adalah **pure JVM test** (`testImplementation("junit:junit:4.13.2")`) — tidak ada Robolectric/instrumentation, tidak ada emulator. Konsekuensinya: hanya business logic yang benar-benar lepas dari `Context`/framework Android (SharedPreferences, ContentResolver, SensorManager, dst.) yang bisa ditest langsung. Pola yang dipakai berulang kali di proyek ini (`PinLockoutPolicy`, dan sejak Batch 27: `ShakePulseTracker`, `MusicRepository.deriveFolderName`, `LibraryFilterStore.shouldKeep`): pisahkan logika murninya ke fungsi/kelas pure, method instance yang butuh Context tinggal delegasikan ke situ tanpa ubah perilaku.

CI (`.github/workflows/build.yml`) menjalankan `gradle testDebugUnitTest` sejak Batch 27, sebelum step build APK — sebelum itu test yang ada di repo tidak pernah benar-benar dijalankan otomatis.

`android.net.Uri` tidak bisa dikonstruksi aman di unit test JVM tanpa Robolectric —
`Uri.parse(...)` dkk mengembalikan `null` di bawah `isReturnDefaultValues = true`, bukan
placeholder aman. Dua pola dipakai tergantung situasi: (1) kalau fungsi pure-nya kita yang
tulis, ubah supaya menerima field mentah (`folderPath`, `id`) saja, bukan `Song` utuh
(`LibraryFilterStore.shouldKeep`); (2) kalau harus benar-benar ada objek `Song` utuh
(`LibrarySearchIndexTest`, karena fungsi yang ditest menerima `List<Song>`), pakai
`org.mockito:mockito-core` → `mock(Uri::class.java)` untuk dapat instance yang tidak throw,
tanpa peduli isinya.

## Belum selesai / dalam pengerjaan
- Tag Editor (Edit Info Lagu) baru menulis MP3/ID3v2.3 untuk lagu MediaStore — FLAC/OGG/M4A/WMA belum didukung (masing-masing format biner beda total, butuh writer terpisah per format), dan lagu dari folder tambahan (SAF) juga belum bisa diedit (folder tambahan cuma diberi izin baca saat ditambahkan, belum ada alur minta izin tulis). Kedua batasan ini ditampilkan langsung ke user lewat pesan di sheet edit, bukan gagal diam-diam
- Gradle Wrapper (`gradlew`/`gradle/wrapper/gradle-wrapper.jar`) belum ada — jar biner itu perlu di-generate lewat `gradle wrapper` (butuh Gradle terpasang) atau diunduh, keduanya tidak tersedia dari lingkungan kerja yang dipakai mengembangkan proyek ini; CI pakai `gradle` dari `setup-gradle@v3` sebagai workaround
- Lint sebagai quality gate rilis (`checkReleaseBuilds`) masih dimatikan — mengaktifkannya butuh baseline lint sungguhan dari `gradle lint` (butuh Android SDK/Gradle terpasang) supaya tidak asal menyalakan gate atas warning yang belum pernah dilihat
- Shared-element transition sungguhan (mini player → Now Playing sebagai satu elemen visual) belum ada — versi sekarang pakai animasi scale-in sebagai pendekatan yang lebih aman (lihat catatan di riwayat commit); butuh bump Compose BOM dari 2024.05.00
- Pull-to-refresh gesture di Library belum ada (cuma tombol manual + auto-refresh saat resume/ContentObserver) — API pull-to-refresh Material3 yang simpel baru stabil di versi Compose BOM lebih baru; naikkan BOM berisiko ke komponen lain yang sudah jalan
- Ekstraksi penuh string hardcode ke `strings.xml` (untuk i18n) belum dikerjakan — 339 string literal tersebar di banyak file (Batch 54 audit), refactor mekanis sebesar itu belum aman dikerjakan tanpa akses compiler untuk verifikasi
- Migrasi ~340 literal `.dp` non-radius (padding/size/offset/blur) ke token spacing terpusat belum dikerjakan — mayoritas one-off/context-specific, beda dari corner-radius yang sudah disentralisasi ke `Spacing.kt`/`Radius` sejak Batch 54; risiko sweep besar tanpa compiler sama seperti poin string di atas
- Belum pernah diuji di perangkat fisik sungguhan oleh siapa pun selain lewat deskripsi/screenshot — termasuk hasil build release dengan minify yang baru aktif sejak Batch 7

## Catatan jujur soal Gapless Playback
Mesin pemutarannya **sudah gapless secara arsitektur** sejak awal — satu ExoPlayer yang hidup terus sepanjang sesi, memutar satu playlist asli (`setMediaItems`/`addMediaItem`), bukan mengganti lagu satu per satu dengan restart/re-prepare. Ini menghilangkan penyebab paling umum dari "jeda" antar lagu (loading ulang, klik/pop transisi). Toggle "Gapless (Murni)" vs "Fade Halus" di dialog Pengaturan Putar menentukan apakah transisi itu **dibiarkan alami** (gapless, tanpa sentuhan volume sama sekali) atau **benar-benar tumpang tindih dua sumber suara** menjelang pergantian lagu.

**Sejak Batch 102**, "Fade Halus" bukan lagi efek volume satu pemutar (landai turun lalu naik lagi di sekitar jeda senyap yang disamarkan) — sekarang dua instance ExoPlayer sungguhan main bersamaan selama beberapa detik terakhir tiap lagu: yang lama melandai turun, yang baru sudah mulai main dan melandai naik, betul-betul tumpang tindih di output audio, baru bertukar kendali secara senyap begitu volume yang lama mencapai nol. Detail mekanismenya (dan kenapa `MediaSession.setPlayer()` hot-swap sengaja dihindari) ada di `CrossfadeEngine.kt` — sesuatu yang, jujur, hanya masuk akal untuk didokumentasikan di kode, bukan di sini.

Batasan yang disadari dan belum dibereskan: Equalizer/Visualizer terikat ke audio session ID satu ExoPlayer (yang "utama"), jadi selama beberapa detik overlap, EQ/visualizer belum ikut memengaruhi suara lagu berikutnya yang baru masuk. Dan kalau slider volume digeser tepat saat lagu sedang bertukar, gesekannya bisa terasa "menyusul" sesaat sampai proses tumpang-tindihnya selesai (di bawah 3 detik). Keduanya fitur opt-in dan sempit dampaknya, jadi sengaja belum diprioritaskan — dicatat di `PROJECT_STATE.md` biar tidak terlupa.

Untuk file **FLAC/WAV** (lossless), jalur "Gapless (Murni)" menjamin sambungan sempurna tanpa jeda — dijamin oleh cara format itu bekerja, bukan sekadar klaim.

Untuk **MP3/AAC** (lossy), gapless yang benar-benar sample-accurate juga bergantung pada metadata encoder di file itu sendiri (LAME tag / iTunSMPB) dan seberapa tepat decoder ExoPlayer memangkas padding-nya — ini terjadi di level library, bukan sesuatu yang bisa "ditambahkan" lewat kode aplikasi ini, dan saya belum bisa memverifikasinya dengan telinga di device fisik. Kalau setelah dicoba masih kerasa ada jeda halus khusus di file MP3/AAC tertentu, itu petunjuk berharga — kabari, biar bisa ditelusuri lebih spesifik ke file/formatnya.

## Build
Build otomatis lewat GitHub Actions setiap push ke `main`. Hasil APK release diunggah sebagai **GitHub Release** bertag `v<versi>-release` (bukan CI artifact — release asset di-serve GitHub apa adanya, tanpa dibungkus `.zip`, dan bisa diunduh publik tanpa login). Kalau secret `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, dan `SIGNING_KEY_PASSWORD` sudah diisi di pengaturan repo, APK ditandatangani pakai keystore release asli — kalau salah satu kosong, otomatis jatuh ke debug key tanpa bikin build gagal.

## Rencana v2 (belum dibuat)
- **Redesign identitas visual terinspirasi CONVX ("Liquid Glass")** — arah baru sejak Batch 278,
  tema ke-5 aktif. Shape/typography sendiri (fase 1-4) final. **Blur asli via Haze
  (`hazeSource`/`hazeEffect`) DIMATIKAN PERMANEN app-wide sejak Batch 329.** Setelah blur genuinely
  aktif di 17/17 `ModalBottomSheet` (Batch 324) & dikonfirmasi visual user (Batch 325), device
  sungguhan justru melaporkan musik stuttering + lag/glitch swipe sheet "Kontrol Lanjutan"; root
  cause: resample GPU per-frame dari blur asli + `MiniPlayerBar` yang SELALU tervisible selama
  musik main — persis risiko yang sudah diperingatkan sejak param `blurRadius` pertama ditambah
  (Batch 296). Sesuai `STABILITY > Speed`, fase 5/5 blur DITUTUP dengan keputusan final: kembali
  ke tint solid opaque (`liquidGlassAlpha` 0.85f gelap / 0.90f terang — fallback yang sama persis
  sudah pernah tervalidasi Batch 311-324), bukan blur. Identitas Liquid Glass sekarang murni
  tint+edge-glow seperti 4 tema lain, TANPA blur asli — final, bukan lagi kandidat tuning
  lanjutan. Detail: `PROJECT_STATE.md` Batch 329, `ROADMAP_LIQUID_GLASS_REDESIGN.md`,
  `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md`.
- **Tema ke-6 "Aurora"** — arah baru sejak Batch 306, permintaan user eksplisit "100% ide sendiri,
  tanpa contek gaya desain visual apapun manapun". Mekanisme orisinal: warna gradien ambient yang
  mengalir pelan (animated hue-shift antar hijau→teal→ungu→magenta, terinspirasi spektrum aurora
  borealis asli), berbeda total dari mekanisme 5 tema lain (bukan shadow/bevel, bukan blur, bukan
  artefak retro). Terkunci **gelap permanen** (pola sama Calm Retro). **Fase 4/N** (Batch 309) —
  `AuroraShapes` terpasang, mekanisme asimetris PERTAMA di project ini (2 sudut diagonal lebih
  membulat mengikuti arah alir gradien `auroraGlow()`, bukan rounded-rect seragam seperti 5 tema
  lain) — dengan itu color (Batch 307) + typography (Batch 308, bobot Light/Normal paling ringan
  dari 6 identitas) + shape (Batch 309) sudah 100% murni sendiri, 0 lagi fallback ke identitas
  lain. **Fase 5/N selesai** (Batch 310) — rim-glow per-panel (sebelumnya ditunda sejak Batch 306)
  akhirnya di-wiring app-wide lewat `frostedGlass()`: MiniPlayerBar, panel NowPlaying, tiap bottom
  sheet, dan card Home/Library sekarang punya rim ber-gradasi 4 warna Aurora di tepinya. **Fase
  6/N — Batch 326 mencoba ANIMATED (1 phase float dibagi via `LocalAuroraPhase`), TAPI
  **DIREVERT Batch 328**: user device sungguhan lapor musik stuttering + lag/glitch swipe sheet
  "Kontrol Lanjutan" — berbagi 1 `rememberInfiniteTransition` tetap memicu recomposition tiap
  frame di semua panel (termasuk `MiniPlayerBar` yang selalu tervisible selama musik main).
  Rim-glow kini **statis permanen** (bukan lagi kandidat animasi — keputusan final, proyek masuk
  fase discontinued). **Batch 327** — user device sungguhan: rim-glow "terlalu tipis, hampir tak
  kasat mata"; alpha puncaknya dinaikkan ke token baru `AuroraRimGlowAlpha` (0.44f, terpisah dari
  `AuroraGlowAlpha` 0.34f milik ambient wash yang tidak dikeluhkan/tidak disentuh) — nilai ini
  TETAP dipakai statis pasca-revert Batch 328. **Dengan
  ini cakupan Aurora yang dikonfirmasi user Batch 306 (ambient background bergerak + rim-glow
  per-panel statis-terang) SELESAI**, di atas color+typography+shape yang sudah lengkap
  sejak Batch 309.
  Detail & urutan fase di `PROJECT_STATE.md`/`CHANGELOG.md` Batch 306-310, 326-328.
- Shared-element transition mini player ↔ Now Playing (butuh bump versi Compose)
- Lirik otomatis (cari/unduh dari internet — versi sekarang murni input manual)
