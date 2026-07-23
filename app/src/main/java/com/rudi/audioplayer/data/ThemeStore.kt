package com.rudi.audioplayer.data

import android.content.Context
import com.rudi.audioplayer.ui.theme.AppTheme

class ThemeStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTheme(): AppTheme = AppTheme.fromStorageKey(prefs.getString(KEY_THEME, null))

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.storageKey).apply()
    }

    companion object {
        private const val PREFS_NAME = "app_theme"
        private const val KEY_THEME = "selected_theme"
    }
}
