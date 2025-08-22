package com.smartswitch.utils.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.net.wifi.p2p.WifiP2pManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.smartswitch.utils.enums.TransferState
import com.smartswitch.R
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.model.TransferInfoModel
import com.smartswitch.domain.repository.MediaHistoryRepository
import com.smartswitch.presentation.MainActivity
import com.smartswitch.presentation.database.AppDatabase
import com.smartswitch.presentation.database.ZMediaHistoryEntity
import com.smartswitch.utils.FileUtils
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.TransferStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.StreamCorruptedException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

class SendOrReceiveService : Service() {
    private var binder: LocalBinder? = LocalBinder()
    var wakeLock: PowerManager.WakeLock? = null
    val BUFFER_SIZE = 131072
    val currentDateTime = System.currentTimeMillis()
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val formattedDate = dateFormatter.format(currentDateTime)


    /**
     * Binder class that clients use to access the service's public methods.
     * This allows components (like activities) to bind to the service and call its functions directly.
     * */
    inner class LocalBinder : Binder() {
        fun getService(): SendOrReceiveService = this@SendOrReceiveService
    }

    /**
     * This method is called when a component binds to the service.
     * It returns an instance of the LocalBinder so the client can access the service.
     * */
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }


    var socket: Socket? = null
    var job: Job? = null
    var receiverjob: Job? = null
    var canceljob: Job? = null
    var wifiP2pManager: WifiP2pManager? = null
    var wifiChannel: WifiP2pManager.Channel? = null
    var PORT_NUMBER = 6062

    companion object {
        private const val COMPLETE_MESSAGE = "COMPLETE"
        private const val CANCEL_MESSAGE = "CANCEL"
        private const val TAG = "service"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        //createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        acquireWakeLock()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("servicee", "onStartCommand: ")
        Log.d("awaisshakeel", "onStartCommand")
        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wifiChannel = wifiP2pManager?.initialize(this, this.mainLooper, null)
        val notification = createNotification()
        //startForeground(100, notification)

        try {
            // Your existing foreground service setup
            startForeground(100, notification)
        } catch (e: ForegroundServiceStartNotAllowedException) {
            // Fallback to regular service or notify user
            Log.e("123456", "Foreground service not allowed", e)
            stopSelf()
            return START_NOT_STICKY
        }
        //return super.onStartCommand(intent, flags, startId)

        return START_STICKY
    }


    fun startSendingToReceiver() {
        isStop = false
        job?.cancel()
        var totalBytesRead = 0L


        wifiP2pManager?.requestConnectionInfo(wifiChannel) { connectionInfo ->

            job = CoroutineScope(Dispatchers.IO).launch {

                try {
                    val hostAddress = connectionInfo.groupOwnerAddress
                    Log.i("startSendingToReceiver", "Resolved receiver host address: $hostAddress")
                    Log.i("startSendingToReceiver", "Resolved receiver Port Number: $PORT_NUMBER")
                    Log.i(
                        "startSendingToReceiver",
                        "Resolved receiver host address: $connectionInfo"
                    )
                    Log.i("startSendingToReceiver", "Resolved receiver host address: $wifiChannel")

                    // Initialize socket connection
//                    socket = Socket().apply {
//                        bind(null)
//                        connect(InetSocketAddress(hostAddress, PORT_NUMBER), 30000)
//                        keepAlive = true
//
//
//                        Log.i(
//                            "startSendingToReceiver",
//                            "Socket successfully connected to receiver at $hostAddress on port $PORT_NUMBER"
//                        )
//                    }
                    try {
                        socket = Socket().apply {
                            bind(null)
                            connect(InetSocketAddress(hostAddress, PORT_NUMBER), 30000)
                            keepAlive = true
                        }
                        Log.i("startSendingToReceiver", "Socket successfully connected to $hostAddress:$PORT_NUMBER")
                    } catch (e: IOException) {
                        Log.e("startSendingToReceiver", "Connection failed: ${e.message}", e)
                        // Optional: Retry logic or user feedback
                    }


                    // Calculate total size of media files
                    val totalSize =
                        SelectedListManager.getSelectedMediaList().sumOf { it?.size ?: 0 }
                    Log.i(
                        "startSendingToReceiver",
                        "Total size of selected media to send: ${FileUtils.formatFileSize(totalSize)}"
                    )

                    ObjectOutputStream(socket?.getOutputStream()).use { objectOutputStream ->
                        // Sending selected files
                        Log.d(
                            "startSendingToReceiver",
                            "startSendingToReceiver: ${SelectedListManager.getSelectedMediaList()}"
                        )
                        Log.d(
                            "startSendingToReceiver",
                            "startSendingToReceiver size: ${SelectedListManager.getSelectedMediaList().size}"
                        )
                        for (media in SelectedListManager.getSelectedMediaList()) {
//                            val mFile = File(media?.uri ?: "")
                            val uriString = media?.uri ?: ""
                            val mFile = if (uriString.startsWith("content://")) {
                                getFileFromContentUri(this@SendOrReceiveService, Uri.parse(uriString))
                            } else {
                                File(uriString)
                            }



                            val fileSize = mFile.length()
                            val fileName = mFile.name
                            val fileType = media?.mediaType?.name
                            val output = ObjectOutputStream(socket?.getOutputStream())
                            if (isStop) {
                                Log.d("startSendingToReceiver", "is Stop :$isStop")
                                output.writeUTF(CANCEL_MESSAGE)
                                output.flush()
                            } else {
                                Log.d("startSendingToReceiver", "is Stop :$isStop")
                            }

                            if (fileName == CANCEL_MESSAGE){
                                Log.d("startReceivingFromSender", "startReceivingFromSender: I am Canceled")
                                TransferStateManager.updateReceivingState(
                                    TransferInfoModel(
                                        state = TransferState.TRANSFER_CANCELLED_STATE,
                                        totalSize = 0,
                                        currentProgress = 0
                                    )
                                )
                                output.writeUTF("CANCEL_SENDING")
                                output.flush()
                                break
                            }

//                            val output = ObjectOutputStream(socket?.getOutputStream())

// Send metadata
                            output.writeUTF(fileName)
                            output.writeUTF(fileType)
                            output.writeLong(fileSize)
                            output.writeLong(totalSize)
                            output.flush()

                            // Send file metadata
//                            objectOutputStream.writeUTF(fileName)
//                            objectOutputStream.writeUTF(fileType)
//                            objectOutputStream.writeLong(fileSize)
//                            objectOutputStream.writeLong(totalSize)
//                            objectOutputStream.flush()

                            // Send file data
                            FileInputStream(mFile).use { fileInputStream ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var bytesRead: Int

                                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                                    socket?.getOutputStream()?.write(buffer, 0, bytesRead)
                                    socket?.getOutputStream()?.flush()
                                    totalBytesRead += bytesRead

                                    val percentage = calculateProgress(totalSize, totalBytesRead)
                                    TransferStateManager.updateSendingState(
                                        TransferInfoModel(
                                            state = TransferState.TRANSFERRING_STATE,
                                            totalSize = totalSize,
                                            currentProgress = totalBytesRead,
                                            percentage = percentage,
                                            fileName = fileName
                                        )
                                    )
                                }
                            }
                            Log.d("startSendingToReceiver", fileName + " " + fileType.toString())
                            media?.uri?.let {
                                saveDataIntoDB(
                                    ZMediaHistoryEntity(
                                        uri = it.toString(),
                                        isSend = true,
                                        mediaType = fileType.toString(),
                                        date = currentDateTime,
                                        size = fileSize,
                                    )
                                )
                                Log.d(
                                    "startSendingToReceiver",
                                    "Received file saved: $fileName, Type: $fileType, URI: $it"
                                )

                            }
                        }

                        // Signal completion
                        objectOutputStream.writeUTF(COMPLETE_MESSAGE)
                        objectOutputStream.flush()
                        Log.i("startSendingToReceiver", "Sent COMPLETE_MESSAGE to receiver")

                        // Await final acknowledgment
                        val finalAck = ObjectInputStream(socket?.getInputStream()).readUTF()
                        if (finalAck == "ACK_COMPLETE") {
                            SelectedListManager.clearSelectedMedia()
                            Log.i(
                                "startSendingToReceiver",
                                "Received ACK_COMPLETE from receiver, transfer complete"
                            )

                            TransferStateManager.updateSendingState(
                                TransferInfoModel(
                                    state = TransferState.TRANSFER_COMPLETE_STATE,
                                    totalSize = 0,
                                    currentProgress = 0
                                )
                            )
                        }
                    }

                } catch (e: Exception) {
                    socket?.close()
                    handleSendingException(e)
                } finally {
                    try {
                        if (socket?.isConnected == true) {
                            Log.i(
                                "startSendingToReceiver",
                                "Socket closed after sending process"
                            )
                            socket?.close()

                            if (socket?.isClosed == true) {
                                socket?.close()
                                Log.i("Socket", "Socket closed successfully.")
                            }

                        }
                        Log.i("startSendingToReceiver", "Socket closed after sending process")
                        Log.i("startSendingToReceiver", "Socket closed Sender")

                    } catch (e: Exception) {
                        Log.e("startSendingToReceiver", "Error closing socket: ${e.message}", e)
                        handleSendingException(e)
                        socket?.close()

                    }
                    Log.i("startSendingToReceiver", "Stopped sending worker")
                }
            }
        }
    }
    fun getFileFromContentUri(context: Context, contentUri: Uri): File {
        val fileName = queryName(context, contentUri)
        val file = File(context.cacheDir, fileName)

        context.contentResolver.openInputStream(contentUri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    fun queryName(context: Context, uri: Uri): String {
        val returnCursor = context.contentResolver.query(uri, null, null, null, null)
        returnCursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            return it.getString(nameIndex)
        }
        return "temp_file"
    }

    private fun saveDataIntoDB(historyEntity: ZMediaHistoryEntity) {
        val mediaHistoryDao = AppDatabase.getDatabase(this).mediaHistoryDao()
        val mediaHistoryRepository = MediaHistoryRepository(mediaHistoryDao)

        // Log the data being saved
        Log.d("saveDataIntoDB", "Preparing to save data: $historyEntity")
        // Launch a coroutine for the suspend function
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mediaHistoryRepository.insertMediaHistory(historyEntity)
                Log.d("saveDataIntoDB", "Data saved successfully: $historyEntity")
            } catch (e: Exception) {
                Log.e("saveDataIntoDB", "Failed to save data: ${e.message}", e)
            }
        }
    }

    fun startReceivingFromSender() {
        receiverjob?.cancel()
        receiverjob = null

        if (receiverjob != null) {
            Log.i("startReceivingFromSender", "Receiver job is already running, returning.")
            return
        }

        Log.i("startReceivingFromSender", "Receiving process initiated")

        receiverjob = CoroutineScope(Dispatchers.IO).launch {
            Log.i("startReceivingFromSender", "Launching receiver job on IO Dispatcher")

            var totalRead = 0L

            var serverSocket: ServerSocket? = null

            try {
                serverSocket = ServerSocket(PORT_NUMBER).apply {
                    soTimeout = 30000    // TODO : Increase timeout to 30 seconds
                    //soTimeout = 5000
                    Log.i("startReceivingFromSender", "ServerSocket created on port: $PORT_NUMBER")
                }

                socket = serverSocket.accept().apply {
                    Log.i(
                        "startReceivingFromSender",
                        "Connection accepted from sender: ${this?.inetAddress}"
                    )
                }

                socket?.apply {
                    socket?.soTimeout = 5000
                    keepAlive = true
                    tcpNoDelay = true
                }

                val inputStream = socket?.getInputStream()
                val outputStream = socket?.getOutputStream()
//                val objectOutputStream = ObjectOutputStream(outputStream)
                val output = ObjectOutputStream(socket?.getOutputStream())
                val objectInputStream = ObjectInputStream(inputStream)

                while (true) {
                    try {
                        val fileName = objectInputStream.readUTF()


                        if (isStop){
                            output.writeUTF(CANCEL_MESSAGE)
                            output.flush()
                            break
                        }


                        if (fileName == CANCEL_MESSAGE) {
                            Log.d("startReceivingFromSender", "startReceivingFromSender: I am Canceled")
                            TransferStateManager.updateReceivingState(
                                TransferInfoModel(
                                    state = TransferState.TRANSFER_CANCELLED_STATE,
                                    totalSize = 0,
                                    currentProgress = 0
                                )
                            )
                            output.writeUTF("CANCEL_SENDING")
                            output.flush()
                            break
                        }
                        if (fileName == "COMPLETE") {
                            Log.i("startReceivingFromSender", "Transfer complete signal received")
                            TransferStateManager.updateReceivingState(
                                TransferInfoModel(
                                    state = TransferState.TRANSFER_COMPLETE_STATE,
                                    totalSize = 0,
                                    currentProgress = 0
                                )
                            )
                            output.writeUTF("ACK_COMPLETE")
                            output.flush()
                            break
                        }


                        val fileType = objectInputStream.readUTF()
                        val metaDataSize = objectInputStream.readLong()
                        val totalSize = objectInputStream.readLong()

                        val fileDirectory = FileUtils.createDirectory(fileType)
                        val validFileName = FileUtils.processFileName(fileName)
                        val fileToCreate = File(fileDirectory, validFileName)
                        val fileOutputStream = FileOutputStream(fileToCreate)

                        val buffer = ByteArray(BUFFER_SIZE)
                        var totalBytesRead = 0

                        while (totalBytesRead < metaDataSize) {
                            val remainingBytes = metaDataSize - totalBytesRead
                            val bytesRead = inputStream?.read(
                                buffer,
                                0,
                                minOf(buffer.size.toLong(), remainingBytes).toInt()
                            ) ?: 0

                            if (bytesRead == -1) throw IOException("Unexpected end of stream")
                            fileOutputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            totalRead += bytesRead

                            TransferStateManager.updateReceivingState(
                                TransferInfoModel(
                                    state = TransferState.TRANSFERRING_STATE,
                                    totalSize = totalSize,
                                    currentProgress = totalRead,
                                    percentage = calculateProgress(totalSize, totalRead),
                                    fileName = fileName
                                )
                            )
                        }

                        fileOutputStream.close()

                        val fileUri = fileToCreate.toUri()
                        saveDataIntoDB(
                            ZMediaHistoryEntity(
                                uri = fileUri.toString(),
                                isSend = false,
                                mediaType = fileType,
                                date = currentDateTime,
                                size = metaDataSize
                            )
                        )
                        Log.d(
                            "startReceivingFromSender",
                            "Received file saved: $fileName, Type: $fileType, URI: $fileUri"
                        )

                        // Update media scanner
                        MediaScannerConnection.scanFile(
                            this@SendOrReceiveService,
                            arrayOf(fileToCreate.path),
                            null,
                            null
                        )

                    } catch (e: Exception) {
                        handleReceivingException(e)
                        break
                    }
                }

                objectInputStream.close()
                inputStream?.close()

            } catch (e: Exception) {
                handleReceivingException(e)
            } finally {
                try {
                    socket?.close()
                    serverSocket?.close()
                    Log.i("startReceivingFromSender", "Socket and ServerSocket closed successfully.")
                } catch (e: Exception) {
                    Log.e("startReceivingFromSender", "Error closing sockets: ${e.message}", e)
                } finally {
                    receiverjob = null // Reset the receiver job reference
                }
            }

        }
    }

    private fun logException(tag: String, e: Exception, defaultMsg: String): TransferState {
        val state = when (e) {
            is SocketTimeoutException -> {
                Log.e(tag, "SocketTimeoutException: Connection timed out while processing data.")
                TransferState.CONNECTION_TIMEOUT_STATE
            }

            is SocketException -> {
                Log.e(
                    tag,
                    "SocketException: A socket error occurred. Possible causes: ${e.message}"
                )
                TransferState.TRANSFER_ASK_RECEIVER_STATE
            }

            is EOFException -> {
                Log.e(
                    tag,
                    "EOFException: End of stream reached unexpectedly while processing data."
                )
                TransferState.TRANSFER_FAILED_STATE
            }

            is StreamCorruptedException -> {
                Log.e(
                    tag,
                    "StreamCorruptedException: Data corruption detected in stream. Message: ${e.message}"
                )
                TransferState.TRANSFER_FAILED_STATE
            }

            is FileNotFoundException -> {
                Log.e(
                    tag,
                    "FileNotFoundException: File could not be created or found. File path: ${e.message}"
                )
                TransferState.TRANSFER_FAILED_STATE
            }

            is IOException -> {
                Log.e(
                    tag,
                    "IOException: An I/O error occurred while processing data. Message: ${e.message}"
                )
                TransferState.TRANSFER_FAILED_STATE
            }

            is SecurityException -> {
                Log.e(
                    tag,
                    "SecurityException: A security violation occurred. Message: ${e.message}"
                )
                TransferState.TRANSFER_FAILED_STATE
            }

            is ClassNotFoundException -> {
                Log.e(
                    tag,
                    "ClassNotFoundException: Required class for deserialization not found. Message: ${e.message}"
                )
                TransferState.TRANSFER_FAILED_STATE
            }

            else -> {
                Log.e(tag, "$defaultMsg: ${e::class.java.simpleName}. Message: ${e.message}")
                TransferState.TRANSFER_FAILED_STATE
            }
        }
        return state
    }

    private fun handleReceivingException(e: Exception) {
        Log.e("startReceivingFromSender", "Error during receiving: ${e.message}", e)
        val state = logException("startReceivingFromSender", e, "Unexpected error while receiving")

        // Update the transfer state accordingly
        TransferStateManager.updateReceivingState(
            TransferInfoModel(
                state = state,
                totalSize = 0,
                currentProgress = 0
            )
        )
    }

    private fun handleSendingException(e: Exception) {
        Log.e("startSendingToReceiver", "Error during sending: ${e.message}", e)
        val state = logException("startSendingToReceiver", e, "Unexpected error while sending")

        // Update the transfer state accordingly
        TransferStateManager.updateSendingState(
            TransferInfoModel(
                state = state,
                totalSize = 0,
                currentProgress = 0
            )
        )
    }


