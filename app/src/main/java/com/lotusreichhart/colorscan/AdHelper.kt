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

object AdHelper {

    private var mInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false

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

    /**
     * Tải trước (preload) Interstitial Ad để tránh bị trễ khi hiển thị.
     */
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

    /**
     * Hiển thị quảng cáo kẽ (Interstitial Ad).
     * @param activity Hoạt động hiện tại để làm context hiển thị quảng cáo.
     * @param onAdDismissed Sự kiện chạy khi quảng cáo đóng lại hoặc tải lỗi, để luồng code chính tiếp tục chạy.
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = mInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    // Tải trước cái mới cho lần sau
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
            // Nếu chưa tải xong quảng cáo kẽ, chạy tiếp luồng chính của app và thử load lại quảng cáo kẽ
            preloadInterstitialAd(activity)
            onAdDismissed()
        }
    }
}