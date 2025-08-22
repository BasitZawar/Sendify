package com.smartswitch.presentation.sendData.audios

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForDisplaying
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.formatFileSize
import com.smartswitch.utils.extensions.setSafeOnClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudiosAdapter(
    private val audios: List<MediaInfoModel>? = null,
    private val onMediaItemClickCallbackForSelectAll: OnMediaItemClickCallbackForSelectAll,
    private val onMediaItemClickCallbackForDisplaying: OnMediaItemClickCallbackForDisplaying,
) : RecyclerView.Adapter<AudiosAdapter.AudioViewHolder>() {

    var context: Context? = null
    var isRootChecked = false

    inner class AudioViewHolder(val binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, audio: MediaInfoModel?) {
            binding.apply {

                CoroutineScope(Dispatchers.IO).launch {
                    val metadata = getAudioMetadata(context, Uri.parse(audio?.uri))
                    withContext(Dispatchers.Main) {
                        subTextView.text = metadata[context.getString(R.string.artist)]
                    }
                    Log.d("dingDong", "AudioViewHolder bound for: ${audio?.name}") // ← Log here safely
                }

                mainTextView.text = audio?.name
                imageView.setImageResource(R.drawable.ic_music)
                checkbox.isChecked = handleCheckState(audio)
                if (checkbox.isChecked) {
                    checkbox.setBackgroundResource(R.drawable.check_circle)
                } else {
                    checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                }

                checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (!binding.checkbox.isPressed) {
                        if (!isRootChecked) {
                            return@setOnCheckedChangeListener
                        }
                    }
                    isRootChecked = false
                    if (isChecked) {
                        SelectedListManager.addSelectedMedia(audio)
                        checkbox.setBackgroundResource(R.drawable.check_circle)
                    } else {
                        SelectedListManager.removeSelectedMedia(audio)
                        checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                    }
                    onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                }

//                root.setOnClickListener {
//                    isRootChecked = true
//                    checkbox.isChecked = !checkbox.isChecked
//                }
                root.setSafeOnClickListener {
                    onMediaItemClickCallbackForDisplaying.onMediaItemClickedForDisplaying(audio!!)
                }

                root.setOnLongClickListener {
                    val modifiedOn = audio?.date?.let { date ->
                        SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.getDefault()
                        ).format(Date(date * 1000))
                    }

                    Log.d("DocumentsAdapter", "Long Clicked : $audio")
                    AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.properties))
                        .setMessage(
                            "${context.getString(R.string.title)} : ${audio?.name} " +
                                    "\n${context.getString(R.string.modified_on)}: $modifiedOn " +
                                    "\n${context.getString(R.string.size)} : ${audio?.size?.formatFileSize()} " +
                                    "\n${context.getString(R.string.type)} : ${audio?.mediaType} " +
                                    "\n${context.getString(R.string.location)} : ${audio?.uri} "
                        )
                        .setPositiveButton(context.getString(R.string.ok), null)
                        .create()
                        .show()
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        Log.e("refresh_behav","onCreateViewHolder")
        val binding = ItemListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return AudioViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return audios?.size ?: 0
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val mediaItem = audios?.get(position)
        context?.let {
            Log.i("refresh_behav","onBindViewHolder")
            holder.bind(context = it, mediaItem)
        }

    }

    fun handleCheckState(item: MediaInfoModel?): Boolean {
        return SelectedListManager.isItemSelected(item)
    }


    @SuppressLint("Recycle")
//    fun getAudioMetadata(context: Context, uri: Uri): Map<String, String> {
//        val retriever = MediaMetadataRetriever()
//        return try {
//
//            if (uri.scheme == "content") {
//                val fileDescriptor =
//                    context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
//                if (fileDescriptor != null) {
//                    retriever.setDataSource(fileDescriptor)
//                } else {
//                    throw IllegalArgumentException("FileDescriptor is null for URI: $uri")
//                }
//            } else {
//                retriever.setDataSource(context, uri)
//            }
//
//            // Extract metadata
//            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
//                ?: context.getString(R.string.unknown_title)
//            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
//                ?: context.getString(R.string.unknown_artist)
//            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
//                ?: context.getString(R.string.unknown_album)
//
//            // Return metadata as a map
//            mapOf(
//                context.getString(R.string.title) to title,
//                context.getString(R.string.artist) to artist,
//                context.getString(R.string.album) to album
//            )
//        } catch (e: Exception) {
//            Log.e("AudioMetadata", "Failed to retrieve metadata for URI: $uri", e)
//            mapOf(
//                context.getString(R.string.title) to context.getString(R.string.unknown_title),
//                context.getString(R.string.artist) to context.getString(R.string.unknown_artist),
//                context.getString(R.string.album) to context.getString(R.string.unknown_album)
//            )
//        } finally {
//            retriever.release()
//        }
//    }
    fun getAudioMetadata(context: Context, uri: Uri): Map<String, String> {
        val retriever = MediaMetadataRetriever()
        return try {
            // Encode the URI if necessary to handle special characters
            val encodedUri = Uri.parse(Uri.encode(uri.toString()))

            // Check if URI scheme is "content"
            if (encodedUri.scheme == "content") {
                val fileDescriptor = context.contentResolver.openFileDescriptor(encodedUri, "r")?.fileDescriptor
                if (fileDescriptor != null) {
                    retriever.setDataSource(fileDescriptor)
                } else {
                    throw IllegalArgumentException("FileDescriptor is null for URI: $encodedUri")
                }
            } else {
                retriever.setDataSource(context, encodedUri)
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
        } catch (e: FileNotFoundException) {
            Log.e("AudioMetadata", "File not found for URI: $uri", e)
            mapOf(
                context.getString(R.string.title) to context.getString(R.string.unknown_title),
                context.getString(R.string.artist) to context.getString(R.string.unknown_artist),
                context.getString(R.string.album) to context.getString(R.string.unknown_album)
            )
        } catch (e: IllegalArgumentException) {
            Log.e("AudioMetadata", "Invalid argument for URI: $uri", e)
            mapOf(
                context.getString(R.string.title) to context.getString(R.string.unknown_title),
                context.getString(R.string.artist) to context.getString(R.string.unknown_artist),
                context.getString(R.string.album) to context.getString(R.string.unknown_album)
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

    override fun onViewRecycled(holder: AudioViewHolder) {
        super.onViewRecycled(holder)
        Log.w("refresh_behav", "View recycled for position: ${holder.adapterPosition}")
    }


}