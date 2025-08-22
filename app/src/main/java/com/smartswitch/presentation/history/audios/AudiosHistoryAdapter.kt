package com.smartswitch.presentation.history.audios

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.ItemListViewBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManagerForDeletion
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.formatFileSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudiosHistoryAdapter(
    private val onMediaItemClickCallbackForSelectAll: OnMediaItemClickCallbackForSelectAll,
) : RecyclerView.Adapter<AudiosHistoryAdapter.AudioHistoryViewHolder>() {

    private var audioHistoryList: MutableList<MediaInfoModel> = mutableListOf()
    var isRootChecked = false
    var context: Context? = null


    inner class AudioHistoryViewHolder(private val binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(audio: MediaInfoModel?) {

            binding.apply {
                // Use the context from the binding instead of the one from the CoroutineScope
                CoroutineScope(Dispatchers.IO).launch {
                    val metadata = getAudioMetadata(
                        binding.root.context,
                        Uri.parse(audio?.uri)
                    ) // Use binding.root.context here
                    withContext(Dispatchers.Main) {
                        //subTextView.text = metadata[context!!.getString(R.string.artist)]
                        subTextView.text = "${audio?.size?.formatFileSize()} ≈ storage${audio?.uri?.substringAfter("0")}"
                    }
                }
            }

            Log.d("AudiosHistoryAdapter", "bind: ${audio?.uri}")
            binding.mainTextView.text = audio?.name
                ?: context!!.getString(R.string.unknown_audio) // Assuming you have a TextView for audio name
            binding.imageView.setImageResource(R.drawable.ic_music)
            binding.checkbox.isChecked = SelectedListManagerForDeletion.isItemSelected(audio)
            binding.checkbox.setBackgroundResource(
                if (binding.checkbox.isChecked) R.drawable.check_circle else R.drawable.uncheck_circle
            )

            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (!binding.checkbox.isPressed && !isRootChecked) return@setOnCheckedChangeListener

                isRootChecked = false

                if (isChecked) {
                    SelectedListManagerForDeletion.addSelectedMedia(audio)
                    binding.checkbox.setBackgroundResource(R.drawable.check_circle)
                } else {
                    SelectedListManagerForDeletion.removeSelectedMedia(audio)
                    binding.checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                }

                onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
            }

            binding.root.setOnClickListener {
                isRootChecked = true
                binding.checkbox.isChecked = !binding.checkbox.isChecked
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioHistoryViewHolder {
        val binding =
            ItemListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return AudioHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioHistoryViewHolder, position: Int) {
        Log.d(
            "AudiosHistoryAdapter",
            "Binding item at position $position: ${audioHistoryList[position]}"
        )
        holder.bind(audioHistoryList[position])
    }

    override fun getItemCount(): Int {
        return audioHistoryList.size
    }

    fun updateData(newAudioHistoryList: List<MediaInfoModel>) {
        Log.d("AudiosHistoryAdapter", "New data for adapter: $newAudioHistoryList")
        audioHistoryList.clear()
        audioHistoryList = newAudioHistoryList.toMutableList()
        notifyDataSetChanged() // You can optimize this with DiffUtil if needed
    }

    fun selectAllMedia(
        isChecked: Boolean,
        audioList: List<MediaInfoModel>,
        lifecycleScope: CoroutineScope,
    ) {
        lifecycleScope.launch {
            // Update the selection state for each item in the list
            audioList.forEach { audio ->
                if (isChecked) {
                    // Mark as selected and add to the selected list manager
                    audio.isSelected = true
                    SelectedListManagerForDeletion.addSelectedMedia(audio)
                } else {
                    // Mark as not selected and remove from the selected list manager
                    audio.isSelected = false
                    SelectedListManagerForDeletion.removeSelectedMedia(audio)
                }
            }

            // Notify the adapter to reflect the updated data
            notifyDataSetChanged()
        }

        // Trigger the callback after updating the selection state
        onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
    }


    private fun handleCheckState(item: MediaInfoModel?): Boolean {
        return SelectedListManagerForDeletion.isItemSelected(item)
    }

    @SuppressLint("Recycle")
    fun getAudioMetadata(context: Context, uri: Uri): Map<String, String> {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme == "content") {
                val fileDescriptor =
                    context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
                if (fileDescriptor != null) {
                    retriever.setDataSource(fileDescriptor)
                } else {
                    throw IllegalArgumentException("FileDescriptor is null for URI: $uri")
                }
            } else {
                retriever.setDataSource(context, uri)
            }

            // Extract metadata
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: context.getString(R.string.unknown_title)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: context.getString(R.string.unknown_artist)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?: context.getString(R.string.unknown_album)

            // Return metadata as a map
            mapOf(
                context.getString(R.string.title) to title,
                context.getString(R.string.artist) to artist,
                context.getString(R.string.album) to album
            )
        } catch (e: Exception) {
            Log.e("AudioMetadata", "Failed to retrieve metadata for URI: $uri", e)
            mapOf(
                context.getString(R.string.title) to context.getString(R.string.unknown_title),
                context.getString(R.string.artist) to context.getString(R.string.unknown_artist),
                context.getString(R.string.album) to context.getString(R.string.unknown_album)
            )
        } finally {
            retriever.release()
        }
    }

}
