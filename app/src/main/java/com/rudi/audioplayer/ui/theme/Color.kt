package com.rudi.audioplayer.ui.theme

import androidx.compose.ui.graphics.Color

// Apple Music-inspired system palette — true black/white extremes are
// deliberate here (unlike the old boutique palette), matching iOS/Apple
// Music's own OLED-friendly, high-legibility dark mode and crisp light mode.

// Dark mode — mirrors iOS system colors (systemBackground / systemGray6/5 dark)
val AppleDarkBackground = Color(0xFF000000)
val AppleDarkSurface = Color(0xFF1C1C1E)
val AppleDarkSurfaceVariant = Color(0xFF2C2C2E)
val AppleDarkText = Color(0xFFFFFFFF)
val AppleDarkSecondaryText = Color(0xFFB0B0B5)

// Light mode — mirrors iOS system colors (systemBackground / systemGray6/5 light)
val AppleLightBackground = Color(0xFFFFFFFF)
val AppleLightSurface = Color(0xFFF2F2F7)
val AppleLightSurfaceVariant = Color(0xFFE5E5EA)
val AppleLightText = Color(0xFF000000)
val AppleLightSecondaryText = Color(0xFF636366)

// Signature accent — a refined deep blue accent for a calmer, premium interface.
// Used for chrome (buttons, active states); the Now Playing screen itself still
// prefers a per-song accent extracted from the album art, the same way a modern
// music player can tint itself from the artwork.
val AppleAccent = Color(0xFF4F7CFF)

// Success/match indicator — iOS system green, one per mode so it stays legible
// against both extremes. Used for positive-confirmation states (e.g. signature
// verification match) instead of a hardcoded color that wouldn't adapt.
val AppleDarkSuccess = Color(0xFF32D74B)
val AppleLightSuccess = Color(0xFF34C759)

// ============================================================================
// TACTILE — "Premium AMOLED Hybrid Glassmorphism + Subtle Midnight Blue +
// Micro-Skeuomorphism" — Batch 53: full repaint from the user-supplied
// compose-amoled-hybrid-glass-final.md spec, superseding Batch 52's literal flat
// Midnight Blue palette entirely (that spec is now explicitly listed as an
// anti-pattern by this one — §24: "a full Midnight Blue theme").
//
// Visual priority per spec §2 (heaviest → lightest): AMOLED foundation > frosted
// glass > Midnight Blue ambience > tactile depth > accent/glow. Every token below
// is named for the *role* it plays in that hierarchy, not for a literal color, so
// existing call sites (isTactile comparisons, tactileEmboss(), frostedGlass()) all
// keep working unmodified — only the values, and the two Theme.kt/BlurUtils.kt/
// TactileDepth.kt implementations that interpret them, change this batch.
// ============================================================================

// --- Level 0: AMOLED foundation (spec §3) ---------------------------------
// Near-black, not pure #000000, so glass layers stacked on top stay perceptible
// (spec §3 "Important"). TactileBackground is still the app's isTactile identity
// token (every isTactile check compares colorScheme.background against it).
val TactileBackground = Color(0xFF030508) // spec §3 AmoledBlack — root canvas
val AmoledSurface = Color(0xFF070A0F) // spec §3 AmoledSurface — secondary near-black reference

// --- Level 1-2: Hybrid glass surfaces (spec §5) ----------------------------
val TactileSurface = Color(0xFF0A0F16) // spec §5 GlassBase — Level 1 translucent glass (M3 `surface`)
val TactileSurfaceVariant = Color(0xFF101722) // spec §5 GlassElevated — Level 2 elevated frosted glass ("raised" stop)

