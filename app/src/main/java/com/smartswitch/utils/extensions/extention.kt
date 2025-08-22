package com.smartswitch.utils.extensions

import android.annotation.SuppressLint
import android.app.Activity
import androidx.fragment.app.Fragment
import com.smartswitch.utils.enums.DateFormatType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Fragment.isAlive(callback: (Activity) -> Unit) {
    if (activity != null && isAdded && !isDetached) {
        activity?.let { it.isActivityAlive { activity -> callback(activity) } }
    }
}
//  TODO : My Addition (1)
//fun Fragment.isAlive(callback: () -> Unit) {
//    // Check if the fragment is added and not detached, and if the activity is alive
//    if (activity != null && isAdded && !isDetached && !requireActivity().isFinishing && !requireActivity().isDestroyed) {
//        callback()
//    }
//}

//  TODO : My Addition (2)
//// Improved isAlive function to avoid redundant checks and handle fragment detachment more gracefully
//fun Fragment.isAlive(callback: (Activity) -> Unit) {
//    activity?.let { activity ->
//        if (isAdded && !isDetached && !activity.isFinishing && !activity.isDestroyed) {
//            callback(activity)
//        } else {
//            // Optionally log or handle this case if needed
//            // Log.e("Fragment", "Activity or Fragment not in a valid state")
//        }
//    }
//}


fun Activity.isActivityAlive(callback: (Activity) -> Unit) {
    try {
        if (isFinishing.not() &&
            isDestroyed.not()
        ) {
            callback(this)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


@SuppressLint("DefaultLocale")
fun Long.formatFileSize(): String {
    return when {
        this < 1024 -> "$this Bytes"
        this < 1048576 -> String.format("%.2f KB", this / 1024.0)
        this < 1073741824 -> String.format("%.2f MB", this / 1048576.0)
        this < 1099511627776 -> String.format("%.2f GB", this / 1073741824.0)
        else -> String.format("%.2f TB", this / 1099511627776.0)
    }
}




// Extension function for Date to format based on DateFormatType


// Extension function for Long to format based on DateFormatType
fun Long.formatTo(type: DateFormatType, customFormat: String? = null): String {
    val dateFormat = when (type) {
        DateFormatType.CUSTOM -> SimpleDateFormat(customFormat ?: "yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        else -> SimpleDateFormat(type.format, Locale.getDefault())
    }
    return dateFormat.format(Date(this))
}
