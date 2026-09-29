package com.rudi.audioplayer.ui

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
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
import com.rudi.audioplayer.ui.theme.TactileHighlight
import com.rudi.audioplayer.ui.theme.TactileShadow
import com.rudi.audioplayer.ui.theme.TactileLightHighlight
import com.rudi.audioplayer.ui.theme.TactileLightShadow
import com.rudi.audioplayer.ui.theme.SkeuAmbientOcclusion
import com.rudi.audioplayer.ui.theme.SkeuHighlight
import com.rudi.audioplayer.ui.theme.SkeuShadow
import com.rudi.audioplayer.ui.theme.SkeuSpecular
import com.rudi.audioplayer.ui.theme.SkeuEmerald
import com.rudi.audioplayer.ui.theme.SkeuLightEmerald
import com.rudi.audioplayer.ui.theme.SkeuLightAmbientOcclusion
import com.rudi.audioplayer.ui.theme.SkeuLightHighlight
import com.rudi.audioplayer.ui.theme.SkeuLightShadow
import com.rudi.audioplayer.ui.theme.SkeuLightSpecular
import com.rudi.audioplayer.ui.theme.LocalIsDarkTheme
import com.rudi.audioplayer.ui.theme.Radius
import kotlinx.coroutines.launch

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
    var totalDrag by remember { mutableStateOf(0f) }
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
        // Batch 59 — same gap as HomeScreen/LibraryScreen/MiniPlayerBar: this hero art (the
        // single largest, most-looked-at surface on the whole screen) was Tactile-only, Skeu
        // fell into the generic Apple shadow-only branch below with no bevel of its own at all.
        val isSkeu = isSkeuTheme()
        val isPanelTheme = isTactile || isSkeu
        // v3 upgrade — Pilar A spec palet_warna_calm_retro_v3.md (CRT scanlines), dipasang di
        // bawah lewat .calmScanlines() SETELAH .clip(heroShape) (bukan sebelum, beda dari
        // teknik shadow Tactile/Skeu di atas yang sengaja bocor sebelum clip) — scanline harus
        // terkurung rapi di dalam bentuk album art, tidak boleh meluber ke luar shape.
        val isCalmRetroHero = isCalmRetroTheme()
        // Batch 74 — fix: this manual draw (unlike skeuEmboss()/tactileEmboss(), which both
        // already branch on LocalIsDarkTheme) hardcoded dark-only tokens (TactileHighlight/
        // TactileShadow, SkeuHighlight/SkeuShadow/SkeuAmbientOcclusion/SkeuSpecular/
        // SkeuInnerGroove) with no light-mode branch at all — since Batch 61 made Tactile/Skeu
        // fully autonomous per light/dark mode, this hero art (the single largest surface on
        // the whole screen) has been silently drawing dark bevel colors over a light-mode panel
        // this whole time. Fixed below via isDark + light-token fallback, matching the pattern
        // skeuEmboss()/tactileEmboss() already use.
        val isDark = LocalIsDarkTheme.current
        // Batch 52: recolored again for the literal Midnight Blue spec
        // (compose-skeuomorphism-lite-midnight-blue.md) — same drawn top-down shadow +
        // vertical-gradient bevel border technique kept from Batch 45/46/49-51, no code changes
        // here at all; TactileHighlight/TactileShadow are plain white/black-based again this
        // batch (see Color.kt), so this hero art picks up the new palette automatically through
        // those same two token references.
        // Batch 347 — user pilih lanjut sempurnakan trade-off yang sengaja ditunda Batch 346
        // ("Radius.hero ikut skala"). Baseline referensi TETAP 280dp (konsisten dgn konvensi
        // `dynamicArtSize` Batch 346 yang sengaja balik ke 280dp persis di layar 360dp lebar) —
        // rasio artSize aktual thd baseline ini dikalikan ke `Radius.hero` (28dp, Spacing.kt)
        // supaya sudut piringan tetap PROPORSIONAL secara visual di ukuran manapun (piringan
        // besar = sudut ikut besar, piringan kecil = sudut ikut kecil), bukan radius absolut
        // tetap yang terlihat makin "tajam"/kurang membulat relatif saat piringan membesar (atau
        // sebaliknya berlebihan membulat saat mengecil). `Dp.div(Dp): Float` & `Dp.times(Float):
        // Dp` — dicek ulang lewat dokumentasi resmi Compose sebelum dipakai (operator baku kelas
        // `Dp`, bukan API custom) — `Dp * Float` sendiri sudah ada presedennya di file ini
        // (`screenHeightDp * 0.28f`, baris `albumArtBoxHeight`).
        // Token `Radius.hero` GLOBAL ITU SENDIRI (Spacing.kt) TIDAK disentuh — dipakai HANYA sbg
        // nilai baseline di sini, bukan diubah jadi dinamis (token itu dipakai juga di Theme.kt
        // utk `MaterialTheme.shapes.large`, dampak globalnya jauh di luar 1 layar ini).
        // Cabang Tactile/Skeu (`isPanelTheme -> MaterialTheme.shapes.large`) SENGAJA TIDAK ikut
        // diskalakan — 2 identitas itu memang didesain pakai bahasa sudut SERAGAM lintas berbagai
        // ukuran permukaan (panel/sheet/kartu lain di app ini semua pakai radius theme yang sama,
        // bukan proporsional per-objek); mengikutkan hero art di sini justru bikin hero beda
        // sendiri dari permukaan besar lain di identitas yang sama — kebalikan dari konsistensi
        // yang justru diinginkan bahasa desain panel itu. Scope PERSIS sesuai literal yang
        // dikonfirmasi user: "Radius.hero ikut skala" — token itu spesifik cuma dipakai di cabang
        // non-panel (Apple/default) ini.
        val heroCornerRadius = Radius.hero * (artSize / 280.dp)
        val heroShape = if (isPanelTheme) MaterialTheme.shapes.large else RoundedCornerShape(heroCornerRadius)
        Box(
            modifier = Modifier
                .size(artSize + 20.dp)
                .blur(90.dp)
                .background(accentColor.copy(alpha = 0.38f), CircleShape)
        )
        AlbumArt(
            artworkUri = artworkUri,
            modifier = Modifier
                .size(artSize)
                .then(
                    when {
                        isTactile -> {
                            val heroHighlight = if (isDark) TactileHighlight else TactileLightHighlight
                            val heroShadow = if (isDark) TactileShadow else TactileLightShadow
                            Modifier
                                .drawBehind {
                                    val outline = heroShape.createOutline(size, layoutDirection, this)
                                    val outlinePath = Path().apply { addOutline(outline) }
                                    translate(top = 9.dp.toPx()) {
                                        drawPath(outlinePath, color = heroShadow.copy(alpha = if (isDark) 0.55f else 0.30f))
                                    }
                                }
                                .clip(heroShape)
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        // Batch 55 — was verticalGradient, the one remaining spot in the
                                        // whole Tactile identity still drawing top-down light instead of
                                        // spec §9's diagonal top-left -> bottom-right (BlurUtils.kt's
                                        // edgeBrush and TactileDepth.kt's tactileEmboss() border both
                                        // already use linearGradient's default diagonal — this hero art
                                        // border was the one inconsistent leftover from Batch 45/46,
                                        // predating the diagonal rule adopted in Batch 53).
                                        Brush.linearGradient(
                                            listOf(
                                                heroHighlight.copy(alpha = if (isDark) 0.12f else heroHighlight.alpha),
                                                heroShadow.copy(alpha = if (isDark) 0.32f else heroShadow.alpha)
                                            )
                                        )
                                    ),
                                    heroShape
                                )
                                // Localized accent glow on the hero art is spec-sanctioned (§9: "Use
                                // [glow] for… selected states… important tactile edges") since this
                                // is the one always-active/selected surface on the whole screen —
                                // alpha trimmed from the old 0.5f for restraint per §9/§13.
                                .shadow(elevation = 18.dp, shape = heroShape, spotColor = accentColor.copy(alpha = 0.42f))
                        }
                        isSkeu -> {
                            // Batch 79 — NEUMORPHISM upgrade: sama arsitektur dual-shadow dgn
                            // skeuEmboss() (TactileDepth.kt, Batch 79) — sisi gelap kanan-bawah
                            // (AO dekat + shadow jauh, 3 layer offset+alpha bertingkat), sisi
                            // terang kiri-atas (specular dekat + highlight jauh, 2 layer) — TIDAK
                            // ADA lagi border/inner-groove sama sekali (neumorphism generik tidak
                            // punya garis batas, kedalaman murni dari bayangan). Manual draw di
                            // sini (bukan lewat skeuEmboss() langsung) tetap dipertahankan karena
                            // Box ini juga membawa .shadow() accent glow per-lagu di bawah, yang
                            // perlu tetap jadi layer terpisah/paling akhir.
                            // Batch 81 — fix 2 hal: (1) sisi TERANG dulu ada di drawBehind TERPISAH
                            // SETELAH .clip(heroShape) (beda dari sisi gelap yg SEBELUM .clip()) —
                            // artinya sisi terang selama ini kepotong tepat di tepi shape, tidak
                            // pernah benar-benar "meluber ke luar" sebagai bayangan lembut kayak
                            // sisi gelap, beda arsitektur dari skeuEmboss() sendiri yg gambar KEDUA
                            // sisi dalam 1 drawBehind sebelum .clip(). Disatukan di bawah, sama
                            // pola dgn skeuEmboss(). (2) clipRect() halo ditambahkan (fix "Ambient
                            // Light gak bocor", instruksi user yg belum tersentuh Batch 79/80) —
                            // hero art ini panel TERBESAR di app, jadi juga yg paling berisiko
                            // numpang-nimpa MiniPlayerBar/tombol kontrol di bawahnya kalau tidak
                            // dibatasi.
                            val heroAo = if (isDark) SkeuAmbientOcclusion else SkeuLightAmbientOcclusion
                            val heroShadow = if (isDark) SkeuShadow else SkeuLightShadow
                            val heroSpecular = if (isDark) SkeuSpecular else SkeuLightSpecular
                            val heroHighlight = if (isDark) SkeuHighlight else SkeuLightHighlight
                            val emerald = if (isDark) SkeuEmerald else SkeuLightEmerald
                            // Batch 80 — fix: Batch 79's emerald di hero art cuma lerp-blend 14%
                            // ke arah heroSpecular (putih/perak nyaris opaque) — di layar HP nyaris
                            // tak berubah dari putih polos (user: "yang kelihatan cuman Titanium
                            // dominan, mana zamrudnya??"). Sekarang jadi radial glint TERPISAH
                            // (warna emerald murni, bukan campuran) di pojok kiri-atas, alpha tetap
                            // & jauh lebih tinggi (0.35f/0.42f) — permanen (hero art statis, tidak
                            // ada state pressed spt skeuEmboss()), genuinely kebaca sebagai titik
                            // hijau di logam titanium, bukan cuma teknis-ada-di-kode.
                            val heroEmeraldAlpha = if (isDark) 0.35f else 0.42f
                            Modifier
                                .drawBehind {
                                    val outline = heroShape.createOutline(size, layoutDirection, this)
                                    val outlinePath = Path().apply { addOutline(outline) }
                                    // Halo tetap (18.dp) — offset terjauh yg dipakai di bawah cuma
                                    // 14.dp (literal, bukan proporsional ke param elevation kayak
                                    // skeuEmboss()). Batch 346 — hero art ini TIDAK LAGI selalu
                                    // 280.dp (sekarang `artSize` dinamis, lihat definisi fungsi) —
                                    // TAPI margin halo 18dp SENGAJA tetap literal, bukan diikutkan
                                    // skala: ini jarak bayangan-ke-tepi-shape yang wajar konstan
                                    // di seluruh rentang ukuran (140dp s/d lebar layar), bukan
                                    // proporsi visual yang perlu ikut membesar/mengecil bareng art.
                                    // 18dp tetap cukup longgar utk tidak memotong bentuk bayangan
                                    // sendiri di ukuran manapun, sekaligus batas tegas yg dijamin
                                    // tidak dilewati.
                                    val haloPx = 18.dp.toPx()
                                    clipRect(
                                        left = -haloPx,
                                        top = -haloPx,
                                        right = size.width + haloPx,
                                        bottom = size.height + haloPx
                                    ) {
                                        // Sisi GELAP — kanan-bawah, 3 layer offset makin jauh +
                                        // alpha makin tipis (faux-blur bertingkat, sama teknik
                                        // skeuEmboss()).
                                        translate(left = 3.dp.toPx(), top = 3.dp.toPx()) {
                                            drawPath(outlinePath, color = heroAo)
                                        }
                                        translate(left = 8.dp.toPx(), top = 8.dp.toPx()) {
                                            drawPath(outlinePath, color = heroShadow.copy(alpha = (if (isDark) 0.40f else heroShadow.alpha) * 0.75f))
                                        }
                                        translate(left = 14.dp.toPx(), top = 14.dp.toPx()) {
                                            drawPath(outlinePath, color = heroShadow.copy(alpha = (if (isDark) 0.40f else heroShadow.alpha) * 0.35f))
                                        }
                                        // Sisi TERANG — kiri-atas, 2 layer, murni Titanium/Silver.
                                        // Batch 81: dipindah ke sini (sebelum .clip()), sisi gelap
                                        // di atas — dulu di drawBehind terpisah SETELAH .clip(),
                                        // jadi tak pernah bisa meluber sama sekali (lihat komentar
                                        // Batch 81 di atas).
                                        translate(left = -3.dp.toPx(), top = -3.dp.toPx()) {
                                            drawPath(outlinePath, color = heroSpecular.copy(alpha = if (isDark) 0.35f else heroSpecular.alpha * 0.6f))
                                        }
                                        translate(left = -8.dp.toPx(), top = -8.dp.toPx()) {
                                            drawPath(outlinePath, color = heroHighlight.copy(alpha = (if (isDark) 0.16f else heroHighlight.alpha) * 0.7f))
                                        }
                                    }
                                }
                                .clip(heroShape)
                                .drawBehind {
                                    // Zamrud — glint bulat kecil terpisah, pojok kiri-atas, warna
                                    // murni (bukan blend) supaya genuinely kebaca hijau. Sengaja
                                    // tetap SETELAH .clip() (beda dari dual-shadow di atas) — ini
                                    // permata di PERMUKAAN panel, bukan bayangan yg perlu meluber.
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(emerald.copy(alpha = heroEmeraldAlpha), Color.Transparent),
                                            center = Offset(size.width * 0.16f, size.height * 0.14f),
                                            radius = size.minDimension.coerceAtLeast(1f) * 0.28f
                                        )
                                    )
                                }
                                .shadow(elevation = 18.dp, shape = heroShape, spotColor = accentColor.copy(alpha = 0.42f))
                        }
                        else -> Modifier.shadow(elevation = 28.dp, shape = heroShape, spotColor = accentColor.copy(alpha = 0.45f))
                    }
                )
                .clip(heroShape)
                .then(if (isCalmRetroHero) Modifier.calmScanlines() else Modifier)
        )
    }
}
