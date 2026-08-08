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

// Tactile (Skeuomorphism-lite Literal Midnight Blue) — Batch 52: full repaint from the
// user-supplied compose-skeuomorphism-lite-midnight-blue.md spec, superseding the Batch 51
// hybrid-glass palette entirely. This spec's own title says "Literal" twice and its header
// states "Mandatory visual baseline: Literal Midnight Blue (#191970) — MANDATORY" — so §2's
// suggested-palette block is treated as exact literal values, same as every prior spec-driven
// Tactile batch. Two structural things this spec does NOT ask for, unlike Batch 51's source:
// (1) no gradient background stop pair (its §2 table lists `Background` as a single flat
// "Near-black AMOLED" tone, not two stops), and (2) no translucent-glass surfaces (§2's
// `Surface`/`SurfaceVariant` literals below are opaque 0xFF, not 0xCC/0xB8 like Batch 51) — so
// the gradient-root Box and glass-overlay wash both go away this batch (see MainActivity.kt/
// BlurUtils.kt), not because they were wrong before, but because this spec's own literal token
// set has nothing for them to express.
val TactileBackground = Color(0xFF191970) // spec §2 literal MidnightBlueBackground — flat, single-stop root/background color (isTactile guards key off this)
val TactileSurface = Color(0xFF161665) // spec §2 literal MidnightBlueSurface — opaque; slightly darker than TactileBackground per the spec's own literal (not a typo — this spec's Surface recedes rather than lifts)
val TactileSurfaceVariant = Color(0xFF20207A) // spec §2 literal MidnightBlueSurfaceVariant — opaque; lightest of the three background-family tones, used as the "raised" stop in tactileEmboss()'s bevel gradient
val TactileText = Color(0xFFF0F1FF) // spec §2 literal TextPrimary
val TactileSecondaryText = Color(0xFFBFC2E6) // spec §2 literal TextSecondary
val TactileAccent = Color(0xFF7278FF) // spec §2 literal MidnightBlueAccent (spec source names this "MidnightBlueMidnightBlueAccent", a doubled-name typo in the doc — kept in this project's existing TactileAccent naming convention instead)
// Not given literally by the spec (its §2 table lists the `Control`/`ControlPressed` roles but
// §2's code block only shows example values for the tokens above, same gap every prior batch's
// spec has left) — carried the Error/Success hues over from Batch 51 unchanged, still legible
// against the new Midnight Blue surfaces at normal text sizes; Control/ControlPressed re-derived
// below to sit correctly in the new (lighter, more saturated) surface hierarchy instead of the
// old near-black one.
val TactileError = Color(0xFFFF6B6B)
val TactileSuccess = Color(0xFF34D399)

// Interactive-control pair from spec §2's token table (`Control` / `ControlPressed`) — "distinct
// Midnight Blue surface" for tactile controls, "darker/recessed surface" when pressed. No
// literal value given by the spec for these two (same gap as every prior batch); still unused by
// any call site (grep confirms), prepared for the future TactileButton/TactileSwitch/
// TactileSlider components spec §7/§12 asks for but this batch still doesn't build (see
// TactileDepth.kt doc). Re-tuned this batch to read as a step apart from TactileSurfaceVariant
// (lighter/more distinct) and TactileSurface (darker when pressed), matching the new hierarchy.
val TactileControl = Color(0xFF23238A)
val TactileControlPressed = Color(0xFF0F0F4A)

// Bevel pair used by tactileEmboss() (TactileDepth.kt) — spec §4's Midnight Blue rule: "Do NOT
// use a bright Color.White border… highlight = very-low-alpha light/primary tone, shadow =
// very-dark neutral." Unlike Batch 51 (which gave its own tinted base colors for these three),
// this spec goes back to plain Color.White/Color.Black as the alpha base — TactileHighlight/
// TactileEdge/TactileShadow below are spec §2 literal values verbatim, including their baked-in
// alpha. tactileEmboss() further scales some of these down per-state (pressed vs. normal) where
// noted in that file's own comments; it never raises them above the literal base.
val TactileHighlight = Color.White.copy(alpha = 0.055f)
val TactileEdge = Color.White.copy(alpha = 0.035f) // spec §4/§2 single-border-color token ("Border" in the §2 table), used where a bevel gradient isn't needed
val TactileShadow = Color.Black.copy(alpha = 0.65f)
