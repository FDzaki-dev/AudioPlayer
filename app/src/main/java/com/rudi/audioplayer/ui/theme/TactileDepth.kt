package com.rudi.audioplayer.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Batch 52 — repainted again for the user-supplied compose-skeuomorphism-lite-midnight-blue.md
 * spec, which supersedes Batch 51's hybrid-glass version of this same function. Structure/
 * signature is still unchanged (all 8 call sites keep working unmodified) — only token *values*
 * changed, entirely inside Color.kt: TactileSurfaceVariant/TactileSurface are opaque again (spec
 * §2 literal 0xFF, not Batch 51's 0xB8/0xCC translucent alpha), so the same
 * `Brush.verticalGradient(TactileSurfaceVariant, TactileSurface)` call below now paints a plain
 * opaque bevel again — there is no glass/backdrop concept in this spec.
 *  1. Tactile depth (spec §4) — top-down `Brush.verticalGradient` using the spec's own literal
 *     §2 tokens, plus a bevel border. Spec §4's Midnight Blue rule: "Do NOT use a bright
 *     Color.White border… highlight = very-low-alpha light/primary tone, shadow = very-dark
 *     neutral" — TactileHighlight/TactileShadow are plain white/black-based again this batch
 *     (see Color.kt), each with its own low baked-in alpha (0.055f/0.65f). The border-top alpha
 *     below is now set to match TactileHighlight's own literal alpha exactly for the normal
 *     state (rather than an independently-tuned higher number like Batch 50/51 used), since the
 *     spec's own literal highlight alpha is already this low; the drop-shadow alpha is likewise
 *     re-matched to TactileShadow's own literal 0.65f base for the same reason.
 *  2. Micro-interactions (spec §6) — unchanged mechanism from Batch 49-51 (`pressed` still drives
 *     real animated depression via `animateDpAsState`/`animateFloatAsState`), still hand-drawn
 *     via `drawBehind` + `Outline`->`Path` rather than native `Modifier.shadow` for the same
 *     reason as ever: a black-on-near-black native shadow is exactly the failure mode that took
 *     5 batches to fix for Matte Noir (see PROJECT_STATE.md Batch 39-44) — this app's own alpha
 *     stays the single source of contrast truth instead of trusting platform shadow rendering.
 *  3. Restrained skeuomorphism (spec §1.3/§8) — this stays the flat/structural-card treatment
 *     only; spec §7's fully tactile Button/Toggle/Slider components are a separate, larger
 *     scope (new files under `ui/components/`, per spec §12) not built this batch either —
 *     sliders/toggles/switches remain plain Material3, same boundary carried over from Batch
 *     49-51.
 */
@Composable
fun Modifier.tactileEmboss(
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 8.dp,
    pressed: Boolean = false
): Modifier {
    val animatedElevation by animateDpAsState(
        targetValue = if (pressed) elevation / 4 else elevation,
        label = "tactileEmbossElevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        label = "tactileEmbossScale"
    )
    // Border stays a whisper per spec §4/§13 ("no bright white border", "no excessive glow") —
    // these are absolute alphas (Color.copy replaces alpha, it doesn't multiply the base
    // token's own 0.055f/0.65f), so the numbers below are the final on-screen values.
    val borderTopAlpha = if (pressed) 0.025f else 0.055f
    val borderBottomAlpha = if (pressed) 0.15f else 0.30f
    // The offset drop-shadow does the actual depth-communication work (spec §1.1: "Shadows:
    // black or very-dark neutral where technically useful") — kept at TactileShadow's own
    // spec-literal 0.65f base rather than diluted further, or it disappears against the app's
    // still-very-dark Midnight Blue background; a faint shadow-on-dark reads as nothing at all —
    // the exact Matte Noir mistake (PROJECT_STATE.md Batch 39-44), not repeated here.
    val shadowAlpha = if (pressed) 0.33f else 0.65f

    return this
        .scale(scale)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = Path().apply { addOutline(outline) }
            translate(top = animatedElevation.toPx() * 0.45f) {
                drawPath(outlinePath, color = TactileShadow.copy(alpha = shadowAlpha))
            }
        }
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(TactileSurfaceVariant, TactileSurface)
            )
        )
        .border(
            BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        TactileHighlight.copy(alpha = borderTopAlpha),
                        TactileShadow.copy(alpha = borderBottomAlpha)
                    )
                )
            ),
            shape
        )
}