//    fun stopService() {
//        stopForeground(STOP_FOREGROUND_REMOVE)
//
//        if (wakeLock?.isHeld == true) {
//            wakeLock?.release()
//        }
//
//        job?.cancel()
//        receiverjob?.cancel()
//
//        stopSelf()
//    }

    var isStop = false
    fun stopService() {
        Log.d("stopService", "stopService() called: Preparing to stop the service.")

        isStop = true

        if (receiverjob != null) {
            receiverjob?.cancel()
            Log.d("stopService", "Receiver Job canceled.")
        } else {
            Log.d("stopService", "No Receiver Job to cancel.")
        }

        /*   stopForeground(STOP_FOREGROUND_REMOVE)
           Log.d("stopService", "Foreground service stopped.")

           if (wakeLock?.isHeld == true) {
               wakeLock?.release()
               Log.d("stopService", "WakeLock released.")
           } else {
               Log.d("stopService", "No WakeLock was held.")
           }

           if (job != null) {
               job?.cancel()
               Log.d("stopService", "Job canceled.")
           } else {
               Log.d("stopService", "No Job to cancel.")
           }

           if (receiverjob != null) {
               receiverjob?.cancel()
               Log.d("stopService", "Receiver Job canceled.")
           } else {
               Log.d("stopService", "No Receiver Job to cancel.")
           }
           if (canceljob != null) {
               canceljob?.cancel()
               Log.d("stopService", "Receiver Job canceled.")
           } else {
               Log.d("stopService", "No Receiver Job to cancel.")
           }

           stopSelf()
           Log.d("stopService", "Service stopped with stopSelf().")*/
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channelId = "send_receive_service_channel"
        val channelName = "Send/Receive Service"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "Channel for Send/Receive Service notifications"
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val channelId = "send_receive_service_channel"
        val notificationTitle = "Service Running"
        val notificationText = "Transferring your data in the background."

        // Intent to open the app when the notification is clicked
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId).setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_notification_fb) // Replace with your app's icon
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Set priority for Android 7.1 and lower
            .build()
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d("awaisshakeel", "service is onDestroy")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        socket?.close()
        releaseWakeLock()
    }

