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
| 4 | Touch Target & Micro Interaction | 🟡 **Sebagian** — bounce-press CTA utama: 4 sheet Batch 124 (Simpan/Potong & Simpan/Aktifkan Vault+Buka/Tandai Sekarang) + 4 sheet Batch 125 (Backup+Restore/Hapus Terpilih/A-B Point/Pilih APK). Belum: `EqualizerSheet` FilterChip, tombol sekunder (TextButton/IconButton) semua sheet, hit-target size audit. |
| 1 | Strings & Wording Consistency | ⬜ Belum mulai |
| 2 | Spacing & Sizing Consistency | ⬜ Belum mulai |
| 3 | Typography Hierarchy | ⬜ Belum mulai |
| 5 | Interactive States (disabled/selected/loading/error) | ⬜ Belum mulai |
| 6-14 | Now Playing s/d Component Consistency | ⬜ Belum mulai |

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

- [ ] Audit alignment artwork, title, artist, dan controls.
- [ ] Audit spacing antar playback controls.
- [ ] Audit slider height/touch area.
- [ ] Pastikan progress/current time/remaining time mudah dibaca.
- [ ] Audit volume/secondary controls.
- [ ] Audit bottom sheet/modal transition.
- [ ] Pastikan selected/repeat/shuffle states mudah dibedakan.
- [ ] Pastikan long title/artist tidak menyebabkan layout shift.
- [ ] Pastikan artwork loading/error/empty state konsisten.
- [ ] Pastikan semua controls memiliki feedback visual.
- [ ] **Jangan mengubah playback logic.**

---

## 🟠 LIBRARY / SONG LIST

- [ ] Samakan tinggi row.
- [ ] Samakan thumbnail/artwork size.
- [ ] Samakan spacing antar metadata.
- [ ] Audit title/artist truncation.
- [ ] Pastikan favorite/overflow/action icon memiliki hit target yang layak.
- [ ] Audit selected/current-playing indicator.
- [ ] Audit empty library state.
- [ ] Audit loading state.
- [ ] Audit search result state.
- [ ] Audit list separator/divider bila digunakan.
- [ ] Hindari visual jumping ketika artwork selesai loading.

---

## 🟠 PLAYLIST / QUEUE

- [ ] Konsistenkan row height dan spacing.
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
