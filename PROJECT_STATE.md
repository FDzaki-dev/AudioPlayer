# PROJECT_STATE.md

[BRANDING_NAME: SONIX]
[TERMUX_ROOT: audioplayer]

RAM instan sesi kerja — hanya rule AKTIF final. Tanpa histori revisi, kutipan user, atau
kronologi batch. Histori lengkap tiap batch: `CHANGELOG.md`. Ringkasan fitur: `README.md`.
Arsip batch lama: `docs/archive/PROJECT_STATE_ARCHIVE.md` (Batch <=219 + Batch 425-503 [dipindah Batch 523]; Batch 220-424 TIDAK ditemukan di arsip — hanya di `CHANGELOG.md`).

## ✅ STATUS PROYEK: ACTIVE
Banner DISCONTINUED dicabut eksplisit oleh user (Batch 432). Proyek lanjut normal, sektor dibuka
per instruksi eksplisit user seperti biasa (lihat "Sektor DITUTUP" di bawah untuk yang masih
butuh reopen spesifik).

**Catatan Batch 534 [user: "ci hijau. lanjut opsi C -> D!!" atas opsi Batch 533; CI hijau = laporan user, tidak diverifikasi di sini]**: **5 file source**: C (`ConstantLocale`) — `data/BackupManager.kt:42`, `util/AppLogger.kt:49-50` `Locale.getDefault()` -> `Locale.ROOT`; D tahap 1 (`AutoboxingStateCreation`, 5 situs Float) — `ui/AlbumArtHero.kt:79`, `ui/PlaylistScreen.kt:107-108`, `ui/QueueSheet.kt:79-80` -> `mutableFloatStateOf(0f)` (+ import di `QueueSheet.kt`). D = 9 file -> dibagi 3 tahap (batas 5 file/batch). **NOT VERIFIED** (sandbox tanpa Gradle/kotlinc): kompilasi + CI belum jalan. Prediksi OFFLINE: detekt 0, lint 13 (Autoboxing 10, ConfigScreen 3, ConstantLocale 0). Detail: `CHANGELOG.md` Batch 534.

**Catatan Batch 533 [user: "eksekusi yang kamu rekomendasi kan!!" atas opsi Batch 532 -> A + B]**: **5 file target** — config: `config/detekt/detekt.yml`, `app/build.gradle.kts` (whitelist lint 10 -> 8 ID); source: `RingtoneEncoder.kt:130`, `LockScreen.kt:138`, `EqualizerSheet.kt:190` (3 temuan lint nyata) + docs.
detekt: `SwallowedException` + `SpreadOperator` dimatikan; lint: `Recycle` + `StartActivityAndCollapseDeprecated` keluar (false positive, dibaca di source). **NOT VERIFIED** (sandbox tanpa Gradle/kotlinc): kompilasi + CI belum jalan. Prediksi OFFLINE: detekt 0, lint 21 (Autoboxing 15, ConfigScreen 3, ConstantLocale 3). Detail: `CHANGELOG.md` Batch 533.

**Catatan Batch 532 [user: upload `ci_report_508.zip` tanpa teks (hasil push Batch 531)]**: **2 file dokumen** (`PROJECT_STATE.md`, `CHANGELOG.md`); 0 kode/Gradle/CI/config. Run #508 (commit `cf2d089`): `build` + `instrumentation` (7 tes, 0 gagal) + **`static-analysis` = success** -> Batch 531 TERVERIFIKASI di CI.
detekt 9 temuan (SwallowedException 8, SpreadOperator 1) dan lint 35 issue (3 error/17 warning/15 hint) COCOK PERSIS dengan prediksi offline Batch 531; `NewApi`/`MissingPermission` = 0. Nomor baris laporan = ZIP (9/9 dicek). Belum ada temuan yang diperbaiki. Detail + daftar lokasi: `CHANGELOG.md` Batch 532.

**Catatan Batch 531 [user: "ubah rencana: 1) lintdebug/detekt difokuskan hanya untuk yang benar-benar bisa diamati dan diperbaiki, jangan masukkan yang tidak perlu!! [non blocking]" + upload `SONIX_v530.zip`]**: **4 file target diubah** (`config/detekt/detekt.yml`,
`app/build.gradle.kts`, `.github/workflows/build.yml` [komentar saja], `.githooks/pre-commit`) + **dihapus** `.github/workflows/baseline.yml`, `app/detekt-baseline.xml`, `app/lint-baseline.xml`; 0 source Kotlin. Rencana baseline A/B/C (Batch 528-530) DIBATALKAN.
detekt = hanya potential-bugs/exceptions/empty-blocks/performance/coroutines (tanpa ktlint/complexity/style/naming/comments; `ignoreFailures = true`, `autoCorrect = false`); lint = whitelist `checkOnly` 10 ID, `abortOnError`/`warningsAsErrors` = false; tanpa baseline.
**NOT VERIFIED** (sandbox tanpa Gradle/SDK): config baru belum dijalankan. Prediksi OFFLINE: detekt ~9 temuan, lint ~35. Detail: `CHANGELOG.md` Batch 531.

