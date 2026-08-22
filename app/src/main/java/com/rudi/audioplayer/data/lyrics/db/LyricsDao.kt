package com.rudi.audioplayer.data.lyrics.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Batch 243 — Lyrics offline-first. `get()` suspend one-shot dipakai Repository buat cek cache
// SEBELUM hit API (logic "if null -> fetch"); `observe()` Flow disediakan terpisah utk
// ViewModel yang mau reaktif ikut update kalau cache berubah dari sumber lain (mis. prefetch
// worker nyelesaiin fetch lagu yg sama saat UI lirik lagi kebuka). WHERE case-sensitive
// (artist/title datang dari MediaMetadata Media3, bukan input user bebas — konsisten dgn
// key yg dipakai saat insert, jadi match persis).
@Dao
interface LyricsDao {

    @Query("SELECT * FROM lyrics_cache WHERE artist = :artist AND title = :title LIMIT 1")
    suspend fun get(artist: String, title: String): LyricsEntity?

    @Query("SELECT * FROM lyrics_cache WHERE artist = :artist AND title = :title LIMIT 1")
    fun observe(artist: String, title: String): Flow<LyricsEntity?>

    // REPLACE by unique index (artist,title) — insert ulang lagu yg sudah ada otomatis update
    // baris lama (mis. re-fetch manual), bukan bikin duplikat/gagal constraint.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LyricsEntity)

    @Query("SELECT COUNT(*) FROM lyrics_cache")
    suspend fun count(): Int

    @Query("DELETE FROM lyrics_cache")
    suspend fun clearAll()
}
