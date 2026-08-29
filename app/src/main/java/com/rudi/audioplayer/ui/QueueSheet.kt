package com.rudi.audioplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.ui.theme.isCalmRetroTheme
import com.rudi.audioplayer.ui.theme.calmScanlines
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rudi.audioplayer.data.Song

/**
 * Bottom sheet showing the current playback queue. Lets the user jump to any song, drag a
 * row (via its handle) or nudge it with the up/down buttons to reorder, and remove songs
 * they don't want anymore. Drag and the arrow buttons both call the same [onMove] — the
 * handle is just a faster path to the same result, not a replacement for the buttons, so
 * precise single-step reordering (or a TalkBack user) still has a reliable fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Song>,
    // Mirrors `queue` 1:1: slotIds[i] is the stable identity of queue[i]. It travels with
    // its song when the queue is reordered, unlike an index-based key which would instead
    // stay attached to the *position* — the difference between a row sliding to its new
    // spot versus every row below the moved one appearing to jump to a new song.
    slotIds: List<Long>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onPlayIndex: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    // A drag gesture runs across many frames inside its own coroutine, started once when the
    // finger goes down. By the time it's mid-drag, `queue`/`slotIds`/`onMove` may have already
    // been replaced by newer ones from recomposition (each reorder step recomposes this whole
    // sheet) — rememberUpdatedState keeps the gesture's callbacks reading the *current* values
    // instead of the stale ones captured when the drag started.
    val currentQueue by rememberUpdatedState(queue)
    val currentSlotIds by rememberUpdatedState(slotIds)
    val currentOnMove by rememberUpdatedState(onMove)

    var draggingSlotId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    var rowHeightPx by remember { mutableStateOf(0f) }
    // Batch 137 — lanjutan spread Pilar A (calmScanlines) Batch 135, pola identik EqualizerSheet.
    val isCalmRetro = isCalmRetroTheme()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass()
                .then(
                    if (isCalmRetro) Modifier.clip(MaterialTheme.shapes.large).calmScanlines() else Modifier
                )
        ) {
            Text(
                text = "Antrean Putar (${queue.size} lagu)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (queue.isEmpty()) {
                EmptyState(
                    title = "Antrean kosong",
                    subtitle = "Tambahkan lagu lewat \"Putar Berikutnya\" atau \"Tambah ke Antrean\".",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 20.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                itemsIndexed(
                    queue,
                    key = { index, _ -> slotIds.getOrElse(index) { index.toLong() } }
                ) { index, song ->
                    val slotId = slotIds.getOrElse(index) { index.toLong() }
                    val isDragging = slotId == draggingSlotId

                    QueueRow(
                        modifier = Modifier
                            .then(if (isDragging) Modifier else Modifier.animateItem())
                            .onGloballyPositioned { coordinates ->
                                if (rowHeightPx == 0f) rowHeightPx = coordinates.size.height.toFloat()
                            }
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetPx else 0f
                                shadowElevation = if (isDragging) 10f else 0f
                            }
                            .zIndex(if (isDragging) 1f else 0f),
                        song = song,
                        isPlaying = index == currentIndex,
                        canMoveUp = index > 0,
                        canMoveDown = index < queue.lastIndex,
                        canRemove = queue.size > 1,
                        onClick = { onPlayIndex(index) },
                        onMoveUp = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMove(index, index - 1)
                        },
                        onMoveDown = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMove(index, index + 1)
                        },
                        onRemove = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRemove(index)
                        },
                        dragHandleModifier = Modifier.pointerInputDragHandle(
                            slotId = slotId,
                            onDragStart = {
                                draggingSlotId = slotId
                                dragOffsetPx = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = {
                                draggingSlotId = null
                                dragOffsetPx = 0f
                            },
                            onDragDelta = { deltaY ->
                                val h = rowHeightPx
                                if (h > 0f) {
                                    dragOffsetPx += deltaY
                                    val fromIndex = currentSlotIds.indexOf(slotId)
                                    if (fromIndex >= 0) {
                                        if (dragOffsetPx > h / 2 && fromIndex < currentQueue.lastIndex) {
                                            currentOnMove(fromIndex, fromIndex + 1)
                                            dragOffsetPx -= h
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else if (dragOffsetPx < -h / 2 && fromIndex > 0) {
                                            currentOnMove(fromIndex, fromIndex - 1)
                                            dragOffsetPx += h
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }
                            }
                        )
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Long-press-then-drag on a dedicated handle only (never the whole row) so it can never hijack
 * the LazyColumn's own vertical scroll or the row's tap-to-play. `deltaY` is reported raw and
 * un-thresholded — the caller decides how many pixels constitute "moved one slot".
 */
private fun Modifier.pointerInputDragHandle(
    slotId: Long,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragDelta: (deltaY: Float) -> Unit
): Modifier = this.then(
    Modifier.pointerInput(slotId) {
        detectDragGesturesAfterLongPress(
            onDragStart = { onDragStart() },
            onDragEnd = { onDragEnd() },
            onDragCancel = { onDragEnd() },
            onDrag = { change, dragAmount ->
                change.consume()
                onDragDelta(dragAmount.y)
            }
        )
    }
)

@Composable
private fun QueueRow(
    song: Song,
    isPlaying: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val background = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = dragHandleModifier
                .size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Tahan lalu geser untuk mengurutkan ulang",
                tint = MaterialTheme.colorScheme.secondary
            )
        }

        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (isPlaying) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = "Sedang diputar",
                    // Batch 229 — Iconography 4/7 (action vs decorative icon). Badge murni
                    // status, 0 onClick — pakai `primary` (warna reserved utk icon actionable/
                    // tombol app ini) bikin ambigu seolah bisa di-tap. Baris drag-handle
                    // persis di atasnya (juga decorative) pakai `secondary` — samakan.
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Naikkan urutan",
                tint = if (canMoveUp) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Turunkan urutan",
                tint = if (canMoveDown) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            )
        }
        IconButton(onClick = onRemove, enabled = canRemove) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Hapus dari antrean",
                tint = if (canRemove) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            )
        }
    }
}
