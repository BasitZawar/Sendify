package com.smartswitch.utils

import android.util.Log
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.extensions.formatFileSize


/*
object SelectedListManager {
    private val selectedMediaList = mutableListOf<MediaInfoModel?>()
    private val selectedContactsList = mutableListOf<MediaInfoModel?>()

    fun addSelectedContact(item: MediaInfoModel?) {
        if (!selectedContactsList.contains(item)) {
            selectedContactsList.add(item)
        }
    }

    fun removeSelectedContact(item: MediaInfoModel?) {
        selectedContactsList.remove(item)
    }

    fun clearSelectedContacts() {
        selectedContactsList.clear()
    }

    fun isContactItemSelected(item: MediaInfoModel?): Boolean {
        return selectedContactsList.contains(item)
    }

    fun getSelectedContactsList(): List<MediaInfoModel?> {
        return selectedContactsList.toList()
    }




    fun addSelectedMedia(item: MediaInfoModel?) {
        if (!selectedMediaList.contains(item)) {
            selectedMediaList.add(item)
        }
    }

    fun removeSelectedMedia(item: MediaInfoModel?) {
        if (selectedMediaList.contains(item)) {
            selectedMediaList.remove(item)
        }

    }

    fun clearSelectedMedia() {
        selectedMediaList.clear()
    }

    fun getSelectedMediaList(): List<MediaInfoModel?> {
        return selectedMediaList.toList()
    }

    fun getSelectedMediaListSize(): Int {
        return selectedMediaList.size
    }

    fun isItemSelected(item: MediaInfoModel?): Boolean {
        return selectedMediaList.contains(item)
    }




    fun addAllSelectedMedia(list: List<MediaInfoModel?>) {
        selectedMediaList.addAll(list.filterNot { selectedMediaList.contains(it) })
    }

    fun removeAllSelectedMedia(list: List<MediaInfoModel?>) {
        selectedMediaList.removeAll(list)
    }

    fun addAllSelectedContacts(list: List<MediaInfoModel?>) {
        selectedContactsList.addAll(list.filterNot { selectedContactsList.contains(it) })
    }

    fun removeAllSelectedContacts(list: List<MediaInfoModel?>) {
        selectedContactsList.removeAll(list)
    }



}*/
import java.util.concurrent.CopyOnWriteArrayList

object SelectedListManager {
    // Using CopyOnWriteArrayList for thread safety.
    private val selectedMediaList = CopyOnWriteArrayList<MediaInfoModel?>()
    private val selectedContactsList = CopyOnWriteArrayList<MediaInfoModel?>()

    // Adding a single contact item if not already present
    fun addSelectedContact(item: MediaInfoModel?) {
        if (item != null && !selectedContactsList.contains(item)) {
            selectedContactsList.add(item)
        }
    }

    // Removing a single contact item
    fun removeSelectedContact(item: MediaInfoModel?) {
        selectedContactsList.remove(item)
    }

    // Clearing all selected contacts
    fun clearSelectedContacts() {
        selectedContactsList.clear()
    }

    // Checking if a contact item is selected
    fun isContactItemSelected(item: MediaInfoModel?): Boolean {
        return selectedContactsList.contains(item)
    }

    // Getting a copy of the selected contacts list
    fun getSelectedContactsList(): List<MediaInfoModel?> {
        return selectedContactsList.toList()
    }

    // Adding all contacts if they are not already present
    fun addAllSelectedContacts(list: List<MediaInfoModel?>) {
        selectedContactsList.addAll(list.filterNot { selectedContactsList.contains(it) })
//        Log.d("selectedList","add all selected contact"+ selectedContactsList.size.toString())
    }

    fun removeAllSelectedContacts(list: List<MediaInfoModel?>) {
        if (list.isEmpty()) {
            Log.d("SelectedListManager", "The provided list is empty. No contacts to remove.")
            return
        }

        synchronized(this) { // Synchronize to ensure thread safety
            if (selectedContactsList.isEmpty()) {
                Log.d("SelectedListManager", "The selected contacts list is already empty.")
                return
            }

            // Safely filter and retain
            selectedContactsList.retainAll { !list.contains(it) }
            Log.d("SelectedListManager", "Contacts removed. Remaining size: ${selectedContactsList.size}")
        }
//        Log.d("selectedList","remove all selected contact"+ selectedContactsList.size.toString())

    }


    // Adding a single media item if not already present
    fun addSelectedMedia(item: MediaInfoModel?) {
        if (item != null && !selectedMediaList.contains(item)) {
            selectedMediaList.add(item)
        }
    }

    // Removing a single media item
    fun removeSelectedMedia(item: MediaInfoModel?) {
        selectedMediaList.remove(item)
    }

    // Clearing all selected media
    fun clearSelectedMedia() {
        selectedMediaList.clear()
    }

    // Getting a copy of the selected media list
    fun getSelectedMediaList(): List<MediaInfoModel?> {
        return selectedMediaList.toList()
    }

    // Getting the size of the selected media list
    fun getSelectedMediaListSize(): Int {
        return selectedMediaList.size
    }

    // Getting the size of the selected media list
    fun getSelectedContactsListSize(): Int {
        return selectedContactsList.size
    }

    // Checking if a media item is selected
    fun isItemSelected(item: MediaInfoModel?): Boolean {
        return selectedMediaList.contains(item)
    }

    // Adding all media items if they are not already present
    fun addAllSelectedMedia(list: List<MediaInfoModel?>) {
        selectedMediaList.addAll(list.filterNot { selectedMediaList.contains(it) })
        Log.d("selectedList","add all selected media = "+ selectedMediaList.size.toString())
    }

    fun removeAllSelectedMedia(list: List<MediaInfoModel?>) {
        selectedMediaList.removeAll(list.toSet())
        Log.d("selectedList","remove all selected media = "+ selectedMediaList.size.toString())
    }
    // Inside SelectedListManager
    fun clearSelected() {
        selectedMediaList.clear()
        selectedContactsList.clear()
    }


    //////
    fun getTotalSize(): String {
        var totalSize = 0L
        for (item in selectedMediaList) {
            totalSize += item?.size!!
        }

        for (item in selectedContactsList) {
            Log.d("SelectedListManager", "Total Size: $item")
            totalSize += item?.size!!
        }
        return totalSize.formatFileSize()
    }
}

