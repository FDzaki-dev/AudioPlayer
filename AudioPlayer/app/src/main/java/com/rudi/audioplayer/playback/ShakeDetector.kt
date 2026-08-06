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
    // Pulse-counting state machine lives in ShakePulseTracker (playback/ShakePulseTracker.kt)
    // so it can be unit-tested directly — this class only owns sensor wiring.
    private val pulseTracker = ShakePulseTracker()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            // Magnitude of acceleration minus gravity's own ~9.8 baseline — a resting or
            // gently-moving phone stays close to 0 here; a real shake spikes well above it.
            val gForce = sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH
            if (gForce <= SHAKE_THRESHOLD) return

            if (pulseTracker.onSample(System.currentTimeMillis())) {
                onShake()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        pulseTracker.reset()
        accelerometer?.let {
            sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(listener)
        pulseTracker.reset()
    }

    companion object {
        private const val SHAKE_THRESHOLD = 18.0
    }
}
