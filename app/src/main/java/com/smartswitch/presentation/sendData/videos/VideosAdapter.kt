package com.smartswitch.presentation.sendData.videos

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartswitch.utils.enums.MediaTypeEnum
import com.smartswitch.R
import com.smartswitch.databinding.ItemDateHeaderBinding
import com.smartswitch.databinding.ItemGalleryBinding
import com.smartswitch.databinding.ItemListViewBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForDisplaying
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.formatFileSize
import com.smartswitch.utils.extensions.loadImage
import com.smartswitch.utils.extensions.loadImage30by30
import com.smartswitch.utils.extensions.setSafeOnClickListener
import com.smartswitch.utils.extensions.visible
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideosAdapter(
    videos: List<MediaInfoModel>? = null,
    private val onMediaItemClickCallbackForSelectAll: OnMediaItemClickCallbackForSelectAll,
    private val onMediaItemClickCallbackForDisplaying: OnMediaItemClickCallbackForDisplaying,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isRootChecked = false
    var context: Context? = null

    companion object {
        const val VIEW_TYPE_DATE_HEADER = 0
        const val VIEW_TYPE_PHOTO_ITEM = 1
        const val VIEW_TYPE_PHOTO_LIST_ITEM = 2 // New type for list layout
    }

    var isGridLayout: Boolean = true
    private val groupedvideos: List<Pair<String, List<MediaInfoModel>>>

    init {
        groupedvideos = groupvideosByDate(videos)
    }

    private fun groupvideosByDate(videos: List<MediaInfoModel>?): List<Pair<String, List<MediaInfoModel>>> {
        return videos?.groupBy {
            it.date?.let { date ->
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(date * 1000))
            }
        }?.mapNotNull { (date, mediaList) ->
            date?.let { Pair(it, mediaList) }
        } ?: emptyList()

    }

    inner class DateHeaderViewHolder(val binding: ItemDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            context: Context, date: String, mediaItems: List<MediaInfoModel>, startPosition: Int,
        ) {
            binding.dateTextView.text = date
            val selectedCount = mediaItems.count { SelectedListManager.isItemSelected(it) }
            val totalCount = mediaItems.size
            binding.dateCheckbox.isChecked = selectedCount == totalCount
            binding.dateCheckbox.setBackgroundResource(
                if (binding.dateCheckbox.isChecked) R.drawable.check_circle
                else R.drawable.uncheck_circle
            )


            binding.selectItem.text = "$selectedCount ${context!!.getString(R.string.item)}"


            binding.dateCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (!binding.dateCheckbox.isPressed && !isRootChecked) return@setOnCheckedChangeListener
                isRootChecked = false

                if (isChecked) {

                    mediaItems.forEach { SelectedListManager.addSelectedMedia(it) }
                    onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                    binding.dateCheckbox.setBackgroundResource(R.drawable.check_circle)
                } else {

                    mediaItems.forEach { SelectedListManager.removeSelectedMedia(it) }
                    onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                    binding.dateCheckbox.setBackgroundResource(R.drawable.uncheck_circle)
                }

                val updatedSelectedCount =
                    mediaItems.count { SelectedListManager.isItemSelected(it) }
                binding.selectItem.text =
                    "$updatedSelectedCount ${context!!.getString(R.string.item)}"


                notifyItemRangeChanged(startPosition + 1, mediaItems.size)
            }


            binding.root.setOnClickListener {
                isRootChecked = true
                binding.dateCheckbox.isChecked = !binding.dateCheckbox.isChecked
            }
        }
    }


    inner class PhotoViewHolder(val binding: ItemGalleryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            context: Context,
            video: MediaInfoModel?,
            date: String,
            mediaItems: List<MediaInfoModel>,
        ) {
            binding.apply {
                imageView.loadImage(context = context, uri = video?.uri)
                checkbox.isChecked = handleCheckState(video)
                checkbox.setBackgroundResource(
                    if (checkbox.isChecked) R.drawable.check_circle else R.drawable.uncheck_circle
                )

                if (video?.mediaType == MediaTypeEnum.VIDEOS) {
                    videoIcon.visible()
                }

                checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (!binding.checkbox.isPressed && !isRootChecked) return@setOnCheckedChangeListener
                    isRootChecked = false

                    if (isChecked) {
                        SelectedListManager.addSelectedMedia(video)
                        checkbox.setBackgroundResource(R.drawable.check_circle)
                    } else {
                        SelectedListManager.removeSelectedMedia(video)
                        checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                    }
                    onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()

                    // Check if all items for the date are still selected
                    updateDateHeaderCheckboxState(date, mediaItems)
                }

//                root.setOnClickListener {
//                    isRootChecked = true
//                    checkbox.isChecked = !checkbox.isChecked
//                }

                binding.root.setSafeOnClickListener {
                    onMediaItemClickCallbackForDisplaying.onMediaItemClickedForDisplaying(video!!)
                }



                binding.root.setOnLongClickListener {
                    val modifiedOn = video?.date?.let { date ->
                        SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.getDefault()
                        ).format(Date(date * 1000))
                    }

                    Log.d("DocumentsAdapter", "Long Clicked : $video")
                    AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.properties))
                        .setMessage(
                            "${context.getString(R.string.title)} : ${video?.name} " +
                                    "\n${context.getString(R.string.modified_on)}: $modifiedOn " +
                                    "\n${context.getString(R.string.size)} : ${video?.size?.formatFileSize()} " +
                                    "\n${context.getString(R.string.type)} : ${video?.mediaType} " +
                                    "\n${context.getString(R.string.location)} : ${video?.uri} "
                        )
                        .setPositiveButton(context.getString(R.string.ok), null)
                        .create()
                        .show()
                    true
                }
            }
        }
    }


    inner class VideoListViewHolder(val binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            context: Context,
            video: MediaInfoModel?,
            date: String,
            mediaItems: List<MediaInfoModel>,
        ) {
            binding.imageView.loadImage30by30(context = context, uri = video?.uri)
            binding.mainTextView.text = video?.name
            binding.subTextView.text = formatFileSize(video?.size ?: 0L)
            binding.checkbox.isChecked = handleCheckState(video)
            binding.checkbox.setBackgroundResource(
                if (binding.checkbox.isChecked) R.drawable.check_circle else R.drawable.uncheck_circle
            )
            if (video?.mediaType == MediaTypeEnum.VIDEOS) {
                binding.videoIcon.visible()
            }

            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (!binding.checkbox.isPressed && !isRootChecked) return@setOnCheckedChangeListener
                isRootChecked = false

                if (isChecked) {
                    SelectedListManager.addSelectedMedia(video)
                    binding.checkbox.setBackgroundResource(R.drawable.check_circle)
                } else {
                    SelectedListManager.removeSelectedMedia(video)
                    binding.checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                }
                onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                updateDateHeaderCheckboxState(date, mediaItems)
            }

