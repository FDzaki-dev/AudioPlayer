# AudioPlayer — Micro UI/UX Gap Audit
## Target: 100% Micro UI/UX Polish

> ## 📦 ARSIP — DIHENTIKAN (Batch 278)
> Dokumen ini **tidak lagi aktif diikuti**. Dihentikan bukan karena isinya salah — 12/14 kategori
> genuinely TUNTAS (kerja nyata, bukan diklaim) — tapi karena arah goal project berubah total:
> user instruksikan pivot ke identitas visual baru terinspirasi **CONVX** (Liquid Glass, iOS-
> inspired, lihat `ROADMAP_LIQUID_GLASS_REDESIGN.md`). Dokumen ini mengukur konsistensi micro-detail
> (spacing/wording/icon/typography-hierarchy/a11y) terhadap SISTEM VISUAL LAMA (Apple/Tactile/
> Neumorphism/Calm Retro) — begitu shape+typography diganti total, sebagian besar checklist ini
> otomatis tidak relevan lagi (mengaudit sesuatu yang segera diganti = kerja sia-sia).
>
> **Yang TETAP relevan/reusable** (tidak spesifik ke skin visual manapun, murni hygiene): kategori
> 1 (Strings/Wording), sebagian kategori 5 (error/success state pattern, bukan warnanya), dan
> kategori Accessibility (TalkBack semantics, focus order, touch target, text-scaling) — prinsip-
> prinsipnya berlaku lintas skin visual apapun. Kalau sesi redesign butuh referensi "wording apa
> yang sudah disepakati konsisten" atau "pola accessibility apa yang harus dipertahankan", cek
> dulu di sini sebelum reinvent.
>
> **Kategori 13-14 (belum sempat mulai)** — dibatalkan, akan didesain ulang dari nol mengikuti
> prinsip shape/typography baru, bukan dilanjutkan dari draft lama.

---

> **STRICT SCOPE:** Presentation/UI/UX polish only.
> **DILARANG:** perubahan logic, playback engine, queue behavior, SAF, database, scanning, persistence, navigation architecture, atau feature behavior kecuali ada bug UI murni yang benar-benar urgent.

---

## 📍 STATUS TRACKING (update tiap batch — taruh di paling atas, jangan di bawah)
Eksekusi mengikuti **FINAL EXECUTION ORDER** di bawah, 1 slice kecil per batch (disiplin
"jangan greedy" dari user — bukan sapuan borongan semua 14 kategori sekaligus). Progres per
kategori dicatat di sini; detail teknis tiap batch ada di `CHANGELOG.md`.

