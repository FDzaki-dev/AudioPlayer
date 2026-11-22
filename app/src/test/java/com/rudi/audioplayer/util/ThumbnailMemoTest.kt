package com.rudi.audioplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailMemoTest {

    private val keyA = "content://media/external/audio/media/61407#200"
    private val keyB = "content://media/external/audio/media/307375#200"

    private fun memo() = ThumbnailMemo<String>(hitTtlMs = 1_000L, missTtlMs = 5_000L)

    @Test
    fun `memo kosong tak tahu apa apa`() {
        assertTrue(memo().recall(keyA, 0L) === ThumbnailMemo.Recall.Unknown)
    }

    @Test
    fun `sampul yang dimuat dipakai ulang selama masa berlaku`() {
        val memo = memo()
        val art = "bitmap-A"
        memo.rememberHit(keyA, art, 100L)
        val recalled = memo.recall(keyA, 600L)
        assertTrue(recalled is ThumbnailMemo.Recall.Hit)
        assertSame(art, (recalled as ThumbnailMemo.Recall.Hit).value)
    }

    @Test
    fun `sampul kedaluwarsa tepat di batas masa berlaku`() {
        val memo = memo()
        memo.rememberHit(keyA, "bitmap-A", 100L)
        assertTrue(memo.recall(keyA, 1_099L) is ThumbnailMemo.Recall.Hit)
        assertTrue(memo.recall(keyA, 1_100L) === ThumbnailMemo.Recall.Unknown)
    }

    @Test
    fun `tanpa sampul diingat lebih lama dari sampul`() {
        val memo = memo()
        memo.rememberMiss(keyA, 100L)
        assertTrue(memo.recall(keyA, 4_000L) === ThumbnailMemo.Recall.Miss)
        assertTrue(memo.recall(keyA, 5_099L) === ThumbnailMemo.Recall.Miss)
        assertTrue(memo.recall(keyA, 5_100L) === ThumbnailMemo.Recall.Unknown)
    }

    @Test
    fun `kunci lain tak tahu dan menyimpan kunci baru membuang yang lama`() {
        val memo = memo()
        memo.rememberMiss(keyA, 0L)
        assertTrue(memo.recall(keyB, 10L) === ThumbnailMemo.Recall.Unknown)
        memo.rememberHit(keyB, "bitmap-B", 20L)
        assertTrue(memo.recall(keyA, 30L) === ThumbnailMemo.Recall.Unknown)
        assertTrue(memo.recall(keyB, 30L) is ThumbnailMemo.Recall.Hit)
    }

    @Test
    fun `ukuran berbeda untuk lagu yang sama adalah kunci berbeda`() {
        val memo = memo()
        memo.rememberHit("content://media/external/audio/media/61407#200", "kecil", 0L)
        assertTrue(memo.recall("content://media/external/audio/media/61407#480", 10L) === ThumbnailMemo.Recall.Unknown)
    }

    @Test
    fun `hasil terbaru untuk kunci yang sama menimpa yang lama`() {
        val memo = memo()
        memo.rememberMiss(keyA, 0L)
        memo.rememberHit(keyA, "bitmap-A", 10L)
        val recalled = memo.recall(keyA, 20L)
        assertEquals("bitmap-A", (recalled as ThumbnailMemo.Recall.Hit).value)

        memo.rememberMiss(keyA, 30L)
        assertTrue(memo.recall(keyA, 40L) === ThumbnailMemo.Recall.Miss)
    }

    @Test
    fun `jam mundur membuat catatan tak dipercaya`() {
        val memo = memo()
        memo.rememberHit(keyA, "bitmap-A", 500L)
        assertTrue(memo.recall(keyA, 499L) === ThumbnailMemo.Recall.Unknown)
    }

    @Test
    fun `nilai bawaan masa berlaku`() {
        assertEquals(120_000L, ThumbnailMemo.HIT_TTL_MS)
        assertEquals(600_000L, ThumbnailMemo.MISS_TTL_MS)
        val memo = ThumbnailMemo<String>()
        memo.rememberHit(keyA, "bitmap-A", 0L)
        assertTrue(memo.recall(keyA, 119_999L) is ThumbnailMemo.Recall.Hit)
        assertTrue(memo.recall(keyA, 120_000L) === ThumbnailMemo.Recall.Unknown)
    }
}
