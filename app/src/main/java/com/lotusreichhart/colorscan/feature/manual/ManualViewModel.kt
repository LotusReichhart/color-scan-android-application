package com.lotusreichhart.colorscan.feature.manual

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lotusreichhart.colorscan.core.data.ColorRepository
import com.lotusreichhart.colorscan.core.data.HistoryRepository
import com.lotusreichhart.colorscan.core.model.ColorItem
import com.lotusreichhart.colorscan.core.util.ColorConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class ManualViewModel(application: Application) : AndroidViewModel(application) {

    private val colorRepository = ColorRepository.getInstance(application)
    private val historyRepository = HistoryRepository.getInstance(application)

    private val _uiState = MutableStateFlow(ManualUiState())
    val uiState: StateFlow<ManualUiState> = _uiState.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    fun onEvent(event: ManualUiEvent) {
        when (event) {
            is ManualUiEvent.SearchQueryChanged -> onSearchQueryChanged(event.query, event.immediate)
            is ManualUiEvent.SelectColor -> selectColor(event.colorItem)
            is ManualUiEvent.SaveColorToHistory -> saveColorToHistory(event.colorItem)
        }
    }

    private fun onSearchQueryChanged(query: String, immediate: Boolean) {
        searchJob?.cancel()
        _uiState.update { it.copy(query = query) }

        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            _uiState.update {
                it.copy(
                    activeColor = null,
                    searchResults = emptyList(),
                    status = SearchStatus.EMPTY,
                    complementColor = null,
                    triadic1 = null,
                    triadic2 = null
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            if (!immediate) {
                delay(300)
            }
            val result = withContext(Dispatchers.Default) {
                computeSearch(trimmedQuery)
            }
            _uiState.update {
                it.copy(
                    activeColor = result.activeColor,
                    searchResults = result.searchResults,
                    status = result.status,
                    complementColor = result.complementColor,
                    triadic1 = result.triadic1,
                    triadic2 = result.triadic2
                )
            }
        }
    }

    private data class SearchResultData(
        val activeColor: ColorItem?,
        val searchResults: List<ColorItem>,
        val status: SearchStatus,
        val complementColor: ColorItem?,
        val triadic1: ColorItem?,
        val triadic2: ColorItem?
    )

    private fun computeSearch(trimmedQuery: String): SearchResultData {
        val parsedColorInt = ColorConverter.parseHex(trimmedQuery)
            ?: ColorConverter.parseRgb(trimmedQuery)

        return if (parsedColorInt != null) {
            val closestColors = colorRepository.findClosestColors(parsedColorInt, limit = 11)
            if (closestColors.isNotEmpty()) {
                val active = closestColors.first()
                val similar = closestColors.drop(1)
                calculateHarmony(active, similar)
            } else {
                SearchResultData(null, emptyList(), SearchStatus.NO_RESULTS, null, null, null)
            }
        } else {
            val matches = colorRepository.searchByName(trimmedQuery)
            if (matches.isNotEmpty()) {
                val active = matches.first()
                val similar = matches.drop(1)
                calculateHarmony(active, similar)
            } else {
                SearchResultData(null, emptyList(), SearchStatus.NO_RESULTS, null, null, null)
            }
        }
    }

    private fun calculateHarmony(active: ColorItem, similar: List<ColorItem>): SearchResultData {
        val activeColorInt = ColorConverter.parseHex(active.hex) ?: android.graphics.Color.BLACK

        val complementColorInt = ColorConverter.getComplementaryColor(activeColorInt)
        val complementHex = ColorConverter.intToHex(complementColorInt)
        val closestComplement = colorRepository.findClosestColor(complementHex)
        val complementColorItem = ColorItem(
            hex = complementHex,
            name = closestComplement?.name ?: "Custom Color",
            rgb = ColorConverter.intToRgbString(complementColorInt)
        )

        val (t1Int, t2Int) = ColorConverter.getTriadicColors(activeColorInt)
        val t1Hex = ColorConverter.intToHex(t1Int)
        val t2Hex = ColorConverter.intToHex(t2Int)
        
        val t1Closest = colorRepository.findClosestColor(t1Hex)
        val t2Closest = colorRepository.findClosestColor(t2Hex)

        val t1Item = ColorItem(
            hex = t1Hex,
            name = t1Closest?.name ?: "Custom Color",
            rgb = ColorConverter.intToRgbString(t1Int)
        )

        val t2Item = ColorItem(
            hex = t2Hex,
            name = t2Closest?.name ?: "Custom Color",
            rgb = ColorConverter.intToRgbString(t2Int)
        )

        return SearchResultData(
            activeColor = active,
            searchResults = similar,
            status = SearchStatus.SUCCESS,
            complementColor = complementColorItem,
            triadic1 = t1Item,
            triadic2 = t2Item
        )
    }

    private fun selectColor(colorItem: ColorItem) {
        viewModelScope.launch {
            val colorInt = ColorConverter.parseHex(colorItem.hex)
            val similar = if (colorInt != null) {
                withContext(Dispatchers.Default) {
                    colorRepository.findClosestColors(colorInt, limit = 11)
                        .filter { it.hex != colorItem.hex }
                        .take(10)
                }
            } else {
                emptyList()
            }
            val result = withContext(Dispatchers.Default) {
                calculateHarmony(colorItem, similar)
            }
            _uiState.update {
                it.copy(
                    activeColor = result.activeColor,
                    searchResults = result.searchResults,
                    status = result.status,
                    complementColor = result.complementColor,
                    triadic1 = result.triadic1,
                    triadic2 = result.triadic2
                )
            }
        }
    }

    private fun saveColorToHistory(colorItem: ColorItem) {
        viewModelScope.launch {
            try {
                historyRepository.saveColor(
                    hex = colorItem.hex,
                    name = colorItem.name,
                    rgb = colorItem.rgb
                )
                Timber.d("Manual saved color to history: ${colorItem.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save manual color to history")
            }
        }
    }
}