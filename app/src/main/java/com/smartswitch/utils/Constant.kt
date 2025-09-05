package com.smartswitch.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartswitch.R

object Constant {
    const val PORT = 8080
    const val preferencefName = "SmartSwitchAwsPref"
    const val multiContactsFileName = "_AllContacts_.zip"
    const val KEY_CONTACTS = "contacts"
    const val STORAGE_PERMISSION_CODE = 100


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

    @SuppressLint("RestrictedApi")
    fun showSnackBar(activity: Activity, message: String, drawable: Int? = null) {
        try {
            val rootView = activity.findViewById<View>(android.R.id.content)
            val snack = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
            val snackBarView = snack.view as Snackbar.SnackbarLayout
            snackBarView.background =
                ContextCompat.getDrawable(activity, R.drawable.snackbar_background)
            val params = snackBarView.layoutParams as FrameLayout.LayoutParams
            params.setMargins(
                30,
                params.topMargin,
                30,
                activity.resources.getDimension(com.intuit.sdp.R.dimen._60sdp).toInt()
            )
            snackBarView.layoutParams = params
            val imageView = ImageView(activity)
            val iconDrawable = drawable?.let { ContextCompat.getDrawable(activity, it) }
            iconDrawable?.let {
                imageView.setImageDrawable(it)
                val imageViewParams = LinearLayout.LayoutParams(
                    60, // Width
                    60  // Height
                )
                imageViewParams.gravity = Gravity.CENTER_VERTICAL
                imageViewParams.setMargins(10, 0, 20, 0) // Margin between icon and text
                imageView.layoutParams = imageViewParams
            }
            // Access the TextView inside the Snackbar
            val textView =
                snackBarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView.maxLines = 1
            (textView.parent as? ViewGroup)?.removeView(textView)
            // Create a new LinearLayout to hold both ImageView and TextView
            val snackBarLayout = LinearLayout(activity)
            snackBarLayout.orientation = LinearLayout.HORIZONTAL
            snackBarLayout.gravity = Gravity.CENTER_VERTICAL
            snackBarLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (iconDrawable != null) {
                snackBarLayout.addView(imageView)
            }
            snackBarLayout.addView(textView)
            snackBarView.removeAllViews()
            snackBarView.addView(snackBarLayout)
            snack.show()
        } catch (_: Exception) {
        }
    }

    fun requestStoragePermission11(
        context: Context, manageAllFilesPermissionLauncher: ActivityResultLauncher<Intent>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    addCategory("android.intent.category.DEFAULT")
                    data = Uri.parse("package:${context.packageName}")
                }
                if (context.packageManager.resolveActivity(
                        intent, PackageManager.MATCH_DEFAULT_ONLY
                    ) != null
                ) {
                    manageAllFilesPermissionLauncher.launch(intent)
                } else {
                    val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    if (context.packageManager.resolveActivity(
                            fallbackIntent, PackageManager.MATCH_DEFAULT_ONLY
                        ) != null
                    ) {
                        manageAllFilesPermissionLauncher.launch(fallbackIntent)
                    } else {
                        // No activity found to handle either intent
                        showSnackBar(
                            context as Activity,
                            "This device does not support managing all files access.",
                            R.drawable.ic_launcher_background
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showSnackBar(
                    context as Activity,
                    "Error requesting storage permission",
                    R.drawable.ic_launcher_background
                )
//                Toast.makeText(context, "Error requesting storage permission: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            // For Android versions below R, request regular storage permissions
            ActivityCompat.requestPermissions(
                context as Activity, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), STORAGE_PERMISSION_CODE
            )
        }
    }

    fun checkPermission(context: Context): Boolean {
        val storagePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return storagePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED
    }

    fun showSettingDialog(context: Context) {
        val exitDialog = MaterialAlertDialogBuilder(context).setTitle("Storage Permission Denied")
            .setMessage("Permission Requires to Proceed with app").setNegativeButton(
                context.getString(R.string.str_cancel)
            ) { dialog, _ ->
                dialog.dismiss()
            }.setPositiveButton(
                context.getString(R.string.open_setting)
            ) { dialog, _ ->
                dialog.dismiss()
                openSettings(context)
            }.create()
        // Set colors for both buttons (Open Settings and Cancel) in a single OnShowListener
        exitDialog.setOnShowListener {
            val positiveButton = exitDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = exitDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setTextColor(ContextCompat.getColor(context, R.color.black))
            negativeButton.setTextColor(ContextCompat.getColor(context, R.color.black))
        }
        exitDialog.show()
    }

    private fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        context.startActivity(intent)
    }
    fun customSystemBars(
        activity: Activity,
        statusBarColor: Int,
        navigationBarColor: Int,
        lightStatusBar: Boolean = false,
        lightNavigationBar: Boolean = true
    ) {
        try {
            val window = activity.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(activity, statusBarColor)
            window.navigationBarColor = ContextCompat.getColor(activity, navigationBarColor)

            // Handle light status bar appearance
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = lightStatusBar
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isAppearanceLightNavigationBars = lightNavigationBar
                }
            }
            // Optional: Handle light navigation bar appearance for API < 26
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && lightNavigationBar) {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}