// --- Level 3-4: Tactile control surfaces (spec §10-13) ---------------------
// Reserved for a future dedicated TactileButton/TactileSwitch/TactileSlider component set
// (spec §23's `ui/components/` hierarchy — not built yet, same gap noted every batch since
// Batch 52). Removed here (Batch 54 technical debt pass): TactileControl/TactileControlPressed/
// GlassPressed/GlassWhite/TactileMutedText had zero call sites (grep-confirmed) — unused design
// tokens rot silently, and their exact values will need re-deriving from whatever spec drives
// that future component batch anyway, so keeping unused placeholders here added no real value.
// Re-add when that component set is actually built.

// --- Typography (spec §16) --------------------------------------------------
val TactileText = Color(0xFFEAF0F8) // spec §16 TextPrimary
val TactileSecondaryText = Color(0xFFAAB5C4) // spec §16 TextSecondary

// --- Semantic status ---------------------------------------------------------
// Not given literally by the spec (glass/tactile system has no error/success
// tokens) — carried over unchanged from Batch 52, still legible against the new
// darker AMOLED-glass surfaces at normal text sizes.
val TactileError = Color(0xFFFF6B6B)
val TactileSuccess = Color(0xFF34D399)

// --- Midnight Blue — atmospheric layer ONLY (spec §6) -----------------------
// Never used as a base/background color directly (spec §6 "Incorrect use"). Only
// consumed as a low-alpha ingredient inside ambient gradients (MainActivity root
// backdrop) and focused/selected glass surfaces — never on cards, text, borders,
// navigation, or inactive components (spec §6 "must NOT dominate").
val MidnightBlue = Color(0xFF191970) // spec §6 literal
val MidnightBlueAmbientAlpha = 0.06f // spec §6 literal ambient-gradient alpha
// Batch 62 — user instruksi eksplisit: "radikal", "tanpa mengikuti batasan light/dark
// system" — ambient wash Midnight Blue (dulu Batch 61 digated hanya utk mode gelap,
// dianggap "trait mode" bukan "trait identitas") sekarang SENGAJA tampil di kedua mode.
// Alpha mode-terang dinaikkan jauh di atas versi gelap (0.06f) karena kontrasnya
// terbalik total: di atas AMOLED nyaris-hitam, wash biru tipis 6% sudah cukup terbaca;
// di atas kanvas nyaris-putih, alpha sekecil itu akan nyaris tak kelihatan sama sekali.
val MidnightBlueLightAmbientAlpha = 0.16f

// --- Accent system (spec §17) — restrained cool-blue, functional only -------
val TactileAccent = Color(0xFF6670FF) // spec §6/§17 MidnightBlueAccent == AccentBlue

// --- Glass edge / highlight tokens (spec §5, §8) -----------------------------
// Never plain Color.White (spec §8 "Never use Color.White as a normal glass
// border") — the highlight must read as reflected light, not an outline.
val TactileHighlight = Color.White.copy(alpha = 0.065f) // spec §5 GlassHighlight
val TactileEdge = Color.White.copy(alpha = 0.035f) // spec §5 GlassBorder
val TactileShadow = Color.Black.copy(alpha = 0.70f) // spec §5 GlassShadow

