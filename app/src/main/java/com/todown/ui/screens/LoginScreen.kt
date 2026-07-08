package com.todown.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.todown.ui.theme.Blue
import com.todown.ui.theme.Red

@Composable
fun LoginScreen(
    onLogin: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?
) {
    var phoneNumber by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Blue
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Acceso a toDown",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ingresa tu numero de telefono de ToDus",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Numero de telefono") },
                prefix = { Text("+53 ", color = Blue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = errorMessage != null,
                supportingText = { if (errorMessage != null) Text(errorMessage, color = Red) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onLogin(phoneNumber) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = phoneNumber.length == 8 && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Continuar", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
