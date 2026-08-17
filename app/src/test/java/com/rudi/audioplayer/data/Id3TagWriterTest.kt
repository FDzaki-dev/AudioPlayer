package com.rudi.audioplayer.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gap List "Wajib" #1 (Tag/Metadata Editor) — pure binary-encoding logic only, no Context/
 *  ContentResolver needed (that's [TagEditor]'s job, untested here — same split as
 *  MusicRepositoryTrackDiscTest testing the pure parse helper while the cursor-reading part
 *  of MusicRepository itself stays untested at this layer). */
class Id3TagWriterTest {

    private fun readSyncsafe(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)

    @Test
    fun `readExistingTagSize returns 0 when file has no ID3v2 header`() {
        val header = "NOTAG12345".toByteArray(Charsets.US_ASCII) // 10 bytes, no "ID3" prefix
        assertEquals(0, Id3TagWriter.readExistingTagSize(header))
    }

    @Test
    fun `readExistingTagSize returns 0 for a too-short header`() {
        assertEquals(0, Id3TagWriter.readExistingTagSize(ByteArray(4)))
    }

    @Test
    fun `readExistingTagSize decodes a real ID3v2 header correctly`() {
        // "ID3" + version(2) + flags(1) + syncsafe size(4) = 500 bytes of frame content.
        val header = byteArrayOf(
            'I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(),
            0x03, 0x00, 0x00,
            0x00, 0x00, 0x03, 0x74 // syncsafe for 500: (3 shl 7) or 0x74 = 500
        )
        // +10 because readExistingTagSize returns the TOTAL bytes to skip (header included).
        assertEquals(510, Id3TagWriter.readExistingTagSize(header))
    }

    @Test
    fun `buildTag syncsafe size matches actual frame bytes length`() {
        val tag = Id3TagWriter.buildTag(
            Id3TagWriter.EditableTags(title = "Judul", artist = "Artis", album = "Album")
        )
        val declaredFramesSize = readSyncsafe(tag, 6)
        val actualFramesSize = tag.size - 10
        assertEquals(actualFramesSize, declaredFramesSize)
    }

    @Test
    fun `buildTag omits optional frames when null or blank`() {
        val tag = Id3TagWriter.buildTag(
            Id3TagWriter.EditableTags(title = "T", artist = "A", album = "Al", genre = null, composer = "  ")
        )
        val tagString = String(tag, Charsets.US_ASCII)
        assertTrue("TCON should be absent", !tagString.contains("TCON"))
        assertTrue("TCOM should be absent (blank)", !tagString.contains("TCOM"))
        assertTrue("TIT2 must be present", tagString.contains("TIT2"))
    }

    @Test
    fun `buildTag includes genre frame when present`() {
        val tag = Id3TagWriter.buildTag(
            Id3TagWriter.EditableTags(title = "T", artist = "A", album = "Al", genre = "Pop")
        )
        assertTrue(String(tag, Charsets.US_ASCII).contains("TCON"))
    }

    @Test
    fun `textFrame content round-trips as UTF-16LE with BOM`() {
        val frame = Id3TagWriter.textFrame("TIT2", "Hello")
        // Header: 4 bytes id + 4 bytes size (big-endian, NOT syncsafe for v2.3 frames) + 2 flags.
        val declaredContentSize = ((frame[4].toInt() and 0xFF) shl 24) or
            ((frame[5].toInt() and 0xFF) shl 16) or
            ((frame[6].toInt() and 0xFF) shl 8) or
            (frame[7].toInt() and 0xFF)
        assertEquals(frame.size - 10, declaredContentSize)
        // content = 1 encoding byte + 2 BOM bytes + UTF-16LE text
        val content = frame.copyOfRange(10, frame.size)
        assertEquals(0x01, content[0].toInt())
        assertEquals(0xFF.toByte(), content[1])
        assertEquals(0xFE.toByte(), content[2])
        val decoded = String(content, 3, content.size - 3, Charsets.UTF_16LE)
        assertEquals("Hello", decoded)
    }

    @Test
    fun `rewrite replaces an existing tag and preserves audio bytes untouched`() {
        val oldTag = byteArrayOf(
            'I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(),
            0x03, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x05 // syncsafe 5 -> 5 bytes of old frame content
        ) + ByteArray(5) { 0x11 } // fake old frame bytes, should be discarded entirely
        val audioBytes = byteArrayOf(0x11, 0x22, 0x33.toByte(), 0x44, 0x55) // fake "audio" data
        val original = oldTag + audioBytes

        val newTag = Id3TagWriter.buildTag(
            Id3TagWriter.EditableTags(title = "New", artist = "Artist", album = "Album")
        )
        val output = ByteArrayOutputStream()
        Id3TagWriter.rewrite(ByteArrayInputStream(original), output, newTag)
        val result = output.toByteArray()

        // New tag at the very start, audio bytes byte-for-byte right after it, old tag gone.
        assertArrayEquals(newTag, result.copyOfRange(0, newTag.size))
        assertArrayEquals(audioBytes, result.copyOfRange(newTag.size, result.size))
    }

    @Test
    fun `rewrite preserves the first bytes when file has no existing ID3v2 tag`() {
        // File that's ALL "audio" from byte 0 — no ID3v2 magic at all.
        val audioBytes = ByteArray(20) { it.toByte() }
        val newTag = Id3TagWriter.buildTag(
            Id3TagWriter.EditableTags(title = "T", artist = "A", album = "Al")
        )
        val output = ByteArrayOutputStream()
        Id3TagWriter.rewrite(ByteArrayInputStream(audioBytes), output, newTag)
        val result = output.toByteArray()

        assertArrayEquals(newTag, result.copyOfRange(0, newTag.size))
        // Every original audio byte must survive, including the first 10 that
        // readExistingTagSize had to peek at to determine there was no tag.
        assertArrayEquals(audioBytes, result.copyOfRange(newTag.size, result.size))
    }
}
