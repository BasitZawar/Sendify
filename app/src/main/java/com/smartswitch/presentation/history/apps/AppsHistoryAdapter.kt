package com.smartswitch.presentation.history.apps

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
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

class AppsHistoryAdapter(
    private val onMediaItemClickCallbackForSelectAll: OnMediaItemClickCallbackForSelectAll
) : RecyclerView.Adapter<AppsHistoryAdapter.AppsHistoryViewHolder>() {

    private var appsHistoryList: List<MediaInfoModel> = mutableListOf()
    var isRootChecked = false
    var context: Context? = null


    inner class AppsHistoryViewHolder(val binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(apps: MediaInfoModel?) {
            binding.apply {
                Log.d("AppsHistoryAdapter___", "bind: ${apps?.date}")
                val formattedDate = apps?.date?.formatTo(DateFormatType.DAY_MONTH_YEAR)
                binding.subTextView.text =
                    "${apps?.size?.formatFileSize()}, ${context!!.getString(R.string.modified)} ${formattedDate}"
                binding.mainTextView.text =
                    apps?.name ?: context!!.getString(R.string.de_select_all)
                binding.imageView.setImageResource(R.drawable.ic_doc)
                binding.checkbox.isChecked = SelectedListManagerForDeletion.isItemSelected(apps)
                binding.checkbox.setBackgroundResource(
                    if (binding.checkbox.isChecked) R.drawable.check_circle else R.drawable.uncheck_circle
                )

                binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (!binding.checkbox.isPressed && !isRootChecked) return@setOnCheckedChangeListener

                    isRootChecked = false

                    if (isChecked) {
                        SelectedListManagerForDeletion.addSelectedMedia(apps)
                        checkbox.setBackgroundResource(R.drawable.check_circle)
                    } else {
                        SelectedListManagerForDeletion.removeSelectedMedia(apps)
                        checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                    }
                    onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                }

                root.setOnClickListener {
                    isRootChecked = true
                    checkbox.isChecked = !checkbox.isChecked
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsHistoryViewHolder {
        val binding =
            ItemListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return AppsHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppsHistoryViewHolder, position: Int) {
        Log.d(
            "DocumentsHistoryAdapter",
            "Binding item at position $position: ${appsHistoryList[position]}"
        )
        holder.bind(appsHistoryList[position])
    }


    override fun getItemCount(): Int {
        return appsHistoryList.size
    }

    fun updateData(newAppsHistoryList: List<MediaInfoModel>) {
        Log.d("AppsHistoryAdapter___", "updateData: $newAppsHistoryList")
        appsHistoryList = newAppsHistoryList.toMutableList()
        notifyDataSetChanged()
    }

    private fun handleCheckState(item: MediaInfoModel?): Boolean {
        return SelectedListManagerForDeletion.isItemSelected(item)
    }

    fun selectAllMedia(
        isChecked: Boolean,
        appsList: List<MediaInfoModel>,
        lifecycleScope: CoroutineScope
    ) {
        lifecycleScope.launch {
            appsList.forEach { apps ->
                if (isChecked) {
                    SelectedListManagerForDeletion.addSelectedMedia(apps)
                } else {
                    SelectedListManagerForDeletion.removeSelectedMedia(apps)
                }
            }
            notifyDataSetChanged()
        }
        onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
    }

}