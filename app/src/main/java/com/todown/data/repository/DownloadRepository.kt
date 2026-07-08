package com.todown.data.repository

import android.os.Environment
import com.downloader.PRDownloader
import com.todown.data.local.DownloadDao
import com.todown.data.local.DownloadEntity
import com.todown.network.xmpp.VideoData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.io.File

class DownloadRepository(
    private val downloadDao: DownloadDao
) {
    
    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    
    suspend fun startDownload(
        videoData: VideoData,
        onProgress: (Float) -> Unit,
        onComplete: (File) -> Unit,
        onError: (Throwable) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val fileName = sanitizeFileName(videoData.fileName)
        val destinationDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES
        ).absolutePath + "/toDown/"
        File(destinationDir).mkdirs()
        
        val downloadId = PRDownloader.download(
            videoData.url,
            destinationDir,
            fileName
        ).build()
        .setOnProgressListener { progress ->
            val percent = (progress.currentBytes.toFloat() / progress.totalBytes.toFloat() * 100)
            launch(Dispatchers.Main) { onProgress(percent) }
            launch(Dispatchers.IO) {
                downloadDao.getByDownloadId(downloadId.toString())?.let {
                    downloadDao.update(it.copy(progress = percent.toInt(), downloadedSize = progress.currentBytes))
                }
            }
        }
        .start(object : com.downloader.OnDownloadListener {
            override fun onDownloadComplete() {
                val file = File(destinationDir + fileName)
                launch(Dispatchers.IO) {
                    downloadDao.getByDownloadId(downloadId.toString())?.let {
                        downloadDao.update(it.copy(progress = 100, status = "completed"))
                    }
                }
                launch(Dispatchers.Main) { onComplete(file) }
            }
            
            override fun onError(error: com.downloader.Error?) {
                launch(Dispatchers.IO) {
                    downloadDao.getByDownloadId(downloadId.toString())?.let {
                        downloadDao.update(it.copy(status = "error"))
                    }
                }
                launch(Dispatchers.Main) { onError(Throwable(error?.serverErrorMessage ?: "Error")) }
            }
        })
        
        val entity = DownloadEntity(
            downloadId = downloadId.toString(),
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
        
        downloadId
    }
    
    suspend fun pauseDownload(downloadId: String) = withContext(Dispatchers.IO) {
        PRDownloader.pause(downloadId.toInt())
        downloadDao.getByDownloadId(downloadId)?.let {
            downloadDao.update(it.copy(status = "paused"))
        }
    }
    
    suspend fun resumeDownload(downloadId: String) = withContext(Dispatchers.IO) {
        PRDownloader.resume(downloadId.toInt())
        downloadDao.getByDownloadId(downloadId)?.let {
            downloadDao.update(it.copy(status = "downloading"))
        }
    }
    
    suspend fun cancelDownload(downloadId: String) = withContext(Dispatchers.IO) {
        PRDownloader.cancel(downloadId.toInt())
        downloadDao.getByDownloadId(downloadId)?.let {
            downloadDao.update(it.copy(status = "canceled"))
        }
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
