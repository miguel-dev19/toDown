package com.todown.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todown.ui.theme.Blue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    hasSession: Boolean?,
    onFinished: (goToHome: Boolean) -> Unit
) {
    val scale = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )
        delay(500)
        // Si ya hay sesion, ir directo a Home
        onFinished(hasSession == true)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Blue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo circular con animacion
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale.value),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "toDown",
                        modifier = Modifier.size(52.dp),
                        tint = Blue
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "toDown",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                ),
                color = MaterialTheme.colorScheme.surface
            )
        }
    }
}
