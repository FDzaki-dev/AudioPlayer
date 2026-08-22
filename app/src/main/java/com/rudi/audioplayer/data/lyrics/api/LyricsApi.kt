package com.rudi.audioplayer.data.lyrics.api

import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Batch 244 — Lyrics offline-first 2/4. Endpoint tunggal `GET /api/get`, sesuai spec (bukan
// `/api/search` — search ambigu/multi-hasil, `/api/get` exact-match artist+title cocok utk
// offline-first 1-baris-per-lagu).
interface LyricsApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("artist_name") artist: String,
        @Query("track_name") title: String
    ): retrofit2.Response<LyricsDto>

    companion object {
        private const val BASE_URL = "https://lrclib.net/"

        // Timeout 10s (spec) — beda dari GitHubReleaseChecker punya UpdateDownloader (15s/20s,
        // buat metadata rilis) krn ini dipanggil di jalur pemutaran lagu (onMetadataChanged),
        // gagal cepat lebih penting drpd nunggu lama di tengah UX dengar lagu.
        private val userAgentInterceptor = Interceptor { chain: Interceptor.Chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "MusicApp/1.3 Hybrid")
                .build()
            chain.proceed(req) as Response
        }

        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(userAgentInterceptor)
            .build()

        fun create(): LyricsApi = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LyricsApi::class.java)
    }
}
