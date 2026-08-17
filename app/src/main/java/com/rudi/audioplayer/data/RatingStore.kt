package com.rudi.audioplayer.data

import android.content.Context

/** Simple 1-5 star rating per song, stored by ID. 0/absent = unrated. */
class RatingStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRating(songId: Long): Int = prefs.getInt(KEY_PREFIX + songId, 0)

    fun setRating(songId: Long, stars: Int) {
        val clamped = stars.coerceIn(0, 5)
        if (clamped == 0) {
            prefs.edit().remove(KEY_PREFIX + songId).apply()
        } else {
            prefs.edit().putInt(KEY_PREFIX + songId, clamped).apply()
        }
    }

    /** Gap List #9 — same reasoning as `FavoritesStore.pruneOrphans`: a star rating for a
     *  deleted file is dead weight, not a record worth keeping. Keys are per-song
     *  (`rating_<id>`), so this enumerates `prefs.all` rather than a single stored set. */
    fun pruneOrphans(validIds: Set<Long>) {
        val editor = prefs.edit()
        var changed = false
        for (key in prefs.all.keys) {
            if (!key.startsWith(KEY_PREFIX)) continue
            val id = key.removePrefix(KEY_PREFIX).toLongOrNull()
            if (id == null || id !in validIds) {
                editor.remove(key)
                changed = true
            }
        }
        if (changed) editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "ratings"
        private const val KEY_PREFIX = "rating_"
    }
}
