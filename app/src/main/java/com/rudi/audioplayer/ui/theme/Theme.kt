package com.rudi.audioplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PremiumDarkColors = darkColorScheme(
    primary = Brass,
    onPrimary = Ink,
    secondary = Mist,
    onSecondary = Ink,
    background = Ink,
    onBackground = Parchment,
    surface = InkSurface,
    onSurface = Parchment,
    surfaceVariant = InkSurfaceVariant,
    onSurfaceVariant = Mist,
    outline = InkSurfaceVariant
)

@Composable
fun AudioPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PremiumDarkColors,
        typography = Typography,
        content = content
    )
}
