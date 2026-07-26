package com.rudi.audioplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Frosted-glass background: a real blur of whatever sits behind this surface on Android 12+
 * (API 31, where Modifier.blur first has an actual RenderEffect to render with), layered over
 * a translucent tint of the surface color so it still reads as "glass" rather than "flat" on
 * older Android versions. minSdk here is 23, so this fallback matters — Modifier.blur() itself
 * is always safe to call at any API level, it simply has no visible effect below 31.
 */
@Composable
fun Modifier.frostedGlass(
    tint: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = 0.72f,
    blurRadius: Dp = 24.dp
): Modifier {
    val blurred = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) this.blur(blurRadius) else this
    return blurred.background(tint.copy(alpha = alpha))
}
