package com.rudi.audioplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

/** Every AsyncImage in the app (Library grid/list rows, Home, MiniPlayerBar, Now Playing)
 * uses Coil's default singleton ImageLoader unless it builds its own request — so configuring
 * it once here applies everywhere. Crossfading album art in (instead of it popping in the
 * instant a bitmap decodes) masks normal decode latency and is one of the cheapest, most
 * broadly-felt wins for making list/grid scrolling feel smooth. */
class AudioPlayerApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(200)
            .build()
    }
}
