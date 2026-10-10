package com.rudi.audioplayer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.ui.theme.LocalIsDarkTheme
import com.rudi.audioplayer.ui.theme.isSkeuTheme
import com.rudi.audioplayer.ui.theme.neuSlidingKey
import com.rudi.audioplayer.ui.theme.neuTrough

// Batch 563 — SAKLAR BERKEDALAMAN, SEMUA 6 tema. Akar bug "saklar tidak kebagian sumur/effect depth
// sama sekali": `androidx.compose.material3.Switch` menggambar track + thumb datar sendiri, tidak lewat
// mesin kedalaman `TactileDepth.kt` yang sudah dipasang ke tile/tab/tombol (Batch 551-562).
//
// Fungsi ini SENGAJA bernama `Switch` di package `com.rudi.audioplayer.ui`: semua pemanggil (7 file di
// package ini) mengimpor `androidx.compose.material3.*` (star import) dan TIDAK mengimpor `Switch`
// secara eksplisit; urutan resolusi Kotlin = impor eksplisit > package yang sama > star import, jadi
// deklarasi ini yang terpilih tanpa menyentuh 14 titik panggil. Pemanggil yang memakai parameter yang
// tidak ada di sini (`colors`, `thumbContent`, `interactionSource`) jatuh balik ke `Switch` M3 (tak
// terjadi hari ini — grep 14/14 titik panggil hanya memakai `checked`, `onCheckedChange`, `enabled`).
//
// Rancangan = bahasa visual bilah tab (Batch 555): PALUNG cekung permanen (`neuTrough`) + SATU kunci
// timbul yang meluncur (`neuSlidingKey`). Status ON = lantai palung bertinta `primary` (di-crossfade
// lewat DUA lapis palung statis: bitmap depth hanya 2 entri cache, tak ada render blur per frame —
// bandingkan `neuAccentKey` yang digambar langsung karena warnanya beranimasi). Bentuk: Neumorphism =
// persegi membulat "tailored" (track 9dp, kunci 7dp); tema lain = pil + kunci bulat.
// Tekan = `LocalIndication` (`NeuDepthIndication`, Batch 560): seluruh saklar terbenam.
// NOT VERIFIED: belum dikompilasi/dilihat di device; alpha/tinta ON = simulasi statis.

private val SwitchTrackWidth = 52.dp
private val SwitchTrackHeight = 30.dp
private val SwitchThumbSize = 22.dp
private val SwitchThumbGap = 4.dp

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = LocalIsDarkTheme.current
    val skeu = isSkeuTheme()
    val trackShape: Shape = if (skeu) RoundedCornerShape(9.dp) else CircleShape
    val thumbShape: Shape = if (skeu) RoundedCornerShape(7.dp) else CircleShape

    // Lantai palung: OFF = permukaan tema; ON = permukaan bertinta primary (gelap 55%, terang 80% supaya
    // lantai terang tetap terbaca sebagai burgundy/aksen, bukan rose pucat). Mesin menggelapkan lagi.
    val offFloor = scheme.surface
    val onFloor = lerp(scheme.surface, scheme.primary, if (isDark) 0.55f else 0.80f)
    // Kunci: Neumorphism gelap = secondary (champagne), selain itu terang netral. Selalu kontras dgn kedua lantai.
    val thumbBase = when {
        skeu && isDark -> scheme.secondary
        isDark -> lerp(scheme.surface, scheme.onSurface, 0.80f)
        else -> scheme.surfaceVariant
    }

    val progress = animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "neuSwitchProgress"
    )

    // Geometri kunci dlm satuan "slot" (count = 2 -> 1 slot = setengah lebar track; neuSlidingKey memakai
    // POSISI PUSAT kunci): jarak pinggir = SwitchThumbGap di kedua ujung, tinggi kunci = tinggi track - 2 x gap.
    val slot = SwitchTrackWidth / 2
    val centerOff = (SwitchThumbGap + SwitchThumbSize / 2) / slot
    val travel = (SwitchTrackWidth - SwitchThumbGap * 2 - SwitchThumbSize) / slot
    val widthFraction = SwitchThumbSize / slot
    val heightFraction = SwitchThumbSize / SwitchTrackHeight

    val interactionSource = remember { MutableInteractionSource() }
    val interactive = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onValueChange = onCheckedChange
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(if (onCheckedChange != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .size(SwitchTrackWidth, SwitchTrackHeight)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(trackShape)
            .then(interactive)
    ) {
        Box(Modifier.size(SwitchTrackWidth, SwitchTrackHeight).neuTrough(trackShape, offFloor, isDark))
        Box(
            Modifier
                .size(SwitchTrackWidth, SwitchTrackHeight)
                .graphicsLayer { alpha = progress.value }
                .neuTrough(trackShape, onFloor, isDark)
        )
        Box(
            Modifier
                .size(SwitchTrackWidth, SwitchTrackHeight)
                .neuSlidingKey(
                    shape = thumbShape,
                    base = thumbBase,
                    count = 2,
                    widthFraction = widthFraction,
                    heightFraction = heightFraction,
                    elevation = 3.dp,
                    isDark = isDark,
                    position = { centerOff + travel * progress.value }
                )
        )
    }
}
