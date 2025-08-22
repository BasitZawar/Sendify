package com.smartswitch.utils

import com.smartswitch.utils.enums.TransferState
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.model.TransferInfoModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TransferStateManager {
    private val _fileSendingState = MutableStateFlow(TransferInfoModel( state = TransferState.INITIAL_STATE, totalSize = 0, currentProgress = 0))
    val fileSendingState: StateFlow<TransferInfoModel> = _fileSendingState

    private val _fileReceivingState = MutableStateFlow(TransferInfoModel( state = TransferState.INITIAL_STATE, totalSize = 0, currentProgress = 0))
    val fileReceivingState: StateFlow<TransferInfoModel> = _fileReceivingState

    private val _completeSentItems = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    val completeSentItems: StateFlow<List<MediaInfoModel>> = _completeSentItems

    private val _completeReceivedItems = MutableStateFlow<List<MediaInfoModel>>(emptyList())
    val completeReceivedItems: StateFlow<List<MediaInfoModel>> = _completeReceivedItems

    fun updateSendingState(state: TransferInfoModel) {
        _fileSendingState.value = state.copy(forceUpdate = !_fileSendingState.value.forceUpdate)
    }
    fun updateReceivingState(state: TransferInfoModel) {
        _fileReceivingState.value = state
    }

    fun updateCompleteSentItems(items: List<MediaInfoModel>) {
        _completeSentItems.value = items
    }

    fun updateCompleteReceivedItems(items: List<MediaInfoModel>) {
        _completeReceivedItems.value = items
    }
}
