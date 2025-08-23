package com.smartswitch.presentation.language

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.smartswitch.R
import com.smartswitch.ads.PrefUtils
import com.smartswitch.ads.inter_ads.InterstitialClass
import com.smartswitch.databinding.FragmentLanguageBinding
import com.smartswitch.new_ads.nativeads.NativeAdsUtil
import com.smartswitch.new_ads.nativeads.NativeTemplateStyle
import com.smartswitch.presentation.SplashSolFragment

import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.extensions.getLanguageData
import com.smartswitch.utils.extensions.getMyLocales
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LanguageFragment : Fragment() {
    private var _binding: FragmentLanguageBinding? = null
    private val binding get() = _binding!!
    private val args: LanguageFragmentArgs by navArgs()

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private var selectedPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun toggleBackButton(show: Boolean) {
        if (show) {
            binding.headerLayout.setNavigationIcon(R.drawable.ic_back_press)
            binding.headerLayout.navigationIcon?.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
        } else {
            binding.headerLayout.navigationIcon = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the saved language position from SharedPreferences
        selectedPosition = sharedPreferences.getInt("lang", 0)
        binding.headerLayout.setSubtitle(requireContext().getLanguageData()[selectedPosition].txt)


        if (args.from == "splash") {
            toggleBackButton(false)
        } else {
            toggleBackButton(true)
        }

        isAlive { activityContext ->
            activityContext.actionBar?.setDisplayHomeAsUpEnabled(false)

            // Done button logic
//            Handler(Looper.getMainLooper()).postDelayed({
//                binding.btnDone.visibility = View.VISIBLE
//            }, 2000L)

            viewLifecycleOwner.lifecycleScope.launch {
                delay(2000L)
                if (_binding != null) {
                    binding.btnDone.visibility = View.VISIBLE
                }
            }

            binding.btnDone.setOnClickListener {
                Log.d("awaiskhan", "Done btn is clicked")
                if ( PrefUtil(requireContext()).getBool("is_premium", false) || !InterstitialClass.isInternetAvailable(
                        requireContext()
                    )
                ) {
                    updateLanguage()
                    navigate()
                } else {
                    Log.d("awaiskhan", "not premium")

                    Log.d("awaiskhan", "get value from sharedpref")
                    InterstitialClass.request_interstitial(
                        requireContext(),
                        requireActivity(),
                        getString(R.string.language_screen_inter)
                    ) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.d("awaiskhan", "after ads")
                            isAlive {
                                updateLanguage()
                                navigate()
                            }
                        }, 200L)
                    }


                }
            }

            if ( PrefUtil(requireContext()).getBool("is_premium", false)) {
                binding.adRel.gone()
            } else {
               isAlive {

                   displayNativeAd()
               }

            }


            // Set up the toolbar's back navigation
            binding.headerLayout.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            (activityContext as FragmentActivity).handleBackPressWithAction {
                //findNavController().popBackStack()
                if (args.from == "splash") {
                    activityContext.finishAffinity()
                    //App.openingNewApp = false

                } else {
                    findNavController().popBackStack()
                }
            }


            // Set up RecyclerView with the language options
            val adapter =
                LanguageAdapter(requireContext().getLanguageData()) { language, position ->
                    selectedPosition = position
                    //val bindin = requireContext().getLanguageData()[position].txt
                    binding.headerLayout.setSubtitle(language)
                    binding.headerLayout.setTitle(getTextForTitle(position))
                    binding.btnDone.text = getTextForDone(position)
                }
            adapter.setPos(selectedPosition)
            binding.rvLanguage.layoutManager = LinearLayoutManager(requireContext())
            binding.rvLanguage.adapter = adapter
        }
    }

    private fun navigate() {
        isAlive {
            Log.d("awaiskhan", "call navigate")

            if (args.from == "splash") {
                if (!PrefUtils.getBoolean(requireContext(), "is_first_time_launch1")) {
//                                        Log.d("SplashFragment___", "Navigating to PermissionFragment")
//                                        findNavController().navigate(
//                                            SplashSolFragmentDirections.actionSplashSendifyFragmentToLanguageFragment(
//                                                "splash"
//                                            )
//                                        )

                    findNavController().navigate(R.id.action_languageFragment_to_onBoardingFragment)
                } else {
//                                        findNavController().navigate(
//                                            SplashSolFragmentDirections.actionSplashSendifyFragmentToLanguageFragment(
//                                                "splash"
//                                            )
//                                        )
                    //findNavController().navigate(R.id.action_languageFragment_to_homeSendifyFragment)


                    if (PrefUtil(requireContext()).getBool("is_premium", false)) {
                        findNavController().navigate(LanguageFragmentDirections.actionLanguageFragmentToHomeSendifyFragment())
                    } else {
                        findNavController().navigate(
                            LanguageFragmentDirections.actionLanguageFragmentToPremiumFragment(
                                "language"
                            )
                        )
                    }
                }
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun updateLanguage() {
        Log.d("awaiskhan", "call update language")
        // Save selected language position
        sharedPreferences.edit().putInt("lang", selectedPosition).apply()

        val baseActivity = activity as? BaseActivity
        baseActivity?.updateLocale(getMyLocales(selectedPosition))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.e("onDestroyView", "onDestroyView: ")
        _binding = null
    }


    private fun displayNativeAd() {
        CoroutineScope(Dispatchers.IO).launch {
            val nativeAd = SplashSolFragment.mNativeAd
            val isPremium = PrefUtil(requireContext()).getBool(
                "is_premium",
                false
            )
            withContext(Dispatchers.Main) {
                if (isPremium) {
                    binding.adRel.visibility = View.GONE
                } else {
                    if (nativeAd != null) {
                        val styles = NativeTemplateStyle.Builder().build()
                        binding.adTemplateView.visibility = View.VISIBLE
                        binding.adTemplateView.setStyles(styles)
                        binding.adTemplateView.setNativeAd(nativeAd)
                    } else {
                        binding.adRel.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun getTextForDone(pos: Int): String {
        val array = arrayOf("Done", "مکمل", "تم", "पूर्ण", "Fertig", "Hecho", "Fatto")
        return array[pos]
    }

    private fun getTextForTitle(pos: Int): String {
        val array = arrayOf("Language", "زبان", "اللغة", "भाषा", "Sprache", "Idioma", "Lingua")
        return array[pos]
    }
}
