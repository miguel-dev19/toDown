package com.todown.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.todown.ui.theme.Blue
import com.todown.utils.FileUtils.formatFileSize
import com.todown.ui.theme.Gray
import com.todown.utils.FileUtils.formatFileSize
import com.todown.ui.theme.White
import com.todown.utils.FileUtils.formatFileSize
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoFile: File,
    fileName: String,
    fileSize: Long,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(videoFile)))
            prepare()
            playWhenReady = true
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
    
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        TopAppBar(
            title = { Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = White)
                }
            },
            actions = {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, "Compartir", tint = White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.8f),
                titleContentColor = White
            )
        )
        
        Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.Black.copy(alpha = 0.9f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatFileSize(fileSize), style = MaterialTheme.typography.bodySmall, color = Gray)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, "Compartir", tint = Blue)
                }
            }
        }
    }
}

