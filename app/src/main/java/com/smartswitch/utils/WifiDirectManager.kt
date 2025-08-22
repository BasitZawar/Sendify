package com.smartswitch.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.smartswitch.utils.wifiutils.WifiDirectUtils
import com.smartswitch.utils.wifiutils.WifiP2pReceiver
import kotlinx.coroutines.flow.MutableStateFlow

/*class WifiDirectManager(
    private val context: Context,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
    private val isSender: Boolean = true
)
{
    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel = wifiP2pManager?.initialize(context, context.mainLooper, null)
    private var intentFilter = IntentFilter()
    private var wifiP2pReceiver: WifiP2pReceiver? = null

    // States
    var isDeviceConnected = false
    var isDiscoveryStarted = false
    private val TAG = "WifiDirectManager"

    // MutableStateFlows for observing state
    val peersList = MutableStateFlow<WifiP2pDeviceList?>(null)
    val isWifiOn = MutableStateFlow(true)
    val deviceName = MutableStateFlow("")
    val connectedDevice = MutableStateFlow<WifiP2pGroup?>(null)

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (isDiscoveryStarted) return

        wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isDiscoveryStarted = true
                Log.d(TAG, "Peer discovery started")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Peer discovery failed: $reasonCode")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (!isDiscoveryStarted) return

        wifiP2pManager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isDiscoveryStarted = false
                Log.d(TAG, "Peer discovery stopped")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Failed to stop peer discovery: $reasonCode")
            }
        })
    }

    fun init() {
        setupIntentFilter()
        setupReceiver(this@WifiDirectManager)

        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        isWifiOn.value = wifiManager.isWifiEnabled
        handleWifiStatus(wifiManager.isWifiEnabled)

        WifiDirectUtils.deletePersistentGroups(wifiP2pManager, channel) { }

        if (isSender) {
            if (!isDeviceConnected) {
                startDiscovery()
            }
        } else {
            recreateGroup()
        }
    }

    private fun setupIntentFilter() {
        intentFilter.apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
        }
    }

    private fun setupReceiver(wifiDi: WifiDirectManager) {
        wifiP2pReceiver = wifiP2pManager?.let { manager ->
            channel?.let { ch ->
                WifiP2pReceiver(
                    manager, ch, wifiDi, lifecycleCoroutineScope,
                    onPeersAvailable = { list ->
                        peersList.value = list
                        Log.i(TAG, "onPeersAvailable")
                    },
                    isWifiOn = { status ->
                        isWifiOn.value = status
                        handleWifiStatus(status)
                    },
                    onWifiP2pConnectionChanged = {
                        Log.i(TAG, "onWifiP2pConnectionChanged")
                        checkIsDeviceConnected()
                    },
                    checkUserRejectRequest = {
                        Log.i(TAG, "checkUserRejectRequest")
                        checkIsDeviceConnected()
                    },
                    deviceName = { name ->
                        deviceName.value = name
                    }
                )
            }
        }
    }

    private fun handleWifiStatus(status: Boolean) {
        if (status && !isDeviceConnected && !isDiscoveryStarted) {
            startDiscovery()
        } else if (!status) {
            isDeviceConnected = false
            stopDiscovery()
        }
    }

    fun registerReceiver() {
        context.registerReceiver(wifiP2pReceiver, intentFilter)
    }

    fun unregisterReceiver() {
        context.unregisterReceiver(wifiP2pReceiver)
    }

    @SuppressLint("MissingPermission")
    fun connectWithDevice(device: WifiP2pDevice, isConnectionStarted: (Boolean) -> Unit) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
            groupOwnerIntent = 0
        }

        wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Successfully connected to device")
                isConnectionStarted(true)
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Failed to connect to device: $reason")
                isConnectionStarted(false)
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun disconnect(isConnectionStopped: (Boolean) -> Unit) {
        wifiP2pManager?.requestGroupInfo(channel) { group ->
            if (group == null) {
                isConnectionStopped(false)
                return@requestGroupInfo
            }

            wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    WifiDirectUtils.deletePersistentGroups(wifiP2pManager, channel) {
                        isDeviceConnected = false
                        connectedDevice.value = null
                        Log.i(TAG, "Connection stopped successfully")
                        isConnectionStopped(true)
                    }
                }

                override fun onFailure(reason: Int) {
                    Log.e(TAG, "Failed to stop connection: $reason")
                    isConnectionStopped(false)
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    fun checkIsDeviceConnected() {
        WifiDirectUtils.isDeviceConnected(wifiP2pManager, channel) { connected ->
            isDeviceConnected = connected
            if (connected) {
                wifiP2pManager?.requestGroupInfo(channel) { group ->
                    stopDiscovery()
                    connectedDevice.value = group
                }
            } else {
                connectedDevice.value = null
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun recreateGroup() {
        WifiDirectUtils.deletePreviousDeviceGroup(wifiP2pManager, channel, lifecycleCoroutineScope) {
            wifiP2pManager?.createGroup(channel, null)
        }
    }
}*/


