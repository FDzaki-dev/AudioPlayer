package com.rudi.audioplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
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
import androidx.compose.foundation.layout.RowScope
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
// Batch 449 — `selectable` (kontrak resmi Compose Foundation, param `indication` eksplisit) +
// `Role` (semantics aksesibilitas Tab) — pengganti langsung `NavigationBarItem` M3 yang dihapus
// dari 3 titik pemakaian `bottomBar` (root cause kilatan kotak abu-abu, lihat komentar panjang
// di `CustomNavBarTabItem`, dekat `GlassTabIcon`).
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.foundation.layout.widthIn
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
// Batch 446 — `drawWithContent` (gambar SESUDAH content; `drawBehind` di atas cuma bisa SEBELUM)
// dipakai NavigationBar di AppNavHost. Batch 448 — titik pemakaian ini diperluas jadi SATU-SATUNYA
// penggambar pill tab-bar (gantikan bridge Batch 446 + 3 pill lama `GlassTabIcon`, lihat
// `navPillIndexAnim`), aktif di semua state bukan cuma saat drag. Offset/Size/CornerRadius/Stroke
// sengaja fully-qualified inline di situ (pola sama persis `Offset(0f,0f)` yang sudah ada di file
// ini) — 0 import baru selain ini.
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.semantics.Role
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
import com.rudi.audioplayer.ui.theme.Motion
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
// Batch 442 — fix label nav bawah terpotong/oversized (regresi Batch 439, lihat komentar
// MagnifyingTabLabel) + drag langsung di tab bar (bukan cuma tap). LocalTextStyle/blur (Batch
// 437) DILEPAS — diganti MaterialTheme.typography.labelMedium (style resmi label nav M3) +
// TextOverflow.Ellipsis (jaring pengaman); 0 blur lagi (root cause efek blur "useless" di
// screenshot user). awaitEachGesture/PointerEventPass/awaitFirstDown = drag-to-switch pada tab
// bar itu sendiri (PointerEventPass.Initial, 0 consume, 0 rebutan dgn tap/ripple NavigationBarItem).
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitFirstDown

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

    // BiometricPrompt.authenticate() called before the window actually has input focus
    // (e.g. straight from onCreate/first composition, especially with installSplashScreen()
    // still holding the splash content) can fail to show at all, silently — showBiometricPrompt()
    // below only overrides onAuthenticationSucceeded, so that failure has no visible effect.
    // Gating the auto-trigger on real window focus is what makes it reliably pop up on its own.
    private var hasWindowFocus by mutableStateOf(false)

    // Batch 516 [laporan user v515: "tab biometric fingerprint yang maksa nampilin dialog berkali-kali,
    // bagaimana dengan user yang mau buka via PIN"] — root cause (pembacaan kode): LaunchedEffect
    // auto-prompt di bawah di-key ke `hasWindowFocus`. BiometricPrompt sendiri MENGAMBIL fokus jendela
    // saat tampil dan MENGEMBALIKANNYA saat ditutup (tombol "Pakai PIN", tap luar, back) -> hasWindowFocus
    // false->true -> effect jalan ULANG -> dialog muncul lagi, tiap kali ditutup. User yang memilih PIN
    // tidak pernah bisa menyentuh keypad dengan tenang. Penanda ini membuat auto-prompt HANYA 1x per sesi
    // kunci; di-reset di onStop() (saat app benar-benar ditinggalkan = sesi kunci baru). Tombol "Sidik
    // Jari" manual di LockScreen tetap bisa dipakai kapan saja untuk memanggil prompt lagi.
    private var biometricAutoPrompted = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hasWindowFocus = hasFocus
    }

    override fun onStop() {
        super.onStop()
        isUnlocked = false
        biometricAutoPrompted = false
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

                LaunchedEffect(needsUnlock, biometricEnabled, hasWindowFocus) {
                    if (needsUnlock && biometricEnabled && hasWindowFocus && !biometricAutoPrompted && isBiometricAvailable()) {
                        biometricAutoPrompted = true
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

    // Batch 471 — lihat KDoc permission REQUEST_IGNORE_BATTERY_OPTIMIZATIONS di AndroidManifest.xml
    // utk root cause (OEM App Standby/battery restriction, bukti Batch 466/467). Dipanggil HANYA
    // dari 2 titik user AKTIF menyalakan toggle (bawah) — SENGAJA TIDAK dipanggil dari
    // LaunchedEffect(Unit) auto-restart di bawah (itu jalan tiap app dibuka tanpa aksi user
    // langsung; minta dialog sistem di situ = nge-nag tiap buka app kalau user pernah menolak,
    // pola anti-pattern). isIgnoringBatteryOptimizations() dicek dulu supaya dialog sistem 0
    // pernah muncul kalau sudah dikecualikan — bukan diminta ulang tiap toggle ON.
    fun requestIgnoreBatteryOptimizationsIfNeeded(context: android.content.Context) {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return
        if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) return
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
        // Gagal (mis. OEM blokir action ini) = diam-diam skip, bubble TETAP nyala lewat
        // startBubbleService() yang sudah dipanggil di pemanggil — dialog ini murni penambah
        // keandalan, bukan syarat bubble bisa jalan sama sekali.
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(overlayPermissionContext)) {
            playerViewModel.setFloatingBubbleEnabled(true)
            startBubbleService(overlayPermissionContext)
            requestIgnoreBatteryOptimizationsIfNeeded(overlayPermissionContext)
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
            requestIgnoreBatteryOptimizationsIfNeeded(overlayPermissionContext)
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
            when (event) {
                Lifecycle.Event.ON_RESUME -> playerViewModel.refreshFloatingBubbleEnabled()
                // Batch 537 (baterai) — loop posisi di ViewModel melambat saat UI tak terlihat.
                Lifecycle.Event.ON_START -> playerViewModel.setUiVisible(true)
                Lifecycle.Event.ON_STOP -> playerViewModel.setUiVisible(false)
                else -> Unit
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
    // Batch 479 — HOISTED dari dalam lambda `bottomBar =` (posisi lama, lihat komentar penuh di
    // titik pemakaian `NavigationBar` di bawah) ke scope `AppNavHost` di sini — pola sinkronisasi
    // yang SAMA PERSIS dgn `tabSwipeScope`/`tabDragOffsetPx`/`tabDragOffset` di atas (semua di
    // scope ini justru SUPAYA bisa dibaca lintas lambda `content =` & `bottomBar =` milik
    // `Scaffold`, 2 lambda terpisah yg 0 saling lihat state satu sama lain). Root cause bug "drag
    // lintas tab, bottom nav diam" (lihat RESUME POINT Batch 478 di PROJECT_STATE.md): sebelum
    // hoist ini, `navPillIndexAnim` cuma bisa disentuh dari DALAM `bottomBar =` (tab-bar-drag-end
    // & 3 onClick `NavigationBarItem`) — swipe KONTEN (`content =`, blok pointerInput di bawah
    // NavHost) 0 pernah bisa menyentuhnya sama sekali walau navigate()-nya sendiri berhasil.
    val navPillIndexAnim = remember {
        Animatable(
            (TAB_ROUTES.indexOfFirst { it == currentRoute }.coerceAtLeast(0) + 0.5f)
        )
    }

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
            // Batch 489 — QA checklist gap #1 (kontrol Stop eksplisit), lihat komentar
            // PlayerViewModel.stopPlayback().
            onStopPlayback = { playerViewModel.stopPlayback() },
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
                        },
                        onDismiss = { playerViewModel.dismissMiniPlayer() }
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
                    // Batch 442 — dibaca oleh pointerInput drag-on-tab-bar di bawah (key `Unit`,
                    // coroutine gesture HIDUP TERUS lintas 3 tab supaya 1 drag jari bisa lewati
                    // lebih dari 1 batas tab tanpa putus — lihat rasionalisasi penuh di situ).
                    // `rememberUpdatedState` WAJIB di sini (bukan baca `currentRoute` langsung di
                    // closure `pointerInput(Unit)`) — closure key-`Unit` cuma dibuat SEKALI, baca
                    // `currentRoute` polos di dalamnya akan BEKU ke nilai komposisi pertama
                    // (stale), tidak ikut update tiap kali tab berpindah selama drag berlangsung.
                    val currentRouteState = rememberUpdatedState(currentRoute)
                    val homeTabInteraction = remember { MutableInteractionSource() }
                    val libraryTabInteraction = remember { MutableInteractionSource() }
                    val settingsTabInteraction = remember { MutableInteractionSource() }
                    // Batch 444 — user eksplisit: pill/capsule drag tab-bar (Batch 442) 0 ikut
                    // posisi jari real-time (baru "lompat" pas commit index-crossing). Posisi
                    // kontinu (satuan "index", 0f..TAB_ROUTES.size) selama drag tab-bar aktif,
                    // NaN = tidak sedang drag di SINI (state TERPISAH dari tabDragOffsetPx milik
                    // swipe KONTEN Batch 435/437 — beda area sentuh, 0 saling pakai). Ditulis
                    // SINKRON langsung di loop awaitEachGesture yang sudah ada (bukan lewat
                    // coroutine/snapTo per-delta) — sumbu sinkron-vs-asinkron yang sama yang
                    // sudah dijaga Batch 433/434.
                    val tabBarDragIndexPx = remember { mutableFloatStateOf(Float.NaN) }
                    // Tahanan visual (rubber-band) saat jari didorong lewat ujung kolom
                    // pertama/terakhir (Beranda/Pengaturan) — sebelumnya 0 sinyal apa pun, index
                    // cuma coerceIn diam-diam. Pola SAMA PERSIS dgn tabDragOffsetPx/tabDragOffset
                    // (swipe konten Batch 435, di bawah) & AlbumArtHero (NowPlayingScreen.kt,
                    // Batch 434): *Px = sumber kebenaran sinkron dibaca graphicsLayer, Animatable
                    // HANYA dipakai fase springback (reuse tabSwipeScope yang sudah ada) supaya
                    // 0 nge-block awaitEachGesture menunggu gesture berikutnya selama springback
                    // masih jalan.
                    val tabBarOverscrollPx = remember { mutableFloatStateOf(0f) }
                    val tabBarOverscrollAnim = remember { Animatable(0f) }
                    // Fallback ke tabMagnifyFocus (Batch 437) kalau 0 drag tab-bar aktif — 0
                    // regresi ke behavior lama (nudge dari swipe konten tetap jalan apa adanya).
                    // Aktif (idxPos bukan NaN): fungsi tenda (segitiga) sederhana dari jarak posisi
                    // kontinu jari ke titik tengah tiap kolom (`tabIndex + 0.5f`) — 1f persis di
                    // tengah kolom, turun linear ke 0f pas jarak 1 kolom penuh (= tengah kolom
                    // tetangga), pas gaya "lensa mengikuti jari" segmented-control asli, 0 nunggu
                    // navigate() commit index-crossing dulu spt sebelumnya.
                    fun tabBarDragFocus(tabIndex: Int): Float {
                        val idxPos = tabBarDragIndexPx.floatValue
                        if (idxPos.isNaN()) return tabMagnifyFocus(tabIndex)
                        val dist = kotlin.math.abs(idxPos - (tabIndex + 0.5f))
                        return (1f - dist).coerceIn(0f, 1f)
                    }
                    // Batch 445 — dibaca `GlassTabIcon` (param `isDragging` baru, lihat 3 titik
                    // pemakaian di bawah) supaya glassAlpha snap 1:1 real-time HANYA selama drag
                    // sungguhan berlangsung — 2 sumber `focus` kontinu yang masuk
                    // `tabBarDragFocus` di atas SAMA-SAMA dicek: drag LANGSUNG di tab-bar
                    // (`tabBarDragIndexPx` bukan NaN) ATAU nudge dari swipe KONTEN (`tabDragOffsetPx`
                    // != 0, termasuk saat masih springback menuju 0 pasca lepas jari — begitu
                    // benar-benar 0 lagi, `tabMagnifyFocus` sudah balik ke nilai stabil 0f/1f,
                    // handoff ke tween(220) di GlassTabIcon 0 lompatan). Tap biasa (0 drag
                    // manapun aktif) = false, tetap dapat cross-fade tween(220) lama.
                    val isTabBarDragging = !tabBarDragIndexPx.floatValue.isNaN() ||
                        tabDragOffsetPx.floatValue != 0f
                    // Batch 448 — ROMBAK TOTAL (gantikan pendekatan "bridge" Batch 446). Root
                    // cause seam/double-pill yang dilaporkan user (dikonfirmasi frame-by-frame
                    // dari video referensi): Batch 446 menambah 1 pill BARU via `drawWithContent`
                    // tapi 3 pill LAMA per-tab (`GlassTabIcon`) TETAP jalan bersamaan — 2 sistem
                    // gambar pill aktif serentak selama drag = tumpang-tindih kotak dgn seam
                    // kelihatan (persis yg direkam user). Fix root-cause (bukan tempel lapisan
                    // ke-3): 3 pill lama DIHAPUS TOTAL (lihat `GlassTabIcon`) — SEKARANG HANYA 1
                    // pill yang pernah digambar, di SINI, aktif di SEMUA state (idle/tap/drag/
                    // nudge), bukan cuma saat drag spt bridge lama.
                    // `navPillIndexAnim`: satu-satunya sumber posisi "rest" pill (satuan index
                    // kontinu, mis. 1.5 = tengah kolom ke-2 dari 3), dipakai saat TIDAK sedang
                    // drag langsung di tab-bar (idle/nudge-konten/baru selesai drag). Selama drag
                    // LANGSUNG di tab-bar, posisi dibaca live dari `tabBarDragIndexPx` (mentah,
                    // 1:1 jari, pola sinkron yang sama persis dipertahankan dari Batch 444/445) —
                    // disinkronkan via snapTo+animateTo eksplisit di 3 jalur: selesai drag LANGSUNG
                    // di tab-bar (di bawah, dekat `tabBarDragIndexPx.floatValue = Float.NaN`), 3
                    // onClick tap biasa (dekat tiap `NavigationBarItem`), DAN (Batch 479) selesai
                    // drag swipe KONTEN (`content =`, blok pointerInput di luar `Scaffold` ini) —
                    // 0 LaunchedEffect(currentRoute) generik dipasang di titik mana pun supaya 0
                    // race kondisi start-animasi ganda antar 3 jalur di atas. Batch 479 — deklarasi
                    // DIPINDAH (hoisted) ke scope `AppNavHost` atas (dekat `tabDragOffsetPx`) supaya
                    // jalur ke-3 di atas (`content =`, scope terpisah dari `bottomBar =` ini) bisa
                    // ikut menyentuhnya — lihat komentar lengkap di titik deklarasi barunya untuk
                    // root cause penuh. Nilai awal (di titik deklarasi) = index tab aktif SAAT
                    // AppNavHost pertama komposisi (bukan hardcode 0/tengah), 0 lompatan visual
                    // pas start app di tab mana pun.
                    val navBarIsSkeu = isSkeuTheme()
                    val navBarAccentTint = MaterialTheme.colorScheme.primary
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
                            )
                            // Batch 442 — permintaan eksplisit user: "tambahkan fitur drag pada
                            // tab, bukan hanya tap-tab doang" (screenshot bottom nav bar). Beda
                            // dari swipe Batch 435 (drag di KONTEN layar, Box pembungkus NavHost
                            // di bawah, dipicu geser di ATAS layar) — ini drag LANGSUNG di atas
                            // bar tab itu sendiri, gaya segmented-control iOS: tekan salah satu
                            // tab lalu geser jari TANPA angkat, tab aktif ikut berpindah mengikuti
                            // posisi jari melintasi 3 kolom (lebar dibagi rata `TAB_ROUTES.size`
                            // — valid krn 0 weight kustom dipasang di 3 `NavigationBarItem` di
                            // bawah, M3 default-nya memang equal-width). 2 mekanisme (tap lama +
                            // drag baru ini) hidup berdampingan, 0 saling timpa:
                            // `PointerEventPass.Initial` dipakai (bukan default Main) supaya
                            // event dibaca SEBELUM `NavigationBarItem` (child, lebih dalam di
                            // tree) memprosesnya di Main pass-nya sendiri — dan `change.consume()`
                            // SENGAJA 0 pernah dipanggil sama sekali di blok ini, jadi tap
                            // polos/ripple-feedback bawaan `NavigationBarItem`+`bouncyPress`
                            // (Batch 438) TIDAK terganggu sedikit pun (tap singkat = jari tidak
                            // pernah keluar kolom awal = `newIndex` tidak pernah beda dari
                            // `hoveredIndex` awal = blok navigate() di bawah tidak pernah
                            // tereksekusi — murni `onClick` bawaan `NavigationBarItem` yang
                            // menangani tap biasa, sama seperti sebelum batch ini). `navController
                            // .navigate` dipanggil dgn opsi IDENTIK popUpTo/launchSingleTop/
                            // restoreState (pola Batch 301/435), 0 state-preservation baru. Key
                            // `pointerInput` sengaja `Unit` (BUKAN `currentRoute`) — lihat
                            // rasionalisasi `currentRouteState` di atas dekat deklarasi
                            // `homeTabInteraction`: NavigationBar composable ini sendiri TIDAK
                            // pernah keluar-masuk komposisi selama pindah antar 3 tab (kondisi
                            // `if` pembungkusnya tetap true di ketiganya), jadi coroutine gesture
                            // key-`Unit` ini aman hidup terus lintas tab tanpa restart — kalau
                            // di-key `currentRoute`, coroutine akan MATI-HIDUP ULANG setiap kali
                            // 1 batas tab terlewati (navigate() mengubah currentRoute = state
                            // baru = key baru), padahal jari MASIH menekan di tengah gesture yang
                            // sama — drag yg melewati lebih dari 1 batas tab (mis. Beranda
                            // langsung ke Pengaturan) akan macet di tab kedua krn instance baru
                            // cuma menunggu event DOWN baru, bukan lanjutan drag yg sudah berjalan.
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                    val barWidthPx = size.width.toFloat()
                                    if (barWidthPx <= 0f) return@awaitEachGesture
                                    // Batch 444 — jaring pengaman sama pola tabDragOffset/
                                    // AlbumArtHero: hentikan springback overscroll lama supaya
                                    // tidak menimpa drag baru yang mulai lagi cepat.
                                    tabSwipeScope.launch { tabBarOverscrollAnim.stop() }
                                    tabBarDragIndexPx.floatValue =
                                        (down.position.x / barWidthPx * TAB_ROUTES.size)
                                            .coerceIn(0f, TAB_ROUTES.size.toFloat())
                                    var hoveredIndex = (down.position.x / barWidthPx * TAB_ROUTES.size)
                                        .toInt()
                                        .coerceIn(0, TAB_ROUTES.size - 1)
                                    // Batch 452 — user: pill/tab kadang berhenti 1 kolom SEBELUM
                                    // posisi jari sungguhan saat lepas ("offside"), terutama drag
                                    // cepat (flick). Root cause: `if (!change.pressed) break` (lama)
                                    // dicek DI AWAL badan loop, SEBELUM posisi event ini dibaca —
                                    // utk event UP (pressed=false) loop break LANGSUNG, jadi posisi
                                    // asli titik lepas jari itu TIDAK PERNAH diproses ke
                                    // `tabBarDragIndexPx`/`hoveredIndex`. `hoveredIndex` (dipakai
                                    // navigate() + target akhir `navPillIndexAnim.animateTo` pasca-
                                    // loop) jadi nyangkut di event MOVE kedua-dari-akhir, yang pada
                                    // flick cepat (event batching sistem) bisa beda 1 kolom penuh dari
                                    // titik lepas sebenarnya. Fix: posisi TIAP event (termasuk UP)
                                    // diproses dulu sama seperti event MOVE, `pressed` dicek
                                    // TERAKHIR (akhir badan loop, lihat bawah) sbg syarat
                                    // lanjut/berhenti — bukan lagi syarat lewati pemrosesan posisi.
                                    // 0 state/mekanisme baru, murni urutan 2 baris dipertukar.
                                    while (true) {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: break
                                        val rawX = change.position.x
                                        val x = rawX.coerceIn(0f, barWidthPx)
                                        // Batch 444 — posisi kontinu utk pill live-tracking
                                        // (tabBarDragFocus di atas), dari `x` yg SAMA PERSIS
                                        // dipakai hitung newIndex di bawah (0 hitungan ganda).
                                        tabBarDragIndexPx.floatValue = x / barWidthPx * TAB_ROUTES.size
                                        // Batch 444 — tahanan visual: overscroll dari rawX
                                        // (BUKAN x yg sudah di-coerce) supaya kebaca begitu jari
                                        // lewat ujung kolom pertama/terakhir. Redaman 0.3f (pola
                                        // sama persis tabDragOffsetPx.floatValue = totalTabDrag *
                                        // 0.3f di bawah) + batas ±24px (jauh lebih kecil dari
                                        // lebar 1 kolom) supaya terasa "ketahan", bukan ikut
                                        // sepenuhnya.
                                        val overscroll = when {
                                            rawX < 0f -> rawX
                                            rawX > barWidthPx -> rawX - barWidthPx
                                            else -> 0f
                                        }
                                        tabBarOverscrollPx.floatValue = (overscroll * 0.3f).coerceIn(-24f, 24f)
                                        val newIndex = (x / barWidthPx * TAB_ROUTES.size)
                                            .toInt()
                                            .coerceIn(0, TAB_ROUTES.size - 1)
                                        if (newIndex != hoveredIndex) {
                                            hoveredIndex = newIndex
                                            val target = TAB_ROUTES.getOrNull(newIndex)
                                            val fromRoute = currentRouteState.value
                                            if (target != null && target != fromRoute) {
                                                tabSwipeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                navController.navigate(target) {
                                                    popUpTo("home") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                        // Batch 452 — `pressed` dicek TERAKHIR (lihat komentar
                                        // panjang di atas `while (true)`): posisi event UP ini SUDAH
                                        // diproses (tabBarDragIndexPx/hoveredIndex/navigate di atas)
                                        // sebelum keluar loop, 0 lagi kehilangan 1 event terakhir.
                                        if (!change.pressed) break
                                    }
                                    // Batch 444 — gesture selesai (naik/batal, 2 jalur break di
                                    // atas SAMA-SAMA jatuh ke sini): lepas live-tracking (fallback
                                    // balik ke tabMagnifyFocus lewat NaN) + springback overscroll
                                    // ke 0 lewat tabSwipeScope (non-blocking, awaitEachGesture
                                    // langsung siap terima down berikutnya tanpa nunggu spring).
                                    // Batch 448 — handoff pill unified: simpan posisi jari
                                    // TERAKHIR sebelum di-reset NaN (lokal, 0 state `remember`
                                    // tambahan — 1 gesture = 1 pemanggilan coroutine ini), lalu
                                    // `navPillIndexAnim` snapTo persis di situ dulu (0 lompatan)
                                    // baru animateTo pusat kolom final (`hoveredIndex + 0.5f`,
                                    // tween 220ms — durasi SAMA PERSIS `glassAlphaAnim` supaya
                                    // pill & warna ikon/label tiba di tujuan BERSAMAAN).
                                    val lastLiveIdxPos = tabBarDragIndexPx.floatValue
                                    tabBarDragIndexPx.floatValue = Float.NaN
                                    tabSwipeScope.launch {
                                        navPillIndexAnim.snapTo(lastLiveIdxPos)
                                        navPillIndexAnim.animateTo(hoveredIndex + 0.5f, tween(220))
                                    }
                                    tabSwipeScope.launch {
                                        tabBarOverscrollAnim.snapTo(tabBarOverscrollPx.floatValue)
                                        tabBarOverscrollAnim.animateTo(
                                            0f,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) {
                                            tabBarOverscrollPx.floatValue = value
                                        }
                                    }
                                }
                            }
                            .graphicsLayer { translationX = tabBarOverscrollPx.floatValue }
                            // Batch 448 — SATU-SATUNYA pill yang pernah digambar utk identitas
                            // kaca (gantikan bridge Batch 446 + 3 pill lama `GlassTabIcon` yang
                            // SUDAH dihapus, lihat komentar panjang di situ). Digambar SESUDAH
                            // `drawContent()` (di atas ikon/label — `drawBehind` cuma bisa SEBELUM,
                            // akan tertutup total oleh Surface+Row NavigationBar sendiri) — alpha
                            // rendah (0.16f/0.14f, IDENTIK pill lama) jadi tetap tidak mengganggu
                            // keterbacaan ikon/label. AKTIF SETIAP SAAT (bukan cuma saat drag spt
                            // bridge lama) — pill kini SELALU ada di bawah tab aktif idle, bukan
                            // cuma nongol pas jari nyentuh bar.
                            .drawWithContent {
                                drawContent()
                                if (!navBarIsSkeu) {
                                    // Batch 448 — 1 sumber posisi, urut prioritas: (1) drag
                                    // LANGSUNG di tab-bar (`tabBarDragIndexPx` bukan NaN, mentah
                                    // 1:1 jari — pola sinkron sama persis Batch 444/445, 0 lag);
                                    // (2) nudge dari swipe KONTEN (`tabDragOffsetPx` != 0, geser
                                    // kecil ±0.5 kolom dari titik rest `navPillIndexAnim`, pola
                                    // pecahan SAMA PERSIS `tabMagnifyFocus` di atas — 0 hitungan
                                    // baru, cuma dipetakan ke satuan index alih-alih 0f..1f);
                                    // (3) rest — `navPillIndexAnim.value`, di-animate-kan tween
                                    // 220ms di titik SELESAI drag & di 3 onClick tap (lihat
                                    // deklarasinya di atas).
                                    val liveBarDrag = tabBarDragIndexPx.floatValue
                                    val nudge = tabDragOffsetPx.floatValue
                                    val idxPos = when {
                                        !liveBarDrag.isNaN() -> liveBarDrag
                                        nudge != 0f -> {
                                            val towardNext = (-nudge / 40f).coerceIn(0f, 1f)
                                            val towardPrev = (nudge / 40f).coerceIn(0f, 1f)
                                            navPillIndexAnim.value + (towardNext - towardPrev) * 0.5f
                                        }
                                        else -> navPillIndexAnim.value
                                    }
                                    val columnWidthPx = size.width / TAB_ROUTES.size
                                    val pillWidthPx = columnWidthPx * 0.74f
                                    val pillHeightPx = size.height * 0.62f
                                    val centerX = (idxPos * columnWidthPx)
                                        .coerceIn(pillWidthPx / 2f, size.width - pillWidthPx / 2f)
                                    val topLeft = androidx.compose.ui.geometry.Offset(
                                        centerX - pillWidthPx / 2f,
                                        (size.height - pillHeightPx) / 2f
                                    )
                                    val pillSize = androidx.compose.ui.geometry.Size(pillWidthPx, pillHeightPx)
                                    val corner = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                                    drawRoundRect(
                                        color = navBarAccentTint,
                                        topLeft = topLeft,
                                        size = pillSize,
                                        cornerRadius = corner,
                                        alpha = 0.16f
                                    )
                                    drawRoundRect(
                                        color = Color.White,
                                        topLeft = topLeft,
                                        size = pillSize,
                                        cornerRadius = corner,
                                        alpha = 0.14f,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                    )
                                }
                            },
                        // Batch 53: lowered from 12.dp — spec §15 keeps navigation "calm and
                        // immediately understandable" and explicitly warns against every item (or
                        // in this case, the whole bar) reading as an accent-tinted glow. M3's
                        // tonalElevation overlay scales with elevation and this app's
                        // surfaceTint is the accent color (Theme.kt), so 12.dp let the bar itself
                        // read as "blue" before "glass" — 6.dp keeps a legible elevated-glass lift
                        // (Level 2, spec §4) without the accent wash dominating the one piece of
                        // chrome that's always on screen. Batch 57: Skeu shares this same 6.dp —
                        // same reasoning (SkeuAccent as surfaceTint would otherwise dominate).
                        tonalElevation = if (navCatchLightColor != null) 6.dp else NavigationBarDefaults.Elevation,
                        // Batch 444 — user: "border tab nav terluar kebesaran". Root cause:
                        // `windowInsets` DEFAULT NavigationBar (`NavigationBarDefaults.windowInsets`)
                        // masih mereservasi tinggi system-nav-bar DI DALAM capsule (Batch 439 0
                        // pernah sentuh param ini — lihat komentar lama di `.padding(...)` atas:
                        // "margin 12.dp TAMBAHAN di atas inset itu, BUKAN pengganti"), padahal
                        // capsule ini sudah "mengambang" lewat margin luar sejak Batch 439 — jadi
                        // inset system-bar itu kini DOBEL terhitung (sekali di dalam tinggi
                        // capsule, sekali lagi di margin luar), bikin capsule terlihat lebih
                        // tebal/besar dari semestinya. Fix: nolkan `windowInsets` di titik ini
                        // SAJA (bukan ganti default NavigationBarDefaults.windowInsets app-wide) —
                        // margin luar 12.dp bawah (Batch 439) TETAP jalan sendiri, tetap 0 risiko
                        // capsule ketutup gesture-nav (device minSdk 31 = gesture-nav umum, margin
                        // 12.dp historisnya sudah cukup sebelum inset dobel ini ditambah).
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        // Batch 449 — `NavigationBarItem` M3 DIHAPUS TOTAL dari 3 titik pemakaian
                        // ini (root cause kilatan kotak abu-abu di tab yang baru ditinggalkan,
                        // lihat komentar panjang di definisi `CustomNavBarTabItem`, dekat
                        // `GlassTabIcon`/`NoRippleIndication`) — diganti `CustomNavBarTabItem` x3.
                        // `selected`/`onClick`/route-navigate/`navPillIndexAnim.animateTo(...)` di
                        // masing-masing 0 diubah SAMA SEKALI (logic isinya identik persis, cuma
                        // pindah wadah pemanggilan dari slot `onClick=`/`icon=` NavigationBarItem
                        // ke param `onClick=`/trailing-lambda `CustomNavBarTabItem`).
                        // `CompositionLocalProvider(LocalIndication provides NoRippleIndication)`
                        // (Batch 439) yang dulu membungkus blok ini juga DIHAPUS — sudah tidak
                        // relevan (0 lagi ada NavigationBarItem yang baca `LocalIndication` di
                        // sini), `.selectable(indication = null, ...)` di `CustomNavBarTabItem`
                        // sudah cukup, 0 override composition-local diperlukan lagi.
                        CustomNavBarTabItem(
                            selected = currentRoute == "home",
                            onClick = {
                                // Batch 301 — user melaporkan stuttering pas transisi antar tab
                                // (beda dari stutter SCROLL Batch 300 yang sudah dijawab lewat
                                // blurRadius). Root cause: `popUpTo`/`navigate` di 6 titik ini
                                // (3 tab bawah + 3 NavigationRailItem, pola identik) 0
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
                                // Batch 448 — tap biasa (0 drag): pill unified ikut pindah ke
                                // kolom ini, tween 220ms SAMA PERSIS `glassAlphaAnim` (icon/label)
                                // supaya tiba bersamaan. Animatable.animateTo mulai otomatis dari
                                // posisi TERKINI (kalau lagi mid-animasi tap sebelumnya, pola sama
                                // `glassAlphaAnim`) — 0 penanganan spesial dibutuhkan.
                                tabSwipeScope.launch { navPillIndexAnim.animateTo(0.5f, tween(220)) }
                            },
                            interactionSource = homeTabInteraction
                        ) {
                            // Batch 439 — label sudah digabung ke 1 slot ini (dulu slot terpisah
                            // `label = { MagnifyingTabLabel(...) }`, lihat komentar Batch 439 di
                            // definisi `GlassTabIcon`). Batch 449 — trailing-lambda
                            // `CustomNavBarTabItem` ini gantikan slot `icon=` NavigationBarItem
                            // lama, isi 0 berubah.
                            GlassTabIcon(
                                icon = Icons.Default.Home,
                                label = "Beranda",
                                focus = tabBarDragFocus(0),
                                selected = currentRoute == "home",
                                interactionSource = homeTabInteraction,
                                isDragging = isTabBarDragging
                            )
                        }
                        CustomNavBarTabItem(
                            selected = currentRoute == "library",
                            onClick = {
                                // Batch 301 — sama seperti onClick "home" di atas.
                                navController.navigate("library") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                // Batch 448 — sama seperti onClick "home" di atas.
                                tabSwipeScope.launch { navPillIndexAnim.animateTo(1.5f, tween(220)) }
                            },
                            interactionSource = libraryTabInteraction
                        ) {
                            // Batch 439/449 — sama seperti "home" di atas.
                            GlassTabIcon(
                                icon = Icons.Default.LibraryMusic,
                                label = "Perpustakaan",
                                focus = tabBarDragFocus(1),
                                selected = currentRoute == "library",
                                interactionSource = libraryTabInteraction,
                                isDragging = isTabBarDragging
                            )
                        }
                        CustomNavBarTabItem(
                            selected = currentRoute == "settings",
                            onClick = {
                                // Batch 301 — sama seperti onClick "home" di atas.
                                navController.navigate("settings") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                // Batch 448 — sama seperti onClick "home" di atas.
                                tabSwipeScope.launch { navPillIndexAnim.animateTo(2.5f, tween(220)) }
                            },
                            interactionSource = settingsTabInteraction
                        ) {
                            // Batch 439/449 — sama seperti "home" di atas.
                            GlassTabIcon(
                                icon = Icons.Default.Settings,
                                label = "Pengaturan",
                                focus = tabBarDragFocus(2),
                                selected = currentRoute == "settings",
                                interactionSource = settingsTabInteraction,
                                isDragging = isTabBarDragging
                            )
                        }
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
        // Batch 477 — FIX BUG (laporan user: "video drag langsung lintas tab ternyata tidak
        // berefek pada pergerakan bilah bottom nav sama sekali"): root cause — pointerInput di
        // bawah (Batch 435, content-area swipe) di-key `currentRoute`. Persis pola bahaya yang
        // SUDAH didokumentasikan di `currentRouteState`/Batch 442 (dekat NavigationBar di atas):
        // begitu `navigate()` di `onDragEnd` mengubah `currentRoute`, key berubah → Compose
        // cancel+restart coroutine gesture ini di komposisi berikutnya. Untuk drag SATU tab tetap
        // 0 kelihatan (restart terjadi SETELAH gesture selesai), tapi utk swipe LANGSUNG LINTAS
        // >1 tab dalam 1 gesture kontinu (jari belum terangkat): begitu batas tab pertama
        // terlewati, instance lama mati, instance baru cuma `awaitFirstDown()` — down utk jari
        // yg SUDAH menekan sejak awal TIDAK PERNAH datang lagi, sisa drag itu 0 diproses sama
        // sekali (tabDragOffsetPx/tabMagnifyFocus/nudge konten beku, pill/label bottom nav 0
        // bergerak lagi). `currentRouteState` yang sudah ada (Batch 442, dekat deklarasi
        // `homeTabInteraction`) TIDAK bisa dipakai ulang di sini — scope-nya di dalam lambda
        // `bottomBar =` milik `Scaffold`, sudah di luar jangkauan di lambda `content =` ini. Fix:
        // instance BARU (scope lokal di sini saja, 0 mengubah yang lama sama sekali) + key
        // `pointerInput` diganti `Unit` di bawah — 0 formula/threshold 120px/damping 0.3f/clamp
        // ±40px Batch 435/437 disentuh, murni pindah pola sinkronisasi state yg SUDAH terbukti.
        val contentSwipeRouteState = rememberUpdatedState(currentRoute)
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
                        Modifier.pointerInput(Unit) {
                            var totalTabDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    totalTabDrag = 0f
                                    // jaring pengaman sama seperti AlbumArtHero: hentikan
                                    // springback lama supaya tidak menimpa drag baru.
                                    tabSwipeScope.launch { tabDragOffset.stop() }
                                },
                                onDragEnd = {
                                    val fromIdx = TAB_ROUTES.indexOfFirst { it == contentSwipeRouteState.value }
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
                                        // Batch 479 — FIX root cause (data log instrumentasi Batch
                                        // 478 dikonfirmasi user: drag/navigate di atas TERBUKTI
                                        // jalan normal, currentRoute berubah sesuai target — tapi
                                        // pill bottom nav tetap diam). Penyebab sebenarnya:
                                        // `navPillIndexAnim` (deklarasi di-hoist ke scope
                                        // AppNavHost atas, lihat komentar di titik deklarasinya)
                                        // SEBELUMNYA cuma disinkronkan dari 2 jalur — selesai drag
                                        // LANGSUNG di tab-bar (Batch 444/448) & onClick 3
                                        // `NavigationBarItem` (Batch 448) — blok swipe KONTEN ini
                                        // manggil navController.navigate() langsung TANPA PERNAH
                                        // menyentuh navPillIndexAnim, jadi pill kehilangan sinyal
                                        // pindah tab walau navigasi & konten sudah benar
                                        // berpindah. Fix: 1 panggilan eksplisit tambahan HANYA di
                                        // jalur ini (bukan LaunchedEffect(currentRoute) generik —
                                        // sengaja dihindari, lihat rasionalisasi asli di titik
                                        // deklarasi navPillIndexAnim, supaya 0 race start-animasi
                                        // ganda dgn 2 jalur lain) — pola tween(220) IDENTIK dgn 3
                                        // onClick NavigationBarItem, 0 angka/formula baru.
                                        tabSwipeScope.launch {
                                            navPillIndexAnim.animateTo(
                                                TAB_ROUTES.indexOfFirst { it == targetRoute } + 0.5f,
                                                tween(220)
                                            )
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
            // Batch 481 — durasi 200/150 di bawah 0 diubah (masih Motion.DURATION_STANDARD/
            // DURATION_QUICK, alias angka yang sama persis), yang diubah HANYA easing:
            // default `tween()` bawaan (FastOutSlowInEasing, kurva Material) diganti
            // `Motion.IosEasing` (S-curve iOS) sesuai instruksi eksplisit user batch ini
            // ("polish animasi/transisi biar mulus like iOS"). Non-breaking murni di
            // lapisan kurva, 0 formula/threshold gesture lain disentuh.
            enterTransition = { fadeIn(animationSpec = tween(Motion.DURATION_STANDARD, easing = Motion.IosEasing)) },
            exitTransition = { fadeOut(animationSpec = tween(Motion.DURATION_QUICK, easing = Motion.IosEasing)) },
            popEnterTransition = { fadeIn(animationSpec = tween(Motion.DURATION_STANDARD, easing = Motion.IosEasing)) },
            popExitTransition = { fadeOut(animationSpec = tween(Motion.DURATION_QUICK, easing = Motion.IosEasing)) }
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
                    // Batch 481 — angka 300 0 diubah (Motion.DURATION_EMPHASIZED, alias
                    // persis), easing diganti Motion.IosEasing (lihat catatan Batch 481 di
                    // NavHost atas untuk rasional lengkap).
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(Motion.DURATION_EMPHASIZED, easing = Motion.IosEasing)
                    ) + fadeIn(tween(Motion.DURATION_EMPHASIZED, easing = Motion.IosEasing))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(Motion.DURATION_EMPHASIZED, easing = Motion.IosEasing)
                    ) + fadeOut(tween(Motion.DURATION_EMPHASIZED, easing = Motion.IosEasing))
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
                // Batch 481 — angka 350/200/300 0 diubah (Motion.DURATION_SCREEN/QUICK/
                // EMPHASIZED, alias persis), easing diganti Motion.IosEasing (rasional
                // lengkap di catatan Batch 481, NavHost atas).
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(Motion.DURATION_SCREEN, easing = Motion.IosEasing)
                    ) + fadeIn(tween(Motion.DURATION_SCREEN, easing = Motion.IosEasing))
                },
                exitTransition = {
                    fadeOut(tween(Motion.DURATION_QUICK, easing = Motion.IosEasing))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(Motion.DURATION_EMPHASIZED, easing = Motion.IosEasing)
                    ) + fadeOut(tween(Motion.DURATION_EMPHASIZED, easing = Motion.IosEasing))
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
