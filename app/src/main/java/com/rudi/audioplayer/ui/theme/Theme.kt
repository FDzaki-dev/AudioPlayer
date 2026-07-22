package com.rudi.audioplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** The app's selectable themes. Each is a full identity — palette, type, and
 * corner-radius language — not just a recolor of the same layout. */
enum class AppTheme(val storageKey: String, val displayName: String, val description: String) {
    INK_BRASS("ink_brass", "Ink & Brass", "Gelap, boutique hi-fi — emas hangat di atas hitam pekat"),
    MIDNIGHT_BLOOM("midnight_bloom", "Midnight Bloom", "Gelap, jewel-tone — rose-orchid lembut di atas aubergine pekat"),
    PAPER_INK("paper_ink", "Paper & Ink", "Terang, gaya editorial — krem hangat dan sienna terbakar");

    companion object {
        fun fromStorageKey(key: String?): AppTheme = entries.find { it.storageKey == key } ?: INK_BRASS
    }
}

private val InkBrassColors = darkColorScheme(
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

private val MidnightBloomColors = darkColorScheme(
    primary = Magenta,
    onPrimary = Plum,
    secondary = Lavender,
    onSecondary = Plum,
    background = Plum,
    onBackground = Bloom,
    surface = PlumSurface,
    onSurface = Bloom,
    surfaceVariant = PlumSurfaceVariant,
    onSurfaceVariant = Lavender,
    outline = PlumSurfaceVariant
)

private val PaperInkColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Paper,
    secondary = WarmGrey,
    onSecondary = Paper,
    background = Paper,
    onBackground = InkText,
    surface = PaperSurface,
    onSurface = InkText,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = WarmGrey,
    outline = PaperSurfaceVariant
)

// Corner language differs per theme too: Ink & Brass keeps the softly rounded
// default; Midnight Bloom goes almost pill-shaped for a louder, clubbier
// feel; Paper & Ink goes crisp and barely-rounded for a printed-page feel.
private val InkBrassShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)
private val MidnightBloomShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)
private val PaperInkShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

fun colorsFor(theme: AppTheme) = when (theme) {
    AppTheme.INK_BRASS -> InkBrassColors
    AppTheme.MIDNIGHT_BLOOM -> MidnightBloomColors
    AppTheme.PAPER_INK -> PaperInkColors
}

fun typographyFor(theme: AppTheme): Typography = when (theme) {
    AppTheme.INK_BRASS -> InkBrassTypography
    AppTheme.MIDNIGHT_BLOOM -> MidnightBloomTypography
    AppTheme.PAPER_INK -> PaperInkTypography
}

fun shapesFor(theme: AppTheme) = when (theme) {
    AppTheme.INK_BRASS -> InkBrassShapes
    AppTheme.MIDNIGHT_BLOOM -> MidnightBloomShapes
    AppTheme.PAPER_INK -> PaperInkShapes
}

@Composable
fun AudioPlayerTheme(theme: AppTheme = AppTheme.INK_BRASS, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorsFor(theme),
        typography = typographyFor(theme),
        shapes = shapesFor(theme),
        content = content
    )
}
