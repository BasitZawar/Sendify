package com.smartswitch.utils.wifiutils

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.smartswitch.utils.WifiDirectManager
import com.smartswitch.utils.WifiDirectManager.Companion.RECEIVER_TAG
import com.smartswitch.utils.WifiDirectManager.Companion.SENDER_TAG
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WifiP2pReceiver(
    private val wifiManager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private var wifiDirectManager: WifiDirectManager?,
    private var lifecycleCoroutineScope: LifecycleCoroutineScope?,
    var onPeersAvailable: (peers: WifiP2pDeviceList) -> Unit = {},
    var isWifiOn: (isWifiOn: Boolean) -> Unit = {},
    var onWifiP2pConnectionChanged: () -> Unit = {},
    var checkUserRejectRequest: () -> Unit = {},
    var deviceName: (String) -> Unit = {},
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Indicate whether Wi-Fi Direct is enabled or not
                Log.i(SENDER_TAG, "onReceive:WIFI_P2P_STATE_CHANGED_ACTION ")
                Log.i(RECEIVER_TAG, "onReceive:WIFI_P2P_STATE_CHANGED_ACTION ")
                Log.i("Receiver_Device_A", "onReceive:WIFI_P2P_STATE_CHANGED_ACTION ")
                Log.i("Receiver_Device_B", "onReceive:WIFI_P2P_STATE_CHANGED_ACTION ")
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    isWifiOn(true)
                } else {
                    isWifiOn(false)
                }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Indicate a change in the list of available peers
                Log.i(SENDER_TAG, "onReceive:WIFI_P2P_PEERS_CHANGED_ACTION ")
                Log.i(RECEIVER_TAG, "onReceive:WIFI_P2P_PEERS_CHANGED_ACTION ")
                Log.i("Receiver_Device_A", "onReceive:WIFI_P2P_PEERS_CHANGED_ACTION ")
                Log.i("Receiver_Device_B", "onReceive:WIFI_P2P_PEERS_CHANGED_ACTION ")
                wifiManager.requestPeers(channel) { peers ->
                    onPeersAvailable(peers)
                }
            }

            // TODO : what its mean ,my device is not connected to another device and not fetching another device and its also called few munutes or seconds
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                /*
                * Indicates the state of Wi-Fi Direct connectivity has changed. Starting with Android 10,
                * this is not sticky. If your app has relied on receiving these broadcasts at registration because they had been sticky,
                * use the appropriate get method at initialization to obtain the information instead.
                * */

                // Indicates the state of Wi-Fi Direct connectivity has changed
                Log.i(SENDER_TAG, "onReceive:WIFI_P2P_CONNECTION_CHANGED_ACTION ")
                Log.i(RECEIVER_TAG, "onReceive:WIFI_P2P_CONNECTION_CHANGED_ACTION ")
                Log.i("Receiver_Device_A", "onReceive:WIFI_P2P_CONNECTION_CHANGED_ACTION ")
                Log.i("Receiver_Device_B", "onReceive:WIFI_P2P_CONNECTION_CHANGED_ACTION ")
                wifiManager.requestConnectionInfo(channel) { info ->
                    // Handle connection info
                    onWifiP2pConnectionChanged()
                }
            }

            // TODO : Whats mean this , i mean when trigger it and why , whats mean this device change action
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                /*
                * Indicates this device's configuration details have changed. Starting with Android 10,
                * this is not sticky. If your app has relied on receiving these broadcasts at registration because they had been sticky,
                * use the appropriate get method at initialization to obtain the information instead.
                * */

                // Indicates this device's details have changed.
                // Android automatically negotiates the connection when another device connects.
                Log.i(SENDER_TAG, "onReceive: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ")
                Log.i(RECEIVER_TAG, "onReceive: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ")
                Log.i("Receiver_Device_A", "onReceive: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ")
                Log.i("Receiver_Device_B", "onReceive: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ")
                // Retrieve the WifiP2pDevice object from the intent
                val device: WifiP2pDevice? =
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                device?.let {
                    val deviceName = it.deviceName
                    Log.i(SENDER_TAG, "Current Wi-Fi Direct Device Name: $deviceName")
                    Log.i(RECEIVER_TAG, "Current Wi-Fi Direct Device Name: $deviceName")
                    Log.i("Receiver_Device_A", "Current Wi-Fi Direct Device Name: $deviceName")
                    Log.i("Receiver_Device_B", "Current Wi-Fi Direct Device Name: $deviceName")

                    deviceName(deviceName)
                    // Use deviceName as needed
                }
                checkUserRejectRequest()
            }

            // NOTE:
            // This block handles the WIFI_P2P_DISCOVERY_CHANGED_ACTION broadcast,
            // which is triggered when Wi-Fi Direct peer discovery starts or stops.
            //
            // ⚠️ Even if we do NOT explicitly call discoverPeers() on the receiver device,
            // this broadcast can still be received due to internal system behavior.
            //
            // Specifically, calling removeGroup() or createGroup() may internally restart
            // discovery or reset the Wi-Fi P2P state machine, causing this action to fire.
            //
            // ✅ To avoid unintended discovery behavior on the receiver side, we check
            // if the device is in sender mode (wifiDirectManager?.isSender == true)
            // before reacting to this broadcast.
            //
            // This ensures only the sender initiates or restarts discovery as needed,
            // while the receiver simply listens and avoids unnecessary peer scanning.

            WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                Log.i(SENDER_TAG, "onReceive:WIFI_P2P_DISCOVERY_CHANGED_ACTION ")
                Log.i(RECEIVER_TAG, "onReceive:WIFI_P2P_DISCOVERY_CHANGED_ACTION ")
                Log.i("Receiver_Device_A", "onReceive:WIFI_P2P_DISCOVERY_CHANGED_ACTION ")
                Log.i("Receiver_Device_B", "onReceive:WIFI_P2P_DISCOVERY_CHANGED_ACTION ")

                if (wifiDirectManager?.isSender != true) {
                    Log.i(SENDER_TAG, "Not restarting discovery – device is sender ")
                    Log.i(RECEIVER_TAG, "Not restarting discovery – device is receiver ")
                    return
                }

                val discoveryState = intent.getIntExtra(
                    WifiP2pManager.EXTRA_DISCOVERY_STATE,
                    WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED
                )
                when (discoveryState) {
                    WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED -> {
                        wifiDirectManager?.isDiscoveryStarted = true
                        Log.i(SENDER_TAG, "onReceive:WIFI_P2P_DISCOVERY_STARTED ")
                        Log.i(RECEIVER_TAG, "onReceive:WIFI_P2P_DISCOVERY_STARTED ")
                        Log.i("Receiver_Device_A", "onReceive:WIFI_P2P_DISCOVERY_STARTED ")
                        Log.i("Receiver_Device_B", "onReceive:WIFI_P2P_DISCOVERY_STARTED ")
                    }

                    WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED -> {
                        wifiDirectManager?.isDiscoveryStarted = false
                        Log.i(SENDER_TAG, "onReceive:WIFI_P2P_DISCOVERY_STOPPED ")
                        Log.i(RECEIVER_TAG, "onReceive:WIFI_P2P_DISCOVERY_STOPPED ")
                        Log.i("Receiver_Device_A", "onReceive:WIFI_P2P_DISCOVERY_STOPPED ")
                        Log.i("Receiver_Device_B", "onReceive:WIFI_P2P_DISCOVERY_STOPPED ")

                        lifecycleCoroutineScope?.launch {
                            delay(2000)
                            WifiDirectUtils.isDeviceConnected(
                                wifiManager, channel
                            ) { isConnected ->
                                if (!isConnected) {
                                    wifiDirectManager?.startDiscovery()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

