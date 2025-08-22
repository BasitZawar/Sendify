package com.smartswitch.domain.repository

import android.annotation.SuppressLint
import com.smartswitch.domain.model.MediaInfoModel
import java.io.File


interface MediaRepository {

    suspend fun getAllPhotos(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit)

    suspend  fun getVideos(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit)


    suspend fun getAudios(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit)


    suspend  fun fetchContacts(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit)

    suspend fun getDocuments(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit)


    @SuppressLint("QueryPermissionsNeeded")
    suspend  fun fetchAllApps(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit)


    suspend fun getFilesFromFolder(mediaType: String): List<File>
}