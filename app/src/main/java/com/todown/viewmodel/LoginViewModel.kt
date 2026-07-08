package com.todown.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todown.data.local.PreferencesManager
import com.todown.network.auth.JwtAuthenticator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val jwtAuthenticator: JwtAuthenticator,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess
    
    fun authenticate(phone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            jwtAuthenticator.authenticate(phone)
                .onSuccess { jwt ->
                    preferencesManager.saveJwtToken(jwt)
                    preferencesManager.savePhoneNumber(phone)
                    _isSuccess.value = true
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Error de autenticacion"
                }
            
            _isLoading.value = false
        }
    }
}
