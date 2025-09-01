package com.smartswitch.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdView
import com.google.android.material.tabs.TabLayout
import com.smartswitch.R
import com.smartswitch.databinding.ActivityConnectionBinding
import com.smartswitch.presentation.PermissionFragment
import com.smartswitch.presentation.language.BaseActivity
import com.smartswitch.utils.Constant.isGPSEnabled
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.WifiDirectFragment
import com.smartswitch.utils.WifiGpsStatusReceiver


@Suppress("DEPRECATION")
class ConnectionActivity : AppCompatActivity(), WifiGpsStatusReceiver.StatusChangeListener {
    private lateinit var binding: ActivityConnectionBinding
    private lateinit var user: String
    private lateinit var locationManager: LocationManager
    private lateinit var wifiManager: WifiManager
    private var permissionFragment: PermissionFragment? = null
    private lateinit var wifiDirectFragment: WifiDirectFragment
    var adView: AdView? = null

    private lateinit var wifiGpsStatusReceiver: WifiGpsStatusReceiver
    private lateinit var intentFilter: IntentFilter

    //    private val binding: ActivityConnectionBinding by lazy {
//        ActivityConnectionBinding.inflate(
//            layoutInflater
//        )
//    }
    var currentPosition = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as (LocationManager)
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        intentFilter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        }
        wifiGpsStatusReceiver = WifiGpsStatusReceiver(this)

        user = intent.getStringExtra("user").toString()
        sender_user = intent.getStringExtra("sender_user").toString()
        receiver_user = intent.getStringExtra("receiver_user").toString()

        Log.e("TESTTAG", "ACTIVITY_SELECTION_CONNECTION user: $user") // sender
        Log.e(
            "TESTTAG", "ACTIVITY_SELECTION_CONNECTION sender_user: $sender_user"
        ) // local_transfer_sender ,,  phone_clone_sender
        Log.e(
            "TESTTAG", "ACTIVITY_SELECTION_CONNECTION receiver_user: $receiver_user"
        ) // local_transfer_receive ,, phone_clone_receiver

        /* if (receiver_user.equals("local_transfer_receive") || sender_user.equals("local_transfer_sender")) {
             binding.textConnectionDesc.text = getString(R.string.wifi_desc)
         } else {
             binding.textConnectionDesc.text =
                 getString(R.string.share_file_using_wifi_and_direct)
         }*/



        if (user == "sender") {
            binding.title.text = getString(R.string.receiver)
        } else {
            binding.title.text = getString(R.string.receiver)
        }

        if (PermissionManager.hasLocationPermission(this)) {
            permissionFragment = PermissionFragment.newInstance(user) {
                checkAndUpdateFragment()
            }
//            showBanner()
        } else {
            binding.frameConnection.visibility = View.GONE
            binding.layoutBeforePermission.visibility = View.VISIBLE
        }

//        binding.tabLayout.tabRippleColor = null
        wifiDirectFragment = WifiDirectFragment.newInstance(user)


//        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab) {
//
//                when (tab.position) {
//                    0 -> {
////                        binding.textConnectionDesc.text =
////                            getString(R.string.share_file_using_wifi_and_direct)
//                        binding.title
//                        currentPosition = 0
//                        checkAndUpdateFragment()
//                        return
//                    }
//
//                    1 -> {
//                        //                        binding.textConnectionDesc.text = getString(R.string.share_file_using_wifi_and_direct)
//                        currentPosition = 1
//                        checkAndUpdateFragment()
//                        return
//                    }
//
//                    else -> {
////                        binding.textConnectionDesc.text = getString(R.string.share_file_using_wifi_and_direct)
//                        binding.title
//                        currentPosition = 0
//                        checkAndUpdateFragment()
//                        return
//                    }
//                }
//            }
//
//            override fun onTabUnselected(
//                tab: TabLayout.Tab?
//            ) {
//            }
//
//            override fun onTabReselected(
//                tab: TabLayout.Tab?
//            ) {
//            }
//        })

