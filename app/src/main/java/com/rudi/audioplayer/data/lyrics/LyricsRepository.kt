package com.rudi.audioplayer.data.lyrics

import android.content.Context
import com.rudi.audioplayer.data.lyrics.api.LyricsApi
import com.rudi.audioplayer.data.lyrics.db.LyricsDatabase
import com.rudi.audioplayer.data.lyrics.db.LyricsEntity
import java.io.IOException

// Batch 245 — Lyrics offline-first 3/4. Hasil domain — sengaja BUKAN LyricsEntity/LyricsDto
// langsung (ViewModel/View di package `ui/lyrics` tidak perlu tahu Room/Retrofit ada di
// belakangnya, cuma peduli ada lirik atau tidak).
sealed class LyricsResult {
    data class Found(val plainLyrics: String?, val syncedLyrics: String?) : LyricsResult()
    data object NotFound : LyricsResult()
}

// Logic offline-first PERSIS spec: dao.get() -> null? api.fetch() -> dao.upsert() -> return;
// ada? return cache langsung, 0 network. Dikonstruksi manual dari Context (bukan Hilt @Inject
// constructor) — konsisten adaptasi Batch 243 (codebase ini 0% pakai DI framework).
class LyricsRepository(context: Context) {
    private val dao = LyricsDatabase.getInstance(context).lyricsDao()
    private val api = LyricsApi.create()

    suspend fun getLyrics(artist: String, title: String, album: String?): LyricsResult {
        val cached = dao.get(artist, title)
        if (cached != null) return LyricsResult.Found(cached.plainLyrics, cached.syncedLyrics)
        return fetchAndCache(artist, title, album)
    }

    // Dipakai LyricsPrefetchWorker (batch 4/4) — cache-check dulu SEBELUM fetch, persis spec
    // item 7 ("Cek cache dulu. Jika tidak ada baru fetch API"), tanpa perlu expose LyricsResult
    // ke caller yg cuma butuh efek-samping "pastikan ke-cache", bukan hasilnya.
    suspend fun ensureCached(artist: String, title: String, album: String?) {
        if (dao.get(artist, title) == null) fetchAndCache(artist, title, album)
    }

    suspend fun clearCache() = dao.clearAll()

    suspend fun cacheCount(): Int = dao.count()

    private suspend fun fetchAndCache(artist: String, title: String, album: String?): LyricsResult {
        return try {
            val response = api.getLyrics(artist, title)
            val dto = if (response.isSuccessful) response.body() else null
            // Spec error case #9: API gagal ATAU cache kosong (di sini: body null/kosong
            // dua-duanya, mis. lagu memang tidak ada di LRCLIB) -> NotFound, bukan exception
            // yg bocor ke ViewModel/UI.
            if (dto == null || (dto.plainLyrics.isNullOrBlank() && dto.syncedLyrics.isNullOrBlank())) {
                return LyricsResult.NotFound
            }
            dao.upsert(
                LyricsEntity(
                    artist = artist,
                    title = title,
                    album = album,
                    plainLyrics = dto.plainLyrics,
                    syncedLyrics = dto.syncedLyrics,
                    lastFetched = System.currentTimeMillis(),
                    source = "lrclib"
                )
            )
            LyricsResult.Found(dto.plainLyrics, dto.syncedLyrics)
        } catch (e: IOException) {
            // Timeout/no-connection (spec: timeout 10s di LyricsApi) — offline murni, jangan
            // crash, cuma NotFound (UI tampilkan pesan, bukan lirik lama yg salah).
            LyricsResult.NotFound
        } catch (e: Exception) {
            LyricsResult.NotFound
        }
    }
}
