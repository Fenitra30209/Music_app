package com.fenitra.music.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenitra.music.data.database.MusicDatabase
import com.fenitra.music.data.entity.Playlist
import com.fenitra.music.data.entity.Song
import com.fenitra.music.data.repository.MusicRepository
import com.fenitra.music.player.MusicPlayerManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository
    private val musicPlayer: MusicPlayerManager

    // États de lecture - DÉCLARER EN PREMIER
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _currentPlaylist = MutableStateFlow<List<Song>>(emptyList())
    private var currentIndex = 0

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    init {
        val database = MusicDatabase.getDatabase(application)
        repository = MusicRepository(database.songDao(), database.playlistDao())
        musicPlayer = MusicPlayerManager(application)

        // Écouter les changements de position du player
        viewModelScope.launch {
            musicPlayer.currentPosition.collect { position ->
                _currentPosition.value = position
            }
        }

        // Gérer la fin de lecture
        musicPlayer.onCompletion = {
            onSongComplete()
        }
    }

    val allSongs: StateFlow<List<Song>> = repository.allSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val searchResults: StateFlow<List<Song>> = _searchQuery.flatMapLatest { query ->
        if (query.isEmpty()) {
            flowOf(emptyList())
        } else {
            repository.searchSongs(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isPlaying: StateFlow<Boolean> = musicPlayer.isPlaying

    val currentSongIsFavorite: StateFlow<Boolean> = _currentSong.flatMapLatest { song ->
        if (song != null) {
            allSongs.map { songs -> songs.find { it.id == song.id }?.isFavorite ?: false }
        } else {
            flowOf(false)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // Fonctions de recherche
    fun searchSongs(query: String) {
        _searchQuery.value = query
    }

    // Fonctions CRUD pour les chansons
    fun insertSong(song: Song) = viewModelScope.launch {
        repository.insertSong(song)
    }

    fun insertSongs(songs: List<Song>) = viewModelScope.launch {
        repository.insertSongs(songs)
    }

    fun deleteSong(song: Song) = viewModelScope.launch {
        repository.deleteSong(song)
    }

    fun toggleFavorite(song: Song) = viewModelScope.launch {
        repository.toggleFavorite(song.id, !song.isFavorite)
    }

    // Fonctions pour les playlists
    fun createPlaylist(name: String, description: String = "") = viewModelScope.launch {
        val playlist = Playlist(name = name, description = description)
        repository.insertPlaylist(playlist)
    }

    fun deletePlaylist(playlist: Playlist) = viewModelScope.launch {
        repository.deletePlaylist(playlist)
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) = viewModelScope.launch {
        repository.addSongToPlaylist(playlistId, songId)
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) = viewModelScope.launch {
        repository.removeSongFromPlaylist(playlistId, songId)
    }

    fun getPlaylistWithSongs(playlistId: Long) =
        repository.getPlaylistWithSongs(playlistId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // ========== FONCTIONS DE LECTURE ==========

    fun playSong(song: Song, playlist: List<Song> = emptyList()) {
        _currentSong.value = song
        _currentPlaylist.value = if (playlist.isEmpty()) allSongs.value else playlist
        currentIndex = _currentPlaylist.value.indexOfFirst { it.id == song.id }

        // Lancer la lecture audio réelle
        musicPlayer.playSong(song.filePath) { error ->
            android.util.Log.e("MusicViewModel", error)
        }
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            musicPlayer.pause()
        } else {
            musicPlayer.resume()
        }
    }

    fun playNext() {
        val playlist = _currentPlaylist.value
        if (playlist.isEmpty()) return

        when {
            _isShuffleEnabled.value -> {
                currentIndex = (0 until playlist.size).random()
            }
            currentIndex < playlist.size - 1 -> {
                currentIndex++
            }
            _repeatMode.value == RepeatMode.ALL -> {
                currentIndex = 0
            }
            else -> {
                musicPlayer.stop()
                return
            }
        }

        val nextSong = playlist[currentIndex]
        _currentSong.value = nextSong
        musicPlayer.playSong(nextSong.filePath)
    }

    fun playPrevious() {
        val playlist = _currentPlaylist.value
        if (playlist.isEmpty()) return

        if (_currentPosition.value > 3000) {
            musicPlayer.seekTo(0L)
            return
        }

        when {
            currentIndex > 0 -> {
                currentIndex--
            }
            _repeatMode.value == RepeatMode.ALL -> {
                currentIndex = playlist.size - 1
            }
            else -> {
                musicPlayer.seekTo(0L)
                return
            }
        }

        val previousSong = playlist[currentIndex]
        _currentSong.value = previousSong
        musicPlayer.playSong(previousSong.filePath)
    }

    fun seekTo(position: Long) {
        musicPlayer.seekTo(position)
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun onSongComplete() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _currentSong.value?.let { song ->
                    musicPlayer.playSong(song.filePath)
                }
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                playNext()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayer.release()
    }
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}