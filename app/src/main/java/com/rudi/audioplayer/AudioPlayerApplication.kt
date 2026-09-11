package com.rudi.audioplayer

import android.app.Application
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.rudi.audioplayer.util.AppLogger
import com.rudi.audioplayer.util.AudioArtFetcher

/** Every AsyncImage in the app (Library grid/list rows, Home, MiniPlayerBar, Now Playing)
 * uses Coil's default singleton ImageLoader unless it builds its own request — so configuring
 * it once here applies everywhere. Crossfading album art in (instead of it popping in the
 * instant a bitmap decodes) masks normal decode latency and is one of the cheapest, most
 * broadly-felt wins for making list/grid scrolling feel smooth.
 *
 * Batch 425 (Coil 2.6.0 -> 3.6.2 migration): `ImageLoaderFactory` was renamed
 * `SingletonImageLoader.Factory` and now receives the platform `Context` as a `newImageLoader`
 * parameter instead of relying on `this@AudioPlayerApplication` as an implicit receiver.
 * Batch 427 (CI FAILED, corrected): `crossfade()` is a top-level Kotlin extension function in
 * Coil 3 (`coil3.request.crossfade`), not an `ImageLoader.Builder` member like it effectively
 * was in Coil 2 — needs its own explicit import.
 * Batch 428 (CI FAILED again, corrected): `components { }` IS a genuine member function of
 * `ImageLoader.Builder` (needs no import, unlike `crossfade`) — removed the invalid
 * `import coil3.components` (no such top-level symbol exists). Also removed the invalid 2-arg
 * `add(factory, CoilUri::class)` call below: `ComponentRegistry.Builder`'s only `Fetcher.Factory`
 * overload is `inline fun <T : Any> add(factory: Fetcher.Factory<T>)` — no KClass-taking
 * overload exists for fetchers (that 2-arg form only exists for `Keyer`/`Mapper`). The
 * single-arg call infers `T = coil3.Uri` automatically from `AudioArtFetcher.Factory`'s own
 * declared type (`Fetcher.Factory<coil3.Uri>`, see that class), same pattern as every working
 * example in Coil's own docs (`add(CustomFetcher.Factory())`, no explicit type argument). */
class AudioPlayerApplication : Application(), SingletonImageLoader.Factory, Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        warmUpSharedPreferences()
    }

    /**
     * Batch 387 (cold-start optimization) — required pairing for removing WorkManager's default
     * `androidx.startup`-based initializer entry in `AndroidManifest.xml` (see that file's
     * comment for the full root-cause story). Implementing `Configuration.Provider` is what lets
     * WorkManager fall back to Google's documented "on-demand initialization": instead of the
     * removed ContentProvider eagerly calling `WorkManager.initialize()` before this class's own
     * `onCreate()` even runs, the framework now calls it lazily, internally, the first time
     * ANYTHING calls `WorkManager.getInstance(context)` — the one and only call site is
     * `LyricsPrefetchWorker.enqueue()`, invoked only after a song has actually started playing.
     *
     * `Configuration.Builder().build()` here is the exact same unmodified default the removed
     * ContentProvider used internally — so the ONLY thing this batch changes is *when*
     * WorkManager sets itself up (on first real use instead of unconditionally on every cold
     * start), never how it behaves once initialized: same executor, same min logging level, same
     * everything else. Documented trade-off of on-demand init generally (per AndroidX's own
     * guidance): WorkManager's automatic rescheduling after a process crash/force-stop is
     * delayed until the next `getInstance()` call instead of happening immediately at the next
     * app launch — acceptable here since the only work this app ever schedules
     * (`LyricsPrefetchWorker`) is a best-effort, already-lossy background prefetch that silently
     * swallows its own failures by design (see that class's kdoc), not something anything else
     * depends on completing reliably.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

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

    override fun newImageLoader(context: android.content.Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(200)
            // Batch 68: extracts embedded art from song.uri instead of Coil's default
            // fetcher trying (and failing) to decode audio bytes as an image. See
            // AudioArtFetcher kdoc for the full regression story.
            // Batch 428: single-arg add() — T (coil3.Uri) infers from AudioArtFetcher.Factory's
            // own Fetcher.Factory<coil3.Uri> declaration, no KClass overload exists to pass one.
            .components { add(AudioArtFetcher.Factory(context)) }
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
