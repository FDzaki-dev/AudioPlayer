package com.rudi.audioplayer.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Readable glass surface for Compose.
 *
 * Important: Modifier.blur() blurs the composable's own content, not the pixels behind it.
 * Applying it to a container therefore also blurs its text and icons, which is the opposite
 * of what a usable frosted-glass surface should do. This implementation intentionally uses a
 * high-opacity tinted surface plus a subtle edge so content stays crisp and readable.
 *
 * The visual result is still glass-like when placed over artwork/ambient color, while avoiding
 * the common "beautiful but unreadable" failure mode caused by blurring the foreground.
 */
@Composable
fun Modifier.frostedGlass(
    tint: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = if (MaterialTheme.colorScheme.background == AppleLightBackground) 0.96f else 0.92f,
    blurRadius: Dp = 24.dp
): Modifier {
    // blurRadius is kept in the API for source compatibility with existing call sites.
    // Real backdrop blur is not performed here because Modifier.blur() would blur foreground
    // content. The surface tint is intentionally opaque enough to preserve contrast.
    val isMatte = MaterialTheme.colorScheme.background == MatteBackground
    // Shape now follows the active theme's own shape tokens instead of a hardcoded 24dp —
    // otherwise every sheet/mini-player using this modifier would keep Apple's soft rounding
    // even under Matte Noir's deliberately sharp/boxy identity.
    val shape = MaterialTheme.shapes.large
    val edge = if (isMatte)
        // A visible copper trim line instead of a faint neutral edge — reads as a machined
        // metal bezel around the panel, reinforcing the "premium hardware" depth cue.
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    else
        MaterialTheme.colorScheme.onSurface.copy(
            alpha = if (MaterialTheme.colorScheme.background == AppleLightBackground) 0.14f else 0.24f
        )
    return this
        .background(tint.copy(alpha = alpha), shape)
        .border(1.dp, edge, shape)
}
