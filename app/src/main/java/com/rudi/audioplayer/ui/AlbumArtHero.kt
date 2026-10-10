package com.rudi.audioplayer.ui

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.ui.theme.isTactileTheme
import com.rudi.audioplayer.ui.theme.isSkeuTheme
import com.rudi.audioplayer.ui.theme.isCalmRetroTheme
import com.rudi.audioplayer.ui.theme.calmScanlines
import com.rudi.audioplayer.ui.theme.LocalIsDarkTheme
import com.rudi.audioplayer.ui.theme.SkeuGilt
import com.rudi.audioplayer.ui.theme.SkeuGiltDeep
import androidx.compose.ui.graphics.drawscope.Stroke
import com.rudi.audioplayer.ui.theme.Radius
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import com.rudi.audioplayer.ui.theme.neuRowTile
import kotlin.math.max

// Batch 510 — Wave 2 T6 (docs/PENDING_CodeTidyPlan.md): AlbumArtHero dipindah MOVE-ONLY dari
// NowPlayingScreen.kt (baris 1715-2029 termasuk KDoc, snapshot v509). Badan fungsi & KDoc identik
// karakter-per-karakter; yang berubah hanya visibilitas `private` -> `internal` (dipanggil
// NowPlayingScreen()). Kelas risiko R3 (berisi gesture swipe horizontal next/previous + springback,
// Batch 178/256/434) — gesture brightness/volume BUKAN di sini (ada di Box induk NowPlayingScreen).
// Paket sama.

/** Apple Music-style hero art: a large rounded-square image with a soft ambient glow
 * (tinted by the same accent color already extracted from this song's artwork) instead of
 * the old spinning vinyl. Horizontal swipe-to-skip gesture logic is unchanged from before.
 * Batch 346 — `artSize` baru (dulu literal 280.dp hardcode di sini): caller (NowPlayingScreen)
 * sekarang menghitung ukuran dinamis ("art scale dinamis") dan meneruskannya ke sini. Glow
 * Box tetap `artSize + 20.dp` (rasio 300/280 lama dipertahankan persis). */
