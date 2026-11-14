package com.rudi.audioplayer.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

// Batch 656 — SPACE, latar kosmos. Lapisan (dari belakang): void gradien -> 4 awan nebula yang hanyut sangat
// pelan -> planet berkilau atmosfer di pojok kanan-atas -> 150 bintang berkelip (putih/cyan/emas, bintang terang
// berhalo) -> meteor melintas tiap ~14 dtk.
//
// PELAJARAN PERFORMA (Batch 328/440: animasi yang dibaca di KOMPOSISI = recomposition tiap frame = musik stutter/HP
// panas), maka di sini: (1) jam animasi `clock` HANYA dibaca di dalam lambda gambar `Canvas` (invalidasi gambar,
// 0 recomposition); (2) `Canvas` berada di lapisan sendiri (`graphicsLayer`) supaya invalidasinya tidak merambat ke
// layar di atasnya; (3) jam dijepit ~30 fps (frame + jeda 16 ms), bukan 60 fps; (4) posisi bintang dibangkitkan SEKALI
// (deterministik, `remember`); (5) 0 bitmap/blur/shader runtime. Frame clock Compose berhenti sendiri saat app di
// latar belakang/layar mati, jadi tidak ada biaya di sana. Titik awal alpha/kecepatan = simulasi, BELUM dituning di device.
private const val SPACE_STAR_COUNT = 150
private const val SPACE_STAR_SEED = 656
private const val SPACE_FRAME_GAP_MS = 16L
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val TWO_PI = 6.2831855f
private const val HALF_TURN = 3.1415927f
private const val NEBULA_SWING_RATE = 0.07f
private const val METEOR_PERIOD_S = 14f
private const val METEOR_ACTIVE_FRACTION = 0.11f
private const val STAR_TINT_WHITE = 0
private const val STAR_TINT_CYAN = 1
private const val STAR_TINT_GOLD = 2

private class SpaceStarField(count: Int, seed: Int) {
    val x = FloatArray(count)
    val y = FloatArray(count)
    val weight = FloatArray(count) // 0..1, 1 = paling terang/besar
    val phase = FloatArray(count)
    val period = FloatArray(count) // detik per satu kedipan penuh
    val tint = IntArray(count)

    init {
        val rnd = Random(seed)
        for (i in 0 until count) {
            x[i] = rnd.nextFloat()
            y[i] = rnd.nextFloat()
            val roll = rnd.nextFloat()
            weight[i] = roll * roll // dominasi bintang kecil, segelintir terang
            phase[i] = rnd.nextFloat()
            period[i] = 3f + rnd.nextFloat() * 6f
            val tintRoll = rnd.nextFloat()
            tint[i] = when {
                tintRoll < 0.62f -> STAR_TINT_WHITE
                tintRoll < 0.86f -> STAR_TINT_CYAN
                else -> STAR_TINT_GOLD
            }
        }
    }
}

/** Latar kosmos tema Space. Dipasang SEKALI di root (MainActivity.kt), di bawah semua layar. */
@Composable
fun SpaceCosmosBackdrop() {
    val stars = remember { SpaceStarField(SPACE_STAR_COUNT, SPACE_STAR_SEED) }
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val origin = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { now -> clock.floatValue = (now - origin) / NANOS_PER_SECOND }
            delay(SPACE_FRAME_GAP_MS)
        }
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { clip = false }
    ) {
        val t = clock.floatValue
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(SpaceVoidTop, SpaceBackground, SpaceVoidBottom)
            )
        )
        drawNebula(t)
        drawPlanet()
        drawStars(stars, t)
        drawMeteor(t)
    }
}

