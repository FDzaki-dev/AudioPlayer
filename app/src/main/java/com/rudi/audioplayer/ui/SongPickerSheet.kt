package com.rudi.audioplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.Song
import com.rudi.audioplayer.ui.theme.frostedGlass

/**
 * Sheet pilih-banyak-lagu generik. Lahir dari laporan user (screenshot tab Favorit & Playlist
 * kosong): satu-satunya cara nambah lagu ke situ sebelumnya WAJIB muter ke tab Lagu dulu, cari
 * manual, baru tekan-lama → "Tambah ke Favorit/Playlist". Sheet ini dipanggil langsung dari FAB
 * di tab tujuan (Favorit/Playlist) — [alreadyAddedIds] otomatis disaring dari daftar biar user
 * gak checklist ulang yang udah ada. [onConfirm] dipanggil SEKALI dengan list id terpilih (bukan
 * per-toggle), biar pemanggil bebas nge-batch write-nya (mis. `onToggleFavorite` dipanggil N kali
 * dalam 1 loop, bukan N kali recomposition terpisah).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongPickerSheet(
    title: String,
    allSongs: List<Song>,
    alreadyAddedIds: Set<Long>,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<Long>()) }

    val candidates = remember(allSongs, alreadyAddedIds) {
        allSongs.filter { it.id !in alreadyAddedIds }
    }
    val filtered = remember(candidates, query) {
        if (query.isBlank()) candidates
        else candidates.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        Column(modifier = Modifier.fillMaxWidth().frostedGlass()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari lagu") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            when {
                candidates.isEmpty() -> EmptyState(
                    title = "Semua lagu sudah ditambahkan",
                    subtitle = "Tidak ada lagu tersisa untuk ditambahkan ke sini.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 20.dp)
                )
                filtered.isEmpty() -> EmptyState(
                    title = "Tidak ditemukan",
                    subtitle = "Coba kata kunci lain.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 20.dp)
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(filtered, key = { it.id }) { song ->
                        val isSelected = song.id in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (isSelected) selected - song.id else selected + song.id
                                }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium,
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
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Batal") }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(selected.toList())
                        onDismiss()
                    },
                    enabled = selected.isNotEmpty()
                ) {
                    Text("Tambah (${selected.size})")
                }
            }
        }
    }
}
