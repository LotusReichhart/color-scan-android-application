package com.lotusreichhart.colorscan.feature.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.lotusreichhart.colorscan.AdHelper
import com.lotusreichhart.colorscan.MainActivity
import com.lotusreichhart.colorscan.R
import com.lotusreichhart.colorscan.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.completeOnboarding()
            finishOnboarding()
        } else {
            Toast.makeText(this, "Camera permission is required to proceed.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        makeFullscreen()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AdHelper.loadBannerAd(binding.bannerContainer)

        setupViewPager()
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

    private fun setupViewPager() {
        val fragments = listOf(
            OnboardingFragment.newInstance(
                0,
                R.drawable.img_onboarding_1,
                R.string.onboarding_title_1,
                R.string.onboarding_desc_1,
                R.string.onboarding_next
            ),
            OnboardingFragment.newInstance(
                1,
                R.drawable.img_onboarding_2,
                R.string.onboarding_title_2,
                R.string.onboarding_desc_2,
                R.string.onboarding_next
            ),
            OnboardingFragment.newInstance(
                2,
                R.drawable.img_onboarding_3,
                R.string.onboarding_title_3,
                R.string.onboarding_desc_3,
                R.string.onboarding_get_started
            )
        )

        fragments.forEach { fragment ->
            fragment.onNextClickListener = {
                val current = binding.viewPager.currentItem
                if (current < fragments.size - 1) {
                    binding.viewPager.currentItem = current + 1
                } else {
                    if (ContextCompat.checkSelfPermission(this@OnboardingActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.completeOnboarding()
                        finishOnboarding()
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        }

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }
    }

    private fun finishOnboarding() {
        val intent = Intent(this@OnboardingActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}