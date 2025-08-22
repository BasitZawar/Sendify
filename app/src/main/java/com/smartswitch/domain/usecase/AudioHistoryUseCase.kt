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

class AudioHistoryUseCase @Inject constructor(
    private val mediaHistoryRepository: MediaHistoryRepository
) {
    fun getAudioHistory(onNoData:()->Unit): Flow<List<MediaInfoModel>> {
        return mediaHistoryRepository.getMediaHistory(MediaTypeEnum.AUDIOS.name)
            .map { mediaHistoryEntities ->
                if (mediaHistoryEntities.isEmpty()) {
                    onNoData.invoke()
                    Log.d("AudioHistoryUseCase___", "No audio history found in the database.")
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
                            mediaType = MediaTypeEnum.AUDIOS,
                            isSend = entity.isSend
                        )
                    }
                }


            }
            .onStart {
                Log.d("AudioHistoryUseCase___", "Starting to fetch audio history.")
            }
            .catch { e ->
                Log.e(
                    "AudioHistoryUseCase___",
                    "Error fetching audio history: ${e.localizedMessage}",
                    e
                )
                emit(emptyList()) // Emit empty list on error
            }

    }

}