class WifiDirectManager(
    private val context: Context,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
    val isSender: Boolean = true
) {
    companion object {
        private var TAG = "WifiDirectManager"
        const val SENDER_TAG = "SENDER_MANAGER"
        const val RECEIVER_TAG = "RECEIVER_MANAGER"
    }

    // Initialize WifiP2pManager
    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel = wifiP2pManager?.initialize(context, context.mainLooper, null)

    // Intent filter for Wi-Fi P2P events
    private var intentFilter = IntentFilter()
    private var wifiP2pReceiver: WifiP2pReceiver? = null
    private var isReceiverRegistered = false

    // States
    var isDeviceConnected = false
    var isDiscoveryStarted = false

    // MutableStateFlows for observing state
    val peersList = MutableStateFlow<WifiP2pDeviceList?>(null)
    val isWifiOn = MutableStateFlow(true)
    val deviceName = MutableStateFlow("")
    val connectedDevice = MutableStateFlow<WifiP2pGroup?>(null)

    @SuppressLint("MissingPermission")
    fun init() {
        Log.v(SENDER_TAG, "<-- Start WifiDirectManager -->")
        Log.v(RECEIVER_TAG, "<-- Start WifiDirectManager -->")
        setupIntentFilter()
        setupBroadcastReceiver(this@WifiDirectManager)

//        WifiDirectUtils.deletePersistentGroups(wifiP2pManager, channel) { str ->
//            Log.d(SENDER_TAG, "Deleting persistent groups : $str")
//            Log.d(RECEIVER_TAG, "Deleting persistent groups : $str")
//        }

        WifiDirectUtils.deletePreviousDeviceGroup(
            wifiP2pManager,
            channel
        ) {

        }

//        WifiDirectUtils.deletePreviousDeviceGroup(
//            wifiP2pManager,
//            channel,
//            lifecycleCoroutineScope
//        ) {
//            if (isSender) {
//                Log.i(SENDER_TAG, "i am Sender")
//                Log.i(
//                    SENDER_TAG,
//                    "Only Remove Persistent Group not need to create group because want to set receiver is server and sender is client"
//                )
//                if (!isDeviceConnected) {
//                    startDiscovery()
//                }
//            } else {
//                Log.i(RECEIVER_TAG, "I am Receiver")
//                // 2. Create a new group, making this device the Group Owner
//                wifiP2pManager?.createGroup(channel, null)
//                Log.i(RECEIVER_TAG, "Device group recreated")
//            }
//        }

        if (isSender) {
            Log.d(SENDER_TAG, "i am Sender")
            if (!isDeviceConnected) {
                startDiscovery()
            }
        } else {
            Log.d(RECEIVER_TAG, "I am Receiver")
            recreateGroup()
        }
    }

    // TODO : BOTH SENDER & RECEIVER
    /** (1) Init Intent Filter */
    private fun setupIntentFilter() {
        Log.v(SENDER_TAG, "Setting up intent filters")
        Log.v(RECEIVER_TAG, "Setting up intent filters")
        intentFilter.apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
        }
    }

    // TODO : BOTH SENDER & RECEIVER
    /** (2) Init Init Broad cast Receiver */
    private fun setupBroadcastReceiver(wifiDi: WifiDirectManager) {
        Log.v(SENDER_TAG, "Setting up WifiP2pReceiver")
        Log.v(RECEIVER_TAG, "Setting up WifiP2pReceiver")
        wifiP2pReceiver = wifiP2pManager?.let { manager ->
            channel?.let { ch ->
                WifiP2pReceiver(
                    manager, ch, wifiDi, lifecycleCoroutineScope,
                    onPeersAvailable = { list ->
                        peersList.value = list
                        Log.i(SENDER_TAG, "Peers available: ${list.deviceList.size} devices found")
                        Log.i(SENDER_TAG, "Peers available: $list devices found ===============")
                        Log.i(
                            RECEIVER_TAG,
                            "Peers available: ${list.deviceList.size} devices found"
                        )
                        Log.i(RECEIVER_TAG, "Peers available: $list devices found ===============")
                    },
                    isWifiOn = { status ->
                        isWifiOn.value = status
                        handleWifiStatus(status)
                        Log.i(SENDER_TAG, "WiFi status changed: $status")
                        Log.i(RECEIVER_TAG, "WiFi status changed: $status")
                    },
                    onWifiP2pConnectionChanged = {
                        Log.i(SENDER_TAG, "WiFi P2P connection state changed")
                        Log.i(RECEIVER_TAG, "WiFi P2P connection state changed")
                        checkIsDeviceConnected()
                    },
                    checkUserRejectRequest = {
                        Log.i(SENDER_TAG, "User may have rejected the request")
                        Log.i(RECEIVER_TAG, "User may have rejected the request")
                        checkIsDeviceConnected()
                    },
                    deviceName = { name ->
                        deviceName.value = name
                        Log.i(SENDER_TAG, "Device name updated: $name")
                        Log.i(RECEIVER_TAG, "Device name updated: $name")
                    }
                )
            }
        }
    }

    // TODO : BOTH SENDER & RECEIVER
    fun registerReceiver() {
//        Log.v(SENDER_TAG, "Registering WifiP2pReceiver")
//        Log.v(RECEIVER_TAG, "Registering WifiP2pReceiver")
//        context.registerReceiver(wifiP2pReceiver, intentFilter)


        if (!isReceiverRegistered) {
            Log.v(SENDER_TAG, "Registering WifiP2pReceiver")
            Log.v(RECEIVER_TAG, "Registering WifiP2pReceiver")
            context.registerReceiver(wifiP2pReceiver, intentFilter)
            isReceiverRegistered = true
        } else {
            Log.v(SENDER_TAG, "WifiP2pReceiver already registered")
        }
    }

    // TODO : BOTH SENDER & RECEIVER
    fun unregisterReceiver() {
//        Log.v(SENDER_TAG, "Unregistering WifiP2pReceiver")
//        Log.v(RECEIVER_TAG, "Unregistering WifiP2pReceiver")
//        context.unregisterReceiver(wifiP2pReceiver)

        if (isReceiverRegistered && wifiP2pReceiver != null) {
            try {
                Log.v(SENDER_TAG, "Unregistering WifiP2pReceiver")
                Log.v(RECEIVER_TAG, "Unregistering WifiP2pReceiver")

                context.unregisterReceiver(wifiP2pReceiver)
            } catch (e: IllegalArgumentException) {
                Log.e(SENDER_TAG, "Receiver not registered or already unregistered: ${e.message}")
            } finally {
                isReceiverRegistered = false
                wifiP2pReceiver = null
            }
        } else {
            Log.v(SENDER_TAG, "WifiP2pReceiver not registered or already unregistered")
        }
    }

    /** (3 for sender) Start Discover */
    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        try {
            Log.v(SENDER_TAG, "Start Discovery")

            if (isDiscoveryStarted) {
                Log.i(SENDER_TAG, "Discovery is already started")
                return
            }

            wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Code for when the discovery initiation is successful goes here.
                    // No services have actually been discovered yet, so this method
                    // can often be left blank. Code for peer discovery goes in the
                    // onReceive method, detailed below.
                    Log.d(SENDER_TAG, "Discovery peer started successfully")
                    isDiscoveryStarted = true
                }

                override fun onFailure(reasonCode: Int) {
                    // Code for when the discovery initiation fails goes here.
                    // Alert the user that something went wrong.
                    Log.e(SENDER_TAG, "Discovery peer failed: $reasonCode")
                    handleDiscoveryFailure(reasonCode) // Call the defined method
                }
            })
        } catch (e: SecurityException) {
            Log.e(SENDER_TAG, "Security exception occurred: ${e.message}")
            handleException(e) // Call the defined method
        } catch (e: IllegalArgumentException) {
            Log.e(SENDER_TAG, "Invalid arguments provided: ${e.message}")
            handleException(e) // Call the defined method
        } catch (e: Exception) {
            Log.e(SENDER_TAG, "An unexpected error occurred during discovery: ${e.message}")
            handleException(e) // Call the defined method
        }
    }

    // TODO : FOR SENDER
    /** Show Discovery Failure Logs */
    private fun handleDiscoveryFailure(reasonCode: Int) {
        when (reasonCode) {
            WifiP2pManager.P2P_UNSUPPORTED -> {
                Log.e(SENDER_TAG, "Wi-Fi Direct is not supported on this device.")
            }

            WifiP2pManager.BUSY -> {
                Log.e(SENDER_TAG, "Wi-Fi Direct is currently busy.")
            }

            WifiP2pManager.ERROR -> {
                Log.e(SENDER_TAG, "An internal error occurred.")
                Toast.makeText(
                    context,
                    "Internal error occurred during discovery.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                Log.e(SENDER_TAG, "Unknown failure reason: $reasonCode")
                Toast.makeText(
                    context,
                    "Unknown error occurred during discovery.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // TODO : BOTH SENDER & RECEIVER
    /** Handles general exceptions and logs or notifies the user */
    private fun handleException(e: Exception) {
        Log.e(SENDER_TAG, "Exception: ${e.message}")
        Log.e(RECEIVER_TAG, "Exception: ${e.message}")
        Toast.makeText(context, "Exception : ${e.message}", Toast.LENGTH_SHORT).show()
    }

    // TODO : FOR SENDER
    /** Stop Discover */
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {

        Log.v(SENDER_TAG, "Attempting to stop peer discovery")
        if (!isDiscoveryStarted) {
            Log.i(SENDER_TAG, "Discovery is not started, so no need to stop")
            return
        }

        wifiP2pManager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isDiscoveryStarted = false
                Log.d(SENDER_TAG, "Discovery peer stopped successfully")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(SENDER_TAG, "Failed to stop peer discovery: $reasonCode")
            }
        })
    }

    // TODO : BOTH SENDER & RECEIVER
//    private fun handleWifiStatus(status: Boolean) {
//        Log.i(SENDER_TAG, "Handling WiFi status: $status")
//        Log.i(RECEIVER_TAG, "Handling WiFi status: $status")
//        if (status && !isDeviceConnected && !isDiscoveryStarted) {
//            Log.i(SENDER_TAG, "WiFi is on and not connected; starting discovery")
//            Log.i(RECEIVER_TAG, "WiFi is on and not connected; starting discovery")
//
//            startDiscovery()
//        } else if (!status) {
//            Log.i(SENDER_TAG, "WiFi is off; stopping discovery")
//            Log.i(RECEIVER_TAG, "WiFi is off; stopping discovery")
//            isDeviceConnected = false
//            stopDiscovery()
//        }
//    }

    private fun handleWifiStatus(status: Boolean) {
        Log.i(SENDER_TAG, "Handling WiFi status: $status")
        Log.i(RECEIVER_TAG, "Handling WiFi status: $status")

        if (status) {
            if (!isDeviceConnected && !isDiscoveryStarted) {
                if (isSender) {
                    Log.i(SENDER_TAG, "WiFi is on and not connected; starting discovery")
                    startDiscovery()
                } else {
                    Log.i(RECEIVER_TAG, "WiFi is on; setting up group as Receiver")
                    // recreateGroup()
                }
            }
        } else {
            Log.i(SENDER_TAG, "WiFi is off; stopping discovery or tearing down group")
            Log.i(RECEIVER_TAG, "WiFi is off; stopping discovery or tearing down group")
            isDeviceConnected = false
            stopDiscovery()
//            if (!isSender) {
//                // Receiver-specific teardown if needed
//                removeGroup() // if you have a function to clean up group
//            }
        }
    }


    // TODO : FOR SENDER
    @SuppressLint("MissingPermission")
    fun connectWithDevice(device: WifiP2pDevice, isConnectionStarted: (Boolean) -> Unit) {
        try {
            Log.v(SENDER_TAG, "Attempting to connect with device: ${device.deviceName}")

            // Request connection info to check the current P2P connection state
            wifiP2pManager?.requestConnectionInfo(channel) { info ->
                if (info != null && info.groupFormed) {
                    // Check if the connected group contains the target device
                    wifiP2pManager.requestGroupInfo(channel) { group ->
                        if (group != null && group.clientList.any { it.deviceAddress == device.deviceAddress }) {
                            // Target device is already part of the group
                            Log.d(SENDER_TAG, "Already connected to the target device.")
                            Toast.makeText(
                                context,
                                "Already connected to the same device via Wi-Fi Direct",
                                Toast.LENGTH_SHORT
                            ).show()
                            isConnectionStarted(false)
                            return@requestGroupInfo
                        }
                    }
                }

                // Proceed with connection if the device is not already connected
                val config = WifiP2pConfig().apply {
                    deviceAddress = device.deviceAddress
                    wps.setup = WpsInfo.PBC
                    groupOwnerIntent = 0
                }

                wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.i(SENDER_TAG, "Successfully connected to device: ${device.deviceName}")
                        isConnectionStarted(true)
                    }

                    override fun onFailure(reason: Int) {
                        Log.e(
                            SENDER_TAG,
                            "Failed to connect to device: ${device.deviceName}, reason: $reason"
                        )
                        Toast.makeText(context, "Connection failed: $reason", Toast.LENGTH_SHORT)
                            .show()
                        isConnectionStarted(false)
                    }
                })
            }
        } catch (e: SecurityException) {
            Log.e(SENDER_TAG, "Security exception occurred: ${e.message}")
            handleException(e)
        } catch (e: Exception) {
            Log.e(SENDER_TAG, "An error occurred while trying to connect: ${e.message}")
            handleException(e)
        }
    }

    // TODO : BOTH SENDER & RECEIVER
    @SuppressLint("MissingPermission")
    fun disconnect(isConnectionStopped: (Boolean) -> Unit) {
        try {
            Log.v(SENDER_TAG, "Attempting to disconnect from current device")
            Log.v(RECEIVER_TAG, "Attempting to disconnect from current device")
            wifiP2pManager?.requestGroupInfo(channel) { group ->
                if (group == null) {
                    Log.w(SENDER_TAG, "No group to disconnect from")
                    Log.w(RECEIVER_TAG, "No group to disconnect from")
                    isConnectionStopped(false)
                    return@requestGroupInfo
                }

                wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.w(SENDER_TAG, "Group removed successfully")
                        Log.w(RECEIVER_TAG, "Group removed successfully")
                        WifiDirectUtils.deletePersistentGroups(wifiP2pManager, channel) {
                            isDeviceConnected = false
                            connectedDevice.value = null
                            Log.w(SENDER_TAG, "Connection stopped and groups deleted")
                            Log.w(RECEIVER_TAG, "Connection stopped and groups deleted")
                            isConnectionStopped(true)
                        }
                    }

                    override fun onFailure(reason: Int) {
                        Log.e(SENDER_TAG, "Failed to stop connection: $reason")
                        Log.e(RECEIVER_TAG, "Failed to stop connection: $reason")
                        isConnectionStopped(false)
                    }
                })
            }
        } catch (e: SecurityException) {
            Log.e(SENDER_TAG, "Security exception during disconnect: ${e.message}")
            Log.e(RECEIVER_TAG, "Security exception during disconnect: ${e.message}")
            handleException(e)
        } catch (e: Exception) {
            Log.e(SENDER_TAG, "Error during disconnect: ${e.message}")
            Log.e(RECEIVER_TAG, "Error during disconnect: ${e.message}")
            handleException(e)
        }
    }

    // TODO : BOTH SENDER & RECEIVER
    /** Check Connection */
    @SuppressLint("MissingPermission")
    fun checkIsDeviceConnected() {
        try {
            Log.v(SENDER_TAG, "Checking if device is connected")
            Log.v(RECEIVER_TAG, "Checking if device is connected")

            WifiDirectUtils.isDeviceConnected(wifiP2pManager, channel) { connected ->
                isDeviceConnected = connected
                if (connected) {
                    try {
                        wifiP2pManager?.requestGroupInfo(channel) { group ->
                            stopDiscovery()
                            connectedDevice.value = group
                            Log.d(SENDER_TAG, "Device connected, group info: $group")
                            Log.d(RECEIVER_TAG, "Device connected, group info: $group")
                        }
                    } catch (e: Exception) {
                        Log.e(SENDER_TAG, "Error while requesting group info", e)
                        Log.e(RECEIVER_TAG, "Error while requesting group info", e)
                        connectedDevice.value = null
                    }
                } else {
                    connectedDevice.value = null
                    Log.w(SENDER_TAG, "Device is not connected")
                    Log.w(RECEIVER_TAG, "Device is not connected")
                }
            }
        } catch (e: Exception) {
            Log.e(SENDER_TAG, "Error during device connection check", e)
            Log.e(RECEIVER_TAG, "Error during device connection check", e)
            connectedDevice.value = null
        }
    }

    // TODO : FOR RECEIVER
    /** (3 for receiver) Recreate Group */
    @SuppressLint("MissingPermission")
