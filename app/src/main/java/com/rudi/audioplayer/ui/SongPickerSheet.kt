package com.rudi.audioplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
 * per-toggle), biar pemanggil bebas nge-batch write-nya.
 *
 * Batch 268 — user laporan layar kurang luas & 0 sweep-select. Sheet diperbesar (hampir
 * setinggi layar, bukan capped 420dp) dan sweep-select (tekan-lama lalu geser buat centang
 * banyak lagu sekaligus) di-port 1:1 dari `SongListView` (`LibraryScreen.kt`) — termasuk
 * hysteresis anti-jitter-nya — biar rasanya konsisten sama sweep-select di tab Lagu, bukan
 * versi kW.
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
    // Batch 269 — user laporan: sweep-select di sheet ini kelewat sensitif, kadang ke-cancel
    // sendiri sebelum sempat kepakai (beda dari tab Lagu yang "normal"). Root cause: sheet ini
    // (`ModalBottomSheet`) punya gesture swipe-to-dismiss BAWAAN aktif di seluruh permukaan
    // sheet — bersaing langsung dgn long-press+drag sweep-select buat event pointer vertikal
    // yang sama. `SongListView` (`LibraryScreen.kt`) di layar biasa TIDAK punya pesaing gesture
    // sejenis ini sama sekali, makanya cuma di sini yang kena. Fix: `isSweeping` diset true
    // begitu long-press sweep berhasil (blok `onDragStart`), `confirmValueChange` sheet nolak
    // SEMUA perubahan state (termasuk dismiss akibat swipe) selama itu true — begitu sweep
    // selesai (`onDragEnd`/`onDragCancel`), dibuka lagi.
    var isSweeping by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !isSweeping })
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

    // Pola sweep-select persis `SongListView` (`LibraryScreen.kt`) — lihat komentar aslinya di
    // sana utk penjelasan lengkap tiap bagian (hysteresis, DisposableEffect cleanup, dst). Beda
    // di sini: tiap row SUDAH selalu dalam "mode pilih" (checkbox selalu tampil), jadi sweep
    // langsung nambah ke `selected`, tidak perlu `selectionMode` terpisah.
    val rowBoundsInRoot = remember(filtered) { mutableStateMapOf<Int, ClosedFloatingPointRange<Float>>() }
    var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var sweepAnchorIndex by remember { mutableStateOf<Int?>(null) }
    var sweepLastIndex by remember { mutableStateOf<Int?>(null) }
    val currentSelected by rememberUpdatedState(selected)
    var sweepBaseSelection by remember { mutableStateOf(setOf<Long>()) }

    fun indexAt(rootY: Float): Int? = rowBoundsInRoot.entries.firstOrNull { rootY in it.value }?.key

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .frostedGlass()
        ) {
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
                        .weight(1f)
                        .padding(vertical = 32.dp, horizontal = 20.dp)
                )
                filtered.isEmpty() -> EmptyState(
                    title = "Tidak ditemukan",
                    subtitle = "Coba kata kunci lain.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 32.dp, horizontal = 20.dp)
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onGloballyPositioned { containerCoordinates = it }
                        .pointerInput(filtered) {
                            val hysteresisPx = 6.dp.toPx()
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val root = containerCoordinates?.localToRoot(offset) ?: return@detectDragGesturesAfterLongPress
                                    val idx = indexAt(root.y) ?: return@detectDragGesturesAfterLongPress
                                    isSweeping = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    sweepAnchorIndex = idx
                                    sweepLastIndex = idx
                                    sweepBaseSelection = currentSelected
                                    selected = sweepBaseSelection + filtered[idx].id
                                },
                                onDrag = { change, _ ->
                                    val anchor = sweepAnchorIndex ?: return@detectDragGesturesAfterLongPress
                                    change.consume()
                                    val root = containerCoordinates?.localToRoot(change.position) ?: return@detectDragGesturesAfterLongPress
                                    val lastIdx = sweepLastIndex ?: return@detectDragGesturesAfterLongPress
                                    val idx = indexAt(root.y) ?: return@detectDragGesturesAfterLongPress
                                    if (idx == lastIdx) return@detectDragGesturesAfterLongPress
                                    val lastBounds = rowBoundsInRoot[lastIdx]
                                    if (lastBounds != null) {
                                        val committed = if (idx > lastIdx) root.y > lastBounds.endInclusive + hysteresisPx
                                        else root.y < lastBounds.start - hysteresisPx
                                        if (!committed) return@detectDragGesturesAfterLongPress
                                    }
                                    sweepLastIndex = idx
                                    val range = minOf(anchor, idx)..maxOf(anchor, idx)
                                    val sweptIds = range.map { filtered[it].id }
                                    selected = sweepBaseSelection + sweptIds
                                },
                                onDragEnd = { isSweeping = false; sweepAnchorIndex = null; sweepLastIndex = null },
                                onDragCancel = { isSweeping = false; sweepAnchorIndex = null; sweepLastIndex = null }
                            )
                        }
                ) {
                    itemsIndexed(filtered, key = { _, song -> song.id }) { index, song ->
                        DisposableEffect(index) {
                            onDispose { rowBoundsInRoot.remove(index) }
                        }
                        val isSelected = song.id in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    val top = coords.positionInRoot().y
                                    rowBoundsInRoot[index] = top..(top + coords.size.height)
                                }
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
