package com.rudi.audioplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

// Batch 61 — REARSITEKTUR: identitas tema (Apple/Tactile/Skeuomorphism) dan mode
// terang/gelap dulunya digabung jadi satu enum AppTheme (SYSTEM/LIGHT/DARK/TACTILE/
// SKEU_DARK_LITE sejajar) — akibatnya Tactile & Skeu "terkunci" gelap permanen,
// tidak pernah benar-benar otonom dari satu mode. User minta identitas & mode
// dicabut & dipisah total, dikendalikan independen dari 1 toggle mode yang sama
// untuk SEMUA identitas. Sekarang 2 enum terpisah:
//  - ThemeIdentity: "wajah" tema (warna/tipografi/shape/bevel) — 3 pilihan.
//  - ThemeMode: terang/gelap/ikuti-sistem — berlaku sama utk ketiga identitas.
// Setiap identitas kustom (Tactile, Skeuomorphism) sekarang wajib py varian
// LIGHT & DARK sendiri (lihat Color.kt) supaya "keluar maksimal" di kedua mode,
// bukan cuma versi gelapnya yang dipoles.
// Batch 79 — SKEU_DARK_LITE's user-facing identity upgraded "Skeuomorphism" -> "Neumorphism"
// atas instruksi eksplisit user (aksen Titanium dominan + sedikit sentuhan Zamrud + depth ultra
// realistic — lihat TactileDepth.kt skeuEmboss() utk mekanisme baru). storageKey "skeu_dark_lite"
// SENGAJA TIDAK diganti (preferensi tema tersimpan milik user yang sudah pernah pilih identitas
// ini tetap valid/tidak ter-reset) — pola blast-radius-terkendali yang sama dgn kenapa nama
// fungsi skeuEmboss()/token Skeu* di Color.kt juga tidak diganti nama, cuma isi/mekanismenya.
enum class ThemeIdentity(val storageKey: String, val displayName: String, val description: String) {
    APPLE("apple", "Apple", "Tampilan bersih khas iOS, mengikuti mode terang/gelap yang dipilih"),
    TACTILE("tactile_lite", "Tactile", "Kaca premium dengan sentuhan Midnight Blue tipis dan kontrol taktil — kini otonom di mode terang maupun gelap"),
    SKEU_DARK_LITE("skeu_dark_lite", "Neumorphism", "Panel lembut menyatu dgn kanvas, dual soft-shadow ultra realistic, aksen Titanium dominan dgn sentuhan Zamrud — otonom di mode terang maupun gelap"),
    CALM_RETRO("calm_retro", "Calm Retro", "Lo-Fi Sci-Fi teduh, aksen Muted Sage — selalu gelap, tidak mengikuti toggle Mode"),
    // Batch 279/280 — ROADMAP_LIQUID_GLASS_REDESIGN.md, §3 dikonfirmasi user: TAMBAH sebagai
    // opsi ke-5 (bukan ganti/konsolidasi 4 di atas), Opsi B (shape+typography+palet statis,
    // tanpa blur asli). Otonom di kedua mode seperti Apple/Tactile/Skeu.
    LIQUID_GLASS("liquid_glass", "Liquid Glass", "Radius besar/pill minimalis ala CONVX, tipografi lebih ringan, palet violet-glass sejuk — otonom di mode terang maupun gelap"),
    // Batch 306/307 — tema ke-6, 100% ide orisinal (bukan reuse mekanisme identitas manapun di
    // atas maupun nama gerakan desain yang sudah ada), dikonfirmasi user via ask_user_input_v0
    // sebelum eksekusi: terkunci gelap permanen (pola sama CALM_RETRO), cakupan efek ambient
    // background saja utk saat ini (rim-glow per-panel ditunda). Mekanisme: `auroraGlow()`
    // (TactileDepth.kt, Batch 306) — warna gradien mengalir pelan, bukan shadow/blur/artefak
    // retro seperti 5 identitas lain.
    AURORA("aurora", "Aurora", "Cahaya aurora borealis mengalir pelan di ambient, aksen hijau-teal-ungu-magenta — selalu gelap, tidak mengikuti toggle Mode");

    companion object {
        fun fromStorageKey(key: String?): ThemeIdentity = entries.find { it.storageKey == key } ?: APPLE
    }
}

enum class ThemeMode(val storageKey: String, val displayName: String) {
    SYSTEM("system", "Ikuti Sistem"),
    LIGHT("light", "Terang"),
    DARK("dark", "Gelap");

