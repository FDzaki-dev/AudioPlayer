# PROJECT_STATE.md

RAM instan sesi kerja — hanya rule AKTIF final. Tanpa histori revisi, kutipan user, atau
kronologi batch. Histori lengkap tiap batch: `CHANGELOG.md`. Ringkasan fitur: `README.md`.
Arsip batch lama (1-424): `docs/archive/PROJECT_STATE_ARCHIVE.md`.

## ✅ STATUS PROYEK: ACTIVE
Banner DISCONTINUED dicabut eksplisit oleh user (Batch 432). Proyek lanjut normal, sektor dibuka
per instruksi eksplisit user seperti biasa (lihat "Sektor DITUTUP" di bawah untuk yang masih
butuh reopen spesifik).

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
`AudioPlayer-batchN-release.zip` melacak nomor batch percakapan (bukan versionName/versionCode).
`versionCode`/`versionName` otomatis dari jumlah commit git. Detail lengkap: README.md §
"Standar Penomoran Versi".
