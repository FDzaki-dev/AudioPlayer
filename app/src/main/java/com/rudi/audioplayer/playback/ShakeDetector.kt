package com.rudi.audioplayer.playback

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/** Detects a deliberate shake gesture via the accelerometer — only meant to be registered
 * while music is actively playing, both to save battery and to avoid random pocket jostling
 * triggering skips when nothing is even playing.
 *
 * This runs inside [PlaybackService] itself (started/stopped purely off isPlaying + the
 * setting), not [com.rudi.audioplayer.playback.PlayerViewModel] — so unlike the ViewModel's
 * own listener, it keeps running even after the Activity/ViewModel is torn down (app swiped
 * from Recents), as long as the foreground service is still playing something. A single
 * g-force spike used to be enough to fire a skip, which was indistinguishable from a phone
 * bouncing in a pocket/bag while walking — exactly the scenario where the app isn't in the
 * foreground to double-check. Now a real shake requires [REQUIRED_PULSES] separate spikes in
 * quick succession (a genuine back-and-forth shake), which ordinary carrying motion rarely
 * produces. */
class ShakeDetector(context: Context, private val onShake: () -> Unit) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastShakeTime = 0L
    private var lastPulseTime = 0L
    private var pulseCount = 0
    private var pulseWindowStart = 0L

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            // Magnitude of acceleration minus gravity's own ~9.8 baseline — a resting or
            // gently-moving phone stays close to 0 here; a real shake spikes well above it.
            val gForce = sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH
            if (gForce <= SHAKE_THRESHOLD) return

            val now = System.currentTimeMillis()

            // Cooldown after a confirmed shake already fired — prevents the tail end of the
            // same physical gesture from immediately starting a new pulse count.
            if (now - lastShakeTime < DEBOUNCE_MS) return

            // Ignore samples too close to the last counted pulse — several accelerometer
            // readings in a row from one single motion shouldn't count as separate pulses.
            if (now - lastPulseTime < MIN_PULSE_GAP_MS) return

            if (now - pulseWindowStart > PULSE_WINDOW_MS) {
                // Window expired (or this is the first pulse) — start counting fresh.
                pulseWindowStart = now
                pulseCount = 1
            } else {
                pulseCount += 1
            }
            lastPulseTime = now

            if (pulseCount >= REQUIRED_PULSES) {
                lastShakeTime = now
                pulseCount = 0
                onShake()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        pulseCount = 0
        accelerometer?.let {
            sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(listener)
        pulseCount = 0
    }

    companion object {
        private const val SHAKE_THRESHOLD = 18.0
        private const val DEBOUNCE_MS = 1200L
        // A deliberate shake is several quick back-and-forth spikes; pocket/bag jostling from
        // walking is far less rhythmic and rarely clears this many pulses inside the window.
        private const val REQUIRED_PULSES = 3
        private const val PULSE_WINDOW_MS = 900L
        private const val MIN_PULSE_GAP_MS = 100L
    }
}