| # | Kategori | Status |
|---|----------|--------|
| 4 | Touch Target & Micro Interaction | ✅ **SELESAI (Batch 124-127, 141)**. CTA/chip (124-126) + tombol sekunder frekuensi-tinggi (127) + hit-target size audit formal (141: 2 gap ditemukan & diperbaiki — `FeatureHintBanner` dismiss 40dp→48dp, `ContinueListeningCard` play 44dp→48dp; sisanya semua sudah Material default 48dp, dikonfirmasi grep bukan tebakan) + ripple-terpotong-container audit (141: 0 kasus ditemukan). **Sengaja tetap tidak disentuh**: TextButton "Batal"/"Tutup" di dalam `AlertDialog` (BackupRestoreSheet/DuplicateFinderSheet/SignatureMatcherSheet/SongInfoEditSheet) — dikonfirmasi ulang Batch 141, ini keputusan SADAR sejak Batch 124/127 (\"sekali-tekan, dampak micro-feedback lebih kecil dari yang sudah dikerjakan\"), bukan gap yang terlewat. |
| 1 | Strings & Wording Consistency | ✅ **SELESAI (Batch 142-145)**. 142: wording undo-hide disamakan (`"Batalkan"`→`"Urungkan"`). 143: audit "Batal" vs "Tutup" (17 titik) — 0 bug, konsisten by-design. 144: audit title dialog (14 titik, pola "?" konsisten) + fix "Aksi"/"Tindakan" disamakan ke "Aksi". 145: audit formal "Hapus" (22 titik) — 0 bug, 4 kelompok beda fungsi (tombol generik/dinamis/title dialog/`contentDescription` aksesibilitas full-context), genuinely bukan kandidat unifikasi. 0 sisa pending. |
| 2 | Spacing & Sizing Consistency | 🟡 **Berlanjut (Batch 146-147, 151-152)**. 146: audit horizontal screen padding — 4 tab utama Home/Settings/NowPlaying/Playlist/SmartPlaylist/StatsDashboard sudah 20dp; 2 gap di `LibraryScreen.kt` diperbaiki (`AlbumGridView` 16dp→20dp horizontal, 4 titik `ListItem` Artis/Folder/hasil-pencarian/riwayat 4dp→20dp). 147: audit ukuran control setara — `LockScreen.kt` ikon Fingerprint(28dp)/Backspace(22dp) disamakan ke 24dp. 151: audit vertical spacing antar section (`SettingsScreen.kt`, 7 titik pola `Spacer(12dp)→Divider→Spacer(20dp)`) — 0 bug, sudah konsisten. 151-152: **gap icon↔text ✅ SELESAI** — audit formal 29 titik `Icon()→Spacer(width)→Text()` di `ui/*.kt`, dikelompokkan per-konteks (default-size icon+label/TextButton icon custom-size/menu-row Icon+Column/section header) — 2 gap nyata ditemukan & diperbaiki (`PlaylistScreen.kt` "Buat Playlist Baru" & `VaultSheet.kt` "Tambah", keduanya `TextButton`+`Icons.Default.Add` default-size 4dp→8dp), sisanya sudah konsisten by-design. Pending: sisa literal `.dp` lain (padding/size/offset di luar radius/icon-gap/screen-padding yang sudah disentuh). |
| 3 | Typography Hierarchy | ✅ **TUNTAS (Batch 149, 154, 159, 160, 161)**. 149: title bottom sheet (1 gap fix). 154: body/label song-row (1 gap fix). 159: field-label caption (1 gap fix). 160: badge/kicker/value-readout (0 bug). 161: line-height — **5 gap sistemik** (`theme/Type.kt` 5 style ter-override tanpa `lineHeight`, app-wide blast radius) diperbaiki, dihitung proporsional dari rasio M3 asli. **Belum diverifikasi visual** Batch 161 — cek prioritas tinggi. Sisa (SENGAJA tidak diklaim tuntas): cakupan penuh truncation/ellipsis (beda sub-kategori, sebagian sudah Batch 37). |
| 5 | Interactive States (disabled/selected/loading/error) | ✅ **Audit selesai 8/8 sub-item (Batch 162-168)**. disabled icon-button tint (5 titik, konsisten), pressed/ripple integrity (0 `indication=null` ditemukan, aman), loading (`ShimmerBrush` shared, konsisten), **selected/active state (Batch 163-164)** — taksonomi 3 pola selected by-design (Card preview→border+elevation; List/dialog option→RadioButton; Tag/filter→FilterChip fill), 2 konsisten, **1 bug ditemukan & diperbaiki (Batch 163)**: `SpeedDialog` TextButton+"✓" disamakan ke RadioButton (samakan `TransitionModeOption` di dialog sama). **1 dari 3 observasi tertunda DIEKSEKUSI (Batch 164)**: `SongRow` (LibraryScreen) sekarang menandai lagu sedang diputar, disamakan pola `QueueRow`. **error state & success/confirmation feedback (Batch 165-166)** — teks validasi inline 100% konsisten (0 bug); banner hasil-operasi (4 titik/3 file) 3-arah tidak konsisten → **DISATUKAN (Batch 166)**: composable shared `ResultBanner` (`Utils.kt`) + `enum ResultBannerStyle {Solid,Tinted,Bare}`, 3 gaya lama dipertahankan lewat parameter, 0 perubahan visual disengaja. **konsistensi lintas-aksi (Batch 168)** — toggle Favorit (LibraryScreen vs NowPlaying): icon/tint/haptic identik, TAPI `bouncyPress` cuma ada di NowPlaying — genuinely ambigu (list padat vs hero screen), dicatat bukan dieksekusi. **3 observasi masih tertunda keputusan user**: (a) `EmptyState` icon hardcode (Batch 162), (b) `LibraryFilterChips` custom-fill vs FilterChip (Batch 163), (c) Favorit `bouncyPress` di atas (Batch 168). |
| 6-14 | Now Playing s/d Component Consistency | 🟡 **Now Playing ✅ TUNTAS (Batch 169-179)**. **Library/Song List ✅ TUNTAS 11/11 (Batch 180-190)**. **Playlist/Queue ✅ TUNTAS 8/8 (Batch 191-214)**. **Settings ✅ TUNTAS 9/9 (Batch 215-223)**: grouping(215)/title-subtitle(216)/spacing(217, 0 bug)/switch alignment(218, 0 bug)/nav affordance(219, 0 bug)/destructive setting(220, 1 fix — konfirmasi nonaktifkan PIN)/disabled visibility(221, 0 bug)/visual density(222, 0 bug)/fungsi tidak berubah(223, verifikasi 0 bug). **Iconography ✅ TUNTAS 7/7 (Batch 224-232)**: ukuran(224, 1 fix Play/Pause 34dp→40dp)/optical alignment(226-227, 1 fix 4 titik PlayArrow offset+1dp)/visual weight(228, 1 fix ErrorOutline→Error di `ResultBanner`)/action-vs-decorative(229, 1 fix tint badge GraphicEq primary→secondary)/contentDescription null(230, 0 bug)/semantic label(231, 1 fix Shuffle+Repeat state-aware)/no-cosmetic-affordance-change(232, verifikasi retrospektif 0 pelanggaran). **Accessibility Micro-Polish ✅ TUNTAS 9/9 (Batch 234-242)**: TalkBack semantics(234, 1 fix Row.clickable+nested RadioButton → Modifier.selectable role=RadioButton, 2 titik NowPlayingScreen.kt)/content descriptions(235, 1 fix icon peredam volume static desc → null, konsisten decorative+text-sibling)/semantic role(236, 1 fix ThemeOptionCard clickable → selectable role=RadioButton, SettingsScreen.kt)/focus order(237, 1 fix 8 field SongInfoEditSheet.kt tanpa imeAction Next chain → ditambah)/minimum touch target(238, 1 fix StarRatingRow IconButton 32dp→default 48dp, NowPlayingScreen.kt)/text scaling(239, 0 bug — semua fontSize/lineHeight sp, 0 dp-based, 0 configChanges skip font-scale, 0 fixed-height clip)/contrast(240, 0 bug — ResultBanner Tinted/Bare by-design, onPrimary/onTertiary luminance-checked manual, disabled alpha WCAG-exempt)/color-only info(241, 1 fix PlaylistSongRow "sedang diputar" cuma warna+bold → ditambah badge GraphicEq, samakan QueueRow/LibraryScreen SongRow)/no-behavior-change(242, verifikasi retrospektif 0 pelanggaran, 6 fix kode 234-241 semua semantics/visual-only). Kategori 13-14 belum mulai. |

---

## 🔴 PRIORITAS UTAMA

### 1. String & Wording Consistency
- [ ] Audit seluruh user-facing hardcoded strings.
- [ ] Centralize UI strings ke resources.
- [ ] Samakan istilah tombol, label, dialog, snackbar, empty state, dan error state.
- [ ] Hindari wording yang berubah-ubah untuk action yang sama.
- [ ] Pastikan capitalization dan punctuation konsisten.
- [ ] Jangan mengubah behavior; hanya presentation/text.

### 2. Spacing & Sizing Consistency
- [ ] Audit seluruh one-off `.dp`.
- [ ] Terapkan spacing token yang konsisten.
- [ ] Samakan horizontal screen padding.
- [ ] Samakan vertical spacing antar section.
- [ ] Samakan gap icon ↔ text.
- [ ] Samakan ukuran control yang setara.
- [ ] Hindari layout yang terlihat terlalu padat atau terlalu renggang.
- [ ] Jangan mengubah struktur screen kecuali diperlukan untuk alignment UI.

### 3. Typography Hierarchy
- [ ] Audit title/subtitle/body/label/caption.
- [ ] Konsistenkan font size.
- [ ] Konsistenkan font weight.
- [ ] Konsistenkan line height.
- [ ] Pastikan truncation/ellipsis tidak merusak informasi penting.
- [ ] Pastikan long title/artist/album tetap rapi.
- [ ] Jangan mengubah data atau logic.

### 4. Interactive States
- [ ] Pastikan pressed state terlihat konsisten.
- [ ] Pastikan disabled state jelas.
- [ ] Pastikan selected/active state konsisten.
- [ ] Pastikan loading state tidak terasa abrupt.
- [ ] Pastikan empty state memiliki hierarchy yang jelas.
- [ ] Pastikan error state mudah dipahami.
- [ ] Pastikan success/confirmation feedback konsisten.
- [ ] Hindari feedback visual yang berbeda untuk action yang sama.

### 5. Touch Target & Micro Interaction
- [ ] Audit seluruh clickable icon/button.
- [ ] Pastikan target sentuh nyaman.
- [ ] Pastikan area klik tidak terlalu kecil.
- [ ] Pastikan icon visual tidak harus sama dengan area hit target.
- [ ] Hindari accidental overlap antar clickable element.
- [ ] Pastikan ripple/pressed feedback tidak terpotong container.
- [ ] Jangan mengubah action yang dilakukan oleh control.

---

## 🟠 NOW PLAYING — FINAL MICRO-POLISH

- [x] Audit alignment artwork, title, artist, dan controls. **✅ 0 bug (Batch 169)** — 1 `Column` root `CenterHorizontally` membungkus semua elemen, alignment konsisten by-construction. Title `basicMarquee()` vs artist `TextOverflow.Ellipsis` = pola umum player disengaja, bukan inkonsistensi.
- [x] Audit spacing antar playback controls. **✅ 1 bug fix (Batch 170)** — Row 5 tombol sebelumnya tanpa `fillMaxWidth()`/`horizontalArrangement` (cluster rapat), ditambahkan `.fillMaxWidth()` + `Arrangement.SpaceEvenly`.
- [x] Audit slider height/touch area. **✅ 0 bug (Batch 171)** — input sentuh `Slider` Material3 standar (accessible touch-target bawaan) ditumpuk atas `WaveformSeekBar` visual-only, dibungkus `Box(height=48dp)` = pas minimum touch target.
- [x] Pastikan progress/current time/remaining time mudah dibaca. **✅ 0 bug (Batch 172)** — `bodySmall` + `colorScheme.secondary` kedua sisi, treatment umum player, mode Audiobook konsisten style.
- [x] Audit volume/secondary controls. **✅ 0 bug baru (Batch 173)** — slider peredam + 2 badge gesture brightness/volume diperiksa; 1 asimetri label ditemukan sudah disengaja & terdokumentasi (disambiguasi volume-sistem vs peredam-internal dari batch lampau).
- [x] Audit bottom sheet/modal transition. **✅ 0 bug (Batch 174)** — semua 5 `ModalBottomSheet` reachable dari Now Playing pakai `rememberModalBottomSheetState(skipPartiallyExpanded=true)` identik, 0 custom transition menyimpang.
- [x] Pastikan selected/repeat/shuffle states mudah dibedakan. **✅ 0 bug (Batch 175)** — tint toggle (`animatedAccent` aktif / `colorScheme.secondary` nonaktif), pola SAMA dgn favorite-icon (`LibraryScreen.kt`) & rating-star (`SmartPlaylistScreen.kt`) app-wide. `RepeatOne` vs `Repeat` icon-swap jadi pembeda tambahan khusus repeat.
- [x] Pastikan long title/artist tidak menyebabkan layout shift. **✅ 0 bug (Batch 176)** — `maxLines=1` di kedua Text (judul `basicMarquee()`, artist `TextOverflow.Ellipsis`), tinggi baris konstan terlepas panjang teks; parent `Column` lebar tetap (`fillMaxSize()+padding(20dp)`), sibling `Spacer` semua fixed-dp — tidak ada elemen yang bisa reflow akibat panjang teks.
- [x] Pastikan artwork loading/error/empty state konsisten. **✅ 0 bug (Batch 177)** — `AlbumArt` (`Utils.kt`) 1 komponen shared dipakai di SEMUA 7 titik (Hero/List/MiniPlayer/Grid) termasuk hero art ini, jadi konsisten by-construction bukan kebetulan. `error` & artwork-`null` render `AlbumArtFallbackIcon` yang SAMA (ikon `MusicNote`), `loading` blank (`surfaceVariant` polos) — wajar utk app 100% offline (URI lokal, dekode nyaris instan, shimmer justru berisiko flicker sub-frame).
- [x] Pastikan semua controls memiliki feedback visual. **✅ 1 bug fix (Batch 178)** — swipe album art (next/prev) sebelumnya 0 feedback visual selama drag (cuma haptic sekali di dragEnd), beda dari gesture brightness/volume di sekitarnya yang sudah punya badge live. Fix: art ikut bergeser mengikuti jari (`Animatable` translationX, damped 0.5x, clamp ±48dp) lalu spring balik ke tengah saat dilepas — threshold/logic swipe (120px) tidak diubah, murni layer visual.
- [x] **Jangan mengubah playback logic.** **✅ Terverifikasi (Batch 179)** — retrospektif Batch 169-178: `PlayerViewModel.kt`/package `playback/` 0 disentuh sepanjang kategori ini (dikonfirmasi grep). Satu-satunya perubahan kode (Batch 178, `AlbumArtHero`) murni visual — `onSwipeNext()`/`onSwipePrevious()` tetap memanggil `onNext`/`onPrevious` yang sama persis, threshold 120px tidak diubah.

---

## 🟠 LIBRARY / SONG LIST — ✅ TUNTAS 11/11 (Batch 180-190)

- [x] Samakan tinggi row. **✅ 0 bug (Batch 180)** — `SongRow` (art 48dp + padding vertical 8dp = tinggi tetap) 1 komponen shared dipakai identik di 5 titik (tab Lagu, Favorit, drill-down Artis, drill-down Folder, hasil Pencarian), konsisten by-construction. Group-header row (`ListItem` navigasi kategori) & `AlbumGridView` (grid) sengaja beda paradigma UI dari song-row, bukan gap.
- [x] Samakan thumbnail/artwork size. **✅ 0 bug (Batch 181)** — 5 file pemakai `AlbumArt` diperiksa, size mengelompok jadi 4 kategori peran UI, masing-masing konsisten internal: list-row scrollable (`SongRow` 48dp, 1 komponen shared 5 titik, Batch 180), persistent compact bar (`MiniPlayerBar` 44dp, konteks tunggal, budget tinggi bar terbatas), featured single-card (`ContinueListeningCard` 56dp, konteks tunggal, Card elevated bukan list-row biasa), carousel/grid fill-container (`HomeSongCard` 120dp width-tetap & `AlbumGridView` `fillMaxWidth+aspectRatio(1f)` — keduanya prinsip sama "art = lebar card", angka beda krn lebar carousel-item vs kolom-grid memang beda). Perbedaan LINTAS kategori = peran UI berbeda disengaja, bukan inkonsistensi DALAM kategori yang sama.
- [x] Samakan spacing antar metadata. **✅ 0 bug (Batch 182)** — pola 3-segmen konsisten di 4 komponen (`SongRow`/`QueueRow`/`ContinueListeningCard`/`MiniPlayerBar`): art↔text `Spacer(width=12dp)`, title↔artist `0dp` eksplisit (mengandalkan line-height Text bawaan, disengaja sama di semua 4, bukan lupa dikasih Spacer), text↔trailing-action `Spacer(width=8dp)`.
- [x] Audit title/artist truncation. **✅ 1 bug fix (Batch 183)** — 3/4 komponen (`SongRow`/`ContinueListeningCard`/`MiniPlayerBar`) sudah pakai `basicMarquee()` di title (scroll penuh, bukan ellipsis diam), artist tetap `TextOverflow.Ellipsis`. `QueueRow` (`QueueSheet.kt`) ketinggalan — title-nya masih ellipsis diam, satu-satunya dari 4. Disamakan ke `basicMarquee()`.
- [x] Pastikan favorite/overflow/action icon memiliki hit target yang layak. **✅ 0 bug (Batch 184)** — Semua `IconButton`/`FilledIconButton` di 4 komponen (favorit `SongRow`, moveUp/moveDown/remove `QueueRow`, play-pause `MiniPlayerBar`) baik pakai ukuran default (48dp) maupun eksplisit lebih kecil (`MiniPlayerBar` play-pause `.size(40.dp)`) — Material3 secara otomatis menegakkan touch target minimum 48dp lewat `minimumInteractiveComponentSize()` internal (dikonfirmasi 0 override `LocalMinimumInteractiveComponentEnforcement` di seluruh file terkait), jadi `.size(40.dp)` cuma memperkecil tampilan visual, bukan area sentuh.
- [x] Audit selected/current-playing indicator. **✅ 0 bug — verifikasi retrospektif (Batch 185)** — sudah dikerjakan Batch 163/164 sebelum kategori ini resmi dimulai (dicatat sbg peringatan di Batch 179). `SongRow` & `QueueRow` sama-sama 3 lapis identik (bg primary 12% alpha + ikon `GraphicEq` + title bold/primary), `ContinueListeningCard` (lagu TERAKHIR diputar, bukan status live) & `MiniPlayerBar` (bar itu sendiri = indikator "sedang diputar") sengaja tanpa lapisan tambahan — beda semantik, bukan gap.
- [x] Audit empty library state. **✅ 0 bug (Batch 186)** — 1 composable `EmptyState` (icon+title+subtitle+CTA opsional) dipakai ulang untuk semua 4 skenario kosong di `LibraryScreen.kt`: perpustakaan kosong total (dengan CTA "Pindai Ulang"), favorit kosong (tanpa CTA, hint tap-hati), hasil filter tab kosong & hasil pencarian kosong (tanpa CTA, "Coba kata kunci lain"/"Tidak ditemukan"). CTA cuma muncul di skenario yang genuinely actionable (rescan), sengaja tidak ada di skenario lain — bukan lupa, konsisten disengaja.
- [x] Audit loading state. **✅ 1 bug fix (Batch 187)** — `ShimmerRow` (skeleton loading `LibraryScreen.kt`) dibuat meniru dimensi `SongRow` asli (art 48dp ✓ match), tapi `vertical padding`-nya 10dp vs `SongRow` sungguhan 8dp — beda 2dp/baris × 8 baris shimmer = shift 16dp pas transisi loading→loaded. Disamakan ke 8dp.
- [x] Audit search result state. **✅ 0 bug (Batch 188)** — `SearchResultsView` (`LibraryScreen.kt`) 1 komponen menangani ketiganya: hasil kosong reuse `EmptyState` yang sama (sudah diaudit item 7), hasil ada dikelompokkan Artis/Album/Lagu dengan `SearchSectionLabel` konsisten (typography+padding sama di ketiga grup), pencarian murni filter sinkron in-memory (`remember(songs, query)`) jadi tidak ada state loading terpisah yang perlu ditangani.
- [x] Audit list separator/divider bila digunakan. **✅ 0 bug (Batch 189)** — 5 titik divider di `LibraryScreen.kt` (`SongListView`, `GroupedListView` 2x — daftar grup & daftar lagu dalam grup, `SearchResultsView`, drill-down Album) semua `HorizontalDivider(color = surfaceVariant)` identik, dipasang setelah SETIAP item termasuk yang terakhir — konsisten. Catatan (bukan bug, di luar cakupan kategori ini): `QueueRow` (§ Playlist/Queue, belum diaudit) 0 divider sama sekali — kandidat utk kategori itu nanti, bukan kategori Library/Song List.
- [x] Hindari visual jumping ketika artwork selesai loading. **✅ 0 bug (Batch 190)** — `AlbumArt` (`Utils.kt`, 1 komponen shared dipakai di semua tempat art muncul) sudah didesain anti-jump sejak awal: `Box` ukurannya SELALU ditentukan `modifier` milik caller (fixed dp/aspectRatio, bukan derive dari ukuran gambar), diisi `background(surfaceVariant)` sebelum gambar sempat tiba, `SubcomposeAsyncImage` pakai `matchParentSize()` (tidak pernah mengubah bounds Box) + `loading = {}` (kosong disengaja, dikonfirmasi komentar KDoc existing — supaya lagu yang PUNYA art tidak sempat kelip ikon fallback dulu). Layout box tidak pernah berubah ukuran dari sebelum→sesudah gambar decode, jadi tidak ada jump yang mungkin terjadi by-construction.

---

## 🟠 PLAYLIST / QUEUE

- [x] Konsistenkan row height dan spacing. **✅ 1 bug fix (Batch 191)** — `QueueRow` (`QueueSheet.kt`) horizontal padding 12dp, outlier dari konvensi 20dp yang dipakai konsisten di SELURUH app (`PlaylistSongRow`, `SongRow`, `ShimmerRow`, dst.). Disamakan ke 20dp. Vertical padding (8dp) & tinggi efektif row sudah konsisten dari awal, tidak diubah.
- [ ] Pastikan drag/reorder affordance jelas jika tersedia. **✅ 1 bug fix (Batch 192)** —
  `QueueRow` drag handle 40dp→48dp (standar Material touch target, samakan dgn 3 `IconButton`
  lain di baris sama). `PlaylistScreen` tidak punya drag handle sama sekali — bukan gap
  (checklist "jika tersedia", di situ memang tidak tersedia; nambah = ubah behavior).
- [ ] Audit selected/current item state. **🟡 1 observasi (Batch 193), belum dieksekusi** —
  `QueueSheet` sudah highlight (background+bold primary). `PlaylistScreen` 0 highlight sama
  sekali — butuh state baru lintas-file (`LibraryScreen`→`PlaylistTabView`→row), bukan pure
  styling, perlu keputusan eksplisit user dulu (mirip kasus `EmptyState` tapi lebih dalam).
- [ ] Audit remove/delete affordance. **✅ 0 bug (Batch 194)** — ikon/deskripsi/touch-target
  konsisten `QueueSheet`+`PlaylistScreen`. Warna `secondary` (bukan error) sudah BENAR sesuai
  konvensi app (error = permanen/hapus dari perangkat; hapus dari antrean/playlist = reversibel).
  `canRemove` beda logic keduanya juga BENAR (queue tidak boleh kosong saat ada lagu diputar,
  playlist boleh kosong total).
- [ ] Pastikan destructive action memiliki visual hierarchy yang tepat. **✅ 1 bug fix (Batch
  195)** — "Hapus playlist" dulu langsung eksekusi 0 konfirmasi + ikon tanpa warna error. Fix:
  `AlertDialog` konfirmasi ditambah (tiru pola `LibraryScreen` "Hapus dari Perangkat?"), ikon
  `tint = error`. Belum diverifikasi visual.
- [ ] Audit empty queue/playlist state.
- [ ] Pastikan queue controls tidak terlalu padat.
- [ ] Jangan mengubah queue behavior.

---

## 🟠 SETTINGS

- [x] Konsistenkan grouping antar section. **✅ 1 bug fix (Batch 215)** — 4 baris tool
  (Statistik Dengar/Cadangkan & Pulihkan/Deteksi Duplikat/Vault) sebelumnya masing-masing
  dibungkus `HorizontalDivider` sendiri TANPA title — tampak seperti 4 section kosong nama,
  beda dari pola section lain di file ini (mis. "Perilaku Pemutaran") yang selalu 1 title
  menaungi beberapa item terkait. Disatukan 1 title "Alat & Utilitas" menaungi ke-4-nya, 3
  divider antar-item dibuang (diganti `Spacer(4.dp)`). 0 logic/navigasi/aksi berubah.
- [x] Konsistenkan title/subtitle row. **✅ 1 bug fix (Batch 216)** — 7 baris navigasi
  icon+title (4 "Alat & Utilitas" + "Cek Signature APK"/"Log Diagnostik"/"Cek Update") diaudit:
  4 punya title+subtitle sendiri, 2 (Signature/Diagnostik) title-only tapi dinaungi 1 deskripsi
  section bersama "Alat Developer" (konteks tetap ada, bukan gap), 1 ("Cek Update") title-only
  TANPA konteks apa pun di dekatnya — genuinely berdiri sendiri. Subtitle ditambah ke "Cek
  Update" saja, 2 baris developer tool sengaja tidak disentuh (pola section-level description
  sudah valid & konsisten dengan "Tema"/"Perilaku Pemutaran").
