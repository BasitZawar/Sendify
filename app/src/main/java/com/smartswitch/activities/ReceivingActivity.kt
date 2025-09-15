package com.smartswitch.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import com.smartswitch.R
import com.smartswitch.connection.SocketHandler
import com.smartswitch.databinding.ActivityReceivingBinding
import com.smartswitch.domain.model.ShowFileModel
import com.smartswitch.presentation.language.BaseActivity
import com.smartswitch.setupPortraitWithWindowInsets
import com.smartswitch.utils.Constant
import com.smartswitch.utils.Constant.customSystemBars
import com.smartswitch.utils.Constant.handleOnBackPress
import com.smartswitch.utils.Constant.multiContactsFileName
import com.smartswitch.utils.Constant.requestStoragePermission11
import com.smartswitch.utils.Constant.showSnackBar
import com.smartswitch.utils.PaperDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.net.Socket
import java.util.Locale

@Suppress("DEPRECATION")
class ReceivingActivity : BaseActivity() {
    val binding: ActivityReceivingBinding by lazy { ActivityReceivingBinding.inflate(layoutInflater) }
    var criticalDialog: Dialog? = null
    var socket: Socket? = null
    lateinit var coroutineJob: Job
    var deviceName = ""
    var PHONE_CLONE = false
    var user = ""
    private lateinit var manageAllFilesPermissionLauncher: ActivityResultLauncher<Intent>
    private var totalBytesReceived: Long = 0 // Track the total bytes received

    val IMAGES_EXTENSIONS = listOf("jpg", "png", "gif", "tiff", "jpeg", "webp")
    val VIDEOS_EXTENSIONS = listOf("mp4", "3gp", "mov", "wmv", "avi", "mkv")
    val AUDIOS_EXTENSIONS = listOf("mp3", "opus", "raw", "wav", "ogg", "m4a")
    val FILES_EXTENSIONS = listOf(
        "doc",
        "docx",
        "html",
        "htm",
        "odt",
        "pdf",
        "xls",
        "xlsx",
        "ods",
        "ppt",
        "pptx",
        "txt",
        "zip"
    )
    val APPS_EXTENSIONS = listOf("apk")
    val CONTACT_EXTENSIONS = listOf("vcf")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupPortraitWithWindowInsets(R.id.main)
        customSystemBars(
            this@ReceivingActivity, R.color.white, R.color.white, true
        )
        deviceName = intent.getStringExtra("DeviceList").toString()
        Log.e("TAG", "onCreate:21 $deviceName")
        binding.deviceName.text = deviceName
        user = intent.getStringExtra("user").toString()

        binding.btnGrant.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    requestStoragePermission11(this, manageAllFilesPermissionLauncher)
                }
            } else {
                if (!Constant.checkPermission(this)) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ), 100
                    )
