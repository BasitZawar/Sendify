package com.smartswitch.presentation.sendOrReceive.receiverScanDevice

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdView
import com.smartswitch.R
import com.smartswitch.ads.banner_ads.setupBannerAd
import com.smartswitch.databinding.FragmentReceiverScanDeviceSolBinding

import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.AlertDialogManager
import com.smartswitch.utils.AlertDialogManager.createCustomDialogWithNoButton
import com.smartswitch.utils.AlertDialogManager.dismissDialogSafely
import com.smartswitch.utils.AlertDialogManager.showDialogSafely
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.WifiDirectManager

import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch


class ReceiverScanDeviceSolFragment : Fragment() {
    private var _binding: FragmentReceiverScanDeviceSolBinding? = null
    private val binding get() = _binding!!

    private var wifiDirectManager: WifiDirectManager? = null

    private lateinit var enableGpsLauncher: ActivityResultLauncher<Intent>
    private lateinit var locationLauncher: ActivityResultLauncher<Intent>
    private lateinit var enableWifiLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReceiverScanDeviceSolBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isAlive { act ->
            registerEnableGpsLocationLauncher()
            registerLocationLauncher()
            registerEnableWifiLauncher()
            checkForPermissionAndInitWifiManager()
            initClickListener()

            (act as FragmentActivity).handleBackPressWithAction {

                isAlive {
                    findNavController().navigateUp()
                }
            }

            showBannerAds(act)
        }
    }

    private fun registerEnableGpsLocationLauncher() {
        enableGpsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
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
            wifiDirectManager = WifiDirectManager(act, lifecycle.coroutineScope, isSender = false)
        }

        wifiDirectManager?.init()
        wifiDirectManager?.checkIsDeviceConnected()
        observeConnection()
        observeWifiState()
    }

    private fun initClickListener() {
        binding.apply {
            headerLayout.setNavigationOnClickListener {
                // TODO : findNavController().navigate(R.id.action_receiverScanDeviceFragment_to_homeFragment)
                findNavController().navigateUp()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeConnection() {

        // Observe connectedDevice
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                wifiDirectManager?.connectedDevice?.catch { e ->
                    Log.e("ReceiverScanDeviceFragment", "Error observing connection: ${e.message}", e)
                    Log.e("mCHECK___", "Error observing connection: ${e.message}", e)

                }?.collect { device ->
                    if (device != null) {
                        isAlive {
                            if (isAdded) {
                                findNavController().navigate(R.id.action_receiverScanDeviceSendifyFragment_to_receiverConnectedDeviceSendifyFragment)
                            } else {
                                Log.w("ReceiverScanDeviceFragment", "Fragment not added, navigation skipped")
                            }
                        }
                    } else {
                        Log.d("ReceiverScanDeviceFragment", "No connected device found")
                    }
                }
            }
        }

        // Observe deviceName
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                wifiDirectManager?.deviceName?.collect { deviceName ->
                    if (view != null) {
                        binding.topTextView.text = getString(R.string.connect_device_text,deviceName)
                    }
                }
            }
        }
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

    private fun requestLocationPermissionWithDialog() {
        activity?.let { act ->
            val title = getString(R.string.permission)
            val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getString(R.string.please_enable_location_and_nearby_permission_to_continue)
            } else {
                getString(R.string.please_enable_location_permission_to_continue)
            }
            val positiveButtonText = getString(R.string.ok)
            AlertDialogManager.createCustomDialog(
                act,
                title,
                message,
                positiveButtonText
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
                    //findNavController().navigateUp()
                }
            ).showDialogSafely(act)
        }
    }

    private fun requestEnableWifiWithDialog() {
        activity?.let { act ->
            AlertDialogManager.createCustomDialog(
                act,
                getString(R.string.enable_wifi),
                getString(R.string.please_enable_Wifi_to_continue),
                getString(R.string.ok)
            ) {
                PermissionManager.openWifiSettings(enableWifiLauncher)
            }.showDialogSafely(act)
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
    }

    private fun showBannerAds(act : FragmentActivity){
        if (PrefUtil(requireContext()).getBool("is_premium", false)) {
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