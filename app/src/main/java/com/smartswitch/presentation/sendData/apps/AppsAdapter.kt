package com.smartswitch.presentation.sendData.apps

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.ItemAppsBinding
import com.smartswitch.databinding.ItemListViewBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.presentation.sendData.photos.PhotosAdapter
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.formatFileSize
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.loadImage30by30
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppsAdapter(
    private val apps: List<MediaInfoModel>? = null,
    private val onMediaItemClickCallbackForSelectAll: OnMediaItemClickCallbackForSelectAll,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var context: Context? = null
    var isRootChecked = false
    var isGridLayout: Boolean = true

    inner class PhotoViewHolder(val binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(app: MediaInfoModel?) {
            binding.apply {
                context?.let {

                    imageView.loadImage30by30(context = it, uri = app?.appIcon)
                    mainTextView.text = app?.name
                    checkbox.isChecked = handleCheckState(app)

                    if (checkbox.isChecked) {
                        checkbox.setBackgroundResource(R.drawable.check_circle)
                    } else {
                        checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                    }

                    subTextView.gone()
                    checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (!binding.checkbox.isPressed && !isRootChecked) {

                            return@setOnCheckedChangeListener

                        }
                        isRootChecked = false
                        if (isChecked) {
                            SelectedListManager.addSelectedMedia(app)
                            checkbox.setBackgroundResource(R.drawable.check_circle)
                        } else {
                            SelectedListManager.removeSelectedMedia(app)
                            checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                        }
                        onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                    }

                    root.setOnClickListener {
                        isRootChecked = true
                        checkbox.isChecked = !checkbox.isChecked
                    }

                    root.setOnLongClickListener {
                        val modifiedOn = app?.date?.let { date ->
                            SimpleDateFormat(
                                "dd MMM yyyy",
                                Locale.getDefault()
                            ).format(Date(date * 1000))
                        }

                        Log.d("DocumentsAdapter", "Long Clicked : $app")
                        AlertDialog.Builder(context)
                            .setTitle(context!!.getString(R.string.properties))
                            .setMessage(
                                "${context!!.getString(R.string.title)} : ${app?.name} " +
                                        "\n${context!!.getString(R.string.modified_on)}: $modifiedOn " +
                                        "\n${context!!.getString(R.string.size)} : ${app?.size?.formatFileSize()} " +
                                        "\n${context!!.getString(R.string.type)} : ${app?.mediaType} " +
                                        "\n${context!!.getString(R.string.location)} : ${app?.uri} "
                            )
                            .setPositiveButton(context!!.getString(R.string.ok), null)
                            .create()
                            .show()
                        true
                    }
                }
            }
        }
    }


    inner class PhotoGridViewHolder(val binding: ItemAppsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(app: MediaInfoModel?) {
            binding.apply {
                context?.let {

                    imageView.loadImage30by30(context = it, uri = app?.appIcon)
                    name.text = app?.name
                    checkbox.isChecked = handleCheckState(app)

                    if (checkbox.isChecked) {
                        checkbox.setBackgroundResource(R.drawable.check_circle)
                    } else {
                        checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                    }

                    // subTextView.gone()
                    size.text = app?.size?.formatFileSize()
                    checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (!binding.checkbox.isPressed && !isRootChecked) {

                            return@setOnCheckedChangeListener

                        }
                        isRootChecked = false
                        if (isChecked) {
                            SelectedListManager.addSelectedMedia(app)
                            checkbox.setBackgroundResource(R.drawable.check_circle)
                        } else {
                            SelectedListManager.removeSelectedMedia(app)
                            checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                        }
                        onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                    }

                    root.setOnClickListener {
                        isRootChecked = true
                        checkbox.isChecked = !checkbox.isChecked
                    }

                    root.setOnLongClickListener {
                        val modifiedOn = app?.date?.let { date ->
                            SimpleDateFormat(
                                "dd MMM yyyy",
                                Locale.getDefault()
                            ).format(Date(date * 1000))
                        }

                        Log.d("DocumentsAdapter", "Long Clicked : $app")
                        AlertDialog.Builder(context)
                            .setTitle(context!!.getString(R.string.properties))
                            .setMessage(
                                "${context!!.getString(R.string.title)} : ${app?.name} " +
                                        "\n${context!!.getString(R.string.modified_on)}: $modifiedOn " +
                                        "\n${context!!.getString(R.string.size)} : ${app?.size?.formatFileSize()} " +
                                        "\n${context!!.getString(R.string.type)} : ${app?.mediaType} " +
                                        "\n${context!!.getString(R.string.location)} : ${app?.uri} "
                            )
                            .setPositiveButton(context!!.getString(R.string.ok), null)
                            .create()
                            .show()
                        true
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {

            PhotosAdapter.VIEW_TYPE_PHOTO_ITEM -> {
                val binding =
                    ItemAppsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                context = parent.context
                PhotoGridViewHolder(binding)
            }

            PhotosAdapter.VIEW_TYPE_PHOTO_LIST_ITEM -> {
                val binding =
                    ItemListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                context = parent.context
                PhotoViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type $viewType")
        }
    }

    override fun getItemCount(): Int {
        return apps?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mediaItem = apps?.get(position)
        context?.let {
            if (isGridLayout) {
                (holder as PhotoGridViewHolder).bind(mediaItem)
            } else {
                (holder as PhotoViewHolder).bind(mediaItem)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isGridLayout) {
            PhotosAdapter.VIEW_TYPE_PHOTO_ITEM
        } else {
            PhotosAdapter.VIEW_TYPE_PHOTO_LIST_ITEM
        }
    }

    fun handleCheckState(item: MediaInfoModel?): Boolean {
        return SelectedListManager.isItemSelected(item)
    }
}