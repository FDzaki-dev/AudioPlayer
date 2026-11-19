package com.rudi.audioplayer.playback

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rudi.audioplayer.R
import com.rudi.audioplayer.util.AppLogger

/**
 * Batch 688 [permintaan user: pemutar harus pulih cepat / melanjutkan musik yang terjeda akibat instalasi update].
 *
 * Alur: instalasi paket mematikan proses (log: `PACKAGE_UPDATED importance=FOREGROUND`), sistem lalu mengirim
 * `ACTION_MY_PACKAGE_REPLACED` ke app baru. Receiver ini (didaftarkan di manifest) memutuskan lewat
 * [UpdateResumePolicy] apakah musik sedang diputar saat proses mati DAN matinya karena instalasi; bila ya, ia
 * memulai [PlaybackService] dengan [PlaybackService.ACTION_RESUME_AFTER_UPDATE] sehingga lagu terakhir lanjut
 * dari posisi tersimpan tanpa user membuka app / mengetuk apa pun.
 *
 * Dasar izin start foreground service dari background: dokumentasi Android 12+ ("Restrictions on starting a
 * foreground service from the background") memasukkan `ACTION_MY_PACKAGE_REPLACED` sebagai pengecualian; larangan
 * `mediaPlayback` Android 15+ hanya untuk `BOOT_COMPLETED`. Namun log perangkat ini (log_20260915_133616) pernah
 * membuktikan `ForegroundServiceStartNotAllowedException` terjadi walau API resmi benar, maka penolakan TIDAK
 * boleh menjatuhkan proses: jatuh ke notifikasi "Ketuk untuk melanjutkan" ([UpdateResumeNotifier]).
 */
class UpdateResumeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val appContext = context.applicationContext
        try {
            handle(appContext)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Pemulihan pasca-update gagal", e)
        }
    }

    private fun handle(context: Context) {
        val store = UpdateResumeStore(context)
        val wasPlaying = store.wasPlaying()
        val exits = readExitRecords(context)
        val decision = UpdateResumePolicy.decide(wasPlaying, exits, System.currentTimeMillis())
        val exitSummary = exits.take(LOGGED_EXITS).joinToString(",") { "${it.reason}@${it.timestampMs}" }
        AppLogger.w(
            TAG,
            "MY_PACKAGE_REPLACED: memutar_sebelum_update=$wasPlaying catatan_keluar=${exits.size} [$exitSummary] keputusan=$decision"
        )
        when (decision) {
            UpdateResumePolicy.Decision.RESUME -> {
                // Dikonsumsi SEKARANG: jalur pemulihan (service atau ketukan notifikasi) menyalakan penanda lagi lewat
                // listener pemain begitu benar-benar memutar; tak ada pemutaran ulang berantai.
                store.setWasPlaying(false)
                startResume(context)
            }
            // Penanda basi: proses lama tidak mati karena instalasi ini. Dibersihkan agar tak memicu di update berikutnya.
            UpdateResumePolicy.Decision.SKIP_NO_UPDATE_EXIT -> store.setWasPlaying(false)
            UpdateResumePolicy.Decision.SKIP_NOT_PLAYING -> Unit
        }
    }

    private fun startResume(context: Context) {
        val serviceIntent = Intent(context, PlaybackService::class.java)
            .setAction(PlaybackService.ACTION_RESUME_AFTER_UPDATE)
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "startForegroundService ditolak saat MY_PACKAGE_REPLACED; jatuh ke notifikasi", e)
            UpdateResumeNotifier.post(context)
        }
    }

    private fun readExitRecords(context: Context): List<ProcessExitRecord> {
        return try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return emptyList()
            manager.getHistoricalProcessExitReasons(context.packageName, 0, EXIT_HISTORY_MAX)
                .map { ProcessExitRecord(it.reason, it.timestamp, it.description) }
        } catch (e: Exception) {
            // tool gagal != lulus: tanpa bukti, kebijakan memilih TIDAK memutar (lihat UpdateResumePolicy).
            AppLogger.e(TAG, "Gagal membaca ApplicationExitInfo", e)
            emptyList()
        }
    }

    private companion object {
        const val TAG = "UpdateResume"
        const val EXIT_HISTORY_MAX = 8
        const val LOGGED_EXITS = 3
    }
}

/**
 * Penanda persisten "pemain berniat memutar" (lihat [UpdateResumePolicy.intendsToPlay]). Ditulis oleh
 * [PlaybackService] tiap `playWhenReady`/status/timeline berubah (hanya menulis disk bila nilainya berubah);
 * dibaca [UpdateResumeReceiver] setelah proses lama dimatikan instalasi. SharedPreferences `apply()` menaruh
 * nilai di memori seketika dan menulis disk async (pola sama `PlaybackStateStore`).
 */
class UpdateResumeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun wasPlaying(): Boolean = prefs.getBoolean(KEY_WAS_PLAYING, false)

    fun setWasPlaying(value: Boolean) {
        if (prefs.getBoolean(KEY_WAS_PLAYING, false) == value) return
        prefs.edit().putBoolean(KEY_WAS_PLAYING, value).apply()
        // Batch 689: tiap PERUBAHAN penanda dicatat (hanya saat berubah, jadi jarang). Log v688 14:14 menunjukkan
        // `memutar_sebelum_update=false` tanpa bukti kapan penanda terakhir ditulis; tanpa baris ini penanda yang
        // tak pernah menyala (bug listener) tak bisa dibedakan dari pemutar yang memang sedang berhenti.
        AppLogger.w(TAG, "Penanda berniat-memutar -> $value")
    }

    private companion object {
        const val TAG = "UpdateResume"
        const val PREFS_NAME = "update_resume"
        const val KEY_WAS_PLAYING = "was_playing"
    }
}

/**
 * Cadangan bila sistem menolak start foreground service dari receiver: notifikasi "Ketuk untuk melanjutkan".
 * Ketukan notifikasi mengirim [PlaybackService.ACTION_RESUME_AFTER_UPDATE] lewat
 * `PendingIntent.getForegroundService` (aksi user pada notifikasi = pengecualian resmi larangan background-start).
 * Memakai API framework (minSdk 31) seperti `PlaybackService.startForegroundColdStartNotification`.
 */
object UpdateResumeNotifier {
    private const val TAG = "UpdateResume"
    private const val CHANNEL_ID = "update_resume"
    private const val REQUEST_CODE = 102
    const val NOTIFICATION_ID = 7002

    fun post(context: Context) {
        val appContext = context.applicationContext
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                AppLogger.w(TAG, "Notifikasi cadangan dilewati: izin POST_NOTIFICATIONS belum diberikan")
                return
            }
            val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Lanjutkan Pemutaran", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            val resumeIntent = Intent(appContext, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_RESUME_AFTER_UPDATE)
            val pendingIntent = PendingIntent.getForegroundService(
                appContext,
                REQUEST_CODE,
                resumeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("SONIX diperbarui")
                .setContentText("Ketuk untuk melanjutkan pemutaran")
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
            AppLogger.w(TAG, "Notifikasi cadangan 'Ketuk untuk melanjutkan' ditampilkan")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Gagal menampilkan notifikasi cadangan", e)
        }
    }
}
