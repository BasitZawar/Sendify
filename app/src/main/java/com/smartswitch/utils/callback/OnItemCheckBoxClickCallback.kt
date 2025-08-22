package com.smartswitch.utils.callback

import com.smartswitch.domain.model.PhoneCloneItem


interface OnItemCheckBoxClickCallback {
    fun onItemCheckBoxClicked(phoneCloneItem: PhoneCloneItem, isChecked: Boolean)
    fun  onAllowPermissionClicked()
}