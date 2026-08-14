package com.rudi.audioplayer.playback

import android.media.audiofx.Visualizer
import com.rudi.audioplayer.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Wraps the platform's android.media.audiofx.Visualizer, attached to the current playback's
 * audio session ID — same sharing mechanism [EqualizerController] already uses via
 * [PlaybackAudioSession] (this ViewModel-side controller only ever talks to a MediaController,
 * which has no way to read ExoPlayer-specific properties like audioSessionId).
 *
 * Unlike [EqualizerController], which is queried on demand, this pushes FFT frames
 * asynchronously at a throttled rate via [Visualizer.OnDataCaptureListener] — [bars] only updates
 * while a listener is actually attached and enabled. Roadmap #9 (ROADMAP_15_FITUR_OFFLINE.md).
 *
 * Requires android.permission.RECORD_AUDIO (true for ANY audio session per the platform's own
 * class docs, not just session 0 / the output mix — there is no "it's my own audio" exception).
 * This controller does NOT request that permission itself; [attach] simply fails safe (caught
 * SecurityException, same as any other unsupported-device outcome) if it isn't granted yet. The
 * caller (VisualizerSheet / PlayerViewModel) is expected to already know whether permission is
 * granted before ever calling [attach] — see MainActivity.kt's visualizerPermissionLauncher.
 */
class AudioVisualizerController {

    private var visualizer: Visualizer? = null

    private val _supported = MutableStateFlow(true)
    val supported: StateFlow<Boolean> = _supported.asStateFlow()

    // Normalized (0f..1f) magnitude per bar, grouped from raw FFT bins. All-zero = nothing
    // captured yet (nothing playing, permission missing, or the listener hasn't fired its first
    // frame). A plain FloatArray (not ImmutableList) is fine here — this is redrawn every frame,
    // list immutability overhead would be pure waste for something this short-lived.
    private val _bars = MutableStateFlow(FloatArray(BAR_COUNT))
    val bars: StateFlow<FloatArray> = _bars.asStateFlow()

    /** Call only once the caller already knows RECORD_AUDIO is granted (never at app startup —
     * requesting a dangerous permission just to draw an optional visual effect is the UI layer's
     * call, not this controller's). Safe to call repeatedly; re-attaching releases the previous
     * instance first, same pattern as [EqualizerController.attach]. */
    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId == 0) return

        try {
            val viz = Visualizer(audioSessionId)
            // Must be a power of 2 within getCaptureSizeRange(); 512 sits comfortably inside the
            // typical [128, 1024] device range and gives enough FFT resolution to group into
            // BAR_COUNT bars without each bar being dominated by a single noisy bin.
            val range = Visualizer.getCaptureSizeRange()
            // setCaptureSize()/setEnabled() below both return an Int status code in the platform
            // class (not void) — Kotlin only synthesizes an assignable property from a Java
            // getter/setter pair when the setter returns Unit, so these must stay explicit method
            // calls, not `viz.captureSize = ...` / `viz.enabled = ...` property syntax (same
            // reason EqualizerController.kt already calls `eq.setEnabled(...)` explicitly).
            viz.setCaptureSize(512.coerceIn(range[0], range[1]))
            // Throttled well below the device max (usually 20fps/20000mHz) — the roadmap's own
            // risk note flags battery drain, and a spectrum bar doesn't need to be smoother than
            // ~15fps to read as "live" to the eye. getMaxCaptureRate() is `public static` in the
            // platform class (confirmed against AOSP source) — must be called on the class, not
            // an instance, or this won't compile.
            val rate = min(Visualizer.getMaxCaptureRate(), TARGET_CAPTURE_RATE_MILLIHZ)
            viz.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                    // Not subscribed to — captureWaveform = false below. Spectrum bars only.
                }

                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if (fft != null) _bars.value = magnitudeBars(fft)
                }
            }, rate, /* captureWaveform = */ false, /* captureFft = */ true)
            viz.setEnabled(true)
            visualizer = viz
            _supported.value = true
        } catch (e: Exception) {
            // Same class of ambiguity EqualizerController's own catch block already documents:
            // this covers both "device genuinely doesn't support Visualizer" AND "RECORD_AUDIO
            // not granted" (SecurityException) as one outcome. The caller already knows which one
            // applies from its own permission check before ever calling attach(), so it doesn't
            // need this to re-derive that distinction from the exception type.
            AppLogger.e("AudioVisualizerController", "Gagal attach visualizer ke sesi audio", e)
            visualizer = null
            _supported.value = false
        }
    }

    /** Converts the raw 8-bit FFT byte array into [BAR_COUNT] normalized magnitude bars, grouping
     * consecutive frequency bins per bar (low bars = bass, high bars = treble — bin index is
     * already frequency-ordered by the platform). Per [Visualizer.getFft]'s documented format:
     * fft[0] = DC magnitude, fft[1] = Nyquist magnitude (both real-only, no imaginary pair), then
     * alternating (real, imaginary) pairs from index 2 onward for the remaining bins. */
    private fun magnitudeBars(fft: ByteArray): FloatArray {
        val usableBins = (fft.size - 2) / 2
        if (usableBins <= 0) return FloatArray(BAR_COUNT)
        val binsPerBar = (usableBins / BAR_COUNT).coerceAtLeast(1)
        val out = FloatArray(BAR_COUNT)
        for (bar in 0 until BAR_COUNT) {
            var sum = 0f
            var count = 0
            val start = bar * binsPerBar
            val end = min(start + binsPerBar, usableBins)
            for (bin in start until end) {
                val idx = 2 + bin * 2
                if (idx + 1 >= fft.size) break
                val re = fft[idx].toInt()
                val im = fft[idx + 1].toInt()
                sum += sqrt((re * re + im * im).toFloat())
                count++
            }
            val avg = if (count > 0) sum / count else 0f
            // Raw magnitude has no fixed ceiling; 90f is an empirically comfortable clip point for
            // signed-byte FFT output at normal listening volume — loud transients clip to 1f (bar
            // hits full height) instead of the whole scale stretching per-frame, which would make
            // the baseline noise level flicker distractingly during quiet passages.
            out[bar] = (avg / 90f).coerceIn(0f, 1f)
        }
        return out
    }

    fun release() {
        visualizer?.let {
            try {
                it.setEnabled(false)
                it.release()
            } catch (e: Exception) {
                AppLogger.e("AudioVisualizerController", "Gagal release visualizer", e)
            }
        }
        visualizer = null
        _bars.value = FloatArray(BAR_COUNT)
    }

    companion object {
        const val BAR_COUNT = 24
        private const val TARGET_CAPTURE_RATE_MILLIHZ = 15000 // ~15fps
    }
}
