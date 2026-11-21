package com.rudi.audioplayer.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Satu filter profil parametrik — subset yang bisa dipetakan: peaking + low/high shelf. */
data class ParametricFilter(
    val type: Type,
    val frequencyHz: Double,
    val gainDb: Double,
    val q: Double
) {
    enum class Type { PEAK, LOW_SHELF, HIGH_SHELF }
}

/**
 * Hasil parse profil. [ignoredFilters] = baris filter AKTIF yang dilewati (tipe tak didukung
 * seperti LPQ/HPQ, nilai di luar batas waras, atau melebihi batas jumlah filter).
 */
data class ParametricProfile(
    val preampDb: Double,
    val filters: List<ParametricFilter>,
    val ignoredFilters: Int
)

/**
 * Batch 542 — impor profil EQ format AutoEq/EqualizerAPO (`ParametricEQ.txt`), ide diambil dari
 * sistem profil EQ Convx (`eq/data/ParametricEQParser`). Kode ditulis ulang dari nol (Convx
 * berlisensi GPL-3.0; yang dipakai hanya format file + rumus biquad standar RBJ Audio EQ Cookbook).
 *
 * BATAS YANG JUJUR: equalizer SONIX = `android.media.audiofx.Equalizer` (umumnya 5 band), BUKAN
 * mesin biquad parametrik. Jadi profil TIDAK diputar ulang sebagai filter sungguhan — respons
 * gabungan semua filter dihitung analitis lalu DISAMPEL pada frekuensi tengah tiap band perangkat
 * (+ preamp sebagai pergeseran semua band). Hasilnya perkiraan kasar kontur profil, bukan koreksi
 * presisi. Objek ini murni Kotlin tanpa dependensi Android (bisa diuji di JVM).
 *
 * Format baris yang dikenali:
 *   Preamp: -5.2 dB
 *   Filter 1: ON PK Fc 70 Hz Gain -6.7 dB Q 0.29
 *   Filter 2: ON LSC Fc 105 Hz Gain 8.8 dB Q 0.70   (LS/LSQ = sama; HSC/HS/HSQ = high shelf)
 */
object EqProfileImport {

    /** Batas baca teks — profil AutoEq nyata < 2 KB; sisanya dipotong, tidak pernah dimuat utuh. */
    const val MAX_TEXT_CHARS = 65_536

    private const val MAX_FILTERS = 32
    private const val SAMPLE_RATE_HZ = 48_000.0
    private const val MIN_FREQ_HZ = 10.0
    private const val MAX_FREQ_HZ = 20_000.0
    private const val MAX_ABS_GAIN_DB = 40.0
    private const val MAX_ABS_PREAMP_DB = 40.0
    private const val MIN_Q = 0.05
    private const val MAX_Q = 20.0
    private const val DEFAULT_PEAK_Q = 1.0
    private const val DEFAULT_SHELF_Q = 0.707
    private const val POWER_FLOOR = 1e-12

