package com.smartswitch.presentation

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.smartswitch.databinding.FragmentPermissionBinding
import com.smartswitch.utils.Constant.isGPSEnabled
import com.smartswitch.utils.Constant.isNetworkEnabled
import com.smartswitch.utils.Constant.showSnackBar

private const val KEY_USER_TYPE = "key_user_types"
private const val ARG_PARAM2 = "param2"

class PermissionFragment : Fragment() {
    lateinit var attachedContext: Context

    private var userType: String = ""
    private var systemSettingPermissionDialog: Dialog? = null
    var binding: FragmentPermissionBinding? = null
    private lateinit var wifiManager: WifiManager
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userType = it.getString(KEY_USER_TYPE).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPermissionBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(
//                requireContext()
//            )
//        ) {
//            showSystemSettingPermissionDialog()
//        } else {
//            if (systemSettingPermissionDialog!!.isShowing) {
//                systemSettingPermissionDialog?.dismiss()
//            }
//        }
//        binding?.let { it1 ->
//            Glide.with(requireActivity()).asGif().load(R.drawable.resource_new).into(
//                it1.system
//            )
//        }
        attachedContext?.let {

            locationManager = it.getSystemService(Context.LOCATION_SERVICE) as (LocationManager)
            wifiManager = it.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager


            activity?.runOnUiThread {
                if (!wifiManager.isWifiEnabled) {
                    binding?.switchWifi?.isChecked = false
                } else {
                    binding?.switchWifi?.isChecked = true
//                    binding?.switchNetwork?.isChecked = isNetworkEnabled(locationManager)
                    binding?.switchWifi?.isChecked = isNetworkEnabled(locationManager)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (!locationManager.isLocationEnabled) {
                        binding?.switchGps?.isChecked = false
                    } else {
                        binding?.switchGps?.isChecked = true
                    }

                } else {
                    val isGpsEnabled =
                        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val isNetworkEnabled =
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    if (!isGpsEnabled && !isNetworkEnabled) {
                        binding?.switchGps?.isChecked = false
                    } else {
                        binding?.switchGps?.isChecked = true
                    }
                }


//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                    !locationManager.isLocationEnabled
//                } else {
//                    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
//                    val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
//
//                    if (!isGpsEnabled && !isNetworkEnabled) {
//                        binding?.switchGps?.isChecked = false
//                    } else {
//                        binding?.switchGps?.isChecked = true
//                    }
//                }


//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                        !locationManager.isLocationEnabled
//                    binding?.switchGps?.isChecked = false
//                } else {
//                    binding?.switchGps?.isChecked = true
//                    binding?.switchGps?.isChecked = isGPSEnabled(locationManager)
//                }
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                binding?.layoutSettings?.visibility = View.GONE
            }

            binding?.switchSettings?.isChecked =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(
                    attachedContext
                ))

            if (wifiManager.isWifiEnabled) {
                binding?.switchWifi?.isChecked = wifiManager.isWifiEnabled
            }

            binding?.switchGps?.setOnClickListener {
                try {
                    if (!isGPSEnabled(locationManager)) startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    else {
                        activity?.runOnUiThread {
//                            if (locationManager.isLocationEnabled) {
//                                binding?.switchGps?.isChecked = true
//                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                if (!locationManager.isLocationEnabled) {
                                    binding?.switchGps?.isChecked = false
                                } else {
                                    binding?.switchGps?.isChecked = true
                                }

                            } else {
                                val isGpsEnabled =
                                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                                val isNetworkEnabled =
                                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                                if (!isGpsEnabled && !isNetworkEnabled) {
                                    binding?.switchGps?.isChecked = false
                                } else {
                                    binding?.switchGps?.isChecked = true
                                }
                            }
                        }
                        showSnackBar(requireActivity(), "GPS is already enabled")

                    }
                } catch (e: Exception) {
                }
            }
            binding?.switchSettings?.setOnClickListener {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(
                            attachedContext
                        )
                    ) {
//                        showSystemSettingPermissionDialog()
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:${attachedContext?.packageName}")
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                }
            }
            binding?.switchWifi?.setOnClickListener {
                try {
                    if (!wifiManager.isWifiEnabled) {
                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    } else {
                        activity?.runOnUiThread {
                            showSnackBar(requireActivity(), "WIFI is already enabled")
                            if (wifiManager.isWifiEnabled) {
                                binding?.switchWifi?.isChecked = true
                            }
                        }
                    }
                } catch (e: Exception) {
                }
            }
            binding?.switchNetwork?.setOnClickListener {
                try {
                    if (!isNetworkEnabled(locationManager)) {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                } catch (e: Exception) {
                }
            }

            registerReceivers()
        }


        val networkCallback: ConnectivityManager.NetworkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    activity?.runOnUiThread {
                        if (wifiManager.isWifiEnabled) {
                            binding?.switchWifi?.isChecked = true
                        }
                    }
                }

                override fun onLost(network: Network) {
                    activity?.runOnUiThread {
                        binding?.switchWifi?.isChecked = false
                    }
                }
            }

