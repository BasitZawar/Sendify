package com.smartswitch.presentation.sendOrReceive.senderScanDevice

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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdView
import com.smartswitch.R
import com.smartswitch.ads.banner_ads.setupBannerAd
import com.smartswitch.databinding.FragmentSenderConnectedDeviceSolBinding
import com.smartswitch.domain.model.MediaInfoModel

import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.AlertDialogManager
import com.smartswitch.utils.AlertDialogManager.dismissDialogSafely
import com.smartswitch.utils.AlertDialogManager.showDialogSafely
import com.smartswitch.utils.FileUtils
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.TransferStateManager
import com.smartswitch.utils.WifiDirectManager
import com.smartswitch.utils.enums.MediaTypeEnum
import com.smartswitch.utils.enums.TransferState

import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.service.SendOrReceiveService
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SenderConnectedDeviceSolFragment : Fragment() {
    private var _binding : FragmentSenderConnectedDeviceSolBinding? = null
    private val binding get() = _binding!!

    private var wifiDirectManager: WifiDirectManager? = null
    var sendOrReceiveService: SendOrReceiveService? = null
    var mBound = false

    companion object {
        const val TAG = "SenderConnectedDevice"
    }

    val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "onServiceConnected")
            val binder = service as SendOrReceiveService.LocalBinder
            sendOrReceiveService = binder.getService()
            mBound = true
            observeTransferringState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected")
            mBound = false
            sendOrReceiveService = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
         _binding = FragmentSenderConnectedDeviceSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isAlive { act ->
            checkForPermissionAndInitWifiManager()
            setListeners()

            (act as FragmentActivity).handleBackPressWithAction {
                Log.d(TAG, "Back pressed, navigating back")
                val intent = Intent(activity, SendOrReceiveService::class.java)
                activity?.stopService(intent)
                findNavController().popBackStack()
            }

            binding.headerLayout.setNavigationOnClickListener {
                Log.d(TAG, "Back pressed Top")
                findNavController().popBackStack()
            }

            showBannerAds(act)
        }
    }

    private fun init() {
        activity?.let { act ->
            wifiDirectManager = WifiDirectManager(act, lifecycle.coroutineScope, isSender = true)
            observeConnectedDevice()
            observeWifiState()
            createContactsFileIfSelected()
            wifiDirectManager?.checkIsDeviceConnected()
        }
    }

    private fun observeConnectedDevice() {
        lifecycleScope.launch {
            wifiDirectManager?.connectedDevice?.collect { device ->
                if (device != null) {
                    Log.d(TAG, "Device connected: ${device.networkName}")
                    startSendingService()
                } else {
                    Log.d(TAG, "No device connected.")
                }
            }
        }
    }

    private fun observeWifiState() {
        lifecycleScope.launch {
            wifiDirectManager?.isWifiOn?.collect { isWifiEnabled ->
                if (!isWifiEnabled) {
                    Toast.makeText(context, getString(R.string.wifi_is_turned_off), Toast.LENGTH_SHORT).show()
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
        binding.apply {
            activity?.let { act ->
                btnStartSending.setOnClickListener {
                    Log.d("sendStart___", "sending start")
                    sendOrReceiveService?.startSendingToReceiver()
                }
                cancelTextView.setOnClickListener {
                    wifiDirectManager?.disconnect {
                        //  AlertDialogManager.createWaitingDialog(act).show()
                        val intent = Intent(activity, SendOrReceiveService::class.java)
                        activity?.stopService(intent)
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun startSendingService() {
        val intent = Intent(activity, SendOrReceiveService::class.java)
        activity?.startService(intent)
        activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (mBound) {
            activity?.unbindService(connection)
            mBound = false
        }
    }

    private fun observeTransferringState() {
        Log.d("TransferState", "observeTransferringState")
        Log.d("TransferState", "observeTransferringState : ${TransferStateManager.fileReceivingState.value.state}")
        lifecycleScope.launch {
            TransferStateManager.fileSendingState.collect {
                when (it.state) {
                    TransferState.INITIAL_STATE -> {
                        Log.d("TransferState", "State: INITIAL_STATE")

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

                            AlertDialogManager.waitingDialog?.dismissDialogSafely(act as FragmentActivity)
                            findNavController().navigate(R.id.action_senderConnectedDeviceSendifyFragment_to_sendingOrReceivingFragment4)
                        }
                    }

                    TransferState.TRANSFER_COMPLETE_STATE -> {

                    }

                    TransferState.TRANSFER_FAILED_STATE -> {
                        Log.e("TransferState", "State: TRANSFER_FAILED_STATE")

                    }

                    TransferState.TRANSFER_CANCELLED_STATE -> {
                        Log.d("TransferState", "State: TRANSFER_CANCELLED_STATE")
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



    private fun checkForPermissionAndInitWifiManager() {

        activity?.let { act ->
            when {
                !PermissionManager.isWifiEnable(act) -> {
                    navigateToErrorFragment(getString(R.string.wifi_is_disabled_Please_enable_wifi))
                    return
                }
                !PermissionManager.isGpsEnabled(act) -> {
                    navigateToErrorFragment(getString(R.string.gps_is_disabled_Please_enable_gps))
                    return
                }
                !PermissionManager.hasLocationAndNearbyPermission(act) -> {
                    navigateToErrorFragment(getString(R.string.location_permission_is_required))
                    return
                }
            }
            init()
        }
    }

    private fun navigateToErrorFragment(errorMessage: String) {
        findNavController().navigate(R.id.action_senderConnectedDeviceSendifyFragment_to_errorSendifyFragment)
    }


    private fun createContactsFileIfSelected() {
        val contactsList = SelectedListManager.getSelectedContactsList()
        if (contactsList.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                activity?.let { act ->
                    val contactFile = FileUtils.generateVcfFile(context = act, contactsList)

                    if (contactFile?.exists() == true && contactFile.length() > 0) {
                        SelectedListManager.addSelectedMedia(
                            MediaInfoModel(
                                contactFile.name,
                                contactFile.path,
                                contactFile.length(),
                                mediaType = MediaTypeEnum.CONTACTS
                            )
                        )
                    } else {
                        Log.i(TAG, "createContactsFileIfSelected: generating file error")
                    }
                }
            }
        }
    }


    private fun showBannerAds(act : FragmentActivity){
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