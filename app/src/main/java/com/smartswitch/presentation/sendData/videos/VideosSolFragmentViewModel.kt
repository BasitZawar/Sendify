package com.smartswitch.presentation.sendData.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.usecase.VideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideosSolFragmentViewModel @Inject constructor(private val videosUseCase: VideosUseCase) :
    ViewModel() {
    val isFetchingComplete: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var videoList = mutableListOf<MediaInfoModel>()

    fun getAllVideos() {
        viewModelScope.launch {
            videosUseCase.getVideos { list ->
                videoList = list.toMutableList()
                isFetchingComplete.value = true
            }
        }

    }
}