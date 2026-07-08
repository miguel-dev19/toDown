package com.todown.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todown.data.local.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    val storagePath: StateFlow<String> = preferencesManager.storagePath
        .stateIn(viewModelScope, SharingStarted.Lazily, "Movies/toDown")
    
    val simultaneousDownloads: StateFlow<Int> = preferencesManager.simultaneousDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, 3)
    
    val threadsPerDownload: StateFlow<Int> = preferencesManager.threadsPerDownload
        .stateIn(viewModelScope, SharingStarted.Lazily, 4)
    
    val phoneNumber: StateFlow<String?> = preferencesManager.phoneNumber
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    
    fun updateSimultaneousDownloads(count: Int) {
        viewModelScope.launch {
            preferencesManager.saveSimultaneousDownloads(count)
        }
    }
    
    fun updateThreadsPerDownload(count: Int) {
        viewModelScope.launch {
            preferencesManager.saveThreadsPerDownload(count)
        }
    }
    
    fun updateStoragePath(path: String) {
        viewModelScope.launch {
            preferencesManager.saveStoragePath(path)
        }
    }
}
