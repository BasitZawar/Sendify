package com.smartswitch.presentation.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaHistoryDao {

    @Query("SELECT * FROM media_history WHERE   mediaType = :category")
     fun getMediaHistory(category: String): Flow<List<ZMediaHistoryEntity>>
    @Insert
    suspend fun insertMediaHistory(mediaHistoryEntity: ZMediaHistoryEntity)

   /* @Query("DELETE FROM media_history WHERE uri IN (:filePaths)")
    suspend fun deleteMediaHistoryByUri(filePaths: List<String?>)*/

    @Query(
        "DELETE FROM media_history WHERE " +
                "uri = :uri AND " +
                "size = :size AND " +
                "(:duration IS NULL OR duration = :duration) AND " +
                "(:date IS NULL OR date = :date) AND " +
                "mediaType = :mediaType AND " +
                "isSend = :isSend"
    )
    suspend fun deleteMatchingEntry(
        uri: String?,
        size: Long?,
        duration: Long? = null,
        date: Long? = null,
        mediaType: String?,
        isSend: Boolean
    )
}