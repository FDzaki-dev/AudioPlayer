# PROJECT_STATE_ARCHIVE.md

Arsip detail batch lama (Batch 1–219) yang dipindah dari `PROJECT_STATE.md` — Batch 1–57 per Batch 158, Batch 58–219 per Batch 321 — supaya
`PROJECT_STATE.md` tidak terus memanjang tanpa batas dan section "⚠️ ATURAN SESI AKTIF" +
"Batch terakhir yang selesai" tetap dekat dengan bagian yang benar-benar aktif dibaca sesi
manapun. Urutan tetap DESCENDING (Batch 219 di atas, Batch 1 di bawah) — sama seperti asalnya,
Chronological Document Rule tetap berlaku di sini juga.

**Ini murni arsip referensi** — kalau butuh konteks batch sangat lama (versi awal fitur, bug
lama yang sudah pernah diperbaiki, keputusan desain awal), cari di sini. Untuk kerja aktif,
`PROJECT_STATE.md` (Batch 220 ke atas) sudah cukup. `CHANGELOG.md` tetap jadi sumber detail penuh
untuk SEMUA batch (termasuk 1-219) — arsip ini cuma memindah salinan ringkasan di
`PROJECT_STATE.md`, bukan satu-satunya tempat sejarah itu ada.

**Update Batch 523**: arsip kini juga memuat blok **Batch 425-503** (bagian teratas di bawah, dipindah VERBATIM dari `PROJECT_STATE.md` v522 — D1 Wave 4).
Batch 220-424 TIDAK ditemukan di arsip ini (hanya di `CHANGELOG.md`). Rujukan "di atas"/"di bawah"/"lihat `[RESUME POINT]`" DI DALAM blok 425-503 menunjuk
struktur `PROJECT_STATE.md` lama (sebelum dipangkas), bukan file ini.

---

## Arsip Batch 425-503 (dipindah dari `PROJECT_STATE.md` pada Batch 523 — verbatim)

### A. Catatan Batch 503 -> 425 + daftar "belum-terverifikasi saat penutupan" (asli `PROJECT_STATE.md` v522 baris 90-2421)

**Catatan Batch 503 [jawaban user re: keputusan bump media3 Gap #2, "hanya jika ada high values,
siapa yang larang?!!"]**: Sebelum eksekusi keputusan, cross-check WAJIB ke source ZIP v502 +
histori batch di bawah (bukan cuma baca [RESUME POINT] tail) menemukan **[RESUME POINT] LAMA
BASI/SALAH**: Gap #2 SUDAH TUNTAS sejak Batch 497 (bump media3 1.10.1 Batch 494 + re-add setter
Batch 495 + fix bug lanjutan Batch 496 + konfirmasi device user "it works heck yeah!!" Batch
497) — 0 "high value" tersisa utk dipertimbangkan, 0 keputusan bump-atau-implisit yang perlu
dibuat, pertanyaannya sendiri sudah usang. **0 kode diubah batch ini** — source (`PlaybackService.
kt` line 105, `app/build.gradle.kts` media3 1.10.1) sudah benar & terkonfirmasi sejak Batch 497,
TIDAK disentuh lagi. **1 file diubah (VIP, dikecualikan batas 3 file)**: `PROJECT_STATE.md` —
`[RESUME POINT]` tail dikoreksi (lihat "[KOREKSI STALE — Batch 503]" di akhir file) supaya sesi
berikutnya tidak tertipu status basi yang sama. Pelajaran proses: `[RESUME POINT]` tail WAJIB
di-cross-check ke catatan batch detail + source tiap kali dibaca, bukan ditelan mentah sebagai
kebenaran tunggal terbaru (kelas kegagalan sama seperti "PELAJARAN PROSES" staleness Batch 492
soal "Catatan Batch 490" yang telat update).

**Catatan Batch 502 [konfirmasi user: "verified, termasuk yang dibawah 30s masih masuk daftar
yang sudah ada"]**: Device-test Gap #6 DIKONFIRMASI user — poin PALING KRITIS eksplisit
disebut: lagu <30 detik yang SUDAH ada di playlist/favorit/queue **TETAP muncul** (BASE_SELECTION
polos di `getSongsByIds()` terbukti tidak kena filter, sesuai rencana). **Gap #6 (filter audio
pendek) RESMI TUNTAS** — status "NOT VERIFIED" Batch 501 DICABUT. 0 kode diubah batch ini (murni
sinkronisasi status, pola sama Batch 451/473/492/500).

**Catatan Batch 501 [jawaban eksplisit user: ambang durasi Gap #6 = "30 detik (umum industri)"]**:
Gap #6 (filter audio pendek — WhatsApp voice note/ringtone/game effect) dieksekusi sesuai
keputusan user, TIDAK diasumsikan (WAJIB tanya dulu, sesuai catatan roadmap Batch 489/500).

**1 file diubah** (dalam batas 3 file/tugas): `MusicRepository.kt`.
1. Konstanta baru `MIN_SONG_DURATION_MS = 30_000L` (companion object, `DURATION` MediaStore
   dalam MILIDETIK — 30 detik = 30_000, bukan 30) + `ALL_SONGS_SELECTION = "$BASE_SELECTION AND
   DURATION > $MIN_SONG_DURATION_MS"`.
2. `getAllSongs()` (SATU-SATUNYA titik disentuh secara behavior): selection `BASE_SELECTION` →
   `ALL_SONGS_SELECTION`.
3. **`getSongsByIds()`/`BASE_SELECTION` polos TETAP TIDAK DISENTUH** (sesuai rencana roadmap
   sejak Batch 489) — grep konfirmasi `getSongsByIds()` dipakai `PlaybackService.kt` (resume
   queue tersimpan) & `LyricsPrefetchWorker.kt`, KEDUANYA harus tetap bisa resolve ID lagu
   pendek yang SUDAH ada di playlist/favorit/queue sebelum fix ini — 0 risiko lagu itu mendadak
   hilang dari koleksi user yang sudah ada. Lagu <30 detik hanya berhenti muncul di scan
   **library baru** (`getAllSongs()`, dipakai `PlayerViewModel.kt` `refreshLibrary()`).
4. 0 grep call site lain ke `getAllSongs()`/`BASE_SELECTION` di luar 2 fungsi ini — 0 file lain
   perlu disentuh.

Balance brace/paren/bracket `MusicRepository.kt`: `{}` 34/34 `()` 124/124 `[]` 3/3.

**NOT VERIFIED** — 0 CI/device fisik sesi ini. **WAJIB DITEST user**:
1. `git push` → cek CI HIJAU (perubahan murni string SQL selection, risiko compile RENDAH).
2. Tombol "Pindai Ulang" (Library) ATAU install ulang (force scan baru) → file audio pendek
   (<30 detik — voice note WhatsApp, ringtone, game effect) TIDAK LAGI muncul di Home/Library.
3. **Paling kritis** (regresi yang WAJIB 0 terjadi): lagu apa pun yang SUDAH ada sebelumnya di
   Playlist/Favorit/Queue tersimpan yang kebetulan <30 detik → HARUS TETAP ada & bisa diputar
   normal (resume-queue app-kill, buka playlist lama, dst) — TIDAK boleh mendadak hilang hanya
   krn scan library baru memfilternya.
4. Lagu ≥30 detik: 0 regresi, tetap muncul seperti biasa.

**Catatan Batch 500 [konfirmasi user: "verified sebagaimana mestinya, non crash"]**: CI Batch 499
HIJAU (fix signature `ShuffleOrder` valid) + device-test Gap #3 DIKONFIRMASI user — eksplisit
menyebut poin PALING KRITIS (tambah/hapus/reorder queue selagi shuffle aktif) **0 crash**. **Gap
#3 (shuffle anti-repeat-nearby) RESMI TUNTAS** — status "NOT VERIFIED" Batch 498/499 DICABUT.
0 kode diubah batch ini (murni sinkronisasi status verifikasi, pola sama Batch 451/473/492).

**Catatan Batch 499 [user kirim `log_fail_478.zip` (`build-output.log`) — CI Batch 498 GAGAL]**:
`Task :app:compileReleaseKotlin`/`compileDebugKotlin` FAILED, `AntiRepeatShuffleOrder.kt:46:1`
"Class 'AntiRepeatShuffleOrder' is not abstract and does not implement abstract members:
`getNextIndex(p0: Int): Int`/`getPreviousIndex(p0: Int): Int`", + 2 error "'getNextIndex'/
'getPreviousIndex' overrides nothing".

**Root cause TERKONFIRMASI dari log (bukan tebakan)**: interface `androidx.media3.exoplayer.
source.ShuffleOrder` (media3 1.10.1, versi pin project sejak Batch 494) mendeklarasikan
`getNextIndex`/`getPreviousIndex` dengan **1 parameter** (`index: Int`) SAJA — bukti LANGSUNG
dari pesan compiler sendiri ("Potential signatures for overriding: fun getNextIndex(p0: Int):
Int"). Batch 498 menulis kedua fungsi dengan **2 parameter** (`index: Int, repeatMode: Int`),
asumsi API yang TIDAK PERNAH match interface asli — Kotlin anggap ini 2 fungsi baru yang 0
nyambung ke `override` manapun ("overrides nothing"), SEKALIGUS kontrak abstrak asli jadi tidak
terpenuhi (class tidak lengkap). Wrap-around `REPEAT_MODE_ALL` BUKAN tanggung jawab
`ShuffleOrder` — ExoPlayer sendiri yang memanggil `getFirstIndex()`/`getLastIndex()` (2 fungsi
ini SUDAH benar sejak Batch 498, 0 diubah) saat traversal kena `INDEX_UNSET` di kondisi
repeat-all, jadi behavior wrap tetap identik tanpa parameter `repeatMode` eksplisit di sini.

**Fix Batch 499 (P0 stability > lanjut Gap #3)**: **1 file diubah** (dalam batas 3 file/tugas):
`AntiRepeatShuffleOrder.kt` — signature `getNextIndex(index: Int, repeatMode: Int)`/
`getPreviousIndex(index: Int, repeatMode: Int)` → `getNextIndex(index: Int)`/`getPreviousIndex
(index: Int)`, cabang `repeatMode == Player.REPEAT_MODE_ALL` di badan fungsi DIHAPUS (redundant/
salah, bukan bagian kontrak interface), fallback `C.INDEX_UNSET` di posisi batas TETAP sama.
Import `androidx.media3.common.Player` ikut dicabut (0 pemakaian kode lagi, cuma disebut di
KDoc). **0 fungsi/logic lain disentuh** — `getLength`/`getLastIndex`/`getFirstIndex`/
`cloneAndInsert`/`cloneAndRemove`/`cloneAndClear`/`windowSizeFor`/`buildAntiRepeatNearbyOrder`
PERSIS Batch 498, 0 perubahan. **0 call site lain terdampak** — grep `PlaybackService.kt`
konfirmasi cuma memanggil constructor + `windowSizeFor()`/`buildAntiRepeatNearbyOrder()`, TIDAK
PERNAH memanggil `getNextIndex`/`getPreviousIndex` langsung (2 fungsi itu murni dipanggil
INTERNAL oleh ExoPlayer sendiri saat traversal shuffle). Balance brace/paren/bracket
`AntiRepeatShuffleOrder.kt`: `{}` 22/22 `()` 99/99 `[]` 31/31 (turun dari 24/91/33 Batch 498 —
murni penyederhanaan `when{...}` jadi `if`, 0 struktur lain berubah).

**NOT VERIFIED** — 0 CI/device fisik sesi ini (0 akses compiler Kotlin dari sandbox ini, fix
murni dari pembacaan langsung pesan error + signature yang PERSIS ditunjukkan log itu sendiri).
**WAJIB DITEST user**: (1) `git push` → cek run GitHub Actions berikutnya HIJAU (resiko regresi
RENDAH — signature-only, 0 logic baru); (2) begitu CI hijau, LANJUTKAN 5 langkah test manual
Gap #3 yang SUDAH tercatat `CHANGELOG.md` § Batch 498 (belum berubah, masih berlaku persis) —
TERUTAMA titik paling kritis: tambah/hapus/reorder queue SELAGI shuffle aktif.

**Catatan Batch 498 [klarifikasi eksplisit user, 3 pertanyaan dijawab satu per satu sebelum
coding]**: Gap #3 (shuffle anti-repeat-nearby) — spesifikasi FINAL dari user: (1) "anti-repeat-
nearby" = KEDUA makna (lagu sama persis 0 boleh bersebelahan langsung, DAN lagu baru-baru
diputar 0 boleh muncul lagi terlalu cepat); (2) ukuran window "baru-baru diputar" diserahkan ke
penilaian terbaik ("idk, sesuai preferensi terbaik aja") — dipilih: skala ¼ ukuran antrean,
dibatasi 5..25, 0 kalau antrean ≤8 lagu; (3) berlaku di KEDUA jalur shuffle yang ada (tombol
Shuffle player + "Shuffle All" Home). **2 file disentuh** (dalam batas 3 file/tugas): file BARU
`AntiRepeatShuffleOrder.kt` (custom `ShuffleOrder` Media3 + fungsi murni
`buildAntiRepeatNearbyOrder()`), dan `PlaybackService.kt` (1 listener baru
`onShuffleModeEnabledChanged` ditambah ke `Player.Listener` yang sudah ada — reaktif ke KEDUA
jalur shuffle sekaligus dari 1 titik, **0 perubahan di `PlayerViewModel.kt`**). Detail teknis
penuh + alasan kenapa custom `ShuffleOrder` (bukan cuma acak ulang list) + urutan test manual
WAJIB: `CHANGELOG.md` § Batch 498. Balance brace/paren/bracket kedua file: konsisten (lihat
CHANGELOG). **NOT VERIFIED** — 0 CI/device fisik sesi ini, terutama titik paling kritis:
tambah/hapus/reorder queue SELAGI shuffle aktif (ExoPlayer memanggil `cloneAndInsert`/
`cloneAndRemove` custom kita otomatis di titik itu) — **WAJIB DITEST user** sebelum Gap #3
dianggap tuntas, lihat CHANGELOG.md § Batch 498 utk 5 langkah test lengkap.

**Catatan Batch 497 [user: "it works heck yeah!!"]**: fix Batch 496 (`seekToPrevious()` di 3 titik
tombol Previous) DIKONFIRMASI device fisik user — BEKERJA. Gap #2 (previous 3 detik eksplisit)
BENAR-BENAR TUNTAS, status "NOT VERIFIED" Batch 496 DICABUT, 0 gap tersisa untuk fitur ini.

**Catatan Batch 496 [user laporkan: "kenapa langsung ke lagu sebelumnya nya woy!!" — Gap #2 TIDAK
jalan]**: **Root cause TERKONFIRMASI dari pembacaan kode langsung** (`grep` semua call site tombol
Previous, bukan sample): `maxSeekToPreviousPositionMs` (Batch 492/495) HANYA berefek pada
`Player.seekToPrevious()`. Ketiga call site tombol Previous project ini (`PlayerViewModel.previous()`,
`PlaybackService.applyWidgetAction` widget, `FloatingBubbleService.sendPlaybackAction` bubble)
ternyata manggil `seekToPreviousMediaItem()` — API lain, SELALU pindah lagu unconditionally,
threshold TIDAK PERNAH kepakai. **3 file diubah** (dalam batas 3 file/tugas, PAS limit):
`PlaybackService.kt`, `PlayerViewModel.kt`, `FloatingBubbleService.kt` — `seekToPreviousMediaItem()`
→ `seekToPrevious()` di ketiga titik, 1-untuk-1, 0 logika lain disentuh. MediaSession default
(tombol hardware/Bluetooth) TIDAK diubah — sudah pakai `seekToPrevious()` bawaan Media3 dari awal,
konsisten dgn kenapa cuma jalur UI/widget/bubble yang bermasalah. **NOT VERIFIED** — 0 CI/device
sesi ini. **WAJIB DITEST user 3 jalur** (tombol app, widget, bubble): lihat CHANGELOG.md § Batch
496 utk langkah test lengkap. Detail lengkap: CHANGELOG.md § Batch 496.

**Catatan Batch 495 [instruksi eksplisit user: "jangan kerjakan setengah-setengah, langsung
tuntaskan"]**: Gap #2 dituntaskan penuh. **1 file diubah** (dalam batas 3 file/tugas):
`PlaybackService.kt` — `.setMaxSeekToPreviousPositionMs(3000L)` di-re-add ke `ExoPlayer.Builder`
player sesi utama (identik Batch 492, kini lolos kompilasi krn media3 1.10.1 dari Batch 494 sudah
punya API-nya). `overlapPlayer` (crossfade privat) tidak disentuh. Balance brace/paren/bracket
`{}` 80/80 `()` 439/439 `[]` 19/19 — konsisten. **Gap #2 SEKARANG TUNTAS** (bump + re-add API, 2
batch beruntun). **0 diverifikasi CI/device sesi ini** — user WAJIB `git push` cek CI HIJAU, lalu
test manual device (lihat CHANGELOG.md § Batch 495 utk langkah test). Gap #6/#3 (filter audio
pendek, shuffle anti-repeat-nearby) TIDAK ikut tersentuh — masih butuh keputusan eksplisit user
terpisah, TIDAK termasuk dalam cakupan "tuntaskan" batch ini (scope tunnel-vision: hanya Gap #2
yang sudah dalam progress aktif Batch 493→494→495).

**Catatan Batch 494 [instruksi eksplisit user: "lakukan bump media3 ke 1.4.0+"]**: konfirmasi yang
diminta Batch 493 ("WAJIB konfirmasi eksplisit user dulu sebelum dieksekusi") SUDAH masuk. **1 file
diubah** (dalam batas 3 file/tugas): `app/build.gradle.kts` — 3 artifact (`media3-exoplayer`/
`media3-session`/`media3-common`) 1.3.1 → **1.10.1** (latest stable per developer.android.com,
dicek web_search Sept 2026 — bukan cuma 1.4.0 minimum diminta user, tapi versi stable terbaru yang
tersedia; 1.11.0-alpha01 di alpha channel, TIDAK dipakai, project stable-only). Scope MURNI version
string — 0 source Kotlin disentuh. **Gap #2 (previous-3-detik) BELUM di-re-add**: user hanya minta
bump, bukan re-implementasi fitur — API `setMaxSeekToPreviousPositionMs` sekarang TERSEDIA di
1.10.1 tapi pemanggilannya (dihapus Batch 493) BELUM ditulis ulang, tunggu instruksi eksplisit
terpisah (pola sama Gap #6/#3). compileSdk 36/minSdk 31/targetSdk 36/AGP 8.13.0/Kotlin 2.4.10 — 0
diubah, sudah kompatibel. **0 diverifikasi CI ulang sesi ini** — user WAJIB `git push` & cek run
GitHub Actions berikutnya HIJAU (ini murni bump dependency, risiko regresi kompilasi rendah tapi
BELUM dibuktikan CI nyata).

**Catatan Batch 493 [user kirim `log_fail_473.zip` (`build-output.log`) — CI Batch 492 GAGAL]**:
`Task :app:compileReleaseKotlin`/`compileDebugKotlin` FAILED, `PlaybackService.kt:107:14
Unresolved reference 'setMaxSeekToPreviousPositionMs'`.

**Root cause TERKONFIRMASI dari log + verifikasi dependency (bukan tebakan)**: `app/build.gradle.
kts` pin `androidx.media3:media3-exoplayer:1.3.1` (juga `media3-session`/`media3-common`).
`ExoPlayer.Builder.setMaxSeekToPreviousPositionMs()` baru ADA sejak Media3 **1.4.0** (changelog
resmi androidx/media, rilis setelah 1.3.1) — 0 ada di 1.3.1 sama sekali, BUKAN masalah `@OptIn`/
`@UnstableApi` (itu akan gagal beda: "must be annotated", bukan "Unresolved reference"). Kesalahan
Batch 492: web-search API Media3 tanpa cross-check ke versi PIN AKTUAL project (1.3.1) — pelajaran
proses, dicatat supaya tidak terulang (pola sama kelas "PELAJARAN PROSES" batch2 floating bubble
di bawah: JANGAN percaya dokumentasi API generik tanpa cek versi pin nyata dulu).

**Fix Batch 493 (P0 stability > selesai fitur)**: **1 file diubah** (dalam batas 3 file/tugas):
`PlaybackService.kt` — baris `.setMaxSeekToPreviousPositionMs(3000L)` + komentarnya DIHAPUS
PERSIS (revert), `.setHandleAudioBecomingNoisy(true).build()` balik seperti SEBELUM Batch 492 kata
per kata. Balance brace/paren/bracket balik PERSIS ke angka tercatat Batch 491 (`{}` 80/80 `()`
435/435 `[]` 19/19) — bukti tekstual revert bersih, 0 sisa. **TIDAK mencoba API pengganti di
1.3.1** — cek changelog 1.3.1→1.4.0 konfirmasi 0 ada setter setara di 1.3.1 (fitur ini genuinely
baru di 1.4.0, bukan cuma rename/pindah kelas). Bump `media3` ke ≥1.4.0 BISA menyelesaikan Gap #2
scara teknis, TAPI itu perubahan dependency version app-wide (bukan 1 baris lokal) — di luar scope
micro-task ini, WAJIB konfirmasi eksplisit user dulu sebelum dieksekusi (pola sama "Aturan sesi
aktif" soal dependency/versi).

**0 diverifikasi CI ulang sesi ini** (0 akses jalankan CI dari sandbox ini — user WAJIB push &
cek run berikutnya HIJAU). **Gap #2 direklasifikasi**: BUKAN LAGI "risiko rendah, 1 file" (asumsi
Batch 489 SALAH, sudah terbukti gagal kompilasi) — sekarang 1 kelompok dengan Gap #6/#3, SEMUA
butuh keputusan/konfirmasi eksplisit user dulu (di sini: mau bump Media3 1.3.1→1.4.0+ demi Gap #2,
atau biarkan implisit selamanya — behavior runtime SAMA PERSIS di kedua kasus, 0 bug nyata,
murni soal eksplisit-vs-implisit).

**Catatan Batch 492 [user konfirmasi device: checklist WAJIB DITEST Batch 491 — 6 poin gabungan
Batch 490+491 — 100% LOLOS]**: bug "EQ balik flat/default" (2 root cause terpisah: headless
resume Batch 490, `release()` salah-scope saat swipe-Recents Batch 491) DITUTUP, terverifikasi
device fisik user, 0 gap tersisa. **Koreksi staleness ditemukan sesi ini**: bagian "Catatan Batch
490" di bawah TIDAK PERNAH diperbarui menyebut Batch 491 (root cause kedua) walau CHANGELOG.md &
`[RESUME POINT]` (akhir file) sudah benar — dicek ke SOURCE langsung (`PlayerViewModel.
onCleared()`/`PlaybackService.onDestroy()`) utk pastikan fix 491 memang ada di kode, bukan cuma
klaim dokumen, SEBELUM tulis catatan ini. Body "Catatan Batch 490" di bawah dibiarkan apa adanya
sbg log historis (bukan diedit) — rujuk `[RESUME POINT]` di akhir file utk detail lengkap Batch
491.

Lanjut **[RESUME POINT berikutnya — Batch 489]** (parkir selama saga bug EQ 490-491, roadmap
sendiri 0 berubah): Gap #2 dieksekusi (kandidat "risiko rendah, scope 1 file", 0 butuh konfirmasi
user lebih lanjut, beda dari Gap #6/#3 di bawah). **1 file diubah** (dalam batas 3 file/tugas):
`PlaybackService.kt`.
1. `ExoPlayer.Builder` player sesi utama (BUKAN `overlapPlayer` privat crossfade — itu tidak
   pernah terhubung ke transport UI/`seekToPreviousMediaItem()`): `.setMaxSeekToPreviousPositionMs
   (3000L)` ditambah. Mengunci threshold "Previous restart lagu ini vs pindah ke lagu sebelumnya"
   secara eksplisit alih-alih warisan default Media3 (`C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS`
   = 3000ms, PERSIS sama — koreksi nama konstanta dari catatan Batch 489, nama benarnya `C.`
   bukan `Player.`). **0 perubahan behavior runtime hari ini** (nilai identik) — murni proteksi
   kalau versi Media3 di-bump nanti (default lib bisa berubah diam-diam, override eksplisit ini
   tidak ikut berubah).

**0 diverifikasi CI/device Batch 492** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren/bracket `PlaybackService.kt`: `{}` 80/80 `()` 438/438 `[]` 19/19). **WAJIB DITEST
user**: (1) putar lagu, biarkan lewat 3 detik, tekan tombol Previous → lagu SAAT INI restart dari
0:00 (BUKAN pindah ke lagu sebelumnya di antrean); (2) tekan Previous lagi < 3 detik sejak lagu
mulai/restart → BARU pindah ke lagu sebelumnya; (3) regresi: tombol Next, Shuffle, Repeat, 4
skenario EQ Batch 490/491 di atas — 0 berubah.

**Sisa Roadmap Gap QA v488 (BELUM dikerjakan, tunggu arahan user)**: (b) filter audio pendek
(`MusicRepository.kt`, `getAllSongs()` saja) — ambang durasi BELUM dikonfirmasi user, tanyakan
dulu sebelum eksekusi, JANGAN asumsi angka; (c) shuffle anti-repeat-nearby — FITUR BARU (custom
shuffle engine), butuh instruksi eksplisit user dulu, bukan micro-task.

**Catatan Batch 490 [laporan user: "preset EQ balik nol/default pasca app di-kill lalu musik
dimainkan lewat eksternal SONIX player"]**: bug BARU, TERPISAH dari roadmap Gap QA v488 (Batch
489, di bawah — 0 disentuh/0 berubah oleh batch ini).

**Root cause TERKONFIRMASI dari pembacaan kode (bukan tebakan)**: `PlaybackAudioSession.
onSessionIdChanged` (Batch 314) HANYA PERNAH diregistrasi dari `PlayerViewModel.init{}` — konstruk
ViewModel yang 0 pernah ada sampai `MainActivity` dibuat. Saat proses dibangkitkan HEADLESS
(resumption via widget/Bluetooth/tombol media/notifikasi/Android Auto — 0 satu pun membuat
Activity) pasca app di-kill total, `PlaybackService` (`onEvents`) tetap membuat sesi ExoPlayer
baru & tetap set `PlaybackAudioSession.sessionId`, TAPI dengan 0 listener terpasang — Equalizer
platform utk sesi BARU itu 0 pernah tahu bands/preset tersimpan sama sekali, jadi bermain di
default flat/nonaktif bawaan Android walau SharedPreferences masih menyimpan nilai asli (bukan
"hilang", cuma tidak pernah diterapkan ULANG ke sesi ini).

**2 file diubah** (dalam batas 3 file/tugas) + `EqualizerController.kt` (companion accessor,
dihitung TERPISAH — reusable infra, bukan target/source spesifik tugas ini): `AudioPlayerApplication.kt`,
`PlayerViewModel.kt`.
1. `EqualizerController.kt`: `getInstance(context)` baru (companion, double-checked locking) —
   shared SATU instance per proses. WAJIB karena fix ini menambah TITIK PANGGIL KEDUA
   (`AudioPlayerApplication`) yang legitimately ingin `EqualizerController` yang sama —
   `android.media.audiofx.Equalizer` TIDAK dedupe per sesi, 2 instance terpisah ke sesi yang sama
   = 2 efek insert nyata, diam-diam MENGGANDAKAN tiap band gain kalau keduanya kebetulan hidup
   bersamaan. `getInstance()` membuat itu MUSTAHIL secara struktural, bukan mengandalkan disiplin
   pemanggil. 0 API method lain di kelas ini berubah (`attach`/`setEnabled`/`setBandLevel`/
   `usePreset`/`useBoldPreset`/`release` 100% sama) — `EqualizerSheet.kt`/`NowPlayingScreen.kt`
   (importir `EqualizerController.BoldPreset`, bukan instance) 0 tersentuh sama sekali.
2. `AudioPlayerApplication.kt` (`onCreate()`): `PlaybackAudioSession.onSessionIdChanged = { id ->
   EqualizerController.getInstance(this).attach(id) }` ditambah SEGERA setelah `AppLogger.init`/
   `warmUpSharedPreferences` — `Application.onCreate()` berjalan SEBELUM komponen mana pun
   (Activity ATAU Service) apa pun pemicu proses dimulai, jadi listener ini sudah ada SEBELUM
   `PlaybackService`-nya player sempat fire `onEvents` pertama kalinya.
3. `PlayerViewModel.kt`: `private val equalizerController = EqualizerController(appContext)` →
   `EqualizerController.getInstance(appContext)` — WAJIB, supaya ViewModel share instance yang
   SAMA dgn Application, bukan bikin instance kedua (lihat poin 1). `init{}` (Batch 314, re-wiring
   callback + attach immediate kalau sesi sudah ada) TIDAK diubah — sekarang cuma re-registrasi
   callback yang FUNGSINYA identik (instance sama) + re-attach yang idempotent (attach() sendiri
   release-lalu-buat-baru, 0 pernah menumpuk instance kedua).

**0 diverifikasi CI/device Batch 490** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren/bracket: `EqualizerController.kt` `{}` 24/24 `()` 117/117 `[]` 6/6;
`PlayerViewModel.kt` `{}` 248/248 `()` 1021/1021 `[]` 39/39; `AudioPlayerApplication.kt` `{}`
14/14 `()` 77/77 `[]` 0/0). **WAJIB DITEST user**:
1. Buka Equalizer (Now Playing → ⋮ → Equalizer), pilih preset apa saja SELAIN Flat (mis. Bass+
   atau preset bawaan perangkat) sampai terdengar jelas berbeda dari flat.
2. Putar lagu, lalu Force-close TOTAL app (App Info → Force Stop, ATAU swipe dari Recents lalu
   tunggu OS recycle proses — BUKAN cuma tombol Home).
3. TANPA membuka app dari launcher sama sekali, picu playback dari LUAR: tombol play di widget
   home-screen, ATAU tombol play/media-button headset/Bluetooth, ATAU notifikasi media, ATAU
   Android Auto (pilih salah satu yang tersedia di device) → dengarkan: preset EQ dari langkah 1
   harus TERASA (bukan flat/datar) sejak detik pertama lagu main, TANPA perlu buka app dulu.
4. Setelah itu BARU buka app dari launcher → sheet Equalizer harus tetap menunjukkan preset yang
   sama (langkah 1), 0 ter-reset ke Flat/nonaktif, DAN suara tidak berubah/tidak "makin kencang
   dobel" saat app dibuka (mengesampingkan resiko double-effect dari fix `getInstance()`).
5. Regression check jalur NORMAL (app dibuka biasa dari launcher, bukan headless): equalizer
   masih berfungsi seperti biasa — ganti band/preset di sheet langsung terdengar, tersimpan lintas
   sesi seperti sebelumnya (0 regresi ke behavior existing).

**[RESUME POINT berikutnya — Batch 489, TIDAK berubah oleh batch ini]**: 3 sisa gap dari checklist
yang punya kandidat fix kode konkret, BELUM dikerjakan (lihat "Roadmap Gap QA v488" di "Catatan
Batch 489" di bawah): (a) previous-3-detik eksplisit; (b) filter audio pendek (ambang durasi belum
dikonfirmasi user); (c) shuffle anti-repeat-nearby (fitur baru, butuh instruksi eksplisit user).

**Catatan Batch 489 [input baru: `QA_Checklist_SONIX_v488_terisi.md`, instruksi eksplisit user
"tanamkan planning dokumen tersebut kedalam project yang disesuaikan dengan kondisi nyata"]**:
checklist adalah hasil audit source-inspection eksternal (BUKAN hasil kerja batch mana pun di
project ini) terhadap `SONIX_v488.zip` — 5 kategori fitur core (Kontrol Pemutaran, Audio
Back-End, Playlist/Queue, Integrasi Sistem, File/Eror) ditandai `[x]`/`[~]`/`[-]` per kriteria.
Checklist ITU SENDIRI eksplisit membedakan 2 kelas gap: (a) gap SUMBER yang bisa dikonfirmasi
dari baca kode (9 poin di "Verdict"), (b) item yang murni BELUM diverifikasi device fisik (tidak
bisa "difix" lewat kode sama sekali, mis. latency nyata, kombinasi tekan TWS, ketahanan RAM
jangka panjang). Batch ini memverifikasi ULANG kesembilan poin (a) satu-satu ke kode sungguhan
(bukan menelan klaim checklist mentah-mentah — checklist adalah dokumen eksternal, P1 tetap ZIP/
source, bukan checklist), lalu MENGEKSEKUSI 1 gap yang paling jelas & aman diperbaiki batch ini
(TUNNEL VISION — sisanya masuk roadmap terlacak di bawah, BUKAN dikerjakan sekaligus).

**Verifikasi ulang 9 gap "Verdict" checklist terhadap source (bukan tebakan)**:
1. **Stop playback eksplisit** — DIKONFIRMASI BENAR gap nyata: grep `controller?.stop()` app-wide
   cuma 1 hit, di dalam `PlayerViewModel.dismissMiniPlayer()` (pola "cancel" — ikut mengosongkan
   queue/state tersimpan via `UndoableAction`, BUKAN tombol Stop biasa). 0 kontrol Stop berdiri
   sendiri di UI mana pun. **DIPERBAIKI batch ini** (lihat di bawah).
2. **Previous 3 detik** — DICEK ke `PlaybackService.kt` (ExoPlayer.Builder) & `PlayerViewModel.
   previous()`: BENAR 0 ada override eksplisit `setSeekBackIncrementMs`/threshold custom, murni
   `controller?.seekToPreviousMediaItem()` polos. Catatan tambahan (tidak ada di checklist):
   default Media3 (`Player.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS`) SUDAH persis 3000ms — jadi
   perilaku RUNTIME kemungkinan besar sudah sesuai spec, gap-nya murni "tidak eksplisit/tidak
   didokumentasikan sebagai keputusan sadar" (rawan berubah diam-diam kalau versi Media3 di-bump
   nanti). **BELUM diperbaiki batch ini** — kandidat fix murah (1 baris `setMaxSeekToPreviousPositionMs`
   di scope `ExoPlayer.Builder` `PlaybackService.kt` untuk mengunci nilainya secara eksplisit,
   bukan warisan default library), masuk roadmap di bawah, BUKAN dikerjakan bareng gap #1 supaya
   batch ini tetap tunnel-vision 1 sektor (transport control UI) tanpa merambah `PlaybackService.kt`.
3. **Shuffle anti-repeat-nearby** — DICEK: shuffle 100% delegasi ke `Player.shuffleModeEnabled`
   Media3 bawaan, 0 algoritma custom di project ini. Constraint "tidak mengulang lagu yang sama
   dalam waktu dekat" MEMANG tidak dijamin (benar, sesuai checklist) — TAPI ini scope FITUR BARU
   (custom shuffle engine), bukan sekadar "gap kecil", risiko regresi ke urutan queue/persistence
   (`PlaybackStateStore`) kalau dikerjakan tergesa. **BELUM diperbaiki** — butuh instruksi
   eksplisit user dulu sebelum dikerjakan (pola sama seperti sektor pill/tab-sync: fitur besar,
   bukan micro-task), dicatat di roadmap.
4. **Bluetooth reconnection** — DICEK `PlaybackService.kt`: `setHandleAudioBecomingNoisy(true)` +
   MediaSession resumption SUDAH terpasang (bukan gap kode) — checklist sendiri sudah bilang ini
   murni butuh uji HARDWARE, 0 baris kode kandidat fix. **Tidak actionable lewat kode.**
5. **Audio focus semua skenario** — DICEK: `AudioAttributes` + `handleAudioFocus = true` Media3
   SUDAH benar dipasang (bukan gap kode) — checklist sendiri bilang murni butuh device-test lintas
   skenario telepon/WA-call/alarm/Maps. **Tidak actionable lewat kode.**
6. **Filter audio pendek (WhatsApp/ringtone/game effect)** — DICEK `MusicRepository.kt`:
   DIKONFIRMASI BENAR, `BASE_SELECTION` cuma `IS_MUSIC != 0 AND DURATION > 0`, 0 batas durasi
   minimum. **BELUM diperbaiki batch ini** — kandidat fix (duration-floor HANYA di `getAllSongs()`,
   `getSongsByIds()` TETAP tidak disentuh supaya lagu pendek yang SUDAH ada di playlist/queue/
   favorit sebelum fix ini tidak mendadak hilang) masuk roadmap, sengaja dipisah dari gap #1 biar
   1 batch = 1 file yang disentuh (`MusicRepository.kt` murni, tanpa dicampur `ui`/`playback`).
7. **Streaming audio online** — checklist SENDIRI sudah menandai ini `[-]` (di luar scope, app ini
   pemutar lokal). **Tidak ada aksi, bukan gap.**
8. **Process-death/background/lock-screen/TWS** — semua murni device-QA, 0 kandidat fix kode baru
   yang tidak sudah dibahas gap #4/#5 di atas. **Tidak actionable lewat kode.**
9. **Multi-format decoder robustness** — DICEK: dukungan format (MP3/AAC/FLAC/WAV/OGG/OPUS/AMR)
   sepenuhnya diwariskan dari MediaStore/Media3 platform, project ini 0 punya decoder sendiri buat
   diaudit. **Tidak actionable lewat kode** — butuh corpus file nyata + device test, sesuai
   checklist sendiri.

**1 file diubah** (dalam batas 3 file/tugas — kode inti) + 2 file wiring UI/Activity (total tetap
dianggap 1 sektor/1 tugas, pola sama Batch 484 yang eksplisit menyebut alasan melebihi hitungan
normal): `PlayerViewModel.kt`, `NowPlayingScreen.kt`, `MainActivity.kt`.
1. `PlayerViewModel.kt`: `stopPlayback()` baru — `pause()` + `seekTo(0L)` lewat `controller`.
   SENGAJA BUKAN `controller.stop()` mentah (itu pindah Media3 ke `STATE_IDLE`, butuh `prepare()`
   ulang sebelum `play()` berikutnya — jalur lifecycle itu 0 diaudit batch ini, lebih aman
   pause+seekTo yang hasil akhirnya sama-sama "berhenti + posisi balik ke 0" tanpa mengubah
   player state machine). 0 menyentuh queue/`currentQueueSlotIds`/`playbackStateStore`/
   `UndoableAction` — beda total dari `dismissMiniPlayer()` (itu "cancel", ini "stop").
2. `NowPlayingScreen.kt`: parameter baru `onStopPlayback` diteruskan ke `AdvancedControlsSheet`,
   1 `AdvancedControlRow` baru ("Stop Pemutaran", ikon `Icons.Default.Stop`) di seksi "Pemutaran"
   sheet "Kontrol Lanjutan" — SENGAJA TIDAK ditaruh di Row transport utama (Shuffle/Prev/Play/
   Next/Repeat, 5 ikon `SpaceEvenly`) supaya 0 menyentuh layout yang sudah di-tuning berkali-kali
   (Batch 170/224/226/469-472 dst) — pola "kontrol yang jarang dipakai casual listener masuk
   sheet Lanjutan" ini SUDAH jadi konvensi file ini sejak Batch 312 (KDoc `AdvancedControlsSheet`).
   0 dialog konfirmasi (beda dari dismiss mini player yang butuh Undo) — aksinya reversibel
   sendiri, tinggal tekan Play lagi.
3. `MainActivity.kt`: 1 baris wiring `onStopPlayback = { playerViewModel.stopPlayback() }` di
   satu-satunya titik panggil `NowPlayingScreen(...)` (`nowPlayingContent` lambda, Batch 101 —
   dipakai Compact & Expanded/two-pane, jadi 1 wiring ini otomatis berlaku ke keduanya).

**0 diverifikasi CI/device Batch 489** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren/bracket: `PlayerViewModel.kt` `{}` 248/248 `()` 1017/1017 `[]` 39/39;
`NowPlayingScreen.kt` `{}` 293/293 `()` 1299/1299 `[]` 1/1; `MainActivity.kt` `{}` 341/341
`()` 1270/1270 `[]` 3/3). **WAJIB DITEST user**:
1. Buka lagu apa saja → Now Playing → ketuk ⋮ ("Kontrol Lanjutan") → scroll ke seksi "Pemutaran"
   → baris baru "Stop Pemutaran" harus muncul (ikon kotak/Stop) di bawah "Repeat A-B & Bookmark".
2. Ketuk baris itu → sheet tertutup, musik BERHENTI + slider posisi balik ke 0:00, TAPI judul
   lagu/artwork/antrean TETAP tampil (bukan reset ke Home/kosong seperti swipe-dismiss mini
   player) — tekan tombol Play (▶) lagi → lagu yang SAMA lanjut main dari awal (0:00), BUKAN
   pindah ke lagu lain di antrean.
3. Pastikan 0 regresi ke swipe-dismiss mini player (Batch 476/480 — masih "cancel total" +
   Snackbar "Urungkan", TIDAK disentuh batch ini) dan ke 5 tombol transport utama (Shuffle/Prev/
   Play/Next/Repeat) — 0 perubahan posisi/ukuran/handler di Row itu.
4. Pastikan 0 crash/force-close saat build (1 fungsi ViewModel baru + 1 parameter baru diteruskan
   lewat 1 composable — 0 signature lama yang dihapus/diubah, semua penambahan murni).

**Roadmap Gap QA v488 (BELUM dikerjakan, urutan bukan prioritas mutlak — tunggu arahan user atau
lanjutkan sebagai micro-task berikutnya)**:
- **Gap #2 (Previous 3 detik eksplisit)** — 1 baris kandidat: `setSeekBackIncrementMs`/setara di
  `ExoPlayer.Builder` (`PlaybackService.kt`) mengunci 3000ms eksplisit alih-alih warisan default
  Media3. Risiko rendah, scope 1 file.
- **Gap #6 (Filter audio pendek)** — kandidat: `MusicRepository.kt`, `getAllSongs()` SAJA dapat
  selection baru dengan `DURATION > <ambang>` (mis. 30 detik, ambang umum industri musik-vs-clip),
  `getSongsByIds()`/`BASE_SELECTION` lama TETAP tidak disentuh (lagu pendek yang SUDAH ada di
  playlist/favorit/queue tidak boleh mendadak hilang — cek `getSongsByIds()` dipakai
  `PlaybackService.kt` resume-queue & `LyricsPrefetchWorker.kt`, KEDUANYA harus tetap bisa
  resolve ID lama). Ambang pasti (30 detik? beda per kategori WhatsApp vs ringtone?) sebaiknya
  dikonfirmasi user dulu — bukan angka yang disebutkan checklist secara eksplisit, murni asumsi
  konservatif kalau dieksekusi tanpa konfirmasi.
- **Gap #3 (Shuffle anti-repeat-nearby)** — FITUR BARU (custom shuffle engine), bukan micro-task
  kecil. Instruksi eksplisit user disyaratkan dulu sebelum dikerjakan (pola sama sektor pill/
  tab-sync: fitur besar butuh audit 1 batch penuh, bukan diselipkan).
- **Gap #4/#5/#8/#9** — 0 kandidat fix kode (kode sudah benar per baca-kode batch ini), murni
  menunggu hasil device-QA fisik dari user. Checklist asli disalin ke `docs/QA_CHECKLIST_SONIX_v488.md`
  sebagai rujukan detail per-poin (bukan diarsipkan — masih aktif, lihat "Aturan sesi aktif" baru
  di bawah).

**Catatan Batch 488 [laporan urgent user: "app tidak load berulang kali setiap aplikasi baru
dibuka kembali pasca app di kill!!"]**: laporan BARU, BUKAN reopen sektor DITUTUP manapun,
TERPISAH dari antrean `.animateItem()` (Batch 481-487 di bawah — TIDAK disentuh/TIDAK berubah
oleh batch ini, resume chain itu tetap berlaku persis seperti tercatat).

**Root cause TERKONFIRMASI dari pembacaan kode (bukan tebakan)**: `PlayerViewModel.
ensureLibraryLoaded()` — `libraryLoadedOnce` murni in-memory `var`, SELALU `false` lagi di
proses baru (app-kill lalu dibuka lagi) → `refreshLibrary()` (full scan MediaStore+SAF) dipicu
ulang dari nol TIAP KALI, `_libraryLoading=true` (shimmer skeleton `HomeScreen`/`LibraryScreen`)
menutupi list SETIAP app dibuka lagi walau library 0 berubah sejak sesi lalu. Sudah
didokumentasikan sendiri Batch 386 ("jalur paling panas cold-start") & Batch 436 (investigasi
laporan user "nunggu buffer ±20 detik") — Batch 436 waktu itu MENYIMPULKAN ini "expected
behavior" (bukan bug), TIDAK di-fix. Batch ini merevisi kesimpulan itu jadi di-fix, sesuai
laporan urgent user.

**2 file diubah** (dalam batas 3 file/tugas, 1 di antaranya file baru): `LibraryCacheStore.kt`
(BARU), `PlayerViewModel.kt`.
1. `LibraryCacheStore.kt` (baru, `data/`): snapshot `List<Song>` hasil scan terakhir disimpan ke
   disk (`context.filesDir`, JSON via `org.json` — bagian Android SDK, 0 dependency baru
   ditambah `build.gradle.kts`) lewat temp-file-lalu-rename (proses di-kill di tengah `save()`
   tidak pernah menyisakan file cache korup). `save()`/`load()` murni sinkron — caller
   (`PlayerViewModel`) yang wajib dispatch ke `Dispatchers.IO`, pola sama persis Store lain
   (`PlaybackStateStore` dkk) di package ini.
2. `PlayerViewModel.kt`:
   - `ensureLibraryLoaded()`: sebelum `refreshLibrary()`, muat cache disk dulu (IO) — kalau ada
     & tidak kosong, `_librarySongs`/`_libraryLoading` diisi LANGSUNG dari situ (list asli
     tampil seketika, 0 shimmer) + `maybeSyncMiniPlayerOnColdStart()` ikut dipanggil (3 titik
     panggil sekarang, pola sama Batch 476 "menutup race mana pun selesai duluan"), LALU
     `refreshLibrary(silent = true)` TETAP jalan di background (menangkap perubahan lagu asli
     sejak sesi lalu — scan asli 0 dihapus/dilewati, cuma tidak lagi memicu shimmer). Kalau
     cache tidak ada/korup/kosong (mis. install baru) → turun ke `refreshLibrary()` biasa
     (`silent=false`), 0 berubah dari sebelum batch ini.
   - `refreshLibrary()`: param baru `silent: Boolean = false` — `_libraryLoading=true` HANYA
     kalau `!silent`. **0 titik panggil LAMA berubah** (pull-to-refresh & tombol "Pindai Ulang"
     `MainActivity.kt`, content-observer, dkk — SEMUA masih panggil tanpa argumen = default
     `false`, shimmer/loading feedback rescan manual TETAP tampil PERSIS seperti sebelumnya).
   - Setelah scan sukses (`_librarySongs.value = songs`): `libraryCacheStore.save(songs)`
     dipanggil fire-and-forget di `Dispatchers.IO` terpisah — gagal/lambatnya cache write TIDAK
     PERNAH menunda update UI (`_librarySongs` sudah di-assign duluan di baris sebelumnya).

**0 diverifikasi CI/device Batch 488** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren/bracket: `PlayerViewModel.kt` `{}` 246/246 `()` 1003/1003 `[]` 38/38;
`LibraryCacheStore.kt` baru `{}` 11/11 `()` 109/109 `[]` 1/1). **WAJIB DITEST user**:
1. Buka app, tunggu library selesai dimuat sekali (boleh shimmer). Force-close TOTAL (App Info →
   Force Stop, ATAU swipe dari Recents lalu tunggu OS recycle proses). Buka app lagi dari
   launcher → Home/Library HARUS langsung tampil isi (0 shimmer/layar kosong), bukan nunggu scan
   ulang seperti sebelumnya.
2. Ulangi test #1 sekali lagi (buka→force-close→buka) → tetap instan & konsisten.
3. Tambah/hapus 1 lagu (file manager/app lain) SAAT app dalam kondisi force-close, lalu buka app
   lagi → list tetap tampil INSTAN dari cache dulu (boleh sesaat belum mencerminkan perubahan
   itu), lalu dalam beberapa detik berikutnya (scan senyap background selesai) list ikut update
   mencerminkan lagu yang ditambah/dihapus — TANPA shimmer muncul di tengah proses ini.
4. Tombol "Pindai Ulang" (Library screen) DAN pull-to-refresh: pastikan shimmer/loading feedback
   SAAT DITEKAN MANUAL tetap muncul seperti biasa (0 regresi — SENGAJA tidak diubah, beda dari
   cold-start otomatis di atas).
5. Install baru / clear app data (skenario 0 cache) → shimmer pertama kali TETAP muncul seperti
   biasa (0 regresi ke kondisi ini — cuma kondisi cold-start BERIKUTNYA yang berubah).
6. Pastikan 0 crash/force-close saat build (1 import baru `LibraryCacheStore` di
   `PlayerViewModel.kt`, 0 dependency baru di `build.gradle.kts`).

Lanjutan: sektor ini dianggap TUNTAS menunggu konfirmasi device 6 poin di atas. Antrean
`.animateItem()` (Batch 481-487) & seluruh investigasi lain (bubble/tab-nav/mini-player) TIDAK
terpengaruh/TIDAK berubah oleh batch ini — lanjutkan sesuai instruksi eksplisit user berikutnya.

**Catatan Batch 486 [instruksi user: "lanjutkan progress yang tertunda!!" — TANPA konfirmasi
eksplisit hasil test Batch 485 (Song Picker/AB Repeat/Equalizer)]**: hasil test Batch 485 BELUM
dikonfirmasi user di sesi ini — dicatat di sini supaya sesi berikutnya 0 salah asumsi ("AI DILARANG
menebak histori"). Resume point Batch 486 memberi 2 cabang: (a) kalau test 485 ✅ → lanjut
`.animateItem()` ke `SmartPlaylistScreen.kt`/`LyricsSheet.kt`; (b) kalau tidak → sektor
pill/tab-sync + gesture-drag, TAPI cabang (b) eksplisit butuh "instruksi eksplisit user utk sektor
itu spesifik" yang TIDAK ada di pesan ini. Karena cabang (b) terkunci tanpa instruksi spesifik,
satu-satunya lanjutan valid dari instruksi generik "lanjutkan" adalah cabang (a) — dieksekusi di
bawah. Sektor pill/tab-sync TETAP 0 disentuh.

**2 file diubah** (dalam batas 3 file/tugas): `SmartPlaylistScreen.kt`, `LyricsSheet.kt`.
1. Kedua file dicek dulu (pola sama Batch 482-485): grep `isDragging`/`draggable`/`reorder` = 0
   hit di keduanya → aman dari resiko `.animateItem()` berebut dgn gesture drag.
2. `SmartPlaylistScreen.kt` (4 titik, semua key sudah stabil sejak awal): (i) list playlist
   otomatis (`itemsIndexed`, key `p.id`); (ii) list lagu cocok aturan (`itemsIndexed`, key
   `song.id`); (iii) chip filter folder di builder sheet (`items`, key = string folder itu
   sendiri); (iv) chip filter genre di builder sheet (`items`, key = string genre itu sendiri) —
   (iii)/(iv) pola sama chip preset `EqualizerSheet.kt` Batch 485 (chip statis, animasi hanya utk
   transisi selected-state/layout, bukan insert/remove).
3. `LyricsSheet.kt` (1 titik, prioritas RENDAH per rasional Batch 483): list baris lirik
   (`itemsIndexed`, key = INDEX bukan id konten — list dibangun ulang via `remember(rawLyrics)`,
   0 pernah insert/remove saat playback normal). Ditambah tetap demi konsistensi antrean; dampak
   nyata cuma muncul di alur edit-lirik-lalu-simpan (jumlah baris berubah) — logic sync
   highlight/auto-scroll (`activeIndex`/`LaunchedEffect`) 0 disentuh.
4. **0 disentuh**: logic filter/aturan smart playlist, logic parse/sync lirik, sektor
   pill/tab-sync + gesture-drag custom (mini player/queue) — keputusan Batch 483 masih berlaku,
   TIDAK dimulai tanpa instruksi eksplisit user utk sektor itu.

**0 diverifikasi CI/device Batch 486** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren dicek per file: `SmartPlaylistScreen.kt` `{}` 106/106 `()` 284/284;
`LyricsSheet.kt` `{}` 65/65 `()` 191/191, keduanya match). **WAJIB DITEST user (gabung dgn test
Batch 485 yang masih pending)**:
1. Smart Playlist → buka list "Playlist Otomatis" (kalau >1 playlist) → transisi tetap mulus saat
   playlist dihapus (FAB ikon sama, 0 regresi); buka salah satu playlist → list "lagu cocok" harus
   tetap smooth kalau aturan diubah lewat pensil (jumlah lagu cocok berubah).
2. Smart Playlist Builder (tombol pensil/buat baru) → toggle chip Folder & Genre → transisi
   visual chip (selected state) tetap mulus, 0 regresi ke pemilihan folder/genre atau simpan
   aturan.
3. Lyrics (buka lirik lagu apa saja) → scroll/auto-scroll sync tetap presisi, 0 regresi; kalau
   sempat edit lirik lalu simpan → transisi baris baru/hilang boleh terlihat (bukan bug).
4. Pastikan 0 crash/force-close saat build (0 signature/parameter baru, murni modifier tambahan).
**[RESUME POINT Batch 487]**: kalau SEMUA test Batch 485 + 486 di atas ✅ (Song Picker, AB Repeat,
Equalizer, Smart Playlist, Lyrics), antrean micro-task polish `.animateItem()` HABIS — baru boleh
lanjut ke sektor pill/tab-sync + gesture-drag custom (mini player/queue), TAPI TETAP WAJIB
instruksi eksplisit user utk sektor itu spesifik (bukan otomatis dari "lanjutkan" generik) krn
riwayat regresi (Batch 448/477/479) butuh audit 1 batch penuh, bukan diselipkan. Kalau ada test
di atas ❌, laporkan detail kegagalannya dulu — jangan lanjut sektor manapun sebelum root cause
jelas.

**Catatan Batch 485 [user konfirmasi fix biometrik Batch 484 ✅ ("applause"), instruksi: "lanjut
kerjakan Polish yang masih tertunda"]**: lanjut resume point Batch 484 (masih valid, 0 berubah
sejak ditulis) — micro-task polish animasi antrean Batch 481/482/483.

**3 file diubah** (dalam batas 3 file/tugas): `SongPickerSheet.kt`, `ABRepeatBookmarkSheet.kt`,
`EqualizerSheet.kt`.
1. Ketiganya dicek dulu (pola sama Batch 482/483): grep `isDragging`/`draggable`/`reorder` di
   masing-masing file = 0 hit → aman dari resiko `.animateItem()` "berebut" dgn gesture drag.
   `SongPickerSheet.kt` punya gesture custom "sweep-select" (`isSweeping`/`rowBoundsInRoot`,
   long-press+drag utk tandai rentang checkbox) — DIPERIKSA MANUAL, ini BUKAN drag-reorder (0
   memindah posisi item, cuma menandai rentang), jadi tetap aman ditambah `.animateItem()`.
2. `SongPickerSheet.kt`: `.animateItem()` di awal modifier chain row lagu (`itemsIndexed`, key
   `song.id` sudah ada sejak awal — prasyarat terpenuhi).
3. `ABRepeatBookmarkSheet.kt`: `BookmarkRow` (private, 1 titik panggil) dapat parameter baru
   `modifier: Modifier = Modifier` (default aman, 0 breaking, pola PERSIS `DuplicateSongRow`
   Batch 482) diteruskan ke root `Row`; titik panggil diberi `modifier = Modifier.animateItem()`.
4. `EqualizerSheet.kt`: 2 `FilterChip` inline di dalam masing-masing `LazyRow` (Preset Kuat +
   Preset Bawaan Perangkat) langsung ditambah `.animateItem()` di awal modifier chain — 0 perlu
   ubah signature (FilterChip sudah terima `modifier` langsung), key masing-masing sudah stabil
   (`preset.name` / `state.presets[index]`).
5. **0 disentuh**: `SmartPlaylistScreen.kt`/`LyricsSheet.kt` (di luar 3 file resume point batch
   ini — tetap di antrean kalau user minta lanjut), logic filter/sweep-select/checkbox
   (SongPicker), logic jump/delete bookmark (ABRepeat), logic select/enabled preset (Equalizer)
   — 0 baris logic berubah, murni tambahan modifier animasi. Sektor pill/tab-sync +
   gesture-drag custom (mini player/queue) TETAP tidak disentuh (keputusan Batch 483 masih
   berlaku).

**0 diverifikasi CI/device Batch 485** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren dicek per file, semua match: `SongPickerSheet.kt` `{}` cocok, `()` cocok;
`ABRepeatBookmarkSheet.kt` `{}` cocok, `()` cocok; `EqualizerSheet.kt` `{}` cocok, `()` cocok).
**WAJIB DITEST user**:
1. Song Picker (mis. tambah lagu ke playlist/queue) → ketik di search box supaya daftar
   terfilter naik-turun → baris yang muncul/hilang harus slide+fade halus (bukan pop instan),
   sisa baris geser mengisi celah; sweep-select (long-press lalu geser utk centang banyak
   sekaligus) harus tetap akurat 100% (0 regresi ke rentang yang ditandai).
2. AB Repeat → tambah beberapa bookmark lalu hapus salah satu → baris yang dihapus slide+fade
   keluar halus, sisa baris geser naik mengisi celah, 0 regresi ke jump-to-position/rename.
3. Equalizer → ganti-ganti preset (Kuat maupun Bawaan Perangkat) → transisi visual chip
   (selected state, layout) tetap mulus, 0 regresi ke enabled/disabled state atau band slider.
4. Pastikan 0 crash/force-close saat build (parameter baru `modifier` di `BookmarkRow`, default
   value jadi 0 breaking di satu-satunya titik panggil).
**[RESUME POINT Batch 486]**: kalau test di atas ✅, `SmartPlaylistScreen.kt`/`LyricsSheet.kt`
masih di antrean `.animateItem()` kalau user minta lanjut ke situ (LyricsSheet prioritas RENDAH,
lihat rasional Batch 483 — list statis 0 pernah insert/remove runtime). Kalau tidak, lanjut ke
sektor pill/tab-sync + gesture-drag custom (mini player/queue) — PALING TERAKHIR per keputusan
Batch 483/484, butuh audit 1 batch penuh (bukan diselipkan) krn riwayat regresi (Batch
448/477/479) — jangan mulai dari situ tanpa instruksi eksplisit user utk sektor itu spesifik.

**Catatan Batch 484 [2 instruksi eksplisit user dalam 1 pesan: (1) "regresi lain di bagian
keamanan yaitu absennya biometrik fingerprint beserta label sidik jari diujung bawah" (Kunci
Aplikasi/LockScreen), (2) "sekalian tanamkan biometrik juga pada vault"]**:

**(1) BUG FIX — root cause TERKONFIRMASI dari dokumentasi platform (bukan tebakan)**:
`AndroidManifest.xml` TIDAK PERNAH mendeklarasikan `android.permission.USE_BIOMETRIC`
(grep app-wide: 0 hasil sebelum batch ini). Dokumentasi resmi `BiometricManager.canAuthenticate()`
eksplisit "Requires android.Manifest.permission#USE_BIOMETRIC" — tanpa deklarasi ini,
`canAuthenticate()` tidak pernah balas `BIOMETRIC_SUCCESS`, jadi `isBiometricAvailable()` di
`MainActivity.kt` SELALU false app-wide, meng-gate HILANG toggle "Buka dengan Sidik Jari" di
Settings DAN tombol sidik jari di `LockScreen.kt` — cocok persis gejala di screenshot user
(numpad PIN tanpa apa-apa di kiri-bawah). Kotlin `LockScreen.kt`/`MainActivity.kt`/
`PlayerViewModel.kt`/`AppLockStore.kt` DIPERIKSA SATU-SATU, wiring-nya sudah 100% benar sejak
awal — 0 bug logic di sana, gap murni 1 baris permission yang hilang.
1. `AndroidManifest.xml`: tambah `<uses-permission android:name="android.permission.USE_BIOMETRIC" />`.
2. `LockScreen.kt`: sebelumnya tombol sidik jari HANYA punya `contentDescription` (teks
   accessibility, tidak pernah kelihatan di layar) — 0 label visual di UI sungguhan. Tambah
   `Text("Sidik Jari")` di bawah ikon (dibungkus `Column`), sesuai instruksi eksplisit user
   ("...beserta label sidik jari").

**(2) FITUR BARU — biometrik Vault** (sebelumnya 0 ada sama sekali, Vault cuma PIN manual):
1. `VaultStore.kt`: `isBiometricEnabled()`/`setBiometricEnabled()` ditambah (pola identik
   `AppLockStore`, prefs KEY baru `vault_biometric_enabled`, own prefs file — TETAP independen
   dari `AppLockStore` sesuai KDoc lama, 0 reuse silang). `disableVault()` ikut clear flag ini.
2. `VaultSheet.kt`: helper privat `isVaultBiometricAvailable()`/`showVaultBiometricPrompt()`
   ditambah LOKAL di file ini (bukan reuse `MainActivity`, sengaja — sheet ini sudah dari awal
   "self-contained, no dependency on AppLockStore", pola sama dipertahankan utk fitur baru).
   Auto-prompt begitu gerbang PIN vault tampil (persis pola `LaunchedEffect` Kunci Aplikasi di
   `MainActivity.kt`) + tombol manual "Sidik Jari" di `VaultUnlockSection` (fallback kalau
   prompt di-cancel) + toggle Switch "Buka dengan Sidik Jari" baru di `VaultContentSection`
   (tampil hanya kalau `biometricAvailable`, sama syarat dgn toggle Kunci Aplikasi di Settings).

**4 file diubah** (di atas batas normal 3 file/tugas — sengaja dilanggar krn user eksplisit minta
2 hal terpisah [fix regresi + fitur baru] dalam 1 pesan yang sama; masing-masing perubahan tetap
minimal & 1 file [`AndroidManifest.xml`] cuma 1 baris tambahan): `AndroidManifest.xml`,
`LockScreen.kt`, `VaultStore.kt`, `VaultSheet.kt`.

**0 diverifikasi CI/device Batch 484** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren: `VaultSheet.kt` `{}` 119/119 `()` 292/292; `LockScreen.kt` `{}` 49/49 `()` 132/132;
`VaultStore.kt` `{}` 26/26 `()` 116/116; manifest XML divalidasi `xmllint --noout` ✅). **WAJIB
DITEST user**:
1. Settings → Kunci Aplikasi → toggle "Buka dengan Sidik Jari" harus MUNCUL (sebelumnya hilang
   total) di device yang punya sidik jari terdaftar; nyalakan lalu buka app dari cold-start →
   ikon sidik jari + teks "Sidik Jari" harus tampil di pojok kiri-bawah numpad PIN.
2. Settings → Vault (buka pakai PIN vault dulu) → scroll ke bawah panel → toggle "Buka dengan
   Sidik Jari" baru harus muncul, nyalakan → tutup & buka lagi tab Vault → prompt sidik jari
   harus auto-muncul; tombol "Sidik Jari" manual di bawah tombol "Buka" jadi fallback kalau
   prompt itu di-cancel.
3. Device TANPA sidik jari terdaftar: toggle biometrik di Settings maupun di Vault harus TETAP
   sembunyi (bukan crash) — perilaku `biometricAvailable`/`canAuthenticate()` gate, 0 diubah.
4. Pastikan 0 crash/force-close saat build (semua import baru sudah ada dependency-nya:
   `androidx.biometric:biometric:1.1.0` sudah lama ada di `app/build.gradle.kts`, 0 dependency
   baru ditambah).
**[RESUME POINT Batch 485]**: kalau test di atas ✅, kembali ke antrean micro-task polish animasi
Batch 481/482 (resume Batch 483 di bawah — masih valid, 0 berubah oleh batch ini): lanjut
`.animateItem()` ke `SongPickerSheet.kt`/`ABRepeatBookmarkSheet.kt`/`EqualizerSheet.kt` (cek
drag-reorder dulu sebelum sentuh). Sektor pill/tab-sync + gesture-drag custom TETAP paling akhir.

**Catatan Batch 483 [BUG FIX regresi/gap dilaporkan user: "background glass tembus pandang pada
tab vault, fix it dan audit yang gejalanya serupa"]**: root cause TERKONFIRMASI dari perbandingan
kode app-wide (bukan tebakan) — BUKAN regresi dari Batch 481/482 (2 batch itu 0 pernah menyentuh
`ModalBottomSheet`/`Column` root VaultSheet). Gap asli: `containerColor = Color.Transparent`
(pola app-wide, dipasang Batch 322/323 sbg fix "blur lintas-window") WAJIB dipasangkan dgn
`.frostedGlass()` di Column konten supaya panel tetap solid — tanpanya, panel benar-benar
tembus pandang (bukan efek estetika, literal 0 alpha). Audit `.frostedGlass()` Batch 340
("lanjutan antrean Audit tambahan Batch 339") menyisir BackupRestoreSheet/DiagnosticLogSheet/
UpdateCheckSheet/DuplicateFinderSheet dkk, TAPI tidak mencakup 3 file. Audit ulang app-wide batch
ini (grep SEMUA 16 file `ModalBottomSheet`+`containerColor=Transparent` vs SEMUA yang punya
`.frostedGlass()`): tepat 3 file bolong — `VaultSheet.kt` (dilaporkan user), `SmartPlaylistScreen.kt`,
`SignatureMatcherSheet.kt`. 13 file lain sudah benar (0 disentuh).

**3 file diubah** (dalam batas 3 file/tugas — pas): `VaultSheet.kt`, `SmartPlaylistScreen.kt`,
`SignatureMatcherSheet.kt`.
1. Ketiganya: `.frostedGlass()` ditambah ke Column konten (posisi setelah `.fillMaxWidth()`,
   pola PERSIS 13 file lain yang sudah benar), dipanggil TANPA argumen (pola sama 12/12 call
   site existing, 0 parameter/angka baru) + import `com.rudi.audioplayer.ui.theme.frostedGlass`
   ditambah di ketiganya (2 file lain belum pernah import ini sama sekali).
2. `SmartPlaylistScreen.kt`: comment historis Batch 263 (rationale `LocalOverscrollConfiguration
   provides null`) SEMPAT tidak sengaja terhapus draft awal — dikembalikan utuh sebelum commit,
   digabung berurutan dgn comment baru Batch 483 (0 histori hilang).
3. 0 logic lain disentuh di ketiga file (PIN gate, filter draft, ApkSignatureChecker, dsb) — 100%
   scope ini murni 1 modifier tambahan x3 titik yang identik.

**0 diverifikasi CI/device Batch 483** — 0 env Android nyata/compiler Kotlin sesi ini (balance
brace/paren: `VaultSheet.kt` `{}` 101/101 `()` 238/238; `SmartPlaylistScreen.kt` `{}` 106/106
`()` 273/273; `SignatureMatcherSheet.kt` `{}` 58/58 `()` 145/145). **WAJIB DITEST user**:
1. Buka tab Vault (Pengaturan → Vault) → panel harus terlihat solid/tinted (bukan tembus pandang
   ke layar di belakangnya), sama seperti sheet lain (mis. Cari Duplikat).
2. Pengaturan → Perpustakaan → "Buat Playlist Otomatis" → panel solid, DAN scroll masih halus
   0 regresi ke fix Batch 262/263 (bouncy-scroll) yang TIDAK disentuh batch ini.
3. Pengaturan → menu update/signature checker (Pencocok Signature APK) → panel solid.
4. Pastikan 0 crash/force-close saat build (3 import baru, 3 pemanggilan modifier tanpa
   argumen — pola identik 12 call site lain yang sudah lama jalan aman).
**[RESUME POINT Batch 484]**: kalau test di atas ✅, kembali ke antrean micro-task polish animasi
Batch 481/482 (lihat resume Batch 482 di atas — masih valid, tidak berubah oleh fix bug ini):
lanjut `.animateItem()` ke `SongPickerSheet.kt`/`ABRepeatBookmarkSheet.kt`/`EqualizerSheet.kt`
(cek drag-reorder dulu sebelum sentuh, sama seperti Batch 482). Sektor pill/tab-sync +
gesture-drag custom TETAP paling akhir.

**Catatan Batch 482 [lanjutan instruksi eksplisit user Batch 481: "polish semua effect animasi,
transisi, dll. agar mulus like iOS" — user konfirmasi "mantap, move"]**: micro-task 2/3.
Rencana resume Batch 481 sempat menaruh "(a) audit pill/tab" sbg prioritas pertama — SETELAH
digali datanya (grep app-wide), (a) ternyata BUKAN sektor terpisah dari gesture-drag custom
(sama-sama 1 web `navPillIndexAnim`+`glassAlphaAnim`+`tabDragOffset`+`tabBarOverscrollAnim`+
drag-physics), jadi risikonya SAMA dengan item (c) yang sudah ditandai "PALING TERAKHIR" — bukan
diagnosis baru, cuma info yang belum lengkap saat resume ditulis. Urutan disusun ulang
berdasarkan data ini (bukan tebakan): (a) DILEWATI dulu, lanjut ke (b) yang independen dari
drag-physics.

**2 file diubah** (dalam batas 3 file/tugas): `DuplicateFinderSheet.kt`, `VaultSheet.kt`.
1. **`.animateItem()` pada list hapus-lagu** (kedua file, list sudah punya `key` stabil sejak
   awal — prasyarat animateItem sudah terpenuhi, cuma modifier belum pernah dipasang, pola
   PERSIS SAMA `LibraryScreen.kt`/`QueueSheet.kt` yang sudah ada, 0 pola baru): dipilih krn
   KEDUA sheet ini murni delete-list (0 drag-reorder, diverifikasi grep `isDragging`/`draggable`/
   `reorder` = 0 hit di kedua file — aman dari resiko `.animateItem()` "berebut" dgn gesture
   drag seperti kasus `QueueSheet`/`PlaylistScreen`) dan interaksi hapusnya sering berulang
   (cocok literal dgn keluhan "peralihan instant yang mengganggu" user).
   - `DuplicateFinderSheet.kt`: `DuplicateSongRow` (private, 2 titik panggil: grup library +
     grup fisik) dapat parameter baru `modifier: Modifier = Modifier` (default aman, 0 breaking)
     diteruskan ke root `Row`; 2 titik panggil diberi `modifier = Modifier.animateItem()`.
   - `VaultSheet.kt`: 2 `Row` inline di dalam `items{}` (`vaultedSongs` & `candidates`) langsung
     ditambah `.animateItem()` di awal modifier chain — 0 perlu ubah signature apa pun (Row-nya
     sudah inline, bukan composable terpisah).
2. **0 disentuh**: pill/tab sync (dilewati, lihat rasional di atas), gesture-drag mini
   player/queue, easing NavHost (sudah Batch 481), checkbox/toggle/select/hapus logic kedua
   file (0 baris logic berubah, murni tambahan modifier animasi).

**0 diverifikasi CI/device Batch 482** — 0 env Android nyata/device fisik/compiler Kotlin sesi
ini (balance brace/paren `DuplicateFinderSheet.kt`: `{}` 62/62, `()` 131/131; `VaultSheet.kt`:
`{}` 101/101, `()` 222/222). **WAJIB DITEST user**:
1. Vault → tandai/hapus beberapa lagu dari daftar vault → tiap baris yang hilang harus slide+
   fade keluar halus (bukan pop instan), sisa baris di bawahnya geser naik halus mengisi celah.
2. Vault → dialog "Tambah" → tambah lagu dari daftar kandidat → baris yang baru ditambah
   masuk halus, baris lain di daftar kandidat geser mengisi celah yang ditinggalkan.
3. Pengaturan → Cari Duplikat → centang beberapa lagu (grup Library maupun grup File Fisik) →
   hapus → baris yang dihapus slide+fade keluar halus, 0 pop instan, 0 regresi ke centang/
   hapus/pilih-semua yang sudah ada.
4. Pastikan 0 crash/force-close saat build (parameter baru `modifier` di `DuplicateSongRow`,
   default value jadi 0 breaking di titik panggil manapun yang mungkin terlewat).
**[RESUME POINT Batch 483]**: kalau test di atas ✅, lanjut micro-task 3/3: `.animateItem()` ke
sisa sheet sekunder yang masih polos & TERKONFIRMASI 0 drag-reorder — cek dulu satu-satu sebelum
sentuh (`SmartPlaylistScreen.kt`, `SongPickerSheet.kt`, `ABRepeatBookmarkSheet.kt`,
`EqualizerSheet.kt`, `LyricsSheet.kt` — LyricsSheet prioritas RENDAH krn list-nya statis 0 pernah
insert/remove runtime, `.animateItem()` disana 0 banyak berguna). Sektor pill/tab-sync +
gesture-drag custom (mini player/queue) TETAP paling akhir, butuh audit KHUSUS 1 batch penuh
(bukan diselipkan) krn riwayat regresi (Batch 448/477/479) — jangan mulai dari situ tanpa
instruksi eksplisit user utk sektor itu spesifik.

**Catatan Batch 481 [instruksi eksplisit user: "polish semua effect animasi, transisi, dll. agar
mulus like iOS, landai tanpa peralihan instant yang mengganggu"]**: target "semua" terlalu masif
utk 1 batch (>12 file punya `tween(...)` app-wide, lihat audit di bawah) — AUTO-HALT/
MICRO-TASKING dipakai: eksekusi bagian teraman+paling universal dulu (transisi level-NavHost,
kena SETIAP tab switch + setiap push/pop, bukan sektor gesture custom yang rawan desync), sisanya
ke `[RESUME POINT]`.

**2 file diubah** (dalam batas 3 file/tugas): `Motion.kt` (BARU), `MainActivity.kt`.
1. **Token gerak bersama** (`Motion.kt`, file baru — 0 file lama disentuh oleh keberadaannya
   sendiri): `Motion.IosEasing` = `CubicBezierEasing(0.42f, 0f, 0.58f, 1f)`, mendekati kurva
   bawaan iOS `easeInEaseOut` (S-curve simetris) — beda dari default `tween()` Compose
   (`FastOutSlowInEasing`, kurva Material deselerasi-berat, "gaya Android"). Plus 5 konstanta
   durasi (`DURATION_QUICK=150/STANDARD=200/TAB=220/EMPHASIZED=300/SCREEN=350`) — SEMUA alias
   angka yang SUDAH dipakai app-wide (audit `tween()` app-wide Batch 481, bukan angka baru).
2. **Transisi level-NavHost** (`MainActivity.kt`): 9 pemanggilan `tween(...)` di 3 blok —
   (a) NavHost root (fade home/library/settings, `enterTransition`/`exitTransition`/
   `popEnterTransition`/`popExitTransition`, Batch 330), (b) push "stats_dashboard"
   (slide+fade, Batch 331), (c) push "now_playing" (slide+fade, existing) — SEMUA diganti dari
   `tween(N)` polos jadi `tween(Motion.DURATION_X, easing = Motion.IosEasing)`. **0 angka durasi
   diubah** (200→DURATION_STANDARD, 150→DURATION_QUICK, 300→DURATION_EMPHASIZED,
   350→DURATION_SCREEN, murni alias) — **HANYA kurva easing** yang berubah (default
   FastOutSlowIn → IosEasing). Non-breaking murni di lapisan interpolasi, 0 threshold/gesture/
   state logic disentuh.

**SENGAJA TIDAK disentuh batch ini (bagian dari micro-task, BUKAN diabaikan)**: `tween(220)`
sinkron pill/tab (`navPillIndexAnim` + `glassAlphaAnim` + `tabBarDragFocus`, dekat
`CustomNavBarTabItem`/`GlassTabIcon`) — comment kode eksplisit bilang durasi ini "SAMA PERSIS"
antar ≥2 Animatable supaya tiba bersamaan (lihat histori desync Batch 479, root cause beda tapi
gejala sama: pill vs konten kehilangan sync). Ganti easing salah satu tanpa yang lain BERISIKO
desync baru (kurva beda = laju tengah beda meski durasi sama) — butuh audit semua Animatable
tersinkron sekaligus, di luar tunnel-vision batch ini. Juga belum disentuh: kurva easing di
`MiniPlayerBar.kt`/`NowPlayingScreen.kt`/`QueueSheet.kt` (gesture-drag, physics-based, punya
resiko regresi sendiri per histori panjang project ini) dan beberapa sheet sekunder yang belum
pakai `.animateItem()` (`SmartPlaylistScreen`, `VaultSheet`, `BackupRestoreSheet`,
`SongPickerSheet`, `LyricsSheet`, `ABRepeatBookmarkSheet`, `EqualizerSheet`, `DuplicateFinderSheet`,
`RingtoneCutterSheet`).

**0 diverifikasi CI/device Batch 481** — 0 env Android nyata/device fisik/compiler Kotlin di sesi
ini (balance brace/paren/bracket `MainActivity.kt`: `{}` 339/339, `()` 1260/1260, `[]` 3/3;
`Motion.kt` file baru: `{}` 1/1, `()` 15/15). **WAJIB DITEST user**:
1. Buka app → pindah tab Beranda↔Perpustakaan↔Pengaturan → fade transisi harus tetap terasa mulus
   (visual: kurva lebih "landai" di ujung awal/akhir dibanding sebelumnya, bukan langsung tancap
   gas) — 0 regresi durasi/urutan/flicker.
2. Pengaturan → buka Statistik (stats_dashboard) → slide dari kanan, lalu tombol back → slide balik
   ke kanan — harus tetap mulus, 0 patah/lompat di tengah animasi.
3. Tap lagu apapun → Now Playing naik dari bawah (slide+fade) → tombol back / swipe-down → turun
   lagi — harus tetap mulus, 0 regresi ke gesture drag-dismiss (Batch 476/477/480, TIDAK disentuh
   batch ini).
4. Pastikan 0 crash/force-close saat build (import `Motion` baru di `MainActivity.kt`).
**[RESUME POINT Batch 482]**: kalau test di atas ✅, lanjut micro-task berikutnya sesuai urutan
risiko naik: (a) audit SEKALIGUS 3 Animatable tersinkron pill/tab (`navPillIndexAnim`/
`glassAlphaAnim`/`tabBarDragFocus`) lalu upgrade easing bertiga BERSAMAAN (bukan 1-1) kalau aman;
(b) `.animateItem()` utk sheet sekunder yang masih polos (daftar di atas); (c) baru pertimbangkan
gesture-drag custom (`MiniPlayerBar`/`QueueSheet`) — PALING TERAKHIR krn riwayat regresi
terpanjang di sektor ini. Jangan mulai dari (c).

**Catatan Batch 480 [instruksi eksplisit user: "sempurnakan mekanisme drag mini player (feedback,
konfirmasi user, dll)"]**: sektor mini player disentuh SESUAI instruksi eksplisit user batch ini
(bukan tebakan) — item WAJIB DITEST Batch 477 utk sektor ini ("swipe threshold → musik BERHENTI
TOTAL") belum ada laporan device balik dari user, tapi TIDAK diasumsikan gagal (tidak ada laporan
gagal juga) — dianggap baseline aktif-stabil sesuai kaidah "0 laporan gagal = lanjutkan" yang
sudah dipakai project ini utk sektor lain (mis. bottom nav Batch 448 di bawah), fix Batch 477 itu
sendiri 0 disentuh.

**2 file diubah** (dalam batas 3 file/tugas): `MiniPlayerBar.kt`, `PlayerViewModel.kt`.
1. **Feedback visual drag** (`MiniPlayerBar.kt`): `graphicsLayer` yang sudah membaca
   `dismissOffsetPx` utk `translationX` (sejak Batch 476) diperluas baca nilai yang SAMA utk
   `alpha`/`scaleX`/`scaleY` (progress 0→1 di 0→120px, dikapkan di 1 persis di titik threshold —
   drag lebih jauh dari situ TIDAK di-clamp posisinya per desain Batch 476, tapi visual feedback
   berhenti menambah di titik itu) — bar meredup+mengecil halus mengikuti jari, sinyal "akan
   hilang" SELAMA drag, bukan cuma snap di akhir. Dibaca LANGSUNG di lambda `graphicsLayer` (pola
   SAMA PERSIS `translationX` yang sudah ada + rasional Batch 397), jadi 0 recomposition
   tambahan tiap frame drag, cuma invalidasi layer.
2. **Haptic 2-tahap** (`MiniPlayerBar.kt`): tick `TextHandleMove` (beda dari `LongPress` yang
   sudah ada) ditambah SEKALI persis saat `dismissOffsetPx` melewati threshold 120px SELAGI masih
   digeser (flag `thresholdHapticFired` per-gesture) — real-time sinyal "lepas sekarang = batal
   terjadi", terpisah dari `LongPress` yang sudah ada di `!change.pressed` (itu tetap konfirmasi
   dismiss BENERAN terjadi saat jari diangkat, 0 disentuh). 0 threshold/formula/spring gesture yg
   sudah ada (120px, damping, dsb, Batch 476/477) disentuh sama sekali.
3. **Konfirmasi user via Undo** (`PlayerViewModel.kt`): `dismissMiniPlayer()` sebelumnya
   destruktif permanen (stop total + queue+state dikosongkan, 0 jalan balik — lihat komentar
   Batch 476 di fungsi itu). Sekarang snapshot (queue, index, posisi, repeat, shuffle, speed,
   isPlaying) diambil DULU dari controller/uiState di main thread SEBELUM 9 baris clear asli
   (0 diubah sama sekali), lalu dipasang lewat `UndoableAction` — infra yg SUDAH ADA & dipakai
   `removeFromQueue()`/`removeSongFromPlaylist()`/`removeAutoPlaylist()` (0 API baru, 0 pola
   baru), otomatis muncul sbg Snackbar "Urungkan" lewat `LaunchedEffect(undoableAction)` yang
   SUDAH ADA di `MainActivity.kt` — **0 baris `MainActivity.kt` diubah sama sekali** (di luar
   hitungan 2 file di atas, bukan salah satu dari 3 file/tugas). Fungsi baru
   `restoreDismissedPlayback()` (private) pulihkan lewat `playQueue()` yang sudah ada, pola sama
   persis `resumeFromSaved()` (shuffle/repeat diset SEBELUM `playQueue()`, alasan sama).

**0 diverifikasi CI/device Batch 480** — 0 env Android nyata/device fisik/compiler Kotlin di sesi
ini (balance brace/paren/bracket kedua file: `MiniPlayerBar.kt` `{}` 32/32 `()` 180/180, tanpa
`[]`; `PlayerViewModel.kt` `{}` 240/240 `()` 971/971 `[]` 37/37). **WAJIB DITEST user**:
1. Swipe mini player kiri/kanan PELAN (belum lepas jari) → bar meredup+mengecil halus mengikuti
   jari; tick getar HALUS terasa SEKALI persis saat lewat titik ~120px (sebelum lepas jari).
2. Lepas jari SETELAH tick itu → bar hilang seperti biasa (getar `LongPress` seperti sebelumnya)
   DAN Snackbar "\"<judul lagu>\" dihentikan" + tombol "Urungkan" muncul di bawah.
3. Tap "Urungkan" pada Snackbar itu → lagu yang sama main lagi persis dari posisi/repeat/
   shuffle/kecepatan sebelum di-dismiss (bukan dari awal lagu / posisi 0).
4. Swipe PELAN tidak sampai threshold, lepas jari → bar pegas balik ke posisi semula seperti
   biasa (regresi Batch 476/477), 0 tick/Snackbar muncul (tick HANYA saat threshold terlampaui).
5. Tap biasa (0 geser) di mini player MASIH buka Now Playing seperti biasa — 0 regresi.
Kirim hasil test ini balik sebelum sektor mini player disentuh lagi.

**Catatan Batch 479 [user kirim log Diagnostik sesuai WAJIB RESUME POINT Batch 478]**: root cause
bug (2) Batch 477 ("drag lintas tab, bottom nav diam") KETEMU dari data log (bukan tebakan) — 4/4
baris `TabSwipe` WAJIB (content Box composed, pointerInput coroutine mulai, onDragStart,
onHorizontalDrag PERTAMA) ADA, `onDragEnd` konsisten hitung `targetRoute` benar tiap gesture (4x
drag berturut Pengaturan→Perpustakaan→Beranda→Perpustakaan→Pengaturan, semua sukses) —
MEMBUKTIKAN gesture+navigate() jalan 100% normal, root cause BUKAN di situ. Sebenarnya:
`navPillIndexAnim` (Animatable posisi "rest" pill bottom nav, Batch 448) sebelumnya HANYA
disinkronkan dari 2 jalur — onClick 3 `NavigationBarItem` & selesai-drag-LANGSUNG-di-tab-bar
(Batch 444) — swipe KONTEN (Batch 435/477, blok terpisah di luar `bottomBar=`) manggil
`navController.navigate()` LANGSUNG tanpa pernah menyentuh `navPillIndexAnim`, jadi
currentRoute+konten berpindah benar tapi pill visual TIDAK PERNAH ikut.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt`.
1. Instrumentasi Batch 478 (6 `AppLogger.w` + import) DICABUT TOTAL sesuai mandat WAJIB DICABUT
   begitu root cause ketemu — 0 log/instrumentasi tersisa.
2. `navPillIndexAnim` DIPINDAH (hoisted) dari scope lokal `bottomBar=` ke scope `AppNavHost`
   (sejajar `tabSwipeScope`/`tabDragOffsetPx`/`tabDragOffset`) supaya lambda `content=` (Scaffold)
   bisa ikut baca/tulis — pola hoist SAMA PERSIS `currentRouteState`/Batch 442 & `tabSwipeScope`
   yang sudah ada, 0 pola baru.
3. `onDragEnd` blok swipe KONTEN (setelah `navController.navigate(targetRoute)` sukses) ditambah 1
   panggilan `navPillIndexAnim.animateTo(index+0.5f, tween(220))` — pola tween(220) IDENTIK 3
   onClick `NavigationBarItem`, 0 formula/angka baru. 0 `LaunchedEffect(currentRoute)` generik
   ditambah (tetap dihindari sesuai rasionalisasi asli Batch 448 — cegah race start-animasi ganda
   antar 3 jalur sync).
4. 0 formula/threshold 120px/damping 0.3f/clamp ±40px/spring MediumBouncy-Low Batch 435/437/477
   disentuh. 0 logic tab-bar-drag (Batch 442/444/448) disentuh.

**0 diverifikasi CI/device Batch 479** — 0 env Android nyata/device fisik/compiler Kotlin di sesi
ini (balance brace/paren/bracket file penuh: `{}` 339/339, `()` 1251/1251, `[]` 3/3). **WAJIB
DITEST user**: swipe horizontal di KONTEN (bukan di atas tab-bar) di Beranda/Perpustakaan/
Pengaturan → pill bottom nav HARUS ikut berpindah ke tab tujuan setelah gesture selesai (bukan
cuma konten yang berganti) — baik utk 1 tab loncat maupun drag kontinu lintas >1 tab. Kirim hasil
balik sebelum sektor tab-swipe/bottom-nav disentuh lagi.

**Catatan Batch 478 [user konfirmasi Batch 477 fix (2) BELUM menyelesaikan masalah: "drag lintas
tab, bottom nav masih diam" — dikonfirmasi ULANG bahkan di tab Pengaturan (nyaris 0 elemen
horizontal-scrollable di sana, jadi teori LazyRow-menelan-drag Batch 477 TERSINGKIR sebagai
penjelasan tunggal)]**: 0 fix baru dikerjakan batch ini — pola PERSIS Batch 463 (0 tebak fix lagi
tanpa data, instrumentasi dulu). **1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt`.
6 titik `AppLogger.w("TabSwipe", ...)` ditambah (import `AppLogger` baru): (1) `LaunchedEffect
(currentRoute)` tepat sebelum Box konten — LEPAS dari `isOnTabRoute`, jadi ABSENnya baris ini
sendiri di Log Diagnostik sudah sinyal (composable ini sendiri 0 ke-invoke); (2) baris pertama
badan `pointerInput(Unit)` — konfirmasi coroutine gesture BENAR mulai + `isOnTabRoute`/`boxSize`
saat itu; (3) `onDragStart`; (4) `onHorizontalDrag` (SEKALI per gesture via flag `firstDragLogged`
— bukan tiap event, supaya 0 membanjiri log); (5) `onDragEnd` (totalTabDrag + targetRoute
terhitung); (6) `onDragCancel`. **0 formula/logic gesture diubah sama sekali** — WAJIB DICABUT
lagi begitu root cause ketemu, bukan instrumentasi permanen.
**WAJIB DARI USER SEBELUM CODING FIX APA PUN LAGI** (lihat `[RESUME POINT]`): reproduksi PERSIS
di tab Pengaturan (drag horizontal apa saja di situ), lalu Settings → Lanjutan → Log Diagnostik →
cari baris `TabSwipe` → kirim balik PERSIS baris mana yang muncul (atau konfirmasi 0 ada sama
sekali). **JANGAN tebak fix lagi tanpa log ini** (pola terlarang eksplisit, sama seperti "PELAJARAN
PROSES Batch 461→462→463").


mini player Batch 476 0 berefek sama sekali; (2) drag lintas-tab konten 0 gerakkan bottom nav]**:
2 bug DI LUAR sektor mana pun yang tertutup, ditemukan lewat REVIEW KODE (bukan tebakan) — root
cause KEDUANYA sudah pernah didokumentasikan sebagai pola bahaya di file yang sama, tapi belum
diterapkan ke titik yang kena batch ini.

**2 file diubah** (dalam batas 3 file/tugas): `MiniPlayerBar.kt`, `MainActivity.kt`.
- **(1) Mini player swipe-cancel 0 berefek**: root cause SAMA PERSIS Batch 350
  (NowPlayingScreen.kt) — `pointerInput{detectHorizontalDragGestures}` (Batch 476) & `.clickable
  (onExpand)` 2 gesture recognizer terpisah bersaing 1 titik sentuh tanpa wasit, SAMA-SAMA pass
  Main (default) → `.clickable()` (lebih dalam di chain) SELALU proses change LEBIH DULU sebelum
  drag detector (lebih luar) sempat consume() gilirannya — clickable TIDAK PERNAH lihat change
  ter-consume, tap menang mutlak, swipe 0 efek visual sama sekali. Fix: `detectHorizontalDrag-
  Gestures` diganti loop manual (`awaitEachGesture`/`awaitFirstDown`/`awaitPointerEvent`) didaftar
  di `PointerEventPass.Initial` (jalan sebelum Main manapun) — pola IDENTIK Batch 350, 0 teori
  baru. Sebelum touchSlop terlampaui 0 consume (tap+ripple `.clickable()` tetap utuh, 0 regresi
  item (c) resume point Batch 476); begitu terlampaui baru consume di sini, `.clickable()` auto-
  cancel sendiri (mekanisme baku Compose). Formula threshold 120px/spring MediumBouncy-Low/haptic
  Batch 476 0 disentuh.
- **(2) Drag lintas-tab 0 gerakkan bottom nav**: root cause — `pointerInput(currentRoute)` (Batch
  435, drag DI KONTEN layar, BUKAN drag-di-atas-bar-tab Batch 442 yang terpisah/sudah benar) di-key
  string yang BERUBAH begitu `navigate()` jalan di `onDragEnd` → Compose cancel+restart coroutine
  gesture di komposisi berikutnya. Utk drag 1 gesture yang cuma lompat 1 tab: 0 kelihatan (restart
  terjadi SETELAH gesture kelar). Utk **drag kontinu yang lintas >1 batas tab tanpa angkat jari**:
  begitu batas tab pertama terlewati, instance lama mati, instance baru cuma `awaitFirstDown()` —
  down utk jari yang SUDAH menekan sejak awal TIDAK PERNAH datang lagi, SISA drag itu 0 diproses
  (tabDragOffsetPx/tabMagnifyFocus/nudge konten beku total, pill/label bottom nav 0 gerak lagi) —
  PERSIS bug class yang sudah didokumentasikan sbg alasan `currentRouteState`/key-`Unit` di
  `pointerInput` drag-di-atas-bar Batch 442 (komentar di dekat deklarasi `homeTabInteraction`),
  tapi belum diterapkan ke titik Batch 435 ini. Fix: key diganti `Unit` + instance BARU
  `rememberUpdatedState(currentRoute)` scope-lokal (`currentRouteState` Batch 442 TIDAK bisa dipakai
  ulang — scope-nya di lambda `bottomBar=` Scaffold, di luar jangkauan lambda `content=` ini). 0
  formula/threshold 120px/damping 0.3f/clamp ±40px Batch 435/437 disentuh.
- **CATATAN BELUM DIVERIFIKASI (teori sekunder, item (2))**: video bukti user menunjukkan titik
  sentuh kemungkinan di ATAS row horizontal (`Mix`/`Favorit` Beranda, atau chip filter Library) —
  row itu LazyRow yang scrollable sendiri, bisa "menelan" drag horizontal duluan (child menang
  Main-pass) SEBELUM sampai ke pointerInput ancestor manapun, independen dari fix keying di atas.
  Fix batch ini TIDAK menyentuh soal ini (butuh konfirmasi user dulu titik sentuh persis — pola
  "JANGAN tebak tanpa data" konsisten sepanjang project). Lihat `[RESUME POINT]`.
- **0 diverifikasi CI/device Batch 477** — review manual (balance brace/paren/bracket: lihat
  `CHANGELOG.md` § Batch 477 untuk angka persis tiap file), 0 env Android nyata/device fisik/
  compiler Kotlin di sesi ini. **WAJIB DITEST user** (lihat `[RESUME POINT]` di bawah).

**Catatan Batch 476 [instruksi baru user: mini player bisa dicancel + sinkronisasi eksternal/
cold-start ke Mini Player Bar]**: 2 permintaan baru user, DI LUAR sektor bubble Batch 475 (target
sekarang `MiniPlayerBar.kt`/`PlayerViewModel.kt` — UI dalam-app, bukan floating bubble luar-app).

**3 file diubah** (dalam batas 3 file/tugas): `PlayerViewModel.kt`, `MiniPlayerBar.kt`,
`MainActivity.kt`. Detail lengkap: `CHANGELOG.md` § Batch 476.
- **(1) Mini player bisa dicancel**: swipe horizontal (kiri/kanan, threshold 120px — konstanta &
  pola gesture SAMA PERSIS dgn swipe next/prev album art `NowPlayingScreen.kt`: Animatable
  snapback, spring dampingRatio MediumBouncy/stiffness Low, 0 API baru diperkenalkan) pada
  `MiniPlayerBar` memanggil `onDismiss` → `PlayerViewModel.dismissMiniPlayer()`: `controller.stop()`
  + `clearMediaItems()`, `_uiState`/`_playbackProgress` direset ke default, DAN
  `playbackStateStore.save(songIds = emptyList(), ...)` (fungsi `save()` yang SUDAH ADA, bukan
  method baru) — `load()` balik null utk `songIds` kosong (string kosong → 0 id tervalidasi),
  PERSIS kondisi "0 saved state" yang sudah ditangani `resumeFromSaved()`/`peekSavedSong()`. Tanpa
  langkah ini, sync (2) di bawah akan membangkitkan lagi lagu yang baru saja di-cancel user pada
  proses berikutnya.
- **(2) Sinkron eksternal/cold-start ke Mini Player Bar**: root cause — `_uiState.currentSong`
  SEBELUMNYA cuma pernah diisi lewat `onMediaItemTransition` (event, hanya nyala saat item
  BERGANTI) atau aksi eksplisit (`playQueue` dkk), TIDAK PERNAH dari status controller yang SUDAH
  berjalan tepat di titik `connect()` — begitu Activity/proses baru muncul sementara
  PlaybackService (session) sudah/masih punya media item aktif, mini bar tetap kosong ("reset")
  sampai ada transisi lagu berikutnya. Fix: `maybeSyncMiniPlayerOnColdStart()` (dipanggil dari 2
  titik — `connect()` & tail sukses `refreshLibrary()` — menutup race urutan mana pun yang selesai
  duluan; dikunci `coldStartSyncDone` supaya PERSIS 1x per proses). 2 cabang: (a)
  `controller.mediaItemCount > 0` (Service tetap hidup — app-kill/backgrounding biasa, ATAU
  playback dimulai dari luar UI app: widget/headset/bubble/lock screen) → state disinkronkan
  LANGSUNG dari controller (mediaId → Song lewat `_librarySongs`), 0 `play()`/`pause()`/`seekTo()`
  dipanggil sama sekali — 0 interupsi ke audio yang sedang berjalan; (b) `mediaItemCount == 0`
  (Service ikut mati total) → reuse `resumeFromSaved(autoPlay = false)` yang SUDAH dipakai jalur
  shortcut "Continue Listening"/tombol Resume HomeScreen — mini player muncul (paused) TANPA
  auto-play mengejutkan.
- **0 diverifikasi CI/device Batch 476** — review manual (balance brace/paren/bracket: lihat
  `CHANGELOG.md` § Batch 476 untuk angka persis tiap file), 0 env Android nyata/device fisik/
  compiler Kotlin di sesi ini. **WAJIB DITEST user** (lihat `[RESUME POINT]` di bawah untuk daftar
  lengkap) sebelum sektor ini dianggap tuntas.

**Catatan Batch 475 [reopen eksplisit user: sinkronisasi cold-start/persistent bubble <-> player
setelah app-kill]**: user eksplisit buka ulang sektor "survive app-kill" (DITUTUP informal Batch
474) dengan instruksi spesifik: "lakukan sinkronisasi mekanisme cold-start/persistent antara
fitur bubble dan eksternal SONIX player after kill the app". Diizinkan per "Aturan sesi aktif" #6
(instruksi eksplisit spesifik minta sektor dibuka lagi, BUKAN instruksi generik "next"/"lanjut").

**2 file diubah** (dalam batas 3 file/tugas): `PlaybackService.kt`, `FloatingBubbleService.kt`.
Detail lengkap: `CHANGELOG.md` § Batch 475. Ringkas: grep seluruh project menemukan 5 titik
`startForegroundService()` yang menyalakan bubble/PlaybackService — cuma 2 (`BubbleBootReceiver.kt`
Batch 466, `FloatingBubbleService.onTaskRemoved` Batch 471) yang dibungkus `runCatching` setelah
device nyata user MEMBUKTIKAN `ForegroundServiceStartNotAllowedException` bisa terjadi walau API
resmi sudah benar. 2 titik LAIN — justru paling relevan ke sinkronisasi bubble<->player pasca
app-kill (`PlaybackService.maybeStartFloatingBubble()`, dipanggil `onIsPlayingChanged(true)` =
funnel bubble menyala balik saat playback resumption eksternal berhasil pasca app-kill total; &
`FloatingBubbleService.sendPlaybackAction()` fallback, arah kebalikan — bubble memicu cold-start
PlaybackService saat tap tombol dalam keadaan cold) — TIDAK ikut terlindungi. Fix: kedua titik
disamakan ke pola `runCatching` yang sama, 0 logic/formula/state lain diubah. Titik ke-5
(`BubbleTileService.kt`) SENGAJA tidak disentuh — tap QS tile user-initiated, exempt dari
background-start restriction, beda kelas risiko.

**0 diverifikasi CI/device Batch 475** — review manual (cek balance brace/paren/bracket:
`PlaybackService.kt` `{}` 80/80 `()` 427/427 `[]` 19/19; `FloatingBubbleService.kt` `{}` 108/108
`()` 730/730 `[]` 204/204), 0 env Android nyata/device fisik/compiler Kotlin di sesi ini. Kondisi
ini SULIT direproduksi sengaja (butuh device dengan restriksi OEM aktif tepat di momen resumption
eksternal) — validasi utama dari user: pastikan 0 regresi ke perilaku normal (bubble tetap nyala
saat playback dipicu widget/headset/Bluetooth/lock-screen seperti Batch 473, tap tombol bubble
cold-start tetap memicu restore seperti Batch 470).

**Catatan Batch 474 [jawaban user: tutup (a) survive app-kill + fitur baru (b) drag vs tap
tombol]**: user jawab 2 poin WAJIB Batch 471/472 sekaligus.

(a) **Survive app-kill — DITUTUP atas permintaan eksplisit user**: "ya. lupakan, sudah bisa
dipicu pakai pemutar SONIX eksternal/malas nunggu bermenit-menit jadi shut up". Ini konfirmasi
INFORMAL (bukan protokol lengkap swipe-Recents-lalu-tunggu-beberapa-menit yang diminta Batch 471)
— user eksplisit menolak lanjut test formal itu (verifikasi lewat trigger-play-dari-sumber-luar
dianggap cukup olehnya) dan **eksplisit minta topik ini TIDAK ditanyakan lagi**. Sesuai instruksi
eksplisit user: item ini DITUTUP di sini, **JANGAN munculkan lagi permintaan test survive-Recents
formal kecuali user sendiri yang buka ulang topiknya**. 0 kode diubah untuk poin ini (murni
perubahan status dokumentasi — tidak ada fix baru yang diminta/diperlukan).

(b) **Drag vs tap tombol kontrol — FITUR BARU eksplisit user**: "bisa gak drag nya diutamakan
dibanding akses tap nya?!!" — user TIDAK menjawab pertanyaan sempit Batch 472 (konfirmasi sisi
kanan lega sampai tombol minimize) secara langsung, malah lanjut ke permintaan lebih luas: drag
yang dimulai PERSIS di atas salah satu dari 4 tombol kontrol (play/pause/prev/next/minimize)
sebelumnya 0 pernah ikut memindah bubble sama sekali (bukan cuma "kalah prioritas" — dispatch
touch Android baku memegang SELURUH sequence 1 pointer ke SATU view yang menangkap `ACTION_DOWN`,
jadi drag yang dimulai di tombol 100% milik tombol itu, tidak pernah "diteruskan" ke mana pun).
**Pertanyaan sempit Batch 472 dianggap TIDAK TERJAWAB EKSPLISIT** (bukan ditolak, bukan
dikonfirmasi) — SOP larang asumsi tanpa data, jadi status "sisi kanan lega sampai minimize"
TETAP "belum dikonfirmasi user" (lihat RESUME POINT), meski implisit konsisten (kalau area dead-
space itu masih terasa sempit, permintaan (b) besar kemungkinan akan menyebut itu duluan).

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. **`setupDrag()` digeneralisasi**: param baru `onTap: () -> Unit` (default = perilaku LAMA
   persis `if (isMinimized) expand() else openApp()`) — 0 breaking change ke 2 pemanggil lama
   (album art, tab minimized), argumen mereka TIDAK perlu diubah sama sekali karena default
   identik. Logic drag (TOUCH_SLOP, clamp screenBounds, readback Batch 469, snap-tepi Batch 100)
   TIDAK disentuh SAMA SEKALI — cuma cabang tap di `ACTION_UP` yang sekarang manggil `onTap()`
   alih-alih hardcode.
2. **`setupControls()` dipasangi `setupDrag()` juga**: ke-4 `ImageButton` (play_pause/prev/next/
   minimize) sekarang JUGA jadi `touchSource` untuk `setupDrag()` (param `onTap` di-override
   manggil `performClick()` milik tombol itu sendiri) — `setOnClickListener` yang SUDAH ADA
   (dengan body `keepAwakeAndScheduleFade()` + `sendPlaybackAction`/`minimize()`) **TIDAK
   disentuh/dipindah sama sekali**, cuma sekarang dipicu lewat `performClick()` bukan lewat
   `View.onTouchEvent` bawaan — 1 satu-satunya sumber kebenaran aksi tombol tetap listener yang
   sama, 0 duplikasi logic. Signature `setupControls()` berubah (`view` saja → `view, windowView,
   params`) supaya bisa meneruskan `container`/`params` yang sama ke `setupDrag()` — 1 titik
   panggil (`addBubbleView()`) ikut diupdate.
3. **Efek samping jujur, disengaja**: bunyi klik sistem Android bawaan (dipicu `View.
   onTouchEvent` default, bukan `performClick()`) tidak lagi terdengar saat tap ke-4 tombol itu —
   konsisten filosofi iOS-look proyek ini (ripple Android sudah dimatikan di bottom nav, Batch
   439), BUKAN regresi. 0 drawable/style tombol disentuh (background statis, 0 `state_pressed`
   yang hilang).
4. 0 formula clamp/posisi/`screenBounds` disentuh (di luar cakupan guard Batch 469), 0 sektor
   DITUTUP disentuh.

**0 diverifikasi CI/device Batch 474** — review manual (baca kode + cek balance brace/paren/
bracket: `{}` 106/106, `()` 725/725, `[]` 202/202 di `FloatingBubbleService.kt`), 0 env Android
nyata/device fisik/compiler Kotlin di sesi ini. **WAJIB dari user** (lihat RESUME POINT untuk
daftar lengkap): (1) drag dimulai dari salah satu 4 tombol kontrol sekarang memindah bubble; (2)
tap biasa (tanpa gerak) di ke-4 tombol itu masih berfungsi 100% normal (0 regresi fungsi
play/pause/prev/next/minimize); (3) drag dari album art & tab minimized (2 titik lama, TIDAK
disentuh batch ini) tetap seperti Batch 472.

**Catatan Batch 473 [0 file diubah — klarifikasi murni, tutup poin 3 Batch 472]**: user jawab tap
pilihan eksplisit untuk klarifikasi "bubble tetap muncul saat player eksternal dimainkan" (dibuka
Batch 472 poin 3) — **maksud (a) DIKONFIRMASI**: bubble SONIX tetap tampil walau lagu dipicu main
dari LUAR UI app (headset/Bluetooth/Android Auto/widget home-screen), BUKAN (b) bubble
menampilkan sesi app lain. Ini **PERILAKU YANG DIHARAPKAN, BUKAN bug** — `PlaybackService`
(`MediaLibraryService`) merespons trigger play dari sumber mana pun (tombol fisik headset,
Bluetooth AVRCP, Android Auto, widget home-screen) via jalur session/Intent yang SAMA, bubble
cuma mencerminkan state `PlaybackService` itu sendiri lepas dari APA yang memicunya — konsisten
dengan `SessionToken` yang HANYA konek ke `PlaybackService` app ini sendiri (lihat "Kontrol/state"
KDoc kelas `FloatingBubbleService.kt`). **0 kode diubah** — poin 3 Batch 472 RESMI DITUTUP, 0
kandidat bug lagi di sektor ini. Sektor bubble (Roadmap #11) masih terbuka untuk item lain
(investigasi Batch 469 & test survive-Recents Batch 471/472 di bawah, keduanya BELUM tersentuh
batch ini).

**Catatan Batch 472 [1 laporan user: drag masih "sempit" pasca-471 + 2 klarifikasi survive
app-kill]**: 0 tumpang tindih investigasi Batch 469 (screenBounds/clamp/posisi 0 disentuh). **1
file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. **Drag mentok penuh ke tepi kanan**: `rect.right` TouchDelegate `bubble_album_art` sekarang
   dinamis = `expanded.width` (tepi kanan `bubble_root` sesungguhnya, melewati SELURUH badan 4
   tombol kontrol) — gantikan `ALBUM_ART_TOUCH_PAD_RIGHT_DP` (Batch 470/471, dicabut, cuma sampai
   gap sebelum `bubble_prev`) yang terbukti belum cukup lega. Aman krn alasan yang SAMA dibuktikan
   Batch 471: tombol selalu menang bounds sendiri di dispatch Android baku, lepas dari overlap
   rect delegate. Kiri/atas/bawah TIDAK disentuh (sudah maksimal fisik sejak Batch 471).
2. **Dialog battery-optimization (pertanyaan Batch 471) DIKONFIRMASI user**: muncul PERSIS sesuai
   desain — hanya terpicu saat user toggle bubble OFF→ON manual, 0 nge-nag di luar alur itu. 0
   kode diubah — murni sinkronisasi status dokumentasi.
3. **Survive app-kill (swipe Recents, pertanyaan Batch 471) BELUM ditest user** — user melaporkan
   1 observasi DI LUAR protokol diminta: "bubble tetap muncul saat player eksternal dimainkan".
   Makna AMBIGU — arsitektur [MediaController] bubble HANYA konek ke `PlaybackService` app ini
   sendiri lewat `SessionToken(ComponentName(this, PlaybackService::class.java))`, 0 jalur kode
   yang bisa menampilkan sesi/metadata app lain. Kandidat makna: (a) bubble tetap tampil walau
   pemicu play datang dari luar UI app (Bluetooth/headset/Android Auto/widget) — WAJAR & sesuai
   desain kalau ini maksudnya; (b) bubble justru menampilkan kontrol/metadata App LAIN (bug nyata
   kalau ini maksudnya, tapi TIDAK didukung baca-kode manapun saat ini). **SOP larang tebak fix
   tanpa data** — 0 kode disentuh, WAJIB klarifikasi dulu dari user (lihat RESUME POINT) SEBELUM
   coding apa pun ke sektor ini, dan test swipe-dari-Recents (yang belum pernah dilakukan) tetap
   WAJIB dari user terlepas dari klarifikasi ini.

**0 diverifikasi CI/device Batch 472** — review manual (baca kode + cek balance brace/paren:
`{}` 102/102, `()` 683/683, `[]` 184/184 di `FloatingBubbleService.kt`), 0 env Android nyata/
device fisik/compiler Kotlin di sesi ini. Perlu dari user: (1) install APK baru, test drag dari
album art lagi — sisi kanan sekarang harus lega sampai ke tombol minimize; (2) jawab klarifikasi
poin 3 di atas; (3) LANJUTKAN test survive app-kill yang masih tertunda (swipe SONIX dari Recents,
BUKAN Force Stop manual, setelah dialog battery-optimization di-grant) — cek bubble/notifikasi
masih ada beberapa menit kemudian.

**Catatan Batch 471 [2 laporan user: drag "sulit/terbatas" + "bubble survive 100%?"]**: 0
tumpang tindih investigasi Batch 469 (screenBounds/clamp/posisi 0 disentuh). **3 file diubah**
(pas batas 3 file/tugas): `FloatingBubbleService.kt`, `AndroidManifest.xml`, `MainActivity.kt`.
1. **Drag diperlebar sisi kanan saja**: `ALBUM_ART_TOUCH_PAD_RIGHT_DP` 3f→6f (full gap asli ke
   `bubble_prev`, terbukti aman krn tombol selalu menang bounds sendiri lepas dari overlap rect
   delegate). Kiri/atas/bawah SUDAH maksimal secara fisik (padding riil `bubble_root` cuma 8dp,
   `ALBUM_ART_TOUCH_PAD_DP`=10f sudah melebihi itu) — perbaikan lanjutan (naikkan padding XML
   `bubble_mini_player.xml`) BELUM diterapkan, WAJIB tunggu konfirmasi user dulu apakah perbaikan
   sisi-kanan-saja ini sudah cukup atau masih perlu breathing room lebih besar.
2. **Bubble survive app-kill**: ditambah `onTaskRemoved` (re-assert foreground defensif, pola
   `runCatching` sama `BubbleBootReceiver`) + `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
   (diminta HANYA saat user aktif toggle ON, 0 nge-nag tiap buka app). **BATAS JUJUR eksplisit
   ke user**: "100%" TIDAK bisa dijanjikan kode manapun — bukti Batch 466 (device user KENA OEM
   App Standby/battery restriction walau API resmi sudah benar). Kalau OEM masih agresif setelah
   grant dialog battery-optimization ini, sisanya (autostart/protected-apps proprietary OEM) di
   luar jangkauan kode sama sekali, WAJIB user whitelist manual di pengaturan HP masing-masing.

**Catatan Batch 470 [2 fitur baru dari user, 0 tumpang tindih investigasi Batch 469]**: user
konfirmasi kliping landscape SEKARANG bekerja ("sudah bisa kliping dalam mode landscape
sekalipun") — konfirmasi UMUM, BUKAN reproduksi protokol spesifik Batch 469 (drag ke tepi
nav-bottom + JANGAN rotasi balik + ekspor Log Diagnostik) yang masih belum pernah dikirim.
Regresi "MENGHILANG TOTAL" Batch 469 karena itu **BELUM RESMI dianggap selesai** (juga belum
terbantahkan) — kalau device fisik user memicunya lagi, protokol reproduksi Batch 469 di bawah
TETAP berlaku, minta log itu duluan sebelum coding fix apa pun ke [screenBounds]/formula clamp.
User mengarahkan sesi ke 2 permintaan baru, **0 pernah menyentuh [screenBounds]/formula
clamp/posisi** (aman dari investigasi Batch 469 yang masih terbuka):
1. **Cold-start bubble**: bug ditemukan lewat review kode (bukan laporan user eksplisit) —
   `FloatingBubbleService.sendPlaybackAction()` pakai panggilan `MediaController` langsung begitu
   `controller != null`, TANPA cek `mediaItemCount`. Di boot murni (bubble auto-start lewat
   `BubbleBootReceiver`, user belum pernah buka app/widget), controller lokal bisa konek DULUAN
   (bind doang) SEBELUM antrean sempat di-restore → `hasQueue` nge-latch `false` selamanya →
   SETIAP tap play/prev/next di bubble jatuh ke `openApp()`, padahal `PlaybackService` sudah
   punya jalur cold-start-restore (dipicu Intent `onStartCommand`, BUKAN panggilan controller
   langsung). Fix: syarat `c.mediaItemCount > 0` sebelum pakai controller langsung; kalau tidak,
   fallback ke Intent yang sama seperti widget (yang memang tidak pernah kena celah ini).
2. **Drag & tap-buka-app discoped ke `bubble_album_art` saja**: sebelumnya seluruh badan pill
   (termasuk padding kosong `bubble_root`, bukan cuma area ke-4 tombol yang memang sudah aman)
   jadi pemicu drag/buka-app. Sekarang `setupDrag` dipanggil 2x terpisah (minimized tab: 0
   berubah; expanded: scoped ke `bubble_album_art`), diperluas via `TouchDelegate` (hit-test
   SAJA, 0 perubahan layout/dp XML) biar tetap gampang disentuh. Jangkauan GERAK drag (setelah
   tersentuh) tetap bebas penuh ke seluruh layar seperti sebelumnya — cuma titik awal sentuh sah
   yang berubah.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`. Detail lengkap KDoc
"Batch 470" di file itu. **0 diverifikasi CI/device** — review manual (baca kode + cek balance
`{}`/`()`/`[]`: 99/99, 640/640, 160/160), 0 env Android nyata/compiler Kotlin di sesi ini —
**WAJIB dari user**: install APK baru, test (a) tap play/prev/next di bubble SEGERA setelah
reboot HP tanpa buka app/widget dulu (cold-start), (b) coba drag mulai dari padding kosong pill
(bukan album art/tombol) → pastikan TIDAK lagi memicu drag/buka-app, (c) drag mulai dari album
art (termasuk sedikit di luar 40dp-nya) → pastikan MASIH memicu drag seperti biasa, jangkauan
gerak tetap bebas ke seluruh layar.

**Catatan Batch 469 [REGRESI BARU pasca-468 — instrumentasi, BUKAN fix lagi]**: user laporkan
temuan baru — bubble MENGHILANG TOTAL saat di-drag ke tepi landscape yang berbeda (sisi
nav-bar-bottom), balik normal HANYA kalau device dirotasi ke portrait lagi. Gejala BARU, LEBIH
PARAH dari sebelum Batch 468 (dulu cuma "kurang ter-klip" — kosmetik; sekarang bisa hilang
total/unreachable — fungsional). Detail root-cause reasoning lengkap: KDoc "Batch 469" di
`FloatingBubbleService.kt`. Ringkas dugaan (BELUM DIPASTIKAN, masih teori): `screenBounds` diisi
dari 2 API BERBEDA tergantung kapan dipanggil — `onCreate` pakai `currentWindowMetrics.bounds`,
`onConfigurationChanged` pakai `newConfig.screenWidthDp/HeightDp * density` (keputusan sah Batch
461 demi alasan freshness/timing) — 2 API ini TIDAK dijamin identik secara semantik. Sebelum
Batch 468 (posisi content-area-relative) potensi selisih ini "aman"; sejak Batch 468 (posisi
full-screen-absolute) selisih itu bisa mendorong posisi clamp keluar dari layar nyata.

**KEPUTUSAN**: TIDAK menebak fix ke-5 (SOP eksplisit larang pola ganti-ganti API metrics tanpa
data, "PELAJARAN PROSES Batch 461→462" — apalagi ini regresi ke-2 di file sama minggu ini).
**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` — **0 formula/clamp/
posisi diubah SAMA SEKALI**, murni 2 titik log baru:
1. `onConfigurationChanged`: log `currentWindowMetrics.bounds` (read-only, pembanding) di
   sebelah `screenBounds` yang BENAR-BENAR dipakai (dari `newConfig`) — kalau 2 angka beda, bukti
   langsung teori di atas.
2. `setupDrag` `ACTION_UP` (drag manual — path yang SEBELUMNYA 0 instrumentasi sama sekali, beda
   dari `snapMinimizedToNearestEdge` yang sudah ter-instrumentasi sejak Batch 463): log target
   akhir drag + `screenBounds` yang dipakai clamp + readback `getLocationOnScreen()` +250ms.

**0 diverifikasi CI/device Batch 469** — review manual (baca kode + cek balance brace/paren:
`{}` 96/96, `()` 571/571, `[]` 111/111 di `FloatingBubbleService.kt`), 0 env Android nyata/device
fisik/compiler Kotlin di sesi ini. **Perlu dari user (protokol reproduksi SPESIFIK)**: ulangi
skenario PERSIS (landscape, drag bubble minimized ke tepi nav-bar-bottom sampai hilang) →
**JANGAN rotasi balik ke portrait dulu** (itu "memperbaiki" gejala TAPI JUGA menghapus jendela
diagnostik yang relevan) → langsung ekspor Log Diagnostik dalam keadaan itu → kirim ke sini.

**Catatan Batch 468 [FIX bubble snap: `FLAG_LAYOUT_IN_SCREEN` — root cause branch (c) jadi kode]**:
user kirim hasil device-test Method A/B (protokol Batch 467, 3 screenshot). (A) portrait: GAP
KOSONG kelihatan antara status bar dan widget/bubble — bubble tidak pernah nutupin status bar.
(B) landscape (app lain fullscreen): bubble minimized tetap mentok PERSIS ke ujung layar TANPA
ter-klip sama sekali — gejala IDENTIK kegagalan landscape-edge-clip Batch 460/461/462 (sebelumnya
dianggap bug terpisah, gagal terdiagnosis 3x). Detail root-cause reasoning lengkap: KDoc "Batch
468" di `FloatingBubbleService.kt`. Ringkas: `addBubbleView()` pasang `FLAG_LAYOUT_NO_LIMITS`
TANPA `FLAG_LAYOUT_IN_SCREEN` → posisi x/y diterapkan WindowManager relatif ke content-area
(exclude status/nav bar), sedangkan target dihitung (`screenBounds`) DAN readback
(`getLocationOnScreen()`) sama-sama full-screen absolut — origin mismatch itu sumber SEMUA delta
readback Batch 463-466, kemungkinan besar JUGA sumber gejala landscape-tak-terklip (inset sisi
landscape mengkompensasi `hiddenWidth` yang seharusnya menyembunyikan bubble). **Hipotesis
PENYATUAN 2 gejala berbeda** — kuat didukung data, BELUM kepastian mutlak sampai device confirm.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. Tambah `WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN` ke flags `addBubbleView()`
   (1 baris, sebelah `FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_NO_LIMITS` existing sejak Batch 100).
2. 0 formula lain diubah — `EDGE_CLIP_FRACTION`/`touchPad`/`visualWidth`/`screenBounds`/
   `ROTATION_RESNAP_DELAYS_MS` TETAP persis Batch 460-464 (1 variabel per percobaan, isolasi
   efek). Readback instrumentasi Batch 463/464 TIDAK dihapus — dipakai validasi (target: delta
   jadi (0,0) di semua kasus pasca-update).
3. 0 sektor DITUTUP disentuh.

**RISIKO REGRESI — WAJIB dibaca sebelum lapor hasil**: flag ini ubah origin koordinat SELURUH
window overlay (bukan cuma snap-edge) — termasuk posisi TERSIMPAN dari sesi sebelum update
(dihitung di sistem koordinat lama/salah). **Geser sekali di buka pertama pasca-update = EXPECTED,
BUKAN bug baru** — drag dikit buat re-snap ke koordinat benar. WAJIB regression-test manual PENUH:
drag manual, mini trigger, minimize/expand/fade/auto-minimize (Batch 98-100/451/453-458), DAN
KHUSUSNYA mentok-tepi-landscape (poin yang gagal 3x, paling penting divalidasi).

**0 diverifikasi CI/device Batch 468** — review manual (baca kode + cek balance brace/paren:
`{}` 90/90, `()` 535/535, `[]` 93/93 di `FloatingBubbleService.kt`), 0 env Android nyata/device
fisik/compiler Kotlin di sesi ini. Perlu dari user: install APK baru, ULANGI Method A/B persis +
tes khusus landscape-edge-clip, kirim hasil/screenshot/log lagi.

**Catatan Batch 467 [Klarifikasi crash Batch 466 + keputusan user: device-test dulu, bukan
coding langsung]**: user konfirmasi crash Batch 466 tidak pernah keliatan sebagai force-close
visible ke mereka, dan tidak ingat pemicu spesifik saat itu — WAJAR: `BOOT_COMPLETED` receiver
crash terjadi di background pas boot, proses aplikasi baru spawn lalu mati sebelum ada UI yang
sempat tampil, dan tidak semua OEM/versi Android munculin dialog "App keeps stopping" utk crash
background-only. **Fix Batch 466 TETAP VALID, TIDAK ditarik**: bukti FATAL stack trace di log
(thread='main', `ForegroundServiceStartNotAllowedException`, cocok 100% ke baris
`startForegroundService()` di `BubbleBootReceiver.kt`) berdiri sendiri lepas dari ingatan
subjektif user — pola sama persis pelajaran "device nyata menang atas asumsi" dari Batch 463.
0 kode diubah batch ini (dokumentasi-only).

Untuk hipotesis bubble `FLAG_LAYOUT_IN_SCREEN` (Batch 466): user pilih **device-test tambahan
dulu**, BUKAN langsung coding fix ke-4. Protokol test dikirim ke user (0 kode baru — pakai
instrumentasi existing Batch 463/464/466 yang masih ada di APK):
- **Method A (visual instan, 0 log)**: drag bubble ke tepi ATAS layar semaksimal mungkin →
  amati apakah bubble NUTUPIN status bar (jam/sinyal/baterai) atau berhenti di GAP kosong PAS
  DI BAWAH status bar. Gap kosong = konfirmasi kuat hipotesis (target dihitung exclude status
  bar, readback include).
- **Method B (log-based, banding kondisi)**: trigger minimize/rotate di 2 kondisi: (a) home
  screen biasa (status+nav bar full kelihatan), (b) di app lain fullscreen/immersive (video/game
  yang nyembunyiin status+nav bar) — lalu ekspor Log Diagnostik lagi, kirim balik. Kalau delta
  target-vs-readback BERUBAH/HILANG di kondisi (b) dibanding (a) → konfirmasi kuat hipotesis.

Sektor bubble (Roadmap #11) masih terbuka, 0 sektor DITUTUP disentuh.

**Catatan Batch 466 [FIX FATAL crash boot + data pertama investigasi bubble snap, branch C
terkonfirmasi]**: user kirim ekspor Log Diagnostik pertama (`log_20260915_133616...txt`, 1130
baris) — sekaligus KONFIRMASI IMPLISIT fix Batch 465 (file ADA, utuh, tidak korup/kepotong →
sheet Log Diagnostik terbukti tidak freeze/force-close lagi saat diekspor). 0 sektor DITUTUP
disentuh.

**Temuan 1 [FATAL, P0 STABILITY, prioritas di atas investigasi bubble]**: log berisi 1 crash
FATAL nyata (07:50:11) — `BubbleBootReceiver` → `context.startForegroundService()` saat
`BOOT_COMPLETED` dilempar `ForegroundServiceStartNotAllowedException` (`mAllowStartForeground
false`), app force-close total tiap boot (toggle bubble ON). `BOOT_COMPLETED` nominal exempt
dari restriksi Android 12+ ini per dok resmi, TAPI device nyata user MEMBUKTIKAN exemption itu
tidak selalu berlaku (kemungkinan OEM App Standby Bucket/battery restriction) — bukti device
menang atas asumsi dokumentasi, sama seperti pelajaran Batch 463.

**1 file diubah** (dalam batas 3 file/tugas): `BubbleBootReceiver.kt`.
1. `context.startForegroundService(serviceIntent)` dibungkus `runCatching` (pola identik
   `FloatingBubbleService.kt`) + `AppLogger.e` kalau gagal — 0 lagi propagate ke uncaught
   handler/force-close. Gagal-senyap jatuh ke fallback yang SUDAH ADA (`MainActivity`'s
   `LaunchedEffect(Unit)`, user buka app manual), bukan crash total.
2. 0 formula/logic lain diubah. Manifest dicek ulang (`RECEIVE_BOOT_COMPLETED`,
   `FOREGROUND_SERVICE_SPECIAL_USE`, `exported=false`) — semua sudah benar, 0 disentuh.

**Temuan 2 [investigasi Roadmap #11, BUKAN fix — decision tree Batch 464 resmi terjawab data]**:
330 baris `WARN [FloatingBubbleService]` di log sama = readback instrumentasi Batch 463/464
pertama dari device fisik NYATA (log Log Diagnostik penuh, bukan potongan logcat lagi).
108 pasang readback +250ms & 27 pasang +800ms dianalisis:
- **Ukuran (w/h) real vs target: 0/27 mismatch** → teori "window WRAP_CONTENT belum tuntas
  resize" (branch a, dugaan utama Batch 464) **GUGUR, dibuktikan data.**
- **+800ms MASIH offset dari target, besarnya SAMA PERSIS dgn +250ms** (0 mengecil seiring
  waktu) → teori "murni delay/settling" (branch b) **GUGUR juga.**
- Pola delta (ΔX,ΔY) TERKUANTISASI (bukan noise acak): (0,99)×46, (99,66)×21, (99,0)×22,
  (0,66)×9 — **branch (c) resmi terkonfirmasi**: root cause BUKAN resize, BUKAN timing.

**HIPOTESIS BARU (BELUM di-fix, WAJIB validasi lanjut — pola tebak-tanpa-data Batch 460-462
JANGAN diulang)**: `addBubbleView()` pasang `FLAG_LAYOUT_NO_LIMITS` TANPA
`FLAG_LAYOUT_IN_SCREEN` (baris ~504). Tanpa `FLAG_LAYOUT_IN_SCREEN`, per dok resmi
`WindowManager.LayoutParams` dgn `Gravity.TOP|START` diposisikan relatif ke content area
(exclude status bar/nav bar), sedangkan `getLocationOnScreen()` (dipakai readback) SELALU
absolut ke layar fisik (include status bar/nav bar) — origin 2 sistem koordinat beda, selisih
= inset sistem saat itu (status bar ≈99px di sebagian kasus, nav bar sisi landscape ≈66px di
kasus lain — cocok kenapa offset beda per orientasi, TIDAK pernah 0). Delta 99/66 MENGUATKAN
hipotesis ini tapi BELUM 100% dikonfirmasi (0 device fisik/compiler sesi ini). Fix kandidat
(BELUM diterapkan): tambah `WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN` ke flags baris
~504 (combo `NO_LIMITS+IN_SCREEN` = pola standar overlay bubble absolut, gaya Messenger
chat-head). **RISIKO kalau diterapkan**: flag ini bisa geser SEMUA posisi X/Y existing
(minimize/expand/drag/auto-minimize), bukan cuma snap-to-edge — WAJIB regression-test manual
penuh ke Batch 98-100/453-458/460-463. TIDAK auto-diterapkan batch ini (STABILITY WINS; 3 fix
tebakan sebelumnya sudah gagal — jangan tambah yang ke-4 tanpa keputusan eksplisit user).

**0 diverifikasi CI/device Batch 466** — review manual (baca kode + cek balance brace/paren:
`{}` 4/4, `()` 18/18, `[]` 2/2 di `BubbleBootReceiver.kt`), 0 env Android nyata/device
fisik/compiler Kotlin di sesi ini. Perlu dari user: (1) install APK baru, restart HP, konfirmasi
0 force-close lagi saat boot; (2) putuskan lanjut/tidak ke hipotesis `FLAG_LAYOUT_IN_SCREEN` di
atas sebelum batch depan coding fix bubble (lihat RESUME POINT).

**Catatan Batch 465 [FIX BLOCKER: Log Diagnostik force-close/freeze]**: user laporkan sheet
Settings > Log Diagnostik sendiri force-close/freeze saat dibuka — ini BLOCKER kritis karena
Log Diagnostik justru alat yang diminta Batch 464 untuk ambil log instrumentasi bubble. Root
cause: `DiagnosticLogSheet.kt` render `logText` (bisa sampai ~200_000 char, `MAX_LOG_BYTES` di
`AppLogger.kt`) sebagai 1 `Text` mentah dalam `Column`+`verticalScroll` — baca file sudah async
sejak Batch 431, tapi LAYOUT teks sepanjang itu di 1 node Compose tetap kerja Main thread saat
sheet pertama tampil; instrumentasi padat `FloatingBubbleService.kt` Batch 463/464 bikin file
log lebih cepat mendekati cap 200KB, cukup memicu ANR/freeze. Sektor bubble (Roadmap #11) TIDAK
disentuh batch ini — murni fix sheet Log Diagnostik. 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `DiagnosticLogSheet.kt`.
1. Render log diganti dari 1 `Text` mentah → `LazyColumn` + 1 `Text` per baris (pola sama
   `LyricsSheet.kt`/`DuplicateFinderSheet.kt`) — Compose cuma measure/layout baris yang
   KELIHATAN di layar, bukan seluruh log sekaligus.
2. 0 baris log dibuang/ditruncate — `logText` tetap dibaca & disimpan utuh dari `AppLogger`.
   "Repack ke Dokumen" tetap ekspor `readLog()` utuh, tidak disentuh.
3. Import tak terpakai (`rememberScrollState`/`verticalScroll`) dibuang, ganti
   `LazyColumn`/`items` (sudah dipakai pola sama di file lain, 0 dependency baru). 0
   formula/state lain diubah.

**0 diverifikasi CI/device Batch 465** — review manual (baca kode + cek balance brace/paren:
`{}` 27/27, `()` 101/101, `[]` 0/0 di `DiagnosticLogSheet.kt`), 0 env Android nyata/device
fisik/compiler Kotlin di sesi ini. Perlu dari user: buka Settings > Log Diagnostik, konfirmasi
sheet terbuka normal (0 freeze/force-close), isi log lengkap/bisa di-scroll/di-ekspor — BARU
setelah itu lanjutkan permintaan Batch 464 (WAJIB DILAKUKAN di RESUME POINT di bawah, masih
berlaku persis, belum terpenuhi).

**Catatan Batch 464 [DATA DEVICE FISIK PERTAMA — MENGEJUTKAN, ganti arah dugaan]**: user kirim 2
potongan logcat hasil instrumentasi Batch 463. Log ke-1 (rotasi) terpotong sebelum baris readback
sempat muncul (0 kesimpulan bisa ditarik). Log ke-2 (rotasi lagi, ditunggu lebih lama) BERHASIL
menangkap readback pertama — hasilnya JUSTRU dari event **minimize() BIASA SEBELUM rotasi terjadi**
(screenWidth=1080, portrait, 0 rotasi terlibat): target `x=1011 y=469` TAPI posisi nyata di layar
250ms kemudian `x=551 y=568` — **selisih 460px di X**. Ini sinyal kuat BARU: mismatch mungkin BUKAN
soal animasi transisi ROTASI (fokus Batch 462 & 463), melainkan window overlay ini `WRAP_CONTENT`
(resize fisik tiap toggle expanded↔minimized) dan `width`/`height` yang dibaca
`snapMinimizedToNearestEdge()` via `container.post{}` mungkin representasi View yang sudah
di-measure TAPI window WindowManager-nya sendiri belum tuntas resize saat `updateViewLayout`
dipanggil — teori KE-4, belum pernah diuji batch mana pun sebelumnya. Rotasi mungkin cuma
kebetulan JUGA memicu fungsi snap yang SAMA, kena race yang SAMA — bukan soal rotasi itu sendiri.
0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. **0 formula/logic diubah lagi** — masih PERSIS instrumentasi, bukan fix.
2. **Readback diperluas**: sekarang JUGA log `container.width`/`container.height` NYATA di momen
   readback, dibandingkan ke `width`/`container.height` yang dipakai saat target dihitung
   (`targetWidth`/`targetHeight`, snapshot lokal) — kalau beda, itu BUKTI LANGSUNG window/view
   masih resize saat snap kita apply (bukan dugaan lagi).
3. **Readback KEDUA ditambah di +800ms** (selain +250ms yang sudah ada dari Batch 463, keduanya
   sekarang pakai 1 fungsi lokal `logReadback(label)` supaya 0 duplikasi kode) — kalau +800ms
   SUDAH cocok ke target (beda dari +250ms yang meleset), itu bukti murni SETTLING/animasi
   sementara (fix: perpanjang delay saja); kalau +800ms MASIH meleset SAMA, itu salah PERMANEN
   (fix: re-urutan resize-dulu-baru-posisikan, BUKAN soal delay).
4. 0 breaking change ke minimize/expand/fade/auto-minimize/rotasi Batch 98-100/453-458/460-463,
   0 import/dependency baru, 0 sektor DITUTUP disentuh.

**0 diverifikasi CI/device Batch 464** — review manual (baca kode + cek balance brace/paren:
`{}` 90/90, `()` 515/515, `[]` 86/86), 0 env Android nyata/device fisik/compiler Kotlin di sesi
ini. Perlu dari user: reproduksi SEKALI LAGI (minimize → rotasi → **diamkan HP minimal 1 detik
penuh**, karena readback terjauh sekarang +800ms), lalu kirim log/ekspor Log Diagnostik. Baca
PERBANDINGAN 250ms vs 800ms + ukuran nyata vs target sebelum putuskan fix apa pun — 2 hasil
berbeda mengarah ke 2 kelas fix yang sama sekali berbeda (lihat poin 3 di atas).

**Catatan Batch 463 [PIVOT KE INSTRUMENTASI]**: user konfirmasi device fisik Batch 462 — "masih
nongol/gak ke kliping" (GAGAL, sama seperti Batch 460/461). User membawa `bubble_log.txt` (logcat
`-iE "floatingbubble|configurationchanged|windowmanager"`) sesuai permintaan eksplisit
PROJECT_STATE.md Batch 462 ("WAJIB logcat device asli sebelum lanjut tebak lagi"). **Log dicek
baris-per-baris (bukan diasumsikan berisi sinyal)**: 0 baris dari `FloatingBubbleService` sama
sekali — 2 satu-satunya kecocokan "floatingbubble" di file itu adalah ECHO PERINTAH grep-nya
sendiri (baris command Termux), BUKAN output aplikasi. Kesimpulan wajib: service ini TIDAK PERNAH
menulis logcat, jadi 3 teori berturut-turut (Batch 460/461/462) SEMUA murni tebakan dari baca-kode,
0 pernah divalidasi data eksekusi nyata. Sesuai SOP sendiri (dilarang tebak fix ke-4 tanpa data),
batch ini **PIVOT — 0 fix baru ke formula/logic, murni instrumentasi**. 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. **0 formula/state/logic diubah** — `EDGE_CLIP_FRACTION`/`touchPad`/`visualWidth`/`screenBounds`/
   `ROTATION_RESNAP_DELAYS_MS` semuanya TETAP persis Batch 460-462, TIDAK disentuh sama sekali.
2. **`AppLogger.w(tag="FloatingBubbleService", ...)` ditambah di 4 titik** (pola sama existing
   `AppLogger.e` di `loadAlbumArtBitmap`, BUKAN `Log.d` polos): (a) entry `onConfigurationChanged`
   — orientation + screenBounds + isMinimized; (b) 2 guard null `bubbleView`/`layoutParams` yang
   sebelumnya `return` diam-diam 0 sinyal; (c) tiap callback `ROTATION_RESNAP_DELAYS_MS` (150ms/
   400ms) benar tereksekusi; (d) di `snapMinimizedToNearestEdge()` — X/Y TARGET tepat sebelum
   `updateViewLayout` + outcome sukses/gagalnya (`runCatching` lama SEBELUMNYA membungkam
   exception total, 0 pernah tahu kalau apply-nya sendiri gagal).
3. **Readback +250ms baru** (tambahan, bukan cuma log): `container.postDelayed { getLocationOnScreen() }`
   membaca posisi NYATA container di layar 250ms setelah tiap apply, dibandingkan ke X/Y target
   yang di-snapshot ke `val` lokal (`targetX`/`targetY`, BUKAN baca ulang `params.x` yang
   objeknya sama dipakai bergantian oleh 3 panggilan snap) — satu-satunya cara MEMBUKTIKAN atau
   MEMBANTAH teori Batch 462 ("sistem menimpa posisi window pasca-snap") dengan data asli, bukan
   dugaan lagi.
4. **`AppLogger.w()` dipilih (bukan `Log.d`)** — otomatis kepakai ke 2 kanal: logcat (tag persis
   "FloatingBubbleService", akan kena grep SAMA yang user pakai) DAN `diagnostic_log.txt` privat
   app (baca/ekspor lewat Settings > Lanjutan > Log Diagnostik) — kalau kanal Termux/adb gagal
   nangkap lagi, kanal kedua tetap ada tanpa perlu setup ADB sama sekali.
5. 0 breaking change ke minimize/expand/fade/auto-minimize Batch 98-100/453/454/455/457/458/460,
   0 sektor DITUTUP disentuh. `AppLogger.w` sudah ada sejak awal (dipakai class lain) — 0 import
   baru, 0 dependency baru.

**0 diverifikasi CI/device Batch 463** — review manual (baca kode + cek balance brace/paren:
`{}` 84/84, `()` 486/486, `[]` 83/83), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Batch ini SENGAJA 0 mengklaim fix — perlu dari user: (1) buka bubble,
minimize, rotasi ke landscape sekali; (2) ambil salah satu — logcat Termux (perintah SAMA persis
`bubble_log.txt` sebelumnya, sekarang HARUS muncul baris "Batch463...") ATAU buka app > Settings >
Lanjutan > Log Diagnostik > ekspor/salin; (3) kirim hasilnya balik. Fix ke-4 (kalau perlu) baru
diputuskan dari log itu, BUKAN teori baru.

**Catatan Batch 462 [FIX RESIDUAL #2]**: konfirmasi device fisik user Batch 461 — masih "nongol",
DIPERJELAS via klarifikasi tap: **100% kelihatan, gak keclip sama sekali** (bukan "kurang tepat
dikit"). Ini membuktikan diagnosis Batch 460/461 ("sumber bounds kurang akurat") SALAH ARAH —
`screenBounds` Batch 461 sudah benar. Kesimpulan: ADA PIHAK LAIN (kemungkinan besar sanitasi posisi
window oleh sistem selama animasi transisi rotasi) menimpa posisi window SETELAH snap kita apply.
0 klarifikasi tap tambahan diperlukan untuk MEMUTUSKAN FIX-nya (root cause tepatnya butuh logcat
device asli yang tidak tersedia di sesi ini — SOP mengizinkan mitigasi defensif berbasis sinyal
kuat yang ada, bukan diam menunggu). 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. **TIDAK ganti formula/sumber data LAGI** — eksplisit mengikuti catatan proses PROJECT_STATE.md
   sendiri (ditulis di Batch 461: "jangan ulang pola ganti API baca metrics"). Formula
   `EDGE_CLIP_FRACTION`/`touchPad`/`visualWidth`/`screenBounds` TETAP tidak disentuh.
2. **Fix — safety-net re-assert 2x delay**: `onConfigurationChanged` memanggil
   `snapMinimizedToNearestEdge()` immediate (seperti sebelumnya) + 2x re-panggil delay 150ms &
   400ms (`ROTATION_RESNAP_DELAYS_MS`, via `view.postDelayed`) — membracket durasi animasi
   transisi rotasi tipikal. Idempotent (0 efek tambahan kalau snap pertama sudah benar) — fallback
   pasti kalau snap pertama ketiban override sistem.
3. 0 breaking change ke formula/state lain Batch 98-100/453-458/460/461, 0 sektor DITUTUP disentuh.

**0 diverifikasi CI/device Batch 462** — review manual (baca kode + cek balance brace/paren:
`{}` 74/74, `()` 436/436, `[]` 75/75), 0 env Android nyata/device fisik/logcat/compiler Kotlin di
sesi ini. **CATATAN JUJUR (WAJIB dibaca sesi berikutnya kalau residual masih ada)**: ini mitigasi
defensif, BUKAN root-cause pasti terverifikasi. Kalau tab masih "nongol"/tidak ter-klip lagi
SETELAH batch ini, JANGAN tebak fix ke-4 tanpa data baru — WAJIB minta logcat device asli user
(command: `adb logcat` atau share via Termux) fokus pada window rotasi + lifecycle
FloatingBubbleService, BARU putuskan fix berikutnya dari situ.

**Catatan Batch 461 [FIX RESIDUAL]**: konfirmasi device fisik user Batch 460 — (1)/(2)/(4)/(5) ✅,
(3) mentok tepi konsisten landscape ❌ ("masih nongol", rotasi bolak-balik). Fix Batch 460
(`resources.displayMetrics`→`currentWindowMetrics.bounds`) TERBUKTI BELUM CUKUP untuk item (3).
0 klarifikasi tap diperlukan — laporan user ("except no.3 ❌ masih nongol") cukup spesifik
dipadukan dengan review kode static (root cause dapat ditentukan deterministik dari dokumentasi
resmi WindowManager, bukan ambiguitas material). 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt`.
1. **Root cause sebenarnya ditemukan**: `windowManager` di kelas ini didapat dari Context `Service`
   biasa (BUKAN `UiContext`/`WindowContext`) — per dokumentasi resmi
   `WindowManager#getCurrentWindowMetrics()`, Context non-UI SELALU jatuh ke
   `getMaximumWindowMetrics()`, TIDAK dijamin sinkron atomik persis di momen rotasi. Kelas masalah
   SAMA dengan `resources.displayMetrics` (Batch 460) — root sumber sama-sama Context Service yang
   sama, swap API Batch 460 mengurangi tapi tidak menghilangkan race, terutama saat rotasi
   bolak-balik cepat (persis skenario device-test #3).
2. **Fix — `screenBounds` (single source of truth, field baru `Rect`)**: diisi dari parameter
   `newConfig` di `onConfigurationChanged` (dp→px via `resources.displayMetrics.density`) — SATU-
   SATUNYA sumber yang DIJAMIN sistem fresh PERSIS di momen callback rotasi, bukan re-query Context
   async. Nilai awal (sebelum rotasi pertama) di-set sekali di `onCreate` dari
   `currentWindowMetrics.bounds` (baseline). Refresh dipindah ke baris PALING ATAS
   `onConfigurationChanged`, sebelum early-return guard `bubbleView`/`layoutParams` null.
3. **4 titik baca diganti ke `screenBounds` ter-cache** (titik sama seperti Batch 460):
   `onConfigurationChanged`, `setupDrag`, `expand()`, `snapMinimizedToNearestEdge()`. 0 lagi query
   `windowManager.currentWindowMetrics` langsung di titik mana pun selain nilai awal `onCreate`.
4. 0 breaking change ke `EDGE_CLIP_FRACTION`/`touchPad`/`visualWidth` (Batch 460), 0 breaking
   change ke minimize/expand/fade/auto-minimize Batch 98-100/453/454/455/457/458.

**0 diverifikasi CI/device Batch 461** — review manual (baca kode + cek balance brace/paren:
`{}` 72/72, `()` 416/416, `[]` 72/72), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. README.md diperbarui (blockquote Batch 460 dikoreksi ANTI-STALE:
item 1/2/4/5 ditandai ✅ device-fisik, item 3 ditandai ❌+fix baru; deskripsi fitur bubble
dikoreksi dari klaim "konsisten landscape" yang ternyata belum terbukti).

**Catatan Batch 460**: 2 instruksi eksplisit user sekaligus — (1) "perluas touch target nya biar
gak nyusahin, sedangkan visual turunkan jadi ~10% yang timbul saja"; (2) "fix juga agar fitur
bubble bisa ke kliping mentok ujung layar walaupun hp sedang dalam mode horizontal". Kelanjutan
langsung dari residual UX yang dicatat Batch 458/459 (mini trigger "sedikit lebih susah" di-tap)
— opsi umum yang sudah dicatat di situ ("perbesar touch target independen dari lebar visual")
sekarang dieksekusi. 0 klarifikasi tap diperlukan untuk instruksi (1): kata "timbul" dipakai
eksplisit, konsisten catatan proses di bawah (Batch 456→457→458). 0 sektor DITUTUP disentuh.

**2 file diubah** (dalam batas 3 file/tugas): `bubble_minimized.xml` + `FloatingBubbleService.kt`.
1. **Touch target dipisah dari visual**: `bubble_minimized.xml` root 48dp→88dp (100% transparan,
   0 background baru) jadi murni area-sentuh; visual bulat asli (background+art) pindah ke child
   baru `bubble_minimized_visual` (tetap 48dp, gravity CENTER, padding sentuh simetris kiri/kanan
   supaya valid di sisi mana pun tab menempel).
2. **`snapMinimizedToNearestEdge()` — 1 fungsi yang sama, dipisah jadi 2 lebar**: `visualWidth`
   dipakai `EDGE_CLIP_FRACTION` (formula lama tidak berubah), `width` root dipakai posisi X
   window. `touchPad` (selisih /2) selalu ikut ke sisi yang tetap di layar — area sentuh naik
   TANPA ikut mengecil saat `EDGE_CLIP_FRACTION` naik (akar masalah residual UX Batch 458: 1 angka
   dulu mengontrol visual DAN touch sekaligus, sekarang 2 parameter independen).
3. **`EDGE_CLIP_FRACTION` 0.7f → 0.9f**: bagian TIMBUL turun 30%→~10% sesuai instruksi (1).
4. **Fix landscape (instruksi 2)**: `resources.displayMetrics` (4 titik: `onConfigurationChanged`,
   `setupDrag`, `expand`, `snapMinimizedToNearestEdge`) diganti `windowManager.
   currentWindowMetrics.bounds` (API 30+, aman minSdk 31) — root cause paling mungkin: metrics
   Context Service tidak dijamin ter-refresh seketika saat `onConfigurationChanged` terpanggil
   pasca-rotasi, beda dari Activity/WindowContext.
5. 0 breaking change ke minimize/expand/fade/auto-minimize Batch 98-100/453/454/455/457/458.

**0 diverifikasi CI/device Batch 460** — review manual (baca kode + cek balance brace/paren:
`{}` 72/72, `()` 390/390, `[]` 63/63), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Perlu konfirmasi device fisik berikutnya: (1) mini trigger LEBIH
GAMPANG di-tap dari Batch 458/459 (target: fix residual UX yang dilaporkan user, BUKAN cuma "masih
bisa di-tap"); (2) bagian kelihatan tab minimized ~10% (lebih ngumpet dari Batch 458's ~30%); (3)
rotasi ke landscape (dan bolak-balik beberapa kali) → tab minimized SELALU mentok tepi kiri/kanan
dengan benar; (4) drag tab minimized tetap 100% kelihatan/terkontrol penuh selagi digeser; (5) 0
regresi ke minimize/expand/fade/auto-minimize Batch 98-100/453/454.

**Catatan Batch 458**: user eksplisit lanjut tuning setelah Batch 457 ("ubah jadi ~30%!!") — angka
mentah tanpa konteks ulang, berisiko ulang kesalahan arah Batch 456. **Diklarifikasi via pilihan
tap (BUKAN ditebak)**: "~30%" merujuk ke bagian TIMBUL (kelihatan), bukan ke fraksi klip itu
sendiri. Tuning lanjutan murni, bukan mandat baru. 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` —
1. **1 nilai konstanta dinaikkan, 0 fungsi baru**: `EDGE_CLIP_FRACTION` 0.5f → 0.7f (fraksi
   SEMBUNYI naik, supaya fraksi TIMBUL/kelihatan turun ke ~30% sesuai konfirmasi tap user).
2. **0 titik panggil baru**: semua pemicu snap (drag-lepas, auto-minimize, chevron, restart,
   rotasi) otomatis ikut nilai baru.
3. Formula `hiddenWidth = width * EDGE_CLIP_FRACTION` (Batch 456) TETAP tidak disentuh — cuma
   nilai konstanta.
4. KDoc kelas & `snapMinimizedToNearestEdge()` diperbarui dengan catatan Batch 458.
5. 0 file lain disentuh, 0 breaking change ke minimize/expand/fade/auto-minimize Batch 453/454.

**0 diverifikasi CI/device Batch 458** — review manual (baca kode + cek balance brace/paren:
`{}` 71/71, `()` 347/347, `[]` 52/52), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini (konsisten pola Batch 435-457). Perlu konfirmasi device fisik
berikutnya: (1) tab minimized SEKARANG ~30% kelihatan/~70% tersembunyi (lebih ngumpet dari Batch
455/457 yang 50/50) di semua jalur snap; (2) mini trigger MASIH gampang di-tap walau makin kecil
bagian kelihatannya (touch target tidak "meleset"); (3) drag tab minimized tetap 100%
kelihatan/terkontrol penuh selagi digeser; (4) 0 regresi ke minimize/expand/fade/auto-minimize
Batch 98-100/453/454.

**Catatan Batch 457**: feedback eksplisit user langsung setelah Batch 456 ("revert progress
kliping. bukannya hilangin yang timbul malah dibikin tambah timbul, bukan saya suruh") — Batch 456
SALAH ARAH: `EDGE_CLIP_FRACTION` 50%→30% justru MEMPERBESAR bagian tab yang kelihatan (nambah
"timbul"), kebalikan dari yang diinginkan. **REVERT murni**, bukan mandat baru. 0 sektor DITUTUP
disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` —
1. **1 nilai konstanta dikembalikan, 0 fungsi baru**: `EDGE_CLIP_FRACTION` 0.3f → 0.5f (nilai asli
   Batch 455). Formula `hiddenWidth = width * EDGE_CLIP_FRACTION` (generalisasi Batch 456) TETAP
   DIPERTAHANKAN — netral arah, cuma nilai konstanta yang salah kemarin.
2. **0 titik panggil baru**: semua pemicu snap (drag-lepas, auto-minimize, chevron, restart,
   rotasi) otomatis ikut nilai revert, 0 perubahan lain.
3. Hasil setelah revert identik matematis dengan Batch 455 (regresi-aman, sudah diverifikasi
   formula-nya di Batch 456).
4. KDoc kelas & KDoc `snapMinimizedToNearestEdge()` diperbarui — Batch 456 ditandai [SALAH ARAH],
   Batch 457 dicatat sebagai revert (ANTI-STALE, 1 file yang sama, 0 file dok terpisah disentuh).
5. 0 file lain disentuh, 0 breaking change ke minimize/expand/fade/auto-minimize Batch 453/454.

**0 diverifikasi CI/device Batch 457** — review manual (baca kode + cek balance brace/paren:
`{}` 71/71, `()` 340/340, `[]` 50/50), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini (konsisten pola Batch 435-456). Perlu konfirmasi device fisik
berikutnya: (1) tab minimized kembali separuh tersembunyi di luar layar/separuh kelihatan sebagai
mini trigger (SAMA seperti Batch 455, BUKAN lagi ~70% kelihatan Batch 456) di semua jalur snap;
(2) mini trigger tetap gampang di-tap; (3) drag tab minimized tetap 100% kelihatan/terkontrol
penuh selagi digeser; (4) 0 regresi ke minimize/expand/fade/auto-minimize Batch 98-100/453/454.

**Catatan Batch 456 [SALAH ARAH — DIREVERT Batch 457 di atas]**: feedback lanjutan user langsung setelah Batch 455 ("sudah ke kliping
walaupun agak timbul") — diklarifikasi via pilihan tap: **"bagian yang kepotong terlalu besar,
perkecil clip-nya"**. Refinement tuning murni ke [snapMinimizedToNearestEdge], bukan mandat baru.
0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` —
1. **1 konstanta baru, 0 fungsi baru**: `EDGE_CLIP_FRACTION = 0.3f` (companion object, pola sama
   `IDLE_FADE_ALPHA` dkk) — fraksi lebar tab yang disembunyikan di luar layar, turun dari 50%
   (`width/2` hardcoded, Batch 455) ke 30%.
2. **Formula digeneralisasi**: `snapMinimizedToNearestEdge()` — `width/2` hardcoded diganti
   `hiddenWidth = width * EDGE_CLIP_FRACTION`. Di `EDGE_CLIP_FRACTION = 0.5f` formula ini identik
   matematis dengan Batch 455 (regresi-aman), tuning berikutnya (kalau ada) tinggal ubah 1 angka.
3. **0 titik panggil baru**: semua pemicu snap yang sudah ada (drag-lepas, auto-minimize Batch
   454, chevron manual, restart service, rotasi) otomatis ikut fraksi baru, 0 perubahan lain.
4. 0 file lain disentuh, 0 breaking change ke minimize/expand/fade Batch 453/auto-minimize Batch
   454/half-clip Batch 455 — cuma besaran fraksi clip yang berubah.

**0 diverifikasi CI/device Batch 456** — review manual (baca kode + cek balance brace/paren:
`{}` 71/71, `()` 339/339, `[]` 49/49), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini (konsisten pola Batch 435-455). Perlu konfirmasi device fisik
berikutnya: (1) tab minimized kelihatan lebih "penuh"/kurang timbul dibanding Batch 455 (~70%
lebar kelihatan, bukan 50%) di SEMUA jalur snap; (2) mini trigger tetap gampang di-tap; (3) drag
tab minimized tetap 100% kelihatan/terkontrol penuh selagi digeser; (4) 0 regresi ke
minimize/expand/fade/auto-minimize Batch 98-100/453/454.

**Catatan Batch 455**: feedback eksplisit user langsung setelah Batch 454 ("minimize otomatis nya
berhasil, TAPI yang benar-benar diinginkan: circle bubble kliping setengah/menyisakan mini
trigger, wajib mentok maksimal ke tepi layar saat idle"). Refinement VISUAL murni ke tab minimized
Batch 100 (bukan mandat baru/sektor baru) — tab yang tadinya flush-tapi-100%-kelihatan di tepi
sekarang setengah lebarnya sengaja melewati batas layar. 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` —
1. **1 titik kontrol yang sama, 0 fungsi baru**: `snapMinimizedToNearestEdge()` — SATU-SATUNYA
   fungsi yang menghitung posisi X tab minimized (dipanggil dari drag-lepas, auto-minimize Batch
   454, tombol chevron manual, restart service, rotasi) — cuma formula X-nya yang berubah, dari
   `0`/`screenWidth - lebarTab` (flush, 100% kelihatan) jadi `-lebarTab/2`/`screenWidth -
   lebarTab/2` (setengah lebar melewati batas layar).
2. **0 flag/permission baru**: window overlay SUDAH `FLAG_LAYOUT_NO_LIMITS` sejak Batch 100 —
   prasyarat X negatif/lewat `screenWidth` diterima WindowManager sudah terpenuhi dari awal,
   sistem yang otomatis memotong render di luar layar, 0 clip manual perlu ditulis.
3. **0 mekanisme/state/timer baru**: `lebarTab/2` dihitung dari `container.width` real via
   `container.post{}` yang sudah ada (pola sama Batch 100) — bukan angka dp ditebak manual.
4. **Drag aktif tidak terpengaruh**: `setupDrag()` (clamp `[0, maxX]` pakai lebar penuh) 0
   disentuh — half-clip HANYA berlaku begitu jari dilepas & tab snap ke tepi dalam keadaan diam
   (idle), sesuai kata "saat idle" di instruksi user, bukan selagi masih digeser.
5. 0 file lain disentuh, 0 breaking change ke `minimize()`/`expand()`/fade Batch 453/auto-minimize
   Batch 454 — cuma X akhir tab yang berubah, mekanisme kapan snap dipanggil sama sekali tidak
   disentuh.

**0 diverifikasi CI/device Batch 455** — review manual (baca kode + cek balance brace/paren:
`{}` 71/71, `()` 325/325, `[]` 46/46), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini (konsisten pola Batch 435-454). Perlu konfirmasi device fisik
berikutnya: (1) tab minimized kelihatan kepotong SETENGAH mentok tepi kiri/kanan (bukan lagi bulat
utuh 100% kelihatan) baik saat manual-minimize, auto-minimize idle, restart app, maupun rotasi;
(2) sisa "mini trigger" yang kelihatan tetap bisa di-tap untuk expand() — touch target setengah
lingkaran tidak "meleset"/butuh tap presisi berlebihan; (3) drag tab minimized (pindah ke posisi
lain) tetap 100% kelihatan/terkontrol penuh SELAGI digeser, cuma clip setengah setelah dilepas;
(4) 0 regresi ke minimize/expand/fade/auto-minimize-idle Batch 98-100/453/454.

**Catatan Batch 454**: lanjutan langsung Batch 453 — mandat UTAMA user ("wajib bisa
split/di-minimize total") belum tuntas Batch 453 (baru fallback minimumnya, fade). Batch ini
menuntaskan mandat utamanya: auto-minimize total otomatis kalau bubble tetap idle lebih lama
lagi setelah fade. Perluasan sektor bubble (Roadmap #11), 0 sektor DITUTUP disentuh.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` —
1. **Timer kedua, 1 titik kontrol yang sama**: `idleMinimizeJob` baru, dijadwalkan/dibatalkan di
   `keepAwakeAndScheduleFade()` — fungsi yang SAMA PERSIS dipanggil `setupDrag`/`setupControls`
   Batch 453, jadi 0 perubahan di kedua fungsi itu. Delay dihitung dari titik interaksi terakhir
   yang SAMA dengan timer fade (bukan ditambah setelah fade selesai) — `IDLE_AUTO_MINIMIZE_DELAY_MS`
   = 6000ms, > `IDLE_FADE_DELAY_MS` (2500ms) supaya urutan visual selalu fade dulu baru collapse.
2. **0 logic collapse baru**: auto-trigger cuma manggil `minimize()` yang sudah ada sejak Batch
   100 apa adanya (termasuk guard `if (isMinimized) return` di dalamnya — aman dipanggil berulang
   walau user sempat minimize manual duluan lewat chevron).
3. **0 mekanisme timer baru**: reuse `bubbleScope` yang sama dgn `idleFadeJob`/`bubbleArtJob` —
   otomatis ikut ter-cancel oleh `bubbleScope.cancel()` di `onDestroy()` yang sudah ada. 0 import
   baru (delay/launch sudah diimport Batch 453).
4. Alpha container TIDAK direset saat auto-minimize (tab hasil collapse mewarisi alpha fade yang
   sedang berjalan — konsisten desain "1 titik kontrol alpha di container" Batch 453).
5. 0 file lain disentuh, 0 breaking change ke `minimize()`/`expand()`/`setupDrag`/fade Batch 453.

**0 diverifikasi CI/device Batch 454** — review manual (baca kode + cek balance brace/paren:
`{}` 71/71, `()` 313/313, `[]` 38/38), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini (konsisten pola Batch 435-453). Perlu konfirmasi device fisik
berikutnya: (1) bubble auto-collapse jadi tab 48dp tepi layar setelah ±6 detik idle TANPA
sentuhan (menyusul fade ±2.5 detik yang sudah jalan lebih dulu); (2) TIDAK auto-collapse selagi
masih digeser/tombol kontrolnya ditekan (timer ikut ter-reset sama seperti fade); (3) minimize
manual (tap chevron) & auto-minimize tidak saling konflik/duplikasi state; (4) 0 regresi ke
mekanisme minimize/expand/snap-tepi/drag-bebas Batch 98-100 & fade Batch 453.

**Catatan Batch 453**: instruksi eksplisit user — fitur mini player mengambang (bubble) "wajib
bisa split/di-minimize total, atau minimal dulu bisa fade out saat tidak digeser". Perluasan
langsung sektor bubble (Roadmap #11, Batch 95-100), bukan reopen sektor DITUTUP manapun.

**1 file diubah** (dalam batas 3 file/tugas): `FloatingBubbleService.kt` —
1. **Cek dulu, bukan reimplementasi dari nol**: mekanisme "total" SUDAH ada sejak Batch 100 —
   tombol chevron minimize mengciutkan pill jadi tab 48dp nempel tepi layar (manual, lewat tap).
   Celah sesungguhnya: kondisi IDLE (bubble dibiarkan diam TANPA aksi apa pun) tetap 100% opaque
   selamanya, menutupi konten di baliknya — persis skenario di video user (pill mengambang diam
   di atas daftar "Paling Sering Diputar"). Fix batch ini menyasar celah itu, sesuai opsi fallback
   eksplisit user ("minimal dulu bisa fade out").
2. **Fitur baru**: `keepAwakeAndScheduleFade()` — meredupkan alpha `bubbleView` (container
   `FrameLayout`, BUKAN per-child) ke 0.45f setelah 2.5 detik tanpa sentuhan, `ViewPropertyAnimator`
   `View.animate()` 250ms. Mengembalikan ke opaque penuh SEKETIKA (`view.animate().cancel()` +
   `alpha=1f`) di SETIAP titik masuk interaksi baru: `setOnTouchListener` root (`setupDrag`, drag
   MAUPUN tap-buka-app) dan ke-4 `setOnClickListener` tombol kontrol (`setupControls` —
   play/pause/prev/next/minimize, WAJIB direset terpisah karena `ImageButton` clickable
   mengonsumsi `ACTION_DOWN` duluan sebelum sempat ke `OnTouchListener` root, lihat KDoc
   `setupDrag` yang sudah ada). Alpha container otomatis berlaku ke child mana pun yang sedang
   `VISIBLE` (pill penuh ATAU tab minimized, mengikuti pola toggle-visibility 1-container Batch
   100) — 1 titik kontrol, 0 duplikasi logic per state.
3. **0 mekanisme baru untuk timer**: reuse `bubbleScope` (`CoroutineScope(Dispatchers.Main +
   Job())`) yang SUDAH ada untuk `bubbleArtJob` — job baru `idleFadeJob` (`delay()` +
   cek-null-lalu-animate), otomatis ikut ter-cancel oleh `bubbleScope.cancel()` di `onDestroy()`
   yang sudah ada, 0 Handler/Thread baru, 0 leak. 1 import baru: `kotlinx.coroutines.delay`
   (satu paket persis dengan `launch`/`withContext` yang sudah diimport).
4. Alpha window TIDAK mengubah keterjangkauan sentuh (`FLAG_NOT_FOCUSABLE` independen dari
   alpha) — tap pada bubble yang lagi pudar tetap berfungsi normal, murni sinyal visual.
5. 0 file lain disentuh, 0 breaking change ke `minimize()`/`expand()`/`setupDrag` logic lama.

**0 diverifikasi CI/device Batch 453** — review manual (baca kode + cek balance brace/paren:
`{}` 70/70, `()` 294/294, `[]` 28/28), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini (konsisten pola Batch 435-452). Perlu konfirmasi device fisik
berikutnya: (1) bubble (pill penuh MAUPUN tab minimized) benar meredup ke ~45% opacity setelah
±2.5 detik diam, TIDAK meredup selagi masih di-drag/di-tap kontrolnya; (2) opacity kembali penuh
SEKETIKA begitu disentuh lagi (drag maupun tap tombol), 0 delay/lompatan visual; (3) 0 regresi ke
mekanisme minimize/expand/snap-tepi/drag-bebas Batch 98-100 yang sudah ada.

**Catatan Batch 452**: 2 instruksi eksplisit user — (1) minimalkan tulisan label nav bawah yang
terpotong ellipsis, (2) animasi pill/tab WAJIB berhenti tepat di tab tujuan tanpa "offside" (baik
mode drag-langsung-di-bar maupun tap-tab biasa).

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. **Fix truncation**: root cause — padding horizontal 12.dp (kiri+kanan) di `Column`
   `GlassTabIcon` memakan 24.dp dari ~1/3 lebar bar SEBELUM `Text` diukur, cukup memicu ellipsis
   pada label 12 huruf ("Perpustakaan") di layar sempit KONDISI NORMAL (bukan cuma font-scale
   aksesibilitas besar spt catatan lama Batch 442). Fix: 12.dp -> 4.dp. Touch target 0 terdampak
   (area sentuh = `Box.weight(1f, fill=true)` di `CustomNavBarTabItem`, pembungkus DI LUAR Column
   ini, bukan padding Column). 0 sentuh style/fontSize/typography token (`labelMedium` dari Batch
   442 tetap dipakai apa adanya) — murni jarak. Efek samping disengaja: pill solid Skeu (dibungkus
   padding sama) ikut sedikit lebih ramping, masih proporsional (aturan solid Batch 58/61/79 tidak
   disentuh).
2. **Fix "offside" drag/tap**: root cause — di loop drag-langsung-di-tab-bar (Batch 442/448),
   `if (!change.pressed) break` lama dicek DI AWAL badan loop, SEBELUM posisi event dibaca. Untuk
   event UP (pelepasan jari), loop break LANGSUNG tanpa pernah memproses posisi UP itu sendiri ke
   `tabBarDragIndexPx`/`hoveredIndex` — keduanya nyangkut di event MOVE kedua-dari-akhir. Pada
   drag cepat (flick, event batching sistem), posisi MOVE terakhir bisa beda 1 kolom penuh dari
   titik lepas jari sungguhan → pill/route commit ke tab yang SALAH (1 kolom sebelum tujuan asli).
   Fix: posisi TIAP event (termasuk UP) diproses dulu sama seperti MOVE, `pressed` dicek TERAKHIR
   (akhir badan loop) sbg syarat lanjut/berhenti — bukan lagi syarat lewati pemrosesan. 0
   state/Animatable/mekanisme baru — murni urutan 2 baris dipertukar dalam loop yang sudah ada.
   Perbaikan ini juga menjamin `hoveredIndex` akurat utk tap biasa (down+up di kolom sama) krn
   posisi UP kini selalu ikut diproses, bukan cuma diasumsikan sama dgn `down`.
3. 0 import baru, 0 file lain disentuh.

**0 diverifikasi CI/device Batch 452** — review manual (baca kode + cek balance brace/paren: `{}`
333/333, `()` 1201/1201, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Item belum-terverifikasi bertambah 2: (1) label 3 tab tidak lagi
kepotong ellipsis di kondisi FONT normal (perlu device fisik, layar sempit maupun lebar — font-
scale aksesibilitas BESAR tetap bisa memicu ellipsis by design, itu memang jaring pengaman Batch
442 yang disengaja, bukan target "minimize" batch ini); (2) drag cepat (flick) di tab-bar berhenti
TEPAT di tab yang jari lepaskan (0 lagi "mundur 1 kolom"), demikian pula tap-tab biasa — perlu
device fisik, terutama drag flick cepat lintas >1 kolom.

**Catatan Batch 451**: user konfirmasi via video device asli — pill kembali ukuran NORMAL (fix
Batch 450 valid, 0 lagi kapsul raksasa). Analisis frame-by-frame 60fps tambahan (bukan cuma
laporan user) juga mengonfirmasi temuan ASLI Batch 449: pill kini meluncur mulus dari 1 tab ke
tab lain TANPA kilatan kotak abu-abu di tab yang ditinggalkan — fix hapus `NavigationBarItem`
(Batch 449) + fix `fillMaxHeight` (Batch 450) keduanya TERBUKTI benar di device fisik. 0 kode
diubah batch ini (murni sinkronisasi status verifikasi ke docs). Detail: `CHANGELOG.md` Batch 451.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. `GlassTabIcon`: background/border pill glass per-tab (non-Skeu) DIHAPUS TOTAL — dulu setiap
   tab menggambar pill sendiri dibatasi lebar kolomnya. Skeu (pill solid diskrit, aturan Batch
   58/61/79) TIDAK disentuh.
2. `AppNavHost`/`NavigationBar`: `tabBarDragLastIndexPx` + `tabBarDragBridgeAlpha` (state Batch
   446, pill "bridge" yang hanya aktif KONDISIONAL saat drag) dihapus, diganti 1
   `Animatable navPillIndexAnim` — sumber posisi rest pill tunggal, valid di SEMUA state.
   `drawWithContent` pada `NavigationBar` sekarang SATU-SATUNYA penggambar pill (aktif idle/tap/
   drag/nudge, dulu cuma drag), posisi: live `tabBarDragIndexPx` saat drag langsung (0 lag,
   mentah 1:1 jari — pola Batch 444/445 dipertahankan persis), fallback nudge-konten
   (`tabDragOffsetPx`, pecahan ±0.5 kolom dari titik rest) atau `navPillIndexAnim.value` saat
   diam. `navPillIndexAnim` di-snapTo+animateTo(tween 220ms) di titik SELESAI drag (handoff dari
   posisi jari terakhir) & di 3 `onClick` tap biasa — durasi SAMA PERSIS `glassAlphaAnim`
   (warna ikon/label) supaya pill & warna tiba bersamaan.
3. Warna ikon/label (`lerp` kontinu ikut jari, `tabBarDragFocus`/`tabMagnifyFocus`) TIDAK
   diubah — bagian itu SUDAH 1:1 sesuai video (state Batch 447), murni pill BACKGROUND yang
   direstrukturisasi total.
4. 0 import baru (`Animatable`/`tween`/`drawWithContent`/dll semua sudah ada sejak batch lalu).
   1 komentar header import (`drawWithContent`, dekat baris import) diperbarui — sebelumnya
   menyebut identifier `tabBarDragBridgeAlpha` yang kini sudah dihapus (anti-stale).

0 file lain disentuh. `README.md` + `CHANGELOG.md` diperbarui (bullet unverified tab-bar +
entry Batch 448 baru) — detail lengkap di masing-masing file.

**0 diverifikasi CI/device Batch 448** — review manual (baca kode + cek balance brace/paren:
`{}` 331/331, `()` 1117/1117, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Item belum-terverifikasi bertambah 1: pill unified meluncur mulus
lintas kolom TANPA seam/kotak ganda (fix utama batch ini, PALING PENTING dikonfirmasi krn itu
persis komplain user), TANPA regresi ke item Batch 442/444/445/446/447 yang sudah ada (label tak
terpotong, tahanan visual ujung kolom, 0 lag drag, warna ikon+label lerp bersamaan) — perlu
konfirmasi device fisik (0 tersedia sesi ini, sama seperti Batch 435-447).

**Catatan Batch 447**: user lampirkan video referensi baru (rekaman iOS Jam asli — drag lintas 4
tab Alarm/Jam dunia/Timer/Stopwatch) + instruksi "lanjutkan progress menuju mekanisme tampilan
sesuai video" — 0 poin komplain tertulis eksplisit, ZIP sama (`SONIX_v446.zip`, 0 source baru).
Perluasan langsung sektor drag tab-bar yang sama (Batch 442/444/445/446), bukan reopen sektor
DITUTUP manapun.

**Analisis video** (50 frame @3fps, cross-check ke kode): mekanisme bridging pill lintas-kolom +
lerp warna ikon (Batch 440/446) SUDAH cocok 1:1 dgn video. 1 gap ditemukan lewat pembacaan kode
(bukan asumsi visual semata): label teks (`MagnifyingTabLabel`) TIDAK ikut lerp warna kontinu —
beda dari ikon yang sudah (root cause detail: komentar inline kode dekat definisi fungsi ini).

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. `MagnifyingTabLabel` param baru `color: Color? = null` (default = 0 override, IDENTIK
   perilaku lama) → `Text(color = color ?: Color.Unspecified, ...)`.
2. `GlassTabIcon` (titik pemanggil): `labelColor` baru = `lerp(unselectedTextColor, tint,
   glassAlpha)` (pola PERSIS `unselectedIconColor` ikon, 0 hitungan/token warna baru), null utk
   Skeu (aturan solid Batch 58/61/79 tidak disentuh). Parameter ke-2 `MagnifyingTabLabel`
   diganti dari `focus` mentah → `glassAlpha` (identik selama drag aktif — `glassAlpha`
   snapTo(focus) tiap frame drag; beda HANYA di jendela easing 220ms pasca lepas jari: kini
   scale/opacity label ikut melunak bareng warna, bukan snap instan sendirian spt sebelumnya).
3. 0 import baru (`Color`/`lerp`/`NavigationBarItemDefaults` semua sudah ada sejak batch lalu).

0 file lain disentuh.

**0 diverifikasi CI/device Batch 447** — review manual (baca kode + cek balance brace/paren:
`{}` 331/331, `()` 1086/1086, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Item belum-terverifikasi bertambah 1: label teks kini benar2 berubah
warna BERSAMAAN dgn ikon secara kontinu selama drag (bukan cuma ikon, teks menyusul-lompat pas
commit index-crossing) DAN scale/opacity label easing mulus pasca lepas jari (0 snap instan) —
perlu konfirmasi device fisik (0 tersedia sesi ini, sama seperti Batch 435-446).

**Catatan Batch 446**: feedback eksplisit user PASCA Batch 445 (video ilustrasi dilampirkan,
perluasan langsung drag tab-bar Batch 442/444/445) — 1 poin: animasi pill masih terpisah oleh
gap kosong kecil di antara label tab; seharusnya warna/semantik ikut jari juga lintas celah itu.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` — root cause: `tabBarDragFocus`
(Batch 444) SUDAH kontinu secara matematis (diverifikasi manual — crossfade tepat 0.5/0.5 pas di
batas 2 kolom), TAPI tiap `GlassTabIcon` (3 titik pemakaian) menggambar pill highlight-nya
SENDIRI-SENDIRI dibatasi ke kolom masing-masing — 2 pill setengah-nyala itu tetap 2 kotak
TERPISAH dgn spasi tak-tergambar (padding internal kolom NavigationBarItem) di antaranya, kebaca
mata sbg "jeda"/patah walau angka focus-nya kontinu. Bug lapisan render, bukan bug angka.
1. **Fix utama**: 1 pill TAMBAHAN (bukan pengganti 3 pill `GlassTabIcon` lama — itu TETAP jalan
   apa adanya utk tap/nudge-konten-swipe/idle, 0 regresi di situ) digambar via
   `Modifier.drawWithContent` pada `NavigationBar` itu sendiri (bukan composable/Box baru, 0
   restrukturisasi tree) — HANYA aktif selama drag LANGSUNG di tab-bar (`tabBarDragIndexPx` bukan
   NaN). Posisi X dari nilai kontinu yang SAMA PERSIS (`idxPos * columnWidth`) — bebas meluncur
   MELINTASI celah antar kolom krn 1 kanvas bersama, bukan per-composable. Warna/alpha/radius
   IDENTIK pill lama (tint primary, 0.16f/0.14f, 16.dp) — 1 aksen visual, cuma lapisannya beda.
2. **State baru**: `tabBarDragLastIndexPx` (posisi valid terakhir, krn `tabBarDragIndexPx` sendiri
   balik NaN duluan tepat saat jari lepas) + `tabBarDragBridgeAlpha` (`Animatable`, snapTo(1)
   instan saat drag mulai — konsisten filosofi real-time Batch 445 — animateTo(0, tween(220)) saat
   jari lepas, durasi sinkron `glassAlphaAnim` yg sudah ada) supaya fade-out pill baru 0
   lompatan/pop visual pas handoff ke 3 pill lama.
3. Skeu DIKECUALIKAN (`isSkeuTheme()`, dibaca 1x baru sbg `navBarIsSkeu`) — aturan lama "solid,
   bukan kaca" (Batch 58/61/79) tidak disentuh.
4. Import baru: `androidx.compose.ui.draw.drawWithContent` (SATU-SATUNYA import baru — Offset/
   Size/CornerRadius/Stroke fully-qualified inline, pola sama persis `Offset(0f,0f)` yg sudah ada).

0 file lain disentuh. **Koreksi dok tambahan** (Anti-Stale): "Konvensi penamaan ZIP & versi" di
bawah & README.md § "Standar Penomoran Versi" masih menyebut `AudioPlayer-batchN-release.zip` —
sumber P1 (nama ZIP user, `SONIX_v445.zip`) & `app_name`/judul README konfirmasi branding AKTIF =
**SONIX** (package `com.rudi.audioplayer`/`rootProject.name="AudioPlayer"` SENGAJA tetap, lihat
"Aturan sesi aktif" #5 — tidak terikat branding). Kedua baris diperbarui ke `SONIX_v<batch>.zip` —
skema versionCode/versionName/APK/tag rilis TIDAK terkait, TIDAK ikut diubah.

**0 diverifikasi CI/device Batch 446** — review manual (baca kode + cek balance brace/paren: `{}`
331/331, `()` 1060/1060, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses jaringan
Gradle di sesi ini. Item belum-terverifikasi bertambah 1: pill drag bersama terlihat MENYATU mulus
melintasi celah antar-label (bukan 2 pill terpisah) sesuai video ilustrasi user, warna/posisi tetap
1:1 sinkron jari (regresi Batch 445 tidak terjadi), DAN handoff ke 3 pill lama pas jari dilepas 0
lompatan visual — semua perlu konfirmasi device fisik (0 tersedia sesi ini, sama seperti Batch
435-445).

**Catatan Batch 445**: feedback eksplisit user PASCA Batch 444 (bukan reopen sektor DITUTUP
manapun, perluasan langsung drag tab-bar Batch 442/444) — 2 poin: (1) "drag jari real-time belum
sepenuhnya smooth like iOS", (2) "floating effect HANYA saat drag, tampilan yang dilewati berubah
warna seketika real-time (bukan cuma pindah warna instant lintas tab)".

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` — root cause TUNGGAL utk kedua
poin: `animateFloatAsState(targetValue = focus, tween(220))` di `GlassTabIcon` (Batch 440) adalah
lapis smoothing KEDUA di atas `focus` yang SUDAH kontinu real-time (fungsi tenda
`tabBarDragFocus`, Batch 444) — targetnya bergerak tiap event pointer-move selama drag, jadi
tween 220ms terus "mengejar" target yang TERUS PINDAH → nilai yang dirender SELALU tertinggal
dari posisi jari asli (poin 1), dan warna ikon/pill (lerp ikut `glassAlpha`) terlihat
menyusul-lompat bukan berubah seketika sinkron dgn jari (poin 2).
1. **Fix utama**: `animateFloatAsState` → `Animatable` manual + param baru `isDragging: Boolean`
   di `GlassTabIcon` (dihitung 1x di pemanggil: `isTabBarDragging = tabBarDragIndexPx bukan NaN
   ATAU tabDragOffsetPx != 0`, cover 2 sumber drag — tab-bar langsung + nudge swipe-konten).
   Selama `isDragging` true: `snapTo(focus)` tiap frame (0 animasi, 1:1 sinkron mentah — pola
   sama `tabDragOffsetPx`/`tabBarOverscrollPx`). Selesai drag: `animateTo(focus, tween(220))`
   dari titik sinkron terakhir (0 lompatan). Tap biasa (0 drag) tetap tween(220) lama, 0 regresi.
2. **Fix pendukung**: `MagnifyingTabLabel` — `style = baseStyle.copy(fontSize = ...)` (real
   remeasure/relayout tiap frame drag, dobel dgn `graphicsLayer` scale) DICABUT, diganti
   `style = baseStyle` polos + faktor `graphicsLayer` scale dinaikkan 0.08f→0.23f (magnitude
   visual akhir dipertahankan sama, ≈1.23x lama). Kontribusi ke stutter drag (layout-pass
   berulang) dihapus, 0 perubahan tampilan yang diminta user.
3. Import `animateFloatAsState` dicabut (0 pemakaian lain tersisa di file).

0 file lain disentuh. 0 dependency baru, 0 import baru (`Animatable`/`tween`/`LaunchedEffect`/
`remember` semua sudah ada sejak batch sebelumnya).

**0 diverifikasi CI/device Batch 445** — review manual (baca kode + cek balance brace/paren:
`{}` 321/321, `()` 1014/1014, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Item belum-terverifikasi bertambah 1: real-time drag tab-bar terasa
1:1 mengikuti jari TANPA lag (poin 1), warna ikon/pill berubah seketika sinkron jari selama drag
DAN tetap cross-fade halus untuk tap biasa (poin 2), transisi mulus TANPA lompatan visual pas
jari dilepas (handoff drag→settle) — SEMUA perlu konfirmasi device fisik (0 tersedia sesi ini,
sama seperti Batch 435-444).

**Catatan Batch 444**: user konfirmasi CI Batch 443 hijau, lanjut feedback eksplisit (bukan
reopen sektor DITUTUP manapun, perluasan langsung drag tab-bar Batch 442) — 3 poin dipilih via
opsi tersaring: (1) pill/capsule 0 ikut posisi jari real-time (baru "lompat" pas commit
index-crossing), (2) 0 tahanan visual di ujung kolom (Beranda/Pengaturan), (3) "border tab nav
terluar kebesaran".

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. **Live pill-tracking**: state baru `tabBarDragIndexPx` (posisi kontinu 0f..3f, NaN = 0 drag
   aktif di tab-bar ini — TERPISAH dari `tabDragOffsetPx` milik swipe konten Batch 435/437, beda
   area sentuh, 0 saling pakai), ditulis SINKRON di loop `awaitEachGesture` yang sudah ada (dari
   `x` yang SAMA PERSIS dipakai hitung `newIndex`, 0 hitungan ganda). Fungsi baru
   `tabBarDragFocus(tabIndex)`: NaN → fallback `tabMagnifyFocus` (0 regresi nudge swipe-konten
   lama); aktif → fungsi tenda (jarak posisi kontinu ke titik tengah tiap kolom, 1f di tengah
   turun linear ke 0f di jarak 1 kolom) gantikan `tabMagnifyFocus` di 3 titik pemakaian
   `GlassTabIcon(focus = ...)` — pill kini "hidup" mengikuti jari kontinu SEBELUM index-crossing
   commit, bukan cuma bereaksi sesudahnya.
2. **Tahanan visual ujung kolom**: `tabBarOverscrollPx` (sumber kebenaran sinkron, dibaca
   `graphicsLayer{translationX=...}` di modifier terluar `NavigationBar`) dari `rawX` (posisi
   jari SEBELUM di-coerce ke batas bar) redaman 0.3f + batas ±24px (pola identik
   `tabDragOffsetPx.floatValue = totalTabDrag * 0.3f` yg sudah ada) — kapsul nge-"give" halus
   pas jari didorong lewat ujung Beranda/Pengaturan, springback ke 0 lewat `tabBarOverscrollAnim`
   (`Animatable`, spring dampingRatio/stiffness IDENTIK `tabDragOffset`/`AlbumArtHero`) via
   `tabSwipeScope` (REUSE scope yang sudah ada) — pola *Px-sinkron/Animatable-springback-only
   PERSIS sumbu fix Batch 433/434 (0 coroutine per-delta), non-blocking (awaitEachGesture 0
   nunggu springback selesai sebelum siap terima down berikutnya).
3. **Fix "kebesaran"**: root cause — `windowInsets` default `NavigationBar`
   (`NavigationBarDefaults.windowInsets`) masih mereservasi tinggi system-nav-bar DI DALAM
   kapsul, padahal Batch 439 sudah floating-kan kapsul via margin LUAR (`.padding(bottom=12.dp)`)
   — inset itu jadi DOBEL terhitung (dalam tinggi kapsul + margin luar), bikin kapsul lebih
   tebal dari semestinya. Fix: `windowInsets = WindowInsets(0,0,0,0)` di titik pemakaian INI SAJA
   (bukan ganti default app-wide). Margin luar 12.dp (Batch 439) TETAP jalan sendiri, 0 risiko
   baru ketutup gesture-nav.

0 file lain disentuh. 0 dependency baru, 0 import baru (`Animatable`/`spring`/`Spring`/
`WindowInsets`/`graphicsLayer` semua sudah ada sejak batch sebelumnya).

**0 diverifikasi CI/device Batch 444** — review manual (baca kode + cek balance brace/paren:
`{}` 317/317, `()` 977/977, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Item belum-terverifikasi bertambah 1: live-tracking pill (halus
mengikuti jari lintas kolom, 0 lag/jitter), tahanan ujung Beranda/Pengaturan (terasa "ketahan"
bukan keras/kaku, springback halus), dan kapsul terlihat lebih ramping (bukan lagi "kebesaran")
tanpa closeup ke gesture-nav bar di device asli — SEMUA perlu konfirmasi device fisik (0 tersedia
sesi ini, sama seperti Batch 435-443).

**Catatan Batch 443**: trigger `log_fail_427.zip` — fix Batch 442 (drag langsung di tab bar) GAGAL
compile CI: `Unresolved reference 'awaitFirstDown'` di 2 titik (`MainActivity.kt:187` importnya
sendiri, `:1349` titik pakainya). Root cause: salah paket saat penulisan Batch 442 — `awaitFirstDown`
(extension fun `AwaitPointerEventScope`, dipakai dgn parameter `pass`) sebenarnya dideklarasikan
di `androidx.compose.foundation.gestures` (satu paket persis dgn `awaitEachGesture` yg SUDAH benar
diimport baris atasnya), BUKAN `androidx.compose.ui.input.pointer` (paket itu isinya
`PointerEventPass`/`AwaitPointerEventScope` doang, 0 fungsi util gesture semacam ini) — bukan API
yang berubah/deprecated, murni asumsi paket keliru.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` — 1 baris import diganti:
`androidx.compose.ui.input.pointer.awaitFirstDown` → `androidx.compose.foundation.gestures.awaitFirstDown`.
0 baris lain disentuh, 0 logic/behavior berubah (fix murni resolusi symbol compile-time).

**0 diverifikasi CI/device Batch 443** — review manual (baca kode + cek balance brace/paren:
`{}` 308/308, `()` 924/924, `[]` 3/3 — IDENTIK Batch 442 krn cuma ganti teks path 1 baris import),
0 env Android nyata/device fisik/compiler Kotlin/akses jaringan Gradle di sesi ini. Seluruh item
belum-terverifikasi Batch 442 (label/pill/blur/drag tab bar) TETAP di daftar bawah apa adanya —
fix compile ini TIDAK otomatis mengkonfirmasi behavior runtime-nya, cuma membuka jalan CI hijau.

**Catatan Batch 442**: laporan eksplisit user (screenshot bottom nav bar) — 3 masalah sekaligus:
(1) label "Perpustakaan"/"Pengaturan" terpotong jadi "Perpusta"/"Pengatur", (2) pill "Beranda"
tampak anomali besar, (3) "efek blur useless" di 2 label nonaktif, + permintaan fitur baru
(4) "tambahkan fitur drag pada tab, bukan hanya tap-tab doang". Perluasan langsung sektor nav
bawah yang sama (Batch 301/435/437/438/439/440/441), bukan reopen sektor DITUTUP manapun.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. **Fix (1)+(2), root cause tunggal**: `MagnifyingTabLabel` (Batch 437) baca `LocalTextStyle
   .current` sbg base style — asumsi ini SALAH sejak Batch 439 memindahkannya dari slot `label`
   NavigationBarItem (yg M3 otomatis bungkus `ProvideTextStyle(labelMedium)`) ke slot `icon`
   (`GlassTabIcon`), di mana `LocalTextStyle.current` jatuh balik ke ambient default
   MaterialTheme (bodyLarge, jauh lebih besar) — persis item "belum-terverifikasi" yg sudah
   diperingatkan sendiri di PROJECT_STATE.md sejak penutupan Batch 439 ("x font-scale besar").
   Fix: baca `MaterialTheme.typography.labelMedium` langsung (token M3 resmi, IDENTIK dgn
   default `label` slot) — 0 hardcode sp baru. `overflow = TextOverflow.Ellipsis` ditambah sbg
   jaring pengaman (sebelumnya 0 di-set, default `Clip` yg menghasilkan potongan huruf mentah).
2. **Fix (3)**: `.blur(((1f - clampedFocus) * 1.3f).dp)` di `MagnifyingTabLabel` DICABUT — radius
   idle (tab tidak sedang digeser) = 1.3dp KONSTAN di 2 dari 3 label SETIAP SAAT, bukan cuma
   sesaat selama drag; screenshot user konfirmasi 0 manfaat visual, cuma bikin
   "Perpustakaan"/"Pengaturan" buram permanen. `scaleX`/`scaleY`/`alpha` (`graphicsLayer`, sinyal
   fokus kontinu Batch 437) TETAP jalan — cuma komponen blur yg dicabut. Import
   `LocalTextStyle`/`androidx.compose.ui.draw.blur` ikut dilepas (sudah 0 pemakaian lain).
3. **Fitur baru (4)**: drag LANGSUNG di atas tab bar (bukan cuma di konten layar spt swipe Batch
   435) — gaya segmented-control iOS, tekan 1 tab lalu geser jari TANPA angkat, tab ikut
   berpindah mengikuti posisi jari lintas 3 kolom (equal-width, M3 default). Teknik:
   `Modifier.pointerInput(Unit) { awaitEachGesture { ... } }` di `PointerEventPass.Initial`
   (bukan default `Main`) + 0 `change.consume()` sama sekali — event dibaca SEBELUM child
   `NavigationBarItem` memproses Main pass-nya sendiri, jadi tap polos/ripple-feedback
   (`bouncyPress` Batch 438) 0 terganggu (tap singkat = index hover tidak pernah berubah dari
   titik down = blok navigate() custom ini tidak pernah tereksekusi, murni `onClick` bawaan yg
   menangani). Key `pointerInput` sengaja `Unit` (bukan `currentRoute`) + `rememberUpdatedState
   (currentRoute)` baru (`currentRouteState`) — `NavigationBar` composable ini TIDAK
   keluar-masuk komposisi selama pindah antar 3 tab (kondisi pembungkusnya tetap true), jadi
   coroutine gesture aman hidup terus lintas tab (drag 1 jari lewat >1 batas tab, mis. Beranda
   langsung ke Pengaturan, tidak macet di tab tengah) — kalau di-key `currentRoute` malah restart
   tiap 1 batas terlewati krn `navigate()` mengubah key itu sendiri di tengah gesture yg sama.
   `navController.navigate` pakai opsi IDENTIK popUpTo/launchSingleTop/restoreState (pola Batch
   301/435), 0 state-preservation baru. Haptic tick per tab berpindah REUSE `tabSwipeHaptic`
   (Batch 435, `HapticFeedbackType.LongPress`), 0 API haptic baru. Swipe konten Batch 435 (Box
   pembungkus NavHost) TIDAK disentuh — 2 mekanisme drag independen, beda area sentuh.

`NavigationRailItem` (tablet) TIDAK disentuh — di luar scope (sama seperti Batch 437-441).

**0 diverifikasi CI/device Batch 442** — review manual (baca kode + cek balance brace/paren:
`{}` 308/308, `()` 924/924, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Perlu ditest device asli: label "Perpustakaan"/"Pengaturan" tidak
lagi terpotong di ukuran font default MAUPUN font-scale aksesibilitas besar, pill "Beranda"
proporsional (bukan lagi anomali besar), 0 blur tersisa di 2 label nonaktif, dan drag jari
lintas tab bar berpindah tab dgn benar (termasuk drag cepat lintas >1 batas tab) TANPA
mengganggu tap biasa/ripple-feedback yang sudah ada. Item belum-terverifikasi bertambah 1.

**Catatan Batch 441**: trigger `log_fail_425.zip` — fix Batch 440 (`IndicationNodeFactory`)
ternyata belum lengkap: interface itu me-re-abstract `equals`/`hashCode` (deklarasi ulang
eksplisit, bukan cuma warisan default `Any`), jadi `object NoRippleIndication` WAJIB
mengimplementasi keduanya eksplisit — 0 diketahui saat migrasi Batch 440 (bukan bagian pesan
error compile SEBELUMNYA, baru muncul SETELAH kontrak lamanya diganti). Perluasan langsung fix
compile Batch 440, sektor sama (nav bawah, Batch 301/435/437/438/439/440).

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` — `NoRippleIndication`
ditambah `override fun equals(other: Any?): Boolean = other === this` +
`override fun hashCode(): Int = -1`. Identity check sederhana cukup (1 instance singleton
sepanjang hidup app, 0 state pembeda) — bukan logic baru, murni memenuhi kontrak interface.
0 file lain disentuh.

**0 diverifikasi CI/device Batch 441** — review manual (baca kode + cek balance brace/paren:
`{}` 300/300, `()` 865/865, `[]` 3/3), 0 env Android nyata/device fisik/compiler Kotlin/akses
jaringan Gradle di sesi ini. Fix ke-2 berturut-turut utk kontrak `IndicationNodeFactory` yang
sama (Batch 440 lalu ini) — BELUM dikonfirmasi CI hijau nyata, run berikutnya WAJIB dicek utuh
(bukan cuma diasumsikan beres krn pesan error sebelumnya sudah hilang dari log). Item
belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 440**: trigger ganda dari user — (1) `log_fail_424.zip`, `compileDebugKotlin`/
`compileReleaseKotlin` FAILED di CI (`e:` bukan `w:` — level deprecation `Indication`/
`IndicationInstance` yang dipakai `NoRippleIndication` Batch 439 sudah naik jadi HARD ERROR di
compose-bom 2026.04.01, bukan lagi cuma warning); (2) re-lampiran panduan
`drag_drop_glass_ios_kotlin.md` + instruksi eksplisit "ubah behavior sesuai source code
lampiran, adaptasi bukan timpa plek ketiplek". Perluasan langsung sektor nav bawah yang sama
(Batch 301/435/437/438/439), bukan reopen sektor DITUTUP manapun.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. **Fix compile**: `NoRippleIndication` dimigrasi dari kontrak lama `Indication`/
   `IndicationInstance` (`rememberUpdatedInstance`) ke kontrak resmi pengganti
   `IndicationNodeFactory` + `Modifier.Node`/`DrawModifierNode` (`create()`/`ContentDrawScope.draw()`).
   0 behavior berubah — masih murni `drawContent()` kosong, 0 layer visual, titik pemakaian
   `CompositionLocalProvider(LocalIndication provides NoRippleIndication)` di `bottomBar` TIDAK
   disentuh (`IndicationNodeFactory` = subtipe `Indication`, tetap kompatibel).
2. **Adaptasi behavior guide**: 2 elemen guide (`lerp` posisi/opacity kapsul & warna ikon
   mengikuti persentase geser jari `HorizontalPager` secara langsung) diadaptasi ke arsitektur
   riil (permanent NavHost routes, BUKAN HorizontalPager — swap ke pager tetap ditolak sejak
   Batch 435/438 dgn alasan sama: breaking ke state-restoration/NavigationRail tablet). App ini
   sudah punya padanan persis `pageOffsetFraction` guide sejak Batch 435/437: `focus`
   (`tabMagnifyFocus`, live tiap frame drag). Target `glassAlpha` (`GlassTabIcon`) diganti dari
   `if (selected) 1f else 0f` (statis, cuma reaksi post-commit) jadi `focus` langsung — idle
   value SAMA PERSIS 1f/0f (0 regresi tap, tween 220ms Batch 439 tetap jalan), bedanya kini
   pill JUGA bereaksi kontinu selama drag berlangsung. Ikon sendiri (elemen guide yg belum
   pernah diadaptasi batch manapun) kini ikut `lerp` warna kontinu persis teknik guide
   (`androidx.compose.ui.graphics.lerp`) dari `NavigationBarItemDefaults.colors().unselectedIconColor`
   (token M3 resmi, 0 hardcode warna baru) ke `tint` (primary, aksen sama dgn pill) — ikon & pill
   kini 1 aksen bergerak bersama. Skeu DIKECUALIKAN dari lerp ikon (aturan solid Batch 58/61/79),
   tetap tint default M3 apa adanya. Reorder drag-to-swap & `HorizontalPager` literal dari guide
   TETAP tidak dipakai (rasionalisasi sama persis Batch 438, tidak diulang di sini).

`NavigationRailItem` (tablet) TIDAK disentuh — di luar scope (sama seperti Batch 437/438/439).
Detail penuh: `CHANGELOG.md` § Batch 440.

**0 diverifikasi CI/device Batch 440** — review manual (baca kode + cek balance brace/paren:
`{}` 300/300, `()` 861/861, `[]` 3/3), tidak ada env Android nyata/device fisik/compiler
Kotlin/akses jaringan Gradle di sesi ini — fix compile berbasis pembacaan API resmi
`IndicationNodeFactory`/`DrawModifierNode` (stabil sejak Compose UI 1.6+, konsisten dgn
compose-bom 2026.04.01 project ini), BELUM dikonfirmasi CI hijau nyata. Item belum-terverifikasi
bertambah 1 (lihat daftar di bawah).

**Catatan Batch 439**: permintaan eksplisit user — 2 screenshot referensi (nav app ini vs tab bar
iOS Jam/Clock), "perbaiki bottom nav bar agar lebih mirip gaya visual iOS app jam tersebut,
matikan ripple khas Android saat klik". Perluasan langsung dari sektor nav bawah yang sama
(Batch 301/435/437/438), bukan reopen sektor DITUTUP manapun.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` —
1. `GlassTabIcon` (Batch 438) diperluas ambil alih slot label (`label`/`focus` param baru,
   memanggil `MagnifyingTabLabel` Batch 437 yang sama persis) supaya highlight pill tab aktif
   membungkus IKON+LABEL sekaligus jadi 1 blok (dulu cuma bungkus ikon) — meniru referensi iOS
   Jam. Bentuk pill jadi `RoundedCornerShape(16.dp)` (dari stadium penuh `percent = 50`, yang
   di tinggi baru ini akan terlihat kapsul obat, bukan kotak rounded seperti referensi).
2. `NavigationBar` bawah kini kapsul mengambang (`.padding(horizontal 16.dp, bottom 12.dp)` LALU
   `.clip(RoundedCornerShape(28.dp))`, urutan modifier ini krusial) alih-alih persegi nempel edge-
   to-edge — meniru referensi iOS Jam. `windowInsets` bawaan (gesture-nav) tidak disentuh, margin
   ini tambahan di atasnya.
3. Ripple Android bawaan di 3 `NavigationBarItem` dimatikan lewat `Indication` kosong baru
   (`NoRippleIndication`, cuma `drawContent()`) dipasang via `CompositionLocalProvider(LocalIndication
   provides ...)` yang MEMBUNGKUS 3 `NavigationBarItem` — bukan `Modifier.clickable` baru, 0
   sentuh `selected`/`onClick`/route logic. `bouncyPress` (scale-down tekan, Batch 438) TETAP
   jalan sebagai feedback tekan pengganti.

`NavigationRailItem` (tablet/foldable) TIDAK disentuh — 2 screenshot referensi user keduanya nav
ponsel, di luar scope. Detail penuh + rasionalisasi: `CHANGELOG.md` § Batch 439.

**0 diverifikasi CI/device Batch 439** — review manual (baca kode + cek balance brace/paren:
`{}` 298/298, `()` 833/833, `[]` 3/3), tidak ada env Android nyata/device fisik/compiler
Kotlin/akses jaringan Gradle di sesi ini. Item belum-terverifikasi bertambah 1 (lihat daftar di
bawah).

**Catatan Batch 438**: permintaan eksplisit user — lampiran `drag_drop_glass_ios_kotlin.md` +
screenshot bottom nav, "hasil sebelumnya (Batch 437, efek kaca PEMBESAR di label) mengecewakan,
adaptasi 100% berdasarkan panduan". Perluasan langsung dari sektor nav bawah yang sama (Batch
301/435/437), bukan reopen sektor DITUTUP manapun.

**1 file diubah** (dalam batas 3 file/tugas): `MainActivity.kt` — composable baru
`GlassTabIcon(icon, selected, interactionSource)` jadi pill indicator translucent (tint 16%
alpha + border 14% alpha, `RoundedCornerShape(percent = 50)`, animasi cross-fade `tween(220)`
mengikuti `selected`) menggantikan indicator flat default M3 di belakang ikon 3 tab bawah,
plus `bouncyPress()` (konvensi tekan-tactile existing) untuk scale-down halus saat ditekan.
Detail penuh + rasionalisasi kenapa 2 elemen panduan asli (reorder drag-to-swap tab, dan
`Modifier.blur(20.dp)` literal di container) SENGAJA tidak dipakai 1:1 — bukan penolakan,
adaptasi ke arsitektur riil (route nav permanen + blur asli Haze sudah dimatikan permanen Batch
329 + `frostedGlass()` existing didesain utk panel besar bukan pill sekecil ini): `CHANGELOG.md`
§ Batch 438. `isSkeuTheme()` dikecualikan (aturan "solid, bukan kaca" Batch 58/61/79, app-wide).
Catatan desain lama Batch 53 ("§15 jangan jadikan item nav jadi glowing glass capsule") SECARA
EKSPLISIT disupersede oleh instruksi user batch ini utk 5 identitas non-Skeu — kaskade
DESCENDING TRUTH: instruksi eksplisit baru > catatan/spec lama, dicatat di sini + README.md
(bukan dihapus diam-diam dari histori). `MagnifyingTabLabel`/`tabMagnifyFocus` (Batch 437) TIDAK
dihapus — 2 efek (kaca ikon + pembesar label) jalan berdampingan.

**0 diverifikasi CI/device Batch 438** — review manual (baca kode + cek balance brace/paren:
`{}` 293/293, `()` 792/792, `[]` 3/3), tidak ada env Android nyata/device fisik/compiler Kotlin
di sesi ini. Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 437**: permintaan FITUR BARU eksplisit user (lampiran screenshot bottom nav) —
efek "kaca pembesar ala iOS" di LABEL 3 tab bawah (Beranda/Perpustakaan/Pengaturan), bereaksi
tergantung "kaca diarahkan kesitu/bukan". Bukan reopen sektor DITUTUP manapun — perluasan
langsung dari fitur swipe Batch 435 (sektor sama, belum pernah ditutup).

**1 file diubah** (dalam batas 3 file/tugas):
1. `MainActivity.kt` (`AppNavHost`) — 0 gesture/state baru: `tabMagnifyFocus(tabIndex)` (fungsi
   lokal baru) murni MEMBACA ULANG `tabDragOffsetPx` (`MutableFloatState` Batch 435, ±40px,
   sudah live tiap frame `onHorizontalDrag` + sudah spring-back ke 0 di `onDragEnd`/
   `onDragCancel`) sebagai bobot fokus 0f..1f per tab — tab yang sedang aktif mulai dari fokus
   1f dan turun mengikuti `max(towardNext, towardPrev)` selama drag, tab tetangga yang dituju
   naik dari 0f ke arah 1f secara kontinu (BUKAN snap di ujung threshold 120px) — persis efek
   lensa bergeser dari 1 label ke label sebelah selama jari masih menekan.
   Label `NavigationBarItem` (`Text("Beranda")` polos dkk) diganti composable baru
   `MagnifyingTabLabel(text, focus)`: `fontSize` discale kontinu dari `LocalTextStyle.current`
   (bukan angka sp hardcode — ikut style/tema label bawaan apa pun yang aktif), plus
   `graphicsLayer{scaleX/scaleY/alpha}` + `Modifier.blur()` (aman tanpa cek `Build.VERSION`,
   minSdk project ini 31 = RenderEffect selalu tersedia) — tab fokus penuh jadi sedikit lebih
   besar/tajam/terang, tab non-fokus mengecil/buram/redup sebagian, transisi mengikuti jari
   frame-demi-frame. HANYA `NavigationBar` bawah (layout ponsel COMPACT, sesuai screenshot user)
   yang disentuh — `NavigationRailItem` (tablet/foldable Medium/Expanded) SENGAJA tidak ikut
   diubah, di luar scope diminta (screenshot user = bottom bar ponsel), 0 side-quest. Dibaca di
   titik pemakaian (dalam tiap `label = { ... }`, bukan di-hoist ke `NavigationBar`) supaya scope
   recomposition sekecil mungkin (hanya `Text` label yang recompose tiap frame drag, bukan
   seluruh bar) — pola read-state-di-leaf yang sama dipakai `BlurUtils.kt`/`IosScrollPhysics.kt`.
   0 breaking change: `selected`/`onClick`/`icon`/route logic Batch 301/435 tidak disentuh sama
   sekali, warna label selected/unselected tetap 100% dari `LocalContentColor` bawaan M3 (tidak
   di-override). Detail lengkap: `CHANGELOG.md` § Batch 437.

**0 diverifikasi CI/device Batch 437** — review manual (baca kode + cek balance brace/paren:
`{}` 286/286, `()` 734/734, `[]` 3/3), tidak ada env Android nyata/device fisik di sesi ini.
Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 436**: user laporan "abis update saya nunggu buffer screen lumayan ±20s",
menduga terkait fitur swipe Batch 435. Investigasi (grep, bukan asumsi) — **0 file diubah**:
1. Diff Batch 435 (`AppNavHost`) di-baca ulang penuh: isinya murni `pointerInput`/
   `detectHorizontalDragGestures` + `graphicsLayer` (kerja UI-thread, non-blocking, 0 I/O, 0
   panggilan network/disk baru). Tidak mungkin jadi sumber jeda 20 detik secara struktural.
2. Root cause sesungguhnya (kode sudah ada SEBELUM Batch 435, tidak disentuh): `ensureLibraryLoaded()`
   → `refreshLibrary()` (`PlayerViewModel.kt`) jalan di **setiap cold start proses** (flag
   `libraryLoadedOnce` in-memory, bukan persisted) — dikonfirmasi lewat komentar existing Batch
   419 sendiri: **"jalur paling panas cold-start"**. Scan `musicRepository.getAllSongs()` +
   (kalau ada) SAF custom folder via Binder/IPC per folder (`customFolderScanner.scan()`) jalan
   di `Dispatchers.IO` — sudah benar secara threading (non-blocking Main), tapi durasi wall-clock
   scan MediaStore tetap naik seiring ukuran library/jumlah folder custom, TIDAK instan. Install
   APK baru = proses baru = scan ini trigger ulang dari nol — persis skenario "abis update".
3. Kesimpulan: **bukan regresi Batch 435**. Perilaku ini sudah ada sebelum swipe gesture ditambah,
   ter-dokumentasi sendiri di README § fitur ("Shimmer skeleton loading" selama fase ini). Tidak
   ada perubahan kode.

**0 diverifikasi CI/device Batch 436** — kesimpulan murni dari pembacaan kode (grep + baca
`PlayerViewModel.kt`/`MusicRepository.kt`), tidak ada env Android nyata/device fisik di sesi ini.

**Catatan Batch 435**: permintaan FITUR BARU eksplisit user — "tambahkan gesture swipe able
lintas 3 tab. alih-alih user hanya bisa tap-tab manual berulang!!" (Beranda/Perpustakaan/
Pengaturan). Bukan reopen sektor DITUTUP manapun (lihat daftar "Sektor DITUTUP" di bawah) —
sektor terpisah, navigasi tab bawah belum pernah masuk 3 sektor yang ditutup itu.

**1 file diubah** (dalam batas 3 file/tugas):
1. `MainActivity.kt` (`AppNavHost`) — app ini pakai Jetpack Navigation Compose dengan 3 route
   top-level TERPISAH (`"home"`/`"library"`/`"settings"`, BUKAN `HorizontalPager` 1-route) —
   swap ke arsitektur pager penuh ditolak sebagai solusi (butuh restrukturisasi NavHost +
   `now_playing`/`stats_dashboard` jadi push-di-atas-pager, risiko regresi jauh lebih besar dari
   scope diminta). Pendekatan dipilih: `Modifier.pointerInput` + `detectHorizontalDragGestures`
   dipasang di `Box` pembungkus `NavHost`, HANYA aktif saat `currentRoute` ada di 3 tab itu
   (`TAB_ROUTES`, konstanta baru) — di `"now_playing"`/`"stats_dashboard"` modifier ini tidak
   terpasang sama sekali (0 rebutan dgn `detectHorizontalDragGestures` `AlbumArtHero` yang sudah
   ada di `now_playing`, Batch 434). Saat threshold ±120px terlampaui, swipe memicu
   `navController.navigate()` dengan opsi IDENTIK ke `onClick` `NavigationBarItem`/
   `NavigationRailItem` yang sudah ada (`popUpTo("home"){saveState=true}` + `launchSingleTop` +
   `restoreState`, pola Batch 301) — 0 state baru di sisi state-preservation, murni trigger
   berbeda (gesture, bukan cuma tap). `enterTransition`/`exitTransition` `NavHost` (fade
   200/150ms, Batch 330) TIDAK disentuh — dipakai apa adanya baik utk tap maupun swipe.

   Pola threshold 120px + haptic (`HapticFeedbackType.LongPress`) + `dragOffsetPx`
   (`MutableFloatState`, sinkron)/`Animatable` (springback-only, via `spring(DampingRatioMediumBouncy,
   StiffnessLow)`) REUSE 1:1 dari `AlbumArtHero` (`NowPlayingScreen.kt`, Batch 434) — sengaja
   tidak reinvent, termasuk `onDragStart` yang panggil `dragOffset.stop()` (jaring pengaman
   sinkron-vs-asinkron yang sama). Beda dari `AlbumArtHero`: nudge visual (`graphicsLayer
   translationX`) di sini dibatasi lebih kecil (±40px, multiplier 0.3, bukan ±48dp/0.5) karena
   yang digeser konten SATU LAYAR PENUH (bukan 1 kartu album), dan page-swap sesungguhnya tetap
   lewat fade `NavHost` yang sudah ada — nudge ini murni sinyal "tergenggam", bukan preview
   halaman berikutnya. `TAB_ROUTES.any{it==currentRoute}`/`indexOfFirst{it==currentRoute}`
   dipakai (bukan `in`/`indexOf` langsung) karena `currentRoute` bertipe `String?` sedangkan
   `List<String>.contains`/`indexOf` mengharap parameter non-null — perbandingan `==` selalu
   type-safe utk operand nullable, `in`/`indexOf` langsung berisiko unresolved/type-mismatch.
   0 breaking change ke `NavigationBarItem`/`NavigationRailItem`/route lain (signature/pola tap
   lama tidak disentuh sama sekali).

**0 diverifikasi CI/device Batch 435** — review manual (baca kode + cek balance brace/paren:
`{}` 280/280, `()` 700/700, `[]` 3/3), tidak ada env Android nyata/device fisik di sesi ini.
Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 434**: reopen eksplisit user — laporan spesifik "effect bounce juga masih
stuttering, belum smooth like butter!!". SAMA KELAS BUG dgn Batch 433 (`IosScrollPhysics.kt`),
tapi di file BEDA: `ui/NowPlayingScreen.kt` → `AlbumArtHero` (swipe horizontal next/prev pada
album art) — ditemukan lewat grep `bounce`/`spring(` menyeluruh ke seluruh `app/src/main/java`
(bukan tebakan single-file), setelah `IosScrollPhysics.kt` sendiri dikonfirmasi baca-kode sudah
bersih dari sumbu bug ini (drag sinkron via `dragOffset` sejak Batch 433, tidak disentuh lagi).

**1 file diubah** (dalam batas 3 file/tugas):
1. `ui/NowPlayingScreen.kt` (`AlbumArtHero`) — root cause identik Batch 433: `onHorizontalDrag`
   menulis posisi lewat `dragScope.launch { dragOffset.snapTo(...) }` di SETIAP delta drag —
   coroutine baru per delta, bisa menumpuk/tidak berurutan saat drag cepat. Fix: `dragOffsetPx`
   (`MutableFloatState` polos, via `mutableFloatStateOf`) jadi sumber kebenaran SINKRON yang
   dibaca `graphicsLayer` (ditulis LANGSUNG dari `onHorizontalDrag`, 0 coroutine). `dragOffset`
   (`Animatable`) tetap ada, sekarang HANYA dipakai di fase springback (`onDragEnd`/
   `onDragCancel`) — `snapTo` posisi drag terakhir dulu, tiap frame `animateTo` disinkronkan
   balik ke `dragOffsetPx` lewat parameter `block` resmi. Tambahan (gap yang tidak muncul di
   Batch 433 krn kasusnya scroll/fling bawaan `scrollable()`, bukan drag-gesture manual):
   `onDragStart` sekarang panggil `dragOffset.stop()` — jaring pengaman springback-lama-vs-
   drag-baru, supaya `block` lama berhenti menimpa `dragOffsetPx` kalau user mulai drag baru
   sebelum springback sebelumnya selesai. `totalDrag`/threshold swipe-next/prev 120px/haptic/
   `dampingRatio`/`stiffness` (Batch 256) TIDAK disentuh — sumbu bug ini murni SINKRON vs
   ASINKRON penulisan offset. Detail lengkap: `CHANGELOG.md` § Batch 434.

**0 diverifikasi CI/device Batch 434** — review manual (baca kode + cek balance brace/paren:
`{}` 292/292, `()` 1287/1287, `[]` 1/1), tidak ada env Android nyata/device fisik di sesi ini.
Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**Catatan Batch 433**: reopen eksplisit user — laporan spesifik "effect scrolling/transition like
iOS masih terasa stuttering gak halus sama sekali". Sumbu BARU, beda dari seluruh histori tuning
`ui/theme/IosScrollPhysics.kt` Batch 364-383 (semuanya soal KARAKTER pegas — stiffness/
dampingRatio/rubberBand, sudah dikonfirmasi user via banyak iterasi) — "stuttering" = gejala frame
drop/jank, bukan parameter animasi mana yang dipakai.

**1 file diubah** (dalam batas 3 file/tugas):
1. `ui/theme/IosScrollPhysics.kt` — root cause: `applyToScroll` (kontrak resmi non-suspend, justru
   supaya overscroll bisa diterapkan sinkron dalam frame sentuhan yang sama) sebelumnya menulis
   posisi lewat `coroutineScope.launch { overscrollOffset.snapTo(...) }` di SETIAP event scroll
   delta selama drag di zona overscroll — tiap delta bikin coroutine baru krn `Animatable.snapTo`
   cuma suspend, dan `launch` menambah giliran dispatcher yang bisa menumpuk/tidak berurutan saat
   drag cepat = persis gejala stutter yang dilaporkan. Fix: state baru `dragOffset`
   (`MutableState<Offset>` polos) jadi sumber kebenaran SINKRON yang ditulis LANGSUNG (0 coroutine)
   dari `applyToScroll`, pola sama `Modifier.pointerInput { detectDragGestures { ... } }` standar
   Compose. `overscrollOffset` (`Animatable`) tetap ada, sekarang HANYA dipakai internal di fase
   settle (`settleToZero`, sudah suspend by design) — tiap frame animasinya disinkron balik ke
   `dragOffset` lewat parameter `block` resmi `Animatable.animateTo`. `measure()` baca `dragOffset`
   (bukan lagi `overscrollOffset` langsung). `dampingRatio`/`stiffness`/`rubberBandResistance`
   (semua tuning Batch 368-383) TIDAK disentuh — sumbu bug ini murni soal SINKRON vs ASINKRON-nya
   penulisan offset, bukan parameter pegasnya. Detail lengkap: `CHANGELOG.md` § Batch 433.

**0 diverifikasi CI/device Batch 433** — review manual (baca kode + cek balance brace/paren), tidak
ada env Android nyata/device fisik di sesi ini. Item belum-terverifikasi bertambah 1 (lihat daftar
di bawah).

**Catatan Batch 425–430**: user secara eksplisit reopen **satu kali khusus** untuk Coil migration
(bump 2.6.0→3.x, 3 file + `build.gradle.kts`), lalu reopen KEDUA secara terpisah eksplisit untuk
sektor Compose optimization (`AlbumArt`, Batch 429) — bukan pencabutan status permanen. Versi
final Coil: **3.3.0**, HIJAU CI. `AlbumArt` AsyncImage swap — user konfirmasi device asli render
NORMAL, 0 regresi visual (Batch 430). Kedua sektor TUNTAS & terverifikasi penuh (CI + device).
Detail teknis lengkap: `CHANGELOG.md` § Batch 425–430.

**Catatan Batch 431**: reopen ketiga, eksplisit user minta audit menyeluruh atas seluruh sektor
"belum terjamah" (di luar `ui/`), KECUALI paket `update/` (`GitHubReleaseChecker.kt`,
`UpdateDownloader.kt`, `UpdateManager.kt` — waktu itu masih tertutup, kini disisir Batch 432
di bawah).

**Catatan Batch 432**: reopen eksplisit user untuk audit paket `update/` (3 file, belum pernah
disisir sebelumnya). Hasil:
- `GitHubReleaseChecker.kt` — `fetchLatest()` pakai `.execute()` blocking sengaja (bukan
  `suspend`), tapi HANYA dipanggil dari dalam `UpdateManager.scope.launch` (Dispatchers.IO) —
  tidak pernah jalan di Main thread. Komentar file menjelaskan `.string()` di sini aman karena
  cuma JSON kecil (beda dari APK binary di `UpdateDownloader`). Tidak diubah.
- `UpdateDownloader.kt` — `download()` streaming 8 KB per chunk ke disk (`Buffer`/`sink()`),
  TIDAK pernah `readBytes()`/`.string()` pada body APK — sesuai Safety Locks SOP. Dipanggil dari
  background thread oleh caller. Tidak diubah.
- `UpdateManager.kt` — **1 file diubah**. Gap nyata: `checkForUpdate()` &
  `downloadAndPrepareInstall()` jalan di `Thread {}` mentah, bukan Coroutines — melanggar SOP
  §2 Thread Safety ("WAJIB Coroutines"), dan Thread lepas tidak bisa dibatalkan kalau proses
  butuh cleanup. Fix: scope baru `CoroutineScope(SupervisorJob() + Dispatchers.IO)` milik
  singleton ini, `Thread { ... }.start()` → `scope.launch { ... }`. `kotlinx.coroutines` sudah
  jadi dependency existing (dipakai `FloatingBubbleService.kt` & lainnya) — 0 dependency baru.
  API publik (`checkForUpdate`, `downloadAndPrepareInstall`, `launchInstall`, `reset`, `state`)
  tidak berubah, 0 breaking change ke `UpdateCheckSheet.kt`.

**0 diverifikasi CI/device Batch 432** — review manual (baca kode + cek balance brace/paren),
sama seperti Batch 431. Item belum-terverifikasi bertambah 1 (lihat daftar di bawah).

**2 file diubah** (dalam batas 3 file/tugas):
1. `ui/DiagnosticLogSheet.kt` — 3 titik panggilan `AppLogger.readLog()`/`exportLogToDocuments()`/
   `clearLog()` sebelumnya jalan LANGSUNG di Main thread (initial value `remember` + 2x `onClick`),
   0 coroutine wrapper. Gap ini LOLOS dari audit literal-grep `ui/` sebelumnya karena syntax I/O
   asli (`FileInputStream`, `readText()`, dst) hidup di `util/AppLogger.kt` — paket lain, bukan di
   file `ui/` itu sendiri — bukan false positif, tapi kelas gap yang memang belum pernah disisir:
   call site fungsi lintas-paket yang blocking. Fix: `rememberCoroutineScope()` +
   `Dispatchers.IO`/`withContext(Dispatchers.Main)`, pola identik `SignatureMatcherSheet.kt`
   (Batch 421)/`BackupRestoreSheet.kt`.
2. `ui/DuplicateFinderSheet.kt` — item Compose-perf yang sudah tercatat (lihat riwayat rule di
   bawah): `DuplicateDetector.findLibraryDuplicates`/`findPhysicalDuplicates` (groupBy +
   sortedByDescending atas seluruh `songs`) sebelumnya jalan synchronous di dalam `remember` =
   bagian dari composition phase (Main thread). Fix: `LaunchedEffect(songs)` +
   `Dispatchers.Default` (CPU-bound, bukan I/O), state `isScanning` baru buat loading indicator
   supaya tidak salah tampil "0 duplikat ditemukan" sebelum hasil async selesai.

**Cakupan audit** (grep pola blocking I/O — `commit()`, `Thread.sleep`, `HttpURLConnection`/
`.execute()`, `FileInputStream`/`FileOutputStream`/`open{Input,Output}Stream`, `readBytes`/
`readText`/`writeText`/`writeBytes`, `runBlocking`, `BitmapFactory`, `MediaMetadataRetriever`,
`listFiles`/`walk`, `contentResolver.query` — di seluruh 70 file Kotlin di luar `ui/` & `update/`):
selain 2 fix di atas, SISANYA sudah benar (konfirmasi baca kode, bukan asumsi) —
`ApkSignatureChecker.inspect()` (Batch 421), `TagEditor`/`RingtoneEncoder` (viewModelScope.launch
Dispatchers.IO di `PlayerViewModel.kt`), `BackupManager` (scope.launch Dispatchers.IO di
`BackupRestoreSheet.kt`), `CustomFolderScanner.scan()` (withContext Dispatchers.IO, Batch 386/418),
`LyricsApi.getLyrics` (`suspend fun` + Retrofit, bukan `.execute()` mentah) — semua sudah
terbungkus dispatcher yang benar sejak batch-batch sebelumnya. `AppLogger.writePublicCrashLog()`
sengaja TETAP synchronous (dipanggil dari uncaught-exception-handler saat proses bisa mati kapan
saja — mendispatch ke thread lain di titik ini tidak aman) — bukan bug, tidak diubah.

**0 diverifikasi CI/device sesi ini** (tidak ada compiler/device fisik tersedia) — 2 fix di atas
murni review manual (baca kode + cross-reference pola batch sebelumnya + cek balance
brace/paren). Item belum-terverifikasi bertambah 2 (lihat daftar di bawah).

**Item belum-terverifikasi saat penutupan** (device fisik tidak pernah tersedia di sesi kerja):
- `MainActivity.kt` labelColor kontinu + easing post-release label (Batch 447, di atas) — 0
  compile log, 0 konfirmasi device. Perlu ditest: teks label berubah warna BERSAMAAN dgn ikon
  (bukan menyusul-lompat) selama drag pelan/parsial (belum commit index-crossing), dan
  scale/opacity label tidak snap instan pasca lepas jari (ikut easing 220ms spt ikon).
- `MainActivity.kt` fix label terpotong/pill oversized + drag-on-tab-bar baru (Batch 442, di
  atas) — 0 compile log, 0 konfirmasi device. Perlu ditest: label 3 tab tidak terpotong di
  ukuran default MAUPUN font-scale aksesibilitas besar, pill "Beranda" proporsional, 0 blur
  tersisa, dan drag jari lintas tab bar (termasuk lintas >1 batas tab dalam 1 drag) berpindah
  tab dgn benar tanpa mengganggu tap/ripple-feedback biasa.
- `MainActivity.kt` kapsul mengambang + pill gabungan ikon+label + ripple mati (Batch 439, di
  atas) — 0 compile log, 0 konfirmasi device. **[Update Batch 442]** sub-item overflow/oversized
  text SUDAH ditemukan+fix (root cause: `LocalTextStyle.current` salah baca style di slot
  `icon`, lihat Batch 442 di atas) — dihapus dari daftar perlu-test di sini, gantinya lihat item
  Batch 442 di atas. 2 sub-item SISA (belum tersentuh batch mana pun): kapsul bawah tidak
  ketutup gesture-nav bar di device asli (margin 12.dp bawah cukup?), dan 0 ripple sama sekali
  terasa saat tap ketiga tab di 5 identitas tema non-Skeu + Skeu.
- `MainActivity.kt` pill indicator glass ikon tab bawah (Batch 438, di atas) — 0 compile log, 0
  konfirmasi device. Perlu ditest: transisi cross-fade pill saat pindah tab (halus, bukan
  patah), kontras pill translucent tetap terbaca di 5 identitas non-Skeu (Apple/Tactile/Liquid
  Glass/Aurora/Calm Retro) x mode terang/gelap, scale-down `bouncyPress` saat tap terasa wajar
  (bukan berlebihan), dan pill Skeu tetap solid 100% (0 kebocoran efek glass ke identitas ini).
- `MainActivity.kt` efek kaca-pembesar label tab bawah (Batch 437, di atas) — 0 compile log, 0
  konfirmasi device. **[Update Batch 442]** komponen `.blur()` DICABUT (screenshot user
  konfirmasi 0 manfaat, cuma bikin buram permanen 2 label nonaktif) — item test blur DIHAPUS.
  Sisa perlu ditest (scale/opacity kontinu, TETAP jalan): drag pelan (fokus label bergeser mulus
  tab-ke-tab, bukan patah-patah), drag cepat lalu lepas sebelum threshold (springback fokus
  kembali ke tab asal mulus), drag di tab ujung (Beranda/Pengaturan, tidak crash walau tidak ada
  tab tujuan), dan 0 frame-drop/jank tambahan saat 3 label render bersamaan.
- `MainActivity.kt` swipe-lintas-3-tab (Batch 435, di atas) — 0 compile log, 0 konfirmasi device.
  Perlu ditest: swipe kiri/kanan di Beranda/Perpustakaan/Pengaturan (compact & rail/tablet),
  swipe di tab ujung (Beranda/Pengaturan) tidak nyasar/crash, swipe pendek (di bawah threshold)
  snapback mulus, dan gesture TIDAK kepicu sama sekali di `now_playing`/`stats_dashboard`.
- `ui/NowPlayingScreen.kt` `AlbumArtHero` (Batch 434, di atas) — 0 compile log, 0 konfirmasi
  device. Perlu ditest: swipe cepat berulang next/prev, springback di dragEnd/dragCancel, dan
  drag baru yang menyusul cepat sebelum springback lama selesai (skenario baru yang dijaga
  `dragOffset.stop()`).
- `ui/theme/IosScrollPhysics.kt` (Batch 433, di atas) — 0 compile log, 0 konfirmasi device. Perlu
  ditest: drag cepat berulang di list panjang (stutter hilang?), transisi antar layar, dan flow
  settle (lepas jari di tengah overscroll) tetap 0 regresi ke karakter pegas Batch 368-383 yang
  sudah disetujui user.
- `ui/DiagnosticLogSheet.kt` & `ui/DuplicateFinderSheet.kt` (Batch 431, di atas) — 0 compile log,
  0 konfirmasi device.
- `update/UpdateManager.kt` (Batch 432, di atas) — 0 compile log, 0 konfirmasi device (Thread →
  Coroutines migration, flow "Cek Update" perlu ditest ulang: check, download, install).
- `docs/archive/MANUAL_QA_CHECKLIST.md` — 0/19 item tercentang (audio focus, Bluetooth, lock-screen,
  headset kabel, process death, background playback jangka panjang).
- Overscroll bounce (`IosScrollPhysics.kt`, `Spring.DampingRatioNoBouncy`) belum dikonfirmasi
  device asli.
- `docs/archive/ROADMAP_LIQUID_GLASS_REDESIGN.md` & fling behavior 14/14 layar — **sudah final CLOSED**,
  bukan item terbuka.

### B. Entri `[RESUME POINT]` Batch 502 -> bawah (asli `PROJECT_STATE.md` v522 baris 2757-3148)

- Batch 502 (sebelum 503/504). ZIP: `SONIX_v502.zip`. **0 file kode diubah** (klarifikasi/
  sinkronisasi status murni) — user konfirmasi device-test Gap #6, TERMASUK poin paling kritis:
  lagu <30 detik yang sudah ada di playlist/favorit/queue TETAP muncul. **Gap #6 RESMI TUNTAS.**
  **[KOREKSI STALE — Batch 503]**: Baris resume point ini (versi sebelum koreksi) SALAH/BASI —
  mengklaim Gap #2 masih butuh keputusan bump media3 1.3.1→1.4.0+. **FAKTA dari source ZIP
  v502 + histori Batch 494-497 di atas (tidak dibaca ulang saat resume point ini terakhir
  ditulis)**: media3 SUDAH di-bump ke **1.10.1 sejak Batch 494** (instruksi eksplisit user),
  `.setMaxSeekToPreviousPositionMs(3000L)` SUDAH ter-pasang di `PlaybackService.kt` (player sesi
  utama, Batch 495) DAN bug lanjutannya (`seekToPreviousMediaItem()` vs `seekToPrevious()` di 3
  titik: app/widget/bubble) SUDAH DIPERBAIKI (Batch 496) serta **DIKONFIRMASI device fisik user:
  "it works heck yeah!!" (Batch 497)**. **Gap #2 RESMI TUNTAS sejak Batch 497 — 0 keputusan
  tersisa, 0 kode untuk diubah.** 0 file source disentuh batch ini (murni koreksi dokumentasi
  basi, pola sama sinkronisasi status Batch 451/473/492/500/502).
  **[RESUME POINT berikutnya]**: **0 gap actionable tersisa dari roadmap Gap QA v488** — Gap #1
  (Batch 489), #2 (Batch 497), #3 (Batch 500), #6 (Batch 502) RESMI TUNTAS; sisa #4/#5/#7/#8/#9
  murni device-QA/di luar scope kode, BELUM ada konfirmasi device eksplisit dari user untuk
  item-item tsb di sesi mana pun sejauh ini. Karena "device-QA lengkap" (syarat kedua utk
  archive) BELUM terpenuhi, `docs/QA_CHECKLIST_SONIX_v488.md` TETAP di lokasi aktif (BELUM
  dipindah ke `docs/archive/`) — JANGAN pindah tanpa konfirmasi eksplisit user per item
  #4/#5/#7/#8/#9, atau instruksi eksplisit user utk menutup sisanya sebagai "di luar scope,
  arsipkan saja".
- Batch 501 (sebelum 502). ZIP: `SONIX_v501.zip`. **1 file diubah** (dalam batas 3 file/tugas):
  `MusicRepository.kt` — Gap #6 (filter audio pendek) dieksekusi, ambang 30 detik
  (`MIN_SONG_DURATION_MS`) sesuai jawaban eksplisit user, HANYA di `getAllSongs()`
  (`ALL_SONGS_SELECTION`) — `getSongsByIds()`/`BASE_SELECTION` TETAP tidak disentuh. **DIKONFIRMASI
  Batch 502 di atas — device-test 0 masalah.**
- Batch 500 (sebelum 501). ZIP: `SONIX_v500.zip`. **0 file kode diubah** (klarifikasi/
  sinkronisasi status murni) — user konfirmasi CI Batch 499 hijau + device-test Gap #3 kritis
  (add/remove/reorder queue selagi shuffle) **0 crash**. **Gap #3 (shuffle anti-repeat-nearby)
  RESMI TUNTAS, 0 gap tersisa untuk fitur ini.**
- Batch 499 (sebelum 500, fix compile — lihat "Catatan Batch 499" di atas). ZIP:
  `SONIX_v499.zip`. **1 file diubah** (dalam batas 3 file/tugas): `AntiRepeatShuffleOrder.kt` —
  FIX CI compile fail dari `log_fail_478.zip` (`getNextIndex`/`getPreviousIndex` signature
  disamakan ke interface asli media3 1.10.1: 1 parameter, bukan 2). **DIKONFIRMASI Batch 500 di
  atas — CI hijau, device-test 0 crash.**
- Batch 498 (sebelum 499, GAGAL CI — lihat "Catatan Batch 499" di atas). ZIP: `SONIX_v498.zip`.
  **2 file disentuh** (dalam batas 3 file/tugas): file BARU `AntiRepeatShuffleOrder.kt` +
  `PlaybackService.kt` diubah (1 listener baru). **Gap #3 (shuffle anti-repeat-nearby) SEKARANG
  PUNYA IMPLEMENTASI KONKRET** — spesifikasi final & detail teknis: lihat "Catatan Batch 498" di
  atas + `CHANGELOG.md` § Batch 498. **WAJIB DITEST user: SUPERSEDED oleh Batch 499 di atas —
  install ZIP terbaru dulu (Batch 498 GAGAL compile, tidak pernah jadi APK), baru lanjut 5
  langkah test Gap #3.**
  **[RESUME POINT berikutnya]**: Sisa roadmap Gap QA v488, masih butuh keputusan/instruksi
  eksplisit user dulu (0 diasumsikan, 0 dieksekusi tanpa tanya):
  (a) **Gap #6 (filter audio pendek)** — filter `getAllSongs()` SAJA (`MusicRepository.kt`) dgn
  `DURATION > <ambang>`, `getSongsByIds()`/`BASE_SELECTION` TETAP tidak disentuh (lagu pendek yang
  SUDAH ada di playlist/favorit/queue tidak boleh mendadak hilang). **Ambang durasi BELUM
  dikonfirmasi user** (kandidat umum industri: 30 detik, TAPI ini asumsi, bukan angka eksplisit
  dari user/checklist — WAJIB tanya dulu sebelum eksekusi).
  (b) Gap #3 di atas: kalau hasil test user Batch 498 GAGAL di salah satu dari 5 langkah
  (terutama add/remove/reorder queue selagi shuffle aktif), root cause paling mungkin ada di
  `AntiRepeatShuffleOrder.cloneAndInsert`/`cloneAndRemove` — cek di situ dulu sebelum teori lain.
- Batch 492 (sebelum 493, GAGAL CI — lihat Batch 493 di atas). ZIP: `SONIX_v492.zip`. **1 file
  diubah**: `PlaybackService.kt` — Gap #2 Roadmap QA v488 (`setMaxSeekToPreviousPositionMs
  (3000L)` eksplisit di `ExoPlayer.Builder` player sesi utama). User konfirmasi device terpisah:
  checklist WAJIB DITEST Batch 491 (6 poin gabungan Batch 490+491, bug EQ balik flat/default) 100%
  LOLOS — saga bug EQ itu DITUTUP (valid, TIDAK terpengaruh revert Batch 493 — beda file/fitur).
  Detail lengkap Batch 492: "Catatan Batch 492" di atas.
  **WAJIB DITEST user: SUPERSEDED oleh Batch 493 (REVERT) — kode Previous-3-detik eksplisit
  SUDAH TIDAK ADA lagi di ZIP, jangan test poin ini, lihat Batch 493 di atas.**
- Batch 491 (sebelum 492). ZIP: `SONIX_v491.zip`. **2 file diubah** (dalam batas 3
  file/tugas): `PlayerViewModel.kt`, `PlaybackService.kt`. Laporan user (ULANG, identik gejala
  Batch 490 — Batch 490 TERBUKTI BELUM TUNTAS): "EQ aktif hanya saat tab dibuka, pasca app-kill
  balik flat/default". **Root cause KEDUA, TERKONFIRMASI dari pembacaan kode (baru ditemukan,
  BUKAN yang sudah dicatat Batch 490)**: `PlayerViewModel.onCleared()` — baris `equalizerController
  .release()` sudah ada SEBELUM Batch 490, tapi sejak Batch 490 mengubah `equalizerController`
  jadi SHARED per-process singleton (`EqualizerController.getInstance()`), baris ini keliru
  me-release INSTANCE BERSAMA itu, bukan cuma milik ViewModel ini lagi. `onCleared()` terpanggil
  begitu Activity benar-benar di-finish (bukan sekadar rotasi) — termasuk skenario UMUM app
  di-swipe dari Recents SELAGI `PlaybackService` sengaja TETAP hidup di background (lihat
  `onTaskRemoved`: sesi dgn antrean/paused tidak ikut mati). Efek Equalizer asli ikut lenyap dari
  sesi yang MASIH main, walau musik terus lanjut — flat sampai re-attach manual (buka tab
  Equalizer) atau `onEvents` lain kebetulan refire callback. Ini root cause TAMBAHAN yang Batch
  490 tidak cakup sama sekali (490 cuma menutup celah proses headless baru; bug ini soal siklus
  hidup UI vs siklus hidup sesi audio, kelas masalah berbeda).
  1. `PlayerViewModel.kt` (`onCleared()`): baris `equalizerController.release()` DIHAPUS. `audio
     VisualizerController.release()` (baris sesudahnya, TIDAK shared/singleton — grep konfirmasi
     0 `companion object getInstance()` di kelas itu, murni instance privat ViewModel) TETAP ada,
     0 disentuh.
  2. `PlaybackService.kt` (`onDestroy()`): `EqualizerController.getInstance(this).release()`
     ditambah — release yang benar sekarang diikat ke AKHIR SESI (Service benar-benar destroy),
     bukan akhir UI. 1 paket yang sama (`com.rudi.audioplayer.playback`), 0 import baru
     diperlukan. 0 baris lain di `onDestroy()`/`onTaskRemoved()` disentuh.
  **0 diverifikasi CI/device Batch 491** — 0 env Android nyata/compiler Kotlin sesi ini (balance
  brace/paren/bracket kedua file: `PlayerViewModel.kt` `{}` 248/248 `()` 1032/1032 `[]` 39/39;
  `PlaybackService.kt` `{}` 80/80 `()` 435/435 `[]` 19/19).
  **WAJIB DITEST user (gabung dgn 5 poin Batch 490 yang masih pending, BELUM pernah lolos)**:
  1. Set preset EQ non-flat (Now Playing → ⋮ → Equalizer) sampai jelas beda dari flat.
  2. **Skenario BARU khusus Batch 491** (app TIDAK di-force-close, cuma di-swipe dari Recents,
     proses TETAP hidup di notifikasi/lock-screen): swipe SONIX dari Recents (bukan Force Stop)
     selagi lagu masih main/paused dgn antrean tidak kosong → dengarkan LANJUT dari notifikasi/
     lock-screen/widget → preset EQ langkah 1 harus TETAP terasa (bukan tiba-tiba flat) TANPA
     perlu buka app lagi sama sekali.
  3. Baru buka app lagi dari launcher → sheet Equalizer masih tampil preset yang sama.
  4. Skenario Batch 490 (force-close TOTAL + trigger dari luar) — ulangi 5 poin lengkap di
     "Catatan Batch 490" di bawah, BELUM pernah dikonfirmasi lolos oleh user.
  5. Regression jalur normal: app dibuka biasa, ganti band/preset di sheet masih langsung
     terdengar & tersimpan lintas sesi seperti sebelumnya.
- Batch 490 (sebelum 491). ZIP: `SONIX_v490.zip`. **2 file diubah + 1 companion accessor
  baru** (dalam batas 3 file/tugas — lihat "Catatan Batch 490" di atas untuk rincian kenapa
  `EqualizerController.kt` dihitung terpisah): `AudioPlayerApplication.kt`, `PlayerViewModel.kt`,
  `EqualizerController.kt`. Laporan user: "preset EQ balik nol/default pasca app di-kill lalu
  musik dimainkan lewat eksternal SONIX player". Ringkas: root cause = re-attach hook Equalizer
  (Batch 314) hanya pernah diregistrasi dari `PlayerViewModel.init{}`, yang 0 pernah ada kalau
  proses dibangkitkan headless (widget/Bluetooth/media-button/notifikasi/Android Auto pasca
  app-kill) — `PlaybackService` tetap bikin sesi baru tapi 0 ada yang re-apply preset tersimpan ke
  situ. Fix: registrasi hook yang SAMA dari `AudioPlayerApplication.onCreate()` (jalan sebelum
  komponen apa pun, apa pun pemicu proses) + `EqualizerController.getInstance()` (shared SATU
  instance per proses, cegah 2 efek Equalizer nyata ke sesi yang sama saling menggandakan gain).
  **0 diverifikasi CI/device Batch 490** — 0 env Android nyata/compiler Kotlin sesi ini (balance
  brace/paren/bracket 3 file: lihat "Catatan Batch 490" di atas untuk angka lengkap).
  **WAJIB DITEST user** (5 poin lengkap di "Catatan Batch 490" di atas): set preset EQ non-flat →
  force-close TOTAL app → trigger play dari LUAR (widget/headset/Bluetooth/notifikasi/Android
  Auto) TANPA buka app dulu → preset harus terdengar SEJAK AWAL (bukan flat) → baru buka app →
  sheet Equalizer masih tampil preset yang sama, 0 suara dobel/makin kencang, DAN jalur normal
  (app dibuka biasa) tetap 0 regresi.
- Batch 489 (sebelum 490). ZIP: `SONIX_v489.zip`. **3 file disentuh** (1 fungsi ViewModel
  baru + 2 file wiring UI/Activity, dianggap 1 sektor/1 tugas — pola sama Batch 484): lihat
  "Catatan Batch 489" di atas untuk detail penuh. Ringkas: checklist QA eksternal
  (`QA_Checklist_SONIX_v488_terisi.md`) diverifikasi ulang ke source (9 poin "Verdict"-nya,
  bukan ditelan mentah), 1 gap dieksekusi (kontrol "Stop Pemutaran" eksplisit baru di sheet
  Kontrol Lanjutan Now Playing, `PlayerViewModel.stopPlayback()` = pause+seekTo(0), TIDAK
  menyentuh queue/state tersimpan — beda dari swipe-dismiss mini player yang "cancel total").
  Checklist asli disalin ke `docs/QA_CHECKLIST_SONIX_v488.md` (aktif, bukan arsip).
  **0 diverifikasi CI/device Batch 489** — 0 env Android nyata/compiler Kotlin sesi ini (balance
  brace/paren/bracket 3 file: lihat "Catatan Batch 489" di atas untuk angka lengkap).
  **WAJIB DITEST user sebelum lanjut roadmap gap QA lain** (daftar lengkap 4 poin di "Catatan
  Batch 489" di atas): Now Playing → ⋮ → "Kontrol Lanjutan" → seksi "Pemutaran" → baris baru
  "Stop Pemutaran" harus ada & berfungsi (jeda + posisi balik ke 0, lagu/antrean TETAP ada, tekan
  Play lagi lanjut dari awal lagu yang sama) — TANPA regresi ke swipe-dismiss mini player atau ke
  5 tombol transport utama.
  **[RESUME POINT berikutnya]**: 3 sisa gap dari checklist yang punya kandidat fix kode konkret,
  BELUM dikerjakan (lihat "Roadmap Gap QA v488" di "Catatan Batch 489" untuk detail per-poin):
  (a) previous-3-detik eksplisit (`PlaybackService.kt`, 1 baris, risiko rendah); (b) filter audio
  pendek (`MusicRepository.kt`, `getAllSongs()` saja — ambang durasi BELUM dikonfirmasi user,
  tanyakan dulu sebelum eksekusi); (c) shuffle anti-repeat-nearby (FITUR BARU, butuh instruksi
  eksplisit user dulu, bukan micro-task). Sisa gap checklist lainnya (#4/#5/#7/#8/#9) murni
  device-QA/di luar scope, 0 kode untuk dikerjakan.
- Batch 488 (sebelum 489). ZIP: `SONIX_v488.zip`. **2 file diubah** (1 file baru): `LibraryCacheStore.kt`
  (baru), `PlayerViewModel.kt` — FIX root cause laporan urgent user ("app tidak load berulang kali
  setiap aplikasi baru dibuka kembali pasca app di-kill") — lihat "Catatan Batch 488" di atas
  untuk detail penuh. Ringkas: snapshot library terakhir disimpan ke disk, dimuat instan (0
  shimmer) saat app dibuka lagi sambil scan asli tetap jalan senyap di background; tombol
  "Pindai Ulang"/pull-to-refresh manual TIDAK berubah (shimmer manual tetap tampil).
  **0 diverifikasi CI/device Batch 488** — 0 env Android nyata/compiler Kotlin di sesi itu.
  **WAJIB DITEST user** (6 poin lengkap di "Catatan Batch 488" di atas): buka app → tunggu
  library termuat → force-close total → buka lagi → library harus langsung tampil isi (0
  shimmer), diulang 2x + skenario tambah/hapus lagu saat force-close + pastikan tombol
  "Pindai Ulang"/pull-to-refresh manual TETAP menampilkan shimmer seperti biasa (0 regresi).
- Batch 480 (sebelum 488). ZIP terakhir: `SONIX_v480.zip`. **2 file diubah** (dalam batas 3
  file/tugas): `MiniPlayerBar.kt`, `PlayerViewModel.kt` — instruksi eksplisit user "sempurnakan
  mekanisme drag mini player (feedback, konfirmasi user, dll)", lihat "Catatan Batch 480" di atas
  untuk detail penuh. Ringkas: (1) feedback visual (alpha+scale mengikuti drag) + haptic 2-tahap
  (tick real-time di threshold, terpisah dari `LongPress` konfirmasi dismiss); (2) dismiss kini
  bisa di-"Urungkan" lewat Snackbar (infra `UndoableAction` yang sudah ada, `MainActivity.kt` 0
  disentuh). 0 threshold/formula/spring gesture yang sudah ada (120px, damping, dsb) disentuh.
  **0 diverifikasi CI/device Batch 480** — 0 env Android nyata/device fisik/compiler Kotlin di
  sesi ini (balance brace/paren/bracket: lihat "Catatan Batch 480" di atas untuk angka lengkap).
  **WAJIB DITEST user sebelum sektor mini player disentuh lagi** (daftar lengkap 5 poin di
  "Catatan Batch 480" di atas): swipe pelan → bar meredup+mengecil + tick getar di ~120px →
  lepas jari → bar hilang + Snackbar "Urungkan" muncul → tap "Urungkan" → lagu main lagi persis
  dari posisi semula (bukan dari awal). Kirim hasil test ini balik sebelum sektor mini player
  disentuh lagi.
- Batch 479 (sebelum 480). ZIP: `SONIX_v479.zip`. **1 file diubah** (dalam batas 3
  file/tugas): `MainActivity.kt` — FIX root cause bug (2) Batch 477 ("drag lintas tab, bottom nav
  diam"), lihat "Catatan Batch 479" di atas untuk detail penuh. Ringkas: `navPillIndexAnim`
  di-hoist ke scope `AppNavHost` + disinkronkan juga dari `onDragEnd` swipe KONTEN (sebelumnya
  cuma dari onClick tab & drag-langsung-di-tab-bar). Instrumentasi log Batch 478 DICABUT TOTAL.
  **0 diverifikasi CI/device Batch 479** — 0 env Android nyata/device fisik/compiler Kotlin di
  sesi ini (balance brace/paren/bracket file penuh: `{}` 339/339, `()` 1251/1251, `[]` 3/3).
  **WAJIB DITEST user sebelum sektor tab-swipe/bottom-nav dianggap tuntas**:
  1. Install ulang dari `SONIX_v479.zip` (via Termux DAILY UPDATE script).
  2. Di Beranda/Perpustakaan/Pengaturan: swipe horizontal di KONTEN (area kosong, bukan di atas
     tab-bar itu sendiri) → pill/label bottom nav HARUS ikut berpindah ke tab tujuan begitu
     gesture selesai (bukan cuma konten yang berganti seperti sebelumnya).
  3. Ulangi dengan drag kontinu (jari 0 terangkat) yang lintas >1 batas tab (mis. Beranda→
     Pengaturan lewat Perpustakaan) → pill harus ikut berpindah tiap batas terlewati, 0 macet.
  4. Pastikan 0 regresi: tap biasa di tab & drag LANGSUNG di atas tab-bar (Batch 442/444/448)
     masih berfungsi identik seperti sebelumnya.
  Kirim hasil test ini balik sebelum sektor tab-swipe/bottom-nav disentuh lagi.
- Batch 478 (sebelum 479). ZIP: `SONIX_v478.zip`. **1 file diubah**: `MainActivity.kt` —
  INSTRUMENTASI SAJA (0 fix logic, lihat "Catatan Batch 478" di atas) — data log dari langkah ini
  adalah yang mengungkap root cause Batch 479 di atas. Instrumentasinya sendiri sudah DICABUT
  TOTAL di Batch 479, jadi langkah WAJIB USER versi Batch 478 (reproduksi + kirim log) SUDAH
  SELESAI/TERPAKAI — jangan diulang.
- Batch 477 (sebelum 478). ZIP: `SONIX_v477.zip`. **2 file diubah** (dalam batas 3
  file/tugas): `MiniPlayerBar.kt`, `MainActivity.kt` — lihat "Catatan Batch 477" di atas. Ringkas:
  (1) swipe-cancel mini player Batch 476 diperbaiki (wasit gesture dipindah ke
  `PointerEventPass.Initial`, pola sama Batch 350); (2) drag-lintas-tab-konten Batch 435 diperbaiki
  (`pointerInput` key `currentRoute`→`Unit` + `rememberUpdatedState`, pola sama Batch 442).
  **0 diverifikasi CI/device sesi ini.**
  **WAJIB DITEST user sebelum lanjut fitur baru lain**:
  (a) swipe mini player kiri/kanan lewat threshold → bar hilang + musik BERHENTI TOTAL (regresi
      test Batch 476 (a)/(b)/(c) — WAJIB re-test, belum pernah dikonfirmasi user sama sekali);
  (b) tap biasa (0 geser) di mini player MASIH buka Now Playing + ripple normal — 0 regresi;
  (c) di Beranda/Perpustakaan/Pengaturan: swipe horizontal PELAN dimulai dari area KOSONG (bukan di
      atas row/carousel/chip manapun) → label tab tujuan harus MEMBESAR bertahap mengikuti jari
      (efek magnify, Batch 437) SELAMA drag, bukan cuma snap di akhir;
  (d) drag horizontal PANJANG dalam 1 gesture (jari 0 terangkat) yang lintas LEBIH dari 1 batas
      tab (mis. Beranda→Pengaturan lewat Perpustakaan) → magnify/nudge harus tetap hidup terus di
      SELURUH drag, bottom nav ikut berpindah tiap batas tab terlewati, 0 macet di tab kedua;
  (e) ULANGI (c)/(d) tapi mulai drag PERSIS DI ATAS row "Mix"/"Favorit" (Beranda) atau chip
      filter (Library) → kalau MASIH 0 efek di sini walau (c)/(d) sudah benar, LAPORKAN balik
      (lihat "CATATAN BELUM DIVERIFIKASI" Batch 477 di atas — teori sekunder LazyRow-menelan-
      drag, fix TERPISAH belum dikerjakan, prioritas sesi berikutnya kalau dikonfirmasi);
  (f) drag di ATAS bar tab itu sendiri (Batch 442, bukan konten) — pastikan 0 regresi, masih
      jalan seperti biasa (0 disentuh batch ini).
  Kirim hasil test ini balik sebelum sektor mini player/tab-navigation disentuh lagi.
- Batch 476 (sebelum 477). ZIP: `SONIX_v476.zip`. **3 file diubah** (dalam batas 3
  file/tugas): `PlayerViewModel.kt`, `MiniPlayerBar.kt`, `MainActivity.kt` — lihat "Catatan Batch
  476" di atas untuk detail penuh. Ringkas: (1) swipe mini player = cancel (stop total + queue
  kosong + saved state dikosongkan, bukan cuma sembunyikan bar); (2) `_uiState` mini player kini
  disinkronkan dari controller/saved-state begitu app dibuka kembali (baik Service masih hidup
  maupun ikut mati total), bukan reset kosong sampai transisi lagu berikutnya.
  **0 diverifikasi CI/device sesi ini** — 0 env Android nyata/compiler Kotlin/device fisik.
  **WAJIB DITEST user sebelum lanjut fitur baru lain**:
  (a) swipe mini player kiri ATAU kanan sampai lewat threshold → bar hilang, musik BERHENTI
      TOTAL (bukan cuma UI-nya hilang sementara musik tetap main di notifikasi/bubble);
  (b) swipe pelan/tidak sampai threshold → bar pegas balik ke posisi semula, TIDAK cancel,
      musik/tap-untuk-expand tetap normal;
  (c) tap biasa (tanpa geser) di mini player MASIH membuka Now Playing seperti biasa — 0 regresi
      ke `onExpand`, drag detector tidak boleh "mencuri" tap;
  (d) putar lagu, tekan tombol Home (bukan swipe-away Recents) supaya app background tapi proses
      TIDAK dibunuh OS, buka lagi app dari launcher → mini player harus tetap tampil benar
      (lagu/isPlaying/progress sama), TIDAK reset;
  (e) putar lagu dari WIDGET/headset/Bluetooth SAAT app dalam kondisi force-close total (App
      Info → Force Stop, atau swipe dari Recents lalu tunggu OS recycle), lalu BUKA APP dari
      launcher (bukan dari notifikasi) → mini player harus langsung sinkron menampilkan lagu yang
      sedang main itu (bukan kosong);
  (f) force-close total app SAAT ada lagu tersimpan (habis diputar lalu di-pause, proses lalu
      dibunuh total via App Info → Force Stop) → buka app dari launcher → mini player muncul
      PAUSED di lagu terakhir (siap lanjut), TANPA auto-play sendiri;
  (g) setelah (a) swipe-cancel, force-close total app lalu buka lagi dari launcher → mini player
      TIDAK boleh muncul lagi (lagu yang di-cancel tidak dibangkitkan oleh sync (e)/(f) di atas).
  Kirim hasil ketujuh test ini balik sebelum sektor mini player/cold-start disentuh lagi.
- Batch 475 (sebelum 476). ZIP: `SONIX_v475.zip`. **2 file diubah** (dalam batas 3
  file/tugas): `PlaybackService.kt`, `FloatingBubbleService.kt` — lihat "Catatan Batch 475" di
  atas untuk detail penuh. Ringkas: 2 titik `startForegroundService()` (bubble<->PlaybackService,
  arah cold-start-eksternal & tap-bubble-cold-start) disamakan ke pola `runCatching` yang sudah
  terbukti di `BubbleBootReceiver.kt`/`onTaskRemoved`. 0 behavior/logic lain diubah, murni
  error-handling. **Sulit direproduksi sengaja** (butuh device dgn restriksi OEM aktif tepat di
  momen resumption eksternal) — validasi dari user CUKUP: pastikan 0 regresi ke perilaku normal
  (bubble tetap nyala saat playback dipicu widget/headset/Bluetooth/lock-screen, tap tombol
  bubble cold-start tetap memicu restore seperti sebelumnya).
- Batch 474 (sebelum 475). ZIP: `SONIX_v474.zip`. **1 file diubah** (dalam batas 3
  file/tugas): `FloatingBubbleService.kt` — lihat "Catatan Batch 474" di atas untuk detail penuh.
  (a) Survive app-kill (Batch 471) **DITUTUP atas permintaan eksplisit user** — konfirmasi
  informal + user eksplisit minta topik ini TIDAK ditanyakan lagi. **JANGAN munculkan lagi
  permintaan test survive-Recents formal kecuali user sendiri buka ulang topiknya.** (b) FITUR
  BARU: `setupDrag()` digeneralisasi (param `onTap`, default identik lama, 0 breaking change ke
  pemanggil lama) lalu dipasang JUGA ke ke-4 tombol kontrol lewat `setupControls()` — drag yang
  dimulai di atas tombol sekarang ikut memindah bubble, tap biasa tetap jalan lewat
  `performClick()` ke listener asli yang tidak disentuh.
- **Status pertanyaan sempit Batch 472 (sisi kanan lega sampai tombol minimize)**: user TIDAK
  menjawab eksplisit — langsung minta fitur (b) di atas. **TETAP status "belum dikonfirmasi
  user"**, SOP larang asumsi — kalau area dead-space `bubble_album_art`→tombol MASIH terasa
  sempit di device fisik, itu kandidat regresi TERPISAH dari fitur (b) batch ini (fitur (b) HANYA
  menambah drag DI ATAS badan tombol itu sendiri, TIDAK mengubah `applyAlbumArtTouchDelegate()`
  Batch 472 sama sekali).
- **WAJIB DITEST user sebelum lanjut fitur baru lain**:
  (1) drag dimulai PERSIS di atas salah satu dari 4 tombol kontrol (coba tiap satu: play/pause,
      prev, next, minimize) sekarang ikut MEMINDAHKAN bubble — sebelumnya 0 efek sama sekali;
  (2) TAP biasa (tanpa gerak jari) di ke-4 tombol itu MASIH berfungsi 100% normal — play/pause
      toggle, skip prev/next, minimize ke tepi layar — 0 regresi fungsi;
  (3) drag dari album art (state expanded) & drag dari tab minimized (2 titik LAMA, TIDAK
      disentuh batch ini) tetap seperti Batch 472 — sisi kanan masih lega sampai tombol minimize,
      jangkauan gerak masih bebas penuh ke seluruh layar.
  Catatan yang TIDAK PERLU dilaporkan sebagai bug: bunyi klik sistem Android saat tap ke-4 tombol
  itu sengaja hilang (lihat "Catatan Batch 474" — konsisten filosofi iOS-look proyek, ripple
  Android juga sudah dimatikan di bottom nav Batch 439). Laporkan HANYA kalau ada regresi FUNGSI.
- Batch 473 (sebelum 474). ZIP: `SONIX_v473.zip`. **0 file kode diubah** (klarifikasi murni) —
  poin 3 Batch 472 ("bubble tetap muncul saat player eksternal dimainkan") RESMI DITUTUP:
  dikonfirmasi PERILAKU YANG DIHARAPKAN (bubble ikut trigger play dari luar UI app —
  headset/Bluetooth/Android Auto/widget), BUKAN bug lintas-app.
- Batch 470 (sebelum 471). ZIP: `SONIX_v470.zip`. **1 file diubah** (dalam batas 3
  file/tugas): `FloatingBubbleService.kt`. 2 fitur baru dari user (lihat "Catatan Batch 470" di
  atas untuk detail): (1) cold-start fix `sendPlaybackAction`/`setupControls` (tap kontrol bubble
  segera setelah reboot, sebelum app/widget pernah dibuka, sekarang tetap bisa trigger restore
  antrean — dulu jatuh ke `openApp()` selamanya begitu controller lokal konek duluan dgn antrean
  kosong); (2) `setupDrag` discoped ke `bubble_album_art` saja untuk state expanded (minimized
  tab 0 berubah) + `TouchDelegate` biar area sentuh tetap luas TANPA ubah layout/dp XML. **0
  pernah menyentuh [screenBounds]/formula clamp/posisi** — 0 tumpang tindih dgn investigasi
  Batch 469 di bawah, yang MASIH terbuka/belum resmi selesai (user cuma konfirmasi kliping
  landscape UMUM, BUKAN protokol reproduksi spesifik Batch 469).
- **WAJIB dari user sebelum lanjut fitur baru lain**: (a) test cold-start — reboot HP, JANGAN
  buka app/widget sama sekali, langsung tap play/prev/next di bubble → harus mulai memutar
  (bukan cuma buka app); (b) test touch-scope — drag mulai dari padding kosong pill (BUKAN album
  art/tombol) harus 0 efek; drag mulai dari album art (termasuk sedikit meleset di luar 40dp-nya)
  harus tetap jalan seperti biasa, jangkauan gerak tetap bebas penuh ke seluruh layar seperti
  sebelumnya. Kirim hasil kedua test ini balik.
- **ITEM WAJIB PALING PRIORITAS begitu regresi Batch 469 muncul lagi** (belum terjadi/dilaporkan
  ulang sesi ini, tapi statusnya BELUM ditutup — lihat "Catatan Batch 469" & "Catatan Batch 470"):
  terima log dari user — WAJIB direproduksi PERSIS (landscape, drag ke tepi nav-bottom sampai
  hilang, **JANGAN rotasi balik ke portrait dulu**, baru ekspor Log Diagnostik) — baca
  `Batch469 bounds-compare` & `Batch469 drag readback` SEBELUM coding fix apa pun. **JANGAN
  tebak fix ke-5** tanpa data ini (pola terlarang eksplisit, lihat KDoc kelas "PELAJARAN PROSES
  Batch 461→462").
- Batch 469 (sebelum 470). ZIP: `SONIX_v469.zip`. **1 file diubah** (dalam batas 3
  file/tugas): `FloatingBubbleService.kt` — **0 formula/clamp/posisi diubah**, murni 2 log baru
  (bounds-compare `onConfigurationChanged` + drag-release/readback `ACTION_UP`). Respons ke
  REGRESI BARU: bubble hilang total saat drag ke tepi landscape berbeda (nav-bottom), balik
  normal cuma kalau rotasi ke portrait. **ITEM WAJIB PALING PRIORITAS sesi berikutnya**: terima
  log dari user — WAJIB direproduksi PERSIS (landscape, drag ke tepi nav-bottom sampai hilang,
  **JANGAN rotasi balik dulu**, baru ekspor Log Diagnostik) — baca `Batch469 bounds-compare` &
  `Batch469 drag readback` SEBELUM coding fix apa pun. **JANGAN tebak fix ke-5** tanpa data ini
  (pola terlarang eksplisit, lihat KDoc kelas "PELAJARAN PROSES Batch 461→462").
- Kalau log BUKTIKAN `newConfigBounds` ≠ `currentWindowMetricsBounds` secara signifikan (bukan
  cuma rounding 1-2px): itu konfirmasi teori Batch 469 — fix kandidat: samakan SATU sumber
  [screenBounds] yang dipakai KONSISTEN di `onCreate` DAN `onConfigurationChanged` (bukan 2 API
  beda), TAPI harus tetap jaga freshness/timing yang jadi alasan Batch 461 pindah ke `newConfig`
  — cek dulu ke user/dokumentasi apakah `currentWindowMetrics` di titik NON-rotasi-transisi
  (mis. di drag ACTION_UP yang sudah pasti settle) aman dipakai FRESH tanpa masalah staleness
  yang sama seperti saat [onConfigurationChanged] (2 konteks timing yang beda, jangan disamakan
  otomatis).
- Kalau log tunjukkan readback drag KELUAR dari `screenBounds` yang dipakai clamp itu sendiri
  (bukan soal 2-API, tapi APLIKASI window yang salah render): itu petunjuk arah lain sama sekali
  — WAJIB baca ulang [addBubbleView]/params.apply sebelum simpulkan apa pun.
- Status fix Batch 468 (`FLAG_LAYOUT_IN_SCREEN`) & item-item verifikasinya (portrait gap,
  landscape-edge-clip, delta readback (0,0)) MASIH BELUM ada laporan device terpisah dari user —
  regresi Batch 469 ini TIDAK otomatis berarti Batch 468 gagal total, cuma menunjukkan ADA
  skenario tambahan (drag manual ke edge landscape tertentu) yang belum ter-cover. Jangan
  simpulkan revert sampai data log ini di tangan.
- Fix crash Batch 466 (`BubbleBootReceiver.kt`) masih BELUM ada konfirmasi eksplisit "0
  force-close lagi setelah reboot" dari user — tetap dianggap valid berdasarkan bukti log,
  tunggu laporan device kalau ada kesempatan (bukan blocker urutan, bisa paralel dgn cek bubble).
- Batch 464 (sebelum 465). ZIP: `SONIX_v464.zip`. **1 file diubah** (dalam batas 3
  file/tugas): `FloatingBubbleService.kt` — **masih instrumentasi, 0 fix formula/logic**. Log
  Batch 463 pertama dari user (device fisik) kasih DATA MENGEJUTKAN: mismatch besar (target
  x=1011 vs nyata x=551, selisih 460px) terjadi saat `minimize()` BIASA — **0 rotasi terlibat**.
  Ini menggeser dugaan dari "animasi transisi rotasi" (fokus Batch 462) ke "window `WRAP_CONTENT`
  belum tuntas resize saat kita baca `width`/apply posisi" — teori BARU, belum pernah diuji. Batch
  464 memperluas readback: (1) log ukuran nyata (`container.width/height`) juga, dibanding ukuran
  saat target dihitung; (2) tambah readback KEDUA di +800ms (selain +250ms) untuk bedakan
  "settling sementara" vs "salah permanen". Detail penuh: "Catatan Batch 464" di atas &
  `CHANGELOG.md`. Sektor bubble (Roadmap #11) masih terbuka, TIDAK ada sektor DITUTUP disentuh.
- **WAJIB DILAKUKAN sebelum lanjut fix apa pun ke file ini (PALING PRIORITAS)**: minta user (1)
  buka bubble → minimize → (boleh) rotasi lagi kalau mau; (2) **diamkan HP minimal 1 detik penuh**
  sebelum ambil log (readback terjauh sekarang +800ms, bukan +250ms lagi); (3) ambil logcat ATAU
  ekspor Settings → Lanjutan → Log Diagnostik; (4) kirim balik. **JANGAN eksekusi fix apa pun ke
  formula/logic sebelum perbandingan 250ms-vs-800ms & ukuran-nyata-vs-target ada di tangan** —
  2 hasil berbeda (lihat "Catatan Batch 464") mengarah ke 2 kelas fix yang sama sekali berbeda.
- **HISTORI KEGAGALAN item (3) — WAJIB dibaca sebelum lanjut**: Batch 460
  (`resources.displayMetrics`→`currentWindowMetrics.bounds`) ❌. Batch 461 (`screenBounds` dari
  `newConfig`) ❌ "100% kelihatan, gak keclip sama sekali". Batch 462 (safety-net re-assert 2x
  delay) ❌ GAGAL LAGI. Batch 463 (pivot instrumentasi, logcat lama TERBUKTI 0 sinyal) → readback
  pertama JUSTRU dari event minimize BIASA (bukan rotasi), mismatch 460px. Batch 464 (perluas
  readback: ukuran nyata + titik +800ms) — hasil BELUM diketahui. **BACA logika keputusan
  berikut begitu log Batch 464 masuk**: (a) kalau `container.width/height` di readback BEDA dari
  target width/height → window memang masih resize saat snap di-apply, fix arahnya: tunda
  `updateViewLayout` sampai window BENAR tuntas resize (bukan cuma `container.post{}` 1x), atau
  hitung target dari ukuran yang SUDAH pasti final (mis. dimensi XML tab minimized langsung,
  bukan `container.width` runtime); (b) kalau ukuran SAMA tapi posisi +250ms meleset namun +800ms
  SUDAH benar → murni soal delay re-assert kurang lama, cukup naikkan
  `ROTATION_RESNAP_DELAYS_MS`/tambah 1 titik lagi; (c) kalau +800ms MASIH meleset SAMA dengan
  ukuran yang SUDAH match target → root cause lain sama sekali (bukan resize, bukan timing) —
  WAJIB investigasi baru, jangan asumsikan salah satu dari (a)/(b) tanpa cek datanya.
- **DIKONFIRMASI device fisik user (Batch 460)**: (1) mini trigger lebih gampang di-tap ✅; (2)
  bagian timbul ~10% ✅; (4) drag tab minimized 100% kelihatan/terkontrol penuh selagi digeser ✅;
  (5) 0 regresi ke minimize/expand/fade/auto-minimize Batch 98-100/453/454 ✅. **(3) mentok tepi
  konsisten landscape ❌ GAGAL 3x berturut-turut (Batch 460, 461, 462)** — lihat "HISTORI
  KEGAGALAN" di atas. Batch 463 TIDAK mencoba fix ke-4, murni instrumentasi.
- **DIKONFIRMASI device fisik user (Batch 458, masih berlaku sebagai baseline utk item 2/4/5)**:
  (1) tab minimized ~30% kelihatan/~70% tersembunyi — nilai ini SUDAH DIGANTIKAN ~10%/~90% oleh
  Batch 460 (dikonfirmasi ✅ di atas); (2) mini trigger tetap bisa di-tap, TAPI "sedikit lebih
  susah" — residual ini DIKONFIRMASI FIX di Batch 460 (✅ di atas); (3) drag tab minimized 100%
  kelihatan/terkontrol penuh selagi digeser — confirmed (baseline utk item 4 Batch 460 di atas);
  (4) 0 regresi ke minimize/expand/fade/auto-minimize Batch 98-100/453/454 — confirmed.
- **CATATAN proses (berlaku terus)**: istilah user "timbul" = bagian KELIHATAN tab, bukan fraksi
  klip (`EDGE_CLIP_FRACTION`) itu sendiri — dua hal berlawanan arah. Kalau user minta angka
  persentase lagi tanpa kata eksplisit "sembunyi/klip" vs "timbul/kelihatan", WAJIB klarifikasi
  arah dulu (pola Batch 456→457→458), jangan tebak. Sejak Batch 460, "timbul"/`EDGE_CLIP_FRACTION`
  TIDAK LAGI otomatis mengontrol lebar area sentuh (dipisah via `touchPad`) — permintaan "perkecil
  timbul" ke depan AMAN dieksekusi tanpa risiko balik memperkecil touch target. **PELAJARAN PROSES
  Batch 461→462→463 (WAJIB diikuti)**: JANGAN ulangi pola "ganti API/sumber baca metrics lagi"
  (sudah 3x terbukti bukan akar masalah, Batch 460/461/462). Minta logcat generik SAJA juga
  TERBUKTI TIDAK CUKUP (Batch 462→463: user bawa logcat, tapi 0 baris app-level karena service
  tidak pernah logcat) — WAJIB ada instrumentasi eksplisit DULU di kode (sudah ditambah Batch 463)
  SEBELUM logcat/Log Diagnostik device asli benar-benar berguna. Kalau residual item (3) muncul
  LAGI setelah log Batch 463 masuk dan dibaca: putuskan fix ke-4 dari perbandingan snap-target vs
  readback +250ms di log itu (lihat "HISTORI KEGAGALAN" di atas), bukan teori baru tanpa bukti.
- **BELUM dikonfirmasi (Batch 452, masih berlaku)**: (1) label 3 tab 0 lagi ellipsis di font
  normal; (2) drag flick cepat + tap-tab biasa berhenti TEPAT di tab tujuan, 0 "mundur 1 kolom".
- **DIKONFIRMASI device fisik user (Batch 451, masih berlaku)**: (1) pill ukuran normal, 0 kapsul
  raksasa (fix Batch 450); (2) 0 kilatan kotak abu-abu di tab ditinggalkan (fix Batch 449).
- **BELUM dikonfirmasi (lama)**: regresi warna ikon/label tema Skeu (video user Batch 451 pakai
  tema default/gelap, bukan Skeu). TalkBack tidak bisa dicek dari rekaman visual.
- Mandat lain: 0 ada. Sektor bottom nav (Batch 448-452) dianggap aktif-stabil kecuali user
  laporkan temuan baru. Lanjutkan sektor manapun yang diminta user berikutnya (0 sektor DITUTUP
  baru dibuka permanen, 0 sektor baru ditutup juga).
- **PELAJARAN PROSES Batch 450 TETAP berlaku** (lihat komentar kode di `CustomNavBarTabItem`):
  modifier layout yang meniru API resmi WAJIB diverifikasi ke source/dokumentasi asli dulu,
  jangan diasumsikan.

---

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

**Batch 57 (toggle tema custom baru: Skeuomorphism Dark Lite)** — User minta tema custom ketiga
(kedua di luar keluarga Apple/Light/Dark/System), tanpa spec eksternal — palet charcoal netral
hangat + panel timbul + aksen tembaga (`#CB8B4B`), sengaja dibedakan dari Tactile (AMOLED-glass,
hue biru). Detail lengkap di `CHANGELOG.md` Batch 57; ringkas:
- `AppTheme.SKEU_DARK_LITE` baru (`Theme.kt`) + token warna (`Color.kt`) + `SkeuDarkColors`/
  `SkeuDarkShapes`/`isSkeuTheme()` (`Theme.kt`) + `skeuEmboss()` (`TactileDepth.kt`, refactor
  `tactileEmboss()` jadi wrapper `embossSurface()` privat bersama — signature/perilaku publik
  `tactileEmboss()` tidak berubah) + `frostedGlass()` 3-arah (`BlurUtils.kt`) + pratinjau hidup di
  `SettingsScreen.kt` + catch-light NavigationBar digeneralisasi (`MainActivity.kt`).
- `ThemeStore.kt`/`AppTheme.fromStorageKey()`/`SettingsScreen.kt`'s `AppTheme.entries.toList()`
  loop TIDAK disentuh — sudah generic sejak awal, toggle baru otomatis muncul & tersimpan.
- **Belum diverifikasi visual/compile** (sama seperti setiap batch tema sebelumnya, tidak ada
  `kotlinc`/emulator di sini) — grep exhaustiveness check atas semua `when (AppTheme)` di codebase
  sudah dilakukan (3 titik, semua sudah mencakup entry baru), brace/paren balance semua 6 file
  yang disentuh seimbang.
- **Sengaja TIDAK dikerjakan** (sama presedan Tactile Batch 45-48→55): `skeuEmboss()` belum
  dipasang ke kontrol individual (play/pause, slider) — baru dipakai 1 tempat (baris pemilih
  tema). Root ambient gradient khusus Skeu — disengaja flat (bukan gap, keputusan desain: identitas
  Skeu adalah panel solid, bukan lapisan kaca). Tipografi custom Skeu — reuse `AppleTypography`.

**Batch 56 (versionCode/versionName reset — bukan tema/fitur)** — User minta reset angka versi
app karena `1.0.<total commit history>` sudah kelihatan besar/tidak sedap dipandang di picker.
`gitCommitCount()` (`app/build.gradle.kts`) & CI (`.github/workflows/build.yml` "Determine version
name") diubah dari `git rev-list --count HEAD` (total history) ke `git rev-list --count
v-reset..HEAD` dengan fallback ke `HEAD` kalau tag belum ada — jadi angkanya restart dari kecil
TANPA rewrite/squash git history (log/blame lama tetap utuh). **Wajib 1x setup manual di Termux
setelah ZIP ini di-push** (belum dijalankan otomatis dari sini karena butuh akses git remote user):
```
git tag v-reset && git push origin v-reset
```
Tanpa tag ini, kedua sisi (gradle & CI) otomatis fallback ke hitungan lama (tidak breaking,
cuma belum ke-reset). 2 file protected disentuh (`app/build.gradle.kts`,
`.github/workflows/build.yml`) — edit parsial saja (fungsi diganti, sisa file & signing config
tidak disentuh).

**Batch 55 (Tactile identity polish, atomic change)** — User minta polish tema custom Tactile
biar perbedaannya sama tema utama (Apple) makin kelihatan. Audit codebase: warna/tipografi/shape/
kaca sudah dibedakan sejak Batch 49-54 (lihat entri Batch 53 di bawah), tapi tombol play/pause —
kontrol paling sering dilihat sepanjang sesi dengar musik (mini bar + Now Playing) — masih render
byte-identik di kedua tema (`FilledIconButton` circle default M3, tanpa bevel apa pun). Itu satu
titik terbesar yang bikin identitas Tactile "hilang" begitu musik diputar. Detail lengkap di
`CHANGELOG.md` Batch 55; ringkas:
- Tombol play/pause utama (`NowPlayingScreen.kt`, 68dp) & mini player (`MiniPlayerBar.kt`, 40dp):
  Tactile sekarang `MaterialTheme.shapes.medium` (rounded-square) + `tactileEmboss()`, Apple tetap
  `CircleShape` + shadow biasa seperti sebelumnya — tidak berubah.
- `AlbumArtHero`'s border: `Brush.verticalGradient` (peninggalan sebelum aturan diagonal spec §9
  Batch 53) diganti `Brush.linearGradient` — satu-satunya border Tactile yang belum ikut arah
  cahaya diagonal top-left→bottom-right yang dipakai di tempat lain.
- **Belum diverifikasi visual/compile** — sama seperti setiap batch tema sebelumnya (tidak ada
  `kotlinc`/emulator di environment ini), diverifikasi lewat baca-manual + brace/paren balance
  check (seimbang di kedua file yang disentuh).
- **Sengaja TIDAK dikerjakan**: custom thumb/track untuk kedua `Slider` (seek bar utama & volume
  dalam-aplikasi) — masih M3 default identik di kedua tema. Butuh slot `thumb`/`track` composable
  (custom draw, bukan sekadar modifier tempel), risiko lebih tinggi tanpa compiler — kandidat
  batch polish berikutnya.

**Batch 54 (technical debt pass, bukan tema/fitur)** — User minta gabungkan seluruh daftar
technical debt murni-kode (hasil audit statis: grep + baca file, bukan dari testing) dengan
technical debt yang sudah tercatat di segmen "Belum selesai / dalam pengerjaan" README.md, jadi
1 batch atomic change. Dikerjakan (pakai batch-limit exception "Atomic Change" — 10 file
tersentuh, lebih dari limit normal 10 file/1 modul, tapi ini satu perubahan logis yang saling
terkait, bukan beberapa fitur independen digabung paksa):
- **`isTactileTheme()` helper baru** di `Theme.kt` — mengganti 6 duplikat manual
  `MaterialTheme.colorScheme.background == TactileBackground` (di `BlurUtils.kt`, `HomeScreen.kt`,
  `LibraryScreen.kt`, `MiniPlayerBar.kt`, `NowPlayingScreen.kt` x2) jadi satu pemanggilan fungsi.
  Perilaku identik, cuma DRY.
- **11 inline fully-qualified reference** (`com.rudi.audioplayer.ui.theme.X` ditulis langsung di
  tengah kode alih-alih lewat `import`) dibersihkan jadi import biasa — 5 file (`MiniPlayerBar.kt`,
  `LibraryScreen.kt`, `HomeScreen.kt`, `NowPlayingScreen.kt`, `MainActivity.kt`). Sudah dicek grep:
  tidak ada FQN inline tersisa di codebase (import legit seperti `import
  com.rudi.audioplayer.ui.theme.frostedGlass` tidak disentuh, itu memang sudah bentuk yang benar).
- **5 token warna dead code dihapus** dari `Color.kt`: `TactileControl`, `TactileControlPressed`,
  `GlassPressed`, `GlassWhite`, `TactileMutedText` — 0 call site (grep-confirmed sebelum dihapus).
  Disiapkan Batch 53 untuk komponen `TactileButton`/`TactileSwitch`/`TactileSlider` yang belum
  pernah dibangun; ditinggalkan komentar penjelas supaya batch masa depan yang benar-benar
  membangun komponen itu tahu kenapa tokennya hilang dan tinggal re-derive dari spec saat itu.
- **`Spacing.kt` (file baru)** — token `Radius` (xs/sm/md/ml/lg/xl/xxl/xxxl/hero, literal
  4/10/12/14/16/18/20/24/28dp) sebagai fondasi sistem spacing/shape terpusat (spec §19). Semua 32
  titik `RoundedCornerShape(N.dp)` literal di seluruh codebase (8 file: `FeatureHintBanner.kt`,
  `HomeScreen.kt`, `LibraryScreen.kt`, `MiniPlayerBar.kt`, `NowPlayingScreen.kt`,
  `SettingsScreen.kt`, `Theme.kt`) dimigrasi ke token ini via script Python (regex match-and-
  replace per file, bukan manual satu-satu) — sudah diverifikasi grep nihil literal
  `RoundedCornerShape(N.dp)` tersisa, dan brace/paren count tiap file yang disentuh seimbang
  sebelum/sesudah (proxy sanity-check tanpa compiler).
- **Sengaja TIDAK dikerjakan batch ini** (didaftar transparan, bukan disembunyikan):
  - **Migrasi penuh ~340 literal `.dp` non-radius sisanya** (padding/size/offset/blur radius) ke
    `Spacing.kt` — beda dengan corner-radius yang punya segelintir nilai berulang jelas, mayoritas
    literal ini one-off/context-specific (misal shadow offset `9.dp` di hero art `NowPlayingScreen`
    yang memang di-tuning presisi untuk 1 tempat). Memaksakan token di sini berisiko kehilangan
    presisi yang disengaja, dan sweep sebesar itu tanpa `kotlinc` untuk verifikasi adalah risiko
    nyata — alasan yang sama persis yang sudah dipakai proyek ini sendiri di Batch 31/35.
  - **Ekstraksi penuh 339 string literal ke `strings.xml`** (untuk i18n) — sudah didaftar duluan
    di README.md "Belum selesai / dalam pengerjaan" dengan alasan identik (refactor mekanis
    sebesar itu, ratusan titik tersebar di banyak file, tidak aman tanpa compiler untuk verifikasi
    argumen format-string/urutan parameter tetap benar). Tidak diulang batch ini.
  - **Pull-to-refresh gesture di Library** & **Shared-element transition sungguhan** (mini player
    → Now Playing) — dua-duanya butuh bump Compose BOM dari 2024.05.00, yang berisiko ke komponen
    lain yang sudah stabil jalan. Ini bukan technical debt murni kode (butuh keputusan
    dependency-version terpisah), jadi tidak dipaksakan masuk batch "atomic" ini.
- **Belum diverifikasi build/compile** — sama seperti setiap batch sebelumnya, environment kerja
  ini tidak punya `kotlinc`. Verifikasi dilakukan lewat grep menyeluruh (nihil sisa referensi lama)
  + brace/paren balance check per file, bukan compile sungguhan.

**Batch 53** — User kirim spec baru `compose-amoled-hybrid-glass-final.md`, minta diterapkan
100% ke tema custom Tactile (menggantikan palet flat Midnight Blue literal Batch 52 sepenuhnya —
spec ini secara eksplisit mendaftar "a full Midnight Blue theme" sebagai anti-pattern di §24, jadi
Batch 52 sendiri sekarang jadi contoh yang harus dihindari). Diterapkan ke SEMUA sektor yang
langsung terlihat user (bukan cuma 6 pilar/screen utama): 4 file token/util pusat
(`Color.kt`/`Theme.kt`/`BlurUtils.kt`/`TactileDepth.kt`) + `MainActivity.kt` (root ambient
gradient + navbar). Karena setiap screen/sheet (Home, Library, MiniPlayer, NowPlaying, Settings,
semua bottom sheet) sudah merutekan visualnya lewat `frostedGlass()`/`tactileEmboss()`/
`MaterialTheme.colorScheme` alih-alih warna literal per-file, rewrite terpusat ini otomatis
menjangkau semuanya tanpa perlu menyentuh tiap file screen satu-satu (dikonfirmasi lewat grep:
15 file `ui/*.kt` semua konsumsi salah satu dari ketiga titik pusat itu).
- `Color.kt` — repaint total mengikuti hierarki spec §2 (AMOLED > glass > Midnight Blue ambient >
  tactile > accent): `TactileBackground` sekarang AMOLED near-black §3 literal 0xFF030508 (bukan
  flat Midnight Blue 0xFF191970 lagi), `TactileSurface`/`TactileSurfaceVariant` sekarang §5
  `GlassBase`/`GlassElevated` literal 0xFF0A0F16/0xFF101722, `TactileText`/`TactileSecondaryText`
  ganti ke §16 `TextPrimary`/`TextSecondary` 0xFFEAF0F8/0xFFAAB5C4, `TactileAccent` sekarang §17
  `AccentBlue` 0xFF6670FF. Token baru: `AmoledSurface`, `GlassPressed`, `GlassWhite`,
  `TactileMutedText`, `MidnightBlue` (0xFF191970 — sekarang HANYA ambient, tidak lagi jadi
  background), `MidnightBlueAmbientAlpha` (0.06f). `TactileHighlight`/`TactileEdge` naik dari
  0.055f/0.035f ke §5 literal `GlassHighlight`/`GlassBorder` 0.065f/0.035f. `TactileError`/
  `TactileSuccess` tidak berubah (spec ini juga tidak beri token error/success literal, sama
  seperti setiap batch tema custom sebelumnya).
- `Theme.kt` — `TactileColors` re-wire ke token baru (fungsinya sama, cuma nilai berubah lewat
  Color.kt); `AppTheme.TACTILE.description` diperbarui dari deskripsi "Midnight Blue taktil" ke
  deskripsi glass-first yang sesuai identitas baru. `onPrimary` tetap `Color.White` (AccentBlue
  0xFF6670FF luma ≈0.49, masih di bawah threshold 0.55).
- `BlurUtils.kt` — `frostedGlass()`'s `edgeBrush` untuk Tactile diganti dari solid
  `primary.copy(alpha=0.22f)` ("accent trim line", melanggar §8 "border harus subtle, bukan
  accent") ke `Brush.linearGradient(TactileHighlight, TactileEdge)` — diagonal top-left→bottom-
  right sesuai §9 "Lighting model", bukan lagi warna aksen solid di sekeliling setiap panel glass.
- `TactileDepth.kt` — `tactileEmboss()`'s bevel gradient ganti dari `verticalGradient` ke
  `linearGradient` (default Offset.Zero→Offset.Infinite = diagonal top-left→bottom-right, sesuai
  §9) memakai token `TactileSurfaceVariant`/`TactileSurface` yang sekarang sudah berarti glass
  bukan bevel opaque. Border/shadow alpha di-re-tune mengikuti base token baru (0.065f/0.70f).
- `MainActivity.kt` — root `Surface` untuk Tactile sekarang `Color.Transparent` + `Box.background`
  dengan `Brush.linearGradient(background, MidnightBlue.copy(alpha=MidnightBlueAmbientAlpha),
  AmoledSurface)` — ini SATU-SATUNYA tempat `MidnightBlue` dipakai sebagai warna nyata di layar
  (§6 "Correct use": hanya sebagai ambient gradient, bukan flat surface), tema lain tidak berubah
  (tetap flat `colorScheme.background`). NavigationBar `tonalElevation` Tactile diturunkan dari
  12.dp ke 6.dp — pada 12.dp, `surfaceTint` (=TactileAccent, biru) membuat seluruh nav bar
  kelihatan biru dulu sebelum "AMOLED glass" (melanggar §2 "Golden Rule" dan §15), 6.dp tetap
  kelihatan terangkat (Level 2 glass) tanpa wash aksen mendominasi.
- **Belum diverifikasi visual** (sama seperti setiap batch tema custom sebelumnya, environment
  kerja ini tidak punya `kotlinc`/emulator): build-test asli + verifikasi visual device disarankan
  sebelum rilis, khususnya diagonal border baru (`Brush.linearGradient` di `BlurUtils.kt`/
  `TactileDepth.kt`) dan root ambient gradient (`MainActivity.kt`) — grep sudah dicek nihil
  referensi token lama yang terlewat, tapi belum diverifikasi visual.

**Batch 52** — User kirim spec baru `compose-skeuomorphism-lite-midnight-blue.md`, minta
diterapkan 100% ke tema custom Tactile (menggantikan palet hybrid-glass Batch 51 sepenuhnya —
spec header eksplisit 2x pakai kata "Literal" dan "Mandatory visual baseline: Literal Midnight
Blue (#191970) — MANDATORY", jadi §2 dipakai sebagai nilai literal langsung, sama seperti setiap
batch tema custom sebelumnya). 6 file kode disentuh, 1 tema kohesif (atomic) — **kali ini justru
menghapus 2 fitur** (gradient root & glass overlay) karena spec baru tidak lagi memintanya, bukan
menambah fitur baru seperti Batch 51.
- `Color.kt` — seluruh palet Tactile diganti dari §2 spec literal: `TactileBackground` 0xFF191970
  (flat, satu stop — bukan pasangan gradient lagi), `TactileSurface` 0xFF161665 dan
  `TactileSurfaceVariant` 0xFF20207A (**keduanya opaque 0xFF lagi**, beda fundamental dari Batch
  51 yang translusen 0xCC/0xB8), `TactileText`/`TactileSecondaryText` ganti ke 0xFFF0F1FF/
  0xFFBFC2E6, `TactileAccent` 0xFF7278FF (ganti dari 0xFF5B9DFF Batch 51 — lebih ungu-biru,
  lebih gelap). `TactileHighlight`/`TactileEdge`/`TactileShadow` balik ke basis `Color.White`/
  `Color.Black` polos ber-alpha rendah (0.055f/0.035f/0.65f, literal §2) — bukan lagi warna
  ber-tint sendiri seperti Batch 51. `TactileBackgroundTop` dan `TactileGlassOverlay` (dua token
  BARU Batch 51) **dihapus** — spec ini tidak punya padanan token untuk gradient stop kedua atau
  glass wash, jadi tidak ada lagi yang memakainya (lihat perubahan MainActivity.kt/BlurUtils.kt
  di bawah). `TactileControl`/`TactileControlPressed` (masih tidak ada pemanggil) direfresh
  nilainya (0xFF23238A/0xFF0F0F4A) biar konsisten sama hierarki permukaan baru yang lebih terang.
- `MainActivity.kt` — **kebalikan dari perubahan fungsional terbesar Batch 51**: root `Surface`
  untuk Tactile balik jadi `MaterialTheme.colorScheme.background` datar (bukan
  `Color.Transparent` + `Box` gradient diagonal lagi) — spec §2 hanya kasih `Background` sebagai
  token tunggal ("Near-black AMOLED" flat), tidak ada pasangan stop gradient seperti spec Batch
  51 (`DarkBackgroundTop`/`DarkBackgroundBottom`), jadi tidak ada lagi yang perlu diekspresikan
  lewat `Brush.linearGradient`. `contentColor` tetap eksplisit seperti sebelumnya (tidak pernah
  bergantung ke bug class Batch 48 di kedua arah perubahan ini). Import `Color` Compose yang jadi
  tidak terpakai ikut dihapus. NavigationBar catch-light line tidak disentuh kodenya (otomatis
  re-warna lewat Color.kt, cuma komentar diperbarui).
- `BlurUtils.kt` (`frostedGlass()`, dipakai 6 file) — cabang khusus Tactile (tint dipakai apa
  adanya + layer `TactileGlassOverlay`) **dihapus**, Tactile sekarang balik pakai jalur generik
  `tint.copy(alpha = alpha)` yang sama dengan tema lain (persis seperti Batch 50) — karena
  `TactileSurface`/`TactileSurfaceVariant` sudah opaque lagi, tidak ada lagi translucency
  spec-literal yang perlu "dijaga" dari ketimpaan alpha generik.
- `TactileDepth.kt` (`tactileEmboss()`) — signature tidak berubah (8 titik pemanggil otomatis
  ikut). `borderTopAlpha` diturunkan 0.09/0.04 → 0.055/0.025 (disamakan persis ke alpha literal
  `TactileHighlight` yang baru jauh lebih rendah dari spec sebelumnya). `shadowAlpha` diturunkan
  0.68/0.35 → 0.65/0.33 (disamakan ke alpha literal `TactileShadow` yang baru). `borderBottomAlpha`
  (0.30/0.15) TIDAK diubah — tidak ada literal spec yang mengharuskan angka baru di situ.
- `Theme.kt` — `TactileColors` tetap `darkColorScheme()` (spec §13 masih larang light-mode
  fallback); **`onPrimary` diganti `Color.Black` → `Color.White`** — `TactileAccent` baru
  (0xFF7278FF) simple-luma ≈0.52, di bawah ambang 0.55 yang dipakai `MiniPlayerBar.kt`/batch-batch
  sebelumnya untuk memilih hitam vs putih, beda dari Batch 51 (0xFF5B9DFF, ≈0.59) yang masih di
  atas ambang. `onTertiary` tetap `Color.Black` (`TactileSuccess` tidak berubah, masih di atas
  ambang). `resolveIsDark(TACTILE)` tetap `true`. `storageKey` TIDAK berubah (`tactile_lite`) —
  repaint keempat atas identitas Tactile yang sama, bukan tema baru. Deskripsi tampilan diupdate
  ("Midnight Blue taktil terprogram… bevel fisik") biar tidak menyesatkan user yang masih baca
  deskripsi lama soal "kaca"/translusen.
- **Di luar cakupan, disengaja**: sama seperti batch-batch sebelumnya, spec §7/§12 minta komponen
  `TactileButton`/`TactileSwitch`/`TactileSlider` custom penuh di `ui/components/` — tidak
  dikerjakan batch ini, scope tetap atomic (recolor + pelepasan 2 fitur gradient/glass yang tidak
  diminta lagi, bukan komponen baru).
- **Belum diverifikasi runtime asli** (tidak ada compiler Android di environment kerja) —
  analisis statis + brace/paren balance dicek otomatis di semua file yang disentuh (0 selisih
  kurung/brace). Prioritas sesi berikutnya SAMA seperti batch-batch sebelumnya (belum pernah
  dirender sama sekali sejak Batch 50): build-test asli + verifikasi visual device, khususnya
  bahwa root screen benar-benar flat (bukan gradient sisa) dan tidak ada titik UI lain yang masih
  berasumsi permukaan Tactile translusen (mis. custom drawing yang sengaja mengandalkan tembus
  pandang glass Batch 51 — grep sudah dicek nihil, tapi belum diverifikasi visual).

**Batch 51** — User kirim spec baru `compose-skeuomorphism-lite-hybrid-glass-dark-blue.md`, minta
diterapkan 100% ke tema custom Tactile (menggantikan palet AMOLED-hitam Batch 50 sepenuhnya —
spec §1.1 eksplisit: "Pure/AMOLED-black styling is not the target", jadi ini bukan sekadar
"biruin dikit", tapi token diambil ulang dari §2 spec secara literal, plus 1 fitur baru yang
belum pernah ada di batch manapun sebelumnya: gradient atmosfer di root, bukan cuma flat color).
6 file kode, 1 tema kohesif (atomic).
- `Color.kt` — seluruh palet Tactile diganti dari §2 spec: `TactileBackground` 0xFF050B18 (flat
  fallback utk `colorScheme.background`/semua guard `isTactile`), `TactileBackgroundTop` BARU
  0xFF0A1630 (stop atas gradient root), `TactileSurface` 0xCC101D35 dan `TactileSurfaceVariant`
  0xB8142745 — **keduanya sekarang translusen** (alpha 0xCC/0xB8 ≈ 80%/72%), beda fundamental
  dari Batch 50 yang opaque; `TactileGlassOverlay` BARU 0x142E6AA3 (wash biru alpha sangat
  rendah, dipakai `BlurUtils.kt`); `TactileAccent` 0xFF5B9DFF (ganti dari 0xFF4DA3FF Batch 50);
  `TactileHighlight`/`TactileEdge`/`TactileShadow` semua diberi warna dasar sendiri oleh spec
  (bukan generic Color.White/Black ber-alpha rendah lagi seperti Batch 50) — 0xFFEAF4FF/0.07f,
  0xFF8FB9E8/0.10f, 0xFF020817/0.68f, semua literal §2. `TactileText`/`TactileSecondaryText`
  TIDAK berubah (spec ini kebetulan pakai nilai identik Batch 50). `TactileControl`/
  `TactileControlPressed` (masih tidak ada pemanggil, disiapkan untuk `ui/components/` masa
  depan) direfresh nilainya biar konsisten arah biru-gelap baru, bukan literal spec.
- `MainActivity.kt` — **perubahan fungsional terbesar batch ini**: root `Surface` untuk Tactile
  sekarang `Color.Transparent` (bukan `colorScheme.background` datar), dengan `Box` gradient
  diagonal (`Brush.linearGradient`, `TactileBackgroundTop` → `colorScheme.background`) dipasang
  tepat di dalamnya — mengimplementasikan mandat spec §1.1/§2/§8 "deep navy→dark-blue gradient
  background, bukan AMOLED-black dominan" yang sebelumnya sama sekali tidak ada (Batch 49/50
  cuma flat color). **Bukan kebangkitan trik Batch 48**: `contentColor` tetap selalu eksplisit
  di kedua cabang, jadi tidak ada jalur `contentColorFor(Transparent)` yang bisa jatuh ke
  `Unspecified` — beda sifat dari root cause Batch 48. NavigationBar catch-light line tidak
  disentuh kodenya (`TactileHighlight` re-warna otomatis lewat Color.kt).
- `BlurUtils.kt` (`frostedGlass()`, dipakai 6 file) — sekarang bercabang: untuk Tactile,
  `tint` (sekarang sudah translusen by-design dari Color.kt) dipakai APA ADANYA + layer
  `TactileGlassOverlay` di atasnya, bukan lagi `.copy(alpha = 0.92f)` yang dulu **membuang**
  translusensi asli spec dan menggantinya jadi hampir opaque. Tema non-Tactile tidak berubah
  sama sekali (cabang lama tetap jalan persis seperti sebelumnya).
- `TactileDepth.kt` (`tactileEmboss()`) + `NowPlayingScreen.kt` (AlbumArtHero) — **nol
  perubahan logika**, cuma komentar diperbarui. Kedua file sudah mereferensikan token by-name
  (`TactileSurfaceVariant`/`TactileSurface`/`TactileHighlight`/`TactileShadow`), jadi begitu
  Color.kt berubah jadi translusen+tinted, kedua file ini otomatis ikut memancarkan efek
  hybrid-glass tanpa disentuh — persis pola yang sama dipakai Batch 50 utk merecolor tanpa
  restructuring.
- `Theme.kt` — `TactileColors` tetap `darkColorScheme()` (spec §13 masih larang light-mode
  fallback), tidak ada perubahan struktur; `resolveIsDark(TACTILE)` tetap `true`; deskripsi
  tampilan tema di `AppTheme.TACTILE` diupdate ("Kaca biru-gelap terprogram… permukaan
  translusen") biar tidak menyesatkan user yang masih baca deskripsi lama "Bevel gelap AMOLED".
  `storageKey` TIDAK berubah (`tactile_lite`) — ini bukan identitas tema baru, cuma repaint
  ketiga atas identitas Tactile yang sama, jadi tidak perlu migrasi/fallback seperti Batch 49.
- **Di luar cakupan, disengaja**: sama seperti Batch 49/50, spec §7/§12 minta komponen
  `TactileButton`/`TactileSwitch`/`TactileSlider` custom penuh di `ui/components/` — tidak
  dikerjakan batch ini, scope tetap atomic (recolor + 1 fitur gradient-root, bukan komponen
  baru). Juga **belum diaudit**: `colorScheme.surface`/`surfaceVariant` kini translusen secara
  default di seluruh scheme (bukan cuma lewat `tactileEmboss()`/`frostedGlass()`), jadi Card/
  Surface M3 polos manapun di layar lain (Home/Library/Settings/dialog) yang masih pakai
  `colorScheme.surface` langsung tanpa lewat 2 helper itu ikut jadi translusen otomatis —
  ini SESUAI spec §8 (glass di seluruh hierarchy), tapi belum diverifikasi visual apakah ada
  titik yang jadi kurang terbaca kalau kebetulan tidak ada apa-apa di baliknya (misal
  BottomSheet/Dialog yang dirender di window terpisah dari root gradient Box) — prioritas
  audit visual sesi berikutnya, bersamaan dengan verifikasi runtime asli di bawah.
- **Belum diverifikasi runtime asli** (tidak ada compiler Android di environment kerja) —
  analisis statis + brace/paren balance dicek manual di semua file yang disentuh. Prioritas
  sesi berikutnya SAMA seperti Batch 50 (belum pernah dirender sama sekali): build-test asli +
  verifikasi visual device, KHUSUSNYA gradient root baru (`Offset.Infinite` untuk diagonal
  linear gradient — API-nya benar per dokumentasi Compose, tapi arah/kecepatan gradiennya
  sendiri belum pernah dilihat langsung) dan titik translucency-surface-tanpa-background di
  atas.

**Batch 50** — User kirim spec baru `compose-skeuomorphism-lite-dark.md`, minta diterapkan 100%
ke tema custom Tactile (menggantikan palet TERANG Batch 49 sepenuhnya — spec §1.1 eksplisit:
"Do not simply invert a light theme. Design the tactile lighting model specifically for dark
surfaces", jadi ini bukan sekadar "gelapkan" nilai lama, tapi token diambil ulang dari §2 spec
secara literal). 6 file kode + 3 file dokumentasi, 1 tema kohesif (atomic).
- `Color.kt` — seluruh palet Tactile diganti dari nol: `TactileBackground` 0xFF05070A (AMOLED),
  `TactileSurface` 0xFF0B0F14, `TactileSurfaceVariant` 0xFF111720, `TactileText` 0xFFE8EEF5,
  `TactileSecondaryText` 0xFFA8B3C0, `TactileAccent` 0xFF4DA3FF (biru dingin, ganti tembaga
  hangat lama), `TactileHighlight`/`TactileEdge`/`TactileShadow` (Color.White/Black ber-alpha
  rendah, literal spec §2) — semua ini nilai literal dari contoh kode spec, bukan tebakan.
  Ditambah 2 token baru dari tabel §2 yang belum ada pemanggilnya batch ini tapi disiapkan untuk
  komponen tactile masa depan: `TactileControl`/`TactileControlPressed` (tidak ada nilai literal
  di spec, diturunkan sendiri agar konsisten dengan hierarki gelap-terang §2). `TactileError`/
  `TactileSuccess` juga tidak ada literal spec, dipilih manual agar cocok skema biru-dingin.
- `TactileDepth.kt` (`tactileEmboss()`) — signature tidak berubah (8 titik pemanggil otomatis
  ikut), tapi seluruh alpha border/shadow ditulis ulang mengikuti aturan dark-mode spec §4
  ("Do NOT use a bright Color.White border") — border top/bottom turun dari 0.9/0.45 ke
  0.09/0.30 (normal), 0.35/0.20 ke 0.04/0.15 (pressed); shadow drop-nya sendiri justru
  dipertahankan dekat alpha penuh (0.65 spec-literal, bukan diturunkan) karena background sudah
  nyaris hitam — **pelajaran dari saga Matte Noir Batch 39-44 dipakai lagi di sini**: bayangan
  hitam-di-atas-hitam yang terlalu tipis alpha-nya akan hilang total, bukan sekadar "restrained".
- `Theme.kt` — `TactileColors` ganti dari `lightColorScheme()` ke `darkColorScheme()`;
  `resolveIsDark(TACTILE)` dibalik `false` → `true` (otomatis membalik ikon status bar/nav bar
  jadi terang lewat `MainActivity.kt` yang sudah ada, tidak perlu disentuh manual); `onPrimary`/
  `onTertiary` dipilih `Color.Black` lewat aturan luminance yang sama dipakai `MiniPlayerBar.kt`
  (>0.55 → hitam) karena `TactileAccent`/`TactileSuccess` baru cukup terang.
- `NowPlayingScreen.kt` (AlbumArtHero) + `MainActivity.kt` (garis catch-light NavigationBar) —
  2 titik manual (bukan lewat `tactileEmboss()`) direcolor & alpha-nya diselaraskan ke aturan
  yang sama: NavigationBar 0.9/0.05 → 0.10/0.02 (dulu praktis garis putih nyaris opaque,
  langsung melanggar §4); AlbumArtHero border 0.9/0.40 → 0.12/0.32, shadow 0.28 → 0.55, glow
  aksen lagu 0.5 → 0.42 (spec §9 izinkan glow di elemen selected/active seperti ini, tapi tetap
  "restrained").
- `BlurUtils.kt` — trim aksen di `frostedGlass()` (dipakai 6 file) alpha 0.35 → 0.22, sekadar
  penyesuaian restraint karena aksen biru baru terasa lebih terang dari tembaga lama di alpha
  yang sama.
- **Di luar cakupan, disengaja**: spec §7/§12 minta komponen `TactileButton`/`TactileSwitch`/
  `TactileSlider` custom penuh di `ui/components/` — batas ini SAMA seperti batas Batch 49
  (slider/toggle/switch tetap Material3 polos), tidak diperluas batch ini supaya scope tetap
  atomic. Kalau user mau cakupan itu juga, itu kerja terpisah yang lebih besar (file baru,
  bukan cuma recolor).
- **Belum diverifikasi runtime asli** (tidak ada compiler Android di environment kerja) —
  analisis statis + brace/paren balance dicek manual di semua file yang disentuh. Prioritas
  sesi berikutnya: build-test asli + verifikasi visual device untuk tema Tactile versi gelap
  ini (belum pernah dirender sama sekali, sama seperti nasib versi terang Batch 49 sebelum
  sempat diverifikasi).

**Batch 49** — User minta hapus SEMUA jejak tema custom "Matte Noir" lama sampai bersih, lalu
terapkan tema custom baru murni dari `compose-skeuomorphism-lite.md`. Selesai: 11 file, atomic
change. Matte Noir (semua warna, shape, typography, `MatteDepth.kt`, enum `AppTheme.MATTE`)
DIHAPUS total, bukan direname doang. Diganti `AppTheme.TACTILE` — palet TERANG baru (warna
`0xFFF8FAFC`/`0xFFE2E8F0` di `Color.kt` adalah literal contoh kode di spec §1, bukan
interpretasi bebas), `TactileDepth.kt` (`tactileEmboss()`, logic sama dengan hasil Batch 46/47
yang sudah sesuai spec, cuma direcolor). Bonus: root `Surface(color=Transparent)` trick di
`MainActivity.kt` (biang kerok Batch 48) DIHAPUS TOTAL, bukan cuma di-patch — root Surface
sekarang selalu opaque + `contentColor` eksplisit utk semua tema, jadi kelas bug itu tidak bisa
terulang lagi sama sekali kedepannya. Storage key preferensi tema berubah dari `matte_noir` ke
`tactile_lite` — user lama yang masih ada preferensi Matte tersimpan otomatis fallback ke
SYSTEM (bukan crash, disengaja). Lihat CHANGELOG.md Batch 49 untuk detail penuh + daftar 11
file. **Belum diverifikasi runtime asli** — batch ini cakupannya paling besar sejauh ini, jadi
build-test asli + verifikasi visual device jadi prioritas MUTLAK sebelum lanjut fitur lain.
Grep akhir `Matte` di seluruh kode aktif = 0 hasil (cuma komentar historis).

**Batch 48** — User kirim screenshot: teks di LockScreen (judul "Masukkan PIN" + digit keypad)
render HITAM di atas background nyaris-hitam Matte, nyaris tak terbaca. Root cause: root
`Surface(color = Color.Transparent)` di `MainActivity.kt` (trik ambient-glow Batch 40) bikin
`contentColorFor(Transparent)` jatuh ke `Unspecified` → fallback ke `LocalContentColor` yang
belum pernah di-set (`AudioPlayerTheme()` cuma bungkus `MaterialTheme(...)`, tidak pernah pakai
Surface) → default mentah Compose: `Color.Black`. LockScreen kena polos karena tidak punya
Surface/Card lokal sendiri untuk "menyelamatkan" diri (beda dari Library yang tiap row list-nya
py Surface sendiri). Fix saat itu: `contentColor = MaterialTheme.colorScheme.onBackground`
eksplisit di Surface root. **Catatan: fix ini sudah DIGANTIKAN total oleh Batch 49** yang
menghapus trik `Transparent` itu sepenuhnya, jadi detail fix Batch 48 ini historis saja.

**Batch 47** — Hotfix compile error Batch 46 dari `log_fail_104.zip`: `MatteDepth.kt` pakai
`by animateDpAsState(...)` / `by animateFloatAsState(...)` tapi lupa
`import androidx.compose.runtime.getValue`. Fix: tambah import. 1 baris, 1 file. Exact match ke
error log, bukan tebakan — confidence tinggi. Prioritas berikutnya masih sama: user
verifikasi tampilan tema Matte hasil Batch 46 di device asli (belum pernah, sejak Batch 40).

**Batch 46** — User kirim spec desain sendiri (`compose-skeuomorphism-lite.md`) karena tema
Matte hasil Batch 40-44 dinilai "jelek banget asli". `matteEmboss()` di `MatteDepth.kt` ditulis
ulang total mengikuti 3 poin spec (gradient top-down + bevel border, animasi tekan-fisik
sungguhan, intensitas diturunkan/flat untuk card struktural). Signature tidak berubah jadi
5 pemanggil lama ikut otomatis; 1 titik manual (AlbumArtHero, `NowPlayingScreen.kt`) ditulis
ulang manual juga biar konsisten + sekalian buang native `Modifier.shadow` yang sudah lama
terbukti invisible di background gelap. Lihat CHANGELOG.md Batch 46 untuk detail penuh. **Belum
diverifikasi device** — kalau user masih bilang jelek, JANGAN tambah shadow/gradient lagi (pola
gagal berulang Batch 40-44), minta screenshot + bagian spesifik yang salah.

**Batch 45** — User lapor bug "gak sinkron" di segmen signature key matching. Ditemukan:
`ApkSignatureChecker.inspect()` ambil `signingCertificateHistory.firstOrNull()` (cabang
single-signer) — array ini oldest→newest per dokumentasi resmi `SigningInfo`, jadi
`firstOrNull()` salah ambil sertifikat ORIGINAL, bukan yang AKTIF sekarang. Untuk app yang
pernah key rotation, ini bikin hasil MATCH/MISMATCH di UI tidak sinkron dengan keputusan
instalasi Android yang sebenarnya. Fix: ganti ke `.lastOrNull()`. Lihat CHANGELOG.md Batch 45
untuk detail. **Belum diverifikasi runtime asli** — kalau user masih lapor "gak sinkron"
setelah ini, minta contoh 2 APK spesifik yang dipakai test (terutama apakah salah satunya
pernah rilis dengan key rotation) sebelum menebak sisi lain.

**Batch 44** — Fix Batch 43 ternyata salah juga: `drawOutline` **tidak pernah ada** di API
Compose (dicek langsung ke source AOSP `DrawScope.kt` — cuma ada drawLine/drawRect/
drawRoundRect/drawCircle/drawOval/drawArc/drawPath/drawPoints). Fix benar: `Path().apply {
addOutline(outline) }` lalu `drawPath(path, color)`. Lihat CHANGELOG.md Batch 44. **Ini fix
build ketiga berturut-turut untuk fitur shadow yang sama (Batch 42→43→44) — kalau build masih
gagal setelah ini, JANGAN tebak nama API lagi; cari signature persis di source AOSP dulu
sebelum tulis kode. Kalau build akhirnya sukses, shadow visual-nya SENDIRI masih belum pernah
diverifikasi di device sama sekali sejak Batch 42 — itu prioritas berikutnya.**

**Batch 43** — Hotfix build gagal dari Batch 42: `log_fail_5.zip` user tunjukkan
`compileDebugKotlin` gagal, `Unresolved reference: drawOutline` di `MatteDepth.kt` (2 baris).
Sebab: `drawOutline` itu extension function `DrawScope`, bukan method bawaan — lupa diimpor
padahal `translate` di baris sebelahnya sudah benar. Fix: tambah 1 baris import. Belum ada
perubahan logika/tampilan lain dari Batch 42. **Prioritas sesi berikutnya masih sama seperti
Batch 42: verifikasi shadow manual ini benar-benar kelihatan di device asli — build sekarang
seharusnya sukses, tapi efek visualnya sendiri belum pernah dirender sama sekali.**

**Batch 42** — Hotfix Batch 41 (lagi): shadow `matteEmboss()` masih invisible di device asli
setelah elevation dinaikkan (bukti screenshot). Root cause lebih dalam: opacity shadow native
`Modifier.shadow` punya cap rendah yang tidak bisa didorong lewat elevation di background
gelap pekat. Fix: buang native shadow total, ganti manual `drawBehind` + `Outline` 2-layer
(`MatteUmbra` alpha 0.30f/0.5f) offset kanan-bawah — kontras sekarang dikontrol alpha kita
sendiri, bukan platform. Lihat CHANGELOG.md Batch 42 untuk detail lengkap. **BELUM
diverifikasi di device — ini prioritas #1 sesi berikutnya. Kalau masih kurang kontras, jangan
otak-atik alpha kecil-kecilan lagi (sudah 2 fix gagal dengan pola itu); screenshot ulang dan
ukur kontras aktual (color picker) sebelum nebak angka lagi.**

**Batch 41** — Hotfix Batch 40 dari screenshot render asli: shadow `matteEmboss()` nyaris tak
kelihatan (`MatteUmbra` cuma ~12 unit lebih gelap dari `MatteBackground`, dan tint warna native
shadow cuma ubah hue bukan opacity). Fix di `MatteDepth.kt` + 5 call-site: elevation dinaikkan
di semua titik (bukan ganti warna — opacity shadow dikontrol elevation), highlight/umbra alpha
gradient dinaikkan. Lihat CHANGELOG.md Batch 41 untuk angka lengkap. **Belum diverifikasi ulang
di device — kalau masih kurang kontras, jangan naikkan elevation lagi tanpa batas; pertimbangkan
manual scrim/blur overlay independen dari native shadow API sebagai gantinya.**

**Batch 40** — Lanjutan langsung Batch 39 ("semua area belum kerasa premium", bukan 1 titik).
User diberi penjelasan 4 gaya kedalaman dulu (neumorphism/skeuomorphic/glass gelap/elevasi+
gradient terarah), pilih kombinasi neumorphism ringan + elevasi/gradient cahaya terarah. Dibuat
1 helper terpusat `Modifier.matteEmboss()` (`MatteDepth.kt`, baru) — shadow dua-warna terarah
(`MatteUmbra`) + gradient diagonal `MatteHighlight → MatteSurface → MatteUmbra` + border
catch-light 1dp, gabungan neumorphism (border edge menangkap cahaya) + directional light
(gradient, bukan tonalElevation datar) dalam 1 modifier reusable. Dipasang di 7 titik: mini
player, ContinueListeningCard Home, undo-snackbar Library, GestureIndicatorBadge Now Playing,
ThemeOptionCard Matte di Settings (showcase depth di picker-nya sendiri), NavigationBar
(catch-light line 2px), plus `AlbumArtHero` dapat treatment lebih kuat manual (shape token +
shadow ganda + border, tidak pakai helper generik karena sudah punya accent-glow per-lagu
sendiri). `matteDepthBrush()` root alpha dinaikkan 0.10f→0.22f (versi lama kalah dari
kecerahan layar asli). 9 file (1 baru+8 edit), 1 tema kohesif. **Tetap murni analisis
statis, belum pernah dirender** — kalau "kureng" lagi di test HP, JANGAN ulangi pola nambah
shadow/gradient lagi (sudah 2x coba begitu), curigai sesuatu yang cuma kelihatan di layar
asli (kontras, ukuran relatif DPI) — lihat CHANGELOG.md Batch 40 untuk detail.

**Batch 39** — Respons user test Batch 38 di HP asli ("masih kureng"): Matte Noir dikasih
efek kedalaman visual. Root cause: `darkColorScheme()`/`lightColorScheme()` M3 diam-diam
isi `surfaceTint` (mekanisme utama M3 buat tonal-elevation depth) dengan ungu baseline
kalau tidak disebut eksplisit — sekarang eksplisit tiap skema (`AppleAccent`/`MatteAccent`).
Plus: `frostedGlass()` (`BlurUtils.kt`, dipakai 6 file) shape-nya ikut
`MaterialTheme.shapes.large` (dulu hardcode 24dp, sekarang 8dp di Matte = boxy) + trim
tembaga di border; `matteDepthBrush()` baru — radial gradient dipasang di root `Surface`
`MainActivity.kt` (Matte-only, dibungkus `Box`, `Surface` jadi transparent supaya gradient
kelihatan); `NavigationBar` tonalElevation 12dp khusus Matte; `MiniPlayerBar.kt` shape
disamakan + shadow ambientColor/spotColor ditinta tembaga elevasi 16dp. 5 file kode +
2 doc. **PENTING**: murni analisis statis, sama sekali belum pernah dirender — kalau user
lapor "masih kureng" lagi, jangan asumsi solusinya menambah lebih banyak color/shadow tanpa
tahu spesifik bagian mana yang dirasa kurang (radial gradient-nya kurang kuat? shape-nya
belum kerasa? nav bar-nya masih flat? tanya spesifik dulu).

**Batch 38** — 2 hal: (1) fix dokumentasi drift tema (README/PROJECT_STATE masih deskripsikan
"3 tema" lama Ink & Brass/Midnight Bloom/Paper & Ink + klaim status bar dipaksa gelap, padahal
`Theme.kt` sudah lama migrasi ke model SYSTEM/LIGHT/DARK ala Apple dan `MainActivity.kt:187`
sudah ikut tema — persis pola yang diperingatkan Batch 17); (2) atas permintaan eksplisit,
tambah tema ke-4 **Matte Noir** — jadikan keluarga Apple (SYSTEM/LIGHT/DARK) tema utama/default
(tidak berubah, tetap `AppTheme.SYSTEM`), plus satu identitas custom `AppTheme.MATTE` yang
sengaja kebalikan Apple: matte hangat bukan hitam/putih ekstrem, aksen tembaga bukan biru,
judul serif (`FontFamily.Serif`, font sistem — tidak nambah aset font) bukan sans, sudut
4/6/8dp nyaris kotak bukan 14/20/28dp membulat, statis selalu gelap (tidak ikut sistem).
Sekalian fix warna hardcode `SignatureMatcherSheet.kt:96` (`Color(0xFF3FA34D)` →
`MaterialTheme.colorScheme.tertiary`) yang jadi alasan nambah role `tertiary`
(sukses/match, hijau) ke skema Apple (`AppleDarkSuccess`/`AppleLightSuccess`) juga.
9 file disentuh, 1 tema kohesif (theme-system expansion, atomic — enum+scheme+shape+
typography+1 caller+2 doc+1 changelog tak terpisah tanpa saling pecah konsistensi).
`colorsFor()` ganti signature dari `(isDark: Boolean)` jadi `(theme: AppTheme, isDark:
Boolean)` — 1 pemanggil di luar `Theme.kt` (`SettingsScreen.kt:288`) sudah disesuaikan,
dicek tak ada pemanggil lain (`grep` bersih). **Belum diverifikasi build/runtime asli**
(tidak ada compiler Android di environment kerja) — analisis statis + brace/paren balance.

**Batch 37** — Truncation tanpa ellipsis, lanjutan temuan Batch 31 yang dulu ditunda. Audit
21 titik `maxLines = 1` di `ui/*.kt`, 4 gap ditemukan & dikerjakan (2 file): album di grid
Library (`LibraryScreen.kt` ~499), judul lagu di daftar-lagu-dalam-album (~522), judul+artis
di daftar "Lagu Disembunyikan" `FolderManagerSheet.kt` (~179, ~182) — semua ditambah
`overflow = TextOverflow.Ellipsis`, import sudah ada di kedua file. Ditemukan tapi belum
dikerjakan: 356 literal `.dp` ad-hoc tanpa `Spacing.kt` terpusat (scope terlalu besar untuk
1 batch), audit `IconButton` tanpa `contentDescription` (hasil: nihil, semua sudah benar).
**Belum diverifikasi build/runtime asli** (tidak ada compiler Android di environment kerja) —
analisis statis + script brace/paren balance.

**Batch 36** — Arahan melebar: sambil nunggu build hijau, debugging+optimalisasi TETAP jalan
tapi sekarang juga polish UI/UX + detail kecil kenyamanan pemakaian (bukan lagi "stop fitur
baru" ketat ala Batch 34). Audit Settings/Library/mini-player nemu 4 hal, user pilih semua:
- `SetPinDialog` (Settings) kini `KeyboardType.NumberPassword` + `PasswordVisualTransformation`
  — konsisten sama `LockScreen` yang sudah lama polished (dot mask, haptic, shake-on-error).
- `LibrarySearchField` kini punya `ImeAction.Search` + `KeyboardActions` yang nutup keyboard
  via `LocalSoftwareKeyboardController` — logika pencarian (live per keystroke) tidak berubah.
- `MiniPlayerBar` kini punya garis `LinearProgressIndicator` 2dp glanceable-only (bukan
  seekable) di tepi bawah, dari `uiState.position/duration` yang sudah ada. **Cek versi API
  dulu sebelum pakai**: overload `progress: Float` sudah deprecated sejak Material3 1.2.0,
  proyek ini pin compose-bom 2024.05.00 (~Material3 1.2.1) jadi pakai overload lambda
  `progress: () -> Float`.
- Teks "Tentang Aplikasi" di Settings disingkat dari `"AudioPlayer versi 1.0.254 (build 254)"`
  jadi `"AudioPlayer versi 1.0.254"` — commit count yang sama sebelumnya nongol dua kali dalam
  format beda (berantakan+kepanjangan). **Skema penomoran versi (git commit count,
  `app/build.gradle.kts`, protected asset) TIDAK disentuh** — murni string tampilan.

Belum diverifikasi build/runtime asli (tidak ada compiler Android di environment kerja) —
analisis statis + audit versi API manual.

**Batch 35** — Lanjutan arahan Batch 34 (debugging + optimalisasi performa, tanpa fitur baru).
Audit statis menyisir file berisiko tinggi (`PlaybackService`, `PlayerViewModel`,
`MusicRepository`, `AccentColorExtractor`, `EqualizerController`, `AppLogger`,
`CustomFolderScanner`, `ShakeDetector`) plus seluruh `LazyColumn`/`LazyRow` di UI — semua
bersih (key list konsisten, I/O sudah di `Dispatchers.IO`, sensor listener paired start/stop).
1 temuan, dikerjakan atas persetujuan user:
- **Widget jank belum tuntas dari Batch 34 (performa)** — `PlayerWidgetProvider.onUpdate()` &
  `onAppWidgetOptionsChanged()` masih manggil `WidgetUpdater.updateAll()` (decode+crop+round
  bitmap album-art) **langsung di main thread** — Batch 34 cuma mindahin call-site di
  `PlaybackService`, dua call-site di sini kelewat karena `AppWidgetProvider` adalah
  `BroadcastReceiver` biasa, bukan Service, jadi tidak otomatis kebagian `serviceScope`.
  Lebih parah dari sisi `PlaybackService`: `onAppWidgetOptionsChanged` dikomentari sendiri di
  kode lama sebagai "fires live as user drags resize handles" — decode blocking bisa numpuk
  tiap event drag. Fix: `goAsync()` (API standar `BroadcastReceiver` buat kerja lanjut setelah
  callback return tanpa blocking pemanggil) + `providerScope.launch(Dispatchers.IO)`,
  `pendingResult.finish()` di `finally` supaya wakelock broadcast tetap dilepas walau
  `updateAll` throw.

**Belum diverifikasi build/runtime asli** (tidak ada compiler Android di environment kerja) —
hanya analisis statis. `versionName` tetap otomatis dari commit count (tidak disentuh batch
ini).

**Batch 34** — Arahan baru mulai batch ini: **stop fitur baru, fokus debugging +
optimalisasi performa & eksekusi sampai aplikasi matang**. Audit kode nyata (bukan asumsi)
menemukan 2 hal, keduanya dikerjakan sekaligus atas persetujuan user:
1. **Widget jank (performa)** — `pushWidgetUpdate()` di `PlaybackService` decode+crop+round
   bitmap album-art secara síncron di **main thread** (lewat `WidgetUpdater.updateAll`),
   dipanggil tiap `onMediaItemTransition` & `onIsPlayingChanged` — dua event paling sering
   dan paling terlihat user (ganti lagu, tap play/pause). Fix: kerjaan berat dipindah ke
   `serviceScope.launch(Dispatchers.IO)`, dengan `widgetUpdateJob` yang di-cancel sebelum
   relaunch supaya skip/toggle cepat tidak bikin update lama nimpa update baru (race
   kondisi art/state jadi stale). `saveState` (SharedPreferences) tetap di thread pemanggil
   karena `.apply()` sudah async-safe sendiri.
2. **Crash logger drift dari spec (debugging)** — `AppLogger.writePublicCrashLog` ternyata
   sudah lama menyimpang dari spec awal ("FIFO Retention max 50, metadata lengkap
   Version/OS/Model/Timestamp/Thread/StackTrace, nama file pakai UUID"): belum ada UUID di
   nama file (risiko overwrite kalau crash-loop dalam detik yang sama), belum ada retensi
   FIFO sama sekali (folder `Documents/AudioPlayer/logs` numpuk tanpa batas), metadata cuma
   Timestamp+Thread+StackTrace (tidak ada Version/OS/Model — paling penting buat triage
   lintas device/build). Fix: tambah `UUID.randomUUID()` di nama file, tambah blok
   Version/OS/Model ke isi log (versi lewat `PackageInfoCompat.getLongVersionCode`, sudah
   ada dependency `androidx.core:core-ktx` di `build.gradle.kts`), tambah
   `enforceCrashLogRetention()` — query MediaStore by `RELATIVE_PATH` terurut
   `DATE_ADDED DESC`, hapus sisa di luar 50 terbaru. Log privat (`diagnostic_log.txt`,
   trim-by-size) tidak disentuh — itu mekanisme terpisah dan sudah benar.

Kedua fix **belum diverifikasi build/runtime asli** (tidak ada compiler Android di
environment kerja) — hanya analisis statis (grep + baca kode + cross-check API behavior).
`versionName` tetap otomatis dari commit count (tidak disentuh batch ini).

**Batch 33** — Hotfix build gagal dari Batch 32, **diagnosis Batch 32 sendiri ternyata
salah**. `log_fail_94.zip` (build #94): error identik dengan sebelum Batch 32 —
`Unresolved reference: matchParentSize` di `Utils.kt` baris 16 (import) & 61 (call).
Root cause sebenarnya: `matchParentSize()` bukan extension biasa milik `BoxScope`, tapi
**member extension function milik `Modifier`, dideklarasikan di dalam interface
`BoxScope`** (`fun Modifier.matchParentSize(): Modifier` di dalam `interface BoxScope`).
Konsekuensinya dua arah: (1) tidak bisa di-`import` sebagai fungsi top-level — baris
import Batch 31 sudah salah sejak awal, cuma kebetulan tidak dicek compiler sampai
sekarang; (2) pemanggilannya **wajib** tetap pakai prefix `Modifier.` (mis.
`Modifier.matchParentSize()`) walau dipanggil di dalam lambda `Box { }` — bukan dibuang
seperti fix Batch 32. Fix: hapus baris import yang salah, kembalikan pemanggilan ke
`Modifier.matchParentSize()`. **Pelajaran: pesan error compiler "receiver type mismatch"
di Batch 32 sudah menunjukkan signature `Modifier.matchParentSize()` secara eksplisit di
teks errornya sendiri — dibaca sebagai "buang prefix Modifier" padahal maksud sebenarnya
kebalikannya. Ke depan, kalau ada API Compose yang tidak lazim (member extension di
dalam interface, bukan top-level), cross-check ke source resmi
`androidx.compose.foundation.layout.Box.kt`, jangan simpulkan dari nama fungsi di pesan
error saja.** Tidak ada usage lain fungsi ini di project (dicek `grep`). **Belum
diverifikasi build/runtime asli (tidak ada compiler di environment kerja).**
`versionName` tetap otomatis dari commit count (tidak disentuh batch ini).

**Batch 32** — Hotfix build gagal dari Batch 31 (**diagnosis salah, lihat Batch 33 di
atas untuk koreksi**). `log_fail_93.zip` (build #93): `Unresolved reference:
matchParentSize` di `Utils.kt` (`AlbumArt`). Fix yang diterapkan saat itu: buang prefix
`Modifier.` — **ternyata inilah yang menyebabkan build #94 gagal lagi dengan error
identik**, dikoreksi di Batch 33. `versionName` tetap otomatis dari commit count (tidak
disentuh batch ini).

**Batch 31** — Polish UI/UX pass pertama (dari audit statis, user pilih 5 dari daftar temuan
lebih luas). 8 file, 1 tema kohesif. (1) Album-art fallback: helper baru `AlbumArt` di
`Utils.kt` (Coil `SubcomposeAsyncImage`, `error` slot → ikon nada musik di atas
`surfaceVariant`, `loading` slot sengaja kosong biar cover asli gak kedip) menggantikan
`AsyncImage` mentah di 6 titik (Home x2, LibraryScreen x2, MiniPlayerBar, NowPlayingScreen
x2) — sebelumnya lagu tanpa cover art cuma nampilin ruang kosong. Backdrop blur NowPlaying
sengaja `showIcon = false` (ikon bakal jadi gumpalan gak jelas kalau di-blur 60dp). (2) Empty
state disatukan ke komponen `EmptyState` yang sudah ada (LibraryScreen.kt) — sebelumnya 3
pola beda (komponen penuh di Library/Playlist, custom inline icon+text di Home, `Text` abu
polos di Queue/FolderManager); `EmptyState` ditambah parameter `modifier` opsional (default
tetap `fillMaxSize().padding(32.dp)`, gak ubah 5 pemanggilan lama) supaya bisa dipakai di
`LazyColumn` item (Home) dan bottom sheet (Queue, FolderManager) tanpa crash fillMaxSize di
context yang gak punya bounded height. (3) Nama artis kepotong tanpa "..." di 4 lokasi
high-traffic (Home x2, MiniPlayerBar, LibraryScreen) — `maxLines=1` tanpa
`TextOverflow.Ellipsis` — ditambahkan; judul lagu di lokasi sama sengaja dibiarkan (pakai
`basicMarquee()`, overflow ditangani lewat scroll bukan potongan). (4) Tombol tutup
`FeatureHintBanner` 28dp → 40dp (di bawah standar target sentuh aksesibilitas). (5)
`animateItemPlacement()` ditambah ke list folder `FolderManagerSheet` (konsisten dengan
Library/Playlist/Queue). Import Coil `AsyncImage` & ikon `LibraryMusic`/`TextAlign` yang jadi
gak kepake dibersihkan dari 4 file. **Batas jaminan: analisis statis saja (brace/paren
balance dicek manual, tidak ada kotlinc di environment ini) — belum diverifikasi
runtime/emulator, termasuk perilaku `SubcomposeAsyncImage` yang baru pertama kali dipakai di
proyek ini (sebelumnya cuma `AsyncImage` biasa).** Ditemukan tapi belum dikerjakan (di luar
scope batch ini, bukan dipilih user): auditor menemukan judul album di grid Library juga
`maxLines=1` tanpa ellipsis, dan token spacing gak seragam (dp value ad-hoc per file, gak ada
`Spacing.kt`) — keduanya dianggap dampak rendah, disimpan untuk batch lanjutan kalau
diminta. `versionName` tetap otomatis dari commit count (tidak disentuh batch ini).

## Batch 30
**Batch 30** — Otomatisasi & minimalisasi versionName. Ditemukan lewat cek `build.yml` untuk
jawab permintaan ini: `versionName` app (manual, `3.9`) dan tag GitHub Release (otomatis,
`v1.0.<commit-count>`) adalah dua angka tak nyambung. Fix: `versionName` sekarang turunan
`gitCommitCount()` yang sama dipakai `versionCode` (pola `1.0.<commit-count>`) — tidak ada
lagi bump manual. Efek samping tanpa nyentuh CI workflow sama sekali: nomor di app dan nomor
di nama file APK/tag Release sekarang selalu match (dua perhitungan independen, git history
sama). Trade-off jujur: tampilan Settings sekarang dua angka mirip (`1.0.254` / `254`), tidak
ada lagi nomor rilis "kurasi" gaya `3.9`. README § Standar Penomoran Versi + footer
PROJECT_STATE.md disinkronkan. **Baris "`versionName` tetap X" di pelaporan batch sudah
tidak relevan mulai batch ini** — naik otomatis tiap commit.

**Batch 29** — Hotfix build gagal dari Batch 28. `log_fail_91.zip` (build #91) dianalisis:
`androidResources { localeFilters += listOf("en") }` gagal kompilasi — `Unresolved
reference: localeFilters`. Root cause: DSL itu benar secara konsep tapi baru ada di rilis
AGP setelah 8.4.1 (versi project ini), bukan sejak AGP 8.0 seperti diasumsikan Batch 28.
Fix: ganti ke `resourceConfigurations += listOf("en")` di `defaultConfig` — DSL lama, sudah
lama stabil, terverifikasi didukung penuh di AGP 8.4.1. Hasil akhir (buang resource
terjemahan library untuk locale selain "en") identik dengan niat Batch 28. Perubahan Guava→
concurrent-futures dari Batch 28 tidak disentuh (bukan sumber kegagalan). **Pelajaran:
klaim "tersedia sejak AGP X" dari sumber pihak ketiga butuh cross-check ke nomor AGP project
ini persis, bukan digeneralisasi.** `versionName` tetap `3.9`.

**Batch 28** — Optimasi ukuran APK. Audit eksternal baru (skor 9.3/10) taruh ukuran APK di
prioritas #1 — beda urutan dari self-review internal Batch 27 (taruh di posisi terakhir);
dipilih karena satu-satunya yang hasilnya bisa dicek objektif dari ukuran APK output CI
tanpa runner. 2 perubahan: (1) `androidResources { localeFilters += listOf("en") }` di
`app/build.gradle.kts` — buang resource terjemahan AndroidX/Compose/Media3/Coil untuk
locale yang tidak dipakai (app sendiri cuma punya `values/` default, tidak kesentuh). (2)
`com.google.guava:guava` penuh diganti `androidx.concurrent:concurrent-futures:1.3.0` —
cuma pernah dipakai untuk `ListenableFuture`/`SettableFuture` (`PlaybackService`, demi API
session callback Media3) dan `MoreExecutors.directExecutor()` (`PlayerViewModel`).
`SettableFuture` → `CallbackToFutureAdapter`, `directExecutor()` → `Executor { it.run() }`
polos. `ListenableFuture` tetap ada sebagai tipe lewat shim kecil
`com.google.guava:listenablefuture:1.0` (dependency transitif concurrent-futures), bukan
guava penuh lagi. **Batas jaminan: analisis statis saja — ukuran APK sebelum/sesudah baru
kelihatan dari artifact GitHub Release setelah push ini.** Prioritas #2-5 dari audit baru
(error handling, testing, technical debt, maintainability) belum disentuh — batch
berikutnya. `versionName` tetap `3.9`.

**Batch 27** — Fondasi testing otomatis. Dari self-review internal (skor 8.8/10, prioritas:
testing → performa → memori/battery → refactor business logic → benchmark → ukuran APK),
dikerjakan prioritas #1 dulu. 2 gap: (1) CI (`.github/workflows/build.yml`) tidak pernah
menjalankan 4 test JVM yang sudah ada di repo — ditambah step `gradle testDebugUnitTest`
sebelum decode keystore. (2) 3 business logic kritis tidak bisa di-unit-test karena menyatu
dengan Context/Android framework — diekstrak ke pure function/class tanpa ubah perilaku:
`ShakeDetector` → `ShakePulseTracker` baru (pulse-counting shake-to-skip dari fix Batch 25,
belum pernah terverifikasi langsung sebelum ini), `MusicRepository.deriveFolderName`
(parsing folder dari path MediaStore), `LibraryFilterStore.shouldKeep` (filter gabungan
folder-dikecualikan + lagu-disembunyikan — sengaja terima `folderPath`/`id` polos, bukan
`Song` utuh, karena `Song.uri` bertipe `android.net.Uri` tidak aman dikonstruksi di pure-JVM
test tanpa Robolectric). 21 test baru total (8 `ShakePulseTracker` + 9 `MusicRepository`
folder-name + 4 `LibraryFilterStore`). **Batas jaminan: seperti biasa analisis statis saja
— tidak ada kotlinc di environment ini, jadi test-test ini belum pernah benar-benar
dijalankan; verifikasi sungguhan baru terjadi di push pertama setelah ini lewat CI (Gap 1
di atas).** Prioritas #2-6 dari self-review (performa, memori/battery, refactor business
logic lanjutan, benchmark, ukuran APK) sengaja belum disentuh — batch berikutnya. Tidak ada
perubahan behavior/fitur user-facing. `versionName` tetap `3.9`.

**Batch 26** — Audit feedback interaksi (scope: "apa yang terjadi/diharapkan saat user
berinteraksi dengan app"), cakupannya **beda dari** audit haptic Batch 25 (favorit,
long-press-select, rating bintang — itu semua sudah kelar duluan). 4 gap ditemukan &
dibenarkan: (1) `LockScreen` — layar paling sering dipencet tiap buka app, ternyata nol
haptic sama sekali termasuk saat PIN salah; ditambah haptic per digit/backspace + haptic
tegas & shake 300ms (keyframes) khusus saat salah/lockout. (2) Semua slider (seek bar &
volume di `NowPlayingScreen`, band + preset di `EqualizerSheet`) nol haptic saat rilis
jari; ditambah `onValueChangeFinished`/`onClick` haptic ringan. (3) Hapus folder tambahan
di `FolderManagerSheet` langsung hilang tanpa konfirmasi ATAU undo — dicek dulu ke kode:
`releasePersistableUriPermission` itu **tidak bisa** di-undo asli (butuh user pilih ulang
lewat SAF picker), jadi pola Undo Snackbar yang sudah ada (queue/playlist) **sengaja tidak
dipakai** di sini karena akan jadi tombol "Urungkan" yang bohong; solusinya AlertDialog
konfirmasi sebelum hapus. (4) 6 titik (`LibraryScreen` x4, `DiagnosticLogSheet`,
`SignatureMatcherSheet`) masih pakai `Toast.makeText` mentah — ganggu identitas visual
"Ink & Brass" (Toast ikut style OS, bukan tema app) dan beda posisi dari SnackbarHost yang
sudah ada; disatukan ke kanal baru `PlayerViewModel.infoMessage` (pola one-shot StateFlow
sama seperti `celebrationMessage`/`actionErrorMessage`/`undoableAction` yang sudah ada),
dirender lewat Snackbar bertema di `MainActivity`. Sekalian dibenerin: tombol "Hapus" di
`DiagnosticLogSheet` (clear log) sebelumnya nol feedback juga padahal aksi destruktif.
**Batas jaminan: analisis statis kode saja (brace/paren balance dicek manual, tidak ada
kotlinc di environment ini) — belum diverifikasi runtime/emulator.** `versionName` naik
3.8 → 3.9. 10 file Kotlin disentuh dalam 1 tema kohesif (feedback-consistency pass, sama
presedennya kayak Batch 6) + 1 baris `app/build.gradle.kts` (version bump, Protected File
edit parsial).

**Batch 25** — 2 bug user-reported diperbaiki. (1) MiniPlayerBar `onExpand` navigate ke
`now_playing` tanpa `launchSingleTop` → numpuk di backstack kalau di-tap cepat, fix: tambah
`launchSingleTop = true`. (2) Lagu skip sendiri saat app di-swipe dari Recents → root cause
kemungkinan besar (dari baca kode, **belum terverifikasi runtime**): `ShakeDetector` di
`PlaybackService` tetap hidup independen dari `PlayerViewModel` (yang mati saat Activity
finish), dan sebelumnya fire dari 1 spike g-force tunggal — nyaris tidak beda dari HP
kebanting di kantong. Fix: syaratkan 3 pulse dalam 900ms sebelum fire. **Kalau Shake-to-Skip
user OFF, diagnosis ini belum tentu penyebabnya — perlu ditelusuri ulang** (lihat
CHANGELOG.md Batch 25 untuk kandidat yang sudah disingkirkan). Susulan sama batch: CI
workflow ternyata masih pakai GitHub Actions artifact (bukan Release) — sudah dibenerin ke
`softprops/action-gh-release`. Susulan lagi: audit konsistensi haptic feedback menemukan 3
gap (toggle favorit beda perlakuan Library vs Now Playing, long-press pilih di Library nol
haptic, rating bintang nol haptic) — dibenarkan semua. `versionName` masih `3.8`.

**Batch 24** — Fix Batch 23 (bump lifecycle 2.8.1→2.8.2) **ternyata tidak cukup** — crash
`LocalLifecycleOwner not present` masih terjadi persis sama (dikonfirmasi lewat crash log baru
via fitur Batch 22). Root cause sebenarnya: ada **DUA** `LocalLifecycleOwner` yang berbeda
sebagai objek — satu di `androidx.compose.ui.platform` (versi lama, dari Compose UI 1.6.x yang
project ini pakai) yang SUDAH terisi benar oleh `setContent()`, dan satu lagi di
`androidx.lifecycle.compose` (versi baru, dipakai internal oleh `collectAsStateWithLifecycle()`)
yang TIDAK otomatis kebridge dari yang lama di Compose UI 1.6.x — apapun versi
`lifecycle-runtime-compose`-nya. Fix definitif: bungkus seluruh konten `setContent {}` di
MainActivity dengan `CompositionLocalProvider` yang secara eksplisit menyediakan
`androidx.lifecycle.compose.LocalLifecycleOwner` dari nilai
`androidx.compose.ui.platform.LocalLifecycleOwner.current` — sekali di titik terluar, otomatis
berlaku ke seluruh pohon composable di bawahnya (termasuk 20+ titik `collectAsStateWithLifecycle`
lain). Bump lifecycle 2.8.2 dari Batch 23 tetap dipertahankan (tidak merugikan), tapi fix
sebenarnya tidak lagi bergantung padanya. **Pelajaran ada di bagian Riwayat Insiden di
bawah.** `versionName` masih `3.8`.

**Batch 23** — Root cause crash yang bikin app "terus berhenti" sejak Batch 20 akhirnya
ditemukan lewat crash log dari fitur Batch 22: `java.lang.IllegalStateException:
CompositionLocal LocalLifecycleOwner not present`, dilempar dari `collectAsStateWithLifecycle()`
di baris paling awal `setContent {}` MainActivity, setiap kali app dibuka. Ini **bug resmi
upstream Google** di `lifecycle-runtime-compose:2.8.1` saat dipasangkan dengan Compose UI
1.6.x (yang dipakai `compose-bom:2024.05.00` di project ini) — bukan bug di kode kita. Sudah
resmi diperbaiki Google di versi **2.8.2**. Fix: bump ketiga dependency `androidx.lifecycle:*`
dari `2.8.1` ke `2.8.2` di `app/build.gradle.kts`. **Pelajaran: crash "LocalLifecycleOwner not
present" setelah menambah `collectAsStateWithLifecycle()` = cek versi `lifecycle-runtime-compose`
dulu vs versi Compose BOM, jangan langsung curiga ke kode sendiri — ini kombinasi versi yang
memang pernah rusak resmi di rilis Google.** `versionName` masih `3.8`.

**Batch 22** — Fitur baru: crash logger ke folder publik. Saat crash fatal, `AppLogger`
sekarang juga menulis salinan stack trace ke `Documents/AudioPlayer/logs/crash_<waktu>.txt`
lewat MediaStore (API 29+, tanpa izin storage tambahan) — supaya bisa diambil pakai File
Manager biasa tanpa ADB/root, khusus untuk kasus app tidak bisa dibuka sama sekali. Log
diagnostik privat yang lama (`Settings → Lanjutan`) tidak diubah, tetap jalan seperti biasa.
`versionName` masih `3.8`.

**Batch 21** — Hotfix build gagal dari Batch 20, 2 root cause terpisah ditemukan lewat 2 kali
log CI: (1) `app/compose_stability_config.conf` pakai komentar `#`, parser
`stabilityConfigurationPath` cuma mengenali `//` — baris `#` dibaca sebagai pattern tidak
valid; (2) `LibraryScreen.kt` baris 115, operator `selectedIds - id` / `selectedIds + id`
resolve ke `kotlin.collections.Set` bawaan (bukan versi `kotlinx.collections.immutable`) karena
tidak ada import operator yang tepat, hasilnya `Set<Long>` bukan `PersistentSet<Long>` yang
diharapkan — diganti ke method `.remove(id)`/`.add(id)` bawaan `PersistentSet` (lebih aman,
tidak tergantung import operator). Tidak ada perubahan behavior/fitur. `versionName` masih
`3.8`.

