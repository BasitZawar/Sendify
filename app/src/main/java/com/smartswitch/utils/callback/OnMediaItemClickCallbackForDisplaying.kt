package com.smartswitch.utils.callback

import com.smartswitch.domain.model.MediaInfoModel

interface OnMediaItemClickCallbackForDisplaying {
    fun onMediaItemClickedForDisplaying(mediaInfoModel: MediaInfoModel)
}

