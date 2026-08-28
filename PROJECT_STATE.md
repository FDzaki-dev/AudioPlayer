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

**Batch 219 (Settings polish item 5/9 — audit navigation affordance, 0 kode + 2 dokumentasi)** —
Lanjutan Batch 215-218. 7 baris navigasi `SettingsScreen.kt` (Statistik/Backup/Duplikat/Vault/
Signature/Diagnostik/Cek Update) 0 punya ikon chevron trailing. Di-grep seluruh `ui/` folder utk
pola `ChevronRight`/`Arrow...Forward`/`NavigateNext` — **0 hasil di mana pun di app**, jadi bukan
gap, tapi pola desain konsisten app-wide: affordance klik ditandai ripple `.clickable{}` full-row
+ icon prefix, bukan chevron (nambah chevron justru jadi elemen baru yg tidak konsisten dgn sisa
app). Row "Lanjutan" (`ExpandMore`/`Less`) BUKAN pembanding valid — itu representasi STATE expand/
collapse, beda peran dari sekadar affordance navigasi ke layar lain. **Hasil: 0 bug** — 0 kode
disentuh. `MICRO_UIUX_AUDIT.md` diupdate (checklist item 5/9). Item berikutnya (6/9): pastikan
destructive setting terlihat berbeda. Detail: `CHANGELOG.md` Batch 219.

**Batch 218 (Settings polish item 4/9 — audit switch/toggle alignment, 0 kode + 2 dokumentasi)** —
Lanjutan Batch 215-217. Grep semua `Switch(` di `SettingsScreen.kt`: 8 titik (4 "Perilaku
Pemutaran", 2 tema Ikuti Sistem/Mode Gelap, 2 kunci app PIN/Sidik Jari). Semua identik: `Row
(verticalAlignment = Alignment.CenterVertically)` + `Column(weight(1f))` title di kiri + `Switch`
polos (0 modifier custom) di kanan — 0 titik pakai `Alignment.Top/Bottom` atau ukuran/offset
manual. Beda `padding` pembungkus luar antar titik (langsung di Row vs diwarisi Column orang tua)
murni struktural per konteks pemanggil, tidak pengaruhi alignment vertikal internal Row. **Hasil:
0 bug** — 0 kode disentuh. `MICRO_UIUX_AUDIT.md` diupdate (checklist item 4/9). Item berikutnya
(5/9): audit navigation affordance. Detail: `CHANGELOG.md` Batch 218.

**Batch 217 (Settings polish item 3/9 — spacing antar setting, 0 kode + 2 dokumentasi)** —
Lanjutan Batch 215-216. Audit menyeluruh spacing di `SettingsScreen.kt` pasca restrukturisasi 2
batch sebelumnya. 3 sub-pola dicek: (1) transisi antar-section (`Spacer12→Divider→Spacer20`, 4
titik) — identik semua. (2) title→konten pertama dalam section (`Spacer8dp`, 3 titik) — identik.
(3) antar-item DALAM 1 section — **2 angka beda ditemukan** (switch-row `Spacer(12dp)` statis vs
nav-row `Spacer(4dp)`+`padding(vertical=8dp)` row = ~20dp efektif), TAPI dicek lebih dalam:
bukan inkonsistensi, beda krn afinitas interaksi — switch-row target sentuhnya cuma `Switch`
(row sendiri 0 padding, spacer murni estetika), nav-row SELURUH row `.clickable{}` (padding
vertikal bagian target sentuh fungsional, bukan estetika). Pola sama precedent Batch 181 (ukuran
beda krn peran UI beda, bukan gap yang perlu disamakan). **Hasil: 0 bug** — 0 kode disentuh.
`MICRO_UIUX_AUDIT.md` diupdate (checklist item 3/9 + status tracking). Item berikutnya (4/9):
audit switch/toggle alignment. Detail: `CHANGELOG.md` Batch 217.

**Batch 216 (Settings polish item 2/9 — title/subtitle row, 1 file kode + 1 dokumentasi)** —
Lanjutan Batch 215. Audit 7 baris navigasi icon+title di `SettingsScreen.kt`: 4 baris "Alat &
Utilitas" (Statistik/Backup/Duplikat/Vault) sudah title+subtitle; "Cek Signature APK"/"Log
Diagnostik" title-only TAPI dinaungi 1 deskripsi section bersama "Alat Developer" tepat di
atasnya (konteks tetap ada, BUKAN gap — pola sama "Tema"/"Perilaku Pemutaran" yang juga pakai 1
deskripsi section utk banyak item); "Cek Update" title-only TANPA konteks apa pun di dekatnya —
satu-satunya baris yang genuinely berdiri sendiri tanpa penjelasan. Fix: subtitle ditambah KE
"Cek Update" SAJA ("Cek versi APK terbaru dari GitHub Release — satu-satunya koneksi internet di
app ini", dikonfirmasi akurat ke `UpdateCheckSheet.kt`). 2 baris developer tool sengaja TIDAK
disentuh — pola section-level description sudah valid, menambah subtitle di situ akan jadi
redundan (bukan konsistensi, malah duplikasi info). 0 logic berubah, 0 protected asset. Brace/
paren seimbang (124/124, 426/426). **Belum diverifikasi visual**. `MICRO_UIUX_AUDIT.md` diupdate.
Item berikutnya (3/9): samakan spacing antar setting (kandidat: cross-check ulang dgn audit
Batch 151 kategori Spacing yang sudah cek pola sama, mungkin 0 bug/audit-only). Detail:
`CHANGELOG.md` Batch 216.

**Batch 215 (Settings polish item 1/9 — grouping antar section, 1 file kode + 1 dokumentasi)** —
Next pending sesuai `MICRO_UIUX_AUDIT.md` § FINAL EXECUTION ORDER: kategori 9 (Playlist/Queue)
tuntas 8/8 di Batch 212-214 (termasuk fix drag-reorder Batch 214), lanjut kategori 10 (Settings
polish), item 1/9. Audit `SettingsScreen.kt`: 4 baris tool (Statistik Dengar/Cadangkan &
Pulihkan/Deteksi File Duplikat/Vault Lagu Privat) masing-masing dibungkus `HorizontalDivider`
sendiri TANPA title section — beda dari pola section lain di file yang SELALU 1 title menaungi
beberapa item terkait (mis. "Perilaku Pemutaran" menaungi 4 switch). Fix: disatukan 1 title baru
"Alat & Utilitas" menaungi ke-4-nya, 3 `HorizontalDivider` antar-item dibuang (diganti
`Spacer(4.dp)` kecil antar-row dalam section yang sama). Divider transisi masuk (dari section
Tema) & keluar (ke section "Lanjutan") TIDAK disentuh — cuma batas internal antar 4 item yang
dihapus. 0 logic/navigasi/aksi berubah (murni `Text`/`Spacer`/`HorizontalDivider` restrukturisasi),
0 protected asset. Brace/paren seimbang (123/123, 421/421). **Belum diverifikasi visual** —
prioritas cek section baru tidak terasa terlalu padat (checklist item 8/9 "kurangi visual
density" nanti). `MICRO_UIUX_AUDIT.md` diupdate (status tracking + checklist item 1/9 Settings).
Item berikutnya (2/9): konsistenkan title/subtitle row. Detail: `CHANGELOG.md` Batch 215.

**Batch 214 (Fix drag reorder buggy — 2 file kode + 1 dokumentasi)** — User laporan ke-4 gejala
sekaligus (stutter/lompat/susah mulai/nyentak) — 1 root cause: `animateItemPlacement()` tetap
aktif di row yang lagi di-drag, rebutan kontrol posisi Y sama `graphicsLayer translationY`
manual, tiap kali `onMove` geser slot list. Fix `QueueSheet.kt`+`PlaylistScreen.kt`: skip
`animateItemPlacement()` khusus row `isDragging` (row lain yang kegeser slot tetap dapat
animasi mulus). Brace/paren kedua file seimbang. 0 protected asset. **Belum diverifikasi
device** — prioritas cek smoothness + apakah ke-4 gejala hilang. Detail: `CHANGELOG.md` Batch
214.

**Batch 213 (Tambah drag-reorder ke PlaylistScreen, 1 file kode + 1 dokumentasi)** — Item
terbuka Batch 211/212, dieksekusi atas permintaan eksplisit user. Porting logic drag
`QueueSheet.kt` ke `PlaylistScreen.kt` 1:1: drag-handle 48dp + `detectDragGesturesAfterLongPress`
+ `graphicsLayer`(translationY/shadowElevation/zIndex) + `rememberUpdatedState` + haptic
identik. Beda teknis: pakai `song.id` langsung (bukan slotIds terpisah), helper gesture
duplikat sendiri `pointerInputPlaylistDragHandle` (masing-masing file private, bukan
shared-extract). Tombol naik/turun TETAP ada sbg fallback aksesibilitas. Brace/paren seimbang
(127/127, 206/206). 0 protected asset. **Belum diverifikasi visual** — prioritas cek reorder
drag beneran jalan + divider (Batch 212) tidak tumpang-tindih row yang di-drag. Dengan ini
**Playlist & Queue sekarang paritas penuh** (drag+tombol keduanya). Detail: `CHANGELOG.md`
Batch 213.

**Batch 212 (Playlist/Queue item 8/8 TERAKHIR — tambah divider antar baris QueueSheet, 1 file
kode + 1 dokumentasi)** — Item flagged sejak Batch 189. `QueueSheet.kt` 0 `HorizontalDivider`
antar baris antrean, beda `PlaylistScreen.kt` yang sudah pakai. Fix: divider identik
(surfaceVariant, tiap item) ditambah, pola disalin persis `PlaylistScreen.kt`. Brace/paren
seimbang (40/40, 126/126), 0 import baru (sudah tercover wildcard `material3.*`). 0 protected
asset. **Belum diverifikasi visual** — cek divider vs row yang lagi di-drag (shadowElevation/
translationY). **§ Playlist/Queue checklist TUNTAS 8/8.** Item terbuka (di luar checklist,
ditunda): `PlaylistScreen.kt` reorder 0 drag gesture (cuma tombol) — kandidat kalau user minta.
Detail: `CHANGELOG.md` Batch 212.

**Batch 211 (Playlist/Queue item 7/8 — audit search-result state atau serupa, 0 kode, 2
dokumentasi)** — `PlaylistScreen.kt`+`QueueSheet.kt` 0 fitur search sama sekali (beda §
Library/Song List yang sudah punya `SearchResultsView`, Batch 188). Item literal tidak
aplikatif. "State serupa" ditemukan: gap reorder — `QueueSheet.kt` drag-handle (gesture +
shadowElevation/translationY/zIndex) + tombol naik/turun; `PlaylistScreen.kt` HANYA tombol
naik/turun, 0 drag gesture. Gap fitur nyata tapi porting drag logic (~80 baris) levelnya
"kerja lebih dalam" bukan micro-fix — **ditunda**, pola sama Batch 193/197. **Kandidat batch
terpisah kalau user eksplisit minta tambah drag-reorder ke PlaylistScreen**. Item berikutnya
(8/8, TERAKHIR checklist § Playlist/Queue): kandidat kemungkinan loading state atau list
separator/divider (pola template § Library/Song List item 8-10). Detail: `CHANGELOG.md` Batch
211.

**Batch 210 (Widget compact — tambah prev/next, 2 file kode)** — Lanjutan Batch 209.
`widget_player_compact.xml`: tambah `widget_prev`/`widget_next` (28dp, lebih kecil dari full
34dp), diapit kiri-kanan tombol play. `WidgetUpdater.kt`: binding prev/next jalan di kedua
layout; artist tetap eksklusif full. XML valid, brace/paren seimbang. 0 protected asset.
Detail: `CHANGELOG.md` Batch 210.

**Batch 209 (Widget — compact mode hilangkan judul total, bukan cuma truncate, 2 file kode)** —
User laporan screenshot: widget compact (dipaksa sempit) render album art + tombol play doang,
center-paksa, TANPA teks judul sama sekali. Root cause: `widget_player_compact.xml` dari awal
memang tidak punya `TextView` judul; `WidgetUpdater.kt` skip binding judul saat `isCompact`. Fix:
tambah `TextView` `@id/widget_title` (1 baris, ellipsize) di compact XML antara art & tombol
play, `gravity="center"`→`"center_vertical"`; `WidgetUpdater.kt` binding judul+click-to-open jalan
di kedua layout (artist+prev/next tetap eksklusif full). XML valid, brace/paren seimbang. 0
protected asset. Detail: `CHANGELOG.md` Batch 209.

**Batch 208 (Widget — kembalikan height-check compact-mode, BUKAN SizeF, 1 file kode)** — User
laporan: masih truncated 1-baris (Batch 207 XML metadata tidak cukup, tidak retroaktif). Analisis
ulang: height-check ITU SENDIRI (Batch 201/202) TIDAK PERNAH crash — cuma salah kalibrasi angka
(sudah dikoreksi 70dp). Crash baru muncul SETELAH `SizeF` map + `setBoolean setSelected` (Batch
203) — 2 hal itu paling dicurigai, BUKAN height-check. Fix: `COMPACT_HEIGHT_THRESHOLD_DP=70` +
`isCompact = width<180 || height<70` dikembalikan (versi Batch 202 terkoreksi), TANPA `SizeF`
map/`setSelected` (tetap non-aktif). 1 file, 0 protected asset, brace/paren seimbang (20/20,
119/119). **Belum diverifikasi device** — kalau crash muncul lagi = height-check-lah penyebabnya,
revert lagi + logcat. Kalau truncation masih tapi tanpa crash = kemungkinan launcher tidak
update `OPTION_APPWIDGET_MIN_HEIGHT` akurat, butuh info merk launcher/HP. Detail: `CHANGELOG.md`
Batch 208.

**Batch 207 (Widget — minResizeWidth/Height + border visual, 3 file XML, 0 logic runtime)** —
Setelah revert total Batch 206, user minta insets & border visual biar tidak truncated lagi,
TANPA logic baru. Fix murni metadata/drawable: (1) `widget_player_info.xml` — `minResizeWidth`/
`minResizeHeight` eksplisit (110dp/80dp) — launcher user terbukti (screenshot) tidak otomatis
membatasi resize ke minimum declared, floor ini pembatas resmi level-launcher; (2) border/stroke
1dp ditambah di `widget_background.xml`+`_light.xml` — batas visual selalu jelas. Insets
internal (padding) dicek, sudah memadai, tidak diubah. 0 Kotlin disentuh (0 risiko crash lagi).
XML valid. **Batasan jujur**: minResizeWidth/Height cuma efektif di launcher patuh kontrak,
bukan jaminan mutlak di semua OEM. Detail: `CHANGELOG.md` Batch 207.

**Batch 206 (REVERT PENUH Batch 201-204 — fallback total widget normal, 2 file kode)** — User
laporan screenshot bertimestamp: widget benar sesaat lalu ~15 detik kemudian jatuh "Ketuk untuk
memulihkan" (pola khas widget provider exception, BUKAN cuma distorsi visual). 4 iterasi
(201/202/203/204) tidak menyelesaikan, makin parah. `WidgetUpdater.kt` ditulis ulang PERSIS
logic pra-201 (width-only threshold, 1 RemoteViews, tanpa setSelected marquee); `widget_
player.xml` gravity balik `center_vertical`, title balik `ellipsize="end"` statis. Brace/paren
seimbang (20/20, 114/114), XML valid. 0 protected asset. **Kalau masih muncul setelah ini** =
bukti kuat penyebabnya BUKAN kode widget Batch 201-204 (sudah tidak ada lagi) — WAJIB logcat
sebelum coba apapun lagi, jangan tebak ulang. Detail: `CHANGELOG.md` Batch 206.

**Batch 205 (Dokumentasi — abadikan kebijakan "prioritas mutakhir, bukan kompat OS lama", 2
dokumentasi, 0 kode)** — Permintaan langsung user, permanen. Ditulis di 2 tempat (pola sama
Batch 157 — pinned summary + detail penuh): item 3 di § "⚠️ ATURAN SESI AKTIF" (atas file) +
§ baru "Kebijakan: prioritas mutakhir, bukan kompatibilitas OS/dependency lama" (bawah, dekat §
"Aturan sesi" Batch 155). Inti: sesi berikutnya WAJIB tawarkan/utamakan API/dependency modern
meski butuh `minSdk` lebih baru, JANGAN habiskan effort fallback compat OS lama yang rumit kalau
ada opsi modern lebih bersih. **`minSdk` sendiri TIDAK diubah** oleh kebijakan ini — itu
keputusan terpisah (device existing di bawah minSdk baru = tidak bisa install sama sekali, beda
kelas risiko dari "fallback visual kurang optimal") — sesi berikutnya boleh SARANKAN naik
`minSdk`, tapi tetap wajib konfirmasi eksplisit dulu, bukan diam-diam. 0 kode, 0 protected
asset (`build.gradle.kts` tidak disentuh, sesuai poin 3 kebijakan itu sendiri). Detail:
`CHANGELOG.md` Batch 205.

**Batch 204 (Fix widget — root full layout wajib center horizontal saat stretch, 1 file
kode)** — User: OS<12 sudah selesai (Batch 203), fokus SEMUA ukuran wajib center + 0 distorsi.
1 gap: root `widget_player.xml` cuma `gravity="center_vertical"` (compact sudah `center` sejak
awal) — widget di-stretch lebar bisa nempel kiri kalau kolom weight=1 tidak menyerap semua sisa
ruang. Fix: ke `gravity="center"`. Distorsi scaleType dicek ulang — TIDAK ada bug (centerCrop/
fitCenter sudah benar, semua ukuran FIXED dp). Live-refresh saat drag juga dicek — sudah benar
sejak Batch 35. 1 file, 0 protected asset, XML valid. **Belum diverifikasi device.** Detail:
`CHANGELOG.md` Batch 204.

**Batch 203 (Widget tahan-banting struktural — responsive RemoteViews API 31+, 1 file kode)** —
User minta tahan banting SUNGGUHAN, bukan tebak threshold lagi (sudah 2x salah: 201/202). Fix:
`RemoteViews(Map<SizeF, RemoteViews>)` (Android 12+/API 31) — OS pilih layout sendiri berdasar
ukuran live, dijamin API-nya tidak pernah render lebih besar dari ruang tersedia → hard-clip
TIDAK MUNGKIN lagi secara struktural di jalur ini. Logic build diekstrak ke `buildViewsFor(...)`
dipakai 2 entry map (compact `SizeF(110,52)`, full `SizeF(180,80)`, angka sama persis threshold
Batch 202). Android <12 (minSdk 23) tetap fallback threshold — batas platform, bukan bug. 1
file, 0 protected asset, brace/paren seimbang (23/23, 139/139). **Belum diverifikasi device** —
prioritas cek resize ekstrem di Android 12+, pastikan 0 clipping di titik manapun. Detail:
`CHANGELOG.md` Batch 203.

**Batch 202 (HOTFIX regresi Batch 201 — threshold compact-height ketinggian, 1 file kode)** —
`COMPACT_HEIGHT_THRESHOLD_DP=90` (Batch 201) lebih tinggi dari `minHeight="80dp"` deklarasi app
sendiri → widget ukuran DEFAULT (bukan di-resize) ikut kena compact, dampak nyaris universal.
Diturunkan ke 70dp (di bawah minHeight, di atas kebutuhan compact ~52dp). 1 file, 0 protected
asset, brace/paren seimbang. Marquee (fix lain Batch 201) tidak disentuh, tidak ada bukti
bermasalah. **Belum diverifikasi device.** Kalau "Ketuk untuk memulihkan" masih muncul, itu
kemungkinan prompt generik launcher, butuh logcat. Detail: `CHANGELOG.md` Batch 202.

**Batch 201 (Fix widget — truncated saat height-only shrink + marquee judul lagu, 2 file
kode)** — User lapor via screenshot. (1) `isCompact` dulu cuma cek lebar, widget yang di-shrink
cuma secara TINGGI tetap pakai layout penuh → clip di tepi (RemoteViews tidak reflow). Fix: cek
`OPTION_APPWIDGET_MIN_HEIGHT` juga. (2) Judul lagu statis `ellipsize="end"`. Fix: `ellipsize=
"marquee"` XML + WAJIB `views.setBoolean(id, "setSelected", true)` di Kotlin (marquee widget
butuh trik ini — focus-based marquee normal tidak jalan di widget, host view tidak focusable).
Brace/paren `WidgetUpdater.kt` seimbang (20/20, 122/122), 0 protected asset. **Belum
diverifikasi device** — marquee riwayat tidak konsisten antar-launcher, prioritas cek resize
height-only + judul panjang beneran scroll. Detail: `CHANGELOG.md` Batch 201.

**Batch 200 (Playlist/Queue item 6/8 — audit empty queue/playlist state, 0 bug, 2
dokumentasi)** — 3 titik `EmptyState` (`QueueSheet`/`PlaylistScreen` list & detail) diperiksa,
semua konsisten + subtitle actionable, 1 punya CTA button. 0 kode. Item berikutnya (7/8): audit
search-result state atau state serupa. Detail: `CHANGELOG.md` Batch 200.

**Batch 199 (SmartPlaylistTabView highlight lagu-sedang-diputar, 2 file — tuntaskan pending
Batch 198)** — `currentSongId` diteruskan ke `SmartPlaylistTabView` (tab 6), styling disalin
persis `PlaylistSongRow`/`QueueRow`. Brace/paren seimbang, 0 protected asset. **SEMUA
composable song-list sekarang konsisten** (Queue/Playlist/SmartPlaylist). Belum diverifikasi
visual. Detail: `CHANGELOG.md` Batch 199.

**Batch 198 (PlaylistScreen highlight lagu-sedang-diputar, 2 file, 1 fitur diperluas — DI LUAR
`MICRO_UIUX_AUDIT.md`, eksekusi observasi Batch 193 yang disetujui user)** — `currentSongId`
dialirkan `LibraryScreen.kt` → `PlaylistTabView` (param baru, default `null`) →
`PlaylistSongRow` (param `isPlaying`). Styling disalin persis dari `QueueRow` (background
primary tint + teks bold primary) — bukan desain baru. 2 file, 0 protected asset. Dengan ini
`QueueSheet` + `PlaylistScreen` konsisten highlight now-playing; `SmartPlaylistTabView` (tab 6)
belum dicek — beda composable lagi, kandidat terpisah. **Belum diverifikasi visual**. Detail:
`CHANGELOG.md` Batch 198.

**Batch 197 (Sweep-select tab Artist + Folder, 1 file, 1 fitur diperluas — lanjutan Batch 196,
DI LUAR `MICRO_UIUX_AUDIT.md`)** — `GroupedListView` (tab Artist `selectedTab==2` + tab Folder
`else`) dulu render lagu lewat `LazyColumn` manual, 0 `SongListView`, 0 selection sama sekali.
Fix: blok manual diganti panggil `SongListView(songs = groupSongs, ...)` (`onSongClick`
behavior identik), 6 param baru ditambah + diteruskan, 2 call site disamakan wiring dgn tab
Lagu/Favorit. Selection state top-level, otomatis persisten lintas grup/tab. Sweep-select
sekarang jalan di 4/7 tab (Lagu/Artist/Folder/Favorit). 1 file, 0 protected asset. **Sisa 3
belum bisa**: Album (grid, paradigma beda), Playlist & SmartPlaylist (row type sendiri, butuh
kerja lebih dalam). **Belum diverifikasi visual**.

**⏭️ ANTRIAN LANGSUNG (diminta user bareng batch ini, DITUNDA ke batch berikutnya sesuai cap
batch)**: kerjakan **PlaylistScreen highlight lagu-sedang-diputar** — observasi Batch 193 yang
tadinya nunggu keputusan user, SEKARANG SUDAH DISETUJUI (user bilang "kerjakan...juga"). Scope:
alirkan `currentSongId` dari `LibraryScreen.kt` (`onSongClick`/`PlaylistTabView` call site,
sekitar baris 326-335) turun ke `PlaylistTabView` → `PlaylistSongRow`, lalu terapkan highlight
gaya sama seperti `QueueRow` (background `primary.copy(alpha=0.12f)` + teks bold primary). 2
file (`LibraryScreen.kt` + `PlaylistScreen.kt`). Detail: `CHANGELOG.md` Batch 197.

**Batch 196 (Sweep-select tab Favorit, 1 file, 1 fitur diperluas — DI LUAR
`MICRO_UIUX_AUDIT.md`, permintaan langsung user)** — Sweep-select (`LibraryScreen.kt`, sejak
Batch 70/73) cuma jalan di tab Lagu; tab Favorit pakai `SongListView` YANG SAMA tapi manggil
positional-pendek jadi param selection-mode jatuh ke default (mati total). Fix: panggilan
disamakan persis dgn tab Lagu — `selectionMode`/`selectedIds` sudah top-level state (bukan
per-tab) jadi otomatis persisten lintas-tab. `SelectionActionBar` dicek 100% generik (filter
`rawSongs` pakai `selectedIds`, 0 referensi `selectedTab`), aman dipakai lintas tab. 1 file, 0
protected asset. **Sisa 4 tab (Album/Grouped/Playlist/SmartPlaylist) BELUM BISA** — semuanya
composable terpisah tanpa `SongListView`/selection-mode built-in, butuh kerja lebih besar (bukan
wiring param doang), kandidat batch terpisah kalau user minta lanjut. **Belum diverifikasi
visual**. Detail: `CHANGELOG.md` Batch 196.

**Batch 195 (Playlist/Queue item 5/8 — konfirmasi hapus playlist + warna error, 1 file, 1 bug
fix nyata)** — "Hapus playlist" (`PlaylistScreen.kt`) dulu langsung eksekusi 1 sentuhan, 0
konfirmasi, ikon tanpa warna error — beda dari pola destructive-confirm established
`LibraryScreen.kt` ("Hapus dari Perangkat?"). Fix: `AlertDialog` konfirmasi ditambah (tiru
persis pola `LibraryScreen`), ikon diberi `tint = error`. 1 file, 0 protected asset. **Belum
diverifikasi visual** — dialog baru, prioritas cek alur tap "Hapus" beneran jalan. Item
berikutnya (6/8): audit empty queue/playlist state. Item PlaylistScreen highlight (Batch 193)
masih pending keputusan user. Detail: `CHANGELOG.md` Batch 195.

**Batch 194 (Playlist/Queue item 4/8 — audit remove/delete affordance, 0 kode + 3 dokumentasi)**
— `QueueSheet`/`PlaylistScreen` remove button diperiksa: ikon+deskripsi konsisten, warna
`secondary` (netral) BENAR sesuai konvensi app (`error` khusus aksi permanen — hapus dari
perangkat — beda dari hapus-dari-antrean/playlist yang reversibel), touch target 48dp keduanya,
`canRemove` `QueueSheet` (cegah antrean kosong) vs `PlaylistScreen` tanpa batasan — keduanya
BENAR sesuai konteks masing-masing. **Hasil: 0 bug**. Item berikutnya (5/8): destructive action
visual hierarchy (kemungkinan tumpang-tindih hasil audit warna batch ini — sudah dikonfirmasi
konvensi error-color benar, jadi mungkin juga 0 bug/audit-only). Item PlaylistScreen highlight
(Batch 193) masih pending keputusan user. Detail: `CHANGELOG.md` Batch 194.

**Batch 193 (Playlist/Queue item 3/8 — audit selected/current item state, 0 kode + 2
dokumentasi)** — `QueueSheet` sudah highlight lagu-sedang-diputar (background primary tint +
teks bold). **Observasi (bukan bug, butuh keputusan user)**: `PlaylistScreen.kt` 0 referensi
`isPlaying`/`currentSong` sama sekali — beda dari `EmptyState` icon (Batch 163, default param 1
file, 0 risiko), ini butuh plumbing state BARU lintas-file (`LibraryScreen` → `PlaylistTabView`
→ row), levelnya di atas "high-value low-risk". SENGAJA tidak dieksekusi — checklist eksplisit
larang ubah queue behavior. **Tanya user dulu** sebelum lanjut kalau mau dikerjakan. 0 protected
asset. Item berikutnya (4/8): audit remove/delete affordance. Detail: `CHANGELOG.md` Batch 193.

**Batch 192 (Playlist/Queue item 2/8 — drag handle touch target 40dp→48dp, 1 file, 1 bug fix)**
— `QueueRow` (`QueueSheet.kt`) drag handle `Box` (gesture nyata via
`detectDragGesturesAfterLongPress`) 40dp, di bawah standar 48dp yang dipakai 3 `IconButton` lain
di baris sama. Disamakan ke 48dp, ikon/contentDescription/gesture logic tidak diubah. Dicek juga:
`PlaylistScreen.kt` tidak punya drag handle sama sekali (cuma tombol naik/turun) — BUKAN gap,
checklist-nya "jika tersedia" dan di situ memang tidak tersedia; menambah drag baru = ubah
behavior, di luar scope. 1 file, 0 protected asset. Item berikutnya (3/8): audit selected/current
item state. **Belum diverifikasi visual di device** — cek drag handle lebih mudah digenggam,
tidak dorong elemen lain. Detail: `CHANGELOG.md` Batch 192.

**Batch 191 (Playlist/Queue item 1/8 — konsistenkan row height dan spacing, 1 file, 1 bug fix)**
— Kategori baru dimulai setelah Library/Song List tuntas. `QueueRow` horizontal padding 12dp,
outlier dari konvensi 20dp yang dipakai konsisten di seluruh app (`PlaylistSongRow`/`SongRow`/
dst.). Disamakan ke 20dp. Vertical padding & tinggi row sudah konsisten, tidak diubah. 1 file, 0
protected asset. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (2/8): pastikan
drag/reorder affordance jelas. **Belum diverifikasi visual di device** — cek drag handle masih
mudah digenggam dari tepi. Detail: `CHANGELOG.md` Batch 191.

**Batch 190 (Library/Song List item 11/11 TERAKHIR — visual jumping artwork, kategori TUNTAS
11/11, 0 kode)** — `AlbumArt` (`Utils.kt`) sudah anti-jump by-design sejak awal: Box ukuran
fixed dari `modifier` caller (bukan derive dari gambar), `background(surfaceVariant)` placeholder
instan, `matchParentSize()` + `loading={}` kosong disengaja. Layout tidak pernah berubah ukuran
sebelum→sesudah decode — jump tidak mungkin terjadi. **Hasil: 0 bug.**

**🏁 KATEGORI "LIBRARY / SONG LIST" RESMI TUNTAS 11/11** (Batch 180-190): 2 bug fix (title
marquee `QueueRow` Batch 183, padding `ShimmerRow` Batch 187), 9 audit bersih. 1 catatan untuk
kategori berikutnya (bukan bug kategori ini): `QueueRow` 0 divider. **Kategori berikutnya:
PLAYLIST / QUEUE** (`MICRO_UIUX_AUDIT.md` § "🟠 PLAYLIST / QUEUE", 8 item, mulai dari
"Konsistenkan row height dan spacing") — ⚠️ item pertama kategori itu kemungkinan langsung
ketemu catatan divider `QueueRow` di atas, bukan temuan baru. `FILE_MANIFEST.txt` tidak berubah
(173/173). Detail: `CHANGELOG.md` Batch 190.

**Batch 189 (Library/Song List item 10/11 — audit list separator/divider, 0 kode)** — 5 titik
`HorizontalDivider` di `LibraryScreen.kt` (SongListView/GroupedListView×2/SearchResultsView/
drill-down Album) semua identik (warna surfaceVariant, posisi setelah setiap item). **Hasil: 0
bug.** Dicatat (bukan bug, di luar cakupan): `QueueRow` 0 divider — kandidat § Playlist/Queue
nanti, kategori terpisah. `FILE_MANIFEST.txt` tidak berubah (173/173). **Item berikutnya
(11/11, TERAKHIR kategori ini)**: hindari visual jumping artwork selesai loading — setelah ini
kategori Library/Song List TUNTAS 11/11, lanjut ke § Playlist/Queue. Detail: `CHANGELOG.md`
Batch 189.

**Batch 188 (Library/Song List item 9/11 — audit search result state, 0 kode)** —
`SearchResultsView` diperiksa: hasil kosong reuse `EmptyState` (sudah konsisten, item 7), hasil
ada dikelompokkan Artis/Album/Lagu dengan `SearchSectionLabel` seragam, pencarian sinkron
in-memory jadi 0 state loading terpisah diperlukan. **Hasil: 0 bug.** `FILE_MANIFEST.txt` tidak
berubah (173/173). Item berikutnya (10/11): audit list separator/divider. Detail:
`CHANGELOG.md` Batch 188.

**Batch 187 (Library/Song List item 8/11 — audit loading state, 1 file, 1 bug fix)** —
`ShimmerRow` skeleton (`LibraryScreen.kt`) padding vertical 10dp vs `SongRow` asli 8dp — beda
2dp × 8 baris = shift 16dp saat transisi loading→loaded. Disamakan ke 8dp. 1 file, 0 protected
asset. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (9/11): audit search result
state. **Belum diverifikasi visual di device.** Detail: `CHANGELOG.md` Batch 187.

**Batch 186 (Library/Song List item 7/11 — audit empty library state, 0 kode)** — 1 composable
shared `EmptyState` dipakai ulang di 4 skenario kosong `LibraryScreen.kt` (perpustakaan kosong
total + CTA rescan, favorit kosong, filter tab kosong, pencarian kosong — 3 terakhir tanpa CTA
disengaja). **Hasil: 0 bug.** `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya
(8/11): audit loading state. Detail: `CHANGELOG.md` Batch 186.

**Batch 185 (Library/Song List item 6/11 — verifikasi retrospektif indikator sedang-diputar, 0
kode)** — Sesuai peringatan Batch 179: item ini sudah dikerjakan Batch 163/164 sebelum kategori
resmi dimulai. **Hasil: 0 bug, dikonfirmasi ulang.** `SongRow`/`QueueRow` 3 lapis identik (bg
primary 12% + ikon GraphicEq + title bold-primary). `ContinueListeningCard`/`MiniPlayerBar`
sengaja tanpa lapisan tambahan (beda semantik: last-played vs live-status vs bar itu sendiri =
indikator). `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (7/11): audit empty
library state. Detail: `CHANGELOG.md` Batch 185.

**Batch 184 (Library/Song List item 5/11 — audit hit target icon, 0 kode)** — 4 komponen
diperiksa (favorit `SongRow`, moveUp/moveDown/remove `QueueRow`, play-pause `MiniPlayerBar`,
`ContinueListeningCard` 0 icon). **Hasil: 0 bug.** Default 48dp aman; `MiniPlayerBar` play-pause
`.size(40.dp)` (sengaja sejak Batch 55) tetap aman krn Material3 `minimumInteractiveComponentSize()`
otomatis menegakkan touch target ≥48dp terlepas dari ukuran visual — dikonfirmasi 0 override
`LocalMinimumInteractiveComponentEnforcement` di file terkait. `FILE_MANIFEST.txt` tidak berubah
(173/173). Item berikutnya (6/11): audit selected/current-playing indicator — ⚠️ ingat catatan
Batch 179: indikator ini SUDAH dikerjakan (Batch 164, `SongRow` currentSongId), verifikasi
retrospektif saja, jangan salah tandai sebagai gap baru. Detail: `CHANGELOG.md` Batch 184.

**Batch 183 (Library/Song List item 4/11 — audit title/artist truncation, 1 file, 1 bug fix)**
— 3/4 komponen (`SongRow`/`ContinueListeningCard`/`MiniPlayerBar`) title-nya sudah
`basicMarquee()`, cuma `QueueRow` (`QueueSheet.kt`) ketinggalan pakai `TextOverflow.Ellipsis`
diam. Disamakan ke `basicMarquee()`, `overflow=Ellipsis` yang redundan dilepas dari Text itu.
Artist di 4 komponen tetap ellipsis semua (tidak diubah, memang bukan fokus). 1 file, 0 protected
asset. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (5/11): hit target favorite/
overflow/action icon. **Belum diverifikasi visual di device.** Detail: `CHANGELOG.md` Batch 183.

**Batch 182 (Library/Song List item 3/11 — audit spacing antar metadata, 0 kode)** — 4 komponen
song-metadata (`SongRow`/`QueueRow`/`ContinueListeningCard`/`MiniPlayerBar`) diperiksa, pola
3-segmen IDENTIK di semua: art↔text 12dp, title↔artist 0dp (line-height, disengaja), text↔
trailing-action 8dp. **Hasil: 0 bug.** `FILE_MANIFEST.txt` tidak berubah (173/173). Item
berikutnya (4/11): audit title/artist truncation. Detail: `CHANGELOG.md` Batch 182.

**Batch 181 (Library/Song List item 2/11 — audit thumbnail/artwork size, 0 kode)** — 5 file
pemakai `AlbumArt` diperiksa, ukuran mengelompok 4 kategori peran UI (list-row `SongRow` 48dp,
compact-bar `MiniPlayerBar` 44dp, featured-card `ContinueListeningCard` 56dp, carousel/grid
fill-container `HomeSongCard`/`AlbumGridView`), masing-masing konsisten internal. Beda lintas-
kategori = peran UI berbeda disengaja. **Hasil: 0 bug.** `FILE_MANIFEST.txt` tidak berubah
(173/173). Item berikutnya (3/11): spacing antar metadata. Detail: `CHANGELOG.md` Batch 181.

**Batch 180 (Library/Song List item 1/11 — audit tinggi row, 0 kode)** — Kategori baru setelah
Now Playing tuntas. `SongRow` (art 48dp + padding vertical 8dp) 1 komponen shared dipakai
identik di 5 titik (tab Lagu/Favorit/drill-down Artis/drill-down Folder/Pencarian), tinggi
konsisten by-construction. Group-header `ListItem` & `AlbumGridView` sengaja beda paradigma,
bukan gap. **Hasil: 0 bug.** `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya
(2/11): samakan thumbnail/artwork size. Detail: `CHANGELOG.md` Batch 180.

**Batch 179 (Now Playing item 11/11 — verifikasi retrospektif "jangan ubah playback logic",
kategori TUNTAS 11/11, 0 kode)** — Dikonfirmasi grep: `PlayerViewModel.kt`/package `playback/`
0 disentuh sepanjang Batch 169-178. Satu-satunya perubahan kode kategori ini (Batch 178, swipe
feedback) diverifikasi ulang tetap panggil `onNext`/`onPrevious` yang sama, threshold 120px
tidak berubah. **Kategori "Now Playing — Final Micro-Polish" resmi TUNTAS**: 2 bug fix
(spacing controls Batch 170, swipe feedback visual Batch 178), 9 audit hasil bersih.
`FILE_MANIFEST.txt` tidak berubah (173/173). **Kategori berikutnya: Library/Song List**
(`MICRO_UIUX_AUDIT.md` § "🟠 LIBRARY / SONG LIST") — ⚠️ item "indikator lagu-sedang-diputar" di
daftar itu SUDAH dikerjakan (Batch 164, `SongRow` currentSongId), jangan salah tandai sebagai
gap baru saat audit dimulai. Detail: `CHANGELOG.md` Batch 179.

**Batch 178 (Now Playing item 10/11 — audit semua controls feedback visual, 1 file, 1 bug fix)**
— Gap ditemukan: swipe album art (next/prev) 0 feedback visual selama drag (cuma haptic di
dragEnd), beda dari gesture brightness/volume di sekitarnya yang sudah live badge. Fix:
`AlbumArtHero` dapat `Animatable dragOffset` — art ikut bergeser mengikuti jari (damped 0.5x,
clamp ±48dp) via `graphicsLayer { translationX = ... }`, spring balik ke tengah saat
dilepas/dibatalkan. **Logic swipe (totalDrag, threshold 120px, kapan skip terpicu) SAMA SEKALI
TIDAK diubah** — murni layer visual tambahan. 1 file, 0 protected asset. Brace/paren
`NowPlayingScreen.kt` (215/215, 750/750) seimbang. `FILE_MANIFEST.txt` tidak berubah (173/173).
**Belum diverifikasi visual di device** — cek art ikut jari saat drag, spring-back mulus saat
dilepas, threshold skip tetap sama persis. Item berikutnya (11/11, verifikasi retrospektif):
pastikan tidak ada perubahan playback logic. Detail: `CHANGELOG.md` Batch 178.

**Batch 177 (Now Playing item 9/11 — audit artwork loading/error/empty state, 0 kode)** —
`AlbumArt` (`Utils.kt`) 1 komponen shared dipakai 7 titik app-wide, konsisten by-construction.
Error & artwork-null render `AlbumArtFallbackIcon` yang sama; loading blank (bukan shimmer)
wajar utk app offline (URI lokal, dekode nyaris instan). **Hasil: 0 bug.** `FILE_MANIFEST.txt`
tidak berubah (173/173). Item berikutnya (10/11): semua controls punya feedback visual. Detail:
`CHANGELOG.md` Batch 177.

**Batch 176 (Now Playing item 8/11 — audit long title/artist layout shift, 0 kode)** — Judul
(`basicMarquee()`) & artist (`Ellipsis`) sudah `maxLines=1` keduanya, tinggi baris konstan;
parent `Column` lebar tetap (`fillMaxSize().padding(20.dp)`). **Hasil: 0 bug** — anti-layout-
shift by construction. `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (9/11):
artwork loading/error/empty state. Detail: `CHANGELOG.md` Batch 176.

**Batch 175 (Now Playing item 7/11 — audit selected/repeat/shuffle states, 0 kode)** — Tombol
Acak/Ulangi pakai tint-toggle (`animatedAccent`/`secondary`), identik pola favorite-icon
(`LibraryScreen.kt`) & rating-star (`SmartPlaylistScreen.kt`). Repeat dapat pembeda tambahan
icon glyph `Repeat`→`RepeatOne`. **Hasil: 0 bug.** `FILE_MANIFEST.txt` tidak berubah (173/173).
Item berikutnya (8/11): long title/artist tidak menyebabkan layout shift. Detail: `CHANGELOG.md`
Batch 175.

**Batch 174 (Now Playing item 6/11 — audit bottom sheet/modal transition, 0 kode)** — 1
`ModalBottomSheet` (Kontrol Lanjutan) + 2 `AlertDialog` di `NowPlayingScreen.kt`, + 4 sheet lain
dibuka via callback (`EqualizerSheet`/`VisualizerSheet`/`SongInfoEditSheet`/
`RingtoneCutterSheet`, file terpisah). **Hasil: 0 bug** — semua 5 `ModalBottomSheet` pakai
`rememberModalBottomSheetState(skipPartiallyExpanded=true)` + `containerColor=Transparent`
identik, tidak ada custom transition menyimpang. `FILE_MANIFEST.txt` tidak berubah (173/173).
Item berikutnya (8/11): selected/repeat/shuffle states. Detail: `CHANGELOG.md` Batch 174.

**Batch 173 (Now Playing item 5/11 — audit volume/secondary controls, 0 kode)** — Slider
"Peredam Dalam Aplikasi" + 2 zona gesture brightness/volume-sistem-HP diperiksa. 1 asimetri
ditemukan (badge volume punya label "Volume HP", badge brightness tidak) TAPI sudah disengaja &
terdokumentasi di kode dari batch lampau (disambiguasi dari slider peredam terpisah, brightness
tidak butuh). **Hasil: 0 bug baru.** `FILE_MANIFEST.txt` tidak berubah (173/173). Item
berikutnya (6/11): bottom sheet/modal transition. Detail: `CHANGELOG.md` Batch 173.

**Batch 172 (Now Playing item 4/11 — progress/current/remaining time mudah dibaca, 0 kode)** —
Baris waktu: `Row SpaceBetween`, `bodySmall` + `colorScheme.secondary` kedua sisi, treatment
umum player (timestamp di-de-emphasize dari warna teks utama). Mode Audiobook (Batch 93) sudah
konsisten style-nya. **Hasil: 0 bug.** `FILE_MANIFEST.txt` tidak berubah (173/173). Item
berikutnya (6/11): volume/secondary controls. Detail: `CHANGELOG.md` Batch 172.

**Batch 171 (Now Playing item 3/11 — audit slider height/touch area, 0 kode)** — Progress
slider: input sentuh sesungguhnya `Slider` Material3 standar (transparan, ditumpuk atas
`WaveformSeekBar` visual-only), dibungkus `Box(height=48dp)` = pas minimum touch target M3.
`Slider` sendiri sudah accessible-by-default terlepas tipisnya track. **Hasil: 0 bug.**
`FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (5/11): progress/waktu mudah
dibaca. Detail: `CHANGELOG.md` Batch 171.

**Batch 170 (Now Playing item 2/11 — spacing antar playback controls, 1 file, 1 bug fix)** —
`Row` 5 tombol playback ternyata TANPA `fillMaxWidth()`/`horizontalArrangement` sama sekali
(cluster rapat, bukan spread merata seperti player pada umumnya) — 0 komentar penjelas, beda
dari kebiasaan file ini yang selalu mendokumentasikan keputusan layout sengaja. Fix:
`.fillMaxWidth()` + `Arrangement.SpaceEvenly`. Brace/paren seimbang. `FILE_MANIFEST.txt` tidak
berubah (173/173). **Belum diverifikasi visual di device** — cek 5 tombol menyebar merata,
ukuran/fungsi tidak berubah. Item berikutnya (3/11): slider height/touch area. Detail:
`CHANGELOG.md` Batch 170.

**Batch 169 (Kategori baru "Now Playing — Final Micro-Polish", item 1/11 — audit alignment
artwork/title/artist/controls, 0 kode)** — Kategori #5 tuntas di Batch 168, pindah ke kategori
berikutnya. `NowPlayingScreen.kt`: 1 `Column` root `CenterHorizontally` membungkus semua elemen
(hero art, title, artist, rating, slider, tombol transport) — alignment konsisten
by-construction. Title `basicMarquee()` vs artist `TextOverflow.Ellipsis` beda treatment tapi
pola umum player (judul discroll, artis dipotong), bukan inkonsistensi kebetulan. **Hasil: 0
bug.** `FILE_MANIFEST.txt` tidak berubah (173/173). Item berikutnya (2/11): spacing antar
playback controls. Detail: `CHANGELOG.md` Batch 169.

**Batch 168 (Micro UI/UX kategori #5 penutup — audit konsistensi lintas-aksi, 0 kode)** — Item
terakhir checklist kategori #5. Audit toggle Favorit (`LibraryScreen.kt` SongRow vs
`NowPlayingScreen.kt`, aksi identik 2 lokasi): icon/tint/haptic/contentDescription sudah
identik (haptic malah sudah pernah disamakan di batch lampau, dikonfirmasi komentar kode
sendiri). **1 beda ditemukan**: `NowPlayingScreen` pakai `bouncyPress(0.75f)` di tombol
favoritnya, `LibraryScreen` tidak sama sekali (0 `bouncyPress` di seluruh file itu) — genuinely
ambigu (list Library ratusan lagu vs `VaultSheet` list pendek yang JUSTRU pakai bouncyPress di
row-nya, jadi bukan pola bersih "list=tanpa-bounce"). **TIDAK dieksekusi**, dicatat observasi
ke-3 tertunda keputusan user (pola sama Batch 162/163/165). Kategori #5 sekarang 8/8 sub-item
teraudit → status ✅ audit selesai, 3 observasi masih tertunda eksekusi total. Kategori
berikutnya (6-14, belum mulai sama sekali): Now Playing s/d Component Consistency. Detail:
`CHANGELOG.md` Batch 168.

**Batch 167 (Hotfix — import `dp` hilang di `Utils.kt`, 1 file, 1 baris)** — User upload log CI
gagal (`compileReleaseKotlin`+`compileDebugKotlin` FAILED, 7x "Unresolved reference: dp").
Akar: composable `ResultBanner` baru (Batch 166) pakai 8 literal `.dp` tapi
`import androidx.compose.ui.unit.dp` tidak pernah ditambahkan — `Utils.kt` sebelumnya genuinely
0 pemakaian `.dp` (2 composable lain di file itu tidak butuh), jadi belum pernah perlu import
ini. Fix: 1 baris import ditambahkan. Dicek ulang 4 file lain Batch 166 — semua sudah punya
import `dp` dari sebelumnya, tidak ada yang ikut kehilangan. 0 perubahan logic/visual lain.
`FILE_MANIFEST.txt` tidak berubah (173/173). **Masih belum diverifikasi compile Gradle ulang
setelah fix** — prioritas TERTINGGI batch berikutnya: `./gradlew assembleDebug` bersih.
**Pelajaran**: composable baru di file shared wajib dicek importnya sendiri dari nol, bukan
diasumsikan lengkap karena bersebelahan kode lain yang sudah lengkap. Detail: `CHANGELOG.md`
Batch 167.

**Batch 166 (Eksekusi pending item Batch 165 — unifikasi ResultBanner, Atomic Change 5 file)**
— User konfirmasi lanjut. Kelompok B Batch 165 (banner hasil-operasi 3-arah tidak konsisten)
disatukan jadi 1 composable shared `ResultBanner` (`Utils.kt`, + `enum ResultBannerStyle
{Solid, Tinted, Bare}`) — BUKAN dipaksa 1 tampilan tunggal, 3 gaya visual lama dipertahankan
lewat parameter `style` (masing-masing mereproduksi PERSIS byte-demi-byte implementasi lama:
warna/shape/padding/gap/text-style, **0 perubahan visual disengaja**). Akar masalah yang
diperbaiki: 3 implementasi manual gampang saling melenceng ke depan, bukan tampilannya sendiri
(yang memang beda sengaja per konteks — hasil-sekali-tampil vs state-persisten vs
status-dalam-stepper).

`BackupRestoreSheet.kt`/`DiagnosticLogSheet.kt` (diedit) → `ResultBanner(style=Solid,...)`.
`SignatureMatcherSheet.kt` (diedit) → `ResultBanner(style=Tinted,...)`, `ApkPickerRow` di file
yang sama TIDAK disentuh. `UpdateCheckSheet.kt` (diedit) — private `StatusBanner` badan
fungsinya delegasi ke `ResultBanner(style=Bare,...)`, KEDUA call site pemanggil TIDAK disentuh
(paling minim-risiko). Import `background`/`RoundedCornerShape`/`Alignment` yang jadi tak
terpakai di 3 file dihapus (dicek grep per simbol dulu, bukan tebakan).

5 file, `Atomic Change` (1 task tidak bisa dipecah lebih kecil, pola sama Batch 91/95/119). 0
file baru, `FILE_MANIFEST.txt` tidak berubah (173/173). Brace/paren ke-5 file seimbang.
**Belum diverifikasi compile Gradle/visual device** — prioritas berikutnya: (1) `./gradlew
assembleDebug`, (2-5) cek visual tiap 1 dari 3 style di device (Backup/Diagnostic/Signature/
Update), pastikan PERSIS sama seperti sebelum batch ini. Detail: `CHANGELOG.md` Batch 166.

**Batch 165 (Micro UI/UX kategori #5 lanjutan — audit error state & success/confirmation
feedback, 0 kode, 3 dokumentasi)** — Sub-item 5&6/8 kategori #5, digabung karena ternyata 1
komponen visual yang sama. Grep 9 file `ui/*.kt` pakai error color/icon, dikelompokkan 2:
**Kelompok A (teks validasi inline, 4 titik: `LockScreen`/`SettingsScreen` SetPinDialog/
`VaultSheet` x2)** — 100% konsisten (Text polos + colorScheme.error + bodySmall, tanpa ikon/bg).
**Hasil: 0 bug**. **Kelompok B (banner hasil operasi, 4 titik/3 file)** — **3-arah TIDAK
konsisten, genuinely gap**: `BackupRestoreSheet`+`DiagnosticLogSheet` (identik, container-solid
primaryContainer/errorContainer+RoundedCornerShape8dp) vs `SignatureMatcherSheet` (tint-alpha
0.15f dari warna semantik sendiri+shapes.medium) vs `UpdateCheckSheet`'s `StatusBanner`
(TANPA background sama sekali). **TIDAK dieksekusi batch ini** — 3 konteks beda bisa jadi
disengaja beda bobot visual (hasil-sekali-tampil vs state-persisten vs status-dalam-stepper),
beda dari kasus `SpeedDialog` Batch 163 yang jelas 2 kontrol identik bersebelahan. Dicatat
sebagai observasi tertunda keputusan user, pola sama Batch 162/163. **Kandidat eksekusi kalau
user pilih lanjut**: ekstrak composable shared `ResultBanner` dgn parameter style, atau pilih 1
treatment jadi standar — belum diasumsikan mana yang benar. Sisa kategori #5: konsistensi
lintas-aksi-sama (item terakhir, belum diperiksa). Detail: `CHANGELOG.md` Batch 165,
`MICRO_UIUX_AUDIT.md` status table baris #5.

**Batch 164 (Eksekusi pending item Batch 163 — indikator "sedang diputar" di SongRow Library, 2
file, 1 protected edit parsial)** — Item tertunda #2 Batch 163, dieksekusi setelah user
konfirmasi lanjut. `SongRow` (LibraryScreen.kt, 3 call site: tab Lagu/GroupedListView/
SearchResultsView) sebelumnya 0 indikator lagu sedang diputar, beda dari `QueueRow` yang sudah
lama punya (primary 12% alpha bg + title bold+primary).

`LibraryScreen.kt` (diedit) — param baru `currentSongId: Long? = null` di level top, diteruskan
lewat 5 titik pemanggilan internal (`SongListView` x2 pakai, `GroupedListView` x2, `SearchResultsView`)
sampai ke `SongRow(isPlaying = song.id == currentSongId)`. `SongRow` sendiri: param baru
`isPlaying: Boolean = false`, render disamakan PERSIS pola `QueueRow` (bg primary 12% alpha
sebelum `.clickable()`, title bold+primary, ikon `GraphicEq` 16dp di depan judul dalam `Row`
baru yang membungkus `Text` — `Text` dapat `Modifier.weight(1f, fill=false)` supaya
`basicMarquee()` tetap jalan).

`MainActivity.kt` (diedit, **protected — edit parsial, 1 titik**) — pemanggilan
`LibraryScreen(...)` dapat 1 baris: `currentSongId = uiState.currentSong?.id` — reuse
`uiState.currentSong` yang sudah ada di scope composable route `"library"` (dipakai
`onPlayNext`/`onAddToQueue` di atasnya), 0 state baru.

0 file baru (FILE_MANIFEST tidak berubah, 173/173 match). Brace/paren `LibraryScreen.kt`
(332/332, 719/719) & `MainActivity.kt` (251/251, 583/583) seimbang. **Belum diverifikasi
compile/runtime Gradle sungguhan** — prioritas berikutnya kalau user push: (1) `./gradlew
assembleDebug` build bersih, (2) putar lagu, cek highlight muncul benar di SEMUA tab yang
menampilkan lagu itu (Lagu/Favorit/Artis/Folder/Pencarian), (3) ganti lagu selagi Library
terbuka, pastikan highlight pindah live tanpa navigasi ulang, (4) cek tidak bentrok visual
dengan `selectionMode`/`Checkbox`. Detail lengkap: `CHANGELOG.md` Batch 164.

**Batch 163 (Micro UI/UX kategori #5 lanjutan — audit selected/active state, 1 bug fix + 2
observasi tertunda, 1 file kode + 3 dokumentasi)** — Sub-item ke-4/8 kategori #5. Taksonomi 3
pola "selected" ditemukan di app ini, semua defensible by-design: Card preview (ThemeOptionCard)
→ border+elevation; List/dialog single-choice → `RadioButton`; Tag/filter chip → Material3
`FilterChip` (secondaryContainer fill). **1 bug nyata ditemukan & diperbaiki**: `SpeedDialog`
(NowPlayingScreen.kt) — daftar kecepatan pakai `TextButton`+teks "✓"/warna teks berubah,
padahal `TransitionModeOption` (Gapless/Fade Halus) di dialog **SAMA PERSIS**, cuma dipisah 1
`HorizontalDivider`, sudah pakai `RadioButton` sungguhan sejak Batch 102. Disamakan ke pola
`RadioButton` (Row+clip+clickable identik `TransitionModeOption`, nol import baru). FilterChip
(7 titik: EqualizerSheet 2x, SmartPlaylistScreen 2x, RingtoneCutterSheet 3x lewat
`DestinationChip`) dicek satu-satu — 0 custom color override, genuinely konsisten, 0 bug.

**2 observasi TERTUNDA keputusan eksplisit user** (pola sama Batch 162 EmptyState — bukan
diam-diam dieksekusi):
1. `LibraryFilterChips` (tab Lagu/Album/Artis + chip "Lainnya", LibraryScreen.kt) — custom
`Box`+`background()` primary SOLID fill saat selected, BEDA dari `FilterChip` secondaryContainer
yang jadi pola di semua tempat lain. Bisa jadi disengaja (navigasi PRIMER pantas lebih tegas
dari filter/tag sekunder — beda hierarki fungsi, bukan inkonsistensi), bisa juga genuinely
gap. **Tidak disentuh** — ini kontrol navigasi paling sering dilihat di seluruh app, mengubahnya
tanpa konfirmasi eksplisit terlalu berisiko utk 1 batch kecil.
2. `SongRow` (LibraryScreen.kt, 3 call site: tab Lagu/GroupedListView/SearchResultsView) TIDAK
PUNYA indikator "sedang diputar" sama sekali, sedangkan `QueueSheet`'s row SUDAH (primary
12% alpha bg + bold, `isPlaying = index == currentIndex`). User yang browsing Library sambil
lagu main tidak pernah lihat baris mana yang aktif — gap cross-context nyata. **Tidak dieksekusi
batch ini**: `LibraryScreen` composable sama sekali tidak menerima currentSong/currentSongId
sebagai parameter (dicek: signature lengkap, 0 field terkait) — perbaikannya perlu parameter
baru + wiring state lewat `MainActivity`/`NavGraph` (**protected**, lintas-file), jelas di luar
cap "3 file/1 task kecil" kalau digabung diam-diam ke batch audit ini.

