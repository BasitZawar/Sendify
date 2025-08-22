package com.smartswitch.domain.usecase

import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.repository.MediaRepository
import javax.inject.Inject

class ContactsUseCase @Inject constructor(private val mediaRepository: MediaRepository) {


   suspend fun getContacts(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit) {
        return mediaRepository.fetchContacts(onCompleteFetch)
    }


}