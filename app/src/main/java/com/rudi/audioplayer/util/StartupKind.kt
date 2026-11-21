package com.rudi.audioplayer.util

/**
 * Batch 680 — penentu `jenis=` pada baris `StartupTiming` `Cold start` ([StartupTrace]). Murni Kotlin tanpa API Android supaya
 * ambangnya bisa diuji di JVM terhadap angka log NYATA.
 *
 * Dasar ambang (log v677 + v679, Infinix X6850 / Android 16): jeda `app_selesai` -> `act_mulai` pada start dingin murni
 * (force-stop -> buka dari launcher) = 33/36/38/41 ms (4 sampel); pada proses yang dihidupkan lebih dulu TANPA UI = 1860 ms
 * (pid 21122, v679: `installPackageLI` mematikan proses lama 06:20:06, `PlayerWidgetProvider.onUpdate` menghidupkan proses
 * baru 06:20:08, Activity baru dibuka 06:20:10 = jeda ketukan "Buka" pemasang), 11480 ms (pid 8027, v677). Ambang Batch 679
 * (2000 ms) SALAH menandai kasus 1860 ms sebagai `start_dingin_murni`; 500 ms = >12x jeda start dingin terbesar yang terukur
 * dan jauh di bawah latensi ketuk manusia.
 */
internal object StartupKind {
    const val HEADLESS_GAP_MS = 500L

    /** Akhiran baris (diawali ` | `). [appDoneMs]/[activityStartMs] = ms sejak proses mulai pada titik `app_selesai`/`act_mulai`. */
    fun describe(appDoneMs: Long, activityStartMs: Long): String {
        val gap = activityStartMs - appDoneMs
        return if (gap >= HEADLESS_GAP_MS) {
            " | jenis=PROSES_HIDUP_TANPA_UI(jeda app_selesai->act_mulai=${gap}ms; BUKAN start dingin murni)"
        } else {
            " | jenis=start_dingin_murni(jeda app_selesai->act_mulai=${gap}ms)"
        }
    }
}
