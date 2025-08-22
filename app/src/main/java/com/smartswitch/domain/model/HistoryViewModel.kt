package com.smartswitch.domain.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartswitch.domain.repository.MediaHistoryRepository
import com.smartswitch.domain.usecase.AppsHistoryUseCase
import com.smartswitch.domain.usecase.AudioHistoryUseCase
import com.smartswitch.domain.usecase.ContactHistoryUseCase
import com.smartswitch.domain.usecase.DocumentHistoryUseCase
import com.smartswitch.domain.usecase.PhotosHistoryUseCase
import com.smartswitch.domain.usecase.VideoHistoryUseCase
import com.smartswitch.presentation.database.ZMediaHistoryEntity
import com.smartswitch.utils.enums.HistoryCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val mediaHistoryRepository: MediaHistoryRepository,
    private val audiosHistoryUseCase: AudioHistoryUseCase,
    private val photosHistoryUseCase: PhotosHistoryUseCase,
    private val videoHistoryUseCase: VideoHistoryUseCase,
    private val documentHistoryUseCase:DocumentHistoryUseCase,
    private val appsHistoryUseCase: AppsHistoryUseCase,
    private val contactHistoryUseCase: ContactHistoryUseCase
) : ViewModel() {

    val isGridLayout: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _selectedFragment = MutableStateFlow(HistoryCategory.SEND.name)

    private val _photoHistoryList = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    private val _AudioHistoryList = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    private val _VideoHistoryList = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    private val _DocomentsHistoryList = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    private val _ContectsHistoryList = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    private val _AppsHistoryList = MutableStateFlow<List<MediaInfoModel>>(emptyList())

    // Expose filtered list based on the selectedFragment
    private val _filteredPhotoHistory = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    val filteredPhotoHistory: StateFlow<List<MediaInfoModel>> get() = _filteredPhotoHistory



    private val _filteredAudioHistory = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    val filteredAudioHistory: StateFlow<List<MediaInfoModel>> get() = _filteredAudioHistory


    private val _filteredVideoHistory = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    val filteredVideoHistory: StateFlow<List<MediaInfoModel>> get() = _filteredVideoHistory


    private val _filteredDocomentsHistory = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    val filteredDocomentsHistory: StateFlow<List<MediaInfoModel>> get() = _filteredDocomentsHistory


    private val _filteredContectssHistory = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    val filteredContectssHistory: StateFlow<List<MediaInfoModel>> get() = _filteredContectssHistory

    private val _filteredAppsHistory = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    val filteredAppsHistory: StateFlow<List<MediaInfoModel>>get() = _filteredAppsHistory

    // Function to update selected fragment
    fun setSelectedFragment(historyCategory: HistoryCategory) {
        _selectedFragment.value = historyCategory.name
    }
    private val _selectedItems = MutableStateFlow<MutableSet<ZMediaHistoryEntity>>(mutableSetOf())
    val selectedItems: StateFlow<Set<ZMediaHistoryEntity>> = _selectedItems



     fun deleteMediaHistory(list: List<MediaInfoModel?>) {
        viewModelScope.launch(Dispatchers.IO) {
          val mlist = list.map {
            it?.let {
                ZMediaHistoryEntity(it.uri,it.size,it.duration,it.date,it.mediaType?.name,it.isSend)
            }
          }
            mlist.let {
                mediaHistoryRepository.deleteMediaHistory(it)
            }

        }
    }

init {
    viewModelScope.launch() {
        launch  (Dispatchers.IO){
            photosHistoryUseCase.getPhotoHistory(){
                _photoHistoryList.value= emptyList()
            }.collect { currentList ->
                if (currentList.isNotEmpty()) {
                    Log.d("PhotoHistoryUseCase___", "Fetched photo history list size: ${currentList.size}")
                    _photoHistoryList.value = currentList // This will trigger combine
                }
            }
        }

        launch(Dispatchers.IO){
            Log.d("ContactHistoryUseCase___", "Fetched Contects history list size:")
            contactHistoryUseCase.getContactHistory(){
                _ContectsHistoryList.value= emptyList()
            }.collect { currentList ->
                if (currentList.isNotEmpty()) {
                    Log.d("ContactHistoryUseCase___", "Fetched Contects history list size: ${currentList.size}")
                    _ContectsHistoryList.value = currentList // This will trigger combine
                }
            }
        }
        launch(Dispatchers.IO){
        audiosHistoryUseCase.getAudioHistory {
            _AudioHistoryList.value = emptyList()
            Log.d("AudioHistoryUseCase___", "Clear All Data")
        }.collect { currentList ->
            if (currentList.isNotEmpty()) {
                Log.d("AudioHistoryUseCase___", "Fetched Audio history list size: ${currentList.size}")
                _AudioHistoryList.value = currentList // This will trigger combine
            }
        }
        }

        launch(Dispatchers.IO) {
        videoHistoryUseCase.getVideoHistory()
        {
            _VideoHistoryList.value= emptyList()

        }.collect { currentList ->
            if (currentList.isNotEmpty()) {
                Log.d("VideoHistoryUseCase___", "Fetched video history list size: ${currentList.size}")

                _VideoHistoryList.value = currentList
            }
        }
        }

        launch(Dispatchers.IO) {
           appsHistoryUseCase.getAppHistory(){
               _AppsHistoryList.value= emptyList()
           }.collect { currentList ->
                if (currentList.isNotEmpty()) {
                    Log.d("AppHistoryUseCase___", "Fetched app history list size: ${currentList.size}")
                    _AppsHistoryList.value = currentList // This will trigger combine
                }
            }
        }
        launch(Dispatchers.IO) {
            documentHistoryUseCase.getDocumentHistory(){
                _DocomentsHistoryList.value= emptyList()

            }.collect { currentList ->
                if (currentList.isNotEmpty()) {
                    Log.d(
                        "DocumentHistoryUseCase___",
                        "Fetched Audio history list size: ${currentList.size}"
                    )
                    _DocomentsHistoryList.value = currentList // This will trigger combine
                }
            }
        }
    }
    viewModelScope.launch {
        combine(_selectedFragment, _photoHistoryList) { selectedFragment, photoList ->
            Log.d("PhotoHistoryUseCase___", "Combining flows: selectedFragment = $selectedFragment, photoList size = ${photoList.size}")
            val isSendMode = selectedFragment == HistoryCategory.SEND.name
            photoList.filter { it.isSend == isSendMode }
        }.collect { filteredList ->
            _filteredPhotoHistory.value = filteredList
            Log.d("PhotoHistoryUseCase___", "Filtered list size: ${_filteredPhotoHistory.value.size}")
        }
    }
    viewModelScope.launch {
        combine(_selectedFragment, _VideoHistoryList) { selectedFragment, videoList ->
            val isSendMode = selectedFragment == HistoryCategory.SEND.name
            videoList.filter { it.isSend == isSendMode }
        }.collect { filteredList ->
            _filteredVideoHistory.value = filteredList
        }
    }
    viewModelScope.launch {
        combine(_selectedFragment, _AudioHistoryList) { selectedFragment, audioList ->
            val isSendMode = selectedFragment == HistoryCategory.SEND.name
            audioList.filter { it.isSend == isSendMode }
        }.collect { filteredList ->
            _filteredAudioHistory.value = filteredList
        }
    }

    viewModelScope.launch {
        combine(_selectedFragment, _ContectsHistoryList) { selectedFragment, contactList ->
            val isSendMode = selectedFragment == HistoryCategory.SEND.name
            contactList.filter { it.isSend == isSendMode }
        }.collect { filteredList ->
            _filteredContectssHistory.value = filteredList
        }
    }
    viewModelScope.launch {
        combine(_selectedFragment, _DocomentsHistoryList) { selectedFragment, documentsList ->
            val isSendMode = selectedFragment == HistoryCategory.SEND.name
            documentsList.filter { it.isSend == isSendMode }
        }.collect { filteredList ->
            _filteredDocomentsHistory.value = filteredList
        }
    }

    viewModelScope.launch {
        combine(_selectedFragment, _AppsHistoryList) { selectedFragment, appsList ->
            Log.d("AppsHistoryUseCase___", "Combining flows: selectedFragment = $selectedFragment, appsList size = ${appsList.size}")
            val isSendMode = selectedFragment == HistoryCategory.SEND.name
            appsList.filter { it.isSend == isSendMode }
        }.collect { filteredList ->
            _filteredAppsHistory.value = filteredList
            Log.d("AppsHistoryUseCase___", "Filtered list size: ${_filteredAppsHistory.value.size}")
        }
    }

}
    
}


