# PROJECT_STATE.md

[BRANDING_NAME: SONIX]
[TERMUX_ROOT: audioplayer]

RAM instan sesi kerja — hanya rule AKTIF final. Tanpa histori revisi, kutipan user, atau
kronologi batch. Histori lengkap tiap batch: `CHANGELOG.md`. Ringkasan fitur: `README.md`.
Arsip batch lama (1-424): `docs/archive/PROJECT_STATE_ARCHIVE.md`.

## ✅ STATUS PROYEK: ACTIVE
Banner DISCONTINUED dicabut eksplisit oleh user (Batch 432). Proyek lanjut normal, sektor dibuka
per instruksi eksplisit user seperti biasa (lihat "Sektor DITUTUP" di bawah untuk yang masih
butuh reopen spesifik).

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

## Aturan sesi aktif
1. Dilarang edit manual `versionCode`/`versionName` di `app/build.gradle.kts` — auto dari jumlah
   commit git. Tiap kirim ZIP wajib sebut nomor batch + ingatkan versionName pasti baru setelah
   `git push`.
2. Box code pesan commit WAJIB tampil di atas heading "Update Harian" tiap respons chat, isi
   penjelasan fitur singkat dari `CHANGELOG.md` batch itu — dilarang angka versi polos saja.
3. Prioritas versi/dependency/komponen paling mutakhir, bukan kompatibilitas OS lama — user tidak
   peduli dukungan Android <12/API 31. Jangan bikin/pertahankan fallback legacy kalau ada opsi
   modern lebih bersih. `minSdk` tidak pernah diubah otomatis — WAJIB konfirmasi eksplisit user.
4. `docs/archive/ARCHIVED_POLISH_AUDIT.md` / `docs/archive/ARCHIVED_MICRO_UIUX_AUDIT.md` = arsip, tidak aktif diikuti.
   `docs/archive/ROADMAP_LIQUID_GLASS_REDESIGN.md` = 100% tuntas, tidak ada item terbuka.
5. Nama folder Termux: `~/projects/audioplayer` (lowercase) — FINAL. `rootProject.name` tetap
   `"AudioPlayer"` (hardcoded `settings.gradle.kts`), tidak terikat nama folder/`git remote`.
6. Sektor DITUTUP — jangan proaktif dibuka ulang pada instruksi generik ("next"/"lanjut"); BOLEH
   dieksekusi kalau user beri instruksi eksplisit spesifik minta sektor ini dibuka lagi:
   - **Thread Safety I/O** — Batch 431: audit pola-blocking menyeluruh selesai untuk 70 file di
     luar `ui/` (KECUALI paket `update/`, waktu itu belum disentuh). Batch 432: paket `update/`
     (3 file) disisir — 1 gap ditemukan+fix (`UpdateManager.kt`, Thread→Coroutines), 2 file
     lainnya (`GitHubReleaseChecker.kt`/`UpdateDownloader.kt`) konfirmasi sudah benar. 0 item
     residual diketahui KECUALI pola di luar 9 kategori grep Batch 431 (mis. Room DAO
     non-suspend, kalau ada — belum dicek eksplisit).
   - **compileSdk/targetSdk** — final di targetSdk 36, compileSdk 36. 0 rencana Play Store,
     device user Android 16 (edge-to-edge/predictive back terverifikasi device asli). 0 item
     residual kecuali user eksplisit minta bump API 37 (blocked di migrasi AGP 9.x) atau ada
     temuan baru.
   - **Compose optimization** — `AlbumArt` TUNTAS Batch 429 (`SubcomposeAsyncImage` →
     `AsyncImage`), device asli konfirmasi render normal 0 regresi (Batch 430).
     `DuplicateFinderSheet.kt` `remember` CPU-heavy TUNTAS Batch 431 (pindah `LaunchedEffect` +
     `Dispatchers.Default`), belum diverifikasi device/CI. 0 utang teknis residual lain diketahui
     di sektor ini.

## Keputusan arsitektur utama
Ringkasan penuh + alasan: README.md § "Keputusan Arsitektur". Poin paling kritis:
- `PlaybackService` pakai `MediaLibraryService`, **bukan** `MediaSessionService` — prasyarat
  Playback Resumption resmi.
- `AppLogger` lokal murni (bukan Crashlytics/Sentry) — app tidak punya izin INTERNET sama
  sekali, bagian dari klaim privasinya.
- `PinLockoutPolicy` dipisah dari `AppLockStore` supaya bisa di-unit-test tanpa Context.
- File paling berisiko diubah tanpa cek dokumentasi dulu: `PlaybackService.kt`,
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
`SONIX_vN.zip` melacak nomor batch percakapan (bukan versionName/versionCode) — diperbarui Batch
446 dari `AudioPlayer-batchN-release.zip` lama (branding aktif = SONIX, lihat `app_name`/README;
`AudioPlayer` cuma nama teknis package/rootProject, SENGAJA tetap, lihat "Aturan sesi aktif" #5).
`versionCode`/`versionName` otomatis dari jumlah commit git — TIDAK terkait, TIDAK ikut berubah.
Detail lengkap: README.md § "Standar Penomoran Versi".

[RESUME POINT]
- Batch terakhir: 469. ZIP terakhir: `SONIX_v469.zip`. **1 file diubah** (dalam batas 3
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
