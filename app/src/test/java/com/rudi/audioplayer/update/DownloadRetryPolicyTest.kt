package com.rudi.audioplayer.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.ServerSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class DownloadRetryPolicyTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // Pesan NYATA dari log v707 (`E/UpdateManager: Unduhan gagal: ...`, 18:03:10).
    private val dnsMessage = "Unable to resolve host \"github.com\": No address associated with hostname"

    @Test
    fun `galat DNS koneksi dan timeout dianggap sementara`() {
        assertTrue(DownloadRetryPolicy.isTransient(UnknownHostException(dnsMessage)))
        assertTrue(DownloadRetryPolicy.isTransient(ConnectException("Connection refused")))
        assertTrue(DownloadRetryPolicy.isTransient(NoRouteToHostException("No route to host")))
        assertTrue(DownloadRetryPolicy.isTransient(SocketException("Connection reset")))
        assertTrue(DownloadRetryPolicy.isTransient(SocketTimeoutException("timeout")))
    }

    @Test
    fun `galat lain tidak diulang`() {
        assertFalse(DownloadRetryPolicy.isTransient(SSLHandshakeException("Trust anchor for certification path not found")))
        assertFalse(DownloadRetryPolicy.isTransient(FileNotFoundException("/data/cache/update.apk (No space left on device)")))
        assertFalse(DownloadRetryPolicy.isTransient(IOException("unexpected end of stream")))
        assertFalse(DownloadRetryPolicy.isTransient(IllegalStateException("closed")))
    }

    @Test
    fun `jeda bertambah per percobaan dan tidak pernah di bawah dasar`() {
        assertEquals(1_500L, DownloadRetryPolicy.retryDelayMs(1))
        assertEquals(3_000L, DownloadRetryPolicy.retryDelayMs(2))
        assertEquals(4_500L, DownloadRetryPolicy.retryDelayMs(3))
        assertEquals(1_500L, DownloadRetryPolicy.retryDelayMs(0))
        assertEquals(1_500L, DownloadRetryPolicy.retryDelayMs(-4))
    }

    @Test
    fun `berhasil pada percobaan pertama tanpa jeda dan tanpa onRetry`() {
        val pauses = mutableListOf<Long>()
        var retries = 0
        var calls = 0
        val outcome = DownloadRetryPolicy.execute<String>(
            shouldRetry = { it == "gagal" },
            pause = { pauses += it },
            onRetry = { _, _, _ -> retries++ },
            attempt = { calls++; "ok" }
        )
        assertEquals("ok", outcome.result)
        assertEquals(1, outcome.attempts)
        assertEquals(1, calls)
        assertEquals(0, retries)
        assertTrue(pauses.isEmpty())
    }

    @Test
    fun `gagal dua kali lalu berhasil seperti log v707`() {
        val results = listOf("gagal-1", "gagal-2", "ok").iterator()
        val pauses = mutableListOf<Long>()
        val retries = mutableListOf<Triple<Int, Long, String>>()
        val outcome = DownloadRetryPolicy.execute<String>(
            shouldRetry = { it.startsWith("gagal") },
            pause = { pauses += it },
            onRetry = { n, delay, failed -> retries += Triple(n, delay, failed) },
            attempt = { results.next() }
        )
        assertEquals("ok", outcome.result)
        assertEquals(3, outcome.attempts)
        assertEquals(listOf(1_500L, 3_000L), pauses)
        assertEquals(listOf(Triple(1, 1_500L, "gagal-1"), Triple(2, 3_000L, "gagal-2")), retries)
    }

    @Test
    fun `berhenti di batas percobaan dan mengembalikan hasil gagal terakhir`() {
        var calls = 0
        val pauses = mutableListOf<Long>()
        val outcome = DownloadRetryPolicy.execute<String>(
            shouldRetry = { true },
            pause = { pauses += it },
            onRetry = { _, _, _ -> },
            attempt = { "gagal-${++calls}" }
        )
        assertEquals("gagal-3", outcome.result)
        assertEquals(DownloadRetryPolicy.MAX_ATTEMPTS, outcome.attempts)
        assertEquals(DownloadRetryPolicy.MAX_ATTEMPTS, calls)
        assertEquals(DownloadRetryPolicy.MAX_ATTEMPTS - 1, pauses.size)
    }

    @Test
    fun `hasil yang tak layak diulang dikembalikan langsung`() {
        var calls = 0
        val pauses = mutableListOf<Long>()
        val outcome = DownloadRetryPolicy.execute<String>(
            shouldRetry = { false },
            pause = { pauses += it },
            onRetry = { _, _, _ -> },
            attempt = { calls++; "HTTP 404" }
        )
        assertEquals("HTTP 404", outcome.result)
        assertEquals(1, outcome.attempts)
        assertEquals(1, calls)
        assertTrue(pauses.isEmpty())
    }

    @Test
    fun `jeda yang diinterupsi menghentikan pengulangan dan memulihkan flag interrupt`() {
        var calls = 0
        try {
            val outcome = DownloadRetryPolicy.execute<String>(
                shouldRetry = { true },
                pause = { throw InterruptedException("dibatalkan") },
                onRetry = { _, _, _ -> },
                attempt = { "gagal-${++calls}" }
            )
            assertEquals("gagal-1", outcome.result)
            assertEquals(1, outcome.attempts)
            assertEquals(1, calls)
            assertTrue(Thread.currentThread().isInterrupted)
        } finally {
            Thread.interrupted() // bersihkan flag supaya tidak bocor ke tes lain
        }
    }

    @Test
    fun `unduhan ke port tertutup diulang tiga kali lalu gagal dengan penanda retryable`() {
        val port = ServerSocket(0).use { it.localPort } // lepas segera: port tertutup -> ConnectException seketika
        val pauses = mutableListOf<Long>()
        val reasons = mutableListOf<String>()
        val result = UpdateDownloader.download(
            url = "http://127.0.0.1:$port/update.apk",
            destFile = tmp.newFile("update_test.apk"),
            onRetry = { _, max, _, reason -> assertEquals(DownloadRetryPolicy.MAX_ATTEMPTS, max); reasons += reason },
            pause = { pauses += it }
        )
        assertTrue(result is UpdateDownloader.DownloadResult.Failure)
        val failure = result as UpdateDownloader.DownloadResult.Failure
        assertTrue(failure.retryable)
        assertEquals(DownloadRetryPolicy.MAX_ATTEMPTS, failure.attempts)
        assertEquals(listOf(1_500L, 3_000L), pauses)
        assertEquals(2, reasons.size)
    }
}
