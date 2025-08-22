package com.smartswitch.presentation.history.videos

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.ItemDateHeaderBinding
import com.smartswitch.databinding.ItemListViewBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.FileUtils.formatFileSize
import com.smartswitch.utils.SelectedListManagerForDeletion

import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.enums.DateFormatType
import com.smartswitch.utils.enums.MediaTypeEnum
import com.smartswitch.utils.extensions.formatFileSize
import com.smartswitch.utils.extensions.formatTo
import com.smartswitch.utils.extensions.loadImage30by30
import com.smartswitch.utils.extensions.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class VideosHistoryAdapter(
    private val onMediaItemClickCallbackForSelectAll: OnMediaItemClickCallbackForSelectAll
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var videoHistoryList: List<MediaInfoModel> = emptyList()

    companion object {
        const val VIEW_TYPE_DATE_HEADER = 0
        const val VIEW_TYPE_VIDEO_ITEM = 1
    }

    var isGridLayout: Boolean = true
    var isRootChecked = false
    private var groupedVideos: List<Pair<String, List<MediaInfoModel>>> = emptyList()

    init {
        groupedVideos = groupVideosByDate(videoHistoryList)
    }


    private fun groupVideosByDate(videos: List<MediaInfoModel>): List<Pair<String, List<MediaInfoModel>>> {
        Log.d("VideosHistoryAdapter___", "Original Videos list size: ${videos.size}")
        videos.forEachIndexed { index, video ->
            Log.d("VideosHistoryAdapter___", "Video at index $index: date=${video.date}, id=${video.name}")
        }
        val groupedVideos = videos.groupBy { media ->
            media.date?.let { date ->
                val formattedDate = date.formatTo(DateFormatType.DAY_MONTH_YEAR)
                Log.d("VideosHistoryAdapter___", "Formatted date: $formattedDate")
                formattedDate
            }
        }

        groupedVideos.forEach { (date, mediaList) ->
            Log.d("VideosHistoryAdapter___", "Date: $date has ${mediaList.size} videos")
        }

        // Map the grouped photos to result with a Pair of date and list
        val result = groupedVideos.mapNotNull { (date, mediaList) ->
            date?.let {
                Pair(it, mediaList).also {
                    Log.d("VideosHistoryAdapter___", "Grouped date: $date with ${mediaList.size} items")
                }
            }
        }
        Log.d("VideosHistoryAdapter___", "Grouped result size: ${result.size}")
        return result
    }

    inner class DateHeaderViewHolder(private val binding: ItemDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(date: String) {
            binding.dateTextView.text = date
        }
    }

    inner class VideoViewHolder(val binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            context: Context,
            video: MediaInfoModel?,
            date: String,
            mediaItems: List<MediaInfoModel>
        ) {
            binding.imageView.loadImage30by30(context = context, uri = video?.uri)
            binding.mainTextView.text = video?.name
            //binding.subTextView.text = formatFileSize(video?.size ?: 0L)
            binding.subTextView.text = "${formatFileSize(video?.size ?: 0L)} ≈ storage${video?.uri?.substringAfter("0")}"
            binding.checkbox.isChecked = SelectedListManagerForDeletion.isItemSelected(video)
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
                    SelectedListManagerForDeletion.addSelectedMedia(video)
                    binding.checkbox.setBackgroundResource(R.drawable.check_circle)
                } else {
                    SelectedListManagerForDeletion.removeSelectedMedia(video)
                    binding.checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                }

                onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                updateDateHeaderCheckboxState(date, mediaItems)
            }

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
            VIEW_TYPE_VIDEO_ITEM -> {
                val binding = ItemListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                VideoViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var currentPosition = position
        for (group in groupedVideos) {
            if (currentPosition == 0) {
                (holder as DateHeaderViewHolder).bind(group.first)
                return
            }
            currentPosition -= 1
            if (currentPosition < group.second.size) {
                // Cast the holder to VideoViewHolder and bind photo data
                (holder as VideoViewHolder).bind(
                    context = holder.itemView.context,
                    video = group.second[currentPosition],
                    date = group.first,
                    mediaItems = group.second
                )
                return
            }
            currentPosition -= group.second.size
        }
    }

    override fun getItemCount(): Int {
        return groupedVideos.sumOf { it.second.size + 1 }
    }

    override fun getItemViewType(position: Int): Int {
        var currentPosition = position
        for (group in groupedVideos) {
            if (currentPosition == 0) {
                return VIEW_TYPE_DATE_HEADER
            }
            currentPosition -= 1
            if (currentPosition < group.second.size) {
                return VIEW_TYPE_VIDEO_ITEM
            }
            currentPosition -= group.second.size
        }
        return VIEW_TYPE_VIDEO_ITEM
    }

    fun updateData(newVideoHistoryList: List<MediaInfoModel>) {
        videoHistoryList = newVideoHistoryList
        groupedVideos = groupVideosByDate(newVideoHistoryList)
        notifyDataSetChanged()
    }

    private fun handleCheckState(item: MediaInfoModel?): Boolean {
        return SelectedListManagerForDeletion.isItemSelected(item)
    }


    private fun updateDateHeaderCheckboxState(date: String, mediaItems: List<MediaInfoModel>) {

    }
    fun selectAllMedia(isChecked: Boolean, videoList: List<MediaInfoModel>, lifecycleScope: CoroutineScope) {
        lifecycleScope.launch {
            videoList.forEach { video ->
                if (isChecked) {
                    SelectedListManagerForDeletion.addSelectedMedia(video)
                } else {
                    SelectedListManagerForDeletion.removeSelectedMedia(video)
                }
            }
            notifyDataSetChanged()
        }
        onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
    }
}
