package com.rudi.audioplayer.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

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
// Batch 81 — tambahan: dual-shadow di bawah sekarang dibungkus clipRect() (lihat komentar di
// dalam fungsi) supaya "Ambient Light gak bocor" (bagian instruksi user yg belum tersentuh di
// Batch 79/80) — bayangan dijamin tidak meluber ke sibling lain, halo-nya proporsional ke
// `elevation` jadi tidak pernah memotong bentuk bayangannya sendiri.
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
            // Batch 81 — fix: "Ambient Light yang gak bocor" (instruksi eksplisit user, belum
            // ditangani Batch 79/80). Compose TIDAK meng-clip drawBehind{} ke bounds layout-nya
            // sendiri by default — bayangan lebar/menjauh bisa kegambar nimpa sibling di
            // sekitarnya (row LazyColumn lain, MiniPlayerBar yg cuma berjarak tipis dari
            // NavigationBar di bawahnya, dst) tanpa ada warning apa pun saat compile. Seluruh
            // dual-shadow di bawah (kedua sisi, 5 layer) sekarang dibungkus 1 clipRect() dgn
            // halo TETAP proporsional ke elevation (1.3x offset terjauh yg dipakai, 1.05x) —
            // bayangan dijamin TIDAK PERNAH meluber lebih jauh dari itu, utk elevation berapa pun
            // yg dikirim caller (MiniPlayerBar's 16.dp termasuk), tanpa memotong bentuknya sendiri
            // (halo > offset terjauh, jadi bayangan tetap utuh, cuma areanya yg dibatasi tegas).
            val haloPx = basePx * 1.3f
            clipRect(
                left = -haloPx,
                top = -haloPx,
                right = size.width + haloPx,
                bottom = size.height + haloPx
            ) {
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

// ============================================================================
// CALM RETRO — bias aberrasi CTA (Batch 129). Terjemahan Compose dari CSS box-shadow ganda di
// spec (`.calm-play-button`) — Compose tidak punya colored box-shadow native, jadi didekati
// lewat 2 lingkaran radial-gradient tipis diposisikan offset kiri-atas (Dusty Rose)/kanan-bawah
// (Dusty Denim), fade ke transparent (meniru blur lembut spec tanpa RenderEffect API 31+, pola
// sama "hand-drawn, bukan bitmap" seperti tactileEmboss()/skeuEmboss()). HANYA dipakai identitas
// Calm Retro (isCalmRetroTheme()), tidak menyentuh mekanisme embossSurface() identitas lain.
// Alpha 0.35f — dalam rentang 30%-40% yang diminta eksplisit spec §"Panduan Desain Penting" #1
// ("opacity rendah ... agar tidak berubah menjadi neon yang tajam").
@Composable
fun Modifier.calmAberration(bias: Dp = 3.dp): Modifier {
    val biasPx = bias
    return this.drawBehind {
        val off = biasPx.toPx()
        val radius = size.minDimension / 2f + off * 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CalmRetroAberrationLeft.copy(alpha = 0.35f), Color.Transparent),
                center = center - Offset(off, off),
                radius = radius
            ),
            radius = radius,
            center = center - Offset(off, off)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CalmRetroAberrationRight.copy(alpha = 0.35f), Color.Transparent),
                center = center + Offset(off, off),
                radius = radius
            ),
            radius = radius,
            center = center + Offset(off, off)
        )
    }
}

// ============================================================================
// CALM RETRO v3 upgrade (palet_warna_calm_retro_v3.md) — 2 pilar baru dari 4 pilar identitas
// yang belum pernah digarap sebelumnya (v2 cuma pilar B/aberrasi, sudah ada di atas sejak
// Batch 129). Pilar A & D ditambahkan di sini; pilar C (tipografi monospace) murni per-Text
// di NowPlayingScreen.kt (tidak butuh primitive baru), diterapkan HANYA ke durasi/waktu sesuai
// larangan eksplisit spec §4 ("JANGAN" pakai efek/font berbeda di judul/lirik).

// PILAR A — Soft CRT Scanlines. Garis horizontal berulang 4px (setengah transparan/setengah
// gelap tipis, meniru CSS `linear-gradient(...50%, rgba(0,0,0,0.3) 50%)` literal spec), teknik
// `TileMode.Repeated` (GPU-side brush, bukan loop draw manual — murah dipanggil tiap frame).
// Dipasang di atas Album Art (permukaan terbesar/paling sering dilihat), BUKAN di teks lirik/
// judul (larangan eksplisit spec). alpha 0.03f = literal "opacity: 0.03" spec.
@Composable
fun Modifier.calmScanlines(): Modifier {
    return this.drawWithContent {
        drawContent()
        val lineHeight = 4.dp.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.50f to Color.Transparent,
                    0.50f to Color.Black,
                    1.00f to Color.Black
                ),
                startY = 0f,
                endY = lineHeight,
                tileMode = TileMode.Repeated
            ),
            alpha = 0.03f
        )
    }
}

