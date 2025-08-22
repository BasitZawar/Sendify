package com.smartswitch.utils.wifiutils

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.smartswitch.utils.WifiDirectManager.Companion.RECEIVER_TAG
import com.smartswitch.utils.WifiDirectManager.Companion.SENDER_TAG
import java.net.InetAddress
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object WifiDirectUtils {

    fun deletePreviousDeviceGroup(
        manager: WifiP2pManager?,
        channel: WifiP2pManager.Channel?,
        allDeleted: (() -> Unit)? = null,
        onFailed: ((reason: Int) -> Unit)? = null,
    ) {
        try {
            deletePersistentGroups(manager, channel){ str ->
                Log.d(SENDER_TAG, "Deleting persistent groups (RECEIVER) : $str")
                Log.d(RECEIVER_TAG, "Deleting persistent groups (RECEIVER) : $str")
                Log.d("awais_shakeel", "Deleting persistent groups (RECEIVER) : $str")
            }
            removeGroupWithRetry(manager, channel, allDeleted = allDeleted, onFailed = onFailed)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


//    @SuppressLint("MissingPermission")
//    private fun removeGroupWithRetry(
//        wifiP2pManager: WifiP2pManager?,
//        channel: WifiP2pManager.Channel?,
//        maxRetries: Int = 3,
//        delayMs: Long = 500,
//        allDeleted: (() -> Unit)? = null,
//        onFailed: ((reason: Int) -> Unit)? = null
//    ) {
//        if (wifiP2pManager == null || channel == null) {
//            Log.w("awais_shakeel", "wifiP2pManager or channel is null, skipping removal")
//            allDeleted?.invoke()
//            return
//        }
//
//        // First check if a group exists
//        wifiP2pManager.requestGroupInfo(channel) { group ->
//            if (group == null) {
//                // No group exists, nothing to remove
//                Log.d("awais_shakeel", "No group exists, skipping removeGroup()")
//                allDeleted?.invoke()
//                return@requestGroupInfo
//            }
//
//            var retryCount = 0
//
//            fun tryRemoveGroup() {
//                wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
//                    override fun onSuccess() {
//                        Log.d("awais_shakeel", "Group removed successfully (RECEIVER)")
//                        allDeleted?.invoke()
//                    }
//
//                    override fun onFailure(reason: Int) {
//                        if (reason == WifiP2pManager.BUSY && retryCount < maxRetries) {
//                            retryCount++
//                            Log.v("awais_shakeel", "Retrying to remove group... Attempt (RECEIVER) : $retryCount")
//                            Handler(Looper.getMainLooper()).postDelayed({
//                                tryRemoveGroup()
//                            }, delayMs)
//                        } else {
//                            Log.w("awais_shakeel", "Failed to remove group after $retryCount retries, reason (RECEIVER) : $reason")
//                            onFailed?.invoke(reason)
//                        }
//                    }
//                })
//            }
//
//            tryRemoveGroup()
//        }
//    }


    private fun removeGroupWithRetry(
        wifiP2pManager: WifiP2pManager?,
        channel: WifiP2pManager.Channel?,
        maxRetries: Int = 3,
        delayMs: Long = 500,
        allDeleted: (() -> Unit)? = null,
        onFailed: ((reason: Int) -> Unit)? = null
    ) {
        var retryCount = 0

        fun tryRemoveGroup() {
            wifiP2pManager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(SENDER_TAG, "Group removed successfully (RECEIVER)")
                    Log.d(RECEIVER_TAG, "Group removed successfully (RECEIVER)")
                    Log.d("awais_shakeel", "Group removed successfully (RECEIVER)")
                   // allDeleted?.invoke()
                    onFailed?.invoke(1)
                }

                override fun onFailure(reason: Int) {
                    if (reason == WifiP2pManager.BUSY && retryCount < maxRetries) {
                        retryCount++
                        Log.v(SENDER_TAG, "Retrying to remove group... Attempt (RECEIVER) : $retryCount")
                        Log.v(RECEIVER_TAG, "Retrying to remove group... Attempt (RECEIVER) : $retryCount")
                        Log.v("awais_shakeel", "Retrying to remove group... Attempt (RECEIVER) : $retryCount")
                        Handler(Looper.getMainLooper()).postDelayed({
                            tryRemoveGroup()
                        }, delayMs)
                    } else {
                        Log.w(SENDER_TAG, "Failed to remove group after $retryCount retries, reason (RECEIVER) : $reason")
                        Log.w(RECEIVER_TAG, "Failed to remove group after $retryCount retries, reason (RECEIVER) : $reason")
                        Log.w("awais_shakeel", "Failed to remove group after $retryCount retries, reason (RECEIVER) : $reason")
                        onFailed?.invoke(reason)
                    }
                }
            })
        }
        tryRemoveGroup()
    }


    /**
     * Backward Compatibility: Older versions of Android or certain devices may use deletePreviousDeviceGroup,
     * while newer versions or other devices may use deletePersistentGroup.
     * Both methods achieve the same goal: deleting persistent Wi-Fi Direct groups
     *
     * ✨ Example Scenario:
     * * Before Deletion: * *
     * Device A and Device B are connected via Wi-Fi Direct.
     * They have a persistent group saved, so they can automatically reconnect when within range.
     *
     * * After Calling deletePersistentGroups: * *
     * Device A and Device B are still connected as before.
     * However, the next time they are within range, Device A will not automatically reconnect to Device B because the persistent group was deleted.
     * Device A needs to manually reconnect (either through the UI or through programmatic logic like connect()).
     *
     * * Conclusion: * *
     * No immediate disconnection: The devices will stay connected even after deleting the persistent group.
     * Future reconnection will require manual initiation: After deletion, the device won’t automatically reconnect. A new connection will need to be initiated manually.
     *
     * * Note: * *
     * If you want to disconnect the devices as well when deleting the persistent groups, you can call the removeGroup() method of WifiP2pManager to force the disconnection before deleting the groups.
    * */
    fun deletePersistentGroups(
        manager: WifiP2pManager?,
        channel: WifiP2pManager.Channel?,
        allDeleted: (String) -> Unit,
    ) {
        // TODO : Improve This Function in Future

        val m1 = WifiP2pManager::class.java.methods
        try {
            for (i in m1.indices) {
                if (m1[i].name == "deletePreviousDeviceGroup") {
                    // Delete any persistent group
                    for (netId in 0..31) {
                        m1[i].invoke(manager, channel, netId, null)
                    }
                }
            }
            //allDeleted()
            allDeleted("deletePreviousDeviceGroup") // TODO : My Changing
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val m2 = WifiP2pManager::class.java.methods
            for (i in m2.indices) {
                if (m2[i].name == "deletePersistentGroup") {
                    // Delete any persistent group
                    for (netId in 0..31) {
                        m2[i].invoke(manager, channel, netId, null)
                    }
                }
            }
            allDeleted("deletePersistentGroup") // TODO : My Changing
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun deletePersistentGroup(manager: WifiP2pManager?, channel: WifiP2pManager.Channel?) {
        try {
            val methods = WifiP2pManager::class.java.methods
            for (i in methods.indices) {
                if (methods[i].name == "deletePersistentGroup") {
                    // Delete any persistent group
                    for (netId in 0..31) {
                        methods[i].invoke(manager, channel, netId, null)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun isWifiSupported(ctx: Context): Boolean {
        val pm = ctx.packageManager
        val features = pm.systemAvailableFeatures
        for (info in features) {
            if (info?.name != null && info.name.equals(
                    "android.hardware.wifi.direct",
                    ignoreCase = true
                )
            ) {
                return true
            }
        }
        return false
    }

//    @SuppressLint("MissingPermission")
//    fun isDeviceConnected(
//        wifiP2pManager: WifiP2pManager?,
//        channel: WifiP2pManager.Channel?,
//        callback: (Boolean) -> Unit
//    ) {
//        wifiP2pManager?.requestGroupInfo(channel) { connectionInfo ->
//            callback(connectionInfo != null && ((connectionInfo.isGroupOwner && !connectionInfo.clientList.isNullOrEmpty()) || !connectionInfo.isGroupOwner))
//        }
//    }

    // TODO : BOTH SENDER & RECEIVER
    @SuppressLint("MissingPermission")
    fun isDeviceConnected(
        wifiP2pManager: WifiP2pManager?,
        channel: WifiP2pManager.Channel?,
        timeoutMs: Long = 10000, // 10 seconds
        callback: (Boolean) -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        var isTimeout = false

        val timeoutRunnable = Runnable {
            isTimeout = true
            Log.w(SENDER_TAG, "Connection check timed out after $timeoutMs ms")
            Log.w(RECEIVER_TAG, "Connection check timed out after $timeoutMs ms")
            callback(false)
        }

        handler.postDelayed(timeoutRunnable, timeoutMs)

        wifiP2pManager?.requestGroupInfo(channel) { connectionInfo ->
            if (!isTimeout) {
                handler.removeCallbacks(timeoutRunnable)
                if (connectionInfo == null) {
                    Log.d(SENDER_TAG, "No group info awailable. Device is not connected.")
                    Log.d(RECEIVER_TAG, "No group info available. Device is not connected.")
                    callback(false)
                } else {
                    val isConnected = (connectionInfo.isGroupOwner && !connectionInfo.clientList.isNullOrEmpty()) || !connectionInfo.isGroupOwner
                    Log.d(SENDER_TAG, "Group info available. Is connected: $isConnected")
                    Log.d(RECEIVER_TAG, "Group info available. Is connected: $isConnected")
                    Log.d(SENDER_TAG, "Is Group Owner: ${connectionInfo.isGroupOwner}, Clients: ${connectionInfo.clientList}")
                    Log.d(RECEIVER_TAG, "Is Group Owner: ${connectionInfo.isGroupOwner}, Clients: ${connectionInfo.clientList}")
                    callback(isConnected)
                }
            } else {
                Log.w(SENDER_TAG, "Timeout already occurred. Ignoring group info callback.")
                Log.w(RECEIVER_TAG, "Timeout already occurred. Ignoring group info callback.")
            }
        }
    }

    suspend fun getHostIpAddress(
        wifiP2pManager: WifiP2pManager?,
        channel: WifiP2pManager.Channel?,
    ): InetAddress? {
        return suspendCoroutine { continuation ->
            wifiP2pManager?.requestConnectionInfo(channel) { info ->
                val hostAddress = info.groupOwnerAddress
                continuation.resume(hostAddress)
            }
        }
    }

}