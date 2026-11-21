package com.rudi.audioplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastTabStoreTest {

    @Test
    fun `start dingin di home dengan tab tersimpan library maka pindah ke library`() {
        assertEquals("library", LastTabStore.restoreTarget(saved = "library", firstRoute = "home"))
    }

    @Test
    fun `start dingin di home dengan tab tersimpan settings maka pindah ke settings`() {
        assertEquals("settings", LastTabStore.restoreTarget(saved = "settings", firstRoute = "home"))
    }

    @Test
    fun `tab tersimpan home maka tidak perlu pindah`() {
        assertNull(LastTabStore.restoreTarget(saved = "home", firstRoute = "home"))
    }

    @Test
    fun `belum ada tab tersimpan maka tidak pindah`() {
        assertNull(LastTabStore.restoreTarget(saved = null, firstRoute = "home"))
    }

    @Test
    fun `navigasi sudah dipulihkan ke tab lain maka tidak ditimpa`() {
        assertNull(LastTabStore.restoreTarget(saved = "library", firstRoute = "library"))
        assertNull(LastTabStore.restoreTarget(saved = "library", firstRoute = "settings"))
    }

    @Test
    fun `rute tersimpan tidak valid diabaikan`() {
        assertNull(LastTabStore.restoreTarget(saved = "now_playing", firstRoute = "home"))
        assertNull(LastTabStore.restoreTarget(saved = "", firstRoute = "home"))
    }

    @Test
    fun `rute pertama belum teramati maka tidak pindah`() {
        assertNull(LastTabStore.restoreTarget(saved = "library", firstRoute = null))
    }

    @Test
    fun `selama pemulihan di home bilah menyorot tab tersimpan`() {
        assertEquals("library", LastTabStore.barRoute(settled = false, saved = "library", current = "home"))
        assertEquals("settings", LastTabStore.barRoute(settled = false, saved = "settings", current = null))
    }

    @Test
    fun `setelah pemulihan selesai bilah mengikuti rute sebenarnya`() {
        assertEquals("library", LastTabStore.barRoute(settled = true, saved = "library", current = "library"))
        assertEquals("home", LastTabStore.barRoute(settled = true, saved = "library", current = "home"))
    }

    @Test
    fun `navigasi sudah di tab lain maka bilah tidak ditimpa tab tersimpan`() {
        assertEquals("settings", LastTabStore.barRoute(settled = false, saved = "library", current = "settings"))
    }

    @Test
    fun `tanpa tab tersimpan atau tersimpan tidak valid bilah mengikuti rute sebenarnya`() {
        assertEquals("home", LastTabStore.barRoute(settled = false, saved = null, current = "home"))
        assertEquals("home", LastTabStore.barRoute(settled = false, saved = "now_playing", current = "home"))
        assertNull(LastTabStore.barRoute(settled = false, saved = null, current = null))
    }
}
