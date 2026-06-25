package com.lotusreichhart.colorscan

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.appopen.AppOpenAd

object AdHelper {

    private var mInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var mAppOpenAd: AppOpenAd? = null
    private var isAppOpenAdLoading = false
    private var mLastAdShowTime = 0L
    private const val MIN_SHOW_INTERVAL = 30000L // 30 seconds interval

    fun loadBannerAd(container: android.view.ViewGroup) {
        val context = container.context
        val adView = AdView(context)
        adView.adUnitId = BuildConfig.BANNER_AD_ID
        adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER)
        container.removeAllViews()
        container.addView(adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    fun preloadInterstitialAd(context: Context) {
        if (mInterstitialAd != null || isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context.applicationContext,
            BuildConfig.INTERSTITIAL_AD_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                    isAdLoading = false
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        val ad = mInterstitialAd
        if (ad != null && (currentTime - mLastAdShowTime >= MIN_SHOW_INTERVAL)) {
            mLastAdShowTime = currentTime
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    preloadInterstitialAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            if (ad == null) {
                preloadInterstitialAd(activity)
            }
            onAdDismissed()
        }
    }

    fun loadAppOpenAd(context: Context) {
        if (mAppOpenAd != null || isAppOpenAdLoading) return

        isAppOpenAdLoading = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context.applicationContext,
            BuildConfig.APP_OPEN_AD_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    mAppOpenAd = ad
                    isAppOpenAdLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mAppOpenAd = null
                    isAppOpenAdLoading = false
                }
            }
        )
    }

    fun showAppOpenAd(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = mAppOpenAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mAppOpenAd = null
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mAppOpenAd = null
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            onAdDismissed()
        }
    }

    fun isAppOpenAdLoading(): Boolean = isAppOpenAdLoading

    fun getAppOpenAd(): AppOpenAd? = mAppOpenAd
}