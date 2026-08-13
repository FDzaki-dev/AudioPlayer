# Changelog

## Batch 87 — Hotfix CI FAILED: `const val` tidak valid di badan script .gradle.kts
User upload `log_fail_139.zip` (build-output.log dari CI run yang gagal) — Batch 86 (`versionMajor`/
`commitsPerMinor`) memang belum pernah diverifikasi compile sungguhan (sudah dicatat sebagai
resiko tertinggi di CHANGELOG Batch 86), dan benar gagal:
```
e: app/build.gradle.kts:57:9: Const 'val' are only allowed on top level, in named objects,
   or in companion objects
```
Root cause: `const val` butuh top-level Kotlin FILE/package asli, sebuah `object`, atau
`companion object` — badan SCRIPT `.gradle.kts` (dikompilasi jadi anggota implicit Script class)
BUKAN salah satu dari itu, walau `val` polos di lokasi yang sama persis (`appVersionCode`,
`appVersionName`, dst — semua di file ini) terbukti jalan normal. Asumsi salah dari saya di Batch
86: kebiasaan `private const val` dari kode app Kotlin biasa (sah di dalam class/companion
object) kebawa ke build script, beda konteks.

Fix (1 file, `app/build.gradle.kts`, protected — edit parsial): `private const val` → `val`
polos untuk `versionMajor`/`commitsPerMinor`, drop `private` sekalian biar konsisten sama semua
deklarasi lain di file ini (tidak ada satu pun yang pakai `private`). Formula
`appVersionName` itu sendiri TIDAK berubah, cuma cara deklarasi 2 konstanta pendukungnya.
`.github/workflows/build.yml` (bash, bukan Kotlin) TIDAK kena bug ini sama sekali — sudah benar
dari Batch 86, tidak disentuh lagi.

Brace/paren `build.gradle.kts` dicek ulang, seimbang. **Masih belum ada CI run baru yang
membuktikan fix ini sungguhan lolos** — cuma menghilangkan 1 kesalahan sintaks spesifik yang
sudah dikonfirmasi dari log asli; kemungkinan (kecil) ada lapisan error lain yang baru kelihatan
setelah baris ini lolos compile, belum bisa dipastikan tanpa run CI baru.

## Batch 86 — versionName: "1.0.<count>" statis -> MAJOR.MINOR.PATCH dinamis (klarifikasi dulu)
User: "sekalian juga bump version statis -> otomatis+dinamis" (Batch 84/85). Ambigu — sempat
ditanya balik ke user (ask_user_input_v0) krn `versionCode`/`versionName` SUDAH 100% otomatis
dari git commit count sejak Batch 30/56, jadi bukan itu yang literally "statis". User konfirmasi:
maksudnya skema `versionCode`/`versionName` app itu sendiri, "mungkin mau diubah caranya". Yang
ternyata benar-benar statis: prefix `"1.0."` di `appVersionName = "1.0.$appVersionCode"` — cuma
angka terakhir yang pernah bergerak, MAJOR.MINOR selamanya beku di `1.0` walau development jalan
terus. 3 file:

1. **`app/build.gradle.kts`** (protected, edit parsial): `appVersionName` sekarang
   `"$versionMajor.${appVersionCode / commitsPerMinor}.${appVersionCode % commitsPerMinor}"`.
   MINOR = commit_count / 50, PATCH = commit_count % 50 — genuinely naik seiring waktu
   (`1.0.x` → `1.1.x` → `1.2.x` dst), bukan cuma angka ketiga yang jalan sendirian. MAJOR
   (`versionMajor = 1`) TETAP konstanta manual — SENGAJA, bukan setengah-kerja: nyaris semua
   skema semver "otomatis" (termasuk tooling semantic-release) tetap gate MAJOR di belakang
   sinyal breaking-change manusia, auto-increment MAJOR dari commit count cuma pindah masalah
   "statis" ke tempat yang lebih menyesatkan (angka yg keliatan bermakna padahal enggak).
   `versionCode` (integer urutan install internal Android, tidak pernah dibaca user) TIDAK
   diubah — tetap `gitCommitCount()` mentah, sengaja tidak diturunkan dari
   major/minor/patch biar tidak menambah risiko non-monotonic demi 0 manfaat nyata.
2. **`.github/workflows/build.yml`** (protected, edit parsial) — step "Determine version name"
   HARUS diubah formula yang sama persis (`COMMITS_PER_MINOR=50` di bash, harus manual disamakan
   kalau angka di Gradle pernah diubah — dicatat di komentar KEDUA file), atau tag GitHub
   Release/nama file APK akan drift dari versionName yang benar-benar ke-baked di APK — persis
   invariant yang dijaga sejak Batch 56/30. Tag jadi `v$VERSION_NAME-release-run<N>` (dulu
   `v1.0.$COUNT-release-run<N>`).
3. **`README.md`** — bagian "Standar Penomoran Versi" diupdate contoh & penjelasan formula baru
   (sekalian perbaiki 1 ketidaksesuaian kecil pre-existing yang ketemu pas nulis ulang bagian
   ini: contoh nama APK di dokumen lama nulis `-release.apk` polos, padahal tag sungguhan dari
   Batch 56 sudah `-release-run<N>.apk`, jadi diselaraskan ke pola tag CI yang aktual).

0 file baru, 0 protected asset LAIN disentuh (dotfiles/manifest/keystore semua utuh). Brace/paren
`build.gradle.kts` seimbang, `build.yml` divalidasi `yaml.safe_load`. **Belum diverifikasi
compile/CI sungguhan** (no gradle/emulator/GitHub Actions run di environment kerja ini) — risiko
TERBESAR sejauh ini krn 2 file protected sekaligus tersentuh; prioritas kalau user push: cek 1
run CI penuh, pastikan `Determine version name` step tidak exit dgn error bash (`$((...))`
arithmetic Kotlin-style di Gradle vs bash sudah ditulis terpisah, tapi belum pernah benar-benar
dieksekusi keduanya berdampingan).

## Batch 85 — Fix "kurang efek depth/3D" (screenshot device sungguhan) di widget Neumorphism
User kirim screenshot widget di device asli (Batch 84 sudah ke-install) — dual-shadow LINEAR
diagonal Batch 84 memang sudah ada di kode, tapi tersebar merata ke SELURUH panel/disc jadi
nyaris tak kelihatan di layar sungguhan (alpha "far/lemah" yang dipilih meniru border tipis
skeuEmboss(), ternyata terlalu halus untuk bidang seluas panel widget). 4 file (widget/drawable,
sama seperti Batch 84):

- `widget_background.xml`/`_light.xml`, `widget_play_button_bg.xml`/`_light.xml`: gradient
  LINEAR diagonal (135°/315°) diganti gradient RADIAL dipusatkan PERSIS di pojok (centerX/Y
  ~0.05-0.18 utk highlight, ~0.85-0.95 utk shadow) — cahaya/bayangan ngumpul di 1 titik pojok
  jauh lebih kebaca sebagai "sumber cahaya" drpd wash tipis merata ke semua arah. Alpha juga
  dinaikkan signifikan (panel: putih 0x29→0x59/0x99, hitam 0x59→0x8C/0x66; disc: 0x40→0x80/0x99)
  — kali ini AMAN pakai alpha lebih tinggi krn falloff radial sendiri yang menjaga area tetap
  sempit (beda dari linear Batch 84 yang nyebar penuh, jadi alpha tinggi disana akan kelihatan
  "banjir 1 warna" bukan cuma di pojok).
- `gradientRadius` pakai nilai dp fix (140dp panel, 40dp disc) — BUKAN persen (`%`/`%p` butuh API
  29+, minSdk app 23), jadi proporsinya bisa sedikit beda kalau widget di-resize sangat
  kecil/besar oleh user — trade-off diterima, dicatat di komentar kode.

0 file baru, 0 protected asset lain disentuh (drawable bukan protected). Ke-4 XML divalidasi
well-formed. **Masih belum diverifikasi visual sungguhan di device untuk perubahan Batch 85 ini
sendiri** — Batch 84 SUDAH diverifikasi user via screenshot (makanya user bisa kasih feedback
spesifik "kurang depth"), tapi versi radial yang baru ini belum; kalau efeknya kebalik-jadi-
berlebihan, kemungkinan besar cukup turunkan alpha lagi, bukan ganti pendekatan.

## Batch 84 — Redesign widget home-screen lama -> Neumorphism, di-hardcode
Arahan user: "redesign theme widget lama -> 'Neumorphism' hardcode". Widget home-screen
(`widget/`, `res/drawable/widget_*`, `res/layout/widget_player*.xml`) itu RemoteViews murni,
BUKAN Compose — jadi tidak pernah bisa ikut ThemeStore/pilihan Tactile-Skeu-Apple yang aktif di
dalam app (RemoteViews cuma bisa reference id drawable resource terkompilasi, tidak bisa jalanin
`Modifier.drawBehind()` dinamis kayak `skeuEmboss()`). Makanya "di-hardcode": widget SELALU
render gaya Neumorphism sekarang, apapun tema di dalam app. Sumbu gelap/terang (`isDark`, dari
`ThemeStore.getMode()` — beda dari pilihan Tactile/Skeu/Apple) TIDAK disentuh/dihapus, cuma
warna di kedua sisinya diganti ke palet Neumorphism.

5 file (di bawah batas normal — 1 modul/`widget/`+drawable terkait):
1. `widget_background.xml` / `widget_background_light.xml` — dari solid polos `#1C1C1E`/`#F2F2F7`
   jadi `layer-list` 3 lapis: base `SkeuNeuSurfaceDark`/`SkeuNeuSurfaceLight` (exact hex dari
   Color.kt, bukan angka baru) + dual-shadow gradient diagonal (terang kiri-atas 135°, gelap
   kanan-bawah 315°, arah sama seperti `skeuEmboss()`). Alpha dipilih dari sisi "far/lemah" token
   asli (gradient di sini menyebar ke seluruh panel, jauh lebih luas dari border tipis
   `skeuEmboss()`). 0 border — konsisten identitas Neumorphism app ("TIDAK ADA border/grain").
2. `widget_play_button_bg.xml` (redesign) + `widget_play_button_bg_light.xml` (BARU) — dulu 1
   file dipakai kedua mode (oval solid merah `#FA233B`, aksen Apple-red peninggalan desain lama).
   Sekarang disc `SkeuEmerald`/`SkeuLightEmerald` + dual-shadow bevel sama pola dgn panel di
   atas. Emerald dipilih (bukan Titanium/`SkeuAccent`) krn `ic_widget_play/pause.png` putih
   polos — kontras jauh lebih jelek di atas titanium abu-terang; emerald juga persis peran
   "sedikit sentuhan zamrud di tengah palet titanium netral" yang sama seperti hero art di
   NowPlayingScreen.
3. `WidgetUpdater.kt` — (a) `TITLE_COLOR_DARK`/`ARTIST_COLOR_DARK`/`TITLE_COLOR_LIGHT`/
   `ARTIST_COLOR_LIGHT` diganti ke hex PERSIS `SkeuDarkText`/`SkeuDarkSecondaryText`/
   `SkeuLightText`/`SkeuLightSecondaryText` dari Color.kt (sebelumnya palet ad-hoc terpisah yang
   kebetulan mirip, bukan token asli — mis. putih murni `#FFFFFF` bukan `#EDEFF2` yang sedikit
   dingin/"logam disikat"). (b) tombol play/pause sekarang juga `setInt(..., "setBackgroundResource", ...)`
   switch dark/light sama pola dengan `widget_root`, ditaruh sebelum blok `if (!isCompact)` biar
   widget compact juga kebagian (compact tetap punya tombol play/pause).

`FILE_MANIFEST.txt` diperbarui (1 file baru, 111→112). 0 protected asset LAIN disentuh
(`AndroidManifest.xml`, `build.gradle.kts`, dotfiles, `.github/workflows/` semua utuh — hanya
`app/src/main/java/.../widget/WidgetUpdater.kt` yang tersentuh dari daftar protected, dan itu
edit parsial 2 spot, bukan rombak fungsi). Brace/paren `WidgetUpdater.kt` seimbang, 4 file XML
drawable baru/diedit divalidasi well-formed (`xml.dom.minidom`).

**Belum diverifikasi visual sungguhan di device** — tidak ada emulator/preview RemoteViews di
environment kerja ini, jadi dual-shadow gradient (angle 135°/315°, harus kelipatan 45° biar valid
di `GradientDrawable`, sudah dicek) BELUM pernah benar-benar dilihat di layar. Prioritas
berikutnya kalau user minta lanjut: pasang widget di 2 mode (gelap/terang) + 2 ukuran
(compact/full), pastikan bevel kebaca jelas tapi tidak berlebih, dan teks title/artist masih
kontras cukup di atas base baru (`#191C21`/`#E8EAED`, beda tipis dari base lama).

## Batch 82 — "debugging+Polish UI": audit lintas ui/ + fix state-leak LyricsSheet + tactile/skeu untuk LockScreen
Arahan user: "debugging+Polish UI" (generik, bukan laporan bug spesifik). Audit statis lintas
semua file `ui/` (MiniPlayerBar, EqualizerSheet, QueueSheet, FolderManagerSheet, LyricsSheet,
LockScreen, PlaylistScreen, dll — file per file, fokus ke pola `remember` tanpa key yang
menyimpan nilai turunan dari parameter yang bisa berubah). 1 bug nyata ditemukan (bukan cuma
gaya penulisan) + 1 gap polish disepakati, 2 file, di bawah batas normal:

1. **Bug — `LyricsSheet.kt` state leak lintas lagu**: `editing`/`draft` di-`remember` TANPA key,
   padahal nilai awalnya diturunkan dari parameter `rawLyrics`. Sheet ini tetap ter-mount kalau
   lagu berganti SAAT sheet masih terbuka — media-session eksternal (tombol headset, notifikasi,
   widget) bisa memicu ganti lagu tanpa lewat UI screen ini sama sekali, jadi `showLyricsSheet`
   di `NowPlayingScreen.kt` tidak punya alasan untuk balik `false` hanya karena lagu berganti.
   Skenario nyata: lagu A belum ada lirik (sheet auto masuk mode edit, `draft=""`), user mulai
   ngetik draft tapi belum simpan, lagu A habis lalu lanjut otomatis ke lagu B (yang SUDAH punya
   lirik) — `editing`/`draft` yang tak berkunci tetap nyangkut ke nilai lagu A, kalau user lalu
   tap "Simpan" draft lagu A ke-lirik-B secara diam-diam. Fix: `remember(rawLyrics) { ... }` di
   kedua state — keduanya sekarang otomatis derive ulang tiap prop `rawLyrics` berubah (lagu
   ganti), membuang draft belum-tersimpan alih-alih salah lampir ke lagu yang salah (lebih aman
   kehilangan draft daripada draft nyasar). **Diaudit, TIDAK bug serupa**: `PlaylistScreen.kt`'s
   `TextInputDialog`'s `initialValue` awalnya kelihatan mirip tapi TIDAK — dialog itu modal
   (AlertDialog, scrim penuh) dan hanya pernah di-mount lewat `if (showRenameDialog)` yang
   di-reset tiap buka/tutup, tidak ada jalur eksternal yang bisa mengubah `selectedPlaylist` saat
   dialog terbuka seperti media-session bisa mengubah lagu.
2. **Polish — `LockScreen.kt` (layar PIN unlock) belum ikut identitas Tactile/Skeu**: layar
   paling sering disentuh di app ini (tiap cold-open kalau App Lock aktif) masih pakai
   `CircleShape` polos untuk semua tombol angka + fingerprint + backspace, padahal MiniPlayerBar,
   NowPlayingScreen, LibraryScreen, SettingsScreen semua sudah dapat `tactileEmboss()`/
   `skeuEmboss()` sejak Batch 49/55/79. Fix: `PinKey` (tombol angka) dan `RoundGlyphButton` baru
   (fingerprint + backspace, sebelumnya `Box` inline terpisah, sekarang 1 composable dipakai
   ulang) keduanya pilih `tactileEmboss(shape=CircleShape, elevation=6.dp, pressed=isPressed)` /
   `skeuEmboss(...)` lewat `isTactileTheme()`/`isSkeuTheme()`, `pressed` diikat ke
   `MutableInteractionSource.collectIsPressedAsState()` (pola sama seperti transport button Now
   Playing) + `bouncyPress()` untuk scale-down saat ditekan. **Apple theme TIDAK berubah sama
   sekali** — cabang `else` tetap `CircleShape` polos persis sebelumnya. State `entered`/`error`/
   `lockedOutUntil` dan alur verifikasi PIN tidak disentuh sama sekali, murni ganti tampilan+
   feedback sentuh 3 tombol.

Tidak ada protected asset disentuh. Brace/paren balance kedua file dicek (`{`/`}`/`(`/`)` sama
persis). **Belum diverifikasi compile/visual sungguhan di device** — sama seperti seluruh batch
sebelumnya, tidak ada `kotlinc`/emulator di environment kerja ini; prioritas berikutnya kalau
user minta lanjut: rebuild CI + install APK, cek transisi pressed/concave di 8 tombol LockScreen
(digit 0-9 dikurangi yang duplikat, fingerprint, backspace) kebaca di kedua tema.

## Batch 81 — Fix "Ambient Light gak bocor" (bagian instruksi user Batch 79 yg belum tersentuh) + bug clip sisi-terang hero art
Instruksi asli user (Batch 79): "Titanium dominan + sedikit sentuhan Zamrud + depth ultra
realistic + Ambient Light yang gak 'bocor'". Batch 79/80 sudah menuntaskan 3 bagian pertama
dengan baik (diverifikasi via audit statis batch ini), tapi bagian ke-4 ("gak bocor") belum
pernah ditangani secara eksplisit — dual-shadow `skeuEmboss()`/hero art digambar di
`drawBehind{}` SEBELUM `.clip()` tanpa batas area apa pun, dan Compose TIDAK meng-clip
`drawBehind{}` ke bounds layout-nya sendiri secara default: bayangan lebar (elevation besar,
mis. MiniPlayerBar's 16.dp) bisa kegambar nimpa sibling di sekitarnya (row lain, NavigationBar
di bawah MiniPlayerBar) tanpa warning apa pun saat compile. 2 file, di bawah batas normal
(10 file/1 modul), tidak perlu Atomic Change exception — scope murni containment fix + 1 bug
tak terkait yg ditemukan saat audit, bukan redesign identitas baru:

1. **`ui/theme/TactileDepth.kt`** — `skeuEmboss()`'s dual-shadow draw (5 layer, kedua sisi)
   sekarang dibungkus 1 `clipRect()` dengan halo **proporsional ke `elevation`** (`basePx * 1.3f`
   — margin di atas offset terjauh yg dipakai, `1.05f`, supaya bentuk bayangan tidak ikut
   terpotong) — bayangan dijamin tidak pernah meluber lebih jauh dari itu, untuk `elevation`
   berapa pun yg dikirim caller manapun (MiniPlayerBar/LibraryScreen/SettingsScreen/HomeScreen/
   NowPlayingScreen — grep-confirmed semua tetap manggil fungsi yg sama, signature tidak
   berubah). Import `clipRect` ditambah.
2. **`ui/NowPlayingScreen.kt`** — AlbumArtHero's `isSkeu` branch, 2 fix sekaligus (ditemukan saat
   audit utk containment fix di atas, bukan laporan user):
   - **Bug ditemukan**: sisi TERANG (specular+highlight) selama ini digambar di `drawBehind{}`
     TERPISAH **SETELAH** `.clip(heroShape)`, sedangkan sisi GELAP (AO+shadow) digambar SEBELUM
     `.clip()` — artinya sisi terang selama ini kepotong tepat di tepi shape, tidak pernah
     benar-benar bisa "meluber ke luar" sebagai soft-shadow seperti sisi gelap, beda arsitektur
     dari `skeuEmboss()` sendiri (yg menggambar KEDUA sisi dalam 1 `drawBehind` yg sama sebelum
     `.clip()`). Fix: disatukan ke 1 `drawBehind{}` sebelum `.clip()`, sama pola dgn
     `skeuEmboss()`.
   - **Containment**: `drawBehind{}` gabungan di atas dibungkus `clipRect()` dgn halo tetap
     18.dp (hero art ukurannya selalu tetap 280.dp, offset terjauh yg dipakai cuma 14.dp literal
     — beda dari `skeuEmboss()` yg proporsional ke param `elevation`).
   - Emerald glint (Batch 80) TIDAK dipindah — tetap 1 `drawBehind{}` terpisah SETELAH `.clip()`,
     sudah benar di posisi situ (dia "permata di permukaan", bukan bayangan yg perlu meluber).
   - Import `clipRect` ditambah.

**Verifikasi audit sebelum fix** (grep + baca manual, bukan asumsi): `SkeuAccent`/`TitaniumDark`/
`TitaniumLight`/`SilverHighlight` masih 100% tidak tersentuh di role M3 primary/surfaceTint
(Titanium tetap dominan, terverifikasi ulang) — token grain/groove lama (`SkeuBrushGrain*`,
`SkeuInnerGroove*`) 0 caller tersisa di seluruh codebase (grep bersih) — `FILE_MANIFEST.txt`
dicek cocok 100% dgn file tree aktual (112/112, tidak ada desync) — brace/paren balance dicek
manual di kedua file yg diedit (`{`/`}` dan `(`/`)` masing-masing seimbang persis).

**README.md juga diperbarui** (di luar 2 file kode di atas, murni dokumentasi) — paragraf tema
custom kedua masih menjelaskan "Skeuomorphism 2.0 — Hyper-Realism UI" 7-layer lama (grain,
border ganda, dst.) yg sudah tidak ada sejak Batch 79, ditulis ulang jadi deskripsi Neumorphism
yg akurat (dual soft-shadow, 0 border/grain, concave saat pressed, Titanium dominan + sentuhan
Zamrud, containment Batch 81 ini).

**Titanium tetap dominan, cakupan Zamrud tidak bertambah** — batch ini murni containment +
1 bug clip, 0 perubahan warna/token accent apa pun. **Masih belum diverifikasi compile/visual
sungguhan di device** — sama seperti Batch 79/80, tidak ada `kotlinc`/emulator di environment
kerja ini; prioritas berikutnya kalau user minta lanjut: rebuild CI + install APK, khususnya cek
MiniPlayerBar (elevation 16.dp, kasus containment paling ketat) & hero art tidak lagi kepotong
di sisi terangnya.

## Batch 80 — Fix visibilitas Zamrud (respons langsung ke feedback user: "mana zamrudnya??")
User feedback setelah Batch 79: "yang kelihatan cuman Titanium dominan. mana zamrud nya??".
Root cause di 3 titik sekaligus: (1) `skeuEmboss()` — emerald di-lerp-blend 55% ke arah
`lightNear` yang nyaris putih opaque (mixing sedikit warna saturasi ke putih nyaris tidak
mengubah hue yg terlihat mata) DAN alpha 0 total saat idle (cuma nyala saat pressed — kalau user
lihat UI diam/screenshot, emerald-nya betul-betul 0%), (2) root ambient wash (MainActivity.kt) —
alpha emerald diturunkan dari `streakAlpha` yg sudah sangat kecil (0.05f/0.12f) dikali 0.9f,
hasil akhir ~0.045-0.108, praktis tak kelihatan, (3) hero art (NowPlayingScreen.kt) — sama pola
lerp-blend-ke-putih spt skeuEmboss(), 14% blend nyaris tak berubah dari putih polos. Fix di
ketiganya: emerald sekarang SELALU layer terpisah dgn warna murni (bukan di-blend ke warna lain)
+ alpha tetap yang jauh lebih tinggi, bukan diturunkan dari nilai lain yang sudah kecil:
1. `TactileDepth.kt` `skeuEmboss()` — radial glint kecil terpisah (warna `SkeuEmerald`/
   `SkeuLightEmerald` murni) di kuadran sisi-terang, alpha baseline 0.20f (idle, tetap "sedikit"
   tapi genuinely visible) naik ke 0.52f saat pressed ("permata menyala" saat disentuh) — bukan
   lagi blend ke `lightNear`. Posisi ikut `dir` (kiri-atas normal, kanan-bawah pressed, konsisten
   dgn concave-flip sisi terang/gelap). Import `Offset` dikembalikan (perlu lagi utk radial
   gradient), import `lerp` dihapus (tidak dipakai lagi di file ini).
2. `MainActivity.kt` (protected, parsial) — root ambient wash: alpha stop emerald di 0.76 diganti
   dari `streakAlpha * 0.9f` (~0.045-0.108, tak kelihatan) jadi alpha TETAP 0.30f gelap/0.36f
   terang (independen dari streakAlpha yg kecil) — masih di bawah accent-glow biasa app ini
   (~0.42-0.45f di tempat lain) supaya tetap "sentuhan", tapi genuinely terlihat sbg vena hijau
   mengikuti kilau silver di 0.62, bukan cuma teknis-ada-di-kode.
3. `ui/NowPlayingScreen.kt` — hero art: lerp-blend 14% ke `heroSpecular` **dihapus**, diganti
   radial glint terpisah (warna murni) di pojok kiri-atas, alpha tetap 0.35f gelap/0.42f terang,
   permanen (hero art statis, tidak ada state pressed). Import `lerp` dihapus (tidak dipakai lagi
   di file ini), import `Offset` dikembalikan.

**Titanium tetap dominan** — tidak ada satu pun perubahan di role M3 primary/surfaceTint
(`SkeuAccent` dkk. sama sekali tidak disentuh); ini murni menaikkan visibilitas 3 titik emerald
yg sudah direncanakan di Batch 79 supaya benar-benar kebaca, bukan menambah cakupan/dominasi
emerald baru. 3 file diedit (di bawah batas 10 file/1 modul, tidak perlu Atomic Change exception
kali ini — scope sudah persis sama dgn 3 dari 6 file Batch 79, cuma tuning angka + 1 teknik
render yg diganti, bukan redesign baru). **Masih belum diverifikasi compile/visual sungguhan di
device** — sama seperti Batch 79, tidak ada kotlinc/emulator di environment kerja ini; prioritas
berikutnya kalau user minta lanjut: rebuild CI + install APK, cek genuinely kelihatan zamrud-nya
di layar HP asli (terutama radial glint di panel/hero art, dan streak root wash).

## Batch 79 — Upgrade identitas Skeuomorphism -> Neumorphism (Titanium dominan + sentuhan Zamrud + depth ultra realistic)
Instruksi eksplisit user: "upgrade skeuomorphism -> Neumorphism dengan accent Titanium yang
dominan dengan sedikit sentuhan zamrud, dan juga depth ultra realistic". **Impact Report / alasan
Atomic Change** (6 file, di atas batas normal 10 file/1 modul TAPI lintas 3 sub-area — ui/theme,
MainActivity.kt root, ui/NowPlayingScreen.kt — jadi dikecualikan sbg Atomic Change: identitas
visual HARUS konsisten di semua titik panggil sekaligus, kalau dipecah jadi >1 batch akan ada
jendela waktu UI campur aduk separuh Hyper-Realism lama + separuh Neumorphism baru):

