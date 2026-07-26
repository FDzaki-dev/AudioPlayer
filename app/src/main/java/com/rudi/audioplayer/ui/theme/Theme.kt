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

/** Light / Dark / Ikuti Sistem — the standard iOS appearance picker, replacing the old
 * five-identity theme system with Apple's own simpler model. */
enum class AppTheme(val storageKey: String, val displayName: String, val description: String) {
    SYSTEM("system", "Ikuti Sistem", "Menyesuaikan mode terang/gelap perangkat"),
    LIGHT("light", "Terang", "Latar putih bersih, khas iOS"),
    DARK("dark", "Gelap", "Hitam pekat, nyaman untuk layar OLED");

    companion object {
        fun fromStorageKey(key: String?): AppTheme = entries.find { it.storageKey == key } ?: SYSTEM
    }
}

private val AppleDarkColors = darkColorScheme(
    primary = AppleAccent,
    onPrimary = Color.White,
    secondary = AppleDarkSecondaryText,
    onSecondary = AppleDarkBackground,
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
    background = AppleLightBackground,
    onBackground = AppleLightText,
    surface = AppleLightSurface,
    onSurface = AppleLightText,
    surfaceVariant = AppleLightSurfaceVariant,
    onSurfaceVariant = AppleLightSecondaryText,
    outline = AppleLightSurfaceVariant,
    error = Color(0xFFFF3B30)
)

// A single, consistent "continuous curve" language across the whole app — Compose's Shapes
// API only supports true rounded rectangles (Apple's real squircle/superellipse corners
// aren't natively expressible), so generous rounding is the closest honest approximation.
val AppleShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun resolveIsDark(theme: AppTheme): Boolean = when (theme) {
    AppTheme.SYSTEM -> isSystemInDarkTheme()
    AppTheme.LIGHT -> false
    AppTheme.DARK -> true
}

fun colorsFor(isDark: Boolean) = if (isDark) AppleDarkColors else AppleLightColors

@Composable
fun AudioPlayerTheme(theme: AppTheme = AppTheme.SYSTEM, content: @Composable () -> Unit) {
    val isDark = resolveIsDark(theme)
    MaterialTheme(
        colorScheme = colorsFor(isDark),
        typography = AppleTypography,
        shapes = AppleShapes,
        content = content
    )
}