//        wifiDirect.setOnClickListener {
//            if (!wifiManager.isWifiEnabled) {
//                AppConstants.presentToast(this, "Please turn on wifi")
//                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
//            } else {
//                if (!isNetworkEnabled(locationManager) && !isGPSEnabled(locationManager)) {
//                    AppConstants.presentToast(this, "Please turn on location")
//                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
//                } else {
//                    startActivity(
//                        Intent(this, ExploreDeviceActivity::class.java)
//                            .putExtra("user", user)
//                    )
//                }
//            }
//        }
//        hotSpot.setOnClickListener {
//            if (user == "sender") {
//                if (!isNetworkEnabled(locationManager) && !isGPSEnabled(locationManager)) {
//                    AppConstants.presentToast(this, "Please turn on location")
//                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
//                } else {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(
//                            this
//                        )
//                    ) {
//                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
//                        intent.data = Uri.parse("package:$packageName")
//                        startActivity(intent)
//                    } else {
//                        startActivity(Intent(this, MakeHotSpotActivity::class.java))
//                    }
//                }
//
//            } else {
//                if (!wifiManager.isWifiEnabled) {
//                    AppConstants.presentToast(this, "Please turn on wifi")
//                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
//                } else {
//                    startActivity(Intent(this, JoinHotspotActivity::class.java))
//                }
//            }
//        }
        binding.imgBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndUpdateFragment()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiGpsStatusReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(wifiGpsStatusReceiver, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
//        unregisterReceiver(wifiGpsStatusReceiver)
        try {
            if (this::wifiGpsStatusReceiver.isInitialized) {
                unregisterReceiver(wifiGpsStatusReceiver)
            }
        } catch (_: IllegalArgumentException) {

        }
    }

    private fun checkAndUpdateFragment() {
        if (!wifiManager.isWifiEnabled || !isGPSEnabled(
                locationManager
            )
        ) {
            permissionFragment?.let { updateFragment(it) }

        } else if (wifiManager.isWifiEnabled && isGPSEnabled(
                locationManager
            )
        ) {
            setUpConnection()

        }
    }

    private fun setUpConnection() {
        if (sender_user == "phone_clone_sender" || sender_user == "local_transfer_sender") {
            if (currentPosition == 0) {
                updateFragment(wifiDirectFragment)
//                wifiDirectFragment.hideQrLayout()
            } else {
//                wifiDirectFragment.showQrLayout()
//                updateFragment(qrcodeFragment)
            }
        } else if (receiver_user == "phone_clone_receiver" || receiver_user == "local_transfer_receive") {
            if (currentPosition == 0) {
                updateFragment(wifiDirectFragment)
            } else {
//                updateFragment(joinHotspotFragment)
            }
        }
    }

    //    else {
//        if (currentPosition == 0) {
//            updateFragment(wifiDirectFragment)
//            wifiDirectFragment.hideQrLayout()
//        } else {
//            wifiDirectFragment.showQrLayout()
////                updateFragment(qrcodeFragment)
//        }
//    }
//    private fun requestLocationPermission() {
//        val permissions: Array<String>
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            permissions = arrayOf(
//                Manifest.permission.NEARBY_WIFI_DEVICES,
//                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            )
//        } else {
//            permissions = arrayOf(
//                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
//            )
//        }
//        com.nabinbhandari.android.permissions.Permissions.check(this,
//            permissions,
//            null,
//            null,
//            object : PermissionHandler() {
//                override fun onGranted() {
//                    binding.layoutBeforePermission.visibility = View.GONE
//                    binding.frameConnection.visibility = View.VISIBLE
//                    binding.constraintLayout6.visibility = View.VISIBLE
//                    permissionFragment = PermissionFragment.newInstance(user) {
//                        checkAndUpdateFragment()
//                    }
////                    showBanner()
//                }
//
//                @SuppressLint("LongLogTag")
//                override fun onDenied(
//                    context: Context?, deniedPermissions: ArrayList<String>?
//                ) {
//                    deniedPermissions?.forEach {
//                        Log.e("TESTTAG deniedPermissions", it)
//                    }
//                    AppConstants.showSnackBar(
//                        this@ConnectionActivity,
//                        "Location Permissions required!",
//                        R.drawable.info
//                    )
//                }
//            })
//    }

    /*
        fun showBanner() {
            Log.e(TAG, "activity selection connection showBanner: ")
            if (!isEnabled || !bannerEnabled)
                binding.bannerSelectConnection.visibility = View.GONE
            return
            if (!banner_connection_Collapsible) {
                AdsManager.loadAdaptorBanner(
                    binding.bannerSelectConnection,
                    this@ConnectionActivity,
                    object : AdsManager.AdmobBannerAdListener {
                        override fun onAdFailed() {
                            Log.e(TAG, "onAdFailed: Banner")
                        }

                        override fun onAdLoaded() {
                            Log.e(TAG, "onAdLoaded: Banner")
                        }
                    },
                    AdIds.getSelectConnectionBannerId()
                )?.let {
                    adView = it
                }
            } else {
                AdsManager.loadCollapsibleBannerTop(
                    binding.bannerSelectConnection,
                    this@ConnectionActivity,
                    object : AdsManager.AdmobBannerAdListener {
                        override fun onAdFailed() {
                            Log.e(TAG, "onAdFailed: Banner")
                        }

                        override fun onAdLoaded() {
                            Log.e(TAG, "onAdLoaded: Banner")
                        }
                    },
                    AdIds.getSelectConnectionBannerId()
                )?.let {
                    adView = it
                }
            }
        }
    */

    @SuppressLint("CommitTransaction")
    fun updateFragment(fragment: Fragment) {
        if (!isFinishing && !supportFragmentManager.isStateSaved) {
            this.supportFragmentManager.beginTransaction()
                .replace(binding.frameConnection.id, fragment).commit()
        }
    }

    override fun onStatusChanged(isWifiEnabled: Boolean, isGpsEnabled: Boolean) {
        if (!isWifiEnabled || !isGpsEnabled) {
            permissionFragment?.let { updateFragment(it) }
        }
    }

    companion object {
        lateinit var sender_user: String
        lateinit var receiver_user: String
    }
}