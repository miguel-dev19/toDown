package com.todown.data.local

import android.content.Context
import androidx.room.*

@Database(entities = [DownloadEntity::class], version = 1, exportSchema = false)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    
    companion object {
        @Volatile
        private var INSTANCE: DownloadDatabase? = null
        
        fun getInstance(context: Context): DownloadDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    DownloadDatabase::class.java,
                    "downloads_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
