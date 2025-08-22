package com.smartswitch.presentation.sendData.contacts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.smartswitch.R
import com.smartswitch.databinding.FragmentContactsSolBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.Dialogs
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnMediaItemClickCallback
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.selectAllContacts
import com.smartswitch.utils.extensions.setSafeOnClickListener
import com.smartswitch.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ContactsSolFragment : Fragment(), OnMediaItemClickCallbackForSelectAll {
    private var _binding: FragmentContactsSolBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsSolFragmentViewModel by viewModels()
    private lateinit var onMediaItemClickCallback: OnMediaItemClickCallback

    private var isDataLoaded = false
    private var adapter: ContactsAdapter? = null


    private val contactPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            isAlive { act ->

                if (granted) {
                    if (!isDataLoaded) {

                        if (PermissionManager.hasContactPermission(act)) {
                            fetchContacts()
                        } else {

                            binding.apply {
                                progressBar.gone()
                                rvContacts.gone()
                                tvNoData.gone()
                                headerLayout.gone()
                                contactPermissionCardView.visible()
                            }
                        }
                    }
                    binding.checkboxSelectAll.isChecked = false
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            act,
                            Manifest.permission.READ_CONTACTS
                        )
                    ) {
                        lifecycleScope.launch {
                            delay(200)
                            Dialogs.permissionDeniedDialog(
                                act,
                                getString(R.string.tap_on_settings_to_enable_required_permissions),
                            ) {

                                val intent = Intent().apply {
                                    action =
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    data = Uri.fromParts("package", act.packageName, null)
                                }
                                startActivity(intent)
                            }
                        }
                    }
                }
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isAlive {
            // Ensure the parent fragment implements the interface
            val parentFragment = parentFragment
            if (parentFragment is OnMediaItemClickCallback) {
                onMediaItemClickCallback = parentFragment
            } else {
                throw IllegalStateException("Parent fragment must implement OnMediaItemClickCallback")
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentContactsSolBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isAlive {
            observeList()
            initListener()

        }
    }

    override fun onResume() {
        super.onResume()
        if (!isDataLoaded) {
            activity?.let { act ->
                if (PermissionManager.hasContactPermission(act)) {
                    fetchContacts()
                } else {

                    binding.apply {
                        progressBar.gone()
                        rvContacts.gone()
                        tvNoData.gone()
                        headerLayout.gone()
                        contactPermissionCardView.visible()
                    }
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchContacts() {
        Log.d("fetch___", "fetchApps")
        lifecycleScope.launch(Dispatchers.IO) {
            activity?.let { act ->
                viewModel.getContacts()
            }
        }
    }

    private fun observeList() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFetchingComplete.collect { isComplete ->
                    updateUi(isComplete)
                }
            }
        }
    }

    private fun updateUi(isComplete: Boolean) {
        if (isComplete) {
            binding.dateTextView.text = "${getString(R.string.contacts)} (${viewModel.contactList.size})"
            setupRecyclerView(viewModel.contactList)
            isDataLoaded = true

        } else {
            binding.progressBar.visible()
            binding.rvContacts.gone()
            binding.tvNoData.gone()
            binding.headerLayout.gone()
            binding.contactPermissionCardView.gone()
        }
    }

    private fun setupRecyclerView(list: List<MediaInfoModel>) {
        if (list.isEmpty()) {
            binding.tvNoData.visible()
            binding.rvContacts.gone()
            binding.progressBar.gone()
            binding.headerLayout.gone()
            binding.contactPermissionCardView.gone()


        } else {
            adapter = ContactsAdapter(list, this)
            binding.tvNoData.gone()
            binding.progressBar.gone()
            binding.contactPermissionCardView.gone()
            binding.rvContacts.visible()
            binding.headerLayout.visible()
            binding.rvContacts.adapter = adapter
        }
    }

    private fun initListener() {
        updateSelectAllState(false)
        binding.apply {
            checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
                if (!checkboxSelectAll.isPressed) {
                    return@setOnCheckedChangeListener
                }
                adapter?.selectAllContacts(isChecked, viewModel.contactList, lifecycleScope){
                    onMediaItemClickCallback.onMediaItemClicked()
                    updateSelectAllState(isChecked)
                }
            }
            allowButton.setSafeOnClickListener {
                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun updateSelectAllState(isChecked: Boolean) {
        binding.checkboxSelectAll.setBackgroundResource(
            if (isChecked) R.drawable.check_circle else R.drawable.uncheck_circle
        )
        binding.selectTv.text = if (isChecked) getString(R.string.de_select_all) else getString(R.string.select_all)
        binding.selectTv.setTextColor(resources.getColor(R.color.sub_heading_text_color, null))
    }

    override fun onMediaItemClickedForSelectAll() {
        onMediaItemClickCallback.onMediaItemClicked()
        val allSelected = SelectedListManager.getSelectedContactsList().containsAll(viewModel.contactList )
        updateSelectAllState(allSelected)
        binding.checkboxSelectAll.isChecked = allSelected
    }
}