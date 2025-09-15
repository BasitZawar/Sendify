package com.smartswitch.domain.model


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShowFileModel(
    var path: String? = null,
    var type: String? = null
) : Parcelable {
    companion object {
        var allFilesReceived: ArrayList<ShowFileModel> = ArrayList()
    }
}

