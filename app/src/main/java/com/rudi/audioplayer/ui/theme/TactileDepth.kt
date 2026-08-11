package com.rudi.audioplayer.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
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
): Modifier {
    // Batch 61 — identitas Tactile sekarang otonom di kedua mode (lihat Theme.kt), jadi bevel-nya
    // juga wajib pilih token light/dark sendiri lewat LocalIsDarkTheme, bukan lagi hardcode token
    // gelap terus-menerus seperti sebelum Batch 61. Alpha border/shadow juga dituning ulang khusus
    // varian terang (kontrasnya terbalik: highlight putih di atas kanvas terang nyaris tak
    // kelihatan di alpha rendah, jadi butuh alpha jauh lebih tinggi; shadow sebaliknya perlu lebih
    // rendah dari versi AMOLED-nya supaya tidak jadi noda gelap kasar di atas kanvas terang).
    val isDark = LocalIsDarkTheme.current
    return this.embossSurface(
        shape = shape,
        elevation = elevation,
        pressed = pressed,
        surfaceTop = if (isDark) TactileSurfaceVariant else TactileLightSurfaceVariant,
        surfaceBottom = if (isDark) TactileSurface else TactileLightSurface,
        highlight = if (isDark) TactileHighlight else TactileLightHighlight,
        shadow = if (isDark) TactileShadow else TactileLightShadow,
        label = "tactileEmboss",
        // Batch 62 — user: "perkuat vibes radikal, tanpa mengikuti batasan light/dark
        // system". Alpha border/shadow dinaikkan jauh di atas versi Batch 61 (dulu 0.065/
        // 0.03/0.30/0.15/0.70/0.35) di KEDUA mode — bevel sekarang jauh lebih dramatis/
        // glossy, sengaja menyimpang dari nada "restrained" spec asli (compose-amoled-
        // hybrid-glass-final.md §9 menyarankan subtlety) atas instruksi eksplisit user.
        borderTopAlphaNormal = if (isDark) 0.16f else 1.0f,
        borderTopAlphaPressed = if (isDark) 0.08f else 0.65f,
        borderBottomAlphaNormal = if (isDark) 0.55f else 0.30f,
        borderBottomAlphaPressed = if (isDark) 0.30f else 0.16f,
        shadowAlphaNormal = if (isDark) 0.90f else 0.34f,
        shadowAlphaPressed = if (isDark) 0.55f else 0.18f
    )
}