**Catatan Batch 530 [user: upload `baseline_1.zip` + `ci_report_507.zip` tanpa teks (setelah menjalankan "Generate Baselines")]**: **2 file baseline baru** (`app/detekt-baseline.xml` 1189 ID, `app/lint-baseline.xml` 194 issue; byte-identik dari artifact) + docs; 0 kode/Gradle/CI.
`baseline.yml` (Batch 528) TERBUKTI jalan: `detektBaseline` + `lintDebug` BUILD SUCCESSFUL, artifact terbentuk (run #1, commit `a02d5ac`). `ci_report_507` = commit SAMA `a02d5ac` (dibuat SEBELUM baseline ada): build + instrumentation sukses, static-analysis merah (detekt 4288/97 file, lint 194).
**PREDIKSI OFFLINE (bukan hasil CI)**: lint 194/194 tertutup baseline; detekt MASIH merah — 345 dari 4288 temuan #507 (150 ID) tidak ada di baseline (format: ArgumentListWrapping, Wrapping, MaxLineLength, Indentation, ...; + 1 `LongMethod` `PlaybackService.onCreate`).
Temuan antar-run goyang pada source sama (#506 vs #507: 19 vs 23 ID beda). Dugaan penyebab (BELUM terbukti): baseline dibuat `autoCorrect=false`, gate jalan `autoCorrect: true`. Threshold TIDAK dilonggarkan. Detail: `CHANGELOG.md` Batch 530.

**Catatan Batch 529 [user: "lanjutkan progress!!" + upload `SONIX_v528.zip` & `ci_report_506.zip`]**: **2 file dokumen**, 0 kode. Artifact `baseline_<run>` BELUM diunggah — `ci_report_506` = laporan `build.yml` biasa, bukan keluaran
"Generate Baselines"; langkah itu tetap tindakan user. Run #506 (commit `914cac1`): `build` sukses, `instrumentation` sukses (7 tes, 0 gagal), `static-analysis` merah (diharapkan; detekt 4281/97 file, lint 194 = 178 error + 16 hint).
4 kandidat lint dicek ke source (0 diubah): 3 bukan cacat nyata/disengaja; `RingtoneEncoder.kt:130` WrongConstant = satu-satunya yang nyata. Nomor baris laporan != ZIP (dugaan `autoCorrect`, belum terbukti). Detail: `CHANGELOG.md` Batch 529.

**Catatan Batch 528 [user atas v527: "A: baseline dibuat lewat gradle detektBaseline dan baseline lint dari toolchain asli, bukan ditulis tangan."]**: **1 file CI baru** `.github/workflows/baseline.yml`
(+ docs); 0 kode, `build.yml` tidak berubah. Workflow manual TERPISAH "Generate Baselines": init script Gradle mengarahkan baseline ke `app/detekt-baseline.xml` + `app/lint-baseline.xml`, jalankan `gradle detektBaseline`
(autoCorrect dimatikan khusus job ini) + `gradle lintDebug`, upload 1 artifact `baseline_<run>` (+ `INDEX.txt` + log). Baseline BELUM ada/BELUM dijalankan; threshold TIDAK dilonggarkan. **NOT VERIFIED** — YAML/`bash -n`/simulasi
lokal OK; init script Groovy belum dikompilasi/dijalankan (sandbox tanpa Gradle/SDK). Detail: `CHANGELOG.md` Batch 528.

**Catatan Batch 527 [user: upload `ci_report_504.zip` tanpa teks]**: **2 file dokumen**, 0 kode. Run #504 (commit `f7d70c5`): `build` sukses, `instrumentation-tests` sukses (7/7), `static-analysis` merah
(diharapkan). `ci_report_504` terbentuk -> Batch 526 terverifikasi sebagian (hapus artifact terpisah + izin `actions: write` BELUM terkonfirmasi). Backlog: detekt 4300 temuan/97 file (~84% aturan format),
lint 194 issue (178 error + 16 hint; 28 = "versi lebih baru"). Temuan format bergeser antar-run pada source sama (503: 4279 vs 504: 4300; sebab belum diketahui) dan ID baseline detekt kasar
(4300 temuan = 1216 ID; Indentation 2589 = 23 ID). `BubbleTileService.kt:80` = false positive (sudah ada guard API 34+). Backlog BELUM ditindak, menunggu keputusan user. Detail: `CHANGELOG.md` Batch 527.

**Catatan Batch 526 [user atas v525 + `static_analysis_report_503.zip`: "tolong juga untuk semua yang berhubungan dengan backlog/log failure dikompilasi jadi 1 kesatuan biar gak bikin unduhan menumpuk!!"]**:
**1 file CI** (`.github/workflows/build.yml`), 0 source Kotlin, 0 file Gradle. Job baru `collect-reports` (jalan setelah `build`/`instrumentation-tests`/`static-analysis`, apa pun hasilnya) menggabungkan
`log_fail_<run>` + `instrumentation_test_report_<run>` + `static_analysis_report_<run>` jadi 1 artifact `ci_report_<run>` (`INDEX.txt` + 1 folder per laporan), lalu menghapus artifact terpisahnya
(hanya kalau `ci_report_<run>` sudah terdaftar). `static-analysis` kini `--continue` (run #503 tidak sempat menjalankan lintDebug). Laporan 503: 4279 temuan detekt / 97 file (BELUM ditindak).
Jalur rilis `build` tidak berubah. **NOT VERIFIED** — YAML/`bash -n`/simulasi lokal skrip INDEX OK; download/upload/hapus artifact + izin `actions: write` belum dijalankan; CI BELUM. Detail: `CHANGELOG.md` Batch 526.

**Catatan Batch 525 [user: "terapkan konfigurasi lintdebug/detect yang ketat sesuai standar konstitusi!!"]**: **5 file target, 0 source Kotlin app diubah** — detekt 1.23.8 (stabil;
2.0.0 masih alpha) + lintDebug `abortOnError`/`warningsAsErrors`; `config/detekt/detekt.yml` (maxIssues 0, autoCorrect, complexity ketat), job CI terpisah `static-analysis`, hook
`.githooks/pre-commit` OPT-IN (skip kalau tanpa Gradle/SDK). `checkReleaseBuilds=false` tetap -> jalur rilis utuh. `build.yml` kini ikut ZIP (menimpa salinan device).
Tanpa baseline -> run pertama `static-analysis` kemungkinan merah karena temuan lama. **NOT VERIFIED** — 0 build/detekt/lint dijalankan (sandbox tanpa Gradle/SDK/jaringan). Detail: `CHANGELOG.md` Batch 525.

**Catatan Batch 524 [user atas v523: "fokus kerjakan next kandidat low-risk!!"]**: satu-satunya sisa kandidat R1 = **T3** (Wave 1). **2 file source**:
`data/PlaybackStateStore.kt` (1 call) + `data/LibraryCacheStore.kt` (2 call): `Log.w(TAG, msg, e)` -> `AppLogger.e(TAG, msg, e)` + tukar import (`android.util.Log` ->
`util.AppLogger`); 0 `Log.` tersisa di kedua file. Dipilih `.e` (bukan `.w`) karena `AppLogger.w` tak punya parameter Throwable -> stack trace akan hilang; `AppLogger.kt`
TIDAK diubah. Efek: kegagalan load/simpan yang tadinya hanya di logcat kini masuk Log Diagnostik (label ERROR); jalur normal `return null` tanpa exception tidak ikut log.
**NOT VERIFIED** — 0 build/test (sandbox tanpa Gradle); CI + device BELUM. Detail: `CHANGELOG.md` Batch 524.

**Catatan Batch 523 [user atas v522: "another zero diff, berikan saya opsi pilihan untuk kesinambungan milestone!!" -> dipilih D1/D2/D3; T11 tidak dipilih]**:
v522 (T10 eksekusi ulang) dianggap nol regresi (rincian CI vs device tidak dirinci) -> **Wave 2 T4-T10 selesai**. **0 file source diubah** (`diff -r app/`
vs `SONIX_v522.zip` = IDENTIK); Wave 4 = dokumen saja. **D1**: `PROJECT_STATE.md` 3148 -> 461 baris (257 -> 44 KB): Catatan Batch 425-503,
daftar "belum-terverifikasi" Batch 431-438, dan entri `[RESUME POINT]` Batch <=502 dipindah VERBATIM ke arsip (dicek programatik: baris keluar == baris masuk);
batch 504-522 tetap (batas rencana = <=503); 4 item terbuka warisan + 4 pelajaran proses dipertahankan di bawah. **D2**: `FILE_MANIFEST.txt` dicocokkan ke isi ZIP nyata —
204 file + 2 dotfile sengaja (`.github/workflows/build.yml`, `.gitignore`) = 206, 0 drift, 0 path berubah. **D3**: pointer 23 rujukan komentar source ke 3 dokumen
`PENDING_*.md` yatim di `docs/PLANNING.md` §5 (dihapus sengaja saat tuntas, Batch 353/360/380); 0 komentar diedit. **NOT VERIFIED** — 0 build/test (0 source berubah);
status CI/device T10 (v522) hanya laporan user, tidak diverifikasi di sini. Detail: `CHANGELOG.md` Batch 523.

**Catatan Batch 522 [user atas v521: "itu mungkin hanya masalah hardware device saya yang over heat. lanjutkan milestone!!"]**: user menduga
regresi performa v520 = HP overheat (dugaan user, TIDAK terverifikasi; belum ada pengukuran suhu-setara) -> penahanan T10 (Batch 521) dicabut,
**T10 dieksekusi ulang**. **2 file source** (`MainActivity.kt` 2272->1873 baris + baru `BottomNavBar.kt`, paket ROOT): 4 simbol bottom nav dipindah
MOVE-ONLY byte-identik (diff = 2 baris `private`->`internal` pada `GlassTabIcon` + `CustomNavBarTabItem`; hasil `MainActivity.kt` identik dengan
v520; rekonstruksi == v519 asli). `AppNavHost` TIDAK dipecah. **NOT VERIFIED** — 0 build/test; CI + device BELUM. Detail: `CHANGELOG.md` Batch 522.

**Catatan Batch 521 [user atas v520: "regression nyata Kali ini cuman performance yang downgrade!!"]**: v520 (T10, pemindahan bottom nav ke
`BottomNavBar.kt`) dilaporkan regresi PERFORMA nyata (gejala spesifik tidak dirinci; hal lain nol regresi). **T10 DITARIK KEMBALI** —
2 file source dikembalikan (`MainActivity.kt` 1873->2272 baris, `BottomNavBar.kt` dihapus); `diff -r app/` == `SONIX_v519.zip` (0 beda). **Wave 2
DIHENTIKAN** (plan §5.6). Akar masalah **BELUM diketahui**: diff v519->v520 byte-identik selain `private`->`internal`, analisis statis
0 mekanisme jelas (0 baseline profile, 0 rujukan nama class) — hipotesis: efek build R8/DEX, atau instal-baru tanpa baseline profile
(kode terkompilasi ART ter-reset). v521 = eksperimen pembeda (kode = v519). **NOT VERIFIED** — 0 build/test; CI + device BELUM.
Detail: `CHANGELOG.md` Batch 521.

**Catatan Batch 520 [user atas v519: "another zero diff, lanjut T10!!"]**: T9 dianggap nol regresi (rincian CI vs device tidak dirinci; user
sempat menyatakan jalur onboarding sulit diuji — dijawab: layar muncul tiap launch tanpa izin audio, bukan cuma install pertama) -> T10
dikerjakan (R3). **2 file source** (`MainActivity.kt` 2272->1873 baris + baru `BottomNavBar.kt`, paket ROOT `com.rudi.audioplayer`):
`MagnifyingTabLabel`/`GlassTabIcon`/`NoRippleIndication`/`CustomNavBarTabItem` dipindah MOVE-ONLY byte-identik (diff = 2 baris
`private`->`internal` pada `GlassTabIcon` + `CustomNavBarTabItem`; 0 perilaku berubah; rekonstruksi programatik == `MainActivity.kt` v519
asli). `AppNavHost` TIDAK dipecah. `NoRippleIndication` = kode mati (0 pemakai), dipindah apa adanya. **NOT VERIFIED** — 0 build/test;
CI + device BELUM. Detail: `CHANGELOG.md` Batch 520.

**Catatan Batch 519 [user atas v518: "v518 tetap zero diff. lanjutkan milestone!!"]**: T8b dianggap nol regresi (rincian CI vs device tidak
dirinci) -> T9 dikerjakan. **2 file source** (`MainActivity.kt` 2414->2272 baris + baru `OnboardingScreens.kt`, paket ROOT
`com.rudi.audioplayer`): `WelcomeScreen`/`WelcomeHighlight`/`PermissionRationale` dipindah MOVE-ONLY (diff = 2 baris `private`->`internal`,
`WelcomeHighlight` tetap private; 0 perilaku berubah; rekonstruksi programatik == `MainActivity.kt` v518 asli). **NOT VERIFIED** — 0
build/test; CI + device BELUM. Detail: `CHANGELOG.md` Batch 519.

**Catatan Batch 518 [user atas v517: "v517 zero diff. lanjut"]**: T8a dianggap nol regresi (rincian CI vs device tidak dirinci) -> T8b
dikerjakan. **2 file source** (`LibraryScreen.kt` 970->670 baris + baru `LibrarySearch.kt`): `LibraryHeader`/`LibrarySearchField`/
`LibraryFilterChips`/`SearchHistoryView`/`SearchSectionLabel`/`SearchResultsView` + 2 `val` label tab dipindah MOVE-ONLY (diff = 5 baris
`private`->`internal`, 0 perilaku berubah). **NOT VERIFIED** — 0 build/test; CI + device BELUM. Detail: `CHANGELOG.md` Batch 518.

**Catatan Batch 517 [user atas v516: "baik, 2 bug telah teratasi. lanjutkan milestone!!"]**: 2 bug v516 dianggap beres user (rincian CI vs
device tidak dirinci) -> penahanan T8a dicabut. **2 file source** (`LibraryScreen.kt` 1460->970 baris + baru `LibraryLists.kt`): `AlbumGridView`/
`SongListView`/`GroupedListView`/`SongRow` dipindah MOVE-ONLY (diff = 4 baris `private`->`internal`, 0 perilaku berubah; fix Batch 516 ikut
terpindah utuh). **NOT VERIFIED** — 0 build/test; CI + device BELUM. Detail: `CHANGELOG.md` Batch 517.

**Catatan Batch 516 [user atas v515: "semua aman terkendali, except" 2 bug]**: T7 (Settings) dinilai aman (tentatif). **3 file source**:
(1) prompt sidik jari otomatis muncul berulang (`MainActivity.kt`, `LaunchedEffect` di-key `hasWindowFocus` yg ikut toggle saat dialog
ditutup) -> penanda `biometricAutoPrompted`, 1x per sesi kunci, reset di `onStop()`; (2a) shimmer = refresh otomatis `ContentObserver`
non-silent menutup list (`PlayerViewModel.kt`) -> `silent` bila list sudah berisi; (2b) sweep-select "gak ikut jari" =
`userScrollEnabled = sweepAnchorIndex == null` di `SongListView` (`LibraryScreen.kt`) — **DUGAAN, belum terbukti**. **NOT VERIFIED** — 0
build/test; CI + device BELUM. Detail: `CHANGELOG.md` Batch 516.

**Catatan Batch 515 [user: "haptic feedback udah bagus, lanjut kerjakan milestone yang tertunda"]**: Haptic dianggap beres user ->
penahanan Wave 2 dicabut; milestone tertunda = T7. **2 file source** (`SettingsScreen.kt` 951->642 baris + baru
`SettingsSections.kt`): `ThemeModeToggleSection`/`ThemeOptionCard`/`AppLockSection`/`SetPinDialog` dipindah MOVE-ONLY (diff = 4
baris `private`->`internal`, 0 perilaku berubah). **NOT VERIFIED** — 0 build/test; CI + device BELUM. Detail: `CHANGELOG.md` Batch 515.

**Catatan Batch 514 [laporan user + 2 screenshot: "root cause: lupa aktifkan haptic feedback ... sekarang getarannya cuma
setara Gboard, geli geli kuku"]**: Akar masalah "tidak terasa" TERKONFIRMASI user = opsi Umpan Balik Haptic sistem HP
(Infinix X-Haptics) belum aktif (kini aktif, slider Tinggi), BUKAN bug kode wrapper. Masalah baru = terlalu lemah: efek
prabuat CLICK/HEAVY_CLICK (dipilih Batch 511-513 krn `hardware-effects` YES) kekuatannya diatur pabrikan. **1 file source
diubah** (`AppHaptics.kt`): `hasAmplitudeControl()` -> one-shot amplitudo 255 (50/100ms, TEBAKAN, 2 konstanta); tanpa
kontrol amplitudo -> perilaku lama. Kategori MEDIA Batch 513 dipertahankan. **NOT VERIFIED** — 0 build/test; kekuatan
riil hanya bisa dinilai user di device. Detail: `CHANGELOG.md` Batch 514.

**Catatan Batch 513 [laporan user atas v512: "masih sama aja!!" + Log Diagnostik]**: Log terbaca (Infinix X6850, sdk 36):
`mode=hardware-effects` (CLICK/HEAVY=1/1), `getar_sentuh=0`, 6 panggilan `vibrate() dikirim tanpa exception`. Wrapper
terpanggil (gugur: "tak terpanggil"), konstanta 50/100ms tak terpakai di HP ini. Dugaan kuat: `vibrate(effect)` tanpa
attributes dianggap USAGE_TOUCH oleh framework -> dibuang saat getar sentuh sistem mati. **1 file source diubah**
(`AppHaptics.kt`): API 33+ pakai `VibrationAttributes.USAGE_MEDIA`. Usage ALARM/ACCESSIBILITY/flag bypass SENGAJA tidak
dipakai. **NOT VERIFIED** — 0 build/test; dugaan akar masalah dari sumber sekunder, BELUM dibuktikan di HP. Detail:
`CHANGELOG.md` Batch 513.

**Catatan Batch 512 [laporan user atas v511: "gak merasakan ada nya perbaikan haptic feedback nyata selain dari
getaran musik yang dimainkan!!"]**: Akar masalah BELUM terbukti — wiring benar secara statis (1 root `setContent`
di bawah `AudioPlayerTheme`, 75 call site via `LocalHapticFeedback`, `minSdk` 31), sisa 3 kandidat: denyut
terlalu pendek utk motor lemah / wrapper tak terpanggil / sistem HP mengabaikan `vibrate()`. **1 file source
diubah** (`AppHaptics.kt`): denyut one-shot 30/55 -> 50/100ms (TEBAKAN), baris status Log Diagnostik diperkaya
(info HP + setelan getar sistem), 6 panggilan haptic pertama dicatat, bug v511 (kegagalan `vibrate()` tak pernah
tercatat) diperbaiki. Ganti usage/atribut getar SENGAJA ditunda sampai ada data log. **NOT VERIFIED** — 0
build/test sesi ini; CI + device BELUM. Detail: `CHANGELOG.md` Batch 512.

## Item terbuka warisan (dari daftar "belum-terverifikasi" Batch <=503 — daftar lengkap ada di arsip)
- `docs/archive/MANUAL_QA_CHECKLIST.md` — 0/19 item tercentang (audio focus, Bluetooth, lock-screen,
  headset kabel, process death, background playback jangka panjang).
- Overscroll bounce (`IosScrollPhysics.kt`, `Spring.DampingRatioNoBouncy`) belum dikonfirmasi
  device asli.
- **BELUM dikonfirmasi (Batch 452, masih berlaku)**: (1) label 3 tab 0 lagi ellipsis di font
  normal; (2) drag flick cepat + tap-tab biasa berhenti TEPAT di tab tujuan, 0 "mundur 1 kolom".
- **BELUM dikonfirmasi (lama)**: regresi warna ikon/label tema Skeu (video user Batch 451 pakai
  tema default/gelap, bukan Skeu). TalkBack tidak bisa dicek dari rekaman visual.

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
   `docs/QA_CHECKLIST_SONIX_v488.md` (Batch 489) BUKAN arsip — checklist QA eksternal AKTIF.
   Gap previous-3-detik: percobaan Batch 492 GAGAL CI (revert Batch 493), TAPI bump media3
   1.10.1 (Batch 494) + re-add setter (Batch 495) + fix bug lanjutan (Batch 496) **TUNTAS &
   dikonfirmasi device user Batch 497** [koreksi stale Batch 503 — baris ini sempat salah
   mengklaim "masih butuh bump"]. Gap #3 (shuffle anti-repeat-nearby) TUNTAS Batch 498/499/500.
   Gap #6 (filter audio pendek) TUNTAS Batch 501/502. **0 gap kode tersisa** dari roadmap Gap QA
   v488; #4/#5/#7/#8/#9 murni device-QA. Pindahkan ke `docs/archive/` HANYA setelah device-QA
   #4/#5/#7/#8/#9 dikonfirmasi user, ATAU user instruksikan eksplisit tutup sisanya.
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

7. Pelajaran proses (MASIH BERLAKU — dipertahankan dari entri Batch <=503 yang diarsipkan; konteks asli di arsip):
   - (Batch 450) Modifier layout yang meniru API resmi WAJIB diverifikasi ke source/dokumentasi asli dulu, jangan diasumsikan.
   - (Batch 461-463) JANGAN ulangi pola "ganti API/sumber baca metrics lagi" tanpa bukti (3x terbukti bukan akar masalah). Logcat generik saja TERBUKTI TIDAK
     CUKUP — WAJIB ada instrumentasi eksplisit di kode DULU; fix berikutnya diputuskan dari data log, bukan teori baru.
   - (Batch 456-458) Permintaan ubah angka "timbul"/persentase tab minimized tanpa kata eksplisit "sembunyi/klip" vs "timbul/kelihatan" -> WAJIB klarifikasi
     arah dulu, jangan tebak ("timbul" = bagian KELIHATAN tab, bukan fraksi klip `EDGE_CLIP_FRACTION`).
   - (Batch 503) Entri `[RESUME POINT]` WAJIB di-cross-check ke catatan batch detail + source, bukan ditelan mentah (pernah basi).
8. Static analysis (Batch 531, instruksi eksplisit user) = FOKUS & NON-BLOCKING: detekt/lintDebug hanya untuk temuan yang bisa diamati + diperbaiki, tidak memblokir apa pun (CI maupun hook), tanpa baseline, `autoCorrect` mati. Whitelist ada di
   `config/detekt/detekt.yml` + `lint { checkOnly }` (`app/build.gradle.kts`); menambah rule = harus bisa dijelaskan "terlihat di laporan DAN punya perbaikan lokal". Mengembalikan gate ketat butuh instruksi eksplisit user.

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
- Batch terakhir: 534. ZIP: `SONIX_v534.zip`. **5 file source** (`data/BackupManager.kt`, `util/AppLogger.kt`, `ui/AlbumArtHero.kt`, `ui/PlaylistScreen.kt`, `ui/QueueSheet.kt`) + docs (`PROJECT_STATE.md`, `CHANGELOG.md`, `README.md`); `FILE_MANIFEST.txt` tak berubah (0 path baru/hilang). **[RESUME POINT: FITUR_BARU / BUG_TARGET -> STATUS TERAKHIR -> LANGKAH SPESIFIK AKUN BERIKUTNYA]**:
  Opsi C (`ConstantLocale` 3 situs) + D tahap 1 (`AutoboxingStateCreation` 5 situs Float, 3 file) -> SUDAH di ZIP, BELUM dikompilasi/dijalankan di CI ->
  (1) USER: push (Box DAILY UPDATE) lalu unggah `ci_report_<run>` hasil push ini. (2) Akun berikutnya: cek 3 job hijau — `build` merah = periksa HANYA `QueueSheet.kt` (import `mutableFloatStateOf` + baris 79-80), `PlaylistScreen.kt:107-108`, `AlbumArtHero.kt:79`, `BackupManager.kt:42`, `AppLogger.kt:49-50`. Bandingkan dengan prediksi OFFLINE (bukan hasil CI): detekt 0; lint 13 = `AutoboxingStateCreation` 10, `ConfigurationScreenWidthHeight` 3, `ConstantLocale` 0. `ConstantLocale` MASIH muncul / Autoboxing tak turun 5 = telaah pesan lint, jangan tambah suppress tanpa instruksi user.
  (3) LANJUT D (instruksi user "C -> D"): **tahap 2 (3 file, Int -> `mutableIntStateOf`)**: `LibraryScreen.kt` 125/130/211 (`selectedTab`, `filterVersion`, `undoBarKey`), `LockScreen.kt:46` (`remainingSeconds`; baris 45 `Long?` JANGAN diubah), `VaultSheet.kt:91` (`vaultedIdsVersion`). **Tahap 3 (3 file)**: `NowPlayingScreen.kt` 303 Int + 305/325/1530 Float (4 situs; Batch 533 mencatat 3 temuan lint, 305/325 multi-baris), `RingtoneCutterSheet.kt` 49 `0L` + 51 Long -> `mutableLongStateOf`, `SmartPlaylistScreen.kt:250` (`initial?.minRating ?: 0`, Int). Ketiganya `runtime.*` (tanpa tambah import); semua `var ... by`, jangan ubah state non-primitif/nullable/Boolean/String. E (`ConfigurationScreenWidthHeight` 3) tetap tidak disarankan.
  (4) JANGAN menyalakan lagi `Recycle`/`SwallowedException`/`SpreadOperator`/`StartActivityAndCollapseDeprecated`, ktlint-formatting/complexity/style, baseline, `abortOnError`/`ignoreFailures=false`, atau `autoCorrect` tanpa instruksi eksplisit user (Aturan sesi #8). Masih terbuka dari Batch 532: artifact terpisah terhapus? (cek tab Artifacts, user). Gagal parah -> kembali `SONIX_v533.zip` (CI hijau per laporan user).
- Batch 533 (sebelum 534). ZIP: `SONIX_v533.zip`. **5 file target** (`config/detekt/detekt.yml`, `app/build.gradle.kts` [hanya daftar `checkOnly` + komentar], `data/RingtoneEncoder.kt`, `ui/LockScreen.kt`, `ui/EqualizerSheet.kt`) + docs (`PROJECT_STATE.md`, `CHANGELOG.md`, `README.md`); `FILE_MANIFEST.txt` tak berubah (0 path baru/hilang). **[RESUME POINT: FITUR_BARU / BUG_TARGET -> STATUS TERAKHIR -> LANGKAH SPESIFIK AKUN BERIKUTNYA]**:
  Opsi A (pangkas whitelist) + B (3 lint nyata: `RingtoneEncoder.kt` flags WrongConstant, `LockScreen.kt` offset lambda, `EqualizerSheet.kt` locale observable) -> SUDAH di ZIP, BELUM dikompilasi/dijalankan di CI ->
  (1) USER: push (Box DAILY UPDATE) lalu unggah `ci_report_<run>` hasil push ini. (2) Akun berikutnya: cek 3 job hijau — `build` merah = kemungkinan besar salah satu dari 3 file Kotlin batch ini tak terkompilasi: periksa HANYA `RingtoneEncoder.kt` blok `bufferInfo.flags = if (...)`, `LockScreen.kt` `Modifier.offset { IntOffset(...) }` (+ import `IntOffset`), `EqualizerSheet.kt` `val locale = LocalConfiguration.current.locales[0]` (+ import `LocalConfiguration`).
  Bandingkan dengan prediksi OFFLINE (bukan hasil CI): detekt 0; lint 21 = `AutoboxingStateCreation` 15, `ConfigurationScreenWidthHeight` 3, `ConstantLocale` 3. Kalau `WrongConstant`/`NonObservableLocale`/`UseOfNonLambdaOffsetOverload` MASIH muncul = perbaikan belum diterima lint -> telaah pesan lint, jangan tambah suppress tanpa instruksi user.
  (3) Sisa pilihan = KEPUTUSAN USER: C) `ConstantLocale` 3 (`BackupManager.kt:42`, `AppLogger.kt:49-50`, `Locale.ROOT` utk stempel nama file/log; perilaku berubah hanya di locale angka non-Latin); D) `AutoboxingStateCreation` 15 di 9 file (`AlbumArtHero`, `LibraryScreen` x3, `LockScreen`, `NowPlayingScreen` x3, `PlaylistScreen` x2, `QueueSheet` x2, `RingtoneCutterSheet`, `SmartPlaylistScreen`, `VaultSheet`; 2 batch, maks 5 file/batch);
  E) `ConfigurationScreenWidthHeight` 3 (`NowPlayingScreen.kt:277`/`:560`, `WindowAdaptive.kt:37`; risiko regresi tertinggi — tidak disarankan); F) instruksi lain di luar static analysis.
  (4) JANGAN menyalakan lagi `Recycle`/`SwallowedException`/`SpreadOperator`/`StartActivityAndCollapseDeprecated`, ktlint-formatting/complexity/style, baseline, `abortOnError`/`ignoreFailures=false`, atau `autoCorrect` tanpa instruksi eksplisit user (Aturan sesi #8). Masih terbuka dari Batch 532: artifact terpisah terhapus? (cek tab Artifacts, user). Gagal parah -> kembali `SONIX_v532.zip` (perilaku app identik dengan v530/v531).
- Batch 532 (sebelum 533). ZIP: `SONIX_v532.zip`. **2 file dokumen** (`PROJECT_STATE.md`, `CHANGELOG.md`); 0 kode, 0 Gradle, 0 CI, 0 config. **[RESUME POINT: FITUR_BARU / BUG_TARGET -> STATUS TERAKHIR -> LANGKAH SPESIFIK AKUN BERIKUTNYA]**:
  Static analysis fokus & non-blocking (Batch 531) -> TERVERIFIKASI di CI run #508 (3 job hijau; detekt 9, lint 35 = prediksi persis); 0 temuan diperbaiki ->
  (1) Tidak ada langkah wajib. Pilihan berikutnya = KEPUTUSAN USER, jangan pilih sendiri; perbaikan HANYA atas instruksi user, 1 rule/1 file per batch, cari lewat nama simbol (nomor baris laporan = ZIP v531):
  A) `RingtoneEncoder.kt:130` WrongConstant (`bufferInfo.flags = extractor.sampleFlags` -> mapping flag eksplisit; satu-satunya lint nyata yang diketahui);
  B) `SwallowedException` 8 titik (6 file: `PlayerViewModel.kt:1010`, `SignatureMatcherSheet.kt:274`, `ListeningHistoryStore.kt:44`, `LyricsRepository.kt:63`/`:67`, `AudioArtFetcher.kt:71`/`:82`, `LyricsPrefetchWorker.kt:51`) — mayoritas fallback sengaja, perlu dinilai satu per satu;
  C) lint error lain: `EqualizerSheet.kt:190` NonObservableLocale; `BubbleTileService.kt:77` StartActivityAndCollapseDeprecated (cabang `else` API<34, `@Suppress("DEPRECATION")` tak menutup ID lint -> butuh `@SuppressLint`; perilaku tak berubah);
  D) `Recycle` 10 (`BackupManager`, `MusicRepository`, `SignatureMatcherSheet`, `AppLogger`), `ConstantLocale` 3, `ConfigurationScreenWidthHeight` 3, `AutoboxingStateCreation` 15 (hint), `SpreadOperator` 1 (`MainActivity.kt:479`), `UseOfNonLambdaOffsetOverload` 1 (`LockScreen.kt:138`).
  (2) JANGAN menyalakan lagi ktlint-formatting/complexity/style, baseline, `abortOnError`/`ignoreFailures=false`, atau `autoCorrect` tanpa instruksi eksplisit user (Aturan sesi #8). Menambah ID ke `checkOnly` = harus "terlihat di laporan DAN punya perbaikan lokal".
  (3) Masih terbuka: penghapusan artifact terpisah (step "Hapus artifact terpisah" di `collect-reports`) tidak terlihat dari isi laporan — cek tab Artifacts oleh user; commit `cf2d089` = Update Harian atau susulan hapus `baseline.yml` belum bisa dipastikan. Gagal parah -> kembali `SONIX_v531.zip`.
- Batch 531 (sebelum 532). ZIP: `SONIX_v531.zip`. **4 file target diubah** (`config/detekt/detekt.yml`, `app/build.gradle.kts` [blok `lint {}`/`detekt {}` + dependency `detekt-formatting`], `.github/workflows/build.yml` [komentar], `.githooks/pre-commit`) + **3 file dihapus** (`.github/workflows/baseline.yml`, `app/detekt-baseline.xml`, `app/lint-baseline.xml`) + docs (`PROJECT_STATE.md`, `CHANGELOG.md`, `FILE_MANIFEST.txt`, `README.md`); 0 source Kotlin. **[RESUME POINT: FITUR_BARU / BUG_TARGET -> STATUS TERAKHIR -> LANGKAH SPESIFIK AKUN BERIKUTNYA]**:
  Static analysis FOKUS & NON-BLOCKING (rencana baseline A/B/C DIBATALKAN user) -> SUDAH di config, BELUM dijalankan di CI/Gradle ->
  (1) USER: push (Box DAILY UPDATE) lalu unggah `ci_report_<run>` hasil push ini. (2) Akun berikutnya: job `static-analysis` HARUS hijau; merah = tool/config gagal (bukan temuan) -> betulkan HANYA file config terkait: `checkOnly.addAll(...)` di `app/build.gradle.kts` blok `lint {}` (DSL/ID), atau
  `config/detekt/detekt.yml` (nama ruleset/rule; validasi `warningsAsErrors` aktif). Bandingkan dengan prediksi OFFLINE (bukan hasil CI): detekt ~9 (SwallowedException ~8, SpreadOperator ~1); lint ~35 (Recycle 10, AutoboxingStateCreation 15, ConstantLocale 3, ConfigurationScreenWidthHeight 3, WrongConstant 1,
  NonObservableLocale 1, UseOfNonLambdaOffsetOverload 1, StartActivityAndCollapseDeprecated 1; NewApi/MissingPermission 0 = tripwire). (3) Perbaikan temuan = HANYA atas instruksi user, 1 rule/1 file per batch; cari lewat nama simbol/Signature, bukan nomor baris. `RingtoneEncoder.kt:130` WrongConstant (`bufferInfo.flags = extractor.sampleFlags`) = satu-satunya yang diketahui nyata.
  (4) JANGAN menyalakan lagi ktlint-formatting/complexity/style, baseline, `abortOnError`/`ignoreFailures=false`, atau `autoCorrect` tanpa instruksi eksplisit user (lihat Aturan sesi #8). Gagal parah -> kembali `SONIX_v530.zip` (perilaku app identik; gate ketat + baseline).
- Batch 530 (sebelum 531; rencana baseline di entri ini DIGANTIKAN Batch 531, jangan dijalankan). ZIP: `SONIX_v530.zip`. **2 file baseline baru** (`app/detekt-baseline.xml`, `app/lint-baseline.xml`) + docs (`PROJECT_STATE.md`, `CHANGELOG.md`, `FILE_MANIFEST.txt`); 0 kode, 0 Gradle, 0 CI. **[RESUME POINT: FITUR_BARU / BUG_TARGET -> STATUS TERAKHIR -> LANGKAH SPESIFIK AKUN BERIKUTNYA]**:
  Opsi A (baseline detekt+lint) -> BASELINE SUDAH MASUK ZIP (dari toolchain asli), CI BELUM dijalankan atasnya; prediksi OFFLINE dari `ci_report_507`: lint 194/194 tertutup, detekt MASIH merah (345 temuan/150 ID belum tertutup + ~20 ID goyang antar-run) ->
  (1) USER: push (Box DAILY UPDATE) lalu unggah `ci_report_<run>` hasil push ini. (2) Akun berikutnya: cocokkan dengan prediksi — lint harus 0 temuan; detekt: hitung temuan di luar baseline (ID = `Rule:Signature` dari `detekt.txt`); prediksi benar kalau sisanya ~150 ID format
  (ArgumentListWrapping, Wrapping, MaxLineLength, Indentation, ImportOrdering, NoSemicolons, MultiLineIfElse) + 1 `LongMethod` `PlaybackService.kt$PlaybackService$@UnstableApi override fun onCreate()`. Jangan edit baseline tangan; jangan longgarkan threshold.
  (3) Dugaan penyebab (BELUM terbukti): `baseline.yml` sengaja `autoCorrect = false`, gate jalan `autoCorrect: true` -> temuan/signature format beda. Pilihan = KEPUTUSAN USER, jangan pilih sendiri: A) ubah HANYA `baseline.yml` (jangan matikan autoCorrect) + jalankan ulang "Generate Baselines"
  — hijau stabil tetap tak terjamin (~20 ID goyang/run); B) telusuri sumber goyang (`autoCorrect`+`parallel` di `detekt {}` -> menyentuh `app/build.gradle.kts`, butuh instruksi eksplisit); C) terima merah (APK tetap terbit).
  (4) Masih terbuka: artifact terpisah run #504/#506/#507 terhapus? (tab Artifacts, cek user); `RingtoneEncoder.kt:130` WrongConstant = satu-satunya lint nyata (perbaikan hanya kalau user minta). Gagal parah -> kembali `SONIX_v529.zip` (perilaku app identik, tanpa baseline).
