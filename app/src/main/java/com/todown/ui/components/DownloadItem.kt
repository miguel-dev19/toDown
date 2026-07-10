package com.todown.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.todown.data.local.DownloadEntity
import com.todown.ui.theme.*

@Composable
fun DownloadItem(
    download: DownloadEntity,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant)
            ) {
                if (!download.thumbnailUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = download.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        tint = Gray
                    )
                }
                if (download.status == "completed") {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Black.copy(alpha = 0.4f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, null, tint = White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = White
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                when (download.status) {
                    "downloading" -> {
                        LinearProgressIndicator(
                            progress = { download.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = Blue,
                            trackColor = SurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${download.progress}%", style = MaterialTheme.typography.bodySmall, color = Gray)
                    }
                    "completed" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Completado", style = MaterialTheme.typography.bodySmall, color = Green)
                        }
                    }
                    "paused" -> {
                        Text("Pausado - ${download.progress}%", style = MaterialTheme.typography.bodySmall, color = Yellow)
                    }
                    "error" -> {
                        Text("Error", style = MaterialTheme.typography.bodySmall, color = Red)
                    }
                    "canceled" -> {
                        Text("Cancelado", style = MaterialTheme.typography.bodySmall, color = Gray)
                    }
                }
            }
            
            when (download.status) {
                "completed" -> {
                    IconButton(onClick = onPlay) {
                        Icon(Icons.Default.PlayCircle, "Reproducir", tint = Green)
                    }
                }
                "downloading" -> {
                    IconButton(onClick = onPause) {
                        Icon(Icons.Default.PauseCircle, "Pausar", tint = Yellow)
                    }
                }
                "paused" -> {
                    IconButton(onClick = onResume) {
                        Icon(Icons.Default.PlayCircle, "Reanudar", tint = Blue)
                    }
                }
                "error" -> {
                    IconButton(onClick = onResume) {
                        Icon(Icons.Default.Refresh, "Reintentar", tint = Red)
                    }
                }
            }
            
            if (download.status != "completed") {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, "Cancelar", tint = Gray)
                }
            }
        }
    }
}
