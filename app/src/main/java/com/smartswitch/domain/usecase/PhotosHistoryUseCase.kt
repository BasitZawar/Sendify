package com.smartswitch.domain.usecase

import android.util.Log
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.repository.MediaHistoryRepository
import com.smartswitch.utils.enums.MediaTypeEnum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject



class PhotosHistoryUseCase @Inject constructor(
    private val mediaHistoryRepository: MediaHistoryRepository
) {
    fun getPhotoHistory(onNoData:()->Unit): Flow<List<MediaInfoModel>> {
        return mediaHistoryRepository.getMediaHistory(MediaTypeEnum.PHOTOS.name)
            .map { mediaHistoryEntities ->
                if (mediaHistoryEntities.isEmpty()) {
                    onNoData.invoke()
                    Log.d("PhotoHistoryUseCase___", "No photo history found in the database.")
                    emptyList() // Emit empty list for no data
                }else{
                    mediaHistoryEntities.map { entity ->
                        val name = entity.uri?.substringAfterLast("/") ?: "Unknown"
                        Log.d(
                            "AudioHistoryUseCase___",
                            "Mapping item - URI: ${entity.uri}, Name: $name, isSent: ${entity.isSend}"
                        )

                        MediaInfoModel(
                            name = name,
                            uri = entity.uri,
                            size = entity.size,
                            date = entity.date,
                            mediaType = MediaTypeEnum.PHOTOS,
                            isSend = entity.isSend
                        )
                    }
                }


            }.onStart {
            Log.d("PhotoHistoryUseCase___", "Starting to fetch photo history.")
        }.catch { e ->
            Log.e(
                "PhotoHistoryUseCase___",
                "Error fetching photo history: ${e.localizedMessage}",
                e
            )
            emit(emptyList())
        }


    }


}

