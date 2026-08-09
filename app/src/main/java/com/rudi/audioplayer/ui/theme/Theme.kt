package com.rudi.audioplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Light / Dark / Ikuti Sistem — the standard iOS appearance picker, main/default
 * theme family — plus one fixed boutique identity (Tactile) for anyone who wants
 * the app to look like distinct programmatic hardware instead of an OS-native surface. */
enum class AppTheme(val storageKey: String, val displayName: String, val description: String) {
    SYSTEM("system", "Ikuti Sistem", "Menyesuaikan mode terang/gelap perangkat"),
    LIGHT("light", "Terang", "Latar putih bersih, khas iOS"),
    DARK("dark", "Gelap", "Hitam pekat, nyaman untuk layar OLED"),
    TACTILE("tactile_lite", "Tactile", "AMOLED hybrid glassmorphism — kaca gelap premium dengan sentuhan Midnight Blue tipis dan kontrol taktil"),
    // Batch 57 — kedua custom theme (bukan Light/Dark/System) sekarang identitas fisik yang
    // beda pendekatan: Tactile = kaca gelap AMOLED, ini = panel timbul charcoal hangat.
    SKEU_DARK_LITE("skeu_dark_lite", "Skeuomorphism Dark Lite", "Panel gelap netral timbul dengan bevel lembut dan aksen tembaga hangat");

    companion object {
        fun fromStorageKey(key: String?): AppTheme = entries.find { it.storageKey == key } ?: SYSTEM
    }
}

private val AppleDarkColors = darkColorScheme(
    primary = AppleAccent,
    onPrimary = Color.White,
    secondary = AppleDarkSecondaryText,
    onSecondary = AppleDarkBackground,
    tertiary = AppleDarkSuccess,
    onTertiary = Color.Black,
    background = AppleDarkBackground,
    onBackground = AppleDarkText,
    surface = AppleDarkSurface,
    onSurface = AppleDarkText,
    surfaceVariant = AppleDarkSurfaceVariant,
    onSurfaceVariant = AppleDarkSecondaryText,
    outline = AppleDarkSurfaceVariant,
    // Unset previously — M3's darkColorScheme() factory silently fills any omitted role with
    // its own purple baseline default, so every elevated Surface/Card/NavigationBar using
    // automatic tonal elevation was tinting toward baseline purple instead of this app's own
    // accent. Explicit surfaceTint fixes that leak for every M3 elevation overlay app-wide.
    surfaceTint = AppleAccent,
    error = Color(0xFFFF453A)
)

private val AppleLightColors = lightColorScheme(
    primary = AppleAccent,
    onPrimary = Color.White,
    secondary = AppleLightSecondaryText,
    onSecondary = AppleLightBackground,
    tertiary = AppleLightSuccess,
    onTertiary = Color.White,
    background = AppleLightBackground,
    onBackground = AppleLightText,
    surface = AppleLightSurface,
    onSurface = AppleLightText,
    surfaceVariant = AppleLightSurfaceVariant,
    onSurfaceVariant = AppleLightSecondaryText,
    outline = AppleLightSurfaceVariant,
    surfaceTint = AppleAccent,
    error = Color(0xFFFF3B30)
)

// Batch 53: still darkColorScheme() — compose-amoled-hybrid-glass-final.md §24 carries the
// "no light-mode fallback" mandate forward implicitly (a light AMOLED surface is a contradiction
// in terms), it just changes what "dark" looks like again (AMOLED-black foundation, translucent
// glass surfaces, Midnight Blue demoted to an ambient-only ingredient — spec §6). onPrimary
// picked by the same luminance rule MiniPlayerBar.kt uses elsewhere (>0.55 luminance -> black
// text): TactileAccent (0xFF6670FF) simple luma ≈0.49, below the threshold, so onPrimary stays
// Color.White. TactileSuccess (0xFF34D399, unchanged) stays above the threshold, so onTertiary
// stays Color.Black as before.
private val TactileColors = darkColorScheme(
    primary = TactileAccent,
    onPrimary = Color.White,
    secondary = TactileSecondaryText,
    onSecondary = TactileBackground,
    tertiary = TactileSuccess,
    onTertiary = Color.Black,
    background = TactileBackground,
    onBackground = TactileText,
    surface = TactileSurface,
    onSurface = TactileText,
    surfaceVariant = TactileSurfaceVariant,
    onSurfaceVariant = TactileSecondaryText,
    outline = TactileSurfaceVariant,
    surfaceTint = TactileAccent,
    error = TactileError
)

