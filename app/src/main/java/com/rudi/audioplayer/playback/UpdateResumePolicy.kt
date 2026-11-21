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
     * Batch 701 — log v700 (21 Nov 05:37): pasang senyap Shizuku yang SAMA dicatat sistem sebagai `alasan=USER_REQUESTED`
     * (kode 10) `[FORCE STOP] stop <paket> due to pkg removed` (bukan PACKAGE_UPDATED) -> `keputusan=SKIP_NO_UPDATE_EXIT` dan
     * musik TIDAK lanjut setelah update. Teks "pkg removed" hanya muncul pada pencopotan/penggantian paket (Force stop dari
     * Pengaturan memakai teks lain), jadi ikut dihitung sebagai kematian-karena-update; jendela usia dan aturan "catatan
     * TERBARU" tak berubah.
     */
    fun isUpdateKill(record: ProcessExitRecord, nowMs: Long): Boolean {
        val age = nowMs - record.timestampMs
        if (age < -MAX_CLOCK_SKEW_MS || age > MAX_EXIT_AGE_MS) return false
        if (record.reason == EXIT_REASON_PACKAGE_UPDATED) return true
        val text = record.description ?: return false
        return text.contains("installPackage", ignoreCase = true) || text.contains("pkg removed", ignoreCase = true)
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

    /** Batch 693 — jumlah maksimum percobaan membaca+menyambung sisa antrean sebelum antrean tersimpan dibekukan. */
    const val REST_SPLICE_MAX_ATTEMPTS = 2

    /**
     * Batch 693 — sisa antrean dianggap HILANG bila tersimpan ada lagu lain ([expectedOthers] > 0) tetapi tak satu pun
     * ditemukan ([foundOthers] == 0): pola khas galat sementara MediaStore/IO, bukan lagu yang memang sudah dihapus
     * (itu hasil sebagian, tetap disambung apa adanya seperti jalur penuh).
     */
    fun isRestMissing(foundOthers: Int, expectedOthers: Int): Boolean = expectedOthers > 0 && foundOthers == 0

    /** Batch 693 — ulangi baca sisa antrean hanya untuk kasus [isRestMissing] dan selama percobaan belum habis. */
    fun shouldRetryRestSplice(foundOthers: Int, expectedOthers: Int, attempt: Int, maxAttempts: Int): Boolean =
        isRestMissing(foundOthers, expectedOthers) && attempt < maxAttempts

    /** Batch 695 — batas selisih (simpan -> proses mati) yang masih dipercaya untuk ditambahkan ke posisi; simpan berkala tiap 10 dtk. */
    const val MAX_POSITION_EXTRAPOLATION_MS = 30_000L

    /** Batch 695 — bila posisi hasil tambahan jatuh dalam jarak ini dari akhir lagu, tambahan dibatalkan (tak melompat ke akhir). */
    const val POSITION_END_MARGIN_MS = 2_000L

    /** Batch 695 — dasar keputusan [resumePosition]; dicatat di log supaya terlihat kenapa posisi tak/ditambah. */
    enum class PositionBasis { EXTRAPOLATED, NO_KILL_TIME, NO_SAVE_STAMP, NO_GAP, GAP_TOO_LARGE, NEAR_SONG_END }

    /** Batch 695 — [positionMs] dipakai pemain; [extrapolatedMs] = tambahan terhadap posisi tersimpan (0 bila tak ditambah). */
    data class ResumePosition(val positionMs: Long, val extrapolatedMs: Long, val basis: PositionBasis)

    /**
     * Batch 695 — log v694 (14:09): simpan berkala terakhir 14:09:03,8 (posisi 4.871.249 ms), proses dimatikan pemasangan
     * 14:09:07,3 (`ApplicationExitInfo`), posisi nyata saat mati ±4.874.800 ms, tetapi pemulihan memakai 4.871.249 -> ±3,5 dtk
     * TERULANG (batas atas = interval simpan 10 dtk). Karena musik berjalan terus (penanda "berniat memutar" menyala sampai mati),
     * posisi saat mati = posisi tersimpan + (waktu mati - waktu simpan) x kecepatan; berlaku untuk SEMUA jalur pasang (installer
     * sistem, Shizuku, adb) karena hanya butuh cap waktu, bukan aba-aba sebelum pasang.
     *
     * Dasar penjumlahan = yang LEBIH BARU antara [savedAtMs] dan [playingSinceMs] (kapan penanda memutar terakhir menyala):
     * bila user menjeda lalu memutar lagi setelah simpan, posisi tersimpan = posisi saat dijeda, jadi hanya waktu sejak mulai
     * memutar lagi yang dihitung. Sengaja KONSERVATIF — kembali ke posisi tersimpan apa adanya (tanpa tambahan) bila: waktu mati /
     * cap simpan tak diketahui, selisih <= 0 (jam dinding bergeser), selisih > [MAX_POSITION_EXTRAPOLATION_MS], atau hasil jatuh
     * dalam [POSITION_END_MARGIN_MS] dari akhir lagu ([durationMs] > 0). Kecepatan tak valid (<= 0 / NaN) dianggap 1x.
     */
    fun resumePosition(
        savedPositionMs: Long,
        savedAtMs: Long,
        playingSinceMs: Long,
        killedAtMs: Long,
        speed: Float,
        durationMs: Long
    ): ResumePosition {
        fun unchanged(basis: PositionBasis) = ResumePosition(savedPositionMs, 0L, basis)
        if (killedAtMs <= 0L) return unchanged(PositionBasis.NO_KILL_TIME)
        if (savedAtMs <= 0L) return unchanged(PositionBasis.NO_SAVE_STAMP)
        val gapMs = killedAtMs - maxOf(savedAtMs, playingSinceMs)
        if (gapMs <= 0L) return unchanged(PositionBasis.NO_GAP)
        if (gapMs > MAX_POSITION_EXTRAPOLATION_MS) return unchanged(PositionBasis.GAP_TOO_LARGE)
        val factor = if (speed.isNaN() || speed <= 0f) 1.0 else speed.toDouble()
        val extraMs = (gapMs * factor).toLong()
        val resumed = savedPositionMs + extraMs
        if (durationMs > 0L && resumed > durationMs - POSITION_END_MARGIN_MS) return unchanged(PositionBasis.NEAR_SONG_END)
        return ResumePosition(resumed, extraMs, PositionBasis.EXTRAPOLATED)
    }

    private const val STATE_IDLE = 1
    private const val STATE_ENDED = 4
}
