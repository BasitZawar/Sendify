package com.smartswitch.presentation.history.contacts

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.ItemListViewBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManagerForDeletion
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ContactsHistoryAdapter(
    private val onMediaItemClickCallbackForSelectAll: OnMediaItemClickCallbackForSelectAll
) : RecyclerView.Adapter<ContactsHistoryAdapter.ContactHistoryViewHolder>() {

    private var contactHistoryList: List<MediaInfoModel> = emptyList()
    var isRootChecked = false
    var context: Context? = null


    inner class ContactHistoryViewHolder(private val binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: MediaInfoModel?) {
            binding.apply {
                binding.mainTextView.text = contact?.name ?: context!!.getString(R.string.unknown_contact)
                subTextView.text = contact?.uri.toString()
                binding.imageView.setImageResource(R.drawable.ic_contact_adp)

                binding.checkbox.isChecked = SelectedListManagerForDeletion.isItemSelected(contact)
                binding.checkbox.setBackgroundResource(
                    if (binding.checkbox.isChecked) R.drawable.check_circle else R.drawable.uncheck_circle
                )

                binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (!binding.checkbox.isPressed && !isRootChecked) return@setOnCheckedChangeListener

                    isRootChecked = false

                    if (isChecked) {
                        SelectedListManagerForDeletion.addSelectedMedia(contact)
                        checkbox.setBackgroundResource(R.drawable.check_circle)
                    } else {
                        SelectedListManagerForDeletion.removeSelectedMedia(contact)
                        checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                    }

                    onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                }
                // Handle the root click to toggle checkbox
                binding.root.setOnClickListener {
                    isRootChecked=true
                    binding.checkbox.isChecked = !binding.checkbox.isChecked
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactHistoryViewHolder {
        val binding = ItemListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return ContactHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactHistoryViewHolder, position: Int) {
        holder.bind(contactHistoryList[position])
    }

    override fun getItemCount(): Int {
        return contactHistoryList.size
    }

    fun updateData(newContactHistoryList: List<MediaInfoModel>) {
        contactHistoryList = newContactHistoryList
        notifyDataSetChanged()
    }

    fun selectAllMedia(isChecked: Boolean, contactsList: List<MediaInfoModel>, lifecycleScope: CoroutineScope) {
        lifecycleScope.launch {
            contactsList.forEach { contact ->
                if (isChecked) {
                    SelectedListManagerForDeletion.addSelectedMedia(contact)
                } else {
                    SelectedListManagerForDeletion.removeSelectedMedia(contact)
                }
            }
            notifyDataSetChanged()
        }
        onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
    }

}
