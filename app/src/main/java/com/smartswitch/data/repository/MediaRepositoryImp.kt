package com.smartswitch.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.domain.repository.MediaRepository
import com.smartswitch.utils.PermissionManager.hasStorageAccessPermission
import com.smartswitch.utils.enums.MediaTypeEnum
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

class MediaRepositoryImp @Inject constructor(@ApplicationContext private val context: Context) : MediaRepository {


    override suspend fun getAllPhotos(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit) {
        val photoList = mutableListOf<MediaInfoModel>()

        withContext(Dispatchers.IO) {
            if (!hasStorageAccessPermission(context )) return@withContext
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.SIZE
            )

            Log.d("getAllPhotos", "Querying MediaStore with collection: $collection")

            val cursor = context.contentResolver.query(
                collection, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC"
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateModifiedIndex = it.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)

                Log.d(
                    "getAllPhotos",
                    "Columns fetched from MediaStore: ID, Name, Data, Size, Date Modified"
                )

                var counter = 0 // To track how many items have been processed
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val path = it.getString(dataColumn)
                    val size = it.getLong(sizeColumn)
                    val uri = path // Simplified uri assignment, adjust if needed
                    val date = it.getLong(dateModifiedIndex)

                    Log.d(
                        "getAllPhotos",
                        "Processing item $counter: Name = $name, Path = $path, Size = $size, Date = $date"
                    )

                    if (size > 0) {
                        photoList.add(
                            MediaInfoModel(
                                uri = uri,
                                name = name,
                                size = size,
                                date = date,
                                mediaType = MediaTypeEnum.PHOTOS
                            )
                        )
                        Log.d("getAllPhotos", "Added item to photoList: $name")
                    } else {
                        Log.w("getAllPhotos", "Skipped file with size 0: $path")
                    }
                    counter++
                }
                Log.d("getAllPhotos", "Total items processed: $counter")
            } ?: run {
                Log.e("getAllPhotos", "Cursor is null, failed to fetch photos")
            }
        }

        withContext(Dispatchers.Main) {
            Log.d("getAllPhotos", "Completed fetching photos, returning result")
            onCompleteFetch(photoList)
        }
    }

    override suspend fun getVideos(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit) {
        val videoList = mutableListOf<MediaInfoModel>()
        withContext(Dispatchers.IO) {
            if (!hasStorageAccessPermission(context )) return@withContext
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
            )

            val cursor = context.contentResolver.query(
                collection, projection, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC"
            )
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateModifiedIndex = it.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val data = it.getString(dataColumn)
                    val size = it.getLong(sizeColumn)
                    val duration = it.getLong(durationColumn)
                    val uri = data
                    val date = it.getLong(dateModifiedIndex)

                    if (size > 0) {
                        videoList.add(
                            MediaInfoModel(
                                uri = uri,
                                name = name,
                                size = size,
                                duration = duration,
                                date = date,
                                mediaType = MediaTypeEnum.VIDEOS
                            )
                        )
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            onCompleteFetch(videoList)
        }

    }

    override suspend fun getAudios(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit) {
        val audioList = mutableListOf<MediaInfoModel>()
        withContext(Dispatchers.IO) {
            if (!hasStorageAccessPermission(context )) return@withContext
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE
            )

            val cursor = context.contentResolver.query(
                collection, projection, null, null, MediaStore.Audio.Media.DATE_ADDED + " DESC"
            )
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val data = it.getString(dataColumn)
                    val size = it.getLong(sizeColumn)
                    val duration = it.getLong(durationColumn)
                    val uri = data

                    if (size > 0) {
                        audioList.add(
                            MediaInfoModel(
                                uri = uri,
                                name = name,
                                size = size,
                                duration = duration,
                                mediaType = MediaTypeEnum.AUDIOS
                            )
                        )
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            onCompleteFetch(audioList)

        }

    }


    override suspend fun fetchContacts(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit) {
        val contacts = mutableListOf<MediaInfoModel>()
        val uniqueContactNumbers = mutableSetOf<String>() // Set to track unique contact numbers

        withContext(Dispatchers.IO) {
            if (!hasStorageAccessPermission(context )) return@withContext
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val dataIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DATA)


                while (it.moveToNext()) {
//                    val id = it.getLong(idIndex)
//                    val name = it.getString(nameIndex)
//                    val number = it.getString(numberIndex)
//                    val path = it.getString(dataIndex)

                    val id = if (idIndex != -1) it.getLong(idIndex) else -1L
                    val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Unknown" else "Unknown"
                    val number = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
                    val path = if (dataIndex != -1) it.getString(dataIndex) ?: "" else ""

                    val normalizedNumber = number.replace("\\s+".toRegex(), "") // Removing spaces

                    // Check if the normalized contact number is already in the set
                    if (normalizedNumber.isNotEmpty() && uniqueContactNumbers.add(normalizedNumber)) { // Only add if it's a new number
                        // Create a MediaInfoModel object and add it to the list
                        // Calculate the approximate size of the contact
                        val nameSize = name.toByteArray(Charsets.UTF_8).size
                        val numberSize = number.toByteArray(Charsets.UTF_8).size
                        val totalSize = nameSize + numberSize

                        contacts.add(
                            MediaInfoModel(
                                uri = path,
                                name = name,
                                size = totalSize.toLong(),
                                contactId = id,
                                contactNumber = number,
                                mediaType = MediaTypeEnum.CONTACTS
                            )
                        )
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            onCompleteFetch(contacts)
        }
    }


    override suspend fun getDocuments(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit) {
        val documentList = mutableListOf<MediaInfoModel>()

        withContext(Dispatchers.IO) {
            if (!hasStorageAccessPermission(context)) return@withContext

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE
            )

//            // Update selection to include ZIP files explicitly by checking MIME type
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("application/%", "%.zip%") // Fetch all documents and ZIP files by name


            // Selection to include multiple document types like PPT, DOC, DOCX, PDF, ZIP, APK

            val cursor = context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateModifiedIndex = it.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val data = it.getString(dataColumn)
                    val size = it.getLong(sizeColumn)
                    val mimeType = it.getString(mimeTypeColumn)
                    val uri = data
                    val date = it.getLong(dateModifiedIndex)

                    // Check if the file is valid, considering ZIP extension
                    if (size > 0 && isValidDocumentExtension(data)) {
                        documentList.add(
                            MediaInfoModel(
                                uri = uri,
                                name = name,
                                size = size,
                                duration = null, // Duration is not applicable for documents
                                date = date,
                                mediaType = MediaTypeEnum.DOCUMENTS
                            )
                        )
                    }
                }
            }
        }

        withContext(Dispatchers.Main) {
            Log.d("getDocuments", "Completed fetching documents, returning result : ${documentList.size}")
            onCompleteFetch(documentList)
        }
    }
