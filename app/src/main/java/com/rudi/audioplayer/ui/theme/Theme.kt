package com.rudi.audioplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
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
// Batch 83 — APPLE's user-facing identity renamed "Apple" -> "Facet" atas instruksi eksplisit
// user: "UI/UX yang premium+expensive 'otonom', gak menjiplak apple music atau apps apapun" ->
// klarifikasi lanjutan: ganti TOTAL identitas Apple (bukan tema ke-4), tapi LAYOUT/shape saja
// dulu — palet warna (AppleDarkColors/AppleLightColors/AppleAccent) SENGAJA TIDAK disentuh batch
// ini (redesign warna ditunda, instruksi eksplisit user "theme gak usah redesign dulu"). Yang
// diganti: AppleShapes di bawah (generous-rounded "squircle" ala iOS -> chamfered single-corner
// language, lihat komentar AppleShapes) + 3 kontrol yg sebelumnya hardcode CircleShape lepas dari
// sistem shape sama sekali (MiniPlayerBar/NowPlayingScreen play-pause, LockScreen PIN keypad) —
// storageKey "apple" & nama konstanta enum APPLE SENGAJA TIDAK diganti (preferensi tema
// tersimpan milik user yang sudah pernah pilih identitas ini tetap valid), pola blast-radius-
// terkendali yang sama dgn SKEU_DARK_LITE (Batch 79) & fungsi skeuEmboss() (nama tetap, isi ganti).
enum class ThemeIdentity(val storageKey: String, val displayName: String, val description: String) {
    APPLE("apple", "Facet", "Geometri chamfer otonom — satu sudut terpotong tegas di tiap panel, bukan generous-rounded ala iOS"),
    TACTILE("tactile_lite", "Tactile", "Kaca premium dengan sentuhan Midnight Blue tipis dan kontrol taktil — kini otonom di mode terang maupun gelap"),
    SKEU_DARK_LITE("skeu_dark_lite", "Neumorphism", "Panel lembut menyatu dgn kanvas, dual soft-shadow ultra realistic, aksen Titanium dominan dgn sentuhan Zamrud — otonom di mode terang maupun gelap");

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

// Batch 83 — was generous RoundedCornerShape ("closest honest approximation" of Apple's own
// squircle, per the removed comment here) — literally an admission this shape language existed
// to resemble Apple Music's own rounded-corner cards/sheets. Replaced with a single-corner
// CHAMFER (CutCornerShape, a native Compose Foundation shape — a diagonal cut, not a curve) on
// topEnd only, other 3 corners left sharp (0dp) — reads as a cut/faceted edge (cut-gem, tailored
// notch, a folded-corner card), a motif none of this app's 3 identities nor Apple Music/most
// music apps use (Tactile/Skeu are both uniform-all-corner RoundedCornerShape families — see
// TactileShapes/SkeuDarkShapes below — so this is also now visually distinct from both of THOSE,
// not just from Apple Music). Same dp magnitudes as the old radii (Radius.md/xl/hero) kept for
// scale continuity — footprint size unchanged, only the corner GEOMETRY changed (cut vs round,
// one corner vs four) — deliberately "layout only" per user's explicit instruction this batch,
// zero color-token changes anywhere in this file or Color.kt.
val AppleShapes = Shapes(
    small = CutCornerShape(topEnd = Radius.md),
    medium = CutCornerShape(topEnd = Radius.xl),
    large = CutCornerShape(topEnd = Radius.hero)
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

fun colorsFor(identity: ThemeIdentity, isDark: Boolean) = when (identity) {
    ThemeIdentity.TACTILE -> if (isDark) TactileDarkColors else TactileLightColors
    ThemeIdentity.SKEU_DARK_LITE -> if (isDark) SkeuDarkColors else SkeuLightColors
    ThemeIdentity.APPLE -> if (isDark) AppleDarkColors else AppleLightColors
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
            // Batch 57 — Skeu reuses AppleTypography (no separate type-scale spec supplied for this
            // theme; skeuomorphic identity here is carried by color/shape/bevel, not custom type).
            typography = if (identity == ThemeIdentity.TACTILE) TactileTypography else AppleTypography,
            shapes = when (identity) {
                ThemeIdentity.TACTILE -> TactileShapes
                ThemeIdentity.SKEU_DARK_LITE -> SkeuDarkShapes
                else -> AppleShapes
            },
            content = content
        )
    }
}
