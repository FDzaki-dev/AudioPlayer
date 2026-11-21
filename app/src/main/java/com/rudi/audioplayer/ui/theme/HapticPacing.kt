package com.rudi.audioplayer.ui.theme

/**
 * Batch 709 — pola getar on/off dan pembatas laju untuk HP TANPA kontrol amplitudo (murni Kotlin, bisa diuji di JVM).
 *
 * DASAR (log v708, `dumpsys vibrator_manager`, Infinix X6850 / Android 16): `capabilities = []` (tanpa kontrol amplitudo),
 * efek prabuat CLICK/HEAVY_CLICK sampai ke motor (`usage: MEDIA`, status `finished`) tetapi terasa lemah ("setara Gboard"),
 * dan cabang amplitudo-penuh Batch 514 tak pernah aktif di HP ini. Satu-satunya tuas kekuatan yang tersisa tanpa kontrol
 * amplitudo adalah DURASI denyut, jadi denyut on/off kustom dipakai menggantikan efek prabuat.
 *
 * Angka di bawah = tebakan awal (belum diuji di tangan); bukti jalur baru ada di `dumpsys vibrator_manager` (MEDIA memuat
 * `Step=45ms` / `Step=90ms`, bukan `Prebaked=`). Menyetel rasa = mengubah konstanta di sini saja.
 */
internal object HapticPacing {

    /** Satu denyut "ketuk" (tick geser/drag), ms. */
    const val TAP_PULSE_MS = 45L

    /** Tiap denyut pada pola "kuat" (tekan lama), ms. */
    const val HEAVY_PULSE_MS = 90L

    /** Jeda mati di antara dua denyut pola "kuat", ms. */
    const val HEAVY_GAP_MS = 45L

    /** Jarak minimum antar-awal dua denyut ketuk: menyisakan jeda mati sepanjang satu denyut, supaya tiap tick terpisah jelas. */
    const val TAP_MIN_GAP_MS = TAP_PULSE_MS * 2

    /** Pola on/off tekan-lama untuk `createWaveform`: [jeda awal, ON, OFF, ON]. Array baru tiap panggilan (tak dibagi). */
    fun heavyTimings(): LongArray = longArrayOf(0L, HEAVY_PULSE_MS, HEAVY_GAP_MS, HEAVY_PULSE_MS)

    /** Total durasi pola "kuat", ms. */
    fun heavyTotalMs(): Long = heavyTimings().sum()
}

/** Dua tingkat haptic yang dipakai app: [TAP] = ketuk/tick, [HEAVY] = tekan lama. */
internal enum class HapticTier { TAP, HEAVY }

/**
 * Pembatas laju: framework membatalkan getaran yang sedang jalan saat getaran baru datang, jadi tick beruntun (drag/geser)
 * akan menyatu jadi dengung dan bisa memotong denyut kuat. [allow] menolak ketuk yang terlalu rapat dengan ketuk sebelumnya
 * atau yang jatuh saat pola kuat masih berjalan; pola kuat selalu diizinkan. Ketuk yang ditolak TIDAK memperpanjang jendela.
 * Waktu disuntik lewat [allow] (`SystemClock.elapsedRealtime()` di pemakai) supaya deterministik di tes.
 */
internal class HapticThrottle(
    private val tapMinGapMs: Long = HapticPacing.TAP_MIN_GAP_MS,
    private val heavyBusyMs: Long = HapticPacing.heavyTotalMs()
) {
    private var lastTapAt = NONE
    private var lastHeavyAt = NONE

    @Synchronized
    fun allow(tier: HapticTier, nowMs: Long): Boolean {
        val isHeavy = tier == HapticTier.HEAVY
        val allowed = isHeavy ||
            (!within(lastHeavyAt, nowMs, heavyBusyMs) && !within(lastTapAt, nowMs, tapMinGapMs))
        if (allowed) {
            if (isHeavy) lastHeavyAt = nowMs else lastTapAt = nowMs
        }
        return allowed
    }

    private companion object {
        const val NONE = -1L

        /** True bila [since] tercatat dan [nowMs] masih dalam [windowMs] sesudahnya (jam mundur dianggap di luar jendela). */
        fun within(since: Long, nowMs: Long, windowMs: Long): Boolean {
            val elapsed = nowMs - since
            return since != NONE && elapsed >= 0L && elapsed < windowMs
        }
    }
}