Sisa kategori #5 (setelah ini, 4/8): empty state (icon-mismatch Batch 162, masih tertunda),
error state, success/confirmation feedback, konsistensi lintas-aksi lain. Detail:
`CHANGELOG.md` Batch 163.

**Batch 162 (Micro UI/UX kategori #5 dimulai — audit disabled/pressed/loading, 0 kode + 3
dokumentasi)** — 3 dari 8 sub-item Interactive States diperiksa: disabled icon-button tint (5
titik `PlaylistScreen`/`QueueSheet`, 100% identik), pressed/ripple integrity (`indication =
null` grep = 0 hasil app-wide, aman), loading (`ShimmerBrush` shared composable 2 titik,
`CircularProgressIndicator` cuma 1 titik). **Hasil: 0 bug**. **Observasi dicatat (bukan
dieksekusi)**: `EmptyState` composable hardcode ikon `MusicNote` utk semua 9 konteks pemanggil
termasuk yang tidak relevan (folder/antrean/statistik) — perlu keputusan eksplisit user dulu
sebelum tambah parameter `icon` custom (nyentuh 9 file, di luar cap batch kecil). **Sisa
kategori #5**: selected/active state, empty state (icon-mismatch di atas), error state, success/
confirmation feedback, konsistensi lintas-aksi-sama. Detail: `CHANGELOG.md` Batch 162.

