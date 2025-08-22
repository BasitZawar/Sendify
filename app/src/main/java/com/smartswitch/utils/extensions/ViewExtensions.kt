package com.smartswitch.utils.extensions

import android.os.SystemClock
import android.view.View

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.enable(){
    this.isEnabled = true
}

fun View.disable(){
    this.isEnabled = false
}

private const val DEFAULT_CLICK_INTERVAL = 300

private var lastTimeClicked: Long = 0

fun View.setSafeOnClickListener(callback: (View) -> Unit) {
    this.setOnClickListener {
        delayClick {
            callback(this)
        }
    }
}

private fun delayClick(todo: () -> Unit) {
    if (SystemClock.elapsedRealtime() - lastTimeClicked > DEFAULT_CLICK_INTERVAL) {
        lastTimeClicked = SystemClock.elapsedRealtime()
        todo()
    }
}