//        val connectivityManager =
//            TedPermissionProvider.attachedContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val connectivityManager =
            attachedContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val request: NetworkRequest =
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }

    }

    private val gpsSwitchStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(attachedContext: Context, intent: Intent) {
            if (LocationManager.PROVIDERS_CHANGED_ACTION == intent.action) {
                val locationManager =
                    attachedContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled =
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                binding?.switchGps?.isChecked = isGpsEnabled || isNetworkEnabled
            }
        }
    }


    fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            attachedContext?.registerReceiver(
                gpsAndWifiStateReceiver,
                filter,
                Context.RECEIVER_EXPORTED
            )
        } else {
            attachedContext?.registerReceiver(gpsAndWifiStateReceiver, filter)
        }
    }

    private val gpsAndWifiStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateSwitches()

            // If both Wi-Fi and GPS are enabled, proceed to the next step
            if (isGPSEnabled(locationManager) && isWifiConnected()) {
                callback?.invoke()
            }
        }
    }

    private fun updateSwitches() {
        binding?.switchGps?.isChecked = isGPSEnabled(locationManager)
        binding?.switchWifi?.isChecked = isWifiConnected()
    }

    private fun isWifiConnected(): Boolean {
        val connectivityManager =
            attachedContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            Log.e("isWifiConnected", "ConnectivityManager is null")
            return false
        }
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
    }

    override fun onResume() {
        updateSwitches()
        super.onResume()
        attachedContext?.let {
            setupUI(it)
            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            filter.addAction(Intent.ACTION_PROVIDER_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity?.registerReceiver(
                    gpsSwitchStateReceiver,
                    filter,
                    Context.RECEIVER_EXPORTED
                )
            }
            activity?.runOnUiThread {
                if (!wifiManager.isWifiEnabled) {
                    binding?.switchWifi?.isChecked = false
                } else {
                    binding?.switchWifi?.isChecked = true
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (!locationManager.isLocationEnabled) {
                        binding?.switchGps?.isChecked = false
                    } else {
                        binding?.switchGps?.isChecked = true
                    }
                } else {
                    val isGpsEnabled =
                        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val isNetworkEnabled =
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    if (!isGpsEnabled && !isNetworkEnabled) {
                        binding?.switchGps?.isChecked = false
                    } else {
                        binding?.switchGps?.isChecked = true
                    }
                }

            }
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            activity?.unregisterReceiver(gpsSwitchStateReceiver)
        } catch (e: Exception) {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            activity?.unregisterReceiver(gpsSwitchStateReceiver)
        } catch (e: Exception) {

        }
    }

    private fun setupUI(it: Context) {
        locationManager = it.getSystemService(Context.LOCATION_SERVICE) as (LocationManager)
        wifiManager =
            it.applicationContext.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager

        binding?.switchGps?.isChecked = isGPSEnabled(locationManager)
        binding?.switchNetwork?.isChecked = isNetworkEnabled(locationManager)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {

            binding?.layoutSettings?.visibility = View.GONE
        }

        binding?.switchSettings?.isChecked =
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(
                attachedContext
            ))

        binding?.switchWifi?.isChecked = wifiManager.isWifiEnabled
//                && isNetworkEnabled(
//            locationManager
//        )
        if (wifiManager.isWifiEnabled && isGPSEnabled(locationManager)) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                callback?.let { it1 -> it1() }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(
                    attachedContext
                )
            ) {
//                showSystemSettingPermissionDialog()
                if (isAdded) {
                    callback?.let { it1 -> it1() }
                } else {
                    Log.e("PermissionFragment", "Callback has not been initialized")
                }
//                callback?.let { it1 -> it1() }
            }

        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        attachedContext = context
    }

    companion object {
        var callback: (() -> Unit)? = null

        @JvmStatic
        fun newInstance(param1: String, mCallback: () -> Unit) = PermissionFragment().apply {
            callback = mCallback
            arguments = Bundle().apply {
                putString(KEY_USER_TYPE, param1)
            }
        }
    }

}