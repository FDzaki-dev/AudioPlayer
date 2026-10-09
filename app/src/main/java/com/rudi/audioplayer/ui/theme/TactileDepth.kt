package com.rudi.audioplayer.ui.theme

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as GfxCanvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
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
// Batch 551 — `embossSurface()` kini hanya bungkus tipis: SKALA tekan (0.985f, sama seperti
// sebelumnya) + mesin kedalaman Neumorphism bersama `neuDepth()` (bagian bawah file ini, port
// dari Boomly B180-B190: permukaan pelat LEBIH TERANG dari kanvas, bevel facet per-sisi menurut
// cahaya kiri-atas, bayangan jatuh Gaussian 3 lapis, sumur cekung saat ditekan). Parameter lama
// (4 alpha border + 2 alpha shadow, Batch 58/62) DIHAPUS karena seluruh gambar border/gradien/
// drop-shadow tangan lama diganti mesin itu — fungsi ini private, satu-satunya pemanggil
// `tactileEmboss()` di bawah, tanda tangan publik tidak berubah.
@Composable
private fun Modifier.embossSurface(
    shape: Shape,
    elevation: Dp,
    pressed: Boolean,
    surfaceTop: Color,
    surfaceBottom: Color,
    label: String
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        label = "${label}Scale"
    )
    return this
        .scale(scale)
        .neuDepth(
            shape = shape,
            elevation = elevation,
            faceTop = surfaceTop,
            faceBottom = surfaceBottom,
            pressed = pressed
        )
}

