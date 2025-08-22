package com.smartswitch.utils.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

fun Context.openFileFromRecyclerView(fileUri: String) {
    // Path to your file
    val file = File(fileUri)

    if (file.exists()) {
        // Use FileProvider to get a content URI
        val contentUri: Uri = FileProvider.getUriForFile(
            this,
            "${this.packageName}.fileprovider",
            file
        )

        // Get the MIME type based on the file extension
        val mimeType = when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "txt" -> "text/plain"
            "html" -> "text/html"
            "zip" -> "application/zip"
            "jpg", "jpeg", "png", "gif" -> "image/*"
            "mp4", "mkv", "avi" -> "video/*"
            "mp3", "wav" -> "audio/*"
            "apk" -> "application/vnd.android.package-archive"
            else -> "*/*" // Default to a generic MIME type
        }

        Log.d("FileViewer___", "openFileFromRecyclerView: MIME type: $mimeType")

        val intent = if (file.extension.lowercase() == "apk") {
            // Special handling for APK files
            Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = contentUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to read the file
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true) // Allow unknown source
                putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, this@openFileFromRecyclerView.packageName)
            }
        } else {
            // General handling for other file types
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType) // Use the determined MIME type
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to read the file
            }
        }

        // Start the activity with the intent
        startActivity(Intent.createChooser(intent, "Open file with"))
    } else {
        Log.d("FileViewer___", "File not found: $fileUri")
    }
}
