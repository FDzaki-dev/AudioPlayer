package com.rudi.audioplayer.update

import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Release Downloader Spec — downloads a GitHub Release binary (the signed APK) straight to
 * disk in fixed 8 KB chunks. Deliberately never calls `ResponseBody.bytes()` / `.string()` on
 * the binary body: those buffer the ENTIRE file in RAM before a single byte reaches disk,
 * which risks OOM on low-RAM devices for a multi-MB APK and freezes the UI thread if ever
 * called off a background thread by mistake. Every call here is blocking I/O — callers must
 * invoke this from a background thread (see UpdateManager).
 */
object UpdateDownloader {

    private const val CHUNK_SIZE = 8L * 1024L // 8 KB per read — small & constant, never grows.

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        // GitHub Release assets are served via a 302 redirect to S3/Azure/Fastly CDN — without
        // this the download would fail outright on the redirect instead of following it.
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    sealed class DownloadResult {
        data class Success(val file: File) : DownloadResult()
        data class Failure(val message: String) : DownloadResult()
    }

    /**
     * @param githubToken optional — only needed for private repos / to raise the anonymous
     *        rate limit. Sent as "Authorization: Bearer <token>" per spec, never logged.
     * @param onProgress called after every chunk with (bytesDownloaded, totalBytes).
     *        totalBytes is -1 if the server response omits Content-Length.
     */
    fun download(
        url: String,
        destFile: File,
        githubToken: String? = null,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): DownloadResult {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "application/octet-stream")
        if (!githubToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $githubToken")
        }

        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return DownloadResult.Failure("HTTP ${response.code}")
                }
                val body = response.body ?: return DownloadResult.Failure("Response tanpa body")
                val totalBytes = body.contentLength()

                destFile.parentFile?.mkdirs()
                if (destFile.exists()) destFile.delete()

                var bytesDownloaded = 0L
                body.source().use { source ->
                    destFile.sink().buffer().use { sink ->
                        val buffer = Buffer()
                        while (true) {
                            // Reads at most CHUNK_SIZE bytes into a small in-memory Buffer, then
                            // immediately writes+flushes that chunk to the sink below — never
                            // accumulates the whole response in memory.
                            val read = source.read(buffer, CHUNK_SIZE)
                            if (read == -1L) break
                            sink.write(buffer, read)
                            bytesDownloaded += read
                            onProgress(bytesDownloaded, totalBytes)
                        }
                        sink.flush()
                    }
                }
                DownloadResult.Success(destFile)
            }
        } catch (e: IOException) {
            // Covers connect/read timeout (15s/20s above) and any dropped-connection case.
            destFile.delete()
            DownloadResult.Failure(e.message ?: "Gagal koneksi atau timeout")
        } catch (e: Exception) {
            destFile.delete()
            DownloadResult.Failure(e.message ?: "Gagal tidak terduga saat unduh")
        }
    }
}
