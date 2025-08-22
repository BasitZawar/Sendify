package com.smartswitch.presentation.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.smartswitch.presentation.sendData.MediaSolFragment
import com.smartswitch.presentation.sendData.apps.AppsSolFragment
import com.smartswitch.presentation.sendData.audios.AudiosSolFragment
import com.smartswitch.presentation.sendData.contacts.ContactsSolFragment
import com.smartswitch.presentation.sendData.documents.DocumentsSendifyFragment
import com.smartswitch.presentation.sendData.photos.PhotosSolFragment
import com.smartswitch.presentation.sendData.videos.VideosSolFragment

class ViewPagerAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle
) :
    FragmentStateAdapter(fm, lifecycle) {
    override fun getItemCount(): Int {
        return 6
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AppsSolFragment()
            1 -> PhotosSolFragment()
            2 -> VideosSolFragment()
            3 -> AudiosSolFragment()
            4 -> DocumentsSendifyFragment()
            5 -> ContactsSolFragment()
            else -> AppsSolFragment()
        }
    }

}