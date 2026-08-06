package com.rudi.audioplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.data.LyricsParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    rawLyrics: String?,
    positionMs: Long,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editing by remember { mutableStateOf(rawLyrics.isNullOrBlank()) }
    var draft by remember { mutableStateOf(rawLyrics.orEmpty()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass()
                .padding(horizontal = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Lirik",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (!editing) {
                    IconButton(onClick = { draft = rawLyrics.orEmpty(); editing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit lirik")
                    }
                    if (!rawLyrics.isNullOrBlank()) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus lirik")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                editing -> {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 360.dp),
                        placeholder = {
                            Text("Tempel lirik di sini. Untuk lirik sinkron, awali tiap baris dengan [mm:ss.xx]")
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {
                            if (rawLyrics.isNullOrBlank()) onDismiss() else editing = false
                        }) { Text("Batal") }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { onSave(draft.trim()); editing = false },
                            enabled = draft.isNotBlank()
                        ) { Text("Simpan") }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
                rawLyrics.isNullOrBlank() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Belum ada lirik untuk lagu ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { editing = true }) { Text("Tambah Lirik") }
                    }
                }
                else -> {
                    val lines = remember(rawLyrics) { LyricsParser.parse(rawLyrics) }
                    val synced = remember(lines) { LyricsParser.isSynced(lines) }
                    val activeIndex = if (synced) LyricsParser.currentLineIndex(lines, positionMs) else -1
                    val listState = rememberLazyListState()

                    LaunchedEffect(activeIndex) {
                        if (activeIndex >= 0) {
                            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                    ) {
                        itemsIndexed(lines, key = { index, _ -> index }) { index, line ->
                            val isActive = synced && index == activeIndex
                            Text(
                                line.text.ifBlank { "\u266A" },
                                style = if (isActive) {
                                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                },
                                color = when {
                                    isActive -> MaterialTheme.colorScheme.primary
                                    synced -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}