    companion object {
        fun fromStorageKey(key: String?): ThemeMode = entries.find { it.storageKey == key } ?: SYSTEM
    }
}

// Batch 61 — dibaca oleh tactileEmboss()/skeuEmboss() (TactileDepth.kt) dan
// frostedGlass() (BlurUtils.kt) supaya keduanya bisa memilih token light/dark
// sendiri TANPA butuh param baru di tiap call site (puluhan titik panggil di
// seluruh app) — cukup 1 CompositionLocal yang di-provide sekali di root
// AudioPlayerTheme(), persis pola LocalContentColor bawaan Compose sendiri.
val LocalIsDarkTheme = staticCompositionLocalOf { true }

// Batch 295 — LIQUID_GLASS_BLUR_ENGINE_DESIGN.md §3a, fase 5 langkah 1 ("fondasi plumbing").
// Sama pola persis LocalIsDarkTheme di atas: 1 HazeState dipegang 1 titik (AppNavHost,
// MainActivity.kt) via rememberHazeState(), diteruskan lewat CompositionLocal ini supaya
// layar/sheet di dalam NavHost (20+ file) tidak perlu terima parameter baru satu-satu.
// Default `HazeState()` di sini HANYA fallback preview/test — nilai sungguhan SELALU datang
// dari provider di AppNavHost, sama seperti LocalIsDarkTheme's default `true` bukan nilai yang
// benar2 dipakai runtime.
val LocalHazeState = staticCompositionLocalOf { HazeState() }

private val AppleDarkColors = darkColorScheme(
    primary = AppleAccent,
    onPrimary = Color.White,
    secondary = AppleDarkSecondaryText,
    onSecondary = AppleDarkBackground,
    tertiary = AppleDarkSuccess,
    onTertiary = Color.Black,
    background = AppleDarkBackground,
    onBackground = AppleDarkText,
    surface = AppleDarkSurface,
    onSurface = AppleDarkText,
    surfaceVariant = AppleDarkSurfaceVariant,
    onSurfaceVariant = AppleDarkSecondaryText,
    outline = AppleDarkSurfaceVariant,
    // Unset previously — M3's darkColorScheme() factory silently fills any omitted role with
    // its own purple baseline default, so every elevated Surface/Card/NavigationBar using
    // automatic tonal elevation was tinting toward baseline purple instead of this app's own
    // accent. Explicit surfaceTint fixes that leak for every M3 elevation overlay app-wide.
    surfaceTint = AppleAccent,
    error = Color(0xFFFF453A)
)

private val AppleLightColors = lightColorScheme(
    primary = AppleAccent,
    onPrimary = Color.White,
    secondary = AppleLightSecondaryText,
    onSecondary = AppleLightBackground,
    tertiary = AppleLightSuccess,
    onTertiary = Color.White,
    background = AppleLightBackground,
    onBackground = AppleLightText,
    surface = AppleLightSurface,
    onSurface = AppleLightText,
    surfaceVariant = AppleLightSurfaceVariant,
    onSurfaceVariant = AppleLightSecondaryText,
    outline = AppleLightSurfaceVariant,
    surfaceTint = AppleAccent,
    error = Color(0xFFFF3B30)
)

// Batch 53: still darkColorScheme() for the dark variant — compose-amoled-hybrid-glass-final.md
// §24's palette is this identity's DARK expression. Batch 61: renamed TactileColors ->
// TactileDarkColors (was implicitly "the only Tactile colors" pre-Batch-61; now explicit since
// TactileLightColors exists as a sibling). onPrimary picked by the same luminance rule
// MiniPlayerBar.kt uses elsewhere (>0.55 luminance -> black text): TactileAccent (0xFF6670FF)
// simple luma ≈0.49, below the threshold, so onPrimary stays Color.White in BOTH variants (the
// accent color itself is shared across light/dark, only the surfaces around it flip).
private val TactileDarkColors = darkColorScheme(
    primary = TactileAccent,
    onPrimary = Color.White,
    secondary = TactileSecondaryText,
    onSecondary = TactileBackground,
    tertiary = TactileSuccess,
    onTertiary = Color.Black,
    background = TactileBackground,
    onBackground = TactileText,
    surface = TactileSurface,
    onSurface = TactileText,
    surfaceVariant = TactileSurfaceVariant,
    onSurfaceVariant = TactileSecondaryText,
    outline = TactileSurfaceVariant,
    surfaceTint = TactileAccent,
    error = TactileError
)

