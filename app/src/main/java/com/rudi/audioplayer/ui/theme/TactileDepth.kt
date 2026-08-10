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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Batch 53 — repainted again for the user-supplied compose-amoled-hybrid-glass-final.md spec,
 * which supersedes Batch 52's flat literal Midnight Blue version of this same function.
 * Structure/signature is still unchanged (all call sites keep working unmodified) — token
 * *values* changed in Color.kt (TactileSurfaceVariant/TactileSurface are now the spec's own
 * GlassElevated/GlassBase §5 tokens, translucent-reading dark glass, not an opaque bevel), and
 * the gradient direction below changed from vertical to diagonal to honor spec §9's single
 * simulated light source.
 *  1. Hybrid glass + tactile depth (spec §4 + §11) — this function is this project's one shared
 *     "elevated tactile surface" primitive (mini player, hero artwork panel, quick-action cards,
 *     the theme picker's own live preview row — grep confirms every call site). Per spec §10 the
 *     surfaces it decorates sit between pure structural glass and a dedicated tactile control:
 *     they are touch-interactive panels, so a restrained tactile cue on top of the glass base is
 *     appropriate, but the cue must stay subtle (spec §11: "restrained shadow", never a heavy
 *     bevel) — hence the diagonal glass gradient plus a whisper-thin border, not a hardware-style
 *     3D extrusion.
 *  2. Lighting model (spec §9) — "Top-left -> bottom-right" for every component, consistently.
 *     `Brush.linearGradient(colors)` without explicit start/end already draws along that exact
 *     diagonal (its default Offset.Zero -> Offset.Infinite), so no manual Offset math is needed —
 *     swapping `verticalGradient` for `linearGradient` here is sufficient and keeps this in sync
 *     with every other diagonal brush in the theme package (see BlurUtils.kt's edgeBrush).
 *  3. Micro-interactions (spec §11: "Pressed: Elevation down, Scale down slightly, Highlight down,
 *     Surface becomes slightly deeper") — unchanged mechanism from prior batches
 *     (`animateDpAsState`/`animateFloatAsState`), still hand-drawn via `drawBehind` + `Outline`->
 *     `Path` rather than native `Modifier.shadow`, for the same reason as ever: a black-on-near-
 *     black native shadow is exactly the failure mode that took 5 batches to fix for Matte Noir
 *     (see PROJECT_STATE.md Batch 39-44) — this app's own alpha stays the single source of
 *     contrast truth instead of trusting platform shadow rendering.
 *  4. Restraint (spec §11 "Keep the animation immediate and short. No exaggerated bounce." + §18
 *     "Glow is an accent, not a material") — no glow/color accent is added here; accent stays
 *     reserved for selected/focused states elsewhere (NavigationBar selection, active controls),
 *     not this general-purpose elevation primitive.
 */
// Batch 57 — extracted as the shared mechanism behind both tactileEmboss() (unchanged
// behavior/signature/defaults below, delegates here with Tactile's own tokens) and the new
// skeuEmboss() (Skeuomorphism Dark Lite's own tokens). Structure/animation/drop-shadow math is
// identical for both identities — only the four surface colors differ — so this avoids a second
// hand-copied 55-line function drifting out of sync with the original the next time either one
// gets a polish pass.
@Composable
private fun Modifier.embossSurface(
    shape: Shape,
    elevation: Dp,
    pressed: Boolean,
    surfaceTop: Color,
    surfaceBottom: Color,
    highlight: Color,
    shadow: Color,
    label: String,
    // Batch 58 — these six used to be literals hardcoded straight into the body below (same
    // values for every caller). That silently discarded whatever alpha `highlight`/`shadow`
    // already carried (Color.copy REPLACES alpha, it doesn't multiply) — harmless for Tactile
    // only because these literals happened to be tuned to exactly match TactileHighlight/
    // TactileShadow's own baked alpha in the first place (Batch 53), but it meant Skeu's own
    // SkeuHighlight (0.10f, deliberately stronger per Color.kt's comment) and SkeuShadow (0.55f,
    // deliberately lower) were quietly overwritten back to Tactile's numbers whenever
    // skeuEmboss() ran — Skeu's bevel never actually rendered as its own designed intensity.
    // Defaults below are the exact previous Tactile literals, so tactileEmboss() (which doesn't
    // pass these) is byte-identical to before; skeuEmboss() now passes its own tuned values.
    borderTopAlphaNormal: Float = 0.065f,
    borderTopAlphaPressed: Float = 0.03f,
    borderBottomAlphaNormal: Float = 0.30f,
    borderBottomAlphaPressed: Float = 0.15f,
    shadowAlphaNormal: Float = 0.70f,
    shadowAlphaPressed: Float = 0.35f
): Modifier {
    val animatedElevation by animateDpAsState(
        targetValue = if (pressed) elevation / 4 else elevation,
        label = "${label}Elevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        label = "${label}Scale"
    )
    // Border stays a whisper per spec §8 ("never a bright white border") / §18 ("no excessive
    // glow") — these are absolute alphas (Color.copy replaces alpha entirely, it doesn't
    // multiply the base token's own alpha), so the numbers below are the final on-screen values.
    val borderTopAlpha = if (pressed) borderTopAlphaPressed else borderTopAlphaNormal
    val borderBottomAlpha = if (pressed) borderBottomAlphaPressed else borderBottomAlphaNormal
    // The offset drop-shadow does the actual depth-communication work (spec §5: "GlassShadow").
    // Kept at the token's own base rather than diluted further, or it disappears against a dark
    // background; a faint shadow-on-near-black reads as nothing at all — the exact Matte Noir
    // mistake (PROJECT_STATE.md Batch 39-44), not repeated here.
    val shadowAlpha = if (pressed) shadowAlphaPressed else shadowAlphaNormal

    return this
        .scale(scale)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = Path().apply { addOutline(outline) }
            translate(top = animatedElevation.toPx() * 0.45f) {
                drawPath(outlinePath, color = shadow.copy(alpha = shadowAlpha))
            }
        }
        .clip(shape)
        .background(
            // Diagonal top-left -> bottom-right gradient (spec §9) between the two elevated
            // surface levels, replacing the old vertical bevel.
            Brush.linearGradient(colors = listOf(surfaceTop, surfaceBottom))
        )
        .border(
            BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        highlight.copy(alpha = borderTopAlpha),
                        shadow.copy(alpha = borderBottomAlpha)
                    )
                )
            ),
            shape
        )
}

