package com.rudi.audioplayer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicRepositoryFolderNameTest {

    // --- API 29+ (RELATIVE_PATH column, useRelativePath = true) ---

    @Test
    fun `relative path with trailing slash returns the last segment`() {
        assertEquals("Music", MusicRepository.deriveFolderName("Music/", useRelativePath = true))
    }

    @Test
    fun `nested relative path returns only the deepest folder`() {
        assertEquals(
            "WhatsApp Audio",
            MusicRepository.deriveFolderName("Music/WhatsApp Audio/", useRelativePath = true)
        )
    }

    @Test
    fun `relative path without trailing slash still resolves`() {
        assertEquals("Music", MusicRepository.deriveFolderName("Music", useRelativePath = true))
    }

    @Test
    fun `blank relative path falls back to Musik`() {
        assertEquals("Musik", MusicRepository.deriveFolderName("", useRelativePath = true))
    }

    @Test
    fun `relative path that is only a slash falls back to Musik`() {
        assertEquals("Musik", MusicRepository.deriveFolderName("/", useRelativePath = true))
    }

    // --- Pre-API 29 (DATA column, useRelativePath = false) ---

    @Test
    fun `absolute file path returns its parent folder name`() {
        assertEquals(
            "Music",
            MusicRepository.deriveFolderName("/storage/emulated/0/Music/song.mp3", useRelativePath = false)
        )
    }

    @Test
    fun `file directly under storage root returns the root folder name`() {
        assertEquals(
            "0",
            MusicRepository.deriveFolderName("/storage/emulated/0/song.mp3", useRelativePath = false)
        )
    }

    @Test
    fun `relative filename with no parent falls back to Musik`() {
        assertEquals("Musik", MusicRepository.deriveFolderName("song.mp3", useRelativePath = false))
    }

    @Test
    fun `blank absolute path falls back to Musik`() {
        assertEquals("Musik", MusicRepository.deriveFolderName("", useRelativePath = false))
    }
}
