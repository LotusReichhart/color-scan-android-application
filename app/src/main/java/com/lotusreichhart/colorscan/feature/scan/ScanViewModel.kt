package com.lotusreichhart.colorscan.feature.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lotusreichhart.colorscan.core.data.ColorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    
    private val colorRepository = ColorRepository.getInstance(application)

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onEvent(event: ScanUiEvent) {
        when (event) {
            is ScanUiEvent.ToggleFreeze -> {
                _uiState.update { it.copy(isFrozen = !it.isFrozen) }
            }
            is ScanUiEvent.ToggleFlash -> {
                _uiState.update { it.copy(isFlashOn = !it.isFlashOn) }
            }
            is ScanUiEvent.ZoomIn -> {
                _uiState.update { 
                    val newZoom = min(4.0f, ((it.zoomRatio + 0.1f) * 10f).roundToInt() / 10f)
                    it.copy(zoomRatio = newZoom)
                }
            }
            is ScanUiEvent.ZoomOut -> {
                _uiState.update { 
                    val newZoom = max(1.0f, ((it.zoomRatio - 0.1f) * 10f).roundToInt() / 10f)
                    it.copy(zoomRatio = newZoom)
                }
            }
        }
    }

    fun updateScannedColor(hex: String, rgb: String) {
        if (_uiState.value.isFrozen) return
        
        val closestColor = colorRepository.findClosestColor(hex)
        val colorName = closestColor?.name ?: "Unknown Color"
        
        _uiState.update { 
            it.copy(
                colorName = colorName,
                colorHex = hex,
                colorRgb = rgb
            )
        }
    }
}