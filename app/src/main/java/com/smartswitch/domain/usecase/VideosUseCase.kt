package com.smartswitch.domain.usecase

import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.repository.MediaRepository
import javax.inject.Inject

class VideosUseCase @Inject constructor(private val mediaRepository: MediaRepository) {


   suspend fun getVideos(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit) {
        return mediaRepository.getVideos(onCompleteFetch)
    }


}