- Batch 529 (sebelum 530). ZIP: `SONIX_v529.zip`. **2 file dokumen** (`PROJECT_STATE.md`, `CHANGELOG.md`); 0 kode. **[RESUME POINT: FITUR_BARU / BUG_TARGET -> STATUS TERAKHIR -> LANGKAH SPESIFIK AKUN BERIKUTNYA]**:
  Opsi A (baseline detekt+lint) -> `baseline.yml` SIAP tapi BELUM dijalankan; baseline BELUM ada (unggahan Batch 529 = `ci_report_506` biasa, bukan `baseline_<run>`); run #506: build + instrumentation hijau, static-analysis merah (diharapkan) ->
  (1) USER: push (Box DAILY UPDATE) -> Actions -> "Generate Baselines" (branch main) -> unduh `baseline_<run>` -> upload ke chat. Tanpa artifact itu, jangan buat batch baru atas baseline kecuali user memberi instruksi lain.
  (2) Akun berikutnya setelah artifact datang: langkah (2)-(4) Batch 528 di bawah TETAP berlaku (HANYA `app/detekt-baseline.xml` + `app/lint-baseline.xml` ke ZIP, tanpa edit tangan, tanpa melonggarkan threshold; job merah -> betulkan HANYA `baseline.yml`).
  (3) Kandidat lint sudah dibaca di source (Batch 529, 0 diubah): `EqualizerController` StaticFieldLeak = false positive (`getInstance()` menyimpan `applicationContext`); `MainActivity` BatteryLife = kebijakan Play Store (ada KDoc Batch 471 di manifest);
  `PlaybackService` ExportedService = kemungkinan sengaja (jangan tambah `android:permission` tanpa uji device); `RingtoneEncoder.kt:130` WrongConstant (`bufferInfo.flags = extractor.sampleFlags`) = satu-satunya yang nyata -> perbaikan (mapping flag eksplisit, 1 file)
  HANYA kalau user minta; baseline akan menyembunyikannya dari gate, perbaikan tetap bisa kapan saja.
  (4) Nomor baris di laporan CI != ZIP (`MainActivity.kt`: `onCreate` 254 vs 287, `AppNavHost` 535 vs 552) -> cari lewat nama simbol/Signature, bukan nomor baris; dugaan `autoCorrect: true` menulis ulang source di runner, BELUM terbukti.
  (5) Masih terbuka: konfirmasi artifact terpisah run #504/#506 terhapus (cek tab Artifacts oleh user). Gagal parah -> kembali `SONIX_v528.zip`.
