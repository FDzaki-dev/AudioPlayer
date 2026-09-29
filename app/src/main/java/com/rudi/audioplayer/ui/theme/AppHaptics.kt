package com.rudi.audioplayer.ui.theme

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.rudi.audioplayer.util.AppLogger
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Batch 511 — FIX regresi dilaporkan user: "haptic selalu lemah, gak pernah terasa nyata terpasang"
// (SONIX v510). Audit source (read-only, Batch 510): 75 titik haptic di seluruh app semuanya lewat
// `LocalHapticFeedback` bawaan Compose, hanya 2 jenis — `TextHandleMove` (43x, tick paling halus
// platform) dan `LongPress` (35x) — tanpa Vibrator/VibrationEffect, tanpa permission VIBRATE, tanpa
// helper terpusat. Bawaan Compose meneruskan ke `View.performHapticFeedback`, yang (a) mengikuti
// setelan "getar sentuh" sistem dan (b) kekuatannya ditentukan tuning motor tiap HP — kalau
// salah satunya lemah/mati, app tidak punya cadangan.
//
// Pendekatan: SATU titik. `AudioPlayerTheme` (Theme.kt) menyediakan `LocalHapticFeedback` versi ini
// untuk seluruh pohon UI (pola sama `LocalOverscrollFactory` Batch 364) — ke-75 call site TIDAK
// disentuh. Hanya 2 jenis yang dipakai app ini yang dialihkan: `LongPress` -> tier KUAT,
// `TextHandleMove` -> tier KETUK. Jenis lain -> dilempar balik ke implementasi bawaan
// (delegasi `by platform`), jadi perilaku jenis lain 0 berubah.
//
// Getaran dikirim LANGSUNG via `Vibrator.vibrate(VibrationEffect)` (butuh permission VIBRATE,
// ditambah di AndroidManifest.xml — permission normal, tanpa dialog). Konsekuensi yang SENGAJA
// diambil (sesuai permintaan user "haptic harus terasa nyata"): jalur ini TIDAK mengikuti toggle
// "getar sentuh" sistem. Kalau `vibrate()` gagal (mis. SecurityException) -> jatuh ke
// implementasi bawaan Compose (= perilaku lama), bukan diam.
//
// Pilihan efek: efek hardware bawaan (`EFFECT_CLICK` / `EFFECT_HEAVY_CLICK`) HANYA dipakai kalau
// `areEffectsSupported` melapor YES untuk keduanya (garansi tajam di motor linear). Selain itu
// (NO/UNKNOWN — umum di HP kelas menengah) dipakai `createOneShot` dengan durasi & amplitudo
// eksplisit di bawah, supaya pasti terasa, tidak bergantung fallback pabrikan.
//
// **Belum ditest di device asli** (sandbox tanpa Gradle/Android/device). Angka durasi di bawah
// ADALAH TEBAKAN AWAL yang wajar, bukan hasil ukur — kalau masih kurang/kebablasan, cukup ubah 2
// konstanta ini (1 baris) tanpa menyentuh call site.

// Batch 512 — laporan user atas v511: "gak merasakan ada nya perbaikan haptic feedback nyata selain dari
// getaran musik yang dimainkan". Akar masalah BELUM terbukti (0 log/0 device di sandbox) — ada 3 kandidat
// yang tidak bisa dibedakan dari source saja: (1) denyut one-shot 30/55ms terlalu pendek utk motor
// lemah (ERM) sehingga nyaris tak terasa; (2) wrapper ini tidak pernah terpanggil; (3) sistem HP MENGABAIKAN
// vibrate() (setelan intensitas getar sistem). Maka batch ini: (a) denyut diperkuat (50/100ms) — tetap
// TEBAKAN, 2 konstanta di bawah; (b) diagnostik DITAMBAH supaya kandidat (2) & (3) terbedakan dari
// Log Diagnostik: baris status kini memuat info HP + setelan getar sistem, dan 6 panggilan haptic PERTAMA
// per proses dicatat (membuktikan wrapper terpanggil); (c) bug v511 diperbaiki: kegagalan vibrate()
// dulu TIDAK PERNAH tercatat karena flag "sekali" sudah terpakai oleh baris status awal.

private const val HAPTIC_TAG = "AppHaptics"
private const val TAP_ONE_SHOT_MS = 50L
private const val HEAVY_ONE_SHOT_MS = 100L

// Jumlah panggilan haptic PERTAMA per proses yang dicatat ke Log Diagnostik (bukti wrapper terpanggil);
// dibatasi supaya log tidak banjir saat drag/scroll memicu tick berulang.
private const val HAPTIC_TRACE_LIMIT = 6

private val hapticLogScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

@Volatile
private var hapticStatusLogged = false

@Volatile
private var hapticFailureLogged = false

private val hapticCallCount = AtomicInteger(0)

internal class HapticEffects(
    val tap: VibrationEffect,
    val heavy: VibrationEffect,
    val mode: String
)

