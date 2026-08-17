package com.rudi.audioplayer.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Batch 103 (Gap List #2, Integration/device testing playback). Real [MediaController] talking
 * to the real, running [PlaybackService] on a device/emulator — see
 * [PlaybackServiceTestHelper] for the connection plumbing and why the test tracks are synthetic
 * WAVs, and .github/workflows/build.yml's "instrumentation-tests" job for where this actually
 * runs (separate job from the release `build` job — a flaky/slow emulator here never blocks a
 * release).
 *
 * Covers the part of the gap list's device-testing checklist that a plain instrumentation test
 * (no real Bluetooth stack, no real lock screen, no real headset hardware) can actually exercise
 * honestly: play/pause, seek, next/previous, all three repeat modes, and the shuffle toggle. The
 * rest of that checklist — Bluetooth/media output, lock-screen controls, notification tap
 * targets, physical headset buttons, process death on a real OS, and Android 15/16-specific
 * platform behavior — needs a real device and a human, or dedicated UI Automator/Bluetooth-mock
 * tooling well beyond this batch's scope; faking a "pass" for those here would be dishonest, so
 * they're tracked instead in MANUAL_QA_CHECKLIST.md at the repo root.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackTransportTest {

    private lateinit var controller: MediaController

    private val h = PlaybackServiceTestHelper

    @Before
    fun setUp() {
        controller = h.connect()
        val queue = h.testQueue()
        h.onMain {
            controller.pause()
            controller.clearMediaItems()
            controller.repeatMode = Player.REPEAT_MODE_OFF
            controller.shuffleModeEnabled = false
            controller.setMediaItems(queue)
            controller.prepare()
        }
    }

    @After
    fun tearDown() {
        h.onMain {
            controller.stop()
            controller.clearMediaItems()
        }
        h.disconnect(controller)
    }

    @Test
    fun playThenPause_updatesIsPlaying() {
        h.onMain { controller.play() }
        assertTrue(
            "isPlaying tidak jadi true dalam batas waktu",
            h.waitUntil(TIMEOUT_MS) { controller.isPlaying }
        )

        h.onMain { controller.pause() }
        assertTrue(
            "isPlaying tidak jadi false dalam batas waktu",
            h.waitUntil(TIMEOUT_MS) { !controller.isPlaying }
        )
    }

    @Test
    fun seekTo_movesPlaybackPosition() {
        h.onMain { controller.play() }
        assertTrue(h.waitUntil(TIMEOUT_MS) { controller.isPlaying })

        val target = 1500L
        h.onMain { controller.seekTo(target) }
        assertTrue(
            "Posisi tidak mendekati target seek dalam batas waktu",
            h.waitUntil(TIMEOUT_MS) { kotlin.math.abs(controller.currentPosition - target) < 500L }
        )
    }

    @Test
    fun skipToNext_advancesToSecondTrack() {
        h.onMain { controller.play() }
        assertTrue(h.waitUntil(TIMEOUT_MS) { controller.isPlaying })

        h.onMain { controller.seekToNextMediaItem() }
        assertTrue(
            "Tidak berpindah ke track kedua dalam batas waktu",
            h.waitUntil(TIMEOUT_MS) { controller.currentMediaItemIndex == 1 }
        )
        assertEquals(1, h.onMain { controller.currentMediaItemIndex })
    }

    @Test
    fun skipToPrevious_returnsToFirstTrack() {
        h.onMain {
            controller.play()
            controller.seekToNextMediaItem()
        }
        assertTrue(h.waitUntil(TIMEOUT_MS) { controller.currentMediaItemIndex == 1 })

        h.onMain { controller.seekToPreviousMediaItem() }
        assertTrue(
            "Tidak kembali ke track pertama dalam batas waktu",
            h.waitUntil(TIMEOUT_MS) { controller.currentMediaItemIndex == 0 }
        )
    }

    @Test
    fun repeatModeOne_loopsSameTrackPastItsOwnDuration() {
        h.onMain {
            controller.repeatMode = Player.REPEAT_MODE_ONE
            // Track A (index 0) sengaja dipilih krn durasinya (3s) lebih pendek dari total
            // timeout tunggu di bawah, supaya test ini benar sampai melewati akhir alami
            // track-nya sendiri — bukan cuma cek nilai setter repeatMode.
            controller.seekTo(0, 0L)
            controller.play()
        }
        assertTrue(h.waitUntil(TIMEOUT_MS) { controller.isPlaying })

        // Tunggu lebih lama dari durasi track A (3000ms) — kalau repeat-one bekerja benar,
        // ExoPlayer mengulang track yang SAMA dari awal, bukan lanjut ke track index 1.
        Thread.sleep(PlaybackServiceTestHelper.TRACK_A_DURATION_MS + 800L)
        assertEquals(
            "repeat-one seharusnya tetap di track index 0 setelah lewat durasi aslinya",
            0,
            h.onMain { controller.currentMediaItemIndex }
        )

        h.onMain { controller.repeatMode = Player.REPEAT_MODE_OFF }
    }

    @Test
    fun repeatModeAll_wrapsFromLastTrackBackToFirst() {
        h.onMain {
            controller.repeatMode = Player.REPEAT_MODE_ALL
            controller.seekTo(1, 0L)
        }
        assertEquals(1, h.onMain { controller.currentMediaItemIndex })

        h.onMain { controller.seekToNextMediaItem() }
        assertTrue(
            "repeat-all seharusnya kembali ke track index 0 setelah track terakhir",
            h.waitUntil(TIMEOUT_MS) { controller.currentMediaItemIndex == 0 }
        )

        h.onMain { controller.repeatMode = Player.REPEAT_MODE_OFF }
    }

    @Test
    fun shuffleToggle_reportsEnabledState() {
        h.onMain { controller.shuffleModeEnabled = true }
        assertTrue(h.onMain { controller.shuffleModeEnabled })

        h.onMain { controller.shuffleModeEnabled = false }
        assertFalse(h.onMain { controller.shuffleModeEnabled })
    }

    companion object {
        private const val TIMEOUT_MS = 8000L
    }
}
