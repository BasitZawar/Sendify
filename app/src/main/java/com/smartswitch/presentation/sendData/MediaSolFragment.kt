package com.smartswitch.presentation.sendData

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smartswitch.R
import com.smartswitch.ads.banner_ads.setupBannerAd
import com.smartswitch.databinding.FragmentMediaSolBinding
import com.smartswitch.presentation.adapter.ViewPagerAdapter
import com.smartswitch.presentation.sendData.apps.AppsSolFragment
import com.smartswitch.presentation.sendData.audios.AudiosSolFragment

import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.MyDialogBox
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.SelectedListManager.getSelectedContactsListSize
import com.smartswitch.utils.SelectedListManager.getSelectedMediaListSize
import com.smartswitch.utils.SelectedListManager.getTotalSize
import com.smartswitch.utils.callback.OnMediaItemClickCallback
import com.smartswitch.utils.extensions.disable
import com.smartswitch.utils.extensions.enable

import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.setSafeOnClickListener
import com.smartswitch.utils.extensions.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MediaSolFragment : Fragment(), OnMediaItemClickCallback {
    private var _binding: FragmentMediaSolBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMediaSolBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isAlive { activityContext ->


            //setViewPager()
            CoroutineScope(Dispatchers.IO).launch {
                delay(300)
                withContext(Dispatchers.Main) {
                    setViewPager()
                }
            }

            (activityContext as FragmentActivity).handleBackPressWithAction {
                isAlive {
                    performBackAction()
                }
            }

            binding.headerLayout.setNavigationOnClickListener {
                performBackAction()
            }

            binding.sendButton.setSafeOnClickListener {
                if (SelectedListManager.getSelectedMediaList().isNotEmpty() ||
                    SelectedListManager.getSelectedContactsList().isNotEmpty()
                ) {
                    val selectedItem =
                        SelectedListManager.getSelectedMediaList().size + SelectedListManager.getSelectedContactsList().size
                    Log.d("TAG", "Send $selectedItem item")
                    findNavController().navigate(R.id.action_mediaSendifyFragment_to_searchingDeviceSendifyFragment)
                } else {
                    Toast.makeText(
                        context,
                        getString(R.string.please_select_at_least_one_item_to_proceed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            if ( PrefUtil(requireContext()).getBool("is_premium", false)) {
                binding.adRel.gone()
            } else {

                        var initialLayoutComplete = false
                        binding.adViewContainer.apply {
                            addView(AdView(activityContext))
                            viewTreeObserver.addOnGlobalLayoutListener {
                                if (!initialLayoutComplete) {
                                    initialLayoutComplete = true
                                    binding.adViewContainer.setupBannerAd(
                                        activityContext,
                                        getString(R.string.banner_all)
                                    )
                                }
                            }
                        }


            }
        }
    }


    @SuppressLint("InflateParams")
    private fun setViewPager() {
        isAlive { activityContext ->

            val tabsHeading = arrayOf(
                getString(R.string.apps),
                getString(R.string.photos),
                getString(R.string.videos),
                getString(R.string.audio),
                getString(R.string.documents),
                getString(R.string.contacts)
            )

            val adapter = ViewPagerAdapter(childFragmentManager, lifecycle)
            binding.apply {
                viewPager.offscreenPageLimit = 1
                viewPager.adapter = adapter
                tabLayout.tabIconTint = null

                // Setup TabLayout with custom tab views
                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    val customTabView =
                        LayoutInflater.from(context).inflate(R.layout.custom_tab, null)
                    val tabIcon = customTabView.findViewById<ShapeableImageView>(R.id.tabIcon)
                    val tabText = customTabView.findViewById<TextView>(R.  id.tabText)

                    // TODO : Set icons for tabs
                    when (position) {
                        0 -> tabIcon.setImageResource(R.drawable.ic_apps)
                        1 -> tabIcon.setImageResource(R.drawable.ic_photo_tab)
                        2 -> tabIcon.setImageResource(R.drawable.ic_video_tab)
                        3 -> tabIcon.setImageResource(R.drawable.ic_audio_tab)
                        4 -> tabIcon.setImageResource(R.drawable.ic_doc_tab)
                        5 -> tabIcon.setImageResource(R.drawable.ic_contact_tab)
                    }
                    tabText.text = tabsHeading[position]
                    tab.customView = customTabView
                }.attach()


                val defaultTabIndex = arguments?.getInt("tab_position") ?: 0
                tabLayout.getTabAt(defaultTabIndex)?.select()

                val defaultCustomTabView = tabLayout.getTabAt(defaultTabIndex)?.customView
                val defaultTabText = defaultCustomTabView?.findViewById<TextView>(R.id.tabText)
                defaultTabText?.setTextColor(
                    ContextCompat.getColor(activityContext, R.color.colorPrimary)
                )

                headerLayout.title = tabsHeading[defaultTabIndex]

                viewPager.setCurrentItem(
                    defaultTabIndex,
                    false
                ) // Ensures ViewPager updates as well

                tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

                    override fun onTabSelected(tab: TabLayout.Tab) {
                        val customTabView = tab.customView
                        val tabText = customTabView?.findViewById<TextView>(R.id.tabText)
                        tabText?.setTextColor(
                            ContextCompat.getColor(activityContext, R.color.colorPrimary)
                        )
                        headerLayout.title = tabsHeading[tab.position]
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab) {
                        val customTabView = tab.customView
                        val tabText = customTabView?.findViewById<TextView>(R.id.tabText)
                        tabText?.setTextColor(
                            ContextCompat.getColor(activityContext, R.color.sub_heading_text_color)
                        )
                    }

                    override fun onTabReselected(tab: TabLayout.Tab) {
                        // TODO : You can handle reselected tab behavior here if needed
                    }
                })
            }
        }
    }

    private fun performBackAction() {
        if (SelectedListManager.getSelectedMediaList().isNotEmpty() ||
            SelectedListManager.getSelectedContactsList().isNotEmpty()
        ) {
            selectedFilesDialog(R.layout.selectedfilesdialog) {
                findNavController().navigateUp()
            }
        } else {
            findNavController().navigateUp()
        }
    }

    private fun selectedFilesDialog(layoutResId: Int, onAllowClicked: () -> Unit) {
        context?.let { context ->
            if (!isAdded || activity == null) {
                Log.e(ContentValues.TAG, "Fragment not attached. Skipping dialog display.")
                return
            }

            val dialogView = LayoutInflater.from(context).inflate(layoutResId, null)
            val noBtn: TextView = dialogView.findViewById(R.id.noBtn)
            val yesBtn: TextView = dialogView.findViewById(R.id.yesBtn)

            val dialog = MyDialogBox.getInstance(requireActivity())
                ?.setContentViewWithDismissCallBack(dialogView, true, 0.85f) {}
                ?.showDialog()


            yesBtn.setOnClickListener {
                if (isAdded) {
                    SelectedListManager.clearSelected()
                    onAllowClicked.invoke()
                }
                dialog?.dismiss()
            }
            noBtn.setOnClickListener {
                dialog?.dismiss()
            }
        } ?: Log.e(ContentValues.TAG, "Context is null. Cannot display dialog.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        updateSendBtnUI()
    }

    override fun onMediaItemClicked() {
        updateSendBtnUI()
    }

    private fun updateSendBtnUI() {
        if (getSelectedMediaListSize() > 0 || getSelectedContactsListSize() > 0) {
            binding.sendButton.enable()
            binding.sendButton.visible()
            binding.selectedTextView.text =
                "${getSelectedMediaListSize() + getSelectedContactsListSize()} ${getString(R.string.files)} / ${getTotalSize()}"
        } else {
            binding.sendButton.disable()
            binding.sendButton.gone()
            binding.selectedTextView.text =
                "${getSelectedMediaListSize() + getSelectedContactsListSize()} ${getString(R.string.files)} / ${getTotalSize()}"
        }
//        binding.sendButton.text =
//            "Share (${getSelectedMediaListSize() + getSelectedContactsListSize()})}"

        binding.selectedTextView.setOnClickListener {
            val mediaList = SelectedListManager.getSelectedMediaList()
            val contactList = SelectedListManager.getSelectedContactsList()
//            Log.d("checking",mediaList.toString())
//            Log.d("checking","============================================================")

            for (i in mediaList) {
                Log.d("checking", i.toString())
            }
            Log.d("checking", "============================================================")

            // Log.d("checking",contactList.toString())

        }
    }
}