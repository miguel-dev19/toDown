package com.todown.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todown.data.local.DownloadEntity
import com.todown.network.xmpp.BotState
import com.todown.network.xmpp.ConnectionState
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
                    Box(modifier = Modifier.fillMaxWidth().background(SurfaceVariant).padding(24.dp)) {
                        Column {
                            Box(modifier = Modifier.size(64.dp)) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = Blue
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(phoneNumber.takeLast(4), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = White)
                                    }
                                }
                                Box(
                                    modifier = Modifier.size(16.dp).align(Alignment.BottomEnd)
                                        .background(
                                            when (connectionState) {
                                                ConnectionState.CONNECTED -> Green
                                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Yellow
                                                else -> Red
                                            },
                                            MaterialTheme.shapes.extraLarge
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("toDown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(phoneNumber, style = MaterialTheme.typography.bodyMedium, color = Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    when (connectionState) {
                                        ConnectionState.CONNECTED -> Icons.Default.CheckCircle
                                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Icons.Default.Sync
                                        else -> Icons.Default.Cancel
                                    },
                                    null, Modifier.size(16.dp),
                                    tint = when (connectionState) {
                                        ConnectionState.CONNECTED -> Green
                                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Yellow
                                        else -> Red
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CheckCircle, null, tint = Green) },
                        label = { Text("Total: $completedCount") },
                        selected = false,
                        onClick = { }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Storage, null, tint = Blue) },
                        label = { Text(FileUtils.formatFileSize(totalSize)) },
                        selected = false,
                        onClick = { }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Configuracion") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onSettingsClick()
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Delete, null, tint = Red) },
                        label = { Text("Limpiar completados", color = Red) },
                        selected = false,
                        onClick = onClearCompleted
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
                onSendUrl = onSendUrl,
                isProcessing = isProcessing,
                botState = botState,
                errorMessage = errorMessage
            )
        }
    }
}
