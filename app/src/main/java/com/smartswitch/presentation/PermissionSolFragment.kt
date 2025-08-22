package com.smartswitch.presentation

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.smartswitch.R
import com.smartswitch.ads.PrefUtils
import com.smartswitch.databinding.FragmentPermissionSolBinding
import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.invisible
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.setSafeOnClickListener
import com.smartswitch.utils.extensions.visible
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class PermissionSolFragment : Fragment() {
    private var _binding: FragmentPermissionSolBinding? = null
    private val binding get() = _binding!!
    private val args: PermissionSolFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPermissionSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Retrieve the navigation type (send or receive) from the arguments
        //val navigationType = arguments?.getString("navigationType")

        isAlive { activityContext ->

            when (args.from) {
                "home" -> binding.skipButton.invisible()
                "intro" -> binding.skipButton.visible()
                else -> binding.skipButton.visible()
            }


            binding.apply {

                (activityContext as FragmentActivity).handleBackPressWithAction {

                    findNavController().navigate(PermissionSolFragmentDirections.actionPermissionSendifyFragmentToHomeSendifyFragment())

                }

                skipButton.setSafeOnClickListener {
                    navigateToDashboard()
                }

                locationAllowButton.setSafeOnClickListener {
                    if (!PermissionManager.hasLocationPermission(activityContext)) {
                        requestPermissionsLocation()
                    }
                }

                nearbyAllowButton.setSafeOnClickListener {
                    if (!PermissionManager.hasNearbyPermission(activityContext)) {
                        requestPermissionsNearBy()
                    }
                }

                storageAllowButton.setSafeOnClickListener {
                    if (!PermissionManager.hasStorageAccessPermission(activityContext)) {
                        requestPermissionStorage()
                    }
                }
                getStartedBtn.setSafeOnClickListener {
                    // Check if all permissions are granted
                    if (PermissionManager.hasLocationPermission(requireContext()) &&
                        PermissionManager.hasNearbyPermission(requireContext()) &&
                        PermissionManager.hasStorageAccessPermission(requireContext())
                    ) {
                        // All permissions granted - proceed with navigation
                        when (args.from) {
                            "home" -> {
                                findNavController().navigateUp()
                            }
                            "intro" -> {
                                navigateToDashboard()
                            }
                            else -> {
                                navigateToDashboard()
                            }
                        }
                    } else {
                        // Request permissions one by one
                        requestPermissionsSequentially()
                    }
                }

            /*    getStartedBtn.setSafeOnClickListener {
                    // Check if all permissions are granted
                    if (PermissionManager.hasLocationPermission(requireContext()) &&
                        PermissionManager.hasNearbyPermission(requireContext()) &&
                        PermissionManager.hasStorageAccessPermission(requireContext())
                    ) {
                        // Navigate based on the navigation type (send or receive)
                        when (args.from) {
                            "home" -> {
                                findNavController().navigateUp()
                            }

                            "intro" -> {
                                navigateToDashboard()
                            }

                            else -> {
                                navigateToDashboard()
                            }
                        }
                    } else {
                        if (!PermissionManager.hasLocationPermission(activityContext)) {
                            requestPermissionsLocation()
                        }
                        if (!PermissionManager.hasNearbyPermission(activityContext)) {
                            requestPermissionsNearBy()
                        }
                        if (!PermissionManager.hasStorageAccessPermission(activityContext)) {
                            requestPermissionStorage()
                        }
                        Snackbar.make(
                            binding.root,
                            resources.getString(R.string.please_allow_all_permissions),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }*/
            }
        }
    }
    private fun requestPermissionsSequentially() {
        when {
            !PermissionManager.hasLocationPermission(requireContext()) -> {
                requestPermissionsLocation()
            }
            !PermissionManager.hasNearbyPermission(requireContext()) -> {
                requestPermissionsNearBy()
            }
            !PermissionManager.hasStorageAccessPermission(requireContext()) -> {
                requestPermissionStorage()
            }
            else -> {
                // All permissions granted - proceed with navigation
                
                when (args.from) {
                    "home" -> {
                        findNavController().navigateUp()
                    }

                    "intro" -> {
                        navigateToDashboard()
                    }

                    else -> {
                        navigateToDashboard()
                    }
                }
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // After one permission is granted, check for next one
            requestPermissionsSequentially()
        } else {
            Snackbar.make(
                binding.root,
                resources.getString(R.string.please_allow_all_permissions),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }
    private fun navigateToDashboard() {
        PrefUtils.setBoolean(requireContext(), "is_first_time_launch1", true)
//        findNavController().navigate(R.id.action_permissionSendifyFragment_to_homeSendifyFragment)
        if (PrefUtil(requireContext()).getBool("is_premium", false)) {
            findNavController().navigate(PermissionSolFragmentDirections.actionPermissionSendifyFragmentToHomeSendifyFragment())
        } else {
            findNavController().navigate(
                PermissionSolFragmentDirections.actionPermissionSendifyFragmentToPremiumFragment(
                    "permission"
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        PrefUtils.setBoolean(requireContext(), "is_first_time_launch1", true)

    }


    override fun onResume() {
        super.onResume()
        buttonCheck()
    }


    private fun buttonCheck() {
        isAlive { activityContext ->


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                binding.nearbyCard.visible()
            } else {
                binding.nearbyCard.gone()
            }

            if (PermissionManager.hasLocationPermission(activityContext)) {
                binding.locationAllowButton.text = resources.getString(R.string.allowed)
                binding.locationAllowButton.isEnabled = false
                binding.locationAllowButton.setBackgroundColor(
                    ContextCompat.getColor(
                        activityContext, R.color.colorPrimary
                    )
                )
                binding.locationAllowButton.setTextColor(
                    ContextCompat.getColor(
                        activityContext,
                        R.color.white
                    ) // Set text color for "Allowed" state
                )

            } else {
                binding.locationAllowButton.text = resources.getString(R.string.allow)
                binding.locationAllowButton.isEnabled = true
                binding.locationAllowButton.setBackgroundColor(
                    ContextCompat.getColor(
                        activityContext, R.color.allowed_bg
                    )
                )
            }

            if (PermissionManager.hasNearbyPermission(activityContext)) {
                binding.nearbyAllowButton.text = resources.getString(R.string.allowed)
                binding.nearbyAllowButton.isEnabled = false
                binding.nearbyAllowButton.setBackgroundColor(
                    ContextCompat.getColor(
                        activityContext, R.color.colorPrimary
                    )
                )
                binding.nearbyAllowButton.setTextColor(
                    ContextCompat.getColor(
                        activityContext,
                        R.color.white
                    ) // Set text color for "Allowed" state
                )

            } else {
                binding.nearbyAllowButton.text = resources.getString(R.string.allow)
                binding.nearbyAllowButton.isEnabled = true
                binding.nearbyAllowButton.setBackgroundColor(
                    ContextCompat.getColor(
                        activityContext, R.color.allowed_bg
                    )
                )
            }


            if (PermissionManager.hasStorageAccessPermission(activityContext)) {
                binding.storageAllowButton.text = resources.getString(R.string.allowed)
                binding.storageAllowButton.isEnabled = false
                binding.storageAllowButton.setBackgroundColor(
                    ContextCompat.getColor(
                        activityContext, R.color.colorPrimary
                    )
                )

                binding.storageAllowButton.setTextColor(
                    ContextCompat.getColor(
                        activityContext,
                        R.color.white
                    ) // Set text color for "Allowed" state
                )


            } else {
                binding.storageAllowButton.text = resources.getString(R.string.allow)
                binding.storageAllowButton.isEnabled = true
                binding.storageAllowButton.setBackgroundColor(
                    ContextCompat.getColor(
                        activityContext, R.color.allowed_bg
                    )
                )
            }
        }
    }


    private fun requestPermissionsLocation() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestPermissionsNearBy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.NEARBY_WIFI_DEVICES

                )
            )
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
                checkPermissionInBackground()
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


    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions =
                permissions.filter { !it.value } // Collect all denied permissions
            if (deniedPermissions.isEmpty()) {
                buttonCheck() // All permissions are granted
            } else {
                // Show a single dialog explaining the denied permissions
                showPermissionRationaleOrSettings(deniedPermissions.keys.toList())
            }
        }


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            buttonCheck()
        }


    private fun checkPermissionInBackground() {
        lifecycleScope.launch {
            while (true) {
                delay(500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        startActivity(
                            Intent(activity, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        )
                        delay(500)
                        break
                    }
                }
            }
        }
    }


    private fun showPermissionRationaleOrSettings(deniedPermissions: List<String>) {
        isAlive { activity ->
            // Check if any of the denied permissions should show rationale
            val shouldShowRationale = deniedPermissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }

            if (shouldShowRationale) {
                // Show a rationale for the denied permissions
                showRationaleDialog(deniedPermissions)
            } else {
                // Direct to settings if permissions were denied permanently
                showSettingsDialog()
            }
        }
    }

    private fun showRationaleDialog(deniedPermissions: List<String>) {
        isAlive { activity ->
            val permissionNames =
                deniedPermissions.joinToString(", ") { getPermissionName(it) } // Get user-friendly names of permissions

            AlertDialog.Builder(activity)
                .setTitle(resources.getString(R.string.permission_required))
                .setMessage(
                    "${resources.getString(R.string.the_app_needs_the_following_permissions)}: $permissionNames. ${
                        resources.getString(
                            R.string.please_grant_these_permissions
                        )
                    }."
                )
                .setPositiveButton(resources.getString(R.string.grant)) { _, _ ->
                    permissionLauncher.launch(deniedPermissions.toTypedArray()) // Re-request the denied permissions
                }
                .setNegativeButton(resources.getString(R.string.cancel), null)
                .create()
                .show()
        }
    }

    private fun showSettingsDialog() {
        isAlive { activity ->
            AlertDialog.Builder(activity)
                .setTitle(resources.getString(R.string.permission_denied))
                .setMessage(resources.getString(R.string.some_permissions_are_permanently_denied))
                .setPositiveButton(resources.getString(R.string.go_to_settings)) { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                    activity.startActivity(intent)
                }
                .setNegativeButton(resources.getString(R.string.cancel), null)
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

}