1. **`ui/theme/Color.kt`** — token grain (`SkeuBrushGrainLight/Dark`,
   `SkeuLightBrushGrainLight/Dark`) & groove (`SkeuInnerGroove(Pressed)`,
   `SkeuLightInnerGroove(Pressed)`) **DIHAPUS TOTAL** (grep-confirmed 0 caller lain sebelum
   hapus, prosedur sama seperti Batch 54/58) — neumorphism generik tidak punya tekstur/garis
   batas sama sekali. `SkeuSpecular`/`SkeuAmbientOcclusion`/`SkeuHighlight`/`SkeuShadow`
   (+ pasangan Light) **nama & value dipertahankan** tapi PERANNYA bergeser jadi layer
   terkuat/terlemah dari tumpukan dual-shadow baru (bukan glint/AO/bevel-border terpisah lagi).
   2 token baru: `SkeuNeuSurfaceDark/Light` (panel fill neumorphic, hampir sewarna kanvas — TIDAK
   menyentuh `SkeuDarkSurface`/`SkeuLightSurfaceVariant` yg dipakai role M3 di Theme.kt, supaya
   Card/Sheet/NavigationBar M3 polos di luar skeuEmboss() tidak ikut berubah kontrasnya) dan
   `SkeuEmerald`/`SkeuLightEmerald` (aksen zamrud baru, `0xFF2FA37C`/`0xFF1E7A5C`).
2. **`ui/theme/TactileDepth.kt`** — `skeuEmboss()` ditulis ulang total: dual soft-shadow
   multi-layer (sisi gelap kanan-bawah 3 layer, sisi terang kiri-atas 2 layer, offset makin jauh
   + alpha makin tipis meniru soft-blur box-shadow ganda CSS neumorphism — DrawScope Compose
   tidak punya blur asli tanpa RenderEffect API 31+, jadi "ultra realistic" di sini = KONTRAS
   TINGGI pada alpha tiap layer, jauh di atas neumorphism generik yg sering nyaris tak
   kelihatan). Pressed = CONCAVE (`dir = -1f` membalik SELURUH sisi terang/gelap, bukan cuma
   mengecil elevasi kayak Tactile) — bahasa visual baku neumorphism "permukaan masuk ke kanvas".
   Grain overlay + outer-bevel-border + inner-groove-border **dihapus total** (0 border sama
   sekali). Sentuhan Zamrud: inti sisi terang berbaur `SkeuEmerald` HANYA saat pressed
   (`emeraldGlow` animatedFloat 0->1), 1 layer saja, alpha rendah — permanen 0 di state normal.
   Import dirapikan: `padding`/`Offset`/`TileMode` dihapus (tidak dipakai lagi), `lerp` tetap.
3. **`ui/theme/BlurUtils.kt`** — `frostedGlass()`'s Skeu edge branch (brushed-metal repeating
   rim) **dihapus total**, Skeu sekarang skip `.border()` sepenuhnya (`return if (isSkeu) base
   else base.border(...)`) — neumorphism tidak pernah punya garis pinggir jenis apa pun. Import
   `Offset`/`TileMode` dihapus (unused).
4. **`MainActivity.kt`** (protected, edit parsial) — root ambient wash Skeu: grain overlay
   (`skeuGrainBrush`, brushed-metal repeating texture di seluruh kanvas app) **dihapus total**
   dari Surface modifier chain. Streak 4-stop Titanium/Silver **tidak diubah** (tetap dominan,
   sesuai instruksi user) + 1 stop BARU di fraction 0.76 (`SkeuEmerald`/`SkeuLightEmerald`,
   alpha `streakAlpha * 0.9f` — sengaja lebih rendah dari kilau silver `*1.8f` di 0.62, supaya
   kebaca vena emerald tipis, bukan aksen kedua yg bersaing). Import token grain dihapus, import
   `Offset`/`TileMode` dihapus (sudah tidak dipakai sama sekali di file ini setelah grain hilang
   — 1 pemakaian Offset lain di file ini pakai fully-qualified name, tidak butuh import).
5. **`ui/NowPlayingScreen.kt`** — hero art Skeu branch ditulis ulang menyamai arsitektur
   `skeuEmboss()` baru (dual soft-shadow multi-layer, 0 border/groove). Sentuhan Zamrud DI SINI
   beda logika dari skeuEmboss(): hero art statis/tidak punya state pressed, jadi inti sisi
   terang SELALU berbaur `SkeuEmerald` sedikit (alpha campur 0.14f, permanen) — satu-satunya
   permukaan yg dapat sentuhan permanen krn ini satu-satunya surface always-active/terbesar di
   layar (prinsip sama dgn komentar §9 branch Tactile soal accent glow always-active, existing
   sebelum batch ini). Import `SkeuInnerGroove`/`SkeuLightInnerGroove`/`Offset` dihapus, `lerp`
   + `SkeuEmerald`/`SkeuLightEmerald` ditambah.
6. **`ui/theme/Theme.kt`** — `SKEU_DARK_LITE.displayName` **"Skeuomorphism" -> "Neumorphism"**,
   `description` ditulis ulang. **`storageKey` "skeu_dark_lite" SENGAJA TIDAK diganti** (preferensi
   tema tersimpan user yg sudah pernah pilih identitas ini tetap valid, tidak ter-reset ke Apple).

**Tidak disentuh sama sekali** (di luar scope instruksi user, verified via grep): `SkeuAccent`/
`TitaniumDark`/`TitaniumLight`/`SilverHighlight` (role M3 primary/surfaceTint — Titanium tetap
SATU-SATUNYA token di situ, memenuhi "Titanium dominan" secara literal), `SkeuDarkBackground/
Surface/SurfaceVariant` + pasangan Light (role M3 background/surface/surfaceVariant — supaya
Card/Sheet/NavigationBar M3 polos di luar panel custom TIDAK ikut berubah kontrasnya),
`SkeuDarkShapes` (shape language tidak diminta berubah), `ui/SettingsScreen.kt` (live-preview
swatch di situ manggil `Modifier.skeuEmboss()` langsung — otomatis ikut render Neumorphism baru
TANPA perlu diedit sama sekali, sudah diverifikasi via grep line 408).

**Belum diverifikasi compile/visual sungguhan di device** — tidak ada `kotlinc`/emulator di
environment kerja ini (sama seperti tiap batch sebelumnya); prioritas berikutnya kalau user minta
lanjut: rebuild CI + install APK, cek dual-shadow neumorphic + emerald touch beneran kebaca di
layar HP asli (terutama transisi pressed/concave skeuEmboss() & hero art NowPlayingScreen).

## Batch 78 — Debugging pass menyeluruh ("debugging semua area")

User minta audit debugging lintas seluruh codebase (bukan laporan bug spesifik). Audit statis
sistematis per area (data/, playback/, ui/, ui/theme/, util/, widget/) — cursor/stream leaks,
GlobalScope, runBlocking, `!!`, listener register/unregister balance, thread-safety widget,
lifecycle service, brace/paren balance token Skeu Hyper-Realism (Batch 73-75, ditandai "belum
pernah berhasil di-compile" — dibaca ulang baris-per-baris, semua token Color.kt yang dirujuk
TactileDepth.kt/BlurUtils.kt/MainActivity.kt/NowPlayingScreen.kt terkonfirmasi ADA & valid,
tidak ada bug baru ditemukan di situ selain fix TileMode Batch 75 yang sudah beres — **masih
tetap belum diverifikasi visual/compile sungguhan**, statis-read tidak bisa gantikan itu).
2 bug nyata ditemukan & diperbaiki, scope 2 file (di bawah batas 10 file/1 modul, tidak perlu
atomic-change exception):

- **`LibraryScreen.kt` — sweep-select bisa nyeleksi baris yang salah setelah list di-scroll**.
  Root cause baru dari gap yang Batch 70 sendiri sudah tandai "belum ditest" tanpa pernah
  di-root-cause: `rowBoundsInRoot` (map index->posisi Y) HANYA PERNAH DITULIS lewat
  `onGloballyPositioned`, tidak pernah DIHAPUS. Begitu sebuah baris di-scroll cukup jauh hingga
  keluar dari komposisi (LazyColumn recycle), entry lamanya tetap nongkrong di map dengan posisi
  Y basi selamanya. Skenario nyata: user sweep, angkat jari, scroll list biasa (gesture ini lolos
  dari `detectDragGesturesAfterLongPress` karena tidak pernah tembus threshold long-press, jadi
  scroll normal LazyColumn tetap jalan seperti biasa), lalu long-press lagi buat lanjut sweep —
  `indexAt()` (`entries.firstOrNull { rootY in it.value }`) bisa kena entry basi yang posisi
  Y-nya sekarang kebetulan overlap baris yang benar-benar berbeda pasca-scroll, sehingga sweep
  diam-diam nyeleksi lagu yang salah. Fix: `DisposableEffect(index) { onDispose { ... remove(index) } }`
  di tiap item `itemsIndexed` — entry dihapus persis saat barisnya keluar dari komposisi, jadi map
  cuma pernah berisi baris yang benar-benar tampil di layar saat itu juga. **Pola relevan utk
  batch depan**: kombinasi `mutableStateMapOf` yang ditulis dari `onGloballyPositioned` container
  yang isinya di-recycle (LazyColumn/LazyRow) SELALU butuh pasangan cleanup di sisi disposal —
  kalau cuma nulis tanpa hapus, itu bug delay-timer, bukan langsung kelihatan dari baca kode
  sekali baca sepintas.
- **`PlayerViewModel.kt` — koneksi `MediaController` bisa bocor kalau ViewModel di-clear sebelum
  handshake async-nya selesai**. `onCleared()` lama cuma panggil `controller?.release()` — kalau
  `controllerFuture` (dari `connect()`) belum resolve saat itu (mis. rotasi/navigasi sangat
  cepat sesaat setelah `connect()` dipanggil), `controller` masih `null`, jadi baris itu no-op:
  future yang masih in-flight TIDAK PERNAH di-cancel/release, listener-nya tetap terpasang, dan
  proses konek ke `PlaybackService` terus jalan di background walau ViewModel-nya sudah tidak ada
  yang pegang — koneksi bocor (jendela sempit, tapi nyata). Fix: `controllerFuture` disimpan
  sebagai field (dulu cuma `val` lokal di `connect()`), `onCleared()` sekarang panggil
  `MediaController.releaseFuture(controllerFuture)` — API resmi Media3 yang menangani KEDUA
  kasus sekaligus (cancel future yang belum resolve, ATAU release controller yang sudah resolve).

**Area yang diaudit tapi TIDAK ditemukan bug baru** (dicek, bukan dilewati): seluruh cursor/stream
I/O (semua sudah `.use{}`), tidak ada `GlobalScope`/`runBlocking`/`!!` di manapun, receiver/listener
register-unregister balance (`ShakeDetector`, `PlaybackService.onDestroy`), thread-safety
`WidgetUpdater.updateAll` (dipanggil dari `Dispatchers.IO` di kedua call site), `AppLogger.kt`
crash-logger (cocok dgn spec MediaStore API 29+/FIFO 50/metadata lengkap di
`PROJECT_STATE.md`/system prompt), `.first()`/`.last()` unsafe-access candidates di
`LibraryScreen.kt` (ketiganya dijaga `groupBy`/empty-check, aman). **Belum diverifikasi
compile/visual sungguhan** — sama seperti batch-batch sebelumnya, tidak ada `kotlinc`/emulator di
environment kerja ini; prioritas berikutnya kalau user minta lanjut: rebuild CI + install APK,
terutama uji sweep-select pasca-scroll (fix di atas) secara langsung di device.

## Batch 77 — Dokumentasi: roadmap 15 fitur generik 100% offline (belum ada implementasi kode)
User minta dokumentasi roadmap 15 fitur generik 100% offline yang belum tersedia di project.
Murni dokumentasi, 0 perubahan kode/behavior aplikasi.

- File baru `ROADMAP_15_FITUR_OFFLINE.md` (root repo) — 15 fitur dipilih dengan syarat: generik
  (bukan niche), 100% jalan lokal tanpa izin INTERNET (konsisten klaim privasi app), dan belum
  ada di `FILE_MANIFEST.txt`/daftar Fitur v1 README. Tiap entri: deskripsi, alasan, perkiraan
  kompleksitas, dependency/risiko teknis utama. Daftar: (1) Editor Tag Metadata ID3, (2) Smart
  Playlist Otomatis, (3) Editor Lirik LRC Tap-to-Sync, (4) A-B Repeat & Bookmark, (5) Ringtone
  Cutter, (6) Pencari & Pembersih Duplikat, (7) Cadangan & Pulihkan Data Lokal, (8) Trim
  Keheningan Otomatis, (9) Visualizer Audio, (10) Dashboard Statistik Dengar, (11) Floating Mini
  Player, (12) Mode Audiobook/Podcast, (13) Konverter Format Audio Lokal, (14) Vault Lagu
  Privat, (15) Alarm Musik. Ditutup tabel ringkasan prioritas (effort/risiko) sebagai saran
  urutan eksekusi, bukan keputusan final.
- `FILE_MANIFEST.txt` diperbarui (110 → 111 file).
- Tidak ada protected asset disentuh, tidak ada kode aplikasi diubah — batch ini murni
  dokumentasi perencanaan untuk keputusan batch berikutnya.

## Batch 76 — Lanjutan pangkas waktu compile CI (sampai habis semua lever aman)

Instruksi eksplisit user: lanjutkan pemangkasan waktu compiler GitHub Actions (Batch 62) sampai
habis. 3 file kode disentuh (2 protected — edit parsial: `.github/workflows/build.yml`,
`app/build.gradle.kts`), semua murni proses build, **kecuali 1 item yang eksplisit BUKAN
zero-effect (ditandai di bawah, bukan disembunyikan)**.

**Diterapkan:**
- `gradle.properties`: heap `-Xmx3072m` -> `-Xmx4096m` + `-XX:+UseParallelGC` eksplisit (runner
  ubuntu-latest 7GB RAM, 3072m di ujung bawah rekomendasi umum AGP+Compose; Parallel GC overhead
  bookkeeping-nya lebih rendah drpd G1 default utk proses build berumur pendek/sekali-jalan).
- **Configuration cache** (`org.gradle.configuration-cache=true`) — lever terbesar yang belum
  disentuh Batch 62: skip SELURUH fase configuration (parsing build script, evaluasi plugin,
  resolusi dependency graph) kalau input sama persis dgn run sebelumnya, beda dari build cache
  yang cuma cache OUTPUT task. `problems=warn` dipasang sbg jaring pengaman krn kombinasi AGP
  8.4.1 + Compose compiler ext 1.5.14 + Kotlin 1.9.24 belum pernah diverifikasi config-cache-
  compatible di sini (tidak ada compiler Android tersedia utk cek langsung) — kalau ada bagian
  yg belum kompatibel, Gradle cuma WARN & lanjut normal, TIDAK gagalkan CI run.
  `.github/workflows/build.yml`: `--configuration-cache` ditambah eksplisit di CLI (redundant-
  safe pattern yg sama dgn `--build-cache` Batch 62) + step `actions/cache@v4` BARU utk
  `.gradle/configuration-cache` — direktori ini project-local, TIDAK ikut ter-cache oleh
  `setup-gradle@v3` yang cuma menyasar Gradle User Home (`~/.gradle`), jadi tanpa step ini
  config cache selalu "cold" tiap run dan kehilangan sebagian besar manfaatnya. Cache key
  berbasis hash `*.gradle.kts`+`gradle.properties` (otomatis cache-miss aman kalau berubah,
  BUKAN error — Gradle sendiri juga validasi fingerprint di dalam entry-nya).

**Diterapkan TAPI bukan zero-effect (baca sebelum anggap ini "gratis"):**
- `app/build.gradle.kts`: `lint { checkReleaseBuilds = false }` — melepas `lintVitalRelease`
  (subset lint fatal: manifest merger, translasi hilang, dll) dari dependency `assembleRelease`.
  TIDAK mengubah 1 byte pun APK (lint murni analisis statis) — tapi INI MENGURANGI 1 lapis
  verifikasi otomatis yang tadinya jalan tiap release build. `./gradlew lint`/`lintRelease`
  tetap bisa dijalankan manual kapan pun; ini cuma lepas pengait otomatisnya dari CI.

**SENGAJA TIDAK diterapkan (lever terbesar yang tersisa, terlalu berisiko tanpa compiler utk
verifikasi):** migrasi Kotlin 1.9.24 -> 2.0+ (compiler K2, umumnya ~2x lebih cepat dari K1).
Bukan cuma bump versi — Compose compiler pindah dari `composeOptions.kotlinCompilerExtensionVersion`
(cara lama, dipakai proyek ini) ke plugin Gradle `org.jetbrains.kotlin.plugin.compose` terpisah
(cara Kotlin 2.0+), dan SEMUA dependency perlu kompatibel Kotlin-2.0. Ini migrasi ekosistem
sungguhan, bukan "murni proses" — kalau ada 1 saja dependency/API yang belum siap, build bisa
gagal total dan TIDAK ada compiler Android di sini utk memverifikasi sebelum push. **Kandidat
batch terpisah kalau user mau lanjut** — bukan sesuatu yang aman diselipkan diam-diam di batch
pemangkasan waktu compile biasa.

## Batch 75 — Fix 3 error compile dari CI Batch 74 (log_fail_128)

CI gagal di `:app:compileDebugKotlin`/`compileReleaseKotlin`: `Unresolved reference: Repeat` di
3 titik (`MainActivity.kt:361`, `BlurUtils.kt:84`, `TactileDepth.kt:256`) — ketiganya bagian dari
teknik "grain"/brushed-metal streak Skeu Hyper-Realism (Batch 73): `Brush.linearGradient(...,
tileMode = TileMode.Repeat)`. `TileMode.Repeat` bukan anggota enum `androidx.compose.ui.graphics.
TileMode` yang valid — nama yang benar adalah `TileMode.Repeated` (entries-nya: Clamp, Repeated,
Mirror, Decal). Fix: ganti `TileMode.Repeat` -> `TileMode.Repeated` di ketiga titik (murni
perbaikan nama enum, tidak ada perubahan visual/logic apa pun dari yang dimaksud Batch 73) +
komentar yang menyebut nama lama ikut diperbaiki biar tidak menyesatkan pembaca berikutnya.
Import `TileMode` sendiri sudah benar di ketiga file (dicek), jadi bukan itu masalahnya.

3 file kode disentuh, 1 baris kode tiap file. Tidak ada protected asset, tidak ada perubahan
visual — batch ini murni memperbaiki compile agar hasil Skeu Hyper-Realism (Batch 73/74) bisa
akhirnya di-build dan diverifikasi visual di device.

## Batch 74 — Debug UI pass: opaque-white-border bug + AlbumArtHero light-mode gap
Audit "debugging UI sampai matang" (bukan laporan user spesifik) menyisir file yang paling
berisiko dari Batch 73 (Skeu Hyper-Realism, belum diverifikasi visual) — 2 bug nyata ditemukan,
keduanya sudah ada sejak Batch 61 (autonomi Light/Dark) tapi baru kelihatan sekarang lewat baca
kode teliti, bukan dari laporan device.

**Bug 1 — `TactileLightHighlight`/`SkeuLightHighlight` = `Color.White` tanpa alpha (opaque
penuh):** Sejak Batch 61, kedua token ini didefinisikan tanpa `.copy(alpha=...)` — beda dari
SEMUA token Highlight/Edge/Shadow lain di `Color.kt` yang semua ber-alpha rendah (`TactileHighlight`
0.065f, `TactileLightEdge` 0.06f, `SkeuHighlight` 0.16f, `SkeuLightShadow` 0.38f, dst). Dipakai
LANGSUNG tanpa override alpha di 2 tempat: `BlurUtils.kt`'s `frostedGlass()` edge brush (Tactile
Light & Skeu Light, dipakai SEMUA sheet/mini-player/card lewat helper bersama) dan `TactileDepth.kt`'s
`skeuEmboss()` outer border (Batch 73, pola `highlight.alpha * outerBorderAlpha` MENGALIKAN bukan
mengganti, jadi alpha 1.0 yang salah lolos bulat-bulat). Hasilnya: border/bevel Tactile Light &
Skeu Light selama ini garis putih SOLID — persis "bright white border" yang dilarang eksplisit di
komentar proyek sendiri (`TactileDepth.kt` baris ~102, mengutip prinsip spec). Fix: `Color.kt` —
`TactileLightHighlight` diberi alpha 0.55f, `SkeuLightHighlight` diberi alpha 0.65f (lebih tinggi,
konsisten dgn `SkeuHighlight` dark 0.16f > `TactileHighlight` dark 0.065f — identitas Skeu memang
bevel lebih tegas).

**Bug 2 — `AlbumArtHero` (`NowPlayingScreen.kt`, permukaan terbesar di seluruh app) tidak baca
mode terang/gelap sama sekali:** Sejak Batch 61 memisah total identitas Tactile/Skeu dari mode
Light/Dark (keduanya sekarang otonom di kedua mode, lihat `Theme.kt`), `skeuEmboss()`/
`tactileEmboss()` sudah benar baca `LocalIsDarkTheme` — tapi `AlbumArtHero` manual-draw-nya (bukan
lewat 2 fungsi itu, karena perlu susun accent-glow `.shadow()` sendiri sebagai layer akhir) masih
hardcode token DARK-ONLY (`TactileHighlight`/`TactileShadow`, `SkeuAmbientOcclusion`/`SkeuHighlight`/
`SkeuShadow`/`SkeuSpecular`/`SkeuInnerGroove`) tanpa cabang isDark apa pun — lolos dari audit Batch
59/73 karena keduanya fokus ke "Skeu dapat treatment sendiri" bukan "light-mode gap". Efeknya: di
Tactile Light / Skeu Light, hero art (piringan album 280dp) selama ini digambar pakai warna
ambient-occlusion/cast-shadow/specular GELAP di atas panel terang — kemungkinan besar terlihat
kotor/terlalu gelap dibanding sisa panel Light-mode lain yang sudah benar. Fix: `isDark = 
LocalIsDarkTheme.current` ditambah, kedua cabang (`isTactile`/`isSkeu`) sekarang pilih token
Light/Dark yang sesuai (`TactileLightHighlight`/`TactileLightShadow`, `SkeuLight*` — 5 token),
alpha override disesuaikan supaya versi Light pakai alpha bawaan token (sudah tepat, bukan lagi
angka literal yang di-tuning cuma untuk Dark).

2 file disentuh (`Color.kt`, `NowPlayingScreen.kt`), tidak ada protected asset. **Masih belum
diverifikasi visual di device** (tidak ada kotlinc/emulator) — brace/paren balance dicek otomatis
(seimbang di kedua file), tapi fix ini sendiri PALING PENTING justru untuk dicek di Light mode
device fisik (Tactile Light & Skeu Light, terutama hero art Now Playing) karena itulah titik yang
selama ini tidak pernah benar sejak Batch 61 tapi juga tidak pernah dilihat langsung.

## Batch 73 — Fix sweep-select tak bisa dilanjutkan + Skeuomorphism 2.0 (Hyper-Realism UI)

**Bug fixed — sweep-select "kepentok", long-press baru mereset bukan melanjutkan:**
Root cause di `LibraryScreen.kt`'s `SongListView`: `detectDragGesturesAfterLongPress.onDragStart`
SELALU memanggil `onSweepSelectRange(persistentSetOf(songs[idx].id))` — set baru isi 1 lagu,
menimpa total apa pun yang sudah terpilih dari sweep/tap sebelumnya. Jadi begitu user kepentok
tepi layar (list tidak auto-scroll dalam 1 gesture), angkat jari, lalu long-press lagi buat
lanjut, seleksi lama langsung hilang diganti 1 lagu baru. Bug kedua yang ikut ditemukan: closure
`onDragStart`/`onDrag` membaca parameter `selectedIds` langsung, padahal `pointerInput(songs)`
cuma restart kalau `songs` berubah — jadi nilai `selectedIds` yang dibaca gesture bisa basi
(snapshot lama), tidak ikut update walau ada tap-toggle lain di antara 2 sweep. Fix: `selectedIds`
dibaca lewat `rememberUpdatedState` (selalu terbaru), `sweepBaseSelection` (snapshot base sebelum
gesture aktif dimulai) diambil di `onDragStart`, dan setiap update sweep (start maupun drag)
sekarang UNION dgn base itu — bukan replace total. 1 file (`LibraryScreen.kt`), signature publik
tidak berubah.

**Skeuomorphism 2.0 — "Hyper-Realism UI" (permintaan eksplisit: total, otonom, tanpa numpang
baseline tema lain):** Batch 57-63 ("Skeuomorphism Dark Lite") masih berbagi mekanisme dasar
(`embossSurface()`) dgn Tactile & border 2-stop yang strukturnya identik — cukup buat identitas
tapi bukan lagi "hyper-realism". Redesain total, 5 file, 1 tema kohesif (atomic):
- `TactileDepth.kt` — `skeuEmboss()` DILEPAS dari `embossSurface()` bersama Tactile, jadi fungsi
  independen penuh dengan 7 layer fisik: (1) ambient occlusion (bayangan kontak lembut, terpisah
  dari cast shadow), (2) cast drop-shadow lebih berat dari Tactile, (3) base surface 4-stop
  diagonal ("curved metal", bukan 2-warna datar) — stop akhir sengaja opaque lewat `lerp()` ke
  hitam/putih, BUKAN `Color.copy(alpha=...)`, supaya identitas "panel solid tak pernah translusen"
  (dari Batch 58) tetap terjaga di gradient baru ini, (4) brushed-metal grain — overlay stripe
  diagonal berulang via `Brush.linearGradient(start, end pendek, TileMode.Repeat)`, (5) specular
  glint — radial-gradient kilau tunggal kuadran kiri-atas, jauh lebih terang dari highlight bevel
  biasa, meredup drastis saat ditekan, (6) outer bevel border (bahasa arah cahaya diagonal yang
  sama dgn Tactile, tapi token & intensitas 100% milik Skeu sendiri), (7) inner groove — border
  kedua di-inset 1dp, kesan tepi panel "diukir turun" sebelum permukaan naik (double-bevel).
- `Color.kt` — token bevel dasar diperkuat (`SkeuHighlight` 0.10f→0.16f, `SkeuShadow` 0.55f→0.65f,
  `SkeuDarkBackground` sedikit lebih gelap utk ruang kontras), + 12 token baru (dark+light):
  `SkeuSpecular(Pressed)`, `SkeuAmbientOcclusion`, `SkeuInnerGroove(Pressed)`,
  `SkeuBrushGrainLight/Dark` — semua murni milik Skeu, tidak ada padanan/turunan di token Tactile.
- `BlurUtils.kt` — `frostedGlass()`'s Skeu edge brush diganti dari 2-stop smooth gradient (sama
  strukturnya dgn Tactile) jadi brushed-metal repeating stripe (`TileMode.Repeat`, teknik sama
  dgn grain di atas) — sekarang benar-benar berbeda struktur dari Tactile, bukan cuma beda warna.
- `MainActivity.kt` (protected, edit parsial) — root ambient wash Skeu dapat 1 layer overlay
  tambahan: brushed-metal grain di level seluruh kanvas app (bukan cuma per-panel), pakai token
  `SkeuBrushGrainLight/Dark` baru. Tactile TIDAK disentuh sama sekali, root wash-nya tetap 3-stop
  lama — kedua identitas sengaja dijaga tidak berbagi layer struktural apa pun lagi.
