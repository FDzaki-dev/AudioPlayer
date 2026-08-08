# PROJECT_STATE.md

Konteks untuk sesi chat AI mana pun yang melanjutkan proyek ini. Baca file ini dulu sebelum
menyentuh kode apa pun. Detail lengkap tiap batch ada di `CHANGELOG.md`; ringkasan fitur
lengkap ada di `README.md`. File ini adalah ringkasan status + jebakan yang sudah pernah
kejadian, bukan pengganti keduanya.

## Batch terakhir yang selesai
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
