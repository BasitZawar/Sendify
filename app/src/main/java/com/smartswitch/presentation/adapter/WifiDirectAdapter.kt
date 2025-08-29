package com.smartswitch.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.databinding.ScanDeviceLayoutBinding
import com.smartswitch.interfaces.ClickInterface

class WifiDirectAdapter(
    private val context: Context,
    private val mArrayList: ArrayList<String>,
    private val clickInterface: ClickInterface
) : RecyclerView.Adapter<WifiDirectAdapter.MViewHolder>() {

    inner class MViewHolder(val binding: ScanDeviceLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MViewHolder {
        return MViewHolder(ScanDeviceLayoutBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MViewHolder, position: Int) {
        holder.binding.tvDeviceName.text = mArrayList[position]

        holder.binding.mainCardView.setOnClickListener {
            clickInterface.onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return mArrayList.size
    }


}