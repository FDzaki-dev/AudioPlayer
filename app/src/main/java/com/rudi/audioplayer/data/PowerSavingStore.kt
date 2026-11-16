package com.rudi.audioplayer.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Batch 673 — saklar uji dua penghemat daya pemutaran (Pengaturan Putar > Hemat Daya), default ON, agar tiap perbaikan bisa
 * dimatikan tanpa kompilasi ulang saat debugging regresi:
 *  - Scheduling Dinamis (Batch 671/672): flag `ExoPlayer.Builder`, dibaca saat player dibangun -> berlaku setelah app dimulai ulang.
 *  - Audio Offload (Batch 672): berlaku LIVE; PlaybackService memantau [registerOnChange] lalu memanggil ulang applyAudioOffload.
 */
class PowerSavingStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDynamicScheduling(): Boolean = prefs.getBoolean(KEY_DYNAMIC_SCHEDULING, true)

    fun setDynamicScheduling(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_SCHEDULING, enabled).apply()
    }

    fun isAudioOffload(): Boolean = prefs.getBoolean(KEY_AUDIO_OFFLOAD, true)

    fun setAudioOffload(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIO_OFFLOAD, enabled).apply()
    }

    /** SharedPreferences hanya menyimpan pendengar secara LEMAH: penelepon wajib memegang referensi kuat [listener]. */
    fun registerOnChange(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnChange(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "power_saving"
        private const val KEY_DYNAMIC_SCHEDULING = "dynamic_scheduling"
        const val KEY_AUDIO_OFFLOAD = "audio_offload"
    }
}
