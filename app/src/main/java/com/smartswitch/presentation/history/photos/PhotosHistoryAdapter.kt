package com.smartswitch.presentation.history.photos

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.ItemDateHeaderBinding
import com.smartswitch.databinding.ItemListViewBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManagerForDeletion
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.enums.DateFormatType
import com.smartswitch.utils.enums.MediaTypeEnum
import com.smartswitch.utils.extensions.formatFileSize
import com.smartswitch.utils.extensions.formatTo
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.loadImage30by30
import com.smartswitch.utils.extensions.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class PhotosHistoryAdapter(
    private val onMediaItemClickCallbackForSelectAll: OnMediaItemClickCallbackForSelectAll
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var groupedPhotosHistoryList: List<Pair<String, List<MediaInfoModel>>> = emptyList()
    var isRootChecked = false
    var isGridLayout: Boolean = true

    companion object {
        const val VIEW_TYPE_DATE_HEADER = 0
        const val VIEW_TYPE_PHOTO_ITEM = 1
    }

    private var photosHistory: List<MediaInfoModel> = emptyList()

    init {
        groupedPhotosHistoryList = groupPhotosByDate(photosHistory)
    }

    inner class DateHeaderViewHolder(private val binding: ItemDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(date: String) {
            binding.dateTextView.text = date
        }
    }

    // Modified PhotoListViewHolder class
    inner class PhotoListViewHolder(private val binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            context: Context,
            photo: MediaInfoModel?,
            date: String,
            mediaItems: List<MediaInfoModel>
        ) {
            binding.imageView.loadImage30by30(context = context, uri = photo?.uri)
            binding.mainTextView.text = photo?.name
            binding.subTextView.text = "${photo?.size?.formatFileSize()} ≈ storage${photo?.uri?.substringAfter("0")}"
            binding.checkbox.isChecked = SelectedListManagerForDeletion.isItemSelected(photo)
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
                    SelectedListManagerForDeletion.addSelectedMedia(photo)
                    binding.checkbox.setBackgroundResource(R.drawable.check_circle)
                } else {
                    SelectedListManagerForDeletion.removeSelectedMedia(photo)
                    binding.checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                }
                onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                updateDateHeaderCheckboxState(date, mediaItems)
            }
            onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()

            binding.root.setOnClickListener {
                isRootChecked = true
                binding.checkbox.isChecked = !binding.checkbox.isChecked
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val binding = ItemDateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DateHeaderViewHolder(binding)
            }
            VIEW_TYPE_PHOTO_ITEM -> {
                val binding = ItemListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PhotoListViewHolder(binding) // Use PhotoListViewHolder here
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var currentPosition = position
        for (group in groupedPhotosHistoryList) {
            if (currentPosition == 0) {
                (holder as DateHeaderViewHolder).bind(group.first)
                return
            }
            currentPosition -= 1
            if (currentPosition < group.second.size) {
                // Cast the holder to PhotoListViewHolder and bind photo data
                (holder as PhotoListViewHolder).bind(
                    context = holder.itemView.context,
                    photo = group.second[currentPosition],
                    date = group.first,
                    mediaItems = group.second
                )
                return
            }
            currentPosition -= group.second.size
        }
    }

    override fun getItemCount(): Int {
        return groupedPhotosHistoryList.sumOf { it.second.size + 1 }
    }

    override fun getItemViewType(position: Int): Int {
        var currentPosition = position
        for (group in groupedPhotosHistoryList) {
            if (currentPosition == 0) {
                return VIEW_TYPE_DATE_HEADER
            }
            currentPosition -= 1
            if (currentPosition < group.second.size) {
                return VIEW_TYPE_PHOTO_ITEM
            }
            currentPosition -= group.second.size
        }
        return VIEW_TYPE_PHOTO_ITEM
    }

    fun updateData(newPhotoHistoryList: List<MediaInfoModel>) {
        Log.d("PhotoAdapter___", "updateData: $newPhotoHistoryList")
        photosHistory = newPhotoHistoryList
        groupedPhotosHistoryList = groupPhotosByDate(newPhotoHistoryList)
        Log.d("PhotoAdapter___", "History list size: ${newPhotoHistoryList.size}")

        Log.d("PhotoAdapter___", "groupedPhotosHistoryList: $groupedPhotosHistoryList")
        notifyDataSetChanged()
    }

    private fun groupPhotosByDate(photos: List<MediaInfoModel>): List<Pair<String, List<MediaInfoModel>>> {
        Log.d("PhotosHistoryAdapter___", "Original photos list size: ${photos.size}")

        photos.forEachIndexed { index, photo ->
            Log.d("PhotosHistoryAdapter___", "Photo at index $index: date=${photo.date}, id=${photo.name}")
        }

        val groupedPhotos = photos.groupBy { media ->
            media.date?.let { date ->
                val formattedDate = date.formatTo(DateFormatType.DAY_MONTH_YEAR)
                Log.d("PhotosHistoryAdapter___", "Formatted date: $formattedDate")
                formattedDate
            }
        }

        groupedPhotos.forEach { (date, mediaList) ->
            Log.d("PhotosHistoryAdapter___", "Date: $date has ${mediaList.size} photos")
        }

        val result = groupedPhotos.mapNotNull { (date, mediaList) ->
            date?.let {
                Pair(it, mediaList).also {
                    Log.d("PhotosHistoryAdapter___", "Grouped date: $date with ${mediaList.size} items")
                }
            }
        }

        Log.d("PhotosHistoryAdapter___", "Grouped result size: ${result.size}")
        return result
    }

    private fun handleCheckState(item: MediaInfoModel?): Boolean {
        return SelectedListManagerForDeletion.isItemSelected(item)
    }

    private fun updateDateHeaderCheckboxState(date: String, mediaItems: List<MediaInfoModel>) {
        val allSelected = mediaItems.all { SelectedListManagerForDeletion.isItemSelected(it)
    }
}

    fun selectAllMedia(isChecked: Boolean, photoList: List<MediaInfoModel>, lifecycleScope: CoroutineScope) {
        lifecycleScope.launch {
            if (isChecked) {
                photoList.forEach { photo ->
                    SelectedListManagerForDeletion.addSelectedMedia(photo)
                }
            } else {
                photoList.forEach { photo ->
                    SelectedListManagerForDeletion.removeSelectedMedia(photo)
                }
            }
            notifyDataSetChanged()
        }
    }



}
