package com.smartswitch.ads.native_ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.smartswitch.R
import com.smartswitch.new_ads.nativeads.NativeTemplateStyle
import com.smartswitch.new_ads.nativeads.TemplateView
import com.smartswitch.subscriptions.PrefUtil

object NativeAdsManager {

    var mNativeAd: NativeAd? = null

    fun loadAndShowNativeAd(
        context: Context,
        adId: String,
        adContainer: ViewGroup,
        adTemplateView: TemplateView,
        shimmerLayout: ShimmerFrameLayout,
        buttonsContainer: ViewGroup
    ) {
        val isPremium = PrefUtil(context).getBool("is_premium", false)
        buttonsContainer.visibility = View.INVISIBLE

        if (isPremium) {
            adContainer.visibility = View.GONE
            buttonsContainer.visibility = View.VISIBLE
            return
        }
        shimmerLayout.startShimmer()
        shimmerLayout.visibility = View.VISIBLE
        adTemplateView.visibility = View.GONE

        val adLoader = AdLoader.Builder(context, adId)
            .forNativeAd { nativeAd ->
                // Destroy old ad if already available
                mNativeAd?.destroy()
                mNativeAd = nativeAd
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                val styles = NativeTemplateStyle.Builder().build()
                adTemplateView.visibility = View.VISIBLE
                adTemplateView.setStyles(styles)
                adTemplateView.setNativeAd(nativeAd)
                adContainer.visibility = View.VISIBLE
                buttonsContainer.visibility = View.VISIBLE

            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("NativeAdUtil", "Failed to load: ${error.message}")
                    adContainer.visibility = View.GONE
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    if (buttonsContainer.visibility == View.INVISIBLE) {
                        buttonsContainer.visibility = View.VISIBLE
                    }
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun destroyAd() {
        mNativeAd?.destroy()
        mNativeAd = null
    }

    const val medium = "medium"

    /**    fun loadNativeAd(
    activity: Activity,
    nativeShimmerEffect: LottieAnimationView,
    frameLayout: LinearLayout,
    adType: String
    ) {
    Log.e("MYADS", "Loading native ad...")

    val adUnitId = activity.getString(R.string.native_ad_id)

    if (adUnitId.isEmpty()) {
    Log.e("MYADS", "Ad unit ID is empty.")
    return
    }

    val adView = when (adType) {
    medium -> {
    activity.layoutInflater.inflate(R.layout.native_medium, frameLayout, false) as NativeAdView
    }
    else -> {
    activity.layoutInflater.inflate(R.layout.native_medium, frameLayout, false) as NativeAdView
    }
    }

    if (mNativeAd == null) {
    Log.e("MYADS", "No cached native ad, requesting a new one...")

    val builder = AdLoader.Builder(activity, adUnitId)
    builder.forNativeAd { nativeAd: NativeAd? ->
    Log.e("MYADS", "Native ad loaded successfully.")
    if (mNativeAd != null) {
    mNativeAd?.destroy()
    }
    mNativeAd = nativeAd
    frameLayout.removeAllViews()
    frameLayout.addView(adView)
    mNativeAd?.let { populateNativeAdView(it, adView) }
    nativeShimmerEffect.visibility = View.GONE // Hide shimmer when ad is loaded
    }.withNativeAdOptions(
    NativeAdOptions.Builder()
    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
    .build()
    )

    val videoOptions = VideoOptions.Builder()
    .setStartMuted(true)
    .build()
    val adOptions = NativeAdOptions.Builder()
    .setVideoOptions(videoOptions)
    .build()

    builder.withNativeAdOptions(adOptions)

    val adLoader = builder.withAdListener(object : AdListener() {
    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
    Log.e("MYADS", "Ad failed to load: ${loadAdError.message}")
    mNativeAd = null
    nativeShimmerEffect.visibility = View.GONE // Hide shimmer effect
    }

    override fun onAdLoaded() {
    Log.e("MYADS", "Ad loaded successfully.")
    nativeShimmerEffect.visibility = View.GONE // Hide shimmer when ad is loaded
    }
    }).build()

    adLoader.loadAd(AdRequest.Builder().build())
    } else {
    Log.e("MYADS", "Using cached native ad.")
    frameLayout.removeAllViews()
    frameLayout.addView(adView)
    nativeShimmerEffect.visibility = View.GONE
    populateNativeAdView(mNativeAd!!, adView)
    }
    }*/


    private fun populateNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView
    ) {
        adView.mediaView = adView.findViewById<View>(R.id.ad_media) as MediaView
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        (adView.headlineView as TextView?)!!.text = nativeAd.headline
        adView.mediaView!!.mediaContent = nativeAd.mediaContent
        if (nativeAd.body == null) {
            adView.bodyView!!.visibility = View.INVISIBLE
        } else {
            adView.bodyView!!.visibility = View.VISIBLE
            (adView.bodyView as TextView?)!!.text = nativeAd.body
        }
        if (nativeAd.callToAction == null) {
            adView.callToActionView!!.visibility = View.INVISIBLE
        } else {
            adView.callToActionView!!.visibility = View.VISIBLE
            (adView.callToActionView as Button?)!!.text = nativeAd.callToAction
        }
        if (nativeAd.icon == null) {
            adView.iconView!!.visibility = View.INVISIBLE
        } else {
            (adView.iconView as ImageView?)!!.setImageDrawable(
                nativeAd.icon!!.drawable
            )
            adView.iconView!!.visibility = View.VISIBLE
        }
        if (nativeAd.starRating == null) {
            adView.starRatingView!!.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as RatingBar?)?.rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView!!.visibility = View.VISIBLE
        }
        if (nativeAd.advertiser == null) {
            adView.advertiserView!!.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as TextView?)!!.text = nativeAd.advertiser
            adView.advertiserView!!.visibility = View.VISIBLE
        }
        adView.setNativeAd(nativeAd)
        val vc = nativeAd.mediaContent!!.videoController
        if (vc.hasVideoContent()) {
            vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoEnd() {
                    super.onVideoEnd()
                }
            }
        }
    }

}