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
    // Batch 325 — user KONFIRMASI LANGSUNG dari device sungguhan (setelah Batch 324 menutup
    // ke-7 gap `containerColor`, 17/17 call site `ModalBottomSheet` app-wide): blur SUDAH
    // kelihatan benar, termasuk sheet/dialog cross-window yang dulu 0% (root cause Batch 311).
    // Fallback aman 0.85f/0.90f (Batch 311) sekarang TIDAK relevan lagi — itu murni jaring
    // pengaman selagi root cause "0 blur sama sekali" belum ketemu, bukan nilai tuning yang
    // pernah divalidasi device. Diturunkan BALIK ke nilai tuning device TERAKHIR yang sah
    // (Batch 299, sebelum Batch 311 menaikkannya darurat karena bug tak-terkait):
    // 0.85f→0.38f (gelap) / 0.90f→0.48f (terang). Bukan angka baru/tebakan — reuse murni nilai
    // yang sudah pernah lolos 1 putaran tuning device dulu, kini blocker-nya sudah tuntas.
    // `blurRadius` (32dp, Batch 298) & gap dark/light (0.10) TETAP tidak disentuh, sama alasan
    // seperti sebelumnya (lever yang relevan cuma tint, bukan radius/kontras).
    // Batch 329 — `hazeEffect` DIMATIKAN PERMANEN app-wide (keputusan user: "matikan blur asli
    // sepenuhnya, balik ke tint solid" — opsi paling aman, lihat rasionalisasi penuh di
    // `glassBase` bawah). Nilai 0.38f/0.48f di atas SENGAJA diasumsikan blur asli ADA di
    // baliknya (persis rasionalisasi Batch 296/299/325 di atas) — sekarang blur itu dimatikan,
    // tint SENDIRIAN wajib menanggung penuh keterbacaan tanpa backdrop tersaring di belakangnya.
    // BUKAN angka baru/tebakan: reuse persis fallback "0 blur terlihat, tint sendiri wajib jaga
    // keterbacaan" yang sudah pernah shipped & valid Batch 311-324 (0.85f gelap / 0.90f terang)
    // — skenario itu dulu darurat/sementara (bug cross-window blur belum ketemu), sekarang jadi
    // status permanen dengan kebutuhan visual yang identik: tint tanpa blur di baliknya.
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
        // Batch 327 — user (device sungguhan): "terlalu tipis, hampir tak kasat mata", scope
        // dikonfirmasi cuma rim-glow ini (`auroraGlow()`'s wash 0 dikeluhkan, TIDAK disentuh).
        // Base alpha pindah `AuroraGlowAlpha` (0.34f, dipakai bareng ambient wash) →
        // `AuroraRimGlowAlpha` (0.44f, token BARU khusus rim — lihat rasionalisasi penuh di
        // Color.kt) supaya menaikkan rim tidak ikut menaikkan wash yang sudah pas. Multiplier
        // taper per-stop JUGA dinaikkan (0.85x/0.6x/0.35x → 0.85x/0.65x/0.46x) — floor stop ke-4
        // naik dari alpha efektif 0.119 (0.34×0.35) ke 0.202 (0.44×0.46), ~70% lebih terang di
        // titik paling redup, sementara taper (tiap stop tetap lebih redup dari sebelumnya) tetap
        // dipertahankan supaya rim masih kebaca "memudar", bukan flat solid.
        // Batch 328 — DIREVERT ke statis. User (device sungguhan): musik stuttering/mandek saat
        // diputar + lag/glitch saat swipe sheet "Kontrol Lanjutan". Root cause paling mungkin:
        // asumsi Batch 326 "1 `rememberInfiniteTransition` dibagi lewat CompositionLocal = aman"
        // TERBUKTI KELIRU di device sungguhan — berbagi 1 instance memang mengurangi JUMLAH
        // transition (12+→1), TAPI tidak menghilangkan bahwa phase berubah tiap frame (~16ms)
        // memicu recomposition brush di SEMUA consumer sekaligus, termasuk `MiniPlayerBar`
        // (selalu tervisible SELAMA musik main) — bersaing langsung dgn thread audio/UI pas
        // playback aktif, PLUS sheet "Kontrol Lanjutan" yang juga baca `frostedGlass()` sambil
        // menangani gesture swipe. Sesuai `STABILITY > Speed`: TIDAK ditambal/dioptimasi lebih
        // jauh (mis. `derivedStateOf`/throttle) — direvert PENUH ke statis, konsisten rasionalisasi
        // ASLI Batch 310 yang sempat (keliru) dianggap sudah teratasi Batch 326. `LocalAuroraPhase`
        // (Theme.kt) & phase computation (`AppNavHost`, MainActivity.kt) DIHAPUS BALIK — bukan
        // cuma berhenti dipakai di sini (dead CompositionLocal ditinggal = risiko re-enable
        // ceroboh nanti). Nilai alpha Batch 327 (`AuroraRimGlowAlpha` 0.44f + taper
        // 0.85x/0.65x/0.46x) TETAP dipertahankan statis — itu bukan penyebab regresi (keluhan user
        // soal alpha & soal stutter adalah 2 laporan device terpisah), dan sudah correct
        // rasionalisasinya (level "accent-glow biasa", lihat Batch 327 di atas).
        isAurora -> Brush.linearGradient(
            colors = listOf(
                AuroraGreen.copy(alpha = AuroraRimGlowAlpha),
                AuroraTeal.copy(alpha = AuroraRimGlowAlpha * 0.85f),
                AuroraViolet.copy(alpha = AuroraRimGlowAlpha * 0.65f),
                AuroraMagenta.copy(alpha = AuroraRimGlowAlpha * 0.46f)
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
    // Batch 329 — `hazeEffect` DIMATIKAN PERMANEN app-wide (keputusan eksplisit user: "matikan
    // blur asli sepenuhnya, balik ke tint solid" — opsi paling aman dari 2 opsi yang ditawarkan).
    // Root cause yang mendasari keputusan ini: blur asli baru genuinely aktif di 17/17
    // ModalBottomSheet sejak Batch 324 (sebelumnya no-op cross-window, root cause Batch 311) —
    // sheet "Kontrol Lanjutan" + `MiniPlayerBar` (SELALU tervisible & terus resample tiap frame
    // selama musik main, capture-nya lewat `hazeSource` di window yg sama) adalah persis biaya
    // GPU per-frame yang sudah diperingatkan sejak param `blurRadius` ini pertama ditambah
    // (komentar Batch 298/300 di atas: "blur asli Haze resample tiap frame"). Beda dari Batch
    // 328 (yang cuma revert animasi Aurora) — batch ini mematikan MEKANISME blur asli itu
    // sendiri, bukan cuma 1 dekorasi di atasnya.
    // Companion change (`MainActivity.kt`, Protected/edit parsial): `.hazeSource(state =
    // hazeState)` di Box NavHost DILEPAS juga — kalau cuma `hazeEffect` di sini yang dimatikan
    // tapi `hazeSource` masih terpasang, capture backdrop tiap frame TETAP jalan tanpa 1
    // consumer pun, jadi tetap bayar sebagian besar biaya GPU yang justru ingin dihilangkan
    // batch ini. `hazeState`/`LocalHazeState`/`CompositionLocalProvider` (Theme.kt,
    // MainActivity.kt) SENGAJA TIDAK dibongkar — dibiarkan reuse persis state Batch 295
    // ("murni plumbing, 0 consumer, 0 perubahan visual", sebelum Batch 296 menyambungkannya ke
    // hazeEffect/hazeSource) — precedent yang sudah pernah shipped, jadi 0 risiko baru drpd
    // membongkar CompositionLocalProvider yang membungkus ratusan baris Scaffold (Batch 295's
    // komentar sendiri: "badan blok TIDAK di-reindent, minim-diff").
    // `glassBase` sekarang SELALU `this` (identik ke-4 identitas lain) — tidak ada lagi cabang
    // isLiquidGlass di sini. Parameter `blurRadius` fungsi ini (default 24.dp) BALIK ke status
    // "kept for source compatibility, unused" persis pra-Batch-296 — tidak dihapus dari
    // signature (0 call site di app ini pernah pass eksplisit, grep masih 17/17 tanpa argumen),
    // supaya tidak mengubah kontrak publik fungsi ini tanpa perlu.
    val glassBase = this
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
