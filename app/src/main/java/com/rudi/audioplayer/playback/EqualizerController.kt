package com.rudi.audioplayer.playback

import android.content.Context
import android.media.audiofx.Equalizer
import com.rudi.audioplayer.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

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
    val selectedPreset: Int = -1,
    val boldPreset: String = "",
    // Batch 542 — preset EQ buatan pengguna (hanya yang jumlah band-nya cocok dgn perangkat ini).
    val userPresets: List<String> = emptyList(),
    val selectedUserPreset: String = ""
)

/** Batch 542 — hasil impor profil AutoEq. [usedFilters] = filter yang berhasil di-parse. */
data class EqImportResult(
    val applied: Boolean,
    val usedFilters: Int,
    val ignoredFilters: Int
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
                selectedPreset = prefs.getInt(KEY_PRESET, -1),
                boldPreset = prefs.getString(KEY_BOLD_PRESET, "") ?: "",
                userPresets = visibleUserPresetNames(bandCount),
                selectedUserPreset = prefs.getString(KEY_USER_PRESET, "") ?: ""
            )
        } catch (e: Exception) {
            // UI menampilkan ini persis sama dengan "device memang tidak mendukung equalizer" —
            // tapi keduanya bisa jadi beda kasus (mis. audioSessionId tidak valid, konflik efek
            // audio lain). Dicatat supaya kalau ada laporan "equalizer saya hilang" di device yang
            // seharusnya mendukung, penyebab sebenarnya tidak perlu ditebak dari nol.
            AppLogger.e("EqualizerController", "Gagal attach equalizer ke sesi audio", e)
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
        val eq = equalizer ?: return
        eq.setBandLevel(band.toShort(), level)
        // Adjusting a band is a clear signal the user wants to hear the effect —
        // don't make them separately remember to flip the enabled switch too.
        eq.setEnabled(true)
        prefs.edit()
            .putInt(KEY_BAND_PREFIX + band, level.toInt())
            .putInt(KEY_PRESET, -1)
            .putString(KEY_BOLD_PRESET, "")
            .putString(KEY_USER_PRESET, "")
            .putBoolean(KEY_ENABLED, true)
            .apply()
        val updatedBands = _state.value.bands.map {
            if (it.index == band) it.copy(levelMillibel = level) else it
        }
        _state.value = _state.value.copy(
            bands = updatedBands,
            selectedPreset = -1,
            enabled = true,
            boldPreset = "",
            selectedUserPreset = ""
        )
    }

    fun usePreset(presetIndex: Int) {
        val eq = equalizer ?: return
        eq.usePreset(presetIndex.toShort())
        eq.setEnabled(true)

        val editor = prefs.edit()
            .putInt(KEY_PRESET, presetIndex)
            .putString(KEY_BOLD_PRESET, "")
            .putString(KEY_USER_PRESET, "")
            .putBoolean(KEY_ENABLED, true)
        val updatedBands = _state.value.bands.map { band ->
            val newLevel = eq.getBandLevel(band.index.toShort())
            editor.putInt(KEY_BAND_PREFIX + band.index, newLevel.toInt())
            band.copy(levelMillibel = newLevel)
        }
        editor.apply()

        _state.value = _state.value.copy(
            bands = updatedBands,
            selectedPreset = presetIndex,
            enabled = true,
            boldPreset = "",
            selectedUserPreset = ""
        )
    }

    /**
     * Applies a hand-shaped, deliberately noticeable curve across whatever bands this device
     * actually has, instead of relying on the platform's often-subtle built-in presets. Intensity
     * is scaled to 85% of the device's real reported range so it stays clear of the hard clip edge.
     */
    fun useBoldPreset(preset: BoldPreset) {
        val eq = equalizer ?: return
        val bands = _state.value.bands
        if (bands.isEmpty()) return

        val maxRange = _state.value.maxLevel.toFloat()
        val editor = prefs.edit()
            .putInt(KEY_PRESET, -1)
            .putString(KEY_BOLD_PRESET, preset.name)
            .putString(KEY_USER_PRESET, "")
            .putBoolean(KEY_ENABLED, true)

        val updatedBands = bands.mapIndexed { i, band ->
            val fraction = if (bands.size == 1) 0f else i / (bands.size - 1).toFloat() // 0 = lowest band, 1 = highest
            val intensity = when (preset) {
                BoldPreset.FLAT -> 0f
                BoldPreset.BASS_BOOST -> ((1f - fraction).coerceIn(0f, 1f)).let { it * it }
                BoldPreset.TREBLE_BOOST -> fraction.let { it * it }
                BoldPreset.VOCAL_BOOST -> (1f - (2f * (fraction - 0.5f)) * (2f * (fraction - 0.5f))).coerceIn(0f, 1f)
            }
            val level = (intensity * maxRange * 0.85f).toInt().toShort()
            eq.setBandLevel(i.toShort(), level)
            editor.putInt(KEY_BAND_PREFIX + i, level.toInt())
            band.copy(levelMillibel = level)
        }
        eq.setEnabled(true)
        editor.apply()

        _state.value = _state.value.copy(
            bands = updatedBands,
            selectedPreset = -1,
            enabled = true,
            boldPreset = preset.name,
            selectedUserPreset = ""
        )
    }

    /**
     * Batch 542 — menerapkan level band (millibel) langsung, satu per band perangkat. Dipakai preset
     * pengguna + impor profil AutoEq. Ukuran [levels] HARUS = jumlah band; kalau tidak -> false, 0 efek.
     */
    fun applyBandLevels(levels: List<Short>, userPreset: String = ""): Boolean {
        val eq = equalizer ?: return false
        val bands = _state.value.bands
        if (bands.isEmpty() || levels.size != bands.size) return false

        val minLevel = _state.value.minLevel
        val maxLevel = _state.value.maxLevel
        val editor = prefs.edit()
            .putInt(KEY_PRESET, -1)
            .putString(KEY_BOLD_PRESET, "")
            .putString(KEY_USER_PRESET, userPreset)
            .putBoolean(KEY_ENABLED, true)

        val updatedBands = bands.mapIndexed { i, band ->
            val level = levels[i].coerceIn(minLevel, maxLevel)
            eq.setBandLevel(i.toShort(), level)
            editor.putInt(KEY_BAND_PREFIX + i, level.toInt())
            band.copy(levelMillibel = level)
        }
        eq.setEnabled(true)
        editor.apply()

        _state.value = _state.value.copy(
            bands = updatedBands,
            selectedPreset = -1,
            enabled = true,
            boldPreset = "",
            selectedUserPreset = userPreset
        )
        return true
    }

    /** Menyimpan level band SAAT INI sebagai preset bernama (nama sama = ditimpa). */
    fun saveUserPreset(rawName: String): Boolean {
        val bands = _state.value.bands
        if (equalizer == null || bands.isEmpty()) return false
        val name = rawName.trim().take(MAX_PRESET_NAME_LENGTH)
        if (name.isEmpty()) return false

        val others = readUserPresets().filter { it.name != name }
        if (others.size >= MAX_USER_PRESETS) return false

        writeUserPresets(others + StoredUserPreset(name, bands.map { it.levelMillibel }))
        prefs.edit().putString(KEY_USER_PRESET, name).apply()
        _state.value = _state.value.copy(
            userPresets = visibleUserPresetNames(bands.size),
            selectedUserPreset = name
        )
        return true
    }

    fun useUserPreset(name: String): Boolean {
        val bandCount = _state.value.bands.size
        val preset = readUserPresets().firstOrNull { it.name == name && it.levels.size == bandCount }
            ?: return false
        return applyBandLevels(preset.levels, name)
    }

    fun deleteUserPreset(name: String) {
        val bandCount = _state.value.bands.size
        writeUserPresets(readUserPresets().filter { it.name != name })
        val wasSelected = _state.value.selectedUserPreset == name
        if (wasSelected) prefs.edit().putString(KEY_USER_PRESET, "").apply()
        _state.value = _state.value.copy(
            userPresets = visibleUserPresetNames(bandCount),
            selectedUserPreset = if (wasSelected) "" else _state.value.selectedUserPreset
        )
    }

    /**
     * Batch 542 — impor profil AutoEq/EqualizerAPO (`ParametricEQ.txt`) lalu terapkan sebagai
     * perkiraan ke band perangkat (lihat batas jujur di [EqProfileImport]).
     */
    fun importProfile(text: String): EqImportResult {
        val profile = EqProfileImport.parse(text) ?: return EqImportResult(false, 0, 0)
        val bands = _state.value.bands
        if (equalizer == null || bands.isEmpty()) {
            return EqImportResult(false, profile.filters.size, profile.ignoredFilters)
        }
        val levels = EqProfileImport.toBandLevels(
            profile = profile,
            centerFreqsHz = bands.map { it.frequencyHz },
            minMillibel = _state.value.minLevel.toInt(),
            maxMillibel = _state.value.maxLevel.toInt()
        )
        return EqImportResult(applyBandLevels(levels), profile.filters.size, profile.ignoredFilters)
    }

    private data class StoredUserPreset(val name: String, val levels: List<Short>)

    private fun visibleUserPresetNames(bandCount: Int): List<String> =
        readUserPresets().filter { it.levels.size == bandCount }.map { it.name }

    private fun readUserPresets(): List<StoredUserPreset> {
        val raw = prefs.getString(KEY_USER_PRESETS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val name = obj.optString("n")
                val levelArray = obj.optJSONArray("l") ?: return@mapNotNull null
                if (name.isBlank()) return@mapNotNull null
                StoredUserPreset(name, (0 until levelArray.length()).map { levelArray.getInt(it).toShort() })
            }
        } catch (e: JSONException) {
            // Data rusak = anggap tidak ada preset; band aktif TIDAK tersentuh.
            AppLogger.e("EqualizerController", "Daftar preset EQ pengguna korup, diabaikan", e)
            emptyList()
        }
    }

    private fun writeUserPresets(presets: List<StoredUserPreset>) {
        val array = JSONArray()
        presets.forEach { preset ->
            array.put(
                JSONObject()
                    .put("n", preset.name)
                    .put("l", JSONArray(preset.levels.map { it.toInt() }))
            )
        }
        prefs.edit().putString(KEY_USER_PRESETS, array.toString()).apply()
    }

    enum class BoldPreset { FLAT, BASS_BOOST, TREBLE_BOOST, VOCAL_BOOST }

    fun release() {
        equalizer?.release()
        equalizer = null
    }

    companion object {
        private const val PREFS_NAME = "equalizer"
        private const val KEY_ENABLED = "eq_enabled"
        private const val KEY_PRESET = "eq_preset"
        private const val KEY_BOLD_PRESET = "eq_bold_preset"
        private const val KEY_USER_PRESET = "eq_user_preset"
        private const val KEY_USER_PRESETS = "eq_user_presets"
        private const val MAX_USER_PRESETS = 20
        private const val MAX_PRESET_NAME_LENGTH = 24
        private const val KEY_BAND_PREFIX = "eq_band_"

        @Volatile
        private var sharedInstance: EqualizerController? = null

        /**
         * Batch 490 — shared per-process instance. Root cause of "preset EQ balik ke
         * nol/default pasca app di-kill lalu musik dimainkan lewat eksternal player": the only
         * place that ever re-attached this controller to a new audio session was
         * [PlayerViewModel]'s `init{}` (Batch 314) — a ViewModel construct that simply does not
         * exist until MainActivity is created. When the process is instead resurrected headlessly
         * (widget/Bluetooth/media-button/notification/Android Auto after a full app-kill),
         * [PlaybackService] still creates a brand-new ExoPlayer session with 0 listener
         * registered, so the platform Equalizer for THAT session never learns the saved
         * bands/preset at all — it just sits at Android's own flat/disabled default even though
         * SharedPreferences still holds the real values.
         *
         * Fix (see [com.rudi.audioplayer.AudioPlayerApplication.onCreate], also Batch 490): the
         * re-attach hook is now ALSO registered there, which runs before any component regardless
         * of what started the process. That means two independent call sites can now legitimately
         * want an [EqualizerController] for the same process. android.media.audiofx.Equalizer
         * does not dedupe by session — two SEPARATE instances attached to the same session would
         * be two real inserted effects, silently double-applying every band gain the moment both
         * happened to be alive together. getInstance() makes that structurally impossible: every
         * caller in this process shares the ONE underlying platform Equalizer object, so attach()
         * only ever recreates it (release-then-new), never stacks a second one alongside it.
         */
        fun getInstance(context: Context): EqualizerController =
            sharedInstance ?: synchronized(this) {
                sharedInstance ?: EqualizerController(context.applicationContext).also { sharedInstance = it }
            }
    }
}
