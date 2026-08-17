package com.rudi.audioplayer.data

import java.io.InputStream
import java.io.OutputStream

/**
 * Gap List "Wajib" #1 (Tag/Metadata Editor) — MVP scope, format MP3/ID3v2.3 saja, disengaja.
 *
 * Kenapa cuma MP3, bukan FLAC/OGG/M4A/WMA sekaligus: masing-masing format itu format biner
 * TOTAL BEDA (Vorbis comment block untuk FLAC/OGG, atom `moov/udta/meta` untuk M4A, dst) —
 * nulis 1 writer yang benar per format tanpa compiler/device sungguhan untuk verifikasi adalah
 * risiko tinggi (file musik user bisa rusak kalau ada bug). ID3v2 dipilih duluan karena MP3
 * adalah format paling umum di library musik nyata, dan strukturnya paling sederhana untuk
 * ditulis dengan aman (tag selalu di AWAL file, byte audio sesudahnya tidak pernah disentuh).
 * Format lain sengaja dapat pesan "belum didukung" di [TagEditor], BUKAN gagal diam-diam.
 *
 * Kenapa cuma ID3v2.3 (bukan v2.4 atau dual-write v1+v2): v2.3 dibaca praktis semua player
 * (termasuk yang cuma dukung v2.3, sebagian belum dukung v2.4 sepenuhnya) — pilihan paling
 * kompatibel untuk 1 writer tunggal. ID3v1 (128 byte trailer di akhir file) SENGAJA tidak
 * ditulis/dihapus — kalaupun ada ID3v1 lama yang jadi "basi", hampir semua player modern
 * prioritaskan ID3v2 kalau keduanya ada, jadi ini cuma imperfection kosmetik, bukan bug
 * fungsional. Dicatat sebagai gap tersisa di CHANGELOG, bukan diselesaikan diam-diam sebagai
 * "sudah lengkap".
 */
object Id3TagWriter {

    /** Field yang bisa diedit lewat UI. Null/blank pada field opsional = frame itu tidak
     *  ditulis sama sekali (bukan ditulis kosong) — title/artist/album tetap ditulis walau
     *  blank supaya user bisa sengaja mengosongkannya (beda dari field opsional lain). */
    data class EditableTags(
        val title: String,
        val artist: String,
        val album: String,
        val albumArtist: String? = null,
        val genre: String? = null,
        val composer: String? = null,
        val trackNumber: Int? = null,
        val discNumber: Int? = null
    )

    /** Ukuran total tag ID3v2 LAMA (10 byte header + isi frame), dibaca dari 10 byte pertama
     *  file. 0 kalau file ini memang belum punya tag ID3v2 sama sekali (byte audio langsung
     *  mulai dari offset 0) — bukan error, kasus paling umum untuk file yang belum pernah
     *  ditag. [header10] harus persis 10 byte pertama file, dibaca APA ADANYA (belum tentu
     *  semua 10 byte kebaca kalau filenya lebih pendek dari itu — caller yang jaga itu). */
    fun readExistingTagSize(header10: ByteArray): Int {
        if (header10.size < 10) return 0
        if (header10[0] != 'I'.code.toByte() || header10[1] != 'D'.code.toByte() || header10[2] != '3'.code.toByte()) {
            return 0
        }
        // Byte index 6-9: ukuran "syncsafe" — cuma 7 bit signifikan per byte (bit paling
        // kiri/MSB selalu 0), supaya decoder MPEG lama yang cari byte 0xFF (sync frame audio)
        // tidak pernah salah kira bagian dalam angka ukuran ini sebagai awal frame audio.
        val size = ((header10[6].toInt() and 0x7F) shl 21) or
            ((header10[7].toInt() and 0x7F) shl 14) or
            ((header10[8].toInt() and 0x7F) shl 7) or
            (header10[9].toInt() and 0x7F)
        return size + 10 // +10 supaya caller bisa langsung skip total byte tag lama (header+isi)
    }

    /** Encode int ke 4-byte syncsafe (dipakai HANYA di header tag ID3v2, BUKAN di header
     *  frame individual — beda dari ID3v2.4 yang syncsafe di frame juga; v2.3 yang ditulis di
     *  sini pakai ukuran frame 32-bit biasa, lihat [textFrame]). */
    private fun syncsafe(size: Int): ByteArray = byteArrayOf(
        ((size shr 21) and 0x7F).toByte(),
        ((size shr 14) and 0x7F).toByte(),
        ((size shr 7) and 0x7F).toByte(),
        (size and 0x7F).toByte()
    )

