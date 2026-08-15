package com.rudi.audioplayer.bubble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.rudi.audioplayer.data.FloatingBubbleStore

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        val serviceIntent = Intent(context, FloatingBubbleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
