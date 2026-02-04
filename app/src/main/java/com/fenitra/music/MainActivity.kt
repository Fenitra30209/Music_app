package com.fenitra.music

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fenitra.music.service.MusicService
import com.fenitra.music.ui.screens.HomeScreen
import com.fenitra.music.ui.screens.NowPlayingScreen
import com.fenitra.music.ui.theme.MusicTheme
import com.fenitra.music.ui.viewmodel.MusicViewModel
import com.fenitra.music.util.MusicScanner

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private val viewModel: MusicViewModel by viewModels()
    private var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
            Log.d(TAG, "Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
            Log.d(TAG, "Service disconnected")
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Permission granted")
            scanMusic()
        } else {
            Log.e(TAG, "Permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        try {
            // Démarrer et lier le service
            val intent = Intent(this, MusicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

            // Vérifier les permissions
            checkPermissions()

            setContent {
                MusicTheme {
                    MusicApp(
                        viewModel = viewModel,
                        musicService = musicService
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
        }
    }

    private fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Permission already granted")
                scanMusic()
            }
            else -> {
                Log.d(TAG, "Requesting permission")
                permissionLauncher.launch(permission)
            }
        }
    }

    private fun scanMusic() {
        try {
            Log.d(TAG, "Scanning music...")
            val scanner = MusicScanner(this)
            val songs = scanner.scanAudioFiles()
            Log.d(TAG, "Found ${songs.size} songs")
            if (songs.isNotEmpty()) {
                viewModel.insertSongs(songs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning music", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (isBound) {
                unbindService(serviceConnection)
                isBound = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }
}

@Composable
fun MusicApp(
    viewModel: MusicViewModel,
    musicService: MusicService?
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToPlaylists = {
                    // TODO: Navigation vers les playlists
                },
                onNavigateToFavorites = {
                    // TODO: Navigation vers les favoris
                },
                onNavigateToNowPlaying = {
                    navController.navigate("nowPlaying")
                }
            )
        }

        composable("nowPlaying") {
            NowPlayingScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // TODO: Ajouter les routes pour playlists et favoris
        /*
        composable("playlists") {
            PlaylistsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("favorites") {
            FavoritesScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        */
    }
}