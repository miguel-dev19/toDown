package com.todown.data.repository

import android.os.Environment
import com.todown.data.local.DownloadDao
import com.todown.data.local.DownloadEntity
import com.todown.network.xmpp.VideoData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DownloadRepository(
    private val downloadDao: DownloadDao
) {
    private val client = OkHttpClient()
    private val activeDownloads = mutableMapOf<String, Job>()
    
    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    
    suspend fun startDownload(
        videoData: VideoData,
        onProgress: (Float) -> Unit,
        onComplete: (File) -> Unit,
        onError: (Throwable) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val fileName = sanitizeFileName(videoData.fileName)
        val destinationDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES
        ).absolutePath + "/toDown/"
        File(destinationDir).mkdirs()
        
        val downloadId = java.util.UUID.randomUUID().toString()
        
        val entity = DownloadEntity(
            downloadId = downloadId,
            url = videoData.url,
            fileName = fileName,
            filePath = destinationDir + fileName,
            totalSize = videoData.size,
            status = "downloading",
            thumbnailUrl = videoData.thumbnailUrl,
            duration = videoData.duration.toString(),
            title = videoData.title
        )
        downloadDao.insert(entity)
        
        val job = launch {
            try {
                val request = Request.Builder()
                    .url(videoData.url)
                    .addHeader("User-Agent", "toDown/1.0")
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body ?: throw IOException("Empty body")
                val totalBytes = body.contentLength()
                
                val file = File(destinationDir + fileName)
                FileOutputStream(file).use { fos ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Long = 0
                        var read: Int
                        
                        while (input.read(buffer).also { read = it } != -1) {
                            if (!isActive) throw CancellationException("Download cancelled")
                            
                            fos.write(buffer, 0, read)
                            bytesRead += read
                            
                            if (totalBytes > 0) {
                                val progress = (bytesRead.toFloat() / totalBytes * 100)
                                withContext(Dispatchers.Main) { onProgress(progress) }
                                downloadDao.update(entity.copy(
                                    progress = progress.toInt(),
                                    downloadedSize = bytesRead
                                ))
                            }
                        }
                    }
                }
                
                downloadDao.update(entity.copy(progress = 100, status = "completed"))
                withContext(Dispatchers.Main) { onComplete(file) }
                
            } catch (e: CancellationException) {
                downloadDao.update(entity.copy(status = "canceled"))
            } catch (e: Exception) {
                downloadDao.update(entity.copy(status = "error"))
                withContext(Dispatchers.Main) { onError(e) }
            } finally {
                activeDownloads.remove(downloadId)
            }
        }
        
        activeDownloads[downloadId] = job
        downloadId
    }
    
    suspend fun pauseDownload(downloadId: String) {
        activeDownloads[downloadId]?.cancel()
        downloadDao.getByDownloadId(downloadId)?.let {
            downloadDao.update(it.copy(status = "paused"))
        }
    }
    
    suspend fun resumeDownload(downloadId: String) {
        // Para simplificar, cancelar y reintentar
        cancelDownload(downloadId)
    }
    
    suspend fun cancelDownload(downloadId: String) {
        activeDownloads[downloadId]?.cancel()
        downloadDao.getByDownloadId(downloadId)?.let {
            downloadDao.update(it.copy(status = "canceled"))
        }
        activeDownloads.remove(downloadId)
    }
    
    suspend fun clearCompleted() = withContext(Dispatchers.IO) {
        downloadDao.clearCompleted()
    }
    
    suspend fun getCompletedCount(): Int = withContext(Dispatchers.IO) {
        downloadDao.getCompletedCount()
    }
    
    suspend fun getTotalSize(): Long = withContext(Dispatchers.IO) {
        downloadDao.getTotalSize()
    }
    
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
