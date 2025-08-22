package com.smartswitch.presentation.sendOrReceive.senderScanDevice

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdView
import com.smartswitch.R
import com.smartswitch.ads.banner_ads.setupBannerAd
import com.smartswitch.databinding.FragmentSentDataSolBinding

import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.PrefUtil

import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.isAlive


class SendDataSolFragment : Fragment() {
    private var _binding : FragmentSentDataSolBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSentDataSolBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isAlive { activityContext ->
            binding.doneBtn.setOnClickListener {
            findNavController().navigate(R.id.action_sendDataSendifyFragment_to_homeSendifyFragment)
            }


            if ( PrefUtil(requireContext()).getBool("is_premium", false)) {
                binding.adRel.gone()
            } else {

                        var initialLayoutComplete = false
                        binding.adViewContainer.apply {
                            addView(AdView(activityContext))
                            viewTreeObserver.addOnGlobalLayoutListener {
                                if (!initialLayoutComplete) {
                                    initialLayoutComplete = true
                                    binding.adViewContainer.setupBannerAd(
                                        activityContext,
                                        getString(R.string.banner_all)
                                    )
                                }


                    }
                }
            }
        }

    }
}