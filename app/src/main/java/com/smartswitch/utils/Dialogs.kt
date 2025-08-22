package com.smartswitch.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.util.Log
import com.smartswitch.databinding.ContactsPermissionDialogBinding
import com.smartswitch.databinding.PermissionDeniedDialogBinding
import com.smartswitch.databinding.PermissionNotificationDeniedDialogBinding
import com.smartswitch.utils.extensions.setSafeOnClickListener

object Dialogs {

    private var alert: AlertDialog? = null

    @SuppressLint("StaticFieldLeak")
    fun cancelDialog() {
        if (alert?.isShowing == true) {
            try {
                alert?.dismiss()
            } catch (e: Exception) {
                Log.e("mainTextView", e.toString())
            }
        }
    }


    fun isDialogShowing(): Boolean {
        return alert?.isShowing == true
    }

    fun contactPermissionDialog(
        mCurrentActivity: Activity,
        onItemClick: (String) -> Unit
    ) {

        try {
            cancelDialog()
            val binding = ContactsPermissionDialogBinding.inflate(mCurrentActivity.layoutInflater)
            val alertDialogBuilder = AlertDialog.Builder(mCurrentActivity)
            alertDialogBuilder.setView(binding.root)



            binding.allowBtn.setSafeOnClickListener {
                alert?.dismiss()
                onItemClick("allow")

            }
            binding.cancelButton.setSafeOnClickListener {
                alert?.dismiss()
                onItemClick("cancel")
            }



            alert = alertDialogBuilder.create()


            alert?.setOnCancelListener {
                onItemClick("cancel")
            }


            val back = ColorDrawable(Color.TRANSPARENT)
            val inset = InsetDrawable(back, 20)
            alert?.window?.setBackgroundDrawable(inset)
            alert?.setCanceledOnTouchOutside(true)
            if (!mCurrentActivity.isFinishing) {
                alert?.show()
            }


        } catch (e: Exception) {
            Log.d("mainTextView", e.toString())
        }
    }


    fun permissionDeniedDialog(
        mCurrentActivity: Activity,
        s: String,
        onItemClick: () -> Unit,
    ) {

        try {
            cancelDialog()
            val binding = PermissionDeniedDialogBinding.inflate(mCurrentActivity.layoutInflater)
            val alertDialogBuilder = AlertDialog.Builder(mCurrentActivity)
            alertDialogBuilder.setView(binding.root)

            binding.closeButton.setSafeOnClickListener {
                alert?.dismiss()
            }

            binding.gotoSettingButton.setSafeOnClickListener {
                alert?.dismiss()
                onItemClick()
            }

            binding.detectAppTextView.text = s


            alert = alertDialogBuilder.create()
            val back = ColorDrawable(Color.TRANSPARENT)
            val inset = InsetDrawable(back, 40)
            alert?.window?.setBackgroundDrawable(inset)
            alert?.setCanceledOnTouchOutside(true)
            if (!mCurrentActivity.isFinishing) {
                alert?.show()
            }
        } catch (e: Exception) {
            Log.d("mainTextView", e.toString())
        }
    }


    fun permissionNotificationDeniedDialog(
        mCurrentActivity: Activity,
        s: String,
        onItemClick: () -> Unit,
    ) {

        try {
            cancelDialog()
            val binding = PermissionNotificationDeniedDialogBinding.inflate(mCurrentActivity.layoutInflater)
            val alertDialogBuilder = AlertDialog.Builder(mCurrentActivity)
            alertDialogBuilder.setView(binding.root)

            binding.closeButton.setSafeOnClickListener {
                alert?.dismiss()
            }

            binding.gotoSettingButton.setSafeOnClickListener {
                alert?.dismiss()
                onItemClick()
            }

            binding.detectAppTextView.text = s


            alert = alertDialogBuilder.create()
            val back = ColorDrawable(Color.TRANSPARENT)
            val inset = InsetDrawable(back, 40)
            alert?.window?.setBackgroundDrawable(inset)
            alert?.setCanceledOnTouchOutside(true)
            if (!mCurrentActivity.isFinishing) {
                alert?.show()
            }
        } catch (e: Exception) {
            Log.d("mainTextView", e.toString())
        }
    }


}