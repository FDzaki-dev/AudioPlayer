package com.rudi.audioplayer.util

/**
 * Batch 719 — memo SATU-SLOT hasil muat sampul untuk pemanggil yang memuat sampul lagu yang SAMA berulang kali.
 *
 * DASAR (log v718, ekspor 22 Nov 18:22): 4 URI lagu TANPA sampul tertanam (probe `embeddedPicture: null` 4/4) masing-masing
 * gagal dimuat 12-13x dalam sehari; `WidgetUpdater` 7x per URI (±3 pembaruan penuh dalam ±1 dtk tiap lagu mulai: onUpdate +
 * metadata + status putar). Tiap percobaan memanggil `ContentResolver.loadThumbnail` lagi dan melempar lagi (lagu WAV 47 MB =
 * berkas dibuka ulang oleh MediaProvider). Lagu BERSAMPUL ikut dimuat + didecode ulang pada tiap pembaruan penuh.
 *
 * Kelas ini murni (tanpa tipe Android) supaya bisa dites di JVM: menyimpan hasil TERAKHIR per kunci ([rememberHit] = nilai, [rememberMiss] =
 * "tanpa sampul") dengan masa berlaku. Kunci berbeda = catatan lama dibuang (satu slot, memori terbatas satu bitmap).
 * Catatan kedaluwarsa dianggap tak diketahui sehingga pemanggil memuat ulang (sampul yang baru ditanam tetap muncul).
 *
 * Seluruh akses diserialkan lewat `@Synchronized`; waktu diberikan pemanggil ([nowMs], jam monotonik) agar tes tak butuh jam asli.
 */
class ThumbnailMemo<V : Any>(
    private val hitTtlMs: Long = HIT_TTL_MS,
    private val missTtlMs: Long = MISS_TTL_MS
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

    private var slotKey: String? = null
    private var slotValue: V? = null
    private var storedAtMs = 0L

    @Synchronized
    fun recall(key: String, nowMs: Long): Recall<V> {
        if (slotKey != key) return Recall.Unknown
        val stored = slotValue
        val ttlMs = if (stored != null) hitTtlMs else missTtlMs
        val ageMs = nowMs - storedAtMs
        // Jam mundur (ageMs < 0) tak mungkin bagi jam monotonik; bila terjadi, catatan tak dipercaya.
        if (ageMs < 0L || ageMs >= ttlMs) return Recall.Unknown
        return if (stored != null) Recall.Hit(stored) else Recall.Miss
    }

    @Synchronized
    fun rememberHit(key: String, value: V, nowMs: Long) {
        slotKey = key
        slotValue = value
        storedAtMs = nowMs
    }

    @Synchronized
    fun rememberMiss(key: String, nowMs: Long) {
        slotKey = key
        slotValue = null
        storedAtMs = nowMs
    }

    companion object {
        /** Sampul yang sudah dimuat dipakai ulang selama 2 menit (cukup untuk rentetan pembaruan widget saat lagu mulai). */
        const val HIT_TTL_MS = 120_000L

        /** "Tanpa sampul" diingat 10 menit; sesudahnya dicoba lagi sekali (berkas bisa dipasangi sampul). */
        const val MISS_TTL_MS = 600_000L
    }
}