//    private fun calculateProgress(total: Long, current: Long): Int {
//        val percent = (current.toDouble() / total.toDouble())
//        return (percent * 100).toInt()
//    }

    private fun calculateProgress(total: Long, current: Long): Int {
        // Prevent division by zero
        if (total == 0L) {
            return 0
        }

        val percent = (current.toDouble() / total.toDouble()) * 100
        return percent.toInt()  // Convert to Int after calculating the percentage
    }

    /**
     * Acquires a partial WakeLock to keep the CPU running even when the screen is off.
     * This is useful for long-running background tasks like file transfers.
     * */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::FileTransferWakeLock")
        wakeLock?.acquire()
    }
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }


    /** Chatgpt Suggestion */
    fun stopServiceCompletely() {
        Log.d("stopService", "Stopping service completely...")

        isStop = true

        // Cancel all jobs
        job?.cancel()
        receiverjob?.cancel()
        canceljob?.cancel()

        // Close socket if open
        try {
            socket?.close()
            Log.d("stopService", "Socket closed.")
        } catch (e: Exception) {
            Log.e("stopService", "Error closing socket: ${e.message}")
        }

        // Release WakeLock
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            Log.d("stopService", "WakeLock released.")
        }

        // Stop foreground and remove notification
        stopForeground(true)

        // Remove notification manually (optional)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(100)

        // Finally stop the service
        stopSelf()
        Log.d("stopService", "Service has been stopped.")
    }

    /** Deep Seek Suggestion */
