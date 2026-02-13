package com.fenitra.music.data.repository

import com.fenitra.music.data.dao.PlaylistDao
import com.fenitra.music.data.dao.SongDao
import com.fenitra.music.data.entity.Playlist
import com.fenitra.music.data.entity.PlaylistSongCrossRef
import com.fenitra.music.data.entity.Song
import kotlinx.coroutines.flow.Flow

class MusicRepository(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) {
    // Songs
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()
    val allArtists: Flow<List<String>> = songDao.getAllArtists()
    val allAlbums: Flow<List<String>> = songDao.getAllAlbums()

    suspend fun getSongById(id: Long) = songDao.getSongById(id)

    fun searchSongs(query: String) = songDao.searchSongs(query)

    fun getSongsByArtist(artist: String) = songDao.getSongsByArtist(artist)

    fun getSongsByAlbum(album: String) = songDao.getSongsByAlbum(album)

    suspend fun insertSong(song: Song) = songDao.insertSong(song)

    suspend fun insertSongs(songs: List<Song>) = songDao.insertSongs(songs)

    suspend fun updateSong(song: Song) = songDao.updateSong(song)

    suspend fun deleteSong(song: Song) = songDao.deleteSong(song)

    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) =
        songDao.updateFavoriteStatus(songId, isFavorite)

    // Playlists
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    val allPlaylistsWithSongs = playlistDao.getAllPlaylistsWithSongs()

    suspend fun getPlaylistById(id: Long) = playlistDao.getPlaylistById(id)

    fun getPlaylistWithSongs(playlistId: Long) = playlistDao.getPlaylistWithSongs(playlistId)

    suspend fun insertPlaylist(playlist: Playlist) = playlistDao.insertPlaylist(playlist)

    suspend fun updatePlaylist(playlist: Playlist) = playlistDao.updatePlaylist(playlist)

    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.deletePlaylist(playlist)

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long, position: Int = 0) {
        playlistDao.insertPlaylistSongCrossRef(
            PlaylistSongCrossRef(playlistId, songId, position)
        )
    }

    suspend fun upsertSongs(songs: List<Song>) {
        songDao.upsertSongs(songs)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.deletePlaylistSongCrossRef(
            PlaylistSongCrossRef(playlistId, songId)
        )
    }

    suspend fun clearPlaylist(playlistId: Long) =
        playlistDao.deleteAllSongsFromPlaylist(playlistId)
}