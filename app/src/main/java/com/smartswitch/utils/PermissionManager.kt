package com.smartswitch.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

object PermissionManager {


    fun hasContactPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNearbyPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }

    fun hasAllFilesAccessPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun hasReadWritePermission(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        } else {
            return false
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun hasNotificationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationAndNearbyPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasLocationPermission(context) && hasNearbyPermission(context)
        } else {
            return hasLocationPermission(context)
        }
    }

    fun checkSelfPermissionForStorage(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasStorageAccessPermission(activity: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasReadWritePermission(activity)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun requestAllFilePermissions(
        context: Context, manageStorageLauncher: ActivityResultLauncher<Intent>
    ) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory("android.intent.category.DEFAULT")

            intent.data = Uri.parse(
                String.format(
                    "package:%s", context.packageName
                )
            )
            manageStorageLauncher.launch(intent)
        } catch (e: java.lang.Exception) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            manageStorageLauncher.launch(intent)
        }

    }

    fun checkForPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun promptUserToEnableGps(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        launcher.launch(intent)
    }

    fun checkPermissionForWifiReceive(context: Context): Boolean {
        val fineLocationPermission = checkForPermission(
            context = context, permission = Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = checkForPermission(
            context = context, permission = Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val nearbyWifiPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkForPermission(
                context = context, permission = Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            true
        }
        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkForPermission(
                context = context, permission = Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            true
        }
        return fineLocationPermission && coarseLocationPermission && nearbyWifiPermission && notificationPermission
    }

    fun checkPermissionForSender(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manageFilePermission = hasAllFilesAccessPermission()
            val contactsPermission =
                checkForPermission(context = context, Manifest.permission.READ_CONTACTS)
            val notificationPermission =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    checkForPermission(
                        context = context, permission = Manifest.permission.POST_NOTIFICATIONS
                    )
                } else {
                    true
                }
            return manageFilePermission && contactsPermission && notificationPermission
        } else {
            val hasReadWritePermission = hasReadWritePermission(context)
            val contactsPermission =
                checkForPermission(context = context, Manifest.permission.READ_CONTACTS)
            return hasReadWritePermission && contactsPermission
        }
    }

    fun openAppSettings(context: Context?, launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri: Uri = Uri.fromParts("package", context?.packageName, null)
        intent.data = uri
        launcher.launch(intent)
    }

    fun openWifiSettings(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        launcher.launch(intent)
    }

    fun isWifiEnable(context: Context): Boolean {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }




}
