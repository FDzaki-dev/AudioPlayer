package com.rudi.audioplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.MoreExecutors
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Batch 103 (Gap List #2, Integration/device testing playback). Shared boilerplate for
 * instrumentation tests (app/src/androidTest — real device/emulator only, see
 * app/build.gradle.kts' `testInstrumentationRunner` and .github/workflows/build.yml's
 * "instrumentation-tests" job) that connect a real [MediaController] to the real, already
 * running [PlaybackService]. Every actual assertion lives in the test classes that use this —
 * this file only builds the connection and the tiny synthetic test queue, so that plumbing only
 * needs writing (and, if wrong, fixing) once instead of once per test class.
 *
 * ## Why the two test tracks are synthetic, generated WAV files, not real songs
 * `test_tone_a.wav` (440Hz, 3s) and `test_tone_b.wav` (660Hz, 2s), both in
 * app/src/androidTest/assets/, were generated locally with Python's stdlib `wave` module — pure
 * sine tones, nothing copyrighted, and no network access needed to fetch a sample. Tests that
 * care about "the other track" rely on the two durations being different, not on recognizing a
 * tone by ear.
 *
 * ## Why the WAV bytes get copied to cacheDir instead of played via `asset:///`
 * ExoPlayer's `asset:///` URI scheme resolves against whichever [Context] built the player — and
 * the real ExoPlayer instances live inside [PlaybackService], built with the *app's own* Context
 * (`app/src/main/...`), not the test APK's Context. Instrumented tests run in the same process as
 * the app under test, but androidTest assets are packaged into a *separate* test APK, only
 * reachable via `InstrumentationRegistry.getInstrumentation().context.assets` — the app's own
 * ExoPlayer can never see them at `asset:///`. Copying the bytes into the app's own `cacheDir`
 * once per test run and playing them via a plain `file://` Uri sidesteps that mismatch entirely
 * and needs no extra permission (an app's own cache dir is always readable/writable by itself).
 *
 * ## Why controller connection/release both go through `runOnMainSync`
 * [MediaController.Builder.buildAsync] creates a Handler tied to `Looper.myLooper()` of the
 * calling thread, so it must be called from a thread that has one (the main thread, via
 * [InstrumentationRegistry.getInstrumentation].runOnMainSync — a background instrumentation test
 * thread does not have one by default and would throw). The wait for connection to complete
 * happens OUTSIDE that block, on the calling test thread via a listener + latch — awaiting
 * (blocking) the main thread itself here would deadlock, since the connection handshake needs
 * that same main thread's message queue free to actually complete.
 */
object PlaybackServiceTestHelper {

    const val TRACK_A_DURATION_MS = 3000L
    const val TRACK_B_DURATION_MS = 2000L
    private const val CONNECT_TIMEOUT_SECONDS = 15L

    fun connect(): MediaController {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val latch = CountDownLatch(1)
        var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            future = MediaController.Builder(context, token).buildAsync().also {
                it.addListener({ latch.countDown() }, MoreExecutors.directExecutor())
            }
        }

        check(latch.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            "MediaController tidak berhasil connect ke PlaybackService dalam ${CONNECT_TIMEOUT_SECONDS}s"
        }
        return checkNotNull(future).get()
    }

    fun disconnect(controller: MediaController) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            controller.release()
        }
    }

    /** Copies the two synthetic sine-tone WAVs from the test APK's assets into the app-under-
     * test's own cacheDir (see class doc for why) and returns them as a ready-to-play queue. */
    fun testQueue(): List<MediaItem> {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val fileA = copyAssetToCache(context, "test_tone_a.wav")
        val fileB = copyAssetToCache(context, "test_tone_b.wav")
        return listOf(
            MediaItem.Builder().setMediaId("test-track-a").setUri(Uri.fromFile(fileA)).build(),
            MediaItem.Builder().setMediaId("test-track-b").setUri(Uri.fromFile(fileB)).build()
        )
    }

    private fun copyAssetToCache(context: Context, assetName: String): File {
        val outFile = File(context.cacheDir, assetName)
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        instrumentationContext.assets.open(assetName).use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }
        return outFile
    }

    /** Runs [action] synchronously on the main thread and returns its result — every
     * [MediaController] call must happen on the same thread it was built on (see class doc). */
    fun <T> onMain(action: () -> T): T {
        var result: T? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = action() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /** Polls [condition] (itself run via [onMain]) until it's true or [timeoutMs] elapses.
     * Playback state changes are asynchronous (real decode/session round-trip), so tests assert
     * on this rather than on state read immediately after issuing a command. */
    fun waitUntil(timeoutMs: Long, pollMs: Long = 100L, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (onMain(condition)) return true
            Thread.sleep(pollMs)
        }
        return false
    }
}