//            binding.root.setOnClickListener {
//                isRootChecked = true
//                binding.checkbox.isChecked = !binding.checkbox.isChecked
//            }

            binding.root.setSafeOnClickListener {
                onMediaItemClickCallbackForDisplaying.onMediaItemClickedForDisplaying(video!!)
            }

            binding.root.setOnLongClickListener {
                val modifiedOn = video?.date?.let { date ->
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(date * 1000))
                }

                Log.d("DocumentsAdapter", "Long Clicked : $video")
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.properties))
                    .setMessage(
                        "${context.getString(R.string.title)} : ${video?.name} " +
                                "\n${context.getString(R.string.modified_on)}: $modifiedOn " +
                                "\n${context.getString(R.string.size)} : ${video?.size?.formatFileSize()} " +
                                "\n${context.getString(R.string.type)} : ${video?.mediaType} " +
                                "\n${context.getString(R.string.location)} : ${video?.uri} "
                    )
                    .setPositiveButton(context.getString(R.string.ok), null)
                    .create()
                    .show()
                true
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val binding = ItemDateHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                context = parent.context
                DateHeaderViewHolder(binding)
            }

            VIEW_TYPE_PHOTO_ITEM -> {
                val binding =
                    ItemGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                context = parent.context
                PhotoViewHolder(binding)
            }

            VIEW_TYPE_PHOTO_LIST_ITEM -> {
                val binding =
                    ItemListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                context = parent.context
                VideoListViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int {
        return groupedvideos.sumOf { it.second.size + 1 } // +1 for the date header
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var currentPosition = position

        // Iterate through grouped videos to find the correct binding
        for (group in groupedvideos) {
            if (currentPosition == 0) {
                context?.let {
                    (holder as DateHeaderViewHolder).bind(it,group.first, group.second, position)
                }
                return
            }

            currentPosition -= 1

            if (currentPosition < group.second.size) {
                val mediaItem = group.second[currentPosition]
                context?.let {
                    if (isGridLayout) {
                        (holder as PhotoViewHolder).bind(
                            context = it,
                            video = mediaItem,
                            date = group.first,
                            mediaItems = group.second
                        )
                    } else {
                        (holder as VideoListViewHolder).bind(
                            context = it,
                            video = mediaItem,
                            date = group.first,
                            mediaItems = group.second
                        )
                    }
                }
                return
            }

            currentPosition -= group.second.size
        }
    }


    override fun getItemViewType(position: Int): Int {
        var currentPosition = position
        for (group in groupedvideos) {
            if (currentPosition == 0) {
                return VIEW_TYPE_DATE_HEADER
            }
            currentPosition -= 1
            if (currentPosition < group.second.size) {
                return if (isGridLayout) VIEW_TYPE_PHOTO_ITEM else VIEW_TYPE_PHOTO_LIST_ITEM
            }
            currentPosition -= group.second.size
        }
        return VIEW_TYPE_PHOTO_ITEM // Default
    }

    fun handleCheckState(item: MediaInfoModel?): Boolean {
        return SelectedListManager.isItemSelected(item)
    }


    private fun updateDateHeaderCheckboxState(date: String, mediaItems: List<MediaInfoModel>) {
        // Check if all media items for the date are selected
        val areAllItemsSelected = mediaItems.all { SelectedListManager.isItemSelected(it) }

        // Find the position of the header for this date
        val headerPosition = groupedvideos.indexOfFirst { it.first == date }

        Log.d(
            "HeaderPosition",
            "Date: $date, Header Position: $headerPosition, All Selected: $areAllItemsSelected"
        )

        if (headerPosition != -1) {
            // Calculate the size of items preceding the header
            val precedingItemsCount = groupedvideos.take(headerPosition).sumOf { it.second.size }

            Log.d("PrecedingItemsCount", "Items before header for date $date: $precedingItemsCount")

            Handler(Looper.getMainLooper()).post {
                notifyItemChanged(headerPosition + precedingItemsCount)
            }
        } else {
            Log.e("HeaderPosition", "No header found for date: $date")
        }
    }


    @SuppressLint("DefaultLocale")
    fun formatFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "${sizeInBytes} Bytes"
            sizeInBytes < 1048576 -> String.format("%.2f KB", sizeInBytes / 1024.0)
            else -> String.format("%.2f MB", sizeInBytes / 1048576.0)
        }
    }

}
