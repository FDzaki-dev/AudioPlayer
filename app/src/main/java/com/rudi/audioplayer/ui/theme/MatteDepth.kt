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
 * Batch 45 — Full rewrite following the user-supplied "Skeuomorphism-lite (Tactile UI)" spec
 * (compose-skeuomorphism-lite.md), replacing the Batch 40-44 "epic" depth pass end to end after
 * user feedback that it looked bad on-device. Same public signature as before
 * (shape/elevation/pressed) — every existing call site (mini player, Home, Library, Settings,
 * Now Playing) picks up the new look automatically, no call-site edits needed.
 *
 * Three changes driven directly by the spec, replacing the old approach in each case:
 *  1. Tactile depth (spec §1): ONE straight top-down light source (Brush.verticalGradient +
 *     a single soft drop shadow directly below the shape) instead of the old diagonal
 *     highlight→surface→umbra gradient + two stacked offset shadow layers. Bevel is now a
 *     single vertical-gradient border (light top edge fading to dark bottom edge) instead of a
 *     one-sided top-left catch-light ring — this is the literal "layering contrasting light and
 *     dark borders" instruction in the spec, and reads as a lit extruded panel rather than a
 *     glow outline.
 *  2. Micro-interactions (spec §2): `pressed` now drives real animated depression via
 *     `animateDpAsState`/`animateFloatAsState` (elevation collapses, shape scales to 0.985)
 *     instead of the old instant alpha swap — the "physical click" feel the spec asks for.
 *  3. Isolated accents (spec §3): intensity is turned down across the board (this modifier is
 *     now the FLAT/structural-card treatment — lower default elevation, single shadow layer,
 *     lighter alpha throughout) so plain containers (mini-player, cards) stop competing for
 *     attention. Physical-utility controls (sliders, toggles) are intentionally left on their
 *     existing Material3 components, per spec §3's "keep structural container cards flat and
 *     minimal" — this app doesn't skin sliders/switches with matteEmboss and this rewrite does
 *     not change that.
 *
 * Native `Modifier.shadow` is still not used — confirmed invisible on Matte's near-black
 * background regardless of elevation/tint (Batch 40/41, re-confirmed here on the AlbumArtHero
 * shadow removed in this same batch). Contrast stays fully in our own alpha values via a single
 * manually-drawn shadow layer, just one layer now instead of two.
 */
@Composable
fun Modifier.matteEmboss(
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 10.dp,
    pressed: Boolean = false
): Modifier {
    val animatedElevation by animateDpAsState(
        targetValue = if (pressed) elevation / 4 else elevation,
        label = "matteEmbossElevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        label = "matteEmbossScale"
    )
    val highlightAlpha = if (pressed) 0.08f else 0.16f
    val borderTopAlpha = if (pressed) 0.14f else 0.38f
    val borderBottomAlpha = if (pressed) 0.10f else 0.28f
    val shadowAlpha = if (pressed) 0.10f else 0.24f

    return this
        .scale(scale)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = Path().apply { addOutline(outline) }
            translate(top = animatedElevation.toPx() * 0.45f) {
                drawPath(outlinePath, color = MatteUmbra.copy(alpha = shadowAlpha))
            }
        }
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(MatteHighlight.copy(alpha = highlightAlpha), MatteSurface)
            )
        )
        .border(
            BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        MatteHighlight.copy(alpha = borderTopAlpha),
                        MatteUmbra.copy(alpha = borderBottomAlpha)
                    )
                )
            ),
            shape
        )
}
