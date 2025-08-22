package com.smartswitch.utils

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.smartswitch.R
import com.smartswitch.databinding.WaitingProgressLayoutBinding
import androidx.core.graphics.drawable.toDrawable

object AlertDialogManager {

    /**
    * (1) : Creating Waiting Dialog box
    * */
    var waitingDialog: AlertDialog? = null
    fun createWaitingDialog(context: Context): AlertDialog {
        val builder = AlertDialog.Builder(context)
        val binding = WaitingProgressLayoutBinding.inflate(LayoutInflater.from(context))
        binding.apply {
            tvWaiting.text = "Please wait"
        }
        builder.setView(binding.root)
        builder.setCancelable(false)
        waitingDialog = builder.create()
        return waitingDialog!!
    }

    /**
     * (2) : Creating Waiting Dialog box
     * */
    fun Dialog.showDialogSafely(activity: Activity?) {
        if (activity != null && !activity.isFinishing && !activity.isDestroyed && !this.isShowing && this.window != null) {
            this.show()
        }
    }

    /**
     * (3) : Creating Waiting Dialog box
     * */
    fun Dialog.dismissDialogSafely(activity: Activity?) {
        if (activity != null && !activity.isFinishing && !activity.isDestroyed && this.isShowing) {
            this.dismiss()
        } else {
            Log.e("AlertDialogManager", "Activity is null or already destroyed")
        }
    }

    fun createCustomDialog(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String,
        onPositiveButtonClick: () -> Unit,
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                dialog.dismiss()
                onPositiveButtonClick()
            }
            .setCancelable(false)
        return builder.create()
    }

    fun createCustomDialogWithNoButton(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String,
        onPositiveButtonClick: () -> Unit,
        onNegativeButtonClick: () -> Unit
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                onPositiveButtonClick()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                onNegativeButtonClick()
            }
            .setCancelable(false)
        return builder.create()
    }

    fun showCustomCancelDialog(
        context: Context,
        title: String,
        message: String,
        onYesClick: () -> Unit,
        onNoClick: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.sendingfaildialog) // Replace with your layout file name
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        // Find views in the dialog
        val txtYes = dialog.findViewById<TextView>(R.id.txtYes)
        val txtNo = dialog.findViewById<TextView>(R.id.txtNo)
        val tvCancel = dialog.findViewById<TextView>(R.id.tvCancel)

        // Customize text if needed
        tvCancel.text = context.getString(R.string.are_you_sure_you_want_to_cancel_data_sending)

        // Set click listeners
        txtYes.setOnClickListener {
            onYesClick()
            dialog.dismiss()
        }

        txtNo.setOnClickListener {
            onNoClick()
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }

}