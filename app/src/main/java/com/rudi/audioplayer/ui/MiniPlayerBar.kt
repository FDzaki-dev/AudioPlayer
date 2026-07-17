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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rudi.audioplayer.playback.PlaybackUiState

@Composable
fun MiniPlayerBar(
    uiState: PlaybackUiState,
    accentColor: Color?,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit
) {
    val song = uiState.currentSong ?: return
    val haptic = LocalHapticFeedback.current
    val animatedAccent by animateColorAsState(
        targetValue = accentColor ?: MaterialTheme.colorScheme.primary,
        animationSpec = tween(700),
        label = "miniAccentColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onExpand),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(20.dp)
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
            AsyncImage(
                model = albumArtUri(song.albumId),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
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
                    contentColor = MaterialTheme.colorScheme.background
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
    }
}
