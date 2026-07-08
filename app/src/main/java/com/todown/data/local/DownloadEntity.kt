package com.todown.data.local

import androidx.room.*

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val downloadId: String,
    val url: String,
    val fileName: String,
    val filePath: String,
    val totalSize: Long,
    val downloadedSize: Long = 0,
    val progress: Int = 0,
    val status: String,
    val thumbnailUrl: String?,
    val duration: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): kotlinx.coroutines.flow.Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE downloadId = :downloadId")
    suspend fun getByDownloadId(downloadId: String): DownloadEntity?
    
    @Insert
    suspend fun insert(download: DownloadEntity)
    
    @Update
    suspend fun update(download: DownloadEntity)
    
    @Delete
    suspend fun delete(download: DownloadEntity)
    
    @Query("DELETE FROM downloads WHERE status IN ('completed', 'error', 'canceled')")
    suspend fun clearCompleted()
    
    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'completed'")
    suspend fun getCompletedCount(): Int
    
    @Query("SELECT COALESCE(SUM(totalSize), 0) FROM downloads WHERE status = 'completed'")
    suspend fun getTotalSize(): Long
}
