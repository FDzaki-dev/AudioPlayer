package com.rudi.audioplayer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.ui.theme.LocalIsDarkTheme
import com.rudi.audioplayer.ui.theme.SkeuAccentLight
import com.rudi.audioplayer.ui.theme.SkeuGilt
import com.rudi.audioplayer.ui.theme.SkeuGiltDeep
import com.rudi.audioplayer.ui.theme.SkeuRubyHi
import com.rudi.audioplayer.ui.theme.SkeuRubyLo
import com.rudi.audioplayer.ui.theme.SkeuWine
import com.rudi.audioplayer.ui.theme.SkeuWineDeep
import com.rudi.audioplayer.ui.theme.SkeuWineLit
import com.rudi.audioplayer.ui.theme.drawSkeuGem
import com.rudi.audioplayer.ui.theme.isSkeuTheme

// Batch 568 — SLIDER Neumorphism (sistem anggur/ruby/gilt, lanjutan Batch 563-566): `Slider` M3 di Pengaturan-turunan
// (EqualizerSheet, AdvancedControlsSheet [volume], RingtoneCutterSheet) masih thumb/track default `primary` = datar,
// di luar mesin kedalaman. `NeuSlider` = pembungkus `Slider` M3 (nama BERBEDA dengan sengaja: `Slider` seek bar
// Now Playing di NowPlayingScreen.kt TIDAK boleh ikut terambil, ia punya visual sendiri `WaveformSeekBar`).
//   - Tema NON-Neumorphism: meneruskan semua argumen ke `Slider` M3 apa adanya (`colors` bawaan pemanggil dipertahankan)
//     -> perilaku & tampilan IDENTIK sebelum Batch 568.
//   - Neumorphism: slot `thumb`/`track` M3. TRACK = sumur bersudut membulat (lantai gelap, bayangan dinding atas, bibir
//     terang di bawah) berisi pita anggur terbenam sepanjang nilai; THUMB = permata ruby berbingkai gilt (`drawSkeuGem`,
//     keluarga permata saklar Batch 564 & seek bar Batch 566). Digambar langsung (0 bitmap/blur); `value` dibaca di fase
//     gambar via lambda `drawBehind`. Interaksi/semantik/hitbox/haptic tetap milik `Slider` M3 (tak disentuh).
// Palet lantai & pita = angka yang sama dgn `neuSwitchPalette` skeu (NeuSwitch.kt) supaya sumur slider & sumur saklar senada.
// NOT VERIFIED: belum dikompilasi/dilihat di device.

private val NeuSliderThumbSize = 28.dp
private val NeuSliderGemRadius = 8.dp
private val NeuSliderTrackHeight = 12.dp
private val NeuSliderTrackCorner = 4.dp
private const val NeuSliderDisabledAlpha = 0.38f

@Composable
internal fun NeuSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    if (!isSkeuTheme()) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            colors = colors
        )
    } else {
        val isDark = LocalIsDarkTheme.current
        val span = valueRange.endInclusive - valueRange.start
        val fraction = if (span > 0f) ((value - valueRange.start) / span).coerceIn(0f, 1f) else 0f
        val dim = if (enabled) 1f else NeuSliderDisabledAlpha
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            thumb = { NeuSliderThumb(isDark = isDark, dim = dim) },
            track = { NeuSliderTrack(fraction = fraction, isDark = isDark, dim = dim) }
        )
    }
}

@Composable
private fun NeuSliderThumb(isDark: Boolean, dim: Float) {
    val rim = if (isDark) SkeuGilt else SkeuGiltDeep
    Box(
        modifier = Modifier
            .size(NeuSliderThumbSize)
            .alpha(dim)
            .drawBehind {
                drawSkeuGem(
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = NeuSliderGemRadius.toPx(),
                    rim = rim,
                    hi = SkeuRubyHi,
                    lo = SkeuRubyLo,
                    halo = 1f
                )
            }
    )
}

@Composable
private fun NeuSliderTrack(fraction: Float, isDark: Boolean, dim: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(NeuSliderTrackHeight)
            .alpha(dim)
            .drawBehind { drawNeuSliderTrack(fraction = fraction, isDark = isDark) }
    )
}

/** Sumur + pita anggur. Lapisan (belakang -> depan): bibir terang bawah, lantai, bayangan dinding atas, pita, sorot pita. */
private fun DrawScope.drawNeuSliderTrack(fraction: Float, isDark: Boolean) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val d = 1.dp.toPx()
    val corner = CornerRadius(NeuSliderTrackCorner.toPx())

    val floorTop = if (isDark) Color(0xFF070405) else Color(0xFFC6BAAA)
    val floorBottom = if (isDark) Color(0xFF181012) else Color(0xFFE0D6C8)
    val ribbonTop = if (isDark) SkeuWineDeep else Color(0xFF4A1224)
    val ribbonBottom = if (isDark) lerp(SkeuWine, SkeuWineLit, 0.5f) else lerp(SkeuAccentLight, SkeuWineLit, 0.4f)
    val lip = if (isDark) Color(0xFFE9D7C4).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.75f)

    // 1) Bibir terang di tepi bawah sumur: bidang penuh di belakang; lantai (2) dibuat 1dp lebih pendek sehingga
    //    hanya sliver bawahnya yang tampak.
    drawRoundRect(color = lip, size = Size(w, h), cornerRadius = corner)

    // 2) Lantai sumur: gradien vertikal gelap -> sedikit lebih terang.
    drawRoundRect(
        brush = Brush.verticalGradient(colors = listOf(floorTop, floorBottom), startY = 0f, endY = h - d),
        size = Size(w, h - d),
        cornerRadius = corner
    )

    // 3) Bayangan dinding atas (terdalam): memudar ke bawah.
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = if (isDark) 0.55f else 0.35f), Color.Transparent),
            startY = 0f,
            endY = h * 0.65f
        ),
        size = Size(w, h - d),
        cornerRadius = corner
    )

    // 4) Pita anggur terbenam (inset 2dp dari dinding) sepanjang nilai.
    val inset = 2f * d
    val ribbonWidth = w * fraction - inset
    if (ribbonWidth > inset) {
        val ribbonSize = Size(ribbonWidth, h - d - 2f * inset)
        val ribbonCorner = CornerRadius((NeuSliderTrackCorner.toPx() - inset / 2f).coerceAtLeast(1f))
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(ribbonTop, ribbonBottom),
                startY = inset,
                endY = inset + ribbonSize.height
            ),
            topLeft = Offset(inset, inset),
            size = ribbonSize,
            cornerRadius = ribbonCorner
        )
        // 5) Sorot pita: garis terang tipis di tepi atas pita.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f),
            topLeft = Offset(inset, inset),
            size = Size(ribbonWidth, 1.5f * d),
            cornerRadius = ribbonCorner
        )
    }
}
