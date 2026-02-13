package com.fenitra.music.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.fenitra.music.data.entity.Song
import com.fenitra.music.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayerManager(private val context: Context) {

    private val TAG = "MusicPlayerManager"
    private var musicService: MusicService? = null
    private var isBound = false
    private var positionUpdateJob: Job? = null

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    var onCompletion: (() -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true

            // Configurer le callback de fin de lecture
            musicService?.onCompletionCallback = {
                onCompletion?.invoke()
            }

            // Synchroniser les états
            CoroutineScope(Dispatchers.Main).launch {
                musicService?.isPlaying?.collect { playing ->
                    _isPlaying.value = playing
                    if (playing) {
                        startPositionUpdate()
                    } else {
                        stopPositionUpdate()
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            musicService = null
            isBound = false
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        val intent = Intent(context, MusicService::class.java)
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun playSong(filePath: String, onError: (String) -> Unit = {}) {
        try {
            if (!isBound || musicService == null) {
                bindService()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(300)
                    playSongInternal(filePath, onError)
                }
            } else {
                playSongInternal(filePath, onError)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing song", e)
            onError("Impossible de lire la chanson: ${e.message}")
        }
    }

    private fun playSongInternal(filePath: String, onError: (String) -> Unit) {
        try {
            // Créer un objet Song temporaire (devrait venir du ViewModel)
            val song = Song(
                title = "Unknown",
                artist = "Unknown",
                album = "Unknown",
                duration = 0,
                filePath = filePath,
                dateAdded = System.currentTimeMillis()
            )
            musicService?.playSong(song)
            startPositionUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Error in playSongInternal", e)
            onError("Erreur: ${e.message}")
        }
    }

    fun playSongWithInfo(song: Song, onError: (String) -> Unit = {}) {
        try {
            if (!isBound || musicService == null) {
                bindService()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(300)
                    musicService?.playSong(song)
                    startPositionUpdate()
                }
            } else {
                musicService?.playSong(song)
                startPositionUpdate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing song with info", e)
            onError("Impossible de lire la chanson: ${e.message}")
        }
    }

    fun pause() {
        try {
            musicService?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing", e)
        }
    }

    fun resume() {
        try {
            musicService?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming", e)
        }
    }

    fun seekTo(position: Long) {
        try {
            musicService?.seekTo(position.toInt())
            _currentPosition.value = position
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }

    fun stop() {
        try {
            stopPositionUpdate()
            val intent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_STOP
            }
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping", e)
        }
    }

    fun getDuration(): Long {
        return try {
            musicService?.getDuration()?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun startPositionUpdate() {
        stopPositionUpdate()
        positionUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && _isPlaying.value) {
                try {
                    val position = musicService?.getCurrentPosition()?.toLong() ?: 0L
                    _currentPosition.value = position
                    delay(500)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating position", e)
                }
            }
        }
    }

    private fun stopPositionUpdate() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun release() {
        try {
            stopPositionUpdate()
            if (isBound) {
                context.unbindService(serviceConnection)
                isBound = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing", e)
        }
    }
}