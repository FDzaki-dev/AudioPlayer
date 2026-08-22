plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Batch 243 — Room (Lyrics cache). Lihat root build.gradle.kts utk versi.
    id("com.google.devtools.ksp")
}

// A hardcoded versionCode has to be remembered and manually bumped on every single
// release, and forgetting even once means the next APK isn't recognized as "newer" —
// which can silently break the update path on some devices/launchers even when the
// signing certificate is correct. Counting git commits gives a versionCode that's
// guaranteed to increase with every push, with no manual step to forget.
//
// Batch 56: counts commits since the "v-reset" tag instead of the whole repo history.
// After many batches the raw full-history count had grown large/ugly to look at in the
// version picker; this restarts the visible number from near-zero WITHOUT rewriting git
// history (no squash, no force-push — history stays intact for git blame/log). One-time
// setup (do this once in Termux, after this ZIP is pushed):
//   git tag v-reset && git push origin v-reset
// Every push after that tag exists counts from it; versionCode/versionName still only ever
// go up (rev-list --count is monotonic within the same range), so the update-path guarantee
// above is unaffected. Falls back to full HEAD count if the tag isn't present yet (so the
// very first build before you create the tag, or a shallow clone missing tags, still builds).
fun gitCommitCount(): Int = try {
    fun count(range: String): Int? {
        val process = ProcessBuilder("git", "rev-list", "--count", range)
            .redirectErrorStream(true)
            .start()
        process.waitFor()
        return process.inputStream.bufferedReader().readText().trim().toIntOrNull()
    }
    maxOf(1, count("v-reset..HEAD") ?: count("HEAD") ?: 1)
} catch (e: Exception) {
    1 // local build outside a git checkout, or git unavailable — never fail the build over this
}

val appVersionCode = gitCommitCount()
// Batch 30: versionName now derives from the same git-commit-count basis as versionCode,
// instead of a separately hand-maintained string. Two wins: (1) never needs a manual bump
// again — same "never forget" guarantee gitCommitCount() already gave versionCode; (2) the
// in-app version now numerically matches the CI-generated GitHub Release tag/APK filename
// (both land on "1.0.<count>" at the time). Nothing in .github/workflows/build.yml needed
// to change to get that match.
//
// Batch 86 — user: "bump version statis -> otomatis+dinamis". versionCode was already fully
// auto; versionName was ALREADY derived from it too (this comment, since Batch 30) — but the
// "1.0." prefix itself was a literal hardcoded string that would sit frozen forever no matter
// how much development happened, only the trailing number ever moved. MINOR is now dynamic
// too — genuinely evolves over time (1.0.x -> 1.1.x -> 1.2.x -> ...) instead of parking at
// "1.0" permanently. MAJOR is kept as a small manually-set constant ON PURPOSE, not an
// oversight: in virtually every real-world semver scheme — including fully "automated"
// release-please/semantic-release style tooling — MAJOR is gated behind an explicit human
// signal for breaking changes; blindly auto-incrementing MAJOR from a commit count would just
// be "static" in a different, more misleading way (a number that looks meaningful but isn't).
// versionCode itself is UNCHANGED (still the raw monotonic commit count, not derived from
// major/minor/patch) — it's an internal Android install-ordering integer only, nothing
// user-facing ever reads it, so it's kept dead-simple rather than adding arithmetic that could
// risk non-monotonicity later for zero benefit.
// Batch 87 — CI build FAILED (log_fail_139.zip, uploaded by user): "Const 'val' are only
// allowed on top level, in named objects, or in companion objects". `const val` needs true
// Kotlin file/package top-level, an object, or a companion object — a .gradle.kts SCRIPT body
// doesn't qualify as any of those (it compiles to members of an implicit Script class), even
// though the rest of this file's plain `val`s (appVersionCode, appVersionName, etc.) work fine
// at that same script-body location. Wrong assumption on my part in Batch 86 — carried over the
// "private const val" convention from regular Kotlin app source (where it's fine, e.g. inside a
// class/companion object) into a build script, where it isn't. Dropped `const` (and `private`,
// to match every other declaration in this file — none of them use it either); plain `val` has
// no such restriction and was already proven working by every other line here.
val versionMajor = 1
val commitsPerMinor = 50 // tunable — how many commits before minor ticks over
val appVersionName = "$versionMajor.${appVersionCode / commitsPerMinor}.${appVersionCode % commitsPerMinor}"

