# PROJECT_STATE.md

Konteks untuk sesi chat AI mana pun yang melanjutkan proyek ini. Baca file ini dulu sebelum
menyentuh kode apa pun. Detail lengkap tiap batch ada di `CHANGELOG.md`; ringkasan fitur
lengkap ada di `README.md`. File ini adalah ringkasan status + jebakan yang sudah pernah
kejadian, bukan pengganti keduanya.

## Batch terakhir yang selesai
**Batch 79 (Upgrade identitas Skeuomorphism -> Neumorphism)** — Instruksi eksplisit user:
"Titanium dominan + sedikit sentuhan Zamrud + depth ultra realistic". **Atomic Change, 6 file**
lintas ui/theme + MainActivity.kt (protected/parsial) + ui/NowPlayingScreen.kt — dikecualikan dari
batas normal 10 file/1 modul krn identitas visual harus konsisten di SEMUA titik panggil
sekaligus (kalau dipecah antar-batch, ada jendela UI campur separuh Hyper-Realism lama/separuh
Neumorphism baru). Ringkasan (detail lengkap: `CHANGELOG.md` Batch 79):
- `skeuEmboss()` (TactileDepth.kt) ditulis ulang total: dual soft-shadow multi-layer (bukan lagi
  panel-logam 4-stop + grain + specular-glint + outer-bevel + inner-groove 7-layer Hyper-Realism)
  — sisi gelap kanan-bawah 3 layer, sisi terang kiri-atas 2 layer, offset+alpha bertingkat meniru
  soft-blur box-shadow ganda CSS neumorphism (DrawScope Compose gaada blur asli tanpa RenderEffect
  API 31+). **0 border/grain sama sekali** — ciri paling khas neumorphism generik. Pressed =
  CONCAVE (`dir=-1f` membalik SELURUH sisi terang/gelap, bukan cuma mengecil elevasi).
- Grain/groove token (`SkeuBrushGrain*`, `SkeuInnerGroove*`) **dihapus total** dari Color.kt
  (grep-confirmed 0 caller). Token baru: `SkeuNeuSurfaceDark/Light` (panel fill hampir sewarna
  kanvas, TIDAK menyentuh role M3 `surface`/`surfaceVariant` di Theme.kt) +
  `SkeuEmerald`/`SkeuLightEmerald` (aksen zamrud baru).
- Sentuhan Zamrud: 3 titik SENGAJA kecil/jarang (bukan role M3 apa pun, supaya Titanium tetap
  satu-satunya token di primary/surfaceTint = "Titanium dominan" literal) — (1) inti glow
  skeuEmboss() HANYA saat pressed, (2) 1 color-stop tambahan di root ambient streak
  (MainActivity.kt, alpha sengaja lebih rendah dari kilau silver utama), (3) inti sisi terang
  hero art NowPlayingScreen.kt SELALU berbaur sedikit (alpha 0.14f, permanen — beda dari
  skeuEmboss() krn hero art statis, tidak punya state pressed).
- `frostedGlass()` (BlurUtils.kt) Skeu skip `.border()` total (dulu brushed-metal repeating rim).
- `Theme.kt`: `SKEU_DARK_LITE.displayName` "Skeuomorphism"->"Neumorphism" + description baru.
  `storageKey` "skeu_dark_lite" **sengaja tidak diganti** (preferensi tema tersimpan user tetap
  valid, tidak ter-reset).
- Live-preview swatch di `SettingsScreen.kt` **tidak disentuh sama sekali** — manggil
  `Modifier.skeuEmboss()` langsung, otomatis ikut render Neumorphism baru.
- **Belum diverifikasi compile/visual sungguhan di device** — statis-read only, tidak ada
  `kotlinc`/emulator di environment kerja ini (konsisten sama seperti batch-batch sebelumnya).
  Prioritas berikutnya kalau user minta lanjut: rebuild CI + install APK, cek dual-shadow +
  emerald touch + transisi pressed/concave beneran kebaca di layar HP asli.

**Batch 78 (Debugging pass menyeluruh — "debugging semua area")** — Audit statis sistematis
lintas SEMUA area (data/, playback/, ui/, ui/theme/, util/, widget/), bukan laporan user. 2 bug
nyata ditemukan & diperbaiki (2 file):
1. **`LibraryScreen.kt` sweep-select** — `rowBoundsInRoot` (map index->posisi Y baris) cuma
   pernah DITULIS lewat `onGloballyPositioned`, tidak pernah DIHAPUS saat baris keluar komposisi
   (LazyColumn recycle) — entry basi bisa ke-hit `indexAt()` setelah list di-scroll di antara dua
   sweep gesture (scroll biasa lolos dari `detectDragGesturesAfterLongPress` karena tidak tembus
   threshold long-press), bikin sweep diam-diam nyeleksi lagu yang salah. **Ini root cause dari
   gap yang Batch 70 sendiri sudah tandai "belum ditest" tapi tidak pernah di-root-cause.** Fix:
   `DisposableEffect(index) { onDispose { rowBoundsInRoot.remove(index) } }` per item.
2. **`PlayerViewModel.kt` MediaController leak** — `onCleared()` lama cuma `controller?.release()`,
   no-op kalau `controllerFuture` (dari `connect()`) belum resolve saat ViewModel di-clear (jendela
   race sempit tapi nyata) — future in-flight tidak pernah di-cancel, koneksi ke `PlaybackService`
   bocor. Fix: `controllerFuture` jadi field, `onCleared()` pakai `MediaController.releaseFuture()`
   (API resmi Media3, handle kedua kasus: future belum resolve ATAU sudah resolve).

**Diaudit, TIDAK ada bug baru** (dicek eksplisit, bukan dilewati): semua cursor/stream I/O (semua
`.use{}`), 0 `GlobalScope`/`runBlocking`/`!!` di seluruh codebase, listener register-unregister
balance (`ShakeDetector`, `PlaybackService.onDestroy`), thread-safety `WidgetUpdater.updateAll`,
`AppLogger.kt` crash-logger (cocok spec MediaStore API 29+/FIFO 50/metadata lengkap). **Token Skeu
Hyper-Realism (Batch 73-75) dibaca ulang baris-per-baris** (TactileDepth.kt/BlurUtils.kt/
MainActivity.kt/NowPlayingScreen.kt vs semua definisi Color.kt) — semua token dirujuk & valid,
tidak ada compile error baru ditemukan selain fix TileMode Batch 75 yang sudah lama beres, TAPI
**masih tetap belum diverifikasi compile/visual sungguhan di device** — statis-read tidak bisa
menggantikan itu, tidak ada `kotlinc`/emulator di environment kerja ini. Detail lengkap:
`CHANGELOG.md` Batch 78.

**Batch 77 (Dokumentasi: roadmap 15 fitur generik 100% offline)** — Murni dokumentasi, 0 kode
disentuh. File baru `ROADMAP_15_FITUR_OFFLINE.md` (root repo): 15 fitur generik yang belum ada
di project, semuanya bisa jalan 100% lokal tanpa izin INTERNET, tiap entri dilengkapi perkiraan
kompleksitas + risiko teknis + dependency yang dibutuhkan kalau nanti dieksekusi. Ditutup tabel
prioritas (effort/risiko rendah→tinggi) sebagai saran urutan, BUKAN keputusan/commitment —
murni referensi kalau user mau pilih salah satu untuk batch implementasi berikutnya. Detail
lengkap: `CHANGELOG.md` Batch 77.

**Batch 76 (Lanjutan pangkas waktu compile CI sampai habis)** — Configuration cache diaktifkan
(`org.gradle.configuration-cache=true` + `problems=warn` sbg jaring pengaman, lever terbesar yg
belum disentuh Batch 62) + step `actions/cache@v4` baru khusus `.gradle/configuration-cache`
(project-local, TIDAK ikut ter-cache `setup-gradle@v3` yang cuma menyasar `~/.gradle`) + heap
4096m/Parallel GC. **1 item BUKAN zero-effect**: `lint { checkReleaseBuilds = false }` — lepas
`lintVitalRelease` dari `assembleRelease` (APK byte-nya tidak berubah, tapi 1 lapis verifikasi
otomatis hilang dari CI, `lint`/`lintRelease` manual masih bisa dijalankan kapan pun). **SENGAJA
TIDAK diterapkan**: migrasi Kotlin 1.9.24->2.0+/K2 (lever terbesar yg tersisa, ~2x lebih cepat,
tapi migrasi ekosistem sungguhan — Compose compiler pindah mekanisme total, semua dependency
perlu kompatibel — terlalu berisiko tanpa compiler Android di sini utk verifikasi; kandidat
batch terpisah kalau user mau lanjut, bukan sesuatu yg aman diselipkan diam-diam). **Belum
diverifikasi CI run sungguhan** — config cache khususnya butuh 1 run pertama utk isi cache
(cold), baru terasa manfaatnya di run KEDUA dst; kalau run pertama pasca-batch ini gagal/warning
config-cache-related, itu bukan tanda batch ini rusak, cek log `problems=warn` output-nya dulu
sebelum panik. Detail lengkap: `CHANGELOG.md` Batch 76.

**Batch 75 (Fix 3 error compile CI dari log_fail_128)** — `TileMode.Repeat` (bukan enum valid) ->
`TileMode.Repeated` di 3 titik teknik grain/brushed-metal Skeu Hyper-Realism (`MainActivity.kt`,
`BlurUtils.kt`, `TactileDepth.kt`, semua dari Batch 73). Murni typo nama enum, 0 perubahan visual
dari yang dimaksud Batch 73/74. **Pola relevan utk batch depan**: `TileMode` valid entries cuma
Clamp/Repeated/Mirror/Decal — jangan asumsikan nama enum dari intuisi/training, terutama utk API
Compose graphics yang jarang dipakai (tileMode jarang muncul di kode biasa). Skeu Hyper-Realism
(Batch 73/74) masih **belum pernah berhasil di-compile sampai batch ini** — jadi juga masih
**belum diverifikasi visual di device sama sekali**, prioritas berikutnya begitu user konfirmasi
build sukses. Detail lengkap: `CHANGELOG.md` Batch 75.

