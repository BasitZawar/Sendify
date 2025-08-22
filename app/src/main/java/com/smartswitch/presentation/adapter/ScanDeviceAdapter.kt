package com.smartswitch.presentation.adapter

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.ScanDeviceLayoutBinding
import com.smartswitch.utils.extensions.setSafeOnClickListener

class ScanDeviceAdapter(
    val onItemClicked: (WifiP2pDevice) -> Unit
) : ListAdapter<WifiP2pDevice, ScanDeviceAdapter.ScanDeviceViewHolder>(DeviceDiffCallback()) {
    inner class ScanDeviceViewHolder(val binding: ScanDeviceLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WifiP2pDevice) {
            binding.apply {
                // Set the device name once
                tvDeviceName.text = item.deviceName

                // Set click listener on item
                itemView.setOnClickListener {
                    // Only update if the state is different, to avoid unnecessary redraws
                    if (mainCardView.strokeColor != ContextCompat.getColor(itemView.context, R.color.colorPrimary)) {
                        // Update the UI elements if necessary
                        mainCardView.strokeColor = ContextCompat.getColor(
                            itemView.context,
                            R.color.colorPrimary
                        )
                        tvDeviceName.setTextColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.text_color
                            )
                        )
                        phoneIcon.imageTintList = ContextCompat.getColorStateList(
                            itemView.context,
                            R.color.colorPrimary
                        )
                    }
                    // Call the item click handler
                    onItemClicked(item)
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanDeviceViewHolder {
        val binding =
            ScanDeviceLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScanDeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScanDeviceViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<WifiP2pDevice>() {

        override fun areItemsTheSame(oldItem: WifiP2pDevice, newItem: WifiP2pDevice): Boolean {
            return oldItem.deviceAddress == newItem.deviceAddress
        }

        override fun areContentsTheSame(oldItem: WifiP2pDevice, newItem: WifiP2pDevice): Boolean {
            return oldItem == newItem
        }
    }
}