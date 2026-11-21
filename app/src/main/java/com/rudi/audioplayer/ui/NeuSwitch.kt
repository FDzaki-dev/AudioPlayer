package com.rudi.audioplayer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.ui.theme.LocalIsDarkTheme
import com.rudi.audioplayer.ui.theme.SkeuAccentLight
import com.rudi.audioplayer.ui.theme.SkeuGilt
import com.rudi.audioplayer.ui.theme.SkeuRubyHi
import com.rudi.audioplayer.ui.theme.SkeuRubyLo
import com.rudi.audioplayer.ui.theme.SkeuWine
import com.rudi.audioplayer.ui.theme.SkeuWineDeep
import com.rudi.audioplayer.ui.theme.SkeuWineLit
import com.rudi.audioplayer.ui.theme.isSkeuTheme

// Batch 564 — SAKLAR DIGAMBAR ULANG: sumur berdinding, kenop kuningan berkubah, permata ruby.
// Batch 563 merakit saklar dari `neuTrough` (bitmap sumur 0.75x) + `neuSlidingKey` (kunci datar): hasilnya
// sumur nyaris tanpa dinding (bayangan dalam 1.6dp), kenop = pelat beige polos tanpa kubah/sorot/pantul.
// Kini seluruh badan digambar langsung dengan gradien (0 bitmap, 0 BlurMaskFilter; state `progress` dibaca di
// fase GAMBAR -> 0 rekomposisi per frame animasi), berlapis dari belakang ke depan:
//   HALO (di luar bidang)  pendar anggur melebar 4 lapis saat ON + bibir luar sumur (gelap kiri-atas, terang
//                          kanan-bawah) — bibir ini yang membuat sumur terbaca MASUK ke permukaan tile.
//   LANTAI                 gradien vertikal: OFF = arang-anggur nyaris hitam, ON = burgundy (atas pekat,
//                          bawah menyala) + pendar ruby yang berpusat di kenop (cahaya permata memantul).
//   DINDING                bayangan dinding atas (terdalam) + kiri, bibir terang dinding kanan-bawah,
//                          oklusi ambien di sepanjang tepi (gradien: kuat kiri-atas, tipis kanan-bawah).
//   TANDA PAHAT            "I" gilt (ON, di sisi kiri yang kosong) / cincin "O" (OFF, di sisi kanan) seperti
//                          saklar fisik; keduanya cross-fade mengikuti progress. Keadaan terbaca tanpa warna.
//   KENOP                  bayangan jatuh 4 lapis ke lantai (jatuh kanan-bawah), badan kuningan diagonal,
//                          piringan cekung (dish) di tengah, sorot spekular kiri-atas, bevel tepi, lalu di
//                          pusat: ON = permata ruby bersorot + pendar, OFF = lubang pahatan gelap.
//                          OFF kenop redup (pewter-champagne), ON mengilap penuh (gilt) — tidak sama persis
//                          supaya "menyala" terbaca dari kenopnya juga.
// Resolusi `Switch` tetap lewat package yang sama (Batch 563): 14 titik panggil TIDAK disentuh; pemanggil yang
// memakai parameter yang tak ada di sini (`colors`, `thumbContent`, `interactionSource`) jatuh balik ke
// `Switch` M3 (hari ini 14/14 titik panggil hanya memakai `checked`, `onCheckedChange`, `enabled`).
// Geometri luar IDENTIK Batch 563 (track 52x30dp, kenop 22dp, jarak 4dp) -> tata letak baris tak bergeser.
// Neumorphism = sudut persegi membulat (track 9dp, kenop 7dp); tema lain = pil + kenop bulat, palet turunan
// colorScheme (bukan token Neumorphism). Tekan = `LocalIndication` (`NeuDepthIndication`, Batch 560).
// NOT VERIFIED: belum dikompilasi/dilihat di device; alpha/warna = simulasi statis.

private val SwitchTrackWidth = 52.dp
private val SwitchTrackHeight = 30.dp
private val SwitchThumbSize = 22.dp
private val SwitchThumbGap = 4.dp

