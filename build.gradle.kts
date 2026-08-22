plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    // Batch 243 — Lyrics offline-first feature (Room). KSP dipilih ATAS kapt (kapt legacy,
    // lebih lambat) — konsisten "prioritas mutakhir" (aturan sesi #3). Versi disamakan Kotlin
    // 1.9.24 di atas.
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
