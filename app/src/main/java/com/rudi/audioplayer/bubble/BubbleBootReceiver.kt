package com.rudi.audioplayer.bubble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.rudi.audioplayer.data.FloatingBubbleStore
import com.rudi.audioplayer.util.AppLogger

/**
 * Batch 98 — sebelum ini, bubble cuma restart lagi kalau user MEMBUKA app-nya secara manual
 * (lihat `MainActivity`'s `LaunchedEffect(Unit)`) — kalau HP di-restart dan user belum sempat
 * buka AudioPlayer lagi, bubble tetap mati sampai kapan pun walau togglenya sebenarnya ON.
 * Receiver ini menutup celah itu: begitu `BOOT_COMPLETED` diterima, langsung cek preferensi +
 * izin lalu restart service tanpa perlu app dibuka dulu.
 *
 * Dua pengecekan wajib sebelum start, BUKAN cuma baca [FloatingBubbleStore] doang: izin overlay
 * bisa dicabut user dari Pengaturan sistem kapan saja tanpa lewat toggle di app ini sama sekali
 * (device settings selalu menang atas preferensi in-app) — start tanpa cek ulang bisa
 * menghasilkan `addView()` gagal senyap di [FloatingBubbleService] (sudah ditangani lewat
 * `runCatching` di sana, tapi lebih baik dicegah dari sini daripada dibiarkan gagal & di-log).
 */
class BubbleBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!FloatingBubbleStore(context).isEnabled()) return
        if (!Settings.canDrawOverlays(context)) return

        val serviceIntent = Intent(context, FloatingBubbleService::class.java)
        // BOOT_COMPLETED is nominally exempt from Android 12+'s background foreground-service
        // start restriction, but real device data (log_20260915_133616, 07:50:11) shows some
        // OEM/App-Standby states still refuse it with ForegroundServiceStartNotAllowedException,
        // which crashed the whole app on launch. A missed auto-restart of the bubble is a minor
        // regression (same fallback already documented above: user opens the app manually); a
        // crash on every boot is not. Never let this receiver take the app down.
        runCatching {
            context.startForegroundService(serviceIntent)
        }.onFailure { e ->
            AppLogger.e("BubbleBootReceiver", "startForegroundService ditolak saat BOOT_COMPLETED", e)
        }
    }
}
