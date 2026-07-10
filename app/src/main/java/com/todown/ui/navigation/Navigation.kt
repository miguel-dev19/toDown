package com.todown.ui.navigation

import android.os.Environment
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.todown.data.local.DownloadDatabase
import com.todown.data.local.PreferencesManager
import com.todown.data.repository.DownloadRepository
import com.todown.network.auth.JwtAuthenticator
import com.todown.network.xmpp.XMPPDataSource
import com.todown.ui.screens.*
import com.todown.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ToDownNavigation(navController: NavHostController) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val xmppDataSource = remember { XMPPDataSource() }
    val jwtAuthenticator = remember { JwtAuthenticator() }
    
    // Verificar si hay sesion guardada
    val hasSession = remember { mutableStateOf<Boolean?>(null) }
    
    LaunchedEffect(Unit) {
        val jwt = preferencesManager.jwtToken.collect { it }
        hasSession.value = jwt != null
    }
    
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                hasSession = hasSession.value,
                onFinished = { goToHome ->
                    if (goToHome) {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("welcome") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable("welcome") {
            WelcomeScreen(
                onContinue = { navController.navigate("login") { popUpTo("welcome") { inclusive = true } } }
            )
        }
        
        composable("login") {
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()
            
            LoginScreen(
                onLogin = { phone ->
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        jwtAuthenticator.authenticate(phone).onSuccess { jwt ->
                            preferencesManager.saveJwtToken(jwt)
                            preferencesManager.savePhoneNumber(phone)
                            isLoading = false
                            navController.navigate("home") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }.onFailure {
                            isLoading = false
                            errorMessage = "Error de autenticacion"
                        }
                    }
                },
                isLoading = isLoading,
                errorMessage = errorMessage
            )
        }
        
        composable("home") {
            val phoneNumber = preferencesManager.phoneNumber.collectAsState(initial = "")
            val completedCount = remember { mutableStateOf(0) }
            val totalSize = remember { mutableStateOf(0L) }
            
            val database = remember { DownloadDatabase.getInstance(context) }
            val downloadRepository = remember { DownloadRepository(database.downloadDao()) }
            
            val homeViewModel: HomeViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return HomeViewModel(downloadRepository, xmppDataSource) as T
                    }
                }
            )
            
            val downloads by homeViewModel.downloads.collectAsState()
            val connectionState by homeViewModel.connectionState.collectAsState()
            val botState by homeViewModel.botState.collectAsState()
            val errorMessage by homeViewModel.errorMessage.collectAsState()
            val isProcessing by homeViewModel.isProcessing.collectAsState()
            
            // Auto-conectar XMPP al iniciar
            LaunchedEffect(Unit) {
                val jwt = preferencesManager.jwtToken.collect { it }
                val phone = preferencesManager.phoneNumber.collect { it }
                if (jwt != null && phone != null) {
                    xmppDataSource.connect(phone, jwt)
                }
            }
            
            LaunchedEffect(downloads) {
                completedCount.value = downloadRepository.getCompletedCount()
                totalSize.value = downloadRepository.getTotalSize()
            }
            
            HomeScreen(
                downloads = downloads,
                connectionState = connectionState,
                phoneNumber = phoneNumber.value ?: "",
                onPlayVideo = { download ->
                    val file = File(download.filePath)
                    if (file.exists()) {
                        navController.navigate("player/${download.fileName}/${download.totalSize}")
                    }
                },
                onPauseDownload = { homeViewModel.pauseDownload(it) },
                onResumeDownload = { homeViewModel.resumeDownload(it) },
                onCancelDownload = { homeViewModel.cancelDownload(it) },
                onSendUrl = { homeViewModel.sendUrlToBot(it) },
                onClearCompleted = { homeViewModel.clearCompleted() },
                completedCount = completedCount.value,
                totalSize = totalSize.value,
                onSettingsClick = { navController.navigate("settings") },
                isProcessing = isProcessing,
                botState = botState,
                errorMessage = errorMessage,
                onClearBotState = { homeViewModel.clearBotState() }
            )
        }
        
        composable("settings") {
            val storagePath = preferencesManager.storagePath.collectAsState(initial = "Movies/toDown")
            
            SettingsScreen(
                onBack = { navController.popBackStack() },
                storagePath = storagePath.value,
                connectionState = xmppDataSource.connectionState.collectAsState().value,
                phoneNumber = preferencesManager.phoneNumber.collectAsState(initial = "").value ?: "",
                preferencesManager = preferencesManager,
                onChangeStoragePath = { }
            )
        }
        
        composable("player/{fileName}/{fileSize}") { backStackEntry ->
            val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
            val fileSize = backStackEntry.arguments?.getString("fileSize")?.toLongOrNull() ?: 0L
            val videoFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "toDown/$fileName"
            )
            
            if (videoFile.exists()) {
                VideoPlayerScreen(
                    videoFile = videoFile,
                    fileName = fileName,
                    fileSize = fileSize,
                    onBack = { navController.popBackStack() },
                    onShare = {
                        val shareIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_STREAM,
                                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", videoFile))
                            type = "video/*"
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir"))
                    }
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}
