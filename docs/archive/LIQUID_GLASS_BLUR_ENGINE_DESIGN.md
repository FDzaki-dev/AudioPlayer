# Desain Teknis: Blur Asli Liquid Glass (Fase 5 §3b Opsi A)

Ditanam Batch 294. Jawaban user atas "mulai dari mana?": **desain teknis dulu, dokumen, 0
kode**. Dokumen ini fokus SEMPIT ke 1 hal — bagaimana caranya blur backdrop SUNGGUHAN
(nyontoh piksel di belakang permukaan, bukan tint statis) diimplementasikan di project ini.
`ROADMAP_LIQUID_GLASS_REDESIGN.md` tetap jadi peta besar (status semua fase); dokumen ini anak
dari situ, khusus fase 5 sub-langkah "infrastruktur `RenderEffect`/`RenderNode`".

**0 kode diubah batch ini.** Semua di bawah adalah rencana, belum eksekusi.

---

## 1. Keputusan pertama: jangan hand-roll, pakai library yang sudah battle-tested

Riset (web search Batch 294, bukan asumsi) nemu beberapa opsi nyata, bukan cuma 1:

| Opsi | Apa itu | Kenapa dipertimbangkan / tidak |
|---|---|---|
| **Haze** (`dev.chrisbanes.haze`, Chris Banes) | Library Compose/Compose-Multiplatform khusus efek backdrop (blur dkk), v2.0 rilis 2026 — bukan proyek mati. Dibangun di atas `GraphicsLayer` API Compose 1.7+. | **DIREKOMENDASIKAN.** Paling matang, paling banyak dipakai, didokumentasikan, dan progresif per-API-level (lihat §2). Ini juga persis alasan CONVX sendiri TIDAK hand-roll blur-nya — mereka vendor library terpisah (`Kyant0/backdrop`) krn masalah ini genuinely spesialis, bukan sesuatu yang aman ditulis ulang tiap project. |
| `imla` (desugar-64) | Custom rendering pipeline lebih dalam (HardwareBuffer, thread GL terpisah, support API 23+). | Lebih canggih tapi jauh lebih kompleks/lebih baru & kurang teruji drpd Haze. Overkill utk kebutuhan project ini (minSdk sudah 31, tidak butuh dukungan API 23). |
| `Cloudy` (skydoves) | Backport blur pakai pendekatan lain, radius dibatasi 0-25. | Lebih simpel tapi lebih terbatas (radius kecil, dan tetap bukan didesain khusus utk kasus "banyak permukaan glass mengambang di atas 1 konten yang sama" seperti kebutuhan app ini). |
| Hand-roll `RenderEffect`+`GraphicsLayer` sendiri | Tulis capture+blur+composite dari nol pakai API Android native. | **TIDAK direkomendasikan** — bukan 1-baris kode (`Modifier.graphicsLayer { renderEffect = ... }` doang cuma blur ISI composable itu sendiri, sama persis limitasi `frostedGlass()` yang SUDAH ada & sudah didokumentasikan jujur di kode — utk sampling backdrop SUNGGUHAN perlu koordinasi 2 pihak: 1 composable "merekam" dirinya ke `GraphicsLayer` bersama, komposabel lain "membaca" layer itu lalu blur+composite ulang. Ini persis yang sudah dipecahkan Haze — reinvent = effort tinggi + resiko bug rendering (race kondisi capture-timing, dsb) utk masalah yang sudah solved. |

**Rekomendasi final: adopsi Haze sebagai dependency baru**, bukan hand-roll.

---

## 2. Ekspektasi realistis per level API (JANGAN overpromise)

Dari riset resmi Haze sendiri: implementasinya BERBEDA tergantung API level device —

- **API 31 (persis minSdk project sekarang)**: fallback **"scrim"** — SECARA VISUAL SAMA SEKALI
  TIDAK LEBIH BAIK dari `frostedGlass()` yang sudah ada sekarang (tint statis). Device di lantai
  minSdk baru project ini TIDAK akan merasakan peningkatan visual apa pun dari kerja fase 5 ini.
- **API 32**: multiple `GraphicsLayer` — blur asli, tapi lewat jalur yang lebih berat.
- **API 33+**: **Runtime Shader** — implementasi paling efisien, blur asli performa terbaik.

**Implikasi jujur, bukan didramatisir maupun ditutup-tutupi**: investasi fase 5 ini baru betul-
betul "terlihat" bedanya oleh user di device API 32 ke atas. Kalau base pengguna app ini banyak
di device API 31 tepat (Android 12 non-L, jarang), sebagian besar effort tidak akan
termanifestasi jadi peningkatan visual buat mereka. Ini BUKAN alasan buat batal — cuma supaya
ekspektasi hasil akhir realistis, tidak dikira "semua device otomatis dapat kaca sungguhan".

---

## 3. Arsitektur — kenapa ini BUKAN cuma "edit 1 fungsi"

`frostedGlass()` (`BlurUtils.kt`) sekarang adalah **1 titik pusat** yang semua permukaan kaca di
app ini panggil (dikonfirmasi ulang di kode: MiniPlayerBar, tiap bottom sheet, kartu Home/
Library, panel NowPlaying — grep sendiri di komentar file). Godaan pertama: "tinggal ubah
fungsi itu". TIDAK SESEDERHANA ITU — blur backdrop asli, beda dari tint statis, secara struktural
butuh **2 pihak yang terkoordinasi**, bukan 1 fungsi mandiri:

1. **Source** — konten yang MAU di-blur (apa yang ada DI BELAKANG kaca — daftar lagu yang lagi
   di-scroll, artwork Now Playing, dst).
2. **Effect** — permukaan kaca itu sendiri, yang MENGAMBIL sampel dari (1) lalu blur+composite.

Keduanya harus berbagi 1 objek state yang sama (`HazeState` di API Haze) supaya (2) tahu persis
piksel mana yang mau di-sample dari (1). Ini kenapa desain ini perlu direncanakan dulu — nge-tag
`frostedGlass()` doang, tanpa juga nge-tag "apa yang ada di belakangnya", tidak menghasilkan
blur apa pun (Haze cuma akan sampling kanvas kosong/tidak nemu source).

### 3a. Di mana `HazeState` dipegang?

Diperiksa struktur nyata `MainActivity.kt`: `AppNavHost` (baris ~516) punya 1 `Scaffold` (baris
~844) yang isinya `MiniPlayerBar` (baris ~877) DAN `NavHost` (baris ~1022) sebagai 2 anak
langsung yang SAMA level — persis pola yang Haze asumsikan (1 shell, beberapa permukaan
mengambang di atas 1 konten yang berganti-ganti per layar). **Rekomendasi: 1 `HazeState`
dipegang di `AppNavHost`**, bukan per-layar terpisah:
- `remember { HazeState() }` di `AppNavHost`, diteruskan lewat `CompositionLocal` baru
  (`LocalHazeState`, pola identik `LocalIsDarkTheme` yang sudah ada) — supaya layar/sheet di
  dalam `NavHost` tidak perlu terima parameter baru satu-satu (lebih dari 20 file kalau lewat
  parameter, `CompositionLocal` lebih konsisten dgn pola desain redesign sejauh ini yang selalu
  "0 refactor tersebar" tiap kemungkinan).
- `NavHost`'s content (tiap layar) ditandai `Modifier.hazeSource(LocalHazeState.current)` di
  root Box/Column masing-masing layar — HANYA saat `isLiquidGlassTheme()`, 0 perubahan tema lain.
- `MiniPlayerBar` (yang mengambang DI ATAS `NavHost`, selalu terlihat lintas layar) jadi
  `Modifier.hazeEffect(LocalHazeState.current) { blurEffect { ... } }` — kandidat blur PERTAMA
  yang paling masuk akal (dampak-terbesar-dulu, pola sama fase 3), krn dia SATU-SATUNYA
  permukaan kaca yang persis "melayang di atas konten yang scroll di belakangnya" 100% waktu.
- Sheet MODAL (Equalizer, Queue, dst) — sumbernya SAMA (`LocalHazeState` yang sama, apa pun yang
  lagi tampil di layar saat sheet dibuka), cuma effect-nya ditambahkan di sheet itu sendiri.
  Tidak perlu `HazeState` terpisah per sheet.

### 3b. Perubahan di `frostedGlass()` sendiri

Cabang `isLiquidGlass` yang SUDAH ADA (Batch 281, saat ini pakai `edgeBrush` gradient statis
`LiquidGlassAccent`) diusulkan **ditambah**, bukan dihapus total: `hazeEffect` jadi lapisan
blur-nya, tint+edge gradient ungu yang sudah ada TETAP dipertahankan DI ATAS blur (bukan
diganti) — persis pola CONVX & Apple Liquid Glass asli (blur based + tint warna tipis + edge
highlight, bukan blur polos tanpa warna). 4 identitas lain (`isTactile`/`isSkeu`/else)
**0 disentuh sama sekali** — pola konsisten sejak fase 1.

---

## 4. Dependency baru yang dibutuhkan (belum ditambahkan — bagian eksekusi, bukan desain)

`dev.chrisbanes.haze:haze` (+ kemungkinan submodul `haze-blur`/`haze-materials` tergantung versi
2.x yang dipakai saat eksekusi — arsitektur modular ini BARU di 2.0, jadi persis modul mana yang
dibutuhkan HARUS dicek ulang di dokumentasi resmi/MavenCentral persis di momen eksekusi, bukan
ditulis versi pasti di sini sekarang — versi yang ditulis hari ini kemungkinan besar sudah basi
di batch eksekusi nanti). Compose Multiplatform-compatible (Android/iOS/Desktop/Web) — tidak
relevan utk project Android-only ini, cuma jadi sinyal tambahan library ini bukan proyek kecil
yang gampang mati.

**`app/build.gradle.kts` (protected asset) akan tersentuh** di batch eksekusi pertama — ini
DIBERITAHUKAN dari sekarang, bukan kejutan nanti.

---

## 5. Rencana batch eksekusi (draft urutan, TUNGGU giliran user minta lanjut)

Strict Micro-Batching tetap berlaku — TIDAK dieksekusi semua sekaligus:

1. **Fondasi plumbing** — tambah dependency Haze (`app/build.gradle.kts`), `LocalHazeState`
   CompositionLocal (file theme yang sudah ada atau baru), `HazeState` dipegang di
   `AppNavHost`. **0 visual berubah** — belum ada yang consume state ini.
   **✅ SELESAI Batch 295.** Versi dipilih: **`dev.chrisbanes.haze:haze:1.7.2`** — dicek ulang
   web_search Agustus 2026 persis di momen eksekusi (sesuai catatan §4 di bawah): tag "Latest"
   resmi (non-prerelease) di GitHub, BUKAN linimasa `2.0.0-alphaXX` yang lebih baru tapi masih
   pre-release aktif (5 alpha ~4 bulan terakhir, tiap rilis "Breaking Changes", modul dipecah
   wajib `haze`+`haze-blur`, API blur dibungkus `blurEffect{}` baru). STABILITY > Speed menang
   di atas rule #3 "prioritas mutakhir" — pijakan fondasi 4 sub-langkah berikutnya pakai API
   yang sudah tag stabil, bukan yang masih berpotensi breaking lagi. `LocalHazeState`
   (`Theme.kt`, pola identik `LocalIsDarkTheme`) + `rememberHazeState()` dipegang di
   `AppNavHost` (`MainActivity.kt`), `Scaffold` dibungkus `CompositionLocalProvider` (pola
   identik wrap Batch 24 yang sudah ada di file yang sama). **Dikonfirmasi grep: 0 pemakaian
   `.hazeSource()`/`.hazeEffect()` di manapun** — genuinely 0 visual berubah.
2. **MiniPlayerBar** — hazeSource di `NavHost` content + hazeEffect di `frostedGlass()`'s cabang
   LiquidGlass. Kandidat VISUAL PERTAMA yang kelihatan.
   **✅ SELESAI Batch 296.** API 1.7.2 dikonfirmasi ulang web_search sesi eksekusi (bukan asumsi
   desain): `hazeSource(state)`/`hazeEffect(state, style, block)`, properti blur (`blurRadius`
   dkk) diset LANGSUNG di lambda `block` (skema flat 1.x). `hazeSource` di `Box` pembungkus
   `NavHost` (`MainActivity.kt`), HANYA saat `isLiquidGlassTheme()`. `hazeEffect` di
   `frostedGlass()`'s cabang `isLiquidGlass`, dipasang SEBELUM `.background()` (urutan gambar:
   blur→tint→edge). `effectiveAlpha` Liquid Glass DITURUNKAN 0.55f/0.65f (dari 0.92f/0.96f
   default) — kalau tidak, tint sepekat default akan menutupi blur nyaris total; **titik awal,
   wajib dituning ulang pas langkah 5 (device)**. Parameter `blurRadius` fungsi (24.dp default,
   dummy sejak Batch 53) akhirnya benar-benar dipakai. **Cakupan otomatis LEBIH LUAS dari
   sekadar MiniPlayerBar**: krn `frostedGlass()` shared, `NowPlayingScreen`'s panel + 8 sheet
   lain ikut nyala jg (semua overlay di atas region `NavHost` yang di-tag) — TAPI langkah 3 di
   bawah TETAP belum ditandai selesai, krn belum ada verifikasi/analisis KHUSUS apakah
   NowPlayingScreen butuh treatment beda (cuma "kemungkinan sudah cukup" via cakupan otomatis,
   belum dikonfirmasi). **Belum diverifikasi compile Gradle sungguhan — WAJIB cek CI SEBELUM
   lanjut langkah 3**, risiko ganda (dependency Batch 295 + API Haze yang baru dipakai sekarang
   sama-sama belum pernah dicompile).