**Batch 161 (Micro UI/UX kategori #3 — audit line-height, 5 gap sistemik ditemukan &
diperbaiki, 1 file kode + 2 dokumentasi)** — `grep lineHeight` seluruh `ui/`: 0 hasil di semua
composable. Akar: `theme/Type.kt`'s 5 style ter-override (`titleLarge`/`titleMedium`/
`bodyMedium`/`bodySmall`/`labelSmall`, di KEDUA `AppleTypography`+`TactileTypography`) dibuat
tanpa `lineHeight` → default `Unspecified` (rapat), BEDA dari 10 style lain yang warisi default
M3 proporsional. Fix: `lineHeight` ditambahkan, dihitung proporsional dari rasio M3 asli per
slot style (`titleLarge` 35.6sp, `titleMedium` 25.5sp, `bodyMedium` 21.4sp, `bodySmall` 17.3sp,
`labelSmall` 16sp — detail rasio di `CHANGELOG.md` Batch 161). **⚠️ Blast radius app-wide** —
`Type.kt` dipakai `MaterialTheme` di SEMUA layar, beda dari batch-batch sebelumnya yang
terisolasi 1-2 file. **Belum diverifikasi visual sama sekali** — prioritas TINGGI: cek beberapa
layar (bottom sheet title, song-row body, badge label) pastikan line-height lebih lega tapi
tidak bikin overflow di card/row sempit. Rollback gampang kalau ada masalah (hapus baris
`lineHeight` per style). Dengan ini **kategori #3 Typography Hierarchy TUNTAS** (149/154/159/
160/161) — sisa sub-item "truncation/ellipsis cakupan penuh" beda sub-kategori, tidak diklaim
tuntas. **Kandidat batch berikutnya**: kategori #2 sisa literal `.dp` (pending sejak Batch 152),
kategori #5 Interactive States (belum mulai), atau verifikasi visual line-height batch ini kalau
user sudah build & lapor hasilnya. Detail: `CHANGELOG.md` Batch 161.

**Batch 160 (Micro UI/UX kategori #3 lanjutan — audit badge/kicker/value-readout, 0 kode + 3
dokumentasi)** — 13 titik sisa `typography.label*` (di luar yang sudah diaudit 149/154/159)
diperiksa: kelompok "setting-item/dialog caption" (7 titik) 100% konsisten `labelSmall`+
secondary; kelompok "screen-title eyebrow" (5 titik: BERANDA/LIBRARY/PENGATURAN/LANJUTKAN
MENDENGARKAN/SEDANG DIPUTAR) ukuran konsisten `labelSmall`, variasi warna (secondary/primary/
animatedAccent) genuinely disengaja untuk highlight, bukan bug; "value readout" persen beda
fungsi dari "Mengunduh…%" `UpdateCheckSheet`, bukan pasangan wajar. **Hasil: 0 bug** — pola
sama presisi Batch 143/145 (audit formal, hasil genuinely konsisten, bukan dipaksa cari bug).
Dengan ini, seluruh 24 titik `typography.label*` app-wide **tuntas diaudit**. **Sisa kategori
#3**: line-height (belum diaudit sama sekali), cakupan penuh truncation/ellipsis (sebagian
Batch 37, belum formal). Detail: `CHANGELOG.md` Batch 160.

**Batch 159 (Micro UI/UX kategori #3 lanjutan — samakan label field `ApkPickerRow`
`SignatureMatcherSheet`, 1 file kode + 2 dokumentasi)** — Item "label/caption text-style audit"
kategori #3 (pending sejak Batch 154). Grep 24 titik `typography.label*` di 26 file `ui/`,
kelompokkan per fungsi: kelompok "field label di atas control" (9 titik, `SmartPlaylistScreen`
5x + `RingtoneCutterSheet` 3x) 89% konsisten `labelLarge`+warna default — 1 gap:
`SignatureMatcherSheet`'s `ApkPickerRow` (label "APK Lama"/"APK Baru") pakai
`labelMedium`+secondary, disamakan ke `labelLarge`+default. Konteks lain (`AbPointButton`,
progress status `LyricsSheet`, badge/axis/nav-text sheet lain) SENGAJA belum diaudit — beda
fungsi atau kelompok terpisah kandidat batch berikutnya. 0 protected asset. **Sisa kategori
#3**: badge/axis-label/nav-text (belum diaudit), line-height, cakupan penuh truncation/ellipsis.
Detail: `CHANGELOG.md` Batch 159.

**Batch 158 (Dokumentasi — arsipkan detail Batch 1-57 ke PROJECT_STATE_ARCHIVE.md, 1 file baru +
3 dokumentasi diedit, 0 kode)** — Eksekusi langsung saran "catatan jujur" Batch 157: file sudah
3102 baris & terus tumbuh, section aktif makin jauh dari batch-batch tua. Batch 57 ke bawah
(737 baris, Batch 57-1) dipotong dari `PROJECT_STATE.md`, dipindah utuh (isi + urutan descending
sama persis) ke `PROJECT_STATE_ARCHIVE.md` (file baru) + pointer 1-baris ditinggal di lokasi
potongnya. `PROJECT_STATE.md` sekarang cuma simpan 100 batch aktif (58-157+), 2388 baris (dari
3102). `FILE_MANIFEST.txt` diperbarui (172→173 file, entri baru ditambahkan tepat setelah
`PROJECT_STATE.md`). Section "Riwayat insiden kronologis (jangan dihapus)" & lainnya di bawahnya
SENGAJA TIDAK disentuh — itu daftar kurasi pitfall eksplisit dilindungi tag "jangan dihapus",
beda dari dump per-batch mentah yang jadi target arsip batch ini. 0 kode, 0 protected asset.
`CHANGELOG.md` tetap simpan detail penuh SEMUA batch (1-157+) tanpa terpotong — arsip ini murni
soal `PROJECT_STATE.md`. **Ambang arsip berikutnya**: kalau `PROJECT_STATE.md` tumbuh lagi ke
~100 batch aktif (sekitar Batch 258), ulangi pola sama — geser cutoff maju 100 batch dari batch
terakhir. Detail: `CHANGELOG.md` Batch 158.

