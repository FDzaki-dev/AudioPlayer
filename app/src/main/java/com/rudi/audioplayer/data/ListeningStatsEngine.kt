package com.rudi.audioplayer.data

import java.time.LocalDate

/**
 * Pure aggregation logic for the Stats Dashboard (Batch 90, roadmap item #10). Takes already-
 * loaded data (song list, raw play counts, daily/hourly counters) as plain parameters rather
 * than reading SharedPreferences itself — same "extract pure function from Context-bound store"
 * pattern established in Batch 27 (ShakePulseTracker, MusicRepository.deriveFolderName,
 * LibraryFilterStore.shouldKeep) and reused by SmartPlaylistEngine in Batch 89. Keeping this
 * Context-free is what makes it unit-testable on plain JVM without Robolectric.
 */
object ListeningStatsEngine {

    data class ArtistCount(val artist: String, val playCount: Int)

    data class DayCount(val date: LocalDate, val playCount: Int)

    data class Snapshot(
        val totalPlays: Int,
        val totalListeningMs: Long,
        val topArtists: List<ArtistCount>,
        val weeklyTrend: List<DayCount>,
        val peakHour: Int?,
        val peakHourCount: Int
    )

    /**
     * Aggregates songs by artist using their raw play counts, highest total first. Blank
     * artist names are excluded (same convention as PlayerViewModel.getTopArtistMix).
     */
    fun topArtists(songs: List<Song>, counts: Map<Long, Int>, limit: Int = 5): List<ArtistCount> {
        if (counts.isEmpty()) return emptyList()
        val songMap = songs.associateBy { it.id }
        return counts.entries
            .mapNotNull { (songId, count) -> songMap[songId]?.let { it.artist to count } }
            .filter { it.first.isNotBlank() }
            .groupBy({ it.first }, { it.second })
            .map { (artist, playCounts) -> ArtistCount(artist, playCounts.sum()) }
            .sortedByDescending { it.playCount }
            .take(limit)
    }

    /** Total milliseconds spent listening, estimated as duration × play count per song —
     * there is no continuous playback-position log, so this is a best-effort estimate that
     * assumes each recorded play ran to completion (consistent with how PlayStatsStore counts
     * a "play" the moment a track starts, not when it finishes). */
    fun totalListeningMs(songs: List<Song>, counts: Map<Long, Int>): Long {
        if (counts.isEmpty()) return 0L
        val songMap = songs.associateBy { it.id }
        return counts.entries.sumOf { (songId, count) -> (songMap[songId]?.duration ?: 0L) * count }
    }

    /** Wraps ListeningHistoryStore.getCountsForLastDays() output into the Snapshot's shape —
     * kept as a thin pass-through (not raw logic) since date-range iteration already lives in
     * the store and duplicating it here would be two sources of truth for the same rule. */
    fun weeklyTrend(rawDailyCounts: List<Pair<LocalDate, Int>>): List<DayCount> =
        rawDailyCounts.map { (date, count) -> DayCount(date, count) }

    /** Hour (0-23) with the highest all-time play count, and that count. Null hour when every
     * bucket is still zero (no plays recorded yet). Ties resolve to the earliest hour. */
    fun peakHour(hourlyCounts: IntArray): Pair<Int?, Int> {
        var bestHour: Int? = null
        var bestCount = 0
        for (hour in hourlyCounts.indices) {
            val count = hourlyCounts[hour]
            if (count > bestCount) {
                bestCount = count
                bestHour = hour
            }
        }
        return bestHour to bestCount
    }

    /** Assembles every dashboard number in one call so the ViewModel/UI only need one entry
     * point instead of wiring 4 stores' worth of individual functions themselves. */
    fun buildSnapshot(
        songs: List<Song>,
        counts: Map<Long, Int>,
        totalPlays: Int,
        rawDailyCounts: List<Pair<LocalDate, Int>>,
        hourlyCounts: IntArray,
        topArtistLimit: Int = 5
    ): Snapshot {
        val (peakHourValue, peakHourCount) = peakHour(hourlyCounts)
        return Snapshot(
            totalPlays = totalPlays,
            totalListeningMs = totalListeningMs(songs, counts),
            topArtists = topArtists(songs, counts, topArtistLimit),
            weeklyTrend = weeklyTrend(rawDailyCounts),
            peakHour = peakHourValue,
            peakHourCount = peakHourCount
        )
    }
}
