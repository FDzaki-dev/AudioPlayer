package com.rudi.audioplayer.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
 *
 * Batch 53 — compose-amoled-hybrid-glass-final.md is this project's primary material for the
 * Tactile identity (spec §4: "Every major elevated surface should feel like a translucent layer
 * floating above the AMOLED canvas"). This is the one shared call site every glass surface in the
 * app routes through (Home/Library cards, MiniPlayerBar, NowPlaying panels, every bottom sheet —
 * grep confirms), so the spec's glass rules are expressed centrally here rather than per screen.
 */
@Composable
fun Modifier.frostedGlass(
    tint: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = if (MaterialTheme.colorScheme.background == AppleLightBackground) 0.96f else 0.92f,
    blurRadius: Dp = 24.dp
): Modifier {
    // blurRadius is kept in the API for source compatibility with existing call sites.
    // Real backdrop blur is not performed here because Modifier.blur() would blur foreground
    // content. The surface tint is intentionally opaque enough to preserve contrast (spec §7:
    // "Text remains readable... Glass must not become milky").
    val isTactile = isTactileTheme()
    // Batch 57 — Skeuomorphism Dark Lite gets the same diagonal-bevel border treatment as
    // Tactile here (both are "physical panel" identities), just with its own tokens — only
    // Apple/Light/Dark fall back to the flat single-tone border below.
    val isSkeu = isSkeuTheme()
    // Shape now follows the active theme's own shape tokens instead of a hardcoded 24dp —
    // otherwise every sheet/mini-player using this modifier would keep Apple's soft rounding
    // even under Tactile's/Skeu's own shape identity.
    val shape = MaterialTheme.shapes.large
    // Batch 53 — spec §8 "Glass edge / highlight" + §9 "Lighting model" (single simulated light,
    // top-left -> bottom-right): a flat single-color border reads as a printed outline, not
    // reflected light. A diagonal two-stop brush (Highlight fading to Edge) is the minimum
    // structure needed to express "highlight top-left, recede bottom-right" without a bespoke
    // per-corner draw. Spec §8 explicitly forbids plain Color.White here — TactileHighlight/
    // TactileEdge are already pre-scaled low-alpha tokens (0.065f / 0.035f), never raised; Skeu's
    // own SkeuHighlight/SkeuEdge (Batch 57) follow the identical pre-scaled-token pattern.
    val edgeBrush = when {
        isTactile -> Brush.linearGradient(colors = listOf(TactileHighlight, TactileEdge))
        isSkeu -> Brush.linearGradient(colors = listOf(SkeuHighlight, SkeuEdge))
        else -> {
            val flat = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (MaterialTheme.colorScheme.background == AppleLightBackground) 0.14f else 0.24f
            )
            Brush.linearGradient(colors = listOf(flat, flat))
        }
    }
    return this
        .background(tint.copy(alpha = alpha), shape)
        .border(1.dp, edgeBrush, shape)
}
