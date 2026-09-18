# PROJECT_STATE.md

[BRANDING_NAME: SONIX]
[TERMUX_ROOT: audioplayer]

RAM instan sesi kerja — hanya rule AKTIF final. Tanpa histori revisi, kutipan user, atau
kronologi batch. Histori lengkap tiap batch: `CHANGELOG.md`. Ringkasan fitur: `README.md`.
Arsip batch lama (1-424): `docs/archive/PROJECT_STATE_ARCHIVE.md`.

## ✅ STATUS PROYEK: ACTIVE
Banner DISCONTINUED dicabut eksplisit oleh user (Batch 432). Proyek lanjut normal, sektor dibuka
per instruksi eksplisit user seperti biasa (lihat "Sektor DITUTUP" di bawah untuk yang masih
butuh reopen spesifik).

**Catatan Batch 489 [input baru: `QA_Checklist_SONIX_v488_terisi.md`, instruksi eksplisit user
"tanamkan planning dokumen tersebut kedalam project yang disesuaikan dengan kondisi nyata"]**:
checklist adalah hasil audit source-inspection eksternal (BUKAN hasil kerja batch mana pun di
project ini) terhadap `SONIX_v488.zip` — 5 kategori fitur core (Kontrol Pemutaran, Audio
Back-End, Playlist/Queue, Integrasi Sistem, File/Eror) ditandai `[x]`/`[~]`/`[-]` per kriteria.
Checklist ITU SENDIRI eksplisit membedakan 2 kelas gap: (a) gap SUMBER yang bisa dikonfirmasi
dari baca kode (9 poin di "Verdict"), (b) item yang murni BELUM diverifikasi device fisik (tidak
bisa "difix" lewat kode sama sekali, mis. latency nyata, kombinasi tekan TWS, ketahanan RAM
jangka panjang). Batch ini memverifikasi ULANG kesembilan poin (a) satu-satu ke kode sungguhan
(bukan menelan klaim checklist mentah-mentah — checklist adalah dokumen eksternal, P1 tetap ZIP/
source, bukan checklist), lalu MENGEKSEKUSI 1 gap yang paling jelas & aman diperbaiki batch ini
(TUNNEL VISION — sisanya masuk roadmap terlacak di bawah, BUKAN dikerjakan sekaligus).

**Verifikasi ulang 9 gap "Verdict" checklist terhadap source (bukan tebakan)**:
1. **Stop playback eksplisit** — DIKONFIRMASI BENAR gap nyata: grep `controller?.stop()` app-wide
   cuma 1 hit, di dalam `PlayerViewModel.dismissMiniPlayer()` (pola "cancel" — ikut mengosongkan
   queue/state tersimpan via `UndoableAction`, BUKAN tombol Stop biasa). 0 kontrol Stop berdiri
   sendiri di UI mana pun. **DIPERBAIKI batch ini** (lihat di bawah).
2. **Previous 3 detik** — DICEK ke `PlaybackService.kt` (ExoPlayer.Builder) & `PlayerViewModel.
   previous()`: BENAR 0 ada override eksplisit `setSeekBackIncrementMs`/threshold custom, murni
   `controller?.seekToPreviousMediaItem()` polos. Catatan tambahan (tidak ada di checklist):
   default Media3 (`Player.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS`) SUDAH persis 3000ms — jadi
   perilaku RUNTIME kemungkinan besar sudah sesuai spec, gap-nya murni "tidak eksplisit/tidak
   didokumentasikan sebagai keputusan sadar" (rawan berubah diam-diam kalau versi Media3 di-bump
   nanti). **BELUM diperbaiki batch ini** — kandidat fix murah (1 baris `setMaxSeekToPreviousPositionMs`
   di scope `ExoPlayer.Builder` `PlaybackService.kt` untuk mengunci nilainya secara eksplisit,
   bukan warisan default library), masuk roadmap di bawah, BUKAN dikerjakan bareng gap #1 supaya
   batch ini tetap tunnel-vision 1 sektor (transport control UI) tanpa merambah `PlaybackService.kt`.
3. **Shuffle anti-repeat-nearby** — DICEK: shuffle 100% delegasi ke `Player.shuffleModeEnabled`
   Media3 bawaan, 0 algoritma custom di project ini. Constraint "tidak mengulang lagu yang sama
   dalam waktu dekat" MEMANG tidak dijamin (benar, sesuai checklist) — TAPI ini scope FITUR BARU
   (custom shuffle engine), bukan sekadar "gap kecil", risiko regresi ke urutan queue/persistence
   (`PlaybackStateStore`) kalau dikerjakan tergesa. **BELUM diperbaiki** — butuh instruksi
   eksplisit user dulu sebelum dikerjakan (pola sama seperti sektor pill/tab-sync: fitur besar,
   bukan micro-task), dicatat di roadmap.
4. **Bluetooth reconnection** — DICEK `PlaybackService.kt`: `setHandleAudioBecomingNoisy(true)` +
   MediaSession resumption SUDAH terpasang (bukan gap kode) — checklist sendiri sudah bilang ini
   murni butuh uji HARDWARE, 0 baris kode kandidat fix. **Tidak actionable lewat kode.**
5. **Audio focus semua skenario** — DICEK: `AudioAttributes` + `handleAudioFocus = true` Media3
   SUDAH benar dipasang (bukan gap kode) — checklist sendiri bilang murni butuh device-test lintas
   skenario telepon/WA-call/alarm/Maps. **Tidak actionable lewat kode.**
6. **Filter audio pendek (WhatsApp/ringtone/game effect)** — DICEK `MusicRepository.kt`:
   DIKONFIRMASI BENAR, `BASE_SELECTION` cuma `IS_MUSIC != 0 AND DURATION > 0`, 0 batas durasi
   minimum. **BELUM diperbaiki batch ini** — kandidat fix (duration-floor HANYA di `getAllSongs()`,
   `getSongsByIds()` TETAP tidak disentuh supaya lagu pendek yang SUDAH ada di playlist/queue/
   favorit sebelum fix ini tidak mendadak hilang) masuk roadmap, sengaja dipisah dari gap #1 biar
   1 batch = 1 file yang disentuh (`MusicRepository.kt` murni, tanpa dicampur `ui`/`playback`).
7. **Streaming audio online** — checklist SENDIRI sudah menandai ini `[-]` (di luar scope, app ini
   pemutar lokal). **Tidak ada aksi, bukan gap.**
8. **Process-death/background/lock-screen/TWS** — semua murni device-QA, 0 kandidat fix kode baru
   yang tidak sudah dibahas gap #4/#5 di atas. **Tidak actionable lewat kode.**
9. **Multi-format decoder robustness** — DICEK: dukungan format (MP3/AAC/FLAC/WAV/OGG/OPUS/AMR)
   sepenuhnya diwariskan dari MediaStore/Media3 platform, project ini 0 punya decoder sendiri buat
   diaudit. **Tidak actionable lewat kode** — butuh corpus file nyata + device test, sesuai
   checklist sendiri.

**1 file diubah** (dalam batas 3 file/tugas — kode inti) + 2 file wiring UI/Activity (total tetap
dianggap 1 sektor/1 tugas, pola sama Batch 484 yang eksplisit menyebut alasan melebihi hitungan
normal): `PlayerViewModel.kt`, `NowPlayingScreen.kt`, `MainActivity.kt`.
1. `PlayerViewModel.kt`: `stopPlayback()` baru — `pause()` + `seekTo(0L)` lewat `controller`.
   SENGAJA BUKAN `controller.stop()` mentah (itu pindah Media3 ke `STATE_IDLE`, butuh `prepare()`
   ulang sebelum `play()` berikutnya — jalur lifecycle itu 0 diaudit batch ini, lebih aman
   pause+seekTo yang hasil akhirnya sama-sama "berhenti + posisi balik ke 0" tanpa mengubah
   player state machine). 0 menyentuh queue/`currentQueueSlotIds`/`playbackStateStore`/
   `UndoableAction` — beda total dari `dismissMiniPlayer()` (itu "cancel", ini "stop").
2. `NowPlayingScreen.kt`: parameter baru `onStopPlayback` diteruskan ke `AdvancedControlsSheet`,
   1 `AdvancedControlRow` baru ("Stop Pemutaran", ikon `Icons.Default.Stop`) di seksi "Pemutaran"
   sheet "Kontrol Lanjutan" — SENGAJA TIDAK ditaruh di Row transport utama (Shuffle/Prev/Play/
   Next/Repeat, 5 ikon `SpaceEvenly`) supaya 0 menyentuh layout yang sudah di-tuning berkali-kali
   (Batch 170/224/226/469-472 dst) — pola "kontrol yang jarang dipakai casual listener masuk
   sheet Lanjutan" ini SUDAH jadi konvensi file ini sejak Batch 312 (KDoc `AdvancedControlsSheet`).
   0 dialog konfirmasi (beda dari dismiss mini player yang butuh Undo) — aksinya reversibel
   sendiri, tinggal tekan Play lagi.
3. `MainActivity.kt`: 1 baris wiring `onStopPlayback = { playerViewModel.stopPlayback() }` di
   satu-satunya titik panggil `NowPlayingScreen(...)` (`nowPlayingContent` lambda, Batch 101 —
   dipakai Compact & Expanded/two-pane, jadi 1 wiring ini otomatis berlaku ke keduanya).

**0 diverifikasi CI/device Batch 489** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren/bracket: `PlayerViewModel.kt` `{}` 248/248 `()` 1017/1017 `[]` 39/39;
`NowPlayingScreen.kt` `{}` 293/293 `()` 1299/1299 `[]` 1/1; `MainActivity.kt` `{}` 341/341
`()` 1270/1270 `[]` 3/3). **WAJIB DITEST user**:
1. Buka lagu apa saja → Now Playing → ketuk ⋮ ("Kontrol Lanjutan") → scroll ke seksi "Pemutaran"
   → baris baru "Stop Pemutaran" harus muncul (ikon kotak/Stop) di bawah "Repeat A-B & Bookmark".
2. Ketuk baris itu → sheet tertutup, musik BERHENTI + slider posisi balik ke 0:00, TAPI judul
   lagu/artwork/antrean TETAP tampil (bukan reset ke Home/kosong seperti swipe-dismiss mini
   player) — tekan tombol Play (▶) lagi → lagu yang SAMA lanjut main dari awal (0:00), BUKAN
   pindah ke lagu lain di antrean.
3. Pastikan 0 regresi ke swipe-dismiss mini player (Batch 476/480 — masih "cancel total" +
   Snackbar "Urungkan", TIDAK disentuh batch ini) dan ke 5 tombol transport utama (Shuffle/Prev/
   Play/Next/Repeat) — 0 perubahan posisi/ukuran/handler di Row itu.
4. Pastikan 0 crash/force-close saat build (1 fungsi ViewModel baru + 1 parameter baru diteruskan
   lewat 1 composable — 0 signature lama yang dihapus/diubah, semua penambahan murni).

**Roadmap Gap QA v488 (BELUM dikerjakan, urutan bukan prioritas mutlak — tunggu arahan user atau
lanjutkan sebagai micro-task berikutnya)**:
- **Gap #2 (Previous 3 detik eksplisit)** — 1 baris kandidat: `setSeekBackIncrementMs`/setara di
  `ExoPlayer.Builder` (`PlaybackService.kt`) mengunci 3000ms eksplisit alih-alih warisan default
  Media3. Risiko rendah, scope 1 file.
- **Gap #6 (Filter audio pendek)** — kandidat: `MusicRepository.kt`, `getAllSongs()` SAJA dapat
  selection baru dengan `DURATION > <ambang>` (mis. 30 detik, ambang umum industri musik-vs-clip),
  `getSongsByIds()`/`BASE_SELECTION` lama TETAP tidak disentuh (lagu pendek yang SUDAH ada di
  playlist/favorit/queue tidak boleh mendadak hilang — cek `getSongsByIds()` dipakai
  `PlaybackService.kt` resume-queue & `LyricsPrefetchWorker.kt`, KEDUANYA harus tetap bisa
  resolve ID lama). Ambang pasti (30 detik? beda per kategori WhatsApp vs ringtone?) sebaiknya
  dikonfirmasi user dulu — bukan angka yang disebutkan checklist secara eksplisit, murni asumsi
  konservatif kalau dieksekusi tanpa konfirmasi.
- **Gap #3 (Shuffle anti-repeat-nearby)** — FITUR BARU (custom shuffle engine), bukan micro-task
  kecil. Instruksi eksplisit user disyaratkan dulu sebelum dikerjakan (pola sama sektor pill/
  tab-sync: fitur besar butuh audit 1 batch penuh, bukan diselipkan).
- **Gap #4/#5/#8/#9** — 0 kandidat fix kode (kode sudah benar per baca-kode batch ini), murni
  menunggu hasil device-QA fisik dari user. Checklist asli disalin ke `docs/QA_CHECKLIST_SONIX_v488.md`
  sebagai rujukan detail per-poin (bukan diarsipkan — masih aktif, lihat "Aturan sesi aktif" baru
  di bawah).

**Catatan Batch 488 [laporan urgent user: "app tidak load berulang kali setiap aplikasi baru
dibuka kembali pasca app di kill!!"]**: laporan BARU, BUKAN reopen sektor DITUTUP manapun,
TERPISAH dari antrean `.animateItem()` (Batch 481-487 di bawah — TIDAK disentuh/TIDAK berubah
oleh batch ini, resume chain itu tetap berlaku persis seperti tercatat).

**Root cause TERKONFIRMASI dari pembacaan kode (bukan tebakan)**: `PlayerViewModel.
ensureLibraryLoaded()` — `libraryLoadedOnce` murni in-memory `var`, SELALU `false` lagi di
proses baru (app-kill lalu dibuka lagi) → `refreshLibrary()` (full scan MediaStore+SAF) dipicu
ulang dari nol TIAP KALI, `_libraryLoading=true` (shimmer skeleton `HomeScreen`/`LibraryScreen`)
menutupi list SETIAP app dibuka lagi walau library 0 berubah sejak sesi lalu. Sudah
didokumentasikan sendiri Batch 386 ("jalur paling panas cold-start") & Batch 436 (investigasi
laporan user "nunggu buffer ±20 detik") — Batch 436 waktu itu MENYIMPULKAN ini "expected
behavior" (bukan bug), TIDAK di-fix. Batch ini merevisi kesimpulan itu jadi di-fix, sesuai
laporan urgent user.

**2 file diubah** (dalam batas 3 file/tugas, 1 di antaranya file baru): `LibraryCacheStore.kt`
(BARU), `PlayerViewModel.kt`.
1. `LibraryCacheStore.kt` (baru, `data/`): snapshot `List<Song>` hasil scan terakhir disimpan ke
   disk (`context.filesDir`, JSON via `org.json` — bagian Android SDK, 0 dependency baru
   ditambah `build.gradle.kts`) lewat temp-file-lalu-rename (proses di-kill di tengah `save()`
   tidak pernah menyisakan file cache korup). `save()`/`load()` murni sinkron — caller
   (`PlayerViewModel`) yang wajib dispatch ke `Dispatchers.IO`, pola sama persis Store lain
   (`PlaybackStateStore` dkk) di package ini.
2. `PlayerViewModel.kt`:
   - `ensureLibraryLoaded()`: sebelum `refreshLibrary()`, muat cache disk dulu (IO) — kalau ada
     & tidak kosong, `_librarySongs`/`_libraryLoading` diisi LANGSUNG dari situ (list asli
     tampil seketika, 0 shimmer) + `maybeSyncMiniPlayerOnColdStart()` ikut dipanggil (3 titik
     panggil sekarang, pola sama Batch 476 "menutup race mana pun selesai duluan"), LALU
     `refreshLibrary(silent = true)` TETAP jalan di background (menangkap perubahan lagu asli
     sejak sesi lalu — scan asli 0 dihapus/dilewati, cuma tidak lagi memicu shimmer). Kalau
     cache tidak ada/korup/kosong (mis. install baru) → turun ke `refreshLibrary()` biasa
     (`silent=false`), 0 berubah dari sebelum batch ini.
   - `refreshLibrary()`: param baru `silent: Boolean = false` — `_libraryLoading=true` HANYA
     kalau `!silent`. **0 titik panggil LAMA berubah** (pull-to-refresh & tombol "Pindai Ulang"
     `MainActivity.kt`, content-observer, dkk — SEMUA masih panggil tanpa argumen = default
     `false`, shimmer/loading feedback rescan manual TETAP tampil PERSIS seperti sebelumnya).
   - Setelah scan sukses (`_librarySongs.value = songs`): `libraryCacheStore.save(songs)`
     dipanggil fire-and-forget di `Dispatchers.IO` terpisah — gagal/lambatnya cache write TIDAK
     PERNAH menunda update UI (`_librarySongs` sudah di-assign duluan di baris sebelumnya).

**0 diverifikasi CI/device Batch 488** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren/bracket: `PlayerViewModel.kt` `{}` 246/246 `()` 1003/1003 `[]` 38/38;
`LibraryCacheStore.kt` baru `{}` 11/11 `()` 109/109 `[]` 1/1). **WAJIB DITEST user**:
1. Buka app, tunggu library selesai dimuat sekali (boleh shimmer). Force-close TOTAL (App Info →
   Force Stop, ATAU swipe dari Recents lalu tunggu OS recycle proses). Buka app lagi dari
   launcher → Home/Library HARUS langsung tampil isi (0 shimmer/layar kosong), bukan nunggu scan
   ulang seperti sebelumnya.
2. Ulangi test #1 sekali lagi (buka→force-close→buka) → tetap instan & konsisten.
3. Tambah/hapus 1 lagu (file manager/app lain) SAAT app dalam kondisi force-close, lalu buka app
   lagi → list tetap tampil INSTAN dari cache dulu (boleh sesaat belum mencerminkan perubahan
   itu), lalu dalam beberapa detik berikutnya (scan senyap background selesai) list ikut update
   mencerminkan lagu yang ditambah/dihapus — TANPA shimmer muncul di tengah proses ini.
4. Tombol "Pindai Ulang" (Library screen) DAN pull-to-refresh: pastikan shimmer/loading feedback
   SAAT DITEKAN MANUAL tetap muncul seperti biasa (0 regresi — SENGAJA tidak diubah, beda dari
   cold-start otomatis di atas).
5. Install baru / clear app data (skenario 0 cache) → shimmer pertama kali TETAP muncul seperti
   biasa (0 regresi ke kondisi ini — cuma kondisi cold-start BERIKUTNYA yang berubah).
6. Pastikan 0 crash/force-close saat build (1 import baru `LibraryCacheStore` di
   `PlayerViewModel.kt`, 0 dependency baru di `build.gradle.kts`).

Lanjutan: sektor ini dianggap TUNTAS menunggu konfirmasi device 6 poin di atas. Antrean
`.animateItem()` (Batch 481-487) & seluruh investigasi lain (bubble/tab-nav/mini-player) TIDAK
terpengaruh/TIDAK berubah oleh batch ini — lanjutkan sesuai instruksi eksplisit user berikutnya.

**Catatan Batch 486 [instruksi user: "lanjutkan progress yang tertunda!!" — TANPA konfirmasi
eksplisit hasil test Batch 485 (Song Picker/AB Repeat/Equalizer)]**: hasil test Batch 485 BELUM
dikonfirmasi user di sesi ini — dicatat di sini supaya sesi berikutnya 0 salah asumsi ("AI DILARANG
menebak histori"). Resume point Batch 486 memberi 2 cabang: (a) kalau test 485 ✅ → lanjut
`.animateItem()` ke `SmartPlaylistScreen.kt`/`LyricsSheet.kt`; (b) kalau tidak → sektor
pill/tab-sync + gesture-drag, TAPI cabang (b) eksplisit butuh "instruksi eksplisit user utk sektor
itu spesifik" yang TIDAK ada di pesan ini. Karena cabang (b) terkunci tanpa instruksi spesifik,
satu-satunya lanjutan valid dari instruksi generik "lanjutkan" adalah cabang (a) — dieksekusi di
bawah. Sektor pill/tab-sync TETAP 0 disentuh.

**2 file diubah** (dalam batas 3 file/tugas): `SmartPlaylistScreen.kt`, `LyricsSheet.kt`.
1. Kedua file dicek dulu (pola sama Batch 482-485): grep `isDragging`/`draggable`/`reorder` = 0
   hit di keduanya → aman dari resiko `.animateItem()` berebut dgn gesture drag.
2. `SmartPlaylistScreen.kt` (4 titik, semua key sudah stabil sejak awal): (i) list playlist
   otomatis (`itemsIndexed`, key `p.id`); (ii) list lagu cocok aturan (`itemsIndexed`, key
   `song.id`); (iii) chip filter folder di builder sheet (`items`, key = string folder itu
   sendiri); (iv) chip filter genre di builder sheet (`items`, key = string genre itu sendiri) —
   (iii)/(iv) pola sama chip preset `EqualizerSheet.kt` Batch 485 (chip statis, animasi hanya utk
   transisi selected-state/layout, bukan insert/remove).
3. `LyricsSheet.kt` (1 titik, prioritas RENDAH per rasional Batch 483): list baris lirik
   (`itemsIndexed`, key = INDEX bukan id konten — list dibangun ulang via `remember(rawLyrics)`,
   0 pernah insert/remove saat playback normal). Ditambah tetap demi konsistensi antrean; dampak
   nyata cuma muncul di alur edit-lirik-lalu-simpan (jumlah baris berubah) — logic sync
   highlight/auto-scroll (`activeIndex`/`LaunchedEffect`) 0 disentuh.
4. **0 disentuh**: logic filter/aturan smart playlist, logic parse/sync lirik, sektor
   pill/tab-sync + gesture-drag custom (mini player/queue) — keputusan Batch 483 masih berlaku,
   TIDAK dimulai tanpa instruksi eksplisit user utk sektor itu.

**0 diverifikasi CI/device Batch 486** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren dicek per file: `SmartPlaylistScreen.kt` `{}` 106/106 `()` 284/284;
`LyricsSheet.kt` `{}` 65/65 `()` 191/191, keduanya match). **WAJIB DITEST user (gabung dgn test
Batch 485 yang masih pending)**:
1. Smart Playlist → buka list "Playlist Otomatis" (kalau >1 playlist) → transisi tetap mulus saat
   playlist dihapus (FAB ikon sama, 0 regresi); buka salah satu playlist → list "lagu cocok" harus
   tetap smooth kalau aturan diubah lewat pensil (jumlah lagu cocok berubah).
2. Smart Playlist Builder (tombol pensil/buat baru) → toggle chip Folder & Genre → transisi
   visual chip (selected state) tetap mulus, 0 regresi ke pemilihan folder/genre atau simpan
   aturan.
3. Lyrics (buka lirik lagu apa saja) → scroll/auto-scroll sync tetap presisi, 0 regresi; kalau
   sempat edit lirik lalu simpan → transisi baris baru/hilang boleh terlihat (bukan bug).
4. Pastikan 0 crash/force-close saat build (0 signature/parameter baru, murni modifier tambahan).
**[RESUME POINT Batch 487]**: kalau SEMUA test Batch 485 + 486 di atas ✅ (Song Picker, AB Repeat,
Equalizer, Smart Playlist, Lyrics), antrean micro-task polish `.animateItem()` HABIS — baru boleh
lanjut ke sektor pill/tab-sync + gesture-drag custom (mini player/queue), TAPI TETAP WAJIB
instruksi eksplisit user utk sektor itu spesifik (bukan otomatis dari "lanjutkan" generik) krn
riwayat regresi (Batch 448/477/479) butuh audit 1 batch penuh, bukan diselipkan. Kalau ada test
di atas ❌, laporkan detail kegagalannya dulu — jangan lanjut sektor manapun sebelum root cause
jelas.

**Catatan Batch 485 [user konfirmasi fix biometrik Batch 484 ✅ ("applause"), instruksi: "lanjut
kerjakan Polish yang masih tertunda"]**: lanjut resume point Batch 484 (masih valid, 0 berubah
sejak ditulis) — micro-task polish animasi antrean Batch 481/482/483.

**3 file diubah** (dalam batas 3 file/tugas): `SongPickerSheet.kt`, `ABRepeatBookmarkSheet.kt`,
`EqualizerSheet.kt`.
1. Ketiganya dicek dulu (pola sama Batch 482/483): grep `isDragging`/`draggable`/`reorder` di
   masing-masing file = 0 hit → aman dari resiko `.animateItem()` "berebut" dgn gesture drag.
   `SongPickerSheet.kt` punya gesture custom "sweep-select" (`isSweeping`/`rowBoundsInRoot`,
   long-press+drag utk tandai rentang checkbox) — DIPERIKSA MANUAL, ini BUKAN drag-reorder (0
   memindah posisi item, cuma menandai rentang), jadi tetap aman ditambah `.animateItem()`.
2. `SongPickerSheet.kt`: `.animateItem()` di awal modifier chain row lagu (`itemsIndexed`, key
   `song.id` sudah ada sejak awal — prasyarat terpenuhi).
3. `ABRepeatBookmarkSheet.kt`: `BookmarkRow` (private, 1 titik panggil) dapat parameter baru
   `modifier: Modifier = Modifier` (default aman, 0 breaking, pola PERSIS `DuplicateSongRow`
   Batch 482) diteruskan ke root `Row`; titik panggil diberi `modifier = Modifier.animateItem()`.
4. `EqualizerSheet.kt`: 2 `FilterChip` inline di dalam masing-masing `LazyRow` (Preset Kuat +
   Preset Bawaan Perangkat) langsung ditambah `.animateItem()` di awal modifier chain — 0 perlu
   ubah signature (FilterChip sudah terima `modifier` langsung), key masing-masing sudah stabil
   (`preset.name` / `state.presets[index]`).
5. **0 disentuh**: `SmartPlaylistScreen.kt`/`LyricsSheet.kt` (di luar 3 file resume point batch
   ini — tetap di antrean kalau user minta lanjut), logic filter/sweep-select/checkbox
   (SongPicker), logic jump/delete bookmark (ABRepeat), logic select/enabled preset (Equalizer)
   — 0 baris logic berubah, murni tambahan modifier animasi. Sektor pill/tab-sync +
   gesture-drag custom (mini player/queue) TETAP tidak disentuh (keputusan Batch 483 masih
   berlaku).

**0 diverifikasi CI/device Batch 485** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren dicek per file, semua match: `SongPickerSheet.kt` `{}` cocok, `()` cocok;
`ABRepeatBookmarkSheet.kt` `{}` cocok, `()` cocok; `EqualizerSheet.kt` `{}` cocok, `()` cocok).
**WAJIB DITEST user**:
1. Song Picker (mis. tambah lagu ke playlist/queue) → ketik di search box supaya daftar
   terfilter naik-turun → baris yang muncul/hilang harus slide+fade halus (bukan pop instan),
   sisa baris geser mengisi celah; sweep-select (long-press lalu geser utk centang banyak
   sekaligus) harus tetap akurat 100% (0 regresi ke rentang yang ditandai).
2. AB Repeat → tambah beberapa bookmark lalu hapus salah satu → baris yang dihapus slide+fade
   keluar halus, sisa baris geser naik mengisi celah, 0 regresi ke jump-to-position/rename.
3. Equalizer → ganti-ganti preset (Kuat maupun Bawaan Perangkat) → transisi visual chip
   (selected state, layout) tetap mulus, 0 regresi ke enabled/disabled state atau band slider.
4. Pastikan 0 crash/force-close saat build (parameter baru `modifier` di `BookmarkRow`, default
   value jadi 0 breaking di satu-satunya titik panggil).
**[RESUME POINT Batch 486]**: kalau test di atas ✅, `SmartPlaylistScreen.kt`/`LyricsSheet.kt`
masih di antrean `.animateItem()` kalau user minta lanjut ke situ (LyricsSheet prioritas RENDAH,
lihat rasional Batch 483 — list statis 0 pernah insert/remove runtime). Kalau tidak, lanjut ke
sektor pill/tab-sync + gesture-drag custom (mini player/queue) — PALING TERAKHIR per keputusan
Batch 483/484, butuh audit 1 batch penuh (bukan diselipkan) krn riwayat regresi (Batch
448/477/479) — jangan mulai dari situ tanpa instruksi eksplisit user utk sektor itu spesifik.

**Catatan Batch 484 [2 instruksi eksplisit user dalam 1 pesan: (1) "regresi lain di bagian
keamanan yaitu absennya biometrik fingerprint beserta label sidik jari diujung bawah" (Kunci
Aplikasi/LockScreen), (2) "sekalian tanamkan biometrik juga pada vault"]**:

