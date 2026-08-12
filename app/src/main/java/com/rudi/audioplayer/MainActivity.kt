package com.rudi.audioplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.app.RecoverableSecurityException
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
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
import com.rudi.audioplayer.playback.PlayerViewModel
import com.rudi.audioplayer.ui.HomeScreen
import com.rudi.audioplayer.ui.LockScreen
import com.rudi.audioplayer.ui.LibraryScreen
import com.rudi.audioplayer.ui.SettingsScreen
import com.rudi.audioplayer.ui.MiniPlayerBar
import com.rudi.audioplayer.ui.NowPlayingScreen
import com.rudi.audioplayer.ui.theme.ThemeIdentity
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
            .setTitle("Buka AudioPlayer")
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
                val identityRootBrush = when (appThemeIdentity) {
                    ThemeIdentity.TACTILE -> Brush.linearGradient(
                        colors = if (isDarkTheme)
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MidnightBlue.copy(alpha = MidnightBlueAmbientAlpha),
                                AmoledSurface
                            )
                        else
                            listOf(
                                MaterialTheme.colorScheme.background,
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
                                0.00f to MaterialTheme.colorScheme.background,
                                0.55f to TitaniumDark.copy(alpha = streakAlpha),
                                // Titik kilau sempit (0.60-0.68) ditumpuk tepat setelah TitaniumDark
                                // — rentang fraction yang sengaja disempitkan (bukan disebar rata
                                // seperti resep 3-stop Tactile) supaya terbaca sebagai satu garis
                                // pantulan cahaya di logam, bukan gradasi warna yang mulus.
                                0.62f to SilverHighlight.copy(alpha = streakAlpha * 1.8f),
                                0.68f to TitaniumDark.copy(alpha = streakAlpha),
                                0.76f to emerald.copy(alpha = emeraldStreakAlpha),
                                1.00f to streakEnd
                            )
                        )                    }
                    else -> null
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (identityRootBrush != null) Modifier.background(identityRootBrush) else Modifier
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
            "Audio Player",
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

@Composable
private fun AppNavHost(playerViewModel: PlayerViewModel, biometricAvailable: Boolean) {
    val navController = rememberNavController()
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val favoriteIds by playerViewModel.favoriteIds.collectAsStateWithLifecycle()
    val sleepTimerRemaining by playerViewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
    val statsVersion by playerViewModel.statsVersion.collectAsStateWithLifecycle()
    val playlists by playerViewModel.playlists.collectAsStateWithLifecycle()
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

    val deleteContext = LocalContext.current
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // On success the system has already deleted the files; just refresh our own scan
        // so they disappear from the library too. On cancel, nothing was deleted — no-op.
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            playerViewModel.refreshLibrary()
        }
    }

    fun deleteSongsFromDevice(songs: List<com.rudi.audioplayer.data.Song>) {
        if (songs.isEmpty()) return
        val resolver = deleteContext.contentResolver
        val uris = songs.map { it.uri }
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+: the only correct path — the system shows its own confirmation
                // and handles the actual deletion; we never touch the files directly.
                val pendingIntent = android.provider.MediaStore.createDeleteRequest(resolver, uris)
                deleteRequestLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                // Android 10: delete() throws a RecoverableSecurityException carrying the
                // exact system confirmation prompt to launch for files this app doesn't own.
                try {
                    uris.forEach { resolver.delete(it, null, null) }
                    playerViewModel.refreshLibrary()
                } catch (e: RecoverableSecurityException) {
                    deleteRequestLauncher.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                }
            }
            else -> {
                // Pre-Android 10: no scoped-storage confirmation flow exists yet; a direct
                // delete (content resolver + backing file) is the standard approach.
                uris.forEach { uri ->
                    try {
                        resolver.delete(uri, null, null)
                    } catch (e: Exception) {
                        // Leave library state consistent even if one file couldn't be removed
                        // (e.g. already gone) — refreshLibrary() below re-syncs regardless.
                    }
                }
                playerViewModel.refreshLibrary()
            }
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

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
                    visible = uiState.currentSong != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    MiniPlayerBar(
                        uiState = uiState,
                        accentColor = accentColor,
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onExpand = {
                            navController.navigate("now_playing") { launchSingleTop = true }
                        }
                    )
                }
                if (currentRoute == "home" || currentRoute == "library" || currentRoute == "settings") {
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
                    NavigationBar(
                        modifier = if (navCatchLightColor != null)
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
                        else Modifier,
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
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Beranda") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "library",
                            onClick = {
                                navController.navigate("library") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                            label = { Text("Perpustakaan") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = {
                                navController.navigate("settings") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text("Pengaturan") }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
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
                    customFolders = customFolders,
                    onAddCustomFolder = { uri -> playerViewModel.addCustomFolder(uri) },
                    onRemoveCustomFolder = { uri -> playerViewModel.removeCustomFolder(uri) },
                    onDeleteSongs = { songs -> deleteSongsFromDevice(songs) },
                    onInfoMessage = { message -> playerViewModel.showInfoMessage(message) }
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
                    onInfoMessage = { message -> playerViewModel.showInfoMessage(message) }
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
                NowPlayingScreen(
                    uiState = uiState,
                    isFavorite = uiState.currentSong?.let { favoriteIds.contains(it.id) } ?: false,
                    currentRating = currentRating,
                    onSetRating = { stars -> playerViewModel.setCurrentSongRating(stars) },
                    sleepTimerRemainingMs = sleepTimerRemaining,
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
                    equalizerState = equalizerState,
                    onOpenEqualizer = { playerViewModel.ensureEqualizerAttached() },
                    onToggleEqualizerEnabled = { playerViewModel.setEqualizerEnabled(it) },
                    onEqualizerBandChange = { band, level -> playerViewModel.setEqualizerBand(band, level) },
                    onEqualizerPresetSelect = { index -> playerViewModel.useEqualizerPreset(index) },
                    onEqualizerBoldPresetSelect = { preset -> playerViewModel.useBoldEqualizerPreset(preset) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun PermissionRationale(onRequest: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
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
