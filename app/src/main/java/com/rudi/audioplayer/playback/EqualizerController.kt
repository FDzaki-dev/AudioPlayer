package com.rudi.audioplayer.playback

import android.content.Context
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerBand(
    val index: Int,
    val frequencyHz: Int,
    val levelMillibel: Short
)

data class EqualizerUiState(
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val minLevel: Short = -1500,
    val maxLevel: Short = 1500,
    val bands: List<EqualizerBand> = emptyList(),
    val presets: List<String> = emptyList(),
    val selectedPreset: Int = -1
)

/**
 * Wraps the platform's android.media.audiofx.Equalizer, attached to the
 * current playback's audio session ID. Settings persist across sessions
 * via SharedPreferences and are re-applied whenever the session changes.
 */
class EqualizerController(private val context: Context) {

    private var equalizer: Equalizer? = null
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(EqualizerUiState())
    val state: StateFlow<EqualizerUiState> = _state.asStateFlow()

    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId == 0) return

        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq

            val bandCount = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val presetCount = eq.numberOfPresets.toInt()
            val presetNames = (0 until presetCount).map { eq.getPresetName(it.toShort()) }

            val savedEnabled = prefs.getBoolean(KEY_ENABLED, false)
            eq.setEnabled(savedEnabled)

            for (i in 0 until bandCount) {
                val saved = prefs.getInt(KEY_BAND_PREFIX + i, Int.MIN_VALUE)
                if (saved != Int.MIN_VALUE) {
                    eq.setBandLevel(i.toShort(), saved.toShort())
                }
            }

            val bands = (0 until bandCount).map { i ->
                EqualizerBand(
                    index = i,
                    frequencyHz = eq.getCenterFreq(i.toShort()) / 1000,
                    levelMillibel = eq.getBandLevel(i.toShort())
                )
            }

            _state.value = EqualizerUiState(
                supported = true,
                enabled = savedEnabled,
                minLevel = range[0],
                maxLevel = range[1],
                bands = bands,
                presets = presetNames,
                selectedPreset = prefs.getInt(KEY_PRESET, -1)
            )
        } catch (e: Exception) {
            equalizer = null
            _state.value = EqualizerUiState(supported = false)
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.setEnabled(enabled)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _state.value = _state.value.copy(enabled = enabled)
    }

    fun setBandLevel(band: Int, level: Short) {
        equalizer?.setBandLevel(band.toShort(), level)
        prefs.edit()
            .putInt(KEY_BAND_PREFIX + band, level.toInt())
            .putInt(KEY_PRESET, -1)
            .apply()
        val updatedBands = _state.value.bands.map {
            if (it.index == band) it.copy(levelMillibel = level) else it
        }
        _state.value = _state.value.copy(bands = updatedBands, selectedPreset = -1)
    }

    fun usePreset(presetIndex: Int) {
        val eq = equalizer ?: return
        eq.usePreset(presetIndex.toShort())

        val editor = prefs.edit().putInt(KEY_PRESET, presetIndex)
        val updatedBands = _state.value.bands.map { band ->
            val newLevel = eq.getBandLevel(band.index.toShort())
            editor.putInt(KEY_BAND_PREFIX + band.index, newLevel.toInt())
            band.copy(levelMillibel = newLevel)
        }
        editor.apply()

        _state.value = _state.value.copy(bands = updatedBands, selectedPreset = presetIndex)
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }

    companion object {
        private const val PREFS_NAME = "equalizer"
        private const val KEY_ENABLED = "eq_enabled"
        private const val KEY_PRESET = "eq_preset"
        private const val KEY_BAND_PREFIX = "eq_band_"
    }
}
