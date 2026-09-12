package com.rudi.audioplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import com.rudi.audioplayer.ui.adaptive.AppWidthClass
import com.rudi.audioplayer.ui.adaptive.rememberAppWidthClass
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
// Batch 439 — 4 import baru, semua utk 1 tujuan: matikan ripple Android bawaan di 3
// NavigationBarItem tab bawah tanpa mengganti mekanisme klik (lihat `NoRippleIndication` +
// pemakaiannya di `bottomBar`). `LocalIndication` (dipakai internal semua komponen
// selectable/clickable Compose Foundation, termasuk `NavigationBarItem`), `InteractionSource`
// = tipe parameter kontrak itu (beda dari `MutableInteractionSource` di atas yang sudah lama
// dipakai — itu implementasi konkretnya), `ContentDrawScope` = receiver wajib method `draw()`.
// Batch 440 — trigger `log_fail_424.zip`: `Indication`/`IndicationInstance` (kontrak lama)
// DIHAPUS dari sini, `compileDebugKotlin`/`compileReleaseKotlin` FAILED (bukan cuma warning —
// versi Compose Foundation di compose-bom 2026.04.01 sudah menaikkan level deprecation
// interface itu jadi ERROR, bukan lagi WARNING). Diganti `IndicationNodeFactory`
// (`NoRippleIndication` di bawah) — kontrak pengganti resmi berbasis `Modifier.Node`, 0 API
// publik lain disentuh, 0 perubahan behavior (masih murni `drawContent()` kosong).
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rudi.audioplayer.bubble.FloatingBubbleService
import com.rudi.audioplayer.playback.PlayerViewModel
import com.rudi.audioplayer.ui.HomeScreen
import com.rudi.audioplayer.ui.LockScreen
import com.rudi.audioplayer.ui.LibraryScreen
import com.rudi.audioplayer.ui.SettingsScreen
import com.rudi.audioplayer.ui.StatsDashboardScreen
import com.rudi.audioplayer.ui.MiniPlayerBar
import com.rudi.audioplayer.ui.NowPlayingScreen
import com.rudi.audioplayer.ui.theme.ThemeIdentity
import com.rudi.audioplayer.ui.theme.isSkeuTheme
import com.rudi.audioplayer.ui.bouncyPress
import com.rudi.audioplayer.ui.theme.ThemeMode
import com.rudi.audioplayer.ui.theme.AudioPlayerTheme
import com.rudi.audioplayer.ui.theme.resolveIsDark
import com.rudi.audioplayer.ui.theme.MidnightBlue
import com.rudi.audioplayer.ui.theme.MidnightBlueAmbientAlpha
import com.rudi.audioplayer.ui.theme.MidnightBlueLightAmbientAlpha
import com.rudi.audioplayer.ui.theme.AmoledSurface
import com.rudi.audioplayer.ui.theme.TactileHighlight
import com.rudi.audioplayer.ui.theme.TactileLightSurfaceVariant
import com.rudi.audioplayer.ui.theme.SkeuHighlight
import com.rudi.audioplayer.ui.theme.SkeuAccent
import com.rudi.audioplayer.ui.theme.TitaniumDark
import com.rudi.audioplayer.ui.theme.SilverHighlight
import com.rudi.audioplayer.ui.theme.SkeuDarkSurfaceVariant
import com.rudi.audioplayer.ui.theme.SkeuLightSurfaceVariant
import com.rudi.audioplayer.ui.theme.SkeuAmbientAlphaDark
import com.rudi.audioplayer.ui.theme.SkeuAmbientAlphaLight
import com.rudi.audioplayer.ui.theme.SkeuEmerald
import com.rudi.audioplayer.ui.theme.SkeuLightEmerald
import com.rudi.audioplayer.ui.theme.calmGrain
import com.rudi.audioplayer.ui.theme.auroraGlow
import com.rudi.audioplayer.ui.theme.LocalHazeState
import dev.chrisbanes.haze.rememberHazeState
// Batch 435 — swipe-lintas-3-tab (Beranda/Perpustakaan/Pengaturan). Semua import di bawah
// disalin persis dari path yang SUDAH terbukti compile di ui/NowPlayingScreen.kt
// (AlbumArtHero, Batch 434) — bukan tebakan path baru.
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
// Batch 437 — kaca pembesar label tab bawah: LocalTextStyle (baca style label bawaan
// NavigationBarItem apa adanya, cuma fontSize yg di-override) + blur (RenderEffect, aman krn
// minSdk 31).
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.draw.blur

// Batch 435 — urutan resmi 3 tab bawah (sama persis urutan NavigationBarItem/NavigationRailItem
// di AppNavHost bawah: Beranda, Perpustakaan, Pengaturan). Dipakai gesture swipe untuk hitung
// tab tujuan (index±1) — SATU sumber kebenaran urutan, bukan didup di 2 tempat.
private val TAB_ROUTES = listOf("home", "library", "settings")

