package com.smartswitch.presentation

import android.animation.ObjectAnimator

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.nativead.NativeAd
import com.smartswitch.R
import com.smartswitch.ads.GoogleMobileAdsConsentManager
import com.smartswitch.databinding.FragmentSplashSolBinding
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.extensions.handleDoubleBackPressToExit
import com.smartswitch.utils.extensions.isAlive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private const val LOG_TAG = "MyApplication"

class SplashSolFragment : Fragment() {
    private var _binding: FragmentSplashSolBinding? = null
    private val binding get() = _binding!!
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private var isAdLoaded = false // Flag to check if the open ad is loaded
    private var isAlReadyShow = false
    var openAd: AppOpenAd? = null
    private var fullScreenContentCallback: FullScreenContentCallback? = null

    companion object {
        var openingNewApp = true
        var mNativeAd: NativeAd? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSplashSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        isAlive { activityContext ->
            animateProgressBar(binding.progressBar, 15000)

            (activityContext as FragmentActivity).handleDoubleBackPressToExit {
                activityContext.finishAffinity()
            }

            showNextProcedureData()


        }
    }

    private fun showNextProcedureData() {
        CoroutineScope(Dispatchers.Main).launch {
            val isPremium = withContext(Dispatchers.IO) {
                PrefUtil(requireContext()).getBool(
                    "is_premium",
                    false
                )
            }
            if (isPremium) {
                animateProgressBar(binding.progressBar, 3000)
                delay(3000)
                intentbutton()
                Log.d("TEGSPLAH", "is_premium")
            } else {
                animateProgressBar(binding.progressBar, 16000)
                requestConsent()
                Log.d("TEGSPLAH", "requestConsent delay")

            }
        }
    }

    private fun animateProgressBar(progressBar: ProgressBar, duration: Long) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", 0, 102).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
        }
        animator.start()
    }


    private fun intentbutton() {
        if (isAdded && !isRemoving && !requireActivity().isFinishing) {
            Log.e(LOG_TAG, "Navigate to onBoarding")

            isAlive {
                if (!PermissionManager.hasLocationPermission(requireContext()) || !PermissionManager.hasNearbyPermission(
                        requireContext()
                    ) || !PermissionManager.hasStorageAccessPermission(requireContext())
                ) {
                    Log.d("SplashFragment___", "Navigating to PermissionFragment")
                    findNavController().navigate(
                        SplashSolFragmentDirections.actionSplashSendifyFragmentToLanguageFragment(
                            "splash"
                        )
                    )
                } else {
                    findNavController().navigate(
                        SplashSolFragmentDirections.actionSplashSendifyFragmentToLanguageFragment(
                            "splash"
                        )
                    )

                }
            }
        }
    }


    private fun requestConsent() {
        Log.d("TEGSPLAH", "requestConsent")
        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(requireContext())
        googleMobileAdsConsentManager.gatherConsent(requireActivity()) { consentError ->
            if (consentError != null) {
                Log.e("TESTSPLASH", "consentError:   ")
                initializeMobileAdsSdkOther()
            }


            if (googleMobileAdsConsentManager.canRequestAds) {

                initializeMobileAdsSdkOther()


            }
            if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
                // Regenerate the options menu to include a privacy setting.
                invalidateOptionsMenu(requireActivity())
            }
        }

        if (googleMobileAdsConsentManager.canRequestAds) {

            initializeMobileAdsSdkOther()

        }


    }


    private fun loadNativeLangAd(adId: String) {
        if (PrefUtil(requireContext()).getBool(
                "is_premium",
                false
            )
        ) return

        val adLoader = AdLoader.Builder(requireContext(), adId)
            .forNativeAd { nativeAd ->
                // Cache the native ad
                mNativeAd = nativeAd
                Log.d("displayNativeAd", "loadNativeAd: ${nativeAd}")
                Log.d("TEGSPLAH", "loadNativeAd: ${nativeAd}")
            }
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

    }

    private fun initializeMobileAdsSdkOther() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        if (PrefUtil(requireContext()).getBool(
                "is_premium",
                false
            )
        ) {
            intentbutton()
            Log.d("TEGSPLAH", "is_premium2")
        } else {

            loadNativeLangAd(getString(R.string.language_screen_native))
            loadSplashOpenAd()
            CoroutineScope(Dispatchers.Main).launch {
                Log.d("TEGSPLAH", "after loadSplashOpenAd")
                delay(16000)
                Log.d("TEGSPLAH", "after 16 sec")
                if (!isAdded) return@launch
                if (isAdLoaded) {
                    Log.e("TESTSPLASH", "after 16 sec")
                    Log.d("TEGSPLAH", "after 16 sec")
                    showSplashOpenAd()
                } else {
                    // If the ad is not loaded after 15 seconds, proceed with intentbutton
                    intentbutton()
                    Log.d("TEGSPLAH", "ad is not loaded:   ")
                }
            }
        }
    }


    fun loadSplashOpenAd() {
        if (PrefUtil(requireContext()).getBool(
                "is_premium",
                false
            )
        )
            return

        Log.d("TEGSPLAH", "onAdFailedLoaded: adid  = ${R.string.app_open_splash}")
        val loadCallback: AppOpenAd.AppOpenAdLoadCallback =
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.e("TESTSPLASH", "onAdLoaded: $ad")
                    Log.d("TEGSPLAH", "onAdLoaded: $ad")
                    openAd = ad
                    isAdLoaded = true // Set the flag to indicate that the ad is loaded
                    showSplashOpenAd()

                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    Log.e("TESTSPLASH", "onAdFailedLoaded: $p0")
                    Log.d("TEGSPLAH", "onAdFailedLoaded: $p0")
                    // Handle ad loading failure
                    if (isAdded) intentbutton()
                }
            }
        val request: AdRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            requireContext(),
            getString(R.string.app_open_splash),
            request,
            loadCallback
        )


    }

    fun showSplashOpenAd() {
        if (!isAdded) return

        if (PrefUtil(requireContext()).getBool("is_premium", false)) {
            intentbutton()
            return
        }
        if (isAlReadyShow) return

        Log.e("TESTSPLASH", "app open called")
        fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (!isAdded) return
                isAlReadyShow = true
                CoroutineScope(Dispatchers.Main).launch {
                    delay(200)
                    if (isAdded) intentbutton()
                }
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                if (isAdded) intentbutton()
            }
        }

        openAd?.fullScreenContentCallback = fullScreenContentCallback
        if (isAdded && openAd != null) {
            openAd!!.show(requireActivity())
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }


}