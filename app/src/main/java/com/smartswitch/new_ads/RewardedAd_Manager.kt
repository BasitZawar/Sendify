package com.smartswitch.new_ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.TextView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.smartswitch.BuildConfig
import com.smartswitch.R
import com.smartswitch.ads.inter_ads.InterstitialClass
import com.smartswitch.interfaces.ActionAfterClosed
import com.smartswitch.subscriptions.PrefUtil
import kotlin.math.log

@SuppressLint("StaticFieldLeak")
object RewardedAd_Manager {
    var mRewardedAd: RewardedAd? = null
    var mContext: Context? = null
    var mActivity: Activity? = null
    var rewardedID: String? = null
    var TAG: String = "ADS_MANAGER"
    var mActionOnAdClosedListener: ActionAfterClosed? = null
    var isAdDecided: Boolean = false
    var isRewardedFailed: Boolean = false
    var isRewardedShowing: Boolean = false
    var isProcessing: Boolean = false
    var stopRewarded: Boolean = false
    var timerCalled: Boolean = false
    var dialog: Dialog? = null

    fun requestRewardedAd(
        context: Context?,
        activity: Activity?,
        actionOnAdClosedListenersm: ActionAfterClosed?,
    ) {
        Log.e("ADS_MANAGER", "requestRewardedAd: ")
//        if (!PrefUtil(context).getBool("playstore")) return
        if (PrefUtil(context!!).getBool(
                "is_premium", false
            ) || !InterstitialClass.isInternetAvailable(
                context
            )
        ) {
            Log.e(TAG, "requestRewardedAd: 00")
            mActionOnAdClosedListener = actionOnAdClosedListenersm
            performAction()
            return
        }
        if (isProcessing) {
            Log.e(TAG, "request_interstitial: Dialog is showing..")
            return
        }
        isProcessing = true
        mContext = context
        mActivity = activity
        if (BuildConfig.DEBUG) {
            rewardedID = "ca-app-pub-3940256099942544/5224354917"
        } else {
            rewardedID = "ca-app-pub-2493449427846338/2478627863"
        }
        mActionOnAdClosedListener = actionOnAdClosedListenersm
        isAdDecided = false

        if (AdTimer.isEligibleForAd()) {
            if (PrefUtil(context).getBool(
                    "is_premium", false
                ) || !InterstitialClass.isInternetAvailable(
                    context
                )
            ) {
                mActionOnAdClosedListener = actionOnAdClosedListenersm
                performAction()
                Log.e(TAG, "requestRewardedAd 1: ")
            } else {
                Log.e(TAG, "requestRewardedAd: 2")
                loadRewarded()
            }
        } else {
            Log.e(TAG, "requestRewardedAd: 3")
            performAction()
        }
    }

    private fun loadRewarded(
    ) {
        Log.e(TAG, "loadRewarded: 1")
        if (mRewardedAd == null) {
            Log.e(TAG, "Premium Interstitial Request Send.")
            showAdDialog()
            stopRewarded = false
            timerCalled = false
            val rewardedAdRequest = AdRequest.Builder().build()
            RewardedAd.load(
                mContext!!, rewardedID!!, rewardedAdRequest, object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        isRewardedFailed = false
                        mRewardedAd = ad
                        isAdDecided = true
                        Log.e(TAG, "Rewarded ad Loaded.")

                        if (!timerCalled) {
                            closeAdDialog()
                            showRewarded()
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(TAG, "Rewarded ad Failed to Load." + loadAdError.message)

                        isRewardedFailed = true
                        mRewardedAd = null
                        isAdDecided = true
                        if (!timerCalled) {
                            closeAdDialog()
                            performAction()
                        }
                    }
                })
            timerAdDecided()
        } else {
            isRewardedFailed = true
            Log.e(TAG, " Premium Ad was already loaded.: ")
            stopRewarded = false
            showAdDialog()
            Handler().postDelayed({
                closeAdDialog()
                showRewarded()
            }, 2000)
        }
    }

    fun showRewarded(
    ) {
        mRewardedAd?.let { ad ->
            ad.show(mActivity!!, OnUserEarnedRewardListener { rewardItem ->
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                Log.d(
                    TAG,
                    "User earned the reward rewardAmount:$rewardAmount and rewardType:$rewardType"
                )
                mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdClicked() {
                        Log.d(TAG, "Rewarded Ad was clicked.")
                    }

                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Rewarded Ad dismissed fullscreen content.")
                        mRewardedAd = null
                        performAction()
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        Log.e(TAG, "Rewarded Ad failed to show fullscreen content.")
                        mRewardedAd = null
                        performAction()
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "Rewarded Ad recorded an impression.")
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Rewarded Ad showed fullscreen content.")
                    }
                }
            })
        } ?: run {
            Log.d(TAG, "Rewarded The rewarded ad wasn't ready yet.")
            performAction()
        }
    }

    fun timerAdDecided(
    ) {
        Handler().postDelayed({
            if (!isAdDecided) {
                stopRewarded = true
                timerCalled = true
                Log.e(TAG, "Handler Cancel.")
                AdTimer.cancelTimer()
                closeAdDialog()
                showRewarded()
            }
        }, 5000)
    }

    fun showAdDialog() {
        if (mActivity != null && !mActivity!!.isFinishing) {
            isRewardedShowing = true
            dialog = Dialog(mActivity!!)
            dialog?.setContentView(R.layout.rewarded_dialog)
            dialog?.setCancelable(false)
            dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            val tvTitle = dialog?.findViewById<TextView>(R.id.tvTitle)
            val tvMessage = dialog?.findViewById<TextView>(R.id.tvMessage)
            tvTitle?.text = "Please Wait."
            tvMessage?.text = "Full Screen Ad is expected to Show."
            try {
                dialog?.show()
            } catch (e: Exception) {
                Log.e("AdDialog", "Error showing custom dialog: ${e.message}")
            }
            dialog = dialog
        }
    }

    fun closeAdDialog() {
        isRewardedShowing = false
        try {
            if (mActivity != null && !mActivity!!.isFinishing) {
                if (Build.VERSION.SDK_INT >= 24) {
                    Log.e(TAG, "rewarded closeAdDialog: ")
                    if (dialog != null && dialog!!.isShowing) {
                        dialog!!.dismiss()
                    }
//                    if (progressDialog != null && progressDialog!!.isShowing) {
//                        progressDialog!!.dismiss()
//                    }
                } else {
                    if (dialog != null && dialog!!.isShowing) {
                        dialog!!.dismiss()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "closeAdDialog: Exception")
        }
    }

    fun performAction() {
        Log.e(TAG, "performAction: Moving next")
        mActionOnAdClosedListener?.ActionAfterAd()
        Handler().postDelayed({ isProcessing = false }, 1000)
    }
}

