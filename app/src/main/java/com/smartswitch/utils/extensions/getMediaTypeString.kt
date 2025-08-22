package com.smartswitch.utils.extensions

import android.content.Context
import com.smartswitch.R
import com.smartswitch.utils.enums.MediaTypeEnum

fun Context.getMediaTypeString(mediaType: String): String {
    return when (mediaType) {
        MediaTypeEnum.PHOTOS.toString() -> getString(R.string.media_type_photos)
        MediaTypeEnum.VIDEOS.toString() -> getString(R.string.media_type_videos)
        MediaTypeEnum.AUDIOS.toString() -> getString(R.string.media_type_audios)
        MediaTypeEnum.APPS.toString() -> getString(R.string.media_type_apps)
        MediaTypeEnum.CONTACTS.toString() -> getString(R.string.media_type_contacts)
        MediaTypeEnum.DOCUMENTS.toString() -> getString(R.string.media_type_documents)
        else -> getString(R.string.media_type_other)
    }
}