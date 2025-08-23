package com.smartswitch.presentation.onBoarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.smartswitch.R
import com.smartswitch.databinding.FragmentOnBoardingBinding
import com.smartswitch.new_ads.nativeads.NativeAdsUtil

import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive


class OnBoardingFragment : Fragment() {
    private var _binding: FragmentOnBoardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOnBoardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isAlive { activityContext ->

            if ( PrefUtil(requireContext()).getBool("is_premium", false)) {
                binding.nativeBannerPlaceHolder.gone()
            } else {
                isAlive {
                    loadNative(getString(R.string.native_intro_screen))
                }

            }

            (activityContext as FragmentActivity).handleBackPressWithAction {
                // findNavController().popBackStack()
                activityContext.finishAffinity()
            }

            // Example data
            val data = arrayListOf<Intro>(
                Intro(
                    R.drawable.intro_1,
                    getString(R.string.heading1),
                    getString(R.string.intro_1_new)
                ),
                Intro(
                    R.drawable.intro_2,
                    getString(R.string.heading2),
                    getString(R.string.intro_2_new)
                ),
                Intro(
                    R.drawable.intro_3,
                    getString(R.string.share),
                    getString(R.string.share_multiple_types_of_files)
                ),

                )

            val adapter = ViewPagerAdapter(data)
            binding.viewpager.adapter = adapter
//            Handler(Looper.getMainLooper()).postDelayed({
//                //binding.btnNext.visibility = View.VISIBLE
//            }, 3000) // Wait for 8 seconds

            binding.btnNext.setOnClickListener {
                if (binding.viewpager.currentItem >= 2) {
                    navigateNextDestination()
                } else {
                    binding.viewpager.currentItem += 1
                }

            }

            binding.skip.setOnClickListener {
                navigateNextDestination()
            }

            /*   binding.btnBackward.setOnClickListener {
                   binding.viewpager.currentItem -= 1

               }*/
            binding.customIndicator.setupIndicators(adapter.itemCount)

            // Set the first indicator as active
            binding.customIndicator.selectIndicator(0)

            binding.viewpager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.customIndicator.selectIndicator(position)
                }
            })
            binding.viewpager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int,
                ) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                    if (position >= data.size - 1) {
                        //binding.btnNext.visibility = View.VISIBLE
                        binding.btnNext.text = getString(R.string.get_started)
                    } else {
                        //binding.btnNext.visibility = View.INVISIBLE
                        binding.btnNext.text = getString(R.string.next)
                    }
                }
            })

        }
    }

    private fun navigateNextDestination() {
        isAlive { activityContext ->
            findNavController().navigate(
                OnBoardingFragmentDirections.actionOnBoardingFragmentToPermissionSendifyFragment(
                    "intro"
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadNative(adId: String) {

        // if (PrefUtil(this@SplashActivity).getBool("is_premium", false)) return
        MobileAds.initialize(requireContext())
        NativeAdsUtil.loadNativeAd(
            requireContext(),
            1,
            adId,
            NativeAdOptions.ADCHOICES_TOP_LEFT
        ) { nativeAd ->
            isAlive {
                val adView = layoutInflater.inflate(
                    R.layout.ads_google_small_native, null
                ) as NativeAdView
                NativeAdsUtil.populateUnifiedNativeAdView(nativeAd, adView)
                binding.nativeAd.removeAllViews()
                binding.nativeAd.addView(adView)
            }
        }
    }
}