package com.rudi.audioplayer.data.lyrics.api

import com.google.gson.annotations.SerializedName

// Batch 244 — Lyrics offline-first 2/4. Bentuk respons persis LRCLIB `/api/get` (field null
// kalau lagu instrumental atau cuma punya salah satu jenis lirik — Repository yg mapping ke
// LyricsEntity, DTO ini sengaja apa-adanya 1:1 sama API, 0 logic).
data class LyricsDto(
    val id: Long? = null,
    @SerializedName("trackName") val trackName: String? = null,
    @SerializedName("artistName") val artistName: String? = null,
    @SerializedName("albumName") val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean? = null,
    @SerializedName("plainLyrics") val plainLyrics: String? = null,
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null
)