    private fun bigEndian4(v: Int): ByteArray = byteArrayOf(
        ((v shr 24) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(),
        (v and 0xFF).toByte()
    )

    /** 1 frame teks ID3v2.3 lengkap (header 10 byte + isi). Encoding dipilih UTF-16LE+BOM
     *  (byte encoding indicator 0x01), BUKAN Latin-1 (0x00) — supaya judul/artis non-Latin
     *  (nama dengan huruf beraksen, dll., umum di library musik Indonesia) tidak keliru
     *  ditulis. Konsisten dipakai untuk SEMUA frame termasuk yang isinya angka (TRCK/TPOS) —
     *  player tetap baca angkanya benar walau encoding-nya "berlebihan" untuk kasus itu;
     *  encoding seragam berarti lebih sedikit percabangan = lebih sedikit permukaan bug. */
    fun textFrame(id: String, value: String): ByteArray {
        require(id.length == 4) { "ID3v2.3 frame id harus 4 karakter, dapat '$id'" }
        val bom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        val textBytes = value.toByteArray(Charsets.UTF_16LE)
        val content = byteArrayOf(0x01) + bom + textBytes
        val header = id.toByteArray(Charsets.US_ASCII) +
            bigEndian4(content.size) +
            byteArrayOf(0x00, 0x00) // frame flags, tidak dipakai
        return header + content
    }

    /** Susun 1 blok tag ID3v2.3 baru yang lengkap (header 10 byte + semua frame), siap ditulis
     *  persis di awal file. title/artist/album SELALU ditulis (termasuk kalau blank — itu
     *  pilihan sengaja user, lihat dok [EditableTags]); field lain hanya ditulis kalau ada
     *  isinya (blank/null = frame itu tidak ditulis sama sekali, BUKAN ditulis kosong). */
    fun buildTag(tags: EditableTags): ByteArray {
        val frames = mutableListOf<ByteArray>()
        frames += textFrame("TIT2", tags.title)
        frames += textFrame("TPE1", tags.artist)
        frames += textFrame("TALB", tags.album)
        tags.albumArtist?.takeIf { it.isNotBlank() }?.let { frames += textFrame("TPE2", it) }
        tags.genre?.takeIf { it.isNotBlank() }?.let { frames += textFrame("TCON", it) }
        tags.composer?.takeIf { it.isNotBlank() }?.let { frames += textFrame("TCOM", it) }
        tags.trackNumber?.takeIf { it > 0 }?.let { frames += textFrame("TRCK", it.toString()) }
        tags.discNumber?.takeIf { it > 0 }?.let { frames += textFrame("TPOS", it.toString()) }

        var framesSize = 0
        for (f in frames) framesSize += f.size
        val framesBytes = ByteArray(framesSize)
        var offset = 0
        for (f in frames) {
            System.arraycopy(f, 0, framesBytes, offset, f.size)
            offset += f.size
        }

        val header = "ID3".toByteArray(Charsets.US_ASCII) +
            byteArrayOf(0x03, 0x00) + // versi 2.3.0
            byteArrayOf(0x00) + // tag flags, tidak dipakai
            syncsafe(framesBytes.size)
        return header + framesBytes
    }

    /**
     * Alirkan [input] (file MP3 utuh, posisi awal persis di byte 0) ke [output] dengan tag
     * ID3v2 LAMA (kalau ada) diganti [newTag] — byte audio SETELAH tag lama disalin apa
     * adanya, tidak pernah diproses/didekode. Caller yang buka & tutup kedua stream (fungsi
     * ini murni menulis) supaya caller bebas validasi tujuan sebelum/sesudah dengan aman.
     * TIDAK menyentuh trailer ID3v1 (128 byte terakhir file, kalau ada) — lihat catatan scope
     * di dok objek ini.
     */
    fun rewrite(input: InputStream, output: OutputStream, newTag: ByteArray) {
        val header10 = ByteArray(10)
        val read = input.read(header10)
        val oldTagTotalSize = if (read == 10) readExistingTagSize(header10) else 0
        output.write(newTag)
        if (oldTagTotalSize == 0) {
            // Bukan tag ID3v2 (atau file lebih pendek dari 10 byte) — 10 byte yang sudah
            // terlanjur dibaca dari input itu byte AUDIO, bukan tag, jadi harus ikut ditulis
            // balik, bukan dibuang.
            if (read > 0) output.write(header10, 0, read)
        } else if (oldTagTotalSize > 10) {
            // Skip sisa tag lama (di luar 10 byte header yang sudah kebaca).
            var toSkip = (oldTagTotalSize - 10).toLong()
            val buf = ByteArray(8192)
            while (toSkip > 0) {
                val n = input.read(buf, 0, minOf(buf.size.toLong(), toSkip).toInt())
                if (n <= 0) break
                toSkip -= n
            }
        }
        input.copyTo(output, bufferSize = 8192)
    }
}