- [x] Samakan spacing antar setting. **✅ 0 bug (Batch 217)** — audit menyeluruh pasca
  restrukturisasi Batch 215-216. **Transisi antar-section** (4 titik: Perilaku Pemutaran/Alat &
  Utilitas/Lanjutan/Tentang Aplikasi): `Spacer(12dp)→Divider→Spacer(20dp)` identik di semua 4,
  konsisten. **Title→konten pertama dalam section**: `Spacer(8dp)` identik di 3 titik (Perilaku
  Pemutaran, Alat & Utilitas, Tentang Aplikasi). **Antar-item DALAM 1 section** — 2 pola beda
  angka TAPI beda alasan fungsional, bukan inkonsistensi: switch-row (Perilaku Pemutaran)
  `Spacer(12dp)` statis, row sendiri 0 padding vertikal (target sentuh = `Switch`-nya saja, bukan
  seluruh row); nav-row (Alat & Utilitas, Alat Developer) `Spacer(4dp)` + `padding(vertical=8dp)`
  bawaan row = ~20dp gap efektif (row SELURUHNYA `.clickable{}`, padding vertikal itu bagian dari
  target sentuh, bukan estetika murni) — 2 spesies row beda afinitas interaksi, pola sama
  precedent Batch 181 (ukuran beda krn peran UI beda, bukan gap).