// Batch 61 — Tactile's own LIGHT expression (Color.kt "TACTILE — LIGHT VARIANT"). Same accent/
// success/error tokens as the dark variant (shared across both, see comment above); only the
// background/surface/text roles flip to the light token set.
private val TactileLightColors = lightColorScheme(
    primary = TactileAccent,
    onPrimary = Color.White,
    secondary = TactileLightSecondaryText,
    onSecondary = TactileLightBackground,
    tertiary = TactileSuccess,
    onTertiary = Color.Black,
    background = TactileLightBackground,
    onBackground = TactileLightText,
    surface = TactileLightSurface,
    onSurface = TactileLightText,
    surfaceVariant = TactileLightSurfaceVariant,
    onSurfaceVariant = TactileLightSecondaryText,
    outline = TactileLightSurfaceVariant,
    surfaceTint = TactileAccent,
    error = TactileError
)

// Batch 57 — Skeuomorphism's own color role mapping (DARK expression). Batch 61: renamed
// SkeuDarkColors kept as-is (name already had "Dark" — now explicitly paired with SkeuLightColors
// below instead of being the only variant). onPrimary picked by the same luminance rule used
// elsewhere (>0.55 luminance -> black text): SkeuAccent (0xFFB6BAC0, Titanium+Silver metalik
// sejak Batch 63 — dulu tembaga 0xFFCB8B4B) simple luma ≈0.73, tetap jauh di atas
// the threshold, so onPrimary is Color.Black in BOTH variants (accent shared across light/dark).
private val SkeuDarkColors = darkColorScheme(
    primary = SkeuAccent,
    onPrimary = Color.Black,
    secondary = SkeuDarkSecondaryText,
    onSecondary = SkeuDarkBackground,
    tertiary = SkeuDarkSuccess,
    onTertiary = Color.Black,
    background = SkeuDarkBackground,
    onBackground = SkeuDarkText,
    surface = SkeuDarkSurface,
    onSurface = SkeuDarkText,
    surfaceVariant = SkeuDarkSurfaceVariant,
    onSurfaceVariant = SkeuDarkSecondaryText,
    outline = SkeuDarkSurfaceVariant,
    surfaceTint = SkeuAccent,
    error = SkeuDarkError
)

// Batch 61 — Skeuomorphism's own LIGHT expression (Color.kt "SKEUOMORPHISM — LIGHT VARIANT").
private val SkeuLightColors = lightColorScheme(
    primary = SkeuAccent,
    onPrimary = Color.Black,
    secondary = SkeuLightSecondaryText,
    onSecondary = SkeuLightBackground,
    tertiary = SkeuDarkSuccess,
    onTertiary = Color.Black,
    background = SkeuLightBackground,
    onBackground = SkeuLightText,
    surface = SkeuLightSurface,
    onSurface = SkeuLightText,
    surfaceVariant = SkeuLightSurfaceVariant,
    onSurfaceVariant = SkeuLightSecondaryText,
    outline = SkeuLightSurfaceVariant,
    surfaceTint = SkeuAccent,
    error = SkeuDarkError
)

// Calm Retro — 1 colorScheme saja (bukan pasangan Dark/Light seperti Tactile/Skeu), karena
// identitas ini sengaja terkunci gelap permanen (lihat colorsFor() — param isDark diabaikan
// untuk identity ini). onPrimary Color.Black: luma CalmRetroAccent (#7FA99B) ≈0.61, di atas
// ambang 0.55 yang sudah dipakai identitas lain di file ini.
private val CalmRetroColors = darkColorScheme(
    primary = CalmRetroAccent,
    onPrimary = Color.Black,
    secondary = CalmRetroSecondaryText,
    onSecondary = CalmRetroBackground,
    // Batch 130 — pemurnian: dulu reuse SkeuDarkSuccess (token identitas LAIN, mengaburkan
    // batas visual antar-identitas). Sekarang 100% derivasi dari palet Calm Retro sendiri —
    // reuse CalmRetroAccent (Muted Sage sudah cukup "hijau positif" tanpa perlu warna asing).
    tertiary = CalmRetroAccent,
    onTertiary = Color.Black,
    background = CalmRetroBackground,
    onBackground = CalmRetroText,
    surface = CalmRetroSurface,
    onSurface = CalmRetroText,
    surfaceVariant = CalmRetroBorder,
    onSurfaceVariant = CalmRetroSecondaryText,
    outline = CalmRetroBorder,
    surfaceTint = CalmRetroAccent,
    // Batch 130 — pemurnian: dulu reuse SkeuDarkError. Sekarang CalmRetroAberrationLeft
    // (Dusty Rose) — token milik Calm Retro sendiri (sudah dipakai di calmAberration()),
    // dipakai ulang di peran semantik error, bukan warna asing dari identitas lain.
    error = CalmRetroAberrationLeft
)

