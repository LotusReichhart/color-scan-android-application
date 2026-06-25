package com.lotusreichhart.colorscan.feature.history

import com.lotusreichhart.colorscan.core.model.HistoryEntity
import com.lotusreichhart.colorscan.core.model.HistorySort

data class HistoryUiState(
    val items: List<HistoryEntity> = emptyList(),
    val sortOption: HistorySort = HistorySort.DESC,
    val countText: String = "0 COLORS SAVED"
)