- [x] Audit switch/toggle alignment. **✅ 0 bug (Batch 218)** — 8 titik `Switch(` di
  `SettingsScreen.kt` dicek: semua pakai pola identik `Row(verticalAlignment =
  Alignment.CenterVertically)` + `Column(weight(1f))` title di kiri + `Switch` polos (0
  modifier ukuran/offset custom) di kanan. 0 titik pakai `Alignment.Top/Bottom` atau padding/
  size manual yang bisa geser switch dari center. Beda `padding` pembungkus luar antar titik
  murni struktural (langsung di Row vs diwarisi Column orang tua), tidak berpengaruh ke
  alignment vertikal internal.
- [x] Audit navigation affordance. **✅ 0 bug (Batch 219)** — 7 baris navigasi (Statistik/
  Backup/Duplikat/Vault/Signature/Diagnostik/Cek Update) 0 punya chevron trailing, TAPI di-grep
  seluruh `ui/` folder: `ChevronRight`/`Arrow...Forward`/`NavigateNext` 0 hasil di mana pun —
  app-wide 0% pernah pakai pola chevron. Affordance klik konsisten pakai ripple `.clickable{}`
  full-row + icon prefix, bukan chevron. Row "Lanjutan" (`ExpandMore`/`Less`) bukan pembanding
  valid — itu representasi STATE expand/collapse, bukan affordance navigasi.
