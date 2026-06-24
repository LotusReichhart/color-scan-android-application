package com.lotusreichhart.colorscan

import android.app.Application
import com.google.android.gms.ads.MobileAds

class ColorScanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
    }
}