/** Palet satu saklar: pasangan OFF/ON di-lerp oleh `progress` (tanpa layer alpha tambahan). */
private data class NeuSwitchPalette(
    val offTop: Color,
    val offBottom: Color,
    val onTop: Color,
    val onBottom: Color,
    val halo: Color,
    val glow: Color,
    val thumbHiOff: Color,
    val thumbMidOff: Color,
    val thumbLoOff: Color,
    val thumbHiOn: Color,
    val thumbMidOn: Color,
    val thumbLoOn: Color,
    val jewelHi: Color,
    val jewelLo: Color,
    val pit: Color,
    val markOn: Color,
    val markOff: Color,
    val lip: Color,
    val lipAlpha: Float
)

private fun neuSwitchPalette(skeu: Boolean, isDark: Boolean, scheme: ColorScheme): NeuSwitchPalette = when {
    skeu && isDark -> NeuSwitchPalette(
        offTop = Color(0xFF070405),
        offBottom = Color(0xFF181012),
        onTop = SkeuWineDeep,
        onBottom = lerp(SkeuWine, SkeuWineLit, 0.5f),
        halo = SkeuWineLit,
        glow = SkeuRubyHi,
        thumbHiOff = Color(0xFFCFC2A9),
        thumbMidOff = Color(0xFF9E927E),
        thumbLoOff = Color(0xFF5F5545),
        thumbHiOn = Color(0xFFFBEBC8),
        thumbMidOn = Color(0xFFDDBF88),
        thumbLoOn = Color(0xFFA2804A),
        jewelHi = SkeuRubyHi,
        jewelLo = SkeuRubyLo,
        pit = Color(0xFF2B2219),
        markOn = SkeuGilt,
        markOff = Color(0xFF8A7B66),
        lip = Color(0xFFE9D7C4),
        lipAlpha = 0.22f
    )
    skeu -> NeuSwitchPalette(
        offTop = Color(0xFFC6BAAA),
        offBottom = Color(0xFFE0D6C8),
        onTop = Color(0xFF4A1224),
        onBottom = lerp(SkeuAccentLight, SkeuWineLit, 0.4f),
        halo = SkeuAccentLight,
        glow = SkeuRubyHi,
        thumbHiOff = Color(0xFFFFFBF2),
        thumbMidOff = Color(0xFFE8DEC9),
        thumbLoOff = Color(0xFFB9A98C),
        thumbHiOn = Color(0xFFFFF4DA),
        thumbMidOn = Color(0xFFEBD3A0),
        thumbLoOn = Color(0xFFB38F57),
        jewelHi = SkeuRubyHi,
        jewelLo = SkeuRubyLo,
        pit = Color(0xFF6B5A44),
        markOn = Color(0xFFF2DFB8),
        markOff = Color(0xFF6E5F52),
        lip = Color.White,
        lipAlpha = 0.75f
    )
    isDark -> NeuSwitchPalette(
        offTop = lerp(scheme.surface, Color.Black, 0.80f),
        offBottom = lerp(scheme.surface, Color.Black, 0.45f),
        onTop = lerp(scheme.primary, Color.Black, 0.70f),
        onBottom = lerp(scheme.primary, Color.Black, 0.25f),
        halo = scheme.primary,
        glow = lerp(scheme.primary, Color.White, 0.35f),
        thumbHiOff = lerp(scheme.onSurface, Color.White, 0.10f),
        thumbMidOff = lerp(scheme.onSurface, scheme.surface, 0.35f),
        thumbLoOff = lerp(scheme.onSurface, Color.Black, 0.55f),
        thumbHiOn = lerp(scheme.onSurface, Color.White, 0.45f),
        thumbMidOn = lerp(scheme.onSurface, Color.White, 0.10f),
        thumbLoOn = lerp(scheme.onSurface, Color.Black, 0.40f),
        jewelHi = lerp(scheme.primary, Color.White, 0.55f),
        jewelLo = lerp(scheme.primary, Color.Black, 0.40f),
        pit = Color(0xFF1A1A1A),
        markOn = lerp(scheme.primary, Color.White, 0.85f),
        markOff = scheme.onSurface.copy(alpha = 0.55f),
        lip = Color.White,
        lipAlpha = 0.20f
    )
    else -> NeuSwitchPalette(
        offTop = lerp(scheme.surface, Color.Black, 0.20f),
        offBottom = lerp(scheme.surface, Color.Black, 0.06f),
        onTop = lerp(scheme.primary, Color.Black, 0.35f),
        onBottom = scheme.primary,
        halo = scheme.primary,
        glow = lerp(scheme.primary, Color.White, 0.40f),
        thumbHiOff = Color.White,
        thumbMidOff = lerp(Color.White, Color.Black, 0.06f),
        thumbLoOff = lerp(Color.White, Color.Black, 0.24f),
        thumbHiOn = Color.White,
        thumbMidOn = lerp(Color.White, Color.Black, 0.04f),
        thumbLoOn = lerp(Color.White, Color.Black, 0.20f),
        jewelHi = lerp(scheme.primary, Color.White, 0.50f),
        jewelLo = lerp(scheme.primary, Color.Black, 0.35f),
        pit = Color(0xFF8A8A8A),
        markOn = Color.White,
        markOff = scheme.onSurface.copy(alpha = 0.50f),
        lip = Color.White,
        lipAlpha = 0.80f
    )
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = LocalIsDarkTheme.current
    val skeu = isSkeuTheme()
    val trackShape: Shape = if (skeu) RoundedCornerShape(9.dp) else CircleShape
    val pal = remember(skeu, isDark, scheme.primary, scheme.surface, scheme.onSurface) {
        neuSwitchPalette(skeu, isDark, scheme)
    }

    val progress = animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 240),
        label = "neuSwitchProgress"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val interactive = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onValueChange = onCheckedChange
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(if (onCheckedChange != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .size(SwitchTrackWidth, SwitchTrackHeight)
            .alpha(if (enabled) 1f else 0.38f)
            // di luar clip: halo + bibir luar sumur boleh melebar keluar bidang track
            .drawBehind { drawNeuSwitchHalo(pal, progress.value, if (skeu) 9.dp.toPx() else size.height / 2f) }
            .clip(trackShape)
            .then(interactive)
            .drawBehind { drawNeuSwitchBody(pal, progress.value, skeu) }
    )
}