**(1) BUG FIX — root cause TERKONFIRMASI dari dokumentasi platform (bukan tebakan)**:
`AndroidManifest.xml` TIDAK PERNAH mendeklarasikan `android.permission.USE_BIOMETRIC`
(grep app-wide: 0 hasil sebelum batch ini). Dokumentasi resmi `BiometricManager.canAuthenticate()`
eksplisit "Requires android.Manifest.permission#USE_BIOMETRIC" — tanpa deklarasi ini,
`canAuthenticate()` tidak pernah balas `BIOMETRIC_SUCCESS`, jadi `isBiometricAvailable()` di
`MainActivity.kt` SELALU false app-wide, meng-gate HILANG toggle "Buka dengan Sidik Jari" di
Settings DAN tombol sidik jari di `LockScreen.kt` — cocok persis gejala di screenshot user
(numpad PIN tanpa apa-apa di kiri-bawah). Kotlin `LockScreen.kt`/`MainActivity.kt`/
`PlayerViewModel.kt`/`AppLockStore.kt` DIPERIKSA SATU-SATU, wiring-nya sudah 100% benar sejak
awal — 0 bug logic di sana, gap murni 1 baris permission yang hilang.
1. `AndroidManifest.xml`: tambah `<uses-permission android:name="android.permission.USE_BIOMETRIC" />`.
2. `LockScreen.kt`: sebelumnya tombol sidik jari HANYA punya `contentDescription` (teks
   accessibility, tidak pernah kelihatan di layar) — 0 label visual di UI sungguhan. Tambah
   `Text("Sidik Jari")` di bawah ikon (dibungkus `Column`), sesuai instruksi eksplisit user
   ("...beserta label sidik jari").

**(2) FITUR BARU — biometrik Vault** (sebelumnya 0 ada sama sekali, Vault cuma PIN manual):
1. `VaultStore.kt`: `isBiometricEnabled()`/`setBiometricEnabled()` ditambah (pola identik
   `AppLockStore`, prefs KEY baru `vault_biometric_enabled`, own prefs file — TETAP independen
   dari `AppLockStore` sesuai KDoc lama, 0 reuse silang). `disableVault()` ikut clear flag ini.
2. `VaultSheet.kt`: helper privat `isVaultBiometricAvailable()`/`showVaultBiometricPrompt()`
   ditambah LOKAL di file ini (bukan reuse `MainActivity`, sengaja — sheet ini sudah dari awal
   "self-contained, no dependency on AppLockStore", pola sama dipertahankan utk fitur baru).
   Auto-prompt begitu gerbang PIN vault tampil (persis pola `LaunchedEffect` Kunci Aplikasi di
   `MainActivity.kt`) + tombol manual "Sidik Jari" di `VaultUnlockSection` (fallback kalau
   prompt di-cancel) + toggle Switch "Buka dengan Sidik Jari" baru di `VaultContentSection`
   (tampil hanya kalau `biometricAvailable`, sama syarat dgn toggle Kunci Aplikasi di Settings).

**4 file diubah** (di atas batas normal 3 file/tugas — sengaja dilanggar krn user eksplisit minta
2 hal terpisah [fix regresi + fitur baru] dalam 1 pesan yang sama; masing-masing perubahan tetap
minimal & 1 file [`AndroidManifest.xml`] cuma 1 baris tambahan): `AndroidManifest.xml`,
`LockScreen.kt`, `VaultStore.kt`, `VaultSheet.kt`.

**0 diverifikasi CI/device Batch 484** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren: `VaultSheet.kt` `{}` 119/119 `()` 292/292; `LockScreen.kt` `{}` 49/49 `()` 132/132;
`VaultStore.kt` `{}` 26/26 `()` 116/116; manifest XML divalidasi `xmllint --noout` ✅). **WAJIB
DITEST user**:
1. Settings → Kunci Aplikasi → toggle "Buka dengan Sidik Jari" harus MUNCUL (sebelumnya hilang
   total) di device yang punya sidik jari terdaftar; nyalakan lalu buka app dari cold-start →
   ikon sidik jari + teks "Sidik Jari" harus tampil di pojok kiri-bawah numpad PIN.
2. Settings → Vault (buka pakai PIN vault dulu) → scroll ke bawah panel → toggle "Buka dengan
   Sidik Jari" baru harus muncul, nyalakan → tutup & buka lagi tab Vault → prompt sidik jari
   harus auto-muncul; tombol "Sidik Jari" manual di bawah tombol "Buka" jadi fallback kalau
   prompt itu di-cancel.
3. Device TANPA sidik jari terdaftar: toggle biometrik di Settings maupun di Vault harus TETAP
   sembunyi (bukan crash) — perilaku `biometricAvailable`/`canAuthenticate()` gate, 0 diubah.
4. Pastikan 0 crash/force-close saat build (semua import baru sudah ada dependency-nya:
   `androidx.biometric:biometric:1.1.0` sudah lama ada di `app/build.gradle.kts`, 0 dependency
   baru ditambah).
**[RESUME POINT Batch 485]**: kalau test di atas ✅, kembali ke antrean micro-task polish animasi
Batch 481/482 (resume Batch 483 di bawah — masih valid, 0 berubah oleh batch ini): lanjut
`.animateItem()` ke `SongPickerSheet.kt`/`ABRepeatBookmarkSheet.kt`/`EqualizerSheet.kt` (cek
drag-reorder dulu sebelum sentuh). Sektor pill/tab-sync + gesture-drag custom TETAP paling akhir.

**Catatan Batch 483 [BUG FIX regresi/gap dilaporkan user: "background glass tembus pandang pada
tab vault, fix it dan audit yang gejalanya serupa"]**: root cause TERKONFIRMASI dari perbandingan
kode app-wide (bukan tebakan) — BUKAN regresi dari Batch 481/482 (2 batch itu 0 pernah menyentuh
`ModalBottomSheet`/`Column` root VaultSheet). Gap asli: `containerColor = Color.Transparent`
(pola app-wide, dipasang Batch 322/323 sbg fix "blur lintas-window") WAJIB dipasangkan dgn
`.frostedGlass()` di Column konten supaya panel tetap solid — tanpanya, panel benar-benar
tembus pandang (bukan efek estetika, literal 0 alpha). Audit `.frostedGlass()` Batch 340
("lanjutan antrean Audit tambahan Batch 339") menyisir BackupRestoreSheet/DiagnosticLogSheet/
UpdateCheckSheet/DuplicateFinderSheet dkk, TAPI tidak mencakup 3 file. Audit ulang app-wide batch
ini (grep SEMUA 16 file `ModalBottomSheet`+`containerColor=Transparent` vs SEMUA yang punya
`.frostedGlass()`): tepat 3 file bolong — `VaultSheet.kt` (dilaporkan user), `SmartPlaylistScreen.kt`,
`SignatureMatcherSheet.kt`. 13 file lain sudah benar (0 disentuh).

**3 file diubah** (dalam batas 3 file/tugas — pas): `VaultSheet.kt`, `SmartPlaylistScreen.kt`,
`SignatureMatcherSheet.kt`.
1. Ketiganya: `.frostedGlass()` ditambah ke Column konten (posisi setelah `.fillMaxWidth()`,
   pola PERSIS 13 file lain yang sudah benar), dipanggil TANPA argumen (pola sama 12/12 call
   site existing, 0 parameter/angka baru) + import `com.rudi.audioplayer.ui.theme.frostedGlass`
   ditambah di ketiganya (2 file lain belum pernah import ini sama sekali).
2. `SmartPlaylistScreen.kt`: comment historis Batch 263 (rationale `LocalOverscrollConfiguration
   provides null`) SEMPAT tidak sengaja terhapus draft awal — dikembalikan utuh sebelum commit,
   digabung berurutan dgn comment baru Batch 483 (0 histori hilang).
3. 0 logic lain disentuh di ketiga file (PIN gate, filter draft, ApkSignatureChecker, dsb) — 100%
   scope ini murni 1 modifier tambahan x3 titik yang identik.

**0 diverifikasi CI/device Batch 483** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren: `VaultSheet.kt` `{}` 101/101 `()` 238/238; `SmartPlaylistScreen.kt` `{}` 106/106
`()` 273/273; `SignatureMatcherSheet.kt` `{}` 58/58 `()` 145/145). **WAJIB DITEST user**:
1. Buka tab Vault (Pengaturan → Vault) → panel harus terlihat solid/tinted (bukan tembus pandang
   ke layar di belakangnya), sama seperti sheet lain (mis. Cari Duplikat).
2. Pengaturan → Perpustakaan → "Buat Playlist Otomatis" → panel solid, DAN scroll masih halus
   0 regresi ke fix Batch 262/263 (bouncy-scroll) yang TIDAK disentuh batch ini.
3. Pengaturan → menu update/signature checker (Pencocok Signature APK) → panel solid.
4. Pastikan 0 crash/force-close saat build (3 import baru, 3 pemanggilan modifier tanpa
   argumen — pola identik 12 call site lain yang sudah lama jalan aman).
**[RESUME POINT Batch 484]**: kalau test di atas ✅, kembali ke antrean micro-task polish animasi
Batch 481/482 (lihat resume Batch 482 di atas — masih valid, tidak berubah oleh fix bug ini):
lanjut `.animateItem()` ke `SongPickerSheet.kt`/`ABRepeatBookmarkSheet.kt`/`EqualizerSheet.kt`
(cek drag-reorder dulu sebelum sentuh, sama seperti Batch 482). Sektor pill/tab-sync +
gesture-drag custom TETAP paling akhir.

**Catatan Batch 482 [lanjutan instruksi eksplisit user Batch 481: "polish semua effect animasi,
transisi, dll. agar mulus like iOS" — user konfirmasi "mantap, move"]**: micro-task 2/3.
Rencana resume Batch 481 sempat menaruh "(a) audit pill/tab" sbg prioritas pertama — SETELAH
digali datanya (grep app-wide), (a) ternyata BUKAN sektor terpisah dari gesture-drag custom
(sama-sama 1 web `navPillIndexAnim`+`glassAlphaAnim`+`tabDragOffset`+`tabBarOverscrollAnim`+
drag-physics), jadi risikonya SAMA dengan item (c) yang sudah ditandai "PALING TERAKHIR" — bukan
diagnosis baru, cuma info yang belum lengkap saat resume ditulis. Urutan disusun ulang
berdasarkan data ini (bukan tebakan): (a) DILEWATI dulu, lanjut ke (b) yang independen dari
drag-physics.

**2 file diubah** (dalam batas 3 file/tugas): `DuplicateFinderSheet.kt`, `VaultSheet.kt`.
1. **`.animateItem()` pada list hapus-lagu** (kedua file, list sudah punya `key` stabil sejak
   awal — prasyarat animateItem sudah terpenuhi, cuma modifier belum pernah dipasang, pola
   PERSIS SAMA `LibraryScreen.kt`/`QueueSheet.kt` yang sudah ada, 0 pola baru): dipilih krn
   KEDUA sheet ini murni delete-list (0 drag-reorder, diverifikasi grep `isDragging`/`draggable`/
   `reorder` = 0 hit di kedua file — aman dari resiko `.animateItem()` "berebut" dgn gesture
   drag seperti kasus `QueueSheet`/`PlaylistScreen`) dan interaksi hapusnya sering berulang
   (cocok literal dgn keluhan "peralihan instant yang mengganggu" user).
   - `DuplicateFinderSheet.kt`: `DuplicateSongRow` (private, 2 titik panggil: grup library +
     grup fisik) dapat parameter baru `modifier: Modifier = Modifier` (default aman, 0 breaking)
     diteruskan ke root `Row`; 2 titik panggil diberi `modifier = Modifier.animateItem()`.
   - `VaultSheet.kt`: 2 `Row` inline di dalam `items{}` (`vaultedSongs` & `candidates`) langsung
     ditambah `.animateItem()` di awal modifier chain — 0 perlu ubah signature apa pun (Row-nya
     sudah inline, bukan composable terpisah).
2. **0 disentuh**: pill/tab sync (dilewati, lihat rasional di atas), gesture-drag mini
   player/queue, easing NavHost (sudah Batch 481), checkbox/toggle/select/hapus logic kedua
   file (0 baris logic berubah, murni tambahan modifier animasi).

**0 diverifikasi CI/device Batch 482** — 0 env Android nyata/device fisik/compiler Kotlin sesi
ini (balance brace/paren `DuplicateFinderSheet.kt`: `{}` 62/62, `()` 131/131; `VaultSheet.kt`:
`{}` 101/101, `()` 222/222). **WAJIB DITEST user**:
1. Vault → tandai/hapus beberapa lagu dari daftar vault → tiap baris yang hilang harus slide+
   fade keluar halus (bukan pop instan), sisa baris di bawahnya geser naik halus mengisi celah.
2. Vault → dialog "Tambah" → tambah lagu dari daftar kandidat → baris yang baru ditambah
   masuk halus, baris lain di daftar kandidat geser mengisi celah yang ditinggalkan.
3. Pengaturan → Cari Duplikat → centang beberapa lagu (grup Library maupun grup File Fisik) →
   hapus → baris yang dihapus slide+fade keluar halus, 0 pop instan, 0 regresi ke centang/
   hapus/pilih-semua yang sudah ada.
4. Pastikan 0 crash/force-close saat build (parameter baru `modifier` di `DuplicateSongRow`,
   default value jadi 0 breaking di titik panggil manapun yang mungkin terlewat).
**[RESUME POINT Batch 483]**: kalau test di atas ✅, lanjut micro-task 3/3: `.animateItem()` ke
sisa sheet sekunder yang masih polos & TERKONFIRMASI 0 drag-reorder — cek dulu satu-satu sebelum
sentuh (`SmartPlaylistScreen.kt`, `SongPickerSheet.kt`, `ABRepeatBookmarkSheet.kt`,
`EqualizerSheet.kt`, `LyricsSheet.kt` — LyricsSheet prioritas RENDAH krn list-nya statis 0 pernah
insert/remove runtime, `.animateItem()` disana 0 banyak berguna). Sektor pill/tab-sync +
gesture-drag custom (mini player/queue) TETAP paling akhir, butuh audit KHUSUS 1 batch penuh
(bukan diselipkan) krn riwayat regresi (Batch 448/477/479) — jangan mulai dari situ tanpa
instruksi eksplisit user utk sektor itu spesifik.

**Catatan Batch 481 [instruksi eksplisit user: "polish semua effect animasi, transisi, dll. agar
mulus like iOS, landai tanpa peralihan instant yang mengganggu"]**: target "semua" terlalu masif
utk 1 batch (>12 file punya `tween(...)` app-wide, lihat audit di bawah) — AUTO-HALT/
MICRO-TASKING dipakai: eksekusi bagian teraman+paling universal dulu (transisi level-NavHost,
kena SETIAP tab switch + setiap push/pop, bukan sektor gesture custom yang rawan desync), sisanya
ke `[RESUME POINT]`.

**2 file diubah** (dalam batas 3 file/tugas): `Motion.kt` (BARU), `MainActivity.kt`.
1. **Token gerak bersama** (`Motion.kt`, file baru — 0 file lama disentuh oleh keberadaannya
   sendiri): `Motion.IosEasing` = `CubicBezierEasing(0.42f, 0f, 0.58f, 1f)`, mendekati kurva
   bawaan iOS `easeInEaseOut` (S-curve simetris) — beda dari default `tween()` Compose
   (`FastOutSlowInEasing`, kurva Material deselerasi-berat, "gaya Android"). Plus 5 konstanta
   durasi (`DURATION_QUICK=150/STANDARD=200/TAB=220/EMPHASIZED=300/SCREEN=350`) — SEMUA alias
   angka yang SUDAH dipakai app-wide (audit `tween()` app-wide Batch 481, bukan angka baru).
2. **Transisi level-NavHost** (`MainActivity.kt`): 9 pemanggilan `tween(...)` di 3 blok —
   (a) NavHost root (fade home/library/settings, `enterTransition`/`exitTransition`/
   `popEnterTransition`/`popExitTransition`, Batch 330), (b) push "stats_dashboard"
   (slide+fade, Batch 331), (c) push "now_playing" (slide+fade, existing) — SEMUA diganti dari
   `tween(N)` polos jadi `tween(Motion.DURATION_X, easing = Motion.IosEasing)`. **0 angka durasi
   diubah** (200→DURATION_STANDARD, 150→DURATION_QUICK, 300→DURATION_EMPHASIZED,
   350→DURATION_SCREEN, murni alias) — **HANYA kurva easing** yang berubah (default
   FastOutSlowIn → IosEasing). Non-breaking murni di lapisan interpolasi, 0 threshold/gesture/
   state logic disentuh.

**SENGAJA TIDAK disentuh batch ini (bagian dari micro-task, BUKAN diabaikan)**: `tween(220)`
sinkron pill/tab (`navPillIndexAnim` + `glassAlphaAnim` + `tabBarDragFocus`, dekat
`CustomNavBarTabItem`/`GlassTabIcon`) — comment kode eksplisit bilang durasi ini "SAMA PERSIS"
antar ≥2 Animatable supaya tiba bersamaan (lihat histori desync Batch 479, root cause beda tapi
gejala sama: pill vs konten kehilangan sync). Ganti easing salah satu tanpa yang lain BERISIKO
desync baru (kurva beda = laju tengah beda meski durasi sama) — butuh audit semua Animatable
tersinkron sekaligus, di luar tunnel-vision batch ini. Juga belum disentuh: kurva easing di
`MiniPlayerBar.kt`/`NowPlayingScreen.kt`/`QueueSheet.kt` (gesture-drag, physics-based, punya
resiko regresi sendiri per histori panjang project ini) dan beberapa sheet sekunder yang belum
pakai `.animateItem()` (`SmartPlaylistScreen`, `VaultSheet`, `BackupRestoreSheet`,
`SongPickerSheet`, `LyricsSheet`, `ABRepeatBookmarkSheet`, `EqualizerSheet`, `DuplicateFinderSheet`,
`RingtoneCutterSheet`).

**0 diverifikasi CI/device Batch 481** — 0 env Android nyata/device fisik/compiler Kotlin di sesi
ini (balance brace/paren/bracket `MainActivity.kt`: `{}` 339/339, `()` 1260/1260, `[]` 3/3;
`Motion.kt` file baru: `{}` 1/1, `()` 15/15). **WAJIB DITEST user**:
1. Buka app → pindah tab Beranda↔Perpustakaan↔Pengaturan → fade transisi harus tetap terasa mulus
   (visual: kurva lebih "landai" di ujung awal/akhir dibanding sebelumnya, bukan langsung tancap
   gas) — 0 regresi durasi/urutan/flicker.
2. Pengaturan → buka Statistik (stats_dashboard) → slide dari kanan, lalu tombol back → slide balik
   ke kanan — harus tetap mulus, 0 patah/lompat di tengah animasi.
3. Tap lagu apapun → Now Playing naik dari bawah (slide+fade) → tombol back / swipe-down → turun
   lagi — harus tetap mulus, 0 regresi ke gesture drag-dismiss (Batch 476/477/480, TIDAK disentuh
   batch ini).
4. Pastikan 0 crash/force-close saat build (import `Motion` baru di `MainActivity.kt`).
**[RESUME POINT Batch 482]**: kalau test di atas ✅, lanjut micro-task berikutnya sesuai urutan
risiko naik: (a) audit SEKALIGUS 3 Animatable tersinkron pill/tab (`navPillIndexAnim`/
`glassAlphaAnim`/`tabBarDragFocus`) lalu upgrade easing bertiga BERSAMAAN (bukan 1-1) kalau aman;
(b) `.animateItem()` utk sheet sekunder yang masih polos (daftar di atas); (c) baru pertimbangkan
gesture-drag custom (`MiniPlayerBar`/`QueueSheet`) — PALING TERAKHIR krn riwayat regresi
terpanjang di sektor ini. Jangan mulai dari (c).

**Catatan Batch 480 [instruksi eksplisit user: "sempurnakan mekanisme drag mini player (feedback,
konfirmasi user, dll)"]**: sektor mini player disentuh SESUAI instruksi eksplisit user batch ini
(bukan tebakan) — item WAJIB DITEST Batch 477 utk sektor ini ("swipe threshold → musik BERHENTI
TOTAL") belum ada laporan device balik dari user, tapi TIDAK diasumsikan gagal (tidak ada laporan
gagal juga) — dianggap baseline aktif-stabil sesuai kaidah "0 laporan gagal = lanjutkan" yang
sudah dipakai project ini utk sektor lain (mis. bottom nav Batch 448 di bawah), fix Batch 477 itu
sendiri 0 disentuh.

**2 file diubah** (dalam batas 3 file/tugas): `MiniPlayerBar.kt`, `PlayerViewModel.kt`.
1. **Feedback visual drag** (`MiniPlayerBar.kt`): `graphicsLayer` yang sudah membaca
   `dismissOffsetPx` utk `translationX` (sejak Batch 476) diperluas baca nilai yang SAMA utk
   `alpha`/`scaleX`/`scaleY` (progress 0→1 di 0→120px, dikapkan di 1 persis di titik threshold —
   drag lebih jauh dari situ TIDAK di-clamp posisinya per desain Batch 476, tapi visual feedback
   berhenti menambah di titik itu) — bar meredup+mengecil halus mengikuti jari, sinyal "akan
   hilang" SELAMA drag, bukan cuma snap di akhir. Dibaca LANGSUNG di lambda `graphicsLayer` (pola
   SAMA PERSIS `translationX` yang sudah ada + rasional Batch 397), jadi 0 recomposition
   tambahan tiap frame drag, cuma invalidasi layer.
2. **Haptic 2-tahap** (`MiniPlayerBar.kt`): tick `TextHandleMove` (beda dari `LongPress` yang
   sudah ada) ditambah SEKALI persis saat `dismissOffsetPx` melewati threshold 120px SELAGI masih
   digeser (flag `thresholdHapticFired` per-gesture) — real-time sinyal "lepas sekarang = batal
   terjadi", terpisah dari `LongPress` yang sudah ada di `!change.pressed` (itu tetap konfirmasi
   dismiss BENERAN terjadi saat jari diangkat, 0 disentuh). 0 threshold/formula/spring gesture yg
   sudah ada (120px, damping, dsb, Batch 476/477) disentuh sama sekali.
3. **Konfirmasi user via Undo** (`PlayerViewModel.kt`): `dismissMiniPlayer()` sebelumnya
   destruktif permanen (stop total + queue+state dikosongkan, 0 jalan balik — lihat komentar
   Batch 476 di fungsi itu). Sekarang snapshot (queue, index, posisi, repeat, shuffle, speed,
   isPlaying) diambil DULU dari controller/uiState di main thread SEBELUM 9 baris clear asli
   (0 diubah sama sekali), lalu dipasang lewat `UndoableAction` — infra yg SUDAH ADA & dipakai
   `removeFromQueue()`/`removeSongFromPlaylist()`/`removeAutoPlaylist()` (0 API baru, 0 pola
   baru), otomatis muncul sbg Snackbar "Urungkan" lewat `LaunchedEffect(undoableAction)` yang
   SUDAH ADA di `MainActivity.kt` — **0 baris `MainActivity.kt` diubah sama sekali** (di luar
   hitungan 2 file di atas, bukan salah satu dari 3 file/tugas). Fungsi baru
   `restoreDismissedPlayback()` (private) pulihkan lewat `playQueue()` yang sudah ada, pola sama
   persis `resumeFromSaved()` (shuffle/repeat diset SEBELUM `playQueue()`, alasan sama).

**0 diverifikasi CI/device Batch 480** — 0 env Android nyata/device fisik/compiler Kotlin di sesi
ini (balance brace/paren/bracket kedua file: `MiniPlayerBar.kt` `{}` 32/32 `()` 180/180, tanpa
`[]`; `PlayerViewModel.kt` `{}` 240/240 `()` 971/971 `[]` 37/37). **WAJIB DITEST user**:
1. Swipe mini player kiri/kanan PELAN (belum lepas jari) → bar meredup+mengecil halus mengikuti
   jari; tick getar HALUS terasa SEKALI persis saat lewat titik ~120px (sebelum lepas jari).
2. Lepas jari SETELAH tick itu → bar hilang seperti biasa (getar `LongPress` seperti sebelumnya)
   DAN Snackbar "\"<judul lagu>\" dihentikan" + tombol "Urungkan" muncul di bawah.
3. Tap "Urungkan" pada Snackbar itu → lagu yang sama main lagi persis dari posisi/repeat/
   shuffle/kecepatan sebelum di-dismiss (bukan dari awal lagu / posisi 0).
4. Swipe PELAN tidak sampai threshold, lepas jari → bar pegas balik ke posisi semula seperti
   biasa (regresi Batch 476/477), 0 tick/Snackbar muncul (tick HANYA saat threshold terlampaui).
5. Tap biasa (0 geser) di mini player MASIH buka Now Playing seperti biasa — 0 regresi.
Kirim hasil test ini balik sebelum sektor mini player disentuh lagi.

**Catatan Batch 479 [user kirim log Diagnostik sesuai WAJIB RESUME POINT Batch 478]**: root cause
bug (2) Batch 477 ("drag lintas tab, bottom nav diam") KETEMU dari data log (bukan tebakan) — 4/4
baris `TabSwipe` WAJIB (content Box composed, pointerInput coroutine mulai, onDragStart,
onHorizontalDrag PERTAMA) ADA, `onDragEnd` konsisten hitung `targetRoute` benar tiap gesture (4x
drag berturut Pengaturan→Perpustakaan→Beranda→Perpustakaan→Pengaturan, semua sukses) —
MEMBUKTIKAN gesture+navigate() jalan 100% normal, root cause BUKAN di situ. Sebenarnya:
`navPillIndexAnim` (Animatable posisi "rest" pill bottom nav, Batch 448) sebelumnya HANYA
disinkronkan dari 2 jalur — onClick 3 `NavigationBarItem` & selesai-drag-LANGSUNG-di-tab-bar
(Batch 444) — swipe KONTEN (Batch 435/477, blok terpisah di luar `bottomBar=`) manggil
`navController.navigate()` LANGSUNG tanpa pernah menyentuh `navPillIndexAnim`, jadi
currentRoute+konten berpindah benar tapi pill visual TIDAK PERNAH ikut.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt`.
1. Instrumentasi Batch 478 (6 `AppLogger.w` + import) DICABUT TOTAL sesuai mandat WAJIB DICABUT
   begitu root cause ketemu — 0 log/instrumentasi tersisa.
2. `navPillIndexAnim` DIPINDAH (hoisted) dari scope lokal `bottomBar=` ke scope `AppNavHost`
   (sejajar `tabSwipeScope`/`tabDragOffsetPx`/`tabDragOffset`) supaya lambda `content=` (Scaffold)
   bisa ikut baca/tulis — pola hoist SAMA PERSIS `currentRouteState`/Batch 442 & `tabSwipeScope`
   yang sudah ada, 0 pola baru.
3. `onDragEnd` blok swipe KONTEN (setelah `navController.navigate(targetRoute)` sukses) ditambah 1
   panggilan `navPillIndexAnim.animateTo(index+0.5f, tween(220))` — pola tween(220) IDENTIK 3
   onClick `NavigationBarItem`, 0 formula/angka baru. 0 `LaunchedEffect(currentRoute)` generik
   ditambah (tetap dihindari sesuai rasionalisasi asli Batch 448 — cegah race start-animasi ganda
   antar 3 jalur sync).
4. 0 formula/threshold 120px/damping 0.3f/clamp ±40px/spring MediumBouncy-Low Batch 435/437/477
   disentuh. 0 logic tab-bar-drag (Batch 442/444/448) disentuh.

**0 diverifikasi CI/device Batch 479** — 0 env Android nyata/device fisik/compiler Kotlin di sesi
ini (balance brace/paren/bracket file penuh: `{}` 339/339, `()` 1251/1251, `[]` 3/3). **WAJIB
DITEST user**: swipe horizontal di KONTEN (bukan di atas tab-bar) di Beranda/Perpustakaan/
Pengaturan → pill bottom nav HARUS ikut berpindah ke tab tujuan setelah gesture selesai (bukan
cuma konten yang berganti) — baik utk 1 tab loncat maupun drag kontinu lintas >1 tab. Kirim hasil
balik sebelum sektor tab-swipe/bottom-nav disentuh lagi.

**Catatan Batch 478 [user konfirmasi Batch 477 fix (2) BELUM menyelesaikan masalah: "drag lintas
tab, bottom nav masih diam" — dikonfirmasi ULANG bahkan di tab Pengaturan (nyaris 0 elemen
horizontal-scrollable di sana, jadi teori LazyRow-menelan-drag Batch 477 TERSINGKIR sebagai
penjelasan tunggal)]**: 0 fix baru dikerjakan batch ini — pola PERSIS Batch 463 (0 tebak fix lagi
tanpa data, instrumentasi dulu). **1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt`.
6 titik `AppLogger.w("TabSwipe", ...)` ditambah (import `AppLogger` baru): (1) `LaunchedEffect
(currentRoute)` tepat sebelum Box konten — LEPAS dari `isOnTabRoute`, jadi ABSENnya baris ini
sendiri di Log Diagnostik sudah sinyal (composable ini sendiri 0 ke-invoke); (2) baris pertama
badan `pointerInput(Unit)` — konfirmasi coroutine gesture BENAR mulai + `isOnTabRoute`/`boxSize`
saat itu; (3) `onDragStart`; (4) `onHorizontalDrag` (SEKALI per gesture via flag `firstDragLogged`
— bukan tiap event, supaya 0 membanjiri log); (5) `onDragEnd` (totalTabDrag + targetRoute
terhitung); (6) `onDragCancel`. **0 formula/logic gesture diubah sama sekali** — WAJIB DICABUT
lagi begitu root cause ketemu, bukan instrumentasi permanen.
**WAJIB DARI USER SEBELUM CODING FIX APA PUN LAGI** (lihat `[RESUME POINT]`): reproduksi PERSIS
di tab Pengaturan (drag horizontal apa saja di situ), lalu Settings → Lanjutan → Log Diagnostik →
cari baris `TabSwipe` → kirim balik PERSIS baris mana yang muncul (atau konfirmasi 0 ada sama
sekali). **JANGAN tebak fix lagi tanpa log ini** (pola terlarang eksplisit, sama seperti "PELAJARAN
PROSES Batch 461→462→463").


mini player Batch 476 0 berefek sama sekali; (2) drag lintas-tab konten 0 gerakkan bottom nav]**:
2 bug DI LUAR sektor mana pun yang tertutup, ditemukan lewat REVIEW KODE (bukan tebakan) — root
cause KEDUANYA sudah pernah didokumentasikan sebagai pola bahaya di file yang sama, tapi belum
diterapkan ke titik yang kena batch ini.

