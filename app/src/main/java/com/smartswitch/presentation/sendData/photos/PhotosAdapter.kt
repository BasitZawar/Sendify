package com.smartswitch.presentation.sendData.photos

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.ItemDateHeaderBinding
import com.smartswitch.databinding.ItemGalleryBinding
import com.smartswitch.databinding.ItemListViewBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForDisplaying
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.enums.MediaTypeEnum
import com.smartswitch.utils.extensions.formatFileSize
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.loadImage
import com.smartswitch.utils.extensions.loadImage30by30
import com.smartswitch.utils.extensions.setSafeOnClickListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class PhotosAdapter(
    photos: List<MediaInfoModel>? = null,
    private val onMediaItemClickCallbackForSelectAll: OnMediaItemClickCallbackForSelectAll,
    private val onMediaItemClickCallbackForDisplaying: OnMediaItemClickCallbackForDisplaying,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isRootChecked = false
    var context: Context? = null

    companion object {
        const val VIEW_TYPE_DATE_HEADER = 0
        const val VIEW_TYPE_PHOTO_ITEM = 1
        const val VIEW_TYPE_PHOTO_LIST_ITEM = 2
    }

    var isGridLayout: Boolean = true
    private val groupedPhotos: List<Pair<String, List<MediaInfoModel>>>

    init {
        groupedPhotos = groupPhotosByDate(photos)
    }

    private fun groupPhotosByDate(photos: List<MediaInfoModel>?): List<Pair<String, List<MediaInfoModel>>> {
        return photos?.groupBy {
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
                    //onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                    binding.dateCheckbox.setBackgroundResource(R.drawable.check_circle)
                } else {
                    mediaItems.forEach { SelectedListManager.removeSelectedMedia(it) }
                    //onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                    binding.dateCheckbox.setBackgroundResource(R.drawable.uncheck_circle)
                }

                val updatedSelectedCount =
                    mediaItems.count { SelectedListManager.isItemSelected(it) }
                binding.selectItem.text =
                    "$updatedSelectedCount ${context!!.getString(R.string.item)}"

                notifyItemRangeChanged(startPosition + 1, mediaItems.size)
                onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
            }
            // onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()

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
            photo: MediaInfoModel?,
            date: String,
            mediaItems: List<MediaInfoModel>,
        ) {
            binding.apply {
                imageView.loadImage(context = context, uri = photo?.uri)
                checkbox.isChecked = handleCheckState(photo)
                checkbox.setBackgroundResource(
                    if (checkbox.isChecked) R.drawable.check_circle else R.drawable.uncheck_circle
                )


                if (photo?.mediaType == MediaTypeEnum.PHOTOS) {
                    videoIcon.gone()
                }

                checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (!binding.checkbox.isPressed && !isRootChecked) return@setOnCheckedChangeListener
                    isRootChecked = false

                    if (isChecked) {
                        SelectedListManager.addSelectedMedia(photo)
                        checkbox.setBackgroundResource(R.drawable.check_circle)
                    } else {
                        SelectedListManager.removeSelectedMedia(photo)
                        checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                    }
                    onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                    updateDateHeaderCheckboxState(date, mediaItems)
                }
                //onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()

                root.setSafeOnClickListener {
                    onMediaItemClickCallbackForDisplaying.onMediaItemClickedForDisplaying(photo!!)
                    // openFullViewWallpaper(photo.name.toString(), photo.uri.toString())
                }

//                binding.checkbox.setOnClickListener {
//                    isRootChecked = true
//                    binding.checkbox.isChecked = !binding.checkbox.isChecked
//                }

                root.setOnLongClickListener {
                    val modifiedOn = photo?.date?.let { date ->
                        SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.getDefault()
                        ).format(Date(date * 1000))
                    }

                    Log.d("DocumentsAdapter", "Long Clicked : $photo")
                    AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.properties))
                        .setMessage(
                            "${context.getString(R.string.title)} : ${photo?.name} " +
                                    "\n${context.getString(R.string.modified_on)}: $modifiedOn " +
                                    "\n${context.getString(R.string.size)} : ${photo?.size?.formatFileSize()} " +
                                    "\n${context.getString(R.string.type)} : ${photo?.mediaType} " +
                                    "\n${context.getString(R.string.location)} : ${photo?.uri} "
                        )
                        .setPositiveButton(context.getString(R.string.ok), null)
                        .create()
                        .show()
                    true
                }
            }
        }
    }

    inner class PhotoListViewHolder(val binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            context: Context,
            photo: MediaInfoModel?,
            date: String,
            mediaItems: List<MediaInfoModel>,
        ) {
            binding.imageView.loadImage30by30(context = context, uri = photo?.uri)
            binding.mainTextView.text = photo?.name
            binding.subTextView.text = photo?.size?.formatFileSize()
            binding.checkbox.isChecked = handleCheckState(photo)
            binding.checkbox.setBackgroundResource(
                if (binding.checkbox.isChecked) R.drawable.check_circle else R.drawable.uncheck_circle
            )
            if (photo?.mediaType == MediaTypeEnum.PHOTOS) {
                binding.videoIcon.gone()
            }

            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (!binding.checkbox.isPressed && !isRootChecked) return@setOnCheckedChangeListener
                isRootChecked = false

                if (isChecked) {
                    SelectedListManager.addSelectedMedia(photo)
                    binding.checkbox.setBackgroundResource(R.drawable.check_circle)
                } else {
                    SelectedListManager.removeSelectedMedia(photo)
                    binding.checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                }
                onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                updateDateHeaderCheckboxState(date, mediaItems)
            }

            //onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
            binding.root.setSafeOnClickListener {
                onMediaItemClickCallbackForDisplaying.onMediaItemClickedForDisplaying(photo!!)
            }
