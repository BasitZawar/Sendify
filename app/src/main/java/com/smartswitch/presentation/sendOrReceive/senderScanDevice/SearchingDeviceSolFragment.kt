package com.smartswitch.presentation.sendOrReceive.senderScanDevice

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdView
import com.smartswitch.R
import com.smartswitch.ads.banner_ads.setupBannerAd
import com.smartswitch.databinding.FragmentSearchingDeviceSolBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.presentation.adapter.ScanDeviceAdapter

import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.AlertDialogManager
import com.smartswitch.utils.AlertDialogManager.createCustomDialog
import com.smartswitch.utils.AlertDialogManager.createCustomDialogWithNoButton
import com.smartswitch.utils.AlertDialogManager.dismissDialogSafely
import com.smartswitch.utils.AlertDialogManager.showDialogSafely
import com.smartswitch.utils.FileUtils
import com.smartswitch.utils.MyDialogBox
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.WifiDirectManager
import com.smartswitch.utils.enums.MediaTypeEnum

import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.wifiutils.WifiDirectUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull



class SearchingDeviceSolFragment : Fragment() {
    private var _binding: FragmentSearchingDeviceSolBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "SearchingDeviceFragment"
    }

    private var wifiDirectManager: WifiDirectManager? = null
    private var adapter: ScanDeviceAdapter? = null
    private lateinit var enableGpsLauncher: ActivityResultLauncher<Intent>
    private lateinit var locationLauncher: ActivityResultLauncher<Intent>
    private lateinit var enableWifiLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSearchingDeviceSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isAlive { activityContext ->
            Log.d(TAG, "onViewCreate")
            registerEnableGpsLocationLauncher()
            registerLocationLauncher()
            registerEnableWifiLauncher()
            checkForPermissionAndInitWifiManager()
            setupRecyclerview()
            observeList()
            initClickListener(activityContext)

            (activityContext as FragmentActivity).handleBackPressWithAction {
                sendCancelDialog(R.layout.sendbackdialog) {
                    wifiDirectManager?.stopDiscovery()
                    AlertDialogManager.waitingDialog?.dismissDialogSafely(activityContext)
                    findNavController().popBackStack()
                }

            }
            showBannerAds(activityContext)
        }
    }

    private fun registerEnableGpsLocationLauncher() {
        enableGpsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
                activity?.let { _ ->
                    checkForPermissionAndInitWifiManager()
                }
            }
    }

    //check for wifi enable, gps enable  permission and nearby permission if all given then start wifi Direct Manager
    private fun checkForPermissionAndInitWifiManager() {
        activity?.let { act ->
            if (PermissionManager.isWifiEnable(act)) {
                if (PermissionManager.isGpsEnabled(act)) {
                    if (PermissionManager.hasLocationAndNearbyPermission(act)) {
                        initWifiDirectManager()
                    } else {
                        requestLocationPermissionWithDialog()
                    }
                } else {
                    requestEnableGpsWithDialog()
                }
            } else {
                requestEnableWifiWithDialog()
            }
        }
    }

    private fun initWifiDirectManager() {
        activity?.let { act ->
            wifiDirectManager = WifiDirectManager(act, lifecycle.coroutineScope, isSender = true)
        }
        wifiDirectManager?.init()
        observeWifiState()
        observeConnectedDevice()
        binding.scaningDeviceGroup.isVisible = true
        binding.nearbyTxt.isVisible = true
        binding.rvDevicesList.isVisible = false
        binding.startScanButton.isVisible = false
        binding.deviceFoundList.isVisible = false
    }

    private fun observeWifiState() {
        lifecycleScope.launch {
            wifiDirectManager?.isWifiOn?.collect { isWifiEnabled ->
                if (!isWifiEnabled) {
                    requestEnableWifiWithDialog()
                } else {
                    isAlive {
                        AlertDialogManager.waitingDialog?.dismissDialogSafely(it)
                    }
                }
            }
        }
    }

    private fun observeConnectedDevice() {
        lifecycleScope.launch {
            activity?.let { act ->
                wifiDirectManager?.connectedDevice?.collect { device ->
                    if (device != null) {
                        isAlive {
                            AlertDialogManager.waitingDialog?.dismissDialogSafely(act)
                            // TODO :
                            findNavController().navigate(R.id.action_searchingDeviceSendifyFragment_to_senderConnectedDeviceSendifyFragment)
                        }
                    } else {
                        /* binding?.tvConnectedDevice?.text = "Connected to null"*/
                        Log.i(TAG, "observeConnectedDevice: Connected to null")
                    }
                }
            }
        }
    }

    private fun registerLocationLauncher() {
        locationLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                activity?.let { act ->
                    checkForPermissionAndInitWifiManager()
                }
            }
    }

    private fun registerEnableWifiLauncher() {
        enableWifiLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                activity?.let { act ->
                    checkForPermissionAndInitWifiManager()
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupRecyclerview() {
        activity?.let { act ->
            binding.apply {
                adapter = ScanDeviceAdapter { device ->
                    Log.d(
                        TAG,
                        "setupRecyclerview:============================================= $device"
                    )
                    Log.d("CHECKING", "Select : ${device.deviceName}")
                    AlertDialogManager.createWaitingDialog(act).showDialogSafely(act)




                    lifecycleScope.launch {


                        val isConnectionStarted = suspendCancellableCoroutine { continuation ->
                            wifiDirectManager?.connectWithDevice(device) { started ->

                                if (continuation.isActive) {
                                    continuation.resume(started) {
                                        Log.d("CHECKING", "continuation.resume : $started")
                                    }
                                } else {
                                    Log.w("CHECKING", "Continuation already resumed, ignoring duplicate callback.")
                                }
                            }
                        }

                        Log.i(TAG, "isConnectionStarted: $isConnectionStarted")
                        Log.d("CHECKING", "isConnectionStarted : $isConnectionStarted")

                        if (isConnectionStarted) {
                            // Wait for a connection to establish
                            val isConnected = withTimeoutOrNull(15000) { // Wait for 15 seconds
                                Log.i(TAG, "Waiting for 15000 sec connection to establish...")
                                Log.i(
                                    "CHECKING",
                                    "Waiting for 15000 sec connection to establish..."
                                )
                                while (wifiDirectManager?.connectedDevice?.value == null) {
                                    Log.i(
                                        TAG, "Waiting for while loop connection to establish."
                                    )
                                    Log.i(
                                        "CHECKING",
                                        "Waiting for while loop connection to establish."
                                    )
                                    Log.i(
                                        TAG,
                                        "wifiDirectManager?.connectedDevice?.value  : ${wifiDirectManager?.connectedDevice?.value}"
                                    )
                                    delay(1000) // Check every second
                                }
                                true
                            } ?: false

                            Log.i(
                                TAG,
                                "wifiDirectManager?.connectedDevice?.value  : ${wifiDirectManager?.connectedDevice?.value}"
                            )
                            Log.i("CHECKING", "isConnected: $isConnected")

                            if (!isConnected) {
                                Log.i(
                                    TAG,
                                    "Connection timed out for device: ${device.deviceName}"
                                )
                                Log.i(
                                    TAG,
                                    "Connection timed out for device: ${device.deviceName}"
                                )

                                AlertDialogManager.waitingDialog?.dismissDialogSafely(act)
                                binding.scaningDeviceGroup.isVisible = false
                                binding.nearbyTxt.isVisible = false
                                binding.deviceFoundList.isVisible = true
                                binding.rvDevicesList.isVisible = true
                                binding.startScanButton.isVisible = true
                                rvDevicesList.itemAnimator = null
                            }
                        } else {
                            Log.i(
                                TAG,
                                "Connection failed to start for device: ${device.deviceName}"
                            )
                            Log.i(
                                TAG,
                                "Connection failed to start for device: ${device.deviceName}"
                            )
                            AlertDialogManager.waitingDialog?.dismissDialogSafely(act)
                            binding.scaningDeviceGroup.isVisible = false
                            binding.nearbyTxt.isVisible = false
                            binding.deviceFoundList.isVisible = true
                            binding.rvDevicesList.isVisible = true
                            binding.startScanButton.isVisible = true
                            rvDevicesList.itemAnimator = null
                        }


                    }
                }
                rvDevicesList.adapter = adapter
            }
        }
    }

    private fun observeList() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            wifiDirectManager?.peersList?.collect { deviceList ->
                activity?.let { act ->
                    if (deviceList?.deviceList?.isNotEmpty() == true) {
                        binding.scaningDeviceGroup.isVisible = false
                        binding.nearbyTxt.isVisible = false
                        binding.rvDevicesList.isVisible = true
                        binding.startScanButton.isVisible = true
                        binding.deviceFoundList.isVisible = true
                        adapter?.submitList(deviceList.deviceList?.toList())
                    } else {
                        AlertDialogManager.waitingDialog?.dismissDialogSafely(act)
                        binding.scaningDeviceGroup.isVisible = true
                        binding.nearbyTxt.isVisible = true
                        binding.deviceFoundList.isVisible = false
                        binding.rvDevicesList.isVisible = false
                        binding.startScanButton.isVisible = false
                    }
                }
            }
        }
    }

    private fun initClickListener(context: Activity) {
        binding.apply {
            startScanButton.setOnClickListener {
                lifecycleScope.launch {
                    binding.deviceFoundList.isVisible = false
                    binding.scaningDeviceGroup.isVisible = true
                    binding.nearbyTxt.isVisible = true
                    binding.rvDevicesList.isVisible = false
                    binding.startScanButton.isVisible = false
                    delay(1000)
                    wifiDirectManager?.startDiscovery()
                }
            }
            headerLayout.setNavigationOnClickListener {
                sendCancelDialog(R.layout.sendbackdialog) {
                    wifiDirectManager?.stopDiscovery()
                    AlertDialogManager.waitingDialog?.dismissDialogSafely(context)
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun sendCancelDialog(layoutResId: Int, onAllowClicked: () -> Unit) {
        context?.let { context ->
            if (!isAdded || activity == null) {
                Log.e(ContentValues.TAG, "Fragment not attached. Skipping dialog display.")
                return
            }
            val dialogView = LayoutInflater.from(context).inflate(layoutResId, null)
            val noBtn: TextView = dialogView.findViewById(R.id.noBtn)
            val yesBtn: TextView = dialogView.findViewById(R.id.yesBtn)

            val dialog = MyDialogBox.getInstance(requireActivity())
                ?.setContentViewWithDismissCallBack(dialogView, true, 0.85f) {}
                ?.showDialog()


            yesBtn.setOnClickListener {
                if (isAdded) {
                    SelectedListManager.clearSelected()
                    onAllowClicked()
                    //findNavController().navigate(R.id.action_searchingDeviceFragment_to_homeFragment)
//                    findNavController().navigateUp()
                }
                dialog?.dismiss()
            }
            noBtn.setOnClickListener {
                checkForPermissionAndInitWifiManager()
                dialog?.dismiss()
            }
        } ?: Log.e(ContentValues.TAG, "Context is null. Cannot display dialog.")
    }

    private fun requestEnableWifiWithDialog() {
        activity?.let { act ->
            createCustomDialog(
                act,
                getString(R.string.enable_wifi),
                getString(R.string.please_enable_Wifi_to_continue),
                getString(R.string.ok)
            ) {
                PermissionManager.openWifiSettings(enableWifiLauncher)
            }.showDialogSafely(act)
        }
    }

    private fun requestLocationPermissionWithDialog() {
        activity?.let { act ->
            createCustomDialog(
                act,
                getString(R.string.location_permission),
                getString(R.string.please_enable_location_permission_to_continue),
                getString(R.string.ok)
            ) {
                PermissionManager.openAppSettings(act, locationLauncher)
            }.showDialogSafely(act)
        }
    }

    private fun requestEnableGpsWithDialog() {
        activity?.let { act ->


            createCustomDialogWithNoButton(
                act,
                getString(R.string.enable_gps),
                getString(R.string.please_enable_gps_to_continue),
                getString(R.string.ok),
                {
                    PermissionManager.promptUserToEnableGps(enableGpsLauncher)
                },
                {
                    sendCancelDialog(R.layout.sendbackdialog) {
                        wifiDirectManager?.stopDiscovery()
                        AlertDialogManager.waitingDialog?.dismissDialogSafely(act)
                        findNavController().popBackStack()
                    }
                }
            ).showDialogSafely(act)
        }
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

    override fun onResume() {
        super.onResume()
        wifiDirectManager?.registerReceiver()
    }

    override fun onPause() {
        super.onPause()
        wifiDirectManager?.unregisterReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiDirectManager?.stopDiscovery()
    }

    private fun showBannerAds(activityContext: FragmentActivity) {
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