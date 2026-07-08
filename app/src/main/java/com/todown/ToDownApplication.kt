package com.todown

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.ketch.Ketch
import com.ketch.NotificationConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ToDownApplication : Application() {
    
    lateinit var ketch: Ketch
        private set
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        ketch = Ketch.builder()
            .setEnableAutoStartDownload(true)
            .setEnableParallelDownload(true)
            .setDefaultParallelCount(3)
            .setHttpClient(
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("User-Agent", "toDown/1.0")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .setNotificationConfig(
                NotificationConfig(
                    enabled = true,
                    channelId = CHANNEL_ID,
                    channelName = "Descargas toDown",
                    smallIcon = android.R.drawable.stat_sys_download,
                    color = getColor(R.color.primary)
                )
            )
            .build(this)
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
