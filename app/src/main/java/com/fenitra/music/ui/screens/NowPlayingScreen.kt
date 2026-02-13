package com.fenitra.music.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fenitra.music.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val isFavorite by viewModel.currentSongIsFavorite.collectAsState()

    // Animation de rotation du disque
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 20000, // 20 secondes pour une rotation complète
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Animation pour les barres vocales (effet de rythme)
    val barHeights = remember { List(40) { 0f } }
    val animatedBarHeights = barHeights.map { initialHeight ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (300..800).random(),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "barHeight"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD5E8F0),
                        Color(0xFFE8F4F8),
                        Color(0xFFF5FBFD)
                    )
                )
            )
    ) {
        currentSong?.let { song ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Song info at top
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp, start = 28.dp, end = 28.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = song.title.uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A3D5C),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 26.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = song.artist,
                        fontSize = 14.sp,
                        color = Color(0xFF5B8AAE),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (song.album.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.album,
                            fontSize = 12.sp,
                            color = Color(0xFF8BB4D1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Large circular album art - 20% HIDDEN on the right (même taille que la barre de contrôle)
                Box(
                    modifier = Modifier
                        .size(340.dp) // Même hauteur que la barre de boutons
                        .align(Alignment.CenterEnd)
                        .offset(x = 68.dp, y = 0.dp) // 20% caché (340 * 0.20 = 68dp), centré verticalement
                        .rotate(if (isPlaying) rotation else 0f) // Rotation uniquement si en lecture
                ) {
                    // White outer ring
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = 20.dp,
                                shape = CircleShape,
                                ambientColor = Color(0xFF4A9FD8).copy(alpha = 0.15f)
                            )
                            .clip(CircleShape)
                            .background(Color.White)
                    )

                    // Inner album art
                    Box(
                        modifier = Modifier
                            .size(310.dp) // Ajusté proportionnellement (340 - 30)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF6E6E73),
                                        Color(0xFF2C2C2E)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.albumArt != null) {
                            AsyncImage(
                                model = song.albumArt,
                                contentDescription = "Album Art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(140.dp),
                                tint = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Vertical control bar CENTERED on left side
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(340.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = 28.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(30.dp),
                            ambientColor = Color.Black.copy(alpha = 0.08f)
                        )
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Color.White.copy(alpha = 0.9f)
                        )
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Repeat/Shuffle
                        CompactControlButton(
                            icon = Icons.Default.Shuffle,
                            tint = Color(0xFF7EB3D6),
                            onClick = { }
                        )

                        // Previous
                        CompactControlButton(
                            icon = Icons.Default.SkipPrevious,
                            tint = Color(0xFF4A9FD8),
                            onClick = { viewModel.playPrevious() }
                        )

                        // Play/Pause
                        CompactControlButton(
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            tint = Color(0xFF4A9FD8),
                            onClick = { viewModel.togglePlayPause() }
                        )

                        // Next
                        CompactControlButton(
                            icon = Icons.Default.SkipNext,
                            tint = Color(0xFF4A9FD8),
                            onClick = { viewModel.playNext() }
                        )

                        // Favorite
                        CompactControlButton(
                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            tint = if (isFavorite) Color(0xFFFF5B7D) else Color(0xFF7EB3D6),
                            onClick = { viewModel.toggleFavorite(song) }
                        )
                    }
                }

                // Back button (top left)
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = Color(0xFF1A3D5C),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Bottom section: Progress bar and animated vocal bars
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 28.dp, vertical = 32.dp)
                ) {
                    // Animated vocal bars (visualizer)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        animatedBarHeights.forEachIndexed { index, animatedHeight ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(
                                        if (isPlaying) animatedHeight.value else 0.2f
                                    )
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF4A9FD8),
                                                Color(0xFF7EB3D6)
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress slider
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = { newPosition ->
                                viewModel.seekTo(newPosition.toLong())
                            },
                            valueRange = 0f..(song.duration.toFloat()),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4A9FD8),
                                activeTrackColor = Color(0xFF4A9FD8),
                                inactiveTrackColor = Color(0xFF4A9FD8).copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Time labels
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                fontSize = 12.sp,
                                color = Color(0xFF5B8AAE)
                            )
                            Text(
                                text = formatTime(song.duration),
                                fontSize = 12.sp,
                                color = Color(0xFF5B8AAE)
                            )
                        }
                    }
                }
            }
        } ?: run {
            // No song playing
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF8BB4D1)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No song playing",
                        color = Color(0xFF8BB4D1),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}