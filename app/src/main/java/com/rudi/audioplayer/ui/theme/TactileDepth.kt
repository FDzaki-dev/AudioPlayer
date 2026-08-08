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
 * Batch 51 — repainted again for the user-supplied compose-skeuomorphism-lite-hybrid-glass-
 * dark-blue.md spec, which supersedes Batch 50's AMOLED-black version of this same function.
 * Structure/signature is still unchanged (all 8 call sites keep working unmodified) — only the
 * token *values* changed, this time entirely inside Color.kt: TactileSurfaceVariant/
 * TactileSurface are now translucent (spec §2 literal 0xB8/0xCC alpha) instead of opaque, so the
 * exact same `Brush.verticalGradient(TactileSurfaceVariant, TactileSurface)` call below now
 * automatically composites as glass over whatever's drawn behind it (MainActivity.kt's new root
 * gradient) — this is the spec §8 "hybrid glass" formula, achieved here with zero logic changes.
 *  1. Tactile depth (spec §4) — top-down `Brush.verticalGradient` using the spec's own literal
 *     §2 tokens, plus a bevel border. Spec §4's hybrid-glass rule: "Do NOT use a bright
 *     Color.White border… highlight = very-low-alpha cool white/blue, shadow = deep navy/near-
 *     black" — TactileHighlight/TactileShadow are themselves now tinted (not generic white/black
 *     at low alpha, see Color.kt), so this file's own border-alpha *scaling* logic is unchanged
 *     from Batch 50, it just scales cooler-tinted base colors now.
 *  2. Micro-interactions (spec §6) — unchanged mechanism from Batch 49/50 (`pressed` still drives
 *     real animated depression via `animateDpAsState`/`animateFloatAsState`), still hand-drawn
 *     via `drawBehind` + `Outline`->`Path` rather than native `Modifier.shadow` for the same
 *     reason as ever: a black-on-near-black native shadow is exactly the failure mode that took
 *     5 batches to fix for Matte Noir (see PROJECT_STATE.md Batch 39-44) — this app's own alpha
 *     stays the single source of contrast truth instead of trusting platform shadow rendering.
 *  3. Restrained skeuomorphism (spec §1.3/§8) — this stays the flat/structural-card treatment
 *     only; spec §7's fully tactile Button/Toggle/Slider components are a separate, larger
 *     scope (new files under `ui/components/`, per spec §12) not built this batch either —
 *     sliders/toggles/switches remain plain Material3, same boundary carried over from Batch
 *     49/50.
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
    // The offset drop-shadow does the actual depth-communication work (spec §2: "Shadows: deep
    // navy/black with restrained opacity where technically useful") — kept close to
    // TactileShadow's own spec-literal 0.68f base rather than diluted further, or it disappears
    // against the app's still-very-dark background; a faint shadow-on-near-black reads as
    // nothing at all — the exact Matte Noir mistake (PROJECT_STATE.md Batch 39-44), not
    // repeated here even though this spec's background is a hair lighter than Batch 50's.
    val shadowAlpha = if (pressed) 0.35f else 0.68f

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
