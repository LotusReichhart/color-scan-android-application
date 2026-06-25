package com.lotusreichhart.colorscan.feature.history

import com.lotusreichhart.colorscan.core.model.HistoryEntity

sealed interface HistoryUiEvent {
    object ToggleSort : HistoryUiEvent
    data class DeleteColor(val entity: HistoryEntity) : HistoryUiEvent
}