package com.rudi.audioplayer.playback

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/** Detects a deliberate shake gesture via the accelerometer — only meant to be registered
 * while music is actively playing, both to save battery and to avoid random pocket jostling
 * triggering skips when nothing is even playing. */
class ShakeDetector(context: Context, private val onShake: () -> Unit) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastShakeTime = 0L

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            // Magnitude of acceleration minus gravity's own ~9.8 baseline — a resting or
            // gently-moving phone stays close to 0 here; a real shake spikes well above it.
            val gForce = sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH
            if (gForce > SHAKE_THRESHOLD) {
                val now = System.currentTimeMillis()
                // Debounced so one shake gesture (several accelerometer samples in a row)
                // fires exactly one skip, not a burst of them.
                if (now - lastShakeTime > DEBOUNCE_MS) {
                    lastShakeTime = now
                    onShake()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        accelerometer?.let {
            sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(listener)
    }

    companion object {
        private const val SHAKE_THRESHOLD = 18.0
        private const val DEBOUNCE_MS = 1200L
    }
}
