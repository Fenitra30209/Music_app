package com.fenitra.music.util

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.fenitra.music.data.entity.Song
import com.fenitra.music.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicScanner(private val context: Context) {

    private val tag = "MusicScanner"

    suspend fun scanAudioFiles(repository: MusicRepository): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idColumn)

                // Vérifier si la chanson existe déjà dans la base de données
                val existingSong = repository.getSongById(mediaId)

                val song = if (existingSong != null) {
                    // IMPORTANT: Préserver TOUTES les données existantes
                    // Ne mettre à jour que les métadonnées qui peuvent changer
                    existingSong.copy(
                        title = cursor.getString(titleColumn) ?: existingSong.title,
                        artist = cursor.getString(artistColumn) ?: existingSong.artist,
                        album = cursor.getString(albumColumn) ?: existingSong.album,
                        duration = cursor.getLong(durationColumn),
                        filePath = cursor.getString(dataColumn) ?: existingSong.filePath
                        // isFavorite et albumArt sont préservés automatiquement par le copy
                    )
                } else {
                    // Nouvelle chanson
                    Song(
                        id = mediaId,
                        title = cursor.getString(titleColumn) ?: "Unknown",
                        artist = cursor.getString(artistColumn) ?: "Unknown Artist",
                        album = cursor.getString(albumColumn) ?: "Unknown Album",
                        duration = cursor.getLong(durationColumn),
                        filePath = cursor.getString(dataColumn),
                        dateAdded = cursor.getLong(dateColumn) * 1000,
                        isFavorite = false
                    )
                }

                songs.add(song)
            }
        }

        Log.d(tag, "Scanned ${songs.size} songs")
        return@withContext songs
    }
}