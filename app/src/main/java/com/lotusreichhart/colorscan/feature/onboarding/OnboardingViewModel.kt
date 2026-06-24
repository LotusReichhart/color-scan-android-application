package com.lotusreichhart.colorscan.feature.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lotusreichhart.colorscan.core.data.UserPreferencesManager

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = UserPreferencesManager.getInstance(application)

    fun completeOnboarding() {
        preferencesManager.setFirstTime(false)
    }
}