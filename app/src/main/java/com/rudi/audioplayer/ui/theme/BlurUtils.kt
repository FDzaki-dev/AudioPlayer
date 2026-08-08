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
    val isTactile = MaterialTheme.colorScheme.background == TactileBackground
    // Shape now follows the active theme's own shape tokens instead of a hardcoded 24dp —
    // otherwise every sheet/mini-player using this modifier would keep Apple's soft rounding
    // even under Tactile's own shape identity.
    val shape = MaterialTheme.shapes.large
    val edge = if (isTactile)
        // A visible accent trim line instead of a faint neutral edge — reads as a machined
        // bezel around the panel, reinforcing the tactile depth cue. Alpha stays 0.22f from
        // Batch 50 for the spec's §9/§13 restrained-glow rule; TactileAccent's Batch 51 value
        // (0xFF5B9DFF) is close enough in brightness to the old one that this didn't need
        // re-tuning.
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    else
        MaterialTheme.colorScheme.onSurface.copy(
            alpha = if (MaterialTheme.colorScheme.background == AppleLightBackground) 0.14f else 0.24f
        )
    return if (isTactile) {
        // Batch 51 — compose-skeuomorphism-lite-hybrid-glass-dark-blue.md §2/§8: TactileSurface
        // now carries its own baked-in translucency (0xCC alpha, spec-literal DarkSurface), so
        // this branch paints `tint` at its own native alpha instead of the generic branch's
        // `.copy(alpha = alpha)` — that would *replace* the spec's ~80%-opacity glass with a
        // flat 0.92f, discarding the exact translucency the hybrid-glass surface system is built
        // on. A second, very-low-alpha TactileGlassOverlay wash (also spec §2 literal) layers a
        // faint cool-blue tint on top, matching §8's layered glass formula ("translucent navy
        // fill" + "subtle linear/radial gradient") without a real blur pass.
        this
            .background(tint, shape)
            .background(TactileGlassOverlay, shape)
            .border(1.dp, edge, shape)
    } else {
        this
            .background(tint.copy(alpha = alpha), shape)
            .border(1.dp, edge, shape)
    }
}