- Batch 528 (sebelum 529). ZIP: `SONIX_v528.zip`. **1 file CI baru** (`.github/workflows/baseline.yml`) + docs (`PROJECT_STATE.md`, `CHANGELOG.md`, `FILE_MANIFEST.txt`, `README.md`); 0 kode. **[RESUME POINT: FITUR_BARU / BUG_TARGET -> STATUS TERAKHIR -> LANGKAH SPESIFIK AKUN BERIKUTNYA]**:
  Opsi A (baseline detekt+lint dari toolchain asli) -> WORKFLOW SIAP, 0 dijalankan, baseline BELUM ada ->
  (1) User push (Box DAILY UPDATE) lalu jalankan manual Actions -> "Generate Baselines" -> unduh artifact `baseline_<run>` -> upload ke chat. (2) Akun berikutnya (setelah artifact datang): baca `INDEX.txt` (jumlah ID detekt ~1216 & issue lint ~194 sebagai
  patokan kasar dari run #504, bukan syarat), taruh HANYA `app/detekt-baseline.xml` + `app/lint-baseline.xml` ke ZIP (`app/build.gradle.kts` membacanya otomatis); WAJIB masuk ZIP — Box DAILY UPDATE menghapus `app/` sebelum unzip, file yang tak ikut ZIP hilang.
  Jangan tulis/edit isi baseline tangan; jangan longgarkan threshold. (3) Job baseline merah -> baca `logs/*.log` + `INDEX.txt` di artifact (tetap diunggah) dan betulkan HANYA `baseline.yml`; kandidat penyebab: init script
  (`plugins.withId` vs blok `lint {}`; tipe `autoCorrect`/`baseline` detekt), `-Dlint.baselines.continue`. JANGAN ubah `app/build.gradle.kts` untuk ini. (4) Setelah baseline masuk: cek `static-analysis` hijau; kalau masih merah, ambil `ci_report_<run>` baru dan telaah
  temuan yang lolos dari baseline (mis. pesan lint berbeda) — jangan longgarkan threshold. (5) Masih terbuka dari Batch 527: konfirmasi artifact terpisah run #504 terhapus (step "Hapus artifact terpisah" di `collect-reports`) + kandidat lint nyata
  (`RingtoneEncoder.kt:130` WrongConstant, `EqualizerController.kt:189` StaticFieldLeak, `AndroidManifest.xml:106` ExportedService, `MainActivity.kt:635` BatteryLife); `BubbleTileService.kt:80` = false positive (sudah ada guard). Gagal parah -> kembali `SONIX_v527.zip`.
- Batch 527 (sebelum 528). ZIP: `SONIX_v527.zip`. **2 file dokumen** (`PROJECT_STATE.md`, `CHANGELOG.md`); 0 kode. **[RESUME POINT: FITUR_BARU / BUG_TARGET -> STATUS TERAKHIR -> LANGKAH SPESIFIK AKUN BERIKUTNYA]**:
  Triage `ci_report_504` (backlog detekt 4300 + lint 194) -> SELESAI DICATAT, BELUM DITINDAK; artifact gabungan terbukti terbentuk, status hapus artifact terpisah belum terkonfirmasi ->
  (1) Tanya/cek user: tab Artifacts run #504 hanya `ci_report_504`? Kalau `instrumentation_test_report_504`/`static_analysis_report_504` masih ada -> betulkan HANYA step "Hapus artifact terpisah" di job `collect-reports`
  (`.github/workflows/build.yml`; kandidat: izin `actions: write`, respons `gh api`); jangan sentuh `build`. (2) Backlog = KEPUTUSAN USER, jangan pilih sendiri: A) baseline detekt+lint (hijau cepat; baseline format kasar,
  pelanggaran baru ber-Signature sama ikut lolos), B) bersihkan bertahap per rule (low-risk dulu: NoUnusedImports 100, WildcardImport 88; batas 3-5 file/task -> banyak batch; `MainActivity.kt` 1235 & `NowPlayingScreen.kt` 865
  paling padat), C) biarkan merah sampai diputuskan (APK tetap terbit). Baseline WAJIB dibuat lewat `gradle detektBaseline` + baseline lint dari toolchain asli (sandbox tanpa Gradle) — JANGAN ditulis tangan,
  JANGAN longgarkan threshold. (3) Kalau pergeseran temuan format mengganggu (baseline/diff antar-run): uji dulu `autoCorrect`+`parallel` di `detekt {}` sebagai penyebab; belum terbukti. (4) Lint yang layak dicek ke source
  lebih dulu (bukan false positive terkonfirmasi): `RingtoneEncoder.kt:130` WrongConstant, `EqualizerController.kt:189` StaticFieldLeak, `AndroidManifest.xml:106` ExportedService, `MainActivity.kt:635` BatteryLife.
  Jangan buang waktu di `BubbleTileService.kt:80` (sudah ada guard). JANGAN sentuh source Kotlin tanpa instruksi. Gagal parah -> kembali `SONIX_v526.zip`.