class MainActivity : FragmentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(applicationContext) as T
            }
        }
    }

    // Backed by Compose state so a shortcut tap (handled in onNewIntent, a plain Activity
    // callback outside the composition) can still signal the composable tree to react.
    private var pendingShortcutAction by mutableStateOf<String?>(null)

    // Re-locks whenever the app is genuinely backgrounded (not on config changes, since
    // onStop only fires when actually leaving, not on rotation) — mutableState so Compose
    // reacts immediately without needing a process restart.
    private var isUnlocked by mutableStateOf(false)

    override fun onStop() {
        super.onStop()
        isUnlocked = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // MainActivity is launchMode="singleTop", so tapping a shortcut while the app is
        // already running redelivers here instead of recreating the Activity — without this
        // override the shortcut would silently do nothing unless the app was cold-started.
        setIntent(intent)
        pendingShortcutAction = intent.data?.toString()
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(this)
        val prompt = androidx.biometric.BiometricPrompt(
            this, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            }
        )
        val info = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka SONIX")
            .setNegativeButtonText("Pakai PIN")
            .build()
        prompt.authenticate(info)
    }

    private fun isBiometricAvailable(): Boolean {
        val manager = androidx.biometric.BiometricManager.from(this)
        return manager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        pendingShortcutAction = intent?.data?.toString()
        playerViewModel.connect()

        setContent {
            // Batch 24: bridge the two separate LocalLifecycleOwner CompositionLocals.
            // androidx.compose.ui.platform.LocalLifecycleOwner (Compose UI 1.6.x, what this
            // project's compose-bom resolves to) IS correctly populated by setContent() here.
            // androidx.lifecycle.compose.LocalLifecycleOwner (a separate, newer CompositionLocal
            // used internally by collectAsStateWithLifecycle()) is NOT automatically bridged from
            // the old one on Compose UI 1.6.x — bumping lifecycle to 2.8.2 alone (Batch 23) did
            // not fix this crash in practice, despite upstream release notes claiming it should.
            // Explicitly providing it here removes the dependency on that fix entirely.
            CompositionLocalProvider(
                androidx.lifecycle.compose.LocalLifecycleOwner provides androidx.compose.ui.platform.LocalLifecycleOwner.current
            ) {
            val appThemeIdentity by playerViewModel.themeIdentity.collectAsStateWithLifecycle()
            val appThemeMode by playerViewModel.themeMode.collectAsStateWithLifecycle()
            AudioPlayerTheme(identity = appThemeIdentity, mode = appThemeMode) {
                // enableEdgeToEdge() above only sets the *initial* system bar icon style once,
                // at process start — it never reacts to the in-app theme picker. Without this,
                // switching to "Terang" leaves status/nav bar icons stuck light-on-light
                // (styled for the dark theme they started in) and effectively invisible.
                val isDarkTheme = resolveIsDark(appThemeMode)
                val decorView = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, decorView).apply {
                        isAppearanceLightStatusBars = !isDarkTheme
                        isAppearanceLightNavigationBars = !isDarkTheme
                    }
                }

                val context = LocalContext.current

                // Both shortcuts mirror an action already reachable from the Home screen
                // (the shuffle icon next to the greeting, and the "Lanjutkan" card) — same
                // effect, just launchable straight from the launcher icon without opening
                // the app first. Neither navigates anywhere; playback simply starts and the
                // mini player appears, exactly like tapping those same buttons would.
                val librarySongsForShortcut by playerViewModel.librarySongs.collectAsStateWithLifecycle()
                LaunchedEffect(pendingShortcutAction, librarySongsForShortcut.isEmpty()) {
                    val action = pendingShortcutAction ?: return@LaunchedEffect
                    if (librarySongsForShortcut.isEmpty()) return@LaunchedEffect
                    when (action) {
                        "audioplayer://shuffle_all" -> playerViewModel.shuffleAll(librarySongsForShortcut)
                        "audioplayer://continue_listening" -> playerViewModel.resumeFromSaved(librarySongsForShortcut)
                    }
                    pendingShortcutAction = null
                }

                val neededPermissions = remember {
                    buildList {
                        add(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                Manifest.permission.READ_MEDIA_AUDIO
                            else
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                }

                fun isAudioPermissionGranted(): Boolean =
                    ContextCompat.checkSelfPermission(context, neededPermissions[0]) ==
                        PackageManager.PERMISSION_GRANTED

                // Reflects Android's actual current grant state instead of always assuming
                // "not granted" — otherwise every fresh process start (very common: the app
                // gets backgrounded, killed for memory, reopened later) would show the
                // welcome/permission screens again even though the permission was already
                // given previously.
                var hasPermission by remember { mutableStateOf(isAudioPermissionGranted()) }
                var permissionRequested by remember { mutableStateOf(false) }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    hasPermission = result[neededPermissions[0]] == true
                }

                val lockEnabled by playerViewModel.lockEnabled.collectAsStateWithLifecycle()
                val biometricEnabled by playerViewModel.biometricEnabled.collectAsStateWithLifecycle()
                val needsUnlock = lockEnabled && !isUnlocked

                LaunchedEffect(needsUnlock, biometricEnabled) {
                    if (needsUnlock && biometricEnabled && isBiometricAvailable()) {
                        showBiometricPrompt { isUnlocked = true }
                    }
                }

                // Batch 49: dropped the Matte-only "transparent Surface + ambient glow Box"
                // trick entirely (matteDepthBrush() removed from Theme.kt) along with the rest
                // of the Matte identity — this also permanently forecloses the whole Batch 48
                // bug class (invisible text from a Transparent-color Surface silently losing
                // contentColor), because contentColor is always passed explicitly below
                // regardless of the color param, so contentColorFor()'s auto-derivation is never
                // consulted for either branch.
                //
                // Batch 52: reverted the Batch 51 transparent-Surface + gradient-Box trick — the
                // new spec (compose-skeuomorphism-lite-midnight-blue.md) §2 gives `Background`
                // as a single flat literal token (0xFF191970), not a two-stop gradient pair, and
                // §1.1 describes it as "near-black / AMOLED-safe dark", a flat description. A
                // plain `color = colorScheme.background` now expresses the spec correctly again,
                // same shape as the non-Tactile branch (contentColor stays explicit either way,
                // so this was never dependent on the Batch 48 Unspecified-content-color bug
                // class regardless of which branch runs).
                // Batch 53 — compose-amoled-hybrid-glass-final.md §6 "Correct use": Midnight Blue
                // is only ever an atmospheric gradient ingredient, applied at the root ambient
                // layer (spec §7's conceptual stack starts with "Ambient background -> subtle
                // Midnight Blue gradient"), never as a flat surface color (§6 "Incorrect use").
                // `color = colorScheme.background` alone (the pre-Batch-53 approach) is flat and
                // AMOLED-only; layering a very-low-alpha diagonal Midnight Blue wash on top via a
                // background Brush (Tactile only — every other theme keeps its plain flat color)
                // is the minimum change needed to express this one spec rule without touching any
                // other screen file, since this Surface is the single shared root every screen
                // renders inside.
                // Batch 62 — DIBATALKAN atas instruksi eksplisit user: "perkuat vibes tiap tema
                // custom secara radikal, tanpa mengikuti batasan light/dark system". Ambient wash
                // sekarang trait IDENTITAS (selalu tampil, di kedua mode, dgn alpha & stop warna
                // masing-masing dituning per mode — lihat Color.kt) — bukan lagi trait mode.
                // Batch 63 — Skeu tidak lagi berbagi resep 3-stop yang identik dgn Tactile (user:
                // "wajib menampilkan visual secara otonom tanpa baseline yang identik"). Tactile
                // tetap 3-stop even wash (kaca atmosferik, structure lama tidak berubah). Skeu naik
                // jadi 4-stop dgn colorStops custom — TitaniumDark & SilverHighlight ditumpuk jadi
                // 1 "kilau" sempit (bukan blend rata sepanjang gradient) meniru pantulan cahaya di
                // logam disikat ("brushed metal streak"), lalu turun lagi ke SkeuSurfaceVariant.
                // Warnanya pun sudah bukan lagi turunan SkeuAccent tembaga (dihapus total) — murni
                // TitaniumDark/SilverHighlight, keluarga token baru khusus utk efek metalik ini.
                // Batch 393 — HOTFIX Batch 392, laporan build gagal user (log_fail_378.zip,
                // Gradle 8.14.3 / CI build #378): `compileDebugKotlin`/`compileReleaseKotlin`
                // GAGAL, 3 error identik "@Composable invocations can only happen from the
                // context of a @Composable function" persis di 3 baris `MaterialTheme
                // .colorScheme.background` yang Batch 392 pindahkan ke dalam `remember { }` di
                // bawah tanpa disadari. Root cause: parameter `calculation` milik `remember()`
                // ditandai `@DisallowComposableCalls` oleh Compose runtime sendiri — memanggil
                // property `@Composable` apa pun (termasuk `MaterialTheme.colorScheme`, getter-
                // nya sendiri `@Composable @ReadOnlyComposable`) di dalam lambda itu ilegal,
                // bukan sekadar gaya penulisan. Ini TIDAK kelihatan dari pembacaan kode statis
                // biasa (compiler Kotlin+Compose plugin sungguhan yang menegakkannya) — persis
                // kelas kesalahan yang PROJECT_STATE.md sendiri sudah wanti-wanti berulang kali:
                // "belum ditest di device asli ATAU build/lint sungguhan" utk tiap batch sejak
                // 385, sekarang benar-benar kejadian. Fix: `MaterialTheme.colorScheme.background`
                // dibaca SEKALI di sini, di context composable biasa (bukan di dalam remember) —
                // lalu `remember(...)` menutup atas NILAI plain `Color` ini (`rootBackgroundColor`,
                // ditambahkan sbg key ke-3 supaya tetap benar kalau suatu saat nilainya berubah
                // lepas dari appThemeIdentity/isDarkTheme), bukan memanggil ulang property
                // composable-nya. **Zero behavior change** dari niat asli Batch 392 — nilai brush
                // yang dihasilkan identik, cuma titik pembacaan warnanya yang dipindah ke luar
                // lambda `remember`. Detail lengkap CHANGELOG.md Batch 393.
                val rootBackgroundColor = MaterialTheme.colorScheme.background
                val identityRootBrush = remember(appThemeIdentity, isDarkTheme, rootBackgroundColor) {
                    when (appThemeIdentity) {
                        ThemeIdentity.TACTILE -> Brush.linearGradient(
                            colors = if (isDarkTheme)
                                listOf(
                                    rootBackgroundColor,
                                    MidnightBlue.copy(alpha = MidnightBlueAmbientAlpha),
                                    AmoledSurface
                                )
                            else
                                listOf(
                                    rootBackgroundColor,
                                    MidnightBlue.copy(alpha = MidnightBlueLightAmbientAlpha),
                                    TactileLightSurfaceVariant
                                )
                        )
                        ThemeIdentity.SKEU_DARK_LITE -> {
                            val streakAlpha = if (isDarkTheme) SkeuAmbientAlphaDark else SkeuAmbientAlphaLight
                            val streakEnd = if (isDarkTheme) SkeuDarkSurfaceVariant else SkeuLightSurfaceVariant
                            // Batch 80 — fix: Batch 79's emerald stop used `streakAlpha * 0.9f`, tapi
                            // streakAlpha itself sudah sangat kecil (0.05f gelap / 0.12f terang) —
                            // hasil akhirnya cuma alpha ~0.045/0.108, praktis tak kelihatan (user:
                            // "yang kelihatan cuman Titanium dominan, mana zamrudnya??"). Beda dgn
                            // SilverHighlight yg walau alpha kecil tetap kebaca krn warnanya nyaris
                            // putih (kontras tinggi thd background gelap/terang), warna emerald yg
                            // medium-saturation butuh alpha jauh lebih tinggi buat kebaca sama sekali.
                            // Sekarang pakai alpha TETAP (tidak lagi diturunkan dari streakAlpha),
                            // sengaja masih di bawah level accent-glow biasa (~0.42-0.45f di tempat
                            // lain di app ini) supaya tetap terbaca "sentuhan", bukan aksen utama —
                            // tapi genuinely visible, bukan cuma teknis-ada-di-kode.
                            val emerald = if (isDarkTheme) SkeuEmerald else SkeuLightEmerald
                            val emeraldStreakAlpha = if (isDarkTheme) 0.30f else 0.36f
                            Brush.linearGradient(
                                *arrayOf(
                                    0.00f to rootBackgroundColor,
                                    // Titik kilau sempit (0.60-0.68) ditumpuk tepat setelah TitaniumDark
                                    // — rentang fraction yang sengaja disempitkan (bukan disebar rata
                                    // seperti resep 3-stop Tactile) supaya terbaca sebagai satu garis
                                    // pantulan cahaya di logam, bukan gradasi warna yang mulus.
                                    0.55f to TitaniumDark.copy(alpha = streakAlpha),
                                    0.62f to SilverHighlight.copy(alpha = streakAlpha * 1.8f),
                                    0.68f to TitaniumDark.copy(alpha = streakAlpha),
                                    0.76f to emerald.copy(alpha = emeraldStreakAlpha),
                                    1.00f to streakEnd
                                )
                            )                        }
                        else -> null
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (identityRootBrush != null) Modifier.background(identityRootBrush) else Modifier
                        )
                        // v3 upgrade (palet_warna_calm_retro_v3.md, Pilar D — Organic Grain
                        // Overlay) — "lapisi seluruh kanvas aplikasi" secara literal berarti
                        // titik root ini (satu-satunya Surface yang dibungkus semua layar),
                        // sama slot arsitektur dengan identityRootBrush di atas untuk identitas
                        // lain, HANYA aktif untuk Calm Retro.
                        .then(
                            if (appThemeIdentity == ThemeIdentity.CALM_RETRO) Modifier.calmGrain() else Modifier
                        )
                        // Batch 308 — Aurora fase 3/N: wiring `auroraGlow()` (TactileDepth.kt,
                        // Batch 306 — sudah ada dari fase 1, 0 call site sampai baris ini) ke root
                        // Surface, pola arsitektur sama persis `calmGrain()` di atas (1 titik
                        // cakupan seluruh app, aktif hanya saat identitas ini yang dipilih). Mulai
                        // batch ini animasi gradien mengalirnya baru benar-benar terlihat di layar
                        // — sebelumnya (Batch 306-307) fungsinya ada tapi tampilan tetap flat.
                        .then(
                            if (appThemeIdentity == ThemeIdentity.AURORA) Modifier.auroraGlow() else Modifier
                        ),
                    color = if (identityRootBrush != null) Color.Transparent else MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            needsUnlock -> LockScreen(
                                biometricEnabled = biometricEnabled && isBiometricAvailable(),
                                onVerifyPin = { pin -> playerViewModel.verifyPin(pin) },
                                onUnlocked = { isUnlocked = true },
                                onRequestBiometric = { showBiometricPrompt { isUnlocked = true } },
                                initialLockedOutUntil = remember(needsUnlock) { playerViewModel.currentPinLockout() }
                            )
                            hasPermission -> AppNavHost(playerViewModel, isBiometricAvailable())
                            !permissionRequested -> WelcomeScreen(
                                onContinue = {
                                    permissionRequested = true
                                    launcher.launch(neededPermissions)
                                }
                            )
                            else -> PermissionRationale(
                                onRequest = { launcher.launch(neededPermissions) }
                            )
                        }
                    }
                }
            }
            } // tutup CompositionLocalProvider (Batch 24)
        }
    }
}

