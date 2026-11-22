package com.rudi.audioplayer.util

/**
 * Batch 719/722 — memo hasil muat sampul untuk pemanggil yang memuat sampul lagu yang SAMA berulang kali.
 *
 * DASAR (log v718, ekspor 22 Nov 18:22): 4 URI lagu TANPA sampul tertanam (probe `embeddedPicture: null` 4/4) masing-masing
 * gagal dimuat 12-13x dalam sehari; `WidgetUpdater` 7x per URI (±3 pembaruan penuh dalam ±1 dtk tiap service/proses baru mulai,
 * mis. pasca-update atau swipe-clean: onUpdate + metadata + status putar; pergantian lagu biasa hanya 1x). Tiap percobaan
 * memanggil `ContentResolver.loadThumbnail` lagi dan melempar lagi (lagu WAV 47 MB = berkas dibuka ulang oleh MediaProvider).
 *
 * Batch 722 (log v720, 23 Nov 06:47:28-29): memo SATU-SLOT Batch 719 kalah oleh pola A -> B -> A (Next lalu Previous dalam 1 dtk):
 * lagu B (bersampul) menimpa catatan "A tanpa sampul" sehingga A gagal dimuat ulang. Kini dua struktur: SATU slot sampul yang
 * sudah dimuat ([rememberHit] = satu bitmap saja, memori terbatas) dan himpunan "tanpa sampul" ber-batas ([rememberMiss] = hanya
 * kunci + waktu, tanpa bitmap) yang TIDAK tertimpa oleh sampul lagu lain.
 *
 * Kelas ini murni (tanpa tipe Android) supaya bisa dites di JVM. Catatan kedaluwarsa dianggap tak diketahui sehingga pemanggil
 * memuat ulang (sampul yang baru ditanam tetap muncul). Himpunan "tanpa sampul" dipangkas dari entri TERLAMA bila melebihi
 * [maxMisses]. Seluruh akses diserialkan lewat `@Synchronized`; waktu diberikan pemanggil ([nowMs], jam monotonik).
 */
class ThumbnailMemo<V : Any>(
    private val hitTtlMs: Long = HIT_TTL_MS,
    private val missTtlMs: Long = MISS_TTL_MS,
    private val maxMisses: Int = MAX_MISSES
) {

    /** Hasil [recall]. */
    sealed interface Recall<out T> {
        /** Tak ada catatan yang masih berlaku untuk kunci ini: pemanggil memuat sendiri. */
        object Unknown : Recall<Nothing>

        /** Sampul sudah dimuat untuk kunci ini dan masih berlaku. */
        class Hit<out T>(val value: T) : Recall<T>

        /** Pemuatan kunci ini baru saja gagal "tanpa sampul" dan catatannya masih berlaku: lewati pemuatan. */
        object Miss : Recall<Nothing>
    }

    private var hitKey: String? = null
    private var hitValue: V? = null
    private var hitAtMs = 0L

    // Urutan penyisipan = urutan waktu (kunci disegarkan dgn remove + put); entri terlama di depan.
    private val misses = LinkedHashMap<String, Long>()

    @Synchronized
    fun recall(key: String, nowMs: Long): Recall<V> {
        val held = hitValue
        if (held != null && hitKey == key) {
            return if (isFresh(hitAtMs, nowMs, hitTtlMs)) Recall.Hit(held) else Recall.Unknown
        }
        val missAtMs = misses[key] ?: return Recall.Unknown
        if (!isFresh(missAtMs, nowMs, missTtlMs)) {
            misses.remove(key)
            return Recall.Unknown
        }
        return Recall.Miss
    }

    @Synchronized
    fun rememberHit(key: String, value: V, nowMs: Long) {
        misses.remove(key)
        hitKey = key
        hitValue = value
        hitAtMs = nowMs
    }

    @Synchronized
    fun rememberMiss(key: String, nowMs: Long) {
        if (hitKey == key) {
            hitKey = null
            hitValue = null
        }
        misses.remove(key)
        misses[key] = nowMs
        if (misses.size > maxMisses) {
            misses.remove(misses.keys.first())
        }
    }

    /** Jam mundur ([nowMs] < [storedAtMs]) tak mungkin bagi jam monotonik; bila terjadi, catatan tak dipercaya. */
    private fun isFresh(storedAtMs: Long, nowMs: Long, ttlMs: Long): Boolean {
        val ageMs = nowMs - storedAtMs
        return ageMs >= 0L && ageMs < ttlMs
    }

    companion object {
        /** Sampul yang sudah dimuat dipakai ulang selama 2 menit (cukup untuk rentetan pembaruan widget saat lagu mulai). */
        const val HIT_TTL_MS = 120_000L

        /** "Tanpa sampul" diingat 10 menit; sesudahnya dicoba lagi sekali (berkas bisa dipasangi sampul). */
        const val MISS_TTL_MS = 600_000L

        /** Batas himpunan "tanpa sampul" (hanya kunci + waktu, tanpa bitmap): cukup untuk pola Next/Previous berurutan. */
        const val MAX_MISSES = 64
    }
}
