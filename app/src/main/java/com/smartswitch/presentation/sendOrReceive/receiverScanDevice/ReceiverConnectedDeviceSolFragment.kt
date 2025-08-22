package com.smartswitch.presentation.sendOrReceive.receiverScanDevice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdView
import com.smartswitch.R
import com.smartswitch.ads.banner_ads.setupBannerAd
import com.smartswitch.databinding.FragmentReceiverConnectedDeviceSolBinding

import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.AlertDialogManager
import com.smartswitch.utils.AlertDialogManager.dismissDialogSafely
import com.smartswitch.utils.AlertDialogManager.showDialogSafely
import com.smartswitch.utils.MyDialogBox
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.TransferStateManager
import com.smartswitch.utils.WifiDirectManager
import com.smartswitch.utils.enums.TransferState

import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.service.SendOrReceiveService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ReceiverConnectedDeviceSolFragment : Fragment() {
    private var _binding : FragmentReceiverConnectedDeviceSolBinding? = null
    private val binding get() = _binding!!

    private var wifiDirectManager: WifiDirectManager? = null
    var sendOrReceiveService: SendOrReceiveService? = null
    var mBound = false

    val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(" ", "Service connected: $name")
            val binder = service as SendOrReceiveService.LocalBinder
            sendOrReceiveService = binder.getService()
            mBound = true

            startReceiving()
            observeTransferState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("ReceiverFragment", "Service disconnected: $name")
            mBound = false
            sendOrReceiveService = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReceiverConnectedDeviceSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ReceiverFragment", "onViewCreated called")
        isAlive { act ->

            checkForPermissionAndInitWifiManager()
            setListeners()
            (act as FragmentActivity).handleBackPressWithAction {
                Log.d("ReceiverFragment", "Back pressed, navigating back")
                val intent = Intent(activity, SendOrReceiveService::class.java)
                activity?.stopService(intent)
                findNavController().popBackStack()
            }

            binding.headerLayout.setNavigationOnClickListener {
                Log.d("ReceiverFragment", "Back pressed Top")
                val intent = Intent(activity, SendOrReceiveService::class.java)
                activity?.stopService(intent)
                findNavController().popBackStack()
            }


            if ( PrefUtil(requireContext()).getBool("is_premium", false)) {
                binding.adRel.gone()
            } else {

                        var initialLayoutComplete = false
                        binding.adViewContainer.apply {
                            addView(AdView(act))
                            viewTreeObserver.addOnGlobalLayoutListener {
                                if (!initialLayoutComplete) {
                                    initialLayoutComplete = true
                                    binding.adViewContainer.setupBannerAd(
                                        act,
                                        getString(R.string.banner_all)
                                    )
                                }
                            }


                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("ReceiverFragment", "onDestroyView called, cleaning up resources")
        _binding = null
        if (mBound) {
            activity?.unbindService(connection)
            Log.d("ReceiverFragment", "Service unbound")
            mBound = false
        }
    }

    private fun init() {
        Log.d("ReceiverFragment", "Initializing WifiDirectManager")
        activity?.let { act ->
            wifiDirectManager = WifiDirectManager(act, lifecycle.coroutineScope, isSender = false)
        }
        observeConnection()
        observeWifiState()
        wifiDirectManager?.checkIsDeviceConnected()
    }


    private fun observeWifiState() {

        lifecycleScope.launch {
            wifiDirectManager?.isWifiOn?.collect { isWifiEnabled ->
                if (!isWifiEnabled) {
                    checkForPermissionAndInitWifiManager()
                } else {
                    isAlive {
                        AlertDialogManager.waitingDialog?.dismissDialogSafely(it)
                    }
                }
            }
        }
    }

    private fun setListeners() {
        Log.d("ReceiverFragment", "Setting up listeners")
        binding.apply {
            activity?.let { act ->
                binding.btnStartSending.setOnClickListener {

                }


                disconnectedTextView.setOnClickListener {
                    Log.d("ReceiverFragment", "Disconnected text view clicked, stopping connection")
                    wifiDirectManager?.disconnect {
                        Log.d("ReceiverFragment", "Connection stopped, navigating back")
                        val intent = Intent(activity, SendOrReceiveService::class.java)
                        activity?.stopService(intent)
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun startReceiving(){
        if (sendOrReceiveService != null) {
            Log.d("ReceiverFragment", "Starting receiving from sender")
            // Notify the sender of receiver readiness
            sendOrReceiveService?.startReceivingFromSender()
        } else {
            Log.e("ReceiverFragment", "SendOrReceiveService is null")
            // Optionally show an error dialog
            isAlive { act ->
                AlertDialogManager.createCustomDialog(
                    act,
                    getString(R.string.service_error),
                    getString(R.string.unable_to_start_receiving_Please_try_again),
                    getString(R.string.ok)
                ) {}.showDialogSafely(act)
            }
        }
    }


    private fun showSendFailDialog(layoutResId: Int,onAllowClicked: () -> Unit){
        context?.let { act->
            val dialogView=LayoutInflater.from(act).inflate(layoutResId,null)

            val cancelBtn: TextView =dialogView.findViewById(R.id.tvCancel)
            val tryAgainBtn: TextView =dialogView.findViewById(R.id.txtTryAgain)

            val defaultColor= ContextCompat.getColor(act,R.color.grey_black)
            val primaryColor= ContextCompat.getColor(act,R.color.colorPrimary)

            val dialog= MyDialogBox.getInstance(requireActivity())
                ?.setContentViewWithDismissCallBack(dialogView,true,0.85f){
                }?.showDialog()

            tryAgainBtn.setOnClickListener{
                tryAgainBtn.setTextColor(primaryColor)
                cancelBtn.setTextColor(defaultColor)
                onAllowClicked.invoke()
                dialog?.dismiss()

            }
            cancelBtn.setOnClickListener {
                cancelBtn.setTextColor(primaryColor)
                tryAgainBtn.setTextColor(defaultColor)
                onAllowClicked.invoke()
                dialog?.dismiss()
            }
        }
    }

    private fun observeConnection() {
        Log.d("" +
                "", "Observing connection state")
        lifecycleScope.launch {
            wifiDirectManager?.connectedDevice?.collect { device ->
                Log.d("ReceiverFragment", "Connection state changed: device = $device")
                if (device != null) {
                    Log.d("ReceiverFragment", "Device connected, starting receiving service")
                    startReceivingService()
                } else {
                    Log.d("ReceiverFragment", "No device connected")
                }
            }
        }
    }

    private fun startReceivingService() {
        Log.d("ReceiverFragment", "Starting and binding receiving service")
        val intent = Intent(activity, SendOrReceiveService::class.java)
        activity?.startService(intent)
        activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }


    private fun observeTransferState() {
        Log.d("ReceiverFragment", "Observing file transfer state")
        lifecycleScope.launch {
            TransferStateManager.fileReceivingState.collect {
                when (it.state) {
                    TransferState.INITIAL_STATE -> {
                        Log.d("TransferState", "State: INITIAL_STATE - Transfer initialized")

                    }

                    TransferState.STARTING_TRANSFER_STATE -> {
                        Log.d("TransferState", "State: STARTING_TRANSFER_STATE - Transfer starting")
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
                        Log.d("TransferState", "State: TRANSFERRING_STATE - Transfer in progress")
                        isAlive { act ->
                            AlertDialogManager.waitingDialog?.dismissDialogSafely(act as FragmentActivity)
                            Log.d("ReceiverFragment", "Navigating to ReceivingDataFragment")
                            findNavController().navigate(R.id.action_receiverConnectedDeviceSendifyFragment_to_receivingDataSendifyFragment)
                        }
                    }

                    TransferState.TRANSFER_COMPLETE_STATE -> {
                        Log.d("TransferState", "State: TRANSFER_COMPLETE_STATE - Transfer complete")

                    }

                    TransferState.TRANSFER_FAILED_STATE -> {
                        Log.e("TransferState", "State: TRANSFER_FAILED_STATE - Transfer failed")
                        isAlive { act ->
                            showSendFailDialog(R.layout.sendcanceldialog){

                            }


                        }
                    }

                    TransferState.TRANSFER_CANCELLED_STATE -> {
                        Log.d(
                            "TransferState",
                            "State: TRANSFER_CANCELLED_STATE - Transfer cancelled"
                        )
                        isAlive { act ->
                            AlertDialogManager.createCustomDialog(
                                act,
                                getString(R.string.transfer_Cancelled),
                                getString(R.string.the_file_transfer_was_cancelled),
                                getString(R.string.ok)
                            ) {}.showDialogSafely(act)
                        }
                    }

                    TransferState.TRANSFER_ASK_RECEIVER_STATE -> {
                        Log.d(
                            "TransferState",
                            "State: TRANSFER_ASK_RECEIVER_STATE - Waiting for receiver"
                        )


                    }

                    TransferState.CONNECTION_TIMEOUT_STATE -> {
                        Log.d(
                            "TransferState",
                            "State: CONNECTION_TIMEOUT_STATE - Connection timed out"
                        )
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


    private fun checkForPermissionAndInitWifiManager() {
        activity?.let { act ->
            when {
                !PermissionManager.isWifiEnable(act) -> {
                    Log.e("ReceiverFragment", "Wi-Fi is not enabled.")
                    showPermissionErrorDialog(
                        getString(R.string.wifi_disabled),
                        getString(R.string.wifi_must_be_enabled_to_continue)
                    )
                    return
                }
                !PermissionManager.isGpsEnabled(act) -> {
                    Log.e("ReceiverFragment", "GPS is not enabled.")
                    showPermissionErrorDialog(
                        getString(R.string.gps_disabled),
                        getString(R.string.gps_must_be_enabled_to_continue)
                    )
                    return
                }
                !PermissionManager.hasLocationAndNearbyPermission(act) -> {
                    Log.e("ReceiverFragment", "Required permissions are missing.")
                    showPermissionErrorDialog(
                        getString(R.string.permissions_missing),
                        getString(R.string.location_and_Nearby_permissions_must_be_granted_to_continue)
                    )
                    return
                }
                else -> {
                    Log.d("ReceiverFragment", "All permissions and settings are satisfied.")
                    init()
                }
            }
        }
    }

    private fun showPermissionErrorDialog(title: String, message: String) {
        isAlive { act ->
            AlertDialogManager.createCustomDialog(
                act,
                title,
                message,
                getString(R.string.ok)
            ) {
                findNavController().popBackStack() // Navigate back after user acknowledgment.
            }.showDialogSafely(act)
        }
    }

}