//    fun stopServiceCompletely() {
//        Log.d("stopService", "stopService() called: Preparing to stop the service.")
//
//        // Set the stop flag first
//        isStop = true
//
//        // Cancel all jobs
//        job?.cancel()
//        receiverjob?.cancel()
//        canceljob?.cancel()
//
//        // Close sockets
//        try {
//            socket?.close()
//            Log.d("stopService", "Socket closed successfully.")
//        } catch (e: Exception) {
//            Log.e("stopService", "Error closing socket: ${e.message}")
//        }
//
//        // Stop foreground and remove notification
//        stopForeground(STOP_FOREGROUND_REMOVE)
//
//        // Release wake lock
//        if (wakeLock?.isHeld == true) {
//            wakeLock?.release()
//            Log.d("stopService", "WakeLock released.")
//        }
//
//        // Stop the service itself
//        stopSelf()
//
//        // Update transfer states to cancelled
//        TransferStateManager.updateSendingState(
//            TransferInfoModel(
//                state = TransferState.TRANSFER_CANCELLED_STATE,
//                totalSize = 0,
//                currentProgress = 0
//            )
//        )
//        TransferStateManager.updateReceivingState(
//            TransferInfoModel(
//                state = TransferState.TRANSFER_CANCELLED_STATE,
//                totalSize = 0,
//                currentProgress = 0
//            )
//        )
//
//        Log.d("stopService", "Service stopped completely.")
//    }

}