package com.smartswitch.presentation.phoneClone

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdView
import com.smartswitch.R
import com.smartswitch.ads.banner_ads.setupBannerAd
import com.smartswitch.databinding.FragmentSelectDataToCloneSolBinding
import com.smartswitch.domain.model.PhoneCloneItem

import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.utils.MyDialogBox
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnItemCheckBoxClickCallback
import com.smartswitch.utils.enums.MediaTypeEnum
import com.smartswitch.utils.extensions.disable
import com.smartswitch.utils.extensions.enable
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.setSafeOnClickListener
import com.smartswitch.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class SelectDataToCloneSolFragment : Fragment(), OnItemCheckBoxClickCallback {
    private var _binding: FragmentSelectDataToCloneSolBinding? = null
    private val binding get() = _binding!!

    val TAG = "PhoneCloneFragmentTAG"

    var adapter: PhoneCloneAdapter? = null

    val viewModel: PhoneCloneViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSelectDataToCloneSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isAlive { activityContext ->
            //SelectedListManager.clearSelected()


            initRecyclerView()
            observeFetchingAllMedia()
            fetchAllMedia()
            intiListener()

            (activityContext as FragmentActivity).handleBackPressWithAction {
                performBackAction()
            }

            binding.headerLayout.setNavigationOnClickListener {
                performBackAction()
            }
            binding.sendButton.setSafeOnClickListener {
                if (SelectedListManager.getSelectedMediaList().isNotEmpty() ||
                    SelectedListManager.getSelectedContactsList().isNotEmpty()
                ) {
                    findNavController().navigate(R.id.action_selectDataToCloneSendifyFragment_to_searchingDeviceSendifyFragment)
                } else {
                    Toast.makeText(
                        context,
                        "Please select at least one item to proceed",
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

    private fun initRecyclerView() {
        binding.apply {
            adapter = PhoneCloneAdapter(this@SelectDataToCloneSolFragment)
            rvMediaItems.adapter = adapter
        }
    }

    private fun observeFetchingAllMedia() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            viewModel.isAllMediaFetched.collect { isFetched ->
                if (isFetched) {
                    // Update the adapter with the latest media data
                    adapter?.submitList(viewModel.fetchMediaNameAndCount())

                    // Update the UI after fetching data
                    updateUiOnDataFetch()
                   // updateSendBtnUI()
                } else {
                    Log.i(TAG, "observeFetchingAllMedia: Data is still being fetched.")
                }
            }
        }
    }

    private fun fetchAllMedia() {
        activity?.let { act ->
            viewModel.fetchAllMedia(act)
        }
    }

    private fun updateUiOnDataFetch() {
        binding.apply {
            progressBar.gone()
            selectAllContainer.visible()
            rvMediaItems.visible()
        }
    }

    private fun intiListener() {
        handleSelectAllCheck()
    }

    private fun handleSelectAllCheck() {
        binding.apply {
            Log.d("awais","handleSelectAllCheck")
            checkboxSelectAll.setOnCheckedChangeListener { view, isChecked ->
                if (!checkboxSelectAll.isPressed) {
                    checkboxSelectAll.isChecked = false
                    return@setOnCheckedChangeListener
                }
                Log.d("awais","checkboxSelectAll.isPressed : ${checkboxSelectAll.isPressed}")
                Log.d("awais","isChecked : $isChecked")
                if (isChecked) {
                    SelectedListManager.addAllSelectedMedia(viewModel.allMedia)
                    SelectedListManager.addAllSelectedContacts(viewModel.allContactsList)
                    checkboxSelectAll.setBackgroundResource(R.drawable.check_circle)
                } else {
                    SelectedListManager.removeAllSelectedMedia(viewModel.allMedia)
                    SelectedListManager.removeAllSelectedContacts(viewModel.allContactsList)
                    checkboxSelectAll.setBackgroundResource(R.drawable.uncheck_circle)
                }
                checkSendButtonState()
                adapter?.updateCheckState(isChecked)
                val list = SelectedListManager.getSelectedMediaList()
                val list2 = SelectedListManager.getSelectedContactsList()
                Log.i(TAG, "handleSelectAllCheck: ${list.size} contacts ${list2.size}")
                updateSendBtnUI()
            }
        }
    }

    private fun handleSelectAllCheckState() {
        if (!isAdded || _binding == null) {
            Log.w(TAG, "Fragment not attached or binding is null")
            return
        }

        Log.d("awais","handleSelectAllCheckState")

        val allMediaSelected = SelectedListManager.getSelectedMediaListSize() == viewModel.allMedia.size
        val allContactsSelected = SelectedListManager.getSelectedContactsList().size == viewModel.allContactsList.size

        binding.checkboxSelectAll.isChecked = allMediaSelected && allContactsSelected

        val isChecked = binding.checkboxSelectAll.isChecked
        binding.checkboxSelectAll.setBackgroundResource(
            if (isChecked) R.drawable.check_circle else R.drawable.uncheck_circle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel.clearState()
        adapter = null
    }

    override fun onItemCheckBoxClicked(phoneCloneItem: PhoneCloneItem, isChecked: Boolean) {
        itemStateOnCheckClicked(phoneCloneItem = phoneCloneItem, isChecked = isChecked){
            Log.i(TAG, "onItemCheckBoxClicked: ${SelectedListManager.getSelectedContactsList().size}")
            Log.i(TAG, "onItemCheckBoxClicked: ${SelectedListManager.getSelectedMediaList().size}")
            handleSelectAllCheckState()
            checkSendButtonState()
            updateSendBtnUI()
        }
    }

    override fun onAllowPermissionClicked() {
        TODO("Not yet implemented")
    }

    private fun checkSendButtonState() {
        if (SelectedListManager.getSelectedMediaList()
                .isNotEmpty() || SelectedListManager.getSelectedContactsList().isNotEmpty()
        ) {
            //binding.sendButton.visible()
            binding.sendButton.enable()

        } else {
            //binding.sendButton.gone()
            binding.sendButton.disable()
        }
        //binding.sendButton.text = "${getString(R.string.share)} (${SelectedListManager.getSelectedMediaListSize() + SelectedListManager.getSelectedContactsListSize()})"
    }

    private fun itemStateOnCheckClicked(
        phoneCloneItem: PhoneCloneItem,
        isChecked: Boolean,
        onComplete: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            when (phoneCloneItem.mediaType) {
                MediaTypeEnum.PHOTOS -> {
                    if (isChecked) {
                        SelectedListManager.addAllSelectedMedia(viewModel.allPhotos)
                    } else {
                        SelectedListManager.removeAllSelectedMedia(viewModel.allPhotos)
                    }
                }
                MediaTypeEnum.VIDEOS -> {
                    if (isChecked) {
                        SelectedListManager.addAllSelectedMedia(viewModel.allVideos)
                    } else {
                        SelectedListManager.removeAllSelectedMedia(viewModel.allVideos)
                    }
                }
                MediaTypeEnum.AUDIOS -> {
                    if (isChecked) {
                        SelectedListManager.addAllSelectedMedia(viewModel.allAudios)
                    } else {
                        SelectedListManager.removeAllSelectedMedia(viewModel.allAudios)
                    }
                }
                MediaTypeEnum.CONTACTS -> {
                    activity?.let { act ->
                        if (PermissionManager.hasContactPermission(act)) {
                            if (isChecked) {
                                SelectedListManager.addAllSelectedContacts(viewModel.allContactsList)
                            } else {
                                SelectedListManager.removeAllSelectedContacts(viewModel.allContactsList)
                            }
                        }
                    }
                }
                MediaTypeEnum.DOCUMENTS -> {
                    if (isChecked) {
                        SelectedListManager.addAllSelectedMedia(viewModel.allDocuments)
                    } else {
                        SelectedListManager.removeAllSelectedMedia(viewModel.allDocuments)
                    }
                }
                MediaTypeEnum.APPS -> {
                    if (isChecked) {
                        SelectedListManager.addAllSelectedMedia(viewModel.allApps)
                    } else {
                        SelectedListManager.removeAllSelectedMedia(viewModel.allApps)
                    }
                }
                MediaTypeEnum.OTHER -> {
                }
            }
            withContext(Dispatchers.Main) {
                onComplete()
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


    private fun updateSendBtnUI() {
        if (SelectedListManager.getSelectedMediaListSize() > 0 || SelectedListManager.getSelectedContactsListSize() > 0) {
            binding.sendButton.enable()
            //binding.sendButton.visible()
            binding.tvDataSize.text = "${getString(R.string.data_size)} : ${SelectedListManager.getTotalSize()}"
            binding.tvSelectedFiles.text = "${getString(R.string.selected_files)} : ${SelectedListManager.getSelectedMediaListSize() + SelectedListManager.getSelectedContactsListSize()} ${getString(R.string.files)}"
        } else {
            binding.sendButton.disable()
           // binding.sendButton.gone()
            binding.tvDataSize.text = "${getString(R.string.data_size)} : ${SelectedListManager.getTotalSize()}"
            binding.tvSelectedFiles.text = "${getString(R.string.selected_files)} : ${SelectedListManager.getSelectedMediaListSize() + SelectedListManager.getSelectedContactsListSize()} ${getString(R.string.files)}"
        }
    }
}