//            binding.linearLayout.setOnClickListener {
//                onMediaItemClickCallbackForDisplaying.onMediaItemClickedForDisplaying(photo!!)
//            }


//            binding.checkbox.setOnClickListener {
//                isRootChecked = true
//                binding.checkbox.isChecked = !binding.checkbox.isChecked
//            }

            binding.root.setOnLongClickListener {
                val modifiedOn = photo?.date?.let { date ->
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(date * 1000))
                }

                Log.d("DocumentsAdapter", "Long Clicked : $photo")
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.properties))
                    .setMessage(
                        "${context.getString(R.string.title)} : ${photo?.name} " +
                                "\n${context.getString(R.string.modified_on)}: $modifiedOn " +
                                "\n${context.getString(R.string.size)} : ${photo?.size?.formatFileSize()} " +
                                "\n${context.getString(R.string.type)} : ${photo?.mediaType} " +
                                "\n${context.getString(R.string.location)} : ${photo?.uri} "
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
                val binding = ItemGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                context = parent.context
                PhotoViewHolder(binding)
            }

            VIEW_TYPE_PHOTO_LIST_ITEM -> {
                val binding = ItemListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                context = parent.context
                PhotoListViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int {
        return groupedPhotos.sumOf { it.second.size + 1 } // +1 for the date header
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var currentPosition = position

        // Iterate through grouped photos to find the correct binding
        for (group in groupedPhotos) {
            if (currentPosition == 0) {
                context?.let {
                    (holder as DateHeaderViewHolder).bind(it, group.first, group.second, position)
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
                            photo = mediaItem,
                            date = group.first,
                            mediaItems = group.second
                        )
                    } else {
                        (holder as PhotoListViewHolder).bind(
                            context = it,
                            photo = mediaItem,
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
        for (group in groupedPhotos) {
            if (currentPosition == 0) {
                return VIEW_TYPE_DATE_HEADER
            }
            currentPosition -= 1
            if (currentPosition < group.second.size) {
                return if (isGridLayout) VIEW_TYPE_PHOTO_ITEM else VIEW_TYPE_PHOTO_LIST_ITEM
            }
            currentPosition -= group.second.size
        }
        return VIEW_TYPE_PHOTO_ITEM
    }

    fun handleCheckState(item: MediaInfoModel?): Boolean {
        return SelectedListManager.isItemSelected(item)
    }


    private fun updateDateHeaderCheckboxState(date: String, mediaItems: List<MediaInfoModel>) {
        val areAllItemsSelected = mediaItems.all { SelectedListManager.isItemSelected(it) }

        // Find the position of the header for this date
        val headerPosition = groupedPhotos.indexOfFirst { it.first == date }

        Log.d(
            "HeaderPosition",
            "Date: $date, Header Position: $headerPosition, All Selected: $areAllItemsSelected"
        )

        if (headerPosition != -1) {
            // Calculate the size of items preceding the header
            val precedingItemsCount = groupedPhotos.take(headerPosition).sumOf { it.second.size }

            Log.d("PrecedingItemsCount", "Items before header for date $date: $precedingItemsCount")

            Handler(Looper.getMainLooper()).post {
                notifyItemChanged(headerPosition + precedingItemsCount)
            }
        } else {
            Log.e("HeaderPosition", "No header found for date: $date")
        }
    }

}
