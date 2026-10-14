package com.rudi.audioplayer.util

/**
 * Batch 595 (laporan user: "Log aplikasi seharusnya descending agar masalah baru tidak menumpuk di bawah dan
 * buang-buang waktu nge-scroll") — mengurutkan teks Log Diagnostik menjadi TERBARU DI ATAS.
 *
 * Berkas di disk ([AppLogger]) SENGAJA tetap append-only (tulis murah, tahan crash, tak perlu menulis ulang
 * 200 KB tiap entri); urutannya dibalik saat DIBACA, sehingga satu perubahan ini otomatis berlaku untuk layar
 * Log Diagnostik, tombol salin, dan ekspor "Repack ke Dokumen" (semuanya lewat `AppLogger.readLog()`).
 *
 * Yang dibalik adalah ENTRI, bukan baris: satu entri bisa multi-baris (stack trace, trace ANR dengan baris kosong)
 * dan isinya harus tetap terbaca dari atas ke bawah. Awal entri = baris berformat persis keluaran
 * `AppLogger.appendEntry`: `yyyy-MM-dd HH:mm:ss LEVEL ...` (LEVEL = FATAL/ERROR/WARN). Baris di awal berkas
 * yang bukan awal entri (sisa entri terpotong oleh pemangkasan ukuran) diperlakukan sebagai entri tertua dan
 * tampil paling bawah. Murni Kotlin tanpa API Android supaya bisa diuji di JVM.
 */
internal object LogOrder {
    private val ENTRY_START = Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} (FATAL|ERROR|WARN) ")

    fun newestFirst(raw: String): String {
        if (raw.isBlank()) return raw
        val entries = ArrayList<StringBuilder>()
        for (line in raw.trimEnd('\n').split('\n')) {
            val current = entries.lastOrNull()
            if (current == null || ENTRY_START.containsMatchIn(line)) {
                entries.add(StringBuilder(line))
            } else {
                current.append('\n').append(line)
            }
        }
        return entries.asReversed().joinToString(separator = "\n", postfix = "\n")
    }
}