- `NowPlayingScreen.kt` — `AlbumArtHero` (permukaan terbesar di app) manual-draw Skeu-nya
  diselaraskan ke bahasa hyper-realism yang sama (AO+cast shadow 2-layer, specular glint, inner
  groove) — bukan lewat `skeuEmboss()` (Box ini masih perlu susun accent-glow `.shadow()` sendiri
  di layer akhir), tapi teknik identik supaya konsisten se-app.
- **Sengaja TIDAK disentuh**: Tactile (semua file/token/mekanismenya) — instruksi eksplisit user
  cuma minta Skeu yang di-redesign; `embossSurface()` privat di `TactileDepth.kt` sekarang murni
  Tactile-only (dipakai `tactileEmboss()` saja), dibiarkan namanya generik krn masih dokumentasi
  historis yang jelas di komentar function-nya.
- **Belum diverifikasi visual/compile** (tidak ada `kotlinc`/emulator di environment kerja) —
  brace/paren balance dicek otomatis di ke-5 file (seimbang), grep konfirmasi token lama
  (`SkeuHighlight`/`SkeuShadow`/`SkeuAccent`/dst.) semua masih dirujuk konsisten, tidak ada nama
  yang berubah/hilang yang masih dipanggil di tempat lain. Prioritas sesi berikutnya: rebuild +
  lihat langsung di device — brushed-metal grain (teknik `TileMode.Repeat` segmen pendek) belum
  pernah dirender sebelumnya di codebase ini, paling berisiko terlihat terlalu halus/kasar di
  device fisik dibanding niat desainnya.

## Batch 72 — Fix sweep-select (gesture conflict) + hardening widget theme call + widget masih perlu 1 langkah manual dari user

**Bug 1 fixed — Sweep-select "gak berfungsi sama sekali":** Root cause: gesture conflict.
`SongRow`'s `combinedClickable(onLongClick = { ... onEnterSelectionMode() })` (Batch 66) dan
`SongListView`'s `detectDragGesturesAfterLongPress` di LazyColumn (Batch 70, fitur sweep) adalah
DUA pengenal long-press yang SALING TIDAK TAHU satu sama lain, berebut sentuhan fisik yang
persis sama — `combinedClickable` (di row, lebih dalam) melacak tekanan/ripple-nya sendiri dan
menandai pointer "consumed" sebagai bagian dari pengenalan long-click-nya sendiri, yang otomatis
membatalkan `awaitLongPressOrCancellation` milik detektor sweep di LazyColumn (lebih luar)
sebelum sempat selesai. Akibatnya sweep nyaris tidak pernah benar-benar terpicu — persis "cuman
omong kosong". Fix: `onLongClick` pada `SongRow` dihapus (jadi `clickable` polos, cuma
`onClick`) — `onDragStart` sweep sendiri sudah mereproduksi "tekan-tahan 1 baris = pilih baris
itu" (tanpa perlu drag), jadi tidak ada fungsi yang hilang. Entry point "Pilih" di menu ⋮ tiap
baris (line ~1119) tetap ada sebagai cara masuk mode pilih tanpa gesture sama sekali.

**Bug 2 & 3 — Widget (Play/Pause icon tak pernah ganti, warna/tema tak pernah sinkron):** Ditinjau
ulang MENYELURUH `WidgetUpdater.kt`, `PlayerWidgetProvider.kt`, `PlaybackService.kt`
(listener→pushWidgetUpdate→saveState+updateAll, applyWidgetAction, onStartCommand),
`PlayerViewModel.setThemeMode/setThemeIdentity` — SEMUA jalur kode (icon play/pause via
`isPlaying`→`setImageViewResource`, background via `resolveIsDark`→`setInt(...,
"setBackgroundResource", ...)`, title/artist/art) memakai `views` RemoteViews yang SAMA, urutan
panggilan yang sama, dipush lewat `manager.updateAppWidget(id, views)` yang sama — tidak
ditemukan cabang kode yang secara logis bisa membuat HANYA icon+background gagal sementara
title/artist/art (yang terbukti jalan — lihat screenshot terbaru user, judul/artis/art akurat)
berhasil, dari SATU pemanggilan fungsi yang sama. Satu bug nyata yang ditemukan & diperbaiki:
`PlayerViewModel.setThemeMode/setThemeIdentity` memanggil `WidgetUpdater.updateAll()` LANGSUNG
di Main thread (dipanggil dari klik Compose) — padahal `updateAll()` melakukan decode
bitmap+crop+round (I/O+CPU blocking), pola yang sama yang sudah dihindari `PlaybackService`
sejak Batch 34. Dipindah ke `viewModelScope.launch(Dispatchers.IO)`. **Tapi ini kemungkinan
BUKAN akar masalah utama** — main-thread blocking biasanya bikin lambat/jank, bukan gagal total
permanen.
**Dugaan kuat akar masalah sebenarnya (butuh konfirmasi user, bukan sesuatu yang bisa
diperbaiki lewat kode)**: widget yang PERTAMA KALI ditempel ke home screen sebelum Batch 68
mungkin masih memakai RemoteViews/id widget versi lama yang di-cache oleh launcher (OneUI/MIUI
dkk dikenal melakukan ini lintas update APK) — `widget_root` (id background) ditambahkan di
Batch 68; kalau widget instance yang ditempel itu sendiri tidak pernah dilepas & ditempel ulang
sejak itu, TIDAK ADA perbaikan kode yang akan pernah terlihat di layar, walau logic-nya 100%
benar. **User WAJIB coba: hapus widget dari home screen, lalu tempel ulang yang baru** — kalau
setelah itu icon play/pause & warna tema masih tidak berubah, itu baru konfirmasi bug kode
sungguhan dan perlu logcat/screen record dari user utk lanjut diagnosa (tidak bisa ditebak lagi
dari pembacaan kode statis semata).

3 file kode disentuh (`LibraryScreen.kt`, `PlayerViewModel.kt`; `WidgetUpdater.kt` tidak
diubah — sudah diperiksa ulang menyeluruh dan tidak ditemukan bug). Tidak ada protected asset.

## Batch 71 — Fix 2 error compile dari CI Batch 70 (log_fail_124)

CI gagal di `:app:compileDebugKotlin`/`compileReleaseKotlin`, 2 error:

1. `PlaybackService.kt:422` — `SongArtBitmapLoader` (dari Batch 69) tidak implement
   `BitmapLoader.supportsMimeType(String): Boolean`. Fungsi ini abstract tanpa default di
   Media3 1.3.1 (sebelumnya lolos compile lokal kemungkinan krn cache/versi Media3 berbeda saat
   Batch 69 ditulis) — MediaSession memanggilnya utk cek apakah loader ini sanggup nangani
   sebuah mime type sebelum kirim raw bytes ke `decodeBitmap()`. Fix: override, `true` utk
   `mimeType.startsWith("image/")` — konsisten dgn `decodeBitmap()` yang memang cuma pakai
   `BitmapFactory` (format image standar), bukan jalur `loadBitmap(uri)` yang justru resolve
   `song.uri` via `loadThumbnail()`/`MediaMetadataRetriever`.
2. `LibraryScreen.kt:318` — `onSweepSelectRange = { ids -> ...; selectedIds = ids }` gagal
   krn parameter lambda `ids` bertipe `ImmutableSet<Long>` (signature `onSweepSelectRange:
   (ImmutableSet<Long>) -> Unit` di `SongListView`) sementara `selectedIds` bertipe
   `PersistentSet<Long>` (perlu `.add()/.remove()` di tempat lain, jadi tipenya tidak diubah).
   `ImmutableSet` adalah supertype `PersistentSet`, assign langsung tidak valid. Fix:
   `selectedIds = ids.toPersistentSet()` — import-nya sudah ada di file, 1 baris saja.

2 file kode disentuh, 1 baris tiap file (kecuali override baru = 8 baris). Tidak ada protected
asset, tidak ada perubahan behavior selain memperbaiki compile.

## Batch 70 — Fitur baru: sweep-select (tekan-lama lalu geser) di tab Lagu
1 file disentuh (`LibraryScreen.kt`, tidak ada protected asset). Menjawab laporan user
"pemilihan lagu masih satu-satu, bikin pegel" (sesi Batch 69) — user pilih mekanisme
"tekan-lama lalu drag jari nyapu ke bawah/atas" dari 3 opsi yang ditawarkan.

- Infrastruktur multi-select (selectionMode/selectedIds/checkbox, bulk "Tambah ke Playlist")
  SUDAH ADA sebelumnya — yang hilang cuma cara masuk-banyak-lagu-sekaligus, sebelumnya wajib
  tap checkbox satu per satu setelah tekan-lama.
- Fix: `SongListView`'s `LazyColumn` sekarang punya `pointerInput` +
  `detectDragGesturesAfterLongPress` di level container (bukan per-row) — tekan-lama 1 lagu
  (masuk selection mode seperti biasa via `SongRow.onLongClick` yang sudah ada), lalu TANPA
  angkat jari, geser ke atas/bawah -> `selectedIds` di-replace dengan rentang kontigu dari lagu
  anchor sampai lagu di bawah jari saat ini (`onSweepSelectRange`, callback baru, prop baru di
  `SongListView`/dipanggil dari `LibraryScreen`). Posisi tiap baris dilacak via
  `onGloballyPositioned`+`positionInRoot()` (koordinat root, direfresh tiap baris
  compose/recycle), posisi jari dikonversi ke koordinat root yang sama via `localToRoot()`
  supaya hit-test-nya konsisten.
- `SongRow` dapat parameter `modifier` baru (default `Modifier`) supaya `SongListView` bisa
  nempel `onGloballyPositioned` per baris tanpa ubah pemanggil lain (`GroupedListView`, tab
  Favorit, dll — semua masih pakai default, tidak disentuh).
- **Scope**: baru diterapkan di tab "Lagu" (Library) — TIDAK di `PlaylistScreen.kt` (list lagu
  di dalam playlist), TIDAK di grup album/artis, TIDAK di tab Favorit. Kalau user mau gesture
  yang sama di tempat lain, itu batch terpisah (pola row-bounds-tracking di atas bisa dipakai
  ulang).
- **Belum diverifikasi visual di device** — gesture Compose custom (`pointerInput` di level
  LazyColumn bareng `combinedClickable` per-row) rawan konflik gesture-detection kalau meleset
  asumsi; scroll normal (swipe cepat tanpa jeda) seharusnya tidak kesenggol karena
  `detectDragGesturesAfterLongPress` cuma aktif setelah threshold tekan-lama (~500ms) terpenuhi
  duluan, tapi ini kelas bug yang paling akurat dicek langsung di HP.

## Batch 69 — Fix tombol Play/Pause tak terlihat + artwork notifikasi/pill kosong
2 file disentuh (tidak ada protected asset). 2 dari 5 laporan bug user pada sesi ini (sisanya:
lihat catatan di PROJECT_STATE.md — widget perlu konfirmasi rebuild APK, 2 lainnya perlu info
tambahan dari user sebelum bisa di-diagnosis).

**Bug A — Tombol Play/Pause utama di Now Playing tak kelihatan (default theme) / jadi box
kosong aneh (custom theme):**
- Root cause: `FilledIconButton` play/pause (`NowPlayingScreen.kt`) set `contentColor =
  MaterialTheme.colorScheme.background` — warna latar HALAMAN, sama sekali tidak berkaitan
  dengan `animatedAccent` (warna lingkaran tombol itu sendiri, aksen dinamis per lagu sejak
  Batch 67). Begitu kebetulan keduanya senasib (sama-sama gelap atau sama-sama terang), ikon
  menyatu sempurna dengan lingkarannya -> tak kelihatan / kotak kosong.
- Fix: pola `luminance() > 0.55f -> Black else White` yang sudah dipakai `MiniPlayerBar.kt`
  (`accentContentColor`) untuk kasus identik, sekarang direplikasi di sini — kontras dihitung
  dari `animatedAccent` sendiri, bukan warna halaman.

**Bug B — Artwork kosong di notifikasi & lock-screen "pill" media control (Image 4):**
- Root cause: sejak Batch 67, `MediaMetadata.artworkUri` diisi `song.uri` (URI file audio,
  benar utk `ContentResolver.loadThumbnail()` yang punya penanganan khusus file audio) — tapi
  `BitmapLoader` BAWAAN Media3 tidak tahu itu, cuma buka URI itu sebagai stream mentah lalu
  coba decode langsung sebagai gambar, yang diam-diam gagal utk file audio. Kelas bug PERSIS
  sama dengan yang Batch 68 perbaiki di Coil (`AudioArtFetcher`) — tapi `BitmapLoader` Media3
  itu loader terpisah yang tidak ikut kesentuh Batch 68.
- Fix: `PlaybackService.kt` — class baru `SongArtBitmapLoader` (implementasi
  `androidx.media3.common.util.BitmapLoader`) yang pakai `loadThumbnail()` (API 29+) / fallback
  `openInputStream()+BitmapFactory` — pola sama persis dgn `WidgetUpdater`/`AudioArtFetcher`.
  Didaftarkan via `MediaLibrarySession.Builder.setBitmapLoader(...)`. `onCreate()` ditandai
  `@UnstableApi` (pola opt-in per-fungsi yang sudah dipakai file ini, lihat
  `onPlaybackResumption`).
- **Pola relevan utk batch depan**: artwork punya SEKARANG 4 loader terpisah yang masing-masing
  butuh fix sendiri kalau sumber URI-nya berubah lagi — Coil (`AudioArtFetcher`), widget
  (`WidgetUpdater.loadAlbumArtBitmap`), `AccentColorExtractor`, dan sekarang MediaSession
  (`SongArtBitmapLoader`). Grep ke-4-nya kalau ganti skema URI artwork lagi.

## Batch 68 — Fix album art hilang total (regresi Batch 67) + widget tidak sinkron saat ganti tema

**Bug 1 — Album art hilang di semua lagu (Library/Home/MiniPlayerBar/NowPlaying):**
Root cause: regresi dari Batch 67. `Utils.kt`'s `AlbumArt` composable dialihkan ke `song.uri`
(URI audio si lagu sendiri), lalu diberikan langsung ke Coil (`SubcomposeAsyncImage`) sebagai
`model`. Fetcher bawaan Coil untuk `content://` (`ContentUriFetcher`) mendekode byte-nya sebagai
gambar (`BitmapFactory`/`ImageDecoder`) — itu cuma jalan kalau URI-nya memang gambar. `song.uri`
adalah file audio, jadi decode-nya gagal untuk SEMUA lagu, `error{}` selalu jatuh ke ikon
"no cover". Batch 67 sendiri sudah menandai "belum diverifikasi visual di device" — ini yang
kejadian. 3 titik non-Coil yang disentuh Batch 67 (widget/`PlaybackService`/
`AccentColorExtractor`) TIDAK kena bug ini karena mereka pakai `contentResolver.loadThumbnail()`
langsung, bukan lewat Coil — itu sebabnya widget/notifikasi/accent-color tetap benar sementara
UI di layar total hilang.
Fix: file baru `AudioArtFetcher.kt` — custom Coil `Fetcher` yang mencegat URI ber-MIME `audio/*`
sebelum fetcher bawaan Coil, lalu ekstrak artwork tertanam pakai pola yang sama persis dengan
3 titik non-Coil (`loadThumbnail()` API 29+, `MediaMetadataRetriever.embeddedPicture` fallback
API 23-28). Didaftarkan sekali di `AudioPlayerApplication.kt` (edit parsial, tidak menyentuh
`crossfade`) lewat `ImageLoader.Builder().components { add(...) }` — otomatis berlaku ke semua
4 pemakai `AlbumArt` tanpa ubah signature composable-nya sama sekali. Hanya mencegat MIME
`audio/*`, jadi tidak akan pernah bentrok kalau nanti Coil dipakai untuk gambar asli.

**Bug 2 — Widget tidak pernah sinkron saat ganti tema:** Root cause 2 lapis. (1)
`PlayerViewModel.setThemeIdentity()`/`setThemeMode()` (sejak `ThemeStore` dipecah 2-key di
Batch 61) tidak pernah memanggil `WidgetUpdater.updateAll()` — grep konfirmasi 0 call path dari
perubahan tema ke widget sebelum batch ini, jadi widget cuma redraw kalau lagu ganti atau sistem
minta (`onUpdate`/resize). (2) `widget_player(.compact).xml` cuma punya 1 palet warna hardcode
(gelap, `#1C1C1E`) — tidak ada varian terang sama sekali, jadi walau dipanggil pun tidak ada apa-
apa untuk ditukar. Fix: `widget_background_light.xml` baru (`#F2F2F7`) sebagai pasangan
`widget_background.xml`; root `LinearLayout` kedua layout widget dikasih id (`widget_root`)
supaya bisa ditarget `RemoteViews.setInt(..., "setBackgroundResource", ...)`; `WidgetUpdater.kt`
baca `ThemeStore(context).getMode()` (fungsi baru `resolveIsDark()`, meniru cabang
`isSystemInDarkTheme()` punya Compose lewat `Configuration.uiMode` karena kelas ini di luar
Composable) lalu pilih background+warna teks title/artist sesuai; `PlayerViewModel.kt`
(`setThemeIdentity`/`setThemeMode`) sekarang panggil `WidgetUpdater.updateAll(appContext)`
setelah nyimpen state. `ThemeIdentity` (Tactile/Skeu) sengaja TIDAK dikasih palet widget sendiri
— keduanya dark-only per desain sejak Batch 61, jadi tidak ada yang perlu disinkronkan di sana;
`setThemeIdentity` tetap ikut manggil `updateAll()` untuk jaga simetri kalau nanti identity-aware
widget ditambah.
5 file kode disentuh (`AudioArtFetcher.kt` baru, `AudioPlayerApplication.kt`, `WidgetUpdater.kt`,
`PlayerViewModel.kt`, `widget_background_light.xml` baru) + 2 layout XML (id attribute saja).
Tidak ada protected asset disentuh. **Belum diverifikasi visual di device** — brace balance +
grep call-path dicek otomatis, tapi ini persis kelas bug yang butuh mata di layar fisik (Batch 67
adalah contoh kenapa itu penting).

## Batch 67 — Fix root cause FileNotFoundException album art (playback + widget + UI)
8 file disentuh (tidak ada protected asset).

- **Root cause** (ditemukan dari analisa `log_*.txt` hasil Repack ke Dokumen, Batch 66): semua
  jalur pemuatan album art membangun URI legacy
  `content://media/external/audio/albumart/$albumId` — tabel cache authority ini sering kosong
  di Android modern (API 29+), jadi `loadThumbnail()`/`openInputStream()` gagal konsisten
  `FileNotFoundException` untuk banyak album (log AccentColorExtractor + WidgetUpdater
  menunjukkan >80 entri sejak Batch 22, 2 hari terakhir saja).
- Bug yang sama diam-diam ada di jalur UI (`AlbumArt` composable, `Utils.kt`) — Coil
  `error{}` fallback bikin gagalnya senyap (jatuh ke ikon musik) dan TIDAK pernah tercatat ke
  log, jadi baru ketahuan lewat audit kode setelah root cause di jalur playback ditemukan.
- **Fix**: semua sumber art diganti pakai `song.uri` (URI file audio-nya sendiri dari
  `MusicRepository`) — didukung `loadThumbnail()` native via `MediaMetadataRetriever`
  fallback, tidak bergantung tabel cache lama.
  - `AccentColorExtractor.extract()`: param `albumId: Long?` → `songUri: Uri?`.
  - `PlaybackService.kt` (`loadSavedQueueItems`) & `PlayerViewModel.kt` (`mediaItemFor`,
    `updateAccentColor`): `setArtworkUri(...)` pakai `song.uri` langsung, hapus konstruksi URI
    legacy.
  - `Utils.kt`: `AlbumArt` composable param `albumId: Long?` → `artworkUri: Uri?`; fungsi
    helper `albumArtUri()` (khusus legacy authority) dihapus, tidak dipakai lagi di mana pun.
  - `MiniPlayerBar.kt`, `LibraryScreen.kt` (x2), `HomeScreen.kt` (x2), `NowPlayingScreen.kt`
    (backdrop blur + `AlbumArtHero`, x2): semua call site diupdate ke `artworkUri = song.uri`.
- Dampak: album art seharusnya jauh lebih sering tampil benar di Home/Library/MiniPlayer/Now
  Playing/widget/notifikasi, dan warna aksen dinamis (`AccentColorExtractor`) lebih sering
  berhasil diekstrak alih-alih jatuh ke aksen statis. **Belum diverifikasi visual di device.**

## Batch 66 — Fix: feedback "Repack ke Dokumen" tidak terlihat (ketutup ModalBottomSheet)
1 file disentuh (tidak ada protected asset).

- Root cause: `onInfoMessage` sudah memicu Snackbar via `MainActivity`, tapi `ModalBottomSheet`
  dirender di layer/window terpisah DI ATAS `Scaffold` — jadi Snackbar itu tampil ketutup sheet
  yang masih terbuka. User pencet "Repack ke Dokumen", tidak lihat apa-apa, dikira macet/gagal.
- Fix: `DiagnosticLogSheet.kt` sekarang punya banner feedback inline di dalam sheet itu sendiri
  (state `exportResult`), muncul tepat di bawah tombol dgn ikon CheckCircle (sukses, hijau
  primaryContainer) / ErrorOutline (gagal, errorContainer) + teks status, auto-hilang 2.5 detik
  (`LaunchedEffect` + `delay`). Haptic juga dibedakan: TextHandleMove (sukses) vs LongPress
  (gagal) — dulu selalu sama terlepas hasil. Panggilan `onInfoMessage` tetap dipertahankan
  (tidak merugikan, tetap berguna kalau sheet sudah tertutup duluan).

## Batch 65 — Fix: nama APK rilis bentrok saat CI di-rerun manual (penyebab "duplikat unduhan")
1 file disentuh (1 protected, edit parsial: `.github/workflows/build.yml`).

- Root cause: tag/nama file APK rilis (`v1.0.$COUNT-release`) murni dari jumlah commit sejak
  `v-reset`. Re-run manual workflow pada commit yang sama (tanpa commit baru) -> `$COUNT` sama
  -> tag & nama file APK identik dgn rilis sebelumnya -> `softprops/action-gh-release` overwrite
  release lama, dan di sisi HP nama file yang sama persis bikin duplikat "(1).apk" di folder
  Unduhan alih-alih dikenali sebagai build baru.
- Fix: tag rilis sekarang `v1.0.$COUNT-release-run${{ github.run_number }}` —
  `github.run_number` unik per eksekusi workflow, jadi re-run pada commit sama tetap hasilkan
  tag & nama file APK baru. `appVersionName`/`appVersionCode` di APK itu sendiri (dari
  `gitCommitCount()` di `app/build.gradle.kts`) TIDAK diubah — tetap murni dari jumlah commit.

## Batch 64 — Ganti tombol "Salin" -> "Repack ke Dokumen" di Log Diagnostik
1 file disentuh (tidak ada protected asset).

- `DiagnosticLogSheet.kt`: tombol copy-to-clipboard diganti `AppLogger.exportLogToDocuments(context)`
  — menulis isi log saat ini ke file baru `log_<timestamp>_<uuid>.txt` di folder publik
  `Documents/AudioPlayer/logs` (folder sama dgn crash_*.txt), via MediaStore API 29+, tanpa
  permission baru. Icon ganti dari ContentCopy -> Archive.
- `AppLogger.kt`: tambah `exportLogToDocuments()` + `enforceExportLogRetention()` (FIFO 20 file,
  scoped ke prefix `log_` saja — retensi 50 file `crash_*.txt` yang sudah ada tidak disentuh).

## Batch 63 — Ganti total aksen tembaga -> Titanium+Silver metalik + baseline Skeu tidak identik lagi
2 instruksi user: (1) "ganti total accent tembaga -> Titanium+silver metallic", (2) "semua
Theme custom wajib menampilkan visual secara otonom tanpa menggunakan baseline yang identik".
3 file disentuh (1 protected, edit parsial: `MainActivity.kt`).

- **`SkeuDarkAccent` (0xFFCB8B4B, tembaga/amber hangat, identitas sejak Batch 53) DIHAPUS
  PERMANEN**, diganti `SkeuAccent` (0xFFB6BAC0, silver-gray metalik) + 3 token gradient baru
  (`TitaniumDark` 0xFF6B6F75, `TitaniumLight` 0xFF9BA0A6 [belum dipakai, disiapkan utk polish
  lanjutan], `SilverHighlight` 0xFFF2F3F5). Rename `SkeuDarkAccent`→`SkeuAccent` konsisten di
  3 file (`Color.kt`, `Theme.kt` — role M3 `primary`/`surfaceTint` + `isSkeuTheme()`
  comparison, `MainActivity.kt`).
- **Undertone hangat (krem/parchment/kulit) Skeu ikut digeser ke dingin (platinum/silver)**
  di `Color.kt` — `SkeuDarkText`/`SecondaryText`, `SkeuLightBackground`/`Surface`/
  `SurfaceVariant`/`Text`/`SecondaryText`/`Shadow` semua direvisi. Alasan: aksen dingin di
  atas kanvas hangat akan terasa "nabrak"/tidak koheren — konsekuensi desain wajar dari
  "ganti total" family logam (tembaga=hangat vs titanium=dingin), bukan scope creep.
- **Struktur ambient wash Skeu (baru di Batch 62) diganti total dari Tactile** (poin 2 user)
  — dulu 3-stop `background→tint→surfaceVariant` PERSIS sama resepnya dgn Tactile, cuma beda
  warna. Sekarang 4-stop dgn `colorStops` custom: `TitaniumDark→SilverHighlight(1.8x alpha)
  →TitaniumDark` ditumpuk sempit di fraction 0.55-0.68 (bukan disebar rata) meniru 1 garis
  kilau pantulan cahaya di logam disikat ("brushed metal streak"), baru turun ke
  `surfaceVariant` di stop akhir. Tactile TIDAK disentuh (3-stop lama tetap, sudah unik sejak
  awal — cuma Skeu yang dulu ikut-ikutan pakai resep sama).
- `ThemeIdentity.SKEU_DARK_LITE.description` diupdate ("aksen tembaga hangat" → "aksen
  Titanium&Silver metalik").
- **Belum diverifikasi visual** — baik palet netral-dingin baru maupun efek streak metalik
  adalah desain baru tanpa referensi device fisik.
- **Sengaja TIDAK dikerjakan**: `TitaniumLight` disiapkan sbg token tapi belum dipakai di
  mana pun — cadangan kalau butuh stop gradient ke-4 saat polish visual lanjutan.

## Batch 62 — Vibes radikal (lepas batasan mode) + CI compile time dipangkas drastis
2 instruksi user digabung 1 batch: (1) "perkuat vibes tiap tema custom secara radikal,
tanpa mengikuti batasan light/dark system", (2) "terapkan semua cara pangkas waktu compile
GitHub Action secara drastis". 6 file disentuh (1 protected — edit parsial: `MainActivity.kt`,
1 protected — edit parsial: `.github/workflows/build.yml`).

