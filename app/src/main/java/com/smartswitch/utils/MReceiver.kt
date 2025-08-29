package com.smartswitch.utils

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import androidx.core.app.ActivityCompat

class MReceiverFragment(
    private val context: Context,
    private val wifiP2pManager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val fragment: WifiDirectFragment
) : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {

        val action = p1!!.action
        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
            if (ActivityCompat.checkSelfPermission(
                    p0!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            wifiP2pManager.requestPeers(channel, fragment.peerList)
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            val networkInfo: NetworkInfo =
                p1.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)!!
            if (networkInfo.isConnected) {
                wifiP2pManager.requestConnectionInfo(
                    channel,
                    fragment.connectionListener
                )
            }
        }
    }
}

class MReceiverNewFragment(
    private val context: Context,
    private val wifiP2pManager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val peerList: WifiP2pManager.PeerListListener,
    private val connectionListener: WifiP2pManager.ConnectionInfoListener
) : BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {

        val action = p1!!.action
        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            wifiP2pManager.requestPeers(channel, peerList)
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            val networkInfo: NetworkInfo =
                p1.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)!!
            if (networkInfo.isConnected) {
                wifiP2pManager.requestConnectionInfo(
                    channel, connectionListener
                )
            }
        }
    }
}