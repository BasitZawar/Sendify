package com.smartswitch.utils

import com.smartswitch.domain.model.MediaInfoModel


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

object SelectedListManagerForDeletion {
    // Using CopyOnWriteArrayList for thread safety.
    private val selectedMediaList = CopyOnWriteArrayList<MediaInfoModel?>()
    private val selectedContactsList = CopyOnWriteArrayList<MediaInfoModel?>()


    // Clearing all selected contacts
    fun clearSelectedContacts() {
        selectedContactsList.clear()
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

    // Getting a copy of the selected media list
    fun getSelectedContactList(): List<MediaInfoModel?> {
        return selectedContactsList.toList()
    }

    // Checking if a media item is selected
    fun isItemSelected(item: MediaInfoModel?): Boolean {
        return selectedMediaList.contains(item)
    }

    fun clearSelected(){
        selectedMediaList.clear()
        selectedContactsList.clear()
    }

    // Getting the size of the selected media list
    fun getSelectedMediaListSize(): Int {
        return selectedMediaList.size
    }

    // Getting the size of the selected media list
    fun getSelectedContactsListSize(): Int {
        return selectedContactsList.size
    }


}

