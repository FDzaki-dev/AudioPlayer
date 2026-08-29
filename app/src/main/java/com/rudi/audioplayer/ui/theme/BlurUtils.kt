package com.rudi.audioplayer.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeEffect

/**
 * Readable glass surface for Compose.
 *
 * Important: Modifier.blur() blurs the composable's own content, not the pixels behind it.
 * Applying it to a container therefore also blurs its text and icons, which is the opposite
 * of what a usable frosted-glass surface should do. This implementation intentionally uses a
 * high-opacity tinted surface plus a subtle edge so content stays crisp and readable.
 *
 * The visual result is still glass-like when placed over artwork/ambient color, while avoiding
 * the common "beautiful but unreadable" failure mode caused by blurring the foreground.
 *
 * Batch 53 — compose-amoled-hybrid-glass-final.md is this project's primary material for the
 * Tactile identity (spec §4: "Every major elevated surface should feel like a translucent layer
 * floating above the AMOLED canvas"). This is the one shared call site every glass surface in the
 * app routes through (Home/Library cards, MiniPlayerBar, NowPlaying panels, every bottom sheet —
 * grep confirms), so the spec's glass rules are expressed centrally here rather than per screen.
 */
@Composable
fun Modifier.frostedGlass(
    tint: Color = MaterialTheme.colorScheme.surface,
    // Batch 61 — was `if (background == AppleLightBackground)`, a comparison that only ever
    // matched the Apple identity's own light background, so Tactile/Skeu's light expressions
    // (different background tokens entirely) silently fell through to the dark-tuned 0.92f.
    // LocalIsDarkTheme is identity-agnostic by construction, so this now works correctly no
    // matter which of the 3 identities is active.
    alpha: Float = if (LocalIsDarkTheme.current) 0.92f else 0.96f,
    // Batch 298 — perkuat efek blur (permintaan user eksplisit): 24dp -> 32dp. Aman dinaikkan
    // lewat default bersama krn dikonfirmasi ulang di atas + grep 12/12 call site frostedGlass()
    // di app ini TANPA argumen (semua pakai default) — 4 identitas lain genuinely tidak pernah
    // membaca parameter ini (lihat komentar isTactile/isSkeu di bawah), jadi menaikkan angka ini
    // 100% hanya mengubah `requestedBlurRadius` yang dikonsumsi `hazeEffect` di cabang
    // isLiquidGlass, 0 dampak ke Apple/Tactile/Skeu/Calm Retro. TETAP "titik awal" seperti
    // liquidGlassAlpha di bawah — dipasangkan sengaja SATU rasionalisasi sama: blur lebih kuat
    // butuh typography lebih kuat juga di atasnya biar tetap kebaca (lihat Type.kt Batch 298).
    // Belum lebih tinggi lagi (mis. 40dp+) krn PROJECT_STATE sudah tandai API32 "berat" utk blur
    // asli — 32dp kompromi naik cukup terasa tanpa melompat ke rentang berisiko performa; WAJIB
    // ikut diverifikasi bareng alpha 0.55/0.65 pas langkah 5/5 roadmap (device sungguhan).
    blurRadius: Dp = 32.dp
): Modifier {
    // Batch 296 — blurRadius was "kept for source compatibility, unused" since Batch 53; now
    // wired to real Haze blur (see `requestedBlurRadius` below) for Liquid Glass specifically.
    // Still a genuine no-op for the other 4 identities — Modifier.blur() would still blur
    // foreground content, so their surface tint remains the readability strategy for them.
    val isTactile = isTactileTheme()
    val isSkeu = isSkeuTheme()
    // Batch 281 — Liquid Glass fase 3: needed below so this identity gets its OWN edgeBrush
    // branch instead of falling into the generic `else`.
    val isLiquidGlass = isLiquidGlassTheme()
    val isDark = LocalIsDarkTheme.current
    // Shape now follows the active theme's own shape tokens instead of a hardcoded 24dp —
    // otherwise every sheet/mini-player using this modifier would keep Apple's soft rounding
    // even under Tactile's/Skeu's own shape identity.
    val shape = MaterialTheme.shapes.large
    // Batch 58 — user feedback: Skeu still reads as glassmorphism. Root cause: this is the one
    // shared helper every panel in the app routes through (mini player, every bottom sheet,
    // Home/Library cards — file header above), and it is literally built as translucent glass
    // (tint at <1 alpha + a soft highlight rim). Skeu's own identity (PROJECT_STATE Batch 57) is
    // explicitly "panel solid, bukan lapisan kaca", so it never should have inherited that look.
    // Forced to full opacity here regardless of the `alpha` param — no call site in this codebase
    // passes one explicitly (grepped), so this can't silently clobber an intentional override.
    // Batch 61 — still forced opaque in BOTH modes: "solid panel, not glass" is an identity trait
    // of Skeu, not something the light/dark toggle should be able to override.
    // Batch 296 — Fase 5 langkah 2/5: alpha tint Liquid Glass diturunkan KHUSUS identitas ini
    // (pola sama persis Skeu's `if (isSkeu) 1f else alpha` di bawah — override per-identitas
    // sudah ada preseden). ALASAN, bukan kosmetik: alpha 0.92/0.96 default sengaja NEAR-OPAQUE
    // krn dulu (Batch 53-281) TIDAK ADA blur asli di belakangnya — tint pekat itu SATU-SATUNYA
    // cara jaga keterbacaan di atas backdrop yang tajam/kacau. Sekarang Liquid Glass dapat
    // `hazeEffect` (blur asli, di bawah), backdrop-nya SUDAH disaring jadi halus — tint
    // setinggi 0.92/0.96 di atas blur asli akan membuat blur itu nyaris tidak kelihatan (cuma
    // nongol 4-8%), menghilangkan tujuan fase 5 ini sama sekali. 0.55f/0.65f = titik awal
    // masuk akal (translucent, blur+variasi warna backdrop tetap kebaca, teks tetap kontras
    // cukup di atas blur+tint gabungan) — BUKAN angka final, WAJIB dituning ulang pas
    // verifikasi visual di device sungguhan (langkah 5/5 roadmap blur), disebut eksplisit di
    // CHANGELOG supaya sesi berikutnya tidak kaget angka ini berubah.
    val liquidGlassAlpha = if (isDark) 0.55f else 0.65f
    val effectiveAlpha = if (isSkeu) 1f else if (isLiquidGlass) liquidGlassAlpha else alpha
    // Batch 53 — spec §8 "Glass edge / highlight" + §9 "Lighting model" (single simulated light,
    // top-left -> bottom-right): a flat single-color border reads as a printed outline, not
    // reflected light. A diagonal two-stop brush (Highlight fading to a second stop) is the
    // minimum structure needed to express "highlight top-left, recede bottom-right" without a
    // bespoke per-corner draw. Batch 61 — both Tactile and Skeu now branch on `isDark` for their
    // own light-tuned token pair (Color.kt "LIGHT VARIANT" sections); Apple's flat branch already
    // handled its own light/dark via the background comparison below, unchanged.
    val edgeBrush = when {
        isTactile -> Brush.linearGradient(
            colors = if (isDark) listOf(TactileHighlight, TactileEdge) else listOf(TactileLightHighlight, TactileLightEdge)
        )
        // Batch 281 — Liquid Glass fase 3, komponen inti pertama (MiniPlayerBar dkk semua
        // route lewat sini — file header di atas). Own branch, BUKAN jatuh ke `else` di bawah:
        // `else` cuma benar mendeteksi "Apple light" (`background == AppleLightBackground`
        // literal), identitas lain (termasuk Liquid Glass yang otonom kedua mode sejak Batch
        // 280 — beda dari Calm Retro yang terkunci gelap permanen jadi tidak pernah kena
        // mismatch ini) akan salah kebagian alpha 0.24f "dark-tuned" bahkan di mode terangnya
        // sendiri — laten bug, bukan disengaja, dihindari dgn branch eksplisit sendiri di sini
        // (pola sama persis Tactile di atas). Pakai `LiquidGlassAccent` (bukan flat neutral
        // onSurface ala Apple) utk highlight rim bernuansa ungu tipis — satu-satunya tempat
        // identitas ini dapat "refraction hint" berwarna, selaras estetika kaca CONVX, TETAP di
        // dalam batas §3b Opsi B (gradient statis, bukan sampling backdrop asli).
        isLiquidGlass -> Brush.linearGradient(
            colors = if (isDark) {
                listOf(LiquidGlassAccent.copy(alpha = 0.32f), LiquidGlassAccent.copy(alpha = 0.06f))
            } else {
                listOf(LiquidGlassAccent.copy(alpha = 0.22f), LiquidGlassAccent.copy(alpha = 0.05f))
            }
        )
        else -> {
            val flat = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (MaterialTheme.colorScheme.background == AppleLightBackground) 0.14f else 0.24f
            )
            Brush.linearGradient(colors = listOf(flat, flat))
        }
    }
    // Batch 58 — Skeu's now-stronger bevel border reads better a hair over the glass-theme
    // hairline (1.dp); Tactile/Apple unchanged.
    val edgeWidth = if (isSkeu) 1.5.dp else 1.dp
    // Batch 296 — Fase 5 langkah 2/5: `hazeEffect` DITAMBAH, bukan menggantikan tint+edge yang
    // sudah ada (§3b desain: blur based + tint warna tipis + edge highlight, bukan blur polos
    // tanpa warna — pola sama CONVX/Apple Liquid Glass asli). Urutan modifier PENTING:
    // `hazeEffect` dipasang PALING LUAR (di `this`, sebelum `.background()`) supaya draw-nya
    // paling belakang — blur tergambar duluan, baru tint semi-transparan (`effectiveAlpha` yg
    // sudah diturunkan di atas) menimpa di atasnya, baru border edge-glow di atas itu lagi.
    // `hazeState` dari `LocalHazeState` (Batch 295's provider di AppNavHost) — 4 identitas lain
    // 0 disentuh (tidak pernah masuk cabang ini sama sekali, `this` mereka tetap Modifier
    // polos apa adanya seperti sebelum batch ini).
    // Bonus kecil: parameter `blurRadius` fungsi ini (baris atas, default 24.dp) sejak dulu
    // cuma "kept for source compatibility" — 0 dipakai sama sekali krn dulu 0 blur asli sama
    // sekali. Sekarang AKHIRNYA benar2 dipakai (utk Liquid Glass), makanya ditangkap ke
    // `requestedBlurRadius` DULU sebelum masuk lambda `hazeEffect{}` — nama beda sengaja,
    // krn di DALAM lambda itu `blurRadius` polos akan merujuk ke property `HazeEffectScope`
    // sendiri (name-shadowing lambda-with-receiver Kotlin), bukan parameter fungsi ini; tanpa
    // capture ke nama lain duluan, `this.blurRadius = blurRadius` di dalam bisa jadi
    // self-assign yang salah/no-op.
    val requestedBlurRadius = blurRadius
    val glassBase = if (isLiquidGlass) {
        this.hazeEffect(state = LocalHazeState.current) { this.blurRadius = requestedBlurRadius }
    } else {
        this
    }
    val base = glassBase.background(tint.copy(alpha = effectiveAlpha), shape)
    // Batch 79 — NEUMORPHISM: Skeu no longer draws ANY edge/border here at all (was a
    // brushed-metal repeating-stripe rim, Batch 73's isSkeu branch above — deleted along with
    // every other border in this identity's redesign, see TactileDepth.kt's skeuEmboss()).
    // Genuine neumorphism has NO line of any kind around a panel — depth comes exclusively from
    // dual-shadow (skeuEmboss()) or, for sheets/panels that only route through frostedGlass()
    // without also being wrapped in skeuEmboss(), from the panel simply being a flat
    // near-background tone (SkeuNeuSurfaceDark/Light isn't literally used here — this modifier
    // still takes whatever `tint` the caller passes — but Skeu's own solid-opaque identity,
    // Batch 58, already reads as a distinct shape without needing a drawn line). Tactile/Apple
    // unchanged — still get their edgeBrush border exactly as before.
    return if (isSkeu) base else base.border(edgeWidth, edgeBrush, shape)
}
