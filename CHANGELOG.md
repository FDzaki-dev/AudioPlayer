# Changelog

## Batch 346 — Fitur: art scale dinamis mengisi sisa ruang ala Spotify, NowPlayingScreen, 1 file kode
User pilih eksplisit lanjutkan trade-off yang dicatat Batch 345 ("Lanjut ide 'art scale dinamis'",
salah satu dari 3 pilihan yang ditawarkan sesi ini). Scope memang lebih besar dari Micro-Batch
biasa (blur/glow/hero shape ikut terdampak, seperti sudah diperingatkan Batch 345) — dieksekusi
sekarang karena user sudah konfirmasi eksplisit, bukan diam-diam.

**Pendekatan: ukur, bukan tebak/hardcode.** Grup konten judul-s/d-baris waktu (dulu dipusatkan
`Arrangement.Center` Batch 345) dibungkus 1 `Column` baru yang diukur via `onGloballyPositioned`
(state baru `contentGroupHeightPx`). Kunci teknis: `verticalScroll` pada Column induknya memberi
constraint tinggi TAK TERBATAS ke children (supaya tahu total tinggi yang bisa discroll) — jadi
tinggi yang dilaporkan grup ini SELALU intrinsik/asli, TIDAK PERNAH terpotong oleh `weight(1f)`
Column induk, beda dari kalau yang diukur adalah Column induk itu sendiri (yang akan selalu
melaporkan tinggi teralokasi, bukan tinggi konten). Pola `onGloballyPositioned` ini bukan hal baru
di codebase — sudah dipakai identik di `LibraryScreen.kt`/`QueueSheet.kt`/`SongPickerSheet.kt`/
`PlaylistScreen.kt`.

**Formula.** `dynamicArtSize` = (tinggi konten tersedia − chrome tetap − tinggi grup konten
terukur − 20dp selisih art↔glow), lalu di-clamp `[140dp, lebarLayar − 80dp]`. "Chrome tetap" =
Row ikon atas (48dp, default `IconButton`) + `Spacer` 12dp + `Spacer` 16dp + Row transport (68dp,
`FilledIconButton` play/pause eksplisit `.size(68.dp)`, child tertinggi di row itu) — keempatnya
SENGAJA dipakai sbg konstanta (bukan diukur run-time seperti grup konten), karena deterministik
dari kode sendiri (0 bergantung ke song/font-scale), demi 1 measurement loop lebih sedikit = risiko
lebih rendah. Batas lebar (`lebarLayar − 80dp`) mencegah piringan persegi lebih lebar dari layar;
80dp = 2× margin 40dp yang sudah dipakai default lama (280dp piringan di layar 360dp lebar = 320dp
konten setelah padding Column 20dp, sisa 40dp margin) — formula ini SENGAJA balik ke 280dp persis
di layar 360dp lebar, konsisten dgn tampilan default lama, bukan lompatan baru.

**Fallback pra-pengukuran.** Sebelum `onGloballyPositioned` sempat invoke sekali (frame pertama,
`contentGroupHeightPx == 0`), `dynamicArtSize` jatuh ke formula lama `albumArtBoxHeight` (Batch
336, sudah adaptif layar pendek) dikurangi 20dp — supaya 0 flash ukuran aneh sebelum pengukuran
nyata mendarat. `verticalArrangement = Arrangement.Center` (Batch 345) SENGAJA TIDAK dihapus dari
Column scrollable — sekarang berperan sbg jaring pengaman visual utk 1 frame transisi itu saja
(begitu ukuran stabil, gap yang perlu di-center-kan Center seharusnya sudah ~0 duluan lewat
piringan yang membesar/mengecil).

**`AlbumArtHero()` diparameterisasi — dulu 300dp/280dp hardcode literal.** Fungsi private ini
(1 titik pemakaian, cuma dipanggil dari `NowPlayingScreen` sendiri) sekarang menerima `artSize: Dp`
dari caller: glow Box `.size(300.dp)` → `.size(artSize + 20.dp)` (rasio 300−280=20dp lama
dipertahankan persis), `AlbumArt()` `.size(280.dp)` → `.size(artSize)`. Shadow/bevel/border/
scanline Tactile/Skeu/Calm Retro TIDAK disentuh sama sekali — semuanya sudah memakai `size` dari
`drawBehind`/`drawScope` di titik pakainya (bukan literal `280.dp` terpisah), jadi otomatis ikut
skala baru tanpa perlu diubah. Satu pengecualian: 1 blok komentar (justifikasi margin halo 18dp
Skeu, sebelumnya bilang "hero art ukurannya selalu tetap 280.dp") diperbarui — klaim itu sekarang
salah sejak batch ini, komentar diganti supaya tidak menyesatkan sesi berikutnya. Angka literal
margin halo itu sendiri (18dp/14dp/8dp/3dp) SENGAJA TETAP konstan di semua ukuran art — ini jarak
bayangan-ke-tepi-shape yang wajar konstan, bukan proporsi visual yang perlu ikut membesar/mengecil.

**1 file**: `NowPlayingScreen.kt` (non-protected). 2 import baru (`androidx.compose.ui.layout.
onGloballyPositioned`, `androidx.compose.ui.unit.Dp`), 1 state baru (`contentGroupHeightPx`), 1
`Column` pembungkus pengukur baru, 1 blok kalkulasi (`dynamicArtSize`/`dynamicGestureBoxHeight`),
1 parameter fungsi baru (`AlbumArtHero(artSize)`), 2 `.size()` literal diganti jadi parameter, 1
komentar basi diperbarui. 0 komposable lain disentuh, 0 logic gesture Batch 334 (brightness/
volume di luar ancestor scrollable) atau footer Batch 343 (Row transport fixed) diubah
(`ZERO-REFACTOR`). Bracket-matcher stack-based (comment/string di-strip) dijalankan atas seluruh
file — struktur bersarang genuinely valid, 0 mismatch. Brace/paren/bracket: 228/228 brace, 674/674
paren, 0/0 bracket (naik dari 222/222 brace & 663/663 paren sebelum batch ini — net penambahan
wajar sesuai volume kode/komentar baru, bukan tanda ketidakseimbangan).

**Belum diverifikasi compile Gradle sungguhan** — WAJIB cek CI. Risiko sintaks rendah (`Dp` &
`onGloballyPositioned` keduanya API resmi Compose, `onGloballyPositioned` sudah dipakai identik
di 4 file lain project ini; operator aritmatika `Dp` seperti `-`/`+`/`coerceIn`/`coerceAtLeast`
juga API standar Compose Foundation). **Belum diverifikasi visual di device** — prioritas cek:
(1) piringan membesar mengisi ruang kosong yang dulu jadi 2 gap Batch 345 di layar tinggi/normal;
(2) piringan tetap proporsional, tidak pernah melebihi lebar layar di device manapun; (3) di layar
pendek ATAU saat hint banner tampil (grup konten jadi lebih tinggi), piringan menyusut wajar &
Row transport tetap presisi di tepi bawah (0 regresi Batch 336-343); (4) transisi 1 frame awal
(fallback `albumArtBoxHeight` → ukuran dinamis terukur) tidak kelihatan kedip/lompat kasar —
entrance scale+alpha animation (sudah ada sejak awal) diharapkan cukup menyamarkan kalaupun ada.

## Batch 345 — Fix gap kosong tunggal jadi terdistribusi (verticalArrangement Top → Center), NowPlayingScreen, 1 file kode
User kirim 2 screenshot (crop Row 4-ikon atas + crop area waktu/transport) + 2 laporan: (1)
"susunan badge anomali yang terpaku oleh jarak", (2) "masih ada bagian kosong karena bagian
atas terlalu mentok ke badge — gak ada susunan normal begitu".

**Investigasi poin 1 (Row ikon atas) — DIUKUR, bukan ditebak.** Analisis pixel langsung atas
screenshot: jarak antar-4-ikon 279px/280px/279px — PERSIS merata, `Arrangement.SpaceBetween`
(Batch 342) masih benar 100%. Bobot visual ke-4 ikon (chevron/heart-border/info-outline/dots)
juga sudah konsisten tipis — `Icons.Outlined.Info` (Batch 343) masih benar, 0 regresi. Cross-
check riwayat: Batch 342 sendiri sudah eksplisit reject pola "1 ikon kiri + 3 ikon klaster
kanan" (dianggap user "berat sebelah") demi symmetric SpaceBetween ini — jadi kembali ke pola
grouped BUKAN arah yang benar (akan mengulang keluhan lama). Kesimpulan: 0 defect nyata di Row
itu sendiri.

**Root cause SEBENARNYA (dikonfirmasi via screenshot ke-2).** Fix Batch 343 (Row transport
dikeluarkan dari Column scrollable jadi footer fixed) MEMINDAH lokasi gap kosong, TIDAK
MENGHILANGKANNYA. Column scrollable+`weight(1f)` itu masih `verticalArrangement` default (Top)
— begitu tinggi konten (judul s/d baris waktu) LEBIH PENDEK dari ruang weighted yang tersedia
(kasus layar user), SEMUA sisa ruang kosong tetap menumpuk jadi SATU gap besar — cuma sekarang
lokasinya pindah dari "di bawah Row transport" (sebelum Batch 343) jadi "di antara baris waktu
& Row transport" (setelah Batch 343) — persis yang tampak di screenshot ke-2 user. Laporan poin
1 & 2 ternyata SATU root cause yang sama, dilihat dari 2 sudut: konten bagian atas (Row
ikon+art+judul+rating+slider) tetap rapat/"mentok"/"terpaku" ke atas walau ruang tersedia jauh
lebih tinggi dari kebutuhannya — 0 distribusi proporsional atas sisa ruang, semua dikumpulkan
jadi 1 blok kosong di 1 sisi saja, bukan "susunan normal" yang biasa dipakai app sejenis.

**Fix.** `verticalArrangement = Arrangement.Center` ditambahkan ke Column scrollable itu (import
`Arrangement` sudah tersedia lewat wildcard `androidx.compose.foundation.layout.*`, 0 import
baru). Saat konten LEBIH PENDEK dari tinggi viewport weighted, `Center` membagi sisa ruang
proporsional ke ATAS (antara art box & judul) DAN ke BAWAH (antara baris waktu & Row transport)
— 1 gap besar jadi 2 gap seimbang, jauh lebih dekat ke "susunan normal" yang diminta. 0 efek
saat konten SUDAH >= tinggi viewport (layar pendek/konten panjang, verticalScroll tetap jalan
identik seperti sebelumnya — `Center` cuma berlaku kalau benar-benar ada sisa ruang, 0 regresi
untuk kasus itu). Row transport TETAP fixed footer presisi di tepi bawah — Batch 343 TIDAK
disentuh/dibatalkan (sudah dikonfirmasi user "no more floating thing", tetap valid).

**Trade-off yang disadari (dicatat, bukan diselesaikan diam-diam):** solusi paling "otentik"
ala Spotify/Apple Music sebenarnya album art yang membesar mengisi sisa ruang layar tinggi
(bukan fixed 280dp) — tapi itu perubahan cakupan jauh lebih besar (blur/glow circle/hero shape
semua ikut terdampak, resiko tinggi utk Micro-Batch). `Center` dipilih sebagai fix minimal-resiko
sekarang; kalau user masih tidak puas & minta art yang scale dinamis, itu task terpisah lebih
besar — jangan dieksekusi diam-diam tanpa konfirmasi eksplisit dulu.

**1 file**: `NowPlayingScreen.kt` (non-protected). 1 parameter baru (`verticalArrangement`) + 1
blok komentar root-cause. 0 komposable lain disentuh, 0 logic scroll/gesture Batch 334 & Batch
343 diubah (`ZERO-REFACTOR`). Brace/paren seimbang raw (227/227, 974/974 — naik dari komentar
baru) + strip-komentar (226/226, 672/672 — IDENTIK sebelum batch ini; `Arrangement.Center` tanpa
kurung panggilan fungsi, 0 paren baru dari kode). Bracket-matcher stack-based (comment/string
di-strip) dijalankan ulang atas seluruh file — struktur bersarang genuinely valid. Diff terhadap
ZIP batch 344 terakhir dikonfirmasi HANYA 1 hunk (blok komentar + 1 baris parameter), 0 file lain
tersentuh. `FILE_MANIFEST.txt` tidak berubah.

**Belum diverifikasi compile Gradle sungguhan** — WAJIB cek CI. Risiko sintaks sangat rendah
(`Arrangement.Center` API resmi Compose Foundation, sudah dipakai identik di file lain project
ini). **Belum diverifikasi visual di device** — prioritas cek: (1) di layar tinggi/ruang lebih
(kasus user), sekarang ada 2 gap breathing room (atas: antara art box & judul; bawah: antara
baris waktu & Row transport) alih-alih 1 gap besar tunggal di bawah; (2) Row transport TETAP
presisi di tepi bawah layar (regresi 0 dari Batch 343); (3) di layar pendek/konten kepanjangan,
scroll tetap berfungsi identik seperti sebelumnya (0 defek baru); (4) Row 4-ikon atas TIDAK
berubah tampilan sama sekali (0 kode disentuh di situ, sesuai kesimpulan investigasi).

## Batch 344 — Fix sistemik: cover art letterbox (ContentScale.Fit → Crop di 1 fungsi bersama), 1 file kode
User kirim screenshot lagu "TOBI - Warm Up Mix 2023" + instruksi: "saya mau layout normal dan
generik". Screenshot menunjukkan kotak seni utama Now Playing (280dp hero) tampil dengan bar
kosong solid di atas & bawah gambar (mirip letterbox video) — foto konser/kembang api di
tengahnya TIDAK penuh mengisi kotak persegi, beda drastis dari screenshot Batch 342/343
sebelumnya (artwork lain, penuh edge-to-edge tanpa bar).

**Root cause (dikonfirmasi via kode, bukan tebakan).** `AlbumArt()` (`Utils.kt`) — fungsi
composable BERSAMA dipakai di 6 titik seluruh app (MiniPlayerBar 44dp, LibraryScreen grid
album `aspectRatio(1f)` + row 48dp, HomeScreen 56dp + 120dp, NowPlayingScreen hero 280dp) —
punya default parameter `contentScale = ContentScale.Fit`. `Fit` mempertahankan SELURUH gambar
tanpa crop di dalam batas kotak — untuk artwork non-1:1 (kasus nyata: thumbnail video 16:9 yang
ikut ke-embed saat lagu di-rip/tag dari YouTube, umum utuk file DJ-mix/mashup), sisa ruang
kosong di atas/bawah gambar menampilkan `Box.background(MaterialTheme.colorScheme.surfaceVariant)`
polos — di tema gelap, `surfaceVariant` gelap, kelihatan PERSIS seperti bar hitam letterbox.
Grep konfirmasi: dari 6 titik pemakaian `AlbumArt()`, HANYA 1 (backdrop blur full-screen Now
Playing, `NowPlayingScreen.kt` baris ~368) yang eksplisit override `contentScale = Crop` sejak
awal — itu sebabnya bug ini TIDAK PERNAH kelihatan di backdrop (selalu Crop, benar), tapi selalu
laten di ke-5 titik lain (kotak seni utama/thumbnail), baru NAMPAK saat kebetulan artwork lagu
yang sedang diputar bukan rasio persegi. Preseden project sendiri sudah konsisten ke arah Crop:
widget home-screen (`widget_player.xml`, Batch 204) sudah pakai `centerCrop`, bukan letterbox —
dan itu genre-standar universal (Spotify/Apple Music/YouTube Music SELALU crop-fill cover art,
tidak pernah pillarbox/letterbox artwork apa pun rasio aslinya) — persis makna "normal dan
generik" yang diminta user.

**Fix (1 baris default parameter + komentar, bukan patch per-titik-pakai).** `Utils.kt`:
`contentScale: ContentScale = ContentScale.Fit` → `= ContentScale.Crop`. Karena ini DEFAULT di
fungsi bersama, ke-5 titik yang sebelumnya diam-diam mengandalkannya (MiniPlayerBar, Library×2,
Home×2, hero Now Playing) otomatis ikut benar sekaligus — TIDAK perlu sentuh 5 file caller satu
per satu (lebih aman & lebih sedikit permukaan diff daripada override eksplisit di tiap titik).
Override eksplisit `Crop` di backdrop blur Now Playing SENGAJA TIDAK dihapus meski kini redundan
— bukan bagian dari bug ini, `ZERO-REFACTOR`.

**1 file**: `Utils.kt` (non-protected) — 1 default parameter diganti + 1 blok komentar
penjelasan root-cause ditambahkan. 0 logic lain di fungsi ini disentuh (fallback icon, tinted
background "no cover", `SubcomposeAsyncImage` loading/error state — semua persis sama). 0 dari
6 titik pemakaian `AlbumArt()` di-edit langsung (efek Batch ini murni lewat 1 default param).
Brace/paren seimbang (17/17 `{}`, 74/74 `()` — RAW dan strip-komentar SAMA karena satu-satunya
tambahan cuma blok komentar dokumentasi, 0 kode struktural baru). `FILE_MANIFEST.txt` tidak
berubah.

**Belum diverifikasi compile Gradle sungguhan** — WAJIB cek CI. Risiko sintaks sangat rendah:
ganti 1 nilai default parameter enum (`ContentScale.Fit` → `.Crop`, keduanya API resmi Compose
UI, sama-sama sudah dipakai project ini — `.Crop` sendiri sudah dipakai eksplisit di baris
lain file berbeda sejak awal). **Belum diverifikasi visual di device** — prioritas cek: (1)
kotak seni utama Now Playing lagu "TOBI - Warm Up Mix 2023" (atau lagu manapun dgn artwork
non-1:1) sekarang penuh mengisi kotak 280dp, 0 bar kosong di atas/bawah; (2) cek juga ke-4 titik
thumbnail lain (MiniPlayerBar, Library grid & row, Home) — pastikan SEMUA konsisten crop-fill,
0 letterbox tersisa di mana pun; (3) lagu dengan artwork PERSEGI (mis. anime cover Batch 342/343
sebelumnya) TIDAK berubah tampilannya (Crop pada gambar sudah persegi = identik hasil dgn Fit,
regresi visual 0 untuk kasus ini); (4) lagu TANPA artwork sama sekali — fallback icon
`MusicNote` + tinted background tetap tampil normal (jalur `else if (showIcon)`, tidak
tersentuh perubahan ini sama sekali).

## Batch 343 — Fix kontrol transport "mengambang" + ikon Info anomali di Row atas (NowPlayingScreen), 1 file kode
User kirim screenshot + 2 laporan eksplisit dalam 1 pesan: (1) "bagian pemutar dilarang keras
untuk mengambang/tidak menyentuh dasar sama sekali", (2) "perbaiki layout menu-menu yang
kelihatan anomali dibagian atas alih-alih rapi".

**Bug 1 — kontrol transport mengambang.** Root cause: Row transport (shuffle/prev/play-pause/
next/repeat) sebelumnya jadi child TERAKHIR di dalam `Column` yang sekaligus `weight(1f)` +
`verticalScroll(...)` (arsitektur "fixed header + scrollable body" sejak Batch 334). `Column`
biasa menaruh anak-anaknya rapat dari ATAS ruang yang tersedia (verticalArrangement default =
Top) — begitu total tinggi konten (judul s/d slider s/d transport) LEBIH PENDEK dari tinggi
weighted-area (kasus umum di layar normal/tinggi, karena album art box sudah fixed 300dp
duluan di header), transport row berhenti persis di bawah kontennya sendiri, menyisakan spasi
kosong di antara transport dan tepi bawah layar — persis "mengambang" yang dilaporkan user di
screenshot, bukan sekadar soal padding/margin yang kurang.

**Fix (struktural, bukan tuning angka/padding).** Row transport DIKELUARKAN dari `Column`
scrollable itu — penutup `Column` scrollable dipindah lebih awal (tepat setelah Row waktu
posisi/durasi), Row transport (beserta `Spacer(16.dp)` pemisahnya) jadi sibling TETAP (fixed)
milik `Column` induk (`fillMaxSize`), bukan lagi child di dalam area scroll. Karena `Column`
induk menaruh `Column` scrollable itu dengan `weight(1f)`, sisa ruang vertikal SELALU mengalir
penuh ke `Column` scrollable itu terlebih dulu, dan Row transport (fixed, ukuran instrinsik)
otomatis menempati posisi PALING TERAKHIR di `Column` induk — hasilnya Row transport selalu
presisi di tepi bawah (sebelum padding 20dp layar), 0 spasi kosong tersisa di bawahnya, apa pun
tinggi konten di atasnya atau tinggi layarnya. Efek samping positif: ini sekaligus menuntaskan
saga reachability transport row Batch 336-338 secara lebih kuat — transport SEKARANG SELALU
terlihat tanpa perlu discroll sama sekali (bukan cuma "terjangkau via scroll" seperti sebelumnya).
0 logic gesture/scroll/timing lain di `Column` scrollable itu diubah — murni 1 child (Row
transport) yang dipindah lokasi strukturalnya.

**Bug 2 — ikon Info Row atas terlihat anomali.** Batch 342 sudah memperbaiki SPACING Row 4-ikon
atas (Tutup/Favorit/Info/Kontrol Lanjutan) jadi `Arrangement.SpaceBetween`, dan screenshot user
kali ini mengonfirmasi spacing itu memang sudah renggang merata (bukan lagi berat sebelah) —
tapi user masih melaporkan Row ini "kelihatan anomali". Root cause BEDA level dari Batch 342:
`Icons.Default.Info` adalah varian "Filled" Material Design, yang me-render sebagai lingkaran
PADAT/solid dengan "i" di dalamnya — satu-satunya ikon berbentuk badge solid di antara 3 ikon
lain di Row yang sama (Tutup/chevron, Favorit-border, Kontrol Lanjutan/titik-tiga) yang SEMUANYA
guratan tipis/outline. Bobot visual yang jomplang inilah yang terbaca sebagai "anomali" — 1 ikon
menonjol sendirian di antara 3 ikon minimalis, persis kelas masalah yang sudah pernah diaudit
di kategori Iconography project ini (Batch 228, "samakan visual weight icon sejenis").

**Fix.** `Icons.Default.Info` → `Icons.Outlined.Info` — varian ini cuma lingkaran GARIS tipis +
"i" tipis, bobot visual sama dengan 3 ikon lain di Row yang sama. Paket `material-icons-extended`
(sumber `Icons.Outlined.*`) SUDAH jadi dependency app ini sejak lama (dikonfirmasi grep
`app/build.gradle.kts`) — 0 dependency baru. Import `androidx.compose.material.icons.filled.Info`
dihapus (grep konfirmasi 1 satu-satunya pemakaian di file ini), diganti
`androidx.compose.material.icons.outlined.Info` (pelajaran Batch 233: ganti nama/varian ikon
WAJIB disertai update import yang sesuai, bukan cuma di titik pemakaian). 0 posisi/spacing/
handler/tooltip Row ini disentuh — `Arrangement.SpaceBetween` dari Batch 342 tetap dipertahankan
apa adanya, terbukti sudah benar dari screenshot user.

**1 file**: `NowPlayingScreen.kt` (non-protected). 1 import dihapus, 1 import baru (net 0). 0
handler/callback/urutan logis ikon diubah, 0 komposable lain di file ini disentuh
(`ZERO-REFACTOR`). Brace/paren diverifikasi seimbang DUA cara: raw 227/227 `{}` + 953/953 `()`
(naik dari komentar penjelasan baru, bukan dari kode — sama pola false-positive yang sudah
berulang kali dicatat project ini, mis. Batch 335/342); strip-komentar 226/226 `{}` + 672/672
`()` — IDENTIK dengan angka sebelum batch ini (murni relokasi brace + swap 1 ikon, 0 logic
baru/hilang). Verifikasi tambahan: bracket-matcher berbasis stack (bukan cuma hitung jumlah)
dijalankan atas seluruh file (comment & string literal di-strip dulu) — mengonfirmasi struktur
bersarang genuinely valid, bukan cuma jumlah simbol yang kebetulan sama. `FILE_MANIFEST.txt`
tidak berubah (0 file baru/dihapus).

**Belum diverifikasi compile Gradle sungguhan** (0 akses jaringan/SDK di sandbox sesi ini) —
**WAJIB cek CI setelah push**. Risiko sintaks rendah: restrukturisasi Bug 1 murni memindah posisi
1 kurung kurawal penutup + 1 blok kode (Row transport) yang isinya sama sekali tidak diubah;
`Icons.Outlined.Info` adalah API resmi Compose Material Icons Extended yang sudah jadi
dependency lama app ini (belum pernah dipakai sebagai `Icons.Outlined.*` di file manapun
sebelumnya — pola BARU untuk project ini, tapi dependency-nya sendiri sudah lama terpasang &
teruji lewat `Icons.Default.*`/`Icons.Filled.*` di ratusan titik lain).

**Belum diverifikasi visual di device** — prioritas cek: (1) buka Now Playing, konfirmasi Row 5
tombol transport sekarang presisi menempel di tepi bawah layar (sebelum padding 20dp), 0 spasi
kosong tersisa di bawahnya, baik di layar tinggi/normal maupun pendek; (2) konfirmasi konten
judul-slider di atasnya TIDAK terpotong/berubah tampilan (murni Row transport yang pindah,
bukan konten lain); (3) ikon Info Row atas sekarang tampil sebagai lingkaran GARIS tipis + "i"
tipis (bukan lagi lingkaran padat solid), bobot visual seragam dengan 3 ikon lain di Row yang
sama; (4) tap ikon Info tetap toggle kartu tip gestur seperti biasa (0 fungsi berubah, murni
tampilan ikon).

## Batch 342 — Relokasi tata letak Row ikon atas NowPlayingScreen jadi simetris, 1 file kode
User kirim screenshot NowPlayingScreen + instruksi eksplisit: "relokasi layout agar simetris dan
professional look!!" — Row ikon atas (Tutup/Favorit/Info/Kontrol Lanjutan) di screenshot terlihat
berat sebelah: tombol Tutup terisolasi di ujung kiri dengan jarak kosong besar, sementara 3 ikon
lain (Favorit/Info/Kontrol Lanjutan, hasil penambahan tombol Info Batch 341) menumpuk rapat di
ujung kanan.

**Root cause.** `Row` induk cuma punya 1 `Spacer(modifier = Modifier.weight(1f))` tunggal tepat
setelah `IconButton` Tutup — mekanisme ini mendorong SEMUA sisa ikon (Favorit/Info/Kontrol
Lanjutan) menempel jadi satu klaster di ujung kanan, membelah Row jadi kelompok 1 ikon lawan 3
ikon alih-alih renggang merata. Ini murni akibat tata letak `Spacer` manual, bukan bug fungsional
— tiap ikon sendiri sudah benar (handler/tint/contentDescription 0 masalah).

**Fix (relokasi murni, 0 ikon ditambah/dihapus/diganti fungsi).** `Spacer(weight(1f))` manual
dibuang; `Row` induk diberi `horizontalArrangement = Arrangement.SpaceBetween` (import sudah
tersedia lewat wildcard `androidx.compose.foundation.layout.*` yang sudah ada di file ini sejak
awal, 0 import baru). Efeknya: ke-4 `IconButton` sekarang tersebar merata sepanjang lebar Row —
Tutup tetap presisi di kiri mentok, Kontrol Lanjutan tetap presisi di kanan mentok (0 perubahan
posisi tepi), Favorit & Info kini punya jarak yang proporsional di antara keduanya dan terhadap
2 ikon tepi, bukan lagi berdesakan sebagai 1 klaster. Urutan logis ikon (Tutup→Favorit→Info→
Kontrol Lanjutan, dari Batch 341) TIDAK diubah — cuma jarak antar-ikon yang direlokasi.

**1 file**: `NowPlayingScreen.kt` (non-protected). 0 import baru, 0 handler/tint/contentDescription
disentuh, 0 komposable lain di file ini disentuh (`ZERO-REFACTOR`). Brace/paren seimbang (226/226,
925/925 raw; 226/226, 671/671 strip-komentar — parens naik murni dari 1 blok komentar penjelasan
baru + parameter `horizontalArrangement` baru, bukan dari logic tambahan). `FILE_MANIFEST.txt`
tidak berubah (0 file baru/dihapus).

**Belum diverifikasi compile Gradle sungguhan** (0 akses jaringan/SDK di sandbox sesi ini) —
**WAJIB cek CI setelah push**, risiko sintaks sangat rendah (`Arrangement.SpaceBetween` adalah
enum resmi Compose Foundation yang sudah dipakai identik di file yang sama, mis. baris 689/1798).
**Belum diverifikasi visual di device** — prioritas cek: buka Now Playing, konfirmasi 4 ikon Row
atas sekarang renggang merata (bukan lagi 1 ikon kiri + 3 ikon menumpuk kanan), Tutup & Kontrol
Lanjutan tetap presisi di kedua tepi layar seperti sebelumnya (0 regresi posisi tepi).

## Batch 341 — FITUR: ganti banner onboarding auto-tampil-sekali jadi tombol info permanen (NowPlayingScreen), 1 file kode
User laporan + screenshot: banner tip gestur (geser=kecerahan/volume, ⋮=Sleep Timer/Kecepatan/
Equalizer) "bisa kena dismiss permanen dan gak balik lagi" — begitu di-tap X sekali, hilang
selamanya, 0 cara buka lagi. Instruksi eksplisit: ganti jadi tombol khusus onboarding di samping
ikon favorit.

**Mekanisme baru.** `showNowPlayingHint` mulai `false` (bukan lagi auto-`true` di first-launch
dari `OnboardingHintStore.hasSeenNowPlayingHint()`), dikontrol tombol baru (ikon Info) di Row
atas, persis di samping ikon favorit. Tap = toggle buka/tutup kartu tip — bisa dibuka lagi
kapan saja, tidak pernah "habis" secara permanen. `onDismiss` kartu tidak lagi memanggil
`hintStore.markNowPlayingHintSeen()` — cuma tutup kartu saat ini. `OnboardingHintStore`/
`hintStore` dihapus dari file ini (class-nya sendiri tetap ada, masih dipakai `LibraryScreen.kt`
untuk hint lain, tidak disentuh).

**Efek samping disengaja.** Cabang `showNowPlayingHint -> 260.dp` di `albumArtBoxHeight` (Batch
338, susutkan art box preemptive selama hint kebetulan tampil) dihapus — alasannya sudah tidak
berlaku sejak hint tidak lagi otomatis muncul tanpa diminta. Ini menuntaskan akar masalah seluruh
saga scroll-reachability Batch 336-338. Cabang layar pendek (Batch 336, `screenHeightDp <
640.dp`) tidak disentuh — fix terpisah, tidak terkait hint.

**1 file**: `NowPlayingScreen.kt`. Brace/paren seimbang (226/226, 920/920 raw; 226/226, 674/674
strip-komentar). Belum diverifikasi compile/device — lihat `PROJECT_STATE.md` Batch 341 untuk
checklist verifikasi lengkap.

## Batch 340 — Lanjutan Batch 339: fix .frostedGlass() kelewat di 3 dari 6 sheet tersisa, 3 file kode
User upload ZIP baru (source of truth, Hard Reset — lompat dari Batch 323 internal sesi
sebelumnya ke Batch 339, isi Batch 324-339 dari sesi lain dibaca ulang bukan ditimpa) +
"sempurnakan latest task!!". Melanjutkan antrean eksplisit Batch 339 (pola identik,
pra-disetujui "boleh dikerjakan tanpa tanya ulang").

Batch 339 menemukan `containerColor = Color.Transparent` (Batch 322/323) ternyata tidak cukup
sendirian tanpa `.frostedGlass()` — hasilnya tembus pandang sungguhan (0 blur, 0 fill), bukan
cuma "kurang blur". Diverifikasi ulang langsung (bukan percaya log mentah): 6 sheet dikonfirmasi
0x `frostedGlass()`. Fix diterapkan ke 3 dari 6: `BackupRestoreSheet.kt`, `DiagnosticLogSheet.kt`,
`DuplicateFinderSheet.kt` (`+.frostedGlass()` setelah `.fillMaxWidth()` + import). Sisa 3
(`SignatureMatcherSheet.kt`, `SmartPlaylistScreen.kt`, `VaultSheet.kt`) diantre — setelah itu
semua 7 sheet dari audit Batch 339 tuntas. Tint `liquidGlassAlpha` masih sengaja tidak diturunkan
(tunggu verifikasi device semua 7 sheet).

## Batch 339 — BUG FIX x2: tab Cek Update — (a) regresi "tembus pandang" (frostedGlass kelewat), (b) unduhan/APK ke-reset kalau sheet salah ke-tap/tertutup (1 file kode)
User laporan + screenshot: "tab update masih mengalami regresi tembus pandang", dan "saat user
sudah selesai install update package tapi gak sengaja salah mencet, malah ke cancel dari awal
lagi unduhannya". (Permintaan ke-3 user di sesi ini — "tab onboarding khusus" pengganti banner
hint — sengaja BELUM dieksekusi, scope-nya arsitektural/besar, ditanyakan dulu terpisah.)

### Bug (a) — "tembus pandang"
**Root cause**: `containerColor = Color.Transparent` (ditambahkan Batch 322/323, syarat wajib
sample resmi Haze untuk blur lintas-window) ternyata **tidak cukup sendirian** — parameter itu
cuma mematikan fill solid default Material3, sama sekali tidak menggambar blur apa pun. Modifier
yang benar-benar menggambar blur adalah `.frostedGlass()` (`BlurUtils.kt`) — dibandingkan 12+
call site lain di app ini (`RingtoneCutterSheet.kt`, `SongInfoEditSheet.kt`, dan sejenisnya),
`UpdateCheckSheet.kt` adalah **satu-satunya** yang kelewat modifier ini sejak sheet ini dibuat.
Container transparan TANPA `frostedGlass()` = benar-benar tembus pandang (0 blur, 0 fill sama
sekali) — bagian "Tentang Aplikasi" dari `SettingsScreen.kt` di baliknya (sheet ini dibuka dari
sana) kelihatan penuh tanpa filter apa pun, tumpang-tindih dengan konten sheet sendiri, persis
seperti screenshot user.

**Fix**: `.frostedGlass()` ditambahkan pada `Column` konten sheet, di posisi identik dengan
`RingtoneCutterSheet.kt`/`SongInfoEditSheet.kt` — setelah `.fillMaxWidth()`, sebelum
`.verticalScroll()`. Pola 1:1, tidak ada penyesuaian tambahan.

**🔍 Audit tambahan (didokumentasikan, TIDAK diperbaiki batch ini — ZERO-REFACTOR, Micro-Batch
1 file sudah terpakai bug (b) di bawah)**: grep ulang seluruh sheet dengan `containerColor =
Color.Transparent` dibanding yang punya `.frostedGlass()` — ditemukan **6 sheet lain** dengan
gap identik, berpotensi mengalami "tembus pandang" yang sama kalau dibuka:
`BackupRestoreSheet.kt`, `DiagnosticLogSheet.kt`, `DuplicateFinderSheet.kt`,
`SignatureMatcherSheet.kt`, `SmartPlaylistScreen.kt`, `VaultSheet.kt`. Bukan laporan user
sekarang, jadi tidak disentuh — tapi kandidat kuat untuk batch berikutnya (pola identik, boleh
dikerjakan tanpa tanya ulang, sama seperti presedan Batch 322→323).

### Bug (b) — unduhan/APK ke-reset ke nol
**Root cause**: `DisposableEffect`'s `onDispose { UpdateManager.reset() }` sebelumnya berjalan
**tanpa syarat** setiap kali sheet ini keluar dari komposisi — baik sengaja ditutup user, maupun
salah ke-tap/ke-dismiss tidak sengaja. Ini termasuk saat state sedang `Downloading` (thread
unduhan sungguhan TETAP berjalan di background terpisah dari lifecycle Compose — tidak benar-
benar ter-cancel) atau sudah `ReadyToInstall` (APK **sudah lengkap** tersimpan di cache).
Me-reset state ke `Idle` pada momen itu membuang progres nyata secara sia-sia; ditambah
`checkForUpdate()` (dipanggil di titik masuk `DisposableEffect`) yang otomatis jalan ulang dari
nol setiap sheet dibuka kembali — hasil akhirnya user harus "mengunduh ulang dari awal" padahal
APK sebenarnya sudah ada/lengkap di cache device.

**Fix**: baik `checkForUpdate()` (saat masuk) maupun `reset()` (saat `onDispose`) sekarang
**dilewati sama sekali** kalau state `UpdateManager` saat itu adalah `Downloading` atau
`ReadyToInstall` — kedua state ini merepresentasikan kerja nyata (unduhan berjalan / APK sudah
jadi) yang tidak boleh hilang hanya karena sheet-nya tertutup. Untuk state lain (`Idle`,
`Checking`, `UpToDate`, `Available`, `Error`) — **0 perubahan perilaku**, tetap cek ulang setiap
dibuka & reset setiap ditutup seperti sebelumnya (tidak ada progres berarti yang bisa hilang di
state-state itu).

**1 file**: `UpdateCheckSheet.kt` (non-protected). 1 import baru:
`com.rudi.audioplayer.ui.theme.frostedGlass`. Brace/paren dicek seimbang (27/27, 99/99).

**Belum divalidasi compile Gradle sungguhan** (0 akses jaringan/SDK di sandbox sesi ini) —
**WAJIB cek CI setelah push**.

**Belum diverifikasi visual di device** — prioritas cek: (1) buka "Cek Update" dari Settings —
background sheet sekarang harus terlihat blur/frosted (bukan transparan polos), 0 teks "Tentang
Aplikasi" tembus dari layar Settings di belakangnya; (2) mulai unduhan update, TUTUP sheet di
tengah proses (tap area gelap di luar sheet) — buka ulang "Cek Update": progres unduhan (state
`Downloading`) harus **tetap lanjut**, bukan balik ke "Mengecek rilis terbaru…"; (3) tunggu
sampai `ReadyToInstall`, lalu tutup sheet (baik sengaja maupun simulasi salah-tap) — buka ulang:
harus **langsung** menampilkan "Unduhan selesai — siap instal" + tombol "Buka Installer",
**bukan** mengunduh ulang dari nol.

## Batch 338 — BUG FIX: scroll tetap kepicu di layar "normal" selama hint banner sekali-tampil masih nongol (3 lever: art box, teks banner, spacer; 1 file kode)
User: "untuk ukuran layar saya, seharusnya mode scroll gak kepicu". Klarifikasi: hint banner
konfirmasi MASIH nongol (belum pernah di-dismiss) di skenario ini.

**Konteks**: Batch 337 menyelesaikan *reachability* (transport row kejangkau via scroll di
layar pendek) — tapi permintaan user di sini lebih jauh: di layar yang dia anggap NORMAL,
scroll idealnya tidak perlu terjadi sama sekali, bukan sekadar "berfungsi kalau terpaksa
terjadi". Root cause: Batch 336 hanya menyusutkan art box di layar `< 640.dp`; Batch 337
memindahkan hint banner supaya bisa ikut discroll. Di layar `>= 640.dp` (dianggap "normal"),
art box tetap penuh `300.dp` + `FeatureHintBanner` (~150dp sebelum dipersingkat, kondisi
sekali-tampil) + sisa konten bisa tetap total melebihi tinggi viewport SELAMA hint masih
tampil — walau device itu sendiri bukan "layar pendek" dalam pengertian Batch 112/336.

**Fix — 3 lever dikombinasi, semuanya SEMENTARA** (aktif hanya selagi `showNowPlayingHint ==
true`; kembali ke ukuran/spacing penuh biasa begitu di-dismiss permanen via `hintStore`):
1. Art box ikut menyusut ke `260.dp` saat hint tampil — tidak lagi bergantung hanya pada
   `screenHeightDp < 640.dp`; kedua kondisi independen, yang menghasilkan ukuran terkecil yang
   dipakai.
2. Teks `FeatureHintBanner` dipersingkat — isi kedua tip (kecerahan/volume via geser piringan;
   Sleep Timer/Kecepatan/Equalizer via menu ⋮) **tidak berkurang maknanya**, hanya dikemas lebih
   padat (perkiraan ~5 baris `bodySmall` → ~2 baris).
3. Dua `Spacer` di sekitar hint banner diciutkan khusus saat hint tampil (`16.dp → 8.dp`,
   `32.dp → 20.dp`).

Estimasi reklaim ruang gabungan ~130–140dp, ditargetkan cukup untuk layar "normal" muat tanpa
scroll selama masa onboarding sekali-tampil ini. Di luar kondisi itu (hint sudah pernah
di-dismiss, atau layar pendek `< 640.dp`) — perilaku Batch 336/337 tidak berubah sama sekali.

**1 file**: `NowPlayingScreen.kt` (non-protected). 0 import baru. Brace/paren dicek seimbang
(224/224, 894/894 — turun 1 pasangan brace dari batch sebelumnya karena `if/else` diganti
`when` untuk `albumArtBoxHeight`; angka baru konsisten seimbang sendiri, bukan indikasi
kerusakan sintaks).

**Belum divalidasi compile Gradle sungguhan** (0 akses jaringan/SDK di sandbox sesi ini) —
**WAJIB cek CI setelah push**.

**Belum diverifikasi visual di device** — prioritas cek: (1) buka Now Playing untuk PERTAMA
KALI (hint tampil) di layar user — harus muat tanpa perlu scroll sama sekali sekarang; (2)
dismiss hint (tombol X), lalu buka ulang Now Playing — art HARUS balik ke `300.dp` penuh seperti
sebelum batch manapun di rangkaian ini (0 regresi tampilan permanen di luar masa onboarding);
(3) teks hint yang dipersingkat tetap jelas, tidak kehilangan makna salah satu dari 2 tip-nya.

## Batch 337 — BUG FIX: Batch 336 (art box adaptif) terbukti belum cukup — root cause satu level lebih dalam, FeatureHintBanner ~150dp (1 file kode)
User konfirmasi via klarifikasi: "Layar pendek: tombol transport MASIH belum kejangkau walau
discroll habis (Batch 336 belum ngefek)".

**Root cause — ditelusuri ulang ke histori Batch 112** (kebijakan Batch 24: fix resmi sudah
diikuti tapi gejala identik/berlanjut → curigai akar beda level, jangan ulangi variasi kecil dari
pendekatan sama). Catatan asli Batch 112 (sumber jaring pengaman ini) eksplisit menyebut
`FeatureHintBanner` (~150dp, kalau belum di-dismiss user) sebagai salah satu kontributor UTAMA
overflow di layar pendek — setara atau lebih besar dari Box art 300dp, terutama dikombinasikan
3-button nav (masih umum di device Android 15 ke bawah/budget). Batch 336 hanya menyusutkan art
box; `FeatureHintBanner` (juga bagian fixed header zone, tidak pernah disentuh) tetap jadi
bottleneck utama ruang scroll — itu sebabnya perbaikan Batch 336 "belum ngefek" di device user:
lever yang ditarik bukan yang paling dominan.

**Fix**: `FeatureHintBanner` (0 custom gesture handling — cuma `Card` + teks + tombol dismiss,
aman dipindah) dipindah dari fixed header zone menjadi child PERTAMA di dalam
`Column(Modifier.weight(1f).verticalScroll(...))` (Batch 334/335/336) — sekarang ikut menjadi
bagian yang bisa "discroll lewat" untuk menjangkau transport row, bukan lagi permanen
menghabiskan jatah fixed zone yang tidak bisa direbut scroll apa pun. Trade-off yang sengaja
diambil: urutan visual hint banner geser dari SEBELUM Box art menjadi SESUDAH Box art (masih di
atas judul lagu) — reachability transport row (fungsi inti aplikasi) diprioritaskan di atas
posisi visual hint banner (onboarding sekali-tampil, dismissable, non-esensial).

Box gesture art (fix nested-scroll-conflict Batch 334), fix overscroll glow (`overscrollEffect =
null`, Batch 335), dan art box adaptif berbasis `LocalConfiguration` (Batch 336) — **ketiganya 0
disentuh**, tetap berlaku bersamaan. Ini PELENGKAP, bukan pengganti, Batch 336 — kedua fix
sama-sama mengurangi porsi fixed non-scrollable, cuma target elemen berbeda (art vs hint banner).

**1 file**: `NowPlayingScreen.kt` (non-protected). 0 import baru (dipindah, bukan ditambah —
`FeatureHintBanner` sudah diimpor/dipakai sebelumnya di file yang sama). Brace/paren dicek
seimbang (225/225, 880/880).

**Belum divalidasi compile Gradle sungguhan** (0 akses jaringan/SDK di sandbox sesi ini) —
**WAJIB cek CI setelah push**. Risiko sintaks rendah — perubahan murni pemindahan blok kode
(`if (showNowPlayingHint) { ... }`) ke lokasi lain di Column yang sama, 0 logic/API baru.

**SUDAH diverifikasi visual di device — user konfirmasi.** Screenshot user (diambil PAS aktif
discroll, jari masih narik layar) menunjukkan kelima tombol transport (shuffle, prev,
play/pause, next, repeat) semua kejangkau penuh — persis target fix ini. Teks
`FeatureHintBanner` yang di screenshot itu kelihatan "kepotong" (mulai dari "volume HP..." bukan
dari awal kalimat "Geser di...") sempat dicurigai bug baru, tapi setelah dikonfirmasi user itu
cuma frame mid-scroll yang wajar — sama seperti konten scrollable manapun, bagian atas (hint
banner, sekarang child pertama Column scrollable) kegeser duluan saat scroll aktif berlangsung,
otomatis kembali utuh dari awal kalimat begitu scroll berhenti/kembali ke posisi 0. **Bukan
bug**, 0 perubahan kode diperlukan untuk item ini.

Root cause 3-lapis lintas batch — Batch 335 (overscroll glow palsu) → Batch 336 (art box fixed
300dp tidak adaptif) → Batch 337 (`FeatureHintBanner` ~150dp ikut fixed, kontributor utama yang
belum tersentuh) — **kini terbukti tuntas** berdasarkan bukti device nyata, bukan cuma asumsi
kode benar. 0 perlu lanjutan kode untuk masalah "transport row tidak kejangkau di layar pendek".

## Batch 336 — BUG FIX: transport row TETAP tidak kejangkau via scroll di layar pendek, jaring pengaman Batch 112/334 regresi nyata (1 file kode)
User laporan device: dikonfirmasi lewat 3 opsi klarifikasi (overscroll glow / transport
kepotong-tidak kejangkau / scroll di layar lain) — user pilih **"transport masih
kepotong/tidak kejangkau di layar pendek"**. Ini persis item yang catatan Batch 335 tandai
"Belum diverifikasi visual di device" (poin (2): "di layar PENDEK yang genuinely butuh scroll,
tombol transport harus TETAP reachable via scroll seperti sebelumnya — regresi ke arah itu TIDAK
boleh terjadi") — dan ternyata memang REGRESI.

**Root cause — BEDA level dari Batch 335** (kebijakan Batch 24: kalau fix resmi sudah diikuti
tapi gejala serupa/berlanjut, curigai akar beda, jangan ulangi variasi kecil dari pendekatan
sama). Batch 335 cuma mematikan efek visual overscroll glow — 0 mengubah soal ALOKASI RUANG.
Header hasil split Batch 334 (Row tombol atas + hint banner opsional + Spacer 12dp + Box gesture
album art **fixed `.height(300.dp)`, sengaja TIDAK scrollable** supaya gesture brightness/volume
lolos dari nested-scroll conflict) sekarang mengunci porsi tetap dari tinggi layar yang TIDAK
bisa disusut oleh scroll apa pun — beda dari sebelum Batch 334, saat semuanya (termasuk art)
masih 1 Column scroll tunggal sehingga art bisa ikut ke-scroll off-screen kalau perlu ruang.
Konsekuensinya: `Column(Modifier.weight(1f).verticalScroll(...))` di bawah header cuma kebagian
SISA tinggi layar setelah header+art (fixed). Di layar pendek (landscape, split-screen, foldable
tertutup) total header+art (~300dp+) bisa mendekati/melebihi tinggi layar itu sendiri — sisa
ruang scroll kepepet sampai nyaris 0dp, transport row jadi TIDAK kejangkau walau Column-nya
secara teknis tetap scrollable.

**Fix**: `Box` gesture art — tinggi hardcode `.height(300.dp)` → `.height(albumArtBoxHeight)`,
dihitung sekali dari `LocalConfiguration.current.screenHeightDp.dp`:
- Layar `>= 640.dp` (mayoritas HP potret normal): tetap `300.dp` persis — **0 perubahan visual**
  dibanding sebelumnya.
- Layar `< 640.dp` (pendek): disusutkan proporsional `screenHeightDp * 0.28f`, dibatasi
  `coerceIn(160.dp, 300.dp)` — lantai 160dp supaya art tidak jadi terlalu kecil untuk dilihat,
  plafon 300dp supaya tidak pernah lebih besar dari sebelumnya.

Struktur & gesture zone TIDAK diubah — Box gesture art TETAP di luar ancestor scrollable manapun
(0 regresi ke fix nested-scroll-conflict Batch 334, swipe brightness/volume 0 terdampak);
`Modifier.weight(1f).verticalScroll(state = ..., overscrollEffect = null)` (Batch 335) juga 0
disentuh. Efeknya murni geometris: susutkan porsi fixed non-scrollable → sisa ruang scrollable
bertambah proporsional → transport row balik kejangkau di layar pendek, tanpa mengubah tampilan
apa pun di layar normal/tinggi.

**1 file**: `NowPlayingScreen.kt` (non-protected). 1 import baru:
`androidx.compose.ui.platform.LocalConfiguration` (satu package dengan `LocalContext`/
`LocalDensity` yang sudah dipakai file ini — bukan dependency baru buat project). Brace/paren
dicek seimbang (225/225, 865/865). 0 import/dependency lain berubah.

**Belum divalidasi compile Gradle sungguhan** (0 akses jaringan/SDK di sandbox sesi ini) —
**WAJIB cek CI setelah push**. Risiko sintaks rendah: `LocalConfiguration`/`screenHeightDp` API
stabil & lama (bukan API baru/eksperimental seperti `overscrollEffect` di Batch 335), pola
umum dipakai luas di ekosistem Compose.

**Belum diverifikasi visual di device** — prioritas cek: (1) layar pendek asli (landscape, atau
split-screen Termux+app berdampingan) — buka Now Playing, scroll area judul-transport sampai
habis, tombol play/pause/next/prev HARUS kejangkau penuh, tidak terpotong; (2) layar
normal/potret biasa — ukuran album art HARUS identik dengan sebelum batch ini (regresi visual di
layar normal = bug baru, tanda `albumArtBoxHeight` salah hitung); (3) swipe kecerahan/volume di
piringan (fix Batch 334) & fix overscroll glow (Batch 335) — keduanya harus TETAP mulus, 0
terdampak batch ini.

## Batch 335 — BUG FIX: overscroll stretch-glow kepicu di Column judul-transport meski konten muat, regresi dari Batch 334 (1 file kode)
User laporan device (format T/J singkat): "bagian bawah (judul-tombol transport) yang masih bisa
discroll — itu gimana?" / "Scroll-nya kepicu padahal konten harusnya muat (bug baru)".

**Root cause — BEDA dari bug Batch 334** (itu soal 2 pointer-drag recognizer axis sama bentrok;
ini soal ukuran & efek visual, bukan konflik gesture): pemisahan Batch 334 memberi Column
judul-transport `Modifier.weight(1f)` sendiri — artinya Column ini sekarang punya tinggi TETAP
dari sisa ruang layar (dipaksa min=max oleh `weight(fill=true)` default), BUKAN lagi unbounded
seperti Column tunggal lama sebelum Batch 334 yang membungkus SELURUH layar (header+art+body
sekaligus). Di layar yang cukup tinggi, konten (judul s/d tombol transport) genuinely muat penuh
di dalam ruang yang dialokasikan (`ScrollState.maxValue` = 0) — TAPI efek overscroll stretch-glow
bawaan Android 12+/Compose Foundation tetap terpicu VISUAL tiap kali area itu disentuh-drag,
independen dari apakah posisi scroll benar-benar berpindah atau tidak (rubber-band effect kosong,
tanpa ada apa pun untuk diungkap). User membaca sensasi visual ini sebagai "masih bisa discroll".

**Fix**: `Modifier.verticalScroll(rememberScrollState())` diganti
`Modifier.verticalScroll(state = rememberScrollState(), overscrollEffect = null)` — overload
resmi `Modifier.verticalScroll()` yang menerima `OverscrollEffect?` langsung sebagai parameter,
matikan overscroll KHUSUS scope Column ini, 0 titik lain di app tersentuh.

**Sengaja BUKAN pola lama** `CompositionLocalProvider(LocalOverscrollConfiguration provides
null)` (dipakai `SmartPlaylistScreen.kt` sejak Batch 263, saat BOM project masih 2024.05.00) —
dicek ulang `web_search` ke dokumentasi resmi Compose Foundation sesi ini: `LocalOverscrollConfiguration`/
`OverscrollConfiguration` **sudah dinyatakan deprecated** (diganti `LocalOverscrollFactory` +
`rememberPlatformOverscrollFactory`), persis risiko yang sudah ditandai eksplisit di catatan
Batch 291 soal lompatan Compose BOM 2024.05.00→2026.04.01 ("`LocalOverscrollConfiguration`
tersangka pertama kalau CI gagal... belum diperbaiki preventif"). Overload `overscrollEffect` di
`verticalScroll()` sendiri sudah tersedia jauh di bawah BOM 2026.04.01 yang dipakai project ini —
dipakai langsung sesuai kebijakan prioritas mutakhir (aturan sesi #3, Batch 205), bukan menambah
1 lagi titik pakai API yang sudah diketahui berisiko usang. `SmartPlaylistScreen.kt` (satu-satunya
pemakai lama pola deprecated itu) **SENGAJA TIDAK disentuh** batch ini — di luar cakupan laporan
bug ini, konsisten ZERO-REFACTOR (kandidat modernisasi terpisah kalau diminta user nanti).

**Cakupan fix**: HANYA Column scrollable hasil split Batch 334 (judul s/d tombol transport). Box
gesture brightness/volume (sekarang tidak lagi scrollable sejak Batch 334) & header (tombol
atas/hint/art) 0 disentuh — tidak relevan ke bug ini. Jaring pengaman scroll genuine untuk layar
pendek (Batch 112/334, kalau konten benar-benar overflow) TETAP jalan penuh lewat `scrollState`
yang sama persis — cuma efek visual overscroll DI LUAR rentang scroll asli yang dimatikan, 0
logic gesture/scroll/threshold lain diubah.

**1 file**: `NowPlayingScreen.kt` (non-protected). Brace/paren dicek seimbang (223/223, 854/854 —
parens naik dari 840→854 murni dari blok komentar baru yang ditambahkan menjelaskan root
cause+fix, bukan dari kode; kedua sisi naik jumlah identik, saldo tetap 0). `FILE_MANIFEST.txt`
tidak berubah (188/188). 0 import baru (`verticalScroll` sudah diimpor sebelumnya di file ini;
parameter `overscrollEffect` cuma dioper literal `null`, `OverscrollEffect` sendiri tidak perlu
diimpor eksplisit).

**Belum divalidasi compile Gradle sungguhan** (0 akses jaringan/SDK di sandbox sesi ini) —
**WAJIB cek CI setelah push**. Risiko sintaks rendah-menengah: overload ini dikonfirmasi ada di
dokumentasi resmi Compose Foundation lewat `web_search` (bukan tebakan dari training data), TAPI
ini kali PERTAMA app ini memakai parameter `overscrollEffect` langsung (pola berbeda dari
`LocalOverscrollConfiguration` lama yang sudah pernah terbukti compile di `SmartPlaylistScreen.kt`)
— jadi tetap wajib dikonfirmasi CI, bukan diasumsikan aman hanya karena dokumentasinya cocok.

**Belum diverifikasi visual di device** — prioritas cek: (1) buka Now Playing di layar yang cukup
tinggi (konten judul-transport harusnya muat penuh tanpa perlu scroll) — coba drag di area
judul/artist/rating/seekbar/baris waktu/tombol transport, TIDAK boleh lagi ada efek stretch/glow/
pergeseran visual apa pun; (2) di layar PENDEK yang genuinely butuh scroll (skenario asli jaring
pengaman Batch 112/334), tombol transport harus TETAP reachable via scroll seperti sebelumnya —
regresi ke arah itu TIDAK boleh terjadi; (3) swipe kecerahan/volume di piringan (fix Batch 334,
area terpisah — di atas Column ini) harus TETAP mulus, 0 terdampak batch ini.

## Batch 334 — BUG FIX: gesture brightness/volume bentrok dengan verticalScroll (1 file kode)
User laporan + screenshot: swipe kecerahan/volume di piringan (Now Playing) "bentrokan langsung"
dengan sesuatu — dikonfirmasi via baca kode (bukan tebakan): root cause SAMA PERSIS pola bug
klasik Compose nested-drag-gesture-conflict.

**Root cause**: Column induk layar ini py `.verticalScroll(rememberScrollState())` (Batch 112,
jaring pengaman biar transport row tidak kepotong di layar pendek). Box gesture brightness/
volume (`detectVerticalDragGestures`, drag VERTIKAL) ada DI DALAM Column itu — 2 pointer-drag
recognizer di SUMBU YANG SAMA (parent scrollable + child custom gesture) bersarang bikin
keduanya berebut touch stream yang identik. Child SUDAH `change.consume()` di `onVerticalDrag`,
TAPI ancestor `Modifier.scrollable()` (dasar `verticalScroll`) tetap bisa menang arbitrase
drag-start/touch-slop LEBIH DULU sebelum child sempat consume — gejala: swipe kecerahan/volume
jadi tersendat/salah kebaca sbg scroll, PERSIS sesuai laporan "bentrokan langsung".

**Fix (struktural, bukan hack pointer-arbitration level-rendah)**: Column induk (header: tombol
atas + hint banner + Box art/gesture) **TIDAK LAGI scrollable sama sekali** — dipisah jadi Column
BARU khusus (`Modifier.weight(1f).verticalScroll(...)`) yang HANYA membungkus konten SETELAH
Box art (judul, artist, rating, waveform+slider, posisi/durasi, tombol transport). Pola "fixed
header + scrollable body" standar Compose, dipilih ketimbang solusi `NestedScrollConnection`/
pointer-pass manual (lebih rumit, lebih riskan salah tanpa bisa dicompile-test di sandbox ini).

**Jaring pengaman Batch 112 TETAP UTUH, cuma scope-nya diperbaiki**: transport row tetap bisa
discroll kalau layar pendek (skenario asli Batch 112 fix) — bedanya sekarang scroll TIDAK LAGI
ikut membungkus area gesture yang architecturally memang tidak boleh ikut ancestor scrollable.
0 logic gesture brightness/volume itu sendiri diubah (`applyBrightness`/`applySystemVolume`/
threshold/dll sama persis) — murni pemindahan posisi 1 `Column` dalam hierarki.

**1 file**: `NowPlayingScreen.kt` (non-protected). Brace/paren dicek seimbang (223/223,
840/840). `FILE_MANIFEST.txt` tidak berubah (188/188, diverifikasi diff eksplisit). **Belum
diverifikasi visual di device** — prioritas cek: (1) swipe kecerahan/volume di piringan
sekarang GENUINELY mulus tanpa tersendat/salah baca, (2) di layar pendek/3-button-nav, transport
row masih bisa dijangkau via scroll (regresi Batch 112 tidak boleh kembali), (3) scroll TIDAK
ikut ter-trigger tanpa sengaja saat swipe di piringan (harus 100% milik gesture brightness/
volume sekarang, 0 scroll-bleed), (4) hint banner + tombol atas (tutup/favorit/⋮) tetap diam di
posisi (bagian fixed, tidak ikut scroll body).

## Batch 333 — Pending Queue item 2: feedback tekan tombol yang belum punya `bouncyPress` (1 file)
Lanjutan audit yang sengaja ditunda Batch 332 ("jangan asumsikan 'tombol kontrol pemutaran' =
transport row yang sudah lama beres — audit dulu"). Hasil audit `grep` menyeluruh SEMUA
`IconButton`/`FilledIconButton` di `NowPlayingScreen.kt` (9 titik) + `MiniPlayerBar.kt` (1
titik):

**Transport row (shuffle/prev/play-pause/next/repeat, 5 titik) — 100% SUDAH `bouncyPress`**,
persis dugaan Batch 332. **0 IconButton di `MiniPlayerBar.kt` yang kurang** (cuma 1 titik total
di file itu, Play/Pause, sudah ada). TAPI ditemukan **3 titik lain DI LAYAR YANG SAMA**
(`NowPlayingScreen.kt`) yang 0 `bouncyPress` sama sekali — inkonsisten krn SEMUA tetangganya di
row/screen yang sama sudah punya:

1. **Tombol tutup** (`KeyboardArrowDown`, baris atas) — `onBack`.
2. **Tombol "⋮ Kontrol Lanjutan"** (`MoreVert`) — pintu masuk ke sheet Queue/Lirik/Sleep
   Timer/Speed/Equalizer dkk.
3. **5 bintang rating** (`StarRatingRow`, loop `for (star in 1..5)`) — masing-masing dapat
   `MutableInteractionSource` SENDIRI (bukan 1 shared utk 5 tombol — tiap bintang perlu scale
   independen saat ditekan sendiri-sendiri).

**Fix**: `bouncyPress()` ditambah ke ketiganya, reuse persis pola yg sudah ada di file yang
sama — tombol tutup & "⋮" pakai default `pressedScale = 0.88f` (sama seperti shuffle/prev/
next/repeat, tanpa override), 5 bintang pakai `pressedScale = 0.75f` (reuse angka favorite-icon
di file yang sama, bukan angka baru — favorit & rating sama-sama ikon kecil sekunder, beda
kelas dari transport 36-68dp).

**Sengaja TIDAK disentuh**: baris-baris di dalam sheet "Kontrol Lanjutan" (`AdvancedControlRow`)
— itu list-row biasa (touch target selebar row, bukan icon-button lepas), pola feedback-nya
ripple standar Material row, BUKAN kandidat `bouncyPress` (modifier ini secara konsisten cuma
dipakai app ini utk kontrol icon-button/circular lepas, 0 preseden dipakai di list row manapun,
grep-confirmed) — memaksakan di sana justru bikin pola baru yang tidak konsisten, bukan
menyamakan.

**1 file**: `NowPlayingScreen.kt` (non-protected, 3 titik). Brace/paren dicek seimbang (222/222,
827/827). `FILE_MANIFEST.txt` tidak berubah (188/188, diverifikasi diff eksplisit). Docs
disinkronkan: README.md (banner), CHANGELOG.md.

**Pending Queue: KOSONG** — kedua item Batch 330 (icon morph Play/Pause + feedback tekan)
selesai dikerjakan (Batch 332 + 333). Sesi berikutnya kembali ke `ROADMAP_LIQUID_GLASS_REDESIGN.md`
kalau tidak ada instruksi/temuan baru dari user (aturan sesi #4).

## Batch 332 — Micro-interaction icon morph Play/Pause (Pending Queue item 1, 2 file kode)
User: "lanjut", melanjutkan item #1 Pending Queue Batch 331 (pra-dicatat, tidak perlu tanya
ulang — pola sama antrean eksplisit Batch 322-324/330-331).

**Konteks**: tombol Play/Pause di `NowPlayingScreen.kt` & `MiniPlayerBar.kt` sama-sama sudah
pakai `AnimatedContent(targetState = uiState.isPlaying)` sejak lama (Batch 224/226, utk
kompensasi ukuran+bias-optik ikon), TAPI tanpa `transitionSpec` eksplisit — jatuh ke default
Compose (`fadeIn() togetherWith fadeOut()`, crossfade polos), bukan "morph" sungguhan.

**Implementasi (identik di 2 file)**: `transitionSpec` ditambah — ikon baru masuk `scaleIn`
(dari 0.6x) + `fadeIn` bersamaan; ikon lama keluar `scaleOut` (ke 0.6x) + `fadeOut` bersamaan.
Durasi REUSE PERSIS pola asimetris "masuk 200ms lebih pelan, keluar 150ms lebih cepat" yang
sudah divalidasi Batch 330 (NavHost tab transition) — bukan angka tebakan baru, konsisten
bahasa gerak app ini. `togetherWith` dipakai (infix modern, bukan `with` yang sudah deprecated
sejak Compose 1.4).

3 import baru per file: `scaleIn`, `scaleOut`, `togetherWith` (`androidx.compose.animation`).
`MiniPlayerBar.kt` sekalian dapat `fadeIn`/`fadeOut` (belum pernah diimport eksplisit di file
itu sebelumnya — dulu cuma `AnimatedContent` tanpa transitionSpec jadi tidak butuh). Offset
+1dp kompensasi-bias-optik PlayArrow (Batch 226) TIDAK disentuh — tetap jalan bersamaan dgn
morph baru ini (independen, beda modifier).

**2 file**: `NowPlayingScreen.kt` (1 titik) + `MiniPlayerBar.kt` (1 titik, non-protected).
Brace/paren dicek seimbang: `NowPlayingScreen.kt` (219/219, 821/821), `MiniPlayerBar.kt`
(13/13, 108/108). `FILE_MANIFEST.txt` tidak berubah (188/188, diverifikasi diff eksplisit).
Docs disinkronkan: README.md (banner), CHANGELOG.md.

**Pending Queue (sisa 1 item dari Batch 330, belum dikerjakan, tunggu instruksi user)**: Feedback
tekan tombol kontrol pemutaran (scale-down halus saat pressed) — catatan: transport
Play/Pause/Skip di 2 file ini SUDAH pakai `.bouncyPress()` (scale-down saat pressed, sejak Batch
72 dst) untuk SEMUA tombolnya termasuk Play/Pause — kandidat ini kemungkinan besar CUMA relevan
utk kontrol LAIN yang belum py `bouncyPress()` (mis. slider seek, tombol di sheet lain) — perlu
audit dulu titik mana yang genuinely belum punya feedback tekan sebelum eksekusi, bukan
diasumsikan "tombol kontrol pemutaran" = transport row yang sudah lama beres.

## Batch 331 — Transisi push horizontal untuk stats_dashboard (drill-down dari Pengaturan), 1 file kode
User: "lanjut" — melanjutkan item #1 Pending Queue Batch 330 (kandidat animasi berikutnya).

`stats_dashboard` sebelumnya ikut default fade generik dari `NavHost` (Batch 330), padahal
navigasi ini hierarkis (drill-down dari Pengaturan), beda sifat dari tab lateral
home/library/settings yang cukup crossfade. Diberi `enterTransition`/`popExitTransition` sendiri
di composable-nya: slide dari kanan + fade saat masuk, slide balik ke kanan + fade saat di-pop
(tombol back) — pola gerak iOS-push. `tween(300)` REUSE persis dari `popExitTransition` rute
"now_playing" di file yang sama, bukan angka baru. 2 import baru: `slideInHorizontally`,
`slideOutHorizontally` (`androidx.compose.animation`).

**Koreksi mid-implementasi**: draft awal sempat menambah 4 field transisi (enter/exit/popEnter/
popExit) ke `stats_dashboard`, tapi diverifikasi ulang lewat dokumentasi resmi Navigation-Compose
(`exitTransition`/`popEnterTransition` sebuah destination hanya dievaluasi kalau destination itu
jadi *initialState* forward-nav / *targetState* pop — bukan kondisi yang pernah terjadi untuk rute
leaf yang cuma dimasuki via forward-nav dari luar & keluar via `popBackStack()`, diverifikasi grep
`navController.navigate(` app-wide). 2 field itu dibuang sebelum dikirim — cuma `enterTransition`
(aktif saat dia jadi target forward-nav) & `popExitTransition` (aktif saat dia jadi initial pop)
yang genuinely dieksekusi. Sisi "settings" (initial saat forward-nav ke sini, target saat pop
balik) pakai default `NavHost` Batch 330 apa adanya, tidak perlu override tambahan.

1 file: `MainActivity.kt` (**Protected, edit parsial** — `composable("stats_dashboard")` diberi 2
parameter transisi + route diubah ke bentuk trailing-lambda parameter, 2 import baru ditambah, 0
baris composable lain disentuh). Brace/paren dicek seimbang (264/264 `{}`, 655/655 `()`).

**Ringkasan file** — 1 file kode (jauh di bawah batas Micro-Batch). `FILE_MANIFEST.txt` tidak
berubah (188/188). Docs disinkronkan: README.md (banner), PROJECT_STATE.md.

**Pending Queue (sisa 2 item dari Batch 330, belum dikerjakan)**: (1) Micro-interaction tombol
Play/Pause (icon morph play↔pause). (2) Feedback tekan tombol kontrol pemutaran (scale-down halus
saat pressed).

## Batch 330 — Default crossfade transisi tab navigasi bawah (Beranda/Perpustakaan/Pengaturan), 1 file kode
User: prioritas animasi/transisi "yang paling berdampak ke user langsung", gaya "smooth kayak
iOS" didefinisikan sebagai fade/slide halus, ringan & minim risiko.

Audit `NavHost` (`MainActivity.kt`) menemukan hanya rute "now_playing" yang punya
`enterTransition`/`exitTransition` sendiri (slide+fade, sudah ada sejak lama) — 4 rute lain
(`home`/`library`/`settings`/`stats_dashboard`) 0 transisi sama sekali, cut instan bawaan Compose
Navigation. Tab bawah (`NavigationBarItem` onClick → `navController.navigate(...)`) adalah
interaksi paling sering dipakai user tiap sesi, jadi diprioritaskan duluan dibanding rute lain
yang lebih jarang disentuh.

`enterTransition`/`exitTransition`/`popEnterTransition`/`popExitTransition` ditambah di level
parameter `NavHost` (bukan per-`composable()`) — jadi default untuk semua rute yang tidak override
sendiri, 0 duplikasi ke 4 tempat. `fadeIn(tween(200))` masuk / `fadeOut(tween(150))` keluar,
simetris maju/mundur — kedua angka REUSE persis dari yang sudah ada di file yang sama
(`tween(200)` exitTransition rute "now_playing", `tween(150)` fadeIn `NowPlayingScreen.kt`), bukan
angka tebakan baru. 0 import baru ditambah (`fadeIn`/`fadeOut`/`tween` sudah dipakai file ini).

Rute "now_playing" TIDAK terdampak — override eksplisitnya sendiri menang atas default `NavHost`
baru ini. `popEnterTransition` baru ini secara teknis inert untuk "now_playing" (diverifikasi grep
`navController.navigate(` app-wide: tidak ada rute yang pernah pop kembali ke "now_playing" — jadi
0 risiko regresi walau parameter itu tidak dideklarasikan eksplisit di composable-nya sendiri).

1 file: `MainActivity.kt` (**Protected, edit parsial** — 4 parameter baru ditambah ke `NavHost(...)`
yang sudah ada, 0 baris lain disentuh). Brace/paren dicek seimbang (260/260 `{}`, 633/633 `()`).

**Ringkasan file** — 1 file kode (jauh di bawah batas Micro-Batch). `FILE_MANIFEST.txt` tidak
berubah (188/188). Docs disinkronkan: README.md (banner), PROJECT_STATE.md.

**Pending Queue (kandidat animasi berikutnya, belum dikerjakan)**: transisi push `stats_dashboard`
saat ini ikut default fade generik yang sama seperti tab lateral, padahal ini navigasi hierarkis
(drill-down dari Pengaturan) — kandidat upgrade ke slide-horizontal ala iOS push kalau user mau
lanjut. Kandidat lain: micro-interaction tombol Play/Pause (icon morph), feedback tekan tombol
kontrol pemutaran (scale-down halus).

## Batch 329 — Matikan blur asli Liquid Glass PERMANEN app-wide, 2 file kode
User pilih opsi paling aman dari 2 opsi yang ditawarkan, setelah root cause stutter/lag Batch 328
ditelusuri lebih dalam: blur asli (`hazeEffect`) baru genuinely aktif di 17/17 `ModalBottomSheet`
sejak Batch 324 — sheet "Kontrol Lanjutan" + `MiniPlayerBar` (SELALU tervisible & terus resample
tiap frame selama musik main) adalah persis biaya GPU per-frame yang sudah diperingatkan sejak
param `blurRadius` pertama ditambah (komentar Batch 298/300).

**Root cause**: berbeda dari Batch 328 (yang cuma revert animasi Aurora di atas glass), batch ini
mematikan MEKANISME blur asli itu sendiri — `hazeEffect` (Haze) resample backdrop tiap frame saat
konten di belakang kaca berubah, dan `MiniPlayerBar` yang selalu tervisible selama playback
bersaing langsung dengan thread audio/UI.

**Keputusan (sesuai `STABILITY > Speed`)**: `hazeEffect` dihapus dari cabang Liquid Glass
(`BlurUtils.kt`) — `glassBase` sekarang selalu `this` (identik ke-4 identitas lain, 0 blur asli).
`hazeSource` (`MainActivity.kt`) juga dilepas — 0 consumer tersisa berarti capture backdrop tiap
frame cuma buang biaya GPU tanpa manfaat visual apa pun kalau dibiarkan terpasang. Tint
(`liquidGlassAlpha`) dinaikkan balik ke fallback opaque 0.85f (gelap) / 0.90f (terang) — BUKAN
angka baru, reuse persis fallback "0 blur terlihat, tint sendiri wajib jaga keterbacaan" yang
sudah pernah tervalidasi Batch 311-324, kini jadi status permanen (bukan darurat sementara).
`hazeState`/`LocalHazeState`/`CompositionLocalProvider` (Theme.kt, MainActivity.kt) SENGAJA TIDAK
dibongkar — direuse persis state Batch 295 (murni plumbing, 0 consumer, 0 perubahan visual),
menghindari risiko membongkar `CompositionLocalProvider` yang membungkus ratusan baris Scaffold.
Parameter `blurRadius` (fungsi `frostedGlass()`) balik ke status "kept for source compatibility,
unused" persis pra-Batch-296 — signature publik tidak diubah.

2 file: `BlurUtils.kt` (hapus import+call `hazeEffect`, `glassBase` selalu `this`,
`liquidGlassAlpha` 0.38f/0.48f → 0.85f/0.90f), `MainActivity.kt` (Protected/edit parsial — hapus
`.then(...)`/`Modifier.hazeSource(state = hazeState)` di Box NavHost, hapus import `hazeSource`
tak terpakai). Brace/paren dicek seimbang keduanya (BlurUtils.kt 5/5 `{}` 159/159 `()`;
MainActivity.kt 256/256 `{}` 620/620 `()`).

**Ringkasan file** — 2 file kode (di bawah batas Micro-Batch). `FILE_MANIFEST.txt` tidak berubah
(188/188). Docs disinkronkan: README.md (banner + § "Rencana v2" Liquid Glass), PROJECT_STATE.md.

## Batch 328 — REVERT Aurora rim-glow animation, regresi performa dikonfirmasi user, 3 file kode
User: "lakukan perbaikan akhir sebelum masuk fase discontinued" → scope dikonfirmasi: "musik
stuttering/mandek saat diputar, lagging & nge glitch saat swipe kontrol lanjutan".

**Root cause**: asumsi Batch 326 ("1 `rememberInfiniteTransition` dibagi = performa aman")
keliru. Berbagi 1 instance mengurangi JUMLAH transition (12+→1) tapi tidak menghilangkan bahwa
phase berubah tiap frame memicu recomposition brush di semua consumer `frostedGlass()` sekaligus
— termasuk `MiniPlayerBar` (selalu tervisible selama musik main) dan sheet "Kontrol Lanjutan".

**Keputusan**: direvert penuh ke statis (bukan ditambal/dioptimasi lebih jauh), sesuai
`STABILITY > Speed` dan mengingat proyek akan masuk fase discontinued. Alpha Batch 327
(`AuroraRimGlowAlpha` 0.44f + taper 0.85x/0.65x/0.46x) tetap dipertahankan statis — bukan
penyebab regresi.

3 file: `Theme.kt` (`LocalAuroraPhase` dihapus penuh, bukan ditinggal dead code),
`MainActivity.kt` (Protected/edit parsial — blok phase computation dihapus, provider balik ke
`LocalHazeState` saja, 5 import tak terpakai dibuang), `BlurUtils.kt` (Aurora branch balik ke
`Brush.linearGradient` statis, import `lerp` dibuang).

## Batch 327 — Naikkan alpha rim-glow Aurora — token baru `AuroraRimGlowAlpha`, 2 file kode
User (device sungguhan): "terlalu tipis, hampir tak kasat mata" — dikonfirmasi lewat
`ask_user_input_v0` scope-nya rim-glow per-panel Batch 326, bukan ambient wash `auroraGlow()`
(0 dikeluhkan, tidak disentuh).

`AuroraRimGlowAlpha = 0.44f` (`Color.kt`) — token baru, terpisah dari `AuroraGlowAlpha` (0.34f)
yang dipakai ambient wash, supaya menaikkan rim tidak ikut menaikkan wash yang sudah pas.
Puncaknya disamakan ke level "accent-glow biasa ~0.42-0.45f" yang sudah didokumentasikan di
tempat lain app ini (komentar emerald streak Skeu Batch 80) — bukan angka tebakan baru.

Multiplier taper per-stop rim-glow (`BlurUtils.kt`) juga dinaikkan: 0.85x/0.6x/0.35x →
0.85x/0.65x/0.46x. Floor (stop ke-4, titik paling redup) naik dari alpha efektif 0.119 → 0.202
(~70% lebih terang), taper tetap dipertahankan (masih memudar antar stop, bukan flat solid).

## Batch 326 — Aurora rim-glow statis → animated via `LocalAuroraPhase`, 3 file kode
User: "next: Aurora statis -> bergerak!!". Kandidat yang sudah dicatat eksplisit sejak komentar
Batch 310 ("kandidat animasi kalau user minta lanjut nanti setelah statis ini terverifikasi
visual dulu") — precondition itu terpenuhi (Batch 325).

Kekhawatiran performa Batch 310 (12+ call site `frostedGlass()` × transition independen)
ditangani via arsitektur: 1 phase float dihitung sekali di `AppNavHost` (pola identik
`hazeState`/`LocalHazeState` Batch 295), dibagi ke semua panel lewat `LocalAuroraPhase`
(CompositionLocal baru, `Theme.kt`) — 0 transition tambahan per panel. Resep animasi (20 detik/
arah, LinearEasing, Reverse) dan mekanisme `lerp()` antar-hue disalin persis dari `auroraGlow()`
(TactileDepth.kt) — bukan API/angka baru.

3 file: `Theme.kt` (+`LocalAuroraPhase`), `MainActivity.kt` (Protected/edit parsial — phase
computation + provider di `AppNavHost`, root Surface's `auroraGlow()` TIDAK disentuh),
`BlurUtils.kt` (Aurora branch `frostedGlass()` kini animated). `TactileDepth.kt` (ambient wash
root) sengaja tidak disentuh sama sekali — scope minimal, 0 risiko tambahan.

## Batch 325 — Turunkan `liquidGlassAlpha` balik ke nilai tuning device terakhir yang sah, 1 file kode
User konfirmasi lewat `ask_user_input_v0`: blur Liquid Glass sudah kelihatan benar di device
sungguhan (termasuk sheet/dialog cross-window yang dulu 0% — root cause Batch 311, dituntaskan
via fix `containerColor` Batch 322-324). Ini melengkapi sub-langkah 5/5 (visual) di
`LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §5.

`liquidGlassAlpha` (`BlurUtils.kt`) diturunkan dari fallback aman darurat Batch 311
(0.85f/0.90f) ke **0.38f/0.48f** — bukan angka baru/tebakan, murni reuse nilai tuning Batch 299
yang sudah pernah lolos 1 putaran verifikasi device dulu, sebelum dinaikkan darurat karena bug
tak-terkait (root cause aslinya `containerColor`, bukan tint terlalu tipis). `blurRadius` (32dp)
dan gap kontras dark/light (0.10) tidak disentuh — bukan lever yang relevan untuk masalah ini.

**Performa (GPU/lag saat MiniPlayerBar re-render) belum eksplisit dikonfirmasi user** — satu-
satunya item terbuka tersisa di seluruh roadmap Liquid Glass; jangan diasumsikan lolos cuma
karena visual sudah OK.

## Batch 324 — Tuntaskan antrean Batch 322/323: fix `containerColor` di `VaultSheet.kt`, 1 file kode
User: "next", melanjutkan antrean eksplisit Batch 322/323 (pola identik, pra-disetujui, tidak perlu
tanya ulang). Fix `+containerColor = Color.Transparent` diterapkan ke `ModalBottomSheet` di
`VaultSheet.kt` + import `androidx.compose.ui.graphics.Color` (belum ada sebelumnya). Brace/paren
dicek seimbang (101/101 `{}`, 210/210 `()`).

Dengan ini **ke-7 gap yang ditemukan grep Batch 322 tuntas semua** — diverifikasi ulang app-wide
dengan grep multi-baris (bukan cuma single-line): **17/17 call site `ModalBottomSheet` sudah
konsisten pasang `containerColor = Color.Transparent`**.

Tint `liquidGlassAlpha` masih sengaja belum diturunkan, sama alasan Batch 322/323 (tunggu
verifikasi visual device dulu sebelum ubah fallback 0.85f/0.90f).

## Batch 323 — Lanjutan Batch 322: fix containerColor 3 dari 4 sheet tersisa, 3 file kode
Melanjutkan antrean eksplisit Batch 322 (pola identik, sudah pra-disetujui). Fix
`containerColor = Color.Transparent` diterapkan ke `SignatureMatcherSheet.kt`,
`SmartPlaylistScreen.kt`, `UpdateCheckSheet.kt`. Sisa 1 file (`VaultSheet.kt`) diantre — setelah
itu ke-7 gap yang ditemukan Batch 322 tuntas semua (17/17 call site `ModalBottomSheet` konsisten).
Tint `liquidGlassAlpha` masih sengaja tidak diturunkan (tunggu verifikasi device, lihat Batch 322).

## Batch 322 — Fix blur lintas-window Liquid Glass (root cause Batch 311 diriset ulang), 3 file kode
User konfirmasi eksplisit: "Fix blur lintas-window (sentuh MainActivity.kt, protected)".

**Riset ulang.** Klaim Batch 311 ("RenderNode Haze tidak bisa sample lintas-window sama sekali")
diverifikasi ke dokumentasi resmi `chrisbanes/haze` (sample `BottomSheet.kt`/`DialogSample.kt`
upstream) — TIDAK akurat. Sample resmi membuktikan `hazeEffect` di dalam `ModalBottomSheet`/`Dialog`
sungguhan BISA sample dari `hazeSource` di window Activity berbeda, asal `containerColor =
Color.Transparent` dipasang & `hazeState` yang sama dipakai kedua sisi.

**`MainActivity.kt` diperiksa, TIDAK diubah.** Wiring `hazeSource`/`CompositionLocalProvider` di
file ini dicek baris-per-baris terhadap pola resmi — sudah sesuai, 0 bug ditemukan. Termasuk
verifikasi eksplisit bahwa sheet "Kontrol Lanjutan" (dilaporkan Batch 311) ada di dalam scope
provider `LocalHazeState`, bukan di luar seperti sempat dicurigai.

**Bug nyata: 7 dari 17 `ModalBottomSheet(` call site tidak pernah pasang `containerColor =
Color.Transparent`** (syarat wajib pola resmi Haze) — termasuk `BackupRestoreSheet.kt`,
`DiagnosticLogSheet.kt`, `DuplicateFinderSheet.kt`, `SignatureMatcherSheet.kt`,
`SmartPlaylistScreen.kt`, `UpdateCheckSheet.kt`, `VaultSheet.kt`. Sheet "Kontrol Lanjutan" sendiri
ternyata SUDAH benar sejak awal — gap ini bug nyata & terpisah, bukan penyebab tunggal laporan asli.
3 diperbaiki batch ini (batas Micro-Batch): `BackupRestoreSheet.kt`, `DiagnosticLogSheet.kt`,
`DuplicateFinderSheet.kt` (`+containerColor = Color.Transparent` + import `Color`). 4 sisanya
diantre untuk batch berikutnya (pola identik, tidak perlu konfirmasi ulang).

**Tint (`liquidGlassAlpha`, `BlurUtils.kt`) sengaja TIDAK diturunkan** — tetap di 0.85f/0.90f
(fallback aman Batch 311) sampai user konfirmasi visual device bahwa blur sungguhan sudah tampil
benar di sheet yang sudah diperbaiki. Menurunkan sebelum konfirmasi berisiko regresi ke bug
"ghost text tembus" asli kalau fix ini ternyata belum menuntaskan seluruh gejala.

## Batch 321 — Arsip `PROJECT_STATE.md`: pindahkan Batch 58–219 ke `PROJECT_STATE_ARCHIVE.md`, 0 kode diubah
Kandidat yang dicatat (BUKAN dieksekusi) di Batch 320: `PROJECT_STATE.md` sudah menyimpan 262
batch aktif (58→320), jauh melebihi target "~100 batch terbaru" sejak Batch 158. User memilih
eksplisit lewat `ask_user_input_v0` sesi ini untuk mengeksekusi arsip ini dulu (docs only),
sebelum memutuskan item kode berikutnya.

**Aksi.** 162 entri batch (Batch 219 turun ke Batch 58, urutan descending) dipindah VERBATIM dari
`PROJECT_STATE.md` ke `PROJECT_STATE_ARCHIVE.md`, disisipkan tepat setelah header arsip di ATAS
entri Batch 57 yang sudah ada (descending tetap terjaga, 0 gap baru/duplikat — gap pre-existing
"Batch 83", yang memang tidak pernah punya entri terpisah di histori asli, dipertahankan apa
adanya sesuai ZERO-REFACTOR). `PROJECT_STATE.md` sekarang menyimpan 101 batch aktif (220→321).
Catatan pointer arsip & referensi "Batch 58 ke atas" diperbarui ke "Batch 220 ke atas" di kedua
file; header `PROJECT_STATE_ARCHIVE.md` disamakan (range + urutan descending).

**Tidak disentuh.** Section "Riwayat insiden kronologis" (permanen, ditandai "jangan dihapus") dan
seluruh section kebijakan/arsitektur permanen di `PROJECT_STATE.md` — 0 dipindah, 0 diedit. 0 file
`.kt`/kode disentuh — murni potong-tempel dokumentasi, dicek verbatim (jumlah baris archived block
sama persis sebelum/sesudah pindah, 0 teks hilang/terduplikasi). `FILE_MANIFEST.txt` tidak berubah
(188/188 — kedua file dokumentasi sudah ada sejak Batch 158).

**Item kode berikutnya masih menunggu pilihan user eksplisit**: fix blur Liquid Glass
lintas-window (`MainActivity.kt`, protected, Batch 311), atau animasi rim-glow Aurora (Batch 310)
— TIDAK dieksekusi diam-diam batch ini.

## Batch 320 — Verifikasi integritas rilis (repack tanpa laporan bug baru), 0 kode diubah
User minta "sempurnakan, repack, lalu present" tanpa laporan bug/log_fail baru spesifik. Sesuai
Fast-Track (task mikro dieksekusi langsung, audit full project dilarang tanpa instruksi), sesi ini
dibatasi ke verifikasi integritas ZIP yang sudah ada — BUKAN audit UI/UX baru.

**Cek 1 — `FILE_MANIFEST.txt` vs disk.** `diff` penuh 188 path di manifest vs 188 file fisik
hasil `find`: 0 selisih kedua arah. Manifest 100% akurat, 0 drift.

**Cek 2 — brace/paren/bracket balance seluruh `.kt` (126 file, main+test+androidTest).** 3 file
tampil "tidak seimbang" di penghitungan karakter mentah, ketiganya diverifikasi manual sebagai
false-positive, 0 bug nyata:
- `LyricsView.kt` — 1 `[`/`)` ekstra dari komentar notasi interval matematika `[start,end)`
  (sudah didokumentasikan sejak Batch 245, bukan temuan baru).
- `Type.kt` — 2 `)` ekstra murni dari prosa komentar Indonesia panjang (parenthetical remarks
  lintas-baris). Dikonfirmasi ulang: setelah baris `//` dibuang, kode-saja 45/45 paren, 0/0 brace
  — file ini genuinely cuma deklarasi `val`, 0 risiko compile.
- `LyricsParserTest.kt` — 2 `[` ekstra dari 1 string uji sengaja tanpa `]` penutup
  (`"...tanpa kurung tutup"`, muncul 2x: input `parse()` + nilai harapan `assertEquals`) — inilah
  test case yang MEMANG menguji penanganan bracket tak tertutup, bukan galat pengetikan.

**Cek 3 — file hasil Batch 318/319 spot-check.** `PlaybackService.kt` (80/80 brace, 388/388
paren, 14/14 bracket) & `NowPlayingScreen.kt` (218/218 brace, 807/807 paren, 1/1 bracket) — cocok
persis angka yang tercatat di entri Batch 318/319, konfirmasi 0 korupsi konten sejak ZIP terakhir.
`ic_notification_play_pause.xml` divalidasi ulang lewat `xmllint` — XML valid.

**Item ROADMAP_LIQUID_GLASS_REDESIGN.md TIDAK disentuh** — blur asli §3b masih menunggu instruksi
eksplisit lanjut sub-langkah berikutnya (aturan sesi #4 PROJECT_STATE.md), 0 eksekusi diam-diam.

**Hasil: 0 bug ditemukan, 0 file kode diedit.** ZIP direpack identik isi kodenya dengan Batch 319
(cuma dokumentasi VIP disinkronkan) — versi APK baru tetap otomatis dari `GITHUB_RUN_NUMBER`
begitu di-push (Versioning Lock, 0 bump manual).

## Batch 319 — Fix efek persistent tidak berlaku via kontrol eksternal setelah app di-kill + notifikasi cold-start jadi statis/universal (2 laporan user, 1 file kode + 1 drawable baru)
User kirim 2 laporan dalam 1 pesan: (1) efek persistent (mis. Kecepatan Putar) tidak berlaku
kalau app di-kill lalu musik diputar via widget/media player eksternal/notifikasi; (2) minta
tombol "Jeda" di notifikasi cold-start diganti ⏯️ custom + judul lagu dibuat statis-tapi-universal,
karena kontrol eksternal itu tidak akan pernah 100% sinkron kalau app sudah mati.

**Bug 1 — persistent state tidak sampai ke player di jalur eksternal.** `PlaybackStateStore`
sudah menyimpan repeatMode/shuffleEnabled (Batch 108) & speed (Batch 317), TAPI cuma dipulihkan
lewat `PlayerViewModel.connect()`/`resumeFromSaved()` — jalur UI yang HANYA jalan kalau app
dibuka. `PlaybackService`'s `SavedQueueItems` (dipakai bareng `restoreLastQueue()` — widget
cold-start — dan `onPlaybackResumption()` — resume dari lock screen/Android Auto/Bluetooth
setelah proses mati total) cuma pernah meneruskan `items`/`startIndex`/`startPositionMs`, 3
field lainnya diam-diam diabaikan sejak field itu ditambah ke store.

**Fix Bug 1** — `SavedQueueItems` dapat 3 field baru (`repeatMode`, `shuffleEnabled`, `speed`),
`loadSavedQueueItems()` mengisinya dari `PlaybackStateStore`. `restoreLastQueue()` set
repeat/shuffle SEBELUM `setMediaItems()` (pola sama `resumeFromSaved()` Batch 108, supaya
shuffle order berlaku sejak item pertama) + `setPlaybackSpeed()`. `onPlaybackResumption()` set
ketiganya ke `mediaSession.player` (parameter callback, instance sama yang akan menerima item
dari `completer.set()`) sebelum future itu di-complete — aman karena repeat/shuffle/speed
independen dari media item mana pun yang sedang/akan dimuat.

**Bug 2 — notifikasi cold-start "SONIX" (tombol "Jeda" + judul lagu) rentan stale.**
`buildColdStartNotification()` (`NotificationCompat` polos, BUKAN `MediaStyle` yang disinkron
Media3 otomatis — sudah didokumentasikan sejak Batch 304) cuma diperbarui dari listener
`onIsPlayingChanged` DI PROSES YANG SAMA — begitu app di-kill lalu state berubah lewat
widget/media player eksternal/notifikasi lain SELAGI placeholder ini masih tampil (jendela s/d
`MAX_HANDOFF_WAIT_MS` 8 detik), teks judul & label tombol yang sudah terlanjur terpasang tidak
pernah ikut ter-refresh.

**Fix Bug 2** (instruksi user eksplisit — berhenti mengejar sinkronisasi yang oleh desainnya
sendiri tidak akan pernah 100%, ganti ke konten STATIS TAPI UNIVERSAL): `contentText` jadi teks
tetap "Ketuk untuk membuka kontrol pemutaran" (bukan lagi judul lagu dinamis); tombol toggle jadi
1 ikon gabungan Putar/Jeda kustom baru (`ic_notification_play_pause.xml`, gaya "⏯️", dibuat khusus
utk app ini — bukan daur ulang `ic_widget_play`/`ic_widget_pause` yang sebelumnya ditukar
bergantian) + label tetap "Putar/Jeda" (bukan lagi "Jeda"/"Lanjutkan" bergantian).
`buildColdStartNotification()` disederhanakan jadi 0 parameter (`isPlaying`/`nowPlayingTitle`
dihapus dari signature, bukan dibiarkan jadi parameter mati) — 3 call site
(`startForegroundColdStartNotification()`, `updateColdStartNotification()`, listener
`onIsPlayingChanged`) disesuaikan. `updateColdStartNotification()` sendiri SENGAJA TETAP
DIPERTAHANKAN walau kini cuma me-repost konten identik — mencabut hook itu dari
`onIsPlayingChanged` sekalian di luar scope 2 laporan ini (listener itu dipakai banyak logic
lain: crossfade, shake detector, floating bubble).

**`ic_notification_play_pause.xml`** (baru) — vector 24dp, segitiga Putar + 2 batang Jeda
berdampingan, putih solid konsisten `ic_widget_*.png`/`ic_bubble_minimize.xml`. Sudut sengaja
tajam (bukan rounded) demi path sederhana yang bisa diverifikasi manual tanpa akses SDK/render
di sandbox ini — rounding bisa ditambah setelah user konfirmasi visual di device.

**Ringkasan file** — 1 file kode diubah (`PlaybackService.kt`), 1 drawable baru
(`ic_notification_play_pause.xml`), 0 dependency baru, 0 protected asset disentuh.
`FILE_MANIFEST.txt` 187→188. Brace/paren `PlaybackService.kt` seimbang utuh: 80/80 brace,
388/388 paren, 14/14 bracket.

**Belum divalidasi compile Gradle sungguhan** (0 akses Android SDK/Gradle/jaringan di sandbox
sesi ini — WAJIB cek CI run berikutnya). Risiko sintaks rendah utk Bug 1 (murni menambah field +
baris assignment ke API `Player` yang sudah dipakai identik di `PlayerViewModel.kt`). Risiko
sedang utk vector drawable baru (`pathData` belum pernah divalidasi parser Android sungguhan di
sandbox ini — sintaks path M/L/Z/H/V standar, gaya sama `ic_bubble_minimize.xml`/
`ic_bubble_tile.xml` yang sudah terbukti compile).

**Belum diverifikasi visual/device** — prioritas cek kalau user push: (1) putar lagu, set
speed≠1x + repeat/shuffle ON, force-stop app dari App Info (simulasi kill proses), tekan play di
widget, konfirmasi speed/repeat/shuffle genuinely ikut ke lagu yang diputar (bukan reset ke
default); (2) ulangi lewat resume dari lock screen/Bluetooth (bukan widget) kalau ada akses
device Bluetooth; (3) trigger notifikasi cold-start "SONIX", pastikan ikon ⏯️ baru tampil benar
(bukan kotak putih/pecah — tanda pathData salah) dan teks/tombol tidak lagi berubah-ubah
mengikuti lagu/status.

## Batch 318 — Fix teks "Fade Halus" ke-clip di dialog Pengaturan Putar (laporan user, 1 file)
User kirim screenshot: subtitle "Fade Halus" di seksi "Transisi Antar Lagu" (`SpeedDialog`, Now
Playing → ⋮ → Kecepatan) terpotong di bawah.

**Root cause** — Pola sama Batch 314-316 (`Column` fixed tanpa `verticalScroll`), tapi di lokasi
yang luput dari audit itu: `SpeedDialog` adalah `AlertDialog`, bukan `ModalBottomSheet` (scope
audit sebelumnya). Total tinggi konten (6 opsi Kecepatan + toggle Audiobook + 2 opsi Transisi
dengan subtitle panjang) melebihi tinggi slot `text` Material3 `AlertDialog`.

**Fix** — `.verticalScroll(rememberScrollState())` ditambah ke `Column` utama di
`NowPlayingScreen.kt`. Import sudah ada di file, 0 import baru. Audit ulang: tidak ada
`AlertDialog` lain di codebase dengan konten sepanjang ini.

**Ringkasan file** — 1 file kode diubah, 0 file baru. `FILE_MANIFEST.txt` tidak berubah
(187/187). Brace/paren seimbang: 218/218 brace, 807/807 paren.

**Belum divalidasi compile Gradle sungguhan** (WAJIB cek CI run berikutnya) — risiko sintaks
rendah, pola sudah dipakai identik di 5+ file lain codebase yang sama.

## Batch 317 — Kecepatan Putar sekarang persistent (laporan user, 2 file)
User minta inspeksi tab Pengaturan/Kecepatan Putar: ketahuan `setPlaybackSpeed()` cuma
`controller?.setPlaybackSpeed()` in-memory, tidak pernah ditulis/dibaca dari `PlaybackStateStore`
— reset ke 1x tiap proses di-kill. Beda dari Mode Audiobook per-lagu (`AudiobookModeStore`,
Batch 93) yang memang sudah persistent tapi cuma untuk lagu yang di-opt-in.

**Root cause & pola fix** — Sama persis Gap List #6 Batch 108 (repeat/shuffle): field `speed`
ditambah ke `PlaybackStateStore` (`SCHEMA_VERSION` 2→3, default 1.0x aman untuk state lama),
ditulis tiap `persistPlaybackState()`, dipulihkan sekali di `connect()` (controller-connect) —
bukan `resumeFromSaved()` — supaya berlaku ke lagu apa pun, bukan cuma saat lanjut queue lama.

**`PlaybackStateStore.kt`** — `SavedPlaybackState.speed`, param `speed` di `save()`, `KEY_SPEED`.

**`PlayerViewModel.kt`** — `restoreSavedSpeed()` baru (dipanggil di `connect()`).
`persistPlaybackState()` dapat param opsional `speedOverride` (default null, 8 call site lama
tidak berubah) supaya `setPlaybackSpeed()` simpan LANGSUNG nilai baru, bukan baca
`_uiState.value.playbackSpeed` yang update-nya lewat listener async. `setPlaybackSpeed()`
sekarang panggil persist tiap dipanggil (bukan nunggu tick ~5s) — speed yang diganti saat PAUSE
tetap tersimpan.

**Interaksi Mode Audiobook** — tidak bentrok: speed per-lagu (kalau opt-in) tetap override speed
global begitu lagu itu mulai diputar (urutan sudah begitu sejak Batch 93).

**Ringkasan file** — 2 file kode diubah, 0 file baru, 0 dependency baru. `FILE_MANIFEST.txt`
tidak berubah (187/187). Brace/paren seimbang: `PlaybackStateStore.kt` 10/10 brace, 48/48 paren;
`PlayerViewModel.kt` 221/221 brace, 786/786 paren.

**Belum divalidasi compile Gradle sungguhan** (WAJIB cek CI run berikutnya) — risiko sintaks
rendah, pola SharedPreferences + default-param persis dipakai di Gap List #6 (Batch 108).

## Batch 316 — Tuntaskan antrean audit Batch 314: `verticalScroll` ke 2 sheet terakhir (item antrean internal, 2 file)
Bukan laporan bug baru user — melengkapi 2 sisa dari 5 sheet yang kena pola sama (`Column` fixed
dalam `ModalBottomSheet` tanpa `verticalScroll`/`LazyColumn` jaring pengaman), ditandai "konten
pendek, risiko rendah" sejak audit Batch 314. Dengan batch ini, audit "pola tab serupa" Batch 314
SELESAI TOTAL — 0 sisa antrean.

**Root cause & pola fix** — PERSIS sama Batch 314/315: kalau total tinggi konten `Column`
melebihi tinggi sheet yang tersedia (layar pendek/gesture-nav/font sistem besar), baris paling
bawah diam-diam ke-clip alih-alih bisa digeser.

**`UpdateCheckSheet.kt`** (prioritas 4 — konten pendek di kebanyakan state, tapi state
`Available` bisa memanjang: judul + catatan rilis multi-baris + tombol) — scroll ditambah
langsung setelah `.fillMaxWidth()` (sheet ini tidak pakai `frostedGlass()`/tema Calm Retro),
sebelum `.padding(horizontal = 20.dp)`.

**`BackupRestoreSheet.kt`** (prioritas 5, terakhir — judul, deskripsi, 2 `OutlinedButton`, banner
hasil opsional) — scroll ditambah dengan pola sama. `AlertDialog` konfirmasi timpa data (di luar
`ModalBottomSheet` ini) tidak disentuh — kontennya pendek, tidak berisiko pola sama.

**Ringkasan file** — 2 file kode diubah, 0 file baru, 0 dependency baru, 0 parameter/callback
publik berubah, 0 komposable lain disentuh. `FILE_MANIFEST.txt` tidak berubah (187/187).
Brace/paren diverifikasi seimbang: `UpdateCheckSheet.kt` 25/25 brace, 72/72 paren;
`BackupRestoreSheet.kt` 33/33 brace, 94/94 paren.

**Belum divalidasi compile Gradle sungguhan** (WAJIB cek CI run berikutnya) — risiko sintaks
rendah, pola sudah dipakai identik di 5 file lain codebase yang sama.

**Status**: audit "pola tab serupa" Batch 314 (5 sheet total) TUNTAS. Tidak ada antrean lanjutan
dari topik ini.

## Batch 315 — Lanjutan audit Batch 314: `verticalScroll` ke 3 dari 5 sheet antrean (item antrean internal, 3 file)
Bukan laporan bug baru user — eksekusi antrean yang sudah ditetapkan Batch 314. Dari 5 sheet yang
kena pola sama (`Column` fixed dalam `ModalBottomSheet` tanpa `verticalScroll`/`LazyColumn` jaring
pengaman), 3 prioritas TERTINGGI dikerjakan batch ini; 2 sisanya (`UpdateCheckSheet.kt`,
`BackupRestoreSheet.kt`, konten pendek & risiko rendah) diantrekan Batch 316 karena limit
Micro-Batch 3 file kode sudah penuh.

**Root cause & pola fix** — PERSIS sama Batch 314: kalau total tinggi konten `Column` melebihi
tinggi sheet yang tersedia (layar pendek/gesture-nav/font sistem besar), baris paling bawah
diam-diam ke-clip alih-alih bisa digeser. Fix di ketiga file: tambah
`.verticalScroll(rememberScrollState())` ke modifier chain `Column` utama (+2 import per file) —
pola jaring-pengaman yang sudah dipakai di 3 tempat lain codebase yang sama
(`NowPlayingScreen.kt`, `SongInfoEditSheet.kt`), bukan pola baru.

**`EqualizerSheet.kt`** (prioritas 1 — jumlah band EQ variatif per device + 2 baris preset chip di
atasnya, risiko ke-clip tertinggi) — scroll ditambah persis setelah blok `.then(...)` tema Calm
Retro, sebelum `.padding(...)`.

**`RingtoneCutterSheet.kt`** (prioritas 2 — judul+lagu, 2 slider awal/akhir, teks durasi, 3
`DestinationChip` sejajar, catatan penyimpanan, tombol "Potong & Simpan") — scroll ditambah
sebelum `.padding(horizontal = 20.dp, vertical = 12.dp)`.

**`VisualizerSheet.kt`** (prioritas 3 — teks edukasi izin Mikrofon 4 baris + `SpectrumBars` 120dp
saat aktif) — scroll ditambah persis setelah blok `.then(...)` tema Calm Retro (shell identik
`EqualizerSheet.kt`).

**Ringkasan file** — 3 file kode diubah, 0 file baru, 0 dependency baru, 0 parameter/callback
publik berubah, 0 komposable lain di ketiga file disentuh. `FILE_MANIFEST.txt` tidak berubah
(187/187). Brace/paren diverifikasi seimbang: `EqualizerSheet.kt` 27/27 brace, 106/106 paren;
`RingtoneCutterSheet.kt` 24/24 brace, 91/91 paren; `VisualizerSheet.kt` 10/10 brace, 73/73 paren.

**Belum divalidasi compile Gradle sungguhan** (WAJIB cek CI run berikutnya) — risiko sintaks
rendah karena `verticalScroll`/`rememberScrollState` sudah dipakai identik di 3 file lain codebase
yang sama, pola bukan hal baru.

**Antrean Batch 316** — terapkan `verticalScroll` ke `UpdateCheckSheet.kt` dan
`BackupRestoreSheet.kt`, 2 sisa dari audit Batch 314.

## Batch 314 — Fix sheet "Kontrol Lanjutan" terpotong + Equalizer tidak auto re-attach ke sesi audio baru (2 laporan user, 3 file)
2 laporan terpisah dalam 1 pesan user, masing-masing juga minta audit pola serupa di codebase.

**Bug 1 — "Kontrol Lanjutan" terpotong (`NowPlayingScreen.kt`, 1 file).** `Column` dalam
`AdvancedControlsSheet` tidak pernah dibungkus `verticalScroll` — begitu total tinggi 3 seksi
(Pemutaran/Audio/Lagu) + 2 divider + slider volume + section header melebihi tinggi sheet yang
tersedia (layar pendek, gesture-nav, atau font sistem besar), baris paling bawah ("Potong Nada
Dering") diam-diam ke-clip di tepi layar, bukan bisa digeser untuk dijangkau. Fix: tambah
`.verticalScroll(rememberScrollState())` ke modifier chain Column — pola jaring-pengaman yang
sudah dipakai di body utama `NowPlayingScreen` sendiri dan `SongInfoEditSheet.kt`, bukan pola
baru. Import `verticalScroll`/`rememberScrollState` sudah ada di file ini sebelumnya, tidak ada
import baru.

**Audit "pola tab serupa"** — dicek seluruh 22 file `ui/*Sheet.kt`/`*Screen.kt` yang punya
`ModalBottomSheet`: 5 file LAIN juga punya `Column` fixed tanpa `verticalScroll` maupun
`LazyColumn` sebagai jaring pengaman — `EqualizerSheet.kt` (risiko tertinggi: jumlah band EQ
variatif per device + 2 baris preset chip), `RingtoneCutterSheet.kt`, `VisualizerSheet.kt`,
`UpdateCheckSheet.kt`, `BackupRestoreSheet.kt` (2 terakhir konten pendek, risiko rendah). Belum
disentuh batch ini (limit Micro-Batch 3 file kode sudah terisi 2 bug yang dilaporkan langsung) —
diantrekan Batch 315, urutan sesuai prioritas risiko di atas. Detail di `PROJECT_STATE.md`.

**Bug 2 — Equalizer tidak persistent (`PlaybackAudioSession.kt` + `PlayerViewModel.kt`, 2
file).** Root cause BUKAN di penyimpanan — `EqualizerController.kt` sudah benar simpan
band/preset/enabled ke SharedPreferences dan `attach()` sudah benar baca+terapkan ulang sejak
awal. Root cause ada di PEMANGGILAN `attach()`: satu-satunya call site adalah
`ensureEqualizerAttached()`, dan itu cuma terpanggil dari `onOpenEqualizer` (`MainActivity.kt`) —
efek `android.media.audiofx.Equalizer` yang NYATA memproses audio cuma ter-reattach ke sesi audio
kalau user membuka sheet Equalizer secara manual. Sesi audio baru yang terjadi lebih dulu
(cold-start app, Service restart, ExoPlayer membuat ulang AudioTrack di tengah pemutaran) tidak
pernah otomatis ter-reattach — settingan tersimpan tetap benar di prefs (kelihatan benar kalau
sheet dibuka), tapi suara yang benar-benar keluar flat tanpa EQ sampai sheet dibuka lagi. Ini
kontradiksi langsung dengan doc-comment `ensureEqualizerAttached()` sendiri yang sudah menyatakan
niatnya "must keep affecting real audio in the background regardless of whether its sheet is
open" — wiring-nya saja yang belum pernah sesuai niat itu.

Fix: `PlaybackAudioSession.kt` — tambah properti `onSessionIdChanged` + setter custom pada
`sessionId` yang meng-invoke listener itu tiap kali ID baru non-zero masuk, supaya
`PlaybackService.kt` (listener `onEvents` yang sudah ada) TIDAK perlu disentuh sama sekali.
`PlayerViewModel.kt` — daftarkan listener itu (`equalizerController.attach(id)`) di `init{}` yang
sudah ada, plus attach sekali langsung kalau sesi sudah tersedia saat ViewModel dibuat (mis.
Service masih main di background sebelum ViewModel dibuat ulang).

**Audit "pola yang sama"** — 1 controller `AudioEffect` lain di codebase ini,
`AudioVisualizerController`, TIDAK kena bug sejenis: lazy-attach-nya memang disengaja (lihat
doc-comment `ensureVisualizerAttached()` — trade-off baterai, beda niat dari Equalizer yang
memang harus selalu aktif). `SilenceSkipStore` dan `CrossfadeStore` sudah benar dibaca ulang di
`PlaybackService.onCreate()` tiap kali Service baru dibuat, tidak kena pola bug ini juga.

**Ringkasan file** — 3 file kode diubah (`PlaybackAudioSession.kt`, `PlayerViewModel.kt`,
`NowPlayingScreen.kt`), 0 file baru, 0 dependency baru, 0 perubahan struktur/urutan grup yang
sudah ada dari batch sebelumnya. Brace/paren diverifikasi seimbang tiap file yang disentuh
(`NowPlayingScreen.kt` 218/218 brace, 800/800 paren; `PlayerViewModel.kt` 220/220 brace, 772/772
paren; `PlaybackAudioSession.kt` 2/2 brace, 7/7 paren).

**Belum divalidasi compile Gradle sungguhan** (tidak ada akses Android SDK/Gradle/jaringan di
sandbox sesi ini — WAJIB cek CI run berikutnya, pelajaran yang sama dari Batch 312→313). Sudah
diverifikasi manual: nama fungsi/properti yang dipakai (`equalizerController.attach`,
`PlaybackAudioSession.sessionId`) dicek memang ada di file targetnya, dan
`verticalScroll`/`rememberScrollState` sudah diimpor+dipakai identik di tempat lain pada file
yang sama sebelum batch ini.

## Batch 313 — Fix CI build gagal: `Modifier.padding()` overload tidak valid di Batch 312 (`log_fail_305.zip` dari user, 1 file)
User upload log build CI (`build-output.log`, Gradle 8.14.3): `:app:compileDebugKotlin` &
`:app:compileReleaseKotlin` GAGAL, error tepat di `NowPlayingScreen.kt:1084:29` — kode baru
`AdvancedControlsSectionHeader` yang ditambah Batch 312. Root cause:
`Modifier.padding(horizontal = 20.dp, top = 4.dp, bottom = 4.dp)` mencampur parameter
`horizontal` (overload 2-parameter `padding(horizontal, vertical)`) dengan `top`/`bottom`
(overload 4-parameter `padding(start, top, end, bottom)`) — 2 overload berbeda, Kotlin tidak bisa
resolve kombinasi keduanya. Regresi murni dari sesi kemarin, tidak pernah lolos compile sungguhan
sebelum dikirim (dicatat eksplisit di entri Batch 312: "Belum divalidasi compile Gradle
sungguhan" — sekarang baru ketahuan lewat CI beneran).

**`NowPlayingScreen.kt`** (1 file, 1 baris) — diganti ke overload 4-parameter yang valid:
`Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 4.dp)` — hasil visual identik
(20dp kiri/kanan, 4dp atas/bawah, sama persis yang dimaksud Batch 312), cuma nama parameter yang
diperbaiki. Grep ulang seluruh file untuk pola campur `horizontal`+`top/bottom` yang sama: 0
kecocokan lain — ini satu-satunya titik yang salah dari Batch 312.

**Ringkasan file** — 1 file kode, 1 baris diubah. 0 file baru, 0 dependency baru, 0 perubahan
struktur/urutan grup (pengelompokan Pemutaran/Audio/Lagu dari Batch 312 tidak disentuh — murni bug
sintaks). `FILE_MANIFEST.txt` tidak berubah (187/187). Brace/paren `NowPlayingScreen.kt`
diverifikasi seimbang utuh: 218/218 braces, 795/795 parens.

**Masih belum divalidasi compile Gradle sungguhan** (WAJIB cek CI run berikutnya) — tapi akar
masalah sudah dikonfirmasi persis dari pesan error compiler asli, bukan tebakan; overload
`Modifier.padding(start, top, end, bottom)` sudah dipakai di tempat lain pada codebase yang sama.

**Pelajaran untuk sesi berikutnya** — komposable baru yang pakai `Modifier.padding()` dengan lebih
dari 2 parameter wajib pastikan kombinasi nama parameter itu benar ada di 1 overload yang sama,
tidak bisa dicampur lintas overload.

## Batch 312 — Rapikan sheet "Kontrol Lanjutan": kelompokkan 9 baris jadi 3 seksi berdasar kegunaan (klarifikasi user, 1 file)
Lanjutan langsung dari Batch 311. Batch 311 salah tafsir "berantakan/tidak-professional" sebagai
bug transparansi tint (sudah benar diperbaiki, TIDAK di-revert batch ini); klarifikasi user kali
ini soal hal lain: "maksud saya rapikan menu utilitas yang tidak dipisahkan berdasarkan kegunaan
umumnya" — 9 baris menu sebelumnya flat berurutan tanpa pengelompokan sama sekali.

**`NowPlayingScreen.kt`** (1 file) — `AdvancedControlsSheet` direstrukturisasi jadi 3 seksi ala
grouped-list iOS (label kecil di atas tiap grup + `HorizontalDivider` di antaranya, gaya divider
sama persis yang sudah ada sebelumnya, sekarang dipakai konsisten di antara semua seksi):
1. **Pemutaran** — Antrean Putar, Sleep Timer, Kecepatan Putar, Repeat A-B & Bookmark.
2. **Audio** — Equalizer, Visualizer Audio, + slider "Peredam Dalam Aplikasi".
3. **Lagu** — Lirik, Edit Info Lagu, Potong Nada Dering.

Composable baru `AdvancedControlsSectionHeader(title: String)` ditambah di sebelah
`AdvancedControlRow` — gaya teks disamakan persis dengan label "Peredam Dalam Aplikasi" yang
sudah ada (`labelSmall` + `secondary`). 0 icon/label/callback diubah atau dihapus — 9
`AdvancedControlRow` + 1 slider volume semua masih ada, cuma urutan & pengelompokan berubah; 9/9
callback (`onOpenQueue`...`onOpenRingtoneCutter`) diverifikasi masih terpasang tepat 1x.

**Ringkasan file** — 1 file kode, jauh di bawah batas Micro-Batch (maksimal 3). 0 file baru, 0
dependency baru, 0 signature publik berubah. `FILE_MANIFEST.txt` tidak berubah (187/187).
Brace/paren `NowPlayingScreen.kt` diverifikasi seimbang utuh: 218/218 braces, 795/795 parens.

**Belum divalidasi compile Gradle sungguhan** (WAJIB cek CI) — risiko sintaks rendah: reorder
composable yang sudah ada + 1 composable baru sangat sederhana.

**Belum diverifikasi visual di device** — harus terlihat 3 grup dengan label "Pemutaran"/
"Audio"/"Lagu" + divider di antaranya, urutan baru mengikuti pengelompokan (bukan regresi).

## Batch 311 — Fix bug: sheet "Kontrol Lanjutan" berantakan/tidak-professional (laporan screenshot user, 1 file)
User kirim screenshot `ModalBottomSheet` "Kontrol Lanjutan" (Now Playing): teks latar (coachmark
"Geser di kiri/kanan piringan buat atur kecerahan & volume HP... Ketuk ⋮ buat Sleep Timer,
Kecepatan, dan Equalizer" dari `NowPlayingScreen.kt`) tembus hampir penuh di belakang sheet,
tumpang-tindih dengan isi sheet sendiri — dilaporkan "berantakan" & "jauh dari kesan professional".

**Root cause** — `frostedGlass()` (`BlurUtils.kt`) untuk identitas Liquid Glass sudah diturunkan
bertahap ke tint sangat tipis (0.38f gelap/0.48f terang, Batch 296-299) dengan asumsi `hazeEffect`
(blur asli via Haze) akan menutupi sisanya. Asumsi itu salah khusus untuk `ModalBottomSheet`/
`Dialog`: keduanya render di Android Window terpisah dari `hazeSource` (`Box` pembungkus NavHost
di `MainActivity.kt`), sehingga capture `RenderNode` Haze tidak bisa sample lintas-window — blur
diam-diam tidak pernah menyala di SEMUA bottom sheet/dialog app-wide (12+ call site
`frostedGlass()`), menyisakan tint 0.38/0.48 itu sendiri tanpa blur di baliknya. Setiap iterasi
tuning Batch 296-299 sebelumnya mengasumsikan arah masalah "blur ketutup tint" (tint diturunkan
tiap kali) — screenshot ini membuktikan arah sebaliknya: 0 blur yang kelihatan sama sekali.

**`BlurUtils.kt`** (1 titik, 1 file) — `liquidGlassAlpha` dinaikkan `0.38f/0.48f` →
**`0.85f/0.90f`** (dekat opaque, bukan full `1f` ala Skeu — sengaja masih menyisakan sedikit
karakter glass untuk elemen yang render dalam window yang sama dengan `hazeSource`, mis.
MiniPlayerBar/card Home-Library/panel NowPlaying, yang capture-nya kemungkinan tetap sah karena 1
window sama). `blurRadius`/`edgeBrush`/pemanggilan `hazeEffect` tidak disentuh — bukan akar
masalah. `MainActivity.kt` (lokasi `hazeSource`, ada di daftar Protect) tidak disentuh — perbaikan
wiring Haze lintas-window sesungguhnya di luar scope fix 1-parameter minimal-risiko ini.

**Kenapa cuma naikkan tint, bukan re-arsitektur Haze cross-window** — STABILITY > Speed +
ZERO-REFACTOR: opsi lain menyentuh `MainActivity.kt`/`NavGraph` yang diproteksi & berisiko pecah
di 12+ call site lain yang sudah benar (MiniPlayerBar dkk, dalam window yang sama). Menaikkan 1
parameter tint adalah perubahan minimal yang pasti memperbaiki keterbacaan (monoton — makin
opaque makin sedikit bleed-through) tanpa menyentuh apa pun yang berisiko ke fitur lain.

**Ringkasan file** — 1 file kode (`BlurUtils.kt`), jauh di bawah batas Micro-Batch (maksimal 3). 0
file baru, 0 dependency baru, 0 token warna baru. `FILE_MANIFEST.txt` tidak berubah (187/187).
Brace/paren `BlurUtils.kt` diverifikasi seimbang utuh: 9/9 braces, 127/127 parens.

**Belum divalidasi compile Gradle sungguhan** (WAJIB cek CI) — risiko sintaks sangat rendah: cuma
ganti 2 literal `Float`, 0 struktur/API/branch baru.

**Belum diverifikasi visual di device** — kalau user buka lagi "Kontrol Lanjutan" (atau bottom
sheet apa pun) di identitas Liquid Glass: latar belakang seharusnya tidak lagi tembus terbaca,
sheet terlihat solid/rapi seperti 4 identitas non-glass lainnya. 5 identitas lain 0 berubah.

**Item berikutnya (belum diminta user, JANGAN dikerjakan diam-diam)**: kalau user mau blur ASLI
(bukan cuma tint opaque fallback) tetap tampak di bottom sheet/dialog Liquid Glass, perlu
investigasi wiring Haze lintas-window sesungguhnya — perubahan itu akan menyentuh
`MainActivity.kt` yang diproteksi, jadi wajib dikonfirmasi user dulu sebelum eksekusi.

## Batch 310 — Tema ke-6 "Aurora", Fase 5/N: rim-glow per-panel, wiring app-wide (lanjutan langsung, 1 file)
Lanjutan langsung dari Batch 309, permintaan user langsung: "lanjut wiring rim-glow kesemua
area!!". Menuntaskan SATU-SATUNYA item Aurora yang masih berstatus "ditunda" sejak Batch 306 —
saat itu dicatat eksplisit "rim-glow per-panel ditunda utk dipertimbangkan lagi nanti, BUKAN
dibatalkan". Dengan batch ini, cakupan efek Aurora yang dikonfirmasi user di awal (ambient
background + rim-glow per-panel) selesai penuh, menyusul color+typography+shape murni yang sudah
lengkap sejak Batch 307-309.

**Dampak nyata mulai batch ini** — setiap panel/card/sheet yang route lewat `frostedGlass()`
(MiniPlayerBar, panel NowPlaying, tiap bottom sheet, card Home/Library) saat Aurora aktif sekarang
punya rim/border ber-gradasi 4 warna lintas spektrum Aurora (hijau→teal→ungu→magenta), bukan lagi
rim flat netral generik seperti sebelumnya.

**`BlurUtils.kt`** (2 titik, 1 file) —
1. `val isAurora = isAuroraTheme()` ditambah setelah `isLiquidGlass`, pola identik dengan 3
   flag identitas lain di fungsi yang sama.
2. `edgeBrush`'s `when` dapat cabang baru `isAurora ->`: `Brush.linearGradient` 4-stop
   (`AuroraGreen`/`AuroraTeal`/`AuroraViolet`/`AuroraMagenta`, urutan hue sama persis
   `auroraGlow()`) dengan alpha menurun tiap stop (1.0x/0.85x/0.6x/0.35x dari `AuroraGlowAlpha` —
   3 multiplier pertama SAMA PERSIS `auroraGlow()`'s brush, 1 falloff tambahan 0.35x utk stop
   ke-4). 0 token warna/alpha baru ditambah ke `Color.kt` — murni reuse token Aurora yang sudah
   ada sejak Batch 306. Own branch, BUKAN jatuh ke `else` di bawah — pola sama alasan
   `isLiquidGlass` di atasnya: `else` cuma benar mendeteksi "Apple light" (perbandingan literal ke
   `AppleLightBackground`), Aurora (dark-locked permanen, `colorsFor()` sengaja mengabaikan
   `isDark`) akan diam-diam kebagian rim flat netral `onSurface` alih-alih warna khas
   identitasnya sendiri kalau tidak dipisah eksplisit.

**Kenapa `frostedGlass()`, bukan sentuh tiap screen 1-per-1** — fungsi ini adalah SATU titik
shared yang dilalui SEMUA panel glass app-wide (MiniPlayerBar, tiap bottom sheet, card
Home/Library, panel NowPlaying — grep 12+ call site, precedent Batch 281 Liquid Glass) — 1 branch
di titik ini otomatis mewujudkan permintaan user "kesemua area" tanpa perlu menyentuh 1-per-1
file screen, arsitektur identik cara Liquid Glass dapat edge-glow terpusat di Batch 281.

**Keputusan disengaja: statis, BUKAN animated** — beda dari `auroraGlow()` (1 instance
`rememberInfiniteTransition` di root Surface `MainActivity.kt`), rim ini dipasang di titik yang
dipanggil 12+ kali sekaligus per komposisi — 12+ infinite transition independen serentak adalah
biaya performa baru yang belum pernah diverifikasi device (project ini sudah pernah kena masalah
stutter blur Liquid Glass, lihat Batch 300). Statis dulu sebagai titik awal paling aman
(STABILITY > Speed) — animasi jadi kandidat lanjutan kalau user minta setelah versi statis ini
terverifikasi visual dulu.

**Tidak dibedakan `isDark`** (beda dari `isTactile`/`isLiquidGlass` di branch yang sama) — Aurora
cuma punya 1 mode terkunci gelap permanen, cabang `isDark` di sini justru berisiko salah pilih
kalau toggle sistem user kebetulan "terang" walau skema warna yang dipakai tetap dipaksa gelap —
alasan yang sama persis kenapa CalmRetro juga tidak dibedakan `isDark` di titik yang sama.

**Ringkasan file** — 1 file kode (`BlurUtils.kt`), jauh di bawah batas Micro-Batch (maksimal 3). 0
file baru, 0 dependency baru, 0 token warna/alpha baru. `FILE_MANIFEST.txt` tidak berubah
(187/187). Brace/paren `BlurUtils.kt` diverifikasi seimbang utuh: 9/9 braces, 117/117 parens.

**Belum divalidasi compile Gradle sungguhan** (WAJIB cek CI) — risiko sintaks rendah:
`Brush.linearGradient(colors = listOf(...))` adalah konstruktor yang sudah dipakai berulang kali
di file yang sama (branch `isTactile`/`isLiquidGlass`/`else` di atas/bawahnya), `isAuroraTheme()`
sudah ada & terbukti compile sejak Batch 308, 0 API baru dipakai.

**Belum diverifikasi visual di device** — kalau user coba pilih Aurora sekarang: MiniPlayerBar/
panel NowPlaying/bottom sheet/card Home-Library harus terlihat punya rim tipis ber-gradasi
hijau→teal→ungu→magenta di tepinya, BUKAN lagi garis flat netral seperti sebelumnya. 5 identitas
lain harus 0 berubah.

**🎉 Cakupan Aurora yang dikonfirmasi user di awal (Batch 306) sekarang selesai penuh** — mode
gelap terkunci (Batch 306), ambient background `auroraGlow()` (mekanisme Batch 306, wired ke root
Surface Batch 308), color/typography/shape murni sendiri (Batch 307/308/309), rim-glow per-panel
wired app-wide (batch ini).

**Item berikutnya (belum diminta user, JANGAN dikerjakan diam-diam)** — animasi rim-glow (lihat
"Keputusan disengaja: statis" di atas), atau tuning alpha/warna rim setelah terverifikasi visual
di device sungguhan.

## Batch 309 — Tema ke-6 "Aurora", Fase 4/N: shape sendiri `AuroraShapes` (lanjutan langsung, 1 file)
Lanjutan langsung dari Batch 308, permintaan user langsung: "lanjut sempurnakan shape murni
nya!!". Menuntaskan item terakhir yang dicatat "belum diminta user" di entri Batch 308. Dengan
batch ini ke-6 identitas app punya color+typography+shape murni sendiri — Aurora menyusul 5
identitas lain yang sudah lengkap sebelumnya (color Batch 307, typography Batch 308).

**Dampak nyata mulai batch ini** — setiap panel/card/sheet yang pakai `Shapes.small/medium/large`
M3 saat Aurora aktif sekarang bersudut ASIMETRIS: 2 sudut diagonal (kiri-atas & kanan-bawah)
lebih besar dari 2 sudut lainnya (kanan-atas & kiri-bawah), bukan rounded-rect seragam seperti 5
identitas lain. Ini pola shape PERTAMA di seluruh project yang non-seragam per sudut.

**`Theme.kt`** (2 titik, 1 file) —
1. `AuroraShapes` ditambah setelah `LiquidGlassShapes`. Mekanisme: `RoundedCornerShape(topStart,
   topEnd, bottomEnd, bottomStart)` 4-parameter (bukan 1-parameter seragam yang dipakai 5
   `*Shapes` lain di file ini) — asimetri ditarik LANGSUNG dari arah diagonal default
   `Brush.linearGradient()` di `auroraGlow()` (`TactileDepth.kt`): tanpa parameter `start`/`end`
   eksplisit di situ, Compose menggambar gradient itu diagonal dari pojok kiri-atas ke
   kanan-bawah (`topStart`→`bottomEnd`) — jadi sudut yang searah diagonal itu dapat radius lebih
   besar (menekankan arah alirnya), sudut yang tegak lurus arah alir dapat radius lebih kecil.
   0 token `Radius` baru ditambah ke `Spacing.kt` — nilai "besar" tiap tier (`Radius.xl`/`xxxl`/
   `liquidLg`) SENGAJA disamakan persis dgn puncak radius seragam `LiquidGlassShapes` (bukan
   melebihinya) — keunikan Aurora murni dari asimetrinya, bukan dari mengejar rekor angka radius
   baru, konsisten dgn semangat "100% ide sendiri" yang sama dipakai `AuroraColors`/
   `AuroraTypography` sebelumnya.
2. `shapes = when (identity)` di `AudioPlayerTheme()` dapat cabang baru `ThemeIdentity.AURORA ->
   AuroraShapes`. Comment di atas `typography`/`shapes` diperbarui: `else -> AppleShapes` sekarang
   murni cabang APPLE saja — satu-satunya identitas tanpa cabang eksplisit di typography maupun
   shapes.

**Ringkasan file** — 1 file kode (`Theme.kt`), jauh di bawah batas Micro-Batch (maksimal 3). 0
file baru, 0 dependency baru, 0 token `Radius` baru. `FILE_MANIFEST.txt` tidak berubah (187/187).
Brace/paren `Theme.kt` diverifikasi seimbang utuh: 14/14 braces, 198/198 parens.

**Belum divalidasi compile Gradle sungguhan** (WAJIB cek CI) — risiko sintaks rendah
(`RoundedCornerShape` 4-parameter adalah konstruktor resmi Compose, type-compatible penuh dgn
`CornerBasedShape`/`Shapes` M3, 0 call site di luar `Theme.kt` berubah). **Risiko VISUAL lebih
tinggi dari batch shape sebelumnya** — ini genuinely mekanisme baru (asimetri per-sudut), bukan
cuma tuning angka radius seragam yang sudah terbukti aman di 5 identitas lain — **belum
diverifikasi di device sungguhan**: kalau dicoba sekarang, Card/Sheet/dialog saat Aurora aktif
harus terlihat bersudut "condong" (2 sudut diagonal lebih membulat), bukan rounded rect simetris
biasa. 5 identitas lain harus 0 berubah.

**Item berikutnya (belum diminta user)** — rim-glow per-panel (status "ditunda" sejak Batch 306,
bukan dibatalkan), satu-satunya sisa item Aurora dari cakupan awal yang dikonfirmasi user.

## Batch 308 — Tema ke-6 "Aurora", Fase 3/N: wiring `auroraGlow()` + typography sendiri (lanjutan langsung, 3 file)
Lanjutan langsung dari Batch 307, permintaan user langsung: "lanjutkan fase 3/N, sempurnakan
juga typography-nya". 2 hal dikerjakan sekaligus: (1) item yang sudah tercatat sejak Batch 307
sebagai "Fase 3, BELUM dikerjakan" — wiring `auroraGlow()` ke root Surface; (2) item tambahan
yang eksplisit diminta user batch ini — typography sendiri untuk Aurora, sebelumnya cuma dicatat
"kalau diminta nanti".

**Dampak nyata mulai batch ini** — pilih Aurora di Settings sekarang menampilkan animasi warna
mengalir pelan (hijau→teal→ungu→magenta, siklus ~40 detik penuh, bolak-balik halus) di ambient
background, bukan lagi flat statis seperti Batch 306-307. Judul & label juga sudah pakai
`AuroraTypography` sendiri, bukan lagi fallback `AppleTypography` — bobot huruf paling ringan
dari 6 identitas (Light/Normal, 0 slot Bold-tier sama sekali), letter-spacing paling terbuka
(kicker `labelSmall` 1.4sp, rekor terlebar melewati Calm Retro 1.2sp), line-height paling
longgar. Shape MASIH fallback `AppleShapes` — user cuma minta typography batch ini, bukan shape.

**`MainActivity.kt`** (protected/parsial, 2 titik) —
1. Import `com.rudi.audioplayer.ui.theme.auroraGlow` ditambah setelah import `calmGrain`.
2. 1 `.then()` baru ditambahkan setelah blok `.then()` `calmGrain()` yang sudah ada di modifier
   root `Surface` — pola arsitektur identik (aktif hanya saat `appThemeIdentity ==
   ThemeIdentity.AURORA`, `Modifier` polos untuk identitas lain). `auroraGlow()` sendiri sudah
   lengkap sejak Batch 306 (fase 1) — batch ini murni titik pemanggilannya, 0 logika baru di
   fungsi itu sendiri (`TactileDepth.kt` 0 baris disentuh).

**`Theme.kt`** (2 titik) —
1. `isAuroraTheme(): Boolean = MaterialTheme.colorScheme.primary == AuroraAccent` ditambah
   setelah `isLiquidGlassTheme()` — helper ke-5, pola identik 4 pendahulunya (Tactile/Skeu/
   CalmRetro/LiquidGlass), belum ada call site selain persiapan untuk `MainActivity.kt`.
2. `typography = when (identity)` di `AudioPlayerTheme()` dapat 1 cabang baru:
   `ThemeIdentity.AURORA -> AuroraTypography`. `shapes = when (identity)` di bawahnya SENGAJA
   TIDAK disentuh — comment ditambah menjelaskan ini keputusan sadar (scope permintaan user cuma
   typography), bukan celah yang terlewat, supaya sesi berikutnya tidak salah asumsi.

**`Type.kt`** (1 definisi baru, `AuroraTypography`) — 5 slot (titleLarge/titleMedium/bodyMedium/
bodySmall/labelSmall), `fontSize`/`fontFamily` identik ke 5 identitas lain (28/17/15/13/11sp,
`FontFamily.Default` — larangan Monospace Batch 133 §4 tetap berlaku, ukuran tidak diubah supaya
0 resiko reflow layout, batasan sama yang dipatuhi Batch 279/298/302/305). 3 sumbu pembeda,
ditarik langsung dari mekanisme `auroraGlow()` & spec identitas ini sendiri (rasional penuh di
comment block `Type.kt`, bukan angka acak):
1. **Weight** — `titleLarge`=`Light`, `titleMedium`=`Normal`: SATU-SATUNYA dari 6 identitas
   dengan 0 slot menyentuh tier Bold/SemiBold/ExtraBold di seluruh scale-nya (SkeuTypography,
   identitas paling ringan sebelumnya, masih `SemiBold` di `titleLarge`). `labelSmall` tetap
   `Medium` — kicker tetap butuh sedikit bobot supaya kebaca di atas `AuroraSurfaceVariant` gelap.
2. **Letter spacing** — positif/terbuka di semua 5 slot (arah sama CalmRetro/LiquidGlass, alasan
   beda: meniru cahaya berdifusi, bukan tracking cetak vintage atau kelapangan CONVX).
   `labelSmall` 1.4sp jadi rekor terlebar dari 6 identitas, melewati CalmRetro 1.2sp.
3. **Line height** — dilonggarkan dari baseline Apple (arah sama CalmRetro/Skeu), alasan beda:
   ruang vertikal supaya animasi `auroraGlow()` "mengalir pelan" tidak terasa terpotong baris teks.

**Pemeriksaan sebelum eksekusi** — grep ulang seluruh app untuk `when (identity)`/
`when (appThemeIdentity)`: `colorsFor()` (exhaustive, sudah lengkap sejak Batch 307, 0 disentuh
batch ini) tetap satu-satunya yang WAJIB exhaustive. `typography`/`shapes` (`Theme.kt`),
`identityRootBrush`/`navCatchLightColor` (`MainActivity.kt`), `ThemeOptionCard`
(`SettingsScreen.kt`) semuanya `else`/`==` biasa — 0 risiko compile break dari cabang yang
ditambahkan batch ini.

**Ringkasan file** — 3 file kode (`MainActivity.kt`, `Theme.kt`, `Type.kt`), pas di batas
Micro-Batch (maksimal 3). 0 file baru, 0 dependency baru, `FILE_MANIFEST.txt` tidak berubah
(187/187). Brace/paren diverifikasi seimbang per-file: `Theme.kt` 179/179 parens + 14/14 braces,
`MainActivity.kt` 617/617 parens + 256/256 braces, `Type.kt` 45/45 parens kode-saja (komentar
prosa dikecualikan dari hitungan — wajar tidak simetris seperti file lain di project ini).

**Belum divalidasi compile Gradle sungguhan** (WAJIB cek CI) dan **belum diverifikasi visual di
device** — kalau dicoba sekarang, pilih Aurora di Settings harus menampilkan ambient background
yang mengalir pelan (bukan flat lagi) dengan judul/label yang terasa lebih ringan/lapang
dibanding 5 tema lain; 5 identitas lain harus 0 berubah.

**Item berikutnya (belum diminta user)** — shape Aurora sendiri (masih fallback `AppleShapes`),
rim-glow per-panel (status "ditunda" sejak Batch 306, bukan dibatalkan).

## Batch 307 — Tema ke-6 "Aurora", Fase 2/N: registrasi identitas + palet lengkap (lanjutan langsung, 2 file)
Lanjutan langsung dari Batch 306 (instruksi "next"). Fase 2 sesuai rencana yang sudah dicatat di
Batch 306: `ThemeIdentity.AURORA` resmi didaftarkan ke enum + `AuroraColors` (`darkColorScheme`)
lengkap dibangun + cabang baru di `colorsFor()`. Ketiganya WAJIB dikerjakan sekaligus dalam 1
batch (bukan dicicil lebih jauh) karena `colorsFor()` di `Theme.kt` pakai `when` **exhaustive**
tanpa `else` — Kotlin memaksa semua cabang enum terisi begitu 1 entry baru ditambahkan, atau
build gagal compile total.

**Dampak nyata mulai batch ini — Aurora SUDAH BISA DIPILIH** di picker tema (`SettingsScreen.kt`)
— pemilih itu mengiterasi `ThemeIdentity.entries.toList()` secara generik, jadi 0 baris di file
itu perlu disentuh supaya opsi baru otomatis muncul. Yang akan terlihat kalau dipilih sekarang:
warna dark-lock milik sendiri (aksen hijau vivid di atas latar near-black night-navy), TAPI
**animasi `auroraGlow()` (Batch 306) belum terpasang di mana pun** — layar akan tampak flat
gelap + aksen hijau statis, bukan mengalir, sampai Fase 3 (wiring ke root Surface
`MainActivity.kt`). Typography & shapes Aurora untuk saat ini jatuh ke `else -> AppleTypography`
/`else -> AppleShapes` di dispatcher — pola bootstrap yang sama persis dijalani semua identitas
lain sebelum dimurnikan (Skeu: Batch 57 → dimurnikan Batch 305; Calm Retro: Batch 130 →
dimurnikan Batch 302) — bukan sesuatu yang lupa dikerjakan, memang urutan yang disengaja.

**`Color.kt`** (5 token baru, ditambah setelah blok Aurora Batch 306) — `AuroraBackground`
(`0xFF05080C`), `AuroraSurface` (`0xFF0B1015`), `AuroraSurfaceVariant` (`0xFF161D22`),
`AuroraText` (`0xFFE7F3EC` — nyaris putih dengan sentuhan hijau-dingin sangat tipis, beda dari
putih murni `LiquidGlassDarkText` `0xF3F4F8` maupun abu-terang `CalmRetroText` `0xE2E4E9`),
`AuroraSecondaryText` (`0xFF7E8C90`). Hex base near-black sengaja beda dari `AmoledSurface`/
`CalmRetroBackground`/`LiquidGlassDarkBackground` yang sudah ada — kemiripan wajar karena semua
"near-black", pembeda identitas sesungguhnya tetap di overlay animasi `auroraGlow()` (Batch
306), bukan di token statis ini.

**`Theme.kt`** (3 titik dalam 1 file) —
1. Entry enum baru `AURORA("aurora", "Aurora", "Cahaya aurora borealis mengalir pelan di
   ambient, aksen hijau-teal-ungu-magenta — selalu gelap, tidak mengikuti toggle Mode")`
   ditambahkan setelah `LIQUID_GLASS`, dengan comment block menjelaskan histori Batch 306/307.
2. `private val AuroraColors = darkColorScheme(...)` ditambahkan setelah `LiquidGlassLightColors`
   — 1 set warna saja (bukan pasangan Dark/Light seperti Tactile/Skeu/LiquidGlass), karena
   dikonfirmasi user terkunci gelap permanen (pola identik `CalmRetroColors`). Setiap role
   diturunkan dari token Aurora sendiri: `primary`=`AuroraAccent`, `tertiary`=`AuroraTeal`
   (`onPrimary`/`onTertiary` = `Color.Black` — luma `AuroraAccent` (`#3DE8A0`) ≈0.75 dan
   `AuroraTeal` (`#2BC9C9`) ≈0.66, keduanya jauh di atas ambang 0.55 yang sudah dipakai
   identitas lain di file ini utk keputusan hitam-vs-putih). `error` = `AuroraMagenta` — SENGAJA
   derivasi dari palet Aurora sendiri, BUKAN reuse token identitas lain maupun hardcode merah
   generik seperti `LiquidGlassDarkColors` di atasnya — ini menerapkan pelajaran eksplisit Batch
   130 ("100% derivasi dari palet [identitas] sendiri" ketimbang meminjam warna asing).
3. `colorsFor()` dapat 1 cabang baru: `ThemeIdentity.AURORA -> AuroraColors`, dengan comment
   menjelaskan `isDark` sengaja diabaikan (pola sama `CALM_RETRO` di atasnya).

**Pemeriksaan exhaustiveness dilakukan eksplisit sebelum eksekusi, bukan diasumsikan aman** —
grep seluruh app untuk setiap `when (identity)`/`when (appThemeIdentity)`: hasilnya cuma
`colorsFor()` yang exhaustive tanpa `else` (satu-satunya yang WAJIB disentuh). Dispatcher
`typography`/`shapes` (`Theme.kt`), `identityRootBrush`/`navCatchLightColor`
(`MainActivity.kt`), dan `ThemeOptionCard` (`SettingsScreen.kt`) semuanya memakai `else`/`==`
biasa (bukan exhaustive `when`) — 0 file itu disentuh batch ini, 0 risiko compile break, Aurora
otomatis jatuh ke cabang fallback yang aman di semuanya (Apple typography/shapes, `null` root
brush/nav catch-light, kartu preview flat generik).

**Ringkasan file**: 2 file kode (`Color.kt` + `Theme.kt`), di bawah batas Micro-Batch (maks 3
file kode). 0 file baru, 0 dependency Gradle baru. `FILE_MANIFEST.txt` tidak berubah (187/187).
0 protected asset disentuh — `MainActivity.kt` dan `SettingsScreen.kt` BELUM disentuh sama
sekali batch ini (keduanya "otomatis benar" lewat fallback generik di atas, bukan lewat edit).
Brace/paren diverifikasi seimbang penuh per-file (`Color.kt`: 243/243 parens, 0/0 braces;
`Theme.kt` utuh: 14/14 braces, 168/168 parens).

**Belum divalidasi compile Gradle sungguhan** (0 akses jaringan sesi ini) — **WAJIB cek CI
setelah push**, risiko sedikit lebih tinggi dari Batch 306: menambah entry ke `when` exhaustive
adalah salah satu titik paling gampang salah di Kotlin (1 cabang lupa terisi = compile error
TOTAL di seluruh modul, bukan cuma 1 fitur rusak) — sudah diperiksa manual lewat grep di atas,
tapi tetap wajib dikonfirmasi CI, bukan diasumsikan aman hanya karena "kelihatan benar secara
manual".

**Belum diverifikasi visual di device** — kalau user coba pilih Aurora di Settings sekarang:
swatch harus tampil bulatan hijau vivid (`AuroraAccent`) di atas lingkaran latar near-black
navy-teal (`AuroraBackground`), **tanpa animasi apa pun** (statis, itu memang belum dipasang —
bukan bug/regresi). 5 identitas lain (Apple/Tactile/Skeu/CalmRetro/LiquidGlass) harus 0 berubah
sama sekali dari sebelum batch ini.

**Item berikutnya (Fase 3, BELUM dikerjakan)**: helper `isAuroraTheme()` (pola sama
`isLiquidGlassTheme()` dkk), wiring `auroraGlow()` ke root Surface `MainActivity.kt`
(protected/parsial — target diagnostik 1 baris tambahan, pola sama `calmGrain()` yang sudah ada
di sana), typography/shape sendiri kalau/ketika diminta (boleh tetap reuse Apple selama belum
diminta, pola sama semua identitas lain dulu). Rim-glow per-panel: masih ditunda sesuai
keputusan user di Batch 306.

## Batch 306 — Tema ke-6 "Aurora", Fase 1/N: fondasi mekanisme + palet (permintaan user langsung, 2 file)
User minta eksplisit: "bikin theme ke-6, tapi murni 100% karya hasil ide sendiri tanpa contek
gaya desain visual apapun". Sebelum kode ditulis, 3+3 konsep orisinal dipitch (Ink Wash,
Paper-fold, Circuit Trace, lalu setelah user minta "beda lagi/gabung": Woven, Contour, Aurora) —
user pilih **Aurora**. 2 keputusan arsitektur dikonfirmasi lewat tool tap-pilih sebelum eksekusi:
mode terkunci **gelap permanen**, dan cakupan efek **ambient background saja dulu** (rim-glow
per-panel ditunda, dipertimbangkan lagi setelah fondasi ini terverifikasi).

**Kenapa dipecah fase, bukan langsung sekali jadi**: nambah identitas baru itu sekelas Liquid
Glass dulu (Batch 279 dst), bukan micro-task. `colorsFor()` di `Theme.kt` pakai `when`
**exhaustive** — begitu 1 entry `ThemeIdentity` baru ditambah, semua cabang warna wajib terisi
sekaligus. Jadi urutan aman (pola sama histori Liquid Glass: token dulu fase 1, `enum`+dispatch
fase 2, helper `isXTheme()`+wiring UI fase 3+) adalah bangun mekanisme+token dulu secara
terisolasi (0 pemakaian di luar file definisi, 0 perubahan visual), baru daftarkan identitasnya
di fase berikutnya.

**Mekanisme baru, genuinely orisinal** — bukan reuse `skeuEmboss()`/`tactileEmboss()` (dual
shadow/bevel), `calmScanlines()`/`calmGrain()`/`calmAberration()` (artefak retro CRT), atau
`hazeEffect()` (blur asli Haze): Aurora dapat kedalaman dari **warna yang mengalir** (animated
hue-shift antar 4 warna aurora), mekanisme yang belum pernah dipakai di app ini sebelumnya.

**`TactileDepth.kt`** (1 fungsi baru, `Modifier.auroraGlow()`, ditambah setelah `calmGrain()` di
akhir file) — 5 titik stop gradien linear TETAP di posisi (0.00/0.22/0.48/0.74/1.00), sengaja
BUKAN posisi yang digeser (menggeser fraction stop berisiko 2 stop bertabrakan tepat di
ujung 0f/1f — red flag rendering gradient). 3 stop tengah warnanya di-`lerp()` (Compose
`androidx.compose.ui.graphics.lerp`) antar 2 hue aurora yang bersebelahan (hijau→teal,
teal→ungu, ungu→magenta) seiring sebuah `phase` float 0f↔1f dari `rememberInfiniteTransition` +
`animateFloat` (`RepeatMode.Reverse`, `tween(20000ms, LinearEasing)` — bolak-balik halus ~40
detik/siklus penuh, bukan `Restart` yang lompat patah di ujung siklus). Stop pertama & terakhir
tetap `Color.Transparent` permanen supaya wash berbaur ke tepi kanvas, bukan kotak warna
bertepi tegas. Pola animasi (`rememberInfiniteTransition`/`animateFloat`/`infiniteRepeatable`/
`tween`) di-copy 1:1 dari `ShimmerBrush()` (`LibraryScreen.kt`) — sudah terbukti compile+jalan
di app ini, BUKAN API baru yang belum pernah diuji di codebase ini. 0 call site memanggil fungsi
ini sampai batch ini (murni definisi, sesuai fase 1). 8 import baru: `LinearEasing`,
`RepeatMode`, `animateFloat`, `infiniteRepeatable`, `rememberInfiniteTransition`, `tween` (semua
`androidx.compose.animation.core`) + `androidx.compose.ui.graphics.lerp`;
`androidx.compose.foundation.background` sudah diimpor sebelumnya (dipakai `this.background(brush)`
di baris terakhir fungsi).

**`Color.kt`** (6 token baru, akhir file) — `AuroraAccent`/`AuroraGreen` (alias, hijau vivid
`0xFF3DE8A0` — calon role `primary` & dasar `isAuroraTheme()` di fase registrasi nanti, pola
sama `TactileAccent`/`SkeuAccent`/`CalmRetroAccent`/`LiquidGlassAccent`), `AuroraTeal`
(`0xFF2BC9C9`), `AuroraViolet` (`0xFF7C6FE0`), `AuroraMagenta` (`0xFFD46FC7`),
`AuroraGlowAlpha` (`0.34f`). 4 hue ditarik langsung dari spektrum aurora borealis asli (hijau =
warna aurora paling umum/dominan → teal → ungu → magenta-pink di aurora kuat), sengaja dijaga
beda dari 5 aksen tema lain yang sudah ada: hijau di sini jauh lebih vivid/saturated drpd
`CalmRetroAccent` (Muted Sage, sengaja pudar/lo-fi by design), ungu di sini beda hue dari
`TactileAccent` (biru-ungu `0x6670FF`) maupun `LiquidGlassAccent` (ungu-violet lebih dingin
`0x8E7CFF`) — dipakai HANYA sbg ingredient gradien alpha-rendah (bukan warna solid dominan di
UI manapun), jadi kemiripan hue longgar itu 0 resiko tabrakan visual langsung dgn 2 aksen
tersebut. `AuroraGlowAlpha` (0.34f) eksplisit ditandai "titik awal" — akan perlu dituning ulang
begitu tampil di device sungguhan, pola sama semua tuning ambient/blur lain di file ini
(`MidnightBlueAmbientAlpha`/`SkeuAmbientAlphaDark`/`liquidGlassAlpha`, lihat histori Batch
296/298/299/300 di atas — semuanya juga "titik awal" pas pertama ditambah, lalu direvisi
berdasar feedback device sungguhan, bukan ditebak sekali jadi final).

**Ringkasan file**: 2 file kode (`TactileDepth.kt` + `Color.kt`), di bawah batas Micro-Batch
(maks 3 file kode). 0 file baru dibuat, 0 dependency Gradle baru (semua API yang dipakai —
`rememberInfiniteTransition`, `animateFloat`, `lerp`, dst — sudah tersedia lewat Compose BOM yang
sudah dipakai app ini, sama seperti `ShimmerBrush()` yang sudah lama compile bersih). 0 protected
asset disentuh — `Theme.kt` dan `MainActivity.kt` BELUM disentuh SAMA SEKALI di batch ini (fase
berikutnya). `FILE_MANIFEST.txt` tidak berubah (187/187, tidak ada file baru). Brace/paren
diverifikasi seimbang penuh per-file (`Color.kt`: 231/231 parens, 0/0 braces — murni deklarasi
`val`; `TactileDepth.kt` utuh: 33/33 braces, 275/275 parens).

**Belum divalidasi compile Gradle sungguhan** (0 akses jaringan sesi ini, pola sama tiap batch)
— **WAJIB cek CI setelah push**. Risiko sedikit lebih tinggi dari batch typography murni
(Batch 302/305) krn ini fungsi BARU dgn state animasi (bukan cuma data `TextStyle`/`Typography`
statis), tapi seluruh pola animasinya di-copy 1:1 dari `ShimmerBrush()` yang sudah terbukti
compile+jalan di app ini sejak lama — risiko tetap rendah-menengah, bukan API yang benar-benar
belum teruji di codebase ini.

**Belum ada apa pun yang terlihat di device** — fungsi ini genuinely 0 dipanggil dari mana pun
sampai batch ini (pola sengaja, bukan lupa). Tidak ada langkah verifikasi visual utk batch ini;
verifikasi baru relevan mulai Fase 3 setelah `auroraGlow()` benar-benar dipasang ke root Surface.

**Item berikutnya (Fase 2, BELUM dikerjakan)**: daftarkan `ThemeIdentity.AURORA` ke enum +
token warna tambahan (`AuroraBackground`/`AuroraSurface`/`AuroraText` dkk) + `AuroraColors =
darkColorScheme(...)` (`Theme.kt`) + wire cabang baru di `colorsFor()` — wajib sekaligus krn
`when` exhaustive. **Fase 3+ (belum)**: helper `isAuroraTheme()`, wiring `auroraGlow()` ke root
Surface `MainActivity.kt` (protected/parsial — target diagnostik: 1 baris tambahan pola
`.then(if (appThemeIdentity == ThemeIdentity.AURORA) Modifier.auroraGlow() else Modifier)`,
sama shape dgn `calmGrain()` yang sudah ada di sana), typography/shape (boleh mulai reuse Apple
dulu seperti tema lain dulu, dimurnikan belakangan — pola sama Skeu/Calm Retro dulu), deskripsi
+ entry picker `SettingsScreen.kt`. Rim-glow per-panel: **ditunda**, dipertimbangkan lagi setelah
fondasi ambient ini terverifikasi di device sungguhan.

## Batch 305 — Perkuat typography khusus tema Neumorphism, murni 100% (permintaan user langsung, 2 file)
User minta eksplisit: "sempurnakan typography Neumorphism 100% murni, tuntas!!" — melanjutkan
pola penguatan typography per-tema (Batch 298 melakukan ini untuk Liquid Glass, Batch 302 untuk
Calm Retro), sekarang giliran Neumorphism (`SKEU_DARK_LITE`), dengan penekanan "murni" (bukan
pinjaman dari identitas lain) dan "tuntas" (menutup gap terakhir yang tersisa).

**Konteks historis yang relevan**: sejak Batch 57, Skeu (waktu itu masih bernama Skeuomorphism,
di-upgrade jadi "Neumorphism" Batch 79) sengaja dibiarkan reuse `AppleTypography` — alasan
eksplisit ketika itu: "no separate type-scale spec supplied for this theme; skeuomorphic identity
here is carried by color/shape/bevel, not custom type". Batch 302 lalu memurnikan Calm Retro
(typography sendiri, bukan pinjaman Apple lagi) tapi SENGAJA tidak menyentuh Skeu — user waktu
itu cuma minta Calm Retro secara eksplisit. Jadi sampai batch ini, Skeu adalah SATU-SATUNYA dari
5 identitas yang masih 100% reuse `AppleTypography` lewat cabang `else` di dispatcher `Theme.kt`.
Permintaan user sekarang menutup gap terakhir itu — sesudah batch ini, ke-5 identitas
(Apple/Tactile/Skeu/CalmRetro/LiquidGlass) semuanya punya `Typography()` murni sendiri, 0 yang
tersisa jatuh ke `else` selain Apple sendiri.

**`Type.kt`** (1 titik baru, additif) — `SkeuTypography` ditambah di akhir file, mengisi 5 slot
yang sama seperti `AppleTypography`/`TactileTypography`/`CalmRetroTypography`
(`titleLarge`/`titleMedium`/`bodyMedium`/`bodySmall`/`labelSmall`), bukan menambah slot M3 baru
(pola sama Batch 302 — 5 slot itu SUDAH terdefinisi lewat reuse Apple, jadi ini murni mengganti
isinya, bukan menambal celah seperti kasus Liquid Glass Batch 298).

Nilai baru dipilih berdasar 3 prinsip pembeda yang ditarik LANGSUNG dari mekanisme `skeuEmboss()`
(`TactileDepth.kt`) dan spec identitas ini sendiri (Batch 79 — dual soft-shadow multi-layer, 0
border/0 grain, panel "dipahat dari material yang sama dengan kanvas", pressed=concave):
1. **Weight satu tingkat lebih RINGAN dari Apple** di tiap slot berjenjang (`titleLarge`
   Bold→SemiBold, `titleMedium` SemiBold→Medium, `labelSmall` SemiBold→Medium; `bodyMedium`/
   `bodySmall` tetap Normal — sudah di tier paling ringan) — kebalikan Tactile yang justru naik
   ke ExtraBold/Bold ("machined label" fisik ditempa keras). Filosofi `skeuEmboss()`: kedalaman
   MURNI dari dual soft-shadow, 0 border/0 grain — bukan dari kontras tinta tebal. Huruf berat
   akan terbaca seperti cetakan tinta DI ATAS permukaan (metafora Apple/Tactile/CalmRetro/
   LiquidGlass, yang semuanya sama atau lebih berat dari baseline Apple), bertentangan dengan
   "molded" (dipahat dari material yang sama), bukan "printed", yang jadi identitas visual Skeu.
   Neumorphism jadi SATU-SATUNYA dari 5 identitas yang lebih ringan dari baseline Apple — sumbu
   berat jadi eksklusif milik Skeu, tidak tumpang tindih Tactile di ujung berlawanan.
2. **letterSpacing DATAR 0.sp di semua 5 slot** — tidak ada dorongan gaya tracking sama sekali,
   beda dari 4 identitas lain yang semuanya punya arah tracking sendiri (Apple negatif/rapat ala
   iOS, CalmRetro positif/lebar ala label cetak vintage, LiquidGlass positif/terbuka ala CONVX,
   Tactile netral tapi dibedakan lewat weight bukan tracking). Ini perpanjangan LANGSUNG dari ciri
   paling literal identitas ini: "0 border, 0 tekstur grain — kedalaman murni dari bayangan"
   (README/Batch 79) — kalau permukaan sengaja dilucuti dari semua gaya selain bayangan, huruf
   ikut dilucuti dari gaya tracking; definisi datang murni dari `skeuEmboss()` di sekitarnya
   (swatch tema, kartu panel), bukan dari bentuk hurufnya sendiri.
3. **lineHeight PALING LONGGAR dari 5 identitas** (lebih longgar dari Calm Retro yang sudah
   dilonggarkan dari Apple) — mencerminkan panel Skeu yang lembut/empuk tanpa sudut/border tegas;
   teks ikut "bernapas" di ruang lebih lega, selaras kesan dipahat dari bantalan material lunak,
   bukan dicetak rapat di atas permukaan keras.

**`fontFamily` TETAP `FontFamily.Default` (sans) di kelima slot — TIDAK diubah ke Monospace.**
Larangan eksplisit Batch 133 §4 (`FontFamily.Monospace` HANYA ke 2 elemen `Text` durasi/waktu Now
Playing, di luar sistem `Typography` M3) tetap berlaku sama seperti Batch 302 tidak melonggarkannya
untuk Calm Retro — permintaan user kali ini tidak menyebut monospace maupun membatalkan
pembatasan itu.

**`Theme.kt`** (1 titik) — blok `when (identity)` dispatcher `typography` di dalam
`AudioPlayerTheme()` dapat 1 cabang baru: `ThemeIdentity.SKEU_DARK_LITE -> SkeuTypography`,
ditambahkan sebelum `else -> AppleTypography`. `APPLE` tetap satu-satunya identitas yang jatuh ke
`else` (memang benar identitasnya sendiri, bukan gap). Komentar block di atas dispatcher
diperbarui menyebut Batch 305 & histori keputusan Batch 57/279/302.

**Cakupan otomatis app-wide** — karena dispatch terjadi 1 titik di `MaterialTheme(typography=...)`
level root `AudioPlayerTheme()`, SETIAP composable yang sudah memanggil
`MaterialTheme.typography.titleLarge`/`titleMedium`/`bodyMedium`/`bodySmall`/`labelSmall` di
seluruh app (Home/Library/NowPlaying/Settings/semua sheet) otomatis ikut `SkeuTypography` begitu
identitas Neumorphism aktif — 0 call site UI individual perlu diedit satu-satu, pola yang sama
seperti `TactileTypography`/`LiquidGlassTypography`/`CalmRetroTypography` sejak awal. Live-preview
swatch `ThemeOptionCard` (`SettingsScreen.kt`, picker identitas tema) ikut otomatis benar tanpa
disentuh — composable itu memanggil `MaterialTheme.typography` langsung untuk preview tiap
identitas, pola yang sudah terbukti benar sejak Batch 128-131/302.

**Ringkasan file**: 2 file kode (`Type.kt` + `Theme.kt`), di bawah batas Micro-Batch (maks 3 file
kode). 0 file baru, 0 dependency baru, 0 import baru (semua simbol yang dipakai — `Typography`,
`TextStyle`, `FontFamily`, `FontWeight`, `sp` — sudah diimpor `Type.kt` sejak awal untuk definisi
typography lain). `FILE_MANIFEST.txt` tidak berubah (187/187, tidak ada file baru). 0 protected
asset disentuh. Brace/paren blok yang ditambahkan diverifikasi seimbang (`Type.kt`: 27/27 parens,
0/0 braces krn murni deklarasi `val`+`TextStyle` tanpa blok kurung kurawal; `Theme.kt` utuh: 14/14
braces, 152/152 parens).

**Belum divalidasi compile Gradle sungguhan** (0 akses jaringan sesi ini, pola sama tiap batch) —
**WAJIB cek CI setelah push**, walau risikonya rendah (1 `val Typography(...)` baru murni data
class + 1 cabang `when` tambahan yang mengacu ke `val` itu, bukan API/dependency baru, pola persis
sama seperti `CalmRetroTypography` Batch 302 yang sudah terbukti compile bersih).

**Belum diverifikasi visual di device** — prioritas cek kalau user build ulang: (1) pilih
Neumorphism di Settings, judul/label/body app terasa lebih ringan bobotnya dari Apple (SemiBold/
Medium, bukan Bold/SemiBold) dan tracking netral rapat (bukan lebar ala Calm Retro atau rapat
negatif ala Apple), (2) baris teks terasa lebih longgar/lega dibanding 4 identitas lain, (3) 4
identitas lain (Apple/Tactile/CalmRetro/Liquid Glass) visualnya TIDAK berubah sama sekali dari
sebelum batch ini (regresi urutan `when` — cabang baru ditambah tanpa mengubah urutan/isi cabang
lain).

## Batch 304 — Fix laporan bug (screenshot): notifikasi cold-start teks statis + tombol Jeda kepatri (1 file)
Laporan ad-hoc dari user berupa screenshot kartu notifikasi ongoing "SONIX" (gaya visual cocok
skin OEM ala MIUI/HyperOS): teks isi selalu "Memuat lagu…" tidak pernah berubah, dan tombol aksi
"Jeda" tidak pernah berganti label walau musik sedang diputar/dijeda. Ini laporan bug baru,
bukan lanjutan antrean "Micro-Polish Terakhir" — item 2-6 dari daftar itu tetap menunggu di
`PROJECT_STATE.md`, tidak tersentuh batch ini.

String persis dari screenshot ditelusuri lewat grep dan ditemukan tepat satu titik sumber:
`buildColdStartNotification()` di `PlaybackService.kt`. Fungsi ini membangun notifikasi
placeholder yang tampil sangat singkat saat widget home-screen ditekan sebelum proses aplikasi
hidup (cold start) — sebelum notifikasi asli Media3 (`MediaStyle`, auto-sync bawaan) mengambil
alih. Dua masalah berbeda ditemukan di fungsi yang sama:

Pertama, `contentText` di-hardcode "Memuat lagu…" tanpa syarat, tidak pernah membaca state atau
metadata apa pun — bahkan setelah lagu confirmed sedang diputar dan metadata sudah tersedia, teks
itu tetap sama. Kedua, mekanisme untuk memperbarui label tombol sebenarnya sudah ada dari batch
sebelumnya (`onIsPlayingChanged` memanggil `updateColdStartNotification(isPlaying)` selama
placeholder ini aktif), tetapi implementasinya hanya `NotificationManagerCompat.notify()` biasa
ke ID notifikasi yang sama. Karena placeholder ini sengaja bukan `MediaStyle` (dicatat eksplisit
di komentar lama sebagai alasan kenapa update manual diperlukan sama sekali), sejumlah skin OEM
yang cocok dengan gaya visual di screenshot dikenal menahan cache tombol aksi untuk notifikasi
foreground non-MediaStyle dan tidak selalu menggambar ulang hanya dari `notify()`.

**Perbaikan:** `buildColdStartNotification()` mendapat parameter baru `nowPlayingTitle` (default
null, sehingga pemanggilan lama yang memang belum punya media item di titik itu tidak perlu
diubah). Isi teks sekarang menampilkan judul lagu begitu tersedia — judul polos saat sedang
diputar, "judul — Dijeda" saat dijeda — dan hanya jatuh ke "Memuat lagu…" pada jendela cold-start
murni sebelum media item pertama termuat. `updateColdStartNotification()` diganti dari
`notify()` polos menjadi memanggil ulang `startForeground()` (mengikuti pola pemeriksaan versi
SDK yang sama persis dengan fungsi pembuat notifikasi awal di bawahnya) — ini adalah cara resmi
Android untuk memperbarui notifikasi milik service foreground sendiri, bersifat idempoten, tidak
memicu flicker atau me-restart service, dan tetap menjadi perbaikan yang valid terlepas dari
apakah dugaan penyebab cache OEM di atas tepat sasaran atau tidak.

Satu file diubah, nol import baru (semua kelas yang dipakai sudah ada di file), nol file baru,
nol dependency baru — `FILE_MANIFEST.txt` tidak berubah (187/187). Belum divalidasi lewat compile
Gradle sungguhan pada sesi ini (perlu dicek hasil CI setelah push) maupun diverifikasi visual di
perangkat asli — prioritas pengecekan berikutnya: dari kondisi aplikasi benar-benar mati, tekan
tombol play di widget home-screen, dan pastikan teks notifikasi berganti jadi judul lagu serta
tombol berganti label dengan benar saat ditekan berulang.

## Batch 303 — Micro-Polish Terakhir 1/6: overflow title/artist/album (3 file) + planning aksesibilitas (0 kode)
User mengirim daftar 6 item "MICRO-POLISH TERAKHIR" dalam satu pesan, dengan item terakhir
(aksesibilitas) ditandai eksplisit oleh user sebagai "planning first, zero code" — lima item
lainnya tidak diberi penanda itu, sehingga dikerjakan sebagai perubahan kode sungguhan. Karena
satu batch mengerjakan satu tugas, keenam item tidak mungkin selesai sekaligus di batch ini —
item pertama (overflow teks) dipilih untuk dieksekusi, lima sisanya dicatat sebagai antrean
eksplisit di `PROJECT_STATE.md`, bukan diabaikan.

Satu asumsi diambil dan didokumentasikan alih-alih ditanyakan balik ke user (sesuai instruksi
user sendiri untuk tidak ragu-ragu menjalankan daftar ini): item keempat menyebut tema
"Matte Noir", yang bukan lagi identitas yang ada di kode saat ini — nama itu adalah tema custom
lama yang sudah dihapus total dan digantikan "Tactile" sejak Batch 49 (riwayat lengkap ada di
`PROJECT_STATE_ARCHIVE.md`). Item itu diasumsikan merujuk ke pengecekan konsistensi warna/surface
di seluruh mode Dark/Light identitas yang aktif sekarang, termasuk fondasi AMOLED near-black milik
Tactile — bukan permintaan untuk menghidupkan kembali tema yang sudah sengaja dihapus. Item ini
sendiri belum dieksekusi sama sekali di batch ini, jadi asumsi ini belum terpatri ke kode apa pun;
masih terbuka untuk dikoreksi user sebelum gilirannya tiba di antrean.

**Overflow title/artist/album** — seluruh pemanggilan `Text()` yang menampilkan field title/
artist/album di `app/src/main/java/com/rudi/audioplayer/ui/*.kt` digrep dan diperiksa satu per
satu (beberapa hasil pencarian otomatis ternyata false positive setelah diperiksa manual: pesan
konfirmasi `AlertDialog` yang secara wajar membungkus banyak baris, dan label statis seperti
"SEDANG DIPUTAR" yang tidak pernah berisiko overflow). Tiga celah nyata ditemukan dan diperbaiki:
baris "Artis" dan "Album" pada hasil pencarian di `LibraryScreen.kt` sebelumnya sama sekali tidak
membatasi baris teks, sehingga nama panjang bisa membungkus tanpa batas dan merusak kerataan
tinggi antar baris daftar — sekarang keduanya dibatasi satu baris dengan tanda pemotongan,
mengikuti pola yang sudah dipakai `SongRow` di file yang sama. Judul lagu pada header
`RingtoneCutterSheet.kt` sudah dibatasi satu baris sebelumnya tetapi tanpa tanda pemotongan resmi,
sehingga teks yang kepanjangan terpotong mentah tanpa elipsis — sekarang diperbaiki. Baris
peringkat "Artis Paling Sering" di `StatsDashboardScreen.kt` juga belum dibatasi sama sekali,
padahal berbagi baris dengan nomor peringkat dan jumlah putar di kanan-kirinya — nama artis yang
panjang bisa membungkus dan merusak kesejajaran elemen-elemen itu. Judul dan artis pada layar
Now Playing sudah diperiksa ulang secara terpisah dan ternyata sudah benar sejak lama (mengikuti
pola yang sama seperti `SongRow`), sehingga tidak ada perubahan yang diperlukan di sana.

**Planning aksesibilitas** — dua pemeriksaan berbasis pencarian pola dijalankan ke seluruh layar:
ikon aksi di dalam `IconButton`/`IconToggleButton` tanpa deskripsi konten (nol ditemukan), dan
target sentuh di bawah 48dp yang menempel pada elemen yang bisa disentuh (empat kandidat awal,
semuanya ternyata ukuran glyph ikon di dalam kontainer `IconButton` yang secara default tetap
48dp — bukan ukuran area sentuh sesungguhnya, konsisten dengan seri audit ikonografi dan target
sentuh yang sudah dilakukan pada batch-batch sebelumnya). Ini baru mencakup sebagian kecil dari
seluruh cakupan aksesibilitas yang diminta — perilaku disabled-state dan deskripsi konten untuk
elemen non-ikon seperti artwork belum diperiksa sama sekali, sehingga item ini tetap terbuka di
antrean dan tidak ada kode yang ditulis untuknya, sesuai permintaan eksplisit user.

Antrean tersisa dari lima item lain dicatat lengkap di `PROJECT_STATE.md`. Tiga file kode (pas di
batas Micro-Batch), dua import baru (`TextOverflow`, dari package yang sudah menjadi dependency,
bukan dependency baru), nol file baru — `FILE_MANIFEST.txt` tidak berubah (187/187). Belum
divalidasi lewat compile Gradle sungguhan pada sesi ini — perlu dicek hasil CI setelah push,
meski risikonya rendah karena hanya menambahkan parameter pada pemanggilan `Text()` yang sudah
ada beserta satu import standar.

## Batch 302 — Perkuat typography khusus tema Calm Retro, murni 100% (permintaan user langsung, 2 file)
User minta eksplisit: "lanjut perkuat typography khusus theme Calm Retro murni 100%!!" —
melanjutkan pola penguatan typography per-tema (Batch 298 sudah melakukan ini untuk Liquid Glass),
sekarang giliran Calm Retro, dengan penekanan "murni" (bukan pinjaman dari identitas lain).

**Konteks historis yang relevan**: Batch 130 "memurnikan" Calm Retro dari token pinjaman identitas
lain — `tertiary`/`error` dipindah dari reuse token Skeu ke token milik Calm Retro sendiri,
`CalmRetroShapes` dibuat baru (dulu jatuh ke `else` mewarisi `AppleShapes`). TAPI typography
SENGAJA tidak ikut dipurifikasi saat itu, dengan alasan eksplisit: "spec tidak beri spesifikasi
tipografi, pola sama seperti Skeu Batch 57 — bukan kebocoran identitas, beda kasus dari
tertiary/error/shape". Jadi sampai batch ini, Calm Retro masih 100% reuse `AppleTypography` lewat
cabang `else` di dispatcher `Theme.kt`. Permintaan user sekarang membalik keputusan itu secara
eksplisit — pemurnian Calm Retro sekarang genuinely 5/5 (color + tertiary + error + shape +
typography), bukan 4/5.

**`Type.kt`** (1 titik baru, additif) — `CalmRetroTypography` ditambah di akhir file, mengisi 5
slot yang sama seperti `AppleTypography`/`TactileTypography` (`titleLarge`/`titleMedium`/
`bodyMedium`/`bodySmall`/`labelSmall`), bukan menambah slot M3 baru (beda dari kasus Liquid Glass
Batch 298 yang menambal lubang 5 slot tak terdefinisi — di sini 5 slot itu SUDAH terdefinisi lewat
reuse Apple, jadi ini murni mengganti isinya, bukan menambal celah).

Nilai baru dipilih berdasar 3 prinsip pembeda yang konsisten dengan identitas visual Calm Retro
yang sudah ada (CRT-scanline, chromatic-aberration CTA, film-grain ambient, dark-locked, palet
dusty rose/muted sage/dusty denim):
1. **letterSpacing dibalik dari negatif ke positif** — Apple pakai tracking rapat/negatif ala iOS
   modern (`-0.4sp` di `titleLarge`), Calm Retro sekarang pakai tracking terbuka/positif
   (`+0.15sp` s/d `+1.2sp` tergantung slot) — mengesankan jarak antar-huruf mesin ketik/label
   cetak vintage, selaras dengan estetika CRT+aberration yang sudah ada di identitas ini.
2. **Weight TIDAK dinaikkan ke tier Tactile** (ExtraBold/Bold, "machined label" fisik/embossed) —
   Calm Retro secara sadar flat/opaque by design sejak Batch 130 ("bevel/glass/ambient-wash
   Tactile/Skeu sengaja tidak direplikasi"), jadi menaikkan weight ke tier fisik itu akan
   bertentangan dengan keputusan arsitektur yang sudah ada. Weight ditahan di tier yang sama
   seperti Apple (Bold `titleLarge`, SemiBold `titleMedium`/`labelSmall`, Normal untuk body) —
   pembeda datang murni dari spacing+lineHeight, bukan dari boldness tambahan.
3. **`labelSmall` dapat lompatan tracking terbesar** (`0.6sp` → `1.2sp`, 2x lipat) — slot ini
   dipakai luas sebagai kicker/eyebrow app-wide (mis. "BERANDA", "SEDANG DIPUTAR", label section
   Settings), jadi ini titik yang paling sering terlihat dan paling terasa "retro"-nya lewat
   tracking lebar ala label cetak/prangko vintage.
4. **lineHeight dilonggarkan sedikit dari padanan Apple di semua slot** (bukan dipadatkan) —
   nama identitas "Calm" secara harfiah diterjemahkan jadi ruang napas antar-baris yang lebih
   lega, bukan rapat.

**`fontFamily` TETAP `FontFamily.Default` (sans) di kelima slot — TIDAK diubah ke Monospace.**
Ini keputusan sadar, bukan kelalaian: Batch 133 (spec `palet_warna_calm_retro_v3.md` Pilar C)
secara eksplisit membatasi `FontFamily.Monospace` HANYA ke 2 elemen `Text` durasi/waktu di Now
Playing (`NowPlayingScreen.kt`, diterapkan inline via `timeFontFamily`, di luar sistem
`Typography` M3 — jadi TIDAK tersentuh sama sekali oleh perubahan batch ini), dengan larangan
eksplisit "SENGAJA tidak disentuh ke judul/lirik" mengutip §4 spec asli. Permintaan user kali ini
("perkuat... murni") tidak menyebut monospace maupun membatalkan pembatasan itu, jadi migrasi
seluruh scale ke monospace akan jadi pembalikan keputusan terdokumentasi yang tidak diminta, bukan
"penguatan" yang diminta — di luar scope, tidak dieksekusi.

**`Theme.kt`** (1 titik) — blok `when (identity)` dispatcher `typography` di dalam
`AudioPlayerTheme()` dapat 1 cabang baru: `ThemeIdentity.CALM_RETRO -> CalmRetroTypography`,
ditambahkan SEBELUM `else -> AppleTypography` (urutan `when` di Kotlin dievaluasi top-to-bottom,
jadi cabang eksplisit baru ini mencegat Calm Retro sebelum sampai ke `else`).
`SKEU_DARK_LITE`/`APPLE` TETAP jatuh ke `else -> AppleTypography` seperti sebelumnya — TIDAK
disentuh, karena permintaan user eksplisit menyebut "Calm Retro" saja, bukan audit typography
menyeluruh ke semua identitas (menyamakan keduanya juga akan menyalahi keputusan sadar Batch 57
soal Skeu — "no separate type-scale spec supplied for this theme"). Komentar block di atas
dispatcher diperbarui menyebut Batch 302 & alasan pembalikan keputusan Batch 130.

**Cakupan otomatis app-wide** — karena dispatch terjadi 1 titik di `MaterialTheme(typography=...)`
level root `AudioPlayerTheme()`, SETIAP composable yang sudah memanggil
`MaterialTheme.typography.titleLarge`/`titleMedium`/`bodyMedium`/`bodySmall`/`labelSmall` di
seluruh app (Home/Library/NowPlaying/Settings/semua sheet) otomatis ikut `CalmRetroTypography`
begitu identitas Calm Retro aktif — 0 call site UI individual perlu diedit satu-satu, pola yang
sama seperti bagaimana `TactileTypography`/`LiquidGlassTypography` bekerja sejak awal. Live-preview
swatch `ThemeOptionCard` (`SettingsScreen.kt`, picker identitas tema) ikut otomatis benar tanpa
disentuh — composable itu memanggil `MaterialTheme.typography` langsung untuk preview tiap
identitas, pola yang sudah terbukti benar sejak Batch 128-131 (warna/shape preview Calm Retro
juga otomatis ikut tanpa edit `SettingsScreen.kt`).

**Ringkasan file**: 2 file kode (`Type.kt` + `Theme.kt`), di bawah batas Micro-Batch (maks 3 file
kode). 0 file baru, 0 dependency baru, 0 import baru (semua simbol yang dipakai — `Typography`,
`TextStyle`, `FontFamily`, `FontWeight`, `sp` — sudah diimpor `Type.kt` sejak awal untuk
definisi typography lain). `FILE_MANIFEST.txt` tidak berubah (187/187, tidak ada file baru). 0
protected asset disentuh. Brace/paren kedua file diverifikasi seimbang (`Type.kt` 33/33 parens,
0/0 braces krn murni deklarasi `val`+`TextStyle` tanpa blok kurung kurawal; `Theme.kt` 14/14
braces, 67/67 parens).

**Belum divalidasi compile Gradle sungguhan** (0 akses jaringan sesi ini, pola sama tiap batch) —
**WAJIB cek CI setelah push**, walau risikonya rendah (1 `val Typography(...)` baru murni data
class + 1 cabang `when` tambahan yang mengacu ke `val` itu, bukan API/dependency baru, pola persis
sama seperti `LiquidGlassTypography` Batch 279/298 yang sudah terbukti compile bersih).

**Belum diverifikasi visual di device** — prioritas cek kalau user build ulang: (1) pilih Calm
Retro di Settings, judul/label/body app terasa beda tracking-nya dari Apple (lebih terbuka, kicker
"BERANDA"/dst terasa lebih lebar spacing-nya), (2) teks waktu/durasi Now Playing TETAP tampil
monospace seperti sebelumnya (path terpisah, seharusnya tidak ikut berubah — tapi tetap perlu
dicek visual krn belum ada compiler di sesi ini), (3) 4 identitas lain (Apple/Tactile/Skeu/Liquid
Glass) visualnya TIDAK berubah sama sekali dari sebelum batch ini (regresi urutan `when` — cabang
baru ditambah tanpa mengubah urutan/isi cabang lain).

## Batch 301 — 2 bug fix lanjutan dari feedback device: tab Library masih flat + stutter transisi tab (2 file)
User melaporkan lagi dalam 1 pesan: (1) tab Beranda **dan** tab Library masih flat total; (2)
stuttering saat **transisi antar tab** masih terjadi. Poin (2) beda dari laporan Batch 300
("stutter scroll") — kata kuncinya kali ini transisi/pindah tab, bukan scroll di dalam 1 layar —
jadi root cause-nya juga beda, bukan sekadar lanjutan tuning `blurRadius`.

**Bug 1 — tab Library masih flat (`LibraryScreen.kt`)**: grep `isPanelTheme` di seluruh app
(metodologi sama persis Batch 300) nemuin LibraryScreen belum pernah ikut audit itu sama sekali —
Batch 300 cuma menyentuh `HomeScreen.kt`/`StatsDashboardScreen.kt`. Hasilnya: 1 titik gap identik
(bar "Urungkan" yang muncul setelah sembunyikan lagu) — cabang isTactile/isSkeu sendiri tapi
Liquid Glass jatuh ke `else` generik (`Modifier` polos + `Surface` warna solid opaque). Ditambah
cabang `isLiquidGlass -> Modifier.frostedGlass()`, pola identik ContinueListeningCard/
StatSectionCard. Ini SATU-SATUNYA `Surface`/panel yang ada di LibraryScreen — header, chip
filter (`LibraryFilterChips`), dan `SongRow` semuanya murni `Box`/`Row` tanpa `Surface` sama
sekali (dibandingkan dulu dengan `FilterChip` M3 di SmartPlaylistScreen/EqualizerSheet/
RingtoneCutterSheet — semuanya juga flat di SEMUA identitas tema, bukan cuma Liquid Glass, jadi
itu bukan bug, itu memang bukan permukaan "kaca").

**Grep yang sama juga nemuin 2 sisa titik lain** (`GestureIndicatorBadge` di `NowPlayingScreen.kt`,
badge overlay swipe volume/brightness) yang punya gap identik tapi SENGAJA TIDAK disentuh —
elemen transient (cuma muncul sekejap saat gesture), bukan bagian dari tab Beranda/Library yang
dilaporkan user, di luar cakupan laporan ini.

**Catatan penting soal Beranda**: `ContinueListeningCard` di `HomeScreen.kt` dicek ulang — fix
Batch 300 SUDAH benar di source saat ini (`isLiquidGlass -> Modifier.frostedGlass()` ada, `color`
sudah Transparent). Kalau user menguji APK Batch 301 ini (bukan build lama sebelum Batch 300)
dan Beranda MASIH terasa flat, kemungkinan besar root cause-nya BUKAN lagi bug routing kode,
melainkan keterbatasan desain: `LiquidGlassDarkBackground`/`LiquidGlassLightBackground`
(`Color.kt`) adalah 1 warna solid FLAT di seluruh layar (bukan wallpaper/gradient/blob ambient) —
blur asli di belakang panel kaca otomatis ikut flat kalau yang dibaurkan cuma warna polos, mau
seberapa pun benar pengkabelan kodenya. Ini item ARSITEKTUR baru (nambah lapisan ambient di
belakang panel), bukan bug routing seperti Bug 1 di atas — per §4 aturan sesi aktif, item roadmap
baru butuh konfirmasi user dulu sebelum dieksekusi, jadi TIDAK dieksekusi batch ini. Perlu
diklarifikasi ke user: APK mana yang sedang diuji, dan apakah ambient background ini sesuatu yang
mau dikejar.

**Bug 2 — stutter transisi tab (`MainActivity.kt`, 6 titik/1 file)**: root cause bukan blur —
`popUpTo`/`navigate` di 6 titik (3 `NavigationBarItem` layar Compact + 3 `NavigationRailItem`
layar Medium/Expanded, pola identik) 0 pernah pakai `saveState`/`restoreState`. Tanpa itu, Nav
Compose MENGHANCURKAN TOTAL layar tujuan (state `LazyColumn`, scroll position, scope ViewModel)
lalu membangunnya dari nol tiap tap tab — persis pola resmi yang didokumentasikan Google sebagai
penyebab jank pindah tab di bottom navigation, dan BEDA dari mekanisme stutter-scroll Batch 300
(resample blur per-frame). Diperbaiki: `popUpTo("home") { saveState = true }` +
`launchSingleTop = true` + `restoreState = true` di keenam titik. Sekalian dilepas
`inclusive = true` yang sebelumnya khusus di tombol Beranda sendiri — grep CHANGELOG/komentar
lama 0 nemuin alasan terdokumentasi kenapa Beranda sengaja dibuat beda dari Library/Settings,
jadi disamakan jadi 1 pola konsisten bertiga (efek sampingnya: tab Beranda sekarang juga
mempertahankan scroll position-nya sendiri saat ditinggal lalu kembali, bukan cuma fix
performa).

Total 2 file kode, di bawah batas Micro-Batch. 0 file lain disentuh, 0 dependency baru, 0 import
baru berupa dependency eksternal (`frostedGlass` di `LibraryScreen.kt` dari package `ui.theme`
yang sudah ada — pola sama Batch 300), 0 file baru — `FILE_MANIFEST.txt` tidak berubah. Brace/
paren kedua file diverifikasi seimbang. Belum divalidasi compile Gradle sungguhan (tidak ada
akses jaringan di sesi ini) — cek hasil CI setelah push; risiko rendah karena hanya menambah 1
cabang `when` (fungsi yang sudah dipakai luas) di `LibraryScreen.kt`, dan mengganti opsi navigasi
standar (bukan API/dependency baru) di 6 titik `MainActivity.kt`.

**Status setelah batch ini**: Bug 2 (stutter transisi) — fix arsitektural langsung, risiko rendah,
kemungkinan besar tuntas tapi tetap wajib dikonfirmasi user di device nyata. Bug 1 — tuntas untuk
SEMUA gap routing kode yang terkonfirmasi lewat grep (Library + Beranda + Statistik = 0 sisa gap
di luar 2 badge NowPlayingScreen yang sengaja di luar cakupan), TAPI kalau Beranda/Library masih
terasa flat setelah build ini, kemungkinan besar sudah keluar dari ranah "bug kode" dan masuk ke
diskusi arsitektur ambient background di atas — bukan sesuatu yang bisa diperbaiki lagi cuma
lewat tuning parameter seperti Batch 296-300.

## Batch 300 — 2 bug fix dari feedback device sungguhan: card Liquid Glass flat + stutter scroll (3 file)
User melaporkan 2 hal sekaligus dari device fisik: (1) efek Liquid Glass cuma kena sebagian card,
sisanya flat total; (2) sedikit stuttering saat scroll aplikasi (tidak sampai freeze). Ini laporan
performa pertama yang masuk sejak diminta di Batch 297/299.

**Bug 1 — card flat (`HomeScreen.kt`, `StatsDashboardScreen.kt`)**: root cause bukan bug
rendering Haze, melainkan gap arsitektur lama. `ContinueListeningCard` (Beranda) dan
`StatSectionCard` (Statistik Dengar — komentar kodenya sendiri menyebut "pola sama persis
ContinueListeningCard") masing-masing punya cabang khusus untuk identitas Tactile dan Skeu, tapi
Liquid Glass dibiarkan jatuh ke cabang `else` generik: `Modifier.clip()` polos plus `Surface`
warna solid opaque — sama sekali tidak melalui `.frostedGlass()`, titik pusat yang seharusnya
dipakai semua permukaan kaca di app ini. Grep ulang mengonfirmasi ini adalah SATU-SATUNYA 2 titik
di seluruh codebase yang lolos dari konvensi itu; 12 titik lain (MiniPlayerBar, NowPlayingScreen,
8 bottom sheet) semua sudah benar sejak Batch 296-297. Diperbaiki dengan menambah cabang
`isLiquidGlass` eksplisit di kedua file, memakai pola yang identik dengan cabang isTactile/isSkeu
yang sudah ada (warna `Surface` diganti Transparent, modifier diganti `.frostedGlass()` yang
menggambar tint+blur+edge miliknya sendiri).

Dua kartu lain sengaja TIDAK disentuh karena berada di luar cakupan bug ini: `ThemeOptionCard`
(SettingsScreen) adalah swatch preview untuk seluruh identitas tema sekaligus di layar pemilih
tema, bukan tema yang sedang aktif — konteksnya beda, menyamakannya dengan tema aktif justru akan
salah. `HomeSongCard` (LazyRow di Beranda) tidak punya `Surface`/panel sama sekali — cuma artwork
dan teks polos di bawahnya — jadi tidak relevan dengan gap `.frostedGlass()` ini.

**Bug 2 — stutter scroll (`BlurUtils.kt`, 1 titik)**: `blurRadius` yang dinaikkan ke 32dp di
Batch 298 diturunkan balik ke **24dp**, nilai sebelum kenaikan itu. Komentar kode Batch 298/299
sendiri sudah menandai 32dp "dekat batas nyaman performa" untuk device kelas API 32 — blur asli
Haze mengambil sampel ulang tiap frame saat konten di belakang permukaan kaca berubah, dan
`MiniPlayerBar` yang selalu melayang di atas layar manapun yang sedang di-scroll (lihat
`LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §5 langkah 5) adalah kandidat biaya GPU terbesar untuk ini.
Radius 24dp sendiri sudah pernah berjalan tanpa 1 pun laporan stutter selama Batch 296-297,
sehingga revert ke angka itu adalah langkah dengan risiko paling rendah. Tint
(`liquidGlassAlpha`, 0.38f/0.48f sejak Batch 299) sengaja TIDAK ikut diubah — itu lever untuk
keterlihatan blur, bukan performa, dan user tidak melaporkan masalah keterlihatan kali ini.

Total 3 file kode, pas di batas Micro-Batch. 0 file lain disentuh, 0 dependency baru, 0 file baru
— `FILE_MANIFEST.txt` tidak berubah (187/187). Import baru di 2 file (`isLiquidGlassTheme`,
`frostedGlass`) berasal dari package `ui.theme` yang sudah ada, bukan dependency eksternal baru.
Brace/paren ketiga file diverifikasi seimbang. Belum divalidasi compile Gradle sungguhan (tidak
ada akses jaringan di sesi ini) — cek hasil CI setelah push; risiko tetap rendah karena hanya
menambah 1 cabang `when` (memakai fungsi yang sudah dipakai luas di file lain) per file, plus 1
literal `Dp`, tanpa API atau dependency baru.

**Status Fase 5 langkah 5/5 setelah batch ini: masih belum selesai.** Kedua fix di atas menjawab
sebagian dari verifikasi device yang diminta (cakupan card + performa), tapi belum ada konfirmasi
ulang dari user apakah 24dp sudah cukup meredakan stutter (kalau masih terasa, lever berikutnya
adalah turunkan radius lagi atau tinjau frekuensi re-render `MiniPlayerBar` saat progress lagu
berjalan — bukan tint), dan tint 0.38f/0.48f dari Batch 299 juga masih menunggu konfirmasi
terpisah sebagai titik akhir atau belum.

## Batch 299 — Fase 5 langkah 5/5: feedback device API 33+ sungguhan, tuning alpha blur iterasi 2 (1 file)
User melaporkan langsung dari device fisik **API 33+** (tier "Runtime Shader" — paling cepat/
ringan menurut `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §2, bukan tier lemah API 31/32) bahwa efek
blur Liquid Glass masih kurang kelihatan. Ini persis skenario yang sudah diantisipasi di komentar
Batch 296/298 sendiri: kalau blur nyaris tak kelihatan, penyebabnya adalah alpha tint yang masih
terlalu pekat menutupi blur asli di bawahnya — bukan bug rendering, karena device ini justru ada
di tier performa terbaik dan API Haze `hazeSource`/`hazeEffect` sudah terkonfirmasi compile dan
berjalan sejak CI Batch 296 hijau.

**`BlurUtils.kt`** — `liquidGlassAlpha` diturunkan lagi: 0.55f → 0.38f (mode gelap), 0.65f →
0.48f (mode terang). Penurunan kali ini sengaja lebih besar dibanding langkah Batch 296 → 298,
karena feedback "masih kurang" ini datang setelah satu putaran tuning sebelumnya (bukan dari
titik awal lama 0.92f/0.96f), jadi wajar kalau koreksinya juga lebih besar. Selisih antara mode
gelap dan terang (0.10) dipertahankan sama seperti seluruh iterasi sebelumnya — mode terang tetap
butuh tint sedikit lebih pekat untuk kontras teks yang setara. `blurRadius` (32dp sejak Batch
298) **sengaja tidak ikut dinaikkan** batch ini — parameter yang teridentifikasi sebagai
penyebab "blur ketutup" adalah tint, bukan radius, dan radius sendiri sudah didokumentasikan
berada dekat batas nyaman performa untuk device kelas API 32.

0 file lain disentuh, 0 import baru, 0 dependency baru — `FILE_MANIFEST.txt` tidak berubah
(187/187). Brace/paren file diverifikasi seimbang (8 buka/8 tutup, 34 buka/34 tutup — komentar
dikecualikan dari perhitungan). Belum divalidasi compile Gradle sungguhan (tidak ada akses
jaringan di sesi ini) — cek hasil CI setelah push, meski risikonya rendah karena hanya mengganti
2 literal `Float`, bukan API atau dependency baru.

**Status Fase 5 langkah 5/5 setelah batch ini: masih belum selesai**, sekarang masuk putaran
tuning ke-2. Nilai 0.38f/0.48f tetap berstatus "titik awal berikutnya", bukan angka final — wajib
dikonfirmasi ulang oleh user setelah mencoba build hasil batch ini. Kalau masih kurang, turunkan
lagi; kalau justru jadi terlalu ramai sampai teks sulit dibaca, naikkan sedikit dari titik ini
(jangan kembali ke 0.55f/0.65f). Info performa (ada lag saat scroll atau tidak) juga masih belum
pernah dilaporkan sejak langkah 5/5 dibuka di Batch 297 — akan berguna untuk sesi berikutnya.

## Batch 298 — Perkuat typography + efek blur Liquid Glass (permintaan user langsung, 2 file)
User minta eksplisit "perkuat typography+efek blur pada theme liquid glass", di luar antrean
roadmap blur (langkah 5/5 — verifikasi visual di device sungguhan — masih tertunda sejak Batch
297, TIDAK diselesaikan batch ini). 2 perubahan berikut sengaja dipasangkan satu alasan: blur
yang lebih kuat membuat backdrop di belakang panel kaca lebih "ramai", jadi teks header/label di
atasnya butuh kontras lebih tinggi supaya tetap terbaca — bukan dua perbaikan yang tidak
berhubungan.

**Blur (`BlurUtils.kt`)** — default `blurRadius` pada `frostedGlass()` naik dari 24dp ke 32dp.
Grep ulang mengonfirmasi 12 dari 12 pemanggilan `.frostedGlass()` di seluruh app memakai default
(tidak ada yang mengirim argumen sendiri), dan parameter ini terbukti tidak pernah dibaca oleh 4
identitas lain (Apple/Tactile/Skeu/Calm Retro) — hanya cabang Liquid Glass yang menangkapnya
untuk diteruskan ke `hazeEffect` milik Haze. Menaikkan angka default ini karena itu murni
menguatkan blur asli Liquid Glass, tanpa efek samping ke tema lain. Angka tidak dinaikkan lebih
jauh (mis. 40dp+) karena desain blur asli (Batch 294) sudah mencatat API 32 sebagai kelas
perangkat yang "berat" untuk efek ini — 32dp dipilih sebagai kenaikan yang terasa tanpa masuk ke
zona risiko performa. Tint (`liquidGlassAlpha`, 0.55f gelap / 0.65f terang) sengaja tidak ikut
diubah batch ini — nilai itu sudah ditandai eksplisit di Batch 296 sebagai titik awal yang wajib
dituning ulang berdasarkan hasil verifikasi di device sungguhan, jadi biar tetap satu sumber
kebenaran sampai data device itu ada.

**Typography (`Type.kt`, `LiquidGlassTypography`)** — dua kelompok perubahan:
1. Bobot naik satu tingkat pada 3 slot yang sudah ada: `titleLarge` (SemiBold → Bold),
   `titleMedium` (Medium → SemiBold), `labelSmall` (Medium → SemiBold). Ukuran, line height, dan
   letter spacing tidak diubah sama sekali — identitas "tracking terbuka, bukan rapat ala Apple"
   dari Batch 279 tetap dipertahankan, murni bobotnya yang naik.
2. Lima slot skala tipografi Material3 yang sebelumnya kosong kini diisi: `headlineSmall`,
   `titleSmall`, `bodyLarge`, `labelLarge`, `labelMedium`. Sebelum menulis kode, dilakukan grep
   `MaterialTheme.typography.*` di seluruh `app/src/main/java` — kelima slot ini ternyata dipakai
   luas (angka besar di `StatsDashboardScreen`, judul seksi di `LibraryScreen`/`SettingsScreen`,
   teks lirik di `LyricsView`/`LyricsSheet`, label filter di `SmartPlaylistScreen`, tombol/label
   di `RingtoneCutterSheet`, teks meta di `NowPlayingScreen`) tapi belum pernah didefinisikan di
   `LiquidGlassTypography` — selama ini diam-diam jatuh ke `Typography()` default Material3
   (Roboto) setiap kali tema Liquid Glass aktif. Liquid Glass adalah satu-satunya dari 5 identitas
   tema yang punya lubang ini (AppleTypography/TactileTypography juga cuma mengisi 5 slot yang
   sama, tapi keduanya di luar cakupan permintaan user kali ini — hanya "liquid glass" yang
   diminta, jadi keduanya tidak disentuh). Ukuran slot baru mengikuti pola yang sudah ada di app
   ini (sedikit di atas ukuran default Material3, bukan angka mentahnya), dan bobotnya mengikuti
   aturan yang sama seperti poin 1: `headlineSmall`/`titleSmall`/`labelLarge` (peran header/label)
   naik ke SemiBold/Bold, sementara `bodyLarge` tetap Normal — sejajar `bodyMedium`/`bodySmall`
   yang juga sengaja tidak ikut ditebalkan, supaya kontras datang dari header versus isi, bukan
   dari menebalkan seluruh teks sekaligus.

Tidak ada import baru, dependency baru, atau file baru — `FILE_MANIFEST.txt` tidak berubah
(187/187). Brace/paren kedua file diverifikasi seimbang (bagian kode, komentar dikecualikan
dari perhitungan). Belum divalidasi compile Gradle sungguhan (tidak ada akses jaringan di sesi
ini) — cek hasil CI setelah push, meski risikonya tergolong rendah karena hanya perubahan nilai
dan pengisian `TextStyle` baru, bukan API atau dependency yang belum pernah dipakai sebelumnya.

## Batch 297 — Blur asli fase 5 langkah 3-4/5: verifikasi ModalBottomSheet + CI Batch 296 hijau (0 kode)
User kirim screenshot CI GitHub Actions: **Batch 296 — Success, 6m 23s, 1 artifact.** Dependency
Haze 1.7.2 + API `hazeSource`/`hazeEffect`/`HazeEffectScope.blurRadius` TERKONFIRMASI compile
bersih — 2 risiko yang ditandai Batch 296 (dependency baru + API yang baru dipakai, sama-sama
belum pernah dicompile) SELESAI terjawab. Gerbang "WAJIB cek CI sebelum lanjut" resmi dibuka.

Lanjut langkah 3 ("NowPlayingScreen — cek treatment beda") + langkah 4 ("LibraryScreen row/
Sheets/Dialog/Settings — reuse audit lama"). Kekhawatiran awal: `NowPlayingScreen`'s "Kontrol
Lanjutan" (baris 957) DAN sisa 8 sheet lain semua `ModalBottomSheet` — apakah render-nya di
window/layer TERPISAH dari main content bikin `hazeEffect` di dalamnya TIDAK BISA nyampling
`hazeSource` yang ditempel di `NavHost` (beda window = beda surface capture)? 2 web search
(bukan asumsi):

1. **Haze punya dukungan resmi utk Dialog/ModalBottomSheet** — dokumentasi resminya eksplisit
   py "DialogSample" + "Bottom Sheet sample... blurred bottom sheet using Haze dengan Material
   3's ModalBottomSheet" sbg pola yang DIDUKUNG & didokumentasikan, bukan limitasi/gotcha. Bug
   historis "Haze'd dialogs not blurring background content" sudah lama diperbaiki (rilis 1.6.7,
   versi project ini 1.7.2 jauh di atasnya).
2. **Syarat yang didokumentasikan**: "Always set the container color to transparent... when
   using Haze with ModalBottomSheet" + "avoid Haze tint directly... use translucent background
   color on the dialog surface instead". Grep ulang: **SEMUA 9 sheet app ini SUDAH
   `containerColor = Color.Transparent`** (konvensi lama, jauh sebelum Haze dipertimbangkan —
   awalnya demi konsisten dgn simulasi kaca `frostedGlass()`, TERNYATA kebetulan PERSIS syarat
   yang Haze minta). Tint juga SUDAH lewat `.background()` kita sendiri (bukan `tints` param
   Haze) — persis rekomendasi resmi kedua ("avoid Haze tint directly").

**Kesimpulan: 0 gap, 0 kode tambahan dibutuhkan.** Arsitektur lama (containerColor transparent +
tint manual via `frostedGlass()`) SUDAH match syarat Haze utk dialog/sheet sejak sebelum fase 5
ini dimulai — kebetulan baik, bukan by design saat itu, tapi hasil akhirnya sama. `AlbumArtHero`
(NowPlayingScreen) sendiri TIDAK butuh `hazeEffect` apa pun — dia SOURCE (bagian dari region yang
di-`hazeSource`), bukan permukaan kaca, jadi 0 perubahan relevan di sana. Langkah 3 & 4 roadmap
**ditandai SELESAI** (reuse penuh dari Batch 296 + arsitektur lama, sesuai rencana asli §5:
"tidak perlu re-audit dari nol").

**Sisa SATU-SATUNYA item fase 5: langkah 5/5 — verifikasi visual+performa di device sungguhan.**
Ini BUKAN tugas kode yang bisa "next" lagi dari sandbox sesi ini (0 compiler/emulator/device di
sini) — perlu user coba langsung: (1) pilih Liquid Glass di Settings, (2) buka MiniPlayerBar +
Now Playing + minimal 1 sheet (mis. Equalizer/Queue), scroll konten di baliknya, pastikan blur
GENUINELY kelihatan (bukan cuma tint pekat spt sebelum Batch 296) — kalau blur nyaris tak
kelihatan, kandidat pertama: alpha 0.55/0.65 (BlurUtils.kt) masih ketinggian, turunkan lagi;
kalau TERLALU transparan/teks susah dibaca, naikkan; (3) cek device API level (Settings > Tentang
Ponsel) — sesuai §2 desain, API 31 tepat = fallback scrim (TIDAK ADA peningkatan visual sama
sekali dari fase ini, itu ekspektasi Haze sendiri bukan bug), API 32 = blur tapi lebih berat,
API 33+ = paling ringan; (4) scroll cepat sambil MiniPlayerBar/sheet terbuka, rasakan ada
lag/stutter atau tidak (blur real-time genuinely berat GPU). Laporkan hasil (device+API level+
kesan visual+performa) supaya batch berikutnya bisa tuning alpha/blurRadius yang tepat, bukan
menebak lagi.

0 file diedit batch ini (verifikasi doang). `FILE_MANIFEST.txt` tidak berubah (187/187). Detail:
`LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §5 langkah 3-4 (ditandai selesai di bawah).

## Batch 296 — Blur asli fase 5 langkah 2/5: hazeSource+hazeEffect nyala (2 file)
User minta lanjut eksekusi langsung (bukan tunggu verifikasi CI Batch 295 dulu). API Haze 1.7.2
dicek ulang via web search PERSIS sesi ini (bukan asumsi dari desain Batch 294): dikonfirmasi
`Modifier.hazeSource(state)` + `Modifier.hazeEffect(state, style, block)` dgn properti blur
(`blurRadius`, `tints`, `noiseFactor`) diset LANGSUNG di dalam lambda `block` (skema flat 1.x,
BUKAN wrapper `blurEffect{}` yang baru wajib di linimasa 2.0-alpha) — cocok persis catatan
Batch 295 kenapa 1.7.2 dipilih.

**`MainActivity.kt`** (protected, 1 titik) — `Box(Modifier.weight(1f))` pembungkus `NavHost`
dapat `.hazeSource(state = hazeState)`, **HANYA saat `appThemeIdentity == LIQUID_GLASS`**
(0 biaya render tambahan utk 4 identitas lain — mereka tidak pernah panggil `hazeEffect` sama
sekali, capture yang tidak pernah dikonsumsi = kerja sia-sia). `hazeState` dibaca dari variabel
lokal `AppNavHost` sendiri (Batch 295's `rememberHazeState()`), bukan `LocalHazeState.current`
— fungsi ini justru PROVIDER composition local itu, bukan consumer.

**`BlurUtils.kt`** — `frostedGlass()`'s cabang `isLiquidGlass` dapat 2 perubahan terkoordinasi:
1. **`hazeEffect`** dipasang PALING LUAR (`this.hazeEffect(...)` sebelum `.background()`) —
   urutan modifier menentukan urutan gambar: blur dulu (belakang), baru tint semi-transparan
   menimpa, baru border edge-glow (Batch 281) di atas itu — persis resep §3b desain (blur+tint
   tipis+edge highlight, BUKAN blur polos tanpa warna, BUKAN edge tanpa blur di belakangnya).
2. **`effectiveAlpha` Liquid Glass diturunkan** jadi 0.55f gelap/0.65f terang (dari default
   0.92f/0.96f yang dipakai 4 identitas lain) — **BUKAN kosmetik, keharusan**: alpha setinggi
   itu SENGAJA near-opaque krn dulu 0 blur asli di belakangnya (tint pekat = satu-satunya cara
   jaga keterbacaan). Sekarang backdrop sudah disaring blur, tint sepekat itu akan membuat blur
   nyaris tidak kelihatan (4-8% doang) — menghilangkan seluruh tujuan fase 5. Angka 0.55/0.65
   TITIK AWAL masuk akal, BUKAN final, wajib dituning ulang pas verifikasi device (langkah 5/5).

**Bonus**: parameter `blurRadius: Dp = 24.dp` fungsi ini sejak Batch 53 cuma "kept for source
compatibility, 0 dipakai" (krn dulu 0 blur asli sama sekali) — SEKARANG akhirnya benar2 dipakai
(ditangkap ke `requestedBlurRadius` SEBELUM masuk lambda `hazeEffect{}`, nama beda disengaja krn
`blurRadius` polos DI DALAM lambda itu merujuk ke property `HazeEffectScope` sendiri —
name-shadowing lambda-with-receiver Kotlin, bukan parameter fungsi ini; tanpa capture nama beda
dulu, bisa jadi self-assign salah/no-op). 0 call site manapun (grep ulang, 9 sheet + MiniPlayerBar
+ NowPlayingScreen) yang override `blurRadius` eksplisit, jadi semua otomatis pakai 24dp.

**Cakupan LEBIH LUAS dari sekadar "MiniPlayerBar"**: krn `frostedGlass()` 1 titik shared (bukan
per-file), fix ini otomatis nyala jg utk `NowPlayingScreen`'s panel (baris 957) DAN sisa 8 sheet
lain — SEMUA pemanggil `frostedGlass()` yang aktif DI DALAM region `NavHost` yang di-tag
`hazeSource` (termasuk sheet modal yang tampil DI ATAS layar manapun yang lagi terbuka, sesuai
desain §3a "sumbernya SAMA, apa pun yang lagi tampil"). **TAPI**: langkah 3/5 roadmap ("NowPlaying
— cek dulu apa perlu treatment beda") secara eksplisit BELUM diperiksa detail batch ini — cakupan
otomatis ini kemungkinan besar SUDAH cukup, tapi klaim "fase 3 selesai" ditahan dulu sampai
verifikasi visual sungguhan (langkah 5), bukan diasumsikan benar dari baca kode doang.

2 file, 0 file baru. `FILE_MANIFEST.txt` tidak berubah (187/187, diverifikasi diff eksplisit).
Brace/paren `MainActivity.kt` (252/252, 604/604) & `BlurUtils.kt` (9/9, 75/75) seimbang. **Belum
diverifikasi compile Gradle sungguhan** (0 akses jaringan sesi ini, sama batasan Batch 295) —
risiko GANDA sekarang (dependency Batch 295 BELUM dikonfirmasi resolve + API `hazeEffect`/
`hazeSource`/`HazeEffectScope.blurRadius` yang dipakai di sini belum pernah dicompile sama
sekali). **WAJIB cek CI build setelah push, prioritas SEBELUM lanjut langkah 3/5** — kelas
masalah sama Batch 291-293 (unresolved-reference cuma ketahuan compile-time). Kalau CI gagal:
kandidat pertama dicurigai adalah signature exact `hazeEffect`/`HazeEffectScope` di 1.7.2 vs versi
yang muncul di hasil pencarian (dokumentasi resmi kadang describe versi TERBARU meski artikelnya
lama) — bandingkan pesan error compiler dgn asumsi di atas sebelum ubah pendekatan lain. Detail
lengkap: `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §5 langkah 2 (ditandai selesai di bawah).

## Batch 295 — Blur asli fase 5 langkah 1/5: fondasi plumbing Haze (3 file, dependency baru)
User: "sempurnakan, repack, present" + eksplisit minta lanjut eksekusi (bukan tunggu lagi seperti
dicatat Batch 294). Langkah 1 `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §5 dieksekusi.

**Keputusan versi Haze** (dicek ulang web_search Agustus 2026, persis di momen eksekusi sesuai
§4 dokumen desain): **`dev.chrisbanes.haze:haze:1.7.2`**, BUKAN linimasa `2.0.0-alphaXX` yang
lebih baru tapi masih pre-release aktif (5 alpha dalam ~4 bulan, modul `haze-blur`/
`haze-utils` dapat rilis sesegar 21 Agustus 2026 — linimasa 2.x genuinely linimasa
pengembangan utama saat ini, BUKAN cuma proyek sampingan basi). 1.7.2 dipilih karena itu tag
"Latest" resmi GitHub (non-prerelease) — setiap rilis alpha 2.x eksplisit "Breaking Changes" di
changelog-nya sendiri (split modul wajib, API blur dibungkus `blurEffect{}` baru, dst). Batch
ini "fondasi plumbing" — pijakan SEMUA 4 sub-langkah blur berikutnya — STABILITY > Speed (Core
Protocol, di atas rule #3 "prioritas mutakhir") menang: pijakan pakai API yang breaking-change-
nya sudah selesai, bukan yang masih berpotensi berubah lagi sebelum sub-langkah 2 sempat jalan.

**`app/build.gradle.kts`** (diedit, protected asset) — `implementation("dev.chrisbanes.haze:
haze:1.7.2")` ditambahkan, 1 baris + komentar alasan versi. `haze-materials` (prebuilt style)
TIDAK ditambahkan — desain fase 5 pakai tint `LiquidGlassAccent` yang SUDAH ADA di
`frostedGlass()`, bukan style bawaan Haze.

**`Theme.kt`** (diedit) — `LocalHazeState` baru (`staticCompositionLocalOf { HazeState() }`),
persis di sebelah `LocalIsDarkTheme` yang sudah ada, pola identik (default cuma fallback
preview/test, nilai sungguhan dari provider di `AppNavHost`).

**`MainActivity.kt`** (diedit, protected asset) — `AppNavHost`: `val hazeState =
rememberHazeState()` ditambahkan setelah `navController`. `Scaffold(...)` (baris ~844-1166,
sudah dikonfirmasi jadi statement top-level terakhir fungsi via cek indentasi manual sebelum
edit, bukan tebakan) dibungkus `CompositionLocalProvider(LocalHazeState provides hazeState) {
... }` — badan blok Scaffold TIDAK di-reindent, pola minim-diff identik
`CompositionLocalProvider` yang SUDAH ADA di file yang sama (Batch 24, baris ~210).

3 file (2 protected asset — `app/build.gradle.kts` & `MainActivity.kt` — edit parsial fokus,
sesuai Protocol §2). Brace/paren ketiganya seimbang (MainActivity.kt 252/252,596/596; Theme.kt
14/14,146/146; app/build.gradle.kts 36/36,179/179). **Dikonfirmasi grep: 0 pemakaian
`.hazeSource()`/`.hazeEffect()` di manapun** — genuinely 0 visual/behavior berubah, murni
plumbing. `FILE_MANIFEST.txt` tidak berubah (0 file baru). **Belum diverifikasi compile Gradle
sungguhan** (0 akses jaringan sesi ini, tidak bisa `./gradlew` fetch dependency baru) — **WAJIB
cek CI build setelah push batch ini** sebelum lanjut sub-langkah 2 (MiniPlayerBar, kandidat
visual pertama) — dependency baru + CompositionLocal baru adalah kombinasi paling rawan
typo/unresolved-reference yang cuma ketahuan compile-time, persis kelas masalah Batch 291-293
sebelumnya. Detail lengkap: `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §5.

## Batch 294 — Desain teknis blur asli Liquid Glass (Fase 5 lanjutan, PERENCANAAN SAJA, 0 kode)
User pilih "desain teknis dulu (dokumen, 0 kode)" setelah ditanya mau mulai dari mana utk blur
asli. Dokumen baru: `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md`.

Riset (web search, bukan asumsi): dibandingkan 4 opsi implementasi (Haze/`dev.chrisbanes.haze`,
`imla`, `Cloudy`, hand-roll `RenderEffect` sendiri). **Rekomendasi: adopsi Haze** — library
Compose khusus backdrop-blur, v2.0 rilis 2026 (aktif dikembangkan), dibangun di atas
`GraphicsLayer` Compose 1.7+. Hand-roll ditolak eksplisit dgn alasan: `RenderEffect`+
`graphicsLayer` 1-baris cuma blur ISI composable itu sendiri — LIMITASI SAMA PERSIS yang sudah
didokumentasikan jujur di `BlurUtils.kt` sekarang — blur backdrop SUNGGUHAN butuh koordinasi 2
composable (1 "source" direkam, 1 "effect" sampling+blur ulang), problem solved yang tidak
untung direinvent. Analogi disebutkan: CONVX sendiri juga vendor library terpisah
(`Kyant0/backdrop`) utk alasan yang sama, bukan hand-roll.

Ekspektasi realistis dicatat eksplisit (dari dokumentasi resmi Haze): fallback per-API — API 31
(persis minSdk project sekarang) = "scrim", SAMA PERSIS visualnya dgn `frostedGlass()` yang
sudah ada (0 peningkatan buat device di lantai minSdk baru); API 32 = multi-`GraphicsLayer`; API
33+ = Runtime Shader (paling efisien). Peningkatan visual nyata baru kerasa di device API 32+.

Arsitektur diperiksa terhadap kode SUNGGUHAN (bukan diagram abstrak): `frostedGlass()`
(`BlurUtils.kt`) adalah 1 titik pusat, tapi blur asli TIDAK bisa cuma "edit 1 fungsi itu" — perlu
`HazeState` dibagi antara konten "source" (apa yang di-blur) dan permukaan "effect" (kaca-nya).
`MainActivity.kt` diperiksa: `AppNavHost` (~baris 516) punya `Scaffold` (~844) berisi
`MiniPlayerBar` (~877) + `NavHost` (~1022) sbg 2 anak sejajar — direkomendasikan `HazeState`
dipegang 1 titik di situ, diteruskan lewat `CompositionLocal` baru (`LocalHazeState`, pola sama
`LocalIsDarkTheme` yg sudah ada), MiniPlayerBar jadi kandidat blur PERTAMA (satu-satunya
permukaan yang 100% waktu melayang di atas konten yang scroll).

5 sub-langkah eksekusi didraft (fondasi plumbing → MiniPlayerBar → NowPlayingScreen →
LibraryScreen/Sheets/Dialog/Settings [reuse hasil audit Batch 282-286] → verifikasi
visual+performa device). Dependency baru (`dev.chrisbanes.haze:haze`) diberitahukan dari sekarang
akan menyentuh `app/build.gradle.kts` (protected asset) di batch eksekusi pertama — TIDAK
ditambahkan sekarang, dan versi pasti SENGAJA tidak ditulis (arsitektur modular Haze 2.x baru,
versi hari ini berisiko basi di batch eksekusi nanti — cek ulang dokumentasi resmi saat itu).

`ROADMAP_LIQUID_GLASS_REDESIGN.md` §5 diupdate menunjuk dokumen baru ini (ringkasan singkat,
bukan duplikasi penuh). `FILE_MANIFEST.txt` diperbarui (+1 file). 0 kode disentuh sama sekali
(instruksi eksplisit user). **Prioritas sesi berikutnya**: TUNGGU user minta lanjut eksekusi —
jangan mulai sub-langkah 1 tanpa instruksi, ini murni desain yang menunggu giliran.

## Batch 293 — Hotfix CI: instrumentation-tests job FAILED "No compatible devices connected" (regresi tertinggal Batch 290, 1 protected asset)
User upload screenshot GitHub Actions run #287 (`build` job HIJAU — konfirmasi fix Batch 292
sukses — tapi job `instrumentation-tests` terpisah FAILED, 7m42s) + `instrumentation_test_report_287.zip`.
Report HTML dibaca (`debug/index.html`, tab "Failed tests"): 1 test, 1 failure, pesan persis
**"No compatible devices connected."** — BUKAN test assertion yang gagal (nama test/class-nya
sendiri kosong di laporan), murni tidak ada device yang cocok utk dites sama sekali.

**Akar masalah, dikonfirmasi silang ke `app/build.gradle.kts`, bukan tebakan**: `minSdk` di
project sudah 31 (dinaikkan Batch 290, bagian eksekusi Liquid Glass blur asli), tapi job
`instrumentation-tests` di `.github/workflows/build.yml` masih pakai `api-level: 30` — TIDAK
ikut diperbarui saat Batch 290 bump minSdk. AGP/Gradle test-runner otomatis menyaring keluar
device yang API levelnya DI BAWAH `minSdk` modul sebelum `connectedDebugAndroidTest` sungguhan
jalan; 1 emulator API 30 yang di-boot job ini jadi 0 device valid dari sudut pandang module
(minSdk 31 > device API 30) — persis kenapa pesannya "no compatible devices", bukan kegagalan
boot emulator/KVM/dsb (emulatornya sendiri sukses boot, cuma ditolak sebelum test jalan).

**Fix** (`.github/workflows/build.yml`, 1 protected asset, edit parsial 1 baris + komentar):
`api-level: 30` → `31`. Nilai baru dipilih TEPAT di lantai `minSdk` baru, bukan asal naik ke
targetSdk(34)/compileSdk(36) — konsisten dgn alasan pemilihan API level ASLI job ini (Batch 103:
"cukup relevan, tidak melebihi apa yang app-nya sendiri sasar"), minSdk 31 sekarang JADI batas
relevan terendah itu. Grep ulang seluruh `build.yml` mengonfirmasi `api-level: 30` cuma 1 titik
konfigurasi (sisa kemunculan "30" lain di file cuma di komentar historis, bukan value aktif) —
bukan asumsi "pasti cuma 1 tempat", benar-benar dicek.

YAML divalidasi ulang (`yaml.safe_load`) sesudah edit. 0 file lain disentuh. **Belum ada CI run
baru yang membuktikan job `instrumentation-tests` sungguhan hijau** — baru menghilangkan akar
masalah yang terkonfirmasi dari report; test SENDIRI (yang sekarang akhirnya akan benar-benar
tereksekusi di device yang valid) belum pernah lolos/gagal secara nyata sama sekali di batch mana
pun sejauh ini karena dari awal tidak pernah dapat device yang jalan.

## Batch 292 — Hotfix CI FAILED: `animateItemPlacement` unresolved (akibat langsung bump BOM Batch 291)
User upload `log_fail_286.zip` + instruksi "debugging sampai tuntas, gak usah denial segala
macem". Build gagal total (`compileDebugKotlin` DAN `compileReleaseKotlin`, keduanya) dgn 7 error
identik: `Unresolved reference 'animateItemPlacement'` di `FolderManagerSheet.kt:95`,
`LibraryScreen.kt:902/968/982/1210`, `PlaylistScreen.kt:157`, `QueueSheet.kt:128`.

**Akar masalah, apa adanya, tanpa berkelit**: ini konsekuensi LANGSUNG dari Batch 291 (bump
Compose BOM `2024.05.00` → `2026.04.01`, demi `RenderEffect`/`GraphicsLayer` API buat Liquid
Glass blur asli). `animateItemPlacement()` sudah lama berstatus deprecated di Compose Foundation
(diganti `animateItem()`), tapi Batch 291 tidak mengecek 7 pemakaian lama ini sebelum push — di
BOM sebaru itu API-nya BUKAN LAGI sekadar deprecated-dgn-warning, tapi sudah 100% dihapus dari
classpath (`Unresolved reference`, bukan `warning: deprecated`). Log CI dicek penuh dari awal
sampai akhir (bukan cuma potongan error) — dikonfirmasi INI SATU-SATUNYA akar masalah, 0 error
lain tersembunyi (kspDebugKotlin/kspReleaseKotlin bersih, 2 warning lain — resourceConfigurations
& versi Gradle deprecated — keduanya cuma warning, tidak menghentikan build).

**Fix** (4 file, semua 7 call site, bukan cuma 1 contoh): `.animateItemPlacement()` →
`.animateItem()` di ke-4 file di atas, mekanis 1:1 (semua 7 pemakaian TANPA argumen custom
`animationSpec`, jadi `animateItem()` tanpa argumen adalah padanan langsung, bukan perkiraan —
default `placementSpec`-nya sama-sama spring). 0 import baru dibutuhkan (keduanya member extension
di `LazyItemScope`, resolve otomatis dari receiver, bukan top-level import). Brace/paren ke-4
file dicek ulang, seimbang (replace mekanis `()`→`()`, jumlah kurung tidak mungkin berubah, tapi
tetap diverifikasi, bukan diasumsikan).

**Belum ada CI run baru yang membuktikan build ini sungguhan hijau** — baru menghilangkan SATU
jenis error yang terkonfirmasi dari log asli; kalau ternyata ada lapisan error lain yang baru
kelihatan setelah baris ini lolos compile, itu akan perlu log_fail baru buat dipastikan, bukan
diasumsikan aman dari sini.

## Batch 291 — Liquid Glass langkah 5 lanjutan: bump Compose BOM 2024.05.00 → 2026.04.01 (1 file kode + 2 dokumentasi)
Lanjutan Batch 290 (minSdk 23→31). Dicek: `GraphicsLayer` capture API (`androidx.compose.ui.
graphics.layer.GraphicsLayer` — mekanisme wajib buat capture+`RenderEffect` blur asli, dipakai
library referensi kayak `Kyant0/backdrop`) baru stabil sejak Compose UI 1.7 (BOM 2024.09.00+),
BOM lama 2024.05.00 (Compose 1.6.x) belum punya sama sekali — blocker teknis kedua setelah
minSdk.

**Keputusan versi — BUKAN BOM terbaru mutlak**: web_search dicek, versi terbaru saat ini BOM
2026.08.00 (Compose 1.12, rilis Agustus 2026) — TAPI itu WAJIB compileSdk 37 + AGP minimum
9.1.1, dan migrasi AGP 9.x itu breaking-change besar (DSL lama `BaseExtension`/`AppExtension`
dihapus total, wajib Gradle 9.1.0+, kompatibilitas plugin Kotlin berbeda) — jauh di luar scope
"butuh 1 API baru buat blur". Dipilih **BOM 2026.04.01** (Compose 1.11, rilis April 2026)
sebagai gantinya — sudah py `GraphicsLayer` stabil, TETAP kompatibel `compileSdk=36`/AGP
8.13.0/Kotlin 2.4.10 yang sudah ada, 0 migrasi toolchain breaking. Rule #3 "prioritas mutakhir"
tetap dipatuhi (lompatan besar dari 2024.05.00, bukan versi seadanya) TANPA melanggar STABILITY
> Speed yang levelnya lebih tinggi di hierarki.

Brace/paren `app/build.gradle.kts` seimbang (35/35, 167/167). **⚠️ Risiko diketahui, BELUM
tervalidasi build sungguhan** (bash tool sesi ini 0 akses jaringan, gak bisa compile-check):
`LocalOverscrollConfiguration` (dipakai `SmartPlaylistScreen.kt`) berpotensi deprecated/pindah
API di rentang lompatan 1.6→1.11 (evolusi umum Compose Foundation ke `overscrollEffect`
parameter) — KALAU build CI gagal spesifik di titik ini, itu tersangka pertama, BELUM
diperbaiki preventif batch ini (menghindari perubahan spekulatif tanpa bukti nyata gagal).
0 protected asset lain disentuh.

## Batch 290 — Liquid Glass langkah 5 (blur asli) DIKONFIRMASI, mulai eksekusi: bump minSdk 23→31 (1 file kode + 2 dokumentasi)
User dikonfirmasi eksplisit lanjut ke Opsi A §3b (blur asli, `RenderEffect`/`RenderNode`) setelah
trade-off dijelaskan (drop dukungan Android <12, effort tinggi, infrastruktur belum ada sama
sekali) — sejalan rule #3 `PROJECT_STATE.md` (prioritas mutakhir). **Sub-langkah pertama**:
`app/build.gradle.kts` `minSdk` 23→31 (`Protect` file, edit fokus SATU baris + komentar, 0
sentuh bagian lain). Fondasi wajib sebelum `RenderEffect` bisa dipakai — API 31+. Digrep codebase
buat guard `SDK_INT < 31` yang bakal jadi dead code: **0 ditemukan**, 0 cleanup diperlukan batch
ini. Brace/paren `app/build.gradle.kts` seimbang (35/35, 160/160).
`ROADMAP_LIQUID_GLASS_REDESIGN.md` §5 diupdate: langkah 5 status "opsional/pending" →
"dikonfirmasi, sedang berjalan", sub-langkah minSdk ditandai selesai, sub-langkah berikutnya
(infrastruktur `RenderEffect` capture+blur) dicatat sbg item lanjutan. 0 protected asset lain.

**PENTING — konsekuensi langsung**: build APK dari versi ini TIDAK BISA diinstall di device
Android <12 (API <31). Kalau ada device testing lama di tangan user, sebaiknya dicek device
utama testing sekarang API berapa sebelum lanjut sub-langkah berikutnya.

## Batch 289 — Sync ROADMAP_LIQUID_GLASS_REDESIGN.md: fase 3 tandai 100% selesai (2 dokumentasi, 0 kode)
`ROADMAP_LIQUID_GLASS_REDESIGN.md` §5 belum disentuh sejak Batch 288 selesai (chip/pill fase 3)
— masih nunjukin "⏳ item berikutnya" padahal udah tuntas. Disinkronkan: sisa fase 3 (chip/pill)
ditandai ✅ SELESAI PENUH, Fase 3 (Terapkan ke komponen inti) keseluruhan ditandai 100% selesai
(Sub-langkah 3: 5/5 area + sisa chip/pill, 0 pending tersisa). Ditambahkan catatan eksplisit:
satu-satunya sisa item roadmap (langkah 5, blur asli §3b) OPSIONAL & butuh konfirmasi user dulu
(bump minSdk) — BUKAN item yang bisa auto-eksekusi tanpa izin, beda dari fase 1-3 yang sudah
terkonfirmasi §3 Batch 279. 0 kode, 0 protected asset.

## Batch 288 — Liquid Glass fase 3: sisa 5 titik Material3 FilterChip bawaan → Radius.liquidPill (3 file kode + 1 dokumentasi)
Menutup Pending Queue Batch 287 (kandidat kedua): Material3 `FilterChip` bawaan (shape default
~8dp kotak-bulat, BEDA dari `LibraryFilterChips` yang custom shape). Titik ditemukan lewat grep
`FilterChip(` di 3 file — semuanya dikasih param `shape =` baru, pola opt-in per-identitas
PERSIS Batch 287 (`if (isLiquidGlassTheme()) RoundedCornerShape(Radius.liquidPill) else
FilterChipDefaults.shape` — else eksplisit ke default M3 asli, BUKAN dibiarkan kosong, biar tema
lain 0 perubahan visual persis kayak sebelumnya):
- `EqualizerSheet.kt`: 2 titik (chip preset kuat + chip preset bawaan perangkat)
- `SmartPlaylistScreen.kt` (`SmartPlaylistBuilderSheet`): 2 titik (chip folder + chip genre)
- `RingtoneCutterSheet.kt` (`DestinationChip`, composable privat sendiri): 1 titik

Total 5 titik, 1 val `chipLiquidShape` per file (dihitung sekali per composable, dipakai ulang
di semua `FilterChip` dalam scope yang sama — bukan re-evaluate `isLiquidGlassTheme()` tiap
chip). **Dengan ini, seluruh kandidat pill/chip Batch 287 SELESAI — 0 kandidat lain tersisa**,
sisa fase 3 Liquid Glass kembali ke item roadmap berikutnya (di luar chip/pill).

Brace/paren ketiga file seimbang (`EqualizerSheet.kt` 27/27,100/100; `SmartPlaylistScreen.kt`
104/104,257/257; `RingtoneCutterSheet.kt` 24/24,86/86). 0 import duplikat (dicek per file). 0
protected asset. **Belum diverifikasi visual.**

## Batch 287 — Liquid Glass fase 3 sisa langkah: pill/chip lebar — LibraryFilterChips → Radius.liquidPill (1 file kode + 1 dokumentasi)
Item pending dari Batch 286: audit pill/chip lebar (lebar≠tinggi, BUKAN tombol persegi/
lingkaran) yang genuinely layak `Radius.liquidPill` eksplisit.

**Kandidat pertama & paling menonjol**: `LibraryFilterChips` (`LibraryScreen.kt`) — tab
Lagu/Album/Artis/Folder/Favorit/Playlist/Otomatis, dilihat setiap kunjungan Library. Radius
sebelumnya `Radius.xxl` (20dp FIXED) — cuma KEBETULAN terlihat pill di ukuran teks pendek yang
dipakai sekarang, BUKAN stadium sungguhan yang auto-adaptif ke tinggi render berapa pun (beda
line-height font di device lain bisa saja tidak pas). Fix: `chipRadius = if
(isLiquidGlassTheme()) Radius.liquidPill else Radius.xxl` (helper `isLiquidGlassTheme()` sudah
ada sejak Batch 280, tinggal reuse) — diterapkan ke KEDUA chip row (3 chip utama + 1 chip
"Lainnya"). Opt-in per-identitas, pola sama seluruh redesign — tema lain (Apple/Tactile/Skeu/
CalmRetro) 0 perubahan visual.

Brace/paren `LibraryScreen.kt` seimbang (351/351, 793/793). 1 file kode + 1 dokumentasi
(`ROADMAP_LIQUID_GLASS_REDESIGN.md`, § rencana eksekusi diperbarui). **⏳ Kandidat LAIN
ditemukan tapi BELUM diaudit** (di luar cap): Material3 `FilterChip` bawaan
(`EqualizerSheet.kt`/`SmartPlaylistScreen.kt`/`RingtoneCutterSheet.kt`'s `DestinationChip`) —
pakai shape default M3 (~8dp kotak-bulat, bukan custom shape kayak `LibraryFilterChips`), perlu
`shape=` param eksplisit kalau mau ikut treatment pill — batch berikutnya.

**Belum diverifikasi visual** — prioritas cek: ganti tema ke Liquid Glass, buka tab Library,
pastikan chip filter (termasuk label panjang seperti "Playlist"/"Otomatis") tampil stadium
penuh, bukan rounded-rect biasa.

## Batch 286 — Liquid Glass fase 3 langkah 5: audit Settings — 0 gap, sub-langkah 3 SELESAI PENUH (2 dokumentasi, 0 kode)
Item berikutnya roadmap Liquid Glass: audit `SettingsScreen.kt`. Grep menyeluruh untuk semua
pola percabangan identitas yang sudah jadi standar audit (`frostedGlass`/`tactileEmboss`/
`skeuEmboss`/`calmAberration`/`calmScanlines`/`identity ==`).

**Hasil: 0 gap.** Cuma 1 cluster branch di SELURUH file: `ThemeOptionCard` (preview live tiap
kartu opsi tema di picker Settings) — 3 pengecekan (`isTactilePreview`/`isSkeuPreview`/
`isCalmRetroPreview`), semuanya `==` ke identitas SPESIFIK LAIN (bukan `LIQUID_GLASS`) — Liquid
Glass otomatis dapat 0 emboss + 0 calmAberration lewat ABSENSI kecocokan (bukan lewat branch
eksplisit `else` seperti file2 lain), efeknya SAMA: preview tampil flat/minimalis, persis sesuai
definisi Liquid Glass yang sudah dikonfirmasi Batch 282. **0 `frostedGlass()` dipakai di file
ini sama sekali** — Settings itu list polos (bukan panel mengambang kayak Sheet/MiniPlayerBar),
jadi sisa file 100% generik `MaterialTheme.colorScheme`, otomatis konsisten tanpa perlu disentuh.

**Sub-langkah 3 (terapkan ke komponen inti) SEKARANG SELESAI PENUH** — 5/5 area diaudit
(MiniPlayerBar+NowPlayingScreen Batch 282, LibraryScreen Batch 283, Sheets/Dialog Batch 284,
Settings Batch 286), 0 gap ditemukan di SEMUA area. `ROADMAP_LIQUID_GLASS_REDESIGN.md` §5
diperbarui.

0 kode, 2 dokumentasi (`CHANGELOG.md`/`ROADMAP_LIQUID_GLASS_REDESIGN.md`). Item berikutnya
(sisa fase 3): audit pill/chip lebar (lebar≠tinggi, bukan tombol persegi/lingkaran) yang layak
`Radius.liquidPill` eksplisit — belum ada kandidat ditemukan sejauh ini, perlu grep terarah baru.

## Batch 285 — Rebranding kosmetik: "Audio Player" → "SONIX" (permintaan user, 8 file kode + 1 dokumentasi, cap DILEWATI — 1 task kohesif)
Permintaan user eksplisit: nama app terasa placeholder generik, minta rebrand ke nama keren
terinspirasi 'CONVX'. **Nama dipilih: SONIX** (sound + gaya akhiran-X ala CONVX, pendek & pas
buat audio player). **Scope dijaga ketat sesuai instruksi user sendiri**: "only kosmetik, user
facing. zero touch bagian vital dan sudah lama stabil".

**Metode**: grep menyeluruh `"AudioPlayer"`/`"Audio Player"` di `app/src/main/java/` +
`app/src/main/res/`, tiap titik diperiksa satu-satu, dikategorikan user-facing (ganti) vs
vital/internal (JANGAN sentuh) SEBELUM eksekusi — bukan sapuan mekanis buta.

**9 titik user-facing diganti**: `strings.xml` `app_name` (label launcher — paling penting),
`MainActivity.kt` (judul prompt biometrik + teks splash "SELAMAT DATANG" — momen branding
paling menonjol), `PlaybackService.kt` (judul notifikasi cold-start), `WidgetUpdater.kt` +
`widget_player.xml` (teks fallback widget), `SettingsScreen.kt` ("SONIX versi..."),
`BackupRestoreSheet.kt` (pesan error, HANYA teks tampilan — logic validasi JSON internal
SENGAJA tidak disentuh, lihat di bawah), `FloatingBubbleService.kt` (teks notifikasi bubble),
`README.md` (judul H1).

**SENGAJA TIDAK disentuh (vital/stable, sesuai instruksi user)**:
- `applicationId`/`namespace` = `com.rudi.audioplayer` — mengubah ini = app dianggap APLIKASI
  BEDA oleh Android (install existing user JADI HILANG datanya, bukan update).
- Nama class/composable internal (`AudioPlayerApplication`, `AudioPlayerTheme`,
  `Theme.AudioPlayer` style resource) — identifier kode, tidak pernah dilihat user, refactor
  lintas-file berisiko tinggi buat perubahan "kosmetik".
- **Path filesystem asli device** (`Documents/AudioPlayer/backups`, `Documents/AudioPlayer/
  logs`, path ringtone MediaStore) — kalau diganti, backup/log yang SUDAH ADA di device user
  jadi orphan (app cari di folder baru, file lama tetap di folder lama, tidak ketemu lagi).
  Teks yang MENAMPILKAN path ini ke user (`BackupRestoreSheet.kt`/`DiagnosticLogSheet.kt`)
  ikut TIDAK diubah — supaya tetap akurat menunjuk lokasi asli, bukan salah info.
- **`BackupManager.kt`'s `root.put("app", "AudioPlayer")`** — marker internal di file JSON
  backup, dipakai validasi restore. Tidak diubah → backup lama DAN baru tetap 100% kompatibel
  direstore (cuma pesan tampilan yang diperbarui, bukan pemeriksaan data).
- Komentar kode yang menyebut "AudioPlayer" — dokumentasi developer, bukan user-facing.

Brace/paren 6 file Kotlin seimbang (`MainActivity.kt` 251/251+583/583, `PlaybackService.kt`
78/78+361/361, `WidgetUpdater.kt` 20/20+118/118, `SettingsScreen.kt` 157/157+481/481,
`BackupRestoreSheet.kt` 33/33+89/89, `FloatingBubbleService.kt` 78/78+276/276). XML
(`widget_player.xml`/`strings.xml`) tervalidasi parse. Grep ulang pasca-edit konfirmasi: SEMUA
sisa occurrence "AudioPlayer" cuma yang sengaja dipertahankan (class/style/path/JSON/komentar),
0 yang kelewat.

**Cap 3-file/batch DILEWATI** (9 file total) — 1 task kohesif (rebranding SATU nama, bukan
gabungan task independen), alasan sama presedan Batch 156/275. **Belum diverifikasi visual** —
prioritas cek: launcher icon label, splash screen, notifikasi, widget, Settings, semua tampil
"SONIX" konsisten; backup/restore lama-baru tetap saling kompatibel.

## Batch 284 — Liquid Glass fase 3 langkah 4: audit Sheets/Dialog (0 kode)
Lanjutan §5 langkah 3, urutan "Sheets/Dialog" setelah LibraryScreen (Batch 283, 0 gap). Grep
`.frostedGlass()` app-wide: **9 sheet** memakainya (`ABRepeatBookmarkSheet`, `EqualizerSheet`,
`FolderManagerSheet`, `LyricsSheet`, `QueueSheet`, `RingtoneCutterSheet`, `SongInfoEditSheet`,
`SongPickerSheet`, `VisualizerSheet`) — SEMUA otomatis kebagian fix edge violet-glass Batch 281
tanpa disentuh satu-satu, sama pola MiniPlayerBar/NowPlayingScreen (Batch 282).

5 dari 9 sheet (`ABRepeatBookmarkSheet`/`EqualizerSheet`/`LyricsSheet`/`QueueSheet`/
`VisualizerSheet`) py 1 branch tambahan identik: `if (isCalmRetro) calmScanlines() else
Modifier` di panel utamanya — Liquid Glass jatuh `else` (polos), BENAR sama alasan `SongRow`
Batch 283 (scanline CRT cuma relevan CalmRetro). 4 sheet lain (`FolderManagerSheet`,
`RingtoneCutterSheet`, `SongInfoEditSheet`, `SongPickerSheet`) malah **0 branch identitas sama
sekali** — murni `frostedGlass()` + `MaterialTheme` polos, otomatis konsisten tanpa perlu
diperiksa lebih lanjut.

Dialog non-sheet (`AlertDialog` Material3 standar — `SpeedDialog` dkk, Batch 163) tidak
diperiksa ulang di sini krn 0 hardcoded warna/shape (semua `MaterialTheme.colorScheme` polos
lewat `AlertDialog` bawaan Compose), otomatis ikut token fase 2, sama kategori "grid card"
Batch 283.

**Hasil: 0 gap.** `FILE_MANIFEST.txt` tidak berubah (186/186, diverifikasi diff eksplisit). Item
berikutnya (§5 langkah 3, urutan roadmap): audit Settings (`SettingsScreen.kt`, termasuk
`ThemeOptionCard` picker itu sendiri — sudah dikonfirmasi generik Batch 280, tapi ada elemen
Settings LAIN yang belum diperiksa detail).

## Batch 283 — Liquid Glass fase 3 langkah 3: audit LibraryScreen.kt (0 kode)
Lanjutan §5 langkah 3, urutan "LibraryScreen row" setelah MiniPlayerBar+NowPlayingScreen (Batch
282, 0 gap). Grep menyeluruh `LibraryScreen.kt` utk SEMUA titik `isTactile`/`isSkeu`/
`isCalmRetro` — cuma 2 titik ditemukan di seluruh file (bukan cuma `SongRow`, ikut cek
`AlbumGridView`/undo-hide snackbar juga supaya tidak salah anggap "row" doang):

1. **Snackbar undo-hide** (`isPanelTheme` = Tactile/Skeu dapat emboss+opaque, `else` dapat
   `Surface` solid `colorScheme.surface` + tonal/shadow elevation Material3 default) — Liquid
   Glass jatuh `else` bareng Apple/CalmRetro, warnanya OTOMATIS benar (violet-cool) krn
   `colorScheme.surface` sudah di-dispatch lewat `colorsFor()` (fase 2, Batch 280) — bukan
   `frostedGlass()` jadi bukan glass-tint, tapi memang panel ini BUKAN glass surface (solid
   snackbar), jadi wajar tidak ikut treatment kaca sama sekali, di identitas manapun.
2. **`SongRow`'s `AlbumArt`** — `.calmScanlines()` HANYA utk CalmRetro (efek CRT scanline,
   identitas visual spesifik dia), Liquid Glass jatuh `else` (polos) — SUDAH BENAR, scanline CRT
   retro sama sekali tidak relevan utk identitas "violet-glass minimalis", bukan celah.

**Hasil: 0 gap.** `AlbumGridView` (grid card tab Album) 0 branch identitas sama sekali — grid
card sudah otomatis konsisten Liquid Glass lewat `MaterialTheme.shapes`/`colorScheme` polos,
tidak butuh cabang tambahan. `FILE_MANIFEST.txt` tidak berubah (186/186, diverifikasi diff
eksplisit). Item berikutnya (§5 langkah 3, urutan roadmap): audit Sheets/Dialog (banyak
`ModalBottomSheet` — cek titik non-`frostedGlass()` yang mungkin sama pola "surface terpisah"
kayak `GestureIndicatorBadge`/snackbar di atas).

## Batch 282 — Liquid Glass fase 3 langkah 2: audit MiniPlayerBar + NowPlayingScreen (0 kode)
Lanjutan §5 langkah 3 roadmap, urutan dikonfirmasi user (Liquid Glass duluan). Setelah Batch 281
(edgeBrush terpusat di `frostedGlass()`), sisa pertanyaan: apakah 2 komponen pertama di urutan
(`MiniPlayerBar`, `NowPlayingScreen`) masih punya branch per-identitas LAIN (di luar
`frostedGlass()`) yang butuh cabang `isLiquidGlass` eksplisit? Dibaca menyeluruh, tiap titik
`when { isTactile -> ...; isSkeu -> ...; isCalmRetro -> ...; else -> ... }` di kedua file.

**`MiniPlayerBar.kt`**: `miniPlayPauseShape` (Tactile/Skeu→`shapes.medium`, else→`CircleShape`)
— Liquid Glass jatuh ke `else`, TAPI utk tombol 40dp persegi, `CircleShape` SUDAH stadium penuh
(pill dgn lebar=tinggi = lingkaran, tidak ada beda visual dgn `Radius.liquidPill` di elemen
persegi). Modifier emboss tombol (Tactile/Skeu/CalmRetro dapat efek, else→`Modifier` polos) —
Liquid Glass "flat, tanpa emboss" ini SESUAI definisinya sendiri (`ThemeIdentity.LIQUID_GLASS`:
"minimalis"), bukan kelalaian — berbagi treatment kosong dgn Apple disengaja, sama alasan
"flat shadow shared dgn Apple" yang sudah didokumentasikan Batch 281.

**`NowPlayingScreen.kt`**: pola identik di `playPauseShape` + modifier tombol transport (baris
~621/644). `backdropBlurRadius`/`backdropAlpha` (baris 315-316, cuma CalmRetro dapat nilai
beda) — Liquid Glass ikut default 60dp/0.5f bareng Apple/Tactile/Skeu, wajar (bukan
identity-lock spesifik kayak CalmRetro). `AlbumArtHero`'s `when` besar (border+glow
Tactile/Skeu bespoke) — `else` (Apple+LiquidGlass) dapat `shadow(28dp, spotColor=accentColor)`
polos; `accentColor` di sini tetap DINAMIS per-lagu (bukan dikunci warna identitas kayak
CalmRetro) — sudah sesuai catatan Batch 280 "Liquid Glass otonom pola sama Apple/Tactile/Skeu",
bukan CalmRetro yang locked. `GestureIndicatorBadge` (badge brightness/volume) py `isPanelTheme`
sendiri (Tactile/Skeu dapat emboss+opaque, else dapat `Surface` translucent 0.9f) — INI TIDAK
lewat `frostedGlass()` (surface glass terpisah, dikonfirmasi baca kode), jadi TIDAK otomatis
kebagian fix Batch 281. TAPI dianalisis: blok ini sengaja cuma membedakan "identitas panel
fisik" (Tactile/Skeu) vs "sisanya", bukan tiap identitas dapat rimnya sendiri — Liquid Glass
(bukan panel fisik) pas masuk kategori "sisanya" bareng Apple/CalmRetro, konsisten by-design.

**Hasil: 0 gap di kedua file.** Semua titik yang relevan sudah benar lewat MaterialTheme dispatch
(fase 2, Batch 280) atau `frostedGlass()` (Batch 281); sisanya adalah treatment KOSONG yang
disengaja utk identitas "flat/minimalis", bukan celah yang lupa dikerjakan. `FILE_MANIFEST.txt`
tidak berubah (186/186, diverifikasi diff eksplisit). Item berikutnya (§5 langkah 3, urutan
roadmap): audit `LibraryScreen.kt` (`SongRow` dkk sudah py `isTactile`/`isSkeu`/`isCalmRetro`
sendiri, grep-confirmed, belum diperiksa detail per-titik).

## Batch 281 — Liquid Glass fase 3 langkah 1: edgeBrush khusus di frostedGlass() (2 file)
`ROADMAP_LIQUID_GLASS_REDESIGN.md` §5 langkah 3 — "Terapkan ke komponen inti, urutan MiniPlayerBar
duluan". `frostedGlass()` (`BlurUtils.kt`) adalah SATU shared helper yang dilalui SEMUA panel
glass di app (mini player, tiap bottom sheet, card Home/Library — dikonfirmasi di komentar file
itu sendiri sejak Batch 53/58), jadi memperbaikinya di titik pusat ini = seluruh titik termasuk
MiniPlayerBar otomatis ikut ter-update, bukan disalin manual per-file — persis pola yang sudah
dipakai identitas Tactile (Batch 53) & Skeu (Batch 58/61) dulu saat masing-masing pertama kali
butuh treatment glass sendiri.

**`Theme.kt`** — helper ke-4 `isLiquidGlassTheme()`, pola identik persis
`isTactileTheme()`/`isSkeuTheme()`/`isCalmRetroTheme()` (`primary == LiquidGlassAccent`).

**`BlurUtils.kt`** — `frostedGlass()`'s `edgeBrush` when-block dapat cabang `isLiquidGlass`
sendiri (bukan jatuh ke `else`). **Alasan BUKAN kosmetik semata**: `else` branch cuma benar
mendeteksi "Apple light" lewat `background == AppleLightBackground` literal — identitas lain
otomatis dianggap "dark-tuned" (alpha 0.24f). Calm Retro aman krn terkunci gelap permanen
(tidak pernah kena), TAPI Liquid Glass otonom di KEDUA mode (Batch 280) — tanpa branch sendiri,
mode terangnya akan diam-diam pakai alpha edge yang dituning utk gelap. Ini laten bug yang
ditemukan SAAT nulis fix, bukan cuma penambahan visual kosmetik. Cabang baru pakai
`LiquidGlassAccent` (bukan flat neutral `onSurface` ala Apple) utk highlight rim ungu tipis —
`0.32f→0.06f` alpha di gelap, `0.22f→0.05f` di terang — satu-satunya sentuhan warna pembeda
identitas ini dari Apple di layer glass, tetap gradient statis (bukan sampling backdrop asli,
sesuai §3b Opsi B yang dikonfirmasi user Batch 279).

**Cakupan otomatis**: `MiniPlayerBar`, `NowPlayingScreen` (2 panel), semua `ModalBottomSheet`,
card Home/Library — SEMUA ikut dapat edge violet-glass begitu user pilih identitas ini, TANPA
perlu diedit satu-satu. Langkah 3 roadmap (MiniPlayerBar→NowPlayingScreen→LibraryScreen row→
Sheets/Dialog→Settings) untuk bagian **glass-edge** boleh dianggap selesai serentak lewat fix
ini — sisa pekerjaan per-komponen di langkah itu (kalau ada) adalah hal LAIN di luar glass-edge,
misalnya cabang emboss/shadow spesifik non-glass (`isTactile`/`isSkeu` punya `tactileEmboss()`/
`skeuEmboss()` sendiri di beberapa file, Liquid Glass SENGAJA tidak — flat shadow shared dgn
Apple dinilai tepat krn identitas ini eksplisit "minimalis" per definisinya sendiri di
`ThemeIdentity.LIQUID_GLASS`, bukan gap yang lupa dikerjakan) atau pemasangan `Radius.liquidPill`
di call site pill spesifik (belum ada kandidat pill yang genuinely butuh diubah dari `CircleShape`
yang sudah ada — circle SUDAH stadium penuh utk elemen persegi, `liquidPill` relevan utk elemen
lebar≠tinggi macam chip/pill button lebar, belum ada di alur file yang disentuh batch ini).

2 file, 0 protected asset. Brace/paren `Theme.kt` (13/13, 140/140) & `BlurUtils.kt` (5/5, 55/55)
seimbang. `FILE_MANIFEST.txt` tidak berubah (186/186, diverifikasi diff eksplisit). **Belum
diverifikasi visual di device** — prioritas cek: pilih Liquid Glass di Settings, buka mini
player + Now Playing + 1 bottom sheet, pastikan rim ungu tipis genuinely terlihat (bukan
ketutup krn alpha kegedean/kekecilan) di kedua mode terang/gelap, DAN pastikan 4 identitas lama
(Apple/Tactile/Skeu/Calm Retro) visualnya SAMA SEKALI TIDAK berubah (regresi `else`-branch
krn urutan `when` baru). Item berikutnya (masih §5 langkah 3): audit apakah ada elemen pill/chip
lebar di MiniPlayerBar/NowPlayingScreen/Sheets yang layak dipasangi `Radius.liquidPill` secara
eksplisit di call site-nya.

## Batch 280 — Liquid Glass fase 2: ThemeIdentity.LIQUID_GLASS lengkap (3 file, additif)
Fase 2 §5 roadmap: identitas ke-5 utuh, MASIH BELUM default (side-by-side dgn 4 lama, sesuai
§3a "tambah" yang sudah dikonfirmasi Batch 279).

**`Color.kt`** (diedit) — +10 token palet statis: Dark/Light × Background/Surface/
SurfaceVariant/Text/SecondaryText, `LiquidGlassAccent` (violet-glass 0x8E7CFF, sengaja beda
dari 4 aksen tema lain — biru Apple/biru-ungu Tactile/Titanium-Zamrud Neumorphism/sage Calm
Retro), `LiquidGlassDarkSuccess`/`LightSuccess` (teal/mint, bukan hijau standar). Palet dari
interpretasi teks riset roadmap (0 screenshot resmi CONVX ditemukan, dicatat jujur di §1) —
ekstraksi-dari-artwork Material You masih fase terpisah, bukan bagian batch ini.

**`Theme.kt`** (diedit) — `ThemeIdentity.LIQUID_GLASS` (otonom kedua mode, pola Apple/Tactile/
Skeu). `LiquidGlassDarkColors`/`LightColors` (`darkColorScheme`/`lightColorScheme` standar).
`LiquidGlassShapes` (`small=Radius.xl 18dp, medium=Radius.xxxl 24dp, large=Radius.liquidLg
34dp`) — **`Radius.liquidPill` (999dp) SENGAJA TIDAK dipasang di `Shapes` generik**: token itu
dipakai di M3 utk Card/Sheet/dialog besar berbagai ukuran, radius 999dp di situ akan clamp jadi
bentuk lensa/blob di surface tinggi, bukan "kartu bersudut besar" yang dimaksud riset — dicek &
dikoreksi sebelum commit (draf awal batch ini sempat salah pasang `liquidPill` di situ,
langsung diperbaiki setelah dipikir ulang dampak ke Card/Sheet, bukan lolos ke ZIP). `liquidPill`
disimpan sbg token, akan dipakai LANGSUNG di call site pill-shaped spesifik (tombol/chip) fase
3. 3 titik dispatch diupdate — `colorsFor()`, `typography` when-block, `shapes` when-block —
dikonfirmasi exhaustive 5/5 identitas (grep ulang setelah edit, bukan asumsi compiler bakal
nangkep sendiri krn sandbox ini 0 Gradle).

**Picker Settings — 0 file disentuh**: dicek dulu `SettingsScreen.kt` (`items(ThemeIdentity.
entries...)` + `ThemeOptionCard`'s `previewColors = colorsFor(identity, isDark)`, keduanya
generik) sebelum menyimpulkan — LIQUID_GLASS otomatis muncul di picker + live-preview warna
begitu enum-nya ada, TIDAK perlu edit tambahan, dikonfirmasi baca kode bukan ditebak.

3 file. Brace/paren seimbang (Color.kt 0/0,212/212; Spacing.kt 1/1,20/20 [tidak berubah batch
ini]; Type.kt 0/0,34/34 [tidak berubah]; Theme.kt 13/13,138/138). `FILE_MANIFEST.txt` tidak
berubah (0 file baru). **Belum diverifikasi visual di device** — cek prioritas: Settings → Tema
→ pilih "Liquid Glass", pastikan muncul di picker dgn live-preview warna violet-glass, pilih →
seluruh app pindah ke skema warna+shape+typography baru tanpa crash, 4 tema lama masih utuh
selectable seperti biasa (regresi paling kritis kalau ada — dispatch salah bisa merusak tema
lain, bukan cuma yang baru).

**Item berikutnya (fase 3, roadmap §5)**: terapkan ke komponen inti, urutan dampak-terbesar-dulu
(MiniPlayerBar→NowPlayingScreen→LibraryScreen row→Sheets/Dialog→Settings) — TAPI fase 2 ini
SUDAH otomatis "diterapkan" di level MaterialTheme (semua komponen yang baca
`MaterialTheme.colorScheme`/`.typography`/`.shapes` generik ikut berubah begitu identitas ini
dipilih); fase 3 sebenarnya utk komponen yang TIDAK baca token generik (custom-drawn/hardcoded
efek per-identitas seperti `tactileEmboss()`/`skeuEmboss()`/`calmAberration()` — Liquid Glass
belum punya efek custom serupa, itu scope potensial fase 3 kalau mau tambah bevel/glow khas
sendiri) + titik pill-shape spesifik (`Radius.liquidPill`, lihat catatan Theme.kt di atas).
Detail lengkap: `ROADMAP_LIQUID_GLASS_REDESIGN.md` §5.

## Batch 279 — Liquid Glass §3 dikonfirmasi user + fase 1 eksekusi: fondasi token radius+typography (2 file, additif)
User jawab 3 keputusan besar `ROADMAP_LIQUID_GLASS_REDESIGN.md` §3: **3a→tambah tema ke-5**
(BUKAN rekomendasi dokumen "ganti/konsolidasi" — 4 tema lama tetap ada), **3b→Opsi B dulu**
("Liquid Glass LOOK" shape+typography murni, TANPA blur asli, bertahap per fase), **3c→4
identitas lama TIDAK di-retire** (konsekuensi langsung 3a=tambah, bukan ganti). Semua final,
dicatat di roadmap sebelum eksekusi kode mulai (dokumen diupdate duluan, bukan diasumsikan
diam-diam).

**Fase 1 §5 dieksekusi**: fondasi token, PURELY ADDITIF, 0 wiring ke tema manapun.

**`Spacing.kt`** (diedit) — 2 token radius baru ditambahkan ke `Radius` object yang sudah ada
(bukan object terpisah — object ini shared-pool dipakai semua identitas, pola sudah ada sejak
Batch 54): `liquidLg` (34dp, 1 langkah di atas `hero` 28dp, panel/card besar) + `liquidPill`
(999dp, stadium shape penuh — dp sengaja jauh melebihi tinggi elemen manapun supaya Compose
selalu clamp ke radius maksimum yang mungkin, pola resmi utk stadium shape, bukan angka
sembarang).

**`Type.kt`** (diedit) — `LiquidGlassTypography` baru ditambahkan, struktur SAMA PERSIS
`AppleTypography` (5 slot: titleLarge/titleMedium/bodyMedium/bodySmall/labelSmall) tapi weight
1 tingkat lebih ringan tiap judul (Bold→SemiBold, SemiBold→Medium) + letterSpacing dibuka
positif/mendekati 0 (bukan negatif rapat ala Apple), sesuai riset roadmap "tipografi lebih
ringan". `fontSize`/`lineHeight` SENGAJA dipertahankan sama seperti Apple — ubah hierarki
ukuran teks itu risiko reflow/wrap terpisah, di luar scope fase 1 ("token murni, belum
diterapkan ke komponen").

2 file. Brace/paren seimbang (Spacing.kt 1/1,20/20; Type.kt 0/0,34/34). **Dikonfirmasi grep: 0
pemakaian token baru di luar 2 file definisi** — genuinely unwired, 0 perubahan visual apa pun
di app sampai fase 2. `FILE_MANIFEST.txt` tidak berubah (0 file baru, cuma isi 2 file existing
bertambah).

**Item berikutnya (fase 2, roadmap §5)**: `ThemeIdentity.LIQUID_GLASS` di `Theme.kt` — warna
(palet statis dulu, ekstraksi-dari-artwork Material You kemungkinan batch terpisah), `Shapes`
baru pakai token fase 1, wiring ke `colorsFor()`/`when(identity)` dispatch (3 titik) + entry
picker tema di Settings. Detail lengkap: `ROADMAP_LIQUID_GLASS_REDESIGN.md` §5.

## Batch 278 — Arsipkan dokumentasi stale + roadmap redesign "Liquid Glass" ala CONVX (PERENCANAAN SAJA, 0 kode)
User: "arsipkan dokumentasi yang sudah stale/gak relevan sama sekali dengan pembaruan terkini.
lalu ganti arah goals project menuju 100% tipografi/shape mirip musik player 'CONVX' yang
clean+minimalis atau bahkan bisa lebih baik lagi!! (documentation planning only first)". Dieksekusi
persis sesuai batasan eksplisit terakhir — 0 file `.kt`/`.xml` disentuh, murni dokumentasi.

**Bagian 1 — Arsip** (pola persis `ARCHIVED_ROADMAP_15_FITUR_OFFLINE.md`, isi dipertahankan
penuh + banner arsip di atas, TIDAK dihapus):
- `MICRO_UIUX_AUDIT.md` → `ARCHIVED_MICRO_UIUX_AUDIT.md` — 12/14 kategori TUNTAS (kerja nyata,
  bukan diklaim), tapi mengukur konsistensi terhadap sistem visual LAMA yang akan diganti.
  Ditandai bagian mana yang TETAP reusable (wording, accessibility — identitas-agnostic).
- `POLISH_AUDIT.md` → `ARCHIVED_POLISH_AUDIT.md` — item 1-3 (Motion/Responsive) tetap valid
  sbg catatan historis; item 4-5 sisa (Repeated Components, Typography Final Check) dihentikan
  krn keduanya audit visual thd shape/type-scale yang segera diganti total.

**Bagian 2 — Roadmap arah baru**: `ROADMAP_LIQUID_GLASS_REDESIGN.md` (file baru). Riset CONVX
(`cosmictaserdev-creator/Convx`, GitHub, via web search — bukan asumsi) diringkas: identitas
"Liquid Glass" = real backdrop blur+refraction (dibangun di atas library terpisah
`Kyant0/backdrop`), motion bouncy iOS-style, Material You dari artwork album, 0 widget Material
stock. Dikontraskan ke kondisi project SEKARANG (dicek dari kode, bukan ingatan): 4 identitas
visual ada (`ThemeIdentity` enum — Apple/Tactile/Neumorphism/Calm Retro, `Theme.kt`); temuan
PALING PENTING — `frostedGlass()` (`BlurUtils.kt`) TERNYATA glass PALSU, bukan blur sungguhan
(`Modifier.blur()` Compose blur foreground composable itu sendiri, bukan piksel di belakangnya —
sudah didokumentasikan jujur di komentar kode sejak Batch 53, bukan bug baru); `minSdk=23`
(`app/build.gradle.kts`) jadi kendala nyata krn blur asli (`RenderEffect`) butuh API 31+.

3 keputusan besar diidentifikasi & DISENGAJA TIDAK ditebak sendiri (ditulis sbg rekomendasi +
alasan, tapi tetap minta konfirmasi eksplisit user sebelum sesi mana pun eksekusi kode): (a)
ganti total 4 tema jadi 1 vs tambah sbg tema ke-5, (b) blur sungguhan [perlu bump minSdk 23→31]
vs "Liquid Glass look" tanpa sampling asli, (c) nasib 4 identitas lama kalau (a) = ganti.
Rencana eksekusi 5 fase didraft (reuse pola urutan `POLISH_AUDIT.md` lama: fondasi token →
1 identitas baru utuh → terapkan ke komponen inti urutan dampak-terbesar → keputusan final →
blur asli opsional). `PROJECT_STATE.md` § ATURAN SESI AKTIF rule #4 diupdate (dulu nunjuk
`POLISH_AUDIT.md`, sekarang nunjuk roadmap baru + catatan "jangan eksekusi tanpa konfirmasi
user"). `README.md` § "Rencana v2" ditambah 1 pointer singkat (bukan duplikasi seluruh roadmap).
`FILE_MANIFEST.txt` diperbarui (2 rename, 1 file baru, total +1).

0 kode disentuh sama sekali (instruksi eksplisit user). **Prioritas sesi berikutnya: JANGAN
eksekusi §5 roadmap langsung** — 3 keputusan §3 (a/b/c) WAJIB dikonfirmasi user dulu di chat,
baru mulai fase 1. Detail lengkap ada di `ROADMAP_LIQUID_GLASS_REDESIGN.md` itu sendiri (tidak
diduplikasi penuh di sini, biar 1 sumber kebenaran).

## Batch 277 — Samakan standar informatif GitHub Release body dgn Build Summary Batch 276 (1 protected asset + 1 file kode)
User bandingkan screenshot: Build Summary (Batch 276) sudah rapi & informatif, tapi halaman
Release (Image 2) — yang juga dibaca app via `UpdateCheckSheet.kt` — masih polos cuma pesan
commit doang. Diminta disamakan standarnya.

**Kendala penting yang dijaga**: `release_notes.txt` punya 2 KONSUMEN sekaligus — (1) halaman
web GitHub Release, render Markdown penuh; (2) `UpdateCheckSheet.kt` di app, `Text()` POLOS,
TIDAK render Markdown — sintaks `##`/tabel/`**` bakal tampil MENTAH sebagai karakter literal di
app kalau dipakai. Jadi enrichment SENGAJA plain text, bukan copy gaya Build Summary yang cuma
dibaca CI (Markdown aman di sana).

**2 file**:
1. **`.github/workflows/build.yml`** (protected, edit parsial) — "Capture commit message for
   release notes" sekarang tulis `${tag} (${short_sha})` + baris kosong SEBELUM pesan commit,
   plain text aman di kedua konsumen. YAML divalidasi parse SEGERA setelah edit (pelajaran
   Batch 276 diterapkan — bukan cuma di akhir).
2. **`UpdateCheckSheet.kt`** — prefix versi baru itu REDUNDAN di app (`"Update tersedia:
   ${tagName}"` sudah tampil di atasnya, versi sama persis). Fix: `releaseNotes.substringAfter(
   "\n\n", releaseNotes)` buang prefix sebelum baris kosong pertama, fallback ke teks utuh kalau
   separator tidak ketemu (rilis lama pra-Batch-277 tetap aman, tidak berubah tampilannya).

Brace/paren `UpdateCheckSheet.kt` seimbang (25/25, 67/67). YAML `build.yml` valid, 13 step
`build` job dikonfirmasi identik posisi/nama. **Belum diverifikasi visual** — prioritas cek:
push batch ini, buka halaman Release baru, pastikan body-nya sekarang ada baris versi+SHA di
atas pesan commit; buka app "Cek Update", pastikan TIDAK ada duplikasi versi (cuma pesan commit
polos, sama seperti sebelumnya).

## Batch 276 — Rapikan .github/workflows/build.yml: section header + Build Summary informatif (1 file, edit-parsial protected asset)
Permintaan user: workflow berantakan & tidak informatif. **Scope dijaga ketat sesuai status
protected asset** (Edit Parsial Only) — TIDAK ada logic/command/urutan yang diubah, cuma
ditambah:

1. **5 section-header comment** (`SETUP`/`VERSIONING`/`BUILD & TEST`/`PACKAGE & RELEASE`/
   `DIAGNOSTICS & SUMMARY` di job `build`; `SETUP`/`EMULATOR & TEST` di job `instrumentation-
   tests`) — murni visual wayfinding, 0 pengaruh eksekusi.
2. **Step baru "Publish build summary"** — sebelum ini, halaman ringkasan run GitHub Actions
   (yang muncul otomatis begitu run selesai, SEBELUM klik ke step manapun) SELALU KOSONG TOTAL
   — satu-satunya cara tahu versi/commit yang di-build adalah buka log mentah step "Determine
   version name" satu-satu. Step baru ini nulis tabel Markdown ke `$GITHUB_STEP_SUMMARY` (fitur
   native GitHub Actions): versi/tag, commit SHA, trigger+aktor, baris pertama pesan commit,
   link langsung ke halaman Release. **100% aditif** — tidak menyentuh build/signing/release
   apa pun, `if: always()` supaya tetap tampil bahkan kalau step lain gagal (dengan pesan
   "APK tidak sampai dibuat" alih-alih kosong).

**Insiden kecil selama edit** (dicatat jujur, bukan disembunyikan): 1 percobaan awal (menambah
section-header di step "Report signing status") sempat TIDAK SENGAJA menghapus baris `run: |`
saat replace — LANGSUNG terdeteksi lewat validasi `python3 -c "import yaml..."` yang dijalankan
setelah SETIAP edit tunggal (bukan cuma di akhir), dan langsung diperbaiki sebelum lanjut ke
edit berikutnya. File final divalidasi ulang penuh: parse YAML sukses, urutan & nama SEMUA 18
step (12 job `build` + 6 job `instrumentation-tests`) dikonfirmasi identik dengan sebelum edit.

1 file (`.github/workflows/build.yml`, protected asset — edit parsial, TIDAK dirombak total).
0 file lain. **Belum diverifikasi run CI sungguhan** — prioritas cek: push batch ini, buka tab
Actions run yang baru, pastikan section "Summary" (bukan cuma daftar step) menampilkan tabel
versi/commit/release seperti dijelaskan di atas.

## Batch 275 — POLISH_AUDIT.md kategori 4: audit Button lintas screen, 4 gap nyata (3 diperbaiki, 1 pending) (3 file kode + 3 dokumentasi, cap DILEWATI 1x — 1 task kohesif)
Sub-item pertama kategori "Repeated Components": bandingkan SEMUA `Button(` (bukan Icon/Text/
Outlined) di 26 file `ui/*.kt` — bukan visual/screenshot (belum bisa), tapi mekanis & terbukti
lewat grep: kehadiran `bouncyPress` (tap-feedback standar app-wide sejak Motion/Batch 256).

**10 titik ditemukan**. **2 awalnya dikira gap TAPI FALSE POSITIVE** (`LyricsSheet.kt` tombol
"Mark"/`SongInfoEditSheet.kt` tombol "Simpan" — modifier `bouncyPress`-nya ada, cuma di baris
lebih jauh dari window awal pengecekan; dicek ulang lebih lebar sebelum disimpulkan, bukan
asumsi). **4 gap NYATA**: `LibraryScreen.kt` `EmptyState` CTA, `SongPickerSheet.kt` tombol
konfirmasi, `SmartPlaylistScreen.kt` tombol simpan, `LyricsSheet.kt:214` tombol "Simpan" edit
manual lirik.

**3 diperbaiki batch ini** (cap 3 file kode): pola identik persis existing (`VaultSheet.kt`
dijadikan referensi) — `val xInteraction = remember { MutableInteractionSource() }` +
`interactionSource = xInteraction` + `modifier = Modifier.bouncyPress(xInteraction)`, + import
`MutableInteractionSource` di 2 file yang belum punya (`LibraryScreen.kt`/`SmartPlaylistScreen.
kt`; `SongPickerSheet.kt` juga ditambah). `LibraryScreen.kt`'s `EmptyState` fix berdampak PALING
LUAS (composable dipakai banyak screen dengan CTA — 1 fix nyebar otomatis). **1 PENDING**
(`LyricsSheet.kt:214`) — di luar cap, batch berikutnya.

Brace/paren ketiga file kode seimbang: `LibraryScreen.kt` (351/351, 786/786), `SongPickerSheet.kt`
(48/48, 133/133), `SmartPlaylistScreen.kt` (104/104, 254/254). 0 protected asset. **Cap 3-file/
batch DILEWATI 1x** (total 6 file: 3 kode + `CHANGELOG.md`/`PROJECT_STATE.md`/`POLISH_AUDIT.md`)
— alasan sama presedan Batch 156: 1 task kohesif (audit+fix 1 sub-kategori), bukan gabungan
task independen, dan dokumentasi WAJIB tiap batch (aturan tetap, tidak boleh di-skip demi cap).

**Belum diverifikasi visual** — perubahan micro-interaction kecil (scale-on-press), risiko
rendah, pola sudah terbukti jalan di 6 titik lain yang sudah ada.

## Batch 274 — POLISH_AUDIT.md: audit disabled/selected state lintas screen — 0 bug (2 dokumentasi, 0 kode)
Item teratas kategori Surface/Color yang masih `[ ]` (Motion & Responsive sudah tuntas). 4
kategori state diperiksa via grep menyeluruh `ui/*.kt`:
1. **Disabled** (tombol reorder/remove non-aktif) — `secondary.copy(alpha=0.3f)`, konsisten 5/5
   titik (`PlaylistScreen.kt`+`QueueSheet.kt`), sengaja override auto-dim `IconButton` bawaan M3.
2. **isPlaying** (badge sedang diputar) — `primary.copy(alpha=0.12f)` bg + teks bold `primary`,
   konsisten 4/4 file (sudah diverifikasi Batch 198-199 sebelumnya, dikonfirmasi ulang di sini).
3. **isSelected** (checkbox multi-pilih) — 0 warna di SEMUA 3 implementasi independen
   (`LibraryScreen.kt`/`SongPickerSheet.kt`/`DuplicateFinderSheet.kt`), checkbox-glyph-only —
   TIDAK tabrakan dgn kategori 2 meski 1 baris bisa kena keduanya sekaligus (playing+selected),
   karena kategori ini tidak pakai warna apapun buat direbut.
4. **Tab-chip selected** — solid `primary` bg + `onPrimary`, SENGAJA beda dari kategori 2 (chip
   kecil butuh kontras penuh, row-highlight butuh wash halus) — beda peran UI, bukan tabrakan.

**Kesimpulan: 0 bug** — tiap kategori konsisten penuh di dalam dirinya sendiri, tidak ada 1
warna dipakai utk 2 makna berbeda. Checkbox `[x]` di `POLISH_AUDIT.md`, TIDAK menciptakan
kerjaan baru demi "menemukan sesuatu" (sesuai instruksi eksplisit dokumen itu sendiri).

2 dokumentasi (`CHANGELOG.md`/`POLISH_AUDIT.md`), 0 kode. Item berikutnya kategori Surface/Color:
"Jangan redesign theme" (guardrail, bukan task) — kategori Surface/Color sekarang TUNTAS,
lanjut kategori 4 (Repeated Components) batch berikutnya.

## Batch 273 — Fix "select→instant self-deselect" di SongPickerSheet (Favorit/Playlist add), PORT dari Batch 271 (1 file kode)
User laporan (screenshot 2 sheet: "Tambah ke Favorit" & "Tambah ke Playlist"): masih kena
instant-cancel pas long-press diam. **Dikonfirmasi lewat baca kode langsung** (bukan tebak):
`SongPickerSheet.kt` masih punya bug PERSIS sama yang baru dibetulkan Batch 271 di `SongListView`
(`LibraryScreen.kt`) — TAPI fix itu TIDAK PERNAH DI-PORT ke sini, karena `SongPickerSheet.kt`
punya gesture DUPLIKAT sendiri sejak Batch 268 (bukan didelegasikan/reuse dari LibraryScreen).

**Root cause identik**: `onDragStart` di sini tidak pernah `.consume()` — tekan-lama diam (tanpa
gerak) bikin `onDrag` (satu-satunya yang consume) tidak pernah jalan, sentuhan asli lolos ke
`.clickable` Row, yang langsung membalik toggle yang baru saja di-set `onDragStart`. Select →
instant self-deselect, persis kasus Batch 271.

**Fix**: pola `suppressClickForId` di-port PERSIS (bukan reimplementasi beda gaya) — diisi di
`onDragStart`, ditelan sekali di `.clickable` Row kalau id cocok, dibersihkan di `onDrag` (gerak
asli terkonfirmasi) & `onDragCancel` (jaring pengaman) — SENGAJA TIDAK di `onDragEnd`, alasan
sama Batch 271 (ordering vs klik-hantu tidak terjamin).

1 file (`SongPickerSheet.kt`), 0 protected asset. Brace/paren seimbang (47/47, 130/130). Fix
NestedScrollConnection (Batch 270) + confirmValueChange (Batch 269) TIDAK disentuh — masih
relevan (beda kelas masalah: itu sheet-vs-scroll, ini within-row click-vs-longpress). **⚠️
Prioritas verifikasi**: tes PERSIS skenario screenshot — buka "Tambah ke Favorit"/"Tambah ke
Playlist", long-press 1 lagu TANPA gerak sama sekali, pastikan checkbox TETAP tercentang (bukan
balik kosong).

## Batch 272 — Fitur: selectionMode WAJIB persist meski selectedIds kosong, keluar cuma lewat tombol Close manual (1 file kode)
Permintaan user eksplisit: kalau user long-press (masuk selectionMode) lalu diam, atau bahkan
iseng deselect lagu pertama yang di-long-press (selectedIds balik ke 0), tab TIDAK BOLEH auto-
kembali ke tampilan normal — satu-satunya jalan keluar WAJIB lewat tombol Close manual di
`SelectionActionBar`. Berlaku ke SEMUA logic terkait, bukan setengah-setengah.

**Audit dulu, baru fix** (`grep "selectionMode"` di seluruh `ui/`): cuma `LibraryScreen.kt` yang
punya konsep selectionMode toggleable (`SongPickerSheet.kt` checkbox SELALU tampil sejak Batch
268, tidak ada "mode" yang bisa keluar-masuk, jadi tidak relevan/tidak disentuh). Di dalam
`LibraryScreen.kt`, cuma SATU titik yang auto-exit: `toggleSelect()` punya baris
`if (selectedIds.isEmpty()) selectionMode = false` — dipakai SEMUA 4 tab (Lagu/Favorit/Artist/
Folder) via arsitektur delegasi 1 fungsi (Batch 197/271), jadi 1 fix ini otomatis berlaku ke
ke-4 nya sekaligus (dikonfirmasi: baris `onToggleSelect = { id -> toggleSelect(id) }` identik
di 4 call site).

**2 perubahan**:
1. `toggleSelect()` — baris auto-exit DIHAPUS. `exitSelectionMode()` (dipanggil `onClose`
   `SelectionActionBar`) sekarang SATU-SATUNYA tempat yang men-set `selectionMode = false`
   (dikonfirmasi lewat grep ulang setelah edit).
2. **Hardening supaya tidak setengah-setengah**: `SelectionActionBar` — count SEKARANG BISA 0
   (state baru yang sebelumnya mustahil). 3 tombol aksi massal (Tambah ke Playlist/Sembunyikan/
   Hapus) di-disable saat `count==0` — bukan kosmetik, mencegah `bulkHide()`/`bulkDelete()`
   beneran jalan atas 0 lagu (potensi dialog konfirmasi "hapus 0 lagu" yang janggal). Tombol
   Close TETAP selalu aktif — itu satu-satunya jalan keluar sah sekarang.

**Sengaja TIDAK disentuh**: `bulkHide()`'s `exitSelectionMode()` di akhir — itu keluar akibat
AKSI SELESAI (deliberate, ditekan tombol), bukan "auto-cancel karena kosong" yang dikeluhkan
user, beda kasus, tetap benar dipertahankan.

1 file (`LibraryScreen.kt`), 0 protected asset. Brace/paren seimbang (350/350, 781/781). **Belum
diverifikasi visual** — prioritas cek: long-press 1 lagu lalu deselect lagu itu sendiri, pastikan
SelectionActionBar TETAP tampil (count: 0 dipilih, 3 tombol aksi abu-abu/disabled), baru hilang
setelah tombol Close ditekan.

## Batch 271 — Fix ROOT CAUSE sweep-select "auto-cancel diri sendiri" saat long-press TANPA gerak (1 file kode + 2 dokumentasi)
User kasih root cause presisi: long-press yang TIDAK dilanjut sweep sama sekali (tekan-tahan-
lepas, 0 gerakan) terkesan auto-cancel/oversensitif — beda dari bug Batch 268-270 (yang soal
`ModalBottomSheet` ganggu SAAT drag aktif). Ini soal kasus TANPA drag sama sekali, di
`SongListView` (`LibraryScreen.kt`, dipakai tab Lagu/Favorit + `GroupedListView` Artist/Folder
via delegasi — 1 titik fix, 4 tab kebagian).

**Diriset & dikonfirmasi via pembacaan kode + pengetahuan `detectDragGesturesAfterLongPress`
Compose**: `onDragStart` MEMANG sudah benar (Batch 72) — begitu long-press dikenali, langsung
`onSweepSelectRange(...)` pilih baris itu + `selectionMode=true`. Tapi `onDragStart` cuma terima
`Offset`, BUKAN `PointerInputChange` — TIDAK PERNAH manggil `.consume()`. Kalau jari lepas tanpa
gerak sama sekali, `onDrag` (satu-satunya tempat yang `consume()`) TIDAK PERNAH jalan — jadi
sentuhan turun-lalu-naik ITU SENDIRI tidak pernah dikonsumsi siapa pun. `SongRow`'s `clickable`
polos (`onClick` doang, `onLongClick` sudah dihapus Batch 72) MASIH mengintip pasangan
turun-naik yang sama itu — `clickable` tanpa `onLongClick` tidak punya timing durasi sendiri,
tekan-lama-lalu-lepas-tanpa-gerak tetap sah sebagai "klik". Klik itu nyusul SEPERSEKIAN DETIK
setelah `onDragStart`, dan karena `selectionMode` SUDAH `true` (baru saja di-set), baris
`if (selectionMode) onToggleSelect() else onClick()` di `SongRow` malah rute ke
`onToggleSelect()` — **membalik baris yang BARU SAJA terpilih itu balik ke tidak-terpilih**.
Select → instant self-deselect = kelihatan kayak 0 kejadian.

**Fix**: latch `suppressClickForId` (state baru `SongListView`) — diisi `songs[idx].id` persis
di `onDragStart` (baris yang sama yg baru dipilih). Di titik panggil `SongRow`, `onClick` DAN
`onToggleSelect` sama-sama dibungkus: kalau `suppressClickForId == song.id` → telan sekali
(`= null`) TANPA jalanin apa pun; kalau tidak cocok → jalan normal seperti biasa. Latch
dibersihkan di `onDrag` (begitu ADA gerakan asli — brarti bukan kasus stasioner, klik hantu
child tidak akan pernah nyampe krn `clickable` batal sendiri kena touch-slop) dan di
`onDragCancel` (jaring pengaman kalau gesture dibatalkan ancestor sebelum salah satu jalur di
atas sempat jalan). SENGAJA TIDAK dibersihkan di `onDragEnd` — urutan klik-hantu vs `onDragEnd`
antar 2 coroutine gesture terpisah tidak terjamin, membersihkan di situ berisiko menghapus latch
SEBELUM klik sempat mengeceknya, yang justru menghidupkan lagi bug yang sama.

1 file kode (+13 baris net: 1 state, 1 assignment, 1 clear di `onDrag`, 1 clear di
`onDragCancel`, 2 lambda dibungkus di call site — semua di dalam `SongListView`, 0 sentuh
`SongRow`/`GroupedListView`/`SearchResultsView` langsung karena arsitektur sudah delegasi sejak
Batch 197). 0 file baru, 0 protected asset. Brace/paren `LibraryScreen.kt` seimbang (350/350,
771/771).

**Bonus housekeeping ketemu pas cek integritas**: `FILE_MANIFEST.txt` ternyata sudah basi sejak
Batch 266 — `SongPickerSheet.kt` (file baru batch itu) tidak pernah ditambahkan ke daftar,
184→185 dibetulkan sekalian (bukan disengajakan skip, ketauan pas `find` vs manifest count
mismatch di awal batch ini).

**Belum diverifikasi visual — PALING PRIORITAS dari semua batch belakangan**: ini fix gesture
inti yang dipakai 4 tab sekaligus, tolong tes: (1) long-press TANPA gerak sama sekali → baris
harus TETAP terpilih (bukan balik kosong), (2) long-press LALU sweep beberapa baris → tetap
akurat seperti biasa (Batch 1 v263 hysteresis tidak boleh regresi), (3) tap biasa (bukan
long-press) di baris LAIN saat sudah ada row lain kepilih dari sweep sebelumnya → toggle normal,
tidak ikut ketelan.

## Batch 270 — Fix sweep-select oversensitif SongPickerSheet, TAKE 2: NestedScrollConnection (bukan confirmValueChange) (1 file kode)
User konfirmasi: fix Batch 269 (`confirmValueChange`) TIDAK cukup, "masih kejadian". Diriset
ulang (bukan tebak lagi) — pola dikenal luas di Material3 `ModalBottomSheet`+`LazyColumn`
bersarang: sheet pakai `anchoredDraggable`, yang MEMPROSES delta drag secara VISUAL duluan
setiap kali `LazyColumn` kehabisan sisa scroll buat dikonsumsi (bukan cuma pas user coba
dismiss — bereaksi ke SETIAP delta "sisa" yang lolos, termasuk gerakan super kecil pas fase
tunggu long-press). `confirmValueChange` cuma menolak STATE AKHIRNYA — gerakan visual yang
mengganggu gesture long-press kita tetap sudah kejadian duluan, itu sebabnya Batch 269 gagal.

**Fix yang benar** (dikonfirmasi lewat riset — bukan spekulasi): `NestedScrollConnection` custom
dipasang di content wrapper (`Column`) via `.nestedScroll(sheetScrollConnection)`,
`onPostScroll` mengembalikan SEMUA sisa delta vertikal (`available.copy(x=0f)`) — sheet jadi
TIDAK PERNAH kebagian delta apapun buat mulai drag-nya sendiri, bukan direaksi-lalu-ditolak
belakangan seperti `confirmValueChange`. `isSweeping`+`confirmValueChange` (Batch 269) TETAP
dipertahankan sebagai lapisan pengaman tambahan (tidak mengganggu, tinggal jaga-jaga).

1 file (`SongPickerSheet.kt`), 0 protected asset, +3 import baru (`NestedScrollConnection`/
`NestedScrollSource`/`nestedScroll`). Brace/paren seimbang (46/46, 119/119). Murni Kotlin/
Compose stdlib, tidak butuh naikkan Material3. **Belum diverifikasi visual** — kalau MASIH
kejadian setelah ini, kemungkinan besar penyebabnya bukan lagi soal sheet-vs-scroll (sudah
ditangani via mekanisme resmi Compose), butuh video/rekaman gesture persis buat diagnosis lebih
lanjut, bukan tebak konsep lagi.

## Batch 269 — Fix sweep-select oversensitif di SongPickerSheet (bukan di tab Lagu) (1 file kode)
User laporan: sebagian sweep-select normal, sebagian lagi kelewat sensitif — sampai membatalkan
diri sendiri sebelum sempat kepakai. Dikonfirmasi: `SongListView` (`LibraryScreen.kt`, tab Lagu)
= normal, `SongPickerSheet.kt` (Batch 266-268, dipakai FAB Favorit/Playlist) = oversensitif.

**Root cause**: `SongPickerSheet` dibungkus `ModalBottomSheet`, yang punya gesture
swipe-to-dismiss BAWAAN aktif di seluruh permukaan sheet — bersaing langsung dengan long-press+
drag sweep-select buat event pointer vertikal yang sama. `SongListView` di layar biasa (bukan
sheet) TIDAK punya pesaing gesture sejenis sama sekali — itu bedanya kenapa cuma satu yang kena.

**Fix**: `isSweeping` (state baru) diset `true` begitu long-press sweep berhasil (`onDragStart`),
`false` lagi begitu selesai (`onDragEnd`/`onDragCancel`). `sheetState` dapat `confirmValueChange
= { !isSweeping }` — sheet MENOLAK semua perubahan state (termasuk dismiss akibat swipe) selama
sweep aktif. Murni state Kotlin, tidak butuh naikkan versi Material3 (BOM 2024.05.00/~1.2.1
tetap, `confirmValueChange` sudah tersedia di versi ini).

1 file (`SongPickerSheet.kt`), 0 protected asset. Brace/paren seimbang (43/43, 112/112).
**Belum diverifikasi visual** — prioritas cek: sweep-select di sheet Favorit/Playlist sekarang
seharusnya sama mulusnya dengan tab Lagu, sheet tidak lagi ketutup sendiri di tengah drag.

## Batch 268 — SongPickerSheet: layar lebih luas + sweep-select (1 file kode)
User laporan (dipakai lewat FAB Favorit & Playlist, Batch 266-267): sheet kecil (capped 420dp)
& 0 sweep-select — checklist manual 1-per-1 gak praktis buat banyak lagu sekaligus. Fix
`SongPickerSheet.kt`: (1) sheet diperbesar `fillMaxHeight(0.92f)` (hampir setinggi layar,
sebelumnya cuma wrap-content+capped), `LazyColumn` pakai `weight(1f)` ngisi ruang sisa; (2)
sweep-select di-port 1:1 dari `SongListView` (`LibraryScreen.kt`) — tekan-lama 1 row lalu geser
buat centang banyak lagu sekaligus, TERMASUK hysteresis 6dp anti-jitter & `DisposableEffect`
cleanup bounds (row yang di-recycle LazyColumn gak nyisain bounds basi) yang udah battle-tested
di situ. Beda dari `SongListView`: di sini gak perlu param `selectionMode` terpisah (checkbox
SELALU tampil tiap row, jadi sweep langsung nambah ke `selected`, bukan toggle mode dulu).
Brace/paren seimbang (41/41, 105/105). 0 protected asset. **Belum diverifikasi visual** —
prioritas cek sweep tetap akurat pas list difilter query pencarian (rowBoundsInRoot di-reset
tiap `filtered` berubah identity).

## Batch 267 — FAB shortcut "Tambah lagu" di detail Playlist (2 file kode)
Menutup Pending Queue Batch 266. `PlaylistScreen.kt` (`PlaylistTabView`): param baru
`onAddSongToPlaylist: (String, Long) -> Boolean` + `onInfoMessage: (String) -> Unit` (dua-duanya
udah tersedia di scope `LibraryScreen.kt`, tinggal diteruskan — 1 call site doang, dicek via
grep). Detail playlist (baik kosong maupun udah terisi) dibungkus `Box` + `FloatingActionButton`
(ikon `+`, BottomEnd) yang buka `SongPickerSheet` (reuse komponen Batch 266) —
`alreadyAddedIds = selectedPlaylist.songIds.toSet()` nyaring lagu yang udah ada,
`onConfirm` loop `onAddSongToPlaylist` per id + hitung `addedCount` (bisa < jumlah dipilih kalau
race duplikat) + toast. FAB "Buat playlist baru" (`+`) di LIST playlist (bukan detail) TIDAK
disentuh — beda konteks/tujuan, tetap seperti sebelumnya. Brace/paren kedua file seimbang
(`PlaylistScreen.kt` 141/141,241/241; `LibraryScreen.kt` 349/349,755/755). 0 protected asset.
**Belum diverifikasi visual.**

## Batch 266 — FAB shortcut "Tambah ke Favorit" + SongPickerSheet reusable (2 file kode + 1 dokumentasi)
User laporan screenshot (tab Favorit & Playlist kosong): satu-satunya cara nambah lagu
sebelumnya WAJIB muter ke tab Lagu dulu, cari manual, tekan-lama, baru pilih "Tambah ke
Favorit/Playlist" — nggak ada shortcut langsung dari tab tujuan. **File baru**
`SongPickerSheet.kt`: bottom sheet generik reusable — cari + checklist banyak lagu sekaligus,
`alreadyAddedIds` otomatis nyaring yang udah ada (gak checklist ulang), `onConfirm` dipanggil
SEKALI dengan list id terpilih (bukan per-toggle terpisah). **`LibraryScreen.kt`**: tab Favorit
(`selectedTab==4`) dibungkus `Box` + `FloatingActionButton` (ikon hati, BottomEnd, disembunyikan
saat `selectionMode` aktif biar gak tabrakan sama `SelectionActionBar`) yang buka
`SongPickerSheet` — `onConfirm` loop `onToggleFavorite` tiap id + toast jumlah. FAB tetap
muncul walau daftar udah terisi (bukan cuma solusi pas kosong doang, biar nambah lanjutan juga
gampang). Brace/paren kedua file seimbang (`LibraryScreen.kt` 349/349,755/755;
`SongPickerSheet.kt` 24/24,69/69). 0 protected asset. **Belum diverifikasi visual.**

**Pending Queue**: FAB serupa buat tab Playlist (di dalam detail playlist) — belum dikerjakan
batch ini demi cap 3 file (butuh sentuh `PlaylistScreen.kt` juga, `onAddSongToPlaylist` belum
diteruskan ke `PlaylistTabView`). Kandidat batch berikutnya.

## Batch 265 — Fix SUNGGUHAN "gak bisa pilih lagu": `showMenu` dropdown SongRow unreachable
User koreksi Batch 264: "gak bisa pilih lagu langsung dari tab favorit, begitu pula tab
playlist!!". Root cause ASLI ditemukan — BUKAN gap per-tab seperti dugaan Batch 262/264: `var
showMenu by remember { mutableStateOf(false) }` di `SongRow` (`LibraryScreen.kt`) TIDAK PERNAH
di-set `true` di MANA PUN — grep `showMenu` di seluruh file cuma nongol di deklarasi + di dalam
`DropdownMenu` itu sendiri (0 trigger). `DropdownMenu` ini isinya "Putar Berikutnya", "Tambah ke
Antrean", "Tambah ke Playlist", "Sembunyikan", **"Pilih"** (= `onEnterSelectionMode()`, ini yang
user cari), "Hapus dari Perangkat" — SEMUANYA unreachable, bukan cuma "Pilih". Konsekuensi:
`SongRow` dipakai ulang di SEMUA tab (Lagu/Favorit/Artis/Folder/Search — 1 composable, komentar
Batch 133 sudah confirm ini), jadi bug ini genuinely lintas-tab, cuma user kebetulan
mengalami/melaporkannya lewat Favorit & Playlist duluan.

Fix (`LibraryScreen.kt`, 1 file): tambah `IconButton(onClick = { showMenu = true })` ber-ikon
`Icons.Default.MoreVert` (import baru), ditaruh setelah tombol favorit-heart, di dalam blok
`if (!selectionMode)` yang sama (konsisten — opsi overflow memang tidak relevan begitu sudah
dalam mode seleksi). Sekarang "Pilih" beneran bisa di-tap dari mana pun `SongRow` muncul.

**Playlist tab TETAP belum tersentuh** — itu `PlaylistTabView`, composable lain total, sama
sekali tidak lewat `SongRow`, jadi fix ini TIDAK menjangkaunya. Masih butuh keputusan desain
terpisah (dicatat Batch 264): sweep-select buat pilih banyak PLAYLIST sekaligus, atau di dalam
tampilan lagu-per-playlist (drill-down)?

Brace/paren `LibraryScreen.kt` seimbang (339/339, 740/740). 1 import baru (`MoreVert`), 0 file
baru/hapus, `FILE_MANIFEST.txt` tidak berubah. 0 protected asset. **Belum diverifikasi visual di
device** — prioritas kalau user push: buka tab Lagu (paling gampang ada isinya), tap "..." di
sebuah lagu, pastikan menu muncul dan "Pilih" beneran masuk mode seleksi (checkbox nongol).

## Batch 264 — Pending Queue item 2: fix sweep-select over-sensitif tab Lagu (standar iOS)
Lanjutan sesi baru (`AudioPlayer_v263_Batch1.zip` sbg source-of-truth, hard reset konteks per
protokol). User: "lanjutkan progress pending urgent". Pending Queue tersisa 2 item dari Batch
262 — dieksekusi item PALING krusial dulu sesuai Strict Micro-Batching (bug aktif mengganggu
fitur yang SUDAH ada > gap fitur di tab yang belum tentu perlu sweep-select sama sekali):

**Item 2 (dikerjakan) — sweep-select tab Lagu over-sensitif**: `SongListView`'s
`detectDragGesturesAfterLongPress` (`LibraryScreen.kt`) memindah `sweepLastIndex` PERSIS saat Y
melewati garis batas 1px row berikutnya — tremor jari normal saat nahan posisi dekat garis batas
terbaca sebagai berkali-kali "melewati batas", jadi seleksi flicker in/out di row yang user tidak
pernah maksud sentuh. Fix: `hysteresisPx` (6dp) — begitu 1 row committed, touch harus lewat
SEJAUH ITU dari batas row sebelumnya (bukan cuma 1px) sebelum row berikutnya boleh commit. Swipe
cepat/sengaja tidak berubah sama sekali (jarak hysteresis kelewat trivial), cuma meredam kasus
jitter-kecil-dekat-batas. 1 file (`LibraryScreen.kt`), gesture ini SATU-SATUNYA implementasi
`detectDragGesturesAfterLongPress` di file ini — dipakai bareng oleh tab Lagu (0) DAN tab
Favorit (4), keduanya lewat `SongListView` yang sama, jadi ke-fix otomatis di keduanya tanpa
edit terpisah.

**Item 1 (koreksi catatan, TIDAK butuh kerjaan)** — dicek ulang kode sungguhan sebelum eksekusi:
klaim "sweep-select belum ada di tab Favorit" di Pending Queue Batch 262 sudah **BASI/salah** —
tab Favorit (`selectedTab == 4`) sudah pakai `SongListView` yang sama dgn `onSweepSelectRange`
terpasang penuh (baris ~337), diverifikasi lewat `primaryLabels`/`moreLabels` di baris 748-749.
Sisa PR sebenarnya CUMA tab Playlist (`selectedTab == 5`, `PlaylistTabView` — composable beda
total, daftar PLAYLIST bukan daftar lagu flat, jadi "sweep-select" di sana bukan sekadar
nyambungin parameter yang sudah ada, perlu desain terpisah) — dicatat ulang sebagai Pending Queue
yang benar di bawah, bukan diasumsikan sama kayak Favorit.

Brace/paren `LibraryScreen.kt` seimbang (337/337, 733/733). `FILE_MANIFEST.txt` tidak berubah (0
file baru/hapus). 0 protected asset disentuh. **Belum diverifikasi visual di device** — gesture
sensitif sulit dinilai lewat baca kode doang, prioritas paling atas kalau user push: coba sweep
pelan-pelan dekat garis batas row (skenario yang tadinya flicker), pastikan sekarang halus tapi
swipe cepat tetap responsif normal (bukan malah jadi lag/nge-lag).

## Batch 263 — Follow-up fix: scroll "bouncy" di sheet Buat Playlist Otomatis (1 file)
User konfirmasi fix Batch 262 berhasil (tombol Batal/Simpan sekarang terjangkau), TAPI lapor
scroll terasa "agak bouncy". Root cause: `verticalScroll` yang baru ditambahkan otomatis ikut
overscroll stretch-glow bawaan Android 12+/Compose Foundation — di dalam `ModalBottomSheet` yang
JUGA punya gesture drag-to-dismiss sendiri, stretch effect itu terasa berlebihan/ganda (2 sistem
gesture bertumpuk).

**`SmartPlaylistScreen.kt`** (diedit) — `Column` dibungkus `CompositionLocalProvider
(LocalOverscrollConfiguration provides null)`, scope CUMA di Column ini (bukan seluruh app,
bukan seluruh sheet — tombol Batal/Simpan di luar Column pun tidak ikut terkena). Compose BOM
project ini 2024.05.00 — `LocalOverscrollConfiguration` masih API terkini utk versi ini (bukan
workaround usang; parameter `overscrollEffect` langsung di `verticalScroll()` baru ada di BOM
lebih baru yang belum dipakai project ini). Scroll drag/fling sendiri TIDAK diubah, cuma efek
visual stretch-glow-nya yang dimatikan.

1 file. Brace/paren seimbang (103/103, 252/252). `FILE_MANIFEST.txt` tidak berubah. **Belum
diverifikasi visual di device** — cek: scroll sheet ini sampai mentok atas/bawah, pastikan 0
lagi efek stretch/bounce, scroll tetap responsif normal.

## Batch 262 — Bug fix urgent: sheet "Buat Playlist Otomatis" truncated & 0 scrollable (1 file)
User lapor + screenshot: sheet `SmartPlaylistScreen.kt` (bikin playlist otomatis) kepotong di
tengah field "Rating minimum", tombol Batal/Simpan di paling bawah sama sekali tidak terjangkau,
0 bisa discroll. Root cause: `Column` pembungkus SEMUA field (nama+folder chips+genre chips+
durasi+tahun+rating+tombol) TIDAK punya `verticalScroll` sama sekali — konten yang lebih tinggi
dari sheet ke-clip diam-diam. **Root cause & fix persis sama Batch 112**
(`NowPlayingScreen.kt`, kasus sheet overflow silent-clip yang sama).

**`SmartPlaylistScreen.kt`** (diedit) — `.verticalScroll(rememberScrollState())` ditambahkan ke
`Column` dialog (+2 import). `LazyRow` folder-chips/genre-chips di dalamnya TIDAK konflik
(scroll horizontal, beda axis dari Column vertical) — dicek eksplisit sebelum edit, bukan
tebakan (nested `LazyColumn` VERTICAL di dalam `verticalScroll` Column akan crash "infinity
height", makanya perlu dicek dulu).

1 file. Brace/paren seimbang (102/102, 246/246). `FILE_MANIFEST.txt` tidak berubah. **Belum
diverifikasi visual di device** — cek prioritas: buka Playlist → Otomatis → Buat Baru, pastikan
sekarang bisa discroll sampai tombol Simpan, konten tidak kepotong lagi.

**Pending Queue (2 item dari laporan user, ditunda ke batch berikutnya sesuai Strict
Micro-Batching)**:
1. Sweep-select belum ada di tab Favorit & Playlist (saat ini cuma di tab Lagu, `LibraryScreen.kt`).
2. Sensitivitas sweep-select (yang sudah ada di tab Lagu) perlu dikonfigurasi ulang ke standar
   iOS — user bilang saat ini "over sensitivitas".

## Batch 261 — POLISH_AUDIT #8 § Surface/Color item 2: samakan treatment border/divider lintas screen (2 file, 3 bug fix)
Item kedua § Surface/Color Consistency. Survey `HorizontalDivider(...)` di seluruh `ui/*.kt`: 24
titik/10 file. **20 dari 24 eksplisit `color = MaterialTheme.colorScheme.surfaceVariant`** —
mayoritas jelas & konsisten. **3 titik (2 file) TIDAK set `color` sama sekali**
(`DuplicateFinderSheet.kt` x2, `VaultSheet.kt` x1) — otomatis jatuh ke default M3
`DividerDefaults.color` (`colorScheme.outlineVariant`, TOKEN BEDA dari `surfaceVariant`, meski
di banyak tema keduanya kebetulan mirip nilainya, keduanya bukan token yang sama secara
semantik). Beda dari temuan Batch 260 (5 file, blast radius besar, genuinely ambigu) — ini
**mayoritas 20:3 jelas, blast radius kecil (2 file), 0 risiko perilaku** (murni parameter warna
sebuah divider statis) — **langsung dieksekusi**, bukan dicatat sebagai observasi.

**`DuplicateFinderSheet.kt`** (diedit, 2 baris) — `color = MaterialTheme.colorScheme.
surfaceVariant` ditambahkan ke 2 `HorizontalDivider` (pemisah antar grup duplikat).
**`VaultSheet.kt`** (diedit, 1 baris) — sama, 1 `HorizontalDivider` (pemisah antar lagu vaulted).

2 file, 3 bug fix. Brace/paren kedua file seimbang. `FILE_MANIFEST.txt` tidak berubah
(184/184). **Belum diverifikasi visual di device** — cek prioritas: buka Duplicate Finder &
Vault, pastikan divider sekarang match warna divider di layar lain (mis. Library/Settings),
bukan lebih tipis-kontras dari sebelumnya (kalau tema device kebetulan bikin outlineVariant≈
surfaceVariant, perubahan visual mungkin nyaris tak kentara — itu tetap benar, cuma jadi tidak
dramatis). Item berikutnya § Surface/Color: audit disabled/selected state lintas screen.

## Batch 260 — POLISH_AUDIT #7 § Surface/Color, item 1: audit background→surface→elevated surface (0 code, observasi baru)
Item pertama § Surface/Color Consistency. Survey `colorScheme.background`/`.surface`/
`.surfaceVariant` di seluruh `ui/*.kt` (26 file): `background` cuma dipakai 3x (semua di
`NowPlayingScreen.kt`), `surface` polos 6 titik tersebar 1 file masing-masing, `surfaceVariant`
lebih luas (11 file). `surfaceContainer*` (token M3 lebih baru khusus elevated-surface) **0
pemakaian sama sekali** — project ini genuinely tidak pakai token itu, elevasi ditangani lewat
`Surface(tonalElevation=...)` manual, bukan lewat role warna terpisah.

**5 titik `Surface` "kartu pembungkus konten"** ditemukan (Home/Library/NowPlaying/
StatsDashboard/Settings) — SEMUA pola identik: `color = colorScheme.surface` (Transparent utk
tema Panel), `contentColor = onSurface` eksplisit (pelajaran Batch 48/49, sudah konsisten).
**TAPI nilai `tonalElevation` beda tanpa penjelasan semantik**: StatsDashboard 2dp, Home &
Settings 4dp, Library & NowPlaying 6dp — 0 komentar di kode manapun yg menjustifikasi kenapa
beda (beda dari "Batch 48/49 lesson" yg didokumentasikan utk bagian contentColor-nya). Kelima
Surface ini secara struktural fungsinya sama (bungkus 1 blok konten jadi kartu), bukan
hierarki visual yg jelas beda tingkat.

**TIDAK dieksekusi batch ini** — pola sama Batch 162/163/165 (EmptyState icon, LibraryFilterChips
fill, banner 3-arah sebelum disatukan): blast radius 5 file, genuinely bisa jadi disengaja
(StatsDashboard mungkin memang dimaksud lebih "flat"/sekunder dari Library/NowPlaying yang jadi
fokus utama), dicatat sebagai observasi tertunda keputusan user, bukan diasumsikan.

0 file kode, 1 dokumentasi (`POLISH_AUDIT.md`). `FILE_MANIFEST.txt` tidak berubah (184/184).
Item berikutnya § Surface/Color: samakan treatment border/divider lintas screen.

## Batch 259 — POLISH_AUDIT #6 § Responsive/Adaptive: audit statis small/large/landscape/font-scale (0 bug baru, 0 code)
Checkbox pertama § Responsive/Adaptive. **Batasan jujur dicatat di depan**: sandbox ini 0
kemampuan render/emulator, jadi audit ini STATIS (grep menyeluruh) bukan visual device betulan
— tidak diklaim setara verifikasi visual.

4 aspek diperiksa: (a) fixed-width besar berisiko clip — 0 ditemukan di 12 file kandidat
(NowPlaying/LibraryScreen/10 `*Sheet.kt`), cuma `Spacer` kecil harmless; (b) long title/artist
song row — sudah `weight(1f)`+`maxLines=1`+marquee/ellipsis, pola flexible standar; (c)
landscape/viewport pendek Now Playing — SUDAH ADA `verticalScroll` safety net dari Batch 112
(komentar detail persis soal skenario 3-button-nav vs gesture-nav); (d) font-scale besar — 0
`fontSize` hardcoded ditemukan di seluruh `ui/*.kt`, semua pakai token typography (otomatis
ikut scale sistem).

**Kesimpulan: 0 bug baru** — pola/safety-net relevan sudah ada dari batch lampau. **TIDAK
diklaim "selesai 100%"** — dicatat sebagai kandidat `MANUAL_QA_CHECKLIST.md` untuk verifikasi
visual manual di device fisik oleh user, bukan dipaksa tuntas cuma krn grep bersih.

0 file kode, 1 dokumentasi (`POLISH_AUDIT.md`). `FILE_MANIFEST.txt` tidak berubah (184/184).
Item berikutnya § Responsive: "Jangan ubah layout architecture" (aturan, bukan checkbox kerja)
— checkbox kerja berikutnya sebenarnya sudah habis di seksi ini (cuma 2 baris, 1 sudah selesai,
1 aturan batas). Lanjut ke § Surface/Color Consistency.

## Batch 258 — POLISH_AUDIT #5: reduced-motion infra check (N/A) — § Motion & Transition TUNTAS (0 code)
Checkbox terakhir § Motion & Transition. `grep -rn "reduced.motion\|ReducedMotion\|
animatorDurationScale\|isReduceMotionEnabled"` di seluruh `app/src/main/java/` → 0 match, project
ini genuinely 0 infrastruktur reduced-motion. Sesuai instruksi eksplisit dokumen sumber: TIDAK
dibuat baru (di luar scope "audit visual", masuk kategori bikin fitur baru). **N/A, STOP.**

**§ Motion & Transition sekarang 6/6 checkbox tuntas** (Batch 254-258). Kategori berikutnya
sesuai urutan dokumen: § Responsive/Adaptive (small/large phone, landscape, font-scale besar —
titik rawan dugaan: `NowPlayingScreen.kt`, `LibraryScreen.kt` song row, 13 `*Sheet.kt`, WAJIB
dicek visual dulu sebelum diedit, bukan tebakan dari nama file).

0 file kode, 1 dokumentasi (`POLISH_AUDIT.md`). `FILE_MANIFEST.txt` tidak berubah (184/184).

## Batch 257 — POLISH_AUDIT #4: animasi vs repeated interaction/scroll cepat, 0 bug + fix manifest gap (0 code, 2 file)
Checkbox berikutnya § Motion & Transition. Kandidat diperiksa: `animateItemPlacement()` (4
titik, `LibraryScreen.kt`) — fire cuma pas list MUTASI (reorder/insert/remove), BUKAN karena
scroll-offset berubah, jadi scroll cepat murni 0 memicu; `basicMarquee()` (3 titik:
`LibraryScreen.kt`/`MiniPlayerBar.kt`/`NowPlayingScreen.kt`) — self-gating bawaan Compose (cuma
jalan kalau teks overflow), independen dari kecepatan scroll. **0 bug ditemukan, STOP** — tidak
ada animasi yang genuinely terikat scroll-offset event.

**Ditemukan sekalian saat cek integritas sebelum repack**: `POLISH_AUDIT.md` (ditanam Batch 253)
ternyata belum pernah masuk `FILE_MANIFEST.txt` — gap lama, bukan dari batch ini. Ditambahkan
(183→184 file), bukan task terpisah (prasyarat wajib sebelum repack manapun, bukan pekerjaan
baru).

0 file kode, 2 dokumentasi (`POLISH_AUDIT.md`, `FILE_MANIFEST.txt`). Item berikutnya §
Motion: "Hormati reduced-motion kalau sudah ada infrastruktur lokalnya" (cek dulu ada/tidaknya
infra sebelum bikin apa pun — kalau belum ada, JANGAN bikin baru).

## Batch 256 — POLISH_AUDIT #3: fix konsistensi spring swipe-snap NowPlayingScreen (1 file patch)
Kerjakan checkbox "audit semua duration/easing lain" `POLISH_AUDIT.md` § Motion. `grep` animasi
(`tween/animateFloatAsState/animateColorAsState/spring`) di seluruh `ui/*.kt` — cakupan nyata cuma
4 file (`LibraryScreen.kt`, `MiniPlayerBar.kt`, `NowPlayingScreen.kt`, `Utils.kt`); 22 file lain 0
animasi custom. Temuan konkret: `bouncyPress()` (`Utils.kt`, shared modifier dipakai semua
tappable control sesuai komentar aslinya "premium music apps put on every tappable control") dan
entrance spring `NowPlayingScreen.kt` (~baris 410) sama-sama eksplisit
`spring(dampingRatio=Spring.DampingRatioMediumBouncy, stiffness=Spring.StiffnessLow)` — tapi 2
spring snap-back drag artwork swipe-next/previous (~baris 1228 `onDragEnd`, ~1231 `onDragCancel`)
cuma set `dampingRatio` tanpa `stiffness` eksplisit (default Compose = `Spring.StiffnessMedium`,
lebih kaku/cepat drpd StiffnessLow yg dipakai 2 animasi bouncy lain — inkonsistensi "rasa" nyata,
bukan dugaan). Fix: tambah `stiffness = Spring.StiffnessLow` ke kedua spring tsb (`sed` in-place,
2 baris identik). `dampingRatio` TIDAK diubah — cuma nyamain stiffness biar 1 sistem. Item lain yg
dicek 0 masalah: `fadeIn(tween(150))`/`fadeOut(tween(300))` (2 titik, identik dgn dirinya sendiri,
sudah konsisten), `tween(280)` entrance-alpha (dipakai tunggal, tidak ada pembanding). Brace/paren
balance OK (217/217+780/780). 0 logic/behavior berubah — swipe-next/previous tetap trigger sama
persis, cuma "rasa" animasi snap-back-nya yang kini seragam dgn animasi bouncy lain di screen ini.

## Batch 255 — POLISH_AUDIT #2: verifikasi tween(700) MiniPlayerBar + NowPlayingScreen — 0 code diubah
Kerjakan 2 checkbox berikutnya `POLISH_AUDIT.md` § Motion (digabung — dicatat di dokumen sebagai
"audit bareng" krn pola identik). `MiniPlayerBar.kt:68` dan `NowPlayingScreen.kt:301` sama-sama
`animateColorAsState(tween(700))` utk cross-fade warna aksen dominan album-art (dipakai CTA/wash/
rating) saat lagu berganti — BUKAN animasi respons tap/klik, jadi standar "micro-feedback harus
cepat" di audit sumber tidak langsung berlaku di sini; yang relevan justru konsistensi ambient
color wash. Hasil: keduanya SUDAH konsisten satu sama lain (700ms identik, tujuan identik). 700ms
sendiri wajar utk color cross-fade non-interaktif (rentang umum 300-800ms) — lebih cepat malah
terasa "kedut" tiap pergantian lagu. Trigger hanya saat song berganti (bukan per-frame/rapid
interaction), jadi 0 risiko numpuk. **Kesimpulan: 0 bug, durasi TIDAK diubah**, sesuai aturan
"0 bug → STOP". `POLISH_AUDIT.md` 2 checkbox dicentang `[x]` dgn catatan verifikasi ini. 0 file
kode disentuh.

## Batch 254 — POLISH_AUDIT #1: verifikasi LibraryScreen.kt:1345 tween(1100) — 0 code diubah
Kerjakan checkbox teratas `POLISH_AUDIT.md` § Motion & Transition. Ternyata `tween(1100, easing=
LinearEasing)` di `LibraryScreen.kt:1345` adalah bagian `ShimmerBrush()` — animasi loading-skeleton
`infiniteRepeatable` (efek shimmer saat list masih loading dari MediaStore/DB), BUKAN animasi
respons interaksi tap/klik (micro-feedback) seperti dugaan di audit sumber. 1100ms per siklus
shimmer masih dalam rentang wajar (pattern shimmer umum di Android/Material biasanya 1000-1500ms
per siklus, terlalu cepat malah bikin shimmer terasa "berkedip" tidak natural). **Kesimpulan: 0
bug, durasi TIDAK diubah** — sesuai aturan eksplisit `POLISH_AUDIT.md` ("kalau 0 bug ditemukan,
STOP, jangan ciptakan kerjaan baru demi 100%"). `POLISH_AUDIT.md` checkbox dicentang `[x]` dgn
catatan verifikasi ini. 0 file kode disentuh.

## Batch 253 — Tanam POLISH_AUDIT.md (backlog micro-polish permanen, 1 file baru, 0 code diubah)
Permintaan user: tanam audit final micro-polish permanen ke repo, adaptasi ke referensi konkret
(bukan tempel mentah). File baru `POLISH_AUDIT.md` (root) — 5 area (Motion & Transition,
Responsive/Adaptive, Surface/Color Consistency, Repeated Components, Typography Final Check),
tiap area jadi checklist `[ ]`/`[x]` descending, cara-pakai eksplisit utk sesi berikutnya (1
checkbox = 1 batch, tetap Strict Micro-Batching). Referensi konkret ditambahkan via grep repo
sendiri: `LibraryScreen.kt:1345` `tween(1100, easing=LinearEasing)`, `MiniPlayerBar.kt:68` +
`NowPlayingScreen.kt:301` sama-sama `tween(700)` accent transition. Catatan penting yg
ditambahkan: project TIDAK punya shared component library (`ui/*.kt` 26 file, semua inline) — jadi
seksi "Repeated Components" WAJIB visual-comparison manual, bukan alasan ekstraksi shared
composable (refactor besar, dilarang eksplisit oleh sumber audit maupun aturan sesi umum project).
`PROJECT_STATE.md` aturan sesi #4 ditambah: `POLISH_AUDIT.md` jadi sumber Pending Queue default.
**0 file kode disentuh, 0 build risk** — batch murni dokumentasi/perencanaan.

## Batch 252 — Fix build FAILED lanjutan 4/4: bump Room 2.6.1→2.8.4 (1 file patch)
Root cause dari `log_fail_258.zip` (build FAILED lagi setelah Batch 251 fix DSL syntax):
`kspDebugKotlin`/`kspReleaseKotlin` FAILED — `[ksp] java.lang.IllegalStateException: unexpected
jvm signature V`. Dicek `web_search`: BUG KSP2 YANG SUDAH DIKENAL (google/ksp issue #2957, #2177)
— muncul saat KSP2 (aktif sejak Kotlin 2.0+/Batch 250) memproses Room DAO suspend function ber-
return Unit, dgn Room versi lama. Root cause BUKAN kode project (`LyricsDao.kt` dkk 0 disentuh) —
murni versi Room (2.6.1) belum kompatibel KSP2. Fix `app/build.gradle.kts`: `room-runtime`/
`room-ktx`/`room-compiler` 2.6.1→2.8.4 (latest stable Room 2.x per Agustus 2026 — line 2.x sekarang
maintenance-mode setelah Room 3.0 alpha rilis Maret 2026, TAPI Room 3.0 SENGAJA TIDAK dipilih:
breaking rewrite total/artifact beda semua `androidx.room3:*`, jauh di luar scope 1-task fix ini).
Dicek juga: 0 KSP-processor lain di project selain Room (`grep ksp(` cuma 1 match) — jadi Room ini
satu-satunya sumber KSP2 incompatibility yang mungkin, 0 lurking issue lain tersisa dari sisi KSP.
Brace/paren balance OK (35/35+156/156). Protected asset (`app/build.gradle.kts`) disentuh SEBAGIAN.

**⚠️ 4× GAGAL BUILD BERTURUT** (Batch 249→250→251→252, akar berbeda-beda: compileSdk/AGP → versi
Kotlin → syntax DSL → versi Room/KSP2 bug). Semua akar SAMA-SAMA konsekuensi lompatan besar Kotlin
1.9.24→2.4.10 sekaligus (Batch 250) — tiap dependency yang belum sempat diaudit compatibility-nya
munculin error baru satu per satu. **Belum ada konfirmasi BUILD SUCCESS dari user sampai sekarang.**

## Batch 251 — Fix build FAILED lanjutan 3/3: migrasi kotlinOptions→compilerOptions DSL (1 file patch)
Root cause dari `log_fail_257.zip` (build FAILED lagi setelah Batch 250 fix Kotlin/KSP/Compose
plugin): `android{kotlinOptions{jvmTarget="17"; freeCompilerArgs+=...}}` — syntax string lama ini
jadi HARD ERROR (bukan cuma deprecated warning) di Kotlin 2.4.10 (Batch 250). Fix `app/
build.gradle.kts`: hapus blok `kotlinOptions{}` dari dalam `android{}`, ganti top-level
`kotlin{compilerOptions{}}` (letak setelah `android{}` ditutup) — `jvmTarget` string →
`JvmTarget.JVM_17` enum (`import org.jetbrains.kotlin.gradle.dsl.JvmTarget` ditambah di top file),
`freeCompilerArgs +=` → `freeCompilerArgs.addAll(...)`. Isi opt-in flags & stabilityConfigurationPath
(Batch 20) PERSIS sama, 0 logic/behavior berubah — murni migrasi syntax DSL. Brace/paren balance OK
(35/35+150/150). Protected asset (`app/build.gradle.kts`) disentuh SEBAGIAN sesuai izin
edit-parsial. **3× gagal build berturut** (Batch 249→log_fail_256→Batch 250→log_fail_257→batch
ini) — tiap fix membuka error lapisan berikutnya (compileSdk→Kotlin version→DSL syntax), semua
akar sama: lompatan versi besar 1.9.24→2.4.10 sekaligus. **WAJIB di-build ulang di CI**, jangan
kirim ZIP lagi ke user sampai ada konfirmasi BUILD SUCCESS — kalau gagal lagi, kemungkinan ada
error keempat yang belum ketauan krn build kemarin selalu berhenti di error pertama yang ketemu.

## Batch 250 — Fix build FAILED lanjutan: bump Kotlin 2.4.10 + Compose compiler plugin (2 file patch)
Root cause dari `log_fail_256.zip` (build FAILED lagi setelah Batch 249 fix AGP/compileSdk):
`kspReleaseKotlin`/`kspDebugKotlin` FAILED — `work-runtime-2.11.2` dikompilasi metadata Kotlin
2.1.0, project masih pakai Kotlin 1.9.24 (binary incompatible, "expected version is 1.9.0").
Fix: `build.gradle.kts` Kotlin (`org.jetbrains.kotlin.android`) `1.9.24`→`2.4.10` (latest STABLE
per kotlinlang.org, dicek `web_search` Agustus 2026 — 2.4.20 masih RC, sengaja tidak dipilih). KSP
`1.9.24-1.0.20`→`2.3.10` (BUKAN `2.4.10-x`, versioning KSP decoupled dari Kotlin sejak KSP 2.3.0 —
pairing ini persis contoh resmi quickstart docs kotlinlang.org, dicek `web_search`). Plugin BARU
`org.jetbrains.kotlin.plugin.compose` version `2.4.10` — WAJIB sejak Kotlin 2.0+, Compose compiler
sudah tidak dibundel otomatis di kotlin-android plugin lagi. `app/build.gradle.kts`: tambah
`id("org.jetbrains.kotlin.plugin.compose")` ke plugins block + HAPUS `composeOptions {
kotlinCompilerExtensionVersion = "1.5.14" }` (obsolete, versi compose compiler sekarang auto ikut
Kotlin via plugin baru — kalau dibiarkan, "1.5.14" incompatible total sama Kotlin 2.4.10, jadi
error baru bukan cuma warning). Brace balance OK 2 file (1/1, 28/28 — turun dari 29/29 krn 1 blok
composeOptions dihapus utuh). Protected assets (build.gradle.kts root+app) disentuh SEBAGIAN sesuai
izin edit-parsial. **Belum diverifikasi ulang di CI** — 2 kali gagal build berturut (Batch
249→log_fail_256, sekarang batch ini) jadi confidence diturunkan drpd batch fix biasa, WAJIB
di-build ulang sebelum dianggap tuntas.

## Batch 249 — Fix build FAILED: bump AGP/compileSdk/Gradle (3 file patch, 0 file baru)
Root cause dari `log_fail_255.zip`: `androidx.work:work-runtime-ktx:2.11.2` (ditambahkan Batch 246
buat `LyricsPrefetchWorker`) butuh compileSdk 35+ & AGP 8.6.0+ — project masih compileSdk 34 & AGP
8.4.1, task `:app:checkDebugAarMetadata` FAILED (4 issues, semua sama akar). Fix: `build.gradle.kts`
AGP `8.4.1`→`8.13.0` (versi 8.x stabil terakhir sebelum AGP 9.x — 9.x classic-DSL-breaking, di luar
scope 1-task fix ini, sengaja TIDAK dipilih meski lebih baru, lihat catatan risiko di bawah).
`app/build.gradle.kts` `compileSdk` 34→36 (bukan pas 35 — 36 adalah max API yg didukung AGP 8.13,
dicek `web_search` Agustus 2026, biar tidak nagih bump lagi kalau ada dependency lain minta 35).
`targetSdk` SENGAJA TIDAK ikut dinaikkan (tetap 34) — di luar scope fix compile error ini, murni
task perbaikan build, bukan task modernisasi target runtime. `.github/workflows/build.yml`
`gradle-version: 8.7`→`8.14.3` (2 titik, job debug+release) — AGP 8.13 butuh Gradle lebih baru dari
8.7 (dicek `web_search`, pasangan umum AGP 8.13↔Gradle 8.13-8.14). Kotlin `1.9.24`/KSP
`1.9.24-1.0.20` SENGAJA TIDAK disentuh — tidak ada indikasi jadi penyebab FAILED ini, ganti versi
Kotlin di luar 1-task scope (Strict Micro-Batching). Brace balance OK 3 file (1/1, 29/29, 38/38).
Protected assets (build.gradle.kts root+app, workflow) disentuh SEBAGIAN sesuai izin edit-parsial.
**Belum diverifikasi ulang di CI** — perbaikan berdasar analisa log_fail_255.zip + web_search
compatibility matrix, bukan hasil re-run build sukses.

## Batch 248 — Wire Lyrics offline-first ke NowPlayingScreen (2 file patch, 0 file baru)
Menutup gap yang dicatat eksplisit di Batch 245/247 ("Belum di-wire ke NowPlayingScreen").
`NowPlayingScreen.kt` (patch): hoist `LyricsViewModel` via `viewModel(factory = LyricsViewModel.factory(context))`
di level composable (bukan di dalam blok `showLyricsSheet`) + `LaunchedEffect(song?.id)` panggil
`loadLyrics(artist, title, album)` tiap ganti lagu — fetch mulai duluan sebelum user buka sheet,
debounce 5 detik dari ViewModel (Batch 245) tetap satu-satunya guard anti-spam. State
`lyricsAutoState` (collectAsState) diteruskan ke `LyricsSheet` sebagai param baru `autoUiState`.
`LyricsSheet.kt` (patch): param baru `autoUiState: LyricsUiState? = null` (default null =
source-compatible, 0 call-site lain kena breaking change). Default `editing` diubah dari
`rawLyrics.isNullOrBlank()` → `false` (dulu lompat langsung ke textbox edit kalau lirik manual
kosong krn 0 alternatif; sekarang kalau auto-fetch ketemu, tampilkan itu dulu). Branch baru:
manual kosong + `autoUiState` Found/Loading → render `LyricsStateView` (dari `ui/lyrics/LyricsView.kt`,
Batch 245) di dalam sheet; manual kosong + auto NotFound/Idle/null → fallback pesan "Belum ada
lirik" + tombol "Tambah Lirik" spt sebelumnya (0 regresi). Lirik manual (`onSaveLyrics`/DB) SELALU
menang atas auto kalau ada — 2 sumber data tetap terpisah, auto cuma fallback tampilan, bukan
auto-save ke penyimpanan manual. Tombol Edit di header (`if (!editing)`) sekarang jadi satu-satunya
jalur override manual dari tampilan auto. Brace/paren balance OK (`LyricsSheet.kt` 65/65+183/183,
`NowPlayingScreen.kt` 217/217+775/775). 0 protected asset disentuh. **Belum diverifikasi
compile/device** seperti biasa.

## Batch 247 — Lyrics offline-first 4/4b: Store toggle + 2 menu Settings (TERAKHIR kategori ini)
Lanjutan Batch 246 (Pending Queue 4/4b). Batch penutup kategori Lyrics offline-first (Batch
243-247).

**`data/lyrics/LyricsPrefetchStore.kt` (baru)**: boilerplate identik `SilenceSkipStore.kt` —
`SharedPreferences` boolean tunggal, `isEnabled()`/`setEnabled()`. **Default ON**, beda dari
semua toggle playback-behavior lain di app ini (`ShakeSettingsStore`/`FloatingBubbleStore`/
`SilenceSkipStore`, semua default OFF) — alasan dijelaskan di KDoc file: fitur ini WiFi-only
(`NetworkType.UNMETERED`), 0 dampak ke pemutaran atau kuota data seluler kalau user tidak pernah
nyambung WiFi, sedangkan toggle lain mengubah perilaku pemutaran yang user rasakan langsung
(makanya wajib opt-in eksplisit). Konsekuensi ON diam-diam jauh lebih ringan di sini.

**`playback/PlaybackService.kt` (patch)**: `onMediaItemTransition` sekarang
`if (LyricsPrefetchStore(this).isEnabled()) LyricsPrefetchWorker.enqueue(this)` — store dibaca
ULANG tiap transition (bukan di-cache ke field Service saat `onCreate`), supaya toggle yang baru
saja diubah user di Settings langsung berlaku transisi lagu berikutnya tanpa perlu restart
Service/proses.

**`ui/SettingsScreen.kt` (patch)**: 2 item baru.
1. **"Prefetch Lirik Saat WiFi"** — switch ke-5 di grup "Perilaku Pemutaran" (title+subtitle+
   `Switch`, `Spacer(12dp)`, haptic `TextHandleMove` — pola identik 4 switch lain di situ).
   SENGAJA ditaruh di sini, BUKAN di "Alat & Utilitas" tempat 4 item lyrics-adjacent lain
   (Statistik/Backup/Duplikat/Vault/Hapus-Cache) berada — karena secara visual ini SWITCH,
   bukan nav-row, dan Batch 217-218 sudah mengaudit+mengkonfirmasi 2 row-species itu punya
   spacing/padding berbeda secara SAH (afinitas interaksi beda). Mencampur toggle ke grup
   nav-row akan mengulang inkonsistensi yang sudah susah payah diperbaiki 2 batch lalu.
2. **"Hapus Cache Lirik"** — nav-row ke-5 di "Alat & Utilitas" (icon `DeleteSweep`+title+
   subtitle, `Spacer(4dp)`, pola identik 4 row lain di situ) + `AlertDialog` konfirmasi (pola
   identik `showDisableLockConfirm` milik `AppLockSection` — icon warna error, `TextButton`
   "Hapus" warna error di confirmButton, "Batal" di dismissButton). Confirm →
   `scope.launch { LyricsRepository(context).clearCache(); onInfoMessage("Cache lirik dihapus") }`.

State (`lyricsPrefetchEnabled`, `context`, `scope`) dibaca/ditulis LANGSUNG via `LocalContext.
current`/`rememberCoroutineScope()` di dalam `SettingsScreen.kt` sendiri — pola sama seperti
Vault/Duplicate/Backup (fitur "utilitas" mandiri di file ini semua begini), BUKAN di-hoist ke
`MainActivity.kt` seperti toggle playback-behavior lama (`shakeToSkipEnabled` dst.) — jadi 0
protected asset disentuh, cukup 3 file kode (cap terpenuhi persis).

Brace/paren 3 file seimbang (`LyricsPrefetchStore.kt` 3/3+12/12, `PlaybackService.kt`
78/78+361/361, `SettingsScreen.kt` 157/157+481/481). `FILE_MANIFEST.txt` 182→183 (1 file baru,
diselipkan alfabetis).

**🎉 Kategori Lyrics offline-first TUNTAS 4/4, 5 batch (243-247)**: Room cache layer → Retrofit
LRCLIB API → Repository+ViewModel+View offline-first (debounce+skip-lagu-sama) → Worker prefetch
WiFi-only (10 lagu depan queue) → Store toggle + 2 menu Settings. **Item terbuka TERPISAH dari
scope 5 batch ini** (eksplisit dicatat sejak Batch 245): `LyricsView`/`LyricsViewModel` belum
di-wire ke layar Now Playing manapun — lirik sudah bisa di-cache/prefetch/dihapus lewat Settings,
tapi belum ada UI yang benar-benar MENAMPILKANNYA saat lagu diputar. Kandidat kategori kerja
terpisah kalau user minta. Belum diverifikasi compile/device seperti biasa.

## Batch 246 — Lyrics offline-first 4/4a: Worker prefetch, 2 file kode + 1 file baru + gradle protected-parsial + 2 dokumentasi
Lanjutan Batch 245. Pending Queue lama ("batch 4/4") membundel Worker + Store toggle + 2 menu
Settings jadi 1 batch — itu 4 file kode sekaligus, lewat batas Maks 3 File / batch. **Dipecah
jadi 4/4a (batch ini) dan 4/4b (Pending Queue baru)**, prioritas mekanisme inti jalan dulu
sebelum kontrol user-facing-nya.

**`worker/LyricsPrefetchWorker.kt` (baru)**: `CoroutineWorker`, `doWork()` baca
`PlaybackStateStore(applicationContext).load()` — infrastruktur SUDAH ADA sejak Batch 108
(playback resumption), dipilih SENGAJA ketimbang pegang ExoPlayer/MediaController langsung
karena Worker bisa dieksekusi WorkManager kapan pun termasuk saat proses app sudah mati total.
Ambil `songIds.drop(index+1).take(10)`, resolve ke `Song` (artist/title/album) via
`MusicRepository.getSongsByIds()` (fungsi sudah ada, dipakai jalur playback-resumption juga),
loop `repository.ensureCached()` per lagu — cek `isStopped` tiap iterasi (WorkManager bisa minta
stop kapan saja, mis. WiFi putus di tengah). 1 lagu gagal fetch tidak menjegal sisanya
(`ensureCached()` sendiri sudah swallow exception, lihat `LyricsRepository.kt` Batch 245).
`enqueue()` companion: `Constraints.NetworkType.UNMETERED` (spec: WiFi saja) +
`enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)` — aman dipanggil berulang,
request lama yang belum jalan otomatis dibuang gantikan window terbaru.

**`playback/PlaybackService.kt` (patch)**: `LyricsPrefetchWorker.enqueue(this)` dipanggil dari
`Player.Listener.onMediaItemTransition` — BUKAN `MediaSessionCompat.Callback.onMetadataChanged()`
seperti draft Pending Queue lama (itu API Media**Compat** lawas; project ini pakai Media3
`androidx.media3.common.Player.Listener`, sudah ada 1 listener yang sama dipakai buat
widget-update sejak Batch 34, tinggal ditambah 1 baris di situ — bukan listener baru).

**Catatan akurasi jujur, bukan diklaim sempurna**: window "10 lagu depan" akurat SEJAUH
`PlaybackStateStore` terakhir di-save — dan itu checkpoint PERIODIK ~5 detik oleh
`PlayerViewModel.persistPlaybackState()` (Batch 108), BUKAN synchronous tepat di titik
`onMediaItemTransition` ini. Kalau Worker kebetulan langsung dieksekusi WorkManager (mis. sudah
di WiFi, 0 delay) di celah beberapa detik sebelum checkpoint berikutnya, window yang dibaca bisa
1 index ketinggalan (prefetch lagu N alih-alih N+1 dst). Konsekuensinya ringan — geser 1 lagu,
bukan salah total atau crash — sengaja TIDAK dikejar presisi sinkron sempurna di batch ini
(butuh sentuh `PlayerViewModel.kt` jadi 4 file, lewat cap), dicatat eksplisit sebagai
known-limitation di `PROJECT_STATE.md`, bukan disembunyikan.

**Gradle (protected-parsial)**: `androidx.work:work-runtime-ktx:2.11.2` — `web_search`
dijalankan dulu ke developer.android.com/kotlin/ktx (halaman resmi, "last updated 2026-08-14")
buat pastikan versi terbaru, bukan angka dari training data yang bisa basi (WorkManager pernah
ubah bentuk API antar rilis). 0 protected asset app (Manifest/MainActivity/dst.) disentuh.

**Bonus temuan (low-risk high-value, di luar scope diminta)**: sebelum edit apa pun batch ini,
`FILE_MANIFEST.txt` dicek dulu vs disk (rutin tiap batch) — ketahuan SUDAH DRIFT dari 3 batch
sebelumnya: 8 file kategori Lyrics (Batch 243-245) tidak pernah tercatat — `LyricsRepository.kt`,
`LyricsApi.kt`, `LyricsDto.kt`, `LyricsDao.kt`, `LyricsDatabase.kt`, `LyricsEntity.kt`,
`LyricsView.kt`, `LyricsViewModel.kt`. Diperbaiki sekaligus bareng 1 file baru batch ini
(173→182), diselipkan alfabetis per folder. 182/182 match disk terverifikasi ulang setelah semua
edit selesai.

**Pending Queue — Lyrics offline-first 4/4b (batch terakhir kategori ini)**: `data/lyrics/
LyricsPrefetchStore.kt` (baru — boilerplate identik `SilenceSkipStore.kt`, boolean "Prefetch
Saat WiFi", default ON) + patch `SettingsScreen.kt` (2 menu baru di grup "Alat & Utilitas": "Hapus
Cache Lirik" → `LyricsRepository.clearCache()` + dialog konfirmasi, "Prefetch Saat WiFi" → toggle
baca/tulis store). Kalau toggle OFF, `PlaybackService.kt` KEMUNGKINAN kena sentuh sekali lagi —
guard `if (LyricsPrefetchStore(this).isEnabled())` sebelum `LyricsPrefetchWorker.enqueue()`
(kalau begitu jadinya, hitung ulang cap: `LyricsPrefetchStore.kt` baru + `SettingsScreen.kt` +
`PlaybackService.kt` = pas 3 file, masih dalam batas).

## Batch 245 — Lyrics offline-first 3/4: Repository+ViewModel+View, 3 file kode + 2 dokumentasi
Lanjutan Batch 244 (API layer). Pending Queue item 3.

File dibuat (3, HARD CAP batch):
- `data/lyrics/LyricsRepository.kt` — `sealed class LyricsResult { Found(plain, synced) /
  NotFound }` (domain model, ViewModel tidak perlu tahu Room/Retrofit ada di belakangnya).
  `getLyrics()`: `dao.get()` → ada? return cache langsung (0 network) → null? `fetchAndCache()`
  (panggil `api.getLyrics()`, map DTO→`LyricsEntity`, `dao.upsert()`, return). `ensureCached()`
  — varian khusus dipanggil `LyricsPrefetchWorker` (batch 4/4): cache-check dulu, cuma fetch
  kalau belum ada, TIDAK expose `LyricsResult` ke caller (prefetch cuma peduli efek-samping
  "sudah ke-cache", bukan isinya). Semua exception (`IOException` timeout/offline, exception
  lain) ditangkap → `NotFound`, tidak pernah crash/bocor ke UI (spec error case #9).
- `ui/lyrics/LyricsViewModel.kt` — `StateFlow<LyricsUiState>` (`Idle`/`Loading`/`Found`/
  `NotFound`). **Debounce 5 detik (`.debounce(5000)`) + skip-lagu-sama (`.distinctUntilChanged`
  bandingin artist+title, album diabaikan dari perbandingan identitas) diimplementasi DI
  VIEWMODEL**, bukan di caller (`PlaybackService`, batch 4/4) — keputusan desain: 1 sumber
  kebenaran drpd duplikasi logic debounce di 2 tempat; `PlaybackService` nanti cukup panggil
  `loadLyrics()` polos tiap `onMetadataChanged`, ViewModel yg jamin request bertubi-tubi
  (seek cepat/event metadata dobel Media3) tidak nembak query berkali-kali.
- `ui/lyrics/LyricsView.kt` — `parseLRC(lrc: String): List<LyricLine>` top-level pure function
  (testable tanpa Compose runtime), regex `\[(\d{2}):(\d{2})\.(\d{2})](.*)` — PENTING: `xx`
  dikonversi `×10` (centisecond→ms), BUKAN padding 3-digit millisecond (format LRC standar 2
  digit setelah titik = centisecond, bukan ms, kalau ×1 sinkronisasi bakal meleset ~10x lipat
  terlalu cepat). `activeLyricIndex()` cari baris LRC terakhir yg `timeMs <= posisi` (baris LRC
  menandai "mulai dari sini", bukan interval eksplisit). `SyncedLyricsContent` — `LazyColumn` +
  `LaunchedEffect(currentPositionMs)` + `animateScrollToItem((index-2).coerceAtLeast(0))`
  (baris aktif nongol ⅓ atas viewport, bukan mepet). Baris aktif dibedakan bold+ukuran font
  (`titleMedium` vs `bodyLarge`) SELAIN warna — konsisten aturan Batch 241 (informasi penting
  jangan cuma dibedakan warna). Fallback: `syncedLyrics` kosong/blank → `plainLyrics`;
  `plainLyrics` juga kosong → `EmptyLyricsMessage` ("Lirik tidak ditemukan"). `LyricsStateView`
  — wrapper terima `LyricsUiState` langsung dari ViewModel (Loading→spinner, NotFound→pesan),
  1 titik pemanggilan simpel dari layar mana pun butuh nampilin lirik.

**Belum di-wire ke `NowPlayingScreen.kt`** — integrasi UI (mounting `LyricsStateView` ke layar
Now Playing + collect `positionMs` dari `PlayerViewModel.uiState`) di luar 10 file yg diminta
spec asli user, sengaja tidak dikerjakan di batch ini (scope creep) — bisa jadi item pending
terpisah kalau user mau.

Brace/paren balance: `LyricsRepository.kt` (9/9, 45/45), `LyricsViewModel.kt` (10/10, 29/29),
`LyricsView.kt` (28/28, 82/83 — selisih 1 `)` berasal dari KOMENTAR notasi interval matematika
`[start,end)` baris 39, bukan kode; diverifikasi manual character-by-character, kode Kotlin
valid). 0 protected asset disentuh batch ini (0 gradle baru — Retrofit/Room sudah cukup, 0
dependency baru dibutuhkan Repository/ViewModel/View).

**Pending Queue — batch terakhir kategori Lyrics offline-first (4/4)**:
`worker/LyricsPrefetchWorker.kt` + patch `PlaybackService.kt` + 2 menu Settings + gradle
work-runtime-ktx + `WorkManager.enqueueUniqueWork`.

## Batch 244 — Lyrics offline-first 2/4: Retrofit API layer LRCLIB, 2 file kode + gradle protected-parsial + 2 dokumentasi
Lanjutan Batch 243 (Room cache layer). Pending Queue item 2.

File dibuat (2, HARD CAP batch):
- `data/lyrics/api/LyricsDto.kt` — bentuk respons 1:1 LRCLIB `/api/get` (`trackName`,
  `artistName`, `albumName`, `duration`, `instrumental`, `plainLyrics`, `syncedLyrics`, semua
  nullable — instrumental/lirik-tanpa-timestamp genuinely bisa null dari API). 0 logic mapping
  di sini, disengaja — DTO→Entity mapping masuk Repository (batch 3/4) biar 1 file 1 concern.
- `data/lyrics/api/LyricsApi.kt` — Retrofit interface, 1 endpoint `GET api/get` dgn query
  `artist_name`/`track_name` (persis spec user; bukan `/api/search` — itu multi-hasil ambigu,
  tidak cocok pola 1-baris-per-lagu offline-first). `companion object.create()` bikin instance
  siap pakai: `OkHttpClient` timeout 10s connect+read (spec eksplisit; SENGAJA lebih ketat dari
  `GitHubReleaseChecker` punya UpdateDownloader yg 15s/20s — endpoint itu dipanggil manual di
  Settings, ini dipanggil otomatis di jalur pemutaran lagu tiap ganti track, gagal cepat lebih
  penting drpd nunggu lama nge-block UX dengar lagu) + `Interceptor` nempel header
  `User-Agent: MusicApp/1.3 Hybrid` (persis spec) ke semua request.

Gradle (protected-parsial, minimal-diff): `app/build.gradle.kts` + `retrofit2:retrofit:2.11.0`
+ `retrofit2:converter-gson:2.11.0`. `okhttp3` TIDAK ditambah baru — sudah ada di project
(dipakai `UpdateDownloader`/`GitHubReleaseChecker`), Retrofit di sini numpang `OkHttpClient`
instance sendiri (beda timeout, lihat atas) tapi versi dependency yg sama, 0 duplikasi
transitive.

Brace/paren balance: `LyricsApi.kt` (3/3, 26/26), `LyricsDto.kt` (0/0, 7/7 — data class).
0 protected asset app (Manifest/MainActivity/Application/PlaybackService) disentuh.

**Pending Queue (batch berikutnya)**:
3. `LyricsRepository.kt` (offline-first logic + mapping DTO→Entity) + `LyricsViewModel.kt`
   (StateFlow, debounce 5s) + `LyricsView.kt` (Composable parseLRC/autoScroll/fallback).
4. `LyricsPrefetchWorker.kt` + patch `PlaybackService.kt` + 2 menu Settings + gradle
   work-runtime-ktx.

## Batch 243 — FITUR BARU Lyrics offline-first 1/4: Room cache layer, 3 file kode + gradle protected-parsial + 2 dokumentasi
Menyela antrean Motion & Transition (ditunda user, urgent). Spec: cache lirik LRCLIB (LRC
synced + plain text), offline-first (cache Room dulu, API cuma kalau miss), prefetch 10 lagu
depan saat WiFi, trigger dari playback service dgn debounce.

**Adaptasi arsitektur (izin eksplisit user: "boleh diadaptasi, hasil akhir sama")**: spec asli
minta Hilt DI penuh. Codebase ini (242 batch, mature) 0% pakai DI framework — semua kelas
`data/*Store.kt` (ThemeStore, VaultStore, dst) dikonstruksi manual dari `Context`,
`PlayerViewModel` pakai `ViewModelProvider.Factory` manual, bukan Hilt. Memaksakan Hilt demi 1
fitur baru berarti nambah `@HiltAndroidApp`/`@AndroidEntryPoint` ke `AudioPlayerApplication.kt`
(Application, protected), `MainActivity.kt` (protected), dan `PlaybackService.kt` (service
playback utama — bukan protected secara eksplisit di daftar tapi core/high-risk) — blast
radius besar demi 1 fitur samping. Diganti singleton manual `LyricsDatabase.getInstance()`,
persis pola existing, 0 protected asset app disentuh batch ini (cuma 2 file gradle,
protected-parsial, minimal-diff).

**Koreksi nama dari spec user**: `PlayerService.kt` yang diminta tidak ada di project ini —
service playback yg ada namanya `playback/PlaybackService.kt`. Semua referensi lyrics-trigger
di batch mendatang pakai nama itu.

File dibuat (3, HARD CAP batch):
- `data/lyrics/db/LyricsEntity.kt` — `@Entity(tableName = "lyrics_cache")`, index
  `UNIQUE(artist, title)`, kolom persis spec (id PK autoGenerate, artist, title, album?,
  plainLyrics?, syncedLyrics?, lastFetched: Long, source: String).
- `data/lyrics/db/LyricsDao.kt` — `get()` suspend one-shot (dipakai Repository logic
  offline-first "cek cache dulu"), `observe()` Flow (reaktif, buat ViewModel), `upsert()`
  `OnConflictStrategy.REPLACE` (match unique index, re-fetch manual tidak bentrok constraint),
  `count()`/`clearAll()` (buat menu Settings "Hapus Cache Lirik" batch mendatang).
- `data/lyrics/db/LyricsDatabase.kt` — singleton `getInstance(context)`, `Room.databaseBuilder`,
  nama file `lyrics_database.db` (persis spec), `exportSchema = false` (Room pertama kali di
  project ini, belum ada folder `schema/` yg perlu diselaraskan).

Gradle (protected-parsial, minimal-diff):
- `build.gradle.kts` (root): + `id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply
  false` — KSP dipilih atas kapt (legacy, lebih lambat compile), sesuai aturan sesi #3
  (prioritas mutakhir). Versi KSP disamakan Kotlin plugin 1.9.24 yg sudah ada.
- `app/build.gradle.kts`: + plugin `com.google.devtools.ksp` (tanpa versi, ambil dari root) +
  3 dependency: `androidx.room:room-runtime:2.6.1`, `androidx.room:room-ktx:2.6.1` (Flow/suspend
  DAO support), `ksp("androidx.room:room-compiler:2.6.1")`.

Brace/paren balance: `LyricsEntity.kt` (0/0, 7/7 — data class, wajar 0 brace), `LyricsDao.kt`
(1/1, 18/18), `LyricsDatabase.kt` (4/4, 13/13). 0 protected asset app (Manifest/MainActivity/
Application/PlaybackService) disentuh — cuma 2 file gradle, parsial.

**Pending Queue (urutan dependency, batch berikutnya)**:
1. `data/lyrics/api/LyricsDto.kt` + `LyricsApi.kt` — Retrofit interface LRCLIB
   (`GET https://lrclib.net/api/get?artist_name={artist}&track_name={title}`), timeout 10s,
   header `User-Agent: MusicApp/1.3 Hybrid`. + gradle retrofit2/converter-gson (okhttp sudah
   ada di project, dipakai UpdateDownloader — retrofit tinggal numpang client yg sama pattern).
2. `data/lyrics/LyricsRepository.kt` (logic offline-first: `dao.get()` → null? `api.fetch()` →
   `dao.upsert()` → return; hit? return langsung cache, 0 network) + `ui/lyrics/
   LyricsViewModel.kt` (`StateFlow<LyricsUiState>`, debounce 5 detik, skip query kalau
   artist+title sama dgn request terakhir) + `ui/lyrics/LyricsView.kt` (Composable
   `parseLRC()`/`updateTime(positionMs)`/autoScroll `LazyColumn`, fallback plain text kalau
   `syncedLyrics` null, pesan "Lirik tidak ditemukan" kalau API gagal DAN cache kosong).
3. `worker/LyricsPrefetchWorker.kt` (`WorkManager`, `Constraints.NetworkType.UNMETERED`, ambil
   10 lagu depan dari `ExoPlayer` queue, skip yg sudah ke-cache via `dao.get()` sebelum fetch)
   + gradle `work-runtime-ktx` + patch `PlaybackService.kt` (dengarkan
   `MediaSessionCompat.Callback.onMetadataChanged()`, debounce 5s, skip kalau lagu sama dgn
   trigger terakhir) + 2 menu baru `SettingsScreen.kt` ("Hapus Cache Lirik" → `dao.clearAll()`,
   "Prefetch Saat WiFi" toggle) + `WorkManager.enqueueUniqueWork("prefetch_lyrics", ...)`.

## Batch 242 — Accessibility Micro-Polish 9/9: verifikasi retrospektif no-behavior-change, 0 bug, 0 kode + 2 dokumentasi → Kategori TUNTAS 9/9
Item terakhir kategori Accessibility Micro-Polish bersifat guard-rail, dieksekusi sbg audit
retrospektif 6 fix kode Batch 234-241 (bukan fix baru — verifikasi semua perubahan sebelumnya
genuinely semantics/visual-only, bukan behavior baru):

- Batch 234/236: handler `onClick` pindah dari child (`RadioButton`) ke parent (`Modifier
  .selectable`) — grep konfirmasi `RadioButton(onClick = null)`, logic klik sama persis cuma
  naik 1 level widget tree (TalkBack semantics, bukan interaksi baru).
- Batch 237: cuma nambah `imeAction`/`focusManager.moveFocus` — 0 logic baru, field terakhir
  tetap `ImeAction.Done` + `clearFocus()` (perilaku submit form tidak berubah).
- Batch 238: cuma ukuran `IconButton` 32dp→48dp — `onRate` callback tidak disentuh.
- Batch 241: cuma nambah `Icon(GraphicEq)` visual, 0 `onClick` baru ditempel padanya.

**Hasil: 0 pelanggaran**, 0 kode disentuh. `MICRO_UIUX_AUDIT.md` diupdate (checklist 9/9 +
`STATUS TRACKING`).

**Rekap kategori Accessibility Micro-Polish (Batch 234-242, 9/9)**: TalkBack semantics(234, 1
fix)/content descriptions(235, 1 fix)/semantic role(236, 1 fix)/focus order(237, 1 fix)/minimum
touch target(238, 1 fix)/text scaling(239, 0 bug)/contrast(240, 0 bug)/color-only info(241, 1
fix)/no-behavior-change(242, 0 bug). Total 6 bug fix nyata + 3 audit bersih.

Kategori berikutnya (belum mulai): 🟡 MOTION & TRANSITION.

## Batch 241 — Accessibility Micro-Polish 8/9: info tidak boleh cuma dibedakan warna, 1 bug fix, 1 file kode + 2 dokumentasi
`PlaylistSongRow` (`PlaylistScreen.kt`) beda pola dari `QueueRow`/`SongRow`: status "sedang
diputar" cuma background tint + bold + warna primary, 0 icon/teks pendamping. `QueueSheet.kt`
& `LibraryScreen.kt` sudah pakai badge `Icons.Default.GraphicEq` (Batch 229) + teks "Sedang
diputar" sbg `contentDescription` — `PlaylistScreen.kt` luput saat itu (scope Batch 229 cuma 2
file, PlaylistScreen tidak masuk).

Fix: tambah import `androidx.compose.material.icons.filled.GraphicEq`, bungkus `Text(song.title)`
dgn `Row` horizontal, sisipkan `Icon(GraphicEq, tint = secondary, 16dp, contentDescription =
"Sedang diputar")` + `Spacer(4dp)` sebelum title kalau `isPlaying`. Title dapat
`Modifier.weight(1f, fill = false)` biar tidak dorong layout row kalau nama lagu panjang (marquee
tidak dipasang di sini, beda dgn `LibraryScreen.kt`/`QueueSheet.kt` yang punya `basicMarquee()` —
scope batch ini cuma nambah icon, bukan nyamain marquee, dicatat biar tidak disangka luput).

Brace/paren balance: 129/129, 218/218. 0 protected asset disentuh. `MICRO_UIUX_AUDIT.md`
diupdate (checklist 8/9). Item berikutnya (9/9, penutup kategori): audit dynamic type / RTL
layout mirroring.

## Batch 240 — Accessibility Micro-Polish 7/9: audit contrast, 0 bug, 0 kode + 2 dokumentasi
Item 7/9: audit contrast.

Cakupan audit app-wide:
1. `ResultBanner` (shared composable, 3 gaya: Solid/Tinted/Bare, dipakai 4 titik call site) —
   Solid pasang M3 container-role color + `onXContainer` pair (kontras dijamin sistem tema).
   Tinted & Bare terlihat mencurigakan sekilas (parameter `containerColor` dan `contentColor`
   diisi variabel warna yang SAMA) — tapi setelah baca implementasi: Bare tidak pernah memakai
   `containerColor` sama sekali (`Row` tanpa `.background()`), dan Tinted memakai
   `containerColor.copy(alpha = 0.15f)` sbg tint latar sementara `contentColor` teks solid
   penuh di atasnya — pola standar "tinted alert banner", bukan bug tabrakan warna.
2. Seluruh `onPrimary`/`onTertiary` per varian tema (`Theme.kt`) sudah disertai komentar
   perhitungan luminance manual (mis. "luma CalmRetroAccent ≈0.61, di atas threshold") — bukan
   nilai hardcode sembarangan.
3. `.alpha(0.4f)` utk disabled state (`LockScreen.kt` PinKey/RoundGlyphButton) — sesuai WCAG,
   kontrol dalam status disabled DIKECUALIKAN dari syarat rasio kontras teks/UI biasa.
4. Grep app-wide: 0 titik `Color.Gray`/`Color.LightGray`/hex hardcoded dipakai sbg warna teks
   (yang bisa memotong jalur jaminan kontras tema M3). 0 titik role `outline`/`outlineVariant`
   (dirancang low-contrast khusus border) disalahgunakan sbg warna teks — semua titik
   `surfaceVariant` yang match adalah `HorizontalDivider` (dekoratif) atau `Slider` inactive
   track, bukan teks.

**Hasil: 0 bug**, 0 kode disentuh.

Item berikutnya (8/9): pastikan informasi penting tidak hanya dibedakan lewat warna.

## Batch 239 — Accessibility Micro-Polish 6/9: audit text scaling, 0 bug, 0 kode + 2 dokumentasi
Item 6/9: audit text scaling (teks harus ikut membesar sesuai pengaturan ukuran font sistem,
tidak boleh terpotong/statis).

Cakupan audit app-wide:
1. Seluruh `fontSize`/`lineHeight` (di `Type.kt` maupun override lokal) — 100% pakai unit `.sp`,
   0 titik pakai `.dp` (kesalahan umum yang membuat teks tidak scaling).
2. Widget (`widget_player.xml`, `widget_player_compact.xml`) — `android:textSize` juga `sp`.
3. `AndroidManifest.xml` — tidak ada `android:configChanges` yang meng-exclude font-scale;
   default behavior (activity recreate saat font-scale berubah) tetap berlaku, state Compose
   aman lewat `rememberSaveable`/ViewModel seperti biasa.
4. Grep `TextOverflow.Clip` & `softWrap = false` app-wide — 0 titik ditemukan; yang ada hanya
   `TextOverflow.Ellipsis` & `basicMarquee()`, keduanya graceful (tidak hard-crop) di ukuran
   font besar.
5. Grep seluruh `.height()`/`.width()` fixed berdekatan dengan `Text(` — semua match ternyata
   `Spacer` (bukan pembungkus teks) atau container scrollable (`LazyColumn.heightIn(max=...)`,
   aman karena discroll, bukan clip). 0 titik container non-scroll fixed-height yang membungkus
   `Text` langsung ditemukan.

**Hasil: 0 bug**, 0 kode disentuh — konsisten dgn temuan kategori Iconography sebelumnya (Batch
230/232) yang juga sempat 0 bug di beberapa item verifikasi retrospektif.

Item berikutnya (7/9): audit contrast.

## Batch 238 — Accessibility Micro-Polish 5/9: minimum touch target StarRatingRow, 1 file kode + 2 dokumentasi
Item 5/9: audit minimum touch target (rekomendasi Material: 48dp). Grep app-wide seluruh
`IconButton(`/`FilledIconButton(` (40 titik) yang punya override `modifier = Modifier.size(...)`
eksplisit — cek apakah ada yang jatuh di bawah 48dp.

Ditemukan 1 titik: `StarRatingRow` (`NowPlayingScreen.kt`, 5 tombol rating bintang di Now
Playing screen) — `IconButton` di-override `Modifier.size(32.dp)`, jauh di bawah 48dp. Glyph
`Icon` di dalamnya sendiri sudah benar 20dp (itu ukuran visual, bukan area sentuh) — tapi area
sentuh keseluruhan tombolnya yang kena pangkas ke 32dp, bikin 5 bintang berjejer jadi target
sentuh sempit & rapat, riskan mis-tap terutama utk user dgn keterbatasan motorik.

**Fix**: hapus override `modifier = Modifier.size(32.dp)` dari `IconButton`, biarkan default
Material (48dp). `Icon` glyph di dalamnya TIDAK disentuh (tetap 20dp) — visual bintang persis
sama, cuma area sentuhnya melebar ke standar. Cek layout: `StarRatingRow` dipanggil full-width
di Now Playing screen (bukan dialog sempit), 5×48dp = 240dp — muat leluasa, tidak overflow.

Brace/paren balance: 215/215, 767/767.

Item berikutnya (6/9): audit text scaling.

## Batch 237 — Accessibility Micro-Polish 4/9: focus order form multi-field, 1 file kode + 2 dokumentasi
Item 4/9: audit focus order. Grep app-wide seluruh form multi-field (`OutlinedTextField`/
`BasicTextField` berurutan) — cek apakah ada rantai `imeAction`/`KeyboardActions` yang
menghubungkan fokus antar field, atau tiap field berdiri sendiri (default `ImeAction.Done`).

Ditemukan: `SongInfoEditSheet.kt` — form edit metadata lagu, 8 field berurutan (Judul → Artis →
Album → Artis Album → Genre → Komposer → No.Track/No.Disc bersebelahan dalam 1 `Row`) — SEMUA
tanpa `imeAction` eksplisit, default jatuh ke `ImeAction.Done`. Dampak: tombol "selesai" di
keyboard pada field pertama langsung MENUTUP keyboard, bukan lanjut ke field kedua — user
(termasuk pengguna keyboard eksternal/switch-access) wajib tap manual satu-satu ke tiap field,
padahal ini form linear yang jelas urutannya.

**Fix**: tambah `LocalFocusManager`. Field 1-6 (Judul s/d Komposer, tersusun vertikal) →
`KeyboardOptions(imeAction = ImeAction.Next)` + `KeyboardActions(onNext = {
focusManager.moveFocus(FocusDirection.Down) })`. No.Track (kiri dalam `Row` horizontal) → `Next`
+ `FocusDirection.Right` (lompat ke No.Disc, bukan Down yang salah arah). No.Disc (field
terakhir) → `ImeAction.Done` + `focusManager.clearFocus()` (baru boleh tutup keyboard di sini).
0 perubahan validasi/logic simpan data — murni urutan fokus & IME action.

Brace/paren balance: 49/49, 125/125.

Item berikutnya (5/9): audit minimum touch target.

## Batch 236 — Accessibility Micro-Polish 3/9: semantic role list pilihan tema, 1 file kode + 2 dokumentasi
Item 3/9: audit semantic role. Grep app-wide seluruh `.clickable(` (13 titik) — cross-check mana
yang sebetulnya representasi pola semantik lain (radio/tab/checkbox) tapi cuma pakai role
default (`Role.Button` implisit dari `clickable`).

Ditemukan: `ThemeOptionCard` (`SettingsScreen.kt`) — item dalam `LazyColumn` list pilihan
identitas tema (`ThemeIdentity.entries`, single-choice, exactly 1 aktif lewat
`currentThemeIdentity`), state terpilih ditandai visual lewat `border` 2dp `primary` — TAPI
pakai `Surface.clickable(onClick)` biasa. TalkBack tidak tahu ini grup pilihan-tunggal atau item
mana yang aktif; hanya baca "double tap to activate" generik.

**Fix**: `.clickable(onClick = onClick)` → `.selectable(selected = selected, onClick = onClick,
role = Role.RadioButton)`. Import baru: `androidx.compose.foundation.selection.selectable`,
`androidx.compose.ui.semantics.Role`. Visual TIDAK berubah (border-based selection tetap sama,
bukan diganti widget RadioButton — beda dgn pola speed-selector Batch 234 yang memang display
radio literal). Fungsi pilih tema tidak berubah.

Brace/paren balance: 136/136, 436/436.

Item berikutnya (4/9): audit focus order.

## Batch 235 — Accessibility Micro-Polish 2/9: decorative icon static contentDescription, 1 file kode + 2 dokumentasi
Item 2/9: audit content descriptions. Scan app-wide: semua `Icon(...)` (124 titik) sudah punya
parameter `contentDescription` eksplisit (0 gap param hilang). Audit lanjut ke kualitas isi
string — cek duplikat, placeholder generik, dan kecocokan dgn state icon.

Ditemukan 1 titik: icon peredam volume dalam-aplikasi (`NowPlayingScreen.kt`, di atas `Slider`
volume) — non-interactive, sudah ada `Text` label sibling persis di atasnya ("Peredam Dalam
Aplikasi (bukan volume HP)"). Icon-nya sendiri berubah glyph mengikuti level (mute/rendah/
tinggi) TAPI `contentDescription` di-hardcode string statis "Peredam dalam aplikasi" — TalkBack
mengumumkan stop tambahan yang tidak menambah info (sudah kebaca dari Text) dan tidak
merefleksikan level volume saat ini.

**Fix**: `contentDescription = "Peredam dalam aplikasi"` → `null`. Konsisten dgn konvensi
codebase (Batch 230): icon decorative + text label sibling → `null`, hindari TalkBack stop
ganda. Behavior/fungsi slider volume TIDAK berubah.

Brace/paren balance: 215/215, 768/768.

Item berikutnya (3/9): audit semantic role.

## Batch 234 — Accessibility Micro-Polish 1/9: TalkBack semantics RadioButton row, 1 file kode + 2 dokumentasi
Kategori baru dimulai: 🟡 ACCESSIBILITY MICRO-POLISH (setelah Iconography tuntas 7/7 di Batch
232-233). Item 1/9: audit TalkBack semantics pada interactive control.

Ditemukan di `NowPlayingScreen.kt` — 2 titik (speed selector di dialog "Pengaturan Putar", dan
`TransitionModeOption`): `Row` pakai `.clickable(onClick=...)` sebagai target sentuh utama, TAPI
`RadioButton` di dalamnya juga punya `onClick` sendiri → 2 node semantics terpisah bertumpuk.
Dampak: TalkBack fokus 2x per baris (swipe kanan pertama kena Row "double tap to activate" tanpa
role, swipe kedua kena RadioButton child "radio button, tidak dicentang") — membingungkan &
role/selected-state tidak terbaca di level baris yang benar.

**Fix**: Row modifier `.clickable(...)` → `.selectable(selected = isSelected, onClick = {...},
role = Role.RadioButton)`; `RadioButton(...)` onClick diset `null` (jadi visual-only, tidak
menyerap sentuhan/semantics sendiri — parent Row yang menyerap & mendeklarasikan role+state).
Hasil: 1 fokus TalkBack per baris, terbaca "radio button, dicentang/tidak dicentang, [label]".

Import ditambah: `androidx.compose.foundation.selection.selectable`,
`androidx.compose.ui.semantics.Role`. Behavior visual & fungsi pilih kecepatan/transisi TIDAK
berubah — murni perbaikan semantics aksesibilitas.

Brace/paren balance: 215/215, 768/768 (NowPlayingScreen.kt utuh).

Item berikutnya (2/9): audit content descriptions.

## Batch 233 — HOTFIX: import Icons.Error hilang (regresi Batch 228), 2 file kode + 2 dokumentasi
User laporkan CI build gagal (`log_fail_251.zip`, GitHub Actions `compileReleaseKotlin`/
`compileDebugKotlin`): `Unresolved reference: Error` di `BackupRestoreSheet.kt:131` &
`DiagnosticLogSheet.kt:130`.

**Root cause**: Batch 228 ganti `Icons.Default.ErrorOutline`→`Icons.Default.Error` di kode,
TAPI codebase ini pakai explicit per-icon import (bukan wildcard) — import statement lupa
diupdate, masih `import ...filled.ErrorOutline` sementara kode sudah pakai `Error`. Referensi
2 titik lain (`UpdateCheckSheet.kt`/`SignatureMatcherSheet.kt`) yg jadi acuan Batch 228 tidak
kena krn sudah punya import `Error` dari awal.

**Fix**: `import androidx.compose.material.icons.filled.ErrorOutline` → `.filled.Error` di
kedua file (urutan alfabetis tetap terjaga: CheckCircle→Error→FileDownload/Upload,
Archive→CheckCircle→DeleteOutline→Error). Brace/paren balance (33/33+89/89, 16/16+68/68).

**Safety net**: scan app-wide seluruh `Icons.Default.X` vs import statement — 0 gap lain
ditemukan (Batch 226/227/229/231 aman, tidak sentuh import).

---

## Batch 232 — Iconography 7/7 penutup: verifikasi retrospektif no-cosmetic-affordance-change (0 bug, 0 kode + 2 dokumentasi) → 🟠 ICONOGRAPHY TUNTAS 7/7 (Batch 224-232)
Item terakhir kategori Iconography bersifat guard-rail (bukan proaktif ganti icon), jadi
dieksekusi sbg verifikasi retrospektif: ditelusuri ulang 4 fix kode sepanjang Batch 224-231,
pastikan tidak ada yg murni kosmetik & mengaburkan affordance.

- Batch 224 (34dp→40dp): ukuran saja, glyph/makna tetap.
- Batch 226-227 (offset +1dp): posisi mikro, bukan ganti glyph.
- Batch 228 (`ErrorOutline`→`Error`): ganti glyph, TAPI berdasar penyamaan bobot dgn pasangan
  `CheckCircle` solid yg sudah eksis di 2 titik lain — konsistensi makna status, bukan estetika.
- Batch 229 (tint `primary`→`secondary`): warna saja; affordance malah DIPERJELAS (pisahkan
  decorative dari actionable), bukan dikaburkan.
- Batch 231 (contentDescription teks): 0 icon visual berubah sama sekali.

**Hasil: 0 pelanggaran**, 0 kode disentuh. `MICRO_UIUX_AUDIT.md` diupdate — checklist 7/7 +
`STATUS TRACKING` baris 6-14 ditandai Iconography TUNTAS.

**Rekap kategori Iconography**: ukuran(224, 1 fix)/optical alignment(226-227, 1 fix, 4 titik)/
visual weight(228, 1 fix)/action-vs-decorative(229, 1 fix)/contentDescription null(230, 0 bug)/
semantic label(231, 1 fix)/no-cosmetic-affordance-change(232, verifikasi 0 bug). Kategori
berikutnya (belum mulai): 🟡 ACCESSIBILITY MICRO-POLISH.

---

## Batch 231 — Iconography 6/7: semantic label actionable icon (1 bug fix, 1 file kode + 2 dokumentasi)
Audit label content description tombol toggle playback mode di `NowPlayingScreen.kt`.

**Bug**: tombol Shuffle & Repeat (3-state OFF→ALL→ONE via `cycleRepeatMode()`) pakai label
statis (`"Acak"`/`"Ulangi"`) — status ON/OFF/mode aktif cuma dibedakan lewat `tint`
(`animatedAccent` vs `colorScheme.secondary`), TalkBack tidak baca warna. Repeat lebih parah:
glyph `Icons.Default.Repeat` identik utk state OFF & ALL (cuma `RepeatOne` beda glyph) —
screen-reader user 100% tidak bisa bedakan OFF vs ALL aktif.

**Fix**: label ikut state — Shuffle: `"Acak: aktif"`/`"Acak: nonaktif"`. Repeat: `"Ulangi:
mati"`/`"Ulangi: semua lagu"`/`"Ulangi: satu lagu"`. Brace/paren `NowPlayingScreen.kt` balance
(216/216, 769/769).

`MICRO_UIUX_AUDIT.md` diupdate (checklist item 6/7). Item berikutnya (7/7, penutup kategori):
jangan mengganti icon hanya demi estetika jika mengubah affordance. Belum diverifikasi TalkBack
di device asli.

---

## Batch 230 — Iconography 5/7: contentDescription null hanya utk decorative (0 bug, 0 kode + 2 dokumentasi)
Grep 69 titik `contentDescription = null` app-wide, dicek satu-satu: semua genuinely decorative
— icon+Text sibling dlm `Button`/`TextButton`/`NavigationBarItem` (label Text terpisah)/
`AlertDialog` (icon dialog standar)/`ListItem leadingContent`/menu `leadingIcon`, atau badge
status murni tanpa onClick (`GraphicEq` "Sedang diputar", `Lock`/`LockOpen` status vault).

Cross-check terpisah: scan window ±12 baris tiap `IconButton(`/`FilledIconButton(` app-wide
mencari kombinasi icon-only actionable + `contentDescription = null` (pola bug aksesibilitas
klasik — icon-only button tanpa label teks butuh deskripsi eksplisit utk TalkBack). **0 titik
ditemukan** — semua `IconButton` icon-only app ini sudah konsisten pakai deskripsi string
(`"Putar"`, `"Jeda"`, `"Hapus lirik"`, dst, lihat Batch 226-228 utk contoh).

**Hasil: 0 bug**, 0 kode disentuh. `MICRO_UIUX_AUDIT.md` diupdate (checklist item 5/7). Item
berikutnya (6/7): semua actionable icon harus memiliki semantic/content label yang sesuai.

---

## Batch 229 — Iconography 4/7: action vs decorative icon (1 bug fix, 2 file kode + 2 dokumentasi)
Audit tint icon decorative (badge status, 0 onClick) app-wide. Konvensi codebase: icon
decorative pakai `colorScheme.secondary` (contoh: `MusicNote` fallback album art di
`LibraryScreen.kt`/`Utils.kt`/`MainActivity.kt`, drag-handle `QueueSheet.kt`), sementara
`primary` reserved utk icon actionable/tombol (mis. `animatedAccent` play/pause).

**Bug**: badge "Sedang diputar" (`Icons.Default.GraphicEq`, murni indikator status — tidak
ada onClick sama sekali) di `QueueSheet.kt` & `LibraryScreen.kt` pakai tint `primary` —
menyamai warna icon actionable, bikin ambigu seolah badge ini bisa di-tap padahal cuma status.

**Fix**: `primary` → `secondary` di kedua titik, samakan dgn baris drag-handle persis di
atasnya (`QueueSheet.kt`) yang sudah pakai `secondary` dgn benar. Brace/paren kedua file
balance (40/40+131/131, 336/336+725/725).

`MICRO_UIUX_AUDIT.md` diupdate (checklist item 4/7). Item berikutnya (5/7):
`contentDescription = null` hanya untuk icon yang benar-benar decorative. Belum diverifikasi
visual di device asli.

---

## Batch 228 — Iconography 3/7: samakan visual weight icon sejenis (1 bug fix, 2 file kode + 2 dokumentasi)
Grep semua call site `ResultBanner(...)` (pola sukses/gagal): 4 titik — `SignatureMatcherSheet.kt`,
`UpdateCheckSheet.kt`, `BackupRestoreSheet.kt`, `DiagnosticLogSheet.kt`.

**Bug**: 2/4 titik (`BackupRestoreSheet.kt`, `DiagnosticLogSheet.kt`) pasangkan `CheckCircle`
(solid, bobot tebal) dengan `ErrorOutline` (garis tipis, bobot ringan) untuk pasangan
sukses/gagal yang sama persis secara semantik — tidak konsisten dengan 2 titik lain
(`SignatureMatcherSheet.kt`, `UpdateCheckSheet.kt`) yang sama-sama pakai bobot solid
(`CheckCircle` + `Error`).

**Fix**: ganti `Icons.Default.ErrorOutline` → `Icons.Default.Error` di kedua file, samakan
dengan pola referensi `SignatureMatcherSheet.kt`. Brace/paren kedua file balance
(33/33+89/89, 16/16+68/68).

`MICRO_UIUX_AUDIT.md` diupdate (checklist item 3/7). Item berikutnya (4/7): pastikan action
icon dapat dibedakan dari decorative icon. Belum diverifikasi visual di device asli.

---

## Batch 227 — Iconography 2/7 penutup: fix HomeScreen.kt (1 bug fix, 1 file kode + 2 dokumentasi)
Menutup Pending Queue Batch 226. `HomeScreen.kt` tombol "Lanjutkan" (continue-listening, 48dp)
pakai `Icons.Default.PlayArrow` selalu (bukan toggle Play/Pause seperti 3 titik lain) — jadi
offset +1dp diterapkan tetap/tidak kondisional, konsisten dgn fix Batch 226. Brace/paren
`HomeScreen.kt` balance (67/67, 202/202).

**Iconography item "audit optical alignment" TUNTAS 4/4 titik** (Batch 226-227).
`MICRO_UIUX_AUDIT.md` diupdate. Item berikutnya (3/7): samakan visual weight icon sejenis.
Belum diverifikasi visual di device asli.

---

## Batch 226 — Iconography 2/7: audit optical alignment (1 bug fix, 3 file kode + 2 dokumentasi)
Grep app-wide `Icons.Default.PlayArrow`: 4 titik ditemukan (`NowPlayingScreen.kt` tombol utama
68dp, `MiniPlayerBar.kt` tombol mini 40dp, `LyricsSheet.kt` tombol sync lirik, `HomeScreen.kt`
tombol "Lanjutkan" continue-listening 48dp).

**Bug**: glyph segitiga PlayArrow secara optik condong ke kiri dalam bounding box 24dp-nya
(vector Material bawaan tidak simetris kiri-kanan seperti Pause). Di 4 titik ini, Play↔Pause
di-swap di posisi/ukuran yang sama persis lewat `AnimatedContent` — akibatnya PlayArrow
berpotensi kelihatan "kegeser kiri" dari titik pusat tombol, padahal Pause pas center.

**Fix**: tambah `Modifier.offset(x = 1.dp)` kondisional — HANYA aktif saat state PlayArrow
(`!playing`/`!isPlaying`), Pause tidak disentuh. Diterapkan ke 3/4 titik: `NowPlayingScreen.kt`,
`MiniPlayerBar.kt`, `LyricsSheet.kt`. Brace/paren ketiga file balance (215/215+762/762,
12/12+96/96, 63/63+165/165).

**Pending Queue (micro-batching cap 3 file/batch)**: `HomeScreen.kt` tombol "Lanjutkan" — fix
sama (offset +1dp saat PlayArrow), belum dikerjakan batch ini.

`MICRO_UIUX_AUDIT.md` diupdate (checklist item 2/7 + `STATUS TRACKING`). Item berikutnya (3/7):
samakan visual weight icon sejenis. Belum diverifikasi visual di device asli.

---

## Batch 225 — Verifikasi visual Batch 224 (Play/Pause icon fix, 0 kode + 2 dokumentasi)
Screenshot user (tema Tactile/Skeu, squircle shape) mengonfirmasi fix Batch 224: icon Play/Pause
(40dp) sekarang tampak jelas lebih besar dari icon Skip Previous/Next di sisi kirinya —
hierarki visual 3-tingkat (Shuffle/Repeat terkecil < Skip < Play/Pause terbesar) sudah benar
secara visual, tidak ada lagi kesan tombol besar tapi glyph kecil. 0 distorsi/kepenuhan dalam
lingkaran/squircle accent. `MICRO_UIUX_AUDIT.md` & `CHANGELOG.md` Batch 224 diupdate status
"belum diverifikasi visual" → "terverifikasi visual (Batch 225)".

## Batch 224 — Iconography item 1/7: audit ukuran icon (1 bug fix — Play/Pause NowPlaying 34dp→40dp)
Kategori baru dimulai (Settings TUNTAS 9/9 di Batch 223). Item 1/7 § Iconography
`MICRO_UIUX_AUDIT.md`: "Audit ukuran icon". Grep semua `Icon(` app-wide (113 titik total di
`ui/*.kt`): **104 default 24dp** (baseline mayoritas konsisten), 9 pakai `.size()` custom eksplisit
tersebar di 4 file — semua dicek konteksnya satu-satu.

**8 dari 9 justified** (peran UI beda, bukan gap): 2× 18dp (`ABRepeatBookmarkSheet` — icon di
dalam `TextButton` disandingkan teks, disamakan tinggi baris teks bukan standalone-icon-button;
pola sama precedent Batch 151-152), 1× 18dp `RingtoneCutterSheet` (leading icon `FilterChip`,
konvensi M3 chip beda dari standalone icon), 2× 16dp Close (1 di dalam `TextButton`+teks sama
alasan di atas; 1 di `FeatureHintBanner` sudah didokumentasikan sadar sejak Batch 141 — hit-target
48dp vs ukuran visual 2 hal beda, dikonfirmasi ulang bukan oversight), 2× 24dp `LockScreen`
(redundant eksplisit tapi SENGAJA disamakan ke default sejak Batch 147, bukan gap baru).

**1 dari 9 genuinely bug**: Icon Play/Pause `NowPlayingScreen.kt` (tombol hero, kontainer PALING
BESAR di row kontrol — 68dp vs default ~48dp `IconButton`) glyph-nya cuma **34dp — LEBIH KECIL**
dari icon `SkipPrevious`/`SkipNext` yang mengapitnya (36dp eksplisit). Row kontrol lengkap
seharusnya 3-tingkat hierarki bobot visual: Shuffle/Repeat (24dp default, aksi sekunder) < Skip
(36dp, aksi navigasi) < Play/Pause (aksi utama, harus PALING besar) — tapi 34 < 36 membalik
tingkat teratas. 0 komentar histori menjelaskan angka 34dp secara spesifik (beda dari kontainernya
sendiri yang punya banyak komentar shape/warna/emboss per-batch), ciri oversight bukan keputusan
sadar.

**Fix**: `34.dp` → `40.dp`. Hierarki dipulihkan (24 < 36 < 40), padding internal circle tetap
proporsional (68−40=28dp total ruang, lebih lega dari margin skip-icon 48−36=12dp, jadi tetap
"bernapas" bukan penuh sesak). 1 file (`NowPlayingScreen.kt`), 1 baris kode. Brace/paren
seimbang (215/215, 756/756). **✅ Terverifikasi visual (Batch 225, screenshot user)** — hierarki
3-tingkat sudah kelihatan benar, 0 distorsi/kepenuhan dalam squircle accent. `MICRO_UIUX_AUDIT.md`
diperbarui (checklist item 1/7). Item berikutnya (2/7): audit optical alignment.

## Batch 223 — Settings polish item 9/9: verifikasi fungsi setting tidak berubah (audit, 0 bug) — SETTINGS 9/9 TUNTAS
Item terakhir § Settings `MICRO_UIUX_AUDIT.md`: "Jangan mengubah fungsi setting" — verifikasi
akhir bahwa Batch 215-222 (8 item sebelumnya di kategori ini) 0 mengubah behavior fungsional.

Ditelusuri titik wiring `MainActivity.kt` → `SettingsScreen(...)`: seluruh 9 callback
(`onSelectThemeIdentity`/`onSelectThemeMode`/`onSetPin`/`onDisableLock`/`onToggleBiometric`/
`onToggleShakeToSkip`/`onToggleRadioAutoContinue`/`onToggleFloatingBubble`/
`onToggleSilenceSkip`) tetap terpasang ke fungsi `PlayerViewModel` yang sama persis seperti
sebelum Batch 215 — **0 baris di `MainActivity.kt` pernah tersentuh** sepanjang seluruh siklus
9 batch ini. Cuma `SettingsScreen.kt` yang pernah diedit (Batch 216: subtitle ditambah; Batch
220: state var + `AlertDialog` konfirmasi ditambah). Batch 220 satu-satunya yang menyentuh alur
eksekusi — tapi `onDisableLock()` masih dipanggil dengan efek 100% identik saat user menekan
"Nonaktifkan"; yang berubah cuma perlu 1 tap konfirmasi ekstra sebelum trigger, bukan efek
`disableLock()` itu sendiri (`AppLockStore.kt`/`PlayerViewModel.kt` juga 0 tersentuh). Bukan
perubahan fungsi setting — murni safety-gate di depan fungsi yang sama.

**Hasil: 0 bug, 0 kode disentuh.** `MICRO_UIUX_AUDIT.md` diperbarui (checklist item 9/9 — kategori
🟠 SETTINGS **TUNTAS 9/9**, Batch 215-223). `STATUS TRACKING` (baris 20 tabel kategori 6-14)
diupdate reflect penutupan. Kategori berikutnya (belum mulai): 🟡 ICONOGRAPHY.

## Batch 222 — Settings polish item 8/9: audit visual density (audit, 0 bug)
Lanjutan Batch 215-221, item 8/9 § Settings `MICRO_UIUX_AUDIT.md`: "Kurangi visual density tanpa
menghilangkan informasi". Ditelusuri seluruh 848 baris `SettingsScreen.kt` — pola spacing pakai
skala Material konsisten (`4dp` title→deskripsi, `8dp` title→konten pertama, `12dp` antar-item
switch, `20dp` antar-section, sudah diverifikasi struktural di Batch 217).

Titik paling "berat" secara tekstual: 4 switch di "Perilaku Pemutaran" — subtitle terpanjang
("Mini Player Mengambang" 3 baris, "Lewati Keheningan Otomatis" 4 baris) berisi info fungsional
non-basa-basi: syarat izin sistem, batasan teknis (belum ada slider sensitivitas), efek samping
potensial (bisa motong intro/outro), saran mitigasi (coba dulu, matikan kalau ganggu). **Semua
kalimat itu WAJIB tetap ada** — instruksi item ini sendiri eksplisit "tanpa menghilangkan
informasi", jadi memangkas kalimat penjelasan izin sistem/batasan teknis demi baris lebih pendek
justru melanggar syarat item ini sendiri, bukan memenuhinya.

**0 titik ditemukan** dengan elemen bertumpuk, spacing di bawah `4dp` (minimum readable gap), atau
Row berisi >3 elemen visual sekaligus yang bisa dianggap "padat" secara objektif. Kepadatan yang
ada murni konsekuensi dari jumlah informasi yang memang perlu disampaikan (bukan tata letak boros
atau redundan) — konsisten dengan kesimpulan spacing Batch 217.

**Hasil: 0 bug, 0 kode disentuh.** `MICRO_UIUX_AUDIT.md` diperbarui (checklist item 8/9). Item
terakhir kategori Settings (9/9): jangan mengubah fungsi setting — verifikasi akhir bahwa 8
perbaikan/audit Batch 215-222 di kategori ini 0 mengubah behavior fungsional apa pun.

## Batch 221 — Settings polish item 7/9: audit disabled setting visibility (audit, 0 bug)
Lanjutan Batch 215-220, item 7/9 § Settings `MICRO_UIUX_AUDIT.md`: "Pastikan disabled setting
terlihat jelas". Grep `enabled = ` di seluruh `SettingsScreen.kt` — cuma **1 titik** di seluruh
file: `Switch` "Mode Gelap" di-`enabled = !followSystem` (nonaktif otomatis saat "Ikuti Sistem"
ON, krn logikanya jadi tidak relevan).

**Dicek 2 lapis**: (1) `Switch` ini 0 pakai `colors = SwitchDefaults.colors(...)` custom — murni
default Material3, yang otomatis render dgn `disabledCheckedThumbColor`/
`disabledUncheckedTrackColor` (alpha diturunkan bawaan) saat `enabled = false`, jadi switch-nya
sendiri sudah visually muted tanpa perlu sentuhan tambahan. (2) Subtitle di bawah judul "Mode
Gelap" SUDAH eksplisit ganti teks jadi "Nonaktif — mengikuti pengaturan sistem" tiap kali
`followSystem = true` — bukan sekadar warna redup, tapi kalimat penjelasan langsung kenapa
switch itu tidak bisa disentuh. Kombinasi switch teredup (M3 default) + subtitle penjelasan
eksplisit sudah lebih jelas dari sekadar dim visual generik.

Sempat dipertimbangkan menambah `alpha` manual pada Text judul "Mode Gelap" biar match tone
redup switch, tapi di-grep app-wide (`alpha = 0.38`/`LocalContentColor`/`ContentAlpha`) — **0
precedent** pola dim-title-saat-disabled di mana pun di codebase ini. Menambah alpha sepihak di
1 titik ini justru jadi elemen baru yang tidak konsisten dgn sisa app, bukan perbaikan.

**Hasil: 0 bug, 0 kode disentuh.** `MICRO_UIUX_AUDIT.md` diperbarui (checklist item 7/9). Item
berikutnya (8/9): kurangi visual density tanpa menghilangkan informasi.

## Batch 220 — Settings polish item 6/9: fix destructive setting (nonaktifkan kunci PIN, 1 file kode + 2 dokumentasi)
Lanjutan Batch 215-219, item 6/9 § Settings `MICRO_UIUX_AUDIT.md`: "Pastikan destructive setting
terlihat berbeda". Ditelusuri sampai layer data: `AppLockStore.disableLock()` **menghapus
permanen** `KEY_PIN_HASH`+`KEY_SALT` dari `SharedPreferences` — kalau lock diaktifkan lagi nanti,
PIN harus dibuat ulang dari nol, 0 cara mengembalikan PIN lama. Tapi di UI, aksi ini dipicu
langsung dari 1 gerakan jari (toggle `Switch` OFF) — **0 konfirmasi, 0 pembeda visual warna
error**, padahal efeknya permanen. Persis pola bug yang sudah dikonfirmasi & diperbaiki Batch 195
di kategori Playlist/Queue ("Hapus playlist" dulu 0 konfirmasi).

**Fix** `SettingsScreen.kt`: `onCheckedChange` saat toggle OFF tidak lagi panggil
`onDisableLock()` langsung, tapi buka `AlertDialog` konfirmasi baru ("Nonaktifkan Kunci
Aplikasi?" + penjelasan PIN akan dihapus permanen + wajib dibuat ulang dari awal kalau
diaktifkan lagi). Tombol konfirmasi "Nonaktifkan" pakai `color = MaterialTheme.colorScheme.error`,
ikon dialog `Icons.Default.LockOpen` di-tint error juga. Tombol "Batal" netral. `Switch` sendiri
tetap `checked = lockEnabled` terikat state asli dari parent (bukan state toggle lokal) — kalau
user tekan "Batal", switch otomatis balik ke posisi semula tanpa flicker/state-jump, karena
`disableLock()` beneran belum pernah dipanggil.

1 import baru (`Icons.Default.LockOpen`, eksplisit — file ini tidak pakai wildcard icons). Brace/
paren `SettingsScreen.kt` seimbang (136/136, 436/436). 0 protected asset. **Belum diverifikasi
visual** — prioritas cek dialog konfirmasi muncul benar saat toggle OFF, switch tidak
kejump/flicker saat "Batal" ditekan, dan aktivasi ulang lock (`showSetPinDialog`) tetap jalan
seperti sebelumnya (jalur ini tidak disentuh). `MICRO_UIUX_AUDIT.md` diperbarui (checklist item
6/9). Item berikutnya (7/9): pastikan disabled setting terlihat jelas.

## Batch 219 — Settings polish item 5/9: audit navigation affordance (audit, 0 bug)
Lanjutan Batch 215-218, item 5/9 § Settings `MICRO_UIUX_AUDIT.md`: "Audit navigation
affordance". 7 baris navigasi (buka sheet/dialog lain) di `SettingsScreen.kt` — Statistik
Dengar/Cadangkan & Pulihkan/Deteksi File Duplikat/Vault Lagu Privat/Cek Signature APK/Log
Diagnostik/Cek Update — dicek: **tidak satu pun** punya ikon chevron/panah trailing yang
menandakan "baris ini navigasi ke tempat lain". Awalnya dicurigai gap, tapi di-grep seluruh
`ui/` folder untuk pola `ChevronRight`/`KeyboardArrowRight`/`Arrow...Forward`/`NavigateNext` —
**0 hasil di mana pun**, termasuk di file lain (`PlaylistScreen.kt`, `QueueSheet.kt`, dst).

**Kesimpulan**: bukan gap, tapi pola desain app-wide yang konsisten — affordance klik ditandai
lewat kombinasi ripple bawaan `.clickable{}` full-width row + icon prefix + title/subtitle,
BUKAN chevron trailing (app ini 0% pernah pakai pola chevron di mana pun, jadi menambah chevron
justru akan jadi elemen baru yang tidak konsisten dengan sisa app). Beda kasus dengan baris
"Lanjutan" (expand/collapse) yang MEMANG punya ikon `ExpandMore`/`ExpandLess` trailing — tapi itu
bukan pembanding valid, krn perannya beda (toggle show/hide state di tempat, bukan navigasi ke
layar lain — ikon expand/collapse WAJIB ada krn merepresentasikan STATE, bukan sekadar affordance
klik).

**Hasil: 0 bug, 0 kode disentuh.** `MICRO_UIUX_AUDIT.md` diperbarui (checklist item 5/9). Item
berikutnya (6/9): pastikan destructive setting terlihat berbeda.

## Batch 218 — Settings polish item 4/9: audit switch/toggle alignment (audit, 0 bug)
Lanjutan Batch 215-217, item 4/9 § Settings `MICRO_UIUX_AUDIT.md`: "Audit switch/toggle
alignment". Grep semua `Switch(` di `SettingsScreen.kt` — 8 titik total: 4 di "Perilaku
Pemutaran" (Goyang untuk Lagu Berikutnya/Lanjutkan Otomatis/Mini Player Mengambang/Lewati
Keheningan), 2 di tema (Ikuti Sistem/Mode Gelap), 2 di kunci app (Kunci Aplikasi PIN/Buka dengan
Sidik Jari).

**Semua 8 dicek satu-satu**: pola identik tanpa kecuali — `Row(verticalAlignment =
Alignment.CenterVertically)` membungkus `Column(modifier = Modifier.weight(1f))` berisi
title(+subtitle) di kiri, lalu `Switch(...)` polos (0 modifier ukuran/offset custom) di kanan.
Tidak ada satu pun titik yang pakai `Alignment.Top`/`Alignment.Bottom` atau `Switch` dengan
padding/size manual yang bisa bikin switch kegeser dari center row-nya. Perbedaan `padding`
pembungkus luar (`Modifier.padding(horizontal = 20.dp)` langsung di beberapa Row vs sudah
diwarisi dari `Column` orang tua di titik lain) tidak berpengaruh ke alignment vertikal internal
Row itu sendiri — struktural per konteks pemanggil, bukan inkonsistensi alignment.

**Hasil: 0 bug, 0 kode disentuh.** `MICRO_UIUX_AUDIT.md` diperbarui (checklist item 4/9). Item
berikutnya (5/9): audit navigation affordance.

## Batch 217 — Settings polish item 3/9: spacing antar setting (audit, 0 bug)
Lanjutan Batch 215-216, item 3/9 § Settings `MICRO_UIUX_AUDIT.md`: "Samakan spacing antar
setting". Audit menyeluruh `SettingsScreen.kt` pasca restrukturisasi Batch 215-216 (khawatir
perubahan struktur "Alat & Utilitas" bisa saja meninggalkan spacing yang tidak sinkron lagi
dengan bagian lain file).

**3 sub-pola dicek terpisah**:
1. **Transisi antar-section** (4 titik: masuk ke Perilaku Pemutaran/Alat & Utilitas/Lanjutan/
   Tentang Aplikasi) — `Spacer(12dp)→HorizontalDivider→Spacer(20dp)` identik di ke-4-nya.
2. **Title section → konten pertama** (3 titik: Perilaku Pemutaran/Alat & Utilitas/Tentang
   Aplikasi) — `Spacer(8dp)` identik. ("Tema" beda krn punya deskripsi section tambahan sebelum
   konten, "Lanjutan" beda krn struktur collapsible dgn sub-title bersarang — keduanya kasus
   struktural beda, bukan pembanding apple-to-apple utk gap ini.)
3. **Antar-item DALAM 1 section** — di sinilah ditemukan 2 angka berbeda: switch-row (4 toggle
   "Perilaku Pemutaran") pakai `Spacer(12dp)` statis antar-row, row itu sendiri 0 padding
   vertikal tambahan; nav-row (4 "Alat & Utilitas" + 2 "Alat Developer") pakai `Spacer(4dp)` +
   `padding(vertical=8dp)` bawaan tiap row → gap visual efektif ~20dp, LEBIH LEBAR dari
   switch-row.

**Dicek apakah ini bug atau disengaja** (bukan langsung disamakan angkanya): switch-row target
sentuhnya cuma komponen `Switch` di kanan (Material sudah menegakkan touch-target minimum
sendiri di situ), jadi `Row` pembungkus tidak perlu padding vertikal ekstra — `Spacer(12dp)`
murni jarak visual antar-baris. Nav-row SELURUH `Row` adalah `.clickable{}` (baris demi baris
navigasi ke sheet lain) — `padding(vertical=8dp)` di situ BUKAN estetika, itu bagian dari target
sentuh fungsional row (lebih lebar dari cuma tinggi teks 2 baris). Dua "spesies" row dengan
afinitas interaksi berbeda secara sah punya angka spacing berbeda — persis pola yang sudah
dikonfirmasi Batch 181 di kategori Library/Song List ("ukuran art beda krn peran UI beda, bukan
inkonsistensi yang perlu disamakan").

**Hasil: 0 bug, 0 kode disentuh.** `MICRO_UIUX_AUDIT.md` diperbarui (checklist item 3/9 dgn
detail temuan + status tracking kategori 6-14). Item berikutnya (4/9): audit switch/toggle
alignment.

## Batch 216 — Settings polish item 2/9: title/subtitle row
Lanjutan Batch 215. Audit `SettingsScreen.kt` § "Konsistenkan title/subtitle row" (item 2/9 §
Settings, `MICRO_UIUX_AUDIT.md`): 7 baris navigasi berpola `Icon + Spacer(12dp) + title` dikenali
di seluruh file — 4 di antaranya ("Alat & Utilitas": Statistik Dengar/Cadangkan & Pulihkan/
Deteksi File Duplikat/Vault Lagu Privat, hasil Batch 215) sudah title+subtitle per-item; 3 sisanya
title-only ("Cek Signature APK", "Log Diagnostik", "Cek Update").

**Dicek satu-satu, bukan disamakan semua secara buta**: "Cek Signature APK" & "Log Diagnostik"
dinaungi 1 `Text` deskripsi section bersama "Alat Developer" ("Bukan untuk penggunaan sehari-hari
— dipakai untuk mengecek APK sebelum instal update manual.") tepat sebelum keduanya — pola ini
identik dengan section "Tema"/"Perilaku Pemutaran" di file yang sama, yang juga pakai 1 deskripsi
section untuk menaungi beberapa item sekaligus (bukan subtitle per-item). Menambah subtitle
duplikat ke 2 baris ini justru mengulang info yang sudah ada, bukan konsistensi. "Cek Update"
BEDA — title-only TANPA satu pun kalimat penjelas di dekatnya (section "Tentang Aplikasi" cuma
punya deskripsi soal app secara umum, bukan soal aksi cek-update itu sendiri) — genuinely satu-
satunya baris yang berdiri sendiri tanpa konteks.

**Fix**: subtitle ditambah ke "Cek Update" saja — "Cek versi APK terbaru dari GitHub Release —
satu-satunya koneksi internet di app ini" (dikonfirmasi akurat ke `UpdateCheckSheet.kt`, yang
memang cuma dipicu manual lewat baris ini, tidak pernah otomatis — komentar KDoc file itu sendiri
sudah menyebut ini "satu-satunya tempat app ini pernah menyentuh jaringan"). Struktur baris
diubah dari `Icon+Spacer+Text` jadi `Icon+Spacer+Column{Text+Text}` (pola identik 4 baris "Alat &
Utilitas"), 0 logic/`onClick`/navigasi berubah.

0 protected asset. Brace/paren `SettingsScreen.kt` seimbang (124/124, 426/426). **Belum
diverifikasi visual**. `MICRO_UIUX_AUDIT.md` diperbarui (status tracking + checklist item 2/9).
Item berikutnya (3/9): "Samakan spacing antar setting" — kandidat cross-check dengan audit Batch
151 (kategori Spacing) yang sudah pernah memeriksa pola spacing section serupa dan menemukan 0
bug; batch berikutnya perlu verifikasi ulang apakah kesimpulan itu masih berlaku setelah struktur
section berubah di Batch 215-216, atau genuinely tuntas tanpa kerja tambahan.

## Batch 215 — Settings polish item 1/9: grouping antar section
Next pending sesuai `MICRO_UIUX_AUDIT.md` § FINAL EXECUTION ORDER — kategori 9 (Playlist/Queue)
tuntas 8/8 (Batch 191-214, termasuk fix drag-reorder Batch 214), lanjut kategori 10 (Settings
polish), item 1/9: "Konsistenkan grouping antar section".

**Audit `SettingsScreen.kt`**: pola section di file ini konsisten SELALU 1 title (`titleMedium`)
menaungi 1+ item terkait di bawahnya, dipisah `HorizontalDivider` dari section berikutnya — mis.
"Tema" (1 deskripsi + grid kartu tema), "Perilaku Pemutaran" (4 switch tanpa title per-switch),
"Lanjutan" (2 sub-section "Keamanan"+"Alat Developer"), "Tentang Aplikasi" (versi + tombol Cek
Update). **1 gap ditemukan**: 4 baris tool berurutan — Statistik Dengar, Cadangkan & Pulihkan,
Deteksi File Duplikat, Vault Lagu Privat — masing-masing dibungkus `HorizontalDivider` SENDIRI
tanpa title section sama sekali, tampak visual seperti 4 section kosong-nama berturut-turut,
bukan 1 kelompok "alat/utilitas" yang koheren.

**Fix**: 1 title baru "Alat & Utilitas" ditambah sebelum baris Statistik Dengar (title pertama
dari kelompok), 3 `HorizontalDivider` yang tadinya memisahkan Cadangkan/Duplikat/Vault dari
tetangganya dibuang, diganti `Spacer(height=4.dp)` — antar-item dalam 1 section yang sama tidak
butuh divider (konsisten dengan pola "Perilaku Pemutaran" yang juga cuma pakai `Spacer(12.dp)`
antar switch, bukan divider). Divider transisi MASUK (dari section "Identitas Tema"/kartu tema)
dan KELUAR (ke section "Lanjutan") sengaja TIDAK disentuh — itu batas section yang genuinely
berbeda, cuma 3 batas INTERNAL antar 4 item yang dihapus.

0 logic/state/navigasi/aksi berubah — murni `Text`/`Spacer`/`HorizontalDivider` restrukturisasi
visual, `onClick`/`onOpenStats`/dst. semua identik. 0 protected asset. Brace/paren `SettingsScreen.kt`
seimbang (123/123, 421/421). **Belum diverifikasi visual** — prioritas cek section baru tidak
terasa terlalu padat (kandidat silang-cek dengan item 8/9 "kurangi visual density" nanti) dan
transisi in/out section masih terasa jelas tanpa divider internal.

`MICRO_UIUX_AUDIT.md` diperbarui: status tracking kategori 6-14 (Playlist/Queue ditandai tuntas
8/8, Settings dimulai) + checklist Settings item 1/9 dicentang dengan detail. Item berikutnya
(2/9): konsistenkan title/subtitle row.

## Batch 214 — Fix drag reorder buggy (patah-patah/lompat/susah mulai/nyentak) — 2 file kode + 1 dokumentasi
User laporan drag reorder buggy di ke-4 aspek sekaligus (stutter, reorder meleset, susah mulai,
gak smooth pas selesai) — pola gejala klasik 1 root cause, bukan 4 bug terpisah. **Root cause**:
`Modifier.animateItemPlacement()` (auto-spring reposisi bawaan Compose tiap key LazyColumn
pindah slot) tetap aktif di row yang SEDANG di-drag, PADAHAL row itu udah dikontrol manual lewat
`graphicsLayer { translationY = dragOffsetPx }`. Dua-duanya rebutan kendali posisi Y row yang
sama tiap kali `onMove` terpanggil (posisi list beneran berubah → key geser slot → Compose mulai
animasi spring KE posisi manual yang lagi di-drag) — hasilnya: row keliatan patah-patah (2
animasi tarik-menarik), threshold reorder jadi gampang meleset (base offset row bergeser tanpa
sepengetahuan drag-delta), kesan "susah mulai" (glitch visual pas frame pertama), dan nyentak
pas dilepas (animasi spring baru sempat aktif nyusul). Fix (`QueueSheet.kt` +
`PlaylistScreen.kt`, logic identik): `animateItemPlacement()` di-skip KHUSUS utk row yang lagi
`isDragging` (`Modifier.then(if (isDragging) Modifier else Modifier.animateItemPlacement())`) —
row lain (yang didorong geser slot karena drag) TETAP dapat animasi masuk-slot-baru yang mulus
(itu efek "buka jalan" yang diinginkan, mirip iOS), cuma row yang tangannya lagi dipegang user
yang murni manual. Brace/paren kedua file seimbang (`QueueSheet.kt` 40/40,128/128;
`PlaylistScreen.kt` 127/127,208/208). 0 protected asset. **Belum diverifikasi device** —
prioritas cek smoothness di HP user (Infinix XOS) & apakah ke-4 gejala hilang; kalau masih ada
sisa gejala spesifik, kandidat lanjut: kalibrasi ulang threshold `h/2` atau delay
long-press-gesture.

## Batch 213 — Tambah drag-reorder ke PlaylistScreen (1 file kode + 1 dokumentasi)
Item terbuka Batch 211/212 dieksekusi atas permintaan eksplisit user. Porting logic drag
`QueueSheet.kt` ke `PlaylistScreen.kt` (detail playlist), 1:1 pola sama: drag-handle icon
(`Icons.Default.DragHandle`, 48dp touch target) di kiri tiap `PlaylistSongRow`, tahan-lama lalu
geser (`detectDragGesturesAfterLongPress`) reorder lagu; `graphicsLayer` translationY+
shadowElevation(10f)+zIndex saat row lagi di-drag; `rememberUpdatedState` cegah closure basi
(pola sama alasan komentar `QueueSheet.kt`); haptic feedback identik (LongPress saat mulai
drag/hapus, TextHandleMove tiap geser 1 slot). Beda teknis dari `QueueSheet.kt`: pakai `song.id`
langsung sbg identitas drag (bukan `slotIds` terpisah — playlist tidak reuse id sama 2x dalam 1
playlist, `itemsIndexed key` sudah pakai `song.id` juga), fungsi helper gesture dibuat DUPLIKAT
sendiri `pointerInputPlaylistDragHandle` (bukan diekstrak shared) karena masing-masing sudah
`private` ke file composable-nya sejak awal, konsisten pola existing. Tombol naik/turun TETAP
ada (bukan diganti) — fallback aksesibilitas persis alasan komentar `QueueSheet.kt`. Brace/paren
`PlaylistScreen.kt` seimbang (127/127, 206/206). 0 protected asset. **Belum diverifikasi
visual** — prioritas cek: drag antar-lagu playlist beneran reorder benar, divider (Batch 212)
tidak tumpang-tindih row yang di-drag.

## Batch 212 — Playlist/Queue item 8/8 (TERAKHIR): tambah divider antar baris QueueSheet (1 file kode + 1 dokumentasi)
Menutup checklist § Playlist/Queue. Item ini flagged sejak Batch 189 ("`QueueRow` 0 divider —
kandidat § Playlist/Queue nanti, kategori terpisah"). Konfirmasi: `QueueSheet.kt` (`itemsIndexed`
baris antrean) 0 `HorizontalDivider` sama sekali, sedangkan `PlaylistScreen.kt` (baris
lagu-dalam-playlist DAN daftar-playlist) sudah pakai `HorizontalDivider(color =
MaterialTheme.colorScheme.surfaceVariant)` setelah tiap item. Fix: divider identik ditambah
setelah tiap `QueueRow`, pola persis disalin dari `PlaylistScreen.kt` (warna surfaceVariant,
posisi unconditional setelah tiap item termasuk terakhir — sama seperti Library/Song List Batch
189). Brace/paren `QueueSheet.kt` seimbang (40/40, 126/126), `HorizontalDivider` sudah ter-cover
wildcard import `androidx.compose.material3.*` (0 import baru). 0 protected asset. **Belum
diverifikasi visual** — prioritas cek divider tidak tumpang-tindih row saat drag aktif
(`shadowElevation`/`translationY` row yang di-drag).

**§ Playlist/Queue checklist SEKARANG TUNTAS 8/8** (row height/touch-target/selected-state/
remove-affordance/destructive-confirm/empty-state/search-state(N/A,gap-reorder-dicatat)/divider).
**Item terbuka dari audit ini** (bukan bagian checklist, ditunda eksplisit): `PlaylistScreen.kt`
reorder cuma tombol naik/turun, 0 drag gesture (beda `QueueSheet.kt` yang sudah drag+tombol) —
kandidat batch terpisah kalau user eksplisit minta.

## Batch 211 — Playlist/Queue item 7/8: audit search-result state atau serupa (0 kode, 2 dokumentasi)
`PlaylistScreen.kt` + `QueueSheet.kt` dicek eksplisit: 0 fitur search/filter sama sekali di
kedua file (grep "search"/"Search" nihil) — beda dari § Library/Song List yang punya
`SearchResultsView` (sudah diaudit Batch 188, 0 bug). Jadi item literal "search-result state"
TIDAK APLIKATIF di § Playlist/Queue. Dicari "state serupa" sesuai catatan Batch 200: ditemukan
gap interaksi nyata — reorder lagu. `QueueSheet.kt` reorder pakai drag-handle (gesture,
`shadowElevation=10f`+`translationY`+`zIndex` saat drag) DAN tombol naik/turun (fallback
aksesibilitas). `PlaylistScreen.kt` (detail playlist) reorder HANYA tombol naik/turun
(`onMoveUp`/`onMoveDown`) — 0 drag gesture/handle sama sekali. **Bukan bug tersembunyi, gap
fitur nyata** tapi porting logic drag `QueueSheet` (~80 baris: offset state, pointerInput
custom, index math) ke `PlaylistScreen` levelnya "kerja lebih dalam", BUKAN micro-fix 1
file kecil — konsisten pola Batch 193/197 (item struktural besar ditunda, bukan dipaksa masuk 1
batch kecil, demi hindari half-baked/truncation risk). **Tidak dieksekusi batch ini.** 0 kode, 0
protected asset.

## Batch 210 — Widget compact: tambah prev/next (2 file kode)
Lanjutan Batch 209. `widget_player_compact.xml`: tambah `widget_prev`/`widget_next`
(28dp, lebih kecil dari full 34dp biar muat), diapit di kiri-kanan tombol play, margin 4dp.
`WidgetUpdater.kt`: binding prev/next dipindah keluar dari blok `if (!isCompact)` (jalan di
kedua layout); artist tetap eksklusif full (compact masih tanpa baris artis, cuma judul).
XML valid, brace/paren `WidgetUpdater.kt` seimbang (20/20, 118/118). 0 protected asset.

## Batch 209 — Widget: compact mode hilangkan judul total, bukan cuma truncate (2 file kode)
User laporan screenshot (widget dipaksa pendek/sempit): compact layout (`widget_player_compact.xml`)
memang dari awal SAMA SEKALI tanpa `TextView` judul — cuma album art + tombol play, gravity
`center`. `WidgetUpdater.kt` juga sengaja skip `setTextViewText(widget_title, ...)` saat
`isCompact`. Root cause: bukan bug truncation, tapi compact layout tidak pernah punya slot judul.
Fix: tambah `TextView` `@id/widget_title` (1 baris, `ellipsize="end"`, `layout_weight=1`) di
antara art & tombol play pada `widget_player_compact.xml`, ganti `gravity="center"` →
`"center_vertical"` biar title bisa isi ruang sisa alih-alih dipaksa ke tengah. `WidgetUpdater.kt`:
binding `widget_title` + click-to-open dipindah keluar dari blok `if (!isCompact)` (jalan di kedua
layout); artist + prev/next tetap eksklusif full layout (compact sengaja tanpa itu). XML valid,
brace/paren `WidgetUpdater.kt` seimbang (20/20, 118/118). 0 protected asset.

## Batch 208 — Widget: kembalikan height-check compact-mode (BUKAN SizeF), tetap truncated 1-baris (1 file kode)
User laporan screenshot: widget diperkecil jadi 1-baris, judul "Music M..." + artis "Forever Y"
kepotong vertikal. Batch 207 (minResizeWidth/Height + border, XML metadata) tidak cukup —
metadata provider tidak berlaku retroaktif ke widget yang sudah terpasang, dan sebagian launcher
tetap longgar soal itu.

**Analisis ulang penyebab crash Batch 201-204** (supaya tidak asal takut ulangi semuanya):
height-check ITU SENDIRI (Batch 201, dikoreksi Batch 202) TIDAK PERNAH menyebabkan crash — cuma
salah kalibrasi angka (90dp > minHeight 80dp declared). Crash "Ketuk untuk memulihkan" baru
muncul SETELAH Batch 203 menambah `RemoteViews(Map<SizeF,...>)` (API 31+) + `setBoolean(...,
"setSelected", true)` — 2 hal itulah yang paling dicurigai (API kompleks/reflection, berpotensi
tidak stabil di sebagian launcher), BUKAN height-check biasa.

**Fix**: `COMPACT_HEIGHT_THRESHOLD_DP=70` + `isCompact = width<180 || height<70` dikembalikan
persis seperti Batch 202 (versi TERKOREKSI, bukan Batch 201's 90dp yang salah) — mekanisme
paling sederhana (baca 1 Int dari options, bandingkan, pilih 1 dari 2 `RemoteViews` statis).
TIDAK mengembalikan `SizeF` map ataupun `setBoolean setSelected` — keduanya tetap non-aktif
sesuai revert Batch 206.

1 file (`WidgetUpdater.kt`), 0 protected asset. Brace/paren seimbang (20/20, 119/119). **Belum
diverifikasi device** — kalau "Ketuk untuk memulihkan" muncul LAGI setelah batch ini, itu bukti
kuat height-check-lah penyebabnya (bukan SizeF/setSelected seperti dugaan), dan perlu direvert
lagi + minta logcat. Kalau TIDAK muncul tapi truncation MASIH terjadi, kemungkinan launcher user
tidak meng-update `OPTION_APPWIDGET_MIN_HEIGHT` secara akurat/live — butuh info lebih (merk
launcher/HP) buat diagnosis lanjut.

## Batch 207 — Widget: minResizeWidth/Height + border visual (3 file, XML-only, 0 logic runtime)
Permintaan user setelah revert total Batch 206: tambah insets & pembatas/border visual biar
tidak truncated lagi saat di-minimize/di-stretch — TANPA logic runtime baru (yang sebelumnya
bikin crash "Ketuk untuk memulihkan", sudah direvert Batch 206).

**2 fix, murni metadata/drawable XML, 0 risiko crash**:
1. **`widget_player_info.xml`** — `minResizeWidth`/`minResizeHeight` ditambahkan eksplisit
   (110dp/80dp, sama dengan `minWidth`/`minHeight`). Tanpa ini, screenshot user menunjukkan
   launcher yang dipakai TIDAK otomatis membatasi resize-handle ke ukuran minimum declared —
   floor resmi ini pembatas level-launcher, mencegah shrink-berlebih dari SUMBERNYA (di
   compliant launcher), bukan reaksi setelah kejadian.
2. **`widget_background.xml` + `widget_background_light.xml`** — border/stroke 1dp ditambah
   sebagai layer terakhir (putih 20% alpha di dark, hitam 15% di light) — batas visual widget
   selalu jelas, murni kosmetik (tidak pengaruh layout/insets internal).

**Insets internal** (padding 14dp full / 8dp compact, `widget_player.xml`/`widget_player_
compact.xml`) dicek ulang — SUDAH memadai sejak sebelumnya, TIDAK diubah (menghindari
scope-creep, fokus 2 fix di atas yang eksplisit diminta).

3 file (`widget_player_info.xml`/`widget_background.xml`/`widget_background_light.xml`), 0
protected asset, 0 file Kotlin disentuh (tidak ada risiko regresi crash Batch 201-206 lagi).
XML tervalidasi parse. **Batasan jujur**: `minResizeWidth/Height` HANYA efektif di launcher yang
patuh kontrak Android — kalau launcher user tetap mengizinkan resize di bawah itu (beberapa OEM
launcher longgar), truncation masih mungkin terjadi; ini pembatas terkuat yang tersedia lewat
XML murni tanpa balik ke logic runtime yang baru saja terbukti crash.

## Batch 206 — REVERT PENUH Batch 201-204 (widget "tahan banting") — fallback total ke widget normal (2 file kode)
User laporan via screenshot bertimestamp: widget render BENAR sesaat (12:39:57), lalu ~15 detik
kemudian (12:40:12) jatuh ke placeholder Android "Ketuk untuk memulihkan" — pola khas widget
provider MELEMPAR EXCEPTION/gagal update, bukan cuma soal layout/distorsi visual seperti diduga
sebelumnya. Sudah 4 iterasi coba (201 fitur baru → 202 hotfix threshold → 203 rewrite struktural
SizeF API → 204 fix gravity) dan situasinya "gak kunjung hilang", makin parah menurut user.

**Keputusan**: STOP coba-tebak lebih lanjut tanpa logcat. User minta eksplisit fallback total ke
kondisi SEBELUM Batch 201, tanpa kosmetik apapun. Dieksekusi persis begitu:

1. **`WidgetUpdater.kt`** — ditulis ulang PERSIS logic pra-Batch-201: `isCompact` balik width-
   only (`COMPACT_WIDTH_THRESHOLD_DP=180`, tidak ada height check), 1 `RemoteViews` per update
   (bukan `Map<SizeF,...>` API 31+), tidak ada `setBoolean(..., "setSelected", true)`.
2. **`widget_player.xml`** — root `gravity` balik `center_vertical` (bukan `center`), `widget_
   title` balik `ellipsize="end"` statis (bukan `marquee`+`singleLine`+`marqueeRepeatLimit`).

Brace/paren `WidgetUpdater.kt` seimbang (20/20, 114/114), XML tervalidasi parse. 2 file kode, 0
protected asset. **Kalau "Ketuk untuk memulihkan" MASIH muncul setelah revert ini** — itu bukti
kuat penyebabnya BUKAN dari perubahan widget Batch 201-204 sama sekali (karena baris kode
penyebabnya sudah tidak ada), kemungkinan ada di tempat lain (mis. `PlaybackService`/album-art
loading/launcher itu sendiri) — WAJIB minta logcat sebelum coba fix apapun lagi, bukan
menebak ulang.

## Batch 205 — Dokumentasi: abadikan kebijakan "prioritas mutakhir, bukan kompat OS lama" (2 dokumentasi, 0 kode)
Permintaan langsung user (permanen, berlaku semua sesi berikutnya) — konteks asal: widget "tahan
banting" (Batch 201-204), fallback threshold buat Android <12 sempat dibuat padahal user cuma
mau solusi PALING BENAR/modern.

Ditulis di 2 tempat `PROJECT_STATE.md` (pola sama Batch 157 — pinned summary supaya tidak
tenggelam + detail penuh): item 3 § "⚠️ ATURAN SESI AKTIF" (atas file, posisi tetap) + § baru
"Kebijakan: prioritas mutakhir, bukan kompatibilitas OS/dependency lama" (bawah).

**Inti kebijakan**: (1) API/dependency modern yang lebih bersih/robust struktural WAJIB
diutamakan meski butuh `minSdk`/versi lebih baru, jangan otomatis dihindari cuma karena device
lama; (2) jangan habiskan effort fallback compat OS lama yang rumit kalau ada opsi modern lebih
sederhana — cukup catat keterbatasannya; (3) `minSdk` sendiri (protected asset) TIDAK ikut
diubah oleh kebijakan ini — beda kelas risiko dari sekadar "fallback visual kurang optimal"
(device di bawah minSdk baru = tidak bisa install sama sekali) — sesi berikutnya boleh sarankan
naik `minSdk`, tapi wajib minta konfirmasi eksplisit dulu, bukan dieksekusi diam-diam; (4)
dependency versi `build.gradle.kts` prioritaskan stabil TERBARU, bukan versi lama tanpa alasan
konkret.

2 dokumentasi (`CHANGELOG.md`/`PROJECT_STATE.md`), 0 kode, 0 protected asset.

## Batch 204 — Fix widget: root full layout wajib center horizontal saat stretch (1 file kode)
User: tidak peduli OS<12 (Batch 203 sudah selesaikan itu, jangan diutak-atik lagi), fokus SEMUA
ukuran widget wajib center + 0 distorsi. Audit ulang `widget_player.xml` vs `widget_player_
compact.xml`: **1 gap nyata** — root `widget_player.xml` cuma `gravity="center_vertical"`
(compact sudah benar `gravity="center"` sejak awal). Kalau widget di-stretch LEBAR dan kolom
judul/artist (`layout_weight="1"`) tidak menyerap semua sisa ruang (mis. teks pendek + minWidth
row terpenuhi), baris konten nempel ke kiri, bukan center. Fix: `center_vertical` → `center`.

**Distorsi visual (scaleType)**: dicek ulang, TIDAK ada bug — album art sudah `centerCrop`
(crop, bukan stretch, aspect ratio selalu terjaga), tombol `fitCenter` (letterbox, bukan
stretch). Semua ukuran art/tombol FIXED dp (52dp/36dp dst, tidak relatif ke ukuran widget) jadi
tidak pernah gepeng di ukuran manapun.

**Live-refresh saat drag**: dicek ulang juga, sudah benar sejak Batch 35 —
`onAppWidgetOptionsChanged` (`PlayerWidgetProvider.kt`) sudah panggil `updateAllAsync` on setiap
event resize live, bukan cuma pas lepas jari — tidak ada "stale render" yang perlu di-snap-balik.

1 file (`widget_player.xml`), 0 protected asset, XML tervalidasi parse. **Belum diverifikasi
device** — prioritas cek: stretch widget lebar-pendek/sempit-tinggi kombinasi apapun, pastikan
konten selalu center, 0 nempel ke salah satu sisi.

## Batch 203 — Widget tahan-banting struktural: responsive RemoteViews API 31+ (1 file kode)
Permintaan user: widget WAJIB tahan banting di-stretch/minimize ekstrem tanpa distorsi visual —
bukan cuma "threshold yang lebih pas lagi" (sudah 2x salah tebak: Batch 201 kurang lebar cakupan,
Batch 202 threshold ketinggian). Root masalah sebenarnya: `OPTION_APPWIDGET_MIN_WIDTH/HEIGHT`
cuma snapshot OPSI TERAKHIR yang di-report sistem, bukan ukuran live selama drag — pendekatan
threshold manapun SELALU bisa salah tebak untuk kombinasi ukuran yang belum kepikiran.

**Fix struktural** (bukan tebak angka lagi): `RemoteViews(Map<SizeF, RemoteViews>)`, API resmi
Android 12+ (SDK 31) yang didesain persis buat kasus ini — OS sendiri yang pilih layout paling
pas berdasar ukuran NYATA widget saat itu, terus-menerus selama resize, dan DIJAMIN oleh
kontrak API-nya sendiri tidak akan pernah render RemoteViews yang butuh ruang lebih besar dari
yang tersedia. Hard-clip/distorsi jadi TIDAK MUNGKIN terjadi lagi di jalur ini secara struktural,
bukan cuma diperkecil kemungkinannya.

**Kode**: logic pembuatan `RemoteViews` (background, teks, warna, listener, dst) diekstrak ke
`buildViewsFor(...)` supaya identik dipakai di 2 entry map (`SizeF(110f,52f)` compact,
`SizeF(180f,80f)` full — angka SAMA dengan threshold Batch 202, biar titik breakpoint konsisten
antar-jalur). Android <12 (minSdk 23) TETAP pakai threshold Batch 202 sebagai fallback — API
`SizeF` map tidak tersedia di bawah API 31, itu batas platform yang tidak bisa dihindari.

1 file (`WidgetUpdater.kt`), 0 protected asset. Brace/paren seimbang (23/23, 139/139). **Jujur:
Android <12 masih pakai threshold (bukan jaminan 100%)** — tapi mayoritas device aktif per Agu
2026 sudah API 31+, jadi cakupan realnya besar. **Belum diverifikasi device** — prioritas cek:
resize widget ekstrem (lebar & tinggi, kombinasi apapun) di device Android 12+, pastikan tidak
ada lagi clipping/distorsi di titik manapun selama drag, bukan cuma di titik lepas jari.

## Batch 202 — HOTFIX regresi Batch 201: threshold compact-height ketinggian, kena widget default (1 file kode)
User lapor "makin parah" via screenshot — widget ukuran NORMAL (bukan di-resize paksa) mendadak
cuma nampilin art+play doang. Root cause: `COMPACT_HEIGHT_THRESHOLD_DP = 90` (Batch 201) LEBIH
TINGGI dari `minHeight="80dp"` yang app sendiri deklarasikan di `widget_player_info.xml` — jadi
widget baru dipasang / ukuran default (tinggi umum 1-baris ~80-110dp) OTOMATIS memenuhi kondisi
compact, bukan cuma yang benar-benar di-shrink paksa. Dampak nyaris universal, bukan edge case.

Fix: turunkan ke `COMPACT_HEIGHT_THRESHOLD_DP = 70` — di bawah `minHeight` 80dp (placement
default balik ke full layout), tetap di atas kebutuhan riil compact (~52dp) supaya kasus ASLI
Batch 201 (widget di-shrink sungguhan <70dp) tetap tertangkap.

1 file (`WidgetUpdater.kt`), 0 protected asset. Brace/paren seimbang (20/20, 126/126). Marquee
judul (fix lain Batch 201) TIDAK disentuh — tidak ada laporan/bukti itu bermasalah, cuma
threshold height yang salah hitung. **Belum diverifikasi device.** Kalau "Ketuk untuk
memulihkan" (screenshot user) masih muncul setelah fix ini, itu kemungkinan bukan lagi soal
compact-mode — kemungkinan besar itu prompt generik launcher/OS ("tap to restore widget stack")
yang butuh logcat buat didiagnosis lebih lanjut, bukan tebakan dari kode saja.

## Batch 201 — Fix widget: truncated saat diperkecil paksa (height-only shrink) + marquee judul lagu (2 file kode)
2 bug dilaporkan user via screenshot resize widget home-screen:

1. **Truncated saat layout dipaksa minimize** — root cause: `isCompact` (`WidgetUpdater.kt`)
   cuma cek `OPTION_APPWIDGET_MIN_WIDTH`, TIDAK PERNAH cek tinggi. Widget yang di-shrink cuma
   secara TINGGI (lebar tetap ≥180dp) tetap pakai `widget_player.xml` (butuh ~80dp: art 52dp +
   padding 14dp×2) — RemoteViews/AppWidgetHost clip overflow, tidak reflow, hasil PERSIS sesuai
   screenshot (title/artist/tombol terpotong di tepi bawah). Fix: tambah cek
   `OPTION_APPWIDGET_MIN_HEIGHT` juga, `isCompact` sekarang true kalau SALAH SATU dimensi di
   bawah threshold (`COMPACT_HEIGHT_THRESHOLD_DP = 90`, compact cuma butuh ~52dp).
2. **Judul lagu tidak auto-scroll** — `widget_title` (`widget_player.xml`) cuma `ellipsize=
   "end"` statis. Diganti `ellipsize="marquee"` + `marqueeRepeatLimit="marquee_forever"` +
   `singleLine="true"`. **Catatan teknis penting**: marquee XML doang TIDAK cukup di widget —
   TextView marquee normal butuh Android focus buat jalan, widget host view TIDAK PERNAH
   focusable. Fix aslinya di `WidgetUpdater.kt`: `views.setBoolean(R.id.widget_title,
   "setSelected", true)` — trik terdokumentasi, marquee juga jalan kalau `isSelected=true`,
   lepas dari focus. Fallback: kalau ada launcher/OS version yang tetap tidak menjalankan trik
   ini, tampilan jatuh balik ke ellipsis statis (bukan crash/rusak).

2 file kode (`widget_player.xml` + `WidgetUpdater.kt`), 0 protected asset. Brace/paren
`WidgetUpdater.kt` seimbang (20/20, 122/122). **Belum diverifikasi device sungguhan** — marquee
widget punya riwayat tidak konsisten antar-launcher (Samsung One UI/Pixel Launcher/dst bisa
beda perilaku) — prioritas cek: (a) resize widget cuma secara tinggi, pastikan compact muncul
sebelum konten kepotong; (b) judul lagu panjang, pastikan benar-benar scroll bukan diam.

## Batch 200 — Playlist/Queue item 6/8: audit empty queue/playlist state — 0 bug (2 dokumentasi, 0 kode)
3 titik empty-state diperiksa: `QueueSheet` (antrean kosong), `PlaylistScreen` (belum ada
playlist — dengan CTA `actionLabel`/`onAction` "Buat Playlist", & playlist kosong tanpa lagu).
**Hasil: 0 bug** — ketiganya sudah pakai komponen `EmptyState` yang sama, subtitle actionable
(kasih tahu cara isi, bukan cuma "kosong"), 1 di antaranya malah punya CTA button langsung.
Konsisten dengan pola app-wide (sama seperti temuan Library/Song List item 6/11, Batch 185-an).
0 file diedit. Item berikutnya (7/8): audit search-result state (kalau ada pencarian dalam
Playlist/Queue) atau state serupa — dicek batch depan.

## Batch 199 — SmartPlaylistTabView highlight lagu-sedang-diputar (2 file, tuntaskan pending Batch 198)
`SmartPlaylistTabView` (tab 6) — satu-satunya composable song-list yang belum ikut highlight
now-playing (`QueueSheet`/`PlaylistScreen`/tab 5 sudah). Fix: param baru `currentSongId: Long? =
null` diteruskan `LibraryScreen.kt` → `SmartPlaylistTabView`; `isPlaying`/`background` dihitung
inline per-item (styling disalin persis dari `PlaylistSongRow`/`QueueRow` — background primary
alpha 0.12f + teks bold primary), + 1 import `FontWeight` baru. Brace/paren kedua file seimbang.
2 file, 0 protected asset. Dengan ini SEMUA composable song-list (Queue/Playlist/SmartPlaylist)
konsisten highlight now-playing. **Belum diverifikasi visual**.

## Batch 198 — PlaylistScreen highlight lagu-sedang-diputar (2 file, 1 fitur diperluas)
Eksekusi observasi Batch 193 (disetujui user). `currentSongId` dialirkan: `LibraryScreen.kt`
(`PlaylistTabView` call site) → `PlaylistTabView` (param baru, default `null` — 0 risiko
pemanggil lain kalau ada) → `PlaylistSongRow` (param baru `isPlaying`, dihitung dari `song.id ==
currentSongId`). Styling **disalin persis dari `QueueRow`** (`QueueSheet.kt`, sudah ada sejak
awal) — background `primary.copy(alpha=0.12f)`, judul lagu bold+warna primary saat diputar.
Bukan desain baru, murni terapkan pola yang sudah established di komponen sejenis.

2 file kode diedit (`LibraryScreen.kt` — 1 baris param baru di call site; `PlaylistScreen.kt` —
signature `PlaylistTabView`+`PlaylistSongRow`, 3 import baru `background`/`Color`/`FontWeight`).
0 file baru, 0 protected asset, `FILE_MANIFEST.txt` tidak berubah (173/173). Brace/paren
seimbang (`LibraryScreen.kt` 336/336 724/724, `PlaylistScreen.kt` 108/108 166/166).

Dengan ini, 3 dari 3 titik "song list" yang teridentifikasi Batch 193 (`QueueSheet`,
`PlaylistScreen`) kini konsisten highlight lagu-sedang-diputar — `SmartPlaylistTabView` (tab 6)
BELUM dicek, kandidat audit terpisah kalau diminta (beda composable lagi, belum ditelusuri).
**Belum diverifikasi visual** — cek: putar lagu yang ada di playlist, buka tab Playlist, pastikan
baris lagu itu ter-highlight & tetap sinkron kalau ganti lagu tanpa keluar-masuk playlist.

## Batch 197 — Sweep-select tab Artist + Folder (1 file, 1 fitur diperluas — lanjutan Batch 196)
Lanjutan permintaan user: `GroupedListView` (dipakai tab "Artist"/`selectedTab==2` DAN tab
"Folder"/branch `else`) dulu render lagu-dalam-grup lewat `LazyColumn { itemsIndexed { SongRow }
}` manual — TIDAK lewat `SongListView`, jadi 0 kapabilitas selection sama sekali (beda akar
masalah dari Favorit/Batch 196 yang cuma param jatuh ke default; di sini komponennya memang
belum pernah dipasangi).

**Fix**: blok manual itu diganti manggil `SongListView(songs = groupSongs, ...)` — behavior
`onSongClick` **identik** (`SongListView` internal manggil `onSongClick(songs, index)`, dengan
`songs` = `groupSongs` yang dioper, sama persis dengan yang dulu manual `onSongClick(groupSongs,
index)`). 6 parameter baru ditambah ke signature `GroupedListView` (`onDeleteSong`/
`selectionMode`/`selectedIds`/`onToggleSelect`/`onEnterSelectionMode`/`onSweepSelectRange`,
semua ada default aman) lalu diteruskan ke `SongListView`. 2 titik pemanggil (`selectedTab==2`
Artist, `else` Folder) disamakan wiring-nya dengan tab Lagu/Favorit.

Selection state tetap top-level `LibraryScreen`, jadi otomatis persisten kalau user pilih lagu
di 1 grup lalu "< Kembali" ke daftar grup lalu masuk grup lain — sama seperti lintas-tab (Batch
196), bukan behavior baru yang perlu dipikirkan ulang.

Dengan ini sweep-select jalan di 4 dari 7 "tab": Lagu, Artist, Folder, Favorit. 1 file, 0 file
baru, 0 protected asset, `FILE_MANIFEST.txt` tidak berubah (173/173). Brace/paren seimbang
(336/336, 724/724). **Sisa 3 belum bisa**: Album (grid, paradigma beda — pilih album ≠ pilih
lagu individual, butuh desain UX baru bukan cuma wiring), Playlist & SmartPlaylist (composable
+ row type sendiri, `PlaylistSongRow` bukan `SongRow`, butuh kerja lebih dalam). **Belum
diverifikasi visual** — cek tab Artist & Folder: masuk grup, sweep beberapa lagu, keluar-masuk
grup lain, pastikan selection tidak berantakan/salah index.

## Batch 196 — Sweep-select tab Favorit (1 file, 1 fitur diperluas — bukan micro-UIUX audit)
**Di luar `MICRO_UIUX_AUDIT.md`** — permintaan user langsung: sweep-select (tekan-lama lalu
seret utk pilih banyak lagu sekaligus, `LibraryScreen.kt` sejak Batch 70/73) cuma jalan di tab
"Lagu" (`selectedTab == 0`). Tab "Favorit" (`selectedTab == 4`) pakai `SongListView` YANG SAMA
persis, tapi manggil dengan argumen positional pendek — `selectionMode`/`selectedIds`/
`onToggleSelect`/`onEnterSelectionMode`/`onSweepSelectRange`/`onDeleteSong` semua jatuh ke
default composable (`false`/`{}`), jadi selection mode terkunci mati total di tab itu meski
komponennya identik.

**Fix**: panggilan `SongListView` di tab Favorit disamakan persis dengan tab Lagu — semua
parameter selection-mode diisi (state `selectionMode`/`selectedIds` di level `LibraryScreen`
sudah top-level, BUKAN per-tab, jadi otomatis persisten kalau user pilih beberapa lagu lalu
pindah tab). Dicek juga `SelectionActionBar` (bulk add-to-playlist/hide/delete) — sudah generik
dari awal, filter `rawSongs` pakai `selectedIds` doang, 0 referensi `selectedTab` — jadi 100%
aman dipakai lintas tab tanpa sentuh logic bulk-action sama sekali.

1 file, 0 file baru, 0 protected asset, `FILE_MANIFEST.txt` tidak berubah (173/173). Brace/paren
seimbang (335/335, 722/722). **Sisa 4 tab lain BELUM bisa** (dicek, bukan diabaikan):
`AlbumGridView` (tab 1) & `GroupedListView` (tab 2) TIDAK PAKAI `SongListView` sama sekali —
implementasi grid/grouped terpisah, 0 selection mode built-in — butuh kerja lebih besar
(bangun selection UI baru), bukan sekadar wiring param. `PlaylistTabView`/`SmartPlaylistTabView`
(tab 5/6) juga composable terpisah dgn row-nya sendiri. **Belum diverifikasi visual** — cek tap
& sweep di tab Favorit beneran masuk selection mode, lalu pindah ke tab Lagu, pastikan selection
tetap ada (bukan cuma teori dari baca kode).

## Batch 195 — Playlist/Queue item 5/8: konfirmasi hapus playlist + warna error (1 file, 1 bug fix nyata)
`PlaylistScreen.kt` — tombol "Hapus playlist" (di header detail playlist) **langsung eksekusi
tanpa konfirmasi apa pun** saat disentuh, beda dari "Ganti nama" yang benar buka dialog dulu.
Dibandingkan dgn pola destructive-confirm yang SUDAH established di `LibraryScreen.kt` ("Hapus
dari Perangkat?" — `AlertDialog` + tombol "Hapus" warna error + "Batal") — playlist tidak
mengikuti pola itu sama sekali. Menghapus playlist itu ireversibel dari sisi urutan/kurasi
(walau lagu-lagunya tetap ada di library), jadi harusnya setara level "destructive" dengan hapus
lagu, bukan langsung tereksekusi 1 sentuhan.

**Fix**: `showDeleteConfirm` state ditambah, ikon `DeleteOutline` diberi `tint = error` (dulu
default/tidak diberi warna — beda dari konvensi error-color-utk-permanen yang dikonfirmasi
Batch 194), `onClick` cuma buka dialog. `AlertDialog` baru ditambah, isi & struktur MENIRU
persis pola `LibraryScreen.kt` (judul tanya, teks jelaskan konsekuensi + tegaskan lagu TIDAK
ikut terhapus dari perangkat, tombol "Hapus" error + "Batal" netral).

1 file, 0 file baru, 0 protected asset, `FILE_MANIFEST.txt` tidak berubah (173/173). Brace/paren
seimbang (108/108, 161/161). **Belum diverifikasi visual** — dialog baru, cek tampilan +
alur tap "Hapus" di dialog beneran memanggil `onDeletePlaylist` & balik ke daftar playlist.

## Batch 194 — Playlist/Queue item 4/8: audit remove/delete affordance — 0 bug (3 dokumentasi, 0 kode)
`QueueSheet.kt` & `PlaylistScreen.kt` row remove button diperiksa dari 4 sisi:

- **Ikon & content description**: `Icons.Default.Close` + deskripsi jelas ("Hapus dari
  antrean"/"Hapus dari playlist") — konsisten di keduanya.
- **Warna**: keduanya `secondary` (netral), BUKAN `colorScheme.error`. Dibandingkan dengan
  aksi hapus permanen app-wide (`LibraryScreen.kt` "Hapus dari Perangkat" `DeleteForever`+error,
  `FolderManagerSheet.kt` dialog hapus+error) — app punya konvensi jelas: `error` khusus aksi
  **permanen/tidak bisa dibalik** (hapus file dari perangkat). Hapus dari antrean/playlist itu
  reversibel (lagu tetap ada di library, tinggal ditambah lagi) — warna netral disini justru
  **benar sesuai konvensi**, bukan gap.
- **Touch target**: `IconButton` default 48dp di keduanya. ✅
- **Enabled/disabled logic**: `QueueSheet` `canRemove = queue.size > 1` (cegah antrean kosong
  saat sedang ada lagu diputar — batasan logic pemutaran yang valid, didimkan jelas 0.3f alpha
  saat nonaktif). `PlaylistScreen` tidak ada batasan begini — **benar, bukan gap**, playlist
  boleh dikosongkan total (tidak terikat status "sedang diputar" seperti antrean aktif).

**Hasil: 0 bug** — pola sama Batch 145/160/162/190 (audit formal, genuinely konsisten & sesuai
konvensi yang sudah ada). 0 file kode diedit, 0 protected asset. `FILE_MANIFEST.txt` tidak
berubah (173/173).

## Batch 193 — Playlist/Queue item 3/8: audit selected/current item state — 1 observasi (bukan bug), butuh keputusan user (2 dokumentasi, 0 kode)
`QueueSheet.kt`'s `QueueRow` sudah highlight lagu sedang diputar (background
`primary.copy(alpha=0.12f)` + teks bold warna primary) — checklist item ini terpenuhi di situ.

**Ditemukan**: `PlaylistScreen.kt`'s `PlaylistTabView`/`PlaylistSongRow` **TIDAK PUNYA highlight
lagu-sedang-diputar sama sekali** — 0 referensi `isPlaying`/`isCurrent`/`currentSong` di file
ini. Beda dari `EmptyState` icon (Batch 163, murni default parameter, 0 risiko) — ini **bukan**
styling yang tinggal dipasang: `PlaylistTabView` memang tidak menerima parameter ID lagu yang
sedang diputar sama sekali, jadi memperbaikinya butuh mengalirkan state baru dari pemanggil
(`LibraryScreen.kt`) turun ke `PlaylistTabView` → `PlaylistSongRow`, nyentuh minimal 2 file +
nambah parameter baru ke beberapa lapis composable. Itu **plumbing data baru**, bukan pure
presentation fix, dan condong ke wilayah "tambah kapabilitas" — mirip kasus `EmptyState` icon
tapi levelnya lebih dalam (state lintas-file, bukan cuma default param 1 file). **SENGAJA tidak
dieksekusi** — checklist kategori ini eksplisit "Jangan mengubah queue behavior", dan
`MICRO_UIUX_AUDIT.md` scope guard melarang sentuh behavior/state plumbing baru tanpa keputusan
eksplisit user dulu.

0 file kode diedit, 0 protected asset. `FILE_MANIFEST.txt` tidak berubah (173/173).

## Batch 192 — Playlist/Queue item 2/8: drag handle touch target 40dp→48dp (1 file, 1 bug fix)
`QueueRow` (`QueueSheet.kt`) drag handle — `Box` pembungkus ikon `DragHandle` + gesture nyata
(`pointerInputDragHandle`, `detectDragGesturesAfterLongPress`) — ukuran 40dp, di bawah standar
touch target Material 48dp yang dipakai konsisten di 3 `IconButton` lain di baris yang sama
(naik/turun/hapus, default Material 48dp). Disamakan ke 48dp — handle jadi lebih mudah digenggam
tanpa geser layout lain (elemen pertama, `Column` berikutnya pakai `weight(1f)` jadi otomatis
menyerap selisih 8dp). Ikon `DragHandle` sendiri, `contentDescription` ("Tahan lalu geser untuk
mengurutkan ulang"), dan gesture logic tidak diubah — murni ukuran touch target.

**Dicek juga (bukan bug)**: `PlaylistScreen.kt` tidak punya drag handle sama sekali, cuma tombol
naik/turun — bukan gap, karena playlist itu memang belum punya mekanisme drag (`dragHandleModifier`
tidak ada di signature-nya). Item checklist "jika tersedia" — untuk `PlaylistScreen` tidak
tersedia, jadi tidak ada yang diaudit di sana. Menambah drag-and-drop baru ke `PlaylistScreen`
akan jadi perubahan behavior baru, di luar scope "perbaiki yang ada" & melanggar item checklist
terakhir kategori ini ("Jangan mengubah queue behavior").

Brace/paren `QueueSheet.kt` seimbang (40/40, 125/125). 1 file, 0 protected asset.
`FILE_MANIFEST.txt` tidak berubah (173/173). **Belum diverifikasi visual di device.**

## Batch 191 — Playlist/Queue item 1/8: konsistenkan row height dan spacing (1 file, 1 bug fix)
Kategori baru setelah Library/Song List tuntas 11/11 (Batch 190). Item 1/8: dibandingkan
`QueueRow` (`QueueSheet.kt`) vs `PlaylistSongRow` (`PlaylistScreen.kt`) — dua row paling mirip
secara fungsi (sama-sama song row dgn moveUp/moveDown/remove untuk reorder dalam list).

**Gap ditemukan**: `QueueRow` `.padding(horizontal = 12.dp, vertical = 8.dp)` — vertical sudah
match `PlaylistSongRow` (8dp), tapi horizontal 12dp adalah OUTLIER dari konvensi 20dp yang
dipakai konsisten di SELURUH app (`PlaylistSongRow`, `SongRow` `LibraryScreen.kt`, `ShimmerRow`,
`LibrarySearchField`, dst — semua `horizontal = 20.dp`). Kemungkinan sengaja lebih sempit dulu
supaya drag handle (40dp, elemen pertama di row) lebih dekat ke tepi layar buat digenggam, tapi
tidak ada precedent lain di app yang mengurangi padding demi alasan serupa.

**Fix**: horizontal padding `QueueRow` disamakan ke `20.dp`. Tinggi efektif row (ditentukan
tinggi konten 2-baris title/artist + padding vertical, sudah sama di kedua row) TIDAK berubah —
murni horizontal padding.

1 file diedit, 0 file baru, 0 protected asset. Brace/paren `QueueSheet.kt` seimbang (40/40 `{}`,
125/125 `()`). `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (2/8): pastikan
drag/reorder affordance jelas. **Belum diverifikasi visual di device** — cek drag handle
`QueueRow` masih cukup mudah digenggam dari tepi setelah padding lebih lebar 8dp.

## Batch 190 — Library/Song List item 11/11 (TERAKHIR): visual jumping artwork, kategori TUNTAS 11/11 (0 kode)
Item 11/11 § Library/Song List — item terakhir kategori ini. Diperiksa `AlbumArt` (`Utils.kt`),
1 komponen shared dipakai di SEMUA tempat art muncul (Home, Library, MiniPlayerBar, Now
Playing).

**Hasil: 0 bug.** Komponen ini sudah didesain anti-jump SEJAK AWAL (dikonfirmasi KDoc comment
existing di atas fungsinya): `Box` ukurannya selalu ditentukan `modifier` milik CALLER (fixed
dp seperti 48dp/56dp, atau `aspectRatio(1f)` — bukan pernah derive dari ukuran natural gambar),
langsung diisi `background(surfaceVariant)` sebagai placeholder solid sebelum gambar sempat
tiba, lalu `SubcomposeAsyncImage` dengan `matchParentSize()` (mengisi bounds Box yang SUDAH
fixed, tidak pernah mengubahnya) + `loading = {}` (sengaja kosong — supaya lagu yang PUNYA art
tidak sempat kelip ikon fallback dulu sebelum Coil selesai decode). Layout box tidak pernah
berubah ukuran dari sebelum→sesudah gambar selesai decode — jump tidak mungkin terjadi
by-construction, bukan kebetulan.

0 file diedit. `FILE_MANIFEST.txt` tidak berubah (173/173).

## 🏁 KATEGORI "LIBRARY / SONG LIST" RESMI TUNTAS 11/11
Ringkasan Batch 180-190: 2 bug fix (Batch 183 title marquee `QueueRow`, Batch 187 padding
`ShimmerRow`), 9 audit hasil bersih (tinggi row, thumbnail size, spacing metadata, hit target
icon, indikator sedang-diputar, empty state, search result state, list divider, visual jumping
artwork). 1 catatan dicatat untuk kategori berikutnya (bukan bug kategori ini): `QueueRow` 0
divider — kandidat § Playlist/Queue.

**Kategori berikutnya: PLAYLIST / QUEUE** (`MICRO_UIUX_AUDIT.md` § "🟠 PLAYLIST / QUEUE", 8
item, dimulai dari "Konsistenkan row height dan spacing"). ⚠️ Item pertama kategori itu
kemungkinan besar akan langsung menemukan catatan divider `QueueRow` di atas — bukan
penemuan baru, sudah diketahui dari batch ini.

## Batch 189 — Library/Song List item 10/11: audit list separator/divider (0 kode)
Item 10/11 § Library/Song List. Grep semua `HorizontalDivider` di `LibraryScreen.kt` — 5 titik:
`SongListView` (tab Lagu/Favorit), `GroupedListView` daftar grup (Artis/Folder), `GroupedListView`
daftar lagu dalam grup, `SearchResultsView`, drill-down Album di `AlbumGridView`.

**Hasil: 0 bug.** Kelimanya `HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)`
identik — warna sama, ketebalan default sama, posisi sama (setelah SETIAP item termasuk item
terakhir, tidak ada special-case skip-last yang beda-beda). 1 usage `HorizontalDivider` lain
(dalam `DropdownMenu` overflow SongRow, pemisah sebelum "Hapus dari Perangkat") sengaja tidak
dihitung — itu separator menu, bukan separator antar-row list.

**Dicatat, sengaja tidak difix di sini**: `QueueRow` (`QueueSheet.kt`) 0 divider sama sekali —
di luar cakupan kategori Library/Song List, `QueueSheet` masuk § Playlist/Queue
(`MICRO_UIUX_AUDIT.md`, belum diaudit, kategori terpisah setelah kategori ini tuntas).

0 file diedit. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (11/11, TERAKHIR):
hindari visual jumping saat artwork selesai loading.

## Batch 188 — Library/Song List item 9/11: audit search result state (0 kode)
Item 9/11 § Library/Song List. Diperiksa `SearchResultsView` (`LibraryScreen.kt`).

**Hasil: 0 bug.** 1 komponen menangani ketiga kondisi pencarian: (1) hasil kosong — reuse
`EmptyState` yang sama persis dengan skenario kosong lain (sudah diaudit & dikonfirmasi
konsisten di item 7, Batch 186), (2) hasil ada — dikelompokkan 3 section (Artis/Album/Lagu),
tiap section pakai `SearchSectionLabel` yang sama (typography `titleSmall` + warna secondary +
padding identik di ketiganya), (3) pencarian murni filter sinkron `remember(songs, query)` atas
list in-memory — tidak ada request async/network, jadi tidak ada state loading terpisah yang
perlu ditangani (beda dari kategori lain yang punya latency nyata).

0 file diedit. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (10/11): audit list
separator/divider.

## Batch 187 — Library/Song List item 8/11: audit loading state (1 file, 1 bug fix)
Item 8/11 § Library/Song List. `ShimmerRow`/`ShimmerList` (skeleton saat `loading=true`,
`LibraryScreen.kt`) diperiksa terhadap `SongRow` sungguhan yang digantikannya begitu data siap.

**Gap ditemukan**: art skeleton 48dp sudah match `SongRow` asli, TAPI `padding(vertical =
10.dp)` shimmer vs `padding(vertical = 8.dp)` `SongRow` asli — beda 2dp per baris. `ShimmerList`
render 8 baris (`repeat(8)`), jadi total tinggi skeleton 16dp lebih tinggi dari 8 baris
`SongRow` sungguhan yang menggantikannya — begitu loading selesai, list asli "melompat" naik
16dp dibanding posisi shimmer terakhir sebelum konten nyata muncul.

**Fix**: `vertical` padding `ShimmerRow` disamakan ke `8.dp`, match persis `SongRow`.

1 file diedit, 0 file baru, 0 protected asset. Brace/paren `LibraryScreen.kt` seimbang (332/332
`{}`, 719/719 `()`). `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (9/11): audit
search result state. **Belum diverifikasi visual di device** — cek transisi loading→loaded tab
Lagu genuinely tanpa jump/shift lagi.

## Batch 186 — Library/Song List item 7/11: audit empty library state (0 kode)
Item 7/11 § Library/Song List. Diperiksa semua skenario "0 lagu" di `LibraryScreen.kt`.

**Hasil: 0 bug.** 1 composable shared `EmptyState` (icon `MusicNote` + title + subtitle + CTA
opsional) dipakai ulang di 4 titik: (1) `songs.isEmpty()` — perpustakaan kosong total, satu-
satunya yang dapat CTA "Pindai Ulang" (genuinely actionable, panggil `onRescan`), (2) tab
Favorit kosong — hint "Ketuk ikon hati ... untuk menambahkannya", tanpa CTA (tidak ada aksi
langsung yang masuk akal selain nge-tap hati di lagu lain), (3) `filteredSongs.isEmpty()` — hasil
filter tab Album/Artis/Folder kosong (jarang terjadi tapi dijaga), (4) hasil Pencarian kosong —
"Tidak ditemukan"/"Coba kata kunci lain", keduanya tanpa CTA (sama alasan). Perbedaan CTA
antar skenario disengaja by-design, bukan inkonsistensi — komponen shared yang sama, cuma
parameter beda sesuai konteks.

0 file diedit. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (8/11): audit
loading state.

## Batch 185 — Library/Song List item 6/11: verifikasi retrospektif indikator sedang-diputar (0 kode)
Item 6/11 § Library/Song List — sesuai peringatan Batch 179, item ini SUDAH dikerjakan lebih
dulu (Batch 163/164, sebelum kategori Library/Song List resmi dimulai Batch 180), jadi batch ini
murni verifikasi ulang, bukan implementasi baru.

**Dikonfirmasi grep + baca kode langsung**: `SongRow` (`LibraryScreen.kt`) & `QueueRow`
(`QueueSheet.kt`) sama-sama 3 lapis identik — `.background(primary alpha 12%)` dipasang SEBELUM
`.clickable` (komentar Batch 163 eksplisit menyebut "samakan pola highlight ... dengan
QueueRow"), ikon `GraphicEq`, title bold+warna primary. `ContinueListeningCard` (Home) & `MiniPlayerBar`
sengaja TANPA lapisan tambahan — beda semantik: card itu nunjuk lagu TERAKHIR diputar (bukan
status live "sedang" diputar sekarang), mini bar itu SENDIRI sudah = indikator "sedang diputar"
(kalau nongol di layar berarti sedang aktif), jadi highlight duplikat di situ justru berlebihan.

0 file diedit. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (7/11): audit empty
library state.

## Batch 184 — Library/Song List item 5/11: audit hit target icon (0 kode)
Item 5/11 § Library/Song List. Diperiksa semua `IconButton`/`FilledIconButton` di 4 komponen
yang sama (favorit `SongRow`, moveUp/moveDown/remove `QueueRow`, play-pause `MiniPlayerBar` —
`ContinueListeningCard` 0 icon button, cuma card-tap biasa).

**Hasil: 0 bug.** Yang pakai ukuran default (`SongRow`/`QueueRow`, tanpa `.size()` eksplisit)
otomatis 48dp — memenuhi minimum Material3. Yang eksplisit lebih kecil (`MiniPlayerBar`
play-pause, `.size(40.dp)`, sengaja sejak Batch 55 untuk footprint mini bar) TETAP aman: Material3
`IconButton`/`FilledIconButton` menegakkan touch target minimum 48dp lewat
`minimumInteractiveComponentSize()` internal — visual mengecil, area sentuh tidak. Dikonfirmasi
grep: 0 pemakaian `LocalMinimumInteractiveComponentEnforcement`/override apa pun di 4 file
terkait yang bisa mematikan proteksi otomatis itu.

0 file diedit. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (6/11): audit
selected/current-playing indicator.

## Batch 183 — Library/Song List item 4/11: audit title/artist truncation (1 file, 1 bug fix)
Item berikutnya dari `MICRO_UIUX_AUDIT.md` § Library/Song List. Diperiksa 4 komponen yang sama
seperti item 1-3 sebelumnya (`SongRow`, `QueueRow`, `ContinueListeningCard`, `MiniPlayerBar`).

**Gap ditemukan**: 3/4 komponen (`SongRow`, `ContinueListeningCard`, `MiniPlayerBar`) title-nya
pakai `Modifier.basicMarquee()` (scroll penuh kalau kepanjangan) — cuma `QueueRow`
(`QueueSheet.kt`) yang masih `overflow = TextOverflow.Ellipsis` diam. Artist di keempat
komponen konsisten sama-sama `TextOverflow.Ellipsis` (tidak diubah — memang bukan fokus
perhatian utama, cukup dipotong).

**Fix**: title `QueueRow` disamakan ke `Modifier.basicMarquee()`, `overflow = Ellipsis` dilepas
dari Text itu (redundan — marquee menggantikan truncation, pola sama persis 3 komponen lain).
1 import baru (`androidx.compose.foundation.basicMarquee`).

1 file diedit, 0 file baru, 0 protected asset. Brace/paren `QueueSheet.kt` seimbang (40/40 `{}`,
125/125 `()`). `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (5/11): hit target
favorite/overflow/action icon. **Belum diverifikasi visual di device** — cek title lagu panjang
di Queue sheet sekarang genuinely scroll marquee, bukan tetap terpotong.

## Batch 182 — Library/Song List item 3/11: audit spacing antar metadata (0 kode)
4 komponen song-metadata (`SongRow`, `QueueRow`, `ContinueListeningCard`, `MiniPlayerBar`)
diperiksa: pola 3-segmen spacing IDENTIK di semua — art↔text `Spacer(width=12dp)`, title↔artist
`0dp` eksplisit (0 `Spacer` di antara 2 `Text` dalam `Column`, mengandalkan line-height Text
bawaan, sama di ke-4 komponen — disengaja bukan lupa), text↔trailing-action `Spacer(width=8dp)`
(sebelum duration+favorite di `SongRow`, sebelum tombol play di `ContinueListeningCard`).

**Hasil: 0 bug.** `FILE_MANIFEST.txt` tidak berubah (173/173, diverifikasi diff eksplisit). Item
berikutnya (4/11): audit title/artist truncation.

## Batch 181 — Library/Song List item 2/11: audit thumbnail/artwork size (0 kode)
5 file pemakai `AlbumArt` (`Utils.kt`) diperiksa: `HomeScreen.kt` (2 titik), `LibraryScreen.kt`
(2 titik), `MiniPlayerBar.kt`, `NowPlayingScreen.kt` (2 titik, sudah diaudit Batch 177 dari sisi
loading/error). Ukuran mengelompok jadi 4 kategori peran UI, masing-masing konsisten internal:

- **List-row scrollable**: `SongRow` 48dp — 1 komponen shared 5 titik pemanggilan (Batch 180).
- **Persistent compact bar**: `MiniPlayerBar` 44dp — konteks tunggal (1 titik), budget tinggi
  bar terbatas (`Row padding 10dp` + art 44dp = tinggi total ~64dp, konvensi umum mini-player).
- **Featured single-card**: `ContinueListeningCard` (`HomeScreen.kt`) 56dp — `Surface` elevated
  dgn padding 14dp sendiri, konteks tunggal, BUKAN list-row biasa (beda `Modifier` chain total:
  tonalElevation/embossed shape/clickable card, bukan `Row` polos kayak `SongRow`).
- **Carousel/grid fill-container**: `HomeSongCard` (`HomeScreen.kt`) `.width(120.dp)` tetap +
  art `.size(120.dp)` (art = lebar card persis), `AlbumGridView` (`LibraryScreen.kt`)
  `fillMaxWidth()+aspectRatio(1f)` (art = lebar kolom grid persis) — PRINSIP sama ("art
  mengisi penuh lebar container-nya"), angka absolut beda krn lebar carousel-item (120dp
  tetap) vs lebar kolom grid (responsif jumlah kolom/lebar layar) memang 2 hal berbeda by
  design, bukan dibandingkan apple-to-apple.

Perbedaan ukuran LINTAS kategori (48 vs 44 vs 56 vs 120) = peran UI genuinely berbeda
(list-row vs compact-bar vs featured-card vs carousel-item), bukan inkonsistensi DI DALAM
kategori yang sama — tiap kategori sendiri sudah 1 komponen shared atau 1 konteks tunggal,
tidak ada duplikasi/fragmentasi angka utk peran yang sama.

**Hasil: 0 bug.** `FILE_MANIFEST.txt` tidak berubah (173/173, diverifikasi diff eksplisit). Item
berikutnya (3/11): samakan spacing antar metadata (title/artist/duration dst).

## Batch 180 — Library/Song List item 1/11: audit tinggi row (0 kode)
Kategori baru setelah Now Playing tuntas (Batch 179). Item 1: `SongRow` (`LibraryScreen.kt`) —
art `.size(48.dp)` + `Row` padding `vertical = 8.dp` = tinggi baris konstan. 1 fungsi private
ini dipakai identik di 5 titik pemanggilan (tab Lagu, tab Favorit, drill-down grup Artis,
drill-down grup Folder, hasil Pencarian — semua lewat `SongListView`/`GroupedListView`/
`SearchResultsView` yang meneruskan ke `SongRow` yang sama, grep-confirmed sejak Batch 164),
jadi tinggi row konsisten by-construction, bukan kebetulan disamakan manual di 5 tempat.

2 elemen list LAIN sengaja diperiksa juga supaya tidak salah tandai sebagai gap: group-header
row (`ListItem` di `GroupedListView` sebelum drill-down) beda semantik — itu navigasi kategori
("Rock — 12 lagu"), bukan representasi 1 lagu, wajar beda tinggi/style dari `SongRow`. `Album
GridView` beda paradigma total (grid card, bukan row list) — juga bukan kandidat perbandingan
row-height, sudah dicek terpisah nanti di item "thumbnail/artwork size".

**Hasil: 0 bug.** `FILE_MANIFEST.txt` tidak berubah (173/173, diverifikasi diff eksplisit). Item
berikutnya (2/11): samakan thumbnail/artwork size.

## Batch 179 — Now Playing item 11/11: verifikasi retrospektif "jangan ubah playback logic" — kategori TUNTAS (0 kode)
Item terakhir kategori "Now Playing — Final Micro-Polish" (11/11) — bukan target audit baru,
melainkan verifikasi retrospektif atas seluruh Batch 169-178: apakah audit micro-polish 10 item
sebelumnya (alignment, spacing, slider, progress-readability, volume, bottom-sheet, selected-
state, layout-shift, artwork-state, feedback-visual) genuinely tidak menyentuh playback logic
sepanjang jalan, sesuai batas STRICT SCOPE yang tertulis di header `MICRO_UIUX_AUDIT.md`.

Dikonfirmasi lewat grep: `PlayerViewModel.kt` (dan seluruh package `playback/`) **0 baris
disentuh** sepanjang Batch 169-178. Satu-satunya perubahan kode nyata di kategori ini (Batch
178, `AlbumArtHero` swipe-feedback) diverifikasi ulang: `onSwipeNext()`/`onSwipePrevious()` di
dalam `detectHorizontalDragGestures` tetap memanggil `onNext`/`onPrevious` yang sama persis
seperti sebelum Batch 178 (call site `NowPlayingScreen(onSwipeNext = onNext, onSwipePrevious =
onPrevious)` tidak berubah), threshold 120px tidak diubah — `dragOffset`/`Animatable` yang
ditambahkan murni membaca `totalDrag` untuk visual, tidak pernah menulis balik ke logic apa pun.

**Hasil: terverifikasi bersih, 0 pelanggaran scope.** Kategori **"Now Playing — Final Micro-
Polish" resmi TUNTAS 11/11** — 2 bug fix (spacing controls Batch 170, swipe feedback visual
Batch 178), 9 audit hasil bersih. `FILE_MANIFEST.txt` tidak berubah (173/173, diverifikasi diff
eksplisit). Kategori berikutnya (`MICRO_UIUX_AUDIT.md` § "🟠 LIBRARY / SONG LIST"): samakan
tinggi row, thumbnail/artwork size, spacing metadata, truncation, hit-target
favorite/overflow/action, indikator lagu-sedang-diputar (⚠️ **sudah dikerjakan** — cek
`CHANGELOG.md` Batch 164 sebelum audit ulang, supaya tidak salah tandai sebagai gap baru), empty/
loading/search-result state, separator/divider, visual-jump saat artwork selesai loading.

## Batch 178 — Now Playing item 10/11: audit semua controls feedback visual — 1 bug fix (1 file)
Audit semua control interaktif Now Playing: transport buttons (`bouncyPress` + ripple, sudah
ada), shuffle/repeat (tint-toggle, Batch 175), star rating (icon swap+tint+haptic), gesture
brightness/volume (`GestureIndicatorBadge` live, sudah ada) — **1 gap ditemukan**: swipe album
art (next/prev, `AlbumArtHero`) 0 feedback visual SELAMA drag berlangsung, cuma haptic sekali di
`onDragEnd` kalau lolos threshold 120px. User tidak tahu sudah "cukup jauh" menggeser sampai
jarinya dilepas — asimetri nyata dibanding gesture brightness/volume tepat di sekitarnya yang
sudah live.

**`NowPlayingScreen.kt`** (diedit, 1 titik, `AlbumArtHero`) — `dragOffset: Animatable<Float>` +
`dragScope: CoroutineScope` baru. `onHorizontalDrag`: `dragOffset.snapTo((totalDrag *
0.5f).coerceIn(-48dp, 48dp))` — damped 0.5x (bukan 1:1) supaya tidak terkesan bisa diseret jauh
tak terbatas, clamp ±48dp jaga art tidak keluar terlalu jauh dari posisi visual wajar.
`onDragEnd`/`onDragCancel`: `dragOffset.animateTo(0f, spring(dampingRatio =
DampingRatioMediumBouncy))` — spring balik ke tengah, bukan snap instan, konsisten "tactile"
dgn animasi lain di file ini (`entranceScale`/`entranceAlpha` pola sama persis, `Animatable` +
`graphicsLayer`). Diterapkan lewat `.graphicsLayer { translationX = dragOffset.value }` di outer
`Box` yang sama (membungkus glow blur + art, keduanya ikut bergeser sebagai satu unit).

**PENTING — logic swipe TIDAK diubah**: `totalDrag` (dipakai buat threshold 120px &
`onSwipeNext()`/`onSwipePrevious()`) sama sekali tidak disentuh, cuma dibaca (bukan direplace)
utk turunkan `dragOffset`. `dragOffset` murni layer visual tambahan di atas logic yang sudah
ada — 0 perubahan kapan/kenapa skip terpicu, cuma menambah "apakah user BISA MELIHAT progress
gesture-nya", sesuai batas STRICT SCOPE audit ini (presentation/UI/UX polish only).

1 file, 0 protected asset. Brace/paren `NowPlayingScreen.kt` (215/215, 750/750) seimbang.
`FILE_MANIFEST.txt` tidak berubah (173/173, diverifikasi diff eksplisit). **Belum diverifikasi
visual di device** — cek: (1) swipe album art pelan-pelan, pastikan art genuinely ikut
bergeser mengikuti jari sebelum threshold, bukan diam sampai tiba-tiba skip, (2) lepas jari
sebelum threshold, pastikan spring balik ke tengah mulus (bukan snap kasar/nyangkut di posisi
offset), (3) swipe cepat lewat threshold, pastikan skip tetap terjadi PERSIS seperti sebelumnya
(behavior threshold benar-benar tidak berubah), (4) clamp ±48dp tidak bikin art terlihat
"nabrak" batas secara janggal di jari yang menggeser sangat jauh. Item terakhir (11/11): jangan
mengubah playback logic (verifikasi retrospektif, bukan gap baru).

## Batch 177 — Now Playing item 9/11: audit artwork loading/error/empty state (0 kode)
`AlbumArtHero` (Now Playing) pakai `AlbumArt` (`Utils.kt`) — komponen shared yang SAMA persis
dipakai 7 titik app-wide (`HomeScreen.kt` x2, `LibraryScreen.kt` x2, `MiniPlayerBar.kt`,
`NowPlayingScreen.kt` x2 termasuk hero art & mini-progress). Konsisten by-construction: 1 sumber
kode, bukan reimplementasi per-layar yang kebetulan mirip.

3 state: `loading = {}` (blank, cuma `background(surfaceVariant)` polos dari Box pembungkus),
`error = { AlbumArtFallbackIcon() }`, dan artwork `null` juga render `AlbumArtFallbackIcon()`
yang SAMA (ikon `MusicNote` 40% ukuran, `onSurfaceVariant` alpha 0.5f) — error & empty state
sengaja disamakan (keduanya secara semantik = "tidak ada visual art untuk ditampilkan").

Loading blank (bukan shimmer) dinilai wajar, bukan gap: app ini 100% offline (README) — artwork
selalu dari URI lokal (embedded ID3/MediaStore), dekode nyaris instan, beda karakteristik dari
network image loading yang butuh shimmer utk state yang genuinely terlihat lama. Shimmer di
sini kemungkinan cuma flicker sub-frame, bukan memperbaiki persepsi.

**Hasil: 0 bug.** `FILE_MANIFEST.txt` tidak berubah (173/173, diverifikasi diff eksplisit). Item
berikutnya (10/11): semua controls punya feedback visual.

## Batch 176 — Now Playing item 8/11: audit long title/artist layout shift (0 kode)
`song?.title` (`titleLarge`, `basicMarquee()`) & `song?.artist` (`bodyMedium`,
`TextOverflow.Ellipsis`) di `NowPlayingScreen.kt` — keduanya sudah `maxLines = 1`. Tinggi 1
baris teks Compose ditentukan line-height style, KONSTAN terlepas panjang konten; `basicMarquee()`
scroll horizontal di dalam box tetap (tidak wrap/growing), `Ellipsis` clip bukan reflow. Parent
`Column` root sudah `fillMaxSize().padding(20.dp)` (lebar tetap, bukan `wrapContentWidth`), jadi
Text tidak bisa memaksa parent melebar. Sibling elemen (`Spacer` 6dp/24dp dst) semua fixed-dp,
tidak reaktif terhadap panjang teks.

**Hasil: 0 bug.** Struktur sudah anti-layout-shift by construction, bukan kandidat perubahan.
`FILE_MANIFEST.txt` tidak berubah (173/173, diverifikasi diff eksplisit). Item berikutnya
(9/11): artwork loading/error/empty state konsisten.

## Batch 175 — Now Playing item 7/11: audit selected/repeat/shuffle states (0 kode)
Tombol Acak (`Shuffle`) & Ulangi (`Repeat`/`RepeatOne`) di `NowPlayingScreen.kt` diperiksa:
keduanya pakai `tint = if (aktif) animatedAccent else colorScheme.secondary` — pola tint-toggle
identik dgn favorite-icon (`LibraryScreen.kt` line 1214: `primary`/`secondary`) & rating-star
(`SmartPlaylistScreen.kt` line 376: `primary`/`secondary`) di tempat lain app. `animatedAccent`
= aksen dinamis per-lagu, berperan setara `primary` di layar ini (bukan warna acak lepas).
Repeat dapat pembeda tambahan: icon glyph ganti `Repeat`→`RepeatOne` saat mode "ulangi 1 lagu",
di atas tint-toggle yang sama.

**Hasil: 0 bug.** Pola sudah konsisten app-wide, bukan kandidat perubahan. `FILE_MANIFEST.txt`
tidak berubah (173/173, diverifikasi diff eksplisit). Item berikutnya (8/11): long title/artist
tidak menyebabkan layout shift.

## Batch 174 — Now Playing item 6/11: audit bottom sheet/modal transition (0 kode)
Item 6. Sheet/dialog dari Now Playing: 1 `ModalBottomSheet` langsung di `NowPlayingScreen.kt`
("Kontrol Lanjutan") + 2 `AlertDialog` (`SleepTimerDialog`, `SpeedDialog` — sudah diaudit
konsistensi opsi-pilihnya sendiri di Batch 163). Ditelusuri juga 4 sheet lain yang dibuka lewat
callback dari Kontrol Lanjutan (dirender di level `MainActivity.kt`, file terpisah):
`EqualizerSheet`, `VisualizerSheet`, `SongInfoEditSheet`, `RingtoneCutterSheet`.

**Hasil: 0 bug** — SEMUA 5 `ModalBottomSheet` (Kontrol Lanjutan + 4 sheet lain) pakai
`rememberModalBottomSheetState(skipPartiallyExpanded = true)` + `containerColor =
Color.Transparent` identik persis, tidak ada 1 pun yang custom animationSpec/transition sendiri
— transisi buka/tutup 100% konsisten framework-default di semua titik yang bisa dijangkau dari
Now Playing.

0 kode, 3 dokumentasi. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (7/11):
selected/repeat/shuffle states mudah dibedakan.

## Batch 173 — Now Playing item 5/11: audit volume/secondary controls (0 kode)
Item 5. 2 kontrol diperiksa: (a) `Slider` "Peredam Dalam Aplikasi" (volume internal, bottom
sheet Kontrol Lanjutan) — Material3 `Slider` standar, icon 3-tingkat (VolumeOff/Down/Up)
sesuai level, warna `secondary` untuk thumb/track aktif; (b) 2 zona gesture swipe brightness
(kiri)/volume sistem HP (kanan) di area artwork, masing-masing 50% lebar x 300dp tinggi
(touch target generous, sudah dikomentari sengaja sejak awal) + `GestureIndicatorBadge` yang
muncul saat drag.

**1 asimetri ditemukan, TAPI sudah disengaja & terdokumentasi**: badge volume-sistem-HP punya
teks label "Volume HP" tambahan, badge brightness tidak — ternyata ini FIX yang sudah pernah
dibuat di batch lampau (komentar eksplisit di kode: label ditambahkan khusus di badge volume
untuk membedakan gesture-volume-sistem ini dari slider "Peredam Dalam Aplikasi" yang terpisah,
brightness tidak butuh disambiguasi serupa karena tidak ada slider-brightness lain di layar
yang sama). **Hasil: 0 bug baru** — icon 3-tingkat konsisten antara slider peredam & badge
volume sistem, asimetri label sudah tepat by-design.

0 kode, 3 dokumentasi. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (6/11):
audit bottom sheet/modal transition.

## Batch 172 — Now Playing item 4/11: progress/current/remaining time mudah dibaca (0 kode)
Item 4. Baris waktu (`formatDuration(position)` kiri, total/sisa kanan) di `NowPlayingScreen.kt`
~baris 531: `Row` `SpaceBetween`, kedua `Text` `bodySmall` + `colorScheme.secondary` —
treatment umum player (timestamp memang selalu di-de-emphasize, bukan warna teks utama) dan
ukuran `bodySmall` standar untuk label pendek semacam ini. Mode Audiobook (Roadmap #12, Batch
93) sudah punya varian "-mm:ss" tersendiri yang konsisten style-nya (font/warna sama, cuma
teksnya beda format). **Hasil: 0 bug** — kontras & ukuran sudah wajar, tidak ada perubahan.

0 kode, 3 dokumentasi. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (5/11):
audit volume/secondary controls.

## Batch 171 — Now Playing item 3/11: audit slider height/touch area (0 kode)
Item 3: "Audit slider height/touch area." Progress slider `NowPlayingScreen.kt` (~baris 508):
input sentuh sesungguhnya adalah `Slider` Material3 standar (thumb+track dibuat transparan via
`SliderDefaults.colors`), ditumpuk DI ATAS `WaveformSeekBar` custom yang murni visual (bukan
penerima sentuhan) — dibungkus `Box(height = 48.dp)`, pas Material minimum touch target 48dp.
`Slider` Material3 sendiri sudah punya touch-target accessible bawaan (area sentuh selalu
memadai secara default terlepas dari tipisnya track visual, ini fitur aksesibilitas resmi M3,
bukan sesuatu yang perlu diatur manual). **Hasil: 0 bug** — tinggi & area sentuh sudah memadai
tanpa perlu perubahan.

0 kode, 3 dokumentasi. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (4/11):
progress/current time/remaining time mudah dibaca. Detail: `MICRO_UIUX_AUDIT.md` bagian NOW
PLAYING.

## Batch 170 — Now Playing item 2/11: spacing antar playback controls (1 file, 1 bug fix)
Item 2: "Audit spacing antar playback controls." `Row` 5 tombol (Acak/Sebelumnya/Play-Pause/
Berikutnya/Ulangi) di `NowPlayingScreen.kt` ternyata TANPA `fillMaxWidth()` maupun
`horizontalArrangement` sama sekali — cuma `verticalAlignment`. Beda dari konvensi player pada
umumnya (Spotify/Apple Music/YouTube Music selalu spread kontrol playback merata sepanjang
lebar layar) dan beda dari kebiasaan file ini sendiri yang SELALU mengomentari keputusan layout
sengaja (glow/shape/shadow di tombol play/pause tepat di baris yang sama komentarnya panjang) —
Row ini 0 komentar sama sekali, ciri oversight bukan keputusan sadar.

**`NowPlayingScreen.kt`** (diedit) — `.fillMaxWidth()` + `horizontalArrangement =
Arrangement.SpaceEvenly` ditambahkan. `SpaceEvenly` dipilih (bukan `SpaceBetween`) supaya
tombol Acak/Ulangi di kedua ujung tetap punya jarak dari tepi Column (bukan menempel rapat ke
padding 20dp), konsisten "bernapas" dengan elemen lain di layar yang sama.

1 file, 1 bug fix. Brace/paren `NowPlayingScreen.kt` seimbang (209/209, 736/736).
`FILE_MANIFEST.txt` tidak berubah (173/173). **Belum diverifikasi visual di device** — cek
prioritas: buka Now Playing, pastikan 5 tombol sekarang menyebar merata (bukan cluster rapat di
tengah) tanpa mengubah ukuran/fungsi masing-masing tombol. Item berikutnya (3/11): audit slider
height/touch area.

## Batch 169 — Kategori baru "Now Playing — Final Micro-Polish", item 1/11: audit alignment artwork/title/artist/controls (0 kode)
Kategori #5 (Interactive States) tuntas 8/8 di Batch 168 — pindah ke kategori berikutnya sesuai
urutan `MICRO_UIUX_AUDIT.md` (🟠 NOW PLAYING — FINAL MICRO-POLISH, 11 checklist item, belum
disentuh sama sekali). Item 1: "Audit alignment artwork, title, artist, dan controls."

Struktur `NowPlayingScreen.kt`: 1 `Column` root dengan `horizontalAlignment =
Alignment.CenterHorizontally` membungkus SEMUA elemen vertikal (hero `AlbumArt` 280dp, label
"SEDANG DIPUTAR", title, artist, `StarRatingRow`, slider, baris tombol transport) — jadi
alignment center konsisten by-construction, bukan disetel manual per elemen (genuinely 0 celah
untuk elemen "kelewatan" beda alignment sendiri-sendiri). Title pakai `basicMarquee()` (scroll
otomatis kalau kepanjangan, `maxLines=1`), artist pakai `TextOverflow.Ellipsis` (potong diam,
`maxLines=1`) — beda treatment, TAPI genuinely pola umum player (judul = info utama layak
di-scroll penuh, artis = sekunder cukup dipotong) bukan inkonsistensi kebetulan. Baris tombol
transport (`Row(verticalAlignment = Alignment.CenterVertically)`) konsisten dgn pola row lain
di file yang sama.

**Hasil: 0 bug.** Item 1/11 kategori Now Playing selesai diaudit. 0 kode, 3 dokumentasi.
`FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya: "Audit spacing antar playback
controls." Detail: `MICRO_UIUX_AUDIT.md` bagian NOW PLAYING.

## Batch 168 — Micro UI/UX kategori #5 penutup: audit konsistensi lintas-aksi (item terakhir, 8/8, 0 kode)
Item terakhir kategori #5 (checklist: "Hindari feedback visual yang berbeda untuk action yang
sama"). Cari aksi yang muncul di >1 lokasi UI dengan fungsi identik — kandidat paling jelas:
toggle Favorit (dipakai persis sama di `LibraryScreen.kt` SongRow dan `NowPlayingScreen.kt`).

**Icon/tint/haptic/contentDescription**: identik di kedua lokasi (ikon Favorite/FavoriteBorder,
tint primary/secondary, `HapticFeedbackType.TextHandleMove`, deskripsi "Tambah/Hapus dari
favorit") — komentar di `LibraryScreen.kt` sendiri malah mencatat haptic ini SUDAH disamakan di
batch lampau ("Was the only place this toggle fired with zero haptic").

**1 beda ditemukan**: `NowPlayingScreen.kt` pakai `Modifier.bouncyPress(pressedScale = 0.75f)`
di tombol favoritnya, `LibraryScreen.kt` TIDAK — grep `bouncyPress` di seluruh `ui/*.kt`
konfirmasi `LibraryScreen.kt` genuinely 0 pemakaian `bouncyPress` di file itu sama sekali
(favorit maupun 7 `IconButton` lain di row yang sama: close/addToPlaylist/hide/delete/dst),
sementara `VaultSheet.kt` (list singkat) DAN `NowPlayingScreen.kt` (hero screen) sama-sama
pakai `bouncyPress` di tombol list-row/aksi utama mereka masing-masing. Jadi bukan pola bersih
"list row = tanpa bounce, non-list = bounce" (`VaultSheet` list row punya bounce) — genuinely
ambigu: bisa jadi disengaja (list Library berisi ratusan lagu, animasi scale di tiap tap bisa
terasa berat/berulang di list sepadat itu, beda dari `VaultSheet` yang listnya pendek) atau
genuinely kelewatan.

**TIDAK dieksekusi batch ini** — pola sama Batch 162/163/165 (EmptyState icon, LibraryFilterChips
fill, banner 3-arah sebelum Batch 166): dicatat sebagai observasi ke-3 yang tertunda keputusan
user, bukan diasumsikan mana yang benar sebelum dikonfirmasi.

**Kategori #5 kini 8/8 sub-item teraudit** (pressed, disabled, loading, selected, empty, error,
success, lintas-aksi) — status naik ke ✅ audit selesai. **3 observasi masih tertunda eksekusi**:
(1) `EmptyState` icon hardcode (Batch 162), (2) `LibraryFilterChips` custom-fill vs FilterChip
(Batch 163), (3) Favorit `bouncyPress` di atas. Kategori berikutnya (belum mulai sama sekali):
6-14 "Now Playing s/d Component Consistency". Detail: `MICRO_UIUX_AUDIT.md` status table baris #5.

## Batch 167 — Hotfix: import `dp` hilang di Utils.kt (dari log CI Batch 166, 1 file)
User upload log CI gagal (`log_fail_220.zip`, GitHub Actions `compileReleaseKotlin`+
`compileDebugKotlin` FAILED, 7x "Unresolved reference: dp" di `Utils.kt` baris 171-191) — ini
tepat verifikasi compile Gradle yang dicatat "belum dilakukan" di `PROJECT_STATE.md` Batch 166,
dan langsung menangkap bug nyata: composable `ResultBanner` baru (Batch 166) pakai 8 literal
`.dp` (`RoundedCornerShape(8.dp)`, `.padding(12.dp, 10.dp)`, `.padding(14.dp)`,
`Modifier.width(8.dp)`/`width(10.dp)`, dst) tapi `import androidx.compose.ui.unit.dp` tidak
pernah ditambahkan — sebelum Batch 166, `Utils.kt` genuinely 0 pemakaian `.dp` sama sekali
(`AlbumArt`/`bouncyPress`, 2 composable lain di file itu, tidak pakai dp literal), jadi import
ini belum pernah dibutuhkan sebelumnya di file ini.

**`Utils.kt`** (diedit, 1 baris) — `import androidx.compose.ui.unit.dp` ditambahkan. Dicek
ulang 4 file lain yang ikut disentuh Batch 166 (`BackupRestoreSheet`/`DiagnosticLogSheet`/
`SignatureMatcherSheet`/`UpdateCheckSheet`) — SEMUA sudah punya import `dp` dari sebelumnya
(dipakai di tempat lain di file yang sama, bukan cuma di blok `ResultBanner` yang diedit), jadi
Batch 166 tidak menghapus import yang masih dibutuhkan di file manapun — murni 1 baris hilang
di 1 file. 0 perubahan logic/visual lain.

1 file, 1 baris. Brace/paren `Utils.kt` seimbang (17/17, 64/64). `FILE_MANIFEST.txt` tidak
berubah (173/173). **Masih belum diverifikasi compile Gradle ulang setelah fix ini** — prioritas
TERTINGGI batch berikutnya kalau user push: `./gradlew assembleDebug` bersih, konfirmasi 0 error
serupa lolos lagi. Pelajaran dicatat: composable baru yang dipindah ke file shared (`Utils.kt`)
wajib dicek importnya SENDIRI dari nol, bukan diasumsikan ikut lengkap cuma karena ditulis
bersebelahan kode lain yang sudah punya semua importnya. Detail: log asli `log_fail_220.zip`.

## Batch 166 — Eksekusi pending item Batch 165: unifikasi ResultBanner (Atomic Change, 5 file)
User konfirmasi lanjut. Kelompok B Batch 165 (banner hasil-operasi, 3-arah tidak konsisten)
disatukan jadi 1 composable shared, BUKAN dipaksa 1 tampilan tunggal — 3 gaya visual yang sudah
ada (masing-masing punya alasan bobot semantik berbeda, lihat Batch 165) dipertahankan lewat
parameter `style`, supaya akar masalahnya (implementasi diketik ulang 3x, gampang saling
melenceng) hilang tanpa memaksa keputusan desain yang belum eksplisit dikonfirmasi user.

**`Utils.kt`** (diedit) — `enum class ResultBannerStyle { Solid, Tinted, Bare }` +
`@Composable fun ResultBanner(style, icon, text, containerColor, contentColor, modifier)` baru,
ditaruh di file yang sama tempat `AlbumArt`/`bouncyPress` (shared composable lain) sudah hidup.
3 cabang `when(style)` mereproduksi PERSIS 3 implementasi lama byte-demi-byte (warna, shape,
padding, gap, text style) — **0 perubahan visual disengaja** dari refactor ini sendiri.
`Solid`: bg container-role + `RoundedCornerShape(8.dp)` + padding 12h/10v + gap 8dp +
`bodySmall`. `Tinted`: bg `containerColor.copy(alpha=0.15f)` + `shapes.medium` + padding 14dp +
gap 10dp + `bodyMedium`. `Bare`: tanpa background sama sekali + gap 8dp + `bodyMedium`.

**`BackupRestoreSheet.kt`/`DiagnosticLogSheet.kt`** (diedit) — blok `Row`+`background`+`Icon`+
`Text` manual diganti `ResultBanner(style = Solid, ...)`. Import `background`/
`RoundedCornerShape`/`Alignment` yang jadi tidak terpakai lagi di kedua file dihapus (dicek
`grep` per simbol dulu sebelum dihapus, bukan tebakan).

**`SignatureMatcherSheet.kt`** (diedit) — blok banner match/mismatch/error diganti
`ResultBanner(style = Tinted, ...)`. `ApkPickerRow` (composable lain di file yang sama, TIDAK
disentuh) masih pakai `Icon`/`Text` sendiri jadi import itu tetap ada — cuma `background` +
`Alignment` yang jadi tak terpakai (dicek sama, dihapus).

**`UpdateCheckSheet.kt`** (diedit) — private `fun StatusBanner(...)` (dipanggil di 2 titik,
KEDUA call site TIDAK disentuh sama sekali) badan fungsinya diganti jadi delegasi 1 baris ke
`ResultBanner(style = Bare, ...)` — cara paling minim-risiko untuk file ini, 0 titik pemanggilan
perlu diubah.

5 file (`Atomic Change` — 1 task "unifikasi ResultBanner", scope memang tidak bisa dipecah lebih
kecil tanpa meninggalkan setengah-selesai; sama presisi kelas pengecualian Batch 91/95/119). 0
file baru, `FILE_MANIFEST.txt` TIDAK berubah (173/173 tetap match — cek diff ulang sebelum
repack). Brace/paren ke-5 file dicek otomatis & seimbang. **Belum diverifikasi compile Gradle
sungguhan maupun visual di device** — prioritas berikutnya kalau user push: (1) `./gradlew
assembleDebug` build bersih (nama fungsi/parameter baru rawan typo Kotlin yang cuma ketahuan
compile-time), (2) buka Settings → Backup/Restore, import 1 file valid & 1 file rusak,
konfirmasi banner sukses/gagal tampil PERSIS sama seperti sebelum batch ini, (3) sama untuk
Diagnostic Log export, (4) Signature Matcher: bandingkan 2 APK sama & 2 APK beda, konfirmasi
warna tertiary(match)/error(mismatch) + tint 15% masih benar, (5) Update Check: picu state Error
(mis. matikan koneksi kalau ada cara simulasi), konfirmasi tampilan bare-banner tidak berubah.
Detail lengkap perbandingan byte-demi-byte tiap style: lihat KDoc `ResultBanner` di `Utils.kt`.

## Batch 165 — Micro UI/UX kategori #5 lanjutan: audit error state & success/confirmation feedback (0 kode, 3 dokumentasi)
Sub-item ke-5&6/8 kategori #5 (2 digabung — ternyata 1 komponen visual yang sama dipakai untuk
keduanya, lihat temuan di bawah). Grep `colorScheme.error`/`Icons.Default.Error`/
`Icons.Default.Warning`/`isError` di seluruh `ui/*.kt` → 9 file. Dikelompokkan per fungsi
sebenarnya (bukan cuma lokasi grep-match), 2 kelompok ditemukan:

**Kelompok A — teks error validasi inline** (4 titik: `LockScreen.kt` "PIN salah"/lockout
countdown, `SettingsScreen.kt` `SetPinDialog`, `VaultSheet.kt` setup+unlock section) — pola
identik di keempatnya: `Text` polos, `colorScheme.error`, `typography.bodySmall`, `Spacer(4.dp)`
di atasnya, TANPA ikon/background. **Hasil: 0 bug**, genuinely konsisten (`VaultSheet.kt` yang
ditulis belakangan — Batch 119 — sudah otomatis mengikuti pola `LockScreen`/`SettingsScreen`
yang lebih tua, bukan kebetulan).

**Kelompok B — banner hasil operasi/status** (4 titik across 3 file) — **3-arah TIDAK
konsisten, genuinely gap, DICATAT bukan langsung dieksekusi**:
1. `BackupRestoreSheet.kt` + `DiagnosticLogSheet.kt` (identik satu sama lain): solid
   `primaryContainer`/`errorContainer` bg + `RoundedCornerShape(8.dp)` + padding 12h/10v +
   gap ikon-teks 8dp + `bodySmall` + warna teks/ikon `onPrimaryContainer`/`onErrorContainer`.
   Konteks: hasil SEKALI TAMPIL setelah 1 aksi user (import/export selesai).
2. `SignatureMatcherSheet.kt`: bg `bannerColor.copy(alpha=0.15f)` (tint transparan dari warna
   semantik itu sendiri, BUKAN container role M3) + `shapes.medium` + padding 14dp semua sisi +
   gap 10dp + `bodyMedium` + warna teks = `bannerColor` langsung (bukan `onXContainer`).
   Konteks: state perbandingan yang tetap terlihat sambil user lanjut baca tombol "Lihat
   Laporan Lengkap" di bawahnya.
3. `UpdateCheckSheet.kt`'s `StatusBanner` (composable privat sendiri): TANPA background sama
   sekali — cuma `Icon`+`Spacer(8dp)`+`Text`, `bodyMedium`, warna = parameter `color` langsung.
   Konteks: 1 state di antara beberapa state stepper (Checking→Available→Downloading→
   ReadyToInstall→Error), sengaja minim biar tidak bersaing visual dengan progress
   bar/tombol di sekitarnya.

Checklist audit sendiri eksplisit menyebut "Hindari feedback visual yang berbeda untuk action
yang sama" — 3 treatment berbeda untuk satu konsep semantik (ikon+warna+teks status) persis
kelas gap itu. **Kenapa TIDAK langsung disamakan batch ini** (beda dari fix `SpeedDialog`
Batch 163 yang langsung dieksekusi): 3 konteks di atas punya alasan bobot-visual yang bisa jadi
disengaja (hasil-sekali-tampil vs state-perbandingan-persisten vs status-dalam-stepper) — bukan
2 kontrol identik bersebelahan dalam 1 dialog seperti kasus `SpeedDialog`. Unifikasi berarti
menyentuh 3 file sekaligus untuk keputusan desain (container-solid vs tint-alpha vs
tanpa-background — yang mana jadi standar?) yang belum eksplisit dikonfirmasi user, pola sama
persis Batch 162 (`EmptyState` icon hardcode) & Batch 163 (`LibraryFilterChips` solid-fill) —
observasi dicatat, tunggu keputusan user sebelum eksekusi.

**Kalau user pilih lanjut eksekusi (kandidat batch berikutnya)**: opsi paling minim-risiko
adalah ekstrak 1 composable shared `ResultBanner(color, icon, text, style: Solid|Tinted|Bare)`
dipakai ulang di 3 file, ATAU pilih 1 treatment jadi standar tunggal untuk ketiganya — perlu
keputusan eksplisit dulu, bukan diasumsikan.

Sisa kategori #5 (setelah ini, 5/8 diperiksa — 2 diperiksa+0bug, 1 diperiksa+1bug-fixed, 1
diperiksa+1observasi-tertunda-lama, 1 diperiksa+1observasi-baru-di-atas): konsistensi
lintas-aksi-sama (item terakhir). Detail lengkap: `MICRO_UIUX_AUDIT.md` status table baris #5.

## Batch 164 — Eksekusi pending item Batch 163: indikator "sedang diputar" di SongRow Library (2 file, 1 protected edit parsial)
Item tertunda #2 dari 2 observasi Batch 163 (kategori #5 selected/active state) — user
konfirmasi lanjut eksekusi. `SongRow` (LibraryScreen.kt, 3 call site: tab Lagu/GroupedListView/
SearchResultsView) sebelumnya 0 indikator lagu mana yang sedang diputar, padahal `QueueSheet`'s
`QueueRow` sudah punya sejak lama (primary 12% alpha background + title bold+primary). Gap
cross-context: user browsing Library sambil lagu main tidak pernah lihat baris mana yang aktif.

**`LibraryScreen.kt`** (diedit, non-protected) — 1 parameter baru di level top `LibraryScreen`:
`currentSongId: Long? = null` (default aman — fixture/preview lain yang mungkin masih memanggil
tanpa parameter ini tetap compile, jatuh ke "tidak ada highlight" seperti sebelum batch ini,
bukan crash). Diteruskan lewat 5 titik pemanggilan internal: `SongListView` (tab Lagu + tab
Favorit, keduanya reuse fungsi yang sama), `GroupedListView` x2 (grouping Artis & Folder),
`SearchResultsView` — masing-masing fungsi private ini juga dapat parameter `currentSongId:
Long? = null` sendiri, diteruskan lagi ke `SongRow(isPlaying = song.id == currentSongId)` di
titik pemanggilan `SongRow` masing-masing (pola perbandingan identik `QueueSheet`'s `isPlaying
= index == currentIndex`, cuma dibandingkan lewat song ID bukan index karena `SongRow` tidak
punya konsep index queue).

`SongRow` sendiri dapat parameter baru `isPlaying: Boolean = false` + render disamakan PERSIS
pola `QueueRow` (bukan didesain ulang dari nol): background `primary.copy(alpha=0.12f)` di
seluruh Row (dipasang SEBELUM `.clickable()` di modifier chain, urutan sama persis QueueRow,
supaya ripple clickable tetap kelihatan di atas warna latar ini bukan ketutup), judul lagu
`fontWeight = Bold` + `color = primary` saat aktif, dan ikon `Icons.Default.GraphicEq` 16dp
(sama ikon yang dipakai `QueueRow`) muncul di depan judul dalam 1 `Row` baru yang membungkus
teks judul (sebelumnya `Text` polos langsung anak `Column`) — `Text` judul dapat
`Modifier.weight(1f, fill=false)` di dalam Row baru itu supaya `basicMarquee()`+truncation tetap
berfungsi sama seperti sebelumnya, cuma sekarang berbagi baris dengan ikon. Artis tidak diubah
(QueueRow juga tidak mem-bold artis, cuma judul).

**`MainActivity.kt`** (diedit, **protected asset — edit parsial**, 1 titik) — pemanggilan
`LibraryScreen(...)` yang sudah ada dapat 1 baris baru: `currentSongId = uiState.currentSong?.id`
— `uiState.currentSong` sudah lama ada di scope composable route `"library"` ini (dipakai
`onPlayNext`/`onAddToQueue` beberapa baris di atasnya untuk cek "belum ada lagu diputar sama
sekali"), jadi 0 state/StateFlow baru diperlukan, murni reuse. `?.id` — null wajar saat cold
start sebelum lagu pertama diputar, `LibraryScreen` sudah menangani lewat default parameter yang
sama, bukan kasus yang perlu di-guard eksplisit di sini.

**Kenapa aman digabung ke 1 batch (beda dari alasan Batch 163 menunda)**: satu-satunya alasan
Batch 163 menunda item ini adalah "perlu parameter baru + wiring state lintas `MainActivity`/
`NavGraph`, di luar cap 3 file/1 task kecil kalau digabung diam-diam ke batch audit" — begitu
jadi task tersendiri (bukan menumpang batch audit lain), cakupannya genuinely cuma 2 file (1
non-protected + 1 protected edit-parsial-1-baris), sudah pas di dalam cap batch normal.

0 file baru → `FILE_MANIFEST.txt` tidak berubah (173/173 tetap match, diverifikasi diff eksplisit
terhadap isi disk). Brace/paren `LibraryScreen.kt` (332/332, 719/719) & `MainActivity.kt`
(251/251, 583/583) dicek otomatis & seimbang. **Belum diverifikasi compile/runtime Gradle
sungguhan** (tidak ada JDK/Android SDK di sandbox ini) — prioritas berikutnya kalau user push:
(1) `./gradlew assembleDebug` build bersih, (2) di device: putar 1 lagu dari tab Lagu, buka tab
Favorit/Artis/Folder/hasil pencarian yang juga memuat lagu yang sama, pastikan baris itu
genuinely ter-highlight (bg + bold + ikon) di SEMUA konteks tersebut, bukan cuma di tab asal,
(3) ganti lagu (skip/next dari mini player atau notifikasi) selagi Library masih terbuka,
pastikan highlight pindah ke baris yang benar tanpa perlu navigasi ulang ke tab Library (uiState
Compose State harusnya sudah live-recompose, tapi belum pernah dilihat langsung di device), (4)
mode `selectionMode` aktif (checkbox tampil) — pastikan `isPlaying` tetap benar dan tidak
bentrok visual dengan `Checkbox`/warna seleksi.

## Batch 163 — Micro UI/UX kategori #5 lanjutan: audit selected/active state — 1 bug fix, 2 observasi tertunda (1 file kode + 3 dokumentasi)
Sub-item ke-4/8 kategori 5 "Interactive States" (`MICRO_UIUX_AUDIT.md`), lanjutan Batch 162.
Scope: cari SEMUA titik `selected =` / `isSelected` app-wide, kelompokkan per pola visual,
tentukan mana genuinely konsisten vs genuinely gap vs "kelihatan beda tapi defensible by-design"
— disiplin sama persis Batch 159/160/161 (audit formal, bukan cari-cari bug dipaksakan).

**Metode**: `grep -rn "selected = \|isSelected" app/src/main/java/com/rudi/audioplayer/ui/*.kt`
→ 13 titik ditemukan di 7 file (`LibraryScreen.kt` x3, `NowPlayingScreen.kt` x5,
`EqualizerSheet.kt` x2, `RingtoneCutterSheet.kt` x4 termasuk definisi `DestinationChip`,
`SettingsScreen.kt` x1, `SmartPlaylistScreen.kt` x2; `QueueSheet.kt` pola beda, `isPlaying`,
dicek terpisah). Tiap titik ditelusuri manual ke implementasi visualnya (bukan cuma dihitung
dari nama parameter), dikelompokkan jadi 3 taksonomi:

**Taksonomi #1 — Card preview selection (border + elevation)**: `ThemeOptionCard`
(SettingsScreen.kt) — kartu preview tema penuh-lebar, `selected` -> `BorderStroke(2.dp, primary)`
+ `shadowElevation` naik. 1 titik, cocok utk konteksnya (kartu preview visual besar, beda level
dari list-option/tag kecil) — bukan dibandingkan ke taksonomi lain, komponennya sendiri memang
beda kelas.

**Taksonomi #2 — List/dialog single-choice (RadioButton)**: `TransitionModeOption`
(NowPlayingScreen.kt, dipakai 2x utk Gapless/Fade Halus — sudah ada sejak Batch 102) memakai
`RadioButton` sungguhan. **Ditemukan 1 bug nyata**: `SpeedDialog`'s daftar kecepatan (0.5x-2x,
di dialog "Pengaturan Putar" — DIALOG YANG SAMA PERSIS dengan `TransitionModeOption`, cuma
dipisah 1 `HorizontalDivider` + 1 `Switch` Mode Audiobook di antaranya) sebelumnya pakai
`TextButton` polos + suffix teks "  ✓" dan warna teks berubah primary/onSurface — bahasa
visual SAMA SEKALI BEDA utk konsep identik (pilih 1 dari beberapa opsi), padahal user scroll
1 layar yang sama utk lihat keduanya berdampingan. Fix: blok `options.forEach` diganti jadi
`Row` + `RadioButton` + `Text`, struktur (`clip(RoundedCornerShape(Radius.md))` + `clickable` +
`padding(vertical=8.dp, horizontal=4.dp)` + `Spacer(width=4.dp)`) disalin persis dari
`TransitionModeOption` yang sudah ada di file yang sama — **0 import baru** (`RadioButton`,
`Radius.md`, `clip`, `clickable` semua sudah dipakai di file ini sebelumnya, dicek `grep -c`
sebelum ditulis). Efek samping baik yang tidak disengaja: whole-row sekarang clickable (dulu
cuma area teks `TextButton`), row jadi lebih besar/nyaman disentuh — konsisten arah Batch 141
hit-target audit walau bukan itu tujuan utama edit ini.

**Taksonomi #3 — Tag/filter chip selection (Material3 FilterChip)**: diperiksa 7 titik —
`EqualizerSheet.kt` (preset kuat x1, preset device x1), `SmartPlaylistScreen.kt` (folder chip
x1, genre chip x1), `RingtoneCutterSheet.kt` (3x lewat `DestinationChip`, wrapper custom yang
internally tetap panggil `FilterChip` polos tanpa override warna). **0 custom `colors =`
override ditemukan di titik manapun** — semua pakai selected-state bawaan Material3
(`secondaryContainer` bg). Genuinely konsisten, 0 bug.

**2 observasi TERTUNDA keputusan eksplisit user** (BUKAN dieksekusi diam-diam — pola persis
Batch 162's EmptyState-icon finding: temuan nyata, tapi berisiko/di luar cap kalau langsung
dieksekusi tanpa konfirmasi):

1. **`LibraryFilterChips` (LibraryScreen.kt) tidak masuk taksonomi #3 walau secara visual
FUNGSINYA tag/filter row** (`LazyRow` pilihan tab Lagu/Album/Artis + chip "Lainnya" dropdown) —
custom `Box().background(if (selected) primary else surface)` SOLID fill, bukan `FilterChip`.
Beda dari 7 titik taksonomi #3 lainnya yang semua `secondaryContainer` (lebih lembut). Dua
bacaan yang sama-sama masuk akal: (a) SENGAJA — ini kontrol navigasi PRIMER (menentukan seluruh
isi layar), pantas lebih tegas/bold dari filter genre/folder yang sifatnya opsional/sekunder,
beda hierarki fungsi bukan bug; (b) genuinely inkonsistensi styling yang tidak disengaja. Ini
kontrol paling sering dilihat siapa pun yang buka app (selalu di atas tab Library) — mengubah
tanpa konfirmasi eksplisit user dulu terlalu berisiko utk batch audit kecil. Dicatat di
`MICRO_UIUX_AUDIT.md` tabel status, menunggu keputusan.

2. **`SongRow` (LibraryScreen.kt, dipakai 3 call site: tab Lagu, `GroupedListView`,
`SearchResultsView` — grep-confirmed via komentar yang sudah ada di file) tidak punya konsep
"lagu ini sedang diputar" SAMA SEKALI** — dibandingkan `QueueSheet`'s row yang SUDAH punya
(`background = if (isPlaying) primary.copy(alpha=0.12f) else Transparent` + `fontWeight =
if (isPlaying) Bold else Normal`, `isPlaying = index == currentIndex`). User yang buka tab
Library sambil lagu lain sedang main tidak pernah dapat sinyal visual lagu mana yang aktif — gap
cross-context yang nyata dan bisa dibilang termasuk "selected/active state" kategori ini.
**Sengaja tidak dieksekusi**: dicek dulu — signature lengkap `fun LibraryScreen(...)` (24
parameter) TIDAK ADA satu pun yang bawa info current-song/currentSongId. Perbaikannya berarti:
(a) parameter baru di `LibraryScreen` + `SongRow` (3 call site internal), (b) wiring data lagu
aktif dari mana pun state itu hidup sekarang (kemungkinan `MainActivity`/`NavGraph`, keduanya
**protected asset**) turun ke `LibraryScreen`. Itu jelas bukan lagi "audit kecil 1 file", di
luar cap batch — butuh keputusan eksplisit user dulu, sama seperti EmptyState-icon Batch 162.

**Sisa kategori #5** (setelah batch ini, 4/8 sub-item selesai): empty state (icon-mismatch
Batch 162, masih tertunda keputusan), error state, success/confirmation feedback, konsistensi
feedback visual lintas-aksi-yang-sama lainnya (di luar 2 yang sudah ditemukan+fix di batch ini).

## Batch 162 — Micro UI/UX kategori #5 dimulai: audit disabled/pressed/loading — 0 bug, ditemukan pola shared-composable yang sudah aman by construction (3 dokumentasi, 0 kode)
Kategori #5 Interactive States (belum mulai sejak checklist diadopsi Batch 125). 3 dari 8
sub-item diperiksa:

**Disabled state (icon-button tint)**: 5 titik `IconButton(enabled = ...)` app-wide
(`PlaylistScreen`/`QueueSheet` — tombol naikkan/turunkan/hapus urutan) — **100% identik**,
`tint = if (canX) secondary else secondary.copy(alpha = 0.3f)`. Dibandingkan dengan disabled
state `LockScreen.kt` (`.alpha(if (enabled) 1f else 0.4f)`, 2 titik keypad/glyph button) — beda
mekanisme (alpha komposit vs tint-kondisional) & beda konteks (dim seluruh tombol vs dim ikon
saja) secara wajar, bukan gap: keypad butuh dim total (nomor tidak relevan sama sekali saat PIN
penuh), reorder-icon cuma butuh dim tint (baris lagu tetap harus kebaca). `Button`/
`OutlinedButton` disabled: grep `disabledContentColor`/`disabledContainerColor` app-wide = 0
hasil — semua andalkan default Material3, otomatis konsisten.

**Pressed state (ripple/indication integrity)**: grep `indication = null` app-wide = 0 hasil —
tidak ada ripple yang sengaja/tidak sengaja dimatikan di mana pun.

**Loading state**: `ShimmerBrush()` (skeleton loading `HomeScreen`+`LibraryScreen`) — 1
composable bersama, 2 titik pakai, otomatis identik by construction. `CircularProgressIndicator`
cuma 1 titik app-wide (`UpdateCheckSheet`) — terlalu sedikit untuk dibandingkan, bukan berarti
gap, cuma belum ada kandidat pembanding.

**Hasil: 0 bug** — pola sama Batch 143/145/160 (audit formal, genuinely konsisten atau
belum-cukup-kandidat, bukan dipaksa cari bug). 0 file kode diedit, 0 protected asset.

**Catatan untuk sesi berikutnya (BUKAN bug, tapi observasi)**: `EmptyState` (`LibraryScreen.kt`,
1 composable dipakai 9 titik) hardcode `Icons.Default.MusicNote` utk SEMUA konteks — termasuk
"Belum ada folder terdeteksi"/"Antrean kosong"/"Belum ada data dengar" yang secara semantik
bukan soal musik. Title/subtitle/spacing 100% konsisten (dijamin shared composable), tapi ikon
generik utk semua konteks berpotensi kurang match. **SENGAJA tidak dieksekusi batch ini** —
menambah parameter `icon` custom berarti ubah signature composable + sentuh 9 file pemanggil
sekaligus, jauh di atas cap batch kecil, dan condong ke wilayah "tambah opsi baru" bukan murni
"perbaiki yang sudah ada" — perlu keputusan eksplisit user dulu sebelum dieksekusi.

**Sisa kategori #5 (5 dari 8 sub-item belum disentuh)**: selected/active state (baru 2 kandidat
ditemukan, `NowPlayingScreen` lyrics-highlight vs `QueueSheet` now-playing-row — beda konteks,
belum cukup data), empty state (icon-mismatch di atas, keputusan tertunda), error state, success/
confirmation feedback, konsistensi feedback lintas-aksi-yang-sama.

## Batch 161 — Micro UI/UX kategori #3 TUNTAS: audit line-height — 5 gap sistemik ditemukan & diperbaiki (1 file kode + 2 dokumentasi)
Item "line-height (belum diaudit sama sekali)" kategori #3 (pending sejak Batch 149/160,
terakhir sub-item kategori #3). Cek `grep -rn "lineHeight"` seluruh `ui/`: **0 hasil di semua
composable file** — hanya var lokal tak-terkait di `TactileDepth.kt` (canvas drawing, bukan
typography). Ditelusuri ke akar: `theme/Type.kt`'s `AppleTypography`/`TactileTypography` (2
`Typography` custom yang override 5 dari 15 style Material3 — `titleLarge`/`titleMedium`/
`bodyMedium`/`bodySmall`/`labelSmall`) dibuat via `TextStyle(...)` manual **tanpa parameter
`lineHeight`** — default Compose untuk itu adalah `TextUnit.Unspecified` (rapat, bukan leading
proporsional M3), SEMENTARA 10 style lain yang TIDAK di-override (`labelLarge`, `bodyLarge`,
`titleSmall`, dst — dipakai luas, termasuk 9 titik "field label" Batch 159 & sisi lain) otomatis
warisi `lineHeight` default M3 yang benar. **Gap sistemik nyata**: teks multi-baris yang pakai
salah satu dari 5 style ini (title bottom sheet Batch 149, body/label song-row Batch 154, badge/
kicker Batch 160 — SEMUA kena) tampil lebih rapat dari yang seharusnya, tidak konsisten dengan
10 style lain di app yang sama.

**Fix**: `lineHeight` ditambahkan ke ke-5 style, di KEDUA `Typography` (simetris) — nilai
dihitung proporsional dari rasio default M3 utk slot style yang sama (bukan angka tebakan):
`titleLarge` 22sp→28sp M3 asli (rasio lineHeight/fontSize 28/22=1.2727) diterapkan ke fontSize
custom 28sp → **35.6sp**; `titleMedium` rasio 24/16=1.5 → fontSize 17sp → **25.5sp**;
`bodyMedium` rasio 20/14=1.4286 → fontSize 15sp → **21.4sp**; `bodySmall` rasio 16/12=1.3333 →
fontSize 13sp → **17.3sp**; `labelSmall` — fontSize custom (11sp) KEBETULAN identik fontSize
default M3 (11sp), jadi lineHeight dipakai persis **16sp** M3 tanpa perlu skala.

1 file kode diedit (`Type.kt`, 10 titik — 5 style × 2 objek Typography), 0 file baru, 0
protected asset. Brace/paren seimbang (0/0 kurung kurawal karena data class builder, 21/21
kurung biasa). **Blast radius LEBIH LUAS dari batch-batch sebelumnya** — `Type.kt` dipakai
`MaterialTheme` app-wide, jadi ini memengaruhi SEMUA teks yang pakai 5 style ini di SELURUH
layar, bukan 1-2 file terisolasi. **Belum diverifikasi visual sama sekali** — prioritas TINGGI
cek manual setelah build: teks judul bottom sheet (`titleMedium`), body song-row (`bodyMedium`),
label/badge (`labelSmall`) di beberapa layar berbeda, pastikan line-height terlihat lebih lega
tapi TIDAK merusak layout yang mepet (card compact, row sempit) — kalau ada yang kepotong/
overflow gara-gara baris jadi lebih tinggi, laporkan baliknya, rollback per-style gampang (baris
`lineHeight` tinggal dihapus).

Dengan ini, **kategori #3 Typography Hierarchy dinyatakan TUNTAS** (Batch 149 title + 154 body/
label + 159 field-label + 160 badge/kicker + 161 line-height) — sisa 1 sub-item "cakupan penuh
truncation/ellipsis" (sebagian sudah Batch 37) SENGAJA tidak diklaim tuntas, beda sub-kategori,
kandidat audit terpisah kalau diminta eksplisit.

## Batch 160 — Micro UI/UX kategori #3 lanjutan: audit badge/kicker/value-readout — 0 bug, pola dikonfirmasi konsisten (3 dokumentasi, 0 kode)
Item "badge/axis-label/nav-text (belum diaudit formal)" kategori #3 (pending sejak Batch 159).
13 titik `typography.label*` sisa (di luar 2 kelompok yang sudah diaudit Batch 149/154/159)
dikelompokkan & diperiksa:

**Kelompok "setting-item/dialog caption" (7 titik, 1 file `NowPlayingScreen.kt` + `EqualizerSheet.kt`
+ `StatsDashboardScreen.kt`)** — "Peredam Dalam Aplikasi", "Preset Kuat", "Preset Bawaan
Perangkat", "Kecepatan", "Transisi Antar Lagu", `label` param generik, day-of-week chart caption
— **100% konsisten**, semua `labelSmall`+secondary.

**Kelompok "screen-title eyebrow/kicker" (5 titik, 4 file)** — "BERANDA"/"LIBRARY"/"PENGATURAN"
(3x `labelSmall`+secondary, baseline screen biasa) vs "LANJUTKAN MENDENGARKAN"
(`labelSmall`+**primary**, kartu highlight) vs "SEDANG DIPUTAR" (`labelSmall`+**animatedAccent**,
warna dinamis ikut sampul album). Ukuran (`labelSmall`) **konsisten di ke-5 titik** — variasi
warna genuinely disengaja (kartu/section yang ingin ditonjolkan pakai warna aksen, bukan lupa
disamakan), bukan bug.

**"Value readout" `NowPlayingScreen.kt` baris 1115** (`labelMedium`+onSurface, angka persen di
bawah ikon slider) dibandingkan `UpdateCheckSheet.kt` "Mengunduh… X%" (`bodyMedium`, teks inline
1 kalimat) — beda fungsi (readout numerik mandiri vs teks kalimat), bukan pasangan yang wajar
disamakan.

**Hasil: 0 bug**, ke-13 titik sisa genuinely konsisten atau berbeda konteks secara sengaja. 0
file kode diedit, 0 protected asset. Kategori #3 kelompok `typography.label*` (24 titik total)
sekarang **tuntas diaudit 100%** (Batch 149 title/149 gap-fix + 154 body/label song-row + 159
field-label + 160 badge/kicker/readout). **Sisa kategori #3**: line-height (belum diaudit sama
sekali), cakupan penuh truncation/ellipsis (sebagian Batch 37, belum formal kategori #3).

## Batch 159 — Micro UI/UX kategori #3 lanjutan: samakan label field ApkPickerRow SignatureMatcherSheet (1 file kode + 2 dokumentasi)
Item "label/caption text-style audit belum dimulai" kategori #3 (pending sejak Batch 154).
Scope: bandingkan style Text "caption/label di atas control" (field-label pattern) di seluruh
`ui/*.kt`. **Metode**: grep semua `typography.label{Small,Medium,Large}` (24 titik, 26 file),
kelompokkan per konteks fungsi.

**Kelompok "field label di atas control" (9 titik/2 file) 89% konsisten**: `SmartPlaylistScreen`
(5x: "Folder"/"Genre"/"Rentang durasi"/"Rentang tahun rilis"/"Rating minimum") +
`RingtoneCutterSheet` (3x: "Awal:"/"Akhir:"/"Simpan sebagai") — semua `labelLarge`, warna default
(bukan secondary). **1 gap nyata**: `SignatureMatcherSheet.kt`'s `ApkPickerRow` (dipakai 2x label
"APK Lama"/"APK Baru" — fungsi identik: caption statis di atas tombol picker) pakai
`labelMedium`+`color = secondary`, beda dari 8 titik lain. Fix: disamakan ke `labelLarge`, warna
default.

**Konteks lain SENGAJA tidak disentuh** (beda fungsi, bukan kandidat unifikasi): `AbPointButton`
(`ABRepeatBookmarkSheet`) caption kecil DI DALAM tombol berpasangan dengan value bold — beda
layout dari field-label-di-luar-control; `LyricsSheet` progress status "Sinkronisasi Lirik —
baris N/M" — caption dinamis bukan field label statis; `NowPlayingScreen`/`HomeScreen`/
`EqualizerSheet`/`StatsDashboardScreen`/`LibraryScreen`/`SettingsScreen` — badge/axis/nav-text,
beda konteks, belum diaudit formal (kandidat kelompok terpisah kalau lanjut).

Brace/paren `SignatureMatcherSheet.kt` seimbang (53/53, 133/133). 1 file kode + 2 dokumentasi. 0
protected asset. **Sisa kategori #3**: badge/axis-label/nav-text (kelompok belum diaudit di
atas), line-height, cakupan penuh truncation/ellipsis (sebagian Batch 37, belum formal kategori
#3). **Belum diverifikasi visual** — perubahan kecil (1 baris style), risiko rendah.

## Batch 158 — Dokumentasi: arsipkan detail Batch 1-57 ke PROJECT_STATE_ARCHIVE.md (1 file baru + 3 dokumentasi, 0 kode)
Eksekusi langsung "catatan jujur" di respons Batch 157: `PROJECT_STATE.md` sudah 3102 baris dan
terus tumbuh tiap batch (Chronological Document Rule — entri baru selalu di baris teratas),
bikin section "wajib dibaca" makin jauh dari mayoritas isi lama yang jarang dibutuhkan lagi.

**Perubahan**: Batch 57 ke bawah (737 baris, rentang Batch 57-1) dipotong utuh dari
`PROJECT_STATE.md`, dipindah — bukan ditulis ulang/diringkas, isi & urutan descending PERSIS
sama — ke file baru `PROJECT_STATE_ARCHIVE.md`, dengan header penjelasan singkat kenapa file ini
ada. 1 baris pointer ditinggal di `PROJECT_STATE.md` di titik potong, mengarah ke arsip.
`PROJECT_STATE.md`: 3102 → 2388 baris, sekarang cuma menyimpan 100 batch paling baru (Batch
58-157). `FILE_MANIFEST.txt` diperbarui (172→173 file). Intro `PROJECT_STATE.md` disebut ulang
soal arsip ini.

**Sengaja TIDAK disentuh**: section "Riwayat insiden kronologis (jangan dihapus)", "Keputusan
arsitektur utama", "Struktur package", "Konvensi penamaan ZIP & versi", "Aturan sesi" — semua
itu daftar kurasi/referensi umum (bukan dump mentah per-batch), beberapa eksplisit ditandai
"jangan dihapus". `CHANGELOG.md` TIDAK ikut dipotong — tetap sumber detail penuh untuk SEMUA
batch (1-157+), arsip ini murni soal `PROJECT_STATE.md`.

0 kode, 0 protected asset. **Ambang arsip berikutnya**: kalau batch aktif tumbuh lagi ke ~100
(sekitar Batch 258), ulangi pola sama — geser cutoff 100 batch dari batch terakhir saat itu.

## Batch 157 — Dokumentasi: pindahkan ringkasan aturan sesi ke posisi tetap paling atas PROJECT_STATE.md (2 dokumentasi, 0 kode)
Pertanyaan langsung user: "yakin rule tadi gak bakal tenggelam oleh informasi baru?" Jawaban
jujur: risikonya NYATA. Rule Batch 155 ("Aturan sesi: transparansi versi & pesan commit")
ditaruh di section paling BAWAH `PROJECT_STATE.md` (~baris 3057 dari 3102 total) — sementara
section paling sering dibaca sesi manapun ada di paling ATAS ("Batch terakhir yang selesai"),
yang terus memanjang tiap batch baru (Chronological Document Rule: entri baru selalu disisipkan
di baris teratas). Kalau ada sesi yang cuma sempat baca sebagian file, atau context ter-
truncate, rule di bawah bisa genuinely terlewat.

**Fix**: ringkasan 2 rule (dari Batch 155) ditambahkan di section BARU "⚠️ ATURAN SESI AKTIF —
WAJIB DIBACA", diletakkan tepat setelah paragraf pembuka file — posisi ini TETAP, tidak pernah
ikut tergeser walau "Batch terakhir yang selesai" terus tumbuh ke bawah. Isi lengkap tetap ada
di § "Aturan sesi: transparansi versi & pesan commit" (tidak dihapus/dipindah, cuma diringkas
ulang di 2 tempat — di atas untuk visibilitas, di bawah untuk detail penuh).

2 dokumentasi (`CHANGELOG.md`/`PROJECT_STATE.md`), 0 kode, 0 protected asset.

## Batch 156 — Fitur: catatan rilis (pesan commit) tampil langsung di layar "Cek Update" app (3 file kode + 2 dokumentasi)
Permintaan langsung user, jawab pertanyaan "apakah pesan update langsung dari aplikasi juga ikut
berubah?" — jawaban sebelumnya: TIDAK, app cuma nampilin `tagName` (angka versi doang), field
`body` GitHub Release tidak pernah di-fetch maupun ditampilkan. User minta dieksekusi utuh.

**3 file kode, rantai lengkap API GitHub → app**:
1. **`.github/workflows/build.yml`** (protected asset, edit parsial) — step baru "Capture commit
   message for release notes": `git log -1 --pretty=%B > release_notes.txt` (file, bukan
   `$GITHUB_OUTPUT`, supaya aman dari delimiter heredoc kalau pesan commit multi-baris). Step
   "Create GitHub Release" ditambah `body_path: release_notes.txt` — mengisi field `body` GitHub
   Release dengan pesan commit HEAD (yang sejak Rule Batch 155 wajib berisi penjelasan fitur
   singkat, bukan cuma angka versi).
2. **`GitHubReleaseChecker.kt`** — `ReleaseInfo` dapat field baru `releaseNotes: String`, di-
   parse dari `json.optString("body", "")` (API `GET /releases/latest`). Fallback `""` untuk
   rilis lama pra-Batch 156 yang belum punya body.
3. **`UpdateCheckSheet.kt`** — state `Available` sekarang render `s.release.releaseNotes` (kalau
   `isNotBlank()`) di bawah baris `"Update tersedia: ..."`, style `bodySmall`+secondary (selaras
   subtitle pattern app-wide, bukan style baru). Blank-check WAJIB — rilis lama tanpa body tidak
   menampilkan kotak kosong.

Brace/paren `GitHubReleaseChecker.kt` (12/12, 53/53) & `UpdateCheckSheet.kt` (25/25, 62/62)
seimbang. YAML `build.yml` divalidasi parse (`python3 -c "import yaml..."`) + urutan step
dikonfirmasi benar (Capture commit message SEBELUM Create GitHub Release). 0 asset lain
terpengaruh (`AndroidManifest.xml`/`build.gradle.kts`/dll tidak disentuh).

**Efek end-to-end mulai push berikutnya**: pesan `git commit -m "..."` yang dikirim lewat skrip
"Update Harian:" (sudah wajib deskriptif sejak Batch 155) otomatis jadi teks yang user lihat di
app-nya sendiri saat "Cek Update" — bukan cuma di chat/CHANGELOG lagi. **Belum diverifikasi di
device/CI run sungguhan** (workflow YAML tervalidasi sintaksnya, tapi belum pernah benar-benar
jalan di GitHub Actions dengan step baru ini) — prioritas cek: push batch ini, pastikan release
GitHub berikutnya field "body"-nya terisi pesan commit, lalu buka "Cek Update" di app buat
konfirmasi teksnya muncul. Cap 3-file/batch SENGAJA dilewati batch ini atas instruksi eksplisit
user ("eksekusi utuh dan sampai tuntas") — 1 fitur kohesif, bukan gabungan beberapa task
independen.

## Batch 155 — Dokumentasi: tambah aturan sesi transparansi versi & pesan commit (2 dokumentasi, 0 kode)
Permintaan langsung user, screenshot layar "Cek Update" app (versi terpasang 1.1.43 vs update
tersedia v1.1.44-run206) sebagai konteks — 2 rule baru wajib buat semua sesi AI berikutnya:

1. **Transparansi versi tiap kirim ZIP** — diadaptasi dari permintaan "bump manual" literal,
   karena `versionCode`/`versionName` SUDAH sengaja auto-derive dari jumlah commit git sejak
   Batch 30 (dipertegas lagi Batch 86/87) justru untuk MENGHAPUS kebutuhan bump manual dan
   risiko lupa yang menyertainya. Mengedit manual balik ke pola lama = regresi arsitektur.
   Sebagai gantinya: tiap sesi wajib sebut nomor batch ZIP + ingatkan versionName final baru
   pasti setelah `git push` (bukan sinkron 1:1 dengan nomor batch chat).
2. **Box code pesan commit di atas heading "Update Harian:"** — diikuti persis sesuai permintaan.
   Mulai batch ini, tiap respons yang punya skrip "Update Harian:" wajib diawali code-box pesan
   commit terpisah, isinya wajib penjelasan fitur singkat dari `CHANGELOG.md` batch tsb (bukan
   cuma angka versi/perbandingan version polos).

Ditulis formal di `PROJECT_STATE.md` § "Aturan sesi: transparansi versi & pesan commit". 2
dokumentasi (`CHANGELOG.md`/`PROJECT_STATE.md`), 0 kode, 0 protected asset —
`app/build.gradle.kts` TIDAK disentuh (sengaja, lihat alasan Rule 1). Diterapkan mulai respons
chat ini juga.

## Batch 154 — Micro UI/UX kategori #3 lanjutan: samakan gaya song-row FolderManagerSheet (1 file kode + 2 dokumentasi)
Item "audit body/label/caption" kategori #3 (pending sejak Batch 149). Scope: bandingkan style
`song.title` di SEMUA "song row ringkas di dalam bottom sheet" (bukan `SongRow` utama layar
Lagu yang memang lebih besar/beda konteks — 48dp album art + durasi, dipakai 3 tempat, `title-
Medium`+`bodySmall`, sudah baseline tersendiri yang wajar).

**Metode**: grep `song.title` berpasangan dengan style tetangganya di semua sheet. **Kelompok
"song row ringkas dalam sheet" (5 titik/3 file) sudah 100% konsisten**: `DuplicateFinderSheet`,
`PlaylistScreen`, `VaultSheet` x2 — title=`bodyMedium`, subtitle(artist)=`bodySmall`+secondary.
**1 gap nyata**: `FolderManagerSheet.kt` baris song "Lagu Disembunyikan" — title pakai
`titleMedium` (ukuran level `SongRow` utama) tapi subtitle tetap `bodySmall`+secondary (level
kelompok sheet-ringkas) — kombinasi CAMPUR dari 2 baseline berbeda, padahal secara fungsi baris
ini identik dengan 5 titik kelompok sheet-ringkas (list lagu terbatas di dalam modal, bukan
layar penuh). Fix: title disamakan ke `bodyMedium`.

Brace/paren `FolderManagerSheet.kt` seimbang (42/42, 105/105). 1 file kode + 2 dokumentasi. 0
protected asset. `MICRO_UIUX_AUDIT.md` status table SENGAJA belum disentuh batch ini (cap 3
file) — disinkronkan batch berikutnya, jangan ditunda >1 batch (pelajaran Batch 148). **Belum
diverifikasi visual** — perubahan kecil (1 baris ukuran font), risiko rendah. **Sisa kategori
#3**: label/caption text-style audit belum dimulai, line-height, cakupan penuh truncation/
ellipsis (sebagian Batch 37, belum formal kategori #3).

## Batch 153 — Dokumentasi: sinkronkan status table kategori #2 di MICRO_UIUX_AUDIT.md (1 dokumentasi, 0 kode)
Item pending PRIORITAS TINGGI Batch 152 (tertunda 2 batch berturut — 151+152 — pelajaran Batch
148 diterapkan, tidak ditunda lebih lama). Baris kategori #2 disinkronkan dengan progres Batch
151 (audit vertical spacing antar section `SettingsScreen.kt` — 0 bug) dan Batch 151-152 (gap
icon↔text ✅ SELESAI PENUH — 2 gap ditemukan & diperbaiki dari 29 titik diaudit). Sisa pending
dicatat: literal `.dp` lain di luar radius/icon-gap/screen-padding yang sudah disentuh. 0 kode,
0 protected asset. Kandidat batch berikutnya: lanjut kategori #2 (sisa literal `.dp`) atau
kategori #3 Typography Hierarchy (body/label/caption, pending sejak Batch 149).

## Batch 152 — Micro UI/UX kategori #2 lanjutan: samakan gap icon↔text tombol "Tambah" VaultSheet (1 file kode + 1 dokumentasi)
Menutup Pending Queue Batch 151 — bug PERSIS sama, ditemukan di audit yang sama tapi ditunda demi
cap 3 file. `VaultSheet.kt`'s `VaultContentSection` tombol "Tambah" (`TextButton`+
`Icons.Default.Add` default-size, tanpa custom `Modifier.size()`) pakai gap 4dp — sama persis
pola `PlaylistScreen.kt` "Buat Playlist Baru" yang sudah dibenarkan Batch 151. Fix: 4dp→8dp,
menyamakan ke konvensi mayoritas app-wide (14 titik lain sudah 8dp untuk kombinasi icon
default-size + label di button manapun). Brace/paren `VaultSheet.kt` seimbang (101/101,
210/210). 0 protected asset.

**Kategori #2 sub-item "gap icon↔text" sekarang ✅ SELESAI PENUH** (audit formal 29 titik Batch
151 + fix 2 titik Batch 151-152, sisanya sudah konsisten by-design). **Sisa kategori #2**: sisa
literal `.dp` lain (padding/size/offset di luar radius/icon-gap/screen-padding yang sudah
disentuh Batch 146-147/151-152) — scope masih terlalu luas untuk 1 batch, kandidat audit
per-konteks terpisah kalau diminta lanjut. Sinkron `MICRO_UIUX_AUDIT.md` status table masih
tertunda (2 batch berturut sekarang — 151+152 — prioritas TINGGI batch berikutnya, jangan
ditunda lebih lama, pola sama pelajaran Batch 148).

## Batch 151 — Micro UI/UX kategori #2 lanjutan: samakan gap icon↔text tombol "Buat Playlist Baru" (1 file kode + 1 dokumentasi)
Item pertama kategori #2 (Spacing & Sizing Consistency) sub-item "gap icon↔text" (pending sejak
Batch 147, ditandai "terlalu kontekstual buat sweep mekanis, butuh pengelompokan per-konteks
dulu"). Pengelompokan dilakukan: grep semua pasangan `Icon()` → `Spacer(width)` → `Text()` di
`ui/*.kt` (29 titik), dikelompokkan per konteks (Button/OutlinedButton default-size icon,
TextButton icon 16-18dp custom-size proporsional, menu-row Icon+Column judul+deskripsi, section
header Icon+Text). Hasil: 3 grup pertama SUDAH konsisten (default-size icon+label mayoritas 8dp
di 14 titik lintas 9 file; TextButton icon 16-18dp custom-size sengaja pakai gap lebih kecil
6dp/4dp proporsional ke ukuran icon-nya — bukan gap, `ABRepeatBookmarkSheet.kt` 2 titik dicek TIDAK
disentuh; menu-row Icon+Column 12dp konsisten 5 titik di 2 file). **1 gap nyata**: `PlaylistScreen.kt`
tombol "Buat Playlist Baru" (`TextButton` + `Icons.Default.Add` default-size, TANPA custom
`Modifier.size()`) pakai gap 4dp — menyimpang dari konvensi mayoritas 8dp utk kombinasi icon
default-size di button manapun. Fix: 4dp→8dp. Brace/paren `PlaylistScreen.kt` seimbang (96/96,
152/152). 0 protected asset.

**Pending Queue kategori #2** (ditemukan di audit yang sama, ditunda demi cap 3 file — SAMA
bug persis, `VaultSheet.kt` baris ~270, `TextButton`+`Icons.Default.Add` default-size, gap 4dp
juga): fix `VaultSheet.kt` di batch berikutnya. Sisa: vertical spacing antar section
(`SettingsScreen.kt` diaudit ulang batch ini — 7 titik pola `Spacer(12dp)→Divider→Spacer(20dp)`
SUDAH 100% konsisten, 0 bug), sisa literal `.dp` lain, sinkron `MICRO_UIUX_AUDIT.md` status
table (tertunda 1 batch demi cap file, jangan ditunda lebih lama).

## Batch 150 — Dokumentasi: sinkronkan status table kategori #3 di MICRO_UIUX_AUDIT.md (1 dokumentasi, 0 kode)
Item pending prioritas tinggi Batch 149 ("jangan tunda lebih dari 1 batch"). Baris kategori #3
diperbarui `⬜ Belum mulai` → `🟡 Berlanjut (Batch 149)` + ringkasan temuan/fix (title
`FolderManagerSheet.kt` disamakan `titleMedium`+Bold) + sisa pending (audit body/label/caption,
line-height, cakupan penuh truncation/ellipsis). 0 kode, 0 protected asset. Kandidat batch
berikutnya: lanjut kategori #3 (body/label/caption typography) atau kategori #2 (vertical
spacing/icon-text gap per-konteks, masih pending sejak Batch 147).

## Batch 149 — Micro UI/UX kategori #3 dimulai: samakan gaya title bottom sheet FolderManagerSheet (1 file kode + 2 dokumentasi)
Item pertama kategori #3 (Typography Hierarchy, urutan `FINAL EXECUTION ORDER`) — "Audit title/
subtitle/body/label/caption" + "Konsistenkan font weight". Scope sengaja 1 pola dulu (title
bottom sheet), bukan audit semua level tipografi sekaligus.

**Metode**: grep semua `Text(...)` yang jadi HEADER `ModalBottomSheet` (baris pertama tiap sheet).
**12 dari 13 sheet sudah konsisten**: `style = MaterialTheme.typography.titleMedium, fontWeight
= FontWeight.Bold` (`BackupRestoreSheet`, `DuplicateFinderSheet`, `SignatureMatcherSheet`,
`VaultSheet`, `DiagnosticLogSheet`, `ABRepeatBookmarkSheet`, `EqualizerSheet`, `LyricsSheet`,
`QueueSheet`, `RingtoneCutterSheet`, `SongInfoEditSheet`, `VisualizerSheet`). **1 gap nyata**:
`FolderManagerSheet.kt` — title `"Kelola Perpustakaan"` pakai `titleLarge` TANPA `fontWeight`
eksplisit (default M3 titleLarge = Normal/400), beda ukuran DAN beda berat huruf dari 12 sheet
lain sekaligus — pecah hierarki visual paling mencolok yang ditemukan sejauh ini di kategori #3.
Fix: disamakan ke `titleMedium` + `FontWeight.Bold` (pola mayoritas), + 1 import baru
`androidx.compose.ui.text.font.FontWeight` (file ini belum pernah pakai `FontWeight` sebelumnya).

Brace/paren `FolderManagerSheet.kt` seimbang (42/42, 105/105). 1 file kode + 2 dokumentasi. 0
protected asset. `MICRO_UIUX_AUDIT.md` status table SENGAJA belum disentuh batch ini (cap 3
file, pola sama presedan Batch 146/147→148) — disinkronkan batch berikutnya. **Belum
diverifikasi visual** — perubahan kecil (ukuran+berat font 1 baris teks), risiko rendah. **Sisa
kategori #3**: audit body/label/caption text-style konsistensi (di luar scope title sheet),
line-height, truncation/ellipsis (sebagian sudah dibereskan Batch 37 — cek ulang cakupan penuh
belum pernah dilakukan formal untuk kategori #3 ini secara spesifik).

## Batch 148 — Dokumentasi: sinkronkan status table kategori #2 di MICRO_UIUX_AUDIT.md (3 dokumentasi, 0 kode)
Item pending prioritas tinggi dari Batch 147 (tertunda 2 batch berturut-turut, 146+147, demi
jaga cap 3 file/batch) — status table kategori #2 masih tertulis `⬜ Belum mulai` padahal 2 batch
kerja nyata (146: horizontal screen padding, 147: ukuran ikon LockScreen) sudah selesai.

Baris kategori #2 diperbarui ke `🟡 Berlanjut (Batch 146-147)` + ringkasan 2 temuan/fix + 3
item pending (vertical spacing antar section, gap icon↔text — dicatat butuh pengelompokan
per-konteks dulu sebelum bisa dieksekusi aman, bukan sweep mekanis, sisa literal `.dp` lain).

**Pelajaran pola sama presedan "callout README telat sync" (Batch 123)**: dokumen tracking
status yang di-update manual (bukan auto-generate) rawan telat begitu beberapa batch berturut
sengaja skip demi cap file — kalau ada laporan "dokumentasi ketinggalan" lagi ke depan, cek juga
status table `MICRO_UIUX_AUDIT.md` ini, bukan cuma urutan `CHANGELOG.md`/`PROJECT_STATE.md`.

3 file dokumentasi (`CHANGELOG.md`/`PROJECT_STATE.md`/`MICRO_UIUX_AUDIT.md`), 0 kode, 0 protected
asset. Kandidat batch berikutnya: lanjut kategori #2 (vertical spacing/icon-text gap, scope
sempit per-konteks) atau mulai kategori #3 Typography Hierarchy.

## Batch 147 — Micro UI/UX kategori #2 lanjutan: samakan ukuran ikon fingerprint/backspace LockScreen (1 file kode + 2 dokumentasi)
Item berikutnya kategori #2 — "Samakan ukuran control yang setara". Audit `Icon(...).size(N.dp)`
eksplisit di seluruh `ui/*.kt`: cuma 9 titik total (sisanya pakai default Material 24dp), jadi
diperiksa satu-satu (bukan sweep mekanis).

**Temuan**: `LockScreen.kt`'s keypad — tombol Fingerprint (28dp) & Backspace (22dp), keduanya
render lewat `RoundGlyphButton` yang SAMA (komentar kode sendiri: `"Fingerprint/backspace: same
round tactile/skeu treatment as PinKey, smaller glyph instead of a digit"`), duduk simetris
flanking tombol "0" di baris terbawah keypad — tapi beda 6dp secara visual, melanggar niat
"treatment yang sama" yang sudah dinyatakan eksplisit di komentar. **Fix**: keduanya disamakan
ke 24dp (default Material, bukan berat sebelah ke salah satu angka lama).

**7 titik `Icon().size()` lain diaudit, TIDAK disentuh** (beda konteks genuinely, bukan
pasangan/kelompok yang perlu seragam): `ABRepeatBookmarkSheet.kt` 3 titik (18dp icon-dalam-Button
x2 + 16dp close x1, masing-masing 1 kemunculan tanpa pasangan sejenis di file yang sama),
`FeatureHintBanner.kt` 16dp close (sudah match ukuran visual `bouncyPress` dismiss lain, Batch
141), `NowPlayingScreen.kt` SkipPrevious/SkipNext 36dp+36dp (SUDAH konsisten satu sama lain,
sengaja lebih besar — transport control primer), `RingtoneCutterSheet.kt` 18dp leadingIcon (1
kemunculan, tidak ada pasangan).

Brace/paren `LockScreen.kt` seimbang (48/48, 128/128). 1 file kode + 2 dokumentasi. 0 protected
asset. `MICRO_UIUX_AUDIT.md` status table masih SENGAJA belum disentuh (cap 3 file) — akan
disinkronkan sekali di batch mendatang begitu ada slot longgar. **Belum diverifikasi visual** —
perubahan kecil (28→24, 22→24), risiko rendah. **Sisa kategori #2**: vertical spacing antar
section, gap icon↔text (diaudit sekilas — sebaran nilai Spacer 4-16dp terlalu kontekstual buat
disamakan mekanis, butuh pengelompokan per-konteks dulu di batch terpisah), sisa literal `.dp`
lain.

## Batch 146 — Micro UI/UX kategori #2 dimulai: audit horizontal screen padding tab Library (1 file kode + 2 dokumentasi)
Item pertama kategori #2 (Spacing & Sizing Consistency, urutan `FINAL EXECUTION ORDER`) —
"Samakan horizontal screen padding", scope sengaja dipersempit ke 1 layar dulu (bukan sapuan
semua `.dp` sekaligus, ~340 literal non-radius sudah ditandai berisiko sejak Batch 54).

**Metode**: bandingkan `contentPadding`/row-padding level-teratas semua tab & screen utama
(`HomeScreen`/`SettingsScreen`/`NowPlayingScreen`/`PlaylistScreen`/`SmartPlaylistScreen`/
`StatsDashboardScreen` — semua sudah konsisten 20dp horizontal, dikonfirmasi grep) vs 5 tab
`LibraryScreen.kt`. **4 dari 5 tab sudah 20dp** (`SongListView` via `SongRow`'s
`.padding(horizontal = 20.dp, ...)`) — **2 gap nyata ditemukan**:
1. **`AlbumGridView`** (tab Album) — `contentPadding = PaddingValues(16.dp)` (all-sides 16dp,
   beda dari konvensi 20dp horizontal app-wide). Fix: `PaddingValues(horizontal = 20.dp,
   vertical = 16.dp)` — vertical 16dp dipertahankan (tidak ada acuan literal buat angka itu),
   cuma horizontal disamakan.
2. **`GroupedListView`** (tab Artis/Folder) + **`SearchResultsView`** (grup Artis/Album di hasil
   pencarian) + **`SearchHistoryView`** (riwayat pencarian) — 4 titik `ListItem(...).padding(
   horizontal = 4.dp)`, JAUH lebih sempit dari 20dp konvensi (beda 16dp, paling jarak visual
   dari semua gap kategori #2 yang mungkin ditemukan). Ke-4 titik ini konsisten SATU SAMA LAIN
   (sub-pola yang sengaja seragam, kemungkinan besar pola lama sebelum konvensi 20dp mapan) tapi
   menyimpang dari konvensi dominan app. Fix: 4dp → 20dp di keempat titik, `LibraryScreen.kt`.

Brace/paren `LibraryScreen.kt` seimbang (330/330, 701/701). 1 file kode + 2 dokumentasi.
`MICRO_UIUX_AUDIT.md` status table SENGAJA belum disentuh batch ini (cap 3 file, pola sama
Batch 144) — disinkronkan batch berikutnya. **Belum diverifikasi visual** — perubahan padding
murni, risiko rendah (angka makin dekat ke konvensi mayoritas app, bukan makin menyimpang), tapi
tetap prioritas cek device: tab Album/Artis/Folder/hasil-pencarian/riwayat-pencarian sekarang
harus terasa sejajar tepi kiri-kanan dengan tab Lagu & layar lain. **Sisa kategori #2** (belum
digarap, scope besar): vertical spacing antar section, gap icon↔text, ukuran control setara,
audit ~340 literal `.dp` non-radius lain di luar horizontal screen padding.

## Batch 145 — Micro UI/UX kategori #1 TUNTAS: audit formal "Hapus" (22 titik) + sinkron status table (3 dokumentasi, 0 kode)
Lanjutan Pending Queue Batch 144, item terakhir kategori #1: tulis formal hasil audit `"Hapus"`.

**Metode**: grep ulang (bukan andalkan taksiran sekilas Batch 142) — persis 22 titik di 15 file
`ui/*.kt`, dibaca konteks satu-satu. **Hasil: 0 bug, 4 kelompok beda fungsi, genuinely bukan
kandidat unifikasi**:
1. **Label tombol konfirmasi generik** (`DiagnosticLogSheet.kt:123`, `DuplicateFinderSheet.kt:142`,
   `FolderManagerSheet.kt:228`, `LibraryScreen.kt:459,797`) — `"Hapus"` polos SENGAJA generik,
   konteks objeknya sudah jelas dari `title` dialog yang sama (pola tombol konfirmasi standar,
   bukan lupa dikasih detail).
2. **Label tombol dgn jumlah dinamis** (`DuplicateFinderSheet.kt:123` `"Hapus N Terpilih"`) —
   1 titik, unik, tidak ada padanan lain yang perlu disamakan.
3. **Title dialog "Hapus X?"** (`DuplicateFinderSheet.kt:134`, `FolderManagerSheet.kt:215`,
   `LibraryScreen.kt:444`) — sudah diaudit formal Batch 144 (pola "?" konsisten), tidak diulang.
4. **`contentDescription` aksesibilitas** (13 titik: bookmark/folder/perangkat/favorit/lirik/
   playlist/antrean/playlist-otomatis) — SENGAJA selalu full-context (`"Hapus dari favorit"`,
   `"Hapus dari antrean"`, dst.), BUKAN dipendekkan ke `"Hapus"` polos seperti kelompok 1 — screen
   reader butuh objek eksplisit karena tidak ada `title` dialog visual yang melengkapinya (beda
   kelas UI dari tombol dalam dialog). Menyamakan gaya ke sini justru akan MERUSAK aksesibilitas,
   bukan konsistensi.

**Kesimpulan kategori #1**: 3 audit wording (Batch 142 undo-label, 143 Batal/Tutup, 144 title
dialog + Aksi/Tindakan, 145 ini) tuntas 0 sisa pending. `MICRO_UIUX_AUDIT.md` status table
disinkronkan ke ✅ SELESAI (Batch 142-145).

3 file dokumentasi (`CHANGELOG.md`/`PROJECT_STATE.md`/`MICRO_UIUX_AUDIT.md`), 0 kode, 0 protected
asset. Kandidat batch berikutnya: kategori #2 Spacing & Sizing Consistency (13 kategori lain
masih ⬜ belum mulai, urutan `FINAL EXECUTION ORDER` di `MICRO_UIUX_AUDIT.md`).

## Batch 144 — Micro UI/UX kategori #1 lanjutan: audit judul dialog + samakan "Aksi"/"Tindakan" (1 file kode + 2 dokumentasi)
Lanjutan Pending Queue Batch 143, item pertama: kapitalisasi & tanda baca title dialog
konfirmasi.

**Audit 14 title `AlertDialog`** — 2 kelompok, keduanya sudah konsisten: (1) dialog konfirmasi
destruktif/dengan aksi tertunda, semua diakhiri "?" (`"Hapus ${'$'}{toDelete.size} file?"`,
`"Hapus folder tambahan?"`, `"Nonaktifkan Vault?"`, `"Timpa data saat ini?"`, `"Hapus dari
Perangkat?"`); (2) dialog form/info tanpa pertanyaan, tanpa "?" (`"Atur PIN"`, `"Sleep Timer"`,
`"Laporan Lengkap"`, dst.). `"Hapus dari Perangkat?"` sengaja Title Case (bukan bug) — dicek
silang: frasa yang SAMA PERSIS dipakai di ikon/menu-item aksi yang sama (`LibraryScreen.kt:630`
`contentDescription`, `:1207` teks menu) SEBELUM dialog ini muncul, jadi title dialog sengaja
mengulang label persis yang baru saja user tap (echo pattern, bukan inkonsistensi kapitalisasi).

**Bug nyata ditemukan** (bukan di title, di body `text`): peringatan "tidak bisa dibatalkan"
untuk aksi hapus permanen pakai kata beda untuk semantik identik — `"Aksi ini tidak bisa
dibatalkan."` (`DuplicateFinderSheet.kt`, `BackupRestoreSheet.kt`) vs `"Tindakan ini tidak bisa
dibatalkan."` (`LibraryScreen.kt`, 2 titik: hapus 1 lagu & hapus banyak lagu). Fix:
`LibraryScreen.kt` disamakan ke `"Aksi"` (pola mayoritas, 2 vs 2 sebelum fix → 4 vs 0 sesudah).

Brace/paren `LibraryScreen.kt` seimbang (330/330, 701/701). 1 file kode + 2 dokumentasi = 3 file
total (cap patuh, tidak sentuh `MICRO_UIUX_AUDIT.md` batch ini — status table disinkronkan batch
berikutnya kalau ada slot). **Sisa Pending Queue kategori #1**: tulis formal hasil cek `"Hapus"`
(22 titik, sudah dicek sekilas Batch 142 — semua konteks beda: hapus dari device/playlist/
favorit/vault/folder/log, bukan kandidat unifikasi).

## Batch 143 — Micro UI/UX kategori #1 lanjutan: audit "Batal" vs "Tutup" — 0 bug, pola dikonfirmasi konsisten (3 file dokumentasi, 0 kode)
Lanjutan Pending Queue Batch 142, item pertama: verifikasi 1-per-1 apakah wording `"Batal"`
(12 titik) vs `"Tutup"` (5 titik, total 17 — sebelumnya ditaksir ~20 dari grep kasar yang ikut
menghitung match non-UI) di `TextButton`/`AlertDialog` seluruh `ui/*.kt` genuinely konsisten.

**Metode**: baca konteks `confirmButton`/`dismissButton` di sekitar tiap 17 titik (bukan cuma
grep nama tombol). **Hasil: pola SUDAH konsisten by-design, 0 bug ditemukan** —
- `"Batal"` selalu dipasang saat dialog punya `confirmButton` yang MELAKUKAN sesuatu (simpan PIN
  `SettingsScreen.kt`, buat playlist `PlaylistScreen.kt`, hapus lagu `LibraryScreen.kt`, nonaktif
  vault `VaultSheet.kt`, dst.) — menutup dialog ini genuinely membatalkan aksi yang tertunda.
- `"Tutup"` selalu dipasang saat dialog TIDAK punya aksi tertunda buat dibatalkan — cuma info/\nviewer (laporan signature `SignatureMatcherSheet.kt`, penjelasan mode transisi
  `NowPlayingScreen.kt`) atau state di mana confirmButton-nya sendiri sudah berubah makna jadi
  "tutup" (Timer dialog tanpa timer aktif, picker playlist tanpa form create aktif
  `PlaylistScreen.kt:324`) — pilihan lain di dialog itu (kalau ada) dieksekusi langsung dari list
  item, bukan lewat confirmButton, jadi tidak ada "batal" yang berarti di situ.

**Kesimpulan**: item checklist "Samakan capitalization dan punctuation konsisten" bagian
Batal/Tutup di `MICRO_UIUX_AUDIT.md` § kategori #1 **selesai tanpa perlu 1 baris kode pun
diubah** — audit murni konfirmasi, bukan berarti pekerjaan "gratis"/dilewati. 0 file kode
disentuh batch ini (sesuai cap 3 file — kalau ada kode + 3 dokumen sekaligus, itu 4 file,
melanggar cap, jadi audit-only ini pas mengisi slot 3 dokumen tanpa kode).

**Sisa Pending Queue kategori #1** (belum digarap): (1) audit kapitalisasi & tanda baca title
dialog konfirmasi (apakah semua diakhiri "?" konsisten), (2) tulis formal hasil cek `"Hapus"`
(22 titik, sudah dicek sekilas Batch 142 — semua konteks beda, bukan kandidat unifikasi, belum
didokumentasikan resmi).

## Batch 142 — Micro UI/UX kategori #1 dimulai: wording undo-hide disamakan ke label kanonik (1 file diedit)
Kategori #4 (Touch Target) sudah ✅ selesai penuh sejak Batch 141 — lanjut ke kategori #1
(String & Wording Consistency) sesuai `FINAL EXECUTION ORDER` di `MICRO_UIUX_AUDIT.md`. Scope
kategori ini **sengaja dipersempit** sejak Batch 125: wording konsisten murni, **tanpa** migrasi
ke `strings.xml` (339 string literal sudah ditandai berisiko di README tanpa compiler untuk
verifikasi refactor sebesar itu).

**Temuan pertama** — audit label tombol Undo lintas `ui/*.kt`: pola kanonik untuk aksi "batalkan
aksi yang baru dilakukan" (undo hapus playlist/queue/dst., `UndoableAction` via Snackbar) sudah
konsisten pakai `"Urungkan"` (`MainActivity.kt:767`, satu-satunya titik `actionLabel` untuk
seluruh alur `UndoableAction`). Tapi 1 titik custom (bukan lewat Snackbar sistem, banner lokal
`LibraryScreen.kt` untuk undo "sembunyikan lagu" — dibuat custom sejak Batch 66 karena Snackbar
sistem ketutup `ModalBottomSheet`) masih pakai `"Batalkan"` — wording beda untuk semantik aksi
yang identik (undo), ditemukan lewat grep silang ke `actionLabel`/`onClick = undo*` bukan
tebakan. Fix: `LibraryScreen.kt:509` `"Batalkan"` → `"Urungkan"`. 0 logic/behavior berubah,
tombol tetap sama (`onClick = undoHide`).

**Sengaja BELUM digarap batch ini** (dicatat sebagai Pending Queue kategori #1, bukan terlewat):
- Audit `"Batal"` (13 titik) vs `"Tutup"` (7 titik) — dicek sekilas, pola tampaknya sudah
  benar by-convention (`"Batal"` = batalkan dialog sebelum aksi terjadi, `"Tutup"` = tutup
  panel/info read-only tanpa aksi pending) tapi BELUM diverifikasi 1-per-1 ke tiap 20 titik —
  perlu batch terpisah supaya tidak melebihi batas micro-batching.
- Audit konsistensi kapitalisasi & tanda baca (title dialog konfirmasi: apakah semua diakhiri
  "?" secara konsisten, apakah title-case/sentence-case konsisten) — belum diaudit sama sekali.
- Audit istilah berulang lain (mis. "Hapus" 22 titik, sudah dicek sekilas semua konteksnya
  legitimate beda — hapus dari device/playlist/favorit/vault dst — bukan 1 aksi yang sama,
  jadi BUKAN kandidat unifikasi, tapi belum ditulis formal di sini).

Brace/paren `LibraryScreen.kt` dicek otomatis & seimbang (330/330 brace, 701/701 paren). 0 file
baru, 0 protected asset. **Kategori #1 status: 🟡 dimulai** (1 temuan ditutup, checklist resmi
kategori ini di `MICRO_UIUX_AUDIT.md` masih panjang — lanjut batch berikutnya kalau user minta
teruskan).

## Batch 141 — Micro UI/UX kategori #4: hit-target audit + ripple-clip audit (2 file diedit)
Menutup 2 item terakhir kategori #4 (Touch Target & Micro Interaction) di `MICRO_UIUX_AUDIT.md`
— sekarang kategori ini ✅ selesai penuh.

**Dicek ulang dulu, bukan diasumsikan**: kandidat TextButton "Batal"/"Tutup" di 4 `AlertDialog`
(BackupRestoreSheet/DuplicateFinderSheet/SignatureMatcherSheet/SongInfoEditSheet) — dikonfirmasi
tetap keputusan sadar sejak Batch 124/127 (sekali-tekan, bukan repetitive-tap, dampak
micro-feedback lebih kecil), TIDAK disentuh batch ini.

**Hit-target size audit** — grep 40 titik `IconButton(`/`FilledIconButton(` + 46 titik
`.clickable()` di `ui/*.kt`, cari `Modifier.size()` eksplisit di bawah 48dp (minimum Material).
2 gap ditemukan:
- `FeatureHintBanner.kt` — tombol dismiss 40dp (pernah dinaikkan dari 28dp di Batch 31, belum
  sampai 48dp) → 48dp. Icon `Close` (16dp) tidak disentuh.
- `HomeScreen.kt` — `ContinueListeningCard`'s tombol play 44dp → 48dp. Icon `PlayArrow`
  (default 24dp) tidak disentuh.

Icon visual DI DALAM tombol sengaja tidak ikut diperbesar — hit-target (area sentuh transparan)
dan ukuran visual icon adalah 2 hal berbeda, menaikkan `Modifier.size()` IconButton tidak bikin
komponennya kelihatan "penuh". 38 IconButton lain dikonfirmasi sudah default Material 48dp tanpa
override (grep, bukan asumsi).

**Ripple-terpotong-container audit** — grep pola `.clip()` yang dipasang langsung di
container/ancestor `IconButton`/`.clickable()` (arah kebalikan dari kelas bug "Ambient Light gak
bocor" Batch 81/scanline containment Batch 135/137 — di sini clip TERLALU ketat yang jadi
concern, bukan bocor). **0 kasus ditemukan.**

2 file diedit, 0 file baru, 0 protected asset. Brace/paren dicek otomatis & seimbang. **Belum
diverifikasi visual/build sungguhan** — prioritas berikutnya: cek Beranda + banner hint apa pun
di device, pastikan area sentuh lebih nyaman tanpa perubahan visual icon yang aneh.

`MICRO_UIUX_AUDIT.md` — status tracking kategori #4 diperbarui jadi ✅ SELESAI PENUH.

## Batch 140 — Arsipkan ROADMAP_15_FITUR_OFFLINE.md (1 file di-rename + 2 dokumentasi diedit)
Keputusan eksplisit user: dokumen roadmap 15-fitur dihentikan — 2 item tersisa (#13 Konverter
Format Audio Lokal, #15 Alarm Musik/Wake-Up Alarm) dinilai user tidak akan dipakai, bukan
sekadar ditunda.

**Diarsipkan, bukan dihapus** — 13/15 item sudah ✅ selesai, riwayatnya tetap berguna sebagai
dokumentasi historis. `ROADMAP_15_FITUR_OFFLINE.md` → `ARCHIVED_ROADMAP_15_FITUR_OFFLINE.md`
(rename), banner "📦 ARSIP — DIHENTIKAN (Batch 140)" ditambah di paling atas menjelaskan alasan
+ bahwa file tidak dihapus supaya bisa dibuka lagi kalau user berubah pikiran. Isi 15 item di
bawah banner TIDAK diubah sama sekali dari Batch 139.

`FILE_MANIFEST.txt` — nama entri diperbarui + posisi alfabetis dikoreksi (git ls-files sortir
huruf besar duluan, `ARCHIVED_*` harusnya sebelum `CHANGELOG.md` bukan sebelum `app/*`).

**Dicek referensi dulu sebelum rename** (bukan asumsi aman) — grep lintas kode: cuma
`FILE_MANIFEST.txt` yang menunjuk nama file secara langsung; beberapa file kode
(`VisualizerSheet.kt`, `ABRepeatBookmarkSheet.kt`, dst.) menyebut "roadmap item #X" di komentar
tapi itu referensi tekstual historis (bukan import/path), aman tidak ikut disentuh.

0 kode disentuh, murni housekeeping dokumentasi.

## Batch 139 — Sinkronkan status Editor Tag Metadata di roadmap (1 file dokumentasi diedit)
User tanya "roadmap apa yang pending" — audit `ROADMAP_15_FITUR_OFFLINE.md` menemukan item #1
(Editor Tag Metadata) masih tercatat belum dikerjakan padahal SUDAH selesai sejak Batch 118,
lewat jalur dokumen Gap List terpisah (bukan dari roadmap 15-fitur ini), jadi status di file ini
tidak pernah ikut ter-sync — kelas masalah yang sama seperti banner "Update terbaru" README yang
pernah telat sync di Batch 123.

Dikonfirmasi ke codebase dulu sebelum ditandai (bukan asumsi): `Id3TagWriter.kt`, `TagEditor.kt`,
`SongInfoEditSheet.kt` semua ada, README § Fitur sudah punya baris "Edit Info Lagu (Tag Editor)".
Fix: `ROADMAP_15_FITUR_OFFLINE.md` item #1 diberi tanda "✅ SELESAI (Batch 118, via Gap List
'Wajib' terpisah)" + 1 paragraf catatan sinkronisasi + baris tabel prioritas diperbarui.

**Hasil audit — sisa roadmap yang genuinely pending (2 dari 15)**:
- #13 Konverter Format Audio Lokal (Tinggi/Tinggi)
- #15 Alarm Musik / Wake-Up Alarm (Sedang-Tinggi/Sedang-Tinggi)

**Dokumen tracking terpisah yang juga pending**: `MICRO_UIUX_AUDIT.md` — 13 dari 14 kategori
polish (#1, #2, #3, #5, #6-14) masih ⬜ belum mulai, cuma kategori #4 (Touch Target) 🟡 sebagian.

0 kode disentuh, murni housekeeping dokumentasi.

## Batch 138 — Isi UPDATE_REPO_OWNER dengan username GitHub asli (1 file diedit)
User kirim URL repo asli: `https://github.com/FDzaki-dev/AudioPlayer`. Menutup item "WAJIB
diisi manual" yang tercatat sejak Batch 136 (Release Downloader Spec).

`gradle.properties` — `UPDATE_REPO_OWNER=ganti-username-github` (placeholder) diganti
`UPDATE_REPO_OWNER=FDzaki-dev`. `UPDATE_REPO_NAME=AudioPlayer` tidak diubah (sudah cocok nama
repo sejak Batch 136). Wiring diverifikasi baca-ulang (bukan diedit) di
`app/build.gradle.kts:102-107` — `buildConfigField("String", "UPDATE_REPO_OWNER",
"\"${project.findProperty("UPDATE_REPO_OWNER") ?: "ganti-username-github"}\"")`, dibaca
`UpdateManager.kt`/`GitHubReleaseChecker.kt` saat runtime untuk `GET
/repos/{owner}/{repo}/releases/latest`.

1 file diedit, 0 file baru, 0 protected asset tersentuh (`gradle.properties` sendiri bukan
protected asset — cuma isinya yang sebelumnya sengaja placeholder aman). **Belum diverifikasi
runtime** (tidak ada akses network/GitHub API di sandbox ini) — prioritas berikutnya: rebuild,
buka Settings → Lanjutan → Tentang Aplikasi → "Cek Update", pastikan genuinely nemu release dari
`FDzaki-dev/AudioPlayer`. Kalau gagal, cek dulu apakah repo itu SUDAH punya minimal 1 GitHub
Release dengan asset `.apk` terlampir — tanpa itu endpoint `releases/latest` akan 404 terlepas
dari config ini benar atau salah.

## Batch 137 — Scanline ke 3 sheet tersisa (LyricsSheet+ABRepeatBookmarkSheet+QueueSheet, 3 file diedit)
Lanjutan langsung item "sengaja belum" Batch 135 — kandidat eksplisit sudah dicatat waktu itu:
`LyricsSheet`, `ABRepeatBookmarkSheet`, `QueueSheet`. Ditutup di batch ini, pola identik persis
`EqualizerSheet.kt`/`VisualizerSheet.kt`.

**Containment dulu, bukan asumsi aman** (pola sama Batch 135) — `frostedGlass()`'s
`background(tint, shape)` sudah shaped untuk cat latar, tapi TIDAK meng-`clip()` children/draw
sesudahnya. Tanpa `clip()` eksplisit, overlay scanline `calmScanlines()` (draw rect penuh lewat
`drawWithContent`) berisiko bocor melewati sudut membulat panel. Fix di ketiga file:
`.clip(MaterialTheme.shapes.large)` dipasang SEBELUM `.calmScanlines()`, `isCalmRetro` di-hoist
di titik yang sama seperti sheet lain (`isCalmRetroTheme()`, sebelum `ModalBottomSheet {}`).

- `LyricsSheet.kt` — 3 import baru (`clip`, `isCalmRetroTheme`, `calmScanlines`), Column modifier
  chain (`fillMaxWidth().frostedGlass().padding(...)`) disisipi `.then(...)` di antara
  `frostedGlass()` dan `padding()`.
- `ABRepeatBookmarkSheet.kt` — pola identik.
- `QueueSheet.kt` — beda kecil: Column modifier chain aslinya cuma
  `fillMaxWidth().frostedGlass()` tanpa `.padding()` level-Column (padding dikelola per-child di
  file ini), jadi `.then(...)` ditaruh langsung setelah `frostedGlass()`, urutan lain tidak
  disentuh.

`frostedGlass()` sendiri TIDAK diubah — perbaikan lokal ke 3 pemanggil baru ini saja, konsisten
dengan keputusan Batch 135 untuk tidak meng-clip semua pemanggil secara general (risiko efek
samping ke shadow/bevel Tactile/Skeu yang sudah lama stabil).

**Cakupan calmScanlines() app-wide sekarang selesai penuh** di semua sheet/panel kontrol:
`AlbumArtHero`, `SongRow` (Batch 134), `EqualizerSheet`, `VisualizerSheet` (Batch 135),
`LyricsSheet`, `ABRepeatBookmarkSheet`, `QueueSheet` (batch ini). 3 file diedit, 0 file baru, 0
protected asset. Brace/paren dicek otomatis & seimbang (LyricsSheet 63/63 brace, 162/162 paren;
ABRepeatBookmarkSheet 54/54 brace, 139/139 paren; QueueSheet 40/40 brace, 124/124 paren). 0
duplikat import ditemukan. **Belum diverifikasi visual/build sungguhan** (0 JDK/SDK di sandbox
ini) — prioritas berikutnya kalau user push: buka Lirik/A-B Repeat & Bookmark/Antrean Putar di
tema Calm Retro, pastikan scanline genuinely muncul dan tidak bocor keluar sudut panel.

## Batch 136 — Release Downloader Spec: cek update manual dari GitHub Release (9 file)
**Membalik keputusan sadar Batch 8** ("app ini tidak punya izin INTERNET sama sekali, itu
bagian dari klaim privasinya") — atas permintaan eksplisit user, dengan syarat tidak mengganggu
logic app yang sudah ada. `INTERNET` + `REQUEST_INSTALL_PACKAGES` ditambah ke
`AndroidManifest.xml`, tapi keduanya HANYA dipakai satu jalur: tombol manual "Cek Update" baru
di Settings → Lanjutan → Tentang Aplikasi. Tidak ada auto-check di background/app start, tidak
ada analytics/ads/telemetry lain yang menumpang izin ini.

- `update/UpdateDownloader.kt` (baru) — unduh biner APK rilis, streaming chunk 8KB langsung ke
  `Buffer`→disk (Okio, transitive dari OkHttp), TIDAK PERNAH `ResponseBody.bytes()`/`.string()`
  untuk biner (itu akan menampung seluruh APK di RAM). Timeout eksplisit connect 15s/read 20s,
  `followRedirects(true)` (aset GitHub Release di-redirect 302 ke CDN), header
  `Accept: application/octet-stream` + opsional `Authorization: Bearer <token>`.
- `update/GitHubReleaseChecker.kt` (baru) — `GET /repos/{owner}/{repo}/releases/latest`, cari
  asset `.apk`. Metadata JSON-nya kecil jadi `.string()` di sini wajar (aturan "jangan buffer di
  RAM" khusus biner APK, bukan payload JSON kecil).
- `update/UpdateManager.kt` (baru) — orkestrasi Checking→Available→Downloading→ReadyToInstall,
  `MutableStateFlow` diamati UI. Singleton terpisah, sengaja tidak menyentuh
  `PlayerViewModel`/`PlaybackService` sama sekali.
- `ui/UpdateCheckSheet.kt` (baru) — bottom sheet, pola sama persis `SignatureMatcherSheet.kt`.
- `ui/SettingsScreen.kt` (diedit, 2 titik) — 1 baris state (`showUpdateCheck`) + 1 row baru di
  bawah "Tentang Aplikasi", murni tambahan, 0 baris existing diubah/dihapus.
- `AndroidManifest.xml` (protected, diedit) — 2 permission + 1 `<provider>` FileProvider baru
  (authorities `${applicationId}.updateprovider`, scoped ke `cacheDir` via `file_paths.xml` baru,
  `exported="false"`).
- `app/build.gradle.kts` (protected, diedit) — 1 dependency baru (`okhttp:4.12.0`) + 2
  `buildConfigField` (`UPDATE_REPO_OWNER`/`UPDATE_REPO_NAME`) dibaca dari `gradle.properties`.
- `gradle.properties` (diedit) — **WAJIB diisi manual**: `UPDATE_REPO_OWNER=ganti-username-github`
  masih placeholder, ganti ke username GitHub asli sebelum "Cek Update" bisa nemu rilis.

**Sengaja belum**: auto-check berkala/di background (spec eksplisit hanya minta jalur manual);
verifikasi signature APK hasil unduhan sebelum install (`SignatureMatcherSheet.kt` sudah ada
sebagai alat manual terpisah, belum diotomatisasi masuk ke alur ini). 0 file dihapus.

## Batch 135 — Scanline disebar ke panel kontrol (Equalizer + Visualizer, 2 file diedit)
Lanjutan langsung item "sengaja belum" Batch 134: Pilar A (`calmScanlines()`) sekarang juga di
`EqualizerSheet.kt` + `VisualizerSheet.kt` — 2 panel kontrol paling literal di app ini (slider
band/preset & spectrum bar), keduanya berbagi shell identik sejak Batch 92 (`ModalBottomSheet` +
`Column.frostedGlass()`).

**Containment dulu, bukan asumsi aman** — dicek dulu ke `frostedGlass()` (`BlurUtils.kt`):
`background(tint, shape)` di dalamnya SUDAH shaped untuk cat latar, tapi TIDAK meng-`clip()`
children/draw sesudahnya (pelajaran sama persis dengan insiden "Ambient Light gak bocor" Batch
81 untuk shadow). Tanpa `clip()` eksplisit, overlay scanline `calmScanlines()` (draw rect penuh
lewat `drawWithContent`) akan menggambar persegi penuh sampai ke bounds Column — bisa bocor
melewati sudut membulat panel. Fix: `.clip(MaterialTheme.shapes.large)` dipasang SEBELUM
`.calmScanlines()` di kedua file (pola sama AlbumArtHero/SongRow — scanline SETELAH clip),
`isCalmRetro` di-hoist di titik yang sama seperti sheet lain.

**Sengaja belum**: sheet lain (`LyricsSheet`, `ABRepeatBookmarkSheet`, `QueueSheet`, dst.) —
kandidat "panel kontrol" juga tapi ditunda demi batch kecil (pola sama presedan aberrasi CTA
yang meluas bertahap Batch 129->130->131, bukan sekaligus semua). `frostedGlass()` sendiri
TIDAK disentuh (perbaikan clip di sini murni lokal ke 2 pemanggil, tidak general — kalau nanti
lebih banyak sheet dapat scanline, pola `.clip(MaterialTheme.shapes.large).calmScanlines()` ini
tinggal disalin, bukan alasan untuk mengubah `frostedGlass()` sendiri jadi meng-`clip()` semua
pemanggilnya, yang berisiko ke perilaku shadow/bevel Tactile/Skeu yang sudah lama stabil).

2 file diedit, 0 file baru, 0 protected asset. Brace/paren dicek otomatis & seimbang
(`EqualizerSheet.kt` 27/27 `{}` 96/96 `()`; `VisualizerSheet.kt` 10/10 `{}` 68/68 `()`). **Belum
diverifikasi compile/visual sungguhan** — prioritas berikutnya kalau user push: `./gradlew
assembleDebug` build bersih, lalu di device buka Equalizer & Visualizer di tema Calm Retro,
pastikan scanline genuinely terkurung rapi di dalam sudut panel (tidak bocor ke tepi sheet) dan
tidak mengganggu keterbacaan slider/label preset.

## Batch 134 — Calm Retro v3: tuntaskan 2 item yang sengaja ditunda Batch 133 (2 file diedit)
Lanjutan langsung Batch 133 ("gak usah greedy" — sengaja ditunda ke batch terpisah, bukan
terlewat). 2 item dari catatan "Sengaja BELUM digarap" batch itu, dikerjakan bersama karena
sama-sama scope kecil & terkait Pilar A yang sama:

**1. Scanline disebar ke Card list lagu** (`LibraryScreen.kt`) — Pilar A (`calmScanlines()`,
Batch 133) sebelumnya cuma di `AlbumArtHero` (Now Playing). Spec markdown eksplisit sebut
"daftar lagu, panel kontrol" sebagai target lain, ditunda demi batch kecil. `SongRow` — 1
composable yang dipakai ulang di SEMUA tampilan daftar lagu (tab Lagu/`GroupedListView`/
`SearchResultsView`, 3 call site, grep-confirmed) — `isCalmRetro` di-hoist (pola sama
`isTactile`/`isSkeu` di `NowPlayingScreen.kt`), `calmScanlines()` dipasang di thumbnail
`AlbumArt` 48dp SETELAH `.clip()` (pola sama AlbumArtHero — scanline terkurung rapi di dalam
shape bulat-persegi, tidak meluber ke row di sekitarnya). 1 edit di `SongRow` otomatis
menjangkau ketiga tampilan sekaligus, tidak perlu disentuh satu-satu. **Sengaja belum**: panel
kontrol lain (Equalizer/Visualizer sheet dst.) — di luar cakupan "list lagu" yang diminta
eksplisit, kandidat lanjutan terpisah kalau diminta.

**2. Audit blur backdrop album-art 80dp/15%** (`NowPlayingScreen.kt`) — item "Do's" spec yang
Batch 133 tandai "belum diaudit". Hasil audit: backdrop blur generik (`AlbumArt` full-screen di
belakang seluruh layar Now Playing) sudah ADA sejak lama (Batch 67, berlaku SEMUA identitas)
— jadi ini bukan gap fungsional (Calm Retro sudah dapat backdrop, sama seperti tema lain),
murni beda ANGKA dari literal spec (spec minta blur 80dp/opacity 15%, kode existing generik
60dp/alpha 50%). Karena "Do's" cuma saran (bukan salah satu 4 Pilar wajib) dan angkanya eksplisit
beda, Calm Retro sekarang dapat intensitasnya sendiri di titik ini — `backdropBlurRadius`/
`backdropAlpha` di-gate `isCalmRetro` (80.dp/0.15f vs 60.dp/0.5f default), reuse `isCalmRetro`
yang sudah di-hoist sejak Batch 129, 0 hoist baru. Identitas lain 0 perubahan perilaku (tetap
60dp/50% seperti sebelum batch ini).

2 file diedit, 0 file baru, 0 token warna baru, 0 protected asset. Brace/paren dicek otomatis &
seimbang di kedua file (`LibraryScreen.kt` 330/330 `{}`, 701/701 `()`; `NowPlayingScreen.kt`
209/209 `{}`, 719/719 `()`). **Belum diverifikasi compile/visual sungguhan** (0 JDK/Android SDK
di sandbox ini, konsisten sama seperti setiap batch tema sebelumnya) — prioritas berikutnya
kalau user push: `./gradlew assembleDebug` build bersih, lalu di device cek (1) scanline di
thumbnail 48dp daftar lagu genuinely kebaca tapi tidak bikin row kelihatan kotor/berdebu, (2)
backdrop Now Playing Calm Retro genuinely lebih halus/samar dari identitas lain (15% vs 50%
alpha — perbedaan besar, paling berisiko meleset dari niat "jauh"/subtle spec tanpa device
untuk verifikasi visual langsung).

## Batch 133 — Calm Retro v3 upgrade: 2 pilar identitas baru dari spec (3 file diedit)
User upload `palet_warna_calm_retro_v3.md` ("Calm Cyber-Analog"), penerus
`palet_warna_calm_retro_v2.md` yang jadi basis Batch 128-132. Diaudit dulu: 7 token HEX §1 di
v3 identik persis dengan yang sudah ada di `Color.kt` (0 perubahan warna dibutuhkan). 4 Pilar
Identitas §2 diaudit satu-satu vs kode existing: Pilar B (aberrasi CTA) sudah ada sejak Batch
129 — 0 sentuhan. Pilar A (CRT scanlines), C (tipografi monospace), D (grain organik) BELUM
pernah digarap — batch ini menutup 2 dari 3 gap itu (A & D, primitive baru) + C (murni per-Text,
tanpa primitive baru), sesuai instruksi eksplisit user "dilarang keras overthinking" — scope
dijaga sempit ke titik paling representatif per pilar (bukan disebar ke semua Card/Sheet
sekaligus, pola "gak usah greedy" yang sama seperti histori Calm Retro Batch 129-131).

**Pilar A — Soft CRT Scanlines** (`TactileDepth.kt`, fungsi baru `calmScanlines()`) — garis
horizontal berulang 4px (setengah transparan/setengah gelap tipis) via `Brush.verticalGradient`
+ `TileMode.Repeated` (GPU-side, murah dipanggil tiap frame, 0 loop draw manual) — literal
terjemahan CSS `linear-gradient(...50%, rgba(0,0,0,0.3) 50%)` di spec, alpha 0.03f = literal
spec "opacity: 0.03". Dipasang di `NowPlayingScreen.kt`'s `AlbumArtHero` (permukaan terbesar,
paling representatif untuk "Card/Album Art" yang diminta spec) — SETELAH `.clip(heroShape)`
(beda dari teknik shadow Tactile/Skeu yang sengaja bocor SEBELUM clip di titik yang sama),
supaya garis scanline terkurung rapi di dalam bentuk album art, tidak meluber ke luar.

**Pilar D — Organic Grain Overlay** (`TactileDepth.kt`, fungsi baru `calmGrain()`) — spec minta
noise monokrom di seluruh kanvas app, opacity maks 4%. Compose tidak punya raster-noise
generator bawaan tanpa RenderEffect (API 31+, di luar `minSdk 23` project ini) — didekati lewat
speckle field seeded (bukan bitmap — pola sama "hand-drawn" seperti `calmAberration()`/
`skeuEmboss()`), posisi & alpha tiap speck dihitung SEKALI per ukuran layar via `drawWithCache`
(bukan re-roll tiap frame, biaya render tetap murah), rentang alpha 0.015f-0.04f (di bawah
plafon 4% spec), warna putih polos. Dipasang di `MainActivity.kt` (protected, edit parsial) —
slot arsitektur SAMA PERSIS dengan `identityRootBrush` (root ambient wash Tactile/Skeu), 1 titik
cakupan seluruh app, HANYA aktif saat `appThemeIdentity == ThemeIdentity.CALM_RETRO`.

**Pilar C — Muted Monospace** (`NowPlayingScreen.kt`, tanpa primitive baru) — `FontFamily.Monospace`
diterapkan HANYA ke 2 `Text` durasi/waktu berjalan (elapsed & total/-mm:ss) di baris waktu Now
Playing, gated `isCalmRetro` yang sudah di-hoist sejak Batch 129. **Sengaja TIDAK diterapkan ke
judul lagu atau lirik** — larangan eksplisit spec §4 ("JANGAN memberikan efek distorsi warna
atau font berbeda pada teks lirik/judul lagu utama... harus tetap bersih"). Typography global
(`AppleTypography`, reuse sejak Batch 130) TIDAK disentuh — keputusan itu tetap benar, monospace
di sini murni override lokal 2 Text, bukan ganti Typography seluruh identitas.

**Sengaja BELUM digarap** (dicatat, bukan terlewat, jaga batch tetap kecil): scanlines belum
disebar ke Card list lagu/panel kontrol lain (spec sebut "daftar lagu, panel kontrol" juga —
kandidat batch lanjutan kalau diminta, sama presedan aberrasi CTA yang mulai dari 1 titik lalu
meluas Batch 130-131); blur album-art 80dp/15% sebagai backdrop jauh Now Playing (bagian
"Do's" §4, bukan salah satu dari 4 Pilar inti) belum diaudit ada/tidaknya mekanisme serupa di
kode existing untuk Calm Retro secara spesifik; monospace belum diperluas ke tag kualitas audio
(`FLAC 24-bit` dst.) — app ini belum punya UI yang menampilkan bitrate/format eksplisit di
Now Playing, jadi tidak ada titik pemasangan yang valid saat ini.

3 file diedit (`TactileDepth.kt`, `NowPlayingScreen.kt`, `MainActivity.kt` protected/parsial), 0
file baru, 0 token warna baru (semua HEX v3 = v2). Brace/paren dicek otomatis & seimbang di
ketiga file. **Belum diverifikasi compile/visual sungguhan** (0 JDK/Android SDK di sandbox ini,
konsisten sama seperti setiap batch tema sebelumnya) — prioritas berikutnya kalau user push:
`./gradlew assembleDebug` build bersih (`TileMode.Repeated`/`drawWithCache` API yang paling
berisiko salah pakai tanpa compiler), lalu di device cek (1) scanline genuinely kebaca tapi
tidak mengganggu keterbacaan album art, (2) grain terasa "berbutir hangat" bukan malah kelihatan
kotor/berdebu berlebihan, (3) angka durasi Now Playing genuinely pakai font monospace tanpa
mengubah tampilan judul/lirik sama sekali.

## Batch 132 — FIX: Calm Retro tenggelam di lagu beraksen kuat (2 file)
User lapor pakai screenshot: tombol play tampak flat merah polos (bukan Muted Sage), aberrasi
Dusty Rose/Denim tak kelihatan sama sekali. Root cause: `animatedAccent` (dipakai jadi warna
CTA + wash latar + rating) selama ini SELALU ikut `accentColor` dinamis hasil ekstraksi warna
dominan album art per-lagu (`accentColor ?: fallback`) — fallback ke warna tema HANYA kalau
ekstraksi gagal/null. Untuk lagu dengan album art didominasi warna kuat (merah di screenshot),
Muted Sage & aberrasi 0.35f-alpha jadi tak mungkin kebaca sama sekali, ketimpa merah + wash
gradient ikut merah juga. Ini bukan bug baru — fitur "tint dari album art" ini disengaja &
lama (Color.kt: "modern music player can tint itself from the artwork"), berlaku sama untuk
SEMUA identitas — tapi khusus Calm Retro, ini bertabrakan langsung dengan filosofi
"identitas terkunci total, tidak ikut-ikutan" yang sudah ditetapkan sejak Batch 128 (dark
terkunci) — locknya sekarang meluas ke accent juga.

`NowPlayingScreen.kt` + `MiniPlayerBar.kt` — 1 baris `targetValue` di masing-masing
`animateColorAsState` (`animatedAccent`): `if (isCalmRetro) fallback else (accentColor ?:
fallback)` — Calm Retro SELALU `CalmRetroAccent` literal, identitas lain 0 perubahan perilaku
(masih dinamis seperti sebelumnya). 1 titik kontrol per file (semua CTA/wash/rating turunan
dari `animatedAccent` yang sama), jadi cukup 2 baris total untuk fix penuh — `isCalmRetro`
sendiri sudah di-hoist sejak Batch 129, 0 hoist baru dibutuhkan.

0 protected asset, 0 file baru, 2 file diedit. **Belum diverifikasi compile Gradle sungguhan**
(tidak ada JDK/SDK di sandbox) — secara logis root cause & fix sudah dikonfirmasi dari kode,
tapi verifikasi visual sungguhan (screenshot ulang setelah build) masih perlu user.

## Batch 131 — Calm Retro: live-showcase preview di picker Settings (1 file)
Menutup gap terakhir dari audit cakupan (dilaporkan ke user setelah Batch 130): Tactile/Skeu
sudah live-showcase di baris preview `ThemeOptionCard`, Calm Retro belum.

`SettingsScreen.kt` (satu-satunya file diedit) — `isCalmRetroPreview` flag baru, `calmAberration
(bias = 2.dp)` (fungsi Batch 129, 0 fungsi baru) diterapkan ke lingkaran aksen 30dp preview
(BUKAN ke seluruh `Surface` kartu seperti `isEmbossPreview` Tactile/Skeu) — sengaja lebih
presisi, meniru scope asli CTA play/pause (Batch 129: aberrasi cuma di tombol bulat, bukan
panel). Konsisten dengan keputusan Batch 130 bahwa Card Calm Retro tetap flat/opaque (identitas
"calm", bukan physical-panel seperti Tactile/Skeu) — cuma titik showcase-nya (lingkaran aksen)
yang dapat efek, sama presisi dengan aplikasi aslinya di app.

0 protected asset, 0 file baru, 0 fungsi baru (reuse `calmAberration()` yang sudah ada), 1 file
diedit. **Belum diverifikasi compile Gradle sungguhan** (tidak ada JDK/SDK di sandbox).

## Batch 130 — Calm Retro: pemisahan & pemurnian visual dari identitas lain (1 file)
Lanjutan Batch 128-129 — fase "pemurnian": hilangkan SEMUA token/warisan visual yang masih
dipinjam dari identitas lain, supaya Calm Retro 100% otonom (prinsip yang sama yang sudah
berlaku utk Tactile/Skeu sejak Batch 61, lihat komentar "Dirancang OTONOM total, tidak
menumpang baseline Tactile" di `Color.kt`).

`Theme.kt` (satu-satunya file diedit):
- `tertiary`/`onTertiary` — dulu reuse `SkeuDarkSuccess` (token identitas Neumorphism).
  Sekarang reuse `CalmRetroAccent` sendiri (Muted Sage sudah cukup "hijau positif", 0 warna
  asing ditambah — sesuai instruksi "gak usah greedy").
- `error` — dulu reuse `SkeuDarkError`. Sekarang `CalmRetroAberrationLeft` (Dusty Rose) —
  token milik Calm Retro sendiri (sudah dipakai di `calmAberration()` Batch 129), dipakai
  ulang di peran semantik error alih-alih warna asing.
- **Shape language baru** — `CalmRetroShapes` (BARU): dulu jatuh ke branch `else` di
  `AudioPlayerTheme()` → warisan `AppleShapes` (generous superellipse-like curve, `Radius.ml/
  xxl/hero`). Sekarang sudut sendiri, PALING mepet dari 4 identitas (`Radius.xs/sm/md`) —
  selaras bacaan "Lo-Fi Sci-Fi teduh"/minimalis spec markdown (bukan literal dari tabel HEX,
  ekstrapolasi desain yg didokumentasikan sebagai keputusan, bukan tebakan diam-diam). Dipakai
  di seluruh Card/Sheet/NavigationBar M3 lewat role `shapes` MaterialTheme — 1 titik ganti,
  blast radius terkendali (persis pola TactileShapes/SkeuDarkShapes yang sudah ada).
- Tombol play/pause (`NowPlayingScreen.kt`/`MiniPlayerBar.kt`) **SENGAJA tidak diubah** —
  `playPauseShape` sudah `CircleShape` utk identitas ini (branch `else`, sama seperti Apple),
  dan itu justru BENAR sesuai spec markdown eksplisit (`.calm-play-button { border-radius:
  50%; }`) — shape sendiri yang baru cuma berlaku ke Card/Sheet/dst, bukan CTA sirkular ini.
- Typografi **SENGAJA tidak diubah** — masih reuse `AppleTypography` (branch `else`), pola
  sama seperti Skeu (Batch 57: "no separate type-scale spec supplied ... identity here is
  carried by color/shape/bevel, not custom type") — spec markdown user juga tidak memberi
  spesifikasi tipografi, jadi reuse di sini BUKAN kebocoran identitas, beda kasus dari
  tertiary/error/shape di atas yang memang py sumber sendiri di palet.

0 protected asset, 0 file baru, 1 file diedit (scope sengaja sekecil mungkin — cuma
`Theme.kt`, tidak ada perubahan di `Color.kt` karena token yang dipakai sudah ada dari Batch
128-129). **Belum diverifikasi compile Gradle sungguhan** (tidak ada JDK/SDK di sandbox).

## Batch 129 — Calm Retro: efek aberrasi CTA play/pause (5 file)
Lanjutan Batch 128 — item kandidat (a) "efek aberration CSS", diminta lanjut user dengan
instruksi eksplisit "gak usah overthinking & greedy" (jadi discoped ke 1 titik CTA saja, bukan
disebar ke semua tombol app).

`Color.kt` — 2 token baru: `CalmRetroAberrationLeft` (#A87C8F Dusty Rose),
`CalmRetroAberrationRight` (#7C96A8 Dusty Denim), literal dari tabel HEX spec.

`Theme.kt` — `isCalmRetroTheme()`, pola sama persis `isTactileTheme()`/`isSkeuTheme()`
(bandingkan `colorScheme.primary`).

`TactileDepth.kt` — `calmAberration()` (fungsi BARU, akhir file, 0 fungsi lain disentuh):
terjemahan Compose dari CSS `box-shadow` ganda spec (`.calm-play-button`) — Compose tidak
punya colored box-shadow native, didekati 2 radial-gradient lingkaran offset kiri-atas
(Rose)/kanan-bawah (Denim) fade ke transparent, alpha 0.35f (dalam rentang 30%-40% yang
diminta eksplisit spec §"Panduan Desain Penting"). Pola "hand-drawn bukan bitmap" sama
seperti `tactileEmboss()`/`skeuEmboss()` yang sudah ada.

`NowPlayingScreen.kt`/`MiniPlayerBar.kt` (diedit) — `isCalmRetro` di-hoist sejajar
`isTactile`/`isSkeu` yang sudah ada, ditambahkan sebagai branch baru di `.then(when {...})`
tombol play/pause utama KEDUA tempat (full player + mini bar) — konsisten dengan pola Batch
55/58 yang selalu sinkronkan kedua lokasi ini tiap identitas baru dapat treatment CTA
(pelajaran Batch 58: identitas yang cuma dapat treatment di 1 lokasi dianggap bug/gap, bukan
fitur selesai). `bias` diperkecil di mini bar (2.dp vs 3.dp default) mengikuti ukuran tombol
yang lebih kecil.

**Sengaja tidak dikerjakan** (tetap discoped sesuai instruksi): tombol lain di luar CTA
play/pause utama (chip, IconButton sekunder, dst — di luar cakupan ".calm-play-button" spec),
varian pressed-state khusus (spec CSS `:hover` tidak relevan di touch/Compose, ditinggalkan).
0 protected asset, 0 file baru selain penambahan fungsi di file yang sudah ada, 5 file diedit.
**Belum diverifikasi compile Gradle sungguhan** (tidak ada JDK/SDK di sandbox).

## Batch 128 — Tema baru: Calm Retro (terkunci gelap, 2 file diedit)
Identitas tema ke-4, dari `palet_warna_calm_retro_v2.md` (user upload). **Terkunci gelap
permanen atas instruksi eksplisit user** ("gak bisa geser mode seenaknya") — beda dari
Tactile/Skeu (Batch 61, otonom di kedua mode): Calm Retro cuma 1 colorScheme, `colorsFor()`
mengabaikan param `isDark` untuk identity ini. Toggle Mode Terang/Gelap tetap ada di
Settings tapi tidak berefek visual saat Calm Retro aktif (behavior sama seperti Tactile/Skeu
versi PRA-Batch-61).

`Color.kt` — 6 token literal dari tabel HEX spec: `CalmRetroBackground` (#0F1015 Midnight
Dust), `CalmRetroSurface` (#161822 Obsidian Gray), `CalmRetroBorder` (#232635, dari border
`.song-card` CSS spec — dipakai `outline`/`surfaceVariant`), `CalmRetroText` (#E2E4E9 Silk
White), `CalmRetroSecondaryText` (#6A6F82 Slate Mist), `CalmRetroAccent` (#7FA99B Muted
Sage). Spec tidak beri token sukses/error sendiri — reuse `SkeuDarkSuccess`/`SkeuDarkError`
yang sudah ada, sesuai instruksi "gak usah greedy" (tanpa nambah token baru yang tak perlu).

`Theme.kt` — `ThemeIdentity.CALM_RETRO` (storageKey `"calm_retro"`) masuk daftar
`ThemeIdentity.entries` (picker Settings otomatis menampilkannya, 0 edit di
`SettingsScreen.kt`). `CalmRetroColors` (`darkColorScheme` tunggal) + case baru di
`colorsFor()`. Shape & typografi reuse `AppleShapes`/`AppleTypography` (branch `else` yang
sudah ada, 0 edit tambahan) — spec tidak menuntut bentuk/huruf custom.

**Sengaja tidak dikerjakan** (instruksi "gak usah overthinking"): efek chromatic-aberration
dual-shadow dari CSS contoh spec (`.calm-play-button`) — itu contoh implementasi CSS
opsional, bukan bagian konfigurasi warna inti; tidak dibuatkan primitive Compose baru
(beda dari `tactileEmboss()`/`skeuEmboss()`), tidak ada varian LIGHT (identity ini memang
1 warna saja by design). 0 protected asset disentuh, 0 file baru, 2 file kode diedit.
**Belum diverifikasi compile Gradle sungguhan** (tidak ada JDK/SDK di sandbox).

## Batch 127 — Micro UI/UX: bounce-press ke tombol sekunder frekuensi-tinggi (3 file)
Lanjutan kategori #4 — kali ini tombol **sekunder** (bukan CTA utama) yang paling sering
ditekan berulang dalam 1 sesi, prioritas sama seperti alasan "Tandai Sekarang" Batch 124:

- `LyricsSheet.kt` — "Mundur" & "Lewati Baris" (2 `TextButton` di flow tap-to-sync, ditekan
  bergantian dengan "Tandai Sekarang" tiap baris lirik — sengaja **tidak** termasuk "Batal,
  Kembali ke Teks" karena itu aksi keluar sekali pakai, bukan repetitive-tap).
- `VaultSheet.kt` — `IconButton` "Keluarkan dari vault" per baris lagu (ditekan berulang saat
  kosongkan vault berisi banyak lagu).
- `ABRepeatBookmarkSheet.kt` — `IconButton` hapus bookmark per baris (ditekan berulang saat
  bersih-bersih daftar bookmark).

3 file kode diedit, 0 file baru, 0 protected asset, 0 perubahan logika/behavior. `pressedScale`
disesuaikan per ukuran kontrol (0.9 untuk TextButton, 0.8 untuk IconButton kecil — makin kecil
kontrolnya, makin terasa efeknya di scale sama, jadi dikecilkan sedikit). Brace/paren tiap file
dicek otomatis & seimbang. **Belum diverifikasi compile Gradle sungguhan** (tidak ada JDK/SDK
di sandbox).

## Batch 126 — Micro UI/UX: tuntaskan bounce-press ke FilterChip (Equalizer + Ringtone Cutter)
Lanjutan kategori #4, giliran `FilterChip` (bukan `Button`/`OutlinedButton` seperti Batch
124-125) — pola sama (`interactionSource` + `Modifier.bouncyPress(...)`, `pressedScale = 0.92f`
sedikit lebih halus dari default 0.88f karena chip lebih kecil dari button penuh).

`EqualizerSheet.kt` — 2 `LazyRow` chip (Preset Kuat 4 chip + Preset Bawaan Perangkat N chip,
paling sering ditekan berulang saat eksplor equalizer). `RingtoneCutterSheet.kt` —
`DestinationChip` (composable bersama, 1 edit → berlaku ke 3 chip tujuan Nada
Dering/Notifikasi/Alarm sekaligus, pola sama `AbPointButton`/`ApkPickerRow` Batch 125).

2 file kode diedit, 0 file baru, 0 protected asset, 0 perubahan logika/behavior. Dengan ini,
**sub-bagian bounce-press untuk CTA & chip utama** di kategori #4 selesai (9 sheet total sejak
Batch 124) — tombol sekunder & hit-target size audit masih belum, `MICRO_UIUX_AUDIT.md`
diupdate mencerminkan ini (bukan ✅ penuh kategori #4, cuma sub-bagian). Brace/paren tiap file
dicek otomatis & seimbang. **Belum diverifikasi compile Gradle sungguhan** (tidak ada JDK/SDK
di sandbox).

## Batch 125 — Micro UI/UX: adopsi MICRO_UIUX_AUDIT.md + lanjut bounce-press ke 4 sheet lagi
User upload `MICRO_UIUX_AUDIT.md` — checklist 14 kategori polish presentation-only (strings,
spacing, typography, touch target, interactive states, dst.), scope eksplisit **dilarang**
sentuh logic/playback/queue/SAF/database/navigasi. Disimpan sebagai dokumen tracking persisten
(pola sama `ROADMAP_15_FITUR_OFFLINE.md`), status per kategori dicatat di bagian atas file
supaya tidak perlu scroll untuk lihat progres terbaru (pelajaran dari Batch 123).

Eksekusi kategori #4 (Touch Target & Micro Interaction) — lanjutan Batch 124, kali ini 4 sheet
yang sebelumnya belum diaudit (item "kandidat batch berikutnya" di `PROJECT_STATE.md` Batch 124):
- `BackupRestoreSheet.kt` — "Buat Backup Sekarang" + "Pulihkan dari File"
- `DuplicateFinderSheet.kt` — "Hapus N Terpilih" (destructive action)
- `ABRepeatBookmarkSheet.kt` — `AbPointButton` composable bersama (1 edit → berlaku ke tombol
  Titik A & Titik B sekaligus)
- `SignatureMatcherSheet.kt` — `ApkPickerRow` composable bersama (1 edit → berlaku ke picker
  APK 1 & APK 2 sekaligus)

4 file kode diedit (2 lewat composable bersama = 4 tombol efektif tersentuh), 1 file dokumentasi
baru (`MICRO_UIUX_AUDIT.md`), 0 protected asset, 0 perubahan logika/behavior — pola identik
Batch 124 (`interactionSource` + `Modifier.bouncyPress(...)` ke `Button`/`OutlinedButton` yang
sudah ada). **Sengaja tidak** disentuh batch ini: `EqualizerSheet` (FilterChip, bukan Button —
pola beda, giliran berikutnya), tombol sekunder (TextButton/IconButton) di semua sheet — tetap
disiplin "1 slice kecil per batch" sesuai arahan user. `FILE_MANIFEST.txt` 166→167. Brace/paren
tiap file dicek otomatis & seimbang. **Belum diverifikasi compile Gradle sungguhan** (tidak ada
JDK/SDK di sandbox).

## Batch 124 — Micro UI/UX: samakan bounce-press ke 4 sheet fitur terbaru
Audit `bouncyPress` (util tekan-mengecil-spring-balik, `Utils.kt`) di seluruh `ui/`: cuma
dipakai di `MiniPlayerBar.kt`/`NowPlayingScreen.kt`/`LockScreen.kt` (kontrol lama). 4 sheet
fitur terbaru (Batch 118-121) — dibangun cepat sebagai MVP fungsional — masih pakai `Button`
polos Material3 tanpa micro-feedback ini. Scope batch ini: **cuma CTA utama tiap sheet**
(bukan sapuan semua tombol, supaya tetap kecil & fokus):
- `SongInfoEditSheet.kt` — "Simpan"
- `RingtoneCutterSheet.kt` — "Potong & Simpan"
- `VaultSheet.kt` — "Aktifkan Vault" + "Buka" (unlock PIN)
- `LyricsSheet.kt` — "Tandai Sekarang" (tombol tap-to-sync, paling sering ditekan berulang saat
  lagu diputar — prioritas tertinggi untuk feedback taktil)

4 file kode diedit, 0 file baru, 0 protected asset, 0 perubahan logika/behavior — murni
`interactionSource` + `Modifier.bouncyPress(...)` ditambahkan ke `Button` yang sudah ada,
pola identik yang sudah dipakai di kontrol lama. Sisa tombol sekunder (TextButton batal/undo/
skip, IconButton hapus) sengaja **tidak** disentuh batch ini — bukan CTA utama, giliran
berikutnya kalau diminta lagi. **Belum diverifikasi compile Gradle sungguhan** (tidak ada
JDK/SDK di sandbox) — brace/paren tiap file dicek otomatis & seimbang.

## Batch 123 — Dokumentasi: sinkronkan callout "Update terbaru" (0 file kode, 1 file dokumentasi diedit)
Audit rutin urutan dokumentasi (pola sama Batch 94): `CHANGELOG.md` & `PROJECT_STATE.md` dicek
ulang urut newest-first — **0 anomali**, keduanya sudah benar. Sumber laporan user ternyata
callout "🆕 Update terbaru" di `README.md`, yang **wajib disinkronkan manual** tiap batch (lihat
Batch 94) — masih menunjuk Batch 121 padahal Batch 122 (fix build) sudah selesai, terlewat
karena Batch 122 murni fix build (0 file dokumentasi disentuh di batch itu). Fix: banner
diperbarui menunjuk Batch 122, tetap kredit fitur Ringtone Cutter dari Batch 121. 0 protected
asset, 0 file kode disentuh.

## Batch 122 — Fix Build: Ringtone Cutter
CI GitHub Actions (`log_fail_176.zip`) melaporkan `compileDebugKotlin`/`compileReleaseKotlin`
gagal: `RingtoneEncoder.kt:142` — `AppLogger.i(...)` dipanggil padahal `AppLogger` (Batch 121,
cek ulang di batch ini) cuma punya `e()`/`w()`, tidak ada method `i()`. Perbaikan: 1 baris,
`AppLogger.i` → `AppLogger.w` (level info-like terdekat yang memang ada). Dicek `grep` — 0 sisa
pemanggilan `AppLogger.i(` di seluruh codebase. 0 file lain tersentuh, 0 protected asset.
**Belum diverifikasi compile Gradle sungguhan ulang** (tetap tidak ada JDK/SDK di sandbox) —
prioritas berikutnya kalau user push: `./gradlew assembleDebug`/`compileReleaseKotlin` harus
hijau kali ini.

## Batch 121 — Roadmap #5: Ringtone Cutter
Item roadmap berikutnya berdasar tabel prioritas effort/risiko (Sedang/Sedang, terendah yang
masih tersisa setelah #1/#15/#13). 0 protected asset selain `MainActivity.kt` (edit parsial).

**`RingtoneCutter.kt`** (baru, `data/`) — logika murni (0 Context, pola sama `AbRepeatLogic`):
`TrimRange` data class + `clampRange()` (jepit start/end ke batas lagu, durasi min 1 detik/maks
60 detik — end diprioritaskan digeser dulu sebelum start supaya titik masuk yang user pilih
tetap dihormati) + `isValid()` + `formatTimestamp()` (`mm:ss`). `RingtoneCutterTest.kt` — 10 test
murni termasuk edge case lagu lebih pendek dari durasi minimum.

**`RingtoneEncoder.kt`** (baru, `data/`) — orkestrasi Context-based, pola scope-narrowing sama
`TagEditor`: (1) lagu MediaStore saja (bukan folder tambahan/SAF), (2) sumber MP3/AAC-M4A saja
(2 format yang aman di-`MediaMuxer`-copy tanpa re-encode di `minSdk` 23 app ini — FLAC/OGG/WAV
ditolak dengan pesan jujur), (3) simpan API 29+ saja. Potong pakai `MediaExtractor` (seek ke
titik mulai, baca sample sampai titik akhir) + `MediaMuxer` (`MUXER_OUTPUT_MPEG_4`, stream-copy
tanpa decode/encode ulang — kualitas audio identik sumber, presentationTimeUs direbase ke 0).
Hasil disimpan sebagai file BARU ke `Ringtones|Notifications|Alarms/AudioPlayer` via MediaStore
`RELATIVE_PATH` (pola sama `BackupManager`/`AppLogger`) dengan flag
`IS_RINGTONE`/`IS_NOTIFICATION`/`IS_ALARM` — **tidak pernah menulis balik ke file asli**, jadi
0 alur consent (`createWriteRequest`/`RecoverableSecurityException`) dibutuhkan sama sekali,
beda dari `TagEditor`. `WRITE_SETTINGS`/`setActualDefaultRingtoneUri` SENGAJA tidak dikerjakan
(izin sensitif) — fallback "tersimpan, pilih manual di Pengaturan > Suara" (flag MediaStore di
atas membuat file otomatis muncul di pemilih nada dering bawaan Android, jadi fallback ini tetap
mulus, bukan jalan buntu).

**`RingtoneCutterSheet.kt`** (baru, `ui/`) — 2 `Slider` terpisah (awal/akhir, bukan
`RangeSlider` — tidak ada precedent komponen itu di codebase) + 3 `FilterChip` tujuan simpan +
tombol "Potong & Simpan" (disabled kalau rentang tidak valid). **MVP disengaja**: 0 preview audio
langsung dari sheet ini (butuh player kedua di luar sesi putar utama) — user dengar hasil dari
file yang sudah tersimpan lewat player lain, kelas keterbatasan sama seperti MVP `VaultSheet`
(Batch 119).

**`NowPlayingScreen.kt`** (diedit) — 1 menu baru "Potong Nada Dering" di `AdvancedControlsSheet`
(pola sama "Edit Info Lagu"), state `showRingtoneCutterSheet` di-key ke `song.id` implisit lewat
guard `song != null`. **`PlayerViewModel.kt`** (diedit) — `requestCutRingtone()` fire-and-forget,
pakai kanal `infoMessage`/`actionErrorMessage` yang sudah ada, 0 kanal baru. **`MainActivity.kt`**
(diedit, **protected asset — edit parsial**) — 1 param baru (`onCutRingtone`) di pemanggilan
`NowPlayingScreen(...)` yang sudah ada.

7 file (4 baru + 3 diedit), 0 protected asset lain selain `MainActivity.kt` (edit parsial).
Brace/paren semua file dicek otomatis & seimbang. `FILE_MANIFEST.txt` 162→166 +
`ROADMAP_15_FITUR_OFFLINE.md` (#5 ditandai selesai) + `README.md` sebelum repack.

**Belum diverifikasi compile/runtime Gradle sungguhan** (tidak ada JDK/Android SDK di sandbox
ini) — prioritas berikutnya kalau user push: (1) `./gradlew assembleDebug` build bersih —
`MediaMuxer`/`MediaExtractor` API paling berisiko salah ketik manual tanpa cek compiler, (2) di
device: potong 1 lagu MP3 & 1 lagu M4A, pastikan file baru muncul di Pengaturan > Suara > Nada
Dering (bukan cuma di file manager), (3) putar hasil potongan di app lain untuk pastikan tidak
korup/silent (stream-copy tanpa re-encode BISA gagal kalau track pertama yang ditemukan
`MediaExtractor` bukan trek audio yang diharapkan pada file eksotis), (4) coba lagu FLAC/WAV,
pastikan pesan "belum didukung" muncul jelas bukan macet/crash, (5) coba di Android 9 ke bawah,
pastikan pesan "butuh Android 10 ke atas" muncul (bukan crash `IllegalArgumentException` dari
`RELATIVE_PATH` yang tidak dikenal API lama). Detail lengkap: `CHANGELOG.md` Batch 121 (entri
ini sendiri).

## Batch 120 — Roadmap #3: Editor Lirik LRC Tap-to-Sync
Item roadmap berikutnya setelah #14 (Batch 119) — dipilih karena reuse infrastruktur lirik yang
sudah ada penuh (`LyricsStore`/`LyricsParser`/highlight-scroll di `LyricsSheet.kt`), 0 dependency
baru, 0 protected asset, cocok untuk sandbox tanpa compiler.

**`LrcSyncEditor.kt`** (baru, `data/`) — logika murni (0 Context/Android, pola sama
`AbRepeatLogic`/`SmartPlaylistEngine`): `SyncSession` data class immutable (`lines`,
`timestamps: List<Long?>`, `currentIndex`), transisi lewat `mark()`/`skip()`/`undo()` yang
masing-masing return instance baru (bukan mutasi in-place — UI cukup `var session by remember`
+ reassign). `mark()` stempel baris saat ini dengan posisi playback lalu maju; `skip()` maju
tanpa stempel (baris tetap disimpan sbg plain text di hasil akhir, bukan dihapus/dipaksa dapat
timestamp — pilihan disengaja, bukan gap); `undo()` mundur 1 baris & hapus stempelnya kalau ada
(undo mark ATAU undo skip, efeknya sama — tidak perlu 2 fungsi terpisah). `formatTimestamp()`
`[mm:ss.xx]` (centisecond 2-digit, konsisten dengan format yang sudah dibaca `LyricsParser`
sejak lama). `buildLrcText()` gabung `lines`+`timestamps` jadi 1 teks siap simpan — baris yang
di-skip tetap plain (tanpa prefix `[...]`), baris yang ditandai dapat prefix — hasilnya **boleh
campur** (sebagian synced sebagian tidak), `LyricsParser.isSynced()` otomatis `false` untuk itu
(disengaja: skip berarti user memang tidak mau baris itu ikut auto-scroll-highlight, bukan bug).

**`LyricsSheet.kt`** (diedit) — 2 parameter baru dengan default aman (`isPlaying: Boolean =
false`, `onPlayPause: () -> Unit = {}` — 0 call site lama selain `NowPlayingScreen.kt` yang
perlu diubah, sudah dicek `grep`). Mode edit teks lama (`OutlinedTextField`) TIDAK diganti,
cuma ditambah 1 tombol "Mode Tap-to-Sync (LRC)" (muncul kalau draft punya minimal 1 baris
non-blank) yang membuka `SyncSession` baru dari `LrcSyncEditor.startSession(draft)`. Flow sync:
1 baris besar ditampilkan per giliran + tombol play/pause inline (`onPlayPause`, reuse
controller yang sama dgn transport utama — sengaja BUKAN tombol play terpisah/palsu) + posisi
berjalan (`formatDuration(positionMs)`, reuse util yang sudah ada di `Utils.kt`, sama package
`ui` jadi 0 import baru) + tombol besar "Tandai Sekarang" (`LrcSyncEditor.mark`) + baris
"Mundur"/"Lewati Baris" + "Batal, Kembali ke Teks" (`syncSession = null`, draft lama tidak
hilang). Begitu `session.isComplete`, `draft` langsung diisi `buildLrcText()` dan sesi ditutup
otomatis — balik ke tampilan `OutlinedTextField` biasa untuk REVIEW manual sebelum tap "Simpan"
(bukan auto-save — user tetap tahan kendali penuh, konsisten pola "jangan overwrite destruktif
tanpa konfirmasi" proyek ini, lihat `BackupManager.readAndValidate()`/`applyBackup()` Batch 115).
`syncSession` di-key ke `rawLyrics` sama seperti `editing`/`draft` (Batch 82) — ganti lagu
selagi sheet terbuka membatalkan sesi sync yang sedang berjalan, bukan bug (timestamp yang
sedang direkam scoped ke lagu yang diputar saat itu, tidak masuk akal dilanjut ke lagu lain).

**`NowPlayingScreen.kt`** (diedit) — 1 titik panggil `LyricsSheet(...)` yang sudah ada dapat 2
baris baru (`isPlaying = uiState.isPlaying`, `onPlayPause = onPlayPause`) — keduanya reuse
state/callback yang sudah ada di scope composable ini sejak lama (dipakai transport button Now
Playing), 0 parameter baru ke `NowPlayingScreen` itu sendiri, jadi **0 baris `MainActivity.kt`
disentuh batch ini** (bukan protected asset yang perlu diedit sama sekali).

**`LrcSyncEditorTest.kt`** (baru, `test/`) — 10 test pure-logic: split baris blank/trim,
mark/skip/undo termasuk kasus tepi (index 0, sesi sudah complete), format timestamp (termasuk
clamp negatif & menit 2-digit di atas 9), dan 1 test round-trip penuh (`buildLrcText` output
di-parse ulang lewat `LyricsParser.parse()` yang sudah ada, verifikasi baris campur
synced+skip dibaca benar dan `isSynced()` mengembalikan `false` seperti yang diharapkan).

**Batasan jujur, disengaja**: kalau draft yang dijadikan sesi sync sudah punya sebagian baris
ber-`[mm:ss.xx]` (bukan murni plain), `startSession()` tetap memperlakukan seluruh baris teks
apa adanya (prefix lama ikut jadi bagian teks baris, bukan di-strip) — MVP ini ditargetkan untuk
lirik plain-text yang belum pernah disinkronkan sama sekali, bukan re-sync sebagian lirik yang
sudah campur. Kalau user butuh itu, alur saat ini: edit manual dulu di text field untuk buang
prefix lama, baru masuk Mode Tap-to-Sync. 2 file kode baru + 2 diedit, 0 protected asset, brace/
paren `LyricsSheet.kt` (60/60, 146/146) & `NowPlayingScreen.kt` (204/204, 681/681) dicek otomatis
& seimbang. `FILE_MANIFEST.txt` 160→162 + `README.md` (1 baris fitur + banner) +
`ROADMAP_15_FITUR_OFFLINE.md` (#3 ditandai selesai; sekalian #6 & #7 yang sudah lama selesai
lewat Gap List Batch 117/115 tapi belum pernah ditandai di file roadmap ini — kelalaian
housekeeping lama, dibetulkan sekalian karena ditemukan saat audit item berikutnya, bukan kerja
tambahan yang disengaja dicari-cari). **Belum diverifikasi compile/runtime Gradle sungguhan**
(tidak ada JDK/Android SDK di sandbox ini) — prioritas berikutnya kalau user push: (1)
`./gradlew testDebugUnitTest` (10 test baru hijau), (2) di device: tempel lirik plain 3-4 baris,
masuk Mode Tap-to-Sync, putar lagu, tekan Tandai Sekarang per baris sambil lagu jalan, pastikan
timestamp yang tersimpan genuinely dekat dengan posisi saat tombol ditekan (bukan telat/gesekan
render), (3) tekan Lewati di 1 baris, pastikan baris itu tetap muncul plain (tanpa timestamp) di
hasil akhir dan tidak mengacaukan highlight baris lain, (4) Mundur setelah Tandai, pastikan
kembali ke baris sebelumnya dan stempelnya genuinely terhapus (coba Tandai ulang, timestamp baru
bukan yang lama), (5) tutup sheet di tengah sesi sync lalu buka lagi — pastikan mulai dari teks
draft lama (bukan crash/nyangkut di state sync). Detail lengkap: lihat file ini.

## Batch 119 — Roadmap #14: Vault Lagu Privat (PIN-gated song vault)
Item "Sangat disarankan" berikutnya dari `ROADMAP_15_FITUR_OFFLINE.md` — Sedang/Rendah risiko,
infrastruktur PIN (`AppLockStore`/`PinLockoutPolicy`) dan pola filter-tampilan
(`LibraryFilterStore`) sudah ada tinggal diikuti polanya, sesuai catatan roadmap sendiri.

**`VaultStore.kt`** (baru, `data/`) — PIN mgmt INDEPENDEN dari `AppLockStore` (prefs `vault`
sendiri, hash/salt/fail-count/lockout sendiri) — bukan reuse `AppLockStore` dengan prefs name
beda, supaya app-lock dan vault-lock 100% tidak bisa saling kontaminasi state, dan supaya user
bisa punya app tidak terkunci tapi lagu tertentu tetap terkunci (atau sebaliknya) tanpa
keduanya saling terikat. Formula lockout escalating TETAP dipakai bersama lewat
`PinLockoutPolicy` (sudah `internal object` murni Context-free, memang dibuat untuk reuse ini),
cuma plumbing hash/storage-nya yang diduplikasi (~15 baris). Juga simpan `Set<Long>` ID lagu
yang divault + `pruneOrphans(validIds)` (pola sama persis `FavoritesStore`/`RatingStore`, Gap
List #9) + `apply(songs)` — one-liner exclude vaulted, dirantai di call site yang sama seperti
`LibraryFilterStore.apply()` sudah dipasang.

**`VaultSheet.kt`** (baru, `ui/`) — 3 state: belum ada PIN (form setup 6 digit + konfirmasi,
pola sama `SetPinDialog` di `SettingsScreen.kt`) → ada PIN tapi belum unlock sesi ini (form PIN
+ countdown lockout live via `LaunchedEffect`+`delay(1000)`, pola sama `LockScreen.kt`) → sudah
unlock (list lagu vaulted + tombol "Keluarkan" per lagu, tombol "Tambah" buka dialog picker
cari-lalu-tap dari SELURUH lagu library yang belum divault, tombol "Nonaktifkan Vault" dengan
`AlertDialog` konfirmasi eksplisit — jelas menyebut jumlah lagu yang akan kembali normal).
State unlock sengaja session-only (`remember` biasa, bukan disimpan) — sheet dibuang & PIN
diminta lagi tiap kali dibuka ulang, konsisten dengan "vault" yang namanya memang untuk dibuka-
tutup sengaja, bukan status permanen. **MVP disengaja, dicatat jujur**: sheet ini murni
manajemen keanggotaan (tambah/keluarkan), TIDAK ada tombol putar langsung dari sini — memutar
lagu yang divault berarti mengeluarkannya dulu. Menahan scope ini menghindari perlu
menyambungkan sheet ke `MediaController`/`PlayerViewModel` penuh di batch pertamanya; kandidat
polish lanjutan kalau diminta.

**`HomeScreen.kt`/`LibraryScreen.kt`** (diedit) — `VaultStore(context).apply(...)` dirantai
SETELAH `LibraryFilterStore(context).apply(rawSongs)` yang sudah ada di kedua titik filter
utama (1 baris tiap file). Sengaja TIDAK menyentuh `LibraryFilterStore.kt` sama sekali —
`LibraryFilterStoreTest.kt` yang sudah ada (4 test) tetap valid tanpa perlu ditinjau ulang,
dan 2 store tetap independen (vault bisa nonaktif total tanpa mempengaruhi hidden-folder/
hidden-song sama sekali). **Batasan jujur**: Vault dikelola dari Settings, bukan dari
Home/Library — perubahan keanggotaan vault baru tercermin di kedua layar itu begitu
`remember(rawSongs, ...)`-nya re-run (navigasi ulang ke layar itu), bukan live sinkron seketika
selagi kedua layar itu tetap terbuka di background. Kelas keterbatasan yang sama sudah diterima
project ini untuk penulisan lintas-store lain (lihat Batch 115, Backup/Restore).

**`SettingsScreen.kt`** (diedit) — 1 row menu baru "Vault Lagu Privat" (pola identik "Deteksi
File Duplikat" tepat di atasnya) + render `VaultSheet`. **0 param baru** ke fungsi
`SettingsScreen(...)` — `songs: List<Song>` sudah ada dari Batch 117 (Duplicate Detection),
dipakai ulang apa adanya sebagai daftar kandidat lagu untuk ditambahkan ke vault.

**`PlayerViewModel.kt`** (diedit) — `vaultStore` field baru + `vaultStore.pruneOrphans(validIds)`
dipanggil di `refreshLibrary()` tepat di sebelah `favoritesStore.pruneOrphans`/
`ratingStore.pruneOrphans`/`playlistStore.pruneOrphans` yang sudah ada (Gap List #9 precedent).

**`README.md`** — 1 baris fitur baru ditambah ke daftar Fitur v1 (persis di bawah Tag Editor,
area privasi/manajemen lagu) + banner "Update terbaru" disinkronkan ke Batch 119 (sebelumnya
masih menunjuk Batch 100 — staleness lama pre-existing, cuma baris ini yang disentuh, bukan
audit penuh seluruh README).

**`ROADMAP_15_FITUR_OFFLINE.md`** — item #14 ditandai ✅ SELESAI + catatan implementasi + baris
tabel prioritas.

6 file kode disentuh (2 baru + 4 diedit), 0 protected asset. Brace/paren semua file kode dicek
otomatis & seimbang. `FILE_MANIFEST.txt` diperbarui (158→160, 2 file baru) sebelum repack, bukan
cuma dicek di folder kerja. **Belum diverifikasi compile/runtime Gradle sungguhan** (tidak ada
JDK/Android SDK/kotlinc di sandbox ini, konsisten sama semua batch sebelumnya) — prioritas
berikutnya kalau user push: (1) `./gradlew assembleDebug` build bersih, (2) di device: atur PIN
vault, tambah 1 lagu ke vault, konfirmasi lagu itu genuinely hilang dari Beranda & Library
(bukan cuma UI vault yang bilang begitu), (3) tutup app / buka ulang sheet Vault, pastikan PIN
diminta lagi (session-only by design, bukan bug kalau memang minta ulang), (4) coba PIN salah
5x berturut-turut, pastikan lockout & countdown-nya jalan sama seperti App Lock, (5) nonaktifkan
vault, pastikan SEMUA lagu yang tadi divault kembali normal di Beranda/Library tanpa perlu
restart app.

## Batch 118 — Gap List "Wajib" #1: Tag/Metadata Editor (MVP: MediaStore + MP3)
Item terakhir dari 4 "Wajib" yang REALISTIS dikerjakan di lingkungan kerja ini (Gradle Wrapper
butuh `gradle-wrapper.jar` biner, Release Lint Gate butuh baseline lint sungguhan — keduanya
butuh `gradle`/Android SDK terpasang, tidak ada di sandbox ini, sama seperti dicatat Batch 117).

**Scope sengaja dipersempit, 2 keputusan besar dicek dulu sebelum nulis kode (bukan ditebak):**
1. **Format: MP3/ID3v2.3 saja.** FLAC (Vorbis comment), OGG, M4A (atom `moov/udta/meta`), WMA
   masing-masing format biner TOTAL BEDA — nulis writer yang benar untuk semuanya sekaligus
   tanpa compiler/device sungguhan untuk verifikasi adalah risiko tinggi (file musik user bisa
   rusak). MP3 dipilih duluan: format paling umum + struktur paling sederhana untuk ditulis
   aman (tag selalu di awal file, byte audio sesudahnya tidak pernah didekode/disentuh).
2. **Sumber: lagu MediaStore saja, BUKAN lagu folder tambahan (SAF).** Dicek ulang ke
   `PlayerViewModel.addCustomFolder()`: folder tambahan cuma diberi
   `FLAG_GRANT_READ_URI_PERMISSION` saat ditambahkan (baca saja) — bukan diasumsikan, benar-benar
   dibaca dari kode. Menulis ke file SAF butuh alur izin tulis terpisah yang belum ada. Sengaja
   dicatat sebagai gap tersisa, bukan dipaksakan dengan asumsi izin yang keliru.

Kedua batasan itu ditampilkan APA ADANYA ke user di `SongInfoEditSheet` (pesan beda untuk tiap
alasan: "folder tambahan belum didukung" vs "format belum didukung, baru MP3") — bukan disembunyikan
atau digagalkan diam-diam.

**`Id3TagWriter.kt`** (baru, `data/`, 0 Context/Android — murni `InputStream`/`OutputStream`) —
`buildTag()` susun blok ID3v2.3 baru (frame teks UTF-16LE+BOM, konsisten untuk semua field
termasuk TRCK/TPOS numerik — encoding seragam = lebih sedikit percabangan/bug). `rewrite()`
baca 10 byte header lama untuk deteksi ukuran tag ID3v2 LAMA (syncsafe int di offset 6-9), lalu
alirkan tag baru + byte audio asli (byte-for-byte, tidak pernah didekode) ke output. ID3v1 (128
byte trailer di akhir file, kalau ada) SENGAJA tidak disentuh/dihapus — kosmetik minor (player
modern prioritaskan ID3v2 kalau keduanya ada), dicatat sebagai gap kosmetik, bukan dianggap
selesai.

**`TagEditor.kt`** (baru, `data/`) — orkestrasi Context/I/O. Alur consent Android 11+ pakai
`MediaStore.createWriteRequest()` (API resmi, pola identik `createDeleteRequest` yang sudah
dipakai `MainActivity.deleteSongsFromDevice`); Android 10 pakai pola resmi
`RecoverableSecurityException` (coba tulis dulu, tangkap exception-nya kalau app ini bukan
pemilik file). **Alur tulis fisik 2 langkah demi keamanan file user**: (1) tulis hasil rewrite
ke file SEMENTARA di cache app dulu — file asli 0% tersentuh kalau ada bug di langkah ini; (2)
baru salin isi file sementara itu ke `song.uri` asli (mode "rwt"). Risiko residual dicatat jujur
di komentar kode: langkah (2) tetap 1 operasi truncate+write ke file asli — kalau app di-kill
paksa PAS di tengah itu, file bisa berakhir TERPOTONG (bukan "rusak diam-diam dengan audio
salah" — kegagalan yang terdeteksi, bukan korupsi senyap). Android tidak punya primitif rename
atomik lintas provider yang bisa diandalkan untuk menghilangkan risiko ini sepenuhnya — TIDAK
diklaim 100% aman. Setelah sukses, `MediaScannerConnection.scanFile()` dipanggil supaya index
MediaStore (termasuk tabel Genres, Gap List #11) sinkron tanpa nunggu scan device berikutnya.

**`SongInfoEditSheet.kt`** (baru, `ui/`) — form edit (judul/artis/album/artis album/genre/
komposer/no. track/no. disc), field angka difilter `isDigit()` saja. Pesan "belum didukung"
dicerminkan APA ADANYA dari logika `TagEditor.editabilityCheck` (dua alasan berbeda, bukan 1
pesan generik) — TIDAK divalidasi ulang dengan logika terpisah yang berisiko beda dari sumber
kebenarannya, sheet tetap kirim `onSave` apa pun hasil pengecekannya sendiri, TagEditor yang
jadi otoritas final.

**`PlayerViewModel.kt`** (diedit) — `requestSaveTags()`/`onTagWriteConsentResult()` +
`pendingTagWriteConsent: StateFlow<IntentSender?>` (pola identik `pendingDeleteRequest` yang
sudah ada di `MainActivity` untuk hapus lagu). Sukses/gagal lewat kanal `infoMessage`/
`actionErrorMessage` yang sudah ada — 0 kanal Snackbar baru ditulis.

**`MainActivity.kt`** (diedit, **protected asset — edit parsial**) — `tagWriteConsentLauncher`
(pola identik `deleteRequestLauncher` tepat di atasnya) + `LaunchedEffect` observe
`pendingTagWriteConsent`, dan 1 baris param baru `onSaveSongTags` ke pemanggilan
`NowPlayingScreen(...)` yang sudah ada (dipakai via `nowPlayingContent` lambda Batch 101 — 1
titik edit, otomatis berlaku untuk mode Compact/Medium DAN panel Expanded).

**`NowPlayingScreen.kt`** (diedit) — 1 param baru `onSaveSongTags`, 1 baris menu baru "Edit Info
Lagu" (ikon `Edit`) di `AdvancedControlsSheet` (pola identik Visualizer/Equalizer/dst.), sheet
baru dipasang persis pola `LyricsSheet`/`ABRepeatBookmarkSheet` (key di `song.id`, supaya draft
tidak salah lagu kalau track ganti sementara sheet terbuka).

**`Id3TagWriterTest.kt`** (baru, `test/`) — murni logic biner (syncsafe encode/decode, frame
builder, `rewrite()` in-memory pakai `ByteArrayInputStream`/`ByteArrayOutputStream`) — 0 Context,
jalan tanpa Robolectric sama seperti test lain di project. `TagEditor.kt` sendiri (butuh
Context/ContentResolver) TIDAK diuji di layer ini — sama pembagian seperti
`MusicRepositoryTrackDiscTest` yang cuma uji helper parse murni, bukan `MusicRepository` yang
butuh cursor.

7 file kode (4 baru + 3 diedit) + `FILE_MANIFEST.txt`/dokumentasi. Brace/paren semua file kode
dicek otomatis & seimbang. `FILE_MANIFEST.txt` diverifikasi 100% match isi fisik (158/158, sort
diff bersih). 0 protected asset lain tersentuh selain `MainActivity.kt` (edit parsial, sesuai
aturan).

**Belum diverifikasi compile/runtime Gradle sungguhan** — prioritas berikutnya kalau user push:
(1) `./gradlew`/`gradle testDebugUnitTest` (pastikan `Id3TagWriterTest.kt` hijau — terutama
assert syncsafe & panjang frame, itu bagian paling gampang salah-hitung-manual), (2)
`assembleRelease`, (3) di device sungguhan: pilih lagu MP3 dari MediaStore, buka "Edit Info
Lagu", ubah judul, Simpan, **verifikasi dengan player LAIN (bukan app ini)** bahwa file benar
berubah — jangan cuma percaya UI app ini sendiri karena itu bisa saja cuma baca ulang state lama
yang di-refresh, bukan bukti file fisik benar tertulis, (4) uji lagu dari folder tambahan (SAF)
memang menampilkan pesan "belum didukung", bukan macet/crash, (5) uji lagu non-MP3 (kalau ada di
library test) sama halnya. Detail lengkap sudah di changelog entry ini sendiri (batch ini besar).

## Batch 117 — Gap List "Wajib" #2: Duplicate Detection
Audit ulang (`AudioPlayer_Coding_Gap_Updated.md`) menandai 4 item "Wajib" pasca gap list lama
tuntas: Tag/Metadata Editor, Duplicate Detection, Gradle Wrapper, Release Lint Gate. Duplicate
Detection dikerjakan lebih dulu karena scope-nya realistis untuk 1 batch tanpa dependency
binary/network — beda dengan Tag Editor (butuh penulisan tag native per format file, effort besar)
atau Gradle Wrapper (butuh `gradle-wrapper.jar` biner asli, TIDAK bisa dibuat sah dari lingkungan
kerja batch ini — tidak ada `gradle` terpasang maupun akses network, dicek eksplisit).

**`DuplicateDetector.kt`** (baru, `data/`) — object murni, 0 Context/I/O/Compose. 2 grouping
terpisah dengan alasan berbeda: `findLibraryDuplicates()` pakai signature identik
`PlayerViewModel.dedupeSignature()` (title+artist trim/lowercase + durasi dibulatkan ke detik) —
mendeteksi "kelihatan lagu yang sama" walau 2 entri adalah 2 file fisik berbeda (mis. rip ulang).
`findPhysicalDuplicates()` pakai heuristik (fileSize, durasi dibulatkan detik) — bukan hash
byte-per-byte (biaya I/O penuh per lagu saat scan, pola penghindaran yang sama dengan keputusan
codec/bitrate/genre sebelumnya), lagu `fileSize <= 0` dikecualikan karena tidak bisa dipakai
heuristik ini. Kedua fungsi HANYA mengembalikan grup (size >= 2) — tidak ada operasi hapus/merge
di file ini sama sekali, sesuai requirement eksplisit gap doc ("Jangan melakukan delete
otomatis").

**`DuplicateFinderSheet.kt`** (baru, `ui/`) — `ModalBottomSheet` (`fillMaxHeight(0.9f)`, bukan
sheet kecil — daftar grup bisa panjang di library besar) menampilkan 2 seksi dari
`DuplicateDetector`, tiap lagu punya `Checkbox` individual (default TIDAK tercentang), tombol
"Hapus N Terpilih" nonaktif kalau seleksi kosong. Tap tombol → `AlertDialog` konfirmasi eksplisit
menyebut jumlah file persis sebelum `onDeleteSongs(toDelete)` benar-benar dipanggil. **0 logic
delete baru** — `onDeleteSongs` murni diteruskan dari `MainActivity.deleteSongsFromDevice` yang
sudah ada (pola identik `onDeleteSongs` di `LibraryScreen.kt`), yang di Android 10+ tetap
memicu dialog konfirmasi sistem (`MediaStore.createDeleteRequest`/`RecoverableSecurityException`)
sebagai lapis kedua di luar kendali sheet ini.

**`SettingsScreen.kt`** (diedit) — row menu baru "Deteksi File Duplikat" persis di bawah
"Cadangkan & Pulihkan" (pola Row+Icon+Column identik), state `showDuplicateFinder`. Signature
`SettingsScreen(...)` dapat 2 param baru di posisi TERAKHIR dengan default value
(`songs: List<Song> = emptyList()`, `onDeleteSongs: (List<Song>) -> Unit = {}`) — default value
sengaja dipasang (bukan cuma nullable) supaya call site lain yang mungkin ada (test fixture)
tidak wajib diubah untuk tetap compile.

**`MainActivity.kt`** (diedit, protected asset — edit parsial 2 baris) — pemanggilan
`SettingsScreen(...)` yang sudah ada dapat `songs = librarySongs` (variable existing, sudah
dipakai `stats_dashboard` route) dan `onDeleteSongs = { deleteSongsFromDevice(it) }` (fungsi
existing, sudah dipakai `LibraryScreen`). 0 fungsi baru ditulis di file ini, 0 baris lain
tersentuh.

**Kenapa Gradle Wrapper (gap #3) TIDAK dikerjakan batch ini**: `gradlew`/`gradlew.bat` adalah
teks shell script yang bisa ditulis manual, tapi `gradle/wrapper/gradle-wrapper.jar` adalah
class file terkompilasi di dalam JAR — resminya cuma didapat dari task `gradle wrapper` (butuh
`gradle` terpasang) atau download `services.gradle.org`. Lingkungan kerja batch ini tidak
punya keduanya (dicek eksplisit: `which gradle` kosong, `curl` ke domain eksternal ditolak
egress proxy dengan `host_not_allowed`). Menulis wrapper dengan jar palsu/kosong akan membuat
`./gradlew` gagal total dengan pesan error yang justru membingungkan — skip terang-terangan
(didokumentasikan di sini + `PROJECT_STATE.md`) lebih aman daripada wrapper yang terlihat ada
tapi rusak. CI tetap jalan normal tanpa wrapper (pakai binary `gradle` dari
`gradle/actions/setup-gradle@v3`, workaround yang sudah ada sejak sebelum gap list ini ditulis
ulang — lihat komentar di `.github/workflows/build.yml`).

Brace/paren 4 file (2 baru + 2 diedit) dicek otomatis & seimbang. 0 protected asset lain
tersentuh (`AndroidManifest.xml`, `build.gradle.kts`, `settings.gradle.kts`, DB schema/DAO,
`.gitignore`, `.github/workflows/*` — semuanya 0 baris berubah). 2 file baru → `FILE_MANIFEST.txt`
diperbarui (2 baris ditambah, urutan alfabetis dalam folder masing-masing). **Belum diverifikasi
compile/runtime Gradle sungguhan** — prioritas berikutnya kalau user push: (1) build bersih
(`./gradlew` atau `gradle testDebugUnitTest assembleRelease`), (2) uji manual di Setelan →
"Deteksi File Duplikat": copy 1 file lagu ke 2 folder berbeda untuk memicu grup "Duplikat File
Fisik", pastikan grup "Duplikat Entri Library" juga muncul untuk lagu dengan title/artist/durasi
mirip, checkbox+tombol hapus berfungsi, dan dialog konfirmasi sistem Android 11+ tetap muncul
setelah konfirmasi in-app.

## Batch 116 — Gap List #11: Genre metadata first-class
Item "Sangat disarankan" kedua (lanjut Batch 115). Genre sebelumnya sengaja di-skip sejak
Batch 89 (SmartPlaylist) dengan alasan "query per-lagu / N+1" — dicek ulang batch ini: alasan
itu benar untuk pendekatan naif (query `Genres.Members` per lagu), tapi TIDAK berlaku kalau
dibalik jadi 1 map id→nama dibangun sekali per scan dari sisi `Genres` (query dibatasi jumlah
genre di device, bukan jumlah lagu).

**`MusicRepository.kt`** — `buildGenreMap()` baru: query `MediaStore.Audio.Genres` (semua
genre), lalu tiap genre query `Genres.Members.getContentUri(...)` (daftar `audio_id` anggotanya)
— hasilnya `Map<Long, String>` id lagu → nama genre, dibangun 1x di awal `querySongs()` lalu
dipakai lookup O(1) per baris cursor. Lagu yang (jarang) masuk >1 genre bucket dapat genre
terakhir yang ditemukan loop — disederhanakan jadi 1 field string, bukan list (gap list sendiri
menandai multi-genre sebagai "bila format memungkinkan", bukan wajib). **Kenapa bukan kolom
langsung di tabel Media** (pola track/disc/album-artist yang sudah ada): tidak ada kolom genre
polos di baris utama `MediaStore.Audio.Media` lintas API yang ditarget app ini — genre cuma ada
lewat tabel relasi terpisah, beda dari track/disc (API 30+ punya kolom string langsung) atau
album-artist/composer (kolom langsung sejak lama). Tidak ditebak dari ingatan — dicek dulu
sebelum tulis kode (pelajaran Batch 14/32/33/44 soal jangan menebak API Android tanpa
verifikasi).

**`CustomFolderScanner.kt`** — `METADATA_KEY_GENRE` dibaca dari `MediaMetadataRetriever` yang
sudah terbuka untuk title/artist/album/dst. (zero I/O tambahan, pola sama seperti albumArtist/
composer Batch 105).

**`Song.kt`** — field baru `genre: String? = null`, posisi terakhir constructor (tidak
mengubah 1 pun call site lama termasuk fixture test).

**`LibrarySearchIndex.kt`** — genre ditambahkan ke `searchableText` (blob null-separated yang
sama dengan title/artist) — "gunakan genre pada filtering/search" dari gap list, sisi
Perpustakaan. String kosong untuk lagu tanpa genre (bukan null-check di query time).

**`SmartPlaylist.kt`/`SmartPlaylistEngine.kt`** — kriteria baru `genre: String?`, EXACT match
case-insensitive (bukan substring seperti `keyword`) — semantik yang benar untuk nilai dari
picker chip (nilai genre asli di library), bukan teks bebas yang rawan typo. Lagu tanpa genre
(null) tidak pernah cocok dengan rule genre-bounded, sama seperti pola `year == 0` yang sudah
ada. `isEmpty()` diperluas ikut cek field baru.

**`SmartPlaylistScreen.kt`/`LibraryScreen.kt`** — param baru `availableGenres` (dihitung sekali
dari `rawSongs.mapNotNull { it.genre }.distinct().sorted()`, presenden persis sama dengan
`availableFolderNames` yang sudah ada) diteruskan ke builder sheet, dirender sebagai baris
`FilterChip` (tap-to-clear di chip yang sama, pola sama tombol rating bintang) tepat di bawah
chip folder. "Integrasikan genre dengan smart playlist" dari gap list — tuntas.

**README.md** — deskripsi Smart Playlist & catatan "belum selesai" genre diperbarui/dihapus.

8 file kode + 1 dokumentasi. Brace/paren dicek otomatis di semua file kode & seimbang. 0 file
baru (murni edit), 0 protected asset tersentuh. **Belum diverifikasi compile/runtime Gradle
sungguhan** (tidak ada JDK/Android SDK/kotlinc di sandbox ini) — prioritas berikutnya kalau
user push: `./gradlew testDebugUnitTest` (pastikan `SmartPlaylistEngineTest.kt` existing tetap
hijau dengan field baru), lalu build APK asli + cek di device (1) genre genuinely muncul untuk
lagu yang device-nya punya tag genre (banyak library musik nyata TIDAK diberi tag genre oleh
media scanner kalau file source-nya sendiri tidak punya frame genre ID3/Vorbis — jangan buru-
buru anggap bug kalau kosong, cek dulu file test punya tag genre atau tidak), (2) `buildGenreMap()`
tidak menambah lag terasa saat refresh library di device dengan genre count wajar, (3) chip
genre di Playlist Otomatis builder muncul & filter benar-benar exact-match (lagu genre lain
tidak ikut lolos).

## Batch 115 — Gap List #10: Backup/restore data lokal
Item "Sangat disarankan" pertama setelah 10 item "Wajib" P0/P1 (1-9) tuntas. Sebelum batch ini
tidak ada mekanisme apa pun untuk mengeluarkan data app (playlist, favorit, rating, dst.) ke
luar SharedPreferences privat — uninstall/ganti device berarti semua hilang total, tidak ada
jalan keluar.

**`BackupManager.kt` (baru, `data/`)** — bundel prefs yang di-whitelist jadi 1 file JSON,
ditulis ke `Documents/AudioPlayer/backups/backup_<timestamp>_<uuid>.json` lewat MediaStore
(API 29+, pola identik `AppLogger.exportLogToDocuments`, tanpa izin storage tambahan), FIFO
retensi 20 file (pola sama retensi log/crash). Whitelist 17 prefs: playlist, playlist otomatis,
favorit, rating, bookmark, mode audiobook, riwayat dengar, statistik putar, statistik jam
dengar, folder/lagu disembunyikan, tema, crossfade, lewati keheningan, kocok-utk-lewati, radio
otomatis, visualizer, mini player mengambang.

**Sengaja dikecualikan dari whitelist** (didokumentasikan di KDoc file, bukan kelupaan):
`app_lock` (PIN/lockout — data keamanan, tidak aman dimuat ulang dari file yang bisa disalin ke
device lain), `custom_folders` (URI SAF terikat device+install asal, restore mentah cuma
menghasilkan entri folder mati), `onboarding_hints` (state UI sekali-pakai), `search_history`
(nilai rendah, di luar scope), `sleep_timer` (state timer yang sedang berjalan, hampir pasti
sudah lewat kalau di-restore di sesi lain).

**Serialisasi tipe-aman**: `SharedPreferences` bisa berisi String/Int/Long/Float/Boolean/
`Set<String>` — JSON tidak membedakan Int/Long/Float secara native, jadi tiap value dibungkus
`{"type": ..., "value": ...}` eksplisit supaya round-trip export→import tidak diam-diam
mengubah tipe (mis. Int jadi Long, yang bisa bikin `ClassCastException` di pemanggil lama yang
masih pakai `prefs.getInt(...)`).

**Validasi sebelum overwrite (guard destruktif)**: `readAndValidate()` (parse + cek
`schemaVersion` dikenal, `null` kalau JSON rusak/format tidak dikenal) dipisah total dari
`applyBackup()` (eksekusi nyata) — di antara keduanya, UI wajib tampilkan ringkasan jumlah item
per kategori lewat `AlertDialog` dan user harus tap "Timpa & Pulihkan" secara eksplisit. Restore
per-prefs bersifat REPLACE penuh (clear lalu isi ulang) bukan merge — deterministik, hasil akhir
selalu sama persis isi file; prefs whitelist yang TIDAK ada di file (backup lama dari sebelum
sebuah fitur ada) sengaja tidak disentuh sama sekali.

**`BackupRestoreSheet.kt` (baru, `ui/`)** — tombol "Buat Backup Sekarang" + "Pulihkan dari
File" (SAF `ActivityResultContracts.OpenDocument()`, mime `application/json`). **Keputusan
arsitektur**: launcher SAF dideklarasikan LANGSUNG di sheet ini, bukan di-drilling dari
`MainActivity.kt` seperti launcher-launcher lain di proyek ini (`visualizerPermissionLauncher`,
`overlayPermissionLauncher`) — `rememberLauncherForActivityResult` cuma butuh
`ActivityResultRegistryOwner`, yang tersedia di seluruh pohon Compose Activity yang sama
termasuk di dalam `ModalBottomSheet`, jadi tidak ada alasan menambah parameter/launcher ke
`MainActivity.kt` (protected asset) untuk fitur yang lingkupnya murni 1 sheet — **0 baris
`MainActivity.kt` disentuh batch ini**. Banner hasil inline (pola sama `DiagnosticLogSheet`,
alasan sama: Snackbar `onInfoMessage` ketutup layer `ModalBottomSheet`).

**`SettingsScreen.kt` (diedit)** — 1 row menu baru "Cadangkan & Pulihkan" ditaruh di level
teratas (bukan di dalam submenu "Lanjutan" — ini fitur mainstream, bukan alat developer), di
antara "Statistik Dengar" dan divider "Lanjutan".

**Batasan jujur, sengaja BELUM digarap**: StateFlow yang sudah di-cache di memori
`PlayerViewModel` (favorit, playlist, dst.) TIDAK otomatis re-read dari SharedPreferences
begitu `applyBackup()` menimpanya langsung di layer data — restore berhasil ke disk, tapi UI
yang sedang terbuka bisa menampilkan data lama sampai app ditutup-buka ulang. Dialog konfirmasi
sudah eksplisit bilang ini ke user (bukan disembunyikan). Memaksa setiap StateFlow terkait
re-load dari `PlayerViewModel` butuh nambah 1 fungsi refresh generik lintas semua store yang
di-whitelist — di luar scope batch ini, kandidat polish lanjutan kalau user lapor kejadian nyata
di device. Belum diverifikasi compile/runtime Gradle sungguhan (tidak ada JDK/Android SDK di
sandbox ini) — prioritas berikutnya kalau user push: buat backup, uninstall+install ulang app
(atau `pm clear`), pulihkan dari file, pastikan playlist/favorit/rating benar-benar kembali
setelah app dibuka ulang.

## Batch 114 — Gap List #9: Library/database consistency
Audit checklist #9 terhadap arsitektur nyata app ini: tidak ada Room/SQL sama sekali — library
selalu live-query `MediaStore` (`MusicRepository.getAllSongs()`, fresh tiap panggilan, tidak ada
tabel lokal yang bisa basi) + folder tambahan via SAF (`CustomFolderScanner`, juga stateless per
scan), digabung dengan `dedupeSignature()` di `refreshLibrary()`. Konsekuensinya, 2 sub-item
checklist (**duplicate song record**, **rescan idempotent**) sudah aman by construction — tidak
ada state inkremental yang bisa drift, tiap refresh menghasilkan snapshot ulang dari nol. Dedup
SAF-vs-MediaStore sendiri sudah diverifikasi benar di Batch 106, dicek ulang di sini tanpa
perubahan.

Gap nyata ada di 2 sub-item lain: **"bersihkan item yang sudah dihapus"** dan **"pastikan
playlist/favorite/history tidak menunjuk entity yang sudah hilang"**. Sebelum batch ini, tidak
ada mekanisme apa pun untuk itu — `FavoritesStore`/`RatingStore`/`PlaylistStore` cuma pernah
ditulis/dibaca lewat aksi user langsung, tidak pernah divalidasi ulang terhadap library yang
sesungguhnya. Kalau sebuah file favorit dihapus/dipindah dari storage, ID-nya numpuk selamanya di
SharedPreferences — tidak salah secara fungsional (UI yang me-lookup by ID otomatis tidak
menampilkannya lagi), tapi tetap dead weight yang tidak pernah dibersihkan, dan `playlist.songIds.size`
jadi tidak lagi mencerminkan jumlah lagu yang benar-benar bisa diputar.

**3 file store (edit parsial, masing-masing 1 fungsi baru `pruneOrphans(validIds: Set<Long>)`)**:
1. **`FavoritesStore.kt`** — filter `getFavorites()` terhadap `validIds`, tulis ulang hanya kalau
   ukurannya berubah (no-op write kalau semua masih valid).
2. **`RatingStore.kt`** — beda pola penyimpanan dari Favorites (per-song key `rating_<id>`, bukan
   1 Set), jadi enumerasi `prefs.all.keys`, buang key yang prefix-nya cocok tapi ID-nya tidak ada
   di `validIds` atau gagal parse.
3. **`PlaylistStore.kt`** — filter `songIds` tiap playlist terhadap `validIds`, `save()` hanya
   kalau hasil filter beda dari sebelumnya (`Playlist` data class, perbandingan list by value).
   **Playlist itu sendiri TIDAK ikut terhapus** walau `songIds`-nya jadi kosong — nama yang user
   pilih sendiri, kosong bukan alasan untuk menghilangkannya tanpa izin eksplisit (konsisten
   dengan aturan "Strict Delete Guard").

**`PlayerViewModel.kt` (edit parsial)** — `refreshLibrary()`, tepat setelah
`_librarySongs.value = songs` di dalam guard `generation == libraryRefreshGeneration` yang sudah
ada (supaya prune selalu berjalan terhadap hasil scan TERBARU, bukan scan basi yang sudah
di-cancel): hitung `validIds` dari `songs.map { it.id }`, panggil ketiga `pruneOrphans()` di atas,
lalu `_playlists.value = playlistStore.getPlaylists()` supaya UI playlist ikut ter-refresh kalau
memang ada yang berubah.

**Sengaja TIDAK diterapkan ke `listeningHistoryStore`/`playStatsStore`**: keduanya catatan
historis ("pernah diputar tanggal X", dipakai fitur Kilas Balik/Stats Dashboard), bukan pointer
state-saat-ini seperti favorite/rating/playlist. Dangling ID di situ semantiknya wajar (riwayat
tetap riwayat walau filenya sudah tidak ada) dan aman — kalau user coba putar ulang dari Kilas
Balik untuk lagu yang sudah hilang, itu sudah tertangani rapi oleh kategorisasi error Batch 113,
bukan sesuatu yang perlu "dibersihkan" duluan.

Brace/paren ke-4 file dicek seimbang. **Belum diverifikasi compile/runtime Gradle sungguhan** —
tidak ada JDK/Android SDK di sandbox kerja. Verifikasi berikutnya (butuh push + build + device
fisik): favoritkan/beri rating/masukkan ke playlist sebuah lagu, hapus file-nya langsung dari
File Manager (bukan lewat app), buka lagi app / trigger refresh library, pastikan favorit &
rating lagu itu hilang dari daftar dan `songIds` playlist berkurang — tanpa playlist itu sendiri
ikut hilang.

## Batch 113 — Gap List #8: Playback error recovery
Checklist Gap List #8 diaudit terhadap `onPlayerError` (`PlayerViewModel.kt`). Sebelumnya: 1
pesan generik untuk SEMUA jenis error, auto-skip ke track berikut tanpa batas apa pun selama
`hasNextMediaItem()` true — aman untuk kasus umum (1-2 file rusak tersebar), tapi kalau seluruh
sisa queue bermasalah (folder sumber dicabut/dihapus total di storage eksternal), ini jadi silent
infinite loop: tiap `seekToNextMediaItem()` + `play()` cuma memicu `onPlayerError` lagi buat track
berikutnya, tanpa henti, tiap iterasi nembak `_playbackErrorMessage` baru (Snackbar spam).

**`PlayerViewModel.kt` (edit parsial, 1 file)**:
1. `describePlaybackErrorReason(error: PlaybackException): String` (baru, private) — map
   `error.errorCode` ke 4 kategori manusiawi: `ERROR_CODE_IO_FILE_NOT_FOUND` → file
   hilang/dipindah, `ERROR_CODE_IO_NO_PERMISSION` → izin ditolak, kelompok
   `PARSING_CONTAINER/MANIFEST_MALFORMED` → rusak/format tidak valid, kelompok
   `PARSING_*_UNSUPPORTED` + `DECODER_INIT_FAILED` + `DECODER_QUERY_FAILED` +
   `DECODING_FAILED` + `DECODING_FORMAT_EXCEEDS_CAPABILITIES` + `DECODING_FORMAT_UNSUPPORTED` →
   format/codec tidak didukung; else → generik. Dipakai di 2 tempat: pesan `_playbackErrorMessage`
   ke user DAN argumen `AppLogger.e(...)` untuk diagnostics — sebelumnya log cuma nangkep stack
   trace mentah tanpa reason terkategorisasi.
2. `consecutiveErrorCount` (private var, class-level, di-reset tiap `onCleared`/lifecycle ViewModel
   baru secara alami) + `MAX_CONSECUTIVE_PLAYBACK_ERRORS = 5` (companion object const) —
   `onPlayerError` sekarang increment counter ini SEBELUM decide auto-skip. Di bawah ambang:
   perilaku sama seperti sebelumnya (skip + play track berikut), cuma pesannya sekarang
   terkategorisasi. Begitu ambang (5) tercapai: `controller?.pause()` dipanggil (BUKAN skip lagi),
   1 pesan jelas ditampilkan ("Beberapa lagu berturut-turut gagal diputar (...). Playback
   dihentikan — periksa apakah file/folder musik kamu masih ada."), counter direset ke 0 supaya
   user bisa coba lagi manual tanpa nyangkut permanen di state "terlalu banyak error".
3. Reset counter: `onIsPlayingChanged(isPlaying: Boolean)` sekarang set `consecutiveErrorCount = 0`
   saat `isPlaying == true` — sengaja BUKAN di `onMediaItemTransition` (yang tetap terpanggil
   untuk track yang ujung-ujungnya error lagi sebelum sempat benar-benar main), `isPlaying=true`
   adalah sinyal paling jujur bahwa satu track berhasil pulih.

**Kenapa angka 5 (bukan konstanta lain)**: cukup toleran untuk pola realistis (2-3 file rusak
tersebar acak di tengah queue panjang, kasus paling umum menurut gap list) tanpa membiarkan loop
tak berkesudahan kalau sumbernya sistemik (folder dicabut total) — dipilih sebagai angka bulat
wajar, bukan hasil tuning empiris (belum ada device/telemetry test untuk kalibrasi lebih presisi,
dicatat sebagai gap tersendiri kalau nanti perlu disesuaikan).

**Yang SENGAJA belum disentuh di batch ini (di luar scope, tidak digabung supaya batch tetap
fokus)**: retry logic per kategori error (mis. retry sekali khusus buat error IO transient),
representasi error per-song di UI Library/Queue (sekarang murni Snackbar sekali tampil, hilang
begitu di-dismiss/auto-timeout) — keduanya tercatat sebagai lanjutan Gap List #8 kalau diperlukan.

Brace/paren `PlayerViewModel.kt` dicek seimbang (196 `{` / 196 `}`, 722 `(` / 722 `)`) setelah
edit. **Belum diverifikasi compile/runtime Gradle sungguhan** — tidak ada JDK/Android SDK di
sandbox kerja. Verifikasi berikutnya (butuh push + build + device fisik): matikan Wi-Fi/lepas SD
card yang berisi beberapa lagu di queue (simulasi file hilang), pastikan pesan error sekarang
menyebut kategori yang masuk akal ("file tidak ditemukan..." bukan generik), lalu coba skenario
seluruh queue nunjuk folder yang sudah dihapus — pastikan playback berhenti bersih dengan 1 pesan
setelah 5 percobaan, bukan Snackbar spam tanpa henti.

## Batch 112 — Fix baris tombol transport Now Playing ke-clip/hilang (root cause terpisah dari Batch 110/111)
User lapor via screenshot: baris tombol shuffle/prev/play/next/repeat di Now Playing masih hilang
dari layar SETELAH Batch 111. Investigasi ulang menemukan ini BUKAN kasus insets yang sama.

**Kenapa bukan kasus Batch 110/111**: `NowPlayingScreen` dipanggil dari `composable("now_playing")`
di dalam `NavHost`, yang ada di dalam `Scaffold` (`AppNavHost`, `MainActivity.kt`). Scaffold's
`padding` (dari `contentWindowInsets` bawaan) SUDAH diterapkan ke `Row` pembungkus `NavHost` — jadi
layar ini, tidak seperti `WelcomeScreen`/`PermissionRationale`/`LockScreen` di Batch 111, sudah
punya proteksi insets sejak awal.

**Root cause asli**: root `Column` di `NowPlayingScreen.kt` — `Modifier.fillMaxSize()` TANPA
`verticalScroll` — isinya kombinasi elemen fixed-height yang cukup besar: `Box` hero album art
300dp, `FeatureHintBanner` (~150dp) kalau belum di-dismiss user, title/artist/star-rating, waveform
+ slider 48dp, baris waktu, baru terakhir baris tombol transport. Total tinggi konten ini gampang
melebihi tinggi viewport SESUNGGUHNYA yang tersisa setelah dipotong status bar + nav bar —
khususnya 3-button nav (masih umum di device Android 15 ke bawah/budget, makan tinggi layar
nyata) dibanding gesture-nav (device Android 16 test, cuma overlay tipis). Karena `Column` tidak
scrollable, konten yang overflow SEBELUMNYA bukan digeser tapi ke-clip diam-diam di tepi layar —
dan karena baris tombol ada PALING BAWAH urutan Column, dia yang paling sering habis duluan.

**Fix — `NowPlayingScreen.kt` (edit parsial, 1 file)**:
- Import baru: `androidx.compose.foundation.rememberScrollState`, `androidx.compose.foundation.verticalScroll`.
- Root `Column` (pembungkus header, hint banner, hero art, title/rating, waveform, tombol
  transport) dapat `.verticalScroll(rememberScrollState())`, disisipkan sebelum `.padding(20.dp)`
  yang sudah ada.

**Kenapa aman untuk gesture drag yang sudah ada**: hero art 300dp punya 2 swipe-zone (`Box`)
untuk brightness (kiri) & volume (kanan) pakai `detectVerticalDragGestures` + `change.consume()`
eksplisit di tiap `onVerticalDrag` — pola ini SUDAH ada sebelum batch ini, dan `change.consume()`
adalah cara standar Compose mencegah `verticalScroll` di ancestor ikut menangkap drag yang sama.
Swipe next/prev (horizontal, di `AlbumArtHero`) beda axis, tidak tersentuh sama sekali.

**Yang TIDAK disentuh**: title lagu yang scroll sendiri (`basicMarquee()`, Now Playing) — user
menegaskan ulang eksplisit itu BUKAN bug, jadi nol perubahan terkait itu. `AlbumArtHero` internal,
`WaveformSeekBar`, ukuran hero art 300dp — tidak diubah (bukan akar masalah, cukup dibuat bisa
discroll saat overflow).

**Verifikasi statis**: brace/paren `NowPlayingScreen.kt` seimbang (199 `{` / 199 `}`, 673 `(` /
673 `)`) setelah edit; import baru dicek tidak duplikat. **Belum diverifikasi compile/runtime
Gradle sungguhan** — tidak ada JDK/Android SDK di sandbox kerja. Verifikasi berikutnya (butuh push
+ build sungguhan + device fisik Android 15 3-button-nav): buka Now Playing dengan hint banner
masih tampil (kondisi termudah memicu overflow, paling gampang direproduksi), pastikan baris
tombol transport tetap terjangkau (scroll kalau perlu, tidak lagi hilang total dari layar), dan
pastikan swipe brightness (kiri)/volume (kanan)/next-prev (di piringan) masih responsif seperti
sebelum batch ini — tidak ada regresi gesture akibat `verticalScroll` baru di ancestor.

## Batch 111 — Fix deformasi layout UI Android 15 ke bawah (eksekusi scope Batch 110)
Lanjutan langsung Batch 110 (audit). Root cause tidak diulang di sini — lihat entry Batch 110 di
bawah. Fix diterapkan:

1. **`MainActivity.kt` (protected, edit parsial — 2 private composable + import)**
   - Import baru: `androidx.compose.foundation.layout.WindowInsets`,
     `androidx.compose.foundation.layout.safeDrawing`,
     `androidx.compose.foundation.layout.windowInsetsPadding`.
   - `WelcomeScreen`: root `Column` modifier dapat `.windowInsetsPadding(WindowInsets.safeDrawing)`
     disisipkan SEBELUM `.padding(32.dp)` yang sudah ada (padding fixed tetap dipertahankan sebagai
     jarak visual, insets padding menutup gap status/nav bar yang sebelumnya nol).
   - `PermissionRationale`: modifier sama persis ditambahkan di root `Column`.
2. **`LockScreen.kt` (edit parsial — root `Column` saja)** — `.windowInsetsPadding(WindowInsets.safeDrawing)`
   ditambahkan di posisi sama (sebelum `.padding(32.dp)`). File ini sudah pakai wildcard import
   `androidx.compose.foundation.layout.*`, jadi tidak perlu import baru.
3. **`AndroidManifest.xml` (protected, edit parsial — 1 atribut)** — `<activity>` MainActivity
   dapat `android:windowLayoutInDisplayCutoutMode="shortEdges"`, eksplisit dinyatakan (sebelumnya
   tidak ada sama sekali di manifest), selaras dengan `enableEdgeToEdge()` yang sudah aktif global
   sejak sebelumnya.

**Yang SENGAJA tidak disentuh**: `AppNavHost`/`Scaffold` (`contentWindowInsets` bawaan sudah benar
untuk semua screen di dalamnya — bukan sumber bug, lihat audit Batch 110). `compileSdk`/`targetSdk`
tetap 34 (di luar scope batch ini, gap terpisah, sudah tercatat sejak Batch 99/110). Title/judul
lagu yang scroll bergerak sendiri di Now Playing (`basicMarquee()`) — user eksplisit menegaskan itu
BUKAN bug, itu perilaku marquee yang disengaja untuk teks panjang; nol perubahan terkait itu di
batch ini.

**Verifikasi statis**: brace/paren `MainActivity.kt` (245 `{` / 245 `}`, 563 `(` / 563 `)`) dan
`LockScreen.kt` (48/48, 128/128) seimbang setelah edit. Manifest divalidasi `xmllint --noout`
(valid). **Belum diverifikasi compile/runtime Gradle sungguhan** — tidak ada JDK/Android SDK di
sandbox kerja. Verifikasi berikutnya (butuh push + build sungguhan): build & install ke device
Android 15 3-button-nav, buka app fresh-install (WelcomeScreen → izin ditolak sekali →
PermissionRationale) dan device dengan App Lock aktif (LockScreen), pastikan konten tidak lagi
ketiban status bar di atas atau nav bar di bawah. Kalau masih ada overlap setelah ini: cek apakah
device test benar-benar 3-button nav (bukan gesture), dan cek `Theme.App.Starting` (splash theme)
tidak override edge-to-edge sebelum `setContent` sempat jalan.

## Batch 110 — Audit deformasi layout UI: normal di Android 16, kacau di Android 15 ke bawah (0 kode app diubah)
Instruksi user: audit kenapa layout tampak normal di OS 16 tapi kacau di OS 15 ke bawah. **2 file
diedit, keduanya dokumentasi** (`PROJECT_STATE.md`, `CHANGELOG.md`) — audit murni, TIDAK ada kode
app yang disentuh batch ini (fix nyata sengaja ditunda ke batch terpisah atas instruksi eksplisit
user: "dokumentasi lengkap dulu, baru eksekusi").

**Metode**: `grep -c` insets keyword (`statusBarsPadding|navigationBarsPadding|safeDrawingPadding|
WindowInsets|imePadding`) ke seluruh 20 file `app/src/main/java/com/rudi/audioplayer/ui/*.kt` +
baca penuh `MainActivity.kt` (1165 baris) + cek `AndroidManifest.xml` (cutout mode) +
`app/build.gradle.kts` (compileSdk/targetSdk) + `ui/adaptive/WindowAdaptive.kt`.

**Temuan terkonfirmasi (bukan dugaan)**:
1. `enableEdgeToEdge()` aktif global di `MainActivity.kt:188`, berlaku semua API 23+ (androidx
   compat) — TAPI **0 dari 20 file** `ui/*.kt` memakai insets modifier apa pun. Satu-satunya
   sumber insets-padding di seluruh app adalah `contentWindowInsets` bawaan `Scaffold` (default,
   tidak dioverride) di `AppNavHost`.
2. 3 screen render **di luar** `Scaffold` itu: `WelcomeScreen`, `PermissionRationale`,
   `LockScreen` (`MainActivity.kt:380-401`, langsung di dalam `Surface`/`Box` polos) — genuinely
   nol proteksi status bar/nav bar/cutout. `LockScreen` paling berisiko (screen pertama yang
   tampil tiap app dibuka ulang kalau PIN lock aktif).
3. `AndroidManifest.xml` tidak mendeklarasikan `windowLayoutInDisplayCutoutMode` — default
   behavior-nya berbeda per API tier (belum pernah diuji eksplisit).
4. `compileSdk`/`targetSdk` masih **34** (`app/build.gradle.kts:73,78`) — Android 15/16 API
   surface belum resmi disasar sama sekali. Ini BUKAN temuan baru — sudah tercatat sebagai gap
   sadar di Batch 99 ("Android 15/16-spesifik butuh targetSdk dinaikkan dulu... sengaja batch
   terpisah") dan `MANUAL_QA_CHECKLIST.md` ("Android 15/16 behavior" masuk daftar belum
   tervalidasi device fisik).

**Diruled out (dicek, bukan diasumsikan aman)**: `rememberAppWidthClass()`
(`ui/adaptive/WindowAdaptive.kt`) — murni baca `LocalConfiguration.screenWidthDp`, API yang
sama persis dan reaktif sejak API 23, tidak ada percabangan versi sama sekali → bukan sumber
perbedaan perilaku lintas OS.

**Hipotesis kenapa OS 16 tampak "normal" vs OS 15 ke bawah "kacau"** (confidence sedang — butuh
screenshot pembanding device fisik untuk dipastikan, dicatat jujur sebagai hipotesis bukan
kesimpulan final): gesture-nav (lazim di device test OS 16) menyisakan overlay tipis/transparan
di atas konten sehingga overlap nyaris tak kelihatan; 3-button nav (masih umum di device
budget/OS lama) memakan tinggi layar tetap & opaque, sehingga teks/tombol di 3 screen tanpa
insets di atas kepotong nyata. Perbedaan tinggi status bar/notch antar generasi device turut
memperbesar gap ini — bukan murni soal nomor versi OS, tapi versi OS berkorelasi kuat dengan
jenis navigasi & device tier yang lebih umum dipakai.

**Confidence diagnosis kode: 85%** (temuan 1-4 pasti dari pembacaan kode langsung; korelasi
spesifik "OS 16 vs OS 15" butuh 1 laporan/screenshot device fisik untuk naik ke ~95%+).

**Scope fix yang disiapkan untuk batch berikutnya** (belum dieksekusi batch ini): tambah insets
padding (`safeDrawingPadding()` atau setara) ke `WelcomeScreen`/`PermissionRationale`/
`LockScreen`, plus deklarasi eksplisit `windowLayoutInDisplayCutoutMode` di manifest. Estimasi
3 file, 0 protected asset inti tersentuh (`MainActivity.kt` sendiri termasuk protected — edit
akan bersifat parsial, hanya di 2 private composable function + 1 tambahan modifier di
`LockScreen` yang filenya sendiri tidak protected).

## Batch 109 — Gap List #7: Sleep timer process-resilient
3 file: `SleepTimerStore.kt` (baru), `PlaybackService.kt` (diedit, protected), `PlayerViewModel.kt`
(diedit).

Checklist Gap List #7 diaudit: sleep timer sebelumnya HANYA `viewModelScope.launch` — kalau
`PlayerViewModel` di-clear (proses di-kill total selagi `PlaybackService` foreground masih
diminta system tetap hidup), timer hilang tanpa jejak, lagu terus main tanpa batas.

- `SleepTimerStore.kt` — SharedPreferences 1 key: `endAt` (epoch millis absolut, bukan durasi).
- `PlaybackService.kt` — eksekusi pause sungguhan dipindah ke `serviceScope` (Service, bukan
  ViewModel). `ACTION_SET_SLEEP_TIMER` custom `SessionCommand` baru (pola identik 2 command yang
  sudah ada). `scheduleSleepTimer(endAtMillis)`/`cancelSleepTimer()` — cancel job lama + tulis/
  hapus store SELALU bersamaan (atomic). `resumeSleepTimerFromStore()` dipanggil di `onCreate`
  setelah `mediaSession` siap: restore sisa waktu yang benar kalau belum lewat, atau pause sekali
  + bersihkan kalau sudah lewat selagi proses mati (bukan diam-diam diabaikan, bukan juga dobel-
  eksekusi di restart berikutnya). `onTaskRemoved()` (queue kosong → `stopSelf()`) sekalian
  `cancelSleepTimer()`.
- `PlayerViewModel.kt` — `setSleepTimer()`/`cancelSleepTimer()` kirim command ke Service (bukan
  lagi `controller?.pause()` langsung dari ViewModel). Coroutine ViewModel yang tersisa murni
  kosmetik (tampilan countdown UI, dihitung ulang dari `endAt - now()` tiap tick supaya tidak
  drift). `init {}` baru: baca `SleepTimerStore` saat ViewModel dibuat, restore tampilan
  countdown kalau ada timer aktif tersisa dari ViewModel sebelumnya — cuma soal UI, tidak
  memengaruhi apakah timer akan benar-benar berbunyi.

**Kenapa bukan `AlarmManager`**: Service ini sudah foreground selama playback (sejak migrasi
`MediaLibraryService` Batch 12) — coroutine di scope Service sudah cukup untuk skenario yang
relevan (proses mati SELAGI foreground service masih terkait). `AlarmManager` menambah
kompleksitas (exact-alarm permission API 31+) untuk skenario di luar cakupan realistis fitur ini
(reboot/force-stop total persis di tengah sleep timer aktif).

**Insiden proses (bukan bug kode)**: 1 `str_replace` sempat memotong docstring
`maybeStartFloatingBubble()` (kehilangan pembuka `/**`) saat menyisipkan pemanggilan
`resumeSleepTimerFromStore()` di akhir `onCreate()` — ketahuan & diperbaiki dari audit
brace/paren otomatis sebelum repack, bukan lolos ke ZIP.

Belum diverifikasi compile/runtime Gradle sungguhan (tidak ada JDK/Android SDK/kotlinc di
sandbox kerja). Prioritas verifikasi berikutnya: set sleep timer, force-stop app dari App Info
(simulasi kill proses), tunggu lewat deadline, buka lagi app — pastikan lagu genuinely sudah
ter-pause dan tidak ada crash log baru.

## Batch 108 — Gap List #6: Durable playback state (repeat/shuffle)
2 file: `PlaybackStateStore.kt` (diedit), `PlayerViewModel.kt` (diedit).

Checklist Gap List #6 diaudit satu per satu: current track/posisi/queue sudah persist sejak
lama, checkpoint sudah cukup sering (~5s saat playing + immediate saat pause) & tidak berlebihan.
Gap nyata yang ditemukan: **repeat mode dan shuffle tidak pernah dipersist**, jadi selalu reset
ke off tiap kali app dibuka ulang meski user terakhir aktifkan keduanya.

- `SavedPlaybackState` (`PlaybackStateStore.kt`) — 2 field baru: `repeatMode: Int`,
  `shuffleEnabled: Boolean`.
- `save()` — signature dapat 2 parameter baru, diisi dari `controller.repeatMode` /
  `controller.shuffleModeEnabled` di titik checkpoint yang sama (`persistPlaybackState()`),
  tidak ada I/O tambahan.
- `load()` — sekarang dibungkus `try/catch` eksplisit: state corrupt/incompatible di masa depan
  jatuh ke `null` (dianggap kosong) bukan melempar exception ke `resumeFromSaved()`. Ditambah
  `KEY_SCHEMA_VERSION`/`SCHEMA_VERSION` const untuk dokumentasi kontrak data (belum ada logic
  migrasi bertingkat — belum perlu, field baru pakai default aman lewat `getInt`/`getBoolean`).
- `PlayerViewModel.resumeFromSaved()` — set `controller.repeatMode` dan
  `controller.shuffleModeEnabled` SEBELUM memanggil `playQueue()`, supaya kalau shuffle aktif,
  urutan shuffle terbentuk sejak media item pertama kali di-set ke controller (bukan re-shuffle
  setelah queue sudah berjalan dengan urutan asli).

**Diaudit & sengaja TIDAK diubah**: volume (`userTargetVolume`) bukan preferensi user — itu
murni level fade internal untuk true crossfade (Batch 102), nilainya balik ke `1f` di luar
window fade. Mempersistnya lintas sesi tidak berarti apa-apa untuk pengalaman user.

**Push pertama gagal (CI run 161, build, log `log_fail_161.zip`)** — `compileReleaseKotlin`/
`compileDebugKotlin` gagal: `PlaybackStateStore.kt:46:56` & `:48:28`, "Returns are not allowed
for functions with expression body. Use block body in '{...}'". Root cause: `load()` ditulis
`fun load(): SavedPlaybackState? = try { ... } catch { ... }` (expression body via `=`), tapi
isinya pakai 2 early-return (`?: return null`, `if (ids.isEmpty()) return null`) — early-return
cuma sah kalau function-nya block body (`{ ... }`), bukan expression body, terlepas dari apa pun
yang ada di dalam try/catch. Fix: ubah ke `fun load(): SavedPlaybackState? { return try { ...
} catch (e: Exception) { ... null } }` — block body eksplisit, isi try/catch persis sama tidak
diubah. **Pelajaran: gaya ringkas `fun x(): T = ...` menggoda dipakai untuk function pendek
apa pun, tapi begitu isinya butuh early-return di tengah jalan, wajib block body — cek pola ini
saat menulis, jangan nunggu ketahuan dari CI log.**

### Fix2 — Crash device sungguhan (`crash_20260817_111602`, Android 15, Infinix X6850)
`IllegalStateException: MediaController method is called from a wrong thread`, thread
`DefaultDispatcher-worker-2` — terjadi berulang tiap ~5 detik selama playback (checkpoint
interval `persistPlaybackState()`). Root cause: `c.repeatMode`/`c.shuffleModeEnabled` (field
baru Fix ini) dibaca DI DALAM `viewModelScope.launch(Dispatchers.IO)`, bukan di main thread
seperti `songIds`/`positionMs`/`index` yang sudah lama benar. `MediaController` melempar
exception ini kalau method-nya dipanggil dari thread mana pun selain thread yang membuat
controller (main). Fix: `repeatMode`/`shuffleEnabled` dibaca sebagai `val` lokal di main thread
tepat SEBELUM `launch(Dispatchers.IO)`, dikirim ke `save()` sebagai parameter biasa — pola
sekarang identik dengan field lama, tidak ada lagi akses `MediaController` di dalam lambda
background dispatcher manapun di fungsi ini.

Crash nyata pertama proyek ini yang ketahuan lewat crash logger (Batch 22) sejak fitur itu ada.
Belum diverifikasi compile/runtime Gradle sungguhan setelah fix ini (tidak ada JDK/Android
SDK/kotlinc di sandbox kerja). Prioritas verifikasi berikutnya: build APK asli, matikan app
total dengan shuffle ON + repeat-one aktif, buka lagi, pastikan keduanya genuinely kepulihkan
di UI dan `MediaController`, DAN tidak ada crash log baru muncul selama playback berjalan lebih
dari beberapa menit.

## Batch 107 — Bersihkan tag & judul GitHub Release
2 file (1 protected). Permintaan langsung user dari screenshot halaman Releases repo: (1) tag
`v1.0.47-release-run159` → hapus kata "release" (run number sendiri sudah cukup unik); (2) judul
rilis yang tampil di daftar Releases dibuat minimalis, cuma nomor versi (`v1.0.47`), tapi tetap
sinkron dengan APK.

**`.github/workflows/build.yml`** — step "Determine version name": output `tag` sekarang
`v$VERSION_NAME-run${{ github.run_number }}` (dulu ada `-release-` di tengah). Output baru
`release_name` = `v$VERSION_NAME` (tanpa run number sama sekali) — khusus untuk judul tampilan,
terpisah dari `tag` yang tetap wajib unik per run (invariant Batch 65: tag/nama file APK harus
beda tiap run walau commit sama, atau hasil unduhan APK bentrok "(1).apk" duplikat di HP). Step
"Create GitHub Release": `tag_name: tag` (tidak berubah), `name: release_name` (dulu `name: tag`
— inilah sebabnya judul rilis dulu ikut menampilkan run number). Step "Rename APK to match
version" tidak diedit sama sekali — sudah otomatis ikut pola baru karena membaca
`steps.version.outputs.tag` secara dinamis, hasilnya `AudioPlayer-v1.0.47-run159.apk`.

Kedua output (`tag` & `release_name`) diturunkan dari `$VERSION_NAME` yang sama, dihitung sekali
di baris yang sama — formula MAJOR.MINOR.PATCH-nya sendiri (Batch 86) sama sekali tidak disentuh,
jadi invariant "app, tag, dan nama file APK selalu match" (Batch 30/56/86) tetap terjaga; yang
berubah cuma REPRESENTASI tag vs judul, bukan sumber angkanya.

**`README.md`** § Standar Penomoran Versi — 2 contoh (`AudioPlayer-v1.5.17-release-run42.apk`)
diupdate ke pola baru (`AudioPlayer-v1.5.17-run42.apk`), + 1 paragraf baru menjelaskan kenapa tag
& judul rilis sengaja beda representasi sekarang.

YAML divalidasi lewat `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml'))"`
— parse sukses. **Belum diverifikasi CI run sungguhan** (tidak ada akses GitHub Actions di
environment kerja ini) — prioritas berikutnya: 1 run penuh, cek tag baru terbentuk benar tanpa
"-release" DAN judul di halaman Releases genuinely tampil minimalis.

## Batch 106 — Gap List #5: SAF parity dengan MediaStore
4 file diedit, 0 baru. Audit 8 sub-item checklist #5, hasil: 2 gap nyata + 1 dokumentasi basi
diperbaiki, 2 sub-item dicek dan sudah benar sejak lama, sisanya (bitrate/sampleRate/dst.,
duplicate hash 32-bit) sudah tertutup di Batch 104/105.

**1. `CustomFolderStore.kt`** — `CustomFolderInfo` dapat field baru `permissionGranted: Boolean
= true`. Default `true` supaya construction site lama (kalaupun ada) tidak berubah perilaku
diam-diam; satu-satunya tempat yang pernah mengisi nilai nyata adalah
`PlayerViewModel.loadCustomFolderInfos()` di bawah.

**2. `PlayerViewModel.kt`** — `hasPersistedReadPermission(uri)` baru, cek langsung ke
`ContentResolver.persistedUriPermissions` (satu-satunya sumber valid — sistem tidak pernah
memberi callback saat izin dicabut dari luar app). Dipanggil di 2 titik: (a)
`loadCustomFolderInfos()` — isi field baru `permissionGranted` tiap kali daftar folder dibangun
ulang; (b) `refreshLibrary()` — sekarang refresh `_customFolders` di awal tiap scan (badge tidak
basi kalau cuma dihitung sekali saat add/remove), DAN skip `scan()` sama sekali untuk folder yang
sudah dikonfirmasi tidak berizin (sebelumnya selalu coba scan dulu, gagal `SecurityException`,
log — sekarang skip lebih awal, bukan lagi kegagalan I/O yang perlu dicatat berulang tiap refresh
untuk kondisi yang sudah diketahui).

**3. `CustomFolderScanner.kt`** — dua perubahan: (a) `MAX_DEPTH` 6→20 (gap list tandai "terlalu
sempit" — struktur `Musik/Artis/Album/CD1/...` realistis bisa >6 level lewat sebagian file
manager/sync tool yang nambah 1 level nesting; 20 tetap hard ceiling, bukan dihapus, karena
traversal ini rekursif jadi tetap butuh batas aman terhadap tree yang sengaja/korup nge-nest
sangat dalam); (b) komentar `albumId = -1L` ditulis ulang — versi lama klaim lagu SAF "tidak ada
artwork lookup". Audit silang ke `AudioArtFetcher.kt`/`PlaybackService.kt`'s
`SongArtBitmapLoader`/`AccentColorExtractor.kt`/`WidgetUpdater.kt` (semua 4 loader artwork di
app) menunjukkan itu sudah basi sejak Batch 67-69 — keempatnya baca artwork lewat `song.uri`
generik (bukan `albumId`), yang bekerja identik untuk content URI dokumen SAF seperti URI
MediaStore. Diperbaiki jadi akurat: lagu SAF sudah dapat artwork tertanam asli, cuma yang benar-
benar tidak punya artwork tertanam yang jatuh ke placeholder — sama seperti lagu MediaStore.

**4. `FolderManagerSheet.kt`** — badge teks merah baru di bawah nama folder tambahan yang
`permissionGranted == false`: "Izin dicabut — lagunya tidak lagi terpindai. Hapus lalu pilih
ulang." Tombol hapus yang sudah ada (Batch 26, sudah toleran ke `releasePersistableUriPermission`
yang gagal karena izin memang sudah hilang) tidak perlu diubah — badge ini murni membuat
penyebabnya kelihatan di UI, bukan mengubah alur hapus.

**Diaudit, TIDAK ada perubahan (sudah benar sejak lama, dicek eksplisit)**: dedupe SAF-vs-
MediaStore (`refreshLibrary()`'s `dedupeSignature()`+`dedupedCustomSongs`, prefer salinan
MediaStore) dan idempotensi refresh (`libraryRefreshGeneration` counter cegah scan lama menimpa
scan baru) — keduanya sudah ada dan benar, tidak disentuh.

**Sengaja BELUM digarap**: "metadata extraction SAF sedekat mungkin dengan MediaStore" dari
checklist #5 — sudah paritas sejak Batch 105 (genre/bitrate/sampleRate/channelCount/codec
sama-sama absen di KEDUA sumber, jadi bukan gap SAF-spesifik, gap umum item #4/#11 terpisah).

Brace/paren 4 file dicek otomatis & seimbang. **Belum diverifikasi compile/runtime Gradle
sungguhan** (tidak ada JDK/Android SDK/kotlinc di sandbox ini) — prioritas berikutnya:
`./gradlew testDebugUnitTest` lalu build APK asli + cek di device (1) cabut izin folder
tambahan lewat Settings sistem, buka Kelola Perpustakaan, pastikan badge merah muncul, (2) lagu
folder SAF yang punya embedded art genuinely tampil (bukan cuma diverifikasi baca-kode), (3)
scan folder dalam (>6 level) tidak lagi terpotong di level 6.

## Batch 105 — Gap List #4: Metadata model diperkuat
4 file (3 diedit + 1 baru). Scope sengaja dipersempit ke field yang bisa didapat TANPA I/O
tambahan — dibaca dari row cursor MediaStore yang sama (bulk scan, sekali query) atau dari pass
`MediaMetadataRetriever` yang SAF sudah buka (satu kali per file, sudah ada sejak awal). Field
yang butuh pass retriever KEDUA per file saat scan (bitrate, sampleRate, channelCount, codec
terverifikasi, kehadiran embedded artwork) **sengaja belum** — biaya N+1 di seluruh library,
alasan sama persis dengan genre yang sudah lama di-skip Batch 89 (genre juga punya gap list
item sendiri, #11, untuk follow-up). Kandidat pendekatan nanti: fetch on-demand per-lagu (sheet
\"Info Lagu\"), bukan biaya bulk-scan yang dibayar semua orang walau tak pernah dilihat.

**1. `Song.kt`** — 6 field baru: `albumArtist`, `composer` (String?, null=absen),
`trackNumber`/`discNumber` (Int?, 1-based, null=absen), `fileSize` (Long, 0=unknown — harusnya
tak pernah terjadi, SIZE MediaStore & `DocumentFile.length()` SAF selalu ada), `mimeType`
(String?, dipakai sbg container/format — bukan codec terverifikasi, itu butuh probe level
decoder, di luar cakupan). Semua nullable/default-0 di posisi TERAKHIR constructor — 0 call
site lama (test fixture termasuk, semua named-arg) yang perlu diubah, dicek grep dulu.

**2. `MusicRepository.kt`** — projection query tambah `ALBUM_ARTIST`/`COMPOSER`/`SIZE`/
`MIME_TYPE` (kolom inti, selalu ada di semua level API, aman `getColumnIndexOrThrow`) + cabang
track/disc: API 30+ (`Build.VERSION_CODES.R`) pakai kolom string `cd_track_number`/`disc_number`
BARU (literal string, bukan konstanta `MediaStore.Audio.AudioColumns.*` — supaya file ini tetap
kompail lepas dari compileSdk stub tanpa gate tambahan, kontrak nama kolomnya sendiri tetap
stabil API 30+ platform); pre-30 fallback ke kolom `TRACK` lama (int gabungan
`disc*1000+track`, konvensi resmi AOSP MediaProvider). Kedua parser jadi fungsi murni testable
di companion: `parseTrackOrDiscString(String?)` (ambil leading digit run, jadi \"5\" maupun
\"5/12\" sama-sama kena parse ke `5`) dan `parseLegacyTrackColumn(Int)` (decode gabungan
disc/track, `<=0` → keduanya absen).

**3. `CustomFolderScanner.kt`** — 4 `extractMetadata()` tambahan di pass `MediaMetadataRetriever`
yang SUDAH terbuka (`METADATA_KEY_ALBUMARTIST`/`COMPOSER`/`CD_TRACK_NUMBER`/`DISC_NUMBER`/
`MIMETYPE`) — 0 pass tambahan, sama disc/track parser dipanggil lewat
`MusicRepository.parseTrackOrDiscString()` (internal, sama modul, dipakai lintas file biar
MediaStore & SAF sepakat 1 aturan parsing) supaya tidak duplikat logic. `fileSize` dari
`doc.length()` (metadata provider yang sudah di-cache, bukan baca isi file).

**4. `MusicRepositoryTrackDiscTest.kt` (baru)** — 9 test: `parseTrackOrDiscString` (bare number,
\"N/M\", null/blank/\"0\"/non-numeric → null, whitespace trim) + `parseLegacyTrackColumn`
(track-only <1000, disc+track gabungan, 0/negatif → keduanya null, disc-only dgn track 0).

**Belum digarap batch ini, sengaja**: field bitrate/sampleRate/channelCount/codec-terverifikasi/
embedded-artwork-presence (butuh retriever pass kedua, N+1 — lihat penjelasan scope di atas),
genre (item gap list terpisah, #11), UI display field-field baru ini (Song sudah bawa datanya,
belum ada layar/sheet yang menampilkannya — kandidat batch polish berikutnya kalau user mau).

Brace/paren 4 file dicek otomatis & seimbang. Grep dikonfirmasi 0 call site `Song(...)` lain
di luar `MusicRepository.kt`/`CustomFolderScanner.kt`/test fixtures (semua named-arg, aman).
`FILE_MANIFEST.txt` 148→149. **Belum diverifikasi compile/test sungguhan** (tidak ada kotlinc
di sandbox ini) — semua API yang dipakai (`MediaStore.Audio.Media.ALBUM_ARTIST/COMPOSER/SIZE/
MIME_TYPE`, `MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST/COMPOSER/CD_TRACK_NUMBER/
DISC_NUMBER/MIMETYPE`) adalah konstanta lama & stabil sejak API awal, risiko compile rendah.
Prioritas berikutnya: `gradle testDebugUnitTest` verifikasi 9 test baru, lalu lanjut Gap List
item #5 (SAF parity dengan MediaStore) atau #6 (Durable playback state, sudah sebagian besar
ada per audit Batch 104).

## Batch 104 — Batch 103 CI CONFIRMED PASSING + Gap List #3/#5: SAF song identity
User upload `instrumentation_test_report_156.zip` (JUnit HTML report dari CI run sungguhan) —
**Batch 103's 7 instrumentation test SEMUA HIJAU** (`PlaybackTransportTest`: play/pause, seek,
next, previous, repeat-off/all/one, shuffle toggle — 7/7 success, 0 failure, 0 skipped). Ini CI
run pertama proyek ini yang benar-benar dieksekusi di emulator (bukan lagi analisis statis) —
`reactivecircus/android-emulator-runner@v2` + API 30 + `runOnMainSync`/listener-latch pattern
Batch 103 terbukti jalan tanpa perlu hotfix. `MANUAL_QA_CHECKLIST.md` (audio focus/Bluetooth/
lock-screen/dll) masih berlaku apa adanya — di luar cakupan emulator CI standar.

**2 file — Gap List #3 & #5 (URI.hashCode() sebagai identitas)**: `CustomFolderScanner.kt`'s
`stableId()` (ID lagu SAF) sebelumnya `String.hashCode()` Java (32-bit) masuk ke 31 bit —
ruang collision cuma ~2.1 miliar bucket dari algoritma yang publicly-known lemah, birthday-bound
collision realistis mulai puluhan ribu URI (library SAF besar bisa kena). Diganti FNV-1a 64-bit
atas byte UTF-8 URI, dimasking ke 63 bit lalu dinegasikan (`stableId()` di `CustomFolderScanner`,
sekarang delegasi ke fungsi murni `Companion.stableId(String)` — dipisah biar testable tanpa
Robolectric, pola sama `MusicRepository.deriveFolderName` Batch 27). Ruang collision sekarang
~2^63, birthday-bound baru mulai ~3 miliar URI berbeda — bukan lagi guaranteed-unique (masih
hash, bukan registry), tapi gap-nya sekarang astronomis lebih kecil, dicatat jujur di komentar
kode. **Namespace MediaStore vs SAF sudah eksplisit dari sign bit** (MediaStore `_ID` selalu
non-negative, SAF `stableId()` selalu negative) — tidak perlu tag tipe tambahan.

`CustomFolderScannerStableIdTest.kt` (baru) — 4 test: determinism (URI sama → id sama), 2 URI
beda → id beda, semua id selalu negatif (0 kemungkinan tabrakan dgn MediaStore), 500 URI
near-identical (`track_1.mp3`...`track_500.mp3`) → 500 id distinct (sanity-check avalanche,
bukan bukti matematis nol-collision — tidak mungkin dibuktikan lewat unit test biasa).

**Validasi queue existing yang sudah cukup baik** (dicek, bukan diabaikan — bagian dari gap #3
\"validasi queue item sebelum restore\"/\"tangani URI tak tersedia\"/\"bersihkan orphan\"):
`PlayerViewModel.resumeFromSaved()` sudah `saved.songIds.mapNotNull { songMap[it] }` — id yang
tidak ada lagi di scan terbaru (file dihapus/uri SAF revoked) otomatis ke-drop, urutan sisanya
tetap terjaga (`mapNotNull` preserve order). Orphan tidak permanen nyangkut — `persistPlaybackState()`
jalan tiap ~5 detik saat main + langsung saat pause, jadi queue yang sudah dibersihkan otomatis
ter-flush ke storage di siklus berikutnya. Tidak ada perubahan diperlukan di titik ini.

Brace/paren 2 file kode dicek otomatis & seimbang. `FILE_MANIFEST.txt` 147→148. **Belum
diverifikasi compile/test sungguhan** (tidak ada kotlinc di sandbox ini) — tapi pola FNV-1a murni
Kotlin standard-library (`Long`/`Byte` arithmetic, `toByteArray(Charsets.UTF_8)`), 0 API baru yang
berisiko meleset. Prioritas berikutnya kalau user push: `gradle testDebugUnitTest` verifikasi 4
test baru, lalu Gap List item #3 sisanya (`3. Queue identity` checklist lain sudah tercakup lewat
audit di atas — item ini sekarang bisa dicentang) dan lanjut ke item #4 (Metadata model
diperkuat) atau #6 (Durable playback state, sudah sebagian besar ada) sesuai prioritas P0/P1
gap list. Detail lengkap: `CustomFolderScanner.kt` komentar `stableId()`.

## Batch 103 — Gap List #2: Integration/device instrumentation testing
Item P0 kedua di `AudioPlayer_Coding_Gap_List.md`. **9 file** (5 baru + 2 file kode diedit + 2
asset WAV biner baru, 2 di antaranya protected — `app/build.gradle.kts` & `.github/workflows/
build.yml`, keduanya edit parsial). Diklasifikasikan sebagai **Atomic Change**: test code, gradle
wiring-nya, dan CI job yang menjalankannya adalah 1 deliverable yang saling bergantung — test
tanpa gradle wiring tidak kompail, test tanpa CI job tidak pernah benar-benar jalan (workflow
Termux-only user ini tidak punya emulator/device lokal buat `./gradlew connectedAndroidTest`
manual), jadi memecahnya jadi >1 batch cuma menunda nilainya tanpa mengurangi risiko.

**Sebelum batch ini**: proyek 100% pure-JVM test (`app/src/test`) saja — komentar yang SUDAH ada
sebelum batch ini di `app/build.gradle.kts` sendiri jujur bilang kenapa: "no Robolectric/
instrumentation, so these run in seconds with no emulator and are cheap enough to actually get
written and kept up to date." Menambah instrumentation test berarti juga wajib menambah tempat
utk itu benar2 dieksekusi — makanya CI job baru masuk batch yang sama.

**1. `app/src/androidTest/java/com/rudi/audioplayer/playback/PlaybackServiceTestHelper.kt`
(baru)** — shared boilerplate koneksi `MediaController` SUNGGUHAN ke `PlaybackService` SUNGGUHAN
yang sedang jalan (bukan fake Player/mock session — device test yang menguji integrasi
sungguhan, sesuai nama gap list item-nya). 3 detail teknis yang sengaja ditulis presisi krn mudah
salah:
- `MediaController.Builder(...).buildAsync()` WAJIB dipanggil dari thread yang punya Looper
(lewat `InstrumentationRegistry.getInstrumentation().runOnMainSync {}`) — thread test
instrumentation default TIDAK punya Looper, langsung exception kalau dipanggil polos.
- Menunggu future SELESAI dilakukan DI LUAR blok `runOnMainSync` itu, via listener + `CountDown
Latch` di thread test — bukan `.get()` blocking di dalam `runOnMainSync` yang sama, karena itu
deadlock: main thread yang diblokir justru thread yang perlu memproses handshake koneksinya
sendiri.
- Queue test 2 track diputar lewat `file://` (hasil copy byte asset test APK ke `cacheDir` app),
BUKAN `asset:///` — `asset:///` di-resolve relatif ke Context yang membangun ExoPlayer-nya, dan
ExoPlayer sungguhan hidup di dalam `PlaybackService` dgn Context APP (`app/src/main`), sedangkan
`app/src/androidTest/assets/` masuk ke APK TEST yang terpisah, cuma bisa dibaca lewat
`InstrumentationRegistry.getInstrumentation().context` (instrumentation context) — bukan lewat
`targetContext` yang dipakai ExoPlayer app sungguhan. Salah pakai `asset:///` di sini akan
compile mulus tapi gagal runtime (file "not found") — dicek manual sebelum ditulis, bukan
kebetulan ketemu benar.

Dua file WAV test (`app/src/androidTest/assets/test_tone_a.wav` 440Hz/3 detik,
`test_tone_b.wav` 660Hz/2 detik) dibuat lokal lewat modul stdlib Python `wave` (nada sinus murni,
fade in/out 50ms cegah klik) — bukan lagu sungguhan, nol isu hak cipta, nol kebutuhan akses
network utk unduh sample.

**2. `app/src/androidTest/java/com/rudi/audioplayer/playback/PlaybackTransportTest.kt`
(baru)** — 7 test method: `playThenPause_updatesIsPlaying`, `seekTo_movesPlaybackPosition`,
`skipToNext_advancesToSecondTrack`, `skipToPrevious_returnsToFirstTrack`,
`repeatModeOne_loopsSameTrackPastItsOwnDuration` (betul2 menunggu lewat 3 detik durasi asli
track A, verifikasi ExoPlayer benar mengulang bukan lanjut ke track berikutnya — bukan cuma
mengecek nilai setter `repeatMode`), `repeatModeAll_wrapsFromLastTrackBackToFirst`,
`shuffleToggle_reportsEnabledState`. Semua assertion async lewat helper `waitUntil()` polling
(bukan baca state segera setelah kirim command — command lewat `MediaController` itu round-trip
Binder/session sungguhan, butuh waktu, walau kecil).

**3. `app/build.gradle.kts` (protected, edit parsial)** — `testInstrumentationRunner =
"androidx.test.runner.AndroidJUnitRunner"` (baru, sebelumnya field ini tidak ada sama sekali di
`defaultConfig`) + 3 dependency baru: `androidx.test.ext:junit:1.2.1`, `androidx.test:
runner:1.6.1`, `androidx.test:core:1.6.1` (`androidTestImplementation`). Sengaja TIDAK menambah
Guava/`concurrent-futures` apa pun tambahan — `com.google.common.util.concurrent.Futures`/
`ListenableFuture`/`MoreExecutors` yang dipakai `PlaybackServiceTestHelper.kt` sudah terbukti
kompail lewat `PlaybackService.kt` yang sudah ada duluan di proyek ini (dicek via `grep import`
sebelum menulis kode, bukan asumsi) — dan `androidTest` 1-modul (bukan modul Gradle terpisah)
secara default AGP mewarisi classpath `implementation` milik `main`, jadi tidak perlu
dideklarasikan ulang.

**4. `.github/workflows/build.yml` (protected, edit parsial)** — job baru `instrumentation-
tests`, divalidasi YAML-nya (`python3 -c "import yaml; yaml.safe_load(...)"`) sebelum di-zip,
bukan cuma dicek visual indentasinya. **Sengaja independen** — TIDAK ada `needs:` ke job `build`
maupun sebaliknya, supaya kegagalan/flaky-nya emulator TIDAK PERNAH memblokir publish GitHub
Release APK (job `build` tetap jalan & sukses normal terlepas hasil job ini). Pakai
`reactivecircus/android-emulator-runner@v2` (action pihak ketiga paling umum dipakai utk
kebutuhan ini, KVM diaktifkan eksplisit dulu di runner ubuntu-latest via udev rule supaya boot
emulator tidak lambat/timeout), `api-level: 30` (BUKAN 35/36 — `compileSdk`/`targetSdk` app ini
sendiri masih 34, menyasar API di atasnya tidak akan mengetes perilaku apa pun yang app-nya
sendiri belum menyasar; lihat `MANUAL_QA_CHECKLIST.md`), command `gradle connectedDebugAndroidTest`
(BUKAN `./gradlew` — proyek ini belum menyertakan Gradle Wrapper sama sekali, gap list item #19,
sengaja bukan bagian batch ini; `gradle` binary versi 8.7 sudah disiapkan di PATH job ini lewat
step `setup-gradle@v3` yang sama persis dgn job `build`).

**5. `MANUAL_QA_CHECKLIST.md` (baru, root)** — pasangan jujur `PlaybackTransportTest.kt`: item
gap list #2 yang secara eksplisit diminta (Bluetooth/media output, lock-screen controls,
notification controls, headset fisik play-pause/next/previous, audio focus real hardware,
process death di device fisik, background playback jangka panjang, Android 15/16 behavior) TIDAK
ditulis sebagai instrumentation test palsu/mock yang cuma memverifikasi mock-nya sendiri —
ditulis sbg checklist manual bertanda-tangan-device, dgn alasan teknis jujur kenapa tiap kategori
butuh device fisik/tooling di luar scope batch ini.

**Sengaja BELUM digarap / batasan disadari** (dicatat, bukan terlewat — lihat juga
`PROJECT_STATE.md`):
- Semua 7 item di atas yang masuk `MANUAL_QA_CHECKLIST.md` — alasan per-kategori ada di file itu
sendiri, bukan diulang di sini.
- Android 15/16-spesifik butuh `targetSdk` dinaikkan dulu (perubahan protected-asset berisiko
tinggi tersendiri: predictive back, foreground service type enforcement, perilaku notifikasi,
dll bisa berubah) — SENGAJA batch terpisah, tidak digabung diam-diam ke sini walau sama-sama
soal "testing".
- Job CI baru menambah runner-minutes GitHub Actions tiap push ke `main` (jalan paralel, bukan
menambah durasi job `build`, tapi tetap biaya total run-menit bertambah) — kalau ini kerasa
berat/mahal, `on:` job `instrumentation-tests` ini sendiri bisa diubah ke `workflow_dispatch`
manual saja di batch berikutnya; belum dilakukan di sini krn gap list eksplisit minta test
BENAR-BENAR tereksekusi, bukan cuma tertulis dan didiamkan.
- **Belum pernah benar-benar dijalankan** — tidak ada akses emulator/device Android di sesi kerja
batch ini. Confidence berdasar: (a) pola resmi Media3/androidx.test yang sudah lama stabil dan
dicek detail thread-safety-nya manual (bukan tebakan, lihat 3 poin teknis di atas), (b)
`Futures`/`MoreExecutors`/`ListenableFuture` yang sudah TERBUKTI kompail di file lain proyek ini
sebelum batch ini ditulis. Titik paling mungkin gagal pertama kali kalau CI merah: konfigurasi
KVM `reactivecircus/android-emulator-runner` yang kebijakan runner GitHub-nya bisa berubah
sewaktu-waktu, atau nama task `connectedDebugAndroidTest` yang perlu persis sesuai variant/
applicationId proyek ini — kalau CI gagal di titik ini, kirim `log_fail_*`/artifact
`instrumentation_test_report_*`-nya, bukan `build-output.log` job `build` (beda job, beda log).

## Batch 102 — Gap List #1: True Crossfade (dual-instance overlap sungguhan)
Dari `AudioPlayer_Coding_Gap_List.md` (upload user), P0 pertama di daftar prioritas: "Fade
Halus" sebelumnya cuma efek volume 1 pemutar, bukan crossfade sungguhan. **4 file** (1 baru, 3
diedit — 1 di antaranya `PlaybackService.kt`, file paling berisiko di proyek ini per catatan
class doc-nya sendiri, edit parsial).

**Kenapa ini sulit, dan pendekatan yang DIHINDARI**: cara paling jelas kelihatan untuk "true
crossfade" adalah dua `ExoPlayer` yang gantian jadi pemilik `MediaSession` lewat
`MediaSession.setPlayer()`. Sebelum menulis kode, ini dicek dulu lewat web search — hasilnya:
API itu memang ada, tapi dilaporkan tidak reliable (GitHub issue `androidx/media#764`, "the
entire media session just seems to end when I switch players in this way"), dan alternatif
resmi yang disarankan maintainer-nya (`ForwardingSimpleBasePlayer`) baru tersedia dari media3
1.4.0 — proyek ini pin di 1.3.1 (`build.gradle.kts`), dan sudah 2x kena insiden dari bump versi
dependency yang dipaksakan tanpa akses compiler untuk verifikasi (Batch 23/24, Batch 29).
Bumping media3 demi 1 fitur, dengan risiko harus ikut memperbaiki API session lain yang mungkin
berubah bentuk (`onPlaybackResumption`, dll — sudah pernah jadi masalah sebelumnya juga), dinilai
tidak sepadan untuk batch ini.

**Desain yang dipakai** — `playback/CrossfadeEngine.kt` (baru): `sessionPlayer` (ExoPlayer yang
sudah ada, dipegang `MediaSession`) TIDAK PERNAH diganti. `overlapPlayer`: ExoPlayer kedua,
privat, tidak pernah diekspos ke session/notifikasi/UI, hanya pegang SATU `MediaItem` (track
berikutnya) di satu waktu.

1. **Mulai overlap** (`maybeStartCrossfade()`, dipanggil tiap tick polling 250ms dari
`PlaybackService`) — begitu sisa waktu `sessionPlayer` \<= 3 detik (durasi crossfade tetap,
belum dibuat bisa diatur user — lihat "Belum digarap"), item berikutnya diambil lewat
`sessionPlayer.nextMediaItemIndex` (API stabil Player interface, otomatis sudah menghormati mode
shuffle & repeat yang sedang aktif — TIDAK ada logic shuffle/repeat baru ditulis ulang di sini
sama sekali, jadi nol risiko baru di area itu). `overlapPlayer` di-`setMediaItem`+`prepare`+
`play` dari volume 0, lalu volume di-ramp bersilangan: `sessionPlayer` turun ke 0, `overlapPlayer`
naik ke volume target — **dua sumber suara sungguhan tumpang tindih di output audio selama
jendela itu**, ini bagian yang sebelumnya tidak ada sama sekali (dulu cuma 1 pemutar, jeda senyap
tetap ada walau disamarkan volume).

2. **Handback senyap** (`onSessionAutoTransition()`, dipanggil dari
`onMediaItemTransition(reason=AUTO)`) — `sessionPlayer` DIBIARKAN mencapai transisi otomatisnya
sendiri tanpa campur tangan sama sekali (lagi-lagi: queue/timeline-nya tidak disentuh). Begitu
itu terjadi, volumenya sudah ~0 (hasil ramp di atas) — jadi AMAN diseek diam-diam ke posisi
`overlapPlayer.currentPosition` (seek yang tidak terdengar karena volume nol, ini kunci kenapa
tidak perlu trik lebih rumit), lalu kendali ditukar balik lewat ramp pendek 400ms
(`sessionPlayer` naik, `overlapPlayer` turun ke 0 lalu di-`pause`+`clearMediaItems`). Karena
posisi keduanya persis sinkron (hasil seek), ramp balik ini tidak menghasilkan gema/dobel suara.

3. **Pembatalan aman** — `onSessionManualDiscontinuity(reason)` dipanggil dari
`onPositionDiscontinuity` `sessionPlayer` utk SEMUA reason; kalau `SEEK` (skip tombol, seek bar,
headset, notifikasi, lock screen, Bluetooth — apa pun yang bikin `Player` diseek dari luar) DAN
bukan seek internal milik langkah 2 di atas (dibedakan lewat flag `internalSeekInFlight`),
crossfade yang sedang jalan langsung dibatalkan (`overlapPlayer` di-pause+clear, volume
`sessionPlayer` dipulihkan). `onSessionPlayWhenReadyChanged(isPlaying)` dipanggil dari
`onIsPlayingChanged` — pause manual mid-crossfade ikut membekukan `overlapPlayer`, resume ikut
melanjutkannya, supaya tidak ada skenario lagu berikutnya kedengaran main sendiri padahal user
sudah menekan jeda. Repeat-one (`Player.REPEAT_MODE_ONE`) di-skip total di
`maybeStartCrossfade()` — "next" track dlm mode itu adalah dirinya sendiri, bukan target
crossfade yang masuk akal, ini poin eksplisit di gap list ("pastikan repeat-one tidak memicu
crossfade yang salah"). `overlapPlayer.onPlayerError` fail-safe: kalau file berikutnya gagal
disiapkan (terhapus/rusak/izin dicabut sejak queue dibangun), crossfade dibatalkan bersih —
sengaja tidak pernah membiarkan `sessionPlayer` macet di volume rendah tanpa jalan keluar.

**`PlaybackService.kt`** (protected, edit parsial) — `overlapPlayer` dibangun dengan
`setAudioAttributes(audioAttributes, /*handleAudioFocus=*/false)` +
`setHandleAudioBecomingNoisy(false)` (sengaja: cuma `sessionPlayer` yang boleh urus fokus audio
& auto-pause headset lepas — dua `ExoPlayer` sama-sama minta fokus akan bentrok). Custom
`SessionCommand` baru `ACTION_SET_CROSSFADE_ENABLED`/`EXTRA_CROSSFADE_ENABLED` didaftarkan di
`onConnect`, pola identik `ACTION_SET_SKIP_SILENCE` yang sudah ada (jembatan satu-satunya dari
`MediaController` sisi UI ke `ExoPlayer`/`CrossfadeEngine` mentah sisi Service — `Player`
interface umum tidak mengekspos ini). Loop `serviceScope.launch { while(isActive) {...; delay
(250) } }` baru — no-op murah lewat guard `enabled`/`isPlaying` internal `CrossfadeEngine` kalau
fitur mati/lagi tidak main. `overlapPlayer.release()` eksplisit di `onDestroy` (tidak ikut kebawa
`mediaSession.player.release()` krn memang bukan bagian dari session).

**`PlayerViewModel.kt`** — `startFadeIn()`/`startFadeOut()`/`animateVolume()`/`fadeJob`/
`fadedOutForIndex`/konstanta `FADE_DURATION_MS`(3000L)/`FADE_FLOOR`(0.15f) dihapus total, semua
logic-nya pindah ke `CrossfadeEngine` (server-side, krn ExoPlayer mentah tidak pernah diekspos ke
ViewModel yang cuma pegang `MediaController`). `setCrossfadeEnabled(Boolean)` sekarang relay
lewat custom command di atas, bukan lagi manipulasi volume langsung. `crossfadeEnabled:
StateFlow<Boolean>` dan signature `setCrossfadeEnabled()` TIDAK berubah — jadi UI konsumen
(`NowPlayingScreen.kt`) tidak perlu perubahan logic, cuma teks subtitle toggle "Fade Halus"
diperbarui (dulu bilang "volume melandai", sekarang jujur bilang "lagu berikutnya mulai main
sebelum lagu ini habis"). `README.md` § *Catatan jujur soal Gapless Playback* diperbarui senada.

**Belum digarap / batasan disadari** (dicatat, bukan terlewat — lihat juga `PROJECT_STATE.md`):
- Durasi crossfade masih hardcode 3000ms (`CrossfadeEngine.crossfadeDurationMs`), belum ada UI
untuk user atur sendiri (mis. slider 1–8 detik) — gap list tidak secara eksplisit minta ini utk
item #1, jadi sengaja belum ditambah supaya batch ini tetap fokus 1 hal.
- Equalizer/Visualizer terikat ke `PlaybackAudioSession.sessionId` milik `sessionPlayer`;
`overlapPlayer` (ExoPlayer/AudioTrack terpisah) punya audio session id sendiri, jadi EQ/
visualizer belum ikut memengaruhi ~3 detik overlap suara lagu yang baru masuk. Sempit dampaknya
(kedua fitur opt-in), belum diprioritaskan.
- Slider volume yang digeser tepat saat crossfade sedang ramp akan terasa "menyusul" sesaat —
ramp ini menulis `sessionPlayer.volume` tiap tick sampai selesai (<3 detik). Transient, bukan bug
fungsional, didokumentasikan sebagai trade-off sadar di `CrossfadeEngine.kt` sendiri.
- **Belum pernah di-build fisik** — tidak ada akses compiler/Gradle di sesi kerja batch ini.
Confidence "seharusnya benar" berdasar API Media3 yang sudah lama stabil lintas versi
(`Player.Listener`, `ExoPlayer.Builder`, `seekTo`/`setVolume`/`clearMediaItems`/
`nextMediaItemIndex`), BUKAN dari hasil compile aktual — dicek satu per satu manual, bukan
tebakan. Kalau ada error compile, titik paling mungkin: nama konstanta
`Player.MEDIA_ITEM_TRANSITION_REASON_AUTO`/`Player.DISCONTINUITY_REASON_SEEK` atau signature
`onPositionDiscontinuity` 3-parameter di versi media3 1.3.1 yang terpasang persis.

## Batch 101 — Adaptive layout multi-device (rail + two-pane) & undo hapus playlist
Instruksi user: audit UX/frontend, gabung semua perbaikan (kecuali TalkBack/Tema/Lokalisasi)
dalam 1 batch, utamakan multi-device/adaptive layout. **5 file** (1 baru, 4 diedit — 1 di
antaranya `MainActivity.kt`, protected, edit parsial).

**1. Adaptive layout — celah UX terbesar dari audit** (roadmap baru, bukan dari 15 fitur
offline). Sebelumnya app fixed layout HP di SEMUA lebar layar — tablet/foldable
terbuka/Chromebook/split-screen lebar tetap dapat satu kolom sempit dgn ruang horizontal
nganggur besar, plus NavigationBar bawah termakan gesture bar di layar lebar.

`ui/adaptive/WindowAdaptive.kt` (baru) — `AppWidthClass` (COMPACT/MEDIUM/EXPANDED) dihitung
dari `LocalConfiguration.screenWidthDp`, breakpoint 600dp/840dp SAMA PERSIS rekomendasi resmi
Material 3 window size classes. Sengaja TIDAK menambah dependency
`material3-window-size-class` baru di `build.gradle.kts` (protected) — breakpoint dp manual
sudah cukup akurat utk kebutuhan app ini (pilih rail-vs-bar & satu-vs-dua-pane), 0 risiko
tambahan di dependency graph. Reactive otomatis thd rotasi/lipat-buka foldable/resize
split-screen krn `LocalConfiguration` sendiri sudah reactive.

`MainActivity.kt` (protected, edit parsial) — 2 perubahan struktural di `AppNavHost`:
- **NavigationRail** menggantikan `NavigationBar` bawah di Medium/Expanded (guard
  `widthClass == COMPACT` ditambah ke kondisi render `NavigationBar` yang sudah ada) — Compact
  (HP potret biasa, mayoritas user) **0 perubahan perilaku**, NavigationBar bawah tetap identik.
- **Two-pane** di Expanded: `NowPlayingScreen(...)` (dulu inline sekali di
  `composable("now_playing")`) diekstrak jadi 1 lambda `nowPlayingContent(onBack)` dipakai 2
  tempat — layar penuh seperti biasa (Compact/Medium, `onBack = popBackStack()`) DAN panel
  persisten 420dp di sisi kanan (Expanded selama ada lagu aktif, `onBack = {}` krn panel bukan
  entry back-stack). `MiniPlayerBar` + `NavigationBar` disembunyikan otomatis saat panel
  tampil (`showTwoPane`) supaya kontrol transport tidak dobel.

**Belum digarap** (di luar scope batch ini, murni styling bukan layout): dua-pane utk
Library→detail folder atau Playlist→isi playlist — skip krn kompleksitas restrukturisasi nav
graph tidak sepadan blast-radius-nya utk batch pertama fitur ini, NowPlaying yang paling sering
dibuka jadi prioritas.

**2. Undo hapus playlist** — audit temukan `deletePlaylist()`/`deleteSmartPlaylist()` hapus
PERMANEN 1 tap tanpa jalan balik sama sekali (beda dari `removeFromQueue`/
`removeSongFromPlaylist` yang sejak awal sudah pakai pola `UndoableAction` + Snackbar
"Urungkan"). `PlaylistStore.restorePlaylist()`/`SmartPlaylistStore.restoreSmartPlaylist()`
(baru) simpan balik objek APA ADANYA (id/nama/isi asli, bukan lewat `create*()` yang generate
id baru) sebelum dihapus, `PlayerViewModel` kedua fungsi delete sekarang snapshot dulu +
publish `UndoableAction` — konsisten dgn pola undo yang sudah ada, 0 dialog konfirmasi baru
ditambahkan (sengaja, ikut konvensi snackbar-undo yang sudah dipakai di app ini alih-alih
modal).

**Ditemukan TAPI TIDAK digarap (sudah bukan gap nyata setelah dicek kode)**:
- *Loading state Playlist/SmartPlaylist*: keduanya baca SharedPreferences sinkron (bukan I/O
  async) — 0 loading state yang berguna ditambahkan di sini, audit awal kurang presisi.
- *Predictive back*: manifest sudah `enableOnBackInvokedCallback="true"` (ada sebelum batch
  ini) + `ModalBottomSheet`/`NavHost` M3 menangani back gesture standar lewat itu — tidak ada
  bug konkret ditemukan yang butuh kode tambahan di compose-bom 2024.05.00 saat ini.

Belum di-build fisik.

## Batch 100 — Floating Mini Player: minimize ke tepi, auto-trigger tanpa buka app, QS Tile
Lanjutan 3 instruksi user yang sebelumnya sempat ditangani sebagian di sesi lain (Batch 98
cuma menuntaskan foreground service, 2 celah lain terlewat/salah dibaca). **7 file** (4 baru,
3 diedit — 2 di antaranya protected).

**1. Minimize ke tepi layar (koreksi keputusan Batch 98)** — CHANGELOG Batch 98 mencatat
"tombol dismiss/close di bubble itu sendiri TIDAK ditambahkan... ditolak" — itu salah baca
instruksi. Permintaan aslinya jelas: "wajib bisa di-minimize... BUKAN di-close total", beda
konsep dari dismiss. Diperbaiki di sini: `FloatingBubbleService.kt`'s `bubbleView` sekarang
`FrameLayout` berisi 2 child (`bubble_mini_player.xml` pill penuh yang sudah ada + `bubble_
minimized.xml` baru, tab bundar 48dp), toggle visibility antara keduanya lewat `minimize()`/
`expand()` — Service & notifikasi foreground TIDAK pernah berhenti, cuma tampilannya yang
menciut. Tombol baru `bubble_minimize` (ikon `ic_bubble_minimize.xml`, chevron Material
standar) di pill. `snapMinimizedToNearestEdge()`: begitu minimized, X SELALU dipaksa ke tepi
0 atau `screenWidth - lebarTab` terdekat (chat-head style, gaya Messenger) — dipanggil saat
tap minimize, saat lepas drag ketika minimized, DAN saat rotasi layar (`onConfigurationChanged`
sekarang re-snap penuh alih-alih cuma clamp untuk state ini). `expand()` memulihkan X terakhir
sebelum diminimize (`lastExpandedX`, in-memory), di-clamp ulang via `container.post{}` supaya
lebar pill yang baru saja terlihat lagi sudah ke-measure sebelum dipakai hitung batas kanan.
Tap-vs-drag di tab minimized pakai ulang pembeda `totalMovement` yang sudah ada — tap
memanggil `expand()`, bukan `openApp()` seperti pill biasa. `FloatingBubbleStore.kt`: `KEY_
MINIMIZED` baru, dibaca ulang tiap Service start supaya sesi berikutnya lanjut dari state
terakhir.

**2. Auto-trigger tanpa buka app** — sebelumnya bubble CUMA start dari `MainActivity` (buka
app manual) atau `BubbleBootReceiver` (reboot HP) — tekan play di WIDGET homescreen, tombol
notifikasi media, atau tombol headset SAAT APP BELUM PERNAH DIBUKA sama sekali tidak pernah
menyalakan bubble walau togglenya ON. `PlaybackService.kt`'s `onIsPlayingChanged(true)` — SATU
titik yang selalu jalan di titik manapun playback benar-benar mulai, dari entry point apa
pun — sekarang panggil `maybeStartFloatingBubble()` baru: cek ulang `FloatingBubbleStore.
isEnabled()` DAN `Settings.canDrawOverlays()` (2 syarat yang sama seperti `BubbleBootReceiver`,
device settings selalu menang atas preferensi in-app), baru start service kalau keduanya
lolos. Aman dipanggil berulang tiap event play — start service yang sudah jalan cuma
re-deliver `onStartCommand()`, `onCreate()` tidak pernah dipanggil ulang, tidak ada risiko
window overlay dobel ke-`addView()`.

**3. Quick Settings Tile baru** — `BubbleTileService.kt` (baru, `bubble/`), toggle bubble
langsung dari shade notifikasi tanpa buka app sama sekali. Baca/tulis preferensi LANGSUNG ke
`FloatingBubbleStore` (SharedPreferences) — BUKAN lewat `PlayerViewModel`'s StateFlow, karena
System UI bisa instansiasi `TileService` kapan pun tanpa `MainActivity`/ViewModel pernah hidup
di sesi itu. Kalau izin overlay belum granted, tap tile buka `Settings.ACTION_MANAGE_
OVERLAY_PERMISSION` lewat `startActivityAndCollapse` (bukan langsung toggle) — API-gated:
overload `PendingIntent` di API 34+ (yang lama di-`@Suppress("DEPRECATION")` untuk di bawahnya,
QS Tile custom sendiri baru ada sejak API 24/`minSdk` 23 di project ini, makanya class ini
diberi `@RequiresApi(Build.VERSION_CODES.N)` — device API 23 tidak pernah menginstansiasi
class ini sama sekali karena fitur QS Tile custom belum ada di situ, tapi anotasi tetap wajib
biar lint `NewApi` tidak menganggap ini API di bawah `minSdk` tanpa pengaman). Ikon baru
`ic_bubble_tile.xml` (lingkaran cincin generik, QS tile system selalu render 1 warna terlepas
dari isi file). Manifest: `<service>` baru `exported="true"` + `permission="...BIND_QUICK_
SETTINGS_TILE"` (kontrak resmi API ini, BEDA dari service internal lain di file yang sengaja
`exported="false"`) + intent-filter `QS_TILE`.

**4. Sinkronisasi state Settings ↔ Tile** — konsekuensi jujur dari poin 3: kalau app KEBETULAN
sedang kebuka bareng saat tile ditoggle, switch bubble di SettingsScreen bisa nunjukin state
BASI (`StateFlow` tidak auto-observe perubahan `SharedPreferences` dari komponen lain).
`PlayerViewModel.kt`: `refreshFloatingBubbleEnabled()` baru, baca ulang dari store.
`MainActivity.kt` (protected, edit parsial): `DisposableEffect(lifecycleOwner)` + `Lifecycle
EventObserver` manual (BUKAN `LifecycleEventEffect` — proyek ini pernah kena masalah nyata
soal `LocalLifecycleOwner` CompositionLocal, lihat Batch 23-24, `addObserver()` manual adalah
API lifecycle polos yang tidak lewat titik gagal historis itu) memanggil `refreshFloating
BubbleEnabled()` tiap `ON_RESUME`.

**AndroidManifest.xml (protected, edit parsial)**: `<service>` `BubbleTileService` baru
(detail poin 3 di atas). Tidak ada `<uses-permission>` baru — `BIND_QUICK_SETTINGS_TILE`
adalah `android:permission` di level `<service>` (App INI yang mensyaratkan siapa pun yang
mem-bind harus punya izin itu — System UI sudah otomatis punya), bukan izin yang app ini minta
sendiri, beda kelas dari `SYSTEM_ALERT_WINDOW`/`FOREGROUND_SERVICE_SPECIAL_USE` sebelumnya.

**Verifikasi**: brace/paren balance dicek semua file .kt yang disentuh (seimbang semua), semua
file .xml baru/diedit divalidasi well-formed lewat `xml.etree.ElementTree` (valid semua),
`FILE_MANIFEST.txt` di-diff ulang terhadap tree aktual (140/140 cocok setelah update). Belum
di-build fisik (tidak ada JDK/Android SDK di sandbox ini, sama seperti semua batch sebelumnya)
— prioritas berikutnya kalau user push & build: (1) minimize/expand tidak nge-lag atau
nyisain artefak visual pas toggle cepat berturut-turut, (2) tab minimized benar-benar nempel
tepi & tidak kepotong sebagian di device nyata (khususnya device dengan notch/punch-hole di
tepi layar), (3) QS tile state (`STATE_ACTIVE`/`STATE_INACTIVE`) ke-refresh benar begitu shade
dibuka ulang, (4) auto-trigger dari widget play benar-benar memunculkan bubble di device yang
app-nya belum pernah dibuka sama sekali sejak install.

## Batch 99 — Audit kompatibilitas mundur Android 14 ke bawah (0 kode diubah)
Instruksi user: "terapkan backward compatibility support untuk Android 14 kebawah". Fokus
audit: kode `specialUse` foreground service dari Batch 98 (fitur khusus Android 14/API 34,
paling berisiko pecah di versi lebih rendah) + audit ulang menyeluruh SEMUA titik version-gate
di proyek, bukan cuma yang baru.

**Metode**: `grep -rn "Build.VERSION.SDK_INT\|Build.VERSION_CODES"` di seluruh `app/src/main/
java` — ketemu 28 titik di 10 file. Tiap titik dicek manual: (1) API level minimum ASLI dari
symbol/constant/method yang dipanggil (dicocokkan ke dokumentasi resmi Android per level), (2)
apakah pemanggilannya benar-benar dibungkus `if (SDK_INT >= level_yang_benar)` dengan fallback
yang valid di cabang else-nya untuk versi di bawahnya.

**Hasil — 0 bug ditemukan, 0 file diubah**. Titik-titik kunci yang diverifikasi:
- `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` (constant API 34) — di-inline compiler
  jadi integer literal saat build (bukan lookup runtime ke class platform), dan jalur
  pemanggilannya sendiri SUDAH dibungkus `SDK_INT >= UPSIDE_DOWN_CAKE` di
  `FloatingBubbleService.startForegroundWithNotification()` — aman dipanggil di device API
  berapa pun, termasuk di bawah 34, karena baris itu memang tidak pernah dieksekusi di sana.
- 3-arg `startForeground(id, notification, type)` (method-nya sendiri ada sejak API 29) — kode
  ini malah LEBIH konservatif dari yang wajib: baru dipakai mulai API 34 (bukan 29), karena
  tidak ada tipe FGS lain yang cocok untuk rentang 29-33. Di bawah 34 fallback ke overload
  2-argumen lama.
- `<service android:foregroundServiceType="specialUse">` + `<property android:name="android.
  app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE">` di manifest — atribut biner statis yang di-resolve
  AAPT2 jadi integer SAAT BUILD (pakai compileSdk 34), bukan divalidasi ulang terhadap versi OS
  device saat parsing di runtime — OS lama membaca int itu tanpa tahu/peduli namanya
  "specialUse", tidak menyebabkan crash instalasi maupun runtime di device manapun ≥ minSdk 23.
- 6 titik lain (`NotificationChannel`/`IMPORTANCE_MIN` API 26, `TYPE_APPLICATION_OVERLAY` API
  26, `startForegroundService()` API 26, `STOP_FOREGROUND_REMOVE` API 24, `loadThumbnail()` API
  29, plus semua titik version-gate pra-Batch-95 di `PlaybackService.kt`/`WidgetUpdater.kt`/
  `AudioArtFetcher.kt`/`AccentColorExtractor.kt`/`MusicRepository.kt`/`ApkSignatureChecker.kt`/
  `MainActivity.kt`) — semua sudah dibungkus `if/else` yang benar dengan fallback API lama yang
  valid, pola konsisten di seluruh proyek sejak batch-batch sebelumnya.
- Satu titik REDUNDAN (bukan bug, cuma tidak perlu): `BubbleBootReceiver`'s
  `SDK_INT >= Build.VERSION_CODES.M` sebelum `Settings.canDrawOverlays()` — karena `minSdk`
  proyek ini sudah 23 (=M persis), kondisi itu SELALU true di device manapun yang bisa install
  app ini sama sekali. Dibiarkan apa adanya (defensif eksplisit tidak salah, cuma tidak
  esensial) — tidak disentuh karena bukan risiko, hanya gaya penulisan.

**Kesimpulan**: proyek ini SUDAH backward-compatible penuh sampai `minSdk 23` (Android 6.0)
termasuk fitur Android 14-only terbaru (Batch 98). Tidak ada perubahan kode yang diperlukan
dari audit ini. Kalau user pernah mengalami crash/perilaku aneh spesifik di device Android
tertentu, laporkan versi Android + langkah reproduksi supaya bisa diselidiki spesifik — audit
statis ini tidak menggantikan pengujian di device fisik sungguhan (sama seperti seluruh proyek,
belum ada JDK/Android SDK di sandbox untuk build/run nyata).

## Batch 98 — Sempurnakan Floating Mini Player (Bubble): reliabilitas & completeness
Lanjutan instruksi user "sempurnakan 100% fungsionalitas Floating Mini Player" — Batch 97
(sesi sebelumnya) sudah membenahi 1 bug jank main-thread tapi sengaja TIDAK menyentuh 3 celah
completeness lain yang sudah teridentifikasi sejak Batch 95 sendiri ("batasan jujur" di
docstring-nya). Batch ini menutup celah-celah itu: **4 file** (1 baru, 3 diedit — 1 di
antaranya protected).

**1. Jadi foreground service beneran** (celah terbesar): sebelumnya `FloatingBubbleService`
BUKAN foreground — cuma mengandalkan window overlay yang tampil menaikkan importance proses
"mendekati visible", dan skin Android agresif tetap bisa membunuhnya kapan saja (persis yang
dicatat sebagai "batasan jujur" di Batch 95). Sekarang `startForeground()` dipanggil beneran
di `onCreate()`, tipe `specialUse` (API 34+ belum punya kategori resmi utk "overlay window",
`specialUse` adalah kategori umum yang dimaksudkan persis utk kasus begini — butuh
`FOREGROUND_SERVICE_SPECIAL_USE` permission baru + `<property android:name="android.app.
PROPERTY_SPECIAL_USE_FGS_SUBTYPE">` di manifest). **Trade-off yang disadari & didokumentasikan
jujur**: nambah 1 notifikasi importance MIN selama bubble aktif — hampir tidak kelihatan (MIN
disembunyikan dari status bar icon, cuma muncul kalau notification shade ditarik turun) tapi
tetap ada, demi kepastian bubble TIDAK dibunuh OS selama masih dianggap app aktif (level
proteksi yang sama seperti `PlaybackService`). Channel/notification builder meniru persis pola
`PlaybackService.startForegroundColdStartNotification()` untuk konsistensi gaya.

**2. Auto-restart setelah reboot HP**: sebelumnya bubble cuma restart lagi kalau user MEMBUKA
app secara manual (`MainActivity`'s `LaunchedEffect(Unit)`) — HP di-restart + user belum sempat
buka app = bubble mati permanen walau togglenya sebenarnya ON. `BubbleBootReceiver.kt` (baru,
`bubble/`) dengar `BOOT_COMPLETED`, cek ulang `FloatingBubbleStore.isEnabled()` DAN
`Settings.canDrawOverlays()` (izin overlay bisa dicabut user dari Pengaturan sistem kapan saja
tanpa lewat toggle app ini sama sekali — device settings selalu menang atas preferensi in-app),
baru restart service kalau keduanya lolos. `LaunchedEffect` di `MainActivity` TETAP ada sebagai
jaring pengaman kedua untuk kasus proses mati TANPA reboot (force-stop manual / OOM kill).

**3. State antrean kosong ditangani**: sebelumnya tombol play/prev/next tetap "aktif" penuh
walau tidak ada lagu dimuat sama sekali di `PlaybackService` — tap play jadi no-op senyap yang
membingungkan (`ExoPlayer` kosong tidak melakukan apa-apa, tanpa umpan balik apa pun ke user).
`refreshBubbleContent()` sekarang cek `player.mediaItemCount > 0` tiap update, simpan ke
`hasQueue`. Kalau kosong: 3 tombol jadi alpha 0.4 (tetap kelihatan bentuknya, pill tidak
"loncat" ukuran) dan tap-nya membuka app alih-alih coba mainkan apa pun. Default `hasQueue =
true` sebelum controller sempat konek (optimistic) — supaya tap paling awal tetap jatuh ke
fallback Intent lama yang sudah ada, bukan langsung dianggap kosong secara keliru.

**4. Posisi bubble di-clamp ulang saat rotasi layar**: sebelumnya rotasi bisa membuat bubble
kepental separuh di luar layar (mis. `y` besar yang valid di portrait jadi melebihi tinggi
layar landscape yang lebih pendek) sampai user drag manual untuk "menemukan" bubble-nya lagi.
`onConfigurationChanged()` baru re-clamp `layoutParams` (dipromosikan dari local var ke field
class supaya bisa diakses dari sini) ke `DisplayMetrics` terkini, `updateViewLayout()` +
`savePosition()` kalau posisi berubah. Sekalian: `DisplayMetrics` di `setupDrag()`'s
`ACTION_MOVE` sekarang dibaca ULANG tiap event (sebelumnya di-cache sekali di awal — device
rotasi PAS lagi di-drag akan pakai metrics basi, celah kecil tapi nyata).

**MainActivity.kt (protected, edit parsial)**: 3 titik pemanggilan service (permission-granted
callback, `toggleFloatingBubble`, `LaunchedEffect`) disatukan ke 1 helper `startBubbleService()`
baru yang API-gated (`startForegroundService()` di O+, `startService()` di bawahnya) — WAJIB
sekarang karena Service benar-benar memanggil `startForeground()` sendiri (sebelumnya `startService()`
polos masih "kebetulan jalan" karena Service tidak pernah benar-benar foreground).

**Di luar cakupan, disengaja** (konsisten pola "Catatan jujur" proyek — lihat README §
Gapless Playback untuk pola serupa): tombol dismiss/close di bubble itu sendiri TIDAK
ditambahkan (drag-to-dismiss dipertimbangkan, ditolak — risiko UX nyata: dismiss diam-diam
tanpa konfirmasi/umpan balik visual terasa seperti bug, bukan fitur; Settings toggle tetap
satu-satunya jalur mematikan, sudah cukup jelas & aman). Threshold/sensitivitas custom untuk
apa pun TIDAK relevan di sini (itu domainnya Batch 96/Silence Skip, bukan bubble). "Belum
diverifikasi di device fisik" (Batch 95/97) masih berlaku sama — tidak ada JDK/Android SDK di
sandbox ini untuk build/run sungguhan.

**Verifikasi**: brace/paren balance dicek manual di semua file yang disentuh — seimbang di
semua. Belum di-build fisik (sama seperti semua batch sebelumnya) — prioritas berikutnya kalau
user push & build: (1) notifikasi MIN benar-benar tidak mengganggu di device asli, (2) bubble
survive force-stop + reboot dengan urutan test toggle ON → reboot → cek bubble muncul TANPA
buka app, (3) rotasi layar berulang tidak pernah membuat bubble hilang dari jangkauan.

## Batch 97 — Sempurnakan Floating Mini Player (Bubble): fix jank main-thread
Instruksi user: "sempurnakan 100% fungsionalitas dari Floating Mini Player (Bubble Mode)".
Audit statis `FloatingBubbleService.kt` (Batch 95) menemukan 1 bug nyata, 1 file disentuh.

**Bug — artwork decode blocking main thread**: `refreshBubbleContent()` (dipanggil dari
`Player.Listener.onEvents()`, main thread) memanggil `loadAlbumArtBitmap()` secara SINKRON —
`contentResolver.loadThumbnail()` (API 29+) atau `MediaMetadataRetriever` (fallback lama)
keduanya I/O blocking. Root cause class-nya SAMA PERSIS dengan widget jank yang sudah pernah
diperbaiki di Batch 34/35 ("decode bitmap sinkron di main thread tiap event ganti lagu/toggle
play"), tapi dampaknya di sini berpotensi lebih terasa: bubble ini window overlay yang digambar
di ATAS app lain apa pun yang sedang dibuka user — jank di titik ini bukan cuma bikin AudioPlayer
sendiri seret, tapi berisiko nge-stutter UI thread app manapun yang kebetulan lagi dipakai user
saat itu.

**Fix**: `bubbleScope` baru (`CoroutineScope(Dispatchers.Main + Job())`, pola identik
`serviceScope` di `PlaybackService.kt`) + `bubbleArtJob` (pola identik `widgetUpdateJob`) —
`bubbleArtJob?.cancel()` dipanggil SEBELUM tiap relaunch (skip/next cepat berturut-turut tidak
lagi berisiko hasil decode lagu lama landing belakangan menimpa art lagu yang lebih baru, race
condition yang sebelumnya mungkin walau jarang kena karena decode-nya sendiri sinkron/blocking).
Decode pindah ke `withContext(Dispatchers.IO)`, update `ImageView` balik ke main thread via
`bubbleScope.launch`. Icon play/pause (`setImageResource`, murni ganti drawable resource, 0 I/O)
sengaja TETAP sync — tidak ada alasan menambah kompleksitas async untuk yang sudah murah.
`bubbleScope.cancel()` ditambah di `onDestroy()` (mencegah job in-flight coba update View yang
sudah dilepas dari `WindowManager` kalau Service di-kill selagi decode masih jalan — dijaga
ganda dengan re-`findViewById` dari `bubbleView` terbaru, bukan closure `view` lama, tepat
sebelum update UI).

**Di luar cakupan, disengaja**: fitur/kontrol baru (mis. tombol tutup/dismiss di bubble,
mengonversi jadi foreground service) TIDAK ditambahkan — permintaan "sempurnakan
fungsionalitas" dibaca sebagai "benarkan bug/gap yang mengganggu fungsi yang SUDAH ada", bukan
memperluas scope 3-tombol pill sesuai roadmap asli. Status "bukan foreground service" tetap
keputusan desain sengaja dari Batch 95 (lihat docstring kelas ini) — belum ada laporan/bukti
baru yang mengubah trade-off itu, jadi tidak disentuh ulang tanpa alasan konkret. "Belum
diverifikasi di device fisik" (Batch 95) masih berlaku sama seperti sebelumnya.

**Verifikasi**: brace/paren balance dicek otomatis (seimbang, 52/52 `{}`, 139/139 `()`).
**Belum diverifikasi compile/runtime Gradle sungguhan** (tidak ada JDK/Android SDK/kotlinc di
sandbox ini, sama seperti semua batch sebelumnya) — prioritas berikutnya kalau user push:
`./gradlew assembleDebug`, lalu cek di device khususnya (1) bubble tetap responsif/tidak nge-lag
app lain saat skip lagu cepat berturut-turut, (2) art tidak pernah salah tampil (lagu lama
menimpa lagu baru) saat spam next/previous.

## Batch 96 — Fitur baru: Trim Keheningan Otomatis / Silence Skip (roadmap item #8)
Dari `ROADMAP_15_FITUR_OFFLINE.md` item #8 (Kompleksitas: Sedang-Tinggi, awalnya dikira butuh
analisis amplitude PCM manual). Toggle baru "Lewati Keheningan Otomatis" di Settings —
mempercepat bagian hening saat playback tanpa perlu buka app.

**Temuan yang memangkas kompleksitas drastis**: Media3/ExoPlayer 1.3.1 sudah punya solusi
bawaan `ExoPlayer.setSkipSilenceEnabled(Boolean)` (memakai `SilenceSkippingAudioProcessor`
internal di audio pipeline) — draf awal roadmap ini mengira perlu riset baca frame PCM manual
sendiri; ternyata tidak sama sekali. **0 kode analisis amplitude custom ditulis di batch ini.**

**Kenapa perlu custom SessionCommand (bukan langsung panggil dari ViewModel)**:
`setSkipSilenceEnabled()` adalah method milik `ExoPlayer` secara spesifik, BUKAN bagian
interface `Player` umum yang diekspos `MediaController` — `PlayerViewModel` (yang cuma pegang
`MediaController`, bukan `ExoPlayer` asli, karena hidup di proses UI sedangkan `ExoPlayer` asli
hidup di `PlaybackService`) tidak bisa memanggilnya langsung. Dijembatani lewat 1
`SessionCommand` baru: `PlaybackService.ACTION_SET_SKIP_SILENCE` diadvertise di
`PlaybackSessionCallback.onConnect()` (ditambahkan ke atas `DEFAULT_SESSION_COMMANDS` lewat
`.buildUpon().add(...)`), ditangani di `onCustomCommand()` baru yang meng-cast
`mediaSession?.player as? ExoPlayer` lalu memanggil `setSkipSilenceEnabled()` langsung.
`PlayerViewModel.setSilenceSkipEnabled()` mengirim command ini via
`controller?.sendCustomCommand(...)` sekaligus simpan preferensi ke `SilenceSkipStore.kt`
(baru, `data/`, pola identik `ShakeSettingsStore`) — kalau `controller` belum sempat konek
saat toggle ditekan, tidak fatal: `PlaybackService.onCreate()` sendiri membaca
`SilenceSkipStore` langsung saat `ExoPlayer` pertama kali dibuat, jadi tetap sinkron begitu
Service benar-benar start (2 jalur baca yang saling melengkapi, bukan saling gantung).

**Default & UI**: OFF by default (sesuai catatan risiko roadmap sendiri — threshold bawaan bisa
memotong intro/outro yang memang senyap secara musikal, bukan cuma silence teknis), teks di
Settings menyebutkan jujur ini pakai "deteksi bawaan Media3" tanpa slider sensitivitas custom,
dan menyarankan user coba dulu lalu matikan kalau terasa mengganggu — bukan diklaim sempurna.
**Belum ada threshold/sensitivitas yang bisa diatur** di batch ini (butuh kustomisasi
`AudioSink`/`DefaultRenderersFactory` level rendah untuk itu, di luar scope MVP toggle
on/off) — dicatat sebagai batasan disengaja, bukan bug, konsisten dengan pola "Catatan jujur"
proyek ini (lihat README § Catatan jujur soal Gapless Playback untuk pola serupa).

**File**: `SilenceSkipStore.kt` (baru, `data/`). `PlaybackService.kt` (edit) — import
`SessionCommand`/`SessionResult`/`Futures`/`Bundle` baru, `player.setSkipSilenceEnabled(...)`
di `onCreate()` (baca sekali untuk proses baru), `onConnect()` + `onCustomCommand()` baru di
`PlaybackSessionCallback`, 2 konstanta baru di companion object. `PlayerViewModel.kt` (edit) —
`silenceSkipEnabled` StateFlow + `setSilenceSkipEnabled()`. `SettingsScreen.kt` (edit) — toggle
row baru. `MainActivity.kt` (protected, edit parsial) — collect state + wiring ke
`SettingsScreen`.

**Verifikasi**: brace/paren balance manual OK di semua file Kotlin yang disentuh (belum
di-build fisik, sama seperti batch-batch sebelumnya).

## Batch 95 — Fitur baru: Floating Mini Player / Bubble (roadmap item #11)
Dari `ROADMAP_15_FITUR_OFFLINE.md` item #11 (Kompleksitas: Tinggi). Mini player mengambang di
atas app lain mana pun (mirip chat bubble Messenger) — play/pause/prev/next tanpa perlu buka
AudioPlayer, pelengkap widget home-screen yang sudah ada.

**Izin & toggle**: `SYSTEM_ALERT_WINDOW` (baru di `AndroidManifest.xml`) diminta HANYA saat user
menyalakan toggle "Mini Player Mengambang (Bubble)" baru di Settings (opt-in, off by default,
filosofi sama `ShakeSettingsStore`) — bukan di onboarding wajib. `Settings.ACTION_MANAGE_
OVERLAY_PERMISSION` dipakai (bukan runtime permission dialog `ActivityResultContracts.
RequestPermission` biasa seperti RECORD_AUDIO Batch 92 — overlay adalah "special permission",
tidak ada callback granted/denied yang bisa diandalkan lintas OEM dari hasil Activity-nya
sendiri), status sebenarnya dicek ulang langsung ke `Settings.canDrawOverlays()` begitu user
kembali dari layar sistem. `FloatingBubbleStore.kt` (baru, `data/`) simpan preferensi + posisi
drag terakhir, pola identik `ShakeSettingsStore`.

**Kenapa plain View, bukan Compose**: `FloatingBubbleService.kt` (baru, `bubble/`) memasang
window lewat `WindowManager` di luar Activity manapun — sebuah `ComposeView` di posisi ini butuh
`LifecycleOwner`/`SavedStateRegistryOwner` rakitan manual sebelum Compose mau nempel, kompleksitas
nyata untuk pil 3-tombol tanpa scroll/animasi rumit. `bubble_mini_player.xml` (layout baru) reuse
`widget_background.xml`/`widget_play_button_bg.xml`/`ic_widget_*.png` APA ADANYA — identitas
visual bubble otomatis konsisten dengan widget, 0 drawable baru dibuat. Selalu varian gelap
(bukan ikut light/dark toggle in-app widget) — bubble mengambang di atas app apa pun termasuk
bertema terang, butuh 1 kontras yang konsisten di segala kondisi.

**Kontrol/state**: `MediaController` asli dikoneksikan langsung dari Service (pola sama persis
`PlayerViewModel.connect()` — `SessionToken` + `MediaController.Builder(...).buildAsync()`) untuk
update LIVE play/pause/art lewat `Player.Listener`, bukan polling. Tap tombol pakai controller
langsung kalau sudah konek; kalau belum, fallback ke Intent `WidgetUpdater.ACTION_TOGGLE_PLAY/
NEXT/PREVIOUS` ke `PlaybackService` — kontrak Intent yang SUDAH ADA dari widget dipakai apa
adanya, 0 action constant baru perlu ditambah. Artwork pakai `contentResolver.loadThumbnail()`
langsung di URI lagu (pola identik `AudioArtFetcher`/`WidgetUpdater`, lihat catatan Batch 68 di
`AudioArtFetcher.kt` kenapa pendekatan lain pernah gagal total untuk kasus ini).

**Drag & pass-through**: `OnTouchListener` di root layout, dibedakan drag-vs-tap lewat TOTAL
jarak gerak (bukan cuma delta awal-akhir — jari gemetar kecil tidak salah dianggap drag), posisi
di-clamp ke batas layar via `DisplayMetrics` + disimpan ke `FloatingBubbleStore` tiap selesai
drag. Tombol play/prev/next tetap dapat klik normal tanpa logic pemisah manual — `ImageButton`
clickable otomatis mengonsumsi `ACTION_DOWN` duluan sebelum ke `OnTouchListener` root. Touch
pass-through ke app di bawah bubble didapat STRUKTURAL dari window `WRAP_CONTENT` (bukan
`MATCH_PARENT` + flag manual per-event) — area di luar pill otomatis tembus.

`MainActivity.kt` (protected, edit parsial) — `overlayPermissionLauncher`
(`StartActivityForResult`) + `toggleFloatingBubble()` + `LaunchedEffect(Unit)` yang me-restart
Service sekali per proses kalau sesi sebelumnya menyalakannya & izin masih ada (proses baru =
Service lama ikut mati, `START_STICKY` tidak menolong lintas proses). `PlayerViewModel.kt` —
`floatingBubbleEnabled` StateFlow, pola identik `shakeToSkipEnabled` (ViewModel murni simpan
preferensi, TIDAK start/stop Service Android sendiri — butuh Context Activity untuk permission
launcher-nya).

**Batasan jujur** (dicatat, bukan diklaim selesai 100%): `FloatingBubbleService` BUKAN foreground
service — window overlay yang tampil sudah menaikkan importance proses mendekati "visible" di
kebanyakan device, tapi skin Android sangat agresif (lihat catatan OEM yang sama di README §
Keputusan Arsitektur) tetap bisa membunuhnya sewaktu-waktu; ini keterbatasan platform yang sama
seperti widget, bukan sesuatu yang bisa dijamin dari kode manapun. Belum diverifikasi di
perangkat fisik sungguhan (sama seperti seluruh proyek ini, lihat README § Belum selesai).

**Impact Report (Atomic Change)**: 11 file disentuh (2 baru: `FloatingBubbleStore.kt`,
`FloatingBubbleService.kt`, `bubble_mini_player.xml`; 4 diedit:
`AndroidManifest.xml`[protected]/`MainActivity.kt`[protected]/`PlayerViewModel.kt`/
`SettingsScreen.kt`; 4 dokumentasi wajib sinkron) — melebihi batas normal 10 file/1 modul,
dideklarasikan sebagai Atomic Change karena 1 fitur koheren yang membentang izin+service+store+
toggle UI+dokumentasi wajib; memecahnya jadi >1 batch akan meninggalkan kode mati (mis. Service
tanpa toggle) atau melanggar aturan proyek sendiri soal dokumentasi wajib sinkron. Sekalian
dikoreksi: header `FILE_MANIFEST.txt` sempat tertulis "127 file" padahal isi list sebenarnya
sudah 131 sebelum batch ini (drift lama, bukan dari batch ini) — dihitung ulang manual jadi 134
(131 + 3 file baru batch ini), diverifikasi cocok 100% dengan isi direktori project.

## Batch 94 — Dokumentasi: rapikan urutan & "welcome-ability" README (murni dokumentasi, 0 kode)
Permintaan user: rapikan dokumentasi proyek + pastikan info terbaru selalu tampil paling atas
di semua file dokumentasi + tambah shortcut unduh APK dari GitHub Release di README.

**Audit urutan newest-first di `CHANGELOG.md`** — ditemukan 3 blok riwayat lama yang tidak
urut turun sempurna (sisa dari penulisan manual bertahap, bukan proses otomatis): Batch
15 & 14 sempat tertulis setelah Batch 7 (harusnya di antara Batch 16 dan Batch 12), dan
Batch 49 & 48 tertulis di antara Batch 46/47 (harusnya sebelum Batch 47). Kedua blok
dipindah ke posisi numerik yang benar — isi tiap entri tidak diubah sama sekali, murni
reposisi. `PROJECT_STATE.md` dicek dengan cara sama (Batch 93 → 6, ditambah `## Batch 30`
sebagai transisi format lama) — sudah urut turun sempurna, tidak ada perubahan. Bagian
"Riwayat insiden kronologis" di `PROJECT_STATE.md` sengaja **tidak** disortir ulang — label
filenya sendiri eksplisit "kronologis" (tertua → terbaru), beda tujuan dari daftar batch di
atasnya. `FILE_MANIFEST.txt` (alfabetis per path) dan `ROADMAP_15_FITUR_OFFLINE.md`
(bernomor per-item roadmap, bukan kronologis) tidak relevan dengan aturan newest-first —
dibiarkan apa adanya.

**README.md** — ditambah bagian "📥 Unduh Aplikasi" persis di bawah judul (link relatif
`../../releases/latest`, otomatis resolve ke rilis GitHub terbaru repo manapun tempat file
ini di-hosting, tidak perlu hardcode nama owner/repo), callout "🆕 Update terbaru" merujuk ke
Batch 93 (batch fitur terakhir) yang WAJIB disinkronkan manual tiap ada batch fitur baru, dan
Daftar Isi (TOC) mengingat file README sudah >180 baris tanpa navigasi sebelumnya.

## Batch 93 — Fitur baru: Mode Audiobook/Podcast (roadmap item #12)
Dari `ROADMAP_15_FITUR_OFFLINE.md` item #12. Ingat kecepatan putar & posisi terakhir per-lagu
individual (bukan speed global yang sekarang berlaku ke semua lagu), plus tampilan "menit
tersisa" alih-alih total durasi — untuk file panjang (podcast/audiobook) yang di-opt-in manual.

**Bukan extend `PlaybackStateStore`** (dugaan awal roadmap) — dicek ulang dulu isi filenya: itu
murni resume 1 QUEUE global (daftar ID lagu + index + posisi), skema field-nya tidak natural
diperluas jadi per-song tanpa merusak bentuk aslinya. `AudiobookModeStore.kt` (baru, `data/`) —
1 record JSON per lagu (`{enabled, speed, lastPositionMs}`), pola storage sama `BookmarkStore`
(key-per-song `KEY_PREFIX + songId`) tapi 1 object bukan array (state tunggal per lagu, bukan
list yang bisa nambah). Nonaktifkan toggle = hapus record sepenuhnya (bukan simpan
`enabled=false`), pola sama `BookmarkStore` menghapus key saat daftar bookmark kosong.

**Opt-in per-lagu, bukan fitur genre/heuristik durasi** — sempat dipertimbangkan "auto-deteksi
audiobook dari durasi panjang", ditolak: app sudah lama sengaja skip metadata Genre (Batch 89,
alasan N+1 query MediaStore) dan heuristik durasi rawan salah tebak (lagu instrumental panjang,
DJ mix, dll juga bisa panjang tanpa jadi audiobook) — toggle manual per-lagu, sama filosofi
proyek ini di tempat lain (silence-trim roadmap item #8 eksplisit larang "default agresif").

`PlayerViewModel.kt`: `setAudiobookModeEnabled()` — toggle scoped ke `currentSong` yang sedang
dibuka, seed speed dari speed yang SEDANG berjalan saat momen toggle ON (biar nyalain toggle
tidak diam-diam lompat ke speed lain), sekalian persist posisi saat ini juga (bukan nunggu tick
periodik ~5 detik — celah kecil kalau app di-kill tepat setelah toggle ON). `onMediaItemTransition`
— begitu lagu pindah, cek `audiobookModeStore.get(song.id)`, kalau enabled: `setPlaybackSpeed()`
+ `seekTo()` ke nilai tersimpan, **sengaja di-skip untuk `MEDIA_ITEM_TRANSITION_REASON_REPEAT`**
(loop Repeat Satu Lagu) — kalau tidak di-skip, tiap loop bakal seek balik ke posisi lama yang
sudah stale alih-alih restart bersih dari 0, fight sama perilaku repeat-one sendiri. Progress
save (`persistPlaybackState()`, cadence ~5 detik-saat-main + langsung-saat-pause yang sudah ada
utk `PlaybackStateStore`) diperluas sekalian panggil `audiobookModeStore.updateProgress()` —
no-op internal kalau lagu itu tidak di-opt-in, jadi aman dipanggil unconditional tiap tick tanpa
cek toggle dulu di titik itu.

`NowPlayingScreen.kt`: toggle baru ("Mode Audiobook/Podcast", `Switch`) ditaruh di dialog
"Pengaturan Putar" yang SUDAH ADA (`SpeedDialog`, bukan sheet/dialog baru — home paling natural
buat kontrol speed per-file karena memang tentang speed). Teks durasi kanan (posisi/durasi di
bawah seek bar) berubah format jadi `-mm:ss` (sisa waktu, konvensi umum podcast player Spotify/
Apple/Google — dipilih drpd kalimat Indonesia panjang, universal & langsung beda visual dari
angka durasi biasa) saat `audiobookModeEnabled` true utk lagu yang sedang main. 3 parameter baru
diteruskan ke `SpeedDialog` + `NowPlayingScreen`.

`MainActivity.kt` (protected, edit parsial) — 1 `collectAsStateWithLifecycle()` baru + 2
parameter diteruskan ke `NowPlayingScreen(...)` yang sudah ada. 0 perubahan struktur NavHost.

Brace/paren 4 file kode dicek otomatis & seimbang. `FILE_MANIFEST.txt` di-diff eksplisit
terhadap isi ZIP sebelum dikirim. **Belum diverifikasi compile/runtime Gradle sungguhan** (tidak
ada JDK/Android SDK/kotlinc di sandbox ini) — prioritas berikutnya: `./gradlew assembleDebug`,
lalu cek di device: (1) toggle ON lalu pindah ke lagu lain lalu balik lagi — speed & posisi
benar kembali tepat seperti terakhir; (2) Repeat Satu Lagu pada lagu yang di-opt-in TIDAK ikut
seek balik ke posisi lama tiap loop (ini titik paling gampang lolos tanpa device — kalau guard
`MEDIA_ITEM_TRANSITION_REASON_REPEAT`-nya salah, gejalanya bakal lagu "macet" muter ulang dari
tengah terus, bukan dari awal); (3) teks `-mm:ss` update benar mengikuti posisi berjalan,
bukan angka statis; (4) toggle OFF benar menghapus record tersimpan (grep SharedPreferences
`audiobook_mode` kosong setelah semua lagu di-toggle off).

## Batch 92 — Fitur baru: Visualizer Audio (roadmap item #9)
Dari `ROADMAP_15_FITUR_OFFLINE.md` item #9. Spectrum bar real-time di sheet baru "Visualizer
Audio", dibuka dari "Kontrol Lanjutan" (Now Playing → titik tiga) — pola sama seperti
Timer/Kecepatan/Equalizer/Repeat A-B di sheet itu.

**Riset izin duluan sebelum nulis kode** — roadmap butir risiko awal cuma bilang "butuh
`RECORD_AUDIO` di beberapa versi Android". Dicek ulang: `android.media.audiofx.Visualizer` wajib
`RECORD_AUDIO` di SEMUA versi Android untuk audio session apa pun, termasuk sesi milik aplikasi
sendiri — tidak ada pengecualian "baca audio sendiri" seperti dugaan awal. Keputusan: minta izin
ini **on-demand** (baru saat user aktifkan toggle di sheet), bukan dilempar ke alur onboarding
wajib di awal — fitur visual opsional yang minta izin semirip mikrofon di first-launch dianggap
overreach untuk fitur yang mayoritas user mungkin tidak pernah buka.

`AndroidManifest.xml` (protected, edit parsial) — tambah `<uses-permission RECORD_AUDIO>` dengan
komentar panjang menjelaskan kenapa (tidak ada rekaman/penyimpanan suara sungguhan, murni baca
sinyal FFT lagu yang sedang diputar sendiri).

`AudioVisualizerController.kt` (baru, `playback/`) — bungkus `android.media.audiofx.Visualizer`.
Attach ke `PlaybackAudioSession.sessionId` — mekanisme sharing session ID yang sama persis dipakai
`EqualizerController` (Batch-batch lama), satu-satunya cara `PlayerViewModel` tahu
`audioSessionId` ExoPlayer karena yang dipegang cuma `MediaController`, bukan instance ExoPlayer
langsung. Capture size 512 (`coerceIn` ke `Visualizer.getCaptureSizeRange()`), capture rate
ditahan ke ~15fps (`min(Visualizer.getMaxCaptureRate(), 15000)`) — sesuai catatan risiko roadmap
soal battery drain, spectrum bar tidak perlu lebih mulus dari itu untuk terlihat "hidup". FFT byte
array (format `[DC, Nyquist, Re1, Im1, Re2, Im2, ...]` per dokumentasi platform) dikelompokkan ke
24 bar magnitude ternormalisasi 0f..1f, clip di 90f (titik potong empiris supaya noise baseline
saat bagian senyap tidak ikut membesar-mengecil mengikuti skala per-frame).

**2 bug method-vs-property ketemu & diperbaiki sebelum file final** (dicek manual terhadap
dokumentasi kelas `Visualizer`/`AudioEffect`, bukan cuma asumsi): `getMaxCaptureRate()` ternyata
`static` — harus dipanggil `Visualizer.getMaxCaptureRate()`, bukan lewat instance. `setCaptureSize()`
dan `setEnabled()` keduanya return `Int` (status code), bukan `void` — Kotlin cuma bikin property
`var` otomatis dari pasangan getter/setter Java kalau setter-nya return `Unit`, jadi keduanya wajib
tetap method call eksplisit (`viz.setCaptureSize(...)`, `viz.setEnabled(...)`), bukan sintaks
property (`viz.captureSize = ...`, `viz.enabled = ...`) — persis alasan `EqualizerController.kt`
lama juga sudah selalu pakai `eq.setEnabled(...)` eksplisit, bukan kebetulan gaya penulisan.

`VisualizerSettingsStore.kt` (baru, `data/`) — toggle on/off persisten, pola identik
`ShakeSettingsStore` (default off, fitur yang butuh izin sensitif harus sengaja dinyalakan user).

`VisualizerSheet.kt` (baru, `ui/`) — header + `Switch`, pola shell sama `EqualizerSheet.kt`
(`frostedGlass()`). Teks status berubah 4 kondisi (izin belum ada / tidak didukung / nonaktif /
aktif) — kalimat izin ditulis eksplisit "BUKAN untuk merekam suara" karena `RECORD_AUDIO` adalah
permission yang paling gampang disalahpahami user awam. `SpectrumBars` — Canvas custom kedua di
codebase ini (setelah `WeeklyTrendChart`, Batch 90), sengaja dibuat minimal (rounded bar + track
transparan buat bar senyap, 0 gridline/axis) untuk alasan sama: tidak ada compiler/emulator di
sandbox ini untuk verifikasi visual langsung.

`PlayerViewModel.kt` — `ensureVisualizerAttached()` (dipanggil saat sheet dibuka, cuma attach kalau
toggle sudah on — beda dari `ensureEqualizerAttached()` yang unconditional karena ekualizer harus
tetap mempengaruhi audio nyata di background, visualizer cuma gambar piksel jadi tidak ada alasan
tetap capture kalau tidak terlihat), `setVisualizerEnabled()` (toggle + attach/release),
`stopVisualizerCapture()` (dipanggil saat sheet ditutup — release capture tapi sengaja TIDAK
mengubah preference tersimpan, supaya reattach otomatis lain kali sheet dibuka tanpa user perlu
nyalakan switch ulang). `audioVisualizerController.release()` ditambah di `onCleared()`.

`NowPlayingScreen.kt` — 8 parameter baru (state + 4 callback), 1 baris baru di
`AdvancedControlsSheet` ("Visualizer Audio", ikon `Icons.Default.GraphicEq`, baru diimpor — sudah
dipakai di layar Welcome `MainActivity.kt` untuk highlight generik, tidak konflik), 1 blok
pemanggilan sheet baru.

`MainActivity.kt` (protected, edit parsial) — `visualizerPermissionLauncher`
(`ActivityResultContracts.RequestPermission()`), diminta cuma saat user coba nyalakan toggle dari
sheet, bukan di alur onboarding izin wajib yang sudah ada. Kalau permission granted saat itu juga,
langsung `setVisualizerEnabled(true)` — supaya user tidak perlu tap switch 2x untuk 1 niat. 3
`collectAsStateWithLifecycle()` baru + 8 parameter baru diteruskan ke `NowPlayingScreen(...)` yang
sudah ada. 0 perubahan struktur NavHost/route.

**Keputusan scope yang sengaja dibuat** (ditulis eksplisit, bukan disembunyikan): spectrum bar
HANYA capture selama sheet "Visualizer Audio" terbuka, TIDAK dirender sebagai elemen ambient
permanen di layar Now Playing utama (mis. di belakang album art). Roadmap menyebut "ditampilkan di
Now Playing" — diinterpretasikan sebagai "bisa diakses dari Now Playing" (sama seperti Equalizer/
Repeat A-B yang juga cuma lewat sheet, bukan elemen permanen), bukan "harus selalu tampil di layar
utama". Alasan: `FloatArray` bukan tipe stabil buat Compose compiler, kalau di-thread terus-menerus
ke seluruh `NowPlayingScreen` (bukan cuma saat sheet-nya sendiri terbuka) berisiko memicu
recomposition ~15fps untuk keseluruhan layar (termasuk animasi album art/blur) — risiko jank yang
tidak bisa diverifikasi tanpa device sungguhan, jadi diperkecil permukaannya duluan.

Brace/paren 7 file kode dicek otomatis & seimbang. `FILE_MANIFEST.txt` di-diff eksplisit terhadap
isi ZIP sebelum dikirim. **Belum diverifikasi compile/runtime Gradle sungguhan** (tidak ada JDK/
Android SDK/kotlinc di sandbox ini) — prioritas berikutnya: `./gradlew assembleDebug`, lalu cek di
device: (1) dialog permission `RECORD_AUDIO` benar muncul saat toggle dinyalakan pertama kali, teks
rasional dari Android-nya sendiri tidak bikin user takut; (2) bar spectrum benar bergerak sinkron
lagu yang diputar (bukan angka acak/statis — bug paling gampang lolos tanpa device sungguhan); (3)
capture size 512 & `getCaptureSizeRange()` benar didukung di device asli (fallback `coerceIn` sudah
ada tapi belum pernah dilihat jalan); (4) tidak ada jank terasa di layar Now Playing selama sheet
Visualizer terbuka, terutama di device kelas bawah; (5) `Visualizer` benar ter-release saat sheet
ditutup (cek battery/CPU tidak terus jalan di background lewat `adb shell dumpsys media.audio_flinger`
atau semacamnya) — `stopVisualizerCapture()` sudah dipanggil di `onDismiss`, tapi urutan lifecycle
`ModalBottomSheet` vs callback ini belum pernah dilihat jalan sungguhan.

## Batch 91 — Fitur baru: A-B Repeat & Bookmark Posisi (roadmap item #4)
Dari `ROADMAP_15_FITUR_OFFLINE.md` item #4. 2 fitur terkait digabung 1 sheet baru "Repeat A-B &
Bookmark", dibuka dari "Kontrol Lanjutan" (Now Playing → titik tiga) — pola sama seperti
Timer/Kecepatan/Equalizer di sheet itu.

**A-B Repeat** — tandai Titik A & Titik B di posisi putar saat ini, playback otomatis loncat
balik ke A begitu posisi lewat B, berulang terus sampai dihapus/lagu ganti. Boundary check
diekstrak ke `AbRepeatLogic.kt` (baru, `playback/`) — pure `object`, 0 dependency Context, pola
identik `SmartPlaylistEngine`/`ListeningStatsEngine` (Batch 89/90) supaya testable murni JVM
tanpa Robolectric. `isActive()`/`shouldLoopBack()` sengaja treat B<=A atau salah satu null
sebagai "belum aktif" (bukan crash/loop di 1 titik) — kasus umum kalau user tap "Tandai B"
sebelum "Tandai A". `AbRepeatLogicTest.kt` (baru, 7 test) termasuk kasus tepi pointA=0L (awal
lagu) supaya tidak salah dianggap "belum diatur" (beda dari null sentinel).

`PlayerViewModel.kt`: state `_abRepeatPointA`/`_abRepeatPointB` (StateFlow, bukan bagian
`PlaybackUiState` — dicek tiap tick 500ms di `startPositionLoop()`, tidak perlu memicu
recomposition uiState penuh setiap kali). Dicek tepat setelah update position/duration di loop
yang sama, `seekTo(pointA)` kalau `AbRepeatLogic.shouldLoopBack(...)` true. Kedua titik direset
otomatis di `onMediaItemTransition` (ganti lagu) — A-B Repeat scoped ke 1 lagu, titik B lagu
lama yang kebawa ke lagu baru berisiko diam-diam memotong intro lagu baru kalau posisinya
kebetulan pas. `setAbRepeatPointA()` juga menghapus titik B lama kalau B<=A baru (mencegah state
"aktif tapi diam" tanpa penjelasan ke user kenapa loop tidak jalan).

**Bookmark Posisi** — tandai beberapa titik favorit per-lagu (intro/reff/solo dll), tap untuk
lompat langsung, hapus per-bookmark. `Bookmark.kt` (baru, `data/`) — model
`{id, label, positionMs}`. `BookmarkStore.kt` (baru) — JSON array per song ID di
SharedPreferences, pola storage sama `SmartPlaylistStore` (parse-with-fallback, JSONArray),
key-per-song sama `LyricsStore` (`KEY_PREFIX + songId`). **Beda dari `PlaybackStateStore`
existing** — itu cuma ingat 1 posisi terakhir untuk seluruh antrean (resume), ini banyak titik
bernama per-lagu individual.

`ABRepeatBookmarkSheet.kt` (baru, `ui/`) — UI kedua fitur, kartu A/B (`OutlinedButton` tampil
posisi mm:ss atau "Tandai"), status teks aktif/nonaktif, list bookmark (`LazyColumn`, tap-untuk-
lompat + ikon hapus per-row), dialog kecil untuk nama bookmark (default "Tanda mm:ss", bisa
diedit) — pola `AlertDialog` sederhana yang sama dengan dialog rename `PlaylistScreen.kt`, tidak
diekspor/dishare (cuma 1 pemanggil lain), dibuat lokal di file ini.

`NowPlayingScreen.kt` — 8 parameter baru (state A/B + 6 callback), 1 baris baru di
`AdvancedControlsSheet` ("Repeat A-B & Bookmark", ikon `Icons.Default.Repeat` yang sudah
diimpor, tidak nambah import baru), 1 blok pemanggilan sheet baru dgn pola `remember(song.id)`
sama seperti `lyricsText` (Batch 82) — list bookmark di-refresh manual setelah
tambah/hapus (`bookmarks = onGetBookmarks(song.id)`) karena bukan StateFlow, sama seperti pola
lyrics existing.

`MainActivity.kt` (protected, edit parsial) — 2 `collectAsStateWithLifecycle()` baru
(`abRepeatPointA`/`abRepeatPointB`) + 8 parameter baru diteruskan ke pemanggilan
`NowPlayingScreen(...)` yang sudah ada. 0 perubahan struktur NavHost/route — sama seperti
Batch 89/90, numpang di layar yang sudah ada, bukan route baru.

Brace/paren 8 file kode dicek otomatis & seimbang. `FILE_MANIFEST.txt` di-diff eksplisit
terhadap isi ZIP sebelum dikirim (127/127 match). **Belum diverifikasi compile/runtime Gradle
sungguhan** (tidak ada JDK/Android SDK/kotlinc di sandbox ini) — prioritas berikutnya:
`./gradlew testDebugUnitTest` verifikasi 7 test baru, lalu build APK asli + cek di device:
(1) A-B Repeat benar-benar loncat balik ke A tepat saat posisi lewat B, tanpa jeda/glitch audio
terasa; (2) titik A/B benar hilang otomatis begitu lagu ganti (manual atau auto-advance); (3)
bookmark tersimpan lintas restart app (SharedPreferences, seharusnya aman tapi belum dilihat
langsung); (4) sheet "Repeat A-B & Bookmark" render benar di kedua tema custom (Tactile/Skeu) —
sheet ini reuse `frostedGlass()` yang sudah ada, risiko rendah, tapi belum pernah dilihat.

## Batch 90 — Fitur baru: Dashboard Statistik Dengar Lokal (roadmap item #10)
Dari `ROADMAP_15_FITUR_OFFLINE.md`: "data sudah dikumpulkan (`PlayStatsStore`,
`ListeningHistoryStore`)... belum ada halaman statistik dedicated." Ditambahkan layar baru,
diakses dari Pengaturan → "Statistik Dengar".

**Isi dashboard** (4 kartu):
1. **Ringkasan** — total lagu diputar (`PlayStatsStore.totalPlayCount()`, sudah ada) + estimasi
   waktu dengar total, dihitung `durasi lagu × jumlah putar` per lagu lalu dijumlah
   (`ListeningStatsEngine.totalListeningMs`). Ini estimasi, bukan log posisi kontinu — app tidak
   pernah mencatat berapa lama tepatnya tiap sesi dengar berlangsung (cuma momen "lagu mulai
   diputar"), jadi asumsinya tiap lagu yang tercatat diputar dianggap selesai penuh, konsisten
   dengan cara `PlayStatsStore` sendiri menghitung "1 putaran" (di momen mulai, bukan selesai).
2. **Tren 7 Hari Terakhir** — grafik batang dari `ListeningHistoryStore.getCountsForLastDays(7)`
   (fungsi baru, murni pass-through baca key tanggal yang sudah ada, 0 skema data baru).
   Digambar via `Canvas` custom di `StatsDashboardScreen.kt` (`WeeklyTrendChart`) — **chart
   custom pertama di codebase ini**. Sengaja dibuat sangat minimal (rounded bar + track abu-abu
   buat slot hari kosong, 0 gridline/axis/teks-di-atas-Canvas) untuk membatasi permukaan
   kesalahan render: environment kerja ini tidak punya compiler Android untuk verifikasi visual,
   jadi API yang dipakai dibatasi ke yang paling basic & paling dikonfirmasi ada di
   `DrawScope` (`drawRoundRect`, `Offset`, `Size`, `CornerRadius` — semua bagian inti
   `androidx.compose.ui.graphics`/`.geometry`, bukan API eksotis). Label hari (Sen/Sel/dst)
   dirender sebagai `Text` composable biasa di bawah Canvas, BUKAN digambar di atas Canvas
   sebagai native text — menghindari kelas bug "tebak nama API Compose yang jarang dipakai"
   yang sudah 3x menyebabkan hotfix build gagal berturut-turut di riwayat proyek ini
   (Batch 42→43→44, saga `drawOutline`).
3. **Jam Favorit Dengar** — data BARU, sebelum batch ini app tidak pernah mencatat jam berapa
   lagu diputar (`ListeningHistoryStore` cuma granularitas per-hari via `LocalDate` key, bukan
   per-jam). `HourlyListenStore.kt` (baru): 24 counter flat (`hour_0`..`hour_23` di
   SharedPreferences), `recordPlay()` increment bucket jam saat ini
   (`Calendar.HOUR_OF_DAY`), `getHourlyCounts(): IntArray`. **Sengaja file terpisah**, bukan
   memperluas skema key tanggal `ListeningHistoryStore` yang sudah lama dipakai dan berisi data
   histori dengar user lama — menambah dimensi jam ke situ berarti migrasi format data existing
   (risiko), sedangkan 24 counter baru yang berdiri sendiri risikonya nol (tidak menyentuh data
   lama sama sekali, dan tidak pernah tumbuh tak terbatas karena cuma 24 key tetap).
4. **Artis Paling Sering** — top 5 artis berdasar total play count lintas SEMUA lagu mereka
   (bukan cuma dari daftar top-N lagu individual seperti `getTopArtistMix()` yang sudah ada,
   yang cuma menyisir 50 lagu ter-top dulu baru cari artis dominan di situ — pendekatan lama itu
   cukup untuk "1 artis dominan" tapi under-count kalau mau ranking 5 artis penuh karena artis
   dengan banyak lagu ber-play-count sedang-sedang saja bisa kalah dari artis dengan 1 lagu
   sangat sering diputar). Perlu akses ke SEMUA play count, bukan cuma top-N — `PlayStatsStore`
   dapat fungsi baru `getAllCounts(): Map<Long, Int>` (murni additive, `getMostPlayedIds()` lama
   tidak diubah/dipakai fungsi lain manapun tetap jalan sama).

**Arsitektur**: `ListeningStatsEngine.kt` (baru, `data/`) — pure aggregator
(`topArtists`/`totalListeningMs`/`peakHour`/`weeklyTrend`/`buildSnapshot`), sama sekali tidak
menyentuh `Context`/SharedPreferences langsung — pola identik `SmartPlaylistEngine` (Batch 89),
yang sendiri meneruskan pola "extract pure function dari store berbasis Context" yang
dirintis Batch 27 (`ShakePulseTracker`, dll). Ini yang bikin `ListeningStatsEngineTest.kt` (13
test baru) bisa jalan di JVM murni tanpa Robolectric — termasuk kasus tepi yang sengaja dites:
tie-breaking `peakHour` (pilih jam paling awal kalau count sama), artis dengan nama kosong
di-exclude, play count untuk lagu yang sudah tidak ada di library (dihapus dari device) di-skip
alih-alih crash.

`PlayerViewModel.kt`: `hourlyListenStore` diinstansiasi di samping store terkait
lain, `recordPlay()`-nya dipanggil TEPAT di titik yang sama dengan
`playStatsStore.recordPlay()`/`listeningHistoryStore.recordPlay()` (dalam
`onMediaItemTransition`) — 1 event "lagu mulai diputar" konsisten tercatat ke ketiga store
sekaligus, tidak ada jalur yang bisa mencatat ke 2 dari 3 store lalu lupa yang ketiga. Fungsi
baru `getListeningStats(allSongs): ListeningStatsEngine.Snapshot` jadi satu-satunya entry point
yang dipanggil UI, mengumpulkan data mentah dari 3 store lalu delegasikan seluruh perhitungan
ke engine — `PlayerViewModel` sendiri tidak punya logika agregasi apa pun, murni wiring.

**File disentuh** (9 kode + 5 dokumentasi, Atomic Change — 1 fitur kohesif data+VM+UI+nav+docs,
sama presedennya dengan Batch 89's 11 file):
- Baru: `HourlyListenStore.kt`, `ListeningStatsEngine.kt`, `StatsDashboardScreen.kt`,
  `ListeningStatsEngineTest.kt`
- Edit: `PlayStatsStore.kt` (+`getAllCounts()`), `ListeningHistoryStore.kt`
  (+`getCountsForLastDays()`), `PlayerViewModel.kt`, `MainActivity.kt` (protected, parsial —
  1 route + 1 callback), `SettingsScreen.kt` (1 menu row, non-protected)
- Dokumentasi: `README.md`, `ROADMAP_15_FITUR_OFFLINE.md` (item #10 ditandai selesai, pola sama
  item #2 Batch 89), `FILE_MANIFEST.txt` (118→122), `PROJECT_STATE.md`, `CHANGELOG.md` (ini)

Brace/paren 9 file kode dicek otomatis & seimbang. `FILE_MANIFEST.txt` di-diff eksplisit
terhadap isi folder kerja sebelum di-zip (122/122 match) — pelajaran Batch 27 revisi 1 (ZIP
nested-folder + `-x '*.git*'` salah exclude `.github/`) diterapkan lagi supaya tidak terulang.

**Sengaja TIDAK dikerjakan**: genre listening breakdown (MediaStore taruh genre di tabel
terpisah, query per-lagu N+1 — sama alasan Smart Playlist Batch 89 skip kriteria genre), streak
harian ("berapa hari berturut-turut dengar musik" — butuh logika gap-detection tambahan di luar
scope 4 kartu yang diminta roadmap), dan export/share data statistik (fitur terpisah, bukan
bagian dashboard read-only ini).

**Belum diverifikasi compile/runtime Gradle sungguhan** (tidak ada JDK/Android SDK/kotlinc di
sandbox ini) — prioritas berikutnya: `./gradlew testDebugUnitTest` verifikasi 13 test baru,
lalu build APK asli + cek tab "Statistik Dengar" render benar di device. **`WeeklyTrendChart`
adalah risiko visual tertinggi di batch ini** (Canvas custom pertama, lihat riwayat insiden
Batch 40-44 soal fitur shadow/chart custom yang butuh 2-3 iterasi sebelum benar secara visual
walau compile-nya sendiri sukses) — kalau bar terlihat kepotong/tidak proporsional di device,
cek dulu `size.height`/`fraction` sebelum menambah lapisan gambar baru.

## Batch 89 — Fitur baru: Playlist Otomatis (Smart Playlist)
Fitur dari ROADMAP_15_FITUR_OFFLINE.md. Playlist berbasis aturan, bukan daftar lagu statis:
sekali diatur (folder, rentang durasi, rating minimum, rentang tahun rilis, kata kunci), lagu
baru yang cocok otomatis ikut masuk tanpa user isi manual — beda dari playlist manual yang
sudah ada (`PlaylistStore`), yang menyimpan daftar ID lagu tetap. Atomic Change (>10 file) —
fitur ini menyentuh data+ui+viewmodel+MainActivity sekaligus, tidak bisa dipecah tanpa state
setengah-jadi yang crash (mis. tab UI tanpa data, atau data tanpa entry point).

**Baru (5 file):**
1. `data/SmartPlaylist.kt` — model kriteria (semua opsional, AND bukan OR kalau digabung).
2. `data/SmartPlaylistEngine.kt` — pure matcher/resolver, terpisah dari Context/Store persis
   pola `LibraryFilterStore.shouldKeep` supaya testable tanpa Android runtime.
3. `data/SmartPlaylistStore.kt` — persist JSON ke SharedPreferences, pola sama `PlaylistStore`.
4. `ui/SmartPlaylistScreen.kt` — `SmartPlaylistTabView` (list + detail, cermin struktur
   `PlaylistTabView`) + `SmartPlaylistBuilderSheet` (bottom sheet buat/ubah aturan).
5. `test/.../SmartPlaylistEngineTest.kt` — 11 unit test (folder, durasi, rating, tahun,
   keyword, gabungan kriteria, resolve), pola `mock(Uri::class.java)` sama
   `LibrarySearchIndexTest.kt`.

**Diedit (6 file):**
1. `data/Song.kt` — tambah `val year: Int = 0` (default → semua call site lama, termasuk
   fixture test, tetap kompatibel tanpa diubah).
2. `data/MusicRepository.kt` — ambil kolom `MediaStore.Audio.Media.YEAR`.
3. `data/CustomFolderScanner.kt` — ambil `METADATA_KEY_YEAR` dari `MediaMetadataRetriever`,
   ambil digit awal saja (`takeWhile { it.isDigit() }`) supaya format non-standar semacam
   "2015-03-01" tetap kebaca "2015", bukan gagal total.
4. `playback/PlayerViewModel.kt` — `smartPlaylistStore` + `StateFlow<List<SmartPlaylist>>` +
   create/update/delete, pola identik blok playlist manual yang sudah ada.
5. `ui/LibraryScreen.kt` — tab ke-6 "Otomatis" di dropdown "Lainnya" (`moreLabels` sekarang 4
   item, `moreSelected` range `3..6`, dropdown loop sudah dinamis dari list jadi tidak perlu
   sentuh UI dropdown-nya sendiri), instance lokal `RatingStore` (pola sama `filterStore`
   lokal di file yang sama), turunkan daftar nama folder unik buat chip builder.
6. `MainActivity.kt` (**protected, edit parsial**) — collect `smartPlaylists` StateFlow +
   3 callback baru, disisipkan ke pemanggilan `LibraryScreen(...)` yang sudah ada. Tidak
   menyentuh `NavHost`/struktur route sama sekali — Smart Playlist numpang di tab Library yang
   sudah ada, bukan route baru, jadi permukaan protected asset yang tersentuh seminimal
   mungkin.

**Sengaja di luar cakupan batch ini:**
- **Genre** — kriteria genre di roadmap sengaja di-skip. `Song` tidak simpan genre, dan
  MediaStore taruh genre di tabel `Genres` terpisah (query per-lagu / N+1, bukan satu kolom
  langsung seperti YEAR) — risiko & kompleksitas lebih tinggi dari sisa fitur di batch ini.
  Belum dijadwalkan ke batch berikutnya.
- Builder pakai text field angka (menit/tahun), bukan slider — konsisten sama alasan yang
  sudah dicatat README soal drag-gesture tanpa compiler buat verifikasi.

Brace/paren tiap file yang disentuh dicek manual & seimbang (`python3 -c "s.count('{')..."`).
**Belum diverifikasi compile/runtime Gradle sungguhan** — tidak ada JDK/Android SDK/kotlinc di
sandbox ini, jadi review murni statis (baca ulang tiap import/signature/call site yang
disambungkan). Jalankan `./gradlew testDebugUnitTest` di Termux untuk verifikasi test baru,
dan build APK asli untuk verifikasi UI baru sebelum rilis produksi.

## Batch 88 — Fix bug mini player dobel di Now Playing + sederhanakan hierarki tombol (feedback user + screenshot)
User laporan: "hierarki tombol nya terlalu membingungkan bagi user awam", disertai screenshot
layar Now Playing yang menunjukkan floating mini player nongol lagi di bawah, menimpa/mepetin
kontrol layar penuh di atasnya. 2 file:

1. **Bug nyata — `MainActivity.kt`**: `AnimatedVisibility` mini player di `bottomBar` Scaffold
   cuma dicek `uiState.currentSong != null`, TANPA cek route sama sekali — beda dari kondisi
   NavigationBar tepat di bawahnya yang sudah benar mengecualikan `"now_playing"`. Akibatnya
   mini player tetap muncul dobel walau user sudah di layar Now Playing sendiri — root cause
   sungguhan screenshot user. Fix: tambah `&& currentRoute != "now_playing"`.
2. **Sederhanakan top bar — `NowPlayingScreen.kt`**: 5 ikon berbobot sama (Tutup/Favorit/
   Antrean/Lirik/Lanjutan) tanpa hierarki jelas. Antrean & Lirik (dipakai situasional, bukan
   tiap sesi dengar) digabung ke sheet "Kontrol Lanjutan" yang sudah ada — pola yang sama
   persis yang sudah dipakai Timer/Kecepatan/Equalizer di sheet itu (doc-comment fungsinya
   sendiri sudah bilang tujuannya "instead of ... crowding the main top bar", tinggal
   diterapkan konsisten ke 2 ikon yang masih tertinggal). Top bar sekarang 3 ikon: Tutup,
   Favorit, Lanjutan (⋮).

Brace/paren balance kedua file dicek manual & seimbang. Rating bintang & susunan lain di bawah
piringan album TIDAK disentuh batch ini — fokus murni ke bug dobel + konsolidasi ikon top bar
yang paling langsung match keluhan user. Masih belum diverifikasi visual sungguhan di device.

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

## Batch 6 dan sebelumnya
Lihat daftar fitur di `README.md` — detail per-batch untuk rentang ini tidak tercatat
terpisah di file ini.
