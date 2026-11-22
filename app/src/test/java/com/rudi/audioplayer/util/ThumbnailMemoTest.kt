package com.rudi.audioplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailMemoTest {

    private val keyA = "content://media/external/audio/media/61407#200"
    private val keyB = "content://media/external/audio/media/307375#200"
    private val keyC = "content://media/external/audio/media/282279#200"

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
    fun `kunci yang belum pernah dimuat tak diketahui`() {
        val memo = memo()
        memo.rememberMiss(keyA, 0L)
        memo.rememberHit(keyB, "bitmap-B", 5L)
        assertTrue(memo.recall(keyC, 10L) === ThumbnailMemo.Recall.Unknown)
    }

    @Test
    fun `pola A ke B ke A - sampul B tak menimpa catatan tanpa sampul A`() {
        val memo = memo()
        memo.rememberMiss(keyA, 0L)
        memo.rememberHit(keyB, "bitmap-B", 10L)
        assertTrue(memo.recall(keyA, 20L) === ThumbnailMemo.Recall.Miss)
        assertTrue(memo.recall(keyB, 20L) is ThumbnailMemo.Recall.Hit)
    }

    @Test
    fun `hanya satu slot sampul - sampul baru membuang sampul lama`() {
        val memo = memo()
        memo.rememberHit(keyA, "bitmap-A", 0L)
        memo.rememberHit(keyB, "bitmap-B", 10L)
        assertTrue(memo.recall(keyA, 20L) === ThumbnailMemo.Recall.Unknown)
        assertTrue(memo.recall(keyB, 20L) is ThumbnailMemo.Recall.Hit)
    }

    @Test
    fun `ukuran berbeda untuk lagu yang sama adalah kunci berbeda`() {
        val memo = memo()
        memo.rememberMiss("content://media/external/audio/media/61407#200", 0L)
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
        memo.rememberMiss(keyB, 500L)
        assertTrue(memo.recall(keyA, 499L) === ThumbnailMemo.Recall.Unknown)
        assertTrue(memo.recall(keyB, 499L) === ThumbnailMemo.Recall.Unknown)
    }

    @Test
    fun `himpunan tanpa sampul dipangkas dari entri terlama`() {
        val memo = ThumbnailMemo<String>(hitTtlMs = 1_000L, missTtlMs = 5_000L, maxMisses = 2)
        memo.rememberMiss("k1", 0L)
        memo.rememberMiss("k2", 1L)
        memo.rememberMiss("k3", 2L)
        assertTrue(memo.recall("k1", 3L) === ThumbnailMemo.Recall.Unknown)
        assertTrue(memo.recall("k2", 3L) === ThumbnailMemo.Recall.Miss)
        assertTrue(memo.recall("k3", 3L) === ThumbnailMemo.Recall.Miss)
    }

    @Test
    fun `mengingat ulang kunci menyegarkan urutan sehingga bukan lagi yang terlama`() {
        val memo = ThumbnailMemo<String>(hitTtlMs = 1_000L, missTtlMs = 5_000L, maxMisses = 2)
        memo.rememberMiss("k1", 0L)
        memo.rememberMiss("k2", 1L)
        memo.rememberMiss("k1", 2L)
        memo.rememberMiss("k3", 3L)
        assertTrue(memo.recall("k2", 4L) === ThumbnailMemo.Recall.Unknown)
        assertTrue(memo.recall("k1", 4L) === ThumbnailMemo.Recall.Miss)
        assertTrue(memo.recall("k3", 4L) === ThumbnailMemo.Recall.Miss)
    }

    @Test
    fun `nilai bawaan masa berlaku dan batas`() {
        assertEquals(120_000L, ThumbnailMemo.HIT_TTL_MS)
        assertEquals(600_000L, ThumbnailMemo.MISS_TTL_MS)
        assertEquals(64, ThumbnailMemo.MAX_MISSES)
        val memo = ThumbnailMemo<String>()
        memo.rememberHit(keyA, "bitmap-A", 0L)
        assertTrue(memo.recall(keyA, 119_999L) is ThumbnailMemo.Recall.Hit)
        assertTrue(memo.recall(keyA, 120_000L) === ThumbnailMemo.Recall.Unknown)
    }
}
