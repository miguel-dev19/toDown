package com.todown.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todown.data.local.PreferencesManager
import com.todown.network.xmpp.ConnectionState
import com.todown.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    storagePath: String,
    connectionState: ConnectionState,
    phoneNumber: String,
    preferencesManager: PreferencesManager,
    onChangeStoragePath: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentPath by remember { mutableStateOf(storagePath) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    
    val directoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = it.toString()
            currentPath = path
            scope.launch {
                preferencesManager.saveStoragePath(path)
                onChangeStoragePath(path)
            }
        }
    }
    
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuracion", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Almacenamiento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Blue, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Blue.copy(alpha = 0.12f)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Folder, null, tint = Blue, modifier = Modifier.size(22.dp)) }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ubicacion de descargas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(currentPath, style = MaterialTheme.typography.bodySmall, color = Gray, maxLines = 1)
                        }
                        FilledTonalButton(onClick = { directoryPicker.launch(null) }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Blue.copy(alpha = 0.15f), contentColor = Blue), shape = RoundedCornerShape(10.dp)) {
                            Text("Cambiar", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Conexion", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Blue, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier.size(44.dp), shape = CircleShape,
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> Green.copy(alpha = 0.12f)
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Yellow.copy(alpha = 0.12f)
                                else -> Red.copy(alpha = 0.12f)
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    when (connectionState) {
                                        ConnectionState.CONNECTED -> Icons.Default.CheckCircle
                                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Icons.Default.Sync
                                        else -> Icons.Default.Cancel
                                    }, null, Modifier.size(22.dp),
                                    tint = when (connectionState) {
                                        ConnectionState.CONNECTED -> Green
                                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Yellow
                                        else -> Red
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                when (connectionState) {
                                    ConnectionState.CONNECTED -> "Conectado"
                                    ConnectionState.CONNECTING -> "Conectando..."
                                    ConnectionState.RECONNECTING -> "Reconectando..."
                                    ConnectionState.DISCONNECTED -> "Desconectado"
                                    ConnectionState.FAILED -> "Error"
                                },
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(phoneNumber, style = MaterialTheme.typography.bodySmall, color = Gray)
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceVariant)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Purple.copy(alpha = 0.12f)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Info, null, tint = Purple, modifier = Modifier.size(22.dp)) }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("toDown v1.0", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Gestor de descargas ToDus", style = MaterialTheme.typography.bodySmall, color = Gray)
                        }
                    }
                }
            }
        }
    }
}
