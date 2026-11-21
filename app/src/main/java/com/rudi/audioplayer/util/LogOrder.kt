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

    // Sama dengan ENTRY_START tetapi mengenali awal BARIS di tengah teks (untuk mencari batas entri pada teks utuh).
    private val ENTRY_START_ANYWHERE =
        Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} (FATAL|ERROR|WARN) ", RegexOption.MULTILINE)

    /**
     * Batch 679 — pemangkasan berkas log ([AppLogger]) per ENTRI berbasis UKURAN. Sebelumnya `trim` membuang separuh BARIS
     * tertua: (1) batas bisa jatuh di tengah entri multi-baris sehingga entri tertua yang tersisa kehilangan kepalanya
     * (baris `ERROR ... : java.lang.X: pesan` hilang, tinggal `\tat ...`) — padahal log wajib memuat masalah mentah
     * apa adanya; (2) separuh BARIS bukan separuh BYTE: berkas dengan sedikit baris sangat panjang (`PowerProbe` ~600
     * karakter, dump) bisa tetap di atas batas sesudah dipangkas sehingga SETIAP tulis berikutnya membaca-menulis ulang
     * 200 KB lagi.
     *
     * Aturan: pertahankan ±separuh teks terbaru, dimulai dari awal ENTRI pertama pada/setelah titik tengah (entri yang
     * melintasi titik tengah dibuang UTUH). Bila tak ada awal entri setelah titik tengah (satu entri raksasa), potong di
     * batas baris berikutnya supaya berkas PASTI mengecil. Murni Kotlin; berkas diakhiri `\n` per entri sehingga hasil
     * tetap berakhir `\n`.
     */
    fun keepNewestHalf(raw: String): String {
        if (raw.isEmpty()) return raw
        val mid = raw.length / 2
        val entryStart = ENTRY_START_ANYWHERE.find(raw, mid)?.range?.first
        val from = entryStart ?: (raw.indexOf('\n', mid) + 1)
        return raw.substring(if (from in 1 until raw.length) from else mid)
    }

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
