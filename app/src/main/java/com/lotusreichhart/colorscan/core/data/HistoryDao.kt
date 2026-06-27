package com.lotusreichhart.colorscan.core.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lotusreichhart.colorscan.core.model.HistoryEntity
import com.lotusreichhart.colorscan.core.model.PhotoHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    @Query("SELECT * FROM color_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM color_history ORDER BY timestamp DESC")
    fun getColorOnlyHistory(): LiveData<List<HistoryEntity>>

    @Delete
    suspend fun delete(history: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoHistory(photoHistory: PhotoHistoryEntity)

    @Query("SELECT * FROM photo_color_history ORDER BY timestamp DESC")
    fun getPhotoColorHistory(): LiveData<List<PhotoHistoryEntity>>

    @Delete
    suspend fun deletePhotoHistory(photoHistory: PhotoHistoryEntity)
}
