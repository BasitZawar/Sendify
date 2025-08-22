package com.smartswitch.presentation.language

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.SelectLanguageItemBinding

class LanguageAdapter(
    private val languageList: ArrayList<LanguageModel>,
    private val callBack: (String, Int) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LangViewHolder>() {

    private var lastCheckedPosition = 0

    fun setPos(pos: Int) {
        lastCheckedPosition = pos
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LangViewHolder {
        val binding = SelectLanguageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LangViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LangViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val lang = languageList[position]
        holder.binding.img.setImageResource(
            if (lastCheckedPosition == position) R.drawable.check_circle else R.drawable.uncheck_circle
        )
        holder.binding.languageImg.setImageResource(lang.img)
        holder.binding.languageText.text = lang.txt
        holder.binding.root.setOnClickListener {
            callBack(lang.txt, position)
            val previousPosition = lastCheckedPosition
            lastCheckedPosition = position
            notifyItemChanged(previousPosition)
            notifyItemChanged(lastCheckedPosition)
        }
    }

    override fun getItemCount() = languageList.size

    class LangViewHolder(val binding: SelectLanguageItemBinding) : RecyclerView.ViewHolder(binding.root)
}
