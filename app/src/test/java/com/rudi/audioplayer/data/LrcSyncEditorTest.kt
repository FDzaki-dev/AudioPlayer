package com.rudi.audioplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcSyncEditorTest {

    @Test
    fun `startSession splits non-blank trimmed lines, all timestamps null`() {
        val session = LrcSyncEditor.startSession("  Baris satu  \n\n   \nBaris dua")

        assertEquals(listOf("Baris satu", "Baris dua"), session.lines)
        assertEquals(listOf(null, null), session.timestamps)
        assertEquals(0, session.currentIndex)
    }

    @Test
    fun `mark stamps current line and advances index`() {
        val session = LrcSyncEditor.startSession("A\nB")
        val marked = LrcSyncEditor.mark(session, 1_500L)

        assertEquals(listOf(1_500L, null), marked.timestamps)
        assertEquals(1, marked.currentIndex)
    }

    @Test
    fun `mark on completed session is no-op`() {
        var session = LrcSyncEditor.startSession("A")
        session = LrcSyncEditor.mark(session, 100L)
        assertTrue(session.isComplete)

        val again = LrcSyncEditor.mark(session, 999L)
        assertEquals(session, again)
    }

    @Test
    fun `skip advances index without stamping`() {
        val session = LrcSyncEditor.startSession("A\nB")
        val skipped = LrcSyncEditor.skip(session)

        assertEquals(listOf(null, null), skipped.timestamps)
        assertEquals(1, skipped.currentIndex)
    }

    @Test
    fun `undo after mark clears timestamp and steps back`() {
        var session = LrcSyncEditor.startSession("A\nB")
        session = LrcSyncEditor.mark(session, 1_000L)
        session = LrcSyncEditor.undo(session)

        assertEquals(listOf(null, null), session.timestamps)
        assertEquals(0, session.currentIndex)
    }

    @Test
    fun `undo at index 0 is no-op`() {
        val session = LrcSyncEditor.startSession("A\nB")
        val again = LrcSyncEditor.undo(session)

        assertEquals(session, again)
    }

    @Test
    fun `undo after skip also steps back cleanly (no stale timestamp)`() {
        var session = LrcSyncEditor.startSession("A\nB")
        session = LrcSyncEditor.skip(session)
        session = LrcSyncEditor.undo(session)

        assertEquals(0, session.currentIndex)
        assertEquals(listOf(null, null), session.timestamps)
    }

    @Test
    fun `formatTimestamp formats mm-ss-cc with zero padding`() {
        assertEquals("[00:00.00]", LrcSyncEditor.formatTimestamp(0L))
        assertEquals("[00:01.50]", LrcSyncEditor.formatTimestamp(1_500L))
        assertEquals("[01:02.03]", LrcSyncEditor.formatTimestamp(62_030L))
        assertEquals("[10:00.00]", LrcSyncEditor.formatTimestamp(600_000L))
    }

    @Test
    fun `formatTimestamp clamps negative to zero`() {
        assertEquals("[00:00.00]", LrcSyncEditor.formatTimestamp(-500L))
    }

    @Test
    fun `buildLrcText mixes stamped and skipped lines, round-trips through LyricsParser`() {
        var session = LrcSyncEditor.startSession("Intro\nReff\nOutro")
        session = LrcSyncEditor.mark(session, 0L)       // Intro -> [00:00.00]
        session = LrcSyncEditor.skip(session)             // Reff -> tetap plain
        session = LrcSyncEditor.mark(session, 45_250L)    // Outro -> [00:45.25]

        val text = LrcSyncEditor.buildLrcText(session)
        assertEquals("[00:00.00]Intro\nReff\n[00:45.25]Outro", text)

        val parsed = LyricsParser.parse(text)
        assertEquals(listOf(0L, null, 45_250L), parsed.map { it.timeMs })
        assertEquals(listOf("Intro", "Reff", "Outro"), parsed.map { it.text })
        // Sengaja TIDAK fully synced (1 baris skip) — LyricsParser.isSynced() harus false di sini,
        // bukan bug: skip() disengaja mempertahankan baris sbg plain-text, bukan dipaksa dapat timestamp.
        assertEquals(false, LyricsParser.isSynced(parsed))
    }
}
