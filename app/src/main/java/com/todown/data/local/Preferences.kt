package com.todown.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        val JWT_KEY = stringPreferencesKey("jwt_token")
        val PHONE_KEY = stringPreferencesKey("phone_number")
        val STORAGE_PATH_KEY = stringPreferencesKey("storage_path")
        val SIMULTANEOUS_DOWNLOADS_KEY = intPreferencesKey("simultaneous_downloads")
        val THREADS_PER_DOWNLOAD_KEY = intPreferencesKey("threads_per_download")
        
        const val DEFAULT_STORAGE_PATH = "Movies/toDown"
        const val DEFAULT_SIMULTANEOUS = 3
        const val DEFAULT_THREADS = 4
    }
    
    val jwtToken: Flow<String?>
        get() = context.dataStore.data.map { it[JWT_KEY] }
    
    val phoneNumber: Flow<String?>
        get() = context.dataStore.data.map { it[PHONE_KEY] }
    
    val storagePath: Flow<String>
        get() = context.dataStore.data.map { it[STORAGE_PATH_KEY] ?: DEFAULT_STORAGE_PATH }
    
    val simultaneousDownloads: Flow<Int>
        get() = context.dataStore.data.map { it[SIMULTANEOUS_DOWNLOADS_KEY] ?: DEFAULT_SIMULTANEOUS }
    
    val threadsPerDownload: Flow<Int>
        get() = context.dataStore.data.map { it[THREADS_PER_DOWNLOAD_KEY] ?: DEFAULT_THREADS }
    
    suspend fun saveJwtToken(token: String) {
        context.dataStore.edit { it[JWT_KEY] = token }
    }
    
    suspend fun savePhoneNumber(phone: String) {
        context.dataStore.edit { it[PHONE_KEY] = phone }
    }
    
    suspend fun saveStoragePath(path: String) {
        context.dataStore.edit { it[STORAGE_PATH_KEY] = path }
    }
    
    suspend fun saveSimultaneousDownloads(count: Int) {
        context.dataStore.edit { it[SIMULTANEOUS_DOWNLOADS_KEY] = count }
    }
    
    suspend fun saveThreadsPerDownload(count: Int) {
        context.dataStore.edit { it[THREADS_PER_DOWNLOAD_KEY] = count }
    }
    
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
