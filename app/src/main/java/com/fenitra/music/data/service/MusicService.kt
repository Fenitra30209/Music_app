package com.fenitra.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.palette.graphics.Palette
import com.fenitra.music.MainActivity
import com.fenitra.music.R
import com.fenitra.music.data.entity.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicService : Service() {

    private val TAG = "MusicService"
    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    var onCompletionCallback: (() -> Unit)? = null

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
        createMediaSession()
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
            ACTION_NEXT -> {
                onCompletionCallback?.invoke()
            }
            ACTION_PREVIOUS -> {
                // Géré par le callback via notification
            }
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            isActive = true
        }
    }

    private fun createBasicNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Music Player")
        .setContentText("Ready to play")
        .setSmallIcon(R.drawable.ic_music_note)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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
                    _isPlaying.value = false
                    onCompletionCallback?.invoke()
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
            mediaSession?.isActive = false
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
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val song = _currentSong.value ?: return@launch

                // Extraire l'artwork de l'album
                val albumArt = getAlbumArt(song.filePath)

                // Créer la notification sur le thread principal
                withContext(Dispatchers.Main) {
                    val notification = buildNotification(song, albumArt)
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing notification", e)
            }
        }
    }

    private fun buildNotification(song: Song, albumArt: Bitmap?) =
        NotificationCompat.Builder(this, CHANNEL_ID).apply {
            // Informations de base
            setContentTitle(song.title)
            setContentText(song.artist)
            setSubText(song.album)
            setSmallIcon(R.drawable.ic_music_note)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Image de l'album
            if (albumArt != null) {
                setLargeIcon(albumArt)

                // Couleurs dynamiques basées sur l'artwork
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Palette.from(albumArt).generate { palette ->
                        palette?.let {
                            color = it.getDominantColor(0xFF1976D2.toInt())
                        }
                    }
                }
            } else {
                // Icône par défaut si pas d'artwork
                setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_music_note_large
                    )
                )
            }

            // Intent pour ouvrir l'application
            val contentIntent = PendingIntent.getActivity(
                this@MusicService,
                0,
                Intent(this@MusicService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setContentIntent(contentIntent)

            // Actions
            val playPauseIcon = if (_isPlaying.value) R.drawable.ic_pause else R.drawable.ic_play
            val playPauseText = if (_isPlaying.value) "Pause" else "Play"
            val playPauseAction = if (_isPlaying.value) ACTION_PAUSE else ACTION_PLAY

            addAction(
                R.drawable.ic_skip_previous,
                "Previous",
                getPendingIntent(ACTION_PREVIOUS)
            )
            addAction(
                playPauseIcon,
                playPauseText,
                getPendingIntent(playPauseAction)
            )
            addAction(
                R.drawable.ic_skip_next,
                "Next",
                getPendingIntent(ACTION_NEXT)
            )

            // Style média
            setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession?.sessionToken)
            )

            // Priorité et comportement
            priority = NotificationCompat.PRIORITY_LOW
            setOngoing(true)
            setShowWhen(false)
            setOnlyAlertOnce(true)

            // Bouton de fermeture (swipe)
            setDeleteIntent(getPendingIntent(ACTION_STOP))
        }.build()

    private fun getAlbumArt(filePath: String): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val art = retriever.embeddedPicture
            retriever.release()

            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                // Redimensionner pour économiser la mémoire
                Bitmap.createScaledBitmap(bitmap, 512, 512, true)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting album art", e)
            null
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
            mediaSession?.release()
            mediaSession = null
            Log.d(TAG, "Service onDestroy")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }
}