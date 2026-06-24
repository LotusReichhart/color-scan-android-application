package com.lotusreichhart.colorscan

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.lotusreichhart.colorscan.databinding.ActivityMainBinding
import com.lotusreichhart.colorscan.feature.history.HistoryFragment
import com.lotusreichhart.colorscan.feature.manual.ManualFragment
import com.lotusreichhart.colorscan.feature.scan.ScanFragment
import com.lotusreichhart.colorscan.feature.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        makeFullscreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showFragment(ScanFragment.newInstance())
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_scan -> ScanFragment.newInstance()
                R.id.nav_manual -> ManualFragment.newInstance()
                R.id.nav_history -> HistoryFragment.newInstance()
                R.id.nav_settings -> SettingsFragment.newInstance()
                else -> ScanFragment.newInstance()
            }
            showFragment(fragment)
            true
        }

        val menuView = binding.bottomNavigation.getChildAt(0) as? ViewGroup
        if (menuView != null) {
            for (i in 0 until menuView.childCount) {
                menuView.getChildAt(i).setOnLongClickListener { true }
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
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