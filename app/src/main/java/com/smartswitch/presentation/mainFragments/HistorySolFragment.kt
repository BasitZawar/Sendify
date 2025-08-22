package com.smartswitch.presentation.mainFragments

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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdView
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smartswitch.R
import com.smartswitch.ads.banner_ads.setupBannerAd
import com.smartswitch.databinding.FragmentHistorySolBinding
import com.smartswitch.domain.model.HistoryViewModel
import com.smartswitch.presentation.adapter.HistoryPagerAdapter

import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.MyDialogBox
import com.smartswitch.utils.SelectedListManagerForDeletion
import com.smartswitch.utils.enums.HistoryCategory

import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.setSafeOnClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistorySolFragment : Fragment() {
    private var _binding: FragmentHistorySolBinding? = null
    private val binding get() = _binding!!

    private val historyViewModel: HistoryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistorySolBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isAlive { activityContext ->
            setViewPager()
            handleBackButton()


            binding.deleteIcon.setSafeOnClickListener {
                val selectedMediaItems = SelectedListManagerForDeletion.getSelectedMediaList()
                val selectedContactItems = SelectedListManagerForDeletion.getSelectedContactList()
                if (selectedMediaItems.isNotEmpty() || selectedContactItems.isNotEmpty()) {
                    deleteDialog(R.layout.deletedialog) {
                        Toast.makeText(context, getString(R.string.item_deleted_successful), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, getString(R.string.no_item_selected), Toast.LENGTH_SHORT).show()
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


    private fun performBackAction() {
        if (SelectedListManagerForDeletion.getSelectedContactList().isNotEmpty() ||
            SelectedListManagerForDeletion.getSelectedMediaList().isNotEmpty()
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
                    SelectedListManagerForDeletion.clearSelected()
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

    @SuppressLint("MissingInflatedId")
    private fun setViewPager() {
        isAlive { activityContext ->

            val tabsHeading = arrayOf(
                getString(R.string.photos),
                getString(R.string.videos),
                getString(R.string.audio),
                getString(R.string.documents),
                getString(R.string.apps),
                getString(R.string.contacts)
            )


            val adapter = HistoryPagerAdapter(this)
            binding.apply {
                viewPager.offscreenPageLimit = 6
                viewPager.adapter = adapter
                tabLayout.tabIconTint = null

                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    val customTabView =
                        LayoutInflater.from(context).inflate(R.layout.custom_tab, null)
                    val tabIcon = customTabView.findViewById<ShapeableImageView>(R.id.tabIcon)
                    val tabText = customTabView.findViewById<TextView>(R.id.tabText)

                    when (position) {
                        0 -> tabIcon.setImageResource(R.drawable.ic_photo_tab)
                        1 -> tabIcon.setImageResource(R.drawable.ic_video_tab)
                        2 -> tabIcon.setImageResource(R.drawable.ic_audio_tab)
                        3 -> tabIcon.setImageResource(R.drawable.ic_doc_tab)
                        4 -> tabIcon.setImageResource(R.drawable.ic_apps)
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
                binding.headerLayout.setTitle(tabsHeading[defaultTabIndex])

                // Primary TabLayout listener for categories
                tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab) {
                        val customTabView = tab.customView
                        val tabText = customTabView?.findViewById<TextView>(R.id.tabText)
                        tabText?.setTextColor(
                            ContextCompat.getColor(activityContext, R.color.colorPrimary)
                        )
                        binding.headerLayout.setTitle(tabsHeading[tab.position])

                    }

                    override fun onTabUnselected(tab: TabLayout.Tab) {
                        val customTabView = tab.customView
                        val tabText = customTabView?.findViewById<TextView>(R.id.tabText)
                        tabText?.setTextColor(
                            ContextCompat.getColor(activityContext, R.color.text_color)
                        )
                    }

                    override fun onTabReselected(tab: TabLayout.Tab) {}
                })

                binding.tabLayoutSendReceive.addTab(
                    binding.tabLayoutSendReceive.newTab().setText(getString(R.string.sent))
                )
                binding.tabLayoutSendReceive.addTab(
                    binding.tabLayoutSendReceive.newTab().setText(getString(R.string.received))
                )

                val defaultSendReceiveTabIndex = 0
                binding.tabLayoutSendReceive.getTabAt(defaultSendReceiveTabIndex)?.select()

                val defaultSendTab =
                    binding.tabLayoutSendReceive.getTabAt(defaultSendReceiveTabIndex)
                defaultSendTab?.view?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.tab_shape)
                val defaultSendTabText =
                    defaultSendTab?.customView?.findViewById<TextView>(R.id.tabText)
                defaultSendTabText?.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPrimary
                    )
                )

                binding.tabLayoutSendReceive.addOnTabSelectedListener(object :
                    TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab) {
                        tab.view.background =
                            ContextCompat.getDrawable(requireContext(), R.drawable.tab_shape)
                        updateFragments(tab.position == 0)
                        historyViewModel.setSelectedFragment(if (tab.position == 0) HistoryCategory.SEND else HistoryCategory.RECEIVED)
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab) {
                        tab.view.background = null
                    }

                    override fun onTabReselected(tab: TabLayout.Tab) {}
                })
            }
        }
    }

    private fun updateFragments(isSentMode: Boolean) {
        (binding.viewPager.adapter as? HistoryPagerAdapter)?.setSendReceiveMode(isSentMode)
    }

    private fun handleBackButton() {
        val activityContext = requireActivity()

        binding.headerLayout.setNavigationOnClickListener {
            performBackAction()
        }

        activityContext.handleBackPressWithAction {
            performBackAction()
        }
    }

