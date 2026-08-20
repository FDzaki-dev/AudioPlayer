package com.rudi.audioplayer.update

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Reads GitHub's public "latest release" API to find this repo's newest signed APK asset.
 * Separate from UpdateDownloader on purpose: this only ever fetches a small JSON document
 * (a few KB), so a plain `.string()` read is fine here — the "never buffer in RAM" rule in
 * the Release Downloader Spec is about the multi-MB APK binary, not this metadata call.
 */
object GitHubReleaseChecker {

    data class ReleaseInfo(
        val tagName: String,
        val htmlUrl: String,
        val apkDownloadUrl: String,
        val apkAssetName: String,
        // Batch 156 — isi field "body" GitHub Release (pesan commit git HEAD saat rilis dibuat,
        // lihat step "Determine version name"/"Create GitHub Release" di build.yml). Kosong
        // ("") kalau CI belum sempat mengisi body (rilis lama sebelum Batch 156) atau API
        // benar-benar tidak mengembalikannya — UpdateCheckSheet.kt WAJIB cek blank sebelum
        // menampilkan section catatan rilis, jangan asumsikan selalu terisi.
        val releaseNotes: String
    )

    sealed class CheckResult {
        data class Success(val release: ReleaseInfo) : CheckResult()
        data object NoApkAsset : CheckResult()
        data class Failure(val message: String) : CheckResult()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** owner/repo come from BuildConfig.UPDATE_REPO_OWNER/NAME (see app/build.gradle.kts) —
     * never hardcode a repo here. githubToken is optional (raises the 60/hr anonymous rate
     * limit to 5000/hr; not required for a public repo). */
    fun fetchLatest(owner: String, repo: String, githubToken: String? = null): CheckResult {
        val requestBuilder = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
        if (!githubToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $githubToken")
        }

        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return CheckResult.Failure("HTTP ${response.code}")
                }
                val json = JSONObject(response.body?.string().orEmpty())
                val tagName = json.optString("tag_name", "")
                val htmlUrl = json.optString("html_url", "")
                // Batch 156 — "body" GitHub Release API = teks yang softprops/action-gh-release
                // kirim lewat parameter `body:` di step "Create GitHub Release" (build.yml),
                // isinya pesan commit git HEAD. optString fallback "" kalau field tidak ada
                // sama sekali di response (rilis lama pra-Batch 156 tidak punya body).
                val releaseNotes = json.optString("body", "")
                val assets: JSONArray = json.optJSONArray("assets") ?: JSONArray()
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        return CheckResult.Success(
                            ReleaseInfo(
                                tagName = tagName,
                                htmlUrl = htmlUrl,
                                apkDownloadUrl = asset.optString("browser_download_url", ""),
                                apkAssetName = name,
                                releaseNotes = releaseNotes
                            )
                        )
                    }
                }
                CheckResult.NoApkAsset
            }
        } catch (e: IOException) {
            CheckResult.Failure(e.message ?: "Gagal koneksi atau timeout")
        } catch (e: Exception) {
            CheckResult.Failure(e.message ?: "Gagal membaca response GitHub")
        }
    }
}
