package com.smartswitch.presentation.history.documents

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.ItemListViewBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManagerForDeletion
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.enums.DateFormatType
import com.smartswitch.utils.extensions.formatFileSize
import com.smartswitch.utils.extensions.formatTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class DocumentsHistoryAdapter(
    private val onMediaItemClickCallbackForSelectAll: OnMediaItemClickCallbackForSelectAll
) : RecyclerView.Adapter<DocumentsHistoryAdapter.DocumentHistoryViewHolder>() {

    private var documentHistoryList: List<MediaInfoModel> = emptyList()
    var isRootChecked = false
    var context: Context? = null

    inner class DocumentHistoryViewHolder(val binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(document: MediaInfoModel?) {
            binding.apply {
                Log.d("DocumentsHistoryAdapter___", "bind: ${document?.date}")

                // Format the document's date
                val formattedDate = document?.date?.formatTo(DateFormatType.DAY_MONTH_YEAR)

                // Format the document's size and modification date
               // subTextView.text = "${document?.size?.formatFileSize()}, ${context!!.getString(R.string.modified)} ${formattedDate}"
                subTextView.text = "${document?.size?.formatFileSize()} ≈ storage${document?.uri?.substringAfter("0")}"

                // Decode the document name in case it is URL-encoded
                val decodedDocumentName = document?.name?.let {
                    try {
                        URLDecoder.decode(it, StandardCharsets.UTF_8.name())
                    } catch (e: Exception) {
                        it // If decoding fails, fall back to the original string
                    }
                } ?: context!!.getString(R.string.unknown_document)

                // Handle document name: set text direction dynamically for Urdu or English
                if (decodedDocumentName.contains(Regex("[\\u0600-\\u06FF]"))) {  // Check if the name contains Urdu characters
                    mainTextView.textDirection = View.TEXT_DIRECTION_RTL
                    mainTextView.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                } else {
                    mainTextView.textDirection = View.TEXT_DIRECTION_LTR
                    mainTextView.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
                }
                mainTextView.text = decodedDocumentName

                // Set the document icon
                imageView.setImageResource(R.drawable.ic_doc)

                // Handle the checkbox state (checked/unchecked)
                checkbox.isChecked = SelectedListManagerForDeletion.isItemSelected(document)
                checkbox.setBackgroundResource(
                    if (checkbox.isChecked) R.drawable.check_circle else R.drawable.uncheck_circle
                )

                // Handle checkbox state change
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (!checkbox.isPressed && !isRootChecked) return@setOnCheckedChangeListener

                    isRootChecked = false

                    if (isChecked) {
                        SelectedListManagerForDeletion.addSelectedMedia(document)
                        checkbox.setBackgroundResource(R.drawable.check_circle)
                    } else {
                        SelectedListManagerForDeletion.removeSelectedMedia(document)
                        checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                    }
                    onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                }

                // Handle root click for toggling checkbox state
                root.setOnClickListener {
                    isRootChecked = true
                    checkbox.isChecked = !checkbox.isChecked
                }
            }
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentHistoryViewHolder {
        val binding = ItemListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return DocumentHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentHistoryViewHolder, position: Int) {
        Log.d("DocumentsHistoryAdapter", "Binding item at position $position: ${documentHistoryList[position]}")
        holder.bind(documentHistoryList[position])
    }

    override fun getItemCount(): Int {
        return documentHistoryList.size
    }

    fun updateData(newDocumentHistoryList: List<MediaInfoModel>) {
        Log.d("DocumentsHistoryAdapter", "New data for adapter: $newDocumentHistoryList")
        documentHistoryList = newDocumentHistoryList
        notifyDataSetChanged() // You can optimize this with DiffUtil if needed
    }

    private fun handleCheckState(item: MediaInfoModel?): Boolean {
        return SelectedListManagerForDeletion.isItemSelected(item)
    }

    fun selectAllMedia(isChecked: Boolean, documentList: List<MediaInfoModel>, lifecycleScope: CoroutineScope) {
        lifecycleScope.launch {
            documentList.forEach { document ->
                if (isChecked) {
                    SelectedListManagerForDeletion.addSelectedMedia(document)
                } else {
                    SelectedListManagerForDeletion.removeSelectedMedia(document)
                }
            }
            notifyDataSetChanged()
        }
        onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
    }

}
