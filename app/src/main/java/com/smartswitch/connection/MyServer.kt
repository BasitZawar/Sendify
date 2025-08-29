package com.smartswitch.connection

import android.util.Log
import com.smartswitch.interfaces.DeviceConnectionInterface
import com.smartswitch.utils.Constant
import java.net.ServerSocket
import java.net.Socket

class MyServer(private val connectionInterface: DeviceConnectionInterface) : Thread() {
    private val TAG = javaClass.canonicalName
    var socket: Socket? = null
    var serverSocket: ServerSocket? = null
    override fun run() {
        try {
            serverSocket?.reuseAddress = true
            serverSocket = ServerSocket(Constant.PORT)

            socket = serverSocket!!.accept()

            SocketHandler.setSocket(socket!!)
            connectionInterface.onConnectionSuccessful()

        } catch (e: Exception) {
            connectionInterface.onConnectionFailed()
            Log.e("TAG", "Exception server 1: ${e.message}")
        }
    }
}