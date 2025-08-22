package com.smartswitch.presentation.sendData.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.usecase.ContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsSolFragmentViewModel @Inject constructor(private val contactsUseCase: ContactsUseCase) :
    ViewModel() {
    val isFetchingComplete: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var contactList = mutableListOf<MediaInfoModel>()

    fun getContacts() {
        viewModelScope.launch {
            contactsUseCase.getContacts { list ->
                contactList = list.toMutableList()
                isFetchingComplete.value = true
            }
        }
    }
}