//
//    override suspend fun fetchAllApps(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit) {
//        val appList = mutableListOf<MediaInfoModel>()
//        withContext(Dispatchers.IO) {
//            if (!hasStorageAccessPermission(context )) return@withContext
//            val myPackageName = context.packageName
//            val intent = Intent(Intent.ACTION_MAIN, null).apply {
//                addCategory(Intent.CATEGORY_LAUNCHER)
//            }
//
//            val apps = context.packageManager.queryIntentActivities(intent, 0)
//
//
//            apps.filter { app ->
//                val packageName = app.activityInfo.packageName
//                packageName != myPackageName
//            }.map { resolveInfo ->
//                val packageName = resolveInfo.activityInfo.packageName
//                val appName = try {
//                    context.packageManager.getApplicationLabel(resolveInfo.activityInfo.applicationInfo)
//                        .toString()
//                } catch (e: Resources.NotFoundException) {
//                    packageName
//                } catch (e: Exception) {
//                    packageName
//                }
//
//
//                val path = resolveInfo.activityInfo.applicationInfo.publicSourceDir
//                val appSize = resolveInfo.activityInfo.applicationInfo.sourceDir.length.toLong()
//
//
//                val drawable = try {
//                    resolveInfo.activityInfo.loadIcon(context.packageManager)
//                } catch (e: PackageManager.NameNotFoundException) {
//                    null
//                }
//
//                if (path != null) {
//                    val file = File(path)
//                    val size: Long = file.length()
//
//                    appList.add(
//                        MediaInfoModel(
//                            uri = file.path,
//                            name = appName,
//                            size = size,
//                            appPackage = packageName,
//                            appIcon = drawable,
//                            mediaType = MediaTypeEnum.APPS
//                        )
//                    )
//                }
//            }
//        }
//        withContext(Dispatchers.Main) {
//            val sortedAppList = appList.sortedBy { it.name }
//            onCompleteFetch(sortedAppList)
//
//        }
//    }

    override suspend fun fetchAllApps(onCompleteFetch: (list: List<MediaInfoModel>) -> Unit) {
        val appList = mutableListOf<MediaInfoModel>()
        withContext(Dispatchers.IO) {
            if (!hasStorageAccessPermission(context)) return@withContext

            val myPackageName = context.packageName
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            // Query the apps with correct flags
            val apps = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

            apps.filter { app ->
                val packageName = app.activityInfo.packageName
                packageName != myPackageName
            }.forEach { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val appName = try {
                    context.packageManager.getApplicationLabel(resolveInfo.activityInfo.applicationInfo)
                        .toString()
                } catch (e: Exception) {
                    packageName
                }

                val path = resolveInfo.activityInfo.applicationInfo.publicSourceDir
                val drawable = try {
                    resolveInfo.activityInfo.loadIcon(context.packageManager)
                } catch (e: Exception) {
                    null
                }

                if (path != null) {
                    val file = File(path)
                    appList.add(
                        MediaInfoModel(
                            uri = file.path,
                            name = appName,
                            size = file.length(),
                            appPackage = packageName,
                            appIcon = drawable,
                            mediaType = MediaTypeEnum.APPS
                        )
                    )
                }
            }
        }

        withContext(Dispatchers.Main) {
            val sortedAppList = appList.sortedBy { it.name }
            onCompleteFetch(sortedAppList)
        }
    }


    private fun isValidDocumentExtension(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            val extension = file.extension.lowercase()
            extension in listOf(
                "pdf", "docx", "txt", "xlsx", "pptx", "epub", "html", "doc", "xls", "ppt","zip"
            )
        } catch (e: IOException) {
            false
        }
    }

    override suspend fun getFilesFromFolder(mediaType: String): List<File> {
        val audioFolder = File(
            Environment.getExternalStorageDirectory(), "Download/MySmartSwitch/$mediaType"
        )
        return if (audioFolder.exists() && audioFolder.isDirectory) {
            audioFolder.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
}