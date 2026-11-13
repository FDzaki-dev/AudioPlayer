package com.rudi.audioplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastTabStoreTest {

    @Test
    fun `start dingin di home dengan tab tersimpan library -> pindah ke library`() {
        assertEquals("library", LastTabStore.restoreTarget(saved = "library", firstRoute = "home"))
    }

    @Test
    fun `start dingin di home dengan tab tersimpan settings -> pindah ke settings`() {
        assertEquals("settings", LastTabStore.restoreTarget(saved = "settings", firstRoute = "home"))
    }

    @Test
    fun `tab tersimpan home -> tidak perlu pindah`() {
        assertNull(LastTabStore.restoreTarget(saved = "home", firstRoute = "home"))
    }

    @Test
    fun `belum ada tab tersimpan -> tidak pindah`() {
        assertNull(LastTabStore.restoreTarget(saved = null, firstRoute = "home"))
    }

    @Test
    fun `navigasi sudah dipulihkan ke tab lain -> tidak ditimpa`() {
        assertNull(LastTabStore.restoreTarget(saved = "library", firstRoute = "library"))
        assertNull(LastTabStore.restoreTarget(saved = "library", firstRoute = "settings"))
    }

    @Test
    fun `rute tersimpan tidak valid diabaikan`() {
        assertNull(LastTabStore.restoreTarget(saved = "now_playing", firstRoute = "home"))
        assertNull(LastTabStore.restoreTarget(saved = "", firstRoute = "home"))
    }

    @Test
    fun `rute pertama belum teramati -> tidak pindah`() {
        assertNull(LastTabStore.restoreTarget(saved = "library", firstRoute = null))
    }
}
