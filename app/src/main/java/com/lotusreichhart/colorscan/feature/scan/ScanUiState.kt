package com.lotusreichhart.colorscan.feature.scan

data class ScanUiState(
    val isFrozen: Boolean = false,
    val isFlashOn: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val colorName: String = "Sunset Orange",
    val colorHex: String = "#FF4500",
    val colorRgb: String = "255, 69, 0"
)
