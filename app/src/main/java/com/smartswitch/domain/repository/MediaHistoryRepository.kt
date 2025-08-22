package com.smartswitch.domain.repository

import android.util.Log
import com.smartswitch.presentation.database.MediaHistoryDao
import com.smartswitch.presentation.database.ZMediaHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class  MediaHistoryRepository @Inject constructor(private val mediaHistoryDao: MediaHistoryDao) {

    // Fetch media history by category, returns Flow
    fun getMediaHistory(category: String): Flow<List<ZMediaHistoryEntity>> {
        Log.d("MediaHistoryRepository", "Fetching media history for category: $category")
        return mediaHistoryDao.getMediaHistory(category)  // Returns a Flow of data
    }

    suspend fun insertMediaHistory(mediaHistoryEntity: ZMediaHistoryEntity) {
        mediaHistoryDao.insertMediaHistory(mediaHistoryEntity)
    }

   /* suspend fun deleteMediaHistory(list: List<ZMediaHistoryEntity?>) {
        val list = list.map {
            it?.uri
        }
        mediaHistoryDao.deleteMatchingEntry(list)
    }*/

    suspend fun deleteMediaHistory(list: List<ZMediaHistoryEntity?>) {
        list.forEach { entity ->
            entity?.let {
                mediaHistoryDao.deleteMatchingEntry(
                    uri = it.uri,
                    size = it.size,
                    duration = it.duration,
                    date = it.date,
                    mediaType = it.mediaType,
                    isSend = it.isSend
                )
            }
        }
    }

}