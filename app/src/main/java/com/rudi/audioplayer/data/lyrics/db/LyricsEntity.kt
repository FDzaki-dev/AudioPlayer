package com.rudi.audioplayer.data.lyrics.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Batch 243 — Lyrics offline-first. Cache permanen 1 baris per lagu (artist+title unik).
// plainLyrics/syncedLyrics nullable krn LRCLIB kadang cuma punya salah satu (instrumental —
// keduanya null — atau lirik tanpa timestamp). `source` disimpan buat bedain hasil dari
// "lrclib" (API) vs kelak sumber lain tanpa perlu migrasi kolom baru.
@Entity(
    tableName = "lyrics_cache",
    indices = [Index(value = ["artist", "title"], unique = true)]
)
data class LyricsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val artist: String,
    val title: String,
    val album: String?,
    val plainLyrics: String?,
    val syncedLyrics: String?,
    val lastFetched: Long,
    val source: String
)