    private val PREAMP_REGEX = Regex("""^\s*Preamp\s*:\s*([-+]?\d+(?:\.\d+)?)\s*dB""", RegexOption.IGNORE_CASE)
    private val FILTER_REGEX = Regex("""^\s*Filter\s*\d*\s*:\s*(ON|OFF)\s+([A-Za-z]+)\b(.*)""", RegexOption.IGNORE_CASE)
    private val FC_REGEX = Regex("""\bFc\s+([-+]?\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
    private val GAIN_REGEX = Regex("""\bGain\s+([-+]?\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
    private val Q_REGEX = Regex("""\bQ\s+([-+]?\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

    /** @return profil, atau null kalau tidak ada satu pun filter yang bisa dipakai. */
    fun parse(text: String): ParametricProfile? {
        var preamp = 0.0
        var ignored = 0
        val filters = mutableListOf<ParametricFilter>()
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val preampMatch = PREAMP_REGEX.find(line)
            if (preampMatch != null) {
                preamp = preampMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                continue
            }
            val filterMatch = FILTER_REGEX.find(line) ?: continue
            if (!filterMatch.groupValues[1].equals("ON", ignoreCase = true)) continue
            val filter = if (filters.size >= MAX_FILTERS) {
                null
            } else {
                buildFilter(filterMatch.groupValues[2], filterMatch.groupValues[3])
            }
            if (filter == null) ignored++ else filters.add(filter)
        }
        if (filters.isEmpty()) return null
        return ParametricProfile(
            preampDb = preamp.coerceIn(-MAX_ABS_PREAMP_DB, MAX_ABS_PREAMP_DB),
            filters = filters,
            ignoredFilters = ignored
        )
    }

    /**
     * Respons gabungan (dB) seluruh filter pada [frequencyHz], TANPA preamp.
     */
    fun responseDb(profile: ParametricProfile, frequencyHz: Double): Double {
        var total = 0.0
        for (filter in profile.filters) {
            total += filterResponseDb(filter, frequencyHz)
        }
        return total
    }

    /**
     * Level tiap band perangkat (millibel) = respons profil di frekuensi tengah band + preamp,
     * dijepit ke rentang yang dilaporkan perangkat. Urutan hasil = urutan [centerFreqsHz].
     */
    fun toBandLevels(
        profile: ParametricProfile,
        centerFreqsHz: List<Int>,
        minMillibel: Int,
        maxMillibel: Int
    ): List<Short> {
        val lo = minOf(minMillibel, maxMillibel)
        val hi = maxOf(minMillibel, maxMillibel)
        return centerFreqsHz.map { freq ->
            val db = responseDb(profile, freq.toDouble()) + profile.preampDb
            val safeDb = if (db.isFinite()) db else 0.0
            (safeDb * 100.0).roundToInt().coerceIn(lo, hi).toShort()
        }
    }

    private fun buildFilter(typeToken: String, rest: String): ParametricFilter? {
        val type = when (typeToken.uppercase()) {
            "PK", "PEQ" -> ParametricFilter.Type.PEAK
            "LSC", "LS", "LSQ" -> ParametricFilter.Type.LOW_SHELF
            "HSC", "HS", "HSQ" -> ParametricFilter.Type.HIGH_SHELF
            else -> return null
        }
        val fc = FC_REGEX.find(rest)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        val gain = GAIN_REGEX.find(rest)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        val q = Q_REGEX.find(rest)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: if (type == ParametricFilter.Type.PEAK) DEFAULT_PEAK_Q else DEFAULT_SHELF_Q
        if (fc < MIN_FREQ_HZ || fc > MAX_FREQ_HZ) return null
        if (gain < -MAX_ABS_GAIN_DB || gain > MAX_ABS_GAIN_DB) return null
        if (q < MIN_Q || q > MAX_Q) return null
        return ParametricFilter(type, fc, gain, q)
    }

    /** Magnitudo (dB) satu biquad RBJ pada [frequencyHz], fs = 48 kHz. */
    private fun filterResponseDb(filter: ParametricFilter, frequencyHz: Double): Double {
        val a = 10.0.pow(filter.gainDb / 40.0)
        val w0 = 2.0 * PI * filter.frequencyHz / SAMPLE_RATE_HZ
        val cosW0 = cos(w0)
        val alpha = sin(w0) / (2.0 * filter.q)
        val shelfTerm = 2.0 * sqrt(a) * alpha
        // Urutan koefisien: b0, b1, b2, a0, a1, a2.
        val c = when (filter.type) {
            ParametricFilter.Type.PEAK -> doubleArrayOf(
                1.0 + alpha * a, -2.0 * cosW0, 1.0 - alpha * a,
                1.0 + alpha / a, -2.0 * cosW0, 1.0 - alpha / a
            )
            ParametricFilter.Type.LOW_SHELF -> doubleArrayOf(
                a * ((a + 1.0) - (a - 1.0) * cosW0 + shelfTerm),
                2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0),
                a * ((a + 1.0) - (a - 1.0) * cosW0 - shelfTerm),
                (a + 1.0) + (a - 1.0) * cosW0 + shelfTerm,
                -2.0 * ((a - 1.0) + (a + 1.0) * cosW0),
                (a + 1.0) + (a - 1.0) * cosW0 - shelfTerm
            )
            ParametricFilter.Type.HIGH_SHELF -> doubleArrayOf(
                a * ((a + 1.0) + (a - 1.0) * cosW0 + shelfTerm),
                -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0),
                a * ((a + 1.0) + (a - 1.0) * cosW0 - shelfTerm),
                (a + 1.0) - (a - 1.0) * cosW0 + shelfTerm,
                2.0 * ((a - 1.0) - (a + 1.0) * cosW0),
                (a + 1.0) - (a - 1.0) * cosW0 - shelfTerm
            )
        }
        val w = 2.0 * PI * frequencyHz / SAMPLE_RATE_HZ
        val cosW = cos(w)
        val cos2W = cos(2.0 * w)
        val numerator = c[0] * c[0] + c[1] * c[1] + c[2] * c[2] +
            2.0 * (c[0] * c[1] + c[1] * c[2]) * cosW + 2.0 * c[0] * c[2] * cos2W
        val denominator = c[3] * c[3] + c[4] * c[4] + c[5] * c[5] +
            2.0 * (c[3] * c[4] + c[4] * c[5]) * cosW + 2.0 * c[3] * c[5] * cos2W
        return 10.0 * log10(max(numerator, POWER_FLOOR) / max(denominator, POWER_FLOOR))
    }
}
