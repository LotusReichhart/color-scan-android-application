package com.lotusreichhart.colorscan.core.data

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import com.lotusreichhart.colorscan.core.model.HistoryEntity
import com.lotusreichhart.colorscan.core.model.PhotoHistoryEntity
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class HistoryRepository private constructor(context: Context) {

    private val historyDao: HistoryDao = AppDatabase.getInstance(context).historyDao()

    fun getAllHistory(): Flow<List<HistoryEntity>> {
        return historyDao.getAllHistory()
    }

    fun getColorOnlyHistory(): LiveData<List<HistoryEntity>> {
        return historyDao.getColorOnlyHistory()
    }

    fun getPhotoColorHistory(): LiveData<List<PhotoHistoryEntity>> {
        return historyDao.getPhotoColorHistory()
    }

    suspend fun saveColor(hex: String, name: String, rgb: String) {
        try {
            val entity = HistoryEntity(
                hex = hex,
                name = name,
                rgb = rgb,
                timestamp = System.currentTimeMillis()
            )
            historyDao.insert(entity)
            Timber.d("Successfully saved color to history: hex=$hex, name=$name")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save color to history")
        }
    }

    suspend fun saveColorWithPhoto(hex: String, name: String, rgb: String, imagePath: String) {
        try {
            // 1. Save color in color_history table
            saveColor(hex, name, rgb)

            // 2. Save in photo_color_history table
            val photoEntity = PhotoHistoryEntity(
                hex = hex,
                name = name,
                rgb = rgb,
                imagePath = imagePath,
                timestamp = System.currentTimeMillis()
            )
            historyDao.insertPhotoHistory(photoEntity)
            Timber.d("Successfully saved color with photo: hex=$hex, name=$name, path=$imagePath")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save color with photo to history")
        }
    }

    suspend fun deleteHistory(history: HistoryEntity) {
        try {
            historyDao.delete(history)
            Timber.d("Successfully deleted color history item: id=${history.id}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete color history item")
        }
    }

    suspend fun deletePhotoHistory(photoHistory: PhotoHistoryEntity) {
        try {
            historyDao.deletePhotoHistory(photoHistory)
            // Delete physical file as well
            val file = java.io.File(photoHistory.imagePath)
            if (file.exists()) {
                file.delete()
            }
            Timber.d("Successfully deleted photo history item: id=${photoHistory.id}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete photo history item")
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: HistoryRepository? = null

        fun getInstance(context: Context): HistoryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HistoryRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
