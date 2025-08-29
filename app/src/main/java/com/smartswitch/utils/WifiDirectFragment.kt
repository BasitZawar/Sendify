package com.smartswitch.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.MacAddress
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.smartswitch.R
import com.smartswitch.connection.MyClient
import com.smartswitch.connection.MyServer
import com.smartswitch.databinding.FragmentWifiDirectBinding
import com.smartswitch.interfaces.ClickInterface
import com.smartswitch.interfaces.DeviceConnectionInterface
import com.smartswitch.presentation.adapter.WifiDirectAdapter
import java.net.InetAddress


private const val KEY_USER = "key_user_type"
private const val KEY_OPTIONAL = "key_user_type2"
private const val KEY_OPTIONAL2 = "key_user_type3"

class WifiDirectFragment : Fragment(), DeviceConnectionInterface {
    private var binding: FragmentWifiDirectBinding? = null
    private lateinit var bannerSelectConnection: FrameLayout
    private lateinit var manageAllFilesPermissionLauncher: ActivityResultLauncher<Intent>
    private var wifiP2pManager: WifiP2pManager? = null
    private lateinit var wifiP2PChannel: WifiP2pManager.Channel
    var receiver: MReceiverFragment? = null
    private lateinit var intentFilter: IntentFilter
    private val peersList: ArrayList<WifiP2pDevice> = ArrayList()
    private var devicesArrayList: ArrayList<String> = ArrayList()
    private lateinit var adapter: WifiDirectAdapter
    private var searchingDialog: Dialog? = null
    var pairingSuccess = false
    var gotIt = false
    var name = ""
    var TAG = "TESTTAG"
    private var userType: String = ""
    private var senderDevice: String = ""
    private var fromScanner: String = ""
    var attachedContext: Context? = null
    lateinit var attachedActivity: Activity
    var loader: LottieAnimationView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userType = it.getString(KEY_USER).toString()
        }
        arguments?.let {
            senderDevice = it.getString(KEY_OPTIONAL).toString()
        }
        arguments?.let {
            fromScanner = it.getString(KEY_OPTIONAL2).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWifiDirectBinding.inflate(inflater, container, false)
        return binding?.root
    }

    @RequiresPermission(Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        manageAllFilesPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
        }
        bannerSelectConnection = activity?.findViewById(R.id.bannerSelectConnection)!!

        initView()
        initScanning()
        if (fromScanner == "fromScanner") {
            binding?.layoutConnecting?.visibility = View.VISIBLE
            binding?.loader?.playAnimation()
            binding?.layoutMain?.visibility = View.GONE
        } else {
            binding?.layoutConnecting?.visibility = View.GONE
            binding?.loader?.pauseAnimation()

            binding?.layoutMain?.visibility = View.VISIBLE
        }
        Handler(Looper.getMainLooper()).postDelayed({
            if (devicesArrayList.isEmpty() && isAdded) {
                scanDevices()
            }
        }, 3000)

        binding?.btnRetry?.setOnClickListener {
            binding?.layoutLoading?.visibility = View.GONE
            binding?.layoutNoDevice?.visibility = View.GONE
                scanDevices()
        }
    }

    private fun initView() {
        attachedContext?.let {
            adapter = WifiDirectAdapter(it, devicesArrayList, object : ClickInterface {
                override fun onItemClick(position: Int) {
//                    if (userType != "sender")
                    requestPairing(position)
                }
            })
            if (fromScanner == "fromScanner") {
                Log.e("TAG", "initView: returned")
                return
            } else {
                Log.e("TAG", "initView: not returned")
                binding?.rv?.layoutManager = LinearLayoutManager(it)
                binding?.rv?.adapter = adapter
            }
        }
    }

    @RequiresPermission(Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION)
    private fun initScanning() {
        try {
            attachedContext?.let {
                wifiP2pManager =
                    attachedContext?.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
                wifiP2PChannel =
                    wifiP2pManager?.initialize(
                        it,
                        Looper.getMainLooper(),
                        null
                    )!!

                disconnectWifiDirect()
                receiver = MReceiverFragment(
                    it, wifiP2pManager!!, wifiP2PChannel, this
                )
                intentFilter = IntentFilter()

                intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
//                intentFilter
//                    .addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                intentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            }
        } catch (e: Exception) {
        }
        scanDevices()
    }

    @RequiresPermission(Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION)
    @SuppressLint("HardwareIds")
    fun scanDevices() {
        Log.e(TAG, "scanDevices: called")
        attachedContext?.let {
            if (fromScanner == "fromScanner") {
                binding?.layoutConnecting?.visibility = View.VISIBLE
                binding?.layoutMain?.visibility = View.GONE
                binding?.loader?.playAnimation()

            } else {
//                showSearchingDialog()
                binding?.layoutLoading?.visibility = View.VISIBLE
                binding?.let { it1 ->
                    Glide.with(attachedActivity).asGif().load(R.raw.scaning)
                        .into(it1.animView)
                }
            }

//            binding?.animView?.playAnimation()
//            dialog = Dialog(it)
//            dialog.setContentView(R.layout.scanning_dialog)
//            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//            dialog.setCanceledOnTouchOutside(true)
//            dialog.setCancelable(true)
//            val loader: LottieAnimationView = dialog.findViewById(R.id.imageAgree)
//            loader.playAnimation()
//            dialog.show()
        } ?: return

        if (attachedContext?.let {
                ActivityCompat.checkSelfPermission(
                    it, Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED) {
            return
        }
        wifiP2pManager?.discoverPeers(wifiP2PChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                pairingSuccess = true
            }

            override fun onFailure(reason: Int) {
                pairingSuccess = true
            }

        })
        if (Build.VERSION.SDK_INT >= 33) {
            val wifiManager: WifiManager = attachedContext?.getSystemService(
                Context.WIFI_SERVICE
            ) as WifiManager
            val wInfo: WifiInfo = wifiManager.getConnectionInfo()
            val macAddress: String = wInfo.getMacAddress()
            wifiP2pManager?.setConnectionRequestResult(wifiP2PChannel,
                MacAddress.fromString(macAddress),
                WifiP2pManager.CONNECTION_REQUEST_ACCEPT,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.e("TESTTAG", "onSuccess: connection success For android 33")
                    }

                    override fun onFailure(reason: Int) {
                        Log.e("TESTTAG", "onFailure: connection failed  For android 33$reason")
                    }
                })
        }
    }

    private fun requestPairing(pos: Int) {
        if (pos < 0 || pos >= peersList.size) {
//            activity?.let {
//                it.showMessage("No device available for pairing")
//            }
            return
        }
        val device = peersList[pos]
        val wifiConfig = WifiP2pConfig()
        wifiConfig.deviceAddress = device.deviceAddress
        wifiConfig.wps.setup = WpsInfo.PBC

        if (attachedContext?.let {
                ActivityCompat.checkSelfPermission(
                    it, Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED) {
            return
        }
        wifiP2pManager?.connect(wifiP2PChannel, wifiConfig, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                pairingSuccess = true
                Log.e("TESTTAG", "CONNECT with $name1")
            }

            override fun onFailure(reason: Int) {
                wifiP2pManager?.cancelConnect(wifiP2PChannel,
                    object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            pairingSuccess = true
//                            activity?.let {
//                                it.showMessage("Retry")
//                            }
                            Log.e(TAG, "onSuccess: Cancel Connection")
                        }

                        override fun onFailure(p0: Int) {
                            Log.e(TAG, "onFailure: Cancel Connection")
                        }
                    })
                Log.e("TESTTAG", "Failure22::$reason")
                return
            }
        })
    }

    val connectionListener = WifiP2pManager.ConnectionInfoListener() {
        val wifiP2pInfo = it
        if (wifiP2pInfo.groupFormed) {
            val groupOwnerAddress: InetAddress = wifiP2pInfo.groupOwnerAddress
            Log.d(
                TAG,
                "connectionListener: called " + (wifiP2pInfo.groupFormed) + " ** " + (wifiP2pInfo.isGroupOwner) + "  **  " + isAdded
            )
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                val serverClass = MyServer(this)
                serverClass.start()
            } else if (wifiP2pInfo.groupFormed) {
                val clientClass = MyClient(groupOwnerAddress, this)
                clientClass.start()
            }
        }
    }
    val peerList = WifiP2pManager.PeerListListener { it ->
        if (!peersList.containsAll(it.deviceList) || !it.deviceList.containsAll(peersList)) {
            peersList.clear()

            peersList.addAll(it.deviceList)
            devicesArrayList.clear()

            Log.e("TESTTAG", "DEVICE LIST121 ${peersList.size}")

            for (device in it.deviceList.withIndex()) {
                devicesArrayList.add(device.value.deviceName)
                name = device.value.deviceName
                Log.e("TESTTAG", "DEVICES 1 ${name}")
            }


//            binding?.animView?.pauseAnimation()
            binding?.layoutLoading?.visibility = View.GONE
//            if (searchingDialog!!.isShowing) {
//                searchingDialog!!.dismiss()
//            }

            if (devicesArrayList.isEmpty()) binding?.layoutNoDevice?.visibility = View.VISIBLE
            else binding?.layoutNoDevice?.visibility = View.GONE
            adapter.notifyDataSetChanged()

            if (devicesArrayList.contains(senderDevice)) {
                requestPairing(devicesArrayList.indexOf(senderDevice))
                Log.d(
                    TAG,
                    "device found start connection now: ${devicesArrayList.indexOf(senderDevice)}"
                )
                senderDevice = ""
            }
        }
    }

    private fun disconnectWifiDirect() {
        if (attachedContext?.let {
                ActivityCompat.checkSelfPermission(
                    it, Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED) {
            return
        }
        wifiP2pManager?.requestGroupInfo(wifiP2PChannel) { group ->
            if (group != null) {
                wifiP2pManager?.removeGroup(wifiP2PChannel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                    }

                    override fun onFailure(reason: Int) {
                    }
                })
            }
        }
    }

    @RequiresPermission(Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        try {
            attachedContext?.let {
                it.registerReceiver(
                    receiver!!, intentFilter, Context.RECEIVER_EXPORTED
                )
            }
            if (PermissionManager.hasLocationPermission(requireContext())) {
                scanDevices()
            } else {
            }
        } catch (e: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
//        attachedContext?.let { it.unregisterReceiver(receiver!!) }
        attachedContext?.let { context ->
            try {
                if (receiver != null) {
                    context.unregisterReceiver(receiver!!)
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace() // The receiver was not registered or was already unregistered
            }
        }
    }

    override fun onAttach(context1: Context) {
        super.onAttach(context1)
        if (isAdded) attachedContext = context1
    }

    override fun onDetach() {
        super.onDetach()
        attachedContext = null
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        attachedActivity = activity
    }

    override fun onConnectionSuccessful() {
        if (userType == "sender") {
//            startActivity(
//                Intent(
//                    attachedContext, SendActivity::class.java
//                )
//                    .putExtra("DeviceList", name1).putExtra("user", "Sender")
//            )
//            activity?.finish()
            Log.e("TAG", "onConnectionSuccessful: sender: $name")
        } else {
//            startActivity(
//                Intent(
//                    attachedContext, ReceiverActivity::class.java
//                ).putExtra("DeviceList", name).putExtra("user", "Receiver")
//            )
//            activity?.finish()
            Log.e("TAG", "onConnectionSuccessful: receiver: $name")
        }
    }

    override fun onConnectionFailed() {
        Log.e("TAG", "onConnectionFailed: Connection")
    }

    companion object {
        var name1 = ""
        fun newInstance(param1: String, param2: String? = null, param3: String? = null) =
            WifiDirectFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_USER, param1)
                    param2?.let { putString(KEY_OPTIONAL, it) }
                    param3?.let { putString(KEY_OPTIONAL2, it) }
                }
            }
    }
}