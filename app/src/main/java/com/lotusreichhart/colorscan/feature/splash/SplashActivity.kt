package com.lotusreichhart.colorscan.feature.splash

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lotusreichhart.colorscan.AdHelper
import com.lotusreichhart.colorscan.MainActivity
import com.lotusreichhart.colorscan.databinding.ActivitySplashBinding
import com.lotusreichhart.colorscan.feature.onboarding.OnboardingActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.getValue

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        makeFullscreen()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AdHelper.loadAppOpenAd(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    binding.progressBar.progress = uiState.progress
                    uiState.destination?.let { destination ->
                        navigateToDestination(destination)
                    }
                }
            }
        }
    }

    private fun navigateToDestination(destination: SplashDestination) {
        if (destination == SplashDestination.Onboarding) {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            lifecycleScope.launch {
                var delayCount = 0
                while (AdHelper.isAppOpenAdLoading() && AdHelper.getAppOpenAd() == null && delayCount < 3000) {
                    delay(100)
                    delayCount += 100
                }
                AdHelper.showAppOpenAd(this@SplashActivity) {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun makeFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }
}