**Batch 74 (Debug UI pass: opaque-white-border bug + AlbumArtHero light-mode gap)** — Audit
"debugging UI sampai matang" (bukan laporan user), fokus ke file paling berisiko dari Batch 73
yang belum diverifikasi visual. 2 bug nyata ditemukan & diperbaiki, keduanya sudah ada sejak
Batch 61 (autonomi Light/Dark) tapi baru kelihatan dari baca kode teliti: (1) `TactileLightHighlight`/
`SkeuLightHighlight` = `Color.White` TANPA alpha (opaque penuh) — beda dari semua token bevel lain
yang ber-alpha rendah — dipakai langsung sbg stop border di `frostedGlass()` (semua sheet/mini
player/card) & `skeuEmboss()` (Batch 73's outer border, pola kali bukan ganti alpha) → border
Tactile Light & Skeu Light selama ini garis putih SOLID, persis "bright white border" yang
komentar proyek sendiri larang. Fix: alpha 0.55f/0.65f. (2) `AlbumArtHero` (`NowPlayingScreen.kt`,
piringan album 280dp, permukaan terbesar di app) manual-draw Tactile & Skeu-nya TIDAK PERNAH baca
`LocalIsDarkTheme` sejak Batch 61 — selalu pakai token dark-only meski mode aktif terang, jadi di
Light mode hero art digambar dgn shadow/AO/specular gelap di atas panel terang. Fix: `isDark`
ditambah, kedua cabang pilih token Light/Dark yang sesuai (5 token Skeu, 2 token Tactile). 2 file
(`Color.kt`, `NowPlayingScreen.kt`), tidak ada protected asset. **Belum diverifikasi visual** —
tapi fix ini justru PALING relevan dicek di Light mode (Tactile Light & Skeu Light, terutama hero
art Now Playing) karena itu titik yang selama ini salah tapi tidak pernah dilihat langsung sejak
Batch 61. Detail lengkap: `CHANGELOG.md` Batch 74.

**Batch 73 (Fix sweep-select tak bisa dilanjutkan + Skeuomorphism 2.0 Hyper-Realism UI)** —
2 instruksi. (1) Sweep-select: `onDragStart` di `SongListView` (`LibraryScreen.kt`) selalu
replace total seleksi dgn `persistentSetOf(songs[idx].id)` tiap gesture baru dimulai — begitu
user kepentok tepi list, angkat jari, long-press lagi buat lanjut, seleksi lama hilang diganti 1
lagu. Ikut ditemukan: closure gesture baca `selectedIds` basi (pointerInput cuma restart saat
`songs` berubah). Fix: `rememberUpdatedState(selectedIds)` + `sweepBaseSelection` snapshot per-
gesture, tiap update sweep sekarang UNION dgn base bukan replace. (2) Skeuomorphism diredesain
total jadi "Hyper-Realism UI" — dilepas dari `embossSurface()` bersama Tactile (dulu berbagi
mekanisme, sekarang independen), 7 layer fisik baru di `skeuEmboss()`: ambient occlusion, cast
shadow 2-layer, base surface 4-stop curved-metal (opaque via `lerp()`, bukan alpha — identitas
"panel solid" dari Batch 58 tetap terjaga), brushed-metal grain (`TileMode.Repeat`), specular
glint (radial, meredup saat ditekan), double-bevel border (outer + inner groove inset). 5 file:
`TactileDepth.kt` (skeuEmboss independen), `Color.kt` (bevel diperkuat + 12 token baru),
`BlurUtils.kt` (edge brush Skeu jadi brushed-stripe, bukan lagi sama strukturnya dgn Tactile),
`MainActivity.kt` (protected, parsial — root wash Skeu +1 layer grain), `NowPlayingScreen.kt`
(AlbumArtHero Skeu ikut bahasa hyper-realism). Tactile TIDAK disentuh sama sekali. **Belum
diverifikasi visual/compile** (tidak ada kotlinc/emulator) — brace/paren seimbang di semua file,
grep konfirmasi semua token lama masih konsisten dirujuk. Prioritas sesi berikutnya: rebuild +
cek langsung di device, teknik `TileMode.Repeat` grain belum pernah dirender di codebase ini
sebelumnya jadi paling berisiko meleset dari niat visual (terlalu halus/terlalu kasar). Detail
lengkap: `CHANGELOG.md` Batch 73.

**Batch 72 (Fix sweep-select gesture conflict + hardening widget theme call)** — Sweep-select
(Batch 70) tidak pernah jalan krn bentrok dgn `combinedClickable.onLongClick` (Batch 66) di
`SongRow` — 2 pengenal long-press pada sentuhan fisik yang sama, satu (row-level) membatalkan
yang lain (LazyColumn-level sweep). Fix: `onLongClick` di `SongRow` dihapus, sweep-nya sendiri
sudah cover kasus "tekan-tahan 1 baris tanpa drag." **Widget (icon play/pause tak ganti +
warna/tema tak sinkron): DITINJAU ULANG MENYELURUH, kode-nya benar** (dicek baris-per-baris,
semua properti dipush lewat RemoteViews+updateAppWidget yang sama persis dgn title/artist/art
yang TERBUKTI berhasil) — 1 bug nyata diperbaiki (main-thread I/O di
`setThemeMode`/`setThemeIdentity`, dipindah ke IO dispatcher) tapi itu bukan penjelasan yang
memuaskan utk "sama sekali tidak berubah, 2 update berturut-turut." **BUTUH TINDAKAN USER**:
lepas widget dari home screen, tempel ulang yang baru — kemungkinan besar launcher meng-cache
RemoteViews/id widget lama dari SEBELUM Batch 68 (`widget_root` id ditambahkan Batch 68) dan
tidak pernah re-bind ke instance widget yang sudah lama nempel. Kalau setelah lepas+tempel ulang
MASIH sama, itu baru bug kode sungguhan — minta user kirim logcat atau screen record widget
sebelum lanjut coba lagi, jangan tebak-tebak dari kode statis lagi. **Pola relevan utk batch
depan**: kalau ada custom `pointerInput`/`detectDragGestures*` yang dipasang di container
(LazyColumn/Column) yang ITEM-ITEM di dalamnya juga punya `clickable`/`combinedClickable`
sendiri, selalu curigai gesture conflict duluan sebelum curiga logic salah — dua pengenal
gesture independent nyaris selalu saling sabotase kalau menyasar sentuhan yang sama. Detail
lengkap: `CHANGELOG.md` Batch 72.

**Batch 71 (Fix 2 error compile CI dari log_fail_124)** — `SongArtBitmapLoader` (Batch 69)
kurang override `BitmapLoader.supportsMimeType()` (abstract tanpa default di Media3 1.3.1) —
ditambah, `true` utk mime `image/*`. `LibraryScreen.kt` `onSweepSelectRange` (Batch 70)
assign `ImmutableSet<Long>` ke var `PersistentSet<Long>` — ditambah `.toPersistentSet()`.
**Pola relevan utk batch depan**: kalau nambah `override fun` dari interface pihak ketiga
(Media3, dll), selalu cek changelog/release notes versi library yang dipakai (lihat
`app/build.gradle.kts`) utk method abstract baru — jangan asumsikan signature interface sama
dgn versi yang diingat dari training. Detail lengkap: `CHANGELOG.md` Batch 71.

**Batch 70 (Fitur sweep-select: tekan-lama lalu geser, tab Lagu)** — Jawaban atas laporan
"pemilihan lagu satu-satu bikin pegel" (Batch 69). User pilih mekanisme via pertanyaan
klarifikasi: tekan-lama 1 lagu lalu (tanpa angkat jari) geser ke atas/bawah buat pilih rentang
lagu sekaligus. Infrastruktur selection mode/checkbox/bulk-add-to-playlist SUDAH ADA dari
sebelumnya — yang ditambah cuma gesture-nya: `LibraryScreen.kt`'s `SongListView` dapat
`pointerInput`+`detectDragGesturesAfterLongPress` di level `LazyColumn`, row bounds dilacak via
`onGloballyPositioned`+`positionInRoot()`/`localToRoot()`. `SongRow` dapat param `modifier`
baru (default aman, pemanggil lain tak berubah). **Scope cuma tab Lagu** — belum di
`PlaylistScreen.kt`, grup album/artis, atau tab Favorit; kalau diminta lagi di tempat lain,
pola row-bounds-tracking ini reusable. **Belum diverifikasi visual di device** — gesture custom
Compose (kombinasi `pointerInput` container + `combinedClickable` per-row) kelas bug yang
idealnya dicek langsung di HP: pastikan scroll normal tab Lagu masih mulus (harusnya aman,
`detectDragGesturesAfterLongPress` cuma ambil alih setelah threshold ~500ms tekan-lama
terpenuhi), dan sweep beneran nyeleksi rentang yang benar saat LazyColumn di-scroll +
sweep dalam satu gesture yang sama (kasus rows recycle di tengah drag — belum ditest).
Detail: `CHANGELOG.md` Batch 70.

**Batch 69 (Fix tombol Play/Pause tak kelihatan + artwork notifikasi/pill kosong)** — User
laporkan 5 bug dari 1 sesi (screenshot + deskripsi), 2 sudah diperbaiki, 3 masih OPEN (perlu
info tambahan atau konfirmasi user sebelum lanjut):
- **FIXED — Play/Pause button di Now Playing tak kelihatan/box kosong**: `contentColor`-nya
  salah ambil dari `colorScheme.background` (warna halaman) bukan dari `animatedAccent` (warna
  lingkaran tombol sendiri) — begitu keduanya senasib gelap/terang, ikon menyatu jadi invisible.
  Fix pakai pola luminance yang sama dgn `MiniPlayerBar.kt`. `NowPlayingScreen.kt`.
- **FIXED — Artwork kosong di notifikasi/lock-screen pill**: Media3 `BitmapLoader` bawaan gak
  tahu cara baca `song.uri` (URI file audio, bukan gambar) — bug sekelas yg Batch 68 perbaiki
  di Coil, tapi loader beda yg gak ikut kesentuh. Fix: `SongArtBitmapLoader` baru di
  `PlaybackService.kt`, didaftar via `setBitmapLoader()`. **Sekarang ada 4 loader artwork
  terpisah** (Coil/`AudioArtFetcher`, widget/`WidgetUpdater`, `AccentColorExtractor`, MediaSession/
  `SongArtBitmapLoader`) — kalau skema URI artwork berubah lagi, ke-4-nya wajib digrep & dicek.
- **OPEN — Widget "gak ada perubahan sama sekali" biarpun ganti tema**: kode `WidgetUpdater`/
  `PlayerViewModel`/layout widget (Batch 68) sudah diaudit ulang di sesi ini — SECARA KODE
  terlihat benar (baca `ThemeStore`, panggil `updateAll()` di kedua setter, `widget_root` id +
  `widget_background_light.xml` ada). Kemungkinan besar user masih test pakai APK versi lama
  (build Batch 68 belum sempat diinstall ulang) — **perlu konfirmasi user sudah rebuild+install
  APK terbaru sebelum diasumsikan masih bug**.
- **OPEN — Player stuck/looping tanpa sebab**: TIDAK ADA data diagnostik dari laporan ini
  (cuma screenshot UI, bukan log). Minta user reproduce lalu export log terbaru lewat "Repack ke
  Dokumen" (Batch 64) begitu kejadian, baru bisa didiagnosis.
- **OPEN — "Slide to choose" multi-select lagu dari playlist buatan**: feature request (bukan
  bug) — swipe/drag multi-select belum ada sama sekali di playlist manapun, saat ini memang
  cuma tap satu-satu. Perlu klarifikasi desain interaksi dari user sebelum diimplementasi
  (mis. long-press lalu drag, atau checkbox mode).
Detail lengkap: `CHANGELOG.md` Batch 69.

**Batch 68 (Fix album art hilang total — regresi Batch 67 — + widget tak sinkron ganti tema)** —
2 bug user laporkan dari 1 sesi debug. (1) Album art hilang di SEMUA lagu di seluruh UI (Library/
Home/MiniPlayerBar/NowPlaying): Batch 67 arahkan `AlbumArt` ke `song.uri` tapi lupa Coil butuh
model yang memang bisa didekode sebagai gambar — `song.uri` itu file audio, jadi Coil selalu
gagal decode & jatuh ke ikon fallback utk semua lagu (persis skenario "belum diverifikasi visual"
yang ditulis Batch 67 sendiri). Fix: `AudioArtFetcher.kt` baru — custom Coil Fetcher yang cegat
MIME `audio/*` lalu ekstrak art pakai `loadThumbnail()`/`MediaMetadataRetriever` (pola yang sama
dgn widget/PlaybackService/AccentColorExtractor yang TIDAK kena bug ini krn mereka bypass Coil).
Didaftarkan di `AudioPlayerApplication.kt`. (2) Widget tidak pernah redraw saat user ganti tema:
`PlayerViewModel.setThemeIdentity/setThemeMode` gak pernah panggil `WidgetUpdater.updateAll()`
(0 call path, dikonfirmasi grep), DAN widget layout gak punya palet terang sama sekali. Fix:
`widget_background_light.xml` baru + `widget_root` id di 2 layout widget + `WidgetUpdater.kt`
baca `ThemeStore` (fungsi baru `resolveIsDark()`) + `PlayerViewModel` panggil `updateAll()`
setelah ganti tema. **Pola relevan utk batch depan**: kalau ada state global baru (tema, dll)
yang perlu tercermin di widget, JANGAN asumsikan widget otomatis ikut — selalu grep
`WidgetUpdater.updateAll(` call sites dulu. **Belum diverifikasi visual di device** — sama
seperti Batch 67, ini kelas bug yang idealnya dicek langsung di HP sebelum dianggap tuntas.
Detail lengkap: `CHANGELOG.md` Batch 68.

**Batch 67 (Fix root cause FileNotFoundException album art — playback+widget+UI)** — Root
cause ditemukan lewat analisa `log_*.txt` (fitur Repack ke Dokumen, Batch 66): URI legacy
`content://media/external/audio/albumart/$albumId` (tabel cache sering kosong di API 29+)
dipakai di 8 tempat — `AccentColorExtractor`, `PlaybackService`, `PlayerViewModel` (semua sudah
tercatat error di log), TAPI JUGA di `Utils.kt`'s `AlbumArt` composable (dipakai
MiniPlayerBar/LibraryScreen/HomeScreen/NowPlayingScreen) yang gagalnya senyap krn Coil
`error{}` fallback ke ikon musik — celah ini baru ketahuan dari audit kode, bukan dari log.
Fix: semua diganti `song.uri` (URI file audio sendiri, didukung `loadThumbnail()` native).
`albumArtUri()` helper lama di `Utils.kt` dihapus total, tidak ada pemanggil tersisa (sudah
di-grep ulang). Tidak ada protected asset disentuh. **Belum diverifikasi visual di device** —
kalau art masih hilang di device tertentu setelah rebuild, kemungkinan besar song itu memang
tidak punya embedded art sama sekali (bukan lagi bug URI). Detail: `CHANGELOG.md` Batch 67.

**Batch 66 (Fix feedback "Repack ke Dokumen" ketutup ModalBottomSheet)** — Root cause:
Snackbar dari `onInfoMessage` dirender oleh `Scaffold` di `MainActivity`, tapi
`ModalBottomSheet` ada di layer terpisah DI ATASNYA, jadi Snackbar itu invisible selama sheet
terbuka -> user kira tombol macet. `DiagnosticLogSheet.kt` (tidak ada protected asset) sekarang
punya banner sukses/gagal inline di dalam sheet (state `exportResult`, auto-hilang 2.5 detik),
haptic dibedakan sukses vs gagal. `onInfoMessage` tetap dipanggil sbg fallback. **Pola ini
relevan utk sheet/dialog lain yg juga cuma pakai `onInfoMessage`** — kalau nanti ada keluhan
serupa, cek dulu apakah komponennya jenis Modal (Bottom Sheet/Dialog) sebelum nambah state
lokal lain. Detail: `CHANGELOG.md` Batch 66.

**Batch 65 (Fix nama APK rilis bentrok saat CI di-rerun manual)** — Root cause "unduhan
duplikat": tag/nama file APK (`v1.0.$COUNT-release`) cuma dari jumlah commit, jadi re-run
manual workflow di commit yg sama = nama file identik = HP bikin "(1).apk" duplikat.
`.github/workflows/build.yml` (edit parsial, protected): tag jadi
`v1.0.$COUNT-release-run${{ github.run_number }}` — unik per run CI. `appVersionName`/
`appVersionCode` APK (dari `gitCommitCount()`) TIDAK berubah. Detail: `CHANGELOG.md` Batch 65.

**Batch 64 (Tombol Log Diagnostik: Salin -> Repack ke Dokumen)** — `DiagnosticLogSheet.kt`
tombol copy-clipboard diganti `AppLogger.exportLogToDocuments(context)`: tulis snapshot log
saat ini ke `log_<timestamp>_<uuid>.txt` di `Documents/AudioPlayer/logs` (MediaStore API 29+,
folder sama dgn crash_*.txt, no permission baru). FIFO retensi 20 file scoped prefix `log_`
(fungsi baru `enforceExportLogRetention`, terpisah dari retensi 50 file `crash_*.txt` yang
sudah ada — tidak disentuh). Icon tombol ContentCopy → Archive. Tidak ada protected asset
disentuh. Detail lengkap di `CHANGELOG.md` Batch 64.

**Batch 63 (Ganti total aksen tembaga → Titanium+Silver metalik + baseline Skeu tidak identik
lagi)** — 2 instruksi: ganti total accent tembaga Skeu → Titanium+Silver metalik, dan semua
tema custom wajib visual otonom tanpa baseline identik. `SkeuDarkAccent` (tembaga, sejak
Batch 53) dihapus permanen → `SkeuAccent` (silver-gray) + token `TitaniumDark`/
`SilverHighlight` baru. Undertone hangat Skeu (krem/parchment) ikut digeser dingin (platinum/
silver) di `Color.kt` — konsekuensi koherensi desain dari ganti keluarga logam. Ambient wash
Skeu (baru Batch 62, dulu 3-stop identik dgn Tactile) diganti struktur 4-stop `colorStops`
custom ("brushed metal streak") — Tactile tidak disentuh. `MainActivity.kt` (edit parsial,
protected) rename `SkeuDarkAccent`→`SkeuAccent` + brush baru pakai spread-operator vararg
(`Brush.linearGradient(*arrayOf(...))`). **Belum diverifikasi visual** — palet & efek streak
baru, tanpa referensi device. Detail lengkap di `CHANGELOG.md` Batch 63.

