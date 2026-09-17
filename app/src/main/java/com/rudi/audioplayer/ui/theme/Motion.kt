package com.rudi.audioplayer.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Batch 481 — token gerak bersama, dibuat sebagai respons instruksi eksplisit user ("polish
 * semua efek animasi/transisi biar mulus like iOS, landai, tanpa peralihan instant yang
 * mengganggu"). File BARU murni — keberadaannya sendiri 0 mengubah 1 baris pun kode lama;
 * dipakai eksplisit pertama kali di `MainActivity.kt` (lihat catatan Batch 481 di sana untuk
 * titik pemakaian & alasan file mana yang disentuh batch ini).
 *
 * [IosEasing] mendekati kurva bawaan iOS `UIView.AnimationCurve.easeInEaseOut` /
 * `CAMediaTimingFunction(controlPoints: 0.42, 0.0, 0.58, 1.0)` — S-curve simetris (landai di
 * awal, puncak kecepatan di tengah, landai lagi di akhir). Ini BEDA dari easing default
 * `tween()` bawaan Compose (`FastOutSlowInEasing`, cubic-bezier Material 0.4/0.0/0.2/1.0 —
 * deselerasi berat sejak awal, "gaya Android"), yang jadi sebab animasi terasa kurang "iOS"
 * meski durasinya sudah pas. Ganti easing TIDAK mengubah durasi/threshold/formula gesture
 * apa pun yang sudah ada — non-breaking murni di lapisan kurva interpolasi saja.
 *
 * Konstanta durasi di bawah SEMUA reuse angka yang SUDAH dipakai berulang app-wide (audit
 * `tween(...)` app-wide Batch 481: 150/200/220/300/350ms adalah 4 nilai paling sering
 * dipakai) — bukan angka baru, murni pemberian nama supaya adopsi bertahap batch berikutnya
 * tinggal reuse token, tidak perlu menebak/mengetik ulang angka mentah.
 */
object Motion {
    /** Kurva ease-in-out gaya iOS. Pakai di `tween(..., easing = Motion.IosEasing)`. */
    val IosEasing: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

    /** Micro-feedback / fade cepat (reuse angka existing, mis. exitTransition NavHost). */
    const val DURATION_QUICK = 150

    /** Transisi standar (reuse angka existing, mis. enterTransition NavHost). */
    const val DURATION_STANDARD = 200

    /** Sinkron tab/pill bottom nav (reuse angka existing, MainActivity Batch 448). */
    const val DURATION_TAB = 220

    /** Push/pop hierarkis, mis. drill-down Stats Dashboard (reuse angka existing). */
    const val DURATION_EMPHASIZED = 300

    /** Slide layar penuh, mis. Now Playing (reuse angka existing). */
    const val DURATION_SCREEN = 350
}
