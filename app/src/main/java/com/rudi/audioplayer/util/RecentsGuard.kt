package com.rudi.audioplayer.util

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Batch 623 — "exclude from recent apps" DINAMIS supaya sesi pemutaran tak ikut terbunuh saat user tak sengaja
 * men-swipe kartu aplikasi.
 *
 * DASAR (fakta log perangkat, Infinix X6850 / Android 16, 9 Nov 17:57:31): `ApplicationExitInfo` alasan=OTHER
 * importance=FOREGROUND_SERVICE deskripsi=`[Swipe-up clean] manual_swipUpClean` = proses (yang sedang memutar, foreground
 * service) dibunuh OEM SEKETIKA saat kartu di-swipe. `onTaskRemoved` TIDAK sempat terpanggil dan tak ada kode aplikasi yang
 * bisa mencegat pembunuhan itu (batas arsitektur, lihat KDoc `FloatingBubbleService.onTaskRemoved`). Satu-satunya tuas dari
 * kode = tidak menyediakan kartu untuk di-swipe: task ditandai `excludeFromRecents` lewat `AppTask.setExcludeFromRecents`.
 *
 * Aturan: task dikecualikan dari Recents SELAMA sesi pemutaran aktif ([onPlaybackActive] true dari `onIsPlayingChanged`
 * PlaybackService) dan tetap dikecualikan [GRACE_MS] setelah jeda (jeda singkat/telepon tak membuka celah swipe). Setelah
 * itu — atau saat sesi berakhir ([onSessionEnded]) — kartu kembali normal, jadi aplikasi tak hilang dari Recents selamanya.
 * [reapply] dipanggil `MainActivity.onResume` supaya task BARU (dibuka dari launcher saat musik main) ikut status terkini.
 *
 * BATAS JUJUR: ini hanya menyembunyikan kartu. Tombol "hapus semua"/pembersih OEM, force-stop, atau daftar autostart/baterai
 * OEM di luar jangkauan kode. Selama tersembunyi, aplikasi dibuka lewat launcher/notifikasi/widget/bubble, bukan Recents.
 * Murni tuas sistem + pencatatan: tidak pernah melempar, tak mengubah pemutaran.
 */
object RecentsGuard {
    private const val TAG = "RecentsGuard"
    private const val GRACE_MS = 10 * 60 * 1000L

    private val excluded = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val token = Any()

    /** Dipanggil dari `onIsPlayingChanged` (thread utama). `true` = mulai/lanjut memutar; `false` = jeda/berhenti. */
    fun onPlaybackActive(context: Context, active: Boolean) {
        val app = context.applicationContext
        mainHandler.removeCallbacksAndMessages(token)
        if (active) {
            update(app, true, "memutar", false)
        } else if (excluded.get()) {
            mainHandler.postDelayed({ update(app, false, "jeda melewati batas tenggang", false) }, token, GRACE_MS)
        }
    }

    /** Sesi pemutaran benar-benar berakhir (Service dihancurkan): kembalikan kartu Recents sekarang juga. */
    fun onSessionEnded(context: Context) {
        mainHandler.removeCallbacksAndMessages(token)
        update(context.applicationContext, false, "sesi berakhir", true)
    }

    /** Task baru/dibuka ulang harus mengikuti status terkini (juga membersihkan tanda basi dari proses yang sudah mati). */
    fun reapply(context: Context) {
        applyToTasks(context.applicationContext, excluded.get(), "activity tampil")
    }

    private fun update(context: Context, value: Boolean, reason: String, force: Boolean) {
        val changed = excluded.getAndSet(value) != value
        if (changed || force) applyToTasks(context, value, reason)
    }

    private fun applyToTasks(context: Context, value: Boolean, reason: String) {
        try {
            val manager = context.getSystemService(ActivityManager::class.java)
            val tasks = manager?.appTasks.orEmpty()
            for (task in tasks) task.setExcludeFromRecents(value)
            AppLogger.w(TAG, "Recents: excludeFromRecents=$value task=${tasks.size} alasan=$reason")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Gagal set excludeFromRecents=$value alasan=$reason", e)
        }
    }
}
