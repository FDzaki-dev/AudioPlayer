package com.rudi.audioplayer.data

import android.content.Context
import android.util.Log

data class SavedPlaybackState(
    val songIds: List<Long>,
    val index: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffleEnabled: Boolean,
    val speed: Float = 1.0f
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
            .apply()
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
            SavedPlaybackState(ids, index, position, repeatMode, shuffleEnabled, speed)
        } catch (e: Exception) {
            // Corrupt/incompatible state tidak boleh menjegal resume seluruh app — anggap saja
            // tidak ada state tersimpan, mulai dari kosong seperti install baru.
            Log.w(TAG, "Gagal load playback state tersimpan, dianggap tidak ada", e)
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
        private const val SCHEMA_VERSION = 3
    }
}
