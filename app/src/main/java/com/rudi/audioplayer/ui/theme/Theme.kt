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
    TACTILE("tactile_lite", "Tactile", "Bevel gelap AMOLED terprogram, bukan gambar — permukaan yang terasa bisa disentuh");

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

// Batch 50: darkColorScheme(), not lightColorScheme() — compose-skeuomorphism-lite-dark.md §1.1
// makes dark mandatory for this identity ("No component may silently fall back to a bright/
// light neumorphic appearance"), so the M3 scheme factory itself now matches. onPrimary/
// onTertiary picked by the same luminance rule MiniPlayerBar.kt already uses elsewhere in this
// app (>0.55 luminance -> black text): TactileAccent (0xFF4DA3FF) and TactileSuccess
// (0xFF34D399) are both light-ish, so black reads better on them than white.
private val TactileColors = darkColorScheme(
    primary = TactileAccent,
    onPrimary = Color.Black,
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

// A single, consistent "continuous curve" language across the whole app — Compose's Shapes
// API only supports true rounded rectangles (Apple's real squircle/superellipse corners
// aren't natively expressible), so generous rounding is the closest honest approximation.
val AppleShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

// Tactile's shape language — moderate, consistent rounding (not Apple's generous curve, not
// sharp/boxy like the old Matte identity) since the tactile spec's realism comes from the
// bevel/gradient/shadow treatment (tactileEmboss(), TactileDepth.kt), not from the shape itself.
val TactileShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun resolveIsDark(theme: AppTheme): Boolean = when (theme) {
    AppTheme.SYSTEM -> isSystemInDarkTheme()
    AppTheme.LIGHT -> false
    AppTheme.DARK -> true
    // Batch 50: flipped false -> true along with the palette itself — this also flips the
    // status bar/nav bar icon color to light (MainActivity.kt's `isAppearanceLightStatusBars =
    // !isDarkTheme`), which is now correct for the AMOLED-dark Tactile background instead of
    // leaving dark icons stranded on a near-black bar.
    AppTheme.TACTILE -> true
}

fun colorsFor(theme: AppTheme, isDark: Boolean) = when (theme) {
    AppTheme.TACTILE -> TactileColors
    else -> if (isDark) AppleDarkColors else AppleLightColors
}

@Composable
fun AudioPlayerTheme(theme: AppTheme = AppTheme.SYSTEM, content: @Composable () -> Unit) {
    val isDark = resolveIsDark(theme)
    MaterialTheme(
        colorScheme = colorsFor(theme, isDark),
        typography = if (theme == AppTheme.TACTILE) TactileTypography else AppleTypography,
        shapes = if (theme == AppTheme.TACTILE) TactileShapes else AppleShapes,
        content = content
    )
}
