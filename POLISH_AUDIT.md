# AudioPlayer — Final Micro-Polish Audit (living checklist)

Ditanam Batch 253 (permintaan user, adaptasi ke referensi konkret repo). Dokumen ini backlog aktif
untuk batch-batch berikutnya — bukan sekali kerja, tapi checklist yang diisi/dicoret per batch
(descending: temuan/perbaikan terbaru di atas tiap seksi, bukan urutan seksi 1-5 di bawah).

## Status
- Estimasi: ~96-98% polished. Fondasi SUDAH KUAT.
- **Refactor besar: DILARANG.** Logic playback/data/SAF/database: **FREEZE** — 5 area di bawah ini
  MURNI visual/motion/consistency, 0 boleh nyentuh behavior.
- 1 sub-item = 1 batch (Strict Micro-Batching tetap berlaku, dokumen ini bukan pengecualian).

## 🔴 Gap yang masih layak dikerjakan

### 1. Motion & Transition
- [x] `LibraryScreen.kt:1345` — `tween(1100, easing = LinearEasing)`. **Diverifikasi Batch 254:
  BUKAN micro-feedback interaktif** — ini `ShimmerBrush()`, animasi loading-skeleton
  `infiniteRepeatable` (shimmer effect saat list masih loading), bukan animasi respons tap/klik.
  1100ms per siklus shimmer masih dalam rentang wajar standar (umumnya 1000-1500ms di
  Material/skeleton pattern lain). **0 bug, STOP — durasi tidak diubah.**
- [x] `MiniPlayerBar.kt:68` — `tween(700)` accent transition. **Diverifikasi Batch 255.**
- [x] `NowPlayingScreen.kt:301` — `tween(700)` accent transition (sama pola dgn MiniPlayerBar,
  audit bareng biar konsisten kalau salah satu diubah). **Diverifikasi Batch 255: KEDUANYA sudah
  konsisten satu sama lain** (700ms, sama-sama `animateColorAsState` utk cross-fade warna aksen
  dominan album-art saat lagu berganti — bukan respons tap/klik, jadi tolok ukurnya beda dari
  micro-feedback tombol). 700ms wajar utk ambient color wash (rentang umum 300-800ms utk color
  cross-fade non-interaktif; lebih cepat drpd ini malah terasa "kedut" tiap ganti lagu). Trigger-nya
  cuma pas song berganti (bukan repeated rapid interaction), jadi tidak ada risiko numpuk/berat.
  **0 bug, STOP — durasi tidak diubah.**
- [x] Audit semua duration/easing lain (`grep -rn "tween(\|animateFloatAsState\|animateColorAsState"
  app/src/main/java/com/rudi/audioplayer/ui`) biar 1 sistem, bukan cuma 3 titik di atas.
  **Diverifikasi+diperbaiki Batch 256.** Cakupan cuma 4 file (`LibraryScreen.kt`,
  `MiniPlayerBar.kt`, `NowPlayingScreen.kt`, `Utils.kt`) — tidak menyebar ke 22 file `ui/*.kt`
  lain. Temuan KONKRET: `bouncyPress()` (`Utils.kt`, shared modifier tap-feedback dipakai semua
  tappable control) & entrance spring (`NowPlayingScreen.kt` ~baris 410) sama-sama pakai
  `spring(dampingRatio=MediumBouncy, stiffness=StiffnessLow)` eksplisit — TAPI 2 spring snap-back
  swipe-next/previous artwork (`NowPlayingScreen.kt` ~1228/1231) cuma pakai `dampingRatio=
  MediumBouncy` tanpa stiffness eksplisit (default ke Medium, beda "rasa" dari sistem bouncy yg
  sudah mapan). **Fix: tambah `stiffness=Spring.StiffnessLow` ke 2 spring itu** — sekarang 3
  animasi bouncy di app ini 1 sistem konsisten. fadeIn(150)/fadeOut(300) (2 titik, identik dgn
  dirinya sendiri) & tween(280) entrance-alpha — sudah konsisten/tunggal, tidak disentuh.