3. **NowPlayingScreen** — sumber konten beda (artwork/panel, bukan list scroll) — dicek dulu apa
   perlu treatment beda dari MiniPlayerBar sebelum asal disamakan.
   **✅ SELESAI Batch 297, hasil: 0 gap, 0 kode tambahan.** Kekhawatiran awal: "Kontrol Lanjutan"
   (`ModalBottomSheet`) render di window/layer terpisah, mungkin tidak bisa nyampling
   `hazeSource` window utama. Dijawab 2 web search: Haze py dukungan RESMI utk Dialog/
   ModalBottomSheet (official sample ada), syarat `containerColor = Color.Transparent` + tint
   manual (bukan Haze `tints`) — **sudah dipenuhi sejak lama** (konvensi existing app ini,
   kebetulan align). `AlbumArtHero` sendiri 0 butuh `hazeEffect` (dia SOURCE, bukan permukaan
   kaca).
4. **LibraryScreen row/Sheets/Dialog/Settings** — reuse titik yang sudah teraudit lengkap di
   Batch 282-286 (semua sudah punya daftar pasti file/baris mana yang pakai `frostedGlass()`,
   tidak perlu re-audit dari nol).
   **✅ SELESAI Batch 297 (sekaligus dgn langkah 3)** — sama alasan: 9 sheet app-wide semua
   sudah `containerColor` transparent, 0 kode tambahan diperlukan lewat titik-titik yang sudah
   dipetakan Batch 282-286.