@Composable
private fun WelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                )
            )
            // Batch 111 — layar ini render DI LUAR Scaffold (lihat setContent di MainActivity),
            // jadi tidak dapat contentWindowInsets bawaan Scaffold sama sekali. Di gesture-nav
            // (Android 16 test device) bar cuma overlay tipis nyaris tak kelihatan; di 3-button
            // nav (masih umum Android 15 ke bawah) bar opaque menutupi konten — insets manual di
            // sini yang menutup gap-nya. Padding fixed 32dp tetap di bawah (jarak visual dari
            // konten ke insets), bukan pengganti.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            "SELAMAT DATANG",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "SONIX",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Untuk menampilkan koleksi musik kamu, aplikasi ini butuh izin membaca file audio di perangkat.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        WelcomeHighlight(Icons.Default.WifiOff, "Semua diproses di HP — tidak ada data yang dikirim ke internet")
        Spacer(modifier = Modifier.height(12.dp))
        WelcomeHighlight(Icons.Default.FolderOff, "Kamu yang atur folder mana yang tampil")
        Spacer(modifier = Modifier.height(12.dp))
        WelcomeHighlight(Icons.Default.GraphicEq, "Equalizer, crossfade, sampai widget — lengkap")

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Lanjutkan")
        }
    }
}

@Composable
private fun WelcomeHighlight(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

// Batch 437 — request eksplisit user: label NavigationBarItem bawah (Beranda/Perpustakaan/
// Pengaturan) diganti dari `Text("Beranda")` polos jadi composable ini — efek "kaca pembesar"
// ala iOS, ukuran/blur/opacity Text bereaksi KONTINU terhadap `focus` (0f..1f, lihat
// `tabMagnifyFocus` di AppNavHost) alih-alih cuma snap ON/OFF ikut boolean `selected`. `style`
// diturunkan dari `LocalTextStyle.current` (bukan style baru) — hanya `fontSize` yang
// di-override eksplisit, warna (selected/unselected, dianimasikan sendiri oleh M3 lewat
// LocalContentColor) & sisanya (letterSpacing/lineHeight/fontWeight) TETAP ikut identitas tema
// aktif apa pun (Apple/Tactile/SkeuDarkLite/LiquidGlass/dst) — 0 hardcode warna baru. `blur()`
// dipakai tanpa percabangan Build.VERSION: minSdk project ini sudah 31, RenderEffect (dasar
// Modifier.blur di Compose) tersedia sejak API 31.
@Composable
private fun MagnifyingTabLabel(text: String, focus: Float) {
    val clampedFocus = focus.coerceIn(0f, 1f)
    val baseStyle = LocalTextStyle.current
    Text(
        text = text,
        maxLines = 1,
        style = baseStyle.copy(fontSize = baseStyle.fontSize * (1f + clampedFocus * 0.14f)),
        modifier = Modifier
            .graphicsLayer {
                val scale = 1f + clampedFocus * 0.08f
                scaleX = scale
                scaleY = scale
                alpha = 0.68f + 0.32f * clampedFocus
            }
            .blur(((1f - clampedFocus) * 1.3f).dp)
    )
}

// Batch 438 — request eksplisit user: lampiran `drag_drop_glass_ios_kotlin.md` + screenshot
// bottom nav, "hasil sebelumnya (Batch 437, efek kaca PEMBESAR di LABEL) mengecewakan ...
// adaptasi 100% berdasarkan panduan". Panduan asli = demo generik (bukan app ini): 3 tab
// custom draggable-reorder + `Modifier.blur(20.dp)` di container. 2 bagian TIDAK dipakai
// literal — bukan penolakan, adaptasi ke arsitektur riil (SOP §HIGH-RISK ADAPTABILITY):
//   1. Reorder drag-to-swap: 3 tab ini route top-level Nav Compose permanen
//      (home/library/settings — dipakai state-restoration Batch 301, gesture-swipe Batch 435,
//      NavigationRailItem tablet). Reorder mengubah pasangan ikon<->rute jadi tidak tetap,
//      breaking change jauh di luar scope "efek visual kaca" yang diminta (pola penolakan sama
//      persis swap-ke-HorizontalPager Batch 435). Interaksi drag guide diadaptasi jadi tap
//      press-scale saja (lihat `bouncyPress` di bawah) — gesture tap tetap 100% dipegang
//      `NavigationBarItem` sendiri, 0 `pointerInput` kustom baru (kalau dipasang akan bersaing
//      gesture dgn klik pindah tab = risiko regresi persis yg diperingatkan SOP §HIGH-RISK).
//   2. `Modifier.blur(20.dp)` di container: PERSIS anti-pattern yang didokumentasikan sendiri
//      oleh proyek ini di `BlurUtils.kt` (blur() mengaburkan KONTEN sendiri, ikon/teks di
//      dalamnya ikut buram — bukan "kaca" yg dimaksud), DAN blur asli (Haze/`hazeEffect`) sudah
//      DIMATIKAN PERMANEN app-wide (Batch 329, root cause: stutter musik device asli — keputusan
//      eksplisit user, tidak diaktifkan ulang batch ini). Diadaptasi jadi translucent-tint +
//      border tipis (teknik sama `frostedGlass()`), TIDAK memanggil `frostedGlass()` langsung
//      karena shape-nya (`MaterialTheme.shapes.large`) + alpha-nya (0.92/0.96, disetel utk panel
//      besar/card/sheet supaya tetap kebaca TANPA blur asli di belakangnya) didesain utk panel
//      besar, bukan pill nav sekecil ini — dipakai versi lokal skala-pill di sini (0 perubahan
//      ke `BlurUtils.kt`/12+ call site lain, sesuai batas 3 file/tugas).
// Elemen guide yang DIPAKAI: translucent overlay tipis + border rim tipis (glass rim) + scale
// saat berinteraksi (guide: 1.08 saat drag-hold; di sini: `bouncyPress` 0.9 saat tap-press,
// konvensi tekan-tactile yg SUDAH dipakai LockScreen/MiniPlayerBar/dst — bukan sistem baru).
// `isSkeuTheme()` DIKECUALIKAN dari efek glass ini — identitas ini punya aturan tegas sejak
// Batch 58/61/79 "panel solid, bukan lapisan kaca, 0 garis tepi apa pun", berlaku app-wide (0
// spesifik ke nav) — pill Skeu tetap solid (indicator M3 default look, direplikasi manual krn
// `indicatorColor` M3 dimatikan/transparent di titik pemakaian, lihat `AppNavHost`).
// Utk 5 identitas lain: catatan lama Batch 53 "§15 jangan jadikan nav item glowing glass
// capsule" DISUPERSEDE eksplisit oleh instruksi user batch ini (kaskade DESCENDING TRUTH SOP:
// instruksi eksplisit baru > catatan/spec lama) — didokumentasikan di PROJECT_STATE.md/README.md,
// bukan dihapus diam-diam.
// Batch 439 — permintaan eksplisit user: 2 screenshot referensi (nav app ini vs tab bar iOS
// Jam/Clock) + instruksi "perbaiki bottom nav bar agar lebih mirip gaya visual iOS app jam
// tersebut, matikan ripple khas Android saat klik". Dibanding referensi iOS Jam, gap utama pill
// Batch 438 (di atas) cuma membungkus IKON — di iOS Jam, highlight tab aktif membungkus IKON+
// LABEL sekaligus jadi satu blok. `GlassTabIcon` diperluas ambil alih slot label juga (param
// `label`/`focus` baru, dipanggil balik ke `MagnifyingTabLabel` yang SAMA PERSIS, 0 logic
// pembesar Batch 437 diubah) lalu 3 titik pemakaian di bawah (`icon = { GlassTabIcon(...) }`)
// melepas parameter `label = { MagnifyingTabLabel(...) }` milik `NavigationBarItem` (M3 selalu
// naruh label itu di SLOT terpisah di bawah ikon, tidak bisa disatukan ke 1 pill dari luar
// composable-nya) — 0 breaking ke `NavigationBarItem` sendiri, cuma pindah tempat rendernya.
// Bentuk pill juga diganti dari stadium penuh (`percent = 50`, cocok utk lingkaran-ikon-saja)
// jadi `RoundedCornerShape(16.dp)` — kotak rounded, sama seperti referensi iOS Jam yang
// membungkus blok ikon+teks (stadium penuh di blok setinggi itu akan terlihat seperti kapsul
// obat, bukan seperti referensi). `NavigationRailItem` (tablet) TIDAK disentuh — tidak dipakai
// `GlassTabIcon` sama sekali (lihat definisinya di `AppNavHost`, pakai `Icon`/`Text` polos), di
// luar scope 2 screenshot yang keduanya nav ponsel.
@Composable
private fun GlassTabIcon(
    icon: ImageVector,
    label: String,
    focus: Float,
    selected: Boolean,
    interactionSource: MutableInteractionSource
) {
    val isSkeu = isSkeuTheme()
    // Cross-fade kontinu (bukan snap ON/OFF) — pill kaca menyala/meredup halus mengikuti
    // transisi selected, pola animasi sama (tween) yang sudah dipakai transisi NavHost (Batch
    // 330, 200/150ms) supaya "rasa" transisi tetap konsisten satu app.
    // Batch 440 — request eksplisit user: adaptasi behavior dari panduan
    // `drag_drop_glass_ios_kotlin.md` (dilampirkan ulang) — kapsul & warna ikon di referensi
    // bertransisi MENGIKUTI PERSENTASE GESER JARI SECARA LANGSUNG (`pageOffsetFraction`
    // HorizontalPager), bukan cuma snap ikut boolean `selected` setelah tab commit. Arsitektur
    // riil app ini TETAP permanent NavHost routes (bukan HorizontalPager — lihat rasionalisasi
    // Batch 438 di atas, reorder/pager sengaja tidak dipakai literal), tapi app ini SUDAH punya
    // padanan persis `pageOffsetFraction` guide: `focus` (param di atas, dihitung live tiap
    // frame selama drag oleh `tabMagnifyFocus`, Batch 435/437 — 1f di tab aktif idle, turun ke
    // 0f digeser menjauh, tab tetangga naik 0f→1f digeser mendekat). Target
    // `animateFloatAsState` diganti dari `if (selected) 1f else 0f` (statis, cuma bereaksi
    // setelah commit) jadi `focus` langsung — idle-nya SAMA PERSIS 1f/0f seperti sebelumnya (0
    // regresi tap, tween 220ms yang sama tetap jalan sbg smoothing), bedanya sekarang capsule
    // ini juga ikut bereaksi kontinu selama jari masih menggeser, persis seperti referensi.
    val glassAlpha by animateFloatAsState(
        targetValue = focus,
        animationSpec = tween(220),
        label = "GlassTabIndicatorAlpha"
    )
    val pillShape = RoundedCornerShape(16.dp)
    val tint = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .widthIn(min = 64.dp)
            .then(
                if (isSkeu) {
                    // Skeu: 0 kaca, replikasi manual solid pill M3 default (indicatorColor
                    // dimatikan/transparent di titik pemakaian supaya 1 composable ini jadi
                    // SATU-SATUNYA penggambar indicator, konsisten lintas identitas).
                    if (selected) Modifier.background(MaterialTheme.colorScheme.secondaryContainer, pillShape)
                    else Modifier
                } else {
                    Modifier
                        .background(tint.copy(alpha = 0.16f * glassAlpha), pillShape)
                        .border(1.dp, Color.White.copy(alpha = 0.14f * glassAlpha), pillShape)
                }
            )
            .bouncyPress(interactionSource, pressedScale = 0.9f)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Batch 440 — elemen guide yang BELUM diadaptasi batch-batch sebelumnya: ikon sendiri
        // (bukan cuma pill di belakangnya) ikut `lerp` warna kontinu, teknik PERSIS
        // `androidx.compose.ui.graphics.lerp` di guide. Titik awal `unselectedIconColor` =
        // token M3 resmi (`NavigationBarItemDefaults`, IDENTIK dgn default lama sebelum batch
        // ini — 0 hardcode warna baru, ikut identitas tema aktif apa pun, konsisten dgn aturan
        // Batch 437 §warna). Titik akhir = `tint` (primary) yang sama dgn aksen pill di atas,
        // supaya ikon & pill bergerak sebagai 1 aksen, bukan 2 warna lepas. Skeu DIKECUALIKAN
        // (aturan "solid, bukan kaca" Batch 58/61/79, app-wide) — tetap tint default M3 apa
        // adanya, 0 lerp.
        if (isSkeu) {
            Icon(icon, contentDescription = null)
        } else {
            val unselectedIconColor = NavigationBarItemDefaults.colors().unselectedIconColor
            Icon(icon, contentDescription = null, tint = lerp(unselectedIconColor, tint, glassAlpha))
        }
        MagnifyingTabLabel(label, focus)
    }
}