// ============================================================================
// TACTILE — LIGHT VARIANT — Batch 61. User instruksi eksplisit: identitas
// light/dark tema custom "dicabut dan dipisahkan total" dari toggle mode —
// Tactile sekarang punya ekspresi terang sendiri, bukan cuma versi gelap yang
// dipaksa tampil terus. Konsepnya tetap "kaca" (spec asli §4/§5), hanya basisnya
// dibalik: kaca bening di atas kanvas nyaris-putih dingin (echo hue Midnight
// Blue lewat undertone biru samar di background), bukan kaca gelap di atas
// AMOLED. TactileAccent/TactileSuccess/TactileError TIDAK diduplikasi — accent
// yang sama sengaja tetap legible di kedua mode (itsendiri sudah divalidasi
// oleh perhitungan luminance onPrimary di Theme.kt).
// ============================================================================
val TactileLightBackground = Color(0xFFF5F7FB) // nyaris-putih, undertone biru dingin tipis
val TactileLightSurface = Color(0xFFFFFFFF)
val TactileLightSurfaceVariant = Color(0xFFE8ECF5)
val TactileLightText = Color(0xFF12141A)
val TactileLightSecondaryText = Color(0xFF5B6472)
// Catch-light & rim untuk kaca terang: highlight putih penuh (dipakai embossSurface()'s
// alpha-replace, bukan alpha bawaan token ini) + rim biru-gelap lembut sebagai stop kedua —
// pola sama seperti TactileHighlight->TactileEdge versi gelap, kontrasnya dibalik supaya tetap
// terbaca di atas kanvas terang (rim putih-di-atas-putih nyaris tak kelihatan).
// Batch 74 — fix: token ini sejak Batch 61 tidak pernah dikasih alpha (opaque penuh),
// berbeda dari SEMUA token Highlight/Edge lain di file ini (semua ber-alpha rendah).
// Dipakai LANGSUNG (bukan lewat .copy(alpha=...) override) sebagai stop gradient border
// di BlurUtils.kt's frostedGlass() — jadi border Tactile Light selama ini garis putih
// SOLID, persis "bright white border" yang komentar proyek sendiri bilang harus dihindari
// (lihat TactileDepth.kt baris ~102). Diberi alpha 0.55f: cukup terbaca sebagai catch-light
// di atas kanvas terang, tapi tidak lagi opaque.
val TactileLightHighlight = Color.White.copy(alpha = 0.55f)
val TactileLightEdge = Color(0xFF1B2436).copy(alpha = 0.06f)
val TactileLightShadow = Color(0xFF1B2436).copy(alpha = 0.18f)

// ============================================================================
// SKEUOMORPHISM 2.0 — HYPER-REALISM UI — Batch 73. Ganti total arah dari "dark
// lite" (Batch 57-63, sekadar bevel highlight+shadow lembut) ke identitas
// fisik yang jauh lebih dramatis: panel dibaca seperti benda logam sungguhan
// yang dipahat/ditekan ke kanvas — bukan cuma kartu dgn pinggiran terang/gelap
// tipis. Dirancang OTONOM total, tidak menumpang baseline Tactile (AMOLED-glass
// biru dingin) — semua nilai & mekanisme (specular glint, brushed-metal grain,
// ambient occlusion, double-bevel carved edge) di bawah ini murni milik Skeu
// sendiri, tidak ada satu pun turunan langsung dari token Tactile manapun.
// Nama token lama (SkeuAccent/SkeuHighlight/SkeuShadow/SkeuAmbientAlpha*/
// SkeuDarkSurfaceVariant/SkeuLightSurfaceVariant) DIPERTAHANKAN karena masih
// dirujuk langsung dari MainActivity.kt (protected) & NowPlayingScreen.kt —
// nilainya diperkuat/diperdalam untuk kontras hyper-realism, bukan diganti
// nama, supaya blast-radius edit tetap terkendali dalam 1 batch atomic.
// ============================================================================

// --- Foundation --------------------------------------------------------------
// Charcoal netral, kontras dinaikkan sedikit dari versi Dark Lite lama supaya
// bevel timbul yang jauh lebih dalam (lihat TactileDepth.kt skeuEmboss()) tetap
// punya "ruang" kontras yang jelas antara kanvas & permukaan panel.
val SkeuDarkBackground = Color(0xFF121417)
val SkeuDarkSurface = Color(0xFF23262B) // panel timbul level 1
val SkeuDarkSurfaceVariant = Color(0xFF2E3238) // panel timbul level 2 (lebih terangkat)

