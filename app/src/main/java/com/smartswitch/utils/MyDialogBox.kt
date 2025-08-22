package com.smartswitch.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager

// TODO : Show only feedback dialog
class MyDialogBox private constructor() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var dialogUtility: MyDialogBox? = null
        var dialog: Dialog? = null
            private set
        @SuppressLint("StaticFieldLeak")
        private var context: Activity? = null
        fun getInstance(con: Activity?): MyDialogBox? {
            context = con
            if (dialogUtility == null) {
                dialogUtility = MyDialogBox()
            }
            return dialogUtility
        }
    }

    fun setContentView(view: View?, isCancelable: Boolean, widthPercent: Float = 0.85f): MyDialogBox? {
        context?.let {
            dialog = Dialog(it).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(view!!)
                window?.apply {
                    setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT
                    )
                    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    setLayout((it.resources.displayMetrics.widthPixels * widthPercent).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                    setBackgroundDrawableResource(android.R.color.transparent)
                }
                setCancelable(isCancelable)
            }
            return dialogUtility
        }
        return null
    }

    fun setContentViewFull(view: View?, isCancelable: Boolean): MyDialogBox? {
        context?.let {
            dialog = Dialog(it)
            dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog?.setContentView(view!!)
            dialog?.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog?.setCancelable(isCancelable)
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            it.windowManager.defaultDisplay.getMetrics(displayMetrics)

            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            return dialogUtility
        }
        return null
    }

    fun setContentViewWithDismissCallBack(view: View?, isCancelable: Boolean, widthPercent: Float = 0.85f, onDismiss:() -> Unit): MyDialogBox? {
        context?.let {
            dialog = Dialog(it).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(view!!)
                window?.apply {
                    setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT
                    )
                    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    setLayout((it.resources.displayMetrics.widthPixels * widthPercent).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                    setBackgroundDrawableResource(android.R.color.transparent)
                }
                setCancelable(isCancelable)
            }

            dialog?.setOnDismissListener { dialogInterface ->
                onDismiss.invoke()
                Log.d("MyDialogBox___", "setContentView: dialog dismissed!")
            }

            return dialogUtility
        }
        return null
    }

    fun showDialog(): Dialog? {
        if (dialog != null && context?.isFinishing == false && context?.isDestroyed == false) {
            if (dialog?.isShowing == false){
                dialog?.show()
            } else {
                dismissDialog()
                showDialog()
            }
        }
        return dialog
    }

    fun isShowingDialog(): Boolean {
        return dialog != null && dialog?.isShowing!!
    }

    fun dismissDialog() {
        dialog?.let {
            if (it.isShowing && context?.isFinishing == false &&  context?.isDestroyed == false) {
                it.dismiss()
            }
        }
    }
}