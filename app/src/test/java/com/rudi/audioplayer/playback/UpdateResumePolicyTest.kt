package com.rudi.audioplayer.playback

import com.rudi.audioplayer.playback.UpdateResumePolicy.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateResumePolicyTest {

    private val now = 1_800_000_000_000L

    private fun exit(reason: Int, ageMs: Long, description: String? = null) =
        ProcessExitRecord(reason = reason, timestampMs = now - ageMs, description = description)

    @Test
    fun `playing plus fresh PACKAGE_UPDATED exit resumes - the exact case from the 19 Nov log`() {
        // log: waktu_mati 08:48:25 alasan=PACKAGE_UPDATED importance=FOREGROUND ... due to installPackageLI; broadcast ~1 dtk kemudian
        val exits = listOf(exit(UpdateResumePolicy.EXIT_REASON_PACKAGE_UPDATED, 1_000L, "stop com.rudi.audioplayer due to installPackageLI"))
        assertEquals(Decision.RESUME, UpdateResumePolicy.decide(true, exits, now))
    }

    @Test
    fun `not playing never resumes even after an update kill - paused music must stay paused`() {
        val exits = listOf(exit(UpdateResumePolicy.EXIT_REASON_PACKAGE_UPDATED, 1_000L))
        assertEquals(Decision.SKIP_NOT_PLAYING, UpdateResumePolicy.decide(false, exits, now))
    }

    @Test
    fun `stale playing flag without any exit record is skipped - tool unavailable is not a pass`() {
        assertEquals(Decision.SKIP_NO_UPDATE_EXIT, UpdateResumePolicy.decide(true, emptyList(), now))
    }

    @Test
    fun `playing flag with only non-update exits is skipped - OEM kill or crash days ago must not start audio`() {
        val exits = listOf(
            exit(reason = 10, ageMs = 2_000L, description = "swipe from recents"), // USER_REQUESTED
            exit(reason = 3, ageMs = 86_400_000L), // LOW_MEMORY
            exit(reason = 4, ageMs = 5_000L) // CRASH
        )
        assertEquals(Decision.SKIP_NO_UPDATE_EXIT, UpdateResumePolicy.decide(true, exits, now))
    }

    @Test
    fun `an old PACKAGE_UPDATED exit from a previous update does not count`() {
        val exits = listOf(exit(UpdateResumePolicy.EXIT_REASON_PACKAGE_UPDATED, UpdateResumePolicy.MAX_EXIT_AGE_MS + 1L))
        assertEquals(Decision.SKIP_NO_UPDATE_EXIT, UpdateResumePolicy.decide(true, exits, now))
    }

    @Test
    fun `exit age boundary - exactly the window counts, one millisecond more does not`() {
        val atLimit = ProcessExitRecord(UpdateResumePolicy.EXIT_REASON_PACKAGE_UPDATED, now - UpdateResumePolicy.MAX_EXIT_AGE_MS, null)
        val past = ProcessExitRecord(UpdateResumePolicy.EXIT_REASON_PACKAGE_UPDATED, now - UpdateResumePolicy.MAX_EXIT_AGE_MS - 1L, null)
        assertTrue(UpdateResumePolicy.isUpdateKill(atLimit, now))
        assertFalse(UpdateResumePolicy.isUpdateKill(past, now))
    }

    @Test
    fun `small clock skew into the future is tolerated, a large one is not`() {
        val slightlyAhead = ProcessExitRecord(UpdateResumePolicy.EXIT_REASON_PACKAGE_UPDATED, now + 30_000L, null)
        val farAhead = ProcessExitRecord(UpdateResumePolicy.EXIT_REASON_PACKAGE_UPDATED, now + 3_600_000L, null)
        assertTrue(UpdateResumePolicy.isUpdateKill(slightlyAhead, now))
        assertFalse(UpdateResumePolicy.isUpdateKill(farAhead, now))
    }

    @Test
    fun `API 31-33 fallback - installPackageLI description counts even with another reason code`() {
        val record = exit(reason = 13, ageMs = 2_000L, description = "stop com.rudi.audioplayer due to installPackageLI")
        assertTrue(UpdateResumePolicy.isUpdateKill(record, now))
    }

    @Test
    fun `unrelated description with a non-update reason does not count`() {
        assertFalse(UpdateResumePolicy.isUpdateKill(exit(13, 2_000L, "stop com.rudi.audioplayer due to swipe"), now))
        assertFalse(UpdateResumePolicy.isUpdateKill(exit(13, 2_000L, null), now))
    }

    @Test
    fun `an update exit anywhere in the list is enough regardless of order`() {
        val exits = listOf(
            exit(reason = 3, ageMs = 500_000L),
            exit(UpdateResumePolicy.EXIT_REASON_PACKAGE_UPDATED, 3_000L),
            exit(reason = 4, ageMs = 900_000L)
        )
        assertEquals(Decision.RESUME, UpdateResumePolicy.decide(true, exits.reversed(), now))
        assertEquals(Decision.RESUME, UpdateResumePolicy.decide(true, exits, now))
    }

    @Test
    fun `intendsToPlay needs playWhenReady, a queue and a state other than IDLE or ENDED`() {
        val idle = 1
        val buffering = 2
        val ready = 3
        val ended = 4
        assertTrue(UpdateResumePolicy.intendsToPlay(true, 135, ready))
        assertTrue("buffering sementara masih berniat memutar", UpdateResumePolicy.intendsToPlay(true, 1, buffering))
        assertFalse("jeda pengguna", UpdateResumePolicy.intendsToPlay(false, 135, ready))
        assertFalse("antrean kosong", UpdateResumePolicy.intendsToPlay(true, 0, ready))
        assertFalse("stop() / dismiss mini player", UpdateResumePolicy.intendsToPlay(true, 135, idle))
        assertFalse("akhir antrean", UpdateResumePolicy.intendsToPlay(true, 135, ended))
    }

    @Test
    fun `intendsToPlay state constants match androidx media3 Player STATE values`() {
        // Player.STATE_IDLE=1, STATE_BUFFERING=2, STATE_READY=3, STATE_ENDED=4 (nilai tetap API publik Media3).
        // Tes JVM murni tak memuat Media3; angka dikunci di sini sebagai dokumentasi + penjaga regresi.
        assertFalse(UpdateResumePolicy.intendsToPlay(true, 1, 1))
        assertFalse(UpdateResumePolicy.intendsToPlay(true, 1, 4))
        assertTrue(UpdateResumePolicy.intendsToPlay(true, 1, 2))
        assertTrue(UpdateResumePolicy.intendsToPlay(true, 1, 3))
    }
}
