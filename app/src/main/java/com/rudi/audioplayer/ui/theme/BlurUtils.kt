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
    val isSkeu = isSkeuTheme()
    // Shape now follows the active theme's own shape tokens instead of a hardcoded 24dp —
    // otherwise every sheet/mini-player using this modifier would keep Apple's soft rounding
    // even under Tactile's/Skeu's own shape identity.
    val shape = MaterialTheme.shapes.large
    // Batch 58 — user feedback: Skeu still reads as glassmorphism. Root cause: this is the one
    // shared helper every panel in the app routes through (mini player, every bottom sheet,
    // Home/Library cards — file header above), and it is literally built as translucent glass
    // (tint at <1 alpha + a soft highlight rim). Skeu's own identity (PROJECT_STATE Batch 57) is
    // explicitly "panel solid, bukan lapisan kaca", so it never should have inherited that look.
    // Forced to full opacity here regardless of the `alpha` param — no call site in this codebase
    // passes one explicitly (grepped), so this can't silently clobber an intentional override.
    val effectiveAlpha = if (isSkeu) 1f else alpha
    // Batch 53 — spec §8 "Glass edge / highlight" + §9 "Lighting model" (single simulated light,
    // top-left -> bottom-right): a flat single-color border reads as a printed outline, not
    // reflected light. A diagonal two-stop brush (Highlight fading to a second stop) is the
    // minimum structure needed to express "highlight top-left, recede bottom-right" without a
    // bespoke per-corner draw. Spec §8 explicitly forbids plain Color.White here — Tactile keeps
    // its original pre-scaled low-alpha pair (TactileHighlight/TactileEdge, 0.065f/0.035f),
    // unchanged, since Tactile's identity is deliberately glass and isn't what this batch fixes.
    val edgeBrush = when {
        isTactile -> Brush.linearGradient(colors = listOf(TactileHighlight, TactileEdge))
        // Batch 58 — second stop swapped from SkeuEdge (a near-invisible 0.12f rim, the same
        // "soft glass sheen" shape Tactile uses) to SkeuShadow (0.55f, a real carved shadow).
        // The border now reads as an engraved bevel — catch-light fading into recessed shadow —
        // instead of a glowing glass-card rim, matching skeuEmboss()'s own border language
        // (TactileDepth.kt) so panel edges and control edges read as one consistent material.
        isSkeu -> Brush.linearGradient(colors = listOf(SkeuHighlight, SkeuShadow))
        else -> {
            val flat = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (MaterialTheme.colorScheme.background == AppleLightBackground) 0.14f else 0.24f
            )
            Brush.linearGradient(colors = listOf(flat, flat))
        }
    }
    // Batch 58 — Skeu's now-stronger bevel border reads better a hair over the glass-theme
    // hairline (1.dp); Tactile/Apple unchanged.
    val edgeWidth = if (isSkeu) 1.5.dp else 1.dp
    return this
        .background(tint.copy(alpha = effectiveAlpha), shape)
        .border(edgeWidth, edgeBrush, shape)
}
