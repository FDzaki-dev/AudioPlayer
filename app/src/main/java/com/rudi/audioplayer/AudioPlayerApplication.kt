package com.rudi.audioplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.rudi.audioplayer.util.AppLogger
import com.rudi.audioplayer.util.AudioArtFetcher

/** Every AsyncImage in the app (Library grid/list rows, Home, MiniPlayerBar, Now Playing)
 * uses Coil's default singleton ImageLoader unless it builds its own request — so configuring
 * it once here applies everywhere. Crossfading album art in (instead of it popping in the
 * instant a bitmap decodes) masks normal decode latency and is one of the cheapest, most
 * broadly-felt wins for making list/grid scrolling feel smooth. */
class AudioPlayerApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        warmUpSharedPreferences()
    }

    /**
     * Batch 385 (cold-start optimization) — `PlayerViewModel` (built the instant
     * `MainActivity.onCreate()` first touches its `by viewModels` delegate) opens **23 separate
     * SharedPreferences files** across its own property initializers (favorites, playback_state,
     * play_stats, ratings, app_lock, vault, listening_history, hourly_listen_stats,
     * shake_settings, radio_settings, floating_bubble_settings, silence_skip_settings,
     * playlists, lyrics, bookmarks, audiobook_mode, equalizer, visualizer_settings, crossfade,
     * custom_folders, app_theme, smart_playlists, sleep_timer) — plus 5 more (library_filter,
     * search_history, onboarding_hints, lyrics_prefetch_settings, widget_state) opened by other
     * Store/Updater classes that screens and the home-screen widget construct moments after
     * cold start, 28 files in total app-wide. Android only starts a file's own background load
     * thread the FIRST time `Context.getSharedPreferences(name, MODE_PRIVATE)` is called for it —
     * and several of `PlayerViewModel`'s StateFlow initializers (theme identity/mode, crossfade,
     * playlists, smart playlists, shake/bubble/silence-skip/radio toggles, app-lock flags) plus
     * its `init {}` block (sleep timer) call a getter on that SAME first call, which blocks the
     * calling thread (`awaitLoadedLocked()`) until that specific file's parse finishes. Today
     * that first call — and therefore the FIRST moment any of these 28 loads can even begin —
     * only happens once `MainActivity.onCreate()` runs, after process start, class-loading, and
     * window setup have already spent part of the cold-start budget.
     *
     * Fix: touch every known file name here, from a background thread, as early as
     * `Application.onCreate()` allows — process start, before any Activity exists. Android
     * caches the resulting `SharedPreferencesImpl` per-file by absolute path (synchronized in
     * `ContextImpl`), so the SAME instance every Store's own `getSharedPreferences()` call
     * returns later — this is a genuine head start for the same load, not a duplicate one. By
     * the time `PlayerViewModel`'s constructor reaches each read, the load has had the entire
     * rest of process/window startup to finish in the background instead of starting cold.
     *
     * Zero behavior change: no value is read or written here, only the file handle is warmed —
     * every Store's own getters/setters, defaults, and first-run behavior are untouched. Safe to
     * leave a stale name here if a Store is ever removed (a harmless no-op file touch), and safe
     * to add a new one later if a Store is added — this list is intentionally a plain
     * best-effort mirror of each Store's own `PREFS_NAME`, not a shared source of truth.
     */
    private fun warmUpSharedPreferences() {
        Thread({
            for (name in PREFS_TO_WARM) {
                runCatching { getSharedPreferences(name, MODE_PRIVATE) }
            }
        }, "PrefsWarmup").start()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(200)
            // Batch 68: extracts embedded art from song.uri instead of Coil's default
            // fetcher trying (and failing) to decode audio bytes as an image. See
            // AudioArtFetcher kdoc for the full regression story.
            .components { add(AudioArtFetcher.Factory(this@AudioPlayerApplication)) }
            .build()
    }

    companion object {
        // Batch 385 — keep in sync (best-effort) with each Store's own `PREFS_NAME` constant
        // under data/*.kt, playback/EqualizerController.kt, and widget/WidgetUpdater.kt.
        private val PREFS_TO_WARM = arrayOf(
            "favorites", "playback_state", "play_stats", "ratings", "app_lock", "vault",
            "listening_history", "hourly_listen_stats", "shake_settings", "radio_settings",
            "floating_bubble_settings", "silence_skip_settings", "playlists", "lyrics",
            "bookmarks", "audiobook_mode", "equalizer", "visualizer_settings", "crossfade",
            "custom_folders", "app_theme", "smart_playlists", "sleep_timer", "library_filter",
            "search_history", "onboarding_hints", "lyrics_prefetch_settings", "widget_state"
        )
    }
}