// Batch 57 — Skeuomorphism Dark Lite's own color role mapping. onPrimary picked by the same
// luminance rule used elsewhere (>0.55 luminance -> black text): SkeuDarkAccent (0xFFCB8B4B)
// simple luma ≈0.60, above the threshold, so onPrimary is Color.Black (unlike Apple/Tactile's
// cooler, darker accents which stay Color.White).
private val SkeuDarkColors = darkColorScheme(
    primary = SkeuDarkAccent,
    onPrimary = Color.Black,
    secondary = SkeuDarkSecondaryText,
    onSecondary = SkeuDarkBackground,
    tertiary = SkeuDarkSuccess,
    onTertiary = Color.Black,
    background = SkeuDarkBackground,
    onBackground = SkeuDarkText,
    surface = SkeuDarkSurface,
    onSurface = SkeuDarkText,
    surfaceVariant = SkeuDarkSurfaceVariant,
    onSurfaceVariant = SkeuDarkSecondaryText,
    outline = SkeuDarkSurfaceVariant,
    surfaceTint = SkeuDarkAccent,
    error = SkeuDarkError
)

// A single, consistent "continuous curve" language across the whole app — Compose's Shapes
// API only supports true rounded rectangles (Apple's real squircle/superellipse corners
// aren't natively expressible), so generous rounding is the closest honest approximation.
val AppleShapes = Shapes(
    small = RoundedCornerShape(Radius.ml),
    medium = RoundedCornerShape(Radius.xxl),
    large = RoundedCornerShape(Radius.hero)
)

// Tactile's shape language — moderate, consistent rounding (not Apple's generous curve, not
// sharp/boxy like the old Matte identity) since the tactile spec's realism comes from the
// bevel/gradient/shadow treatment (tactileEmboss(), TactileDepth.kt), not from the shape itself.
val TactileShapes = Shapes(
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg)
)

// Batch 57 — Skeuomorphism Dark Lite's shape language: one notch more rounded than Tactile at
// every step (md/lg/xxl vs Tactile's sm/md/lg), reading as soft physical buttons/panels rather
// than Tactile's moderate glass-panel rounding or Apple's generous continuous curve. Distinct
// 3-value set from both other theme families so no two themes share a shape token.
val SkeuDarkShapes = Shapes(
    small = RoundedCornerShape(Radius.md),
    medium = RoundedCornerShape(Radius.lg),
    large = RoundedCornerShape(Radius.xxl)
)

@Composable
fun resolveIsDark(theme: AppTheme): Boolean = when (theme) {
    AppTheme.SYSTEM -> isSystemInDarkTheme()
    AppTheme.LIGHT -> false
    AppTheme.DARK -> true
    // Still true since Batch 50 — the literal Midnight Blue repaint this batch (spec
    // compose-skeuomorphism-lite-midnight-blue.md) is still unambiguously dark, so status
    // bar/nav bar icons stay light (MainActivity.kt's `isAppearanceLightStatusBars =
    // !isDarkTheme`), same as before.
    AppTheme.TACTILE -> true
    // Batch 57 — "Dark Lite" in the name is its identity, not a light-mode hint: no light
    // variant this batch, same precedent as Tactile (no light-mode fallback).
    AppTheme.SKEU_DARK_LITE -> true
}

// Batch 54 (technical debt pass) — this exact comparison
// (`MaterialTheme.colorScheme.background == TactileBackground`) was hand-duplicated in 6 places
// across the codebase (BlurUtils.kt, TactileDepth.kt call sites in HomeScreen/LibraryScreen/
// MiniPlayerBar/NowPlayingScreen x2) every time a new screen needed to branch on the active
// theme's identity. One shared helper here means future call sites (and any future rename of
// the underlying identity token) only need to change in one place.
@Composable
fun isTactileTheme(): Boolean = MaterialTheme.colorScheme.background == TactileBackground

// Batch 57 — same pattern as isTactileTheme() (Batch 54 dedup rationale applies identically):
// one shared helper instead of hand-duplicating this comparison at every future call site that
// needs to branch on the Skeu identity (frostedGlass() this batch; more likely as the theme
// gets polish passes the same way Tactile did across Batch 49-55).
@Composable
fun isSkeuTheme(): Boolean = MaterialTheme.colorScheme.background == SkeuDarkBackground

fun colorsFor(theme: AppTheme, isDark: Boolean) = when (theme) {
    AppTheme.TACTILE -> TactileColors
    AppTheme.SKEU_DARK_LITE -> SkeuDarkColors
    else -> if (isDark) AppleDarkColors else AppleLightColors
}

@Composable
fun AudioPlayerTheme(theme: AppTheme = AppTheme.SYSTEM, content: @Composable () -> Unit) {
    val isDark = resolveIsDark(theme)
    MaterialTheme(
        colorScheme = colorsFor(theme, isDark),
        // Batch 57 — Skeu reuses AppleTypography (no separate type-scale spec supplied for this
        // theme; skeuomorphic identity here is carried by color/shape/bevel, not custom type).
        typography = if (theme == AppTheme.TACTILE) TactileTypography else AppleTypography,
        shapes = when (theme) {
            AppTheme.TACTILE -> TactileShapes
            AppTheme.SKEU_DARK_LITE -> SkeuDarkShapes
            else -> AppleShapes
        },
        content = content
    )
}
