package com.rudi.audioplayer.data

import android.content.Context

/**
 * On/off preference for the floating mini player bubble (Roadmap #11), plus the last screen
 * position the user dragged it to — so turning it back on (or the next app launch) restores
 * the bubble where it was left instead of resetting to a default corner every time.
 *
 * Off by default — same reasoning as [ShakeSettingsStore]: an overlay drawn on top of every
 * other app is intrusive enough (and requires an explicit, sensitive `SYSTEM_ALERT_WINDOW`
 * permission grant) that it must be something the user deliberately opts into from Settings,
 * never silently active out of the box.
 */
class FloatingBubbleStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Null kalau bubble belum pernah digeser sama sekali (dipakai posisi default awal). */
    fun getPosition(): Pair<Int, Int>? {
        if (!prefs.contains(KEY_POS_X)) return null
        return prefs.getInt(KEY_POS_X, 0) to prefs.getInt(KEY_POS_Y, 0)
    }

    fun savePosition(x: Int, y: Int) {
        prefs.edit().putInt(KEY_POS_X, x).putInt(KEY_POS_Y, y).apply()
    }

    companion object {
        private const val PREFS_NAME = "floating_bubble_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_POS_X = "pos_x"
        private const val KEY_POS_Y = "pos_y"
    }
}