// --- Typography ----------------------------------------------------------------
// Batch 63 — undertone digeser dari krem hangat ("kertas/kulit") ke abu-perak dingin,
// supaya koheren dgn pergantian aksen tembaga -> Titanium+Silver metalik di bawah
// (metal dingin di atas typography hangat akan terasa nabrak/tidak otonom).
val SkeuDarkText = Color(0xFFEDEFF2) // hampir-putih dingin, nuansa "logam disikat"
val SkeuDarkSecondaryText = Color(0xFFA6ABB2) // abu dingin sekunder

// --- Semantic status -------------------------------------------------------------
val SkeuDarkError = Color(0xFFE5675A)
val SkeuDarkSuccess = Color(0xFF7FB86B)

// --- Accent ------------------------------------------------------------------
// Batch 63 — GANTI TOTAL atas instruksi eksplisit user: tembaga/amber hangat
// (0xFFCB8B4B, identitas sejak Batch 53) DIHAPUS PERMANEN, diganti keluarga
// Titanium+Silver metalik. SkeuAccent (representasi flat, dipakai di role M3
// primary/surfaceTint & isSkeuTheme() — role itu cuma bisa 1 Color, bukan Brush)
// + 3 token gradient (Titanium.../SilverHighlight) di bawah untuk efek "brushed
// metal" nyata di elemen non-M3-role (root ambient wash — lihat MainActivity.kt).
// Hue sengaja dingin-netral, kontras total dgn AppleAccent (biru) & TactileAccent
// (biru-ungu) — sekarang murni dibedakan lewat TEMPERATUR (dingin vs dingin lain)
// bukan lagi lewat hue hangat-vs-dingin seperti sebelumnya; pembeda utama Skeu kini
// ada di STRUKTUR (bevel timbul + brushed-metal streak), bukan lagi warna hangat.
val SkeuAccent = Color(0xFFB6BAC0)
val TitaniumDark = Color(0xFF6B6F75)
val TitaniumLight = Color(0xFFCDD1D6)
val SilverHighlight = Color(0xFFF2F3F5)

// --- Bevel tokens (dipakai skeuEmboss() & frostedGlass()'s Skeu branch) ------
// Batch 73 — Hyper-Realism: highlight & shadow dinaikkan lebih jauh lagi dari
// Batch 62/63 (0.10f/0.55f) — panel sekarang harus terbaca sebagai objek fisik
// bertekstur logam yang dipahat, bukan kartu dgn pinggiran lembut.
val SkeuHighlight = Color.White.copy(alpha = 0.16f)
val SkeuShadow = Color.Black.copy(alpha = 0.65f)
// Batch 62 — ambient root wash Skeu pertama kali (dulu selalu flat total).
// Batch 63 — sumber warna wash diganti dari SkeuDarkAccent (tembaga, sudah tidak ada)
// ke TitaniumDark/SilverHighlight, DAN strukturnya sendiri diganti dari resep 3-stop
// yang sama persis dgn Tactile (background->tint->surfaceVariant) menjadi 4-stop
// "brushed-metal streak" (lihat MainActivity.kt) — supaya Tactile & Skeu tidak lagi
// berbagi baseline struktural yang identik, sesuai instruksi eksplisit user.
val SkeuAmbientAlphaDark = 0.05f
val SkeuAmbientAlphaLight = 0.12f
// Batch 58 — SkeuEdge (dulu 0xFF000000 alpha 0.12f, dipakai sebagai stop kedua
// border frostedGlass()'s Skeu branch) dihapus: diganti SkeuShadow di sana
// (BlurUtils.kt) supaya border-nya kebaca bevel terukir/carved, bukan lagi rim
// kaca lembut ala Tactile — grep dicek nihil pemanggil lain sebelum dihapus.

