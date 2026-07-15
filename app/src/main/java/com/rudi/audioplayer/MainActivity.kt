package com.rudi.audioplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        playerViewModel.connect()

        setContent {
            AudioPlayerTheme {
                var hasPermission by remember { mutableStateOf(false) }

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

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    hasPermission = result[neededPermissions[0]] == true
                }

                LaunchedEffect(Unit) {
                    launcher.launch(neededPermissions)
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (hasPermission) {
                        AppNavHost(playerViewModel)
                    } else {
                        PermissionRationale { launcher.launch(neededPermissions) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(playerViewModel: PlayerViewModel) {
    val navController = rememberNavController()
    val uiState by playerViewModel.uiState.collectAsState()
    val favoriteIds by playerViewModel.favoriteIds.collectAsState()
    val sleepTimerRemaining by playerViewModel.sleepTimerRemaining.collectAsState()

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
                    favoriteIds = favoriteIds,
                    onSongClick = { songs, index -> playerViewModel.playQueue(songs, index) },
                    resumePreview = { songs -> playerViewModel.peekSavedSong(songs) },
                    onResumeClick = { songs -> playerViewModel.resumeFromSaved(songs) }
                )
            }
            composable("library") {
                LibraryScreen(
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
                    }
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
                    onPlayQueueIndex = { playerViewModel.playFromQueueIndex(it) },
                    onMoveQueueItem = { from, to -> playerViewModel.moveQueueItem(from, to) },
                    onRemoveFromQueue = { playerViewModel.removeFromQueue(it) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun PermissionRationale(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Izin akses musik dibutuhkan untuk menampilkan library kamu.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Berikan Izin") }
    }
}
