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
    // Batch 300 — verifikasi device itu SUDAH datang: user laporkan sedikit stuttering pas
    // scroll (tidak sampai freeze). Ini persis risiko yang param ini sendiri sudah tandai di
    // atas ("dekat batas nyaman performa") — blur asli Haze resample tiap frame saat konten di
    // belakang kaca berubah (mis. MiniPlayerBar yang selalu melayang di atas layar yg lagi
    // di-scroll, per §5 langkah 5 LIQUID_GLASS_BLUR_ENGINE_DESIGN.md), jadi radius lebih besar =
    // GPU cost per-frame lebih besar. Diturunkan balik 32dp → 24dp (nilai sebelum dinaikkan
    // Batch 298, satu-satunya batch yang mengubah angka ini sejak diaktifkan Batch 296, dan 0
    // laporan stutter pernah masuk selama radius masih di 24dp). Tint (`liquidGlassAlpha`,
    // 0.38f/0.48f sejak Batch 299) TIDAK ikut disentuh — itu lever "blur ketutup/tidak", bukan
    // lever performa, dan user kali ini tidak melaporkan masalah visibilitas. Kalau stutter
    // masih terasa di 24dp, lever berikutnya yang harus dicoba adalah radius lebih rendah lagi
    // atau meninjau frekuensi re-render MiniPlayerBar saat progress lagu jalan — BUKAN tint.
    blurRadius: Dp = 24.dp
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
    // Batch 310 — Aurora rim-glow per-panel: needed below for the same reason as isLiquidGlass
    // above, its own edgeBrush branch instead of the generic `else`.
    val isAurora = isAuroraTheme()
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
    // `hazeEffect` (blur asli, di bawah), backdrop-nya SUDAH disaring jadi halus — tint tinggi
    // di atas blur asli akan membuat blur itu nyaris tidak kelihatan, menghilangkan tujuan
    // fase 5 ini sama sekali.
    // Batch 299 — langkah 5/5 iterasi kedua: user melaporkan LANGSUNG dari device API 33+
    // sungguhan (bukan API 31/32 — justru tier "Runtime Shader" tercepat/paling ringan per
    // LIQUID_GLASS_BLUR_ENGINE_DESIGN.md §2) bahwa blur masih kurang kelihatan dgn titik awal
    // 0.55f/0.65f Batch 296. Ini persis skenario yang sudah diantisipasi komentar Batch
    // 296/298 ("kalau nyaris tak kelihatan → alpha masih ketinggian, turunkan lagi") — BUKAN
    // bug rendering (device sudah tier terbaik, `hazeSource`/`hazeEffect` sudah dikonfirmasi
    // compile+jalan sejak CI Batch 296), murni parameter tint yang masih terlalu pekat
    // menutupi blur asli di bawahnya. Diturunkan lagi 0.55f→0.38f (gelap) / 0.65f→0.48f
    // (terang) — langkah lebih besar dari turun Batch 296→298 krn feedback "masih kurang"
    // datang SETELAH satu putaran tuning, bukan dari titik awal 0.92/0.96 lawas. `blurRadius`
    // (32dp, Batch 298) TIDAK ikut dinaikkan batch ini — lever yg diidentifikasi user & kode
    // ini utk masalah "blur ketutup" adalah tint, bukan radius; radius sudah didokumentasikan
    // dekat batas nyaman performa (§ komentar `blurRadius` param di atas). Gap dark/light
    // (0.10) dipertahankan sama seperti semua iterasi sebelumnya (mode terang butuh tint
    // sedikit lebih pekat drpd gelap utk kontras teks yg setara).
    // Batch 311 — DIBALIK ARAH, screenshot user "Kontrol Lanjutan" (ModalBottomSheet):
    // background NowPlayingScreen (coachmark "Geser di kiri/kanan piringan buat atur
    // kecerahan & volume HP...") tembus HAMPIR PENUH di belakang sheet, tumpang-tindih sama
    // teks sheet sendiri — bukan "blur ketutup tint" (arah yang selalu diasumsikan tiap
    // iterasi 296-299 di atas), tapi kebalikannya: 0 blur sama sekali yang kelihatan.
    // Root cause: `ModalBottomSheet`/`Dialog` render di Android Window terpisah dari
    // `hazeSource` (Box NavHost, `MainActivity.kt`) — capture RenderNode Haze tidak bisa
    // sample lintas-window, jadi `hazeEffect` di titik ini diam-diam no-op utk SEMUA bottom
    // sheet/dialog (12+ call site `frostedGlass()` yg lewat sini), sisa cuma tint
    // 0.38f/0.48f itu sendiri tanpa blur di baliknya — jauh terlalu tipis buat berdiri
    // sendiri, persis penyebab "berantakan/tidak professional" yg dilaporkan user.
    // Fix: naikkan tint balik dekat opaque (0.85f/0.90f, BUKAN full 1f ala Skeu — masih
    // sisakan sedikit karakter glass utk elemen DALAM window yg sama spt MiniPlayerBar/card
    // Home-Library/panel NowPlaying, yg capture-nya kemungkinan tetap sah krn 1 window sama
    // dgn `hazeSource`-nya). radius/edgeBrush/hazeEffect call TIDAK disentuh (bukan akar
    // masalah, lihat root cause di atas) — murni 1 parameter tint yg jadi fallback aman
    // terlepas blur cross-window itu jalan atau tidak. Investigasi wiring Haze lintas-window
    // yang sesungguhnya (mis. pindah scrim/blur ke layer yg sama) BELUM dikerjakan (di luar
    // scope 1-parameter fix ini, risiko lebih tinggi ke MainActivity.kt yg diproteksi) — jadi
    // dicatat sebagai item lanjutan, bukan ditutup permanen di sini.
    val liquidGlassAlpha = if (isDark) 0.85f else 0.90f
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
        // Batch 310 — Aurora rim-glow per-panel, permintaan user langsung ("lanjut wiring
        // rim-glow kesemua area") menutup item yang sejak Batch 306 dicatat eksplisit "ditunda,
        // BUKAN dibatalkan". Own branch, bukan jatuh ke `else` di bawah — pola sama alasan
        // isLiquidGlass di atas: `else` cuma benar mendeteksi "Apple light" (perbandingan literal
        // ke AppleLightBackground), Aurora (dark-locked permanen sejak Batch 306, `colorsFor()`
        // sengaja mengabaikan `isDark` — sama seperti CalmRetro) akan diam-diam kebagian rim flat
        // netral `onSurface` alih-alih warna khas identitasnya sendiri kalau tidak dipisah.
        //
        // Sengaja TIDAK dibedakan `isDark` (beda dari isTactile/isLiquidGlass di atas) — identitas
        // ini cuma punya 1 mode terkunci gelap, cabang isDark di sini justru berisiko salah pilih
        // kalau toggle sistem user kebetulan "terang" walau skema warna yang dipakai tetap
        // dipaksa gelap (alasan yang sama persis kenapa CalmRetro juga tidak dibedakan isDark).
        //
        // `frostedGlass()` adalah SATU titik shared yang dilalui SEMUA panel glass app-wide
        // (MiniPlayerBar, tiap bottom sheet, card Home/Library, panel NowPlaying — grep 12+ call
        // site, precedent Batch 281) — jadi 1 branch di sini otomatis "wiring rim-glow kesemua
        // area" tanpa perlu menyentuh 1-per-1 file screen, pola arsitektur identik cara Liquid
        // Glass dapat edge-glow terpusat Batch 281.
        //
        // Warna: 4-stop linear gradient lintas SELURUH spektrum Aurora sendiri (Green->Teal->
        // Violet->Magenta, urutan hue sama persis `auroraGlow()`) — BUKAN 2-stop flat highlight
        // ->fade ala Tactile/Liquid Glass, supaya rim ini kebaca sebagai "irisan aurora" bukan
        // cuma glow generik bertopeng warna tema. Alpha menurun tiap stop (pola sama persis
        // `auroraGlow()`'s multiplier 1.0x/0.85x/0.6x dari `AuroraGlowAlpha`) + 1 falloff
        // tambahan 0.35x utk stop ke-4 — 0 token warna/alpha baru ditambah ke Color.kt, murni
        // reuse AuroraGreen/Teal/Violet/Magenta + AuroraGlowAlpha yang sudah ada sejak Batch 306.
        // SENGAJA statis (bukan animated ala `auroraGlow()`) — `frostedGlass()` dipanggil 12+
        // call site sekaligus, 12+ `rememberInfiniteTransition` independen serentak adalah biaya
        // performa baru yang belum pernah diverifikasi device (beda dari `auroraGlow()` yang cuma
        // 1 instance di root Surface) — titik awal paling aman, kandidat animasi kalau user minta
        // lanjut nanti setelah statis ini terverifikasi visual dulu.
        isAurora -> Brush.linearGradient(
            colors = listOf(
                AuroraGreen.copy(alpha = AuroraGlowAlpha),
                AuroraTeal.copy(alpha = AuroraGlowAlpha * 0.85f),
                AuroraViolet.copy(alpha = AuroraGlowAlpha * 0.6f),
                AuroraMagenta.copy(alpha = AuroraGlowAlpha * 0.35f)
            )
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
