package com.smartswitch.presentation.sendData.contacts

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.ItemListViewBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.formatFileSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContactsAdapter(
    private val contacts: List<MediaInfoModel>? = null,
    private val onMediaItemClickCallbackForSelectAll: OnMediaItemClickCallbackForSelectAll
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    var context: Context? = null
    var isRootChecked = false


    inner class ContactViewHolder(val binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: MediaInfoModel?) {
            binding.apply {
                mainTextView.text = contact?.name
                subTextView.text = contact?.uri.toString()
                imageView.setImageResource(R.drawable.ic_contact_adp)

                checkbox.isChecked = handleCheckState(contact)
                if (checkbox.isChecked) {
                    checkbox.setBackgroundResource(R.drawable.check_circle)

                } else {
                    checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                }
                checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (!binding.checkbox.isPressed && !isRootChecked) {
                            return@setOnCheckedChangeListener

                    }

                    isRootChecked = false

                    if (isChecked) {
                        SelectedListManager.addSelectedContact(contact)
                        checkbox.setBackgroundResource(R.drawable.check_circle)
                    } else {
                        SelectedListManager.removeSelectedContact(contact)
                        checkbox.setBackgroundResource(R.drawable.uncheck_circle)
                    }
                    onMediaItemClickCallbackForSelectAll.onMediaItemClickedForSelectAll()
                }

                root.setOnClickListener {
                    isRootChecked = true
                    checkbox.isChecked = !checkbox.isChecked
                }

                binding.root.setOnLongClickListener {
                    AlertDialog.Builder(context)
                        .setTitle(context!!.getString(R.string.properties))
                        .setMessage("${context!!.getString(R.string.name)} : ${contact?.name} " +
                                "\n${context!!.getString(R.string.size)} : ${contact?.size?.formatFileSize()} " +
                                "\n${context!!.getString(R.string.type)} : ${contact?.mediaType} " +
                                "\n${context!!.getString(R.string.contact_no)} : ${contact?.contactNumber} ")
                        .setPositiveButton(context!!.getString(R.string.ok),null)
                        .create()
                        .show()
                    true
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding =
            ItemListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return ContactViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return contacts?.size ?: 0
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val mediaItem = contacts?.get(position)
        context?.let { holder.bind(mediaItem) }

    }

    fun handleCheckState(item: MediaInfoModel?): Boolean {
        return SelectedListManager.isContactItemSelected(item)
    }


}