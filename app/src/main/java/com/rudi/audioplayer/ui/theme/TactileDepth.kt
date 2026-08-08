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
 * Batch 49 — replaces MatteDepth.kt / matteEmboss() entirely, alongside the rest of the Matte
 * Noir identity (Color.kt, Type.kt, Theme.kt, this file), following the user-supplied
 * compose-skeuomorphism-lite.md spec on a fresh light palette instead of recoloring the old
 * dark one. Same three spec points as the Batch 46/47 rewrite of matteEmboss() carried over
 * (that engineering was already close to the spec, just wearing the wrong palette + a
 * still-present root content-color bug elsewhere — see MainActivity.kt Batch 48/49 notes):
 *  1. Tactile depth (spec §1) — one top-down `Brush.verticalGradient` using the spec's own
 *     literal example stops (TactileSurfaceHighlight 0xFFF8FAFC -> TactileSurfaceShadow
 *     0xFFE2E8F0), plus a vertical-gradient bevel border (bright top edge -> muted slate-gray
 *     bottom edge) instead of a glow ring — "layering contrasting light and dark borders".
 *  2. Micro-interactions (spec §2) — `pressed` drives real animated depression
 *     (`animateDpAsState`/`animateFloatAsState`: shadow collapses, scale settles to 0.985).
 *  3. Isolated accents (spec §3) — this stays the flat/structural-card treatment (low default
 *     elevation, single shadow layer, restrained alpha). Sliders/toggles/switches are left on
 *     plain Material3 components, not skinned by this modifier — same boundary as before.
 *
 * Shadow is still hand-drawn via `drawBehind` + `Outline`->`Path`, not native `Modifier.shadow`
 * — no longer because native shadow is invisible (that was specifically a near-black-background
 * problem, see the old MatteDepth.kt history), but kept for the same reason it was adopted: our
 * own alpha value is the single source of truth for contrast, consistent across every surface
 * regardless of API level or background brightness, light theme or not.
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
    val borderTopAlpha = if (pressed) 0.35f else 0.9f
    val borderBottomAlpha = if (pressed) 0.20f else 0.45f
    val shadowAlpha = if (pressed) 0.08f else 0.18f

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
                colors = listOf(TactileSurfaceHighlight, TactileSurfaceShadow)
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
