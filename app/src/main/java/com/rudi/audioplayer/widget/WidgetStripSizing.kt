package com.rudi.audioplayer.widget

import kotlin.math.min

/**
 * Batch 640 — ukuran sampul + kenop layout STRIP (`widget_player_strip.xml`) menurut ukuran widget. Fungsi murni (tanpa Android)
 * supaya bisa diuji unit.
 *
 * Latar (screenshot + log v639): strip 46x263dp sudah benar, tetapi strip 129x264dp memakai ukuran dasar layout (sampul 32dp,
 * kenop 24/28/24dp) -> isi kecil di palung gelap yang luas. Di sini sampul dan kenop dibesarkan mengikuti ruang yang ada.
 *
 * Geometri layout (dp): padding atas+bawah 14 + sampul + margin palung 5 + palung (kluster kenop + padding palung 3+3).
 * Kluster = (prev 24 + play 28 + next 24 + 2 jarak 10) x skala + 6 -> 102dp pada skala 1 (= ambang tinggi strip 160dp, ruang sisa 7dp).
 */
internal object WidgetStripSizing {
    const val PAD_H_DP = 8
    const val PAD_V_DP = 7
    const val WELL_MARGIN_DP = 5
    const val ART_MIN_DP = 32f
    const val KEY_DP = 24f
    const val PLAY_DP = 28f
    const val GAP_DP = 10f
    const val KEY_PAD_DP = 5f
    const val PLAY_PAD_DP = 6f
    const val SCALE_MAX = 1.5f

    private const val CLUSTER_BASE_DP = 96f // 24 + 28 + 24 + 2 x 10, pada skala 1
    private const val CLUSTER_FIXED_DP = 6f // padding atas+bawah palung (3 + 3), tidak ikut skala
    private const val BREATH_DP = 12f // napas minimum palung di atas+bawah kluster bila ruang cukup

    data class Sizes(val artDp: Float, val scale: Float)

    /**
     * @param widthDp lebar minimum widget (dp), @param heightDp tinggi minimum widget (dp).
     * Skala kenop: tumbuh dengan lebar isi (maks [SCALE_MAX]) dan dibatasi tinggi agar sampul minimum + napas + kluster muat;
     * tak pernah di bawah 1. Sampul = sisa tinggi setelah kluster + napas, tak lebih lebar dari isi, tak di bawah [ART_MIN_DP].
     * Invarian (diuji): sampul + kluster + 19dp <= tinggi untuk tinggi >= 160dp.
     */
    fun compute(widthDp: Int, heightDp: Int): Sizes {
        val contentW = (widthDp - 2 * PAD_H_DP).toFloat().coerceAtLeast(ART_MIN_DP)
        val freeH = (heightDp - 2 * PAD_V_DP - WELL_MARGIN_DP).toFloat()
        val byWidth = contentW / ART_MIN_DP
        val byHeight = (freeH - ART_MIN_DP - BREATH_DP - CLUSTER_FIXED_DP) / CLUSTER_BASE_DP
        val scale = min(SCALE_MAX, min(byWidth, byHeight)).coerceAtLeast(1f)
        val cluster = CLUSTER_BASE_DP * scale + CLUSTER_FIXED_DP
        val art = min(contentW, freeH - cluster - BREATH_DP).coerceAtLeast(ART_MIN_DP)
        return Sizes(art, scale)
    }
}
