package com.rudi.audioplayer.data

/**
 * Batch 597 — pilihan urutan tab Lagu & Album (laporan audit: urutan terkunci, tak ada pilihan).
 * [DEFAULT] = urutan bawaan pemindaian (perilaku lama, tidak diubah). Tersimpan di `LibraryFilterStore`.
 */
enum class SongSort(val label: String) {
    DEFAULT("Bawaan"),
    TITLE("Judul A–Z"),
    ARTIST("Artis A–Z"),
    ALBUM("Album A–Z"),
    DATE_ADDED("Terbaru ditambahkan"),
    DURATION("Terpanjang");

    companion object {
        fun fromName(name: String?): SongSort = values().firstOrNull { it.name == name } ?: DEFAULT
    }
}

enum class AlbumSort(val label: String) {
    NAME("Nama A–Z"),
    ARTIST("Artis A–Z"),
    RECENT("Terbaru ditambahkan"),
    SONG_COUNT("Terbanyak lagu");

    companion object {
        fun fromName(name: String?): AlbumSort = values().firstOrNull { it.name == name } ?: NAME
    }
}

/** Pengurut murni (tanpa Android) — stabil; teks kosong selalu di urutan paling akhir. */
object LibrarySort {
    private const val LAST = "\uFFFF"

    private fun key(text: String): String = if (text.isBlank()) LAST else text.trim().lowercase()

    fun sortSongs(songs: List<Song>, sort: SongSort): List<Song> = when (sort) {
        SongSort.DEFAULT -> songs
        SongSort.TITLE -> songs.sortedWith(
            compareBy<Song> { key(it.title) }.thenBy { key(it.artist) }
        )
        SongSort.ARTIST -> songs.sortedWith(
            compareBy<Song> { key(it.artist) }
                .thenBy { key(it.album) }
                .thenBy { it.discNumber ?: 0 }
                .thenBy { it.trackNumber ?: Int.MAX_VALUE }
                .thenBy { key(it.title) }
        )
        SongSort.ALBUM -> songs.sortedWith(
            compareBy<Song> { key(it.album) }
                .thenBy { it.discNumber ?: 0 }
                .thenBy { it.trackNumber ?: Int.MAX_VALUE }
                .thenBy { key(it.title) }
        )
        SongSort.DATE_ADDED -> songs.sortedWith(
            compareByDescending<Song> { it.dateAdded }.thenBy { key(it.title) }
        )
        SongSort.DURATION -> songs.sortedWith(
            compareByDescending<Song> { it.duration }.thenBy { key(it.title) }
        )
    }

    /** Urutan kunci album (nama album) pada hasil `groupBy` tab Album. [AlbumSort.NAME] = perilaku lama. */
    fun sortAlbumKeys(grouped: Map<String, List<Song>>, sort: AlbumSort): List<String> {
        val byName = compareBy<String> { it.lowercase() }
        return when (sort) {
            AlbumSort.NAME -> grouped.keys.sortedWith(byName)
            AlbumSort.ARTIST -> grouped.keys.sortedWith(
                compareBy<String> { key(albumArtistOf(grouped[it].orEmpty())) }.then(byName)
            )
            AlbumSort.RECENT -> grouped.keys.sortedWith(
                compareByDescending<String> { name -> grouped[name].orEmpty().maxOfOrNull { it.dateAdded } ?: 0L }
                    .then(byName)
            )
            AlbumSort.SONG_COUNT -> grouped.keys.sortedWith(
                compareByDescending<String> { grouped[it]?.size ?: 0 }.then(byName)
            )
        }
    }

    private fun albumArtistOf(songs: List<Song>): String =
        songs.firstNotNullOfOrNull { it.albumArtist?.takeIf { artist -> artist.isNotBlank() } }
            ?: songs.firstOrNull()?.artist.orEmpty()
}