- [x] Pastikan destructive setting terlihat berbeda. **✅ 1 bug fix (Batch 220)** — toggle OFF
  "Kunci Aplikasi (PIN)" ternyata memicu `AppLockStore.disableLock()` yang **hapus permanen** PIN
  hash+salt dari `SharedPreferences` (harus dibuat ulang dari nol kalau diaktifkan lagi), tapi
  dulu dieksekusi langsung dari toggle 0 konfirmasi 0 warna error — pola sama bug Batch 195. Fix:
  `AlertDialog` konfirmasi ditambah + tombol "Nonaktifkan" & ikon `LockOpen` di-tint
  `colorScheme.error`. Switch tetap terikat state asli parent (0 flicker saat Batal).
- [x] Pastikan disabled setting terlihat jelas. **✅ 0 bug (Batch 221)** — cuma 1 titik
  `enabled = ` di seluruh file: `Switch` "Mode Gelap" (`enabled = !followSystem`). 0 custom
  `SwitchDefaults.colors`, murni default M3 yg otomatis dim saat disabled + subtitle SUDAH
  eksplisit ganti teks jadi "Nonaktif — mengikuti pengaturan sistem". Dipertimbangkan tambah
  alpha manual ke title, tapi di-grep app-wide: 0 precedent pola dim-title — batal, hindari
  elemen baru yg tidak konsisten dgn sisa app.