**Vibes radikal:**
- Ambient root wash (dulu Batch 53 Midnight Blue Tactile-only, lalu Batch 61 digated ke mode
  gelap saja) sekarang trait IDENTITAS murni — tampil di KEDUA mode, tanpa gate `isDarkTheme`
  sama sekali (`MainActivity.kt`, var direname `tactileRootBrush` → `identityRootBrush`
  karena sekarang menangani 2 identitas). Alpha mode terang jauh lebih tinggi dari versi
  gelap (`MidnightBlueLightAmbientAlpha` 0.16f vs 0.06f) — kontrasnya terbalik, alpha kecil
  yang cukup di atas AMOLED nyaris tak kelihatan di atas kanvas terang.
- **Skeu dapat ambient wash sendiri untuk pertama kali** (`SkeuAmbientAlphaDark`/
  `SkeuAmbientAlphaLight` di `Color.kt`) — dulu selalu flat total ("panel solid, bukan kaca"),
  sekarang simetris dengan Tactile: wash tembaga tipis di root, lintas mode juga.
- Bevel `tactileEmboss()`/`skeuEmboss()` (`TactileDepth.kt`): semua alpha border/shadow
  dinaikkan signifikan di kedua mode (contoh Tactile light: border-top normal 0.90f→1.0f,
  shadow normal 0.22f→0.34f; Tactile dark: border-top 0.065f→0.16f, shadow 0.70f→0.90f) —
  sengaja menyimpang dari nada "restrained" spec asli demi instruksi eksplisit user.
- **Belum diverifikasi visual** — nilai alpha adalah tuning baru tanpa referensi device,
  kandidat penyesuaian lanjutan begitu dicoba nyata (terutama shadow normal 0.90f Tactile
  dark yang cukup ekstrem, sesuai literal permintaan "radikal").

**CI compile time (murni proses, TIDAK mengubah output APK — minify/shrinkResources release
sengaja tidak disentuh karena itu risiko integritas rilis, bukan cuma waktu compile):**
- `gradle.properties`: `org.gradle.caching=true` (build cache lintas-run, otomatis ikut
  ke-cache oleh `setup-gradle@v3` yang sudah dipakai workflow — tidak perlu ubah apa pun lagi
  di situ), `org.gradle.parallel=true`, `org.gradle.configureondemand=true`,
  `kotlin.incremental=true`, heap dinaikkan 2048m→3072m.
- `.github/workflows/build.yml`:
  - Checkout: tambah `filter: blob:none` (partial clone) — commit history/metadata tetap
    lengkap (jadi `git rev-list --count` di step "Determine version name" tetap akurat),
    tapi isi file historis lama tidak ikut didownload di awal.
  - **2 invocation Gradle digabung jadi 1**: `gradle testDebugUnitTest assembleRelease` (dulu
    2 step terpisah = 2x fase configuration project penuh). Fail-fast TETAP terjaga tanpa
    flag tambahan — default Gradle (tanpa `--continue`) langsung stop begitu
    `testDebugUnitTest` gagal, `assembleRelease` di invocation yang sama tidak akan pernah
    mulai, persis perilaku lama.
  - Step diurut ulang: decode keystore + determine version (keduanya tidak butuh Gradle sama
    sekali) sekarang jalan SEBELUM satu-satunya invocation Gradle, supaya env var
    `SIGNING_*` sudah siap sejak awal invocation seperti alur lama.
  - `--build-cache` ditambahkan eksplisit ke command Gradle (redundant-safe dengan
    `org.gradle.caching=true` di gradle.properties, tapi eksplisit di CLI memastikan aktif
    walau properties file entah kenapa tidak terbaca).
  - "Upload failure log artifact": `test-output.log` dihapus dari daftar path (sekarang cuma
    ada 1 log gabungan, `build-output.log`) — mencegah warning "file not found" palsu.
- **Release Blocking Rule dicek**: tidak ada perubahan pada `.github/workflows/` yang
  menghapus/mengubah publish "GitHub Release" (masih `softprops/action-gh-release@v2`,
  masih pakai `secrets.SIGNING_*` yang sama, masih upload `.apk` langsung sebagai release
  asset) — murni percepatan proses di sekitarnya.

## Batch 61 — Pisah total identitas tema dari mode: Tactile & Skeu kini otonom di Light/Dark
User koreksi Batch 60: maksudnya bukan cuma UI toggle, tapi identitas Tactile & Skeuomorphism
(dulu hardcode gelap permanen lewat `resolveIsDark()`) harus DICABUT dari 1 mode dan dikendalikan
langsung oleh toggle mode yang sama dengan Apple — supaya "nuansa otonom"-nya keluar maksimal di
kedua mode, bukan cuma dark. Rearsitektur data model, 8 file disentuh (2 protected — edit
parsial saja: `MainActivity.kt`):
- **`AppTheme` enum (5 nilai campur identity+mode) dihapus total**, diganti 2 enum independen di
  `Theme.kt`: `ThemeIdentity` (APPLE/TACTILE/SKEU_DARK_LITE — "wajah" tema) & `ThemeMode`
  (SYSTEM/LIGHT/DARK — terang/gelap, berlaku sama ke ketiganya). `resolveIsDark()` sekarang murni
  fungsi dari `ThemeMode`, sudah tidak tahu-menahu identitas apa yang aktif.
- **`Color.kt`**: token LIGHT baru utk Tactile & Skeuomorphism (background/surface/text/bevel
  masing-masing) — bukan sekadar invert warna, didesain ulang per identitas (Tactile light =
  kaca bening di atas kanvas nyaris-putih dingin; Skeu light = panel krem/parchment hangat),
  accent/success/error tetap sama di kedua mode per identitas (sengaja, biar identitas tetap
  "satu" walau mode beda).
- **`Theme.kt`**: `colorsFor(identity, isDark)` sekarang punya 4 skema (Apple×2, Tactile×2,
  Skeu×2, dari 3 sebelumnya). `isTactileTheme()`/`isSkeuTheme()` (dipakai 5 file lain tanpa perlu
  disentuh — `MiniPlayerBar.kt`, `BlurUtils.kt`, `LibraryScreen.kt`, `HomeScreen.kt`,
  `NowPlayingScreen.kt`) diganti perbandingannya dari `background` (beda tiap mode, jadi tidak
  bisa match keduanya) ke `primary`/accent (sama di kedua mode per identitas by design) —
  fix diam-diam yang WAJIB, kalau tidak diubah kedua helper ini akan selalu `false` di mode
  terang. `LocalIsDarkTheme` (CompositionLocal baru) di-provide sekali di `AudioPlayerTheme()`
  supaya `tactileEmboss()`/`skeuEmboss()`/`frostedGlass()` bisa baca mode aktif tanpa param baru
  di puluhan call site.
- **`TactileDepth.kt`**: `tactileEmboss()`/`skeuEmboss()` sekarang baca `LocalIsDarkTheme` dan
  pilih token+alpha border/shadow sendiri utk light vs dark (alpha di-tuning ulang khusus varian
  terang — kontrasnya terbalik dari versi AMOLED/charcoal gelap).
- **`BlurUtils.kt`**: `frostedGlass()`'s default `alpha` (dulu hardcode compare ke
  `AppleLightBackground`, jadi salah untuk Tactile/Skeu light) & `edgeBrush` Tactile/Skeu
  sekarang branch ke `LocalIsDarkTheme`, pakai token light barunya sendiri.
- **`ThemeStore.kt`**: 1 key (`selected_theme`) → 2 key (`selected_identity`, `selected_mode`) +
  migrasi otomatis dari key lama (Tactile/Skeu lama → identity sama + mode DARK; Light/Dark lama
  → Apple + mode sama; System lama → Apple + SYSTEM) — user existing tidak kehilangan preferensi.
- **`PlayerViewModel.kt`**: `appTheme: StateFlow<AppTheme>` + `setAppTheme()` → 2 StateFlow
  independen (`themeIdentity`, `themeMode`) + 2 setter (`setThemeIdentity`/`setThemeMode`).
- **`MainActivity.kt`** (edit parsial, protected): semua referensi `AppTheme.TACTILE`/
  `SKEU_DARK_LITE` → `ThemeIdentity`, `AudioPlayerTheme(theme=...)` → `AudioPlayerTheme(identity=,
  mode=)`, `SettingsScreen(...)` dipanggil dgn 2 state + 2 callback. Midnight Blue ambient wash
  root (Tactile-only sejak Batch 53) digated tambahan `&& isDarkTheme` — ambient AMOLED itu
  spesifik ekspresi gelap Tactile, bukan trait identitas Tactile itu sendiri (light Tactile pakai
  kanvas flat seperti identitas lain).
- **`SettingsScreen.kt`**: 2 section terpisah — toggle mode (dari Batch 60, sekarang berlaku
  sama ke SEMUA identitas, bukan cuma Apple) di atas, lalu 3 card identitas (Apple/Tactile/
  Skeuomorphism) di bawah, live-preview tiap card sekarang pakai `isDark` dari mode AKTIF
  (bukan `resolveIsDark(identity)` yang statis) — jadi toggle "Mode Gelap" langsung mengubah
  swatch preview Tactile/Skeu di real time, bukti visual langsung bahwa identitasnya otonom.
- **Belum diverifikasi visual/compile** (tidak ada kotlinc/emulator) — brace/paren balance
  seimbang di ke-8 file; grep konfirmasi 0 referensi `AppTheme` aktif tersisa di seluruh
  `app/src/main/java` (hanya di komentar historis) dan 0 call site lain ke `isTactileTheme()`/
  `isSkeuTheme()`/`tactileEmboss()`/`skeuEmboss()`/`frostedGlass()` yang perlu ikut diubah
  (signature publiknya tidak berubah).
- **Sengaja TIDAK dikerjakan**: nilai token warna LIGHT baru (Tactile & Skeu) adalah desain baru
  tanpa spec eksternal (sama presedan Skeu Batch 57) — belum pernah dilihat di device fisik,
  kandidat polish lanjutan kalau kontras/keterbacaannya perlu disesuaikan setelah dicoba nyata.

## Batch 60 — Rombak arsitektur picker tema: card select-only → Switch on-off (Light/Dark)
User minta sektor tema di Settings dirombak dari "button 1 arah" (card select-only, tidak ada
jalan balik langsung) jadi toggle on-off yang fleksibel untuk mode Light/Dark. 1 file disentuh
(`SettingsScreen.kt`), tidak ada perubahan pada `AppTheme` enum/`Theme.kt`/`ThemeStore.kt` (data
model & storage key tetap sama persis — low risk, tidak ada migrasi diperlukan):
- Trio card `System`/`Light`/`Dark` (dulu bagian dari loop `AppTheme.entries` yang sama dengan
  Tactile & Skeu) diganti komponen baru `ThemeModeToggleSection` — 2 `Switch` M3 saling
  menyesuaikan: "Ikuti Sistem" (ON = `AppTheme.SYSTEM`) dan "Mode Gelap" (disabled otomatis saat
  Ikuti Sistem ON atau saat tema kustom Tactile/Skeu aktif, karena keduanya dark-only by design —
  `resolveIsDark()` tidak diubah). Mematikan "Ikuti Sistem" jatuh ke `DARK`/`LIGHT` sesuai posisi
  "Mode Gelap" terakhir (tidak pernah reset ke default).
- `ThemeOptionCard` (card visual dengan live swatch warna) TIDAK dihapus — masih dipakai untuk
  Tactile & Skeu Dark Lite (2 custom identity, bukan bagian keluarga Light/Dark), sekarang di-`filter`
  dari loop lama alih-alih `AppTheme.entries.toList()` mentah.
- **Belum diverifikasi visual/compile** (tidak ada `kotlinc`/emulator di environment ini) — brace/
  paren balance file dicek otomatis (seimbang), grep konfirmasi hanya 1 titik pemanggil lama
  (`items(AppTheme.entries...)`) yang tersentuh, tidak ada call site lain ke `ThemeOptionCard`
  yang perlu disesuaikan.
- **Sengaja TIDAK dikerjakan**: styling Switch mengikuti M3 default (belum di-custom warna per
  tema seperti `skeuEmboss()`/`tactileEmboss()`) — Switch hanya muncul saat tema aktif adalah
  System/Light/Dark (Apple family), jadi selalu pakai `AppleAccent` M3 default; polish visual
  Switch jika diinginkan adalah kandidat batch terpisah.

## Batch 59 — Skeu "otonom": tuntaskan gap identitas + filter pending jadi 1 batch low-risk
Gabungan 2 instruksi user: tema custom selalu ada sisa "flat/hybrid" yang bikin identitasnya
nggak otonom (tolong diperbaiki) + gabungkan semua item pending jadi 1 batch atomic, tapi hanya
yang low-risk. Lihat PROJECT_STATE.md Batch 59 untuk rasional lengkap kenapa 6 dari 8 item
pending ditolak masuk batch ini (bukan low-risk) dan kenapa 2 sisanya (gap wiring `skeuEmboss()`)
kebetulan juga langsung menjawab keluhan "flat/hybrid".

- `HomeScreen.kt` — `ContinueListeningCard`: import `skeuEmboss`/`isSkeuTheme` ditambah, cabang
  `if (isTactile)` jadi `when { isTactile / isSkeu / else }`, `isPanelTheme = isTactile || isSkeu`
  dipakai untuk `color`/`tonalElevation`.
- `LibraryScreen.kt` — banner undo-sembunyikan-lagu: perubahan identik pola di atas.
- `NowPlayingScreen.kt` — import `SkeuHighlight`/`SkeuShadow`/`isSkeuTheme` ditambah.
  `AlbumArtHero`: `heroShape` sekarang `if (isPanelTheme)` (bukan `isTactile` saja). Blok
  `drawBehind`+`clip`+`border`+`shadow` Tactile disalin jadi cabang `isSkeu` terpisah (bukan
  fungsi bersama — duplikasi disengaja untuk minim risiko tanpa compiler), pakai
  `SkeuShadow.copy(alpha=0.40f)` untuk shadow (Tactile: `TactileShadow` 0.55f) dan
  `Brush.linearGradient(SkeuHighlight.copy(alpha=0.16f), SkeuShadow.copy(alpha=0.40f))` untuk
  border (Tactile: `TactileHighlight`/`TactileShadow` 0.12f/0.32f) — angka dipilih meneruskan
  prinsip Skeu sejak Batch 57/58 ("catch-light lebih kuat, shadow lebih rendah dari Tactile").
- `MiniPlayerBar.kt` — perbaikan arsitektur (bukan cuma nambah cabang tema baru): outer bar Box
  adalah satu-satunya titik di app yang memasang `skeuEmboss()` DAN `frostedGlass()` sekaligus di
  chain yang sama. Karena `frostedGlass()` Skeu sudah full-opaque sejak Batch 58, layer kedua ini
  selalu menutup total background+border `skeuEmboss()` di bawahnya — identitas Skeu sendiri
  secara visual tidak pernah sampai ke layar di titik ini, definisi persis "masih hybrid, tidak
  otonom". Fix: `.then(if (isSkeu) Modifier else Modifier.frostedGlass())` menggantikan
  `.frostedGlass()` polos — Skeu skip, Tactile/Apple tidak berubah. Komentar lama di baris atasnya
  ("under either theme") diperbaiki karena sudah tidak akurat lagi untuk Skeu.
- **Tactile TIDAK disentuh sama sekali** — namanya sendiri "Hybrid Glassmorphism" (spec eksternal
  Batch 53 yang di-supply user), hybrid itu memang identitasnya, bukan bug. Keluhan user
  ditafsirkan sebagai gap penerapan (leftover default Apple di titik-titik yang belum ke-cover),
  bukan permintaan menghilangkan konsep hybrid dari Tactile.
- **Ditolak dari batch ini (bukan low-risk)**: shared-element transition & pull-to-refresh (butuh
  bump Compose BOM), sweep `strings.xml`/token spacing (mekanis besar lintas banyak file, tanpa
  compiler risikonya kumulatif), lirik otomatis (fitur baru berbasis network, bukan polish),
  `TactileButton`/`TactileSwitch`/`TactileSlider` custom (custom drag-gesture di kontrol paling
  sering dipakai — risiko tertinggi kalau salah, sengaja ditunda ke batch terpisah kalau user mau
  ambil risikonya), verifikasi device fisik (bukan kerja kode).
- **Belum diverifikasi visual/compile** (tidak ada `kotlinc`/emulator di environment ini) —
  brace/paren balance dicek otomatis di ke-4 file Kotlin (seimbang), grep konfirmasi 0 duplikat
  import di tiap file yang disentuh.


## Batch 58 — Polish Skeuomorphism Dark Lite: hilangkan sisa glassmorphism
User lapor lewat screenshot bahwa kesan glassmorphism di app masih terlalu kuat, minta tema
custom terbaru (Skeuomorphism Dark Lite, Batch 57) di-polish sampai "matang". Lihat
PROJECT_STATE.md Batch 58 untuk ringkasan lengkap + rasional per file. Detail teknis:

- `BlurUtils.kt` (`frostedGlass()`) — Skeu sekarang dipaksa `alpha = 1f` (full opaque) di dalam
  fungsi, mengabaikan default parameter `alpha` (0.92f/0.96f) yang tadinya dipakai semua tema
  tanpa kecuali. Aman: grep konfirmasi tidak ada satupun dari 6 call site (`LyricsSheet.kt`,
  `QueueSheet.kt`, `MiniPlayerBar.kt`, `FolderManagerSheet.kt`, `NowPlayingScreen.kt`,
  `EqualizerSheet.kt`) yang pernah pass parameter `alpha` eksplisit. Border Skeu diganti dari
  `Brush.linearGradient(SkeuHighlight, SkeuEdge)` (rim kaca lembut alpha 0.10f→0.12f, pola sama
  persis dengan Tactile) ke `Brush.linearGradient(SkeuHighlight, SkeuShadow)` (alpha 0.10f→0.55f,
  transisi jauh lebih kontras — kebaca sebagai tepi terukir, bukan reflected light kaca). Lebar
  border Skeu naik 1.dp→1.5.dp. Tactile branch (`TactileHighlight`/`TactileEdge`, alpha default)
  TIDAK disentuh — identitas Tactile memang glassmorphism by design, bukan target batch ini.
- `Color.kt` — `SkeuEdge` (`Color.Black.copy(alpha=0.12f)`) dihapus, 0 call site tersisa setelah
  perubahan di atas (grep-confirmed sebelum dihapus, mengikuti presedan pembersihan token mati
  Batch 54). Komentar blok token bevel Skeu diperbarui (referensi ke "Edge" diganti "Shadow").
- `TactileDepth.kt` (`embossSurface()`, mesin privat bersama `tactileEmboss()`/`skeuEmboss()`) —
  6 nilai alpha (`borderTopAlpha`/`borderBottomAlpha`/`shadowAlpha`, masing-masing varian
  normal/pressed) yang tadinya literal hardcode langsung di body fungsi sekarang jadi parameter
  eksplisit dengan default = angka literal Tactile yang lama persis
  (0.065f/0.03f, 0.30f/0.15f, 0.70f/0.35f) — `tactileEmboss()` tidak pass parameter baru ini sama
  sekali, jadi perilakunya byte-identik dengan sebelum batch ini. Root cause bug yang diperbaiki:
  sebelumnya, `highlight.copy(alpha = borderTopAlpha)` / `shadow.copy(alpha = shadowAlpha)`
  MENGGANTI (bukan mengalikan) alpha yang sudah dibawa `highlight`/`shadow` — jadi `SkeuHighlight`
  (dibuat dengan alpha 0.10f) dan `SkeuShadow` (alpha 0.55f) di Color.kt selalu ditimpa balik ke
  angka Tactile (0.065f/0.070f) setiap kali `skeuEmboss()` dipanggil, walau komentar Color.kt
  sudah lama bilang keduanya "sengaja beda" dari Tactile. `skeuEmboss()` sekarang pass angka
  sendiri: `borderTopAlphaNormal/Pressed = 0.10f/0.045f`, `borderBottomAlphaNormal/Pressed =
  0.24f/0.12f` (lebih rendah dari Tactile 0.30f/0.15f, sesuai komentar "shadow lebih rendah"),
  `shadowAlphaNormal/Pressed = 0.55f/0.28f` (akhirnya memakai alpha asli `SkeuShadow`, bukan lagi
  angka Tactile).
- `MiniPlayerBar.kt` — import `skeuEmboss`/`isSkeuTheme` ditambah. Bar luar: cabang `when` baru
  (`isTactile`/`isSkeu`/else) menggantikan `if/else` lama, Skeu sekarang dapat
  `skeuEmboss(shape=barShape, elevation=16.dp)` alih-alih fallback `Modifier.shadow()` Apple.
  `miniPlayPauseShape`: `if (isTactile || isSkeu) MaterialTheme.shapes.medium else CircleShape` —
  `MaterialTheme.shapes.medium` otomatis resolve ke `SkeuDarkShapes.medium` (beda dari
  `TactileShapes.medium`) lewat `AudioPlayerTheme()`, tidak perlu token shape baru. Tombol
  play/pause mini (40dp): cabang `when` yang sama, Skeu dapat `skeuEmboss(shape=..., elevation=
  6.dp)` (sebelumnya nol depth cue sama sekali, fallback `Modifier` polos).
- `NowPlayingScreen.kt` — import ditambah sama seperti di atas. `isSkeu` di-hoist di level
  composable utama (sama seperti `isTactile` sejak Batch 55) dan dipakai di 2 titik: (1) tombol
  play/pause utama (68dp) — `playPauseShape` sekarang `if (isTactile || isSkeu)
  MaterialTheme.shapes.medium else CircleShape` + `skeuEmboss(shape=playPauseShape,
  elevation=10.dp)`, persis presedan Tactile Batch 55; (2) `GestureIndicatorBadge` (badge popup
  geser kecerahan/volume) — sebelumnya `isTactile`-only, cabang else-nya (dipakai Skeu) masih
  `Surface` translusen (`colorScheme.surface.copy(alpha=0.9f)` + `tonalElevation=6.dp` +
  `shadowElevation=4.dp`, mekanisme M3 tonal-elevation-blend, kasus glassmorphism lain yang
  kelewat) — sekarang `isPanelTheme = isTactile || isSkeu` dipakai untuk semua 4 parameter
  (`modifier`/`color`/`tonalElevation`/`shadowElevation`), Skeu dapat `Color.Transparent` +
  `skeuEmboss()` + elevasi 0.dp sama seperti Tactile.
- `README.md` — paragraf tema Skeuomorphism Dark Lite diperbarui (opaque/panel solid, border
  ukiran, daftar titik baru yang dapat `skeuEmboss()`).
- **Catatan arsitektur, bukan bug baru**: pada Box luar `MiniPlayerBar`, `skeuEmboss()` dan
  `frostedGlass()` sama-sama dipasang di modifier chain yang sama (Tactile sudah begini sejak
  lama). Karena `frostedGlass()` untuk Skeu sekarang opaque (lihat di atas), background+border
  gradient `skeuEmboss()` di titik itu ketutup penuh oleh background+border `frostedGlass()` yang
  digambar belakangan dalam chain — hanya drop-shadow `skeuEmboss()` (digambar via `drawBehind`
  sebelum `.clip()`, jadi tidak ikut ketutup/ke-clip) yang tetap kelihatan. Sama persis dengan
  bagaimana Tactile sudah bekerja selama ini (bukan regresi), tapi juga bukan yang paling efisien
  — kandidat polish lanjutan kalau animasi scale/elevation `skeuEmboss()` di titik itu ingin
  benar-benar terlihat penuh (lepas dari `frostedGlass()` pada Box yang sama).
- **Belum diverifikasi visual/compile** (tidak ada `kotlinc`/emulator di environment ini) —
  brace/paren balance dicek otomatis di semua 5 file Kotlin yang disentuh (seimbang), grep
  konfirmasi `SkeuEdge` 0 call site tersisa dan `frostedGlass()` 0 call site yang pass `alpha`
  eksplisit sebelum perubahan diterapkan.
- **Sengaja TIDAK dikerjakan**: `HomeScreen.kt` (`ContinueListeningCard`) dan `LibraryScreen.kt`
  (row list) juga punya cabang `isTactile`-only, tapi audit menunjukkan cabang else-nya (dipakai
  Skeu) sudah `MaterialTheme.colorScheme.surface` TANPA `.copy(alpha=...)` — sudah opaque dari
  awal, bukan kontributor kesan glassmorphism, jadi di luar scope perbaikan spesifik batch ini.


## Batch 57 — Toggle tema custom baru: Skeuomorphism Dark Lite
User minta tema custom ketiga (kedua di luar keluarga Apple), tanpa spesifikasi eksternal
disediakan — palet dirancang sendiri sesuai definisi umum skeuomorphism dark-lite, sengaja
dibedakan dari Tactile (AMOLED-glass, hue biru dingin) lewat basis charcoal netral hangat + panel
timbul fisik + aksen tembaga. Atomic change, 6 file disentuh (di atas limit normal 10 file/1
modul secara jumlah file oke, tapi lintas beberapa file `ui/theme/` + 1 layar + `MainActivity.kt`
karena satu toggle tema baru inheren menyentuh setiap titik dispatch tema yang sudah ada — bukan
beberapa fitur independen digabung paksa):

