package com.smartswitch.domain.model

import com.smartswitch.utils.enums.TransferState
import java.io.Serializable

data class TransferInfoModel(
    val state: TransferState,
    val totalSize: Long=0L,
    val currentProgress: Long=0L,
    val percentage: Int = 0,
    val fileName: String = "",
    val forceUpdate: Boolean = false
) : Serializable