@Composable
internal fun AlbumArtHero(
    artworkUri: Uri?,
    accentColor: Color,
    artSize: Dp,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var totalDrag by remember { mutableFloatStateOf(0f) }
    // Batch 178 — Now Playing item 10/11 fix: swipe-to-skip di sini sebelumnya 0 feedback
    // visual selama drag berlangsung (cuma haptic SEKALI di dragEnd kalau lolos threshold
    // 120px) — beda dari gesture brightness/volume di Box induk (GestureIndicatorBadge
    // muncul LIVE mengikuti drag). User tidak tahu sudah "cukup jauh" menggeser sampai
    // jarinya dilepas. `dragOffset` bikin art ikut bergeser mengikuti jari (clamp ±48dp,
    // damped 0.5x — bukan 1:1, supaya tidak terkesan bisa diseret jauh tak terbatas) lalu
    // spring balik ke tengah begitu jari dilepas/gesture dibatalkan. Threshold/logic
    // swipe-next/prev itu sendiri (totalDrag, 120px) SAMA SEKALI TIDAK DIUBAH — murni layer
    // visual tambahan di atasnya, bukan perubahan playback/navigation logic.
    // Batch 434 — User laporan: "effect bounce juga masih stuttering, belum smooth like
    // butter!!". SAMA KELAS BUG dgn Batch 433 (`ui/theme/IosScrollPhysics.kt`), file BEDA:
    // `onHorizontalDrag` di bawah sebelumnya menulis posisi lewat `dragScope.launch {
    // dragOffset.snapTo(...) }` di SETIAP delta drag. `Animatable.snapTo` cuma suspend (dijaga
    // `MutatorMutex` internal) — tiap delta bikin coroutine baru lewat `launch` alih-alih
    // menulis nilai langsung, dan coroutine-coroutine ini bisa menumpuk/tidak berurutan saat
    // drag cepat (persis root cause Batch 433). Efeknya RANGKAP di sini dibanding kasus
    // scroll: springback `onDragEnd`/`onDragCancel` (`animateTo`) masuk `MutatorMutex` queue
    // YANG SAMA dgn `snapTo` — kalau masih ada `snapTo` lama menumpuk pas jari lepas, pegas
    // balik ("bounce") baru mulai SETELAH semuanya itu beres (bukan seketika jari lepas),
    // persis gejala "belum smooth like butter" yang dilaporkan.
    //
    // Fix: pola identik Batch 433 — `dragOffsetPx` (`MutableFloatState` polos) jadi sumber
    // kebenaran SINKRON yang dibaca `graphicsLayer` (ditulis LANGSUNG, 0 coroutine, dari
    // `onHorizontalDrag`). `dragOffset` (`Animatable`) TETAP ADA, sekarang HANYA dipakai di
    // fase springback (`onDragEnd`/`onDragCancel`, sudah suspend by design) — `snapTo` posisi
    // drag TERAKHIR dulu (titik AWAL pegas balik = posisi jari terakhir yang benar), lalu tiap
    // frame animasinya disinkronkan balik ke `dragOffsetPx` lewat parameter `block` resmi
    // `Animatable.animateTo`. `onDragStart` baru panggil `dragOffset.stop()` — jaring pengaman
    // supaya kalau user mulai drag BARU sementara springback drag SEBELUMNYA masih jalan,
    // `block` lama itu berhenti menimpa `dragOffsetPx` (perilaku ini otomatis didapat GRATIS
    // sebelum fix ini krn semua tulisan lewat 1 `Animatable`/`MutatorMutex` yang sama —
    // konsekuensi wajib dipulihkan manual sekarang krn jalur drag aktif sudah lepas dari
    // Animatable itu). `totalDrag`/threshold 120px/haptic/`dampingRatio`/`stiffness` (Batch 256)
    // TIDAK disentuh — sumbu bug ini murni SINKRON vs ASINKRON penulisan offset, bukan
    // parameter gesture/pegasnya.
    //
    // **Belum ditest di device asli** (tidak ada env Android nyata/device fisik di sesi ini) —
    // perlu konfirmasi: drag horizontal cepat berulang (swipe next/prev), springback di
    // dragEnd/dragCancel, dan drag baru yang menyusul cepat sebelum springback lama selesai —
    // semuanya diharapkan 0 regresi ke threshold/haptic/karakter pegas Batch 178/256 yang sudah
    // disetujui user.
    val dragOffsetPx = remember { mutableFloatStateOf(0f) }
    val dragOffset = remember { Animatable(0f) }
    val dragScope = rememberCoroutineScope()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer { translationX = dragOffsetPx.floatValue }
            .pointerInput(Unit) {
            val maxOffsetPx = 48.dp.toPx()
            // Batch 256 — POLISH_AUDIT §Motion: stiffness = Spring.StiffnessLow ditambah ke 2
            // spring snap-back di bawah (onDragEnd + onDragCancel), dulu default (Medium),
            // beda dari bouncyPress (Utils.kt) & entrance spring (baris ~410) yg sama-sama pakai
            // StiffnessLow eksplisit — biar swipe-snap terasa 1 sistem sama animasi bouncy lain
            // di screen ini, bukan 2 "rasa" beda. dampingRatio (MediumBouncy) tidak diubah.
            detectHorizontalDragGestures(
                onDragStart = {
                    totalDrag = 0f
                    // Batch 434 — hentikan springback lama (kalau masih jalan) supaya block-nya
                    // stop menimpa dragOffsetPx; lihat catatan Batch 434 di atas fungsi ini.
                    dragScope.launch { dragOffset.stop() }
                },
                onDragEnd = {
                    if (totalDrag < -120f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwipeNext()
                    } else if (totalDrag > 120f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwipePrevious()
                    }
                    dragScope.launch {
                        dragOffset.snapTo(dragOffsetPx.floatValue)
                        dragOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) {
                            dragOffsetPx.floatValue = value
                        }
                    }
                },
                onDragCancel = {
                    dragScope.launch {
                        dragOffset.snapTo(dragOffsetPx.floatValue)
                        dragOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) {
                            dragOffsetPx.floatValue = value
                        }
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    totalDrag += dragAmount
                    change.consume()
                    dragOffsetPx.floatValue = (totalDrag * 0.5f).coerceIn(-maxOffsetPx, maxOffsetPx)
                }
            )
        }
    ) {
        val isTactile = isTactileTheme()
        val isSkeu = isSkeuTheme()
        val isPanelTheme = isTactile || isSkeu
        // CRT scanlines Calm Retro tetap dipasang SETELAH clip (terkurung di dalam bentuk art).
        val isCalmRetroHero = isCalmRetroTheme()
        val isDark = LocalIsDarkTheme.current
        // Batch 347 — sudut piringan proporsional thd ukuran art (baseline 280dp; `Radius.hero`
        // global TIDAK diubah). Cabang Tactile/Skeu tetap `MaterialTheme.shapes.large`.
        val heroCornerRadius = Radius.hero * (artSize / 280.dp)
        val heroShape = if (isPanelTheme) MaterialTheme.shapes.large else RoundedCornerShape(heroCornerRadius)
        // Batch 560 — user (screenshot): "album tidak menampilkan effect depth sama sekali saat
        // tidak ada kontak". Dulu hero hanya bayangan jatuh tipis (neuCastOnly + .shadow) -> datar
        // saat diam. Kini SEMUA tema: PELAT TIMBUL (`neuRowTile`: Tactile -> tactileEmboss,
        // Neumorphism -> skeuEmboss, lainnya -> neuSurface; permukaan + bevel + bayangan Gaussian
        // 3 lapis) berisi ART CEKUNG (bingkai `frameInset`, bayangan dalam kiri-atas gelap /
        // kanan-bawah terang). Ukuran luar pelat = `artSize` (layout NowPlaying tak berubah); art
        // di dalam mengecil sebesar 2x bingkai.
        val frameInset = (artSize * 0.045f).coerceIn(8.dp, 14.dp)
        val innerShape = if (isPanelTheme) MaterialTheme.shapes.medium
        else RoundedCornerShape((heroCornerRadius - frameInset).coerceAtLeast(8.dp))
        // Glow aksen di belakang. Batch 560 — fix: `blur()` default `BlurredEdgeTreatment.Rectangle`
        // memotong glow keras di tepi Box -> tampak KOTAK lebih terang di belakang art (terlihat
        // di screenshot user). `Unbounded` membiarkan blur meluruh halus keluar tepi.
        Box(
            modifier = Modifier
                .size(artSize + 20.dp)
                .blur(90.dp, BlurredEdgeTreatment.Unbounded)
                .background(accentColor.copy(alpha = 0.38f), CircleShape)
        )
        // Batch 562 — user: "Album hero masih kurang berasa realistic depth nya saat idle". Statis (0
        // animasi/sensor/loop: aman baterai), 4 petunjuk kedalaman TAMBAHAN di atas Batch 560:
        // (1) bayangan LANTAI = elips gelap tepat di bawah pelat (benda tampak melayang, bukan menempel);
        // (2) pelat naik 14 -> 16dp (skala bayangan mesin maks 2x) + cahaya pantul warna aksen di bingkai;
        // (3) art ditutup "kaca": kilau diagonal + catchlight tepi atas + vignette sudut (lengkung cembung);
        // (4) bayangan bibir bingkai pada art dipertebal (band 10 -> 14dp). Revert = `SONIX_v561.zip`.
        Box(
            modifier = Modifier
                .size(artSize)
                .drawBehind {
                    val cx = size.width / 2f
                    val cy = size.height + 10.dp.toPx()
                    val r = size.width * 0.50f
                    withTransform({ scale(1f, 0.13f, pivot = Offset(cx, cy)) }) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                0f to Color.Black.copy(alpha = if (isDark) 0.70f else 0.34f),
                                0.55f to Color.Black.copy(alpha = if (isDark) 0.32f else 0.14f),
                                1f to Color.Black.copy(alpha = 0f),
                                center = Offset(cx, cy),
                                radius = r
                            ),
                            radius = r,
                            center = Offset(cx, cy)
                        )
                    }
                }
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(artSize)
                .neuRowTile(shape = heroShape, elevation = 16.dp)
                // Cahaya pantul aksen pada bingkai (ter-clip ke bentuk pelat; art menutupi tengah).
                .clip(heroShape)
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            0.70f to accentColor.copy(alpha = 0f),
                            1f to accentColor.copy(alpha = if (isDark) 0.34f else 0.24f),
                            center = center,
                            radius = size.width * 0.55f
                        )
                    )
                }
        ) {
            AlbumArt(
                artworkUri = artworkUri,
                modifier = Modifier
                    .size(artSize - frameInset * 2)
                    .clip(innerShape)
                    .then(if (isCalmRetroHero) Modifier.calmScanlines() else Modifier)
                    .drawWithContent {
                        drawContent()
                        val w = size.width
                        val h = size.height
                        // Batch 562 — vignette sudut (lengkung cembung kaca): terang di sisi cahaya
                        // kiri-atas, menggelap ke sudut kanan-bawah.
                        drawRect(
                            brush = Brush.radialGradient(
                                0.60f to Color.Black.copy(alpha = 0f),
                                1f to Color.Black.copy(alpha = if (isDark) 0.34f else 0.20f),
                                center = Offset(w * 0.42f, h * 0.38f),
                                radius = max(w, h) * 0.85f
                            )
                        )
                        // Sumur cekung di tepi art (bibir bingkai): gelap kiri-atas, terang kanan-bawah.
                        val band = 14.dp.toPx()
                        val dark = Color.Black.copy(alpha = if (isDark) 0.52f else 0.32f)
                        val light = Color.White.copy(alpha = if (isDark) 0.12f else 0.30f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(dark, Color.Transparent),
                                startY = 0f,
                                endY = band
                            ),
                            size = Size(size.width, band)
                        )
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(dark, Color.Transparent),
                                startX = 0f,
                                endX = band
                            ),
                            size = Size(band, size.height)
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, light),
                                startY = size.height - band,
                                endY = size.height
                            ),
                            topLeft = Offset(0f, size.height - band),
                            size = Size(size.width, band)
                        )
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, light),
                                startX = size.width - band,
                                endX = size.width
                            ),
                            topLeft = Offset(size.width - band, 0f),
                            size = Size(band, size.height)
                        )
                        // Batch 562 — kilau kaca: sapuan diagonal dari sudut kiri-atas (searah sumber cahaya).
                        drawRect(
                            brush = Brush.linearGradient(
                                0f to Color.White.copy(alpha = if (isDark) 0.16f else 0.26f),
                                0.34f to Color.White.copy(alpha = 0.05f),
                                0.52f to Color.White.copy(alpha = 0f),
                                start = Offset(0f, 0f),
                                end = Offset(w, h)
                            )
                        )
                        // Batch 562 — catchlight: garis terang tipis di tepi atas kaca.
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.White.copy(alpha = 0f),
                                0.20f to Color.White.copy(alpha = 0.55f),
                                0.80f to Color.White.copy(alpha = 0.30f),
                                1f to Color.White.copy(alpha = 0f)
                            ),
                            size = Size(w, 1.5.dp.toPx())
                        )
                        // Batch 566 — Neumorphism: FILLET gilt tipis di tepi art (seperti bingkai lukisan): garis emas diagonal
                        // terang -> redup -> terang, 2.5dp berpusat di tepi (separuh luar ter-clip `innerShape` = 1.25dp tampak),
                        // digambar PALING ATAS di atas kaca. Tema lain 0 perubahan.
                        if (isSkeu) {
                            val filletGilt = if (isDark) SkeuGilt else SkeuGiltDeep
                            drawOutline(
                                outline = innerShape.createOutline(size, layoutDirection, this),
                                brush = Brush.linearGradient(
                                    0.0f to filletGilt.copy(alpha = 0.90f),
                                    0.5f to filletGilt.copy(alpha = 0.18f),
                                    1.0f to filletGilt.copy(alpha = 0.60f),
                                    start = Offset(0f, 0f),
                                    end = Offset(w, h)
                                ),
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }
                    }
            )
        }
    }
}
