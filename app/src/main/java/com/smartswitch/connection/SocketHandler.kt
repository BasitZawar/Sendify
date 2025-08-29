package com.smartswitch.connection

import java.net.Socket

class SocketHandler {
    companion object {
        private var socket: Socket? = null

        @Synchronized
        fun getSocket(): Socket? {
            return socket
        }

        @Synchronized
        fun setSocket(socket: Socket) {
            Companion.socket = socket
        }
    }
}