package com.todown.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.todown.network.xmpp.VideoData
import com.todown.ui.theme.*

@Composable
fun VideoPreviewCard(
    videoData: VideoData,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 80.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (videoData.thumbnailUrl.isNotEmpty()) {
                        AsyncImage(
                            model = videoData.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Black.copy(alpha = 0.3f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PlayCircle,
                                null,
                                tint = White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = videoData.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VideoFile, null, Modifier.size(14.dp), tint = Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            formatSize(videoData.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray
                        )
                    }
                    if (videoData.duration > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, null, Modifier.size(14.dp), tint = Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                formatDuration(videoData.duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Download, null, Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Descargar", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.2f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

fun formatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%d:%02d".format(min, sec)
}
