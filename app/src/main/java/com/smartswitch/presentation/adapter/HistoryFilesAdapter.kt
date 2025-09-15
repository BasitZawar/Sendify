package com.smartswitch.presentation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartswitch.R
import com.smartswitch.databinding.ItemListViewBinding
import com.smartswitch.interfaces.OnFileClick
import com.smartswitch.presentation.history.HistoryFragment
import com.smartswitch.utils.Constant
import com.smartswitch.utils.FileEnum

class HistoryFilesAdapter(
    private var allFiles: ArrayList<HistoryFragment.HistoryModel>,
    var context: Context,
    var onFileClick: OnFileClick
) : RecyclerView.Adapter<HistoryFilesAdapter.FileHolder>() {
    inner class FileHolder(var binding: ItemListViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {

                onFileClick.onHistoryClick(adapterPosition, filestype)
//
                onFileClick.onFileClick(adapterPosition)
            }
//            binding.ivCheckedUnchecked.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder {
        return FileHolder(
            ItemListViewBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: FileHolder, position: Int) {
        val mFileModel = allFiles[position]

        val type: FileEnum = Constant.getFileType(mFileModel.path)
        holder.binding.mainTextView.text = mFileModel.title
        holder.binding.subTextView.text = mFileModel.size

//        holder.binding.ivBackground.visibility = View.INVISIBLE
        if (type == FileEnum.Image || type == FileEnum.Video) {
            Glide.with(context).load(mFileModel.path).into(holder.binding.imageView)
        } else if (type == FileEnum.Audio) {
            Glide.with(context).load(R.drawable.ic_music).into(holder.binding.imageView)
        } else if (type == FileEnum.Document) {
            Glide.with(context).load(R.drawable.ic_docs)
                .into(holder.binding.imageView)
        } else if (mFileModel.title.endsWith(".vcf")) {
            Glide.with(context).load(R.drawable.ic_contact_adp)
                .into(holder.binding.imageView)
        } else {
            Glide.with(context).load(R.drawable.apps).into(holder.binding.imageView)
        }
    }

    override fun getItemCount(): Int {
        return allFiles.size
    }

    companion object {
        var filestype = 0
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newFiles: ArrayList<HistoryFragment.HistoryModel>, s: Int) {
        Log.e("TESTTAG", "TYPE $s")
        allFiles = newFiles
        filestype = s
        notifyDataSetChanged()
    }
}