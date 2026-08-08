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

// Tactile (Skeuomorphism-lite) — Batch 49: replaces Matte Noir entirely, per the user-supplied
// compose-skeuomorphism-lite.md spec. Deliberately a LIGHT palette — the spec's own §1 example
// gradient stops (0xFFF8FAFC top-highlight -> 0xFFE2E8F0 shadow-side) are used verbatim as the
// surface gradient here, not just as inspiration. Like Matte Noir before it, a single fixed
// "boutique" identity that doesn't follow system light/dark — the spec's own §Accessibility
// "Dark Mode Adaptation" guidance (swap highlights for glows) is a follow-up for a future dark
// variant, out of scope for this batch to keep the theme-system replacement atomic.
val TactileBackground = Color(0xFFEEF1F5)
val TactileSurfaceHighlight = Color(0xFFF8FAFC) // spec §1 literal top-of-gradient stop
val TactileSurfaceShadow = Color(0xFFE2E8F0) // spec §1 literal bottom-of-gradient stop
val TactileSurfaceVariant = Color(0xFFE2E8F0)
val TactileText = Color(0xFF1E293B)
val TactileSecondaryText = Color(0xFF64748B)
val TactileAccent = Color(0xFFB8622A) // warm burnt-orange, tactile-hardware mood, distinct hue from the old Matte copper
val TactileError = Color(0xFFDC2626)
val TactileSuccess = Color(0xFF16A34A)

// Bevel pair used by tactileEmboss() (TactileDepth.kt) — the spec's §1 "layering contrasting
// light and dark borders" instruction, literally: a bright top-edge highlight fading down, a
// muted slate shadow-edge fading up. TactileShadow is a muted slate gray, NOT near-black — this
// theme has no true-black surface anywhere, so a shadow tuned for a dark theme (like Matte's old
// MatteUmbra) would just look like a stray dark smudge here.
val TactileHighlight = Color(0xFFFFFFFF)
val TactileShadow = Color(0xFF94A3B8)