@Composable
fun Modifier.tactileEmboss(
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 8.dp,
    pressed: Boolean = false
): Modifier {
    // Batch 61 — identitas Tactile otonom di kedua mode (lihat Theme.kt): token light/dark dipilih
    // lewat LocalIsDarkTheme, bukan parameter baru di tiap call site.
    // Batch 551 — nada border/shadow Batch 62 (alpha 0.16/0.55/0.90 dst) digantikan profil mesin
    // `neuDepth()` (alpha bevel/bayangan diturunkan dari warna permukaan Tactile sendiri, mode
    // terang memakai kekuatan bayangan 0.42x supaya tidak jadi noda gelap di kanvas terang).
    val isDark = LocalIsDarkTheme.current
    return this.embossSurface(
        shape = shape,
        elevation = elevation,
        pressed = pressed,
        surfaceTop = if (isDark) TactileSurfaceVariant else TactileLightSurfaceVariant,
        surfaceBottom = if (isDark) TactileSurface else TactileLightSurface,
        label = "tactileEmboss"
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
// Batch 551 — dual-shadow tumpukan manual Batch 79-81 (5 layer drawPath offset + clipRect halo)
// DIGANTIKAN mesin kedalaman Neumorphism Boomly `neuDepth()` (bagian bawah file ini): permukaan
// pelat sekarang diangkat dari kanvas oleh LUMINANSI + bevel facet + bayangan Gaussian, dan
// pressed = sumur cekung (lantai gelap + bayangan dalam + bibir terang) alih-alih membalik
// diagonal terang/gelap. Yang DIPERTAHANKAN dari Batch 79-81: skala tekan 0.978f, warna panel
// `SkeuNeuSurfaceDark/Light`, dan glint Zamrud (identitas Titanium + sentuhan Emerald) apa
// adanya — tanda tangan publik tidak berubah. Token Skeu{Specular,AmbientOcclusion,Highlight,
// Shadow} dkk. di Color.kt tidak lagi dibaca fungsi ini (tidak dihapus: dipakai AlbumArtHero.kt).
@Composable
fun Modifier.skeuEmboss(
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 8.dp,
    pressed: Boolean = false
): Modifier {
    val isDark = LocalIsDarkTheme.current
    val panelFill = if (isDark) SkeuNeuSurfaceDark else SkeuNeuSurfaceLight
    val emerald = if (isDark) SkeuEmerald else SkeuLightEmerald

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.978f else 1f,
        label = "skeuEmbossScale"
    )
    // Batch 80 — glint Zamrud: layer TERPISAH (radial kecil, warna murni SkeuEmerald), idle 0.20f
    // naik 0.52f saat pressed (permata menyala redup di logam titanium).
    val emeraldAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.52f else 0.20f,
        label = "skeuEmbossEmeraldGlow"
    )
    // Posisi glint ikut sisi terang: kiri-atas normal, kanan-bawah saat pressed (sumur cekung
    // memantulkan cahaya dari dinding seberang).
    val dir = if (pressed) -1f else 1f

    return this
        .scale(scale)
        .neuDepth(
            shape = shape,
            elevation = elevation,
            faceTop = panelFill,
            faceBottom = panelFill,
            pressed = pressed
        )
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
// Batch 399 — Optimasi Compose: kelas bug sama Batch 392/395/396/397/398 (allocation dibangun
// ulang tiap recomposition tanpa `remember`), tapi titik penyebarannya beda — bukan
// `Brush.*Gradient()` LANGSUNG di badan sebuah screen (grep app-wide sudah 0 sisa sejak Batch
// 398), melainkan fungsi Modifier EXTENSION di file ini yang dipanggil ulang dari scope panas.
// Ditemukan lewat audit lanjutan: `calmAberration()` dipanggil `MiniPlayerBar.kt` (baris
// `isCalmRetro -> Modifier.calmAberration(bias = 2.dp)`) — MiniPlayerBar sengaja koleksi
// `playbackProgress` LOKAL tiap detik selama musik main (desain Batch 353), jadi SELURUH badan
// fungsi itu (termasuk `when` yang memanggil `calmAberration()` ini) recompose 1x/detik. Sebelum
// fix ini, tiap tick memanggil ulang `this.drawBehind { ... }` — lambda BARU (capture `biasPx`)
// tiap panggilan, ekuivalen 1 instance `DrawBehindElement` baru tiap detik untuk identitas Calm
// Retro, padahal `bias` itu sendiri konstan (literal `2.dp` di MiniPlayerBar, `3.dp` default di
// 2 pemanggil lain — NowPlayingScreen.kt/SettingsScreen.kt, keduanya TIDAK dalam scope panas
// tick 1x/detik sejak Batch 353 memisahkan posisi/durasi keluar dari body composable besar).
// Fix: Modifier hasil `drawBehind` dibungkus `remember(bias)` — begitu `bias` sama (kasus
// SEMUA 3 pemanggil, tiap panggilan pakai literal Dp tetap), instance yang sama dipakai ulang
// alih-alih dibangun dari nol. `size`/`center`/`radius` di dalam lambda draw TETAP dihitung
// fresh tiap draw call sungguhan (DrawScope, bukan sesuatu yang bisa/perlu di-remember —
// bergantung ukuran layout aktual) — cuma WADAH Modifier-nya yang sekarang stabil lintas
// recomposition tak-terkait. **Zero behavior change**: 2 lingkaran radial-gradient aberrasi
// tetap identik visual & posisi, cuma alokasi objek berkurang saat `bias` tidak berubah.
@Composable
fun Modifier.calmAberration(bias: Dp = 3.dp): Modifier {
    val aberrationModifier = remember(bias) {
        Modifier.drawBehind {
            val off = bias.toPx()
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
    return this.then(aberrationModifier)
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

// ============================================================================
// Batch 551 — MESIN KEDALAMAN NEUMORPHISM BERSAMA (port dari Boomly B180-B190, request user:
// "Terapkan effect kedalaman Neumorphism punya project Boomly ke semua theme yang ada di project
// SONIX"). Bukan tema baru: ini mekanisme KEDALAMAN yang dipakai SEMUA 6 identitas (Apple, Tactile,
// Neumorphism/Skeu, Calm Retro, Liquid Glass, Aurora) lewat 3 pintu — (1) `tactileEmboss()` /
// `skeuEmboss()` (panel padat Tactile & Neumorphism), (2) `frostedGlass()` (BlurUtils.kt: panel
// kaca Apple/Calm Retro/Liquid Glass/Aurora, lewat `neuHollowShadow()` + `neuBevelOnly()`),
// (3) `neuSurface()` (kartu datar Apple/Calm Retro/Aurora di Home/Statistik/preview tema).
// Palet/identitas tiap tema TIDAK diganti — mesin menurunkan warna bevel/bayangan/lantai sumur dari
// warna permukaan tema itu sendiri (prinsip Boomly B187: "mesin & aturan SAMA, palet/kekuatan
// mengikuti identitas tema").
//
// 3 sumber kedalaman yang terbaca (alasan Boomly B180 \"nyaru\": panel hanya +-3 level dari kanvas
// + bayangan sehue kanvas = kedalaman nyaris tak terlihat):
//  1. LUMINANSI — permukaan pelat diangkat lebih terang dari kanvas (`neuStyle()`), lantai sumur
//     jauh lebih gelap.
//  2. BEVEL FACET per-sisi menurut cahaya kiri-atas (-0.5522, -0.8337): sisi menghadap cahaya =
//     sorot, membelakangi = gelap, alpha berskala cos sudut (bukan gradien miring palsu).
//  3. BAYANGAN JATUH Gaussian 3 lapis (kontak / tengah / ambient) di bitmap perangkat lunak
//     (`Canvas(ImageBitmap)` + `BlurMaskFilter` — BUKAN canvas hardware), di-cache GLOBAL ber-batas
//     (LRU 48) sehingga elemen berukuran sama berbagi 1 bitmap & 0 render blur per frame.
// Pressed = sumur CEKUNG (lantai gelap + bayangan dalam + bibir terang), di-crossfade 110ms.
//
// Beda dari port Boomly (sengaja, scope lebih ramping): TANPA tekstur butiran, alur ukir bingkai,
// kubah knob/rim pil/tab, dan TANPA template 9-slice (bitmap per-ukuran saja — kartu SONIX tidak
// beranimasi ukuran). Alpha/warna = simulasi statis, BELUM dituning di device.
// ============================================================================

private const val NeuLightX = -0.5522f
private const val NeuLightY = -0.8337f
private const val NeuCornerSteps = 4
private const val NeuSmallScale = 0.75f
private const val NeuBigScale = 0.3333f
private const val NeuHugeScale = 0.2f
private const val NeuWellScale = 0.75f

/** Profil kedalaman turunan dari 2 warna permukaan + mode. [shadowStrength] 1f (gelap) / 0.42f
 *  (terang): bayangan hitam pekat di kanvas terang jadi noda kasar. */
internal data class NeuStyle(
    val faceTop: Color,
    val faceBottom: Color,
    val rimLight: Color,
    val rimLightAlpha: Float,
    val rimShade: Color,
    val rimShadeAlpha: Float,
    val bevelWidth: Dp,
    val castShadow: Color,
    val shadowStrength: Float,
    val wellFloor: Color
)

private fun neuStyle(faceTop: Color, faceBottom: Color, isDark: Boolean): NeuStyle {
    val top = faceTop.copy(alpha = 1f)
    val bottom = faceBottom.copy(alpha = 1f)
    return if (isDark) {
        NeuStyle(
            faceTop = lerp(top, Color.White, 0.09f),
            faceBottom = lerp(bottom, Color.White, 0.04f),
            rimLight = lerp(top, Color.White, 0.88f),
            rimLightAlpha = 0.40f,
            rimShade = lerp(bottom, Color.Black, 0.92f),
            rimShadeAlpha = 0.58f,
            bevelWidth = 1.5.dp,
            castShadow = lerp(bottom, Color.Black, 0.95f),
            shadowStrength = 1f,
            wellFloor = lerp(bottom, Color.Black, 0.55f)
        )
    } else {
        NeuStyle(
            faceTop = lerp(top, Color.White, 0.55f),
            faceBottom = bottom,
            rimLight = Color.White,
            rimLightAlpha = 0.95f,
            rimShade = lerp(bottom, Color.Black, 0.55f),
            rimShadeAlpha = 0.30f,
            bevelWidth = 1.5.dp,
            castShadow = lerp(bottom, Color.Black, 0.60f),
            shadowStrength = 0.42f,
            wellFloor = lerp(bottom, Color.Black, 0.12f)
        )
    }
}

/** Satu lapis bayangan jatuh. [dx]/[dy] = offset (dp, + = kanan/bawah, menjauhi cahaya kiri-atas),
 *  [blur] = lebar blur (~2 sigma, dp), [alpha] = kepekatan lapis. */
private data class NeuShadowLayer(val dx: Float, val dy: Float, val blur: Float, val alpha: Float)

private data class NeuShadowSpec(val layers: List<NeuShadowLayer>, val bleedDp: Float)

/** Lapisan = pelat Boomly (kontak tajam + tengah + ambient lebar), diskalakan ke `elevation`
 *  (8.dp = 1x, dikuantisasi 0.25 supaya spec/bitmap dibagi antar elemen berelevasi mirip). */
private fun neuShadowSpec(elevation: Dp, strength: Float): NeuShadowSpec {
    val k = ((elevation.value / 8f).coerceIn(0.5f, 2f) * 4f).roundToInt() / 4f
    return NeuShadowSpec(
        layers = listOf(
            NeuShadowLayer(1.0f * k, 1.5f * k, 2.0f * k, 0.85f * strength),
            NeuShadowLayer(4.0f * k, 6.0f * k, 8.0f * k, 0.62f * strength),
            NeuShadowLayer(10.0f * k, 15.0f * k, 18.0f * k, 0.50f * strength)
        ),
        bleedDp = ceil(34f * k)
    )
}

/** `BlurMaskFilter.radius` -> sigma = 0.57735 * radius + 0.5 (konversi Skia), dibalik supaya
 *  parameter layer = sigma yang diinginkan (px bitmap); minimal 0.5 (radius <= 0 melempar). */
private fun neuBlurRadius(sigmaPx: Float): Float = ((sigmaPx - 0.5f) / 0.57735f).coerceAtLeast(0.5f)

private fun Outline.toNeuPath(): Path {
    val o = this
    return when (o) {
        is Outline.Rectangle -> Path().apply { addRect(o.rect) }
        is Outline.Rounded -> Path().apply { addRoundRect(o.roundRect) }
        is Outline.Generic -> o.path
    }
}

/** Kunci cache global. [kind]: 2 bayangan jatuh per-ukuran, 3 bayangan dalam sumur, 4 permukaan. */
private data class NeuStoreKey(
    val kind: Int,
    val w: Int,
    val h: Int,
    val density: Float,
    val shape: Shape?,
    val style: NeuStyle?,
    val extra: Any?
)

/** Cache GLOBAL bitmap efek (LRU, maks 48 entri) — hanya dipakai dari blok cache gambar (thread UI). */
private object NeuBitmapStore {
    private const val MAX_ENTRIES = 48
    private val map = object : LinkedHashMap<Any, ImageBitmap>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Any, ImageBitmap>?): Boolean =
            size > MAX_ENTRIES
    }

    fun get(key: Any, build: () -> ImageBitmap): ImageBitmap = synchronized(map) {
        val hit = map[key]
        if (hit != null) {
            hit
        } else {
            val fresh = build()
            map[key] = fresh
            fresh
        }
    }
}

/** Pembungkus blok `drawWithCache` yang `equals`-nya = kesamaan KUNCI (pola Boomly B190): rantai
 *  modifier yang diulang saat rekomposisi dgn kunci sama dianggap SAMA -> node tak di-update &
 *  cache gambar tak di-invalidate (tanpa `composed`). Kunci = SEMUA parameter yang dipakai blok. */
private class NeuDrawBlock(
    private val keys: List<Any?>,
    private val block: CacheDrawScope.() -> DrawResult
) : (CacheDrawScope) -> DrawResult {
    override fun invoke(scope: CacheDrawScope): DrawResult = scope.block()

    override fun equals(other: Any?): Boolean = other is NeuDrawBlock && other.keys == keys

    override fun hashCode(): Int = keys.hashCode()
}

private fun Modifier.neuDraw(tag: String, vararg keys: Any?, block: CacheDrawScope.() -> DrawResult): Modifier =
    this.drawWithCache(NeuDrawBlock(listOf(tag, *keys), block))

/** Permukaan pelat ter-bake 48x48 (tak bergantung ukuran, di-skala bilinear): gradien
 *  kiri-atas -> kanan-bawah + peredupan lembut ke kanan-bawah + kilau lembut dari sisi cahaya.
 *  1 `drawImage` opak menggantikan beberapa pengisian gradien layar penuh per kartu. */
private fun renderNeuFace(style: NeuStyle): ImageBitmap {
    val n = 48
    val top = style.faceTop.toArgb()
    val bottom = style.faceBottom.toArgb()
    val lit = style.rimLight.toArgb()
    val shade = style.rimShade.toArgb()
    val shifts = intArrayOf(16, 8, 0)
    val px = IntArray(n * n)
    for (y in 0 until n) {
        val v = (y + 0.5f) / n
        for (x in 0 until n) {
            val u = (x + 0.5f) / n
            val t = (u + v) * 0.5f
            val dv = hypot(u - 0.40f, v - 0.35f).coerceIn(0f, 1f)
            val va = dv * 0.14f
            val ds = (hypot(u - 0.20f, v - 0.05f) / 0.85f).coerceIn(0f, 1f)
            val sa = (1f - ds) * 0.07f
            var rgb = 0
            for (shift in shifts) {
                val c0 = ((top shr shift) and 255).toFloat()
                val c1 = ((bottom shr shift) and 255).toFloat()
                var c = c0 + (c1 - c0) * t
                c = c * (1f - va) + ((shade shr shift) and 255).toFloat() * va
                c = c * (1f - sa) + ((lit shr shift) and 255).toFloat() * sa
                rgb = rgb or (c.roundToInt().coerceIn(0, 255) shl shift)
            }
            px[y * n + x] = (0xFF shl 24) or rgb
        }
    }
    return Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Render bayangan jatuh (timbul) ke bitmap perangkat lunak. Bitmap = bentuk + margin seragam
 *  `spec.bleedDp` di semua sisi; semua panjang dihitung dalam px-bitmap (`dp * density * scale`),
 *  `Density(k)` membuat sudut (dalam dp) ikut skala. */
private fun renderNeuCastShadow(
    shape: Shape,
    wPx: Float,
    hPx: Float,
    density: Float,
    style: NeuStyle,
    spec: NeuShadowSpec,
    scale: Float
): ImageBitmap {
    val k = density * scale
    val m = spec.bleedDp
    val bw = ceil((wPx + 2f * m * density) * scale).toInt().coerceAtLeast(2)
    val bh = ceil((hPx + 2f * m * density) * scale).toInt().coerceAtLeast(2)
    val bmp = ImageBitmap(bw, bh, ImageBitmapConfig.Argb8888)
    val canvas = GfxCanvas(bmp)
    val base = shape.createOutline(Size(wPx * scale, hPx * scale), LayoutDirection.Ltr, Density(k)).toNeuPath()
    for (layer in spec.layers) {
        val path = Path().apply { addPath(base, Offset(m * k + layer.dx * k, m * k + layer.dy * k)) }
        val paint = Paint().apply {
            isAntiAlias = true
            color = style.castShadow.copy(alpha = layer.alpha)
        }
        paint.asFrameworkPaint().maskFilter =
            BlurMaskFilter(neuBlurRadius(layer.blur * 0.5f * k), BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(path, paint)
    }
    return bmp
}

/** Render bayangan DALAM (cekung): oklusi ambien merata + bayangan gelap dari dinding sisi
 *  kiri-atas + bibir terang tipis di dinding sisi kanan-bawah, dipotong ke bentuk (`DstOut` pada
 *  area luar = tepi tetap mulus). */
private fun renderNeuWellInner(shape: Shape, wPx: Float, hPx: Float, density: Float, style: NeuStyle): ImageBitmap {
    val bw = (wPx * NeuWellScale).roundToInt().coerceAtLeast(1)
    val bh = (hPx * NeuWellScale).roundToInt().coerceAtLeast(1)
    val k = density * NeuWellScale
    val bmp = ImageBitmap(bw, bh, ImageBitmapConfig.Argb8888)
    val canvas = GfxCanvas(bmp)
    val shapePath = shape.createOutline(Size(bw.toFloat(), bh.toFloat()), LayoutDirection.Ltr, Density(k)).toNeuPath()
    val pad = 48f * k
    fun outside(dx: Float, dy: Float): Path = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(-pad + dx, -pad + dy, bw + pad + dx, bh + pad + dy))
        addPath(shapePath, Offset(dx, dy))
    }
    val ao = Paint().apply {
        isAntiAlias = true
        color = style.castShadow.copy(alpha = 0.40f)
    }
    ao.asFrameworkPaint().maskFilter = BlurMaskFilter(neuBlurRadius(2.6f * k), BlurMaskFilter.Blur.NORMAL)
    canvas.drawPath(outside(0.4f * k, 0.6f * k), ao)
    val shadow = Paint().apply {
        isAntiAlias = true
        color = style.castShadow.copy(alpha = 0.85f)
    }
    shadow.asFrameworkPaint().maskFilter = BlurMaskFilter(neuBlurRadius(1.6f * k), BlurMaskFilter.Blur.NORMAL)
    canvas.drawPath(outside(2.0f * k, 2.6f * k), shadow)
    val lip = Paint().apply {
        isAntiAlias = true
        color = style.rimLight.copy(alpha = 0.20f)
    }
    lip.asFrameworkPaint().maskFilter = BlurMaskFilter(neuBlurRadius(0.5f * k), BlurMaskFilter.Blur.NORMAL)
    canvas.drawPath(outside(-1.0f * k, -1.2f * k), lip)
    val cut = Paint().apply {
        isAntiAlias = true
        color = Color.Black
        blendMode = BlendMode.DstOut
    }
    canvas.drawPath(outside(0f, 0f), cut)
    return bmp
}

/** Gambar bitmap bayangan per-ukuran (margin seragam [bleedDp] di semua sisi). */
private fun DrawScope.drawNeuShadow(bmp: ImageBitmap, density: Float, scale: Float, bleedDp: Float, alpha: Float) {
    val m = (bleedDp * density).roundToInt()
    drawImage(
        image = bmp,
        dstOffset = IntOffset(-m, -m),
        dstSize = IntSize((bmp.width / scale).roundToInt(), (bmp.height / scale).roundToInt()),
        alpha = alpha
    )
}

private fun neuQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Path =
    Path().apply {
        moveTo(x0, y0)
        lineTo(x1, y1)
        lineTo(x2, y2)
        lineTo(x3, y3)
        close()
    }

/** Bevel FACET untuk pelat bersudut radius [r] (0 = persegi), lebar [b]: 4 sisi lurus + 4 sudut
 *  yang dipecah [NeuCornerSteps] irisan busur; tiap facet diwarnai menurut `dot(normal, arahCahaya)`
 *  (sisi menghadap cahaya = sorot, membelakangi = gelap). [scale] mengecilkan alpha bevel (panel
 *  kaca memakai < 1 supaya tak bertumpuk berat dgn border tema). */
private fun neuRoundFacets(
    out: MutableList<Pair<Path, Color>>,
    w: Float,
    h: Float,
    r: Float,
    b: Float,
    style: NeuStyle,
    scale: Float
) {
    fun colorFor(nx: Float, ny: Float): Color {
        val l = nx * NeuLightX + ny * NeuLightY
        val scaled = min(1f, abs(l) / 0.83f)
        return if (l > 0f) {
            style.rimLight.copy(alpha = style.rimLightAlpha * scaled * scale)
        } else {
            style.rimShade.copy(alpha = style.rimShadeAlpha * scaled * scale)
        }
    }
    val bi = (r - b).coerceAtLeast(0f)
    if (w - 2f * r > 0.5f) {
        out.add(Pair(neuQuad(r, 0f, w - r, 0f, w - r, b, r, b), colorFor(0f, -1f)))
        out.add(Pair(neuQuad(r, h, w - r, h, w - r, h - b, r, h - b), colorFor(0f, 1f)))
    }
    if (h - 2f * r > 0.5f) {
        out.add(Pair(neuQuad(w, r, w, h - r, w - b, h - r, w - b, r), colorFor(1f, 0f)))
        out.add(Pair(neuQuad(0f, r, 0f, h - r, b, h - r, b, r), colorFor(-1f, 0f)))
    }
    if (r < 0.5f) return
    // Pusat busur & sudut awal (derajat, y ke bawah): kiri-atas 180->270, kanan-atas 270->360,
    // kanan-bawah 0->90, kiri-bawah 90->180.
    val cxs = floatArrayOf(r, w - r, w - r, r)
    val cys = floatArrayOf(r, r, h - r, h - r)
    val starts = floatArrayOf(180f, 270f, 0f, 90f)
    val step = 90f / NeuCornerSteps
    for (i in 0 until 4) {
        val cx = cxs[i]
        val cy = cys[i]
        val outerRect = Rect(cx - r, cy - r, cx + r, cy + r)
        val innerRect = Rect(cx - bi, cy - bi, cx + bi, cy + bi)
        for (s in 0 until NeuCornerSteps) {
            val a0 = starts[i] + s * step
            val mid = Math.toRadians((a0 + step / 2f).toDouble())
            val path = Path().apply {
                arcTo(outerRect, a0, step, true)
                if (bi > 0.05f) {
                    arcTo(innerRect, a0 + step, -step, false)
                } else {
                    lineTo(cx, cy)
                }
                close()
            }
            out.add(Pair(path, colorFor(cos(mid).toFloat(), sin(mid).toFloat())))
        }
    }
}

/** Bayangan jatuh (digambar DI BELAKANG, boleh keluar batas — jangan di-clip di atasnya).
 *  [press] = progres tekan 0..1 (null = tak pernah ditekan): bayangan memudar saat pressed, bibir
 *  sumur (garis terang tipis di tepi kanan-bawah LUAR) muncul. [hollow] = permukaan di atasnya
 *  tembus pandang (panel kaca): area DALAM bentuk dikecualikan lewat `ClipOp.Difference` supaya
 *  bayangan tak menggelapkan isi kaca. */
private fun Modifier.neuCastShadow(
    shape: Shape,
    style: NeuStyle,
    spec: NeuShadowSpec,
    press: State<Float>?,
    hollow: Boolean
): Modifier = neuDraw("neuCast", shape, style, spec, press, hollow) {
    val w = size.width
    val h = size.height
    val d = density
    if (w < 2f || h < 2f) {
        onDrawBehind { }
    } else {
        val wi = w.roundToInt()
        val hi = h.roundToInt()
        // Panel raksasa (sheet kaca layar-penuh) memakai skala 0.2f: bayangannya sangat buram,
        // jadi bitmap kecil di-upsample tetap mulus & memori per entri cache tetap < ~0.6 MB.
        val scale = when {
            w * h > (480f * d) * (480f * d) -> NeuHugeScale
            max(w, h) > 160f * d -> NeuBigScale
            else -> NeuSmallScale
        }
        val bmp = NeuBitmapStore.get(NeuStoreKey(2, wi, hi, d, shape, style, spec)) {
            renderNeuCastShadow(shape, w, h, d, style, spec, scale)
        }
        val base = shape.createOutline(size, layoutDirection, this).toNeuPath()
        val lip: Path? = if (!hollow && press != null) {
            Path().apply { addPath(base, Offset(0.7f * d, 1.0f * d)) }
        } else {
            null
        }
        onDrawBehind {
            val t = press?.value ?: 0f
            val raisedAlpha = 1f - t
            if (raisedAlpha > 0.01f) {
                if (hollow) {
                    clipPath(base, ClipOp.Difference) {
                        drawNeuShadow(bmp, d, scale, spec.bleedDp, raisedAlpha)
                    }
                } else {
                    drawNeuShadow(bmp, d, scale, spec.bleedDp, raisedAlpha)
                }
            }
            if (t > 0.01f && lip != null) {
                drawPath(lip, style.rimLight.copy(alpha = 0.22f * t))
            }
        }
    }
}

/** Permukaan pelat + bevel + (saat pressed) sumur cekung, digambar DI DALAM bentuk (pasang SETELAH
 *  `.clip(shape)` bila [paintFace]). [paintFace] false = hanya bevel (panel kaca: tint tema sudah
 *  digambar `frostedGlass()`, pasang SETELAH `.background(...)`). [bevelScale] = pengali alpha bevel. */
private fun Modifier.neuFace(
    shape: Shape,
    style: NeuStyle,
    press: State<Float>?,
    paintFace: Boolean,
    bevelScale: Float
): Modifier = neuDraw("neuFace", shape, style, press, paintFace, bevelScale) {
    val w = size.width
    val h = size.height
    val d = density
    if (w < 4f || h < 4f) {
        onDrawBehind { }
    } else {
        val wi = w.roundToInt()
        val hi = h.roundToInt()
        val face: ImageBitmap? =
            if (paintFace) NeuBitmapStore.get(NeuStoreKey(4, 0, 0, 1f, null, style, null)) { renderNeuFace(style) } else null
        val well: ImageBitmap? =
            if (paintFace && press != null) {
                NeuBitmapStore.get(NeuStoreKey(3, wi, hi, d, shape, style, null)) {
                    renderNeuWellInner(shape, wi.toFloat(), hi.toFloat(), d, style)
                }
            } else {
                null
            }
        val b = style.bevelWidth.toPx()
        val outline = shape.createOutline(size, layoutDirection, this)
        val radius: Float = when (outline) {
            is Outline.Rounded -> min(outline.roundRect.topLeftCornerRadius.x, min(w, h) / 2f)
            is Outline.Rectangle -> 0f
            is Outline.Generic -> -1f
        }
        val facets = ArrayList<Pair<Path, Color>>(4 + 4 * NeuCornerSteps)
        if (radius >= 0f) neuRoundFacets(facets, w, h, radius, b, style, bevelScale)
        val fallbackPath: Path? = if (radius < 0f) outline.toNeuPath() else null
        val fallbackBrush = Brush.linearGradient(
            0.00f to style.rimLight.copy(alpha = style.rimLightAlpha * bevelScale),
            0.50f to Color.Transparent,
            1.00f to style.rimShade.copy(alpha = style.rimShadeAlpha * bevelScale),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        onDrawBehind {
            val t = press?.value ?: 0f
            if (face != null) drawImage(face, dstSize = IntSize(wi, hi))
            if (t > 0.01f) {
                drawRect(style.wellFloor.copy(alpha = t))
                if (well != null) drawImage(well, dstSize = IntSize(wi, hi), alpha = t)
            }
            val bevelAlpha = 1f - t
            if (bevelAlpha > 0.01f) {
                for (f in facets) {
                    drawPath(f.first, f.second.copy(alpha = f.second.alpha * bevelAlpha))
                }
                if (fallbackPath != null) {
                    drawPath(fallbackPath, brush = fallbackBrush, alpha = bevelAlpha, style = Stroke(width = b))
                }
            }
        }
    }
}

/**
 * Panel PADAT berkedalaman (Tactile, Neumorphism, dan kartu datar Apple/Calm Retro/Aurora):
 * bayangan jatuh Gaussian di belakang -> `.clip(shape)` -> permukaan pelat ter-angkat + bevel
 * facet. [pressed] non-null = elemen interaktif (sumur cekung saat true); null = statis (tanpa
 * bitmap sumur). [faceTop]/[faceBottom] = warna permukaan tema (mesin mengangkat luminansinya);
 * [isDark] default = mode tema aktif (preview picker tema mengirim mode preview-nya sendiri).
 */
@Composable
fun Modifier.neuDepth(
    shape: Shape,
    elevation: Dp,
    faceTop: Color,
    faceBottom: Color,
    pressed: Boolean? = null,
    isDark: Boolean = LocalIsDarkTheme.current
): Modifier {
    val style = remember(faceTop, faceBottom, isDark) { neuStyle(faceTop, faceBottom, isDark) }
    val spec = remember(elevation, style.shadowStrength) { neuShadowSpec(elevation, style.shadowStrength) }
    val press: State<Float>? = if (pressed != null) {
        animateFloatAsState(
            targetValue = if (pressed) 1f else 0f,
            animationSpec = tween(durationMillis = 110),
            label = "neuDepthPress"
        )
    } else {
        null
    }
    return this
        .neuCastShadow(shape, style, spec, press, false)
        .clip(shape)
        .neuFace(shape, style, press, true, 1f)
}

/** Kartu datar tema non-panel (Apple, Calm Retro, Aurora) — warna permukaan dari `colorScheme.surface`.
 *  Pasangkan dengan `Surface(color = Color.Transparent, tonalElevation = 0.dp)` (pola yang sama
 *  dgn cabang Tactile/Neumorphism): fill digambar di sini, bukan oleh Surface. */
@Composable
fun Modifier.neuSurface(shape: Shape, elevation: Dp = 8.dp): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    return this.neuDepth(shape = shape, elevation = elevation, faceTop = surface, faceBottom = surface)
}

/** Bayangan jatuh untuk panel KACA (tint di atasnya tembus pandang) — dipakai `frostedGlass()`
 *  SEBELUM `.background(tint)`. Area dalam bentuk dikecualikan (lihat [neuCastShadow]). */
@Composable
internal fun Modifier.neuHollowShadow(
    shape: Shape,
    tint: Color,
    elevation: Dp = 8.dp,
    isDark: Boolean = LocalIsDarkTheme.current
): Modifier {
    val style = remember(tint, isDark) { neuStyle(tint, tint, isDark) }
    val spec = remember(elevation, style.shadowStrength) { neuShadowSpec(elevation, style.shadowStrength) }
    return this.neuCastShadow(shape, style, spec, null, true)
}

/** Bevel facet untuk panel KACA — dipakai `frostedGlass()` SETELAH `.background(tint)` supaya
 *  tidak tertimbun tint. Alpha 0.7x (border tema tetap digambar sesudahnya). */
@Composable
internal fun Modifier.neuBevelOnly(
    shape: Shape,
    tint: Color,
    isDark: Boolean = LocalIsDarkTheme.current
): Modifier {
    val style = remember(tint, isDark) { neuStyle(tint, tint, isDark) }
    return this.neuFace(shape, style, null, false, 0.7f)
}