**Batch 157 (Dokumentasi — pindahkan ringkasan aturan sesi ke posisi tetap paling atas file, 2
dokumentasi, 0 kode)** — User bertanya langsung: "yakin rule tadi gak bakal tenggelam?" Jawaban
jujur: TIDAK yakin — rule Batch 155 ditaruh di § paling BAWAH file (3102 baris total saat itu),
sementara bagian paling sering dibaca sesi manapun ada di paling ATAS ("Batch terakhir yang
selesai"). Fix: ringkasan 2 rule ditambahkan di § baru "⚠️ ATURAN SESI AKTIF — WAJIB DIBACA"
tepat setelah intro pembuka file (posisi TETAP — tidak ikut tergeser walau "Batch terakhir yang
selesai" terus memanjang ke bawah tiap batch baru), sambil isi lengkap tetap di § "Aturan sesi"
bawah (tidak dihapus, cuma diringkas ulang di 2 tempat). 0 kode, 0 protected asset. Detail:
`CHANGELOG.md` Batch 157.

**Batch 156 (Fitur — catatan rilis/pesan commit tampil di layar "Cek Update" app, 3 file kode +
2 dokumentasi, cap file DILEWATI atas instruksi eksplisit user "eksekusi utuh dan sampai
tuntas")** — Jawab pertanyaan user: app SEBELUMNYA tidak pernah nampilin pesan commit/release
notes, cuma `tagName` (angka versi). Rantai lengkap 3 file: (1) `build.yml` (protected, edit
parsial) — step baru tulis `git log -1 --pretty=%B` ke file, `body_path:` di step release
GitHub; (2) `GitHubReleaseChecker.kt` — `ReleaseInfo.releaseNotes` baru, parse `body` dari API;
(3) `UpdateCheckSheet.kt` — render releaseNotes (blank-checked) di state `Available`. Brace/
paren kedua file Kotlin seimbang, YAML tervalidasi parse + urutan step benar. **Efek**: pesan
commit yang sejak Batch 155 wajib deskriptif (bukan cuma angka versi) sekarang otomatis jadi
teks yang muncul di app user sendiri saat "Cek Update", bukan cuma di chat. **Belum
diverifikasi device/CI sungguhan** — prioritas cek: push, pastikan body release GitHub
berikutnya terisi, buka "Cek Update" di app konfirmasi teks muncul. 0 protected asset lain
tersentuh. Detail: `CHANGELOG.md` Batch 156.

**Batch 155 (Dokumentasi — tambah aturan sesi: transparansi versi & pesan commit, 2
dokumentasi, 0 kode)** — Permintaan langsung user (2 rule baru untuk SEMUA sesi berikutnya),
ditulis formal di § "Aturan sesi: transparansi versi & pesan commit" (bawah file ini). **Rule 1
diadaptasi**, bukan diikuti mentah-mentah: literal "bump manual" akan MEMBALIK keputusan
arsitektur `versionCode`/`versionName` auto-derive dari commit count (sengaja dibuat sejak Batch
30/86 justru untuk menghilangkan risiko lupa bump manual — lihat § "Konvensi penamaan ZIP &
versi" tepat di atas). Diganti jadi kewajiban TRANSPARANSI: tiap kirim ZIP wajib sebut nomor
batch + ingatkan versionName asli baru pasti setelah `git push`. **Rule 2 diikuti persis**: box
code pesan commit sekarang WAJIB tampil di atas heading "Update Harian:" tiap respons, isinya
WAJIB ambil penjelasan fitur langsung dari `CHANGELOG.md`, dilarang cuma angka versi. 0 kode, 0
protected asset (build.gradle.kts TIDAK disentuh — sengaja, lihat alasan Rule 1 di atas).
Diterapkan mulai respons INI juga. Detail: `CHANGELOG.md` Batch 155.

**Batch 154 (Micro UI/UX kategori #3 lanjutan — samakan gaya song-row FolderManagerSheet, 1
file kode + 2 dokumentasi)** — Item "audit body/label/caption" (pending sejak Batch 149). Grep
`song.title` berpasangan style tetangga di semua sheet. Kelompok "song row ringkas dalam sheet"
(5 titik/3 file: `DuplicateFinderSheet`/`PlaylistScreen`/`VaultSheet`x2) sudah 100% konsisten
(title=`bodyMedium`, subtitle=`bodySmall`+secondary). **1 gap nyata**: `FolderManagerSheet.kt`
baris "Lagu Disembunyikan" — title pakai `titleMedium` (level `SongRow` utama layar penuh) tapi
subtitle tetap `bodySmall` (level sheet-ringkas) — kombinasi CAMPUR 2 baseline padahal secara
fungsi identik kelompok sheet-ringkas. Fix: title disamakan ke `bodyMedium`. Brace/paren
seimbang (42/42, 105/105). 0 protected asset. `MICRO_UIUX_AUDIT.md` status table SENGAJA belum
disentuh (cap 3 file) — disinkronkan batch berikutnya, jangan ditunda >1 batch (pelajaran Batch
148). **Belum diverifikasi visual**. **Sisa kategori #3**: label/caption text-style belum
dimulai, line-height, cakupan penuh truncation/ellipsis. Detail: `CHANGELOG.md` Batch 154.

**Batch 153 (Dokumentasi — sinkronkan status table kategori #2 di MICRO_UIUX_AUDIT.md, 1
dokumentasi, 0 kode)** — Item pending PRIORITAS TINGGI Batch 152 (tertunda 2 batch berturut,
tidak ditunda lebih lama sesuai pelajaran Batch 148). Baris kategori #2 disinkronkan: audit
vertical spacing antar section (Batch 151, `SettingsScreen.kt` 7 titik — 0 bug) + gap icon↔text
✅ SELESAI PENUH (Batch 151-152, 2 gap ditemukan & diperbaiki dari 29 titik diaudit —
`PlaylistScreen.kt`/`VaultSheet.kt` tombol Add 4dp→8dp). Sisa pending: literal `.dp` lain di
luar radius/icon-gap/screen-padding yang sudah disentuh. 0 kode, 0 protected asset. Kandidat
batch berikutnya: lanjut kategori #2 (sisa literal `.dp`, scope masih luas) atau kategori #3
Typography Hierarchy (body/label/caption font size/weight audit, pending sejak Batch 149).
Detail: `CHANGELOG.md` Batch 153.

**Batch 152 (Micro UI/UX kategori #2 lanjutan — samakan gap icon↔text tombol "Tambah"
VaultSheet, 1 file kode + 1 dokumentasi)** — Menutup Pending Queue Batch 151: bug PERSIS sama
(`TextButton`+`Icons.Default.Add` default-size, gap 4dp) di `VaultSheet.kt`'s
`VaultContentSection` tombol "Tambah" — disamakan ke 8dp, pola sama fix `PlaylistScreen.kt`
Batch 151. Brace/paren seimbang (101/101, 210/210). 0 protected asset. **Kategori #2 sub-item
"gap icon↔text" sekarang ✅ SELESAI PENUH** (29 titik diaudit formal Batch 151, 2 gap ditemukan
& diperbaiki Batch 151-152, sisanya sudah konsisten by-design — default-size icon+label 8dp/14
titik, TextButton icon custom-size proporsional 6dp/4dp disengaja, menu-row Icon+Column 12dp/5
titik). **Sisa kategori #2**: sisa literal `.dp` lain (di luar radius/icon-gap/screen-padding
yang sudah disentuh Batch 146-147/151-152) — scope luas, kandidat batch terpisah. **PRIORITAS
TINGGI batch berikutnya**: sinkron `MICRO_UIUX_AUDIT.md` status table — tertunda 2 batch
berturut (151+152), jangan ditunda lebih lama (pelajaran Batch 148). Detail: `CHANGELOG.md`
Batch 152.

**Batch 151 (Micro UI/UX kategori #2 lanjutan — samakan gap icon↔text tombol "Buat Playlist
Baru", 1 file kode + 1 dokumentasi)** — Item "gap icon↔text" (pending sejak Batch 147): 29 titik
`Icon()`→`Spacer(width)`→`Text()` di `ui/*.kt` dikelompokkan per konteks dulu (bukan sweep
mekanis buta). 3 grup SUDAH konsisten (default-size icon+label 8dp, 14 titik/9 file; TextButton
icon custom-size 16-18dp proporsional 6dp/4dp, disengaja bukan bug; menu-row Icon+Column
judul+deskripsi 12dp, 5 titik/2 file). **1 gap nyata**: `PlaylistScreen.kt` "Buat Playlist Baru"
(`TextButton`+`Icons.Default.Add` default-size) gap 4dp → disamakan ke 8dp. Brace/paren
seimbang (96/96, 152/152). 0 protected asset. **Pending Queue kategori #2**: (1) `VaultSheet.kt`
~baris 270 — bug PERSIS sama (`TextButton`+`Icons.Default.Add` default-size, gap 4dp) ditemukan
di audit yang sama, ditunda demi cap 3 file — jangan ditunda >1 batch. (2) `SettingsScreen.kt`
vertical spacing antar section diaudit ulang batch ini — 7 titik pola `Spacer(12dp)→Divider→
Spacer(20dp)` SUDAH 100% konsisten, 0 bug, kategori ini bisa dianggap selesai kalau tidak ada
screen lain yang perlu dicek. (3) sisa literal `.dp` lain. (4) sinkron `MICRO_UIUX_AUDIT.md`
status table (tertunda 1 batch demi cap file — pelajaran Batch 148: jangan ditunda >1 batch
berturut-turut). Detail: `CHANGELOG.md` Batch 151.

**Batch 150 (Dokumentasi — sinkronkan status table kategori #3 di MICRO_UIUX_AUDIT.md, 1
dokumentasi, 0 kode)** — Item pending prioritas tinggi Batch 149 (tertunda 0 batch, langsung
disinkronkan sesuai pelajaran Batch 148 soal dokumen tracking manual rawan telat). Baris
kategori #3 diperbarui `⬜ Belum mulai` → `🟡 Berlanjut (Batch 149)` + ringkasan temuan/fix
(title `FolderManagerSheet.kt` disamakan `titleMedium`+Bold) + pending (body/label/caption,
line-height, cakupan penuh truncation/ellipsis). 0 kode, 0 protected asset. Kandidat batch
berikutnya: lanjut kategori #3 Typography Hierarchy (body/label/caption font size/weight audit)
atau kategori #2 (vertical spacing antar section, gap icon↔text — masih pending sejak Batch 147).
Detail: `CHANGELOG.md` Batch 150.

**Batch 149 (Micro UI/UX kategori #3 dimulai — samakan gaya title bottom sheet
FolderManagerSheet, 1 file kode + 2 dokumentasi)** — Item pertama Typography Hierarchy: grep
semua header `ModalBottomSheet` (13 sheet). **12 sudah konsisten** `titleMedium` + `Font-
Weight.Bold`, **1 gap nyata**: `FolderManagerSheet.kt` pakai `titleLarge` tanpa `fontWeight`
eksplisit — beda ukuran DAN berat huruf dari 12 sheet lain, pecah hierarki visual paling
mencolok kategori #3 sejauh ini. Fix: disamakan ke `titleMedium`+Bold + 1 import baru
(`FontWeight`, file ini belum pernah pakainya). Brace/paren `FolderManagerSheet.kt` seimbang
(42/42, 105/105). 0 protected asset. `MICRO_UIUX_AUDIT.md` status table SENGAJA belum disentuh
(cap 3 file) — disinkronkan batch berikutnya (jangan tunda lebih dari 1 batch, pelajaran Batch
148: dokumen tracking manual rawan telat kalau ditunda berturut-turut). **Belum diverifikasi
visual**. **Sisa kategori #3**: audit body/label/caption, line-height, cakupan penuh truncation/
ellipsis (sebagian sudah Batch 37, belum formal untuk kategori #3 spesifik). Detail:
`CHANGELOG.md` Batch 149.

**Batch 148 (Dokumentasi — sinkronkan status table kategori #2 di MICRO_UIUX_AUDIT.md, 3
dokumentasi, 0 kode)** — Item pending prioritas tinggi Batch 147 (tertunda 2 batch demi cap 3
file). Baris kategori #2 diperbarui `⬜ Belum mulai` → `🟡 Berlanjut (Batch 146-147)` + ringkasan
2 temuan/fix (horizontal screen padding tab Library, ukuran ikon LockScreen) + 3 item pending
(vertical spacing antar section, gap icon↔text — butuh pengelompokan per-konteks dulu sebelum
aman dieksekusi, sisa literal `.dp` lain). **Pelajaran dicatat** (pola sama presedan Batch 123
soal README telat sync): dokumen tracking manual rawan telat kalau beberapa batch berturut
sengaja skip demi cap file — cek status table ini juga kalau ada laporan dokumentasi
ketinggalan ke depan. 0 kode, 0 protected asset. Kandidat batch berikutnya: lanjut kategori #2
(vertical spacing/icon-text gap per-konteks) atau mulai kategori #3 Typography Hierarchy. Detail:
`CHANGELOG.md` Batch 148.

**Batch 147 (Micro UI/UX kategori #2 lanjutan — samakan ukuran ikon fingerprint/backspace
LockScreen, 1 file kode + 2 dokumentasi)** — Item "ukuran control setara": audit 9 titik
`Icon().size()` eksplisit di `ui/*.kt` (sisanya default Material 24dp). 1 gap nyata:
`LockScreen.kt` Fingerprint (28dp) vs Backspace (22dp) — keduanya render via `RoundGlyphButton`
yang sama (komentar kode sendiri menyatakan "same round tactile/skeu treatment"), duduk simetris
flanking tombol "0", tapi beda 6dp visual. Fix: disamakan ke 24dp (default Material). 7 titik
lain diaudit & TIDAK disentuh — beda konteks genuinely (tidak ada pasangan sejenis yang perlu
diseragamkan; `NowPlayingScreen.kt` SkipPrevious/SkipNext 36dp+36dp SUDAH konsisten). Brace/paren
`LockScreen.kt` seimbang (48/48, 128/128). 0 protected asset. `MICRO_UIUX_AUDIT.md` status table
masih SENGAJA belum disentuh (cap 3 file, 3 batch berturut-turut sekarang — 146+147 — jadi
prioritas TINGGI disinkronkan batch berikutnya, jangan ditunda lagi lebih lama). **Belum
diverifikasi visual**. **Sisa kategori #2**: vertical spacing antar section, gap icon↔text
(diaudit sekilas — sebaran Spacer 4-16dp terlalu kontekstual buat sweep mekanis, butuh
pengelompokan per-konteks batch terpisah), sisa literal `.dp` lain. Detail: `CHANGELOG.md`
Batch 147.

**Batch 146 (Micro UI/UX kategori #2 dimulai — audit horizontal screen padding tab Library, 1
file kode + 2 dokumentasi)** — Item pertama kategori #2 (Spacing & Sizing Consistency), scope
sengaja 1 layar dulu (bukan sapuan ~340 literal `.dp` sekaligus, sudah ditandai berisiko sejak
Batch 54). Semua screen utama lain sudah 20dp horizontal (dikonfirmasi grep) — 2 gap nyata di
`LibraryScreen.kt`: (1) `AlbumGridView` `contentPadding` 16dp all-sides → `horizontal=20dp,
vertical=16dp`; (2) 4 titik `ListItem(...).padding(horizontal=4dp)` (tab Artis/Folder +
hasil-pencarian + riwayat-pencarian, konsisten satu sama lain tapi menyimpang jauh dari
konvensi 20dp app) → disamakan ke 20dp. Brace/paren seimbang (330/330, 701/701). 1 file kode,
0 protected asset. `MICRO_UIUX_AUDIT.md` status table SENGAJA belum disentuh (cap 3 file, pola
sama Batch 144→145) — kategori #2 masih 🟡 (baru 1 dari banyak sub-item: horizontal screen
padding), belum ✅. **Belum diverifikasi visual** — prioritas device: tab Album/Artis/Folder/
hasil-pencarian/riwayat-pencarian sekarang sejajar tepi kiri-kanan dengan tab Lagu. **Sisa
kategori #2**: vertical spacing antar section, gap icon↔text, ukuran control setara, audit
literal `.dp` lain. Detail: `CHANGELOG.md` Batch 146.

**Batch 145 (Micro UI/UX kategori #1 TUNTAS — audit formal 22 titik "Hapus" + sinkron status
table, 3 dokumentasi, 0 kode)** — Lanjutan & penutup Pending Queue kategori #1. Grep ulang
(bukan andalkan taksiran Batch 142) konfirmasi persis 22 titik `"Hapus"` di 15 file `ui/*.kt`,
dibaca konteks 1-per-1. **0 bug, 4 kelompok fungsi beda, genuinely bukan kandidat unifikasi**:
(1) label tombol konfirmasi generik (5 titik, `"Hapus"` polos — konteks sudah jelas dari title
dialog), (2) label dgn jumlah dinamis (1 titik, unik), (3) title dialog "Hapus X?" (3 titik,
sudah diaudit formal Batch 144), (4) `contentDescription` aksesibilitas (13 titik, SENGAJA
full-context spt `"Hapus dari favorit"` — screen reader butuh objek eksplisit, menyamakan ke
gaya kelompok 1 justru MERUSAK aksesibilitas). **Kategori #1 (String & Wording Consistency)
sekarang ✅ SELESAI PENUH** (Batch 142-145: undo-label, Batal/Tutup, title dialog+Aksi/Tindakan,
Hapus) — `MICRO_UIUX_AUDIT.md` status table disinkronkan. 0 protected asset. Kandidat batch
berikutnya: kategori #2 Spacing & Sizing Consistency (13 kategori lain masih ⬜, urutan
`FINAL EXECUTION ORDER` di `MICRO_UIUX_AUDIT.md`). Detail: `CHANGELOG.md` Batch 145.

**Batch 144 (Micro UI/UX kategori #1 lanjutan — audit judul dialog + samakan "Aksi"/"Tindakan",
1 file kode + 2 dokumentasi)** — Audit 14 title `AlertDialog`: 2 kelompok (konfirmasi destruktif
selalu diakhiri "?", form/info tidak) sudah konsisten. `"Hapus dari Perangkat?"` Title Case
dikonfirmasi SENGAJA (echo label menu/ikon yang sama persis, bukan bug). **Bug nyata**: warning
"tidak bisa dibatalkan" pakai `"Aksi"` (2 file) vs `"Tindakan"` (`LibraryScreen.kt`, 2 titik) —
disamakan ke `"Aksi"` (pola mayoritas). Brace/paren `LibraryScreen.kt` seimbang. **Cap 3 file
dijaga ketat**: 1 kode + 2 dokumentasi, `MICRO_UIUX_AUDIT.md` sengaja TIDAK disentuh batch ini
(status table-nya menyusul batch berikutnya kalau ada slot — tidak mau ulang pelanggaran cap
Batch 142). **Sisa Pending Queue kategori #1**: tulis formal hasil cek `"Hapus"` (22 titik,
sudah dicek sekilas — semua konteks beda, bukan kandidat unifikasi) + sinkronkan status table
`MICRO_UIUX_AUDIT.md` (masih tertulis "Batch 142-143" di sana, belum sebut Batch 144). Detail:
`CHANGELOG.md` Batch 144.

**Batch 143 (Micro UI/UX kategori #1 lanjutan — audit "Batal" vs "Tutup", 0 bug, 3 file
dokumentasi, 0 kode)** — Lanjutan Pending Queue Batch 142. Baca konteks 17 titik `"Batal"`/
`"Tutup"` (bukan cuma grep nama tombol): **pola sudah konsisten by-design** — `"Batal"` selalu
di dialog yang punya `confirmButton` beraksi (ada yang bisa dibatalkan), `"Tutup"` selalu di
dialog info-only/tanpa aksi tertunda (viewer laporan, penjelasan, atau state confirmButton
sudah berubah makna). 0 bug, 0 file kode diedit. **Catatan kepatuhan batch-limit**: batch ini
sengaja HANYA 3 file dokumentasi (CHANGELOG/PROJECT_STATE/MICRO_UIUX_AUDIT), 0 kode — kalau ada
temuan bug yang perlu fix kode di audit lanjutan, dokumentasi WAJIB dipangkas jadi ≤2 file supaya
total tetap ≤3 (pelajaran dari pelanggaran cap di Batch 142, ditandai user). **Sisa Pending
Queue kategori #1**: (1) kapitalisasi & tanda baca title dialog konfirmasi, (2) tulis formal
hasil cek `"Hapus"` (22 titik, sudah dicek sekilas — beda konteks, bukan kandidat unifikasi).
Detail: `CHANGELOG.md` Batch 143.

**Batch 142 (Micro UI/UX kategori #1 dimulai — wording undo-hide disamakan, 1 file diedit)** —
Kategori #4 (Touch Target) ✅ selesai penuh sejak Batch 141, lanjut kategori #1 (String & Wording
Consistency) per `FINAL EXECUTION ORDER` di `MICRO_UIUX_AUDIT.md`. Scope tetap sengaja sempit
sejak Batch 125 — wording murni, **tanpa** migrasi ke `strings.xml`. Temuan pertama: label undo
di banner custom `LibraryScreen.kt` (undo sembunyikan-lagu, Batch 66) pakai `"Batalkan"`, beda
dari label kanonik `"Urungkan"` yang dipakai semua `UndoableAction` lain via Snackbar
(`MainActivity.kt:767`) — disamakan. 0 logic berubah. **Pending Queue kategori #1** (belum
digarap, bukan terlewat — micro-batching): (1) verifikasi 1-per-1 20 titik `"Batal"`/`"Tutup"`
apakah polanya genuinely konsisten (baru dicek sekilas, tampak benar tapi belum formal), (2)
audit kapitalisasi & tanda baca title dialog konfirmasi, (3) tulis formal hasil audit `"Hapus"`
(22 titik, sudah dicek sekilas semua beda konteks — bukan kandidat unifikasi). Brace/paren
`LibraryScreen.kt` seimbang (330/330, 701/701). Kategori #1 status: 🟡 dimulai. Detail:
`CHANGELOG.md` Batch 142.

**Batch 141 (Micro UI/UX kategori #4 — hit-target audit formal + ripple-clip audit, tuntaskan
kategori #4 penuh, 2 file diedit)** — Lanjutan `MICRO_UIUX_AUDIT.md`, 2 item terakhir yang
tercatat "belum" di kategori #4: hit-target size audit formal + ripple-terpotong-container audit.
Kandidat tombol sekunder di 4 sheet (BackupRestoreSheet/DuplicateFinderSheet/SignatureMatcherSheet/
SongInfoEditSheet — TextButton "Batal"/"Tutup" dalam `AlertDialog`) DICEK ULANG dulu, dikonfirmasi
tetap keputusan sadar sejak Batch 124/127 (sekali-tekan, bukan repetitive-tap) — TIDAK disentuh,
supaya tidak mengulang kerja yang sudah pernah ditolak dengan alasan jelas.

**Hit-target size audit** — grep seluruh `IconButton(`/`FilledIconButton(` (40 titik total) +
custom `.clickable()` (46 titik) di `ui/*.kt`, cari `Modifier.size()` eksplisit di bawah 48dp
(minimum Material). 2 gap nyata ditemukan (bukan tebakan): `FeatureHintBanner.kt` dismiss button
40dp (sudah pernah dinaikkan dari 28dp di Batch 31, tapi belum sampai 48dp) dan
`HomeScreen.kt`'s `ContinueListeningCard` play button 44dp. Fix: keduanya dinaikkan ke 48dp —
**icon visual DI DALAM tombol TIDAK ikut diperbesar** (16dp close icon, 24dp default PlayArrow),
karena hit-target vs ukuran visual adalah 2 hal berbeda: IconButton 48dp cuma memperluas area
sentuh transparan di sekeliling icon kecil yang sama, bukan bikin komponennya kelihatan lebih
"penuh" secara visual. Semua 38 IconButton lain sudah default Material 48dp tanpa override
eksplisit (dikonfirmasi grep, bukan diasumsikan).

**Ripple-terpotong-container audit** — grep pola `.clip()` yang dipasang langsung di
container/ancestor `IconButton`/`.clickable()` (kelas bug yang sama dengan "Ambient Light gak
bocor" Batch 81 & scanline containment Batch 135/137, tapi arah sebaliknya — clip yang terlalu
ketat bisa memotong ripple, bukan cuma bocor). **0 kasus ditemukan** — tidak ada `IconButton`
yang di-clip ancestor-nya secara langsung di seluruh codebase.

**Kategori #4 (Touch Target & Micro Interaction) sekarang ✅ SELESAI PENUH** — checklist ini
ditutup total (Batch 124-127 + 141), giliran berikutnya kalau lanjut MICRO_UIUX: kategori #1
(String & Wording Consistency) sesuai urutan `FINAL EXECUTION ORDER` di `MICRO_UIUX_AUDIT.md`
(kategori #4 sebenarnya dikerjakan duluan atas permintaan eksplisit user waktu itu, bukan urutan
dokumen — 13 kategori lain masih ⬜ belum mulai). 2 file diedit, 0 file baru, 0 protected asset.
Brace/paren dicek otomatis & seimbang (FeatureHintBanner 4/4 brace 22/22 paren, HomeScreen
67/67 brace 199/199 paren). **Belum diverifikasi visual/build sungguhan** — prioritas
berikutnya kalau user push: buka Beranda (kartu "Lanjutkan Mendengarkan") & banner hint apa pun,
pastikan area sentuh terasa lebih nyaman tanpa icon-nya kelihatan membesar aneh. Detail:
`CHANGELOG.md` Batch 141.

**Batch 140 (Dokumentasi — arsipkan ROADMAP_15_FITUR_OFFLINE.md, 1 file di-rename + 2 dokumentasi
diedit)** — Keputusan eksplisit user: dokumen roadmap dihentikan karena 2 item tersisa (#13
Konverter Format Audio Lokal, #15 Alarm Musik) dinilai user tidak akan dipakai. **Diarsipkan,
BUKAN dihapus** — 13 dari 15 fitur di dalamnya sudah ✅ selesai dan riwayat itu tetap berguna
sebagai referensi, tiap entri ✅ menunjuk nomor Batch yang bisa dicari di `CHANGELOG.md`. File
di-rename `ROADMAP_15_FITUR_OFFLINE.md` → `ARCHIVED_ROADMAP_15_FITUR_OFFLINE.md` + banner
"📦 ARSIP — DIHENTIKAN" ditambah di paling atas (isi 15 item di bawahnya TIDAK diubah sama
sekali). `FILE_MANIFEST.txt` diperbarui (nama file + posisi alfabetis dikoreksi — sempat salah
taruh sebelum `app/*` padahal harusnya sebelum `CHANGELOG.md`, huruf besar disortir duluan di
`git ls-files`). Dicek dulu referensi lain sebelum rename (bukan asumsi aman): cuma
`FILE_MANIFEST.txt` yang menunjuk nama file ini secara langsung; file kode lain (`VisualizerSheet.kt`
dkk.) yang menyebut "roadmap item #X" di komentar cuma referensi tekstual historis, bukan
import/path — aman tidak ikut disentuh. 0 kode disentuh. **Kalau user berubah pikiran soal
#13/#15 nanti, tinggal buka file arsip ini lagi — tidak perlu dibuat ulang dari nol.** Detail:
`CHANGELOG.md` Batch 140.

**Batch 139 (Dokumentasi — sinkronkan status Editor Tag Metadata di ROADMAP_15_FITUR_OFFLINE.md,
1 file dokumentasi diedit)** — User tanya "roadmap apa yang pending", audit ditemukan item #1
(Editor Tag Metadata) di roadmap ini masih tercatat belum dikerjakan padahal SUDAH selesai sejak
Batch 118 — dikerjakan lewat jalur dokumen Gap List terpisah (`AudioPlayer_Coding_Gap_Updated.md`,
bukan dari daftar 15 fitur roadmap ini), jadi status di file ini tidak pernah ikut ter-update.
Dikonfirmasi langsung ke codebase sebelum ditandai (bukan asumsi): `Id3TagWriter.kt`/
`TagEditor.kt`/`SongInfoEditSheet.kt` semua ada + README § Fitur baris "Edit Info Lagu (Tag
Editor)" sudah ada. Fix: item #1 ditandai ✅ SELESAI (Batch 118) + catatan sinkronisasi + tabel
prioritas diperbarui. 0 kode disentuh, murni housekeeping dokumentasi (pola sama Batch 123).

**Sisa roadmap yang genuinely PENDING setelah audit ini** (2 dari 15 item, keduanya sengaja
belum dieksekusi — bukan terlewat):
- **#13 Konverter Format Audio Lokal** (Effort Tinggi/Risiko Tinggi) — butuh encoder codec
  tambahan (FLAC/MP3 encoder tidak semua built-in `MediaCodec`), isu ukuran APK & lisensi encoder
  belum diaudit.
- **#15 Alarm Musik (Wake-Up Alarm)** (Effort Sedang-Tinggi/Risiko Sedang-Tinggi) — butuh
  `AlarmManager.setExactAndAllowWhileIdle` + `SCHEDULE_EXACT_ALARM` (API 31+) + `BOOT_COMPLETED`
  receiver baru (pola mirip `BubbleBootReceiver` Batch 98, tapi domain beda total).

**Selain roadmap 15-fitur ini, ada 1 dokumen tracking terpisah yang juga pending**:
`MICRO_UIUX_AUDIT.md` (14 kategori polish presentation-only) — baru kategori #4 (Touch Target)
yang disentuh (🟡 sebagian, Batch 124-127), 13 kategori lain (#1 String Consistency, #2 Spacing,
#3 Typography, #5 Interactive States, #6-14 Now Playing s/d Component Consistency) masih ⬜
belum mulai sama sekali. Detail: `CHANGELOG.md` Batch 139.

**Batch 138 (Konfigurasi — isi UPDATE_REPO_OWNER, 1 file diedit)** — User kirim URL repo asli
(`https://github.com/FDzaki-dev/AudioPlayer`), menutup item "WAJIB diisi manual" yang tercatat
sejak Batch 136. `gradle.properties`: `UPDATE_REPO_OWNER=ganti-username-github` (placeholder) →
`UPDATE_REPO_OWNER=FDzaki-dev`. `UPDATE_REPO_NAME=AudioPlayer` sudah benar sejak awal (nama repo
cocok), tidak disentuh. Dicek ulang wiring-nya di `app/build.gradle.kts` (protected, TIDAK
diedit batch ini — cuma dibaca utk verifikasi): `buildConfigField` baca
`project.findProperty("UPDATE_REPO_OWNER")` persis dari key ini, jadi fitur "Cek Update" di
Settings → Lanjutan → Tentang Aplikasi sekarang genuinely bisa nemu rilis dari repo yang benar
begitu di-build ulang. 1 file diedit (bukan protected asset — `gradle.properties` sendiri bukan
di daftar protected, cuma nilai di dalamnya yang sebelumnya sengaja placeholder), 0 file baru, 0
protected asset tersentuh. **Belum diverifikasi runtime** (tidak ada network/GitHub API access
di sandbox ini) — prioritas berikutnya kalau user push: rebuild, buka "Cek Update", pastikan
app genuinely menemukan release terbaru dari `FDzaki-dev/AudioPlayer` (bukan 404 — cek juga
minimal ada 1 GitHub Release dgn asset `.apk` di repo tsb, kalau belum pernah rilis apa pun
"Cek Update" akan gagal bukan karena config salah). Detail: `CHANGELOG.md` Batch 138.

**Batch 137 (Calm Retro v3 — scanline ke 3 sheet tersisa: LyricsSheet+ABRepeatBookmarkSheet+
QueueSheet, 3 file diedit)** — Menutup "sengaja belum" Batch 135 (waktu itu ditunda demi batch
kecil, kandidat eksplisit: `LyricsSheet`, `ABRepeatBookmarkSheet`, `QueueSheet`). Pola identik
persis `EqualizerSheet.kt`/`VisualizerSheet.kt` Batch 135: `.clip(MaterialTheme.shapes.large)`
DULU sebelum `.calmScanlines()` (containment wajib — `frostedGlass()`'s `background()` sudah
shaped tapi tidak meng-`clip()` children/draw sesudahnya, kelas bug sama "Ambient Light gak
bocor" Batch 81), `isCalmRetro` di-hoist di titik yang sama seperti sheet lain. 3 import baru
per file (`androidx.compose.ui.draw.clip`, `isCalmRetroTheme`, `calmScanlines`) — 0 file file
sebelumnya sudah punya `clip` import (dicek grep dulu). `QueueSheet.kt` beda kecil dari 2 file
lain: Column modifier chain aslinya cuma `fillMaxWidth().frostedGlass()` tanpa `.padding()`
(padding dikelola per-child), jadi `.then(...)` ditaruh langsung setelah `frostedGlass()` tanpa
menyentuh urutan lain. `frostedGlass()` sendiri TIDAK diubah (perbaikan lokal ke 3 pemanggil
baru, bukan general clip semua pemanggil — pola sama presedan Batch 135). 0 file baru, 0
protected asset. **Cakupan calmScanlines() app-wide sekarang selesai penuh di semua sheet/panel
kontrol** (`AlbumArtHero`, `SongRow`, `EqualizerSheet`, `VisualizerSheet`, `LyricsSheet`,
`ABRepeatBookmarkSheet`, `QueueSheet`) — kalau ada sheet baru ke depannya, tinggal copy pola
yang sama, bukan gap yang perlu diaudit ulang. **PENTING kalau lanjut sesi baru**: fix ini baru
diverifikasi LOGIS dari kode (0 JDK/SDK di sandbox) — belum ada konfirmasi visual/build dari
user untuk ketiga sheet ini. Detail: `CHANGELOG.md` Batch 137.

**Batch 136 (Release Downloader Spec — cek update manual dari GitHub Release, 9 file)** —
Membalik keputusan Batch 8 (dulu sengaja 0 INTERNET permission demi klaim privasi), atas
persetujuan eksplisit user. `INTERNET`+`REQUEST_INSTALL_PACKAGES`+`<provider>` FileProvider baru
di `AndroidManifest.xml`, dipakai HANYA oleh tombol manual "Cek Update" baru (Settings → Lanjutan
→ Tentang Aplikasi) — tidak ada auto-check background. Downloader (`update/UpdateDownloader.kt`)
streaming chunk 8KB ke disk via Okio, TIDAK PERNAH buffer biner APK penuh di RAM; timeout
15s/20s, follow-redirect (CDN GitHub), header Accept+Authorization Bearer sesuai spec.
`update/GitHubReleaseChecker.kt` baca `releases/latest`, `update/UpdateManager.kt` orkestrasi
state (singleton terisolasi, 0 sentuh PlaybackService/PlayerViewModel). **PENTING kalau lanjut
sesi baru**: `gradle.properties` punya `UPDATE_REPO_OWNER=ganti-username-github` — masih
placeholder, WAJIB diganti ke username GitHub asli sebelum fitur ini berfungsi (lihat juga
"Owner/repo" di bawah). Belum ada verifikasi build (0 JDK/SDK di sandbox) — cek compile pertama
kali di Termux/CI. Detail: `CHANGELOG.md` Batch 136.

**Batch 135 (Calm Retro v3 — scanline ke panel kontrol Equalizer+Visualizer, 2 file diedit)** —
Lanjutan item "sengaja belum" Batch 134: `calmScanlines()` disebar ke `EqualizerSheet.kt` +
`VisualizerSheet.kt` (shell identik sejak Batch 92). Ditemukan risiko containment SEBELUM
dipasang (cross-check `frostedGlass()`): `background(tint,shape)` di situ tidak `clip()`
children/draw sesudahnya (kelas bug sama dgn "Ambient Light gak bocor" Batch 81) — fix:
`.clip(MaterialTheme.shapes.large)` dipasang SEBELUM `.calmScanlines()` di kedua file, `isCalmRetro`
di-hoist pola sama sheet lain. `frostedGlass()` sendiri TIDAK diubah (perbaikan lokal ke 2
pemanggil, bukan general clip semua pemanggil — hindari efek samping ke shadow/bevel Tactile/
Skeu). 0 file baru, 0 protected asset. **Sengaja belum**: sheet lain (`LyricsSheet`,
`ABRepeatBookmarkSheet`, `QueueSheet`, dst.) — kandidat sama tapi ditunda, pola sama presedan
aberrasi CTA yang meluas bertahap. **PENTING kalau lanjut sesi baru**: fix ini baru diverifikasi
LOGIS dari kode (0 JDK/SDK di sandbox) — belum ada konfirmasi visual/build dari user, terutama
apakah clip baru ini menyebabkan efek visual tak diinginkan lain di panel Equalizer/Visualizer
Calm Retro (belum pernah dirender). Detail: `CHANGELOG.md` Batch 135.

**Batch 134 (Calm Retro v3 — tuntaskan 2 item tunda Batch 133, 2 file diedit)** — Lanjutan
langsung 2 catatan "sengaja belum digarap" Batch 133: (1) `calmScanlines()` (Pilar A) disebar
dari `AlbumArtHero` ke `SongRow` (`LibraryScreen.kt`) — 1 composable dipakai ulang di tab Lagu/
`GroupedListView`/`SearchResultsView` (3 call site), jadi 1 edit (`isCalmRetro` hoist + scanline
di thumbnail AlbumArt 48dp SETELAH `.clip()`) otomatis menjangkau ketiganya. (2) Audit "blur
album-art 80dp/15% backdrop" — ternyata BUKAN gap fungsional (backdrop generik sudah ada semua
identitas sejak Batch 67), cuma beda angka dari literal spec; Calm Retro sekarang dapat
intensitasnya sendiri (`NowPlayingScreen.kt`, backdrop 80dp/alpha 0.15f digate `isCalmRetro`,
identitas lain tetap 60dp/0.5f seperti sebelumnya). 0 file baru, 0 protected asset. **Sengaja
belum**: scanline ke panel kontrol/sheet lain (Equalizer/Visualizer dst.) — di luar cakupan
"daftar lagu" yang diminta, kandidat lanjutan terpisah. **PENTING kalau lanjut sesi baru**: fix
ini baru diverifikasi LOGIS dari kode (0 JDK/SDK di sandbox) — belum ada konfirmasi visual/build
dari user, terutama kontras 15% vs 50% alpha backdrop yang paling berisiko meleset dari niat
"jauh"/subtle spec tanpa device. Detail: `CHANGELOG.md` Batch 134.

**Batch 133 (Calm Retro v3 upgrade — Pilar A/C/D dari spec baru, 3 file diedit)** — User upload
`palet_warna_calm_retro_v3.md`, penerus v2 (Batch 128-132). 7 HEX §1 identik v2 (0 warna
berubah). 3 pilar identitas yang belum pernah digarap ditutup: (A) `calmScanlines()` baru
(`TactileDepth.kt`) — garis CRT 4px via `Brush.verticalGradient`+`TileMode.Repeated`, dipasang
di `AlbumArtHero` (`NowPlayingScreen.kt`) SETELAH `.clip()` supaya tidak meluber. (D)
`calmGrain()` baru (`TactileDepth.kt`) — speckle field seeded via `drawWithCache` (bukan
bitmap/RenderEffect, minSdk 23 tidak dukung itu), dipasang di root Surface `MainActivity.kt`
(protected, parsial) slot sama dgn `identityRootBrush` identitas lain, alpha 0.015-0.04f. (C)
`FontFamily.Monospace` HANYA di 2 `Text` durasi/waktu Now Playing (gated `isCalmRetro`), SENGAJA
tidak disentuh ke judul/lirik (larangan eksplisit spec §4). Pilar B (aberrasi CTA) sudah ada
sejak Batch 129, 0 sentuhan batch ini. **Sengaja belum digarap**: scanlines belum disebar ke
Card list lagu/panel kontrol lain (spec sebut itu juga, kandidat lanjutan kalau diminta — pola
sama presedan aberrasi CTA yang mulai 1 titik lalu meluas Batch 130-131); blur album-art
80dp/15% backdrop (bagian "Do's" spec, bukan salah satu 4 Pilar inti) belum diaudit; monospace
belum ke tag kualitas audio (app belum punya UI bitrate/format eksplisit). **PENTING kalau
lanjut sesi baru**: fix ini baru diverifikasi LOGIS dari kode (0 JDK/SDK di sandbox) — belum ada
konfirmasi visual/build dari user, jangan anggap selesai sebelum ada screenshot/build hijau baru.
Detail: `CHANGELOG.md` Batch 133.

**Batch 132 (FIX — Calm Retro tenggelam di lagu beraksen kuat, 2 file diedit)** — User lapor
pakai screenshot: CTA play tampak flat merah polos, aberrasi tak kelihatan sama sekali. Root
cause: `animatedAccent` (CTA+wash+rating) selalu ikut `accentColor` dinamis dari ekstraksi
album art per-lagu (`accentColor ?: fallback`), fallback ke warna tema HANYA kalau ekstraksi
null — utk album art didominasi warna kuat, Muted Sage & aberrasi 0.35f-alpha ketimpa total.
Fitur tint-dari-album-art ini disengaja & lama (berlaku semua identitas), tapi bertabrakan
dgn filosofi "Calm Retro terkunci total" (Batch 128 dark-lock) — lock-nya sekarang meluas ke
accent. Fix 1 baris/file di `NowPlayingScreen.kt`+`MiniPlayerBar.kt`: `if (isCalmRetro)
fallback else (accentColor ?: fallback)` — identitas lain 0 perubahan perilaku (`isCalmRetro`
sudah di-hoist sejak Batch 129, 0 hoist baru). **PENTING kalau lanjut sesi baru**: fix ini
baru diverifikasi LOGIS dari kode (0 JDK/SDK di sandbox) — user BELUM konfirmasi visual hasil
build ulang, jangan anggap selesai sebelum ada konfirmasi/screenshot baru dari user. Detail:
`CHANGELOG.md` Batch 132.

**Batch 131 (Calm Retro — live-showcase preview picker Settings, 1 file diedit)** — Menutup
gap terakhir dari audit cakupan (Batch 130): Tactile/Skeu sudah live-showcase di baris preview
`ThemeOptionCard`, Calm Retro belum. `SettingsScreen.kt` — `calmAberration()` (fungsi Batch 129,
reuse) diterapkan ke lingkaran aksen 30dp preview saja (bukan seluruh Surface kartu seperti
Tactile/Skeu — Card Calm Retro tetap flat/opaque sesuai keputusan Batch 130), meniru scope asli
CTA play/pause. 0 fungsi baru, 0 protected asset, 1 file diedit. **Audit cakupan Calm Retro app-
wide sekarang selesai penuh**: warna/shape otomatis ke seluruh app lewat MaterialTheme, CTA
utama (2 lokasi) + preview picker sudah dapat efek aberrasi khas; bevel/glass/ambient-wash
Tactile/Skeu sengaja tidak direplikasi (bukan gap, identitas Calm Retro memang flat/opaque per
spec). **Kandidat batch berikutnya kalau diminta lanjut**: lanjutkan `FINAL EXECUTION ORDER` di
`MICRO_UIUX_AUDIT.md` (lihat Batch 127-128) — di luar itu, identitas Calm Retro dianggap
selesai kecuali ada instruksi baru dari user. Detail: `CHANGELOG.md` Batch 131.

**Batch 130 (Calm Retro — pemisahan & pemurnian visual dari identitas lain, 1 file diedit)** —
Lanjutan Batch 128-129, fase "pemurnian": hapus semua token yang masih dipinjam identitas lain
supaya Calm Retro otonom penuh (prinsip sama dgn Tactile/Skeu sejak Batch 61). `Theme.kt`
satu-satunya file: `tertiary`/`error` dulu reuse `SkeuDarkSuccess`/`SkeuDarkError`, sekarang
reuse token milik Calm Retro sendiri (`CalmRetroAccent`/`CalmRetroAberrationLeft` — 0 warna
baru ditambah, "gak usah greedy"). `CalmRetroShapes` (BARU) — dulu jatuh ke `else` branch
(warisan `AppleShapes`), sekarang shape sendiri paling mepet dari 4 identitas (`Radius.xs/sm/
md`), dipakai di Card/Sheet/NavigationBar M3. **Sengaja tidak diubah**: shape play/pause tetap
`CircleShape` (branch `else` tetap benar — sesuai literal spec `.calm-play-button {border-
radius:50%}`), typografi tetap reuse `AppleTypography` (spec tidak beri spesifikasi tipografi,
pola sama seperti Skeu Batch 57 — bukan kebocoran identitas, beda kasus dari tertiary/error/
shape). **Kandidat batch berikutnya kalau diminta lanjut**: preview live-showcase Calm Retro
di `ThemeOptionCard` (`SettingsScreen.kt`, sengaja belum disentuh Batch 128-130, pola sama
`isTactilePreview`/`isSkeuPreview`), atau lanjutkan `FINAL EXECUTION ORDER` di
`MICRO_UIUX_AUDIT.md` (lihat Batch 127-128). Detail: `CHANGELOG.md` Batch 130.

**Batch 129 (Calm Retro — efek aberrasi CTA play/pause, 5 file diedit)** — Lanjutan Batch 128,
item kandidat (a), user minta lanjut dengan instruksi eksplisit "gak usah overthinking &
greedy" — discoped ke 1 titik CTA (tombol play/pause) saja, bukan disebar semua tombol app.
`calmAberration()` (fungsi baru di `TactileDepth.kt`, akhir file) — terjemahan Compose dari CSS
`box-shadow` ganda spec markdown (`.calm-play-button`): 2 radial-gradient offset kiri-atas
(Dusty Rose)/kanan-bawah (Dusty Denim) fade transparent, alpha 0.35f (sesuai guideline spec
"30%-40%"). `isCalmRetroTheme()` (`Theme.kt`) pola sama `isTactileTheme()`/`isSkeuTheme()`.
Diwire ke KEDUA lokasi tombol play/pause utama (`NowPlayingScreen.kt` + `MiniPlayerBar.kt`) —
ikut pola Batch 55/58 yang selalu sinkronkan dua lokasi ini (identitas yang cuma dapat
treatment di 1 lokasi = bug, bukan selesai, lihat pelajaran Batch 58). **Kandidat batch
berikutnya kalau diminta lanjut**: (b) lanjutkan `FINAL EXECUTION ORDER` di
`MICRO_UIUX_AUDIT.md` (lihat Batch 127/128). Detail: `CHANGELOG.md` Batch 129.

**Batch 128 (Tema baru — Calm Retro, terkunci gelap, 2 file diedit)** — Identitas ke-4 dari
`palet_warna_calm_retro_v2.md` user, TERKUNCI GELAP PERMANEN atas instruksi eksplisit user
(beda dari Tactile/Skeu Batch 61 yang otonom di kedua mode) — `colorsFor()` mengabaikan param
`isDark` untuk `ThemeIdentity.CALM_RETRO`, cuma 1 `CalmRetroColors` (`darkColorScheme`). 6
token warna literal dari tabel HEX spec ditambah ke `Color.kt`; sukses/error reuse token Skeu
yang sudah ada (instruksi "gak usah greedy" — tanpa token baru tak perlu). Shape/typografi
reuse `AppleShapes`/`AppleTypography` (branch `else` sudah ada). **Sengaja tidak dikerjakan**:
efek chromatic-aberration CSS dari spec (`.calm-play-button`) — cuma contoh implementasi
opsional di markdown, bukan bagian konfigurasi warna inti; tidak dibuat primitive Compose
baru. Picker Settings otomatis menampilkan tema baru ini (loop `ThemeIdentity.entries`), 0
edit `SettingsScreen.kt`. **Kandidat batch berikutnya kalau diminta lanjut**: (a) efek
aberration CSS di atas kalau user memang mau, (b) lanjutkan `FINAL EXECUTION ORDER` di
`MICRO_UIUX_AUDIT.md` (sisa tombol sekunder + hit-target audit, lihat Batch 127). Detail:
`CHANGELOG.md` Batch 128.

**Batch 127 (Micro UI/UX — bounce-press tombol sekunder frekuensi-tinggi, 3 file kode diedit)**
— Lanjutan kategori #4: tombol **sekunder** yang ditekan berulang (bukan CTA sekali-tekan) —
`LyricsSheet` (Mundur/Lewati Baris di flow tap-to-sync), `VaultSheet` (icon keluarkan-dari-vault
per baris), `ABRepeatBookmarkSheet` (icon hapus bookmark per baris). Sengaja **tidak** disentuh:
tombol keluar/batal sekali-pakai (bukan repetitive-tap, prioritas rendah), sheet lain yang belum
diaudit sekunder-nya (`BackupRestoreSheet`/`DuplicateFinderSheet`/`SignatureMatcherSheet`/
`SongInfoEditSheet`/`RingtoneCutterSheet`/`EqualizerSheet`). **Kandidat batch berikutnya**: (a)
tuntaskan sisa tombol sekunder di 6 sheet itu (tapi turunkan prioritas: kebanyakan sekali-tekan,
dampak micro-feedback lebih kecil dari yang sudah dikerjakan), (b) hit-target size audit formal
kategori #4 (IconButton semua sudah Material default 48dp secara implisit, tinggal verifikasi
eksplisit + cek ripple tidak terpotong container), atau (c) mulai kategori #1 String & Wording
Consistency (scope sempit, tanpa migrasi `strings.xml`). Detail: `CHANGELOG.md` Batch 127.

**Batch 126 (Micro UI/UX — bounce-press FilterChip Equalizer+Ringtone Cutter, 2 file kode
diedit)** — Lanjutan kategori #4 `MICRO_UIUX_AUDIT.md`, giliran `FilterChip` (beda pola dari
`Button`): `EqualizerSheet` (2 baris preset chip) + `RingtoneCutterSheet` (`DestinationChip`
composable bersama, 1 edit → 3 chip). Dengan ini sub-bagian "bounce-press CTA & chip utama"
kategori #4 selesai di 9 sheet (Batch 124-126) — **belum** tombol sekunder & hit-target size
audit, jangan tandai kategori #4 ✅ penuh. **Kandidat batch berikutnya**: (a) tuntaskan sisa
kategori #4 (tombol sekunder TextButton/IconButton semua sheet + audit ukuran hit-target), atau
(b) mulai kategori #1 String & Wording Consistency (scope sempit: wording konsisten murni,
**tanpa** migrasi ke `strings.xml` — sudah ditandai berisiko di README soal 339 string literal
tanpa compiler). Detail: `CHANGELOG.md` Batch 126.

**Batch 125 (Micro UI/UX — adopsi MICRO_UIUX_AUDIT.md + bounce-press 4 sheet lagi, 4 file kode +
1 dokumentasi baru)** — User upload checklist 14-kategori polish presentation-only, disimpan
sebagai `MICRO_UIUX_AUDIT.md` (tracking persisten, status per kategori di paling atas file —
lihat pelajaran Batch 123 soal banner yang telat sync). Lanjutan kategori #4 (Touch Target &
Micro Interaction) dari Batch 124: `BackupRestoreSheet`/`DuplicateFinderSheet`/
`ABRepeatBookmarkSheet` (via `AbPointButton`)/`SignatureMatcherSheet` (via `ApkPickerRow`) kini
pakai `bouncyPress`. **Sengaja kecil**: `EqualizerSheet` (FilterChip) + semua tombol sekunder
ditunda ke batch berikutnya, sesuai arahan user "jangan greedy". **Kandidat batch berikutnya**:
lanjut `FINAL EXECUTION ORDER` di `MICRO_UIUX_AUDIT.md` — abis touch-target selesai (giliran
`EqualizerSheet` + tombol sekunder), lanjut ke kategori #1 (String & Wording Consistency, tapi
**tanpa** bagian "centralize ke resources" — itu tumpang tindih dengan item "Belum selesai" di
README soal 339 string literal yang sudah ditandai berisiko tanpa compiler; scope ke wording
konsisten murni, bukan refactor ke `strings.xml`). Detail: `CHANGELOG.md` Batch 125.

**Batch 124 (Micro UI/UX — bounce-press ke 4 sheet fitur terbaru, 4 file diedit)** — Audit
`bouncyPress` (`Utils.kt`) ternyata cuma dipakai di kontrol lama (`MiniPlayerBar`/
`NowPlayingScreen`/`LockScreen`); 4 sheet MVP Batch 118-121 (`SongInfoEditSheet`,
`RingtoneCutterSheet`, `VaultSheet`, `LyricsSheet`) masih `Button` polos. Fix: tambah
`interactionSource` + `.bouncyPress(...)` ke CTA utama tiap sheet saja (Simpan/Potong & Simpan/
Aktifkan Vault+Buka/Tandai Sekarang) — sengaja **tidak** sapu tombol sekunder (batal/undo/skip/
hapus), supaya batch tetap kecil sesuai arahan user "dilarang greedy". 0 logika berubah, 0
protected asset. **Kandidat batch berikutnya kalau diminta lanjut**: tombol sekunder di 4 sheet
ini, plus sheet lain yang belum diaudit (`BackupRestoreSheet`, `DuplicateFinderSheet`,
`ABRepeatBookmarkSheet`, `SignatureMatcherSheet`, `EqualizerSheet` — 0 tombol Button ditemukan
di grep awal, cek ulang kalau perlu). Detail: `CHANGELOG.md` Batch 124.

