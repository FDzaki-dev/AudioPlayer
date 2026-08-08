package com.rudi.audioplayer.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Batch 40 — Matte Noir "epic" depth pass, direct response to on-device feedback that Batch
 * 38/39's flat tonalElevation port from Apple's own glass-panel model never actually read as
 * premium hardware on every surface, not just one. Root problem: a flat tonal-overlay Surface
 * has no directional light — it just gets a bit lighter/darker as a solid fill, which is why
 * it always looked "kureng" no matter how much elevation/tonalElevation was cranked up.
 *
 * matteEmboss() is the single shared building block used at every "epic" touch point instead
 * (Home, Library, Now Playing, Settings, mini-player, nav bar) so the whole app reads as one
 * consistent light source falling across brushed matte metal, combining three cues at once on
 * one shape:
 *  1. A strong copper-umbra directional drop shadow — manual (drawBehind + Outline, see
 *     Batch 42 note below matteEmboss()), not native Modifier.shadow. The native version was
 *     tried in Batch 40/41 and stayed invisible on-device both times regardless of tint color
 *     or elevation, so contrast here is set directly via alpha, identical on every API level.
 *  2. A diagonal surface gradient (MatteHighlight → MatteSurface → MatteUmbra) instead of a
 *     flat fill — simulates one light source hitting the top-left corner and falling off
 *     toward the bottom-right, the literal definition of "not flat" that tonalElevation alone
 *     can't produce.
 *  3. A 1dp highlight-brush border fading from MatteHighlight to transparent — the actual
 *     physical cue neumorphism relies on: the top-left edge of a lit panel visibly catches
 *     light. Pressed state inverts/dims this so tappable cards feel like they sink back in.
 *
 * Matte-only by construction (uses Matte* colors directly, not colorScheme roles) — every call
 * site gates with `isMatte` before applying this and falls back to the existing Apple-style
 * Surface/tonalElevation otherwise, so SYSTEM/LIGHT/DARK stay byte-for-byte unchanged.
 */
@Composable
fun Modifier.matteEmboss(
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 16.dp,
    pressed: Boolean = false
): Modifier {
    // Batch 41 tried raising `elevation` on the assumption that native Modifier.shadow's
    // opacity (not its tint color) was the missing piece. Confirmed via on-device screenshot
    // AFTER Batch 41 shipped: still invisible. Root cause was deeper than either color or
    // elevation — Android's native RenderNode ambient/spot shadow is capped at a low max
    // opacity regardless of elevation once the surrounding background is already near-black;
    // there is no elevation value that pushes it past that cap. Native Modifier.shadow is
    // dropped entirely here.
    //
    // Batch 42 — manual shadow, guaranteed visible regardless of platform shadow-opacity caps:
    // draw the shape's own Outline twice with drawBehind, offset down-right, using MatteUmbra
    // at a fixed, deliberately high alpha (0.5f core / 0.30f wider halo) — a plain semi-
    // transparent silhouette peeking from behind the card edge, not a physically-simulated
    // blur. Cruder than a real Gaussian shadow, but its contrast against MatteBackground is
    // set directly by us (alpha channel), not by the OS, so it can never silently degrade to
    // invisible again the way the tinted RenderNode shadow did twice in a row (Batch 40, 41).
    //
    // Batch 43 shipped a broken fix for this: added
    // `import androidx.compose.ui.graphics.drawscope.drawOutline` assuming it was a top-level
    // extension needing an import, same shape as `translate` above it. Build failed again
    // (log_fail_6.zip) — unresolved even at the import line itself, because no such top-level
    // function exists at that path.
    //
    // Batch 44 — actual fix: `DrawScope` has no `drawOutline` function at all (checked directly
    // against the AOSP DrawScope.kt source — only drawLine/drawRect/drawRoundRect/drawCircle/
    // drawOval/drawArc/drawPath/drawPoints are members). The real conversion path is
    // `Path().apply { addOutline(outline) }` (top-level extension in `androidx.compose.ui.
    // graphics`, confirmed to exist in that package's reference listing) followed by the
    // genuine DrawScope member `drawPath(path, color)`. **Lesson: verify an unfamiliar Compose
    // API call against the actual interface/package source before writing an import for it —
    // a plausible-looking name in the same neighborhood as a working one (translate) is not
    // evidence it exists.**
    val effectiveElevation = if (pressed) elevation / 3 else elevation
    // highlightAlpha was tuned on a full-size mockup; on the small real card sizes (mini-player,
    // badges) 0.14f/0.05f read as flat. Raised so the top-left catch-light is legible at
    // actual on-screen scale.
    val highlightAlpha = if (pressed) 0.09f else 0.24f
    val borderAlpha = if (pressed) 0.15f else 0.5f
    val coreShadowAlpha = if (pressed) 0.22f else 0.5f
    val haloShadowAlpha = if (pressed) 0.12f else 0.30f
    return this
        .drawBehind {
            val offsetPx = effectiveElevation.toPx() * 0.6f
            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = Path().apply { addOutline(outline) }
            // Wider, fainter halo layer first (drawn under the core layer).
            translate(left = offsetPx * 0.4f, top = offsetPx) {
                drawPath(outlinePath, color = MatteUmbra.copy(alpha = haloShadowAlpha))
            }
            // Tighter, stronger core layer on top of the halo.
            translate(left = offsetPx * 0.2f, top = offsetPx * 0.55f) {
                drawPath(outlinePath, color = MatteUmbra.copy(alpha = coreShadowAlpha))
            }
        }
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    MatteHighlight.copy(alpha = highlightAlpha),
                    MatteSurface,
                    MatteUmbra.copy(alpha = 0.65f)
                )
            )
        )
        .border(
            BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(MatteHighlight.copy(alpha = borderAlpha), Color.Transparent)
                )
            ),
            shape
        )
}
