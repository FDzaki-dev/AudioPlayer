# ROADMAP: Redesign arah "Liquid Glass" terinspirasi CONVX

Ditanam Batch 278 (instruksi eksplisit user: "ganti arah goals project menuju 100% tipografi/
shape mirip musik player 'CONVX' yang clean+minimalis atau bahkan bisa lebih baik lagi").

**Status batch ini: PERENCANAAN SAJA, sesuai instruksi eksplisit user ("documentation planning
only first"). 0 kode disentuh — 0 file `.kt`/`.xml` diedit, 0 shape/warna/tipografi diubah.**
Dokumen ini adalah peta buat sesi-sesi berikutnya, bukan laporan kerja yang sudah selesai.

---

## 1. Apa itu CONVX (riset, bukan asumsi)

CONVX (`cosmictaserdev-creator/Convx` di GitHub, fork dari `vivi-music`) adalah music player
Android open-source dengan identitas visual **"Liquid Glass"** — terinspirasi bahasa desain iOS
terbaru Apple sendiri, bernama sama. Fakta konkret dari README resminya (bukan tebakan gaya):

- **Real backdrop blur & refraction** — permukaan (nav bar, floating button, sheet) benar-benar
  mengambil sampel piksel DI BELAKANGNYA lalu di-blur+refract secara real-time, bukan warna
  translusen statis. Dibangun di atas library terpisah, `Kyant0/backdrop` (di-vendor, bukan
  dependency biasa) — CONVX sendiri menyebutnya "foundational project", bukan hal remeh.
- **Motion ala iOS**: bouncy rubber-band overscroll, transisi halaman blur, "nav puck" (indikator
  tab aktif) yang springy.
- **Material You**: warna aksen diekstrak otomatis dari artwork album yang sedang main.
- **Sengaja menghindari widget Material stock** — semua permukaan custom-drawn glass.
- Arsitektur teknis: Compose + MVVM (mirip project ini), `Modifier.liquidGlass(...)` sebagai 1
  titik pemakaian terpusat (mirip pola `frostedGlass()` project ini — lihat §3).

Sumber: README resmi repo (`github.com/cosmictaserdev-creator/Convx`), diakses via web search
Batch 278. Tidak ada screenshot resmi yang berhasil ditemukan lewat image search — deskripsi di
atas murni dari teks README+artikel pihak ketiga, BUKAN dari melihat langsung UI-nya. Kalau user
punya screenshot/video CONVX yang lebih spesifik (warna aksen, radius sudut persis, ukuran font),
itu akan jauh lebih akurat daripada riset tekstual ini — lampirkan kalau ada, bakal mempertajam
rencana di bawah.

---

## 2. Kondisi project SEKARANG (basis perbandingan, dicek dari kode sungguhan)

- **4 identitas visual selectable** (`ThemeIdentity` enum, `Theme.kt`): `Apple` ("Tampilan bersih
  khas iOS"), `Tactile` (kaca premium Midnight Blue), `Neumorphism`/Skeu (panel lembut,
  dual-shadow, Titanium+Zamrud), `Calm Retro` (Lo-Fi Sci-Fi, selalu gelap). Semua toggle-able user
  di Settings, bukan 1 skin tunggal.
- **`frostedGlass()` (`BlurUtils.kt`) BUKAN blur sungguhan** — ini temuan PALING PENTING buat
  roadmap ini. Komentar di kode sendiri jujur menjelaskan kenapa: `Modifier.blur()` di Compose
  nge-blur ISI composable itu sendiri, bukan piksel di belakangnya — kalau dipakai ke container,
  teks/ikon di dalamnya ikut buram, kebalikan dari "kaca buram yang bisa dibaca". Solusi yang
  dipakai sekarang: permukaan tinted opacity tinggi (0.92-0.96 alpha) + tepi tipis — glass
  PALSU/simulasi, bukan sampling real-time seperti CONVX. Ini bukan bug — ini keputusan sadar
  Batch 53, didokumentasikan jujur di komentar kode.
- **Shape system**: token radius tunggal (`Radius.xs/sm/md/lg/xl/xxl/hero`, `Spacing.kt`,
  4dp-28dp) dikombinasikan beda-beda per identitas jadi `Shapes(small/medium/large)`
  (`AppleShapes`/`TactileShapes`/`SkeuDarkShapes`/`CalmRetroShapes`, `Theme.kt`) — sudah ada
  sistem token, bukan hardcode tersebar, jadi infrastruktur-nya SIAP dipakai ulang buat radius
  baru, bukan mulai dari nol.
- **`minSdk = 23`** (`app/build.gradle.kts`) — lihat §4, ini kendala teknis nyata buat blur
  sungguhan.
- **Aturan permanen yang TETAP berlaku, redesign visual tidak mengubah ini**: FREEZE playback
  engine/data/SAF/database (sama seperti boundary `POLISH_AUDIT.md` lama, lihat
  `ARCHIVED_POLISH_AUDIT.md`) — ini murni pekerjaan presentation layer.

---

## 3. Keputusan besar yang HARUS dikonfirmasi user dulu sebelum batch eksekusi pertama

**✅ DIKONFIRMASI USER (Batch 279)** — semua 3 keputusan berikut FINAL, bukan lagi rekomendasi:
- **§3a → TAMBAH sebagai tema ke-5** (BUKAN rekomendasi dokumen ini yang "ganti/konsolidasi" —
  user eksplisit override: 4 tema lama TETAP ada, `LIQUID_GLASS` jadi opsi baru di sampingnya).
- **§3b → Opsi B dulu** ("Liquid Glass LOOK" shape+typography murni, TANPA blur asli/minSdk
  bump), dieksekusi BERTAHAP per fase §5. Opsi A (blur asli) TETAP fase terpisah jauh
  setelahnya, belum dikonfirmasi kapan/apakah dieksekusi.
- **§3c → 4 identitas lama TIDAK di-retire, tetap dipertahankan penuh** — konsekuensi langsung
  dari §3a "tambah" (bukan "ganti"), premis asli §3c ("kalau opsi 3a = ganti") jadi tidak
  berlaku, dicatat di sini biar jelas kenapa bagian rekomendasi di bawah TIDAK dieksekusi.

Sisa bagian di bawah (rekomendasi awal sebelum konfirmasi) dipertahankan apa adanya sebagai
jejak keputusan — jangan dihapus, biar sesi berikutnya bisa lihat apa yang direkomendasikan vs
apa yang akhirnya dipilih user.

### 3a. Ganti 4 tema jadi 1, atau tambah sebagai tema ke-5?
**Rekomendasi: ganti/konsolidasi**, bukan tambah opsi ke-5. Alasan: instruksi user bilang "ganti
arah **goals project**" (arah keseluruhan), bukan "tambah pilihan baru" — kalau cuma nambah
opsi, users lama tetap bisa pakai 3 tema lama dan kerja redesign ini jadi opsional/tersembunyi,
bukan "arah baru". Tapi ini keputusan besar (4 tema itu representasi ratusan batch kerja
sebelumnya) — **perlu konfirmasi eksplisit**, bukan dieksekusi diam-diam di batch pertama nanti.

### 3b. Blur sungguhan (real backdrop sampling) vs "glass look" tanpa blur asli?
Dua opsi valid, trade-off beda jauh:
- **Opsi A — Blur asli**: pakai `RenderEffect`/`RenderNode` (API 31+, `android.graphics.RenderEffect`)
  buat capture+blur konten di belakang composable, pola serupa `Kyant0/backdrop`/CONVX sendiri.
  Hasil paling mendekati CONVX sungguhan. **Butuh bump `minSdk` 23→31** (lihat §4) — sejalan
  dengan rule #3 `PROJECT_STATE.md` ("prioritas mutakhir, bukan kompatibilitas lama") jadi
  KEMUNGKINAN BESAR selaras dgn preferensi user yang sudah ada, tapi tetap perubahan besar
  (drop dukungan Android <12) yang harus disetujui eksplisit, bukan efek samping tak terduga.
  Effort tinggi — infrastruktur blur real-time belum ada sama sekali di project ini.
- **Opsi B — "Liquid Glass LOOK" tanpa sampling asli**: pertahankan pendekatan `frostedGlass()`
  yang sudah ada (tinted-surface simulasi), tapi ganti SHAPE+TYPOGRAPHY-nya biar terasa minimalis
  ala CONVX (radius lebih besar/pill, kontras lebih lembut, tipografi lebih ringan) — **tidak
  butuh bump minSdk**, effort jauh lebih kecil, tapi tidak akan pernah benar-benar seperti CONVX
  (yang justru blur SUNGGUHAN itu ciri khas utamanya).

**Rekomendasi: mulai dari Opsi B dulu** (shape+typography murni, sesuai instruksi user sendiri
"100% tipografi/shape" — TIDAK secara eksplisit minta blur-engine baru), blur asli (Opsi A) jadi
fase terpisah SETELAH shape/typography beres DAN user konfirmasi mau invest effort tinggi + bump
minSdk. Ini juga alasan kenapa dokumen ini dipisah dari implementasi — biar keputusan besar ini
kelihatan dulu sebelum ada kode yang harus "dibongkar lagi" kalau ternyata pilihannya beda.

### 3c. Nasib 4 identitas lama kalau opsi 3a = "ganti"
Retire total (hapus `Tactile`/`Neumorphism`/`Calm Retro`, `Apple` dievolusikan jadi basis Liquid
Glass baru), atau tetap disimpan sebagai kode tapi tidak lagi jadi arah utama? Rekomendasi:
**`Apple` dievolusikan** (paling dekat secara filosofi — sama-sama "iOS-inspired, clean"), 3
lainnya di-retire bertahap per-batch (bukan sekali hapus besar — resiko regresi tinggi kalau
dibongkar sekaligus, sejalan prinsip Strict Micro-Batching yang sudah jadi standar project ini).

---

## 4. Kendala teknis konkret (bukan pendapat, dicek dari kode+dokumentasi resmi)

| Kendala | Fakta | Implikasi |
|---|---|---|
| `minSdk = 23` | `app/build.gradle.kts` baris 88 | `RenderEffect`/blur asli perlu API 31+. Opsi A §3b TIDAK JALAN tanpa bump minSdk. |
| `Modifier.blur()` blur foreground, bukan background | Komentar `BlurUtils.kt`, dikonfirmasi perilaku resmi Compose | Alasan `frostedGlass()` sekarang glass palsu — bukan bug yang "gampang" diperbaiki, perlu infrastruktur baru total kalau mau blur asli. |
| 4 shape-set sudah ada, per-identitas | `Theme.kt` baris 220-254 | Infrastruktur token radius (`Radius.*`, `Spacing.kt`) SIAP dipakai ulang — redesign shape tidak mulai dari nol. |
| 0 shared component library | Catatan `ARCHIVED_POLISH_AUDIT.md` §4 ("26 file `ui/*.kt`, semua inline") | Redesign shape/tipografi akan MENYENTUH BANYAK FILE satu per satu (bukan 1 titik pusat) — perlu direncanakan per-komponen (Button/Card/Sheet/dst), bukan 1 batch raksasa. Selaras Strict Micro-Batching yang sudah standar. |

---

## 5. Rencana eksekusi bertahap

**Status: §3 terkonfirmasi Batch 279, eksekusi dimulai.**

Tiap fase = beberapa batch terpisah (1 sub-item/batch, standar project). Urutan diusulkan
mengikuti pola `POLISH_AUDIT.md` lama (Motion→Responsive→Surface/Color→Component→Typography)
karena pola itu sudah terbukti jalan 25+ batch tanpa masalah, bukan re-invent proses:

1. **Fondasi token baru** — definisikan skala radius baru (lebih besar/pill, minimalis) +
   skala tipografi baru (font-weight lebih ringan, letter-spacing, line-height) di file token
   yang SUDAH ADA (`Spacing.kt`/`Type.kt`) sebagai identitas terpisah dulu (belum jadi default).
   **✅ SELESAI Batch 279**: `Radius.liquidLg` (34dp) + `Radius.liquidPill` (999dp, stadium
   shape) ditambahkan `Spacing.kt`; `LiquidGlassTypography` (weight 1 tingkat lebih ringan tiap
   judul + letterSpacing lebih terbuka dari `AppleTypography`, size/lineHeight dipertahankan
   sama sengaja) ditambahkan `Type.kt`. Purely additif, 0 dipakai di manapun (dikonfirmasi grep
   0 hasil di luar 2 file definisi) — belum ada visual berubah sama sekali sampai fase 2.
2. **1 identitas baru utuh** (`Theme.kt`) — reuse pola `ThemeIdentity` enum yang sudah ada,
   named misalnya `LIQUID_GLASS`, warna+shape+typography lengkap, TAPI belum jadi default/belum
   retire yang lama (opsional dulu, biar bisa dibandingkan side-by-side sebelum commit §3a).
   **✅ SELESAI Batch 280**: `ThemeIdentity.LIQUID_GLASS` ditambahkan (otonom kedua mode, pola
   Apple/Tactile/Skeu). `Color.kt` +10 token (Dark/Light Background/Surface/SurfaceVariant/
   Text/SecondaryText + Accent violet-glass + Success teal/mint, palet STATIS — ekstraksi
   artwork Material You masih fase terpisah). `Theme.kt` +`LiquidGlassDarkColors`/
   `LightColors` + `LiquidGlassShapes` (`small=Radius.xl, medium=Radius.xxxl,
   large=Radius.liquidLg` — `liquidPill` 999dp SENGAJA TIDAK dipasang di `Shapes` generik,
   berisiko blob di surface besar, disimpan utk dipakai langsung di komponen pill spesifik fase
   3). 3 titik dispatch diupdate: `colorsFor()`, `typography` when-block, `shapes` when-block —
   semua dikonfirmasi exhaustive (5/5 identitas). **Picker Settings 0 disentuh** — sudah iterate
   `ThemeIdentity.entries` generik + `ThemeOptionCard` derive warna via `colorsFor()` generik,
   LIQUID_GLASS otomatis muncul begitu enum-nya ada, dikonfirmasi baca kode dulu sebelum
   diasumsikan.
3. **Terapkan ke komponen inti** (urutan dampak-terbesar-dulu, sama pola `POLISH_AUDIT.md`):
   MiniPlayerBar → NowPlayingScreen → LibraryScreen row → Sheets/Dialog → Settings. **← SEDANG
   BERJALAN.** Ini juga titik yang tepat utk mulai pasang `Radius.liquidPill` (999dp, stadium
   penuh) LANGSUNG di call site komponen yang genuinely pill (tombol besar/chip/FAB), bukan
   lewat `Shapes` generik (lihat catatan Batch 280 di §2 di atas).
   **✅ Sub-langkah "glass-edge" SELESAI serentak Batch 281**: `frostedGlass()` (`BlurUtils.kt`)
   dapat cabang `isLiquidGlass` sendiri di `edgeBrush` (highlight rim violet tipis + sekalian
   perbaiki laten bug alpha-terbalik mode-terang). Karena helper ini 1 shared call site dilalui
   SEMUA panel glass (MiniPlayerBar/NowPlayingScreen/tiap Sheet/card Home-Library), urutan
   "MiniPlayerBar dulu baru NowPlayingScreen dst" untuk bagian glass-edge spesifiknya jadi
   otomatis serentak, bukan perlu diulang per file. **Belum diverifikasi visual di device.**
   **⏳ Sisa sub-langkah 3 yang BELUM dikerjakan** (bukan glass-edge, hal lain): (a) audit apakah
   ada elemen pill/chip lebar (lebar≠tinggi) di komponen-komponen itu yang layak dipasangi
   `Radius.liquidPill` eksplisit di call site — belum ada kandidat ditemukan sejauh file yang
   sudah dibaca; (b) cek apakah ada styling HARDCODED per-identitas lain (bukan lewat
   MaterialTheme dispatch) di MiniPlayerBar/NowPlayingScreen/LibraryScreen row/Sheets/Settings
   yang butuh cabang Liquid Glass eksplisit — baru diperiksa utk `frostedGlass()`'s edgeBrush,
   BELUM diperiksa menyeluruh utk cabang lain (mis. `tactileEmboss`/`skeuEmboss`-style flourish
   custom per komponen di luar helper shared ini).
4. **Keputusan final §3a** — **sudah final: TAMBAH, bukan ganti** (lihat §3 di atas), jadi
   langkah ini SUDAH TIDAK PERLU dieksekusi terpisah (dulu didraft "putuskan setelah fase 3
   kelihatan hasilnya" — sekarang sudah diputuskan duluan oleh user, bukan ditunda).
5. **(Opsional, fase terpisah jauh setelahnya)** Opsi A §3b — blur asli, kalau user konfirmasi
   mau invest + bump minSdk.

---

## 6. Yang TIDAK berubah (boundary permanen, bukan spesifik dokumen ini)

Playback engine, Queue/shuffle/repeat, MediaStore/SAF, Database, Repository, Persistence,
MediaSession, Audio focus, Background playback, Navigation architecture, Feature behavior —
sama persis boundary `ARCHIVED_POLISH_AUDIT.md`, berlaku permanen untuk redesign visual apa pun.

---

## Cara pakai dokumen ini (sesi berikutnya)

1. **Jangan langsung eksekusi §5** — konfirmasi §3a/3b/3c ke user dulu (lewat chat, bukan
   diasumsikan dari dokumen ini sendiri).
2. Begitu terkonfirmasi, mulai §5 fase 1, 1 batch = 1 sub-item, Strict Micro-Batching tetap
   berlaku (tidak ada pengecualian utk dokumen ini).
3. Update dokumen ini per fase selesai (descending, temuan terbaru di atas) — pola sama seperti
   `PROJECT_STATE.md`/`CHANGELOG.md`.
