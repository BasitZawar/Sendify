package com.smartswitch.utils.extensions

import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun FragmentActivity.handleBackPressWithAction(onBackPressedAction: () -> Unit) {
    val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressedAction()
        }
    }
    onBackPressedDispatcher.addCallback(this, backPressCallback)
}


fun FragmentActivity.handleDoubleBackPressToExit(
    message: String,
    onExit: () -> Unit
) {
    val backPressCallback = object : OnBackPressedCallback(true) {
        private var isBackPressedOnce = false

        override fun handleOnBackPressed() {
            if (isBackPressedOnce) {
                onExit()
            } else {
                isBackPressedOnce = true
                Toast.makeText(this@handleDoubleBackPressToExit, message, Toast.LENGTH_SHORT).show()

                lifecycleScope.launch {
                    delay(2000) // Reset after 2 sec
                    isBackPressedOnce = false
                }
            }
        }
    }
    onBackPressedDispatcher.addCallback(this, backPressCallback)
}
