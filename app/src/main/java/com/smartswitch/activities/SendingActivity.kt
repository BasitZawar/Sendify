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
import com.smartswitch.domain.model.ShowFileModel
import com.smartswitch.presentation.language.BaseActivity
import com.smartswitch.presentation.sendData.SelectedData
import com.smartswitch.setupPortraitWithWindowInsets
import com.smartswitch.utils.Constant
import com.smartswitch.utils.Constant.customSystemBars
import com.smartswitch.utils.Constant.handleOnBackPress
import com.smartswitch.utils.Constant.multiContactsFileName
import com.smartswitch.utils.PaperDB
import com.smartswitch.utils.WifiDirectFragment
import com.smartswitch.utils.enums.MediaTypeEnum
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
        setupPortraitWithWindowInsets(R.id.main)

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

    /*
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
            val (contacts, otherFiles) = mArrayList.partition { it. == Contact.KEY_CONTACTS }
            Log.e("TESTTAG", "ArrayList: ${contacts.size} **  ${otherFiles.size}")
            if (contacts.size > 1) {
                val contactSingleVCF = createSingleFileForMultiContacts(contacts)!!
                if (contactSingleVCF != null) {
                    mArrayList.add(
                        MediaInfoModel(
                            multiContactsFileName, contactSingleVCF.absolutePath, "contacts", "$"
                        )
                    )
                }
                mArrayList.addAll(otherFiles)
            } else mArrayList.addAll(combinedList)

            Log.e("TESTTAG", "ArrayList: ${mArrayList.size} ** ${mArrayList.get(0).uri}")

            //latest sending code
    //        sendDataUsingJobsOverallPercentage()
            sendDataUsingJobs()*/
    /*
                   val transfer = TransferData()
                   transfer.start()
            *//*

        binding.cancel.setOnClickListener { onBackPressed() }
    }
*/
    private fun getFilesAndTransfer() {
        val sharedPreferences = getSharedPreferences(Constant.preferencefName, MODE_PRIVATE)
        val json = sharedPreferences.getString("selectedDataList", null)
        if (json == null) {
            Log.e("TESTTAG", "No data found in SharedPreferences")
            return
        }
        val gson = Gson()
        val type = object : TypeToken<SelectedData>() {}.type
        val selectedData: SelectedData = gson.fromJson(json, type)

        val mediaList = selectedData.mediaList ?: emptyList()
        val contactList = selectedData.contactList ?: emptyList()
        if (mArrayList == null) {
            mArrayList = ArrayList()
        }
        val cleanMediaList = mediaList
            .filterNotNull()
            .filter { it.mediaType != MediaTypeEnum.CONTACTS }

        Log.e(TAG, "ArrayList: ${cleanMediaList.size} **  ${contactList.size}")
        Log.d(TAG, "Retrieved Media List: ${mediaList.size}, Contacts: ${contactList.size}")
        Log.d(TAG, "Retrieved Media List: ${mediaList.size}, Contacts: ${contactList}")
        if (contactList.size > 1) {
            Log.e(TAG, "getFilesAndTransfer: 1")
            val contactSingleVCF = createSingleFileForMultiContacts(contactList)!!
            if (contactSingleVCF != null) {
                Log.e(TAG, "getFilesAndTransfer: 2")
                mArrayList.add(
                    MediaInfoModel(
                        multiContactsFileName,              // File name
                        contactSingleVCF.absolutePath,      // File path
                        contactSingleVCF.length(),                         // Type
                        0,
                        0,
                        "",
                        "",
                        0,
                        "",
                        "",
                        null,
                        MediaTypeEnum.CONTACTS,
                        false,
                        false,
                        false// Dummy size or placeholder
                    )
                )
            } else {
                Log.e(TAG, "getFilesAndTransfer: 3")
            }
        } else if (contactList.size == 1) {
            val singleContact = contactList[0]
            if (singleContact != null) {
                val vcfFile = createVcfFileForSingleContact(singleContact)
                if (vcfFile != null) {
                    mArrayList.add(
                        MediaInfoModel(
                            name = vcfFile.name ?: "Contact",
                            uri = vcfFile.absolutePath ?: "",     // File path
                            vcfFile.length(),                         // Type
                            0,
                            0,
                            "",
                            "",
                            0,
                            singleContact.contactNumber,
                            "",
                            null,
                            MediaTypeEnum.CONTACTS,
                            false,
                            false,
                        )
                    )
                }
            }
        }

        mArrayList.addAll(cleanMediaList)


        Log.e(
            TAG,
            "sendDataUsingJobs: list size ${mArrayList.size}"
        )
        if (mArrayList.isNotEmpty()) {
            Log.e(TAG, "sendDataUsingJobs mediaType: ${mArrayList[0].mediaType}")
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
        } else {
            Log.e(TAG, "sendDataUsingJobs: mArrayList is EMPTY")
        }
        sendDataUsingJobs()
        binding.cancel.setOnClickListener { onBackPressed() }
    }

    private fun createSingleFileForMultiContacts(contacts: List<MediaInfoModel?>): File? {
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

                val name = item?.name
                val number = item?.contactNumber

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
            fw.close()
            /*fileModelList.add(
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
        if (binding.cancel.text == "Complete" || binding.cancel.text == "Go Back") {
            socket?.let {
                if (it.isConnected) {
                    it.close()
                }
            }
            finish()
        } else {
            handleOnBackPress(this, "Press again to cancel Sending") {
                socket?.let {
                    if (it.isConnected) {
                        it.close()
                    }
                }
                finish()
            }
        }
    }

    private fun createVcfFileForSingleContact(contact: MediaInfoModel): File? {
        return try {
            val vcfDirectory = File(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                ".${getString(R.string.app_name)}"
            )
            if (!vcfDirectory.exists()) {
                vcfDirectory.mkdirs()
            }

            // File name = contactName.vcf (fallback if null)
            val fileName = (contact.name ?: "Contact") + ".vcf"
            val vcfFile = File(vcfDirectory, fileName)

            FileWriter(vcfFile).use { fw ->
                fw.write("BEGIN:VCARD\r\n")
                fw.write("VERSION:3.0\r\n")
                fw.write("FN:${contact.name ?: "Unknown"}\r\n")
                fw.write("TEL;TYPE=WORK,VOICE:${contact.contactNumber ?: ""}\r\n")
                fw.write("END:VCARD\r\n")
            }

            vcfFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    //    private fun getPackageName(path: String): String {
//        Log.e(TAG, "getPackageName: path = $path")
//        val info: PackageInfo? = this@SendingActivity.packageManager.getPackageArchiveInfo(path, 0)
//        Log.e(TAG, "getPackageName: packageInfo = $info")
//        val pkgName: String = info!!.packageName
//        Log.e(TAG, "getPackageName: pkgName = $pkgName")
//        return pkgName
//    }
    private fun getPackageName(path: String): String? {
        return try {
            Log.e(TAG, "getPackageName: path = $path")
            val info: PackageInfo? =
                this@SendingActivity.packageManager.getPackageArchiveInfo(path, 0)

            if (info != null) {
                val pkgName = info.packageName
                Log.e(TAG, "getPackageName: pkgName = $pkgName")
                pkgName
            } else {
                Log.e(TAG, "getPackageName: PackageInfo is null")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getPackageName: failed to parse package", e)
            null
        }
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
        if (mArrayList.isEmpty()) {
            Toast.makeText(this@SendingActivity, "No File Selected", Toast.LENGTH_SHORT).show()
            return
        }
        socket = getSocket()
        socket?.let { sock ->
            sendingJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.e(TAG, "sendDataUsingJobs: list size ${mArrayList.size}")
                    Log.e(TAG, "sendDataUsingJobs: list $mArrayList")

                    // Calculate total size
                    val totalSize = mArrayList.sumOf { File(it.uri).length() }
                    Log.e(TAG, "Sending totalSize: $totalSize")

                    var totalBytesTransferred: Long = 0
                    objOutputStream = ObjectOutputStream(sock.getOutputStream())
                    dataOutputStream = DataOutputStream(objOutputStream)

                    // Write header info
                    dataOutputStream?.writeInt(mArrayList.size)
                    dataOutputStream?.writeBoolean(PHONE_CLONE)
                    dataOutputStream?.writeLong(totalSize)

                    // Loop through files
                    for (i in mArrayList.indices) {
                        this@SendingActivity.runOnUiThread {
                            binding.totalFiless.text = "Total Files ${mArrayList.size}"
                        }
                        Log.e(TAG, "sendDataUsingJobs: 1")
                        try {
                            val currentItem = mArrayList[i]
                            var currentFile: File? = null
                            var fileName: String
                            Log.e(TAG, "sendDataUsingJobs: 2")
                            if (currentItem.mediaType!!.equals("CONTACTS")) {
                                val contactData =
                                    "${currentItem.name}:${currentItem.uri}".toByteArray()
                                fileName = currentItem.name ?: "Unknown Contact"
                                val fileLength = contactData.size.toLong()
                                Log.e(TAG, "sendDataUsingJobs: 3")
                                // Write meta info
                                dataOutputStream?.writeUTF("CONTACT")
                                dataOutputStream?.writeUTF(fileName)
                                dataOutputStream?.writeLong(fileLength)

                                // Write actual data
                                dataOutputStream?.write(contactData)
                                totalBytesTransferred += fileLength
                            } else if (currentItem.uri?.endsWith(".apk") == true) {
                                Log.e(TAG, "sendDataUsingJobs: 4")
                                val packageName = currentItem.uri?.let { getPackageName(it) }
                                currentFile = getApkFile(this@SendingActivity, packageName!!)
                                fileName = getAppName(packageName) + ".apk"
                            } else {
                                Log.e(TAG, "sendDataUsingJobs: 5")
                                currentFile = File(currentItem.uri)
                                fileName = currentFile.name
                            }
                            Log.e(TAG, "sendDataUsingJobs: 6")
                            if (currentFile != null) {
                                val fileLength = currentFile.length()
                                val buffer = ByteArray(1 * 1024 * 1024) // 1MB buffer

                                // Write file metadata
                                dataOutputStream?.writeUTF(currentFile.path)
                                dataOutputStream?.writeUTF(fileName)
                                dataOutputStream?.writeLong(fileLength)

                                // Write file content
                                FileInputStream(currentFile).use { fileInputStream ->
                                    var bytesRead: Int
                                    var totalBytesReadForFile: Long = 0

                                    while (totalBytesReadForFile < fileLength) {
                                        bytesRead = fileInputStream.read(buffer)
                                        if (bytesRead == -1) break
                                        totalBytesReadForFile += bytesRead
                                        totalBytesTransferred += bytesRead
                                        dataOutputStream?.write(buffer, 0, bytesRead)

                                        this@SendingActivity.runOnUiThread {
                                            val overallPercentage =
                                                (totalBytesTransferred.toDouble() / totalSize.toDouble()) * 100
                                            binding.sentSize.text =
                                                "${formatFileSize(totalBytesTransferred)} of ${
                                                    formatFileSize(
                                                        totalSize
                                                    )
                                                } Sent"
                                            binding.percentage.text =
                                                "${overallPercentage.toInt()} %"
                                            binding.circularProgressbar.max = 100
                                            binding.circularProgressbar.progress =
                                                overallPercentage.toInt()
                                        }
                                    }
                                }
                                PaperDB.addSentHistory(
                                    ShowFileModel(
                                        currentFile.path, "Sent"
                                    )
                                )
                                objOutputStream?.flush()
                            }
                        } catch (ex: Exception) {
                            this@SendingActivity.runOnUiThread {
                                binding.cancel.text = "Go Back"
                            }
                            Constant.showSnackBar(
                                this@SendingActivity,
                                "An unexpected issue occurred."
                            )
                            Log.e(TAG, "Sending Files: Exception: ${ex.localizedMessage}")

                            sendingJob?.cancel()
                            criticalDialog?.takeIf { it.isShowing }?.dismiss()
                            break
                        }
                    }

                    // Completed sending
                    if (sendingJob?.isActive == true) {
                        this@SendingActivity.runOnUiThread {
                            criticalDialog?.takeIf { it.isShowing }?.dismiss()
                            binding.cancel.text = "Complete"
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "sendDataUsingJobs: Exception: $ex")
                    this@SendingActivity.runOnUiThread {
                        Constant.showSnackBar(
                            this@SendingActivity,
                            "Connection Lost"
                        )
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
