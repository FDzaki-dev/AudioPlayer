package com.rudi.audioplayer.ui.theme

/**
 * Batch 724 — skala tampilan global (murni Kotlin, bisa diuji di JVM). Dipasang SATU kali di [AudioPlayerTheme] lewat
 * `LocalDensity`, jadi berlaku untuk semua layar, sheet, dialog, dan popup di bawah tema tanpa menyentuh call site.
 *
 * DASAR (keluhan user "tampilan serba jumbo, tidak kelihatan minimalis" + log v723, Infinix X6850 / Android 16):
 * `fontScale=1.15` (ukuran font sistem 115%) terbaca di baris `Marquee diag`. Compose mengonversi `sp` dengan
 * `fontScale x density`, jadi SEMUA teks app (28/17/15/13/11sp) ikut membesar 15% di atas desainnya, sementara kotak
 * ber-`dp` (tab bawah, mini player, baris daftar, chip, FAB) tetap besar sesuai desain -> tampilan terasa padat dan jumbo.
 *
 * Dua tuas, keduanya hanya dibaca dari sini (menyetel rasa = mengubah konstanta di bawah saja):
 * - [DENSITY_FACTOR]: pengali kepadatan; seluruh `dp` DAN `sp` mengecil sebesar (1 - faktor). 0.90 = 10% lebih kecil.
 * - [MAX_FONT_SCALE]: batas atas skala font sistem yang dihormati app. 1.0 = pembesaran font sistem DIABAIKAN di dalam
 *   app (pengecilan di bawah 1.0 tetap dihormati). Teks kini mengecil lebih banyak daripada kotaknya, jadi risiko teks
 *   terpotong/melipat hanya berkurang, tidak bertambah.
 */
internal object UiScale {

    /** Pengali kepadatan layar untuk seluruh isi app (`dp` dan `sp`). */
    const val DENSITY_FACTOR = 0.90f

    /** Batas atas skala font sistem yang dipakai di dalam app. */
    const val MAX_FONT_SCALE = 1.0f

    /** Kepadatan efektif di dalam app dari kepadatan asli perangkat [baseDensity]. */
    fun density(baseDensity: Float): Float = baseDensity * DENSITY_FACTOR

    /** Skala font efektif di dalam app dari skala font sistem [baseFontScale] (hanya dibatasi dari atas). */
    fun fontScale(baseFontScale: Float): Float = baseFontScale.coerceAtMost(MAX_FONT_SCALE)
}
