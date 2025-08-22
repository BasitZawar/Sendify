package com.smartswitch.domain.usecase

import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.repository.MediaRepository
import javax.inject.Inject

class DocUseCase @Inject constructor(private val mediaRepository: MediaRepository) {


   suspend fun getDoc(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit) {
        return mediaRepository.getDocuments(onCompleteFetch)
    }


}