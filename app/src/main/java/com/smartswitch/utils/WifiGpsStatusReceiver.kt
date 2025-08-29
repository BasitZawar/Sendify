package com.smartswitch.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager

class WifiGpsStatusReceiver(private val listener: StatusChangeListener) : BroadcastReceiver() {

    interface StatusChangeListener {
        fun onStatusChanged(isWifiEnabled: Boolean, isGpsEnabled: Boolean)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val isWifiEnabled = wifiManager.isWifiEnabled

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGpsEnabled =
            locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)

        listener.onStatusChanged(isWifiEnabled, isGpsEnabled)
    }
}