// PILAR D — Organic Grain Overlay. Spec minta "monochromatic noise" bertekstur pasir/debu di
// SELURUH kanvas app dengan opacity maksimal 4%. Compose tidak punya raster-noise generator
// bawaan tanpa RenderEffect (API 31+, di luar minSdk 23 project ini) — didekati dengan speckle
// field seeded (bukan bitmap, pola sama "hand-drawn" seperti calmAberration()/skeuEmboss()):
// posisi & alpha tiap speck dihitung SEKALI per ukuran layar lewat drawWithCache (bukan re-roll
// tiap frame — biaya render tetap murah), rentang alpha 0.015f-0.04f (di bawah plafon 4% spec),
// warna putih polos (monokrom). Dipasang di root Surface (MainActivity.kt) HANYA saat identitas
// Calm Retro aktif — 1 titik cakupan seluruh app, sama seperti root ambient wash identitas lain.
@Composable
fun Modifier.calmGrain(): Modifier {
    val density = LocalDensity.current
    return this.drawWithCache {
        val cellPx = with(density) { 32.dp.toPx() }.coerceAtLeast(1f)
        val cols = (size.width / cellPx).toInt().coerceAtLeast(1)
        val rows = (size.height / cellPx).toInt().coerceAtLeast(1)
        val rnd = Random(42)
        val specks = buildList {
            for (cx in 0 until cols) {
                for (cy in 0 until rows) {
                    val x = cx * cellPx + rnd.nextFloat() * cellPx
                    val y = cy * cellPx + rnd.nextFloat() * cellPx
                    val alpha = 0.015f + rnd.nextFloat() * 0.025f
                    val r = 0.6f + rnd.nextFloat() * 0.8f
                    add(Triple(Offset(x, y), alpha, r))
                }
            }
        }
        onDrawWithContent {
            drawContent()
            specks.forEach { (offset, alpha, r) ->
                drawCircle(color = Color.White.copy(alpha = alpha), radius = r, center = offset)
            }
        }
    }
}

// ============================================================================
// AURORA — Batch 306, tema ke-6. Permintaan user eksplisit: "100% karya hasil ide sendiri tanpa
// contek gaya desain visual apapun" — jadi mekanisme di bawah ini SENGAJA tidak meniru
// tactileEmboss()/skeuEmboss() (shadow/bevel) di atas, calmScanlines()/calmGrain() (retro
// artifact), atau hazeEffect() BlurUtils.kt (blur asli) — kedalaman/identitas di sini datang
// dari WARNA YANG MENGALIR (animated hue-shift), sebuah mekanisme yang belum pernah dipakai di
// app ini sama sekali sampai batch ini.
//
// FASE 1/N dari rollout tema baru (pola sama persis LiquidGlassTypography/LiquidGlassShapes
// Batch 279 — "purely additif, 0 pemakaian di luar file definisi, 0 perubahan visual sampai
// fase registrasi identitas"): fungsi ini BELUM dipanggil dari mana pun (0 call site), dan
// ThemeIdentity.AURORA BELUM ditambahkan ke enum — sengaja dipisah krn `colorsFor()` di
// Theme.kt pakai `when` EXHAUSTIVE (bukan `when` + `else` seperti dispatcher typography/shapes),
// jadi begitu 1 entry enum baru ditambah, SEMUA cabang termasuk warna/typography/shapes WAJIB
// terisi sekaligus di batch yang sama — pola sama kenapa Liquid Glass dulu juga menunda
// registrasi enum ke fase terpisah (isLiquidGlassTheme() sendiri baru muncul Batch 281, fase 3).
//
// Konsep yang sudah dikonfirmasi user sebelum batch ini: (1) Aurora terkunci GELAP PERMANEN
// (aurora borealis = fenomena malam — pola sama CalmRetroColors, bukan otonom 2 mode ala
// Apple/Tactile/Skeu/LiquidGlass), (2) cakupan efek AMBIENT BACKGROUND SAJA dulu (bukan
// rim-glow di tiap panel — itu eksplisit "dipertimbangkan lagi nanti" oleh user, BUKAN dibatalkan
// permanen, BUKAN juga dikerjakan diam-diam duluan).
//
// Mekanisme: BUKAN posisi gradien yang bergeser (menggeser fraction stop berisiko 2 stop
// bertabrakan di 0f/1f, red flag rendering) — sebagai gantinya 5 titik stop TETAP di
// (0/0.22/0.48/0.74/1.0), dan WARNA di 3 stop tengah saling di-lerp() antar 2 hue aurora
// bersebelahan seiring `phase` (0f<->1f, infinite, Reverse — bolak-balik halus, bukan
// Restart yang lompat patah di ujung siklus). Stop pertama & terakhir tetap Color.Transparent
// permanen supaya wash ini berbaur ke tepi kanvas, bukan kotak warna bertepi tegas.
// `rememberInfiniteTransition`+`animateFloat` adalah pola yang SUDAH terbukti compile+jalan di
// app ini (ShimmerBrush(), LibraryScreen.kt) — dipakai lagi di sini apa adanya, bukan API baru.
// Durasi 20000ms (20 detik) SATU ARAH, direverse (~40 detik/siklus penuh) — sengaja lambat/tenang
// krn ini elemen ambient di BELAKANG seluruh konten, bukan aksen yang butuh menarik perhatian;
// titik awal, seperti semua tuning ambient lain di file/project ini, WAJIB dikonfirmasi ulang
// begitu tampil di device sungguhan.
//
// BELUM dipasang ke root Surface (MainActivity.kt, protected/parsial) — itu fase berikutnya,
// sesudah ThemeIdentity.AURORA + AuroraColors terdaftar (fase 2).
@Composable
fun Modifier.auroraGlow(): Modifier {
    val transition = rememberInfiniteTransition(label = "auroraGlow")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraPhase"
    )
    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0.00f to Color.Transparent,
            0.22f to lerp(AuroraGreen, AuroraTeal, phase).copy(alpha = AuroraGlowAlpha),
            0.48f to lerp(AuroraTeal, AuroraViolet, phase).copy(alpha = AuroraGlowAlpha * 0.85f),
            0.74f to lerp(AuroraViolet, AuroraMagenta, phase).copy(alpha = AuroraGlowAlpha * 0.6f),
            1.00f to Color.Transparent
        )
    )
    return this.background(brush)
}
