package com.rudi.audioplayer.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.rudi.audioplayer.util.AppLogger
import com.rudi.audioplayer.util.ShizukuBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Release Downloader Spec — manual "check for update" flow (Settings → Lanjutan → Cek Update).
 * Entirely opt-in: nothing here runs unless the user taps the button. Deliberately isolated in
 * its own package/singleton so it never touches PlayerViewModel/PlaybackService or any existing
 * app logic — worst case if this fails, only the update screen shows an error.
 *
 * Batch 432 — background work pindah dari `Thread {}` mentah ke `CoroutineScope(SupervisorJob()
 * + Dispatchers.IO)` milik singleton ini sendiri: `SupervisorJob` supaya 1 gagal job (mis. check
 * gagal) tidak membatalkan job lain di scope yang sama, `Dispatchers.IO` untuk I/O jaringan/disk
 * sesuai SOP Thread Safety. Scope ini sengaja live selama proses aplikasi (bukan
 * viewModelScope) — job tetap definisikan cara kerja sama seperti Thread lama, tapi kini pakai
 * primitif yang terstruktur alih-alih Thread lepas tanpa pelacakan.
 */
object UpdateManager {

    private const val TAG = "UpdateManager"

    /** Batas waktu `pm install` via Shizuku: alir APK ±25 MB lewat stdin + pemasangan; macet lebih lama = jatuh ke installer sistem. */
    private const val SILENT_INSTALL_TIMEOUT_MS = 120_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    sealed class UpdateState {
        data object Idle : UpdateState()
        data object Checking : UpdateState()
        data class UpToDate(val currentVersion: String) : UpdateState()
        data class Available(val release: GitHubReleaseChecker.ReleaseInfo) : UpdateState()
        data class Downloading(val progressPercent: Int) : UpdateState()
        data class ReadyToInstall(val apkFile: File) : UpdateState()
        /** Batch 694 — pasang sedang berjalan (jalur Shizuku senyap, atau transisi singkat sebelum installer sistem dibuka). */
        data object Installing : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    fun checkForUpdate(currentVersionName: String) {
        _state.value = UpdateState.Checking
        scope.launch {
            val result = GitHubReleaseChecker.fetchLatest(
                owner = com.rudi.audioplayer.BuildConfig.UPDATE_REPO_OWNER,
                repo = com.rudi.audioplayer.BuildConfig.UPDATE_REPO_NAME
            )
            _state.value = when (result) {
                is GitHubReleaseChecker.CheckResult.Success -> {
                    // Tag ditulis CI sebagai "v<versionName>-run<N>" (lihat build.yml) —
                    // dicocokkan ke versionName lokal murni angka, bukan string tag mentah.
                    val latestVersion = result.release.tagName.removePrefix("v").substringBefore("-run")
                    if (latestVersion == currentVersionName) {
                        UpdateState.UpToDate(currentVersionName)
                    } else {
                        UpdateState.Available(result.release)
                    }
                }
                is GitHubReleaseChecker.CheckResult.NoApkAsset ->
                    UpdateState.Error("Rilis terbaru tidak punya file .apk")
                is GitHubReleaseChecker.CheckResult.Failure ->
                    UpdateState.Error(result.message)
            }
        }
    }

    fun downloadAndPrepareInstall(context: Context, release: GitHubReleaseChecker.ReleaseInfo) {
        val appContext = context.applicationContext
        scope.launch {
            val destFile = File(appContext.cacheDir, "update_${release.apkAssetName}")
            val result = UpdateDownloader.download(
                url = release.apkDownloadUrl,
                destFile = destFile,
                onProgress = { downloaded, total ->
                    if (total > 0) {
                        _state.value = UpdateState.Downloading(((downloaded * 100) / total).toInt())
                    }
                }
            )
            _state.value = when (result) {
                is UpdateDownloader.DownloadResult.Success -> UpdateState.ReadyToInstall(result.file)
                is UpdateDownloader.DownloadResult.Failure -> UpdateState.Error(result.message)
            }
        }
    }

    /**
     * Batch 694 — titik pasang tunggal. Shizuku READY (izin sudah diberikan) -> APK dipasang SENYAP lewat `pm install` di proses
     * Shizuku (tanpa dialog konfirmasi installer; lihat [ShizukuBridge.installApk]); sistem lalu mematikan proses ini dan
     * pemulihan pemutaran (UpdateResume*) berjalan seperti biasa. Shizuku tak siap ATAU gagal -> installer sistem ([launchInstall],
     * perilaku lama) sehingga pengguna tak pernah buntu; keluaran mentah kegagalan dicatat di log diagnostik.
     */
    fun install(context: Context, apkFile: File) {
        if (_state.value is UpdateState.Installing) return
        val appContext = context.applicationContext
        _state.value = UpdateState.Installing
        scope.launch {
            var installed = false
            if (ShizukuBridge.state(appContext) == ShizukuBridge.State.READY) {
                val result = ShizukuBridge.installApk(appContext, apkFile, SILENT_INSTALL_TIMEOUT_MS)
                installed = result.success
                if (installed) {
                    AppLogger.w(TAG, "Pasang senyap via Shizuku selesai: ${result.detail}")
                } else {
                    AppLogger.e(TAG, "Pasang senyap via Shizuku GAGAL (jatuh ke installer sistem). Keluaran mentah:\n${result.detail}")
                }
            }
            _state.value = UpdateState.ReadyToInstall(apkFile)
            if (!installed) {
                try {
                    launchInstall(appContext, apkFile)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Gagal membuka installer sistem", e)
                    _state.value = UpdateState.Error(e.message ?: "Gagal membuka installer sistem")
                }
            }
        }
    }

    /** Hands the downloaded APK to the system installer via FileProvider (see file_paths.xml /
     * AndroidManifest.xml provider entry) — never a bare file:// URI (blocked on Android N+). */
    fun launchInstall(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updateprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun reset() {
        _state.value = UpdateState.Idle
    }
}
