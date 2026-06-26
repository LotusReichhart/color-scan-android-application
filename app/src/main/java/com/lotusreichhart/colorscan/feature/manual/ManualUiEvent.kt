package com.lotusreichhart.colorscan.feature.manual

import com.lotusreichhart.colorscan.core.model.ColorItem

sealed interface ManualUiEvent {
    data class SearchQueryChanged(val query: String, val immediate: Boolean = false) : ManualUiEvent
    data class SelectColor(val colorItem: ColorItem) : ManualUiEvent
    data class SaveColorToHistory(val colorItem: ColorItem) : ManualUiEvent
}