package com.smartswitch.presentation.sendOrReceive.receiverScanDevice

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdView
import com.smartswitch.R
import com.smartswitch.ads.banner_ads.setupBannerAd
import com.smartswitch.ads.inter_ads.InterstitialClass
import com.smartswitch.databinding.FragmentSendingOrReceivingBinding

import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.AlertDialogManager
import com.smartswitch.utils.AlertDialogManager.showDialogSafely
import com.smartswitch.utils.MyDialogBox
import com.smartswitch.utils.TransferStateManager
import com.smartswitch.utils.WifiDirectManager
import com.smartswitch.utils.enums.TransferState

import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.setSafeOnClickListener
import com.smartswitch.utils.service.SendOrReceiveService
import kotlinx.coroutines.launch


class ReceivingDataSolFragment : Fragment() {
    private var _binding : FragmentSendingOrReceivingBinding? = null
    private val binding get() = _binding!!

    var wifiDirectManager: WifiDirectManager? = null
    var sendOrReceiveService: SendOrReceiveService? = null
    var mBound = false
    val TAG = "SenderConnectedDevice"

    val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            //we have bound to service so get reference to it
            val binder = service as SendOrReceiveService.LocalBinder
            sendOrReceiveService = binder.getService()
            mBound = true

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
            sendOrReceiveService = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSendingOrReceivingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isAlive { activityContext->

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

            bindService()
            init()
            observeConnectedDevice()
            binding.apply {
                progressIndicator.max = 100
                sendingDataTextView.text = getString(R.string.receiving_data)
                binding.headerLayout.title = getString(R.string.receive)
                topTextView.text = getString(R.string.data_receiving_is_in_progress)


                binding.btnCancelSending.setSafeOnClickListener {
                    if (isAdded) {
                        showCancelDialog(R.layout.sendingfaildialog) {
                            Log.d(TAG, "Cancel confirmed. Unbinding service.")

                            isAlive {
                                sendOrReceiveService?.stopService()
                                if (mBound) {
                                    activity?.unbindService(connection)
                                    mBound = false
                                }
                                findNavController().navigate(R.id.action_receivingDataSendifyFragment_to_homeSendifyFragment)
                            }
                        }
                    } else {
                        Log.e(TAG, "Fragment is not attached. Cannot show cancel dialog.")
                    }
                }



                binding.headerLayout.setNavigationOnClickListener {
                    if (isAdded) {
                        showCancelDialog(R.layout.sendingfaildialog) {
                            Log.d(TAG, "Cancel confirmed. Unbinding service.")

                            isAlive {
                                sendOrReceiveService?.stopService()
                                if (mBound) {
                                    activity?.unbindService(connection)
                                    mBound = false
                                }
                                findNavController().navigate(R.id.action_receivingDataSendifyFragment_to_homeSendifyFragment)
                            }
                        }
                    } else {
                        Log.e(TAG, "Fragment is not attached. Cannot show cancel dialog.")
                    }
                }
            }
        }
    }

    private fun showCancelDialog(layoutResId: Int, onAllowClicked: () -> Unit) {
        context?.let { act ->
            if (!isAdded || activity == null) {
                Log.e(TAG, "Fragment not attached. Skipping dialog display.")
                return
            }
            val dialogView = LayoutInflater.from(act).inflate(layoutResId, null)
            val yesBtn: TextView = dialogView.findViewById(R.id.txtYes)
            val noBtn: TextView = dialogView.findViewById(R.id.txtNo)

            val defaultColor = ContextCompat.getColor(act, R.color.grey_black)
            val primaryColor = ContextCompat.getColor(act, R.color.colorPrimary)

            yesBtn.setTextColor(defaultColor)
            noBtn.setTextColor(defaultColor)

            val dialog = MyDialogBox.getInstance(act as Activity)
                ?.setContentViewWithDismissCallBack(dialogView, true, 0.85f) {}
                ?.showDialog()

            yesBtn.setOnClickListener {

                onAllowClicked.invoke()

                dialog?.dismiss()
            }

            noBtn.setOnClickListener {
                noBtn.setTextColor(primaryColor)
                yesBtn.setTextColor(defaultColor)
                dialog?.dismiss()
            }
        } ?: Log.e(TAG, "Context is null. Cannot display dialog.")
    }




    private fun init() {
        Log.d("ReceiverFragment", "Initializing WifiDirectManager")
        activity?.let { act ->
            wifiDirectManager = WifiDirectManager(act, lifecycle.coroutineScope, isSender = false)
        }
        wifiDirectManager?.checkIsDeviceConnected()
    }

    private fun bindService() {
        val intent = Intent(activity, SendOrReceiveService::class.java)
        activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        val intent = Intent(activity, SendOrReceiveService::class.java)
        activity?.stopService(intent)
        // Unbind the service and set the flag to false
        if (mBound) {
            Log.d("awaisshakeel","service is bound 1")
            activity?.unbindService(connection)
            mBound = false
        } else {
            Log.d("awaisshakeel","service is not bound 1")
        }
        wifiDirectManager?.disconnect {  }
    }

    private fun observeConnectedDevice() {
        lifecycleScope.launch {
            TransferStateManager.fileReceivingState.collect {
                when (it.state) {
                    TransferState.INITIAL_STATE -> {
                        Log.d("TransferState", "State: INITIAL_STATE")
                        isAlive { act ->
                            AlertDialogManager.createCustomDialog(
                                act,
                                getString(R.string.transfer_initialized),
                                getString(R.string.transfer_has_been_initialized),
                                getString(R.string.ok)
                            ) {}.showDialogSafely(act)
                        }
                    }

                    TransferState.STARTING_TRANSFER_STATE -> {
                        Log.d("TransferState", "State: STARTING_TRANSFER_STATE")
                        isAlive { act ->
                            AlertDialogManager.createCustomDialog(
                                act,
                                getString(R.string.transfer_Starting),
                                getString(R.string.transfer_is_starting_please_wait),
                                getString(R.string.ok)
                            ) {}.showDialogSafely(act)
                        }
                    }

                    TransferState.TRANSFERRING_STATE -> {
                        Log.d("TransferState", "State: TRANSFERRING_STATE")
                        isAlive { act ->

                            binding.apply {
                                progressIndicator.setProgressCompat(it.percentage, false)
                                percentAgeTextView.text = "${it.percentage}%"
                            }
                        }
                    }

                    TransferState.TRANSFER_COMPLETE_STATE -> {
                        Log.d("TransferState", "State: TRANSFER_COMPLETE_STATE")
                        isAlive {activityContext->
                            try {
                                if (isAdded) {

                                    if( PrefUtil(requireContext()).getBool("is_premium", false) || !InterstitialClass.isInternetAvailable(requireContext())){
                                        findNavController().navigate(R.id.action_receivingDataSendifyFragment_to_receivedDataSendifyFragment)
                                    } else{
                                        InterstitialClass.request_interstitial(
                                            requireContext(),
                                            requireActivity(),
                                            getString(R.string.inter_send_receive_button)
                                        ) {
                                            Handler(Looper.getMainLooper()).postDelayed({
                                                isAlive {
                                                    findNavController().navigate(R.id.action_receivingDataSendifyFragment_to_receivedDataSendifyFragment)
                                                    Log.d("Navigate___", "Navigation to ReceivedDataFragment successful")
                                                }
                                            }, 200L)
                                        }
                                    }
                                } else {
                                    Log.e("Navigate___", "Fragment not attached. Cannot navigate.")
                                }
                            } catch (e: Exception) {
                                Log.e("Navigate___", "Navigation failed: ${e.localizedMessage}")
                            }
                        }
                    }
                    TransferState.TRANSFER_FAILED_STATE -> {
                        Log.e("TransferState", "State: TRANSFER_FAILED_STATE")

                    }

                    TransferState.TRANSFER_CANCELLED_STATE -> {
                        Log.d("TransferState", "State: TRANSFER_CANCELLED_STATE")
                        isAlive { act ->
                            AlertDialogManager.createCustomDialog(
                                act,
                                "Sender Failed Sending Process",
                                getString(R.string.the_file_transfer_was_cancelled),
                                getString(R.string.ok)
                            ) {
                                findNavController().navigate(R.id.action_receivingDataSendifyFragment_to_homeSendifyFragment)
                            }.showDialogSafely(act)
                        }
                    }
                    TransferState.TRANSFER_ASK_RECEIVER_STATE -> {
                        Log.d("TransferState", "State: TRANSFER_ASK_RECEIVER_STATE")

                    }

                    TransferState.CONNECTION_TIMEOUT_STATE -> {
                        Log.d("TransferState", "State: CONNECTION_TIMEOUT_STATE")
                        isAlive { act ->
                            AlertDialogManager.createCustomDialog(
                                act,
                                getString(R.string.connection_timeout),
                                getString(R.string.the_connection_has_timed_out),
                                getString(R.string.ok)
                            ) {}.showDialogSafely(act)
                        }
                    }
                }
            }
        }
    }
}