- Batch 526 (sebelum 527). ZIP: `SONIX_v526.zip`. **1 file target** (`.github/workflows/build.yml`); 0 source Kotlin, 0 file Gradle. **[RESUME POINT: FITUR_BARU / BUG_TARGET -> STATUS TERAKHIR -> LANGKAH SPESIFIK AKUN BERIKUTNYA]**:
  CI: log failure + laporan digabung jadi 1 artifact `ci_report_<run>` (job `collect-reports`; `static-analysis` + `--continue`) -> KONFIG SELESAI, 0 dijalankan di CI; backlog 503 (4279 temuan detekt/97 file; lint belum pernah jalan) belum ditindak ->
  (1) push, cek run: job `collect-reports` hijau DAN tab Artifacts hanya memuat `ci_report_<run>` (3 artifact terpisah terhapus). (2) `collect-reports` merah / artifact terpisah masih ada -> betulkan HANYA job itu
  (kandidat: perilaku `download-artifact@v4` tanpa name/pattern; izin `actions: write`); jangan sentuh `build`. (3) Backlog detekt/lint (baseline vs perbaikan) = LANGKAH TERPISAH, tunggu keputusan user — langkah 525 (3) di bawah
  tetap berlaku, tapi ambil laporan dari `ci_report_<run>` (folder `static_analysis_report_<run>/` di dalamnya); JANGAN longgarkan threshold. JANGAN sentuh source Kotlin app untuk task ini. Gagal parah -> kembali `SONIX_v525.zip`.
- Batch 525 (sebelum 526). ZIP: `SONIX_v525.zip`. **5 file target** (`build.gradle.kts`, `app/build.gradle.kts`, `config/detekt/detekt.yml` baru, `.github/workflows/build.yml`, `.githooks/pre-commit` baru); 0 source Kotlin app. **[RESUME POINT: FITUR_BARU / BUG_TARGET -> STATUS TERAKHIR -> LANGKAH SPESIFIK AKUN BERIKUTNYA]**:
  detekt 1.23.8 + lintDebug ketat (konstitusi v3.1) -> KONFIG SELESAI, 0 dijalankan; job `static-analysis` belum pernah jalan, hook belum aktif, baseline belum ada ->
  (1) push, baca job `static-analysis` (TERPISAH dari `build`; "CI baseline hijau" G2 = job `build`). (2) Merah karena CONFIG/PLUGIN (bukan temuan) — mis. "Property ... is misspelled" atau "compiled with Kotlin" —
  betulkan HANYA `config/detekt/detekt.yml` / blok `detekt {}` + pin 2.0.21 di `app/build.gradle.kts`. (3) Merah karena TEMUAN LAMA (diharapkan): ambil artifact `ci_report_<run>` (folder `static_analysis_report_<run>/`; sejak Batch 526), generate `gradle detektBaseline`
  + baseline lint -> `app/detekt-baseline.xml` & `app/lint-baseline.xml` (otomatis terbaca bila ada); JANGAN longgarkan threshold. (4) Baru aktifkan hook: `chmod +x .githooks/pre-commit && git config core.hooksPath .githooks`.
  JANGAN sentuh source Kotlin app untuk task ini. Gagal parah -> kembali `SONIX_v524.zip`.
- Batch 524 (sebelum 525). ZIP: `SONIX_v524.zip`. **2 file source diubah** (`PlaybackStateStore.kt`, `LibraryCacheStore.kt`) — Wave 1 T3 (R1). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: user "fokus kerjakan next kandidat low-risk!!" -> T3 (3 `Log.w` -> `AppLogger`) ->
  Status: KODE SELESAI — 3 call `Log.w(TAG, msg, e)` -> `AppLogger.e(TAG, msg, e)` (PlaybackStateStore.kt `load()` catch; LibraryCacheStore.kt `save()` + `load()` catch), import
  ditukar, diff dibaca, 0 `Log.` tersisa. `AppLogger.kt` utuh (w() tanpa Throwable = alasan pakai e()). **0 build/test dijalankan** -> CI BELUM, device BELUM, JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + cek Log Diagnostik tidak banjir ERROR "Gagal load/simpan" pada start dingin normal (2-3x). Wave 1 T1-T3 semuanya KODE SELESAI;
  Wave 2 T4-T10 KODE SELESAI (T10 dilaporkan zero diff); G2 (CI baseline), Wave 3 (T11-T13), Wave 5 BELUM ->
  Next Action: tunggu hasil CI/device user. Log banjir/CI merah -> kembali ke `SONIX_v523.zip`. JANGAN lanjut otomatis; opsi berikut: T13 (analisis `parseLRC` vs `LyricsParser`, 0 kode),
  T12 (audit IME/insets, baca saja), T11 (R2-R3), G2. Wave 5 tetap butuh Compose UI Test dulu.
- Batch 523 (sebelum 524). ZIP: `SONIX_v523.zip`. **0 file source diubah** — Wave 4 D1+D2+D3 (dokumen saja). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: user "v522 another zero diff, berikan opsi pilihan milestone" -> dipilih D1/D2/D3 (T11 tidak dipilih) ->
  Status: SELESAI (dokumen) — D1 `PROJECT_STATE.md` 3148 -> 461 baris, Catatan Batch 425-503 + entri RESUME <=502 dipindah VERBATIM ke arsip; D2 manifest
  == isi ZIP (204 + 2 dotfile = 206, 0 drift); D3 pointer rujukan yatim di `docs/PLANNING.md` §5. `diff -r app/` == v522 (0 beda). **0 build/test dijalankan**;
  verifikasi CI/device T10 (v522) TETAP hanya laporan user ("zero diff"), rincian tidak dirinci ->
  Remaining: belum dipilih user — T3 (opsional), G2 (konfirmasi CI baseline hijau), Wave 3 (T11-T13), Wave 5 ->
  Next Action: tunggu pilihan eksplisit user; JANGAN lanjut otomatis. Opsi: T11 `rememberSaveable` `NowPlayingScreen.kt` (R2-R3, ubah perilaku rotasi; soal sheet berat
  Equalizer/Visualizer boleh terbuka lagi setelah rotasi, user menyerahkan ke penilaian paling aman), T13 analisis `parseLRC` vs `LyricsParser` (0 kode), T12 audit IME/insets
  (baca saja), T3 (3 `Log.w` -> `AppLogger`), G2. Wave 5 tetap butuh Compose UI Test dulu.