// Batch 439 — 0 dampak ke `selected`/klik: `Indication` kosong (cuma `drawContent()`, 0 layer
// visual digambar) dipasang lewat `CompositionLocalProvider(LocalIndication provides ...)` di
// `bottomBar`, BUKAN `Modifier.clickable` baru. `NavigationBarItem` (M3) membaca ripple-nya dari
// `LocalIndication.current` secara internal persis seperti komponen selectable/clickable
// Compose Foundation lain — override di titik pemakaian ini cukup, 0 perlu sentuh
// `NavigationBarItem`/`selected`/route logic sama sekali. Efek gelombang ripple bawaan Android
// hilang, tapi warna & scale-down `bouncyPress` (Batch 438) di `GlassTabIcon` tetap jalan penuh
// (2 mekanisme feedback tekan yang independen) — cocok dengan referensi iOS Jam yang 0 ripple
// tapi tetap ada feedback visual saat tab ditekan.
// Batch 440 — implementasi kontrak `Indication`/`IndicationInstance` (`rememberUpdatedInstance`
// + objek `drawIndication()`) di atas kini HARD ERROR compiler (bukan lagi cuma deprecated
// warning) di compose-bom 2026.04.01 — `compileDebugKotlin`/`compileReleaseKotlin` FAILED,
// lihat `log_fail_424.zip`. Migrasi ke kontrak resmi pengganti (`IndicationNodeFactory` +
// `Modifier.Node`/`DrawModifierNode`) — 0 behavior berubah, MASIH murni `drawContent()` kosong,
// 0 layer visual digambar, titik pemakaian `CompositionLocalProvider(LocalIndication provides
// NoRippleIndication)` di `bottomBar` TIDAK disentuh (tetap kompatibel — `IndicationNodeFactory`
// adalah subtipe `Indication`).
private object NoRippleIndication : IndicationNodeFactory {
    private class NoRippleIndicationNode : Modifier.Node(), DrawModifierNode {
        override fun ContentDrawScope.draw() {
            drawContent()
        }
    }

    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return NoRippleIndicationNode()
    }
}

