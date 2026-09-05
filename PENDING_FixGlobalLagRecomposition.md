# PENDING — Fix root cause lag/stutter kronis app-wide

> **[WAJIB BACA]** File ini otomatis harus dibaca AI di awal sesi manapun yang menerima ZIP baru
> (Isolated Pending Queue, aturan VIP). Konteks lengkap investigasi ada di `PROJECT_STATE.md`
> Batch 351 — file ini cuma menyimpan rencana eksekusi konkretnya.

## Status
**Opsi B SUDAH DIEKSEKUSI Batch 352** (`delay(500)`→`delay(1000)` di `PlayerViewModel.kt`,
detail lengkap di `CHANGELOG.md`/`PROJECT_STATE.md` Batch 352) sebagai quick-win 1 batch, atas
keputusan gabungan user. **Opsi A (di bawah) BELUM dieksekusi — masih diantre sesi berikutnya
sebagai fix permanen**, rencana & cakupan file TIDAK berubah dari versi asli file ini. File ini
TETAP wajib dibaca sesi berikutnya sebelum eksekusi Opsi A — jangan anggap selesai cuma karena
Opsi B sudah jalan; Opsi B murni mitigasi frekuensi, SCOPE recomposition `AppNavHost` masih utuh.

## Root cause (ringkasan, detail penuh di PROJECT_STATE.md Batch 351)
`AppNavHost` (`MainActivity.kt`) mengoleksi `uiState` (`PlaybackUiState`, termasuk
`position`/`duration` yang di-tick tiap 500ms selama `isPlaying`) di scope teratas fungsi —
`Scaffold`+`MiniPlayerBar`+`NavHost` semua anak fungsi yang sama. Tiap tick 500ms selama musik
main memaksa SELURUH `AppNavHost` recompose, bukan cuma bagian yang genuinely butuh posisi
terbaru. Ini menjelaskan gejala lag/stutter yang muncul di SEMUA situasi (scroll, ganti tab,
MiniPlayerBar, Now Playing/sheet) dan kenapa fix-fix performa lama (blur radius, hazeEffect
dicabut total Batch 329) tidak pernah menuntaskannya — semua menyasar cost render, bukan
frekuensi+scope recomposition.

## Opsi A — Fix struktural (surgical, direkomendasikan, TAPI sentuh protected file)
Pisahkan `position`/`duration` dari `PlaybackUiState` gabungan jadi `StateFlow` tersendiri
(mis. `PlaybackProgress(position, duration)`), dikoleksi LANGSUNG di scope lokal komposable yang
genuinely butuh (`MiniPlayerBar`, slider `NowPlayingScreen`) — bukan lagi di-hoist & dibaca di
level teratas `AppNavHost`. Efeknya: tick 500ms cuma invalidate composable kecil itu sendiri,
`AppNavHost` (Scaffold+NavHost+tab switching+scroll) tidak lagi ikut ter-restart tiap 500ms.

**Perkiraan cakupan file (kemungkinan >3 file kode, WAJIB dipecah 2-3 batch terpisah, TIDAK bisa
1 batch)**:
1. `PlayerViewModel.kt` — tambah `_playbackProgress: MutableStateFlow<PlaybackProgress>` baru,
   `startPositionLoop()` tulis ke situ (bisa tetap ATAU berhenti tulis field position/duration
   lama di `_uiState` — perlu dicek dulu apa masih ada consumer lain yang baca
   `uiState.position`/`uiState.duration` selain MiniPlayerBar & NowPlayingScreen sebelum
   diputuskan hapus vs pertahankan untuk kompatibilitas).
2. `MainActivity.kt` (**protected, WAJIB edit-parsial + minim-diff**) — `AppNavHost` berhenti
   collect posisi di level teratas; teruskan `playerViewModel` (atau `Flow`-nya langsung, bukan
   `State` yang sudah di-collect) ke `MiniPlayerBar`/pemanggilan `NowPlayingScreen` supaya
   collection terjadi di scope anak, bukan di sini.
3. `MiniPlayerBar.kt` — ubah signature terima `playbackProgress: StateFlow<PlaybackProgress>`
   (atau `PlayerViewModel`), `collectAsStateWithLifecycle()` di DALAM fungsi ini sendiri,
   pisah dari `uiState` yang masih dipakai untuk currentSong/isPlaying/dll.
4. `NowPlayingScreen.kt` (kemungkinan, perlu dicek dulu seberapa dalam slider-nya terikat ke
   `uiState.position` yang sama) — pola sama, collect lokal bukan dari parameter yang di-hoist.

**Risiko**: perubahan tipe sinyal ke composable inti yang sangat sering disentuh
(`MiniPlayerBar`/`NowPlayingScreen`) — WAJIB verifikasi compile CI + visual device sebelum
dianggap beres, sesuai standar project ini. **Belum ada bukti profiler/systrace sungguhan** —
diagnosis ini dari audit kode statis (grep+baca langsung), bukan dari hasil profiling device
asli (sandbox 0 akses compiler/emulator/profiler) — kemungkinan ada KONTRIBUTOR LAIN yang belum
ketemu, tapi ini kandidat PALING KUAT yang cocok dgn SEMUA 4 gejala sekaligus.

## Opsi B — Mitigasi cepat, rendah-risiko (bukan solusi akar, tapi 1 file saja)
Turunkan frekuensi `startPositionLoop()` dari 500ms ke misal 1000ms (`PlayerViewModel.kt` saja,
1 baris `delay(500)` → `delay(1000)`). Mengurangi FREKUENSI recomposition storm setengahnya
(2x/detik → 1x/detik), TAPI tidak menghilangkan SCOPE masalahnya (`AppNavHost` tetap invalidate
utuh tiap tick, cuma lebih jarang) — progress bar mini bar & slider juga jadi kurang presisi
per detik. Cocok kalau user mau perbaikan cepat dulu sambil Opsi A direncanakan lebih matang,
TAPI kemungkinan besar user masih akan lapor "kadang masih lag" (gejala berkurang, bukan hilang).

## Yang TIDAK direkomendasikan
Menyentuh `blurRadius`/`liquidGlassAlpha`/Haze lagi — sudah terbukti BUKAN akar masalah
(Batch 329 sudah cabut 100%, gejala tetap ada). Mengulang variasi tuning render-cost akan
mengulang pola "gak pernah benar-benar fix" yang sudah dikeluhkan user secara eksplisit.

## Pertanyaan yang perlu dijawab user sebelum Opsi A dieksekusi
1. Opsi A (struktural, 2-3 batch, sentuh MainActivity.kt protected) atau Opsi B (mitigasi cepat,
   1 file, 1 batch) dulu?
2. Kalau Opsi A: boleh lanjut walau BELUM ada bukti profiler sungguhan (murni audit statis kode)?