- Batch 522 (sebelum 523). ZIP: `SONIX_v522.zip`. **2 file source diubah** (`MainActivity.kt`, + baru `BottomNavBar.kt`) — Wave 2 T10 (R3)
  move-only byte-identik, EKSEKUSI ULANG (0 perilaku berubah). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: user "itu mungkin hanya HP over heat, lanjutkan milestone" -> T10 Wave 2 (eksekusi ke-2) ->
  Status: KODE SELESAI — 4 simbol dipindah (paket root, 0 import baru di `MainActivity.kt`), diff 2 baris `private`->`internal`;
  `MainActivity.kt` identik dengan v520, rekonstruksi == v519 asli. `AppNavHost` utuh. Dugaan overheat = pernyataan user, TIDAK terverifikasi.
  **0 build/test dijalankan** -> CI BELUM dikonfirmasi, device BELUM diuji, JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + device smoke test bottom nav (daftar "Uji device v520/v522" di `CHANGELOG.md` Batch 520/522)
  + bandingkan performa v522 vs v521 pada SUHU SETARA. Turun lagi pada kondisi setara -> hentikan, WAJIB data (layar/gejala, Log Diagnostik),
  jangan tebak fix; kembali ke `SONIX_v521.zip`. **Wave 2 T4-T10 semuanya KODE SELESAI**; T3 (opsional) + G2 (CI baseline) BELUM; Wave 3
  (T11-T13), Wave 4 (D1-D3), Wave 5 BELUM. `NoRippleIndication` (kode mati, 0 pemakai) ikut pindah, tidak dihapus.
  `SongPickerSheet.kt` punya detektor sweep serupa — BELUM dicek ->
  Next Action: tunggu hasil CI/device user. Hijau/nol regresi -> Wave 2 selesai; JANGAN lanjut otomatis — Wave 3-5, T3, D1-D3 butuh pilihan
  eksplisit user (lihat `docs/PENDING_CodeTidyPlan.md` §3).
- Batch 521 (sebelum 522). ZIP: `SONIX_v521.zip`. **ROLLBACK T10**: 2 file source dikembalikan (`MainActivity.kt`, `BottomNavBar.kt` dihapus) —
  source `app/` = v519 utuh — **DIBALIK Batch 522 (user menduga overheat)**. **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: user atas v520 "regression nyata, cuman performance yang downgrade" -> tarik kembali T10 ->
  Status: SELESAI dikembalikan — `diff -r app/` vs `SONIX_v519.zip` = IDENTIK. Akar masalah performa **BELUM diketahui/tidak diklaim**
  (analisis statis 0 mekanisme jelas; hipotesis: dampak build R8/DEX atau instal-baru tanpa baseline profile). **0 build/test
  dijalankan** -> CI BELUM dikonfirmasi, device BELUM diuji ->
  Remaining: user instal v521 lalu nilai performa. (a) pulih -> T10/buildnya penyebab -> T10 DITAHAN, JANGAN diulang tanpa data pengukuran;
  (b) tetap lambat -> BUKAN T10 (kode = v519) -> WAJIB data (layar/gejala, Log Diagnostik, perbandingan v519 vs v521 di HP sama), jangan tebak
  fix. **Wave 2 status akhir: T4-T9 KODE SELESAI, T10 DIBATALKAN/DITAHAN.** T3 (opsional) + G2 (CI baseline) BELUM; Wave 3-5, D1-D3 BELUM
  (butuh pilihan eksplisit user). `NoRippleIndication` (kode mati, 0 pemakai) masih ada di `MainActivity.kt` ->
  Next Action: tunggu hasil performa v521 dari user. JANGAN mulai wave/item lain otomatis; JANGAN ulang T10 tanpa keputusan user + data.
- Batch 520 (sebelum 521). ZIP: `SONIX_v520.zip`. **2 file source diubah** (`MainActivity.kt`, + baru `BottomNavBar.kt`) — Wave 2
  T10 (R3) move-only byte-identik — **DITARIK KEMBALI Batch 521 (regresi performa)**. **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: user "v519 another zero diff, lanjut T10" -> T10 Wave 2 ->
  Status: KODE SELESAI — 4 simbol dipindah (paket root, 0 import baru di `MainActivity.kt`), diff 2 baris `private`->`internal`,
  `MainActivity.kt` baru = v519 dikurangi 399 baris (dicek programatik). `AppNavHost` utuh. **0 build/test dijalankan** -> CI BELUM
  dikonfirmasi, device BELUM diuji, JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + device smoke test bottom nav (daftar "Uji device v520" di `CHANGELOG.md` Batch 520: tap,
  drag tab-bar, label tak ellipsis, font besar, minimize/expand, 0 kilatan/ripple). **Wave 2 T4-T10 semuanya KODE SELESAI**; T3
  (opsional) + G2 (CI baseline) masih BELUM; Wave 3 (T11-T13), Wave 4 (D1-D3), Wave 5 BELUM. Temuan: `NoRippleIndication` kode mati
  (0 pemakai, tidak dihapus). `SongPickerSheet.kt` punya detektor sweep serupa — BELUM dicek ->
  Next Action: tunggu hasil CI/device user. Hijau/nol regresi -> Wave 2 selesai; JANGAN lanjut otomatis — Wave 3-5, T3, dan D1-D3
  butuh pilihan eksplisit user (lihat `docs/PENDING_CodeTidyPlan.md` §3; Wave 3 mengubah perilaku terlihat, Wave 5 syaratnya Compose UI
  Test belum ada). Crash/regresi bottom nav -> kembali ke `SONIX_v519.zip`, hentikan wave.
- Batch 519 (sebelum 520). ZIP: `SONIX_v519.zip`. **2 file source diubah** (`MainActivity.kt`, + baru `OnboardingScreens.kt`) — Wave 2
  T9 move-only (0 perilaku berubah). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: user "v518 tetap zero diff, lanjutkan milestone" -> T9 Wave 2 ->
  Status: KODE SELESAI — 3 composable dipindah (paket root, 0 import baru di `MainActivity.kt`), diff 2 baris `private`->`internal`,
  `MainActivity.kt` baru = v518 dikurangi 142 baris (dicek programatik). **0 build/test dijalankan** -> CI BELUM dikonfirmasi, device
  BELUM diuji, JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + device smoke test jalur onboarding (daftar "Uji device v519" di `CHANGELOG.md` Batch 519;
  hanya muncul bila izin audio belum diberikan). T10 BELUM; T3 (opsional) + G2 (CI baseline) masih BELUM. Observasi (BUKAN bug
  terbukti): kedua layar onboarding pakai `Column` tanpa scroll -> potensi terpotong di layar pendek/landscape/font besar, belum diuji.
  `SongPickerSheet.kt` punya detektor sweep serupa — BELUM dicek ->
  Next Action: tunggu hasil CI/device user. Hijau/nol regresi -> lanjut T10 (`BottomNavBar.kt`, R3, TERAKHIR: WAJIB byte-identik,
  `AppNavHost` TIDAK dipecah; baca dulu sektor bottom nav di file ini + verifikasi ulang nomor baris/pemakai 4 simbol sebelum memindah;
  daftar uji = seret tab-bar, tap tab, label tidak ellipsis, font besar, 0 regresi minimize/expand); crash/regresi onboarding ->
  kembali ke `SONIX_v518.zip`.
- Batch 518 (sebelum 519). ZIP: `SONIX_v518.zip`. **2 file source diubah** (`ui/LibraryScreen.kt`, + baru `ui/LibrarySearch.kt`) — Wave 2
  T8b move-only (0 perilaku berubah). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: user "v517 zero diff, lanjut" -> T8b Wave 2 ->
  Status: KODE SELESAI — 6 composable + 2 val dipindah, diff 5 baris `private`->`internal`, `LibraryScreen.kt` = 670 baris pertama asli
  (identik). **0 build/test dijalankan** -> CI BELUM dikonfirmasi, device BELUM diuji, JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + device smoke test tab Library/pencarian (daftar "Uji device v518" di `CHANGELOG.md`
  Batch 518). T9/T10 BELUM; T3 (opsional) + G2 (CI baseline) masih BELUM. `SongPickerSheet.kt` punya detektor sweep serupa — BELUM
  dicek ->
  Next Action: tunggu hasil CI/device user. Hijau/nol regresi -> lanjut T9 (`OnboardingScreens.kt`, 1 target/batch; periksa dulu
  isi `WelcomeScreen`/`PermissionRationale` sebelum memindah); crash/regresi Library -> kembali ke `SONIX_v517.zip`. T10 = R3,
  dikerjakan terakhir.
- Batch 517 (sebelum 518). ZIP: `SONIX_v517.zip`. **2 file source diubah** (`ui/LibraryScreen.kt`, + baru `ui/LibraryLists.kt`) — Wave 2
  T8a move-only (0 perilaku berubah). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: user "2 bug telah teratasi, lanjutkan milestone" -> T8a Wave 2 ->
  Status: KODE SELESAI — 4 composable dipindah, diff 4 baris `private`->`internal`, `LibraryScreen.kt` = asli minus blok (identik).
  **0 build/test dijalankan** -> CI BELUM dikonfirmasi, device BELUM diuji, JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + device smoke test tab Library (daftar "Uji device v517" di `CHANGELOG.md` Batch 517).
  T8b/T9/T10 BELUM; T3 (opsional) + G2 (CI baseline) masih BELUM. `SongPickerSheet.kt` punya detektor sweep serupa — BELUM dicek ->
  Next Action: tunggu hasil CI/device user. Hijau/nol regresi -> lanjut T8b (`LibrarySearch.kt`, 1 target/batch); crash/regresi
  Library -> kembali ke `SONIX_v516.zip`. T10 = R3, dikerjakan terakhir. **Hasil (Batch 518)**: user melaporkan "v517 zero diff"
  (rincian CI vs device tidak dirinci) -> T8b dikerjakan.
- Batch 516 (sebelum 517). ZIP: `SONIX_v516.zip`. **3 file source diubah** (`MainActivity.kt`, `playback/PlayerViewModel.kt`,
  `ui/LibraryScreen.kt`) — fix 2 bug laporan user atas v515 (perilaku SENGAJA berubah: prompt biometrik otomatis 1x per sesi kunci;
  refresh otomatis MediaStore tanpa shimmer bila list sudah berisi; scroll list mati selama sweep-select).
  **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: user "v515 aman terkendali, except" (1) prompt sidik jari berulang, (2) drag-select tidak ikut jari + shimmer ->
  Status: KODE SELESAI. Bug 1 root cause dari kode (keyakinan tinggi); Bug 2a mekanisme terbukti dari kode tapi pemicu
  perubahan MediaStore BELUM; Bug 2b DUGAAN. **0 build/test dijalankan** -> CI BELUM dikonfirmasi, device BELUM diuji, JANGAN klaim
  verified ->
  Remaining: CI hijau di commit batch ini + device (daftar "Uji device v516" di `CHANGELOG.md` Batch 516). `SongPickerSheet.kt`
  punya detektor sweep serupa — BELUM dicek/disentuh. T8a/T8b/T9/T10 BELUM; T3 (opsional) + G2 (CI baseline) masih BELUM ->
  Next Action: tunggu hasil CI/device user. Prompt biometrik beres -> selesai. Sweep masih tidak ikut jari -> JANGAN fix ke-3 dari
  tebakan: minta user sebut layar persis (tab Lagu / Favorit / sheet pilih lagu) lalu pasang instrumentasi (pola Batch 463, tidak
  lewat `AppLogger` di thread Main). Semua beres -> lanjut T8a (`LibraryLists.kt`, 1 target/batch); crash/regresi -> kembali ke
  `SONIX_v515.zip`. T10 = R3, dikerjakan terakhir. **Hasil (Batch 517)**: user melaporkan "2 bug telah teratasi"
  (rincian CI vs device tidak dirinci) -> T8a dikerjakan.
