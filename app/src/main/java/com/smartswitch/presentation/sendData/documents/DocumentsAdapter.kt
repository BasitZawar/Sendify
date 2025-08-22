package com.smartswitch.presentation.sendData.documents

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.ItemListViewBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.formatFileSize
import com.smartswitch.utils.extensions.openFileFromRecyclerView
import com.smartswitch.utils.extensions.setSafeOnClickListener
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log

class DocumentsAdapter(
    private val documents: List<MediaInfoModel>? = null,
    private val onMediaItemClickCallbackForSelectAll: OnMediaItemClickCallbackForSelectAll,
) : RecyclerView.Adapter<DocumentsAdapter.PhotoViewHolder>() {

    var context: Context? = null
    var isRootChecked = false

    inner class PhotoViewHolder(val binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(document: MediaInfoModel?) {
            binding.apply {


                val formattedDate = document?.date?.let { date ->
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(date * 1000))
                }
                subTextView.text = "${document?.size?.formatFileSize()}, ${context!!.getString(R.string.modified)} ${formattedDate}"
                mainTextView.text = document?.name



                if (document?.name?.endsWith(".zip") == true) {
                    imageView.setImageResource(R.drawable.ic_zip)
                } else if (document?.name?.endsWith(".pdf") == true || document?.name?.endsWith(".PDF") == true) {
                    imageView.setImageResource(R.drawable.ic_pdf)
                } else if (document?.name?.endsWith(".docx") == true || document?.name?.endsWith(".doc") == true) {
                    imageView.setImageResource(R.drawable.ic_docs)
                } else if (document?.name?.endsWith(".ppt") == true || document?.name?.endsWith(".pptx") == true) {
                    imageView.setImageResource(R.drawable.ic_ppt)
                } else {
                    imageView.setImageResource(R.drawable.ic_doc)
                }


                checkbox.isChecked = handleCheckState(document)
                if (checkbox.isChecked) {
                    checkbox.setBackgroundResource(R.drawable.check_circle)
                } else {
                    checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                }
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (!binding.checkbox.isPressed && !isRootChecked) return@setOnCheckedChangeListener
                    isRootChecked = false
                    if (isChecked) {
                        SelectedListManager.addSelectedMedia(document)
                        checkbox.setBackgroundResource(R.drawable.check_circle)
                    } else {
                        SelectedListManager.removeSelectedMedia(document)
                        checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                    }
                    onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                }

//                root.setOnClickListener {
//                    isRootChecked = true
//                    checkbox.isChecked = !checkbox.isChecked
//                }

                root.setSafeOnClickListener {
                    //openDocumentFromRecyclerView(document?.uri.toString())
                    context!!.openFileFromRecyclerView(document?.uri.toString())
                }

                root.setOnLongClickListener {
                    val modifiedOn = document?.date?.let { date ->
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(date * 1000))
                    }

                    Log.d("DocumentsAdapter", "Long Clicked : $document")
                    AlertDialog.Builder(context)
                        .setTitle(context!!.getString(R.string.properties))
                        .setMessage("${context!!.getString(R.string.title)} : ${document?.name} " +
                                "\n${context!!.getString(R.string.modified_on)}: $modifiedOn " +
                                "\n${context!!.getString(R.string.size)} : ${document?.size?.formatFileSize()} " +
                                "\n${context!!.getString(R.string.type)} : ${document?.mediaType} " +
                                "\n${context!!.getString(R.string.location)} : ${document?.uri} ")
                        .setPositiveButton(context!!.getString(R.string.ok),null)
                        .create()
                        .show()
                    true
                }
            }
        }
    }

//    private fun openDocumentFromRecyclerView(documentUri: String) {
//        // Path to your document file
//        val documentFile = File(documentUri)
//
//        if (documentFile.exists()) {
//            // Use FileProvider to get a content URI
//            val documentContentUri: Uri = FileProvider.getUriForFile(
//                context!!,
//                "${context!!.packageName}.fileprovider",
//                documentFile
//            )
//
//            // Get the MIME type based on the file extension
//            val mimeType = when (documentFile.extension.lowercase()) {
//                "pdf" -> "application/pdf"
//                "doc", "docx" -> "application/msword"
//                "xls", "xlsx" -> "application/vnd.ms-excel"
//                "ppt", "pptx" -> "application/vnd.ms-powerpoint"
//                "txt" -> "text/plain"
//                "html" -> "text/html"
//                "zip" -> "application/zip"
//                "jpg", "jpeg", "png", "gif" -> "image/*"
//                "mp4", "mkv", "avi" -> "video/*"
//                "mp3", "wav" -> "audio/*"
//                else -> "*/*" // Default to a generic MIME type
//            }
//
//            Log.d("DocumentViewer___", "openDocumentFromRecyclerView: MIME type: $mimeType")
//
//            // Create an intent to open the document
//            val intent = Intent(Intent.ACTION_VIEW).apply {
//                setDataAndType(documentContentUri, mimeType) // Use the determined MIME type
//                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to read the file
//            }
//
//            // Start the activity with the intent
//            context!!.startActivity(Intent.createChooser(intent, "Open document with"))
//        } else {
//            println("File not found: $documentUri")
//        }
//    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding =
            ItemListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return PhotoViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return documents?.size ?: 0
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val mediaItem = documents?.get(position)
        context?.let { holder.bind(mediaItem) }

    }

    fun handleCheckState(item: MediaInfoModel?): Boolean {
        return SelectedListManager.isItemSelected(item)
    }


}