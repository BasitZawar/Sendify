package com.smartswitch.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.ads.AdView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartswitch.R
import com.smartswitch.connection.SocketHandler.Companion.getSocket
import com.smartswitch.databinding.ActivitySendingBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.presentation.language.BaseActivity
import com.smartswitch.presentation.sendData.SelectedData
import com.smartswitch.utils.Constant
import com.smartswitch.utils.Constant.customSystemBars
import com.smartswitch.utils.Constant.multiContactsFileName
import com.smartswitch.utils.WifiDirectFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.ObjectOutputStream
import java.net.Socket

@Suppress("DEPRECATION")
class SendingActivity : BaseActivity() {
    val binding: ActivitySendingBinding by lazy { ActivitySendingBinding.inflate(layoutInflater) }
    private var mArrayList: ArrayList<MediaInfoModel> = ArrayList()
    private var combinedList: List<Any?> = emptyList()
    var socket: Socket? = null
    var adView: AdView? = null
    var criticalDialog: Dialog? = null
    private var sendingJob: Job? = null
    private var deviceName = ""
    private var PHONE_CLONE = false
    var user = ""
    var TAG = "TESTAG"
    private var objOutputStream: ObjectOutputStream? = null
    private var dataOutputStream: DataOutputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        customSystemBars(
            this@SendingActivity, R.color.white, R.color.white, true
        )
        deviceName = intent.getStringExtra("DeviceList").toString()
        Log.e("TAG", "onCreate:12 $deviceName")
        user = intent.getStringExtra("user").toString()
        getFilesAndTransfer()