- [ ] Pastikan animasi tidak berat di repeated interaction (mis. scroll cepat trigger animasi
  berkali-kali).
- [ ] Hormati reduced-motion **kalau sudah ada infrastruktur lokalnya** — kalau belum ada, JANGAN
  bikin infra baru (itu masuk kategori "menciptakan pekerjaan baru").

### 2. Responsive / Adaptive
- [ ] Audit small phone / large phone / landscape / font-scale besar — clipping, overlap, long
  title/artist/album text, bottom controls, sheet/dialog, Now Playing artwork+control spacing.
- [ ] **Jangan ubah layout architecture** — cuma padding/constraint/overflow-handling lokal.
- Kandidat titik rawan (belum diverifikasi, sekadar dugaan berdasar nama file — WAJIB dicek visual
  betulan sebelum diedit): `NowPlayingScreen.kt` (artwork+kontrol), `LibraryScreen.kt`/song row
  (long title/artist), sheet-sheet di atas (`*Sheet.kt`, ada 13 file) utk font scale besar.

### 3. Surface / Color Consistency
- [ ] Audit background → surface → elevated surface di seluruh `ui/*.kt`.
- [ ] Samakan treatment border/divider lintas screen.
- [ ] Audit disabled/selected state lintas screen (warna sama = makna sama, cari yg dobel makna).
- [ ] **Jangan redesign theme** — ini audit konsistensi pemakaian token yang sudah ada, bukan bikin
  token baru.

### 4. Repeated Components (cross-screen visual comparison)
Project ini TIDAK punya shared component library file (`Button.kt`/`Chip.kt`/dst tidak ada) — semua
inline per-screen di 26 file `ui/*.kt`. Jadi audit ini murni **visual comparison manual**, BUKAN
ekstraksi ke shared composable (itu refactor, dilarang eksplisit).
- [ ] Button, IconButton, ListRow/song-row, Card, Chip, Dialog, BottomSheet (13 `*Sheet.kt`),
  Snackbar, Search, Slider, Switch — bandingkan visual antar screen.
- [ ] Perbaiki HANYA ketidakkonsistenan yang terbukti visual (screenshot/side-by-side), bukan dugaan.

### 5. Typography — Final Visual Check
- [ ] Line-height sudah diperbaiki (batch sebelumnya) tapi belum diverifikasi visual. Cek: title,
  subtitle, song row, dialog, sheet, long text, font scaling besar.
- [ ] **Jangan ubah hierarchy** yang sudah benar — ini verifikasi, bukan redesign.

## 🟡 Jangan disentuh (batas keras, berlaku semua sub-item di atas)
Playback engine, Queue/shuffle/repeat, MediaStore/SAF, Database, Repository, Persistence,
MediaSession, Audio focus, Background playback, Navigation architecture, Feature behavior,
Feature expansion, Refactor besar (termasuk ekstraksi shared component).

## 🟢 Sudah tidak perlu diulang
Touch target, wording utama, typography hierarchy dasar, interactive states, Now Playing,
Library/Song List, Queue/Playlist, Settings, Iconography, Accessibility micro-pass.

## Cara pakai dokumen ini (utk sesi berikutnya)
1. Ambil 1 checkbox teratas yang masih `[ ]` (urutan Motion → Responsive → Surface/Color →
   Component → Typography, sesuai FINAL VERDICT asli).
2. Kerjakan sebagai 1 batch (Strict Micro-Batching — max 3 file/1 task tetap berlaku).
3. Centang `[x]` + catat hasil singkat di baris yang sama, sisanya turun ke `CHANGELOG.md`.
4. **Kalau audit 1 area tidak nemu bug konkret → centang `[x]` dgn catatan "0 bug ditemukan,
   STOP" — JANGAN menciptakan pekerjaan baru demi 100%.** Ini instruksi eksplisit dari audit asli.