**Batch 123 (Dokumentasi — sinkronkan callout "Update terbaru", 1 file dokumentasi diedit)** —
User lapor sebagian entri dokumentasi terasa basi/harus scroll dulu baru kelihatan perubahan.
Audit ulang urutan `CHANGELOG.md` + `PROJECT_STATE.md` (pola sama Batch 94): **0 anomali**,
keduanya sudah newest-first dengan benar. Sumber sebenarnya: callout "🆕 Update terbaru" di
`README.md` (wajib disinkronkan manual tiap batch, lihat Batch 94) masih menunjuk Batch 121,
terlewat sync karena Batch 122 murni fix build (0 file dokumentasi disentuh di batch itu). Fix:
banner diperbarui ke Batch 122, tetap kredit fitur Batch 121. **Pelajaran**: callout manual-sync
di README rawan telat tiap kali ada batch fix-only (tanpa sentuh dokumentasi) yang menyusul
batch fitur — cek banner ini juga, bukan cuma urutan CHANGELOG/PROJECT_STATE, tiap ada laporan
dokumentasi "ketinggalan". Detail: `CHANGELOG.md` Batch 123.

**Batch 122 (Fix Build — Ringtone Cutter, 1 file diedit)** — CI (`log_fail_176.zip`) melaporkan
`compileDebugKotlin`/`compileReleaseKotlin` gagal: `RingtoneEncoder.kt:142` panggil
`AppLogger.i(...)` yang tidak ada (`AppLogger` cuma punya `e()`/`w()`). Fix 1 baris → `AppLogger.w`.
0 sisa pemanggilan `.i(` lain dicek via grep. **Masih belum diverifikasi compile Gradle
sungguhan** (sandbox tidak ada JDK/SDK) — ini fix pertama berdasar log CI ASLI (bukan
tebakan), jadi keyakinan lebih tinggi dari batch-batch sebelumnya, tapi tetap perlu 1 run CI
lagi untuk konfirmasi final (mungkin ada error lain yang baru kelihatan setelah error pertama
ini teratasi — Kotlin compiler kadang berhenti di error pertama per-file). Detail: `CHANGELOG.md`
Batch 122.

**Batch 121 (Roadmap #5 — Ringtone Cutter, 7 file — 4 baru + 3 diedit)** — Item berikutnya dari
tabel prioritas effort/risiko (Sedang/Sedang, terendah tersisa), dipilih karena reuse pola
scope-narrowing `TagEditor` (Batch 118) dan pola simpan-MediaStore `BackupManager`/`AppLogger`,
0 dependency Gradle baru.

`RingtoneCutter.kt` (baru, `data/`) — `TrimRange`+`clampRange()` (jepit ke batas lagu, durasi
1-60 detik)+`isValid()`+`formatTimestamp()`, pure/testable pola `AbRepeatLogic`.
`RingtoneEncoder.kt` (baru, `data/`) — potong via `MediaExtractor`+`MediaMuxer` stream-copy
(tanpa re-encode, `MUXER_OUTPUT_MPEG_4`), scope dipersempit ke lagu MediaStore + format MP3/AAC
saja (pola sama `TagEditor`), simpan sebagai file BARU ke `Ringtones|Notifications|
Alarms/AudioPlayer` (flag `IS_RINGTONE` dst, API 29+) — **karena selalu file baru (tidak pernah
menulis balik ke file asli), 0 alur consent dibutuhkan**, beda dari `TagEditor`.
`WRITE_SETTINGS`/set-as-default-otomatis SENGAJA tidak dikerjakan — fallback "simpan, pilih
manual di Pengaturan > Suara" (tetap mulus karena flag MediaStore bikin file auto-muncul di
pemilih nada dering sistem). `RingtoneCutterSheet.kt` (baru, `ui/`) — 2 `Slider` (bukan
`RangeSlider`, 0 precedent komponen itu) + 3 `FilterChip` tujuan. **MVP disengaja**: 0 preview
audio dari sheet ini. `RingtoneCutterTest.kt` (baru, `test/`) — 10 test pure logic.

`NowPlayingScreen.kt`/`PlayerViewModel.kt` (diedit) — 1 menu "Potong Nada Dering" +
`requestCutRingtone()` fire-and-forget lewat kanal `infoMessage`/`actionErrorMessage` yang
sudah ada. `MainActivity.kt` (diedit, **protected asset — edit parsial**) — 1 param baru di
call site `NowPlayingScreen(...)` yang sudah ada.

**Batasan jujur**: hasil potongan TIDAK otomatis jadi nada dering aktif sistem (butuh
`WRITE_SETTINGS`, izin sensitif yang sengaja dilewati) — user pilih manual dari Pengaturan >
Suara setelah tersimpan. Stream-copy tanpa re-encode BISA gagal diam-diam kalau `MediaExtractor`
salah pilih trek pada file berformat eksotis (belum diverifikasi di device sungguhan).

