package com.smartswitch.presentation

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.annotation.NonNull
import com.smartswitch.App
import com.smartswitch.R
import com.smartswitch.ads.inter_ads.InterstitialClass
import com.smartswitch.databinding.ActivityMainBinding
import com.smartswitch.presentation.language.BaseActivity
import com.smartswitch.setupPortraitWithWindowInsets
import com.smartswitch.subscriptions.GooglePlayBuySubscription
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupPortraitWithWindowInsets(R.id.main)
        GooglePlayBuySubscription.initBillingClient(this@MainActivity)
        GooglePlayBuySubscription.makeGooglePlayConnectionRequest()
        Log.d("MainActivity","onCreate")

        Log.v("LOG_TESTING", "This log is v");
        Log.d("LOG_TESTING", "This log is d");
        Log.w("LOG_TESTING", "This log is w");
        Log.i("LOG_TESTING", "This log is i");
        Log.e("LOG_TESTING", "This log is v");

       // throw RuntimeException("This is a forced crash")
    }

    override fun onStart() {
        super.onStart()
//        SplashSolFragment.openingNewApp = true
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity","onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity","onPause")
    }


    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing || !isChangingConfigurations) {
            Log.d("MainActivity", "onDestroy")
            Log.d("MainActivityfgdgdfgsdfgsdfgdsfgfdgfddfdfg", "onDestroy")
            SplashSolFragment.openingNewApp = false
        }
    }
}