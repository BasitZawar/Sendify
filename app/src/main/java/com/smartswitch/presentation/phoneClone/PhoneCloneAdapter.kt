package com.smartswitch.presentation.phoneClone

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.ItemListViewCloneBinding
import com.smartswitch.domain.model.PhoneCloneItem
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnItemCheckBoxClickCallback
import com.smartswitch.utils.enums.MediaTypeEnum
import com.smartswitch.utils.extensions.getMediaTypeString

class PhoneCloneAdapter(
    private val onItemCheckBoxClickCallback: OnItemCheckBoxClickCallback) :
    ListAdapter<PhoneCloneItem, PhoneCloneAdapter.PhoneCloneViewHolder>(PhoneCloneDiffCallback()) {

    var isAllChecked = false
    var isRootChecked = false
    var context: Context? = null

    inner class PhoneCloneViewHolder(val binding: ItemListViewCloneBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PhoneCloneItem, onItemCheckBoxClickCallback: OnItemCheckBoxClickCallback) {
            binding.apply {
                mainTextView.text = item.name
                mainTextView.text = context!!.getMediaTypeString(item.name)
                subTextView.text = "${item.count} ${context!!.getString(R.string.item)}"
                imageView.setImageResource(item.image)
                checkbox.isChecked = isAllChecked

                if (checkbox.isChecked) {
                    checkbox.setBackgroundResource(R.drawable.check_circle)
                } else {
                    checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                }
                checkbox.setOnCheckedChangeListener { view, isChecked ->
                    if (!checkbox.isPressed && !isRootChecked) {
                        return@setOnCheckedChangeListener
                    }

                    isRootChecked = false

                    if (isChecked) {
                        SelectedListManager.getSelectedMediaList()
                        SelectedListManager.getSelectedContactsList()
                        checkbox.setBackgroundResource(R.drawable.check_circle)
                    } else {
                        checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                    }

                    onItemCheckBoxClickCallback.onItemCheckBoxClicked(item, isChecked)
                }
                root.setOnClickListener {
                    isRootChecked = true
                    checkbox.isChecked = !checkbox.isChecked
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhoneCloneViewHolder {
        val binding = ItemListViewCloneBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        context = parent.context
        return PhoneCloneViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhoneCloneViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemCheckBoxClickCallback)
    }

    fun updateCheckState(isChecked: Boolean) {
        isAllChecked = isChecked
        notifyDataSetChanged()
    }
}

class PhoneCloneDiffCallback : DiffUtil.ItemCallback<PhoneCloneItem>() {
    override fun areItemsTheSame(oldItem: PhoneCloneItem, newItem: PhoneCloneItem): Boolean {
        return oldItem.name == newItem.name && oldItem.count == newItem.count
    }

    override fun areContentsTheSame(oldItem: PhoneCloneItem, newItem: PhoneCloneItem): Boolean {
        return oldItem == newItem
    }


}