// Batch 279/280 — Liquid Glass, otonom di kedua mode (pola sama Apple/Tactile/Skeu, bukan
// terkunci gelap spt Calm Retro). error pakai token M3 iOS-merah standar (sama seperti Apple)
// — belum ada token error khusus Liquid Glass sendiri, palet fase 2 fokus ke background/surface/
// text/accent/success dulu sesuai scope roadmap ("shape+typography+palet", bukan semantic-error
// baru yang tidak disebut riset CONVX sama sekali).
private val LiquidGlassDarkColors = darkColorScheme(
    primary = LiquidGlassAccent,
    onPrimary = Color.White,
    secondary = LiquidGlassDarkSecondaryText,
    onSecondary = LiquidGlassDarkBackground,
    tertiary = LiquidGlassDarkSuccess,
    onTertiary = Color.Black,
    background = LiquidGlassDarkBackground,
    onBackground = LiquidGlassDarkText,
    surface = LiquidGlassDarkSurface,
    onSurface = LiquidGlassDarkText,
    surfaceVariant = LiquidGlassDarkSurfaceVariant,
    onSurfaceVariant = LiquidGlassDarkSecondaryText,
    outline = LiquidGlassDarkSurfaceVariant,
    surfaceTint = LiquidGlassAccent,
    error = Color(0xFFFF453A)
)

private val LiquidGlassLightColors = lightColorScheme(
    primary = LiquidGlassAccent,
    onPrimary = Color.White,
    secondary = LiquidGlassLightSecondaryText,
    onSecondary = LiquidGlassLightBackground,
    tertiary = LiquidGlassLightSuccess,
    onTertiary = Color.White,
    background = LiquidGlassLightBackground,
    onBackground = LiquidGlassLightText,
    surface = LiquidGlassLightSurface,
    onSurface = LiquidGlassLightText,
    surfaceVariant = LiquidGlassLightSurfaceVariant,
    onSurfaceVariant = LiquidGlassLightSecondaryText,
    outline = LiquidGlassLightSurfaceVariant,
    surfaceTint = LiquidGlassAccent,
    error = Color(0xFFFF3B30)
)

// Batch 307 — Fase 2/N tema Aurora. 1 colorScheme saja (bukan pasangan Dark/Light), sama alasan
// CalmRetroColors di atas: identitas ini terkunci gelap permanen (lihat colorsFor() — param
// isDark diabaikan utk identity ini, pola sama persis CALM_RETRO).
// onPrimary Color.Black: luma AuroraAccent (#3DE8A0) ≈0.75, jauh di atas ambang 0.55 yang
// dipakai identitas lain di file ini. tertiary/onTertiary sama: luma AuroraTeal (#2BC9C9) ≈0.66.
// error = AuroraMagenta (BUKAN token asing/generik ala LiquidGlassDarkColors di atas) — pelajaran
// Batch 130 eksplisit: "100% derivasi dari palet [identitas] sendiri" drpd reuse token identitas
// lain atau warna generik tak-berhubungan; magenta-pink sudah masuk keluarga hue aurora sendiri
// (Color.kt) dan cukup dekat semantik "perhatian/warning" spt CalmRetroAberrationLeft (Dusty
// Rose) dipakai utk peran yang sama di sana.
private val AuroraColors = darkColorScheme(
    primary = AuroraAccent,
    onPrimary = Color.Black,
    secondary = AuroraSecondaryText,
    onSecondary = AuroraBackground,
    tertiary = AuroraTeal,
    onTertiary = Color.Black,
    background = AuroraBackground,
    onBackground = AuroraText,
    surface = AuroraSurface,
    onSurface = AuroraText,
    surfaceVariant = AuroraSurfaceVariant,
    onSurfaceVariant = AuroraSecondaryText,
    outline = AuroraSurfaceVariant,
    surfaceTint = AuroraAccent,
    error = AuroraMagenta
)

