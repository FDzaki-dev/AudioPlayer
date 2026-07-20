package com.rudi.audioplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
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
import com.rudi.audioplayer.ui.LibraryScreen
import com.rudi.audioplayer.ui.MiniPlayerBar
import com.rudi.audioplayer.ui.NowPlayingScreen
import com.rudi.audioplayer.ui.theme.AudioPlayerTheme

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(applicationContext) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        playerViewModel.connect()

        setContent {
            AudioPlayerTheme {
                val context = LocalContext.current

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

                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        hasPermission -> AppNavHost(playerViewModel)
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
private fun AppNavHost(playerViewModel: PlayerViewModel) {
    val navController = rememberNavController()
    val uiState by playerViewModel.uiState.collectAsState()
    val favoriteIds by playerViewModel.favoriteIds.collectAsState()
    val sleepTimerRemaining by playerViewModel.sleepTimerRemaining.collectAsState()
    val statsVersion by playerViewModel.statsVersion.collectAsState()
    val playlists by playerViewModel.playlists.collectAsState()
    val accentColor by playerViewModel.accentColor.collectAsState()
    val equalizerState by playerViewModel.equalizerState.collectAsState()
    val crossfadeEnabled by playerViewModel.crossfadeEnabled.collectAsState()
    val customFolders by playerViewModel.customFolders.collectAsState()
    val librarySongs by playerViewModel.librarySongs.collectAsState()
    val libraryLoading by playerViewModel.libraryLoading.collectAsState()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
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
                        onExpand = { navController.navigate("now_playing") }
                    )
                }
                if (currentRoute == "home" || currentRoute == "library") {
                    NavigationBar {
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
                    onRemoveCustomFolder = { uri -> playerViewModel.removeCustomFolder(uri) }
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
