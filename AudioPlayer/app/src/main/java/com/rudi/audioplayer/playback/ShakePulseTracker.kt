package com.rudi.audioplayer.playback

/**
 * Pure pulse-counting state machine behind the shake-to-skip gesture, extracted out of
 * [ShakeDetector] so the logic itself can be unit-tested (src/test, plain JVM) without
 * needing a real android.hardware.SensorEvent/SensorManager. [ShakeDetector] still owns all
 * sensor wiring; this class only knows "a sample above threshold arrived at time X" and
 * decides whether that completes a confirmed shake.
 *
 * Extracted specifically because the pulse-counting rule here (Batch 25) was the fix for a
 * real user-reported bug — songs skipping on their own from pocket jostling — and had never
 * been directly verified, only reasoned about from reading the code. See PROJECT_STATE.md
 * Batch 27 for the verification this unlocked.
 */
class ShakePulseTracker(
    private val requiredPulses: Int = REQUIRED_PULSES,
    private val pulseWindowMs: Long = PULSE_WINDOW_MS,
    private val minPulseGapMs: Long = MIN_PULSE_GAP_MS,
    private val debounceMs: Long = DEBOUNCE_MS
) {
    private var lastShakeTime: Long? = null
    private var lastPulseTime: Long? = null
    private var pulseCount = 0
    private var pulseWindowStart: Long? = null

    /**
     * Feed one already-above-threshold sample timestamped [now] (millis, same clock as
     * System.currentTimeMillis() in production). Returns true exactly on the sample that
     * completes a confirmed shake ([requiredPulses] pulses inside [pulseWindowMs]).
     */
    fun onSample(now: Long): Boolean {
        // Cooldown after a confirmed shake already fired — prevents the tail end of the same
        // physical gesture from immediately starting a new pulse count. Nothing to debounce
        // against until a shake has actually fired once (lastShakeTime == null).
        lastShakeTime?.let { if (now - it < debounceMs) return false }

        // Ignore samples too close to the last counted pulse — several accelerometer readings
        // in a row from one single motion shouldn't count as separate pulses.
        lastPulseTime?.let { if (now - it < minPulseGapMs) return false }

        val windowStart = pulseWindowStart
        if (windowStart == null || now - windowStart > pulseWindowMs) {
            // Window expired (or this is the first pulse) — start counting fresh.
            pulseWindowStart = now
            pulseCount = 1
        } else {
            pulseCount += 1
        }
        lastPulseTime = now

        if (pulseCount >= requiredPulses) {
            lastShakeTime = now
            pulseCount = 0
            return true
        }
        return false
    }

    /** Called when the detector (re)starts listening — clears any in-progress pulse count. */
    fun reset() {
        pulseCount = 0
        pulseWindowStart = null
    }

    companion object {
        // A deliberate shake is several quick back-and-forth spikes; pocket/bag jostling from
        // walking is far less rhythmic and rarely clears this many pulses inside the window.
        const val REQUIRED_PULSES = 3
        const val PULSE_WINDOW_MS = 900L
        const val MIN_PULSE_GAP_MS = 100L
        const val DEBOUNCE_MS = 1200L
    }
}
