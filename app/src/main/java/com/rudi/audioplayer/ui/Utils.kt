package com.rudi.audioplayer.ui

import android.app.Activity
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import java.util.Locale

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
 *
 * Batch 67: [artworkUri] is the song's own MediaStore content URI (`song.uri`), not an
 * albumId-derived URI. Used to build `content://media/external/audio/albumart/$albumId`
 * (the deprecated per-album cache authority) — that table is frequently empty on modern
 * Android, so most albums silently fell back to the icon below even when the song actually
 * has embedded art. Coil resolves a song's own content URI reliably instead.
 *
 * Batch 344 — BUG FIX (laporan user, screenshot): default [contentScale] SEBELUMNYA
 * `ContentScale.Fit` — untuk lagu dengan embedded artwork NON-1:1 (mis. thumbnail video
 * 16:9 yang ikut ke-embed saat rip/tag dari YouTube, kasus nyata user: "TOBI - Warm Up Mix
 * 2023"), `Fit` mempertahankan seluruh gambar TANPA crop di dalam kotak persegi manapun
 * (44dp MiniPlayerBar, 48dp/56dp/120dp Library/Home, 280dp hero Now Playing) — sisa
 * ruang kosong di atas/bawah gambar menampilkan `background(surfaceVariant)` polos, kelihatan
 * seperti bar hitam/letterbox video, PERSIS "tidak normal/generik" yang dilaporkan. Root cause
 * SISTEMIK (default di 1 fungsi bersama ini), bukan spesifik 1 layar — SEMUA 6 titik pemakaian
 * [AlbumArt] app ini (grep) mengandalkan default ini, 0 satu pun override eksplisit KECUALI
 * backdrop blur Now Playing (`NowPlayingScreen.kt`, sudah `Crop` sejak awal — makanya bug ini
 * baru kelihatan di kotak seni ART UTAMA/thumbnail, bukan di backdrop). Preseden project SENDIRI
 * juga sudah konsisten: widget home-screen (`widget_player.xml`, Batch 204) pakai `centerCrop`,
 * bukan letterbox — dan itu genre-standar universal (Spotify/Apple Music/YouTube Music SELALU
 * crop-fill cover art, tidak pernah pillarbox/letterbox apa pun rasio sumbernya).
 * Fix: default diganti `ContentScale.Crop` — cover art SEKARANG selalu penuh mengisi kotaknya
 * (crop sisi berlebih, 0 bar kosong) di ke-6 titik pemakaian sekaligus, konsisten dgn genre app
 * ini & preseden widget. Override eksplisit `Crop` di backdrop blur Now Playing SENGAJA TIDAK
 * dihapus walau kini redundan — bukan bagian dari bug ini, ZERO-REFACTOR.
 */
@Composable
fun AlbumArt(
    artworkUri: Uri?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showIcon: Boolean = true
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (artworkUri != null) {
            SubcomposeAsyncImage(
                model = artworkUri,
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

/**
 * Visual weight for [ResultBanner] — kept as 3 distinct looks on purpose, not collapsed into
 * one. Batch 165 audit found these had each been hand-duplicated slightly differently across
 * 3 files; the fix here is giving them ONE shared implementation (so future drift is
 * impossible), not forcing them to look identical — each already carried a different semantic
 * weight that's reasonable to keep: [Solid] for a result that's shown once and won't change
 * again, [Tinted] for a state that stays visible while the user reviews/acts further, [Bare]
 * for one status among several in a multi-step flow where a full banner would compete with
 * the progress indicator/button around it.
 */
enum class ResultBannerStyle { Solid, Tinted, Bare }

/**
 * Shared icon+color+text result/status banner. 0 visual change from this refactor by design —
 * [Solid] reproduces exactly what `BackupRestoreSheet.kt`/`DiagnosticLogSheet.kt` already had
 * (container-role background, `RoundedCornerShape(8.dp)`, 12dp/10dp padding, 8dp icon-gap,
 * `bodySmall`), [Tinted] reproduces `SignatureMatcherSheet.kt` (15%-alpha tint of the semantic
 * color itself, `shapes.medium`, 14dp padding, 10dp gap, `bodyMedium`), [Bare] reproduces
 * `UpdateCheckSheet.kt`'s old private `StatusBanner` (no background at all, 8dp gap,
 * `bodyMedium`). For [Solid], pass the M3 container role color (e.g. `primaryContainer`) as
 * [containerColor] and its `onXContainer` pair as [contentColor]; for [Tinted] and [Bare],
 * pass the same base semantic color (e.g. `colorScheme.error`) as both.
 */
@Composable
fun ResultBanner(
    style: ResultBannerStyle,
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    when (style) {
        ResultBannerStyle.Solid -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .background(color = containerColor, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = contentColor)
        }
        ResultBannerStyle.Tinted -> Row(
            modifier = modifier
                .fillMaxWidth()
                .background(containerColor.copy(alpha = 0.15f), shape = MaterialTheme.shapes.medium)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = contentColor)
        }
        ResultBannerStyle.Bare -> Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, color = contentColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
