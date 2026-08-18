package com.rudi.audioplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.rudi.audioplayer.data.Bookmark as BookmarkModel
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.ui.theme.isCalmRetroTheme
import com.rudi.audioplayer.ui.theme.calmScanlines

/**
 * Roadmap #4 (`ROADMAP_15_FITUR_OFFLINE.md`), Batch 91 — combines two related "mark a moment
 * in this song" features behind one sheet entry ("Repeat A-B & Bookmark" in Kontrol Lanjutan):
 * A-B Repeat (loop between two points) and Bookmark Posisi (named jump points, persisted per
 * song). Kept in one file/sheet since both act on the same [positionMs] the user is currently
 * at and both are scoped to [songId] — splitting them into two separate sheet entries would
 * just mean opening a near-identical sheet twice for closely related actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ABRepeatBookmarkSheet(
    songId: Long,
    positionMs: Long,
    pointAMs: Long?,
    pointBMs: Long?,
    bookmarks: List<BookmarkModel>,
    onDismiss: () -> Unit,
    onSetPointA: (Long) -> Unit,
    onSetPointB: (Long) -> Unit,
    onClearAbRepeat: () -> Unit,
    onSeek: (Long) -> Unit,
    onAddBookmark: (String, Long) -> Unit,
    onDeleteBookmark: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    // Batch 137 — lanjutan spread Pilar A (calmScanlines) Batch 135, pola identik EqualizerSheet.
    val isCalmRetro = isCalmRetroTheme()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass()
                .then(
                    if (isCalmRetro) Modifier.clip(MaterialTheme.shapes.large).calmScanlines() else Modifier
                )
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Repeat A-B",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                when {
                    pointAMs != null && pointBMs != null && pointBMs > pointAMs ->
                        "Aktif — mengulang ${formatDuration(pointAMs)} ↔ ${formatDuration(pointBMs)}"
                    pointAMs != null || pointBMs != null ->
                        "Belum aktif — tandai kedua titik untuk mulai mengulang"
                    else -> "Tandai Titik A lalu Titik B untuk mengulang satu bagian lagu"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AbPointButton(
                    label = "Titik A",
                    value = pointAMs,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSetPointA(positionMs)
                    }
                )
                AbPointButton(
                    label = "Titik B",
                    value = pointBMs,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSetPointB(positionMs)
                    }
                )
            }

            if (pointAMs != null || pointBMs != null) {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClearAbRepeat()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hapus Repeat A-B")
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Bookmark Posisi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showAddBookmarkDialog = true
                }) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tandai")
                }
            }

            if (bookmarks.isEmpty()) {
                Text(
                    "Belum ada bookmark untuk lagu ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        BookmarkRow(
                            bookmark = bookmark,
                            onJump = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSeek(bookmark.positionMs)
                                onDismiss()
                            },
                            onDelete = { onDeleteBookmark(bookmark.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAddBookmarkDialog) {
        AddBookmarkDialog(
            suggestedLabel = "Tanda ${formatDuration(positionMs)}",
            onDismiss = { showAddBookmarkDialog = false },
            onConfirm = { label ->
                onAddBookmark(label.ifBlank { "Tanda ${formatDuration(positionMs)}" }, positionMs)
                showAddBookmarkDialog = false
            }
        )
    }
}

@Composable
private fun AbPointButton(label: String, value: Long?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    OutlinedButton(onClick = onClick, interactionSource = interaction, modifier = modifier.bouncyPress(interaction)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                value?.let { formatDuration(it) } ?: "Tandai",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BookmarkRow(bookmark: BookmarkModel, onJump: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onJump)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(bookmark.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                formatDuration(bookmark.positionMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        val deleteInteraction = remember { MutableInteractionSource() }
        IconButton(
            onClick = onDelete,
            interactionSource = deleteInteraction,
            modifier = Modifier.bouncyPress(deleteInteraction, pressedScale = 0.8f)
        ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus bookmark \"${bookmark.label}\"")
        }
    }
}

/** Self-contained rename/label dialog — deliberately not shared with `PlaylistScreen`'s private
 *  `TextInputDialog` (different file, would require exporting it) since this is the only other
 *  call site; same simple pattern (label + Batal/Simpan), kept local. */
@Composable
private fun AddBookmarkDialog(suggestedLabel: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(suggestedLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.BookmarkAdd, contentDescription = null) },
        title = { Text("Tandai Posisi Ini") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Nama bookmark") },
                placeholder = { Text(suggestedLabel) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Batal")
            }
        }
    )
}
