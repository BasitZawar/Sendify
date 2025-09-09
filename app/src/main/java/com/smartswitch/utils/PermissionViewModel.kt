package com.smartswitch.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PermissionViewModel : ViewModel() {
    private val _isPermissionGranted = MutableLiveData<Boolean>()
    val isPermissionGranted: LiveData<Boolean> get() = _isPermissionGranted
    var isStoragePermission = true

    // Call this method when permission is granted
    fun setPermissionGranted(isGranted: Boolean) {
        _isPermissionGranted.value = isGranted
    }
}
