package com.rudi.audioplayer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.rudi.audioplayer.ui.theme.isCalmRetroTheme
import com.rudi.audioplayer.ui.theme.calmScanlines
import com.rudi.audioplayer.ui.theme.Radius
import com.rudi.audioplayer.ui.theme.neuRowTile
import com.rudi.audioplayer.ui.theme.rememberIosFlingBehavior
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.Song
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet

// Batch 517 — Wave 2 T8a (MOVE-ONLY): AlbumGridView/SongListView/GroupedListView/SongRow dipindah dari
// LibraryScreen.kt. Badan fungsi identik karakter-per-karakter; satu-satunya beda = `private` -> `internal`
// pada 4 deklarasi fungsi (dipanggil dari LibraryScreen.kt / SearchResultsView). Lihat CHANGELOG Batch 517.

@Composable
internal fun AlbumGridView(songs: List<Song>, onSongClick: (List<Song>, Int) -> Unit) {
    var selectedAlbum by remember(songs) { mutableStateOf<String?>(null) }
    val grouped = remember(songs) { songs.groupBy { it.album.ifBlank { "Album Tidak Diketahui" } } }
    val sortedAlbumKeys = remember(grouped) { grouped.keys.sortedBy { it.lowercase() } }

    if (selectedAlbum == null) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sortedAlbumKeys, key = { it }) { album ->
                val albumSongs = grouped[album].orEmpty()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAlbum = album }
                ) {
                    AlbumArt(
                        artworkUri = albumSongs.first().uri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(Radius.xxxl))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(album, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${albumSongs.size} lagu",
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    } else {
        val albumSongs = grouped[selectedAlbum].orEmpty()
        Column {
            TextButton(onClick = { selectedAlbum = null }) { Text("< Kembali ke Album") }
            LazyColumn {
                itemsIndexed(albumSongs, key = { _, song -> song.id }) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(albumSongs, index) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(formatDuration(song.duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun SongListView(
    songs: List<Song>,
    favoriteIds: ImmutableSet<Long>,
    onFavoriteToggle: (Long) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onHideSong: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit = {},
    selectionMode: Boolean = false,
    selectedIds: ImmutableSet<Long> = persistentSetOf(),
    onToggleSelect: (Long) -> Unit = {},
    onEnterSelectionMode: (Long) -> Unit = {},
    onSweepSelectRange: (ImmutableSet<Long>) -> Unit = {},
    currentSongId: Long? = null
) {
    val haptic = LocalHapticFeedback.current
    // Batch 70 — root-coordinate bounds of every currently-composed row, refreshed as
    // LazyColumn recycles/composes items. Keyed by index (not song id) since that's what a
    // contiguous "from here to here" range is naturally expressed in.
    val rowBoundsInRoot = remember(songs) { mutableStateMapOf<Int, ClosedFloatingPointRange<Float>>() }
    var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var sweepAnchorIndex by remember { mutableStateOf<Int?>(null) }
    var sweepLastIndex by remember { mutableStateOf<Int?>(null) }
    // Batch 73 — fix "sweep-select kepentok, long-press baru mereset bukan melanjutkan": a
    // second sweep gesture (e.g. user hit the edge of the visible list, lifted their finger,
    // and long-pressed again to keep extending the selection) used to always start from
    // `persistentSetOf(songs[idx].id)` — a brand-new single-item set — discarding whatever was
    // already selected from the PREVIOUS sweep/tap. `selectedIds` is read via
    // rememberUpdatedState because this pointerInput block is only relaunched when `songs`
    // changes, not when `selectedIds` changes — without this, onDragStart/onDrag would close
    // over a stale snapshot of the selection from whenever the gesture detector was last
    // (re)installed, silently undoing selection changes made by taps in between sweeps too.
    val currentSelectedIds by rememberUpdatedState(selectedIds)
    // Snapshot of the selection that existed before the CURRENT sweep gesture began — every
    // sweep-in-progress update below is (this base) UNION (range just swept), so lifting the
    // finger and starting a new long-press-drag extends on top of prior selections instead of
    // replacing them. Captured once per gesture in onDragStart, not read continuously, so that
    // dragging back-and-forth within one continuous gesture still behaves like a plain range
    // select (shrinking the range removes rows again) rather than only ever growing.
    var sweepBaseSelection by remember { mutableStateOf(persistentSetOf<Long>()) }
    // Root cause (user report): a stationary long-press (held, then released with ZERO
    // movement) looked like it did nothing — worse, felt like it actively CANCELLED itself.
    // Sequence: onDragStart below fires normally (Batch 72 already fixed the earlier "long
    // press does nothing AT ALL" bug by removing SongRow's competing onLongClick), selects the
    // row, sets selectionMode=true. But `onDrag` never runs (no movement = no
    // PointerInputChange to consume), so the ORIGINATING down/up touch itself is never
    // consumed by this detector — SongRow's own plain `clickable` (still listening to that same
    // down/up pair for its own click, since `clickable` has no long-press timing of its own,
    // just a press-then-release) sees a perfectly normal, unconsumed tap and fires `onClick`
    // a beat later. Because `selectionMode` is now (correctly) true, SongRow's own
    // `if (selectionMode) onToggleSelect() else onClick()` routes that phantom tap to
    // `onToggleSelect` — which immediately toggles the row BACK OFF. Net visible result: select
    // → instant self-deselect, i.e. nothing. Fix: latch which row id the sweep gesture just
    // touched; the very next click/toggle for THAT id is swallowed once (self-clearing), every
    // other row and every later, genuine tap behaves exactly as before.
    var suppressClickForId by remember { mutableStateOf<Long?>(null) }

    fun indexAt(rootY: Float): Int? = rowBoundsInRoot.entries.firstOrNull { rootY in it.value }?.key

    LazyColumn(
        modifier = Modifier
            .onGloballyPositioned { containerCoordinates = it }
            .pointerInput(songs) {
                // Batch 1 (v263 session) — Pending Queue item 2: user reported sweep-select in
                // tab Lagu as over-sensitive vs iOS's standard feel. Root cause: `indexAt()`
                // flips `sweepLastIndex` the INSTANT the Y coordinate crosses a row's exact
                // pixel boundary — completely normal finger tremor while holding roughly still
                // near a boundary line reads as several rapid crossings, so selection flickered
                // in/out on rows the user never meant to touch. Fix: hysteresisPx — once a row
                // is committed, the touch must travel that much PAST the previous row's boundary
                // (not just 1px past it) before the next row is allowed to commit. Doesn't
                // change fast/deliberate swipes at all (those clear the margin trivially), only
                // damps the tiny-jitter-near-a-boundary case.
                val hysteresisPx = 6.dp.toPx()
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val root = containerCoordinates?.localToRoot(offset) ?: return@detectDragGesturesAfterLongPress
                        val idx = indexAt(root.y) ?: return@detectDragGesturesAfterLongPress
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sweepAnchorIndex = idx
                        sweepLastIndex = idx
                        sweepBaseSelection = currentSelectedIds.toPersistentSet()
                        suppressClickForId = songs[idx].id
                        onSweepSelectRange(sweepBaseSelection.add(songs[idx].id))
                    },
                    onDrag = { change, _ ->
                        val anchor = sweepAnchorIndex ?: return@detectDragGesturesAfterLongPress
                        change.consume()
                        // Real movement confirmed — this is an actual sweep, not a stationary
                        // press, so the anchor row's own click will never fire (SongRow's
                        // `clickable` self-cancels once touch slop is exceeded). Clear the latch
                        // now instead of leaving it set until some unrelated future tap on this
                        // same row, which would otherwise get silently swallowed by mistake.
                        suppressClickForId = null
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
                        val sweptIds = range.map { songs[it].id }
                        onSweepSelectRange(sweepBaseSelection.addAll(sweptIds))
                    },
                    onDragEnd = { sweepAnchorIndex = null; sweepLastIndex = null },
                    // Defensive cleanup only — the stationary-press case is expected to clear
                    // `suppressClickForId` itself via the swallowed click (see wiring below), and
                    // the real-drag case already clears it in `onDrag` above. This just prevents
                    // a leak into some unrelated future tap on the same row in any edge case
                    // where neither of those paths runs (e.g. gesture cancelled by an ancestor
                    // before either fires).
                    onDragCancel = { sweepAnchorIndex = null; sweepLastIndex = null; suppressClickForId = null }
                )
            },
        // Batch 364 — kurva fling ala iOS (glide lebih panjang & mulus, lihat
        // IosScrollPhysics.kt). Rubber-band overscroll-nya sendiri sudah otomatis app-wide lewat
        // LocalOverscrollFactory (Theme.kt), tidak perlu disentuh di sini. Layar ini duluan krn
        // paling sering di-scroll user (daftar lagu utama) — sisanya di PENDING_IosFlingBehavior.md.
        flingBehavior = rememberIosFlingBehavior(),
        // Batch 516 [laporan user v515: "drag to select ... gak ikut jari dan malah scrolling normal"] —
        // scroll bawaan LazyColumn dan detektor sweep di atas berebut gerakan jari yang SAMA setelah
        // long-press; kalau scroll yang menang, jari "menyeret list", bukan menyapu pilihan. Selama sweep
        // aktif (`sweepAnchorIndex` != null: dari onDragStart sampai onDragEnd/onDragCancel), scroll user
        // dimatikan supaya gerakan jari cuma dibaca detektor sweep. DUGAAN penyebab (dari pembacaan kode,
        // BELUM dibuktikan di device) — lihat CHANGELOG Batch 516. Scroll normal (geser cepat tanpa
        // long-press) tidak tersentuh: `sweepAnchorIndex` baru terisi SETELAH long-press terkonfirmasi.
        userScrollEnabled = sweepAnchorIndex == null
    ) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            // Batch 78 — fix: rowBoundsInRoot only ever got entries WRITTEN (onGloballyPositioned),
            // never REMOVED. Once a row scrolled far enough to leave composition (LazyColumn
            // recycling), its bounds entry stayed in the map forever at its last known (now stale)
            // position — Batch 70 flagged this exact scroll-during-sweep scenario as "belum
            // ditest" without root-causing it. Concretely: user sweeps, lifts finger, scrolls the
            // list normally (unclaimed by detectDragGesturesAfterLongPress since it never reaches
            // long-press threshold), then long-presses again — indexAt() does
            // `entries.firstOrNull { rootY in it.value }` over a map that can contain both live
            // entries (current on-screen positions) AND stale ones (disposed rows' old positions,
            // which now overlap completely different rows after the scroll) — whichever the map
            // happens to hit first wins, so the sweep could silently anchor on/extend through the
            // wrong songs. DisposableEffect removes each row's own entry the moment it leaves
            // composition, so the map only ever holds bounds for rows actually on screen right now.
            DisposableEffect(index) {
                onDispose { rowBoundsInRoot.remove(index) }
            }
            SongRow(
                modifier = Modifier.onGloballyPositioned { coords ->
                    val top = coords.positionInRoot().y
                    rowBoundsInRoot[index] = top..(top + coords.size.height)
                },
                song = song,
                isFavorite = favoriteIds.contains(song.id),
                onFavoriteToggle = { onFavoriteToggle(song.id) },
                onClick = {
                    if (suppressClickForId == song.id) suppressClickForId = null
                    else onSongClick(songs, index)
                },
                onPlayNext = { onPlayNext(song) },
                onAddToQueue = { onAddToQueue(song) },
                onAddToPlaylist = { onAddToPlaylist(song) },
                onHideSong = { onHideSong(song) },
                onDeleteSong = { onDeleteSong(song) },
                selectionMode = selectionMode,
                isSelected = selectedIds.contains(song.id),
                onToggleSelect = {
                    if (suppressClickForId == song.id) suppressClickForId = null
                    else onToggleSelect(song.id)
                },
                onEnterSelectionMode = { onEnterSelectionMode(song.id) },
                isPlaying = song.id == currentSongId
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
internal fun GroupedListView(
    songs: List<Song>,
    groupOf: (Song) -> String,
    favoriteIds: ImmutableSet<Long>,
    onFavoriteToggle: (Long) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onHideSong: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit = {},
    selectionMode: Boolean = false,
    selectedIds: ImmutableSet<Long> = persistentSetOf(),
    onToggleSelect: (Long) -> Unit = {},
    onEnterSelectionMode: (Long) -> Unit = {},
    onSweepSelectRange: (ImmutableSet<Long>) -> Unit = {},
    currentSongId: Long? = null
) {
    var selectedGroup by remember(songs) { mutableStateOf<String?>(null) }
    val grouped = remember(songs) { songs.groupBy(groupOf) }
    val sortedGroupKeys = remember(grouped) { grouped.keys.sorted() }

    if (selectedGroup == null) {
        LazyColumn {
            items(sortedGroupKeys, key = { it }) { group ->
                ListItem(
                    headlineContent = { Text(group, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("${grouped[group]?.size ?: 0} lagu") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .animateItem()
                        .clickable { selectedGroup = group }
                        .padding(horizontal = 20.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    } else {
        val groupSongs = grouped[selectedGroup].orEmpty()
        Column {
            TextButton(onClick = { selectedGroup = null }) { Text("< Kembali") }
            SongListView(
                songs = groupSongs,
                favoriteIds = favoriteIds,
                onFavoriteToggle = onFavoriteToggle,
                onSongClick = onSongClick,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onHideSong = onHideSong,
                onDeleteSong = onDeleteSong,
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                onToggleSelect = onToggleSelect,
                onEnterSelectionMode = onEnterSelectionMode,
                onSweepSelectRange = onSweepSelectRange,
                currentSongId = currentSongId
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SongRow(
    song: Song,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onHideSong: () -> Unit,
    onDeleteSong: () -> Unit = {},
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onEnterSelectionMode: () -> Unit = {},
    // Pending item Batch 163: sebelumnya SongRow 0 indikator "sedang diputar" sama sekali,
    // beda dari QueueRow yang sudah punya (primary 12% alpha bg + bold title). Default false —
    // 0 caller lama di luar 3 titik yang sudah diupdate (SongListView/GroupedListView/
    // SearchResultsView) yang perlu berubah.
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    // v3 upgrade lanjutan — spread Pilar A (CRT scanlines, Batch 133) dari AlbumArtHero (Now
    // Playing) ke sini: kandidat "Card list lagu" yang spec sebut eksplisit tapi sengaja
    // ditunda batch itu ("gak usah greedy", pola sama presedan aberrasi CTA Batch 129->130-131).
    // SongRow ini 1 titik dipakai ulang di semua tampilan daftar lagu (tab Lagu/GroupedListView/
    // SearchResultsView — 3 call site, grep-confirmed), jadi 1 edit di sini otomatis menjangkau
    // ketiganya, tidak perlu disentuh 1-1.
    val isCalmRetro = isCalmRetroTheme()

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Batch 557 — baris = tile berkedalaman (`neuRowTile()`, semua tema). Margin 12/3dp +
                // padding dalam 8/5dp = total 20/8dp SAMA dgn padding lama: posisi teks/art/durasi
                // dan tinggi baris (64dp) TIDAK berubah. Highlight "sedang diputar" di bawah kini
                // ter-clip ke bentuk tile.
                .padding(horizontal = 12.dp, vertical = 3.dp)
                .neuRowTile(shape = RoundedCornerShape(Radius.xl), elevation = 3.dp)
                // Batch 163 pending-item fix: samakan pola highlight "sedang diputar" dengan
                // `QueueRow` (primary 12% alpha bg) — background dipasang SEBELUM clickable,
                // urutan modifier sama persis QueueRow, supaya ripple clickable tetap kelihatan
                // di atas warna latar ini, bukan ketutup.
                .background(if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                // Batch 72: this used to also carry onLongClick -> onEnterSelectionMode()
                // (Batch 66) — a second, INDEPENDENT long-press recognizer on the exact same
                // touch as SongListView's new sweep-select detectDragGesturesAfterLongPress
                // (Batch 70, wraps the whole LazyColumn). Two unrelated long-press gesture
                // families racing for the same physical touch is why sweep-select "did
                // literally nothing" — combinedClickable's own press/ripple tracking marks the
                // pointer consumed as part of recognizing ITS long click, which cancels the
                // outer sweep detector's awaitLongPressOrCancellation before it can ever fire.
                // Sweep's own onDragStart already reproduces plain "press-and-hold this row"
                // (calls onSweepSelectRange with just that one id when the finger never
                // leaves it), so this isn't lost functionality — "Pilih" in the row's overflow
                // menu (search onEnterSelectionMode() below) remains as the explicit-tap entry
                // point into selection mode.
                .clickable(onClick = { if (selectionMode) onToggleSelect() else onClick() })
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                Spacer(modifier = Modifier.width(4.dp))
            }
            AlbumArt(
                artworkUri = song.uri,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(Radius.xxl))
                    .then(if (isCalmRetro) Modifier.calmScanlines() else Modifier)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPlaying) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = "Sedang diputar",
                            // Batch 229 — Iconography 4/7 (action vs decorative icon), konsisten
                            // dgn fix QueueSheet.kt: badge status murni, bukan `primary`.
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        song.title,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false).basicMarquee()
                    )
                }
                Text(
                    song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!selectionMode) {
                Text(
                    formatDuration(song.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                IconButton(onClick = {
                    // Was the only place this toggle fired with zero haptic — Now Playing's
                    // identical favorite button already had it (see below). Same action,
                    // same feedback, regardless of which screen it's tapped from.
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFavoriteToggle()
                }) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Hapus dari favorit" else "Tambah ke favorit",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
                // Batch 265 (v263 session Batch2) — user lapor "gak bisa pilih lagu langsung dari
                // tab favorit/playlist". Root cause SUNGGUHAN, bukan gap per-tab: `showMenu`
                // (DropdownMenu di bawah, isinya termasuk "Pilih" -> onEnterSelectionMode()) TIDAK
                // PERNAH di-set true di MANA PUN di file ini — grep `showMenu` cuma nongol di
                // deklarasi + di dalam DropdownMenu itu sendiri, 0 trigger. Menu ini sepenuhnya
                // unreachable, di SEMUA tab yang lewat SongRow (Lagu/Favorit/Artis/Folder/Search
                // — 1 composable dipakai ulang, grep-confirmed komentar Batch 133 di atas), bukan
                // cuma Favorit/Playlist seperti dugaan awal (Batch 262/264). Playlist tab sendiri
                // TETAP belum tersentuh — itu `PlaylistTabView`, composable lain total, tidak
                // lewat SongRow sama sekali, root cause ini tidak menjangkaunya.
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opsi lagu lainnya"
                    )
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Putar Berikutnya") },
                leadingIcon = { Icon(Icons.Default.PlaylistPlay, contentDescription = null) },
                onClick = { showMenu = false; onPlayNext() }
            )
            DropdownMenuItem(
                text = { Text("Tambah ke Antrean") },
                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                onClick = { showMenu = false; onAddToQueue() }
            )
            DropdownMenuItem(
                text = { Text("Tambah ke Playlist") },
                leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                onClick = { showMenu = false; onAddToPlaylist() }
            )
            DropdownMenuItem(
                text = { Text("Sembunyikan") },
                leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                onClick = { showMenu = false; onHideSong() }
            )
            DropdownMenuItem(
                text = { Text("Pilih") },
                leadingIcon = { Icon(Icons.Default.CheckCircleOutline, contentDescription = null) },
                onClick = { showMenu = false; onEnterSelectionMode() }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            DropdownMenuItem(
                text = { Text("Hapus dari Perangkat", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = { showMenu = false; onDeleteSong() }
            )
        }
    }
}
