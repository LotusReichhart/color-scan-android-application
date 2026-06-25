package com.lotusreichhart.colorscan.core.model

enum class HistorySort{
    ASC,
    DESC
}

data class UserPreferences(
    val isTheFirst: Boolean = true,
    val historySort: HistorySort = HistorySort.DESC
)