7 file (4 baru + 3 diedit), 0 protected asset lain selain `MainActivity.kt` (edit parsial).
Brace/paren dicek otomatis & seimbang. `FILE_MANIFEST.txt` 162→166 + `README.md` (1 baris fitur
+ banner) + `ROADMAP_15_FITUR_OFFLINE.md` (#5 selesai) sebelum repack. **Belum diverifikasi
compile/runtime Gradle sungguhan** (tidak ada JDK/Android SDK di sandbox ini) — prioritas
berikutnya kalau user push: (1) `./gradlew assembleDebug` build bersih (`MediaMuxer`/
`MediaExtractor` API paling berisiko salah ketik manual), (2) di device: potong 1 lagu MP3 & 1
M4A, pastikan hasil muncul di Pengaturan > Suara > Nada Dering, (3) putar hasil di app LAIN
(bukan app ini sendiri) pastikan tidak korup/silent, (4) coba lagu FLAC/WAV pastikan pesan
"belum didukung" muncul jelas, (5) coba di Android 9 ke bawah pastikan pesan "butuh Android 10+"
muncul (bukan crash). Detail lengkap: `CHANGELOG.md` Batch 121.

**Batch 120 (Roadmap #3 — Editor Lirik LRC Tap-to-Sync, 4 file — 2 baru + 2 diedit)** — Item
roadmap berikutnya setelah #14 (Batch 119), dipilih karena 100% reuse infrastruktur lirik yang
sudah ada (`LyricsStore`/`LyricsParser`/highlight-scroll `LyricsSheet.kt`), 0 dependency baru,
0 protected asset.

`LrcSyncEditor.kt` (baru, `data/`) — logika murni `SyncSession` (immutable) +
`mark()`/`skip()`/`undo()`/`formatTimestamp()`/`buildLrcText()`, pola sama `AbRepeatLogic`. Baris
yang di-skip tetap plain di hasil akhir (bukan dipaksa dapat timestamp) — `buildLrcText()` boleh
hasilkan campuran synced+plain, disengaja. `LyricsSheet.kt` (diedit) — 2 param baru default aman
(`isPlaying`/`onPlayPause`), tombol "Mode Tap-to-Sync (LRC)" baru di mode edit teks yang sudah
ada, flow sync 1-baris-per-giliran (Tandai/Mundur/Lewati/Batal) yang begitu selesai auto-isi
`draft` lalu balik ke text field untuk REVIEW manual sebelum "Simpan" (bukan auto-save).
`NowPlayingScreen.kt` (diedit) — 2 baris baru di call site `LyricsSheet(...)` yang sudah ada
(`uiState.isPlaying`/`onPlayPause` yang sudah ada di scope) — **0 baris `MainActivity.kt`
disentuh** (bukan protected asset). `LrcSyncEditorTest.kt` (baru, `test/`) — 10 test murni.

**Batasan jujur**: kalau draft sudah campur sebagian ber-`[mm:ss.xx]`, Mode Tap-to-Sync
memperlakukan prefix lama itu apa adanya sebagai bagian teks baris (tidak di-strip) — MVP ini
untuk lirik plain-text murni, bukan re-sync sebagian. Sekalian dibetulkan: `ROADMAP_15_FITUR_
OFFLINE.md` item #6 & #7 (sudah selesai lewat Gap List Batch 117/115) baru ditandai selesai di
file roadmap ini sekarang — housekeeping lama yang kelewat, ditemukan pas audit roadmap batch
ini, bukan kerja tambahan yang dicari-cari.

4 file (2 baru + 2 diedit), 0 protected asset. Brace/paren dicek otomatis & seimbang.
`FILE_MANIFEST.txt` 160→162 + `README.md` (1 baris fitur + banner) + `ROADMAP_15_FITUR_
OFFLINE.md` (#3 selesai + #6/#7 dibetulkan) sebelum repack. **Belum diverifikasi compile/
runtime Gradle sungguhan** (tidak ada JDK/Android SDK di sandbox ini) — prioritas berikutnya
kalau user push: (1) `./gradlew assembleDebug` build bersih, (2) di device: tempel lirik plain,
masuk Mode Tap-to-Sync, tandai tiap baris sambil lagu diputar, pastikan timestamp akurat & baris
Lewati tetap plain di hasil akhir, (3) Mundur mengembalikan ke baris sebelumnya dgn stempel
genuinely terhapus, (4) tutup+buka lagi sheet di tengah sesi sync tidak nyangkut/crash. Detail
lengkap: `CHANGELOG.md` Batch 120.

**Batch 119 (Roadmap #14 — Vault Lagu Privat, PIN-gated song vault, 6 file — 2 baru + 4
diedit)** — Item "Sangat disarankan" berikutnya (Sedang/Rendah risiko) dari
`ROADMAP_15_FITUR_OFFLINE.md`, dipilih karena sudah eksplisit dicatat "banyak infrastruktur
sudah ada tinggal disambungkan ulang" (reuse pola `AppLockStore`/`PinLockoutPolicy` +
`LibraryFilterStore.apply()`), 0 dependency Gradle/network baru — cocok dikerjakan di sandbox
ini tanpa risiko blocking seperti Gradle Wrapper/Release Lint Gate.

`VaultStore.kt` (baru, `data/`) — PIN sendiri, INDEPENDEN total dari `AppLockStore` (prefs
`vault` terpisah, bukan reuse `AppLockStore` dengan nama prefs beda) — 2 lock sengaja tidak
saling terikat (app boleh tidak terkunci sementara lagu tertentu tetap terkunci). Formula
lockout escalating tetap dipakai bersama lewat `PinLockoutPolicy` (memang pure/Context-free
untuk reuse ini), cuma plumbing hash/storage-nya diduplikasi (~15 baris, sengaja — menghindari
AppLockStore ikut tersentuh sama sekali). Simpan `Set<Long>` lagu vaulted +
`pruneOrphans(validIds)` (pola sama `FavoritesStore`/`RatingStore`, Gap List #9) +
`apply(songs)` one-liner, dirantai di call site yang sama seperti `LibraryFilterStore.apply()`.

`VaultSheet.kt` (baru, `ui/`) — 3 state (setup PIN → unlock PIN dgn countdown lockout live →
list lagu vaulted + tambah/keluarkan/nonaktifkan). Unlock state SESSION-ONLY (`remember`
biasa) — sheet ditutup = PIN diminta lagi berikutnya, disengaja bukan bug. **MVP disengaja**:
murni manajemen keanggotaan, 0 tombol putar langsung dari sheet ini (keluarkan dulu dari vault
untuk memutar).

`HomeScreen.kt`/`LibraryScreen.kt` (diedit) — `VaultStore(context).apply(...)` dirantai SETELAH
`LibraryFilterStore(context).apply(rawSongs)` yang sudah ada (1 baris/file). `LibraryFilterStore.kt`
sendiri SENGAJA tidak disentuh sama sekali — `LibraryFilterStoreTest.kt` tetap valid tanpa
ditinjau ulang, 2 store tetap independen. `SettingsScreen.kt` (diedit) — 1 row menu baru, 0
parameter baru ke fungsi (reuse `songs: List<Song>` yang sudah ada sejak Batch 117). `PlayerViewModel.kt`
(diedit) — `vaultStore.pruneOrphans(validIds)` di `refreshLibrary()`, pola sama 3 store lain
di titik yang sama.

**Batasan jujur**: perubahan keanggotaan vault (dikelola dari Settings) baru tercermin di
Home/Library begitu `remember(rawSongs, ...)` re-run di layar itu (navigasi ulang), bukan live
sinkron seketika kalau kedua layar itu tetap terbuka bersamaan — kelas keterbatasan yang sama
sudah diterima project ini untuk penulisan lintas-store lain (Batch 115).

6 file kode (2 baru + 4 diedit), 0 protected asset. Brace/paren semua file dicek otomatis &
seimbang. `FILE_MANIFEST.txt` diperbarui (158→160) + `README.md` (1 baris fitur + banner
"Update terbaru" disinkronkan) + `ROADMAP_15_FITUR_OFFLINE.md` (#14 ditandai selesai) sebelum
repack. **Belum diverifikasi compile/runtime Gradle sungguhan** (tidak ada JDK/Android SDK di
sandbox ini) — prioritas berikutnya kalau user push: (1) `./gradlew assembleDebug` build
bersih, (2) di device: atur PIN vault, tambah 1 lagu, konfirmasi genuinely hilang dari
Beranda/Library (bukan cuma UI vault yang bilang begitu), (3) tutup+buka ulang sheet Vault,
pastikan PIN diminta lagi (disengaja, bukan bug), (4) PIN salah 5x, pastikan lockout &
countdown jalan sama seperti App Lock, (5) nonaktifkan vault, pastikan SEMUA lagu yang tadi
divault kembali normal tanpa perlu restart app. Detail lengkap: `CHANGELOG.md` Batch 119.

**Batch 118 (Gap List "Wajib" #1 — Tag/Metadata Editor MVP, 4 file baru + 3 diedit)** — Item
terakhir dari 4 "Wajib" yang realistis dikerjakan di lingkungan kerja ini (Gradle Wrapper &
Release Lint Gate sama-sama butuh `gradle`/Android SDK terpasang yang tidak ada di sandbox ini,
lihat catatan Batch 117). **Scope sengaja dipersempit, dicek dulu ke kode sebelum ditulis**: (1)
format MP3/ID3v2.3 SAJA (FLAC/OGG/M4A/WMA masing-masing format biner beda total, risiko rusak
file user tanpa compiler/device untuk verifikasi), (2) lagu MediaStore SAJA, BUKAN lagu folder
tambahan (SAF) — dicek ulang ke `PlayerViewModel.addCustomFolder()`: folder tambahan cuma dikasih
`FLAG_GRANT_READ_URI_PERMISSION` (baca saja), menulis balik ke situ butuh alur izin terpisah yang
belum ada. Kedua batasan ditampilkan APA ADANYA ke user di sheet edit (pesan beda per alasan),
bukan disembunyikan.

`Id3TagWriter.kt` (baru, `data/`) — writer/rewriter ID3v2.3 murni (0 Context/Android):
`buildTag()` (frame teks UTF-16LE+BOM seragam semua field), `rewrite()` (baca ukuran tag ID3v2
lama dari 10 byte header via syncsafe int, ganti dengan tag baru, byte audio sesudahnya disalin
byte-for-byte tanpa pernah didekode). ID3v1 trailer (kalau ada) sengaja tidak disentuh — gap
kosmetik dicatat, bukan diklaim selesai. `TagEditor.kt` (baru, `data/`) — orkestrasi consent
(Android 11+ `MediaStore.createWriteRequest`, pola identik `createDeleteRequest` yang sudah ada
untuk hapus lagu; Android 10 `RecoverableSecurityException`). **Alur tulis 2 langkah demi
keamanan file user**: tulis ke file sementara cache app dulu (file asli 0% tersentuh kalau ada
bug), baru salin isinya ke `song.uri` asli — risiko residual (file bisa TERPOTONG, bukan rusak
diam-diam, kalau app di-kill paksa persis di langkah kedua) dicatat jujur di komentar, TIDAK
diklaim 100% aman (Android tidak punya rename atomik lintas provider yang bisa diandalkan).

`SongInfoEditSheet.kt` (baru, `ui/`) — form edit metadata, pesan tidak-didukung dicerminkan apa
adanya dari `TagEditor.editabilityCheck` (TagEditor tetap otoritas final, sheet tidak validasi
ulang dengan logika terpisah yang bisa nyimpang). `Id3TagWriterTest.kt` (baru, `test/`) — test
murni logic biner (syncsafe, frame, rewrite in-memory), pembagian sama seperti
`MusicRepositoryTrackDiscTest` (helper murni diuji, bagian Context/cursor tidak).

`PlayerViewModel.kt` (diedit) — `requestSaveTags()`/`onTagWriteConsentResult()` +
`pendingTagWriteConsent`, pakai kanal `infoMessage`/`actionErrorMessage` yang sudah ada, 0 kanal
baru. `MainActivity.kt` (diedit, **protected asset — edit parsial**) — `tagWriteConsentLauncher`
(pola identik `deleteRequestLauncher`) + 1 param baru ke pemanggilan `NowPlayingScreen(...)` yang
sudah ada (lewat `nowPlayingContent` lambda Batch 101 — 1 titik edit berlaku utk Compact/Medium
DAN panel Expanded). `NowPlayingScreen.kt` (diedit) — 1 param baru, 1 menu "Edit Info Lagu" di
`AdvancedControlsSheet`, sheet baru key di `song.id` (pola sama `LyricsSheet`).

7 file kode (4 baru + 3 diedit) + `FILE_MANIFEST.txt` diverifikasi 100% match fisik (158/158,
diff bersih) + dokumentasi. Brace/paren semua file kode dicek otomatis & seimbang. 0 protected
asset lain tersentuh selain `MainActivity.kt` (edit parsial, sesuai aturan).

**Belum diverifikasi compile/runtime Gradle sungguhan** — prioritas berikutnya kalau user push:
(1) `testDebugUnitTest` (cek `Id3TagWriterTest.kt` hijau — bagian syncsafe/panjang frame paling
gampang salah hitung manual), (2) `assembleRelease`, (3) **di device sungguhan**: edit 1 lagu MP3
MediaStore, lalu verifikasi hasilnya pakai PLAYER LAIN (bukan app ini) — jangan cuma percaya UI
app ini sendiri, itu bisa saja cuma baca ulang state lama yang di-refresh, bukan bukti file fisik
benar tertulis, (4) pastikan lagu folder tambahan & lagu non-MP3 menampilkan pesan "belum
didukung" dengan benar, bukan macet/crash. Detail lengkap: `CHANGELOG.md` Batch 118.

**Batch 117 (Gap List "Wajib" #2 — Duplicate Detection, 2 file baru + 2 diedit)** — Audit ulang
(`AudioPlayer_Coding_Gap_Updated.md`) menandai 4 item "Wajib": Tag/Metadata Editor, Duplicate
Detection, Gradle Wrapper, Release Lint Gate. Duplicate Detection dikerjakan duluan — murni Kotlin
tanpa dependency binary/network, scope realistis untuk 1 batch (beda dari Tag Editor yang butuh
penulisan ID3/Vorbis/MP4 tag per format, atau Gradle Wrapper yang butuh `gradle-wrapper.jar`
biner asli yang TIDAK bisa dibuat dari lingkungan kerja ini — tidak ada akses network/`gradle`
lokal untuk generate-nya secara sah, lihat catatan di bawah).

`DuplicateDetector.kt` (baru, `data/`) — murni fungsi list-in/groups-out, 0 Context/I/O, 2
grouping TERPISAH secara sengaja: (1) "duplikat entri library" pakai signature identik
`PlayerViewModel.dedupeSignature()` (title+artist+durasi dibulatkan ke detik) — 2 entri bisa
match ini walau file fisiknya beda; (2) "duplikat file fisik" pakai heuristik (fileSize, durasi)
— bukan hash byte-per-byte (biaya I/O per lagu yang sama-sama dihindari di keputusan
bitrate/codec/genre sebelumnya), lagu `fileSize <= 0` dikecualikan. Tidak ada logic hapus di file
ini sama sekali (gap doc eksplisit: "Jangan melakukan delete otomatis").

`DuplicateFinderSheet.kt` (baru, `ui/`) — ModalBottomSheet tinggi 90% layar, 2 seksi (library vs
fisik) dari `DuplicateDetector`, checkbox per-lagu (manual, tidak ada default tercentang), tombol
"Hapus N Terpilih" hanya aktif kalau ada seleksi → `AlertDialog` konfirmasi eksplisit sebelum
`onDeleteSongs` dipanggil. **0 baris delete baru ditulis** — `onDeleteSongs` diteruskan dari
`MainActivity.deleteSongsFromDevice` yang sudah ada (persis pola `onDeleteSongs` LibraryScreen),
yang di Android 10+ tetap lewat dialog konfirmasi sistem (scoped storage) sebagai lapis kedua.

`SettingsScreen.kt` (diedit) — 1 row menu baru "Deteksi File Duplikat" (pola identik "Cadangkan &
Pulihkan"), 2 param baru `songs: List<Song> = emptyList()` dan `onDeleteSongs: (List<Song>) -> Unit
= {}` — DEFAULT VALUE sengaja dipasang (bukan cuma nullable) supaya call site lama/test fixture
lain yang mungkin memanggil `SettingsScreen(...)` tanpa 2 param ini tetap compile tanpa disentuh.
`MainActivity.kt` (diedit, **protected asset — edit parsial**) — 2 baris ditambah ke pemanggilan
`SettingsScreen(...)` yang sudah ada (`songs = librarySongs`, `onDeleteSongs = { deleteSongsFromDevice(it) }`),
keduanya reuse variable/fungsi yang sudah ada, 0 fungsi baru ditulis di file ini.

**Gradle Wrapper (gap #3) SENGAJA DILEWATI batch ini, bukan lupa**: `gradlew`/`gradlew.bat` teks
scriptnya bisa ditulis manual, tapi `gradle/wrapper/gradle-wrapper.jar` adalah file JAR biner
(bukan teks) yang resminya di-generate oleh `gradle wrapper` task atau didownload dari
`services.gradle.org` — lingkungan kerja batch ini tidak punya `gradle` terpasang maupun akses
network (dicek eksplisit, `curl` ke `raw.githubusercontent.com` ditolak proxy egress). Menulis
wrapper TANPA jar asli (mis. taruh placeholder/file kosong) akan membuat `./gradlew` gagal total
dengan error yang membingungkan — lebih aman terang-terangan skip daripada kasih wrapper rusak.
**Prioritas kalau user sendiri punya akses**: jalankan `gradle wrapper --gradle-version 8.7` sekali
di project (device Termux mana pun yang sudah punya `gradle` dari `setup-gradle` cache atau
install manual), commit hasil `gradlew`/`gradlew.bat`/`gradle/wrapper/*` sekali — setelahnya CI
BISA disederhanakan balik ke `./gradlew` biasa (saat ini pakai `gradle` binary dari
`setup-gradle@v3` sebagai workaround, lihat komentar Batch 62/76 di `.github/workflows/build.yml`
baris ~224-226).

**Belum diverifikasi compile/runtime Gradle sungguhan** — prioritas berikutnya kalau user push:
(1) `./gradlew`/`gradle testDebugUnitTest assembleRelease` build bersih, (2) buka Setelan →
"Deteksi File Duplikat" dengan library yang punya duplikat sungguhan (copy 1 lagu ke 2 folder
untuk uji "Duplikat File Fisik", atau tag ulang 1 file supaya title/artist sama tapi durasi mirip
untuk uji "Duplikat Entri Library"), pastikan checkbox & tombol hapus jalan, dan pastikan delete
via `MediaStore.createDeleteRequest` (Android 11+) tetap munculkan dialog sistem seperti biasa.
Detail lengkap: `CHANGELOG.md` Batch 117.

**Batch 116 (Gap List #11 — Genre metadata first-class, 8 file kode + 1 dokumentasi)** — Item
kedua daftar "Sangat disarankan" (lanjut Batch 115). Genre di-skip sejak Batch 89 dengan alasan
"N+1 query per lagu" — dicek ulang, alasan itu cuma berlaku untuk pendekatan naif (query per
lagu); dibalik jadi 1 map id→nama dibangun SEKALI per scan dari sisi `MediaStore.Audio.Genres`
(dibatasi jumlah genre di device, bukan jumlah lagu) menghilangkan biayanya sama sekali.
`MusicRepository.kt`'s `buildGenreMap()` baru (query `Genres` lalu `Genres.Members` per genre,
bukan per lagu) dipanggil sekali di awal `querySongs()`, lookup O(1) per baris cursor.
`CustomFolderScanner.kt` baca `METADATA_KEY_GENRE` dari retriever yang sudah terbuka (zero I/O
tambahan, pola sama albumArtist/composer Batch 105). `Song.kt` dapat field `genre: String?`
(default null, posisi terakhir — 0 call site lama berubah). **Dicek dulu ke referensi resmi
sebelum nulis kode** (bukan ditebak dari ingatan) — tidak ada kolom genre polos di baris utama
`MediaStore.Audio.Media` lintas API yang ditarget app ini (beda dari track/disc/album-artist
yang semuanya kolom langsung), genre HANYA ada lewat tabel relasi `Genres`/`Genres.Members` —
pelajaran Batch 14/32/33/44 (jangan tebak API Android) diterapkan lagi di sini sebelum menulis
`buildGenreMap()`.

`LibrarySearchIndex.kt` — genre masuk `searchableText` (blob null-separated sama seperti title/
artist) — sisi "gunakan genre pada filtering/search" gap list. `SmartPlaylist.kt`/
`SmartPlaylistEngine.kt` — kriteria baru `genre: String?`, EXACT match case-insensitive (BUKAN
substring seperti `keyword` — semantiknya beda, nilai genre datang dari picker chip nilai asli
library, bukan teks bebas), lagu tanpa genre tidak pernah cocok rule genre-bounded (pola sama
`year == 0`). `SmartPlaylistScreen.kt`/`LibraryScreen.kt` — param `availableGenres` (persis
presenden `availableFolderNames`) → baris `FilterChip` tap-to-clear di builder sheet, tepat di
bawah chip folder. `README.md` — deskripsi Smart Playlist & catatan "belum selesai" genre lama
dihapus/diperbarui.

Brace/paren 8 file kode dicek otomatis & seimbang. 0 file baru (murni edit 8 file existing), 0
protected asset tersentuh, 0 file manifest berubah (tidak ada file baru → `FILE_MANIFEST.txt`
tidak perlu diedit). **Belum diverifikasi compile/runtime Gradle sungguhan** — prioritas
berikutnya kalau user push: `./gradlew testDebugUnitTest` (pastikan `SmartPlaylistEngineTest.kt`
existing tetap hijau dengan field baru default null), build APK asli + cek device (1) genre
genuinely terisi untuk lagu yang filenya punya tag genre (banyak file musik nyata TIDAK punya
tag genre sama sekali — kosong belum tentu bug, cek dulu file test-nya sendiri bertag atau
tidak), (2) `buildGenreMap()` tidak menambah lag terasa saat refresh library, (3) chip genre di
Playlist Otomatis builder exact-match benar (lagu genre lain tidak ikut lolos). Detail lengkap:
`CHANGELOG.md` Batch 116.

**Batch 115 (Gap List #10 — Backup/restore data lokal, 3 file — 2 baru + 1 diedit)** — Item
pertama dari daftar "Sangat disarankan" setelah 10 item "Wajib" P0/P1 (crossfade sampai database
consistency) tuntas di Batch 102-114. `BackupManager.kt` (baru, `data/`) — export 17 prefs
whitelist (playlist, playlist otomatis, favorit, rating, bookmark, mode audiobook, riwayat/
statistik dengar, folder/lagu disembunyikan, tema, & 6 pengaturan toggle) jadi 1 file JSON ke
`Documents/AudioPlayer/backups/` lewat MediaStore (pola identik `AppLogger`, FIFO retensi 20).
**Sengaja dikecualikan** (didokumentasikan di KDoc, bukan kelupaan): `app_lock` (PIN — data
keamanan), `custom_folders` (URI SAF terikat device asal, restore mentah = folder mati),
`onboarding_hints`/`search_history`/`sleep_timer` (nilai rendah/state transien). Tiap value
`SharedPreferences` (String/Int/Long/Float/Boolean/`Set<String>`) dibungkus tag tipe eksplisit
di JSON — round-trip export→import tidak diam-diam mengubah Int jadi Long. `readAndValidate()`
(parse+cek schemaVersion) dipisah dari `applyBackup()` (eksekusi) — UI wajib tampilkan ringkasan
jumlah item per kategori + user tap konfirmasi eksplisit sebelum data ditimpa (guard "jangan overwrite destruktif tanpa validasi"), restore per-prefs REPLACE penuh bukan merge.
`BackupRestoreSheet.kt` (baru, `ui/`) — tombol export + import (SAF `OpenDocument`, mime
`application/json`). **Launcher SAF dideklarasikan langsung di sheet ini** (bukan di-drilling ke
`MainActivity.kt`) — `rememberLauncherForActivityResult` cuma butuh `ActivityResultRegistryOwner`
dan itu tersedia di seluruh pohon Compose Activity termasuk di dalam `ModalBottomSheet`, jadi
**0 baris `MainActivity.kt` disentuh batch ini**. `SettingsScreen.kt` (diedit) — 1 row menu
baru "Cadangkan & Pulihkan" di level teratas (bukan submenu "Lanjutan" — ini fitur mainstream).
**Batasan jujur**: StateFlow yang sudah di-cache `PlayerViewModel` (favorit/playlist/dst.) TIDAK
otomatis re-read begitu `applyBackup()` menimpa SharedPreferences langsung — restore berhasil ke
disk, tapi UI yang sedang terbuka bisa tampil data lama sampai app ditutup-buka ulang (dialog
konfirmasi sudah bilang ini eksplisit ke user). Brace/paren 3 file dicek seimbang. **Belum
diverifikasi compile/runtime Gradle sungguhan** — prioritas berikutnya kalau user push: buat
backup, `pm clear`/uninstall-install ulang, pulihkan dari file, pastikan playlist/favorit/rating
benar-benar kembali setelah app dibuka ulang. Detail lengkap: `CHANGELOG.md` Batch 115.

**Batch 114 (Gap List #9 — Library/database consistency, 4 file diedit)** — Audit checklist #9:
app ini tidak pakai Room/SQL (murni MediaStore live-query + SharedPreferences/JSON stores), jadi
"duplicate song record" & "rescan idempotent" SUDAH aman by construction (`getAllSongs()` selalu
query fresh, dedup SAF-vs-MediaStore via `dedupeSignature()` sudah dicek benar sejak Batch 106,
diverifikasi ulang — 0 perubahan di situ). Gap nyata: "bersihkan item yang sudah dihapus" +
"playlist/favorite tidak menunjuk entity yang sudah hilang" — belum ada mekanisme apa pun,
favorit/rating/playlist-entry untuk file yang dihapus/dipindah numpuk selamanya di storage,
tidak pernah dibersihkan. **`FavoritesStore.kt`/`RatingStore.kt`/`PlaylistStore.kt`** masing-
masing dapat `pruneOrphans(validIds: Set<Long>)` (no-op write kalau tidak ada yang stale).
Dipanggil dari **`PlayerViewModel.kt`**'s `refreshLibrary()`, tepat setelah `_librarySongs.value`
diisi hasil scan terbaru. **Sengaja TIDAK diterapkan** ke `listeningHistoryStore`/`playStatsStore`
— itu catatan historis ("pernah diputar tanggal X"), bukan pointer state-saat-ini, dangling ID di
situ wajar & aman (replay lagu yang sudah hilang cukup ditangani pesan error Batch 113, bukan
dihapus riwayatnya). Playlist yang jadi kosong akibat prune TETAP dipertahankan sebagai playlist
(bukan ikut terhapus) — nama yang user pilih sendiri. Brace/paren 4 file dicek seimbang. **Belum
diverifikasi compile/runtime Gradle sungguhan**. Detail lengkap: `CHANGELOG.md` Batch 114.

**Batch 113 (Gap List #8 — Playback error recovery, 1 file diedit)** — Audit
`onPlayerError` (`PlayerViewModel.kt`) vs checklist #8: sebelumnya 1 pesan generik untuk semua
jenis error ("file mungkin dihapus atau rusak") + auto-skip tanpa batas kalau `hasNextMediaItem()`
— risiko nyata: kalau SISA queue rusak semua (folder sumber dicabut total), auto-skip mental dari
error ke error tanpa henti (silent infinite loop, Snackbar spam). 2 gap utama ditutup: (1)
`describePlaybackErrorReason()` baru — map `PlaybackException.errorCode` ke 4 kategori (file
hilang/izin ditolak/format tidak didukung/rusak-malformed) + fallback generik, dipakai baik di
pesan user maupun log diagnostics. (2) `consecutiveErrorCount` + `MAX_CONSECUTIVE_PLAYBACK_ERRORS`
(5) — auto-skip cuma jalan di bawah ambang ini; kalau tercapai, `pause()` + 1 pesan jelas
("beberapa lagu berturut-turut gagal..."), BUKAN terus mental. Counter direset di
`onIsPlayingChanged(true)` (sinyal paling jujur playback beneran pulih, bukan cuma pindah index
yang berujung error lagi). Brace/paren dicek seimbang (196/196, 722/722). Item gap list #8 yang
BELUM disentuh (di luar scope batch ini, sengaja tidak digabung): retry logic per-error-type,
UI state error per-song di Library/Queue (saat ini murni Snackbar sekali tampil). **Belum
diverifikasi compile/runtime Gradle sungguhan**. Detail lengkap: `CHANGELOG.md` Batch 113.

**Batch 112 (Fix baris tombol transport Now Playing ke-clip/hilang — root cause TERPISAH dari
Batch 110/111, 1 file diedit)** — User lapor (screenshot): baris tombol shuffle/prev/play/next/
repeat di Now Playing masih "deformasi" SETELAH Batch 111. Ternyata bukan kasus yang sama:
`NowPlayingScreen` render DI DALAM `Scaffold`/`AppNavHost` (bukan di luar seperti 3 screen Batch
111), jadi sudah dapat `contentWindowInsets` via `padding` di `AppNavHost` — insets BUKAN
masalahnya di sini. Root cause asli: root `Column` layar ini `fillMaxSize()` TANPA scroll, isinya
fixed-height (hero art 300dp + hint banner ~150dp saat tampil + title/rating/waveform/time/tombol)
— total tinggi konten gampang melebihi viewport asli terutama saat 3-button nav (Android 15 ke
bawah, makan tinggi layar riil) dibanding gesture-nav (Android 16 test device, overlay tipis) —
match observasi "normal di 16, kacau di 15 ke bawah" yang sama persis, TAPI mekanisme beda dari
Batch 110/111. Konten overflow sebelumnya di-clip diam-diam di tepi layar, baris tombol (paling
bawah urutan Column) paling sering jadi korban — persis yang di screenshot.
Fix: `NowPlayingScreen.kt` (edit parsial) — root `Column` dapat `.verticalScroll(rememberScrollState())`
(2 import baru: `androidx.compose.foundation.rememberScrollState`, `.verticalScroll`). Kalau konten
muat (layar tinggi/gesture-nav), scroll offset tetap 0, nol perubahan visual dari sebelumnya; kalau
tidak muat, sekarang bisa digeser bukan ke-clip hilang. Gesture drag vertikal untuk
brightness/volume (2 `Box` swipe-zone di dalam hero art 300dp) TETAP aman — masing-masing sudah
pakai `change.consume()` di `detectVerticalDragGestures`-nya sejak sebelum batch ini, pola standar
yang mencegah `verticalScroll` ancestor ikut menangkap drag yang sama; swipe next/prev (horizontal,
`AlbumArtHero`) juga tidak terpengaruh (beda axis). Title marquee (`basicMarquee()`) SEKALI LAGI
tidak disentuh sama sekali — dikonfirmasi ulang bukan bug. Brace/paren dicek seimbang (199/199,
673/673). **Belum diverifikasi compile/runtime Gradle sungguhan** — prioritas berikutnya kalau
user push: buka Now Playing di device Android 15 3-button-nav dengan hint banner masih tampil
(kondisi termudah memicu overflow), pastikan baris tombol transport tetap terjangkau (via scroll
kalau perlu) dan swipe brightness/volume/next/prev masih responsif seperti biasa. Detail lengkap:
`CHANGELOG.md` Batch 112.

**Batch 111 (Fix deformasi layout UI Android 15 ke bawah — eksekusi scope Batch 110, 3 file
diedit)** — Root cause & diagnosis lengkap: lihat Batch 110 di bawah (tidak diulang di sini).
Fix: tambah `.windowInsetsPadding(WindowInsets.safeDrawing)` (sebelum `.padding(32.dp)` fixed
yang sudah ada, bukan pengganti) di 3 titik: `WelcomeScreen` (`MainActivity.kt`, private
composable), `PermissionRationale` (`MainActivity.kt`, private composable), `LockScreen`
(`LockScreen.kt`, root `Column`). Ketiganya render di luar `Scaffold` (`setContent` di
`MainActivity.onCreate`) sehingga sebelumnya nol proteksi insets. `MainActivity.kt` dapat 3
import baru (`WindowInsets`, `safeDrawing`, `windowInsetsPadding`); `LockScreen.kt` sudah pakai
wildcard `foundation.layout.*`, tidak perlu import baru. Manifest (protected, edit parsial 1
atribut): `<activity>` MainActivity dapat `android:windowLayoutInDisplayCutoutMode="shortEdges"`
— eksplisit dinyatakan (sebelumnya tidak dideklarasikan sama sekali), konsisten dengan
`enableEdgeToEdge()` yang sudah aktif. `compileSdk`/`targetSdk` 34 TIDAK dinaikkan di batch ini
(di luar scope yang disetujui, tetap gap tercatat terpisah). **Catatan eksplisit dari user**:
title/judul lagu yang bergerak sendiri (`basicMarquee()` di Now Playing) BUKAN bagian dari bug
deformasi ini — perilaku itu memang disengaja (marquee scroll teks panjang), tidak disentuh sama
sekali di batch ini. Brace/paren `MainActivity.kt` (245/245, 563/563) & `LockScreen.kt` (48/48,
128/128) dicek seimbang; manifest XML valid (`xmllint`). **Belum diverifikasi compile/runtime
Gradle sungguhan** (tidak ada JDK/Android SDK di sandbox) — prioritas berikutnya kalau user push:
build & install ke device Android 15 3-button-nav sungguhan, cek WelcomeScreen/PermissionRationale
saat first-launch dan LockScreen kalau App Lock aktif, pastikan konten tidak lagi ketiban status
bar/nav bar. Detail lengkap: `CHANGELOG.md` Batch 111.

**Batch 110 (Audit deformasi layout UI: normal di Android 16, kacau di Android 15 ke bawah — 2
file diedit, keduanya dokumentasi, 0 kode app diubah)** — Instruksi user eksplisit: dokumentasi
lengkap dulu sebelum eksekusi fix. Audit `grep` insets keyword ke SEMUA 20 file `ui/*.kt`: **0
hasil di semua file** — tidak ada satu pun screen yang handle `WindowInsets` manual. Root cause:
`enableEdgeToEdge()` aktif global (`MainActivity.kt:188`) tapi satu-satunya sumber insets-padding
di app ini adalah `contentWindowInsets` bawaan `Scaffold` di `AppNavHost` — sementara 3 screen
(`WelcomeScreen`, `PermissionRationale`, `LockScreen`, `MainActivity.kt:380-401`) render DI LUAR
`Scaffold` itu, genuinely nol proteksi status/nav bar. Ditambah: `windowLayoutInDisplayCutoutMode`
tidak dideklarasikan di manifest, dan `compileSdk`/`targetSdk` masih 34 (Android 15/16 API surface
belum resmi disasar — gap yang sudah sadar dicatat sejak Batch 99). `rememberAppWidthClass()`
sudah dicek eksplisit dan DIRULED OUT (murni `LocalConfiguration.screenWidthDp`, aman lintas API
23+, bukan sumber bug). Hipotesis kenapa OS16 tampak normal vs OS15 ke bawah kacau (confidence
sedang, bukan pasti tanpa device test): gesture-nav (lazim di device OS16 test) = overlay tipis,
overlap nyaris tak kelihatan; 3-button nav (masih umum di device OS15 ke bawah/budget) = bar
opaque tetap makan tinggi layar, overlap kelihatan nyata. Confidence diagnosis kode: 85%. **Scope
fix disiapkan untuk batch berikutnya (BELUM dieksekusi)**: insets padding di 3 screen di atas +
deklarasi `windowLayoutInDisplayCutoutMode` — estimasi 3 file, 0 protected asset inti tersentuh
(`MainActivity.kt` protected, tapi editnya akan parsial di 2 private composable saja). Detail
lengkap: `CHANGELOG.md` Batch 110.

**Batch 109 (Gap List #7 — Sleep timer process-resilient, 3 file — 1 baru + 2 diedit)** —
Sebelumnya sleep timer HANYA hidup sebagai `viewModelScope.launch` murni: kalau `PlayerViewModel`
di-clear (proses di-kill total selagi `PlaybackService` foreground diminta system tetap
hidup/di-restart lewat Playback Resumption), timer hilang diam-diam, lagu terus main tanpa batas
tanpa jejak apa pun.

1. **`SleepTimerStore.kt` (baru)** — SharedPreferences kecil, simpan 1 nilai: `endAt` ABSOLUT
   (epoch millis), bukan "sisa menit" — supaya sisa waktu bisa dihitung ulang benar dari
   `endAt - now()` di titik proses mana pun, tanpa perlu tahu berapa lama proses sempat mati.
2. **`PlaybackService.kt` (protected, edit parsial)** — eksekusi NYATA (pause sungguhan)
   dipindah ke sini, `serviceScope` (bukan ViewModel scope). Custom `SessionCommand` baru
   (`ACTION_SET_SLEEP_TIMER`, pola identik `ACTION_SET_SKIP_SILENCE`/`ACTION_SET_CROSSFADE_ENABLED`
   yang sudah ada) jadi jembatan ViewModel→Service. `scheduleSleepTimer()`/`cancelSleepTimer()`
   SELALU cancel job lama + tulis/hapus store BERSAMAAN (atomic — tidak pernah ada state job
   jalan tapi store kosong atau sebaliknya). `resumeSleepTimerFromStore()` dipanggil sekali di
   `onCreate` (setelah `mediaSession` terbentuk): kalau ada `endAt` tersimpan & belum lewat,
   lanjutkan delay dari SISA waktu yang benar (bukan mulai dari awal lagi); kalau sudah lewat
   selagi proses mati, tetap pause sekali (aksi tidak boleh hilang cuma karena telat) lalu
   bersihkan — mencegah pause ganda di restart berikutnya. `onTaskRemoved()` (antrean kosong →
   `stopSelf()`) sekalian `cancelSleepTimer()` — playback dihentikan total, timer jadi tidak
   berarti apa-apa kalau dibiarkan nyangkut.
3. **`PlayerViewModel.kt` (diedit)** — `setSleepTimer()`/`cancelSleepTimer()` sekarang MENGIRIM
   command ke Service (eksekusi asli di sana), coroutine ViewModel yang tersisa MURNI kosmetik
   (cuma angka countdown UI, dihitung ulang dari `endAt - now()` tiap tick — bukan decrement
   lokal — supaya tidak drift). `init {}` baru: baca `SleepTimerStore` sekali saat ViewModel
   dibuat, kalau ada timer aktif tersisa dari sebelum ViewModel ini ada, tampilan countdown
   langsung terisi lagi — TIDAK memengaruhi apakah timer benar-benar akan berbunyi (itu murni
   urusan Service), cuma soal UI tidak "lupa" ada timer jalan.

**Kenapa bukan `AlarmManager`**: sengaja tidak dipakai — Service ini sudah foreground selama
playback jalan (prasyarat arsitektur sejak migrasi `MediaLibraryService` Batch 12), jadi
coroutine di scope Service sudah cukup resilient untuk kasus yang benar-benar relevan (proses
mati SELAGI masih ada foreground service terkait). `AlarmManager` akan menambah kompleksitas
(exact-alarm permission API 31+, dll) untuk skenario yang sangat sempit (device reboot/force-stop
total di TENGAH sleep timer aktif) yang di luar cakupan realistis fitur ini.

Brace/paren 3 file dicek otomatis & seimbang setelah 1 kesalahan `str_replace` (docstring
`maybeStartFloatingBubble` sempat terpotong) ditemukan & diperbaiki sebelum repack. **Belum
diverifikasi compile/runtime Gradle sungguhan** (tidak ada JDK/Android SDK/kotlinc di sandbox
kerja) — prioritas berikutnya kalau user push: set sleep timer, force-stop app dari App Info
(mensimulasikan kill proses), tunggu lewat deadline, buka lagi app, pastikan (1) lagu genuinely
sudah ter-pause, (2) tidak ada crash log baru. Detail lengkap: `CHANGELOG.md` Batch 109.

**Batch 108 (Gap List #6 — Durable playback state: repeat/shuffle, 2 file)** — Audit
`PlaybackStateStore.kt`/`PlayerViewModel.kt` terhadap checklist #6: track/posisi/queue sudah
persist sejak lama (checkpoint tiap ~5s saat playing + immediate on pause, Batch-batch awal),
tapi **repeat mode & shuffle selalu reset ke off tiap resume** — gap nyata, belum pernah
ditangani. `SavedPlaybackState` dapat 2 field baru (`repeatMode`, `shuffleEnabled`), `save()`
menyimpannya dari `controller.repeatMode`/`controller.shuffleModeEnabled` di titik checkpoint
yang sama (zero I/O tambahan). `resumeFromSaved()` set repeat/shuffle ke controller SEBELUM
`playQueue()` (bukan sesudah) supaya shuffle order berlaku sejak media item pertama di-set,
bukan re-shuffle setelah queue sudah jalan. Ditambah `SCHEMA_VERSION` (const, belum dipakai utk
migrasi bertingkat — cuma dokumentasi kontrak) + `load()` dibungkus try/catch eksplisit: state
corrupt/incompatible jatuh ke `null` (dianggap tidak ada state, mulai kosong), bukan crash
resume — SharedPreferences typed getters sendiri sudah aman ClassCastException, ini jaring
pengaman tambahan untuk kasus masa depan. Volume (`userTargetVolume`) SENGAJA tidak dipersist —
diaudit, itu murni level fade internal crossfade (Batch 102), bukan preferensi user yang berarti
disimpan lintas sesi. Brace/paren 2 file dicek otomatis & seimbang. **Push pertama gagal (CI run 161, build)**: `e: Returns are not allowed for functions with
expression body` di `PlaybackStateStore.kt:46/48` — `load()` ditulis gaya `fun load(): T? =
try { ... }` (expression body) tapi isinya pakai early-return (`?: return null`), yang cuma sah
di block body. Diperbaiki: `fun load(): T? { return try { ... } catch { ... } }` — block body
eksplisit, `return` di dalam try/catch sah. **Pelajaran: `return` awal (early-return) di dalam
body tidak boleh dicampur dengan gaya singkat `fun x() = ...` (expression body) sependek apa
pun, meski tanpa early-return sah-sah saja — cek pola ini SEBELUM push tiap kali menulis
function baru bergaya ringkas.**

**Push kedua crash di device sungguhan (crash log `crash_20260817_111602`, Android 15, Infinix
X6850)** — `IllegalStateException: MediaController method is called from a wrong thread`, thread
`DefaultDispatcher-worker-2`. Root cause: fix Batch 108 sendiri (repeat/shuffle persistence)
salah taruh `c.repeatMode`/`c.shuffleModeEnabled` DI DALAM `viewModelScope.launch(Dispatchers.IO)`
— MediaController wajib diakses dari thread yang membuatnya (main), method apa pun yang dipanggil
dari thread lain melempar exception ini. `songIds`/`positionMs`/`currentMediaItemIndex` sudah
lama benar dibaca DI LUAR coroutine (di main thread) sebelum `launch`; 2 field baru Batch 108
tidak ikut pola yang sama. Fix: `repeatMode`/`shuffleEnabled` dibaca sebagai `val` di main thread
tepat sebelum `launch(Dispatchers.IO)`, lalu dikirim sebagai parameter biasa — pola identik
dengan field lama. **Pelajaran: SETIAP kali menambah field baru yang sumbernya `MediaController`
ke dalam blok yang sebagian jalan di background dispatcher, wajib baca nilainya DI LUAR blok
`launch` itu dulu — jangan asumsikan aman cuma karena field lain di file yang sama sudah benar,
tiap penambahan baru harus dicek pola threading-nya sendiri.** Ini crash nyata pertama proyek
ini yang ketahuan lewat crash logger (Batch 22) sejak logger itu ada — kena tiap ~5 detik selama
playback jalan (checkpoint interval), jadi dampaknya besar meski baru 1 device yang melaporkan.

Belum diverifikasi compile Gradle sungguhan setelah fix ini — prioritas berikutnya kalau user
push ulang: pastikan compile hijau, lalu matikan app dengan shuffle/repeat-one aktif, buka lagi,
pastikan keduanya genuinely kepulihkan (bukan cuma baca kode) DAN tidak ada crash log baru
selama playback berjalan lebih dari beberapa menit. Detail lengkap: `CHANGELOG.md` Batch 108.

**Batch 107 (Permintaan user langsung dari screenshot GitHub Releases — bersihkan tag & judul
rilis, 2 file, 1 protected)** — 2 hal: (1) hapus `-release` dari tag/nama file APK (sudah punya
`-run<N>` sendiri, "release" di tengah cuma noise, tidak nambah informasi keunikan apa pun);
(2) judul rilis yang tampil di daftar Releases repo (screenshot user: `v1.0.47-release-run159`,
gambar 2) dibuat minimalis — cuma nomor versi.

`.github/workflows/build.yml` (protected, edit parsial) — step "Determine version name" sekarang
punya 2 output terpisah, bukan 1: `tag` (`v$VERSION_NAME-run<run_number>`, WAJIB tetap unik per
run — invariant Batch 65, kalau tidak unik nama file APK bentrok lagi jadi "(1).apk" duplikat)
dan `release_name` baru (`v$VERSION_NAME` polos, tanpa run number — ini yang jadi judul di
daftar Releases). Step "Create GitHub Release" — `tag_name` tetap pakai `tag`, `name` sekarang
pakai `release_name` (dulu keduanya sama-sama pakai `tag`, itu sebabnya judul rilis ikut
menampilkan run number yang user rasa berantakan). Step "Rename APK" TIDAK disentuh — sudah
otomatis ikut berubah krn membaca `steps.version.outputs.tag` secara dinamis (jadi
`AudioPlayer-v1.0.47-run159.apk`, bukan lagi `-release-run159`).

**Tetap sinkron dengan APK** (syarat eksplisit user) — `tag` dan `release_name` SAMA-SAMA
diturunkan dari `$VERSION_NAME` yang dihitung SEKALI di baris yang sama (formula identik dengan
`gitCommitCount()` di `app/build.gradle.kts`, invariant Batch 30/56/86 tidak disentuh) — cuma 2
representasi beda dari angka yang sama (unik-untuk-tag vs minimalis-untuk-judul), bukan 2 sumber
angka independen yang bisa drift.

`README.md` § Standar Penomoran Versi — 2 contoh lama (`AudioPlayer-v1.5.17-release-run42.apk`)
diperbarui ke pola baru, + paragraf baru menjelaskan kenapa tag & judul rilis sekarang sengaja
beda representasi.

YAML divalidasi (`python3 -c "import yaml; yaml.safe_load(...)"`) — parse sukses, tidak ada
syntax error. **Belum diverifikasi CI run sungguhan** (tidak ada akses GitHub Actions di
environment kerja ini) — prioritas berikutnya kalau user push: pastikan 1 run penuh sukses,
tag baru `vX.Y.Z-runN` (tanpa "-release") kebentuk benar, DAN judul rilis di halaman Releases
repo genuinely tampil minimalis (`vX.Y.Z` polos) sesuai screenshot yang diminta user diperbaiki.
Detail lengkap: `CHANGELOG.md` Batch 107.

**Batch 106 (Gap List #5 — SAF parity, 4 file diedit)** — Lanjutan langsung Gap List (#4 Batch
105 selesai). Audit `CustomFolderScanner.kt`/`PlayerViewModel.kt` terhadap 8 sub-item checklist
#5: 2 gap nyata + 1 dokumentasi basi ditemukan & dibenarkan, sisanya (dedupe vs MediaStore,
refresh idempotent) ternyata SUDAH benar sejak lama (dicek eksplisit, bukan diasumsikan).

1. **"Tangani permission revoke" (gap nyata, belum pernah ditangani)** — izin SAF folder
   tambahan bisa dicabut dari LUAR app kapan saja (layar sistem semacam "Kelola akses file"),
   tanpa broadcast/callback apa pun ke app. Sebelumnya: `scan()` lempar `SecurityException`,
   ditangkap, dicatat ke log, lagu folder itu diam-diam hilang dari library SELAMANYA tanpa
   penjelasan ke user — persis kelas bug "kok folder saya kosong" yang Batch 16 sudah tutup
   untuk kegagalan izin AWAL, tapi belum untuk pencabutan BELAKANGAN. Fix: `CustomFolderInfo`
   dapat field baru `permissionGranted: Boolean`, dihitung ulang tiap kali (satu-satunya sumber
   valid: `ContentResolver.persistedUriPermissions`, dicek fresh — tidak bisa di-cache) lewat
   `PlayerViewModel.hasPersistedReadPermission()` baru. `loadCustomFolderInfos()` DAN
   `refreshLibrary()` (badge basi kalau cuma dihitung sekali saat add/remove) sama-sama panggil
   ini. `FolderManagerSheet.kt` — badge teks merah muncul di folder yang izinnya sudah dicabut,
   mengarahkan user hapus lalu pilih ulang (tombol hapus yang sudah ada dari Batch 26 sudah
   toleran ke `releasePersistableUriPermission` yang gagal karena izin memang sudah hilang,
   tidak perlu diubah).
2. **`refreshLibrary()` skip-scan folder yang sudah dikonfirmasi tidak berizin** — sebelumnya
   tiap refresh (content observer MediaStore fire cukup sering) selalu coba `scan()` ulang lalu
   gagal lagi dengan `SecurityException` yang sama, log spam tanpa henti untuk kondisi yang
   sudah diketahui. Sekarang cek `hasPersistedReadPermission()` DULU sebelum coba scan — kalau
   sudah dikonfirmasi tidak ada izin, skip diam-diam (bukan exception yang ditangkap, jadi bukan
   kegagalan I/O yang perlu dicatat tiap kali) alih-alih exception-catch-log berulang.
3. **`MAX_DEPTH` 6→20** — ditandai gap list "terlalu sempit". Struktur folder musik nyata
   (`Musik/Artis/Album/CD1/...`) bisa lebih dari 6 level lewat beberapa file manager/sync tool
   yang menambah 1 level nesting ekstra. 20 tetap jadi hard ceiling (bukan dihapus total) karena
   traversal ini rekursif — folder tree yang dibuat/korup sengaja nge-nest sangat dalam tetap
   punya batas aman.
4. **Komentar `albumId = -1L` diperbaiki, bukan cuma kosmetik** — komentar lama klaim lagu SAF
   "tidak ada artwork lookup, jatuh ke placeholder default". Ternyata SUDAH TIDAK BENAR sejak
   Batch 67-69: `AudioArtFetcher`/`SongArtBitmapLoader`/`AccentColorExtractor`/`WidgetUpdater`
   semua baca artwork generik lewat `song.uri` (bukan `albumId`) via `loadThumbnail()`/
   `MediaMetadataRetriever` — mekanisme yang sama persis bekerja untuk content URI dokumen SAF
   seperti untuk URI MediaStore. Jadi lagu SAF SUDAH dapat artwork tertanam asli di mana pun
   filenya punya itu; cuma file tanpa artwork tertanam sama sekali yang jatuh ke placeholder,
   identik dengan lagu MediaStore. Ditemukan lewat audit silang 4 file artwork sebelum menulis
   ulang komentar, bukan tebakan.

**Sub-item #5 yang diaudit & TERNYATA SUDAH BENAR (dicek eksplisit, bukan terlewat)**: "Pastikan
custom-folder scan tidak menduplikasi MediaStore entry" — `refreshLibrary()`'s
`dedupeSignature()`(title+artist+duration-bucketed) + `dedupedCustomSongs` sudah ada sejak lama,
prefer salinan MediaStore. "Pastikan refresh library aman/idempotent" — `libraryRefreshGeneration`
counter sudah cegah scan lama menimpa hasil scan baru. Kedua ini TIDAK disentuh batch ini.

**Sengaja BELUM digarap dari checklist #5** (dicatat, bukan terlewat): "Metadata extraction SAF
sedekat mungkin dengan MediaStore" — sudah sedekat yang aman tanpa pass kedua sejak Batch 105
(genre/bitrate/sampleRate/channelCount/codec sama-sama belum ada di KEDUA sumber, jadi sudah
paritas, bukan gap SAF-spesifik). Brace/paren 4 file dicek otomatis & seimbang. **Belum
diverifikasi compile/runtime Gradle sungguhan** (tidak ada JDK/Android SDK/kotlinc di sandbox
ini) — prioritas berikutnya: `./gradlew testDebugUnitTest` lalu build APK asli + cek di device
(1) badge "Izin dicabut" benar muncul setelah user cabut izin folder dari Settings sistem lalu
buka lagi Kelola Perpustakaan, (2) `MAX_DEPTH` 20 tidak berdampak terasa ke waktu scan folder
besar, (3) artwork SAF genuinely tampil untuk file yang punya embedded art (klaim komentar baru
di poin 4 belum pernah dilihat langsung, cuma diverifikasi lewat baca kode 4 file artwork).
Detail lengkap: `CHANGELOG.md` Batch 106.

**Batch 105 (Gap List #4 — Metadata model diperkuat, 4 file — 3 diedit + 1 baru)** — `Song.kt`
dapat 6 field baru (`albumArtist`, `composer`, `trackNumber`, `discNumber`, `fileSize`,
`mimeType`), semua nullable/default-0 di posisi terakhir constructor jadi 0 call site lama perlu
diubah. Diisi dari `MusicRepository.kt` (kolom MediaStore yang sudah ada di row yang sama, +
cabang API 30+/pre-30 utk track/disc) dan `CustomFolderScanner.kt` (extractMetadata tambahan di
pass retriever SAF yang sudah terbuka) — **zero I/O tambahan**, tidak ada query/pass kedua.
Field yang BUTUH pass kedua per file (bitrate/sampleRate/channelCount/codec/embedded-artwork)
sengaja belum — N+1 cost, alasan sama genre (Batch 89). `MusicRepositoryTrackDiscTest.kt` baru,
9 test parser murni. Belum diverifikasi compile. Detail lengkap: `CHANGELOG.md` Batch 105.

**Batch 104 (Konfirmasi CI Batch 103 HIJAU + Gap List #3/#5 — SAF song identity, 2 file)** — User
upload `instrumentation_test_report_156.zip`: 7 instrumentation test Batch 103 **SEMUA HIJAU**
di CI sungguhan (7/7 success, 0 fail) — pertama kalinya proyek ini punya bukti eksekusi runtime
asli, bukan cuma analisis statis. Lalu lanjut item gap list berikutnya: `CustomFolderScanner.kt`'s
`stableId()` (identitas lagu SAF) diganti dari `String.hashCode()` 32-bit (lemah, birthday-bound
collision realistis di library besar) ke FNV-1a 64-bit murni (ruang collision ~2^63) — fungsi
dipisah ke `Companion.stableId(String)` biar testable tanpa Robolectric (`CustomFolderScannerStableIdTest.kt`
baru, 4 test). Namespace MediaStore(non-negative)/SAF(negative) sudah eksplisit lewat sign bit,
tidak perlu tag tambahan. Queue restore (`PlayerViewModel.resumeFromSaved()`) diaudit — sudah
`mapNotNull` drop orphan + preserve order + auto-flush lewat `persistPlaybackState()` periodik,
tidak perlu diubah. **Belum diverifikasi compile sungguhan** (tidak ada kotlinc di sandbox) tapi
FNV-1a murni Kotlin stdlib, 0 API eksternal baru. Detail lengkap: `CHANGELOG.md` Batch 104.

**Batch 103 (Gap List #2 — Integration/device testing playback, 9 file — 5 baru + 2 diedit + 2
asset biner baru, 2 protected, Atomic Change)** — Item P0 kedua di `AudioPlayer_Coding_Gap_List.
md`. Proyek ini sebelumnya 0% instrumentation test (cuma `app/src/test`, pure-JVM) — lihat
komentar jujur yang sudah ada di `app/build.gradle.kts` sebelum batch ini ("no
Robolectric/instrumentation... cheap enough to actually get written").

1. **`app/src/androidTest/` (baru)** — `PlaybackServiceTestHelper.kt`: sambungkan
`MediaController` sungguhan ke `PlaybackService` sungguhan (bukan fake/mock), lewat
`runOnMainSync` (wajib — `MediaController.Builder` butuh thread berlooper) + listener+latch
(bukan blocking `.get()` di main thread — itu deadlock, main thread yg justru harus proses
handshake koneksinya sendiri). `PlaybackTransportTest.kt`: 7 test — play/pause, seek, next,
previous, repeat-off/all/one (repeat-one betul2 nunggu lewat durasi asli track, bukan cuma cek
setter), shuffle toggle. Queue test pakai 2 file WAV SINTETIS (`test_tone_a.wav` 440Hz/3s,
`test_tone_b.wav` 660Hz/2s, dibuat lewat `wave` stdlib Python, nol isu hak cipta, nol network) —
disalin dari asset test APK ke `cacheDir` app lalu diputar via `file://` (BUKAN `asset:///`, krn
`asset:///` resolve ke Context ExoPlayer yg sedang jalan sungguhan di `PlaybackService` — itu
Context APP, bukan Context test APK, jadi tidak akan pernah ketemu file di
`androidTest/assets/`).

2. **`app/build.gradle.kts` (protected, edit parsial)** — `testInstrumentationRunner` baru
(sebelumnya tidak ada sama sekali) + 3 `androidTestImplementation` (`androidx.test.ext:junit`,
`androidx.test:runner`, `androidx.test:core`). `Futures`/`MoreExecutors`/`ListenableFuture` dari
`com.google.common.util.concurrent` TIDAK perlu dependency baru — sudah terbukti kompail lewat
`PlaybackService.kt` yang sudah ada, androidTest source set otomatis warisi classpath `main`
(perilaku AGP standar utk androidTest 1-modul, bukan modul terpisah).

3. **`.github/workflows/build.yml` (protected)** — job baru `instrumentation-tests`, SENGAJA
paralel/independen (tanpa `needs:` ke job `build`) — emulator flaky/lambat tidak pernah
menghalangi publish GitHub Release. `reactivecircus/android-emulator-runner@v2`, API 30 (bukan
35/36 — `compileSdk`/`targetSdk` app ini sendiri masih 34, menyasar API di atas itu tidak
mengetes apa pun yg app-nya belum menyasar), pakai `gradle` bukan `./gradlew` (proyek ini belum
punya Gradle Wrapper — gap list item #19, batch terpisah).

4. **`MANUAL_QA_CHECKLIST.md` (baru, root)** — item yang JUJUR tidak bisa diotomasi berarti lewat
emulator CI standar: audio focus (panggilan telepon/duck), Bluetooth (media output
switch/tombol fisik), lock-screen & notification controls, headset kabel, process death di
device fisik, background playback jangka panjang, Android 15/16-spesifik (ditandai eksplisit
"belum bisa diuji berarti" krn app belum menyasar SDK itu).

**Sengaja BELUM digarap** (dicatat, bukan terlewat):
- Audio focus/Bluetooth/lock-screen/notification/headset fisik — lihat `MANUAL_QA_CHECKLIST.md`,
alasannya di situ.
- Android 15/16 behavior testing — butuh naikkan `targetSdk` dulu (protected, berisiko tinggi
tersendiri), batch terpisah.
- CI job baru menambah runner-minutes tiap push (trade-off disadari, dicatat di
`CHANGELOG.md` — bisa diubah ke `workflow_dispatch` manual kalau terasa berat).
- Belum pernah benar-benar dijalankan (tidak ada akses emulator/device di sesi kerja ini) —
confidence berdasar pola resmi Media3/androidx.test yang sudah lama stabil + `Futures`/
`MoreExecutors` yang sudah terbukti kompail di file lain proyek ini, BUKAN dari eksekusi CI
aktual. Titik paling mungkin gagal pertama kali: `reactivecircus/android-emulator-runner`
konfigurasi KVM di runner GitHub yg bisa berubah kebijakannya, atau `gradle connectedDebug
AndroidTest` butuh task name persis sesuai `applicationId`/variant proyek ini.

Detail lengkap: `CHANGELOG.md` Batch 103.

**Batch 102 (Gap List #1 — True Crossfade, 4 file — 1 baru + 3 diedit, 1 protected)** — Dari
`AudioPlayer_Coding_Gap_List.md` yang user upload, item P0 pertama di daftar prioritas. "Fade
Halus" sebelumnya BUKAN crossfade sungguhan — cuma satu ExoPlayer yang volume-nya dilandaikan
turun lalu naik di sekitar titik ganti lagu (jeda senyap tetap ada, cuma disamarkan). Batch ini
ganti jadi overlap dua sumber suara sungguhan.

1. **`playback/CrossfadeEngine.kt` (baru)** — mesin dual-ExoPlayer. `sessionPlayer` (yang sudah
ada, dipegang `MediaSession`) TIDAK PERNAH diganti/di-swap — sengaja dihindari, sudah dicek lewat
web search: `MediaSession.setPlayer()` hot-swap dilaporkan bisa bikin session-nya berhenti total
(GitHub `androidx/media#764`), dan alternatif resminya (`ForwardingSimpleBasePlayer`) baru ada
dari media3 1.4.0 — proyek ini pin di 1.3.1 (lihat alasan di `PlaybackService.kt`, sudah 2x kena
insiden dari bump versi yang dipaksakan tanpa compiler: Batch 23/24, Batch 29). Sebagai
gantinya: `overlapPlayer`, ExoPlayer KEDUA yang privat (tidak pernah disentuh session/notifikasi/
UI), cuma pegang SATU MediaItem berikutnya, mulai main ~3 detik sebelum `sessionPlayer` habis,
lalu volume di-ramp bersilangan (sessionPlayer turun, overlapPlayer naik) — tumpang tindih
sungguhan di output audio. `sessionPlayer` DIBIARKAN mencapai transisi otomatisnya sendiri
(queue/shuffle/repeat-nya sama sekali tidak disentuh/di-reimplement — nol risiko baru di area
itu); begitu itu terjadi dia sudah senyap (volume ~0), jadi aman diseek diam-diam ke posisi
`overlapPlayer` (seek yang tidak terdengar krn volumenya nol) lalu bertukar kendali balik lewat
ramp singkat 400ms — sync posisi persis, jadi ramp balik ini tidak menghasilkan gema.
Skip/seek manual (tombol, notifikasi, headset, lock screen) mem-batalkan crossfade yang sedang
jalan lewat `onPositionDiscontinuity(reason=SEEK)` (dibedakan dari seek internal milik engine ini
sendiri via flag `internalSeekInFlight`); pause manual ikut membekukan `overlapPlayer` lewat
`onIsPlayingChanged`. Repeat-one sengaja di-skip total (next item = diri sendiri = bukan
crossfade yang masuk akal). `onPlayerError` di `overlapPlayer` fail-safe ke "batal crossfade kali
ini", tidak pernah macet di volume rendah.

2. **`PlaybackService.kt` (protected, edit parsial)** — bikin `overlapPlayer`
(`handleAudioFocus=false`, `setHandleAudioBecomingNoisy(false)`, cuma `sessionPlayer` yang boleh
urus fokus audio), custom `SessionCommand` baru `ACTION_SET_CROSSFADE_ENABLED` (pola identik
`ACTION_SET_SKIP_SILENCE`), hook 3 listener (`onMediaItemTransition` reason AUTO,
`onPositionDiscontinuity`, `onIsPlayingChanged`) ke `CrossfadeEngine`, loop polling 250ms baru,
release `overlapPlayer` eksplisit di `onDestroy` (tidak ikut kebawa `mediaSession.player.
release()`).

3. **`PlayerViewModel.kt`** — `startFadeIn()`/`startFadeOut()`/`animateVolume()`/
`fadedOutForIndex`/`FADE_DURATION_MS`/`FADE_FLOOR` dihapus total (pindah ke `CrossfadeEngine`).
`setCrossfadeEnabled()` sekarang relay lewat custom command persis pola
`setSilenceSkipEnabled()` yang sudah ada, karena ExoPlayer mentah tidak diekspos lewat
`MediaController`. `crossfadeEnabled: StateFlow<Boolean>` + `setCrossfadeEnabled()` — API publik
ke UI TIDAK berubah signature-nya, jadi `NowPlayingScreen.kt` cuma perlu update teks subtitle
toggle (`ui/NowPlayingScreen.kt`, bukan file "protected" tapi disebut krn ikut diedit), tidak ada
perubahan logic di sana.

**Batasan yang disadari, sengaja BELUM dibereskan** (dicatat, bukan terlewat):
- Equalizer/Visualizer terikat ke `PlaybackAudioSession.sessionId` (audio session id
`sessionPlayer`) — `overlapPlayer` punya audio session id sendiri (ExoPlayer/AudioTrack
terpisah), jadi EQ/visualizer belum ikut memengaruhi ~3 detik overlap suara lagu yang baru masuk.
Sempit dampaknya (fitur opt-in), belum jadi prioritas.
- Slider volume yang digeser TEPAT saat crossfade sedang ramp bisa terasa "menyusul" sesaat —
ramp engine ini overwrite `sessionPlayer.volume` tiap tick sampai selesai (<3 detik). Transient,
bukan bug fungsional.
- Belum di-build fisik (tidak ada akses compiler/Gradle di sesi kerja ini) — confidence
"seharusnya benar" berdasar API Media3 yang stabil lintas versi (`Player.Listener`,
`ExoPlayer.Builder`, `seekTo`/`setVolume`/`clearMediaItems`), BUKAN dari hasil compile aktual.
Prioritas verifikasi pertama kali dicoba: dengarkan baik-baik momen pergantian lagu dgn "Fade
Halus" ON — kalau ada gema/dobel suara sepersekian detik di titik handback, cek dulu
`CrossfadeEngine.onSessionAutoTransition()`.

Detail lengkap: `CHANGELOG.md` Batch 102.

**Batch 101 (Adaptive layout multi-device + undo hapus playlist, 5 file — 1 baru + 4 diedit, 1
protected)** — Instruksi user: audit UX/frontend (dijawab di chat, bukan kode), lalu gabung
semua perbaikan KECUALI TalkBack/Tema/Lokalisasi jadi 1 batch, utamakan adaptive layout.

1. **Adaptive layout (prioritas)**: `ui/adaptive/WindowAdaptive.kt` baru —
`rememberAppWidthClass()` (COMPACT<600dp/MEDIUM<840dp/EXPANDED>=840dp, breakpoint identik
rekomendasi resmi M3, dihitung dari `LocalConfiguration.screenWidthDp` — SENGAJA tidak nambah
dependency `material3-window-size-class` di `build.gradle.kts`). `MainActivity.kt`'s
`AppNavHost` (protected, edit parsial): `NavigationRail` gantikan `NavigationBar` bawah di
Medium/Expanded (Compact 0 berubah); `NowPlayingScreen(...)` diekstrak jadi lambda
`nowPlayingContent(onBack)` dipakai di route `now_playing` normal DAN panel two-pane 420dp
persisten kanan yang muncul di Expanded selama ada lagu aktif (`showTwoPane`).
`MiniPlayerBar`/`NavigationBar` auto-hide saat panel tampil, cegah kontrol dobel.

2. **Undo hapus playlist**: `deletePlaylist()`/`deleteSmartPlaylist()` (`PlayerViewModel.kt`)
sebelumnya PERMANEN 1 tap tanpa undo (beda dari `removeFromQueue` yang sudah pakai pola
`UndoableAction`) — sekarang snapshot dulu + `UndoableAction`, dgn `PlaylistStore.
restorePlaylist()`/`SmartPlaylistStore.restoreSmartPlaylist()` baru (simpan balik objek APA
ADANYA, bukan lewat `create*()` yg generate id baru).

**Sengaja TIDAK digarap** dari audit awal setelah dicek lebih dalam kodenya, bukan gap nyata:
loading state Playlist/SmartPlaylist (baca SharedPreferences sinkron, bukan async), predictive
back (manifest `enableOnBackInvokedCallback` sudah ada sebelum batch ini + `ModalBottomSheet`
M3 sudah tangani standar). Dua-pane Library/Playlist→detail juga belum digarap (di luar scope,
NowPlaying diprioritaskan krn paling sering dibuka). Belum di-build fisik. Detail lengkap:
`CHANGELOG.md` Batch 101.

**Batch 100 (Floating Mini Player: minimize ke tepi, auto-trigger tanpa buka app, QS Tile, 7
file — 4 baru + 3 diedit, 2 protected)** — Lanjutan 3 instruksi user yang sebelumnya cuma
tertangani sebagian (Batch 98, sesi lain, cuma menuntaskan foreground service + SALAH BACA
"minimize" sebagai "close/dismiss" lalu menolaknya — dikoreksi di sini).

1. **Minimize ke tepi**: `FloatingBubbleService.kt`'s `bubbleView` sekarang `FrameLayout` 2
child (pill penuh + `bubble_minimized.xml` baru, tab 48dp) — toggle visibility via `minimize()`
/`expand()`, Service/notifikasi TIDAK pernah berhenti. `snapMinimizedToNearestEdge()`:
chat-head-style, X selalu dipaksa ke tepi 0/`screenWidth-lebarTab` terdekat — dipanggil saat
minimize, lepas-drag ketika minimized, DAN rotasi layar. Tombol baru `bubble_minimize` (ikon
`ic_bubble_minimize.xml`). `FloatingBubbleStore.kt`: `isMinimized()`/`setMinimized()` baru.

2. **Auto-trigger tanpa buka app**: `PlaybackService.kt`'s `onIsPlayingChanged(true)` — SATU
titik yang selalu jalan dari entry point apa pun (widget/notifikasi/headset) — panggil
`maybeStartFloatingBubble()` baru, cek `isEnabled()` + `canDrawOverlays()` ulang tiap kali
(sama pola `BubbleBootReceiver`).

3. **Quick Settings Tile**: `BubbleTileService.kt` baru (`@RequiresApi(N)`, QS Tile custom
baru ada API 24, 1 di atas `minSdk` 23 — class ini tidak pernah diinstansiasi sistem di device
API 23), baca/tulis LANGSUNG ke `FloatingBubbleStore` (bukan lewat ViewModel StateFlow — System
UI bisa instansiasi tanpa `MainActivity` pernah hidup). Ikon `ic_bubble_tile.xml`. Manifest:
`<service exported="true" permission="...BIND_QUICK_SETTINGS_TILE">` + intent-filter `QS_TILE`.

4. **Sinkronisasi**: `PlayerViewModel.refreshFloatingBubbleEnabled()` + `MainActivity.kt`
(protected, edit parsial) `DisposableEffect` + `LifecycleEventObserver` MANUAL (bukan
`LifecycleEventEffect` — pola sengaja dipilih menghindari titik gagal historis `Local
LifecycleOwner`, lihat Batch 23-24) di `ON_RESUME`, supaya switch Settings tidak basi kalau
bubble ditoggle dari tile saat app masih hidup di background.

Belum di-build fisik. Detail lengkap: `CHANGELOG.md` Batch 100.

**Batch 99 (Audit kompatibilitas mundur Android 14 ke bawah, 0 file kode diubah, 2 file
dokumentasi)** — Instruksi user: "terapkan backward compatibility support untuk Android 14
kebawah". Audit 28 titik `Build.VERSION.SDK_INT`/`VERSION_CODES` di 10 file, fokus khusus kode
`specialUse` foreground service Batch 98 (fitur Android 14/API 34-only, paling berisiko).

**Hasil: 0 bug, 0 file diubah** — semua titik sudah benar dibungkus `if (SDK_INT >= level_yang_
tepat)` dengan fallback API lama yang valid. Kunci: `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`
(constant API 34) di-inline compiler jadi integer literal, jalur pemanggilannya sendiri sudah
digate `>= UPSIDE_DOWN_CAKE` jadi tidak pernah tereksekusi di device <34. `<service
foregroundServiceType="specialUse">` + `<property>` di manifest adalah atribut biner statis
di-resolve AAPT2 SAAT BUILD (compileSdk 34), bukan divalidasi ulang terhadap versi OS device
saat parsing runtime — OS lama baca int itu tanpa peduli namanya, tidak crash di device manapun
≥ minSdk 23. 1 titik redundan (bukan bug) ditemukan di `BubbleBootReceiver` (cek `SDK_INT>=M`
yang selalu true karena minSdk sudah 23=M) — dibiarkan, cuma gaya penulisan bukan risiko.

**Kesimpulan**: proyek sudah backward-compatible penuh ke `minSdk 23` termasuk fitur Android
14-only terbaru. Detail lengkap per-titik: `CHANGELOG.md` Batch 99.

**Batch 98 (Sempurnakan Floating Mini Player/Bubble — reliabilitas & completeness, 4 file —
1 baru + 3 diedit, 1 protected)** — Lanjutan instruksi user "sempurnakan 100% fungsionalitas".
Batch 97 (sesi sebelumnya) sengaja cuma fix 1 bug jank, menyisakan 3 celah completeness yang
sudah dicatat sejak Batch 95 sendiri ("batasan jujur") — batch ini menutupnya.

**1. Foreground service beneran**: sebelumnya cuma mengandalkan window overlay tampil utk
importance "mendekati visible", skin agresif tetap bisa membunuh kapan saja. `startForeground()`
dipanggil di `onCreate()` (tipe `specialUse` API 34+, `FOREGROUND_SERVICE_SPECIAL_USE` permission
+ `<property>` baru di manifest). Trade-off disadari & dicatat jujur: 1 notifikasi importance
MIN ekstra selama bubble aktif (nyaris tak kelihatan — MIN sembunyi dari status bar).

**2. Auto-restart setelah reboot**: `BubbleBootReceiver.kt` (baru, `bubble/`) dengar
`BOOT_COMPLETED`, cek `FloatingBubbleStore.isEnabled()` DAN `Settings.canDrawOverlays()` (izin
bisa dicabut dari luar app kapan saja) sebelum restart — sebelumnya cuma restart saat app
dibuka manual.

**3. State antrean kosong**: `hasQueue` baru (`player.mediaItemCount > 0`) — kosong = 3 tombol
alpha 0.4 + tap buka app (bukan no-op senyap). Default optimistic `true` sebelum controller
konek, supaya fallback Intent lama tetap jalan di tap paling awal.

**4. Rotasi layar**: `layoutParams` dipromosikan local var → field class, `onConfigurationChanged()`
baru re-clamp posisi ke `DisplayMetrics` terkini + `updateViewLayout()`/`savePosition()` kalau
berubah. `DisplayMetrics` di `setupDrag()`'s `ACTION_MOVE` juga dibaca ulang tiap event (bukan
cache basi).

`MainActivity.kt` (protected, edit parsial) — 3 titik start service disatukan ke helper
`startBubbleService()` API-gated (`startForegroundService()` WAJIB sekarang di O+, sebelumnya
`startService()` polos "kebetulan jalan").

**Sengaja TIDAK ditambah**: tombol dismiss di bubble (drag-to-dismiss ditolak — risiko UX
dismiss diam-diam tanpa konfirmasi). "Belum diverifikasi device fisik" masih berlaku sama.
Detail lengkap: `CHANGELOG.md` Batch 98.

**Batch 97 (Sempurnakan Floating Mini Player/Bubble — fix bug jank main-thread, 1 file)** —
Instruksi user: "sempurnakan 100% fungsionalitas dari Floating Mini Player (Bubble Mode)".
Audit `FloatingBubbleService.kt` (Batch 95) nemu 1 bug nyata: `refreshBubbleContent()` decode
artwork (`loadThumbnail()`/`MediaMetadataRetriever`, keduanya I/O blocking) SINKRON di
`Player.Listener.onEvents()` (main thread) — root cause class SAMA PERSIS widget jank Batch
34/35, tapi lebih parah di sini krn bubble ini overlay window di ATAS app lain apa pun, jank-nya
berisiko nyeret UI thread app yang lagi dibuka user, bukan cuma AudioPlayer sendiri.

Fix: `bubbleScope` (`CoroutineScope(Dispatchers.Main + Job())`, pola sama `serviceScope`
`PlaybackService.kt`) + `bubbleArtJob` (pola sama `widgetUpdateJob`) — `cancel()` sebelum tiap
relaunch (skip/next cepat tidak lagi berisiko art lagu lama menimpa lagu baru), decode pindah
`withContext(Dispatchers.IO)`, update `ImageView` balik main thread. Icon play/pause (murni
`setImageResource`, 0 I/O) sengaja TETAP sync. `bubbleScope.cancel()` ditambah `onDestroy()` +
re-`findViewById` dari `bubbleView` terbaru (bukan closure lama) sebelum update UI, jaga-jaga
Service di-kill selagi decode masih jalan.

**Batch 96 (Fitur baru: Trim Keheningan Otomatis/Silence Skip, roadmap #8, 5 file — 1 baru + 3
kode diedit + 1 protected)** — Toggle baru "Lewati Keheningan Otomatis" di Settings.

**Temuan kunci**: Media3/ExoPlayer 1.3.1 sudah punya `ExoPlayer.setSkipSilenceEnabled(Boolean)`
bawaan — draf roadmap awal mengira perlu analisis PCM manual, ternyata TIDAK, 0 kode amplitude
custom ditulis. Method ini milik `ExoPlayer` spesifik (bukan interface `Player` umum), jadi
`MediaController` (dipegang `PlayerViewModel`) tidak bisa panggil langsung — dijembatani 1
custom `SessionCommand` baru (`PlaybackService.ACTION_SET_SKIP_SILENCE`), diadvertise di
`onConnect()`, ditangani di `onCustomCommand()` baru yang cast ke `ExoPlayer` lalu panggil
method-nya. 2 jalur baca saling melengkapi: `PlaybackService.onCreate()` baca
`SilenceSkipStore` langsung utk proses baru, `PlayerViewModel.setSilenceSkipEnabled()` kirim
command LIVE + simpan ke store yang sama utk Service yang sudah jalan.

`SilenceSkipStore.kt` (baru, `data/`, pola identik `ShakeSettingsStore`) — OFF by default
(sesuai risiko roadmap: threshold bawaan bisa memotong intro/outro musikal). **Belum ada
slider sensitivitas/threshold custom** — pakai default ExoPlayer apa adanya, disebutkan jujur
di teks Settings, dicatat sebagai batasan disengaja bukan bug (konsisten pola "Catatan jujur"
proyek, lihat README § Gapless Playback untuk pola serupa). `MainActivity.kt` (protected, edit
parsial) — collect state + wiring `SettingsScreen`. Detail lengkap: `CHANGELOG.md` Batch 96.

**Batch 95 (Fitur baru: Floating Mini Player/Bubble, roadmap #11, Atomic Change 11 file — 3
baru + 4 kode diedit + 4 dokumentasi)** — Mini player mengambang di atas app lain mana pun
(play/pause/prev/next), butuh izin sensitif `SYSTEM_ALERT_WINDOW`, opt-in via toggle baru di
Settings (off by default).

`FloatingBubbleService.kt` (baru, `bubble/`) — plain Android View lewat `WindowManager`
(BUKAN Compose — ComposeView di luar Activity butuh LifecycleOwner/SavedStateRegistryOwner
rakitan manual, kompleksitas tidak sepadan utk pil 3-tombol). `bubble_mini_player.xml` (layout
baru) reuse drawable widget APA ADANYA (`widget_background.xml`, `widget_play_button_bg.xml`,
`ic_widget_*.png`) — 0 asset baru, identitas visual otomatis konsisten widget↔bubble.
`FloatingBubbleStore.kt` (baru, `data/`) simpan toggle + posisi drag terakhir, pola identik
`ShakeSettingsStore`.

**Kontrol**: `MediaController` asli dikoneksikan langsung dari Service (pola sama
`PlayerViewModel.connect()`) utk update LIVE, fallback ke Intent `WidgetUpdater.ACTION_TOGGLE_
PLAY/NEXT/PREVIOUS` yang SUDAH ADA ke `PlaybackService` (0 action constant baru). Artwork pakai
`contentResolver.loadThumbnail()` langsung di URI lagu, pola identik `AudioArtFetcher`.

**Permission**: `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` (bukan runtime permission dialog
biasa — tidak ada callback granted/denied yang bisa diandalkan lintas OEM), status dicek ulang
via `Settings.canDrawOverlays()` begitu user kembali dari layar sistem. `MainActivity.kt`
(protected, edit parsial) — `overlayPermissionLauncher` + `toggleFloatingBubble()` +
`LaunchedEffect(Unit)` restart Service sekali per proses kalau sesi sebelumnya ON & izin masih
ada (proses baru = Service lama ikut mati). `PlayerViewModel.kt` — `floatingBubbleEnabled`
StateFlow murni simpan preferensi (TIDAK start/stop Service sendiri, butuh Context Activity).

**Batasan jujur**: bukan foreground service (window overlay tampil = importance proses
mendekati "visible" di kebanyakan device, tapi skin agresif tetap bisa membunuhnya — sama
seperti keterbatasan widget). Belum diverifikasi di device fisik. **Atomic Change**: 11 file
(>10 batas normal) — dideklarasikan karena 1 fitur koheren membentang izin+service+store+UI+
dokumentasi wajib, memecah jadi >1 batch akan meninggalkan kode mati. Detail lengkap:
`CHANGELOG.md` Batch 95.

**Batch 94 (Dokumentasi: rapikan urutan newest-first + "welcome-ability" README, 0 file kode,
2 file dokumentasi diedit)** — Permintaan user: pastikan info terbaru selalu di paling atas
di semua file dokumentasi + tambah shortcut unduh APK GitHub Release di README.

`CHANGELOG.md` — audit urutan ditemukan 2 blok riwayat lama tidak urut sempurna turun
(Batch 15/14 setelah Batch 7, harusnya sebelum Batch 12; Batch 49/48 di antara 46/47,
harusnya sebelum 47) — dipindah ke posisi numerik benar, isi entri tidak diubah. File ini
sekarang (dan sebelumnya) sudah urut turun sempurna, tidak disentuh. "Riwayat insiden
kronologis" di bawah sengaja **tidak** ikut disortir — label filenya eksplisit kronologis
(tertua→terbaru), beda tujuan dari daftar batch. `FILE_MANIFEST.txt` (alfabetis) &
`ROADMAP_15_FITUR_OFFLINE.md` (bernomor per-item) di luar cakupan aturan newest-first.

`README.md` — bagian baru "📥 Unduh Aplikasi" di bawah judul (link relatif
`../../releases/latest`, auto-resolve ke GitHub Release terbaru tanpa hardcode nama
repo/owner), callout "🆕 Update terbaru" (**wajib disinkronkan manual** tiap ada batch fitur
baru — saat ini menunjuk Batch 93), dan Daftar Isi (TOC) untuk navigasi (file sudah >180
baris, sebelumnya tanpa TOC).

**Batch 93 (Fitur baru: Mode Audiobook/Podcast, 4 file kode + 4 file dokumentasi)** — Dari
`ROADMAP_15_FITUR_OFFLINE.md` item #12. Ingat kecepatan & posisi terakhir per-lagu individual
(bukan speed global yang berlaku ke semua lagu), tampilan "menit tersisa" (`-mm:ss`) untuk file
yang di-opt-in.

**Bukan extend `PlaybackStateStore`** seperti dugaan awal roadmap (dicek dulu isi filenya — itu
murni resume 1 QUEUE global, tidak natural diperluas per-song). `AudiobookModeStore.kt` (baru,
`data/`) — 1 record JSON per lagu, pola sama `BookmarkStore` (key-per-song) tapi object tunggal
bukan array. **Opt-in manual per-lagu, bukan heuristik durasi/genre** — genre sudah lama sengaja
di-skip (Batch 89, N+1 query), heuristik durasi rawan salah tebak (instrumental panjang, DJ mix).

`PlayerViewModel.kt`: `setAudiobookModeEnabled()` (seed speed dari yang sedang jalan + persist
posisi langsung, bukan nunggu tick ~5s), `onMediaItemTransition` resume speed+posisi lagu yang
di-opt-in — **sengaja skip untuk `MEDIA_ITEM_TRANSITION_REASON_REPEAT`** (Repeat Satu Lagu),
kalau tidak di-skip tiap loop bakal seek balik ke posisi lama alih-alih restart bersih dari 0.
`persistPlaybackState()` (cadence ~5s-saat-main + langsung-saat-pause yang sudah ada) diperluas
sekalian save progress audiobook (no-op internal kalau lagu tidak di-opt-in).

`NowPlayingScreen.kt` — toggle baru ditaruh di dialog "Pengaturan Putar" yang SUDAH ADA (bukan
sheet baru — home paling natural karena memang soal speed per-file), teks durasi kanan berubah
`-mm:ss` (konvensi podcast player) saat mode aktif untuk lagu yang sedang main. `MainActivity.kt`
(protected, edit parsial) — 1 `collectAsStateWithLifecycle()` + 2 parameter diteruskan. 0
perubahan struktur NavHost.

Brace/paren 4 file kode dicek otomatis & seimbang. `FILE_MANIFEST.txt` di-diff eksplisit
terhadap isi ZIP — match. **Belum diverifikasi compile/runtime Gradle sungguhan** — prioritas
berikutnya: `./gradlew assembleDebug`, cek di device (1) toggle ON lalu pindah lagu lalu balik —
speed & posisi kembali tepat, (2) **Repeat Satu Lagu pada lagu ter-opt-in TIDAK seek balik tiap
loop** (titik paling berisiko meleset tanpa device — kalau guard reason-nya salah, lagu akan
terlihat "macet" muter dari tengah terus bukan dari awal), (3) teks `-mm:ss` update mengikuti
posisi berjalan bukan statis, (4) toggle OFF benar menghapus record tersimpan. Detail lengkap:
`CHANGELOG.md` Batch 93.

**Batch 92 (Fitur baru: Visualizer Audio, 7 file kode + 4 file dokumentasi)** — Dari
`ROADMAP_15_FITUR_OFFLINE.md` item #9. Sheet baru "Visualizer Audio" di Now Playing → Kontrol
Lanjutan (pola sama Timer/Kecepatan/Equalizer/Repeat A-B), spectrum bar 24-bar dari
`android.media.audiofx.Visualizer`.

**Riset izin duluan**: `RECORD_AUDIO` ternyata wajib di SEMUA versi Android untuk audio session
apa pun (bukan "beberapa versi" seperti dugaan awal di roadmap, tidak ada pengecualian "baca
audio sendiri"). Diminta on-demand (baru saat toggle dinyalakan di sheet), bukan di onboarding
wajib — `visualizerPermissionLauncher` (`MainActivity.kt`), auto-nyala kalau granted (user tak
perlu tap switch 2x). `AndroidManifest.xml` (protected, edit parsial) dapat komentar panjang
kenapa izin ini bukan berarti app merekam suara.

`AudioVisualizerController.kt` (baru, `playback/`) — bungkus `Visualizer`, attach ke
`PlaybackAudioSession.sessionId` (mekanisme sharing session ID sama persis `EqualizerController`
pakai — satu-satunya cara tahu `audioSessionId` ExoPlayer karena `PlayerViewModel` cuma pegang
`MediaController`). Capture rate ditahan ~15fps, FFT byte array dikelompokkan jadi 24 bar
magnitude ternormalisasi. **2 bug method-vs-property ditemukan & diperbaiki sebelum final**:
`getMaxCaptureRate()` itu `static` (harus `Visualizer.getMaxCaptureRate()`, bukan lewat
instance); `setCaptureSize()`/`setEnabled()` keduanya return `Int` bukan `void` — Kotlin tidak
bisa treat sebagai property assignable, wajib method call eksplisit (`viz.setCaptureSize(...)`,
bukan `viz.captureSize = ...`) — persis alasan `EqualizerController.kt` lama sudah selalu pakai
`eq.setEnabled(...)` eksplisit.

`VisualizerSettingsStore.kt` (baru, pola `ShakeSettingsStore`) + `VisualizerSheet.kt` (baru, shell
sama `EqualizerSheet.kt`, `SpectrumBars` — Canvas custom KEDUA di codebase setelah
`WeeklyTrendChart` Batch 90) + `PlayerViewModel.kt` (`ensureVisualizerAttached()`/
`setVisualizerEnabled()`/`stopVisualizerCapture()` — beda dari equalizer, capture cuma jalan
selagi sheet terbuka, tidak ada alasan tetap capture kalau tidak terlihat) + `NowPlayingScreen.kt`
(8 param baru, 1 row baru ikon `GraphicEq`) + `MainActivity.kt` (protected, edit parsial — 3
`collectAsStateWithLifecycle()` + permission launcher + 8 param diteruskan).

**Keputusan scope eksplisit**: spectrum bar HANYA capture selagi sheet terbuka, TIDAK dirender
permanen di layar Now Playing utama — `FloatArray` bukan tipe stabil buat Compose compiler,
thread terus-menerus ke seluruh `NowPlayingScreen` berisiko recomposition ~15fps termasuk animasi
album art/blur, risiko jank yang tak bisa diverifikasi tanpa device.

Brace/paren 7 file kode dicek otomatis & seimbang. **Belum diverifikasi compile/runtime Gradle
sungguhan** — prioritas berikutnya: `./gradlew assembleDebug`, cek di device (1) dialog permission
muncul benar saat toggle pertama kali, (2) bar spectrum genuinely sinkron lagu (bukan statis/acak
— bug paling gampang lolos tanpa device), (3) capture size 512 didukung device asli, (4) tidak ada
jank di Now Playing selagi sheet terbuka, (5) `Visualizer` benar ter-release saat sheet ditutup.
Detail lengkap: `CHANGELOG.md` Batch 92.

**Batch 91 (Fitur baru: A-B Repeat & Bookmark Posisi, Atomic Change 8 file kode + 5 file
dokumentasi)** — Dari `ROADMAP_15_FITUR_OFFLINE.md` item #4. Sheet baru "Repeat A-B & Bookmark"
di Now Playing → Kontrol Lanjutan (pola sama Timer/Kecepatan/Equalizer).

**A-B Repeat**: tandai Titik A & B di posisi saat ini, playback loncat balik ke A begitu lewat
B, berulang sampai dihapus/lagu ganti. Boundary check di `AbRepeatLogic.kt` baru — pure
`object`, 0 Context, pola sama `SmartPlaylistEngine`/`ListeningStatsEngine` (Batch 89/90),
`isActive()`/`shouldLoopBack()` treat B<=A atau salah satu null sebagai "belum aktif" (bukan
crash/loop-di-1-titik) — `AbRepeatLogicTest.kt` 7 test termasuk kasus tepi pointA=0L (jangan
disalahartikan sebagai "belum diatur"). State `_abRepeatPointA`/`_abRepeatPointB`
(`PlayerViewModel.kt`, StateFlow terpisah dari `PlaybackUiState` — dicek tiap tick 500ms di
`startPositionLoop()` yang sudah ada, tidak perlu memicu recomposition uiState penuh). Direset
otomatis di `onMediaItemTransition` — scoped 1 lagu, titik B lagu lama yang kebawa ke lagu baru
berisiko diam-diam memotong intro. `setAbRepeatPointA()` sekalian hapus titik B lama kalau
B<=A baru (cegah state "aktif tapi diam" tanpa penjelasan).

**Bookmark Posisi**: tandai beberapa titik favorit per-lagu (intro/reff/solo dll, dinamai
sendiri), tap-untuk-lompat, hapus per-bookmark. `Bookmark.kt`+`BookmarkStore.kt` baru — JSON per
song ID, pola storage sama `SmartPlaylistStore`, key-per-song sama `LyricsStore`. **Beda dari
`PlaybackStateStore` existing** (itu cuma 1 posisi resume utk seluruh antrean, ini banyak titik
bernama per-lagu).

`ABRepeatBookmarkSheet.kt` baru (UI kedua fitur) + `NowPlayingScreen.kt` (8 parameter baru, 1
row baru di `AdvancedControlsSheet` pakai ikon `Repeat` yang sudah diimpor — 0 import baru,
pola `remember(song.id)` sama seperti `lyricsText` Batch 82) + `MainActivity.kt` (protected,
edit parsial — 2 `collectAsStateWithLifecycle()` + 8 parameter diteruskan ke pemanggilan
`NowPlayingScreen(...)` yang sudah ada, 0 perubahan struktur NavHost/route, numpang layar
existing sama seperti Batch 89/90).

Brace/paren semua 8 file kode dicek otomatis & seimbang. `FILE_MANIFEST.txt` di-diff eksplisit
terhadap isi ZIP sebelum dikirim — 127/127 match. **Belum diverifikasi compile/runtime Gradle
sungguhan** (tidak ada JDK/Android SDK/kotlinc di sandbox ini) — prioritas berikutnya:
`./gradlew testDebugUnitTest` verifikasi 7 test baru, lalu build APK asli + cek di device: (1)
A-B Repeat loncat balik ke A tepat saat lewat B tanpa glitch audio terasa, (2) titik A/B hilang
otomatis saat lagu ganti (manual & auto-advance), (3) bookmark tersimpan lintas restart app,
(4) sheet render benar di kedua tema custom (Tactile/Skeu, reuse `frostedGlass()` existing,
risiko rendah tapi belum pernah dilihat). Detail lengkap: `CHANGELOG.md` Batch 91.

**Batch 90 (Fitur baru: Dashboard Statistik Dengar Lokal, Atomic Change 9 file kode + 5 file
dokumentasi)** — Dari `ROADMAP_15_FITUR_OFFLINE.md` item #10. Layar baru di
Pengaturan → "Statistik Dengar": total lagu diputar, estimasi waktu dengar (durasi × jumlah
putar per lagu — bukan log posisi kontinu, jadi ini estimasi best-effort, bukan angka presisi),
grafik batang tren 7 hari terakhir (Canvas custom — **chart pertama di codebase ini**, sengaja
dibuat minimal: cuma rounded-bar, tanpa gridline/axis/text-di-canvas, supaya area kesalahan
render kecil tanpa compiler untuk verifikasi), jam favorit dengar (dari 24 bucket jam-dalam-hari,
all-time), dan 5 artis paling sering diputar.

Route baru `stats_dashboard` (`MainActivity.kt`, protected — edit parsial, cuma nambah 1
composable + 1 callback ke `SettingsScreen`, 0 perubahan struktur route lain). Data lama sudah
cukup untuk sebagian besar (`PlayStatsStore`, `ListeningHistoryStore`), **kecuali jam favorit**
— sebelum batch ini app tidak pernah mencatat jam berapa lagu diputar (`ListeningHistoryStore`
cuma granularitas per-hari, bukan per-jam). Ditambah `HourlyListenStore.kt` baru (24 counter
flat per jam) — sengaja file terpisah, BUKAN memperluas skema key `ListeningHistoryStore` yang
sudah ada, supaya nol risiko migrasi untuk histori dengar yang sudah tersimpan user lama.

7 file data/logic + `ListeningStatsEngine.kt` baru (pure aggregator — `topArtists`,
`totalListeningMs`, `peakHour`, `buildSnapshot` — pola sama seperti `SmartPlaylistEngine` Batch
89, Context-free supaya bisa di-unit-test tanpa Robolectric) + `ListeningStatsEngineTest.kt`
(13 unit test) + `StatsDashboardScreen.kt` (UI, reuse pola `StatSectionCard` conditional
Tactile/Skeu emboss dari `ContinueListeningCard` Batch 59) + `PlayerViewModel.kt` (wire
`hourlyListenStore` di titik `recordPlay` yang sama dgn `playStatsStore`/
`listeningHistoryStore`, + 1 fungsi `getListeningStats()`) + `SettingsScreen.kt` (1 menu row
baru, non-protected). `PlayStatsStore`/`ListeningHistoryStore` masing-masing dapat 1 fungsi
tambahan (`getAllCounts()`/`getCountsForLastDays()`) — murni additive, 0 fungsi lama diubah.

Brace/paren semua 9 file kode dicek otomatis & seimbang. `FILE_MANIFEST.txt` di-diff eksplisit
terhadap isi ZIP sebelum dikirim (bukan cuma dicek di folder kerja) — 122/122 match, pelajaran
dari insiden Batch 27 revisi 1 (ZIP nested + exclude flag salah) diterapkan lagi di sini.
**Belum diverifikasi compile/runtime Gradle sungguhan** (tidak ada JDK/Android SDK/kotlinc di
sandbox ini) — prioritas berikutnya: `./gradlew testDebugUnitTest` verifikasi 13 test baru
(ekstra hati-hati ke `peakHour` tie-breaking & `totalListeningMs` overflow untuk library besar),
lalu build APK asli + cek tab "Statistik Dengar" render benar di device, KHUSUSNYA
`WeeklyTrendChart` (Canvas custom pertama di app ini — paling berisiko meleset visual dari
niatnya dibanding bagian lain batch ini yang murni reuse pola existing). Detail lengkap:
`CHANGELOG.md` Batch 90.

**Batch 89 (Fitur baru: Playlist Otomatis / Smart Playlist, Atomic Change 11 file kode)** —
Dari `ROADMAP_15_FITUR_OFFLINE.md`. Playlist berbasis aturan (folder, rentang durasi, rating
minimum, rentang tahun rilis, kata kunci) — beda dari playlist manual yang sudah ada
(`PlaylistStore`, simpan daftar ID lagu tetap), Smart Playlist cuma simpan kriteria dan
`SmartPlaylistEngine` resolve daftar lagu LIVE tiap dibuka, jadi lagu baru yang cocok otomatis
ikut masuk. Numpang di tab Library yang sudah ada ("Otomatis", tab ke-6 di dropdown "Lainnya")
— **bukan** route NavHost baru, jadi permukaan protected asset (`MainActivity.kt`) yang
tersentuh minimal (cuma thread StateFlow + 3 callback baru ke pemanggilan `LibraryScreen(...)`
yang sudah ada, 0 perubahan struktur `NavHost`/route).
3 file data baru (`SmartPlaylist.kt` model, `SmartPlaylistEngine.kt` pure matcher/resolver,
`SmartPlaylistStore.kt` persist JSON) + `SmartPlaylistScreen.kt` (tab view + builder sheet) +
`SmartPlaylistEngineTest.kt` (11 unit test). `Song.kt` dapat field baru `year: Int = 0`
(default → backward-compatible ke semua call site lama termasuk fixture test) supaya kriteria
"rentang tahun rilis" bisa jalan — diisi dari `MediaStore.Audio.Media.YEAR`
(`MusicRepository.kt`) & `METADATA_KEY_YEAR` (`CustomFolderScanner.kt`).
**Genre sengaja di-skip** dari roadmap — MediaStore taruh genre di tabel terpisah (query
per-lagu, N+1), risiko/kompleksitas lebih tinggi dari sisa kriteria di batch ini, belum
dijadwalkan. Builder pakai text field angka (menit/tahun), bukan slider — alasan sama README
soal drag-gesture custom tanpa compiler buat verifikasi. Brace/paren tiap file dicek manual &
seimbang. **Belum diverifikasi compile/runtime Gradle sungguhan** (tidak ada JDK/Android SDK/
kotlinc di sandbox ini) — prioritas berikutnya: `./gradlew testDebugUnitTest` verifikasi 11
test baru, lalu build APK asli + cek tab "Otomatis" render & builder sheet berfungsi di device.
Detail lengkap: `CHANGELOG.md` Batch 89.

**Batch 88 (Fix bug mini player dobel di Now Playing + sederhanakan hierarki tombol)** — User
laporan "hierarki tombol nya terlalu membingungkan bagi user awam" + screenshot layar Now
Playing yang nunjukkan floating mini player nongol lagi di bawah, nimpa/mepetin kontrol layar
penuh di atasnya. 2 file: (1) `MainActivity.kt` — bug nyata, `AnimatedVisibility` mini player
di `bottomBar` cuma cek `currentSong != null` TANPA cek route, beda dari NavigationBar tepat di
bawahnya yang sudah benar exclude `"now_playing"` — fix tambah `&& currentRoute != "now_playing"`.
(2) `NowPlayingScreen.kt` — top bar disederhanakan dari 5 ikon jadi 3 (Tutup/Favorit/Lanjutan),
Antrean & Lirik dipindah gabung ke sheet "Kontrol Lanjutan" yang sudah ada (pola sama dgn
Timer/Kecepatan/Equalizer di situ). Detail lengkap: `CHANGELOG.md` Batch 88.

**Batch 87 (Hotfix CI FAILED — user upload `log_fail_139.zip`, 1 file PROTECTED)** — Batch 86
gagal compile sungguhan di CI: `const val` (`versionMajor`/`commitsPerMinor`) tidak valid di
badan script `.gradle.kts` ("Const 'val' are only allowed on top level, in named objects, or in
companion objects" — script body bukan salah satu dari itu, beda dari `.kt` file/class biasa).
Fix: `private const val` → `val` polos, konsisten dgn semua deklarasi lain di file ini. Formula
versionName sendiri (MAJOR.MINOR.PATCH dari commit count) TIDAK berubah. ⚠️ **Belum ada CI run
baru yang membuktikan fix ini lolos** — baru menghilangkan 1 error spesifik yang terkonfirmasi
dari log asli. Prioritas paling atas kalau user push: pastikan run CI berikutnya BENAR-BENAR
hijau sebelum dianggap selesai — jangan andalkan audit statis lagi untuk area ini, sudah terbukti
sekali meleset. Detail lengkap: `CHANGELOG.md` Batch 87.

**Batch 86 ("bump version statis -> otomatis+dinamis", diklarifikasi dulu via ask_user_input_v0
— 3 file, 2 di antaranya PROTECTED)** — `versionName` app: prefix `"1.0."` yang selama ini beku
permanen (cuma commit-count di belakang yang jalan) diganti MAJOR.MINOR.PATCH genuinely dinamis
(`MINOR = commit_count / 50`, `PATCH = commit_count % 50`, jadi `1.0.x → 1.1.x → 1.2.x` seiring
waktu). MAJOR (`= 1`) tetap konstanta manual SENGAJA (standar semver — MAJOR selalu gate di
belakang keputusan manusia, bukan oversight). `versionCode` TIDAK diubah (tetap commit count
mentah, internal-only). `app/build.gradle.kts` DAN `.github/workflows/build.yml` (2 PROTECTED
asset) harus diubah BERSAMAAN dgn formula identik (`commitsPerMinor`/`COMMITS_PER_MINOR = 50` di
kedua tempat) — kalau salah satu diubah tanpa yang lain, tag GitHub Release drift dari
versionName sungguhan di APK (invariant yg sama dijaga sejak Batch 30/56). `README.md` bagian
"Standar Penomoran Versi" diupdate contoh, sekalian 1 ketidaksesuaian kecil pre-existing
diperbaiki (`-release.apk` → `-release-run<N>.apk`, menyesuaikan tag CI yang sebenarnya).

⚠️ **Belum diverifikasi compile/CI sungguhan** — risiko tertinggi sejauh ini krn 2 protected
asset tersentuh bersamaan, no gradle/GitHub Actions run di environment kerja ini. Prioritas
paling atas kalau user push: jalankan 1 CI run penuh, cek step "Determine version name" tidak
error, dan versionName yg tampil di app SAMA PERSIS dgn tag GitHub Release. Detail lengkap:
`CHANGELOG.md` Batch 86.

**Batch 85 (Fix "kurang efek depth/3D" — feedback screenshot device sungguhan, 4 file)** —
Gradient LINEAR diagonal Batch 84 (dual-shadow panel+disc widget) ternyata nyaris tak kelihatan
di layar sungguhan (nyebar merata ke seluruh bidang, alpha "far/lemah" token asli terlalu halus
utk bidang seluas ini). Diganti gradient RADIAL dipusatkan di pojok (highlight kiri-atas,
shadow kanan-bawah) + alpha dinaikkan signifikan — radial falloff sendiri yg jaga area tetap
sempit jadi aman lebih tinggi. `gradientRadius` dp fix (140dp panel/40dp disc, bukan persen —
minSdk 23, `%p` butuh API 29+). 0 file baru. **Belum diverifikasi visual utk perubahan Batch 85
ini sendiri** (Batch 84 sudah, via screenshot user). Detail lengkap: `CHANGELOG.md` Batch 85.

Pending dari user, BELUM dikerjakan (butuh klarifikasi, ditanya di chat, bukan diasumsikan):
"bump version statis -> otomatis+dinamis" — ambigu, versionCode/versionName di
`app/build.gradle.kts` SUDAH 100% otomatis dari git commit count sejak Batch 30/56 (dikonfirmasi
ulang di sesi Batch 83). Kemungkinan yang dimaksud malah nomor "vN" di NAMA FILE ZIP output
(dipilih manual tiap batch oleh asisten, sempat skip v83 krn batch itu audit-only) — atau hal
lain. Jangan asumsikan salah satu tanpa konfirmasi user dulu, protected asset (build.gradle.kts)
resikonya CI/release rusak kalau salah tebak.

**Batch 84 (Arahan "redesign theme widget lama -> Neumorphism hardcode" — 5 file)** — Widget
home-screen (RemoteViews, bukan Compose, jadi tidak pernah bisa ikut ThemeStore Tactile/Skeu/
Apple) diganti render Neumorphism SELALU ("di-hardcode"), apapun tema di dalam app. Sumbu
gelap/terang (`isDark`, terpisah dari pilihan tema) tidak disentuh. `widget_background(_light).xml`:
solid polos → `layer-list` base `SkeuNeuSurfaceDark`/`Light` + dual-shadow gradient diagonal
(135°/315°, alpha sisi "far" token asli, 0 border). `widget_play_button_bg.xml` (redesign) +
`_light.xml` (BARU): oval merah `#FA233B` peninggalan lama → disc `SkeuEmerald`/`SkeuLightEmerald`
(dipilih drpd Titanium krn ikon play/pause putih polos, kontras lebih baik). `WidgetUpdater.kt`:
warna teks diganti ke hex PERSIS token Color.kt (bukan palet ad-hoc terpisah), + tombol
play/pause sekarang `setBackgroundResource` switch dark/light sama pola dgn root.
`FILE_MANIFEST.txt` 111→112 (1 file baru). 0 protected asset lain disentuh. **Belum diverifikasi
visual sungguhan di device** — no emulator/RemoteViews preview di environment kerja ini. Detail
lengkap: `CHANGELOG.md` Batch 84.

**Batch 82 (Arahan "debugging+Polish UI" — audit lintas ui/, 2 file)** — Audit statis sistematis
semua file `ui/`, fokus pola `remember { mutableStateOf(paramTurunan) }` tanpa key (kandidat
state-leak kalau composable tetap ter-mount lintas perubahan data). 1 bug nyata + 1 polish:
1. **Bug — `LyricsSheet.kt`**: `editing`/`draft` unkeyed `remember`, padahal diturunkan dari
   `rawLyrics`. Sheet bisa tetap terbuka lintas pergantian lagu (media-session eksternal —
   headset/notifikasi/widget — bisa ganti lagu tanpa lewat sheet ini), state lama nyangkut ke
   lagu baru; worst-case draft lirik lagu A ke-simpan ke lagu B. Fix: `remember(rawLyrics) {...}`
   di keduanya. Diaudit: `PlaylistScreen.kt` TextInputDialog **mirip tapi TIDAK bug** — modal
   AlertDialog, selalu di-mount fresh per `if (showRenameDialog)`, tidak ada jalur eksternal
   mengubah `selectedPlaylist` selagi dialog terbuka.
2. **Polish — `LockScreen.kt`**: layar PIN (paling sering disentuh, tiap cold-open App Lock)
   satu-satunya kontrol frekuensi-tinggi yang belum dapat identitas Tactile/Skeu (`CircleShape`
   polos sejak sebelum Batch 79). `PinKey` + `RoundGlyphButton` (baru, gantikan 2 `Box` inline
   fingerprint/backspace yg terpisah) sekarang pakai `tactileEmboss()`/`skeuEmboss()` +
   `pressed` dari `collectIsPressedAsState()` + `bouncyPress()`, sama pola dengan transport
   button Now Playing. **Apple theme 0 perubahan** (cabang `else` tetap CircleShape polos).
   State/alur verifikasi PIN tidak disentuh.

0 protected asset disentuh, brace/paren balance kedua file dicek manual & seimbang. **Masih
belum diverifikasi compile/visual sungguhan di device** — sama seperti seluruh batch
sebelumnya, tidak ada `kotlinc`/emulator di environment kerja ini; prioritas berikutnya kalau
user minta lanjut: rebuild CI + install APK, cek (a) lirik tidak lagi nyasar antar-lagu saat
auto-advance sambil sheet terbuka, (b) 3 tombol LockScreen (digit/fingerprint/backspace) kebaca
tactile/skeu-nya di kedua tema. Detail lengkap: `CHANGELOG.md` Batch 82.

**Batch 81 (Fix "Ambient Light gak bocor" — bagian instruksi user Batch 79 yg belum tersentuh)** —
Instruksi asli user Batch 79 punya 4 bagian: Titanium dominan, sentuhan Zamrud, depth ultra
realistic, DAN "Ambient Light yang gak bocor". Batch 79/80 tuntaskan 3 bagian pertama dgn baik,
tapi containment ("gak bocor") belum pernah ditangani — dual-shadow `skeuEmboss()`/hero art
digambar di `drawBehind{}` sebelum `.clip()` TANPA batas area (Compose tidak clip `drawBehind{}`
ke bounds layout-nya sendiri by default), jadi bayangan lebar (mis. MiniPlayerBar elevation
16.dp) berisiko nimpa sibling di sekitarnya. Fix, 2 file:
1. `TactileDepth.kt`'s `skeuEmboss()` — dual-shadow (5 layer) dibungkus `clipRect()`, halo
   proporsional ke `elevation` (`*1.3f`, di atas offset terjauh `1.05f` biar bentuk bayangan
   tidak ikut terpotong) — bayangan dijamin tidak meluber lebih jauh dari itu di caller manapun.
2. `NowPlayingScreen.kt`'s AlbumArtHero — ditemukan 1 bug sekalian saat audit: sisi TERANG dulu
   di `drawBehind{}` terpisah SETELAH `.clip()` (beda dari sisi gelap yg sebelum `.clip()`) —
   jadi sisi terang selama ini kepotong tepat di tepi, tak pernah bisa "meluber" sama sekali,
   beda arsitektur dari `skeuEmboss()` sendiri. Disatukan ke 1 `drawBehind{}` sebelum `.clip()`
   + dibungkus `clipRect()` halo tetap 18.dp. Emerald glint (Batch 80) TIDAK dipindah — sudah
   benar sbg layer terpisah setelah `.clip()` ("permata di permukaan", bukan bayangan).

**README.md juga diperbarui** — paragraf tema custom kedua masih mendeskripsikan "Skeuomorphism
2.0 — Hyper-Realism UI" 7-layer lama (grain, border ganda) yg sudah tidak ada sejak Batch 79,
ditulis ulang jadi deskripsi Neumorphism akurat. **Audit sebelum fix** (bukan asumsi): grep
konfirmasi `SkeuAccent`/Titanium* masih 100% tidak tersentuh (Titanium tetap dominan), token
grain/groove lama 0 caller tersisa, `FILE_MANIFEST.txt` cocok 100% dgn file tree (112/112),
brace/paren balance kedua file diedit dicek manual & seimbang. Titanium tetap dominan, cakupan
Zamrud tidak bertambah dari Batch 80 — batch ini murni containment + 1 bug clip. **Masih belum
diverifikasi compile/visual sungguhan di device** — sama seperti Batch 79/80, tidak ada
`kotlinc`/emulator di environment kerja ini; prioritas berikutnya kalau user minta lanjut:
rebuild CI + install APK, cek khususnya MiniPlayerBar (elevation 16.dp, kasus containment
paling ketat) & hero art tidak lagi kepotong di sisi terangnya. Detail lengkap: `CHANGELOG.md`
Batch 81.

**Batch 80 (Fix visibilitas Zamrud — respons langsung feedback user "mana zamrudnya??")** —
Batch 79 sengaja bikin emerald sangat halus (blend rendah + cuma nyala saat pressed + alpha
diturunkan dari nilai kecil), efeknya kebablasan: user lihat UI/screenshot idle dan emerald-nya
betul-betul 0% kelihatan di 3 titik sekaligus. Fix, 3 file (di bawah batas normal, TANPA perlu
Atomic Change exception — scope = subset 3 dari 6 file Batch 79, tuning angka + 1 teknik render,
bukan redesign baru):
1. `skeuEmboss()` (TactileDepth.kt) — emerald sekarang radial glint TERPISAH (warna murni, bukan
   di-blend ke `lightNear` putih) — alpha baseline 0.20f idle (genuinely visible tapi tetap
   "sedikit") naik ke 0.52f saat pressed. Posisi ikut `dir` (concave-flip yg sama dgn sisi
   terang/gelap).
2. Root ambient wash (MainActivity.kt, protected/parsial) — alpha stop emerald diganti dari
   `streakAlpha * 0.9f` (~0.045-0.108, tak kelihatan) jadi alpha TETAP 0.30f/0.36f, independen
   dari streakAlpha yang kecil.
3. Hero art (NowPlayingScreen.kt) — lerp-blend 14% ke `heroSpecular` dihapus, diganti radial
   glint terpisah warna murni, alpha tetap 0.35f/0.42f, permanen (statis, no pressed state).

**Titanium tetap dominan** — 0 perubahan di role M3 primary/surfaceTint (`SkeuAccent` dkk. sama
sekali tidak disentuh); ini murni menaikkan visibilitas 3 titik emerald yang sudah direncanakan
Batch 79 supaya genuinely kebaca, bukan menambah cakupan/dominasi emerald baru. Detail lengkap:
`CHANGELOG.md` Batch 80. **Masih belum diverifikasi compile/visual sungguhan di device** — sama
seperti batch-batch sebelumnya, tidak ada `kotlinc`/emulator di environment kerja ini; prioritas
berikutnya kalau user minta lanjut: rebuild CI + install APK, cek genuinely kelihatan zamrud-nya
di layar HP asli.

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

> **Arsip Batch 1–57** dipindah ke `PROJECT_STATE_ARCHIVE.md` (Batch 158) — detail lengkap batch-batch lama ada di sana, urutan descending sama seperti asalnya. File ini (`PROJECT_STATE.md`) sekarang cuma menyimpan 100 batch paling baru (Batch 58 ke atas) supaya tidak terus memanjang tanpa batas. `CHANGELOG.md` tetap punya detail penuh untuk SEMUA batch termasuk 1-57.


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