private fun buildHapticEffects(vibrator: Vibrator): HapticEffects {
    // Kode dukungan platform: 0 = UNKNOWN, 1 = YES, 2 = NO (urutan: CLICK/HEAVY_CLICK).
    val support: IntArray? = runCatching {
        vibrator.areEffectsSupported(
            VibrationEffect.EFFECT_CLICK,
            VibrationEffect.EFFECT_HEAVY_CLICK
        )
    }.getOrNull()
    val supportLabel = support?.joinToString("/") ?: "n/a"
    val hardwareEffectsOk = support != null && support.isNotEmpty() &&
        support.all { it == Vibrator.VIBRATION_EFFECT_SUPPORT_YES }
    if (hardwareEffectsOk) {
        return HapticEffects(
            tap = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK),
            heavy = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK),
            mode = "hardware-effects(dukungan CLICK/HEAVY=$supportLabel)"
        )
    }
    val amplitude = if (vibrator.hasAmplitudeControl()) 255 else VibrationEffect.DEFAULT_AMPLITUDE
    return HapticEffects(
        tap = VibrationEffect.createOneShot(TAP_ONE_SHOT_MS, amplitude),
        heavy = VibrationEffect.createOneShot(HEAVY_ONE_SHOT_MS, amplitude),
        mode = "one-shot(${TAP_ONE_SHOT_MS}ms/${HEAVY_ONE_SHOT_MS}ms, amp=$amplitude, " +
            "dukungan CLICK/HEAVY=$supportLabel)"
    )
}

// Foto setelan getar sistem SAAT tema dibuat (bukan realtime). Kunci selain HAPTIC_FEEDBACK_ENABLED adalah
// setelan tersembunyi (hidden) — tidak ada di semua ROM, jadi dibaca defensif: gagal/tak ada -> "n/a".
// Nilai intensitas umumnya 0 = mati, 1 = rendah, 2 = sedang, 3 = tinggi.
private fun systemHapticSnapshot(context: Context): String {
    fun setting(key: String): String =
        runCatching { Settings.System.getInt(context.contentResolver, key).toString() }
            .getOrDefault("n/a")
    val ringer = runCatching { context.getSystemService(AudioManager::class.java)?.ringerMode?.toString() }
        .getOrNull() ?: "n/a"
    return "sdk=${Build.VERSION.SDK_INT} hp=${Build.MANUFACTURER}/${Build.MODEL} " +
        "getar_sentuh=${setting(Settings.System.HAPTIC_FEEDBACK_ENABLED)} " +
        "vibrate_on=${setting("vibrate_on")} " +
        "intensitas_sentuh=${setting("haptic_feedback_intensity")} " +
        "intensitas_media=${setting("media_vibration_intensity")} ringer=$ringer"
}

// Sekali per proses supaya Log Diagnostik tidak banjir; I/O file di IO dispatcher (bukan Main).
private fun logHapticStatusOnce(message: String) {
    if (hapticStatusLogged) return
    hapticStatusLogged = true
    hapticLogScope.launch { AppLogger.w(HAPTIC_TAG, message) }
}

// Terpisah dari flag status di atas (bug v511: flag yang sama dipakai keduanya, jadi kegagalan vibrate()
// tidak pernah tercatat karena baris status awal sudah menghabiskan "jatah sekali").
private fun logHapticFailureOnce(message: String) {
    if (hapticFailureLogged) return
    hapticFailureLogged = true
    hapticLogScope.launch { AppLogger.w(HAPTIC_TAG, message) }
}

private fun traceHaptic(message: String) {
    hapticLogScope.launch { AppLogger.w(HAPTIC_TAG, message) }
}

private fun obtainVibrator(context: Context): Vibrator? = runCatching {
    context.getSystemService(VibratorManager::class.java)?.defaultVibrator?.takeIf { it.hasVibrator() }
}.getOrNull()

internal class StrongHapticFeedback(
    private val platform: HapticFeedback,
    private val vibrator: Vibrator?,
    systemInfo: String
) : HapticFeedback by platform {

    private val effects: HapticEffects? = vibrator?.let { buildHapticEffects(it) }

    init {
        logHapticStatusOnce(
            if (effects != null) "Haptic aktif: mode=${effects.mode} | $systemInfo"
            else "Haptic: tidak ada Vibrator/tidak mendukung getar, pakai haptic bawaan Compose | $systemInfo"
        )
    }

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        val isHeavy = hapticFeedbackType == HapticFeedbackType.LongPress
        val isTap = hapticFeedbackType == HapticFeedbackType.TextHandleMove
        val tier = if (isHeavy) "kuat" else if (isTap) "ketuk" else "delegasi"
        val effect = if (isHeavy) effects?.heavy else if (isTap) effects?.tap else null
        val n = hapticCallCount.incrementAndGet()
        val trace = n <= HAPTIC_TRACE_LIMIT
        if (effect != null && vibrator != null) {
            try {
                vibrator.vibrate(effect)
                if (trace) traceHaptic("panggilan #$n tier=$tier: vibrate() dikirim tanpa exception")
                return
            } catch (e: Exception) {
                logHapticFailureOnce("Haptic: vibrate() gagal (${e.javaClass.simpleName}), jatuh ke haptic bawaan Compose")
                if (trace) traceHaptic("panggilan #$n tier=$tier: vibrate() gagal (${e.javaClass.simpleName})")
            }
        } else if (trace) {
            traceHaptic("panggilan #$n tier=$tier: diteruskan ke haptic bawaan Compose")
        }
        platform.performHapticFeedback(hapticFeedbackType)
    }
}

/** Dipanggil SEKALI dari `AudioPlayerTheme` (bukan dari call site). Dibaca SEBELUM provider aktif,
 * jadi `LocalHapticFeedback.current` di sini = implementasi bawaan Compose (dipakai sbg delegasi
 * & fallback). */
@Composable
internal fun rememberStrongHapticFeedback(): HapticFeedback {
    val platform = LocalHapticFeedback.current
    val context = LocalContext.current
    return remember(platform, context) {
        StrongHapticFeedback(platform, obtainVibrator(context), systemHapticSnapshot(context))
    }
}
