package com.rudi.audioplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.data.LrcSyncEditor
import com.rudi.audioplayer.data.LyricsParser
import com.rudi.audioplayer.data.SyncSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    rawLyrics: String?,
    positionMs: Long,
    isPlaying: Boolean = false,
    onPlayPause: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Batch 82 debug fix — keyed on `rawLyrics` (was bare `remember`, 0 key). This sheet stays
    // mounted across a track change if the song advances (headset button, notification, widget —
    // all bypass this screen entirely) while the sheet is still open: `showLyricsSheet` has no
    // reason to flip false just because the underlying song did. Unkeyed `remember` meant
    // `editing`/`draft` kept whatever the PREVIOUS song left behind — worst case, an unsaved
    // draft typed for song A silently overwrote song B's real lyrics if "Simpan" was tapped right
    // after the auto-advance. Keying on `rawLyrics` (the exact prop these two derive from) makes
    // both re-derive fresh every time the sheet is showing a different song's lyrics, discarding
    // any unsaved draft along with it — losing an un-saved edit on track change is much safer
    // than mis-attributing it to the wrong song.
    var editing by remember(rawLyrics) { mutableStateOf(rawLyrics.isNullOrBlank()) }
    var draft by remember(rawLyrics) { mutableStateOf(rawLyrics.orEmpty()) }
    // Roadmap #3 — Tap-to-Sync: null = mode edit teks biasa, non-null = sedang di flow sync
    // baris-per-baris. Sama alasan `editing`/`draft` di atas, dikey ke `rawLyrics` — ganti lagu
    // selagi sheet terbuka membatalkan sesi sync yang sedang jalan (masuk akal, timestamp yang
    // sedang direkam memang scoped ke lagu yang sedang diputar saat itu).
    var syncSession by remember(rawLyrics) { mutableStateOf<SyncSession?>(null) }

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
                editing && syncSession != null -> {
                    val session = syncSession!!
                    Text(
                        "Sinkronisasi Lirik \u2014 baris ${(session.currentIndex + 1).coerceAtMost(session.lines.size)}/${session.lines.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        session.currentLine ?: "\u2713 Semua baris sudah diproses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = onPlayPause) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar"
                            )
                        }
                        Text(formatDuration(positionMs), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val next = LrcSyncEditor.mark(session, positionMs)
                            if (next.isComplete) {
                                draft = LrcSyncEditor.buildLrcText(next)
                                syncSession = null
                            } else {
                                syncSession = next
                            }
                        },
                        enabled = !session.isComplete,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Tandai Sekarang") }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { syncSession = LrcSyncEditor.undo(session) },
                            enabled = session.currentIndex > 0
                        ) { Text("Mundur") }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                val next = LrcSyncEditor.skip(session)
                                if (next.isComplete) {
                                    draft = LrcSyncEditor.buildLrcText(next)
                                    syncSession = null
                                } else {
                                    syncSession = next
                                }
                            },
                            enabled = !session.isComplete
                        ) { Text("Lewati Baris") }
                    }
                    TextButton(
                        onClick = { syncSession = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Batal, Kembali ke Teks") }
                    Spacer(modifier = Modifier.height(12.dp))
                }
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
                    Spacer(modifier = Modifier.height(8.dp))
                    if (LrcSyncEditor.splitPlainLines(draft).isNotEmpty()) {
                        TextButton(
                            onClick = { syncSession = LrcSyncEditor.startSession(draft) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Mode Tap-to-Sync (LRC)")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
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
