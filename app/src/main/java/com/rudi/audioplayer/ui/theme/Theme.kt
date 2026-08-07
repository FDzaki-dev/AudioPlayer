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
 * theme family — plus one fixed boutique identity (Matte Noir) for anyone who wants
 * the app to look like distinct premium hardware instead of an OS-native surface. */
enum class AppTheme(val storageKey: String, val displayName: String, val description: String) {
    SYSTEM("system", "Ikuti Sistem", "Menyesuaikan mode terang/gelap perangkat"),
    LIGHT("light", "Terang", "Latar putih bersih, khas iOS"),
    DARK("dark", "Gelap", "Hitam pekat, nyaman untuk layar OLED"),
    MATTE("matte_noir", "Matte Noir", "Hitam matte, aksen tembaga, judul serif — kebalikan gaya kaca Apple");

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
    error = Color(0xFFFF3B30)
)

private val MatteColors = darkColorScheme(
    primary = MatteAccent,
    onPrimary = Color.White,
    secondary = MatteSecondaryText,
    onSecondary = MatteBackground,
    tertiary = MatteSuccess,
    onTertiary = Color.White,
    background = MatteBackground,
    onBackground = MatteText,
    surface = MatteSurface,
    onSurface = MatteText,
    surfaceVariant = MatteSurfaceVariant,
    onSurfaceVariant = MatteSecondaryText,
    outline = MatteSurfaceVariant,
    error = MatteError
)

// A single, consistent "continuous curve" language across the whole app — Compose's Shapes
// API only supports true rounded rectangles (Apple's real squircle/superellipse corners
// aren't natively expressible), so generous rounding is the closest honest approximation.
val AppleShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

// Matte Noir's shape opposite: sharp, near-rectangular corners instead of Apple's
// generous rounding — reads as machined/boutique hardware rather than soft glass.
val MatteShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp)
)

@Composable
fun resolveIsDark(theme: AppTheme): Boolean = when (theme) {
    AppTheme.SYSTEM -> isSystemInDarkTheme()
    AppTheme.LIGHT -> false
    AppTheme.DARK -> true
    AppTheme.MATTE -> true
}

fun colorsFor(theme: AppTheme, isDark: Boolean) = when (theme) {
    AppTheme.MATTE -> MatteColors
    else -> if (isDark) AppleDarkColors else AppleLightColors
}

@Composable
fun AudioPlayerTheme(theme: AppTheme = AppTheme.SYSTEM, content: @Composable () -> Unit) {
    val isDark = resolveIsDark(theme)
    MaterialTheme(
        colorScheme = colorsFor(theme, isDark),
        typography = if (theme == AppTheme.MATTE) MatteTypography else AppleTypography,
        shapes = if (theme == AppTheme.MATTE) MatteShapes else AppleShapes,
        content = content
    )
}