- [x] Kurangi visual density tanpa menghilangkan informasi. **✅ 0 bug (Batch 222)** — spacing
  pakai skala Material konsisten (4/8/12/20dp, diverifikasi struktural Batch 217). Subtitle
  terpanjang (Mini Player Mengambang/Lewati Keheningan) berisi info fungsional wajib (syarat
  izin, batasan teknis, efek samping) — memangkas demi baris lebih pendek justru melanggar
  syarat item ini sendiri ("tanpa menghilangkan informasi"). 0 elemen bertumpuk/Row padat
  ditemukan; kepadatan murni konsekuensi jumlah info yang memang perlu, bukan tata letak boros.
- [x] Jangan mengubah fungsi setting. **✅ 0 bug — verifikasi akhir (Batch 223)** — semua 9
  callback `SettingsScreen(...)` di `MainActivity.kt` tetap terpasang ke fungsi `PlayerViewModel`
  yang sama persis, 0 baris `MainActivity.kt` tersentuh sepanjang Batch 215-222. Batch 220
  (satu-satunya yg sentuh alur eksekusi) cuma tambah safety-gate `AlertDialog` di depan
  `onDisableLock()` — fungsi & efeknya identik, bukan perubahan behavior. **🟠 SETTINGS TUNTAS
  9/9 (Batch 215-223).**

---

## 🟡 ICONOGRAPHY

- [x] Audit ukuran icon. **✅ 1 bug fix (Batch 224)** — 113 titik `Icon(` app-wide dicek: 104
  default 24dp (baseline), 9 custom size. 8/9 justified (TextButton/FilterChip icon disandingkan
  teks = beda konvensi, `LockScreen` disamakan sadar sejak Batch 147, `FeatureHintBanner`
  hit-target vs visual size didokumentasikan sejak Batch 141). **1 bug nyata**: Play/Pause
  `NowPlayingScreen.kt` (kontainer terbesar 68dp) glyph cuma 34dp — LEBIH KECIL dari Skip
  Prev/Next yang mengapitnya (36dp), membalik hierarki bobot visual (Shuffle/Repeat 24 < Skip 36
  < harusnya Play/Pause terbesar). Fix: 34dp→40dp, hierarki 24<36<40 dipulihkan. **✅
  Terverifikasi visual (Batch 225, screenshot user)**.
