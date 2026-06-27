package com.lotusreichhart.colorscan.feature.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.lotusreichhart.colorscan.core.data.HistoryRepository
import com.lotusreichhart.colorscan.core.data.UserPreferencesManager
import com.lotusreichhart.colorscan.core.model.HistoryEntity
import com.lotusreichhart.colorscan.core.model.HistorySort
import com.lotusreichhart.colorscan.core.model.PhotoHistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val historyRepository = HistoryRepository.getInstance(application)
    private val preferencesManager = UserPreferencesManager.getInstance(application)

    private val dbColorOnlyHistory = historyRepository.getColorOnlyHistory()
    private val dbPhotoColorHistory = historyRepository.getPhotoColorHistory()

    private val _sortOption = MutableStateFlow(HistorySort.DESC)
    val sortOption: StateFlow<HistorySort> = _sortOption.asStateFlow()

    val colorOnlyHistory = MediatorLiveData<List<HistoryEntity>>().apply {
        addSource(dbColorOnlyHistory) { list ->
            value = sortColorHistory(list, _sortOption.value)
        }
    }

    val photoColorHistory = MediatorLiveData<List<PhotoHistoryEntity>>().apply {
        addSource(dbPhotoColorHistory) { list ->
            value = sortPhotoHistory(list, _sortOption.value)
        }
    }

    init {
        val initialSort = preferencesManager.getUserPreferences().historySort
        _sortOption.value = initialSort
    }

    fun toggleSort() {
        val nextSort = if (_sortOption.value == HistorySort.DESC) {
            HistorySort.ASC
        } else {
            HistorySort.DESC
        }
        preferencesManager.setHistorySort(nextSort)
        _sortOption.value = nextSort

        // Force reload sorted values
        colorOnlyHistory.value = sortColorHistory(dbColorOnlyHistory.value, nextSort)
        photoColorHistory.value = sortPhotoHistory(dbPhotoColorHistory.value, nextSort)
    }

    fun deleteColor(entity: HistoryEntity) {
        viewModelScope.launch {
            historyRepository.deleteHistory(entity)
        }
    }

    fun deletePhotoColor(entity: PhotoHistoryEntity) {
        viewModelScope.launch {
            historyRepository.deletePhotoHistory(entity)
        }
    }

    private fun sortColorHistory(list: List<HistoryEntity>?, sort: HistorySort): List<HistoryEntity> {
        if (list == null) return emptyList()
        return if (sort == HistorySort.ASC) {
            list.sortedBy { it.timestamp }
        } else {
            list.sortedByDescending { it.timestamp }
        }
    }

    private fun sortPhotoHistory(list: List<PhotoHistoryEntity>?, sort: HistorySort): List<PhotoHistoryEntity> {
        if (list == null) return emptyList()
        return if (sort == HistorySort.ASC) {
            list.sortedBy { it.timestamp }
        } else {
            list.sortedByDescending { it.timestamp }
        }
    }
}