//                    requestStoragePermission11(this, manageAllFilesPermissionLauncher)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                binding.finalLayout.visibility = View.VISIBLE
                binding.layoutBeforePermission.visibility = View.GONE
                receivingUsingJob()
            } else {
                binding.finalLayout.visibility = View.GONE
                binding.layoutBeforePermission.visibility = View.VISIBLE
            }
        } else {
            if (Constant.checkPermission(this@ReceivingActivity)) {
                binding.finalLayout.visibility = View.VISIBLE
                binding.layoutBeforePermission.visibility = View.GONE
                receivingUsingJob()
            } else {
                binding.finalLayout.visibility = View.GONE
                binding.layoutBeforePermission.visibility = View.VISIBLE
            }
        }

        manageAllFilesPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    binding.finalLayout.visibility = View.VISIBLE
                    binding.layoutBeforePermission.visibility = View.GONE
                    receivingUsingJob()
                } else {
                }
            }
        }
        binding.imgBack.setOnClickListener { onBackPressed() }
        binding.user.text = user
        Log.e("TAG", "Device Name Receiver: ${binding.deviceName.text}")
        binding.cancel.setOnClickListener { onBackPressed() }
    }

    @SuppressLint("SetTextI18n")
    private fun receivingUsingJob() {
        socket = SocketHandler.getSocket()
        coroutineJob = CoroutineScope(Dispatchers.IO).launch {
            socket?.let {
                if (it == null) {
                    return@launch
                } else {
                    try {
                        if (it != null) {
                            try {
                                val objInputStream = ObjectInputStream(it.getInputStream())
                                val dataInputStream = DataInputStream(objInputStream)
                                val totalFiles = dataInputStream.readInt()
                                PHONE_CLONE = dataInputStream.readBoolean()
                                val totalFileSize = dataInputStream.readLong()
                                Log.e("TESTAG", "receiving list: ${totalFiles}")
                                for (i in 0 until totalFiles) {
                                    this@ReceivingActivity.runOnUiThread {
                                        binding.totalFiless.text =
                                            "Total Files ${totalFiles}"
                                    }
                                    try {
                                        val filePath = dataInputStream.readUTF()
                                        val fileName = dataInputStream.readUTF()
                                        val fileLength = dataInputStream.readLong()
                                        Log.e(
                                            "TESTAG", "receivingUsingJob filePath: $filePath"
                                        )
                                        Log.e(
                                            "TESTAG", "receivingUsingJob fileName: $fileName"
                                        )
                                        Log.e(
                                            "TESTAG", "receivingUsingJob fileLength: $fileLength"
                                        )
                                        val fileExtension = getFileExtension(fileName)
                                        when {
                                            IMAGES_EXTENSIONS.contains(
                                                fileExtension.lowercase(
                                                    Locale.ROOT
                                                )
                                            ) -> {
                                                Log.e(
                                                    "TESTAG",
                                                    "ReceiveFilesThread: IMAGES_EXTENSIONS.contains(fileExtension)"
                                                )
                                                saveFileToFolderChucks(
                                                    filePath,
                                                    fileName,
                                                    "Images",
                                                    fileLength,
                                                    i,
                                                    dataInputStream,
                                                    totalFileSize
                                                )
                                            }

                                            VIDEOS_EXTENSIONS.contains(
                                                fileExtension.lowercase(
                                                    Locale.getDefault()
                                                )
                                            ) -> {
                                                Log.e(
                                                    "TESTAG",
                                                    "ReceiveFilesThread: VIDEOS_EXTENSIONS.contains(fileExtension)"
                                                )
                                                saveFileToFolderChucks(
                                                    filePath,
                                                    fileName,
                                                    "Videos",
                                                    fileLength,
                                                    i,
                                                    dataInputStream,
                                                    totalFileSize
                                                )
                                            }

                                            AUDIOS_EXTENSIONS.contains(fileExtension.lowercase()) -> {
                                                Log.e(
                                                    "TESTAG",
                                                    "ReceiveFilesThread: AUDIOS_EXTENSIONS.contains(fileExtension)"
                                                )
                                                saveFileToFolderChucks(
                                                    filePath,
                                                    fileName,
                                                    "Audios",
                                                    fileLength,
                                                    i,
                                                    dataInputStream,
                                                    totalFileSize
                                                )
                                            }

                                            FILES_EXTENSIONS.contains(
                                                fileExtension.lowercase(
                                                    Locale.getDefault()
                                                )
                                            ) -> {
                                                Log.e(
                                                    "TESTAG",
                                                    "ReceiveFilesThread: FILES_EXTENSIONS.contains(fileExtension)"
                                                )
                                                if (fileName == multiContactsFileName) {
                                                    saveFileToFolderChucksMultiContacts(
                                                        fileName,
                                                        fileLength,
                                                        dataInputStream,
                                                        totalFileSize
                                                    )
                                                } else
                                                    saveFileToFolderChucks(
                                                        filePath,
                                                        fileName,
                                                        "Documents",
                                                        fileLength,
                                                        i,
                                                        dataInputStream,
                                                        totalFileSize
                                                    )
                                            }

                                            APPS_EXTENSIONS.contains(fileExtension.lowercase()) -> {
                                                Log.e(
                                                    "TESTAG",
                                                    "ReceiveFilesThread: APPS_EXTENSIONS.contains(fileExtension)"
                                                )
                                                saveFileToFolderChucks(
                                                    filePath,
                                                    fileName,
                                                    "Apps",
                                                    fileLength,
                                                    i,
                                                    dataInputStream,
                                                    totalFileSize
                                                )
                                            }

                                            CONTACT_EXTENSIONS.contains(fileExtension.lowercase()) -> {
                                                Log.e(
                                                    "TESTAG",
                                                    "receivingUsingJob filePath: $filePath"
                                                )
                                                saveVcfFileToFolder(
                                                    fileName,
                                                    "Contacts",
                                                    fileLength,
                                                    dataInputStream,
                                                    totalFileSize
                                                )
                                            }

                                            else -> {
                                                Log.e("TESTAG", "ReceiveFilesThread: else 2nd")
                                            }
                                        }
//                                        this@ReceiverActivity.runOnUiThread {
//                                            val percentage =
//                                                ((i + 1).toFloat() / totalFiles.toFloat()) * 100
//                                            binding.percentage.text = "${percentage.toInt()} %"
//                                            binding.circularProgressbar.max = totalFiles
//                                            binding.circularProgressbar.progress = i + 1
//                                        }
                                        this@ReceivingActivity.sendBroadcast(
                                            Intent(
                                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                                Uri.fromFile(File(filePath))
                                            )
                                        )
                                    } catch (ex: Exception) {
                                        this@ReceivingActivity.runOnUiThread {
//                                            showCancelDialog(this@ReceiverActivity, socket)
                                            binding.cancel.text = "Go Back"
                                        }
                                        showSnackBar(
                                            this@ReceivingActivity,
                                            "An unexpected issue occurred.",
                                        )
                                        Log.e(
                                            "TESTAG",
                                            "ReceiveFilesThread: Exception1: ${ex.localizedMessage}"
                                        )
                                        if (this@ReceivingActivity::coroutineJob.isInitialized && coroutineJob.isActive && !coroutineJob.isCompleted) {
                                            coroutineJob.cancel()
                                            if (criticalDialog != null && criticalDialog!!.isShowing && !isFinishing && !isDestroyed) {
                                                criticalDialog!!.dismiss()
                                            }
                                        }
                                        break
                                    } finally {
                                        try {
                                        } catch (e: Exception) {
                                            this@ReceivingActivity.runOnUiThread {
//                                                showCancelDialog(this@ReceiverActivity, socket)
                                                binding.cancel.text = "Go Back"
                                            }
                                            showSnackBar(
                                                this@ReceivingActivity,
                                                "An unexpected issue occurred.",
                                            )
                                            Log.e(
                                                "TESTAG",
                                                "receivingUsingJob: Exception 2 : ${e.message}"
                                            )
                                        }
                                    }
                                }
                                if (coroutineJob.isActive) {
                                    this@ReceivingActivity.runOnUiThread {
                                        binding.cancel.text = "Complete"
                                        binding.title.text = "Complete"
                                        if (criticalDialog != null && criticalDialog!!.isShowing && !isFinishing && !isDestroyed) {
                                            criticalDialog!!.dismiss()
                                        }
                                    }
                                }
                            } catch (ex: Exception) {
                                this@ReceivingActivity.runOnUiThread {
                                    binding.cancel.text = "Go Back"
                                }
                                showSnackBar(
                                    this@ReceivingActivity,
                                    "An unexpected issue occurred.",
                                )
                                Log.e(
                                    "TESTAG",
                                    "ReceiveFilesThread: Exception2: ${ex.localizedMessage}"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        this@ReceivingActivity.runOnUiThread {
//                            showCancelDialog(this@ReceiverActivity, socket)
                            binding.cancel.text = "Go Back"
                        }
                        showSnackBar(
                            this@ReceivingActivity,
                            "An unexpected issue occurred.",
                        )
                        Log.e(
                            "TESTAG", "ReceiveFilesThread: Exception3: ${e.localizedMessage}"
                        )
                    }
                }
            }
        }
        coroutineJob.start()
    }

    @SuppressLint("SetTextI18n")
    private fun saveFileToFolderChucks(
        filePath: String,
        fileName: String,
        folderName: String,
        fileLength: Long,
        index: Int,
        dataInputStream: DataInputStream,
        totalSize: Long
    ) {
        val folder = File(
            Environment.getExternalStorageDirectory(), "Phone Clone /$folderName"
        )
        if (!folder.exists()) {
            folder.mkdirs()
            MediaScannerConnection.scanFile(
                this@ReceivingActivity?.applicationContext, arrayOf(folder.absolutePath), null
            ) { path, uri ->
                Log.d("MediaScanner", "Scanned file $path")
            }
        }
        var (baseName, extension) = fileName.split(".", limit = 2)
        var newFileName = "$baseName"
        var count = 1
        while (File(folder, "$newFileName.$extension").exists()) {
            newFileName = "${baseName}_${count++}"
        }
        val fileOutputStream = FileOutputStream("$folder/$newFileName.$extension")
        val buffer = ByteArray(1 * 1024 * 1024) // 1MB buffer
        var bytesRead: Int
        var totalBytesReadForFile: Long = 0
        while (totalBytesReadForFile < fileLength) {
            val remainingBytes = fileLength - totalBytesReadForFile
            val bytesToRead =
                if (remainingBytes > buffer.size) buffer.size else remainingBytes.toInt()
            bytesRead = dataInputStream.read(buffer, 0, bytesToRead)
            if (bytesRead == -1) break
            totalBytesReadForFile += bytesRead
            totalBytesReceived += bytesRead // Update total bytes received for all files
            fileOutputStream.write(buffer, 0, bytesRead)
            // Update the overall progress
            val overallPercentage = (totalBytesReceived.toDouble() / totalSize.toDouble()) * 100
            val receivingSize = formatFileSize(totalBytesReceived)
            val totalSizeFormatted = formatFileSize(totalSize)
            this@ReceivingActivity.runOnUiThread {
                binding.receivedSize.text = "$receivingSize of $totalSizeFormatted Received"
                binding.percentage.text = "${overallPercentage.toInt()} %"
                binding.circularProgressbar.max = 100
                binding.circularProgressbar.progress = overallPercentage.toInt()
            }
        }
        fileOutputStream.close()
        val file = File(folder, "$newFileName.$extension")
        Log.e("TESTAG", "Received file.absolutePath: ${file.absolutePath}")

        PaperDB.addReceiveHistory(
            ShowFileModel(
                file.path, "Received"
            )
        )

        val filePathsToScan = listOf(file.absolutePath)
        Log.d("MediaScanner", "filePathsToScan $filePathsToScan")
        scanMediaFiles(this@ReceivingActivity, filePathsToScan)
    }

    @SuppressLint("SetTextI18n")
    private fun saveFileToFolderChucksMultiContacts(
        fileName: String,
        fileLength: Long,
        dataInputStream: DataInputStream,
        totalSize: Long
    ) {
        val folder = File(
            Environment.getExternalStorageDirectory(), "Phone Clone /Contacts"
        )
        if (!folder.exists()) {
            folder.mkdirs()
            MediaScannerConnection.scanFile(
                this@ReceivingActivity?.applicationContext, arrayOf(folder.absolutePath), null
            ) { path, uri ->
                Log.d("MediaScanner", "Scanned file $path")
            }
        }
        var (baseName, extension) = fileName.split(".", limit = 2)
        var newFileName = baseName
        extension = ".vcf"
        var count = 1
        while (File(folder, "$newFileName$extension").exists()) {
            newFileName = "${baseName}_${count++}"
        }
        val fileOutputStream = FileOutputStream("$folder/$newFileName$extension")
        val buffer = ByteArray(1 * 1024 * 1024) // 1MB buffer
        var bytesRead: Int
        var totalBytesReadForFile: Long = 0
        while (totalBytesReadForFile < fileLength) {
            val remainingBytes = fileLength - totalBytesReadForFile
            val bytesToRead =
                if (remainingBytes > buffer.size) buffer.size else remainingBytes.toInt()
            bytesRead = dataInputStream.read(buffer, 0, bytesToRead)
            if (bytesRead == -1) break
            totalBytesReadForFile += bytesRead
            totalBytesReceived += bytesRead // Update total bytes received for all files
            fileOutputStream.write(buffer, 0, bytesRead)
            // Update the overall progress
            val overallPercentage = (totalBytesReceived.toDouble() / totalSize.toDouble()) * 100
            val receivingSize = formatFileSize(totalBytesReceived)
            val totalSizeFormatted = formatFileSize(totalSize)
            this@ReceivingActivity.runOnUiThread {
                binding.receivedSize.text = "$receivingSize of $totalSizeFormatted Received"
                binding.percentage.text = "${overallPercentage.toInt()} %"
                binding.circularProgressbar.max = 100
                binding.circularProgressbar.progress = overallPercentage.toInt()
            }
        }
        fileOutputStream.close()
        val file = File(folder, "$newFileName$extension")
        Log.e("TESTAG", "Received file.absolutePath: ${file.absolutePath}")

         PaperDB.addReceiveHistory(
             ShowFileModel(
                 file.path, "Received"
             )
         )

        val filePathsToScan = listOf(file.absolutePath)
        Log.d("MediaScanner", "filePathsToScan $filePathsToScan")
        scanMediaFiles(this@ReceivingActivity, filePathsToScan)
    }

    @SuppressLint("SetTextI18n")
    private fun saveVcfFileToFolder(
        fileName: String,
        folderName: String,
        fileLength: Long,
        dataInputStream: DataInputStream,
        totalFileSize: Long
    ) {
        Log.e(
            "TESTAG", " fileName: $fileName"
        )
        val buffer = ByteArray(fileLength.toInt())
        dataInputStream.readFully(buffer)
        val folder = File(
            Environment.getExternalStorageDirectory(), "Phone Clone /$folderName"
        )
        if (!folder.exists()) {
            folder.mkdirs()
            MediaScannerConnection.scanFile(
                this@ReceivingActivity.applicationContext, arrayOf(folder.absolutePath), null
            ) { path, uri ->
                Log.d("MediaScanner", "Scanned file $path")
            }
            Log.e(
                "TESTAG", " FOLDER CREATED"
            )
        } else {
            Log.e(
                "TESTAG", " FOLDER CREATED ELSE"
            )
            val lines = fileName.split("\n")
            val name = lines[0]
            val number = lines[1].substringBefore(".vcf")
            Log.e("TESTAG", " number $number")
            // Create VCF content
            val vcfContent = createVcfContent(name, number)
            var newFileName = "$name.vcf"
            var count = 1
            val vcfFile = File(folder, newFileName)
            vcfFile.writeText(vcfContent)
            // Update the overall progress
            val overallPercentage = (vcfFile.length() / totalFileSize.toDouble()) * 100
            val receivingSize = formatFileSize(vcfFile.length())
            val totalSizeFormatted = formatFileSize(totalFileSize)
//            this@ReceiverActivity.runOnUiThread {
//                val receivedSize = formatFileSize(vcfFile.length())
//                val totalSize = formatFileSize(vcfFile.length())
////                binding.receivedSize.text = receivedSize
////                binding.totalSize.text = "from: $totalSize"
//            }
            this@ReceivingActivity.runOnUiThread {
                binding.receivedSize.text = "$receivingSize of $totalSizeFormatted Received"
                binding.percentage.text = "${overallPercentage.toInt()} %"
                binding.circularProgressbar.max = 100
                binding.circularProgressbar.progress = overallPercentage.toInt()
            }
            Log.e("TESTAG", "Received VCF file.absolutePath: ${vcfFile.absolutePath}")
            val filePathsToScan = listOf(vcfFile.absolutePath)
            MediaScannerConnection.scanFile(
                this@ReceivingActivity?.applicationContext, arrayOf(vcfFile.absolutePath), null
            ) { path, uri ->
                Log.d("MediaScanner", "Scanned file $path")
            }

            PaperDB.addReceiveHistory(
                ShowFileModel(
                    vcfFile.path, "Received"
                )
            )
        }
    }

    private fun createVcfContent(name: String, phoneNumber: String): String {
        return """
        BEGIN:VCARD
        VERSION:3.0
        FN:$name
        TEL;TYPE=CELL:$phoneNumber
        END:VCARD
    """.trimIndent()
    }

    @SuppressLint("DefaultLocale")
    private fun formatFileSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var fileSize = size.toDouble()
        var unitIndex = 0
        while (fileSize > 1024 && unitIndex < units.size - 1) {
            fileSize /= 1024
            unitIndex++
        }
        return String.format("%.2f %s", fileSize, units[unitIndex])
    }

    private fun scanMediaFiles(context: Context, filePaths: List<String>) {
        MediaScannerConnection.scanFile(
            context.applicationContext, filePaths.toTypedArray(), null
        ) { path, uri ->
            Log.d("MediaScanner", "Scanned file $path")
        }
    }

    private fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex != -1) {
            fileName.substring(lastDotIndex + 1)
        } else {
            ""
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                if (binding.layoutProgress.isVisible) {
                    if (binding.cancel.text == "Complete" || binding.cancel.text == "Go Back") {
                        socket?.let {
                            if (it.isConnected) {
                                it.close()
                            }
                        }
                        finish()
                    }else {
                        handleOnBackPress(this,"Press again to cancel Receiving"){
                            socket?.let {
                                if (it.isConnected) {
                                    it.close()
                                }
                            }
                            finish()
                        }
                    }
                }
            } else {
                finish()
            }
        } else {
            if (Constant.checkPermission(this)) {
                if (binding.layoutProgress.isVisible) {
                    if (binding.cancel.text == "Complete" || binding.cancel.text == "Go Back") {
                        socket?.let {
                            if (it.isConnected) {
                                it.close()
                            }
                        }
                        finish()
                    } else {
                        handleOnBackPress(this,"Press again to cancel Receiving"){
                            socket?.let {
                                if (it.isConnected) {
                                    it.close()
                                }
                            }
                            finish()
                        }
                    }
                }
            } else {
                finish()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Environment.isExternalStorageManager()) {
            binding.finalLayout.visibility = View.VISIBLE
            binding.layoutBeforePermission.visibility = View.GONE
        } else {
            binding.finalLayout.visibility = View.GONE
            binding.layoutBeforePermission.visibility = View.VISIBLE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                binding.finalLayout.visibility = View.VISIBLE
                binding.layoutBeforePermission.visibility = View.GONE
                receivingUsingJob()
            } else {
//                // Permission denied
                binding.finalLayout.visibility = View.GONE
                binding.layoutBeforePermission.visibility = View.VISIBLE
                Constant.showSettingDialog(this)
            }
        }
    }
}