@Composable
private fun AppNavHost(playerViewModel: PlayerViewModel, biometricAvailable: Boolean) {
    val navController = rememberNavController()
    // Batch 295 — fase 5 langkah 1 ("fondasi plumbing"), lihat LIQUID_GLASS_BLUR_ENGINE_DESIGN.md
    // §3a. 1 HazeState dipegang di sini (bukan per-layar), diteruskan ke seluruh NavHost/
    // MiniPlayerBar via LocalHazeState (Theme.kt) supaya tidak perlu parameter baru di 20+
    // file. 0 consumer sama sekali batch ini (belum ada .hazeSource()/.hazeEffect() dipasang
    // di manapun) — murni plumbing, 0 perubahan visual.
    val hazeState = rememberHazeState()
    // Batch 326 sempat menambah `auroraPhaseTransition`/`auroraPhase` di sini (1
    // `rememberInfiniteTransition` dibagi ke semua `frostedGlass()` rim-glow lewat
    // `LocalAuroraPhase`). Batch 328 MENGHAPUS BALIK — user (device sungguhan): musik
    // stuttering/mandek + lag/glitch saat swipe sheet "Kontrol Lanjutan". Asumsi Batch 326
    // ("1 instance dibagi = aman") terbukti keliru: phase berubah tiap frame tetap memicu
    // recomposition brush di semua consumer sekaligus, termasuk `MiniPlayerBar` yang selalu
    // tervisible selama musik main — bersaing langsung dgn thread audio/UI. Rasionalisasi penuh:
    // `PROJECT_STATE.md`/`CHANGELOG.md` Batch 328, komentar Aurora branch `frostedGlass()`
    // (BlurUtils.kt).
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val favoriteIds by playerViewModel.favoriteIds.collectAsStateWithLifecycle()
    // Batch 354 — sleepTimerRemaining SENGAJA TIDAK dikoleksi di sini lagi (dulu `by ...
    // collectAsStateWithLifecycle()` persis di scope teratas ini, pola identik bug
    // position/duration Batch 353). StateFlow-nya sendiri dioper mentah ke NowPlayingScreen di
    // bawah, dikoleksi lokal di SleepTimerDialog/AdvancedControlsSheet.
    val abRepeatPointA by playerViewModel.abRepeatPointA.collectAsStateWithLifecycle()
    val abRepeatPointB by playerViewModel.abRepeatPointB.collectAsStateWithLifecycle()
    val statsVersion by playerViewModel.statsVersion.collectAsStateWithLifecycle()
    val playlists by playerViewModel.playlists.collectAsStateWithLifecycle()
    val smartPlaylists by playerViewModel.smartPlaylists.collectAsStateWithLifecycle()
    val accentColor by playerViewModel.accentColor.collectAsStateWithLifecycle()
    val equalizerState by playerViewModel.equalizerState.collectAsStateWithLifecycle()
    val crossfadeEnabled by playerViewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val customFolders by playerViewModel.customFolders.collectAsStateWithLifecycle()
    val librarySongs by playerViewModel.librarySongs.collectAsStateWithLifecycle()
    val libraryLoading by playerViewModel.libraryLoading.collectAsStateWithLifecycle()
    val celebrationMessage by playerViewModel.celebrationMessage.collectAsStateWithLifecycle()
    val playbackErrorMessage by playerViewModel.playbackErrorMessage.collectAsStateWithLifecycle()
    val actionErrorMessage by playerViewModel.actionErrorMessage.collectAsStateWithLifecycle()
    val undoableAction by playerViewModel.undoableAction.collectAsStateWithLifecycle()
    val infoMessage by playerViewModel.infoMessage.collectAsStateWithLifecycle()
    val currentRating by playerViewModel.currentRating.collectAsStateWithLifecycle()
    val lockEnabled by playerViewModel.lockEnabled.collectAsStateWithLifecycle()
    val biometricEnabled by playerViewModel.biometricEnabled.collectAsStateWithLifecycle()
    val shakeToSkipEnabled by playerViewModel.shakeToSkipEnabled.collectAsStateWithLifecycle()
    val radioAutoContinueEnabled by playerViewModel.radioAutoContinueEnabled.collectAsStateWithLifecycle()
    val appThemeIdentity by playerViewModel.themeIdentity.collectAsStateWithLifecycle()
    val visualizerEnabled by playerViewModel.visualizerEnabled.collectAsStateWithLifecycle()
    val visualizerSupported by playerViewModel.visualizerSupported.collectAsStateWithLifecycle()
    // Batch 354 — visualizerBars sama, dihapus dari sini (dulu `by ...
    // collectAsStateWithLifecycle()` di scope ini, ~15fps + tembus 4 layer composable turunan —
    // kandidat kuat kenapa lag masih terasa meski Batch 353 sudah jalan). StateFlow mentah dioper
    // ke NowPlayingScreen, dikoleksi lokal di SpectrumBars (VisualizerSheet.kt).
    val audiobookModeEnabled by playerViewModel.audiobookModeEnabled.collectAsStateWithLifecycle()
    val floatingBubbleEnabled by playerViewModel.floatingBubbleEnabled.collectAsStateWithLifecycle()
    val silenceSkipEnabled by playerViewModel.silenceSkipEnabled.collectAsStateWithLifecycle()

    val deleteContext = LocalContext.current

    // Batch 92 (Roadmap #9, Visualizer Audio) — RECORD_AUDIO is a dangerous permission (API 23+),
    // deliberately requested here on-demand (only when the user turns the Visualizer on inside
    // its own sheet) rather than folded into the mandatory onboarding flow above — an optional
    // visual effect asking for a microphone-sounding permission at first launch would be a real
    // privacy/UX overreach for a feature most people will never open.
    val visualizerPermissionContext = LocalContext.current
    var visualizerPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(visualizerPermissionContext, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val visualizerPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        visualizerPermissionGranted = granted
        // Granting here means the user just tapped "on" wanting the visualizer active right now
        // — flip it on immediately instead of making them go tap the switch a second time.
        if (granted) playerViewModel.setVisualizerEnabled(true)
    }
    // Roadmap #11, Floating Mini Player — SYSTEM_ALERT_WINDOW BUKAN runtime permission dialog
    // biasa (tidak ada callback granted/denied yang bisa diandalkan lintas OEM dari hasil
    // Activity-nya sendiri) — pola yang benar adalah buka layar sistem lalu cek ulang langsung
    // ke Settings.canDrawOverlays() begitu user kembali, bukan percaya result code seperti
    // visualizerPermissionLauncher di atas.
    val overlayPermissionContext = LocalContext.current

    // Batch 98 — FloatingBubbleService sekarang foreground service beneran (lihat KDoc kelasnya)
    // dan MEMANGGIL startForeground() SENDIRI di onCreate(), jadi caller wajib pakai
    // startForegroundService() (bukan startService() biasa) di Android O+ — pola yang sama
    // persis sudah dipakai di PlaybackService/FloatingBubbleService's sendPlaybackAction
    // fallback, sekarang disatukan di 1 helper biar tidak diketik ulang 3x di bawah.
    fun startBubbleService(context: android.content.Context) {
        val intent = Intent(context, FloatingBubbleService::class.java)
        context.startForegroundService(intent)
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(overlayPermissionContext)) {
            playerViewModel.setFloatingBubbleEnabled(true)
            startBubbleService(overlayPermissionContext)
        }
        // Ditolak/dibatalkan: toggle di SettingsScreen tetap OFF (floatingBubbleEnabled tidak
        // pernah diset true di sini), tidak perlu penanganan tambahan.
    }

    fun toggleFloatingBubble(enabled: Boolean) {
        if (!enabled) {
            playerViewModel.setFloatingBubbleEnabled(false)
            overlayPermissionContext.stopService(Intent(overlayPermissionContext, FloatingBubbleService::class.java))
            return
        }
        if (Settings.canDrawOverlays(overlayPermissionContext)) {
            playerViewModel.setFloatingBubbleEnabled(true)
            startBubbleService(overlayPermissionContext)
        } else {
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${overlayPermissionContext.packageName}")
                )
            )
        }
    }

    // Restart bubble sekali per proses kalau sesi SEBELUMNYA menyalakannya dan izin masih ada
    // (proses baru = Service lama ikut mati, START_STICKY tidak menolong lintas proses baru).
    // Batch 98: ini kini cuma jaring pengaman tambahan — BubbleBootReceiver sudah menutup celah
    // "HP restart, app belum dibuka" lebih dulu; effect ini tetap perlu untuk kasus proses mati
    // TANPA reboot (mis. app di-force-stop manual, atau OOM kill lalu user buka app lagi).
    LaunchedEffect(Unit) {
        if (floatingBubbleEnabled && Settings.canDrawOverlays(overlayPermissionContext)) {
            startBubbleService(overlayPermissionContext)
        }
    }

    // Batch 100 — bubble sekarang bisa ditoggle dari LUAR app sepenuhnya (Quick Settings Tile,
    // lihat BubbleTileService.kt — baca/tulis langsung ke FloatingBubbleStore, tidak lewat
    // ViewModel StateFlow di atas sama sekali). Tanpa ini, switch bubble di SettingsScreen bisa
    // nunjukin state BASI kalau user toggle dari tile lalu balik ke app yang masih hidup di
    // background (StateFlow tidak auto-observe SharedPreferences dari komponen lain). Re-sync
    // tiap ON_RESUME — observer manual (bukan LifecycleEventEffect) sengaja dipilih: proyek ini
    // pernah kena masalah nyata soal LocalLifecycleOwner CompositionLocal (lihat CHANGELOG Batch
    // 23-24), addObserver() manual adalah API lifecycle polos yang tidak lewat titik gagal itu.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                playerViewModel.refreshFloatingBubbleEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // On success the system has already deleted the files; just refresh our own scan
        // so they disappear from the library too. On cancel, nothing was deleted — no-op.
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            playerViewModel.refreshLibrary()
        }
    }

    // Gap List "Wajib" #1 (Tag Editor) — pola identik deleteRequestLauncher di atas, untuk
    // dialog izin tulis MediaStore.createWriteRequest (Android 11+). ViewModel yang simpan
    // lagu/tag yang tertunda; launcher ini cuma jembatan Activity-result → hasil boolean.
    val tagWriteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        playerViewModel.onTagWriteConsentResult(result.resultCode == android.app.Activity.RESULT_OK)
    }
    val pendingTagWriteConsent by playerViewModel.pendingTagWriteConsent.collectAsStateWithLifecycle()
    LaunchedEffect(pendingTagWriteConsent) {
        pendingTagWriteConsent?.let { sender ->
            tagWriteConsentLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    fun deleteSongsFromDevice(songs: List<com.rudi.audioplayer.data.Song>) {
        if (songs.isEmpty()) return
        val resolver = deleteContext.contentResolver
        val uris = songs.map { it.uri }
        // minSdk 31 (Android 12) guarantees SDK_INT >= R (30) unconditionally — this used to be
        // the first branch of a `when` whose Q/else branches were shadowed dead code (R always
        // matched first, not because Q/pre-Q were individually below minSdk). The only path any
        // device on this app can actually take: the system shows its own confirmation and
        // handles the actual deletion; we never touch the files directly.
        val pendingIntent = android.provider.MediaStore.createDeleteRequest(resolver, uris)
        deleteRequestLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Batch 435 — request eksplisit user: "tambahkan gesture swipe able lintas 3 tab, alih-alih
    // user hanya bisa tap-tab manual berulang". App ini pakai Jetpack Navigation Compose dengan
    // 3 route top-level TERPISAH (home/library/settings, bukan HorizontalPager 1-route) — swipe
    // di sini DIDETEKSI lalu memicu navController.navigate() yang SAMA PERSIS dgn onClick
    // NavigationBarItem/NavigationRailItem di bawah (popUpTo("home"){saveState=true} +
    // launchSingleTop + restoreState), jadi 0 perubahan ke state-preservation tab yang sudah ada.
    // enter/exitTransition NavHost (Batch 330, fade 200/150ms) SENGAJA tidak disentuh — tetap
    // dipakai apa adanya baik utk tap maupun swipe. Pola threshold 120px + haptic +
    // dragOffsetPx(sinkron)/Animatable(springback-only) di bawah REUSE 1:1 dari AlbumArtHero
    // (NowPlayingScreen.kt, Batch 434) — sumbu bug sinkron-vs-asinkron yang sama sengaja tidak
    // diulang di sini. Modifier gesture-nya sendiri HANYA dipasang saat currentRoute ada di
    // TAB_ROUTES (lihat Box pembungkus NavHost di bawah) supaya 0 rebutan dgn gesture horizontal
    // lain yang sudah ada di layar non-tab (mis. swipe next/prev AlbumArtHero di "now_playing").
    val tabSwipeHaptic = LocalHapticFeedback.current
    val tabSwipeScope = rememberCoroutineScope()
    val tabDragOffsetPx = remember { mutableFloatStateOf(0f) }
    val tabDragOffset = remember { Animatable(0f) }

    // Batch 437 — request eksplisit user: efek "kaca pembesar" ala iOS di label bawah, bereaksi
    // kontinu mengikuti gesture drag yang SAMA PERSIS dgn Batch 435 (0 gesture/state baru) —
    // `tabDragOffsetPx` di atas SUDAH live ter-update tiap frame selama `onHorizontalDrag`
    // (±40px, lihat Box pembungkus NavHost di bawah) dan sudah spring-back ke 0 di `onDragEnd`/
    // `onDragCancel`; fungsi ini murni MEMBACA ulang sinyal itu sebagai bobot fokus per-tab, 0
    // pointerInput/Animatable kedua. Dipanggil di titik pemakaian (dalam tiap `label = { ... }`
    // NavigationBarItem, bukan di-hoist ke NavigationBar) SENGAJA — scope recomposition tetap
    // sekecil mungkin (hanya Text label yang berubah tiap frame drag, bukan seluruh NavigationBar).
    fun tabMagnifyFocus(tabIndex: Int): Float {
        val fromIdx = TAB_ROUTES.indexOfFirst { it == currentRoute }
        if (fromIdx < 0) return 0f
        val offset = tabDragOffsetPx.floatValue // -40f..40f; negatif = geser ke arah tab BERIKUTNYA
        val towardNext = (-offset / 40f).coerceIn(0f, 1f)
        val towardPrev = (offset / 40f).coerceIn(0f, 1f)
        return when (tabIndex) {
            fromIdx -> 1f - maxOf(towardNext, towardPrev)
            fromIdx + 1 -> towardNext
            fromIdx - 1 -> towardPrev
            else -> 0f
        }
    }

    // Batch 101 — Adaptive (multi-device). widthClass dihitung dari LocalConfiguration, jadi
    // otomatis berubah live saat rotasi/lipat-buka foldable/resize split-screen — TIDAK perlu
    // di-remember manual. showTwoPane sengaja exclude currentRoute == "now_playing" supaya
    // panel kanan tidak dobel dengan layar penuh Now Playing kalau user tetap memaksa navigasi
    // ke sana (mis. lewat deep link) selagi di lebar Expanded.
    val widthClass = rememberAppWidthClass()
    val showTwoPane = widthClass == AppWidthClass.EXPANDED &&
        uiState.currentSong != null &&
        currentRoute != "now_playing"

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(celebrationMessage) {
        val message = celebrationMessage ?: return@LaunchedEffect
        try {
            snackbarHostState.showSnackbar(message)
        } finally {
            playerViewModel.consumeCelebrationMessage()
        }
    }

    LaunchedEffect(playbackErrorMessage) {
        val message = playbackErrorMessage ?: return@LaunchedEffect
        try {
            snackbarHostState.showSnackbar(message)
        } finally {
            playerViewModel.consumePlaybackErrorMessage()
        }
    }

    LaunchedEffect(actionErrorMessage) {
        val message = actionErrorMessage ?: return@LaunchedEffect
        try {
            snackbarHostState.showSnackbar(message)
        } finally {
            playerViewModel.consumeActionErrorMessage()
        }
    }

    LaunchedEffect(infoMessage) {
        val message = infoMessage ?: return@LaunchedEffect
        try {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        } finally {
            playerViewModel.consumeInfoMessage()
        }
    }

    LaunchedEffect(undoableAction) {
        val action = undoableAction ?: return@LaunchedEffect
        try {
            val result = snackbarHostState.showSnackbar(
                message = action.message,
                actionLabel = "Urungkan",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                action.undo()
            }
        } finally {
            playerViewModel.consumeUndoableAction()
        }
    }

    // Batch 101 — Adaptive (multi-device). Body NowPlayingScreen() yg sebelumnya inline persis
    // sekali di composable("now_playing") sekarang dibungkus 1 lambda dgn parameter `onBack`,
    // dipakai DUA tempat: (1) composable("now_playing") seperti biasa di Compact/Medium dgn
    // onBack = popBackStack(), (2) panel persisten kanan di Expanded (lihat showTwoPane) dgn
    // onBack = no-op — panel BUKAN entry back-stack, popBackStack() di situ justru akan keluar
    // dari layar kiri (Home/Library) yg sedang tampil, bukan menutup panel. 0 parameter lain yg
    // berubah, isi NowPlayingScreen(...) copy persis dari sebelumnya.
    val nowPlayingContent: @Composable (onBack: () -> Unit) -> Unit = { onBackAction ->
        NowPlayingScreen(
            uiState = uiState,
            playbackProgress = playerViewModel.playbackProgress,
            isFavorite = uiState.currentSong?.let { favoriteIds.contains(it.id) } ?: false,
            currentRating = currentRating,
            onSetRating = { stars -> playerViewModel.setCurrentSongRating(stars) },
            sleepTimerRemaining = playerViewModel.sleepTimerRemaining,
            accentColor = accentColor,
            onPlayPause = { playerViewModel.togglePlayPause() },
            onNext = { playerViewModel.next() },
            onPrevious = { playerViewModel.previous() },
            onSeek = { playerViewModel.seekTo(it) },
            onShuffle = { playerViewModel.toggleShuffle() },
            onRepeat = { playerViewModel.cycleRepeatMode() },
            onToggleFavorite = { uiState.currentSong?.let { playerViewModel.toggleFavorite(it.id) } },
            onSetSleepTimer = { playerViewModel.setSleepTimer(it) },
            onCancelSleepTimer = { playerViewModel.cancelSleepTimer() },
            onSetSpeed = { playerViewModel.setPlaybackSpeed(it) },
            crossfadeEnabled = crossfadeEnabled,
            onSetCrossfadeEnabled = { playerViewModel.setCrossfadeEnabled(it) },
            onSetVolume = { playerViewModel.setVolume(it) },
            onPlayQueueIndex = { playerViewModel.playFromQueueIndex(it) },
            onMoveQueueItem = { from, to -> playerViewModel.moveQueueItem(from, to) },
            onRemoveFromQueue = { playerViewModel.removeFromQueue(it) },
            onGetLyrics = { id -> playerViewModel.getLyrics(id) },
            onSaveLyrics = { id, text -> playerViewModel.saveLyrics(id, text) },
            onDeleteLyrics = { id -> playerViewModel.deleteLyrics(id) },
            abRepeatPointA = abRepeatPointA,
            abRepeatPointB = abRepeatPointB,
            onSetAbRepeatPointA = { playerViewModel.setAbRepeatPointA(it) },
            onSetAbRepeatPointB = { playerViewModel.setAbRepeatPointB(it) },
            onClearAbRepeat = { playerViewModel.clearAbRepeat() },
            onGetBookmarks = { id -> playerViewModel.getBookmarks(id) },
            onAddBookmark = { id, label, positionMs -> playerViewModel.addBookmark(id, label, positionMs) },
            onDeleteBookmark = { id, bookmarkId -> playerViewModel.deleteBookmark(id, bookmarkId) },
            equalizerState = equalizerState,
            onOpenEqualizer = { playerViewModel.ensureEqualizerAttached() },
            onToggleEqualizerEnabled = { playerViewModel.setEqualizerEnabled(it) },
            onEqualizerBandChange = { band, level -> playerViewModel.setEqualizerBand(band, level) },
            onEqualizerPresetSelect = { index -> playerViewModel.useEqualizerPreset(index) },
            onEqualizerBoldPresetSelect = { preset -> playerViewModel.useBoldEqualizerPreset(preset) },
            audiobookModeEnabled = audiobookModeEnabled,
            onToggleAudiobookMode = { playerViewModel.setAudiobookModeEnabled(it) },
            visualizerEnabled = visualizerEnabled,
            visualizerSupported = visualizerSupported,
            visualizerPermissionGranted = visualizerPermissionGranted,
            visualizerBars = playerViewModel.visualizerBars,
            onOpenVisualizer = { playerViewModel.ensureVisualizerAttached() },
            onCloseVisualizer = { playerViewModel.stopVisualizerCapture() },
            onToggleVisualizerEnabled = { playerViewModel.setVisualizerEnabled(it) },
            onRequestVisualizerPermission = { visualizerPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onSaveSongTags = { song, tags -> playerViewModel.requestSaveTags(song, tags) },
            onCutRingtone = { song, range, destination, label ->
                playerViewModel.requestCutRingtone(song, range, destination, label)
            },
            // Batch 358 — ikon "Plus" baru di Now Playing (samping judul). Sama persis 3 baris
            // yang sudah dipakai LibraryScreen (di atas, ~line 1153-1157), playlists StateFlow-nya
            // pun sudah dikoleksi dari playerViewModel lebih awal di fungsi ini (val playlists),
            // 0 sumber data baru.
            playlists = playlists,
            onAddSongToPlaylist = { id, songId -> playerViewModel.addSongToPlaylist(id, songId) },
            onCreatePlaylist = { name -> playerViewModel.createPlaylist(name) },
            onBack = onBackAction
        )
    }

    // Batch 295 — bungkus Scaffold (bukan cuma pasang provider di root Compose tree lebih
    // atas) supaya scope-nya jelas 1 titik, sama gaya CompositionLocalProvider yang SUDAH ADA
    // di file ini (Batch 24, baris ~210) — badan blok TIDAK di-reindent (pola sama persis
    // Batch 24: minim-diff, bukan reformat besar-besaran demi estetika indentasi).
    CompositionLocalProvider(LocalHazeState provides hazeState) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    action = data.visuals.actionLabel?.let { label ->
                        {
                            TextButton(onClick = { data.performAction() }) {
                                Text(label, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                ) {
                    Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        bottomBar = {
            Column {
                AnimatedVisibility(
                    // Fix bug (user screenshot: Now Playing screen showed a redundant floating
                    // mini player bar overlapping the full screen's own controls below it) —
                    // this condition only checked `currentSong != null`, with no route check at
                    // all, unlike the NavigationBar condition right below it which correctly
                    // excludes "now_playing". So the mini player kept rendering even while the
                    // user was already ON the Now Playing screen, duplicating the play/pause
                    // control and crowding the transport row beneath it — the actual root cause
                    // of "hierarki tombol nya terlalu membingungkan".
                    visible = uiState.currentSong != null && currentRoute != "now_playing" && !showTwoPane,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    MiniPlayerBar(
                        uiState = uiState,
                        playbackProgress = playerViewModel.playbackProgress,
                        accentColor = accentColor,
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onExpand = {
                            navController.navigate("now_playing") { launchSingleTop = true }
                        }
                    )
                }
                if (widthClass == AppWidthClass.COMPACT &&
                    (currentRoute == "home" || currentRoute == "library" || currentRoute == "settings")
                ) {
                    // Batch 40: tonalElevation alone still reads flat (no directional light) —
                    // a 1-2px catch-light line along the top edge is the same border cue
                    // tactileEmboss() uses elsewhere, applied here without restructuring
                    // NavigationBar's own internals (it's a whole M3 component, not a bare
                    // Surface tactileEmboss() could wrap directly).
                    // Batch 53 — spec §15 "Navigation should be calm... Do not turn every
                    // navigation item into a glowing glass capsule" + §5 GlassHighlight is now
                    // 0.065f (was 0.055f pre-Batch-53), so the catch-light line's own alphas are
                    // re-matched to that new base (0.13f/0.03f) to keep the same relative
                    // brightness step it always had.
                    // Batch 57: the catch-light line + raised tonalElevation is a "physical
                    // panel" cue, not Tactile-specific — Skeuomorphism Dark Lite is the same
                    // kind of identity (raised surface catching light from top-left) just with
                    // its own warmer highlight token, so it gets the same treatment here with
                    // SkeuHighlight instead of TactileHighlight. Apple/Light/Dark stay untouched.
                    val navCatchLightColor = when (appThemeIdentity) {
                        ThemeIdentity.TACTILE -> TactileHighlight
                        ThemeIdentity.SKEU_DARK_LITE -> SkeuHighlight
                        else -> null
                    }
                    // Batch 438 — 1 interactionSource per tab, dibagi ke NavigationBarItem (klik)
                    // dan GlassTabIcon (bouncyPress) supaya keduanya sepakat kapan "pressed" true,
                    // pola identik `PinKey`/`RoundGlyphButton` (LockScreen.kt).
                    val homeTabInteraction = remember { MutableInteractionSource() }
                    val libraryTabInteraction = remember { MutableInteractionSource() }
                    val settingsTabInteraction = remember { MutableInteractionSource() }
                    NavigationBar(
                        // Batch 439 — referensi iOS Jam: bar bawah bukan persegi nempel penuh
                        // ke tepi layar, tapi kapsul rounded yang "mengambang" dengan jarak dari
                        // 3 tepi (kiri/kanan/bawah). `NavigationBar` M3 sendiri 0 parameter
                        // `shape` publik (Surface internalnya default persegi) — `.clip(...)`
                        // dipasang LANGSUNG di modifier terluar composable ini, cukup untuk
                        // membulatkan render akhirnya tanpa bongkar internal M3. `.padding(...)`
                        // WAJIB di LUAR `.clip(...)` (urutan modifier menentukan) supaya jaraknya
                        // benar-benar "di luar" kapsul (margin), bukan padding konten di DALAM
                        // kapsul yang sudah dibulatkan. `windowInsets` bawaan NavigationBar (utk
                        // gesture-nav Android) TIDAK disentuh — margin 12.dp di bawah ini
                        // tambahan DI ATAS inset itu, bukan pengganti, jadi 0 risiko kapsul
                        // ketutup gesture bar.
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .then(
                                if (navCatchLightColor != null)
                                    Modifier.drawBehind {
                                        drawLine(
                                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                listOf(
                                                    navCatchLightColor.copy(alpha = 0.13f),
                                                    navCatchLightColor.copy(alpha = 0.03f)
                                                )
                                            ),
                                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                            strokeWidth = 2f
                                        )
                                    }
                                else Modifier
                            ),
                        // Batch 53: lowered from 12.dp — spec §15 keeps navigation "calm and
                        // immediately understandable" and explicitly warns against every item (or
                        // in this case, the whole bar) reading as an accent-tinted glow. M3's
                        // tonalElevation overlay scales with elevation and this app's
                        // surfaceTint is the accent color (Theme.kt), so 12.dp let the bar itself
                        // read as "blue" before "glass" — 6.dp keeps a legible elevated-glass lift
                        // (Level 2, spec §4) without the accent wash dominating the one piece of
                        // chrome that's always on screen. Batch 57: Skeu shares this same 6.dp —
                        // same reasoning (SkeuAccent as surfaceTint would otherwise dominate).
                        tonalElevation = if (navCatchLightColor != null) 6.dp else NavigationBarDefaults.Elevation
                    ) {
                        // Batch 439 — bungkus 3 NavigationBarItem dgn Indication kosong
                        // (`NoRippleIndication`, definisi di atas dekat `GlassTabIcon`) supaya
                        // ripple gelombang Android bawaan mati di titik pemakaian ini SAJA
                        // (CompositionLocalProvider otomatis kembali ke ripple normal di luar
                        // scope ini — `NavigationRailItem` tablet, Button/TextButton lain di app
                        // 0 kesentuh). `selected`/`onClick`/route logic di bawah 0 diubah.
                        CompositionLocalProvider(LocalIndication provides NoRippleIndication) {
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = {
                                // Batch 301 — user melaporkan stuttering pas transisi antar tab
                                // (beda dari stutter SCROLL Batch 300 yang sudah dijawab lewat
                                // blurRadius). Root cause: `popUpTo`/`navigate` di 6 titik ini
                                // (3 NavigationBarItem + 3 NavigationRailItem, pola identik) 0
                                // pernah pakai `saveState`/`restoreState` — tiap tap tab
                                // MENGHANCURKAN TOTAL layar tujuan (LazyColumn state, scroll
                                // position, ViewModel scope) lalu membangunnya dari nol, bukan
                                // cuma pindah tampilan. Ini pola resmi Google utk bottom nav.
                                // `inclusive = true` di sini (khusus Home) juga dilepas — grep
                                // CHANGELOG/komentar lama 0 nemuin alasan terdokumentasi kenapa
                                // Home beda sendiri dari Library/Settings di bawah, jadi ini
                                // disamakan jadi 1 pola konsisten bertiga, bukan kasus khusus.
                                navController.navigate("home") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                // Batch 439 — `label` pindah ke dalam sini (dulu slot terpisah
                                // `label = { MagnifyingTabLabel(...) }` di bawah `icon`, lihat
                                // komentar Batch 439 di definisi `GlassTabIcon`).
                                GlassTabIcon(
                                    icon = Icons.Default.Home,
                                    label = "Beranda",
                                    focus = tabMagnifyFocus(0),
                                    selected = currentRoute == "home",
                                    interactionSource = homeTabInteraction
                                )
                            },
                            interactionSource = homeTabInteraction,
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                        )
                        NavigationBarItem(
                            selected = currentRoute == "library",
                            onClick = {
                                // Batch 301 — sama seperti onClick "home" di atas.
                                navController.navigate("library") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                // Batch 439 — sama seperti "home" di atas.
                                GlassTabIcon(
                                    icon = Icons.Default.LibraryMusic,
                                    label = "Perpustakaan",
                                    focus = tabMagnifyFocus(1),
                                    selected = currentRoute == "library",
                                    interactionSource = libraryTabInteraction
                                )
                            },
                            interactionSource = libraryTabInteraction,
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                        )
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = {
                                // Batch 301 — sama seperti onClick "home" di atas.
                                navController.navigate("settings") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                // Batch 439 — sama seperti "home" di atas.
                                GlassTabIcon(
                                    icon = Icons.Default.Settings,
                                    label = "Pengaturan",
                                    focus = tabMagnifyFocus(2),
                                    selected = currentRoute == "settings",
                                    interactionSource = settingsTabInteraction
                                )
                            },
                            interactionSource = settingsTabInteraction,
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                        )
                        } // tutup CompositionLocalProvider (Batch 439, NoRippleIndication)
                    }
                }
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Batch 101 — Adaptive: NavigationRail permanen di Medium/Expanded (tablet, foldable
            // terbuka, Chromebook, split-screen lebar) menggantikan NavigationBar bawah — hemat
            // tinggi layar & memanfaatkan ruang horizontal yang nganggur di layar lebar. Compact
            // (HP potret biasa) TIDAK tersentuh sama sekali, NavigationBar bawah tetap seperti
            // semula persis (lihat guard widthClass == COMPACT di atas).
            if (widthClass != AppWidthClass.COMPACT) {
                NavigationRail {
                    NavigationRailItem(
                        selected = currentRoute == "home",
                        onClick = {
                            // Batch 301 — mirror NavigationBarItem "home" fix di atas (layar
                            // Medium/Expanded pakai Rail ini, bukan NavigationBar bawah).
                            navController.navigate("home") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Beranda") }
                    )
                    NavigationRailItem(
                        selected = currentRoute == "library",
                        onClick = {
                            // Batch 301 — sama seperti onClick "home" Rail di atas.
                            navController.navigate("library") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                        label = { Text("Perpustakaan") }
                    )
                    NavigationRailItem(
                        selected = currentRoute == "settings",
                        onClick = {
                            // Batch 301 — sama seperti onClick "home" Rail di atas.
                            navController.navigate("settings") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Pengaturan") }
                    )
                }
            }
        val isOnTabRoute = TAB_ROUTES.any { it == currentRoute }
        Box(
            modifier = Modifier
                .weight(1f)
                // Batch 329 — `.hazeSource(state = hazeState)` (Batch 296) DILEPAS. Root cause:
                // user matikan `hazeEffect` permanen app-wide (BlurUtils.kt) krn ini + MiniPlayerBar
                // yang terus resample tiap frame selama musik main adalah biaya GPU yang sudah
                // diperingatkan sejak awal. Kalau capture ini dibiarkan terpasang tanpa 1 consumer
                // pun, tetap bayar sebagian besar biaya capture tsb demi 0 manfaat visual — jadi
                // dilepas juga, bukan cuma dinonaktifkan di sisi consumer. `hazeState`/
                // `CompositionLocalProvider(LocalHazeState)` di bawah SENGAJA TIDAK dibongkar —
                // reuse persis state Batch 295 (murni plumbing, 0 consumer), lihat rasionalisasi
                // penuh di BlurUtils.kt.
                // Batch 435 — nudge visual damped searah jari selama swipe (feedback "tergenggam",
                // bukan page-turn 1:1 — page-swap SEBENARNYA tetap lewat fade NavHost Batch 330
                // begitu navigate() terpicu, translationX ini cuma sinyal tangkapan gesture).
                .graphicsLayer { translationX = tabDragOffsetPx.floatValue }
                .then(
                    if (isOnTabRoute) {
                        Modifier.pointerInput(currentRoute) {
                            var totalTabDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    totalTabDrag = 0f
                                    // jaring pengaman sama seperti AlbumArtHero: hentikan
                                    // springback lama supaya tidak menimpa drag baru.
                                    tabSwipeScope.launch { tabDragOffset.stop() }
                                },
                                onDragEnd = {
                                    val fromIdx = TAB_ROUTES.indexOfFirst { it == currentRoute }
                                    val targetRoute = when {
                                        totalTabDrag < -120f -> TAB_ROUTES.getOrNull(fromIdx + 1)
                                        totalTabDrag > 120f -> TAB_ROUTES.getOrNull(fromIdx - 1)
                                        else -> null
                                    }
                                    if (targetRoute != null) {
                                        tabSwipeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Pola navigate IDENTIK dgn onClick NavigationBarItem/
                                        // NavigationRailItem (Batch 301) — 0 state baru, reuse
                                        // saveState/restoreState yang sudah ada.
                                        navController.navigate(targetRoute) {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    tabSwipeScope.launch {
                                        tabDragOffset.snapTo(tabDragOffsetPx.floatValue)
                                        tabDragOffset.animateTo(
                                            0f,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) {
                                            tabDragOffsetPx.floatValue = value
                                        }
                                    }
                                },
                                onDragCancel = {
                                    tabSwipeScope.launch {
                                        tabDragOffset.snapTo(tabDragOffsetPx.floatValue)
                                        tabDragOffset.animateTo(
                                            0f,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) {
                                            tabDragOffsetPx.floatValue = value
                                        }
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    totalTabDrag += dragAmount
                                    change.consume()
                                    tabDragOffsetPx.floatValue = (totalTabDrag * 0.3f).coerceIn(-40f, 40f)
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
        NavHost(
            navController = navController,
            startDestination = "home",
            // Batch 330 — default crossfade app-wide (tab bawah home/library/settings +
            // push stats_dashboard). Sebelum ini 0 enter/exitTransition di level NavHost =
            // cut instan bawaan Compose Navigation, kontras dgn "now_playing" yang sudah
            // punya slide+fade sendiri sejak lama. Angka 200/150 REUSE persis dari yang
            // sudah ada (tween(200) exitTransition "now_playing" bawah, tween(150) fadeIn
            // NowPlayingScreen.kt) — bukan angka baru. Simetris maju/mundur (pop = sama
            // dgn forward) karena tab switch bukan hierarki push/pop searah.
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(150)) }
        ) {
            composable("home") {
                HomeScreen(
                    rawSongs = librarySongs,
                    loading = libraryLoading,
                    favoriteIds = favoriteIds,
                    onSongClick = { songs, index -> playerViewModel.playQueue(songs, index) },
                    resumePreview = { songs -> playerViewModel.peekSavedSong(songs) },
                    onResumeClick = { songs -> playerViewModel.resumeFromSaved(songs) },
                    recentSongsProvider = { songs -> playerViewModel.getRecentSongs(songs) },
                    mostPlayedProvider = { songs -> playerViewModel.getMostPlayedSongs(songs) },
                    topArtistMixProvider = { songs -> playerViewModel.getTopArtistMix(songs) },
                    flashbackProvider = { songs -> playerViewModel.getFlashback(songs) },
                    statsVersion = statsVersion,
                    onShuffleAll = { songs -> playerViewModel.shuffleAll(songs) }
                )
            }
            composable("library") {
                LibraryScreen(
                    rawSongs = librarySongs,
                    loading = libraryLoading,
                    onRescan = { playerViewModel.refreshLibrary() },
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { playerViewModel.toggleFavorite(it) },
                    onSongClick = { songs, index -> playerViewModel.playQueue(songs, index) },
                    onPlayNext = { song ->
                        if (uiState.currentSong == null) {
                            playerViewModel.playQueue(listOf(song), 0)
                        } else {
                            playerViewModel.playNext(song)
                        }
                    },
                    onAddToQueue = { song ->
                        if (uiState.currentSong == null) {
                            playerViewModel.playQueue(listOf(song), 0)
                        } else {
                            playerViewModel.addToQueue(song)
                        }
                    },
                    playlists = playlists,
                    onCreatePlaylist = { name -> playerViewModel.createPlaylist(name) },
                    onDeletePlaylist = { id -> playerViewModel.deletePlaylist(id) },
                    onRenamePlaylist = { id, name -> playerViewModel.renamePlaylist(id, name) },
                    onAddSongToPlaylist = { id, songId -> playerViewModel.addSongToPlaylist(id, songId) },
                    onRemoveSongFromPlaylist = { id, songId -> playerViewModel.removeSongFromPlaylist(id, songId) },
                    onMoveSongInPlaylist = { id, from, to -> playerViewModel.moveSongInPlaylist(id, from, to) },
                    smartPlaylists = smartPlaylists,
                    onCreateSmartPlaylist = { playlist -> playerViewModel.createSmartPlaylist(playlist) },
                    onUpdateSmartPlaylist = { playlist -> playerViewModel.updateSmartPlaylist(playlist) },
                    onDeleteSmartPlaylist = { id -> playerViewModel.deleteSmartPlaylist(id) },
                    customFolders = customFolders,
                    onAddCustomFolder = { uri -> playerViewModel.addCustomFolder(uri) },
                    onRemoveCustomFolder = { uri -> playerViewModel.removeCustomFolder(uri) },
                    onDeleteSongs = { songs -> deleteSongsFromDevice(songs) },
                    onInfoMessage = { message -> playerViewModel.showInfoMessage(message) },
                    // Pending item Batch 163: SongRow tidak pernah tahu lagu mana yang sedang
                    // diputar — `uiState.currentSong` sudah lama ada di scope composable ini
                    // (dipakai `onPlayNext`/`onAddToQueue` di atas), cuma belum pernah
                    // diteruskan ke LibraryScreen. `?.id` — null wajar saat belum ada lagu
                    // diputar sama sekali (cold start), LibraryScreen sudah handle null lewat
                    // default parameter yang sama.
                    currentSongId = uiState.currentSong?.id
                )
            }
            composable("settings") {
                val settingsThemeIdentity by playerViewModel.themeIdentity.collectAsStateWithLifecycle()
                val settingsThemeMode by playerViewModel.themeMode.collectAsStateWithLifecycle()
                SettingsScreen(
                    currentThemeIdentity = settingsThemeIdentity,
                    currentThemeMode = settingsThemeMode,
                    onSelectThemeIdentity = { identity -> playerViewModel.setThemeIdentity(identity) },
                    onSelectThemeMode = { mode -> playerViewModel.setThemeMode(mode) },
                    lockEnabled = lockEnabled,
                    biometricEnabled = biometricEnabled,
                    biometricAvailable = biometricAvailable,
                    onSetPin = { pin -> playerViewModel.setPin(pin) },
                    onDisableLock = { playerViewModel.disableLock() },
                    onToggleBiometric = { enabled -> playerViewModel.setBiometricEnabled(enabled) },
                    shakeToSkipEnabled = shakeToSkipEnabled,
                    onToggleShakeToSkip = { enabled -> playerViewModel.setShakeToSkipEnabled(enabled) },
                    radioAutoContinueEnabled = radioAutoContinueEnabled,
                    onToggleRadioAutoContinue = { enabled -> playerViewModel.setRadioAutoContinueEnabled(enabled) },
                    floatingBubbleEnabled = floatingBubbleEnabled,
                    onToggleFloatingBubble = { enabled -> toggleFloatingBubble(enabled) },
                    silenceSkipEnabled = silenceSkipEnabled,
                    onToggleSilenceSkip = { enabled -> playerViewModel.setSilenceSkipEnabled(enabled) },
                    onInfoMessage = { message -> playerViewModel.showInfoMessage(message) },
                    onOpenStats = { navController.navigate("stats_dashboard") },
                    songs = librarySongs,
                    onDeleteSongs = { songs -> deleteSongsFromDevice(songs) }
                )
            }
            composable(
                route = "stats_dashboard",
                // Batch 331 — override default fade NavHost (Batch 330) khusus rute ini:
                // "stats_dashboard" adalah push hierarkis (drill-down dari Pengaturan), beda
                // sifat dari tab lateral home/library/settings yang cukup crossfade generik.
                // Pola gerak iOS-push: layar ini slide dari kanan + fade saat masuk, slide balik
                // ke kanan + fade saat di-pop (tombol back). Angka tween(300) REUSE persis dari
                // popExitTransition rute "now_playing" di file yang sama (bukan angka baru).
                // HANYA 2 field (bukan 4): per dokumentasi resmi Navigation-Compose,
                // enterTransition/popExitTransition dievaluasi dari destination ini SENDIRI saat
                // dia jadi targetState(forward)/initialState(pop) — tapi exitTransition/
                // popEnterTransition destination ini hanya berlaku kalau dia jadi
                // initialState(forward)/targetState(pop), yang TIDAK PERNAH terjadi untuk rute
                // leaf ini (0 rute lain navigate() forward dari sini, 0 rute pop kembali ke
                // sini — diverifikasi grep app-wide). "settings" (initial saat forward-nav,
                // target saat pop) pakai default NavHost Batch 330 (fadeOut 150 / fadeIn 200)
                // buat sisi dia, tidak perlu override tambahan.
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(300)
                    ) + fadeIn(tween(300))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(300)
                    ) + fadeOut(tween(300))
                }
            ) {
                val statsSnapshot = remember(librarySongs, statsVersion) {
                    playerViewModel.getListeningStats(librarySongs)
                }
                StatsDashboardScreen(
                    snapshot = statsSnapshot,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "now_playing",
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(350)
                    ) + fadeIn(tween(350))
                },
                exitTransition = {
                    fadeOut(tween(200))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(300)
                    ) + fadeOut(tween(300))
                }
            ) {
                nowPlayingContent { navController.popBackStack() }
            }
        }
        } // tutup Box(weight) pembungkus NavHost (Batch 101)

            // Batch 101 — Panel Now Playing persisten sisi kanan, HANYA di lebar Expanded
            // (>=840dp) selama ada lagu aktif & user tidak sedang di route "now_playing"
            // (showTwoPane, dihitung di atas). Garis pemisah 1dp tipis pakai outlineVariant
            // (token M3 khusus utk garis pemisah low-emphasis, bukan warna aksen) supaya
            // terbaca sebagai batas panel, bukan elemen dekoratif baru.
            if (showTwoPane) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Box(modifier = Modifier.width(420.dp).fillMaxHeight()) {
                    nowPlayingContent { }
                }
            }
        } // tutup Row adaptif (Batch 101)
    } // tutup Scaffold
    } // tutup CompositionLocalProvider(LocalHazeState) (Batch 295)
}

@Composable
private fun PermissionRationale(onRequest: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Batch 111 — sama seperti WelcomeScreen, layar ini juga render di luar Scaffold.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Izin akses musik dibutuhkan",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Aplikasi tidak bisa menampilkan lagu tanpa izin ini. Kalau tombol di bawah tidak memunculkan dialog izin, aktifkan izinnya lewat Pengaturan Aplikasi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Text("Coba Lagi")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }) {
            Text("Buka Pengaturan Aplikasi")
        }
    }
}