/** Pendar anggur 4 lapis (faux-blur murah) saat ON + bibir luar sumur. Digambar SEBELUM clip. */
private fun DrawScope.drawNeuSwitchHalo(pal: NeuSwitchPalette, p: Float, trackR: Float) {
    val d = 1.dp.toPx()
    val w = size.width
    val h = size.height
    if (p > 0.02f) {
        for (i in 1..4) {
            val grow = i * 1.6f * d
            drawRoundRect(
                color = pal.halo.copy(alpha = 0.075f * p * (5 - i) / 4f),
                topLeft = Offset(-grow, -grow),
                size = Size(w + 2f * grow, h + 2f * grow),
                cornerRadius = CornerRadius(trackR + grow)
            )
        }
    }
    // bibir luar: sisi kiri-atas membelakangi cahaya = gelap, sisi kanan-bawah menangkap cahaya = terang.
    val e = 0.75f * d
    drawRoundRect(
        brush = Brush.linearGradient(
            0.00f to Color.Black.copy(alpha = 0.55f),
            0.42f to Color.Black.copy(alpha = 0f),
            0.58f to pal.lip.copy(alpha = 0f),
            1.00f to pal.lip.copy(alpha = pal.lipAlpha),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        ),
        topLeft = Offset(-e, -e),
        size = Size(w + 2f * e, h + 2f * e),
        cornerRadius = CornerRadius(trackR + e),
        style = Stroke(width = 1.5f * d)
    )
}

