# AudioPlayer — Micro UI/UX Gap Audit
## Target: 100% Micro UI/UX Polish

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
| 6-14 | Now Playing s/d Component Consistency | 🟡 **Now Playing ✅ TUNTAS (Batch 169-179)**. **Library/Song List berlanjut (Batch 180-182)**: tinggi row — 0 bug (180). Thumbnail/artwork size — 0 bug (181). Spacing antar metadata — 0 bug (182, pola 3-segmen 12dp/0dp/8dp konsisten 4 komponen). Kategori 8-14 belum mulai. |

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
- [ ] Pastikan drag/reorder affordance jelas jika tersedia.
- [ ] Audit selected/current item state.
- [ ] Audit remove/delete affordance.
- [ ] Pastikan destructive action memiliki visual hierarchy yang tepat.
- [ ] Audit empty queue/playlist state.
- [ ] Pastikan queue controls tidak terlalu padat.
- [ ] Jangan mengubah queue behavior.

---

## 🟠 SETTINGS

- [ ] Konsistenkan grouping antar section.
- [ ] Konsistenkan title/subtitle row.
- [ ] Samakan spacing antar setting.
- [ ] Audit switch/toggle alignment.
- [ ] Audit navigation affordance.
- [ ] Pastikan destructive setting terlihat berbeda.
- [ ] Pastikan disabled setting terlihat jelas.
- [ ] Kurangi visual density tanpa menghilangkan informasi.
- [ ] Jangan mengubah fungsi setting.

---

## 🟡 ICONOGRAPHY

- [ ] Audit ukuran icon.
- [ ] Audit optical alignment.
- [ ] Samakan visual weight icon sejenis.
- [ ] Pastikan action icon dapat dibedakan dari decorative icon.
- [ ] `contentDescription = null` hanya untuk icon yang benar-benar decorative.
- [ ] Semua actionable icon harus memiliki semantic/content label yang sesuai.
- [ ] Jangan mengganti icon hanya demi estetika jika mengubah affordance.

---

## 🟡 ACCESSIBILITY MICRO-POLISH

- [ ] Audit TalkBack semantics pada interactive control.
- [ ] Audit content descriptions.
- [ ] Audit semantic role.
- [ ] Audit focus order.
- [ ] Audit minimum touch target.
- [ ] Audit text scaling.
- [ ] Audit contrast.
- [ ] Pastikan informasi penting tidak hanya dibedakan melalui warna.
- [ ] Jangan mengubah behavior aplikasi.

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
