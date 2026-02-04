package com.fenitra.music.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
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
    private var mediaPlayer: MediaPlayer? = null
    private var positionUpdateJob: Job? = null

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    var onCompletion: (() -> Unit)? = null

    fun playSong(filePath: String, onError: (String) -> Unit = {}) {
        try {
            stop()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(filePath))
                prepare()
                start()

                setOnCompletionListener {
                    _isPlaying.value = false
                    onCompletion?.invoke()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    onError("Erreur de lecture: $what")
                    true
                }
            }

            _isPlaying.value = true
            startPositionUpdate()

        } catch (e: Exception) {
            Log.e(TAG, "Error playing song", e)
            onError("Impossible de lire la chanson: ${e.message}")
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            stopPositionUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            _isPlaying.value = true
            startPositionUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming", e)
        }
    }

    fun seekTo(position: Long) {
        try {
            mediaPlayer?.seekTo(position.toInt())
            _currentPosition.value = position
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }

    fun stop() {
        try {
            stopPositionUpdate()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            _isPlaying.value = false
            _currentPosition.value = 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping", e)
        }
    }

    fun getDuration(): Long {
        return try {
            mediaPlayer?.duration?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun startPositionUpdate() {
        stopPositionUpdate()
        positionUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && _isPlaying.value) {
                try {
                    val position = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    _currentPosition.value = position
                    delay(500) // Mise à jour toutes les 500ms
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
        stop()
    }
}