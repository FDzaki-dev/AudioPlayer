package com.rudi.audioplayer.data

import android.content.Context
import com.rudi.audioplayer.ui.theme.ThemeIdentity
import com.rudi.audioplayer.ui.theme.ThemeMode

// Batch 61 — dulu 1 key gabungan (AppTheme: SYSTEM/LIGHT/DARK/TACTILE/SKEU_DARK_LITE sejajar).
// Sekarang 2 key terpisah (identity + mode) supaya identitas tema & mode terang/gelap benar-benar
// independen, sesuai instruksi user. Migrasi otomatis dari key lama tetap dijaga: user lama yang
// belum pernah buka app sejak batch ini akan tetap mendapat kombinasi identity+mode yang setara
// dengan pilihan lama mereka, tanpa perlu re-pilih tema secara manual.
class ThemeStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getIdentity(): ThemeIdentity {
        prefs.getString(KEY_IDENTITY, null)?.let { return ThemeIdentity.fromStorageKey(it) }
        // Belum pernah migrasi — turunkan dari key lama.
        return when (prefs.getString(KEY_THEME_LEGACY, null)) {
            "tactile_lite" -> ThemeIdentity.TACTILE
            "skeu_dark_lite" -> ThemeIdentity.SKEU_DARK_LITE
            else -> ThemeIdentity.APPLE
        }
    }

    fun getMode(): ThemeMode {
        prefs.getString(KEY_MODE, null)?.let { return ThemeMode.fromStorageKey(it) }
        // Belum pernah migrasi — turunkan dari key lama. Tactile/Skeu lama selalu gelap
        // (identitas itu dulu terkunci gelap permanen), jadi dipetakan ke DARK di sini; user
        // bebas menyalakan mode terang untuk identitas yang sama kapan pun lewat toggle baru.
        return when (prefs.getString(KEY_THEME_LEGACY, null)) {
            "light" -> ThemeMode.LIGHT
            "dark", "tactile_lite", "skeu_dark_lite" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    fun setIdentity(identity: ThemeIdentity) {
        prefs.edit().putString(KEY_IDENTITY, identity.storageKey).apply()
    }

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.storageKey).apply()
    }

    companion object {
        private const val PREFS_NAME = "app_theme"
        private const val KEY_THEME_LEGACY = "selected_theme"
        private const val KEY_IDENTITY = "selected_identity"
        private const val KEY_MODE = "selected_mode"
    }
}
