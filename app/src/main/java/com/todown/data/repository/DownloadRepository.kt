package com.todown.data.repository

import android.os.Environment
import com.todown.data.local.DownloadDao
import com.todown.data.local.DownloadEntity
import com.todown.network.xmpp.VideoData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class DownloadRepository(
    private val ketch: com.ketch.Ketch,
    private val downloadDao: DownloadDao
) {
    
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
        
        val downloadId = ketch.download(
            url = videoData.url,
            fileName = fileName,
            path = destinationDir,
            tag = "video_download",
            headers = mapOf("User-Agent" to "toDown/1.0")
        )
        
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
        
        CoroutineScope(Dispatchers.IO).launch {
            ketch.observeDownloadById(downloadId)
                .flowOn(Dispatchers.IO)
                .collect { downloadModel ->
                    when {
                        downloadModel.isCompleted -> {
                            updateDatabase(downloadId, 100, "completed")
                            withContext(Dispatchers.Main) { onComplete(File(downloadModel.filePath)) }
                        }
                        downloadModel.isFailed -> {
                            updateDatabase(downloadId, 0, "error")
                            withContext(Dispatchers.Main) { onError(Throwable(downloadModel.errorMessage)) }
                        }
                        else -> {
                            updateDatabase(downloadId, downloadModel.progress, "downloading")
                            withContext(Dispatchers.Main) { onProgress(downloadModel.progress / 100f) }
                        }
                    }
                }
        }
        downloadId
    }
    
    private suspend fun updateDatabase(downloadId: String, progress: Int, status: String) {
        downloadDao.getByDownloadId(downloadId)?.let {
            downloadDao.update(it.copy(progress = progress, status = status))
        }
    }
    
    suspend fun pauseDownload(downloadId: String) = withContext(Dispatchers.IO) { ketch.pause(downloadId) }
    suspend fun resumeDownload(downloadId: String) = withContext(Dispatchers.IO) { ketch.resume(downloadId) }
    suspend fun cancelDownload(downloadId: String) = withContext(Dispatchers.IO) { ketch.cancel(downloadId) }
    suspend fun clearCompleted() = withContext(Dispatchers.IO) { downloadDao.clearCompleted() }
    suspend fun getCompletedCount(): Int = withContext(Dispatchers.IO) { downloadDao.getCompletedCount() }
    suspend fun getTotalSize(): Long = withContext(Dispatchers.IO) { downloadDao.getTotalSize() }
    
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
