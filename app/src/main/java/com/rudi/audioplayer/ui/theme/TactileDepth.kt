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
import androidx.compose.ui.geometry.Offset
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

// Batch 79 — NEUMORPHISM. "Upgrade Skeuomorphism -> Neumorphism" atas instruksi eksplisit user:
// aksen Titanium tetap dominan (SkeuAccent dkk. TIDAK disentuh — tetap satu-satunya token di
// role M3 primary/surfaceTint), sedikit sentuhan Zamrud/Emerald baru (SkeuEmerald, Color.kt),
// dan "depth ultra realistic". Bukan penghalusan Hyper-Realism (Batch 73-75) — diganti total,
// draw order (back to front) beda arsitektur:
//   1-2. Dual soft-shadow, masing-masing 3-4 layer offset+alpha bertingkat (faux-blur via
//        tumpukan, DrawScope Compose tidak punya blur asli tanpa RenderEffect API 31+): sisi
//        GELAP (kanan-bawah normal) pakai SkeuAmbientOcclusion sbg layer terdekat/terkuat +
//        SkeuShadow sbg 2 layer terjauh/lebih tipis; sisi TERANG (kiri-atas normal) pakai
//        SkeuSpecular sbg layer terdekat/terkuat + SkeuHighlight sbg 2 layer terjauh. SEMUA
//        digambar SEBELUM .clip() (persis pola embossSurface()/Hyper-Realism lama) supaya boleh
//        meluber sedikit di luar tepi shape, itu yang bikin bayangannya kebaca "lembut", bukan
//        garis tegas.
//   3. Base surface — SATU warna flat (SkeuNeuSurfaceDark/Light, BARU, Color.kt) yang sengaja
//      hampir sewarna kanvas — bukan lagi gradient panel-logam 4-stop. Prinsip inti neumorphism:
//      panel terbaca "dipahat dari material yang sama dengan kanvas"; kedalaman 100% tanggung
//      jawab dual-shadow di atas, BUKAN dari kontras warna panel-vs-kanvas.
//   TIDAK ADA lagi (dihapus total dari Hyper-Realism): brushed-metal grain (neumorphism itu
//   MULUS, tanpa tekstur), outer bevel border, inner groove border (neumorphism TIDAK PUNYA
//   garis batas SAMA SEKALI — ciri paling khas gaya ini; kedalaman murni dari bayangan, bukan
//   garis. Ini penyederhanaan besar dari 7-layer Hyper-Realism lama, bukan penambahan).
// Pressed = CONCAVE, bukan cuma mengecil: `dir = -1f` membalik sisi mana yang terang/gelap
// (kanan-bawah jadi terang, kiri-atas jadi gelap) — bahasa visual baku neumorphism utk
// "permukaan masuk ke kanvas", beda dari Tactile/Hyper-Realism lama yg cuma meredupkan/
// mengecilkan elevasi tanpa membalik sisi.
// Sentuhan Zamrud: SATU titik kecil saja — inti sisi terang berbaur ke SkeuEmerald HANYA saat
// pressed (animatedFloat emeraldGlow, 0 saat normal) — kesan permata kecil di logam titanium yg
// menyala redup pas panel ditekan. Sengaja "sedikit" (1 layer, alpha rendah, cuma nyala saat
// interaksi) persis instruksi user "Titanium dominan, sedikit sentuhan zamrud" — Emerald TIDAK
// pernah dipakai di role M3 apa pun supaya mustahil menyebar tanpa sengaja.
@Composable
fun Modifier.skeuEmboss(
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 8.dp,
    pressed: Boolean = false
): Modifier {
    val isDark = LocalIsDarkTheme.current
    val panelFill = if (isDark) SkeuNeuSurfaceDark else SkeuNeuSurfaceLight
    val lightNear = if (isDark) {
        if (pressed) SkeuSpecularPressed else SkeuSpecular
    } else {
        if (pressed) SkeuLightSpecularPressed else SkeuLightSpecular
    }
    val lightFar = if (isDark) SkeuHighlight else SkeuLightHighlight
    val darkNear = if (isDark) SkeuAmbientOcclusion else SkeuLightAmbientOcclusion
    val darkFar = if (isDark) SkeuShadow else SkeuLightShadow
    val emerald = if (isDark) SkeuEmerald else SkeuLightEmerald

    val animatedElevation by animateDpAsState(
        targetValue = if (pressed) elevation * 0.6f else elevation,
        label = "skeuEmbossElevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.978f else 1f,
        label = "skeuEmbossScale"
    )
    // Batch 80 — fix: Batch 79's emerald ONLY appeared blended into lightNear (near-white/silver
    // specular) and ONLY while actively pressed — user feedback: "yang kelihatan cuman Titanium
    // dominan, mana zamrudnya??", karena (a) lerp 55% ke arah putih terang nyaris tak mengubah
    // hue yang terlihat mata (mixing a small % of saturated color into near-opaque white mostly
    // just desaturates it, it doesn't read as that color), dan (b) alpha 0 total saat idle —
    // kalau user cuma lihat screenshot/UI diam, emerald-nya betul-betul 0%, bukan cuma "sedikit".
    // Fix: emerald sekarang LAYER SENDIRI (radial glint kecil, bukan di-blend ke lightNear) +
    // baseline idle > 0 (0.20f, tetap "sedikit" tapi genuinely visible) yang naik ke 0.52f saat
    // pressed (efek "permata menyala" yang jelas kelihatan pas disentuh).
    val emeraldAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.52f else 0.20f,
        label = "skeuEmbossEmeraldGlow"
    )

    // Concave flip: -1f saat pressed membalik SELURUH diagonal terang/gelap, bukan sekadar
    // memperkecil offset-nya (itu bedanya dengan Tactile — lihat komentar di atas fungsi).
    val dir = if (pressed) -1f else 1f

    return this
        .scale(scale)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = Path().apply { addOutline(outline) }
            val basePx = animatedElevation.toPx()
            // Sisi GELAP — kanan-bawah normal / kiri-atas saat pressed.
            translate(left = basePx * 0.28f * dir, top = basePx * 0.28f * dir) {
                drawPath(outlinePath, color = darkNear)
            }
            translate(left = basePx * 0.60f * dir, top = basePx * 0.60f * dir) {
                drawPath(outlinePath, color = darkFar.copy(alpha = darkFar.alpha * 0.7f))
            }
            translate(left = basePx * 1.05f * dir, top = basePx * 1.05f * dir) {
                drawPath(outlinePath, color = darkFar.copy(alpha = darkFar.alpha * 0.35f))
            }
            // Sisi TERANG — kiri-atas normal / kanan-bawah saat pressed.
            translate(left = -basePx * 0.28f * dir, top = -basePx * 0.28f * dir) {
                drawPath(outlinePath, color = lightNear)
            }
            translate(left = -basePx * 0.60f * dir, top = -basePx * 0.60f * dir) {
                drawPath(outlinePath, color = lightFar.copy(alpha = lightFar.alpha * 0.7f))
            }
        }
        .clip(shape)
        // Base surface — flat, hampir sewarna kanvas. Kedalaman 100% dari dual-shadow di atas.
        .background(panelFill)
        // Batch 80 — Zamrud, layer TERPISAH (bukan blend) di atas panelFill: titik radial kecil
        // di kuadran sisi-terang (ikut `dir` — kiri-atas normal, kanan-bawah pressed), warna murni
        // SkeuEmerald sendiri, jadi selalu kebaca sebagai hijau, bukan cuma putih yang sedikit
        // kurang saturasi.
        .drawBehind {
            val cx = if (dir > 0f) size.width * 0.18f else size.width * 0.82f
            val cy = if (dir > 0f) size.height * 0.16f else size.height * 0.84f
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(emerald.copy(alpha = emeraldAlpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = size.minDimension.coerceAtLeast(1f) * 0.32f
                )
            )
        }
}
