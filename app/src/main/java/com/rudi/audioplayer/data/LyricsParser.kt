package com.rudi.audioplayer.data

/** A single lyrics line. [timeMs] is null for plain (unsynced) lyrics. */
data class LyricLine(val timeMs: Long?, val text: String)

object LyricsParser {
    private val lrcLineRegex = Regex("""^\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?](.*)$""")

    /** Parses raw lyrics text. Lines matching `[mm:ss.xx]text` become synced lines; everything else is plain text. */
    fun parse(raw: String): List<LyricLine> =
        raw.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val match = lrcLineRegex.find(line)
                if (match != null) {
                    val (min, sec, ms, text) = match.destructured
                    val minutes = min.toLongOrNull() ?: 0L
                    val seconds = sec.toLongOrNull() ?: 0L
                    val millis = when (ms.length) {
                        0 -> 0L
                        1 -> ms.toLong() * 100
                        2 -> ms.toLong() * 10
                        else -> ms.toLong()
                    }
                    LyricLine(minutes * 60_000 + seconds * 1000 + millis, text.trim())
                } else {
                    LyricLine(null, line)
                }
            }

    /** True if every parsed line carries a timestamp (i.e. this is an LRC-synced lyric). */
    fun isSynced(lines: List<LyricLine>): Boolean = lines.isNotEmpty() && lines.all { it.timeMs != null }

    /** Index of the line that should be highlighted for the given playback position, or -1 before the first cue. */
    fun currentLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        var result = -1
        for (i in lines.indices) {
            val t = lines[i].timeMs ?: continue
            if (t <= positionMs) result = i else break
        }
        return result
    }
}