**2 file diubah** (dalam batas 3 file/tugas): `MiniPlayerBar.kt`, `MainActivity.kt`.
- **(1) Mini player swipe-cancel 0 berefek**: root cause SAMA PERSIS Batch 350
  (NowPlayingScreen.kt) — `pointerInput{detectHorizontalDragGestures}` (Batch 476) & `.clickable
  (onExpand)` 2 gesture recognizer terpisah bersaing 1 titik sentuh tanpa wasit, SAMA-SAMA pass
  Main (default) → `.clickable()` (lebih dalam di chain) SELALU proses change LEBIH DULU sebelum
  drag detector (lebih luar) sempat consume() gilirannya — clickable TIDAK PERNAH lihat change
  ter-consume, tap menang mutlak, swipe 0 efek visual sama sekali. Fix: `detectHorizontalDrag-
  Gestures` diganti loop manual (`awaitEachGesture`/`awaitFirstDown`/`awaitPointerEvent`) didaftar
  di `PointerEventPass.Initial` (jalan sebelum Main manapun) — pola IDENTIK Batch 350, 0 teori
  baru. Sebelum touchSlop terlampaui 0 consume (tap+ripple `.clickable()` tetap utuh, 0 regresi
  item (c) resume point Batch 476); begitu terlampaui baru consume di sini, `.clickable()` auto-
  cancel sendiri (mekanisme baku Compose). Formula threshold 120px/spring MediumBouncy-Low/haptic
  Batch 476 0 disentuh.
- **(2) Drag lintas-tab 0 gerakkan bottom nav**: root cause — `pointerInput(currentRoute)` (Batch
  435, drag DI KONTEN layar, BUKAN drag-di-atas-bar-tab Batch 442 yang terpisah/sudah benar) di-key
  string yang BERUBAH begitu `navigate()` jalan di `onDragEnd` → Compose cancel+restart coroutine
  gesture di komposisi berikutnya. Utk drag 1 gesture yang cuma lompat 1 tab: 0 kelihatan (restart
  terjadi SETELAH gesture kelar). Utk **drag kontinu yang lintas >1 batas tab tanpa angkat jari**:
  begitu batas tab pertama terlewati, instance lama mati, instance baru cuma `awaitFirstDown()` —
  down utk jari yang SUDAH menekan sejak awal TIDAK PERNAH datang lagi, SISA drag itu 0 diproses
  (tabDragOffsetPx/tabMagnifyFocus/nudge konten beku total, pill/label bottom nav 0 gerak lagi) —
  PERSIS bug class yang sudah didokumentasikan sbg alasan `currentRouteState`/key-`Unit` di
  `pointerInput` drag-di-atas-bar Batch 442 (komentar di dekat deklarasi `homeTabInteraction`),
  tapi belum diterapkan ke titik Batch 435 ini. Fix: key diganti `Unit` + instance BARU
  `rememberUpdatedState(currentRoute)` scope-lokal (`currentRouteState` Batch 442 TIDAK bisa dipakai
  ulang — scope-nya di lambda `bottomBar=` Scaffold, di luar jangkauan lambda `content=` ini). 0
  formula/threshold 120px/damping 0.3f/clamp ±40px Batch 435/437 disentuh.
- **CATATAN BELUM DIVERIFIKASI (teori sekunder, item (2))**: video bukti user menunjukkan titik
  sentuh kemungkinan di ATAS row horizontal (`Mix`/`Favorit` Beranda, atau chip filter Library) —
  row itu LazyRow yang scrollable sendiri, bisa "menelan" drag horizontal duluan (child menang
  Main-pass) SEBELUM sampai ke pointerInput ancestor manapun, independen dari fix keying di atas.
  Fix batch ini TIDAK menyentuh soal ini (butuh konfirmasi user dulu titik sentuh persis — pola
  "JANGAN tebak tanpa data" konsisten sepanjang project). Lihat `[RESUME POINT]`.
- **0 diverifikasi CI/device Batch 477** — review manual (balance brace/paren/bracket: lihat
  `CHANGELOG.md` § Batch 477 untuk angka persis tiap file), 0 env Android nyata/device fisik/
  compiler Kotlin di sesi ini. **WAJIB DITEST user** (lihat `[RESUME POINT]` di bawah).

**Catatan Batch 476 [instruksi baru user: mini player bisa dicancel + sinkronisasi eksternal/
cold-start ke Mini Player Bar]**: 2 permintaan baru user, DI LUAR sektor bubble Batch 475 (target
sekarang `MiniPlayerBar.kt`/`PlayerViewModel.kt` — UI dalam-app, bukan floating bubble luar-app).

**3 file diubah** (dalam batas 3 file/tugas): `PlayerViewModel.kt`, `MiniPlayerBar.kt`,
`MainActivity.kt`. Detail lengkap: `CHANGELOG.md` § Batch 476.
- **(1) Mini player bisa dicancel**: swipe horizontal (kiri/kanan, threshold 120px — konstanta &
  pola gesture SAMA PERSIS dgn swipe next/prev album art `NowPlayingScreen.kt`: Animatable
  snapback, spring dampingRatio MediumBouncy/stiffness Low, 0 API baru diperkenalkan) pada
  `MiniPlayerBar` memanggil `onDismiss` → `PlayerViewModel.dismissMiniPlayer()`: `controller.stop()`
  + `clearMediaItems()`, `_uiState`/`_playbackProgress` direset ke default, DAN
  `playbackStateStore.save(songIds = emptyList(), ...)` (fungsi `save()` yang SUDAH ADA, bukan
  method baru) — `load()` balik null utk `songIds` kosong (string kosong → 0 id tervalidasi),
  PERSIS kondisi "0 saved state" yang sudah ditangani `resumeFromSaved()`/`peekSavedSong()`. Tanpa
  langkah ini, sync (2) di bawah akan membangkitkan lagi lagu yang baru saja di-cancel user pada
  proses berikutnya.
- **(2) Sinkron eksternal/cold-start ke Mini Player Bar**: root cause — `_uiState.currentSong`
  SEBELUMNYA cuma pernah diisi lewat `onMediaItemTransition` (event, hanya nyala saat item
  BERGANTI) atau aksi eksplisit (`playQueue` dkk), TIDAK PERNAH dari status controller yang SUDAH
  berjalan tepat di titik `connect()` — begitu Activity/proses baru muncul sementara
  PlaybackService (session) sudah/masih punya media item aktif, mini bar tetap kosong ("reset")
  sampai ada transisi lagu berikutnya. Fix: `maybeSyncMiniPlayerOnColdStart()` (dipanggil dari 2
  titik — `connect()` & tail sukses `refreshLibrary()` — menutup race urutan mana pun yang selesai
  duluan; dikunci `coldStartSyncDone` supaya PERSIS 1x per proses). 2 cabang: (a)
  `controller.mediaItemCount > 0` (Service tetap hidup — app-kill/backgrounding biasa, ATAU
  playback dimulai dari luar UI app: widget/headset/bubble/lock screen) → state disinkronkan
  LANGSUNG dari controller (mediaId → Song lewat `_librarySongs`), 0 `play()`/`pause()`/`seekTo()`
  dipanggil sama sekali — 0 interupsi ke audio yang sedang berjalan; (b) `mediaItemCount == 0`
  (Service ikut mati total) → reuse `resumeFromSaved(autoPlay = false)` yang SUDAH dipakai jalur
  shortcut "Continue Listening"/tombol Resume HomeScreen — mini player muncul (paused) TANPA
  auto-play mengejutkan.
- **0 diverifikasi CI/device Batch 476** — review manual (balance brace/paren/bracket: lihat
  `CHANGELOG.md` § Batch 476 untuk angka persis tiap file), 0 env Android nyata/device fisik/
  compiler Kotlin di sesi ini. **WAJIB DITEST user** (lihat `[RESUME POINT]` di bawah untuk daftar
  lengkap) sebelum sektor ini dianggap tuntas.

**Catatan Batch 475 [reopen eksplisit user: sinkronisasi cold-start/persistent bubble <-> player
setelah app-kill]**: user eksplisit buka ulang sektor "survive app-kill" (DITUTUP informal Batch
474) dengan instruksi spesifik: "lakukan sinkronisasi mekanisme cold-start/persistent antara
fitur bubble dan eksternal SONIX player after kill the app". Diizinkan per "Aturan sesi aktif" #6
(instruksi eksplisit spesifik minta sektor dibuka lagi, BUKAN instruksi generik "next"/"lanjut").

**2 file diubah** (dalam batas 3 file/tugas): `PlaybackService.kt`, `FloatingBubbleService.kt`.
Detail lengkap: `CHANGELOG.md` § Batch 475. Ringkas: grep seluruh project menemukan 5 titik
`startForegroundService()` yang menyalakan bubble/PlaybackService — cuma 2 (`BubbleBootReceiver.kt`
Batch 466, `FloatingBubbleService.onTaskRemoved` Batch 471) yang dibungkus `runCatching` setelah
device nyata user MEMBUKTIKAN `ForegroundServiceStartNotAllowedException` bisa terjadi walau API
resmi sudah benar. 2 titik LAIN — justru paling relevan ke sinkronisasi bubble<->player pasca
app-kill (`PlaybackService.maybeStartFloatingBubble()`, dipanggil `onIsPlayingChanged(true)` =
funnel bubble menyala balik saat playback resumption eksternal berhasil pasca app-kill total; &
`FloatingBubbleService.sendPlaybackAction()` fallback, arah kebalikan — bubble memicu cold-start
PlaybackService saat tap tombol dalam keadaan cold) — TIDAK ikut terlindungi. Fix: kedua titik
disamakan ke pola `runCatching` yang sama, 0 logic/formula/state lain diubah. Titik ke-5
(`BubbleTileService.kt`) SENGAJA tidak disentuh — tap QS tile user-initiated, exempt dari
background-start restriction, beda kelas risiko.

**0 diverifikasi CI/device Batch 475** — review manual (cek balance brace/paren/bracket:
`PlaybackService.kt` `{}` 80/80 `()` 427/427 `[]` 19/19; `FloatingBubbleService.kt` `{}` 108/108
`()` 730/730 `[]` 204/204), 0 env Android nyata/device fisik/compiler Kotlin di sesi ini. Kondisi
ini SULIT direproduksi sengaja (butuh device dengan restriksi OEM aktif tepat di momen resumption
eksternal) — validasi utama dari user: pastikan 0 regresi ke perilaku normal (bubble tetap nyala
saat playback dipicu widget/headset/Bluetooth/lock-screen seperti Batch 473, tap tombol bubble
cold-start tetap memicu restore seperti Batch 470).

**Catatan Batch 474 [jawaban user: tutup (a) survive app-kill + fitur baru (b) drag vs tap
tombol]**: user jawab 2 poin WAJIB Batch 471/472 sekaligus.

(a) **Survive app-kill — DITUTUP atas permintaan eksplisit user**: "ya. lupakan, sudah bisa
dipicu pakai pemutar SONIX eksternal/malas nunggu bermenit-menit jadi shut up". Ini konfirmasi
INFORMAL (bukan protokol lengkap swipe-Recents-lalu-tunggu-beberapa-menit yang diminta Batch 471)
— user eksplisit menolak lanjut test formal itu (verifikasi lewat trigger-play-dari-sumber-luar
dianggap cukup olehnya) dan **eksplisit minta topik ini TIDAK ditanyakan lagi**. Sesuai instruksi
eksplisit user: item ini DITUTUP di sini, **JANGAN munculkan lagi permintaan test survive-Recents
formal kecuali user sendiri yang buka ulang topiknya**. 0 kode diubah untuk poin ini (murni
perubahan status dokumentasi — tidak ada fix baru yang diminta/diperlukan).

(b) **Drag vs tap tombol kontrol — FITUR BARU eksplisit user**: "bisa gak drag nya diutamakan
dibanding akses tap nya?!!" — user TIDAK menjawab pertanyaan sempit Batch 472 (konfirmasi sisi
kanan lega sampai tombol minimize) secara langsung, malah lanjut ke permintaan lebih luas: drag
yang dimulai PERSIS di atas salah satu dari 4 tombol kontrol (play/pause/prev/next/minimize)
sebelumnya 0 pernah ikut memindah bubble sama sekali (bukan cuma "kalah prioritas" — dispatch
touch Android baku memegang SELURUH sequence 1 pointer ke SATU view yang menangkap `ACTION_DOWN`,
jadi drag yang dimulai di tombol 100% milik tombol itu, tidak pernah "diteruskan" ke mana pun).
**Pertanyaan sempit Batch 472 dianggap TIDAK TERJAWAB EKSPLISIT** (bukan ditolak, bukan
dikonfirmasi) — SOP larang asumsi tanpa data, jadi status "sisi kanan lega sampai minimize"
TETAP "belum dikonfirmasi user" (lihat RESUME POINT), meski implisit konsisten (kalau area dead-
space itu masih terasa sempit, permintaan (b) besar kemungkinan akan menyebut itu duluan).

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. **`setupDrag()` digeneralisasi**: param baru `onTap: () -> Unit` (default = perilaku LAMA
   persis `if (isMinimized) expand() else openApp()`) — 0 breaking change ke 2 pemanggil lama
   (album art, tab minimized), argumen mereka TIDAK perlu diubah sama sekali karena default
   identik. Logic drag (TOUCH_SLOP, clamp screenBounds, readback Batch 469, snap-tepi Batch 100)
   TIDAK disentuh SAMA SEKALI — cuma cabang tap di `ACTION_UP` yang sekarang manggil `onTap()`
   alih-alih hardcode.
2. **`setupControls()` dipasangi `setupDrag()` juga**: ke-4 `ImageButton` (play_pause/prev/next/
   minimize) sekarang JUGA jadi `touchSource` untuk `setupDrag()` (param `onTap` di-override
   manggil `performClick()` milik tombol itu sendiri) — `setOnClickListener` yang SUDAH ADA
   (dengan body `keepAwakeAndScheduleFade()` + `sendPlaybackAction`/`minimize()`) **TIDAK
   disentuh/dipindah sama sekali**, cuma sekarang dipicu lewat `performClick()` bukan lewat
   `View.onTouchEvent` bawaan — 1 satu-satunya sumber kebenaran aksi tombol tetap listener yang
   sama, 0 duplikasi logic. Signature `setupControls()` berubah (`view` saja → `view, windowView,
   params`) supaya bisa meneruskan `container`/`params` yang sama ke `setupDrag()` — 1 titik
   panggil (`addBubbleView()`) ikut diupdate.
3. **Efek samping jujur, disengaja**: bunyi klik sistem Android bawaan (dipicu `View.
   onTouchEvent` default, bukan `performClick()`) tidak lagi terdengar saat tap ke-4 tombol itu —
   konsisten filosofi iOS-look proyek ini (ripple Android sudah dimatikan di bottom nav, Batch
   439), BUKAN regresi. 0 drawable/style tombol disentuh (background statis, 0 `state_pressed`
   yang hilang).
4. 0 formula clamp/posisi/`screenBounds` disentuh (di luar cakupan guard Batch 469), 0 sektor
   DITUTUP disentuh.

**0 diverifikasi CI/device Batch 474** — review manual (baca kode + cek balance brace/paren/
bracket: `{}` 106/106, `()` 725/725, `[]` 202/202 di `FloatingBubbleService.kt`), 0 env Android
nyata/device fisik/compiler Kotlin di sesi ini. **WAJIB dari user** (lihat RESUME POINT untuk
daftar lengkap): (1) drag dimulai dari salah satu 4 tombol kontrol sekarang memindah bubble; (2)
tap biasa (tanpa gerak) di ke-4 tombol itu masih berfungsi 100% normal (0 regresi fungsi
play/pause/prev/next/minimize); (3) drag dari album art & tab minimized (2 titik lama, TIDAK
disentuh batch ini) tetap seperti Batch 472.

