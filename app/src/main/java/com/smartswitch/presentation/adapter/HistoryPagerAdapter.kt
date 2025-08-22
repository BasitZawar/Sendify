package com.smartswitch.presentation.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.smartswitch.presentation.history.apps.AppsHistorySolFragment
import com.smartswitch.presentation.history.audios.AudiosHistorySolFragment
import com.smartswitch.presentation.history.contacts.ContactsHistorySolFragment
import com.smartswitch.presentation.history.documents.DocumentsHistorySolFragment
import com.smartswitch.presentation.history.photos.PhotosHistorySolFragment
import com.smartswitch.presentation.history.videos.VideosHistorySolFragment

class HistoryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private var isSentMode = false


    fun setSendReceiveMode(isSent: Boolean) {
        isSentMode = isSent
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = 6

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> PhotosHistorySolFragment()
            1 -> VideosHistorySolFragment()
            2 -> AudiosHistorySolFragment()
            3 -> DocumentsHistorySolFragment()
            4 -> AppsHistorySolFragment()
            5 -> ContactsHistorySolFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }

        fragment.arguments = Bundle().apply {
            putBoolean("isSentMode", isSentMode)
        }

        return fragment
    }
}
