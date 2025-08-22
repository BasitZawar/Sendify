package com.smartswitch.domain.usecase

import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.repository.MediaRepository
import javax.inject.Inject

class AppsUseCase @Inject constructor(private val mediaRepository: MediaRepository) {


    suspend fun getApps(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit) {
        return mediaRepository.fetchAllApps(onCompleteFetch)
    }


}