package com.rudi.audioplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.rudi.audioplayer.ui.theme.skeuEmboss
import com.rudi.audioplayer.ui.theme.isTactileTheme
import com.rudi.audioplayer.ui.theme.isSkeuTheme
import com.rudi.audioplayer.ui.theme.isCalmRetroTheme
import com.rudi.audioplayer.ui.theme.calmAberration
import com.rudi.audioplayer.ui.theme.Radius

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
    val isTactile = isTactileTheme()
    // Batch 58 — Skeu now gets the same "physical panel" branch as Tactile here (previously fell
    // into the Apple-else default: plain shadow + a now-fully-opaque frostedGlass() fill, which
    // read closer to a flat card than Skeu's own bevelled identity).
    val isSkeu = isSkeuTheme()
    // Batch 129 — sama alasan komentar Batch 55 di atas miniPlayPauseShape: identitas baru wajib
    // konsisten muncul di mini bar juga, bukan cuma NowPlayingScreen.
    val isCalmRetro = isCalmRetroTheme()
    val animatedAccent by animateColorAsState(
        // Batch 132 — sama fix/alasan persis NowPlayingScreen.kt: Calm Retro terkunci ke
        // CalmRetroAccent literal (MaterialTheme.colorScheme.primary), tidak ikut accentColor
        // dinamis per-lagu.
        targetValue = if (isCalmRetro) MaterialTheme.colorScheme.primary else (accentColor ?: MaterialTheme.colorScheme.primary),
        animationSpec = tween(700),
        label = "miniAccentColor"
    )
    // Album-art accents can be very bright or very dark. Choose the control icon
    // color from luminance so the primary action remains readable in every case.
    val accentContentColor = if (animatedAccent.luminance() > 0.55f) Color.Black else Color.White
    val miniPlayPauseShape = if (isTactile || isSkeu) MaterialTheme.shapes.medium else CircleShape

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .then(
                // Batch 49: swapped matteEmboss() for the Tactile equivalent, tactileEmboss()
                // (directional shadow + top-down gradient + bevel border) so the bar reads as
                // a lifted tactile panel, not just a rectangle with a bigger shadow.
                // frostedGlass() further below still draws the readable tinted fill on top for
                // Tactile/Apple, matching its own shape/border so corners line up — Skeu opts out
                // of it since Batch 59 (see comment there).
                when {
                    isTactile -> Modifier.tactileEmboss(shape = barShape, elevation = 16.dp)
                    isSkeu -> Modifier.skeuEmboss(shape = barShape, elevation = 16.dp)
                    else -> Modifier.shadow(elevation = 12.dp, shape = barShape, ambientColor = Color.Black, spotColor = Color.Black)
                }
            )
            .clip(barShape)
            // Batch 59 — Skeu skips frostedGlass() here: skeuEmboss() above already paints a
            // complete opaque background + its own tuned bevel border (Batch 58's
            // frostedGlass()/embossSurface() fixes), so stacking frostedGlass() on top would just
            // re-cover both with its own flat fill + border again, exactly the "still hybrid, not
            // autonomous" gap flagged this batch — the mini bar would visually read identical to
            // whatever frostedGlass() draws regardless of skeuEmboss(), wasting its distinct bevel
            // and its press-elevation/scale animation. Tactile/Apple unchanged: Tactile's identity
            // is deliberately hybrid glass-over-emboss (this file's earlier comment, still true for
            // Tactile only), and Apple has no background of its own before this — frostedGlass() is
            // the only fill it gets.
            .then(if (isSkeu) Modifier else Modifier.frostedGlass())
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
                artworkUri = song.uri,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(Radius.ml))
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
                // Batch 55 — matches the same shape/emboss treatment now on the full Now Playing
                // screen's transport button (NowPlayingScreen.kt), scaled down for the mini bar's
                // smaller footprint, so the identity difference is visible everywhere the play
                // button appears, not just after opening the full player.
                shape = miniPlayPauseShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = animatedAccent,
                    contentColor = accentContentColor
                ),
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        when {
                            isTactile -> Modifier.tactileEmboss(shape = miniPlayPauseShape, elevation = 6.dp)
                            isSkeu -> Modifier.skeuEmboss(shape = miniPlayPauseShape, elevation = 6.dp)
                            isCalmRetro -> Modifier.calmAberration(bias = 2.dp)
                            else -> Modifier
                        }
                    )
                    .bouncyPress(playPauseInteraction, pressedScale = 0.82f)
            ) {
                AnimatedContent(
                    targetState = uiState.isPlaying,
                    label = "miniPlayPause",
                    // Batch 332 — konsisten persis dgn fix NowPlayingScreen.kt (Pending Queue
                    // item 1, Batch 330): morph scale+fade, durasi asimetris 200ms masuk/150ms
                    // keluar reuse dari NavHost tab transition (Batch 330).
                    transitionSpec = {
                        (scaleIn(initialScale = 0.6f, animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)))
                            .togetherWith(scaleOut(targetScale = 0.6f, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)))
                    }
                ) { playing ->
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Jeda" else "Putar",
                        // Batch 226 — Iconography 2/7 (audit optical alignment), konsisten
                        // dengan fix NowPlayingScreen.kt: kompensasi bias visual kiri PlayArrow.
                        modifier = if (!playing) Modifier.offset(x = 1.dp) else Modifier
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
