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
}
