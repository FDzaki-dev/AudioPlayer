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

// Matte Noir — the deliberate opposite of the Apple palette above: warm matte
// darks instead of true-black/true-white extremes, brushed-copper instead of
// cool blue, sharp corners instead of generous rounding. A single fixed
// "boutique" identity (doesn't follow system light/dark) for anyone who wants
// the app to feel like distinct premium hardware rather than an OS-native surface.
val MatteBackground = Color(0xFF14120F)
val MatteSurface = Color(0xFF1E1A16)
val MatteSurfaceVariant = Color(0xFF2A241D)
val MatteText = Color(0xFFEDE6DA)
val MatteSecondaryText = Color(0xFFA89A85)
val MatteAccent = Color(0xFFC9793C)
val MatteError = Color(0xFFE5584A)
val MatteSuccess = Color(0xFF6B8F5A)
