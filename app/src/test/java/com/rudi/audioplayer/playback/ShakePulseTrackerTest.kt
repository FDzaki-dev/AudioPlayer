package com.rudi.audioplayer.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakePulseTrackerTest {

    @Test
    fun `a single pulse never fires a shake`() {
        val tracker = ShakePulseTracker()
        assertFalse(tracker.onSample(0L))
    }

    @Test
    fun `three pulses inside the window fire a shake on the third`() {
        val tracker = ShakePulseTracker()
        assertFalse(tracker.onSample(0L))
        assertFalse(tracker.onSample(300L))
        assertTrue(tracker.onSample(600L))
    }

    @Test
    fun `pulses spread beyond the window do not accumulate`() {
        val tracker = ShakePulseTracker()
        assertFalse(tracker.onSample(0L))
        assertFalse(tracker.onSample(300L))
        // 1300ms after the window start (900ms) -> counter resets to 1, not a shake yet
        assertFalse(tracker.onSample(1300L))
    }

    @Test
    fun `a lone spike every few seconds never fires - this is the pocket-jostling regression case`() {
        // This is exactly the scenario Batch 25 fixed: isolated single spikes from walking,
        // each far outside the 900ms pulse window from the last, must never accumulate.
        val tracker = ShakePulseTracker()
        assertFalse(tracker.onSample(0L))
        assertFalse(tracker.onSample(2000L))
        assertFalse(tracker.onSample(5000L))
        assertFalse(tracker.onSample(9000L))
    }

    @Test
    fun `samples too close to a real pulse are ignored, delaying when the shake threshold is reached`() {
        val tracker = ShakePulseTracker()
        assertFalse(tracker.onSample(0L)) // real pulse 1
        assertFalse(tracker.onSample(50L)) // ignored: <100ms gap from pulse 1
        assertFalse(tracker.onSample(300L)) // real pulse 2
        assertFalse(tracker.onSample(350L)) // ignored: <100ms gap from pulse 2
        // Naively counting every sample, this would already be the 5th — well past the
        // threshold. Correctly ignoring the two too-close samples, it's only the 3rd real
        // pulse, and this is exactly where the shake should fire.
        assertTrue(tracker.onSample(600L))
    }

    @Test
    fun `debounce blocks any new counting right after a confirmed shake`() {
        val tracker = ShakePulseTracker()
        assertFalse(tracker.onSample(0L))
        assertFalse(tracker.onSample(300L))
        assertTrue(tracker.onSample(600L))
        // 400ms later, inside the 1200ms debounce -> ignored entirely, not even counted
        assertFalse(tracker.onSample(1000L))
    }

    @Test
    fun `a fresh shake can fire again once debounce has cleared`() {
        val tracker = ShakePulseTracker()
        assertFalse(tracker.onSample(0L))
        assertFalse(tracker.onSample(300L))
        assertTrue(tracker.onSample(600L))
        // 1300ms after the confirmed shake, past the 1200ms debounce -> counting resumes
        assertFalse(tracker.onSample(1900L))
        assertFalse(tracker.onSample(2200L))
        assertTrue(tracker.onSample(2500L))
    }

    @Test
    fun `reset clears an in-progress pulse count`() {
        val tracker = ShakePulseTracker()
        assertFalse(tracker.onSample(0L))
        assertFalse(tracker.onSample(300L))
        tracker.reset()
        // without the reset this would be pulse 3 and fire; after reset it's pulse 1
        assertFalse(tracker.onSample(600L))
    }
}