private fun DrawScope.drawCloud(center: Offset, radius: Float, color: Color, alpha: Float) {
    // Ujung memudar ke warna SAMA ber-alpha 0 (bukan Color.Transparent hitam) supaya tidak ada pinggiran abu-abu.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.35f),
                color.copy(alpha = 0f)
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawNebula(t: Float) {
    val w = size.width
    val h = size.height
    val reach = max(w, h)
    val swing = t * NEBULA_SWING_RATE
    drawCloud(
        center = Offset(w * (0.14f + 0.07f * sin(swing)), h * (0.12f + 0.05f * cos(swing * 0.8f))),
        radius = reach * 0.95f,
        color = SpaceNebulaIndigo,
        alpha = 0.36f
    )
    drawCloud(
        center = Offset(w * (0.96f - 0.06f * cos(swing * 0.7f + 1.3f)), h * (0.48f + 0.06f * sin(swing * 0.9f + 0.6f))),
        radius = reach * 0.80f,
        color = SpaceNebulaViolet,
        alpha = 0.24f
    )
    drawCloud(
        center = Offset(w * (0.06f + 0.05f * sin(swing * 0.6f + 2.1f)), h * (0.88f + 0.04f * cos(swing + 0.4f))),
        radius = reach * 0.75f,
        color = SpaceNebulaRose,
        alpha = 0.17f
    )
    drawCloud(
        center = Offset(w * 0.86f, h * 1.02f),
        radius = reach * 0.55f,
        color = SpaceAccent,
        alpha = 0.12f
    )
}

private fun DrawScope.drawPlanet() {
    val w = size.width
    val r = w * 0.55f
    val center = Offset(w * 0.98f, size.height * 0.02f)
    // Badan planet: gelap, sisi menghadap cahaya nebula (kiri-bawah) sedikit lebih terang.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SpaceSurfaceVariant.copy(alpha = 0.80f), SpaceBackground.copy(alpha = 0.92f)),
            center = center + Offset(-r * 0.45f, r * 0.45f),
            radius = r * 1.5f
        ),
        radius = r,
        center = center
    )
    // Atmosfer: cincin cyan tipis tepat di tepi badan (stop 0.925 = r / (r * 1.08)), memudar ke luar & ke dalam.
    val glow = r * 1.08f
    drawCircle(
        brush = Brush.radialGradient(
            0.80f to SpaceAccent.copy(alpha = 0f),
            0.925f to SpaceAccent.copy(alpha = 0.30f),
            1.00f to SpaceAccent.copy(alpha = 0f),
            center = center,
            radius = glow
        ),
        radius = glow,
        center = center
    )
}

private fun DrawScope.drawStars(stars: SpaceStarField, t: Float) {
    val w = size.width
    val h = size.height
    val unit = 1.dp.toPx()
    for (i in stars.x.indices) {
        val twinkle = 0.5f + 0.5f * sin(TWO_PI * (t / stars.period[i] + stars.phase[i]))
        val weight = stars.weight[i]
        val alpha = (0.28f + 0.72f * twinkle) * (0.45f + 0.55f * weight)
        val color = when (stars.tint[i]) {
            STAR_TINT_CYAN -> SpaceAccent
            STAR_TINT_GOLD -> SpaceStarGold
            else -> SpaceStarWhite
        }
        val center = Offset(stars.x[i] * w, stars.y[i] * h)
        val radius = (0.55f + 1.25f * weight) * unit
        if (weight > 0.72f) {
            drawCircle(color = color.copy(alpha = alpha * 0.16f), radius = radius * 4.2f, center = center)
        }
        drawCircle(color = color.copy(alpha = alpha), radius = radius, center = center)
    }
}

private fun DrawScope.drawMeteor(t: Float) {
    val cycle = t / METEOR_PERIOD_S
    val phase = cycle - floor(cycle)
    if (phase > METEOR_ACTIVE_FRACTION) return
    val progress = phase / METEOR_ACTIVE_FRACTION
    val w = size.width
    val h = size.height
    val start: Offset
    val dir: Offset
    when (cycle.toInt() % 3) {
        0 -> {
            start = Offset(w * 0.96f, h * 0.05f)
            dir = Offset(-0.83f, 0.56f)
        }
        1 -> {
            start = Offset(w * 0.64f, h * 0.02f)
            dir = Offset(-0.74f, 0.67f)
        }
        else -> {
            start = Offset(w * 0.40f, h * 0.09f)
            dir = Offset(-0.90f, 0.44f)
        }
    }
    val head = start + dir * (w * 0.55f * progress)
    val tail = head - dir * (w * 0.16f)
    val fade = sin(HALF_TURN * progress) // masuk-keluar halus
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(SpaceStarWhite.copy(alpha = 0f), SpaceStarWhite.copy(alpha = fade)),
            start = tail,
            end = head
        ),
        start = tail,
        end = head,
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawCircle(color = SpaceStarWhite.copy(alpha = fade), radius = 1.6.dp.toPx(), center = head)
}
