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
- [x] Pastikan animasi tidak berat di repeated interaction (mis. scroll cepat trigger animasi
  berkali-kali). **Diverifikasi Batch 257: 0 bug ditemukan, STOP.** Kandidat diperiksa: (a)
  `animateItemPlacement()` (4 titik, `LibraryScreen.kt`) — API ini fire cuma pas list
  MUTASI (reorder/insert/remove), BUKAN karena posisi-scroll berubah, jadi scroll cepat murni
  (tanpa list berubah) 0 memicu animasi ini sama sekali; (b) `basicMarquee()` (3 titik:
  `LibraryScreen.kt` judul lagu per-row, `MiniPlayerBar.kt`, `NowPlayingScreen.kt`) — API bawaan
  Compose ini self-gating (cuma jalan kalau teks genuinely overflow lebar box-nya, no-op kalau
  muat), jadi di list dgn banyak row yg judulnya pendek, mayoritas row 0 animasi jalan; row yg
  judulnya panjang tetap terus scroll independen dari kecepatan scroll list-nya (bukan
  retrigger tiap frame scroll). Tidak ditemukan animasi yang genuinely terikat ke scroll-offset
  event.
- [x] Hormati reduced-motion **kalau sudah ada infrastruktur lokalnya** — kalau belum ada, JANGAN
  bikin infra baru (itu masuk kategori "menciptakan pekerjaan baru"). **Diverifikasi Batch 258:
  0 infrastruktur reduced-motion di project ini** (`grep -rn "reduced.motion\|ReducedMotion\|
  animatorDurationScale\|isReduceMotionEnabled"` di seluruh `app/src/main/java/` — 0 match).
  Sesuai instruksi eksplisit dokumen ini sendiri: TIDAK dibuat baru. **N/A, STOP.**

### 2. Responsive / Adaptive
- [x] Audit small phone / large phone / landscape / font-scale besar — clipping, overlap, long
  title/artist/album text, bottom controls, sheet/dialog, Now Playing artwork+control spacing.
  **Diaudit statis Batch 259** (grep menyeluruh, BUKAN visual device — sandbox ini 0 kemampuan
  render/emulator, jujur dicatat sebagai batasan, bukan diklaim "terverifikasi visual"): (a)
  fixed-width besar yg berisiko clip — `grep ".width(NNN.dp)"` di 12 file kandidat (NowPlaying,
  LibraryScreen, 10 `*Sheet.kt`) → 0 ditemukan, cuma `Spacer` kecil (12-16dp, gap harmless); (b)
  long title/artist song row — sudah `Column(weight(1f))` + `maxLines=1` +
  `basicMarquee()`/`TextOverflow.Ellipsis`, pola flexible-width standar; (c) landscape/viewport
  pendek Now Playing — SUDAH ADA safety net `verticalScroll` (Batch 112, komentar detail di
  kode persis soal skenario ini: 3-button-nav vs gesture-nav, root cause overflow silent-clip
  sebelumnya, sekarang jadi scrollable bukan hilang); (d) font-scale besar —
  `grep "fontSize\s*=\s*[0-9]"` di seluruh `ui/*.kt` → 0 match, semua text pakai token
  `MaterialTheme.typography.*` (otomatis ikut scale sistem, 0 bypass). **Kesimpulan: audit
  statis 0 bug baru ditemukan** — safety net & pola responsive yg relevan SUDAH ada dari batch
  lampau. **Catatan jujur**: ini bukan pengganti QA visual di device fisik (small phone/
  landscape/font-scale besar sungguhan) — kandidat untuk `MANUAL_QA_CHECKLIST.md` kalau user mau
  verifikasi manual, TIDAK dipaksa "selesai 100%" cuma krn statis bersih.
- [ ] **Jangan ubah layout architecture** — cuma padding/constraint/overflow-handling lokal.
- Kandidat titik rawan (belum diverifikasi, sekadar dugaan berdasar nama file — WAJIB dicek visual
  betulan sebelum diedit): `NowPlayingScreen.kt` (artwork+kontrol), `LibraryScreen.kt`/song row
  (long title/artist), sheet-sheet di atas (`*Sheet.kt`, ada 13 file) utk font scale besar.

### 3. Surface / Color Consistency
- [x] Audit background → surface → elevated surface di seluruh `ui/*.kt`. **Diaudit Batch 260**:
  `surfaceContainer*` (token M3 elevated-surface) 0 dipakai — elevasi ditangani manual via
  `Surface(tonalElevation=...)`. 5 titik "kartu pembungkus" (Home/Library/NowPlaying/
  StatsDashboard/Settings) pola identik (`color=surface`, `contentColor=onSurface` eksplisit)
  **TAPI nilai tonalElevation beda tanpa penjelasan semantik**: 2dp/4dp/4dp/6dp/6dp. **TIDAK
  dieksekusi** — dicatat observasi tertunda keputusan user (blast radius 5 file, bisa jadi
  disengaja beda tingkat hierarki). Detail: `CHANGELOG.md` Batch 260.
- [x] Samakan treatment border/divider lintas screen. **Diaudit+diperbaiki Batch 261**: 24
  `HorizontalDivider` di 10 file, 20 eksplisit `surfaceVariant`, **3 titik (2 file) tanpa color
  (default M3 `outlineVariant`, token beda)** — `DuplicateFinderSheet.kt` (2) + `VaultSheet.kt`
  (1) disamakan ke `surfaceVariant`, mayoritas 20:3 jelas & blast radius kecil, langsung
  dieksekusi (beda dari item 1 yg diobservasi saja). Detail: `CHANGELOG.md` Batch 261.
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
