package com.lotusreichhart.colorscan.feature.splash

sealed interface SplashDestination {
    object Onboarding : SplashDestination
    object Home : SplashDestination
}

data class SplashUiState(
    val progress: Int = 0,
    val destination: SplashDestination? = null
)
