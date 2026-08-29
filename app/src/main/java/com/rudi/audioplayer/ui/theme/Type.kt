package com.rudi.audioplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Emulates the SF Pro feel (bold weight, tight tracking, clear size jumps
// between hierarchy levels) using the system sans-serif — SF Pro itself is
// Apple's proprietary font and isn't licensed for use outside Apple platforms,
// so this is a deliberate look-alike, not a bundled copy of the real thing.
val AppleTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 35.6.sp,
        letterSpacing = (-0.4).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 25.5.sp,
        letterSpacing = (-0.2).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.4.sp,
        letterSpacing = (-0.1).sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    )
)

// Tactile (Skeuomorphism-lite) typography — Batch 49. The spec (compose-skeuomorphism-lite.md)
// doesn't prescribe a typeface; sans-serif kept throughout (no separate serif/sans split like
// the old Matte identity) since the tactile identity here comes from the surfaces themselves
// (bevel/gradient/press), not from lettering — titles just go bolder than Apple's for a
// "machined label" read.
val TactileTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 35.6.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 25.5.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.4.sp,
        letterSpacing = (-0.1).sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    )
)

// Batch 279 — ROADMAP_LIQUID_GLASS_REDESIGN.md §5 fase 1, §3b Opsi B ("Liquid Glass LOOK":
// shape+typography murni, TANPA blur asli/minSdk bump). CONVX-terinspirasi "tipografi lebih
// ringan" — dikontraskan sengaja terhadap AppleTypography di atas: weight 1 tingkat lebih
// RINGAN di tiap slot judul (Bold->SemiBold, SemiBold->Medium) sesuai riset roadmap (README
// CONVX: "clean+minimalis", motion & permukaan yang jadi ciri khas, bukan huruf tebal), dan
// letterSpacing dibuka positif/mendekati 0 (bukan negatif rapat ala Apple) untuk kesan lapang.
// fontSize/lineHeight SENGAJA dipertahankan sama seperti AppleTypography — perubahan hierarki
// ukuran teks itu risiko layout terpisah (reflow/wrap beda), di luar scope fase 1 ("token
// murni, belum diterapkan ke komponen mana pun"). Purely ADDITIF — belum dipakai di Theme.kt
// manapun sampai `LiquidGlassShapes`+dispatch identitas baru ditambahkan (fase 2).
// Batch 298 — perkuat typography Liquid Glass, dipasangkan sengaja dengan blurRadius yang
// dinaikkan di BlurUtils.kt (blur lebih kuat = backdrop lebih "ramai", header/label butuh
// kontras lebih tinggi biar tetap kebaca di atasnya — bukan 2 perubahan lepas-lepas).
// 2 kelompok perubahan dari baseline Batch 279:
// 1) BOBOT: titleLarge/titleMedium/labelSmall naik 1 tingkat (SemiBold->Bold, Medium->SemiBold,
//    Medium->SemiBold) — ukuran & letterSpacing TIDAK disentuh (tetap sengaja terbuka/0, bukan
//    rapat ala Apple, sesuai identitas asli), murni bobot yang naik.
// 2) SLOT BARU: headlineSmall/titleSmall/bodyLarge/labelLarge/labelMedium — 5 slot M3 yang
//    dipakai luas di app (StatsDashboardScreen, LibraryScreen, SettingsScreen, LyricsView/Sheet,
//    SmartPlaylistScreen dkk — grep MaterialTheme.typography) tapi belum pernah didefinisikan di
//    sini, jadi diam-diam jatuh ke Typography() default Material3 (Roboto) tiap Liquid Glass
//    aktif — 1 satu-satunya identitas dari 5 yang punya lubang ini. Nilai baru mengikuti pola
//    ukuran "app selalu +-beberapa sp di atas default M3" yang sudah ada di slot lama (bukan
//    angka M3 mentah), dan mengikuti aturan tier bobot yang sama: slot berperan header/label
//    (headlineSmall/titleSmall/labelLarge) ikut naik ke SemiBold/Bold, slot berperan teks baca
//    (bodyLarge) tetap Normal seperti bodyMedium/bodySmall di sebelahnya — SENGAJA tidak semua
//    teks ditebalkan, kontras datang dari header vs body, bukan dari blanket bold.
val LiquidGlassTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 35.6.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 25.5.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.4.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.3.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    )
)
