package com.rudi.audioplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Batch 717 — tes JVM murni untuk urutan Shuffle: jangkar lagu aktif, antrean baru tak identitas, "Putar Berikutnya". */
class AntiRepeatShuffleOrderTest {

    private fun traverse(order: AntiRepeatShuffleOrder): List<Int> {
        val out = ArrayList<Int>()
        var i = order.getFirstIndex()
        while (i != -1) {
            out.add(i)
            i = order.getNextIndex(i)
        }
        return out
    }

    private fun orderOf(vararg values: Int, current: () -> Int = { -1 }, onReplaced: () -> Unit = {}) =
        AntiRepeatShuffleOrder(values, current, onReplaced)

    @Test
    fun `jangkar dipaku di posisi pertama dan segar mendahului baru diputar`() {
        val ids = (0 until 20).map { it.toLong() }
        val recent = setOf(1L, 2L, 3L, 7L)
        for (seed in 0 until 50) {
            val order = buildAntiRepeatNearbyOrder(ids, recent, anchorIndex = 7, random = Random(seed))
            assertEquals(7, order[0])
            assertEquals((0 until 20).toList(), order.sorted())
            val rest = order.drop(1)
            val firstRecent = rest.indexOfFirst { ids[it] in recent }
            assertTrue(rest.drop(firstRecent).all { ids[it] in recent })
        }
    }

    @Test
    fun `tanpa jangkar tetap permutasi penuh seperti sebelumnya`() {
        val ids = (0 until 12).map { it.toLong() }
        for (seed in 0 until 30) {
            val order = buildAntiRepeatNearbyOrder(ids, setOf(4L, 5L), random = Random(seed))
            assertEquals((0 until 12).toList(), order.sorted())
        }
    }

    @Test
    fun `jangkar di luar rentang diabaikan`() {
        val ids = (0 until 6).map { it.toLong() }
        val order = buildAntiRepeatNearbyOrder(ids, emptySet(), anchorIndex = 99, random = Random(1))
        assertEquals((0 until 6).toList(), order.sorted())
    }

    @Test
    fun `penjaga duplikat tidak menggeser jangkar`() {
        val ids = listOf(5L, 5L, 5L, 6L, 7L, 5L, 8L)
        for (seed in 0 until 60) {
            val order = buildAntiRepeatNearbyOrder(ids, emptySet(), anchorIndex = 0, random = Random(seed))
            assertEquals(0, order[0])
            assertEquals(ids.indices.toList(), order.sorted())
        }
    }

    @Test
    fun `antrean satu lagu dan kosong aman`() {
        assertEquals(listOf(0), buildAntiRepeatNearbyOrder(listOf(9L), emptySet(), anchorIndex = 0).toList())
        assertEquals(0, buildAntiRepeatNearbyOrder(emptyList(), emptySet(), anchorIndex = 0).size)
    }

    @Test
    fun `antrean baru ke urutan kosong diacak bukan identitas dan memicu hook sekali`() {
        var replaced = 0
        val empty = orderOf(onReplaced = { replaced++ })
        val filled = empty.cloneAndInsert(0, 30) as AntiRepeatShuffleOrder
        val visited = traverse(filled)
        assertEquals(1, replaced)
        assertEquals((0 until 30).toList(), visited.sorted())
        assertNotEquals((0 until 30).toList(), visited)

        filled.cloneAndInsert(30, 1)
        assertEquals(1, replaced)
    }

    @Test
    fun `mengganti antrean lewat hapus semua lalu sisip memicu hook`() {
        var replaced = 0
        val old = orderOf(2, 0, 3, 1, 4, onReplaced = { replaced++ })
        val cleared = old.cloneAndRemove(0, 5)
        assertEquals(0, cleared.getLength())
        assertEquals(0, replaced)
        val replacedOrder = cleared.cloneAndInsert(0, 25) as AntiRepeatShuffleOrder
        assertEquals(1, replaced)
        assertNotEquals((0 until 25).toList(), traverse(replacedOrder))
    }

    @Test
    fun `cloneAndClear lalu sisip juga dianggap antrean baru`() {
        var replaced = 0
        val cleared = orderOf(1, 0, onReplaced = { replaced++ }).cloneAndClear()
        cleared.cloneAndInsert(0, 3)
        assertEquals(1, replaced)
    }

    @Test
    fun `Putar Berikutnya disisip tepat setelah lagu aktif di urutan Shuffle`() {
        val order = orderOf(2, 0, 3, 1, current = { 0 })
        val inserted = order.cloneAndInsert(1, 1) as AntiRepeatShuffleOrder
        assertEquals(listOf(3, 0, 1, 4, 2), traverse(inserted))
        assertEquals(1, inserted.getNextIndex(0))
    }

    @Test
    fun `Putar Berikutnya beberapa lagu tetap berurutan setelah lagu aktif`() {
        val order = orderOf(2, 0, 3, 1, current = { 3 })
        val inserted = order.cloneAndInsert(4, 2) as AntiRepeatShuffleOrder
        // indeks aktif 3, sisip di 4 = ujung antrean (size 4): ekor, bukan "setelah aktif".
        assertEquals(listOf(2, 0, 3, 1, 4, 5), traverse(inserted))

        val mid = orderOf(2, 0, 3, 1, current = { 1 }).cloneAndInsert(2, 2) as AntiRepeatShuffleOrder
        // indeks aktif 1 (posisi 3 di urutan), sisip di 2: nilai >=2 bergeser +2 -> [4,0,5,1], lalu 2,3 tepat setelah indeks 1.
        assertEquals(listOf(4, 0, 5, 1, 2, 3), traverse(mid))
    }

    @Test
    fun `penambahan di ujung antrean tetap masuk ekor walau lagu aktif indeks terakhir`() {
        val order = orderOf(2, 0, 3, 1, current = { 3 })
        assertEquals(listOf(2, 0, 3, 1, 4), traverse(order.cloneAndInsert(4, 1) as AntiRepeatShuffleOrder))
    }

    @Test
    fun `sisip bukan di indeks aktif plus satu masuk ekor`() {
        val order = orderOf(2, 0, 3, 1, current = { 0 })
        assertEquals(listOf(2, 0, 4, 1, 3), traverse(order.cloneAndInsert(3, 1) as AntiRepeatShuffleOrder))
    }

    @Test
    fun `provider yang melempar exception jatuh ke ekor tanpa crash`() {
        val order = orderOf(2, 0, 3, 1, current = { throw IllegalStateException("thread salah") })
        assertEquals(listOf(3, 0, 4, 2, 1), traverse(order.cloneAndInsert(1, 1) as AntiRepeatShuffleOrder))
    }

    @Test
    fun `hapus mempertahankan urutan relatif dan menggeser indeks`() {
        val removed = orderOf(2, 0, 3, 1).cloneAndRemove(1, 3) as AntiRepeatShuffleOrder
        assertEquals(listOf(0, 1), traverse(removed))
    }

    @Test
    fun `next dan previous di luar rentang atau ujung bernilai tak ada`() {
        val order = orderOf(1, 0)
        assertEquals(-1, order.getNextIndex(0))
        assertEquals(-1, order.getPreviousIndex(1))
        assertEquals(-1, order.getNextIndex(5))
        assertEquals(-1, order.getPreviousIndex(-3))
        assertEquals(0, order.getLastIndex())
        assertEquals(1, order.getFirstIndex())
    }
}
