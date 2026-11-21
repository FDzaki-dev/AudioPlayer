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
        /**
         * [message] = pesan mentah galat. [retryable] = galat jaringan yang layak diulang ([DownloadRetryPolicy.isTransient]).
         * [attempts] = jumlah percobaan yang dijalankan sampai gagal (Batch 708).
         */
        data class Failure(val message: String, val retryable: Boolean = false, val attempts: Int = 1) : DownloadResult()
    }

    /**
     * @param githubToken optional — only needed for private repos / to raise the anonymous
     *        rate limit. Sent as "Authorization: Bearer <token>" per spec, never logged.
     * @param onProgress called after every chunk with (bytesDownloaded, totalBytes).
     *        totalBytes is -1 if the server response omits Content-Length.
     * @param onRetry Batch 708 — dipanggil sebelum jeda pengulangan akibat galat jaringan sementara
     *        ([DownloadRetryPolicy]): (percobaan yang gagal, total percobaan, lama jeda ms, pesan galat mentah).
     * @param pause Batch 708 — jeda antar percobaan (blocking); default `Thread.sleep`, diganti di tes.
     */
    fun download(
        url: String,
        destFile: File,
        githubToken: String? = null,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _ -> },
        onRetry: (failedAttempt: Int, maxAttempts: Int, delayMs: Long, reason: String) -> Unit = { _, _, _, _ -> },
        pause: (Long) -> Unit = { Thread.sleep(it) }
    ): DownloadResult {
        val outcome = DownloadRetryPolicy.execute<DownloadResult>(
            shouldRetry = { it is DownloadResult.Failure && it.retryable },
            pause = pause,
            onRetry = { failedAttempt, delayMs, failed ->
                val reason = (failed as? DownloadResult.Failure)?.message.orEmpty()
                onRetry(failedAttempt, DownloadRetryPolicy.MAX_ATTEMPTS, delayMs, reason)
            },
            attempt = { downloadOnce(url, destFile, githubToken, onProgress) }
        )
        val last = outcome.result
        return if (last is DownloadResult.Failure) last.copy(attempts = outcome.attempts) else last
    }

    /** Satu percobaan unduh (isi `download` sebelum Batch 708). Galat IO ditandai [DownloadResult.Failure.retryable]. */
    private fun downloadOnce(
        url: String,
        destFile: File,
        githubToken: String?,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
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
                // Batch 706 — unduhan 0 byte / terpotong tak boleh dilaporkan Success (log v705: berkas 0 byte tetap jadi
                // `ReadyToInstall`). Dicek SETELAH sink ditutup supaya yang dihitung = yang benar-benar tertulis.
                val problem = UpdateApkCache.downloadProblem(bytesDownloaded, totalBytes)
                if (problem != null) {
                    destFile.delete()
                    return DownloadResult.Failure("Unduhan ${destFile.name} gagal: $problem (HTTP ${response.code})")
                }
                DownloadResult.Success(destFile)
            }
        } catch (e: IOException) {
            // Covers connect/read timeout (15s/20s above) and any dropped-connection case.
            // Batch 708 — DNS/koneksi/timeout ditandai retryable supaya `download` mengulang (log v707: DNS gagal 18:03:10, pulih ±5 dtk).
            destFile.delete()
            DownloadResult.Failure(
                e.message ?: "Gagal koneksi atau timeout (${e.javaClass.simpleName})",
                retryable = DownloadRetryPolicy.isTransient(e)
            )
        } catch (e: Exception) {
            destFile.delete()
            DownloadResult.Failure(e.message ?: "Gagal tidak terduga saat unduh")
        }
    }
}