- Batch 515 (sebelum 516). ZIP: `SONIX_v515.zip`. **2 file source diubah** (`ui/SettingsScreen.kt`, + baru `ui/SettingsSections.kt`) — Wave 2
  T7 move-only (0 perilaku berubah). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: user "haptic feedback udah bagus, lanjut milestone yang tertunda" -> T7 Wave 2 ->
  Status: KODE SELESAI — 4 composable dipindah, diff 4 baris `private`->`internal`. **0 build/test dijalankan** -> CI BELUM
  dikonfirmasi, device BELUM diuji, JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + device smoke test Settings (kartu tema + toggle mode; Kunci Aplikasi: set PIN,
  biometrik, nonaktifkan). T8a/T8b/T9/T10 BELUM; T3 (opsional) + G2 (CI baseline) masih BELUM ->
  Next Action: tunggu hasil CI/device user. Hijau/nol regresi -> lanjut T8a (`LibraryLists.kt`, 1 target/batch); crash/regresi
  Settings -> kembali ke `SONIX_v514.zip`. T10 = R3, dikerjakan terakhir.
- Batch 514 (sebelum 515). ZIP: `SONIX_v514.zip`. **1 file source diubah** (`ui/theme/AppHaptics.kt`) — haptic terlalu lemah
  (perilaku SENGAJA berubah: getaran amplitudo penuh). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: user melapor akar masalah = setelan haptic sistem belum aktif (kini aktif), lalu getaran app "setara Gboard" (lemah)
  -> Status: KODE SELESAI — one-shot amplitudo 255 (TAP 50ms / HEAVY 100ms, TEBAKAN) bila `hasAmplitudeControl()`; tanpa itu
  perilaku Batch 511-513. **0 build/test dijalankan** -> CI BELUM dikonfirmasi, device BELUM diuji, JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + device: kekuatan tier ketuk vs kuat (kurang/pas/kebablasan), TextField (geser
  handle seleksi) tidak menyiksa, baris Log Diagnostik "Haptic aktif: mode=one-shot-kuat(...) kontrol_amplitudo=true" ->
  Next Action: tunggu rasa user. Kurang kuat -> naikkan 2 konstanta `AppHaptics.kt`; kebablasan -> turunkan (tier ketuk
  dulu); pas -> lanjut T7 (`SettingsSections.kt`, 1 target/batch); crash/regresi -> kembali ke `SONIX_v513.zip`.
  Toggle/slider kekuatan haptic di Settings BELUM ada (fitur terpisah, hanya kalau user minta). **Hasil (Batch 515)**: user menilai
  haptic v514 "udah bagus" -> penahanan Wave 2 dicabut, T7 dikerjakan.
- Batch 513 (sebelum 514). ZIP: `SONIX_v513.zip`. **1 file source diubah** (`ui/theme/AppHaptics.kt`) — FIX haptic ke-3
  (perilaku SENGAJA berubah: getaran kategori MEDIA di API 33+). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: laporan user v512 "masih sama aja" + Log Diagnostik (getar_sentuh=0, wrapper terpanggil, hardware-effects) ->
  Status: KODE SELESAI. Dugaan akar masalah (getaran USAGE_TOUCH dibuang saat getar sentuh sistem mati) BELUM dibuktikan;
  **0 build/test dijalankan** -> CI BELUM dikonfirmasi, device BELUM diuji, JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + device: getar terasa (Now Playing, Library tekan-tahan, swipe art, reorder
  antrean); uji cepat: nyalakan getar sentuh sistem -> terasa? (membuktikan/mematahkan dugaan); baca Log Diagnostik
  "Haptic aktif: ... usage=MEDIA" + "panggilan #1..#6" ->
  Next Action: tunggu rasa/log user. Terasa -> lanjut T7 (`SettingsSections.kt`, 1 target/batch); tetap tidak terasa
  padahal usage=MEDIA -> minta user cek setelan getar MEDIA sistem, JANGAN tebak usage lain tanpa data; crash/regresi ->
  kembali ke `SONIX_v512.zip`. Wave 2 T7-T10 tetap DITAHAN sampai haptic beres.
- Batch 512 (sebelum 513). ZIP: `SONIX_v512.zip`. **1 file source diubah** (`ui/theme/AppHaptics.kt`) — FIX lanjutan
  haptic (perilaku SENGAJA berubah: denyut lebih kuat). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: laporan user v511 "haptic gak terasa nyata" (denyut 50/100ms + diagnostik + fix flag log) ->
  Status: KODE SELESAI, akar masalah BELUM terbukti (3 kandidat, lihat "Catatan Batch 512"). **0 build/test
  dijalankan** (sandbox tanpa Gradle/Kotlin/jaringan) -> CI BELUM dikonfirmasi, device BELUM diuji, JANGAN klaim
  verified ->
  Remaining: CI hijau di commit batch ini + device: rasa getar (Now Playing, Library tekan-tahan, swipe art,
  reorder antrean), lalu baca Log Diagnostik: baris "Haptic aktif: ..." (mode, dukungan efek, `getar_sentuh`,
  `vibrate_on`, `intensitas_sentuh`, `intensitas_media`) + baris "panggilan #1..#6" ->
  Next Action: tunggu log/rasa user. Tak ada baris "panggilan" -> masalah wiring/APK (cek versi terpasang);
  "panggilan ... dikirim tanpa exception" tapi tak terasa + intensitas 0 -> sistem menolak, putuskan fix usage
  getar dari DATA itu (jangan tebak); intensitas normal tapi lemah -> naikkan 2 konstanta `AppHaptics.kt`;
  crash/regresi -> kembali ke `SONIX_v511.zip`. Wave 2 T7-T10 tetap DITAHAN sampai haptic beres.
- Batch 511 (sebelum 512). ZIP: `SONIX_v511.zip`. **3 file source/target diubah** (`ui/theme/AppHaptics.kt` BARU,
  `ui/theme/Theme.kt`, `AndroidManifest.xml`) — FIX haptic lemah, perilaku SENGAJA berubah, di luar daftar T
  (instruksi eksplisit user: haptic dieksekusi DULU sebelum lanjut milestone). **[RESUME POINT: Task -> Status ->
  Remaining -> Next Action]**:
  Task: haptic terpusat & lebih kuat (`LocalHapticFeedback` diganti `StrongHapticFeedback` di `AudioPlayerTheme`;
  `LongPress`->tier kuat, `TextHandleMove`->tier ketuk; `Vibrator.vibrate` langsung; permission `VIBRATE`) ->
  Status: KODE SELESAI — ke-75 call site TIDAK disentuh; jenis haptic lain didelegasikan ke bawaan Compose;
  `vibrate()` gagal -> fallback ke bawaan. Laporan user soal v510: selain haptic dianggap nol regresi (tentatif,
  rincian CI vs device tidak dirinci). Akar masalah = sistemik, BUKAN akibat T6. **0 build/test dijalankan**
  (sandbox tanpa Gradle/Kotlin/jaringan) -> CI BELUM dikonfirmasi, device BELUM diuji, JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + device: getar terasa (play/pause, swipe art, multi-select, reorder,
  Settings), baca baris "Haptic aktif: mode=..." di Log Diagnostik, TextField (geser handle seleksi) tidak
  menyiksa, HP dgn getar sentuh sistem mati tetap terasa, 0 crash. Angka `TAP_ONE_SHOT_MS`/`HEAVY_ONE_SHOT_MS`
  (30/55ms) = TEBAKAN AWAL -> setel dari laporan rasa user. Toggle haptic di Settings BELUM ada (fitur terpisah,
  hanya jika user minta). Wave 2 T7–T10 BELUM (T10 R3 terakhir); T3 (opsional) BELUM; Wave 3–5 BELUM.
  **Observasi terbuka Batch 510 MASIH terbuka**: tampilan sheet "Kontrol Lanjutan" (baris terpotong tepi panel) —
  belum ada jawaban user apakah sama di v508 ->
  Next Action: tunggu hasil CI + device haptic dari user. Hijau/OK -> lanjut T7 (`SettingsSections.kt`, 1 target per
  batch); kurang kuat/kebablasan -> setel 2 konstanta di `AppHaptics.kt`; crash/regresi -> kembali ke
  `SONIX_v510.zip`, hentikan.
- Batch 510 (sebelum 511). ZIP: `SONIX_v510.zip`. **2 file source diubah** (`NowPlayingScreen.kt`, + baru
  `ui/AlbumArtHero.kt`) — Wave 2 `docs/PENDING_CodeTidyPlan.md` T6 (diturunkan ke **R3**: blok berisi gesture
  swipe horizontal next/previous; brightness/volume BUKAN di blok; move-only byte-identik, perilaku TIDAK
  berubah). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: Wave 2 pecah file besar, T6 (`AlbumArtHero` -> `AlbumArtHero.kt`) ->
  Status: KODE SELESAI — 315 baris blok identik (diff programatik, tepat 1 baris beda: `private`->`internal`
  karena dipanggil `NowPlayingScreen()`); 25 import yatim dihapus dari `NowPlayingScreen.kt`. Hasil T5
  DILAPORKAN user nol regresi TAPI tentatif ("I guess", Batch 510; rincian CI vs device tidak dirinci) ->
  BELUM verified. **0 build/test dijalankan** (sandbox tanpa Gradle/Kotlin/jaringan) -> CI BELUM
  dikonfirmasi untuk T6, device BELUM diuji, JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + device smoke test T6 (swipe kiri/kanan di art = next/prev + haptik,
  art ikut jari + springback, drag cepat berulang, swipe brightness/volume tetap jalan & tidak bentrok, tampilan
  hero di tema default/Tactile/Skeu/Calm Retro terang+gelap, layar kecil/font besar/landscape/rotasi); T7–T10
  BELUM (T10 R3 terakhir, byte-identik); T3 (opsional) BELUM; Wave 3–5 BELUM. **Observasi terbuka (0 kode
  diubah, penyebab belum diketahui)**: screenshot user sheet "Kontrol Lanjutan" — baris "Edit Info Lagu"
  terlihat terpotong tepi bawah panel dan "Potong Nada Dering" tergambar di bawah batas panel; belum bisa
  dibedakan perilaku lama (Batch 314 `frostedGlass()`+`verticalScroll`) vs regresi T5 -> tanya user apakah
  sama di v508 sebelum menyimpulkan apa pun ->
  Next Action: tunggu hasil CI + device T6 dari user (+ jawaban soal tampilan sheet v508 vs v509+). Hijau/OK ->
  lanjut T7 (`SettingsSections.kt`, 1 target per batch); merah/regresi -> kembali ke `SONIX_v509.zip`,
  hentikan wave.
