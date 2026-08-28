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
val LiquidGlassTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 35.6.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 25.5.sp,
        letterSpacing = 0.sp
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
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    )
)
