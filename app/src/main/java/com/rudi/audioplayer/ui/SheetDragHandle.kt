package com.rudi.audioplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Batch 732 — pelacak tarik-turun untuk MENUTUP sheet lewat handle. Murni (tanpa tipe Compose) agar teruji di JVM.
 *
 * Sheet TIDAK bergerak mengikuti jari (tak ada gerak = tak ada pantulan/"flickering" seperti Batch 729-730); tarikan hanya
 * diukur. [onDelta] mengembalikan true TEPAT SEKALI per tarikan, saat total gerak ke bawah mencapai [thresholdPx].
 * Gerak ke atas mengurangi total tetapi tak pernah di bawah 0 (jari yang mulai naik sedikit tak membuat tarikan ke bawah
 * berikutnya harus menempuh jarak ekstra).
 */
internal class DragDismissTracker(private val thresholdPx: Float) {
    private var totalPx = 0f
    private var fired = false

    /** Mulai tarikan baru: total dan status "sudah memicu" disetel ulang. */
    fun begin() {
        totalPx = 0f
        fired = false
    }

    /** [deltaY] > 0 = ke bawah. true = ambang baru saja tercapai (hanya sekali sampai [begin] dipanggil lagi). */
    fun onDelta(deltaY: Float): Boolean {
        if (fired) return false
        totalPx = (totalPx + deltaY).coerceAtLeast(0f)
        if (totalPx < thresholdPx) return false
        fired = true
        return true
    }
}

/** Jarak tarik ke bawah (sesudah touch slop) yang menutup sheet. */
private const val DRAG_DISMISS_THRESHOLD_DP = 32

/**
 * Batch 732 — handle sheet yang BERFUNGSI: tarik ke bawah >= [DRAG_DISMISS_THRESHOLD_DP] atau ketuk = [onDismiss].
 * Pengganti `BottomSheetDefaults.DragHandle()` untuk sheet yang `sheetGesturesEnabled = false` (Batch 731: handle bawaan
 * jadi pajangan — tarikan di handle tak melakukan apa pun). Area sentuh selebar sheet x 48 dp; ketukan tanpa
 * ripple/indikasi (indikasi di handle sebelumnya terlihat berkedip).
 */
@Composable
internal fun SheetDragHandle(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val thresholdPx = with(LocalDensity.current) { DRAG_DISMISS_THRESHOLD_DP.dp.toPx() }
    val tracker = remember(thresholdPx) { DragDismissTracker(thresholdPx) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(tracker) {
                detectVerticalDragGestures(
                    onDragStart = { tracker.begin() },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (tracker.onDelta(dragAmount)) currentOnDismiss()
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Tutup panel",
                onClick = { currentOnDismiss() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
        )
    }
}