// --- NEUMORPHISM layer set (Batch 79 — supersedes Batch 73's HYPER-REALISM layer set;
// "upgrade Skeuomorphism -> Neumorphism" atas instruksi eksplisit user). SkeuSpecular/
// SkeuAmbientOcclusion NAMANYA DIPERTAHANKAN (masih dipakai TactileDepth.kt & NowPlayingScreen.kt,
// pola blast-radius-terkendali yang sama seperti Batch 73 dulu) tapi PERANNYA bergeser: dulu
// masing-masing 1 layer tunggal (glint radial / kontak-shadow tunggal), sekarang jadi layer
// TERKUAT/TERDEKAT dari tumpukan multi-layer dual-shadow skeuEmboss() yang baru — teknik
// "banyak layer offset+alpha bertingkat" yang meniru soft-shadow blur ganda ala CSS neumorphism
// klasik (DrawScope Compose tidak punya blur asli tanpa RenderEffect API 31+). SkeuInnerGroove/
// SkeuBrushGrain*/SkeuInnerGroovePressed DIHAPUS TOTAL (grep-confirmed, 0 caller lain setelah
// redesign) — neumorphism generik TIDAK PUNYA border/groove/tekstur SAMA SEKALI, kedalamannya
// murni dari bayangan, ini justru penyederhanaan besar dari 7-layer Hyper-Realism lama.
// "Ultra realistic depth" (instruksi eksplisit user) diterjemahkan sebagai KONTRAS TINGGI pada
// alpha layer-layer ini (jauh di atas neumorphism generik yang sering nyaris tak kelihatan),
// bukan sebagai lebih banyak jenis layer.
val SkeuSpecular = Color.White.copy(alpha = 0.55f)
val SkeuAmbientOcclusion = Color.Black.copy(alpha = 0.40f)
// Pressed-state: sisi terang/gelap TERTUKAR di skeuEmboss() (bukan cuma diredupkan) — bahasa
// visual baku neumorphism utk "permukaan masuk ke kanvas" (concave), lihat TactileDepth.kt.
val SkeuSpecularPressed = Color.White.copy(alpha = 0.10f)

// --- Panel fill Neumorphism (Batch 79, BARU) — hampir sewarna dengan SkeuDarkBackground/
// SkeuLightBackground, BUKAN turunan SkeuDarkSurface/SkeuLightSurfaceVariant (dua token itu
// TETAP dipakai apa adanya utk role M3 surface/surfaceVariant di Theme.kt — Card/Sheet/
// NavigationBar M3 polos di luar skeuEmboss() sengaja TIDAK ikut berubah kontrasnya, supaya
// blast radius perubahan ini tetap murni di dalam skeuEmboss() sendiri). Prinsip inti
// neumorphism: panel terbaca "dipahat dari material yang sama dengan kanvas", bukan lagi
// panel logam 4-stop yang jelas beda warna dari kanvas seperti Hyper-Realism lama.
val SkeuNeuSurfaceDark = Color(0xFF191C21)
val SkeuNeuSurfaceLight = Color(0xFFE8EAED)

// --- Aksen Zamrud (Batch 79, BARU) — "aksen Titanium yang dominan dengan sedikit sentuhan
// zamrud" per instruksi eksplisit user. SkeuAccent (Titanium+Silver) TIDAK diganti/disentuh —
// tetap satu-satunya token di role M3 primary/surfaceTint, jadi Titanium tetap dominan di
// semua tempat yang otomatis mengikuti M3 (NavigationBar terpilih, tombol, dst). Emerald HANYA
// dipakai di 2 titik kecil yang sengaja dipilih manual (bukan lewat role M3 apa pun, supaya
// mustahil "menyebar" tanpa sengaja ke tempat lain): (1) inti glow skeuEmboss() saat DITEKAN
// saja (tidak pernah terlihat di state normal), (2) satu color-stop sempit tambahan di root
// ambient streak (MainActivity.kt, protected/parsial).
val SkeuEmerald = Color(0xFF2FA37C)
val SkeuLightEmerald = Color(0xFF1E7A5C)

