package com.rudi.audioplayer.ui.theme

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.rudi.audioplayer.util.AppLogger
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
private const val HAPTIC_TAG = "AppHaptics"
private const val TAP_ONE_SHOT_MS = 30L
private const val HEAVY_ONE_SHOT_MS = 55L

private val hapticLogScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

@Volatile
private var hapticStatusLogged = false

internal class HapticEffects(
    val tap: VibrationEffect,
    val heavy: VibrationEffect,
    val mode: String
)

private fun buildHapticEffects(vibrator: Vibrator): HapticEffects {
    val hardwareEffectsOk = runCatching {
        vibrator.areEffectsSupported(
            VibrationEffect.EFFECT_CLICK,
            VibrationEffect.EFFECT_HEAVY_CLICK
        ).all { it == Vibrator.VIBRATION_EFFECT_SUPPORT_YES }
    }.getOrDefault(false)
    if (hardwareEffectsOk) {
        return HapticEffects(
            tap = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK),
            heavy = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK),
            mode = "hardware-effects"
        )
    }
    val amplitude = if (vibrator.hasAmplitudeControl()) 255 else VibrationEffect.DEFAULT_AMPLITUDE
    return HapticEffects(
        tap = VibrationEffect.createOneShot(TAP_ONE_SHOT_MS, amplitude),
        heavy = VibrationEffect.createOneShot(HEAVY_ONE_SHOT_MS, amplitude),
        mode = "one-shot(${TAP_ONE_SHOT_MS}ms/${HEAVY_ONE_SHOT_MS}ms, amp=$amplitude)"
    )
}

// Sekali per proses supaya Log Diagnostik tidak banjir; I/O file di IO dispatcher (bukan Main).
private fun logHapticStatusOnce(message: String) {
    if (hapticStatusLogged) return
    hapticStatusLogged = true
    hapticLogScope.launch { AppLogger.w(HAPTIC_TAG, message) }
}

private fun obtainVibrator(context: Context): Vibrator? = runCatching {
    context.getSystemService(VibratorManager::class.java)?.defaultVibrator?.takeIf { it.hasVibrator() }
}.getOrNull()

internal class StrongHapticFeedback(
    private val platform: HapticFeedback,
    private val vibrator: Vibrator?
) : HapticFeedback by platform {

    private val effects: HapticEffects? = vibrator?.let { buildHapticEffects(it) }

    init {
        logHapticStatusOnce(
            if (effects != null) "Haptic aktif: mode=${effects.mode}"
            else "Haptic: tidak ada Vibrator/tidak mendukung getar, pakai haptic bawaan Compose"
        )
    }

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        val effect = when (hapticFeedbackType) {
            HapticFeedbackType.LongPress -> effects?.heavy
            HapticFeedbackType.TextHandleMove -> effects?.tap
            else -> null
        }
        if (effect != null && vibrator != null) {
            try {
                vibrator.vibrate(effect)
                return
            } catch (e: Exception) {
                logHapticStatusOnce("Haptic: vibrate() gagal (${e.javaClass.simpleName}), jatuh ke haptic bawaan Compose")
            }
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
        StrongHapticFeedback(platform, obtainVibrator(context))
    }
}
