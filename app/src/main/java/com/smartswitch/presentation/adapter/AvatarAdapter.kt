package com.smartswitch.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R

class AvatarAdapter(
    private val avatars: List<Int>,
    private val onAvatarSelected: (Int) -> Unit,
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    var selectedAvatar: Int? = null

    inner class AvatarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarImage: ImageView = itemView.findViewById(R.id.ivAvatarItem)

        fun bind(avatarRes: Int) {
            avatarImage.setImageResource(avatarRes)

            // Highlight the selected avatar
            if (avatarRes == selectedAvatar) {
                itemView.setBackgroundResource(R.drawable.avatar_item_selector) // Circular highlight
            } else {
                itemView.setBackgroundResource(0) // No background
            }

            itemView.setOnClickListener {
                if (selectedAvatar != avatarRes) {
                    val previousSelected = selectedAvatar
                    selectedAvatar = avatarRes
                    notifyItemChanged(avatars.indexOf(previousSelected))
                    notifyItemChanged(adapterPosition)

                    // Trigger callback for the selected avatar
                    onAvatarSelected(avatarRes)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.avatar_item, parent, false)
        return AvatarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        holder.bind(avatars[position])
    }

    override fun getItemCount(): Int = avatars.size
}
