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
val TactileLightHighlight = Color.White
val TactileLightEdge = Color(0xFF1B2436).copy(alpha = 0.06f)
val TactileLightShadow = Color(0xFF1B2436).copy(alpha = 0.18f)

// ============================================================================
// SKEUOMORPHISM DARK LITE — Batch 57. No external spec file supplied for this
// theme (unlike Tactile's spec-driven batches 49-55) — palette dirancang
// sendiri sesuai definisi umum "skeuomorphism dark-lite": panel netral gelap
// yang terbaca timbul/fisik lewat bevel highlight+shadow lembut, bukan lewat
// warna aksen mencolok. Batch 63 — aksen tembaga hangat (identitas awal) diganti
// total Titanium+Silver metalik dingin; pembeda dari Tactile (AMOLED near-black +
// hue biru dingin) sekarang murni lewat STRUKTUR (bevel timbul + brushed-metal
// streak, bukan lagi lewat temperatur warna), supaya kedua tema custom tetap
// tidak terasa jadi varian satu sama lain walau sama-sama netral dingin sekarang.
// ============================================================================

// --- Foundation --------------------------------------------------------------
// Charcoal netral, bukan AMOLED near-black — skeuomorphism butuh jarak
// kontras yang cukup antara background & panel timbul supaya bevelnya
// terbaca "fisik", beda tujuan dari Tactile yang justru menargetkan OLED
// near-black sebagai lapisan dasar kaca.
val SkeuDarkBackground = Color(0xFF16181C)
val SkeuDarkSurface = Color(0xFF23262B) // panel timbul level 1
val SkeuDarkSurfaceVariant = Color(0xFF2C3036) // panel timbul level 2 (lebih terangkat)

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
// Pola sama seperti TactileHighlight/Shadow (dua-stop diagonal untuk border,
// alpha tunggal untuk shadow) tapi nilainya sendiri: highlight sedikit lebih
// kuat (bevel skeuomorphic butuh catch-light lebih jelas biar "timbul" terbaca
// di atas charcoal, bukan AMOLED-black), shadow lebih rendah (kontras dasarnya
// sudah lebih tinggi dari AMOLED jadi tak perlu shadow sekuat Tactile).
val SkeuHighlight = Color.White.copy(alpha = 0.10f)
val SkeuShadow = Color.Black.copy(alpha = 0.55f)
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
val SkeuLightHighlight = Color.White
val SkeuLightShadow = Color(0xFF23262B).copy(alpha = 0.30f)