// ============================================================================
// SKEUOMORPHISM — LIGHT VARIANT — Batch 61. Sama alasan dengan TACTILE LIGHT di
// atas: identitas Skeu dipisah total dari mode gelap/terang.
// Batch 63 — basis digeser lagi dari krem/parchment hangat (cocok dgn tembaga lama)
// ke platinum/silver dingin (cocok dgn Titanium+Silver baru) — tetap mempertahankan
// jarak kontras background->panel yang jadi ciri khas bevel Skeu, cuma temperatur
// warnanya yang dibalik total supaya identitasnya tetap 1 kesatuan koheren, bukan
// aksen dingin di atas kanvas hangat yang terasa "nabrak".
// ============================================================================
val SkeuLightBackground = Color(0xFFE4E6E9) // platinum/silver netral
val SkeuLightSurface = Color(0xFFF2F3F5) // panel timbul level 1
val SkeuLightSurfaceVariant = Color(0xFFFAFBFC) // panel timbul level 2 (lebih terangkat)
val SkeuLightText = Color(0xFF212327)
val SkeuLightSecondaryText = Color(0xFF63676D)
// Batch 74 — bug sama persis dengan TactileLightHighlight di atas (opaque penuh sejak
// Batch 61, tidak pernah dikasih alpha). Dipakai langsung di frostedGlass()'s Skeu edge
// (BlurUtils.kt) DAN di skeuEmboss()'s outer bevel border (TactileDepth.kt, Batch 73 —
// pola `highlight.alpha * outerBorderAlpha` di situ MENGALIKAN, jadi alpha 1.0 yang salah
// ini lolos bulat-bulat jadi border putih solid 1.5dp di setiap panel Skeu Light).
// Alpha 0.65f dipilih lebih tinggi dari Tactile (0.55f) — konsisten dengan SkeuHighlight
// dark (0.16f) yang juga lebih kuat dari TactileHighlight dark (0.065f), sesuai identitas
// bevel Skeu yang memang lebih tegas/"hyper-realism".
val SkeuLightHighlight = Color.White.copy(alpha = 0.65f)
val SkeuLightShadow = Color(0xFF23262B).copy(alpha = 0.38f)

// --- NEUMORPHISM layer set — LIGHT (Batch 79, supersedes Batch 73's Hyper-Realism light
// set) — kontras tetap dibalik dari versi gelap seperti sebelumnya (spec/AO harus tetap legibel
// di atas kanvas terang), tapi SkeuLightInnerGroove/SkeuLightBrushGrainLight/Dark/
// SkeuLightInnerGroovePressed DIHAPUS TOTAL sama seperti versi gelap (lihat komentar di atas —
// grep-confirmed 0 caller lain setelah redesign).
val SkeuLightSpecular = Color.White
val SkeuLightAmbientOcclusion = Color(0xFF23262B).copy(alpha = 0.22f)
val SkeuLightSpecularPressed = Color.White.copy(alpha = 0.25f)

// ============================================================================
// CALM RETRO — "Calm Retro-Futurism / Lo-Fi Sci-Fi" — dari palet_warna_calm_retro_v2.md
// user. Identitas ke-3, TERKUNCI GELAP PERMANEN (instruksi eksplisit user) — beda dari
// Tactile/Skeu (Batch 61, otonom di kedua mode), theme ini sengaja HANYA punya 1 set warna,
// dipakai apa pun ThemeMode yang aktif. Nilai HEX di bawah literal dari tabel spesifikasi
// markdown, 0 turunan/tebakan.
// ============================================================================
val CalmRetroBackground = Color(0xFF0F1015) // Midnight Dust
val CalmRetroSurface = Color(0xFF161822) // Obsidian Gray
val CalmRetroBorder = Color(0xFF232635) // border .song-card di spec — dipakai outline/surfaceVariant
val CalmRetroText = Color(0xFFE2E4E9) // Silk White
val CalmRetroSecondaryText = Color(0xFF6A6F82) // Slate Mist
val CalmRetroAccent = Color(0xFF7FA99B) // Muted Sage
