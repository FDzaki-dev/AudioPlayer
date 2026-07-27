package com.rudi.audioplayer.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun WaveformSeekBar(seed: Long, progress: Float, playedColor: Color, unplayedColor: Color, modifier: Modifier = Modifier) {
    val barHeights = remember(seed) { kotlin.random.Random(seed).let { r -> List(48) { 0.25f + r.nextFloat() * 0.75f } } }
    Canvas(modifier) {
        val w = size.width / 48
        val gap = w * 0.35f
        val playedBars = (progress * 48).toInt()
        barHeights.forEachIndexed { i, h ->
            val hPx = size.height * h
            drawRoundRect(
                if (i < playedBars) playedColor else unplayedColor,
                Offset(i * w + gap / 2, (size.height - hPx) / 2),
                Size(w - gap, hPx),
                CornerRadius(2f, 2f)
            )
        }
    }
}

@Composable
fun StarRatingRow(rating: Int, onRate: (Int) -> Unit, accentColor: Color) {
    Row {
        for (star in 1..5) {
            IconButton({ onRate(if (rating == star) 0 else star) }, Modifier.size(32.dp)) {
                Icon(if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder, null, tint = if (star <= rating) accentColor else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun GestureBadge(icon: ImageVector, value: Float, accentColor: Color) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(0.9f), shadowElevation = 4.dp) {
        Column(Modifier.padding(16.dp, 14.dp), Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accentColor)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun AlbumArtHero(albumId: Long?, accentColor: Color, onNext: () -> Unit, onPrev: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var drag by remember { mutableStateOf(0f) }
    Box(Alignment.Center, Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures(onDragStart = { drag = 0f }, onDragEnd = {
            if (drag < -120f) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNext() }
            else if (drag > 120f) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onPrev() }
        }, onHorizontalDrag = { c, d -> drag += d; c.consume() })
    }) {
        Box(Modifier.size(300.dp).blur(90.dp).background(accentColor.copy(0.38f), CircleShape))
        AsyncImage(albumId?.let { albumArtUri(it) }, null, modifier = Modifier.size(280.dp).shadow(28.dp, RoundedCornerShape(28.dp), spotColor = accentColor.copy(0.45f)).clip(RoundedCornerShape(28.dp)))
    }
}
