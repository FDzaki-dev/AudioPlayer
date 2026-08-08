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

// Tactile (Skeuomorphism-lite) — Batch 50: full repaint from the user-supplied
// compose-skeuomorphism-lite-dark.md spec, which supersedes the Batch 49 light palette below
// entirely. Spec §1.1 is explicit: "Do not simply invert a light theme" — this is a from-scratch
// dark/AMOLED-first token set, not the old light values darkened. §2's suggested palette block
// is used verbatim as literal values (not just inspiration), same treatment Batch 49 gave the
// light spec's own example stops.
val TactileBackground = Color(0xFF05070A) // spec §2 literal DarkBackground — near-black, AMOLED-safe
val TactileSurface = Color(0xFF0B0F14) // spec §2 literal DarkSurface — main panels
val TactileSurfaceVariant = Color(0xFF111720) // spec §2 literal DarkSurfaceVariant — secondary panels, one step lighter
val TactileText = Color(0xFFE8EEF5) // spec §2 literal TextPrimary
val TactileSecondaryText = Color(0xFFA8B3C0) // spec §2 literal TextSecondary
val TactileAccent = Color(0xFF4DA3FF) // spec §2 literal Accent — cool blue; replaces the old light-theme burnt-orange, per spec §1.1 "design specifically for dark surfaces" rather than recolor-in-place
// Not given literally by the spec (its §2 table lists the roles but only shows example values
// for the ones above) — chosen to fit the same cool/restrained AMOLED palette, legible on
// TactileBackground/TactileSurface at normal text sizes.
val TactileError = Color(0xFFFF6B6B)
val TactileSuccess = Color(0xFF34D399)

// Interactive-control pair from spec §2's token table (`Control` / `ControlPressed`) — "distinct
// dark surface" for tactile controls, "darker/recessed surface" when pressed. No literal value
// given by the spec for these two; placed one step lighter than TactileSurfaceVariant (Control,
// so it reads as "lifted" against structural panels) and one step darker than TactileBackground
// itself (ControlPressed, so a press genuinely recesses below the app's own floor).
val TactileControl = Color(0xFF141B24)
val TactileControlPressed = Color(0xFF080B10)

// Bevel pair used by tactileEmboss() (TactileDepth.kt) — spec §4's dark-mode rule: "Do NOT use a
// bright Color.White border… highlight = very-low-alpha light tone, shadow = very-dark neutral."
// TactileHighlight/TactileEdge are spec §2 literal values (Color.White at 0.055f/0.035f alpha —
// deliberately near-invisible on their own, not a flat opaque color); TactileShadow is also
// spec-literal (Color.Black at 0.65f). tactileEmboss() further scales these down per-state
// (pressed vs. normal), it never raises them — see that file's own alpha comments.
val TactileHighlight = Color.White.copy(alpha = 0.055f)
val TactileEdge = Color.White.copy(alpha = 0.035f) // spec §4 single-border-color token, used where a bevel gradient isn't needed
val TactileShadow = Color.Black.copy(alpha = 0.65f)
