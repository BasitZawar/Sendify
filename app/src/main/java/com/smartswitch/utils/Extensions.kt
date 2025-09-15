package com.smartswitch.utils

import android.content.Context
import android.widget.Toast
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

fun Long.formatLength(kilo: Boolean = true): String {
    val unit = if (kilo) 1000 else 1024
    if (this < unit) return "$this B"
    val expression = (ln(this.toDouble()) / ln(unit.toDouble())).toInt()
    val prefix = (if (kilo) "kMGTPE" else "KMGTPE")[expression - 1] + if (kilo) "" else "i"
    return String.format(
        Locale.getDefault(),
        "%.1f %sB",
        this / unit.toDouble().pow(expression.toDouble()),
        prefix
    )
}

fun Context.showMessage(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun verifyInstallerId(context: Context): Boolean {
    val validInstallers: MutableList<String> = ArrayList()
    validInstallers.add("com.android.vending")
    validInstallers.add("com.google.android.feedback")
    val installer = context.packageManager.getInstallerPackageName(context.packageName)
    return installer != null && validInstallers.contains(installer)
}