**Batch 62 (Vibes radikal lepas batasan mode + CI compile time dipangkas drastis)** — 2
instruksi digabung. (1) Ambient root wash (Midnight Blue Tactile, dulu digated ke mode gelap
di Batch 61) sekarang trait IDENTITAS murni — tampil di kedua mode tanpa gate, alpha mode
terang jauh lebih tinggi (kontras terbalik). Skeu dapat ambient wash tembaga sendiri utk
pertama kali (dulu selalu flat). Bevel `tactileEmboss()`/`skeuEmboss()` alpha dinaikkan
signifikan di kedua mode, sengaja menyimpang dari nada "restrained" spec asli atas instruksi
eksplisit user. `MainActivity.kt` disentuh (edit parsial, protected) — var `tactileRootBrush`
→ `identityRootBrush`. **Belum diverifikasi visual** — tuning baru, terutama shadow Tactile
dark 0.90f cukup ekstrem sesuai literal "radikal".
(2) CI compile time: `gradle.properties` (caching/parallel/configureondemand/incremental +
heap naik ke 3072m), `.github/workflows/build.yml` (edit parsial, protected) — checkout
partial-clone (`filter: blob:none`), 2 invocation Gradle (test lalu build) digabung jadi 1
(`testDebugUnitTest assembleRelease` sekali jalan, fail-fast tetap terjaga tanpa flag
tambahan), step diurut ulang (decode keystore + determine version duluan karena tidak butuh
Gradle). TIDAK menyentuh minify/shrinkResources release (risiko integritas rilis, bukan
waktu compile) atau langkah publish GitHub Release itu sendiri. Detail lengkap di
`CHANGELOG.md` Batch 62.

**Batch 61 (Pisah total identitas tema dari mode: Tactile & Skeu otonom di Light/Dark)** — User
koreksi Batch 60: identitas Tactile/Skeuomorphism (dulu hardcode gelap permanen) harus dicabut
dari 1 mode & dikendalikan langsung oleh toggle mode yang sama dgn Apple. `AppTheme` enum lama
(5 nilai campur identity+mode) dihapus total, diganti `ThemeIdentity` (APPLE/TACTILE/
SKEU_DARK_LITE) + `ThemeMode` (SYSTEM/LIGHT/DARK) independen. 8 file disentuh: `Color.kt` (token
LIGHT baru utk Tactile & Skeu — desain baru, belum pernah dilihat di device), `Theme.kt`
(colorsFor 4 skema, `LocalIsDarkTheme` CompositionLocal baru, `isTactileTheme()`/`isSkeuTheme()`
diubah dari compare `background`→`primary` — **fix wajib**, kalau tidak selalu `false` di mode
terang), `TactileDepth.kt` & `BlurUtils.kt` (emboss/glass baca `LocalIsDarkTheme`),
`ThemeStore.kt` (1 key → 2 key + migrasi otomatis dari key lama, user existing tidak kehilangan
preferensi), `PlayerViewModel.kt` (1 StateFlow → 2 independen), `MainActivity.kt` (edit parsial,
protected — semua ref `AppTheme.*` diganti, Midnight Blue ambient wash digated `&& isDarkTheme`),
`SettingsScreen.kt` (toggle mode berlaku ke semua identitas + card identitas preview pakai mode
aktif real-time). Detail lengkap di `CHANGELOG.md` Batch 61. **Belum diverifikasi visual/compile**
(tidak ada kotlinc/emulator) — brace/paren balance seimbang, grep konfirmasi 0 `AppTheme` aktif
tersisa & 0 call site lain yang perlu ikut diubah (signature publik helper tidak berubah).
**Sengaja TIDAK dikerjakan**: nilai token warna LIGHT baru belum divalidasi visual — kandidat
polish lanjutan begitu dicoba di device fisik.

**Batch 60 (Rombak arsitektur picker tema: card select-only → Switch on-off Light/Dark)** — User
minta sektor tema di Settings diubah dari card select-only 1 arah jadi toggle on-off fleksibel
untuk Light/Dark. 1 file disentuh (`SettingsScreen.kt`), `AppTheme` enum/`Theme.kt`/`ThemeStore.kt`
TIDAK diubah (storage key & data model sama persis, tidak perlu migrasi). Ganti trio card
System/Light/Dark dengan `ThemeModeToggleSection` (2 `Switch` M3: "Ikuti Sistem" + "Mode Gelap",
saling disable/enable sesuai state). Card Tactile/Skeu Dark Lite tetap ada di bawahnya (custom
identity, dark-only, di luar cakupan toggle Light/Dark). Detail lengkap di `CHANGELOG.md` Batch 60.
**Belum diverifikasi visual/compile** (tidak ada kotlinc/emulator di sini) — brace/paren balance
dicek otomatis (seimbang).

**Batch 59 (Skeu "otonom" — tuntaskan gap identitas + filter pending jadi 1 batch low-risk)** —
2 instruksi digabung: (1) user observasi: semua tema custom yang pernah dikerjakan selalu ada
sisa "flat/hybrid" yang bikin identitasnya nggak benar-benar otonom; (2) gabungkan seluruh
daftar pending (dikirim balasan sebelumnya) jadi 1 batch atomic, TAPI hanya yang low-risk.
Hasil audit pending: dari 6 item, 4 di antaranya ditolak masuk batch ini karena memang bukan
low-risk (lihat "Sengaja TIDAK dikerjakan" di bawah) — sisa 2 area yang benar-benar aman
dieksekusi kebetulan JUGA persis instruksi (1): titik-titik `isTactileTheme()`-only yang masih
menyisakan Skeu di cabang default Apple (flat, tanpa bevel sendiri) — pola yang sama sudah
terbukti aman 6x di Batch 58, di-generalisasi lagi ke titik yang tersisa. 4 file kode disentuh:
- `HomeScreen.kt` (`ContinueListeningCard`) — kartu pertama yang kelihatan di Beranda, dulu
  Tactile-only, Skeu sekarang dapat `skeuEmboss()` sendiri.
- `LibraryScreen.kt` (banner undo-sembunyikan-lagu) — sama, dulu Tactile-only.
- `NowPlayingScreen.kt` (`AlbumArtHero`) — permukaan terbesar di seluruh app (piringan album
  280dp), dulu Tactile-only (border bevel + shadow custom), Skeu jatuh ke cabang shadow polos
  Apple tanpa border sama sekali. Sekarang dapat border+shadow versi sendiri (SkeuHighlight
  0.16f / SkeuShadow 0.40f — lebih kuat catch-light-nya & lebih rendah shadow-nya dari Tactile,
  konsisten dengan prinsip desain Skeu sejak Batch 57/58).
- `MiniPlayerBar.kt` — **perbaikan arsitektur, bukan cuma nambah cabang baru**: outer Box bar
  ternyata satu-satunya titik di app yang memasang `skeuEmboss()` DAN `frostedGlass()` di
  modifier chain yang sama. Karena `frostedGlass()` untuk Skeu sudah full-opaque sejak Batch 58,
  background+border `skeuEmboss()` di situ selalu ketutup total oleh `frostedGlass()` yang
  digambar belakangan — persis definisi "tidak otonom, masih hybrid" yang dikeluhkan: identitas
  Skeu sendiri secara visual tidak pernah benar-benar sampai ke layar di titik itu. Fix: Skeu
  sekarang skip `frostedGlass()` di Box ini (`skeuEmboss()`-nya sendiri sudah background+border
  lengkap), Tactile/Apple TIDAK diubah (Tactile emang sengaja hybrid glass-over-emboss by
  spec/nama, Apple gak punya background lain selain dari `frostedGlass()`).
- **Tactile sengaja TIDAK disentuh sama sekali batch ini** — nama/identitas/spec Tactile
  ("Premium AMOLED Hybrid Glassmorphism", spec eksternal yang di-supply user sendiri di Batch 53)
  memang literal "hybrid" secara desain, bukan cacat. Observasi user soal "selalu ada unsur
  flat/hybrid" ditafsirkan sebagai gap penerapan tema-nya sendiri yang belum tuntas ke semua
  layar (leftover default Apple), bukan permintaan menghapus konsep hybrid dari Tactile.
- **Sengaja TIDAK dikerjakan (ditolak karena bukan low-risk, sesuai instruksi eksplisit user)**:
  1) Shared-element transition & 2) Pull-to-refresh — keduanya butuh bump Compose BOM dari
  2024.05.00, versi baru bisa mempengaruhi komponen lain yang sudah jalan, tanpa compiler risiko
  terlalu tinggi. 3) 339 string hardcode → `strings.xml` dan 4) ~340 literal `.dp` → token
  spacing — sweep mekanis skala besar lintas puluhan file, tanpa compiler untuk verifikasi tiap
  perubahan risikonya kumulatif tinggi meski satu-satu kecil. 5) Lirik otomatis (fetch dari
  internet) — fitur baru (API/network call, state loading/error baru), bukan polish existing
  code, scope-nya beda kelas. 6) `TactileButton`/`TactileSwitch`/`TactileSlider` custom (spec
  §7/§12, item yang paling langsung menjawab keluhan "flat" tapi juga paling berisiko) — custom
  draw + custom drag-gesture handling untuk slider seek/volume adalah kontrol paling sering
  dipakai di app, kalau salah taruh bisa merusak fungsi inti; ditolak dari batch **low-risk** ini
  secara sadar, tetap kandidat batch terpisah kalau user mau ambil risikonya. 7) "belum pernah
  diuji di device fisik" — bukan kerja kode, tidak bisa dieksekusi dari sisi ini.
- **Belum diverifikasi visual/compile** — sama seperti semua batch tema sebelumnya (tidak ada
  `kotlinc`/emulator di environment ini); brace/paren balance dicek otomatis di ke-4 file Kotlin
  (seimbang), grep konfirmasi 0 duplikat import.

**Batch 58 (polish Skeuomorphism Dark Lite: hilangkan sisa glassmorphism)** — User lapor lewat
screenshot: kesan glassmorphism di app masih terlalu kuat, minta tema custom terbaru (Skeu,
Batch 57) di-polish sampai "matang". Root cause: `frostedGlass()` (`BlurUtils.kt`) — satu helper
yang dipakai SEMUA panel besar di app (mini player, tiap bottom sheet, kartu Home/Library) — masih
menerapkan tint tembus-pandang (alpha 0.92) + border rim lembut ala kaca ke Skeu juga, padahal
identitas Skeu (Batch 57) eksplisit "panel solid, bukan lapisan kaca". Ditemukan juga bug kedua:
`embossSurface()` (`TactileDepth.kt`, mesin bersama `tactileEmboss()`/`skeuEmboss()`) hardcode
alpha border/shadow yang sama untuk kedua tema, diam-diam menimpa alpha `SkeuHighlight`/
`SkeuShadow` yang sudah didesain beda (lebih kuat/lebih rendah) sejak Batch 57 — jadi bevel Skeu
selama ini tidak pernah benar-benar tampil sesuai desainnya sendiri. 5 file disentuh, 1 tema
kohesif (atomic — semua bagian saling terkait, satu fix "de-glass Skeu"):
- `BlurUtils.kt` — Skeu sekarang full opaque (alpha dipaksa 1f, bukan lagi ikut default 0.92)
  regardless parameter caller (grep dicek: tidak ada call site yang pernah pass alpha eksplisit,
  jadi aman); border Skeu diganti dari `SkeuHighlight→SkeuEdge` (rim kaca lembut, sama pola
  dengan Tactile) ke `SkeuHighlight→SkeuShadow` (bevel ukiran kontras lebih tinggi) + lebar border
  1.dp→1.5.dp khusus Skeu. Tactile TIDAK disentuh sama sekali (identitasnya memang kaca).
- `Color.kt` — `SkeuEdge` dihapus (0 call site setelah perubahan di atas, grep-confirmed),
  komentar token diperbarui.
- `TactileDepth.kt` — `embossSurface()` param alpha border/shadow yang tadinya literal hardcode
  di dalam body sekarang jadi parameter dengan default = literal Tactile lama persis (jadi
  `tactileEmboss()` byte-identik, tidak berubah sama sekali karena tidak pass parameter baru ini).
  `skeuEmboss()` sekarang pass angka sendiri (highlight 0.10/0.045, shadow-border 0.24/0.12,
  shadow-drop 0.55/0.28 normal/pressed) — akhirnya benar-benar memakai intensitas yang sudah lama
  didokumentasikan di komentar Color.kt tapi tidak pernah efektif.
- `MiniPlayerBar.kt` + `NowPlayingScreen.kt` — `skeuEmboss()` (sudah ada sejak Batch 57 tapi cuma
  dipakai 1 tempat) sekarang dipasang di: bar mini player + tombol play/pause mini (40dp), tombol
  play/pause utama Now Playing (68dp, + shape rounded-square sama seperti Tactile dapat di Batch
  55), dan `GestureIndicatorBadge` (badge geser kecerahan/volume — sebelumnya masih Surface
  translusen 0.9f alpha untuk Skeu, cabang `isTactile`-only yang kelewat). Catatan arsitektur:
  pada mini player bar luar, `skeuEmboss()` dan `frostedGlass()` dipasang di Modifier chain yang
  sama (persis pola lama Tactile) — karena `frostedGlass()` sekarang opaque untuk Skeu, background/
  border gradient `skeuEmboss()` di situ secara visual ketutup oleh background/border
  `frostedGlass()` yang digambar belakangan (drop-shadow-nya sendiri tetap kelihatan, drawBehind-
  nya tidak ikut ke-clip/ketutup) — sama seperti yang sudah lama terjadi di Tactile, bukan
  regresi baru, tapi juga bukan yang paling efisien; kalau mau elevasi/scale animasi
  `skeuEmboss()` (press feedback) benar-benar terlihat penuh di titik itu, lepas `frostedGlass()`
  dari Box yang sama adalah kandidat polish lanjutan.
