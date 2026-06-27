package com.lotusreichhart.colorscan

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.lotusreichhart.colorscan.databinding.ActivityMainBinding
import com.lotusreichhart.colorscan.feature.history.HistoryFragment
import com.lotusreichhart.colorscan.feature.manual.ManualFragment
import com.lotusreichhart.colorscan.feature.scan.ScanFragment
import com.lotusreichhart.colorscan.feature.settings.SettingsFragment
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appUpdateManager: AppUpdateManager
    private val UPDATE_REQUEST_CODE = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        makeFullscreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForAppUpdate()

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

    private fun checkForAppUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    val updateOptions = com.google.android.play.core.appupdate.AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        this,
                        updateOptions,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start in-app update flow")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    val updateOptions = com.google.android.play.core.appupdate.AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        this,
                        updateOptions,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to resume in-app update flow")
                }
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Timber.w("Update flow failed or was canceled by user! ResultCode: $resultCode")
                // Toast or handle update cancel if needed
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