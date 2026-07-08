package com.todown.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todown.network.xmpp.BotState
import com.todown.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkBottomSheet(
    onDismiss: () -> Unit,
    onSendUrl: (String) -> Unit,
    isProcessing: Boolean,
    botState: BotState?,
    errorMessage: String?
) {
    var url by remember { mutableStateOf("") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp).height(4.dp)
                    .background(Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            if (!isProcessing && botState == null) {
                Icon(Icons.Default.Link, null, modifier = Modifier.size(48.dp), tint = Blue)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Agregar video", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ingresa la URL del video", style = MaterialTheme.typography.bodyMedium, color = Gray)
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://...", color = Gray.copy(alpha = 0.5f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue,
                        unfocusedBorderColor = SurfaceVariant,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { onSendUrl(url) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = url.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continuar", style = MaterialTheme.typography.titleMedium)
                }
            } else if (errorMessage != null) {
                Icon(Icons.Default.Error, null, modifier = Modifier.size(48.dp), tint = Red)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Error", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Red)
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = Gray)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Cancelar")
                    }
                    Button(onClick = { onSendUrl(url) }, modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                        Text("Reintentar")
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = Blue, strokeWidth = 4.dp)
                Spacer(modifier = Modifier.height(24.dp))
                
                AnimatedContent(targetState = botState) { state ->
                    when (state) {
                        BotState.ANALYZING -> StatusRow(Icons.Default.Search, "Analizando enlace...", Blue)
                        BotState.DOWNLOADING -> StatusRow(Icons.Default.Download, "Descargando video...", Blue)
                        BotState.PROCESSING -> StatusRow(Icons.Default.Image, "Procesando thumbnail...", Purple)
                        BotState.UPLOADING -> StatusRow(Icons.Default.CloudUpload, "Subiendo video...", Yellow)
                        BotState.READY -> StatusRow(Icons.Default.CheckCircle, "Video listo", Green)
                        else -> StatusRow(Icons.Default.HourglassEmpty, "Procesando...", Gray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatusRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = White)
    }
}
