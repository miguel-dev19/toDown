package com.todown.network.download

import android.content.Context
import com.ketch.Ketch
import com.ketch.NotificationConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object KetchManager {
    
    fun create(context: Context): Ketch {
        return Ketch.builder()
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
                    channelId = "download_channel",
                    channelName = "Descargas toDown",
                    smallIcon = android.R.drawable.stat_sys_download,
                    color = android.graphics.Color.parseColor("#1E88E5")
                )
            )
            .build(context)
    }
}
