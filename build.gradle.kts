plugins {
    id("com.android.application") version "8.13.0" apply false
    // Batch 250 — bump 1.9.24→2.4.10. Trigger: build FAILED (`log_fail_256.zip`),
    // work-runtime-2.11.2 dikompilasi metadata Kotlin 2.1.0, project masih 1.9.24 (binary
    // incompatible, kspReleaseKotlin/kspDebugKotlin FAILED). Versi 2.4.10 dipilih (bukan 2.1.20
    // minimal) — latest STABLE per kotlinlang.org Agustus 2026 (dicek web_search), konsisten
    // "prioritas mutakhir" (aturan sesi #3), 2.4.20 masih RC jadi tidak dipilih.
    id("org.jetbrains.kotlin.android") version "2.4.10" apply false
    // Batch 250 — WAJIB sejak Kotlin 2.0+: Compose compiler sudah tidak dibundel di
    // kotlin-android plugin, harus plugin terpisah ini. Tanpa ini, `composeOptions.
    // kotlinCompilerExtensionVersion` lama (dihapus di app/build.gradle.kts batch ini) bakal
    // gagal krn compose compiler artifact versi 1.5.14 tidak ada utk Kotlin 2.4.10.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    // Batch 243 — Lyrics offline-first feature (Room). KSP dipilih ATAS kapt (kapt legacy,
    // lebih lambat) — konsisten "prioritas mutakhir" (aturan sesi #3). Versi disamakan Kotlin
    // 1.9.24 di atas.
    // Batch 250 — 2.3.10 (BUKAN 2.4.10, KSP versioning sudah decoupled dari Kotlin sejak KSP
    // 2.3.0, dicek web_search: pairing resmi kotlinlang.org docs Kotlin 2.4.10 + KSP 2.3.10).
    id("com.google.devtools.ksp") version "2.3.10" apply false
}
