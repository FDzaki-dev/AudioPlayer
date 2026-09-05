# PROJECT_STATE.md

Konteks untuk sesi chat AI mana pun yang melanjutkan proyek ini. Baca file ini dulu sebelum
menyentuh kode apa pun. Detail lengkap tiap batch ada di `CHANGELOG.md`; ringkasan fitur
lengkap ada di `README.md`. File ini adalah ringkasan status + jebakan yang sudah pernah
kejadian, bukan pengganti keduanya. **Sejak Batch 158**, file ini cuma menyimpan 100 batch
paling baru (Batch 58 ke atas) — detail Batch 1-57 ada di `PROJECT_STATE_ARCHIVE.md`.

## ⚠️ ATURAN SESI AKTIF — WAJIB DIBACA (posisi tetap, tidak ikut tergeser batch baru)
Detail lengkap ada di § "Aturan sesi: transparansi versi & pesan commit" di BAWAH file ini
(sengaja diringkas ulang di sini juga, supaya tidak tenggelam kalau sesi cuma sempat baca bagian
atas file yang terus memanjang):
1. **Dilarang edit manual `versionCode`/`versionName`** di `app/build.gradle.kts` — auto dari
   commit git sejak Batch 30. Tiap kirim ZIP wajib sebut nomor batch + ingatkan versionName asli
   baru pasti setelah `git push`.
2. **Box code pesan commit WAJIB tampil di atas heading "Update Harian:"** tiap respons chat,
   isinya wajib penjelasan fitur singkat dari `CHANGELOG.md` batch itu — dilarang cuma angka
   versi polos.
3. **Prioritas mutakhir, bukan kompatibilitas OS/dependency lama** (Batch 205, permintaan user
   eksplisit) — user TIDAK PEDULI dukungan OS/API lama (mis. Android <12/API 31). Struktur,
   komponen, dan dependency WAJIB pakai versi paling mutakhir yang tersedia; JANGAN habiskan
   effort bikin/pertahankan fallback kompatibilitas legacy yang rumit kalau ada opsi modern yang
   lebih bersih. Detail lengkap § "Kebijakan: prioritas mutakhir" di bawah.
4. **`POLISH_AUDIT.md`/`MICRO_UIUX_AUDIT.md` DIARSIPKAN Batch 278** (jadi
   `ARCHIVED_POLISH_AUDIT.md`/`ARCHIVED_MICRO_UIUX_AUDIT.md`) — digantikan
   `ROADMAP_LIQUID_GLASS_REDESIGN.md` sebagai sumber task default kalau tidak ada instruksi/
   log_fail baru dari user. **TAPI**: roadmap itu butuh konfirmasi user dulu (§3 di dalamnya)
   sebelum sesi mana pun mulai eksekusi kode — kalau belum ada konfirmasi, JANGAN eksekusi
   apa pun dari situ, tanya user dulu.
5. **Nama folder Termux proyek ini `audioplayer` (lowercase, default kebab-case) — SUDAH FINAL,
   JANGAN diubah lagi.** Sempat direvisi ke `AudioPlayer` (kapital) tapi DIBATALKAN user: local
   folder Termux TIDAK terikat ke `rootProject.name` (sudah di-hardcode `"AudioPlayer"` di
   `settings.gradle.kts`, tidak baca nama folder) maupun ke `git remote` (cuma URL, bukan
   casing folder) — jadi lowercase aman dan konsisten sama default preferensi. Migrasi 1x
   (rename/hapus folder lama kapital) sudah diarahkan ke user Batch 266. Semua skrip Termux
   berikutnya WAJIB pakai `~/projects/audioplayer`.

## Batch terakhir yang selesai
**Batch 352 (MITIGASI — Opsi B dari `PENDING_FixGlobalLagRecomposition.md` dieksekusi, 1 file
kode)** — User keputusan gabungan: Opsi B (mitigasi cepat, throttle tick) dieksekusi SEKARANG
sebagai quick win 1 batch; Opsi A (fix struktural permanen, sentuh `MainActivity.kt` protected)
tetap DIANTRE untuk sesi berikutnya. `PlayerViewModel.kt` `startPositionLoop()`:
`delay(500)`→`delay(1000)`. Dikompensasi mandiri (tidak diminta eksplisit di opsi, ditemukan saat
baca kode `positionTick` dipakai di mana): modulo `persistPlaybackState()` dari `% 10` ke `% 5`
supaya cadence save tetap ~5 detik, TIDAK ikut molor ke ~10 detik (mencegah window kehilangan
posisi playback saat crash 2x lebih lebar dari sebelumnya). `AbRepeatLogic.kt` disentuh
komentar-doc saja (referensi "~500ms" jadi "~1000ms since Batch 352"), 0 logika berubah.

**Efek samping melekat (bukan bug, konsekuensi Opsi B by design)**: presisi progress bar
MiniPlayerBar/slider Now Playing turun ke update per 1 detik (dari 0.5 detik); overshoot A-B
Repeat lewat titik B naik ke maks ~1 detik (dari ~0.5 detik). **SCOPE masalah utama BELUM
tuntas** — `AppNavHost` (`MainActivity.kt`) masih recompose PENUH tiap tick, cuma lebih jarang
(1x/detik, bukan 2x/detik). User kemungkinan besar masih lapor "lag berkurang, belum hilang" —
sudah diperingatkan eksplisit di PENDING doc sebelum eksekusi.

**Status PENDING_FixGlobalLagRecomposition.md**: diupdate (bukan diarsipkan) — Opsi A masih
berlaku penuh utk sesi berikutnya, root cause & rencana file belum berubah sama sekali, cuma
status header yang diubah dari "menunggu pilih" jadi "Opsi B sudah jalan Batch 352, Opsi A masih
nunggu confirm eksekusi".

**Ringkasan file** — 2 file kode disentuh (1 logic: `PlayerViewModel.kt`; 1 komentar-saja:
`AbRepeatLogic.kt`) + 3 dokumentasi VIP (PROJECT_STATE.md/CHANGELOG.md/README.md) +
`PENDING_FixGlobalLagRecomposition.md` diupdate (bukan file baru). `FILE_MANIFEST.txt` tetap 189
(0 file baru/hapus).

**Batch 351 (Investigasi root cause lag/stutter kronis app-wide — DITEMUKAN kandidat kuat,
0 kode diubah, tunggu konfirmasi user sebelum eksekusi fix)** — User laporan: item terbuka
terakhir roadmap Liquid Glass (performa GPU/lag) MASIH kerasa stutter walau Haze blur sudah
dicabut PERMANEN sejak Batch 329 (`hazeEffect` dihapus total dari `frostedGlass()`, 0 blur asli
lagi di identitas manapun). Diklarifikasi via 2 tappable question: (1) situasi — user pilih
SEMUA 4 opsi sekaligus (scroll list Home/Library, ganti tab bawah, MiniPlayerBar pas musik
muter, buka Now Playing/sheet) + tambahan eksplisit **"dari dulu gejala nya gak pernah
benar-benar fix!!"**; (2) tema — "belum yakin/gak merhatiin". Kombinasi ini MEMBATALKAN asumsi
lama (blur/GPU cost khusus Liquid Glass, fokus Batch 296-329): kalau gejala muncul di SEMUA
situasi tanpa exclusive ke 1 tema/1 screen, root cause kemungkinan besar BUKAN soal
rendering-cost sama sekali — lebih ke arah FREKUENSI/SCOPE recomposition yang tidak pernah
disentuh batch manapun sebelumnya.

**Root cause candidate — ditemukan via baca kode langsung (grep+view), BUKAN tebakan.**
`AppNavHost` (`MainActivity.kt`, protected, fungsi ~700 baris mulai baris 530) adalah SATU
composable raksasa yang mengoleksi **35+ StateFlow sekaligus** lewat `collectAsStateWithLifecycle()`
tepat di scope teratasnya (baris 547-577 + 696) — termasuk `uiState` (`PlaybackUiState` gabungan,
field `position`/`duration` di-tick **tiap 500ms selama `isPlaying`** via `startPositionLoop()`
di `PlayerViewModel.kt` baris 885-909). `Scaffold` (baris 878) yang membungkus `MiniPlayerBar`
(baris 911, terima `uiState` utuh sbg parameter) DAN `NavHost` (baris 1092, semua destinasi
termasuk `now_playing`) ada DI DALAM function yang SAMA PERSIS itu — bukan 2 composable terpisah.
Karena `uiState` dibaca langsung di level teratas function (bukan di dalam lambda anak yang
sudah terisolasi scope-nya) — dipakai di baris 750/816/907/911/1132/1139/1167 utk keperluan
MiniPlayerBar visibility/currentSongId/dst. — setiap tick 500ms selama musik main memaksa
SELURUH scope recomposition `AppNavHost` invalid, bukan cuma bagian yang genuinely butuh posisi
terbaru (progress bar mini bar / slider Now Playing).

**Kenapa ini match PERSIS ke-4 gejala sekaligus**: MiniPlayerBar (baca `uiState` langsung, jelas
kena) — scroll list & ganti tab (SEMUA jalan sbg anak `Scaffold`/`NavHost` yang notabene anak
`AppNavHost` yang sama, jadi ikut kompetisi frame budget dgn recomposition storm 2x/detik function
induknya) — Now Playing/sheet (composable turunan struktur yang sama). **Kenapa "dari dulu gak
pernah fix"**: seluruh riwayat tuning performa Liquid Glass (Batch 296-300 blurRadius, Batch 299
liquidGlassAlpha, Batch 328 revert Aurora rim-glow animasi, Batch 329 cabut hazeEffect total)
SEMUANYA menyasar **COST render per-frame** (apa yang digambar) — bukan **FREKUENSI+SCOPE
recomposition** (seberapa sering & seluas apa yang dipaksa gambar ulang). Biarpun Haze dicabut
100% (Batch 329, cost render sudah nol), pemicu recomposition 2x/detik ini tetap ada & 0 pernah
tersentuh batch manapun — persis kenapa gejalanya "gak pernah benar-benar fix" walau berkali-kali
"diperbaiki".

**Kenapa 0 kode diubah batch ini (murni investigasi)**: (1) fix yang genuinely BUKAN sekadar
mitigasi (menurunkan frekuensi tick 500ms→1000ms cuma mengurangi gejala, bukan akar masalah)
perlu MEMISAHKAN `position`/`duration` dari `PlaybackUiState` gabungan jadi StateFlow tersendiri
yang dikoleksi LANGSUNG di scope lokal `MiniPlayerBar`/`NowPlayingScreen` sendiri (bukan
di-hoist di `AppNavHost`) — supaya tick 500ms cuma invalidate composable yang genuinely butuh,
bukan seluruh `AppNavHost`; (2) ini WAJIB menyentuh `MainActivity.kt` (**protected asset**, wajib
edit-parsial + minim-diff) + `PlayerViewModel.kt` + `MiniPlayerBar.kt` (+kemungkinan
`NowPlayingScreen.kt`) — lebih dari batas Micro-Batch 3 file kode & scope-nya arsitektural,
bukan micro; (3) sesuai pola project ini (Batch 290 minSdk bump, Batch 322 investigasi
`MainActivity.kt`): perubahan besar ke file protected WAJIB dikonfirmasi eksplisit user dulu
sebelum eksekusi, bukan diasumsikan/langsung ditembak.

**Pending Queue (file terpisah dibuat, WAJIB dibaca sesi berikutnya)**:
`PENDING_FixGlobalLagRecomposition.md` — rencana fix 2-3 batch terpisah + 2 opsi pendekatan
(surgical state-split vs mitigasi cepat throttle tick), menunggu user pilih sebelum eksekusi.

**Ringkasan file** — 0 file kode, 3 dokumentasi (PROJECT_STATE.md/CHANGELOG.md/README.md) +
1 file `PENDING_*.md` baru (kebal limit Micro-Batch, dokumentasi VIP). Dicek ulang persis
(`diff` manifest vs disk, 0 selisih sebelum batch ini): `FILE_MANIFEST.txt` 188→189
(nambah `PENDING_FixGlobalLagRecomposition.md`).

**Batch 350 (BUG FIX — swipe vertikal brightness/volume yang mendarat DI ATAS vinyl ditelan
gestur horizontal swipe-next/prev, 1 file kode)** — User laporan (setelah Batch 349 selesai):
"mau swipe brightness/volume malah yang ke geser album nya" — dan secara eksplisit menandai ini
BUKAN bug baru: **"dari dulu soal swipe brightness/volume, susahnya minta ampun"** (keluhan
kronis, bukan regresi dari Batch 349 kemarin). Diklarifikasi dulu via tappable option (4 opsi
gejala konkret) sebelum eksekusi — user pilih persis: **"Swipe brightness/volume malah ikut
ganti lagu"**.

**Root cause (kontradiksi dgn asumsi komentar lama Batch 112 dst.).** Komentar lama di `Row`
gesture ini eksplisit klaim "vinyl dapat first claim di dalam bounds-nya, HANYA leftover DI LUAR
bounds yang sampai ke zona brightness/volume" — asumsi ini TIDAK match perilaku device asli:
`detectHorizontalDragGestures` milik `AlbumArtHero` (pass Main, default) tetap "menang" duluan
utk touch yang mendarat DI ATAS vinyl, WALAU gerakan jarinya vertikal murni — karena 2 gesture
recognizer (kiri/kanan `detectVerticalDragGestures` vs vinyl `detectHorizontalDragGestures`)
sama-sama independen memantau raw pointer event yang sama tanpa 1 wasit yang memutuskan SUMBU
gerakan lebih dulu; vinyl kebetulan lebih "gigih" claim krn radius wajar pengguna paling sering
menyentuh area itu duluan (elemen visual terbesar di layar).

**Fix — 1 `pointerInput` BARU di `PointerEventPass.Initial`, 0 baris `AlbumArtHero` diubah.**
Ditambahkan di Box pembungkus vinyl (sebelum `AlbumArtHero` dipanggil): pass `Initial` dijalankan
Compose LEBIH DULU (top-down) drpd pass `Main` default milik gesture vinyl sendiri di bawahnya.
Selama sumbu belum jelas (belum lewati `touchSlop`), 0 event di-consume — vinyl bebas mendeteksi
sendiri seperti biasa (jaga kompatibilitas horizontal 100%). Begitu akumulasi delta melewati
slop: kalau dominan **horizontal** → tetap 0 disentuh, vinyl lanjut normal (swipe ganti lagu 0
regresi, threshold-120px/spring/haptic Batch 178/256 SAMA SEKALI tidak disentuh). Kalau dominan
**vertikal** → `change.consume()` di pass Initial mulai dari titik itu → `detectHorizontal-
DragGestures` vinyl (pass Main, berjalan belakangan) melihat change yang sudah consumed →
otomatis cancel via `onDragCancel` bawaannya sendiri (spring `dragOffset` balik ke 0, 0 kode baru
ditulis utk itu) — sementara delta-Y dialihkan ke `applyBrightness`/`applySystemVolume` yang SAMA
PERSIS dipakai 2 Box zona kiri/kanan existing, termasuk indikator pill + delay 600ms, biar 1
pengalaman konsisten dgn versi di luar vinyl. Kiri/kanan ditentukan dari X sentuh-awal relatif ke
lebar vinyl sendiri — karena vinyl dipusatkan di Box induk yang sama, titik tengahnya otomatis
sejajar garis tengah layar yang sama dipakai 2 zona existing (0 konversi koordinat manual).

**0 logic lama diubah** — 2 Box zona kiri/kanan (`detectVerticalDragGestures`) & gesture internal
`AlbumArtHero` (`detectHorizontalDragGestures`, threshold/spring/haptic) 100% utuh, murni
ditambal celah "vertikal yang mendarat DI ATAS vinyl". **1 file**: `NowPlayingScreen.kt`
(non-protected). Import baru: `awaitEachGesture`, `awaitFirstDown` (`androidx.compose.foundation.
gestures`), `PointerEventPass` (`androidx.compose.ui.input.pointer`), `kotlin.math.abs`. Delta
posisi dihitung manual via `change.position - change.previousPosition` (operator member `Offset`,
BUKAN ekstensi `positionChange()` — dihindari krn tidak yakin path importnya tanpa akses
dokumentasi online saat ini, `position`/`previousPosition` dipastikan properti langsung
`PointerInputChange`, risiko compile lebih rendah). Brace/paren diverifikasi ulang (stack-based,
comment/string di-strip): 244/244 brace (naik 16 dari Batch 349 — blok try/finally + when 3
cabang + while + pointerInput+awaitEachGesture lambda baru), 697/697 paren (naik 20), 0/0
bracket. `FILE_MANIFEST.txt` tidak berubah.

**Belum diverifikasi compile Gradle sungguhan** — WAJIB cek CI SEBELUM anggap ini beres; pola
`PointerEventPass.Initial` + `awaitEachGesture` + `awaitPointerEvent(pass=Initial)` adalah idiom
Compose Foundation yang dikenal (dipakai riil utk axis-disambiguation/pre-emptive gesture
interception), TAPI ini kali PERTAMA dipakai di file/app ini — risiko sintaks lebih tinggi drpd
batch tata-letak biasa, WAJIB cek log build Gradle Actions teliti (bukan cuma asumsi compile
sukses dari kompilasi mental/analisis statis). **Belum diverifikasi visual di device** —
prioritas cek: (1) swipe vertikal MULAI DI ATAS vinyl kini benar-benar mengubah brightness
(separuh kiri)/volume (separuh kanan), indikator pill muncul sama seperti versi di luar vinyl;
(2) swipe horizontal (ganti lagu) DI ATAS vinyl 0 regresi — masih threshold 120px, masih ada efek
geser+spring-back+haptic seperti sebelumnya; (3) tap singkat/tanpa gerak di vinyl (buka now
playing dari mini-player dst., kalau ada) 0 kena pengaruh (axisLocked tetap null, 0 consume,
tetap terusan Main pass ke handler lain kalau ada); (4) swipe DI LUAR vinyl (zona kiri/kanan
existing, 0 disentuh batch ini) tetap identik seperti sebelumnya — regresi 0 di jalur lama.
Detail: `CHANGELOG.md` Batch 350.

**Batch 349 (Row 4 ikon atas NowPlayingScreen: revert `SpaceBetween` → Tutup kiri sendiri +
3 ikon rapat kanan, 1 file kode)** — Lanjutan LANGSUNG dari Batch 348 (batch itu sendiri baru
saja mengonfirmasi `SpaceBetween` "sudah benar" via screenshot). User menyatakan tidak suka
dengan hasilnya ("jelek banget") tanpa detail awal — diklarifikasi 2 tahap via tappable option
(bukan ditebak langsung, sesuai `STOP->BLOCKER jika info kurang`): (1) kategori masalah — user
pilih "Urutan/posisi ikon"; (2) setelah user lebih spesifik lapor keberatannya sebenarnya soal
"row berjejer simetris" (bukan urutan logis 4 ikonnya), ditanya ulang arah pengelompokan yang
diinginkan — user pilih eksplisit **"Tutup sendiri di kiri, 3 ikon lain rapat di kanan"**.

**Catatan penting — ini pola yang SAMA PERSIS dengan versi PRA-Batch-342** (yang waktu itu
diganti ke `SpaceBetween` karena dilaporkan "berat sebelah/tidak profesional", dan investigasi
Batch 345 sempat mencatat pola ini sebagai "yang pernah ditolak"). **Ini BUKAN kesalahan
eksekusi** — preferensi user bisa berubah antar-sesi, dan pilihan REALTIME+EKSPLISIT sesi ini
(dikonfirmasi 2x lewat tappable option, bukan tebakan) adalah rujukan yang berlaku sekarang,
sesuai Hierarki `User Inst > Core Protocol > PROJECT_STATE.md`. Dicatat eksplisit di komentar
kode & di sini supaya sesi berikutnya TIDAK reflexively "mengoreksi balik" ke `SpaceBetween`
hanya karena riwayat lama menyebutnya sebagai "fix" — perubahan ini disengaja & dikonfirmasi.

**Fix.** `Row` induk: `horizontalArrangement = Arrangement.SpaceBetween` dibuang (balik ke
default `Start`). `Spacer(modifier = Modifier.weight(1f))` dipasang lagi persis setelah
`IconButton` Tutup (posisi sama seperti versi pra-Batch-342). Hasil: Tutup presisi mentok kiri,
Favorit+Info+Kontrol Lanjutan menumpuk rapat berdekatan di ujung kanan. Komentar Batch 342 di
atas `Row` (yang menjelaskan alasan SpaceBetween) diganti komentar Batch 349 baru; 1 kalimat di
komentar Batch 343 yang menyebut "SpaceBetween tetap dipertahankan, terbukti sudah benar" ikut
diperbarui (bukan dihapus) supaya tidak lagi kontradiktif dengan state kode saat ini — pola sama
disiplin dokumentasi anti-stale yang sudah dipakai batch-batch sebelumnya.

**0 ikon ditambah/dihapus/diganti fungsi, 0 urutan logis 3 ikon kanan diubah** (tetap
Favorit→Info→Kontrol Lanjutan, sesuai Batch 341), 0 handler/tint/contentDescription disentuh —
murni relokasi tata letak, `ZERO-REFACTOR` di luar itu. **1 file**: `NowPlayingScreen.kt`
(non-protected). Brace/paren diverifikasi ulang (stack-based, comment/string di-strip): 228/228
brace (identik Batch 347/348, 0 blok baru), 677/677 paren (naik 2 dari Batch 347's 675 — persis
dari 1 `Spacer(...weight(1f))` baru, `Arrangement.SpaceBetween` yang dibuang 0 paren), 0/0
bracket. `FILE_MANIFEST.txt` tidak berubah.

**Belum diverifikasi compile Gradle sungguhan** — WAJIB cek CI. Risiko sintaks sangat rendah
(murni buang 1 named-argument + tambah 1 `Spacer` yang sudah dipakai identik di versi lama file
ini sebelum Batch 342, pola sudah terbukti compile dulu). **Belum diverifikasi visual di
device** — prioritas cek: Tutup presisi kiri mentok, Favorit/Info/Kontrol Lanjutan menumpuk
rapat di kanan mentok (bukan lagi renggang merata), 0 regresi handler/tint/icon (Favorit toggle,
Info toggle kartu tip, ⋮ buka sheet Kontrol Lanjutan tetap identik). Detail: `CHANGELOG.md`
Batch 349.

**Batch 348 (Verifikasi Row 4 ikon atas NowPlayingScreen via screenshot device sungguhan — 0
kode, 3 dokumentasi)** — User kirim screenshot Now Playing screen sungguhan (identitas Liquid
Glass, backdrop artwork lagu phonk-mix non-persegi — konsisten fix crop `ContentScale` Batch
344) + transkrip klarifikasi sesi sebelumnya: sempat perlu dipastikan yang dimaksud "Row 4"
adalah Row ikon atas (Tutup/Favorit/Info/Kontrol Lanjutan, sejak Batch 342/343), **bukan** panel
album, lalu ditanyakan arah perubahan yang diinginkan. User jawab eksplisit: "yang normal dan
generik terbaik aja!!" — serahkan ke standar generik terbaik, bukan preferensi spesifik baru.

**Verifikasi (dicek ulang ke kode, bukan diasumsikan benar).** `NowPlayingScreen.kt` Row ikon
atas (baris ~429-503): `horizontalArrangement = Arrangement.SpaceBetween` (Batch 342, sudah
dikonfirmasi pixel-perfect merata via analisis Batch 345) + `Icons.Outlined.Info` (Batch 343,
bobot visual guratan-tipis seragam dgn 3 ikon lain: chevron/heart-border/dots) — dicocokkan ke
screenshot: 4 ikon (chevron-bawah, hati-garis, info-lingkaran-garis, titik-tiga-vertikal)
tersebar merata sepanjang lebar layar, gaya ikon konsisten tipis/outline di semuanya. Pola ini
PERSIS pola generik standar aplikasi pemutar musik sejenis (Spotify/Apple Music/YouTube Music) —
definisi harfiah "normal dan generik" yang diminta user.

**Kesimpulan: 0 gap ditemukan, 0 kode diubah.** Implementasi Batch 342/343/345 yang sudah ada
TERBUKTI (lewat screenshot device sungguhan kali ini — bukti lebih kuat dari analisis pixel
statis Batch 345 sebelumnya) sudah memenuhi permintaan user apa adanya. Batch ini murni
dokumentasi konfirmasi (`Mandatory Docs Sync`), supaya sesi berikutnya tidak keliru menganggap
Row ini belum diverifikasi atau masih ada pekerjaan tertunda di baliknya.

**0 file kode disentuh** — `FILE_MANIFEST.txt` tidak berubah (0 file baru/dihapus).
`PROJECT_STATE.md`/`CHANGELOG.md`/`README.md` disinkronkan (dokumentasi VIP, kebal limit
Micro-Batch). **Belum ada laporan/instruksi baru tersisa** — sesi berikutnya kembali ke
`ROADMAP_LIQUID_GLASS_REDESIGN.md` kalau 0 instruksi/temuan baru user (aturan sesi #4).

**Batch 347 (Penyempurnaan: `Radius.hero` ikut skala proporsional ke `artSize` dinamis Batch
346, `NowPlayingScreen.kt`, 1 file kode)** — User pilih eksplisit lanjutkan trade-off yang sengaja
ditunda Batch 346 ("Radius.hero ikut skala", opsi ke-2 dari 3 pilihan yang ditawarkan sesi ini).

**Formula.** `heroCornerRadius = Radius.hero * (artSize / 280.dp)` — 280dp baseline (konsisten
dgn konvensi `dynamicArtSize` Batch 346 yang sengaja balik ke 280dp persis di layar 360dp lebar).
`Dp.div(Dp): Float` & `Dp.times(Float): Dp` dicek ulang lewat dokumentasi resmi Compose sebelum
dipakai (operator baku kelas `Dp`, `Dp * Float` sendiri sudah punya preseden di file ini —
`screenHeightDp * 0.28f`, formula `albumArtBoxHeight`). Hasilnya: sudut piringan tetap
PROPORSIONAL di ukuran manapun (piringan besar → sudut ikut besar, piringan kecil → sudut ikut
kecil), bukan radius absolut tetap yang relatif makin "tajam" saat piringan membesar.

**Scope SENGAJA dibatasi ke cabang non-panel (Apple/default) saja.** Token `Radius.hero` GLOBAL
itu sendiri (`Spacing.kt`) TIDAK disentuh — dipakai HANYA sbg nilai baseline lokal di sini, bukan
diubah jadi dinamis (token itu juga dipakai `Theme.kt` utk `MaterialTheme.shapes.large`, dampak
globalnya jauh di luar 1 layar ini kalau diubah langsung). Cabang Tactile/Skeu (`isPanelTheme ->
MaterialTheme.shapes.large`) SENGAJA TIDAK ikut diskalakan — 2 identitas itu memang didesain
pakai bahasa sudut SERAGAM lintas berbagai ukuran permukaan (panel/sheet/kartu lain di app ini
semua pakai radius theme yang sama, bukan proporsional per-objek); mengikutkan hero art di sini
justru bikin hero beda sendiri dari permukaan besar lain di identitas yang sama — kebalikan dari
konsistensi yang justru diinginkan bahasa desain panel itu. Scope ini persis sesuai literal yang
dikonfirmasi user ("Radius.hero ikut skala") — token itu spesifik cuma dipakai di cabang non-panel.

**1 file**: `NowPlayingScreen.kt` (non-protected). 1 val baru (`heroCornerRadius`) + 1 baris
`heroShape` diubah (`Radius.hero` literal → `heroCornerRadius` terhitung, cabang panel 0
disentuh) + 1 blok komentar rasional. 0 file lain disentuh (termasuk `Spacing.kt`/`Theme.kt`, 0
token global diubah), 0 logic Batch 334/343/346 lain diubah (`ZERO-REFACTOR`). Brace/paren/
bracket seimbang & valid (stack-based matcher, comment/string di-strip): 228/228 brace, 675/675
paren (naik 1 paren dari Batch 346 — cuma dari `(artSize / 280.dp)`), 0/0 bracket.

**Belum diverifikasi compile Gradle sungguhan** — WAJIB cek CI. Risiko sintaks rendah (`Dp.div`/
`Dp.times` API resmi Compose, dikonfirmasi via dokumentasi; `Dp * Float` sudah punya preseden
jalan di file yang sama). **Belum diverifikasi visual di device** — prioritas cek: (1) sudut
piringan identitas Apple/default terlihat proporsional di layar tinggi (art besar) MAUPUN layar
pendek/hint tampil (art kecil), tidak lagi "kurang membulat" saat besar; (2) identitas Tactile/
Skeu VISUALNYA TIDAK BERUBAH SAMA SEKALI dari Batch 346 (regresi 0, cabang itu sengaja tidak
disentuh); (3) 0 elemen LAIN di app (dialog/sheet/kartu manapun yg pakai `MaterialTheme.shapes.
large`) berubah tampilan — `Radius.hero` token global dikonfirmasi tidak diedit. Detail:
`CHANGELOG.md` Batch 347.

**Batch 346 (Fitur: art scale dinamis mengisi sisa ruang ala Spotify — piringan membesar/
mengecil otomatis, bukan lagi gap kosong, `NowPlayingScreen.kt`, 1 file kode)** — User pilih
eksplisit lanjutkan trade-off yang dicatat Batch 345 (opsi "Lanjut ide 'art scale dinamis'" dari
3 pilihan yang ditawarkan sesi ini). Scope memang lebih besar dari Micro-Batch biasa (blur/glow/
hero shape ikut terdampak, seperti sudah diperingatkan Batch 345) — dieksekusi SEKARANG karena
user sudah konfirmasi eksplisit, bukan diam-diam.

**Pendekatan: ukur, bukan tebak/hardcode.** Grup konten judul-s/d-waktu (dulu dipusatkan
`Arrangement.Center` Batch 345) dibungkus 1 Column baru yang diukur `onGloballyPositioned`
(`contentGroupHeightPx`) — kunci teknis: `verticalScroll` (Column induknya) memberi constraint
tinggi TAK TERBATAS ke children, jadi tinggi yang dilaporkan SELALU intrinsik/asli, tidak pernah
terpotong `weight(1f)`. Pola `onGloballyPositioned` ini BUKAN hal baru di codebase (sudah dipakai
identik di `LibraryScreen.kt`/`QueueSheet.kt`/`SongPickerSheet.kt`/`PlaylistScreen.kt`).

**Formula.** `dynamicArtSize` = (tinggi konten tersedia − chrome tetap [Row ikon 48dp + Spacer
12dp + Spacer 16dp + Row transport 68dp] − tinggi grup konten terukur − 20dp selisih art↔glow),
di-clamp `[140dp, lebarLayar−80dp]` (batas lebar: piringan persegi tak boleh lebih lebar dari
layar; 80dp = margin default lama, formula SENGAJA balik ke 280dp persis di layar 360dp lebar —
0 lompatan visual di device umum). Row ikon (48dp)/Row transport (68dp) SENGAJA konstanta
(deterministik dari kode sendiri — IconButton default & `.size(68.dp)` eksplisit), bukan diukur
run-time, demi 1 measurement loop lebih sedikit. Sebelum pengukuran pertama mendarat
(`contentGroupHeightPx == 0`, 1 frame awal), fallback ke `albumArtBoxHeight` (formula lama Batch
336, tetap valid sbg adaptif layar pendek) — 0 flash ukuran aneh. `verticalArrangement = Center`
(Batch 345) SENGAJA TIDAK dihapus — jaring pengaman visual utk 1 frame transisi itu saja.

**`AlbumArtHero()` diparameterisasi.** Dulu hardcode `.size(300.dp)` (glow)/`.size(280.dp)`
(art) literal — sekarang terima `artSize: Dp` dari caller, glow tetap `artSize + 20.dp` (rasio
lama dipertahankan persis). Shadow/bevel Tactile/Skeu/border/scanline TIDAK disentuh (semua pakai
`size` dari `drawBehind` scope, otomatis ikut skala) — KECUALI 1 komentar basi ("ukurannya selalu
tetap 280.dp", justifikasi margin halo 18dp) diperbarui supaya tidak menyesatkan sesi berikutnya;
angka literal margin halo itu sendiri (18dp/14dp/dst) SENGAJA TETAP konstan di semua ukuran
(bukan proporsi visual yang perlu ikut skala).

**1 file**: `NowPlayingScreen.kt` (non-protected). 2 import baru (`onGloballyPositioned`, `Dp`),
1 state baru (`contentGroupHeightPx`), 1 Column pembungkus pengukur, 1 blok kalkulasi
`dynamicArtSize`/`dynamicGestureBoxHeight`, 1 parameter baru `AlbumArtHero(artSize)`. 0 file lain
disentuh, 0 logic gesture Batch 334/footer Batch 343 diubah (`ZERO-REFACTOR`). Brace/paren/
bracket seimbang & valid (stack-based matcher, comment/string di-strip): 228/228 brace, 674/674
paren (naik dari 222/222 & 663/663 sebelum batch ini — net penambahan wajar sesuai kode baru).

**Belum diverifikasi compile Gradle sungguhan** — WAJIB cek CI. **Belum diverifikasi visual di
device** — prioritas cek: (1) piringan membesar mengisi ruang kosong di layar tinggi/normal
(bukan lagi 2 gap Batch 345); (2) piringan tetap proporsional/tidak melebihi lebar layar; (3) di
layar pendek/hint banner tampil, piringan menyusut wajar & transport tetap presisi di tepi bawah
(0 regresi Batch 336-343); (4) transisi ukuran 1 frame awal (fallback → dinamis) tidak kelihatan
kedip. Detail: `CHANGELOG.md` Batch 346.

**Batch 345 (Fix gap kosong tunggal jadi terdistribusi — `verticalArrangement` Top → `Center`,
NowPlayingScreen, 1 file kode)** — User kirim 2 screenshot (crop Row 4-ikon atas + crop area
waktu/transport) + 2 laporan: (1) "susunan badge anomali yang terpaku oleh jarak", (2) "masih
ada bagian kosong karena bagian atas terlalu mentok ke badge — gak ada susunan normal begitu".

**Investigasi poin 1 (Row ikon atas) — diukur, bukan ditebak.** Analisis pixel langsung: jarak
antar-4-ikon 279px/280px/279px, PERSIS merata — `SpaceBetween` (Batch 342) masih benar 100%.
Bobot visual ke-4 ikon juga konsisten tipis — `Outlined.Info` (Batch 343) masih benar. Cross-
check riwayat: Batch 342 EKSPLISIT reject pola "1 ikon kiri + 3 klaster kanan" — jadi kembali ke
grouped BUKAN arah benar (mengulang keluhan lama). Kesimpulan: 0 defect nyata di Row itu sendiri.

**Root cause sebenarnya (dikonfirmasi via screenshot ke-2).** Fix Batch 343 (Row transport jadi
footer fixed di luar Column scrollable) MEMINDAH lokasi gap kosong, TIDAK MENGHILANGKANNYA.
Column scrollable+`weight(1f)` masih `verticalArrangement` default (Top) — begitu tinggi konten
(judul s/d waktu) LEBIH PENDEK dari ruang weighted tersedia (kasus layar user), semua sisa ruang
tetap menumpuk jadi SATU gap besar — cuma lokasinya pindah dari "di bawah Row transport"
(sebelum Batch 343) jadi "di antara baris waktu & Row transport" (setelah Batch 343). Laporan
poin 1 & 2 ternyata SATU root cause dilihat dari 2 sudut: konten atas tetap "mentok"/"terpaku"
walau ruang tersedia jauh lebih tinggi — 0 distribusi proporsional, semua kosong dikumpulkan di
1 sisi, bukan "susunan normal" app sejenis.

**Fix.** `verticalArrangement = Arrangement.Center` di Column scrollable itu (import `Arrangement`
sudah ada via wildcard, 0 import baru). Saat konten lebih pendek dari viewport, `Center` membagi
sisa ruang proporsional ke ATAS (art box↔judul) & BAWAH (waktu↔transport) — 1 gap besar jadi 2
gap seimbang. 0 efek saat konten >= tinggi viewport (layar pendek, scroll identik, 0 regresi).
Row transport TETAP fixed footer di tepi bawah — Batch 343 TIDAK dibatalkan (sudah dikonfirmasi
user "no more floating thing").

**Trade-off disadari, dicatat bukan diselesaikan diam-diam:** solusi paling "otentik" (album art
scale dinamis mengisi sisa ruang, ala Spotify) cakupannya jauh lebih besar (blur/glow/hero shape
ikut terdampak) — di luar Micro-Batch aman. `Center` = fix minimal-resiko sekarang; kalau user
masih kurang puas & mau art scale dinamis, itu task terpisah, WAJIB konfirmasi eksplisit dulu.

**1 file**: `NowPlayingScreen.kt` (non-protected). 1 parameter baru + 1 blok komentar root-cause.
0 komposable lain disentuh, 0 logic Batch 334/343 diubah (`ZERO-REFACTOR`). Brace/paren seimbang
raw (227/227, 974/974) + strip-komentar (226/226, 672/672 — IDENTIK sebelum batch ini,
`Arrangement.Center` 0 paren baru). Bracket-matcher stack-based dijalankan ulang, valid. Diff vs
ZIP batch 344 dikonfirmasi HANYA 1 hunk, 0 file lain tersentuh. `FILE_MANIFEST.txt` tidak berubah.

**Belum diverifikasi compile Gradle sungguhan** — WAJIB cek CI. Risiko sintaks sangat rendah
(`Arrangement.Center` API resmi, sudah dipakai identik di file lain project ini). **Belum
diverifikasi visual di device** — prioritas cek: (1) di layar tinggi/ruang lebih, sekarang 2 gap
breathing room (bukan 1 gap besar tunggal); (2) Row transport tetap presisi di tepi bawah (0
regresi Batch 343); (3) layar pendek/konten panjang, scroll identik seperti sebelumnya; (4) Row
4-ikon atas TIDAK berubah tampilan (0 kode disentuh di situ). Detail: `CHANGELOG.md` Batch 345.

**Batch 344 (Fix sistemik: cover art letterbox — `ContentScale.Fit` → `Crop` di 1 fungsi
bersama `AlbumArt()`, 1 file kode)** — User kirim screenshot lagu "TOBI - Warm Up Mix 2023" +
instruksi "saya mau layout normal dan generik". Screenshot: kotak seni utama Now Playing (hero
280dp) tampil dgn bar kosong solid di atas & bawah gambar (mirip letterbox video) — foto
konser/kembang api TIDAK penuh mengisi kotak persegi, beda drastis dari artwork lagu di
screenshot Batch 342/343 sebelumnya (penuh edge-to-edge, 0 bar).

**Root cause (dikonfirmasi via kode).** `AlbumArt()` (`Utils.kt`) — composable BERSAMA dipakai
di 6 titik seluruh app (MiniPlayerBar 44dp, LibraryScreen grid album `aspectRatio(1f)` + row
48dp, HomeScreen 56dp + 120dp, NowPlayingScreen hero 280dp) — punya default parameter
`contentScale = ContentScale.Fit`. `Fit` mempertahankan SELURUH gambar tanpa crop di dalam
batas kotak — untuk artwork non-1:1 (kasus nyata: thumbnail video 16:9 ikut ke-embed saat lagu
di-rip/tag dari YouTube, umum utk file DJ-mix/mashup), sisa ruang kosong di atas/bawah gambar
menampilkan `Box.background(surfaceVariant)` polos — di tema gelap kelihatan persis bar hitam
letterbox. Grep konfirmasi: dari 6 titik pemakaian, HANYA 1 (backdrop blur full-screen Now
Playing, `NowPlayingScreen.kt` ~baris 368) eksplisit override `Crop` sejak awal — makanya bug
ini tidak pernah kelihatan di backdrop, tapi laten di 5 titik lain (kotak seni utama/thumbnail),
baru nampak saat kebetulan artwork lagu yang diputar bukan rasio persegi. Preseden project
sendiri sudah konsisten ke arah Crop: widget home-screen (`widget_player.xml`, Batch 204) sudah
`centerCrop`, bukan letterbox — genre-standar universal (Spotify/Apple Music/YouTube Music
SELALU crop-fill cover art) — persis makna "normal dan generik" yang diminta user.

**Fix.** `Utils.kt`: `contentScale: ContentScale = ContentScale.Fit` → `= ContentScale.Crop`.
Karena DEFAULT di fungsi bersama, 5 titik yang sebelumnya diam-diam mengandalkannya (MiniPlayerBar,
Library×2, Home×2, hero Now Playing) otomatis ikut benar sekaligus — TIDAK perlu sentuh 5 file
caller satu per satu (lebih aman, permukaan diff lebih kecil daripada override eksplisit per
titik). Override `Crop` eksplisit di backdrop blur Now Playing SENGAJA TIDAK dihapus meski kini
redundan — bukan bagian dari bug ini, `ZERO-REFACTOR`.

**1 file**: `Utils.kt` (non-protected) — 1 default parameter diganti + 1 blok komentar
root-cause. 0 logic lain di fungsi disentuh (fallback icon, tinted background "no cover",
loading/error state `SubcomposeAsyncImage` — semua persis sama). 0 dari 6 titik pemakaian
`AlbumArt()` di-edit langsung. Brace/paren seimbang (17/17 `{}`, 74/74 `()`, RAW = strip-komentar
karena tambahan cuma komentar dokumentasi, 0 kode struktural baru). `FILE_MANIFEST.txt` tidak
berubah.

**Belum diverifikasi compile Gradle sungguhan** — WAJIB cek CI. Risiko sintaks sangat rendah
(ganti 1 nilai default parameter enum, keduanya API resmi Compose UI yang sudah dipakai project
ini). **Belum diverifikasi visual di device** — prioritas cek: (1) kotak seni utama Now Playing
lagu artwork non-1:1 sekarang penuh mengisi 280dp, 0 bar kosong; (2) ke-4 titik thumbnail lain
(MiniPlayerBar, Library grid & row, Home) konsisten crop-fill juga; (3) lagu artwork PERSEGI
(mis. cover Batch 342/343) tampilan TIDAK berubah (Crop pada gambar sudah persegi = identik
hasil dgn Fit sebelumnya, regresi visual 0); (4) lagu tanpa artwork — fallback icon `MusicNote`
tetap normal (jalur `showIcon`, tidak tersentuh). Detail: `CHANGELOG.md` Batch 344.

**Batch 343 (Fix 2 laporan user dalam 1 pesan: kontrol transport "mengambang" + ikon Info Row
atas anomali, NowPlayingScreen, 1 file kode)** — User kirim screenshot + instruksi eksplisit:
"bagian pemutar dilarang keras untuk mengambang/tidak menyentuh dasar sama sekali" DAN
"perbaiki layout menu-menu yang kelihatan anomali dibagian atas alih-alih rapi".

**Bug 1 — root cause (transport mengambang).** Row transport (shuffle/prev/play-pause/next/
repeat) sebelumnya jadi child TERAKHIR di dalam `Column` yang sekaligus `weight(1f)` +
`verticalScroll(...)` (arsitektur "fixed header + scrollable body", Batch 334). `Column` biasa
(verticalArrangement default = Top) menaruh anak-anaknya rapat dari ATAS ruang yang tersedia —
begitu total tinggi konten (judul s/d slider) LEBIH PENDEK dari tinggi weighted-area (kasus umum
di layar normal/tinggi, art box sudah fixed 300dp duluan di header), transport row berhenti
persis di bawah kontennya sendiri, MENYISAKAN spasi kosong di antara transport dan tepi bawah
layar — persis "mengambang" di screenshot user, bukan sekadar kurang padding.

**Fix Bug 1 (struktural, bukan tuning angka).** Penutup `Column` scrollable dipindah lebih awal
(tepat setelah Row waktu posisi/durasi, SEBELUM Row transport) — Row transport (+ `Spacer(16.dp)`
pemisahnya) jadi sibling TETAP (fixed) milik `Column` induk (`fillMaxSize`), BUKAN lagi child di
dalam area scroll. Karena `Column` induk menaruh `Column` scrollable itu dengan `weight(1f)`,
sisa ruang vertikal SELALU mengalir penuh ke situ dulu, dan Row transport (fixed, ukuran
instrinsik) otomatis jadi child PALING TERAKHIR `Column` induk — Row transport sekarang SELALU
presisi di tepi bawah (sebelum padding 20dp layar), 0 spasi kosong tersisa, apa pun tinggi
konten/layarnya. **Bonus**: ini sekaligus menuntaskan saga reachability transport Batch 336-338
lebih kuat — transport SEKARANG SELALU terlihat tanpa perlu discroll sama sekali (bukan cuma
"terjangkau via scroll"). 0 logic scroll/gesture/timing lain diubah — murni 1 child dipindah.

**Bug 2 — root cause (ikon Info anomali).** Batch 342 sudah memperbaiki SPACING Row 4-ikon atas
(`Arrangement.SpaceBetween`), dan screenshot user kali ini mengonfirmasi spacing itu memang
sudah renggang merata — TAPI user masih lapor "anomali". Root cause beda level dari Batch 342:
`Icons.Default.Info` (varian "Filled") me-render sebagai lingkaran PADAT/solid dengan "i" di
dalamnya — satu-satunya ikon berbentuk badge solid di antara 3 ikon lain di Row yang sama
(Tutup/chevron, Favorit-border, Kontrol Lanjutan/titik-tiga) yang semuanya guratan tipis/outline.
Bobot visual jomplang inilah yang terbaca "anomali" — persis kelas masalah "samakan visual weight
icon sejenis" yang sudah pernah diaudit project ini (Batch 228).

**Fix Bug 2.** `Icons.Default.Info` → `Icons.Outlined.Info` (lingkaran garis tipis + "i" tipis,
bobot visual seragam dgn 3 ikon lain). Paket `material-icons-extended` (sumber `Icons.Outlined.*`)
SUDAH jadi dependency app ini sejak lama (grep `app/build.gradle.kts` konfirmasi) — 0 dependency
baru, TAPI ini pertama kalinya file ini (atau project ini secara umum, grep app-wide 0 hit lain)
memakai varian `Outlined` — pola baru, sengaja dipilih HANYA di 1 titik ini karena itu yang
genuinely menjawab laporan user, bukan diterapkan spekulatif ke ikon lain. Import
`filled.Info` dihapus (1 satu-satunya pemakaian di file ini, dikonfirmasi grep), diganti
`outlined.Info` — pelajaran Batch 233 (ganti varian ikon WAJIB sertakan update import) diikuti.
0 posisi/spacing/handler/tooltip Row ini disentuh — `SpaceBetween` Batch 342 dipertahankan apa
adanya, terbukti sudah benar dari screenshot user.

**1 file**: `NowPlayingScreen.kt` (non-protected). 1 import dihapus + 1 import baru (net 0). 0
handler/callback/urutan logis ikon diubah, 0 komposable lain di file ini disentuh
(`ZERO-REFACTOR`). Brace/paren seimbang raw (227/227, 953/953 — naik murni dari komentar baru,
pola false-positive yang sudah berulang kali dicatat, mis. Batch 335/342) + strip-komentar
(226/226, 672/672 — IDENTIK dengan sebelum batch ini, murni relokasi brace + swap 1 ikon, 0
logic baru/hilang). Verifikasi tambahan: bracket-matcher berbasis stack (comment & string
di-strip dulu, bukan cuma hitung jumlah simbol) dijalankan atas seluruh file — konfirmasi
struktur bersarang genuinely valid. `FILE_MANIFEST.txt` tidak berubah.

**Belum diverifikasi compile Gradle sungguhan** — WAJIB cek CI. Risiko sintaks rendah: Bug 1
murni memindah posisi 1 kurung kurawal penutup + 1 blok kode yang isinya tidak diubah sama
sekali; Bug 2 pakai API resmi Compose Material Icons Extended dari dependency yang sudah lama
terpasang (baru pertama kali dipakai sebagai `Outlined` di project ini, tapi paketnya sendiri
sudah teruji lewat `Icons.Default`/`Filled` di ratusan titik lain). **Belum diverifikasi visual
di device** — prioritas cek: (1) Row 5 tombol transport presisi menempel tepi bawah layar (0
spasi kosong di bawahnya) baik di layar tinggi/normal maupun pendek; (2) konten judul-slider di
atasnya TIDAK terpotong/berubah (murni Row transport yang pindah); (3) ikon Info Row atas
sekarang lingkaran garis tipis (bukan lagi padat solid), bobot visual seragam dgn 3 ikon lain;
(4) tap ikon Info tetap toggle kartu tip gestur seperti biasa (0 fungsi berubah). Detail:
`CHANGELOG.md` Batch 343.

**Batch 342 (Relokasi tata letak Row ikon atas NowPlayingScreen jadi simetris, 1 file kode)** —
User kirim screenshot + instruksi eksplisit: "relokasi layout agar simetris dan professional
look!!". Screenshot menunjukkan Row ikon atas (Tutup/Favorit/Info/Kontrol Lanjutan) berat
sebelah: Tutup terisolasi di kiri dengan jarak kosong besar, 3 ikon lain (Favorit/Info/Kontrol
Lanjutan — Info baru ditambah Batch 341) menumpuk rapat di kanan.

**Root cause** — `Row` induk cuma punya 1 `Spacer(modifier = Modifier.weight(1f))` tunggal tepat
setelah `IconButton` Tutup, mendorong SEMUA sisa ikon jadi 1 klaster di ujung kanan (1 ikon
lawan 3, bukan renggang merata). Murni akibat tata letak `Spacer` manual — tiap ikon sendiri
(handler/tint/contentDescription) sudah benar, 0 bug fungsional.

**Fix (relokasi murni tata letak, 0 ikon ditambah/dihapus/diganti fungsi)** — `Spacer(weight(1f))`
dibuang, `Row` induk diberi `horizontalArrangement = Arrangement.SpaceBetween` (0 import baru,
`Arrangement` sudah masuk lewat wildcard `androidx.compose.foundation.layout.*` yang sudah ada
di file ini). Ke-4 `IconButton` sekarang tersebar merata sepanjang lebar Row — Tutup tetap
presisi kiri mentok, Kontrol Lanjutan tetap presisi kanan mentok (0 perubahan posisi tepi),
Favorit & Info dapat jarak proporsional di antaranya, bukan lagi 1 klaster. Urutan logis ikon
(Tutup→Favorit→Info→Kontrol Lanjutan, dari Batch 341) TIDAK diubah — cuma jarak antar-ikon yang
direlokasi.

**1 file**: `NowPlayingScreen.kt` (non-protected). 0 import baru, 0 handler/tint/
contentDescription disentuh, 0 komposable lain di file ini disentuh (ZERO-REFACTOR). Brace/paren
seimbang (226/226, 925/925 raw; 226/226, 671/671 strip-komentar). `FILE_MANIFEST.txt` tidak
berubah. **Belum diverifikasi compile Gradle sungguhan** — WAJIB cek CI. **Belum diverifikasi
visual di device** — prioritas cek: buka Now Playing, konfirmasi 4 ikon Row atas renggang merata
(bukan lagi 1 kiri + 3 menumpuk kanan), Tutup & Kontrol Lanjutan tetap presisi di kedua tepi
layar (0 regresi posisi tepi). Detail: `CHANGELOG.md` Batch 342.

**Batch 341 (FITUR — ganti banner onboarding auto-tampil-sekali jadi tombol info permanen di
Row atas NowPlayingScreen, 1 file kode)** — User laporan + screenshot NowPlayingScreen: banner
tip gestur (Batch 112, "Geser piringan: kiri=kecerahan, kanan=volume. Ketuk ⋮ buat Sleep Timer,
Kecepatan & Equalizer.") "bisa kena dismiss permanen dan gak balik lagi" — begitu di-tap X
sekali, hilang SELAMANYA (persist via `OnboardingHintStore.markNowPlayingHintSeen()`), 0 cara
buka lagi kalau user lupa isinya/salah tap X. Instruksi eksplisit: "mending buat kan button
khusus onboarding disamping logo love" — bukan lagi banner auto-tampil, tombol permanen.

**Perubahan mekanisme (total, bukan tambal)** — `showNowPlayingHint` (state yang sama, dipakai
ulang 1:1) TIDAK LAGI diinisialisasi dari `!hintStore.hasSeenNowPlayingHint()` (auto true di
first-launch) — sekarang mulai `false`, murni dikontrol toggle tombol baru (ikon `Info`, di Row
atas persis di samping ikon favorit sesuai instruksi). `onDismiss` kartu (`FeatureHintBanner`,
komponen 0 diubah, dipakai ulang apa adanya) TIDAK LAGI panggil `hintStore.markNowPlayingHintSeen()`
— cuma toggle tutup kartu SAAT INI, bisa dibuka lagi kapan saja via tombol yang sama (toggle,
simetris: tap buka / tap lagi tutup, sama seperti tombol X di kartu). `OnboardingHintStore` &
`hintStore` (import + variabel) DIHAPUS dari file ini (jadi genuinely tidak terpakai) — TAPI
class `OnboardingHintStore` itu sendiri (`data/OnboardingHintStore.kt`) SENGAJA TIDAK disentuh/
dihapus, karena masih dipakai `LibraryScreen.kt` untuk hint lain yang tidak terkait (ZERO-REFACTOR).

**Efek samping yang sengaja diikutkan (bukan bug baru, konsekuensi logis)** — cabang
`showNowPlayingHint -> 260.dp` di `albumArtBoxHeight` (Batch 338, susutkan art box preemptive
selama hint "kebetulan" masih nongol) DIHAPUS: alasannya sudah tidak berlaku sama sekali sejak
hint tidak lagi otomatis muncul tanpa diminta — kalau user SEKARANG tap tombol info, itu aksi
sadar/sengaja, wajar kalau perlu scroll dikit buat nutup lagi, bukan lagi "kejutan" first-launch
yang harus dikompensasi preemptif. Ini secara efektif MENUNTASKAN seluruh saga scroll-reachability
Batch 336-338 (yang akar masalahnya justru banner auto-tampil-tak-diminta ini) — cabang layar
pendek (`screenHeightDp < 640.dp`, Batch 336) TIDAK disentuh sama sekali, itu fix legitimate
terpisah (device fisik pendek), 0 terkait hint.

**1 file**: `NowPlayingScreen.kt` (non-protected). 1 import baru (`Icons.Default.Info`), 1 import
dihapus (`OnboardingHintStore`, sudah tidak terpakai di file ini). Brace/paren seimbang raw
(226/226, 920/920 — kebetulan bersih tanpa perlu strip komentar batch ini) + dicek ulang strip-
komentar juga (226/226, 674/674). **Belum diverifikasi compile Gradle sungguhan** — WAJIB cek
CI. **Belum diverifikasi visual di device** — prioritas cek: (1) tombol info (ikon 🛈, warna
`primary` saat kartu terbuka / `secondary` saat tertutup) muncul persis di antara ikon favorit
& ikon Kontrol Lanjutan (⋮); (2) tap sekali → kartu tip muncul (posisi sama seperti banner lama,
di atas "SEDANG DIPUTAR"); (3) tap tombol info LAGI (atau X di kartu) → kartu tutup; (4) tap
tombol info berkali-kali (buka-tutup-buka-tutup) → HARUS selalu bisa dibuka lagi, TIDAK PERNAH
"habis"/permanen hilang seperti sebelumnya — ini inti dari fix batch ini; (5) art box SEKARANG
selalu 300dp penuh di layar normal (kecuali layar pendek <640dp) — TIDAK lagi menyusut ke 260dp
cuma krn kartu tip lagi kebuka.

**Batch 340 (Lanjutan antrean "🔍 Audit tambahan" Batch 339: fix `.frostedGlass()` kelewat di 3
dari 6 sheet tersisa, 3 file kode)** — User upload ZIP baru (`AudioPlayer_v339_Batch1.zip`,
lompat dari internal Batch 323 sesi sebelumnya ke Batch 339 — Hard Reset, ZIP user = source of
truth, isi Batch 324-339 dari sesi lain TIDAK di-merge/ditimpa, cuma dibaca ulang) + instruksi
"sempurnakan latest task!!". Batch 339 (sesi lain) menemukan gap lebih dalam dari perkiraan
Batch 322/323 sesi ini: `containerColor = Color.Transparent` ternyata TIDAK CUKUP sendirian —
sheet yang kelewat `.frostedGlass()` (elemen yang benar-benar menggambar blur) jadi TEMBUS
PANDANG SUNGGUHAN (0 blur, 0 fill), bukan cuma "kurang blur" seperti diasumsikan sebelumnya.
Batch 339 sudah fix 1 contoh (`UpdateCheckSheet.kt`) + catat eksplisit 6 sheet lain kena gap
SAMA PERSIS sebagai antrean "BOLEH dikerjakan tanpa tanya ulang, pola identik".

**Verifikasi ulang (bukan cuma percaya log Batch 339 mentah-mentah)** — grep langsung ke 7 file
tsb di ZIP baru: `UpdateCheckSheet.kt` dikonfirmasi 4x `frostedGlass()` (sudah diperbaiki
Batch 339). 6 sisanya dikonfirmasi 0x `frostedGlass()` di seluruh file (`BackupRestoreSheet.kt`,
`DiagnosticLogSheet.kt`, `DuplicateFinderSheet.kt`, `SignatureMatcherSheet.kt`,
`SmartPlaylistScreen.kt`, `VaultSheet.kt`) — gap dikonfirmasi nyata, bukan cuma klaim.

**Fix (3 dari 6, batas Micro-Batch)** — `.frostedGlass()` ditambah di posisi identik
`UpdateCheckSheet.kt` (setelah `.fillMaxWidth()`, sebelum modifier scroll/height berikutnya) +
import `com.rudi.audioplayer.ui.theme.frostedGlass` (belum ada di ketiganya sebelumnya):
- `BackupRestoreSheet.kt`
- `DiagnosticLogSheet.kt`
- `DuplicateFinderSheet.kt`

**Antrean tersisa (3 file, sama alasan boleh lanjut tanpa tanya)**: `SignatureMatcherSheet.kt`,
`SmartPlaylistScreen.kt`, `VaultSheet.kt` — fix identik. Setelah ini SEMUA 7 sheet dari audit
Batch 339 akan tuntas.

**Verifikasi sintaks** — brace seimbang ketiganya (33/33, 16/16, 56/56). Paren: dicek DUA cara
— raw grep (108/107 utk `BackupRestoreSheet.kt`, sisanya cocok) SEMPAT beda krn komentar
multi-baris prosa (bukan kode) yang sudah ada SEJAK Batch 321, bukan diperkenalkan batch ini;
verifikasi ulang dgn strip semua baris `//` dulu baru hitung → kode SUNGGUHAN seimbang penuh
ketiganya (88/88, 68/68, 104/104). Pola false-positive raw-count ini sudah pernah dicatat normal
di histori proyek (Batch 337/338) — bukan tanda korupsi sintaks.

**`liquidGlassAlpha` masih SENGAJA tidak disentuh** (sama alasan Batch 322 — tunggu verifikasi
visual device dulu untuk SEMUA 7 sheet sebelum menurunkan tint balik).

**Ringkasan file** — 3 file kode (batas Micro-Batch). `FILE_MANIFEST.txt` tidak berubah.

**Batch 339 (BUG FIX x2 — tab Cek Update: (a) regresi "tembus pandang" krn `frostedGlass()`
kelewat sejak Batch 322/323, (b) unduhan/APK ke-reset ke nol kalau sheet ke-tap salah/ketutup;
1 file kode)** — User laporan + screenshot: (2) "tab update masih mengalami regresi tembus
pandang", (3) "saat user sudah selesai install update package tapi gak sengaja salah mencet,
malah ke cancel dari awal lagi unduhannya". (Task 1 user — tab onboarding khusus pengganti
banner ijo — DITANYAKAN dulu ke user, scope-nya besar/arsitektural, lihat pesan terpisah.)

**Bug (a) — root cause**: `containerColor = Color.Transparent` (Batch 322/323, syarat Haze)
ternyata TIDAK CUKUP sendirian — itu cuma matikan fill solid default, 0 menggambar blur.
`.frostedGlass()` (`BlurUtils.kt`) itulah yang benar-benar menggambar blur di baliknya —
`UpdateCheckSheet.kt` SATU-SATUNYA sheet (dibanding 12+ call site lain, mis.
`RingtoneCutterSheet.kt`, `SongInfoEditSheet.kt`) yang kelewat modifier ini sejak dibuat.
Transparent TANPA frostedGlass = tembus pandang sungguhan (0 blur, 0 fill) — konten
"Tentang Aplikasi" dari `SettingsScreen.kt` di baliknya kelihatan penuh tanpa filter, persis
screenshot user. **Fix**: `.frostedGlass()` ditambah di posisi identik ke-2 sheet contoh di
atas (setelah `.fillMaxWidth()`, sebelum `.verticalScroll()`).

**🔍 Audit tambahan (TIDAK diperbaiki batch ini, ZERO-REFACTOR — cuma didokumentasikan utk
antrean nanti, BOLEH dikerjakan tanpa tanya ulang, pola identik)**: grep ulang seluruh sheet
`containerColor = Color.Transparent` vs `.frostedGlass()` — **6 sheet LAIN** kena gap SAMA
PERSIS (berpotensi "tembus pandang" sama kalau dibuka): `BackupRestoreSheet.kt`,
`DiagnosticLogSheet.kt`, `DuplicateFinderSheet.kt`, `SignatureMatcherSheet.kt`,
`SmartPlaylistScreen.kt`, `VaultSheet.kt`. Tidak disentuh batch ini (user cuma laporkan tab
Update, Micro-Batch 1 file sudah dipakai bug (b) di bawah) — TAPI kandidat kuat utk sesi
berikutnya kalau user lapor gejala serupa di salah satu sheet itu.

**Bug (b) — root cause**: `DisposableEffect`'s `onDispose { UpdateManager.reset() }`
SEBELUMNYA jalan TANPA SYARAT tiap sheet keluar komposisi (sengaja ditutup ATAU salah
ke-tap/dismiss) — termasuk saat state `Downloading` (thread unduhan TETAP jalan di background,
tidak ikut ke-cancel betulan) atau `ReadyToInstall` (APK SUDAH lengkap di cache). Reset di
momen itu buang progres asli SIA-SIA; `checkForUpdate()` (on-enter) juga jalan ulang dari nol
tiap sheet dibuka lagi. **Fix**: skip `checkForUpdate()`/`reset()` SAMA SEKALI kalau state saat
itu `Downloading`/`ReadyToInstall` — 2 state itu representasi kerja nyata yang tidak boleh
hilang cuma krn sheet ke-tutup. State lain (Idle/Checking/UpToDate/Available/Error) — 0
perubahan perilaku, progres di state itu memang tidak ada yang bisa hilang.

**1 file**: `UpdateCheckSheet.kt` (non-protected). 1 import baru
(`com.rudi.audioplayer.ui.theme.frostedGlass`). Brace/paren seimbang (27/27, 99/99). **Belum
diverifikasi compile Gradle sungguhan** — WAJIB cek CI. **Belum diverifikasi visual di device**
— prioritas cek: (1) buka Cek Update — background sheet harus keliatan blur/frosted, 0 teks
"Tentang Aplikasi" tembus dari belakang; (2) mulai unduh, TUTUP sheet di tengah proses (tap di
luar sheet), buka ulang — progres/`Downloading` harus TETAP lanjut, bukan balik ke Checking;
(3) sampai `ReadyToInstall`, tutup sheet (sengaja/salah tap), buka ulang — harus LANGSUNG
`ReadyToInstall` lagi (tombol "Buka Installer"), BUKAN unduh ulang dari nol. Detail:
`CHANGELOG.md` Batch 339.

**Batch 338 (BUG FIX — scroll TETAP kepicu di layar "normal" (user) selama hint banner
sekali-tampil masih nongol; 3 lever dikombinasi: art box, teks banner, spacer; 1 file kode)** —
User: "untuk ukuran layar saya, seharusnya mode scroll gak kepicu". Klarifikasi: hint banner
MASIH nongol (belum pernah di-dismiss) saat ini terjadi. Batch 337 sudah selesaikan
REACHABILITY (transport kejangkau via scroll) — tapi user maunya lebih jauh: di layar yang dia
anggap NORMAL, scroll idealnya TIDAK PERLU terjadi sama sekali, bukan cuma "berfungsi kalau
terjadi". Root cause: Batch 336 cuma nyusutin art box di layar <640dp; Batch 337 mindahin hint
banner biar bisa discroll — TAPI di layar >=640dp (dianggap "normal"), art box TETAP full 300dp
+ hint banner (~150dp, sebelum dipersingkat) + semua konten lain bisa TETAP total melebihi
viewport SELAMA hint (kondisi sekali-tampil) masih ada, walau device-nya sendiri tidak "pendek".

**Fix (3 lever, semuanya SEMENTARA — cuma aktif selagi `showNowPlayingHint == true`, balik ke
ukuran/spacing penuh biasa begitu di-dismiss PERMANEN)**: (1) art box ikut nyusut ke `260.dp`
saat hint tampil, TIDAK LAGI cuma bergantung `screenHeightDp < 640.dp`; (2) teks
`FeatureHintBanner` dipersingkat (isi 2 tip SAMA — kecerahan/volume + menu ⋮ — dikemas lebih
padat, ~5 baris → ~2 baris); (3) 2 Spacer sekitar hint diciutkan (16dp→8dp, 32dp→20dp khusus
saat hint tampil). Kombinasi 3 lever ini ditargetkan reklaim ~130-140dp tambahan tanpa
permanen mengecilkan tampilan Now Playing di luar masa onboarding sekali-tampil ini.

**1 file**: `NowPlayingScreen.kt` (non-protected). 0 import baru. Brace/paren seimbang
(224/224, 894/894 — jumlah brace turun 1 dari batch sebelumnya krn `if/else` diganti `when`,
angka baru tetap seimbang sendiri, bukan tanda korupsi sintaks). **Belum diverifikasi compile
Gradle sungguhan** — WAJIB cek CI. **Belum diverifikasi visual di device** — prioritas cek:
(1) buka Now Playing PERTAMA KALI (hint tampil) di layar user — pastikan SEKARANG muat tanpa
perlu scroll sama sekali; (2) begitu hint di-dismiss (tombol X), buka lagi Now Playing — art
harus balik full 300dp seperti biasa (0 regresi tampilan permanen); (3) teks hint yang
dipersingkat tetap jelas & tidak keliru makna. Detail: `CHANGELOG.md` Batch 338.

**Batch 337 (BUG FIX — Batch 336 (art box adaptif) TERBUKTI belum cukup, "belum ngefek" di
device user; root cause satu level lebih dalam: FeatureHintBanner ~150dp, 1 file kode)** — User
konfirmasi lewat klarifikasi: opsi "Layar pendek: tombol transport MASIH belum kejangkau walau
discroll habis (Batch 336 belum ngefek)". Sesuai kebijakan Batch 24 (fix resmi sudah diikuti,
gejala identik/berlanjut → curigai akar beda, jangan ulangi variasi kecil), ditelusuri ulang
histori Batch 112 (asal jaring pengaman ini) — catatan aslinya EKSPLISIT sebut `FeatureHintBanner`
(~150dp) sebagai salah satu kontributor UTAMA overflow, setara/lebih besar dari art 300dp,
terutama dikombinasikan 3-button nav. Batch 336 cuma susutkan art; hint banner (juga fixed,
tidak pernah disentuh) tetap jadi bottleneck ruang scroll — itu kenapa "belum ngefek".

**Fix**: `FeatureHintBanner` (0 gesture handling — cuma Card+teks+tombol dismiss, aman dipindah)
dipindah dari fixed header zone jadi child PERTAMA di dalam Column scrollable
(`weight(1f).verticalScroll(...)`, Batch 334/335/336) — sekarang ikut bisa "discroll lewat" utk
menjangkau transport row, bukan lagi permanen menghabiskan jatah fixed zone yang tidak bisa
direbut scroll. Trade-off sengaja: urutan visual hint geser dari SEBELUM art jadi SESUDAH art
(masih di atas judul lagu) — reachability transport (fungsi inti) diprioritaskan di atas posisi
visual hint (onboarding sekali-tampil, dismissable). Box gesture art (Batch 334) & fix overscroll
(Batch 335) & art box adaptif (Batch 336) 0 disentuh, tetap berlaku bersamaan (saling melengkapi,
bukan saling gantikan).

**1 file**: `NowPlayingScreen.kt` (non-protected). 0 import baru. Brace/paren seimbang
(225/225, 880/880). **Belum diverifikasi compile Gradle sungguhan** (0 akses jaringan/SDK di
sandbox) — **WAJIB cek CI setelah push**. **SUDAH diverifikasi visual di device (user
konfirmasi)** — screenshot user (diambil PAS aktif discroll, jari masih narik layar) menunjukkan
5 tombol transport (shuffle/prev/play-pause/next/repeat) semua kejangkau penuh; teks hint banner
yang kelihatan "kepotong" di screenshot itu cuma frame mid-scroll wajar (bagian atas konten
kegeser duluan saat discroll aktif — normal utk scrollable manapun, bukan bug), otomatis utuh
lagi begitu scroll berhenti/scroll=0. Root cause 3-lapis (Batch 335 overscroll glow → Batch 336
art box adaptif → Batch 337 hint banner relokasi) TERBUKTI SELESAI, 0 perlu perubahan kode lagi
utk item ini. Detail: `CHANGELOG.md` Batch 337.

**Batch 336 (BUG FIX — transport row TETAP TIDAK kejangkau via scroll di layar pendek, jaring
pengaman Batch 112/334 regresi nyata, root cause beda level dari Batch 335, 1 file kode)** — User
laporan device: dari 3 opsi klarifikasi (overscroll glow / transport kepotong-tidak kejangkau /
scroll di layar lain), user pilih **"transport masih kepotong/tidak kejangkau di layar pendek"** —
persis item yang Batch 335 tandai "Belum diverifikasi visual di device" (baris "jaring pengaman
Batch 112/334, transport row harus TETAP reachable... behavior itu tidak boleh regresi"), dan
ternyata REGRESI.

**Root cause (BEDA dari Batch 335, ikut kebijakan Batch 24 — kalau fix resmi sudah diikuti tapi
gejala identik/berlanjut, curigai level akar beda, jangan ulangi variasi kecil dari pendekatan
sama)**: Batch 335 cuma matikan overscroll GLOW — 0 sentuh soal RUANG. Header hasil split Batch
334 (Row tombol atas + hint banner opsional + Spacer 12dp + Box gesture art **fixed 300dp, tidak
scrollable**) sebelum Batch 334 semuanya ikut 1 Column scroll tunggal (art bisa ke-scroll
off-screen kalau perlu). Setelah split, art box DIKUNCI fixed (sengaja, supaya gesture
brightness/volume-nya lolos dari nested-scroll conflict — lihat Batch 334) — konsekuensinya:
`Column(weight(1f).verticalScroll(...))` di bawahnya cuma kebagian SISA tinggi layar setelah
header+art (fixed, tidak bisa disusut oleh scroll apa pun). Di layar pendek (landscape/
split-screen/foldable tertutup) total header+art (~300dp+) bisa nyaris menghabiskan seluruh
tinggi layar, sisa ruang buat Column konten kepepet sampai nyaris 0dp — transport row jadi
TIDAK kejangkau walau discroll, meski secara teknis Column-nya scrollable.

**Fix**: `Box` gesture art — tinggi HARDCODE `.height(300.dp)` → `.height(albumArtBoxHeight)`,
dihitung dari `LocalConfiguration.current.screenHeightDp.dp`: layar `>= 640.dp` (mayoritas HP
potret normal) tinggi TETAP 300dp persis (0 perubahan visual, byte-identical ke sebelumnya);
layar `< 640.dp` (pendek) disusutkan proporsional `screenHeightDp * 0.28f`, dibatasi
`coerceIn(160.dp, 300.dp)` (lantai 160dp supaya art tidak jadi terlalu kecil buat dilihat,
plafon 300dp). Struktur/gesture zone TIDAK diubah — Box gesture art TETAP di luar ancestor
scrollable manapun (0 regresi ke fix nested-scroll-conflict Batch 334); `weight(1f).
verticalScroll(..., overscrollEffect = null)` (Batch 335) juga TIDAK disentuh. Efeknya murni:
susutkan porsi fixed → sisa ruang scroll bertambah proporsional → transport row balik kejangkau
di layar pendek, tanpa mengubah tampilan sama sekali di layar normal/tinggi.

**1 file**: `NowPlayingScreen.kt` (non-protected). 1 import baru (`androidx.compose.ui.platform.
LocalConfiguration` — `LocalContext`/`LocalDensity` dari package sama sudah dipakai file ini,
jadi bukan dependency baru). Brace/paren seimbang (225/225, 865/865 — diverifikasi otomatis).
**Belum diverifikasi compile Gradle sungguhan** (0 akses jaringan/SDK di sandbox sesi ini) —
**WAJIB cek CI setelah push**. **Belum diverifikasi visual di device** — prioritas cek: (1) layar
pendek asli (landscape, atau split-screen Termux+app) — buka Now Playing, scroll area
judul-transport sampai habis, tombol play/pause/next/prev HARUS kejangkau penuh; (2) layar
normal/potret biasa — pastikan ukuran album art TIDAK berubah sama sekali dibanding sebelumnya
(regresi visual = bug baru). Detail: `CHANGELOG.md` Batch 336.

**Batch 335 (BUG FIX — overscroll stretch-glow kepicu di Column judul-transport meski konten
muat, regresi dari Batch 334, 1 file kode)** — User laporan device (format T/J): \"bagian bawah
(judul-tombol transport) yang masih bisa discroll\" — \"Scroll-nya kepicu padahal konten harusnya
muat (bug baru)\". Root cause BEDA dari bug Batch 334 (itu soal 2 pointer-drag recognizer axis
sama bentrok; ini soal Column `weight(1f).verticalScroll(...)` hasil split Batch 334 SEKARANG
punya fixed-height dari sisa ruang layar, bukan lagi unbounded seperti Column tunggal lama
sebelum Batch 334) — begitu tinggi layar cukup, konten (judul s/d tombol transport) genuinely
muat penuh (`ScrollState.maxValue` = 0), TAPI overscroll stretch-glow bawaan Android 12+/Compose
Foundation tetap terpicu visual tiap kali disentuh-drag, terlepas dari apakah posisi scroll
benar-benar berpindah atau tidak (rubber-band kosong) — user membacanya sebagai \"masih bisa
discroll\".

**Fix**: `verticalScroll(rememberScrollState())` → `verticalScroll(state = rememberScrollState(),
overscrollEffect = null)` — overload resmi `Modifier.verticalScroll()` yang menerima parameter
`OverscrollEffect?` langsung (matikan overscroll KHUSUS Column ini, 0 titik lain app tersentuh).
**Sengaja BUKAN pola lama** `CompositionLocalProvider(LocalOverscrollConfiguration provides
null)` (dipakai `SmartPlaylistScreen.kt` Batch 263 saat BOM project masih 2024.05.00) — dicek
ulang `web_search` ke dokumentasi resmi Compose Foundation sesi ini: `LocalOverscrollConfiguration`/
`OverscrollConfiguration` **SUDAH DEPRECATED** (diganti `LocalOverscrollFactory`/
`rememberPlatformOverscrollFactory`), persis risiko yang sudah ditandai eksplisit di catatan
Batch 291 soal lompatan BOM 2024.05.00→2026.04.01 (\"`LocalOverscrollConfiguration` tersangka
pertama kalau CI gagal... belum diperbaiki preventif\") — overload `overscrollEffect` di
`verticalScroll()` sendiri sudah tersedia jauh di bawah BOM 2026.04.01 project ini, jadi dipakai
langsung sesuai kebijakan prioritas mutakhir (aturan sesi #3) alih-alih menambah 1 lagi titik
pakai API yang sudah diketahui usang. `SmartPlaylistScreen.kt` (pemakai lama pola deprecated itu)
**SENGAJA TIDAK disentuh** batch ini — di luar laporan bug, ZERO-REFACTOR (kandidat modernisasi
terpisah kalau user minta nanti).

**Cakupan fix**: HANYA Column scrollable hasil split Batch 334 (judul s/d tombol transport). Box
gesture brightness/volume (Batch 334, tidak lagi scrollable) & header (tombol atas/hint/art) TIDAK
disentuh — 0 relevansi ke bug ini. Getaran/animasi scroll asli (kalau konten genuinely overflow di
layar pendek, jaring pengaman Batch 112/334) TETAP jalan penuh via `scrollState` yang sama — cuma
efek visual overscroll DI LUAR rentang scroll asli yang dimatikan, 0 logic gesture/scroll lain
diubah.

**1 file**: `NowPlayingScreen.kt` (non-protected). Brace/paren seimbang (223/223, 854/854 — parens
naik dari 840→854 murni dari blok komentar baru yang ditambahkan, bukan dari kode; diverifikasi
saldo tetap sama di kedua sisi). `FILE_MANIFEST.txt` tidak berubah (188/188), 0 import baru
(`verticalScroll` sudah diimpor, overload beda cuma butuh parameter tambahan — `OverscrollEffect`
sendiri tidak perlu diimpor eksplisit krn cuma dioper literal `null`). **Belum diverifikasi compile
Gradle sungguhan** (0 akses jaringan/SDK di sandbox sesi ini) — **WAJIB cek CI setelah push**,
risiko sintaks rendah (overload resmi Compose Foundation, dikonfirmasi `web_search` ke dokumentasi
resmi, bukan tebakan) TAPI ini kali pertama app ini memakai parameter `overscrollEffect` langsung
(pola berbeda dari `LocalOverscrollConfiguration` lama), jadi tetap wajib dikonfirmasi CI bukan
diasumsikan aman. **Belum diverifikasi visual di device** — prioritas cek: buka Now Playing di
layar yang cukup tinggi (konten judul-transport harusnya muat penuh tanpa scroll), coba drag di
area judul/rating/seekbar/transport — TIDAK boleh lagi ada efek stretch/glow/pergeseran apa pun;
di layar PENDEK yang genuinely butuh scroll (jaring pengaman Batch 112/334), transport row harus
TETAP reachable via scroll seperti sebelumnya (behavior itu tidak boleh regresi). Detail:
`CHANGELOG.md` Batch 335.

**Batch 334 (BUG FIX — gesture brightness/volume bentrok dengan verticalScroll, 1 file kode)** —
User laporan+screenshot: swipe kecerahan/volume "bentrokan langsung". Root cause: Column induk
py `verticalScroll()` (Batch 112) membungkus Box gesture (`detectVerticalDragGestures`, drag
vertikal) — 2 recognizer sumbu sama bersarang bentrok, `change.consume()` tidak cukup krn
ancestor `scrollable()` bisa menang arbitrase drag-start duluan.

**Fix struktural**: Column induk (header: tombol atas+hint+Box art/gesture) TIDAK LAGI
scrollable — dipisah jadi Column baru (`weight(1f).verticalScroll(...)`) yang HANYA bungkus
konten SETELAH art (judul s/d transport). Pola "fixed header + scrollable body" standar,
dipilih drpd hack pointer-arbitration level-rendah (lebih riskan tanpa bisa dicompile-test).
Jaring pengaman Batch 112 (transport row layar pendek) TETAP UTUH, cuma scope diperbaiki. 0
logic gesture brightness/volume diubah.

**1 file**: `NowPlayingScreen.kt` (non-protected). Brace/paren seimbang (223/223, 840/840).
`FILE_MANIFEST.txt` tidak berubah (188/188). **Belum diverifikasi visual di device** — cek:
swipe kecerahan/volume mulus, transport row masih reachable via scroll di layar pendek, 0
scroll-bleed saat swipe piringan, header tetap diam. Detail: `CHANGELOG.md` Batch 334.

**Batch 333 (Pending Queue item 2 — feedback tekan tombol yang belum `bouncyPress`, 1 file)** —
Audit menyeluruh `IconButton`/`FilledIconButton` (`NowPlayingScreen.kt` 9 titik +
`MiniPlayerBar.kt` 1 titik): transport row 100% SUDAH `bouncyPress` (dugaan Batch 332 benar).
Ditemukan 3 titik LAIN di layar yang sama 0 feedback tekan: tombol tutup (`onBack`), tombol
"⋮ Kontrol Lanjutan", 5 bintang rating (interaction source sendiri-sendiri per bintang). Fix:
`bouncyPress()` ditambah ke ketiganya — tombol tutup/"⋮" pakai default 0.88f (sama transport),
bintang pakai 0.75f (reuse angka favorite-icon di file sama). Sengaja TIDAK sentuh baris list
`AdvancedControlRow` (row biasa, ripple standar, `bouncyPress` konsisten cuma dipakai utk
icon-button lepas di app ini).

**1 file**: `NowPlayingScreen.kt` (non-protected). Brace/paren seimbang (222/222, 827/827).
`FILE_MANIFEST.txt` tidak berubah (188/188). Docs disinkronkan: README.md, CHANGELOG.md.

**Pending Queue: KOSONG** — kedua item Batch 330 selesai (Batch 332+333). Sesi berikutnya
kembali ke `ROADMAP_LIQUID_GLASS_REDESIGN.md` kalau 0 instruksi/temuan baru user (aturan sesi
#4). Detail: `CHANGELOG.md` Batch 333.

**Batch 332 (Micro-interaction icon morph Play/Pause, Pending Queue item 1, 2 file kode)** —
User: "lanjut", melanjutkan item #1 Pending Queue Batch 331.

**Konteks**: `AnimatedContent(targetState = uiState.isPlaying)` di `NowPlayingScreen.kt` &
`MiniPlayerBar.kt` (sejak Batch 224/226) 0 `transitionSpec` eksplisit — jatuh ke default
Compose (crossfade polos), bukan morph. **Implementasi (identik 2 file)**: `transitionSpec`
ditambah — masuk `scaleIn(0.6x)+fadeIn` togetherWith keluar `scaleOut(0.6x)+fadeOut`. Durasi
REUSE PERSIS pola asimetris Batch 330 (200ms masuk/150ms keluar, NavHost tab transition) —
bukan angka baru. `togetherWith` (bukan `with` deprecated). Offset bias-optik PlayArrow
(Batch 226) tidak disentuh, tetap jalan bersamaan.

**2 file**: `NowPlayingScreen.kt` + `MiniPlayerBar.kt` (non-protected keduanya). Brace/paren
seimbang (219/219+821/821, 13/13+108/108). `FILE_MANIFEST.txt` tidak berubah (188/188). Docs
disinkronkan: README.md (banner), CHANGELOG.md.

**Pending Queue (sisa 1 item, belum dikerjakan, tunggu instruksi user)**: Feedback tekan tombol
kontrol pemutaran (scale-down saat pressed) — **catatan penting**: transport Play/Pause/Skip di
2 file ini SUDAH pakai `.bouncyPress()` sejak lama (6 titik `NowPlayingScreen.kt`, grep-
confirmed) — item ini kemungkinan besar cuma relevan utk kontrol LAIN yang belum py
`bouncyPress()` (slider seek, tombol sheet lain). **WAJIB audit titik mana yang genuinely
belum punya feedback tekan dulu sebelum eksekusi** — jangan asumsikan "tombol kontrol
pemutaran" = transport row yang sudah lama beres. Detail: `CHANGELOG.md` Batch 332.

**Batch 331 (Transisi push horizontal untuk `stats_dashboard`, 1 file kode)** — User: "lanjut",
melanjutkan item #1 Pending Queue Batch 330 (pra-dicatat, tidak perlu tanya ulang — pola sama
antrean eksplisit Batch 322-324 dst).

**Konteks**: `stats_dashboard` (drill-down dari Pengaturan → Statistik) sebelumnya ikut default
fade generik `NavHost` (Batch 330) yang sama seperti tab lateral home/library/settings, padahal
navigasinya hierarkis (push, bukan swap lateral) — kandidat upgrade sudah dicatat eksplisit di
Pending Queue Batch 330.

**Implementasi**: `composable("stats_dashboard")` diberi `enterTransition` (slide dari kanan +
fade, `tween(300)`) & `popExitTransition` (slide balik ke kanan + fade, `tween(300)`) sendiri —
pola gerak iOS-push. `tween(300)` REUSE persis dari `popExitTransition` rute "now_playing" di
file yang sama, bukan angka baru. 2 import baru: `slideInHorizontally`, `slideOutHorizontally`.

**Koreksi mid-implementasi (self-caught sebelum dikirim)**: draft awal sempat menambah 4 field
(enter/exit/popEnter/popExit), tapi diverifikasi ulang lewat dokumentasi resmi Navigation-Compose
(`web_search`, bukan tebakan): `exitTransition`/`popEnterTransition` sebuah destination hanya
dievaluasi kalau destination itu jadi *initialState* forward-nav / *targetState* pop — kondisi
yang TIDAK PERNAH terjadi untuk rute leaf ini (0 rute lain `navigate()` forward dari sini, 0 rute
pop kembali ke sini, diverifikasi grep `navController.navigate(` app-wide). 2 field itu dibuang
sebelum dikirim (dead code kalau dipertahankan) — sisa `enterTransition` (aktif: destination ini
jadi target forward-nav) & `popExitTransition` (aktif: destination ini jadi initial saat di-pop).
Sisi "settings" (initial saat forward-nav ke sini, target saat pop balik ke situ) pakai default
`NavHost` Batch 330 apa adanya (fadeOut 150 masuk / fadeIn 200 balik) — tidak perlu override
tambahan, sudah cukup untuk sisi dia.

**1 file**: `MainActivity.kt` (**Protected, edit parsial** — `composable("stats_dashboard")`
diubah ke bentuk dengan parameter transisi (route jadi named-parameter, bukan positional-string),
2 import baru ditambah dekat import `slideInVertically`/`slideOutVertically` yang sudah ada, 0
baris composable lain disentuh). Brace/paren dicek seimbang (264/264 `{}`, 655/655 `()`).

**Ringkasan file** — 1 file kode (jauh di bawah batas Micro-Batch). `FILE_MANIFEST.txt` tidak
berubah (188/188). Docs disinkronkan: README.md (banner), CHANGELOG.md.

**Pending Queue (sisa 2 item dari Batch 330, belum dikerjakan, tunggu instruksi user)**: (1)
Micro-interaction tombol Play/Pause (icon morph play↔pause). (2) Feedback tekan tombol kontrol
pemutaran (scale-down halus saat pressed).

**Batch 330 (Default crossfade transisi tab navigasi bawah — Beranda/Perpustakaan/Pengaturan, 1
file kode)** — User: prioritas animasi/transisi pertama = "yang paling berdampak ke user
langsung"; gaya "smooth kayak iOS" = fade/slide halus, ringan & minim risiko (dijawab lewat 2
pertanyaan klarifikasi sesi ini).

**Audit sebelum eksekusi**: grep app-wide `navController.navigate(` (`MainActivity.kt`)
konfirmasi graf rute cuma 5 total — `home`/`library`/`settings`/`stats_dashboard`/`now_playing`.
Hanya rute "now_playing" yang punya `enterTransition`/`exitTransition`/`popExitTransition` sendiri
(slide+fade, sudah lama ada). 4 rute lain 0 transisi — cut instan bawaan Compose Navigation. Tab
bawah (`NavigationBarItem` onClick → `navController.navigate("home"/"library"/"settings"){
popUpTo(...saveState=true); launchSingleTop=true; restoreState=true}`, pola resmi bottom-nav
Batch 301) adalah interaksi tersering per sesi user — dipilih sebagai lever #1 "paling berdampak
langsung".

**Keputusan**: `enterTransition`/`exitTransition`/`popEnterTransition`/`popExitTransition`
ditambah di level parameter `NavHost(...)` (bukan diduplikasi ke tiap `composable()`) — otomatis
jadi default untuk semua rute yang belum override sendiri, 0 duplikasi kode. `fadeIn(tween(200))`
masuk / `fadeOut(tween(150))` keluar, simetris maju-mundur (tab switch bukan hierarki searah).
Kedua angka REUSE persis dari yang sudah ada di file yang sama (`tween(200)` = exitTransition
rute "now_playing" yang sudah ada; `tween(150)` = fadeIn di `NowPlayingScreen.kt`) — bukan angka
tebakan baru, konsisten kebiasaan project ini reuse angka tervalidasi. 0 import baru (`fadeIn`/
`fadeOut`/`tween` sudah dipakai file ini sejak rute "now_playing" ditambah).

**Rute "now_playing" TIDAK terdampak** — override eksplisitnya sendiri (enter/exit/popExit) selalu
menang atas default `NavHost` baru ini, tidak berubah sama sekali. `popEnterTransition` baru ini
secara teknis inert khusus untuk "now_playing" (grep app-wide konfirmasi: 0 rute pernah
`navigate()` balik ke "now_playing" — dia leaf, jadi tidak ada skenario pop-masuk yang memicu
parameter itu untuk rute tsb).

**1 file**: `MainActivity.kt` (**Protected, edit parsial** — 4 parameter baru ditambah persis ke
blok `NavHost(...)` yang sudah ada, 0 baris composable lain disentuh). Brace/paren dicek seimbang
(260/260 `{}`, 633/633 `()`).

**Ringkasan file** — 1 file kode (jauh di bawah batas Micro-Batch). `FILE_MANIFEST.txt` tidak
berubah (188/188). Docs disinkronkan: README.md (banner), CHANGELOG.md.

**Pending Queue (kandidat animasi berikutnya, belum dikerjakan, tunggu instruksi user)**: (1)
transisi push `stats_dashboard` saat ini ikut default fade generik yang sama seperti tab lateral,
padahal navigasi ini hierarkis (drill-down dari Pengaturan) — kandidat upgrade ke slide-horizontal
ala iOS push. (2) Micro-interaction tombol Play/Pause (icon morph play↔pause). (3) Feedback tekan
tombol kontrol pemutaran (scale-down halus saat pressed). Ketiga kandidat ini TIDAK dikerjakan
batch ini — cuma dicatat sebagai next-candidate, konsisten Micro-Batch (1 batch = 1 task).

**Batch 329 (Matikan blur asli Liquid Glass PERMANEN app-wide, 2 file kode)** — User pilih opsi
paling aman dari 2 opsi yang ditawarkan, setelah root cause stutter/lag Batch 328 ditelusuri
lebih dalam.

**Root cause**: blur asli (`hazeEffect`) baru genuinely aktif di 17/17 `ModalBottomSheet` sejak
Batch 324 (sebelumnya no-op cross-window, root cause Batch 311) — sheet "Kontrol Lanjutan" +
`MiniPlayerBar` (SELALU tervisible & terus resample tiap frame selama musik main, capture-nya
lewat `hazeSource` di window yang sama) adalah persis biaya GPU per-frame yang sudah
diperingatkan sejak param `blurRadius` pertama ditambah (komentar Batch 298/300: "blur asli Haze
resample tiap frame"). Beda dari Batch 328 (yang cuma revert animasi Aurora di atas kaca) — batch
ini mematikan MEKANISME blur asli itu sendiri.

**Keputusan (sesuai `STABILITY > Speed`)**: `hazeEffect` dihapus dari cabang Liquid Glass
(`BlurUtils.kt`) — `glassBase` sekarang selalu `this`, identik ke-4 identitas lain (0 blur asli).
`hazeSource` (`MainActivity.kt`) juga dilepas — kalau capture backdrop dibiarkan terpasang tanpa
1 consumer pun, tetap bayar sebagian besar biaya GPU yang justru ingin dihilangkan. Tint
(`liquidGlassAlpha`) dinaikkan balik ke fallback opaque 0.85f (gelap) / 0.90f (terang) — BUKAN
angka baru, reuse persis fallback "0 blur terlihat, tint sendiri wajib jaga keterbacaan" yang
sudah pernah tervalidasi Batch 311-324, kini status permanen (bukan darurat sementara).
`hazeState`/`LocalHazeState`/`CompositionLocalProvider` (Theme.kt, MainActivity.kt) SENGAJA TIDAK
dibongkar — direuse persis state Batch 295 (murni plumbing, 0 consumer, 0 perubahan visual),
menghindari risiko membongkar `CompositionLocalProvider` yang membungkus ratusan baris Scaffold
(Batch 295's komentar sendiri: "badan blok TIDAK di-reindent, minim-diff"). Parameter
`blurRadius` (fungsi `frostedGlass()`) balik ke status "kept for source compatibility, unused"
persis pra-Batch-296 — signature publik tidak diubah.

**2 file**: `BlurUtils.kt` (hapus import+call `hazeEffect`, `glassBase` selalu `this`,
`liquidGlassAlpha` 0.38f/0.48f → 0.85f/0.90f), `MainActivity.kt` (**Protected, edit parsial** —
hapus `.then(...)`/`Modifier.hazeSource(state = hazeState)` di Box NavHost, hapus import
`hazeSource` tak terpakai). Brace/paren dicek seimbang keduanya (BlurUtils.kt 5/5 `{}` 159/159
`()`; MainActivity.kt 256/256 `{}` 620/620 `()`).

**Ringkasan file** — 2 file kode (di bawah batas Micro-Batch). `FILE_MANIFEST.txt` tidak berubah
(188/188). Docs disinkronkan: README.md (banner + § "Rencana v2" Liquid Glass), CHANGELOG.md.

**Batch 328 (REVERT Aurora rim-glow animation — regresi performa dikonfirmasi user device
sungguhan, 3 file kode)** — User: "lakukan perbaikan akhir sebelum masuk fase discontinued", scope
dikonfirmasi eksplisit: "musik stuttering/mandek saat diputar, lagging & nge glitch saat swipe
kontrol lanjutan".

**Root cause**: asumsi Batch 326 ("1 `rememberInfiniteTransition` dibagi via `LocalAuroraPhase` =
aman, kekhawatiran performa Batch 310 tertangani") TERBUKTI KELIRU di device sungguhan. Berbagi 1
instance transition memang mengurangi JUMLAH transition (12+ call site → 1), TAPI TIDAK
menghilangkan bahwa phase berubah tiap frame (~16ms) tetap memicu recomposition brush di SEMUA
consumer `frostedGlass()` sekaligus — termasuk `MiniPlayerBar` (SELALU tervisible selama musik
main, bersaing langsung dgn thread audio/UI pas playback aktif) dan sheet "Kontrol Lanjutan" yang
juga baca `frostedGlass()` sambil menangani gesture swipe. Analisis "1 instance = performa aman"
Batch 326 keliru menyamakan "jumlah objek timer" dengan "jumlah recomposition" — 2 hal berbeda.

**Keputusan (sesuai `STABILITY > Speed`)**: TIDAK ditambal/dioptimasi lebih jauh (mis.
`derivedStateOf`, throttle update, scope animasi ke kondisi tertentu) — DIREVERT PENUH ke statis,
konsisten rasionalisasi ASLI Batch 310 yang sempat (keliru) dianggap sudah teratasi Batch 326.
Alasan tidak coba optimasi lanjutan: proyek akan masuk fase discontinued — lebih aman berhenti di
state statis yang sudah terbukti stabil (Batch 306-310, 325) daripada wariskan animasi belum
matang tanpa sesi lanjutan utk iterasi.

**3 file**: `Theme.kt` (`LocalAuroraPhase` DIHAPUS, bukan ditinggal dead code — cegah re-enable
ceroboh tanpa re-baca rasionalisasi ini), `MainActivity.kt` (**Protected, edit parsial** — blok
`auroraPhaseTransition`/`auroraPhase` DIHAPUS, `CompositionLocalProvider` balik ke
`LocalHazeState` saja, 5 import animasi tak terpakai dibuang), `BlurUtils.kt` (Aurora branch balik
ke `Brush.linearGradient` statis, import `lerp` tak terpakai dibuang). **Alpha Batch 327
(`AuroraRimGlowAlpha` 0.44f + taper 0.85x/0.65x/0.46x) TETAP dipertahankan** — itu bukan penyebab
regresi (keluhan alpha & keluhan stutter adalah 2 laporan device terpisah), rasionalisasinya sudah
benar. Brace/paren dicek seimbang ketiganya (Theme.kt 14/14 `{}` 206/206 `()`; MainActivity.kt
256/256 `{}` 623/623 `()`; BlurUtils.kt 9/9 `{}` 153/153 `()`).

**Ringkasan file** — 3 file kode (pas batas Micro-Batch). `FILE_MANIFEST.txt` tidak berubah
(188/188). Docs disinkronkan.

**Batch 327 (Naikkan alpha rim-glow Aurora — token baru `AuroraRimGlowAlpha`, 2 file kode)** —
User dikonfirmasi lewat `ask_user_input_v0`: keluhan "terlalu tipis, hampir tak kasat mata"
scope-nya rim-glow per-panel Batch 326, BUKAN ambient wash `auroraGlow()` (0 dikeluhkan, TIDAK
disentuh). `AuroraRimGlowAlpha = 0.44f` (`Color.kt`, token BARU terpisah dari `AuroraGlowAlpha`
0.34f yang dipakai wash — supaya naikkan rim tidak ikut menaikkan wash) — puncaknya disamakan ke
level "accent-glow biasa ~0.42-0.45f" yang sudah didokumentasikan (komentar emerald streak Skeu
Batch 80, MainActivity.kt), bukan tebakan baru. Multiplier taper per-stop (`BlurUtils.kt`,
Aurora branch `frostedGlass()`) juga dinaikkan 0.85x/0.6x/0.35x → 0.85x/0.65x/0.46x — floor stop
ke-4 naik dari alpha efektif 0.119 → 0.202 (~70% lebih terang di titik paling redup), taper
tetap dipertahankan (masih memudar, bukan flat). Brace/paren dicek seimbang (Color.kt 0/0 `{}`
251/251 `()`; BlurUtils.kt 10/10 `{}` 157/157 `()`).

**Ringkasan file** — 2 file kode (di bawah batas Micro-Batch). `FILE_MANIFEST.txt` tidak berubah
(188/188). Docs disinkronkan.

**Batch 326 (Aurora rim-glow statis → animated via `LocalAuroraPhase`, 3 file kode)** — User:
"next: Aurora statis -> bergerak!!". Ini KANDIDAT yang sudah dicatat eksplisit sejak komentar
Batch 310 (`frostedGlass()`, BlurUtils.kt): "SENGAJA statis ... kandidat animasi kalau user minta
lanjut nanti setelah statis ini terverifikasi visual dulu" — precondition itu TERPENUHI (blur+rim
statis dikonfirmasi user Batch 325).

Kekhawatiran performa asli Batch 310 (12+ call site `frostedGlass()` × `rememberInfiniteTransition`
independen = biaya baru belum diverifikasi) DITANGANI via arsitektur, bukan diabaikan: 1 phase
float dihitung SEKALI di `AppNavHost` (`MainActivity.kt`, pola identik `hazeState`/`LocalHazeState`
Batch 295) via `rememberInfiniteTransition`, dibagi ke semua panel lewat `LocalAuroraPhase`
(`Theme.kt`, CompositionLocal baru) — 0 transition tambahan per panel, tetap 1 total (terpisah
dari transition internal `auroraGlow()`'s ambient wash di root Surface — itu TIDAK disentuh batch
ini, 0 risiko ke situ/`TactileDepth.kt`). Resep durasi/easing/RepeatMode (20 detik/arah,
LinearEasing, Reverse) disalin persis dari `auroraGlow()`, bukan angka baru. Mekanisme warna:
`lerp()` antar-hue identik `auroraGlow()`, stop ke-4 di-lerp balik ke `AuroraGreen` (bukan diam di
Magenta) supaya rim terasa mengalir memutar penuh.

**3 file**: `Theme.kt` (+`LocalAuroraPhase`), `MainActivity.kt` (**Protected, edit parsial** —
+phase computation di `AppNavHost` dekat `hazeState`, +provide di `CompositionLocalProvider` yang
sudah ada; root Surface's `auroraGlow()` call site BARIS 400-an TIDAK disentuh), `BlurUtils.kt`
(Aurora branch `frostedGlass()` baca `LocalAuroraPhase.current`, ganti brush statis → animated).
Brace/paren dicek seimbang ketiganya (Theme.kt 15/15 `{}` 206/206 `()`; MainActivity.kt 256/256
`{}` 632/632 `()`; BlurUtils.kt 10/10 `{}` 148/148 `()`).

**Ringkasan file** — 3 file kode (pas batas Micro-Batch). `FILE_MANIFEST.txt` tidak berubah
(188/188). Docs disinkronkan: README.md (banner + bullet fitur Aurora, "Fase 6/N selesai"),
CHANGELOG.md.

**Batch 325 (Turunkan `liquidGlassAlpha` balik ke nilai tuning device terakhir yang sah, 1 file
kode)** — User dikonfirmasi lewat `ask_user_input_v0`: "Sudah dicoba, blur OK — lanjut turunkan
tint alpha" (verifikasi visual device sub-langkah 5/5 `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §5,
setelah ke-7 gap `containerColor` tuntas Batch 322-324). `liquidGlassAlpha` (`BlurUtils.kt`)
diturunkan 0.85f/0.90f (fallback aman darurat Batch 311) → **0.38f/0.48f** — BUKAN angka baru,
reuse murni nilai tuning Batch 299 yang sudah pernah lolos 1 putaran device dulu, sebelum
dinaikkan darurat karena bug tak-terkait (blur 0% cross-window, akar masalahnya `containerColor`,
bukan tint). `blurRadius` (32dp) & gap dark/light (0.10) TIDAK disentuh (bukan lever yang
relevan). Brace/paren dicek seimbang (9/9 `{}`, 136/136 `()`).

**⏳ PERFORMA (GPU/lag saat MiniPlayerBar re-render) BELUM eksplisit dikonfirmasi user** — satu-
satunya item terbuka tersisa di seluruh `ROADMAP_LIQUID_GLASS_REDESIGN.md`/
`LIQUID_GLASS_BLUR_ENGINE_DESIGN.md`. Jangan diasumsikan lolos cuma karena visual sudah OK.

**Ringkasan file** — 1 file kode (di bawah batas Micro-Batch). `FILE_MANIFEST.txt` tidak berubah
(188/188).

**Batch 324 (Tuntaskan antrean Batch 322/323: fix `containerColor` di `VaultSheet.kt`, 1 file
kode)** — User: "next", melanjutkan antrean eksplisit Batch 323 ("BOLEH dikerjakan tanpa tanya
ulang — pola identik, bukan keputusan arsitektur baru"). Fix identik diterapkan ke
`VaultSheet.kt`: `+containerColor = Color.Transparent` pada `ModalBottomSheet` + import
`androidx.compose.ui.graphics.Color` (belum ada sebelumnya). Brace/paren dicek seimbang
(101/101 `{}`, 210/210 `()`).

**Ke-7 gap `containerColor` yang ditemukan grep Batch 322 TUNTAS semua** — diverifikasi ulang
app-wide pakai grep multi-baris (single-line grep sempat salah nunjuk 13/17 karena beberapa
call site nulis parameter di baris lanjutan): **17/17 call site `ModalBottomSheet` app-wide
sudah konsisten pasang `containerColor = Color.Transparent`**.

**`liquidGlassAlpha` masih SENGAJA tidak disentuh** (sama alasan Batch 322/323 — tunggu
verifikasi visual device dulu, lihat PROJECT_STATE.md Batch 322 utk rasionalisasi penuh).

**Ringkasan file** — 1 file kode (di bawah batas Micro-Batch). `FILE_MANIFEST.txt` tidak berubah
(188/188).

**Batch 323 (Lanjutan antrean Batch 322: fix `containerColor` 3 dari 4 sheet tersisa, 3 file
kode)** — User: "next", melanjutkan antrean eksplisit Batch 322 ("BOLEH dikerjakan tanpa tanya
ulang — pola identik, bukan keputusan arsitektur baru"). Fix identik Batch 322 (`+containerColor
= Color.Transparent`) diterapkan ke:
- `SignatureMatcherSheet.kt` — + import `Color` (belum ada sebelumnya).
- `SmartPlaylistScreen.kt` — `Color` sudah diimpor, cuma tambah parameter.
- `UpdateCheckSheet.kt` — `Color` sudah diimpor, cuma tambah parameter.
Brace/paren dicek seimbang ketiganya (52/52+126/126, 104/104+259/259, 25/25+74/74).

**Antrean tersisa (1 file, sama alasan boleh lanjut tanpa tanya)**: `VaultSheet.kt` — fix 1-baris
identik + import `Color` (belum ada). Setelah ini, ke-7 gap `containerColor` yang ditemukan grep
Batch 322 akan TUNTAS semua (17/17 call site `ModalBottomSheet` app-wide konsisten).

**`liquidGlassAlpha` masih SENGAJA tidak disentuh** (sama alasan Batch 322 — tunggu verifikasi
visual device dulu, lihat PROJECT_STATE.md Batch 322 utk rasionalisasi penuh).

**Ringkasan file** — 3 file kode (batas Micro-Batch). `FILE_MANIFEST.txt` tidak berubah (188/188).

**Batch 322 (Fix blur lintas-window Liquid Glass — investigasi ulang root cause Batch 311,
3 file kode + `MainActivity.kt` DIPERIKSA tapi TIDAK diubah, dikonfirmasi eksplisit user)** —
User pilih lewat `ask_user_input_v0`: "Fix blur lintas-window (sentuh MainActivity.kt, protected)".

**Riset ulang root cause (WAJIB sebelum sentuh protected file)** — Batch 311 mengklaim "capture
`RenderNode` Haze tidak bisa sample lintas-window sama sekali" sebagai akar masalah. Diverifikasi
ulang batch ini lewat dokumentasi RESMI `chrisbanes/haze` (sample `BottomSheet.kt` & `DialogSample.kt`
upstream, di-fetch langsung dari GitHub) — klaim itu TIDAK akurat: sample resmi library ini
MEMBUKTIKAN `hazeEffect` di dalam `ModalBottomSheet`/`Dialog` sungguhan (window Android terpisah)
BISA sample dari `hazeSource` yang berada di window Activity yang berbeda, asalkan
`containerColor = Color.Transparent` (ModalBottomSheet) dipasang & `hazeState` yang sama dipakai
di kedua sisi. Batch 311 benar soal GEJALA (blur 0% kelihatan di screenshot "Kontrol Lanjutan"),
tapi teori PENYEBAB-nya keliru — sekadar tebakan dari 1 screenshot, bukan bukti Android/Compose
langsung.

**Audit `MainActivity.kt` (protected, diperiksa detail baris-per-baris)** — dibandingkan struktur
wiring `hazeState`/`hazeSource`/`CompositionLocalProvider(LocalHazeState)` di file ini terhadap
pola resmi Haze di atas: SESUAI. `hazeSource` benar terpasang di `Box` pembungkus `NavHost` (baris
~1069-1082, Batch 296), `CompositionLocalProvider(LocalHazeState provides hazeState)` membungkus
seluruh `Scaffold` termasuk `NavHost` (baris 867-1228, Batch 295) — dicek eksplisit bahwa KEDUA
titik pemanggilan `nowPlayingContent { ... }` (baris 1205 & 1223, termasuk sheet "Kontrol Lanjutan"
yang dilaporkan Batch 311) ADA di dalam scope provider ini, bukan di luar seperti sempat dicurigai.
**Kesimpulan: 0 bug ditemukan di `MainActivity.kt` — TIDAK diubah batch ini.** Mengubah file
protected ini tanpa bug yang terverifikasi akan melanggar ZERO-REFACTOR & berisiko ke 12+ call
site `frostedGlass()` lain yang sudah benar (MiniPlayerBar, card Home/Library) — bukan menjalankan
otorisasi user secara serampangan.

**Bug nyata yang ditemukan & diperbaiki (3 file, batas Micro-Batch)** — grep ULANG seluruh 17 call
site `ModalBottomSheet(` app ini: 7 file TIDAK pernah pasang `containerColor = Color.Transparent`
(syarat WAJIB di sample resmi Haze), berbeda dari 10 file lain yang sudah benar sejak awal.
Sheet "Kontrol Lanjutan" (`NowPlayingScreen.kt`, sheet yang dilaporkan Batch 311) TERNYATA SUDAH
benar (`containerColor` sudah Transparent sejak dibuat) — jadi gap ini BUKAN penyebab tunggal bug
yang dilaporkan, tapi tetap bug nyata & terpisah yang wajib diperbaiki di 7 file lain. Micro-Batch
membatasi 3 file kode/batch (pola sama Batch 315/316 memecah antrean `verticalScroll` 5-file jadi
2 batch) — 3 diperbaiki batch ini:
- `BackupRestoreSheet.kt` — `+containerColor = Color.Transparent` + import `Color`.
- `DiagnosticLogSheet.kt` — sama.
- `DuplicateFinderSheet.kt` — sama.
Brace/paren dicek seimbang ketiganya (33/33+101/101, 16/16+69/69, 56/56+104/104).

**Antrean Batch berikutnya (sama, BOLEH dikerjakan tanpa tanya ulang — pola identik, bukan
keputusan arsitektur baru)**: `SignatureMatcherSheet.kt`, `SmartPlaylistScreen.kt`,
`UpdateCheckSheet.kt`, `VaultSheet.kt` — fix 1-baris identik (`+containerColor = Color.Transparent`
+ import `Color` kalau belum ada).

**`liquidGlassAlpha` (`BlurUtils.kt`) SENGAJA TIDAK disentuh** — tetap 0.85f/0.90f (fallback aman
Batch 311). Menurunkan balik ke 0.38f/0.48f sebelum user verifikasi visual device akan berisiko:
kalau fix `containerColor` ini TERNYATA tidak menuntaskan seluruh gejala (mis. ada faktor lain yang
tidak kelihatan dari static analysis — device sungguhan adalah bukti akhir, bukan grep), app akan
regresi balik ke bug asli "ghost text tembus". Tint hanya boleh diturunkan lagi SETELAH user
konfirmasi blur sungguhan sudah tampil benar di device pada sheet yang sudah diperbaiki.

**Verifikasi visual device (WAJIB, tidak bisa disimulasikan di sandbox ini)** — minta user cek 3
sheet ini (Backup/Restore, Diagnostic Log, Duplicate Finder) di identitas Liquid Glass: kalau blur
SUDAH kelihatan → fix ini terbukti benar, lanjut ke 4 sheet antrean + evaluasi turunkan tint lagi.
Kalau MASIH 0% blur di 3 sheet ini padahal `containerColor` sudah benar → bukti kuat penyebabnya
lebih dalam dari sekadar parameter ini (mis. perbedaan tier RenderNode vs RuntimeShader API 33+
yang disebut `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §2 — TIDAK bisa dipastikan tanpa device, sengaja
tidak ditebak lebih jauh batch ini).

**Ringkasan file** — 3 file kode (batas Micro-Batch). `FILE_MANIFEST.txt` tidak berubah (188/188).

**Batch 321 (Arsip `PROJECT_STATE.md`: pindahkan Batch 58–219 ke `PROJECT_STATE_ARCHIVE.md`, sesuai
kebijakan ~100 batch aktif Batch 158 — dikonfirmasi eksplisit user sesi ini, 0 kode, 2 dokumentasi)** —
Kandidat yang dicatat (BUKAN dieksekusi) di Batch 320: file ini sudah menyimpan 262 batch aktif
(58→320), jauh melebihi target "~100 batch terbaru" sejak Batch 158 (rencana re-arsip "sekitar
Batch 258" saat itu tidak pernah dieksekusi). User memilih eksplisit lewat `ask_user_input_v0`
sesi ini: "Arsipkan PROJECT_STATE.md dulu (docs only, lalu tanya lagi)" — 3 kandidat kode lain
(fix blur lintas-window, animasi rim-glow Aurora) TETAP menunggu keputusan terpisah, TIDAK
disentuh batch ini (murni arsip, sesuai pilihan user).

**Aksi** — 162 entri batch (Batch 219 turun ke Batch 58, urutan descending, dipindah VERBATIM
tanpa parafrase/pemotongan/edit isi — cuma lokasi file yang berubah) dipindah dari
`PROJECT_STATE.md` ke `PROJECT_STATE_ARCHIVE.md`, disisipkan tepat setelah header arsip +
separator `---`, di ATAS entri Batch 57 yang sudah ada di sana (descending tetap terjaga:
219→58→57→…→1, 0 gap baru/duplikat — gap pre-existing "Batch 83" yang memang tidak
pernah punya entri terpisah di histori asli TETAP dipertahankan apa adanya, BUKAN "diperbaiki"
diam-diam, sesuai ZERO-REFACTOR). `PROJECT_STATE.md` sekarang menyimpan 101 batch aktif
(Batch 220→320), mendekati target awal "~100 batch". Catatan pointer arsip
(`> Arsip Batch 1–57 dipindah…`) diperbarui jadi `Arsip Batch 1–219 dipindah…`, referensi
"Batch 58 ke atas" di kedua file diganti "Batch 220 ke atas". Header `PROJECT_STATE_ARCHIVE.md`
disamakan (deskripsi range + urutan descending dikonfirmasi ulang tetap benar).

**TIDAK disentuh (batas tugas murni arsip)** — section "## Riwayat insiden kronologis (jangan
dihapus)" (permanen, eksplisit ditandai jangan dihapus, kurasi insiden kritis lintas semua batch
lama — BUKAN bagian rotasi per-batch biasa) + section "Keputusan arsitektur utama"/"Struktur
package"/"Konvensi penamaan ZIP"/"Aturan sesi"/"Kebijakan prioritas mutakhir" — semua tetap
persis di `PROJECT_STATE.md`, 0 dipindah, 0 diedit. 0 file `.kt`/kode disentuh sama sekali batch
ini — murni potong-tempel teks dokumentasi.

**Verifikasi** — jumlah baris archived block (2852 baris, 161 header batch — 162 dikurangi 1
gap pre-existing "Batch 83") dicocokkan persis antara yang dipotong dari `PROJECT_STATE.md` dan
yang disisipkan ke `PROJECT_STATE_ARCHIVE.md`, 0 selisih, 0 teks hilang/terduplikasi. Total
gabungan batch kedua file tetap 320 (101 aktif + 219 arsip) sebelum dan sesudah — cuma lokasi
yang berubah, bukan isi. `FILE_MANIFEST.txt` TIDAK berubah (188/188 — 0 file baru/dihapus,
`PROJECT_STATE_ARCHIVE.md` sudah ada sejak Batch 158).

**Item selanjutnya (kode sungguhan, BUTUH pilihan user eksplisit — ditanya ulang setelah batch
ini)**: (1) investigasi fix blur Liquid Glass lintas-window (`MainActivity.kt`, protected, root
cause Batch 311), (2) animasi rim-glow Aurora (statis→bergerak, Batch 310), atau (3) instruksi/
laporan bug baru dari user — ketiganya TIDAK dieksekusi diam-diam batch ini (murni arsip sesuai
pilihan user).

**Batch 320 (Verifikasi integritas rilis — repack tanpa laporan bug baru, 0 kode, 3 dokumentasi)**
— User minta "sempurnakan, repack, lalu present" tanpa laporan bug/log_fail spesifik. Sesuai
Fast-Track (audit full project dilarang tanpa instruksi eksplisit), dibatasi ke verifikasi
integritas: (1) `FILE_MANIFEST.txt` vs disk — `diff` 188/188, 0 drift; (2) brace/paren/bracket
seluruh 126 file `.kt` (main+test+androidTest) — 3 file tampil "tidak seimbang" di hitungan
karakter mentah, SEMUA dikonfirmasi false-positive manual (`LyricsView.kt` komentar notasi
interval matematika, sudah didokumentasikan sejak Batch 245; `Type.kt` cuma prosa komentar
panjang, kode-saja 45/45 paren terverifikasi; `LyricsParserTest.kt` 1 string uji sengaja tanpa
`]` penutup, menguji penanganan bracket tak tertutup — bukan galat); (3) spot-check hasil Batch
318/319 (`PlaybackService.kt`/`NowPlayingScreen.kt`/`ic_notification_play_pause.xml`) — angka
brace/paren & validitas XML cocok persis catatan batch asalnya, 0 korupsi konten.

**Hasil: 0 bug, 0 file kode diedit.** `ROADMAP_LIQUID_GLASS_REDESIGN.md` (blur asli §3b) TIDAK
disentuh — masih menunggu instruksi eksplisit lanjut sub-langkah (aturan sesi #4), bukan
dieksekusi diam-diam. ZIP direpack identik isi kode dengan Batch 319, 3 dokumentasi VIP
disinkronkan. Versi APK tetap auto dari `GITHUB_RUN_NUMBER` (0 bump manual).

**Catatan observasi (BUKAN dieksekusi, butuh konfirmasi user dulu)**: `PROJECT_STATE.md` ini
sendiri sudah menyimpan batch 58→320 (262 batch aktif) — jauh melebihi pola "~100 batch lalu
arsipkan" yang ditetapkan Batch 158 (rencana lanjutan waktu itu: re-arsip "sekitar Batch 258",
belum pernah dieksekusi). File ini sekarang 5000+ baris. Kandidat batch terpisah kalau user minta:
potong batch lama (mis. 58-219) ke `PROJECT_STATE_ARCHIVE.md`, sisakan ~100 batch terbaru aktif —
TIDAK dieksekusi sesi ini karena bukan instruksi eksplisit dan berisiko kalau dipotong tergesa di
file sebesar ini tanpa konfirmasi dulu.

**Batch 319 (2 laporan user dalam 1 pesan — (1) efek persistent speed/repeat/shuffle tidak
berlaku kalau app di-kill lalu diputar via widget/media player eksternal/notifikasi, (2)
notifikasi cold-start "SONIX" dibuat statis/universal, 1 file kode + 1 drawable baru)** —

**Bug 1 (root cause)** — `PlaybackStateStore` sudah menyimpan repeatMode/shuffleEnabled (Batch
108) & speed (Batch 317), TAPI cuma pernah dipulihkan lewat `PlayerViewModel.connect()`/
`resumeFromSaved()` — jalur UI yang HANYA jalan kalau app dibuka. `PlaybackService`'s
`SavedQueueItems` (dipakai bareng `restoreLastQueue()` — widget cold-start — dan
`onPlaybackResumption()` — resume dari lock screen/Android Auto/Bluetooth setelah proses mati
total) cuma pernah meneruskan `items`/`startIndex`/`startPositionMs` ke player, 3 field lain
diam-diam diabaikan sejak field itu ditambah ke store — persis laporan user.

**Fix Bug 1 (`PlaybackService.kt`)** — `SavedQueueItems` +3 field (`repeatMode`,
`shuffleEnabled`, `speed`), `loadSavedQueueItems()` mengisinya dari `PlaybackStateStore`.
`restoreLastQueue()` set repeat/shuffle SEBELUM `setMediaItems()` (pola sama `resumeFromSaved()`
Batch 108) + `setPlaybackSpeed()`. `onPlaybackResumption()` set ketiganya ke `mediaSession.player`
(parameter callback, instance sama yang akan menerima item dari `completer.set()`) sebelum future
di-complete.

**Bug 2 (root cause)** — `buildColdStartNotification()` (`NotificationCompat` polos, BUKAN
`MediaStyle` — Batch 304) cuma diperbarui dari `onIsPlayingChanged` DI PROSES YANG SAMA; begitu
app di-kill lalu state berubah lewat widget/media player eksternal/notifikasi lain selagi
placeholder ini masih tampil (jendela s/d `MAX_HANDOFF_WAIT_MS` 8 detik), judul & label tombol
yang sudah terpasang tidak pernah ikut ter-refresh — sinkronisasi yang oleh desainnya sendiri
tidak akan pernah 100%.

**Fix Bug 2 (instruksi user eksplisit — statis tapi universal, bukan kejar sinkron)** —
`contentText` jadi teks tetap "Ketuk untuk membuka kontrol pemutaran" (bukan lagi judul lagu
dinamis). Tombol toggle jadi 1 ikon gabungan Putar/Jeda kustom baru (`ic_notification_play_pause.xml`,
baru, gaya "⏯️", BUKAN daur ulang `ic_widget_play`/`ic_widget_pause` yang sebelumnya ditukar
bergantian) + label tetap "Putar/Jeda" (bukan lagi "Jeda"/"Lanjutkan" bergantian).
`buildColdStartNotification()` disederhanakan jadi 0 parameter (`isPlaying`/`nowPlayingTitle`
dihapus, bukan dibiarkan jadi parameter mati) — 3 call site disesuaikan. `updateColdStartNotification()`
SENGAJA TETAP DIPERTAHANKAN (kini cuma me-repost konten identik) — mencabut hook itu dari
`onIsPlayingChanged` di luar scope 2 laporan ini (listener itu dipakai crossfade/shake
detector/floating bubble juga, ZERO-REFACTOR).

**Ringkasan file** — 1 file kode diubah (`PlaybackService.kt`), 1 drawable baru
(`ic_notification_play_pause.xml`), 0 dependency baru, 0 protected asset disentuh.
`FILE_MANIFEST.txt` 187→188. Brace/paren `PlaybackService.kt` seimbang utuh: 80/80 brace,
388/388 paren, 14/14 bracket.

**Belum divalidasi compile Gradle sungguhan** (0 akses SDK/Gradle/jaringan di sandbox sesi ini —
WAJIB cek CI run berikutnya). Risiko rendah utk Bug 1 (field + assignment ke API `Player` yang
sudah dipakai identik di `PlayerViewModel.kt`). Risiko sedang utk vector drawable baru (`pathData`
belum pernah divalidasi parser Android sungguhan di sandbox ini — sintaks M/L/Z/H/V standar, gaya
sama `ic_bubble_minimize.xml`/`ic_bubble_tile.xml` yang sudah terbukti compile).

**Belum diverifikasi visual/device** — prioritas cek: (1) set speed≠1x + repeat/shuffle ON,
force-stop app, tekan play di widget, konfirmasi ketiganya ikut ke lagu yang diputar; (2) ulangi
lewat resume lock screen/Bluetooth kalau ada akses; (3) trigger notifikasi cold-start, pastikan
ikon ⏯️ baru tampil benar (bukan kotak putih/pecah) dan teks/tombol tidak lagi berubah-ubah
mengikuti lagu/status. Detail lengkap: `CHANGELOG.md` Batch 319.

**Batch 318 (Laporan user, screenshot: teks "Fade Halus" ke-clip di dialog Pengaturan Putar — 1
file)** — User kirim screenshot `SpeedDialog` (dibuka dari Now Playing → ⋮ → Kecepatan): opsi
"Fade Halus" di seksi "Transisi Antar Lagu" (paling bawah) subtitle-nya terpotong.

**Root cause** — PERSIS pola yang sama Batch 314-316 (`Column` fixed tanpa jaring pengaman
scroll), tapi di lokasi yang LUPUT dari audit "pola tab serupa" batch-batch itu: audit itu
scope-nya cuma 5 `ModalBottomSheet`, sedangkan `SpeedDialog` ini `AlertDialog` — kontainer beda,
gejala identik. Total tinggi konten (6 opsi Kecepatan + toggle Mode Audiobook/Podcast + 2 opsi
Transisi Antar Lagu dengan subtitle panjang) melebihi tinggi yang dialokasikan Material3
`AlertDialog` ke slot `text`, baris paling bawah diam-diam ke-clip.

**Fix** — `.verticalScroll(rememberScrollState())` ditambah ke `Column` utama di slot `text`
milik `SpeedDialog` (`NowPlayingScreen.kt`). Import sudah ada di file ini sebelumnya (dipakai di
tempat lain), 0 import baru.

**Cek lokasi serupa lain** — Audit ulang cepat: `AlertDialog` LAIN di codebase ini (konfirmasi
hapus, dsb.) kontennya pendek/statis (1-2 baris), tidak berisiko pola sama. `SpeedDialog` adalah
satu-satunya `AlertDialog` dengan konten sepanjang ini.

**Ringkasan file** — 1 file kode diubah (`NowPlayingScreen.kt`), 0 file baru, 0 dependency baru,
0 komposable lain di file ini disentuh (`ZERO-REFACTOR`). `FILE_MANIFEST.txt` tidak berubah
(187/187). Brace/paren diverifikasi seimbang: 218/218 brace, 807/807 paren, 1/1 bracket.

**Belum divalidasi compile Gradle sungguhan** (tidak ada akses Android SDK/Gradle/jaringan di
sandbox sesi ini — WAJIB cek CI run berikutnya). Risiko sintaks rendah: `verticalScroll`/
`rememberScrollState` sudah dipakai identik di 5+ file lain codebase yang sama.

**Batch 317 (Laporan user: Kecepatan Putar tidak persistent — 2 file)** — User minta inspeksi tab
Pengaturan/Kecepatan Putar, ketahuan `setPlaybackSpeed()` di `PlayerViewModel` cuma
`controller?.setPlaybackSpeed()` in-memory, tidak pernah ditulis/dibaca dari `PlaybackStateStore`
— hilang tiap proses di-kill (beda dari Mode Audiobook per-lagu di `AudiobookModeStore`, yang
memang sudah persistent tapi cuma untuk lagu yang di-opt-in, Batch 93).

**Root cause & pola fix** — Sama persis Gap List #6 Batch 108 (repeat/shuffle): field baru
ditambah ke `PlaybackStateStore` (`SCHEMA_VERSION` 2→3, `KEY_SPEED`, default 1.0x aman untuk
state lama), ditulis di `persistPlaybackState()` (sudah baca `_uiState.value.playbackSpeed`
sebelumnya, tinggal diteruskan ke `save()`), dipulihkan di titik BARU: `connect()`
(controller-connect) — BUKAN `resumeFromSaved()` — supaya berlaku ke lagu apa pun yang diputar
duluan, bukan cuma saat user lanjut queue lama (`playQueue()` sendiri tidak pernah reset speed
eksplisit, jadi begitu di-set saat connect, otomatis nempel ke instance ExoPlayer yang sama untuk
lagu berikutnya).

**`PlaybackStateStore.kt`** — `SavedPlaybackState.speed`, param `speed` di `save()`, `KEY_SPEED` +
`SCHEMA_VERSION` 3.

**`PlayerViewModel.kt`** — `restoreSavedSpeed()` baru, dipanggil sekali di `connect()` setelah
controller ready. `persistPlaybackState()` dapat param opsional `speedOverride` (default null —
8 call site lama TIDAK berubah) supaya `setPlaybackSpeed()` bisa simpan LANGSUNG nilai yang baru
di-set, bukan baca `_uiState.value.playbackSpeed` yang update-nya lewat listener
`onPlaybackParametersChanged` (async, belum tentu sudah landing di call stack yang sama).
`setPlaybackSpeed()` sekarang panggil `persistPlaybackState(speedOverride = speed)` tiap
dipanggil (bukan nunggu tick periodik ~5s), supaya speed yang diganti saat PAUSE tetap tersimpan.

**Interaksi dengan Mode Audiobook per-lagu** — TIDAK bentrok: begitu lagu ber-status opt-in
Audiobook mulai transisi (`onMediaItemTransition`), speed per-lagu dari `AudiobookModeStore`
tetap override speed global (urutan sudah begitu sejak Batch 93) — speed global cuma "default"
untuk lagu yang TIDAK opt-in.

**Ringkasan file** — 2 file kode diubah, 0 file baru, 0 dependency baru, 0 parameter/callback
publik BERUBAH (cuma nambah 1 optional param `speedOverride` berdefault null — semua caller lama
tetap valid tanpa ubah). `FILE_MANIFEST.txt` tidak berubah (187/187). Brace/paren diverifikasi
seimbang: `PlaybackStateStore.kt` 10/10 brace, 48/48 paren, 1/1 bracket; `PlayerViewModel.kt`
221/221 brace, 786/786 paren, 29/29 bracket.

**Belum divalidasi compile Gradle sungguhan** (tidak ada akses Android SDK/Gradle/jaringan di
sandbox sesi ini — WAJIB cek CI run berikutnya). Risiko sintaks rendah: pola SharedPreferences +
default-param persis dipakai di Gap List #6 (Batch 108) yang sudah terbukti compile.

**Batch 316 (Tuntaskan antrean audit Batch 314: terapkan `verticalScroll` ke 2 sheet terakhir —
`UpdateCheckSheet.kt`, `BackupRestoreSheet.kt`, 2 file, item antrean internal Batch 314/315, bukan
laporan bug baru user)** — Melengkapi 2 sisa dari 5 sheet yang kena pola sama (`Column` fixed
dalam `ModalBottomSheet` tanpa `verticalScroll`/`LazyColumn` jaring pengaman), yang sudah ditandai
"konten pendek, risiko rendah" sejak audit Batch 314. Dengan ini, audit "pola tab serupa" Batch
314 (5 sheet) SELESAI TOTAL — 0 sisa antrean pola ini.

**Root cause & pola fix** — PERSIS sama Batch 314/315: kalau total tinggi konten `Column`
melebihi tinggi sheet yang tersedia (layar pendek/gesture-nav/font sistem besar), baris paling
bawah diam-diam ke-clip alih-alih bisa digeser.

**`UpdateCheckSheet.kt`** (prioritas 4 — konten pendek di kebanyakan state
Idle/Checking/UpToDate/Error, TAPI state `Available` bisa memanjang: judul + catatan rilis
multi-baris + tombol) — `.verticalScroll(rememberScrollState())` ditambah ke modifier chain
`Column` utama, sebelum `.padding(horizontal = 20.dp)`. Sheet ini TIDAK pakai `frostedGlass()`/
tema Calm Retro (beda dari `EqualizerSheet.kt`/`VisualizerSheet.kt`), jadi tidak ada blok
`.then(...)` yang perlu dilewati — scroll langsung setelah `.fillMaxWidth()`.

**`BackupRestoreSheet.kt`** (prioritas 5, terakhir — judul, deskripsi, 2 `OutlinedButton`, banner
hasil opsional) — `.verticalScroll(rememberScrollState())` ditambah dengan pola sama, langsung
setelah `.fillMaxWidth()`, sebelum `.padding(horizontal = 20.dp)`. `AlertDialog` konfirmasi timpa
data (composable terpisah, di luar `ModalBottomSheet` ini) TIDAK disentuh — sudah pakai `Column`
pendek tanpa risiko serupa (cuma teks + daftar ringkas jumlah data).

**Ringkasan file** — 2 file kode diubah, masing-masing +2 import (`rememberScrollState`,
`verticalScroll`) + 1 blok komentar + 1 baris modifier. 0 file baru, 0 dependency baru, 0
parameter/callback publik berubah, 0 komposable lain di kedua file disentuh (`ZERO-REFACTOR`).
`FILE_MANIFEST.txt` tidak berubah (187/187 — 2 file sudah ada sebelumnya). Brace/paren
diverifikasi seimbang tiap file: `UpdateCheckSheet.kt` 25/25 brace, 72/72 paren;
`BackupRestoreSheet.kt` 33/33 brace, 94/94 paren. Jauh di bawah limit Micro-Batch (maks 3 file
kode).

**Belum divalidasi compile Gradle sungguhan** (tidak ada akses Android SDK/Gradle/jaringan di
sandbox sesi ini — WAJIB cek CI run berikutnya). Risiko sintaks rendah: `verticalScroll`/
`rememberScrollState` sudah dipakai identik di 5 file lain codebase yang sama
(`NowPlayingScreen.kt`, `SongInfoEditSheet.kt`, `EqualizerSheet.kt`, `RingtoneCutterSheet.kt`,
`VisualizerSheet.kt`), pola sudah terbukti compile, bukan hal baru.

**Status audit "pola tab serupa" Batch 314** — TUNTAS. Ke-5 sheet yang teridentifikasi punya
`Column` fixed tanpa jaring pengaman scroll (`EqualizerSheet.kt`/`RingtoneCutterSheet.kt`/
`VisualizerSheet.kt` Batch 315, `UpdateCheckSheet.kt`/`BackupRestoreSheet.kt` batch ini) semua
sudah dapat `verticalScroll`. Tidak ada antrean lanjutan dari topik ini untuk batch berikutnya.

**Batch 315 (Lanjutan audit Batch 314: terapkan `verticalScroll` ke 3 dari 5 sheet antrean —
`EqualizerSheet.kt`, `RingtoneCutterSheet.kt`, `VisualizerSheet.kt`, 3 file, item antrean internal
Batch 314, bukan laporan bug baru user)** — Eksekusi urutan prioritas yang sudah ditetapkan Batch
314: dari 5 sheet yang kena pola sama (`Column` fixed dalam `ModalBottomSheet` tanpa
`verticalScroll`/`LazyColumn` jaring pengaman), 3 prioritas TERTINGGI dikerjakan batch ini (limit
Micro-Batch 3 file kode sudah penuh), 2 sisanya (`UpdateCheckSheet.kt`, `BackupRestoreSheet.kt` —
konten pendek, risiko rendah) diantrekan Batch 316.

**Root cause & pola fix** — PERSIS sama Batch 314 (yang sendiri PERSIS sama fix awal
`NowPlayingScreen.kt`/`SongInfoEditSheet.kt`): kalau total tinggi konten `Column` melebihi tinggi
sheet yang tersedia (layar pendek/gesture-nav/font sistem besar), baris paling bawah diam-diam
ke-clip alih-alih bisa digeser. Bukan pola baru — murni menerapkan jaring-pengaman yang sudah
terbukti dipakai di 3 tempat lain codebase yang sama.

**`EqualizerSheet.kt`** (prioritas 1 — jumlah band EQ variatif per device, makin banyak band makin
tinggi total konten, ditambah 2 baris preset chip di atasnya) — `.verticalScroll
(rememberScrollState())` ditambah ke modifier chain `Column` utama, persis setelah blok
`.then(...)` tema Calm Retro, sebelum `.padding(...)` — urutan sama seperti pola
`SongInfoEditSheet.kt`/`AdvancedControlsSheet` Batch 314 (padding ikut discroll bersama konten).

**`RingtoneCutterSheet.kt`** (prioritas 2 — judul+lagu, 2 slider awal/akhir, teks durasi, 3
`DestinationChip` sejajar, catatan penyimpanan, tombol "Potong & Simpan") — `.verticalScroll
(rememberScrollState())` ditambah ke `Column` utama, sebelum `.padding(horizontal = 20.dp,
vertical = 12.dp)`.

**`VisualizerSheet.kt`** (prioritas 3 — teks edukasi izin Mikrofon 4 baris saat
`!permissionGranted` + `SpectrumBars` 120dp saat aktif) — `.verticalScroll
(rememberScrollState())` ditambah persis setelah blok `.then(...)` tema Calm Retro (shell identik
`EqualizerSheet.kt`, sama-sama warisan pola v3 Batch 134→135), sebelum `.padding(...)`.

**Ringkasan file** — 3 file kode diubah, masing-masing +2 import (`rememberScrollState`,
`verticalScroll`) + 1 blok komentar + 1 baris modifier. 0 file baru, 0 dependency baru, 0
parameter/callback publik berubah, 0 komposable lain di ketiga file disentuh (`ZERO-REFACTOR`).
`FILE_MANIFEST.txt` tidak berubah (187/187 — 3 file sudah ada sebelumnya). Brace/paren
diverifikasi seimbang tiap file: `EqualizerSheet.kt` 27/27 brace, 106/106 paren;
`RingtoneCutterSheet.kt` 24/24 brace, 91/91 paren; `VisualizerSheet.kt` 10/10 brace, 73/73 paren.

**Belum divalidasi compile Gradle sungguhan** (tidak ada akses Android SDK/Gradle/jaringan di
sandbox sesi ini — WAJIB cek CI run berikutnya). Risiko sintaks rendah: `verticalScroll`/
`rememberScrollState` sudah dipakai identik di 3 file lain codebase yang sama
(`NowPlayingScreen.kt`, `SongInfoEditSheet.kt`), import & posisi dalam modifier chain mengikuti
pola yang sudah terbukti compile, bukan pola baru yang belum pernah diuji.

**Antrean Batch 316** — terapkan `verticalScroll` (pola sama) ke `UpdateCheckSheet.kt` dan
`BackupRestoreSheet.kt`, 2 sisa dari audit Batch 314 — konten pendek & risiko rendah, tidak
dikerjakan batch ini karena limit Micro-Batch 3 file kode sudah penuh oleh 3 prioritas tertinggi.

**Batch 314 (Fix sheet "Kontrol Lanjutan" terpotong + Equalizer tidak auto re-attach ke sesi audio
baru, 3 file, 2 laporan user)** — 2 laporan terpisah dalam 1 pesan, masing-masing diminta plus
audit pola serupa.

**Bug 1 — "Kontrol Lanjutan" terpotong.** `NowPlayingScreen.kt`: `Column` dalam
`AdvancedControlsSheet` tidak pernah dibungkus `verticalScroll` — begitu total tinggi 3 seksi +
2 divider + slider volume + section header melebihi tinggi sheet yang tersedia (layar pendek/
gesture-nav/font besar), baris paling bawah ("Potong Nada Dering") diam-diam ke-clip, bukan bisa
digeser. Root cause & fix PERSIS sama dengan yang sudah pernah diperbaiki di body utama
`NowPlayingScreen` sendiri (lihat komentar `verticalScroll` di scaffold utama fungsi itu) — fix
kali ini murni menerapkan pola jaring-pengaman yang SUDAH ADA di codebase yang sama (juga dipakai
`SongInfoEditSheet.kt`), bukan pola baru. **Audit "pola tab serupa"** (semua `ModalBottomSheet` di
`ui/`): dari 22 sheet, 5 LAINNYA juga punya `Column` fixed tanpa `verticalScroll` maupun
`LazyColumn` sebagai jaring pengaman — `EqualizerSheet.kt` (prioritas tertinggi, jumlah band EQ
variatif per device + 2 baris preset), `RingtoneCutterSheet.kt`, `VisualizerSheet.kt`,
`UpdateCheckSheet.kt`, `BackupRestoreSheet.kt` (2 terakhir konten pendek, risiko rendah). Sengaja
BELUM disentuh batch ini (lihat batas Micro-Batch 3 file kode di bawah) — **diantrekan Batch 315**,
urutan sesuai prioritas di atas.

**Bug 2 — Equalizer tidak persistent.** Root cause BUKAN di penyimpanan (`EqualizerController.kt`
sudah benar simpan band/preset/enabled ke SharedPreferences sejak awal, dan `attach()` sudah benar
baca+terapkan ulang) — root cause di PEMANGGILAN `attach()`: satu-satunya call site adalah
`ensureEqualizerAttached()`, dan itu cuma dipanggil dari `onOpenEqualizer` (`MainActivity.kt`) —
artinya efek `android.media.audiofx.Equalizer` NYATA cuma ter-reattach ke sesi audio kalau user
BUKA sheet Equalizer secara manual. Sesi audio baru yang lebih dulu terjadi (cold-start app,
Service restart, ExoPlayer bikin ulang AudioTrack di tengah pemutaran) TIDAK pernah otomatis
ter-reattach — settingan tersimpan tetap ada di prefs (kelihatan benar kalau sheet dibuka), tapi
suara yang benar-benar keluar flat/tanpa EQ sampai user buka sheet lagi. Ini kontradiksi langsung
dengan doc-comment `ensureEqualizerAttached()` sendiri yang sudah menyatakan niatnya "must keep
affecting real audio in the background regardless of whether its sheet is open" — wiring-nya saja
yang belum pernah sesuai niat itu. **Fix**: `PlaybackAudioSession.kt` — tambah hook
`onSessionIdChanged` (setter `sessionId` custom, invoke listener tiap kali ID baru non-zero masuk)
supaya `PlaybackService.kt` (listener `onEvents`-nya) TIDAK perlu disentuh sama sekali.
`PlayerViewModel.kt` — daftarkan listener itu di `init{}` (`equalizerController.attach(id)`), plus
attach sekali langsung kalau sesi sudah ada saat ViewModel dibuat (mis. Service masih main di
background). **Audit "pola yang sama"**: 1 controller `AudioEffect` lain di codebase ini
(`AudioVisualizerController`) — TIDAK kena bug sejenis; lazy-attach-nya memang disengaja (baca
doc-comment `ensureVisualizerAttached()`: trade-off baterai, bukan niat "selalu aktif" seperti
Equalizer). `SilenceSkipStore`/`CrossfadeStore` dua-duanya sudah benar dibaca ulang di
`PlaybackService.onCreate()` tiap kali Service baru, tidak kena pola bug ini.

**Ringkasan file** — 3 file kode diubah (`PlaybackAudioSession.kt` +8 baris,
`PlayerViewModel.kt` +14 baris di `init{}`, `NowPlayingScreen.kt` +8 baris + 1 modifier baru). 0
file baru, 0 dependency baru, 0 perubahan struktur/urutan grup yang sudah ada. Brace/paren
diverifikasi seimbang tiap file yang disentuh (`NowPlayingScreen.kt` 218/218 brace, 800/800 paren;
`PlayerViewModel.kt` 220/220 brace, 772/772 paren; `PlaybackAudioSession.kt` 2/2 brace, 7/7 paren).

**Belum divalidasi compile Gradle sungguhan** (tidak ada akses Android SDK/Gradle/jaringan di
sandbox sesi ini — WAJIB cek CI run berikutnya, sama seperti pelajaran Batch 312→313). Sudah
diverifikasi manual: nama fungsi/properti (`equalizerController.attach`, `PlaybackAudioSession.
sessionId`, `rememberScrollState`/`verticalScroll`) dicek ada & sudah dipakai identik di tempat
lain file yang sama; import `verticalScroll`/`rememberScrollState` sudah ada di
`NowPlayingScreen.kt` (dipakai composable lain di file yang sama), tidak perlu import baru.

**Antrean Batch 315** — terapkan `verticalScroll` (pola sama Batch 314) ke `EqualizerSheet.kt`
(prioritas 1), `RingtoneCutterSheet.kt`, `VisualizerSheet.kt`, `UpdateCheckSheet.kt`,
`BackupRestoreSheet.kt`, dari hasil audit di atas — belum dieksekusi batch ini karena limit
Micro-Batch 3 file kode sudah penuh oleh 2 bug yang dilaporkan user langsung.

**Batch 313 (Fix CI build gagal: `Modifier.padding()` overload tidak valid di Batch 312, 1 file,
`log_fail_305.zip` dari user)** — User upload log build CI (`build-output.log`, Gradle 8.14.3):
`:app:compileDebugKotlin` & `:app:compileReleaseKotlin` GAGAL, error tepat di
`NowPlayingScreen.kt:1084:29` — kode baru `AdvancedControlsSectionHeader` yang ditambah Batch 312
kemarin. Root cause: `Modifier.padding(horizontal = 20.dp, top = 4.dp, bottom = 4.dp)` mencampur
parameter `horizontal` (dari overload 2-parameter `padding(horizontal, vertical)`) dengan
`top`/`bottom` (dari overload 4-parameter `padding(start, top, end, bottom)`) — 2 overload
BERBEDA, Kotlin tidak bisa resolve kombinasi keduanya sekaligus, compile error "None of the
following candidates is applicable". Regresi murni dari sesi kemarin, TIDAK pernah lolos compile
sungguhan sebelum dikirim (dicatat eksplisit di entri Batch 312: "Belum divalidasi compile Gradle
sungguhan (WAJIB cek CI)" — sekarang baru ketahuan lewat CI beneran, persis alasan catatan itu ada).

**`NowPlayingScreen.kt`** (1 file, 1 baris) — diganti ke overload 4-parameter yang valid:
`Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 4.dp)` — hasil visual IDENTIK
(20dp kiri/kanan, 4dp atas/bawah, sama persis yang dimaksud Batch 312), cuma nama parameter yang
diperbaiki jadi kombinasi yang benar-benar ada di API `Modifier.padding()`. 0 baris lain di file
ini disentuh — grep ulang seluruh file utk pola `horizontal=...`+`top/bottom=...` campur yang
sama: 0 kecocokan lain ditemukan, jadi ini SATU-SATUNYA titik yang salah dari Batch 312.

**Ringkasan file** — 1 file kode, 1 baris diubah — jauh di bawah batas Micro-Batch. 0 file baru, 0
dependency baru, 0 perubahan struktur/urutan grup (pengelompokan Pemutaran/Audio/Lagu dari Batch
312 tidak disentuh sama sekali, murni bug sintaks). `FILE_MANIFEST.txt` tidak berubah (187/187).
Brace/paren `NowPlayingScreen.kt` diverifikasi seimbang utuh: 218/218 braces, 795/795 parens.

**Masih belum divalidasi compile Gradle sungguhan** (WAJIB cek CI run berikutnya) — TAPI kali ini
akar masalahnya sudah dikonfirmasi persis dari pesan error compiler asli (bukan tebakan): overload
`Modifier.padding(start, top, end, bottom)` adalah API resmi Compose foundation yang sudah dipakai
di tempat lain pada codebase yang sama, risiko sisa sangat rendah.

**Pelajaran utk sesi berikutnya** — komposable baru yang pakai `Modifier.padding()` dengan lebih
dari 2 parameter WAJIB pastikan kombinasi nama parameter itu benar ada di 1 overload yang sama
(`(horizontal, vertical)` ATAU `(start, top, end, bottom)` ATAU `(all)` ATAU `(PaddingValues)` —
TIDAK BISA dicampur lintas overload), bukan cuma diasumsikan dari pola visual yang diinginkan.

**Batch 312 (Rapikan sheet "Kontrol Lanjutan": kelompokkan 9 baris jadi 3 seksi berdasar
kegunaan, 1 file, klarifikasi user langsung — "maksud saya rapikan menu utilitas yang tidak
dipisahkan berdasarkan kegunaan umumnya")** — Lanjutan langsung dari Batch 311. Batch 311 salah
tafsir "berantakan/tidak-professional" sebagai bug transparansi tint (sudah benar diperbaiki,
TIDAK di-revert batch ini); klarifikasi user kali ini soal hal LAIN: 9 baris menu (Antrean, Lirik,
Sleep Timer, Kecepatan, Equalizer, Repeat A-B & Bookmark, Visualizer, Edit Info, Potong Nada
Dering) sebelumnya flat berurutan tanpa pengelompokan sama sekali — dianggap tidak rapi/tidak
professional secara struktur, terlepas dari isu tint kemarin.

**`NowPlayingScreen.kt`** (1 file) — `AdvancedControlsSheet` composable direstrukturisasi jadi 3
seksi ala grouped-list iOS (label kecil di atas tiap grup + `HorizontalDivider` di antaranya, gaya
divider SAMA PERSIS yang sudah ada sebelumnya sebelum "Peredam Dalam Aplikasi", cuma sekarang
dipakai konsisten di antara SEMUA seksi bukan cuma 1 titik):
1. **"Pemutaran"** — Antrean Putar, Sleep Timer, Kecepatan Putar, Repeat A-B & Bookmark (kontrol
   yang mengatur JALANNYA putar lagu saat ini/berikutnya).
2. **"Audio"** — Equalizer, Visualizer Audio, + slider "Peredam Dalam Aplikasi" (pemrosesan/
   tampilan sinyal audio, bukan soal urutan/waktu putar).
3. **"Lagu"** — Lirik, Edit Info Lagu, Potong Nada Dering (konten/metadata per-lagu — hasil akhir
   aksinya nempel ke lagu itu sendiri, bukan ke sesi putar yang sedang berjalan).

Composable baru `AdvancedControlsSectionHeader(title: String)` ditambah tepat di sebelah
`AdvancedControlRow` (private, cuma dipakai di sheet ini) — gaya teks SENGAJA disamakan persis
dengan label "Peredam Dalam Aplikasi" yang sudah ada sebelumnya (`labelSmall` + `secondary`),
supaya section header terasa 1 sistem konsisten dengan yang sudah dikenal user, bukan pola baru
yang asing. 0 icon/label/callback yang diubah atau dihapus — 9 `AdvancedControlRow` + 1 baris
slider volume SEMUA masih ada, cuma urutan & pengelompokan yang berubah; 9/9 parameter callback
(`onOpenQueue`...`onOpenRingtoneCutter`) diverifikasi masih terpasang tepat 1x masing-masing.

**Ringkasan file** — 1 file kode (`NowPlayingScreen.kt`), jauh di bawah batas Micro-Batch
(maksimal 3). 0 file baru, 0 dependency baru, 0 parameter/callback publik yang berubah tanda
tangan (`AdvancedControlsSheet` tetap private, dipanggil sama seperti sebelumnya). 0 komposable
lain di file ini disentuh (`ZERO-REFACTOR`). `FILE_MANIFEST.txt` tidak berubah (187/187).
Brace/paren `NowPlayingScreen.kt` diverifikasi seimbang utuh: 218/218 braces, 795/795 parens.

**Belum divalidasi compile Gradle sungguhan** (WAJIB cek CI) — risiko sintaks rendah: murni
reorder pemanggilan composable yang sudah ada + 1 composable baru sangat sederhana (`Text` +
`Modifier.padding`, pola yang sudah dipakai berulang di file yang sama).

**Belum diverifikasi visual di device** — kalau user buka lagi "Kontrol Lanjutan": harus terlihat
3 grup terpisah label "Pemutaran"/"Audio"/"Lagu" dengan divider di antaranya (bukan lagi 9 baris
flat tanpa jeda). Urutan baru: Antrean→Sleep Timer→Kecepatan→Repeat A-B (grup 1), Equalizer→
Visualizer→slider volume (grup 2), Lirik→Edit Info→Potong Nada Dering (grup 3) — beda dari urutan
lama, disengaja mengikuti pengelompokan, bukan regresi urutan.

**Batch 311 (Fix bug: sheet "Kontrol Lanjutan" berantakan/tidak-professional, 1 file, laporan
screenshot user langsung)** — User kirim screenshot `ModalBottomSheet` "Kontrol Lanjutan" (Now
Playing) dengan teks latar (coachmark "Geser di kiri/kanan piringan buat atur kecerahan & volume
HP... Ketuk ⋮ buat Sleep Timer, Kecepatan, dan Equalizer" dari `NowPlayingScreen.kt`) tembus
HAMPIR PENUH di belakang sheet, tumpang-tindih sama isi sheet sendiri — dilaporkan user
"berantakan" & "jauh dari kesan professional".

**Root cause** — `frostedGlass()` (`BlurUtils.kt`) utk identitas Liquid Glass sudah diturunkan
bertahap ke tint sangat tipis (0.38f gelap/0.48f terang, Batch 296-299) dengan asumsi `hazeEffect`
(blur asli via Haze) akan menutupi sisanya. Asumsi itu SALAH khusus utk `ModalBottomSheet`/
`Dialog`: keduanya render di Android Window terpisah dari `hazeSource` (`Box` pembungkus NavHost
di `MainActivity.kt`), jadi capture `RenderNode` Haze tidak bisa sample lintas-window — blur diam-
diam no-op utk SEMUA bottom sheet/dialog app-wide (12+ call site `frostedGlass()`), sisa cuma tint
0.38/0.48 itu sendiri TANPA blur di baliknya. Setiap iterasi tuning Batch 296-299 sebelumnya
mengasumsikan arah masalah "blur ketutup tint" (tint diturunkan tiap kali) — screenshot ini
membuktikan arah SEBALIKNYA: 0 blur yang kelihatan sama sekali, tint sendirian jauh terlalu tipis.

**`BlurUtils.kt`** (1 titik, 1 file) — `liquidGlassAlpha` dinaikkan 0.38f/0.48f → **0.85f/0.90f**
(dekat opaque, BUKAN full 1f ala Skeu — sengaja masih sisakan sedikit karakter glass utk elemen
yang render DALAM window yang sama dengan `hazeSource`, mis. MiniPlayerBar/card Home-Library/
panel NowPlaying, yang capture-nya kemungkinan tetap sah karena 1 window sama). `blurRadius`/
`edgeBrush`/pemanggilan `hazeEffect` TIDAK disentuh — bukan akar masalah (lihat root cause di
atas), murni 1 parameter tint yang jadi fallback aman terlepas blur cross-window itu jalan atau
tidak. `MainActivity.kt` (lokasi `hazeSource`, ada di daftar Protect) TIDAK disentuh — perbaikan
wiring Haze lintas-window sesungguhnya (mis. pindah scrim/blur ke layer yang sama dengan sheet)
di luar scope fix 1-parameter minimal-risiko ini, dicatat sebagai item lanjutan di bawah.

**Kenapa cuma naikkan tint, bukan re-arsitektur Haze cross-window** — STABILITY > Speed +
ZERO-REFACTOR: opsi lain (pindahkan `hazeSource` ke root Activity, atau ganti `ModalBottomSheet`
ke komponen non-Popup) menyentuh `MainActivity.kt`/`NavGraph` yang diproteksi & berisiko pecah di
12+ call site lain yang justru sudah benar (MiniPlayerBar dkk, dalam window yang sama). Menaikkan
1 parameter tint adalah perubahan minimal yang PASTI memperbaiki keterbacaan (monoton — makin
opaque makin sedikit bleed-through) tanpa menyentuh apa pun yang berisiko ke fitur lain.

**Ringkasan file** — 1 file kode (`BlurUtils.kt`), jauh di bawah batas Micro-Batch (maksimal 3). 0
file baru, 0 dependency baru, 0 token warna baru — reuse nama variabel `liquidGlassAlpha` yang
sudah ada sejak Batch 296. `FILE_MANIFEST.txt` tidak berubah (187/187, 0 file baru/dihapus).
Brace/paren `BlurUtils.kt` diverifikasi seimbang utuh: 9/9 braces, 127/127 parens.

**Belum divalidasi compile Gradle sungguhan** (WAJIB cek CI) — risiko sintaks sangat rendah: cuma
ganti 2 literal `Float` (`0.38f`/`0.48f` → `0.85f`/`0.90f`), 0 struktur/API/branch baru.

**Belum diverifikasi visual di device** — kalau user buka lagi "Kontrol Lanjutan" (atau bottom
sheet apa pun) di identitas Liquid Glass: latar belakang seharusnya TIDAK lagi tembus terbaca,
sheet terlihat solid/rapi seperti 4 identitas non-glass lainnya (bukan lagi ghost text). 5
identitas lain (Apple/Tactile/Skeu/CalmRetro/Aurora) 0 berubah — tidak menyentuh cabang mereka.

**Item berikutnya (belum diminta user, JANGAN dikerjakan diam-diam)**: kalau user mau blur ASLI
(bukan cuma tint opaque fallback) tetap tampak di bottom sheet/dialog Liquid Glass, perlu
investigasi wiring Haze lintas-window sesungguhnya (root cause di atas) — perubahan itu akan
menyentuh `MainActivity.kt` yang diproteksi, jadi WAJIB dikonfirmasi user dulu sebelum eksekusi.

**Batch 310 (Tema ke-6 "Aurora", Fase 5/N — rim-glow per-panel, wiring app-wide lewat
`frostedGlass()`, 1 file, permintaan user langsung "lanjut wiring rim-glow kesemua area!!")** —
Lanjutan langsung dari Batch 309. Menuntaskan SATU-SATUNYA item Aurora yang masih berstatus
"ditunda" sejak Batch 306 ("rim-glow per-panel eksplisit ditunda utk dipertimbangkan lagi nanti,
BUKAN dibatalkan") — sekarang diminta eksplisit oleh user. Dengan batch ini, cakupan efek Aurora
yang dikonfirmasi user di awal (ambient background + rim-glow per-panel) SELESAI PENUH, di atas
color+typography+shape murni yang sudah lengkap sejak Batch 307-309.

**Dampak nyata mulai batch ini**: setiap panel/card/sheet yang route lewat `frostedGlass()`
(MiniPlayerBar, panel NowPlaying, tiap bottom sheet, card Home/Library) saat Aurora aktif sekarang
punya rim/border ber-gradasi 4 warna lintas spektrum Aurora (hijau→teal→ungu→magenta), bukan lagi
rim flat netral generik seperti sebelumnya.

**`BlurUtils.kt`** (2 titik, 1 file) —
1. `val isAurora = isAuroraTheme()` ditambah setelah `isLiquidGlass`, pola identik.
2. `edgeBrush`'s `when` dapat cabang baru `isAurora ->`: `Brush.linearGradient` 4-stop
   (AuroraGreen/Teal/Violet/Magenta) dengan alpha menurun tiap stop (1.0x/0.85x/0.6x/0.35x dari
   `AuroraGlowAlpha` — 3 multiplier pertama SAMA PERSIS `auroraGlow()`, 1 falloff tambahan utk
   stop ke-4). 0 token warna/alpha baru — murni reuse token Aurora yang sudah ada sejak Batch 306.
   Own branch, BUKAN jatuh ke `else` (pola sama alasan `isLiquidGlass`) — kalau dibiarkan `else`,
   Aurora (dark-locked, `colorsFor()` abaikan `isDark`) akan kebagian rim flat netral `onSurface`,
   bukan warna khas identitasnya.

**Kenapa `frostedGlass()`, bukan sentuh tiap screen 1-per-1** — fungsi ini SATU titik shared yang
dilalui SEMUA panel glass app-wide (grep 12+ call site, precedent Batch 281 Liquid Glass) — 1
branch di sini otomatis mewujudkan permintaan user "kesemua area" tanpa perlu edit per-file
screen, arsitektur identik cara Liquid Glass dapat edge-glow terpusat.

**Keputusan disengaja: statis, BUKAN animated** — beda dari `auroraGlow()` (1 instance
`rememberInfiniteTransition` di root Surface), rim ini dipasang di titik yang dipanggil 12+ kali
sekaligus per komposisi — 12+ infinite transition independen serentak adalah biaya performa baru
yang belum pernah diverifikasi device (project ini sudah pernah kena masalah stutter blur Liquid
Glass, Batch 300). Statis dulu sebagai titik awal paling aman; animasi jadi kandidat lanjutan
kalau user minta setelah versi statis ini terverifikasi visual.

**Tidak dibedakan `isDark`** (beda dari `isTactile`/`isLiquidGlass` di branch yang sama) — Aurora
cuma punya 1 mode terkunci gelap permanen, cabang `isDark` di sini berisiko salah pilih kalau
toggle sistem user "terang" walau skema warna yang dipakai tetap dipaksa gelap — alasan sama
persis kenapa CalmRetro juga tidak dibedakan `isDark` di titik yang sama.

**Ringkasan file**: 1 file kode (`BlurUtils.kt`), jauh di bawah batas Micro-Batch (maksimal 3). 0
file baru, 0 dependency baru, 0 token warna/alpha baru. `FILE_MANIFEST.txt` tidak berubah
(187/187). Brace/paren `BlurUtils.kt` diverifikasi seimbang utuh: 9/9 braces, 117/117 parens.

**Belum divalidasi compile Gradle sungguhan** — WAJIB cek CI. Risiko sintaks rendah
(`Brush.linearGradient(colors = listOf(...))` konstruktor yang sudah dipakai berulang kali di
file yang sama, `isAuroraTheme()` sudah ada sejak Batch 308, 0 API baru).

**Belum diverifikasi visual di device** — kalau user coba pilih Aurora sekarang: MiniPlayerBar/
panel NowPlaying/bottom sheet/card Home-Library harus terlihat punya rim tipis ber-gradasi
hijau→teal→ungu→magenta di tepinya, BUKAN lagi garis flat netral. 5 identitas lain harus 0
berubah.

**🎉 Cakupan Aurora yang dikonfirmasi user di awal (Batch 306) SEKARANG SELESAI PENUH** — mode
gelap terkunci (306), ambient background `auroraGlow()` (306, wired 308), color/typography/shape
murni sendiri (307/308/309), rim-glow per-panel wired app-wide (batch ini). **Item berikutnya
(belum diminta user, JANGAN dikerjakan diam-diam)**: animasi rim-glow (lihat "keputusan disengaja:
statis" di atas), atau tuning alpha/warna rim setelah terverifikasi visual device.

**Batch 309 (Tema ke-6 "Aurora", Fase 4/N — shape sendiri `AuroraShapes`, 1 file, permintaan
user langsung "lanjut sempurnakan shape murni nya!!")** — Lanjutan langsung dari Batch 308.
Menuntaskan item yang dicatat "belum diminta user" di entri Batch 308: shape Aurora, sekarang
diminta eksplisit. Dengan ini ke-6 identitas app punya color+typography+shape murni sendiri —
Aurora (color Batch 307, typography Batch 308, shape batch ini) menyusul 5 identitas lain yang
sudah lengkap sebelumnya.

**Dampak nyata mulai batch ini**: setiap panel/card/sheet yang pakai `Shapes.small/medium/large`
M3 saat Aurora aktif sekarang bersudut ASIMETRIS (bukan rounded-rect seragam seperti 5 identitas
lain) — 2 sudut diagonal (kiri-atas & kanan-bawah) lebih besar dari 2 sudut lainnya, mengikuti
arah alir `Brush.linearGradient()` `auroraGlow()` di baliknya. Ini pola shape PERTAMA di seluruh
project yang non-seragam per sudut.

**`Theme.kt`** (2 titik, 1 file) —
1. `AuroraShapes` ditambah setelah `LiquidGlassShapes` — mekanisme baru:
   `RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)` 4-parameter (bukan 1-parameter
   seragam yang dipakai 5 `*Shapes` lain di file ini). Rasional lengkap ada di comment block di
   atas definisinya sendiri (tidak diulang di sini): asimetri ditarik LANGSUNG dari arah diagonal
   default `Brush.linearGradient()` (topStart→bottomEnd, TANPA parameter `start`/`end` eksplisit
   di `auroraGlow()`) — sudut yang searah diagonal itu dapat radius lebih besar, sudut tegak
   lurus dapat radius lebih kecil. 0 token `Radius` baru ditambah ke `Spacing.kt` — nilai "besar"
   tiap tier (`xl`/`xxxl`/`liquidLg`) SENGAJA disamakan persis dgn puncak radius seragam
   LiquidGlass (bukan melebihinya) — keunikan murni dari asimetrinya, bukan dari rekor angka baru.
2. `shapes = when (identity)` dapat cabang baru `ThemeIdentity.AURORA -> AuroraShapes`, comment
   diperbarui: `else -> AppleShapes` sekarang murni cabang APPLE saja (satu-satunya identitas
   tanpa cabang eksplisit di typography MAUPUN shapes).

**Ringkasan file**: 1 file kode (`Theme.kt`) — jauh di bawah batas Micro-Batch (maksimal 3). 0
file baru, 0 dependency baru, 0 token `Radius` baru. `FILE_MANIFEST.txt` tidak berubah (187/187).
Brace/paren `Theme.kt` diverifikasi seimbang utuh: 14/14 braces, 198/198 parens.

**Belum divalidasi compile Gradle sungguhan** — WAJIB cek CI. Risiko sintaks rendah (`RoundedCornerShape`
4-parameter adalah konstruktor resmi Compose, type-compatible penuh dgn `CornerBasedShape`/`Shapes`
M3, 0 call site di luar `Theme.kt` berubah), TAPI risiko VISUAL lebih tinggi dari batch shape
sebelumnya — ini genuinely mekanisme baru (asimetri per-sudut), bukan cuma tuning angka radius
seragam yang sudah terbukti aman di 5 identitas lain.

**Belum diverifikasi visual di device** — kalau user coba pilih Aurora sekarang: Card/Sheet/dialog
harus terlihat bersudut "condong" (2 sudut diagonal lebih membulat dari 2 lainnya), BUKAN rounded
rect simetris biasa. Kalau ternyata terbaca sebagai bug/aneh di komponen tertentu (bukan efek yang
diinginkan), itu feedback berharga untuk sesi berikutnya — belum ada laporan device sejauh ini. 5
identitas lain harus 0 berubah.

**Item berikutnya (belum diminta user, JANGAN dikerjakan diam-diam)**: rim-glow per-panel (status
"ditunda" sejak Batch 306, BUKAN dibatalkan) — sekarang satu-satunya sisa item Aurora yang belum
dikerjakan dari cakupan awal yang dikonfirmasi user.

**Batch 308 (Tema ke-6 "Aurora", Fase 3/N — wiring `auroraGlow()` ke root Surface + typography
sendiri, 3 file, permintaan user langsung "lanjutkan fase 3/N, sempurnakan juga
typography-nya")** — Lanjutan langsung dari Batch 307. Item yang tercatat "Fase 3, BELUM
dikerjakan" di entri Batch 307 di bawah sekarang tuntas, PLUS 1 item tambahan yang eksplisit
diminta user batch ini (typography) — sebelumnya cuma dicatat "kalau diminta nanti".

**Dampak nyata mulai batch ini**: pilih Aurora di Settings sekarang menampilkan animasi warna
mengalir pelan (hijau→teal→ungu→magenta, 20 detik/arah, bolak-balik) di ambient background —
sebelumnya (Batch 306-307) tampilan flat statis. Judul & label juga sudah pakai tipografi
sendiri (bukan lagi reuse AppleTypography) — bobot huruf lebih ringan dari 5 identitas lain
(Light/Normal, 0 slot Bold-tier), tracking lebih lebar (kicker `labelSmall` terlebar dari 6
identitas), line-height paling longgar. Shape MASIH fallback `AppleShapes` — belum diminta user.

**`TactileDepth.kt`** — 0 baris disentuh batch ini; `auroraGlow()` sudah lengkap sejak Batch 306
(fase 1), batch ini murni soal MEMANGGILNYA dari root Surface.

**`MainActivity.kt`** (protected/parsial, 2 titik) — (1) import `auroraGlow` ditambah setelah
import `calmGrain`; (2) 1 `.then()` baru ditambah setelah blok `.then()` `calmGrain()` di root
`Surface`, pola arsitektur identik (aktif hanya saat `appThemeIdentity == ThemeIdentity.AURORA`).
0 baris lain di file (1277 baris) disentuh — sesuai status protected, cuma titik yang memang jadi
target fase 3 sejak dicatat di Batch 307.

**`Theme.kt`** (2 titik) — (1) `isAuroraTheme()` ditambah setelah `isLiquidGlassTheme()`, pola
identik 4 helper sebelumnya; (2) `typography = when (identity)` dapat cabang baru
`ThemeIdentity.AURORA -> AuroraTypography`. `shapes = when (identity)` SENGAJA TIDAK disentuh —
user cuma minta typography batch ini, bukan shape; comment ditambah di titik itu supaya sesi
berikutnya tahu ini keputusan sadar, bukan celah lupa.

**`Type.kt`** (1 definisi baru) — `AuroraTypography`, 5 slot (titleLarge/titleMedium/bodyMedium/
bodySmall/labelSmall, pola sama 5 identitas lain — belum menambal slot M3 baru seperti
LiquidGlass Batch 298). fontSize/fontFamily identik ke semua identitas lain (28/17/15/13/11sp,
`FontFamily.Default`) — larangan Monospace Batch 133 §4 tetap berlaku, size tidak diubah supaya
0 resiko reflow layout. 3 sumbu pembeda (rasional lengkap ada di comment block `Type.kt` sendiri,
tidak diulang di sini): (1) WEIGHT — Light/Normal, SATU-SATUNYA dari 6 identitas dengan 0 slot
Bold-tier; (2) letterSpacing — positif/terbuka, `labelSmall` 1.4sp jadi REKOR terlebar (melewati
CalmRetro 1.2sp); (3) lineHeight — dilonggarkan dari baseline Apple, ditujukan supaya animasi
`auroraGlow()` tidak terasa terpotong baris teks.

**Ringkasan file**: 3 file kode (`MainActivity.kt`, `Theme.kt`, `Type.kt`) — PAS di batas
Micro-Batch (maksimal 3). 0 file baru, 0 dependency baru. `FILE_MANIFEST.txt` tidak berubah
(187/187 — hitungan file, semua 3 file yang disentuh sudah ada sebelumnya). Brace/paren
diverifikasi seimbang: `Theme.kt` 179/179 parens + 14/14 braces (utuh), `MainActivity.kt`
617/617 parens + 256/256 braces (utuh), `Type.kt` 45/45 parens kode-saja (di luar prosa
komentar, yang wajar tidak selalu simetris — sama seperti file lain di project ini).

**Belum divalidasi compile Gradle sungguhan** — **WAJIB cek CI**. Risiko rendah: 2 dari 3 file
cuma nambah 1 cabang `when`/1 `.then()` baru ke pola yang sudah terbukti jalan di 5 identitas
lain; `Type.kt` murni `val` baru, 0 titik lain di app yang referensinya berubah.

**Belum diverifikasi visual di device** — kalau user coba pilih Aurora sekarang: ambient
background harus mulai terlihat mengalir pelan (bukan lagi flat), judul/label harus terasa lebih
ringan/lapang dibanding 5 tema lain. 5 identitas lain harus 0 berubah.

**Item berikutnya (belum diminta user, JANGAN dikerjakan diam-diam)**: shape Aurora sendiri
(masih fallback `AppleShapes`), rim-glow per-panel (status "ditunda" sejak Batch 306, BUKAN
dibatalkan) — sama aturan yang sudah berlaku sejak fase 1: tunggu instruksi eksplisit.

**Batch 307 (Tema ke-6 "Aurora", Fase 2/N — registrasi identitas + palet lengkap, 2 file)** —
Lanjutan langsung dari Batch 306 ("next"). Fase ini: `ThemeIdentity.AURORA` resmi didaftarkan ke
enum + `AuroraColors` (`darkColorScheme`) lengkap + wire `colorsFor()`. Ini WAJIB dikerjakan
sekaligus (bukan dicicil lagi) krn `colorsFor()` pakai `when` **exhaustive** tanpa `else` —
begitu 1 entry enum ditambah, Kotlin memaksa SEMUA cabang terisi di commit yang sama atau
gagal compile.

**Dampak nyata mulai batch ini**: Aurora **SUDAH BISA DIPILIH** di Settings (picker
`SettingsScreen.kt` iterasi `ThemeIdentity.entries.toList()` — otomatis generik, 0 baris
disentuh di file itu) — pilih warna dark-lock sendiri (hijau vivid + night-navy), TAPI **BELUM**
menampilkan animasi `auroraGlow()` (fase 3, MainActivity.kt) — layar akan terlihat flat gelap +
aksen hijau statis dulu kalau dicoba sekarang. Typography/shapes Aurora jatuh ke `else ->
AppleTypography`/`else -> AppleShapes` (pola bootstrap sama persis semua identitas lain dulu —
Skeu 57→305, Calm Retro 130→302 — dimurnikan belakangan, bukan lupa).

**`Color.kt`** (5 token baru) — `AuroraBackground` (0x05080C), `AuroraSurface` (0x0B1015),
`AuroraSurfaceVariant` (0x161D22), `AuroraText` (0xE7F3EC, nyaris putih+sentuhan hijau tipis),
`AuroraSecondaryText` (0x7E8C90). Hex sengaja beda dari base near-black identitas lain
(AmoledSurface/CalmRetroBackground/LiquidGlassDarkBackground) — wajar sedikit mirip krn semua
"near-black", pembeda asli tetap di overlay `auroraGlow()` (Batch 306), bukan di sini.

**`Theme.kt`** (3 titik) — (1) enum `AURORA("aurora", "Aurora", ...)` ditambah setelah
`LIQUID_GLASS`; (2) `private val AuroraColors = darkColorScheme(...)` ditambah setelah
`LiquidGlassLightColors` — 1 set saja (bukan pasangan Dark/Light) krn terkunci gelap permanen,
pola sama `CalmRetroColors`. `onPrimary`/`onTertiary` = Color.Black, luma dihitung & dicatat di
komentar (AuroraAccent ≈0.75, AuroraTeal ≈0.66 — jauh di atas ambang 0.55 yang dipakai identitas
lain). `error = AuroraMagenta` — SENGAJA derivasi dari palet Aurora sendiri (bukan token identitas
lain, bukan juga hardcode merah generik ala `LiquidGlassDarkColors`), menerapkan pelajaran Batch
130 eksplisit ("100% derivasi dari palet sendiri"); (3) `colorsFor()` dapat cabang baru
`ThemeIdentity.AURORA -> AuroraColors`, isDark diabaikan (komentar menjelaskan, pola sama
CALM_RETRO).

**Diperiksa sebelum eksekusi (bukan asumsi)**: grep seluruh app utk `when (identity)`/
`when (appThemeIdentity)` — cuma `colorsFor()` yang exhaustive tanpa `else` (WAJIB disentuh).
`typography`/`shapes` dispatcher Theme.kt, `identityRootBrush`/`navCatchLightColor`
MainActivity.kt, dan `ThemeOptionCard` SettingsScreen.kt semuanya pakai `else`/`==` biasa — 0
disentuh, 0 resiko compile break, Aurora otomatis jatuh ke fallback aman di semua itu.

**Ringkasan file**: 2 file kode (`Color.kt` + `Theme.kt`), di bawah batas Micro-Batch. 0 file
baru, 0 dependency baru. `FILE_MANIFEST.txt` tidak berubah (187/187). 0 protected asset disentuh
— `MainActivity.kt`/`SettingsScreen.kt` BELUM disentuh (fase 3). Brace/paren diverifikasi
seimbang penuh per-file (`Color.kt`: 243/243 parens, 0/0 braces; `Theme.kt` utuh: 14/14 braces,
168/168 parens).

**Belum divalidasi compile Gradle sungguhan** — **WAJIB cek CI**, resiko sedikit lebih tinggi
dari Batch 306 krn `colorsFor()` exhaustive-when adalah titik yang PALING gampang salah (lupa 1
cabang = compile error total, bukan cuma 1 fitur rusak) — sudah di-double-check manual (grep di
atas) tapi tetap WAJIB dikonfirmasi CI, bukan diasumsikan aman krn "kelihatan benar".

**Belum diverifikasi visual di device** — kalau user coba pilih Aurora sekarang: swatch di
picker Settings harus tampil hijau vivid di atas lingkaran night-navy gelap, TANPA animasi
apa pun (itu memang belum dipasang, bukan bug). 4 identitas lain harus 0 berubah.

**Item berikutnya (Fase 3, BELUM dikerjakan)**: `isAuroraTheme()` helper, wire `auroraGlow()` ke
root Surface `MainActivity.kt` (protected/parsial, target 1 baris pola sama `calmGrain()`),
deskripsi tambahan/typography-shape sendiri kalau diminta nanti. Rim-glow per-panel: masih
ditunda.

**Batch 306 (Tema ke-6 "Aurora", Fase 1/N — fondasi mekanisme + palet, 2 file, permintaan user
langsung)** — User minta eksplisit: "bikin theme ke-6, tapi murni 100% karya hasil ide sendiri
tanpa contek gaya desain visual apapun". Sebelum kode: pitch 3+3 konsep orisinal (Ink Wash,
Paper-fold, Circuit Trace, lalu Woven, Contour, **Aurora** — dipilih user) lewat
`ask_user_input_v0`, lalu 2 keputusan arsitektur dikonfirmasi user via tool yang sama sebelum
eksekusi: (1) mode terkunci **GELAP PERMANEN** (bukan otonom 2 mode), (2) cakupan efek **ambient
background saja dulu** (rim-glow per-panel eksplisit ditunda utk dipertimbangkan lagi nanti,
bukan dibatalkan).

**Kenapa dipecah jadi fase, bukan 1 batch langsung jadi** — nambah identitas tema baru itu
sekelas Liquid Glass dulu (Batch 279-301+, bukan micro-task), dan `colorsFor()` di `Theme.kt`
pakai `when` **exhaustive** (bukan `when`+`else` seperti dispatcher typography/shapes) — begitu
1 entry `ThemeIdentity` baru ditambah, SEMUA cabang (warna/typography/shapes) wajib terisi
SEKALIGUS di batch yang sama, jadi pola aman (persis histori Liquid Glass — enum-nya sendiri
baru didaftarkan fase 2, `isLiquidGlassTheme()` malah baru Batch 281/fase 3) adalah: bangun
mekanisme+token dulu secara terisolasi (0 pemakaian di luar file definisi, 0 perubahan visual),
BARU daftarkan `ThemeIdentity.AURORA` + `colorsFor()` sekaligus di fase berikutnya.

**Mekanisme baru, genuinely orisinal** — bukan reuse `skeuEmboss()`/`tactileEmboss()` (shadow/
bevel), `calmScanlines()`/`calmGrain()` (artefak retro), atau `hazeEffect()` (blur asli): Aurora
dapat kedalaman dari **warna yang mengalir** (animated hue-shift), mekanisme yang belum pernah
ada di app ini. 5 titik stop gradien TETAP di posisi (0/0.22/0.48/0.74/1.0) — sengaja BUKAN
menggeser posisi stop (resiko 2 stop bertabrakan di ujung 0f/1f) — 3 stop tengah warnanya
di-`lerp()` antar 2 hue aurora bersebelahan seiring `phase` (`rememberInfiniteTransition` +
`animateFloat`, 0f↔1f, `RepeatMode.Reverse`, 20 detik satu arah/~40 detik siklus penuh — pola
animasi yang SAMA PERSIS dgn `ShimmerBrush()` di `LibraryScreen.kt`, sudah terbukti compile+jalan
di app ini, bukan API baru). Stop pertama/terakhir tetap `Color.Transparent` permanen supaya wash
berbaur ke tepi kanvas.

**`TactileDepth.kt`** (1 fungsi baru + 8 import baru) — `Modifier.auroraGlow()` ditambah di akhir
file (setelah `calmGrain()`), 0 dipanggil dari mana pun sampai batch ini (0 call site). Import
baru: `LinearEasing`, `RepeatMode`, `animateFloat`, `infiniteRepeatable`,
`rememberInfiniteTransition`, `tween` (semua dari `androidx.compose.animation.core`, pola sama
persis `LibraryScreen.kt`'s `ShimmerBrush()`) + `androidx.compose.ui.graphics.lerp`.
`androidx.compose.foundation.background` sudah diimpor sebelumnya (dipakai `this.background(brush)`
di akhir fungsi), 0 import baru utk itu.

**`Color.kt`** (6 token baru) — `AuroraAccent`/`AuroraGreen` (hijau vivid 0x3DE8A0, calon
`primary` + dasar `isAuroraTheme()` fase nanti), `AuroraTeal` (0x2BC9C9), `AuroraViolet`
(0x7C6FE0), `AuroraMagenta` (0xD46FC7), `AuroraGlowAlpha` (0.34f, titik awal — WAJIB dituning
ulang stlh tampil di device, pola sama semua tuning ambient lain di file ini). 4 hue ditarik dari
spektrum aurora borealis asli (hijau dominan → teal → ungu → magenta), sengaja dijaga beda dari
5 aksen tema lain (hijau JAUH lebih vivid drpd CalmRetroAccent Muted Sage yg sengaja pudar; ungu
beda hue dari TactileAccent & LiquidGlassAccent) — dipakai hanya sbg ingredient gradien
alpha-rendah jadi 0 resiko tabrakan visual langsung.

**Ringkasan file**: 2 file kode (`TactileDepth.kt` + `Color.kt`), di bawah batas Micro-Batch. 0
file baru, 0 dependency Gradle baru (semua import dari `androidx.compose.animation.core`/
`androidx.compose.ui.graphics` sudah tersedia lewat Compose BOM yang sudah dipakai). 0 protected
asset disentuh — `Theme.kt`/`MainActivity.kt` BELUM disentuh sama sekali batch ini (fase
berikutnya). `FILE_MANIFEST.txt` tidak berubah (187/187). Brace/paren blok yang ditambahkan
diverifikasi seimbang penuh per-file (`Color.kt`: 231/231 parens, 0/0 braces; `TactileDepth.kt`
utuh: 33/33 braces, 275/275 parens).

**Belum divalidasi compile Gradle sungguhan** (0 akses jaringan sesi ini) — **WAJIB cek CI**,
risiko sedikit lebih tinggi dari batch typography murni krn ini FUNGSI baru (bukan cuma data
`TextStyle`/`Typography`), tapi pola animasinya di-copy 1:1 dari `ShimmerBrush()` yang sudah
terbukti jalan, jadi risiko tetap rendah-menengah, bukan tinggi.

**Item berikutnya (Fase 2, BELUM dikerjakan)**: daftarkan `ThemeIdentity.AURORA` ke enum +
`AuroraBackground`/`AuroraSurface`/`AuroraText` dkk token tambahan + `AuroraColors =
darkColorScheme(...)` (`Theme.kt`) + wire `colorsFor()` — WAJIB sekaligus krn exhaustive `when`.
**Fase 3+ (belum)**: `isAuroraTheme()` helper, wiring `auroraGlow()` ke root Surface
`MainActivity.kt` (protected/parsial, target diagnostik: 1 baris `.then(if (identity==AURORA)
Modifier.auroraGlow() else Modifier)`, pola sama `calmGrain()`), typography/shape (boleh mulai
reuse Apple dulu spt tema lain, dimurnikan belakangan), deskripsi + entry picker `SettingsScreen.kt`.
Rim-glow per-panel: **ditunda**, dipertimbangkan lagi setelah fondasi ambient ini terverifikasi
di device.

**Batch 305 (Perkuat typography khusus tema Neumorphism, murni 100% — permintaan user langsung,
2 file)** — User minta eksplisit: "sempurnakan typography Neumorphism 100% murni, tuntas!!" —
melanjutkan pola penguatan typography per-identitas (Batch 298 Liquid Glass, Batch 302 Calm
Retro), sekarang giliran Neumorphism (`SKEU_DARK_LITE`), TERAKHIR dari 5 identitas yang masih
reuse `AppleTypography` lewat cabang `else` dispatcher `Theme.kt` (Batch 302 sengaja tidak
menyentuhnya krn user waktu itu cuma minta Calm Retro). Sesudah batch ini, ke-5 identitas
(Apple/Tactile/Skeu/CalmRetro/LiquidGlass) semuanya punya `Typography()` murni sendiri — 0 yang
tersisa jatuh ke `else`.

**`Type.kt`** (1 titik baru, additif) — `SkeuTypography` ditambah di akhir file, mengisi 5 slot
yang sama seperti identitas lain (`titleLarge`/`titleMedium`/`bodyMedium`/`bodySmall`/
`labelSmall`) — mengganti isi, bukan menambal lubang slot M3 baru (5 slot itu sudah terdefinisi
lewat reuse Apple sebelumnya, pola sama Batch 302, beda dari kasus Liquid Glass Batch 298).

3 sumbu pembeda, ditarik langsung dari mekanisme `skeuEmboss()` (`TactileDepth.kt`) dan spec
identitas ini (Batch 79 — dual soft-shadow, 0 border/0 grain, "dipahat dari material yang sama
dengan kanvas"), bukan angka acak:
1. **Weight satu tingkat lebih ringan dari Apple** di tiap slot berjenjang (Bold->SemiBold,
   SemiBold->Medium) — kebalikan Tactile (naik ke ExtraBold/Bold, "machined label" fisik). Filosofi
   Skeu: kedalaman murni dari bayangan, bukan dari kontras tinta tebal — huruf tebal akan terbaca
   seperti "dicetak di atas" permukaan (metafora 4 identitas lain), bertentangan dengan "molded"
   yang jadi ciri Skeu. Satu-satunya dari 5 identitas yang lebih ringan dari baseline Apple.
2. **letterSpacing datar 0.sp di semua 5 slot** — tidak ada dorongan gaya tracking sama sekali,
   beda dari 4 identitas lain yang masing-masing punya arah tracking sendiri. Perpanjangan
   langsung dari ciri paling literal identitas ini ("0 border, 0 tekstur grain — kedalaman murni
   dari bayangan"): permukaan dilucuti dari semua gaya selain bayangan, huruf ikut dilucuti dari
   gaya tracking.
3. **lineHeight paling longgar dari 5 identitas** (lebih dari Calm Retro yang sudah dilonggarkan
   dari Apple) — mencerminkan panel Skeu yang lembut/empuk tanpa sudut/border tegas.

`fontFamily` tetap `FontFamily.Default` (sans) di kelima slot — larangan Monospace Batch 133 §4
(HANYA 2 Text durasi/waktu Now Playing) tetap berlaku, tidak dilonggarkan. `fontSize` dipertahankan
identik ke 4 identitas lain — hindari risiko reflow/wrap layout terpisah, di luar scope permintaan
"typography" (pola sama Batch 279/298/302).

**`Theme.kt`** (1 titik) — blok `when (identity)` dispatcher `typography` dapat 1 cabang baru:
`ThemeIdentity.SKEU_DARK_LITE -> SkeuTypography`, ditambahkan sebelum `else -> AppleTypography`.
`APPLE` tetap satu-satunya yang jatuh ke `else` (memang benar identitasnya sendiri). Komentar block
di atas dispatcher diperbarui menyebut Batch 305 & histori keputusan Batch 57/279/302.

**Cakupan otomatis app-wide** — dispatch terjadi 1 titik di `MaterialTheme(typography=...)` level
root `AudioPlayerTheme()`, jadi setiap composable yang sudah memanggil
`MaterialTheme.typography.titleLarge/titleMedium/bodyMedium/bodySmall/labelSmall` di seluruh app
otomatis ikut `SkeuTypography` begitu identitas Neumorphism aktif — 0 call site UI individual
perlu diedit satu-satu (pola sama seperti 4 identitas lain). Live-preview swatch `ThemeOptionCard`
(`SettingsScreen.kt`) ikut otomatis benar tanpa disentuh (memanggil `MaterialTheme.typography`
langsung, pola sama Batch 128-131/302).

**Ringkasan file**: 2 file kode (`Type.kt` + `Theme.kt`), di bawah batas Micro-Batch (maks 3 file
kode). 0 file baru, 0 dependency baru, 0 import baru (`Typography`/`TextStyle`/`FontFamily`/
`FontWeight`/`sp` sudah diimpor `Type.kt` sejak awal). `FILE_MANIFEST.txt` tidak berubah (187/187).
0 protected asset disentuh. Brace/paren blok yang ditambahkan diverifikasi seimbang (`Type.kt`:
27/27 parens, 0/0 braces — murni deklarasi `val`+`TextStyle`; `Theme.kt` utuh: 14/14 braces,
152/152 parens).

**Belum divalidasi compile Gradle sungguhan** (0 akses jaringan sesi ini, pola sama tiap batch) —
**WAJIB cek CI setelah push**, walau risiko rendah (1 `val Typography(...)` baru murni data class +
1 cabang `when` tambahan, pola persis sama seperti `CalmRetroTypography` Batch 302 yang sudah
terbukti compile bersih).

**Belum diverifikasi visual di device** — prioritas cek kalau user build ulang: (1) pilih
Neumorphism di Settings, judul/label/body app terasa lebih ringan bobotnya (SemiBold/Medium,
bukan Bold/SemiBold ala Apple) & tracking netral/rapat (bukan lebar ala Calm Retro), (2) baris
teks terasa lebih longgar/lega dari 4 identitas lain, (3) 4 identitas lain (Apple/Tactile/
CalmRetro/LiquidGlass) visualnya TIDAK berubah sama sekali dari sebelum batch ini.

**Batch 304 (Fix laporan bug lewat screenshot — cold-start notification: teks statis + tombol
Jeda kepatri, 1 file kode)** — Laporan ad-hoc user (screenshot notifikasi "SONIX" ongoing), BUKAN
lanjutan antrean "Sisa antrean Micro-Polish Terakhir" di bawah (item 2-6 itu TETAP menunggu,
tidak tersentuh batch ini — laporan bug baru selalu interupsi, sesuai pola batch-batch
sebelumnya). Screenshot menunjukkan kartu notifikasi ongoing "SONIX" dgn `contentText` "Memuat
lagu…" dan action button "Jeda" — gaya visual kartu (header waktu terpisah dari body, bentuk
pill button) cocok skin OEM ala MIUI/HyperOS.

**Diagnosis:** grep string persis dari screenshot ("Memuat lagu") ketemu SATU titik:
`buildColdStartNotification()` di `PlaybackService.kt` — notifikasi placeholder yang tampil
SANGAT SINGKAT saat widget home-screen ditekan sebelum proses app ada (cold start), sebelum
notifikasi Media3 asli (MediaStyle, auto-sync) mengambil alih. Dua akar masalah berbeda,
sama-sama di fungsi builder yang sama:
1. **Teks statis** — `.setContentText("Memuat lagu…")` HARDCODE, tidak pernah baca state/
   metadata apa pun, bahkan setelah lagu confirmed playing (`isPlaying=true` & `currentMediaItem`
   sudah terisi di titik itu). User selalu lihat teks yang sama walau lagu sudah jalan.
2. **Tombol "Jeda" kepatri** — mekanisme update SEBENARNYA sudah ada (`onIsPlayingChanged` ->
   `updateColdStartNotification(isPlaying)` kalau `coldStartNotificationActive`, ditambahkan
   batch sebelumnya persis untuk kasus ini — lihat komentar existing di kode). TAPI
   implementasinya cuma `NotificationManagerCompat.notify()` polos ke ID yang sama. Notifikasi
   placeholder ini SENGAJA bukan `MediaStyle` (`Media3` tidak ikut sinkronkan otomatis — dicatat
   eksplisit di komentar lama), dan sejumlah skin OEM (cocok gaya screenshot user) diketahui
   menahan cache action-button utk notifikasi foreground non-MediaStyle, tidak selalu redraw dari
   `notify()` ulang walau `Notification` baru sudah dikirim dgn label berbeda.

**Fix (`PlaybackService.kt`, 1 file):**
- `buildColdStartNotification()` — parameter baru `nowPlayingTitle: String? = null` (default null,
  jadi call site lama di `startForegroundColdStartNotification()` — yang memang selalu belum
  punya media item persis di titik itu — TIDAK perlu diubah, 0 risiko regresi di jalur itu).
  `contentText` sekarang `when`: judul lagu kalau tersedia (`isPlaying` -> judul polos; `!isPlaying`
  -> "{judul} — Dijeda"), fallback "Memuat lagu…" HANYA kalau benar-benar belum ada apa pun
  diketahui (window cold-start murni, sebelum media item pertama termuat).
- `updateColdStartNotification()` — ganti `NotificationManagerCompat.notify()` polos jadi
  `startForeground()` ulang (pola SDK-check API 29+ `ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_
  PLAYBACK` disalin persis dari `startForegroundColdStartNotification()` di bawahnya, supaya
  konsisten). `startForeground()` ulang adalah cara resmi Android utk memperbarui notifikasi
  service foreground miliknya sendiri — idempotent, tidak me-restart service/bikin flicker —
  dan tidak bergantung pada asumsi soal cache OEM tertentu buat berhasil refresh, jadi perbaikan
  ini valid terlepas dari root cause persis quirk OEM-nya benar dugaan di atas atau bukan. Juga
  ambil `mediaSession?.player?.currentMediaItem?.mediaMetadata?.title?.toString()` di titik ini
  (pola sama persis `pushWidgetUpdate()` di file yang sama) buat dioper ke `nowPlayingTitle`.

0 import baru (`ServiceInfo`/`NotificationCompat`/`NotificationManagerCompat` semua sudah ada di
file), 0 file baru, 0 dependency baru — `FILE_MANIFEST.txt` tidak berubah (187/187). 0 protected
asset disentuh (di dalam `PlaybackService.kt`, protected utk EDIT PARSIAL — perubahan ini murni
2 fungsi terisolasi, bukan sentuh lifecycle/session/manifest-nya). Brace/paren file diverifikasi
seimbang (81/81 `{}`, 373/373 `()`). **Belum divalidasi compile Gradle sungguhan** (0 akses
jaringan sesi ini, pola sama tiap batch) — **WAJIB cek CI setelah push**. **Belum diverifikasi
visual di device asli** (terutama device OEM yang cocok screenshot user) — prioritas cek: matikan
app total, tekan tombol play di widget home-screen, amati notifikasi "SONIX" muncul: (1)
`contentText` berubah dari "Memuat lagu…" jadi judul lagu begitu lagu mulai jalan, (2) tombol
berganti label "Jeda" (bukan lagi kepatri), (3) tekan tombol itu, konfirmasi label balik jadi
"Lanjutkan" — kalau OEM tertentu masih tidak refresh walau sudah pakai `startForeground()` ulang,
itu sinyal root cause bukan (cuma) cache action-button seperti dugaan, perlu investigasi lanjut
sesi berikutnya (kemungkinan lain: migrasi placeholder ini ke `MediaStyle` sungguhan, TAPI itu
scope lebih besar dari micro-fix ini — sengaja tidak dicoba di batch ini).

**Batch 303 (Micro-Polish Terakhir 1/6 — overflow title/artist/album, 3 file kode + planning
aksesibilitas 0 kode)** — User kirim daftar 6 item "MICRO-POLISH TERAKHIR" dalam 1 pesan (lihat
"Sisa antrean" di bawah), item terakhir (Aksesibilitas) ditandai eksplisit "(planning first, zero
code)" oleh user sendiri — 5 item lain TIDAK ditandai begitu, jadi dieksekusi kode sungguhan.
**1 batch = 1 task** (aturan Micro-Batch) berarti 6 item ini TIDAK muat 1 batch — dipilih item
#1 (overflow) sbg kode batch ini, sisa 5 masuk antrean eksplisit di bawah, BUKAN diabaikan.

**Asumsi yang diambil (didokumentasikan, bukan ditanya balik — instruksi user "gak usah denial
segala macem"):** item #4 user tulis "Dark/Light/**Matte Noir**" — "Matte Noir" bukan identitas
yang ada sekarang (grep: 0 hit di `enum class ThemeIdentity` aktif; nama itu tema custom LAMA yg
sudah dihapus total & diganti "Tactile" sejak Batch 49, riwayat lengkap ada di
`PROJECT_STATE_ARCHIVE.md`). Diasumsikan maksud user: cek konsistensi warna/surface di mode
Dark/Light SEMUA identitas aktif + fondasi AMOLED near-black Tactile (`#030508`, kandidat plesetan
paling dekat ke "matte noir" di kode SEKARANG) — BUKAN membangkitkan lagi tema lama yang sudah
sengaja dihapus. Kalau salah tangkap, koreksi user ditunggu sebelum item #4 dieksekusi (item itu
sendiri belum masuk kode batch ini, jadi 0 risiko salah arah kepatri ke kode).

**Item #1 — overflow title/artist/album (`TextOverflow`, 3 file):** metodologi: grep pola
`Text(` yang render field title/artist/album app-wide (`app/src/main/java/com/rudi/audioplayer/
ui/*.kt`), 1-per-1 titik yang lolos filter otomatis diperiksa manual (auto-filter sempat false-
positive di beberapa: `AlertDialog` body message LibraryScreen.kt — wajar wrap multi-baris utk
kalimat konfirmasi, BUKAN bug; label statis "SEDANG DIPUTAR"/"Potong Nada Dering" — string tetap,
0 risiko overflow). 3 celah nyata ketemu & diperbaiki:
- `LibraryScreen.kt` (2 titik, hasil pencarian tab "Lainnya"): `ListItem` baris "Artis" & "Album"
  di daftar hasil cari — keduanya `Text(artist)`/`Text(album)` POLOS 0 `maxLines`/`overflow`,
  nama panjang bisa wrap tak terbatas & bikin tinggi row ListItem tidak rata dgn baris lain.
  Ditambah `maxLines = 1, overflow = TextOverflow.Ellipsis` (pola sama persis dgn `song.artist`
  di `SongRow` bawahnya di file yang sama — bukan pola baru).
- `RingtoneCutterSheet.kt` (1 titik + 1 import baru): `song.title` di header sheet SUDAH punya
  `maxLines = 1` tapi 0 `overflow` — default Compose diam-diam jatuh ke `TextOverflow.Clip`
  (kepotong mentah tanpa "…", bukan ellipsis rapi). Ditambah `overflow = TextOverflow.Ellipsis`.
- `StatsDashboardScreen.kt` (1 titik + 1 import baru): baris ranking "Artis Paling Sering" —
  `artistCount.artist` pakai `Modifier.weight(1f)` tapi 0 `maxLines`/`overflow`, nama artis
  panjang bisa wrap 2+ baris & merusak alignment vertikal terhadap nomor rank & play-count di
  kanan-kirinya (yang tetap 1 baris). Ditambah `maxLines = 1, overflow = TextOverflow.Ellipsis`.

`NowPlayingScreen.kt` (title/artist hero) dicek juga — SUDAH benar sejak lama (`maxLines=1` +
`basicMarquee()` utk title, `maxLines=1` + `Ellipsis` utk artist, pola identik `SongRow`), 0
perubahan diperlukan di situ (auto-filter sempat flag baris label "SEDANG DIPUTAR" di atasnya,
false positive, sudah diverifikasi manual).

**Planning Aksesibilitas (0 kode, sesuai instruksi eksplisit user):** 2 pengecekan grep-heuristik
dijalankan app-wide (`app/src/main/java/com/rudi/audioplayer/ui/*.kt`): (1) `IconButton`/
`IconToggleButton` dgn `contentDescription = null` di dalamnya (icon aksi tanpa label TalkBack)
— **0 ditemukan**; (2) touch-target di bawah 48dp yg menempel `.clickable`/`IconButton` — 4 hit
awal, SEMUA false positive setelah diperiksa manual (itu ukuran GLYPH icon di dalam kontainer
`IconButton` 48dp default, bukan ukuran area sentuhnya — pola yg sama persis sudah diaudit &
didokumentasikan eksplisit Batch 141/224/226/229/231, seri "Iconography 1/7" s.d. "7/7" yang
disebut berulang di komentar `NowPlayingScreen.kt`). Kesimpulan sementara: 2 kategori ini
kemungkinan besar SUDAH beres dari seri audit lama, bukan celah baru — TAPI ini BARU 2 dari
sekian sub-area aksesibilitas yang diminta ("disabled state" belum diperiksa sama sekali,
`contentDescription` utk elemen non-`Icon` seperti `Image`/artwork juga belum). **Belum planning
lengkap** — sisa sub-area masuk giliran item #6 di antrean bawah, BUKAN ditutup dini di batch ini.

**Sisa antrean Micro-Polish Terakhir (permintaan user, urutan sesuai pesan asli):**
2. Empty/error/loading state — konsisten di seluruh screen (belum diaudit sama sekali).
3. Dialog/Sheet — perilaku back button, tap-di-luar, dan tap-berulang (belum diaudit).
4. Dark/Light/"Matte Noir" — 0 inkonsistensi surface/warna (lihat asumsi di atas; belum diaudit).
5. Animation — 0 transisi yang terasa lambat/berlebihan (belum diaudit).
6. Aksesibilitas — lanjutan planning (baru 2/banyak sub-area tercek, lihat paragraf di atas),
   TETAP zero-code sampai user konfirmasi hasil planning lengkap & minta lanjut ke eksekusi kode.

3 file kode (pas batas Micro-Batch), 2 import baru (`TextOverflow` — package sudah ada di
dependency, 0 dependency baru), 0 file baru — `FILE_MANIFEST.txt` tidak berubah (187/187). 0
protected asset disentuh. Brace/paren ketiga file diverifikasi seimbang. **Belum divalidasi
compile Gradle sungguhan** (0 akses jaringan sesi ini) — **WAJIB cek CI setelah push**, risiko
rendah (parameter `Text()` + 1 import standar, bukan API/dependency baru). Detail lengkap:
`CHANGELOG.md` Batch 303.

**Batch 302 (Perkuat typography khusus tema Calm Retro, murni 100% — permintaan user langsung,
2 file kode)** — Menutup celah yang SENGAJA dibiarkan terbuka di Batch 130: waktu itu Calm Retro
"dipurifikasi" (tertiary/error/shape dilepas dari token pinjaman identitas lain, `CalmRetroShapes`
dibuat sendiri) TAPI typography-nya SENGAJA dibiarkan tetap reuse `AppleTypography` ("spec tidak
beri spesifikasi tipografi... bukan kebocoran identitas, beda kasus dari tertiary/error/shape").
User sekarang minta eksplisit dibalik — typography ikut murni jadi milik sendiri.

**`Type.kt`** (1 titik baru): `CalmRetroTypography` (5 slot, pola sama Apple/Tactile) ditambah di
akhir file. **fontFamily TETAP `FontFamily.Default`** di kelima slot — larangan eksplisit spec §4
(Batch 133: monospace HANYA boleh di 2 `Text` durasi/waktu Now Playing, dilarang ke judul/lirik)
masih berlaku, TIDAK dilonggarkan batch ini ("murni" = kurva weight/letterSpacing/lineHeight
sendiri, bukan migrasi ke monospace — itu akan membalik keputusan terdokumentasi, bukan
penguatan). Pembeda dari 3 identitas lain: (1) vs Apple — letterSpacing dibalik dari negatif/rapat
jadi positif/terbuka (+0.15sp s/d +1.2sp per slot), kesan mesin-ketik/label cetak vintage, sejalan
CRT-scanline+chromatic-aberration identitas ini; (2) vs Tactile (ExtraBold/Bold "machined label"
fisik) — Calm Retro TIDAK ikut naik ke tier itu, ditahan di Bold/SemiBold sama seperti Apple krn
identitas ini flat/opaque by design (Batch 130), pembeda murni dari spacing bukan weight; (3)
`labelSmall` (dipakai luas sbg kicker/eyebrow app-wide — "BERANDA"/"SEDANG DIPUTAR" dkk) dapat
lompatan tracking terbesar (0.6sp→1.2sp, 2x lipat) — titik paling terasa "retro" dari 5 slot;
(4) lineHeight tiap slot dilonggarkan sedikit dari padanan Apple (bukan dipadatkan) — "calm"
secara harfiah berarti ruang napas antar-baris lebih lega.

**`Theme.kt`** (1 titik): blok `when (identity)` dispatch `typography` — `ThemeIdentity.CALM_RETRO
-> CalmRetroTypography` ditambah eksplisit (sebelumnya jatuh ke `else -> AppleTypography`).
`SKEU_DARK_LITE`/`APPLE` TETAP jatuh ke `else -> AppleTypography` — TIDAK disentuh, di luar scope
permintaan user (cuma Calm Retro yang diminta). Komentar di atas blok itu diperbarui menyebut
Batch 302.

2 file kode (di bawah batas Micro-Batch), 0 file baru, 0 dependency baru — `FILE_MANIFEST.txt`
tidak berubah (187/187). 0 protected asset disentuh. Brace/paren kedua file diverifikasi seimbang.
Cakupan otomatis app-wide lewat `MaterialTheme.typography` — setiap composable yang sudah pakai
token M3 (`titleLarge`/`titleMedium`/`bodyMedium`/`bodySmall`/`labelSmall`) otomatis ikut
`CalmRetroTypography` saat identitas ini aktif, 0 call site UI perlu diedit satu-satu. Live-preview
swatch `ThemeOptionCard` (SettingsScreen) ikut otomatis (manggil `MaterialTheme.typography`
langsung, tidak disentuh — pola sama Batch 128-131). **Belum divalidasi compile Gradle sungguhan**
(0 akses jaringan sesi ini, pola sama tiap batch) — **WAJIB cek CI setelah push**, risiko rendah
(1 `val Typography(...)` baru + 1 cabang `when` tambahan, bukan API/dependency baru). **Belum
diverifikasi visual di device** — prioritas cek: pilih Calm Retro di Settings, judul/label/body di
seluruh app (Home/Library/NowPlaying/Settings) terasa beda dari Apple (tracking lebih terbuka,
kicker lebih lebar), teks waktu/durasi Now Playing TETAP monospace seperti sebelumnya (tidak
kebawa berubah), 4 identitas lain (Apple/Tactile/Skeu/Liquid Glass) visualnya TIDAK berubah sama
sekali (regresi urutan `when`). Detail lengkap: `CHANGELOG.md` Batch 302.

**Batch 301 (2 bug fix lanjutan dari feedback device: tab Library masih flat + stutter transisi
tab, 2 file kode)** — User laporkan lagi dalam 1 pesan: (1) tab Beranda **dan** tab Library masih
flat total; (2) stuttering pas **transisi antar tab** (beda dari "stutter scroll" Batch 300).

**Bug 1 (`LibraryScreen.kt`):** grep `isPanelTheme` app-wide (metodologi sama Batch 300) nemuin
LibraryScreen belum pernah diaudit — Batch 300 cuma HomeScreen/StatsDashboard. 1 gap identik
ketemu: bar "Urungkan" (isTactile/isSkeu ada, Liquid Glass jatuh else generik) — ditambah cabang
`isLiquidGlass -> Modifier.frostedGlass()`. Ini SATU-SATUNYA `Surface` di LibraryScreen — header/
chip filter/`SongRow` murni `Box`/`Row` tanpa panel (chip dibandingkan ke `FilterChip` M3 di
tempat lain, flat di SEMUA tema, bukan bug). 2 sisa titik `GestureIndicatorBadge`
(NowPlayingScreen, badge gesture transient) SENGAJA tidak disentuh — di luar tab yang dilaporkan.
**PENTING:** `ContinueListeningCard` (Home) dicek ulang, fix Batch 300 SUDAH benar di source ini.
Kalau Beranda MASIH flat di APK batch ini (bukan build lama), kemungkinan besar BUKAN lagi bug
kode — `LiquidGlassDarkBackground`/`LightBackground` (`Color.kt`) itu 1 warna solid FLAT di
seluruh layar (0 wallpaper/gradient/blob ambient), jadi blur asli otomatis ikut flat walau
kodenya benar. Ini butuh keputusan arsitektur baru (lapisan ambient), BUKAN tuning parameter lagi
— per §4 aturan sesi aktif, TIDAK dieksekusi tanpa konfirmasi user dulu. **Tanya user: APK yang
diuji itu hasil Batch 300/301 atau lebih lama, dan apakah ambient background mau dikejar.**

**Bug 2 (`MainActivity.kt`, 6 titik):** root cause BUKAN blur — `popUpTo`/`navigate` di 6 titik
(3 `NavigationBarItem` + 3 `NavigationRailItem`, Compact vs Medium/Expanded) 0 pernah pakai
`saveState`/`restoreState`, jadi Nav Compose menghancurkan-total+membangun ulang layar tujuan
tiap tap tab (state `LazyColumn`/scroll/ViewModel scope reset dari nol) — pola resmi Google
sebagai penyebab jank tab switch, mekanisme beda total dari stutter-scroll (resample blur per-
frame) yang sudah dijawab Batch 300. Fix: `popUpTo("home"){saveState=true}` +
`launchSingleTop=true` + `restoreState=true` di keenamnya. `inclusive=true` yang tadinya khusus
tombol Beranda ikut dilepas (0 alasan terdokumentasi ketemu lewat grep CHANGELOG/komentar) —
disamakan 1 pola konsisten; efek samping: Beranda sekarang juga simpan scroll position sendiri.

2 file kode (di bawah batas Micro-Batch): `LibraryScreen.kt`, `MainActivity.kt`. 0 file lain
disentuh, 0 dependency baru, 0 file baru — `FILE_MANIFEST.txt` tidak berubah. Import baru
(`frostedGlass` di `LibraryScreen.kt`) dari `ui.theme` yang sudah ada. Brace/paren kedua file
diverifikasi seimbang. Belum divalidasi compile Gradle sungguhan (0 akses jaringan sesi ini) —
**WAJIB cek CI setelah push**, risiko rendah (1 cabang `when` + opsi navigasi standar, 0 API/
dependency baru). Detail lengkap: `CHANGELOG.md` Batch 301.

**Status setelah batch ini:** Bug 2 kemungkinan besar tuntas (fix arsitektural langsung) tapi
tetap wajib dikonfirmasi user di device nyata. Bug 1 tuntas untuk SEMUA gap routing kode yang
terkonfirmasi lewat grep (0 sisa gap di luar 2 badge NowPlayingScreen yang di luar cakupan) —
TAPI kalau Beranda/Library masih terasa flat setelah build ini, kemungkinan sudah keluar dari
ranah "bug kode" (lihat catatan Bug 1 di atas), bukan sesuatu yang bisa dijawab tuning parameter
lagi seperti Batch 296-300.

**Batch 300 (2 bug fix dari feedback device sungguhan: card Liquid Glass yang flat + stuttering
scroll, 3 file kode)** — User laporkan 2 hal dari device fisik dalam 1 pesan: (1) efek Liquid
Glass cuma kena sebagian card, sisanya flat total; (2) sedikit stuttering pas scroll (belum
sampai freeze). Ini pertama kalinya info performa yang diminta sejak Batch 297/299 datang.

**Bug 1 — card flat (`HomeScreen.kt`, `StatsDashboardScreen.kt`):** root cause BUKAN bug
rendering Haze, tapi gap arsitektur lama: `ContinueListeningCard` (Home) dan `StatSectionCard`
(Stats, komentarnya sendiri bilang "pola sama persis ContinueListeningCard") punya cabang
isTactile/isSkeu sendiri tapi Liquid Glass jatuh ke `else` generik (`Modifier.clip()` +
`Surface` warna solid opaque) — SATU-SATUNYA 2 titik di seluruh app yang tidak routing lewat
`.frostedGlass()` (grep ulang: 12/12 call site lain — MiniPlayerBar, NowPlayingScreen, 8 sheet —
sudah benar sejak Batch 296/297). Ditambah cabang `isLiquidGlass` eksplisit di kedua file, pola
identik isTactile/isSkeu (`Surface` color → Transparent, modifier → `.frostedGlass()`).
`ThemeOptionCard` (SettingsScreen, picker tema) SENGAJA tidak disentuh — itu preview swatch utk
SEMUA identitas tema sekaligus (bukan tema aktif), konteks beda, bukan bagian dari bug ini.
`HomeSongCard` (LazyRow Home) juga tidak disentuh — murni thumbnail+teks, tidak ada Surface/panel
sama sekali, jadi tidak relevan dengan gap ini.

**Bug 2 — stutter scroll (`BlurUtils.kt`, 1 titik):** `blurRadius` (32dp sejak Batch 298)
diturunkan balik ke **24dp** — nilai sebelum Batch 298 menaikkannya, yang mana sudah ditandai
eksplisit di komentar kode itu sendiri sebagai "dekat batas nyaman performa". Blur asli Haze
resample tiap frame saat konten di belakang kaca berubah; MiniPlayerBar (selalu melayang di atas
layar yang sedang di-scroll, per `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §5 langkah 5) adalah
kandidat GPU-cost terbesar. 0 laporan stutter pernah masuk selama radius masih 24dp (Batch
296-297), jadi revert ke situ adalah langkah paling minim risiko. `liquidGlassAlpha`
(0.38f/0.48f, Batch 299) TIDAK disentuh — itu lever tint/visibilitas, bukan lever performa, dan
user tidak melaporkan masalah visibilitas kali ini.

3 file kode (pas batas Micro-Batch): `HomeScreen.kt`, `StatsDashboardScreen.kt`, `BlurUtils.kt`.
0 file lain disentuh, 0 import baru selain `isLiquidGlassTheme`/`frostedGlass` (sudah ada di
`theme/` package, bukan dependency baru), 0 file baru — `FILE_MANIFEST.txt` tidak berubah
(187/187). Brace/paren ketiga file diverifikasi seimbang. Belum divalidasi compile Gradle
sungguhan (0 akses jaringan sesi ini, pola sama tiap batch) — **WAJIB cek CI setelah push**,
risiko rendah (menambah 1 cabang `when` + 1 import per file existing function, 1 literal `Dp`,
bukan API/dependency baru).

**Status Fase 5 langkah 5/5 setelah batch ini: MASIH BELUM SELESAI** — kedua fix di atas
menjawab BAGIAN dari verifikasi device yang diminta (cakupan card + performa), tapi belum ada
konfirmasi ulang user apakah 24dp sudah cukup meredakan stutter (kalau MASIH stutter, turunkan
radius lagi atau tinjau frekuensi re-render MiniPlayerBar saat progress lagu jalan — bukan
tint), dan tint 0.38f/0.48f dari Batch 299 juga masih belum dikonfirmasi user sebagai titik akhir
(bisa jadi butuh sesi terpisah kalau user belum sempat menilai keduanya sekaligus). Detail
lengkap: `CHANGELOG.md` Batch 300.

**Batch 299 (Fase 5 langkah 5/5 — feedback device API 33+ sungguhan: blur masih kurang, tuning
alpha iterasi 2, 1 file kode)** — User laporkan langsung dari device fisik API 33+ (tier
"Runtime Shader" tercepat/paling ringan per `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §2, BUKAN tier
lemah API 31/32): efek blur Liquid Glass masih kurang kelihatan. Ini persis skenario yang sudah
diantisipasi di komentar Batch 296/298 sendiri ("kalau nyaris tak kelihatan → alpha masih
ketinggian, turunkan lagi") — didiagnosis sebagai parameter tint yang masih terlalu pekat
menutupi blur asli, BUKAN bug rendering (device tier terbaik, API Haze `hazeSource`/`hazeEffect`
sudah terkonfirmasi compile+jalan sejak CI Batch 296 hijau).

**`BlurUtils.kt`** (1 titik, `liquidGlassAlpha`): diturunkan lagi 0.55f→0.38f (gelap) /
0.65f→0.48f (terang) — langkah lebih besar dari turun Batch 296→298 krn feedback "masih kurang"
datang SETELAH satu putaran tuning (titik awal 0.92/0.96 lawas), bukan feedback pertama. Gap
dark/light (0.10) dipertahankan sama seperti iterasi-iterasi sebelumnya. `blurRadius` (32dp,
Batch 298) SENGAJA TIDAK ikut dinaikkan — lever yang teridentifikasi utk masalah "blur ketutup"
adalah tint, bukan radius, dan radius sudah didokumentasikan dekat batas nyaman performa (API 32
"berat"). 0 file lain disentuh, 0 import baru, 0 dependency baru — `FILE_MANIFEST.txt` tidak
berubah (187/187). Brace/paren file diverifikasi seimbang (8/8, 34/34 — komentar dikecualikan).

**Status Fase 5 langkah 5/5 setelah batch ini: MASIH BELUM SELESAI**, sekarang di putaran ke-2.
0.38f/0.48f TETAP "titik awal berikutnya", bukan angka final — WAJIB dikonfirmasi ulang user
pas coba build hasil batch ini: kalau MASIH kurang → turunkan lagi; kalau JUSTRU jadi terlalu
ramai/teks susah dibaca → naikkan sedikit dari 0.38/0.48 (jangan balik ke 0.55/0.65). Juga tetap
kumpulkan info performa (ada lag scroll/tidak) yang belum pernah dilaporkan sejak langkah 5/5
dibuka Batch 297. Belum divalidasi compile Gradle sungguhan (0 akses jaringan sesi ini, pola sama
tiap batch) — **WAJIB cek CI setelah push**, meski risiko rendah (2 angka `Float` literal, bukan
API/dependency baru). Detail lengkap: `CHANGELOG.md` Batch 299.

**Batch 298 (Perkuat typography + efek blur Liquid Glass — permintaan user langsung, 2 file kode)**
— User minta eksplisit "perkuat typography+efek blur pada theme liquid glass" di luar antrean
roadmap (langkah 5/5 blur asli masih nunggu verifikasi device sungguhan sejak Batch 297 — batch
INI TIDAK menyelesaikan itu, cuma menambah 2 hal baru yang ikut perlu diverifikasi bareng nanti).
2 perubahan dipasangkan SATU rasionalisasi: blur lebih kuat bikin backdrop lebih "ramai", jadi
teks header/label butuh kontras lebih tinggi biar tetap kebaca di atasnya — bukan 2 task lepas.

**`BlurUtils.kt`** (1 titik): default parameter `blurRadius` fungsi `frostedGlass()` naik
24dp → 32dp. Aman lewat default bersama (bukan cabang khusus baru) krn dikonfirmasi ulang: grep
12/12 call site `.frostedGlass()` di app ini TANPA argumen (semua pakai default), dan parameter
ini genuinely no-op utk 4 identitas lain (Apple/Tactile/Skeu/Calm Retro tidak pernah membacanya
— cuma `isLiquidGlass` yang menangkapnya ke `requestedBlurRadius` lalu diteruskan ke
`hazeEffect`). Jadi menaikkan default ini 100% cuma menguatkan blur asli Liquid Glass, 0 dampak
identitas lain. Belum dinaikkan lebih jauh lagi (mis. 40dp+) krn PROJECT_STATE sudah menandai
API32 "berat" utk blur asli (Batch 294 §2) — 32dp kompromi naik cukup terasa tanpa lompat ke
rentang berisiko performa. `liquidGlassAlpha` (0.55f/0.65f) SENGAJA TIDAK disentuh — itu tint,
bukan blur, dan sudah ditandai eksplisit di Batch 296 sebagai "titik awal, wajib dituning ulang
pas verifikasi visual device" — biar 1 sumber kebenaran, bukan diubah dua kali di dua batch beda
sebelum ada data device sungguhan.

**`Type.kt`** (`LiquidGlassTypography`, 1 titik): 2 kelompok perubahan dari baseline Batch 279.
(1) **Bobot naik 1 tingkat** di 3 slot lama yang sudah ada: `titleLarge` SemiBold→Bold,
`titleMedium` Medium→SemiBold, `labelSmall` Medium→SemiBold — ukuran/lineHeight/letterSpacing
TIDAK disentuh (tetap terbuka/0 ala identitas asli, bukan rapat ala Apple), murni bobot.
(2) **5 slot M3 baru diisi**: `headlineSmall`/`titleSmall`/`bodyLarge`/`labelLarge`/`labelMedium`
— digrep dulu (`MaterialTheme.typography.*` di seluruh `app/src/main/java`) sebelum nulis kode:
kelima slot ini dipakai luas (StatsDashboardScreen angka besar, LibraryScreen+SettingsScreen
judul seksi, LyricsView/LyricsSheet, SmartPlaylistScreen filter, RingtoneCutterSheet,
NowPlayingScreen) tapi belum pernah didefinisikan di `LiquidGlassTypography` — diam-diam jatuh
ke `Typography()` default Material3 (Roboto) tiap Liquid Glass aktif, SATU-SATUNYA dari 5
identitas yang punya lubang ini (AppleTypography/TactileTypography juga cuma isi 5 slot yang
sama, tapi TIDAK disentuh batch ini — di luar scope permintaan user, "liquid glass" doang).
Nilai baru: ukuran ikut pola "app selalu sedikit di atas default M3" yang sudah ada di slot
lama (bukan angka M3 mentah), bobot ikut tier yang sama dgn (1) — `headlineSmall`/`titleSmall`/
`labelLarge` naik ke SemiBold/Bold (peran header/label), `bodyLarge` tetap Normal (peran teks
baca, sejajar `bodyMedium`/`bodySmall` yang JUGA sengaja tidak disentuh) — kontras datang dari
header vs body, bukan dari menebalkan semua teks sekaligus.

Brace/paren kedua file diverifikasi seimbang (kode-only, komentar dikecualikan). 0 import baru,
0 dependency baru, 0 file baru — `FILE_MANIFEST.txt` tidak berubah (187/187). **Belum
divalidasi compile Gradle sungguhan** (0 akses jaringan sesi ini, pola sama tiap batch) —
**WAJIB cek CI setelah push**, walau risikonya rendah (perubahan value + isi `TextStyle` baru,
bukan API/dependency baru yang belum pernah dipakai). Detail lengkap: `CHANGELOG.md` Batch 298.

**Batch 297 (Blur asli fase 5 langkah 3-4/5 — verifikasi ModalBottomSheet + CI Batch 296 hijau,
0 kode)** — User kirim screenshot CI: **Batch 296 Success, 6m 23s** — dependency Haze + API
`hazeSource`/`hazeEffect` TERKONFIRMASI compile bersih, 2 risiko yang ditandai Batch 296
terjawab. 2 web search: (1) Haze py dukungan RESMI utk `ModalBottomSheet`/Dialog (official
sample), bug historis cross-window sudah lama fix (1.6.7, project di 1.7.2); (2) syarat resmi
"containerColor Transparent + tint manual (bukan Haze tints)" — grep ulang, **SEMUA 9 sheet app
ini SUDAH match syarat itu sejak lama** (konvensi lama, kebetulan align). **Kesimpulan: 0 gap,
0 kode tambahan.** Langkah 3 (NowPlayingScreen) & langkah 4 (LibraryScreen/Sheets/Settings)
roadmap DITANDAI SELESAI — reuse penuh Batch 296 + arsitektur lama, sesuai rencana asli.

**Fase 5 kini tersisa SATU item: langkah 5/5 — verifikasi visual+performa di DEVICE
SUNGGUHAN.** Ini bukan tugas kode lagi (0 compiler/emulator di sandbox) — perlu USER coba
langsung: pilih Liquid Glass, buka MiniPlayerBar+NowPlaying+1 sheet, cek blur genuinely
kelihatan (kalau nyaris tak kelihatan → alpha `BlurUtils.kt` 0.55/0.65 masih ketinggian,
turunkan lagi), cek API level device (§2 desain: API31=scrim/0 peningkatan, API32=berat,
API33+=ringan), cek lag saat scroll. Laporkan hasil supaya tuning berikutnya presisi, bukan
tebak-tebak. 0 file diedit. `FILE_MANIFEST.txt` tidak berubah (187/187). Detail: `CHANGELOG.md`
Batch 297, `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §5.

**Batch 296 (Blur asli fase 5 langkah 2/5 — hazeSource+hazeEffect nyala, 2 file)** — User minta
lanjut langsung (bukan tunggu CI Batch 295 dulu). API Haze 1.7.2 dicek ulang web_search sesi
ini: `hazeSource`/`hazeEffect` skema flat 1.x (properti blur langsung di lambda, bukan wrapper
`blurEffect{}` 2.0). `MainActivity.kt` (protected, 1 titik): `Box` pembungkus `NavHost` dapat
`.hazeSource(state=hazeState)` HANYA saat Liquid Glass aktif. `BlurUtils.kt`: `frostedGlass()`'s
cabang `isLiquidGlass` dapat `hazeEffect` (dipasang PALING LUAR sebelum `.background()` —
urutan gambar blur→tint→edge) + `effectiveAlpha` diturunkan 0.55f gelap/0.65f terang (BUKAN
kosmetik — tint setinggi default 0.92/0.96 akan bikin blur nyaris tak kelihatan; TITIK AWAL,
wajib dituning device). Bonus: parameter `blurRadius` yg dari Batch 53 cuma dummy, akhirnya
dipakai sungguhan (capture ke `requestedBlurRadius` dulu, hindari name-shadowing lambda Haze).

**Cakupan LEBIH LUAS dari sekadar MiniPlayerBar**: `frostedGlass()` 1 titik shared → otomatis
nyala jg utk `NowPlayingScreen`'s panel + 8 sheet lain yang overlay di atas region ter-tag.
Langkah 3/5 roadmap ("NowPlaying — cek treatment beda") BELUM diperiksa detail — klaim "selesai"
ditahan sampai verifikasi visual (langkah 5). 2 file, 0 file baru. `FILE_MANIFEST.txt` tidak
berubah (187/187). Brace/paren seimbang. **Belum diverifikasi compile Gradle** — risiko GANDA
(dependency Batch 295 + API Haze yang baru dipakai sekarang, belum pernah dicompile). **WAJIB
cek CI build setelah push, SEBELUM lanjut langkah 3/5** — kelas masalah sama Batch 291-293.
Detail: `CHANGELOG.md` Batch 296, `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §5.

**Batch 295 (Blur asli fase 5 langkah 1/5 — fondasi plumbing Haze, 3 file, dependency baru)** —
User minta lanjut eksekusi (bukan tunggu). Versi **`dev.chrisbanes.haze:haze:1.7.2`** dipilih
(dicek ulang web_search persis di momen eksekusi) — tag "Latest" resmi GitHub, BUKAN linimasa
`2.0.0-alphaXX` yang lebih baru tapi masih pre-release aktif (breaking changes tiap rilis
alpha). STABILITY > Speed menang di atas rule #3 "prioritas mutakhir" — pijakan fondasi 4
sub-langkah berikutnya pakai API yang sudah selesai breaking-change-nya. `app/build.gradle.kts`
(protected, +1 dependency) + `Theme.kt` (+`LocalHazeState`, pola identik `LocalIsDarkTheme`) +
`MainActivity.kt` (protected, `AppNavHost`: `rememberHazeState()` + `Scaffold` dibungkus
`CompositionLocalProvider`, pola minim-diff identik wrap Batch 24 yang sudah ada di file yang
sama). **Dikonfirmasi grep: 0 pemakaian `.hazeSource()`/`.hazeEffect()` di manapun** — genuinely
0 visual/behavior berubah, murni plumbing. Brace/paren seimbang. `FILE_MANIFEST.txt` tidak
berubah. **Belum diverifikasi compile Gradle sungguhan** (0 akses jaringan sesi ini) — **WAJIB
cek CI build setelah push** sebelum lanjut sub-langkah 2 (MiniPlayerBar, kandidat visual
pertama) — dependency+CompositionLocal baru rawan unresolved-reference yang cuma ketahuan
compile-time (kelas masalah sama Batch 291-293). Detail: `CHANGELOG.md` Batch 295,
`LIQUID_GLASS_BLUR_ENGINE_DESIGN.md` §5.

**Batch 294 (Desain teknis blur asli Liquid Glass — PERENCANAAN SAJA, 0 kode, 1 dokumen baru)**
— User pilih "desain dulu" utk Fase 5 (blur asli). Dokumen baru `LIQUID_GLASS_BLUR_ENGINE_DESIGN.md`:
riset 4 opsi (Haze/imla/Cloudy/hand-roll), **rekomendasi adopsi Haze** (`dev.chrisbanes.haze`) —
hand-roll ditolak krn `RenderEffect` 1-baris cuma blur ISI composable sendiri, limitasi SAMA
persis yg sudah ada di `frostedGlass()` sekarang. Ekspektasi realistis dicatat: API 31 (minSdk
sekarang) fallback "scrim" = 0 peningkatan visual, baru kerasa bedanya API 32+. Arsitektur
diperiksa ke kode nyata: `HazeState` direkomendasikan dipegang 1 titik di `AppNavHost`
(`MainActivity.kt`, `Scaffold` berisi `MiniPlayerBar`+`NavHost` sejajar), diteruskan
`CompositionLocal` baru. MiniPlayerBar jadi kandidat blur pertama. 5 sub-langkah eksekusi
didraft, TIDAK dieksekusi. Dependency Haze BELUM ditambahkan, versi sengaja tidak ditulis
(resiko basi). `ROADMAP_LIQUID_GLASS_REDESIGN.md` §5 diupdate nunjuk dokumen ini. 0 kode
disentuh. **Prioritas: TUNGGU user minta lanjut eksekusi**, jangan mulai sub-langkah 1 sendiri.
Detail: `CHANGELOG.md` Batch 294.

**Batch 293 (Hotfix CI — user upload screenshot run #287 + `instrumentation_test_report_287.zip`,
1 protected asset)** — `build` job Batch 292 HIJAU (konfirmasi), tapi job `instrumentation-tests`
terpisah FAILED: "No compatible devices connected." Akar masalah dikonfirmasi silang ke
`app/build.gradle.kts`: `minSdk` sudah 31 sejak Batch 290, tapi `api-level: 30` di job ini
(`.github/workflows/build.yml`) TIDAK ikut diupdate saat itu — emulator API 30 otomatis
ditolak AGP test-runner krn di bawah minSdk modul (30 < 31), makanya 0 device valid meski
emulator sendiri boot sukses. Fix: `api-level: 30` → `31` (tepat di lantai minSdk baru, bukan
asal naik ke targetSdk 34/compileSdk 36 — konsisten alasan asli job ini Batch 103). Grep ulang
konfirmasi cuma 1 titik config aktif (bukan asumsi). YAML divalidasi ulang. ⚠️ **Belum ada CI run
baru yang membuktikan job ini sungguhan hijau** — test itu sendiri belum PERNAH tereksekusi
nyata di batch manapun (selalu gagal dapat device duluan sebelum test-nya sendiri sempat jalan).
Detail: `CHANGELOG.md` Batch 293.

**Batch 292 (Hotfix CI FAILED — user upload `log_fail_286.zip`, "debugging sampai tuntas, gak
usah denial", 4 file kode)** — Konsekuensi LANGSUNG bump BOM Batch 291: `animateItemPlacement()`
(dipakai 7x di 4 file — `FolderManagerSheet.kt`, `LibraryScreen.kt`x4, `PlaylistScreen.kt`,
`QueueSheet.kt`) sudah 100% DIHAPUS dari BOM 2026.04.01 (bukan cuma deprecated-warning lagi),
`Unresolved reference` di compileDebugKotlin DAN compileReleaseKotlin. Log dicek penuh dari awal
— dikonfirmasi ini SATU-SATUNYA akar masalah, 0 error lain tersembunyi. Fix: `.animateItemPlacement()`
→ `.animateItem()`, mekanis di ke-7 lokasi (semua tanpa `animationSpec` custom, jadi padanan
langsung, bukan perkiraan). 0 import baru. Brace/paren ke-4 file diverifikasi seimbang. ⚠️
**Belum ada CI run baru yang membuktikan hijau** — baru menghilangkan 1 jenis error yang
terkonfirmasi dari log, prioritas kalau user push: pastikan run berikutnya BENAR-BENAR lolos,
bukan cuma dianggap selesai dari sisi statis. Detail: `CHANGELOG.md` Batch 292.

**Batch 291 (Liquid Glass langkah 5 lanjutan — bump Compose BOM 2024.05.00→2026.04.01, 1 file
kode + 2 dokumentasi)** — Blocker teknis kedua (setelah minSdk Batch 290): `GraphicsLayer`
capture API (wajib buat blur asli) baru stabil BOM 2024.09.00+, lama belum punya. **Keputusan
SENGAJA bukan BOM paling baru** (2026.08.00/Compose 1.12) — itu maksa compileSdk 37+AGP
9.1.1+migrasi breaking (DSL lama dihapus, Gradle 9.1.0+ wajib) — dipilih **2026.04.01** (Compose
1.11) yg sudah py `GraphicsLayer` TAPI tetap kompatibel compileSdk 36/AGP 8.13.0 yg sudah ada,
0 migrasi breaking. STABILITY > Speed menang di atas "prioritas mutakhir mutlak". Brace/paren
seimbang. **⚠️ BELUM tervalidasi build sungguhan** (0 akses jaringan sesi ini) —
`LocalOverscrollConfiguration` (`SmartPlaylistScreen.kt`) tersangka pertama kalau CI gagal
(potensi API pindah ke `overscrollEffect` di rentang lompatan ini), BELUM diperbaiki preventif
(hindari perubahan spekulatif). **WAJIB cek hasil CI build setelah push batch ini** sebelum
lanjut ke sub-langkah GraphicsLayer/RenderEffect modifier sungguhan. Detail: `CHANGELOG.md`
Batch 291.

**Batch 290 (Liquid Glass langkah 5 DIKONFIRMASI, mulai eksekusi: bump minSdk 23→31, 1 file
kode + 2 dokumentasi)** — User konfirmasi eksplisit lanjut Opsi A §3b (blur asli) setelah
trade-off dijelaskan. Sub-langkah pertama: `app/build.gradle.kts` `minSdk` 23→31 (edit fokus 1
baris+komentar). 0 guard `SDK_INT < 31` ditemukan (0 dead-code cleanup perlu). Brace/paren
seimbang. `ROADMAP_LIQUID_GLASS_REDESIGN.md` §5 disinkron: langkah 5 "sedang berjalan".
**⚠️ KONSEKUENSI PENTING**: APK dari sini TIDAK BISA diinstall device Android <12 (API <31) —
kalau device testing utama user API-nya <31, WAJIB diinfokan/dicek sebelum lanjut sub-langkah
berikutnya (infrastruktur `RenderEffect` capture+blur, belum ada sama sekali, effort tinggi).
Detail: `CHANGELOG.md` Batch 290.

**Batch 289 (Sync ROADMAP_LIQUID_GLASS_REDESIGN.md — fase 3 100% selesai, 2 dokumentasi, 0
kode)** — §5 belum disinkron sejak Batch 288. Sisa fase 3 (chip/pill) ditandai ✅ SELESAI PENUH,
Fase 3 keseluruhan 100% selesai (0 pending). Catatan ditambahkan: satu-satunya sisa roadmap
(langkah 5, blur asli §3b) OPSIONAL, butuh konfirmasi user dulu (bump minSdk) — TIDAK bisa
auto-eksekusi. **Sesi berikutnya kalau user tidak punya instruksi baru: TANYA dulu mau lanjut
blur asli atau anggap redesign Liquid Glass selesai — jangan eksekusi kode langsung dari
roadmap ini lagi tanpa itu.** 0 kode, 0 protected asset. Detail: `CHANGELOG.md` Batch 289.

**Batch 288 (Liquid Glass fase 3 — sisa 5 titik Material3 FilterChip bawaan, 3 file kode + 1
dokumentasi)** — Menutup Pending Queue Batch 287. `EqualizerSheet.kt` (2 titik: preset
kuat+bawaan perangkat), `SmartPlaylistScreen.kt`'s `SmartPlaylistBuilderSheet` (2 titik: folder+
genre), `RingtoneCutterSheet.kt`'s `DestinationChip` (1 titik) — semua dikasih `shape =
if (isLiquidGlassTheme()) RoundedCornerShape(Radius.liquidPill) else FilterChipDefaults.shape`,
pola persis Batch 287. **Kandidat pill/chip fase 3 SEKARANG SELESAI PENUH — 0 sisa.** Brace/
paren ketiga file seimbang, 0 import duplikat. 0 protected asset. **Belum diverifikasi visual.**
Detail: `CHANGELOG.md` Batch 288.

**Batch 287 (Liquid Glass fase 3 sisa langkah — pill/chip lebar, LibraryFilterChips →
Radius.liquidPill, 1 file kode + 1 dokumentasi)** — Kandidat pertama & paling menonjol:
`LibraryFilterChips` (tab Library, dilihat tiap kunjungan). Radius dulu `Radius.xxl` (20dp
FIXED, cuma kebetulan terlihat pill di ukuran teks sekarang). Fix: `chipRadius = if
(isLiquidGlassTheme()) Radius.liquidPill else Radius.xxl` (reuse helper Batch 280), diterapkan
2 chip row. Opt-in per-identitas, tema lain 0 perubahan. Brace/paren seimbang (351/351,
793/793). **⏳ Kandidat lain BELUM diaudit** (Material3 `FilterChip` bawaan di 3 file lain,
shape default beda dari custom shape ini) — batch berikutnya. **Belum diverifikasi visual.**
Detail: `CHANGELOG.md` Batch 287.

**Batch 286 (Liquid Glass fase 3 langkah 5 — audit Settings, 0 gap, sub-langkah 3 SELESAI
PENUH, 2 dokumentasi)** — Grep menyeluruh `SettingsScreen.kt`: cuma 1 cluster branch identitas
di seluruh file (`ThemeOptionCard` preview tema, 3 cek ke identitas spesifik LAIN — Liquid
Glass otomatis flat/minimalis lewat absensi kecocokan, bukan branch eksplisit, hasil SAMA
dengan file lain). 0 `frostedGlass()` di file ini — Settings = list polos, sisa 100% generik.
**Sub-langkah 3 (MiniPlayerBar→NowPlayingScreen→LibraryScreen→Sheets/Dialog→Settings) SEKARANG
TUNTAS PENUH, 0 gap di 5/5 area.** 0 kode. Item berikutnya: audit pill/chip lebar layak
`Radius.liquidPill` (belum ada kandidat, perlu grep terarah baru). Detail: `CHANGELOG.md`
Batch 286.

**Batch 285 (Rebranding kosmetik "Audio Player" → "SONIX", 8 file kode + 1 dokumentasi, cap
dilewati — 1 task kohesif)** — Permintaan user eksplisit, nama terinspirasi CONVX. 9 titik
user-facing diganti (`strings.xml` app_name, splash screen, notifikasi, widget, Settings,
biometric prompt, backup error msg, README). **SENGAJA TIDAK disentuh** (vital/stable, sesuai
instruksi user): `applicationId`/`namespace` (ganti = app dianggap beda oleh Android, data user
hilang), nama class/style internal, **path filesystem device asli** (Documents/AudioPlayer/
backups+logs — ganti = file lama user jadi orphan), marker JSON internal backup (kompatibilitas
restore lama↔baru tetap terjaga), komentar kode. Brace/paren 6 file Kotlin seimbang, XML valid,
grep ulang konfirmasi 0 titik user-facing kelewat. Cap dilewati (9 file, 1 task kohesif, presedan
Batch 156/275). **Belum diverifikasi visual**. Detail: `CHANGELOG.md` Batch 285.

**Batch 284 (Liquid Glass fase 3 langkah 4 — audit Sheets/Dialog, 0 kode)** — 9 sheet pemakai
`.frostedGlass()` (`ABRepeatBookmarkSheet`/`EqualizerSheet`/`FolderManagerSheet`/`LyricsSheet`/
`QueueSheet`/`RingtoneCutterSheet`/`SongInfoEditSheet`/`SongPickerSheet`/`VisualizerSheet`)
SEMUA otomatis kebagian fix edge Batch 281. 5 di antaranya py branch `isCalmRetro→calmScanlines`
tambahan, Liquid Glass jatuh `else` dgn benar. 4 sisanya 0 branch identitas sama sekali. Dialog
non-sheet (`AlertDialog` Material3 standar) 0 hardcoded warna, otomatis token fase 2. **Hasil: 0
gap.** `FILE_MANIFEST.txt` tidak berubah (186/186). Item berikutnya: audit Settings. Detail:
`CHANGELOG.md` Batch 284.

**Batch 283 (Liquid Glass fase 3 langkah 3 — audit LibraryScreen.kt, 0 kode)** — 2 titik
`isTactile`/`isSkeu`/`isCalmRetro` ditemukan di seluruh file (snackbar undo-hide, `SongRow`'s
`calmScanlines`), keduanya diperiksa: Liquid Glass jatuh `else` di kedua titik, SUDAH BENAR
(snackbar solid otomatis dapat warna violet-cool lewat `colorScheme.surface`; scanline CRT
memang cuma relevan CalmRetro). `AlbumGridView` 0 branch identitas sama sekali — otomatis
konsisten. **Hasil: 0 gap.** `FILE_MANIFEST.txt` tidak berubah (186/186). Item berikutnya: audit
Sheets/Dialog. Detail: `CHANGELOG.md` Batch 283.

**Batch 282 (Liquid Glass fase 3 langkah 2 — audit MiniPlayerBar + NowPlayingScreen, 0 kode)** —
Semua titik `when { isTactile/isSkeu/isCalmRetro/else }` di 2 file diperiksa: `else` (Liquid
Glass jatuh ke sini bareng Apple) SEMUA sudah benar — shape stadium (CircleShape = liquidPill
utk elemen persegi), flat/tanpa-emboss SESUAI definisi "minimalis" identitas ini, accentColor
tetap dinamis per-lagu (bukan locked kayak CalmRetro, sesuai catatan Batch 280).
`GestureIndicatorBadge` py surface terpisah (bukan lewat `frostedGlass()`) tapi konsisten
by-design: cuma bedakan "panel fisik" (Tactile/Skeu) vs "sisanya". **Hasil: 0 gap.**
`FILE_MANIFEST.txt` tidak berubah (186/186). Item berikutnya: audit `LibraryScreen.kt`
(`SongRow` dkk). Detail: `CHANGELOG.md` Batch 282.

**Batch 281 (Liquid Glass fase 3 langkah 1 — edgeBrush khusus di `frostedGlass()`, 2 file)** —
`frostedGlass()` (`BlurUtils.kt`) = 1 shared helper dilalui SEMUA panel glass app-wide (mini
player, tiap bottom sheet, card Home/Library). `Theme.kt` +`isLiquidGlassTheme()` (pola identik
3 helper identitas lain). `BlurUtils.kt`'s `edgeBrush` dapat cabang `isLiquidGlass` sendiri
(bukan jatuh ke `else` yang cuma benar deteksi "Apple light" — tanpa branch sendiri, Liquid
Glass mode TERANG akan diam-diam pakai alpha edge yang dituning utk GELAP, laten bug ditemukan
saat fix, bukan cuma kosmetik). Cabang baru: highlight rim `LiquidGlassAccent` ungu tipis
(0.32→0.06 gelap, 0.22→0.05 terang), gradient statis (bukan blur asli, sesuai §3b Opsi B).

**Cakupan otomatis**: MiniPlayerBar/NowPlayingScreen/semua Sheet/card Home-Library SEMUA ikut
dapat edge violet-glass serentak — roadmap §5 langkah 3 bagian "glass-edge" per-komponen
dianggap selesai lewat 1 fix terpusat ini (bukan pola per-file). 2 file, 0 protected asset.
Brace/paren seimbang. `FILE_MANIFEST.txt` tidak berubah (186/186). **Belum diverifikasi visual
di device** — cek rim ungu terlihat di kedua mode + 4 identitas lama TIDAK berubah (regresi
urutan `when`). Item berikutnya: audit elemen pill/chip lebar yang layak `Radius.liquidPill`
eksplisit (belum ketemu kandidat). Detail: `CHANGELOG.md` Batch 281.

**Batch 280 (Liquid Glass fase 2 — ThemeIdentity.LIQUID_GLASS lengkap, 3 file, additif)** —
Identitas ke-5 utuh (belum default, side-by-side 4 lama sesuai §3a). `Color.kt` +10 token
palet statis (bg/surface/text × dark/light, accent violet-glass, success teal). `Theme.kt`
+`ThemeIdentity.LIQUID_GLASS` + `LiquidGlassDarkColors`/`LightColors` +
`LiquidGlassShapes` (small=18dp/medium=24dp/large=34dp — **`Radius.liquidPill` 999dp SENGAJA
TIDAK dipasang di `Shapes` generik**, berisiko blob di Card/Sheet besar, disimpan utk call site
pill spesifik fase 3; draf awal sempat salah pasang di situ, dikoreksi sebelum commit). 3 titik
dispatch (`colorsFor()`+2 when-block) exhaustive 5/5, dikonfirmasi grep ulang. **Picker Settings
0 disentuh** — sudah generik (`ThemeIdentity.entries` + `colorsFor()` di `ThemeOptionCard`),
dikonfirmasi baca kode dulu, LIQUID_GLASS otomatis muncul. Brace/paren seimbang. `FILE_MANIFEST.
txt` tidak berubah. **Belum diverifikasi visual di device** — prioritas cek: pilih Liquid Glass
di Settings, app pindah tema tanpa crash, 4 tema lama masih utuh. Item berikutnya (fase 3):
terapkan ke komponen custom-effect (belum ada punya Liquid Glass sendiri) + pasang `liquidPill`
di call site pill spesifik. Detail: `CHANGELOG.md` Batch 280.

**Batch 279 (Liquid Glass §3 dikonfirmasi user + fase 1 eksekusi — fondasi token
radius+typography, 2 file, additif)** — User jawab §3 `ROADMAP_LIQUID_GLASS_REDESIGN.md`:
**3a→tambah tema ke-5** (bukan ganti/konsolidasi seperti rekomendasi dokumen — 4 tema lama
tetap ada), **3b→Opsi B dulu** (shape+typography murni, tanpa blur asli, bertahap per fase),
**3c→4 identitas lama TIDAK di-retire**. Roadmap diupdate mencatat keputusan final SEBELUM
eksekusi kode. Fase 1 dieksekusi: `Spacing.kt` +2 token radius (`liquidLg` 34dp, `liquidPill`
999dp stadium), `Type.kt` +`LiquidGlassTypography` (weight 1 tingkat lebih ringan dari Apple,
letterSpacing lebih terbuka, size/lineHeight dipertahankan sama). **Purely additif, 0
pemakaian di luar 2 file definisi (dikonfirmasi grep)** — 0 perubahan visual sampai fase 2.
Brace/paren seimbang. `FILE_MANIFEST.txt` tidak berubah. Item berikutnya (fase 2):
`ThemeIdentity.LIQUID_GLASS` lengkap di `Theme.kt` (warna+shape+dispatch+picker Settings).
Detail: `CHANGELOG.md` Batch 279, `ROADMAP_LIQUID_GLASS_REDESIGN.md` §5.

**Batch 278 (Arahan user — arsipkan dokumentasi stale + arahkan goals ke redesign "Liquid
Glass" terinspirasi CONVX, PERENCANAAN SAJA, 0 kode)** — 2 bagian instruksi:
1. **Arsip**: `MICRO_UIUX_AUDIT.md`→`ARCHIVED_MICRO_UIUX_AUDIT.md`,
   `POLISH_AUDIT.md`→`ARCHIVED_POLISH_AUDIT.md` (isi dipertahankan penuh, cuma ditambah banner
   arsip di atas, pola sama `ARCHIVED_ROADMAP_15_FITUR_OFFLINE.md`). Bukan karena isinya salah
   (12/14 kategori genuinely TUNTAS) — karena keduanya mengaudit konsistensi SISTEM VISUAL LAMA
   yang segera diganti total; melanjutkan sekarang = kerja terbuang.
2. **Roadmap baru**: `ROADMAP_LIQUID_GLASS_REDESIGN.md` (file baru) — riset CONVX via web search
   (real backdrop blur/refraction, motion iOS-style, Material You dari artwork, dibangun di atas
   library `Kyant0/backdrop` terpisah) dikontraskan ke kondisi project sekarang (4 identitas
   visual ada: Apple/Tactile/Neumorphism/Calm Retro; `frostedGlass()` di `BlurUtils.kt` TERNYATA
   glass PALSU — `Modifier.blur()` Compose blur foreground bukan background, jadi sekarang cuma
   tinted-surface simulasi, bukan sampling real-time; `minSdk=23` jadi kendala nyata utk blur
   asli yang butuh API 31+). 3 keputusan besar diidentifikasi BUTUH konfirmasi user dulu sebelum
   eksekusi kode dimulai (ganti vs tambah tema; blur sungguhan [bump minSdk] vs "look" tanpa
   blur asli; nasib 4 identitas lama) — TIDAK ditebak sendiri, resiko puluhan batch salah arah.
   Rencana eksekusi bertahap 5 fase didraft, urutan reuse pola `POLISH_AUDIT.md` lama.
   `README.md` § "Rencana v2" ditambah pointer singkat ke roadmap ini.

`FILE_MANIFEST.txt` diperbarui (2 rename, 1 file baru). 0 file kode (`.kt`/`.xml`) disentuh sama
sekali — sesuai instruksi eksplisit user "documentation planning only first". **Prioritas
sesi berikutnya: JANGAN langsung eksekusi roadmap** — konfirmasi §3a/3b/3c
`ROADMAP_LIQUID_GLASS_REDESIGN.md` ke user dulu. Detail: `CHANGELOG.md` Batch 278.

**Batch 277 (Samakan standar informatif GitHub Release body dgn Build Summary, 1 protected
asset + 1 file kode)** — User bandingkan screenshot: Summary sudah rapi, Release body masih
polos. Kendala dijaga: `release_notes.txt` dibaca 2 konsumen (web GitHub Markdown-render vs app
`Text()` polos) — enrichment SENGAJA plain text, bukan Markdown spt Summary. Fix: `build.yml`
tulis `${tag} (${sha})` + baris kosong sebelum pesan commit (YAML divalidasi segera, bukan di
akhir); `UpdateCheckSheet.kt` buang prefix redundan itu (`substringAfter("\n\n", ...)`, fallback
aman ke rilis lama). Brace/paren seimbang (25/25, 67/67), YAML valid 13 step. **Belum
diverifikasi visual** — cek: halaman Release baru ada versi+SHA di body, app "Cek Update" TIDAK
duplikasi versi. Detail: `CHANGELOG.md` Batch 277.

**Batch 276 (Rapikan `.github/workflows/build.yml` — section header + Build Summary informatif,
1 file, protected asset edit-parsial)** — User: workflow berantakan & tidak informatif. Scope
ketat: 0 logic/command/urutan diubah, cuma ditambah: 5 section-header comment (wayfinding
visual) + step baru "Publish build summary" yang nulis tabel Markdown ke `$GITHUB_STEP_SUMMARY`
(versi/tag, commit, trigger+aktor, pesan commit, link Release) — sebelumnya halaman ringkasan
run GitHub Actions SELALU KOSONG TOTAL, cuma daftar step. 100% aditif, `if: always()`. **Insiden
kecil dicatat jujur**: 1 edit sempat tidak sengaja hapus baris `run: |`, LANGSUNG terdeteksi via
validasi YAML setelah SETIAP edit tunggal (bukan cuma di akhir), langsung diperbaiki. File final
divalidasi penuh: 18 step (12+6) dikonfirmasi identik posisi/nama dgn sebelum edit. 1 file, 0
file lain. **Belum diverifikasi run CI sungguhan** — prioritas cek: push, buka tab Actions run
baru, pastikan section Summary muncul. Detail: `CHANGELOG.md` Batch 276.

**Batch 275 (POLISH_AUDIT.md kategori 4 — audit Button lintas screen, 4 gap (3 fix, 1 pending),
3 file kode + 3 dokumentasi, cap dilewati 1x)** — Sub-item "Button": grep semua `Button(` di
26 file `ui/`, cek kehadiran `bouncyPress` (standar app-wide sejak Batch 256). 10 titik, 2
awalnya dikira gap tapi FALSE POSITIVE (dicek ulang lebih teliti), 4 gap NYATA. 3 diperbaiki
(`LibraryScreen.kt` EmptyState CTA — dampak PALING LUAS krn dipakai banyak screen,
`SongPickerSheet.kt` tombol konfirmasi, `SmartPlaylistScreen.kt` tombol simpan) pakai pola
identik referensi `VaultSheet.kt`. 1 PENDING (`LyricsSheet.kt:214`, di luar cap). Brace/paren
3 file seimbang. Cap dilewati (alasan sama Batch 156: 1 task kohesif, dokumentasi tetap wajib).
**Belum diverifikasi visual**, risiko rendah (pola sudah terbukti di 6 titik lain). Detail:
`CHANGELOG.md` Batch 275.

**Batch 274 (POLISH_AUDIT.md — audit disabled/selected state lintas screen, 0 bug, 2
dokumentasi)** — Item teratas kategori Surface/Color. 4 kategori state diperiksa: disabled
(5/5 konsisten), isPlaying (4/4 konsisten, dikonfirmasi ulang), isSelected (0 warna di 3
implementasi independen — checkbox-glyph-only, tidak tabrakan dgn isPlaying meski bisa kena
bareng), tab-chip-selected (beda treatment SENGAJA, beda peran UI). **0 bug ditemukan** — tidak
menciptakan kerjaan baru. 0 kode. Kategori Surface/Color sekarang TUNTAS (guardrail "jangan
redesign theme" bukan task nyata). Item berikutnya: kategori 4 Repeated Components. Detail:
`CHANGELOG.md` Batch 274.

**Batch 273 (Fix "select→instant self-deselect" di SongPickerSheet, PORT dari Batch 271, 1
file kode)** — User: screenshot 2 sheet (Favorit/Playlist) masih instant-cancel. Dikonfirmasi
baca kode: `SongPickerSheet.kt` punya gesture DUPLIKAT sendiri (Batch 268, bukan delegasi dari
`LibraryScreen.kt`) — jadi fix Batch 271 (`.consume()` bug, `onDragStart` gak consume →
tekan-diam bocor ke `.clickable` → toggle balik) TIDAK PERNAH ikut ter-port ke sini. Fix: pola
`suppressClickForId` di-port PERSIS dari `SongListView` — isi di `onDragStart`, telan sekali di
`.clickable` Row, bersih di `onDrag`/`onDragCancel` (bukan `onDragEnd`, sama alasan Batch 271).
1 file, 0 protected asset, brace/paren seimbang (47/47, 130/130). Fix Batch 269/270 (sheet-vs-
scroll) TIDAK disentuh, beda kelas masalah, masih relevan. **⚠️ PRIORITAS verifikasi**: long-
press 1 lagu TANPA gerak di sheet Favorit/Playlist, checkbox harus TETAP tercentang. Detail:
`CHANGELOG.md` Batch 273.

**Batch 272 (Fitur — selectionMode WAJIB persist meski selectedIds kosong, 1 file kode)** — User:
long-press lalu diam/iseng deselect lagu pertama TIDAK BOLEH auto-kembali normal, keluar cuma
lewat tombol Close manual, berlaku semua logic terkait. Audit `grep selectionMode` seluruh `ui/`
— cuma `LibraryScreen.kt` yang relevan (`SongPickerSheet.kt` checkbox selalu tampil, tidak ada
"mode"). 1 titik auto-exit ditemukan: `toggleSelect()`'s `if (selectedIds.isEmpty())
selectionMode=false` — dipakai 4 tab via delegasi 1 fungsi, jadi 1 fix cover semua. Dihapus;
`exitSelectionMode()` (tombol Close) sekarang satu-satunya exit (dikonfirmasi grep ulang).
Hardening: `SelectionActionBar` 3 tombol aksi massal di-disable saat count==0 (cegah bulk-action
jalan atas 0 lagu), tombol Close tetap selalu aktif. `bulkHide()`'s exit-setelah-aksi SENGAJA
tidak disentuh (beda kasus, itu deliberate bukan auto-cancel). 1 file, 0 protected asset,
brace/paren seimbang (350/350, 781/781). **Belum diverifikasi visual** — prioritas cek:
deselect lagu tunggal pas selectionMode aktif, ActionBar harus tetap ada (count:0, tombol aksi
abu-abu) sampai Close ditekan. Detail: `CHANGELOG.md` Batch 272.

**Batch 271 (Fix ROOT CAUSE sweep-select "auto-cancel diri sendiri" saat long-press TANPA gerak,
1 file kode + 2 dokumentasi)** — User kasih root cause: long-press tanpa sweep sama sekali
terkesan auto-cancel. Akar: `onDragStart` (`SongListView`, `LibraryScreen.kt`) pilih baris +
`selectionMode=true`, TAPI tidak pernah `.consume()` (cuma terima `Offset`) — kalau jari lepas
tanpa gerak, `onDrag` (satu-satunya yg consume) tidak pernah jalan, jadi sentuhan asli tidak
pernah dikonsumsi. `SongRow`'s `clickable` polos (0 `onLongClick` sejak Batch 72) masih
mengintip pasangan sentuh yang sama, nyusul jadi "klik" — karena `selectionMode` sudah `true`,
klik itu rute ke `onToggleSelect()` yang MEMBALIK baris yang baru saja terpilih. Select →
instant self-deselect. Fix: latch `suppressClickForId`, diisi persis di `onDragStart`, ditelan
sekali di `onClick`/`onToggleSelect` `SongRow` kalau id cocok, dibersihkan di `onDrag` (gerak
asli) & `onDragCancel` (jaring pengaman) — SENGAJA TIDAK di `onDragEnd` (ordering vs klik-hantu
tidak terjamin, bisa balikin bug). 1 titik fix, 4 tab kebagian (Lagu/Favorit/Artist/Folder,
arsitektur delegasi sejak Batch 197). 1 file, 0 protected asset. **Bonus**: `FILE_MANIFEST.txt`
basi sejak Batch 266 (`SongPickerSheet.kt` tidak pernah ditambah) — dibetulkan 184→185, ketauan
pas cek integritas awal batch. **⚠️ PALING PRIORITAS diverifikasi visual dari semua batch
belakangan** — ini gesture inti 4 tab: tes long-press-tanpa-gerak, sweep-normal (jangan
regresi hysteresis Batch 1/v263), dan tap baris lain saat ada row lain kepilih. Detail:
`CHANGELOG.md` Batch 271.

**Batch 270 (Fix sweep-select SongPickerSheet TAKE 2 — NestedScrollConnection, 1 file kode)** —
User konfirmasi Batch 269 (`confirmValueChange`) tidak cukup. Diriset ulang: `ModalBottomSheet`'s
`anchoredDraggable` MEMPROSES delta drag visual duluan setiap `LazyColumn` kehabisan sisa scroll
(termasuk gerakan kecil pas fase tunggu long-press) — `confirmValueChange` cuma tolak state
akhir, gerakan yang ganggu long-press sudah kejadian duluan. Fix resmi/dikenal luas: custom
`NestedScrollConnection` di content wrapper, `onPostScroll` habiskan semua sisa delta vertikal
— sheet TIDAK PERNAH kebagian delta buat mulai drag. `isSweeping` guard (Batch 269) tetap
dipertahankan sebagai lapisan tambahan. 1 file, +3 import, 0 protected asset, brace/paren
seimbang (46/46, 119/119). **Belum diverifikasi visual** — kalau masih terjadi, butuh
video/rekaman gesture buat diagnosis lanjut. Detail: `CHANGELOG.md` Batch 270.

**Batch 269 (Fix sweep-select oversensitif di SongPickerSheet, 1 file kode)** — User: sebagian
sweep-select normal, sebagian mem-batalkan diri sendiri. Root cause: `SongPickerSheet.kt`
dibungkus `ModalBottomSheet` yang punya swipe-to-dismiss bawaan bersaing dengan long-press-drag
sweep-select buat gesture vertikal yang sama — `SongListView` di layar biasa (tab Lagu) tidak
punya pesaing gesture ini, itu bedanya. Fix: state `isSweeping` blok `confirmValueChange` sheet
selama sweep aktif (`onDragStart`→true, `onDragEnd`/`onDragCancel`→false), sheet tidak bisa
dismiss di tengah drag. 1 file, 0 protected asset, brace/paren seimbang (43/43, 112/112). Murni
Kotlin state, tidak perlu naikkan Material3. **Belum diverifikasi visual.** Detail:
`CHANGELOG.md` Batch 269.

**Batch 268 (SongPickerSheet: layar lebih luas + sweep-select, 1 file kode)** — User laporan
sheet (FAB Favorit/Playlist Batch 266-267) kecil & 0 sweep-select. Fix `SongPickerSheet.kt`: (1)
`fillMaxHeight(0.92f)` + `LazyColumn weight(1f)` (dulu capped 420dp); (2) sweep-select di-port
1:1 dari `SongListView` (`LibraryScreen.kt`) — tekan-lama+geser centang banyak lagu, termasuk
hysteresis 6dp & `DisposableEffect` cleanup. Beda: checkbox SELALU tampil (gak perlu
`selectionMode` terpisah), sweep langsung nambah ke `selected`. Brace/paren seimbang. 0
protected asset. **Belum diverifikasi visual** — cek sweep tetap akurat pas list difilter
pencarian. Detail: `CHANGELOG.md` Batch 268.

**Batch 267 (FAB shortcut "Tambah lagu" di detail Playlist, 2 file kode)** — Menutup Pending
Queue Batch 266. `PlaylistScreen.kt` (`PlaylistTabView`): param baru `onAddSongToPlaylist`+
`onInfoMessage` diteruskan dari `LibraryScreen.kt` (1 call site). Detail playlist (kosong/isi)
dibungkus `Box`+`FAB` (`+`, BottomEnd) buka `SongPickerSheet` (reuse Batch 266),
`alreadyAddedIds` nyaring lagu yg udah ada, `onConfirm` loop+hitung `addedCount`+toast. FAB
"Buat playlist baru" di LIST playlist (beda konteks) TIDAK disentuh. Brace/paren kedua file
seimbang. 0 protected asset. **Belum diverifikasi visual.** Detail: `CHANGELOG.md` Batch 267.

**Batch 266 (FAB shortcut "Tambah ke Favorit" + SongPickerSheet reusable, 2 file kode + 1
dokumentasi)** — User laporan screenshot: tab Favorit & Playlist kosong, satu-satunya cara nambah
lagu WAJIB muter ke tab Lagu dulu. File baru `SongPickerSheet.kt` — sheet cari+checklist banyak
lagu, `alreadyAddedIds` nyaring otomatis, `onConfirm` sekali dgn list id. `LibraryScreen.kt`: tab
Favorit dibungkus `Box`+`FloatingActionButton` (ikon hati, BottomEnd, sembunyi saat
selectionMode) buka `SongPickerSheet`, loop `onToggleFavorite`. FAB tetap muncul walau daftar
udah terisi. Brace/paren kedua file seimbang. 0 protected asset. **Belum diverifikasi visual.**
**Pending Queue**: FAB serupa buat tab Playlist (detail playlist) — ditunda demi cap 3 file,
butuh sentuh `PlaylistScreen.kt` (`onAddSongToPlaylist` belum diteruskan ke `PlaylistTabView`).
Detail: `CHANGELOG.md` Batch 266.

**Batch 265 (Fix SUNGGUHAN "gak bisa pilih lagu" — koreksi user atas Batch 264, 1 file)** — Root
cause ASLI: `showMenu` (state `DropdownMenu` di `SongRow`, isinya termasuk "Pilih" ->
`onEnterSelectionMode()`) TIDAK PERNAH di-set `true` di mana pun — menu 100% unreachable, di
SEMUA tab lewat `SongRow` (Lagu/Favorit/Artis/Folder/Search), bukan gap per-tab seperti dugaan
Batch 262/264. Fix: tambah `IconButton` "..." (`Icons.Default.MoreVert`, import baru) yang
men-trigger `showMenu = true`. **Playlist tab TETAP belum tersentuh** (composable lain total,
`PlaylistTabView`, tidak lewat `SongRow`) — masih butuh keputusan desain terpisah, sama seperti
dicatat Batch 264. Brace/paren seimbang (339/339, 740/740). `FILE_MANIFEST.txt` tidak berubah. 0
protected asset. **Belum diverifikasi visual di device** — prioritas kalau user push: tap "..."
di sebuah lagu (tab manapun), pastikan menu muncul + "Pilih" masuk mode seleksi. Detail:
`CHANGELOG.md` Batch 265.

**Pending Queue (1 item, sama seperti Batch 264, belum berubah)**: sweep-select/multi-select di
tab Playlist (`selectedTab == 5`, `PlaylistTabView`) — perlu keputusan desain: pilih banyak
PLAYLIST sekaligus, atau di dalam tampilan lagu-per-playlist (drill-down)? Tanya user dulu
sebelum eksekusi.

**Batch 264 (Pending Queue item 2 — fix sweep-select over-sensitif tab Lagu, 1 file)** —
Lanjutan sesi baru (`AudioPlayer_v263_Batch1.zip`, hard reset). `detectDragGesturesAfterLongPress`
di `SongListView`/`LibraryScreen.kt` (satu-satunya implementasi, dipakai bareng tab Lagu(0)+
Favorit(4)) dulu pindah row PERSIS saat Y lewat batas 1px — tremor jari dekat garis batas =
flicker seleksi. Fix: `hysteresisPx` (6dp), row baru commit kalau touch lewat SEJAUH itu dari
batas row sebelumnya. Swipe cepat tidak berubah. **Koreksi Pending Queue item 1 (sendirinya
terkoreksi ulang oleh Batch 265 di atas — sweep-select bukan masalahnya, dropdown "Pilih" yang
unreachable)** — diverifikasi ulang ke kode sungguhan dulu (bukan asumsi): tab Favorit TERNYATA
SUDAH punya sweep-select (basi/salah di catatan Batch 262), sisa PR sebenarnya cuma tab Playlist
(composable beda total, `PlaylistTabView`, bukan sekadar sambung parameter). Brace/paren
seimbang (337/337, 733/733). `FILE_MANIFEST.txt` tidak berubah. 0 protected asset. **Belum
diverifikasi visual di device** — gesture sensitif susah dinilai dari kode doang. Detail:
`CHANGELOG.md` Batch 264.

**Batch 263 (Follow-up fix — scroll "bouncy" di sheet Buat Playlist Otomatis, 1 file)** — User
konfirmasi Batch 262 berhasil, lapor scroll terasa bouncy: overscroll stretch-glow bawaan
Android 12+ bertumpuk dgn gesture drag `ModalBottomSheet`. Fix: `Column` dibungkus
`CompositionLocalProvider(LocalOverscrollConfiguration provides null)`, scope cuma Column ini.
API masih terkini utk Compose BOM 2024.05.00 project ini. Brace/paren seimbang. `FILE_MANIFEST.txt`
tidak berubah. **Belum diverifikasi visual di device.** Detail: `CHANGELOG.md` Batch 263.

**Batch 262 (Bug fix urgent — sheet "Buat Playlist Otomatis" truncated & 0 scrollable, 1
file)** — User lapor + screenshot: `Column` dialog `SmartPlaylistScreen.kt` 0
`verticalScroll`, konten (rating+tombol Simpan) ke-clip diam-diam. Root cause & fix persis sama
Batch 112 (`NowPlayingScreen.kt`). Fix: `.verticalScroll(rememberScrollState())`. `LazyRow`
nested di dalamnya dicek dulu (horizontal, 0 konflik axis). Brace/paren seimbang.
`FILE_MANIFEST.txt` tidak berubah. **Belum diverifikasi visual di device.**

**Pending Queue (2 item, ditunda sesuai Strict Micro-Batching)**: (1) sweep-select belum ada di
tab Favorit & Playlist (baru ada di tab Lagu). (2) sensitivitas sweep-select tab Lagu perlu
dikonfigurasi ke standar iOS (user bilang saat ini over-sensitif). Detail: `CHANGELOG.md`
Batch 262.

**Batch 261 (POLISH_AUDIT #8 § Surface/Color item 2 — samakan treatment border/divider lintas
screen, 2 file, 3 bug fix)** — 24 `HorizontalDivider` di 10 file, 20 eksplisit `surfaceVariant`,
3 titik (`DuplicateFinderSheet.kt` x2, `VaultSheet.kt` x1) tanpa color (default M3
`outlineVariant`, token beda) — disamakan. Mayoritas 20:3 jelas + blast radius kecil (2 file) +
0 risiko perilaku → langsung dieksekusi (beda dari observasi Batch 260). Brace/paren seimbang.
`FILE_MANIFEST.txt` tidak berubah (184/184). **Belum diverifikasi visual di device**. Item
berikutnya: audit disabled/selected state lintas screen. Detail: `CHANGELOG.md` Batch 261.

**Batch 260 (POLISH_AUDIT #7 § Surface/Color item 1 — audit background→surface→elevated
surface, 0 code, observasi baru)** — `surfaceContainer*` (M3 elevated token) 0 dipakai, elevasi
manual via `Surface(tonalElevation=...)`. 5 titik "kartu pembungkus" (Home/Library/NowPlaying/
StatsDashboard/Settings) pola identik TAPI `tonalElevation` beda tanpa penjelasan: 2/4/4/6/6dp.
**TIDAK dieksekusi** — observasi tertunda keputusan user (pola sama Batch 162/163/165), blast
radius 5 file, bisa jadi disengaja. `FILE_MANIFEST.txt` tidak berubah (184/184). Item
berikutnya: samakan treatment border/divider lintas screen. Detail: `CHANGELOG.md` Batch 260.

**Batch 259 (POLISH_AUDIT #6 § Responsive/Adaptive — audit statis small/large/landscape/
font-scale, 0 bug baru, 0 code)** — **Batasan jujur**: audit STATIS (grep), sandbox 0 kemampuan
render/emulator, bukan verifikasi visual device. 4 aspek: fixed-width clip-risk (0 ditemukan di
12 file kandidat), long title/artist song row (sudah flexible+marquee/ellipsis), landscape Now
Playing (SUDAH ADA `verticalScroll` safety net Batch 112), font-scale (0 `fontSize` hardcoded,
semua pakai token typography). **0 bug baru**, TIDAK diklaim selesai 100% — kandidat
`MANUAL_QA_CHECKLIST.md` untuk verifikasi visual manual user. `FILE_MANIFEST.txt` tidak berubah
(184/184). Sisa § Responsive cuma guardrail ("jangan ubah layout architecture", bukan task),
lanjut § Surface/Color Consistency batch berikutnya. Detail: `CHANGELOG.md` Batch 259.

**Batch 258 (POLISH_AUDIT #5 — reduced-motion infra check, N/A, § Motion TUNTAS, 0 code)** —
0 infrastruktur reduced-motion ditemukan (`grep` menyeluruh), sesuai instruksi dokumen sumber
TIDAK dibuat baru. **N/A, STOP.** § Motion & Transition sekarang 6/6 tuntas (Batch 254-258).
Kategori berikutnya: § Responsive/Adaptive (small/large phone, landscape, font-scale besar —
WAJIB dicek visual dulu, bukan tebakan). `FILE_MANIFEST.txt` tidak berubah (184/184). Detail:
`CHANGELOG.md` Batch 258.

**Batch 257 (POLISH_AUDIT #4 — animasi vs repeated interaction/scroll cepat, 0 bug + fix
manifest gap, 0 code)** — `animateItemPlacement()` (4 titik) fire cuma pas mutasi list, bukan
scroll-offset; `basicMarquee()` (3 titik) self-gating, independen kecepatan scroll. **0 bug,
STOP.** Ditemukan sekalian: `POLISH_AUDIT.md` belum ada di `FILE_MANIFEST.txt` sejak Batch 253
— ditambahkan (183→184). Item berikutnya § Motion: reduced-motion (cek infra dulu, jangan bikin
baru kalau belum ada). Detail: `CHANGELOG.md` Batch 257.

**Batch 256 (POLISH_AUDIT #3 — fix konsistensi spring swipe-snap `NowPlayingScreen.kt`, 1 file
patch)** — Audit menyeluruh animasi di `ui/*.kt` (cakupan nyata cuma 4 file, bukan 26). Temuan
konkret: spring snap-back swipe-next/previous artwork (~baris 1228/1231) beda stiffness dari
`bouncyPress()` (`Utils.kt`) & entrance spring (~410) yg sama-sama `StiffnessLow` eksplisit. Fix:
tambah `stiffness=Spring.StiffnessLow` ke 2 spring itu — 3 animasi bouncy sekarang 1 sistem.
`dampingRatio` tidak diubah. Brace/paren balance OK (217/217+780/780). Detail: `CHANGELOG.md`
Batch 256.

**Batch 255 (POLISH_AUDIT #2 — verifikasi `MiniPlayerBar.kt:68` + `NowPlayingScreen.kt:301`
tween(700), 0 code diubah)** — Kedua `tween(700)` adalah `animateColorAsState` accent-color
cross-fade saat lagu berganti (bukan respons tap/klik) — sudah konsisten satu sama lain (sama
700ms, sama tujuan), 700ms wajar utk ambient color wash non-interaktif. Trigger cuma pas song
berganti, 0 risiko numpuk di repeated interaction. **0 bug ditemukan, durasi tidak diubah.**
Detail: `CHANGELOG.md` Batch 255.

**Batch 254 (POLISH_AUDIT #1 — verifikasi `LibraryScreen.kt:1345` tween(1100), 0 code diubah)** —
Cek checkbox teratas `POLISH_AUDIT.md` § Motion. Ternyata `tween(1100)` itu `ShimmerBrush()`
(animasi loading-skeleton `infiniteRepeatable`), BUKAN micro-feedback interaktif spt dugaan audit
asli. 1100ms wajar utk siklus shimmer (standar umum 1000-1500ms). **0 bug ditemukan, durasi tidak
diubah** — dicentang `[x]` sesuai aturan "0 bug → STOP". Detail: `CHANGELOG.md` Batch 254.

**Batch 253 (Tanam `POLISH_AUDIT.md` — backlog micro-polish permanen, 1 file baru, 0 code
diubah)** — Permintaan user: tanam audit final micro-polish (5 area: Motion, Responsive,
Surface/Color, Repeated Components, Typography) permanen ke repo + adaptasi ke referensi konkret
project (bukan cuma tempel mentah). Dibuat `POLISH_AUDIT.md` di root — checklist hidup
(descending per-seksi, dicentang progresif tiap batch berikutnya kerja 1 sub-item). Referensi
konkret hasil grep: `LibraryScreen.kt:1345` (`tween(1100)`), `MiniPlayerBar.kt:68` +
`NowPlayingScreen.kt:301` (`tween(700)` accent, 2 titik pola sama). Dicatat eksplisit: project TIDAK
punya shared component library file (`Button.kt`/`Chip.kt`/dst tidak eksis, semua inline di 26
file `ui/*.kt`) — jadi seksi "Repeated Components" audit ini murni visual comparison manual, BUKAN
alasan ekstraksi ke shared composable (itu refactor, dilarang eksplisit di dokumen sumbernya).
`PROJECT_STATE.md`: tambah aturan sesi #4 — `POLISH_AUDIT.md` jadi rujukan default Pending Queue
kalau tidak ada instruksi/log_fail baru. **0 code/logic disentuh** — batch ini murni dokumentasi.

**Batch 252 (Fix build FAILED lanjutan 4/4 — bump Room 2.6.1→2.8.4, 1 file patch)** — Root cause
`log_fail_258.zip`: `[ksp] unexpected jvm signature V` — BUG KSP2 YANG SUDAH DIKENAL (dicek
`web_search`, google/ksp#2957/#2177), muncul krn Room DAO suspend-Unit function diproses KSP2
(aktif sejak Batch 250) pakai Room versi lama. Root cause BUKAN kode project — murni versi
dependency. Fix `app/build.gradle.kts`: Room 2.6.1→2.8.4 (latest stable 2.x, BUKAN Room 3.0 —
breaking rewrite total, di luar scope). Dicek juga 0 KSP-processor lain di project selain Room
(`grep ksp(` 1 match) — 0 lurking issue KSP lain tersisa. Brace/paren balance OK (35/35+156/156).

**⚠️ 4× GAGAL BUILD BERTURUT** (Batch 249→250→251→252: compileSdk/AGP → versi Kotlin → syntax DSL
→ versi Room/KSP2 bug) — semua konsekuensi lompatan besar Kotlin 1.9.24→2.4.10 sekaligus (Batch
250), satu per satu ketauan pas dependency yang belum diaudit kena. **Belum ada konfirmasi BUILD
SUCCESS dari user.** Kalau `log_fail_259` masih muncul: sesi berikutnya WAJIB full-audit SEMUA
dependency lain yang mungkin masih pakai versi lama pra-Kotlin-2.0 (bukan cuma Room), bukan tambal
1-per-1 lagi — cek `grep -n "implementation\|ksp(" app/build.gradle.kts` menyeluruh sekali jalan.
Detail: `CHANGELOG.md` Batch 252.

**Batch 251 (Fix build FAILED lanjutan 3/3 — migrasi kotlinOptions→compilerOptions DSL, 1 file
patch)** — Root cause `log_fail_257.zip`: `android{kotlinOptions{jvmTarget="17"; freeCompilerArgs
+=...}}` jadi HARD ERROR di Kotlin 2.4.10 (Batch 250 kemarin), bukan warning lagi. Fix `app/
build.gradle.kts`: pindah ke top-level `kotlin{compilerOptions{jvmTarget.set(JvmTarget.JVM_17);
freeCompilerArgs.addAll(...)}}`, isi opt-in flags PERSIS sama (0 logic berubah, murni syntax).
Brace/paren balance OK (35/35+150/150).

**Batch 250 (Fix build FAILED lanjutan — Kotlin 2.4.10 + Compose compiler plugin, 2 file patch)**
— Root cause `log_fail_256.zip`: `kspReleaseKotlin`/`kspDebugKotlin` FAILED, `work-runtime-2.11.2`
metadata Kotlin 2.1.0 vs project 1.9.24 (binary incompatible). Fix: `build.gradle.kts` Kotlin
→`2.4.10` (latest stable, 2.4.20 masih RC), KSP →`2.3.10` (decoupled versioning, pairing resmi
kotlinlang.org docs), plugin BARU `org.jetbrains.kotlin.plugin.compose` `2.4.10` (wajib sejak
Kotlin 2.0+). `app/build.gradle.kts`: tambah plugin id tsb + HAPUS `composeOptions.
kotlinCompilerExtensionVersion="1.5.14"` (obsolete total, auto-follow Kotlin sekarang). Brace
balance OK (1/1, 28/28). **2× gagal build berturut** (Batch 249 fix compileSdk/AGP ternyata belum
cukup, root cause kedua ini baru ketauan dari log_fail_256) — confidence diturunkan, **WAJIB
di-build ulang** sebelum dianggap tuntas, jangan asumsikan otomatis fix. Detail: `CHANGELOG.md`
Batch 250.

**Batch 249 (Fix build FAILED — bump AGP/compileSdk/Gradle, 3 file patch, 0 file baru)** — Root
cause dari `log_fail_255.zip`: `work-runtime-ktx:2.11.2` (Batch 246) butuh compileSdk 35+ & AGP
8.6.0+, project masih 34/8.4.1 → `:app:checkDebugAarMetadata` FAILED. Fix: `build.gradle.kts` AGP
→`8.13.0` (bukan lompat ke AGP 9.x — breaking DSL, di luar scope 1-task fix), `app/build.gradle.kts`
`compileSdk` 34→36 (max yg didukung AGP 8.13), `.github/workflows/build.yml` `gradle-version`
8.7→8.14.3 (2 titik). `targetSdk` & Kotlin/KSP version SENGAJA tidak disentuh (di luar scope,
bukan penyebab FAILED). Brace balance OK 3 file. **Belum diverifikasi ulang di CI** — fix berdasar
analisa log + web_search compatibility, bukan hasil re-run sukses. Detail: `CHANGELOG.md` Batch 249.

**Batch 248 (Wire Lyrics offline-first ke NowPlayingScreen, 2 file patch, 0 file baru)** —
Menutup gap "Belum di-wire" yang dicatat eksplisit Batch 245/247. `NowPlayingScreen.kt`: hoist
`LyricsViewModel` (factory pattern, konsisten `EqualizerController` dkk) + `LaunchedEffect(song?.id)`
→ `loadLyrics(artist,title,album)` tiap ganti lagu, `lyricsAutoState` diteruskan ke `LyricsSheet`
via param baru `autoUiState`. `LyricsSheet.kt`: param `autoUiState: LyricsUiState? = null`
(default null, source-compatible). Default `editing` diubah `rawLyrics.isNullOrBlank()` → `false`
(dulu lompat ke textbox edit kalau manual kosong; sekarang tampilkan auto-fetch dulu kalau ada).
Branch baru: manual kosong + auto Found/Loading → `LyricsStateView`; manual kosong + auto
NotFound/Idle/null → fallback lama ("Belum ada lirik" + tombol "Tambah Lirik", 0 regresi). Lirik
manual SELALU menang kalau ada — auto cuma fallback tampilan, 2 sumber data tetap terpisah (tidak
auto-save ke DB manual). Tombol Edit header jadi satu-satunya jalur override manual. Brace/paren
2 file seimbang. 0 protected asset. **Belum diverifikasi compile/device**. Detail: `CHANGELOG.md`
Batch 248.

**🎉 Kategori Lyrics offline-first (Batch 243-248) SEKARANG BENAR-BENAR TUNTAS termasuk wiring UI**
— sebelumnya Batch 247 nutup 4/4 tapi item wiring NowPlayingScreen sengaja ditunda (di luar scope
4 batch itu), sekarang ditutup di batch ini.

**Batch 247 (Lyrics offline-first 4/4b — Store toggle + 2 menu Settings, batch TERAKHIR
kategori Lyrics offline-first, 1 file baru + 2 file patch + 2 dokumentasi)** — Lanjutan Batch
246. `data/lyrics/LyricsPrefetchStore.kt` (baru, boilerplate identik `SilenceSkipStore.kt`,
boolean "Prefetch Saat WiFi" — **default ON**, sengaja beda dari toggle playback-behavior lain
yang semua default OFF, alasan: fitur ini WiFi-only 0 dampak kuota/pemutaran, sedangkan toggle
lain mengubah perilaku pemutaran yang user rasakan langsung). `PlaybackService.kt` (patch):
`onMediaItemTransition` sekarang guard `if (LyricsPrefetchStore(this).isEnabled())` sebelum
`LyricsPrefetchWorker.enqueue()` — dibaca ulang tiap transition (bukan di-cache di field
Service), toggle baru langsung berlaku transisi berikutnya. `SettingsScreen.kt` (patch): 2 item
baru — "Prefetch Lirik Saat WiFi" jadi switch ke-5 di grup "Perilaku Pemutaran" (row-species
switch, BUKAN nav-row "Alat & Utilitas" — sengaja dipisah situ, konsisten pembagian row-species
yang diaudit Batch 217/218), "Hapus Cache Lirik" jadi nav-row ke-5 di "Alat & Utilitas" +
`AlertDialog` konfirmasi (pola identik `showDisableLockConfirm`) → `LyricsRepository.clearCache()`
+ `onInfoMessage()`. State toggle & context dibaca LANGSUNG via `LocalContext`/Store (pola sama
Vault/Duplicate/Backup di file yang sama), TIDAK di-hoist ke `MainActivity.kt` — 0 protected
asset disentuh. Brace/paren 3 file seimbang. `FILE_MANIFEST.txt` 182→183.

**🎉 Kategori Lyrics offline-first TUNTAS 4/4 (Batch 243-247)**: Room cache (243) → Retrofit
LRCLIB API (244) → Repository+ViewModel+View offline-first (245) → Worker prefetch WiFi (246) →
Store toggle+menu Settings (247). **Belum di-wire ke NowPlayingScreen** (`LyricsView`/
`LyricsViewModel` belum dipanggil dari layar Now Playing manapun — item terbuka terpisah, BUKAN
bagian scope 4 batch ini yang eksplisit cuma trigger+worker+Settings menu, lihat catatan Batch
245). **Belum diverifikasi compile/device** seperti biasa.

**Batch 246 (Lyrics offline-first 4/4a — Worker prefetch, 2 file kode + 1 file baru + gradle
protected-parsial + 2 dokumentasi)** — Lanjutan Batch 245. **Direvisi jadi 4/4a/4/4b** (Pending
Queue lama membundel Worker+Store+2-menu Settings jadi 1 batch — 4 file kode, lewat batas Maks 3
File; dipecah, sisa di bawah). `worker/LyricsPrefetchWorker.kt` (baru): `CoroutineWorker` baca
`PlaybackStateStore.load()` (infrastruktur SUDAH ADA sejak Batch 108, playback resumption) buat
tahu 10 lagu depan tanpa perlu pegangan ke ExoPlayer langsung (Worker bisa jalan walau proses app
sudah mati), resolve ke `Song` via `MusicRepository.getSongsByIds()`, `repository.ensureCached()`
per lagu (cek `isStopped` tiap iterasi). `enqueue()` companion: `Constraints.NetworkType.UNMETERED`
+ `enqueueUniqueWork(REPLACE)`. Dipanggil dari `PlaybackService.onMediaItemTransition` (patch,
bukan `MediaSessionCompat.Callback` lama seperti draft Pending Queue — project ini pakai Media3
`Player.Listener`, sudah ada listener yg sama dipakai widget-update). **Catatan akurasi jujur**:
window "10 lagu depan" dibaca dari `PlaybackStateStore` yang di-save PERIODIK ~5 detik oleh
`PlayerViewModel` (BUKAN sinkron di titik transition ini) — kalau Worker kebetulan run instan
(sudah WiFi), window bisa 1 index ketinggalan. Efeknya ringan (prefetch geser 1 lagu, bukan salah
total), didokumentasikan sebagai known-limitation, bukan dikejar sempurna di batch ini. Gradle:
`work-runtime-ktx:2.11.2` (dicek `web_search` — rekomendasi resmi terbaru per Agustus 2026, bukan
tebakan training data). Brace/paren 3 file seimbang. 0 protected asset app disentuh (cuma gradle).

**Bonus temuan (di luar scope diminta, low-risk high-value)**: `FILE_MANIFEST.txt` SUDAH DRIFT
dari disk SEBELUM batch ini disentuh — 8 file lyrics (Batch 243-245: `LyricsRepository.kt`,
`LyricsApi.kt`, `LyricsDto.kt`, `LyricsDao.kt`, `LyricsDatabase.kt`, `LyricsEntity.kt`,
`LyricsView.kt`, `LyricsViewModel.kt`) tidak pernah tercatat sesi-sesi sebelumnya. Diperbaiki
sekaligus (173→182, termasuk 1 file baru batch ini) — 182/182 match disk terverifikasi.

**Pending Queue — Lyrics offline-first 4/4b (batch terakhir kategori ini)**: `data/lyrics/
LyricsPrefetchStore.kt` (baru, boilerplate identik `SilenceSkipStore.kt` — boolean "Prefetch
Saat WiFi", default ON) + patch `SettingsScreen.kt` (2 menu baru: "Hapus Cache Lirik" →
`repository.clearCache()` + konfirmasi dialog, "Prefetch Saat WiFi" toggle → baca/tulis store
tsb, OFF = jangan `LyricsPrefetchWorker.enqueue()` dari `PlaybackService`) — makanya
`PlaybackService.kt` KEMUNGKINAN kena sentuh sekali lagi (guard `if (store.isEnabled())` sebelum
`enqueue()`). Detail: `CHANGELOG.md` Batch 246.

**Batch 245 (Lyrics offline-first 3/4 — Repository+ViewModel+View, 3 file kode + 2
dokumentasi)** — Lanjutan Batch 244. `LyricsRepository.kt` (`sealed class LyricsResult
{Found/NotFound}`, logic offline-first PERSIS spec: `dao.get()`→null?`api.getLyrics()`→map→
`dao.upsert()`→return; hit→return cache 0 network; `ensureCached()` khusus dipanggil
PrefetchWorker batch 4/4 — cache-check dulu tanpa expose hasil; `IOException`/exception apa pun
→ `NotFound`, tidak crash) → `LyricsViewModel.kt` (`StateFlow<LyricsUiState>` 4 state
Idle/Loading/Found/NotFound; **debounce 5 detik + skip-lagu-sama diimplementasi DI SINI**,
bukan di PlaybackService pemanggil batch 4/4 — 1 sumber kebenaran, PlaybackService cukup
panggil `loadLyrics()` polos) → `LyricsView.kt` (`parseLRC()` regex `[mm:ss.xx]text`, xx=
centisecond ×10 bukan ×1 spt millisecond; `activeLyricIndex()` cari baris terakhir
timeMs≤posisi; `LazyColumn`+`animateScrollToItem` autoscroll; baris aktif bold+ukuran beda
BUKAN cuma warna, konsisten aturan Batch 241; fallback plain→pesan "Lirik tidak ditemukan";
`LyricsStateView` wrapper terima `LyricsUiState` langsung). **Belum di-wire ke NowPlayingScreen**
(integrasi UI di luar 10 file yg diminta spec — sengaja, scope batch 4/4 cuma trigger+worker+
Settings menu). Brace/paren balance OK 3 file (1 titik `)` di komentar `LyricsView.kt`
notasi interval matematika `[start,end)` — false-positive checker karakter, bukan kode, sudah
diverifikasi manual). 0 protected asset disentuh.

**Pending Queue — Lyrics offline-first 4/4 (batch terakhir kategori ini)**:
`worker/LyricsPrefetchWorker.kt` (WorkManager `Constraints.NetworkType.UNMETERED`, 10 lagu
depan queue ExoPlayer, `repository.ensureCached()` per lagu — skip yg sudah ke-cache otomatis
via cek internal) + gradle `work-runtime-ktx` + patch `PlaybackService.kt` (dengarkan
`MediaSessionCompat.Callback.onMetadataChanged()`, panggil `lyricsViewModel.loadLyrics()`) + 2
menu baru `SettingsScreen.kt` ("Hapus Cache Lirik" → `repository.clearCache()`, "Prefetch Saat
WiFi" toggle) + `WorkManager.enqueueUniqueWork("prefetch_lyrics", ...)`. Detail: `CHANGELOG.md`
Batch 245.

**Batch 244 (Lyrics offline-first 2/4 — Retrofit API layer LRCLIB, 2 file kode + gradle
protected-parsial + 2 dokumentasi)** — Lanjutan Batch 243. `LyricsDto.kt` (bentuk respons 1:1
LRCLIB `/api/get`, 0 logic, mapping ke `LyricsEntity` didelegasikan ke Repository batch
berikutnya) + `LyricsApi.kt` (Retrofit interface, endpoint `GET api/get?artist_name=&
track_name=` — persis spec, bukan `/api/search` yg ambiguous multi-hasil). Timeout 10s
(connect+read, spec) — SENGAJA beda dari `GitHubReleaseChecker` (15s/20s) krn ini dipanggil di
jalur pemutaran lagu (`onMetadataChanged`), gagal cepat > nunggu lama. Header
`User-Agent: MusicApp/1.3 Hybrid` via `Interceptor` (persis spec). Gradle (protected-parsial):
`app/build.gradle.kts` +retrofit2/converter-gson 2.11.0 (okhttp3 sudah ada dari
UpdateDownloader/GitHubReleaseChecker, numpang instance-pattern yg sama, bukan http client
baru). Brace/paren balance (`LyricsApi.kt` 3/3+26/26, `LyricsDto.kt` 0/0+7/7). 0 protected asset
app disentuh (cuma gradle).

**Pending Queue — Lyrics offline-first (batch berikutnya)**:
3. `data/lyrics/LyricsRepository.kt` (offline-first: `dao.get()` → null? `api.getLyrics()` →
   map DTO→Entity → `dao.upsert()` → return; hit? return cache, 0 network; API gagal & cache
   kosong → emit "Lirik tidak ditemukan") + `ui/lyrics/LyricsViewModel.kt`
   (`StateFlow<LyricsUiState>`, debounce 5 detik, skip kalau lagu sama) + `ui/lyrics/
   LyricsView.kt` (`parseLRC()`/`updateTime()`/autoScroll Composable, fallback plain text).
4. `worker/LyricsPrefetchWorker.kt` (WorkManager Unmetered, 10 lagu depan queue ExoPlayer, skip
   yg sudah ke-cache) + gradle work-runtime-ktx + patch `PlaybackService.kt` (trigger
   `onMetadataChanged`, debounce 5s) + 2 menu `SettingsScreen.kt` ("Hapus Cache Lirik",
   "Prefetch Saat WiFi") + `WorkManager.enqueueUniqueWork`. Detail: `CHANGELOG.md` Batch 244.

**Batch 243 (FITUR BARU — Lyrics offline-first 1/4: Room cache layer, 3 file kode + gradle
protected-parsial + 2 dokumentasi) → 🆕 LYRICS OFFLINE-FIRST dimulai (menyela antrean Motion &
Transition, ditunda user — urgent)** — Spec user: cache lirik LRCLIB (LRC synced+plain),
offline-first (cache dulu baru API), prefetch WiFi, trigger dari playback service. **Adaptasi
arsitektur (izin eksplisit user "boleh diadaptasi")**: spec minta Hilt DI, TAPI codebase ini
0% pakai DI framework (semua `data/*Store.kt` manual-construct dari Context, `PlayerViewModel`
manual Factory) — nambah Hilt cuma demi 1 fitur berarti nyentuh Application/MainActivity/
PlaybackService (3 protected asset) demi framework baru. Diganti singleton manual
(`LyricsDatabase.getInstance()`), pola sama persis konvensi existing, 0 protected asset app
disentuh (cuma gradle). Nama file/class disesuaikan realita project: **`PlaybackService.kt`**
(bukan `PlayerService.kt` — nama itu tidak ada di project ini).

File batch ini (3, HARD CAP): `data/lyrics/db/LyricsEntity.kt` (table `lyrics_cache`, index
UNIQUE(artist,title), kolom persis spec), `LyricsDao.kt` (get suspend + observe Flow + upsert
REPLACE + clearAll + count), `LyricsDatabase.kt` (singleton, DB name `lyrics_database.db`,
exportSchema=false — Room pertama kali di project ini, 0 folder schema/ existing yg kebentur).
Gradle (protected-parsial): root `build.gradle.kts` +KSP plugin 1.9.24-1.0.20 (KSP dipilih atas
kapt — modern, sesuai aturan sesi #3), `app/build.gradle.kts` +room-runtime/room-ktx/
room-compiler(ksp) 2.6.1. Brace/paren balance semua 3 file OK (lihat CHANGELOG).

**Pending Queue — Lyrics offline-first (batch berikutnya, urutan sesuai dependency)**:
2. `data/lyrics/api/LyricsDto.kt` + `LyricsApi.kt` (Retrofit interface LRCLIB, timeout 10s,
   User-Agent "MusicApp/1.3 Hybrid") + gradle retrofit/okhttp-logging (okhttp sudah ada).
3. `data/lyrics/LyricsRepository.kt` (offline-first logic: dao.get() → null? fetch API →
   dao.upsert() → return; cache hit → return langsung) + `ui/lyrics/LyricsViewModel.kt`
   (StateFlow<LyricsUiState>, debounce 5s, skip kalau lagu sama) + `ui/lyrics/LyricsView.kt`
   (parseLRC/updateTime/autoScroll Composable, fallback plain text, pesan "Lirik tidak
   ditemukan" kalau API gagal & cache kosong).
4. `worker/LyricsPrefetchWorker.kt` (WorkManager, constraint Unmetered, 10 lagu depan dari
   ExoPlayer queue, skip yg sudah ke-cache) + gradle work-runtime-ktx + patch
   `PlaybackService.kt` (trigger onMetadataChanged, debounce 5s) + 2 menu Settings ("Hapus
   Cache Lirik", "Prefetch Saat WiFi") + panggilan `WorkManager.enqueueUniqueWork`.

**⚠️ CATATAN SESI BARU**: mulai batch ini, fitur "Lyrics" pakai konvensi nama `PlaybackService`
(bukan `PlayerService`) — kalau user sebut "PlayerService" di prompt lanjutan, itu maksudnya
`playback/PlaybackService.kt`. Detail: `CHANGELOG.md` Batch 243.

**Batch 242 (Accessibility Micro-Polish 9/9 — verifikasi retrospektif no-behavior-change, 0 bug,
0 kode + 2 dokumentasi) → 🟠 ACCESSIBILITY MICRO-POLISH TUNTAS 9/9 (Batch 234-242)** — Item
terakhir bersifat guard-rail, audit retrospektif 6 fix kode Batch 234-241: 234/236 (handler
pindah ke parent `.selectable`, `onClick` child jadi `null` — semantics-only, `onClick` logic
sama persis cuma naik level)/237 (nambah navigasi fokus imeAction, 0 logic baru)/238 (ukuran
touch target, 0 logic)/241 (nambah icon visual, 0 onClick baru). **Hasil: 0 pelanggaran**, 0
kode disentuh. `MICRO_UIUX_AUDIT.md` diupdate (checklist 9/9 + `STATUS TRACKING`). **Rekap
kategori Accessibility Micro-Polish**: TalkBack semantics(234)/content desc(235)/semantic
role(236)/focus order(237)/touch target(238)/text scaling(239, 0 bug)/contrast(240, 0
bug)/color-only info(241)/no-behavior-change(242, 0 bug). Kategori berikutnya (belum mulai):
🟡 MOTION & TRANSITION. Detail: `CHANGELOG.md` Batch 242.


**Batch 241 (Accessibility Micro-Polish 8/9 — informasi tidak boleh cuma dibedakan lewat warna,
1 bug fix, 1 file kode + 2 dokumentasi)** — `PlaylistSongRow` (`PlaylistScreen.kt`) beda pola
dari 2 saudaranya: status "sedang diputar" di `QueueRow` (`QueueSheet.kt`) & `SongRow`
(`LibraryScreen.kt`) sama-sama pakai badge icon `GraphicEq` + teks bold + warna primary, TAPI
`PlaylistSongRow` cuma pakai background tint 12% alpha + bold + warna primary — 0 icon/teks,
murni warna (masalah klasik utk low-vision/color-blind user, background tint 12% alpha apalagi
sangat subtle). Fix: tambah `Icon(Icons.Default.GraphicEq, tint = secondary, 16dp)` +
`contentDescription = "Sedang diputar"` persis pola 2 file lain, dibungkus `Row` horizontal di
atas title. Brace/paren balance (129/129, 218/218). `MICRO_UIUX_AUDIT.md` diupdate. Item
berikutnya (9/9, penutup kategori Accessibility Micro-Polish): audit dynamic type / RTL layout
mirroring. Detail: `CHANGELOG.md` Batch 241.

**Batch 240 (Accessibility Micro-Polish 7/9 — audit contrast, 0 bug, 0 kode + 2 dokumentasi)** —
Cek app-wide: (1) `ResultBanner` (3 varian: Solid/Tinted/Bare) — Solid pasang container-role +
`onXContainer` pair (kontras terjamin M3), Tinted & Bare sengaja pass warna semantik sama ke
`containerColor`+`contentColor` TAPI itu BY DESIGN (Bare: containerColor tidak dipakai sama
sekali, 0 background; Tinted: containerColor cuma jadi tint 15% alpha, contentColor teks solid
di atasnya — pola standar "tinted alert banner"), bukan bug. (2) Seluruh `onPrimary`/`onTertiary`
di `Theme.kt` per varian tema sudah py komentar luminance-check manual (mis. luma≈0.49/0.61
dibandingkan threshold) — bukan tebakan. (3) `.alpha(0.4f)` disabled-state di `LockScreen.kt`
(PinKey/RoundGlyphButton) — WCAG mengecualikan kontrol disabled dari syarat rasio kontras, bukan
pelanggaran. (4) 0 titik `Color.Gray`/hardcoded hex dipakai sbg warna teks (bypass jaminan
kontras tema), 0 titik role `outline`/`outlineVariant` (didesain low-contrast utk border) disalah
gunakan sbg warna teks — seluruh titik `surfaceVariant` yang ditemukan adalah `HorizontalDivider`
(dekoratif, bukan teks) atau `Slider` track. **Hasil: 0 bug**, 0 kode disentuh.
`MICRO_UIUX_AUDIT.md` diupdate. Item berikutnya (8/9): pastikan informasi penting tidak hanya
dibedakan lewat warna. Detail: `CHANGELOG.md` Batch 240.

**Batch 239 (Accessibility Micro-Polish 6/9 — audit text scaling, 0 bug, 0 kode + 2
dokumentasi)** — Cek app-wide: (1) semua `fontSize`/`lineHeight` di `Type.kt` & seluruh
composable pakai unit `.sp` (0 titik pakai `.dp` buat ukuran teks — bug klasik yang bikin teks
tidak ikut scaling sistem), (2) widget `widget_player.xml`/`widget_player_compact.xml` juga
`sp`, (3) `AndroidManifest.xml` tidak override `configChanges` yang skip font-scale (activity
recreate default, state Compose aman), (4) 0 titik `TextOverflow.Clip`/`softWrap = false` (yang
ada cuma `Ellipsis`/`basicMarquee`, keduanya graceful di font besar), (5) grep seluruh
`.height()`/`.width()` fixed dgn `Text` bersebelahan — semua ternyata `Spacer` atau container
scrollable (`LazyColumn.heightIn`) yang aman, 0 fixed-height non-scroll pembungkus teks yang
bisa clip. **Hasil: 0 bug**, 0 kode disentuh. `MICRO_UIUX_AUDIT.md` diupdate. Item berikutnya
(7/9): audit contrast. Detail: `CHANGELOG.md` Batch 239.

**Batch 238 (Accessibility Micro-Polish 5/9 — minimum touch target StarRatingRow, 1 bug fix, 1
file kode + 2 dokumentasi)** — `StarRatingRow` (`NowPlayingScreen.kt`, 5 `IconButton` rating
bintang) override eksplisit `modifier = Modifier.size(32.dp)` pada `IconButton` — DI BAWAH
minimum touch target 48dp (default Material `IconButton` kalau tidak di-override). Glyph
bintang sendiri cuma 20dp (oke, cuma ukuran visual), tapi area sentuh keseluruhan yang dipangkas
ke 32dp — target kecil utk jari, apalagi 5 star berjejer rapat. Fix: hapus override
`.size(32.dp)`, biarkan `IconButton` pakai default 48dp (glyph `Icon` 20dp di dalamnya tidak
disentuh, tetap sama). Row full-width di Now Playing screen (bukan dialog sempit) — 5×48dp=
240dp muat leluasa, tidak overflow. Brace/paren balance (215/215, 767/767).
`MICRO_UIUX_AUDIT.md` diupdate. Item berikutnya (6/9): audit text scaling. Detail:
`CHANGELOG.md` Batch 238.

**Batch 237 (Accessibility Micro-Polish 4/9 — focus order form multi-field, 1 bug fix, 1 file
kode + 2 dokumentasi)** — `SongInfoEditSheet.kt` (form edit metadata, 8 `OutlinedTextField`:
Judul/Artis/Album/Artis Album/Genre/Komposer/No.Track/No.Disc) tidak punya `imeAction`/
`KeyboardActions` sama sekali — tiap field default `ImeAction.Done`, tiap kali user tekan tombol
next di keyboard, keyboard malah TERTUTUP alih-alih pindah ke field berikutnya. User harus tap
manual tiap field satu-satu, TalkBack/keyboard-only user makin terhambat. Fix: field 1-6
(vertikal) → `ImeAction.Next` + `focusManager.moveFocus(FocusDirection.Down)`; No.Track (dalam
`Row` horizontal) → `Next` + `FocusDirection.Right` ke No.Disc; No.Disc (field terakhir) →
`ImeAction.Done` + `focusManager.clearFocus()`. Brace/paren balance (49/49, 125/125).
`MICRO_UIUX_AUDIT.md` diupdate. Item berikutnya (5/9): audit minimum touch target. Detail:
`CHANGELOG.md` Batch 237.

**Batch 236 (Accessibility Micro-Polish 3/9 — semantic role list pilihan tema, 1 bug fix, 1
file kode + 2 dokumentasi)** — `ThemeOptionCard` (`SettingsScreen.kt`, list "Identitas Tema" —
single-choice pilih 1 dari beberapa identitas tema) pakai `Surface.clickable()` polos: TalkBack
cuma baca "double tap to activate", tidak ada indikasi ini bagian dari grup pilihan-tunggal
ataupun status terpilih/tidak (padahal visual border 2dp `primary` menandakan `selected`).
Fix: `.clickable(onClick)` → `.selectable(selected, onClick, role = Role.RadioButton)` — role +
selected state sekarang terbaca TalkBack ("radio button, dicentang/tidak, [nama tema]"), tanpa
mengubah visual (masih border-based, bukan literal RadioButton widget — identitas tema
divisualkan lewat card preview, bukan icon). Brace/paren balance (136/136, 436/436).
`MICRO_UIUX_AUDIT.md` diupdate. Item berikutnya (4/9): audit focus order. Detail:
`CHANGELOG.md` Batch 236.

**Batch 235 (Accessibility Micro-Polish 2/9 — decorative icon static contentDescription, 1 bug
fix, 1 file kode + 2 dokumentasi)** — Icon peredam volume dalam aplikasi (`NowPlayingScreen.kt`,
di atas `Slider`) genuinely decorative (non-clickable, sudah ada Text label sibling "Peredam
Dalam Aplikasi (bukan volume HP)" di atasnya), TAPI pakai `contentDescription` string statis
("Peredam dalam aplikasi") padahal glyph-nya berubah (mute/rendah/tinggi) — TalkBack baca stop
tambahan yang redundan & tidak merefleksikan state. Fix: → `null`, konsisten dgn konvensi
decorative-icon+text-sibling (Batch 230). Brace/paren balance (215/215, 768/768).
`MICRO_UIUX_AUDIT.md` diupdate. Item berikutnya (3/9): audit semantic role. Detail:
`CHANGELOG.md` Batch 235.

**Batch 234 (Accessibility Micro-Polish 1/9 — TalkBack semantics RadioButton row, 1 bug fix, 1
file kode + 2 dokumentasi) → 🟡 ACCESSIBILITY MICRO-POLISH dimulai** — 2 titik di
`NowPlayingScreen.kt` (speed selector dialog & transition-mode option) pakai pola `Row.clickable`
+ `RadioButton(onClick=...)` nested — TalkBack berhenti 2x per baris (Row lalu child
RadioButton), role tidak terbaca sebagai radio button di level Row. Fix: Row → `Modifier
.selectable(selected, onClick, role = Role.RadioButton)`, `RadioButton` jadi `onClick = null`
(visual-only, semantics diambil alih parent) — 1 fokus TalkBack per baris, role+selected state
terbaca benar. Brace/paren balance (215/215, 768/768). `MICRO_UIUX_AUDIT.md` diupdate. Item
berikutnya (2/9): audit content descriptions. Detail: `CHANGELOG.md` Batch 234.

**Batch 233 (HOTFIX — import Icons.Error hilang, regresi Batch 228, 2 file kode + 2
dokumentasi)** — CI build gagal (`compileReleaseKotlin`/`compileDebugKotlin`, dari
`log_fail_251.zip` user): `Unresolved reference: Error` di `BackupRestoreSheet.kt`/
`DiagnosticLogSheet.kt`. Root cause: Batch 228 ganti kode `ErrorOutline`→`Error` tapi lupa
update import (codebase pakai explicit per-icon import, bukan wildcard). Fix: import
`ErrorOutline`→`Error` di kedua file. Brace/paren balance (33/33+89/89, 16/16+68/68). Safety
net: scan app-wide seluruh `Icons.Default.X` vs import — 0 gap lain. **⚠️ CATATAN SESI**: mulai
batch ini, WAJIB cross-check import statement setiap kali ganti nama icon (`Icons.Default.X`→
`Y`), bukan cuma ganti di body kode. Detail: `CHANGELOG.md` Batch 233.

**Batch 232 (Iconography 7/7 penutup — verifikasi retrospektif no-cosmetic-affordance-change,
0 bug, 0 kode + 2 dokumentasi) → 🟠 ICONOGRAPHY TUNTAS 7/7 (Batch 224-232)** — Item terakhir
bersifat guard-rail, dieksekusi sbg audit retrospektif 4 fix kode Batch 224-231: ukuran(224)/
offset posisi(226-227)/ganti glyph `ErrorOutline`→`Error`(228, dasar konsistensi bobot bukan
estetika)/tint(229, affordance diperjelas bukan dikaburkan)/teks label(231, 0 icon visual
berubah). **Hasil: 0 pelanggaran ditemukan**, 0 kode disentuh. `MICRO_UIUX_AUDIT.md` diupdate
(checklist 7/7 + `STATUS TRACKING`). **Rekap kategori Iconography**: ukuran(224, 1 fix)/optical
alignment(226-227, 1 fix)/visual weight(228, 1 fix)/action-vs-decorative(229, 1 fix)/
contentDescription null(230, 0 bug)/semantic label(231, 1 fix)/no-cosmetic-change(232, 0 bug).
Kategori berikutnya (belum mulai): 🟡 ACCESSIBILITY MICRO-POLISH. Detail: `CHANGELOG.md`
Batch 232.

**Batch 231 (Iconography 6/7 — semantic label actionable icon, 1 bug fix, 1 file kode + 2
dokumentasi)** — Tombol Shuffle & Repeat (3-state) di `NowPlayingScreen.kt` pakai label statis
("Acak"/"Ulangi"), status cuma dibedakan lewat tint (tidak terbaca TalkBack); Repeat lebih
parah krn glyph OFF vs ALL identik. Fix: label ikut state ("Acak: aktif/nonaktif", "Ulangi:
mati/semua lagu/satu lagu"). Brace/paren balance (216/216, 769/769). `MICRO_UIUX_AUDIT.md`
diupdate. Item berikutnya (7/7, penutup kategori Iconography): jangan mengganti icon hanya
demi estetika jika mengubah affordance. Detail: `CHANGELOG.md` Batch 231.

**Batch 230 (Iconography 5/7 — contentDescription null hanya utk decorative, 0 bug, 0 kode + 2
dokumentasi)** — Grep 69 titik `contentDescription = null` app-wide: semua genuinely
decorative (icon+Text sibling di Button/NavigationBarItem/AlertDialog/ListItem, atau badge
status murni). Cross-check terpisah: 0 titik `IconButton` icon-only (tanpa label teks) pakai
null ditemukan (window ±12 baris tiap `IconButton(`) — semua actionable icon-only sudah punya
deskripsi string. **Hasil: 0 bug**, 0 kode disentuh. `MICRO_UIUX_AUDIT.md` diupdate. Item
berikutnya (6/7): semua actionable icon harus memiliki semantic/content label yang sesuai.
Detail: `CHANGELOG.md` Batch 230.

**Batch 229 (Iconography 4/7 — action vs decorative icon, 1 bug fix, 2 file kode + 2
dokumentasi)** — Konvensi codebase: decorative icon pakai `secondary`, `primary` reserved
utk actionable. Bug: badge status "Sedang diputar" (`GraphicEq`, 0 onClick) di `QueueSheet.kt`
& `LibraryScreen.kt` pakai `primary` — ambigu seolah tappable. Fix: → `secondary`, samakan
drag-handle di atasnya. Brace/paren balance (40/40+131/131, 336/336+725/725).
`MICRO_UIUX_AUDIT.md` diupdate. Item berikutnya (5/7): `contentDescription = null` hanya utk
icon benar-benar decorative. Detail: `CHANGELOG.md` Batch 229.

**Batch 228 (Iconography 3/7 — samakan visual weight icon sejenis, 1 bug fix, 2 file kode + 2
dokumentasi)** — Grep 4 call site `ResultBanner(...)` (pola banner sukses/gagal): 2/4 titik
(`BackupRestoreSheet.kt`, `DiagnosticLogSheet.kt`) pasangkan `CheckCircle` (solid) dengan
`ErrorOutline` (garis tipis) — beda bobot visual dari 2 titik lain (`SignatureMatcherSheet.kt`,
`UpdateCheckSheet.kt`) yang konsisten solid (`CheckCircle`+`Error`). Fix: `ErrorOutline` →
`Error` di kedua file. Brace/paren balance (33/33+89/89, 16/16+68/68). `MICRO_UIUX_AUDIT.md`
diupdate. Item berikutnya (4/7): pastikan action icon dapat dibedakan dari decorative icon.
Detail: `CHANGELOG.md` Batch 228.

**Batch 227 (Iconography 2/7 penutup — fix HomeScreen.kt, 1 bug fix, 1 file kode + 2
dokumentasi) → 🟠 Optical alignment TUNTAS 4/4 titik (Batch 226-227)** — Menutup Pending Queue
Batch 226. `HomeScreen.kt` tombol "Lanjutkan" pakai PlayArrow selalu (bukan toggle), offset
+1dp diterapkan tetap. Brace/paren balance (67/67, 202/202). `MICRO_UIUX_AUDIT.md` diupdate.
Item berikutnya (3/7): samakan visual weight icon sejenis. Detail: `CHANGELOG.md` Batch 227.

**Batch 226 (Iconography 2/7 — audit optical alignment, 1 bug fix, 3 file kode + 2 dokumentasi)**
— Grep app-wide `Icons.Default.PlayArrow`: 4 titik (`NowPlayingScreen.kt`, `MiniPlayerBar.kt`,
`LyricsSheet.kt`, `HomeScreen.kt` "Lanjutkan"). Bug: glyph segitiga PlayArrow condong optik ke
kiri dalam bounding box (beda dari Pause yang simetris) — pas `AnimatedContent` swap Play↔Pause
di posisi sama, PlayArrow kelihatan kegeser kiri dari pusat tombol. Fix: `Modifier.offset(x =
1.dp)` kondisional, aktif HANYA saat PlayArrow, Pause tidak disentuh. **3/4 titik dikerjakan**
(`NowPlayingScreen.kt`/`MiniPlayerBar.kt`/`LyricsSheet.kt`) — brace/paren balance semua
(215/215+762/762, 12/12+96/96, 63/63+165/165). **Pending Queue: `HomeScreen.kt` tombol
"Lanjutkan"** (fix sama, offset+1dp saat PlayArrow) — ditunda ke batch berikutnya sesuai cap 3
file/batch. Belum diverifikasi visual device asli. `MICRO_UIUX_AUDIT.md` diupdate (checklist
2/7). Item berikutnya (3/7): samakan visual weight icon sejenis. Detail: `CHANGELOG.md` Batch
226.

**Batch 225 (verifikasi visual Batch 224 — Play/Pause icon fix, 0 kode + 2 dokumentasi)** —
Screenshot user (tema Tactile/Skeu, squircle) mengonfirmasi: icon Play/Pause (40dp) sekarang
jelas lebih besar dari Skip Previous/Next (36dp), hierarki visual 3-tingkat benar, 0 distorsi/
kepenuhan dalam squircle accent. `MICRO_UIUX_AUDIT.md` & `CHANGELOG.md` Batch 224 diupdate
status jadi terverifikasi. Item berikutnya (Iconography 2/7): audit optical alignment. Detail:
`CHANGELOG.md` Batch 225.

**Batch 224 (Iconography item 1/7 — audit ukuran icon, 1 bug fix, 1 file kode + 2 dokumentasi)**
— Kategori baru (Settings TUNTAS 9/9 di Batch 223). Grep 113 titik `Icon(` app-wide: 104 default
24dp (baseline), 9 custom size — 8/9 justified (TextButton/FilterChip icon+teks beda konvensi,
`LockScreen` disamakan sadar Batch 147, `FeatureHintBanner` hit-target vs visual size sejak
Batch 141). **1 bug nyata**: Play/Pause `NowPlayingScreen.kt` (kontainer terbesar di row, 68dp)
glyph cuma 34dp — LEBIH KECIL dari Skip Previous/Next yang mengapitnya (36dp eksplisit),
membalik hierarki 3-tingkat bobot visual yang seharusnya Shuffle/Repeat(24) < Skip(36) <
Play/Pause(terbesar). 0 komentar histori soal angka 34dp spesifik (beda dari kontainernya sendiri
yang penuh komentar per-batch) — ciri oversight. Fix: 34dp→40dp, hierarki 24<36<40 dipulihkan,
padding internal circle tetap lega (28dp total ruang). Brace/paren `NowPlayingScreen.kt`
seimbang (215/215, 756/756). **Belum diverifikasi visual** — prioritas cek proporsi icon dalam
lingkaran accent di device asli. `MICRO_UIUX_AUDIT.md` diupdate (checklist item 1/7 + `STATUS
TRACKING`). Item berikutnya (2/7): audit optical alignment. Detail: `CHANGELOG.md` Batch 224.

**Batch 223 (Settings polish item 9/9 — verifikasi fungsi setting tidak berubah, 0 kode + 2
dokumentasi) → 🟠 SETTINGS TUNTAS 9/9 (Batch 215-223)** — Item penutup kategori: ditelusuri
wiring `MainActivity.kt` → `SettingsScreen(...)`, seluruh 9 callback tetap terpasang fungsi
`PlayerViewModel` yang sama persis, 0 baris `MainActivity.kt` tersentuh sepanjang siklus 9 batch
ini. Cuma `SettingsScreen.kt` pernah diedit (216: subtitle; 220: `AlertDialog` konfirmasi).
Batch 220 satu-satunya yang sentuh alur eksekusi, tapi `onDisableLock()` tetap dipanggil dgn
efek identik saat dikonfirmasi — cuma tambah 1 tap safety-gate, bukan ubah fungsi. **Hasil: 0
bug** — 0 kode disentuh. `MICRO_UIUX_AUDIT.md` diupdate (checklist 9/9 + `STATUS TRACKING`
kategori 6-14). **Rekap kategori Settings**: grouping(215, 1 fix)/title-subtitle(216, 1 fix)/
spacing(217, 0 bug)/switch alignment(218, 0 bug)/nav affordance(219, 0 bug)/destructive
setting(220, 1 fix)/disabled visibility(221, 0 bug)/visual density(222, 0 bug)/fungsi tidak
berubah(223, verifikasi). Kategori berikutnya (belum mulai): 🟡 ICONOGRAPHY. Detail:
`CHANGELOG.md` Batch 223.

**Batch 222 (Settings polish item 8/9 — audit visual density, 0 kode + 2 dokumentasi)** —
Lanjutan Batch 215-221. Ditelusuri seluruh 848 baris `SettingsScreen.kt`: spacing pakai skala
Material konsisten (4/8/12/20dp, sudah diverifikasi struktural Batch 217). Titik tekstual
terpanjang (subtitle "Mini Player Mengambang"/"Lewati Keheningan Otomatis") berisi info
fungsional wajib — syarat izin sistem, batasan teknis, efek samping, saran mitigasi — memangkas
demi baris lebih pendek justru melanggar syarat item ini sendiri ("tanpa menghilangkan
informasi"). 0 elemen bertumpuk atau Row berisi >3 elemen visual ditemukan; kepadatan yang ada
murni konsekuensi jumlah info yang memang perlu disampaikan, bukan tata letak boros/redundan.
**Hasil: 0 bug** — 0 kode disentuh. `MICRO_UIUX_AUDIT.md` diupdate (checklist item 8/9). Item
terakhir kategori Settings (9/9): jangan mengubah fungsi setting — verifikasi akhir 8 perbaikan/
audit Batch 215-222 di kategori ini 0 mengubah behavior fungsional. Detail: `CHANGELOG.md`
Batch 222.

**Batch 221 (Settings polish item 7/9 — audit disabled setting visibility, 0 kode + 2
dokumentasi)** — Lanjutan Batch 215-220. Cuma 1 titik `enabled = ` di seluruh
`SettingsScreen.kt`: `Switch` "Mode Gelap" (`enabled = !followSystem`, nonaktif otomatis saat
"Ikuti Sistem" ON). Dicek 2 lapis: (1) `Switch` 0 custom `SwitchDefaults.colors`, murni default
Material3 yg otomatis dim saat `enabled = false`; (2) subtitle di bawahnya SUDAH eksplisit ganti
teks jadi "Nonaktif — mengikuti pengaturan sistem" — bukan cuma warna redup, tapi kalimat
penjelasan langsung. Sempat pertimbangkan tambah alpha manual ke title text biar match tone
switch, tapi di-grep app-wide (`alpha=0.38`/`LocalContentColor`/`ContentAlpha`) — 0 precedent
pola itu di mana pun di codebase, jadi dibatalkan (hindari elemen baru yg tidak konsisten dgn
sisa app). **Hasil: 0 bug** — 0 kode disentuh. `MICRO_UIUX_AUDIT.md` diupdate (checklist item
7/9). Item berikutnya (8/9): kurangi visual density tanpa menghilangkan informasi. Detail:
`CHANGELOG.md` Batch 221.

**Batch 220 (Settings polish item 6/9 — fix destructive setting nonaktifkan kunci PIN, 1 file
kode + 2 dokumentasi)** — Lanjutan Batch 215-219. Ditelusuri sampai layer data:
`AppLockStore.disableLock()` hapus PERMANEN PIN hash+salt dari `SharedPreferences` (PIN harus
dibuat ulang dari nol kalau diaktifkan lagi), tapi UI-nya cuma 1 toggle `Switch` OFF langsung
eksekusi — 0 konfirmasi, 0 pembeda warna error, padahal efeknya permanen. Pola sama persis bug
Batch 195 (Playlist/Queue) yang sudah diperbaiki. Fix `SettingsScreen.kt`: toggle OFF sekarang
buka `AlertDialog` konfirmasi baru ("Nonaktifkan Kunci Aplikasi?" + penjelasan PIN dihapus
permanen), tombol "Nonaktifkan" + ikon `LockOpen` di-tint `colorScheme.error`, tombol "Batal"
netral. `Switch` tetap `checked = lockEnabled` terikat state asli parent — kalau user tekan
Batal, switch balik sendiri tanpa flicker krn `disableLock()` belum pernah dipanggil. 1 import
baru (`LockOpen`, eksplisit). Brace/paren seimbang (136/136, 436/436). 0 protected asset.
**Belum diverifikasi visual** — prioritas cek dialog muncul benar + switch tidak state-jump saat
Batal + jalur aktivasi ulang (`showSetPinDialog`) tidak ikut kesenggol. `MICRO_UIUX_AUDIT.md`
diupdate (checklist item 6/9). Item berikutnya (7/9): pastikan disabled setting terlihat jelas.
Detail: `CHANGELOG.md` Batch 220.

> **Arsip Batch 1–219** dipindah ke `PROJECT_STATE_ARCHIVE.md` (Batch 1–57 sejak Batch 158, Batch 58–219 sejak Batch 321) — detail lengkap batch-batch lama ada di sana, urutan descending sama seperti asalnya. File ini (`PROJECT_STATE.md`) sekarang cuma menyimpan ~100 batch paling baru (Batch 220 ke atas) supaya tidak terus memanjang tanpa batas. `CHANGELOG.md` tetap punya detail penuh untuk SEMUA batch termasuk 1-219.


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
├── data/      — Store & repository (SharedPreferences/MediaStore), model data (Song, Playlist,
│                SmartPlaylist — rule-based, resolve live via SmartPlaylistEngine)
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

## Aturan sesi: transparansi versi & pesan commit (Batch 155, permintaan user)
Berlaku untuk SEMUA sesi AI berikutnya yang mengirim artifact ZIP dari repo ini:
1. **Transparansi versi, BUKAN bump manual di kode.** Setiap sesi WAJIB menyatakan status versi
   di chat sebelum/saat mengirim ZIP — TAPI dilarang mengedit `versionCode`/`versionName` secara
   manual di `app/build.gradle.kts`. Angka itu sengaja auto-derive dari jumlah commit git sejak
   Batch 30 (arsitektur, § "Keputusan arsitektur utama" atas) justru untuk MENGHILANGKAN risiko
   lupa bump manual — mengedit manual balik ke pola lama akan mengembalikan bug class yang
   sengaja dihindari. Yang WAJIB ditampilkan tiap kirim ZIP: nomor batch ZIP saat ini, dan
   pengingat bahwa versionName asli baru pasti setelah `git push` (auto dari commit count, bukan
   dari nomor batch chat).
2. **Box code pesan commit, DI ATAS heading "Update Harian:".** Tiap respons yang menyertakan
   skrip "Update Harian:" WAJIB diawali 1 code-box terpisah berisi draft pesan commit (persis
   yang dipakai di `git commit -m "..."` pada skrip di bawahnya), diletakkan sebelum heading
   "Update Harian:". **DILARANG** isinya cuma angka versi/perbandingan versi belaka (mis. "bump
   v1.1.43 -> v1.1.44") — WAJIB memuat penjelasan fitur/perbaikan singkat yang diambil LANGSUNG
   dari isi entri `CHANGELOG.md` batch tersebut, bukan digeneralisasi ulang jadi generik.

## Kebijakan: prioritas mutakhir, bukan kompatibilitas OS/dependency lama (Batch 205, permintaan user)
Berlaku untuk SEMUA sesi AI berikutnya, permanen sampai user bilang sebaliknya:

**User TIDAK PEDULI dukungan OS/API/dependency lama.** Konteks asal: widget "tahan banting"
(Batch 201-204) — sempat dibikinkan fallback threshold buat Android <12/API 31 (device tanpa
`RemoteViews(Map<SizeF,...>)`), padahal user cuma mau solusi PALING BENAR/modern, bukan solusi
yang juga jalan di device lama.

**Implikasi konkret buat sesi berikutnya**:
1. Kalau ada API/library/pendekatan MODERN yang lebih bersih/robust secara struktural
   (bukan sekadar preferensi gaya) tapi butuh `minSdk`/versi dependency lebih baru — WAJIB
   diutamakan/ditawarkan, JANGAN otomatis dihindari cuma karena "nanti device lama gak kebagian".
2. JANGAN habiskan effort ekstra bikin/pertahankan fallback compat kompleks buat OS/API lama
   kalau ada opsi modern yang jauh lebih sederhana & robust — cukup catat keterbatasannya (device
   mana yang tidak kebagian), tidak perlu direkayasa workaround rumit.
3. **`minSdk` (`app/build.gradle.kts`, protected asset) TIDAK diubah otomatis** oleh kebijakan
   ini sendiri — itu keputusan terpisah dengan konsekuensi instalasi nyata (device existing di
   bawah `minSdk` baru tidak bisa install app sama sekali, beda dari "cuma fallback visual
   kurang optimal"). Sesi berikutnya boleh MENYARANKAN naikkan `minSdk` kalau relevan, tapi tetap
   minta konfirmasi eksplisit dulu sebelum mengeksekusi — bukan diam-diam dinaikkan.
4. Dependency (`build.gradle.kts` versions, library versi) — prioritaskan versi stabil TERBARU
   yang tersedia saat sesi berjalan, bukan versi lama yang "aman/teruji" tanpa alasan konkret.

