package com.fenitra.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fenitra.music.MainActivity
import com.fenitra.music.R
import com.fenitra.music.data.entity.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MusicService : Service() {

    private val TAG = "MusicService"
    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"
        const val ACTION_STOP = "ACTION_STOP"
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()

        // Créer une notification de base pour démarrer le service en foreground
        startForeground(NOTIFICATION_ID, createBasicNotification())
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service onBind")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand: ${intent?.action}")
        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun createBasicNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Music Player")
        .setContentText("Ready to play")
        .setSmallIcon(R.drawable.ic_music_note)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    fun playSong(song: Song) {
        try {
            Log.d(TAG, "Playing song: ${song.title}")
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(song.filePath)
                setOnPreparedListener {
                    start()
                    _isPlaying.value = true
                    Log.d(TAG, "MediaPlayer started")
                }
                setOnCompletionListener {
                    playNext()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    false
                }
                prepareAsync()
            }
            _currentSong.value = song
            showNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing song", e)
        }
    }

    fun play() {
        try {
            mediaPlayer?.start()
            _isPlaying.value = true
            showNotification()
            Log.d(TAG, "Play")
        } catch (e: Exception) {
            Log.e(TAG, "Error in play", e)
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            showNotification()
            Log.d(TAG, "Pause")
        } catch (e: Exception) {
            Log.e(TAG, "Error in pause", e)
        }
    }

    fun playNext() {
        Log.d(TAG, "Play next (not implemented)")
        // TODO: Implémenter avec playlist
    }

    fun playPrevious() {
        Log.d(TAG, "Play previous (not implemented)")
        // TODO: Implémenter avec playlist
    }

    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }

    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting position", e)
            0
        }
    }

    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting duration", e)
            0
        }
    }

    private fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            _isPlaying.value = false
            _currentSong.value = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            Log.d(TAG, "Service stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping service", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        try {
            val song = _currentSong.value ?: return

            val contentIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val playPauseAction = if (_isPlaying.value) {
                NotificationCompat.Action(
                    R.drawable.ic_pause,
                    "Pause",
                    getPendingIntent(ACTION_PAUSE)
                )
            } else {
                NotificationCompat.Action(
                    R.drawable.ic_play,
                    "Play",
                    getPendingIntent(ACTION_PLAY)
                )
            }

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_skip_previous, "Previous", getPendingIntent(ACTION_PREVIOUS))
                .addAction(playPauseAction)
                .addAction(R.drawable.ic_skip_next, "Next", getPendingIntent(ACTION_NEXT))
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            Log.d(TAG, "Service onDestroy")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }
}