5. **Verifikasi visual+performa di device sungguhan** — WAJIB, tidak bisa diverifikasi dari
   environment kerja sesi ini (tidak ada compiler/emulator). Termasuk cek performa (blur
   real-time genuinely berat GPU, terutama MiniPlayerBar yang sering re-render saat progress
   lagu jalan) — bukan cuma "kelihatan benar", tapi juga "tidak nge-lag".
   **✅ VISUAL dikonfirmasi user Batch 325** (blur kelihatan benar di device sungguhan, termasuk
   sheet/dialog cross-window yang dulu 0% — root cause Batch 311 kini genuinely tuntas via fix
   `containerColor` Batch 322-324). `liquidGlassAlpha` diturunkan balik 0.85f/0.90f→0.38f/0.48f
   (`BlurUtils.kt`, reuse nilai tuning Batch 299 yang sudah pernah lolos device dulu). **⏳ PERFORMA
   (GPU/lag saat MiniPlayerBar re-render) BELUM eksplisit dikonfirmasi user** — sisa item terbuka
   sub-langkah ini, jangan diasumsikan lolos cuma karena visual OK.

---

## 6. Yang TIDAK berubah

Sama seperti seluruh redesign Liquid Glass sejauh ini: 4 identitas lama (Apple/Tactile/
Neumorphism/Calm Retro) 0 tersentuh — cabang `isLiquidGlass` di `frostedGlass()` yang jadi
tempat SATU-SATUNYA logic baru ini hidup. Boundary permanen project (playback/data/SAF/database
FREEZE) berlaku sama seperti biasa.