//    fun recreateGroup() {
//        Log.d(RECEIVER_TAG, "Recreating Group Call")
//        Log.d("awais_shakeel", "Recreating Group Call")
//        // 1. Delete any existing group info (persistent group)
//        WifiDirectUtils.deletePreviousDeviceGroup(
//            wifiP2pManager,
//            channel,
//            {
//                // 2. Create a new group, making this device the Group Owner
//                //wifiP2pManager?.createGroup(channel, null)
//                Log.i(RECEIVER_TAG, "Device group recreated")
//                Log.i("awais_shakeel", "Device group recreated")
//                wifiP2pManager?.createGroup(channel, object : WifiP2pManager.ActionListener {
//                    override fun onSuccess() {
//                        Toast.makeText(
//                            context,
//                            "Device is ready to accept incoming connections from Sender.",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        Log.i(
//                            "awais_shakeel",
//                            "Device is ready to accept incoming connections from Sender."
//                        )
//
//                    }
//
//                    override fun onFailure(reason: Int) {
//                        Toast.makeText(
//                            context,
//                            "P2P group creation failed. Retry : $reason",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        Log.i("awais_shakeel", "P2P group creation failed. Retry : $reason")
//
//                    }
//                })
//            },
//            { reason ->
//                Log.e(RECEIVER_TAG, "Failed to delete group before recreating: $reason")
//                Log.e("awais_shakeel", "Failed to delete group before recreating: $reason")
//            }
//        )
//    }

    fun recreateGroup() {
        Log.d(RECEIVER_TAG, "Recreating Group Call")
        Log.d("awais_shakeel", "Recreating Group Call")
        // 1. Delete any existing group info (persistent group)
        WifiDirectUtils.deletePreviousDeviceGroup(
            wifiP2pManager,
            channel
        )
        {
            // 2. Create a new group, making this device the Group Owner
            //wifiP2pManager?.createGroup(channel, null)
            Log.i(RECEIVER_TAG, "Device group recreated")
            Log.i("awais_shakeel", "Device group recreated")
            wifiP2pManager?.createGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Toast.makeText(
                        context,
                        "Device is ready to accept incoming connections from Sender.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.i(
                        "awais_shakeel",
                        "Device is ready to accept incoming connections from Sender."
                    )
                }

                override fun onFailure(reason: Int) {
                    Toast.makeText(
                        context,
                        "P2P group creation failed. Retry : $reason",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.i("awais_shakeel", "P2P group creation failed. Retry : $reason")
                }
            })
        }
    }


