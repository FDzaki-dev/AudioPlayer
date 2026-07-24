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
 * corner-radius language — not just a recolor of the same layout. All five
 * are deliberately low-contrast: no near-black/near-white extremes anywhere,
 * so long listening sessions stay easy on the eyes regardless of which one
 * is picked. */
enum class AppTheme(val storageKey: String, val displayName: String, val description: String) {
    DEFAULT("default", "Default", "Netral dan tenang — abu slate lembut dengan aksen biru dusty"),
    INK_BRASS("ink_brass", "Ink & Brass", "Boutique hi-fi — emas hangat yang lembut di atas charcoal hangat"),
    MIDNIGHT_BLOOM("midnight_bloom", "Midnight Bloom", "Jewel-tone tenang — rose-mauve dusty di atas aubergine lembut"),
    PAPER_INK("paper_ink", "Paper & Ink", "Terang, gaya editorial — krim hangat dan clay lembut"),
    BOTANICAL("botanical", "Botanical", "Tenang dan alami — hijau sage lembut di atas hutan gelap");

    companion object {
        fun fromStorageKey(key: String?): AppTheme = entries.find { it.storageKey == key } ?: DEFAULT
    }
}

private val DefaultColors = darkColorScheme(
    primary = SteelBlue,
    onPrimary = Slate,
    secondary = SlateMist,
    onSecondary = Slate,
    background = Slate,
    onBackground = SlateText,
    surface = SlateSurface,
    onSurface = SlateText,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateMist,
    outline = SlateSurfaceVariant
)

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

private val BotanicalColors = darkColorScheme(
    primary = Sage,
    onPrimary = Forest,
    secondary = Olive,
    onSecondary = Forest,
    background = Forest,
    onBackground = ForestText,
    surface = ForestSurface,
    onSurface = ForestText,
    surfaceVariant = ForestSurfaceVariant,
    onSurfaceVariant = Olive,
    outline = ForestSurfaceVariant
)

// Corner language differs per theme too: Default stays close to standard
// Material rounding (the neutral choice); Ink & Brass keeps its softly
// rounded signature; Midnight Bloom goes almost pill-shaped for a louder,
// clubbier feel; Paper & Ink stays crisp and barely-rounded for a printed
// feel; Botanical goes generously soft and organic.
private val DefaultShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)
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
private val BotanicalShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

fun colorsFor(theme: AppTheme) = when (theme) {
    AppTheme.DEFAULT -> DefaultColors
    AppTheme.INK_BRASS -> InkBrassColors
    AppTheme.MIDNIGHT_BLOOM -> MidnightBloomColors
    AppTheme.PAPER_INK -> PaperInkColors
    AppTheme.BOTANICAL -> BotanicalColors
}

fun typographyFor(theme: AppTheme): Typography = when (theme) {
    AppTheme.DEFAULT -> DefaultTypography
    AppTheme.INK_BRASS -> InkBrassTypography
    AppTheme.MIDNIGHT_BLOOM -> MidnightBloomTypography
    AppTheme.PAPER_INK -> PaperInkTypography
    AppTheme.BOTANICAL -> BotanicalTypography
}

fun shapesFor(theme: AppTheme) = when (theme) {
    AppTheme.DEFAULT -> DefaultShapes
    AppTheme.INK_BRASS -> InkBrassShapes
    AppTheme.MIDNIGHT_BLOOM -> MidnightBloomShapes
    AppTheme.PAPER_INK -> PaperInkShapes
    AppTheme.BOTANICAL -> BotanicalShapes
}

@Composable
fun AudioPlayerTheme(theme: AppTheme = AppTheme.DEFAULT, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorsFor(theme),
        typography = typographyFor(theme),
        shapes = shapesFor(theme),
        content = content
    )
}
