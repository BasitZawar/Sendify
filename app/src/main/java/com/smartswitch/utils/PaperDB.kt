package com.smartswitch.utils

import com.smartswitch.domain.model.ShowFileModel
import io.paperdb.Paper

class PaperDB {
    companion object {
        private const val DATA_HISTORY = "data_history"
        private const val DATA_HISTORY_SEND = "data_history_send"
        private const val DATA_HISTORY_SEND_REMOTE = "data_history_send_remote"

        //smart switch sent
        @JvmStatic
        fun addSentHistory(showFileModel: ShowFileModel) {
            Paper.book(DATA_HISTORY_SEND)
                .write(System.currentTimeMillis().toString(), showFileModel)
        }

        //smart switch received history
        @JvmStatic
        fun addReceiveHistory(showFileModel: ShowFileModel) {
            Paper.book(DATA_HISTORY).write(System.currentTimeMillis().toString(), showFileModel)
        }

        @JvmStatic
        fun readHistory(): ArrayList<ShowFileModel> {
            val listPlaylist: ArrayList<ShowFileModel> = ArrayList()
            for (key in Paper.book(DATA_HISTORY).allKeys) {
                listPlaylist.add(Paper.book(DATA_HISTORY).read(key)!!)
            }
            return listPlaylist
        }

        @JvmStatic
        fun readSendHistory(): ArrayList<ShowFileModel> {
            val listPlaylist: ArrayList<ShowFileModel> = ArrayList()
            for (key in Paper.book(DATA_HISTORY_SEND).allKeys) {
                listPlaylist.add(Paper.book(DATA_HISTORY_SEND).read(key)!!)
            }
            return listPlaylist
        }
    }
}