package com.todown.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.todown.network.xmpp.BotState
import com.todown.network.xmpp.ConnectionState
import com.todown.network.xmpp.OwnerProfile
import com.todown.ui.components.DownloadItem
import com.todown.ui.components.LinkBottomSheet
import com.todown.ui.theme.*
import com.todown.utils.FileUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    downloads: List<DownloadEntity>,
    connectionState: ConnectionState,
    phoneNumber: String,
    ownerProfile: OwnerProfile?,
    onPlayVideo: (DownloadEntity) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onSendUrl: (String) -> Unit,
    onClearCompleted: () -> Unit,
    completedCount: Int,
    totalSize: Long,
    onSettingsClick: () -> Unit,
    isProcessing: Boolean,
    botState: BotState?,
    errorMessage: String?,
    onClearBotState: () -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = Surface
            ) {
                Column(modifier = Modifier.fillMaxHeight()) {
                    // Header con perfil
                    Box(modifier = Modifier.fillMaxWidth().background(SurfaceVariant).padding(24.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Avatar
                                Box(modifier = Modifier.size(56.dp)) {
                                    if (ownerProfile?.thumbUrl?.isNotEmpty() == true) {
                                        AsyncImage(
                                            model = ownerProfile.thumbUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            shape = CircleShape,
                                            color = Blue
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    (ownerProfile?.alias?.firstOrNull()?.toString() ?: phoneNumber.takeLast(1)).uppercase(),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = White
                                                )
                                            }
                                        }
                                    }
                                    // Indicador de conexión
                                    Box(
                                        modifier = Modifier.size(14.dp).align(Alignment.BottomEnd)
                                            .background(
                                                when (connectionState) {
                                                    ConnectionState.CONNECTED -> Green
                                                    ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Yellow
                                                    else -> Red
                                                },
                                                CircleShape
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        ownerProfile?.alias?.ifEmpty { "toDown" } ?: "toDown",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                    if (ownerProfile?.toDusId?.isNotEmpty() == true) {
                                        Text("@${ownerProfile.toDusId}", style = MaterialTheme.typography.bodySmall, color = Blue)
                                    }
                                    Text(phoneNumber, style = MaterialTheme.typography.bodySmall, color = Gray)
                                }
                            }
                            
                            if (ownerProfile?.bio?.isNotEmpty() == true) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    ownerProfile.bio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    when (connectionState) {
                                        ConnectionState.CONNECTED -> Icons.Default.CheckCircle
                                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Icons.Default.Sync
                                        else -> Icons.Default.Cancel
                                    },
                                    null, Modifier.size(14.dp),
                                    tint = when (connectionState) {
                                        ConnectionState.CONNECTED -> Green
                                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Yellow
                                        else -> Red
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    when (connectionState) {
                                        ConnectionState.CONNECTED -> "Conectado"
                                        ConnectionState.CONNECTING -> "Conectando..."
                                        ConnectionState.RECONNECTING -> "Reconectando..."
                                        ConnectionState.DISCONNECTED -> "Desconectado"
                                        ConnectionState.FAILED -> "Error"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (connectionState) {
                                        ConnectionState.CONNECTED -> Green
                                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Yellow
                                        else -> Red
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Estadísticas
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CheckCircle, null, tint = Green) },
                        label = { Text("Total: $completedCount descargas") },
                        selected = false, onClick = { }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Storage, null, tint = Blue) },
                        label = { Text("Espacio: ${FileUtils.formatFileSize(totalSize)}") },
                        selected = false, onClick = { }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Configuracion") },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() }; onSettingsClick() }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Delete, null, tint = Red) },
                        label = { Text("Limpiar completados", color = Red) },
                        selected = false, onClick = onClearCompleted
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, null, tint = Blue, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("toDown", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showBottomSheet = true }, containerColor = Blue) {
                    Icon(Icons.Default.Add, "Agregar")
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (downloads.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudDownload, null, Modifier.size(80.dp), tint = Gray.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No hay descargas", color = Gray)
                            Text("Toca + para agregar", color = Gray.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(downloads, key = { it.id }) { download ->
                            DownloadItem(
                                download = download,
                                onPlay = { onPlayVideo(download) },
                                onPause = { onPauseDownload(download.downloadId) },
                                onResume = { onResumeDownload(download.downloadId) },
                                onCancel = { onCancelDownload(download.downloadId) }
                            )
                        }
                    }
                }
            }
        }
        
        if (showBottomSheet) {
            LinkBottomSheet(
                onDismiss = { showBottomSheet = false; onClearBotState() },
                onSendUrl = onSendUrl, isProcessing = isProcessing,
                botState = botState, errorMessage = errorMessage
            )
        }
    }
}
