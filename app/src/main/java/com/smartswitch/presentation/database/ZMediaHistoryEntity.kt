package com.smartswitch.presentation.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_history")
data class ZMediaHistoryEntity(
    val uri: String?,
    val size: Long?,
    val duration: Long? = null,
    val date: Long? = null,
    val mediaType: String? = null,
    val isSend:Boolean = false,

    @PrimaryKey(autoGenerate = true) val id: Long = 0,

)
