package com.rudi.audioplayer.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Batch 40 — Matte Noir "epic" depth pass, direct response to on-device feedback that Batch
 * 38/39's flat tonalElevation port from Apple's own glass-panel model never actually read as
 * premium hardware on every surface, not just one. Root problem: a flat tonal-overlay Surface
 * has no directional light — it just gets a bit lighter/darker as a solid fill, which is why
 * it always looked "kureng" no matter how much elevation/tonalElevation was cranked up.
 *
 * matteEmboss() is the single shared building block used at every "epic" touch point instead
 * (Home, Library, Now Playing, Settings, mini-player, nav bar) so the whole app reads as one
 * consistent light source falling across brushed matte metal, combining three cues at once on
 * one shape:
 *  1. A strong copper-umbra directional drop shadow (native Modifier.shadow — colored
 *     ambient/spot tint only renders on API 28+; below that it silently degrades to a plain
 *     gray shadow, never crashes, so minSdk 23 stays fully supported).
 *  2. A diagonal surface gradient (MatteHighlight → MatteSurface → MatteUmbra) instead of a
 *     flat fill — simulates one light source hitting the top-left corner and falling off
 *     toward the bottom-right, the literal definition of "not flat" that tonalElevation alone
 *     can't produce.
 *  3. A 1dp highlight-brush border fading from MatteHighlight to transparent — the actual
 *     physical cue neumorphism relies on: the top-left edge of a lit panel visibly catches
 *     light. Pressed state inverts/dims this so tappable cards feel like they sink back in.
 *
 * Matte-only by construction (uses Matte* colors directly, not colorScheme roles) — every call
 * site gates with `isMatte` before applying this and falls back to the existing Apple-style
 * Surface/tonalElevation otherwise, so SYSTEM/LIGHT/DARK stay byte-for-byte unchanged.
 */
@Composable
fun Modifier.matteEmboss(
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 10.dp,
    pressed: Boolean = false
): Modifier {
    val effectiveElevation = if (pressed) elevation / 3 else elevation
    val highlightAlpha = if (pressed) 0.05f else 0.14f
    val borderAlpha = if (pressed) 0.15f else 0.5f
    return this
        .shadow(
            elevation = effectiveElevation,
            shape = shape,
            ambientColor = MatteUmbra,
            spotColor = MatteUmbra
        )
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    MatteHighlight.copy(alpha = highlightAlpha),
                    MatteSurface,
                    MatteUmbra.copy(alpha = 0.45f)
                )
            )
        )
        .border(
            BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(MatteHighlight.copy(alpha = borderAlpha), Color.Transparent)
                )
            ),
            shape
        )
}
