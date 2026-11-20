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
    fun `the newest exit decides regardless of list order`() {
        val exits = listOf(
            exit(reason = 3, ageMs = 500_000L),
            exit(UpdateResumePolicy.EXIT_REASON_PACKAGE_UPDATED, 3_000L),
            exit(reason = 4, ageMs = 900_000L)
        )
        assertEquals(Decision.RESUME, UpdateResumePolicy.decide(true, exits.reversed(), now))
        assertEquals(Decision.RESUME, UpdateResumePolicy.decide(true, exits, now))
    }

    @Test
    fun `a newer non-update exit makes an older update exit stale - OEM kill between two updates`() {
        // update #1 (5 mnt lalu, proses dilanjutkan) -> OEM membunuh proses 2 mnt lalu (penanda tetap true) -> update #2 saat app mati
        val exits = listOf(
            exit(reason = 13, ageMs = 120_000L, description = "reclaim_process_start"),
            exit(UpdateResumePolicy.EXIT_REASON_PACKAGE_UPDATED, 300_000L, "stop com.rudi.audioplayer due to installPackageLI")
        )
        assertEquals(Decision.SKIP_NO_UPDATE_EXIT, UpdateResumePolicy.decide(true, exits, now))
        assertEquals(Decision.SKIP_NO_UPDATE_EXIT, UpdateResumePolicy.decide(true, exits.reversed(), now))
    }

    @Test
    fun `newest is chosen by timestamp not by list position`() {
        val exits = listOf(
            exit(reason = 3, ageMs = 800_000L),
            exit(UpdateResumePolicy.EXIT_REASON_PACKAGE_UPDATED, 2_000L),
            exit(reason = 13, ageMs = 4_000_000L)
        )
        assertEquals(Decision.RESUME, UpdateResumePolicy.decide(true, exits, now))
        assertEquals(Decision.RESUME, UpdateResumePolicy.decide(true, exits.reversed(), now))
    }

    @Test
    fun `an update exit sharing the newest timestamp with another record still counts`() {
        val exits = listOf(
            exit(reason = 13, ageMs = 2_000L),
            exit(UpdateResumePolicy.EXIT_REASON_PACKAGE_UPDATED, 2_000L)
        )
        assertEquals(Decision.RESUME, UpdateResumePolicy.decide(true, exits, now))
    }

    @Test
    fun `splice allowed only while the player still holds exactly the restored head item`() {
        assertTrue(UpdateResumePolicy.canSpliceRestoredQueue("361288", 361288L, 1))
    }

    @Test
    fun `splice refused when the queue was replaced or already grown`() {
        assertFalse("antrean sudah >1 (user/komponen lain)", UpdateResumePolicy.canSpliceRestoredQueue("361288", 361288L, 2))
        assertFalse("antrean kosong", UpdateResumePolicy.canSpliceRestoredQueue(null, 361288L, 0))
    }

    @Test
    fun `splice refused when the single item is a different song`() {
        assertFalse(UpdateResumePolicy.canSpliceRestoredQueue("42", 361288L, 1))
        assertFalse(UpdateResumePolicy.canSpliceRestoredQueue(null, 361288L, 1))
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

    @Test
    fun `Batch 693 rest is missing only when others were saved but none were found`() {
        assertTrue(UpdateResumePolicy.isRestMissing(foundOthers = 0, expectedOthers = 134))
        assertFalse(UpdateResumePolicy.isRestMissing(foundOthers = 120, expectedOthers = 134)) // sebagian dihapus = bukan galat
        assertFalse(UpdateResumePolicy.isRestMissing(foundOthers = 0, expectedOthers = 0)) // antrean 1 lagu memang tanpa sisa
    }

    @Test
    fun `Batch 693 empty rest is retried until attempts run out`() {
        val max = UpdateResumePolicy.REST_SPLICE_MAX_ATTEMPTS
        assertTrue(UpdateResumePolicy.shouldRetryRestSplice(0, 134, attempt = 1, maxAttempts = max))
        assertFalse(UpdateResumePolicy.shouldRetryRestSplice(0, 134, attempt = max, maxAttempts = max))
    }

    @Test
    fun `Batch 693 partial or complete rest is never retried`() {
        assertFalse(UpdateResumePolicy.shouldRetryRestSplice(1, 134, attempt = 1, maxAttempts = 2))
        assertFalse(UpdateResumePolicy.shouldRetryRestSplice(134, 134, attempt = 1, maxAttempts = 2))
        assertFalse(UpdateResumePolicy.shouldRetryRestSplice(0, 0, attempt = 1, maxAttempts = 2))
    }

    // ---- Batch 695: posisi pulih = posisi tersimpan + (waktu mati - waktu simpan) x kecepatan ----

    private val killAt = 1_795_154_947_317L // ApplicationExitInfo log v694: 2026-11-20 14:09:07,317 (+08:00)

    @Test
    fun `Batch 695 exact v694 log case - saved 3_5 s before the kill resumes at the real position, not 3_5 s back`() {
        // log v694: simpan berkala 14:09:03,829 posisi 4.871.249; mati 14:09:07,317; posisi nyata saat mati ±4.874.8xx.
        val r = UpdateResumePolicy.resumePosition(
            savedPositionMs = 4_871_249L, savedAtMs = killAt - 3_488L, playingSinceMs = killAt - 14_000L,
            killedAtMs = killAt, speed = 1.0f, durationMs = 5_416_000L
        )
        assertEquals(4_874_737L, r.positionMs)
        assertEquals(3_488L, r.extrapolatedMs)
        assertEquals(UpdateResumePolicy.PositionBasis.EXTRAPOLATED, r.basis)
    }

    @Test
    fun `Batch 695 playing started after the save - only time since play restarted counts`() {
        // simpan saat jeda (posisi beku), lalu putar lagi 4 dtk sebelum mati -> hanya 4 dtk yang ditambah, bukan seluruh jeda.
        val r = UpdateResumePolicy.resumePosition(
            savedPositionMs = 100_000L, savedAtMs = killAt - 20_000L, playingSinceMs = killAt - 4_000L,
            killedAtMs = killAt, speed = 1.0f, durationMs = 300_000L
        )
        assertEquals(104_000L, r.positionMs)
        assertEquals(4_000L, r.extrapolatedMs)
    }

    @Test
    fun `Batch 695 playback speed scales the added time`() {
        val r = UpdateResumePolicy.resumePosition(50_000L, killAt - 4_000L, 0L, killAt, 1.5f, 300_000L)
        assertEquals(56_000L, r.positionMs)
        assertEquals(6_000L, r.extrapolatedMs)
    }

    @Test
    fun `Batch 695 invalid speed is treated as 1x`() {
        assertEquals(53_000L, UpdateResumePolicy.resumePosition(50_000L, killAt - 3_000L, 0L, killAt, 0f, 300_000L).positionMs)
        assertEquals(53_000L, UpdateResumePolicy.resumePosition(50_000L, killAt - 3_000L, 0L, killAt, Float.NaN, 300_000L).positionMs)
        assertEquals(53_000L, UpdateResumePolicy.resumePosition(50_000L, killAt - 3_000L, 0L, killAt, -2f, 300_000L).positionMs)
    }

    @Test
    fun `Batch 695 unknown kill time or save stamp keeps the saved position untouched`() {
        val noKill = UpdateResumePolicy.resumePosition(50_000L, killAt - 3_000L, 0L, 0L, 1f, 300_000L)
        assertEquals(50_000L, noKill.positionMs)
        assertEquals(UpdateResumePolicy.PositionBasis.NO_KILL_TIME, noKill.basis)
        val noStamp = UpdateResumePolicy.resumePosition(50_000L, 0L, 0L, killAt, 1f, 300_000L)
        assertEquals(50_000L, noStamp.positionMs)
        assertEquals(0L, noStamp.extrapolatedMs)
        assertEquals(UpdateResumePolicy.PositionBasis.NO_SAVE_STAMP, noStamp.basis)
    }

    @Test
    fun `Batch 695 zero or negative gap (clock stepped back) never moves the position`() {
        val zero = UpdateResumePolicy.resumePosition(50_000L, killAt, 0L, killAt, 1f, 300_000L)
        val negative = UpdateResumePolicy.resumePosition(50_000L, killAt + 5_000L, 0L, killAt, 1f, 300_000L)
        assertEquals(UpdateResumePolicy.PositionBasis.NO_GAP, zero.basis)
        assertEquals(UpdateResumePolicy.PositionBasis.NO_GAP, negative.basis)
        assertEquals(50_000L, zero.positionMs)
        assertEquals(50_000L, negative.positionMs)
    }

    @Test
    fun `Batch 695 gap boundary - exactly the cap extrapolates, one millisecond more does not`() {
        val cap = UpdateResumePolicy.MAX_POSITION_EXTRAPOLATION_MS
        val atCap = UpdateResumePolicy.resumePosition(50_000L, killAt - cap, 0L, killAt, 1f, 0L)
        val past = UpdateResumePolicy.resumePosition(50_000L, killAt - cap - 1L, 0L, killAt, 1f, 0L)
        assertEquals(UpdateResumePolicy.PositionBasis.EXTRAPOLATED, atCap.basis)
        assertEquals(50_000L + cap, atCap.positionMs)
        assertEquals(UpdateResumePolicy.PositionBasis.GAP_TOO_LARGE, past.basis)
        assertEquals(50_000L, past.positionMs)
    }

    @Test
    fun `Batch 695 result near the end of the song is cancelled - never jump to the end`() {
        val margin = UpdateResumePolicy.POSITION_END_MARGIN_MS
        // 5 dtk sebelum akhir + 4 dtk tambahan = 1 dtk dari akhir (< margin) -> dibatalkan
        val near = UpdateResumePolicy.resumePosition(295_000L, killAt - 4_000L, 0L, killAt, 1f, 300_000L)
        assertEquals(UpdateResumePolicy.PositionBasis.NEAR_SONG_END, near.basis)
        assertEquals(295_000L, near.positionMs)
        // tepat sejauh margin dari akhir masih diizinkan
        val edge = UpdateResumePolicy.resumePosition(300_000L - margin - 3_000L, killAt - 3_000L, 0L, killAt, 1f, 300_000L)
        assertEquals(UpdateResumePolicy.PositionBasis.EXTRAPOLATED, edge.basis)
        assertEquals(300_000L - margin, edge.positionMs)
    }

    @Test
    fun `Batch 695 unknown duration still extrapolates - the end guard only applies when the length is known`() {
        val r = UpdateResumePolicy.resumePosition(50_000L, killAt - 3_000L, 0L, killAt, 1f, 0L)
        assertEquals(UpdateResumePolicy.PositionBasis.EXTRAPOLATED, r.basis)
        assertEquals(53_000L, r.positionMs)
    }

    @Test
    fun `Batch 695 playing-since in the future relative to the kill is ignored as no gap`() {
        val r = UpdateResumePolicy.resumePosition(50_000L, killAt - 3_000L, killAt + 1L, killAt, 1f, 300_000L)
        assertEquals(UpdateResumePolicy.PositionBasis.NO_GAP, r.basis)
        assertEquals(50_000L, r.positionMs)
    }
}
