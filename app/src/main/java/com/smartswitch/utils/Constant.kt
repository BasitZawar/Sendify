package com.smartswitch.utils

import android.location.LocationManager

object Constant {
    const val PORT = 8080

    fun isGPSEnabled(locationManager: LocationManager): Boolean {
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
        }
        return false
    }
    fun isNetworkEnabled(locationManager: LocationManager): Boolean {
        try {
            return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
        }
        return false
    }
}