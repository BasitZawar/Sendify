package com.smartswitch.presentation.language

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.zeugmasolutions.localehelper.LocaleHelper
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegate
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegateImpl
import java.util.Locale

open class BaseActivity : AppCompatActivity() {
    private val localeDelegate: LocaleHelperActivityDelegate = LocaleHelperActivityDelegateImpl()

    override fun getDelegate() = localeDelegate.getAppCompatDelegate(super.getDelegate())

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(localeDelegate.attachBaseContext(newBase))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("BaseActivity", "Error during onCreate: ")
        super.onCreate(savedInstanceState)
        localeDelegate.onCreate(this)
//        checkToTheme()
    }

    override fun onResume() {
        Log.e("BaseActivity", "Error during onResume: ")
        super.onResume()
//        checkToTheme()
        try {
            localeDelegate.onResumed(this)
        } catch (e: Exception) {
            Log.e("BaseActivity", "Error during onResume: ${e.message}", e)
        }
    }

    override fun onPause() {
        Log.d("BaseActivity", "onPause: ")
        super.onPause()
        localeDelegate.onPaused()
    }

    override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
        val context = super.createConfigurationContext(overrideConfiguration)
        return LocaleHelper.onAttach(context)
    }

    override fun getApplicationContext(): Context =
        localeDelegate.getApplicationContext(super.getApplicationContext())

    open fun updateLocale(locale: Locale) {
        Log.d("BaseActivity", "updateLocale: ")
        localeDelegate.setLocale(this, locale)
    }
}