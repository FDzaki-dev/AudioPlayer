package com.rudi.audioplayer.data

import android.content.Context

/**
 * Persists the sleep timer's absolute end-timestamp (epoch millis), not a remaining-duration
 * countdown. Gap List #7 (Batch 109) — sebelumnya sleep timer HANYA hidup sebagai
 * `viewModelScope.launch` di `PlayerViewModel`, hilang total kalau proses di-kill (jarang tapi
 * nyata: OS bisa kill seluruh proses termasuk ViewModel-nya sementara `PlaybackService`
 * foreground diminta system tetap hidup/di-restart via Playback Resumption) — timer diam-diam
 * tidak pernah berbunyi, lagu terus main tanpa batas.
 *
 * Menyimpan TIMESTAMP absolut (bukan "sisa menit") supaya begitu `PlaybackService` dibuat ulang
 * (onCreate), sisa waktu tinggal dihitung ulang dari `endAt - now` — tidak butuh tahu berapa
 * lama proses sempat mati.
 */
class SleepTimerStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Null artinya tidak ada timer aktif tersimpan. */
    fun setEndAt(endAtMillis: Long?) {
        if (endAtMillis == null) {
            prefs.edit().remove(KEY_END_AT).apply()
        } else {
            prefs.edit().putLong(KEY_END_AT, endAtMillis).apply()
        }
    }

    fun getEndAt(): Long? {
        val value = prefs.getLong(KEY_END_AT, -1L)
        return if (value <= 0L) null else value
    }

    companion object {
        private const val PREFS_NAME = "sleep_timer"
        private const val KEY_END_AT = "end_at_millis"
    }
}
