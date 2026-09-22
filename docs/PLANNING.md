# PLANNING.md — SONIX (Roadmap & Audit Teknis)

Dibuat Batch 504. Dokumen perencanaan ke depan — BUKAN pengganti `PROJECT_STATE.md` (RAM sesi
aktif) atau `CHANGELOG.md` (histori lengkap). Isi di sini: (1) temuan integritas, (2) audit
teknis baru bersumber source asli, (3) kandidat roadmap (PROPOSAL, bukan eksekusi). Update
manual tiap ada temuan/keputusan arah baru — bukan log per-batch.

## 1. TEMUAN P0 — `docs/archive/` hilang dari ZIP ini
`FILE_MANIFEST.txt` mencatat 194 file termasuk 7 file di `docs/archive/`
(`PROJECT_STATE_ARCHIVE.md`, `MANUAL_QA_CHECKLIST.md`, `ARCHIVED_POLISH_AUDIT.md`,
`ARCHIVED_MICRO_UIUX_AUDIT.md`, `ARCHIVED_ROADMAP_15_FITUR_OFFLINE.md`,
`LIQUID_GLASS_BLUR_ENGINE_DESIGN.md`, `ROADMAP_LIQUID_GLASS_REDESIGN.md`) — direferensikan
berkali-kali di `PROJECT_STATE.md`/`README.md`. **Diverifikasi Batch 504: ZIP
`AudioPlayer-1.7.20-run482.zip` cuma berisi 187 file — TEPAT 7 file itu yang hilang, bukan
kebetulan.** `.gitignore` tidak mengecualikan `docs/`. Isi asli 7 file itu TIDAK bisa
direkonstruksi dari sesi ini (0 akses histori Batch 1-424 archive) — sengaja TIDAK dikarang.

**RISIKO NYATA**: skrip `[DAILY UPDATE]` menghapus seluruh isi folder (kecuali dotfile/
`release.keystore`/`PROJECT_STATE.md`) SEBELUM unzip ZIP baru. Kalau ZIP berikutnya juga tidak
menyertakan `docs/archive/`, folder itu terhapus permanen dari direktori Termux — walau saat ini
kemungkinan masih ada di sana (device belum pernah dijalankan pakai ZIP yang bolong ini).

**WAJIB sebelum jalankan script di bawah dengan ZIP ini**: cek manual
`~/projects/audioplayer/docs/archive/` di Termux. Kalau folder itu masih ada → **jangan jalankan
`[DAILY UPDATE]` dulu**, salin isinya ke tempat aman, lalu cari tahu kenapa proses build/export
ZIP di sisi user tidak menyertakan folder ini (kandidat: tool export punya default exclude
`archive`/folder bersarang, atau folder sempat terhapus manual sebelum sesi ini). Kalau folder
itu sudah tidak ada juga di device → sudah hilang permanen, cek riwayat commit git remote
(`git log --all -- docs/archive/`) sebagai upaya pemulihan terakhir.

## 2. Audit Teknis Baru (Batch 504, digroundkan ke source asli — bukan asumsi)
1. **CI instrumentation test ADA tapi cakupan sempit**: `.github/workflows/build.yml` punya 2
   job — `build` (unit test + assemble) DAN `instrumentation-tests` (emulator API 31,
   `connectedDebugAndroidTest`, sejak Batch 103). Tapi `app/src/androidTest/` cuma 2 file
   (`PlaybackTransportTest.kt` + helper) — cakupan otomatis HANYA transport playback dasar.
   **Seluruh histori 500+ batch soal UI/gesture (drag tab-bar, bubble, animasi nav, mini
   player, dll) 0% diotomasi** — semua bergantung verifikasi MANUAL user tiap batch, itu akar
   kenapa daftar "belum dikonfirmasi device" di `PROJECT_STATE.md`/README terus menumpuk
   (puluhan item, sebagian sudah 50+ batch tanpa konfirmasi).
2. **Rasio test file**: 113 file source (`app/src/main/java`) vs 16 unit test (`app/src/test`,
   murni layer `data`/`playback`/`ui` non-Compose) + 2 androidTest. 0 `TODO`/`FIXME`/`XXX`
   ditemukan di seluruh source (`grep` app-wide) — bersih dari penanda utang teknis eksplisit.
3. **Room DAO (`LyricsDao.kt`)** — item yang di `PROJECT_STATE.md` § "Aturan sesi aktif" #6
   ditandai "belum dicek eksplisit" (celah audit Thread Safety Batch 431/432). **DIVERIFIKASI
   Batch 504: sudah benar** — `get`/`upsert`/`count`/`clearAll` semua `suspend`, `observe`
   `Flow`, 0 query blocking. Bisa dicoret dari catatan "belum dicek" di `PROJECT_STATE.md`.
4. **Gradle Wrapper belum ada di repo** — dicatat sendiri di komentar `build.yml` job
   `instrumentation-tests` sbg "gap list item #19, belum digarap". CI jalan pakai binary
   `gradle` dari `setup-gradle` action (bukan `./gradlew`); kontributor/sesi Termux lokal tetap
   butuh `gradle` terpasang manual di device.

## 3. Kandidat Roadmap (PROPOSAL — butuh instruksi eksplisit user per sektor sebelum eksekusi,
konsisten pola project ini soal fitur besar/keputusan arsitektur)
- **A. Compose UI Test untuk sektor nav bawah** (`ComposeTestRule` + `performTouchInput` simulasi
  drag) — sektor paling sering regresi berulang (Batch 435-452) dan paling sering "0
  diverifikasi", TAPI paling mungkin diotomasi penuh (gesture disimulasikan, 0 perlu hardware
  fisik) dari semua item di §2.1. ROI tertinggi untuk mengurangi backlog manual-QA.
- **B. Tambahkan Gradle Wrapper (`gradlew`)** ke repo — lepas ketergantungan versi `gradle`
  global runner/device, konsisten dgn prioritas "versi paling mutakhir" (Aturan sesi aktif #3).
- **C. Audit `NavigationRailItem` (tablet/foldable)** — TIDAK disentuh sejak Batch 437, tercatat
  eksplisit "di luar scope" di 6+ batch berturut (437-452) sementara `NavigationBarItem` ponsel
  sudah banyak berubah (pill unified, drag, easing iOS) — risiko drift visual/fungsional makin
  lebar antara 2 layout kalau dibiarkan terus.
- **D. Pulihkan `docs/archive/`** + audit proses build/export ZIP milik user — lihat §1.

## 4. Backlog Device-QA (pointer, bukan duplikat)
Daftar lengkap per-item (>15 entri individual: overscroll bounce, mini-player drag, bottom-nav
pill sync, floating bubble landscape-clip, dll) TETAP di `PROJECT_STATE.md` §`[RESUME POINT]`
dan blockquote atas `README.md` — sengaja tidak disalin ulang di sini (anti-duplikasi/ZERO-FLUFF).
Ringkas per sumber:
- `docs/QA_CHECKLIST_SONIX_v488.md` (aktif): item #4/#5/#7/#8/#9 — murni device-QA, 0 kandidat
  kode.
- `docs/archive/MANUAL_QA_CHECKLIST.md` (HILANG, lihat §1): 19 item, 0/19 pernah dicentang.
- Puluhan item UI/animasi/gesture Batch 433-503 — lihat §2.1, kandidat solusi = Roadmap A.
