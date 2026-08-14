package com.rudi.audioplayer.data

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.time.LocalDate

class ListeningStatsEngineTest {

    // Same reasoning as SmartPlaylistEngineTest's helper: Uri.parse(...) returns null on plain
    // JVM (no real Android runtime), which throws when assigned to Song's non-null `uri` field.
    private fun song(
        id: Long,
        artist: String,
        durationMs: Long = 200_000L
    ) = Song(
        id = id,
        title = "Title $id",
        artist = artist,
        album = "Album",
        albumId = 1L,
        duration = durationMs,
        dateAdded = 0L,
        uri = mock(Uri::class.java),
        folderName = "Musik",
        folderPath = "/Musik",
        year = 0
    )

    @Test
    fun `topArtists sums plays across multiple songs by the same artist`() {
        val songs = listOf(
            song(1L, "Artist A"),
            song(2L, "Artist A"),
            song(3L, "Artist B")
        )
        val counts = mapOf(1L to 3, 2L to 4, 3L to 5)
        val result = ListeningStatsEngine.topArtists(songs, counts, limit = 5)
        assertEquals("Artist A", result[0].artist)
        assertEquals(7, result[0].playCount)
        assertEquals("Artist B", result[1].artist)
        assertEquals(5, result[1].playCount)
    }

    @Test
    fun `topArtists excludes blank artist names`() {
        val songs = listOf(song(1L, ""), song(2L, "Real Artist"))
        val counts = mapOf(1L to 10, 2L to 1)
        val result = ListeningStatsEngine.topArtists(songs, counts)
        assertEquals(1, result.size)
        assertEquals("Real Artist", result[0].artist)
    }

    @Test
    fun `topArtists respects limit`() {
        val songs = (1..10L).map { song(it, "Artist $it") }
        val counts = (1..10L).associateWith { 1 }
        val result = ListeningStatsEngine.topArtists(songs, counts, limit = 3)
        assertEquals(3, result.size)
    }

    @Test
    fun `topArtists returns empty list when counts empty`() {
        val songs = listOf(song(1L, "Artist A"))
        assertTrue(ListeningStatsEngine.topArtists(songs, emptyMap()).isEmpty())
    }

    @Test
    fun `topArtists ignores counts for songs no longer in the library`() {
        val songs = listOf(song(1L, "Artist A"))
        val counts = mapOf(1L to 2, 999L to 50)
        val result = ListeningStatsEngine.topArtists(songs, counts)
        assertEquals(1, result.size)
        assertEquals(2, result[0].playCount)
    }

    @Test
    fun `totalListeningMs multiplies duration by play count`() {
        val songs = listOf(song(1L, "A", durationMs = 100_000L), song(2L, "B", durationMs = 50_000L))
        val counts = mapOf(1L to 2, 2L to 3)
        // 100_000*2 + 50_000*3 = 350_000
        assertEquals(350_000L, ListeningStatsEngine.totalListeningMs(songs, counts))
    }

    @Test
    fun `totalListeningMs is zero when there are no plays`() {
        val songs = listOf(song(1L, "A"))
        assertEquals(0L, ListeningStatsEngine.totalListeningMs(songs, emptyMap()))
    }

    @Test
    fun `peakHour picks the highest bucket`() {
        val hourly = IntArray(24)
        hourly[9] = 3
        hourly[20] = 7
        hourly[21] = 5
        val (hour, count) = ListeningStatsEngine.peakHour(hourly)
        assertEquals(20, hour)
        assertEquals(7, count)
    }

    @Test
    fun `peakHour returns null hour when every bucket is zero`() {
        val (hour, count) = ListeningStatsEngine.peakHour(IntArray(24))
        assertNull(hour)
        assertEquals(0, count)
    }

    @Test
    fun `peakHour breaks ties by picking the earliest hour`() {
        val hourly = IntArray(24)
        hourly[5] = 4
        hourly[15] = 4
        val (hour, _) = ListeningStatsEngine.peakHour(hourly)
        assertEquals(5, hour)
    }

    @Test
    fun `weeklyTrend passes through raw daily counts unchanged`() {
        val today = LocalDate.of(2026, 8, 14)
        val raw = listOf(today.minusDays(1) to 2, today to 5)
        val result = ListeningStatsEngine.weeklyTrend(raw)
        assertEquals(2, result.size)
        assertEquals(today, result[1].date)
        assertEquals(5, result[1].playCount)
    }

    @Test
    fun `buildSnapshot assembles all fields consistently`() {
        val songs = listOf(song(1L, "Artist A", durationMs = 60_000L))
        val counts = mapOf(1L to 4)
        val hourly = IntArray(24).also { it[10] = 4 }
        val today = LocalDate.of(2026, 8, 14)
        val snapshot = ListeningStatsEngine.buildSnapshot(
            songs = songs,
            counts = counts,
            totalPlays = 4,
            rawDailyCounts = listOf(today to 4),
            hourlyCounts = hourly
        )
        assertEquals(4, snapshot.totalPlays)
        assertEquals(240_000L, snapshot.totalListeningMs)
        assertEquals(1, snapshot.topArtists.size)
        assertEquals(10, snapshot.peakHour)
        assertEquals(4, snapshot.peakHourCount)
        assertEquals(1, snapshot.weeklyTrend.size)
    }
}
