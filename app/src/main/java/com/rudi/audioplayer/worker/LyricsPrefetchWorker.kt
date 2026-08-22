package com.rudi.audioplayer.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rudi.audioplayer.data.MusicRepository
import com.rudi.audioplayer.data.PlaybackStateStore
import com.rudi.audioplayer.data.lyrics.LyricsRepository

/**
 * Batch 246 — Lyrics offline-first 4/4a. Prefetch lirik 10 lagu DEPAN queue saat WiFi
 * (`NetworkType.UNMETERED`), spec item 7-8.
 *
 * Sengaja TIDAK dikasih pegangan ke ExoPlayer/MediaController langsung — [CoroutineWorker]
 * bisa dieksekusi WorkManager kapan pun (termasuk saat proses app sudah mati total sejak
 * request terakhir), jadi tidak boleh berasumsi ada Service/Player hidup. Sumber "isi queue +
 * lagi di lagu ke berapa" dibaca dari [PlaybackStateStore] — infrastruktur SUDAH ADA (dipakai
 * playback resumption sejak Batch 108), bukan mekanisme baru: kalau itu cukup akurat buat
 * memulihkan seluruh playback setelah proses mati, otomatis cukup akurat juga buat sekadar tahu
 * "10 lagu berikutnya apa saja".
 */
class LyricsPrefetchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val saved = PlaybackStateStore(applicationContext).load() ?: return Result.success()
            val nextIds = saved.songIds.drop(saved.index + 1).take(LOOKAHEAD_COUNT)
            if (nextIds.isEmpty()) return Result.success()

            // getSongsByIds() query `IN (...)` — urutan hasil TIDAK dijamin sama seperti
            // urutan input (dokumentasi fungsinya sendiri di MusicRepository.kt). Tidak masalah
            // di sini: seluruh next-N tetap diproses satu-satu, tidak ada yang butuh urutan.
            val songs = MusicRepository(applicationContext).getSongsByIds(nextIds)
            val repository = LyricsRepository(applicationContext)
            for (song in songs) {
                // WorkManager bisa minta worker berhenti kapan saja (mis. WiFi putus di
                // tengah, constraint tidak lagi terpenuhi) — cek tiap iterasi, bukan cuma di
                // awal, supaya loop 10 lagu tidak lanjut memaksa network call yang percuma.
                if (isStopped) break
                // ensureCached() sendiri sudah swallow IOException/exception apa pun jadi
                // no-op (lihat LyricsRepository.kt) — 1 lagu gagal fetch TIDAK menjegal 9
                // lainnya, tidak perlu try/catch per-item di sini.
                repository.ensureCached(song.artist, song.title, song.album)
            }
            Result.success()
        } catch (e: Exception) {
            // PlaybackStateStore/MusicRepository gagal baca (mis. storage corrupt) — bukan
            // kegagalan yang berguna di-retry WorkManager otomatis, cukup diam.
            Result.failure()
        }
    }

    companion object {
        private const val LOOKAHEAD_COUNT = 10
        private const val WORK_NAME = "lyrics_prefetch"

        /**
         * Aman dipanggil berulang-ulang (tiap pindah lagu) — [ExistingWorkPolicy.REPLACE]
         * membuang request lama yang belum sempat jalan (mis. masih menunggu WiFi) dan
         * gantikan dengan window 10-lagu-depan yang baru, tanpa numpuk banyak worker antre
         * untuk queue-state yang sudah usang.
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()
            val request = OneTimeWorkRequestBuilder<LyricsPrefetchWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
