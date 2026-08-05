plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// A hardcoded versionCode has to be remembered and manually bumped on every single
// release, and forgetting even once means the next APK isn't recognized as "newer" —
// which can silently break the update path on some devices/launchers even when the
// signing certificate is correct. Counting git commits gives a versionCode that's
// guaranteed to increase with every push, with no manual step to forget.
fun gitCommitCount(): Int = try {
    val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
        .redirectErrorStream(true)
        .start()
    process.waitFor()
    process.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 1
} catch (e: Exception) {
    1 // local build outside a git checkout, or git unavailable — never fail the build over this
}

val appVersionCode = gitCommitCount()

android {
    namespace = "com.rudi.audioplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rudi.audioplayer"
        minSdk = 23
        targetSdk = 34
        versionCode = appVersionCode
        // versionCode auto-increments with git history (see gitCommitCount() above) so it can
        // never be forgotten. versionName stays a deliberate, human-readable string set per
        // release — kept in sync with the zip filename and commit message for that update.
        versionName = "3.9"
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

    implementation("com.google.guava:guava:33.2.1-android")

    // Pure-JVM unit tests only (src/test) — no Robolectric/instrumentation, so these run in
    // seconds with no emulator and are cheap enough to actually get written and kept up to date.
    testImplementation("junit:junit:4.13.2")
}
