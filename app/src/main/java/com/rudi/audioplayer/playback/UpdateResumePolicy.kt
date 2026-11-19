package com.rudi.audioplayer.playback

/**
 * Satu catatan keluar proses (cuplikan `ApplicationExitInfo`), dipisah dari tipe Android supaya keputusan
 * di [UpdateResumePolicy] bisa diuji di JVM murni.
 */
data class ProcessExitRecord(
    val reason: Int,
    val timestampMs: Long,
    val description: String?
)

/**
 * Batch 688 [permintaan user: pemutar harus pulih cepat / melanjutkan musik yang terjeda akibat instalasi update].
 *
 * DASAR (log perangkat Infinix X6850 / Android 16, 19 Nov 08:48): musik sedang jalan (`playing=true`, 08:48:23), lalu
 * `ApplicationExitInfo alasan=PACKAGE_UPDATED importance=FOREGROUND ... due to installPackageLI` (08:48:25) mematikan
 * proses beserta service pemutar; proses baru lahir 1 dtk kemudian hanya untuk widget (`onUpdate`), `PlaybackService`
 * baru dibuat 3 dtk kemudian oleh Activity, dan antrean dipulihkan DALAM KEADAAN JEDA (`PAUSED`, `playing=false`).
 *
 * Keputusan "lanjutkan atau tidak" sengaja membutuhkan DUA bukti:
 * 1. [wasPlaying] — penanda persisten milik [UpdateResumeStore] ("pemain memang berniat memutar" saat proses mati);
 * 2. catatan keluar proses terbaru yang menyatakan proses dimatikan OLEH INSTALASI (bukan LMK/OEM/crash) dan baru saja.
 * Bukti 2 mencegah musik menyala tiba-tiba: penanda basi (proses lama mati mendadak saat memutar lalu tak pernah dibuka
 * lagi) tidak boleh membunyikan audio berhari-hari kemudian hanya karena ada update.
 */
object UpdateResumePolicy {
    /** `ApplicationExitInfo.REASON_PACKAGE_UPDATED` (API 34); literal karena compileSdk/stub tak selalu memuat konstanta. */
    const val EXIT_REASON_PACKAGE_UPDATED = 16

    /** Jarak maksimum catatan keluar proses ke waktu broadcast agar dianggap "karena update ini" (instal + dexopt lambat). */
    const val MAX_EXIT_AGE_MS = 10 * 60_000L

    /** Toleransi jam dinding bergeser mundur (sinkronisasi NTP) antara catatan keluar proses dan broadcast. */
    private const val MAX_CLOCK_SKEW_MS = 60_000L

    enum class Decision { RESUME, SKIP_NOT_PLAYING, SKIP_NO_UPDATE_EXIT }

    /**
     * True bila [record] = proses dimatikan oleh instalasi paket baru, dan masih dalam jendela [MAX_EXIT_AGE_MS].
     * API 34+ memberi `REASON_PACKAGE_UPDATED`; API 31-33 (belum diverifikasi di perangkat) diperkirakan memberi
     * alasan lain dengan `description` berisi "installPackageLI" (format teks AOSP di log: "stop <paket> due to
     * installPackageLI"), jadi deskripsi dipakai sebagai cadangan.
     */
    fun isUpdateKill(record: ProcessExitRecord, nowMs: Long): Boolean {
        val age = nowMs - record.timestampMs
        if (age < -MAX_CLOCK_SKEW_MS || age > MAX_EXIT_AGE_MS) return false
        if (record.reason == EXIT_REASON_PACKAGE_UPDATED) return true
        return record.description?.contains("installPackage", ignoreCase = true) == true
    }

    /**
     * Batch 689 — HANYA catatan keluar TERBARU yang menentukan (bukan "ada satu saja"): kematian proses yang lebih baru
     * dari kematian-karena-update berarti penanda "memutar" sudah basi. Skenario yang ditutup: update #1 (proses
     * dilanjutkan, penanda menyala) -> OEM mematikan proses 3 menit kemudian tanpa aba-aba (penanda tetap true) ->
     * update #2 saat app tidak berjalan; catatan update #1 masih dalam jendela 10 menit tetapi bukan yang terbaru.
     * Urutan daftar tidak diandalkan (dipilih dari cap waktu); catatan berstempel sama dengan yang terbaru ikut dinilai.
     * Log perangkat 19 Nov: daftar sistem urut terbaru-dulu dan catatan PACKAGE_UPDATED memang yang terbaru.
     */
    fun decide(wasPlaying: Boolean, exits: List<ProcessExitRecord>, nowMs: Long): Decision {
        if (!wasPlaying) return Decision.SKIP_NOT_PLAYING
        if (exits.isEmpty()) return Decision.SKIP_NO_UPDATE_EXIT
        val newestTimestamp = exits.maxOf { it.timestampMs }
        val newestIsUpdateKill = exits.any { it.timestampMs == newestTimestamp && isUpdateKill(it, nowMs) }
        return if (newestIsUpdateKill) Decision.RESUME else Decision.SKIP_NO_UPDATE_EXIT
    }

    /**
     * Penanda "pemain berniat memutar": `playWhenReady` aktif, ada antrean, dan bukan IDLE/ENDED. Dipisah dari
     * `Player` (parameter primitif) agar bisa diuji. [playbackState] memakai konstanta `Player.STATE_*`
     * (IDLE=1, BUFFERING=2, READY=3, ENDED=4).
     */
    fun intendsToPlay(playWhenReady: Boolean, mediaItemCount: Int, playbackState: Int): Boolean =
        playWhenReady && mediaItemCount > 0 && playbackState != STATE_IDLE && playbackState != STATE_ENDED

    /**
     * Batch 692 — apakah sisa antrean tersimpan boleh disambung ke pemain: HANYA bila pemain masih berisi satu lagu = lagu
     * pembuka yang dipulihkan tadi. Hitungan lain / mediaId lain = user atau komponen lain sudah mengganti antrean sehingga
     * menyisipkan antrean lama akan merusaknya.
     */
    fun canSpliceRestoredQueue(currentMediaId: String?, headId: Long, mediaItemCount: Int): Boolean =
        mediaItemCount == 1 && currentMediaId == headId.toString()

    private const val STATE_IDLE = 1
    private const val STATE_ENDED = 4
}
