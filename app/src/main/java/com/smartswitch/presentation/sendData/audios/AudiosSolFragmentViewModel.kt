package com.smartswitch.presentation.sendData.audios


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.usecase.AudioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudiosSolFragmentViewModel @Inject constructor(private val audioUseCase: AudioUseCase) :
    ViewModel() {
    val isFetchingComplete: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var audioList = mutableListOf<MediaInfoModel>()

    fun getAudios() {
        viewModelScope.launch {
            audioUseCase.getAudios { list ->
                audioList = list.toMutableList()
                isFetchingComplete.value = true
            }
        }
    }
}