package com.rudi.audioplayer.playback

/**
 * Batch 597 — definisi "satu putaran" (laporan audit: `recordPlay` dulu dipanggil di SETIAP
 * `onMediaItemTransition`, apa pun alasannya, sehingga lagu yang di-skip 2 detik dihitung putaran penuh dan
 * mengotori "Paling Sering Diputar", Mix, Radio, Kilas Balik, dan Statistik).
 *
 * Aturan baru: sebuah lagu dihitung SEKALI per sesi putar bila waktu dengar NYATA (jam dinding saat benar-benar
 * memutar, jeda tidak dihitung, seek tidak menambah) mencapai `min(30 detik, setengah durasi)`; durasi tak
 * diketahui memakai 30 detik. Pindah lagu sebelum ambang = tidak dihitung. Mengulang lagu yang sama
 * (repeat-one) = sesi baru, bisa dihitung lagi. Murni Kotlin tanpa API Android (waktu disuntikkan lewat `nowMs`
 * monoton, mis. `SystemClock.elapsedRealtime()`), tidak thread-safe — dipakai dari satu thread (Main).
 */
class PlayCountTracker {
    private var songId: Long? = null
    private var thresholdMs = 0L
    private var accumulatedMs = 0L
    private var playingSinceMs: Long? = null
    private var counted = false

    /** Memulai sesi baru untuk [songId] (membuang sesi sebelumnya, terhitung atau tidak). */
    fun startItem(songId: Long, durationMs: Long, nowMs: Long, playing: Boolean) {
        this.songId = songId
        thresholdMs = thresholdFor(durationMs)
        accumulatedMs = 0L
        counted = false
        playingSinceMs = if (playing) nowMs else null
    }

    fun clearItem() {
        songId = null
        accumulatedMs = 0L
        playingSinceMs = null
        counted = false
    }

    fun hasItem(): Boolean = songId != null

    /** Memberi tahu status memutar/jeda; tanpa sesi aktif diabaikan. */
    fun setPlaying(playing: Boolean, nowMs: Long) {
        if (songId == null) return
        if (playing) {
            if (playingSinceMs == null) playingSinceMs = nowMs
        } else {
            playingSinceMs?.let { accumulatedMs += (nowMs - it).coerceAtLeast(0L) }
            playingSinceMs = null
        }
    }

    /** Sisa ms sampai ambang bila sedang memutar; null bila jeda / tanpa sesi / sudah dihitung. */
    fun msUntilQualified(nowMs: Long): Long? {
        if (songId == null || counted || playingSinceMs == null) return null
        return (thresholdMs - listenedMs(nowMs)).coerceAtLeast(0L)
    }

    /** Mengembalikan id lagu TEPAT SEKALI saat ambang tercapai; selain itu null. */
    fun takeIfQualified(nowMs: Long): Long? {
        val id = songId ?: return null
        if (counted || listenedMs(nowMs) < thresholdMs) return null
        counted = true
        return id
    }

    private fun listenedMs(nowMs: Long): Long =
        accumulatedMs + (playingSinceMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L)

    companion object {
        const val FULL_THRESHOLD_MS = 30_000L
        private const val MIN_THRESHOLD_MS = 1_000L

        fun thresholdFor(durationMs: Long): Long =
            if (durationMs > 0L) minOf(FULL_THRESHOLD_MS, maxOf(MIN_THRESHOLD_MS, durationMs / 2)) else FULL_THRESHOLD_MS
    }
}

/**
 * Batch 615 — siapa yang MENCATAT putaran. Pengawas Batch 597 hidup di `PlayerViewModel`, sehingga pemutaran
 * lewat widget/headset/bubble setelah Activity ditutup (ViewModel dibuang, MediaController dilepas) tak pernah
 * tercatat. `PlaybackService` kini punya pelacak sendiri yang SELALU berjalan; ia hanya MENCATAT bila tak ada
 * ViewModel yang mendengar (`viewModelListening == false`), dan tetap menghabiskan status "ambang tercapai"
 * walau tak mencatat — jadi tak ada hitung ganda saat UI dibuka/ditutup di tengah lagu.
 */
object PlayCountOwner {
    @Volatile
    var viewModelListening: Boolean = false
}
