package com.lotusreichhart.colorscan.feature.scan

sealed interface ScanUiEvent {
    data object ToggleFreeze : ScanUiEvent
    data object ToggleFlash : ScanUiEvent
    data object ZoomIn : ScanUiEvent
    data object ZoomOut : ScanUiEvent
}