package com.fenitra.music.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun SplashScreenWithWaves(
    onSplashFinished: () -> Unit
) {
    // Animation de scale
    val scale = remember { Animatable(0f) }

    // Animation d'alpha
    val alpha = remember { Animatable(0f) }

    // Animation des ondes sonores
    val infiniteTransition = rememberInfiniteTransition(label = "waves")

    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )

    LaunchedEffect(key1 = true) {
        // Animation du logo
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        // Animation du texte
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000)
        )

        // Attendre puis terminer
        delay(3000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ondes sonores en arrière-plan
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2

            // Dessiner 3 cercles ondulants
            listOf(wave1, wave2, wave3).forEachIndexed { index, wave ->
                val radius = 100f + (wave * 200f) + (index * 50f)
                val alpha = 1f - wave

                drawCircle(
                    color = Color(0xFF4A9FD8).copy(alpha = alpha * 0.3f),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3f)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo avec animation
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale.value),
                contentAlignment = Alignment.Center
            ) {
                // Fond circulaire
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4A9FD8).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )

                // Icône de musique
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music",
                    modifier = Modifier.size(90.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Nom de l'application
            Text(
                text = "Fenitra Music",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Slogan
            Text(
                text = "Feel the rhythm",
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF6BB8E8),
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}