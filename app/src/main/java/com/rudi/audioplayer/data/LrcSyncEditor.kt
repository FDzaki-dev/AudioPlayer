package com.rudi.audioplayer.data

/**
 * Logika murni untuk fitur "Tap-to-Sync" (Roadmap #3) — mengubah lirik polos (baris teks
 * tanpa timestamp) jadi format LRC tersinkron `[mm:ss.xx]teks`, dengan user menekan tombol
 * "Tandai" persis saat baris itu mulai dinyanyikan di playback yang sedang berjalan.
 *
 * 0 Context/Android di file ini (pola sama `LyricsParser`/`AbRepeatLogic`) — semua state sesi
 * sync (`SyncSession`) murni immutable data class, transisi lewat fungsi pure `mark()`/`skip()`/
 * `undo()` yang masing-masing mengembalikan instance baru. UI (`LyricsSheet.kt`) cuma pegang 1
 * `var session by remember { ... }` dan reassign hasil pemanggilan fungsi ini.
 */

/** Timestamp `null` berarti baris ini sengaja dilewati (`skip()`), tetap disimpan sbg teks polos saat export. */
data class SyncSession(
    val lines: List<String>,
    val timestamps: List<Long?>,
    val currentIndex: Int
) {
    val isComplete: Boolean get() = currentIndex >= lines.size
    val currentLine: String? get() = lines.getOrNull(currentIndex)
}

object LrcSyncEditor {

    /** Pecah teks lirik mentah jadi baris-baris non-blank, trimmed — sumber sesi sync baru. */
    fun splitPlainLines(raw: String): List<String> =
        raw.lines().map { it.trim() }.filter { it.isNotEmpty() }

    fun startSession(raw: String): SyncSession {
        val lines = splitPlainLines(raw)
        return SyncSession(lines = lines, timestamps = List(lines.size) { null }, currentIndex = 0)
    }

    /** Tandai baris saat ini dgn [positionMs], maju ke baris berikutnya. No-op kalau sesi sudah selesai. */
    fun mark(session: SyncSession, positionMs: Long): SyncSession {
        if (session.isComplete) return session
        val updated = session.timestamps.toMutableList().apply { this[session.currentIndex] = positionMs }
        return session.copy(timestamps = updated, currentIndex = session.currentIndex + 1)
    }

    /** Lewati baris saat ini (tetap plain, tanpa timestamp), maju ke baris berikutnya. */
    fun skip(session: SyncSession): SyncSession {
        if (session.isComplete) return session
        return session.copy(currentIndex = session.currentIndex + 1)
    }

    /** Mundur 1 baris, menghapus timestamp baris itu kalau ada (undo mark ATAU undo skip, sama efeknya). */
    fun undo(session: SyncSession): SyncSession {
        if (session.currentIndex <= 0) return session
        val prevIndex = session.currentIndex - 1
        val updated = session.timestamps.toMutableList().apply { this[prevIndex] = null }
        return session.copy(timestamps = updated, currentIndex = prevIndex)
    }

    /** `[mm:ss.xx]` — 2-digit menit (cukup utk lagu wajar, sama batas implisit `LyricsParser`), centisecond 2-digit. */
    fun formatTimestamp(ms: Long): String {
        val clamped = ms.coerceAtLeast(0L)
        val totalCentis = clamped / 10
        val minutes = totalCentis / 6000
        val seconds = (totalCentis / 100) % 60
        val centis = totalCentis % 100
        return "[%02d:%02d.%02d]".format(minutes, seconds, centis)
    }

    /** Gabungkan sesi (selesai atau belum) jadi 1 teks siap simpan — baris ber-timestamp dapat prefix LRC. */
    fun buildLrcText(session: SyncSession): String =
        session.lines.indices.joinToString("\n") { i ->
            val t = session.timestamps.getOrNull(i)
            if (t != null) formatTimestamp(t) + session.lines[i] else session.lines[i]
        }
}