// Batch 73 — SKEUOMORPHISM 2.0 / HYPER-REALISM. No longer delegates to the shared
// embossSurface() mechanism above (that function is now Tactile-only) — Skeu's physical-panel
// identity needs layers Tactile's restrained glass-panel primitive was never designed for
// (specular glint, ambient occlusion, an inner carved groove, brushed-metal grain), so sharing
// the function any further would mean bolting Skeu-only branches onto Tactile's primitive or
// smuggling Tactile-shaped assumptions into Skeu — either way the two identities stop being
// independently editable, which is the exact "not autonomous, still hybrid" complaint this
// batch exists to fix. Draw order (back to front), all in one drawBehind before .clip():
//   1. Ambient occlusion — soft, slightly-oversized dark ring UNDER the panel (contact shadow
//      at the base, distinct from #2's cast shadow which simulates panel-to-canvas distance).
//   2. Cast drop-shadow — same translated-outline technique as embossSurface(), offset further
//      down-right than Tactile's for a heavier, more physical sense of elevation.
// Then, after .clip(shape):
//   3. Base surface — 4-stop diagonal gradient (not a flat 2-color bevel) simulating a subtly
//      curved metal surface rather than a flat painted panel.
//   4. Brushed-metal grain — a second background layer: Brush.linearGradient with a very short
//      start->end segment + TileMode.Repeated, which repeats that short diagonal stripe across
//      the whole surface — the standard Compose technique for a brushed-metal/hairline texture
//      without a custom Shader. Alpha is low enough to read as texture, not banding.
//   5. Specular glint — a small radial-gradient highlight anchored top-left, far brighter than
//      any bevel highlight, standing in for a direct reflection off brushed metal. Dims sharply
//      when pressed (light source reads as "moving away" as the panel physically recedes).
// Finally, two border strokes stacked (both after .clip()):
//   6. Outer bevel — catch-light (top-left) fading to shadow (bottom-right), same diagonal
//      lighting model as Tactile's border for cross-theme consistency of *direction*, but
//      Skeu's own tokens/alphas (already much stronger — see Color.kt).
//   7. Inner groove — a second, thinner stroke INSET from the outer edge (drawn via a second
//      border pass at reduced size through padding), reading as the panel being carved down
//      slightly before its surface rises — the double-bevel signature of hyper-realism vs a
//      single flat highlight/shadow border.
@Composable
fun Modifier.skeuEmboss(
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 8.dp,
    pressed: Boolean = false
): Modifier {
    val isDark = LocalIsDarkTheme.current
    val surfaceTop = if (isDark) SkeuDarkSurfaceVariant else SkeuLightSurfaceVariant
    val surfaceMid = if (isDark) SkeuDarkSurface else SkeuLightSurface
    // Deliberately opaque (lerp toward black/white, never Color.copy(alpha=...)) — Skeu's
    // "solid panel, never translucent glass" identity (established Batch 58) still applies to
    // this new 4-stop curved-metal gradient; only the endpoint darkens, it never lets whatever
    // is layered underneath show through.
    val surfaceBottom = if (isDark) lerp(surfaceMid, Color.Black, 0.18f) else lerp(surfaceMid, Color.Black, 0.06f)
    val highlight = if (isDark) SkeuHighlight else SkeuLightHighlight
    val shadow = if (isDark) SkeuShadow else SkeuLightShadow
    val ao = if (isDark) SkeuAmbientOcclusion else SkeuLightAmbientOcclusion
    val grainLight = if (isDark) SkeuBrushGrainLight else SkeuLightBrushGrainLight
    val grainDark = if (isDark) SkeuBrushGrainDark else SkeuLightBrushGrainDark
    val specular = if (isDark) {
        if (pressed) SkeuSpecularPressed else SkeuSpecular
    } else {
        if (pressed) SkeuLightSpecularPressed else SkeuLightSpecular
    }
    val groove = if (isDark) {
        if (pressed) SkeuInnerGroovePressed else SkeuInnerGroove
    } else {
        if (pressed) SkeuLightInnerGroovePressed else SkeuLightInnerGroove
    }

    val animatedElevation by animateDpAsState(
        targetValue = if (pressed) elevation / 5 else elevation,
        label = "skeuEmbossElevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.978f else 1f,
        label = "skeuEmbossScale"
    )
    val specularAlphaMul by animateFloatAsState(
        targetValue = if (pressed) 0.25f else 1f,
        label = "skeuEmbossSpecular"
    )

    val outerBorderAlpha = if (pressed) 0.55f else 1f
    val brushGrain = Brush.linearGradient(
        colors = listOf(grainLight, grainDark),
        start = Offset(0f, 0f),
        end = Offset(3f, 3f),
        tileMode = TileMode.Repeated
    )

    return this
        .scale(scale)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = Path().apply { addOutline(outline) }
            // 1. Ambient occlusion — wider, softer, closer to the panel base.
            translate(top = animatedElevation.toPx() * 0.15f) {
                drawPath(outlinePath, color = ao)
            }
            // 2. Cast shadow — heavier offset than Tactile's for a more physical drop.
            translate(top = animatedElevation.toPx() * 0.65f) {
                drawPath(outlinePath, color = shadow)
            }
        }
        .clip(shape)
        // 3. Base surface — curved-metal 4-stop diagonal.
        .background(
            Brush.linearGradient(
                *arrayOf(
                    0.0f to surfaceTop,
                    0.35f to surfaceMid,
                    0.7f to surfaceMid,
                    1.0f to surfaceBottom
                )
            )
        )
        // 4. Brushed-metal grain overlay.
        .background(brushGrain)
        // 5. Specular glint — small radial highlight, top-left quadrant.
        .drawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        specular.copy(alpha = specular.alpha * specularAlphaMul),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.24f, size.height * 0.18f),
                    radius = size.minDimension.coerceAtLeast(1f) * 0.65f
                )
            )
        }
        // 6. Outer bevel border — diagonal catch-light -> shadow.
        .border(
            BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    colors = listOf(
                        highlight.copy(alpha = highlight.alpha * outerBorderAlpha),
                        shadow.copy(alpha = shadow.alpha * outerBorderAlpha)
                    )
                )
            ),
            shape
        )
        // 7. Inner groove — inset second stroke, reads as a carved recess just inside the
        // outer edge before the panel's own surface begins.
        .padding(1.dp)
        .border(BorderStroke(1.dp, groove), shape)
}
