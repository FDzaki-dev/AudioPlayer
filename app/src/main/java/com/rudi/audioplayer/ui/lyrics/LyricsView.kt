package com.rudi.audioplayer.ui.lyrics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Batch 245 — Lyrics offline-first 3/4. Baris LRC ter-parse (`timeMs`, `text`), top-level pure
// data class — testable tanpa Compose runtime.
data class LyricLine(val timeMs: Long, val text: String)

private val LRC_LINE_REGEX = Regex("""\[(\d{2}):(\d{2})\.(\d{2})](.*)""")

// Format spec: [mm:ss.xx]text — xx = centisecond (2 digit), BUKAN millisecond (3 digit),
// makanya *10 (bukan padding 3 digit) buat konversi ke ms.
fun parseLRC(lrc: String): List<LyricLine> =
    lrc.lineSequence()
        .mapNotNull { line -> LRC_LINE_REGEX.find(line)?.let { m ->
            val (mm, ss, cs, text) = m.destructured
            val timeMs = (mm.toLong() * 60_000L) + (ss.toLong() * 1_000L) + (cs.toLong() * 10L)
            LyricLine(timeMs, text.trim())
        } }
        .sortedBy { it.timeMs }
        .toList()

// Index baris aktif = baris TERAKHIR yang timeMs-nya <= posisi sekarang (baris LRC ditulis
// menandai "mulai dari sini", bukan interval [start,end) eksplisit).
fun activeLyricIndex(lines: List<LyricLine>, currentPositionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var result = -1
    for (i in lines.indices) {
        if (lines[i].timeMs <= currentPositionMs) result = i else break
    }
    return result
}

/**
 * Render lirik: synced LRC dgn auto-scroll+highlight kalau [syncedLyrics] ada, fallback ke
 * [plainLyrics] (scroll manual, 0 highlight — tidak ada timestamp buat disinkron), fallback
 * lagi ke pesan "Lirik tidak ditemukan" kalau dua-duanya null/kosong (spec error case #9).
 */
@Composable
fun LyricsView(
    plainLyrics: String?,
    syncedLyrics: String?,
    currentPositionMs: Long,
    modifier: Modifier = Modifier
) {
    when {
        !syncedLyrics.isNullOrBlank() -> SyncedLyricsContent(syncedLyrics, currentPositionMs, modifier)
        !plainLyrics.isNullOrBlank() -> PlainLyricsContent(plainLyrics, modifier)
        else -> EmptyLyricsMessage(modifier)
    }
}

@Composable
private fun SyncedLyricsContent(lrcString: String, currentPositionMs: Long, modifier: Modifier) {
    val lines = remember(lrcString) { parseLRC(lrcString) }
    if (lines.isEmpty()) {
        // Regex 0 match sama sekali (string ada tapi bukan format LRC valid) — turun ke plain
        // apa-adanya drpd nampilin kosong.
        PlainLyricsContent(lrcString, modifier)
        return
    }

    val listState = rememberLazyListState()
    var activeIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(currentPositionMs, lines) {
        val newIndex = activeLyricIndex(lines, currentPositionMs)
        if (newIndex != activeIndex) {
            activeIndex = newIndex
            if (newIndex >= 0) {
                // -2 biar baris aktif nongol sepertiga atas viewport, bukan mepet paling atas.
                listState.animateScrollToItem((newIndex - 2).coerceAtLeast(0))
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 120.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeIndex
            Text(
                text = line.text,
                textAlign = TextAlign.Center,
                // Bold+ukuran beda (bukan cuma warna) buat baris aktif — konsisten aturan
                // Batch 241 "informasi penting jangan cuma dibedakan warna".
                style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PlainLyricsContent(text: String, modifier: Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
        item {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun EmptyLyricsMessage(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Lirik tidak ditemukan",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
    }
}

/** Dipanggil dari layar Now Playing (batch integrasi UI, belum di-wire di batch ini) — 1 titik
 * yg langsung terima [LyricsUiState] dari [LyricsViewModel] tanpa caller perlu tahu isi state. */
@Composable
fun LyricsStateView(uiState: LyricsUiState, currentPositionMs: Long, modifier: Modifier = Modifier) {
    when (uiState) {
        is LyricsUiState.Found -> LyricsView(uiState.plainLyrics, uiState.syncedLyrics, currentPositionMs, modifier)
        LyricsUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        LyricsUiState.NotFound -> EmptyLyricsMessage(modifier)
        LyricsUiState.Idle -> {}
    }
}
