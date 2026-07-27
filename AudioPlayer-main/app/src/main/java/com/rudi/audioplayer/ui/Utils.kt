package com.rudi.audioplayer.ui

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import java.util.Locale

fun albumArtUri(albumId: Long): Uri =
    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

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
