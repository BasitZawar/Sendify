package com.smartswitch.domain.usecase

import android.util.Log
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.repository.MediaHistoryRepository
import com.smartswitch.utils.enums.MediaTypeEnum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class ContactHistoryUseCase @Inject constructor(
    private val mediaHistoryRepository: MediaHistoryRepository
) {
    fun getContactHistory(onNoData:()->Unit): Flow<List<MediaInfoModel>> {
        return mediaHistoryRepository.getMediaHistory(MediaTypeEnum.CONTACTS.name)
            .map { mediaHistoryEntities ->
                if (mediaHistoryEntities.isEmpty()) {
                    onNoData.invoke()
                    Log.d("ContactHistoryUseCase___", "No contact history found in the database.")
                    emptyList() // Emit empty list for no data
                }else{
                    mediaHistoryEntities.map { entity ->
                        val name = entity.uri?.substringAfterLast("/") ?: "Unknown"
                        Log.d(
                            "ContactHistoryUseCase___",
                            "Mapping item - URI: ${entity.uri}, Name: $name, isSent: ${entity.isSend}"
                        )

                        MediaInfoModel(
                            name = name,
                            uri = entity.uri,
                            size = entity.size,
                            date = entity.date,
                            mediaType = MediaTypeEnum.CONTACTS,
                            isSend = entity.isSend
                        )
                    }
                }


            }.onStart {
            // Optional: You can add logging or any action that needs to occur before collecting data
            Log.d("ContactHistoryUseCase___", "Starting to fetch contact history.")
        }.catch { e ->
            // Handle any errors during collection of flow
            Log.e(
                "ContactHistoryUseCase___",
                "Error fetching contact history: ${e.localizedMessage}",
                e
            )
            emit(emptyList()) // Emit empty list in case of error
        }


    }

}
