package com.smartswitch.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Environment
import com.smartswitch.domain.model.FileMetaData
import com.smartswitch.domain.model.MediaInfoModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.net.Socket
import java.util.Locale

object FileUtils {

    fun createDirectory(fileType: String): File {
        val downloadsDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val smartSwitchDirectory = File(downloadsDirectory, "MySmartSwitch")

        if (!smartSwitchDirectory.exists()) {
            smartSwitchDirectory.mkdir()
        }
        val fileTypeDirectory = File(smartSwitchDirectory, fileType)
        if (!fileTypeDirectory.exists()) {
            fileTypeDirectory.mkdir()
        }
        return fileTypeDirectory
    }

    fun sendFileMetaData(socket: Socket?, fileMetaData: FileMetaData) {
        val outputStream = ObjectOutputStream(socket?.getOutputStream())
        outputStream.writeObject(fileMetaData)
        outputStream.flush()
    }

    fun sanitizeFileName(fileName: String?): String {
        // Define invalid characters
        val invalidChars = Regex("[<>:\"/|?*]")
        // Replace invalid characters with an underscore or any other preferred character
        return fileName?.replace(invalidChars, "_") ?: "file_${System.currentTimeMillis()}"
    }

    fun processFileName(fileName: String): String {
        // Extract the file extension
        val extension = fileName.substringAfterLast('.', "")

        // Extract the name without extension
        val nameWithoutExtension = if (extension.isNotEmpty()) {
            fileName.substringBeforeLast('.')
        } else {
            fileName
        }

        // Shorten the name to 30 characters
        val shortenedName = if (nameWithoutExtension.length > 30) {
            nameWithoutExtension.take(30)
        } else {
            nameWithoutExtension
        }

        val sanitizedName = sanitizeFileName(shortenedName)

        // Reconstruct the file name
        return if (extension.isNotEmpty()) {
            "$sanitizedName.$extension"
        } else {
            sanitizedName
        }
    }

    fun getApkIcon(apkFilePath: String?, packageManager: PackageManager): Drawable? {
        val packageInfo: PackageInfo? =
            apkFilePath?.let { packageManager.getPackageArchiveInfo(it, 0) }
        packageInfo?.applicationInfo?.apply {
            sourceDir = apkFilePath
            publicSourceDir = apkFilePath
            return packageManager.getApplicationIcon(this)
        }
        return null
    }

    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(
            Locale.getDefault(),
            "%.2f %s",
            sizeInBytes / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    fun generateVCard(contact: MediaInfoModel): String {
        return """
        BEGIN:VCARD
        VERSION:3.0
        FN:${contact.name}
        TEL:${contact.contactNumber}
        UID:${contact.contactId}
        END:VCARD
    """.trimIndent()
    }

    fun generateVcfFile(context: Context, contacts: List<MediaInfoModel?>): File? {
        val vcfStringBuilder = StringBuilder()

        // Generate VCard for each contact and append to StringBuilder
        contacts.forEach { contact ->
            val vCard = contact?.let { generateVCard(it) }
            vcfStringBuilder.append(vCard).append("\n")
        }

        // Create the "contacts" directory if it doesn't exist
        val contactsDir = File(context.getExternalFilesDir(null), "contacts")
        if (!contactsDir.exists()) {
            contactsDir.mkdirs()
        }

        // Create a VCF file
        val vcfFileName = "contacts_smart_switch_${System.currentTimeMillis()}.vcf"
        val vcfFile = File(contactsDir, vcfFileName)

        return try {
            FileOutputStream(vcfFile).use { outputStream ->
                outputStream.write(vcfStringBuilder.toString().toByteArray())
            }
            vcfFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun deleteAllFilesInContactsFolder(context: Context): Boolean {
        // Get the "contacts" directory
        val contactsDir = File(context.getExternalFilesDir(null), "contacts")

        return if (contactsDir.exists()) {
            // List all files and delete each one
            contactsDir.listFiles()?.forEach { file ->
                file.delete()
            }
            // Optionally delete the directory itself if empty
            contactsDir.delete()
        } else {
            false // Directory does not exist
        }
    }


}