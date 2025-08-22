package com.smartswitch.utils.extensions

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.smartswitch.utils.service.SendOrReceiveWorker

// Extension function for Context to start SendOrReceiveWorker
fun Context.startSendOrReceiveWorker(transferMode: String) {
    // Create input data for the worker
    val inputData = Data.Builder()
        .putString("TRANSFER_MODE", transferMode) // Specify the transfer mode
        .build()

    // Create a WorkRequest
    val workRequest = OneTimeWorkRequest.Builder(SendOrReceiveWorker::class.java)
        .setInputData(inputData)
        .addTag("SEND_RECEIVE_TAG")
        .build()

    // Enqueue the work
    WorkManager.getInstance(this).enqueue(workRequest)
}


fun Context.stopSendOrReceiveWorker() {
    // Cancel all work by the tag associated with the worker
    WorkManager.getInstance(this).cancelAllWorkByTag("SEND_RECEIVE_TAG")
}