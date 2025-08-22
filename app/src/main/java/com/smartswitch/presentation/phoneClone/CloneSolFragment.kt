package com.smartswitch.presentation.phoneClone

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.smartswitch.R
import com.smartswitch.ads.inter_ads.InterstitialClass
import com.smartswitch.databinding.FragmentCloneSolBinding
import com.smartswitch.new_ads.nativeads.NativeTemplateStyle
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.setSafeOnClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CloneSolFragment : Fragment() {
    private var _binding: FragmentCloneSolBinding? = null
    private val binding get() = _binding!!
    //private var nativeAd : NativeAd? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCloneSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        isAlive { activityContext ->
            binding.oldPhoneButton.setSafeOnClickListener {
                if (PrefUtil(requireContext()).getBool(
                        "is_premium",
                        false
                    ) || !InterstitialClass.isInternetAvailable(
                        requireContext()
                    )
                ) {
                    findNavController().navigate(R.id.action_cloneSendifyFragment_to_selectDataToCloneSendifyFragment)
                } else {
                    InterstitialClass.request_interstitial(
                        requireContext(),
                        requireActivity(),
                        getString(R.string.inter_all)
                    ) {
                        isAlive {
                            Handler(Looper.getMainLooper()).postDelayed({
                                isAlive {
                                    findNavController().navigate(R.id.action_cloneSendifyFragment_to_selectDataToCloneSendifyFragment)
                                }
                            }, 200L)
                        }
                    }
                }
            }

            binding.newPhoneButton.setSafeOnClickListener {
                if (PrefUtil(requireContext()).getBool(
                        "is_premium",
                        false
                    ) || !InterstitialClass.isInternetAvailable(
                        requireContext()
                    )
                ) {
                    findNavController().navigate(R.id.action_cloneSendifyFragment_to_receiverScanDeviceSendifyFragment)
                } else {
                    InterstitialClass.request_interstitial(
                        requireContext(),
                        requireActivity(),
                        getString(R.string.inter_all)
                    ) {
                        isAlive {
                            Handler(Looper.getMainLooper()).postDelayed({
                                isAlive {
                                    findNavController().navigate(R.id.action_cloneSendifyFragment_to_receiverScanDeviceSendifyFragment)
                                }
                            }, 200L)
                        }
                    }
                }
            }

            (activityContext as FragmentActivity).handleBackPressWithAction {
                isAlive {
                    findNavController().popBackStack()
                }

            }

            binding.headerLayout.setNavigationOnClickListener {
                isAlive {
                    findNavController().popBackStack()
                }
            }
            isAlive { activityContext ->

                if (PrefUtil(requireContext()).getBool("is_premium", false)) {
                    binding.adRel.gone()
                } else {
                    val adLoader = AdLoader.Builder(requireContext(), getString(R.string.exit_screen_native))
                        .forNativeAd { nativeAd ->
                            // Launch on main thread to update UI safely
                            CoroutineScope(Dispatchers.Main).launch {
                                // Check if fragment is still alive before doing anything
                                if (!isAdded || _binding == null) {
                                    nativeAd.destroy() // cleanup to avoid leaks
                                    return@launch
                                }

                                val isPremium = PrefUtil(requireContext()).getBool("is_premium", false)
                                if (isPremium) {
                                    binding.adRel.visibility = View.GONE
                                    nativeAd.destroy() // no need to keep ad
                                } else {
                                    val styles = NativeTemplateStyle.Builder().build()
                                    binding.adTemplateView.visibility = View.VISIBLE
                                    binding.adTemplateView.setStyles(styles)
                                    binding.adTemplateView.setNativeAd(nativeAd)
                                }
                            }
                        }
                        .build()

                    adLoader.loadAd(AdRequest.Builder().build())
                }
            }
/*
            if (PrefUtil(requireContext()).getBool("is_premium", false)) {
                binding.adRel.gone()
            } else {
                val adLoader =
                    AdLoader.Builder(requireContext(), getString(R.string.exit_screen_native))
                        .forNativeAd { nativeAd ->
                            CoroutineScope(Dispatchers.IO).launch {

                                val isPremium =
                                    PrefUtil(requireContext()).getBool("is_premium", false)
                                withContext(Dispatchers.Main) {
                                    _binding?.let { binding ->

                                        if (isPremium) {
                                            binding.adRel.visibility = View.GONE
                                        } else {
                                            nativeAd.let {
                                                val styles =
                                                    NativeTemplateStyle.Builder().build()
                                                binding.adTemplateView.visibility = View.VISIBLE
                                                binding.adTemplateView.setStyles(styles)
                                                binding.adTemplateView.setNativeAd(it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .build()
                adLoader.loadAd(AdRequest.Builder().build())


            }*/
        }
    }
}