- [x] Audit optical alignment. **✅ 1 bug fix (Batch 226-227), TUNTAS 4/4 titik** — grep semua
  `Icons.Default.PlayArrow` app-wide: 4 titik (`NowPlayingScreen.kt`, `MiniPlayerBar.kt`,
  `LyricsSheet.kt`, `HomeScreen.kt` "Lanjutkan"). Glyph segitiga PlayArrow punya bobot visual
  condong kiri dalam bounding box (beda dari Pause yang simetris kiri-kanan) — pas
  `AnimatedContent` switch Play↔Pause di posisi identik, PlayArrow kelihatan "kegeser kiri" dari
  titik pusat tombol. **Fix**: offset +1dp kanan. Batch 226: 3 titik toggle Play/Pause
  (`NowPlayingScreen.kt`/`MiniPlayerBar.kt`/`LyricsSheet.kt`), kondisional HANYA saat PlayArrow.
  Batch 227 (penutup): `HomeScreen.kt` — tombol ini selalu PlayArrow (bukan toggle), offset
  diterapkan tetap. Belum diverifikasi visual di device asli.
- [x] Samakan visual weight icon sejenis. **✅ 1 bug fix (Batch 228)** — grep pasangan
  ikon sukses/gagal `ResultBanner(...)` (4 call site app-wide): `SignatureMatcherSheet.kt` &
  `UpdateCheckSheet.kt` pakai `CheckCircle` (solid) vs `Error` (solid) — bobot konsisten.
  **Bug**: `BackupRestoreSheet.kt` & `DiagnosticLogSheet.kt` pakai `CheckCircle` (solid) vs
  `ErrorOutline` (garis tipis) — 1 pasangan sukses/gagal yg sama tapi bobot visual beda dgn
  2 site lain. Fix: `ErrorOutline` → `Error` di kedua file, samakan dgn pola referensi
  `SignatureMatcherSheet.kt`. Belum diverifikasi visual di device asli.
- [x] Pastikan action icon dapat dibedakan dari decorative icon. **✅ 1 bug fix (Batch 229)** —
  audit tint icon decorative (badge status, tanpa onClick) app-wide. Konvensi codebase: icon
  decorative pakai `colorScheme.secondary` (mis. `MusicNote` fallback album art, drag-handle
  `QueueSheet.kt`), `primary` reserved utk icon actionable/tombol. **Bug**: badge "Sedang
  diputar" (`GraphicEq`, murni status 0 onClick) di `QueueSheet.kt` & `LibraryScreen.kt` pakai
  `primary` — ambigu seolah bisa di-tap, padahal cuma indikator. Fix: `primary` → `secondary`
  di kedua titik, samakan dgn baris drag-handle persis di atasnya (`QueueSheet.kt`) yang sudah
  benar. Belum diverifikasi visual di device asli.
- [x] `contentDescription = null` hanya untuk icon yang benar-benar decorative. **✅ 0 bug
  (Batch 230)** — grep 69 titik `contentDescription = null` app-wide. Semua genuinely
  decorative: (a) icon+Text sibling dlm `Button`/`TextButton`/`NavigationBarItem`/`AlertDialog`/
  `ListItem leadingContent`/`leadingIcon` (TalkBack baca label teksnya, null menghindari
  duplikasi announcement — pola benar), atau (b) badge status murni tanpa onClick. 0 titik
  `IconButton`/`clickable` icon-only (tanpa Text pendamping) ditemukan pakai null — dicek
  window ±12 baris tiap `IconButton(` app-wide, semua actionable icon-only sudah punya
  `contentDescription` deskriptif.
- [x] Semua actionable icon harus memiliki semantic/content label yang sesuai. **✅ 1 bug fix
  (Batch 231)** — audit label toggle playback mode di `NowPlayingScreen.kt`. **Bug**: tombol
  Shuffle & Repeat (3-state OFF→ALL→ONE) pakai label statis ("Acak"/"Ulangi") — status ON/OFF/
  mode aktif cuma dibedakan lewat `tint` (animatedAccent vs secondary), tidak terbaca TalkBack.
  Repeat lebih parah: glyph icon OFF vs ALL identik (`Icons.Default.Repeat`), jadi user
  screen-reader sama sekali tidak bisa tahu status OFF vs ALL aktif. Fix: label ikut state
  ("Acak: aktif/nonaktif", "Ulangi: mati/semua lagu/satu lagu"). Belum diverifikasi TalkBack
  di device asli.
- [x] Jangan mengganti icon hanya demi estetika jika mengubah affordance. **✅ Verifikasi
  retrospektif (Batch 232), TUNTAS 7/7 kategori ICONOGRAPHY** — ditelusuri ulang 4 fix kode
  Batch 224-231, tidak ada satupun murni kosmetik yg mengubah/mengaburkan affordance: (1)
  Batch 224 ukuran 34dp→40dp = size saja, glyph & makna tetap. (2) Batch 226-227 offset +1dp =
  posisi mikro, bukan ganti glyph. (3) Batch 228 `ErrorOutline`→`Error` = ganti glyph TAPI
  dasarnya menyamakan bobot dgn pasangan `CheckCircle` solid yg sudah ada (konsistensi makna
  status gagal, bukan estetika semata). (4) Batch 229 tint `primary`→`secondary` = warna saja,
  affordance malah DIPERJELAS (bedakan decorative dari actionable, bukan dikaburkan). (5) Batch
  231 contentDescription = teks saja, 0 icon visual berubah. **Hasil: 0 pelanggaran**, 0 kode
  disentuh batch ini.

---

## 🟡 ACCESSIBILITY MICRO-POLISH

- [x] Audit TalkBack semantics pada interactive control. (Batch 234: 1 fix — nested
      Row.clickable+RadioButton di `NowPlayingScreen.kt` → `Modifier.selectable(role =
      Role.RadioButton)` + `RadioButton(onClick = null)`, 2 titik)
