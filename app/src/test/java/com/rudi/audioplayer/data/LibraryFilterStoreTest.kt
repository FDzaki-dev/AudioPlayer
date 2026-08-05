package com.rudi.audioplayer.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFilterStoreTest {

    @Test
    fun `song with no exclusions applied is kept`() {
        assertTrue(
            LibraryFilterStore.shouldKeep(
                folderPath = "Music",
                id = 1L,
                excludedFolders = emptySet(),
                hiddenIds = emptySet()
            )
        )
    }

    @Test
    fun `song in an excluded folder is dropped even if not individually hidden`() {
        assertFalse(
            LibraryFilterStore.shouldKeep(
                folderPath = "WhatsApp Audio",
                id = 1L,
                excludedFolders = setOf("WhatsApp Audio"),
                hiddenIds = emptySet()
            )
        )
    }

    @Test
    fun `individually hidden song is dropped even in a non-excluded folder`() {
        assertFalse(
            LibraryFilterStore.shouldKeep(
                folderPath = "Music",
                id = 42L,
                excludedFolders = emptySet(),
                hiddenIds = setOf(42L)
            )
        )
    }

    @Test
    fun `song excluded by both folder and hidden-id rules is still just dropped once`() {
        assertFalse(
            LibraryFilterStore.shouldKeep(
                folderPath = "WhatsApp Audio",
                id = 42L,
                excludedFolders = setOf("WhatsApp Audio"),
                hiddenIds = setOf(42L)
            )
        )
    }

    @Test
    fun `song in a different folder with a different hidden id is kept`() {
        assertTrue(
            LibraryFilterStore.shouldKeep(
                folderPath = "Music",
                id = 1L,
                excludedFolders = setOf("WhatsApp Audio"),
                hiddenIds = setOf(42L)
            )
        )
    }
}
