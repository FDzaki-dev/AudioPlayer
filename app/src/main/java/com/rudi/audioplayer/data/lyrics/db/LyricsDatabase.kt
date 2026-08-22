package com.rudi.audioplayer.data.lyrics.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Batch 243 — Lyrics offline-first. Singleton manual (bukan Hilt @Provides) — codebase ini
// 100% belum pakai DI framework apa pun (semua "Store" di package data/ dikonstruksi langsung
// dari Context, lihat ThemeStore.kt dst, PlayerViewModel juga manual Factory). Nambah Hilt
// cuma buat 1 fitur ini artinya nyentuh Application/MainActivity/PlaybackService (protected
// assets) demi DI framework baru — di luar scope permintaan ("boleh diadaptasi, hasil akhir
// sama"). Pola singleton getInstance() di bawah ini konsisten arsitektur existing, 0 protected
// asset lain disentuh. exportSchema=false — codebase ini juga belum ada folder schema/ export
// utk Room manapun (project pertama kali pakai Room di batch ini).
@Database(entities = [LyricsEntity::class], version = 1, exportSchema = false)
abstract class LyricsDatabase : RoomDatabase() {
    abstract fun lyricsDao(): LyricsDao

    companion object {
        @Volatile private var instance: LyricsDatabase? = null

        fun getInstance(context: Context): LyricsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LyricsDatabase::class.java,
                    "lyrics_database.db"
                ).build().also { instance = it }
            }
    }
}