// A single, consistent "continuous curve" language across the whole app — Compose's Shapes
// API only supports true rounded rectangles (Apple's real squircle/superellipse corners
// aren't natively expressible), so generous rounding is the closest honest approximation.
val AppleShapes = Shapes(
    small = RoundedCornerShape(Radius.ml),
    medium = RoundedCornerShape(Radius.xxl),
    large = RoundedCornerShape(Radius.hero)
)

// Tactile's shape language — moderate, consistent rounding (not Apple's generous curve, not
// sharp/boxy like the old Matte identity) since the tactile spec's realism comes from the
// bevel/gradient/shadow treatment (tactileEmboss(), TactileDepth.kt), not from the shape itself.
// Shared identically by both light & dark expressions — shape is an identity trait, not a mode
// trait (Batch 61 principle applies here too).
val TactileShapes = Shapes(
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg)
)

// Batch 57 — Skeuomorphism's shape language: one notch more rounded than Tactile at every step
// (md/lg/xxl vs Tactile's sm/md/lg), reading as soft physical buttons/panels. Shared by both
// light & dark expressions (same Batch 61 principle as TactileShapes above).
val SkeuDarkShapes = Shapes(
    small = RoundedCornerShape(Radius.md),
    medium = RoundedCornerShape(Radius.lg),
    large = RoundedCornerShape(Radius.xxl)
)

// Batch 130 — pemurnian visual: dulu Calm Retro jatuh ke branch `else` di AudioPlayerTheme()
// (warisan AppleShapes, generous superellipse-like curve). Sekarang shape sendiri, sudut PALING
// mepet dari 4 identitas (Apple generous > Skeu > Tactile > Calm Retro di sini) — selaras bacaan
// "Lo-Fi Sci-Fi teduh"/minimalis spec (garis lebih tenang/tegas, bukan bounce lembut ala Apple),
// juga jadi pembeda visual paling langsung yang mudah dikenali di picker Settings.
val CalmRetroShapes = Shapes(
    small = RoundedCornerShape(Radius.xs),
    medium = RoundedCornerShape(Radius.sm),
    large = RoundedCornerShape(Radius.md)
)

// Batch 279/280 — Liquid Glass shape language: paling generous dari SEMUA 5 identitas
// (termasuk lebih besar dari Apple, yang sebelumnya paling generous) — pakai token `Radius.
// liquidLg` fase 1 (Batch 279) di slot `large`. `Radius.liquidPill` (999dp, stadium PENUH)
// SENGAJA TIDAK dipasang di sini — `Shapes.large` M3 dipakai generik di banyak komponen
// (Card/Sheet/dialog besar berbagai tinggi/lebar), radius sebesar 999dp di situ akan clamp jadi
// bentuk lensa/blob di surface tinggi, bukan "kartu bersudut besar" yang dimaksud. `liquidPill`
// disimpan sebagai token, dipakai LANGSUNG di call site spesifik yang benar2 pill (tombol/chip)
// di fase 3 nanti — bukan lewat `Shapes` generik ini.
val LiquidGlassShapes = Shapes(
    small = RoundedCornerShape(Radius.xl),
    medium = RoundedCornerShape(Radius.xxxl),
    large = RoundedCornerShape(Radius.liquidLg)
)

