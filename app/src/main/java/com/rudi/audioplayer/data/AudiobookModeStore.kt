package com.rudi.audioplayer.data

import android.content.Context
import com.rudi.audioplayer.util.AppLogger
import org.json.JSONObject

data class AudiobookModeState(
    val enabled: Boolean = false,
    val speed: Float = 1.0f,
    val lastPositionMs: Long = 0L
)

/**
 * Persists per-song playback speed + last position for Roadmap #12 (Mode Audiobook/Podcast,
 * Batch 93) — a single JSON object per song ID, keyed like [BookmarkStore]/[LyricsStore]
 * (`KEY_PREFIX + songId`), but exactly ONE record per song rather than a growable array — this
 * feature has one state per song, not a list.
 *
 * Deliberately separate from [PlaybackStateStore] (which remembers only the whole QUEUE's last
 * position, one global value) — this is per-file speed+position, opt-in per song, meant for long
 * files (podcast/audiobook) that get revisited individually outside of any particular queue.
 */
class AudiobookModeStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(songId: Long): AudiobookModeState {
        val raw = prefs.getString(KEY_PREFIX + songId, null) ?: return AudiobookModeState()
        return try {
            val obj = JSONObject(raw)
            AudiobookModeState(
                enabled = obj.optBoolean("enabled", false),
                speed = obj.optDouble("speed", 1.0).toFloat(),
                lastPositionMs = obj.optLong("lastPositionMs", 0L)
            )
        } catch (e: Exception) {
            // Same tradeoff as BookmarkStore/SmartPlaylistStore: an unparsable record falls back
            // to "mode nonaktif, dari awal" instead of crashing playback, logged locally so it's
            // traceable without ever surfacing to the user mid-listen.
            AppLogger.e("AudiobookModeStore", "Gagal parse status audiobook untuk lagu $songId", e)
            AudiobookModeState()
        }
    }

    /** Turns the mode on/off for one song. [currentSpeed] seeds the saved speed only the moment
     * this transitions off→on — flipping the toggle shouldn't itself jump playback to some other
     * default speed than whatever was already playing. */
    fun setEnabled(songId: Long, enabled: Boolean, currentSpeed: Float) {
        val existing = get(songId)
        val newSpeed = if (enabled && !existing.enabled) currentSpeed else existing.speed
        save(songId, existing.copy(enabled = enabled, speed = newSpeed))
    }

    /** Called from the same periodic/pause-triggered save tick [PlayerViewModel] already uses for
     * [PlaybackStateStore] (~5s while playing, immediately on pause) — silently no-ops if this
     * song was never opted into audiobook mode, so it's safe to call unconditionally for
     * whatever song happens to be current. */
    fun updateProgress(songId: Long, speed: Float, positionMs: Long) {
        val existing = get(songId)
        if (!existing.enabled) return
        save(songId, existing.copy(speed = speed, lastPositionMs = positionMs))
    }

    private fun save(songId: Long, state: AudiobookModeState) {
        if (!state.enabled) {
            // Opting out clears the record entirely rather than leaving a disabled-but-populated
            // entry behind — re-enabling later starts fresh, same as BookmarkStore removing its
            // key once a song's bookmark list empties out.
            prefs.edit().remove(KEY_PREFIX + songId).apply()
            return
        }
        val obj = JSONObject()
        obj.put("enabled", state.enabled)
        obj.put("speed", state.speed.toDouble())
        obj.put("lastPositionMs", state.lastPositionMs)
        prefs.edit().putString(KEY_PREFIX + songId, obj.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "audiobook_mode"
        private const val KEY_PREFIX = "audiobook_"
    }
}
