package com.lotusreichhart.colorscan.feature.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lotusreichhart.colorscan.core.data.HistoryRepository
import com.lotusreichhart.colorscan.core.data.UserPreferencesManager
import com.lotusreichhart.colorscan.core.model.HistoryEntity
import com.lotusreichhart.colorscan.core.model.HistorySort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val historyRepository = HistoryRepository.getInstance(application)
    private val preferencesManager = UserPreferencesManager.getInstance(application)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var rawHistoryList: List<HistoryEntity> = emptyList()

    init {
        val initialSort = preferencesManager.getUserPreferences().historySort
        _uiState.update { it.copy(sortOption = initialSort) }

        viewModelScope.launch {
            historyRepository.getAllHistory().collect { historyList ->
                rawHistoryList = historyList
                updateSortedList()
            }
        }
    }

    fun onEvent(event: HistoryUiEvent) {
        when (event) {
            is HistoryUiEvent.ToggleSort -> toggleSort()
            is HistoryUiEvent.DeleteColor -> deleteColor(event.entity)
        }
    }

    private fun toggleSort() {
        val nextSort = if (_uiState.value.sortOption == HistorySort.DESC) {
            HistorySort.ASC
        } else {
            HistorySort.DESC
        }
        preferencesManager.setHistorySort(nextSort)
        _uiState.update { it.copy(sortOption = nextSort) }
        updateSortedList()
    }

    private fun deleteColor(entity: HistoryEntity) {
        viewModelScope.launch {
            historyRepository.deleteHistory(entity)
        }
    }

    private fun updateSortedList() {
        val currentSort = _uiState.value.sortOption
        val sortedList = if (currentSort == HistorySort.ASC) {
            rawHistoryList.sortedBy { it.timestamp }
        } else {
            rawHistoryList.sortedByDescending { it.timestamp }
        }

        val count = sortedList.size
        val countText = if (count == 1) "1 COLOR SAVED" else "$count COLORS SAVED"

        _uiState.update {
            it.copy(
                items = sortedList,
                countText = countText
            )
        }
    }
}