        binding.imgBack.setOnClickListener { onBackPressed() }
        binding.deviceName.text = WifiDirectFragment.name1
        binding.user.text = user
    }

    private fun getFilesAndTransfer() {
        val sharedPreferences = getSharedPreferences(Constant.preferencefName, MODE_PRIVATE)
        val json = sharedPreferences.getString("selectedDataList", null)
        if (json != null) {
            val gson = Gson()
            val type = object : TypeToken<SelectedData>() {}.type
            val selectedData: SelectedData = gson.fromJson(json, type)
            val mediaList = selectedData.mediaList
            val contactList = selectedData.contactList
            combinedList = selectedData.mediaList + selectedData.contactList
            mArrayList = ArrayList(combinedList.filterIsInstance<MediaInfoModel>())

            Log.d("TAG", "Retrieved Media List: $mediaList and $contactList")
        }

        Log.e("TESTTAG", "List combinedList: ${combinedList.size}")
        if (mArrayList == null) {
            mArrayList = ArrayList()
        }
        // Partition the list into even and odd numbers
//        val (contacts, otherFiles) = combinedList.partition { it. == Constant.KEY_CONTACTS }
//        Log.e("TESTTAG", "ArrayList: ${contacts.size} **  ${otherFiles.size}")
//        if (contacts.size > 1) {
//            val contactSingleVCF = createSingleFileForMultiContacts(contacts)!!
//            if (contactSingleVCF != null) {
//                mArrayList.add(
//                    MediaInfoModel(
//                        multiContactsFileName, contactSingleVCF.absolutePath, "contacts", "$"
//                    )
//                )
//            }
//            mArrayList.addAll(otherFiles)
//        } else mArrayList.addAll(combinedList)

        Log.e("TESTTAG", "ArrayList: ${mArrayList.size} ** ${mArrayList.get(0).uri}")

        //latest sending code
//        sendDataUsingJobsOverallPercentage()
        sendDataUsingJobs()/*
               val transfer = TransferData()
               transfer.start()
        */
        binding.cancel.setOnClickListener { onBackPressed() }
    }

    private fun createSingleFileForMultiContacts(contacts: List<MediaInfoModel>): File? {
        try {
            val vdfDirectory = File(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + ".${
                    getString(
                        R.string.app_name
                    )
                }"
            )
            if (!vdfDirectory.exists()) {
                vdfDirectory.mkdirs()
            }
            val vcfFile = File(
                vdfDirectory, multiContactsFileName
            )
            var fw: FileWriter? = null
            fw = FileWriter(vcfFile)
            for (item in contacts) {/*val lines = item.split("\n")
                val name = lines[0]
                val number = lines[1].substringBefore(".vcf")*/

                val name = item.name
                val number = item.contactNumber

                fw.write("BEGIN:VCARD\r\n")
                fw.write("VERSION:3.0\r\n")
                fw.write("FN:${name}\r\n")

                fw.write(
                    """
            TEL;TYPE=WORK,VOICE:${number}

            """.trimIndent()
                )

                fw.write("END:VCARD\r\n")
            }
            fw.close()/*fileModelList.add(
                FilesModel(
                    "AllContacts",
                    vcfFile.absolutePath,
                    "contacts",
                    "$"
                )
            )*/

            return vcfFile
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onBackPressed() {
        if (binding.cancel.text == "Back to Home") {
            socket?.let {
                if (it.isConnected) {
                    it.close()
                }
            }
            finish()
        }
    }

    private fun getPackageName(path: String): String {
        Log.e(TAG, "getPackageName: path = $path")
        val info: PackageInfo? = this@SendingActivity.packageManager.getPackageArchiveInfo(path, 0)
        Log.e(TAG, "getPackageName: packageInfo = $info")
        val pkgName: String = info!!.packageName
        Log.e(TAG, "getPackageName: pkgName = $pkgName")
        return pkgName
    }

    private fun getAppName(packageName: String): String {
        val ai: ApplicationInfo?
        ai = try {
            this@SendingActivity.packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        val applicationName =
            (if (ai != null) this@SendingActivity.packageManager.getApplicationLabel(ai) else packageName) as String
        return applicationName
    }

    @SuppressLint("SetTextI18n")
    private fun sendDataUsingJobs() {
        if (mArrayList.size <= 0) {
            Toast.makeText(this@SendingActivity, "No File Selected", Toast.LENGTH_SHORT).show()
        } else {
            socket = getSocket()
            socket?.let {
                sendingJob = CoroutineScope(Dispatchers.IO).launch {
                    if (it == null) {
                        return@launch
                    } else {
                        Log.e(TAG, "sendDataUsingJobs uri: ${mArrayList[0].uri}")
                        Log.e(TAG, "sendDataUsingJobs apkPath: ${mArrayList[0].apkPath}")
                        Log.e(TAG, "sendDataUsingJobs isSend: ${mArrayList[0].isSend}")
                        Log.e(TAG, "sendDataUsingJobs size: ${mArrayList[0].size}")
                        Log.e(TAG, "sendDataUsingJobs name: ${mArrayList[0].name}")
                        Log.e(TAG, "sendDataUsingJobs appIcon: ${mArrayList[0].appIcon}")
                        Log.e(TAG, "sendDataUsingJobs appPackage: ${mArrayList[0].appPackage}")
                        Log.e(TAG, "sendDataUsingJobs date: ${mArrayList[0].date}")
                        Log.e(TAG, "sendDataUsingJobs dateTime: ${mArrayList[0].dateTime}")
                        Log.e(TAG, "sendDataUsingJobs mediaType: ${mArrayList[0].mediaType}")
                        try {
                            // Calculate the total size of all files
                            val totalSize = mArrayList.sumOf { File(it.uri).length() }
                            Log.e("TESTAG", "Sending totalSize: ${totalSize}")
                            var totalBytesTransferred: Long = 0
                            Log.d(TAG, "run: try 1st")
                            objOutputStream = ObjectOutputStream(it.getOutputStream())
                            dataOutputStream = DataOutputStream(objOutputStream)
                            dataOutputStream?.writeInt(mArrayList.size)
                            dataOutputStream?.writeBoolean(PHONE_CLONE)
                            dataOutputStream?.writeLong(totalSize)
                            for (i in 0 until mArrayList.size) {
                                this@SendingActivity.runOnUiThread {
                                    binding.totalFiless.text =
                                        "Total Files ${mArrayList.size}"
                                }
                                try {
                                    var currentFile: File
                                    var fileName: String
                                    Log.e("TESTAG", "Sending totalSize: $totalSize")
                                    if (mArrayList[i].uri!!.endsWith(".apk")) {
                                        val packageName = mArrayList[i].uri?.let { it1 ->
                                            getPackageName(
                                                it1
                                            )
                                        }
                                        currentFile =
                                            getApkFile(this@SendingActivity, packageName!!)
                                        fileName =
                                            packageName?.let { it1 -> getAppName(it1) } + ".apk"
                                    } else {
                                        currentFile = File(mArrayList[i].uri)
                                        fileName = currentFile.name
                                    }
                                    val fileLength = currentFile.length()
                                    val buffer = ByteArray(1 * 1024 * 1024) // 1MB buffer
                                    dataOutputStream?.writeUTF(currentFile.path)
                                    dataOutputStream?.writeUTF(fileName)
                                    dataOutputStream?.writeLong(fileLength)
                                    val fileInputStream = FileInputStream(currentFile)
                                    var bytesRead: Int
                                    var totalBytesReadForFile: Long = 0
                                    while (totalBytesReadForFile < fileLength) {
                                        bytesRead = fileInputStream.read(buffer)
                                        if (bytesRead == -1) break
                                        totalBytesReadForFile += bytesRead
                                        totalBytesTransferred += bytesRead
                                        dataOutputStream?.write(buffer, 0, bytesRead)
                                        this@SendingActivity.runOnUiThread {
                                            // Update the overall progress
                                            val overallPercentage =
                                                (totalBytesTransferred.toDouble() / totalSize.toDouble()) * 100
                                            val sendingPercentage =
                                                formatFileSize(totalBytesTransferred)
                                            binding.sentSize.text =
                                                "$sendingPercentage of ${formatFileSize(totalSize)} Sent"
                                            Log.e(
                                                TAG,
                                                "sendDataUsingJobs: Overall Progress = $overallPercentage%",
                                            )
                                            binding.percentage.text =
                                                "${overallPercentage.toInt()} %"
                                            binding.circularProgressbar.max = 100
                                            binding.circularProgressbar.progress =
                                                overallPercentage.toInt()
                                            binding.cancel.text = "Back to Home"
                                            binding.title.text = "Complete"
                                        }
                                    }
                                    /*
                                     set database for history later
                                     PaperDB.addSentHistory(
                                           ShowFileModel(
                                               currentFile.path, "Sent"
                                           )
                                       )*/
                                    // Check for cancellation signal
//                                    if (dataOutputStream.available() > 0) {
//                                        val signal = dataInputStream.readUTF()
//                                        if (signal == "CANCEL") {
//                                            handleCancellation()
//                                            break
//                                        }
//                                    }
                                    fileInputStream.close()
                                    objOutputStream?.flush()
                                } catch (ex: Exception) {
                                    this@SendingActivity.runOnUiThread {
//                                        showCancelDialog(this@SendActivity, socket)
                                        binding.cancel.text = "Back to Home"
                                    }
                                    Constant.showSnackBar(
                                        this@SendingActivity,
                                        "An unexpected issue occurred.",
                                    )
                                    Log.e(
                                        "TAG", "Sending Files: Exception1: ${ex.localizedMessage}"
                                    )
                                    if (sendingJob!!.isActive && !sendingJob!!.isCompleted) {
                                        sendingJob!!.cancel()
                                    }
                                    if (criticalDialog != null && criticalDialog!!.isShowing) {
                                        criticalDialog!!.dismiss()
                                    }
                                    break
                                }
                            }
                            if (sendingJob!!.isActive) {
                                this@SendingActivity.runOnUiThread {
                                    if (criticalDialog != null && criticalDialog?.isShowing == true) {
                                        criticalDialog?.dismiss()
                                    }
                                    binding.cancel.text == "Back to Home"
                                }
                            }
                        } catch (ex: Exception) {
                            Log.e(TAG, "run: Exception 3:$ex")
//                            AppConstants.showSnackBar(
//                                this@SendActivity,
//                                "Connection Lost"
//                            )
                        }
                    }
                }
            }
            sendingJob?.start()
        }
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

    override fun onDestroy() {
        adView?.let {
            it.destroy()
            val parent = it.parent as ViewGroup?
            parent?.removeView(it)
        }
        super.onDestroy()
    }

    private fun getApkFile(context: Context, packageName: String): File {
        try {
            return File(
                context.packageManager.getApplicationInfo(
                    packageName, 0
                ).sourceDir
            )
        } catch (ex: Exception) {

        }
        return File("")
    }
}