/** Lantai + dinding + tanda pahat + kenop. Digambar DI DALAM clip bentuk track. */
private fun DrawScope.drawNeuSwitchBody(pal: NeuSwitchPalette, p: Float, skeu: Boolean) {
    val d = 1.dp.toPx()
    val w = size.width
    val h = size.height
    val ts = SwitchThumbSize.toPx()
    val gap = SwitchThumbGap.toPx()
    val trackR = if (skeu) 9.dp.toPx() else h / 2f
    val thumbR = if (skeu) 7.dp.toPx() else ts / 2f
    val tx = gap + (w - 2f * gap - ts) * p
    val ty = (h - ts) / 2f
    val cr = CornerRadius(trackR)

    // ---- LANTAI ----
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(lerp(pal.offTop, pal.onTop, p), lerp(pal.offBottom, pal.onBottom, p))
        ),
        cornerRadius = cr
    )
    if (p > 0.02f) {
        // cahaya permata memantul di lantai: berpusat di kenop, ikut meluncur
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(pal.glow.copy(alpha = 0.50f * p), pal.glow.copy(alpha = 0f)),
                center = Offset(tx + ts / 2f, h * 0.58f),
                radius = h * 1.15f
            ),
            cornerRadius = cr
        )
    }

    // ---- DINDING ----
    val topBand = 11f * d
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.70f), Color.Black.copy(alpha = 0.22f), Color.Black.copy(alpha = 0f)),
            startY = 0f,
            endY = topBand
        ),
        size = Size(w, topBand)
    )
    val leftBand = 9f * d
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0f)),
            startX = 0f,
            endX = leftBand
        ),
        size = Size(leftBand, h)
    )
    val lipBand = 6f * d
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(pal.lip.copy(alpha = 0f), pal.lip.copy(alpha = pal.lipAlpha * 0.55f)),
            startY = h - lipBand,
            endY = h
        ),
        topLeft = Offset(0f, h - lipBand),
        size = Size(w, lipBand)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(pal.lip.copy(alpha = 0f), pal.lip.copy(alpha = pal.lipAlpha * 0.40f)),
            startX = w - lipBand,
            endX = w
        ),
        topLeft = Offset(w - lipBand, 0f),
        size = Size(lipBand, h)
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            0.0f to Color.Black.copy(alpha = 0.65f),
            1.0f to Color.Black.copy(alpha = 0.15f),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        ),
        cornerRadius = cr,
        style = Stroke(width = 2f * d)
    )

    // ---- TANDA PAHAT: "I" (ON, sisi kiri kosong) dan cincin "O" (OFF, sisi kanan kosong) ----
    val cy = h / 2f
    val cxL = gap + ts / 2f
    val cxR = w - gap - ts / 2f
    if (p > 0.02f) {
        drawLine(
            color = pal.markOn.copy(alpha = 0.22f * p),
            start = Offset(cxL, cy - 5f * d),
            end = Offset(cxL, cy + 5f * d),
            strokeWidth = 4.2f * d,
            cap = StrokeCap.Round
        )
        drawLine(
            color = pal.markOn.copy(alpha = 0.92f * p),
            start = Offset(cxL, cy - 4.2f * d),
            end = Offset(cxL, cy + 4.2f * d),
            strokeWidth = 1.7f * d,
            cap = StrokeCap.Round
        )
    }
    val q = 1f - p
    if (q > 0.02f) {
        drawCircle(
            color = pal.lip.copy(alpha = 0.14f * q),
            radius = 3.6f * d,
            center = Offset(cxR + 0.8f * d, cy + 0.9f * d),
            style = Stroke(width = 1.5f * d)
        )
        drawCircle(
            color = pal.markOff.copy(alpha = pal.markOff.alpha * 0.9f * q),
            radius = 3.6f * d,
            center = Offset(cxR, cy),
            style = Stroke(width = 1.5f * d)
        )
    }

    // ---- KENOP ----
    // bayangan jatuh ke lantai (cahaya kiri-atas -> bayangan kanan-bawah), 4 lapis faux-blur
    for (i in 0..3) {
        val grow = i * 0.9f * d
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.30f * (4 - i) / 4f),
            topLeft = Offset(tx + 1.0f * d - grow, ty + 2.0f * d - grow),
            size = Size(ts + 2f * grow, ts + 2f * grow),
            cornerRadius = CornerRadius(thumbR + grow)
        )
    }
    val hi = lerp(pal.thumbHiOff, pal.thumbHiOn, p)
    val mid = lerp(pal.thumbMidOff, pal.thumbMidOn, p)
    val lo = lerp(pal.thumbLoOff, pal.thumbLoOn, p)
    val tl = Offset(tx, ty)
    val sz = Size(ts, ts)
    val tcr = CornerRadius(thumbR)
    drawRoundRect(
        brush = Brush.linearGradient(
            0.00f to hi,
            0.50f to mid,
            1.00f to lo,
            start = Offset(tx, ty),
            end = Offset(tx + ts, ty + ts)
        ),
        topLeft = tl,
        size = sz,
        cornerRadius = tcr
    )
    // piringan cekung: gelap kiri-atas, terang kanan-bawah (kebalikan badan luar)
    val inset = 3.6f * d
    drawRoundRect(
        brush = Brush.linearGradient(
            0.0f to Color.Black.copy(alpha = 0.26f),
            1.0f to Color.White.copy(alpha = 0.22f),
            start = Offset(tx + inset, ty + inset),
            end = Offset(tx + ts - inset, ty + ts - inset)
        ),
        topLeft = Offset(tx + inset, ty + inset),
        size = Size(ts - 2f * inset, ts - 2f * inset),
        cornerRadius = CornerRadius((thumbR - inset).coerceAtLeast(1f))
    )
    // sorot spekular lembut kiri-atas
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0f)),
            center = Offset(tx + 0.30f * ts, ty + 0.24f * ts),
            radius = 0.62f * ts
        ),
        topLeft = tl,
        size = sz,
        cornerRadius = tcr
    )
    // bevel tepi
    drawRoundRect(
        brush = Brush.linearGradient(
            0.00f to Color.White.copy(alpha = 0.70f),
            0.40f to Color.White.copy(alpha = 0f),
            0.60f to Color.Black.copy(alpha = 0f),
            1.00f to Color.Black.copy(alpha = 0.50f),
            start = Offset(tx, ty),
            end = Offset(tx + ts, ty + ts)
        ),
        topLeft = Offset(tx + 0.5f * d, ty + 0.5f * d),
        size = Size(ts - d, ts - d),
        cornerRadius = CornerRadius((thumbR - 0.5f * d).coerceAtLeast(0f)),
        style = Stroke(width = d)
    )

    // pusat kenop: lubang pahatan (OFF) <-> permata ruby (ON)
    val c = Offset(tx + ts / 2f, ty + ts / 2f)
    val jr = 3.4f * d
    drawCircle(
        brush = Brush.linearGradient(
            0.0f to Color.Black.copy(alpha = 0.55f),
            1.0f to Color.White.copy(alpha = 0.40f),
            start = Offset(c.x - jr, c.y - jr),
            end = Offset(c.x + jr, c.y + jr)
        ),
        radius = jr + 1.1f * d,
        center = c
    )
    if (q > 0.02f) {
        drawCircle(color = pal.pit.copy(alpha = 0.80f * q), radius = jr, center = c)
    }
    if (p > 0.02f) {
        drawCircle(color = pal.jewelHi.copy(alpha = 0.30f * p), radius = jr + 2.2f * d, center = c)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(pal.jewelHi, pal.jewelLo),
                center = Offset(c.x - 0.9f * d, c.y - 1.0f * d),
                radius = jr * 1.5f
            ),
            radius = jr,
            center = c,
            alpha = p
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.75f * p),
            radius = 0.75f * d,
            center = Offset(c.x - 1.0f * d, c.y - 1.1f * d)
        )
    }
}
