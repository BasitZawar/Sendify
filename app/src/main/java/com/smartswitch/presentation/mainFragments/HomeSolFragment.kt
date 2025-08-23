package com.smartswitch.presentation.mainFragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.smartswitch.R
import com.smartswitch.ads.PrefUtils
import com.smartswitch.ads.inter_ads.InterstitialClass
import com.smartswitch.databinding.DialogExitBinding
import com.smartswitch.databinding.FragmentHomeSolBinding
import com.smartswitch.new_ads.nativeads.NativeAdsUtil
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.Dialogs
import com.smartswitch.utils.MyDialogBox
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.setSafeOnClickListener
import com.smartswitch.utils.extensions.visible
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale


class HomeSolFragment : Fragment() {
    private var _binding: FragmentHomeSolBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        Log.d("testing", "onPause: ")
    }

    override fun onResume() {
        super.onResume()
        Log.d("testing", "onResume: ")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("testing", "onDestroyView: ")
    }

    private fun loadNative(adId: String) {

        // if (PrefUtil(this@SplashActivity).getBool("is_premium", false)) return
        MobileAds.initialize(requireContext())
        NativeAdsUtil.loadNativeAd(
            requireContext(),
            1,
            adId,
            NativeAdOptions.ADCHOICES_TOP_LEFT
        ) { nativeAd ->
            isAlive {
                val adView = layoutInflater.inflate(
                    R.layout.ads_google_small_native, null
                ) as NativeAdView
                NativeAdsUtil.populateUnifiedNativeAdView(nativeAd, adView)
                binding.nativeAd.removeAllViews()
                binding.nativeAd.addView(adView)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isAlive { activityContext ->

            if (PrefUtil(requireContext()).getBool("is_premium", false)) {

                binding.nativeBannerPlaceHolder.gone()
            } else {
                binding.nativeBannerPlaceHolder.visible()
                loadNative(getString(R.string.native_home))


            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermission(activityContext)
            }



            (activityContext as FragmentActivity).handleBackPressWithAction {
                showExitDialog {
                    activityContext.finishAffinity()

                }
            }

            binding.toolbar.setNavigationOnClickListener {
                findNavController().navigate(R.id.action_homeSendifyFragment_to_settingsSendifyFragment)
            }


            binding.premiumBtn.setSafeOnClickListener {
                //findNavController().navigate(R.id.action_homeSendifyFragment_to_premiumLifeTimeFragment)
                //findNavController().navigate(HomeSolFragmentDirections.actionHomeSendifyFragmentToPremiumLifeTimeFragment("home"))
                findNavController().navigate(
                    HomeSolFragmentDirections.actionHomeSendifyFragmentToPremiumFragment(
                        "home"
                    )
                )
            }

           /* binding.historyBtn.setSafeOnClickListener {
                findNavController().navigate(R.id.action_homeSendifyFragment_to_historySendifyFragment)
            }*/

            binding.historyBtn.setSafeOnClickListener {
                val navController = findNavController()
                val currentId = navController.currentDestination?.id

                if (currentId == R.id.homeSendifyFragment) {
                    navController.navigate(R.id.action_homeSendifyFragment_to_historySendifyFragment)
                } else {
                    Log.w("NavigationWarning", "Tried to navigate from wrong fragment: $currentId")
                }
            }

            binding.cloneBtn.setSafeOnClickListener {


                if (PermissionManager.hasLocationPermission(activityContext) && PermissionManager.hasNearbyPermission(
                        activityContext
                    ) && PermissionManager.hasStorageAccessPermission(activityContext)
                ) {
                    if (PrefUtil(requireContext()).getBool(
                            "is_premium",
                            false
                        ) || !InterstitialClass.isInternetAvailable(
                            requireContext()
                        )
                    ) {
                        findNavController().navigate(R.id.action_homeSendifyFragment_to_cloneSendifyFragment)
                    } else {
                        InterstitialClass.request_interstitial(
                            requireContext(),
                            requireActivity(),
                            getString(R.string.inter_all)
                        ) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                isAlive {
                                    findNavController().navigate(R.id.action_homeSendifyFragment_to_cloneSendifyFragment)
                                }
                            }, 200L)
                        }


                    }
                } else {
                    findNavController().navigate(
                        HomeSolFragmentDirections.actionHomeSendifyFragmentToPermissionSendifyFragment(
                            "home"
                        )
                    )
                }
            }

            binding.sendBtn.setSafeOnClickListener {


                if (PermissionManager.hasLocationPermission(activityContext) && PermissionManager.hasNearbyPermission(
                        activityContext
                    ) && PermissionManager.hasStorageAccessPermission(activityContext)
                ) {
                    if (PrefUtil(requireContext()).getBool(
                            "is_premium",
                            false
                        ) || !InterstitialClass.isInternetAvailable(
                            requireContext()
                        )
                    ) {
                        findNavController().navigate(R.id.action_homeSendifyFragment_to_mediaSendifyFragment)
                    } else {
                        InterstitialClass.request_interstitial(
                            requireContext(),
                            requireActivity(),
                            getString(R.string.inter_send_receive_button)
                        ) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                isAlive {
                                    findNavController().navigate(R.id.action_homeSendifyFragment_to_mediaSendifyFragment)
                                }
                            }, 200L)
                        }
                    }
                } else {

                    findNavController().navigate(
                        HomeSolFragmentDirections.actionHomeSendifyFragmentToPermissionSendifyFragment(
                            "home"
                        )
                    )
                }
            }

            binding.receiveBtn.setSafeOnClickListener {

                if (PermissionManager.hasLocationPermission(activityContext) && PermissionManager.hasNearbyPermission(
                        activityContext
                    ) && PermissionManager.hasStorageAccessPermission(activityContext)
                ) {
                    if (PrefUtil(requireContext()).getBool(
                            "is_premium",
                            false
                        ) || !InterstitialClass.isInternetAvailable(
                            requireContext()
                        )
                    ) {
                        findNavController().navigate(R.id.action_homeSendifyFragment_to_receiverScanDeviceSendifyFragment)
                    } else {
                        InterstitialClass.request_interstitial(
                            requireContext(),
                            requireActivity(),
                            getString(R.string.inter_send_receive_button)
                        ) {
                            isAlive {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    isAlive {
                                        findNavController().navigate(R.id.action_homeSendifyFragment_to_receiverScanDeviceSendifyFragment)
                                    }
                                }, 200L)
                            }
                        }
                    }
                } else {

                    val bundle = Bundle().apply {
                        putString("navigationType", "receive")
                    }

                    findNavController().navigate(
                        HomeSolFragmentDirections.actionHomeSendifyFragmentToPermissionSendifyFragment(
                            "home"
                        )
                    )
                }
            }

            binding.imagesBtn.setSafeOnClickListener {

                if (permissionsGranted(activityContext)) {

                    if (PrefUtil(requireContext()).getBool(
                            "is_premium",
                            false
                        ) || !InterstitialClass.isInternetAvailable(
                            requireContext()
                        )
                    ) {
                        navigateToMediaFragment(1)
                    } else {
                        InterstitialClass.request_interstitial(
                            requireContext(),
                            requireActivity(),
                            getString(R.string.inter_all)
                        ) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                Log.d("PermissionCheck", "permissionsGranted: ")
                                isAlive {
                                    navigateToMediaFragment(1)
                                }
                            }, 200L)
                        }
                    }
                } else {
                    showPermissionDialog(
                        layoutResId = R.layout.photo_permission_dialog,
                        onAllowClicked = {
                            storagePermission()

                        },
                        onDismissed = {
                        }
                    )

                }
            }

            binding.videosBtn.setSafeOnClickListener {

                if (permissionsGranted(activityContext)) {

                    if (PrefUtil(requireContext()).getBool(
                            "is_premium",
                            false
                        ) || !InterstitialClass.isInternetAvailable(
                            requireContext()
                        )
                    ) {
                        navigateToMediaFragment(2) // Document tab
                    } else {
                        InterstitialClass.request_interstitial(
                            requireContext(),
                            requireActivity(),
                            getString(R.string.inter_all)
                        ) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                Log.d("PermissionCheck", "permissionsGranted: ")
                                isAlive {
                                    navigateToMediaFragment(2) // Document tab
                                }
                            }, 200L)
                        }
                    }
                } else {
                    showPermissionDialog(
                        layoutResId = R.layout.video_permission_dialog,
                        onAllowClicked = {
                            storagePermission()

                        },
                        onDismissed = {
                        }
                    )

                }
            }

            binding.audiosBtn.setSafeOnClickListener {

                if (permissionsGranted(activityContext)) {
                    if (PrefUtil(requireContext()).getBool(
                            "is_premium",
                            false
                        ) || !InterstitialClass.isInternetAvailable(
                            requireContext()
                        )
                    ) {
                        navigateToMediaFragment(3) // Document tab
                    } else {
                        InterstitialClass.request_interstitial(
                            requireContext(),
                            requireActivity(),
                            getString(R.string.inter_all)
                        ) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                isAlive {
                                    Log.d("PermissionCheck", "permissionsGranted: ")
                                    navigateToMediaFragment(3) // Document tab

                                }
                            }, 200L)

                        }
                    }
                } else {
                    showPermissionDialog(
                        layoutResId = R.layout.audio_permission_dialog,
                        onAllowClicked = {
                            storagePermission()

                        },
                        onDismissed = {
                        }
                    )

                }
            }

            binding.docsBtn.setSafeOnClickListener {

                if (permissionsGranted(activityContext)) {
                    if (PrefUtil(requireContext()).getBool(
                            "is_premium",
                            false
                        ) || !InterstitialClass.isInternetAvailable(
                            requireContext()
                        )
                    ) {
                        navigateToMediaFragment(4) // Document tab
                    } else {
                        InterstitialClass.request_interstitial(
                            requireContext(),
                            requireActivity(),
                            getString(R.string.inter_all)
                        ) {
                            Handler(Looper.getMainLooper()).postDelayed({

                                isAlive {
                                    navigateToMediaFragment(4) // Document tab
                                    Log.d("PermissionCheck", "permissionsGranted: ")
                                }
                            }, 200L)
                        }
                    }
                } else {
                    showPermissionDialog(
                        layoutResId = R.layout.document_permission_dialog,
                        onAllowClicked = {
                            storagePermission()

                        },
                        onDismissed = {

                        }
                    )

                }
            }

            binding.appsBtn.setSafeOnClickListener {

                if (permissionsGranted(activityContext)) {
                    if (PrefUtil(requireContext()).getBool(
                            "is_premium",
                            false
                        ) || !InterstitialClass.isInternetAvailable(
                            requireContext()
                        )
                    ) {
                        navigateToMediaFragment(0) // Document tab
                    } else {
                        InterstitialClass.request_interstitial(
                            requireContext(),
                            requireActivity(),
                            getString(R.string.inter_all)
                        ) {
                            isAlive {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    isAlive {
                                        Log.d("PermissionCheck", "permissionsGranted: ")
                                        navigateToMediaFragment(0) // Document tab
                                    }
                                }, 200L)
                            }
                        }
                    }
                } else {
                    showPermissionDialog(
                        layoutResId = R.layout.app_permission_dialog,
                        onAllowClicked = {
                            storagePermission()

                        },
                        onDismissed = {
                        }
                    )

                }
            }

            binding.contactBtn.setSafeOnClickListener {

                if (PermissionManager.hasContactPermission(activityContext)) {
                    if (PrefUtil(requireContext()).getBool(
                            "is_premium",
                            false
                        ) || !InterstitialClass.isInternetAvailable(
                            requireContext()
                        )
                    ) {
                        navigateToMediaFragment(5) // Document tab
                    } else {
                        InterstitialClass.request_interstitial(
                            requireContext(),
                            requireActivity(),
                            getString(R.string.inter_all)
                        ) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                isAlive {
                                    Log.d("PermissionCheck", "permissionsGranted: ")
                                    navigateToMediaFragment(5) // Document tab
                                }
                            }, 200L)
                        }
                    }
                } else {
                    showPermissionDialog(
                        layoutResId = R.layout.contacts_permission_dialog,
                        onAllowClicked = {
                            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)

                        },
                        onDismissed = {
                        }
                    )

                }
            }

            getStorageStatus()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun notificationPermission(context: Activity) {
        if (PermissionManager.hasNotificationPermission(context)) {
            //navigateToMediaFragment(5) // Document tab
            Log.d("PermissionCheck", "permissionsGranted: ")
        } else {

            showPermissionDialog(
                layoutResId = R.layout.notification_permission_dialog,
                onAllowClicked = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                },
                onDismissed = {

                }
            )


        }
    }

    override fun onStop() {
        super.onStop()
        PrefUtils.setBoolean(requireContext(), "is_first_time_launch", true)
    }

    private fun permissionsGranted(activityContext: FragmentActivity): Boolean {
        return PermissionManager.hasStorageAccessPermission(activityContext)
    }

    private var dialog: Dialog? = null
    private fun showPermissionDialog(
        layoutResId: Int,
        onAllowClicked: () -> Unit,
        onDismissed: () -> Unit
    ) {
        context?.let { act ->
            val dialogView = LayoutInflater.from(act).inflate(layoutResId, null)

            // Access the buttons inside dialog
            val allowBtn: View = dialogView.findViewById(R.id.allowBtn)
            val cancelButton: View = dialogView.findViewById(R.id.cancel_button)


            dialog = MyDialogBox.getInstance(act as Activity)
                ?.setContentViewWithDismissCallBack(dialogView, true, 0.85f) {
                    onDismissed() // 👈 invoke your dismiss action
                }?.showDialog()
            allowBtn.setOnClickListener {
                onAllowClicked.invoke()
                dialog?.dismiss()
            }

            cancelButton.setOnClickListener {
                dialog?.dismiss()
            }
        }
    }

    private fun storagePermission() {
        if (!PermissionManager.hasStorageAccessPermission(requireActivity())) {
            requestPermissionStorage()
        }
    }

    private fun requestPermissionStorage() {
        isAlive { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val intent =
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            addCategory(Intent.CATEGORY_DEFAULT)
                            data = Uri.parse("package:${activity.packageName}")
                        }
                    requestPermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    requestPermissionLauncher.launch(intent)
                }
                //checkPermissionInBackground()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Log.d("PermissionCheck", "Managed Permission is granted.")
                } else {
                    Log.d("PermissionCheck", "Managed Permission is not granted.")
                }
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions =
                permissions.filter { !it.value } // Collect all denied permissions
            if (deniedPermissions.isEmpty()) {
                navigateToMediaFragment(0) // Document tab
            } else {
                // Show a single dialog explaining the denied permissions
                showPermissionRationaleOrSettings(deniedPermissions.keys.toList())
            }
        }

    private fun navigateToMediaFragment(tabPosition: Int) {

        findNavController().navigate(
            R.id.action_homeSendifyFragment_to_mediaSendifyFragment,
            Bundle().apply { putInt("tab_position", tabPosition) })
    }

    private fun showPermissionRationaleOrSettings(deniedPermissions: List<String>) {
        isAlive { activity ->
            // Check if any of the denied permissions should show rationale
            val shouldShowRationale = deniedPermissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }

            if (shouldShowRationale) {
                showRationaleDialog(deniedPermissions)
            } else {
                showSettingsDialog()
            }
        }
    }

    private fun showRationaleDialog(deniedPermissions: List<String>) {
        isAlive { activity ->
            val permissionNames =
                deniedPermissions.joinToString(", ") { getPermissionName(it) } // Get user-friendly names of permissions

            AlertDialog.Builder(activity)
                .setTitle(getString(R.string.permission_required))
                .setMessage(
                    "${getString(R.string.the_app_needs_the_following_permissions)}: $permissionNames. ${
                        getString(
                            R.string.please_grant_these_permissions
                        )
                    }."
                )
                .setPositiveButton(getString(R.string.grant)) { _, _ ->
                    permissionLauncher.launch(deniedPermissions.toTypedArray()) // Re-request the denied permissions
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .create()
                .show()
        }
    }

    private fun showSettingsDialog() {
        isAlive { activity ->
            AlertDialog.Builder(activity)
                .setTitle(getString(R.string.permission_denied))
                .setMessage(getString(R.string.some_permissions_are_permanently_denied))
                .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                    activity.startActivity(intent)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .create()
                .show()
        }
    }

    // Helper function to get user-friendly permission names
    private fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> "Fine Location"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Coarse Location"
            Manifest.permission.NEARBY_WIFI_DEVICES -> "Nearby WiFi Devices"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Write External Storage"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Read External Storage"
            else -> permission
        }
    }

    private val contactPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            isAlive { act ->

                if (granted) {
                    navigateToMediaFragment(5) // Document tab
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            act,
                            Manifest.permission.READ_CONTACTS
                        )
                    ) {
                        lifecycleScope.launch {
                            delay(200)
                            Dialogs.permissionDeniedDialog(
                                act,
                                getString(R.string.tap_on_settings_to_enable_required_permissions),
                            ) {

                                val intent = Intent().apply {
                                    action =
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    data = Uri.fromParts("package", act.packageName, null)
                                }
                                startActivity(intent)
                            }
                        }
                    }
                }
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            isAlive { act ->

                if (granted) {
                    //navigateToMediaFragment(5) // Document tab
                    Log.d("PermissionCheck", "permissionsGranted: ")
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            act,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    ) {
                        lifecycleScope.launch {
                            delay(200)
                            Dialogs.permissionNotificationDeniedDialog(
                                act,
                                getString(R.string.tap_on_settings_to_enable_required_permissions),
                            ) {

                                val intent = Intent().apply {
                                    action =
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    data = Uri.fromParts("package", act.packageName, null)
                                }
                                startActivity(intent)
                            }
                        }
                    }
                }
            }
        }


    @SuppressLint("SetTextI18n")
    private fun getStorageStatus() {
        val iPath: File = Environment.getDataDirectory()
        val iStat = StatFs(iPath.path)
        val iBlockSize = iStat.blockSizeLong
        val iAvailableBlocks = iStat.availableBlocksLong
        val iTotalBlocks = iStat.blockCountLong
        val iAvailableSpaceBytes = iAvailableBlocks * iBlockSize
        val iTotalSpaceBytes = iTotalBlocks * iBlockSize
        val iUsedSpaceBytes = iTotalSpaceBytes - iAvailableSpaceBytes

        val percentUsed = (iUsedSpaceBytes.toFloat() / iTotalSpaceBytes.toFloat()) * 100f

        binding.circularProgressView.setProgress(percentUsed)

        binding.totalSpaceTv.text =
            "${getString(R.string.total_space)} : ${getReadableFileSize(iTotalSpaceBytes)}"
        //binding.availSpaceTv.text = "${getString(R.string.free)} : ${getReadableFileSize(iAvailableSpaceBytes)}"
        binding.storageText.text =
            "${getReadableFileSize(iAvailableSpaceBytes)} \n${getString(R.string.free)}"
    }


    private fun getReadableFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes B" // Bytes
            sizeInBytes < 1024 * 1024 -> String.format(
                Locale.getDefault(),
                "%.2f KB",
                sizeInBytes / 1024.0
            ) // Kilobytes
            sizeInBytes < 1024 * 1024 * 1024 -> String.format(
                Locale.getDefault(),
                "%.2f MB",
                sizeInBytes / (1024.0 * 1024.0)
            ) // Megabytes
            else -> String.format(
                Locale.getDefault(),
                "%.2f GB",
                sizeInBytes / (1024.0 * 1024.0 * 1024.0)
            ) // Gigabytes
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    private fun showExitDialog(onAllowClicked: () -> Unit) {
        context?.let { act ->
            val binding = DialogExitBinding.inflate(LayoutInflater.from(act))

            val dialog = MyDialogBox.getInstance(act as Activity)
                ?.setContentViewWithDismissCallBack(binding.root, true, 0.85f) {
                }?.showDialog()

            binding.allowBtn.setOnClickListener {
                onAllowClicked.invoke()
                dialog?.dismiss()
            }

            binding.cancelButton.setOnClickListener {
                dialog?.dismiss()
            }
        }
    }


}