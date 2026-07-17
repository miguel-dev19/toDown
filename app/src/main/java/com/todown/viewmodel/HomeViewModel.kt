package com.todown.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todown.data.local.DownloadEntity
import com.todown.data.repository.DownloadRepository
import com.todown.network.xmpp.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val downloadRepository: DownloadRepository,
    private val xmppDataSource: XmppClient
) : ViewModel() {
    
    val downloads: StateFlow<List<DownloadEntity>> = downloadRepository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val connectionState: StateFlow<ConnectionState> = xmppDataSource.connectionState
    val botState: StateFlow<BotState?> = xmppDataSource.botState
    val errorMessage: StateFlow<String?> = xmppDataSource.errorMessage
    val videoData: StateFlow<VideoData?> = xmppDataSource.videoMessages
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing
    
    fun sendUrlToBot(url: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            xmppDataSource.clearVideoData()
            xmppDataSource.sendUrlToBot(url)
            
            launch {
                xmppDataSource.videoMessages
                    .filterNotNull()
                    .first()
                    .let { video -> startDownload(video) }
            }
        }
    }
    
    fun startDownload(videoData: VideoData) {
        viewModelScope.launch {
            downloadRepository.startDownload(
                videoData = videoData,
                onProgress = { },
                onComplete = {
                    _isProcessing.value = false
                    xmppDataSource.clearVideoData()
                },
                onError = {
                    _isProcessing.value = false
                }
            )
        }
    }
    
    fun pauseDownload(downloadId: String) {
        viewModelScope.launch { downloadRepository.pauseDownload(downloadId) }
    }
    
    fun resumeDownload(downloadId: String) {
        viewModelScope.launch { downloadRepository.resumeDownload(downloadId) }
    }
    
    fun cancelDownload(downloadId: String) {
        viewModelScope.launch { downloadRepository.cancelDownload(downloadId) }
    }
    
    fun clearCompleted() {
        viewModelScope.launch { downloadRepository.clearCompleted() }
    }
    
    fun clearBotState() {
        _isProcessing.value = false
        xmppDataSource.clearVideoData()
    }
}
