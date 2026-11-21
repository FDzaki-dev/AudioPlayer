package com.rudi.audioplayer.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Batch 681 — pintu "tulis hanya bila berubah", per kunci. Murni Kotlin tanpa API Android supaya perilakunya bisa
 * diuji di JVM terhadap pola log NYATA.
 *
 * Dasar (log v680, `sonix_debug_20261118_063950.txt`): 212 dari 771 baris Log Diagnostik = `[WidgetUpdater] Marquee diag`
 * (54.412 dari 176.578 byte = 30,8% isi log; batas log 200 KB lalu dipangkas separuh), padahal hanya 36 baris yang membawa
 * informasi baru (diulang tiap pembaruan widget — tiap ganti lagu / putar-jeda — dengan ukuran/mode/skala font yang sama).
 * Simulasi pada log itu (pintu direset tiap proses baru): 36 baris / 9.181 byte tersisa, hemat 45.231 byte = 25,6% log.
 *
 * Aman dari banyak thread (peta konkuren). [changed] membandingkan dengan tanda TERAKHIR (bukan semua yang pernah terlihat):
 * A -> B -> A tertulis tiga kali, karena itu memang tiga perubahan keadaan.
 */
internal class ChangeGate {
    private val lastSignature = ConcurrentHashMap<Int, String>()

    /**
     * `true` bila [signature] untuk [key] berbeda dari panggilan sebelumnya atau [key] baru dilihat; tanda baru disimpan.
     * `false` bila sama persis (jangan tulis log).
     */
    fun changed(key: Int, signature: String): Boolean = lastSignature.put(key, signature) != signature
}
