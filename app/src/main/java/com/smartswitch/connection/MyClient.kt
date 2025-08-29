package com.smartswitch.connection

import android.util.Log
import com.smartswitch.interfaces.DeviceConnectionInterface
import com.smartswitch.utils.Constant
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class MyClient internal constructor(
    address: InetAddress,
    connectionInterface: DeviceConnectionInterface
) : Thread() {
    var socket: Socket = Socket()
    var hostAddress: String = address.hostAddress!!
    var mConnectionInterface = connectionInterface
    override fun run() {
        try {
            socket.connect(InetSocketAddress(hostAddress, Constant.PORT), 500)
            SocketHandler.setSocket(socket)
            mConnectionInterface.onConnectionSuccessful()
//            if (mType == "Sender") {
//                mActivity.startActivity(
//                    Intent(
//                        mActivity,
//                        DisplayDataMainScreenActivity::class.java
//                    )
//                )
//                mActivity.finish()
//            } else {
//                mActivity.startActivity(Intent(mActivity, MyDataReceiverActivity::class.java))
//                mActivity.finish()
//            }
        } catch (e: Exception) {
            mConnectionInterface.onConnectionFailed()
            Log.e("TAG", "Exception Client: ${e.message}")
        }
    }
}