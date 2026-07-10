package com.todown

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ToDownApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Descargas toDown",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Notificaciones de descargas" }
            
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    
    companion object {
        const val CHANNEL_ID = "download_channel"
    }
}
