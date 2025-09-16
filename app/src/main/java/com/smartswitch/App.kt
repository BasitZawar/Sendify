package com.smartswitch

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.smartswitch.ads.inter_ads.InterstitialClass.isInterstitalIsShowing
import com.smartswitch.presentation.mainFragments.SettingsSolFragment
import com.smartswitch.subscriptions.PrefUtil
import com.zeugmasolutions.localehelper.LocaleAwareApplication
import com.zeugmasolutions.localehelper.LocaleHelper
import com.zeugmasolutions.localehelper.LocaleHelperApplicationDelegate
import dagger.hilt.android.HiltAndroidApp
import io.paperdb.Paper
import java.util.Date
import javax.inject.Inject


@HiltAndroidApp
class App : LocaleAwareApplication(), Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {
    private val localeAppDelegate = LocaleHelperApplicationDelegate()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(localeAppDelegate.attachBaseContext(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeAppDelegate.onConfigurationChanged(this)
    }

    override fun getApplicationContext(): Context =
        LocaleHelper.onAttach(super.getApplicationContext())

    @Inject
    lateinit var sharedPreferences: SharedPreferences
    var TAG = "APPLICATION"
    var currentFragmentTag: String? = null
    private var currentActivity: Activity? = null

    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super<LocaleAwareApplication>.onCreate()
        Paper.init(this@App)
        FirebaseApp.initializeApp(this)
        registerActivityLifecycleCallbacks(this)

        // Register for app process lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()

        initFirebaseNotification()
        if (sharedPreferences.getBoolean(SettingsSolFragment.Theme_STATUS_PREFERENCE_KEY, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CrashHandler", "Uncaught exception in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.e(TAG, "onStart: called ")
        super.onStart(owner)
        currentActivity?.let {
            appOpenAdManager.showAdIfAvailable(it)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "App moved to background → preload app open ad")
        appOpenAdManager.loadAd(applicationContext, "background_preload")
    }

    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        private var loadTime: Long = 0

        fun loadAd(context: Context, source: String) {
            Log.e(TAG, "loadAd: from ${source}} ")
            if (isLoadingAd || PrefUtil(applicationContext).getBool(
                    "is_premium",
                    false
                ) || isAdAvailable() || isInterstitalIsShowing
            ) {
                return
            }
            if (appOpenAd != null)
                return
            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                getString(R.string.app_open_on_resume),
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                        Log.d(TAG, "app open resume Loaded.")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd = false
                        Log.d(TAG, "app open resume  onAdFailedToLoad: " + loadAdError.message)
                    }
                },
            )
        }

        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        private fun isAdAvailable(): Boolean {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        }

        fun showAdIfAvailable(activity: Activity) {
            Log.e(TAG, "showAdIfAvailable 1:  called")
            val app = activity.application as App
            if (app.currentFragmentTag == "SplashSolFragment") {
                Log.d(TAG, "⏭ Skipping ad on SplashSolFragment")
                return
            }
            showAdIfAvailable(
                activity,
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        Log.e(TAG, "onShowAdComplete: ")
                    }
                },
            )
        }

        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener,
        ) {
            Log.e(TAG, "showAdIfAvailable 2:  called")
            if (isShowingAd || PrefUtil(applicationContext).getBool(
                    "is_premium",
                    false
                ) || isInterstitalIsShowing
            ) {
                Log.d(TAG, "The  ad is already showing.")
                return
            }

            if (!isAdAvailable()) {
                Log.d(TAG, "The app open ad is not ready yet.")
                onShowAdCompleteListener.onShowAdComplete()
                return
            }

            Log.d(TAG, "Will show ad.")
            appOpenAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null
                        isShowingAd = false
                        Log.d(TAG, "app open resume onAdDismissedFullScreenContent.")

                        onShowAdCompleteListener.onShowAdComplete()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        appOpenAd = null
                        isShowingAd = false
                        Log.d(TAG, " app open resume onAdFailedToShowFullScreenContent: " + adError.message)

                        onShowAdCompleteListener.onShowAdComplete()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "app open resume onAdShowedFullScreenContent.")
                    }
                }
            isShowingAd = true
            appOpenAd?.show(activity)
        }
    }

    private fun saveCrashLog(throwable: Throwable) {
        // Save stack trace to a file or SharedPreferences
        val crashInfo = Log.getStackTraceString(throwable)
        getSharedPreferences("crash_logs", MODE_PRIVATE)
            .edit()
            .putString("last_crash", crashInfo)
            .apply()
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun initFirebaseNotification() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "Current Token: $token")
            }
        }
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        if (p0 is AppCompatActivity) {
            p0.supportFragmentManager.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                        super.onFragmentResumed(fm, f)
                        currentFragmentTag = f::class.java.simpleName
                        Log.d(TAG, "Current Fragment = $currentFragmentTag")
                    }
                },
                true
            )
        }
    }

    override fun onActivityStarted(p0: Activity) {
        currentActivity = p0
    }

    override fun onActivityResumed(p0: Activity) {
    }

    override fun onActivityPaused(p0: Activity) {
    }

    override fun onActivityStopped(p0: Activity) {
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
    }

    override fun onActivityDestroyed(p0: Activity) {
    }

    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }
}