// Batch 309 — Aurora fase 4/N, permintaan user eksplisit "sempurnakan shape murni-nya" (lanjutan
// langsung dari Batch 308's "belum diminta user" catatan soal shape). SATU-SATUNYA dari 6
// identitas yang TIDAK pakai `RoundedCornerShape(Radius.x)` seragam (1 argumen, 4 sudut sama) —
// mekanisme baru: `RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)` ASIMETRIS per
// sudut, ditarik LANGSUNG dari arah `Brush.linearGradient()` di `auroraGlow()` (TactileDepth.kt):
// tanpa parameter `start`/`end` eksplisit, Compose menggambar linear gradient itu diagonal dari
// pojok kiri-atas ke kanan-bawah kanvas (topStart -> bottomEnd) — jadi 2 sudut YANG SEARAH
// diagonal itu (`topStart` & `bottomEnd`) dapat radius LEBIH BESAR (menekankan arah alirnya),
// sementara 2 sudut yang TEGAK LURUS arah alir (`topEnd` & `bottomStart`) dapat radius LEBIH
// KECIL — setiap panel/card/sheet berbentuk `Shapes` di app ini ikut "condong" ke arah yang sama
// persis dengan ambient wash yang mengalir di baliknya, bukan sekadar rounded rect generik.
// SENGAJA TIDAK ikut lomba jadi radius PALING besar dari 6 identitas (LiquidGlass di atas tetap
// pemegang radius seragam terbesar, 34dp/`liquidLg`) — nilai "besar" Aurora di sudut diagonalnya
// SAMA PERSIS dgn `liquidLg`/`xxxl`/`xl` LiquidGlass di tiap tier (0 token baru ditambah ke
// `Radius` di `Spacing.kt`), keunikannya murni dari ASIMETRI-nya, bukan dari mengejar rekor angka
// baru — konsisten dgn semangat Batch 306 ("100% ide sendiri, tanpa contek gaya apapun") yang
// sama diterapkan ke `AuroraColors`/`AuroraTypography`, sekarang menuntaskan sisi shape.
// CATATAN RISIKO: ini pola shape PERTAMA di seluruh project yang non-seragam per sudut — 5
// identitas lain semuanya 4-sudut sama. Type-compatible penuh dgn M3 `Shapes`/`CornerBasedShape`
// (0 API baru, 0 call site di luar `Theme.kt` perlu berubah, sama seperti giliran Aurora
// sebelumnya), TAPI belum pernah diverifikasi visual di komponen nyata (Card/Sheet/dialog) — WAJIB
// dicek device sungguhan lebih hati-hati dari batch shape sebelumnya, krn ini genuinely mekanisme
// baru, bukan cuma tuning angka radius seragam yang sudah terbukti aman di 5 identitas lain.
val AuroraShapes = Shapes(
    small = RoundedCornerShape(
        topStart = Radius.xl,      // 18dp — searah gradient masuk (kiri-atas)
        topEnd = Radius.sm,        // 10dp — tegak lurus arah alir
        bottomEnd = Radius.xl,     // 18dp — searah gradient keluar (kanan-bawah)
        bottomStart = Radius.sm    // 10dp — tegak lurus arah alir
    ),
    medium = RoundedCornerShape(
        topStart = Radius.xxxl,    // 24dp
        topEnd = Radius.md,        // 12dp
        bottomEnd = Radius.xxxl,   // 24dp
        bottomStart = Radius.md    // 12dp
    ),
    large = RoundedCornerShape(
        topStart = Radius.liquidLg, // 34dp — sama persis puncak radius LiquidGlass, TIDAK melebihi
        topEnd = Radius.lg,         // 16dp
        bottomEnd = Radius.liquidLg,// 34dp
        bottomStart = Radius.lg     // 16dp
    )
)

// Batch 61 — mode resolution is now completely identity-agnostic (used to take an AppTheme and
// hardcode TACTILE/SKEU_DARK_LITE to always-true). Every identity honors SYSTEM/LIGHT/DARK the
// same way; an identity's own light/dark visual DIFFERENCE lives entirely in colorsFor() below.
@Composable
fun resolveIsDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

// Batch 54/57 dedup helpers — Batch 61: comparison switched from `background` (which now differs
// between an identity's own light/dark expression, so it could never match both) to `primary`
// (the one role every identity's light/dark pair deliberately SHARES — see TactileDarkColors/
// TactileLightColors & SkeuDarkColors/SkeuLightColors comments above), so these helpers now
// correctly report true for their identity regardless of which mode is active.
@Composable
fun isTactileTheme(): Boolean = MaterialTheme.colorScheme.primary == TactileAccent

@Composable
fun isSkeuTheme(): Boolean = MaterialTheme.colorScheme.primary == SkeuAccent

@Composable
fun isCalmRetroTheme(): Boolean = MaterialTheme.colorScheme.primary == CalmRetroAccent

// Batch 281 — Liquid Glass fase 3 dimulai: helper ke-4 pola identik 3 di atas, dibutuhkan
// komponen (BlurUtils.kt duluan; call site UI lain menyusul) buat cabang khusus identitas ini,
// sama seperti Tactile/Skeu/Calm Retro dulu.
@Composable
fun isLiquidGlassTheme(): Boolean = MaterialTheme.colorScheme.primary == LiquidGlassAccent

