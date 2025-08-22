package com.smartswitch.utils.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import com.smartswitch.utils.enums.MediaTypeEnum
import com.smartswitch.utils.enums.TransferState
import com.google.common.util.concurrent.ListenableFuture
import com.smartswitch.R
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.model.TransferInfoModel
import com.smartswitch.presentation.MainActivity
import com.smartswitch.utils.FileUtils
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.TransferStateManager
import com.smartswitch.utils.extensions.stopSendOrReceiveWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import java.net.BindException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class SendOrReceiveWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : ListenableWorker(appContext, workerParams) {

    private val BUFFER_SIZE = 16384
    private val PORT_NUMBER = 6062
    private var wakeLock: PowerManager.WakeLock? = null

    private val completeSentItems = mutableListOf<MediaInfoModel?>()
    private val completeReceivedItems = mutableListOf<MediaInfoModel?>()

    var job: Job? = null
    var receiverjob: Job? = null
    private val context = appContext

    companion object {
        private const val COMPLETE_MESSAGE = "COMPLETE"
    }

    private val notificationManager: NotificationManager by lazy {
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @SuppressLint("RestrictedApi")
    override fun startWork(): ListenableFuture<Result> {

        acquireWakeLock()
        val transferMode = inputData.getString("TRANSFER_MODE")

        val foregroundInfo: ForegroundInfo =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ForegroundInfo(1, createNotification(transferMode))
            } else {
                ForegroundInfo(
                    1, createNotification(transferMode), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            }
        setForegroundAsync(foregroundInfo)

        acquireWakeLock()

        if (transferMode == "SEND") {
            startSendingToReceiver()
        } else if (transferMode == "RECEIVE") {
            startReceivingFromSender()
        }

        return SettableFuture.create()


    }

    private fun createNotification(transferMode: String?): Notification {
        var notificationText = ""
        if (transferMode == "SEND"){
            notificationText = "Data Sending is in progress"
        }else if (transferMode == "RECEIVE"){
            notificationText = "Data Receiving is in progress"
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                1.toString(), notificationText, NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val resultIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    context,
                    1,
                    resultIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                PendingIntent.getActivity(
                    context,
                    1,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, 1.toString())
                .setContentTitle(notificationText)
                .setBadgeIconType(R.drawable.splashimg)
                .setSmallIcon(R.drawable.splashimg)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(null)

        return builder.build()
    }

    private fun startSendingToReceiver() {

        job?.cancel()
        var totalBytesRead = 0L
        var socket: Socket? = null

        Log.i("startSendingToReceiver", "Starting the sending process")

        val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val wifiChannel = wifiP2pManager.initialize(context, context.mainLooper, null)

        wifiP2pManager.requestConnectionInfo(wifiChannel) { connectionInfo ->

            job = CoroutineScope(Dispatchers.IO).launch {

                try {
                    val hostAddress = connectionInfo.groupOwnerAddress
                    Log.i("startSendingToReceiver", "Resolved receiver host address: $hostAddress")

                    // Initialize socket connection
                    socket = Socket().apply {
                        bind(null)
                        connect(InetSocketAddress(hostAddress, PORT_NUMBER), 20000)
                        setKeepAlive(true)

                        Log.i("startSendingToReceiver", "Socket successfully connected to receiver at $hostAddress on port $PORT_NUMBER")
                    }

                    // Calculate total size of media files
                    val totalSize = SelectedListManager.getSelectedMediaList().sumOf { it?.size ?: 0 }
                    Log.i("startSendingToReceiver", "Total size of selected media to send: ${FileUtils.formatFileSize(totalSize)}")

                    ObjectOutputStream(socket?.getOutputStream()).use { objectOutputStream ->
                        // Sending selected files
                        for (media in SelectedListManager.getSelectedMediaList()) {
                            val mFile = File(media?.uri ?: "")
                            val fileSize = mFile.length()
                            val fileName = mFile.name
                            val fileType = media?.mediaType?.name

                            Log.i("startSendingToReceiver", "Preparing to send file: $fileName, Type: $fileType, Size: ${FileUtils.formatFileSize(fileSize)}")

                            // Send file metadata
                            objectOutputStream.writeUTF(fileName)
                            objectOutputStream.writeUTF(fileType)
                            objectOutputStream.writeLong(fileSize)
                            objectOutputStream.writeLong(totalSize)
                            objectOutputStream.flush()
                            Log.i("startSendingToReceiver", "Sent file info for $fileName")

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
                                    Log.i("startSendingToReceiver", "Sent $bytesRead bytes for $fileName, Progress: $percentage%")
                                }
                            }
                            Log.i("startSendingToReceiver", "File $fileName sent successfully")
                        }

                        // Signal completion
                        objectOutputStream.writeUTF(COMPLETE_MESSAGE)
                        objectOutputStream.flush()
                        Log.i("startSendingToReceiver", "Sent COMPLETE_MESSAGE to receiver")

                        // Await final acknowledgment
                        val finalAck = ObjectInputStream(socket?.getInputStream()).readUTF()
                        if (finalAck == "ACK_COMPLETE") {
                            Log.i("startSendingToReceiver", "Received ACK_COMPLETE from receiver, transfer complete")

                            TransferStateManager.updateSendingState(
                                TransferInfoModel(
                                    state = TransferState.TRANSFER_COMPLETE_STATE,
                                    totalSize = 0,
                                    currentProgress = 0,
                                )
                            )
                        }
                    }

                } catch (e: Exception) {
                    Log.e("startSendingToReceiver", "Error during sending process: ${e.message}", e)
                    handleSendingException(e)
                } finally {
                    try {
                        socket?.let {
                            if (it.isClosed) {
                                Log.i("SendSocketCheck", "Socket is already closed.")
                            } else {
                                it.close()
                                Log.i("SendSocketCheck", "Socket closed successfully.")
                            }
                        } ?: Log.w("SendSocketCheck", "Socket is null, nothing to close.")
                    } catch (e: IOException) {
                        Log.e("SendSocketCheck", "Error while closing the sender socket: ${e.message}", e)
                    } finally {
                        context.stopSendOrReceiveWorker()
                        Log.i("SendSocketCheck", "Sending worker stopped.")
                    }
                }

            }
                    context.stopSendOrReceiveWorker()
                    Log.i("startSendingToReceiver", "Stopped sending worker")
                }
            }


    private fun startReceivingFromSender() {
        Log.i("startReceivingFromSender", "Receiving process initiated")
        var totalRead = 0L
        var socket: Socket? = null
        var serverSocket: ServerSocket? = null
        receiverjob?.cancel()

        try {
            receiverjob = CoroutineScope(Dispatchers.IO).launch {
                Log.i("startReceivingFromSender", "Launching receiver job on IO Dispatcher")

                try {
                    // Try to create the server socket, handle BindException if port is in use
                    serverSocket = ServerSocket(PORT_NUMBER).also {
                        Log.i("startReceivingFromSender", "ServerSocket created on port: $PORT_NUMBER")
                    }
                } catch (e: BindException) {
                    Log.e("startReceivingFromSender", "Port $PORT_NUMBER is already in use, trying a new port")
                    // Try with a different port or alert the user
                    /*serverSocket = ServerSocket(0).also {
                        Log.i("startReceivingFromSender", "ServerSocket created on a dynamic port: ${it.localPort}")
                    }*/
                }

                socket = serverSocket?.accept().also {
                    Log.i("startReceivingFromSender", "Connection accepted from sender: ${it?.inetAddress}")
                }

                socket?.apply {
                    soTimeout = 3000
                    setKeepAlive(true)
                    tcpNoDelay = true
                    Log.i("startReceivingFromSender", "Socket settings applied (timeout: 60000ms, keepAlive: true, tcpNoDelay: true)")
                }

                val inputStream = socket?.getInputStream()
                val outputStream = socket?.getOutputStream()
                val objectOutputStream = ObjectOutputStream(outputStream)
                val objectInputStream = ObjectInputStream(inputStream)

                while (true) {
                    try {
                        val fileName = objectInputStream.readUTF()
                        Log.i("startReceivingFromSender", "Received file name: $fileName")

                        if (fileName == "COMPLETE") {
                            Log.i("startReceivingFromSender", "Transfer complete signal received")
                            TransferStateManager.updateReceivingState(
                                TransferInfoModel(
                                    state = TransferState.TRANSFER_COMPLETE_STATE,
                                    totalSize = 0,
                                    currentProgress = 0
                                )
                            )
                            objectOutputStream.writeUTF("ACK_COMPLETE")
                            objectOutputStream.flush()
                            Log.i("startReceivingFromSender", "Acknowledgement sent for completion")
                            break
                        }

                        val fileType = objectInputStream.readUTF()
                        val metaDataSize = objectInputStream.readLong()
                        val totalSize = objectInputStream.readLong()

                        Log.i("startReceivingFromSender", "File type: $fileType, Metadata size: $metaDataSize bytes, Total size: $totalSize bytes")

                        val fileDirectory = FileUtils.createDirectory(fileType)
                        val validFileName = FileUtils.processFileName(fileName)
                        val fileToCreate = File(fileDirectory, validFileName)
                        val fileOutputStream = FileOutputStream(fileToCreate)

                        val buffer = ByteArray(BUFFER_SIZE)
                        var totalBytesRead = 0

                        while (totalBytesRead < metaDataSize) {
                            val remainingBytes = metaDataSize - totalBytesRead
                            val bytesRead = inputStream?.read(buffer, 0, minOf(buffer.size.toLong(), remainingBytes).toInt()) ?: 0

                            if (bytesRead == -1) throw IOException("Unexpected end of stream")
                            fileOutputStream.write(buffer, 0, bytesRead)

                            totalBytesRead += bytesRead
                            totalRead += bytesRead

                            Log.i("startReceivingFromSender", "Read $bytesRead bytes, total bytes read: $totalBytesRead")

                            // Update progress
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

                        Log.i("startReceivingFromSender", "File received and saved: ${fileToCreate.path}, Size: ${fileToCreate.length()} bytes")
                        fileOutputStream.close()

                        completeReceivedItems.add(
                            MediaInfoModel(
                                name = fileName,
                                uri = fileToCreate.path,
                                size = fileToCreate.length(),
                                mediaType = MediaTypeEnum.valueOf(fileType)
                            )
                        )

                        MediaScannerConnection.scanFile(context, arrayOf(fileToCreate.path), null, null)
                    } catch (e: SocketException) {
                        Log.e("startReceivingFromSender", "SocketException during file transfer: ${e.message}")
                        break
                    } catch (e: IOException) {
                        Log.e("startReceivingFromSender", "IOException during file transfer: ${e.message}")
                        break
                    }
                }

                objectInputStream.close()
                inputStream?.close()

            }
        } catch (e: Exception) {
            Log.e("startReceivingFromSender", "Exception in receiving process: ${e.message}", e)
            handleReceivingException(e)
        } finally {
            try {
                socket?.let {
                    if (it.isClosed) {
                        Log.i("ReceiveSocketCheck", "Socket is already closed.")
                    } else {
                        it.close()
                        Log.i("ReceiveSocketCheck", "Socket closed successfully.")
                    }
                } ?: Log.w("ReceiveSocketCheck", "Socket is null, nothing to close.")

                serverSocket?.let {
                    if (it.isClosed) {
                        Log.i("ReceiveSocketCheck", "ServerSocket is already closed.")
                    } else {
                        it.close()
                        Log.i("ReceiveSocketCheck", "ServerSocket closed successfully.")
                    }
                } ?: Log.w("ReceiveSocketCheck", "ServerSocket is null, nothing to close.")
            } catch (e: IOException) {
                Log.e("ReceiveSocketCheck", "Error while closing sockets: ${e.message}", e)
            } finally {
                context.stopSendOrReceiveWorker()
                Log.i("ReceiveSocketCheck", "Receiving worker stopped.")
            }
        }

        context.stopSendOrReceiveWorker()
            Log.i("startReceivingFromSender", "Receiving worker stopped")
        }


    private fun logException(tag: String, e: Exception, defaultMsg: String): TransferState {
        val state = when (e) {
            is SocketTimeoutException -> {
                Log.e(tag, "SocketTimeoutException: Connection timed out while processing data.")
                TransferState.CONNECTION_TIMEOUT_STATE
            }
            is SocketException -> {
                Log.e(tag, "SocketException: A socket error occurred. Possible causes: ${e.message}")
                TransferState.TRANSFER_FAILED_STATE
            }
            is EOFException -> {
                Log.e(tag, "EOFException: End of stream reached unexpectedly while processing data.")
                TransferState.TRANSFER_FAILED_STATE
            }
            is StreamCorruptedException -> {
                Log.e(tag, "StreamCorruptedException: Data corruption detected in stream. Message: ${e.message}")
                TransferState.TRANSFER_FAILED_STATE
            }
            is FileNotFoundException -> {
                Log.e(tag, "FileNotFoundException: File could not be created or found. File path: ${e.message}")
                TransferState.TRANSFER_FAILED_STATE
            }
            is IOException -> {
                Log.e(tag, "IOException: An I/O error occurred while processing data. Message: ${e.message}")
                TransferState.TRANSFER_FAILED_STATE
            }
            is SecurityException -> {
                Log.e(tag, "SecurityException: A security violation occurred. Message: ${e.message}")
                TransferState.TRANSFER_FAILED_STATE
            }
            is ClassNotFoundException -> {
                Log.e(tag, "ClassNotFoundException: Required class for deserialization not found. Message: ${e.message}")
                TransferState.TRANSFER_FAILED_STATE
            }
            else -> {
                Log.e(tag, "$defaultMsg: ${e::class.java.simpleName}. Message: ${e.message}")
                TransferState.TRANSFER_FAILED_STATE
            }
        }
        return state
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


    private fun calculateProgress(total: Long, current: Long): Int {
        val percent = (current.toDouble() / total.toDouble())
        return (percent * 100).toInt()
    }

    private fun acquireWakeLock() {
        val powerManager =
            context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::FileTransferWakeLock"
            )
        wakeLock?.acquire()
    }

    private fun releaseWakeLock() {
        wakeLock?.release()
    }


    override fun onStopped() {
        job?.cancel()
        receiverjob?.cancel()
        releaseWakeLock()
        super.onStopped()
    }

}
