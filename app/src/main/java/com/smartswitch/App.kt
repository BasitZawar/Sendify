package com.smartswitch

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.smartswitch.presentation.mainFragments.SettingsSolFragment
import com.zeugmasolutions.localehelper.LocaleAwareApplication
import com.zeugmasolutions.localehelper.LocaleHelper
import com.zeugmasolutions.localehelper.LocaleHelperApplicationDelegate
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlin.system.exitProcess


@HiltAndroidApp
class App : LocaleAwareApplication() {
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
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        initFirebaseNotification()
        if (sharedPreferences.getBoolean(SettingsSolFragment.Theme_STATUS_PREFERENCE_KEY, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log the crash
            Log.e("CrashHandler", "Uncaught exception in thread ${thread.name}", throwable)

            // Save crash details for later upload
//            saveCrashLog(throwable)

            // Optional: Restart the app after crash
//            restartApp()

            // Call the original handler (to let Crashlytics or system handle it)
            defaultHandler?.uncaughtException(thread, throwable)
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
}