android {
    namespace = "com.rudi.audioplayer"
    // Batch 249 — bump 34→36. Trigger: build gagal (`log_fail_255.zip`), `androidx.work:
    // work-runtime-ktx:2.11.2` (Batch 246) butuh compileSdk 35+ & AGP 8.6.0+, project masih 34/8.4.1.
    // Pilih 36 (bukan pas 35) — max API yg didukung AGP 8.13 (dicek web_search Agustus 2026) skalian,
    // biar tidak perlu bump lagi kalau ada dependency lain nagih 35 nanti.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rudi.audioplayer"
        minSdk = 23
        targetSdk = 34
        versionCode = appVersionCode
        // versionCode and versionName both auto-derive from git commit count (see
        // gitCommitCount()/appVersionName above) — neither needs a manual bump.
        versionName = appVersionName
        // Batch 29 (hotfix): Batch 28 used androidResources.localeFilters, which doesn't exist
        // yet on this project's AGP 8.4.1 (confirmed by CI: "Unresolved reference:
        // localeFilters" — that DSL landed in a later AGP release than what's pinned here).
        // resourceConfigurations does the same job (drop unused library translation resources)
        // and is fully supported on 8.4.1 — only deprecated starting AGP 8.8, which this
        // project isn't on.
        resourceConfigurations += listOf("en")

        // Batch 103 (Gap List #2, Instrumentation testing) — runner default AndroidJUnitRunner
        // dari androidx.test, dibutuhkan supaya app/src/androidTest bisa dieksekusi lewat
        // `./gradlew connectedAndroidTest` (device/emulator fisik) atau job CI baru di
        // .github/workflows/build.yml. Tidak berlaku sama sekali utk build release biasa —
        // murni metadata utk target test task, nol dampak ke APK yang di-Release.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Release Downloader Spec — owner/repo GitHub yang dicek UpdateManager untuk rilis
        // terbaru. Sengaja dibaca dari gradle.properties (bukan hardcoded di Kotlin) supaya
        // ganti target repo cukup edit 1 baris properties, nol sentuh source/logic app.
        buildConfigField(
            "String", "UPDATE_REPO_OWNER",
            "\"${project.findProperty("UPDATE_REPO_OWNER") ?: "ganti-username-github"}\""
        )
        buildConfigField(
            "String", "UPDATE_REPO_NAME",
            "\"${project.findProperty("UPDATE_REPO_NAME") ?: rootProject.name}\""
        )
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("SIGNING_KEYSTORE_PATH")
            if (keystorePath != null && file(keystorePath).let { it.exists() && it.length() > 0 }) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Shrinks + obfuscates the release APK. Media3, Coil, and AndroidX all ship
            // their own consumer proguard rules, and this project has no reflection-based
            // JSON/serialization (playlists etc. are hand-rolled org.json), so there's
            // nothing here that R8 needs extra keep rules for.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Uses the stable release keystore when CI actually decoded a non-empty one
            // (consistent signature across builds, no more uninstall-before-install). Falls
            // back to the debug keystore whenever that file is missing or empty — e.g. local
            // builds, or a CI run where the secret wasn't set.
            val keystorePath = System.getenv("SIGNING_KEYSTORE_PATH")
            val hasStableKeystore = keystorePath != null && file(keystorePath).let { it.exists() && it.length() > 0 }
            signingConfig = if (hasStableKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            // Batch 20: see app/compose_stability_config.conf for why this exists.
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=" +
                "${project.projectDir}/compose_stability_config.conf"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
    // Batch 76 — "pangkas waktu compile sampai habis": lanjutan Batch 62. AGP secara default
    // menjalankan lintVitalRelease sebagai dependency assembleRelease (subset lint fokus
    // isu fatal — manifest merger, translasi hilang, dst) SEBELUM APK release dirakit. Ini
    // TIDAK mengubah 1 byte pun output APK (lint murni analisis statis, tidak pernah menulis
    // apa pun ke APK) — tapi juga bukan "zero-cost": ini genuinely MENGURANGI 1 lapis
    // verifikasi otomatis yang tadinya jalan tiap release build, jadi beda dari lever Batch 62
    // yang sepenuhnya tanpa efek samping. Catatan jujur, bukan disembunyikan: `./gradlew lint`/
    // `lintRelease` tetap bisa dijalankan manual kapan pun kalau perlu cek fatal-lint issues;
    // ini cuma melepas pengait OTOMATISnya dari tiap assembleRelease di CI.
    lint {
        checkReleaseBuilds = false
    }

    // The unit tests under src/test are plain JVM tests (no Robolectric, no emulator) — any
    // Android SDK call they happen to touch (e.g. Uri.parse while building a test fixture)
    // would otherwise throw "not mocked" instead of just returning a harmless default.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// NOTE: output APK renaming is handled in .github/workflows/build.yml (the "Rename APK to
// match version" step), not here — doing it in both places would make the workflow's `cp`
// step fail looking for a file this block already renamed out from under it.

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    // Release Downloader Spec — satu-satunya dependency jaringan di proyek ini, dipakai
    // UpdateDownloader/GitHubReleaseChecker (Settings → Lanjutan → Cek Update). Okio (streaming
    // sink chunk-by-chunk) sudah ikut sebagai transitive dependency OkHttp, tidak perlu baris
    // terpisah.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    // Batch 20: lifecycle-aware state collection (collectAsStateWithLifecycle) so Compose
    // stops collecting StateFlows while the app is backgrounded, instead of collectAsState()
    // which keeps collecting regardless of lifecycle state.
    // Batch 23: 2.8.1 has a confirmed upstream bug (fixed in 2.8.2, aosp/3105647 b/336842920)
    // where collectAsStateWithLifecycle() throws "CompositionLocal LocalLifecycleOwner not
    // present" when paired with Compose UI 1.6.x (which is what compose-bom 2024.05.00 below
    // resolves to) — crashed on every single launch since Batch 20 first called it. All 3
    // lifecycle-* artifacts above bumped together to 2.8.2 to stay consistent.
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    // Batch 20: stable, structurally-comparable collections for favoriteIds/selectedIds so
    // Compose can skip recomposing the library list when neither actually changed, instead of
    // treating plain Set<Long> as always-unstable and recomposing every visible row on any
    // unrelated recomposition (e.g. playback position ticking every second).
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")

    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")
    implementation("androidx.media3:media3-common:1.3.1")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.7.1")

    // Batch 28: replaces full com.google.guava:guava (only ever used here for ListenableFuture/
    // SettableFuture/MoreExecutors.directExecutor() to satisfy Media3's session-callback API
    // surface, which returns ListenableFuture). concurrent-futures pulls in just the tiny
    // com.google.guava:listenablefuture:1.0 interface shim, not the full Guava jar — same
    // ListenableFuture type, none of the rest of Guava's collections/cache/etc. weight.
    implementation("androidx.concurrent:concurrent-futures:1.3.0")

    // Pure-JVM unit tests only (src/test) — no Robolectric/instrumentation, so these run in
    // seconds with no emulator and are cheap enough to actually get written and kept up to date.
    testImplementation("junit:junit:4.13.2")
    // Uri.parse() (and any other android.net.Uri call) returns null under isReturnDefaultValues
    // = true, not a harmless placeholder — assigning that to Song's non-null `uri: Uri` field
    // throws a NullPointerException. mockito-core's mock(Uri::class.java) builds a real Uri
    // instance via bytecode proxying instead of calling into the stubbed platform class, so
    // test fixtures that need *a* Uri (without caring what it resolves to) can get one safely.
    testImplementation("org.mockito:mockito-core:5.12.0")

    // Batch 103 (Gap List #2) — src/androidTest, BEDA dari src/test di atas: ini jalan di
    // device/emulator sungguhan (bukan pure-JVM), lewat `./gradlew connectedAndroidTest` atau
    // job CI baru "instrumentation-tests" di .github/workflows/build.yml (emulator, job
    // TERPISAH dari job release `build` — release tidak pernah ikut gagal kalau job ini
    // flaky/lambat). Baru ditambah sekarang justru karena kebutuhannya sudah ada: menguji
    // MediaController sungguhan bicara ke PlaybackService sungguhan (play/pause/seek/skip/
    // repeat/shuffle) butuh Android runtime beneran, bukan sesuatu yang bisa disimulasikan
    // pure-JVM seperti test di atas.
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:core:1.6.1")

    // Batch 243 — Lyrics offline-first (cache Room). room-ktx bawa Flow/suspend DAO support.
    // Ver 2.6.1 = stabil terbaru per KSP 1.9.24 pairing di atas.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Batch 244 — Lyrics offline-first 2/4. Retrofit2 + converter-gson buat LRCLIB API.
    // okhttp3 sudah ada (UpdateDownloader/GitHubReleaseChecker) — Retrofit numpang instance
    // client yg sama pattern (timeout eksplisit), bukan dependency baru buat http-nya sendiri.
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Batch 246 — Lyrics offline-first 4/4a. CoroutineWorker buat LyricsPrefetchWorker.
    // Versi 2.11.2 = rekomendasi resmi developer.android.com/kotlin/ktx per Agustus 2026
    // (dicek langsung, bukan diasumsikan dari training data — versi WorkManager sering
    // berubah shape antar rilis).
    implementation("androidx.work:work-runtime-ktx:2.11.2")
}
