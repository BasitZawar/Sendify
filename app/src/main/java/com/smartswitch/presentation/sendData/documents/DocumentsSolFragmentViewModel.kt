package com.smartswitch.presentation.sendData.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.usecase.DocUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentsSolFragmentViewModel @Inject constructor(private val docUseCase: DocUseCase) :
    ViewModel() {
    val isFetchingComplete: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var documentsList = mutableListOf<MediaInfoModel>()

    fun getDocs() {
        viewModelScope.launch {
            docUseCase.getDoc { list ->
                documentsList = list.toMutableList()
                isFetchingComplete.value = true
            }
        }

    }
}