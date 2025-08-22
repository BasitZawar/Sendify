package com.smartswitch.domain.model

import com.smartswitch.utils.enums.MediaTypeEnum

data class PhoneCloneItem(
    val name: String,
    val count: Int,
    val image: Int,
    val mediaType: MediaTypeEnum,
)