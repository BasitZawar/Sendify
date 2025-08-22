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


fun FragmentActivity.handleDoubleBackPressToExit(onExit: () -> Unit) {
    var isBackPressedOnce = false
    val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isBackPressedOnce) {
                onExit()
            } else {
                isBackPressedOnce = true
                Toast.makeText(this@handleDoubleBackPressToExit, "Tap again to exit", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch {
                    delay(2000) // Resets the flag after 2 seconds
                    isBackPressedOnce = false
                }
            }
        }
    }
    onBackPressedDispatcher.addCallback(this, backPressCallback)
}
