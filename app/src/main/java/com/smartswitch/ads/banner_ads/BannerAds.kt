package com.smartswitch.ads.banner_ads

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowMetrics
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

fun ViewGroup.setupBannerAd(activity: Activity, adUnitId: String) {
    val adView = AdView(activity)
    this.addView(adView)

    var initialLayoutComplete = false

    fun calculateAdSize(): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        var adWidthPixels = this.width.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    this.viewTreeObserver.addOnGlobalLayoutListener {
        if (!initialLayoutComplete) {
            initialLayoutComplete = true
            val adSizeValue = calculateAdSize()
            adView.adUnitId = adUnitId
            adView.setAdSize(adSizeValue)

            // Create an ad request.
            val adRequest = AdRequest.Builder().build()

            // Start loading the ad in the background.
            adView.loadAd(adRequest)

            adView.adListener = object: AdListener() {
                override fun onAdClicked() {
                    // Code to be executed when the user clicks on an ad.
                    Log.d("AdMob", "Ad clicked")
                }

                override fun onAdClosed() {
                    // Code to be executed when the user is about to return
                    // to the app after tapping on an ad.
                    Log.d("AdMob", "Ad closed")
                }

                override fun onAdFailedToLoad(adError : LoadAdError) {
                    // Code to be executed when an ad request fails.
                    Log.d("AdMob", "Ad failed to load: ${adError.message}")
                }

                override fun onAdImpression() {
                    // Code to be executed when an impression is recorded
                    // for an ad.
                    Log.d("AdMob", "Ad impression")
                }

                override fun onAdLoaded() {
                    // Code to be executed when an ad finishes loading.
                    Log.d("AdMob", "Ad loaded (Unit Id): ${adView.adUnitId}")
                    Log.d("AdMob", "Ad loaded (adRequest): $adRequest")
                }

                override fun onAdOpened() {
                    // Code to be executed when an ad opens an overlay that
                    // covers the screen.
                    Log.d("AdMob", "Ad opened")
                }
            }
        }
    }
}
var adView1:AdView?=null
fun Activity.loadBanner(adViewContainer: FrameLayout) {

        // Start shimmer animation while loading ad

        // [START create_ad_view]
        // Create a new ad view.
        val adView = AdView(this)
        adView.adUnitId = "ca-app-pub-2493449427846338/2587022898"
        adView.setAdSize(getAdSize())
        adView1 = adView

        // Replace ad container with new ad view.
        adViewContainer.removeAllViews()
        adViewContainer.addView(adView)
        // [END create_ad_view]

        // [START load_ad]
        // Start loading the ad in the background.
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                // Stop shimmer and show the ad when it's loaded

            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                // Handle the error and stop shimmer if necessary
//             shimmerFrameLayout.stopShimmer()
            }
        }
    }
    // [END load_ad]


fun Activity.getAdSize(): AdSize {

    val displayMetrics = resources.displayMetrics
    val adWidthPixels =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = this.windowManager.currentWindowMetrics
            windowMetrics.bounds.width()
        } else {
            displayMetrics.widthPixels
        }
    val density = displayMetrics.density
    val adWidth = (adWidthPixels / density).toInt()
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
}