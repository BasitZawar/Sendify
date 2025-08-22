package com.smartswitch.presentation.sendData.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.usecase.PhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotosSolFragmentViewModel @Inject constructor(private val photosUseCase: PhotosUseCase) :
    ViewModel() {
    val isFetchingComplete: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var photoList = mutableListOf<MediaInfoModel>()

    fun getAllPhotos() {
        viewModelScope.launch {
            photosUseCase.getImages { list ->
                photoList = list.toMutableList()
                isFetchingComplete.value = true
            }
        }

    }
}