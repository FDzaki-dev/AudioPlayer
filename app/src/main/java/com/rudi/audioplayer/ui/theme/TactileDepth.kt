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
 * Batch 50 — full repaint from the user-supplied compose-skeuomorphism-lite-dark.md spec, which
 * supersedes the Batch 49 light-palette version of this same function. Structure/signature is
 * unchanged (all 8 call sites keep working unmodified) — only the token values and alpha
 * constants change, per spec §1.1's own instruction: **"Do not simply invert a light theme.
 * Design the tactile lighting model specifically for dark surfaces."**
 *  1. Tactile depth (spec §4) — top-down `Brush.verticalGradient` using the spec's own literal
 *     §2 tokens (TactileSurfaceVariant -> TactileSurface, i.e. the "lifted" panel is a hair
 *     lighter than its recessed background), plus a bevel border. Spec §4's explicit dark-mode
 *     rule: "Do NOT use a bright Color.White border… highlight = very-low-alpha light tone,
 *     shadow = very-dark neutral" — border alphas below are deliberately far lower than the old
 *     light-theme version's (0.9/0.45), never a flat opaque edge.
 *  2. Micro-interactions (spec §6) — unchanged mechanism from Batch 49 (`pressed` still drives
 *     real animated depression via `animateDpAsState`/`animateFloatAsState`), still hand-drawn
 *     via `drawBehind` + `Outline`->`Path` rather than native `Modifier.shadow` for the same
 *     reason as ever: a black-on-near-black native shadow is exactly the failure mode that took
 *     5 batches to fix for Matte Noir (see PROJECT_STATE.md Batch 39-44) — this app's own alpha
 *     stays the single source of contrast truth instead of trusting platform shadow rendering
 *     against an AMOLED background.
 *  3. Restrained skeuomorphism (spec §1.3/§8) — this stays the flat/structural-card treatment
 *     only; spec §7's fully tactile Button/Toggle/Slider components are a separate, larger
 *     scope (new files under `ui/components/`, per spec §12) not built this batch — sliders/
 *     toggles/switches remain plain Material3, same boundary carried over from Batch 49.
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
    val borderTopAlpha = if (pressed) 0.04f else 0.09f
    val borderBottomAlpha = if (pressed) 0.15f else 0.30f
    // The offset drop-shadow does the actual depth-communication work (spec §2: "Shadows: black
    // or very-dark neutral where technically useful") — kept close to TactileShadow's own
    // spec-literal 0.65f base rather than diluted further, or it disappears entirely against
    // TactileBackground (0xFF05070A is already near-black; a faint black-on-black shadow reads
    // as nothing at all — the exact Matte Noir mistake, not repeated here).
    val shadowAlpha = if (pressed) 0.35f else 0.65f

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
