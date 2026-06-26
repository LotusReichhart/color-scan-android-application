package com.lotusreichhart.colorscan.core.data

import android.annotation.SuppressLint
import android.content.Context
import com.lotusreichhart.colorscan.core.model.HistoryEntity
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class HistoryRepository private constructor(context: Context) {

    private val historyDao: HistoryDao = AppDatabase.getInstance(context).historyDao()

    fun getAllHistory(): Flow<List<HistoryEntity>> {
        return historyDao.getAllHistory()
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

    suspend fun deleteHistory(history: HistoryEntity) {
        try {
            historyDao.delete(history)
            Timber.d("Successfully deleted color history item: id=${history.id}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete color history item")
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
