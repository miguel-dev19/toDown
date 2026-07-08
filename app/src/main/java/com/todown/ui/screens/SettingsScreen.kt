package com.todown.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todown.network.xmpp.XMPPConnectionState
import com.todown.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    storagePath: String,
    simultaneousDownloads: Int,
    threadsPerDownload: Int,
    connectionState: XMPPConnectionState,
    phoneNumber: String,
    onChangeStoragePath: () -> Unit,
    onSimultaneousDownloadsChange: (Int) -> Unit,
    onThreadsPerDownloadChange: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuracion", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard(Icons.Default.Folder, "Ubicacion de almacenamiento", storagePath) {
                Button(onClick = onChangeStoragePath, colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                    Text("Cambiar")
                }
            }
            
            SettingsCard(Icons.Default.Downloading, "Descargas simultaneas", simultaneousDownloads.toString()) {
                Slider(
                    value = simultaneousDownloads.toFloat(),
                    onValueChange = { onSimultaneousDownloadsChange(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue)
                )
            }
            
            SettingsCard(Icons.Default.Speed, "Hilos por descarga", threadsPerDownload.toString()) {
                Slider(
                    value = threadsPerDownload.toFloat(),
                    onValueChange = { onThreadsPerDownloadChange(it.toInt()) },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue)
                )
            }
            
            SettingsCard(Icons.Default.DarkMode, "Tema oscuro", "Activado por defecto") {
                Switch(
                    checked = true,
                    onCheckedChange = { },
                    enabled = false,
                    colors = SwitchDefaults.colors(checkedThumbColor = Blue, checkedTrackColor = Blue.copy(alpha = 0.3f))
                )
            }
            
            SettingsCard(
                Icons.Default.Cloud,
                "Conexion XMPP",
                when (connectionState) {
                    is XMPPConnectionState.Connected -> "Conectado - $phoneNumber"
                    is XMPPConnectionState.Connecting -> "Conectando..."
                    is XMPPConnectionState.Disconnected -> "Desconectado"
                    is XMPPConnectionState.Error -> "Error: ${connectionState.message}"
                }
            ) {
                Icon(
                    when (connectionState) {
                        is XMPPConnectionState.Connected -> Icons.Default.CheckCircle
                        is XMPPConnectionState.Connecting -> Icons.Default.Sync
                        is XMPPConnectionState.Disconnected -> Icons.Default.Cancel
                        is XMPPConnectionState.Error -> Icons.Default.Error
                    },
                    null, Modifier.size(24.dp),
                    tint = when (connectionState) {
                        is XMPPConnectionState.Connected -> Green
                        is XMPPConnectionState.Connecting -> Yellow
                        else -> Red
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(icon, null, tint = Blue, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Gray)
                }
                trailing()
            }
        }
    }
}