// Batch 308 — Aurora fase 3/N, helper ke-5 pola identik 4 di atas (Tactile/Skeu/CalmRetro/
// LiquidGlass). Dipakai pertama kali di `MainActivity.kt` (wiring `auroraGlow()` ke root
// Surface) — call site UI lain yang butuh cabang khusus identitas ini menyusul nanti kalau ada.
@Composable
fun isAuroraTheme(): Boolean = MaterialTheme.colorScheme.primary == AuroraAccent

fun colorsFor(identity: ThemeIdentity, isDark: Boolean) = when (identity) {
    ThemeIdentity.TACTILE -> if (isDark) TactileDarkColors else TactileLightColors
    ThemeIdentity.SKEU_DARK_LITE -> if (isDark) SkeuDarkColors else SkeuLightColors
    ThemeIdentity.APPLE -> if (isDark) AppleDarkColors else AppleLightColors
    // isDark sengaja diabaikan — Calm Retro terkunci gelap permanen (instruksi eksplisit user),
    // beda dari Tactile/Skeu yang otonom di kedua mode.
    ThemeIdentity.CALM_RETRO -> CalmRetroColors
    // Batch 279/280 — Liquid Glass otonom di kedua mode (pola sama Apple/Tactile/Skeu).
    ThemeIdentity.LIQUID_GLASS -> if (isDark) LiquidGlassDarkColors else LiquidGlassLightColors
    // Batch 307 — isDark sengaja diabaikan, sama alasan CALM_RETRO di atas: Aurora terkunci
    // gelap permanen (dikonfirmasi user sebelum Batch 306).
    ThemeIdentity.AURORA -> AuroraColors
}

@Composable
fun AudioPlayerTheme(
    identity: ThemeIdentity = ThemeIdentity.APPLE,
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = resolveIsDark(mode)
    // Batch 61 — provides the mode signal to every descendant (tactileEmboss()/skeuEmboss()/
    // frostedGlass()) so their light/dark token branching stays centralized instead of each call
    // site re-deriving it.
    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = colorsFor(identity, isDark),
            // Batch 57 — Skeu awalnya reuse AppleTypography (no separate type-scale spec supplied
            // for this theme; skeuomorphic identity here is carried by color/shape/bevel, not
            // custom type). Batch 279/280 — LIQUID_GLASS dapat typography sendiri
            // (LiquidGlassTypography, fase 1), bukan reuse Apple — beda dari Skeu, tipografi
            // justru salah satu dari 2 pembeda utama identitas ini (§3b Opsi B: "shape+typography").
            // Batch 302 — CALM_RETRO menyusul dapat typography sendiri (CalmRetroTypography),
            // membalik keputusan reuse-Apple Batch 130 atas instruksi eksplisit user
            // ("perkuat typography khusus tema Calm Retro, murni 100%") — menuntaskan pemurnian
            // identitas itu ke ranah typography (color/shape sudah murni sejak Batch 130).
            // Batch 305 — SKEU_DARK_LITE menyusul terakhir dapat typography sendiri
            // (SkeuTypography) atas instruksi eksplisit user ("sempurnakan typography Neumorphism
            // 100% murni, tuntas!!") — menutup reuse-Apple TERAKHIR dari 5 identitas saat itu;
            // sesudah batch itu ke-5 identitas semuanya punya Typography() murni sendiri.
            // Batch 309 — AURORA menyusul dapat shape sendiri (AuroraShapes) atas instruksi
            // eksplisit user ("sempurnakan shape murni-nya") — menutup 3/3 pemurnian identitas
            // ini (color Batch 307, typography Batch 308, shape batch ini). `else -> AppleShapes`
            // di bawah sekarang murni APPLE saja (satu-satunya identitas tanpa cabang eksplisit).
            typography = when (identity) {
                ThemeIdentity.TACTILE -> TactileTypography
                ThemeIdentity.SKEU_DARK_LITE -> SkeuTypography
                ThemeIdentity.LIQUID_GLASS -> LiquidGlassTypography
                ThemeIdentity.CALM_RETRO -> CalmRetroTypography
                ThemeIdentity.AURORA -> AuroraTypography
                else -> AppleTypography
            },
            shapes = when (identity) {
                ThemeIdentity.TACTILE -> TactileShapes
                ThemeIdentity.SKEU_DARK_LITE -> SkeuDarkShapes
                ThemeIdentity.CALM_RETRO -> CalmRetroShapes
                ThemeIdentity.LIQUID_GLASS -> LiquidGlassShapes
                ThemeIdentity.AURORA -> AuroraShapes
                else -> AppleShapes
            },
            content = content
        )
    }
}
