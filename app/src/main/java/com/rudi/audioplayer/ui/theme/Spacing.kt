package com.rudi.audioplayer.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Batch 54 (technical debt pass) — centralized shape/spacing tokens. Not previously mandated by
 * any design spec batch, but compose-amoled-hybrid-glass-final.md §19 ("Spacing & Shape
 * Language") gives the same rounded-geometry guidance this project has followed ad hoc since
 * Batch 1: "Do not mix many unrelated corner radii" + a recommended scale (small controls
 * 10-12dp, standard controls 12-16dp, cards 16-20dp, large surfaces 20-24dp).
 *
 * Scope note: this file introduces the token system and migrates every `RoundedCornerShape(N.dp)`
 * corner-radius literal in the codebase (31 call sites across 8 files, all distinct values 4-28dp)
 * to a named `Radius` token — the single most-duplicated class of `.dp` literal in the project
 * (grep: 10x `20.dp`, 6x `18.dp` alone). General padding/size/offset `.dp` literals (~340 more,
 * spread across paddings, icon sizes, gesture thresholds, blur radii, etc.) are intentionally
 * NOT touched this batch: unlike corner radii — which map cleanly to a handful of repeated
 * "shape family" values — most padding/size literals are one-off and context-specific (e.g. a
 * `9.dp` shadow offset tuned to one exact hero art shadow), so forcing them into a shared token
 * table would either lose that intentional specificity or require a much larger token surface
 * than this file's actual reuse would justify. This is the same reasoning PROJECT_STATE.md
 * Batch 31/35 already gave for deferring a full sweep, still valid: doing 340+ mechanical
 * substitutions across every screen file without a compiler in this environment to verify the
 * result is a real risk of a silent syntax/reference break that a grep-only pass can't catch.
 */
object Radius {
    val xs = 4.dp    // small accents (e.g. mini progress bar corners)
    val sm = 10.dp   // small controls (spec §19: "Small controls 10-12dp")
    val md = 12.dp   // standard controls (spec §19: "Standard controls 12-16dp")
    val ml = 14.dp   // banners / mid-weight controls
    val lg = 16.dp   // large tactile surfaces (spec §19 upper end of "Standard controls")
    val xl = 18.dp   // elevated panels / hero rows
    val xxl = 20.dp  // cards (spec §19: "Cards 16-20dp")
    val xxxl = 24.dp // large surfaces (spec §19: "Large surfaces 20-24dp")
    val hero = 28.dp // Apple theme's own large-shape token (AppleShapes.large)

    // Batch 279 — ROADMAP_LIQUID_GLASS_REDESIGN.md §5 fase 1 ("fondasi token baru... sebagai
    // identitas terpisah dulu, belum jadi default"). CONVX-terinspirasi: "radius lebih besar/
    // pill, minimalis" (§3b Opsi B, shape+typography TANPA blur asli). Purely ADDITIF — 0 nilai
    // di atas diubah, 0 theme lama kebagian efek (belum dipakai di manapun sampai
    // `LiquidGlassShapes` didefinisikan di Theme.kt, fase 2 roadmap yang sama).
    val liquidLg = 34.dp  // 1 langkah lebih besar dari `hero` — panel/card besar ala CONVX
    val liquidPill = 999.dp // stadium/pill penuh — tombol & chip liquid glass, dp sengaja jauh
                             // melebihi tinggi elemen manapun di app ini supaya Compose selalu
                             // clamp ke radius maksimum yg mungkin (setengah tinggi elemen),
                             // menjamin ujung selalu bulat sempurna apa pun tinggi kontrolnya —
                             // ini pola RESMI utk stadium shape di Compose (lihat dokumentasi
                             // RoundedCornerShape), bukan angka sembarang.
}