@Composable
fun Modifier.tactileEmboss(
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 8.dp,
    pressed: Boolean = false
): Modifier = this.embossSurface(
    shape = shape,
    elevation = elevation,
    pressed = pressed,
    surfaceTop = TactileSurfaceVariant,
    surfaceBottom = TactileSurface,
    highlight = TactileHighlight,
    shadow = TactileShadow,
    label = "tactileEmboss"
)

// Batch 57 — Skeuomorphism Dark Lite's own emboss primitive: same mechanism as tactileEmboss()
// (see embossSurface() above) with Skeu's own charcoal/copper tokens instead of Tactile's
// AMOLED-glass ones. Batch 58: wired into MiniPlayerBar.kt/NowPlayingScreen.kt (mirroring the
// same rollout Tactile got in Batch 55), and now passes its own bevel alphas explicitly instead
// of silently inheriting Tactile's (see embossSurface()'s Batch 58 comment above) — highlight
// stronger (0.10f vs Tactile's 0.065f, Color.kt's own long-standing design comment), shadow
// lower (0.55f vs 0.70f) so the bevel reads as a distinct, more restrained "carved panel" instead
// of a re-skinned copy of Tactile's glass-tuned depth cue.
@Composable
fun Modifier.skeuEmboss(
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 8.dp,
    pressed: Boolean = false
): Modifier = this.embossSurface(
    shape = shape,
    elevation = elevation,
    pressed = pressed,
    surfaceTop = SkeuDarkSurfaceVariant,
    surfaceBottom = SkeuDarkSurface,
    highlight = SkeuHighlight,
    shadow = SkeuShadow,
    label = "skeuEmboss",
    borderTopAlphaNormal = 0.10f,
    borderTopAlphaPressed = 0.045f,
    borderBottomAlphaNormal = 0.24f,
    borderBottomAlphaPressed = 0.12f,
    shadowAlphaNormal = 0.55f,
    shadowAlphaPressed = 0.28f
)
