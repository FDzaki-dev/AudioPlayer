package com.rudi.audioplayer.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Batch 101 — Adaptive layout (multi-device: tablet/foldable/Chromebook/split-screen).
 *
 * Breakpoint dp di bawah SAMA PERSIS dengan rekomendasi resmi Material 3 window size classes
 * (https://m3.material.io/foundations/layout/applying-layout/window-size-classes) — Compact
 * <600dp, Medium 600-839dp, Expanded >=840dp. Sengaja dihitung langsung dari
 * `LocalConfiguration.screenWidthDp` alih-alih menambah dependency
 * `androidx.compose.material3:material3-window-size-class` baru di `build.gradle.kts`
 * (protected asset) — angka breakpoint identik, cuma beda sumber baca, jadi 0 risiko tambahan
 * di dependency graph untuk kebutuhan app ini (pilih rail-vs-bar & satu-pane-vs-dua-pane).
 *
 * `LocalConfiguration.current` sendiri sudah reactive terhadap rotasi layar, masuk/keluar mode
 * multi-window/split-screen, dan lipat/buka foldable — jadi nilai ini otomatis recompose ulang
 * di semua kejadian itu tanpa kode tambahan apa pun di pemanggil.
 */
enum class AppWidthClass {
    /** HP dalam potret biasa. Semua perilaku UI di app ini SAMA seperti sebelum Batch 101 di
     *  kelas ini — NavigationBar bawah, satu layar penuh per waktu, tanpa panel tambahan. */
    COMPACT,

    /** HP landscape lebar, foldable terlipat, tablet kecil potret. NavigationRail menggantikan
     *  NavigationBar bawah (lebih hemat tinggi layar), tapi tetap satu panel konten. */
    MEDIUM,

    /** Tablet/foldable terbuka, Chromebook, split-screen lebar. NavigationRail + panel Now
     *  Playing persisten di sisi kanan selagi ada lagu aktif (lihat AppNavHost/MainActivity). */
    EXPANDED
}

@Composable
fun rememberAppWidthClass(): AppWidthClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> AppWidthClass.COMPACT
        widthDp < 840 -> AppWidthClass.MEDIUM
        else -> AppWidthClass.EXPANDED
    }
}
