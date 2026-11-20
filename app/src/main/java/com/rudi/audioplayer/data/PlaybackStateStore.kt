package com.rudi.audioplayer.data

import android.content.Context
import com.rudi.audioplayer.util.AppLogger
import java.io.File

/**
 * [savedAtMs] — Batch 695: jam dinding (`System.currentTimeMillis()`) saat [positionMs] ditulis; 0 = tak diketahui. Dipakai
 * pemulihan pasca-update untuk menambah posisi dengan waktu yang berlalu sampai proses dimatikan (lihat
 * `UpdateResumePolicy.resumePosition`).
 */
data class SavedPlaybackState(
    val songIds: List<Long>,
    val index: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffleEnabled: Boolean,
    val speed: Float = 1.0f,
    val savedAtMs: Long = 0L
)

/**
 * Persists the last playback queue + position (+ repeat/shuffle mode) so listening can
 * resume exactly where the user left off, even after the app process was killed.
 *
 * Gap List #6 (Batch 108) — sebelumnya cuma songIds/index/positionMs tersimpan, repeat/shuffle
 * selalu reset ke default tiap resume. Ditambah `SCHEMA_VERSION` + `load()` dibungkus try/catch:
 *
 * Batch 317 — pola sama diulang untuk `speed` (Kecepatan Putar): sebelumnya cuma hidup di
 * ExoPlayer in-memory (hilang tiap proses di-kill, laporan user langsung), sekarang tersimpan +
 * dipulihkan lewat field ini. Lihat [PlayerViewModel.connect] untuk titik pulih (controller-
 * connect, bukan resumeFromSaved(), supaya berlaku ke lagu apa pun, bukan cuma lanjut queue lama).
 * SharedPreferences typed getters sendiri sudah aman dari ClassCastException lintas versi (beda
 * key kalau tipe berubah), tapi ini jaring pengaman eksplisit kalau versi data lampau pernah
 * menyimpan bentuk lain di masa depan — corrupt/incompatible state jatuh ke null (baris kosong),
 * bukan crash saat resume.
 */
class PlaybackStateStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Batch 695 — berkas XML SharedPreferences ini: cap waktu CADANGAN bagi simpanan lama yang ditulis sebelum KEY_SAVED_AT ada.
    private val prefsFile = File(context.dataDir, "shared_prefs/$PREFS_NAME.xml")

    fun save(
        songIds: List<Long>,
        index: Int,
        positionMs: Long,
        repeatMode: Int,
        shuffleEnabled: Boolean,
        speed: Float
    ) {
        prefs.edit()
            .putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
            .putString(KEY_IDS, songIds.joinToString(","))
            .putInt(KEY_INDEX, index)
            .putLong(KEY_POSITION, positionMs)
            .putInt(KEY_REPEAT_MODE, repeatMode)
            .putBoolean(KEY_SHUFFLE, shuffleEnabled)
            .putFloat(KEY_SPEED, speed)
            // Batch 695 — jam dinding penulisan: pemulihan pasca-update menambah posisi dengan (waktu mati proses - cap ini); log
            // v694: simpan berkala 14:09:03,8 (posisi 4.871.249) tetapi proses mati 14:09:07,3 -> lagu mundur ±3,5 dtk (bisa ±10 dtk).
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    /**
     * Batch 695 — cap waktu simpanan: KEY_SAVED_AT bila ada; simpanan lama (ditulis versi sebelum 695, tak punya kunci itu) memakai
     * waktu ubah berkas XML-nya (ditulis `apply()` beberapa ms setelah `save`, jadi selisihnya hanya membuat tambahan posisi LEBIH
     * KECIL, tak pernah berlebih) agar update PERTAMA setelah 695 pun tak mundur. 0 = tak diketahui.
     */
    private fun savedAtMs(): Long {
        val stamped = prefs.getLong(KEY_SAVED_AT, 0L)
        if (stamped > 0L) return stamped
        return try {
            prefsFile.lastModified().coerceAtLeast(0L)
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "Gagal membaca waktu ubah berkas playback_state", e)
            0L
        }
    }

    fun load(): SavedPlaybackState? {
        // Batch 108 push pertama (CI run 161) gagal compile: `return null`/`return null` di
        // dalam expression-body function (`fun load(): T? = try { ... }`) tidak diizinkan
        // Kotlin ("Returns are not allowed for functions with expression body"). Diperbaiki
        // jadi block body eksplisit ({ return try { ... } }) — `return` di dalam try/catch
        // sah selama function-nya sendiri block body, bukan expression body. Pelajaran: early-
        // return (`?: return null`) di dalam body TIDAK bisa dicampur dengan gaya
        // `fun x() = ...` sesingkat apa pun bodinya, walau tanpa early-return dia sah-sah saja.
        return try {
            val idsRaw = prefs.getString(KEY_IDS, null) ?: return null
            val ids = idsRaw.split(",").mapNotNull { it.toLongOrNull() }
            if (ids.isEmpty()) return null
            val index = prefs.getInt(KEY_INDEX, 0).coerceIn(0, ids.size - 1)
            val position = prefs.getLong(KEY_POSITION, 0L).coerceAtLeast(0L)
            // KEY_REPEAT_MODE/KEY_SHUFFLE ditambah di SCHEMA_VERSION 2 — belum ada di state
            // lama (versi 1), getInt/getBoolean default aman ke off tanpa versi checking
            // eksplisit.
            val repeatMode = prefs.getInt(KEY_REPEAT_MODE, 0)
            val shuffleEnabled = prefs.getBoolean(KEY_SHUFFLE, false)
            // KEY_SPEED ditambah di SCHEMA_VERSION 3 — sama seperti repeat/shuffle dulu (versi
            // 2), belum ada di state lama, getFloat default aman ke 1.0x tanpa version checking
            // eksplisit.
            val speed = prefs.getFloat(KEY_SPEED, 1.0f)
            SavedPlaybackState(ids, index, position, repeatMode, shuffleEnabled, speed, savedAtMs())
        } catch (e: Exception) {
            // Corrupt/incompatible state tidak boleh menjegal resume seluruh app — anggap saja
            // tidak ada state tersimpan, mulai dari kosong seperti install baru.
            AppLogger.e(TAG, "Gagal load playback state tersimpan, dianggap tidak ada", e)
            null
        }
    }

    companion object {
        private const val TAG = "PlaybackStateStore"
        private const val PREFS_NAME = "playback_state"
        private const val KEY_SCHEMA_VERSION = "schema_version"
        private const val KEY_IDS = "song_ids"
        private const val KEY_INDEX = "index"
        private const val KEY_POSITION = "position"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        private const val KEY_SHUFFLE = "shuffle_enabled"
        private const val KEY_SPEED = "speed"
        private const val KEY_SAVED_AT = "saved_at_wall_ms"
        private const val SCHEMA_VERSION = 3
    }
}
