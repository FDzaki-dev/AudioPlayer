package com.rudi.audioplayer.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
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
    // Batch 61 — was `if (background == AppleLightBackground)`, a comparison that only ever
    // matched the Apple identity's own light background, so Tactile/Skeu's light expressions
    // (different background tokens entirely) silently fell through to the dark-tuned 0.92f.
    // LocalIsDarkTheme is identity-agnostic by construction, so this now works correctly no
    // matter which of the 3 identities is active.
    alpha: Float = if (LocalIsDarkTheme.current) 0.92f else 0.96f,
    blurRadius: Dp = 24.dp
): Modifier {
    // blurRadius is kept in the API for source compatibility with existing call sites.
    // Real backdrop blur is not performed here because Modifier.blur() would blur foreground
    // content. The surface tint is intentionally opaque enough to preserve contrast (spec §7:
    // "Text remains readable... Glass must not become milky").
    val isTactile = isTactileTheme()
    val isSkeu = isSkeuTheme()
    val isDark = LocalIsDarkTheme.current
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
    // Batch 61 — still forced opaque in BOTH modes: "solid panel, not glass" is an identity trait
    // of Skeu, not something the light/dark toggle should be able to override.
    val effectiveAlpha = if (isSkeu) 1f else alpha
    // Batch 53 — spec §8 "Glass edge / highlight" + §9 "Lighting model" (single simulated light,
    // top-left -> bottom-right): a flat single-color border reads as a printed outline, not
    // reflected light. A diagonal two-stop brush (Highlight fading to a second stop) is the
    // minimum structure needed to express "highlight top-left, recede bottom-right" without a
    // bespoke per-corner draw. Batch 61 — both Tactile and Skeu now branch on `isDark` for their
    // own light-tuned token pair (Color.kt "LIGHT VARIANT" sections); Apple's flat branch already
    // handled its own light/dark via the background comparison below, unchanged.
    val edgeBrush = when {
        isTactile -> Brush.linearGradient(
            colors = if (isDark) listOf(TactileHighlight, TactileEdge) else listOf(TactileLightHighlight, TactileLightEdge)
        )
        // Batch 73 — Hyper-Realism: no longer a plain 2-stop diagonal (that was structurally
        // identical to Tactile's own edgeBrush, exactly the "not autonomous" gap this batch
        // fixes). Skeu's panel edge now reads as a brushed-metal rim: a short repeating
        // highlight/shadow segment (TileMode.Repeat — same technique as skeuEmboss()'s grain
        // overlay in TactileDepth.kt) instead of one smooth gradient sweep.
        isSkeu -> Brush.linearGradient(
            colors = if (isDark) listOf(SkeuHighlight, SkeuShadow) else listOf(SkeuLightHighlight, SkeuLightShadow),
            start = Offset(0f, 0f),
            end = Offset(6f, 6f),
            tileMode = TileMode.Repeat
        )
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
