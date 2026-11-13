package com.rudi.audioplayer.data

import android.content.Context

/**
 * Batch 651 — mengingat tab bawah (Beranda / Perpustakaan / Pengaturan) terakhir yang dibuka user DI DISK.
 *
 * Kenapa perlu: Batch 647 hanya menyelamatkan state navigasi selama proses app masih hidup (kunci -> unlock).
 * Log device menunjukkan proses app sering dimatikan OEM saat di latar belakang (`ApplicationExitInfo`
 * `reclaim_process_start`, importance CACHED) lalu dimulai ulang saat user kembali — state di memori hilang dan
 * NavController mulai dari `startDestination = "home"`. Nilai di sini selamat dari kematian proses.
 *
 * Hanya menyimpan 1 string rute tab; nilai di luar daftar tab diabaikan (null).
 */
class LastTabStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLastTab(): String? = prefs.getString(KEY_LAST_TAB, null)?.takeIf { it in TABS }

    fun setLastTab(route: String) {
        if (route in TABS) prefs.edit().putString(KEY_LAST_TAB, route).apply()
    }

    companion object {
        /** Rute awal NavHost (selalu `home`); pemulihan hanya dilakukan bila app baru mulai di sini. */
        const val START_TAB = "home"

        private const val PREFS_NAME = "last_tab"
        private const val KEY_LAST_TAB = "route"
        private val TABS = setOf(START_TAB, "library", "settings")

        /**
         * Rute tujuan pemulihan, atau null bila tidak perlu pindah. Pindah HANYA bila rute pertama yang teramati
         * adalah rute awal (`home` = start dingin / proses baru) dan tab tersimpan adalah tab valid yang BERBEDA.
         * Bila NavController sudah dipulihkan sistem/SaveableStateProvider ke tab lain, rute pertama bukan `home`
         * -> null (tidak menimpa pemulihan yang sudah benar).
         */
        fun restoreTarget(saved: String?, firstRoute: String?): String? =
            saved?.takeIf { firstRoute == START_TAB && it != START_TAB && it in TABS }
    }
}
