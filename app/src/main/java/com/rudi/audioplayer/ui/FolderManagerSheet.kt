package com.rudi.audioplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.Song

data class FolderSummary(
    val path: String,
    val name: String,
    val songCount: Int,
    val excluded: Boolean
)

/**
 * Bottom sheet to scope which folders get scanned (e.g. turn off WhatsApp's
 * audio folder) and to bring back individually hidden songs. Purely a display
 * filter — never touches files on disk.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderManagerSheet(
    folders: List<FolderSummary>,
    hiddenSongs: List<Song>,
    onDismiss: () -> Unit,
    onToggleFolder: (String, Boolean) -> Unit,
    onUnhideSong: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Kelola Perpustakaan",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Matikan folder yang tidak ingin ditampilkan, misalnya folder audio WhatsApp.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (folders.isEmpty()) {
                Text(
                    "Belum ada folder terdeteksi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )
            }

            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(folders, key = { it.path }) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folder.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${folder.songCount} lagu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Switch(
                            checked = !folder.excluded,
                            onCheckedChange = { checked -> onToggleFolder(folder.path, !checked) }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }

                if (hiddenSongs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Lagu Disembunyikan",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(hiddenSongs, key = { it.id }) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    song.artist,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            TextButton(onClick = { onUnhideSong(song.id) }) {
                                Text("Tampilkan")
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}
