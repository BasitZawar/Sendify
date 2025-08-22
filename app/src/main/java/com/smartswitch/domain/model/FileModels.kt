package com.smartswitch.domain.model

import android.graphics.drawable.Drawable
import android.util.Log
import com.smartswitch.utils.enums.MediaTypeEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Serializable

data class MediaInfoModel(
    val name: String?,
    val uri: String?,
    val size: Long?,
    val duration: Long? = null,
    val date: Long? = null,
    val dateTime:String?=null,
    val appPackage: String? = null,
    val contactId: Long? = null,
    val contactNumber: String? = null,
    val apkPath: String? = null,
    val appIcon: Drawable? = null,
    val mediaType: MediaTypeEnum? = null,
    val isSend:Boolean = false,
    val isReceived:Boolean=false,
    var isSelected: Boolean = false
) : Serializable

data class FileMetaData(val name: String?, val size: Long?, val fileTypeEnum: MediaTypeEnum) :
    Serializable

// Define the function to convert MediaInfoModel to HistoryFileItem
suspend fun MediaInfoModel.toHistoryFileItem(isSent: Boolean = false): HistoryFileItem {
    return HistoryFileItem(
        fileName = name ?: "Unknown Name",
        filePath = uri ?: "Unknown Path",
        fileSize = size ?: 0L,
        fileType = mediaType?.name ?: "Unknown Type",
        isSent = isSent
    )
}

suspend fun List<MediaInfoModel?>.toHistoryFileItemList(isSent: Boolean = false): List<HistoryFileItem?> =
    withContext(
        Dispatchers.Default
    ) {
        map {
            Log.i("MediaList___", "toHistoryFileItemList: $it")
            it?.toHistoryFileItem(isSent)

        }
    }