//    fun recreateGroup(retryCount: Int = 3) {
//        Log.d(RECEIVER_TAG, "Recreating device group, retries left: $retryCount")
//
//        WifiDirectUtils.deletePreviousDeviceGroup(
//            wifiP2pManager,
//            channel,
//            {
//                Log.i(RECEIVER_TAG, "Attempted to recreate device group")
//                wifiP2pManager?.createGroup(channel, object : WifiP2pManager.ActionListener {
//                    override fun onSuccess() {
//                        Toast.makeText(
//                            context,
//                            "Device is ready to accept incoming connections from Sender.",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        Log.i(RECEIVER_TAG, "Device group created successfully")
//                    }
//
//                    override fun onFailure(reason: Int) {
//                        Toast.makeText(
//                            context,
//                            "P2P group creation failed. Reason: $reason",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        Log.w(RECEIVER_TAG, "Group creation failed with reason: $reason")
//
//                        if (reason == WifiP2pManager.BUSY && retryCount > 0) {
//                            Handler(Looper.getMainLooper()).postDelayed({
//                                recreateGroup(retryCount - 1)  // Retry recursively
//                            }, 2000)
//                        } else {
//                            Log.e(
//                                RECEIVER_TAG,
//                                "Group creation failed permanently or retries exhausted"
//                            )
//                        }
//                    }
//                })
//            },
//            { reason ->
//                Log.e(RECEIVER_TAG, "Failed to delete group before recreating: $reason")
//            }
//        )
//    }

}
