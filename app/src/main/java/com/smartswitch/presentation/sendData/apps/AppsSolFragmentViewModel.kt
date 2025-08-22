package com.smartswitch.presentation.sendData.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.usecase.AppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppsSolFragmentViewModel @Inject constructor(private val appsUseCase: AppsUseCase) :
    ViewModel() {
    val isFetchingComplete: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var appsList = mutableListOf<MediaInfoModel>()

    fun getApps() {
        viewModelScope.launch {
            appsUseCase.getApps { list ->
                appsList = list.toMutableList()
                isFetchingComplete.value = true
            }
        }

    }
}