- [x] Audit content descriptions. (Batch 235: 1 fix — icon peredam volume `NowPlayingScreen.kt`
      static desc → `null`, konsisten decorative+text-sibling)
- [x] Audit semantic role. (Batch 236: 1 fix — `ThemeOptionCard` clickable → selectable
      role=RadioButton, `SettingsScreen.kt`)
- [x] Audit focus order. (Batch 237: 1 fix — 8 field `SongInfoEditSheet.kt` tanpa imeAction
      Next chain → ditambah `focusManager.moveFocus`, No.Disc jadi `Done`)
- [x] Audit minimum touch target. (Batch 238: 1 fix — `StarRatingRow` `IconButton` override
      32dp → hapus, default 48dp, `NowPlayingScreen.kt`)
- [x] Audit text scaling. (Batch 239: 0 bug — semua fontSize/lineHeight `.sp`, widget XML `sp`,
      manifest 0 configChanges skip font-scale, 0 fixed-height container clip Text)
- [x] Audit contrast. (Batch 240: 0 bug — ResultBanner Tinted/Bare by-design, onPrimary/
      onTertiary luminance-checked manual per tema, disabled alpha WCAG-exempt)
- [x] Pastikan informasi penting tidak hanya dibedakan melalui warna. (Batch 241: 1 fix —
      `PlaylistSongRow` "sedang diputar" cuma warna+bold → tambah badge `GraphicEq`, samakan
      `QueueRow`/`LibraryScreen SongRow`)
- [x] Jangan mengubah behavior aplikasi. (Batch 242: 0 bug — audit retrospektif 6 fix kode
      234-241: 234/236 handler pindah ke parent `.selectable` tapi `onClick` sama persis
      (`RadioButton(onClick = null)`, semantics-only naik level, bukan logic baru), 237 cuma
      nambah navigasi fokus (imeAction), 238 cuma ukuran touch target, 241 cuma tambah icon
      visual 0 onClick baru. **Hasil: 0 pelanggaran**, 0 kode disentuh.)

---

## 🟡 MOTION & TRANSITION

- [ ] Audit transition duration agar konsisten.
- [ ] Hindari animation yang terlalu lambat.
- [ ] Hindari abrupt layout change.
- [ ] Pastikan animation tidak menghalangi interaction.
- [ ] Pastikan loading/content transition terasa natural.
- [ ] Hormati reduced-motion/accessibility preference bila relevan.
- [ ] **Dilarang mengubah timing/logic playback.**

---

## 🟡 RESPONSIVE / ADAPTIVE MICRO-POLISH

Validasi:
- [ ] Small phone.
- [ ] Large phone.
- [ ] Landscape.
- [ ] Large window/tablet bila target mendukung.
- [ ] Dynamic font scaling.
- [ ] Long text.
- [ ] Empty state.
- [ ] Error state.

Audit:
- [ ] Tidak ada clipping.
- [ ] Tidak ada overlap.
- [ ] Tidak ada unnecessary horizontal scrolling.
- [ ] Alignment tetap konsisten.
- [ ] Control tetap mudah disentuh.

---

## 🟡 COLOR / SURFACE CONSISTENCY

- [ ] Konsistenkan background/surface hierarchy.
- [ ] Konsistenkan primary/secondary text contrast.
- [ ] Konsistenkan disabled opacity.
- [ ] Konsistenkan selected/active surface.
- [ ] Konsistenkan divider/border treatment.
- [ ] Hindari warna yang dipakai untuk dua makna berbeda.
- [ ] Jangan mengubah branding/theme secara drastis.
- [ ] Tidak ada perubahan logic.

---

## 🟡 COMPONENT CONSISTENCY

Audit komponen yang berulang:
- [ ] Buttons.
- [ ] Icon buttons.
- [ ] List rows.
- [ ] Cards.
- [ ] Chips.
- [ ] Dialogs.
- [ ] Bottom sheets.
- [ ] Snackbar/toast.
- [ ] Search field.
- [ ] Sliders.
- [ ] Switches.
- [ ] Dropdown/menus.

Target:
- [ ] Satu pola visual untuk fungsi yang sama.
- [ ] Satu pola spacing.
- [ ] Satu pola typography.
- [ ] Satu pola state.
- [ ] Satu pola interaction feedback.

---

# 🚫 HARD NO-GO ZONE

Jangan melakukan perubahan berikut dalam task Micro UI/UX:

- [ ] Playback engine.
- [ ] Crossfade algorithm.
- [ ] Queue algorithm.
- [ ] Shuffle/repeat behavior.
- [ ] MediaStore scanning.
- [ ] SAF implementation.
- [ ] Database schema.
- [ ] Repository logic.
- [ ] Playback persistence.
- [ ] Sleep timer behavior.
- [ ] Navigation architecture.
- [ ] Permission architecture.
- [ ] Audio focus behavior.
- [ ] MediaSession behavior.
- [ ] Background playback.
- [ ] Feature removal.
- [ ] Feature expansion.

**Pengecualian hanya untuk bug UI yang urgent dan terbukti langsung dari presentation layer.**

---

# FINAL EXECUTION ORDER

1. Strings/writing consistency
2. Spacing tokens
3. Typography
4. Touch targets
5. Interactive states
6. Icon semantics
7. Now Playing polish
8. Library/list polish
9. Queue/playlist polish
10. Settings polish
11. Accessibility micro-pass
12. Motion/transition pass
13. Responsive validation
14. Final visual consistency pass

---

# FINAL VERDICT

**Target:** `100% Micro UI/UX polished`

**Strategi:**  
`Presentation-only → no unnecessary logic refactor → no feature expansion → no behavioral changes`

Core application behavior yang sudah benar harus dianggap **frozen** selama fase ini.

Setiap perubahan harus menjawab:

> **“Apakah ini memperbaiki UI/UX secara langsung tanpa mengubah behavior aplikasi?”**

Jika jawabannya **tidak**, jangan dilakukan.