- `README.md` — paragraf Skeu diperbarui (opaque, border ukiran, titik-titik baru yang dapat
  `skeuEmboss()`).
- **Belum diverifikasi visual/compile** — sama seperti setiap batch tema sebelumnya (tidak ada
  `kotlinc`/emulator di environment ini), diverifikasi lewat baca-manual + brace/paren balance
  check (seimbang di semua 5 file Kotlin yang disentuh) + grep konfirmasi 0 call site tersisa
  untuk `SkeuEdge` dan 0 call site `frostedGlass()` yang pass alpha eksplisit.
- **Sengaja TIDAK dikerjakan**: `HomeScreen.kt`/`LibraryScreen.kt` juga punya cabang
  `isTactile`-only (kartu Continue Listening, row Library) tapi audit menunjukkan cabang else-nya
  sudah pakai `MaterialTheme.colorScheme.surface` TANPA `.copy(alpha=...)` — sudah opaque dari
  awal, bukan sumber kesan glassmorphism, jadi di luar scope perbaikan spesifik batch ini (kandidat
  batch "wire skeuEmboss() ke sana juga" terpisah kalau user mau, sama seperti histori Tactile
  Batch 45-55 yang juga bertahap).

**Batch 57 (toggle tema custom baru: Skeuomorphism Dark Lite)** — User minta tema custom ketiga
(kedua di luar keluarga Apple/Light/Dark/System), tanpa spec eksternal — palet charcoal netral
hangat + panel timbul + aksen tembaga (`#CB8B4B`), sengaja dibedakan dari Tactile (AMOLED-glass,
hue biru). Detail lengkap di `CHANGELOG.md` Batch 57; ringkas:
- `AppTheme.SKEU_DARK_LITE` baru (`Theme.kt`) + token warna (`Color.kt`) + `SkeuDarkColors`/
  `SkeuDarkShapes`/`isSkeuTheme()` (`Theme.kt`) + `skeuEmboss()` (`TactileDepth.kt`, refactor
  `tactileEmboss()` jadi wrapper `embossSurface()` privat bersama — signature/perilaku publik
  `tactileEmboss()` tidak berubah) + `frostedGlass()` 3-arah (`BlurUtils.kt`) + pratinjau hidup di
  `SettingsScreen.kt` + catch-light NavigationBar digeneralisasi (`MainActivity.kt`).
- `ThemeStore.kt`/`AppTheme.fromStorageKey()`/`SettingsScreen.kt`'s `AppTheme.entries.toList()`
  loop TIDAK disentuh — sudah generic sejak awal, toggle baru otomatis muncul & tersimpan.
- **Belum diverifikasi visual/compile** (sama seperti setiap batch tema sebelumnya, tidak ada
  `kotlinc`/emulator di sini) — grep exhaustiveness check atas semua `when (AppTheme)` di codebase
  sudah dilakukan (3 titik, semua sudah mencakup entry baru), brace/paren balance semua 6 file
  yang disentuh seimbang.
- **Sengaja TIDAK dikerjakan** (sama presedan Tactile Batch 45-48→55): `skeuEmboss()` belum
  dipasang ke kontrol individual (play/pause, slider) — baru dipakai 1 tempat (baris pemilih
  tema). Root ambient gradient khusus Skeu — disengaja flat (bukan gap, keputusan desain: identitas
  Skeu adalah panel solid, bukan lapisan kaca). Tipografi custom Skeu — reuse `AppleTypography`.