- Batch 509 (sebelum 510). ZIP: `SONIX_v509.zip`. **2 file source diubah** (`NowPlayingScreen.kt`, + baru
  `ui/AdvancedControlsSheet.kt`) — Wave 2 `docs/PENDING_CodeTidyPlan.md` T5 (R2, move-only, perilaku
  TIDAK berubah). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: Wave 2 pecah file besar, T5 (`AdvancedControlsSheet`/`AdvancedControlsSectionHeader`/
  `AdvancedControlRow` -> `AdvancedControlsSheet.kt`) ->
  Status: KODE SELESAI — 208 baris blok identik (diff programatik), hanya `private`->`internal` di
  `AdvancedControlsSheet` (dipanggil `NowPlayingScreen()`); 8 import ikon yatim dihapus. Hasil T4
  DILAPORKAN user nol regresi (Batch 509, "wave 2 nol regression nyata"; rincian CI vs device tidak
  dirinci; ditafsirkan = T4). **0 build/test dijalankan** (sandbox tanpa Gradle/Kotlin/jaringan) ->
  CI BELUM dikonfirmasi untuk T5, device BELUM diuji, JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + device smoke test T5 (sheet "Kontrol Lanjutan": 3 seksi,
  tiap baris membuka tujuan benar, status Sleep Timer & nilai Kecepatan, slider peredam, scroll di
  layar pendek/font besar); T6–T10 BELUM (T6 periksa dulu gesture/brightness/volume, T10 R3
  terakhir); T3 (opsional) BELUM; Wave 3–5 BELUM ->
  Next Action: tunggu hasil CI + device T5 dari user. Hijau/OK -> lanjut T6 (`AlbumArtHero`, periksa
  dulu gesture/brightness/volume; kalau ada turun ke R3; 1 target per batch); merah/regresi -> kembali
  ke `SONIX_v508.zip`, hentikan wave.
- Batch 508 (sebelum 509). ZIP: `SONIX_v508.zip`. **2 file source diubah** (`NowPlayingScreen.kt`, + baru
  `ui/NowPlayingDialogs.kt`) — Wave 2 `docs/PENDING_CodeTidyPlan.md` T4 (R2, move-only, perilaku TIDAK
  berubah). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: Wave 2 pecah file besar, T4 (`SleepTimerDialog`/`SpeedDialog`/`RatingDialog`/
  `TransitionModeOption` -> `NowPlayingDialogs.kt`) ->
  Status: KODE SELESAI — 281 baris blok identik (diff programatik), hanya `private`->`internal` di 3 fungsi
  yang dipanggil `NowPlayingScreen()`; 2 import yatim (`selectable`, `Role`) dihapus. Wave 1 (T1+T2)
  DILAPORKAN berhasil oleh user (Batch 508, rincian CI vs device tidak dirinci). **0 build/test
  dijalankan** (sandbox tanpa Gradle/Kotlin/jaringan) -> CI BELUM dikonfirmasi, device BELUM diuji,
  JANGAN klaim verified ->
  Remaining: CI hijau di commit batch ini + device smoke test T4 (Sleep Timer, Pengaturan Putar, Beri
  Rating); T5–T10 BELUM (T6 periksa dulu gesture/brightness/volume, T10 R3 terakhir); T3 (opsional) BELUM;
  Wave 3–5 BELUM ->
  Next Action: tunggu hasil CI + device T4 dari user. Hijau/OK -> lanjut T5 (`AdvancedControlsSheet`,
  1 target per batch); merah/regresi -> kembali ke `SONIX_v507.zip`, hentikan wave.
- Batch 507 (sebelum 508). ZIP: `SONIX_v507.zip`. **3 file source diubah** (`LibraryScreen.kt`,
  `PlayerViewModel.kt`, + baru `ui/SharedComponents.kt`) — Wave 1 `docs/PENDING_CodeTidyPlan.md` T1 + T2
  (R1, perilaku TIDAK berubah). **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: Wave 1 perapihan kode (T1 komponen bersama, T2 sleep-timer) ->
  Status: KODE SELESAI — T1 move-only (blok 107 baris identik, `ShimmerList` private->internal, 11 import
  tak terpakai dihapus dari `LibraryScreen.kt`); T2 ekstrak `startSleepTimerCountdown(endAt)` menggantikan
  2 loop identik. **0 build/test dijalankan** (sandbox tanpa Gradle/Kotlin/jaringan) → CI BELUM
  dikonfirmasi, device BELUM diuji, JANGAN klaim verified ->
  Remaining: G2 (CI hijau di commit batch ini) + device smoke test T1 (Library kosong/loading, layar
  pemakai `EmptyState` lain) & T2 (set timer -> hitung mundur -> batal; restart app saat timer aktif);
  T3 (opsional, ubah perilaku log) BELUM; Wave 2–5 BELUM ->
  Next Action: tunggu hasil CI + device dari user. Hijau/OK -> lanjut Wave 2 (mulai R2 terendah, mis. T4)
  HANYA atas pilihan user; merah/regresi -> kembali ke `SONIX_v506.zip`, hentikan wave.
- Batch 506 (sebelum 507). ZIP: `SONIX_v506.zip`. **0 file source diubah** — dokumentasi murni: 7 file
  `docs/archive/` DIPULIHKAN dari riwayat git (commit restore di-push user, `6d02a4d..96c3768`) dan
  disertakan di ZIP ini. **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: G1 (P0 `docs/archive/`, `docs/PLANNING.md` §1) ->
  Status: TUNTAS — 7 file terverifikasi utuh dari `docs_archive.zip` unggahan user (0 error ZIP,
  0 pola secret, `MANUAL_QA_CHECKLIST.md` 19 item/0 dicentang, cocok catatan PLANNING §4) ->
  Remaining: G2 (CI baseline hijau, dicek user di tab Actions) + G3 (user pilih ID item di
  `docs/PENDING_CodeTidyPlan.md`, saran mulai T1/T2) + Wave 1–5 semua BELUM; akar masalah kenapa ZIP
  sebelumnya tidak memuat `docs/archive/` BELUM dikonfirmasi ->
  Next Action: tunggu user pilih ID item, JANGAN eksekusi otomatis. Jalankan [DAILY UPDATE] HANYA
  dgn ZIP yang memuat `docs/archive/` (v506+), kalau tidak folder itu terhapus lagi dari tip git.
  **[KOREKSI STALE — Batch 506]**: baris "TEMUAN P0 ... MASIH BERLAKU" di entri Batch 505 & 504 di
  bawah SUDAH USANG (arsip sudah dipulihkan & ada di ZIP ini) — entri itu dibiarkan sbg log historis.
- Batch 505 (sebelum 506). ZIP: `SONIX_v505.zip`. **0 file source diubah** — dokumentasi baru murni:
  `docs/PENDING_CodeTidyPlan.md` (planning perapihan kode berbasis konstitusi pipeline; dipicu
  instruksi eksplisit user "buatkan dan tanamkan file planning doc… merapikan code project").
  **[RESUME POINT: Task -> Status -> Remaining -> Next Action]**:
  Task: perapihan kode sesuai konstitusi (`docs/PENDING_CodeTidyPlan.md`) ->
  Status: PLANNING SAJA — 0 item dieksekusi, 0 build/test dijalankan, angka dokumen = hasil
  grep/wc/baca source ZIP (snapshot Batch 504) ->
  Remaining: Wave 0 gerbang (G1 P0 `docs/archive/` dari `docs/PLANNING.md` §1, G2 CI baseline
  hijau, G3 user pilih item) + Wave 1–5 (T1–T13, D1–D3) semua BELUM ->
  Next Action: tunggu user memilih ID item (mis. T1/T2, risiko terendah) — JANGAN eksekusi
  otomatis; sektor DITUTUP & file dikecualikan (`PlaybackService.kt`, `AppLockStore.kt`,
  `app/build.gradle.kts`, `FloatingBubbleService.kt`, bottom nav) tetap tidak disentuh (lihat §2
  dokumen). Temuan utama: 7 file >800 baris = ≈42% kode; `rememberSaveable` 0 pemakaian sementara
  `MainActivity` tanpa `configChanges` (perilaku rotasi BELUM diuji device); sleep-timer loop
  duplikat di `PlayerViewModel.kt`; 2 parser LRC dalam 1 layar (`LyricsSheet.kt`); `EmptyState`/
  `Shimmer*` salah rumah di `LibraryScreen.kt`. **P0 `docs/archive/` dari Batch 504 MASIH
  BERLAKU** — ZIP sumber sesi ini tetap tidak memuat 7 file itu (tidak dikarang ulang).
- Batch 504 (sebelum 505). ZIP: `SONIX_v504.zip`. **0 file source diubah** — dokumentasi baru murni:
  `docs/PLANNING.md` (roadmap + audit teknis, dipicu instruksi eksplisit user "tanamkan planning
  dokumentasi"). **TEMUAN P0 (baca sebelum jalankan [DAILY UPDATE] dgn ZIP ini)**: 7 file
  `docs/archive/` (tercatat `FILE_MANIFEST.txt`, direferensikan berkali-kali di file ini/
  README.md) TIDAK ADA di ZIP sumber sesi ini (`AudioPlayer-1.7.20-run482.zip`, 187 file nyata
  vs 194 klaim manifest lama) — risiko terhapus permanen kalau ZIP berikutnya juga bolong (skrip
  [DAILY UPDATE] hapus-lalu-unzip). **Detail lengkap + langkah verifikasi: `docs/PLANNING.md` §
  1.** Audit teknis tambahan (CI instrumentation-test sempit, rasio test file, Room DAO
  `LyricsDao.kt` — item "belum dicek eksplisit" Aturan sesi aktif #6 DIVERIFIKASI beres, Gradle
  Wrapper belum ada): `docs/PLANNING.md` § 2. Kandidat roadmap (PROPOSAL, tunggu instruksi
  eksplisit user per sektor sebelum eksekusi): `docs/PLANNING.md` § 3. **0 diverifikasi CI/device
  batch ini** — murni file dokumentasi baru + 2 edit kecil VIP (`FILE_MANIFEST.txt`, blok ini),
  0 risiko regresi kode (0 source Kotlin/Gradle disentuh).
  **[RESUME POINT berikutnya]**: sama seperti sebelum batch ini (lihat "Batch 502" di arsip § "Arsip Batch 425-503" — dipindah Batch 523) —
  **0 gap actionable KODE tersisa** dari roadmap Gap QA v488, sisa #4/#5/#7/#8/#9 murni
  device-QA. Prioritas baru dari batch ini (belum dieksekusi, PROPOSAL murni): (a) verifikasi
  `docs/archive/` di Termux SEBELUM `[DAILY UPDATE]` berikutnya (lihat TEMUAN P0 di atas —
  paling mendesak, P0 stability dokumentasi); (b) 4 kandidat roadmap `docs/PLANNING.md` § 3,
  TIDAK ada yang dieksekusi otomatis — tunggu user pilih sektor mana yang mau dilanjutkan.
- Entri `[RESUME POINT]` Batch <=502 dan Catatan Batch 425-503 dipindah Batch 523 -> `docs/archive/PROJECT_STATE_ARCHIVE.md` § "Arsip Batch 425-503".
