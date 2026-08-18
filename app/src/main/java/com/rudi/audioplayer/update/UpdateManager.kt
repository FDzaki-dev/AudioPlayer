package com.rudi.audioplayer.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Release Downloader Spec — manual "check for update" flow (Settings → Lanjutan → Cek Update).
 * Entirely opt-in: nothing here runs unless the user taps the button. Deliberately isolated in
 * its own package/singleton so it never touches PlayerViewModel/PlaybackService or any existing
 * app logic — worst case if this fails, only the update screen shows an error.
 *
 * Runs its background work on a plain Thread rather than a coroutine scope: this is a singleton
 * object (not a ViewModel), and a raw Thread avoids introducing a long-lived CoroutineScope that
 * would need its own cancellation/lifecycle management for what is a short, one-shot operation.
 */
object UpdateManager {

    sealed class UpdateState {
        data object Idle : UpdateState()
        data object Checking : UpdateState()
        data class UpToDate(val currentVersion: String) : UpdateState()
        data class Available(val release: GitHubReleaseChecker.ReleaseInfo) : UpdateState()
        data class Downloading(val progressPercent: Int) : UpdateState()
        data class ReadyToInstall(val apkFile: File) : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    fun checkForUpdate(currentVersionName: String) {
        _state.value = UpdateState.Checking
        Thread {
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
        }.start()
    }

    fun downloadAndPrepareInstall(context: Context, release: GitHubReleaseChecker.ReleaseInfo) {
        val appContext = context.applicationContext
        Thread {
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
        }.start()
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
