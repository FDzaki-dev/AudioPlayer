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
import android.os.Process
import android.os.SystemClock
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
        try {
            UpdateResumeCoordinator.run(context.applicationContext, UpdateResumeCoordinator.TRIGGER_BROADCAST)
        } catch (e: Exception) {
            AppLogger.e("UpdateResume", "Pemulihan pasca-update gagal", e)
        }
    }
}

/**
 * Batch 691 — logika keputusan+pemicu pemulihan dipakai DUA pemicu:
 * 1. [TRIGGER_BROADCAST] (jalur terjamin, Batch 688): `ACTION_MY_PACKAGE_REPLACED`, dikecualikan resmi dari larangan start
 *    foreground service dari background.
 * 2. [TRIGGER_EARLY] (EKSPERIMEN): dari thread latar di `Application.onCreate`. Log v690 (19 Nov 19:18): proses baru lahir
 *    +1,0 dtk setelah proses lama mati, tetapi broadcast baru dikirim sistem ±1,1 dtk SETELAH itu (thread utama idle,
 *    tak ada pesan lambat sebelum receiver) — jendela ±0,8 dtk yang TAK bisa dipakai receiver. Apakah Android mengizinkan
 *    start FGS dari titik awal ini TIDAK diketahui (pengecualian resminya terikat pada penerimaan broadcast), maka
 *    penolakan hanya dicatat dan jalur 1 tetap berlaku (penanda TIDAK dikonsumsi bila start gagal).
 * Kebijakan sama persis ([UpdateResumePolicy]): penanda "memutar" + catatan keluar TERBARU = kematian oleh instalasi.
 * Pada cold start biasa percobaan awal diam (tanpa log) kecuali keputusan RESUME.
 */
object UpdateResumeCoordinator {
    private const val TAG = "UpdateResume"
    const val TRIGGER_BROADCAST = "MY_PACKAGE_REPLACED"
    const val TRIGGER_EARLY = "proses_mulai"
    private const val EXIT_HISTORY_MAX = 8
    private const val LOGGED_EXITS = 3

    /** Dipanggil dari `Application.onCreate`; thread daemon sehingga biaya di thread utama ~0. */
    fun startEarlyAttempt(context: Context) {
        val appContext = context.applicationContext
        val worker = Thread({
            try {
                run(appContext, TRIGGER_EARLY)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Percobaan awal pemulihan pasca-update gagal", e)
            }
        }, "UpdateResumeEarly")
        worker.isDaemon = true
        worker.start()
    }

    fun run(context: Context, trigger: String) {
        val early = trigger == TRIGGER_EARLY
        val store = UpdateResumeStore(context)
        val wasPlaying = store.wasPlaying()
        if (early && !wasPlaying) return
        val nowMs = System.currentTimeMillis()
        val exits = readExitRecords(context)
        val decision = UpdateResumePolicy.decide(wasPlaying, exits, nowMs)
        if (early && decision != UpdateResumePolicy.Decision.RESUME) return
        val exitSummary = exits.take(LOGGED_EXITS).joinToString(",") { "${it.reason}@${it.timestampMs}" }
        val sinceKillMs = exits.maxOfOrNull { it.timestampMs }?.let { nowMs - it } ?: -1L
        val sinceStartMs = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()
        AppLogger.w(
            TAG,
            "$trigger: memutar_sebelum_update=$wasPlaying catatan_keluar=${exits.size} [$exitSummary] keputusan=$decision " +
                "sejak_mati=${sinceKillMs}ms sejak_proses_mulai=${sinceStartMs}ms"
        )
        // Batch 695 — waktu mati proses (jam dinding) = dasar penambahan posisi pulih (UpdateResumePolicy.resumePosition).
        val killedAtMs = exits.maxOfOrNull { it.timestampMs } ?: 0L
        when (decision) {
            UpdateResumePolicy.Decision.RESUME ->
                if (early) startEarly(context, store, killedAtMs) else startFromBroadcast(context, store, killedAtMs)
            // Penanda basi: proses lama tidak mati karena instalasi ini. Dibersihkan agar tak memicu di update berikutnya.
            UpdateResumePolicy.Decision.SKIP_NO_UPDATE_EXIT -> store.setWasPlaying(false)
            UpdateResumePolicy.Decision.SKIP_NOT_PLAYING -> Unit
        }
    }

    private fun resumeIntent(context: Context, killedAtMs: Long): Intent =
        Intent(context, PlaybackService::class.java)
            .setAction(PlaybackService.ACTION_RESUME_AFTER_UPDATE)
            .putExtra(PlaybackService.EXTRA_KILLED_AT_MS, killedAtMs)

    private fun startFromBroadcast(context: Context, store: UpdateResumeStore, killedAtMs: Long) {
        // Dikonsumsi SEKARANG: jalur pemulihan (service atau ketukan notifikasi) menyalakan penanda lagi lewat
        // listener pemain begitu benar-benar memutar; tak ada pemutaran ulang berantai.
        store.setWasPlaying(false)
        try {
            ContextCompat.startForegroundService(context, resumeIntent(context, killedAtMs))
        } catch (e: Exception) {
            AppLogger.e(TAG, "startForegroundService ditolak saat MY_PACKAGE_REPLACED; jatuh ke notifikasi", e)
            UpdateResumeNotifier.post(context, killedAtMs)
        }
    }

    private fun startEarly(context: Context, store: UpdateResumeStore, killedAtMs: Long) {
        try {
            ContextCompat.startForegroundService(context, resumeIntent(context, killedAtMs))
            store.setWasPlaying(false)
            AppLogger.w(TAG, "Percobaan awal: start foreground service DIIZINKAN; pemulihan dimulai sebelum broadcast")
        } catch (e: Exception) {
            // Penanda tidak disentuh: jalur broadcast (terjamin) masih akan memutuskan sendiri.
            AppLogger.e(TAG, "Percobaan awal: start foreground service ditolak; jalur broadcast tetap berlaku", e)
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

    /**
     * Batch 695 — jam dinding saat penanda TERAKHIR berubah false -> true (tak dihapus saat turun ke false, supaya masih terbaca oleh
     * pemulihan yang menurunkan penanda lebih dulu); 0 = tak diketahui. Lihat `UpdateResumePolicy.resumePosition`.
     */
    fun playingSinceMs(): Long = prefs.getLong(KEY_PLAYING_SINCE, 0L)

    fun setWasPlaying(value: Boolean) {
        if (prefs.getBoolean(KEY_WAS_PLAYING, false) == value) return
        val editor = prefs.edit().putBoolean(KEY_WAS_PLAYING, value)
        if (value) editor.putLong(KEY_PLAYING_SINCE, System.currentTimeMillis())
        editor.apply()
        // Batch 689: tiap PERUBAHAN penanda dicatat (hanya saat berubah, jadi jarang). Log v688 14:14 menunjukkan
        // `memutar_sebelum_update=false` tanpa bukti kapan penanda terakhir ditulis; tanpa baris ini penanda yang
        // tak pernah menyala (bug listener) tak bisa dibedakan dari pemutar yang memang sedang berhenti.
        AppLogger.w(TAG, "Penanda berniat-memutar -> $value")
    }

    private companion object {
        const val TAG = "UpdateResume"
        const val PREFS_NAME = "update_resume"
        const val KEY_WAS_PLAYING = "was_playing"
        const val KEY_PLAYING_SINCE = "playing_since_wall_ms"
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

    fun post(context: Context, killedAtMs: Long = 0L) {
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
                .putExtra(PlaybackService.EXTRA_KILLED_AT_MS, killedAtMs)
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
