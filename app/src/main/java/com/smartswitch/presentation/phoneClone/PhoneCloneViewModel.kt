package com.smartswitch.presentation.phoneClone

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartswitch.R
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.model.PhoneCloneItem
import com.smartswitch.domain.usecase.AppsUseCase
import com.smartswitch.domain.usecase.AudioUseCase
import com.smartswitch.domain.usecase.ContactsUseCase
import com.smartswitch.domain.usecase.DocUseCase
import com.smartswitch.domain.usecase.PhotosUseCase
import com.smartswitch.domain.usecase.VideosUseCase
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.enums.MediaTypeEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PhoneCloneViewModel @Inject constructor(
    private val appsUseCase: AppsUseCase,
    private val audioUseCase: AudioUseCase,
    private val docUseCase: DocUseCase,
    private val photoUseCase: PhotosUseCase,
    private val videoUseCase: VideosUseCase,
    private val contactsUseCase: ContactsUseCase
) : ViewModel() {
    var allPhotos = mutableListOf<MediaInfoModel>()
    var allVideos = mutableListOf<MediaInfoModel>()
    var allAudios = mutableListOf<MediaInfoModel>()
    var allContacts = mutableListOf<MediaInfoModel>()
    var allDocuments = mutableListOf<MediaInfoModel>()
    var allApps = mutableListOf<MediaInfoModel>()

    //All media
    var allMedia = mutableListOf<MediaInfoModel>()
    var allContactsList = mutableListOf<MediaInfoModel>()
    val phoneCloneItems = mutableListOf<PhoneCloneItem>()


    private var isPhotosFetched = false
    private var isVideosFetched = false
    private var isAudiosFetched = false
    private var isContactsFetched = false
    private var isDocumentsFetched = false
    private var isAppsFetched = false


    var isAllMediaFetched: MutableStateFlow<Boolean> = MutableStateFlow(false)

    fun fetchAllMedia(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            allMedia.clear()
            allContactsList.clear()
            allMedia.addAll(getAllPhotos())
            allMedia.addAll(getAllVideos())
            allMedia.addAll(getAllAudios())
            allMedia.addAll(getAllDocuments())
            allMedia.addAll(getAllApps())

            if (PermissionManager.hasContactPermission(context)) {
                // Contacts list
                allContactsList.addAll(getAllContacts())
            } else {
                isContactsFetched = true
                isAllDataFetched()
            }
        }
    }

    suspend fun getAllPhotos(): List<MediaInfoModel> {
        return withContext(Dispatchers.IO) {
            photoUseCase.getImages { list ->
                allPhotos = list.toMutableList()
            }
            isPhotosFetched = true
            isAllDataFetched()
            allPhotos
        }
    }


    suspend fun getAllVideos(): List<MediaInfoModel> {
        return withContext(Dispatchers.IO) {
            videoUseCase.getVideos { list ->
                allVideos = list.toMutableList()
            }
            isVideosFetched = true
            isAllDataFetched()
            allVideos
        }
    }


    suspend fun getAllAudios(): List<MediaInfoModel> {
        return withContext(Dispatchers.IO) {
            audioUseCase.getAudios { list ->
                allAudios = list.toMutableList()
            }
            isAudiosFetched = true
            isAllDataFetched()
            allAudios
        }

    }

    suspend fun getAllContacts(): List<MediaInfoModel> {
        return withContext(Dispatchers.IO) {
            contactsUseCase.getContacts { list ->
                allContacts = list.toMutableList()
            }
            isContactsFetched = true
            isAllDataFetched()
            allContacts
        }

    }

    suspend fun getAllDocuments(): List<MediaInfoModel> {
        return withContext(Dispatchers.IO) {
            docUseCase.getDoc { list ->
                allDocuments = list.toMutableList()
            }
            isDocumentsFetched = true
            isAllDataFetched()
            allDocuments
        }

    }

    suspend fun getAllApps(): List<MediaInfoModel> {
        return withContext(Dispatchers.IO) {
            appsUseCase.getApps { list ->
                allApps = list.toMutableList()
            }
            isAppsFetched = true
            isAllDataFetched()
            allApps
        }

    }

    fun isAllDataFetched() {
        isAllMediaFetched.value =
            isPhotosFetched && isVideosFetched && isAudiosFetched && isContactsFetched && isDocumentsFetched && isAppsFetched
    }

    fun fetchMediaNameAndCount(): List<PhoneCloneItem> {
        phoneCloneItems.clear()

        phoneCloneItems.add(
            PhoneCloneItem(
                MediaTypeEnum.DOCUMENTS.name,
                allDocuments.size, R.drawable.ic_clone_files,
                MediaTypeEnum.DOCUMENTS
            )
        )

        phoneCloneItems.add(
            PhoneCloneItem(
                MediaTypeEnum.CONTACTS.name,
                allContacts.size, R.drawable.ic_clone_contact,
                MediaTypeEnum.CONTACTS
            )
        )


        phoneCloneItems.add(
            PhoneCloneItem(
                MediaTypeEnum.PHOTOS.name,
                allPhotos.size,
                R.drawable.ic_clone_photo,
                MediaTypeEnum.PHOTOS


            )
        )
        phoneCloneItems.add(
            PhoneCloneItem(
                MediaTypeEnum.VIDEOS.name,
                allVideos.size, R.drawable.ic_clone_video,
                MediaTypeEnum.VIDEOS
            )
        )
        phoneCloneItems.add(
            PhoneCloneItem(
                MediaTypeEnum.AUDIOS.name,
                allAudios.size, R.drawable.ic_clone_audio,
                MediaTypeEnum.AUDIOS

            )
        )

        phoneCloneItems.add(
            PhoneCloneItem(
                MediaTypeEnum.APPS.name,
                allApps.size, R.drawable.ic_clone_app,
                MediaTypeEnum.APPS

            )
        )


        return phoneCloneItems
    }



    fun clearState() {
        isPhotosFetched = false
        isVideosFetched = false
        isAudiosFetched = false
        isContactsFetched = false
        isDocumentsFetched = false
        isAppsFetched = false
        isAllMediaFetched.value = false
        allMedia.clear()
        allContactsList.clear()
    }

}