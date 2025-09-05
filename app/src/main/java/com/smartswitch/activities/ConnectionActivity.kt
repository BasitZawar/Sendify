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
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.smartswitch.R
import com.smartswitch.databinding.ActivityConnectionBinding
import com.smartswitch.presentation.PermissionFragment
import com.smartswitch.presentation.language.BaseActivity
import com.smartswitch.utils.Constant.isGPSEnabled
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.WifiDirectFragment
import com.smartswitch.utils.WifiGpsStatusReceiver

@Suppress("DEPRECATION")
class ConnectionActivity : BaseActivity(), WifiGpsStatusReceiver.StatusChangeListener {
    private lateinit var binding: ActivityConnectionBinding
    private lateinit var user: String
    private lateinit var locationManager: LocationManager
    private lateinit var wifiManager: WifiManager
    private var permissionFragment: PermissionFragment? = null
    private lateinit var wifiDirectFragment: WifiDirectFragment
    private lateinit var wifiGpsStatusReceiver: WifiGpsStatusReceiver
    private lateinit var intentFilter: IntentFilter

    var currentPosition = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)

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

        Log.e("TESTTAG", "ACTIVITY_SELECTION_CONNECTION user: $user")

        if (user == "sender") {
            binding.title.text = "Sender"
        } else {
            binding.title.text = "Waiting for Connection"
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

        wifiDirectFragment = WifiDirectFragment.newInstance(user)
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
            Log.e("TESTTAG", "checkAndUpdateFragment1: ")

        } else if (wifiManager.isWifiEnabled && isGPSEnabled(
                locationManager
            )
        ) {
            Log.e("TESTTAG", "checkAndUpdateFragment 2 else if: ")

            setUpConnection()
        }
    }

    private fun setUpConnection() {
        if (sender_user == "phone_clone_sender" || sender_user == "local_transfer_sender") {
            if (currentPosition == 0) {
                Log.e("TESTTAG", "setUpConnection 1: ")

                updateFragment(wifiDirectFragment)

            } else {
//                wifiDirectFragment.showQrLayout()
//                updateFragment(qrcodeFragment)
            }
        } else if (receiver_user == "phone_clone_receiver" || receiver_user == "local_transfer_receive") {
            if (currentPosition == 0) {
                Log.e("TESTTAG", "setUpConnection 2: ")

                updateFragment(wifiDirectFragment)

            } else {
                updateFragment(wifiDirectFragment)
//                updateFragment(joinHotspotFragment)
            }
        } else {
            updateFragment(wifiDirectFragment)
        }
    }

    @SuppressLint("CommitTransaction")
    fun updateFragment(fragment: Fragment) {
        Log.e("TESTTAG", "updateFragment fragment: $fragment")

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