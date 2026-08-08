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

// Tactile (Skeuomorphism-lite Hybrid Glass) — Batch 51: full repaint from the user-supplied
// compose-skeuomorphism-lite-hybrid-glass-dark-blue.md spec, which supersedes the Batch 50
// AMOLED-black palette below entirely. Spec §1.1 is explicit: "Pure/AMOLED-black styling is not
// the target" — the whole point of this batch is replacing the near-black Batch 50 tokens with a
// calm dark-blue *gradient* atmosphere plus translucent glass surfaces, not just another recolor
// pass. §2's suggested palette block is used verbatim as literal values (same treatment every
// prior spec-driven Tactile batch has given its own source's example stops).
val TactileBackground = Color(0xFF050B18) // spec §2 literal DarkBackgroundBottom — flat fallback for colorScheme.background/equality checks (isTactile guards); the actual root visual is the 2-stop gradient below, not this flat value alone
val TactileBackgroundTop = Color(0xFF0A1630) // spec §2 literal DarkBackgroundTop — gradient's top-left stop, paired with TactileBackground as the bottom-right stop (MainActivity.kt root Box, spec §3 top-left→bottom-right light direction)
val TactileSurface = Color(0xCC101D35) // spec §2 literal DarkSurface — deliberately translucent (0xCC ≈ 80% opacity), not opaque like Batch 50: this is what makes panels read as "glass" over the gradient behind them, per spec §2/§8
val TactileSurfaceVariant = Color(0xB8142745) // spec §2 literal DarkSurfaceVariant — more translucent again (0xB8 ≈ 72%), one step lighter/bluer than TactileSurface
val TactileGlassOverlay = Color(0x142E6AA3) // spec §2 literal GlassOverlay — very-low-alpha blue wash layered on top of glass fills (BlurUtils.kt frostedGlass()) per spec §8's "subtle linear/radial gradient" formula step
val TactileText = Color(0xFFE8EEF5) // spec §2 literal TextPrimary — unchanged from Batch 50, this spec keeps the same value
val TactileSecondaryText = Color(0xFFA8B3C0) // spec §2 literal TextSecondary — unchanged from Batch 50, this spec keeps the same value
val TactileAccent = Color(0xFF5B9DFF) // spec §2 literal Accent — slightly softer/lighter cool blue than Batch 50's 0xFF4DA3FF, tuned for the new lighter dark-blue surfaces instead of near-black ones
// Not given literally by the spec (its §2 table lists the roles but only shows example values
// for the ones above) — carried over from Batch 50 unchanged, still fits this cooler-but-still-
// restrained palette, legible on TactileBackground/TactileSurface at normal text sizes.
val TactileError = Color(0xFFFF6B6B)
val TactileSuccess = Color(0xFF34D399)

// Interactive-control pair from spec §2's token table (`Control` / `ControlPressed`) — "Opaque/
// translucent blue-black glass" for tactile controls, "Deeper navy/recessed glass" when pressed.
// No literal value given by the spec for these two (same gap as Batch 50); still unused by any
// call site (grep confirms), prepared for the future TactileButton/TactileSwitch/TactileSlider
// components spec §7/§12 asks for but this batch still doesn't build (see TactileDepth.kt doc).
val TactileControl = Color(0xD4152840)
val TactileControlPressed = Color(0xF0060D1C)

// Bevel pair used by tactileEmboss() (TactileDepth.kt) — spec §4's hybrid-glass rule: "Do NOT
// use a bright Color.White border… highlight = very-low-alpha cool white/blue, shadow = deep
// navy/near-black." Unlike Batch 50 (which used pure Color.White/Color.Black as the alpha base),
// this spec gives its own tinted base colors for all three — TactileHighlight/TactileEdge/
// TactileShadow below are all spec §2 literal values now, not generic white/black at low alpha.
// tactileEmboss() further scales these down per-state (pressed vs. normal), it never raises them
// — see that file's own alpha comments.
val TactileHighlight = Color(0xFFEAF4FF).copy(alpha = 0.07f)
val TactileEdge = Color(0xFF8FB9E8).copy(alpha = 0.10f) // spec §4 single-border-color token, used where a bevel gradient isn't needed
val TactileShadow = Color(0xFF020817).copy(alpha = 0.68f)
