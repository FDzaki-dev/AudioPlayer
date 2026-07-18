package com.rudi.audioplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.Song

/**
 * Bottom sheet showing the current playback queue. Lets the user jump to any
 * song, nudge items up/down to reorder, and remove songs they don't want anymore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Song>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onPlayIndex: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Antrean Putar (${queue.size} lagu)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (queue.isEmpty()) {
                Text(
                    text = "Antrean kosong.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(20.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                itemsIndexed(queue, key = { index, song -> "${song.id}_$index" }) { index, song ->
                    QueueRow(
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
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

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
    onRemove: () -> Unit
) {
    val background = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (isPlaying) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = "Sedang diputar",
                    tint = MaterialTheme.colorScheme.primary,
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
                overflow = TextOverflow.Ellipsis
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
