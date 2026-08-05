package com.rudi.audioplayer.data

import android.content.Context

/**
 * Minimal favorites persistence backed by SharedPreferences.
 * No database needed for a simple set of favorited song IDs.
 */
class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFavorites(): Set<String> = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()

    fun toggleFavorite(songId: Long) {
        val key = songId.toString()
        val current = getFavorites().toMutableSet()
        if (current.contains(key)) current.remove(key) else current.add(key)
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    companion object {
        private const val PREFS_NAME = "favorites"
        private const val KEY_FAVORITES = "favorite_ids"
    }
}
