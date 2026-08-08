package com.rudi.audioplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.playback.PlaybackUiState
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.ui.theme.tactileEmboss

@Composable
fun MiniPlayerBar(
    uiState: PlaybackUiState,
    accentColor: Color?,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit
) {
    val song = uiState.currentSong ?: return
    val haptic = LocalHapticFeedback.current
    // Must match the shape frostedGlass() now derives from MaterialTheme.shapes.large,
    // or the outer shadow/clip corners (this Box) would mismatch the inner tinted fill's
    // corners — Tactile's own rounding vs Apple's.
    val barShape = MaterialTheme.shapes.large
    val isTactile = MaterialTheme.colorScheme.background == com.rudi.audioplayer.ui.theme.TactileBackground
    val animatedAccent by animateColorAsState(
        targetValue = accentColor ?: MaterialTheme.colorScheme.primary,
        animationSpec = tween(700),
        label = "miniAccentColor"
    )
    // Album-art accents can be very bright or very dark. Choose the control icon
    // color from luminance so the primary action remains readable in every case.
    val accentContentColor = if (animatedAccent.luminance() > 0.55f) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .then(
                // Batch 49: swapped matteEmboss() for the Tactile equivalent, tactileEmboss()
                // (directional shadow + top-down gradient + bevel border) so the bar reads as
                // a lifted tactile panel, not just a rectangle with a bigger shadow.
                // frostedGlass() below still draws the readable tinted fill on top, matching
                // its own shape/border so corners line up under either theme.
                if (isTactile)
                    Modifier.tactileEmboss(shape = barShape, elevation = 16.dp)
                else
                    Modifier.shadow(elevation = 12.dp, shape = barShape, ambientColor = Color.Black, spotColor = Color.Black)
            )
            .clip(barShape)
            .frostedGlass()
            .clickable(onClick = onExpand)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(animatedAccent.copy(alpha = 0.16f), Color.Transparent)
                    )
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(
                albumId = song.albumId,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            val playPauseInteraction = remember { MutableInteractionSource() }
            FilledIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayPause()
                },
                interactionSource = playPauseInteraction,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = animatedAccent,
                    contentColor = accentContentColor
                ),
                modifier = Modifier
                    .size(40.dp)
                    .bouncyPress(playPauseInteraction, pressedScale = 0.82f)
            ) {
                AnimatedContent(targetState = uiState.isPlaying, label = "miniPlayPause") { playing ->
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Jeda" else "Putar"
                    )
                }
            }
        }
        // Batch 36: uiState.position/duration sudah di-tick tiap detik untuk NowPlayingScreen
        // (lihat startPositionTicker di PlayerViewModel) tapi mini bar tidak menampilkannya sama
        // sekali — user harus buka full player cuma buat lihat sudah sampai mana. Garis tipis di
        // tepi bawah ini murni glanceable, tidak seekable (bukan Slider), jadi tidak menambah
        // target sentuh baru yang bisa konflik dengan onExpand di Box pembungkus. Overload
        // progress lambda dipakai karena overload Float sudah deprecated sejak Material3 1.2.0
        // (proyek ini pin compose-bom 2024.05.00 / Material3 ~1.2.1, sudah include lambda ini).
        val progressFraction = if (uiState.duration > 0) {
            (uiState.position.toFloat() / uiState.duration.toFloat()).coerceIn(0f, 1f)
        } else 0f
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomStart),
            color = animatedAccent,
            trackColor = animatedAccent.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Butt
        )
    }
}