**Catatan Batch 473 [0 file diubah — klarifikasi murni, tutup poin 3 Batch 472]**: user jawab tap
pilihan eksplisit untuk klarifikasi "bubble tetap muncul saat player eksternal dimainkan" (dibuka
Batch 472 poin 3) — **maksud (a) DIKONFIRMASI**: bubble SONIX tetap tampil walau lagu dipicu main
dari LUAR UI app (headset/Bluetooth/Android Auto/widget home-screen), BUKAN (b) bubble
menampilkan sesi app lain. Ini **PERILAKU YANG DIHARAPKAN, BUKAN bug** — `PlaybackService`
(`MediaLibraryService`) merespons trigger play dari sumber mana pun (tombol fisik headset,
Bluetooth AVRCP, Android Auto, widget home-screen) via jalur session/Intent yang SAMA, bubble
cuma mencerminkan state `PlaybackService` itu sendiri lepas dari APA yang memicunya — konsisten
dengan `SessionToken` yang HANYA konek ke `PlaybackService` app ini sendiri (lihat "Kontrol/state"
KDoc kelas `FloatingBubbleService.kt`). **0 kode diubah** — poin 3 Batch 472 RESMI DITUTUP, 0
kandidat bug lagi di sektor ini. Sektor bubble (Roadmap #11) masih terbuka untuk item lain
(investigasi Batch 469 & test survive-Recents Batch 471/472 di bawah, keduanya BELUM tersentuh
batch ini).

**Catatan Batch 472 [1 laporan user: drag masih "sempit" pasca-471 + 2 klarifikasi survive
app-kill]**: 0 tumpang tindih investigasi Batch 469 (screenBounds/clamp/posisi 0 disentuh). **1
file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. **Drag mentok penuh ke tepi kanan**: `rect.right` TouchDelegate `bubble_album_art` sekarang
   dinamis = `expanded.width` (tepi kanan `bubble_root` sesungguhnya, melewati SELURUH badan 4
   tombol kontrol) — gantikan `ALBUM_ART_TOUCH_PAD_RIGHT_DP` (Batch 470/471, dicabut, cuma sampai
   gap sebelum `bubble_prev`) yang terbukti belum cukup lega. Aman krn alasan yang SAMA dibuktikan
   Batch 471: tombol selalu menang bounds sendiri di dispatch Android baku, lepas dari overlap
   rect delegate. Kiri/atas/bawah TIDAK disentuh (sudah maksimal fisik sejak Batch 471).
2. **Dialog battery-optimization (pertanyaan Batch 471) DIKONFIRMASI user**: muncul PERSIS sesuai
   desain — hanya terpicu saat user toggle bubble OFF→ON manual, 0 nge-nag di luar alur itu. 0
   kode diubah — murni sinkronisasi status dokumentasi.
3. **Survive app-kill (swipe Recents, pertanyaan Batch 471) BELUM ditest user** — user melaporkan
   1 observasi DI LUAR protokol diminta: "bubble tetap muncul saat player eksternal dimainkan".
   Makna AMBIGU — arsitektur [MediaController] bubble HANYA konek ke `PlaybackService` app ini
   sendiri lewat `SessionToken(ComponentName(this, PlaybackService::class.java))`, 0 jalur kode
   yang bisa menampilkan sesi/metadata app lain. Kandidat makna: (a) bubble tetap tampil walau
   pemicu play datang dari luar UI app (Bluetooth/headset/Android Auto/widget) — WAJAR & sesuai
   desain kalau ini maksudnya; (b) bubble justru menampilkan kontrol/metadata App LAIN (bug nyata
   kalau ini maksudnya, tapi TIDAK didukung baca-kode manapun saat ini). **SOP larang tebak fix
   tanpa data** — 0 kode disentuh, WAJIB klarifikasi dulu dari user (lihat RESUME POINT) SEBELUM
   coding apa pun ke sektor ini, dan test swipe-dari-Recents (yang belum pernah dilakukan) tetap
   WAJIB dari user terlepas dari klarifikasi ini.

**0 diverifikasi CI/device Batch 472** — review manual (baca kode + cek balance brace/paren:
`{}` 102/102, `()` 683/683, `[]` 184/184 di `FloatingBubbleService.kt`), 0 env Android nyata/
device fisik/compiler Kotlin di sesi ini. Perlu dari user: (1) install APK baru, test drag dari
album art lagi — sisi kanan sekarang harus lega sampai ke tombol minimize; (2) jawab klarifikasi
poin 3 di atas; (3) LANJUTKAN test survive app-kill yang masih tertunda (swipe SONIX dari Recents,
BUKAN Force Stop manual, setelah dialog battery-optimization di-grant) — cek bubble/notifikasi
masih ada beberapa menit kemudian.

**Catatan Batch 471 [2 laporan user: drag "sulit/terbatas" + "bubble survive 100%?"]**: 0
tumpang tindih investigasi Batch 469 (screenBounds/clamp/posisi 0 disentuh). **3 file diubah**
(pas batas 3 file/tugas): `FloatingBubbleService.kt`, `AndroidManifest.xml`, `MainActivity.kt`.
1. **Drag diperlebar sisi kanan saja**: `ALBUM_ART_TOUCH_PAD_RIGHT_DP` 3f→6f (full gap asli ke
   `bubble_prev`, terbukti aman krn tombol selalu menang bounds sendiri lepas dari overlap rect
   delegate). Kiri/atas/bawah SUDAH maksimal secara fisik (padding riil `bubble_root` cuma 8dp,
   `ALBUM_ART_TOUCH_PAD_DP`=10f sudah melebihi itu) — perbaikan lanjutan (naikkan padding XML
   `bubble_mini_player.xml`) BELUM diterapkan, WAJIB tunggu konfirmasi user dulu apakah perbaikan
   sisi-kanan-saja ini sudah cukup atau masih perlu breathing room lebih besar.
2. **Bubble survive app-kill**: ditambah `onTaskRemoved` (re-assert foreground defensif, pola
   `runCatching` sama `BubbleBootReceiver`) + `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
   (diminta HANYA saat user aktif toggle ON, 0 nge-nag tiap buka app). **BATAS JUJUR eksplisit
   ke user**: "100%" TIDAK bisa dijanjikan kode manapun — bukti Batch 466 (device user KENA OEM
   App Standby/battery restriction walau API resmi sudah benar). Kalau OEM masih agresif setelah
   grant dialog battery-optimization ini, sisanya (autostart/protected-apps proprietary OEM) di
   luar jangkauan kode sama sekali, WAJIB user whitelist manual di pengaturan HP masing-masing.

**Catatan Batch 470 [2 fitur baru dari user, 0 tumpang tindih investigasi Batch 469]**: user
konfirmasi kliping landscape SEKARANG bekerja ("sudah bisa kliping dalam mode landscape
sekalipun") — konfirmasi UMUM, BUKAN reproduksi protokol spesifik Batch 469 (drag ke tepi
nav-bottom + JANGAN rotasi balik + ekspor Log Diagnostik) yang masih belum pernah dikirim.
Regresi "MENGHILANG TOTAL" Batch 469 karena itu **BELUM RESMI dianggap selesai** (juga belum
terbantahkan) — kalau device fisik user memicunya lagi, protokol reproduksi Batch 469 di bawah
TETAP berlaku, minta log itu duluan sebelum coding fix apa pun ke [screenBounds]/formula clamp.
User mengarahkan sesi ke 2 permintaan baru, **0 pernah menyentuh [screenBounds]/formula
clamp/posisi** (aman dari investigasi Batch 469 yang masih terbuka):
1. **Cold-start bubble**: bug ditemukan lewat review kode (bukan laporan user eksplisit) —
   `FloatingBubbleService.sendPlaybackAction()` pakai panggilan `MediaController` langsung begitu
   `controller != null`, TANPA cek `mediaItemCount`. Di boot murni (bubble auto-start lewat
   `BubbleBootReceiver`, user belum pernah buka app/widget), controller lokal bisa konek DULUAN
   (bind doang) SEBELUM antrean sempat di-restore → `hasQueue` nge-latch `false` selamanya →
   SETIAP tap play/prev/next di bubble jatuh ke `openApp()`, padahal `PlaybackService` sudah
   punya jalur cold-start-restore (dipicu Intent `onStartCommand`, BUKAN panggilan controller
   langsung). Fix: syarat `c.mediaItemCount > 0` sebelum pakai controller langsung; kalau tidak,
   fallback ke Intent yang sama seperti widget (yang memang tidak pernah kena celah ini).
2. **Drag & tap-buka-app discoped ke `bubble_album_art` saja**: sebelumnya seluruh badan pill
   (termasuk padding kosong `bubble_root`, bukan cuma area ke-4 tombol yang memang sudah aman)
   jadi pemicu drag/buka-app. Sekarang `setupDrag` dipanggil 2x terpisah (minimized tab: 0
   berubah; expanded: scoped ke `bubble_album_art`), diperluas via `TouchDelegate` (hit-test
   SAJA, 0 perubahan layout/dp XML) biar tetap gampang disentuh. Jangkauan GERAK drag (setelah
   tersentuh) tetap bebas penuh ke seluruh layar seperti sebelumnya — cuma titik awal sentuh sah
   yang berubah.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`. Detail lengkap KDoc
"Batch 470" di file itu. **0 diverifikasi CI/device** — review manual (baca kode + cek balance
`{}`/`()`/`[]`: 99/99, 640/640, 160/160), 0 env Android nyata/compiler Kotlin di sesi ini —
**WAJIB dari user**: install APK baru, test (a) tap play/prev/next di bubble SEGERA setelah
reboot HP tanpa buka app/widget dulu (cold-start), (b) coba drag mulai dari padding kosong pill
(bukan album art/tombol) → pastikan TIDAK lagi memicu drag/buka-app, (c) drag mulai dari album
art (termasuk sedikit di luar 40dp-nya) → pastikan MASIH memicu drag seperti biasa, jangkauan
gerak tetap bebas ke seluruh layar.

**Catatan Batch 469 [REGRESI BARU pasca-468 — instrumentasi, BUKAN fix lagi]**: user laporkan
temuan baru — bubble MENGHILANG TOTAL saat di-drag ke tepi landscape yang berbeda (sisi
nav-bar-bottom), balik normal HANYA kalau device dirotasi ke portrait lagi. Gejala BARU, LEBIH
PARAH dari sebelum Batch 468 (dulu cuma "kurang ter-klip" — kosmetik; sekarang bisa hilang
total/unreachable — fungsional). Detail root-cause reasoning lengkap: KDoc "Batch 469" di
`FloatingBubbleService.kt`. Ringkas dugaan (BELUM DIPASTIKAN, masih teori): `screenBounds` diisi
dari 2 API BERBEDA tergantung kapan dipanggil — `onCreate` pakai `currentWindowMetrics.bounds`,
`onConfigurationChanged` pakai `newConfig.screenWidthDp/HeightDp * density` (keputusan sah Batch
461 demi alasan freshness/timing) — 2 API ini TIDAK dijamin identik secara semantik. Sebelum
Batch 468 (posisi content-area-relative) potensi selisih ini "aman"; sejak Batch 468 (posisi
full-screen-absolute) selisih itu bisa mendorong posisi clamp keluar dari layar nyata.

**KEPUTUSAN**: TIDAK menebak fix ke-5 (SOP eksplisit larang pola ganti-ganti API metrics tanpa
data, "PELAJARAN PROSES Batch 461→462" — apalagi ini regresi ke-2 di file sama minggu ini).
**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` — **0 formula/clamp/
posisi diubah SAMA SEKALI**, murni 2 titik log baru:
1. `onConfigurationChanged`: log `currentWindowMetrics.bounds` (read-only, pembanding) di
   sebelah `screenBounds` yang BENAR-BENAR dipakai (dari `newConfig`) — kalau 2 angka beda, bukti
   langsung teori di atas.
2. `setupDrag` `ACTION_UP` (drag manual — path yang SEBELUMNYA 0 instrumentasi sama sekali, beda
   dari `snapMinimizedToNearestEdge` yang sudah ter-instrumentasi sejak Batch 463): log target
   akhir drag + `screenBounds` yang dipakai clamp + readback `getLocationOnScreen()` +250ms.

**0 diverifikasi CI/device Batch 469** — review manual (baca kode + cek balance brace/paren:
`{}` 96/96, `()` 571/571, `[]` 111/111 di `FloatingBubbleService.kt`), 0 env Android nyata/device
fisik/compiler Kotlin di sesi ini. **Perlu dari user (protokol reproduksi SPESIFIK)**: ulangi
skenario PERSIS (landscape, drag bubble minimized ke tepi nav-bar-bottom sampai hilang) →
**JANGAN rotasi balik ke portrait dulu** (itu "memperbaiki" gejala TAPI JUGA menghapus jendela
diagnostik yang relevan) → langsung ekspor Log Diagnostik dalam keadaan itu → kirim ke sini.

**Catatan Batch 468 [FIX bubble snap: `FLAG_LAYOUT_IN_SCREEN` — root cause branch (c) jadi kode]**:
user kirim hasil device-test Method A/B (protokol Batch 467, 3 screenshot). (A) portrait: GAP
KOSONG kelihatan antara status bar dan widget/bubble — bubble tidak pernah nutupin status bar.
(B) landscape (app lain fullscreen): bubble minimized tetap mentok PERSIS ke ujung layar TANPA
ter-klip sama sekali — gejala IDENTIK kegagalan landscape-edge-clip Batch 460/461/462 (sebelumnya
dianggap bug terpisah, gagal terdiagnosis 3x). Detail root-cause reasoning lengkap: KDoc "Batch
468" di `FloatingBubbleService.kt`. Ringkas: `addBubbleView()` pasang `FLAG_LAYOUT_NO_LIMITS`
TANPA `FLAG_LAYOUT_IN_SCREEN` → posisi x/y diterapkan WindowManager relatif ke content-area
(exclude status/nav bar), sedangkan target dihitung (`screenBounds`) DAN readback
(`getLocationOnScreen()`) sama-sama full-screen absolut — origin mismatch itu sumber SEMUA delta
readback Batch 463-466, kemungkinan besar JUGA sumber gejala landscape-tak-terklip (inset sisi
landscape mengkompensasi `hiddenWidth` yang seharusnya menyembunyikan bubble). **Hipotesis
PENYATUAN 2 gejala berbeda** — kuat didukung data, BELUM kepastian mutlak sampai device confirm.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. Tambah `WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN` ke flags `addBubbleView()`
   (1 baris, sebelah `FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_NO_LIMITS` existing sejak Batch 100).
2. 0 formula lain diubah — `EDGE_CLIP_FRACTION`/`touchPad`/`visualWidth`/`screenBounds`/
   `ROTATION_RESNAP_DELAYS_MS` TETAP persis Batch 460-464 (1 variabel per percobaan, isolasi
   efek). Readback instrumentasi Batch 463/464 TIDAK dihapus — dipakai validasi (target: delta
   jadi (0,0) di semua kasus pasca-update).
3. 0 sektor DITUTUP disentuh.

**RISIKO REGRESI — WAJIB dibaca sebelum lapor hasil**: flag ini ubah origin koordinat SELURUH
window overlay (bukan cuma snap-edge) — termasuk posisi TERSIMPAN dari sesi sebelum update
(dihitung di sistem koordinat lama/salah). **Geser sekali di buka pertama pasca-update = EXPECTED,
BUKAN bug baru** — drag dikit buat re-snap ke koordinat benar. WAJIB regression-test manual PENUH:
drag manual, mini trigger, minimize/expand/fade/auto-minimize (Batch 98-100/451/453-458), DAN
KHUSUSNYA mentok-tepi-landscape (poin yang gagal 3x, paling penting divalidasi).

**0 diverifikasi CI/device Batch 468** — review manual (baca kode + cek balance brace/paren:
`{}` 90/90, `()` 535/535, `[]` 93/93 di `FloatingBubbleService.kt`), 0 env Android nyata/device
fisik/compiler Kotlin di sesi ini. Perlu dari user: install APK baru, ULANGI Method A/B persis +
tes khusus landscape-edge-clip, kirim hasil/screenshot/log lagi.

**Catatan Batch 467 [Klarifikasi crash Batch 466 + keputusan user: device-test dulu, bukan
coding langsung]**: user konfirmasi crash Batch 466 tidak pernah keliatan sebagai force-close
visible ke mereka, dan tidak ingat pemicu spesifik saat itu — WAJAR: `BOOT_COMPLETED` receiver
crash terjadi di background pas boot, proses aplikasi baru spawn lalu mati sebelum ada UI yang
sempat tampil, dan tidak semua OEM/versi Android munculin dialog "App keeps stopping" utk crash
background-only. **Fix Batch 466 TETAP VALID, TIDAK ditarik**: bukti FATAL stack trace di log
(thread='main', `ForegroundServiceStartNotAllowedException`, cocok 100% ke baris
`startForegroundService()` di `BubbleBootReceiver.kt`) berdiri sendiri lepas dari ingatan
subjektif user — pola sama persis pelajaran "device nyata menang atas asumsi" dari Batch 463.
0 kode diubah batch ini (dokumentasi-only).

Untuk hipotesis bubble `FLAG_LAYOUT_IN_SCREEN` (Batch 466): user pilih **device-test tambahan
dulu**, BUKAN langsung coding fix ke-4. Protokol test dikirim ke user (0 kode baru — pakai
instrumentasi existing Batch 463/464/466 yang masih ada di APK):
- **Method A (visual instan, 0 log)**: drag bubble ke tepi ATAS layar semaksimal mungkin →
  amati apakah bubble NUTUPIN status bar (jam/sinyal/baterai) atau berhenti di GAP kosong PAS
  DI BAWAH status bar. Gap kosong = konfirmasi kuat hipotesis (target dihitung exclude status
  bar, readback include).
- **Method B (log-based, banding kondisi)**: trigger minimize/rotate di 2 kondisi: (a) home
  screen biasa (status+nav bar full kelihatan), (b) di app lain fullscreen/immersive (video/game
  yang nyembunyiin status+nav bar) — lalu ekspor Log Diagnostik lagi, kirim balik. Kalau delta
  target-vs-readback BERUBAH/HILANG di kondisi (b) dibanding (a) → konfirmasi kuat hipotesis.

Sektor bubble (Roadmap #11) masih terbuka, 0 sektor DITUTUP disentuh.

**Catatan Batch 466 [FIX FATAL crash boot + data pertama investigasi bubble snap, branch C
terkonfirmasi]**: user kirim ekspor Log Diagnostik pertama (`log_20260915_133616...txt`, 1130
baris) — sekaligus KONFIRMASI IMPLISIT fix Batch 465 (file ADA, utuh, tidak korup/kepotong →
sheet Log Diagnostik terbukti tidak freeze/force-close lagi saat diekspor). 0 sektor DITUTUP
disentuh.

**Temuan 1 [FATAL, P0 STABILITY, prioritas di atas investigasi bubble]**: log berisi 1 crash
FATAL nyata (07:50:11) — `BubbleBootReceiver` → `context.startForegroundService()` saat
`BOOT_COMPLETED` dilempar `ForegroundServiceStartNotAllowedException` (`mAllowStartForeground
false`), app force-close total tiap boot (toggle bubble ON). `BOOT_COMPLETED` nominal exempt
dari restriksi Android 12+ ini per dok resmi, TAPI device nyata user MEMBUKTIKAN exemption itu
tidak selalu berlaku (kemungkinan OEM App Standby Bucket/battery restriction) — bukti device
menang atas asumsi dokumentasi, sama seperti pelajaran Batch 463.

**1 file diubah** (dalam batas 3 file/tugas): `BubbleBootReceiver.kt`.
1. `context.startForegroundService(serviceIntent)` dibungkus `runCatching` (pola identik
   `FloatingBubbleService.kt`) + `AppLogger.e` kalau gagal — 0 lagi propagate ke uncaught
   handler/force-close. Gagal-senyap jatuh ke fallback yang SUDAH ADA (`MainActivity`'s
   `LaunchedEffect(Unit)`, user buka app manual), bukan crash total.
2. 0 formula/logic lain diubah. Manifest dicek ulang (`RECEIVE_BOOT_COMPLETED`,
   `FOREGROUND_SERVICE_SPECIAL_USE`, `exported=false`) — semua sudah benar, 0 disentuh.

**Temuan 2 [investigasi Roadmap #11, BUKAN fix — decision tree Batch 464 resmi terjawab data]**:
330 baris `WARN [FloatingBubbleService]` di log sama = readback instrumentasi Batch 463/464
pertama dari device fisik NYATA (log Log Diagnostik penuh, bukan potongan logcat lagi).
108 pasang readback +250ms & 27 pasang +800ms dianalisis:
- **Ukuran (w/h) real vs target: 0/27 mismatch** → teori "window WRAP_CONTENT belum tuntas
  resize" (branch a, dugaan utama Batch 464) **GUGUR, dibuktikan data.**
- **+800ms MASIH offset dari target, besarnya SAMA PERSIS dgn +250ms** (0 mengecil seiring
  waktu) → teori "murni delay/settling" (branch b) **GUGUR juga.**
- Pola delta (ΔX,ΔY) TERKUANTISASI (bukan noise acak): (0,99)×46, (99,66)×21, (99,0)×22,
  (0,66)×9 — **branch (c) resmi terkonfirmasi**: root cause BUKAN resize, BUKAN timing.

**HIPOTESIS BARU (BELUM di-fix, WAJIB validasi lanjut — pola tebak-tanpa-data Batch 460-462
JANGAN diulang)**: `addBubbleView()` pasang `FLAG_LAYOUT_NO_LIMITS` TANPA
`FLAG_LAYOUT_IN_SCREEN` (baris ~504). Tanpa `FLAG_LAYOUT_IN_SCREEN`, per dok resmi
`WindowManager.LayoutParams` dgn `Gravity.TOP|START` diposisikan relatif ke content area
(exclude status bar/nav bar), sedangkan `getLocationOnScreen()` (dipakai readback) SELALU
absolut ke layar fisik (include status bar/nav bar) — origin 2 sistem koordinat beda, selisih
= inset sistem saat itu (status bar ≈99px di sebagian kasus, nav bar sisi landscape ≈66px di
kasus lain — cocok kenapa offset beda per orientasi, TIDAK pernah 0). Delta 99/66 MENGUATKAN
hipotesis ini tapi BELUM 100% dikonfirmasi (0 device fisik/compiler sesi ini). Fix kandidat
(BELUM diterapkan): tambah `WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN` ke flags baris
~504 (combo `NO_LIMITS+IN_SCREEN` = pola standar overlay bubble absolut, gaya Messenger
chat-head). **RISIKO kalau diterapkan**: flag ini bisa geser SEMUA posisi X/Y existing
(minimize/expand/drag/auto-minimize), bukan cuma snap-to-edge — WAJIB regression-test manual
penuh ke Batch 98-100/453-458/460-463. TIDAK auto-diterapkan batch ini (STABILITY WINS; 3 fix
tebakan sebelumnya sudah gagal — jangan tambah yang ke-4 tanpa keputusan eksplisit user).

**0 diverifikasi CI/device Batch 466** — review manual (baca kode + cek balance brace/paren:
`{}` 4/4, `()` 18/18, `[]` 2/2 di `BubbleBootReceiver.kt`), 0 env Android nyata/device
fisik/compiler Kotlin di sesi ini. Perlu dari user: (1) install APK baru, restart HP, konfirmasi
0 force-close lagi saat boot; (2) putuskan lanjut/tidak ke hipotesis `FLAG_LAYOUT_IN_SCREEN` di
atas sebelum batch depan coding fix bubble (lihat RESUME POINT).

**Catatan Batch 465 [FIX BLOCKER: Log Diagnostik force-close/freeze]**: user laporkan sheet
Settings > Log Diagnostik sendiri force-close/freeze saat dibuka — ini BLOCKER kritis karena
Log Diagnostik justru alat yang diminta Batch 464 untuk ambil log instrumentasi bubble. Root
cause: `DiagnosticLogSheet.kt` render `logText` (bisa sampai ~200_000 char, `MAX_LOG_BYTES` di
`AppLogger.kt`) sebagai 1 `Text` mentah dalam `Column`+`verticalScroll` — baca file sudah async
sejak Batch 431, tapi LAYOUT teks sepanjang itu di 1 node Compose tetap kerja Main thread saat
sheet pertama tampil; instrumentasi padat `FloatingBubbleService.kt` Batch 463/464 bikin file
log lebih cepat mendekati cap 200KB, cukup memicu ANR/freeze. Sektor bubble (Roadmap #11) TIDAK
disentuh batch ini — murni fix sheet Log Diagnostik. 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `DiagnosticLogSheet.kt`.
1. Render log diganti dari 1 `Text` mentah → `LazyColumn` + 1 `Text` per baris (pola sama
   `LyricsSheet.kt`/`DuplicateFinderSheet.kt`) — Compose cuma measure/layout baris yang
   KELIHATAN di layar, bukan seluruh log sekaligus.
2. 0 baris log dibuang/ditruncate — `logText` tetap dibaca & disimpan utuh dari `AppLogger`.
   "Repack ke Dokumen" tetap ekspor `readLog()` utuh, tidak disentuh.
3. Import tak terpakai (`rememberScrollState`/`verticalScroll`) dibuang, ganti
   `LazyColumn`/`items` (sudah dipakai pola sama di file lain, 0 dependency baru). 0
   formula/state lain diubah.

**0 diverifikasi CI/device Batch 465** — review manual (baca kode + cek balance brace/paren:
`{}` 27/27, `()` 101/101, `[]` 0/0 di `DiagnosticLogSheet.kt`), 0 env Android nyata/device
fisik/compiler Kotlin di sesi ini. Perlu dari user: buka Settings > Log Diagnostik, konfirmasi
sheet terbuka normal (0 freeze/force-close), isi log lengkap/bisa di-scroll/di-ekspor — BARU
setelah itu lanjutkan permintaan Batch 464 (WAJIB DILAKUKAN di RESUME POINT di bawah, masih
berlaku persis, belum terpenuhi).

**Catatan Batch 464 [DATA DEVICE FISIK PERTAMA — MENGEJUTKAN, ganti arah dugaan]**: user kirim 2
potongan logcat hasil instrumentasi Batch 463. Log ke-1 (rotasi) terpotong sebelum baris readback
sempat muncul (0 kesimpulan bisa ditarik). Log ke-2 (rotasi lagi, ditunggu lebih lama) BERHASIL
menangkap readback pertama — hasilnya JUSTRU dari event **minimize() BIASA SEBELUM rotasi terjadi**
(screenWidth=1080, portrait, 0 rotasi terlibat): target `x=1011 y=469` TAPI posisi nyata di layar
250ms kemudian `x=551 y=568` — **selisih 460px di X**. Ini sinyal kuat BARU: mismatch mungkin BUKAN
soal animasi transisi ROTASI (fokus Batch 462 & 463), melainkan window overlay ini `WRAP_CONTENT`
(resize fisik tiap toggle expanded↔minimized) dan `width`/`height` yang dibaca
`snapMinimizedToNearestEdge()` via `container.post{}` mungkin representasi View yang sudah
di-measure TAPI window WindowManager-nya sendiri belum tuntas resize saat `updateViewLayout`
dipanggil — teori KE-4, belum pernah diuji batch mana pun sebelumnya. Rotasi mungkin cuma
kebetulan JUGA memicu fungsi snap yang SAMA, kena race yang SAMA — bukan soal rotasi itu sendiri.
0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. **0 formula/logic diubah lagi** — masih PERSIS instrumentasi, bukan fix.
2. **Readback diperluas**: sekarang JUGA log `container.width`/`container.height` NYATA di momen
   readback, dibandingkan ke `width`/`container.height` yang dipakai saat target dihitung
   (`targetWidth`/`targetHeight`, snapshot lokal) — kalau beda, itu BUKTI LANGSUNG window/view
   masih resize saat snap kita apply (bukan dugaan lagi).
3. **Readback KEDUA ditambah di +800ms** (selain +250ms yang sudah ada dari Batch 463, keduanya
   sekarang pakai 1 fungsi lokal `logReadback(label)` supaya 0 duplikasi kode) — kalau +800ms
   SUDAH cocok ke target (beda dari +250ms yang meleset), itu bukti murni SETTLING/animasi
   sementara (fix: perpanjang delay saja); kalau +800ms MASIH meleset SAMA, itu salah PERMANEN
   (fix: re-urutan resize-dulu-baru-posisikan, BUKAN soal delay).
4. 0 breaking change ke minimize/expand/fade/auto-minimize/rotasi Batch 98-100/453-458/460-463,
   0 import/dependency baru, 0 sektor DITUTUP disentuh.

**0 diverifikasi CI/device Batch 464** — review manual (baca kode + cek balance brace/paren:
`{}` 90/90, `()` 515/515, `[]` 86/86), 0 env Android nyata/device fisik/compiler Kotlin di sesi
ini. Perlu dari user: reproduksi SEKALI LAGI (minimize → rotasi → **diamkan HP minimal 1 detik
penuh**, karena readback terjauh sekarang +800ms), lalu kirim log/ekspor Log Diagnostik. Baca
PERBANDINGAN 250ms vs 800ms + ukuran nyata vs target sebelum putuskan fix apa pun — 2 hasil
berbeda mengarah ke 2 kelas fix yang sama sekali berbeda (lihat poin 3 di atas).

**Catatan Batch 463 [PIVOT KE INSTRUMENTASI]**: user konfirmasi device fisik Batch 462 — "masih
nongol/gak ke kliping" (GAGAL, sama seperti Batch 460/461). User membawa `bubble_log.txt` (logcat
`-iE "floatingbubble|configurationchanged|windowmanager"`) sesuai permintaan eksplisit
PROJECT_STATE.md Batch 462 ("WAJIB logcat device asli sebelum lanjut tebak lagi"). **Log dicek
baris-per-baris (bukan diasumsikan berisi sinyal)**: 0 baris dari `FloatingBubbleService` sama
sekali — 2 satu-satunya kecocokan "floatingbubble" di file itu adalah ECHO PERINTAH grep-nya
sendiri (baris command Termux), BUKAN output aplikasi. Kesimpulan wajib: service ini TIDAK PERNAH
menulis logcat, jadi 3 teori berturut-turut (Batch 460/461/462) SEMUA murni tebakan dari baca-kode,
0 pernah divalidasi data eksekusi nyata. Sesuai SOP sendiri (dilarang tebak fix ke-4 tanpa data),
batch ini **PIVOT — 0 fix baru ke formula/logic, murni instrumentasi**. 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. **0 formula/state/logic diubah** — `EDGE_CLIP_FRACTION`/`touchPad`/`visualWidth`/`screenBounds`/
   `ROTATION_RESNAP_DELAYS_MS` semuanya TETAP persis Batch 460-462, TIDAK disentuh sama sekali.
2. **`AppLogger.w(tag="FloatingBubbleService", ...)` ditambah di 4 titik** (pola sama existing
   `AppLogger.e` di `loadAlbumArtBitmap`, BUKAN `Log.d` polos): (a) entry `onConfigurationChanged`
   — orientation + screenBounds + isMinimized; (b) 2 guard null `bubbleView`/`layoutParams` yang
   sebelumnya `return` diam-diam 0 sinyal; (c) tiap callback `ROTATION_RESNAP_DELAYS_MS` (150ms/
   400ms) benar tereksekusi; (d) di `snapMinimizedToNearestEdge()` — X/Y TARGET tepat sebelum
   `updateViewLayout` + outcome sukses/gagalnya (`runCatching` lama SEBELUMNYA membungkam
   exception total, 0 pernah tahu kalau apply-nya sendiri gagal).
3. **Readback +250ms baru** (tambahan, bukan cuma log): `container.postDelayed { getLocationOnScreen() }`
   membaca posisi NYATA container di layar 250ms setelah tiap apply, dibandingkan ke X/Y target
   yang di-snapshot ke `val` lokal (`targetX`/`targetY`, BUKAN baca ulang `params.x` yang
   objeknya sama dipakai bergantian oleh 3 panggilan snap) — satu-satunya cara MEMBUKTIKAN atau
   MEMBANTAH teori Batch 462 ("sistem menimpa posisi window pasca-snap") dengan data asli, bukan
   dugaan lagi.
4. **`AppLogger.w()` dipilih (bukan `Log.d`)** — otomatis kepakai ke 2 kanal: logcat (tag persis
   "FloatingBubbleService", akan kena grep SAMA yang user pakai) DAN `diagnostic_log.txt` privat
   app (baca/ekspor lewat Settings > Lanjutan > Log Diagnostik) — kalau kanal Termux/adb gagal
   nangkap lagi, kanal kedua tetap ada tanpa perlu setup ADB sama sekali.
5. 0 breaking change ke minimize/expand/fade/auto-minimize Batch 98-100/453/454/455/457/458/460,
   0 sektor DITUTUP disentuh. `AppLogger.w` sudah ada sejak awal (dipakai class lain) — 0 import
   baru, 0 dependency baru.

**0 diverifikasi CI/device Batch 463** — review manual (baca kode + cek balance brace/paren:
`{}` 84/84, `()` 486/486, `[]` 83/83), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Batch ini SENGAJA 0 mengklaim fix — perlu dari user: (1) buka bubble,
minimize, rotasi ke landscape sekali; (2) ambil salah satu — logcat Termux (perintah SAMA persis
`bubble_log.txt` sebelumnya, sekarang HARUS muncul baris "Batch463...") ATAU buka app > Settings >
Lanjutan > Log Diagnostik > ekspor/salin; (3) kirim hasilnya balik. Fix ke-4 (kalau perlu) baru
diputuskan dari log itu, BUKAN teori baru.

**Catatan Batch 462 [FIX RESIDUAL #2]**: konfirmasi device fisik user Batch 461 — masih "nongol",
DIPERJELAS via klarifikasi tap: **100% kelihatan, gak keclip sama sekali** (bukan "kurang tepat
dikit"). Ini membuktikan diagnosis Batch 460/461 ("sumber bounds kurang akurat") SALAH ARAH —
`screenBounds` Batch 461 sudah benar. Kesimpulan: ADA PIHAK LAIN (kemungkinan besar sanitasi posisi
window oleh sistem selama animasi transisi rotasi) menimpa posisi window SETELAH snap kita apply.
0 klarifikasi tap tambahan diperlukan untuk MEMUTUSKAN FIX-nya (root cause tepatnya butuh logcat
device asli yang tidak tersedia di sesi ini — SOP mengizinkan mitigasi defensif berbasis sinyal
kuat yang ada, bukan diam menunggu). 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. **TIDAK ganti formula/sumber data LAGI** — eksplisit mengikuti catatan proses PROJECT_STATE.md
   sendiri (ditulis di Batch 461: "jangan ulang pola ganti API baca metrics"). Formula
   `EDGE_CLIP_FRACTION`/`touchPad`/`visualWidth`/`screenBounds` TETAP tidak disentuh.
2. **Fix — safety-net re-assert 2x delay**: `onConfigurationChanged` memanggil
   `snapMinimizedToNearestEdge()` immediate (seperti sebelumnya) + 2x re-panggil delay 150ms &
   400ms (`ROTATION_RESNAP_DELAYS_MS`, via `view.postDelayed`) — membracket durasi animasi
   transisi rotasi tipikal. Idempotent (0 efek tambahan kalau snap pertama sudah benar) — fallback
   pasti kalau snap pertama ketiban override sistem.
3. 0 breaking change ke formula/state lain Batch 98-100/453-458/460/461, 0 sektor DITUTUP disentuh.

**0 diverifikasi CI/device Batch 462** — review manual (baca kode + cek balance brace/paren:
`{}` 74/74, `()` 436/436, `[]` 75/75), 0 env Android nyata/device fisik/logcat/compiler Kotlin di
sesi ini. **CATATAN JUJUR (WAJIB dibaca sesi berikutnya kalau residual masih ada)**: ini mitigasi
defensif, BUKAN root-cause pasti terverifikasi. Kalau tab masih "nongol"/tidak ter-klip lagi
SETELAH batch ini, JANGAN tebak fix ke-4 tanpa data baru — WAJIB minta logcat device asli user
(command: `adb logcat` atau share via Termux) fokus pada window rotasi + lifecycle
FloatingBubbleService, BARU putuskan fix berikutnya dari situ.

**Catatan Batch 461 [FIX RESIDUAL]**: konfirmasi device fisik user Batch 460 — (1)/(2)/(4)/(5) ✅,
(3) mentok tepi konsisten landscape ❌ ("masih nongol", rotasi bolak-balik). Fix Batch 460
(`resources.displayMetrics`→`currentWindowMetrics.bounds`) TERBUKTI BELUM CUKUP untuk item (3).
0 klarifikasi tap diperlukan — laporan user ("except no.3 ❌ masih nongol") cukup spesifik
dipadukan dengan review kode static (root cause dapat ditentukan deterministik dari dokumentasi
resmi WindowManager, bukan ambiguitas material). 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. **Root cause sebenarnya ditemukan**: `windowManager` di kelas ini didapat dari Context `Service`
   biasa (BUKAN `UiContext`/`WindowContext`) — per dokumentasi resmi
   `WindowManager#getCurrentWindowMetrics()`, Context non-UI SELALU jatuh ke
   `getMaximumWindowMetrics()`, TIDAK dijamin sinkron atomik persis di momen rotasi. Kelas masalah
   SAMA dengan `resources.displayMetrics` (Batch 460) — root sumber sama-sama Context Service yang
   sama, swap API Batch 460 mengurangi tapi tidak menghilangkan race, terutama saat rotasi
   bolak-balik cepat (persis skenario device-test #3).
2. **Fix — `screenBounds` (single source of truth, field baru `Rect`)**: diisi dari parameter
   `newConfig` di `onConfigurationChanged` (dp→px via `resources.displayMetrics.density`) — SATU-
   SATUNYA sumber yang DIJAMIN sistem fresh PERSIS di momen callback rotasi, bukan re-query Context
   async. Nilai awal (sebelum rotasi pertama) di-set sekali di `onCreate` dari
   `currentWindowMetrics.bounds` (baseline). Refresh dipindah ke baris PALING ATAS
   `onConfigurationChanged`, sebelum early-return guard `bubbleView`/`layoutParams` null.
3. **4 titik baca diganti ke `screenBounds` ter-cache** (titik sama seperti Batch 460):
   `onConfigurationChanged`, `setupDrag`, `expand()`, `snapMinimizedToNearestEdge()`. 0 lagi query
   `windowManager.currentWindowMetrics` langsung di titik mana pun selain nilai awal `onCreate`.
4. 0 breaking change ke `EDGE_CLIP_FRACTION`/`touchPad`/`visualWidth` (Batch 460), 0 breaking
   change ke minimize/expand/fade/auto-minimize Batch 98-100/453/454/455/457/458.

**0 diverifikasi CI/device Batch 461** — review manual (baca kode + cek balance brace/paren:
`{}` 72/72, `()` 416/416, `[]` 72/72), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. README.md diperbarui (blockquote Batch 460 dikoreksi ANTI-STALE:
item 1/2/4/5 ditandai ✅ device-fisik, item 3 ditandai ❌+fix baru; deskripsi fitur bubble
dikoreksi dari klaim "konsisten landscape" yang ternyata belum terbukti).

**Catatan Batch 460**: 2 instruksi eksplisit user sekaligus — (1) "perluas touch target nya biar
gak nyusahin, sedangkan visual turunkan jadi ~10% yang timbul saja"; (2) "fix juga agar fitur
bubble bisa ke kliping mentok ujung layar walaupun hp sedang dalam mode horizontal". Kelanjutan
langsung dari residual UX yang dicatat Batch 458/459 (mini trigger "sedikit lebih susah" di-tap)
— opsi umum yang sudah dicatat di situ ("perbesar touch target independen dari lebar visual")
sekarang dieksekusi. 0 klarifikasi tap diperlukan untuk instruksi (1): kata "timbul" dipakai
eksplisit, konsisten catatan proses di bawah (Batch 456→457→458). 0 sektor DITUTUP disentuh.

**2 file diubah** (dalam batas 3 file/tugas): `bubble_minimized.xml` + `FloatingBubbleService.kt`.
1. **Touch target dipisah dari visual**: `bubble_minimized.xml` root 48dp→88dp (100% transparan,
   0 background baru) jadi murni area-sentuh; visual bulat asli (background+art) pindah ke child
   baru `bubble_minimized_visual` (tetap 48dp, gravity CENTER, padding sentuh simetris kiri/kanan
   supaya valid di sisi mana pun tab menempel).
2. **`snapMinimizedToNearestEdge()` — 1 fungsi yang sama, dipisah jadi 2 lebar**: `visualWidth`
   dipakai `EDGE_CLIP_FRACTION` (formula lama tidak berubah), `width` root dipakai posisi X
   window. `touchPad` (selisih /2) selalu ikut ke sisi yang tetap di layar — area sentuh naik
   TANPA ikut mengecil saat `EDGE_CLIP_FRACTION` naik (akar masalah residual UX Batch 458: 1 angka
   dulu mengontrol visual DAN touch sekaligus, sekarang 2 parameter independen).
3. **`EDGE_CLIP_FRACTION` 0.7f → 0.9f**: bagian TIMBUL turun 30%→~10% sesuai instruksi (1).
4. **Fix landscape (instruksi 2)**: `resources.displayMetrics` (4 titik: `onConfigurationChanged`,
   `setupDrag`, `expand`, `snapMinimizedToNearestEdge`) diganti `windowManager.
   currentWindowMetrics.bounds` (API 30+, aman minSdk 31) — root cause paling mungkin: metrics
   Context Service tidak dijamin ter-refresh seketika saat `onConfigurationChanged` terpanggil
   pasca-rotasi, beda dari Activity/WindowContext.
5. 0 breaking change ke minimize/expand/fade/auto-minimize Batch 98-100/453/454/455/457/458.

**0 diverifikasi CI/device Batch 460** — review manual (baca kode + cek balance brace/paren:
`{}` 72/72, `()` 390/390, `[]` 63/63), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Perlu konfirmasi device fisik berikutnya: (1) mini trigger LEBIH
GAMPANG di-tap dari Batch 458/459 (target: fix residual UX yang dilaporkan user, BUKAN cuma "masih
bisa di-tap"); (2) bagian kelihatan tab minimized ~10% (lebih ngumpet dari Batch 458's ~30%); (3)
rotasi ke landscape (dan bolak-balik beberapa kali) → tab minimized SELALU mentok tepi kiri/kanan
dengan benar; (4) drag tab minimized tetap 100% kelihatan/terkontrol penuh selagi digeser; (5) 0
regresi ke minimize/expand/fade/auto-minimize Batch 98-100/453/454.

**Catatan Batch 458**: user eksplisit lanjut tuning setelah Batch 457 ("ubah jadi ~30%!!") — angka
mentah tanpa konteks ulang, berisiko ulang kesalahan arah Batch 456. **Diklarifikasi via pilihan
tap (BUKAN ditebak)**: "~30%" merujuk ke bagian TIMBUL (kelihatan), bukan ke fraksi klip itu
sendiri. Tuning lanjutan murni, bukan mandat baru. 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` —
1. **1 nilai konstanta dinaikkan, 0 fungsi baru**: `EDGE_CLIP_FRACTION` 0.5f → 0.7f (fraksi
   SEMBUNYI naik, supaya fraksi TIMBUL/kelihatan turun ke ~30% sesuai konfirmasi tap user).
2. **0 titik panggil baru**: semua pemicu snap (drag-lepas, auto-minimize, chevron, restart,
   rotasi) otomatis ikut nilai baru.
3. Formula `hiddenWidth = width * EDGE_CLIP_FRACTION` (Batch 456) TETAP tidak disentuh — cuma
   nilai konstanta.
4. KDoc kelas & `snapMinimizedToNearestEdge()` diperbarui dengan catatan Batch 458.
5. 0 file lain disentuh, 0 breaking change ke minimize/expand/fade/auto-minimize Batch 453/454.

**0 diverifikasi CI/device Batch 458** — review manual (baca kode + cek balance brace/paren:
`{}` 71/71, `()` 347/347, `[]` 52/52), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini (konsisten pola Batch 435-457). Perlu konfirmasi device fisik
berikutnya: (1) tab minimized SEKARANG ~30% kelihatan/~70% tersembunyi (lebih ngumpet dari Batch
455/457 yang 50/50) di semua jalur snap; (2) mini trigger MASIH gampang di-tap walau makin kecil
bagian kelihatannya (touch target tidak "meleset"); (3) drag tab minimized tetap 100%
kelihatan/terkontrol penuh selagi digeser; (4) 0 regresi ke minimize/expand/fade/auto-minimize
Batch 98-100/453/454.

**Catatan Batch 457**: feedback eksplisit user langsung setelah Batch 456 ("revert progress
kliping. bukannya hilangin yang timbul malah dibikin tambah timbul, bukan saya suruh") — Batch 456
SALAH ARAH: `EDGE_CLIP_FRACTION` 50%→30% justru MEMPERBESAR bagian tab yang kelihatan (nambah
"timbul"), kebalikan dari yang diinginkan. **REVERT murni**, bukan mandat baru. 0 sektor DITUTUP
disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` —
1. **1 nilai konstanta dikembalikan, 0 fungsi baru**: `EDGE_CLIP_FRACTION` 0.3f → 0.5f (nilai asli
   Batch 455). Formula `hiddenWidth = width * EDGE_CLIP_FRACTION` (generalisasi Batch 456) TETAP
   DIPERTAHANKAN — netral arah, cuma nilai konstanta yang salah kemarin.
2. **0 titik panggil baru**: semua pemicu snap (drag-lepas, auto-minimize, chevron, restart,
   rotasi) otomatis ikut nilai revert, 0 perubahan lain.
3. Hasil setelah revert identik matematis dengan Batch 455 (regresi-aman, sudah diverifikasi
   formula-nya di Batch 456).
4. KDoc kelas & KDoc `snapMinimizedToNearestEdge()` diperbarui — Batch 456 ditandai [SALAH ARAH],
   Batch 457 dicatat sebagai revert (ANTI-STALE, 1 file yang sama, 0 file dok terpisah disentuh).
5. 0 file lain disentuh, 0 breaking change ke minimize/expand/fade/auto-minimize Batch 453/454.

**0 diverifikasi CI/device Batch 457** — review manual (baca kode + cek balance brace/paren:
`{}` 71/71, `()` 340/340, `[]` 50/50), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini (konsisten pola Batch 435-456). Perlu konfirmasi device fisik
berikutnya: (1) tab minimized kembali separuh tersembunyi di luar layar/separuh kelihatan sebagai
mini trigger (SAMA seperti Batch 455, BUKAN lagi ~70% kelihatan Batch 456) di semua jalur snap;
(2) mini trigger tetap gampang di-tap; (3) drag tab minimized tetap 100% kelihatan/terkontrol
penuh selagi digeser; (4) 0 regresi ke minimize/expand/fade/auto-minimize Batch 98-100/453/454.

**Catatan Batch 456 [SALAH ARAH — DIREVERT Batch 457 di atas]**: feedback lanjutan user langsung setelah Batch 455 ("sudah ke kliping
walaupun agak timbul") — diklarifikasi via pilihan tap: **"bagian yang kepotong terlalu besar,
perkecil clip-nya"**. Refinement tuning murni ke [snapMinimizedToNearestEdge], bukan mandat baru.
0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` —
1. **1 konstanta baru, 0 fungsi baru**: `EDGE_CLIP_FRACTION = 0.3f` (companion object, pola sama
   `IDLE_FADE_ALPHA` dkk) — fraksi lebar tab yang disembunyikan di luar layar, turun dari 50%
   (`width/2` hardcoded, Batch 455) ke 30%.
2. **Formula digeneralisasi**: `snapMinimizedToNearestEdge()` — `width/2` hardcoded diganti
   `hiddenWidth = width * EDGE_CLIP_FRACTION`. Di `EDGE_CLIP_FRACTION = 0.5f` formula ini identik
   matematis dengan Batch 455 (regresi-aman), tuning berikutnya (kalau ada) tinggal ubah 1 angka.
3. **0 titik panggil baru**: semua pemicu snap yang sudah ada (drag-lepas, auto-minimize Batch
   454, chevron manual, restart service, rotasi) otomatis ikut fraksi baru, 0 perubahan lain.
4. 0 file lain disentuh, 0 breaking change ke minimize/expand/fade Batch 453/auto-minimize Batch
   454/half-clip Batch 455 — cuma besaran fraksi clip yang berubah.

**0 diverifikasi CI/device Batch 456** — review manual (baca kode + cek balance brace/paren:
`{}` 71/71, `()` 339/339, `[]` 49/49), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini (konsisten pola Batch 435-455). Perlu konfirmasi device fisik
berikutnya: (1) tab minimized kelihatan lebih "penuh"/kurang timbul dibanding Batch 455 (~70%
lebar kelihatan, bukan 50%) di SEMUA jalur snap; (2) mini trigger tetap gampang di-tap; (3) drag
tab minimized tetap 100% kelihatan/terkontrol penuh selagi digeser; (4) 0 regresi ke
minimize/expand/fade/auto-minimize Batch 98-100/453/454.

**Catatan Batch 455**: feedback eksplisit user langsung setelah Batch 454 ("minimize otomatis nya
berhasil, TAPI yang benar-benar diinginkan: circle bubble kliping setengah/menyisakan mini
trigger, wajib mentok maksimal ke tepi layar saat idle"). Refinement VISUAL murni ke tab minimized
Batch 100 (bukan mandat baru/sektor baru) — tab yang tadinya flush-tapi-100%-kelihatan di tepi
sekarang setengah lebarnya sengaja melewati batas layar. 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` —
1. **1 titik kontrol yang sama, 0 fungsi baru**: `snapMinimizedToNearestEdge()` — SATU-SATUNYA
   fungsi yang menghitung posisi X tab minimized (dipanggil dari drag-lepas, auto-minimize Batch
   454, tombol chevron manual, restart service, rotasi) — cuma formula X-nya yang berubah, dari
   `0`/`screenWidth - lebarTab` (flush, 100% kelihatan) jadi `-lebarTab/2`/`screenWidth -
   lebarTab/2` (setengah lebar melewati batas layar).
2. **0 flag/permission baru**: window overlay SUDAH `FLAG_LAYOUT_NO_LIMITS` sejak Batch 100 —
   prasyarat X negatif/lewat `screenWidth` diterima WindowManager sudah terpenuhi dari awal,
   sistem yang otomatis memotong render di luar layar, 0 clip manual perlu ditulis.
3. **0 mekanisme/state/timer baru**: `lebarTab/2` dihitung dari `container.width` real via
   `container.post{}` yang sudah ada (pola sama Batch 100) — bukan angka dp ditebak manual.
4. **Drag aktif tidak terpengaruh**: `setupDrag()` (clamp `[0, maxX]` pakai lebar penuh) 0
   disentuh — half-clip HANYA berlaku begitu jari dilepas & tab snap ke tepi dalam keadaan diam
   (idle), sesuai kata "saat idle" di instruksi user, bukan selagi masih digeser.
5. 0 file lain disentuh, 0 breaking change ke `minimize()`/`expand()`/fade Batch 453/auto-minimize
   Batch 454 — cuma X akhir tab yang berubah, mekanisme kapan snap dipanggil sama sekali tidak
   disentuh.

**0 diverifikasi CI/device Batch 455** — review manual (baca kode + cek balance brace/paren:
`{}` 71/71, `()` 325/325, `[]` 46/46), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini (konsisten pola Batch 435-454). Perlu konfirmasi device fisik
berikutnya: (1) tab minimized kelihatan kepotong SETENGAH mentok tepi kiri/kanan (bukan lagi bulat
utuh 100% kelihatan) baik saat manual-minimize, auto-minimize idle, restart app, maupun rotasi;
(2) sisa "mini trigger" yang kelihatan tetap bisa di-tap untuk expand() — touch target setengah
lingkaran tidak "meleset"/butuh tap presisi berlebihan; (3) drag tab minimized (pindah ke posisi
lain) tetap 100% kelihatan/terkontrol penuh SELAGI digeser, cuma clip setengah setelah dilepas;
(4) 0 regresi ke minimize/expand/fade/auto-minimize-idle Batch 98-100/453/454.

**Catatan Batch 454**: lanjutan langsung Batch 453 — mandat UTAMA user ("wajib bisa
split/di-minimize total") belum tuntas Batch 453 (baru fallback minimumnya, fade). Batch ini
menuntaskan mandat utamanya: auto-minimize total otomatis kalau bubble tetap idle lebih lama
lagi setelah fade. Perluasan sektor bubble (Roadmap #11), 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` —
1. **Timer kedua, 1 titik kontrol yang sama**: `idleMinimizeJob` baru, dijadwalkan/dibatalkan di
   `keepAwakeAndScheduleFade()` — fungsi yang SAMA PERSIS dipanggil `setupDrag`/`setupControls`
   Batch 453, jadi 0 perubahan di kedua fungsi itu. Delay dihitung dari titik interaksi terakhir
   yang SAMA dengan timer fade (bukan ditambah setelah fade selesai) — `IDLE_AUTO_MINIMIZE_DELAY_MS`
   = 6000ms, > `IDLE_FADE_DELAY_MS` (2500ms) supaya urutan visual selalu fade dulu baru collapse.
2. **0 logic collapse baru**: auto-trigger cuma manggil `minimize()` yang sudah ada sejak Batch
   100 apa adanya (termasuk guard `if (isMinimized) return` di dalamnya — aman dipanggil berulang
   walau user sempat minimize manual duluan lewat chevron).
3. **0 mekanisme timer baru**: reuse `bubbleScope` yang sama dgn `idleFadeJob`/`bubbleArtJob` —
   otomatis ikut ter-cancel oleh `bubbleScope.cancel()` di `onDestroy()` yang sudah ada. 0 import
   baru (delay/launch sudah diimport Batch 453).
4. Alpha container TIDAK direset saat auto-minimize (tab hasil collapse mewarisi alpha fade yang
   sedang berjalan — konsisten desain "1 titik kontrol alpha di container" Batch 453).
5. 0 file lain disentuh, 0 breaking change ke `minimize()`/`expand()`/`setupDrag`/fade Batch 453.

**0 diverifikasi CI/device Batch 454** — review manual (baca kode + cek balance brace/paren:
`{}` 71/71, `()` 313/313, `[]` 38/38), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini (konsisten pola Batch 435-453). Perlu konfirmasi device fisik
berikutnya: (1) bubble auto-collapse jadi tab 48dp tepi layar setelah ±6 detik idle TANPA
sentuhan (menyusul fade ±2.5 detik yang sudah jalan lebih dulu); (2) TIDAK auto-collapse selagi
masih digeser/tombol kontrolnya ditekan (timer ikut ter-reset sama seperti fade); (3) minimize
manual (tap chevron) & auto-minimize tidak saling konflik/duplikasi state; (4) 0 regresi ke
mekanisme minimize/expand/snap-tepi/drag-bebas Batch 98-100 & fade Batch 453.

**Catatan Batch 453**: instruksi eksplisit user — fitur mini player mengambang (bubble) "wajib
bisa split/di-minimize total, atau minimal dulu bisa fade out saat tidak digeser". Perluasan
langsung sektor bubble (Roadmap #11, Batch 95-100), bukan reopen sektor DITUTUP manapun.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` —
1. **Cek dulu, bukan reimplementasi dari nol**: mekanisme "total" SUDAH ada sejak Batch 100 —
   tombol chevron minimize mengciutkan pill jadi tab 48dp nempel tepi layar (manual, lewat tap).
   Celah sesungguhnya: kondisi IDLE (bubble dibiarkan diam TANPA aksi apa pun) tetap 100% opaque
   selamanya, menutupi konten di baliknya — persis skenario di video user (pill mengambang diam
   di atas daftar "Paling Sering Diputar"). Fix batch ini menyasar celah itu, sesuai opsi fallback
   eksplisit user ("minimal dulu bisa fade out").
2. **Fitur baru**: `keepAwakeAndScheduleFade()` — meredupkan alpha `bubbleView` (container
   `FrameLayout`, BUKAN per-child) ke 0.45f setelah 2.5 detik tanpa sentuhan, `ViewPropertyAnimator`
   `View.animate()` 250ms. Mengembalikan ke opaque penuh SEKETIKA (`view.animate().cancel()` +
   `alpha=1f`) di SETIAP titik masuk interaksi baru: `setOnTouchListener` root (`setupDrag`, drag
   MAUPUN tap-buka-app) dan ke-4 `setOnClickListener` tombol kontrol (`setupControls` —
   play/pause/prev/next/minimize, WAJIB direset terpisah karena `ImageButton` clickable
   mengonsumsi `ACTION_DOWN` duluan sebelum sempat ke `OnTouchListener` root, lihat KDoc
   `setupDrag` yang sudah ada). Alpha container otomatis berlaku ke child mana pun yang sedang
   `VISIBLE` (pill penuh ATAU tab minimized, mengikuti pola toggle-visibility 1-container Batch
   100) — 1 titik kontrol, 0 duplikasi logic per state.
3. **0 mekanisme baru untuk timer**: reuse `bubbleScope` (`CoroutineScope(Dispatchers.Main +
   Job())`) yang SUDAH ada untuk `bubbleArtJob` — job baru `idleFadeJob` (`delay()` +
   cek-null-lalu-animate), otomatis ikut ter-cancel oleh `bubbleScope.cancel()` di `onDestroy()`
   yang sudah ada, 0 Handler/Thread baru, 0 leak. 1 import baru: `kotlinx.coroutines.delay`
   (satu paket persis dengan `launch`/`withContext` yang sudah diimport).
4. Alpha window TIDAK mengubah keterjangkauan sentuh (`FLAG_NOT_FOCUSABLE` independen dari
   alpha) — tap pada bubble yang lagi pudar tetap berfungsi normal, murni sinyal visual.
5. 0 file lain disentuh, 0 breaking change ke `minimize()`/`expand()`/`setupDrag` logic lama.

**0 diverifikasi CI/device Batch 453** — review manual (baca kode + cek balance brace/paren:
`{}` 70/70, `()` 294/294, `[]` 28/28), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini (konsisten pola Batch 435-452). Perlu konfirmasi device fisik
berikutnya: (1) bubble (pill penuh MAUPUN tab minimized) benar meredup ke ~45% opacity setelah
±2.5 detik diam, TIDAK meredup selagi masih di-drag/di-tap kontrolnya; (2) opacity kembali penuh
SEKETIKA begitu disentuh lagi (drag maupun tap tombol), 0 delay/lompatan visual; (3) 0 regresi ke
mekanisme minimize/expand/snap-tepi/drag-bebas Batch 98-100 yang sudah ada.

**Catatan Batch 452**: 2 instruksi eksplisit user — (1) minimalkan tulisan label nav bawah yang
terpotong ellipsis, (2) animasi pill/tab WAJIB berhenti tepat di tab tujuan tanpa "offside" (baik
mode drag-langsung-di-bar maupun tap-tab biasa).

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. **Fix truncation**: root cause — padding horizontal 12.dp (kiri+kanan) di `Column`
   `GlassTabIcon` memakan 24.dp dari ~1/3 lebar bar SEBELUM `Text` diukur, cukup memicu ellipsis
   pada label 12 huruf ("Perpustakaan") di layar sempit KONDISI NORMAL (bukan cuma font-scale
   aksesibilitas besar spt catatan lama Batch 442). Fix: 12.dp -> 4.dp. Touch target 0 terdampak
   (area sentuh = `Box.weight(1f, fill=true)` di `CustomNavBarTabItem`, pembungkus DI LUAR Column
   ini, bukan padding Column). 0 sentuh style/fontSize/typography token (`labelMedium` dari Batch
   442 tetap dipakai apa adanya) — murni jarak. Efek samping disengaja: pill solid Skeu (dibungkus
   padding sama) ikut sedikit lebih ramping, masih proporsional (aturan solid Batch 58/61/79 tidak
   disentuh).
2. **Fix "offside" drag/tap**: root cause — di loop drag-langsung-di-tab-bar (Batch 442/448),
   `if (!change.pressed) break` lama dicek DI AWAL badan loop, SEBELUM posisi event dibaca. Untuk
   event UP (pelepasan jari), loop break LANGSUNG tanpa pernah memproses posisi UP itu sendiri ke
   `tabBarDragIndexPx`/`hoveredIndex` — keduanya nyangkut di event MOVE kedua-dari-akhir. Pada
   drag cepat (flick, event batching sistem), posisi MOVE terakhir bisa beda 1 kolom penuh dari
   titik lepas jari sungguhan → pill/route commit ke tab yang SALAH (1 kolom sebelum tujuan asli).
   Fix: posisi TIAP event (termasuk UP) diproses dulu sama seperti MOVE, `pressed` dicek TERAKHIR
   (akhir badan loop) sbg syarat lanjut/berhenti — bukan lagi syarat lewati pemrosesan. 0
   state/Animatable/mekanisme baru — murni urutan 2 baris dipertukar dalam loop yang sudah ada.
   Perbaikan ini juga menjamin `hoveredIndex` akurat utk tap biasa (down+up di kolom sama) krn
   posisi UP kini selalu ikut diproses, bukan cuma diasumsikan sama dgn `down`.
3. 0 import baru, 0 file lain disentuh.

**0 diverifikasi CI/device Batch 452** — review manual (baca kode + cek balance brace/paren: `{}`
333/333, `()` 1201/1201, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Item belum-terverifikasi bertambah 2: (1) label 3 tab tidak lagi
kepotong ellipsis di kondisi FONT normal (perlu device fisik, layar sempit maupun lebar — font-
scale aksesibilitas BESAR tetap bisa memicu ellipsis by design, itu memang jaring pengaman Batch
442 yang disengaja, bukan target "minimize" batch ini); (2) drag cepat (flick) di tab-bar berhenti
TEPAT di tab yang jari lepaskan (0 lagi "mundur 1 kolom"), demikian pula tap-tab biasa — perlu
device fisik, terutama drag flick cepat lintas >1 kolom.

**Catatan Batch 451**: user konfirmasi via video device asli — pill kembali ukuran NORMAL (fix
Batch 450 valid, 0 lagi kapsul raksasa). Analisis frame-by-frame 60fps tambahan (bukan cuma
laporan user) juga mengonfirmasi temuan ASLI Batch 449: pill kini meluncur mulus dari 1 tab ke
tab lain TANPA kilatan kotak abu-abu di tab yang ditinggalkan — fix hapus `NavigationBarItem`
(Batch 449) + fix `fillMaxHeight` (Batch 450) keduanya TERBUKTI benar di device fisik. 0 kode
diubah batch ini (murni sinkronisasi status verifikasi ke docs). Detail: `CHANGELOG.md` Batch 451.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. `GlassTabIcon`: background/border pill glass per-tab (non-Skeu) DIHAPUS TOTAL — dulu setiap
   tab menggambar pill sendiri dibatasi lebar kolomnya. Skeu (pill solid diskrit, aturan Batch
   58/61/79) TIDAK disentuh.
2. `AppNavHost`/`NavigationBar`: `tabBarDragLastIndexPx` + `tabBarDragBridgeAlpha` (state Batch
   446, pill "bridge" yang hanya aktif KONDISIONAL saat drag) dihapus, diganti 1
   `Animatable navPillIndexAnim` — sumber posisi rest pill tunggal, valid di SEMUA state.
   `drawWithContent` pada `NavigationBar` sekarang SATU-SATUNYA penggambar pill (aktif idle/tap/
   drag/nudge, dulu cuma drag), posisi: live `tabBarDragIndexPx` saat drag langsung (0 lag,
   mentah 1:1 jari — pola Batch 444/445 dipertahankan persis), fallback nudge-konten
   (`tabDragOffsetPx`, pecahan ±0.5 kolom dari titik rest) atau `navPillIndexAnim.value` saat
   diam. `navPillIndexAnim` di-snapTo+animateTo(tween 220ms) di titik SELESAI drag (handoff dari
   posisi jari terakhir) & di 3 `onClick` tap biasa — durasi SAMA PERSIS `glassAlphaAnim`
   (warna ikon/label) supaya pill & warna tiba bersamaan.
3. Warna ikon/label (`lerp` kontinu ikut jari, `tabBarDragFocus`/`tabMagnifyFocus`) TIDAK
   diubah — bagian itu SUDAH 1:1 sesuai video (state Batch 447), murni pill BACKGROUND yang
   direstrukturisasi total.
4. 0 import baru (`Animatable`/`tween`/`drawWithContent`/dll semua sudah ada sejak batch lalu).
   1 komentar header import (`drawWithContent`, dekat baris import) diperbarui — sebelumnya
   menyebut identifier `tabBarDragBridgeAlpha` yang kini sudah dihapus (anti-stale).

0 file lain disentuh. `README.md` + `CHANGELOG.md` diperbarui (bullet unverified tab-bar +
entry Batch 448 baru) — detail lengkap di masing-masing file.

**0 diverifikasi CI/device Batch 448** — review manual (baca kode + cek balance brace/paren:
`{}` 331/331, `()` 1117/1117, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Item belum-terverifikasi bertambah 1: pill unified meluncur mulus
lintas kolom TANPA seam/kotak ganda (fix utama batch ini, PALING PENTING dikonfirmasi krn itu
persis komplain user), TANPA regresi ke item Batch 442/444/445/446/447 yang sudah ada (label tak
terpotong, tahanan visual ujung kolom, 0 lag drag, warna ikon+label lerp bersamaan) — perlu
konfirmasi device fisik (0 tersedia sesi ini, sama seperti Batch 435-447).

**Catatan Batch 447**: user lampirkan video referensi baru (rekaman iOS Jam asli — drag lintas 4
tab Alarm/Jam dunia/Timer/Stopwatch) + instruksi "lanjutkan progress menuju mekanisme tampilan
sesuai video" — 0 poin komplain tertulis eksplisit, ZIP sama (`SONIX_v446.zip`, 0 source baru).
Perluasan langsung sektor drag tab-bar yang sama (Batch 442/444/445/446), bukan reopen sektor
DITUTUP manapun.

**Analisis video** (50 frame @3fps, cross-check ke kode): mekanisme bridging pill lintas-kolom +
lerp warna ikon (Batch 440/446) SUDAH cocok 1:1 dgn video. 1 gap ditemukan lewat pembacaan kode
(bukan asumsi visual semata): label teks (`MagnifyingTabLabel`) TIDAK ikut lerp warna kontinu —
beda dari ikon yang sudah (root cause detail: komentar inline kode dekat definisi fungsi ini).

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. `MagnifyingTabLabel` param baru `color: Color? = null` (default = 0 override, IDENTIK
   perilaku lama) → `Text(color = color ?: Color.Unspecified, ...)`.
2. `GlassTabIcon` (titik pemanggil): `labelColor` baru = `lerp(unselectedTextColor, tint,
   glassAlpha)` (pola PERSIS `unselectedIconColor` ikon, 0 hitungan/token warna baru), null utk
   Skeu (aturan solid Batch 58/61/79 tidak disentuh). Parameter ke-2 `MagnifyingTabLabel`
   diganti dari `focus` mentah → `glassAlpha` (identik selama drag aktif — `glassAlpha`
   snapTo(focus) tiap frame drag; beda HANYA di jendela easing 220ms pasca lepas jari: kini
   scale/opacity label ikut melunak bareng warna, bukan snap instan sendirian spt sebelumnya).
3. 0 import baru (`Color`/`lerp`/`NavigationBarItemDefaults` semua sudah ada sejak batch lalu).

0 file lain disentuh.

**0 diverifikasi CI/device Batch 447** — review manual (baca kode + cek balance brace/paren:
`{}` 331/331, `()` 1086/1086, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Item belum-terverifikasi bertambah 1: label teks kini benar2 berubah
warna BERSAMAAN dgn ikon secara kontinu selama drag (bukan cuma ikon, teks menyusul-lompat pas
commit index-crossing) DAN scale/opacity label easing mulus pasca lepas jari (0 snap instan) —
perlu konfirmasi device fisik (0 tersedia sesi ini, sama seperti Batch 435-446).

**Catatan Batch 446**: feedback eksplisit user PASCA Batch 445 (video ilustrasi dilampirkan,
perluasan langsung drag tab-bar Batch 442/444/445) — 1 poin: animasi pill masih terpisah oleh
gap kosong kecil di antara label tab; seharusnya warna/semantik ikut jari juga lintas celah itu.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` — root cause: `tabBarDragFocus`
(Batch 444) SUDAH kontinu secara matematis (diverifikasi manual — crossfade tepat 0.5/0.5 pas di
batas 2 kolom), TAPI tiap `GlassTabIcon` (3 titik pemakaian) menggambar pill highlight-nya
SENDIRI-SENDIRI dibatasi ke kolom masing-masing — 2 pill setengah-nyala itu tetap 2 kotak
TERPISAH dgn spasi tak-tergambar (padding internal kolom NavigationBarItem) di antaranya, kebaca
mata sbg "jeda"/patah walau angka focus-nya kontinu. Bug lapisan render, bukan bug angka.
1. **Fix utama**: 1 pill TAMBAHAN (bukan pengganti 3 pill `GlassTabIcon` lama — itu TETAP jalan
   apa adanya utk tap/nudge-konten-swipe/idle, 0 regresi di situ) digambar via
   `Modifier.drawWithContent` pada `NavigationBar` itu sendiri (bukan composable/Box baru, 0
   restrukturisasi tree) — HANYA aktif selama drag LANGSUNG di tab-bar (`tabBarDragIndexPx` bukan
   NaN). Posisi X dari nilai kontinu yang SAMA PERSIS (`idxPos * columnWidth`) — bebas meluncur
   MELINTASI celah antar kolom krn 1 kanvas bersama, bukan per-composable. Warna/alpha/radius
   IDENTIK pill lama (tint primary, 0.16f/0.14f, 16.dp) — 1 aksen visual, cuma lapisannya beda.
2. **State baru**: `tabBarDragLastIndexPx` (posisi valid terakhir, krn `tabBarDragIndexPx` sendiri
   balik NaN duluan tepat saat jari lepas) + `tabBarDragBridgeAlpha` (`Animatable`, snapTo(1)
   instan saat drag mulai — konsisten filosofi real-time Batch 445 — animateTo(0, tween(220)) saat
   jari lepas, durasi sinkron `glassAlphaAnim` yg sudah ada) supaya fade-out pill baru 0
   lompatan/pop visual pas handoff ke 3 pill lama.
3. Skeu DIKECUALIKAN (`isSkeuTheme()`, dibaca 1x baru sbg `navBarIsSkeu`) — aturan lama "solid,
   bukan kaca" (Batch 58/61/79) tidak disentuh.
4. Import baru: `androidx.compose.ui.draw.drawWithContent` (SATU-SATUNYA import baru — Offset/
   Size/CornerRadius/Stroke fully-qualified inline, pola sama persis `Offset(0f,0f)` yg sudah ada).

0 file lain disentuh. **Koreksi dok tambahan** (Anti-Stale): "Konvensi penamaan ZIP & versi" di
bawah & README.md § "Standar Penomoran Versi" masih menyebut `AudioPlayer-batchN-release.zip` —
sumber P1 (nama ZIP user, `SONIX_v445.zip`) & `app_name`/judul README konfirmasi branding AKTIF =
**SONIX** (package `com.rudi.audioplayer`/`rootProject.name="AudioPlayer"` SENGAJA tetap, lihat
"Aturan sesi aktif" #5 — tidak terikat branding). Kedua baris diperbarui ke `SONIX_v<batch>.zip` —
skema versionCode/versionName/APK/tag rilis TIDAK terkait, TIDAK ikut diubah.

**0 diverifikasi CI/device Batch 446** — review manual (baca kode + cek balance brace/paren: `{}`
331/331, `()` 1060/1060, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses jaringan
Gradle di sesi ini. Item belum-terverifikasi bertambah 1: pill drag bersama terlihat MENYATU mulus
melintasi celah antar-label (bukan 2 pill terpisah) sesuai video ilustrasi user, warna/posisi tetap
1:1 sinkron jari (regresi Batch 445 tidak terjadi), DAN handoff ke 3 pill lama pas jari dilepas 0
lompatan visual — semua perlu konfirmasi device fisik (0 tersedia sesi ini, sama seperti Batch
435-445).

**Catatan Batch 445**: feedback eksplisit user PASCA Batch 444 (bukan reopen sektor DITUTUP
manapun, perluasan langsung drag tab-bar Batch 442/444) — 2 poin: (1) "drag jari real-time belum
sepenuhnya smooth like iOS", (2) "floating effect HANYA saat drag, tampilan yang dilewati berubah
warna seketika real-time (bukan cuma pindah warna instant lintas tab)".

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` — root cause TUNGGAL utk kedua
poin: `animateFloatAsState(targetValue = focus, tween(220))` di `GlassTabIcon` (Batch 440) adalah
lapis smoothing KEDUA di atas `focus` yang SUDAH kontinu real-time (fungsi tenda
`tabBarDragFocus`, Batch 444) — targetnya bergerak tiap event pointer-move selama drag, jadi
tween 220ms terus "mengejar" target yang TERUS PINDAH → nilai yang dirender SELALU tertinggal
dari posisi jari asli (poin 1), dan warna ikon/pill (lerp ikut `glassAlpha`) terlihat
menyusul-lompat bukan berubah seketika sinkron dgn jari (poin 2).
1. **Fix utama**: `animateFloatAsState` → `Animatable` manual + param baru `isDragging: Boolean`
   di `GlassTabIcon` (dihitung 1x di pemanggil: `isTabBarDragging = tabBarDragIndexPx bukan NaN
   ATAU tabDragOffsetPx != 0`, cover 2 sumber drag — tab-bar langsung + nudge swipe-konten).
   Selama `isDragging` true: `snapTo(focus)` tiap frame (0 animasi, 1:1 sinkron mentah — pola
   sama `tabDragOffsetPx`/`tabBarOverscrollPx`). Selesai drag: `animateTo(focus, tween(220))`
   dari titik sinkron terakhir (0 lompatan). Tap biasa (0 drag) tetap tween(220) lama, 0 regresi.
2. **Fix pendukung**: `MagnifyingTabLabel` — `style = baseStyle.copy(fontSize = ...)` (real
   remeasure/relayout tiap frame drag, dobel dgn `graphicsLayer` scale) DICABUT, diganti
   `style = baseStyle` polos + faktor `graphicsLayer` scale dinaikkan 0.08f→0.23f (magnitude
   visual akhir dipertahankan sama, ≈1.23x lama). Kontribusi ke stutter drag (layout-pass
   berulang) dihapus, 0 perubahan tampilan yang diminta user.
3. Import `animateFloatAsState` dicabut (0 pemakaian lain tersisa di file).

0 file lain disentuh. 0 dependency baru, 0 import baru (`Animatable`/`tween`/`LaunchedEffect`/
`remember` semua sudah ada sejak batch sebelumnya).

**0 diverifikasi CI/device Batch 445** — review manual (baca kode + cek balance brace/paren:
`{}` 321/321, `()` 1014/1014, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Item belum-terverifikasi bertambah 1: real-time drag tab-bar terasa
1:1 mengikuti jari TANPA lag (poin 1), warna ikon/pill berubah seketika sinkron jari selama drag
DAN tetap cross-fade halus untuk tap biasa (poin 2), transisi mulus TANPA lompatan visual pas
jari dilepas (handoff drag→settle) — SEMUA perlu konfirmasi device fisik (0 tersedia sesi ini,
sama seperti Batch 435-444).

**Catatan Batch 444**: user konfirmasi CI Batch 443 hijau, lanjut feedback eksplisit (bukan
reopen sektor DITUTUP manapun, perluasan langsung drag tab-bar Batch 442) — 3 poin dipilih via
opsi tersaring: (1) pill/capsule 0 ikut posisi jari real-time (baru "lompat" pas commit
index-crossing), (2) 0 tahanan visual di ujung kolom (Beranda/Pengaturan), (3) "border tab nav
terluar kebesaran".

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. **Live pill-tracking**: state baru `tabBarDragIndexPx` (posisi kontinu 0f..3f, NaN = 0 drag
   aktif di tab-bar ini — TERPISAH dari `tabDragOffsetPx` milik swipe konten Batch 435/437, beda
   area sentuh, 0 saling pakai), ditulis SINKRON di loop `awaitEachGesture` yang sudah ada (dari
   `x` yang SAMA PERSIS dipakai hitung `newIndex`, 0 hitungan ganda). Fungsi baru
   `tabBarDragFocus(tabIndex)`: NaN → fallback `tabMagnifyFocus` (0 regresi nudge swipe-konten
   lama); aktif → fungsi tenda (jarak posisi kontinu ke titik tengah tiap kolom, 1f di tengah
   turun linear ke 0f di jarak 1 kolom) gantikan `tabMagnifyFocus` di 3 titik pemakaian
   `GlassTabIcon(focus = ...)` — pill kini "hidup" mengikuti jari kontinu SEBELUM index-crossing
   commit, bukan cuma bereaksi sesudahnya.
2. **Tahanan visual ujung kolom**: `tabBarOverscrollPx` (sumber kebenaran sinkron, dibaca
   `graphicsLayer{translationX=...}` di modifier terluar `NavigationBar`) dari `rawX` (posisi
   jari SEBELUM di-coerce ke batas bar) redaman 0.3f + batas ±24px (pola identik
   `tabDragOffsetPx.floatValue = totalTabDrag * 0.3f` yg sudah ada) — kapsul nge-"give" halus
   pas jari didorong lewat ujung Beranda/Pengaturan, springback ke 0 lewat `tabBarOverscrollAnim`
   (`Animatable`, spring dampingRatio/stiffness IDENTIK `tabDragOffset`/`AlbumArtHero`) via
   `tabSwipeScope` (REUSE scope yang sudah ada) — pola *Px-sinkron/Animatable-springback-only
   PERSIS sumbu fix Batch 433/434 (0 coroutine per-delta), non-blocking (awaitEachGesture 0
   nunggu springback selesai sebelum siap terima down berikutnya).
3. **Fix "kebesaran"**: root cause — `windowInsets` default `NavigationBar`
   (`NavigationBarDefaults.windowInsets`) masih mereservasi tinggi system-nav-bar DI DALAM
   kapsul, padahal Batch 439 sudah floating-kan kapsul via margin LUAR (`.padding(bottom=12.dp)`)
   — inset itu jadi DOBEL terhitung (dalam tinggi kapsul + margin luar), bikin kapsul lebih
   tebal dari semestinya. Fix: `windowInsets = WindowInsets(0,0,0,0)` di titik pemakaian INI SAJA
   (bukan ganti default app-wide). Margin luar 12.dp (Batch 439) TETAP jalan sendiri, 0 risiko
   baru ketutup gesture-nav.

0 file lain disentuh. 0 dependency baru, 0 import baru (`Animatable`/`spring`/`Spring`/
`WindowInsets`/`graphicsLayer` semua sudah ada sejak batch sebelumnya).

**0 diverifikasi CI/device Batch 444** — review manual (baca kode + cek balance brace/paren:
`{}` 317/317, `()` 977/977, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Item belum-terverifikasi bertambah 1: live-tracking pill (halus
mengikuti jari lintas kolom, 0 lag/jitter), tahanan ujung Beranda/Pengaturan (terasa "ketahan"
bukan keras/kaku, springback halus), dan kapsul terlihat lebih ramping (bukan lagi "kebesaran")
tanpa closeup ke gesture-nav bar di device asli — SEMUA perlu konfirmasi device fisik (0 tersedia
sesi ini, sama seperti Batch 435-443).

**Catatan Batch 443**: trigger `log_fail_427.zip` — fix Batch 442 (drag langsung di tab bar) GAGAL
compile CI: `Unresolved reference 'awaitFirstDown'` di 2 titik (`MainActivity.kt:187` importnya
sendiri, `:1349` titik pakainya). Root cause: salah paket saat penulisan Batch 442 — `awaitFirstDown`
(extension fun `AwaitPointerEventScope`, dipakai dgn parameter `pass`) sebenarnya dideklarasikan
di `androidx.compose.foundation.gestures` (satu paket persis dgn `awaitEachGesture` yg SUDAH benar
diimport baris atasnya), BUKAN `androidx.compose.ui.input.pointer` (paket itu isinya
`PointerEventPass`/`AwaitPointerEventScope` doang, 0 fungsi util gesture semacam ini) — bukan API
yang berubah/deprecated, murni asumsi paket keliru.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` — 1 baris import diganti:
`androidx.compose.ui.input.pointer.awaitFirstDown` → `androidx.compose.foundation.gestures.awaitFirstDown`.
0 baris lain disentuh, 0 logic/behavior berubah (fix murni resolusi symbol compile-time).

**0 diverifikasi CI/device Batch 443** — review manual (baca kode + cek balance brace/paren:
`{}` 308/308, `()` 924/924, `[]` 3/3 — IDENTIK Batch 442 krn cuma ganti teks path 1 baris import),
0 env Android nyata/device fisik/compiler Kotlin/akses jaringan Gradle di sesi ini. Seluruh item
belum-terverifikasi Batch 442 (label/pill/blur/drag tab bar) TETAP di daftar bawah apa adanya —
fix compile ini TIDAK otomatis mengkonfirmasi behavior runtime-nya, cuma membuka jalan CI hijau.

**Catatan Batch 442**: laporan eksplisit user (screenshot bottom nav bar) — 3 masalah sekaligus:
(1) label "Perpustakaan"/"Pengaturan" terpotong jadi "Perpusta"/"Pengatur", (2) pill "Beranda"
tampak anomali besar, (3) "efek blur useless" di 2 label nonaktif, + permintaan fitur baru
(4) "tambahkan fitur drag pada tab, bukan hanya tap-tab doang". Perluasan langsung sektor nav
bawah yang sama (Batch 301/435/437/438/439/440/441), bukan reopen sektor DITUTUP manapun.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. **Fix (1)+(2), root cause tunggal**: `MagnifyingTabLabel` (Batch 437) baca `LocalTextStyle
   .current` sbg base style — asumsi ini SALAH sejak Batch 439 memindahkannya dari slot `label`
   NavigationBarItem (yg M3 otomatis bungkus `ProvideTextStyle(labelMedium)`) ke slot `icon`
   (`GlassTabIcon`), di mana `LocalTextStyle.current` jatuh balik ke ambient default
   MaterialTheme (bodyLarge, jauh lebih besar) — persis item "belum-terverifikasi" yg sudah
   diperingatkan sendiri di PROJECT_STATE.md sejak penutupan Batch 439 ("x font-scale besar").
   Fix: baca `MaterialTheme.typography.labelMedium` langsung (token M3 resmi, IDENTIK dgn
   default `label` slot) — 0 hardcode sp baru. `overflow = TextOverflow.Ellipsis` ditambah sbg
   jaring pengaman (sebelumnya 0 di-set, default `Clip` yg menghasilkan potongan huruf mentah).
2. **Fix (3)**: `.blur(((1f - clampedFocus) * 1.3f).dp)` di `MagnifyingTabLabel` DICABUT — radius
   idle (tab tidak sedang digeser) = 1.3dp KONSTAN di 2 dari 3 label SETIAP SAAT, bukan cuma
   sesaat selama drag; screenshot user konfirmasi 0 manfaat visual, cuma bikin
   "Perpustakaan"/"Pengaturan" buram permanen. `scaleX`/`scaleY`/`alpha` (`graphicsLayer`, sinyal
   fokus kontinu Batch 437) TETAP jalan — cuma komponen blur yg dicabut. Import
   `LocalTextStyle`/`androidx.compose.ui.draw.blur` ikut dilepas (sudah 0 pemakaian lain).
3. **Fitur baru (4)**: drag LANGSUNG di atas tab bar (bukan cuma di konten layar spt swipe Batch
   435) — gaya segmented-control iOS, tekan 1 tab lalu geser jari TANPA angkat, tab ikut
   berpindah mengikuti posisi jari lintas 3 kolom (equal-width, M3 default). Teknik:
   `Modifier.pointerInput(Unit) { awaitEachGesture { ... } }` di `PointerEventPass.Initial`
   (bukan default `Main`) + 0 `change.consume()` sama sekali — event dibaca SEBELUM child
   `NavigationBarItem` memproses Main pass-nya sendiri, jadi tap polos/ripple-feedback
   (`bouncyPress` Batch 438) 0 terganggu (tap singkat = index hover tidak pernah berubah dari
   titik down = blok navigate() custom ini tidak pernah tereksekusi, murni `onClick` bawaan yg
   menangani). Key `pointerInput` sengaja `Unit` (bukan `currentRoute`) + `rememberUpdatedState
   (currentRoute)` baru (`currentRouteState`) — `NavigationBar` composable ini TIDAK
   keluar-masuk komposisi selama pindah antar 3 tab (kondisi pembungkusnya tetap true), jadi
   coroutine gesture aman hidup terus lintas tab (drag 1 jari lewat >1 batas tab, mis. Beranda
   langsung ke Pengaturan, tidak macet di tab tengah) — kalau di-key `currentRoute` malah restart
   tiap 1 batas terlewati krn `navigate()` mengubah key itu sendiri di tengah gesture yg sama.
   `navController.navigate` pakai opsi IDENTIK popUpTo/launchSingleTop/restoreState (pola Batch
   301/435), 0 state-preservation baru. Haptic tick per tab berpindah REUSE `tabSwipeHaptic`
   (Batch 435, `HapticFeedbackType.LongPress`), 0 API haptic baru. Swipe konten Batch 435 (Box
   pembungkus NavHost) TIDAK disentuh — 2 mekanisme drag independen, beda area sentuh.

`NavigationRailItem` (tablet) TIDAK disentuh — di luar scope (sama seperti Batch 437-441).

**0 diverifikasi CI/device Batch 442** — review manual (baca kode + cek balance brace/paren:
`{}` 308/308, `()` 924/924, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Perlu ditest device asli: label "Perpustakaan"/"Pengaturan" tidak
lagi terpotong di ukuran font default MAUPUN font-scale aksesibilitas besar, pill "Beranda"
proporsional (bukan lagi anomali besar), 0 blur tersisa di 2 label nonaktif, dan drag jari
lintas tab bar berpindah tab dgn benar (termasuk drag cepat lintas >1 batas tab) TANPA
mengganggu tap biasa/ripple-feedback yang sudah ada. Item belum-terverifikasi bertambah 1.

**Catatan Batch 441**: trigger `log_fail_425.zip` — fix Batch 440 (`IndicationNodeFactory`)
ternyata belum lengkap: interface itu me-re-abstract `equals`/`hashCode` (deklarasi ulang
eksplisit, bukan cuma warisan default `Any`), jadi `object NoRippleIndication` WAJIB
mengimplementasi keduanya eksplisit — 0 diketahui saat migrasi Batch 440 (bukan bagian pesan
error compile SEBELUMNYA, baru muncul SETELAH kontrak lamanya diganti). Perluasan langsung fix
compile Batch 440, sektor sama (nav bawah, Batch 301/435/437/438/439/440).

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` — `NoRippleIndication`
ditambah `override fun equals(other: Any?): Boolean = other === this` +
`override fun hashCode(): Int = -1`. Identity check sederhana cukup (1 instance singleton
sepanjang hidup app, 0 state pembeda) — bukan logic baru, murni memenuhi kontrak interface.
0 file lain disentuh.

**0 diverifikasi CI/device Batch 441** — review manual (baca kode + cek balance brace/paren:
`{}` 300/300, `()` 865/865, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Fix ke-2 berturut-turut utk kontrak `IndicationNodeFactory` yang
sama (Batch 440 lalu ini) — BELUM dikonfirmasi CI hijau nyata, run berikutnya WAJIB dicek utuh
(bukan cuma diasumsikan beres krn pesan error sebelumnya sudah hilang dari log). Item
belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 440**: trigger ganda dari user — (1) `log_fail_424.zip`, `compileDebugKotlin`/
`compileReleaseKotlin` FAILED di CI (`e:` bukan `w:` — level deprecation `Indication`/
`IndicationInstance` yang dipakai `NoRippleIndication` Batch 439 sudah naik jadi HARD ERROR di
compose-bom 2026.04.01, bukan lagi cuma warning); (2) re-lampiran panduan
`drag_drop_glass_ios_kotlin.md` + instruksi eksplisit "ubah behavior sesuai source code
lampiran, adaptasi bukan timpa plek ketiplek". Perluasan langsung sektor nav bawah yang sama
(Batch 301/435/437/438/439), bukan reopen sektor DITUTUP manapun.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. **Fix compile**: `NoRippleIndication` dimigrasi dari kontrak lama `Indication`/
   `IndicationInstance` (`rememberUpdatedInstance`) ke kontrak resmi pengganti
   `IndicationNodeFactory` + `Modifier.Node`/`DrawModifierNode` (`create()`/`ContentDrawScope.draw()`).
   0 behavior berubah — masih murni `drawContent()` kosong, 0 layer visual, titik pemakaian
   `CompositionLocalProvider(LocalIndication provides NoRippleIndication)` di `bottomBar` TIDAK
   disentuh (`IndicationNodeFactory` = subtipe `Indication`, tetap kompatibel).
2. **Adaptasi behavior guide**: 2 elemen guide (`lerp` posisi/opacity kapsul & warna ikon
   mengikuti persentase geser jari `HorizontalPager` secara langsung) diadaptasi ke arsitektur
   riil (permanent NavHost routes, BUKAN HorizontalPager — swap ke pager tetap ditolak sejak
   Batch 435/438 dgn alasan sama: breaking ke state-restoration/NavigationRail tablet). App ini
   sudah punya padanan persis `pageOffsetFraction` guide sejak Batch 435/437: `focus`
   (`tabMagnifyFocus`, live tiap frame drag). Target `glassAlpha` (`GlassTabIcon`) diganti dari
   `if (selected) 1f else 0f` (statis, cuma reaksi post-commit) jadi `focus` langsung — idle
   value SAMA PERSIS 1f/0f (0 regresi tap, tween 220ms Batch 439 tetap jalan), bedanya kini
   pill JUGA bereaksi kontinu selama drag berlangsung. Ikon sendiri (elemen guide yg belum
   pernah diadaptasi batch manapun) kini ikut `lerp` warna kontinu persis teknik guide
   (`androidx.compose.ui.graphics.lerp`) dari `NavigationBarItemDefaults.colors().unselectedIconColor`
   (token M3 resmi, 0 hardcode warna baru) ke `tint` (primary, aksen sama dgn pill) — ikon & pill
   kini 1 aksen bergerak bersama. Skeu DIKECUALIKAN dari lerp ikon (aturan solid Batch 58/61/79),
   tetap tint default M3 apa adanya. Reorder drag-to-swap & `HorizontalPager` literal dari guide
   TETAP tidak dipakai (rasionalisasi sama persis Batch 438, tidak diulang di sini).

`NavigationRailItem` (tablet) TIDAK disentuh — di luar scope (sama seperti Batch 437/438/439).
Detail penuh: `CHANGELOG.md` § Batch 440.

**0 diverifikasi CI/device Batch 440** — review manual (baca kode + cek balance brace/paren:
`{}` 300/300, `()` 861/861, `[]` 3/3), tidak ada env Android nyata/device fisik/compiler
Kotlin/akses jaringan Gradle di sesi ini — fix compile berbasis pembacaan API resmi
`IndicationNodeFactory`/`DrawModifierNode` (stabil sejak Compose UI 1.6+, konsisten dgn
compose-bom 2026.04.01 project ini), BELUM dikonfirmasi CI hijau nyata. Item belum-terverifikasi
bertambah 1 (lihat daftar di bawah).

**Catatan Batch 439**: permintaan eksplisit user — 2 screenshot referensi (nav app ini vs tab bar
iOS Jam/Clock), "perbaiki bottom nav bar agar lebih mirip gaya visual iOS app jam tersebut,
matikan ripple khas Android saat klik". Perluasan langsung dari sektor nav bawah yang sama
(Batch 301/435/437/438), bukan reopen sektor DITUTUP manapun.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. `GlassTabIcon` (Batch 438) diperluas ambil alih slot label (`label`/`focus` param baru,
   memanggil `MagnifyingTabLabel` Batch 437 yang sama persis) supaya highlight pill tab aktif
   membungkus IKON+LABEL sekaligus jadi 1 blok (dulu cuma bungkus ikon) — meniru referensi iOS
   Jam. Bentuk pill jadi `RoundedCornerShape(16.dp)` (dari stadium penuh `percent = 50`, yang
   di tinggi baru ini akan terlihat kapsul obat, bukan kotak rounded seperti referensi).
2. `NavigationBar` bawah kini kapsul mengambang (`.padding(horizontal 16.dp, bottom 12.dp)` LALU
   `.clip(RoundedCornerShape(28.dp))`, urutan modifier ini krusial) alih-alih persegi nempel edge-
   to-edge — meniru referensi iOS Jam. `windowInsets` bawaan (gesture-nav) tidak disentuh, margin
   ini tambahan di atasnya.
3. Ripple Android bawaan di 3 `NavigationBarItem` dimatikan lewat `Indication` kosong baru
   (`NoRippleIndication`, cuma `drawContent()`) dipasang via `CompositionLocalProvider(LocalIndication
   provides ...)` yang MEMBUNGKUS 3 `NavigationBarItem` — bukan `Modifier.clickable` baru, 0
   sentuh `selected`/`onClick`/route logic. `bouncyPress` (scale-down tekan, Batch 438) TETAP
   jalan sebagai feedback tekan pengganti.

`NavigationRailItem` (tablet/foldable) TIDAK disentuh — 2 screenshot referensi user keduanya nav
ponsel, di luar scope. Detail penuh + rasionalisasi: `CHANGELOG.md` § Batch 439.

**0 diverifikasi CI/device Batch 439** — review manual (baca kode + cek balance brace/paren:
`{}` 298/298, `()` 833/833, `[]` 3/3), tidak ada env Android nyata/device fisik/compiler
Kotlin/akses jaringan Gradle di sesi ini. Item belum-terverifikasi bertambah 1 (lihat daftar di
bawah).

**Catatan Batch 438**: permintaan eksplisit user — lampiran `drag_drop_glass_ios_kotlin.md` +
screenshot bottom nav, "hasil sebelumnya (Batch 437, efek kaca PEMBESAR di label) mengecewakan,
adaptasi 100% berdasarkan panduan". Perluasan langsung dari sektor nav bawah yang sama (Batch
301/435/437), bukan reopen sektor DITUTUP manapun.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` — composable baru
`GlassTabIcon(icon, selected, interactionSource)` jadi pill indicator translucent (tint 16%
alpha + border 14% alpha, `RoundedCornerShape(percent = 50)`, animasi cross-fade `tween(220)`
mengikuti `selected`) menggantikan indicator flat default M3 di belakang ikon 3 tab bawah,
plus `bouncyPress()` (konvensi tekan-tactile existing) untuk scale-down halus saat ditekan.
Detail penuh + rasionalisasi kenapa 2 elemen panduan asli (reorder drag-to-swap tab, dan
`Modifier.blur(20.dp)` literal di container) SENGAJA tidak dipakai 1:1 — bukan penolakan,
adaptasi ke arsitektur riil (route nav permanen + blur asli Haze sudah dimatikan permanen Batch
329 + `frostedGlass()` existing didesain utk panel besar bukan pill sekecil ini): `CHANGELOG.md`
§ Batch 438. `isSkeuTheme()` dikecualikan (aturan "solid, bukan kaca" Batch 58/61/79, app-wide).
Catatan desain lama Batch 53 ("§15 jangan jadikan item nav jadi glowing glass capsule") SECARA
EKSPLISIT disupersede oleh instruksi user batch ini utk 5 identitas non-Skeu — kaskade
DESCENDING TRUTH: instruksi eksplisit baru > catatan/spec lama, dicatat di sini + README.md
(bukan dihapus diam-diam dari histori). `MagnifyingTabLabel`/`tabMagnifyFocus` (Batch 437) TIDAK
dihapus — 2 efek (kaca ikon + pembesar label) jalan berdampingan.

**0 diverifikasi CI/device Batch 438** — review manual (baca kode + cek balance brace/paren:
`{}` 293/293, `()` 792/792, `[]` 3/3), tidak ada env Android nyata/device fisik/compiler Kotlin
di sesi ini. Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 437**: permintaan FITUR BARU eksplisit user (lampiran screenshot bottom nav) —
efek "kaca pembesar ala iOS" di LABEL 3 tab bawah (Beranda/Perpustakaan/Pengaturan), bereaksi
tergantung "kaca diarahkan kesitu/bukan". Bukan reopen sektor DITUTUP manapun — perluasan
langsung dari fitur swipe Batch 435 (sektor sama, belum pernah ditutup).

**1 file diubah** (dalam batas 3 file/tugas):
1. `MainActivity.kt` (`AppNavHost`) — 0 gesture/state baru: `tabMagnifyFocus(tabIndex)` (fungsi
   lokal baru) murni MEMBACA ULANG `tabDragOffsetPx` (`MutableFloatState` Batch 435, ±40px,
   sudah live tiap frame `onHorizontalDrag` + sudah spring-back ke 0 di `onDragEnd`/
   `onDragCancel`) sebagai bobot fokus 0f..1f per tab — tab yang sedang aktif mulai dari fokus
   1f dan turun mengikuti `max(towardNext, towardPrev)` selama drag, tab tetangga yang dituju
   naik dari 0f ke arah 1f secara kontinu (BUKAN snap di ujung threshold 120px) — persis efek
   lensa bergeser dari 1 label ke label sebelah selama jari masih menekan.
   Label `NavigationBarItem` (`Text("Beranda")` polos dkk) diganti composable baru
   `MagnifyingTabLabel(text, focus)`: `fontSize` discale kontinu dari `LocalTextStyle.current`
   (bukan angka sp hardcode — ikut style/tema label bawaan apa pun yang aktif), plus
   `graphicsLayer{scaleX/scaleY/alpha}` + `Modifier.blur()` (aman tanpa cek `Build.VERSION`,
   minSdk project ini 31 = RenderEffect selalu tersedia) — tab fokus penuh jadi sedikit lebih
   besar/tajam/terang, tab non-fokus mengecil/buram/redup sebagian, transisi mengikuti jari
   frame-demi-frame. HANYA `NavigationBar` bawah (layout ponsel COMPACT, sesuai screenshot user)
   yang disentuh — `NavigationRailItem` (tablet/foldable Medium/Expanded) SENGAJA tidak ikut
   diubah, di luar scope diminta (screenshot user = bottom bar ponsel), 0 side-quest. Dibaca di
   titik pemakaian (dalam tiap `label = { ... }`, bukan di-hoist ke `NavigationBar`) supaya scope
   recomposition sekecil mungkin (hanya `Text` label yang recompose tiap frame drag, bukan
   seluruh bar) — pola read-state-di-leaf yang sama dipakai `BlurUtils.kt`/`IosScrollPhysics.kt`.
   0 breaking change: `selected`/`onClick`/`icon`/route logic Batch 301/435 tidak disentuh sama
   sekali, warna label selected/unselected tetap 100% dari `LocalContentColor` bawaan M3 (tidak
   di-override). Detail lengkap: `CHANGELOG.md` § Batch 437.

**0 diverifikasi CI/device Batch 437** — review manual (baca kode + cek balance brace/paren:
`{}` 286/286, `()` 734/734, `[]` 3/3), tidak ada env Android nyata/device fisik di sesi ini.
Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 436**: user laporan "abis update saya nunggu buffer screen lumayan ±20s",
menduga terkait fitur swipe Batch 435. Investigasi (grep, bukan asumsi) — **0 file diubah**:
1. Diff Batch 435 (`AppNavHost`) di-baca ulang penuh: isinya murni `pointerInput`/
   `detectHorizontalDragGestures` + `graphicsLayer` (kerja UI-thread, non-blocking, 0 I/O, 0
   panggilan network/disk baru). Tidak mungkin jadi sumber jeda 20 detik secara struktural.
2. Root cause sesungguhnya (kode sudah ada SEBELUM Batch 435, tidak disentuh): `ensureLibraryLoaded()`
   → `refreshLibrary()` (`PlayerViewModel.kt`) jalan di **setiap cold start proses** (flag
   `libraryLoadedOnce` in-memory, bukan persisted) — dikonfirmasi lewat komentar existing Batch
   419 sendiri: **"jalur paling panas cold-start"**. Scan `musicRepository.getAllSongs()` +
   (kalau ada) SAF custom folder via Binder/IPC per folder (`customFolderScanner.scan()`) jalan
   di `Dispatchers.IO` — sudah benar secara threading (non-blocking Main), tapi durasi wall-clock
   scan MediaStore tetap naik seiring ukuran library/jumlah folder custom, TIDAK instan. Install
   APK baru = proses baru = scan ini trigger ulang dari nol — persis skenario "abis update".
3. Kesimpulan: **bukan regresi Batch 435**. Perilaku ini sudah ada sebelum swipe gesture ditambah,
   ter-dokumentasi sendiri di README § fitur ("Shimmer skeleton loading" selama fase ini). Tidak
   ada perubahan kode.

**0 diverifikasi CI/device Batch 436** — kesimpulan murni dari pembacaan kode (grep + baca
`PlayerViewModel.kt`/`MusicRepository.kt`), tidak ada env Android nyata/device fisik di sesi ini.

**Catatan Batch 435**: permintaan FITUR BARU eksplisit user — "tambahkan gesture swipe able
lintas 3 tab. alih-alih user hanya bisa tap-tab manual berulang!!" (Beranda/Perpustakaan/
Pengaturan). Bukan reopen sektor DITUTUP manapun (lihat daftar "Sektor DITUTUP" di bawah) —
sektor terpisah, navigasi tab bawah belum pernah masuk 3 sektor yang ditutup itu.

**1 file diubah** (dalam batas 3 file/tugas):
1. `MainActivity.kt` (`AppNavHost`) — app ini pakai Jetpack Navigation Compose dengan 3 route
   top-level TERPISAH (`"home"`/`"library"`/`"settings"`, BUKAN `HorizontalPager` 1-route) —
   swap ke arsitektur pager penuh ditolak sebagai solusi (butuh restrukturisasi NavHost +
   `now_playing`/`stats_dashboard` jadi push-di-atas-pager, risiko regresi jauh lebih besar dari
   scope diminta). Pendekatan dipilih: `Modifier.pointerInput` + `detectHorizontalDragGestures`
   dipasang di `Box` pembungkus `NavHost`, HANYA aktif saat `currentRoute` ada di 3 tab itu
   (`TAB_ROUTES`, konstanta baru) — di `"now_playing"`/`"stats_dashboard"` modifier ini tidak
   terpasang sama sekali (0 rebutan dgn `detectHorizontalDragGestures` `AlbumArtHero` yang sudah
   ada di `now_playing`, Batch 434). Saat threshold ±120px terlampaui, swipe memicu
   `navController.navigate()` dengan opsi IDENTIK ke `onClick` `NavigationBarItem`/
   `NavigationRailItem` yang sudah ada (`popUpTo("home"){saveState=true}` + `launchSingleTop` +
   `restoreState`, pola Batch 301) — 0 state baru di sisi state-preservation, murni trigger
   berbeda (gesture, bukan cuma tap). `enterTransition`/`exitTransition` `NavHost` (fade
   200/150ms, Batch 330) TIDAK disentuh — dipakai apa adanya baik utk tap maupun swipe.

   Pola threshold 120px + haptic (`HapticFeedbackType.LongPress`) + `dragOffsetPx`
   (`MutableFloatState`, sinkron)/`Animatable` (springback-only, via `spring(DampingRatioMediumBouncy,
   StiffnessLow)`) REUSE 1:1 dari `AlbumArtHero` (`NowPlayingScreen.kt`, Batch 434) — sengaja
   tidak reinvent, termasuk `onDragStart` yang panggil `dragOffset.stop()` (jaring pengaman
   sinkron-vs-asinkron yang sama). Beda dari `AlbumArtHero`: nudge visual (`graphicsLayer
   translationX`) di sini dibatasi lebih kecil (±40px, multiplier 0.3, bukan ±48dp/0.5) karena
   yang digeser konten SATU LAYAR PENUH (bukan 1 kartu album), dan page-swap sesungguhnya tetap
   lewat fade `NavHost` yang sudah ada — nudge ini murni sinyal "tergenggam", bukan preview
   halaman berikutnya. `TAB_ROUTES.any{it==currentRoute}`/`indexOfFirst{it==currentRoute}`
   dipakai (bukan `in`/`indexOf` langsung) karena `currentRoute` bertipe `String?` sedangkan
   `List<String>.contains`/`indexOf` mengharap parameter non-null — perbandingan `==` selalu
   type-safe utk operand nullable, `in`/`indexOf` langsung berisiko unresolved/type-mismatch.
   0 breaking change ke `NavigationBarItem`/`NavigationRailItem`/route lain (signature/pola tap
   lama tidak disentuh sama sekali).

**0 diverifikasi CI/device Batch 435** — review manual (baca kode + cek balance brace/paren:
`{}` 280/280, `()` 700/700, `[]` 3/3), tidak ada env Android nyata/device fisik di sesi ini.
Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 434**: reopen eksplisit user — laporan spesifik "effect bounce juga masih
stuttering, belum smooth like butter!!". SAMA KELAS BUG dgn Batch 433 (`IosScrollPhysics.kt`),
tapi di file BEDA: `ui/NowPlayingScreen.kt` → `AlbumArtHero` (swipe horizontal next/prev pada
album art) — ditemukan lewat grep `bounce`/`spring(` menyeluruh ke seluruh `app/src/main/java`
(bukan tebakan single-file), setelah `IosScrollPhysics.kt` sendiri dikonfirmasi baca-kode sudah
bersih dari sumbu bug ini (drag sinkron via `dragOffset` sejak Batch 433, tidak disentuh lagi).

**1 file diubah** (dalam batas 3 file/tugas):
1. `ui/NowPlayingScreen.kt` (`AlbumArtHero`) — root cause identik Batch 433: `onHorizontalDrag`
   menulis posisi lewat `dragScope.launch { dragOffset.snapTo(...) }` di SETIAP delta drag —
   coroutine baru per delta, bisa menumpuk/tidak berurutan saat drag cepat. Fix: `dragOffsetPx`
   (`MutableFloatState` polos, via `mutableFloatStateOf`) jadi sumber kebenaran SINKRON yang
   dibaca `graphicsLayer` (ditulis LANGSUNG dari `onHorizontalDrag`, 0 coroutine). `dragOffset`
   (`Animatable`) tetap ada, sekarang HANYA dipakai di fase springback (`onDragEnd`/
   `onDragCancel`) — `snapTo` posisi drag terakhir dulu, tiap frame `animateTo` disinkronkan
   balik ke `dragOffsetPx` lewat parameter `block` resmi. Tambahan (gap yang tidak muncul di
   Batch 433 krn kasusnya scroll/fling bawaan `scrollable()`, bukan drag-gesture manual):
   `onDragStart` sekarang panggil `dragOffset.stop()` — jaring pengaman springback-lama-vs-
   drag-baru, supaya `block` lama berhenti menimpa `dragOffsetPx` kalau user mulai drag baru
   sebelum springback sebelumnya selesai. `totalDrag`/threshold swipe-next/prev 120px/haptic/
   `dampingRatio`/`stiffness` (Batch 256) TIDAK disentuh — sumbu bug ini murni SINKRON vs
   ASINKRON penulisan offset. Detail lengkap: `CHANGELOG.md` § Batch 434.

**0 diverifikasi CI/device Batch 434** — review manual (baca kode + cek balance brace/paren:
`{}` 292/292, `()` 1287/1287, `[]` 1/1), tidak ada env Android nyata/device fisik di sesi ini.
Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 433**: reopen eksplisit user — laporan spesifik "effect scrolling/transition like
iOS masih terasa stuttering gak halus sama sekali". Sumbu BARU, beda dari seluruh histori tuning
`ui/theme/IosScrollPhysics.kt` Batch 364-383 (semuanya soal KARAKTER pegas — stiffness/
dampingRatio/rubberBand, sudah dikonfirmasi user via banyak iterasi) — "stuttering" = gejala frame
drop/jank, bukan parameter animasi mana yang dipakai.

**1 file diubah** (dalam batas 3 file/tugas):
1. `ui/theme/IosScrollPhysics.kt` — root cause: `applyToScroll` (kontrak resmi non-suspend, justru
   supaya overscroll bisa diterapkan sinkron dalam frame sentuhan yang sama) sebelumnya menulis
   posisi lewat `coroutineScope.launch { overscrollOffset.snapTo(...) }` di SETIAP event scroll
   delta selama drag di zona overscroll — tiap delta bikin coroutine baru krn `Animatable.snapTo`
   cuma suspend, dan `launch` menambah giliran dispatcher yang bisa menumpuk/tidak berurutan saat
   drag cepat = persis gejala stutter yang dilaporkan. Fix: state baru `dragOffset`
   (`MutableState<Offset>` polos) jadi sumber kebenaran SINKRON yang ditulis LANGSUNG (0 coroutine)
   dari `applyToScroll`, pola sama `Modifier.pointerInput { detectDragGestures { ... } }` standar
   Compose. `overscrollOffset` (`Animatable`) tetap ada, sekarang HANYA dipakai internal di fase
   settle (`settleToZero`, sudah suspend by design) — tiap frame animasinya disinkron balik ke
   `dragOffset` lewat parameter `block` resmi `Animatable.animateTo`. `measure()` baca `dragOffset`
   (bukan lagi `overscrollOffset` langsung). `dampingRatio`/`stiffness`/`rubberBandResistance`
   (semua tuning Batch 368-383) TIDAK disentuh — sumbu bug ini murni soal SINKRON vs ASINKRON-nya
   penulisan offset, bukan parameter pegasnya. Detail lengkap: `CHANGELOG.md` § Batch 433.

**0 diverifikasi CI/device Batch 433** — review manual (baca kode + cek balance brace/paren), tidak
ada env Android nyata/device fisik di sesi ini. Item belum-terverifikasi bertambah 1 (lihat daftar
di bawah).

**Catatan Batch 425–430**: user secara eksplisit reopen **satu kali khusus** untuk Coil migration
(bump 2.6.0→3.x, 3 file + `build.gradle.kts`), lalu reopen KEDUA secara terpisah eksplisit untuk
sektor Compose optimization (`AlbumArt`, Batch 429) — bukan pencabutan status permanen. Versi
final Coil: **3.3.0**, HIJAU CI. `AlbumArt` AsyncImage swap — user konfirmasi device asli render
NORMAL, 0 regresi visual (Batch 430). Kedua sektor TUNTAS & terverifikasi penuh (CI + device).
Detail teknis lengkap: `CHANGELOG.md` § Batch 425–430.

**Catatan Batch 431**: reopen ketiga, eksplisit user minta audit menyeluruh atas seluruh sektor
"belum terjamah" (di luar `ui/`), KECUALI paket `update/` (`GitHubReleaseChecker.kt`,
`UpdateDownloader.kt`, `UpdateManager.kt` — waktu itu masih tertutup, kini disisir Batch 432
di bawah).

**Catatan Batch 432**: reopen eksplisit user untuk audit paket `update/` (3 file, belum pernah
disisir sebelumnya). Hasil:
- `GitHubReleaseChecker.kt` — `fetchLatest()` pakai `.execute()` blocking sengaja (bukan
  `suspend`), tapi HANYA dipanggil dari dalam `UpdateManager.scope.launch` (Dispatchers.IO) —
  tidak pernah jalan di Main thread. Komentar file menjelaskan `.string()` di sini aman karena
  cuma JSON kecil (beda dari APK binary di `UpdateDownloader`). Tidak diubah.
- `UpdateDownloader.kt` — `download()` streaming 8 KB per chunk ke disk (`Buffer`/`sink()`),
  TIDAK pernah `readBytes()`/`.string()` pada body APK — sesuai Safety Locks SOP. Dipanggil dari
  background thread oleh caller. Tidak diubah.
- `UpdateManager.kt` — **1 file diubah**. Gap nyata: `checkForUpdate()` &
  `downloadAndPrepareInstall()` jalan di `Thread {}` mentah, bukan Coroutines — melanggar SOP
  §2 Thread Safety ("WAJIB Coroutines"), dan Thread lepas tidak bisa dibatalkan kalau proses
  butuh cleanup. Fix: scope baru `CoroutineScope(SupervisorJob() + Dispatchers.IO)` milik
  singleton ini, `Thread { ... }.start()` → `scope.launch { ... }`. `kotlinx.coroutines` sudah
  jadi dependency existing (dipakai `FloatingBubbleService.kt` & lainnya) — 0 dependency baru.
  API publik (`checkForUpdate`, `downloadAndPrepareInstall`, `launchInstall`, `reset`, `state`)
  tidak berubah, 0 breaking change ke `UpdateCheckSheet.kt`.

**0 diverifikasi CI/device Batch 432** — review manual (baca kode + cek balance brace/paren),
sama seperti Batch 431. Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**2 file diubah** (dalam batas 3 file/tugas):
1. `ui/DiagnosticLogSheet.kt` — 3 titik panggilan `AppLogger.readLog()`/`exportLogToDocuments()`/
   `clearLog()` sebelumnya jalan LANGSUNG di Main thread (initial value `remember` + 2x `onClick`),
   0 coroutine wrapper. Gap ini LOLOS dari audit literal-grep `ui/` sebelumnya karena syntax I/O
   asli (`FileInputStream`, `readText()`, dst) hidup di `util/AppLogger.kt` — paket lain, bukan di
   file `ui/` itu sendiri — bukan false positif, tapi kelas gap yang memang belum pernah disisir:
   call site fungsi lintas-paket yang blocking. Fix: `rememberCoroutineScope()` +
   `Dispatchers.IO`/`withContext(Dispatchers.Main)`, pola identik `SignatureMatcherSheet.kt`
   (Batch 421)/`BackupRestoreSheet.kt`.
2. `ui/DuplicateFinderSheet.kt` — item Compose-perf yang sudah tercatat (lihat riwayat rule di
   bawah): `DuplicateDetector.findLibraryDuplicates`/`findPhysicalDuplicates` (groupBy +
   sortedByDescending atas seluruh `songs`) sebelumnya jalan synchronous di dalam `remember` =
   bagian dari composition phase (Main thread). Fix: `LaunchedEffect(songs)` +
   `Dispatchers.Default` (CPU-bound, bukan I/O), state `isScanning` baru buat loading indicator
   supaya tidak salah tampil "0 duplikat ditemukan" sebelum hasil async selesai.

**Cakupan audit** (grep pola blocking I/O — `commit()`, `Thread.sleep`, `HttpURLConnection`/
`.execute()`, `FileInputStream`/`FileOutputStream`/`open{Input,Output}Stream`, `readBytes`/
`readText`/`writeText`/`writeBytes`, `runBlocking`, `BitmapFactory`, `MediaMetadataRetriever`,
`listFiles`/`walk`, `contentResolver.query` — di seluruh 70 file Kotlin di luar `ui/` & `update/`):
selain 2 fix di atas, SISANYA sudah benar (konfirmasi baca kode, bukan asumsi) —
`ApkSignatureChecker.inspect()` (Batch 421), `TagEditor`/`RingtoneEncoder` (viewModelScope.launch
Dispatchers.IO di `PlayerViewModel.kt`), `BackupManager` (scope.launch Dispatchers.IO di
`BackupRestoreSheet.kt`), `CustomFolderScanner.scan()` (withContext Dispatchers.IO, Batch 386/418),
`LyricsApi.getLyrics` (`suspend fun` + Retrofit, bukan `.execute()` mentah) — semua sudah
terbungkus dispatcher yang benar sejak batch-batch sebelumnya. `AppLogger.writePublicCrashLog()`
sengaja TETAP synchronous (dipanggil dari uncaught-exception-handler saat proses bisa mati kapan
saja — mendispatch ke thread lain di titik ini tidak aman) — bukan bug, tidak diubah.

**0 diverifikasi CI/device sesi ini** (tidak ada compiler/device fisik tersedia) — 2 fix di atas
murni review manual (baca kode + cross-reference pola batch sebelumnya + cek balance
brace/paren). Item belum-terverifikasi bertambah 2 (lihat daftar di bawah).

**Item belum-terverifikasi saat penutupan** (device fisik tidak pernah tersedia di sesi kerja):
- `MainActivity.kt` labelColor kontinu + easing post-release label (Batch 447, di atas) — 0
  compile log, 0 konfirmasi device. Perlu ditest: teks label berubah warna BERSAMAAN dgn ikon
  (bukan menyusul-lompat) selama drag pelan/parsial (belum commit index-crossing), dan
  scale/opacity label tidak snap instan pasca lepas jari (ikut easing 220ms spt ikon).
- `MainActivity.kt` fix label terpotong/pill oversized + drag-on-tab-bar baru (Batch 442, di
  atas) — 0 compile log, 0 konfirmasi device. Perlu ditest: label 3 tab tidak terpotong di
  ukuran default MAUPUN font-scale aksesibilitas besar, pill "Beranda" proporsional, 0 blur
  tersisa, dan drag jari lintas tab bar (termasuk lintas >1 batas tab dalam 1 drag) berpindah
  tab dgn benar tanpa mengganggu tap/ripple-feedback biasa.
- `MainActivity.kt` kapsul mengambang + pill gabungan ikon+label + ripple mati (Batch 439, di
  atas) — 0 compile log, 0 konfirmasi device. **[Update Batch 442]** sub-item overflow/oversized
  text SUDAH ditemukan+fix (root cause: `LocalTextStyle.current` salah baca style di slot
  `icon`, lihat Batch 442 di atas) — dihapus dari daftar perlu-test di sini, gantinya lihat item
  Batch 442 di atas. 2 sub-item SISA (belum tersentuh batch mana pun): kapsul bawah tidak
  ketutup gesture-nav bar di device asli (margin 12.dp bawah cukup?), dan 0 ripple sama sekali
  terasa saat tap ketiga tab di 5 identitas tema non-Skeu + Skeu.
- `MainActivity.kt` pill indicator glass ikon tab bawah (Batch 438, di atas) — 0 compile log, 0
  konfirmasi device. Perlu ditest: transisi cross-fade pill saat pindah tab (halus, bukan
  patah), kontras pill translucent tetap terbaca di 5 identitas non-Skeu (Apple/Tactile/Liquid
  Glass/Aurora/Calm Retro) x mode terang/gelap, scale-down `bouncyPress` saat tap terasa wajar
  (bukan berlebihan), dan pill Skeu tetap solid 100% (0 kebocoran efek glass ke identitas ini).
- `MainActivity.kt` efek kaca-pembesar label tab bawah (Batch 437, di atas) — 0 compile log, 0
  konfirmasi device. **[Update Batch 442]** komponen `.blur()` DICABUT (screenshot user
  konfirmasi 0 manfaat, cuma bikin buram permanen 2 label nonaktif) — item test blur DIHAPUS.
  Sisa perlu ditest (scale/opacity kontinu, TETAP jalan): drag pelan (fokus label bergeser mulus
  tab-ke-tab, bukan patah-patah), drag cepat lalu lepas sebelum threshold (springback fokus
  kembali ke tab asal mulus), drag di tab ujung (Beranda/Pengaturan, tidak crash walau tidak ada
  tab tujuan), dan 0 frame-drop/jank tambahan saat 3 label render bersamaan.
- `MainActivity.kt` swipe-lintas-3-tab (Batch 435, di atas) — 0 compile log, 0 konfirmasi device.
  Perlu ditest: swipe kiri/kanan di Beranda/Perpustakaan/Pengaturan (compact & rail/tablet),
  swipe di tab ujung (Beranda/Pengaturan) tidak nyasar/crash, swipe pendek (di bawah threshold)
  snapback mulus, dan gesture TIDAK kepicu sama sekali di `now_playing`/`stats_dashboard`.
- `ui/NowPlayingScreen.kt` `AlbumArtHero` (Batch 434, di atas) — 0 compile log, 0 konfirmasi
  device. Perlu ditest: swipe cepat berulang next/prev, springback di dragEnd/dragCancel, dan
  drag baru yang menyusul cepat sebelum springback lama selesai (skenario baru yang dijaga
  `dragOffset.stop()`).
- `ui/theme/IosScrollPhysics.kt` (Batch 433, di atas) — 0 compile log, 0 konfirmasi device. Perlu
  ditest: drag cepat berulang di list panjang (stutter hilang?), transisi antar layar, dan flow
  settle (lepas jari di tengah overscroll) tetap 0 regresi ke karakter pegas Batch 368-383 yang
  sudah disetujui user.
- `ui/DiagnosticLogSheet.kt` & `ui/DuplicateFinderSheet.kt` (Batch 431, di atas) — 0 compile log,
  0 konfirmasi device.
- `update/UpdateManager.kt` (Batch 432, di atas) — 0 compile log, 0 konfirmasi device (Thread →
  Coroutines migration, flow "Cek Update" perlu ditest ulang: check, download, install).
- `docs/archive/MANUAL_QA_CHECKLIST.md` — 0/19 item tercentang (audio focus, Bluetooth, lock-screen,
  headset kabel, process death, background playback jangka panjang).
- Overscroll bounce (`IosScrollPhysics.kt`, `Spring.DampingRatioNoBouncy`) belum dikonfirmasi
  device asli.
- `docs/archive/ROADMAP_LIQUID_GLASS_REDESIGN.md` & fling behavior 14/14 layar — **sudah final CLOSED**,
  bukan item terbuka.

## Aturan sesi aktif
1. Dilarang edit manual `versionCode`/`versionName` di `app/build.gradle.kts` — auto dari jumlah
   commit git. Tiap kirim ZIP wajib sebut nomor batch + ingatkan versionName pasti baru setelah
   `git push`.
2. Box code pesan commit WAJIB tampil di atas heading "Update Harian" tiap respons chat, isi
   penjelasan fitur singkat dari `CHANGELOG.md` batch itu — dilarang angka versi polos saja.
3. Prioritas versi/dependency/komponen paling mutakhir, bukan kompatibilitas OS lama — user tidak
   peduli dukungan Android <12/API 31. Jangan bikin/pertahankan fallback legacy kalau ada opsi
   modern lebih bersih. `minSdk` tidak pernah diubah otomatis — WAJIB konfirmasi eksplisit user.
4. `docs/archive/ARCHIVED_POLISH_AUDIT.md` / `docs/archive/ARCHIVED_MICRO_UIUX_AUDIT.md` = arsip, tidak aktif diikuti.
   `docs/archive/ROADMAP_LIQUID_GLASS_REDESIGN.md` = 100% tuntas, tidak ada item terbuka.
   `docs/QA_CHECKLIST_SONIX_v488.md` (Batch 489) BUKAN arsip — checklist QA eksternal AKTIF,
   3 gap-nya (previous-3-detik, filter audio pendek, shuffle anti-repeat-nearby) masih di
   roadmap terbuka (lihat "Catatan Batch 489"/"[RESUME POINT berikutnya]"), pindahkan ke
   `docs/archive/` HANYA setelah seluruh gap actionable-nya tuntas + device-QA lengkap.
5. Nama folder Termux: `~/projects/audioplayer` (lowercase) — FINAL. `rootProject.name` tetap
   `"AudioPlayer"` (hardcoded `settings.gradle.kts`), tidak terikat nama folder/`git remote`.
6. Sektor DITUTUP — jangan proaktif dibuka ulang pada instruksi generik ("next"/"lanjut"); BOLEH
   dieksekusi kalau user beri instruksi eksplisit spesifik minta sektor ini dibuka lagi:
   - **Thread Safety I/O** — Batch 431: audit pola-blocking menyeluruh selesai untuk 70 file di
     luar `ui/` (KECUALI paket `update/`, waktu itu belum disentuh). Batch 432: paket `update/`
     (3 file) disisir — 1 gap ditemukan+fix (`UpdateManager.kt`, Thread→Coroutines), 2 file
     lainnya (`GitHubReleaseChecker.kt`/`UpdateDownloader.kt`) konfirmasi sudah benar. 0 item
     residual diketahui KECUALI pola di luar 9 kategori grep Batch 431 (mis. Room DAO
     non-suspend, kalau ada — belum dicek eksplisit).
   - **compileSdk/targetSdk** — final di targetSdk 36, compileSdk 36. 0 rencana Play Store,
     device user Android 16 (edge-to-edge/predictive back terverifikasi device asli). 0 item
     residual kecuali user eksplisit minta bump API 37 (blocked di migrasi AGP 9.x) atau ada
     temuan baru.
   - **Compose optimization** — `AlbumArt` TUNTAS Batch 429 (`SubcomposeAsyncImage` →
     `AsyncImage`), device asli konfirmasi render normal 0 regresi (Batch 430).
     `DuplicateFinderSheet.kt` `remember` CPU-heavy TUNTAS Batch 431 (pindah `LaunchedEffect` +
     `Dispatchers.Default`), belum diverifikasi device/CI. 0 utang teknis residual lain diketahui
     di sektor ini.

## Keputusan arsitektur utama
Ringkasan penuh + alasan: README.md § "Keputusan Arsitektur". Poin paling kritis:
- `PlaybackService` pakai `MediaLibraryService`, **bukan** `MediaSessionService` — prasyarat
  Playback Resumption resmi.
- `AppLogger` lokal murni (bukan Crashlytics/Sentry) — app tidak punya izin INTERNET sama
  sekali, bagian dari klaim privasinya.
- `PinLockoutPolicy` dipisah dari `AppLockStore` supaya bisa di-unit-test tanpa Context.
- File paling berisiko diubah tanpa cek dokumentasi dulu: `PlaybackService.kt`,
  `AppLockStore.kt`, `app/build.gradle.kts`.

## Struktur package (ringkas)
```
com.rudi.audioplayer/
├── data/      — Store & repository (SharedPreferences/MediaStore), model data (Song, Playlist,
│                SmartPlaylist — rule-based, resolve live via SmartPlaylistEngine)
├── playback/  — PlaybackService (MediaLibraryService), PlayerViewModel, Equalizer, ShakeDetector
├── ui/        — Semua Composable screen & sheet (Home, Library, NowPlaying, Settings, dst.)
├── ui/theme/  — Apple SYSTEM/LIGHT/DARK (utama) + Matte Noir (custom, kebalikan), warna, tipografi
├── util/      — AppLogger (log diagnostik lokal), ApkSignatureChecker
└── widget/    — Home screen widget (PlayerWidgetProvider, WidgetUpdater)
```

## Konvensi penamaan ZIP & versi
`SONIX_vN.zip` melacak nomor batch percakapan (bukan versionName/versionCode) — diperbarui Batch
446 dari `AudioPlayer-batchN-release.zip` lama (branding aktif = SONIX, lihat `app_name`/README;
`AudioPlayer` cuma nama teknis package/rootProject, SENGAJA tetap, lihat "Aturan sesi aktif" #5).
`versionCode`/`versionName` otomatis dari jumlah commit git — TIDAK terkait, TIDAK ikut berubah.
Detail lengkap: README.md § "Standar Penomoran Versi".

[RESUME POINT]
- Batch terakhir: 489. ZIP terakhir: `SONIX_v489.zip`. **3 file disentuh** (1 fungsi ViewModel
  baru + 2 file wiring UI/Activity, dianggap 1 sektor/1 tugas — pola sama Batch 484): lihat
  "Catatan Batch 489" di atas untuk detail penuh. Ringkas: checklist QA eksternal
  (`QA_Checklist_SONIX_v488_terisi.md`) diverifikasi ulang ke source (9 poin "Verdict"-nya,
  bukan ditelan mentah), 1 gap dieksekusi (kontrol "Stop Pemutaran" eksplisit baru di sheet
  Kontrol Lanjutan Now Playing, `PlayerViewModel.stopPlayback()` = pause+seekTo(0), TIDAK
  menyentuh queue/state tersimpan — beda dari swipe-dismiss mini player yang "cancel total").
  Checklist asli disalin ke `docs/QA_CHECKLIST_SONIX_v488.md` (aktif, bukan arsip).
  **0 diverifikasi CI/device Batch 489** — 0 env Android nyata/compiler Kotlin sesi ini (balance
  brace/paren/bracket 3 file: lihat "Catatan Batch 489" di atas untuk angka lengkap).
  **WAJIB DITEST user sebelum lanjut roadmap gap QA lain** (daftar lengkap 4 poin di "Catatan
  Batch 489" di atas): Now Playing → ⋮ → "Kontrol Lanjutan" → seksi "Pemutaran" → baris baru
  "Stop Pemutaran" harus ada & berfungsi (jeda + posisi balik ke 0, lagu/antrean TETAP ada, tekan
  Play lagi lanjut dari awal lagu yang sama) — TANPA regresi ke swipe-dismiss mini player atau ke
  5 tombol transport utama.
  **[RESUME POINT berikutnya]**: 3 sisa gap dari checklist yang punya kandidat fix kode konkret,
  BELUM dikerjakan (lihat "Roadmap Gap QA v488" di "Catatan Batch 489" untuk detail per-poin):
  (a) previous-3-detik eksplisit (`PlaybackService.kt`, 1 baris, risiko rendah); (b) filter audio
  pendek (`MusicRepository.kt`, `getAllSongs()` saja — ambang durasi BELUM dikonfirmasi user,
  tanyakan dulu sebelum eksekusi); (c) shuffle anti-repeat-nearby (FITUR BARU, butuh instruksi
  eksplisit user dulu, bukan micro-task). Sisa gap checklist lainnya (#4/#5/#7/#8/#9) murni
  device-QA/di luar scope, 0 kode untuk dikerjakan.
- Batch 488 (sebelum 489). ZIP: `SONIX_v488.zip`. **2 file diubah** (1 file baru): `LibraryCacheStore.kt`
  (baru), `PlayerViewModel.kt` — FIX root cause laporan urgent user ("app tidak load berulang kali
  setiap aplikasi baru dibuka kembali pasca app di-kill") — lihat "Catatan Batch 488" di atas
  untuk detail penuh. Ringkas: snapshot library terakhir disimpan ke disk, dimuat instan (0
  shimmer) saat app dibuka lagi sambil scan asli tetap jalan senyap di background; tombol
  "Pindai Ulang"/pull-to-refresh manual TIDAK berubah (shimmer manual tetap tampil).
  **0 diverifikasi CI/device Batch 488** — 0 env Android nyata/compiler Kotlin di sesi itu.
  **WAJIB DITEST user** (6 poin lengkap di "Catatan Batch 488" di atas): buka app → tunggu
  library termuat → force-close total → buka lagi → library harus langsung tampil isi (0
  shimmer), diulang 2x + skenario tambah/hapus lagu saat force-close + pastikan tombol
  "Pindai Ulang"/pull-to-refresh manual TETAP menampilkan shimmer seperti biasa (0 regresi).
- Batch 480 (sebelum 488). ZIP terakhir: `SONIX_v480.zip`. **2 file diubah** (dalam batas 3
  file/tugas): `MiniPlayerBar.kt`, `PlayerViewModel.kt` — instruksi eksplisit user "sempurnakan
  mekanisme drag mini player (feedback, konfirmasi user, dll)", lihat "Catatan Batch 480" di atas
  untuk detail penuh. Ringkas: (1) feedback visual (alpha+scale mengikuti drag) + haptic 2-tahap
  (tick real-time di threshold, terpisah dari `LongPress` konfirmasi dismiss); (2) dismiss kini
  bisa di-"Urungkan" lewat Snackbar (infra `UndoableAction` yang sudah ada, `MainActivity.kt` 0
  disentuh). 0 threshold/formula/spring gesture yang sudah ada (120px, damping, dsb) disentuh.
  **0 diverifikasi CI/device Batch 480** — 0 env Android nyata/device fisik/compiler Kotlin di
  sesi ini (balance brace/paren/bracket: lihat "Catatan Batch 480" di atas untuk angka lengkap).
  **WAJIB DITEST user sebelum sektor mini player disentuh lagi** (daftar lengkap 5 poin di
  "Catatan Batch 480" di atas): swipe pelan → bar meredup+mengecil + tick getar di ~120px →
  lepas jari → bar hilang + Snackbar "Urungkan" muncul → tap "Urungkan" → lagu main lagi persis
  dari posisi semula (bukan dari awal). Kirim hasil test ini balik sebelum sektor mini player
  disentuh lagi.
- Batch 479 (sebelum 480). ZIP: `SONIX_v479.zip`. **1 file diubah** (dalam batas 3
  file/tugas): `MainActivity.kt` — FIX root cause bug (2) Batch 477 ("drag lintas tab, bottom nav
  diam"), lihat "Catatan Batch 479" di atas untuk detail penuh. Ringkas: `navPillIndexAnim`
  di-hoist ke scope `AppNavHost` + disinkronkan juga dari `onDragEnd` swipe KONTEN (sebelumnya
  cuma dari onClick tab & drag-langsung-di-tab-bar). Instrumentasi log Batch 478 DICABUT TOTAL.
  **0 diverifikasi CI/device Batch 479** — 0 env Android nyata/device fisik/compiler Kotlin di
  sesi ini (balance brace/paren/bracket file penuh: `{}` 339/339, `()` 1251/1251, `[]` 3/3).
  **WAJIB DITEST user sebelum sektor tab-swipe/bottom-nav dianggap tuntas**:
  1. Install ulang dari `SONIX_v479.zip` (via Termux DAILY UPDATE script).
  2. Di Beranda/Perpustakaan/Pengaturan: swipe horizontal di KONTEN (area kosong, bukan di atas
     tab-bar itu sendiri) → pill/label bottom nav HARUS ikut berpindah ke tab tujuan begitu
     gesture selesai (bukan cuma konten yang berganti seperti sebelumnya).
  3. Ulangi dengan drag kontinu (jari 0 terangkat) yang lintas >1 batas tab (mis. Beranda→
     Pengaturan lewat Perpustakaan) → pill harus ikut berpindah tiap batas terlewati, 0 macet.
  4. Pastikan 0 regresi: tap biasa di tab & drag LANGSUNG di atas tab-bar (Batch 442/444/448)
     masih berfungsi identik seperti sebelumnya.
  Kirim hasil test ini balik sebelum sektor tab-swipe/bottom-nav disentuh lagi.
- Batch 478 (sebelum 479). ZIP: `SONIX_v478.zip`. **1 file diubah**: `MainActivity.kt` —
  INSTRUMENTASI SAJA (0 fix logic, lihat "Catatan Batch 478" di atas) — data log dari langkah ini
  adalah yang mengungkap root cause Batch 479 di atas. Instrumentasinya sendiri sudah DICABUT
  TOTAL di Batch 479, jadi langkah WAJIB USER versi Batch 478 (reproduksi + kirim log) SUDAH
  SELESAI/TERPAKAI — jangan diulang.
- Batch 477 (sebelum 478). ZIP: `SONIX_v477.zip`. **2 file diubah** (dalam batas 3
  file/tugas): `MiniPlayerBar.kt`, `MainActivity.kt` — lihat "Catatan Batch 477" di atas. Ringkas:
  (1) swipe-cancel mini player Batch 476 diperbaiki (wasit gesture dipindah ke
  `PointerEventPass.Initial`, pola sama Batch 350); (2) drag-lintas-tab-konten Batch 435 diperbaiki
  (`pointerInput` key `currentRoute`→`Unit` + `rememberUpdatedState`, pola sama Batch 442).
  **0 diverifikasi CI/device sesi ini.**
  **WAJIB DITEST user sebelum lanjut fitur baru lain**:
  (a) swipe mini player kiri/kanan lewat threshold → bar hilang + musik BERHENTI TOTAL (regresi
      test Batch 476 (a)/(b)/(c) — WAJIB re-test, belum pernah dikonfirmasi user sama sekali);
  (b) tap biasa (0 geser) di mini player MASIH buka Now Playing + ripple normal — 0 regresi;
  (c) di Beranda/Perpustakaan/Pengaturan: swipe horizontal PELAN dimulai dari area KOSONG (bukan di
      atas row/carousel/chip manapun) → label tab tujuan harus MEMBESAR bertahap mengikuti jari
      (efek magnify, Batch 437) SELAMA drag, bukan cuma snap di akhir;
  (d) drag horizontal PANJANG dalam 1 gesture (jari 0 terangkat) yang lintas LEBIH dari 1 batas
      tab (mis. Beranda→Pengaturan lewat Perpustakaan) → magnify/nudge harus tetap hidup terus di
      SELURUH drag, bottom nav ikut berpindah tiap batas tab terlewati, 0 macet di tab kedua;
  (e) ULANGI (c)/(d) tapi mulai drag PERSIS DI ATAS row "Mix"/"Favorit" (Beranda) atau chip
      filter (Library) → kalau MASIH 0 efek di sini walau (c)/(d) sudah benar, LAPORKAN balik
      (lihat "CATATAN BELUM DIVERIFIKASI" Batch 477 di atas — teori sekunder LazyRow-menelan-
      drag, fix TERPISAH belum dikerjakan, prioritas sesi berikutnya kalau dikonfirmasi);
  (f) drag di ATAS bar tab itu sendiri (Batch 442, bukan konten) — pastikan 0 regresi, masih
      jalan seperti biasa (0 disentuh batch ini).
  Kirim hasil test ini balik sebelum sektor mini player/tab-navigation disentuh lagi.
- Batch 476 (sebelum 477). ZIP: `SONIX_v476.zip`. **3 file diubah** (dalam batas 3
  file/tugas): `PlayerViewModel.kt`, `MiniPlayerBar.kt`, `MainActivity.kt` — lihat "Catatan Batch
  476" di atas untuk detail penuh. Ringkas: (1) swipe mini player = cancel (stop total + queue
  kosong + saved state dikosongkan, bukan cuma sembunyikan bar); (2) `_uiState` mini player kini
  disinkronkan dari controller/saved-state begitu app dibuka kembali (baik Service masih hidup
  maupun ikut mati total), bukan reset kosong sampai transisi lagu berikutnya.
  **0 diverifikasi CI/device sesi ini** — 0 env Android nyata/compiler Kotlin/device fisik.
  **WAJIB DITEST user sebelum lanjut fitur baru lain**:
  (a) swipe mini player kiri ATAU kanan sampai lewat threshold → bar hilang, musik BERHENTI
      TOTAL (bukan cuma UI-nya hilang sementara musik tetap main di notifikasi/bubble);
  (b) swipe pelan/tidak sampai threshold → bar pegas balik ke posisi semula, TIDAK cancel,
      musik/tap-untuk-expand tetap normal;
  (c) tap biasa (tanpa geser) di mini player MASIH membuka Now Playing seperti biasa — 0 regresi
      ke `onExpand`, drag detector tidak boleh "mencuri" tap;
  (d) putar lagu, tekan tombol Home (bukan swipe-away Recents) supaya app background tapi proses
      TIDAK dibunuh OS, buka lagi app dari launcher → mini player harus tetap tampil benar
      (lagu/isPlaying/progress sama), TIDAK reset;
  (e) putar lagu dari WIDGET/headset/Bluetooth SAAT app dalam kondisi force-close total (App
      Info → Force Stop, atau swipe dari Recents lalu tunggu OS recycle), lalu BUKA APP dari
      launcher (bukan dari notifikasi) → mini player harus langsung sinkron menampilkan lagu yang
      sedang main itu (bukan kosong);
  (f) force-close total app SAAT ada lagu tersimpan (habis diputar lalu di-pause, proses lalu
      dibunuh total via App Info → Force Stop) → buka app dari launcher → mini player muncul
      PAUSED di lagu terakhir (siap lanjut), TANPA auto-play sendiri;
  (g) setelah (a) swipe-cancel, force-close total app lalu buka lagi dari launcher → mini player
      TIDAK boleh muncul lagi (lagu yang di-cancel tidak dibangkitkan oleh sync (e)/(f) di atas).
  Kirim hasil ketujuh test ini balik sebelum sektor mini player/cold-start disentuh lagi.
- Batch 475 (sebelum 476). ZIP: `SONIX_v475.zip`. **2 file diubah** (dalam batas 3
  file/tugas): `PlaybackService.kt`, `FloatingBubbleService.kt` — lihat "Catatan Batch 475" di
  atas untuk detail penuh. Ringkas: 2 titik `startForegroundService()` (bubble<->PlaybackService,
  arah cold-start-eksternal & tap-bubble-cold-start) disamakan ke pola `runCatching` yang sudah
  terbukti di `BubbleBootReceiver.kt`/`onTaskRemoved`. 0 behavior/logic lain diubah, murni
  error-handling. **Sulit direproduksi sengaja** (butuh device dgn restriksi OEM aktif tepat di
  momen resumption eksternal) — validasi dari user CUKUP: pastikan 0 regresi ke perilaku normal
  (bubble tetap nyala saat playback dipicu widget/headset/Bluetooth/lock-screen, tap tombol
  bubble cold-start tetap memicu restore seperti sebelumnya).
- Batch 474 (sebelum 475). ZIP: `SONIX_v474.zip`. **1 file diubah** (dalam batas 3
  file/tugas): `FloatingBubbleService.kt` — lihat "Catatan Batch 474" di atas untuk detail penuh.
  (a) Survive app-kill (Batch 471) **DITUTUP atas permintaan eksplisit user** — konfirmasi
  informal + user eksplisit minta topik ini TIDAK ditanyakan lagi. **JANGAN munculkan lagi
  permintaan test survive-Recents formal kecuali user sendiri buka ulang topiknya.** (b) FITUR
  BARU: `setupDrag()` digeneralisasi (param `onTap`, default identik lama, 0 breaking change ke
  pemanggil lama) lalu dipasang JUGA ke ke-4 tombol kontrol lewat `setupControls()` — drag yang
  dimulai di atas tombol sekarang ikut memindah bubble, tap biasa tetap jalan lewat
  `performClick()` ke listener asli yang tidak disentuh.
- **Status pertanyaan sempit Batch 472 (sisi kanan lega sampai tombol minimize)**: user TIDAK
  menjawab eksplisit — langsung minta fitur (b) di atas. **TETAP status "belum dikonfirmasi
  user"**, SOP larang asumsi — kalau area dead-space `bubble_album_art`→tombol MASIH terasa
  sempit di device fisik, itu kandidat regresi TERPISAH dari fitur (b) batch ini (fitur (b) HANYA
  menambah drag DI ATAS badan tombol itu sendiri, TIDAK mengubah `applyAlbumArtTouchDelegate()`
  Batch 472 sama sekali).
- **WAJIB DITEST user sebelum lanjut fitur baru lain**:
  (1) drag dimulai PERSIS di atas salah satu dari 4 tombol kontrol (coba tiap satu: play/pause,
      prev, next, minimize) sekarang ikut MEMINDAHKAN bubble — sebelumnya 0 efek sama sekali;
  (2) TAP biasa (tanpa gerak jari) di ke-4 tombol itu MASIH berfungsi 100% normal — play/pause
      toggle, skip prev/next, minimize ke tepi layar — 0 regresi fungsi;
  (3) drag dari album art (state expanded) & drag dari tab minimized (2 titik LAMA, TIDAK
      disentuh batch ini) tetap seperti Batch 472 — sisi kanan masih lega sampai tombol minimize,
      jangkauan gerak masih bebas penuh ke seluruh layar.
  Catatan yang TIDAK PERLU dilaporkan sebagai bug: bunyi klik sistem Android saat tap ke-4 tombol
  itu sengaja hilang (lihat "Catatan Batch 474" — konsisten filosofi iOS-look proyek, ripple
  Android juga sudah dimatikan di bottom nav Batch 439). Laporkan HANYA kalau ada regresi FUNGSI.
- Batch 473 (sebelum 474). ZIP: `SONIX_v473.zip`. **0 file kode diubah** (klarifikasi murni) —
  poin 3 Batch 472 ("bubble tetap muncul saat player eksternal dimainkan") RESMI DITUTUP:
  dikonfirmasi PERILAKU YANG DIHARAPKAN (bubble ikut trigger play dari luar UI app —
  headset/Bluetooth/Android Auto/widget), BUKAN bug lintas-app.
- Batch 470 (sebelum 471). ZIP: `SONIX_v470.zip`. **1 file diubah** (dalam batas 3
  file/tugas): `FloatingBubbleService.kt`. 2 fitur baru dari user (lihat "Catatan Batch 470" di
  atas untuk detail): (1) cold-start fix `sendPlaybackAction`/`setupControls` (tap kontrol bubble
  segera setelah reboot, sebelum app/widget pernah dibuka, sekarang tetap bisa trigger restore
  antrean — dulu jatuh ke `openApp()` selamanya begitu controller lokal konek duluan dgn antrean
  kosong); (2) `setupDrag` discoped ke `bubble_album_art` saja untuk state expanded (minimized
  tab 0 berubah) + `TouchDelegate` biar area sentuh tetap luas TANPA ubah layout/dp XML. **0
  pernah menyentuh [screenBounds]/formula clamp/posisi** — 0 tumpang tindih dgn investigasi
  Batch 469 di bawah, yang MASIH terbuka/belum resmi selesai (user cuma konfirmasi kliping
  landscape UMUM, BUKAN protokol reproduksi spesifik Batch 469).
- **WAJIB dari user sebelum lanjut fitur baru lain**: (a) test cold-start — reboot HP, JANGAN
  buka app/widget sama sekali, langsung tap play/prev/next di bubble → harus mulai memutar
  (bukan cuma buka app); (b) test touch-scope — drag mulai dari padding kosong pill (BUKAN album
  art/tombol) harus 0 efek; drag mulai dari album art (termasuk sedikit meleset di luar 40dp-nya)
  harus tetap jalan seperti biasa, jangkauan gerak tetap bebas penuh ke seluruh layar seperti
  sebelumnya. Kirim hasil kedua test ini balik.
- **ITEM WAJIB PALING PRIORITAS begitu regresi Batch 469 muncul lagi** (belum terjadi/dilaporkan
  ulang sesi ini, tapi statusnya BELUM ditutup — lihat "Catatan Batch 469" & "Catatan Batch 470"):
  terima log dari user — WAJIB direproduksi PERSIS (landscape, drag ke tepi nav-bottom sampai
  hilang, **JANGAN rotasi balik ke portrait dulu**, baru ekspor Log Diagnostik) — baca
  `Batch469 bounds-compare` & `Batch469 drag readback` SEBELUM coding fix apa pun. **JANGAN
  tebak fix ke-5** tanpa data ini (pola terlarang eksplisit, lihat KDoc kelas "PELAJARAN PROSES
  Batch 461→462").
- Batch 469 (sebelum 470). ZIP: `SONIX_v469.zip`. **1 file diubah** (dalam batas 3
  file/tugas): `FloatingBubbleService.kt` — **0 formula/clamp/posisi diubah**, murni 2 log baru
  (bounds-compare `onConfigurationChanged` + drag-release/readback `ACTION_UP`). Respons ke
  REGRESI BARU: bubble hilang total saat drag ke tepi landscape berbeda (nav-bottom), balik
  normal cuma kalau rotasi ke portrait. **ITEM WAJIB PALING PRIORITAS sesi berikutnya**: terima
  log dari user — WAJIB direproduksi PERSIS (landscape, drag ke tepi nav-bottom sampai hilang,
  **JANGAN rotasi balik dulu**, baru ekspor Log Diagnostik) — baca `Batch469 bounds-compare` &
  `Batch469 drag readback` SEBELUM coding fix apa pun. **JANGAN tebak fix ke-5** tanpa data ini
  (pola terlarang eksplisit, lihat KDoc kelas "PELAJARAN PROSES Batch 461→462").
- Kalau log BUKTIKAN `newConfigBounds` ≠ `currentWindowMetricsBounds` secara signifikan (bukan
  cuma rounding 1-2px): itu konfirmasi teori Batch 469 — fix kandidat: samakan SATU sumber
  [screenBounds] yang dipakai KONSISTEN di `onCreate` DAN `onConfigurationChanged` (bukan 2 API
  beda), TAPI harus tetap jaga freshness/timing yang jadi alasan Batch 461 pindah ke `newConfig`
  — cek dulu ke user/dokumentasi apakah `currentWindowMetrics` di titik NON-rotasi-transisi
  (mis. di drag ACTION_UP yang sudah pasti settle) aman dipakai FRESH tanpa masalah staleness
  yang sama seperti saat [onConfigurationChanged] (2 konteks timing yang beda, jangan disamakan
  otomatis).
- Kalau log tunjukkan readback drag KELUAR dari `screenBounds` yang dipakai clamp itu sendiri
  (bukan soal 2-API, tapi APLIKASI window yang salah render): itu petunjuk arah lain sama sekali
  — WAJIB baca ulang [addBubbleView]/params.apply sebelum simpulkan apa pun.
- Status fix Batch 468 (`FLAG_LAYOUT_IN_SCREEN`) & item-item verifikasinya (portrait gap,
  landscape-edge-clip, delta readback (0,0)) MASIH BELUM ada laporan device terpisah dari user —
  regresi Batch 469 ini TIDAK otomatis berarti Batch 468 gagal total, cuma menunjukkan ADA
  skenario tambahan (drag manual ke edge landscape tertentu) yang belum ter-cover. Jangan
  simpulkan revert sampai data log ini di tangan.
- Fix crash Batch 466 (`BubbleBootReceiver.kt`) masih BELUM ada konfirmasi eksplisit "0
  force-close lagi setelah reboot" dari user — tetap dianggap valid berdasarkan bukti log,
  tunggu laporan device kalau ada kesempatan (bukan blocker urutan, bisa paralel dgn cek bubble).
- Batch 464 (sebelum 465). ZIP: `SONIX_v464.zip`. **1 file diubah** (dalam batas 3
  file/tugas): `FloatingBubbleService.kt` — **masih instrumentasi, 0 fix formula/logic**. Log
  Batch 463 pertama dari user (device fisik) kasih DATA MENGEJUTKAN: mismatch besar (target
  x=1011 vs nyata x=551, selisih 460px) terjadi saat `minimize()` BIASA — **0 rotasi terlibat**.
  Ini menggeser dugaan dari "animasi transisi rotasi" (fokus Batch 462) ke "window `WRAP_CONTENT`
  belum tuntas resize saat kita baca `width`/apply posisi" — teori BARU, belum pernah diuji. Batch
  464 memperluas readback: (1) log ukuran nyata (`container.width/height`) juga, dibanding ukuran
  saat target dihitung; (2) tambah readback KEDUA di +800ms (selain +250ms) untuk bedakan
  "settling sementara" vs "salah permanen". Detail penuh: "Catatan Batch 464" di atas &
  `CHANGELOG.md`. Sektor bubble (Roadmap #11) masih terbuka, TIDAK ada sektor DITUTUP disentuh.
- **WAJIB DILAKUKAN sebelum lanjut fix apa pun ke file ini (PALING PRIORITAS)**: minta user (1)
  buka bubble → minimize → (boleh) rotasi lagi kalau mau; (2) **diamkan HP minimal 1 detik penuh**
  sebelum ambil log (readback terjauh sekarang +800ms, bukan +250ms lagi); (3) ambil logcat ATAU
  ekspor Settings → Lanjutan → Log Diagnostik; (4) kirim balik. **JANGAN eksekusi fix apa pun ke
  formula/logic sebelum perbandingan 250ms-vs-800ms & ukuran-nyata-vs-target ada di tangan** —
  2 hasil berbeda (lihat "Catatan Batch 464") mengarah ke 2 kelas fix yang sama sekali berbeda.
- **HISTORI KEGAGALAN item (3) — WAJIB dibaca sebelum lanjut**: Batch 460
  (`resources.displayMetrics`→`currentWindowMetrics.bounds`) ❌. Batch 461 (`screenBounds` dari
  `newConfig`) ❌ "100% kelihatan, gak keclip sama sekali". Batch 462 (safety-net re-assert 2x
  delay) ❌ GAGAL LAGI. Batch 463 (pivot instrumentasi, logcat lama TERBUKTI 0 sinyal) → readback
  pertama JUSTRU dari event minimize BIASA (bukan rotasi), mismatch 460px. Batch 464 (perluas
  readback: ukuran nyata + titik +800ms) — hasil BELUM diketahui. **BACA logika keputusan
  berikut begitu log Batch 464 masuk**: (a) kalau `container.width/height` di readback BEDA dari
  target width/height → window memang masih resize saat snap di-apply, fix arahnya: tunda
  `updateViewLayout` sampai window BENAR tuntas resize (bukan cuma `container.post{}` 1x), atau
  hitung target dari ukuran yang SUDAH pasti final (mis. dimensi XML tab minimized langsung,
  bukan `container.width` runtime); (b) kalau ukuran SAMA tapi posisi +250ms meleset namun +800ms
  SUDAH benar → murni soal delay re-assert kurang lama, cukup naikkan
  `ROTATION_RESNAP_DELAYS_MS`/tambah 1 titik lagi; (c) kalau +800ms MASIH meleset SAMA dengan
  ukuran yang SUDAH match target → root cause lain sama sekali (bukan resize, bukan timing) —
  WAJIB investigasi baru, jangan asumsikan salah satu dari (a)/(b) tanpa cek datanya.
- **DIKONFIRMASI device fisik user (Batch 460)**: (1) mini trigger lebih gampang di-tap ✅; (2)
  bagian timbul ~10% ✅; (4) drag tab minimized 100% kelihatan/terkontrol penuh selagi digeser ✅;
  (5) 0 regresi ke minimize/expand/fade/auto-minimize Batch 98-100/453/454 ✅. **(3) mentok tepi
  konsisten landscape ❌ GAGAL 3x berturut-turut (Batch 460, 461, 462)** — lihat "HISTORI
  KEGAGALAN" di atas. Batch 463 TIDAK mencoba fix ke-4, murni instrumentasi.
- **DIKONFIRMASI device fisik user (Batch 458, masih berlaku sebagai baseline utk item 2/4/5)**:
  (1) tab minimized ~30% kelihatan/~70% tersembunyi — nilai ini SUDAH DIGANTIKAN ~10%/~90% oleh
  Batch 460 (dikonfirmasi ✅ di atas); (2) mini trigger tetap bisa di-tap, TAPI "sedikit lebih
  susah" — residual ini DIKONFIRMASI FIX di Batch 460 (✅ di atas); (3) drag tab minimized 100%
  kelihatan/terkontrol penuh selagi digeser — confirmed (baseline utk item 4 Batch 460 di atas);
  (4) 0 regresi ke minimize/expand/fade/auto-minimize Batch 98-100/453/454 — confirmed.
- **CATATAN proses (berlaku terus)**: istilah user "timbul" = bagian KELIHATAN tab, bukan fraksi
  klip (`EDGE_CLIP_FRACTION`) itu sendiri — dua hal berlawanan arah. Kalau user minta angka
  persentase lagi tanpa kata eksplisit "sembunyi/klip" vs "timbul/kelihatan", WAJIB klarifikasi
  arah dulu (pola Batch 456→457→458), jangan tebak. Sejak Batch 460, "timbul"/`EDGE_CLIP_FRACTION`
  TIDAK LAGI otomatis mengontrol lebar area sentuh (dipisah via `touchPad`) — permintaan "perkecil
  timbul" ke depan AMAN dieksekusi tanpa risiko balik memperkecil touch target. **PELAJARAN PROSES
  Batch 461→462→463 (WAJIB diikuti)**: JANGAN ulangi pola "ganti API/sumber baca metrics lagi"
  (sudah 3x terbukti bukan akar masalah, Batch 460/461/462). Minta logcat generik SAJA juga
  TERBUKTI TIDAK CUKUP (Batch 462→463: user bawa logcat, tapi 0 baris app-level karena service
  tidak pernah logcat) — WAJIB ada instrumentasi eksplisit DULU di kode (sudah ditambah Batch 463)
  SEBELUM logcat/Log Diagnostik device asli benar-benar berguna. Kalau residual item (3) muncul
  LAGI setelah log Batch 463 masuk dan dibaca: putuskan fix ke-4 dari perbandingan snap-target vs
  readback +250ms di log itu (lihat "HISTORI KEGAGALAN" di atas), bukan teori baru tanpa bukti.
- **BELUM dikonfirmasi (Batch 452, masih berlaku)**: (1) label 3 tab 0 lagi ellipsis di font
  normal; (2) drag flick cepat + tap-tab biasa berhenti TEPAT di tab tujuan, 0 "mundur 1 kolom".
- **DIKONFIRMASI device fisik user (Batch 451, masih berlaku)**: (1) pill ukuran normal, 0 kapsul
  raksasa (fix Batch 450); (2) 0 kilatan kotak abu-abu di tab ditinggalkan (fix Batch 449).
- **BELUM dikonfirmasi (lama)**: regresi warna ikon/label tema Skeu (video user Batch 451 pakai
  tema default/gelap, bukan Skeu). TalkBack tidak bisa dicek dari rekaman visual.
- Mandat lain: 0 ada. Sektor bottom nav (Batch 448-452) dianggap aktif-stabil kecuali user
  laporkan temuan baru. Lanjutkan sektor manapun yang diminta user berikutnya (0 sektor DITUTUP
  baru dibuka permanen, 0 sektor baru ditutup juga).
- **PELAJARAN PROSES Batch 450 TETAP berlaku** (lihat komentar kode di `CustomNavBarTabItem`):
  modifier layout yang meniru API resmi WAJIB diverifikasi ke source/dokumentasi asli dulu,
  jangan diasumsikan.
