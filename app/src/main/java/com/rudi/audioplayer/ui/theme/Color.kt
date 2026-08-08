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
val GlassPressed = Color(0xFF070B11) // spec §5 GlassPressed — recessed glass when a surface is pressed/active

// --- Level 3-4: Tactile control surfaces (spec §10-13) ---------------------
// Distinct-but-restrained glass tone for interactive controls (buttons, switches,
// sliders, knobs) — one step lighter than GlassElevated so tactile chrome reads as
// its own layer without becoming a bright card. No literal value given by the spec
// for this role (same gap every prior batch's spec left); tuned to sit correctly
// in the new (darker, glass-first) hierarchy.
val TactileControl = Color(0xFF141C29)
val TactileControlPressed = GlassPressed

// --- Typography (spec §16) --------------------------------------------------
val TactileText = Color(0xFFEAF0F8) // spec §16 TextPrimary
val TactileSecondaryText = Color(0xFFAAB5C4) // spec §16 TextSecondary
val TactileMutedText = Color(0xFF737E8C) // spec §16 TextMuted — captions/disabled, not previously modeled

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

// --- Accent system (spec §17) — restrained cool-blue, functional only -------
val TactileAccent = Color(0xFF6670FF) // spec §6/§17 MidnightBlueAccent == AccentBlue

// --- Glass edge / highlight tokens (spec §5, §8) -----------------------------
// Never plain Color.White (spec §8 "Never use Color.White as a normal glass
// border") — the highlight must read as reflected light, not an outline.
val TactileHighlight = Color.White.copy(alpha = 0.065f) // spec §5 GlassHighlight
val TactileEdge = Color.White.copy(alpha = 0.035f) // spec §5 GlassBorder
val GlassWhite = Color.White.copy(alpha = 0.045f) // spec §5 GlassWhite — subtle wash, distinct from the border/highlight roles
val TactileShadow = Color.Black.copy(alpha = 0.70f) // spec §5 GlassShadow
