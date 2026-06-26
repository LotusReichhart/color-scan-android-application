package com.lotusreichhart.colorscan.feature.manual

import com.lotusreichhart.colorscan.core.model.ColorItem

enum class SearchStatus {
    EMPTY,
    SUCCESS,
    NO_RESULTS
}

data class ManualUiState(
    val query: String = "",
    val activeColor: ColorItem? = null,
    val searchResults: List<ColorItem> = emptyList(),
    val status: SearchStatus = SearchStatus.EMPTY,
    val complementColor: ColorItem? = null,
    val triadic1: ColorItem? = null,
    val triadic2: ColorItem? = null
)
