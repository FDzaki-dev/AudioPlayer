// Batch 570 — SpotBugs (laporan SARIF) untuk workflow `.github/workflows/android-artifact.yml`.
// OPT-IN: berkas ini HANYA di-apply bila `-Psonix.spotbugs=true` (blok `if` di app/build.gradle.kts).
// Build normal (`build.yml`: `testReleaseUnitTest assembleRelease`) TIDAK memuat plugin/dependency/
// skrip ini sama sekali -> jalur rilis tidak tersentuh.
//
// Beda dari contoh `github-claude-artifact.md` (dan alasannya):
//  - Plugin `com.github.spotbugs` hanya membuat task untuk proyek `java`; bagian "Apply to Android
//    project" di README resminya masih "TBU". Task `spotbugsRelease` TIDAK ADA di proyek Android, jadi
//    `tasks.named<SpotBugsTask>("spotbugsRelease")` gagal saat configuration dan merusak SELURUH build.
//    Di sini dipakai plugin dasar `com.github.spotbugs-base` + task didaftarkan manual (cara yang
//    disebut README resmi: "apply the base plugin instead").
//  - Versi plugin dipin 6.5.6 (SpotBugs 4.10.2 bawaan; rilis 10 Jun 2026 = >14 hari, stable).
//  - Tanpa `auxClassPaths`: classpath compile Android (AAR) tak bisa dipakai SpotBugs apa adanya;
//    SpotBugs tetap jalan, hanya mencatat "classes needed for analysis were missing" (presisi turun).
// Jalankan lokal: gradle spotbugsRelease -Psonix.spotbugs=true  ->  app/build/reports/spotbugs/spotbugs.sarif
import com.github.spotbugs.snom.SpotBugsTask

buildscript {
    repositories { gradlePluginPortal() }
    dependencies {
        classpath("com.github.spotbugs-base:com.github.spotbugs-base.gradle.plugin:6.5.6")
    }
}

apply(plugin = "com.github.spotbugs-base")

// Kelas hasil kompilasi varian release (Kotlin = tmp/kotlin-classes, Java = intermediates/javac).
val sonixKotlinClasses = layout.buildDirectory.dir("tmp/kotlin-classes/release").get().asFile
val sonixJavaClasses =
    layout.buildDirectory.dir("intermediates/javac/release/compileReleaseJavaWithJavac/classes").get().asFile

tasks.register<SpotBugsTask>("spotbugsRelease") {
    group = "verification"
    description = "SpotBugs (laporan SARIF) atas kelas varian release; NON-BLOCKING."
    dependsOn("compileReleaseKotlin", "compileReleaseJavaWithJavac")
    classes = fileTree(sonixKotlinClasses) { include("**/*.class") } +
        fileTree(sonixJavaClasses) { include("**/*.class") }
    sourceDirs.setFrom(file("src/main/java"))
    // Temuan tetap tercatat di laporan tapi task TIDAK gagal (selaras detekt: ignoreFailures = true).
    ignoreFailures = true
    reports.create("sarif") {
        required.set(true)
        outputLocation.set(layout.buildDirectory.file("reports/spotbugs/spotbugs.sarif"))
    }
}
