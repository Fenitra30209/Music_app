package com.fenitra.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fenitra.music.data.entity.Song
import com.fenitra.music.ui.components.SongItem
import com.fenitra.music.ui.components.MiniPlayer
import com.fenitra.music.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToNowPlaying: () -> Unit
) {
    val songs by viewModel.allSongs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Music") },
                actions = {
                    IconButton(onClick = onNavigateToFavorites) {
                        Icon(Icons.Default.Favorite, contentDescription = "Favorites")
                    }
                    IconButton(onClick = onNavigateToPlaylists) {
                        Icon(Icons.Default.PlaylistPlay, contentDescription = "Playlists")
                    }
                }
            )
        },
        bottomBar = {
            // Afficher le MiniPlayer seulement si une chanson est en cours
            if (currentSong != null) {
                MiniPlayer(
                    viewModel = viewModel,
                    onExpandClick = onNavigateToNowPlaying
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barre de recherche
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchSongs(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search songs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.searchSongs("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            // Liste des chansons
            val displaySongs = if (searchQuery.isEmpty()) songs else searchResults

            if (displaySongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No songs found" else "No results",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = displaySongs,
                        key = { it.id }
                    ) { song ->
                        SongItem(
                            song = song,
                            onClick = {
                                // Lancer la chanson avec la liste actuelle
                                viewModel.playSong(song, displaySongs)
                                onNavigateToNowPlaying()
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(song) }
                        )
                    }
                }
            }
        }
    }
}