//    private fun navigateBackToOrigin(bottomNavigationView: BottomNavigationView) {
//        val origin =
//            arguments?.getString("origin", "home") // Default to "home" if no origin provided
//
//        when (origin) {
//            "home" -> {
//                findNavController().popBackStack(R.id.homeSendifyFragment, false)
//                bottomNavigationView.selectedItemId = R.id.homeItem
//            }
//
//            "settings" -> {
//                findNavController().popBackStack(R.id.settingsSendifyFragment, false)
//                bottomNavigationView.selectedItemId = R.id.settingItem
//            }
//
//            else -> {
//                findNavController().popBackStack() // Default back behavior
//            }
//        }
//    }

    private fun deleteDialog(layoutResId: Int, onAllowClicked: () -> Unit) {
        context?.let { act ->
            val dialogView = LayoutInflater.from(act).inflate(layoutResId, null)
            val deleteBtn: MaterialButton = dialogView.findViewById(R.id.delBtn)
            val cancelBtn: MaterialButton = dialogView.findViewById(R.id.cancelBtn)


            val dialog = MyDialogBox.getInstance(requireActivity())
                ?.setContentViewWithDismissCallBack(dialogView, true, 0.85f) {}
                ?.showDialog()

            deleteBtn.setOnClickListener {
                val selectedMediaItems = SelectedListManagerForDeletion.getSelectedMediaList()
                val selectedContactItems = SelectedListManagerForDeletion.getSelectedContactList()
                Log.d(
                    "VideoHistoryUseCase___",
                    "History list selectedMediaItems: ${selectedMediaItems.size}"
                )
                Log.d(
                    "VideoHistoryUseCase___",
                    "History list selectedContactItems: ${selectedContactItems.size}"
                )
                Log.d(
                    "VideoHistoryUseCase___",
                    "History list selectedMediaItems: ${selectedMediaItems}"
                )
                Log.d(
                    "VideoHistoryUseCase___",
                    "History list selectedMediaItems: ${selectedContactItems}"
                )
                if (selectedMediaItems.isNotEmpty() || selectedContactItems.isNotEmpty()) {
                    historyViewModel.deleteMediaHistory(selectedMediaItems)
                    SelectedListManagerForDeletion.clearSelectedMedia()

                    Toast.makeText(context, getString(R.string.selected_items_deleted), Toast.LENGTH_SHORT).show()
                    onAllowClicked.invoke()
                    dialog?.dismiss()
                } else {
                    Toast.makeText(
                        context,
                        getString(R.string.please_select_at_least_one_item_to_proceed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            cancelBtn.setOnClickListener {
                dialog?.dismiss()
            }
        }
    }

//    private fun clearSelections() {
//        SelectedListManagerForDeletion.clearSelectedMedia()
//        SelectedListManagerForDeletion.clearSelectedContacts()
//    }

//    private fun setupDeleteButton() {
//        binding.deleteIcon.setSafeOnClickListener {
//            val selectedMedia = SelectedListManagerForDeletion.getSelectedMediaList()
//            Log.d("HistoryFragment", "Selected media for deletion: $selectedMedia")
//            Log.d(
//                "AudioHistoryUseCase___",
//                "Fetched video history selectedMedia size: ${selectedMedia.size}"
//            )
//            Log.d(
//                "AudioHistoryUseCase___",
//                "Fetched video history selectedMedia size: ${selectedMedia}"
//            )
//            historyViewModel.deleteMediaHistory(selectedMedia)
//            clearSelections()
//        }
//    }

}