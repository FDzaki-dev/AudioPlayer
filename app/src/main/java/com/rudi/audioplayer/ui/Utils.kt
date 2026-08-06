package com.rudi.audioplayer.ui

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import java.util.Locale

fun albumArtUri(albumId: Long): Uri =
    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

/**
 * Shared album-art loader with one themed "no cover" fallback, used everywhere art is shown
 * (Home, Library, MiniPlayerBar, Now Playing) instead of each screen leaving a blank space
 * when a song simply has no embedded artwork. [modifier] should already carry the caller's
 * size + clip — this only layers a tinted background and a centered fallback icon behind/
 * instead of the image, so the "no cover" look is identical everywhere. [showIcon] is false
 * for decorative/blurred usages (e.g. Now Playing's full-screen blurred backdrop), where a
 * note icon would just smear into a shapeless blob once blurred — the tinted background alone
 * reads better there. `loading` is intentionally blank (not the fallback icon) so songs that
 * *do* have art don't flash the icon first while Coil decodes.
 */
@Composable
fun AlbumArt(
    albumId: Long?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    showIcon: Boolean = true
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (albumId != null) {
            SubcomposeAsyncImage(
                model = albumArtUri(albumId),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize(),
                loading = {},
                error = { if (showIcon) AlbumArtFallbackIcon() }
            )
        } else if (showIcon) {
            AlbumArtFallbackIcon()
        }
    }
}

@Composable
private fun AlbumArtFallbackIcon() {
    Icon(
        Icons.Default.MusicNote,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxSize(0.4f)
    )
}

/**
 * Compose's LocalContext is often a themed ContextWrapper, not the Activity itself.
 * Needed to reach the current window (e.g. to set a per-app screen brightness override
 * for the brightness swipe gesture) without requiring any extra permission.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

/**
 * Subtle scale-down-then-spring-back on press — the tactile micro-feedback
 * premium music apps put on every tappable control, not just the play button.
 * Pass the same [interactionSource] the button itself uses so the two agree
 * on when a press is happening.
 */
fun Modifier.bouncyPress(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.88f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bouncyPress"
    )
    this.scale(scale)
}
