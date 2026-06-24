package com.lotusreichhart.colorscan.feature.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lotusreichhart.colorscan.core.data.UserPreferencesManager
import com.lotusreichhart.colorscan.core.model.UserPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SplashViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = UserPreferencesManager.getInstance(application)

    private val _userPreferences = MutableStateFlow<UserPreferences?>(null)

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        startLoading()
    }

    fun loadPreferences(): UserPreferences {
        val prefs = preferencesManager.getUserPreferences()
        _userPreferences.value = prefs
        return prefs
    }

    private fun startLoading() {
        viewModelScope.launch {
            val prefsDeferred = async {
                loadPreferences()
            }

            val totalDuration = 2000L
            val steps = 35
            val delayPerStep = totalDuration / steps

            for (i in 1..steps) {
                delay(delayPerStep.milliseconds)
                val currentProgress = (i * 100) / steps
                _uiState.update { it.copy(progress = currentProgress) }
            }

            val userPrefs = prefsDeferred.await()

            val destination = if (userPrefs.isTheFirst) {
                SplashDestination.Onboarding
            } else {
                SplashDestination.Home
            }

            _uiState.update { it.copy(destination = destination) }
        }
    }
}