**Batch 56 (versionCode/versionName reset — bukan tema/fitur)** — User minta reset angka versi
app karena `1.0.<total commit history>` sudah kelihatan besar/tidak sedap dipandang di picker.
`gitCommitCount()` (`app/build.gradle.kts`) & CI (`.github/workflows/build.yml` "Determine version
name") diubah dari `git rev-list --count HEAD` (total history) ke `git rev-list --count
v-reset..HEAD` dengan fallback ke `HEAD` kalau tag belum ada — jadi angkanya restart dari kecil
TANPA rewrite/squash git history (log/blame lama tetap utuh). **Wajib 1x setup manual di Termux
setelah ZIP ini di-push** (belum dijalankan otomatis dari sini karena butuh akses git remote user):
```
git tag v-reset && git push origin v-reset
```
Tanpa tag ini, kedua sisi (gradle & CI) otomatis fallback ke hitungan lama (tidak breaking,
cuma belum ke-reset). 2 file protected disentuh (`app/build.gradle.kts`,
`.github/workflows/build.yml`) — edit parsial saja (fungsi diganti, sisa file & signing config
tidak disentuh).

**Batch 55 (Tactile identity polish, atomic change)** — User minta polish tema custom Tactile
biar perbedaannya sama tema utama (Apple) makin kelihatan. Audit codebase: warna/tipografi/shape/
kaca sudah dibedakan sejak Batch 49-54 (lihat entri Batch 53 di bawah), tapi tombol play/pause —
kontrol paling sering dilihat sepanjang sesi dengar musik (mini bar + Now Playing) — masih render
byte-identik di kedua tema (`FilledIconButton` circle default M3, tanpa bevel apa pun). Itu satu
titik terbesar yang bikin identitas Tactile "hilang" begitu musik diputar. Detail lengkap di
`CHANGELOG.md` Batch 55; ringkas:
- Tombol play/pause utama (`NowPlayingScreen.kt`, 68dp) & mini player (`MiniPlayerBar.kt`, 40dp):
  Tactile sekarang `MaterialTheme.shapes.medium` (rounded-square) + `tactileEmboss()`, Apple tetap
  `CircleShape` + shadow biasa seperti sebelumnya — tidak berubah.
- `AlbumArtHero`'s border: `Brush.verticalGradient` (peninggalan sebelum aturan diagonal spec §9
  Batch 53) diganti `Brush.linearGradient` — satu-satunya border Tactile yang belum ikut arah
  cahaya diagonal top-left→bottom-right yang dipakai di tempat lain.
- **Belum diverifikasi visual/compile** — sama seperti setiap batch tema sebelumnya (tidak ada
  `kotlinc`/emulator di environment ini), diverifikasi lewat baca-manual + brace/paren balance
  check (seimbang di kedua file yang disentuh).
- **Sengaja TIDAK dikerjakan**: custom thumb/track untuk kedua `Slider` (seek bar utama & volume
  dalam-aplikasi) — masih M3 default identik di kedua tema. Butuh slot `thumb`/`track` composable
  (custom draw, bukan sekadar modifier tempel), risiko lebih tinggi tanpa compiler — kandidat
  batch polish berikutnya.

**Batch 54 (technical debt pass, bukan tema/fitur)** — User minta gabungkan seluruh daftar
technical debt murni-kode (hasil audit statis: grep + baca file, bukan dari testing) dengan
technical debt yang sudah tercatat di segmen "Belum selesai / dalam pengerjaan" README.md, jadi
1 batch atomic change. Dikerjakan (pakai batch-limit exception "Atomic Change" — 10 file
tersentuh, lebih dari limit normal 10 file/1 modul, tapi ini satu perubahan logis yang saling
terkait, bukan beberapa fitur independen digabung paksa):
- **`isTactileTheme()` helper baru** di `Theme.kt` — mengganti 6 duplikat manual
  `MaterialTheme.colorScheme.background == TactileBackground` (di `BlurUtils.kt`, `HomeScreen.kt`,
  `LibraryScreen.kt`, `MiniPlayerBar.kt`, `NowPlayingScreen.kt` x2) jadi satu pemanggilan fungsi.
  Perilaku identik, cuma DRY.
- **11 inline fully-qualified reference** (`com.rudi.audioplayer.ui.theme.X` ditulis langsung di
  tengah kode alih-alih lewat `import`) dibersihkan jadi import biasa — 5 file (`MiniPlayerBar.kt`,
  `LibraryScreen.kt`, `HomeScreen.kt`, `NowPlayingScreen.kt`, `MainActivity.kt`). Sudah dicek grep:
  tidak ada FQN inline tersisa di codebase (import legit seperti `import
  com.rudi.audioplayer.ui.theme.frostedGlass` tidak disentuh, itu memang sudah bentuk yang benar).
- **5 token warna dead code dihapus** dari `Color.kt`: `TactileControl`, `TactileControlPressed`,
  `GlassPressed`, `GlassWhite`, `TactileMutedText` — 0 call site (grep-confirmed sebelum dihapus).
  Disiapkan Batch 53 untuk komponen `TactileButton`/`TactileSwitch`/`TactileSlider` yang belum
  pernah dibangun; ditinggalkan komentar penjelas supaya batch masa depan yang benar-benar
  membangun komponen itu tahu kenapa tokennya hilang dan tinggal re-derive dari spec saat itu.
- **`Spacing.kt` (file baru)** — token `Radius` (xs/sm/md/ml/lg/xl/xxl/xxxl/hero, literal
  4/10/12/14/16/18/20/24/28dp) sebagai fondasi sistem spacing/shape terpusat (spec §19). Semua 32
  titik `RoundedCornerShape(N.dp)` literal di seluruh codebase (8 file: `FeatureHintBanner.kt`,
  `HomeScreen.kt`, `LibraryScreen.kt`, `MiniPlayerBar.kt`, `NowPlayingScreen.kt`,
  `SettingsScreen.kt`, `Theme.kt`) dimigrasi ke token ini via script Python (regex match-and-
  replace per file, bukan manual satu-satu) — sudah diverifikasi grep nihil literal
  `RoundedCornerShape(N.dp)` tersisa, dan brace/paren count tiap file yang disentuh seimbang
  sebelum/sesudah (proxy sanity-check tanpa compiler).
- **Sengaja TIDAK dikerjakan batch ini** (didaftar transparan, bukan disembunyikan):
  - **Migrasi penuh ~340 literal `.dp` non-radius sisanya** (padding/size/offset/blur radius) ke
    `Spacing.kt` — beda dengan corner-radius yang punya segelintir nilai berulang jelas, mayoritas
    literal ini one-off/context-specific (misal shadow offset `9.dp` di hero art `NowPlayingScreen`
    yang memang di-tuning presisi untuk 1 tempat). Memaksakan token di sini berisiko kehilangan
    presisi yang disengaja, dan sweep sebesar itu tanpa `kotlinc` untuk verifikasi adalah risiko
    nyata — alasan yang sama persis yang sudah dipakai proyek ini sendiri di Batch 31/35.
  - **Ekstraksi penuh 339 string literal ke `strings.xml`** (untuk i18n) — sudah didaftar duluan
    di README.md "Belum selesai / dalam pengerjaan" dengan alasan identik (refactor mekanis
    sebesar itu, ratusan titik tersebar di banyak file, tidak aman tanpa compiler untuk verifikasi
    argumen format-string/urutan parameter tetap benar). Tidak diulang batch ini.
  - **Pull-to-refresh gesture di Library** & **Shared-element transition sungguhan** (mini player
    → Now Playing) — dua-duanya butuh bump Compose BOM dari 2024.05.00, yang berisiko ke komponen
    lain yang sudah stabil jalan. Ini bukan technical debt murni kode (butuh keputusan
    dependency-version terpisah), jadi tidak dipaksakan masuk batch "atomic" ini.
- **Belum diverifikasi build/compile** — sama seperti setiap batch sebelumnya, environment kerja
  ini tidak punya `kotlinc`. Verifikasi dilakukan lewat grep menyeluruh (nihil sisa referensi lama)
  + brace/paren balance check per file, bukan compile sungguhan.

**Batch 53** — User kirim spec baru `compose-amoled-hybrid-glass-final.md`, minta diterapkan
100% ke tema custom Tactile (menggantikan palet flat Midnight Blue literal Batch 52 sepenuhnya —
spec ini secara eksplisit mendaftar "a full Midnight Blue theme" sebagai anti-pattern di §24, jadi
Batch 52 sendiri sekarang jadi contoh yang harus dihindari). Diterapkan ke SEMUA sektor yang
langsung terlihat user (bukan cuma 6 pilar/screen utama): 4 file token/util pusat
(`Color.kt`/`Theme.kt`/`BlurUtils.kt`/`TactileDepth.kt`) + `MainActivity.kt` (root ambient
gradient + navbar). Karena setiap screen/sheet (Home, Library, MiniPlayer, NowPlaying, Settings,
semua bottom sheet) sudah merutekan visualnya lewat `frostedGlass()`/`tactileEmboss()`/
`MaterialTheme.colorScheme` alih-alih warna literal per-file, rewrite terpusat ini otomatis
menjangkau semuanya tanpa perlu menyentuh tiap file screen satu-satu (dikonfirmasi lewat grep:
15 file `ui/*.kt` semua konsumsi salah satu dari ketiga titik pusat itu).
- `Color.kt` — repaint total mengikuti hierarki spec §2 (AMOLED > glass > Midnight Blue ambient >
  tactile > accent): `TactileBackground` sekarang AMOLED near-black §3 literal 0xFF030508 (bukan
  flat Midnight Blue 0xFF191970 lagi), `TactileSurface`/`TactileSurfaceVariant` sekarang §5
  `GlassBase`/`GlassElevated` literal 0xFF0A0F16/0xFF101722, `TactileText`/`TactileSecondaryText`
  ganti ke §16 `TextPrimary`/`TextSecondary` 0xFFEAF0F8/0xFFAAB5C4, `TactileAccent` sekarang §17
  `AccentBlue` 0xFF6670FF. Token baru: `AmoledSurface`, `GlassPressed`, `GlassWhite`,
  `TactileMutedText`, `MidnightBlue` (0xFF191970 — sekarang HANYA ambient, tidak lagi jadi
  background), `MidnightBlueAmbientAlpha` (0.06f). `TactileHighlight`/`TactileEdge` naik dari
  0.055f/0.035f ke §5 literal `GlassHighlight`/`GlassBorder` 0.065f/0.035f. `TactileError`/
  `TactileSuccess` tidak berubah (spec ini juga tidak beri token error/success literal, sama
  seperti setiap batch tema custom sebelumnya).
- `Theme.kt` — `TactileColors` re-wire ke token baru (fungsinya sama, cuma nilai berubah lewat
  Color.kt); `AppTheme.TACTILE.description` diperbarui dari deskripsi "Midnight Blue taktil" ke
  deskripsi glass-first yang sesuai identitas baru. `onPrimary` tetap `Color.White` (AccentBlue
  0xFF6670FF luma ≈0.49, masih di bawah threshold 0.55).
- `BlurUtils.kt` — `frostedGlass()`'s `edgeBrush` untuk Tactile diganti dari solid
  `primary.copy(alpha=0.22f)` ("accent trim line", melanggar §8 "border harus subtle, bukan
  accent") ke `Brush.linearGradient(TactileHighlight, TactileEdge)` — diagonal top-left→bottom-
  right sesuai §9 "Lighting model", bukan lagi warna aksen solid di sekeliling setiap panel glass.
- `TactileDepth.kt` — `tactileEmboss()`'s bevel gradient ganti dari `verticalGradient` ke
  `linearGradient` (default Offset.Zero→Offset.Infinite = diagonal top-left→bottom-right, sesuai
  §9) memakai token `TactileSurfaceVariant`/`TactileSurface` yang sekarang sudah berarti glass
  bukan bevel opaque. Border/shadow alpha di-re-tune mengikuti base token baru (0.065f/0.70f).
- `MainActivity.kt` — root `Surface` untuk Tactile sekarang `Color.Transparent` + `Box.background`
  dengan `Brush.linearGradient(background, MidnightBlue.copy(alpha=MidnightBlueAmbientAlpha),
  AmoledSurface)` — ini SATU-SATUNYA tempat `MidnightBlue` dipakai sebagai warna nyata di layar
  (§6 "Correct use": hanya sebagai ambient gradient, bukan flat surface), tema lain tidak berubah
  (tetap flat `colorScheme.background`). NavigationBar `tonalElevation` Tactile diturunkan dari
  12.dp ke 6.dp — pada 12.dp, `surfaceTint` (=TactileAccent, biru) membuat seluruh nav bar
  kelihatan biru dulu sebelum "AMOLED glass" (melanggar §2 "Golden Rule" dan §15), 6.dp tetap
  kelihatan terangkat (Level 2 glass) tanpa wash aksen mendominasi.
- **Belum diverifikasi visual** (sama seperti setiap batch tema custom sebelumnya, environment
  kerja ini tidak punya `kotlinc`/emulator): build-test asli + verifikasi visual device disarankan
  sebelum rilis, khususnya diagonal border baru (`Brush.linearGradient` di `BlurUtils.kt`/
  `TactileDepth.kt`) dan root ambient gradient (`MainActivity.kt`) — grep sudah dicek nihil
  referensi token lama yang terlewat, tapi belum diverifikasi visual.

**Batch 52** — User kirim spec baru `compose-skeuomorphism-lite-midnight-blue.md`, minta
diterapkan 100% ke tema custom Tactile (menggantikan palet hybrid-glass Batch 51 sepenuhnya —
spec header eksplisit 2x pakai kata "Literal" dan "Mandatory visual baseline: Literal Midnight
Blue (#191970) — MANDATORY", jadi §2 dipakai sebagai nilai literal langsung, sama seperti setiap
batch tema custom sebelumnya). 6 file kode disentuh, 1 tema kohesif (atomic) — **kali ini justru
menghapus 2 fitur** (gradient root & glass overlay) karena spec baru tidak lagi memintanya, bukan
menambah fitur baru seperti Batch 51.
- `Color.kt` — seluruh palet Tactile diganti dari §2 spec literal: `TactileBackground` 0xFF191970
  (flat, satu stop — bukan pasangan gradient lagi), `TactileSurface` 0xFF161665 dan
  `TactileSurfaceVariant` 0xFF20207A (**keduanya opaque 0xFF lagi**, beda fundamental dari Batch
  51 yang translusen 0xCC/0xB8), `TactileText`/`TactileSecondaryText` ganti ke 0xFFF0F1FF/
  0xFFBFC2E6, `TactileAccent` 0xFF7278FF (ganti dari 0xFF5B9DFF Batch 51 — lebih ungu-biru,
  lebih gelap). `TactileHighlight`/`TactileEdge`/`TactileShadow` balik ke basis `Color.White`/
  `Color.Black` polos ber-alpha rendah (0.055f/0.035f/0.65f, literal §2) — bukan lagi warna
  ber-tint sendiri seperti Batch 51. `TactileBackgroundTop` dan `TactileGlassOverlay` (dua token
  BARU Batch 51) **dihapus** — spec ini tidak punya padanan token untuk gradient stop kedua atau
  glass wash, jadi tidak ada lagi yang memakainya (lihat perubahan MainActivity.kt/BlurUtils.kt
  di bawah). `TactileControl`/`TactileControlPressed` (masih tidak ada pemanggil) direfresh
  nilainya (0xFF23238A/0xFF0F0F4A) biar konsisten sama hierarki permukaan baru yang lebih terang.
- `MainActivity.kt` — **kebalikan dari perubahan fungsional terbesar Batch 51**: root `Surface`
  untuk Tactile balik jadi `MaterialTheme.colorScheme.background` datar (bukan
  `Color.Transparent` + `Box` gradient diagonal lagi) — spec §2 hanya kasih `Background` sebagai
  token tunggal ("Near-black AMOLED" flat), tidak ada pasangan stop gradient seperti spec Batch
  51 (`DarkBackgroundTop`/`DarkBackgroundBottom`), jadi tidak ada lagi yang perlu diekspresikan
  lewat `Brush.linearGradient`. `contentColor` tetap eksplisit seperti sebelumnya (tidak pernah
  bergantung ke bug class Batch 48 di kedua arah perubahan ini). Import `Color` Compose yang jadi
  tidak terpakai ikut dihapus. NavigationBar catch-light line tidak disentuh kodenya (otomatis
  re-warna lewat Color.kt, cuma komentar diperbarui).
- `BlurUtils.kt` (`frostedGlass()`, dipakai 6 file) — cabang khusus Tactile (tint dipakai apa
  adanya + layer `TactileGlassOverlay`) **dihapus**, Tactile sekarang balik pakai jalur generik
  `tint.copy(alpha = alpha)` yang sama dengan tema lain (persis seperti Batch 50) — karena
  `TactileSurface`/`TactileSurfaceVariant` sudah opaque lagi, tidak ada lagi translucency
  spec-literal yang perlu "dijaga" dari ketimpaan alpha generik.
- `TactileDepth.kt` (`tactileEmboss()`) — signature tidak berubah (8 titik pemanggil otomatis
  ikut). `borderTopAlpha` diturunkan 0.09/0.04 → 0.055/0.025 (disamakan persis ke alpha literal
  `TactileHighlight` yang baru jauh lebih rendah dari spec sebelumnya). `shadowAlpha` diturunkan
  0.68/0.35 → 0.65/0.33 (disamakan ke alpha literal `TactileShadow` yang baru). `borderBottomAlpha`
  (0.30/0.15) TIDAK diubah — tidak ada literal spec yang mengharuskan angka baru di situ.
- `Theme.kt` — `TactileColors` tetap `darkColorScheme()` (spec §13 masih larang light-mode
  fallback); **`onPrimary` diganti `Color.Black` → `Color.White`** — `TactileAccent` baru
  (0xFF7278FF) simple-luma ≈0.52, di bawah ambang 0.55 yang dipakai `MiniPlayerBar.kt`/batch-batch
  sebelumnya untuk memilih hitam vs putih, beda dari Batch 51 (0xFF5B9DFF, ≈0.59) yang masih di
  atas ambang. `onTertiary` tetap `Color.Black` (`TactileSuccess` tidak berubah, masih di atas
  ambang). `resolveIsDark(TACTILE)` tetap `true`. `storageKey` TIDAK berubah (`tactile_lite`) —
  repaint keempat atas identitas Tactile yang sama, bukan tema baru. Deskripsi tampilan diupdate
  ("Midnight Blue taktil terprogram… bevel fisik") biar tidak menyesatkan user yang masih baca
  deskripsi lama soal "kaca"/translusen.
- **Di luar cakupan, disengaja**: sama seperti batch-batch sebelumnya, spec §7/§12 minta komponen
  `TactileButton`/`TactileSwitch`/`TactileSlider` custom penuh di `ui/components/` — tidak
  dikerjakan batch ini, scope tetap atomic (recolor + pelepasan 2 fitur gradient/glass yang tidak
  diminta lagi, bukan komponen baru).
- **Belum diverifikasi runtime asli** (tidak ada compiler Android di environment kerja) —
  analisis statis + brace/paren balance dicek otomatis di semua file yang disentuh (0 selisih
  kurung/brace). Prioritas sesi berikutnya SAMA seperti batch-batch sebelumnya (belum pernah
  dirender sama sekali sejak Batch 50): build-test asli + verifikasi visual device, khususnya
  bahwa root screen benar-benar flat (bukan gradient sisa) dan tidak ada titik UI lain yang masih
  berasumsi permukaan Tactile translusen (mis. custom drawing yang sengaja mengandalkan tembus
  pandang glass Batch 51 — grep sudah dicek nihil, tapi belum diverifikasi visual).

**Batch 51** — User kirim spec baru `compose-skeuomorphism-lite-hybrid-glass-dark-blue.md`, minta
diterapkan 100% ke tema custom Tactile (menggantikan palet AMOLED-hitam Batch 50 sepenuhnya —
spec §1.1 eksplisit: "Pure/AMOLED-black styling is not the target", jadi ini bukan sekadar
"biruin dikit", tapi token diambil ulang dari §2 spec secara literal, plus 1 fitur baru yang
belum pernah ada di batch manapun sebelumnya: gradient atmosfer di root, bukan cuma flat color).
6 file kode, 1 tema kohesif (atomic).
- `Color.kt` — seluruh palet Tactile diganti dari §2 spec: `TactileBackground` 0xFF050B18 (flat
  fallback utk `colorScheme.background`/semua guard `isTactile`), `TactileBackgroundTop` BARU
  0xFF0A1630 (stop atas gradient root), `TactileSurface` 0xCC101D35 dan `TactileSurfaceVariant`
  0xB8142745 — **keduanya sekarang translusen** (alpha 0xCC/0xB8 ≈ 80%/72%), beda fundamental
  dari Batch 50 yang opaque; `TactileGlassOverlay` BARU 0x142E6AA3 (wash biru alpha sangat
  rendah, dipakai `BlurUtils.kt`); `TactileAccent` 0xFF5B9DFF (ganti dari 0xFF4DA3FF Batch 50);
  `TactileHighlight`/`TactileEdge`/`TactileShadow` semua diberi warna dasar sendiri oleh spec
  (bukan generic Color.White/Black ber-alpha rendah lagi seperti Batch 50) — 0xFFEAF4FF/0.07f,
  0xFF8FB9E8/0.10f, 0xFF020817/0.68f, semua literal §2. `TactileText`/`TactileSecondaryText`
  TIDAK berubah (spec ini kebetulan pakai nilai identik Batch 50). `TactileControl`/
  `TactileControlPressed` (masih tidak ada pemanggil, disiapkan untuk `ui/components/` masa
  depan) direfresh nilainya biar konsisten arah biru-gelap baru, bukan literal spec.
- `MainActivity.kt` — **perubahan fungsional terbesar batch ini**: root `Surface` untuk Tactile
  sekarang `Color.Transparent` (bukan `colorScheme.background` datar), dengan `Box` gradient
  diagonal (`Brush.linearGradient`, `TactileBackgroundTop` → `colorScheme.background`) dipasang
  tepat di dalamnya — mengimplementasikan mandat spec §1.1/§2/§8 "deep navy→dark-blue gradient
  background, bukan AMOLED-black dominan" yang sebelumnya sama sekali tidak ada (Batch 49/50
  cuma flat color). **Bukan kebangkitan trik Batch 48**: `contentColor` tetap selalu eksplisit
  di kedua cabang, jadi tidak ada jalur `contentColorFor(Transparent)` yang bisa jatuh ke
  `Unspecified` — beda sifat dari root cause Batch 48. NavigationBar catch-light line tidak
  disentuh kodenya (`TactileHighlight` re-warna otomatis lewat Color.kt).
- `BlurUtils.kt` (`frostedGlass()`, dipakai 6 file) — sekarang bercabang: untuk Tactile,
  `tint` (sekarang sudah translusen by-design dari Color.kt) dipakai APA ADANYA + layer
  `TactileGlassOverlay` di atasnya, bukan lagi `.copy(alpha = 0.92f)` yang dulu **membuang**
  translusensi asli spec dan menggantinya jadi hampir opaque. Tema non-Tactile tidak berubah
  sama sekali (cabang lama tetap jalan persis seperti sebelumnya).
- `TactileDepth.kt` (`tactileEmboss()`) + `NowPlayingScreen.kt` (AlbumArtHero) — **nol
  perubahan logika**, cuma komentar diperbarui. Kedua file sudah mereferensikan token by-name
  (`TactileSurfaceVariant`/`TactileSurface`/`TactileHighlight`/`TactileShadow`), jadi begitu
  Color.kt berubah jadi translusen+tinted, kedua file ini otomatis ikut memancarkan efek
  hybrid-glass tanpa disentuh — persis pola yang sama dipakai Batch 50 utk merecolor tanpa
  restructuring.
- `Theme.kt` — `TactileColors` tetap `darkColorScheme()` (spec §13 masih larang light-mode
  fallback), tidak ada perubahan struktur; `resolveIsDark(TACTILE)` tetap `true`; deskripsi
  tampilan tema di `AppTheme.TACTILE` diupdate ("Kaca biru-gelap terprogram… permukaan
  translusen") biar tidak menyesatkan user yang masih baca deskripsi lama "Bevel gelap AMOLED".
  `storageKey` TIDAK berubah (`tactile_lite`) — ini bukan identitas tema baru, cuma repaint
  ketiga atas identitas Tactile yang sama, jadi tidak perlu migrasi/fallback seperti Batch 49.
- **Di luar cakupan, disengaja**: sama seperti Batch 49/50, spec §7/§12 minta komponen
  `TactileButton`/`TactileSwitch`/`TactileSlider` custom penuh di `ui/components/` — tidak
  dikerjakan batch ini, scope tetap atomic (recolor + 1 fitur gradient-root, bukan komponen
  baru). Juga **belum diaudit**: `colorScheme.surface`/`surfaceVariant` kini translusen secara
  default di seluruh scheme (bukan cuma lewat `tactileEmboss()`/`frostedGlass()`), jadi Card/
  Surface M3 polos manapun di layar lain (Home/Library/Settings/dialog) yang masih pakai
  `colorScheme.surface` langsung tanpa lewat 2 helper itu ikut jadi translusen otomatis —
  ini SESUAI spec §8 (glass di seluruh hierarchy), tapi belum diverifikasi visual apakah ada
  titik yang jadi kurang terbaca kalau kebetulan tidak ada apa-apa di baliknya (misal
  BottomSheet/Dialog yang dirender di window terpisah dari root gradient Box) — prioritas
  audit visual sesi berikutnya, bersamaan dengan verifikasi runtime asli di bawah.
- **Belum diverifikasi runtime asli** (tidak ada compiler Android di environment kerja) —
  analisis statis + brace/paren balance dicek manual di semua file yang disentuh. Prioritas
  sesi berikutnya SAMA seperti Batch 50 (belum pernah dirender sama sekali): build-test asli +
  verifikasi visual device, KHUSUSNYA gradient root baru (`Offset.Infinite` untuk diagonal
  linear gradient — API-nya benar per dokumentasi Compose, tapi arah/kecepatan gradiennya
  sendiri belum pernah dilihat langsung) dan titik translucency-surface-tanpa-background di
  atas.

**Batch 50** — User kirim spec baru `compose-skeuomorphism-lite-dark.md`, minta diterapkan 100%
ke tema custom Tactile (menggantikan palet TERANG Batch 49 sepenuhnya — spec §1.1 eksplisit:
"Do not simply invert a light theme. Design the tactile lighting model specifically for dark
surfaces", jadi ini bukan sekadar "gelapkan" nilai lama, tapi token diambil ulang dari §2 spec
secara literal). 6 file kode + 3 file dokumentasi, 1 tema kohesif (atomic).
- `Color.kt` — seluruh palet Tactile diganti dari nol: `TactileBackground` 0xFF05070A (AMOLED),
  `TactileSurface` 0xFF0B0F14, `TactileSurfaceVariant` 0xFF111720, `TactileText` 0xFFE8EEF5,
  `TactileSecondaryText` 0xFFA8B3C0, `TactileAccent` 0xFF4DA3FF (biru dingin, ganti tembaga
  hangat lama), `TactileHighlight`/`TactileEdge`/`TactileShadow` (Color.White/Black ber-alpha
  rendah, literal spec §2) — semua ini nilai literal dari contoh kode spec, bukan tebakan.
  Ditambah 2 token baru dari tabel §2 yang belum ada pemanggilnya batch ini tapi disiapkan untuk
  komponen tactile masa depan: `TactileControl`/`TactileControlPressed` (tidak ada nilai literal
  di spec, diturunkan sendiri agar konsisten dengan hierarki gelap-terang §2). `TactileError`/
  `TactileSuccess` juga tidak ada literal spec, dipilih manual agar cocok skema biru-dingin.
- `TactileDepth.kt` (`tactileEmboss()`) — signature tidak berubah (8 titik pemanggil otomatis
  ikut), tapi seluruh alpha border/shadow ditulis ulang mengikuti aturan dark-mode spec §4
  ("Do NOT use a bright Color.White border") — border top/bottom turun dari 0.9/0.45 ke
  0.09/0.30 (normal), 0.35/0.20 ke 0.04/0.15 (pressed); shadow drop-nya sendiri justru
  dipertahankan dekat alpha penuh (0.65 spec-literal, bukan diturunkan) karena background sudah
  nyaris hitam — **pelajaran dari saga Matte Noir Batch 39-44 dipakai lagi di sini**: bayangan
  hitam-di-atas-hitam yang terlalu tipis alpha-nya akan hilang total, bukan sekadar "restrained".
- `Theme.kt` — `TactileColors` ganti dari `lightColorScheme()` ke `darkColorScheme()`;
  `resolveIsDark(TACTILE)` dibalik `false` → `true` (otomatis membalik ikon status bar/nav bar
  jadi terang lewat `MainActivity.kt` yang sudah ada, tidak perlu disentuh manual); `onPrimary`/
  `onTertiary` dipilih `Color.Black` lewat aturan luminance yang sama dipakai `MiniPlayerBar.kt`
  (>0.55 → hitam) karena `TactileAccent`/`TactileSuccess` baru cukup terang.
- `NowPlayingScreen.kt` (AlbumArtHero) + `MainActivity.kt` (garis catch-light NavigationBar) —
  2 titik manual (bukan lewat `tactileEmboss()`) direcolor & alpha-nya diselaraskan ke aturan
  yang sama: NavigationBar 0.9/0.05 → 0.10/0.02 (dulu praktis garis putih nyaris opaque,
  langsung melanggar §4); AlbumArtHero border 0.9/0.40 → 0.12/0.32, shadow 0.28 → 0.55, glow
  aksen lagu 0.5 → 0.42 (spec §9 izinkan glow di elemen selected/active seperti ini, tapi tetap
  "restrained").
- `BlurUtils.kt` — trim aksen di `frostedGlass()` (dipakai 6 file) alpha 0.35 → 0.22, sekadar
  penyesuaian restraint karena aksen biru baru terasa lebih terang dari tembaga lama di alpha
  yang sama.
- **Di luar cakupan, disengaja**: spec §7/§12 minta komponen `TactileButton`/`TactileSwitch`/
  `TactileSlider` custom penuh di `ui/components/` — batas ini SAMA seperti batas Batch 49
  (slider/toggle/switch tetap Material3 polos), tidak diperluas batch ini supaya scope tetap
  atomic. Kalau user mau cakupan itu juga, itu kerja terpisah yang lebih besar (file baru,
  bukan cuma recolor).
- **Belum diverifikasi runtime asli** (tidak ada compiler Android di environment kerja) —
  analisis statis + brace/paren balance dicek manual di semua file yang disentuh. Prioritas
  sesi berikutnya: build-test asli + verifikasi visual device untuk tema Tactile versi gelap
  ini (belum pernah dirender sama sekali, sama seperti nasib versi terang Batch 49 sebelum
  sempat diverifikasi).

**Batch 49** — User minta hapus SEMUA jejak tema custom "Matte Noir" lama sampai bersih, lalu
terapkan tema custom baru murni dari `compose-skeuomorphism-lite.md`. Selesai: 11 file, atomic
change. Matte Noir (semua warna, shape, typography, `MatteDepth.kt`, enum `AppTheme.MATTE`)
DIHAPUS total, bukan direname doang. Diganti `AppTheme.TACTILE` — palet TERANG baru (warna
`0xFFF8FAFC`/`0xFFE2E8F0` di `Color.kt` adalah literal contoh kode di spec §1, bukan
interpretasi bebas), `TactileDepth.kt` (`tactileEmboss()`, logic sama dengan hasil Batch 46/47
yang sudah sesuai spec, cuma direcolor). Bonus: root `Surface(color=Transparent)` trick di
`MainActivity.kt` (biang kerok Batch 48) DIHAPUS TOTAL, bukan cuma di-patch — root Surface
sekarang selalu opaque + `contentColor` eksplisit utk semua tema, jadi kelas bug itu tidak bisa
terulang lagi sama sekali kedepannya. Storage key preferensi tema berubah dari `matte_noir` ke
`tactile_lite` — user lama yang masih ada preferensi Matte tersimpan otomatis fallback ke
SYSTEM (bukan crash, disengaja). Lihat CHANGELOG.md Batch 49 untuk detail penuh + daftar 11
file. **Belum diverifikasi runtime asli** — batch ini cakupannya paling besar sejauh ini, jadi
build-test asli + verifikasi visual device jadi prioritas MUTLAK sebelum lanjut fitur lain.
Grep akhir `Matte` di seluruh kode aktif = 0 hasil (cuma komentar historis).

**Batch 48** — User kirim screenshot: teks di LockScreen (judul "Masukkan PIN" + digit keypad)
render HITAM di atas background nyaris-hitam Matte, nyaris tak terbaca. Root cause: root
`Surface(color = Color.Transparent)` di `MainActivity.kt` (trik ambient-glow Batch 40) bikin
`contentColorFor(Transparent)` jatuh ke `Unspecified` → fallback ke `LocalContentColor` yang
belum pernah di-set (`AudioPlayerTheme()` cuma bungkus `MaterialTheme(...)`, tidak pernah pakai
Surface) → default mentah Compose: `Color.Black`. LockScreen kena polos karena tidak punya
Surface/Card lokal sendiri untuk "menyelamatkan" diri (beda dari Library yang tiap row list-nya
py Surface sendiri). Fix saat itu: `contentColor = MaterialTheme.colorScheme.onBackground`
eksplisit di Surface root. **Catatan: fix ini sudah DIGANTIKAN total oleh Batch 49** yang
menghapus trik `Transparent` itu sepenuhnya, jadi detail fix Batch 48 ini historis saja.

**Batch 47** — Hotfix compile error Batch 46 dari `log_fail_104.zip`: `MatteDepth.kt` pakai
`by animateDpAsState(...)` / `by animateFloatAsState(...)` tapi lupa
`import androidx.compose.runtime.getValue`. Fix: tambah import. 1 baris, 1 file. Exact match ke
error log, bukan tebakan — confidence tinggi. Prioritas berikutnya masih sama: user
verifikasi tampilan tema Matte hasil Batch 46 di device asli (belum pernah, sejak Batch 40).

**Batch 46** — User kirim spec desain sendiri (`compose-skeuomorphism-lite.md`) karena tema
Matte hasil Batch 40-44 dinilai "jelek banget asli". `matteEmboss()` di `MatteDepth.kt` ditulis
ulang total mengikuti 3 poin spec (gradient top-down + bevel border, animasi tekan-fisik
sungguhan, intensitas diturunkan/flat untuk card struktural). Signature tidak berubah jadi
5 pemanggil lama ikut otomatis; 1 titik manual (AlbumArtHero, `NowPlayingScreen.kt`) ditulis
ulang manual juga biar konsisten + sekalian buang native `Modifier.shadow` yang sudah lama
terbukti invisible di background gelap. Lihat CHANGELOG.md Batch 46 untuk detail penuh. **Belum
diverifikasi device** — kalau user masih bilang jelek, JANGAN tambah shadow/gradient lagi (pola
gagal berulang Batch 40-44), minta screenshot + bagian spesifik yang salah.

**Batch 45** — User lapor bug "gak sinkron" di segmen signature key matching. Ditemukan:
`ApkSignatureChecker.inspect()` ambil `signingCertificateHistory.firstOrNull()` (cabang
single-signer) — array ini oldest→newest per dokumentasi resmi `SigningInfo`, jadi
`firstOrNull()` salah ambil sertifikat ORIGINAL, bukan yang AKTIF sekarang. Untuk app yang
pernah key rotation, ini bikin hasil MATCH/MISMATCH di UI tidak sinkron dengan keputusan
instalasi Android yang sebenarnya. Fix: ganti ke `.lastOrNull()`. Lihat CHANGELOG.md Batch 45
untuk detail. **Belum diverifikasi runtime asli** — kalau user masih lapor "gak sinkron"
setelah ini, minta contoh 2 APK spesifik yang dipakai test (terutama apakah salah satunya
pernah rilis dengan key rotation) sebelum menebak sisi lain.

**Batch 44** — Fix Batch 43 ternyata salah juga: `drawOutline` **tidak pernah ada** di API
Compose (dicek langsung ke source AOSP `DrawScope.kt` — cuma ada drawLine/drawRect/
drawRoundRect/drawCircle/drawOval/drawArc/drawPath/drawPoints). Fix benar: `Path().apply {
addOutline(outline) }` lalu `drawPath(path, color)`. Lihat CHANGELOG.md Batch 44. **Ini fix
build ketiga berturut-turut untuk fitur shadow yang sama (Batch 42→43→44) — kalau build masih
gagal setelah ini, JANGAN tebak nama API lagi; cari signature persis di source AOSP dulu
sebelum tulis kode. Kalau build akhirnya sukses, shadow visual-nya SENDIRI masih belum pernah
diverifikasi di device sama sekali sejak Batch 42 — itu prioritas berikutnya.**

**Batch 43** — Hotfix build gagal dari Batch 42: `log_fail_5.zip` user tunjukkan
`compileDebugKotlin` gagal, `Unresolved reference: drawOutline` di `MatteDepth.kt` (2 baris).
Sebab: `drawOutline` itu extension function `DrawScope`, bukan method bawaan — lupa diimpor
padahal `translate` di baris sebelahnya sudah benar. Fix: tambah 1 baris import. Belum ada
perubahan logika/tampilan lain dari Batch 42. **Prioritas sesi berikutnya masih sama seperti
Batch 42: verifikasi shadow manual ini benar-benar kelihatan di device asli — build sekarang
seharusnya sukses, tapi efek visualnya sendiri belum pernah dirender sama sekali.**

**Batch 42** — Hotfix Batch 41 (lagi): shadow `matteEmboss()` masih invisible di device asli
setelah elevation dinaikkan (bukti screenshot). Root cause lebih dalam: opacity shadow native
`Modifier.shadow` punya cap rendah yang tidak bisa didorong lewat elevation di background
gelap pekat. Fix: buang native shadow total, ganti manual `drawBehind` + `Outline` 2-layer
(`MatteUmbra` alpha 0.30f/0.5f) offset kanan-bawah — kontras sekarang dikontrol alpha kita
sendiri, bukan platform. Lihat CHANGELOG.md Batch 42 untuk detail lengkap. **BELUM
diverifikasi di device — ini prioritas #1 sesi berikutnya. Kalau masih kurang kontras, jangan
otak-atik alpha kecil-kecilan lagi (sudah 2 fix gagal dengan pola itu); screenshot ulang dan
ukur kontras aktual (color picker) sebelum nebak angka lagi.**

**Batch 41** — Hotfix Batch 40 dari screenshot render asli: shadow `matteEmboss()` nyaris tak
kelihatan (`MatteUmbra` cuma ~12 unit lebih gelap dari `MatteBackground`, dan tint warna native
shadow cuma ubah hue bukan opacity). Fix di `MatteDepth.kt` + 5 call-site: elevation dinaikkan
di semua titik (bukan ganti warna — opacity shadow dikontrol elevation), highlight/umbra alpha
gradient dinaikkan. Lihat CHANGELOG.md Batch 41 untuk angka lengkap. **Belum diverifikasi ulang
di device — kalau masih kurang kontras, jangan naikkan elevation lagi tanpa batas; pertimbangkan
manual scrim/blur overlay independen dari native shadow API sebagai gantinya.**

**Batch 40** — Lanjutan langsung Batch 39 ("semua area belum kerasa premium", bukan 1 titik).
User diberi penjelasan 4 gaya kedalaman dulu (neumorphism/skeuomorphic/glass gelap/elevasi+
gradient terarah), pilih kombinasi neumorphism ringan + elevasi/gradient cahaya terarah. Dibuat
1 helper terpusat `Modifier.matteEmboss()` (`MatteDepth.kt`, baru) — shadow dua-warna terarah
(`MatteUmbra`) + gradient diagonal `MatteHighlight → MatteSurface → MatteUmbra` + border
catch-light 1dp, gabungan neumorphism (border edge menangkap cahaya) + directional light
(gradient, bukan tonalElevation datar) dalam 1 modifier reusable. Dipasang di 7 titik: mini
player, ContinueListeningCard Home, undo-snackbar Library, GestureIndicatorBadge Now Playing,
ThemeOptionCard Matte di Settings (showcase depth di picker-nya sendiri), NavigationBar
(catch-light line 2px), plus `AlbumArtHero` dapat treatment lebih kuat manual (shape token +
shadow ganda + border, tidak pakai helper generik karena sudah punya accent-glow per-lagu
sendiri). `matteDepthBrush()` root alpha dinaikkan 0.10f→0.22f (versi lama kalah dari
kecerahan layar asli). 9 file (1 baru+8 edit), 1 tema kohesif. **Tetap murni analisis
statis, belum pernah dirender** — kalau "kureng" lagi di test HP, JANGAN ulangi pola nambah
shadow/gradient lagi (sudah 2x coba begitu), curigai sesuatu yang cuma kelihatan di layar
asli (kontras, ukuran relatif DPI) — lihat CHANGELOG.md Batch 40 untuk detail.

**Batch 39** — Respons user test Batch 38 di HP asli ("masih kureng"): Matte Noir dikasih
efek kedalaman visual. Root cause: `darkColorScheme()`/`lightColorScheme()` M3 diam-diam
isi `surfaceTint` (mekanisme utama M3 buat tonal-elevation depth) dengan ungu baseline
kalau tidak disebut eksplisit — sekarang eksplisit tiap skema (`AppleAccent`/`MatteAccent`).
Plus: `frostedGlass()` (`BlurUtils.kt`, dipakai 6 file) shape-nya ikut
`MaterialTheme.shapes.large` (dulu hardcode 24dp, sekarang 8dp di Matte = boxy) + trim
tembaga di border; `matteDepthBrush()` baru — radial gradient dipasang di root `Surface`
`MainActivity.kt` (Matte-only, dibungkus `Box`, `Surface` jadi transparent supaya gradient
kelihatan); `NavigationBar` tonalElevation 12dp khusus Matte; `MiniPlayerBar.kt` shape
disamakan + shadow ambientColor/spotColor ditinta tembaga elevasi 16dp. 5 file kode +
2 doc. **PENTING**: murni analisis statis, sama sekali belum pernah dirender — kalau user
lapor "masih kureng" lagi, jangan asumsi solusinya menambah lebih banyak color/shadow tanpa
tahu spesifik bagian mana yang dirasa kurang (radial gradient-nya kurang kuat? shape-nya
belum kerasa? nav bar-nya masih flat? tanya spesifik dulu).

**Batch 38** — 2 hal: (1) fix dokumentasi drift tema (README/PROJECT_STATE masih deskripsikan
"3 tema" lama Ink & Brass/Midnight Bloom/Paper & Ink + klaim status bar dipaksa gelap, padahal
`Theme.kt` sudah lama migrasi ke model SYSTEM/LIGHT/DARK ala Apple dan `MainActivity.kt:187`
sudah ikut tema — persis pola yang diperingatkan Batch 17); (2) atas permintaan eksplisit,
tambah tema ke-4 **Matte Noir** — jadikan keluarga Apple (SYSTEM/LIGHT/DARK) tema utama/default
(tidak berubah, tetap `AppTheme.SYSTEM`), plus satu identitas custom `AppTheme.MATTE` yang
sengaja kebalikan Apple: matte hangat bukan hitam/putih ekstrem, aksen tembaga bukan biru,
judul serif (`FontFamily.Serif`, font sistem — tidak nambah aset font) bukan sans, sudut
4/6/8dp nyaris kotak bukan 14/20/28dp membulat, statis selalu gelap (tidak ikut sistem).
Sekalian fix warna hardcode `SignatureMatcherSheet.kt:96` (`Color(0xFF3FA34D)` →
`MaterialTheme.colorScheme.tertiary`) yang jadi alasan nambah role `tertiary`
(sukses/match, hijau) ke skema Apple (`AppleDarkSuccess`/`AppleLightSuccess`) juga.
9 file disentuh, 1 tema kohesif (theme-system expansion, atomic — enum+scheme+shape+
typography+1 caller+2 doc+1 changelog tak terpisah tanpa saling pecah konsistensi).
`colorsFor()` ganti signature dari `(isDark: Boolean)` jadi `(theme: AppTheme, isDark:
Boolean)` — 1 pemanggil di luar `Theme.kt` (`SettingsScreen.kt:288`) sudah disesuaikan,
dicek tak ada pemanggil lain (`grep` bersih). **Belum diverifikasi build/runtime asli**
(tidak ada compiler Android di environment kerja) — analisis statis + brace/paren balance.

**Batch 37** — Truncation tanpa ellipsis, lanjutan temuan Batch 31 yang dulu ditunda. Audit
21 titik `maxLines = 1` di `ui/*.kt`, 4 gap ditemukan & dikerjakan (2 file): album di grid
Library (`LibraryScreen.kt` ~499), judul lagu di daftar-lagu-dalam-album (~522), judul+artis
di daftar "Lagu Disembunyikan" `FolderManagerSheet.kt` (~179, ~182) — semua ditambah
`overflow = TextOverflow.Ellipsis`, import sudah ada di kedua file. Ditemukan tapi belum
dikerjakan: 356 literal `.dp` ad-hoc tanpa `Spacing.kt` terpusat (scope terlalu besar untuk
1 batch), audit `IconButton` tanpa `contentDescription` (hasil: nihil, semua sudah benar).
**Belum diverifikasi build/runtime asli** (tidak ada compiler Android di environment kerja) —
analisis statis + script brace/paren balance.

**Batch 36** — Arahan melebar: sambil nunggu build hijau, debugging+optimalisasi TETAP jalan
tapi sekarang juga polish UI/UX + detail kecil kenyamanan pemakaian (bukan lagi "stop fitur
baru" ketat ala Batch 34). Audit Settings/Library/mini-player nemu 4 hal, user pilih semua:
- `SetPinDialog` (Settings) kini `KeyboardType.NumberPassword` + `PasswordVisualTransformation`
  — konsisten sama `LockScreen` yang sudah lama polished (dot mask, haptic, shake-on-error).
- `LibrarySearchField` kini punya `ImeAction.Search` + `KeyboardActions` yang nutup keyboard
  via `LocalSoftwareKeyboardController` — logika pencarian (live per keystroke) tidak berubah.
- `MiniPlayerBar` kini punya garis `LinearProgressIndicator` 2dp glanceable-only (bukan
  seekable) di tepi bawah, dari `uiState.position/duration` yang sudah ada. **Cek versi API
  dulu sebelum pakai**: overload `progress: Float` sudah deprecated sejak Material3 1.2.0,
  proyek ini pin compose-bom 2024.05.00 (~Material3 1.2.1) jadi pakai overload lambda
  `progress: () -> Float`.
- Teks "Tentang Aplikasi" di Settings disingkat dari `"AudioPlayer versi 1.0.254 (build 254)"`
  jadi `"AudioPlayer versi 1.0.254"` — commit count yang sama sebelumnya nongol dua kali dalam
  format beda (berantakan+kepanjangan). **Skema penomoran versi (git commit count,
  `app/build.gradle.kts`, protected asset) TIDAK disentuh** — murni string tampilan.

Belum diverifikasi build/runtime asli (tidak ada compiler Android di environment kerja) —
analisis statis + audit versi API manual.

**Batch 35** — Lanjutan arahan Batch 34 (debugging + optimalisasi performa, tanpa fitur baru).
Audit statis menyisir file berisiko tinggi (`PlaybackService`, `PlayerViewModel`,
`MusicRepository`, `AccentColorExtractor`, `EqualizerController`, `AppLogger`,
`CustomFolderScanner`, `ShakeDetector`) plus seluruh `LazyColumn`/`LazyRow` di UI — semua
bersih (key list konsisten, I/O sudah di `Dispatchers.IO`, sensor listener paired start/stop).
1 temuan, dikerjakan atas persetujuan user:
- **Widget jank belum tuntas dari Batch 34 (performa)** — `PlayerWidgetProvider.onUpdate()` &
  `onAppWidgetOptionsChanged()` masih manggil `WidgetUpdater.updateAll()` (decode+crop+round
  bitmap album-art) **langsung di main thread** — Batch 34 cuma mindahin call-site di
  `PlaybackService`, dua call-site di sini kelewat karena `AppWidgetProvider` adalah
  `BroadcastReceiver` biasa, bukan Service, jadi tidak otomatis kebagian `serviceScope`.
  Lebih parah dari sisi `PlaybackService`: `onAppWidgetOptionsChanged` dikomentari sendiri di
  kode lama sebagai "fires live as user drags resize handles" — decode blocking bisa numpuk
  tiap event drag. Fix: `goAsync()` (API standar `BroadcastReceiver` buat kerja lanjut setelah
  callback return tanpa blocking pemanggil) + `providerScope.launch(Dispatchers.IO)`,
  `pendingResult.finish()` di `finally` supaya wakelock broadcast tetap dilepas walau
  `updateAll` throw.

**Belum diverifikasi build/runtime asli** (tidak ada compiler Android di environment kerja) —
hanya analisis statis. `versionName` tetap otomatis dari commit count (tidak disentuh batch
ini).

**Batch 34** — Arahan baru mulai batch ini: **stop fitur baru, fokus debugging +
optimalisasi performa & eksekusi sampai aplikasi matang**. Audit kode nyata (bukan asumsi)
menemukan 2 hal, keduanya dikerjakan sekaligus atas persetujuan user:
1. **Widget jank (performa)** — `pushWidgetUpdate()` di `PlaybackService` decode+crop+round
   bitmap album-art secara síncron di **main thread** (lewat `WidgetUpdater.updateAll`),
   dipanggil tiap `onMediaItemTransition` & `onIsPlayingChanged` — dua event paling sering
   dan paling terlihat user (ganti lagu, tap play/pause). Fix: kerjaan berat dipindah ke
   `serviceScope.launch(Dispatchers.IO)`, dengan `widgetUpdateJob` yang di-cancel sebelum
   relaunch supaya skip/toggle cepat tidak bikin update lama nimpa update baru (race
   kondisi art/state jadi stale). `saveState` (SharedPreferences) tetap di thread pemanggil
   karena `.apply()` sudah async-safe sendiri.
2. **Crash logger drift dari spec (debugging)** — `AppLogger.writePublicCrashLog` ternyata
   sudah lama menyimpang dari spec awal ("FIFO Retention max 50, metadata lengkap
   Version/OS/Model/Timestamp/Thread/StackTrace, nama file pakai UUID"): belum ada UUID di
   nama file (risiko overwrite kalau crash-loop dalam detik yang sama), belum ada retensi
   FIFO sama sekali (folder `Documents/AudioPlayer/logs` numpuk tanpa batas), metadata cuma
   Timestamp+Thread+StackTrace (tidak ada Version/OS/Model — paling penting buat triage
   lintas device/build). Fix: tambah `UUID.randomUUID()` di nama file, tambah blok
   Version/OS/Model ke isi log (versi lewat `PackageInfoCompat.getLongVersionCode`, sudah
   ada dependency `androidx.core:core-ktx` di `build.gradle.kts`), tambah
   `enforceCrashLogRetention()` — query MediaStore by `RELATIVE_PATH` terurut
   `DATE_ADDED DESC`, hapus sisa di luar 50 terbaru. Log privat (`diagnostic_log.txt`,
   trim-by-size) tidak disentuh — itu mekanisme terpisah dan sudah benar.

Kedua fix **belum diverifikasi build/runtime asli** (tidak ada compiler Android di
environment kerja) — hanya analisis statis (grep + baca kode + cross-check API behavior).
`versionName` tetap otomatis dari commit count (tidak disentuh batch ini).

**Batch 33** — Hotfix build gagal dari Batch 32, **diagnosis Batch 32 sendiri ternyata
salah**. `log_fail_94.zip` (build #94): error identik dengan sebelum Batch 32 —
`Unresolved reference: matchParentSize` di `Utils.kt` baris 16 (import) & 61 (call).
Root cause sebenarnya: `matchParentSize()` bukan extension biasa milik `BoxScope`, tapi
**member extension function milik `Modifier`, dideklarasikan di dalam interface
`BoxScope`** (`fun Modifier.matchParentSize(): Modifier` di dalam `interface BoxScope`).
Konsekuensinya dua arah: (1) tidak bisa di-`import` sebagai fungsi top-level — baris
import Batch 31 sudah salah sejak awal, cuma kebetulan tidak dicek compiler sampai
sekarang; (2) pemanggilannya **wajib** tetap pakai prefix `Modifier.` (mis.
`Modifier.matchParentSize()`) walau dipanggil di dalam lambda `Box { }` — bukan dibuang
seperti fix Batch 32. Fix: hapus baris import yang salah, kembalikan pemanggilan ke
`Modifier.matchParentSize()`. **Pelajaran: pesan error compiler "receiver type mismatch"
di Batch 32 sudah menunjukkan signature `Modifier.matchParentSize()` secara eksplisit di
teks errornya sendiri — dibaca sebagai "buang prefix Modifier" padahal maksud sebenarnya
kebalikannya. Ke depan, kalau ada API Compose yang tidak lazim (member extension di
dalam interface, bukan top-level), cross-check ke source resmi
`androidx.compose.foundation.layout.Box.kt`, jangan simpulkan dari nama fungsi di pesan
error saja.** Tidak ada usage lain fungsi ini di project (dicek `grep`). **Belum
diverifikasi build/runtime asli (tidak ada compiler di environment kerja).**
`versionName` tetap otomatis dari commit count (tidak disentuh batch ini).

**Batch 32** — Hotfix build gagal dari Batch 31 (**diagnosis salah, lihat Batch 33 di
atas untuk koreksi**). `log_fail_93.zip` (build #93): `Unresolved reference:
matchParentSize` di `Utils.kt` (`AlbumArt`). Fix yang diterapkan saat itu: buang prefix
`Modifier.` — **ternyata inilah yang menyebabkan build #94 gagal lagi dengan error
identik**, dikoreksi di Batch 33. `versionName` tetap otomatis dari commit count (tidak
disentuh batch ini).

**Batch 31** — Polish UI/UX pass pertama (dari audit statis, user pilih 5 dari daftar temuan
lebih luas). 8 file, 1 tema kohesif. (1) Album-art fallback: helper baru `AlbumArt` di
`Utils.kt` (Coil `SubcomposeAsyncImage`, `error` slot → ikon nada musik di atas
`surfaceVariant`, `loading` slot sengaja kosong biar cover asli gak kedip) menggantikan
`AsyncImage` mentah di 6 titik (Home x2, LibraryScreen x2, MiniPlayerBar, NowPlayingScreen
x2) — sebelumnya lagu tanpa cover art cuma nampilin ruang kosong. Backdrop blur NowPlaying
sengaja `showIcon = false` (ikon bakal jadi gumpalan gak jelas kalau di-blur 60dp). (2) Empty
state disatukan ke komponen `EmptyState` yang sudah ada (LibraryScreen.kt) — sebelumnya 3
pola beda (komponen penuh di Library/Playlist, custom inline icon+text di Home, `Text` abu
polos di Queue/FolderManager); `EmptyState` ditambah parameter `modifier` opsional (default
tetap `fillMaxSize().padding(32.dp)`, gak ubah 5 pemanggilan lama) supaya bisa dipakai di
`LazyColumn` item (Home) dan bottom sheet (Queue, FolderManager) tanpa crash fillMaxSize di
context yang gak punya bounded height. (3) Nama artis kepotong tanpa "..." di 4 lokasi
high-traffic (Home x2, MiniPlayerBar, LibraryScreen) — `maxLines=1` tanpa
`TextOverflow.Ellipsis` — ditambahkan; judul lagu di lokasi sama sengaja dibiarkan (pakai
`basicMarquee()`, overflow ditangani lewat scroll bukan potongan). (4) Tombol tutup
`FeatureHintBanner` 28dp → 40dp (di bawah standar target sentuh aksesibilitas). (5)
`animateItemPlacement()` ditambah ke list folder `FolderManagerSheet` (konsisten dengan
Library/Playlist/Queue). Import Coil `AsyncImage` & ikon `LibraryMusic`/`TextAlign` yang jadi
gak kepake dibersihkan dari 4 file. **Batas jaminan: analisis statis saja (brace/paren
balance dicek manual, tidak ada kotlinc di environment ini) — belum diverifikasi
runtime/emulator, termasuk perilaku `SubcomposeAsyncImage` yang baru pertama kali dipakai di
proyek ini (sebelumnya cuma `AsyncImage` biasa).** Ditemukan tapi belum dikerjakan (di luar
scope batch ini, bukan dipilih user): auditor menemukan judul album di grid Library juga
`maxLines=1` tanpa ellipsis, dan token spacing gak seragam (dp value ad-hoc per file, gak ada
`Spacing.kt`) — keduanya dianggap dampak rendah, disimpan untuk batch lanjutan kalau
diminta. `versionName` tetap otomatis dari commit count (tidak disentuh batch ini).

## Batch 30
**Batch 30** — Otomatisasi & minimalisasi versionName. Ditemukan lewat cek `build.yml` untuk
jawab permintaan ini: `versionName` app (manual, `3.9`) dan tag GitHub Release (otomatis,
`v1.0.<commit-count>`) adalah dua angka tak nyambung. Fix: `versionName` sekarang turunan
`gitCommitCount()` yang sama dipakai `versionCode` (pola `1.0.<commit-count>`) — tidak ada
lagi bump manual. Efek samping tanpa nyentuh CI workflow sama sekali: nomor di app dan nomor
di nama file APK/tag Release sekarang selalu match (dua perhitungan independen, git history
sama). Trade-off jujur: tampilan Settings sekarang dua angka mirip (`1.0.254` / `254`), tidak
ada lagi nomor rilis "kurasi" gaya `3.9`. README § Standar Penomoran Versi + footer
PROJECT_STATE.md disinkronkan. **Baris "`versionName` tetap X" di pelaporan batch sudah
tidak relevan mulai batch ini** — naik otomatis tiap commit.

**Batch 29** — Hotfix build gagal dari Batch 28. `log_fail_91.zip` (build #91) dianalisis:
`androidResources { localeFilters += listOf("en") }` gagal kompilasi — `Unresolved
reference: localeFilters`. Root cause: DSL itu benar secara konsep tapi baru ada di rilis
AGP setelah 8.4.1 (versi project ini), bukan sejak AGP 8.0 seperti diasumsikan Batch 28.
Fix: ganti ke `resourceConfigurations += listOf("en")` di `defaultConfig` — DSL lama, sudah
lama stabil, terverifikasi didukung penuh di AGP 8.4.1. Hasil akhir (buang resource
terjemahan library untuk locale selain "en") identik dengan niat Batch 28. Perubahan Guava→
concurrent-futures dari Batch 28 tidak disentuh (bukan sumber kegagalan). **Pelajaran:
klaim "tersedia sejak AGP X" dari sumber pihak ketiga butuh cross-check ke nomor AGP project
ini persis, bukan digeneralisasi.** `versionName` tetap `3.9`.

**Batch 28** — Optimasi ukuran APK. Audit eksternal baru (skor 9.3/10) taruh ukuran APK di
prioritas #1 — beda urutan dari self-review internal Batch 27 (taruh di posisi terakhir);
dipilih karena satu-satunya yang hasilnya bisa dicek objektif dari ukuran APK output CI
tanpa runner. 2 perubahan: (1) `androidResources { localeFilters += listOf("en") }` di
`app/build.gradle.kts` — buang resource terjemahan AndroidX/Compose/Media3/Coil untuk
locale yang tidak dipakai (app sendiri cuma punya `values/` default, tidak kesentuh). (2)
`com.google.guava:guava` penuh diganti `androidx.concurrent:concurrent-futures:1.3.0` —
cuma pernah dipakai untuk `ListenableFuture`/`SettableFuture` (`PlaybackService`, demi API
session callback Media3) dan `MoreExecutors.directExecutor()` (`PlayerViewModel`).
`SettableFuture` → `CallbackToFutureAdapter`, `directExecutor()` → `Executor { it.run() }`
polos. `ListenableFuture` tetap ada sebagai tipe lewat shim kecil
`com.google.guava:listenablefuture:1.0` (dependency transitif concurrent-futures), bukan
guava penuh lagi. **Batas jaminan: analisis statis saja — ukuran APK sebelum/sesudah baru
kelihatan dari artifact GitHub Release setelah push ini.** Prioritas #2-5 dari audit baru
(error handling, testing, technical debt, maintainability) belum disentuh — batch
berikutnya. `versionName` tetap `3.9`.

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

- **Batch 27 (revisi 1)** — ZIP hasil batch ini sempat dibungkus folder (`AudioPlayer/`), padahal aturan
  proyek jelas: "file proyek langsung di root ZIP". Karena Termux update command sudah
  `unzip -d ~/projects/AudioPlayer/` (destinasi = folder repo itu sendiri), pembungkusan ini
  bikin nested-duplicate — persis pola gejala yang sudah dicatat sebelumnya di sesi lama
  ("Recurring cruft... nested duplicate AudioPlayer-main/ folder"), tapi kali ini sampai
  ter-push ke `main`. **Bug kedua, lebih parah**: exclude flag `-x '*.git*'` saat zip
  ternyata juga mencocokkan `.gitignore` dan `.github/` (substring `.git` ada di keduanya) —
  ini persis bug yang sudah pernah diperbaiki dulu ("`-x "*.git*"` inadvertently excluded
  `.github/workflows/`", lihat `recent-work`), tapi terulang lagi di batch ini karena
  ditulis ulang dari nol tanpa cek riwayat. Akibatnya CI fix Gap 1 (Batch 27 sendiri) nyaris
  tidak pernah benar-benar terkirim. **Fix**: `-x '.git/*'` (scoped, bukan wildcard longgar)
  + isi ZIP di-diff eksplisit terhadap `FILE_MANIFEST.txt` sebelum dikirim, bukan cuma
  dicek di folder kerja sebelum di-zip.

- **Batch 27 (revisi 2)** — CI test run pertama (baru bisa jalan berkat fix revisi 1) langsung
  nemu 9 test gagal dari 53, di 2 file berbeda:
  - `ShakePulseTrackerTest` (4 gagal) — bug asli di `ShakePulseTracker` sendiri: `lastShakeTime`
    default `0L` bikin sample dengan timestamp kecil (test pakai `0L`, `300L`, dst., bukan
    epoch sungguhan) salah kena anggap "masih dalam debounce dari shake di waktu 0". Tidak
    pernah muncul di device asli (`System.currentTimeMillis()` selalu angka besar), tapi tetap
    bug nyata. **Fix**: `lastShakeTime`/`lastPulseTime`/`pulseWindowStart` jadi `Long?`
    (null = belum pernah ada pulsa/shake), bukan default `0L` — benar untuk timestamp
    berapa pun, bukan cuma epoch besar. 1 dari 4 test yang gagal itu ternyata testnya sendiri
    salah hitung pulsa (diperbaiki, lihat komentar di file test).
  - `LibrarySearchIndexTest` (5 gagal) — bug **lama**, sudah ada sejak file ini dibuat,
    baru ketahuan sekarang karena CI baru pertama kali menjalankan test sama sekali:
    `Uri.parse(...)` di unit test JVM murni mengembalikan `null` (bukan placeholder aman
    seperti komentar lama di `build.gradle.kts` klaim), yang crash saat diisi ke field
    `Song.uri` yang non-null. **Fix**: `mockito-core` (dependency baru) + `mock(Uri::class.java)`
    menggantikan `Uri.parse(...)` di fixture test — proyek tetap sengaja menghindari
    Robolectric.
  - Sekalian ditambah: CI upload artifact `log_fail_<run_number>` kalau ada step gagal
    (test atau build) — isi output mentah + laporan JUnit — supaya tidak perlu download
    seluruh raw log lewat UI Actions tiap kali ada kegagalan.
  - **Batas jaminan tetap sama**: fix ini juga belum pernah dijalankan compiler sungguhan,
    baru terverifikasi di push berikutnya.

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
- **Batch 29 — Build gagal total, `androidResources.localeFilters` (Batch 28) tidak dikenal
  compiler**: `e: Unresolved reference: localeFilters` di `app/build.gradle.kts`, ditemukan
  dari `log_fail_91.zip` (build #91) yang diupload user. DSL itu sendiri benar — dokumentasi
  resmi Android menyebutnya cara modern untuk locale filtering — tapi baru tersedia mulai
  rilis AGP setelah 8.4.1, dan project ini terkunci di 8.4.1 (`build.gradle.kts` root). Fix:
  `defaultConfig { resourceConfigurations += listOf("en") }`, DSL lama yang sudah bertahun-
  tahun stabil dan terverifikasi jalan di 8.4.1 (baru deprecated mulai AGP 8.8). **Pelajaran:
  sumber pihak ketiga (blog/artikel) yang bilang "tersedia sejak AGP 8.0+" tidak otomatis
  berarti tersedia di versi AGP project ini — DSL Gradle baru wajib dicek nomor AGP project
  ini persis (di sini: 8.4.1) sebelum dipakai, bukan digeneralisasi dari klaim versi minimum
  yang lebih rendah. Tanpa `kotlinc` di environment kerja, ini juga jenis kesalahan yang
  hanya kelihatan lewat build CI yang benar-benar gagal — bukan dari baca kode statis.**

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
├── ui/theme/  — Apple SYSTEM/LIGHT/DARK (utama) + Matte Noir (custom, kebalikan), warna, tipografi
├── util/      — AppLogger (log diagnostik lokal), ApkSignatureChecker
└── widget/    — Home screen widget (PlayerWidgetProvider, WidgetUpdater)
```

## Konvensi penamaan ZIP & versi
`AudioPlayer-batchN-release.zip` melacak nomor batch percakapan (bukan versionName/versionCode).
`versionCode` dan `versionName` (sejak Batch 30) sama-sama otomatis dari jumlah commit git —
tidak ada lagi bump manual untuk keduanya. Detail lengkap di README.md § "Standar Penomoran
Versi".
