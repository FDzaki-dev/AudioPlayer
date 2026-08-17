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

    /** Gap List #9 — favorites only make sense pointing at a song that still exists; unlike
     *  history/stats (legitimate records of the past), a "favorite" for a file that's gone is
     *  just dead weight that silently accumulates forever (nothing else in the app ever removes
     *  a favorite ID on its own). Called opportunistically after each library refresh with the
     *  freshly-scanned valid ID set — no-op (no write) if nothing was actually stale. */
    fun pruneOrphans(validIds: Set<Long>) {
        val current = getFavorites()
        val pruned = current.filter { it.toLongOrNull() in validIds }.toSet()
        if (pruned.size != current.size) {
            prefs.edit().putStringSet(KEY_FAVORITES, pruned).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "favorites"
        private const val KEY_FAVORITES = "favorite_ids"
    }
}
