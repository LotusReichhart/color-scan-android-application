package com.lotusreichhart.colorscan

import android.app.Application
import com.google.android.gms.ads.MobileAds
import timber.log.Timber

class ColorScanApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("ColorScanApplication onCreate....")

        MobileAds.initialize(this) {}
    }
}