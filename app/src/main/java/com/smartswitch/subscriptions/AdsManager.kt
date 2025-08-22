package com.smartswitch.subscriptions

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.widget.RelativeLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

@Suppress("DEPRECATION")
class AdsManager(val context: Context)
{
    private var adView: AdView? = null
    private val TAG = javaClass.simpleName

    private fun getAdSize(ad_view_container: RelativeLayout, context: Context): AdSize {
        val display = (context as Activity).windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        var adWidthPixels = ad_view_container.width.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

//    fun loadBannerAd(adLayout: RelativeLayout?, context: Context): AdView? {
//        if (PrefUtil(requireContext()).getBool("is_premium", false)) {
//            return null
//        }
//        if (adLayout == null) return null
//        adLayout.removeAllViews()
//        adLayout.visibility = View.VISIBLE
//        val adRequest = AdRequest.Builder().build()
//        adView = AdView(context)
//
//        adView!!.adUnitId = context.getString(R.string.banner_id_main)
//
//        adView?.setAdSize(getAdSize(adLayout, context))
//        adLayout.addView(adView)
//        adView!!.loadAd(adRequest)
//        adView!!.adListener = object : AdListener() {
//            override fun onAdLoaded() {
//                super.onAdLoaded()
//                adLayout.visibility = View.VISIBLE
//                Log.e(TAG, "Banner Ad Loaded")
//            }
//
//            override fun onAdFailedToLoad(p0: LoadAdError) {
//                super.onAdFailedToLoad(p0)
//                Log.e(TAG, "Bannner Loading Failed - " + p0.message)
//            }
//        }
//
//        return adView
//    }

    fun loadInterstitial(interAdId: String?, from: String) {
        if (PrefUtil(context).getBool("is_premium", false)) {
            return
        }

        if (mInterstitialAd != null) {
            return
        }

//        if (mInterstitialAd != null && from != "splash") {
//            return
//        }
        interAdId?.let {

            val adsid: String = interAdId
            var a = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                adsid,
                a,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        mInterstitialAd = ad

                        Log.e(TAG, "onAdLoaded - INTER_MED : " + ad.responseInfo.mediationAdapterClassName )

                        if (from == "Dashboard") {
                            Constants.isDashboardInterLoaded = true
                        }

                        if (from.contains("BackPress")) {
                            Constants.isBackPressInterLoaded = true
                        }

                        Log.e(TAG, "Interstitial ad loaded from : $from")
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        mInterstitialAd = null

                        if (from == "Dashboard") {
                            Constants.isDashboardInterLoaded = false
                        }

                        if (from.contains("BackPress")) {
                            Constants.isBackPressInterLoaded = false
                        }

                        Log.e(TAG, "Interstitial ad load failed from : $from")
                    }
                })
        }
    }

    fun showInterstitial(
        activity: Activity,
        interType: String?,
        listener: InterstitialAdListener
    ) {

        if (PrefUtil(context).getBool("is_premium", false)) {
            listener.onAdClosed()
            return
        }

        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.e(TAG, "Inter Ad Dismissed from : $interType")
                        mInterstitialAd = null
                        CommonKeys.isInterstitialAdShowed = true
                        listener.onAdClosed()
                        if (interType == "Dashboard") {
                            Constants.isDashboardInterLoaded = false
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "Inter Ad failed to show from : $interType")
                        mInterstitialAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.e(TAG, "Inter Ad Showed fullscreen content from : $interType")
                        mInterstitialAd = null
                    }
                }
            mInterstitialAd!!.show(activity)
            Log.e(TAG, "Inter Ad Showed")
        } else {
            Log.e(TAG, "Inter Ad Failed")
            listener.onAdClosed()
        }
    }

    fun loadBackPressInterstitial(interAdId: String?, from: String) {
        if (PrefUtil(context).getBool("is_premium", false)) {
            return
        }

        if (mBackPressInterstitialAd != null) {
            return
        }

        interAdId?.let {

            val adsid: String = interAdId
            var a = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                adsid,
                a,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        mBackPressInterstitialAd = ad

                        Log.e(TAG, "onAdLoaded - BACK_INTER_MED : " + ad.responseInfo.mediationAdapterClassName )

                        if (from.contains("BackPress")) {
                            Constants.isBackPressInterLoaded = true
                        }

                        Log.e(TAG, "Interstitial ad loaded from : $from")
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        mBackPressInterstitialAd = null

                        if (from.contains("BackPress")) {
                            Constants.isBackPressInterLoaded = false
                        }

                        Log.e(TAG, "Interstitial ad load failed from : $from")
                    }
                })
        }
    }

    fun showBackPressInterstitial(
        activity: Activity,
        interType: String?,
        listener: InterstitialAdListener
    ) {

        if (PrefUtil(context).getBool("is_premium", false)) {
            listener.onAdClosed()
            return
        }

        if (mBackPressInterstitialAd != null) {
            mBackPressInterstitialAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.e(TAG, "Inter Ad Dismissed from : $interType")
                        mBackPressInterstitialAd = null
                        listener.onAdClosed()

                        if (interType!!.contains("BackPress")) {
                            Log.e(TAG, "onAdDismissedFullScreenContent: BackPress" )
                            Constants.isBackPressInterLoaded = false
                        }

                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "Inter Ad failed to show from : $interType")
                        mBackPressInterstitialAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.e(TAG, "Inter Ad Showed fullscreen content from : $interType")
                        mBackPressInterstitialAd = null
                    }
                }
            mBackPressInterstitialAd!!.show(activity)
            Log.e(TAG, "Inter Ad Showed")
        } else {
            Log.e(TAG, "Inter Ad Failed")
            listener.onAdClosed()
        }
    }

    interface InterstitialAdListener {
        fun onAdClosed()
    }

    companion object {
        var reload = true
//        var interstatialshow: Boolean = false
        private var mInterstitialAd: InterstitialAd? = null
        private var mBackPressInterstitialAd: InterstitialAd? = null
        private var adsManagerInstance: AdsManager? = null
        private var mContext: Context? = null
        val instance: AdsManager
            get() {
                if (adsManagerInstance == null) {
                    adsManagerInstance = AdsManager(mContext!!)
                }
                return adsManagerInstance!!
            }
    }

    init {
        if (mContext == null) {
            mContext = context

            MobileAds.initialize(context) { initializationStatus ->
                val statusMap = initializationStatus.adapterStatusMap
                for (adapterClass in statusMap.keys) {
                    val status = statusMap[adapterClass]
                }

            }
        }
    }

}