- `AppTheme` (`Theme.kt`) — entry baru `SKEU_DARK_LITE` (`storageKey = "skeu_dark_lite"`).
  `ThemeStore.kt`/`AppTheme.fromStorageKey()` generic by design (Batch-batch sebelumnya), tidak
  perlu diubah — persistensi & picker (`SettingsScreen.kt`'s `AppTheme.entries.toList()`) otomatis
  ikut tema baru ini.
- `Color.kt` — token baru: `SkeuDarkBackground` (`#16181C`), `SkeuDarkSurface`/`SkeuDarkSurfaceVariant`
  (panel level 1/2), `SkeuDarkText`/`SkeuDarkSecondaryText`, `SkeuDarkAccent` (`#CB8B4B`, tembaga
  hangat — sengaja beda hue dari `AppleAccent`/`TactileAccent`), `SkeuDarkError`/`SkeuDarkSuccess`,
  `SkeuHighlight`/`SkeuEdge`/`SkeuShadow` (pola sama seperti `TactileHighlight`/`TactileEdge`/
  `TactileShadow`, nilai sendiri).
- `Theme.kt` — `SkeuDarkColors` (`darkColorScheme`, `onPrimary = Color.Black` karena SkeuDarkAccent
  luma ≈0.60 di atas threshold 0.55 — beda dari Apple/Tactile yang `onPrimary = Color.White`),
  `SkeuDarkShapes` (12/16/20dp — satu tingkat lebih membulat dari `TactileShapes` di tiap ukuran,
  supaya ketiga keluarga shape tidak ada yang identik), `resolveIsDark()` (selalu gelap, sama
  precedent Tactile — "Dark Lite" di nama bukan hint mode terang), `colorsFor()`, `isSkeuTheme()`
  helper (pola sama `isTactileTheme()`, Batch 54 dedup rationale), dan `AudioPlayerTheme()`
  dispatch shapes 3 arah. Typography: sengaja reuse `AppleTypography` (tidak ada spec type-scale
  terpisah diminta — identitas Skeu ini dibawa lewat warna/shape/bevel, bukan tipografi).
- `TactileDepth.kt` — `tactileEmboss()` di-refactor jadi wrapper tipis di atas `embossSurface()`
  privat baru (parameter: shape/elevation/pressed/surfaceTop/surfaceBottom/highlight/shadow) —
  perilaku & signature publik 100% tidak berubah (diverifikasi: parameter default sama, urutan
  operasi drawBehind→clip→background→border sama persis dengan versi sebelum refactor, cuma
  literal token diganti parameter). `skeuEmboss()` baru — wrapper sama dengan token Skeu. Belum
  dipanggil di layar manapun (tombol play/pause dst.) — kandidat batch polish berikutnya, sama
  seperti histori Tactile sendiri (Batch 45-48 bangun primitif dulu, Batch 55 baru pasang ke
  tombol play/pause).
- `BlurUtils.kt` — `frostedGlass()`'s `edgeBrush` digeneralisasi dari 2 arah (`isTactile`/else) ke
  3 arah (`isTactile`/`isSkeu`/else) — Skeu dapat border bevel diagonal sendiri
  (`SkeuHighlight`→`SkeuEdge`), bukan fallback flat Apple.
- `SettingsScreen.kt` — baris tema di pemilih (`ThemeOptionCard`) untuk Skeu sekarang pratinjau
  hidup pakai `skeuEmboss()` (pola sama seperti baris Tactile sejak Batch 49), bukan kartu flat
  seperti Light/Dark/System. Import `skeuEmboss` ditambah.
- `MainActivity.kt` — NavigationBar catch-light line + `tonalElevation` 6.dp (sebelumnya
  hardcoded `appTheme == AppTheme.TACTILE`) digeneralisasi lewat `navCatchLightColor` (null untuk
  Apple/Light/Dark, `TactileHighlight` untuk Tactile, `SkeuHighlight` untuk Skeu) — kedua identitas
  "panel fisik" dapat efek yang sama, alasan sama seperti Batch 53 (tonalElevation tinggi + accent
  surfaceTint bikin nav bar kelihatan warna aksen dulu sebelum identitas panelnya). Root ambient
  gradient (`tactileRootBrush`) SENGAJA tidak diperluas ke Skeu — itu memang Tactile-only (glass
  ambient), Skeu flat by design (panel timbul solid, bukan lapisan kaca berlapis), jadi root
  Surface-nya otomatis pakai `color = colorScheme.background` flat seperti Apple, tidak perlu
  sentuhan tambahan di sana.
- **Belum diverifikasi visual/compile** — sama seperti setiap batch tema sebelumnya (tidak ada
  `kotlinc`/emulator di environment ini), diverifikasi lewat baca-manual + brace/paren balance
  check (seimbang di semua 6 file yang disentuh) + grep exhaustiveness check (`when` atas
  `AppTheme` di `resolveIsDark()`/`colorsFor()`/`AudioPlayerTheme()` sudah mencakup entry baru,
  tidak ada `when` lain atas `AppTheme` di codebase yang butuh cabang tambahan).
- **Sengaja TIDAK dikerjakan** (didaftar transparan): bevel/emboss di kontrol individual
  (play/pause button, slider, dst.) untuk tema Skeu — `skeuEmboss()` sudah siap pakai tapi baru
  dipasang di 1 tempat (baris pemilih tema); root ambient gradient khusus Skeu (disengaja flat,
  lihat di atas); tipografi custom untuk Skeu (reuse Apple).


Satu entri per batch pengembangan. Ditulis supaya sesi chat AI yang baru (atau siapa pun
yang baru gabung ke proyek ini) bisa langsung baca file ini dan tahu histori keputusan,
tanpa harus gali `git log` atau riwayat chat lama yang sudah tidak bisa diakses lagi.

Catatan: pencatatan per-batch di file ini baru dimulai dari Batch 5. Semua hasil kerja
sebelum itu (fondasi awal: scan MediaStore, playback dasar via Media3, UI Library/Home/Now
Playing/Settings, dst.) sudah terangkum sebagai satu kesatuan di daftar fitur pada
`README.md` — tidak dipecah ulang per batch di sini karena detail per-batch-nya sudah tidak
tersedia.

## Batch 56 — Reset versionCode/versionName ke basis tag `v-reset` (bukan tema/fitur)
User minta reset nomor versi app, `1.0.<total commit history>` sudah terasa besar/tidak sedap
dipandang. 2 file protected disentuh (edit parsial, bukan replace total).

- `app/build.gradle.kts` — `gitCommitCount()`: `git rev-list --count HEAD` (total history) →
  `git rev-list --count v-reset..HEAD`, fallback ke `HEAD` count kalau tag `v-reset` belum ada
  (biar build pertama sebelum tag dibuat tetap jalan). `maxOf(1, ...)` ditambah karena commit
  tepat di tag itu sendiri akan menghasilkan 0 (Android tidak terima versionCode < 1).
- `.github/workflows/build.yml` — step "Determine version name": rumus disamakan persis (COUNT
  pakai `2>/dev/null ||` fallback yang sama) supaya tag GitHub Release & angka di dalam APK tidak
  drift satu sama lain (dua tempat independen yang sama-sama jalankan `git rev-list`, harus tetap
  identik seperti alasan awal Batch 30 kenapa keduanya digabung ke satu basis).
- **Setup manual 1x wajib** (di luar cakupan ZIP ini — tidak ada akses ke git remote user dari
  sini): `git tag v-reset && git push origin v-reset`, sekali saja setelah ZIP ini di-push. Tidak
  breaking kalau lupa — kedua sisi fallback ke hitungan lama sampai tag dibuat.
- git history TIDAK di-rewrite/squash (tidak ada force-push) — cuma cara menghitung versionCode
  yang berubah, bukan datanya.

## Batch 55 — Tactile identity polish (atomic change): play/pause button shape+emboss, diagonal border fix
User minta polish tema custom Tactile biar perbedaannya sama tema utama (Apple) makin kelihatan.
Audit: warna/tipografi/shape/kaca sudah dibedakan sejak Batch 49-54, tapi tombol play/pause —
kontrol paling sering dilihat di seluruh app (mini bar + Now Playing) — masih render identik di
kedua tema (`FilledIconButton` circle default M3, tanpa bevel). Itu satu-satunya titik besar yang
bikin identitas Tactile "hilang" pas lagi dengar musik. 3 file kode tersentuh (1 modul UI, atomic).

- `NowPlayingScreen.kt`: tombol play/pause utama (68dp) sekarang pakai `MaterialTheme.shapes.medium`
  (rounded-square, bahasa bentuk Tactile) alih-alih `CircleShape` Apple, dibungkus `tactileEmboss()`
  (elevation 10dp) khusus tema Tactile — jadi terbaca sebagai tombol fisik terangkat, bukan cuma
  lingkaran warna aksen. `isTactile` di-hoist ke scope `NowPlayingScreen()` (sebelumnya cuma
  dihitung di 2 composable anak).
- `MiniPlayerBar.kt`: tombol play/pause (40dp) dapat perlakuan sama (shape + `tactileEmboss()`
  elevation 6dp, lebih kecil sesuai ukuran bar) — perbedaan identitas kelihatan dari mini player
  juga, tidak cuma setelah buka full player.
- `NowPlayingScreen.kt` — `AlbumArtHero`: border hero art masih pakai `Brush.verticalGradient`
  peninggalan Batch 45/46 (sebelum aturan diagonal spec §9 diadopsi Batch 53) — satu-satunya
  border Tactile di codebase yang belum ikut arah cahaya diagonal top-left→bottom-right yang
  dipakai `BlurUtils.kt`/`TactileDepth.kt`. Diganti ke `Brush.linearGradient` (konsisten, bukan
  perubahan visual besar, cuma menyamakan arah bevel).
- **Belum diverifikasi visual/compile** (sama seperti setiap batch tema sebelumnya, environment
  ini tidak punya `kotlinc`/emulator) — verifikasi lewat baca-manual + brace/paren balance check
  tiap file yang disentuh (semua seimbang).
- **Sengaja tidak dikerjakan** (didaftar transparan): custom thumb/track untuk `Slider` (seek bar
  utama & volume dalam-aplikasi) — kedua slider masih M3 default identik di kedua tema. Butuh
  slot `thumb`/`track` composable (tersedia di Material3 1.2.1 yang dipakai proyek ini) tapi
  bentuknya lebih rumit (custom draw, bukan modifier tempel) dan berisiko lebih tinggi tanpa
  compiler untuk verifikasi — kandidat batch polish berikutnya, bukan dipaksakan masuk sini.

## Batch 54 — Technical debt cleanup (atomic change): dedup isTactile, hapus FQN inline & dead token, sentralisasi corner-radius
User minta gabungkan daftar technical debt murni-kode (dari audit statis: isTactile terduplikasi,
inline fully-qualified reference, dp literal tak tersentralisasi) dengan debt yang sudah tercatat
di README.md ("Belum selesai / dalam pengerjaan") jadi 1 batch atomic. Bukan repaint tema, bukan
fitur baru — murni pembersihan kode. Batch-limit exception dipakai (10 file tersentuh) karena ini
satu perubahan logis yang saling terkait (semua berakar dari "duplikasi & sentralisasi token"),
bukan gabungan paksa beberapa fitur independen.

- `Theme.kt`: tambah `isTactileTheme()` composable helper — dipanggil dari `BlurUtils.kt`,
  `HomeScreen.kt`, `LibraryScreen.kt`, `MiniPlayerBar.kt`, `NowPlayingScreen.kt` (2 tempat),
  menggantikan `MaterialTheme.colorScheme.background == TactileBackground` yang sebelumnya
  diketik ulang manual di 6 tempat. Perilaku identik.
- 11 inline fully-qualified reference (`com.rudi.audioplayer.ui.theme.X` di tengah kode) diganti
  jadi `import` biasa — `MiniPlayerBar.kt`, `LibraryScreen.kt`, `HomeScreen.kt`,
  `NowPlayingScreen.kt`, `MainActivity.kt`.
- `Color.kt`: hapus 5 token warna dead code (`TactileControl`, `TactileControlPressed`,
  `GlassPressed`, `GlassWhite`, `TactileMutedText`) — 0 call site, sisa persiapan Batch 53 untuk
  komponen tactile yang belum dibangun. Komentar penjelas ditinggalkan untuk batch masa depan.
- **File baru `Spacing.kt`**: token `Radius` (9 nilai, 4dp-28dp) untuk corner-radius terpusat
  (spec §19 "Spacing & Shape Language"). 32 titik `RoundedCornerShape(N.dp)` literal di 8 file
  (`FeatureHintBanner.kt`, `HomeScreen.kt`, `LibraryScreen.kt`, `MiniPlayerBar.kt`,
  `NowPlayingScreen.kt`, `SettingsScreen.kt`, `Theme.kt`) dimigrasi otomatis pakai script regex,
  diverifikasi grep nihil literal tersisa + brace/paren balance check tiap file.
- **Sengaja tidak dikerjakan** (didaftar, bukan disembunyikan): migrasi ~340 literal `.dp` non-
  radius sisanya (terlalu one-off/context-specific untuk ditoken-kan aman), ekstraksi 339 string
  ke `strings.xml` untuk i18n (README sudah catat alasan: refactor mekanis sebesar itu tak aman
  tanpa compiler untuk verifikasi), pull-to-refresh Library & shared-element transition sungguhan
  (butuh bump Compose BOM — keputusan dependency-version terpisah, bukan technical debt kode).
- Total file: 108 → 109 (nambah `Spacing.kt`). `FILE_MANIFEST.txt` diperbarui.
- **Belum diverifikasi build** — environment tidak punya `kotlinc`, verifikasi lewat grep +
  brace/paren balance check per file, bukan compile sungguhan.

## Batch 53 — Timpa palet tema custom Tactile pakai spec AMOLED Hybrid Glassmorphism baru (compose-amoled-hybrid-glass-final.md)
User kirim spec baru sekaligus perintah eksplisit: override total tema custom sebelumnya, terapkan
100% ke SEMUA sektor yang langsung terlihat user, bukan cuma pilar utama. Spec baru ini secara
eksplisit mendaftar "a full Midnight Blue theme" (persis Batch 52) sebagai anti-pattern (§24),
jadi ini bukan sekadar retuning nilai — arah visual identitasnya berbalik: AMOLED-black + glass
translucent jadi material utama, Midnight Blue turun jadi ambient gradient tipis saja (§6), dan
tactile/skeuomorphism dibatasi ke kontrol interaktif (§10).

Pola implementasi sama seperti Batch 50-52: 4 file token/util pusat (`Color.kt`, `Theme.kt`,
`BlurUtils.kt`, `TactileDepth.kt`) disentuh untuk mendefinisikan ulang makna token, tanpa
mengubah struktur/signature `frostedGlass()` atau `tactileEmboss()` — jadi ke-15 file `ui/*.kt`
yang sudah memakai keduanya otomatis ikut berubah tampilannya tanpa disentuh satu per satu. Beda
dari batch-batch sebelumnya: `MainActivity.kt` juga disentuh kali ini (root ambient gradient +
navbar tonal elevation), karena §6 spec ini secara eksplisit minta Midnight Blue cuma boleh muncul
sebagai gradient di layer ambient/root — sesuatu yang tidak bisa diekspresikan lewat token warna
solid saja.

- **`Color.kt`**: repaint total mengikuti §2 hierarki (AMOLED > glass > Midnight Blue ambient >
  tactile > accent). `TactileBackground` → §3 `AmoledBlack` 0xFF030508. `TactileSurface`/
  `TactileSurfaceVariant` → §5 `GlassBase`/`GlassElevated` 0xFF0A0F16/0xFF101722 (sebelumnya
  opaque bevel Batch 52, sekarang translucent-glass-reading). `TactileText`/`TactileSecondaryText`
  → §16 `TextPrimary`/`TextSecondary`. `TactileAccent` → §17 `AccentBlue` 0xFF6670FF.
  `TactileHighlight`/`TactileEdge` naik ke §5 `GlassHighlight`/`GlassBorder` (0.065f/0.035f,
  masih jauh di bawah `Color.White` polos sesuai §8). Token baru: `AmoledSurface`, `GlassPressed`,
  `GlassWhite`, `TactileMutedText`, `MidnightBlue` (kini murni ambient, bukan background lagi),
  `MidnightBlueAmbientAlpha`.
- **`Theme.kt`**: `TactileColors` re-wire otomatis lewat token baru; deskripsi picker tema
  diperbarui dari framing "Midnight Blue taktil" ke framing glass-first.
- **`BlurUtils.kt`**: `frostedGlass()` border Tactile ganti dari solid accent-trim
  (`primary.copy(alpha=0.22f)`, melanggar §8) ke `Brush.linearGradient(TactileHighlight,
  TactileEdge)` diagonal, sesuai §9 lighting model (satu arah cahaya top-left→bottom-right
  konsisten di semua komponen).
- **`TactileDepth.kt`**: `tactileEmboss()` bevel gradient ganti `verticalGradient` →
  `linearGradient` (diagonal top-left→bottom-right, §9), border/shadow alpha di-re-tune ke base
  token baru.
- **`MainActivity.kt`**: root `Surface` Tactile sekarang transparan + `Box.background(Brush)`
  3-stop (`background` → `MidnightBlue.copy(alpha=MidnightBlueAmbientAlpha)` → `AmoledSurface`) —
  satu-satunya tempat `MidnightBlue` benar-benar dirender di layar, sesuai §6 "Correct use"; tema
  lain (System/Light/Dark) tidak disentuh, tetap flat. `NavigationBar` `tonalElevation` Tactile
  diturunkan 12.dp → 6.dp supaya `surfaceTint` (aksen biru) tidak mendominasi nav bar sebelum
  "AMOLED glass" (§2 Golden Rule, §15).
- Tidak ada file ditambah/dihapus batch ini (murni repaint token + util pusat) — total file tetap
  108, `FILE_MANIFEST.txt` tidak berubah.
- **Belum diverifikasi visual** — sama seperti Batch 50-52, environment kerja tidak punya
  `kotlinc`/emulator. Disarankan build-test asli sebelum rilis produksi, khususnya brush diagonal
  baru dan root ambient gradient.

## Batch 52 — Timpa palet tema custom Tactile pakai spec literal Midnight Blue baru (compose-skeuomorphism-lite-midnight-blue.md)
User kirim spec baru: masih dark-mode, tapi kali ini header spec eksplisit pakai kata "Literal"
dua kali dan "Mandatory visual baseline: Literal Midnight Blue (#191970) — MANDATORY" — diterapkan
100% menggantikan palet hybrid-glass Batch 51. 6 file kode disentuh, 1 tema kohesif, atomic.

**Root cause / kenapa ini bukan sekadar recolor lagi**: spec ini menghilangkan dua hal yang jadi
inti Batch 51 — (1) §2 tabel token `Surface`/`SurfaceVariant` tidak lagi punya alpha channel
(0xFF opaque, bukan 0xCC/0xB8 translusen), jadi konsep "kaca"/glass hilang total; (2) §2 tidak
punya pasangan token gradient background (cuma satu `Background` token flat), jadi konsep
atmosfer gradient root juga hilang. Efeknya kebalikan dari Batch 51: batch ini justru
**menghapus** fitur (root gradient, glass overlay) alih-alih menambah, karena token literal
sumbernya sendiri sudah tidak meminta keduanya lagi.

**File yang diubah:**
- `Color.kt` — repaint total token Tactile dari §2 spec (literal): `TactileBackground`
  0xFF191970, `TactileSurface` 0xFF161665, `TactileSurfaceVariant` 0xFF20207A (dua terakhir ini
  **opaque lagi** — beda fundamental dari Batch 51 yang translusen), `TactileText`/
  `TactileSecondaryText` 0xFFF0F1FF/0xFFBFC2E6, `TactileAccent` 0xFF7278FF, `TactileHighlight`
  0xFFFFFFFF/alpha 0.055f, `TactileEdge` 0xFFFFFFFF/alpha 0.035f, `TactileShadow` 0xFF000000/
  alpha 0.65f — tiga terakhir ini balik ke basis `Color.White`/`Color.Black` polos (bukan
  ber-tint sendiri seperti Batch 51). `TactileBackgroundTop` dan `TactileGlassOverlay` (token
  baru Batch 51) **dihapus dari file** — tidak ada padanan di spec baru dan tidak ada lagi
  pemanggilnya setelah MainActivity.kt/BlurUtils.kt diubah (lihat di bawah). `TactileControl`/
  `TactileControlPressed` (tetap tanpa pemanggil) direfresh nilainya manual (0xFF23238A/
  0xFF0F0F4A, tidak ada literal spec utk keduanya) biar konsisten sama hierarki baru.
- `MainActivity.kt` — root `Surface` untuk tema Tactile **dikembalikan** ke
  `color = MaterialTheme.colorScheme.background` datar (dari `Color.Transparent` + `Box`
  `Brush.linearGradient` Batch 51) — spec §2 baru cuma kasih satu token `Background` flat, tidak
  ada pasangan stop gradient untuk diekspresikan. `contentColor` tetap eksplisit di kedua arah
  perubahan ini, jadi tidak pernah menyentuh bug class Batch 48. Import
  `androidx.compose.ui.graphics.Color` yang jadi tidak terpakai (satu-satunya pemakai,
  `Color.Transparent`, ikut hilang) dihapus juga. NavigationBar catch-light line tidak disentuh
  kodenya — otomatis ikut warna baru lewat Color.kt, cuma komentar diperbarui.
- `BlurUtils.kt` (`frostedGlass()`, 6 caller: `LyricsSheet`, `QueueSheet`, `MiniPlayerBar`,
  `FolderManagerSheet`, `NowPlayingScreen`, `EqualizerSheet`, semua tanpa override parameter) —
  cabang khusus Tactile dari Batch 51 (`tint` dipakai apa adanya + layer `TactileGlassOverlay`)
  **dihapus**, Tactile sekarang balik berbagi jalur generik `.copy(alpha = alpha)` yang sama
  dengan tema lain, sama seperti Batch 50 — karena `TactileSurface`/`TactileSurfaceVariant` sudah
  opaque lagi, tidak ada translucency spec-literal yang perlu dijaga dari ketimpaan alpha
  generik. Cabang non-Tactile (Apple System/Light/Dark) sama sekali tidak berubah perilakunya.
- `TactileDepth.kt` (`tactileEmboss()`, 8 caller tidak berubah) dan `NowPlayingScreen.kt`
  (AlbumArtHero) — border/shadow alpha di `tactileEmboss()` diselaraskan ke alpha literal token
  baru yang jauh lebih rendah: `borderTopAlpha` 0.09/0.04 → 0.055/0.025, `shadowAlpha` 0.68/0.35
  → 0.65/0.33 (`borderBottomAlpha` 0.30/0.15 tidak berubah, tidak ada literal spec untuk itu).
  `NowPlayingScreen.kt` nol perubahan logika, cuma komentar diperbarui — sudah mereferensikan
  token by-name jadi otomatis ikut palet baru.
- `Theme.kt` — `TactileColors.onPrimary` diganti `Color.Black` → `Color.White`: `TactileAccent`
  baru (0xFF7278FF) simple-luma ≈0.52, di bawah ambang 0.55 yang dipakai project (beda dari
  Batch 51 yang ≈0.59, masih di atas ambang) — `Color.Black` di atasnya sekarang jadi kontras
  rendah. `onTertiary` tetap `Color.Black` (`TactileSuccess` tidak berubah). `storageKey` dan
  struktur `darkColorScheme()` lainnya tidak berubah. Deskripsi `AppTheme.TACTILE` diupdate ke
  "Midnight Blue taktil terprogram… bevel fisik", menghapus kata "kaca"/translusen yang sudah
  tidak akurat.
- `README.md` — deskripsi tema Tactile di daftar fitur diupdate: nama spec, nomor batch, dan
  penghapusan kata "gradient"/"kaca translusen" diganti "literal Midnight Blue (#191970)
  rata/opak".

**Di luar cakupan, disengaja**: sama seperti batch-batch sebelumnya, spec §7/§12 minta komponen
`TactileButton`/`TactileSwitch`/`TactileSlider` custom penuh di `ui/components/` — batas ini
tidak berubah (slider/toggle/switch tetap Material3 polos).

**Belum diverifikasi runtime asli** (tidak ada compiler Android di environment kerja) — analisis
statis + brace/paren balance dicek otomatis (skrip Python, bukan manual) di semua file yang
disentuh, 0 selisih kurung/brace di keenamnya.

## Batch 51 — Timpa palet tema custom Tactile pakai spec hybrid-glass dark-blue baru (compose-skeuomorphism-lite-hybrid-glass-dark-blue.md)
User kirim spec baru: masih dark-mode, tapi eksplisit menolak AMOLED-black sebagai tujuan
(§1.1: "Pure/AMOLED-black styling is not the target") dan minta atmosfer gradient navy→biru-gelap
plus permukaan kaca translusen ("hybrid glassmorphism"), diterapkan 100% menggantikan palet
Batch 50. 6 file kode disentuh, 1 tema kohesif, atomic.

**Root cause / kenapa ini bukan sekadar recolor lagi**: dua hal di spec ini tidak ada
padanannya di spec Batch 49/50 manapun — (1) `Surface`/`SurfaceVariant` di tabel §2 secara
eksplisit dikasih nilai literal ber-alpha-channel (0xCC/0xB8, bukan 0xFF opaque), artinya
translucency itu sendiri adalah bagian dari desain token, bukan sesuatu yang ditambah lewat
parameter `alpha` di modifier seperti sebelumnya; (2) §2/§8 minta *gradient* background, bukan
flat color — sesuatu yang `MaterialTheme.colorScheme.background` (tipe `Color`, bukan `Brush`)
secara struktural tidak bisa mengekspresikan sendirian.

**File yang diubah:**
- `Color.kt` — repaint total token Tactile dari §2 spec (literal): `TactileBackground`
  0xFF050B18, `TactileBackgroundTop` (BARU) 0xFF0A1630, `TactileSurface` 0xCC101D35,
  `TactileSurfaceVariant` 0xB8142745 (dua terakhir ini translusen — beda fundamental dari
  Batch 50 yang opaque penuh), `TactileGlassOverlay` (BARU) 0x142E6AA3, `TactileAccent`
  0xFF5B9DFF, `TactileHighlight` 0xFFEAF4FF/alpha 0.07f, `TactileEdge` 0xFF8FB9E8/alpha 0.10f,
  `TactileShadow` 0xFF020817/alpha 0.68f — tiga terakhir ini sekarang punya warna dasar
  ber-tint sendiri dari spec (bukan generic `Color.White`/`Color.Black` ber-alpha rendah
  seperti Batch 50). `TactileText`/`TactileSecondaryText` tidak berubah nilainya (spec
  kebetulan pakai literal identik Batch 50). `TactileControl`/`TactileControlPressed`
  (tetap tanpa pemanggil) direfresh nilainya manual (tidak ada literal spec utk keduanya) biar
  konsisten sama arah warna baru.
- `MainActivity.kt` — root `Surface` untuk tema Tactile diganti `color = Color.Transparent`
  (dari `MaterialTheme.colorScheme.background` datar), dibungkus `Box` baru dengan
  `Brush.linearGradient(TactileBackgroundTop -> colorScheme.background, start = Offset(0f,0f),
  end = Offset.Infinite)` — gradient diagonal kiri-atas→kanan-bawah sesuai arah cahaya spec
  §3, dipakai juga di level atmosfer background per §8. Tema lain (Apple System/Light/Dark)
  sama sekali tidak tersentuh — root Surface-nya tetap pakai jalur lama, opaque flat color.
  **Penting soal keamanan pola ini**: `contentColor` tetap SELALU diisi eksplisit
  (`MaterialTheme.colorScheme.onBackground`) di kedua cabang if/else color, jadi
  `contentColorFor(Transparent)` tidak pernah dikonsultasi — beda akar masalah dari bug Batch
  48 (Surface Transparent yang lupa isi `contentColor` sama sekali, sehingga Compose jatuh ke
  fallback `LocalContentColor` yang belum di-set → hitam). NavigationBar catch-light line
  (kode `TactileHighlight.copy(alpha=...)`) tidak disentuh — otomatis ikut warna baru lewat
  Color.kt.
- `BlurUtils.kt` (`frostedGlass()`, 6 caller: `LyricsSheet`, `QueueSheet`, `MiniPlayerBar`,
  `FolderManagerSheet`, `NowPlayingScreen`, `EqualizerSheet`, semua tanpa override parameter)
  — ditambah cabang khusus Tactile: `tint` dipakai apa adanya (translucency native dari
  Color.kt terjaga) + layer `TactileGlassOverlay` ekstra di atasnya, alih-alih cabang lama
  `.copy(alpha = alpha)` yang untuk Tactile akan MENIMPA alpha spec-literal jadi hampir
  opaque (0.92f) — bug regresi kalau tidak diperbaiki, karena §2/§8 spec ini secara eksplisit
  mensyaratkan permukaan glass yang translusen. Cabang non-Tactile (Apple System/Light/Dark)
  sama sekali tidak berubah perilakunya.
- `TactileDepth.kt` (`tactileEmboss()`, 8 caller tidak berubah) dan `NowPlayingScreen.kt`
  (`AlbumArtHero`) — **nol perubahan logika**, cuma dokumentasi komentar diperbarui. Berkat
  kedua file ini mereferensikan token Color.kt by-name (bukan hardcode literal), efek
  hybrid-glass (translucency + tint biru) otomatis muncul begitu Color.kt berubah — pola yang
  sama dipakai Batch 50 untuk merecolor Batch 49 tanpa menyentuh struktur kode.
- `Theme.kt` — `TactileColors` tetap `darkColorScheme()` (spec §13 masih larang light-mode
  fallback, tidak berubah dari Batch 50); `resolveIsDark(TACTILE)` tetap `true`; deskripsi
  tampilan `AppTheme.TACTILE` (string yang muncul di UI picker tema) diupdate dari "Bevel gelap
  AMOLED terprogram" jadi "Kaca biru-gelap terprogram… permukaan translusen" biar tidak
  menyesatkan. `storageKey` ("tactile_lite") TIDAK berubah — ini masih identitas Tactile yang
  sama, cuma repaint ketiga, jadi tidak perlu migrasi preferensi tersimpan seperti Batch 49.

**Di luar cakupan, disengaja**: sama seperti Batch 49/50, spec §7/§12 minta komponen
`TactileButton`/`TactileSwitch`/`TactileSlider` custom penuh di `ui/components/` — tidak
dikerjakan batch ini supaya scope tetap atomic.

**Risiko yang belum diaudit**: `colorScheme.surface`/`surfaceVariant` kini translusen secara
default di seluruh `TactileColors` scheme, bukan cuma lewat `tactileEmboss()`/`frostedGlass()`
— artinya Card/Surface M3 polos manapun di layar lain yang masih memanggil
`colorScheme.surface` langsung (tanpa lewat 2 helper itu) ikut jadi translusen otomatis. Ini
SESUAI mandat spec §8 (glass di seluruh hierarchy komponen), tapi belum diverifikasi apakah
ada titik yang jadi kurang terbaca — khususnya komponen yang dirender di window Compose
terpisah dari root gradient Box (misal `ModalBottomSheet`/`Dialog`), yang tidak akan punya
gradient di baliknya untuk ditembus translucency-nya.

**Belum diverifikasi runtime asli** (tidak ada compiler Android di environment kerja) —
analisis statis + brace/paren balance dicek manual di semua file yang disentuh (lihat laporan
build-check di bawah). Prioritas sesi berikutnya: build-test asli + verifikasi visual device,
khususnya arah/kecepatan gradient root baru (`Offset.Infinite` untuk diagonal) dan titik
translucency-tanpa-latar di atas.

## Batch 50 — Timpa palet tema custom Tactile pakai spec dark-mode baru (compose-skeuomorphism-lite-dark.md)
User kirim spec baru khusus dark-mode/AMOLED, minta diterapkan 100% ke tema Tactile
(menggantikan palet TERANG dari Batch 49). Spec §1.1 eksplisit: "Do not simply invert a light
theme" — jadi token diambil ulang literal dari spec §2, bukan sekadar menggelapkan nilai lama.
6 file kode + 3 dokumentasi, 1 tema kohesif (atomic).

1. **Color.kt** — palet Tactile diganti total: `TactileBackground` 0xFF05070A,
   `TactileSurface` 0xFF0B0F14, `TactileSurfaceVariant` 0xFF111720, `TactileText` 0xFFE8EEF5,
   `TactileSecondaryText` 0xFFA8B3C0, `TactileAccent` 0xFF4DA3FF (biru dingin, ganti tembaga),
   `TactileHighlight`/`TactileEdge`/`TactileShadow` (Color.White/Black alpha rendah) — semua
   nilai literal §2 spec. `TactileControl`/`TactileControlPressed` ditambah dari tabel token §2
   (belum ada pemanggil, disiapkan untuk komponen tactile masa depan). `TactileError`/
   `TactileSuccess` tidak ada literal di spec, dipilih manual agar cocok skema biru-dingin.
2. **TactileDepth.kt (`tactileEmboss()`)** — signature tidak berubah (8 pemanggil otomatis
   ikut). Alpha border diturunkan drastis mengikuti spec §4 ("Do NOT use a bright Color.White
   border"): top/bottom 0.9/0.45 → 0.09/0.30 (normal), 0.35/0.20 → 0.04/0.15 (pressed). Shadow
   drop justru DIPERTAHANKAN dekat alpha penuh (0.65, literal spec) — pelajaran dari saga Matte
   Noir Batch 39-44 dipakai lagi: bayangan hitam-di-atas-hitam yang alpha-nya terlalu rendah
   hilang total, bukan sekadar "restrained".
3. **Theme.kt** — `TactileColors`: `lightColorScheme()` → `darkColorScheme()`;
   `resolveIsDark(TACTILE)` `false` → `true` (otomatis membalik ikon status bar/nav bar jadi
   terang, `MainActivity.kt` tidak perlu disentuh manual untuk itu); `onPrimary`/`onTertiary`
   pakai aturan luminance yang sama dengan `MiniPlayerBar.kt` (>0.55 → hitam).
4. **NowPlayingScreen.kt (AlbumArtHero) + MainActivity.kt (garis NavigationBar)** — 2 titik
   manual di luar `tactileEmboss()` diselaraskan ke aturan sama: NavigationBar 0.9/0.05 →
   0.10/0.02 (dulu nyaris garis putih opaque, melanggar §4 langsung); AlbumArtHero border
   0.9/0.40 → 0.12/0.32, shadow 0.28 → 0.55, glow aksen lagu 0.5 → 0.42 (§9 izinkan glow di
   elemen selected/active, tetap direstrain).
5. **BlurUtils.kt** — trim aksen `frostedGlass()` (6 pemanggil) alpha 0.35 → 0.22, penyesuaian
   restraint karena aksen biru baru terasa lebih terang dari tembaga lama di alpha yang sama.

**Di luar cakupan, disengaja**: spec §7/§12 minta komponen `TactileButton`/`TactileSwitch`/
`TactileSlider` custom penuh di `ui/components/` — batas ini sama seperti Batch 49
(slider/toggle/switch tetap Material3 polos). Bukan diabaikan, tapi scope terpisah & lebih besar
dari "recolor tema" kalau user memang mau cakupan itu juga.

**Belum diverifikasi runtime asli** (tidak ada compiler Android di environment kerja) — analisis
statis + brace/paren balance dicek manual di semua file yang disentuh.

## Batch 46 — Timpa tema custom Matte Noir pakai spec user "Skeuomorphism-lite" (compose-skeuomorphism-lite.md)
User bilang hasil Batch 40-44 "jelek banget asli" dan kirim markdown spec desain sendiri untuk
dipakai. Ditimpa total, bukan tempel di atas yang lama: `MatteDepth.kt` (`matteEmboss()`)
ditulis ulang dari nol mengikuti 3 poin spec:
1. **Tactile depth (spec §1)** — dari gradient diagonal 3-warna + 2 layer shadow offset
   ("epic" stack Batch 42) jadi 1 gradient vertikal atas→bawah + 1 shadow tunggal, plus bevel
   border gradient vertikal (terang di atas → gelap di bawah) menggantikan ring catch-light
   satu sisi — persis instruksi spec "layering contrasting light and dark borders".
2. **Micro-interactions (spec §2)** — param `pressed` sekarang benar-benar animasi
   (`animateDpAsState`/`animateFloatAsState`: elevation collapse + scale 0.985) bukan swap
   alpha instan seperti sebelumnya — efek "fisik ketekan" sesuai spec.
3. **Isolated accents (spec §3)** — intensitas diturunkan di semua parameter default (alpha,
   elevation) supaya `matteEmboss()` sekarang treatment untuk card struktural yang flat/minimal,
   bukan efek berat di semua titik. Sesuai spec, slider/toggle sengaja TIDAK diskin dengan
   modifier ini — tetap komponen Material3 biasa, tidak diubah batch ini.
Karena signature (`shape`/`elevation`/`pressed`) tidak berubah, 5 dari 6 pemanggil lama (mini
player, Home, Library, Settings, NavigationBar di `MainActivity.kt`) otomatis ikut tampilan baru
tanpa disentuh sama sekali. Satu pemanggil manual yang TIDAK lewat `matteEmboss()` — AlbumArtHero
di `NowPlayingScreen.kt`, sengaja beda sejak Batch 40 — juga ditulis ulang ke teknik yang sama
(shadow gambar-manual + border gradient vertikal) supaya konsisten 1 bahasa visual, sekalian
buang native `Modifier.shadow` ganda yang sudah terbukti invisible di background nyaris-hitam
(temuan Batch 40/41, sekarang dikonfirmasi ulang berlaku juga di titik ini). 2 file disentuh
(`MatteDepth.kt` ditulis ulang penuh, `NowPlayingScreen.kt` 1 blok + 3 baris import). **Belum
diverifikasi runtime asli** (tidak ada compiler Android di environment kerja) — analisis statis
+ brace/paren balance. Prioritas sesi berikutnya: user test di device asli — kalau masih
"kureng"/jelek, JANGAN ulangi pola nambah layer shadow/gradient lagi (sudah 4x gagal dengan pola
itu, Batch 40→44); minta screenshot spesifik bagian mana yang salah dulu.

## Batch 47 — Hotfix Batch 46: compileDebugKotlin FAILED, `by` delegate tanpa import getValue
`log_fail_104.zip` → `MatteDepth.kt:59` & `:63`: `Type 'State<Dp>' has no method
'getValue(...)' and thus it cannot serve as a delegate` pada `val animatedElevation by
animateDpAsState(...)` dan `val scale by animateFloatAsState(...)`. Root cause: Batch 46
menulis ulang file ini dari nol dan pakai sintaks `by` tapi lupa
`import androidx.compose.runtime.getValue` (operator delegate `by` untuk `State<T>` butuh
import eksplisit ini, bukan otomatis ikut `androidx.compose.runtime.Composable`). File lain
yang disentuh Batch 46 (`NowPlayingScreen.kt`) tidak kena karena sudah punya
`import androidx.compose.runtime.*` dari sebelumnya. Fix: tambah 1 baris import. 1 file, fix
atomik. **Belum diverifikasi runtime asli** — tapi ini kesalahan compile-time yang exact match
dengan error log (bukan tebakan), jadi confidence tinggi dibanding hotfix Batch 41/43
sebelumnya yang sempat salah tebak duluan.

## Batch 49 — Hapus total identitas "Matte Noir" lama, ganti dengan tema custom baru "Tactile" murni dari spec skeuomorphism-lite.md
User minta eksplisit: hapus SEMUA jejak tema custom lama sampai bersih, baru terapkan spec baru
— bukan overlay/patch di atas yang lama (beda dari Batch 46 yang cuma nulis ulang
`matteEmboss()` tapi masih di palet gelap Matte). Atomic change, 11 file (di atas batas normal
8-10 file, dijustifikasi sebagai 1 modul tema yang tidak bisa dipecah tanpa state build rusak
di tengah jalan — hapus warna Matte sementara call site masih pakai nama lama = gagal compile):

**Dihapus total:** `MatteDepth.kt` (file dihapus), semua `val Matte*` di `Color.kt`
(`MatteBackground/Surface/SurfaceVariant/Text/SecondaryText/Accent/Error/Success/Highlight/
Umbra`), `MatteTypography` (`Type.kt`), `MatteColors`/`MatteShapes`/`matteDepthBrush()`
(`Theme.kt`), `AppTheme.MATTE` enum entry + storage key `"matte_noir"` (pengguna lama yang masih
tersimpan preferensi ini otomatis fallback ke SYSTEM lewat `fromStorageKey()` — bukan crash,
cuma reset preferensi, disengaja).

**Diganti dengan (baru, bukan reskin):** `TactileDepth.kt` (`tactileEmboss()`, logic sama
persis dengan hasil Batch 46/47 yang sudah teruji sesuai 3 poin spec — cuma direcolor total),
`Color.kt` dapat palet TERANG baru (`TactileBackground/SurfaceHighlight/SurfaceShadow/
SurfaceVariant/Text/SecondaryText/Accent/Error/Success/Highlight/Shadow`) — `TactileSurfaceHighlight`
(0xFFF8FAFC) dan `TactileSurfaceShadow` (0xFFE2E8F0) adalah warna LITERAL dari contoh kode di
spec §1, bukan cuma terinspirasi. `TactileTypography` (Type.kt, sans-serif semua, ExtraBold
untuk title — spec tidak mensyaratkan font khusus). `TactileColors`/`TactileShapes`
(`Theme.kt`) — `lightColorScheme` (bukan dark lagi), rounding 10/12/16dp (bukan sharp 4/6/8dp
Matte, bukan juga rounding besar ala Apple).

**Bonus fix arsitektural, mencegah kelas bug Batch 48 terulang selamanya:** root `Surface` di
`MainActivity.kt` yang dulu pakai trik `color = Color.Transparent` (khusus Matte, buat ambient
glow tembus) DIHAPUS total bersama `matteDepthBrush()` — sekarang root Surface SELALU opaque +
`contentColor` eksplisit untuk semua tema, jadi tidak ada lagi Surface `Transparent` di root
sama sekali. 4 titik Surface lain yang juga pakai pola `color = if (isMatte) Transparent else
...` (mini player tak ada, tapi Library undo-bar, Settings ThemeOptionCard, Home
ContinueListeningCard, NowPlaying GestureIndicatorBadge) SEMUA ditambah `contentColor` eksplisit
juga sebagai pencegahan, bukan cuma di-rename ke Tactile — supaya kalau nanti ada tema
Transparent lagi, tidak akan pernah lagi diam-diam jatuh ke `Color.Black` default Compose.

**File yang disentuh (11):** `Color.kt`, `Type.kt`, `Theme.kt`, `TactileDepth.kt` (baru,
`MatteDepth.kt` dihapus), `BlurUtils.kt`, `MiniPlayerBar.kt`, `LibraryScreen.kt`,
`SettingsScreen.kt`, `HomeScreen.kt`, `NowPlayingScreen.kt`, `MainActivity.kt`.
`FILE_MANIFEST.txt` diupdate (baris `MatteDepth.kt` → `TactileDepth.kt`).

**Verifikasi sebelum packing:** brace/paren balance semua 11 file file OK; grep akhir
`Matte|isMatte|matteEmboss|matteDepthBrush|MATTE|matte_noir` di seluruh `.kt` → nol hasil aktif
(cuma komentar historis yang menyebut nama lama sebagai konteks, bukan simbol kode); dicek
manual `import androidx.compose.runtime.getValue` ADA di `TactileDepth.kt` (pelajaran langsung
dari kegagalan Batch 47 — operator `by` butuh import ini, jangan sampai lupa lagi). **Belum
diverifikasi runtime asli** (tidak ada compiler Android di environment kerja) — tapi ini batch
dengan cakupan terbesar sejauh ini, jadi risiko ada 1 typo/kasus tak terduga di suatu tempat
lebih tinggi dari batch-batch kecil sebelumnya. **Prioritas mutlak sesi berikutnya: build actual
di GitHub Actions + user verifikasi visual di device** sebelum nambah fitur/tema apapun lagi di
atas ini.

## Batch 48 — Fix nyata: teks nyaris invisible (bukan soal "jelek", ini bug kontras hitam-di-hitam)
User kirim screenshot layar PIN pasca-reinstall Batch 47 — digit keypad & judul "Masukkan PIN"
render HITAM di atas background nyaris-hitam. Root cause BUKAN di `MatteDepth.kt` (yang sudah
benar sejak Batch 47), tapi di `MainActivity.kt` root `Surface` pembungkus seluruh konten
(`needsUnlock -> LockScreen(...)` / `hasPermission -> AppNavHost(...)`), sisa trik Batch 40:
`Surface(color = Color.Transparent)` khusus Matte theme supaya ambient-glow radial di belakang
tembus. Masalahnya: `AudioPlayerTheme()` (`Theme.kt`) cuma bungkus `MaterialTheme(...)` — TIDAK
PERNAH ada `Surface` di root, jadi `LocalContentColor` tidak pernah di-set ke `onBackground`
sampai SUATU `Surface` men-set-nya. `Surface`'s default `contentColor` param dihitung dari
`contentColorFor(color)`; untuk `color = Color.Transparent`, ini tidak cocok role manapun di
ColorScheme → `Unspecified` → fallback ke `LocalContentColor` yang SEDANG AKTIF, yaitu default
mentah Compose sendiri: `Color.Black` (belum pernah di-override). Jadi utk Matte theme, semua
child di bawah Surface ini mestinya kena teks hitam — tapi Library kelihatan baik-baik saja
karena tiap row list-nya punya Card/Surface sendiri dengan background opaque yang menghitung
ulang contentColor dengan benar secara lokal. LockScreen (cuma `Column`+`Box` polos, tanpa
Surface sendiri) satu-satunya layar yang membiarkan bug ini polos tanpa "penyelamat" lokal —
makanya paling kentara di situ, meski secara teori seluruh app kena bug yang sama, cuma
ketutupan oleh Surface/Card lokal di layar lain. Fix: set `contentColor` eksplisit
`= MaterialTheme.colorScheme.onBackground` di `Surface` root itu — no-op untuk tema non-Matte
(sudah benar lewat jalur `color` biasa di situ), memperbaiki kasus Matte yang transparent. 1
file (`MainActivity.kt`), 1 blok, fix atomik. **Belum diverifikasi runtime asli** tapi ini akar
masalah yang persis cocok sama gejala di screenshot (hitam-di-hitam, bukan cuma "kurang epic"),
bukan tebakan kosong. **Batch ini adalah bug LAMA sejak Batch 40** (root cause-nya trik
`Surface(color=Transparent)`), baru kelihatan sekarang karena baru sekarang LockScreen
di-screenshot dalam kondisi Matte theme aktif — bukan regresi baru dari Batch 46/47.

## Batch 45 — Fix bug lama: SignatureMatcherSheet bandingkan key signing yang SALAH kalau app pernah rotasi key
User laporan "gak sinkron" di fitur pencocok signature APK (`SignatureMatcherSheet` +
`ApkSignatureChecker`). Root cause di `ApkSignatureChecker.inspect()` (API 28+, jalur
`GET_SIGNING_CERTIFICATES`): saat `hasMultipleSigners() == false` (kasus normal, app
bersertifikat tunggal), kode ambil `signingInfo.signingCertificateHistory.firstOrNull()`.
Menurut dokumentasi resmi `SigningInfo.getSigningCertificateHistory()`: array ini terurut
**oldest→newest**, sertifikat original di index 0, sertifikat **AKTIF/current** di index
**terakhir**. `.firstOrNull()` diam-diam ambil sertifikat PALING LAMA, bukan yang dipakai
sekarang — untuk app mana pun yang pernah melalui APK Signature Scheme v3 key rotation, hasil
SHA-256 yang dibandingkan bukan key yang sebenarnya menentukan apakah Android mengizinkan
update in-place. Ini persis sumber "gak sinkron": hasil MATCH/MISMATCH di UI bisa berbeda dari
apa yang sebenarnya dilakukan installer Android. Fix: `.lastOrNull()` untuk cabang
`signingCertificateHistory`. Cabang `hasMultipleSigners() == true` (`apkContentsSigners`)
tidak diubah — app multi-signer tidak bisa rotasi key sama sekali, jadi urutan tidak relevan
di situ. 1 file (`ApkSignatureChecker.kt`), fix atomik. **Belum diverifikasi runtime asli**
(tidak ada compiler Android di environment kerja) — analisis statis + brace/paren balance +
cross-check langsung ke dokumentasi resmi `SigningInfo` (bukan asumsi/tebakan, lihat pelajaran
Batch 14/33 soal ini). Kasus paling mungkin memicu laporan user: salah satu dari 2 APK yang
dibandingkan (biasanya "APK Lama") pernah dirilis dengan key rotation aktif.

## Batch 44 — Hotfix Batch 43 (fix-nya sendiri salah): drawOutline tidak pernah ada
`log_fail_6.zip` user tunjukkan build masih gagal setelah Batch 43 — dengan pesan
`Unresolved reference: drawOutline` yang sama, **termasuk di baris import itu sendiri**
(`import androidx.compose.ui.graphics.drawscope.drawOutline`). Itu petunjuk kunci: kalau
importnya sendiri unresolved, artinya tidak ada simbol top-level bernama itu di path tersebut
sama sekali — bukan soal lupa import seperti dugaan Batch 43.

Verifikasi langsung ke source resmi AOSP `DrawScope.kt` (bukan cuma dokumentasi/blog):
interface `DrawScope` **tidak punya fungsi `drawOutline`** — anggota resminya cuma
`drawLine`/`drawRect`/`drawRoundRect`/`drawCircle`/`drawOval`/`drawArc`/`drawPath`/
`drawPoints`. Fungsi `drawOutline` yang dipakai Batch 42 itu tidak pernah eksis — dugaan awal
"itu member DrawScope" salah total. Konversi `Outline` yang benar: `Path().apply {
addOutline(outline) }` (extension asli `androidx.compose.ui.graphics.addOutline`, dikonfirmasi
ada di listing resmi paket itu), lalu digambar pakai `drawPath(path, color)` — member asli
`DrawScope`, tidak perlu import apa pun. Fix: ganti 2 pemanggilan `drawOutline(...)` jadi
`Path().apply { addOutline(outline) }` + `drawPath(outlinePath, color = ...)`, hapus import
`drawOutline` yang tidak valid, tambah import `Path` dan `addOutline`. **Pelajaran: nama fungsi
yang terlihat masuk akal (mirip pola `translate` yang memang valid) BUKAN bukti fungsi itu
ada — begitu error "unresolved" muncul bahkan di baris import-nya sendiri, itu sinyal untuk
verifikasi ke source/reference resmi API tersebut dulu, bukan menambah import lain dengan
tebakan nama yang mirip.** Belum diverifikasi ulang di device asli — prioritas sesi berikutnya
tetap sama: pastikan build sukses dulu, baru cek shadow benar-benar kelihatan.

## Batch 43 — Hotfix build gagal dari Batch 42: `drawOutline` unresolved reference
Build CI gagal total di `compileDebugKotlin`, ditemukan dari `log_fail_5.zip` (test-output.log)
yang diupload user: `Unresolved reference: drawOutline` di 2 baris `MatteDepth.kt` (dalam
`drawBehind {}` yang ditambah Batch 42). Root cause simpel: `drawOutline(outline, color)` di
dalam `DrawScope` adalah **extension function** di `androidx.compose.ui.graphics.drawscope`
(bukan method bawaan `DrawScope` seperti `drawRect`/`drawCircle`), jadi wajib diimpor eksplisit
sama seperti `translate` yang di baris sebelahnya sudah benar diimpor — cuma `drawOutline`-nya
yang lupa. Fix: tambah `import androidx.compose.ui.graphics.drawscope.drawOutline`. **Pelajaran:
saat menambah pemanggilan fungsi baru di dalam lambda `DrawScope` (`drawBehind`/`Canvas`/dst.),
cek satu per satu apakah tiap fungsi itu method resmi `DrawScope` atau extension function
terpisah — keduanya terlihat identik dipanggil tanpa prefix di dalam lambda, tapi cuma yang
kedua butuh import manual. Tanpa `kotlinc` di environment kerja, kesalahan sekelas ini hanya
kelihatan dari build CI yang benar-benar gagal, bukan dari baca kode statis — semakin
menguatkan kenapa `log_fail_*.zip` dari user harus selalu dicek duluan sebelum menebak.**

## Batch 42 — Hotfix Batch 41 (lagi): ganti native shadow ke manual drawBehind
Screenshot device asli setelah Batch 41 dipasang (elevation dinaikkan) menunjukkan shadow
`matteEmboss()` **masih** nyaris tak kelihatan di semua 6 titik (Beranda, Pengaturan tema,
dst) — cuma border seleksi oranye yang kelihatan beda, bukan shadow-nya. Kesimpulan: fix
elevation Batch 41 tidak cukup karena akar masalahnya lebih dalam dari sekadar elevation atau
warna — opacity shadow native `Modifier.shadow` (RenderNode ambient/spot) punya batas atas
(cap) yang rendah begitu background di belakangnya sudah gelap pekat, dan tidak ada nilai
elevation yang bisa mendorongnya lewat batas itu.

Fix: `Modifier.shadow` dibuang total dari `matteEmboss()`, diganti shadow manual lewat
`Modifier.drawBehind` — ambil `Outline` dari `shape` yang sama, gambar 2 layer (halo lebar
alpha 0.30f + core rapat alpha 0.5f, keduanya `MatteUmbra`) yang di-offset ke kanan-bawah
proporsional terhadap `elevation`. Kontras shadow sekarang murni dikontrol alpha channel yang
kita set sendiri, bukan API OS — jadi tidak bisa lagi diam-diam mendegradasi jadi tak terlihat
seperti yang terjadi 2x berturut-turut (Batch 40, 41). Trade-off: bukan blur Gaussian fisik
asli, cuma silhouette offset — cukup untuk kontras yang pasti terlihat, bukan estetika shadow
paling halus. **Pelajaran: begitu 1 pendekatan (native platform shadow) gagal terlihat 2x
berturut-turut dengan variasi parameter berbeda (warna lalu elevation), jangan coba variasi
parameter ketiga di API yang sama — ganti total ke mekanisme yang kontrasnya kita kontrol
langsung, bukan diserahkan ke platform.** Belum diverifikasi ulang di device asli.

## Batch 41 — Hotfix Batch 40: shadow matteEmboss() invisible di device asli
Ditemukan dari screenshot render asli (bukan cuma baca kode) setelah Batch 40 dipasang.
Root cause: `MatteUmbra` (0xFF080503) cuma ~12 unit lebih gelap dari `MatteBackground`
(0xFF14120F) — warna shadow-nya "ada" di kode tapi nyaris tak terlihat karena kontras
mepet. Ditambah, tint warna (`ambientColor`/`spotColor`) di `Modifier.shadow` native cuma
mengubah HUE, bukan opacity — opacity shadow dikontrol `elevation`, bukan kontras warnanya.
Fix: naikkan `elevation` di semua 6 titik pakai `matteEmboss()` (bukan ganti warna) supaya
shadow native render dengan spread/opacity cukup: default 10dp→16dp, dan tiap call-site
eksplisit disesuaikan sebanding (MiniPlayerBar tetap 16dp, LibraryScreen 12→18dp,
SettingsScreen 8/14→13/20dp selected, HomeScreen 10→16dp, NowPlayingScreen 8→13dp). Highlight
gradient stop juga dinaikkan (0.14f→0.24f non-pressed, 0.05f→0.09f pressed) — dites di kartu
kecil (mini-player, badge) yang sebelumnya kelihatan flat di skala render asli. Umbra alpha di
gradient fill 0.45f→0.65f. **Pelajaran: tuning warna/opacity untuk efek shadow Compose native
tidak cukup dicek dari baca kode/nilai hex saja — kontras warna vs elevation-driven opacity
harus divalidasi di screenshot render asli sebelum dianggap selesai, persis seperti yang sudah
diwanti-wanti di Batch 40 sendiri.**

## Batch 40 — Matte Noir: "epic" depth pass menyeluruh (neumorphism + directional light)
User diberi penjelasan 4 gaya kedalaman (neumorphism, skeuomorphic, glass gelap, elevasi+gradient
terarah) sebelum memilih; keluhan awal "semua area belum kerasa premium" (bukan 1 titik spesifik)
setelah Batch 39 murni analisis statis tanpa pernah dirender. Dipilih kombinasi neumorphism ringan
+ elevasi/gradient cahaya terarah, diterapkan lewat 1 helper terpusat baru supaya konsisten di
semua touch point sekaligus (bukan tambal-sulam per file seperti Batch 39):

- **`MatteDepth.kt` (baru)** — `Modifier.matteEmboss(shape, elevation, pressed)`: menggabungkan
  (1) shadow dua-warna terarah (`ambientColor`/`spotColor` = `MatteUmbra`, coklat-hitam hangat,
  bukan hitam pekat generik — tint berwarna cuma render di API 28+, di bawahnya fallback abu-abu
  polos tanpa crash, minSdk 23 tetap aman), (2) `Brush.linearGradient` diagonal
  `MatteHighlight → MatteSurface → MatteUmbra` menggantikan fill solid — mensimulasikan satu
  sumber cahaya jatuh dari kiri-atas, inti dari "kedalaman 3D" yang diminta, dan (3) border 1dp
  brush `MatteHighlight → transparent` di sisi kiri-atas yang sama — cue fisik neumorphism
  (tepi panel yang "menangkap" cahaya) yang tidak bisa dipalsukan cuma dari tonalElevation datar.
  Parameter `pressed` meredupkan ketiganya untuk state tertekan (efek "masuk ke permukaan").
- **`Color.kt`**: 2 warna baru — `MatteHighlight` (krem-tembaga terang, catch-light) dan
  `MatteUmbra` (hitam hangat nyaris-tapi-bukan-pekat, shadow) — dua ujung satu sumber cahaya yang
  dipakai `matteEmboss()` di atas.
- **`Theme.kt`**: `matteDepthBrush()` (glow ambient root, `MainActivity.kt`) alpha dinaikkan
  0.10f → 0.22f + 1 stop highlight tambahan — versi Batch 39 (belum pernah dirender di device
  asli) ternyata terlalu halus dibanding kecerahan layar/cahaya ambient sungguhan.
- **7 touch point** dipasangi `matteEmboss()` (Matte-only, gated `isMatte`, tema lain 100% tidak
  berubah): `MiniPlayerBar` (ganti shadow copper ad-hoc lama), `ContinueListeningCard` di
  `HomeScreen` (sekaligus perbaiki shape hardcode 18dp yang dulu tidak ikut token tema),
  Undo-snackbar `LibraryScreen`, `GestureIndicatorBadge` (badge volume/brightness)
  `NowPlayingScreen`, `ThemeOptionCard` Matte Noir di `SettingsScreen` (jadi showcase depth
  hidup di picker-nya sendiri), plus `NavigationBar` `MainActivity` (garis catch-light 2px tepi
  atas, tanpa restrukturisasi internal komponen M3-nya).
- **`AlbumArtHero` (`NowPlayingScreen`, fokus visual utama layar Now Playing)** dapat perlakuan
  khusus lebih kuat dari `matteEmboss()` biasa: shape hardcode 28dp diganti `MaterialTheme.shapes.large`
  (ikut token Matte yang sengaja nyaris kotak), 2 shadow ditumpuk (umbra tembaga 20dp + accent
  glow lama 18dp tetap dipertahankan) + border catch-light 1.5dp — bukan dipindah ke helper
  generik karena sudah punya accent-glow per-lagu sendiri yang perlu tetap jalan bersamaan.

9 file (1 baru + 8 diedit), 1 tema kohesif (depth-system rollout, atomic — helper generik +
2 file warna/brush dasar + 6 titik pemakaian tak terpisah tanpa saling pecah konsistensi visual).
**Tetap belum diverifikasi build/runtime asli** (tidak ada compiler Android di environment
kerja) — analisis statis + script brace/paren balance manual. Shadow berwarna
(`ambientColor`/`spotColor`) hanya tampak penuh di API 28+; di device API 23-27 kembali ke abu-abu
default (bukan crash, cuma kurang dramatis) — belum ada cara memverifikasi ini tanpa device fisik
lawas. **Kalau user test lagi masih bilang "kureng"**: jangan ulangi pola menambah lebih banyak
shadow/gradient tanpa tahu titik spesifik — Batch 39 & 40 sama-sama tidak pernah dirender
sungguhan, jadi kemungkinan root cause selanjutnya bukan soal "kurang banyak efek" tapi sesuatu
yang cuma kelihatan di layar asli (kontras warna, ukuran shadow relatif terhadap DPI, dll).

## Batch 39 — Matte Noir: efek kedalaman visual (respons user test Batch 38 "masih kureng")
User test Batch 38 di HP asli: tema Matte Noir dirasa datar ("kureng"), minta ada efek
kedalaman/3D yang bikin dia unik. Root cause yang ditemukan sekalian: `darkColorScheme()`/
`lightColorScheme()` M3 diam-diam mengisi role yang tidak disebutkan eksplisit (termasuk
`surfaceTint`, dipakai M3 untuk automatic tonal-elevation overlay — mekanisme utama M3
menandakan kedalaman tanpa drop shadow manual) dengan baseline ungu default M3, BUKAN
diturunkan dari `primary` custom app ini — jadi semua Surface/Card/NavigationBar yang pakai
`tonalElevation` otomatis dapat tint ungu generik, bukan warna tema. 5 perubahan, 1 tema
kohesif (Matte Noir depth pass):

- **`surfaceTint` eksplisit** ditambah ke 3 skema warna (`Theme.kt`): `AppleAccent` untuk
  kedua skema Apple, `MatteAccent` (tembaga) untuk Matte — sekarang setiap elevasi tonal
  M3 di seluruh app pakai warna aksen tema asli, bukan ungu baseline tersembunyi.
- **`frostedGlass()` (`BlurUtils.kt`)**, dipakai 6 file (`EqualizerSheet`, `FolderManagerSheet`,
  `LyricsSheet`, `MiniPlayerBar`, `NowPlayingScreen`, `QueueSheet`) tanpa perlu disentuh
  satu-satu: shape hardcode `RoundedCornerShape(24.dp)` → `MaterialTheme.shapes.large`
  (ikut token tema aktif — 28dp Apple vs 8dp Matte), plus border edge jadi trim tembaga
  `colorScheme.primary` alpha 0.35 khusus Matte (dulu abu-abu netral di semua tema) — kesan
  bezel logam dikerjakan-tangan di sekeliling tiap sheet/mini-player.
- **`matteDepthBrush()` baru (`Theme.kt`)**: radial gradient tembaga→matte-surface→matte-
  background, dipasang sekali di root `Surface` (`MainActivity.kt`, dibungkus `Box` baru)
  — cahaya ambient lembut di tengah layar meluruh ke gelap di tepi, kayak cahaya jatuh di
  panel logam matte. Hanya aktif saat `AppTheme.MATTE` (Surface jadi `Color.Transparent`
  supaya gradient di baliknya kelihatan); tema lain 100% tidak berubah (Surface tetap warna
  solid seperti semula).
- **`NavigationBar` (`MainActivity.kt`)**: `tonalElevation` 12dp khusus Matte (vs default
  M3) — bareng fix `surfaceTint` di atas, bar bawah kelihatan "terangkat" hangat, bukan
  cuma garis flat.
- **`MiniPlayerBar.kt`**: shape ikut `MaterialTheme.shapes.large` juga (harus disamakan
  dengan fix `frostedGlass` di atas — kalau tidak, shadow/clip luar 24dp hardcode bentrok
  sama fill dalam yang sudah ikut tema, sudut jadi tidak konsisten/aneh), plus shadow
  `ambientColor`/`spotColor` ditinta tembaga (`MatteAccent` alpha 0.5) & elevasi naik ke
  16dp khusus Matte — dulu memakai default shadow hitam datar di semua tema.

5 file kode disentuh (`Theme.kt`, `Color.kt` — tidak ada perubahan isi tambahan di luar yang
sudah ada dari Batch 38, jadi tidak dihitung ulang di sini —, `BlurUtils.kt`,
`MainActivity.kt`, `MiniPlayerBar.kt`) + `CHANGELOG.md`/`PROJECT_STATE.md`. Tidak ada file
baru (0→0 di `FILE_MANIFEST.txt`). Diklaim **Atomic Change**: shape fix di `frostedGlass`
dan `MiniPlayerBar` saling bergantung (harus konsisten bareng), root vignette butuh
`appTheme` yang baru dikoleksi di scope `AppNavHost` juga bareng NavigationBar — dipecah
antar-commit akan bikin state pertengahan yang visually broken.

**Batas jaminan — ini yang paling penting dibaca sebelum test lagi**: seluruh pass ini
murni analisis statis (brace/paren balance + grep cross-reference tiap pemanggil yang
disentuh), **tidak ada compiler Android atau device/emulator di environment kerja** untuk
benar-benar merender gradient/shadow/shape ini. Kombinasi warna & radius dipilih dari
prinsip desain (kontras, hierarki tonal) bukan dari melihat hasil render — kalau efeknya
ternyata masih kurang terasa, kurang match warnanya, atau performanya berat (radial
gradient di layar tinggi resolusi/refresh rate tinggi bisa mahal — belum diukur), laporkan
spesifik bagian mana yang masih kureng biar batch berikutnya bisa dikoreksi terarah,
bukan tebak-tebak lagi.

## Batch 38 — Fix dokumentasi drift tema + tambah tema custom Matte Noir
Audit lanjutan menemukan `README.md`/`PROJECT_STATE.md` masih deskripsikan sistem tema lama
("3 tema penuh": Ink & Brass/Midnight Bloom/Paper & Ink) dan klaim status bar "dipaksa kontras
terang, tidak ikut mode terang/gelap sistem" — padahal `ui/theme/Theme.kt` sudah lama migrasi
ke model Apple-style `SYSTEM`/`LIGHT`/`DARK` (komentar kode sendiri: *"replacing the old
five-identity theme system"*), dan `MainActivity.kt:187` sudah `isAppearanceLightStatusBars =
!isDarkTheme` (ikut tema, bukan dipaksa). Persis pola drift yang diperingatkan di Batch 17
("fitur baru wajib langsung masuk README di batch yang sama, jangan ditunda") — kali ini
migrasinya sendiri yang lolos tak terdokumentasi.

Atas permintaan eksplisit, dikerjakan sekaligus dalam 1 tema kohesif (theme-system expansion):
- **Apple (SYSTEM/LIGHT/DARK) dikukuhkan sebagai tema utama/default** — tidak ada perubahan
  perilaku, `AppTheme.SYSTEM` tetap default di `AudioPlayerTheme()` & `ThemeStore`, cuma
  ditegaskan lewat dokumentasi & komentar kode.
- **Tema ke-4: `AppTheme.MATTE` ("Matte Noir")** — identitas custom yang sengaja dibuat
  berkebalikan dari Apple, tapi tetap terasa "matte native ultra premium":
  - Warna (`Color.kt`): `MatteBackground` #14120F (matte hangat, bukan hitam OLED murni),
    `MatteAccent` #C9793C (tembaga, bukan biru `AppleAccent`), `MatteSuccess` #6B8F5A (hijau
    matte teredam untuk role `tertiary`)
  - Bentuk (`Theme.kt`, `MatteShapes`): sudut 4/6/8dp nyaris kotak — kebalikan `AppleShapes`
    yang 14/20/28dp membulat generous
  - Tipografi (`Type.kt`, `MatteTypography`): judul pakai `FontFamily.Serif` (font sistem
    bawaan, tidak nambah aset font baru) untuk kesan editorial boutique-hi-fi, body tetap
    sans-serif `Default` demi keterbacaan list panjang
  - Statis selalu gelap (`resolveIsDark(MATTE) = true`) — tidak ikut mode sistem, sesuai
    perilaku 3 tema identity lama sebelum migrasi
- **`colorsFor()` refactor**: signature berubah dari `(isDark: Boolean)` jadi `(theme:
  AppTheme, isDark: Boolean)` supaya bisa cabang ke `MatteColors` tanpa collision dengan
  boolean dark/light Apple biasa. 1 pemanggil luar (`SettingsScreen.kt:288`, preview swatch
  di kartu pemilih tema) disesuaikan; `resolveIsDark()` sendiri tidak berubah signature
  (masih dipakai apa adanya oleh `MainActivity.kt:183` untuk ikon status/nav bar).
- **Fix warna hardcode** (temuan sebelah, sekalian dikerjakan): `SignatureMatcherSheet.kt:96`
  `Color(0xFF3FA34D)` (hijau MATCH signature APK) → `MaterialTheme.colorScheme.tertiary`,
  jadi otomatis theme-aware di 4 tema (dulu MISMATCH/ERROR sudah pakai `colorScheme.error`,
  MATCH-nya saja yang ketinggalan). Ini alasan `tertiary`/`onTertiary` ditambah ke
  `AppleDarkColors`/`AppleLightColors` (pakai `AppleDarkSuccess`/`AppleLightSuccess`, hijau
  sistem iOS) — dulu tidak diisi eksplisit (default M3 generik). Import `Color` yang jadi
  tak terpakai di file itu ikut dibersihkan.

9 file disentuh: `Theme.kt`, `Color.kt`, `Type.kt`, `SettingsScreen.kt`,
`SignatureMatcherSheet.kt`, `README.md`, `PROJECT_STATE.md`, `CHANGELOG.md`,
`FILE_MANIFEST.txt` (tak berubah isi, cuma diverifikasi tetap 107 — tidak ada file baru,
semua penambahan masuk file existing). Diklaim sebagai **Atomic Change** (1 tema kohesif,
lintas file saling bergantung — enum baru butuh scheme+shape+typography+1 pemanggil
disesuaikan bareng, tidak bisa dipecah tanpa bikin build merah di antara commit).
**Belum diverifikasi build/runtime asli** (tidak ada compiler Android di environment kerja) —
dicek statis: brace/paren balance tiap file, `grep` seluruh pemanggil `colorsFor`/
`resolveIsDark` untuk pastikan tidak ada yang lolos dari refactor signature.

Ditemukan tapi belum dikerjakan: swatch preview `ThemeOptionCard` di `SettingsScreen.kt`
belum menunjukkan bentuk sudut/tipografi khas Matte Noir (cuma warna) — kalau mau preview
lebih representatif, perlu sentuh render swatch-nya juga (di luar scope batch ini).

## Batch 37 — Polish UI/UX: truncation tanpa ellipsis (lanjutan temuan Batch 31)
Audit lanjutan menyisir seluruh `maxLines = 1` di `ui/*.kt` (21 titik) untuk cari yang belum
punya `overflow = TextOverflow.Ellipsis` maupun `basicMarquee()` — pola yang sudah konsisten
dipakai di 17 titik lain. 4 gap ditemukan di 2 file, 1 tema kohesif (truncation-consistency
pass, presedennya sama kayak Batch 31/6):
- **`LibraryScreen.kt` — nama album di kartu grid Album** (baris ~499): temuan Batch 31 yang
  dulu sengaja ditunda ("dampak rendah, disimpan untuk batch lanjutan kalau diminta") — kini
  dikerjakan. Nama album panjang sebelumnya kepotong mendadak tanpa indikator visual.
- **`LibraryScreen.kt` — judul lagu di daftar lagu dalam Album** (baris ~522, tampilan detail
  album setelah tap salah satu album): `Row` dengan `Modifier.weight(1f)`, judul panjang
  kepotong tanpa "..." — beda dari daftar lagu lain di Library (baris ~988) yang sudah pakai
  `basicMarquee()`, dan `PlaylistScreen`/`QueueSheet` yang sudah pakai `TextOverflow.Ellipsis`.
  Dipilih `Ellipsis` (bukan marquee) karena konteksnya list padat multi-baris, bukan satu
  fokus item seperti mini-player/now-playing.
- **`FolderManagerSheet.kt` — judul lagu & nama artis di daftar "Lagu Disembunyikan"** (baris
  ~179, ~182): satu-satunya daftar lagu di seluruh app yang masih nol `Ellipsis` sama sekali
  di kedua baris teksnya — folder/artis lain di file yang sama (baris ~151) sudah benar.
Semua 4 titik memakai import `TextOverflow` yang sudah ada di kedua file (tidak perlu import
baru). **Batas jaminan: analisis statis saja (brace/paren balance dicek manual & via script,
tidak ada kotlinc di environment ini) — belum diverifikasi runtime.**

Ditemukan tapi belum dikerjakan (di luar scope batch ini, sama seperti Batch 31): token
spacing masih ad-hoc (356 literal `.dp` tersebar di `ui/*.kt`, tidak ada `Spacing.kt`
terpusat) — scope refactor ini jauh melebihi batas 1 tema/10 file per batch, disimpan untuk
batch besar terpisah kalau diminta eksplisit. Audit icon-only `IconButton` untuk
`contentDescription` kosong (potensi gap aksesibilitas) — nihil, seluruh `IconButton` di
proyek ini sudah punya label yang benar.

## Batch 36 — Polish UI/UX: 4 detail kecil yang bikin app lebih nyaman dipakai
Arahan baru: sambil nunggu build hijau, fokus debugging+optimalisasi DAN polish UI/UX +
detail kecil kenyamanan pemakaian. Audit statis di layar Settings/Library/mini-player
menemukan 4 hal, semuanya dikerjakan atas persetujuan user (multi-select — user pilih
semua):
- **PIN dialog tidak konsisten & kurang privat**: `LockScreen` (unlock app) sudah lama pakai
  PIN pad custom dengan dot mask + haptic + shake-on-error. Tapi `SetPinDialog` di Settings
  ("Atur PIN"/"Ubah PIN") masih `OutlinedTextField` polos — PIN kelihatan jelas di layar
  sambil diketik, dan keyboard yang muncul QWERTY penuh bukan numerik. Fix:
  `KeyboardType.NumberPassword` (numeric pad + masking titik bawaan platform sekaligus, tanpa
  perlu `VisualTransformation` manual terpisah) + `PasswordVisualTransformation()` eksplisit
  di kedua field (PIN & konfirmasi PIN).
- **Search Library tidak ada aksi keyboard**: `LibrarySearchField` tidak set `ImeAction` sama
  sekali. Hasil pencarian sudah live/reaktif per keystroke, tapi tombol "Selesai/Cari" di
  keyboard tidak melakukan apa-apa — satu-satunya cara nutup keyboard cuma tombol back atau
  tap di luar field. Fix: `ImeAction.Search` + `KeyboardActions(onSearch = { ...hide() })`
  lewat `LocalSoftwareKeyboardController`. Murni soal nutup keyboard, logika pencarian itu
  sendiri tidak disentuh.
- **Mini player tanpa indikator progres**: `uiState.position`/`duration` sudah di-tick tiap
  detik (dipakai NowPlayingScreen) tapi mini bar tidak menampilkannya — user harus buka full
  player cuma buat lihat sudah sampai mana. Fix: garis `LinearProgressIndicator` tipis (2dp) di
  tepi bawah mini bar, glanceable-only (bukan `Slider`, tidak seekable) supaya tidak bikin
  target sentuh baru yang konflik dengan `onExpand` di `Box` pembungkus. Pakai overload
  `progress: () -> Float` (lambda) karena overload `Float` langsung sudah deprecated sejak
  Material3 1.2.0 — proyek ini pin compose-bom 2024.05.00 (Material3 ~1.2.1) yang sudah
  menyediakan overload lambda ini.
- **Tampilan versi berantakan & kepanjangan**: teks "Tentang Aplikasi" sebelumnya
  `"AudioPlayer versi 1.0.254 (build 254)"` — angka commit count yang sama (basis
  `versionName`/`versionCode` dari Batch 30, lihat `app/build.gradle.kts`) muncul dua kali
  dalam format berbeda, jadi keliatan berantakan dan lebih panjang dari perlu. Fix: cuma buang
  suffix `"(build N)"` yang redundan — jadi `"AudioPlayer versi 1.0.254"`. **Skema penomoran
  versi (auto dari git commit count) itu sendiri TIDAK diubah** — `app/build.gradle.kts`
  (protected asset) tidak disentuh, ini murni perubahan string tampilan di `SettingsScreen.kt`.

Belum diverifikasi build/runtime asli (tidak ada compiler Android di environment kerja) —
hanya analisis statis + audit manual tiap API (mis. cek versi Material3 sebelum pakai overload
`LinearProgressIndicator` lambda, supaya tidak kejadian lagi kayak kasus `localeFilters` di
Batch 29).

## Batch 35 — Lanjutan debugging + performa: widget jank yang kelewat di Batch 34
Audit statis lanjutan (arahan Batch 34 masih berlaku: tanpa fitur baru), menyisir file
berisiko tinggi (`PlaybackService`, `PlayerViewModel`, `MusicRepository`,
`AccentColorExtractor`, `EqualizerController`, `AppLogger`, `CustomFolderScanner`,
`ShakeDetector`) dan seluruh `LazyColumn`/`LazyRow` di UI — semua bersih. 1 masalah
ditemukan dan diperbaiki:
- **Widget jank, sisi yang kelewat**: Batch 34 memindah `pushWidgetUpdate()` di
  `PlaybackService` ke `Dispatchers.IO`, tapi `WidgetUpdater.updateAll()` (decode+crop+round
  bitmap album-art yang sama) masih dipanggil langsung di dua tempat lain:
  `PlayerWidgetProvider.onUpdate()` dan `.onAppWidgetOptionsChanged()`. `AppWidgetProvider`
  adalah `BroadcastReceiver` biasa — `onReceive`-nya (dan turunannya) jalan di main thread
  secara default, beda dari `PlaybackService` yang sudah punya `serviceScope` sendiri.
  `onAppWidgetOptionsChanged` lebih berisiko lagi: komentar lama di kode sendiri bilang ini
  "fires live as user drags resize handles" — decode blocking bisa numpuk tiap event drag,
  bukan cuma sekali per update. Fix: `goAsync()` (API standar `BroadcastReceiver` untuk kerja
  lanjut setelah callback return tanpa nge-block pemanggil) dibungkus
  `providerScope.launch(Dispatchers.IO)`, dengan `pendingResult.finish()` di `finally` supaya
  wakelock broadcast tetap dilepas walau `updateAll` throw.

Belum diverifikasi build/runtime asli (tidak ada compiler di environment kerja) — hanya
analisis statis. `versionName` tetap otomatis dari commit count.

## Batch 34 — Fokus baru: debugging + performa (bukan fitur baru)
Arahan proyek berubah mulai batch ini: berhenti nambah fitur, fokus mematangkan yang sudah
ada lewat debugging & optimalisasi. Audit kode nyata menemukan 2 masalah, dikerjakan
sekaligus:
- **Widget jank**: `PlaybackService.pushWidgetUpdate()` decode+crop+round bitmap album-art
  di main thread, dipanggil tiap ganti lagu & tiap tap play/pause (`onMediaItemTransition`,
  `onIsPlayingChanged`). Dipindah ke `serviceScope.launch(Dispatchers.IO)`, plus
  `widgetUpdateJob?.cancel()` sebelum relaunch biar skip/toggle cepat nggak bikin update
  lama nimpa balik update baru. `saveState` (SharedPreferences, sudah async-safe) tetap di
  thread asal.
- **Crash logger drift dari spec**: `AppLogger.writePublicCrashLog` belum pernah punya UUID
  di nama file (risiko overwrite pas crash-loop), belum ada FIFO retention (folder
  `Documents/AudioPlayer/logs` numpuk tanpa batas), metadata belum ada Version/OS/Model.
  Ditambah `UUID.randomUUID()` di filename, blok metadata versi (`PackageInfoCompat`) + OS +
  model perangkat, dan `enforceCrashLogRetention()` yang jaga folder tetap ≤50 file
  (query MediaStore `RELATIVE_PATH`, urut `DATE_ADDED DESC`, hapus sisa di luar 50 terbaru).
  Log privat (`diagnostic_log.txt`) tidak berubah.

Belum diverifikasi build/runtime asli (tidak ada compiler di environment kerja) — hanya
analisis statis. `versionName` tetap otomatis dari commit count.

## Batch 33 — Hotfix build gagal dari Batch 32 (koreksi diagnosis Batch 32 sendiri)
`log_fail_94.zip` (build #94) dianalisis: error identik dengan sebelum Batch 32 —
`Unresolved reference: matchParentSize` di `Utils.kt` baris 16 (import) & 61 (call).
**Diagnosis Batch 32 ternyata salah.** Root cause sebenarnya: `matchParentSize()` bukan
extension biasa milik `BoxScope`, tapi member extension function milik `Modifier` yang
dideklarasikan *di dalam* interface `BoxScope` (`fun Modifier.matchParentSize(): Modifier`
di dalam `interface BoxScope`, lihat source resmi `androidx.compose.foundation.layout.Box.kt`).
Artinya: (1) tidak bisa diimpor sebagai fungsi top-level — import Batch 31 sudah salah
sejak awal; (2) pemanggilannya tetap wajib pakai prefix `Modifier.` walau di dalam lambda
`Box { }`, karena hanya dispatch receiver (`BoxScope`) yang implicit, extension receiver
(`Modifier`) tetap harus eksplisit. Fix: hapus baris import yang salah, kembalikan
pemanggilan ke `Modifier.matchParentSize()`. Tidak ada usage lain fungsi ini di project
(dicek `grep`). **Pelajaran**: pesan error compiler Batch 32 ("receiver type mismatch...
`Modifier.matchParentSize()` defined in `BoxScope`") sudah menyebutkan signature yang
benar secara eksplisit di teksnya sendiri — salah dibaca sebagai instruksi membuang
prefix `Modifier.`, padahal maksudnya kebalikan itu. `versionName` tetap otomatis dari
commit count (tidak disentuh batch ini).

## Batch 32 — Hotfix build gagal dari Batch 31 (⚠️ diagnosis salah — lihat Batch 33)
`log_fail_93.zip` (build #93) dianalisis: `Utils.kt` gagal kompilasi di `AlbumArt` —
`Unresolved reference: matchParentSize`. Root cause **yang disimpulkan saat itu** (keliru):
`matchParentSize()` dianggap extension function biasa milik `BoxScope`, bukan `Modifier` —
kode Batch 31 dianggap salah menulis `Modifier.matchParentSize()`. Fix yang diterapkan:
hapus prefix `Modifier.`. **Fix ini yang justru menyebabkan build #94 gagal lagi dengan
error identik** — root cause sebenarnya dan fix yang benar ada di entri Batch 33 di atas.
`versionName` tetap otomatis dari commit count (tidak disentuh batch ini).

## Batch 31 — Polish UI/UX pass pertama
Permintaan: "Fokus Polish UI dan UX sampai masuk tahap akhir". Dikerjakan lewat audit statis
dulu (12 screen/sheet) → user pilih 5 dari daftar temuan untuk batch ini.

- **Album-art fallback**: helper `AlbumArt` baru (`Utils.kt`, Coil `SubcomposeAsyncImage`)
  menggantikan `AsyncImage` mentah di 6 titik (Home x2, LibraryScreen x2, MiniPlayerBar,
  NowPlayingScreen x2) — lagu tanpa cover art sebelumnya cuma nampilin ruang kosong, sekarang
  ikon nada musik di atas `surfaceVariant`. Backdrop blur NowPlaying pakai `showIcon = false`
  (ikon akan jadi gumpalan gak jelas kalau kena blur 60dp)
- **Empty state disatukan**: 3 pola beda (komponen `EmptyState` di Library/Playlist, custom
  inline di Home, `Text` abu polos di Queue/FolderManager) → semua pakai `EmptyState`.
  Ditambah parameter `modifier` opsional (default sama seperti sebelumnya, 5 pemanggilan lama
  tidak berubah) supaya aman dipakai di `LazyColumn` item dan bottom sheet
- **Ellipsis nama artis**: 4 lokasi (Home x2, MiniPlayerBar, LibraryScreen) punya
  `maxLines=1` tanpa `TextOverflow.Ellipsis` — nama panjang kepotong mendadak. Judul lagu di
  lokasi sama sengaja tidak disentuh (pakai `basicMarquee()`, bukan potongan)
  Semua status batch di atas belum diverifikasi runtime/emulator.
- **Touch target**: tombol tutup `FeatureHintBanner` 28dp → 40dp (di bawah standar
  aksesibilitas 44dp+)
- **Animasi list**: `animateItemPlacement()` ditambah ke folder list `FolderManagerSheet`,
  konsisten dengan Library/Playlist/Queue
- Beres-beres: import Coil `AsyncImage` dan `Icons.Default.LibraryMusic`/`TextAlign` yang jadi
  tidak terpakai dihapus dari 4 file

**Ditemukan tapi tidak dikerjakan** (di luar 5 pilihan user, disimpan untuk batch lanjutan):
judul album di grid Library juga `maxLines=1` tanpa ellipsis; token spacing (dp) tidak
seragam antar file, belum ada `Spacing.kt` terpusat.

## Batch 30 — Otomatisasi & minimalisasi versionName
Permintaan: "otomatisasi dan minimalisasi yang terkait" bump versi. Ditemukan saat cek
`build.yml`: `versionName` app (`3.9`, manual) dan tag GitHub Release (`v1.0.<commit-count>`,
otomatis) adalah **dua angka yang tidak nyambung** — sumber kebingungan/friksi yang dimaksud.

- `versionName` sekarang ikut turunan dari `gitCommitCount()` yang sama dipakai
  `versionCode` — pola `1.0.<jumlah commit>`. Tidak ada lagi string manual yang perlu diingat
  atau dibump
- **Efek samping otomatis, tanpa nyentuh `.github/workflows/build.yml` sama sekali**: karena
  CI juga menghitung `git rev-list --count HEAD` dari commit yang sama untuk tag rilis, nomor
  di app (`Settings → AudioPlayer versi 1.0.254`) dan nomor di nama file APK/tag GitHub
  Release (`AudioPlayer-v1.0.254-release.apk`) sekarang **selalu match** — dua perhitungan
  independen, git history yang sama, hasil dijamin sama
- **Catatan jujur (trade-off, bukan dianggap masalah karena ini yang diminta)**: tampilan di
  `SettingsScreen` (`"AudioPlayer versi ${VERSION_NAME} (build ${VERSION_CODE})"`) sekarang
  menampilkan dua angka yang mirip (`1.0.254` dan `254`) — tidak lagi ada nomor rilis
  "kurasi" bergaya `3.9`/`4.0`. Kalau nanti ingin nomor yang lebih rapi user-facing lagi,
  ini bisa direvisi lagi terpisah
- Dokumentasi disinkronkan: README.md § "Standar Penomoran Versi" + PROJECT_STATE.md §
  "Konvensi penamaan ZIP & versi" ditulis ulang, keduanya masih menyebut skema manual lama
  sebelum batch ini
- **Efek ke pelaporan batch selanjutnya**: baris "`versionName` tetap X" yang biasa ditulis
  di tiap entri Batch/PROJECT_STATE **tidak relevan lagi mulai batch ini** — `versionName`
  sekarang otomatis naik tiap commit sama seperti `versionCode`, bukan keputusan per-batch
- **Batas jaminan**: analisis statis saja. Perubahan murni satu baris ekspresi Kotlin
  (`val appVersionName = "1.0.$appVersionCode"`), risiko kegagalan build sangat rendah
  dibanding Batch 28 (tidak ada DSL/API baru yang perlu diverifikasi terhadap versi AGP)

## Batch 29 — Hotfix build gagal Batch 28
`log_fail_91.zip` (test-output.log, build #91) dianalisis: `androidResources { localeFilters
+= listOf("en") }` dari Batch 28 gagal kompilasi — `e: Unresolved reference: localeFilters`
di `app/build.gradle.kts:106`. Root cause: `localeFilters` di blok `androidResources` memang
DSL AGP yang benar secara konsep, tapi baru tersedia di rilis AGP setelah 8.4.1 (versi yang
dipakai project ini) — bukan sejak AGP 8.0 seperti yang diasumsikan Batch 28.

- **Fix**: ganti ke `resourceConfigurations += listOf("en")` di `defaultConfig` — DSL lama
  tapi terverifikasi didukung penuh di AGP 8.4.1 (baru deprecated mulai AGP 8.8, project ini
  belum di sana). Hasil akhir identik dengan niat Batch 28: buang resource terjemahan
  AndroidX/Compose/Media3/Coil untuk locale selain "en"
- Perubahan Guava → `androidx.concurrent:concurrent-futures` dari Batch 28 **tidak
  disentuh** — bukan sumber kegagalan build ini (error murni di baris `localeFilters`,
  gagal sebelum sempat compile Kotlin sama sekali)
- **Pelajaran**: klaim "tersedia sejak AGP X" dari sumber pihak ketiga (blog, artikel) tidak
  cukup tanpa verifikasi ke changelog resmi AGP per versi — Batch 28 percaya klaim itu tanpa
  cross-check ke versi AGP project (`8.4.1`) secara spesifik. Ke depan: DSL Gradle baru wajib
  dicek terhadap nomor versi AGP project ini persis, bukan generalisasi "AGP 8.0+"
- **Batas jaminan**: analisis statis saja (tidak ada kotlinc di environment ini) —
  `resourceConfigurations` adalah DSL yang sudah lama stabil (bertahun-tahun, pra-AGP 8.0),
  jadi risiko kegagalan yang sama jauh lebih rendah dari Batch 28, tapi verifikasi sungguhan
  tetap baru terjadi di push ini. `versionName` tetap `3.9`.

## Batch 28 — Optimasi ukuran APK
Audit eksternal terbaru (skor 9.3/10) menandai ukuran APK sebagai prioritas #1 (beda urutan
dari self-review internal Batch 27 yang taruh ini di posisi terakhir — dua sumber, dua
urutan; dipilih ukuran APK karena satu-satunya dari daftar itu yang hasilnya bisa dicek
objektif dari ukuran file APK output CI, tanpa butuh runner/emulator untuk verifikasi).

- **Locale filtering** (`app/build.gradle.kts`): app ini cuma punya satu set resource
  `values/` sendiri (tanpa `values-xx`), tapi AndroidX/Compose/Media3/Coil masing-masing
  bawa puluhan string hasil terjemahan (kebanyakan label aksesibilitas) untuk locale yang
  tidak pernah dipakai app ini. `androidResources { localeFilters += listOf("en") }`
  membatasi resource dari library ke locale itu saja — tidak menyentuh string app sendiri
  (default/tanpa-qualifier selalu ikut apa pun locale filter-nya)
- **Guava penuh → `androidx.concurrent:concurrent-futures`**: `com.google.guava:guava`
  cuma pernah dipakai untuk 3 hal — `ListenableFuture`/`SettableFuture` di
  `PlaybackService.onPlaybackResumption` dan `MoreExecutors.directExecutor()` di
  `PlayerViewModel` (keduanya cuma demi memenuhi API session callback Media3 yang memang
  mengembalikan `ListenableFuture`). Diganti: `SettableFuture` → `CallbackToFutureAdapter`
  (alur set/setException async lewat coroutine sama persis, cuma lewat `Completer`, bukan
  future yang di-`set()` langsung), `MoreExecutors.directExecutor()` → `Executor { it.run() }`
  polos (tidak ada perilaku khusus di baliknya selain itu). `ListenableFuture` sebagai tipe
  tetap ada — datang dari shim kecil `com.google.guava:listenablefuture:1.0` yang jadi
  dependency transitif `concurrent-futures`, bukan lagi jar Guava penuh
- **Batas jaminan**: seperti biasa analisis statis saja (tidak ada kotlinc di environment
  ini) — verifikasi ukuran APK sebelum/sesudah baru bisa dilihat dari artifact GitHub
  Release setelah push ini. Prioritas #2-5 dari audit baru (error handling, testing,
  technical debt, maintainability) sengaja belum disentuh — batch berikutnya. Tidak ada
  perubahan behavior/fitur user-facing. `versionName` tetap `3.9`.

## Batch 27 — Fondasi testing otomatis
Self-review internal proyek (skor 8.8/10) menandai prioritas berikutnya: bukan fitur baru,
tapi stabilitas/testing/pengurangan technical debt. Batch ini mengerjakan prioritas
pertama dari daftar itu: testing otomatis, dimulai dari dua gap paling mendesak — CI yang
tidak pernah menjalankan test yang sudah ada, dan business logic kritis yang belum bisa
di-unit-test sama sekali karena masih menyatu dengan kelas ber-Context/Android framework.

- **Gap 1 — CI tidak pernah menjalankan unit test**: 4 file test JVM murni sudah ada di
  repo sejak beberapa batch lalu (`LyricsParserTest`, `PinLockoutPolicyTest`,
  `LibrarySearchIndexTest`, `UtilsTest`), tapi `.github/workflows/build.yml` langsung
  `assembleRelease` tanpa pernah menjalankannya — kalau ada test gagal, build tetap lolos
  dan APK tetap ter-release
  - **Fix**: step baru `gradle testDebugUnitTest --no-daemon`, ditaruh sebelum decode
    keystore supaya gagal cepat tanpa buang waktu signing kalau logikanya sendiri salah
- **Gap 2 — `ShakeDetector` (logika shake-to-skip dari fix Batch 25) belum pernah
  terverifikasi langsung, cuma dianalisis dari baca kode**: state machine pulse-counting-nya
  menyatu dengan `SensorEventListener`/`SensorManager`, tidak bisa di-unit-test tanpa
  Robolectric (proyek ini sengaja pure-JVM test only)
  - **Fix**: pulse-counting diekstrak murni ke `ShakePulseTracker` baru (menerima timestamp
    `Long`, kembalikan `Boolean` — nol dependency Android), `ShakeDetector` tinggal
    delegasikan ke situ. Perilaku eksternal identik, bukan perubahan behavior. 8 test baru
    memverifikasi persis skenario yang jadi alasan fix Batch 25 (pocket-jostling: spike
    tunggal berjarak jauh tidak pernah terakumulasi jadi shake)
- **Gap 3 & 4 — dua business logic lain yang juga tidak bisa di-unit-test karena menyatu
  dengan Context**: folder-name derivation di `MusicRepository` (parsing path MediaStore,
  ada beberapa edge case — path kosong, trailing slash, file di root) dan filter
  gabungan folder-dikecualikan + lagu-disembunyikan di `LibraryFilterStore`
  - **Fix**: masing-masing diekstrak jadi pure function di companion object
    (`MusicRepository.deriveFolderName`, `LibraryFilterStore.shouldKeep`) — method instance
    lama tinggal delegasikan, tidak ada perubahan perilaku. `LibraryFilterStore.shouldKeep`
    sengaja menerima `folderPath`/`id` polos, bukan `Song` utuh — `Song.uri` bertipe
    `android.net.Uri` yang tidak bisa dikonstruksi aman di pure-JVM test tanpa Robolectric

**Batas jaminan: seperti biasa, analisis statis kode saja — tidak ada kotlinc di environment
ini, jadi 8+9+4 test baru (21 total) belum pernah benar-benar dijalankan. Verifikasi
sungguhan baru terjadi begitu CI (Gap 1) jalan pertama kali di push berikutnya.**
`versionName` tetap `3.9` (batch infrastruktur, bukan titik rilis user-facing). 4 file
Kotlin utama disentuh + 3 file test baru + 1 file workflow CI.

**Revisi pasca-push** (detail lengkap di `PROJECT_STATE.md` § Riwayat insiden): (1) ZIP
sempat salah paket (folder terbungkus + exclude flag nyaplok `.gitignore`/`.github/`) —
diperbaiki. (2) CI test run pertama nemu 9 test gagal dari 53 — 1 bug asli di
`ShakePulseTracker` (`lastShakeTime` default `0L`, sekarang `Long?`) + 1 bug lama yang baru
ketahuan di `LibrarySearchIndexTest` (`Uri.parse` null di unit test JVM murni, sekarang pakai
`mockito-core`). Sekalian ditambah CI artifact `log_fail_<run_number>` saat ada step gagal.

## Batch 26 — Audit konsistensi feedback interaksi (segmen "feedback")
Scope: apa yang terjadi/diharapkan saat user berinteraksi dengan app — **beda cakupan** dari
audit haptic Batch 25 (favorit, long-press-select Library, rating bintang — sudah selesai
duluan, tidak disentuh ulang di sini).

- **Gap 1 — `LockScreen.kt` nol haptic sama sekali**: layar dengan interaksi paling sering
  (tiap buka app) justru satu-satunya yang tidak punya tactile feedback apa pun, termasuk
  saat PIN salah (cuma teks merah, diam total)
  - **Fix**: haptic `TextHandleMove` tiap tombol angka & backspace; haptic `LongPress` +
    animasi shake 300ms (`Animatable` + `keyframes`, offset ±10dp/±8dp/±4dp) khusus saat PIN
    salah atau kena lockout
- **Gap 2 — semua slider di app nol haptic saat rilis jari**: seek bar & slider volume di
  `NowPlayingScreen.kt`, slider band equalizer + preset chip di `EqualizerSheet.kt`
  - **Fix**: `onValueChangeFinished` (slider) / `onClick` (chip) → haptic `TextHandleMove`.
    Slider volume sebelumnya malah tidak punya `onValueChangeFinished` sama sekali (cuma
    `onValueChange` kontinu) — ditambah tanpa ubah perilaku live-drag-nya
- **Gap 3 — hapus folder tambahan (`FolderManagerSheet.kt`) langsung hilang, nol feedback,
  nol konfirmasi**: dicek dulu ke kode `PlayerViewModel.removeCustomFolder()` sebelum
  nyontek pola Undo Snackbar yang sudah ada (dipakai di queue/playlist) —
  `releasePersistableUriPermission()` yang dipanggilnya **tidak bisa** di-undo asli, akses
  URI itu hilang permanen kecuali user pilih ulang lewat SAF picker. Undo Snackbar di sini
  jadi tombol "Urungkan" yang bohong
  - **Fix**: `AlertDialog` konfirmasi ("Hapus folder tambahan?" + penjelasan perlu pilih
    ulang) sebelum manggil `onRemoveCustomFolder`, bukan undo palsu
- **Gap 4 — 6 titik masih pakai `Toast.makeText` mentah** (`LibraryScreen.kt` x4: playNext,
  addToQueue, 2x konfirmasi tambah-ke-playlist tunggal + bulk; `DiagnosticLogSheet.kt` x1:
  salin log; `SignatureMatcherSheet.kt` x1: salin laporan) — Toast ikut style OS (bukan tema
  gelap/terang "Ink & Brass"), posisinya juga beda dari `SnackbarHost` yang sudah dipakai
  untuk undo queue/playlist
  - **Fix**: kanal baru `PlayerViewModel.infoMessage` (`MutableStateFlow<String?>`,
    `showInfoMessage()`/`consumeInfoMessage()` — pola one-shot sama persis dengan
    `celebrationMessage`/`actionErrorMessage`/`undoableAction` yang sudah ada), dirender
    lewat `snackbarHostState.showSnackbar(..., duration = Short)` di `MainActivity.kt`.
    Param baru `onInfoMessage: (String) -> Unit` di-thread: `MainActivity` →
    `LibraryScreen`/`SettingsScreen` → `DiagnosticLogSheet`/`SignatureMatcherSheet`
  - Sekalian: tombol "Hapus" (clear log) di `DiagnosticLogSheet.kt` sebelumnya nol feedback
    juga (padahal aksi destruktif, walau data lokal berisiko rendah) — ditambah haptic
    `LongPress` + konfirmasi via `infoMessage`
- 10 file Kotlin disentuh dalam satu tema kohesif (feedback-consistency, presedennya sama
  kayak Batch 6 — smoothness pass lintas banyak file): `PlayerViewModel.kt`,
  `MainActivity.kt`, `LockScreen.kt`, `NowPlayingScreen.kt`, `EqualizerSheet.kt`,
  `FolderManagerSheet.kt`, `LibraryScreen.kt`, `DiagnosticLogSheet.kt`,
  `SignatureMatcherSheet.kt`, `SettingsScreen.kt`. Tidak ada file baru/dihapus
- `import android.widget.Toast` dihapus dari 3 file yang sudah tidak butuh lagi (sudah
  dicek tidak ada sisa pemakaian lain); `LocalContext` juga dihapus dari
  `DiagnosticLogSheet.kt` (tidak dipakai lagi setelah Toast hilang) dan dari
  `SignatureLogDialog` privat di `SignatureMatcherSheet.kt` (composable luar tetap pakai
  `LocalContext` untuk `ApkSignatureChecker.inspect`, tidak disentuh)
- **Batas jaminan**: environment ini tidak punya `kotlinc`/compiler — verifikasi terbatas ke
  analisis statis (brace/paren balance dicek manual per file, semua seimbang) + baca-ulang
  tiap referensi silang (import, parameter, pemanggil). **Belum diverifikasi build/runtime
  sungguhan** — cek hasil GitHub Actions setelah push
- `versionName` naik `3.8` → `3.9` (`app/build.gradle.kts`, edit parsial 1 baris — Protected
  File)

## Batch 25 — 2 bug user-reported: navigasi numpuk & skip sendiri di background
- **Bug 1 — MiniPlayerBar numpuk kalau di-tap cepat berkali-kali**: `onExpand` MiniPlayerBar
  di `MainActivity.kt` manggil `navController.navigate("now_playing")` tanpa
  `launchSingleTop`, beda dari 3 item `NavigationBar` lain yang sudah benar. Tiap tap nambah
  instance baru di backstack, jadi butuh back berkali-kali buat keluar
  - **Fix**: tambah `{ launchSingleTop = true }` di navigate call itu — konsisten dengan pola
    yang sudah dipakai di 3 tempat lain di file yang sama
- **Bug 2 — Lagu skip sendiri tanpa tap, utamanya saat app di-swipe dari Recents**:
  - Ditelusuri lewat baca kode (bukan verifikasi runtime — tidak ada runner): satu-satunya
    pemicu `seekToNextMediaItem()` yang hidup independen dari `PlayerViewModel` (yang
    `onCleared()`-nya jalan begitu Activity di-finish saat app di-swipe dari Recents) adalah
    `ShakeDetector` di `PlaybackService` — start/stop-nya cuma terikat ke `isPlaying`, jadi
    tetap aktif selama musik main di background lewat foreground service
  - `ShakeDetector` lama fire dari **satu** spike g-force tunggal di atas threshold — nyaris
    tidak bisa dibedakan dari HP kebanting-banting di kantong/tas saat jalan, khususnya
    persis di skenario "di luar app" yang dilaporkan
  - **Fix**: `ShakeDetector.kt` sekarang mensyaratkan 3 pulse terpisah dalam window 900ms
    (gerakan kocok asli) sebelum fire, bukan 1 spike tunggal — pola klasik deteksi shake yang
    jauh lebih tahan dari goyangan biasa
  - **Belum terverifikasi 100%** — kalau Shake-to-Skip di pengaturan user OFF, root cause di
    atas bukan penyebabnya dan bug butuh ditelusuri ulang (kandidat lain yang sudah dicek dan
    disingkirkan: `onPlayerError`-based auto-skip di `PlayerViewModel` — listener itu justru
    mati bareng ViewModel-nya saat app di-swipe, jadi bukan itu)
- 2 file berubah (`MainActivity.kt`, `ShakeDetector.kt`), tidak ada file baru/dihapus, tidak
  ada perubahan behavior yang terlihat user selain 2 fix di atas
- **Susulan (sama batch) — CI/CD**: `.github/workflows/build.yml` ternyata masih pakai
  `actions/upload-artifact` (CI artifact, expire + otomatis dibungkus `.zip` oleh GitHub saat
  diunduh) — bukan **GitHub Release** seperti yang seharusnya sesuai aturan rilis proyek ini.
  Ini kelewat sejak workflow pertama kali dibuat, ketahuan dari laporan user. Diganti ke
  `softprops/action-gh-release@v2`, nempel APK signed langsung sebagai release asset
  (permanen, publik-diunduh tanpa login, tidak dibungkus `.zip`), plus `permissions:
  contents: write` di job biar `GITHUB_TOKEN` bisa bikin tag+release. README.md § Build dan
  § Standar Penomoran Versi disesuaikan sebutannya (artifact → Release)
- **Susulan (sama batch) — audit konsistensi feedback**: tap-to-toggle-favorit dapat haptic di
  Now Playing tapi tidak di Perpustakaan (`SongRow`) padahal aksi identik, long-press masuk
  mode-pilih di Perpustakaan nol haptic (padahal pola umum di app besar), rating bintang di
  Now Playing juga nol haptic. Ketiganya dibenarkan supaya konsisten dengan pola haptic yang
  sudah ada di file yang sama (`LibraryScreen.kt`, `NowPlayingScreen.kt`) — tidak ada fitur
  baru, cuma nyamain feedback yang sudah semestinya ada

## Batch 24 — Fix definitif crash "terus berhenti" (fix Batch 23 belum cukup)
- **Kondisi**: setelah Batch 23 (bump `lifecycle-runtime-compose` 2.8.1→2.8.2), crash log baru
  dari crash logger (Batch 22) menunjukkan `IllegalStateException: CompositionLocal
  LocalLifecycleOwner not present` **masih terjadi, stack trace identik** dengan sebelumnya
- **Root cause sebenarnya**: sejak lifecycle 2.8.0, `LocalLifecycleOwner` dipecah jadi dua
  CompositionLocal terpisah — `androidx.compose.ui.platform.LocalLifecycleOwner` (lama, dari
  Compose UI, yang SUDAH diisi benar oleh `setContent()`) dan `androidx.lifecycle.compose
  .LocalLifecycleOwner` (baru, dipakai internal `collectAsStateWithLifecycle()`). Keduanya
  TIDAK otomatis saling terhubung di Compose UI 1.6.x (compose-bom project ini,
  `2024.05.00`), berapa pun versi `lifecycle-runtime-compose`-nya — klaim release notes bahwa
  2.8.2 "bekerja dengan versi Compose apa pun" tidak terbukti berlaku di kasus ini
- **Fix**: `MainActivity.kt`, seluruh isi `setContent {}` dibungkus
  `CompositionLocalProvider(androidx.lifecycle.compose.LocalLifecycleOwner provides
  androidx.compose.ui.platform.LocalLifecycleOwner.current) { ... }` — dipasang sekali di titik
  terluar, otomatis berlaku ke seluruh pohon composable di bawahnya termasuk `AppNavHost` dan
  semua layar lain, tidak perlu menyentuh satu pun dari 20+ titik `collectAsStateWithLifecycle`
  individual
- Bump lifecycle ke 2.8.2 dari Batch 23 tetap dipertahankan (tidak ada ruginya), tapi fix ini
  sudah tidak bergantung padanya sama sekali
- 1 file berubah (`MainActivity.kt`), tidak ada file baru/dihapus, tidak ada perubahan behavior
  yang terlihat user

## Batch 23 — Root cause crash "terus berhenti" ditemukan & diperbaiki
- **Crash**: `java.lang.IllegalStateException: CompositionLocal LocalLifecycleOwner not present`
  — dilempar `collectAsStateWithLifecycle()` di baris pertama `setContent {}` MainActivity,
  jadi app crash di SETIAP kali dibuka sejak Batch 20, sebelum satu frame UI pun sempat
  tergambar. Ditemukan lewat file `Documents/AudioPlayer/logs/crash_*.txt` dari fitur crash
  logger Batch 22 — sebelumnya sudah diaudit manual per file (MainActivity, PlayerViewModel,
  LibraryScreen, manifest, theme, proguard) dan nihil, karena memang bukan bug di kode kita
- **Root cause asli**: bug upstream resmi di `androidx.lifecycle:lifecycle-runtime-compose:2.8.1`
  saat dipasangkan dengan Compose UI 1.6.x (versi yang di-resolve `compose-bom:2024.05.00`).
  Dikonfirmasi di release notes resmi Lifecycle: "Fixed CompositionLocal LocalLifecycleOwner
  not present errors when using Lifecycle 2.8.X with Compose 1.6.X or earlier — you can now
  use Lifecycle 2.8.2 with any version of Compose without any workarounds required"
- **Fix**: bump `androidx.lifecycle:lifecycle-runtime-ktx`, `lifecycle-viewmodel-compose`, dan
  `lifecycle-runtime-compose` — ketiganya dari `2.8.1` ke `2.8.2` di `app/build.gradle.kts`.
  Tidak ada perubahan lain; tidak perlu menyentuh satupun dari 20+ titik pemanggilan
  `collectAsStateWithLifecycle()` di `MainActivity.kt`
- 1 file berubah (`app/build.gradle.kts`, 3 baris versi), tidak ada file baru/dihapus

## Batch 22 — Fitur: crash logger ke folder publik (Documents/AudioPlayer/logs)
- **Latar belakang**: saat app crash sebelum sempat dibuka sama sekali, log diagnostik privat
  yang sudah ada (`Settings → Lanjutan → Log Diagnostik`) jadi tidak terjangkau — tidak ada
  cara ambil isinya tanpa root/ADB
- **Implementasi**: `AppLogger` — begitu `Thread.UncaughtExceptionHandler` menangkap crash
  fatal, selain tetap menulis ke log privat seperti biasa, sekarang juga menulis file terpisah
  `crash_<yyyyMMdd_HHmmss>.txt` ke `Documents/AudioPlayer/logs/` lewat MediaStore
  (`RELATIVE_PATH` + `MediaStore.Files`) — file baru per crash (bukan satu file ditimpa terus),
  isinya waktu + nama thread + stack trace lengkap
- **Kenapa MediaStore, bukan `WRITE_EXTERNAL_STORAGE` + path langsung**: menulis ke koleksi
  publik lewat MediaStore tidak butuh permission apa pun di app dengan scoped storage (API
  29+) — jadi tidak perlu nambah permission baru di manifest. Di bawah API 29 (belum ada
  scoped storage), fungsi ini langsung `return` tanpa melakukan apa-apa — daripada menambah
  kerumitan minta izin storage tepat saat crash, error non-fatal tetap tercatat di log privat
  seperti biasa
- Tidak menyentuh log diagnostik privat yang sudah ada — murni tambahan, bukan pengganti
- 1 file berubah (`app/src/main/java/com/rudi/audioplayer/util/AppLogger.kt`), README
  diperbarui untuk menyebutkan lokasi file publik ini

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
