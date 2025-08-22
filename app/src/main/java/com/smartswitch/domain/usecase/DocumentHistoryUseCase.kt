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

class DocumentHistoryUseCase @Inject constructor(
    private val mediaHistoryRepository: MediaHistoryRepository
) {

    fun getDocumentHistory(onNoData:()->Unit): Flow<List<MediaInfoModel>> {
        return mediaHistoryRepository.getMediaHistory(MediaTypeEnum.DOCUMENTS.name)
            .map { mediaHistoryEntities ->
                if (mediaHistoryEntities.isEmpty()) {
                    onNoData.invoke()
                    Log.d("DocumentHistoryUseCase___", "No document history found in the database.")
                    emptyList() // Emit empty list for no data
                }else{
                    mediaHistoryEntities.map { entity ->
                        val name = entity.uri?.substringAfterLast("/") ?: "Unknown"
                        Log.d(
                            "DocumentHistoryUseCase___",
                            "Mapping item - URI: ${entity.uri}, Name: $name, isSent: ${entity.isSend}"
                        )

                        MediaInfoModel(
                            name = name,
                            uri = entity.uri,
                            size = entity.size,
                            date = entity.date,
                            mediaType = MediaTypeEnum.DOCUMENTS,
                            isSend = entity.isSend
                        )
                    }
                }


            }.onStart {
            // Optional: You can add logging or any action that needs to occur before collecting data
            Log.d("DocumentHistoryUseCase___", "Starting to fetch document history.")
        }.catch { e ->
            // Handle any errors during collection of flow
            Log.e(
                "DocumentHistoryUseCase___",
                "Error fetching document history: ${e.localizedMessage}"
            )
            emit(emptyList()) // Emit empty list in case of error
        }


    }
}
