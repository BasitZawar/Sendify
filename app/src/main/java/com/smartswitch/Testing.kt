package com.smartswitch

import android.app.Activity

import android.content.pm.ActivityInfo

import android.view.View

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun calculateSquare(number: Int): Int = suspendCoroutine { cont ->
    val result = number * number
    cont.resume(result)
}

fun main() = runBlocking {
    val result = calculateSquare(5)
    println("Square of 5 is: $result")
}



fun Activity.setupPortraitWithWindowInsets(rootViewId: Int) {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    val rootView = findViewById<View>(rootViewId)

    ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        insets
    }
}
